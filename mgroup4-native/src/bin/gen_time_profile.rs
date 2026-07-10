//! Per-generation time profile — Probe B (mgroup4 Phase G §1, `phase_g_plan.md`).
//!
//! Records, for every gen of a parse, `(elapsed_nanos, main_root_live_shapes,
//! total_live_shapes)` and prints it as CSV. Forces `MG4_INTERIOR_N=1`
//! internally regardless of the ambient env — this is the "current engine"
//! path (mgroup4 merge pass never runs), matching the population that the
//! live-shape-proportional-time regression (Phase G's `L` coefficient) is
//! about. `mg4_shape_stats.rs` cross-checks the *merged-vs-base* ratio at
//! arbitrary n; this bin is single-purpose: per-gen timing at n=1.
//!
//! Timing: `Instant::now()` around each `parse_step` call only (load/init and
//! post-parse bookkeeping — `ctx.paths` lookups for the shape counts — happen
//! outside the timed span, immediately after, so they don't pollute the
//! per-step timer). Because a single gen can be sub-microsecond, the
//! `Instant::now()` pair itself has non-negligible overhead relative to the
//! signal; this bin also measures that overhead directly (empty back-to-back
//! `Instant::now()` calls, same iteration count as the real run) and reports
//! it on stderr so the analysis can judge how much of small `elapsed_nanos`
//! values is measurement noise vs signal.
//!
//! Usage: gen_time_profile <parserdata.pb> <input.txt> [csv_out]
//!   csv_out defaults to stdout. Columns: gen,elapsed_ns,main_shapes,total_shapes
//!   Overhead calibration and summary go to stderr.

use std::io::Write as _;
use std::time::Instant;

use mgroup4_native::parser::Mgroup4Parser;
use mgroup4_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Force n=1: no interior-group merge pass runs, so this is exactly the
    // current (pre-mgroup4) engine's per-step cost. Set before touching any
    // other env so a caller's ambient MG4_INTERIOR_N can't leak in.
    unsafe {
        std::env::set_var("MG4_INTERIOR_N", "1");
    }

    let mut args = std::env::args().skip(1);
    let data_path = args
        .next()
        .expect("usage: gen_time_profile <parserdata.pb> <input.txt> [csv_out]");
    let input_path = args
        .next()
        .expect("usage: gen_time_profile <parserdata.pb> <input.txt> [csv_out]");
    let csv_out_path = args.next();

    let data_bytes = std::fs::read(&data_path)?;
    let data = Mgroup3ParserData::decode(data_bytes.as_slice())?;
    let parser = Mgroup4Parser::new_with_n(data, 1);
    assert_eq!(parser.interior_group_max_depth(), 1, "n=1 required for Probe B (current-engine path)");

    let input = std::fs::read_to_string(&input_path)?;
    let chars: Vec<char> = input.chars().collect();
    let total = chars.len();

    // ---- calibration: empty Instant::now() pair overhead, same iter count ----
    // Measures the cost of `let t = Instant::now(); ... t.elapsed().as_nanos()`
    // with no work in between, so the analysis can subtract/attribute this
    // floor from small per-gen elapsed values.
    let mut overhead_samples: Vec<u64> = Vec::with_capacity(total);
    for _ in 0..total {
        let t = Instant::now();
        let e = t.elapsed().as_nanos() as u64;
        std::hint::black_box(e);
        overhead_samples.push(t.elapsed().as_nanos() as u64);
    }
    overhead_samples.sort_unstable();
    let ov_mean: f64 = overhead_samples.iter().sum::<u64>() as f64 / overhead_samples.len() as f64;
    let ov_median = overhead_samples[overhead_samples.len() / 2];
    let ov_p90 = overhead_samples[(overhead_samples.len() as f64 * 0.90) as usize];

    // ---- real per-gen timed parse ----
    let mut ctx = parser.init_ctx();
    let mut rows: Vec<(usize, u64, usize, usize)> = Vec::with_capacity(total);
    let t_all = Instant::now();
    for (idx, c) in chars.iter().enumerate() {
        let t0 = Instant::now();
        ctx = parser.parse_step(ctx, *c, idx + 1 == total)?;
        let elapsed_ns = t0.elapsed().as_nanos() as u64;

        let main_shapes = ctx.paths.get(&ctx.main_root).map(|m| m.len()).unwrap_or(0);
        let total_shapes: usize = ctx.paths.values().map(|m| m.len()).sum();

        rows.push((idx + 1, elapsed_ns, main_shapes, total_shapes));
    }
    let wall = t_all.elapsed();

    // ---- emit CSV ----
    let mut csv = String::with_capacity(rows.len() * 24 + 64);
    csv.push_str("gen,elapsed_ns,main_shapes,total_shapes\n");
    for (gen_idx, ns, main_shapes, total_shapes) in &rows {
        csv.push_str(&format!("{},{},{},{}\n", gen_idx, ns, main_shapes, total_shapes));
    }
    match csv_out_path {
        Some(path) => {
            std::fs::write(&path, &csv)?;
            eprintln!("csv written: {} ({} rows)", path, rows.len());
        }
        None => {
            let stdout = std::io::stdout();
            let mut lock = stdout.lock();
            lock.write_all(csv.as_bytes())?;
        }
    }

    // ---- stderr summary + overhead calibration ----
    let sum_step_ns: u64 = rows.iter().map(|r| r.1).sum();
    eprintln!(
        "file={} gens={} wall={:?} sum_step_ns={:.3}ms (n={})",
        std::path::Path::new(&input_path).file_name().unwrap().to_string_lossy(),
        rows.len(),
        wall,
        sum_step_ns as f64 / 1_000_000.0,
        parser.interior_group_max_depth(),
    );
    eprintln!(
        "instant-pair overhead calibration (n={} empty samples): mean={:.1}ns median={}ns p90={}ns",
        overhead_samples.len(),
        ov_mean,
        ov_median,
        ov_p90,
    );

    Ok(())
}
