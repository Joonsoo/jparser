//! Structure-sharing history container for `ParsingCtx.history`.
//!
//! The parse builds one `HistoryEntry` per gen. Naively this lived in a flat
//! `Vec<HistoryEntry>`, but the incremental session needs to (a) snapshot a
//! checkpoint every K gens, (b) retain the last parse's full history as a
//! canonical source, and (c) hand a resumable ctx a *prefix* of that history —
//! all of which were O(gen) deep copies, making a full parse O(N²/K) and every
//! edit O(N). See `session.rs` (design §1.3 / §2.2).
//!
//! `History` splits the log into sealed, immutable, `Rc`-shared CHUNKs plus a
//! mutable `tail`. Cloning bumps the chunk `Rc`s (no entry copy) and deep-copies
//! only the small unsealed tail; a `prefix(k)` shares every whole chunk within
//! `[0, k)` by `Rc` bump and copies only the (≤ CHUNK) straddling remainder. So
//! the session's snapshot / canonical-retain / restore operations drop from
//! O(gen) to O(#chunks + CHUNK). Reads (`get`) are O(log #chunks); the sequential
//! `iter`/`push`/`last` the parser hot path uses stay O(1) amortized.
//!
//! The container is behavior-transparent: `iter`/`get`/`len`/`last` over a
//! `History` yield exactly what the equivalent `Vec<HistoryEntry>` would, so the
//! parser output is byte-identical (enforced by the session differential oracle).

use std::rc::Rc;

use crate::parsing_ctx::HistoryEntry;

/// tail auto-seal threshold. A `push` that fills the tail to `CHUNK` entries
/// seals it into a shared chunk (buffer move, no copy).
const CHUNK: usize = 256;

/// Chunked, structure-sharing sequence of `HistoryEntry`.
///
/// Invariants:
/// - `offsets.len() == chunks.len()`; `offsets[i]` is the absolute start index of
///   `chunks[i]`. Offsets are strictly ascending (every chunk has ≥ 1 entry).
/// - `len == (sum of chunk lens) + tail.len()`.
/// - the chunked region occupies indices `[0, len - tail.len())`; the tail
///   occupies `[len - tail.len(), len)`.
/// - chunks may be *ragged* (an explicit `seal()` can produce a chunk shorter
///   than `CHUNK`); `offsets` makes indexing independent of chunk width.
#[derive(Clone, Debug, Default)]
pub struct History {
    /// Sealed, immutable, shared chunks (ragged allowed).
    chunks: Vec<Rc<Vec<HistoryEntry>>>,
    /// Absolute start index of each chunk (same length as `chunks`).
    offsets: Vec<usize>,
    /// Not-yet-sealed suffix; `push` appends here.
    tail: Vec<HistoryEntry>,
    /// Total entry count (cache of chunked + tail).
    len: usize,
}

impl History {
    /// Empty history.
    pub fn new() -> Self {
        History { chunks: Vec::new(), offsets: Vec::new(), tail: Vec::new(), len: 0 }
    }

    /// History holding a single entry (the parser's gen-0 init entry).
    pub fn from_entry(entry: HistoryEntry) -> Self {
        History { chunks: Vec::new(), offsets: Vec::new(), tail: vec![entry], len: 1 }
    }

    #[inline]
    pub fn len(&self) -> usize {
        self.len
    }

    #[inline]
    pub fn is_empty(&self) -> bool {
        self.len == 0
    }

    /// Last entry (tail's last if the tail is non-empty, else the last sealed
    /// chunk's last — the tail is empty only right after a seal or when empty).
    pub fn last(&self) -> Option<&HistoryEntry> {
        if let Some(e) = self.tail.last() {
            Some(e)
        } else {
            self.chunks.last().and_then(|c| c.last())
        }
    }

    /// Append an entry. Auto-seals when the tail reaches `CHUNK` entries.
    pub fn push(&mut self, entry: HistoryEntry) {
        self.tail.push(entry);
        self.len += 1;
        if self.tail.len() == CHUNK {
            self.seal();
        }
    }

    /// Move a non-empty tail into a shared chunk (buffer move via `mem::take`,
    /// no entry copy). No-op on an empty tail.
    pub fn seal(&mut self) {
        if self.tail.is_empty() {
            return;
        }
        let start = self.len - self.tail.len();
        self.chunks.push(Rc::new(std::mem::take(&mut self.tail)));
        self.offsets.push(start);
    }

    /// Entry at absolute index `i` (`None` if out of range). O(log #chunks):
    /// `partition_point` locates the owning chunk via `offsets`.
    pub fn get(&self, i: usize) -> Option<&HistoryEntry> {
        if i >= self.len {
            return None;
        }
        let chunked_len = self.len - self.tail.len();
        if i >= chunked_len {
            return self.tail.get(i - chunked_len);
        }
        // Last chunk whose start offset <= i (offsets ascending, offsets[0]==0).
        let ci = self.offsets.partition_point(|&off| off <= i) - 1;
        self.chunks[ci].get(i - self.offsets[ci])
    }

    /// Iterate every entry in order (chunks then tail).
    pub fn iter(&self) -> impl Iterator<Item = &HistoryEntry> {
        self.chunks.iter().flat_map(|c| c.iter()).chain(self.tail.iter())
    }

    /// A new `History` holding the first `k` entries. Whole chunks within
    /// `[0, k)` are shared by `Rc` bump; the single chunk (or tail) straddling
    /// `k` contributes its first `k - start` entries as a fresh (deep-copied)
    /// tail. `k == 0` → empty; `k >= len` → a full clone.
    pub fn prefix(&self, k: usize) -> History {
        let k = k.min(self.len);
        if k == 0 {
            return History::new();
        }
        let mut chunks: Vec<Rc<Vec<HistoryEntry>>> = Vec::new();
        let mut offsets: Vec<usize> = Vec::new();
        let mut tail: Vec<HistoryEntry> = Vec::new();
        for (ci, chunk) in self.chunks.iter().enumerate() {
            let start = self.offsets[ci];
            let end = start + chunk.len();
            if end <= k {
                // Whole chunk inside the prefix — share by Rc bump.
                chunks.push(Rc::clone(chunk));
                offsets.push(start);
            } else if start < k {
                // Straddling chunk — deep-copy its first (k - start) entries.
                tail.extend_from_slice(&chunk[..k - start]);
                return History { chunks, offsets, tail, len: k };
            } else {
                break;
            }
        }
        // k reaches into the tail region (all chunks fully shared) — copy the
        // needed tail prefix.
        let chunked_len = self.len - self.tail.len();
        if k > chunked_len {
            tail.extend_from_slice(&self.tail[..k - chunked_len]);
        }
        History { chunks, offsets, tail, len: k }
    }

    /// Mutable ref to the last entry. If it lives in the tail it is returned
    /// directly; if the tail is empty (last entry in a sealed chunk) the chunk is
    /// `Rc::make_mut` copy-on-write'd first. No production caller — only a
    /// `parser/result.rs` test that mutates the final gen's records.
    pub fn last_mut(&mut self) -> Option<&mut HistoryEntry> {
        if !self.tail.is_empty() {
            return self.tail.last_mut();
        }
        let chunk = self.chunks.last_mut()?;
        Rc::make_mut(chunk).last_mut()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A cheaply-distinguishable `HistoryEntry`: tag the gen into `line`-free
    /// `HistoryEntry`… it has no scalar field, so encode the tag as a synthetic
    /// `AddedKernelRecord` symbol_id and compare on that.
    fn entry(tag: i32) -> HistoryEntry {
        use crate::accept_condition::AcceptCondition;
        use crate::parsing_ctx::AddedKernelRecord;
        use crate::path_root::PathRoot;
        let mut e = HistoryEntry::default();
        e.added_kernels.push(AddedKernelRecord {
            symbol_id: tag,
            pointer: 0,
            begin_gen: 0,
            end_gen: 0,
            condition: AcceptCondition::Always,
            root: PathRoot::new(0, 0),
        });
        e
    }

    fn tag_of(e: &HistoryEntry) -> i32 {
        e.added_kernels[0].symbol_id
    }

    /// Reference model: a plain Vec that we mirror every op onto.
    fn assert_equiv(h: &History, reference: &[HistoryEntry]) {
        assert_eq!(h.len(), reference.len(), "len");
        assert_eq!(h.is_empty(), reference.is_empty(), "is_empty");
        assert_eq!(
            h.last().map(tag_of),
            reference.last().map(tag_of),
            "last"
        );
        // get() over the full range + boundaries.
        for i in 0..reference.len() {
            assert_eq!(h.get(i).map(tag_of), Some(tag_of(&reference[i])), "get({i})");
        }
        assert!(h.get(reference.len()).is_none(), "get(len) must be None");
        assert!(h.get(reference.len() + 5).is_none(), "get(len+5) must be None");
        // iter() order.
        let got: Vec<i32> = h.iter().map(tag_of).collect();
        let want: Vec<i32> = reference.iter().map(tag_of).collect();
        assert_eq!(got, want, "iter order");
    }

    #[test]
    fn push_seal_get_iter_equivalence() {
        // Push well past several CHUNK boundaries so auto-seal kicks in.
        let n = CHUNK * 3 + 37;
        let mut h = History::new();
        let mut reference: Vec<HistoryEntry> = Vec::new();
        assert_equiv(&h, &reference);
        for g in 0..n {
            h.push(entry(g as i32));
            reference.push(entry(g as i32));
        }
        assert_equiv(&h, &reference);
        // A trailing manual seal must not change observable content.
        h.seal();
        assert_equiv(&h, &reference);
        // Sealing again (empty tail) is a no-op.
        h.seal();
        assert_equiv(&h, &reference);
    }

    #[test]
    fn from_entry_then_push() {
        let mut h = History::from_entry(entry(0));
        let mut reference = vec![entry(0)];
        for g in 1..(CHUNK + 5) {
            h.push(entry(g as i32));
            reference.push(entry(g as i32));
        }
        assert_equiv(&h, &reference);
    }

    #[test]
    fn ragged_chunks_from_early_seal() {
        // Seal at arbitrary points, then keep pushing — produces ragged chunks.
        let mut h = History::new();
        let mut reference: Vec<HistoryEntry> = Vec::new();
        let seal_after = [3usize, 10, 11, 300, 301, 600];
        let n = 700;
        for g in 0..n {
            h.push(entry(g as i32));
            reference.push(entry(g as i32));
            if seal_after.contains(&g) {
                h.seal();
            }
        }
        assert_equiv(&h, &reference);
    }

    #[test]
    fn prefix_all_cases() {
        // Build ragged chunks so prefix hits boundary / straddle / tail cases.
        let mut h = History::new();
        let mut reference: Vec<HistoryEntry> = Vec::new();
        let seal_after = [5usize, 260, 300, 590];
        let n = CHUNK * 2 + 100; // 612
        for g in 0..n {
            h.push(entry(g as i32));
            reference.push(entry(g as i32));
            if seal_after.contains(&g) {
                h.seal();
            }
        }
        // Cover: 0, full, over-full, exact chunk boundaries, straddles, tail cuts.
        let cuts = [
            0usize, 1, 6, 100, 256, 261, 301, 400, 512, 591, 600, n - 1, n, n + 10,
        ];
        for &k in &cuts {
            let p = h.prefix(k);
            let want_len = k.min(n);
            assert_equiv(&p, &reference[..want_len]);
            // The prefix must be independently pushable afterwards.
            let mut p2 = p;
            p2.push(entry(-1));
            let mut want2: Vec<HistoryEntry> = reference[..want_len].to_vec();
            want2.push(entry(-1));
            assert_equiv(&p2, &want2);
        }
    }

    #[test]
    fn prefix_shares_chunks_by_rc() {
        // A prefix that lands exactly on a chunk boundary must SHARE the backing
        // chunk (Rc bump), not deep-copy it.
        let mut h = History::new();
        for g in 0..(CHUNK * 2) {
            h.push(entry(g as i32));
        }
        // one full chunk of width CHUNK exists at offset 0; strong count == 1.
        let p = h.prefix(CHUNK);
        assert_eq!(p.len(), CHUNK);
        // The shared chunk now has strong count 2 (h + p).
        assert_eq!(Rc::strong_count(&h.chunks[0]), 2, "prefix should Rc-share the chunk");
    }

    #[test]
    fn clone_after_seal_shares() {
        let mut h = History::new();
        for g in 0..(CHUNK + 10) {
            h.push(entry(g as i32));
        }
        h.seal();
        let c = h.clone();
        // Every chunk is shared with the clone.
        for chunk in &h.chunks {
            assert_eq!(Rc::strong_count(chunk), 2, "clone should bump chunk Rc");
        }
        assert_eq!(c.len(), h.len());
    }

    #[test]
    fn last_mut_cow_on_sealed_chunk() {
        let mut h = History::new();
        for g in 0..CHUNK {
            h.push(entry(g as i32));
        }
        h.seal(); // last entry now lives in a sealed chunk; tail empty.
        let shared = h.clone(); // force the chunk to be shared (strong_count 2).
        // Mutating last must CoW, not corrupt the shared clone.
        h.last_mut().unwrap().added_kernels[0].symbol_id = 999;
        assert_eq!(tag_of(h.last().unwrap()), 999);
        assert_eq!(tag_of(shared.last().unwrap()), (CHUNK - 1) as i32, "clone unchanged");
    }
}
