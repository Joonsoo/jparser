//! Gen-rebase for splice reuse (Phase I2) — an ADDITIVE, read-only-input helper.
//!
//! When the incremental session (`session.rs`) detects that the edited parse's
//! STRICT state at gen `q*` is shift-equivalent to the previous parse's state at
//! gen `q*-delta`, it reuses the OLD parse's suffix (final ctx + history segment)
//! instead of re-parsing it. Reuse requires mapping every absolute gen embedded
//! in that old suffix into the new document's gen space.
//!
//! ## The split mapping (design §2.2, review correction — NOT a uniform +delta)
//!
//! An edit at char position `p` shifts everything AFTER `p` by
//! `delta = new_len - old_len`, but leaves the prefix `[0..=p]` unchanged. The
//! suffix state carries gens anchored BOTH before the edit (long-lived watchers /
//! chain-root nodes born in the unchanged prefix) and after it. So the mapping is
//! per-gen, not uniform:
//!
//! ```text
//!   map_gen(g) = if g <= p { g }            // prefix anchor — unchanged
//!                else       { g + delta }    // post-edit — shifted
//! ```
//!
//! This is *exactly* the anchor split the convergence fingerprint used
//! (`fingerprint::enc_gen`: `g <= p` absolute, `g > p` offset). Because the
//! fingerprint judged equivalence under this split, rebasing under the SAME split
//! makes the reused old state byte-identical to the state a full re-parse would
//! have produced. A uniform `+delta` would wrongly shove prefix anchors forward
//! and desync condition evaluation.
//!
//! ## Field-unit, not entry-unit
//!
//! A single `HistoryEntry` or ctx node can mix prefix-anchored gens (e.g. a
//! watcher `PathRoot.start_gen` born before the edit) and post-edit gens (its
//! finish coordinates). So the mapping is applied per gen FIELD, never wholesale
//! per record. Every gen-bearing field is enumerated below and in `rebase_entry`.
//!
//! ## Complete gen-field inventory (audited against parsing_ctx.rs + core.rs)
//!
//! `HistoryEntry` (all consumed by `kernels_history` / `is_accepted` /
//! `RecordConditionEvaluator`):
//!   - `action_applications: Vec<ActionApplication>`, each carrying SEVEN
//!     absolute-gen fields — `rt_curr`, `rt_mid`, `next`, `rt_grand`, `rep_curr`,
//!     `rep_mid`, `rep_grand` — plus `root: PathRoot` (`.start_gen`) and
//!     `condition: AcceptCondition` (leaf gens). (`actions` is a static template
//!     `Arc`; NOT gen-bearing — the template's gen *tags* were already resolved
//!     into the seven fields above at parse time.)
//!   - `finished_kernels: Vec<FinishedKernelRecord>` — `kernel.gen_idx` (the
//!     begin gen; `kernel.symbol_id`/`pointer` are static), `condition` leaf gens,
//!     `root.start_gen`.
//!   - `added_kernels: Vec<AddedKernelRecord>` — `begin_gen`, `end_gen`,
//!     `condition` leaf gens, `root.start_gen`.
//!   - `cond_path_finishes: HashMap<PathRoot, AcceptCondition>` — key
//!     `PathRoot.start_gen` + value condition leaf gens.
//!   - `late_cond_path_finishes: HashMap<PathRoot, AcceptCondition>` — same.
//!   - `active_cond_paths: HashSet<PathRoot>` — each `.start_gen`.
//!   - `main_root_finish: Option<AcceptCondition>` — leaf gens.
//!   - `reported_cond_roots: HashSet<PathRoot>` — each `.start_gen`.
//!
//! `AcceptCondition` leaf gens: `start_gen`, `min_end_gen`, `end_gen` (whichever
//! the leaf carries). Composites (`And`/`Or`) are stored in CANONICAL SORTED
//! order; rebasing leaf gens can change the sort key, so composites are rebuilt
//! via `and_from` / `or_from` to re-canonicalize (keeps the exact Vec order a
//! full re-parse would produce — required for byte-identical memo/equality).
//!
//! `ParsingCtx` live state (final ctx, for accept / kernels_history):
//!   - `gen_idx` — the current gen; mapped (always post-edit for a converged
//!     final ctx, but mapped through the split for uniformity).
//!   - `main_root: PathRoot` (`.start_gen`) — the start root, born at gen 0 (a
//!     prefix anchor; maps to itself, but mapped through the split for
//!     uniformity).
//!   - `paths: HashMap<PathRoot, PathMap>` — keys `PathRoot.start_gen`; values
//!     are `PathMap = HashMap<PathShape, AcceptCondition>` where `PathShape`
//!     holds an `Rc<MilestonePath>` chain (each node: `gen_idx`, `report_gen`,
//!     `milestone_report_gen`, `milestone.gen_idx`) and the condition holds leaf
//!     gens. The `Rc` chain has gens baked into each node, so it cannot be shared
//!     across the rebase — a fresh chain is rebuilt (O(state), once per splice).
//!   - `ever_seen_cond_roots: HashSet<PathRoot>` — each `.start_gen`.
//!   - `root_report_gens: HashMap<PathRoot, i32>` — key `PathRoot.start_gen` AND
//!     value (a report gen). Both mapped.
//!   - `history` — handled separately by the session's lazy materialization
//!     (`rebase_entry` per old-suffix entry).
//!   - `term_action_cache`, `step_scratch` — parse-local scratch, NOT gen-bearing
//!     in a way that affects output; reset to `Default` on the rebased final ctx
//!     (they are rebuilt on demand and never read by kernels_history/is_accepted).

use std::rc::Rc;

use rustc_hash::{FxHashMap as HashMap, FxHashSet as HashSet};

use crate::accept_condition::AcceptCondition;
use crate::history::History;
use crate::parsing_ctx::{
    ActionApplication, AddedKernelRecord, FinishedKernelRecord, HistoryEntry, Kernel,
    MilestonePath, ParsingCtx, PathMap, PathShape,
};
use crate::path_root::PathRoot;

/// The split gen mapping. `p` is the edit position (a gen boundary == the edit's
/// starting char index); `delta = new_len - old_len`.
#[derive(Clone, Copy, Debug)]
pub struct GenRebase {
    pub p: i32,
    pub delta: i32,
}

impl GenRebase {
    #[inline]
    pub fn new(p: i32, delta: i32) -> Self {
        Self { p, delta }
    }

    /// Map one absolute gen: `g <= p` unchanged, `g > p` shifted by delta.
    #[inline]
    pub fn map(&self, g: i32) -> i32 {
        if g <= self.p {
            g
        } else {
            g + self.delta
        }
    }

    #[inline]
    fn root(&self, r: PathRoot) -> PathRoot {
        PathRoot::new(r.symbol_id, self.map(r.start_gen))
    }

    #[inline]
    fn kernel(&self, k: Kernel) -> Kernel {
        // symbol_id/pointer are static; only gen_idx is a coordinate.
        Kernel::new(k.symbol_id, k.pointer, self.map(k.gen_idx))
    }

    /// Rebase an `AcceptCondition`. Composites are rebuilt via `and_from`/`or_from`
    /// so the canonical sort order matches a full re-parse (rebasing a leaf's gen
    /// can change its sort key).
    pub fn cond(&self, c: &AcceptCondition) -> AcceptCondition {
        use AcceptCondition::*;
        match c {
            Always => Always,
            Never => Never,
            NoLongerMatch { symbol_id, start_gen, min_end_gen } => NoLongerMatch {
                symbol_id: *symbol_id,
                start_gen: self.map(*start_gen),
                min_end_gen: self.map(*min_end_gen),
            },
            NeedLongerMatch { symbol_id, start_gen, min_end_gen } => NeedLongerMatch {
                symbol_id: *symbol_id,
                start_gen: self.map(*start_gen),
                min_end_gen: self.map(*min_end_gen),
            },
            NotExists { symbol_id, start_gen } => {
                NotExists { symbol_id: *symbol_id, start_gen: self.map(*start_gen) }
            }
            Exists { symbol_id, start_gen } => {
                Exists { symbol_id: *symbol_id, start_gen: self.map(*start_gen) }
            }
            Unless { symbol_id, start_gen, end_gen } => Unless {
                symbol_id: *symbol_id,
                start_gen: self.map(*start_gen),
                end_gen: self.map(*end_gen),
            },
            OnlyIf { symbol_id, start_gen, end_gen } => OnlyIf {
                symbol_id: *symbol_id,
                start_gen: self.map(*start_gen),
                end_gen: self.map(*end_gen),
            },
            And { items } => AcceptCondition::and_from(items.iter().map(|c| self.cond(c))),
            Or { items } => AcceptCondition::or_from(items.iter().map(|c| self.cond(c))),
        }
    }

    fn action_application(&self, app: &ActionApplication) -> ActionApplication {
        ActionApplication {
            actions: std::sync::Arc::clone(&app.actions),
            root: self.root(app.root),
            rt_curr: self.map(app.rt_curr),
            rt_mid: self.map(app.rt_mid),
            next: self.map(app.next),
            rt_grand: self.map(app.rt_grand),
            rep_curr: self.map(app.rep_curr),
            rep_mid: self.map(app.rep_mid),
            rep_grand: self.map(app.rep_grand),
            condition: self.cond(&app.condition),
        }
    }

    fn finished_kernel(&self, r: &FinishedKernelRecord) -> FinishedKernelRecord {
        FinishedKernelRecord {
            kernel: self.kernel(r.kernel),
            condition: self.cond(&r.condition),
            root: self.root(r.root),
        }
    }

    fn added_kernel(&self, r: &AddedKernelRecord) -> AddedKernelRecord {
        AddedKernelRecord {
            symbol_id: r.symbol_id,
            pointer: r.pointer,
            begin_gen: self.map(r.begin_gen),
            end_gen: self.map(r.end_gen),
            condition: self.cond(&r.condition),
            root: self.root(r.root),
        }
    }

    fn root_cond_map(
        &self,
        m: &HashMap<PathRoot, AcceptCondition>,
    ) -> HashMap<PathRoot, AcceptCondition> {
        m.iter().map(|(r, c)| (self.root(*r), self.cond(c))).collect()
    }

    fn root_set(&self, s: &HashSet<PathRoot>) -> HashSet<PathRoot> {
        s.iter().map(|r| self.root(*r)).collect()
    }

    /// Rebase a whole `HistoryEntry` (every gen field per the inventory above).
    pub fn entry(&self, e: &HistoryEntry) -> HistoryEntry {
        HistoryEntry {
            action_applications: e
                .action_applications
                .iter()
                .map(|a| self.action_application(a))
                .collect(),
            finished_kernels: e.finished_kernels.iter().map(|r| self.finished_kernel(r)).collect(),
            added_kernels: e.added_kernels.iter().map(|r| self.added_kernel(r)).collect(),
            cond_path_finishes: self.root_cond_map(&e.cond_path_finishes),
            late_cond_path_finishes: self.root_cond_map(&e.late_cond_path_finishes),
            active_cond_paths: self.root_set(&e.active_cond_paths),
            main_root_finish: e.main_root_finish.as_ref().map(|c| self.cond(c)),
            reported_cond_roots: self.root_set(&e.reported_cond_roots),
        }
    }

    // -- live-state (final ctx) rebase -------------------------------------

    /// Rebuild a `MilestonePath` chain with every node's gens mapped. The `Rc`
    /// chain bakes gens into each node, so we cannot share the old chain — a fresh
    /// chain is built bottom-up. A memo dedups shared ancestors (chains share
    /// parents heavily via `Rc`), keeping this O(distinct nodes).
    fn milestone_path(
        &self,
        node: &Option<Rc<MilestonePath>>,
        memo: &mut HashMap<usize, Rc<MilestonePath>>,
    ) -> Option<Rc<MilestonePath>> {
        let node = node.as_ref()?;
        let key = Rc::as_ptr(node) as usize;
        if let Some(existing) = memo.get(&key) {
            return Some(Rc::clone(existing));
        }
        let parent = self.milestone_path(&node.parent, memo);
        let rebuilt = Rc::new(MilestonePath::new(
            self.map(node.gen_idx),
            self.kernel(node.milestone),
            parent,
            std::sync::Arc::clone(&node.observing_cond_symbol_ids),
            self.map(node.report_gen),
            self.map(node.milestone_report_gen),
        ));
        memo.insert(key, Rc::clone(&rebuilt));
        Some(rebuilt)
    }

    fn path_shape(
        &self,
        shape: &PathShape,
        memo: &mut HashMap<usize, Rc<MilestonePath>>,
    ) -> PathShape {
        PathShape::new(self.milestone_path(&shape.milestone_path, memo), shape.tip_group_id)
    }

    fn path_map(
        &self,
        pm: &PathMap,
        memo: &mut HashMap<usize, Rc<MilestonePath>>,
    ) -> PathMap {
        let mut out = PathMap::default();
        for (shape, cond) in pm.iter() {
            // Rebasing can make two distinct old shapes map to the same new shape
            // (only if their gen fields collapse under the split — not expected on
            // a converged suffix, but Or-merge to be safe and output-invariant,
            // exactly as `add_path` would during a real parse).
            crate::parsing_ctx::add_path(&mut out, self.path_shape(shape, memo), self.cond(cond));
        }
        out
    }

    /// Rebase a whole final `ParsingCtx` live state (design §2.2-a): one O(state)
    /// reconstruction at the splice point. `history` is REPLACED by the caller
    /// with the materialized spliced history; `term_action_cache`/`step_scratch`
    /// are reset (parse-local, rebuilt on demand, never read by the consumers).
    pub fn ctx(&self, ctx: &ParsingCtx, spliced_history: History) -> ParsingCtx {
        let mut memo: HashMap<usize, Rc<MilestonePath>> = HashMap::default();
        let mut paths: HashMap<PathRoot, PathMap> = HashMap::default();
        for (root, pm) in ctx.paths.iter() {
            paths.insert(self.root(*root), self.path_map(pm, &mut memo));
        }
        let root_report_gens: HashMap<PathRoot, i32> =
            ctx.root_report_gens.iter().map(|(r, g)| (self.root(*r), self.map(*g))).collect();
        ParsingCtx {
            gen_idx: self.map(ctx.gen_idx),
            // line/col are copied from the OLD parse's final ctx and may be stale
            // when the edit changed the number of newlines before the suffix. They
            // are error-message coordinates only — never read by kernels_history /
            // is_accepted (the gated outputs) — and a spliced result is by
            // construction a completed parse, so no error path consumes them.
            // If a future consumer needs exact line/col on spliced results,
            // recompute from the session's current document instead.
            line: ctx.line,
            col: ctx.col,
            main_root: self.root(ctx.main_root),
            paths,
            history: spliced_history,
            ever_seen_cond_roots: self.root_set(&ctx.ever_seen_cond_roots),
            root_report_gens,
            term_action_cache: Default::default(),
            step_scratch: Default::default(),
        }
    }
}

// ---------------------------------------------------------------------------
// Structural equality under a rebase (splice verification, design §2.1/§2.5).
//
// The convergence fingerprint is a 128-bit hash; a collision would splice a
// state that only *looks* equivalent. Before committing a splice the session
// verifies the OLD state at `q*-delta`, rebased into the new gen space, is
// STRUCTURALLY IDENTICAL to the freshly-parsed NEW state at `q*`. If not, it
// abandons the splice and keeps parsing (correctness preserved).
//
// We compare the two live `paths` maps for exact equality after rebasing the old
// one. `PathRoot`/`PathShape`/`MilestonePath`/`AcceptCondition` all derive/impl
// structural `Eq` (MilestonePath includes report-shadow gens via rebuilt nodes;
// PathShape/PathRoot compare structurally). Equality of the two HashMaps is
// order-free and value-exact — precisely the "structural full match" the design
// calls for.
// ---------------------------------------------------------------------------

/// True iff `old_ctx.paths`, rebased into the new gen space, equals
/// `new_ctx.paths` exactly. Used as the one-shot collision guard before a splice.
pub fn paths_match_after_rebase(
    rebase: &GenRebase,
    old_ctx: &ParsingCtx,
    new_ctx: &ParsingCtx,
) -> bool {
    if old_ctx.paths.len() != new_ctx.paths.len() {
        return false;
    }
    let mut memo: HashMap<usize, Rc<MilestonePath>> = HashMap::default();
    for (root, pm) in old_ctx.paths.iter() {
        let mapped_root = rebase.root(*root);
        let Some(new_pm) = new_ctx.paths.get(&mapped_root) else {
            return false;
        };
        let rebased_pm = rebase.path_map(pm, &mut memo);
        if rebased_pm.len() != new_pm.len() {
            return false;
        }
        for (shape, cond) in rebased_pm.iter() {
            match new_pm.get(shape) {
                Some(c) if c == cond => {}
                _ => return false,
            }
        }
    }
    true
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn map_split() {
        let r = GenRebase::new(5, 2);
        assert_eq!(r.map(0), 0);
        assert_eq!(r.map(5), 5); // <= p unchanged
        assert_eq!(r.map(6), 8); // > p shifted
        assert_eq!(r.map(10), 12);
    }

    #[test]
    fn map_negative_delta() {
        let r = GenRebase::new(3, -2);
        assert_eq!(r.map(3), 3);
        assert_eq!(r.map(4), 2);
        assert_eq!(r.map(10), 8);
    }

    #[test]
    fn cond_leaf_split() {
        let r = GenRebase::new(4, 3);
        // start_gen=2 (<=p) stays, end_gen=6 (>p) shifts to 9.
        let c = AcceptCondition::Unless { symbol_id: 7, start_gen: 2, end_gen: 6 };
        assert_eq!(
            r.cond(&c),
            AcceptCondition::Unless { symbol_id: 7, start_gen: 2, end_gen: 9 }
        );
    }

    #[test]
    fn cond_composite_recanonicalizes() {
        let r = GenRebase::new(0, 5);
        // Two leaves whose relative sort order can flip after shifting.
        let a = AcceptCondition::Exists { symbol_id: 1, start_gen: 1 };
        let b = AcceptCondition::Exists { symbol_id: 1, start_gen: 2 };
        let original = AcceptCondition::or_from([a.clone(), b.clone()]);
        let rebased = r.cond(&original);
        // Rebasing then re-canonicalizing must equal building from the rebased
        // leaves directly.
        let expected = AcceptCondition::or_from([r.cond(&a), r.cond(&b)]);
        assert_eq!(rebased, expected);
    }

    #[test]
    fn root_split() {
        let r = GenRebase::new(3, 4);
        assert_eq!(r.root(PathRoot::new(9, 1)), PathRoot::new(9, 1));
        assert_eq!(r.root(PathRoot::new(9, 5)), PathRoot::new(9, 9));
    }
}
