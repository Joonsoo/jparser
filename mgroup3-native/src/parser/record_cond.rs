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
use std::sync::OnceLock;

use rustc_hash::FxHashMap as HashMap;

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

pub struct RecordConditionEvaluator<'a> {
    history: &'a History,
    end_late_fins: &'a HashMap<PathRoot, AcceptCondition>,
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
        end_late_fins: &'a HashMap<PathRoot, AcceptCondition>,
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
        let result = self.eval_cond(cond, record_gen, &[]);
        if diff_check_enabled() {
            let replayed =
                evaluate_record_condition(cond, self.history, record_gen, self.end_late_fins);
            assert_eq!(
                result, replayed,
                "RecordConditionEvaluator mismatch: record_gen={record_gen} cond={cond}"
            );
        }
        result
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
            AcceptCondition::NoLongerMatch { symbol_id, start_gen, min_end_gen }
            | AcceptCondition::NeedLongerMatch { symbol_id, start_gen, min_end_gen } => {
                let root = PathRoot::new(*symbol_id, *start_gen);
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

            AcceptCondition::NoLongerMatch { symbol_id, start_gen, min_end_gen } => {
                let root = PathRoot::new(*symbol_id, *start_gen);
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
            AcceptCondition::NeedLongerMatch { symbol_id, start_gen, min_end_gen } => {
                let root = PathRoot::new(*symbol_id, *start_gen);
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
            AcceptCondition::NotExists { symbol_id, start_gen } => {
                let root = PathRoot::new(*symbol_id, *start_gen);
                if visiting.contains(&root) {
                    self.eval_cond(cond, from_gen + 1, &[])
                } else {
                    !self.any_absorbed_fin_true(root, from_gen, i32::MIN, i32::MIN, visiting)
                }
            }
            AcceptCondition::Exists { symbol_id, start_gen } => {
                let root = PathRoot::new(*symbol_id, *start_gen);
                if visiting.contains(&root) {
                    self.eval_cond(cond, from_gen + 1, &[])
                } else {
                    self.any_absorbed_fin_true(root, from_gen, i32::MIN, i32::MIN, visiting)
                }
            }

            AcceptCondition::Unless { symbol_id, start_gen, end_gen } => {
                let root = PathRoot::new(*symbol_id, *start_gen);
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
            AcceptCondition::OnlyIf { symbol_id, start_gen, end_gen } => {
                let root = PathRoot::new(*symbol_id, *start_gen);
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
    ) -> Option<(&'a AcceptCondition, i32)> {
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

    fn late_fin_at(&self, root: PathRoot, g: i32) -> Option<(&'a AcceptCondition, i32)> {
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
