//! Record-condition evaluation without replay. Mirror of Kotlin
//! `RecordConditionEvaluator.kt`.
//!
//! `evaluate_record_condition` (core.rs) replays the entire remaining parse
//! per record — O(records × gens). This evaluator computes the same answer
//! with per-leaf lookups derived from the `evolve_accept_condition` branches
//! (see `mgroup3/docs/kernels_history_optimization.md`):
//!  - Unless/OnlyIf: only the exact-span finish — eager at `gen == end_gen`,
//!    late at `gen == end_gen + 1`; if no eager fin, wait one step only while
//!    the root is alive at `end_gen` (the evolve active_cond_paths branch).
//!  - NoLongerMatch/NeedLongerMatch: only finishes with end >= min_end_gen
//!    (eager fin end == gen, late fin end == gen-1).
//!  - NotExists/Exists: every finish on both channels, end-agnostic.
//!  - Absorption window: in the replay a leaf drops its pending obligation
//!    (and resolves) at the first step where the root is inactive, so the fin
//!    scan is capped at [from_gen, last_active+1] (fins at last_active+1 are
//!    registered by the paths dying that step — still absorbed). If the root
//!    is inactive at from_gen, only that step's fins are absorbed. The
//!    ever_seen_cond_roots rule (a root is never restarted) makes the active
//!    interval contiguous and forbids fins after death.
//!  - Absorbed finish conditions recurse at their observed gen.
//!  - visiting (same-step self-recursion guard): NoLongerMatch AND
//!    NeedLongerMatch both become Always (mirroring evolve — not duals!);
//!    the other leaves skip only the current step's consumption and continue
//!    fresh from the next step (= eval_cond(cond, from_gen + 1, ∅)).
//!  - End of input: a virtual late step at `gen == history.len()` carries
//!    `end_late_fins`; residual leaves resolve NoLongerMatch/NotExists/Unless
//!    true and their duals false.
//!
//! One evaluator per `kernels_history` / `is_accepted` call — no shared
//! mutable cache on the parser struct (it is shared across threads by the
//! bibix4 FFI; a shared cache measured 40s of lock contention before).

use std::cell::RefCell;
use std::sync::atomic::{AtomicBool, AtomicI32, Ordering};
use std::sync::OnceLock;

use rustc_hash::{FxHashMap as HashMap, FxHashSet as HashSet};

use crate::accept_condition::AcceptCondition;
use crate::history::History;
use crate::path_root::PathRoot;

use super::core::evaluate_record_condition;

/// Debug switch: also run the replay evaluation and panic on mismatch.
/// Test/debug only — must be off for performance runs.
fn diff_check_enabled() -> bool {
    static FLAG: OnceLock<bool> = OnceLock::new();
    *FLAG.get_or_init(|| std::env::var_os("MG3_RECORD_COND_DIFF").is_some())
}

/// Per-root contiguous active interval `[first_active, last_active]` (gen units),
/// extracted from a parse's per-gen `active_cond_paths`. This is exactly the
/// `first_active`/`last_active` computation `RecordConditionEvaluator::new`
/// performs (kept in sync with it), exposed standalone so the incremental
/// session can compute its reuse boundary (`EditReuse`) without building a full
/// evaluator. A root's activity is contiguous (the `ever_seen_cond_roots` rule
/// forbids restart), so a single first/last pair per root is exact.
pub fn active_intervals(history: &History) -> HashMap<PathRoot, (i32, i32)> {
    let mut intervals: HashMap<PathRoot, (i32, i32)> = HashMap::default();
    for (g, entry) in history.iter().enumerate() {
        let g = g as i32;
        for root in &entry.active_cond_paths {
            intervals.entry(*root).and_modify(|iv| iv.1 = g).or_insert((g, g));
        }
    }
    intervals
}

/// The `PathRoot` a single accept-condition LEAF observes, or `None` for
/// `Always`/`Never`/composites. THE canonical leaf→root mapping: `compute`
/// (the evaluator) and `collect_referenced_roots` (the reuse-graph builder)
/// BOTH go through this so the graph can never interpret a leaf differently
/// from the evaluator (a drift there would make the transitive-closure
/// dirty-boundary unsound). Every leaf variant observes
/// `PathRoot::new(symbol_id, start_gen)`.
#[inline]
pub fn leaf_root(cond: &AcceptCondition) -> Option<PathRoot> {
    match cond {
        AcceptCondition::NoLongerMatch { symbol_id, start_gen, .. }
        | AcceptCondition::NeedLongerMatch { symbol_id, start_gen, .. }
        | AcceptCondition::NotExists { symbol_id, start_gen }
        | AcceptCondition::Exists { symbol_id, start_gen }
        | AcceptCondition::Unless { symbol_id, start_gen, .. }
        | AcceptCondition::OnlyIf { symbol_id, start_gen, .. } => {
            Some(PathRoot::new(*symbol_id, *start_gen))
        }
        AcceptCondition::Always
        | AcceptCondition::Never
        | AcceptCondition::And { .. }
        | AcceptCondition::Or { .. } => None,
    }
}

/// All roots the condition tree references (union over its leaves), via
/// `leaf_root` — exactly the roots the evaluator's `eval_cond` would resolve and
/// scan when evaluating `cond`. Feeds the reuse reference graph.
pub fn collect_referenced_roots(cond: &AcceptCondition, out: &mut HashSet<PathRoot>) {
    match cond {
        AcceptCondition::And { items } | AcceptCondition::Or { items } => {
            for c in items {
                collect_referenced_roots(c, out);
            }
        }
        _ => {
            if let Some(r) = leaf_root(cond) {
                out.insert(r);
            }
        }
    }
}

/// The set of roots whose record-condition replay could reach gen `resume_gen`
/// or beyond (into the edited / re-parsed region) — the DANGEROUS set `D` for
/// the Stage-1 reuse boundary (§2 + `compute_dirty_lo`). This is the transitive
/// closure that closes the direct-reference gap: a prefix record referencing a
/// "safe" root whose finish condition in turn references a dangerous root would
/// otherwise be mis-classified reusable.
///
/// Edges: `r → r'` iff root `r`'s finish condition (in `cond_path_finishes` /
/// `late_cond_path_finishes` per gen, and the end-of-input `end_late_fins` — the
/// SAME fin sources the evaluator follows) references `r'` (via
/// `collect_referenced_roots`). A root is dangerous if its own activity reaches
/// `resume_gen` (`D0 = { r : last_active(r) >= resume_gen }`) OR it can reach a
/// `D0` root along edges. Since danger propagates from `r'` back to `r`, we build
/// REVERSE adjacency (`referenced_by[r'] = { r }`) and BFS out from `D0`.
pub fn dangerous_roots(
    history: &History,
    end_late_fins: &HashMap<PathRoot, AcceptCondition>,
    intervals: &HashMap<PathRoot, (i32, i32)>,
    resume_gen: i32,
) -> HashSet<PathRoot> {
    // referenced_by[r'] = roots whose fin condition references r'.
    let mut referenced_by: HashMap<PathRoot, Vec<PathRoot>> = HashMap::default();
    let mut scratch: HashSet<PathRoot> = HashSet::default();
    let add_fin_edges = |r: PathRoot,
                         cond: &AcceptCondition,
                         referenced_by: &mut HashMap<PathRoot, Vec<PathRoot>>,
                         scratch: &mut HashSet<PathRoot>| {
        scratch.clear();
        collect_referenced_roots(cond, scratch);
        for &rp in scratch.iter() {
            referenced_by.entry(rp).or_default().push(r);
        }
    };
    for entry in history.iter() {
        for (r, cond) in &entry.cond_path_finishes {
            add_fin_edges(*r, cond, &mut referenced_by, &mut scratch);
        }
        for (r, cond) in &entry.late_cond_path_finishes {
            add_fin_edges(*r, cond, &mut referenced_by, &mut scratch);
        }
    }
    for (r, cond) in end_late_fins {
        add_fin_edges(*r, cond, &mut referenced_by, &mut scratch);
    }

    // Seed D0 (roots active at/after resume) and BFS backward over the edges.
    let mut d: HashSet<PathRoot> = HashSet::default();
    let mut work: Vec<PathRoot> = Vec::new();
    for (&r, &(_fa, la)) in intervals {
        if la >= resume_gen && d.insert(r) {
            work.push(r);
        }
    }
    while let Some(r) = work.pop() {
        if let Some(preds) = referenced_by.get(&r) {
            for &p in preds {
                if d.insert(p) {
                    work.push(p);
                }
            }
        }
    }
    d
}

// -- reference-birth invariant probe (debug/verification only) --------------
//
// `compute_dirty_lo` bounds "the earliest gen a record could reference a
// dangerous root r" by `first_active(r)`, i.e. it assumes a record's condition
// only references roots ALREADY born by the record's gen (no forward / lookahead
// reference to an unborn root). This probe measures that assumption over real
// parses: when enabled, every top-level `evaluate(cond, record_gen)` records
// `max(first_active(referenced_root) - record_gen)` (a positive value would be a
// violation — a record referencing a root born LATER than itself). The oracle
// asserts the max skew is <= 0 (invariant holds, no dirty_lo margin needed).
static BIRTH_PROBE_ON: AtomicBool = AtomicBool::new(false);
static BIRTH_MAX_SKEW: AtomicI32 = AtomicI32::new(i32::MIN);

/// Enable the reference-birth probe and reset its running max. Test-only.
pub fn birth_probe_enable() {
    BIRTH_MAX_SKEW.store(i32::MIN, Ordering::SeqCst);
    BIRTH_PROBE_ON.store(true, Ordering::SeqCst);
}
/// Disable the probe. Test-only.
pub fn birth_probe_disable() {
    BIRTH_PROBE_ON.store(false, Ordering::SeqCst);
}
/// Max observed `first_active(referenced_root) - record_gen` since the last
/// `birth_probe_enable` (`i32::MIN` if the probe saw no checkable reference).
pub fn birth_probe_max_skew() -> i32 {
    BIRTH_MAX_SKEW.load(Ordering::SeqCst)
}

pub struct RecordConditionEvaluator<'a> {
    history: &'a History,
    /// Owned so a `KernelsQuery` can hold an evaluator without a self-referential
    /// borrow of the caller-local `end_late` map (see `Mgroup3Parser::kernels_query`).
    end_late_fins: HashMap<PathRoot, AcceptCondition>,
    history_size: i32,
    /// Inverted index: root → gens that have a fin for it (ascending — built
    /// in history order).
    eager_fin_gens: HashMap<PathRoot, Vec<i32>>,
    late_fin_gens: HashMap<PathRoot, Vec<i32>>,
    /// Contiguous active interval per root: [first_active, last_active].
    first_active: HashMap<PathRoot, i32>,
    last_active: HashMap<PathRoot, i32>,
    /// (condition, normalized gen, visiting) → result. RefCell so the
    /// recursive `eval_cond` can hold `&self` references into `history`.
    #[allow(clippy::type_complexity)]
    memo: RefCell<HashMap<AcceptCondition, HashMap<(i32, Vec<PathRoot>), bool>>>,
}

impl<'a> RecordConditionEvaluator<'a> {
    pub fn new(
        history: &'a History,
        end_late_fins: HashMap<PathRoot, AcceptCondition>,
    ) -> Self {
        let mut eager_fin_gens: HashMap<PathRoot, Vec<i32>> = HashMap::default();
        let mut late_fin_gens: HashMap<PathRoot, Vec<i32>> = HashMap::default();
        let mut first_active: HashMap<PathRoot, i32> = HashMap::default();
        let mut last_active: HashMap<PathRoot, i32> = HashMap::default();
        for (g, entry) in history.iter().enumerate() {
            let g = g as i32;
            for root in entry.cond_path_finishes.keys() {
                eager_fin_gens.entry(*root).or_default().push(g);
            }
            for root in entry.late_cond_path_finishes.keys() {
                late_fin_gens.entry(*root).or_default().push(g);
            }
            for root in &entry.active_cond_paths {
                first_active.entry(*root).or_insert(g);
                last_active.insert(*root, g);
            }
        }
        Self {
            history,
            end_late_fins,
            history_size: history.len() as i32,
            eager_fin_gens,
            late_fin_gens,
            first_active,
            last_active,
            memo: RefCell::new(HashMap::default()),
        }
    }

    pub fn evaluate(&self, cond: &AcceptCondition, record_gen: i32) -> bool {
        if BIRTH_PROBE_ON.load(Ordering::Relaxed) {
            self.probe_reference_birth(cond, record_gen);
        }
        let result = self.eval_cond(cond, record_gen, &[]);
        if diff_check_enabled() {
            let replayed =
                evaluate_record_condition(cond, self.history, record_gen, &self.end_late_fins);
            assert_eq!(
                result, replayed,
                "RecordConditionEvaluator mismatch: record_gen={record_gen} cond={cond}"
            );
        }
        result
    }

    /// Reference-birth probe (see the `BIRTH_PROBE_*` statics): record the max
    /// `first_active(referenced_root) - record_gen` for this top-level record. A
    /// root the record references but that never became active has no
    /// `first_active` — it is unconditionally safe (never in the dangerous set,
    /// since danger requires activity or a fin edge) so it is skipped.
    fn probe_reference_birth(&self, cond: &AcceptCondition, record_gen: i32) {
        let mut refs: HashSet<PathRoot> = HashSet::default();
        collect_referenced_roots(cond, &mut refs);
        for r in &refs {
            if let Some(&fa) = self.first_active.get(r) {
                BIRTH_MAX_SKEW.fetch_max(fa - record_gen, Ordering::Relaxed);
            }
        }
    }

    fn eval_cond(&self, cond: &AcceptCondition, from_gen: i32, visiting: &[PathRoot]) -> bool {
        match cond {
            AcceptCondition::Always => return true,
            AcceptCondition::Never => return false,
            _ => {}
        }
        // With a non-empty visiting set the consumption at from_gen itself
        // differs, so no normalization.
        let eff_gen =
            if visiting.is_empty() { self.normalized_gen(cond, from_gen) } else { from_gen };
        let inner_key = (eff_gen, visiting.to_vec());
        if let Some(&v) = self.memo.borrow().get(cond).and_then(|m| m.get(&inner_key)) {
            return v;
        }
        let result = self.compute(cond, eff_gen, visiting);
        self.memo.borrow_mut().entry(cond.clone()).or_default().insert(inner_key, result);
        result
    }

    /// Clamp `g` to a representative gen within a same-answer range — keeps
    /// the memo key space small (records reference the same condition from
    /// many different record gens).
    ///  - Unless/OnlyIf consume nothing before end_gen (the `gen < endGen`
    ///    evolve branch), so every g <= end_gen answers alike; g >= end_gen+2
    ///    likewise.
    ///  - NLM/NeedLM inside the active interval ignore fins before the
    ///    min_end_gen clamp, so g <= min_end_gen answers alike (the window
    ///    cap last_active+1 does not depend on g). Inactive g is not
    ///    normalized: whether that step's fins are absorbed depends on g.
    ///  - NotExists/Exists and composites: from_gen itself is the scan start.
    fn normalized_gen(&self, cond: &AcceptCondition, g: i32) -> i32 {
        match cond {
            AcceptCondition::Unless { end_gen, .. } | AcceptCondition::OnlyIf { end_gen, .. } => {
                if g <= *end_gen {
                    *end_gen
                } else {
                    g.min(*end_gen + 2)
                }
            }
            AcceptCondition::NoLongerMatch { min_end_gen, .. }
            | AcceptCondition::NeedLongerMatch { min_end_gen, .. } => {
                let root = leaf_root(cond).expect("NLM/NeedLM is a leaf");
                let Some(&fa) = self.first_active.get(&root) else { return g };
                let la = self.last_active[&root];
                if g < fa || g > la {
                    g
                } else if g <= *min_end_gen {
                    (*min_end_gen).min(la)
                } else {
                    g
                }
            }
            _ => g,
        }
    }

    fn compute(&self, cond: &AcceptCondition, from_gen: i32, visiting: &[PathRoot]) -> bool {
        match cond {
            AcceptCondition::Always => true,
            AcceptCondition::Never => false,
            AcceptCondition::And { items } => {
                items.iter().all(|c| self.eval_cond(c, from_gen, visiting))
            }
            AcceptCondition::Or { items } => {
                items.iter().any(|c| self.eval_cond(c, from_gen, visiting))
            }

            AcceptCondition::NoLongerMatch { min_end_gen, .. } => {
                let root = leaf_root(cond).expect("NoLongerMatch is a leaf");
                // evolve: reaching one's own root while visiting makes both
                // NLM and NeedLM Always.
                if visiting.contains(&root) {
                    true
                } else {
                    !self.any_absorbed_fin_true(
                        root,
                        from_gen,
                        *min_end_gen,
                        *min_end_gen + 1,
                        visiting,
                    )
                }
            }
            AcceptCondition::NeedLongerMatch { min_end_gen, .. } => {
                let root = leaf_root(cond).expect("NeedLongerMatch is a leaf");
                if visiting.contains(&root) {
                    true
                } else {
                    self.any_absorbed_fin_true(
                        root,
                        from_gen,
                        *min_end_gen,
                        *min_end_gen + 1,
                        visiting,
                    )
                }
            }
            AcceptCondition::NotExists { .. } => {
                let root = leaf_root(cond).expect("NotExists is a leaf");
                if visiting.contains(&root) {
                    self.eval_cond(cond, from_gen + 1, &[])
                } else {
                    !self.any_absorbed_fin_true(root, from_gen, i32::MIN, i32::MIN, visiting)
                }
            }
            AcceptCondition::Exists { .. } => {
                let root = leaf_root(cond).expect("Exists is a leaf");
                if visiting.contains(&root) {
                    self.eval_cond(cond, from_gen + 1, &[])
                } else {
                    self.any_absorbed_fin_true(root, from_gen, i32::MIN, i32::MIN, visiting)
                }
            }

            AcceptCondition::Unless { end_gen, .. } => {
                let root = leaf_root(cond).expect("Unless is a leaf");
                if visiting.contains(&root) {
                    self.eval_cond(cond, from_gen + 1, &[])
                } else {
                    match self.bounded_fin(root, from_gen, *end_gen) {
                        None => true,
                        Some((fin, obs_gen)) => !self.eval_cond(
                            fin,
                            obs_gen,
                            &fin_visiting(obs_gen, from_gen, visiting, root),
                        ),
                    }
                }
            }
            AcceptCondition::OnlyIf { end_gen, .. } => {
                let root = leaf_root(cond).expect("OnlyIf is a leaf");
                if visiting.contains(&root) {
                    self.eval_cond(cond, from_gen + 1, &[])
                } else {
                    match self.bounded_fin(root, from_gen, *end_gen) {
                        None => false,
                        Some((fin, obs_gen)) => self.eval_cond(
                            fin,
                            obs_gen,
                            &fin_visiting(obs_gen, from_gen, visiting, root),
                        ),
                    }
                }
            }
        }
    }

    /// The finish an Unless/OnlyIf consumes and its observed gen — exactly
    /// the evolve bounded branches:
    ///  - from_gen > end_gen+1: past the consumption point (none).
    ///  - from_gen == end_gen+1: late channel only (that step's
    ///    late_cond_path_finishes; at `history_size` the virtual end-of-input
    ///    late step = end_late_fins).
    ///  - from_gen <= end_gen: eager at end_gen; if absent, wait one step for
    ///    the late fin only while the root is alive at end_gen. An end_gen
    ///    outside the real steps is unobservable.
    fn bounded_fin(
        &self,
        root: PathRoot,
        from_gen: i32,
        end_gen: i32,
    ) -> Option<(&AcceptCondition, i32)> {
        if from_gen > end_gen + 1 {
            return None;
        }
        if from_gen == end_gen + 1 {
            return self.late_fin_at(root, end_gen + 1);
        }
        if end_gen >= self.history_size {
            return None;
        }
        let entry = self.history.get(end_gen as usize).expect("history entry in range");
        if let Some(fin) = entry.cond_path_finishes.get(&root) {
            return Some((fin, end_gen));
        }
        if !entry.active_cond_paths.contains(&root) {
            return None;
        }
        self.late_fin_at(root, end_gen + 1)
    }

    fn late_fin_at(&self, root: PathRoot, g: i32) -> Option<(&AcceptCondition, i32)> {
        let fin = if g < self.history_size {
            self.history.get(g as usize).unwrap().late_cond_path_finishes.get(&root)
        } else if g == self.history_size {
            self.end_late_fins.get(&root)
        } else {
            None
        };
        fin.map(|f| (f, g))
    }

    /// Does any fin absorbed by NLM/NeedLM/NotExists/Exists evaluate true?
    /// `eager_min_gen`/`late_min_gen` carry the min_end_gen clamp (eager fin
    /// end == gen, late fin end == gen-1 → late starts at min_end_gen+1);
    /// NotExists/Exists pass i32::MIN (no clamp).
    fn any_absorbed_fin_true(
        &self,
        root: PathRoot,
        from_gen: i32,
        eager_min_gen: i32,
        late_min_gen: i32,
        visiting: &[PathRoot],
    ) -> bool {
        // Window cap: with the root active at from_gen scan through
        // last_active+1 (fins of the dying step); inactive → only from_gen's
        // own step (the replay drops the pending obligation there).
        let window_end = match self.first_active.get(&root) {
            None => from_gen,
            Some(&fa) => {
                let la = self.last_active[&root];
                if from_gen < fa || from_gen > la {
                    from_gen
                } else {
                    (la + 1).min(self.history_size)
                }
            }
        };
        let real_end = window_end.min(self.history_size - 1);

        if let Some(gens) = self.eager_fin_gens.get(&root) {
            let lo = from_gen.max(eager_min_gen);
            for &g in &gens[gens.partition_point(|&g| g < lo)..] {
                if g > real_end {
                    break;
                }
                let fin = &self.history.get(g as usize).unwrap().cond_path_finishes[&root];
                if self.eval_cond(fin, g, &fin_visiting(g, from_gen, visiting, root)) {
                    return true;
                }
            }
        }
        if let Some(gens) = self.late_fin_gens.get(&root) {
            let lo = from_gen.max(late_min_gen);
            for &g in &gens[gens.partition_point(|&g| g < lo)..] {
                if g > real_end {
                    break;
                }
                let fin = &self.history.get(g as usize).unwrap().late_cond_path_finishes[&root];
                if self.eval_cond(fin, g, &fin_visiting(g, from_gen, visiting, root)) {
                    return true;
                }
            }
        }
        // Virtual end-of-input late step (gen == history_size).
        if window_end >= self.history_size && self.history_size >= from_gen.max(late_min_gen) {
            if let Some(fin) = self.end_late_fins.get(&root) {
                let v = fin_visiting(self.history_size, from_gen, visiting, root);
                if self.eval_cond(fin, self.history_size, &v) {
                    return true;
                }
            }
        }
        false
    }
}

/// Visiting set for a fin's recursive evaluation: absorption at the same step
/// (from_gen) accumulates onto the existing set; a later step corresponds to
/// the replay's fresh per-step visiting — just {root}. Kept sorted so equal
/// sets hash equally in the memo key (the guard rules out duplicates).
fn fin_visiting(obs_gen: i32, from_gen: i32, visiting: &[PathRoot], root: PathRoot) -> Vec<PathRoot> {
    if obs_gen == from_gen {
        let mut v = visiting.to_vec();
        let pos = v.partition_point(|r| *r < root);
        v.insert(pos, root);
        v
    } else {
        vec![root]
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::parsing_ctx::HistoryEntry;

    fn root(sym: i32, sg: i32) -> PathRoot {
        PathRoot::new(sym, sg)
    }
    /// A leaf condition that references `r` (any leaf variant works; the graph
    /// uses `leaf_root`, which maps all leaves to `(symbol_id, start_gen)`).
    fn refs(r: PathRoot) -> AcceptCondition {
        AcceptCondition::Exists { symbol_id: r.symbol_id, start_gen: r.start_gen }
    }
    fn entry(active: &[PathRoot], fins: &[(PathRoot, AcceptCondition)]) -> HistoryEntry {
        let mut e = HistoryEntry::default();
        e.active_cond_paths = active.iter().copied().collect();
        for (r, c) in fins {
            e.cond_path_finishes.insert(*r, c.clone());
        }
        e
    }
    fn history(entries: Vec<HistoryEntry>) -> History {
        let mut h = History::new();
        for e in entries {
            h.push(e);
        }
        h
    }
    fn d_set(h: &History, resume: i32) -> HashSet<PathRoot> {
        let iv = active_intervals(h);
        let empty = HashMap::default();
        dangerous_roots(h, &empty, &iv, resume)
    }
    fn set(items: &[PathRoot]) -> HashSet<PathRoot> {
        items.iter().copied().collect()
    }

    #[test]
    fn leaf_root_maps_every_leaf() {
        let r = root(3, 7);
        for c in [
            AcceptCondition::NoLongerMatch { symbol_id: 3, start_gen: 7, min_end_gen: 9 },
            AcceptCondition::NeedLongerMatch { symbol_id: 3, start_gen: 7, min_end_gen: 9 },
            AcceptCondition::NotExists { symbol_id: 3, start_gen: 7 },
            AcceptCondition::Exists { symbol_id: 3, start_gen: 7 },
            AcceptCondition::Unless { symbol_id: 3, start_gen: 7, end_gen: 9 },
            AcceptCondition::OnlyIf { symbol_id: 3, start_gen: 7, end_gen: 9 },
        ] {
            assert_eq!(leaf_root(&c), Some(r), "leaf {c:?}");
        }
        assert_eq!(leaf_root(&AcceptCondition::Always), None);
        assert_eq!(leaf_root(&AcceptCondition::Never), None);
        // Composite: no leaf root itself, but collect_referenced_roots unions.
        let comp = AcceptCondition::and_from([refs(root(1, 0)), refs(root(2, 0))]);
        assert_eq!(leaf_root(&comp), None);
        let mut refs_out = HashSet::default();
        collect_referenced_roots(&comp, &mut refs_out);
        assert_eq!(refs_out, set(&[root(1, 0), root(2, 0)]));
    }

    #[test]
    fn dangerous_direct_seed_only() {
        // r_d active past resume; nothing references anything → D = {r_d}.
        let r_d = root(9, 0);
        let r_safe = root(1, 0); // active only early, no edges
        let h = history(vec![
            entry(&[], &[]),
            entry(&[r_safe], &[]),
            entry(&[], &[]),
            entry(&[r_d], &[]),
        ]);
        assert_eq!(d_set(&h, 3), set(&[r_d]));
    }

    #[test]
    fn dangerous_one_step_reference() {
        // r_safe finishes referencing r_d (dangerous) → both dangerous.
        let r_d = root(9, 0);
        let r_safe = root(1, 0);
        let h = history(vec![
            entry(&[], &[]),
            entry(&[r_safe], &[(r_safe, refs(r_d))]),
            entry(&[], &[]),
            entry(&[r_d], &[]),
        ]);
        assert_eq!(d_set(&h, 3), set(&[r_safe, r_d]));
    }

    #[test]
    fn dangerous_two_step_chain() {
        // r_a → r_b → r_c, r_c dangerous. All three in D.
        let (r_a, r_b, r_c) = (root(1, 0), root(2, 0), root(9, 0));
        let h = history(vec![
            entry(&[], &[]),
            entry(&[r_a, r_b], &[(r_a, refs(r_b)), (r_b, refs(r_c))]),
            entry(&[], &[]),
            entry(&[r_c], &[]),
        ]);
        assert_eq!(d_set(&h, 3), set(&[r_a, r_b, r_c]));
    }

    #[test]
    fn dangerous_safe_chain_excluded() {
        // r_a → r_b, neither reaches a dangerous root → D empty.
        let (r_a, r_b) = (root(1, 0), root(2, 0));
        let h = history(vec![
            entry(&[], &[]),
            entry(&[r_a, r_b], &[(r_a, refs(r_b))]),
            entry(&[], &[]),
            entry(&[], &[]),
        ]);
        assert!(d_set(&h, 3).is_empty());
    }

    #[test]
    fn dangerous_cycle_no_d0_is_empty() {
        // r_x ⇄ r_y cycle, neither active past resume, no D0 → terminates empty.
        let (r_x, r_y) = (root(1, 0), root(2, 0));
        let h = history(vec![
            entry(&[], &[]),
            entry(&[r_x, r_y], &[(r_x, refs(r_y)), (r_y, refs(r_x))]),
            entry(&[], &[]),
            entry(&[], &[]),
        ]);
        assert!(d_set(&h, 3).is_empty());
    }

    #[test]
    fn dangerous_cycle_reaching_d0() {
        // r_x ⇄ r_y cycle; r_y also → r_z (dangerous). Cycle must terminate and
        // pull in the whole cycle plus r_z.
        let (r_x, r_y, r_z) = (root(1, 0), root(2, 0), root(9, 0));
        let h = history(vec![
            entry(&[], &[]),
            entry(&[r_x, r_y], &[(r_x, refs(r_y)), (r_y, refs(r_x))]),
            entry(&[r_x, r_y], &[(r_y, refs(r_z))]),
            entry(&[], &[]),
            entry(&[r_z], &[]),
        ]);
        assert_eq!(d_set(&h, 4), set(&[r_x, r_y, r_z]));
    }

    #[test]
    fn dangerous_end_late_edges_followed() {
        // An edge that exists ONLY in end_late_fins (a root alive at input end):
        // r_e → r_d via the end-of-input late sweep. r_d dangerous → r_e too.
        let (r_e, r_d) = (root(1, 0), root(9, 0));
        let h = history(vec![entry(&[], &[]), entry(&[], &[]), entry(&[r_d], &[])]);
        let iv = active_intervals(&h);
        let mut end_late: HashMap<PathRoot, AcceptCondition> = HashMap::default();
        end_late.insert(r_e, refs(r_d));
        let d = dangerous_roots(&h, &end_late, &iv, 2);
        assert_eq!(d, set(&[r_e, r_d]));
    }
}
