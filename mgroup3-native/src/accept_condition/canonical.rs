//! Deterministic ordering for `AcceptCondition` children so that `And(X, Y)` and
//! `And(Y, X)` collapse to the same canonical form.
//!
//! Kotlin sorts by `(hashCode, toString)`. Rust uses just `Display` as the key —
//! `Display` already uniquely identifies a condition (the parser round-trips),
//! so this is a total order without needing to mirror JVM hashing.

use super::AcceptCondition;

/// Sort two children into canonical order, returning them as a `(low, high)` pair.
pub fn canonical_pair(a: AcceptCondition, b: AcceptCondition) -> (AcceptCondition, AcceptCondition) {
    if a.to_string() <= b.to_string() {
        (a, b)
    } else {
        (b, a)
    }
}

/// Sort an `&mut Vec` of children into canonical order in place.
pub fn canonical_sort(children: &mut [AcceptCondition]) {
    children.sort_by(|a, b| a.to_string().cmp(&b.to_string()));
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
        assert_eq!(lo.to_string(), "Exists(symbolId=1, startGen=0)");
        assert_eq!(hi.to_string(), "Exists(symbolId=2, startGen=0)");
    }

    #[test]
    fn pair_reorders() {
        // "Exists(symbolId=2..)" > "Exists(symbolId=1..)" lexicographically
        let (lo, hi) = canonical_pair(ex(2, 0), ex(1, 0));
        assert_eq!(lo.to_string(), "Exists(symbolId=1, startGen=0)");
        assert_eq!(hi.to_string(), "Exists(symbolId=2, startGen=0)");
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
        let names: Vec<String> = v.iter().map(|c| c.to_string()).collect();
        // Lex order: Always < Exists(1..) < Exists(2..) < Exists(3..) < Never
        assert_eq!(
            names,
            vec![
                "Always",
                "Exists(symbolId=1, startGen=0)",
                "Exists(symbolId=2, startGen=0)",
                "Exists(symbolId=3, startGen=0)",
                "Never",
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
