//! Set of `PathRoot`s referenced by an `AcceptCondition`.
//!
//! Mirrors the Kotlin `RootSet` (sealed class with `Empty`/`Single`/`Many` cases)
//! at `AcceptCondition.kt:19-115`. The size-stratified representation keeps the
//! common cases (no roots, one root) free of heap allocations.

use std::collections::BTreeSet;

use crate::path_root::PathRoot;

use super::AcceptCondition;

#[derive(Clone, PartialEq, Eq, Debug)]
pub enum RootSet {
    Empty,
    Single(PathRoot),
    Many(BTreeSet<PathRoot>),
}

impl RootSet {
    pub fn len(&self) -> usize {
        match self {
            RootSet::Empty => 0,
            RootSet::Single(_) => 1,
            RootSet::Many(s) => s.len(),
        }
    }

    pub fn is_empty(&self) -> bool {
        matches!(self, RootSet::Empty)
    }

    pub fn contains(&self, root: &PathRoot) -> bool {
        match self {
            RootSet::Empty => false,
            RootSet::Single(r) => r == root,
            RootSet::Many(s) => s.contains(root),
        }
    }

    pub fn for_each<F: FnMut(&PathRoot)>(&self, mut f: F) {
        match self {
            RootSet::Empty => {}
            RootSet::Single(r) => f(r),
            RootSet::Many(s) => {
                for r in s {
                    f(r);
                }
            }
        }
    }

    /// True iff none of `self`'s roots appear in `other`.
    pub fn is_disjoint_from(&self, other: &BTreeSet<PathRoot>) -> bool {
        match self {
            RootSet::Empty => true,
            RootSet::Single(r) => !other.contains(r),
            RootSet::Many(s) => s.is_disjoint(other),
        }
    }
}

/// Union of two roots into a `RootSet`. Avoids allocation in the common case
/// where one side is `Empty` or both sides are the same `Single`.
pub fn union_of_pair(a: &RootSet, b: &RootSet) -> RootSet {
    match (a, b) {
        (RootSet::Empty, x) | (x, RootSet::Empty) => x.clone(),
        (RootSet::Single(ra), RootSet::Single(rb)) => {
            if ra == rb {
                RootSet::Single(*ra)
            } else {
                let mut s = BTreeSet::new();
                s.insert(*ra);
                s.insert(*rb);
                RootSet::Many(s)
            }
        }
        _ => {
            let mut s = BTreeSet::new();
            a.for_each(|r| {
                s.insert(*r);
            });
            b.for_each(|r| {
                s.insert(*r);
            });
            collapse(s)
        }
    }
}

/// Union of every child's `referenced_roots`. Used when constructing an `And`/`Or`.
pub fn union_of_slice(children: &[AcceptCondition]) -> RootSet {
    let mut combined: Option<BTreeSet<PathRoot>> = None;
    let mut single: Option<PathRoot> = None;
    for child in children {
        let rs = referenced_roots(child);
        match rs {
            RootSet::Empty => {}
            RootSet::Single(r) => match (&mut combined, single) {
                (Some(set), _) => {
                    set.insert(r);
                }
                (None, None) => single = Some(r),
                (None, Some(existing)) if existing == r => {
                    // identical single — no change
                }
                (None, Some(existing)) => {
                    let mut s = BTreeSet::new();
                    s.insert(existing);
                    s.insert(r);
                    combined = Some(s);
                    single = None;
                }
            },
            RootSet::Many(many) => {
                let set = combined.get_or_insert_with(BTreeSet::new);
                if let Some(existing) = single.take() {
                    set.insert(existing);
                }
                set.extend(many);
            }
        }
    }
    match (combined, single) {
        (Some(s), _) => collapse(s),
        (None, Some(r)) => RootSet::Single(r),
        (None, None) => RootSet::Empty,
    }
}

/// `AcceptCondition::referenced_roots` — used during composite construction.
fn referenced_roots(cond: &AcceptCondition) -> RootSet {
    match cond {
        AcceptCondition::Always | AcceptCondition::Never => RootSet::Empty,
        AcceptCondition::NoLongerMatch { symbol_id, start_gen, .. }
        | AcceptCondition::NeedLongerMatch { symbol_id, start_gen, .. }
        | AcceptCondition::NotExists { symbol_id, start_gen }
        | AcceptCondition::Exists { symbol_id, start_gen }
        | AcceptCondition::Unless { symbol_id, start_gen }
        | AcceptCondition::OnlyIf { symbol_id, start_gen } => {
            RootSet::Single(PathRoot::new(*symbol_id, *start_gen))
        }
        // Composites haven't gotten their cached field yet (Task 6). For Task 3
        // we still need this to recurse for tests; once `CompositeCache` lands
        // we'll switch to reading the cache directly.
        AcceptCondition::And { items } | AcceptCondition::Or { items } => union_of_slice(items),
    }
}

/// Collapse a `BTreeSet` produced during union back into a `RootSet`, picking the
/// smallest representation.
fn collapse(s: BTreeSet<PathRoot>) -> RootSet {
    match s.len() {
        0 => RootSet::Empty,
        1 => RootSet::Single(*s.iter().next().unwrap()),
        _ => RootSet::Many(s),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn pr(s: i32, g: i32) -> PathRoot {
        PathRoot::new(s, g)
    }

    fn nlm(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::NoLongerMatch { symbol_id: s, start_gen: g, from_next_gen: false }
    }

    #[test]
    fn empty_and_single_basics() {
        assert!(RootSet::Empty.is_empty());
        assert_eq!(RootSet::Empty.len(), 0);
        let s = RootSet::Single(pr(1, 2));
        assert!(!s.is_empty());
        assert_eq!(s.len(), 1);
        assert!(s.contains(&pr(1, 2)));
        assert!(!s.contains(&pr(1, 3)));
    }

    #[test]
    fn union_pair_empty_short_circuit() {
        let a = RootSet::Empty;
        let b = RootSet::Single(pr(1, 1));
        assert_eq!(union_of_pair(&a, &b), b);
        assert_eq!(union_of_pair(&b, &a), b);
    }

    #[test]
    fn union_pair_two_singles_same() {
        let a = RootSet::Single(pr(1, 2));
        let b = RootSet::Single(pr(1, 2));
        assert_eq!(union_of_pair(&a, &b), RootSet::Single(pr(1, 2)));
    }

    #[test]
    fn union_pair_two_singles_distinct_makes_many() {
        let a = RootSet::Single(pr(1, 2));
        let b = RootSet::Single(pr(3, 4));
        let u = union_of_pair(&a, &b);
        assert_eq!(u.len(), 2);
        assert!(u.contains(&pr(1, 2)) && u.contains(&pr(3, 4)));
    }

    #[test]
    fn union_slice_leaves() {
        let children = vec![
            AcceptCondition::Always,
            nlm(1, 2),
            nlm(3, 4),
            nlm(1, 2), // dup
        ];
        let u = union_of_slice(&children);
        assert_eq!(u.len(), 2);
        assert!(u.contains(&pr(1, 2)) && u.contains(&pr(3, 4)));
    }

    #[test]
    fn union_slice_empty_only() {
        let children = vec![AcceptCondition::Always, AcceptCondition::Never];
        assert_eq!(union_of_slice(&children), RootSet::Empty);
    }

    #[test]
    fn union_slice_collapses_single() {
        // All leaves share the same root.
        let children = vec![nlm(7, 9), nlm(7, 9), AcceptCondition::Always];
        assert_eq!(union_of_slice(&children), RootSet::Single(pr(7, 9)));
    }

    #[test]
    fn is_disjoint_from() {
        let s = RootSet::Single(pr(1, 2));
        let mut other = BTreeSet::new();
        other.insert(pr(3, 4));
        assert!(s.is_disjoint_from(&other));
        other.insert(pr(1, 2));
        assert!(!s.is_disjoint_from(&other));
    }
}
