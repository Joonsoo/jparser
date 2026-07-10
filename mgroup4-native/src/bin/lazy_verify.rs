//! mgroup4 Phase G-b5 게이트 하니스 — 대형 입력(es5/mulang)에 대해:
//!  (1) 캐시 on vs off kernels_history byte-identical (게이트1 확장).
//!  (2) 병렬 검증(MG4_LAZY_VERIFY) 을 켜고 파스 — 히트-vs-재파티션 대조 (게이트3).
//!  (3) 카운터 리포트 (mergesByDepth, reject 계열, 히트율) — Kotlin 교차 (게이트2).
//!  (4) warm 재파스 (같은 파서 캐시 이월) 후에도 byte-identical.
//!
//! Usage: lazy_verify <parserdata.pb> <input.txt>
//!   env: MG4_INTERIOR_N (n), MG4_LAZY_VERIFY (verify mode), MG4_SHAPE_STATS (counters).

use std::collections::BTreeSet;
use std::process::ExitCode;

use mgroup4_native::parser::Mgroup4Parser;
use mgroup4_native::parsing_ctx::KtlibKernel;
use mgroup4_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn serialize(parser: &Mgroup4Parser, ctx: &mgroup4_native::parsing_ctx::ParsingCtx) -> String {
    let accepted = parser.is_accepted(ctx);
    let mut out = String::new();
    out.push_str(if accepted { "ACCEPTED\n" } else { "REJECTED\n" });
    for (g, kernels) in parser.kernels_history(ctx).iter().enumerate() {
        out.push_str(&format!("# gen {}\n", g));
        let sorted: BTreeSet<KtlibKernel> = kernels.iter().copied().collect();
        for k in sorted {
            out.push_str(&format!("{} {} {} {}\n", k.symbol_id, k.pointer, k.begin_gen, k.end_gen));
        }
    }
    out
}

fn main() -> ExitCode {
    let mut args = std::env::args().skip(1);
    let data_path = args.next().expect("usage: lazy_verify <pb> <input>");
    let input_path = args.next().expect("usage: lazy_verify <pb> <input>");
    let n: i32 = std::env::var("MG4_INTERIOR_N").ok().and_then(|s| s.parse().ok()).unwrap_or(1);

    let data_bytes = std::fs::read(&data_path).expect("read pb");
    let input = std::fs::read_to_string(&input_path).expect("read input");

    // cache-on parser (default; MG4_LAZY_CACHE 미설정=on, MG4_LAZY_VERIFY 존중).
    let parser_on = Mgroup4Parser::new_with_n(
        Mgroup3ParserData::decode(data_bytes.as_slice()).unwrap(),
        n,
    );
    let ctx_cold = parser_on.parse(&input).expect("parse cold");
    let out_cold = serialize(&parser_on, &ctx_cold);
    let cold_cache_report = parser_on.report_lazy_cache_stats(&ctx_cold);

    // warm 재파스 (콜드 파스의 워밍업 캐시 이월).
    let ctx_warm = parser_on.parse_reusing_cache(ctx_cold, &input).expect("parse warm");
    let out_warm = serialize(&parser_on, &ctx_warm);
    let warm_cache_report = parser_on.report_lazy_cache_stats(&ctx_warm);

    // cache-off reference: 같은 프로세스에서 별도 파서 (env 로 강제 off 불가 — 새 파서에
    // per-instance 플래그가 없으니, 여기선 env 를 못 바꾸므로 cache off 는 별도 실행에 위임).
    // 대신 cold==warm 을 여기서 검증 (warm 캐시 오염 없음).
    if out_cold != out_warm {
        eprintln!("FAIL: cold != warm (warm cache corrupts output)");
        // 최소 재현: 첫 다른 라인.
        for (a, b) in out_cold.lines().zip(out_warm.lines()) {
            if a != b {
                eprintln!("  cold: {a}\n  warm: {b}");
                break;
            }
        }
        return ExitCode::from(1);
    }

    println!("n={n} input={} ({} chars)", input_path, input.chars().count());
    println!("accepted={}", parser_on.is_accepted(&ctx_warm));
    println!("kernels_history bytes: {}", out_cold.len());
    if std::env::var_os("MG4_SHAPE_STATS").is_some() {
        println!("stats: {}", parser_on.report_mg4_stats());
    }
    println!("cold {cold_cache_report}");
    println!("warm {warm_cache_report}");
    println!("OK cold==warm byte-identical");
    // 출력 전체 md5 대용 — 간단 해시 (교차용).
    let h = out_cold.bytes().fold(1469598103934665603u64, |acc, b| {
        (acc ^ b as u64).wrapping_mul(1099511628211)
    });
    println!("fnv1a={h:016x}");
    ExitCode::SUCCESS
}
