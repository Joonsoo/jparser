//! mgroup4 Phase G-b5 — 파스 간 캐시 공유 + LRU 축출 정확성 게이트 (설계 §4 G-b3).
//!
//! (1) 같은 파서로 다중 파스 (콜드 → 캐시 이월 warm) → 각 파스가 단일-파스 결과와
//!     byte-identical (공유가 오염 안 냄).
//! (2) 작은 LRU 예산으로 축출을 강제해도 결과 identical (축출은 재구성만 되므로 정확성
//!     불변 — Kotlin multiParseCacheSharing tinyBudget 경로에 대응).
//! (3) clearLazyCaches 후 재파스도 identical (축출/클리어 정확성).
//!
//! 이 테스트는 캐시가 병합 결정만 상각하고 verdict 재확인이 매 gen 정확성을 보장한다는
//! 계약을 실행으로 증명한다. fixture 는 mgroup3-native 것을 읽기 전용 참조 (복사 금지).

use std::collections::BTreeSet;
use std::fs;
use std::path::PathBuf;

use mgroup4_native::parser::{LazyMergeCache, Mgroup4Parser, ParsingError};
use mgroup4_native::parsing_ctx::{KtlibKernel, ParsingCtx};
use mgroup4_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn fixtures_root() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../mgroup3-native/tests/fixtures/parser")
}

fn serialize(parser: &Mgroup4Parser, ctx: &ParsingCtx) -> String {
    let mut out = String::new();
    out.push_str(if parser.is_accepted(ctx) { "ACCEPTED\n" } else { "REJECTED\n" });
    for (g, kernels) in parser.kernels_history(ctx).iter().enumerate() {
        out.push_str(&format!("# gen {}\n", g));
        let sorted: BTreeSet<KtlibKernel> = kernels.iter().copied().collect();
        for k in sorted {
            out.push_str(&format!("{} {} {} {}\n", k.symbol_id, k.pointer, k.begin_gen, k.end_gen));
        }
    }
    out
}

fn parse_and_serialize(parser: &Mgroup4Parser, input: &str) -> String {
    match parser.parse(input) {
        Ok(ctx) => serialize(parser, &ctx),
        Err(ParsingError::UnexpectedInput { .. } | ParsingError::UnexpectedEof { .. }) => {
            "REJECTED\n".to_string()
        }
    }
}

/// Collect (case_name, data.pb bytes, Vec<input>) for every fixture case with inputs.
fn all_cases() -> Vec<(String, Vec<u8>, Vec<String>)> {
    let root = fixtures_root();
    let mut cases = Vec::new();
    let mut dirs: Vec<PathBuf> = fs::read_dir(&root)
        .unwrap()
        .filter_map(|e| e.ok().map(|e| e.path()))
        .filter(|p| p.is_dir() && p.join("data.pb").exists())
        .collect();
    dirs.sort();
    for case_dir in dirs {
        let name = case_dir.file_name().unwrap().to_string_lossy().into_owned();
        let bytes = fs::read(case_dir.join("data.pb")).unwrap();
        let inputs_dir = case_dir.join("inputs");
        let mut inputs = Vec::new();
        if let Ok(rd) = fs::read_dir(&inputs_dir) {
            let mut ins: Vec<PathBuf> =
                rd.filter_map(|e| e.ok().map(|e| e.path())).filter(|p| p.is_dir()).collect();
            ins.sort();
            for d in ins {
                if let Ok(s) = fs::read_to_string(d.join("input.txt")) {
                    inputs.push(s);
                }
            }
        }
        if !inputs.is_empty() {
            cases.push((name, bytes, inputs));
        }
    }
    cases
}

/// (1) warm 재파스 (캐시 이월) 가 콜드와 byte-identical — 공유가 결과를 오염시키지 않음.
/// n=4 로 병합 패스를 활성화 (n=1 은 캐시 미접촉이라 이 게이트가 무의미).
#[test]
fn cross_parse_cache_sharing_is_identical() {
    let n = 4;
    for (name, bytes, inputs) in all_cases() {
        let parser =
            Mgroup4Parser::new_with_n(Mgroup3ParserData::decode(bytes.as_slice()).unwrap(), n);
        // 콜드 기준선 (각 입력을 독립 파서로 — 완전 콜드 캐시).
        let cold: Vec<String> = inputs
            .iter()
            .map(|inp| {
                let fresh =
                    Mgroup4Parser::new_with_n(Mgroup3ParserData::decode(bytes.as_slice()).unwrap(), n);
                parse_and_serialize(&fresh, inp)
            })
            .collect();

        // warm: 하나의 파서 캐시를 입력들 사이로 이월 (콜드 → warm1 → warm2 ...). 각 warm
        // 파스 결과를 콜드 기준선과 byte-비교. 파스 성공 시 그 ctx 의 (더 워밍업된) 캐시를
        // 다음 입력으로 이월; 파스 실패면 다음 입력은 콜드 파서로 다시 시작.
        let mut carry: Option<ParsingCtx> = None;
        for (i, inp) in inputs.iter().enumerate() {
            let outcome = match carry.take() {
                None => parser.parse(inp),
                Some(prev) => parser.parse_reusing_cache(prev, inp),
            };
            let got = match &outcome {
                Ok(ctx) => serialize(&parser, ctx),
                Err(_) => "REJECTED\n".to_string(),
            };
            assert_eq!(
                cold[i], got,
                "{}: warm re-parse diverged from cold for input #{i}",
                name
            );
            carry = outcome.ok();
        }
    }
}

/// (2) 작은 LRU 예산으로 축출을 강제해도 결과 identical — 축출은 재구성만 되므로 정확성 불변.
/// (3) clear 후 재파스도 identical.
#[test]
fn tiny_budget_eviction_and_clear_are_identical() {
    let n = 4;
    for (name, bytes, inputs) in all_cases() {
        let parser =
            Mgroup4Parser::new_with_n(Mgroup3ParserData::decode(bytes.as_slice()).unwrap(), n);
        for inp in &inputs {
            // 기준: 무제한 캐시 (기본).
            let baseline = parse_and_serialize(&parser, inp);

            // 작은 예산(1) 로 매 삽입마다 축출 — 예산 1 캐시를 주입해 파스.
            let tiny = LazyMergeCache::new(1, 1);
            let chars: Vec<char> = inp.chars().collect();
            let total = chars.len();
            let mut cur: Option<ParsingCtx> = Some(parser.init_ctx_reusing_cache(tiny));
            for (idx, ch) in chars.into_iter().enumerate() {
                let c = cur.take().unwrap();
                match parser.parse_step(c, ch, idx + 1 == total) {
                    Ok(nc) => cur = Some(nc),
                    Err(_) => {
                        cur = None;
                        break;
                    }
                }
            }
            let tiny_out = match cur {
                Some(c) => serialize(&parser, &c),
                None => "REJECTED\n".to_string(),
            };
            assert_eq!(
                baseline, tiny_out,
                "{}: tiny-budget eviction diverged from unlimited cache",
                name
            );

            // (3) clear 후 재파스.
            let mut warm = parser.parse(inp).ok();
            if let Some(w) = warm.as_mut() {
                w.lazy_cache.clear();
            }
            let after_clear = match warm {
                Some(w) => parser.parse_reusing_cache(w, inp).map(|c| serialize(&parser, &c)).ok(),
                None => None,
            };
            if let Some(ac) = after_clear {
                assert_eq!(baseline, ac, "{}: post-clear re-parse diverged", name);
            }
        }
    }
}
