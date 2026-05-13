//! Builders for `And` / `Or` that apply the canonical-form invariants:
//! short-circuit on absorbers, drop identities, flatten nested same-kind,
//! dedup, canonical sort.
//!
//! Mirrors `AcceptCondition.kt:139-181` (And) and lines 250-296 (Or). The exact
//! Kotlin tactics (LinkedHashSet for dedup, hash-based sort) differ from this
//! Rust port — see plan §"Guiding principle". What matters is that
//! `And::from([X, Y])` and `And::from([Y, X])` return equal values.

use rustc_hash::FxHashSet as HashSet;

use super::canonical::canonical_sort;
use super::AcceptCondition;

impl AcceptCondition {
    /// Build an `And` from any iterable of children. Returns the canonical form.
    pub fn and_from<I>(children: I) -> AcceptCondition
    where
        I: IntoIterator<Item = AcceptCondition>,
    {
        let mut flat = Vec::new();
        for c in children {
            match c {
                AcceptCondition::Never => return AcceptCondition::Never,
                AcceptCondition::Always => {}
                AcceptCondition::And { items } => flat.extend(items),
                other => flat.push(other),
            }
        }
        dedup_inplace(&mut flat);
        match flat.len() {
            0 => AcceptCondition::Always,
            1 => flat.pop().unwrap(),
            _ => {
                canonical_sort(&mut flat);
                AcceptCondition::And { items: flat }
            }
        }
    }

    /// Build an `Or` from any iterable of children. Returns the canonical form.
    pub fn or_from<I>(children: I) -> AcceptCondition
    where
        I: IntoIterator<Item = AcceptCondition>,
    {
        let mut flat = Vec::new();
        for c in children {
            match c {
                AcceptCondition::Always => return AcceptCondition::Always,
                AcceptCondition::Never => {}
                AcceptCondition::Or { items } => flat.extend(items),
                other => flat.push(other),
            }
        }
        dedup_inplace(&mut flat);
        match flat.len() {
            0 => AcceptCondition::Never,
            1 => flat.pop().unwrap(),
            _ => {
                canonical_sort(&mut flat);
                AcceptCondition::Or { items: flat }
            }
        }
    }

    /// Convenience pair variant — equivalent to `and_from([a, b])` but slightly
    /// terser at call sites.
    pub fn and_from_pair(a: AcceptCondition, b: AcceptCondition) -> AcceptCondition {
        AcceptCondition::and_from([a, b])
    }

    pub fn or_from_pair(a: AcceptCondition, b: AcceptCondition) -> AcceptCondition {
        AcceptCondition::or_from([a, b])
    }
}

/// In-place dedup preserving order. Items must be `Hash + Eq`. Linear-probe
/// HashSet is fine for the small lists we build.
fn dedup_inplace(items: &mut Vec<AcceptCondition>) {
    if items.len() < 2 {
        return;
    }
    let original = std::mem::take(items);
    let mut seen: HashSet<AcceptCondition> =
        HashSet::with_capacity_and_hasher(original.len(), Default::default());
    items.reserve(original.len());
    for c in original {
        if seen.insert(c.clone()) {
            items.push(c);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn ex(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::Exists { symbol_id: s, start_gen: g }
    }
    fn nex(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::NotExists { symbol_id: s, start_gen: g }
    }

    // ---- And ----

    #[test]
    fn and_empty_is_always() {
        assert_eq!(AcceptCondition::and_from([]), AcceptCondition::Always);
    }

    #[test]
    fn and_single_passes_through() {
        assert_eq!(AcceptCondition::and_from([ex(1, 0)]), ex(1, 0));
    }

    #[test]
    fn and_with_never_short_circuits() {
        assert_eq!(
            AcceptCondition::and_from([ex(1, 0), AcceptCondition::Never, ex(2, 0)]),
            AcceptCondition::Never
        );
    }

    #[test]
    fn and_drops_always() {
        let r = AcceptCondition::and_from([AcceptCondition::Always, ex(1, 0), AcceptCondition::Always]);
        assert_eq!(r, ex(1, 0));
    }

    #[test]
    fn and_all_always_is_always() {
        assert_eq!(
            AcceptCondition::and_from([AcceptCondition::Always, AcceptCondition::Always]),
            AcceptCondition::Always
        );
    }

    #[test]
    fn and_dedups() {
        let r = AcceptCondition::and_from([ex(1, 0), ex(2, 0), ex(1, 0)]);
        match r {
            AcceptCondition::And { items } => {
                assert_eq!(items.len(), 2);
                assert!(items.contains(&ex(1, 0)) && items.contains(&ex(2, 0)));
            }
            other => panic!("expected And, got {other}"),
        }
    }

    #[test]
    fn and_order_independent() {
        let a = AcceptCondition::and_from([ex(1, 0), ex(2, 0)]);
        let b = AcceptCondition::and_from([ex(2, 0), ex(1, 0)]);
        assert_eq!(a, b);
    }

    #[test]
    fn and_flattens_nested_and() {
        let inner = AcceptCondition::and_from([ex(1, 0), ex(2, 0)]);
        let outer = AcceptCondition::and_from([inner, ex(3, 0)]);
        match outer {
            AcceptCondition::And { items } => {
                assert_eq!(items.len(), 3);
                assert!(items.contains(&ex(1, 0)));
                assert!(items.contains(&ex(2, 0)));
                assert!(items.contains(&ex(3, 0)));
            }
            other => panic!("expected flat And, got {other}"),
        }
    }

    #[test]
    fn and_does_not_flatten_or() {
        // Outer And should NOT pull children out of an inner Or.
        let inner_or = AcceptCondition::or_from([ex(1, 0), ex(2, 0)]);
        let outer = AcceptCondition::and_from([inner_or.clone(), ex(3, 0)]);
        match outer {
            AcceptCondition::And { items } => {
                assert_eq!(items.len(), 2);
                assert!(items.contains(&inner_or));
                assert!(items.contains(&ex(3, 0)));
            }
            other => panic!("expected And, got {other}"),
        }
    }

    #[test]
    fn and_five_children_spills_to_vec() {
        let r = AcceptCondition::and_from((0..5).map(|i| ex(i, 0)));
        match r {
            AcceptCondition::And { items } => assert_eq!(items.len(), 5),
            other => panic!("expected And, got {other}"),
        }
    }

    #[test]
    fn and_dedup_collapses_to_single() {
        let r = AcceptCondition::and_from([ex(1, 0), ex(1, 0), ex(1, 0)]);
        assert_eq!(r, ex(1, 0));
    }

    // ---- Or ----

    #[test]
    fn or_empty_is_never() {
        assert_eq!(AcceptCondition::or_from([]), AcceptCondition::Never);
    }

    #[test]
    fn or_single_passes_through() {
        assert_eq!(AcceptCondition::or_from([ex(1, 0)]), ex(1, 0));
    }

    #[test]
    fn or_with_always_short_circuits() {
        assert_eq!(
            AcceptCondition::or_from([ex(1, 0), AcceptCondition::Always, ex(2, 0)]),
            AcceptCondition::Always
        );
    }

    #[test]
    fn or_drops_never() {
        let r = AcceptCondition::or_from([AcceptCondition::Never, ex(1, 0), AcceptCondition::Never]);
        assert_eq!(r, ex(1, 0));
    }

    #[test]
    fn or_flattens_nested_or() {
        let inner = AcceptCondition::or_from([ex(1, 0), ex(2, 0)]);
        let outer = AcceptCondition::or_from([inner, ex(3, 0)]);
        match outer {
            AcceptCondition::Or { items } => assert_eq!(items.len(), 3),
            other => panic!("expected flat Or, got {other}"),
        }
    }

    #[test]
    fn or_does_not_flatten_and() {
        let inner_and = AcceptCondition::and_from([ex(1, 0), ex(2, 0)]);
        let outer = AcceptCondition::or_from([inner_and.clone(), ex(3, 0)]);
        match outer {
            AcceptCondition::Or { items } => {
                assert_eq!(items.len(), 2);
                assert!(items.contains(&inner_and));
            }
            other => panic!("expected Or, got {other}"),
        }
    }

    #[test]
    fn pair_helpers() {
        assert_eq!(
            AcceptCondition::and_from_pair(ex(1, 0), nex(2, 0)),
            AcceptCondition::and_from([ex(1, 0), nex(2, 0)])
        );
        assert_eq!(
            AcceptCondition::or_from_pair(ex(1, 0), nex(2, 0)),
            AcceptCondition::or_from([ex(1, 0), nex(2, 0)])
        );
    }
}
