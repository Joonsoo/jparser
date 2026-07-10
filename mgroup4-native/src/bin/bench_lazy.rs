//! mgroup4 Phase G-b5 최종 시간 실측 하니스 (설계 §5.2, phase_b §5.2 방법론).
//!
//! 한 (pb, input, n, mode) 셀의 parse-only 시간을 잰다. 진단 env 는 전부 off 로 두고
//! (MG4_SHAPE_STATS/MG4_MERGE_PROFILE/MG4_LAZY_VERIFY 미설정) 실행해야 오버헤드 없는 실측.
//!
//! mode:
//!   off   — 캐시 off (MG4_LAZY_CACHE=0 을 이 프로세스에 세팅해 실행; = Phase B packing baseline)
//!   cold  — 캐시 on, 매 측정 파스가 콜드 캐시 (새 파서 인스턴스마다 캐시 리셋)
//!   warm  — 캐시 on, 측정 파스가 같은 캐시를 재사용 (parse_reusing_cache — 워처 프로덕션 관련성)
//!
//! Usage: bench_lazy <pb> <input> <n> <off|cold|warm> [warmup=2] [measure=11]
//!   결과: median/min (ms) — parse only. is_accepted/kernels_history 는 측정 밖.
//!
//! ★ 방법론: 측정 전 유휴 확인 (경쟁 프로세스 없음), 전부 동기 실행, 셀당 warmup 2 + measure 11.
//!   off 는 별도 프로세스 실행이 정석이나 (env 는 프로세스 시작 전 세팅), 이 바이너리는 mode
//!   인자로 per-run 플래그를 흉내낸다 — 캐시 off 는 파서 생성 시 env 를 안 보고 인자로 강제.

use std::time::Instant;

use mgroup4_native::parser::Mgroup4Parser;
use mgroup4_native::parsing_ctx::ParsingCtx;
use mgroup4_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn stats(mut v: Vec<f64>) -> (f64, f64) {
    v.sort_by(|a, b| a.partial_cmp(b).unwrap());
    let median = v[v.len() / 2];
    let min = v[0];
    (median, min)
}

fn main() {
    let a: Vec<String> = std::env::args().skip(1).collect();
    if a.len() < 4 {
        eprintln!("usage: bench_lazy <pb> <input> <n> <off|cold|warm> [warmup] [measure]");
        std::process::exit(2);
    }
    let pb = &a[0];
    let input_path = &a[1];
    let n: i32 = a[2].parse().unwrap();
    let mode = a[3].as_str();
    let warmup: usize = a.get(4).and_then(|s| s.parse().ok()).unwrap_or(2);
    let measure: usize = a.get(5).and_then(|s| s.parse().ok()).unwrap_or(11);

    let bytes = std::fs::read(pb).expect("read pb");
    let data = Mgroup3ParserData::decode(bytes.as_slice()).expect("decode pb");
    let input = std::fs::read_to_string(input_path).expect("read input");

    // 캐시 off 모드는 env MG4_LAZY_CACHE=0 로 강제 (이 프로세스에서만; 파서 생성 전 세팅).
    // 단일 스레드 main 초반 (다른 스레드 없음) 이라 env 변경 안전 — edition 2024 unsafe 요구.
    unsafe {
        if mode == "off" {
            std::env::set_var("MG4_LAZY_CACHE", "0");
        } else {
            std::env::remove_var("MG4_LAZY_CACHE");
        }
        // 진단 env 는 전부 off (측정 오염 방지).
        for k in ["MG4_SHAPE_STATS", "MG4_MERGE_PROFILE", "MG4_LAZY_VERIFY", "MG4_HIT_TIMING"] {
            std::env::remove_var(k);
        }
    }

    let parser = Mgroup4Parser::new_with_n(data, n);

    // warm 모드: 하나의 캐시를 계속 이월. off/cold: 매 파스가 콜드 캐시 (init_ctx 가 빈 캐시).
    let run_once = |carry: Option<ParsingCtx>| -> (f64, ParsingCtx) {
        let t = Instant::now();
        let ctx = match carry {
            Some(prev) if mode == "warm" => parser.parse_reusing_cache(prev, &input),
            _ => parser.parse(&input),
        }
        .expect("parse");
        let ms = t.elapsed().as_secs_f64() * 1000.0;
        (ms, ctx)
    };

    // 웜업.
    let mut carry: Option<ParsingCtx> = None;
    for _ in 0..warmup {
        let (_, ctx) = run_once(carry.take());
        carry = Some(ctx);
    }
    // 측정.
    let mut times = Vec::with_capacity(measure);
    for _ in 0..measure {
        let (ms, ctx) = run_once(carry.take());
        times.push(ms);
        carry = Some(ctx);
    }
    let (median, min) = stats(times.clone());
    let sd = {
        let mean = times.iter().sum::<f64>() / times.len() as f64;
        (times.iter().map(|x| (x - mean).powi(2)).sum::<f64>() / times.len() as f64).sqrt()
    };
    println!(
        "n={n} mode={mode} chars={} median={median:.2}ms min={min:.2}ms sd={sd:.2}ms (warmup={warmup} measure={measure})",
        input.chars().count()
    );
}
