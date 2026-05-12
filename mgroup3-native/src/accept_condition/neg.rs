//! Logical negation. Mirrors Kotlin `AcceptCondition.kt:118, 124, 218-222, 246,
//! 389, 396, 402, 408, 414, 420`.
//!
//! `from_next_gen` is preserved across `NoLongerMatch ↔ NeedLongerMatch`.
//! Negation of composites flips And/Or via De Morgan and recurses through the
//! canonical builders.

use super::AcceptCondition;

impl AcceptCondition {
    pub fn neg(&self) -> AcceptCondition {
        match self {
            AcceptCondition::Always => AcceptCondition::Never,
            AcceptCondition::Never => AcceptCondition::Always,
            AcceptCondition::NoLongerMatch { symbol_id, start_gen, from_next_gen } => {
                AcceptCondition::NeedLongerMatch {
                    symbol_id: *symbol_id,
                    start_gen: *start_gen,
                    from_next_gen: *from_next_gen,
                }
            }
            AcceptCondition::NeedLongerMatch { symbol_id, start_gen, from_next_gen } => {
                AcceptCondition::NoLongerMatch {
                    symbol_id: *symbol_id,
                    start_gen: *start_gen,
                    from_next_gen: *from_next_gen,
                }
            }
            AcceptCondition::NotExists { symbol_id, start_gen } => {
                AcceptCondition::Exists { symbol_id: *symbol_id, start_gen: *start_gen }
            }
            AcceptCondition::Exists { symbol_id, start_gen } => {
                AcceptCondition::NotExists { symbol_id: *symbol_id, start_gen: *start_gen }
            }
            AcceptCondition::Unless { symbol_id, start_gen } => {
                AcceptCondition::OnlyIf { symbol_id: *symbol_id, start_gen: *start_gen }
            }
            AcceptCondition::OnlyIf { symbol_id, start_gen } => {
                AcceptCondition::Unless { symbol_id: *symbol_id, start_gen: *start_gen }
            }
            AcceptCondition::And { items } => {
                AcceptCondition::or_from(items.iter().map(|c| c.neg()))
            }
            AcceptCondition::Or { items } => {
                AcceptCondition::and_from(items.iter().map(|c| c.neg()))
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn nlm(s: i32, g: i32, fng: bool) -> AcceptCondition {
        AcceptCondition::NoLongerMatch { symbol_id: s, start_gen: g, from_next_gen: fng }
    }
    fn need(s: i32, g: i32, fng: bool) -> AcceptCondition {
        AcceptCondition::NeedLongerMatch { symbol_id: s, start_gen: g, from_next_gen: fng }
    }
    fn ex(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::Exists { symbol_id: s, start_gen: g }
    }
    fn nex(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::NotExists { symbol_id: s, start_gen: g }
    }
    fn unless(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::Unless { symbol_id: s, start_gen: g }
    }
    fn only_if(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::OnlyIf { symbol_id: s, start_gen: g }
    }

    #[test]
    fn neg_constants() {
        assert_eq!(AcceptCondition::Always.neg(), AcceptCondition::Never);
        assert_eq!(AcceptCondition::Never.neg(), AcceptCondition::Always);
    }

    #[test]
    fn neg_no_longer_match_preserves_from_next_gen() {
        assert_eq!(nlm(1, 2, false).neg(), need(1, 2, false));
        assert_eq!(nlm(1, 2, true).neg(), need(1, 2, true));
    }

    #[test]
    fn neg_need_longer_match_preserves_from_next_gen() {
        assert_eq!(need(1, 2, false).neg(), nlm(1, 2, false));
        assert_eq!(need(1, 2, true).neg(), nlm(1, 2, true));
    }

    #[test]
    fn neg_exists_family() {
        assert_eq!(ex(3, 5).neg(), nex(3, 5));
        assert_eq!(nex(3, 5).neg(), ex(3, 5));
        assert_eq!(unless(3, 5).neg(), only_if(3, 5));
        assert_eq!(only_if(3, 5).neg(), unless(3, 5));
    }

    #[test]
    fn neg_and_becomes_or() {
        let c = AcceptCondition::and_from([ex(1, 0), nex(2, 0)]);
        let n = c.neg();
        // !(Exists(1) ∧ NotExists(2)) == NotExists(1) ∨ Exists(2)
        let expected = AcceptCondition::or_from([nex(1, 0), ex(2, 0)]);
        assert_eq!(n, expected);
    }

    #[test]
    fn neg_or_becomes_and() {
        let c = AcceptCondition::or_from([ex(1, 0), nex(2, 0)]);
        let n = c.neg();
        let expected = AcceptCondition::and_from([nex(1, 0), ex(2, 0)]);
        assert_eq!(n, expected);
    }

    #[test]
    fn double_neg_is_identity() {
        let cases = [
            AcceptCondition::Always,
            AcceptCondition::Never,
            nlm(1, 2, false),
            nlm(1, 2, true),
            need(0, 0, true),
            ex(3, 5),
            nex(3, 5),
            unless(3, 5),
            only_if(3, 5),
            AcceptCondition::and_from([ex(1, 0), nex(2, 0), ex(3, 0)]),
            AcceptCondition::or_from([ex(1, 0), AcceptCondition::and_from([nex(2, 0), ex(3, 0)])]),
        ];
        for c in cases {
            assert_eq!(c.neg().neg(), c, "double neg failed for {c}");
        }
    }
}
