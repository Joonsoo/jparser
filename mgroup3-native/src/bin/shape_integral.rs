//! Area-under-curve profiler: per-char parse that reports the INTEGRAL of
//! live shapes / cond nodes / action applications over the whole input (not
//! just the peak), so we can compare files whose peak is similar but whose
//! sustained baseline differs. profile_steps 가 peak/top-N 중심인 것을 보완.
//!
//! 용도: mgroup4 Phase 0 의 P1 (interior-merge ceiling) 기저 계측 —
//! docs/mgroup4_bounded_interior_groups.md §3. 첫 사용례: mulang ccgen.mu
//! 조사 (2026-07-09) — "시간 ∝ live shapes (corr 0.997)" 와 "ccgen 은 peak
//! 이 아니라 sustained 고-shape 대역이 원인 (파스 시간의 65.6% 가 ≥50-shape
//! 스텝)" 을 이 적분 계측으로 확인했다.
//!
//! Usage: shape_integral <parserdata.pb> <input.txt>

use std::time::Instant;

use mgroup3_native::accept_condition::AcceptCondition;
use mgroup3_native::parser::Mgroup3Parser;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn cond_size(c: &AcceptCondition) -> usize {
    match c {
        AcceptCondition::And { items } | AcceptCondition::Or { items } => {
            1 + items.iter().map(cond_size).sum::<usize>()
        }
        _ => 1,
    }
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut args = std::env::args().skip(1);
    let data_path = args.next().expect("usage: shape_integral <parserdata.pb> <input.txt>");
    let input_path = args.next().expect("usage: shape_integral <parserdata.pb> <input.txt>");

    let data_bytes = std::fs::read(&data_path)?;
    let data = Mgroup3ParserData::decode(data_bytes.as_slice())?;
    let parser = Mgroup3Parser::new(data);
    let input = std::fs::read_to_string(&input_path)?;
    let chars: Vec<char> = input.chars().collect();
    let total = chars.len();

    let mut ctx = parser.init_ctx();
    let mut sum_shapes: u64 = 0;
    let mut sum_roots: u64 = 0;
    let mut sum_cond: u64 = 0;
    let mut sum_apps: u64 = 0;
    let mut sum_micros: u128 = 0;
    let mut max_shapes: usize = 0;
    // shape histogram buckets
    let mut buckets = [0u64; 8]; // 0,1-50,51-100,101-200,201-300,301-400,401-500,501+
    let t_all = Instant::now();
    for (idx, c) in chars.iter().enumerate() {
        let t = Instant::now();
        ctx = parser.parse_step(ctx, *c, idx + 1 == total)?;
        sum_micros += t.elapsed().as_micros();
        let roots = ctx.paths.len();
        let shapes: usize = ctx.paths.values().map(|m| m.len()).sum();
        let mut cond_nodes = 0usize;
        for pm in ctx.paths.values() {
            for cond in pm.values() {
                cond_nodes += cond_size(cond);
            }
        }
        let apps = ctx.history.last().map(|e| e.action_applications.len()).unwrap_or(0);
        sum_shapes += shapes as u64;
        sum_roots += roots as u64;
        sum_cond += cond_nodes as u64;
        sum_apps += apps as u64;
        max_shapes = max_shapes.max(shapes);
        let b = match shapes {
            0 => 0,
            1..=50 => 1,
            51..=100 => 2,
            101..=200 => 3,
            201..=300 => 4,
            301..=400 => 5,
            401..=500 => 6,
            _ => 7,
        };
        buckets[b] += 1;
    }
    let wall = t_all.elapsed();
    println!("input: {} chars", total);
    println!("wall: {:?}  (sum_step_micros={} = {:.1}ms)", wall, sum_micros, sum_micros as f64 / 1000.0);
    println!("INTEGRALS (sum over all steps):");
    println!("  shapes:    total={:>12}  mean={:>8.1}/step  max={}", sum_shapes, sum_shapes as f64 / total as f64, max_shapes);
    println!("  roots:     total={:>12}  mean={:>8.2}/step", sum_roots, sum_roots as f64 / total as f64);
    println!("  condNodes: total={:>12}  mean={:>8.1}/step", sum_cond, sum_cond as f64 / total as f64);
    println!("  apps:      total={:>12}  mean={:>8.1}/step", sum_apps, sum_apps as f64 / total as f64);
    let labels = ["=0", "1-50", "51-100", "101-200", "201-300", "301-400", "401-500", "501+"];
    println!("  shape histogram (fraction of steps):");
    for (i, l) in labels.iter().enumerate() {
        println!("    {:>8}: {:>6} steps ({:>5.1}%)", l, buckets[i], buckets[i] as f64 * 100.0 / total as f64);
    }
    Ok(())
}
