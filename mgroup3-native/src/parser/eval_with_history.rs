//! History-aware condition evaluation. Distinct from
//! `accept_condition::eval::evaluate_accept_condition` because it walks every
//! `HistoryEntry.cond_path_finishes` rather than relying on a passed-in
//! `condPathFins` map.
//!
//! Port of `Mgroup3Parser.kt:724-804`.

use rustc_hash::FxHashSet as HashSet;

use crate::accept_condition::AcceptCondition;
use crate::parsing_ctx::HistoryEntry;
use crate::path_root::PathRoot;

/// Evaluate `cond` given the full parse history and the set of cond roots that
/// are still active at the end of parsing.
pub fn evaluate_with_history(
    cond: &AcceptCondition,
    history: &[HistoryEntry],
    active_cond_paths: &HashSet<PathRoot>,
) -> bool {
    match cond {
        AcceptCondition::Always => true,
        AcceptCondition::Never => false,
        AcceptCondition::And { items } => items
            .iter()
            .all(|c| evaluate_with_history(c, history, active_cond_paths)),
        AcceptCondition::Or { items } => items
            .iter()
            .any(|c| evaluate_with_history(c, history, active_cond_paths)),

        AcceptCondition::NoLongerMatch { symbol_id, start_gen, from_next_gen } => {
            // Survived NoLongerMatch leaves are decided by the LAST history
            // entry's cond_path_finishes. fromNextGen=true → trivially true
            // (this step's finish doesn't combine yet).
            if *from_next_gen {
                return true;
            }
            let root = PathRoot::new(*symbol_id, *start_gen);
            match history.last().and_then(|e| e.cond_path_finishes.get(&root)) {
                None => true,
                Some(fin) => !evaluate_with_history(fin, history, active_cond_paths),
            }
        }

        AcceptCondition::NeedLongerMatch { symbol_id, start_gen, from_next_gen } => {
            if *from_next_gen {
                return false;
            }
            let root = PathRoot::new(*symbol_id, *start_gen);
            match history.last().and_then(|e| e.cond_path_finishes.get(&root)) {
                None => false,
                Some(fin) => evaluate_with_history(fin, history, active_cond_paths),
            }
        }

        AcceptCondition::NotExists { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let finishes = collect_finishes_at_or_after(history, root, *start_gen);
            if finishes.is_empty() {
                true
            } else {
                !finishes.iter().any(|c| evaluate_with_history(c, history, active_cond_paths))
            }
        }

        AcceptCondition::Exists { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let finishes = collect_finishes_at_or_after(history, root, *start_gen);
            finishes.iter().any(|c| evaluate_with_history(c, history, active_cond_paths))
        }

        AcceptCondition::Unless { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let finishes = collect_finishes_at_or_after(history, root, *start_gen);
            if finishes.is_empty() {
                true
            } else {
                !finishes.iter().any(|c| evaluate_with_history(c, history, active_cond_paths))
            }
        }

        AcceptCondition::OnlyIf { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let finishes = collect_finishes_at_or_after(history, root, *start_gen);
            finishes.iter().any(|c| evaluate_with_history(c, history, active_cond_paths))
        }
    }
}

/// Collect every `cond_path_finishes[root]` from history entries at gen index
/// `>= min_gen`. The list preserves history order.
pub fn collect_finishes_at_or_after<'a>(
    history: &'a [HistoryEntry],
    root: PathRoot,
    min_gen: i32,
) -> Vec<&'a AcceptCondition> {
    let mut out = Vec::new();
    for (gen_idx, entry) in history.iter().enumerate() {
        if (gen_idx as i32) < min_gen {
            continue;
        }
        if let Some(c) = entry.cond_path_finishes.get(&root) {
            out.push(c);
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    fn entry_with_finish(root: PathRoot, c: AcceptCondition) -> HistoryEntry {
        let mut e = HistoryEntry::default();
        e.cond_path_finishes.insert(root, c);
        e
    }

    fn always_pr(s: i32, g: i32) -> (PathRoot, AcceptCondition) {
        (PathRoot::new(s, g), AcceptCondition::Always)
    }

    #[test]
    fn always_and_never() {
        let h: Vec<HistoryEntry> = vec![];
        let a: HashSet<PathRoot> = HashSet::default();
        assert!(evaluate_with_history(&AcceptCondition::Always, &h, &a));
        assert!(!evaluate_with_history(&AcceptCondition::Never, &h, &a));
    }

    #[test]
    fn no_longer_match_from_next_gen_true() {
        let h: Vec<HistoryEntry> = vec![];
        let a: HashSet<PathRoot> = HashSet::default();
        let c =
            AcceptCondition::NoLongerMatch { symbol_id: 1, start_gen: 0, from_next_gen: true };
        assert!(evaluate_with_history(&c, &h, &a));
    }

    #[test]
    fn no_longer_match_no_fin_true() {
        let h: Vec<HistoryEntry> = vec![HistoryEntry::default()];
        let a: HashSet<PathRoot> = HashSet::default();
        let c =
            AcceptCondition::NoLongerMatch { symbol_id: 1, start_gen: 0, from_next_gen: false };
        assert!(evaluate_with_history(&c, &h, &a));
    }

    #[test]
    fn no_longer_match_with_true_fin_false() {
        let (root, fin) = always_pr(1, 0);
        let h = vec![entry_with_finish(root, fin)];
        let a: HashSet<PathRoot> = HashSet::default();
        let c =
            AcceptCondition::NoLongerMatch { symbol_id: 1, start_gen: 0, from_next_gen: false };
        assert!(!evaluate_with_history(&c, &h, &a));
    }

    #[test]
    fn need_longer_match_from_next_gen_false() {
        let h: Vec<HistoryEntry> = vec![];
        let a: HashSet<PathRoot> = HashSet::default();
        let c =
            AcceptCondition::NeedLongerMatch { symbol_id: 1, start_gen: 0, from_next_gen: true };
        assert!(!evaluate_with_history(&c, &h, &a));
    }

    #[test]
    fn exists_finds_finish_at_or_after_min_gen() {
        // start_gen = 2; finish recorded at gen 3.
        let (root, fin) = always_pr(5, 2);
        let mut h = vec![HistoryEntry::default(), HistoryEntry::default(), HistoryEntry::default()];
        h.push(entry_with_finish(root, fin));
        let a: HashSet<PathRoot> = HashSet::default();
        let c = AcceptCondition::Exists { symbol_id: 5, start_gen: 2 };
        assert!(evaluate_with_history(&c, &h, &a));
    }

    #[test]
    fn exists_skips_finishes_before_min_gen() {
        // Finish recorded at gen 1, but start_gen is 2 — must be skipped.
        let (root, fin) = always_pr(5, 2);
        let mut h = vec![HistoryEntry::default(), entry_with_finish(root, fin)];
        h.push(HistoryEntry::default());
        let a: HashSet<PathRoot> = HashSet::default();
        let c = AcceptCondition::Exists { symbol_id: 5, start_gen: 2 };
        assert!(!evaluate_with_history(&c, &h, &a));
    }

    #[test]
    fn not_exists_inverse_of_exists() {
        let h: Vec<HistoryEntry> = vec![HistoryEntry::default()];
        let a: HashSet<PathRoot> = HashSet::default();
        let c = AcceptCondition::NotExists { symbol_id: 1, start_gen: 0 };
        // No finishes → true
        assert!(evaluate_with_history(&c, &h, &a));
    }

    #[test]
    fn and_or_combine() {
        let h: Vec<HistoryEntry> = vec![HistoryEntry::default()];
        let a: HashSet<PathRoot> = HashSet::default();
        // (true ∧ false) = false; (true ∨ false) = true.
        let nlm =
            AcceptCondition::NoLongerMatch { symbol_id: 1, start_gen: 0, from_next_gen: false }; // true
        let need =
            AcceptCondition::NeedLongerMatch { symbol_id: 1, start_gen: 0, from_next_gen: false }; // false
        let and = AcceptCondition::and_from([nlm.clone(), need.clone()]);
        let or = AcceptCondition::or_from([nlm, need]);
        assert!(!evaluate_with_history(&and, &h, &a));
        assert!(evaluate_with_history(&or, &h, &a));
    }
}
