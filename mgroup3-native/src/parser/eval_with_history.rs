//! History-aware condition evaluation. Distinct from
//! `accept_condition::eval::evaluate_accept_condition` because it walks every
//! `HistoryEntry.cond_path_finishes` rather than relying on a passed-in
//! `condPathFins` map.
//!
//! Port of `Mgroup3Parser.kt:724-804`.

use rustc_hash::{FxHashMap, FxHashSet as HashSet};

use crate::accept_condition::AcceptCondition;
use crate::parsing_ctx::HistoryEntry;
use crate::path_root::PathRoot;

/// Pre-built inverted index over `history[*].cond_path_finishes`.
///
/// For each `PathRoot` that ever finishes, stores the list of
/// `(gen_idx, &AcceptCondition)` pairs in history order. This replaces the
/// O(N) scan `collect_finishes_at_or_after` was doing per `Exists` / `NotExists`
/// / `Unless` / `OnlyIf` leaf with an O(1) lookup + O(k) filter, where `k` is
/// the number of finishes for that root.
///
/// Also caches `history.len()` so `NoLongerMatch` / `NeedLongerMatch` can
/// answer "the last entry's finish for this root" without re-walking.
pub struct HistoryIndex<'a> {
    /// Per-root list of `(gen_idx, &cond)`, in ascending `gen_idx` order.
    by_root: FxHashMap<PathRoot, Vec<(i32, &'a AcceptCondition)>>,
    /// Number of entries in the underlying history (cached for last-entry lookups).
    history_len: usize,
}

impl<'a> HistoryIndex<'a> {
    pub fn build(history: &'a [HistoryEntry]) -> Self {
        let mut by_root: FxHashMap<PathRoot, Vec<(i32, &'a AcceptCondition)>> =
            FxHashMap::default();
        for (gen_idx, entry) in history.iter().enumerate() {
            let gen_idx = gen_idx as i32;
            for (root, cond) in &entry.cond_path_finishes {
                by_root.entry(*root).or_default().push((gen_idx, cond));
            }
        }
        Self { by_root, history_len: history.len() }
    }

    /// Finish recorded for `root` in the last history entry (if any).
    fn last_entry_finish(&self, root: PathRoot) -> Option<&'a AcceptCondition> {
        if self.history_len == 0 {
            return None;
        }
        let last_gen = (self.history_len - 1) as i32;
        let v = self.by_root.get(&root)?;
        // Last entry in the per-root list is the latest finish; check that its
        // gen_idx matches the last history index.
        match v.last() {
            Some((g, c)) if *g == last_gen => Some(*c),
            _ => None,
        }
    }

    /// All finishes for `root` at history gens `>= min_gen`, history order.
    fn finishes_at_or_after(&self, root: PathRoot, min_gen: i32) -> &[(i32, &'a AcceptCondition)] {
        let Some(v) = self.by_root.get(&root) else { return &[] };
        // Vector is sorted by gen_idx ascending (built that way). Binary-search
        // for the first entry with gen_idx >= min_gen.
        let start = v.partition_point(|(g, _)| *g < min_gen);
        &v[start..]
    }
}

/// Evaluate `cond` given the pre-built history index and the set of cond roots
/// that are still active at the end of parsing.
pub fn evaluate_with_history(
    cond: &AcceptCondition,
    index: &HistoryIndex<'_>,
    active_cond_paths: &HashSet<PathRoot>,
) -> bool {
    match cond {
        AcceptCondition::Always => true,
        AcceptCondition::Never => false,
        AcceptCondition::And { items } => items
            .iter()
            .all(|c| evaluate_with_history(c, index, active_cond_paths)),
        AcceptCondition::Or { items } => items
            .iter()
            .any(|c| evaluate_with_history(c, index, active_cond_paths)),

        AcceptCondition::NoLongerMatch { symbol_id, start_gen, from_next_gen } => {
            if *from_next_gen {
                return true;
            }
            let root = PathRoot::new(*symbol_id, *start_gen);
            match index.last_entry_finish(root) {
                None => true,
                Some(fin) => !evaluate_with_history(fin, index, active_cond_paths),
            }
        }

        AcceptCondition::NeedLongerMatch { symbol_id, start_gen, from_next_gen } => {
            if *from_next_gen {
                return false;
            }
            let root = PathRoot::new(*symbol_id, *start_gen);
            match index.last_entry_finish(root) {
                None => false,
                Some(fin) => evaluate_with_history(fin, index, active_cond_paths),
            }
        }

        AcceptCondition::NotExists { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let finishes = index.finishes_at_or_after(root, *start_gen);
            if finishes.is_empty() {
                true
            } else {
                !finishes
                    .iter()
                    .any(|(_, c)| evaluate_with_history(c, index, active_cond_paths))
            }
        }

        AcceptCondition::Exists { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let finishes = index.finishes_at_or_after(root, *start_gen);
            finishes
                .iter()
                .any(|(_, c)| evaluate_with_history(c, index, active_cond_paths))
        }

        AcceptCondition::Unless { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let finishes = index.finishes_at_or_after(root, *start_gen);
            if finishes.is_empty() {
                true
            } else {
                !finishes
                    .iter()
                    .any(|(_, c)| evaluate_with_history(c, index, active_cond_paths))
            }
        }

        AcceptCondition::OnlyIf { symbol_id, start_gen } => {
            let root = PathRoot::new(*symbol_id, *start_gen);
            let finishes = index.finishes_at_or_after(root, *start_gen);
            finishes
                .iter()
                .any(|(_, c)| evaluate_with_history(c, index, active_cond_paths))
        }
    }
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

    fn eval(cond: &AcceptCondition, history: &[HistoryEntry], active: &HashSet<PathRoot>) -> bool {
        let idx = HistoryIndex::build(history);
        evaluate_with_history(cond, &idx, active)
    }

    #[test]
    fn always_and_never() {
        let h: Vec<HistoryEntry> = vec![];
        let a: HashSet<PathRoot> = HashSet::default();
        assert!(eval(&AcceptCondition::Always, &h, &a));
        assert!(!eval(&AcceptCondition::Never, &h, &a));
    }

    #[test]
    fn no_longer_match_from_next_gen_true() {
        let h: Vec<HistoryEntry> = vec![];
        let a: HashSet<PathRoot> = HashSet::default();
        let c =
            AcceptCondition::NoLongerMatch { symbol_id: 1, start_gen: 0, from_next_gen: true };
        assert!(eval(&c, &h, &a));
    }

    #[test]
    fn no_longer_match_no_fin_true() {
        let h: Vec<HistoryEntry> = vec![HistoryEntry::default()];
        let a: HashSet<PathRoot> = HashSet::default();
        let c =
            AcceptCondition::NoLongerMatch { symbol_id: 1, start_gen: 0, from_next_gen: false };
        assert!(eval(&c, &h, &a));
    }

    #[test]
    fn no_longer_match_with_true_fin_false() {
        let (root, fin) = always_pr(1, 0);
        let h = vec![entry_with_finish(root, fin)];
        let a: HashSet<PathRoot> = HashSet::default();
        let c =
            AcceptCondition::NoLongerMatch { symbol_id: 1, start_gen: 0, from_next_gen: false };
        assert!(!eval(&c, &h, &a));
    }

    #[test]
    fn need_longer_match_from_next_gen_false() {
        let h: Vec<HistoryEntry> = vec![];
        let a: HashSet<PathRoot> = HashSet::default();
        let c =
            AcceptCondition::NeedLongerMatch { symbol_id: 1, start_gen: 0, from_next_gen: true };
        assert!(!eval(&c, &h, &a));
    }

    #[test]
    fn exists_finds_finish_at_or_after_min_gen() {
        // start_gen = 2; finish recorded at gen 3.
        let (root, fin) = always_pr(5, 2);
        let mut h = vec![HistoryEntry::default(), HistoryEntry::default(), HistoryEntry::default()];
        h.push(entry_with_finish(root, fin));
        let a: HashSet<PathRoot> = HashSet::default();
        let c = AcceptCondition::Exists { symbol_id: 5, start_gen: 2 };
        assert!(eval(&c, &h, &a));
    }

    #[test]
    fn exists_skips_finishes_before_min_gen() {
        // Finish recorded at gen 1, but start_gen is 2 — must be skipped.
        let (root, fin) = always_pr(5, 2);
        let mut h = vec![HistoryEntry::default(), entry_with_finish(root, fin)];
        h.push(HistoryEntry::default());
        let a: HashSet<PathRoot> = HashSet::default();
        let c = AcceptCondition::Exists { symbol_id: 5, start_gen: 2 };
        assert!(!eval(&c, &h, &a));
    }

    #[test]
    fn not_exists_inverse_of_exists() {
        let h: Vec<HistoryEntry> = vec![HistoryEntry::default()];
        let a: HashSet<PathRoot> = HashSet::default();
        let c = AcceptCondition::NotExists { symbol_id: 1, start_gen: 0 };
        // No finishes → true
        assert!(eval(&c, &h, &a));
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
        assert!(!eval(&and, &h, &a));
        assert!(eval(&or, &h, &a));
    }
}
