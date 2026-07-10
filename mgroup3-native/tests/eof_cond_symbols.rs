//! Pins `ParserDataPlain::eof_cond_symbols` (the structural "anychar single-char"
//! detection that eager EOF resolution folds against), giving detection parity
//! with the Kotlin `EofCondSymbolsDetectionTest`:
//!   - mulang (real grammar with `EOF = !.`): exactly {20}, and 20 is AnyChar.
//!   - structure-only committed fixtures (no `!.`): the empty set.
//! mulang's data.pb is a large gitignored generated fixture; when absent the
//! mulang case is skipped (structure fixtures always run).

use std::collections::BTreeSet;
use std::path::PathBuf;

use mgroup3_native::parser_data::ParserDataPlain;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use mgroup3_native::proto::com::giyeok::jparser::proto::term_group::TermGroup as TermGroupOneof;
use prost::Message;

fn manifest_dir() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
}

fn load_plain(rel: &str) -> Option<ParserDataPlain> {
    let pb = manifest_dir().join(rel);
    let bytes = std::fs::read(pb).ok()?;
    let proto = Mgroup3ParserData::decode(bytes.as_slice()).expect("decode data.pb");
    Some(ParserDataPlain::from_proto(proto))
}

/// True iff sym's starter group is a single term action over *all* characters —
/// the structural signature of AnyChar (the `.` in `EOF = !.`).
fn is_anychar_symbol(plain: &ParserDataPlain, sym: i32) -> bool {
    let Some(info) = plain.path_roots.get(&sym) else { return false };
    let Some(actions) = plain.term_actions.get(&info.milestone_group_id) else { return false };
    if actions.len() != 1 {
        return false;
    }
    match actions[0].term_group.term_group.as_ref() {
        Some(TermGroupOneof::AllCharsExcluding(ace)) => match ace.excluding.as_ref() {
            None => true,
            Some(cg) => cg.unicode_categories.is_empty() && cg.chars.is_empty(),
        },
        _ => false,
    }
}

#[test]
fn mulang_has_exactly_one_anychar_eof_symbol() {
    let Some(plain) = load_plain("tests/fixtures/parser_generated/mulang/data.pb") else {
        eprintln!(
            "note: mulang generated fixture missing — run `bibix4 runMgroup3FixtureGen`; skipping"
        );
        return;
    };
    let got: BTreeSet<i32> = plain.eof_cond_symbols.iter().copied().collect();
    assert_eq!(
        got,
        BTreeSet::from([20]),
        "mulang eof_cond_symbols drifted from the pinned {{20}}"
    );
    assert!(
        is_anychar_symbol(&plain, 20),
        "sym20 detected as eof cond symbol but is not structurally AnyChar"
    );
}

#[test]
fn structure_only_grammars_detect_no_eof_symbols() {
    for case in ["simple_sequence", "repeat0", "nested_repeat", "choice", "optional"] {
        let plain = load_plain(&format!("tests/fixtures/parser/{case}/data.pb"))
            .unwrap_or_else(|| panic!("committed fixture {case}/data.pb missing"));
        assert!(
            plain.eof_cond_symbols.is_empty(),
            "{case} unexpectedly detected eof cond symbols: {:?}",
            plain.eof_cond_symbols
        );
    }
}
