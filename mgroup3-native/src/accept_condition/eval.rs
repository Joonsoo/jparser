//! `evaluate_accept_condition` and `evolve_accept_condition`.
//!
//! Mirror of Kotlin `AcceptCondition.kt` after the exact-span discharge fix:
//! - `evaluate_accept_condition`: pure walker. No history; per-leaf table reads
//!   from `cond_path_fins`.
//! - `evolve_accept_condition`: per-generation rewrite. Two finish channels:
//!   `cond_path_fins` (eager — finishes ending at `gen_idx`) and
//!   `late_cond_path_fins` (registered by dying cond paths — finishes ending
//!   at `gen_idx - 1`). Bounded shapes (Unless/OnlyIf) discharge only at their
//!   exact `end_gen` (eager at `gen == end`, late at `gen == end + 1`);
//!   NoLongerMatch/NeedLongerMatch absorb only finishes with end >= min_end_gen
//!   and keep their pending obligation while the root lives; NotExists/Exists
//!   absorb both channels, the cumulative `seen_cond_path_fins` record, and
//!   stay pending while the root lives.

use rustc_hash::{FxHashMap as HashMap, FxHashSet as HashSet};

use crate::path_root::PathRoot;

use super::AcceptCondition;

/// Pure walker — `condPathFins` is the only oracle. Used at end-of-input by
/// callers that don't have `HistoryEntry` available. (The history-aware variant
/// lives in the parser module, not here.)
pub fn evaluate_accept_condition(
    cond: &AcceptCondition,
    cond_path_fins: &HashMap<PathRoot, AcceptCondition>,
    active_cond_paths: &HashSet<PathRoot>,
) -> bool {
    match cond {
        AcceptCondition::Always => true,
        AcceptCondition::Never => false,
        AcceptCondition::And { items } => items
            .iter()
            .all(|c| evaluate_accept_condition(c, cond_path_fins, active_cond_paths)),
        AcceptCondition::Or { items } => items
            .iter()
            .any(|c| evaluate_accept_condition(c, cond_path_fins, active_cond_paths)),

        AcceptCondition::NoLongerMatch { symbol_id, start_gen, .. }
        | AcceptCondition::NotExists { symbol_id, start_gen }
        | AcceptCondition::Unless { symbol_id, start_gen, .. } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            match cond_path_fins.get(&root) {
                Some(fin) => !evaluate_accept_condition(fin, cond_path_fins, active_cond_paths),
                None => true,
            }
        }
        AcceptCondition::NeedLongerMatch { symbol_id, start_gen, .. }
        | AcceptCondition::Exists { symbol_id, start_gen }
        | AcceptCondition::OnlyIf { symbol_id, start_gen, .. } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            match cond_path_fins.get(&root) {
                Some(fin) => evaluate_accept_condition(fin, cond_path_fins, active_cond_paths),
                None => false,
            }
        }
    }
}

/// Public entry. See module docs for the two finish channels and per-shape
/// discharge rules.
///
/// `seen_cond_path_fins`: cumulative record of cond-root finishes observed in
/// *earlier* gens (root → Or-merged finish condition, itself evolved every step
/// from its observation gen so it is current-gen relative). NotExists/Exists
/// ONLY. Kotlin defaults this to `emptyMap()`; Rust has no default arguments, so
/// callers with no cumulative map pass `&HashMap::default()`.
///
/// Why it exists (bug B): the truth value of an unbounded lookahead condition
/// `NotExists(b, b, X)` is a function of `(X, b)` alone — "does a match of X
/// starting at b exist (any end)" — independent of *when* the condition instance
/// was created. But in the milestone family a condition only materializes on the
/// step the dot passes the conditional kernel (interior kernels are folded into
/// the group closure, not milestones), so a leaf legitimately gets born *after*
/// the watcher it guards has finished and died. Looking only at the per-step
/// channels, such a leaf sees "no finish this step + root inactive" and is
/// mis-resolved to `Always`, silently dropping the lookahead.
///
/// Bounded (`Unless`/`OnlyIf`) and longest (`NoLongerMatch`/`NeedLongerMatch`)
/// must discharge on the *exact* span's finish, so they never consult it.
pub fn evolve_accept_condition(
    cond: &AcceptCondition,
    cond_path_fins: &HashMap<PathRoot, AcceptCondition>,
    late_cond_path_fins: &HashMap<PathRoot, AcceptCondition>,
    active_cond_paths: &HashSet<PathRoot>,
    gen_idx: i32,
    seen_cond_path_fins: &HashMap<PathRoot, AcceptCondition>,
) -> AcceptCondition {
    let visiting = HashSet::default();
    evolve_inner(
        cond,
        cond_path_fins,
        late_cond_path_fins,
        active_cond_paths,
        gen_idx,
        seen_cond_path_fins,
        &visiting,
    )
}

#[allow(clippy::too_many_arguments)]
fn evolve_inner(
    cond: &AcceptCondition,
    cond_path_fins: &HashMap<PathRoot, AcceptCondition>,
    late_cond_path_fins: &HashMap<PathRoot, AcceptCondition>,
    active_cond_paths: &HashSet<PathRoot>,
    gen_idx: i32,
    seen_cond_path_fins: &HashMap<PathRoot, AcceptCondition>,
    visiting: &HashSet<PathRoot>,
) -> AcceptCondition {
    let rec = |c: &AcceptCondition, v: &HashSet<PathRoot>| {
        evolve_inner(
            c,
            cond_path_fins,
            late_cond_path_fins,
            active_cond_paths,
            gen_idx,
            seen_cond_path_fins,
            v,
        )
    };
    match cond {
        AcceptCondition::Always | AcceptCondition::Never => cond.clone(),
        AcceptCondition::And { items } => {
            let evolved: Vec<_> = items.iter().map(|c| rec(c, visiting)).collect();
            AcceptCondition::and_from(evolved)
        }
        AcceptCondition::Or { items } => {
            let evolved: Vec<_> = items.iter().map(|c| rec(c, visiting)).collect();
            AcceptCondition::or_from(evolved)
        }

        // longest: only matches strictly longer than the body (end >= min_end_gen)
        // violate the condition. Eager fins end at gen_idx, late fins at gen_idx-1.
        // Keep the pending obligation while the root lives.
        AcceptCondition::NoLongerMatch { symbol_id, start_gen, min_end_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            if visiting.contains(&root) {
                return AcceptCondition::Always;
            }
            let mut parts: Vec<AcceptCondition> = Vec::new();
            if gen_idx >= *min_end_gen {
                if let Some(fin) = cond_path_fins.get(&root) {
                    let nv = with_added(visiting, root);
                    parts.push(rec(&fin.neg(), &nv));
                }
            }
            if gen_idx - 1 >= *min_end_gen {
                if let Some(fin) = late_cond_path_fins.get(&root) {
                    let nv = with_added(visiting, root);
                    parts.push(rec(&fin.neg(), &nv));
                }
            }
            if active_cond_paths.contains(&root) {
                parts.push(cond.clone());
            }
            if parts.is_empty() {
                AcceptCondition::Always
            } else {
                AcceptCondition::and_from(parts)
            }
        }

        AcceptCondition::NeedLongerMatch { symbol_id, start_gen, min_end_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            if visiting.contains(&root) {
                return AcceptCondition::Always;
            }
            let mut parts: Vec<AcceptCondition> = Vec::new();
            if gen_idx >= *min_end_gen {
                if let Some(fin) = cond_path_fins.get(&root) {
                    let nv = with_added(visiting, root);
                    parts.push(rec(fin, &nv));
                }
            }
            if gen_idx - 1 >= *min_end_gen {
                if let Some(fin) = late_cond_path_fins.get(&root) {
                    let nv = with_added(visiting, root);
                    parts.push(rec(fin, &nv));
                }
            }
            if active_cond_paths.contains(&root) {
                parts.push(cond.clone());
            }
            if parts.is_empty() {
                AcceptCondition::Never
            } else {
                AcceptCondition::or_from(parts)
            }
        }

        // lookahead: any end counts — absorb both per-step channels AND the
        // cumulative record of finishes already observed in *earlier* gens
        // (`seen_cond_path_fins`; the condition can materialize after the watcher
        // died — bug B). Keep pending while the root lives.
        AcceptCondition::NotExists { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            if visiting.contains(&root) {
                return cond.clone();
            }
            let mut parts: Vec<AcceptCondition> = Vec::new();
            if let Some(fin) = cond_path_fins.get(&root) {
                let nv = with_added(visiting, root);
                parts.push(rec(&fin.neg(), &nv));
            }
            if let Some(fin) = late_cond_path_fins.get(&root) {
                let nv = with_added(visiting, root);
                parts.push(rec(&fin.neg(), &nv));
            }
            if let Some(fin) = seen_cond_path_fins.get(&root) {
                let nv = with_added(visiting, root);
                parts.push(rec(&fin.neg(), &nv));
            }
            if active_cond_paths.contains(&root) {
                parts.push(cond.clone());
            }
            if parts.is_empty() {
                AcceptCondition::Always
            } else {
                AcceptCondition::and_from(parts)
            }
        }

        AcceptCondition::Exists { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            if visiting.contains(&root) {
                return cond.clone();
            }
            let mut parts: Vec<AcceptCondition> = Vec::new();
            if let Some(fin) = cond_path_fins.get(&root) {
                let nv = with_added(visiting, root);
                parts.push(rec(fin, &nv));
            }
            if let Some(fin) = late_cond_path_fins.get(&root) {
                let nv = with_added(visiting, root);
                parts.push(rec(fin, &nv));
            }
            if let Some(fin) = seen_cond_path_fins.get(&root) {
                let nv = with_added(visiting, root);
                parts.push(rec(fin, &nv));
            }
            if active_cond_paths.contains(&root) {
                parts.push(cond.clone());
            }
            if parts.is_empty() {
                AcceptCondition::Never
            } else {
                AcceptCondition::or_from(parts)
            }
        }

        // bounded (except/join): only the exact (start, end) span decides.
        // A finish ending at end_gen is seen eagerly at gen == end_gen, or as a
        // late registration at gen == end_gen + 1; anything else is another span.
        AcceptCondition::Unless { symbol_id, start_gen, end_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            if visiting.contains(&root) {
                return cond.clone();
            }
            if gen_idx < *end_gen {
                cond.clone()
            } else if gen_idx == *end_gen {
                if let Some(fin) = cond_path_fins.get(&root) {
                    let nv = with_added(visiting, root);
                    rec(&fin.neg(), &nv)
                } else if active_cond_paths.contains(&root) {
                    // a late finish may still surface when the root dies next step
                    cond.clone()
                } else {
                    AcceptCondition::Always
                }
            } else if gen_idx == *end_gen + 1 {
                if let Some(fin) = late_cond_path_fins.get(&root) {
                    let nv = with_added(visiting, root);
                    rec(&fin.neg(), &nv)
                } else {
                    AcceptCondition::Always
                }
            } else {
                AcceptCondition::Always
            }
        }

        AcceptCondition::OnlyIf { symbol_id, start_gen, end_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            if visiting.contains(&root) {
                return cond.clone();
            }
            if gen_idx < *end_gen {
                cond.clone()
            } else if gen_idx == *end_gen {
                if let Some(fin) = cond_path_fins.get(&root) {
                    let nv = with_added(visiting, root);
                    rec(fin, &nv)
                } else if active_cond_paths.contains(&root) {
                    cond.clone()
                } else {
                    AcceptCondition::Never
                }
            } else if gen_idx == *end_gen + 1 {
                if let Some(fin) = late_cond_path_fins.get(&root) {
                    let nv = with_added(visiting, root);
                    rec(fin, &nv)
                } else {
                    AcceptCondition::Never
                }
            } else {
                AcceptCondition::Never
            }
        }
    }
}

fn with_added(set: &HashSet<PathRoot>, r: PathRoot) -> HashSet<PathRoot> {
    let mut s = set.clone();
    s.insert(r);
    s
}

#[cfg(test)]
mod tests {
    use super::*;

    fn pr(s: i32, g: i32) -> PathRoot {
        PathRoot::new(s, g)
    }
    fn nex(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::NotExists { symbol_id: s, start_gen: g }
    }
    fn nlm(s: i32, g: i32, m: i32) -> AcceptCondition {
        AcceptCondition::NoLongerMatch { symbol_id: s, start_gen: g, min_end_gen: m }
    }
    fn unless(s: i32, b: i32, e: i32) -> AcceptCondition {
        AcceptCondition::Unless { symbol_id: s, start_gen: b, end_gen: e }
    }

    fn no_fins() -> HashMap<PathRoot, AcceptCondition> {
        HashMap::default()
    }
    fn fins(items: Vec<(PathRoot, AcceptCondition)>) -> HashMap<PathRoot, AcceptCondition> {
        items.into_iter().collect()
    }
    fn active(items: Vec<PathRoot>) -> HashSet<PathRoot> {
        items.into_iter().collect()
    }

    /// `evolve_accept_condition` with an empty cumulative `seen` map — the
    /// Kotlin default argument. Keeps the pre-existing assertions verbatim.
    fn evolve(
        cond: &AcceptCondition,
        f: &HashMap<PathRoot, AcceptCondition>,
        l: &HashMap<PathRoot, AcceptCondition>,
        a: &HashSet<PathRoot>,
        gen_idx: i32,
    ) -> AcceptCondition {
        evolve_accept_condition(cond, f, l, a, gen_idx, &HashMap::default())
    }

    #[test]
    fn evaluate_constants() {
        let f = no_fins();
        let a = active(vec![]);
        assert!(evaluate_accept_condition(&AcceptCondition::Always, &f, &a));
        assert!(!evaluate_accept_condition(&AcceptCondition::Never, &f, &a));
    }

    #[test]
    fn unless_eager_absorbs_only_at_end_gen() {
        let f = fins(vec![(pr(1, 2), AcceptCondition::Always)]);
        let l = no_fins();
        let a = active(vec![]);
        // gen == end: fin present -> Never
        assert_eq!(
            evolve(&unless(1, 2, 5), &f, &l, &a, 5),
            AcceptCondition::Never
        );
        // gen > end+1: fin of another span is ignored -> Always
        assert_eq!(
            evolve(&unless(1, 2, 4), &f, &l, &a, 6),
            AcceptCondition::Always
        );
        // gen < end: pending
        assert_eq!(
            evolve(&unless(1, 2, 7), &f, &l, &a, 5),
            unless(1, 2, 7)
        );
    }

    #[test]
    fn unless_waits_one_step_for_late_fin() {
        let f = no_fins();
        let l = no_fins();
        let a = active(vec![pr(1, 2)]);
        // root alive at end gen, no eager fin -> wait for late
        assert_eq!(
            evolve(&unless(1, 2, 5), &f, &l, &a, 5),
            unless(1, 2, 5)
        );
        // late fin at end+1 -> absorb
        let l2 = fins(vec![(pr(1, 2), AcceptCondition::Always)]);
        assert_eq!(
            evolve(&unless(1, 2, 5), &f, &l2, &active(vec![]), 6),
            AcceptCondition::Never
        );
        // no late fin at end+1 -> resolved Always
        assert_eq!(
            evolve(&unless(1, 2, 5), &f, &no_fins(), &active(vec![]), 6),
            AcceptCondition::Always
        );
    }

    #[test]
    fn no_longer_match_respects_min_end_gen() {
        let f = fins(vec![(pr(1, 2), AcceptCondition::Always)]);
        let l = no_fins();
        let a = active(vec![]);
        // fin ends at gen 5 < min_end 6: ignored -> Always
        assert_eq!(
            evolve(&nlm(1, 2, 6), &f, &l, &a, 5),
            AcceptCondition::Always
        );
        // fin ends at gen 6 >= min_end 6: absorbed -> Never
        assert_eq!(
            evolve(&nlm(1, 2, 6), &f, &l, &a, 6),
            AcceptCondition::Never
        );
    }

    #[test]
    fn no_longer_match_keeps_pending_after_conditional_fin() {
        let fin_cond = nex(9, 9);
        let f = fins(vec![(pr(1, 2), fin_cond.clone())]);
        let l = no_fins();
        let a = active(vec![pr(1, 2), pr(9, 9)]);
        let result = evolve(&nlm(1, 2, 3), &f, &l, &a, 4);
        // ¬fin ∧ still-pending — both parts must be present
        let expected = AcceptCondition::and_from([
            AcceptCondition::Exists { symbol_id: 9, start_gen: 9 },
            nlm(1, 2, 3),
        ]);
        assert_eq!(result, expected);
    }

    #[test]
    fn not_exists_absorbs_late_fins() {
        let f = no_fins();
        let l = fins(vec![(pr(1, 2), AcceptCondition::Always)]);
        let a = active(vec![]);
        assert_eq!(
            evolve(&nex(1, 2), &f, &l, &a, 7),
            AcceptCondition::Never
        );
    }

    #[test]
    fn not_exists_pending_while_root_alive() {
        let f = no_fins();
        let l = no_fins();
        let a = active(vec![pr(1, 2)]);
        assert_eq!(evolve(&nex(1, 2), &f, &l, &a, 7), nex(1, 2));
        // root dead, nothing seen -> Always
        assert_eq!(
            evolve(&nex(1, 2), &f, &l, &active(vec![]), 7),
            AcceptCondition::Always
        );
    }

    /// bug B: the guarded body outran the lookahead's match, so by the time the
    /// leaf materializes the watcher root is neither active nor finishing this
    /// step — both per-step channels are empty. Only the cumulative
    /// `seen_cond_path_fins` record still carries the earlier observation, and it
    /// must falsify the NotExists (without it the leaf resolves to `Always` and
    /// the lookahead silently disappears).
    #[test]
    fn not_exists_falsified_by_seen_cond_path_fins() {
        let f = no_fins();
        let l = no_fins();
        let a = active(vec![]);
        let seen = fins(vec![(pr(1, 2), AcceptCondition::Always)]);
        assert_eq!(
            evolve_accept_condition(&nex(1, 2), &f, &l, &a, 7, &seen),
            AcceptCondition::Never
        );
        // Dual: Exists is satisfied by the same record.
        let ex = AcceptCondition::Exists { symbol_id: 1, start_gen: 2 };
        assert_eq!(
            evolve_accept_condition(&ex, &f, &l, &a, 7, &seen),
            AcceptCondition::Always
        );
        // A record for a different span must not discharge this leaf.
        let other = fins(vec![(pr(1, 3), AcceptCondition::Always)]);
        assert_eq!(
            evolve_accept_condition(&nex(1, 2), &f, &l, &a, 7, &other),
            AcceptCondition::Always
        );
        // Bounded / longest shapes need exact-span discharge — they must NOT
        // consult the cumulative record.
        assert_eq!(
            evolve_accept_condition(&unless(1, 2, 7), &f, &l, &a, 7, &seen),
            AcceptCondition::Always
        );
        assert_eq!(
            evolve_accept_condition(&nlm(1, 2, 3), &f, &l, &a, 7, &seen),
            AcceptCondition::Always
        );
    }
}
