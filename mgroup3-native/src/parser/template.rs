//! `AcceptConditionTemplate` → `AcceptCondition` materialization.
//!
//! Port of `Mgroup3Parser.kt:853-891` (the `toAcceptCondition` extension) plus
//! `resolveGen` (`:379-386`). The template is the static proto representation;
//! materialization happens at call sites that know `prev_gen / mid_gen / gen /
//! grand_gen` (the four reference points for the `KernelTemplateGen` enum).

use crate::accept_condition::AcceptCondition;
use crate::proto::com::giyeok::jparser::mgroup3::proto::accept_condition_template::Condition;
use crate::proto::com::giyeok::jparser::mgroup3::proto::{
    AcceptConditionTemplate, KernelTemplateGen,
};

/// Resolve a `KernelTemplateGen` tag against the four reference gens.
/// Mirrors `Mgroup3Parser.kt:379-386`. Unknown / default falls back to `prev`.
pub fn resolve_gen(tag: KernelTemplateGen, prev: i32, mid: i32, next: i32, grand: i32) -> i32 {
    match tag {
        KernelTemplateGen::Curr => prev,
        KernelTemplateGen::Mid => mid,
        KernelTemplateGen::Next => next,
        KernelTemplateGen::Grand => grand,
    }
}

/// Resolve a `KernelTemplateGen` stored as `i32` (prost decodes proto3 enums as
/// `i32` on message fields).
pub fn resolve_gen_i32(tag_value: i32, prev: i32, mid: i32, next: i32, grand: i32) -> i32 {
    match KernelTemplateGen::try_from(tag_value) {
        Ok(t) => resolve_gen(t, prev, mid, next, grand),
        Err(_) => prev,
    }
}

/// Build an `AcceptCondition` from its template, resolving `start_gen` refs
/// against the four reference points. Mirrors `Mgroup3Parser.kt:853-891`.
///
/// `prev_gen` is the parent milestone's gen (Kotlin's `prevGen`); `mid_gen` is
/// `ctx.gen` before the current input; `gen` is after the current input;
/// `grand_gen` is one level further up the milestone chain.
pub fn build_condition(
    tpl: &AcceptConditionTemplate,
    prev_gen: i32,
    mid_gen: i32,
    gen_idx: i32,
    grand_gen: i32,
) -> AcceptCondition {
    let cond = tpl.condition.as_ref().expect("AcceptConditionTemplate.condition not set");
    match cond {
        Condition::Always(_) => AcceptCondition::Always,
        Condition::And(multi) => AcceptCondition::and_from(
            multi.conditions.iter().map(|c| build_condition(c, prev_gen, mid_gen, gen_idx, grand_gen)),
        ),
        Condition::Or(multi) => AcceptCondition::or_from(
            multi.conditions.iter().map(|c| build_condition(c, prev_gen, mid_gen, gen_idx, grand_gen)),
        ),
        Condition::NoLongerMatch(t) => {
            let start_gen = resolve_gen_i32(t.start_gen, prev_gen, mid_gen, gen_idx, grand_gen);
            AcceptCondition::NoLongerMatch {
                symbol_id: t.symbol_id,
                start_gen,
                // Kotlin sets fromNextGen=true at materialization time.
                // evolveAcceptCondition strips the flag on its first pass.
                from_next_gen: true,
            }
        }
        Condition::LookaheadFound(t) => AcceptCondition::Exists {
            symbol_id: t.symbol_id,
            start_gen: resolve_gen_i32(t.start_gen, prev_gen, mid_gen, gen_idx, grand_gen),
        },
        Condition::LookaheadNotfound(t) => AcceptCondition::NotExists {
            symbol_id: t.symbol_id,
            start_gen: resolve_gen_i32(t.start_gen, prev_gen, mid_gen, gen_idx, grand_gen),
        },
        Condition::Except(t) => AcceptCondition::Unless {
            symbol_id: t.symbol_id,
            start_gen: resolve_gen_i32(t.start_gen, prev_gen, mid_gen, gen_idx, grand_gen),
        },
        Condition::Join(t) => AcceptCondition::OnlyIf {
            symbol_id: t.symbol_id,
            start_gen: resolve_gen_i32(t.start_gen, prev_gen, mid_gen, gen_idx, grand_gen),
        },
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::proto::com::giyeok::jparser::mgroup3::proto::{
        ExceptTemplate, JoinTemplate, LookaheadFoundTemplate, LookaheadNotFoundTemplate,
        MultiAcceptConditions, NoLongerMatchTemplate,
    };

    fn tpl(c: Condition) -> AcceptConditionTemplate {
        AcceptConditionTemplate { condition: Some(c) }
    }

    #[test]
    fn resolve_gen_arms() {
        assert_eq!(resolve_gen(KernelTemplateGen::Curr, 10, 20, 30, 40), 10);
        assert_eq!(resolve_gen(KernelTemplateGen::Mid, 10, 20, 30, 40), 20);
        assert_eq!(resolve_gen(KernelTemplateGen::Next, 10, 20, 30, 40), 30);
        assert_eq!(resolve_gen(KernelTemplateGen::Grand, 10, 20, 30, 40), 40);
    }

    #[test]
    fn always() {
        let t = tpl(Condition::Always(()));
        assert_eq!(build_condition(&t, 0, 0, 0, 0), AcceptCondition::Always);
    }

    #[test]
    fn no_longer_match_sets_from_next_gen_true() {
        let t = tpl(Condition::NoLongerMatch(NoLongerMatchTemplate {
            symbol_id: 5,
            start_gen: KernelTemplateGen::Curr as i32,
        }));
        let result = build_condition(&t, 7, 0, 0, 0);
        assert_eq!(
            result,
            AcceptCondition::NoLongerMatch { symbol_id: 5, start_gen: 7, from_next_gen: true }
        );
    }

    #[test]
    fn lookahead_found_to_exists() {
        let t = tpl(Condition::LookaheadFound(LookaheadFoundTemplate {
            symbol_id: 9,
            start_gen: KernelTemplateGen::Mid as i32,
        }));
        let result = build_condition(&t, 0, 11, 0, 0);
        assert_eq!(result, AcceptCondition::Exists { symbol_id: 9, start_gen: 11 });
    }

    #[test]
    fn lookahead_notfound_to_not_exists() {
        let t = tpl(Condition::LookaheadNotfound(LookaheadNotFoundTemplate {
            symbol_id: 9,
            start_gen: KernelTemplateGen::Next as i32,
        }));
        let result = build_condition(&t, 0, 0, 12, 0);
        assert_eq!(result, AcceptCondition::NotExists { symbol_id: 9, start_gen: 12 });
    }

    #[test]
    fn except_to_unless() {
        let t = tpl(Condition::Except(ExceptTemplate {
            symbol_id: 9,
            start_gen: KernelTemplateGen::Grand as i32,
        }));
        let result = build_condition(&t, 0, 0, 0, 13);
        assert_eq!(result, AcceptCondition::Unless { symbol_id: 9, start_gen: 13 });
    }

    #[test]
    fn join_to_only_if() {
        let t = tpl(Condition::Join(JoinTemplate {
            symbol_id: 9,
            start_gen: KernelTemplateGen::Curr as i32,
        }));
        let result = build_condition(&t, 14, 0, 0, 0);
        assert_eq!(result, AcceptCondition::OnlyIf { symbol_id: 9, start_gen: 14 });
    }

    #[test]
    fn and_recurses() {
        let t = tpl(Condition::And(MultiAcceptConditions {
            conditions: vec![
                tpl(Condition::Always(())),
                tpl(Condition::LookaheadFound(LookaheadFoundTemplate {
                    symbol_id: 1,
                    start_gen: KernelTemplateGen::Curr as i32,
                })),
            ],
        }));
        let result = build_condition(&t, 5, 0, 0, 0);
        // Always ∧ Exists(1, 5) → Exists(1, 5)
        assert_eq!(result, AcceptCondition::Exists { symbol_id: 1, start_gen: 5 });
    }

    #[test]
    fn or_recurses() {
        let t = tpl(Condition::Or(MultiAcceptConditions {
            conditions: vec![
                tpl(Condition::LookaheadFound(LookaheadFoundTemplate {
                    symbol_id: 1,
                    start_gen: KernelTemplateGen::Curr as i32,
                })),
                tpl(Condition::LookaheadFound(LookaheadFoundTemplate {
                    symbol_id: 2,
                    start_gen: KernelTemplateGen::Curr as i32,
                })),
            ],
        }));
        let result = build_condition(&t, 5, 0, 0, 0);
        let expected = AcceptCondition::or_from([
            AcceptCondition::Exists { symbol_id: 1, start_gen: 5 },
            AcceptCondition::Exists { symbol_id: 2, start_gen: 5 },
        ]);
        assert_eq!(result, expected);
    }
}
