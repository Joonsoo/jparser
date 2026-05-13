//! Encode a parse outcome into `Mgroup3ParseResult` proto bytes for transport
//! to the JVM side. The transport channel itself (JNI / FFM / subprocess) is
//! out of scope here — this module only produces the `Vec<u8>` payload.
//!
//! See `mgroup3/schema/proto/Mgroup3ParserResult.proto` for the wire format
//! and `mgroup3/nativeParser/kotlin/.../Mgroup3NativeResult.kt` for the
//! Kotlin-side decoder.

use prost::Message as ProstMessage;

use crate::parser::Mgroup3Parser;
use crate::parser::ParsingError;
use crate::parsing_ctx::ParsingCtx;
use crate::proto::com::giyeok::jparser::mgroup3::proto::{
    parse_error_msg, KernelGen, KernelMsg, KernelsHistoryMsg, Mgroup3ParseResult, ParseErrorMsg,
};
use crate::proto::com::giyeok::jparser::mgroup3::proto::mgroup3_parse_result::Outcome;
use crate::term_group::TermSet;

/// Encode a parse outcome into `Mgroup3ParseResult` bytes.
///
/// - `Ok(ctx)` with `is_accepted(ctx) == true` → `Accepted` with sorted
///   `kernels_history`.
/// - `Ok(ctx)` with `is_accepted(ctx) == false` → `UnexpectedEof` synthesized
///   from `expected_inputs_of(ctx)` at `ctx.gen_idx/line/col`. Mirrors
///   `Mgroup3Parser.kt:806-812` (`parseOrThrow`).
/// - `Err(UnexpectedInput { .. })` / `Err(UnexpectedEof { .. })` → typed
///   `ParseErrorMsg`.
pub fn encode_parse_result(
    parser: &Mgroup3Parser,
    outcome: Result<&ParsingCtx, &ParsingError>,
) -> Vec<u8> {
    let result = build_result(parser, outcome);
    result.encode_to_vec()
}

/// Same as `encode_parse_result` but returns the proto message instead of
/// bytes — useful in tests that want to assert structural fields directly.
pub fn build_result(
    parser: &Mgroup3Parser,
    outcome: Result<&ParsingCtx, &ParsingError>,
) -> Mgroup3ParseResult {
    match outcome {
        Ok(ctx) => {
            if parser.is_accepted(ctx) {
                let history = build_history_msg(parser, ctx);
                Mgroup3ParseResult { accepted: true, outcome: Some(Outcome::History(history)) }
            } else {
                // Parsed cleanly but the start symbol didn't satisfy any accept
                // condition. Fold into UnexpectedEof per parseOrThrow.
                let expected = parser.expected_inputs_of(ctx);
                let error = ParseErrorMsg {
                    error: Some(parse_error_msg::Error::UnexpectedEof(
                        parse_error_msg::UnexpectedEof {
                            loc: ctx.gen_idx,
                            line: ctx.line,
                            col: ctx.col,
                            expected_term_groups: term_set_into_groups(expected),
                        },
                    )),
                };
                Mgroup3ParseResult { accepted: false, outcome: Some(Outcome::Error(error)) }
            }
        }
        Err(ParsingError::UnexpectedInput { loc, line, col, expected, actual }) => {
            let error = ParseErrorMsg {
                error: Some(parse_error_msg::Error::UnexpectedInput(
                    parse_error_msg::UnexpectedInput {
                        loc: *loc,
                        line: *line,
                        col: *col,
                        expected_term_groups: term_set_into_groups(expected.clone()),
                        actual_codepoint: u32::from(*actual) as i32,
                    },
                )),
            };
            Mgroup3ParseResult { accepted: false, outcome: Some(Outcome::Error(error)) }
        }
        Err(ParsingError::UnexpectedEof { loc, line, col, expected }) => {
            let error = ParseErrorMsg {
                error: Some(parse_error_msg::Error::UnexpectedEof(parse_error_msg::UnexpectedEof {
                    loc: *loc,
                    line: *line,
                    col: *col,
                    expected_term_groups: term_set_into_groups(expected.clone()),
                })),
            };
            Mgroup3ParseResult { accepted: false, outcome: Some(Outcome::Error(error)) }
        }
    }
}

fn build_history_msg(parser: &Mgroup3Parser, ctx: &ParsingCtx) -> KernelsHistoryMsg {
    let mut entries: Vec<KernelGen> = Vec::with_capacity(ctx.history.len());
    for kernels in parser.kernels_history(ctx) {
        let mut kernel_msgs: Vec<KernelMsg> = kernels
            .iter()
            .map(|k| KernelMsg {
                symbol_id: k.symbol_id,
                pointer: k.pointer,
                begin_gen: k.begin_gen,
                end_gen: k.end_gen,
            })
            .collect();
        // Sort so two encoders produce byte-identical output.
        kernel_msgs.sort_by(|a, b| {
            (a.symbol_id, a.pointer, a.begin_gen, a.end_gen).cmp(&(
                b.symbol_id,
                b.pointer,
                b.begin_gen,
                b.end_gen,
            ))
        });
        entries.push(KernelGen { kernels: kernel_msgs });
    }
    KernelsHistoryMsg { entries }
}

fn term_set_into_groups(term_set: TermSet) -> Vec<crate::proto::com::giyeok::jparser::proto::TermGroup> {
    term_set.term_groups
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::proto::com::giyeok::jparser::mgroup3::proto::accept_condition_template::Condition;
    use crate::proto::com::giyeok::jparser::mgroup3::proto::{
        AcceptConditionTemplate, Mgroup3ParserData, PathRootInfo,
    };
    use crate::proto::com::giyeok::jparser::proto::term_group::TermGroup as TermGroupOneof;
    use crate::proto::com::giyeok::jparser::proto::{CharsGroup, TermGroup};

    fn empty_data_with_start(start: i32) -> Mgroup3ParserData {
        let mut d = Mgroup3ParserData::default();
        d.start_symbol_id = start;
        d.path_roots.insert(
            start,
            PathRootInfo {
                symbol_id: start,
                milestone_group_id: 0,
                initial_cond_symbol_ids: vec![],
                self_finish_accept_condition: None,
                parsing_actions: None,
            },
        );
        d
    }

    fn always_self_finish_data(start: i32) -> Mgroup3ParserData {
        let mut d = empty_data_with_start(start);
        d.path_roots.get_mut(&start).unwrap().self_finish_accept_condition =
            Some(AcceptConditionTemplate { condition: Some(Condition::Always(())) });
        d
    }

    fn term_group_chars(chars: &str) -> TermGroup {
        TermGroup {
            term_group: Some(TermGroupOneof::CharsGroup(CharsGroup {
                unicode_categories: vec![],
                excluding_chars: String::new(),
                chars: chars.to_string(),
            })),
        }
    }

    fn decode(bytes: &[u8]) -> Mgroup3ParseResult {
        Mgroup3ParseResult::decode(bytes).expect("decode")
    }

    #[test]
    fn accepted_initial_finish() {
        // Self-finish at gen 0 produces an immediate Accepted result with a
        // single-gen history.
        let parser = Mgroup3Parser::new(always_self_finish_data(7));
        let ctx = parser.init_ctx();
        let bytes = encode_parse_result(&parser, Ok(&ctx));
        let msg = decode(&bytes);
        assert!(msg.accepted);
        match msg.outcome {
            Some(Outcome::History(h)) => {
                assert_eq!(h.entries.len(), 1);
                let gen0 = &h.entries[0];
                assert!(gen0.kernels.iter().any(|k| k.symbol_id == 7 && k.pointer == 1));
            }
            other => panic!("expected History outcome, got {:?}", other),
        }
    }

    #[test]
    fn accepted_history_is_sorted_within_each_gen() {
        // Build a synthetic ctx with multiple finished kernels in a single gen,
        // entered in non-canonical order. The accepted branch is triggered by
        // self-finish (start sym kernel (7, 1, 0) with Always); we add extra
        // finished records at gen 0 with deliberately out-of-order
        // (symbol_id, pointer) so the encoder's sort path is exercised.
        use crate::accept_condition::AcceptCondition;
        use crate::parsing_ctx::{FinishedKernelRecord, Kernel};

        let parser = Mgroup3Parser::new(always_self_finish_data(7));
        let mut ctx = parser.init_ctx();
        // init_ctx already inserted (7, 1, 0) at gen 0. Add a few more in
        // intentionally bad order. These won't affect is_accepted (which
        // checks the start-sym finish), but they will go through
        // kernels_history → sorted KernelGen.
        let entry = ctx.history.last_mut().unwrap();
        entry.finished_kernels.push(FinishedKernelRecord {
            kernel: Kernel::new(9, 0, 0),
            condition: AcceptCondition::Always,
        });
        entry.finished_kernels.push(FinishedKernelRecord {
            kernel: Kernel::new(3, 0, 0),
            condition: AcceptCondition::Always,
        });
        entry.finished_kernels.push(FinishedKernelRecord {
            kernel: Kernel::new(5, 2, 0),
            condition: AcceptCondition::Always,
        });

        let bytes = encode_parse_result(&parser, Ok(&ctx));
        let msg = decode(&bytes);
        let h = match msg.outcome.unwrap() {
            Outcome::History(h) => h,
            other => panic!("expected History, got {:?}", other),
        };
        let gen0 = &h.entries[0].kernels;
        let pairs: Vec<_> = gen0
            .iter()
            .map(|k| (k.symbol_id, k.pointer, k.begin_gen, k.end_gen))
            .collect();
        let mut sorted = pairs.clone();
        sorted.sort();
        assert_eq!(pairs, sorted, "gen 0 kernels are not in canonical order");
    }

    #[test]
    fn unexpected_input_round_trip() {
        // Empty grammar + "ab": first char 'a' produces empty main paths and
        // is_last_input=false, so step 7 throws at loc=0.
        let parser = Mgroup3Parser::new(empty_data_with_start(1));
        let err = parser.parse("ab").expect_err("ab should reject on empty grammar");
        let bytes = encode_parse_result(&parser, Err(&err));
        let msg = decode(&bytes);
        assert!(!msg.accepted);
        match msg.outcome.unwrap() {
            Outcome::Error(e) => match e.error.unwrap() {
                parse_error_msg::Error::UnexpectedInput(ui) => {
                    assert_eq!(ui.loc, 0);
                    assert_eq!(ui.line, 0);
                    assert_eq!(ui.col, 0);
                    assert_eq!(ui.actual_codepoint, 'a' as i32);
                }
                other => panic!("expected UnexpectedInput, got {:?}", other),
            },
            other => panic!("expected Error outcome, got {:?}", other),
        }
    }

    #[test]
    fn unexpected_eof_round_trip_via_synthetic() {
        // Build an UnexpectedEof error by hand and round-trip it. We construct
        // the error directly because no current synthetic data fixture
        // naturally produces UnexpectedEof — parseOrThrow does, but parse()
        // alone returns Ok(unaccepted) which the encoder folds into
        // UnexpectedEof through `is_accepted=false`. Test both paths.
        let parser = Mgroup3Parser::new(empty_data_with_start(1));
        let term_set = TermSet { term_groups: vec![term_group_chars("xyz")] };
        let err = ParsingError::UnexpectedEof {
            loc: 5,
            line: 1,
            col: 2,
            expected: term_set,
        };
        let bytes = encode_parse_result(&parser, Err(&err));
        let msg = decode(&bytes);
        assert!(!msg.accepted);
        match msg.outcome.unwrap() {
            Outcome::Error(e) => match e.error.unwrap() {
                parse_error_msg::Error::UnexpectedEof(eof) => {
                    assert_eq!(eof.loc, 5);
                    assert_eq!(eof.line, 1);
                    assert_eq!(eof.col, 2);
                    assert_eq!(eof.expected_term_groups.len(), 1);
                }
                other => panic!("expected UnexpectedEof, got {:?}", other),
            },
            other => panic!("expected Error outcome, got {:?}", other),
        }
    }

    #[test]
    fn parsed_but_not_accepted_folds_into_unexpected_eof() {
        // Empty grammar + 'a' → parse returns Ok(ctx) but is_accepted=false.
        // The encoder folds this into UnexpectedEof at ctx.gen_idx.
        let parser = Mgroup3Parser::new(empty_data_with_start(1));
        let ctx = parser.parse("a").expect("single char succeeds");
        assert!(!parser.is_accepted(&ctx));
        let bytes = encode_parse_result(&parser, Ok(&ctx));
        let msg = decode(&bytes);
        assert!(!msg.accepted);
        match msg.outcome.unwrap() {
            Outcome::Error(e) => match e.error.unwrap() {
                parse_error_msg::Error::UnexpectedEof(eof) => {
                    assert_eq!(eof.loc, ctx.gen_idx);
                    assert_eq!(eof.line, ctx.line);
                    assert_eq!(eof.col, ctx.col);
                }
                other => panic!("expected UnexpectedEof, got {:?}", other),
            },
            other => panic!("expected Error outcome, got {:?}", other),
        }
    }

    #[test]
    fn accepted_field_matches_outcome() {
        // Invariant: accepted=true iff outcome=History.
        let parser = Mgroup3Parser::new(always_self_finish_data(7));
        let ctx = parser.init_ctx();
        let msg_acc = build_result(&parser, Ok(&ctx));
        assert!(msg_acc.accepted);
        assert!(matches!(msg_acc.outcome, Some(Outcome::History(_))));

        let parser2 = Mgroup3Parser::new(empty_data_with_start(1));
        let err = parser2.parse("ab").unwrap_err();
        let msg_rej = build_result(&parser2, Err(&err));
        assert!(!msg_rej.accepted);
        assert!(matches!(msg_rej.outcome, Some(Outcome::Error(_))));
    }
}
