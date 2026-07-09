//! Deterministic ordering for `AcceptCondition` children so that `And(X, Y)` and
//! `And(Y, X)` collapse to the same canonical form.
//!
//! Kotlin sorts by `(hashCode, toString)`. Rust uses the derived `Ord` directly
//! on `AcceptCondition`: any total order suffices — what matters is that
//! `and_from([X, Y])` and `and_from([Y, X])` produce equal values. Sorting on
//! `Ord` avoids the String allocations the previous Display-based key forced.

use super::AcceptCondition;

/// Sort two children into canonical order, returning them as a `(low, high)` pair.
pub fn canonical_pair(a: AcceptCondition, b: AcceptCondition) -> (AcceptCondition, AcceptCondition) {
    if a <= b {
        (a, b)
    } else {
        (b, a)
    }
}

/// Sort an `&mut Vec` of children into canonical order in place.
pub fn canonical_sort(children: &mut [AcceptCondition]) {
    children.sort();
}

#[cfg(test)]
mod tests {
    use super::*;

    fn ex(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::Exists { symbol_id: s, start_gen: g }
    }

    #[test]
    fn pair_in_order() {
        let (lo, hi) = canonical_pair(ex(1, 0), ex(2, 0));
        assert_eq!(lo, ex(1, 0));
        assert_eq!(hi, ex(2, 0));
    }

    #[test]
    fn pair_reorders() {
        let (lo, hi) = canonical_pair(ex(2, 0), ex(1, 0));
        assert_eq!(lo, ex(1, 0));
        assert_eq!(hi, ex(2, 0));
    }

    #[test]
    fn sort_multiple() {
        let mut v = vec![
            ex(3, 0),
            ex(1, 0),
            AcceptCondition::Always,
            ex(2, 0),
            AcceptCondition::Never,
        ];
        canonical_sort(&mut v);
        // Derived Ord: variant index first (Always(0), Never(1), Exists(5)),
        // then fields lex-on-tuple for same-variant entries.
        assert_eq!(
            v,
            vec![
                AcceptCondition::Always,
                AcceptCondition::Never,
                ex(1, 0),
                ex(2, 0),
                ex(3, 0),
            ]
        );
    }

    #[test]
    fn sort_stable_for_equal_keys() {
        // Two equal conditions sort adjacent; deterministic.
        let mut v = vec![ex(1, 0), ex(1, 0)];
        canonical_sort(&mut v);
        assert_eq!(v[0], v[1]);
    }
}
