//! `evaluate_accept_condition` and `evolve_accept_condition`.
//!
//! Mirror of Kotlin `AcceptCondition.kt:426-589`:
//! - `evaluate_accept_condition`: pure walker. No history; per-leaf table reads
//!   from `cond_path_fins`.
//! - `evolve_accept_condition`: walks the tree, expanding leaves whose root has
//!   a `condPathFins` entry. The `visiting` set prevents infinite recursion on
//!   self-referential fins; the cycle-fallback per variant follows the table in
//!   the plan.

use std::collections::{HashMap, HashSet};

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

        AcceptCondition::NoLongerMatch { symbol_id, start_gen, .. } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            match cond_path_fins.get(&root) {
                Some(fin) => !evaluate_accept_condition(fin, cond_path_fins, active_cond_paths),
                None => true,
            }
        }
        AcceptCondition::NeedLongerMatch { symbol_id, start_gen, .. } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            match cond_path_fins.get(&root) {
                Some(fin) => evaluate_accept_condition(fin, cond_path_fins, active_cond_paths),
                None => false,
            }
        }
        AcceptCondition::NotExists { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            match cond_path_fins.get(&root) {
                Some(fin) => !evaluate_accept_condition(fin, cond_path_fins, active_cond_paths),
                None => true,
            }
        }
        AcceptCondition::Exists { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            match cond_path_fins.get(&root) {
                Some(fin) => evaluate_accept_condition(fin, cond_path_fins, active_cond_paths),
                None => false,
            }
        }
        AcceptCondition::Unless { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            match cond_path_fins.get(&root) {
                Some(fin) => !evaluate_accept_condition(fin, cond_path_fins, active_cond_paths),
                None => true,
            }
        }
        AcceptCondition::OnlyIf { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            match cond_path_fins.get(&root) {
                Some(fin) => evaluate_accept_condition(fin, cond_path_fins, active_cond_paths),
                None => false,
            }
        }
    }
}

/// Public entry. Walks `cond` rewriting leaves whose root has an entry in
/// `cond_path_fins`. The `gen` parameter is currently unused by the walker; it
/// is plumbed through so the signature lines up with the Kotlin parser hooks
/// that will land in Step 3+.
pub fn evolve_accept_condition(
    cond: &AcceptCondition,
    cond_path_fins: &HashMap<PathRoot, AcceptCondition>,
    active_cond_paths: &HashSet<PathRoot>,
    gen_idx: i32,
) -> AcceptCondition {
    let visiting = HashSet::new();
    evolve_inner(cond, cond_path_fins, active_cond_paths, gen_idx, &visiting)
}

fn evolve_inner(
    cond: &AcceptCondition,
    cond_path_fins: &HashMap<PathRoot, AcceptCondition>,
    active_cond_paths: &HashSet<PathRoot>,
    gen_idx: i32,
    visiting: &HashSet<PathRoot>,
) -> AcceptCondition {
    match cond {
        AcceptCondition::Always | AcceptCondition::Never => cond.clone(),
        AcceptCondition::And { items } => {
            let evolved: Vec<_> = items
                .iter()
                .map(|c| evolve_inner(c, cond_path_fins, active_cond_paths, gen_idx, visiting))
                .collect();
            AcceptCondition::and_from(evolved)
        }
        AcceptCondition::Or { items } => {
            let evolved: Vec<_> = items
                .iter()
                .map(|c| evolve_inner(c, cond_path_fins, active_cond_paths, gen_idx, visiting))
                .collect();
            AcceptCondition::or_from(evolved)
        }
        AcceptCondition::NoLongerMatch { symbol_id, start_gen, from_next_gen } => {
            if *from_next_gen {
                AcceptCondition::NoLongerMatch {
                    symbol_id: *symbol_id,
                    start_gen: *start_gen,
                    from_next_gen: false,
                }
            } else {
                let root = PathRoot::new(*symbol_id, *start_gen);
                let fin = cond_path_fins.get(&root);
                if let Some(fin) = fin {
                    if !visiting.contains(&root) {
                        let next_visiting = with_added(visiting, root);
                        return evolve_inner(
                            &fin.neg(),
                            cond_path_fins,
                            active_cond_paths,
                            gen_idx,
                            &next_visiting,
                        );
                    }
                }
                if visiting.contains(&root) {
                    AcceptCondition::Always
                } else if active_cond_paths.contains(&root) {
                    cond.clone()
                } else {
                    AcceptCondition::Always
                }
            }
        }
        AcceptCondition::NeedLongerMatch { symbol_id, start_gen, from_next_gen } => {
            if *from_next_gen {
                AcceptCondition::NeedLongerMatch {
                    symbol_id: *symbol_id,
                    start_gen: *start_gen,
                    from_next_gen: false,
                }
            } else {
                let root = PathRoot::new(*symbol_id, *start_gen);
                let fin = cond_path_fins.get(&root);
                if let Some(fin) = fin {
                    if !visiting.contains(&root) {
                        let next_visiting = with_added(visiting, root);
                        return evolve_inner(
                            fin,
                            cond_path_fins,
                            active_cond_paths,
                            gen_idx,
                            &next_visiting,
                        );
                    }
                }
                if visiting.contains(&root) {
                    AcceptCondition::Always
                } else if active_cond_paths.contains(&root) {
                    cond.clone()
                } else {
                    AcceptCondition::Never
                }
            }
        }
        AcceptCondition::NotExists { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let fin = cond_path_fins.get(&root);
            if let Some(fin) = fin {
                if !visiting.contains(&root) {
                    let next_visiting = with_added(visiting, root);
                    return evolve_inner(
                        &fin.neg(),
                        cond_path_fins,
                        active_cond_paths,
                        gen_idx,
                        &next_visiting,
                    );
                }
            }
            if active_cond_paths.contains(&root) {
                cond.clone()
            } else if fin.is_some() {
                cond.clone()
            } else {
                AcceptCondition::Always
            }
        }
        AcceptCondition::Exists { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let fin = cond_path_fins.get(&root);
            if let Some(fin) = fin {
                if !visiting.contains(&root) {
                    let next_visiting = with_added(visiting, root);
                    return evolve_inner(
                        fin,
                        cond_path_fins,
                        active_cond_paths,
                        gen_idx,
                        &next_visiting,
                    );
                }
            }
            if active_cond_paths.contains(&root) {
                cond.clone()
            } else if fin.is_some() {
                cond.clone()
            } else {
                AcceptCondition::Never
            }
        }
        AcceptCondition::Unless { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let fin = cond_path_fins.get(&root);
            if let Some(fin) = fin {
                if !visiting.contains(&root) {
                    let next_visiting = with_added(visiting, root);
                    return evolve_inner(
                        &fin.neg(),
                        cond_path_fins,
                        active_cond_paths,
                        gen_idx,
                        &next_visiting,
                    );
                }
            }
            if active_cond_paths.contains(&root) {
                cond.clone()
            } else if fin.is_some() {
                cond.clone()
            } else {
                AcceptCondition::Always
            }
        }
        AcceptCondition::OnlyIf { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let fin = cond_path_fins.get(&root);
            if let Some(fin) = fin {
                if !visiting.contains(&root) {
                    let next_visiting = with_added(visiting, root);
                    return evolve_inner(
                        fin,
                        cond_path_fins,
                        active_cond_paths,
                        gen_idx,
                        &next_visiting,
                    );
                }
            }
            if active_cond_paths.contains(&root) {
                cond.clone()
            } else if fin.is_some() {
                cond.clone()
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
    fn ex(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::Exists { symbol_id: s, start_gen: g }
    }
    fn nex(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::NotExists { symbol_id: s, start_gen: g }
    }
    fn nlm(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::NoLongerMatch { symbol_id: s, start_gen: g, from_next_gen: false }
    }
    fn need(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::NeedLongerMatch { symbol_id: s, start_gen: g, from_next_gen: false }
    }
    fn unless(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::Unless { symbol_id: s, start_gen: g }
    }
    fn only_if(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::OnlyIf { symbol_id: s, start_gen: g }
    }

    fn empty_active() -> HashSet<PathRoot> {
        HashSet::new()
    }

    fn fins(items: Vec<(PathRoot, AcceptCondition)>) -> HashMap<PathRoot, AcceptCondition> {
        items.into_iter().collect()
    }

    #[test]
    fn always_never() {
        let f = HashMap::new();
        let a = empty_active();
        assert!(evaluate_accept_condition(&AcceptCondition::Always, &f, &a));
        assert!(!evaluate_accept_condition(&AcceptCondition::Never, &f, &a));
    }

    #[test]
    fn no_longer_match_no_fin_is_true() {
        let f = HashMap::new();
        let a = empty_active();
        assert!(evaluate_accept_condition(&nlm(1, 2), &f, &a));
    }

    #[test]
    fn no_longer_match_with_true_fin_is_false() {
        let f = fins(vec![(pr(1, 2), AcceptCondition::Always)]);
        let a = empty_active();
        assert!(!evaluate_accept_condition(&nlm(1, 2), &f, &a));
    }

    #[test]
    fn need_longer_match_no_fin_is_false() {
        let f = HashMap::new();
        let a = empty_active();
        assert!(!evaluate_accept_condition(&need(1, 2), &f, &a));
    }

    #[test]
    fn need_longer_match_with_true_fin_is_true() {
        let f = fins(vec![(pr(1, 2), AcceptCondition::Always)]);
        let a = empty_active();
        assert!(evaluate_accept_condition(&need(1, 2), &f, &a));
    }

    #[test]
    fn exists_family_with_and_without_fin() {
        let f = fins(vec![(pr(1, 2), AcceptCondition::Always)]);
        let a = empty_active();
        assert!(evaluate_accept_condition(&ex(1, 2), &f, &a));
        assert!(!evaluate_accept_condition(&ex(9, 9), &f, &a)); // no fin
        assert!(!evaluate_accept_condition(&nex(1, 2), &f, &a));
        assert!(evaluate_accept_condition(&nex(9, 9), &f, &a));
        assert!(!evaluate_accept_condition(&unless(1, 2), &f, &a));
        assert!(evaluate_accept_condition(&unless(9, 9), &f, &a));
        assert!(evaluate_accept_condition(&only_if(1, 2), &f, &a));
        assert!(!evaluate_accept_condition(&only_if(9, 9), &f, &a));
    }

    #[test]
    fn and_all_true_or_any_true() {
        let f = fins(vec![
            (pr(1, 0), AcceptCondition::Always),
            (pr(2, 0), AcceptCondition::Always),
        ]);
        let a = empty_active();
        let cand = AcceptCondition::and_from([ex(1, 0), ex(2, 0)]);
        assert!(evaluate_accept_condition(&cand, &f, &a));

        let f_partial = fins(vec![(pr(1, 0), AcceptCondition::Always)]);
        assert!(!evaluate_accept_condition(&cand, &f_partial, &a));

        let cor = AcceptCondition::or_from([ex(1, 0), ex(2, 0)]);
        assert!(evaluate_accept_condition(&cor, &f_partial, &a));
    }

    #[test]
    fn nested_fin_recurses() {
        // root pr(1,0) has fin Exists(2,0); root pr(2,0) has fin Always.
        // Eval of Exists(1,0) recurses through both.
        let f = fins(vec![
            (pr(1, 0), ex(2, 0)),
            (pr(2, 0), AcceptCondition::Always),
        ]);
        let a = empty_active();
        assert!(evaluate_accept_condition(&ex(1, 0), &f, &a));
    }

    // ---- evolve tests ----

    fn active_with(roots: &[PathRoot]) -> HashSet<PathRoot> {
        roots.iter().copied().collect()
    }

    #[test]
    fn evolve_constants_passthrough() {
        let f = HashMap::new();
        let a = empty_active();
        assert_eq!(
            evolve_accept_condition(&AcceptCondition::Always, &f, &a, 0),
            AcceptCondition::Always
        );
        assert_eq!(
            evolve_accept_condition(&AcceptCondition::Never, &f, &a, 0),
            AcceptCondition::Never
        );
    }

    #[test]
    fn evolve_strips_from_next_gen() {
        let f = HashMap::new();
        let a = empty_active();
        let c =
            AcceptCondition::NoLongerMatch { symbol_id: 1, start_gen: 2, from_next_gen: true };
        let expected = AcceptCondition::NoLongerMatch {
            symbol_id: 1,
            start_gen: 2,
            from_next_gen: false,
        };
        assert_eq!(evolve_accept_condition(&c, &f, &a, 0), expected);

        let c2 =
            AcceptCondition::NeedLongerMatch { symbol_id: 1, start_gen: 2, from_next_gen: true };
        let expected2 = AcceptCondition::NeedLongerMatch {
            symbol_id: 1,
            start_gen: 2,
            from_next_gen: false,
        };
        assert_eq!(evolve_accept_condition(&c2, &f, &a, 0), expected2);
    }

    #[test]
    fn evolve_nlm_no_fin_no_active_is_always() {
        let f = HashMap::new();
        let a = empty_active();
        let c = nlm(1, 2);
        assert_eq!(evolve_accept_condition(&c, &f, &a, 0), AcceptCondition::Always);
    }

    #[test]
    fn evolve_nlm_active_keeps_cond() {
        let f = HashMap::new();
        let a = active_with(&[pr(1, 2)]);
        let c = nlm(1, 2);
        assert_eq!(evolve_accept_condition(&c, &f, &a, 0), c);
    }

    #[test]
    fn evolve_nlm_with_fin_expands_neg() {
        // fin pr(1,2) = Always → !Always = Never
        let f = fins(vec![(pr(1, 2), AcceptCondition::Always)]);
        let a = empty_active();
        assert_eq!(evolve_accept_condition(&nlm(1, 2), &f, &a, 0), AcceptCondition::Never);
    }

    #[test]
    fn evolve_need_no_fin_no_active_is_never() {
        let f = HashMap::new();
        let a = empty_active();
        assert_eq!(evolve_accept_condition(&need(1, 2), &f, &a, 0), AcceptCondition::Never);
    }

    #[test]
    fn evolve_need_with_fin_expands() {
        let f = fins(vec![(pr(1, 2), AcceptCondition::Always)]);
        let a = empty_active();
        assert_eq!(evolve_accept_condition(&need(1, 2), &f, &a, 0), AcceptCondition::Always);
    }

    #[test]
    fn evolve_exists_no_fin_no_active_is_never() {
        let f = HashMap::new();
        let a = empty_active();
        assert_eq!(evolve_accept_condition(&ex(1, 2), &f, &a, 0), AcceptCondition::Never);
    }

    #[test]
    fn evolve_notexists_no_fin_no_active_is_always() {
        let f = HashMap::new();
        let a = empty_active();
        assert_eq!(evolve_accept_condition(&nex(1, 2), &f, &a, 0), AcceptCondition::Always);
    }

    #[test]
    fn evolve_unless_no_fin_no_active_is_always() {
        let f = HashMap::new();
        let a = empty_active();
        assert_eq!(evolve_accept_condition(&unless(1, 2), &f, &a, 0), AcceptCondition::Always);
    }

    #[test]
    fn evolve_only_if_no_fin_no_active_is_never() {
        let f = HashMap::new();
        let a = empty_active();
        assert_eq!(evolve_accept_condition(&only_if(1, 2), &f, &a, 0), AcceptCondition::Never);
    }

    #[test]
    fn evolve_cycle_nlm_falls_back_to_always() {
        // fin pr(1,0) references the same root (cycle).
        // First descent: visiting={}, fin found → recurse fin.neg() with visiting={pr(1,0)}.
        // fin.neg() = NeedLongerMatch(1,0). Recurse with visiting={pr(1,0)} but no fin
        // entry for any new root → wait, fin IS Exists(1,0) below. Let's craft cleaner:
        // fin = NoLongerMatch(1,0) itself. Then fin.neg() = NeedLongerMatch(1,0), and the
        // root of that leaf is pr(1,0), which is in visiting → falls back to Always.
        let f = fins(vec![(pr(1, 0), nlm(1, 0))]);
        let a = empty_active();
        assert_eq!(evolve_accept_condition(&nlm(1, 0), &f, &a, 0), AcceptCondition::Always);
    }

    #[test]
    fn evolve_cycle_exists_falls_back_appropriately() {
        // fin pr(1,0) = Exists(1,0). Eval Exists(1,0):
        //   visiting={}, fin found, !visiting → recurse fin with visiting={pr(1,0)}.
        //   fin = Exists(1,0). Now root in visiting. fin lookup hits cond_path_fins again
        //   but visiting blocks recursion → fall through. fin.is_some() && !active → return cond.
        let f = fins(vec![(pr(1, 0), ex(1, 0))]);
        let a = empty_active();
        // The walker returns the leaf with fin.is_some() && !active branch.
        // Hard to assert exact form without running; verify result is deterministic and finite.
        let result = evolve_accept_condition(&ex(1, 0), &f, &a, 0);
        // It should not panic / loop. The exact form for Exists cycle is `cond` (the inner ex).
        assert_eq!(result, ex(1, 0));
    }

    #[test]
    fn evolve_recurses_through_and() {
        let f = fins(vec![
            (pr(1, 0), AcceptCondition::Always),
            (pr(2, 0), AcceptCondition::Never),
        ]);
        let a = empty_active();
        // And(Exists(1), Exists(2)) → Exists(1) evolves to Always (via fin), Exists(2) to Never.
        // And(Always, Never) → Never.
        let c = AcceptCondition::and_from([ex(1, 0), ex(2, 0)]);
        assert_eq!(evolve_accept_condition(&c, &f, &a, 0), AcceptCondition::Never);
    }

    #[test]
    fn evolve_recurses_through_or() {
        let f = fins(vec![
            (pr(1, 0), AcceptCondition::Always),
            (pr(2, 0), AcceptCondition::Never),
        ]);
        let a = empty_active();
        let c = AcceptCondition::or_from([ex(1, 0), ex(2, 0)]);
        // Or(Always, Never) → Always
        assert_eq!(evolve_accept_condition(&c, &f, &a, 0), AcceptCondition::Always);
    }
}
