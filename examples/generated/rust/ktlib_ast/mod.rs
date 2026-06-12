// Hand-written sketch of what Stage4RustEmit's per-grammar crates will depend on.
// Mirrors `com.giyeok.jparser.ktlib.{Kernel,KernelSet}` plus the helpers in
// `AstifierUtil.kt`. Eventually this lives inside `mgroup3-native` (or a new
// `mgroup3-ktlib` crate) as a stable dependency for all generated parsers.
//
// Module is `pub use`'d by every generated *_ast.rs file so the generator can
// reference paths like `crate::ktlib::Kernel`.

use std::collections::HashSet;

#[derive(Copy, Clone, PartialEq, Eq, Hash, Debug)]
pub struct Kernel {
    pub symbol_id: i32,
    pub pointer: i32,
    pub begin_gen: i32,
    pub end_gen: i32,
}

#[derive(Clone, Debug, Default)]
pub struct KernelSet {
    pub kernels: HashSet<Kernel>,
}

impl KernelSet {
    pub fn new(kernels: HashSet<Kernel>) -> Self {
        Self { kernels }
    }

    pub fn contains(&self, kernel: &Kernel) -> bool {
        self.kernels.contains(kernel)
    }

    pub fn filter_by_begin_gen(
        &self,
        symbol_id: i32,
        pointer: i32,
        begin_gen: i32,
    ) -> Vec<Kernel> {
        self.kernels
            .iter()
            .filter(|k| k.symbol_id == symbol_id && k.pointer == pointer && k.begin_gen == begin_gen)
            .copied()
            .collect()
    }

    pub fn find_by_begin_gen(&self, symbol_id: i32, pointer: i32, begin_gen: i32) -> Kernel {
        let matches = self.filter_by_begin_gen(symbol_id, pointer, begin_gen);
        check_single(&matches);
        matches[0]
    }

    pub fn find_by_begin_gen_opt(
        &self,
        symbol_id: i32,
        pointer: i32,
        begin_gen: i32,
    ) -> Option<Kernel> {
        let matches = self.filter_by_begin_gen(symbol_id, pointer, begin_gen);
        check_single_or_none(&matches);
        matches.into_iter().next()
    }

    /// Mirrors `KernelSet.getSingle(symbolId, pointer, beginGen, endGen)`.
    /// Panics if no kernel at that exact 4-tuple.
    pub fn get_single(&self, symbol_id: i32, pointer: i32, begin_gen: i32, end_gen: i32) -> Kernel {
        self.kernels
            .iter()
            .copied()
            .find(|k| {
                k.symbol_id == symbol_id
                    && k.pointer == pointer
                    && k.begin_gen == begin_gen
                    && k.end_gen == end_gen
            })
            .unwrap_or_else(|| {
                panic!(
                    "no kernel ({symbol_id},{pointer},{begin_gen}..{end_gen}) in set",
                )
            })
    }
}

fn check_single<T>(xs: &[T]) {
    assert_eq!(
        xs.len(),
        1,
        "Kernel size was expected to be 1, but it was {}",
        xs.len()
    );
}

fn check_single_or_none<T>(xs: &[T]) {
    assert!(xs.len() <= 1, "Kernel size was expected to be <= 1, was {}", xs.len());
}

pub fn has_single_true(bs: &[bool]) -> bool {
    bs.iter().filter(|b| **b).count() == 1
}

/// `(begin_gen, end_gen)` pair, matching Kotlin's `Pair<Int, Int>`.
pub type GenSpan = (i32, i32);

/// Port of `AstifierUtil.getSequenceElems`. Walks back through a fixed-length
/// sequence and returns one `(begin, end)` per element in left-to-right order.
pub fn get_sequence_elems(
    history: &[KernelSet],
    sequence_id: i32,
    elems: &[i32],
    begin_gen: i32,
    end_gen: i32,
) -> Vec<GenSpan> {
    let n = elems.len();
    let last_elem = history[end_gen as usize].find_by_begin_gen(sequence_id, n as i32, begin_gen);
    let mut list = vec![last_elem];
    let mut curr_gen = last_elem.end_gen;
    for pointer in (0..n).rev() {
        let candidates = history[curr_gen as usize]
            .filter_by_begin_gen(sequence_id, pointer as i32, begin_gen);
        let curr_gen_snapshot = curr_gen;
        let filtered: Vec<Kernel> = candidates
            .into_iter()
            .filter(|prev| {
                history[curr_gen_snapshot as usize].contains(&Kernel {
                    symbol_id: elems[pointer],
                    pointer: 1,
                    begin_gen: prev.end_gen,
                    end_gen: curr_gen_snapshot,
                })
            })
            .collect();
        check_single(&filtered);
        let prev_elem = filtered[0];
        list.push(prev_elem);
        curr_gen = prev_elem.end_gen;
    }
    // list[0] = outer seq kernel, list[k] = prev for pointer (n-k) walking
    // right-to-left. Output spans in left-to-right element order: idx i in
    // result corresponds to pointer i, span (list[n-i].end_gen, list[n-i-1].end_gen).
    (0..n)
        .map(|i| (list[n - i].end_gen, list[n - i - 1].end_gen))
        .collect()
}

/// Port of `AstifierUtil.unrollRepeat0`. Returns one `(begin, end)` per
/// repeated item, left-to-right. Empty list for zero repetitions.
pub fn unroll_repeat0(
    history: &[KernelSet],
    symbol_id: i32,
    item_sym_id: i32,
    base_seq: i32,
    repeat_seq: i32,
    begin_gen: i32,
    end_gen: i32,
) -> Vec<GenSpan> {
    let mut acc: Vec<GenSpan> = Vec::new();
    let mut bg = begin_gen;
    let mut eg = end_gen;
    loop {
        let base = history[eg as usize].find_by_begin_gen_opt(base_seq, 0, bg);
        let repeat = history[eg as usize].find_by_begin_gen_opt(repeat_seq, 2, bg);
        assert!(has_single_true(&[base.is_some(), repeat.is_some()]));
        if base.is_some() {
            return acc;
        }
        let seq = get_sequence_elems(history, repeat_seq, &[symbol_id, item_sym_id], bg, eg);
        let repeating = seq[0];
        let item = seq[1];
        acc.insert(0, item);
        bg = repeating.0;
        eg = repeating.1;
    }
}

/// Port of `AstifierUtil.unrollRepeat1`. Identical shape but the base case
/// produces one item rather than empty.
pub fn unroll_repeat1(
    history: &[KernelSet],
    symbol_id: i32,
    item_sym_id: i32,
    base_seq: i32,
    repeat_seq: i32,
    begin_gen: i32,
    end_gen: i32,
) -> Vec<GenSpan> {
    let mut acc: Vec<GenSpan> = Vec::new();
    let mut bg = begin_gen;
    let mut eg = end_gen;
    loop {
        let base = history[eg as usize].find_by_begin_gen_opt(base_seq, 1, bg);
        let repeat = history[eg as usize].find_by_begin_gen_opt(repeat_seq, 2, bg);
        assert!(has_single_true(&[base.is_some(), repeat.is_some()]));
        if base.is_some() {
            let base_item = history[eg as usize].find_by_begin_gen(item_sym_id, 1, bg);
            acc.insert(0, (base_item.begin_gen, base_item.end_gen));
            return acc;
        }
        let seq = get_sequence_elems(history, repeat_seq, &[symbol_id, item_sym_id], bg, eg);
        let repeating = seq[0];
        let item = seq[1];
        acc.insert(0, item);
        bg = repeating.0;
        eg = repeating.1;
    }
}

/// Issues monotonically-increasing node IDs. Matches `IdIssuerImpl` from Kotlin.
pub struct IdIssuer {
    next: i32,
}

impl IdIssuer {
    pub fn new(start: i32) -> Self {
        Self { next: start }
    }
    pub fn next_id(&mut self) -> i32 {
        let id = self.next;
        self.next += 1;
        id
    }
}
