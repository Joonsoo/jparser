//! MEASUREMENT — the residual O(n) per-edit terms of the Stage-1 reuse boundary.
//!
//! Stage 1 lets a consumer skip re-walking/-encoding the reusable prefix+suffix,
//! but computing the boundary (`EditReuse`) still needs, per edit:
//!   (1) the active-interval maps of both parses (`active_intervals`, O(n)),
//!   (2) building a `KernelsQuery` (one evaluator, O(n) index),
//!   (3) `query.at(gen)` for the dirty window (per-gen, ~O(1) amortized).
//! This bin times each on a synthetic mulang document across n ∈ {1k,4k,16k,64k}
//! so we can judge how much O(n) remains — i.e. whether Stage 1.5 (incremental
//! interval / evaluator maintenance) is warranted. Full `kernels_history` is
//! timed alongside as the "old world" baseline (what every edit re-does today).
//!
//! Usage: reuse_measure <parserdata.pb|.pb.gz> [--sizes 1,4,16,64] [--reps 5]
//! (`--sizes` in units of ~1000 chars; parserdata resolved from the arg, else
//! from the newest ~/.cache/mulang-native/*/mulang-mg3-parserdata.pb.gz.)

use std::path::PathBuf;
use std::sync::Arc;
use std::time::{Duration, Instant};

use mgroup3_native::parser::record_cond::{active_intervals, dangerous_roots};
use mgroup3_native::parser::Mgroup3Parser;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

// -- loading ----------------------------------------------------------------
fn load_parser(path: &str) -> Result<Mgroup3Parser, Box<dyn std::error::Error>> {
    let raw = std::fs::read(path)?;
    let bytes = if path.ends_with(".gz") {
        use flate2::read::GzDecoder;
        use std::io::Read;
        let mut buf = Vec::new();
        GzDecoder::new(raw.as_slice()).read_to_end(&mut buf)?;
        buf
    } else {
        raw
    };
    let data = Mgroup3ParserData::decode(bytes.as_slice())?;
    Ok(Mgroup3Parser::new(data))
}

fn resolve_default_parserdata() -> Option<PathBuf> {
    let home = std::env::var("HOME").ok()?;
    let root = PathBuf::from(home).join(".cache/mulang-native");
    let mut best: Option<(std::time::SystemTime, PathBuf)> = None;
    for entry in std::fs::read_dir(&root).ok()? {
        let Ok(entry) = entry else { continue };
        let cand = entry.path().join("mulang-mg3-parserdata.pb.gz");
        if let Ok(md) = std::fs::metadata(&cand) {
            let mtime = md.modified().unwrap_or(std::time::UNIX_EPOCH);
            if best.as_ref().map(|(t, _)| mtime > *t).unwrap_or(true) {
                best = Some((mtime, cand));
            }
        }
    }
    best.map(|(_, p)| p)
}

// -- synthetic doc (mirrors quadratic_probe::make_doc) ----------------------
fn block(i: usize) -> String {
    format!("// note {i} ....\nclass C{i} {{\n  fieldA{i}: string\n  fieldB{i}: bool\n}}\n\n")
}
fn make_doc(target_chars: usize) -> String {
    let mut s = "namespace probe.gen\n\n".to_string();
    let mut i = 0usize;
    while s.chars().count() < target_chars {
        s.push_str(&block(i));
        i += 1;
    }
    s
}

// -- timing -----------------------------------------------------------------
fn median(mut v: Vec<Duration>) -> Duration {
    v.sort_unstable();
    v[v.len() / 2]
}
fn ms(d: Duration) -> f64 {
    d.as_secs_f64() * 1000.0
}
fn us(d: Duration) -> f64 {
    d.as_secs_f64() * 1e6
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args: Vec<String> = std::env::args().skip(1).collect();
    let mut sizes: Vec<usize> = vec![1, 4, 16, 64];
    let mut reps = 5usize;
    let mut data_path: Option<String> = None;
    let mut it = args.iter().peekable();
    while let Some(a) = it.next() {
        match a.as_str() {
            "--sizes" => {
                if let Some(v) = it.next() {
                    sizes = v.split(',').filter_map(|s| s.trim().parse().ok()).collect();
                }
            }
            "--reps" => {
                if let Some(v) = it.next() {
                    reps = v.parse().unwrap_or(5);
                }
            }
            s if !s.starts_with("--") && data_path.is_none() => data_path = Some(s.to_string()),
            _ => {}
        }
    }
    let data_path = data_path
        .or_else(|| resolve_default_parserdata().map(|p| p.to_string_lossy().into_owned()))
        .expect("no parserdata: pass a path or populate ~/.cache/mulang-native/");

    eprintln!("[load] {data_path}");
    let parser = Arc::new(load_parser(&data_path)?);

    println!("=== STAGE-1 REUSE-BOUNDARY COST vs n (synthetic mulang doc) ===");
    println!("  reps per cell = {reps} (median). All on a freshly-parsed ctx.");
    println!(
        "{:>8} {:>8} | {:>12} {:>14} | {:>12} {:>14} | {:>12} | {:>12} {:>14} | {:>14}",
        "n", "gens", "intervals_ms", "intervals_ns/gen", "query_new_ms", "query_ns/gen",
        "at_gen_us", "graph+clo_ms", "graph+clo_ns/gn", "full_hist_ms"
    );

    struct Row {
        n: usize,
        intervals_ms: f64,
        query_ms: f64,
        at_gen_us: f64,
        graph_ms: f64,
        full_hist_ms: f64,
    }
    let mut rows: Vec<Row> = Vec::new();

    for &kilo in &sizes {
        let text = make_doc(kilo * 1000);
        let n = text.chars().count();
        let ctx = match parser.parse(&text) {
            Ok(c) => c,
            Err(e) => {
                println!("n={n}: PARSE ERROR {e:?}");
                continue;
            }
        };
        let gens = ctx.history.len();

        // (1) active_intervals over the full history.
        let intervals = {
            let mut ts = Vec::new();
            for _ in 0..reps {
                let t = Instant::now();
                let iv = active_intervals(&ctx.history);
                ts.push(t.elapsed());
                std::hint::black_box(iv);
            }
            median(ts)
        };

        // (2) KernelsQuery construction (evaluator + end_late).
        let query_build = {
            let mut ts = Vec::new();
            for _ in 0..reps {
                let t = Instant::now();
                let q = parser.kernels_query(&ctx);
                ts.push(t.elapsed());
                std::hint::black_box(q.num_gens());
            }
            median(ts)
        };

        // (3) query.at(single gen) — spread across the document, median per call.
        let at_gen = {
            let q = parser.kernels_query(&ctx);
            let probes: Vec<usize> =
                (0..16).map(|i| (gens.saturating_sub(1)) * i / 15).collect();
            let mut ts = Vec::new();
            for _ in 0..reps {
                for &g in &probes {
                    let t = Instant::now();
                    let ks = q.at(g);
                    ts.push(t.elapsed());
                    std::hint::black_box(ks.len());
                }
            }
            median(ts)
        };

        // (4) reference-graph construction + transitive dangerous closure (the
        // Stage-1.1 addition). Timed on precomputed intervals + end_late (which
        // the boundary already builds for the query), at a representative resume
        // gen — the same call `edit_reuse` makes, once per parse.
        let graph_closure = {
            let intervals_map = active_intervals(&ctx.history);
            let end_late = parser.end_of_input_late_fins(&ctx);
            let resume = (gens / 2) as i32;
            let mut ts = Vec::new();
            for _ in 0..reps {
                let t = Instant::now();
                let d = dangerous_roots(&ctx.history, &end_late, &intervals_map, resume);
                ts.push(t.elapsed());
                std::hint::black_box(d.len());
            }
            median(ts)
        };

        // Baseline: full kernels_history (the O(n) work every edit re-does today).
        let full_hist = {
            let mut ts = Vec::new();
            for _ in 0..reps {
                let t = Instant::now();
                let h = parser.kernels_history(&ctx);
                ts.push(t.elapsed());
                std::hint::black_box(h.len());
            }
            median(ts)
        };

        println!(
            "{:>8} {:>8} | {:>12.3} {:>14.1} | {:>12.3} {:>14.1} | {:>12.3} | {:>12.3} {:>14.1} | {:>14.3}",
            n,
            gens,
            ms(intervals),
            intervals.as_secs_f64() * 1e9 / gens as f64,
            ms(query_build),
            query_build.as_secs_f64() * 1e9 / gens as f64,
            us(at_gen),
            ms(graph_closure),
            graph_closure.as_secs_f64() * 1e9 / gens as f64,
            ms(full_hist),
        );
        rows.push(Row {
            n,
            intervals_ms: ms(intervals),
            query_ms: ms(query_build),
            at_gen_us: us(at_gen),
            graph_ms: ms(graph_closure),
            full_hist_ms: ms(full_hist),
        });
    }

    println!();
    println!("=== SCALING (ratio vs smallest n) — should be ~linear in n for (1)/(2), flat for (3) ===");
    if let Some(base) = rows.first() {
        println!(
            "{:>8} {:>8} | {:>10} {:>10} {:>10} {:>10} {:>12}",
            "n", "n/n0", "interval x", "query x", "at_gen x", "graph x", "full_hist x"
        );
        for r in &rows {
            println!(
                "{:>8} {:>8.2} | {:>10.2} {:>10.2} {:>10.2} {:>10.2} {:>12.2}",
                r.n,
                r.n as f64 / base.n as f64,
                r.intervals_ms / base.intervals_ms.max(1e-9),
                r.query_ms / base.query_ms.max(1e-9),
                r.at_gen_us / base.at_gen_us.max(1e-9),
                r.graph_ms / base.graph_ms.max(1e-9),
                r.full_hist_ms / base.full_hist_ms.max(1e-9),
            );
        }
    }
    println!();
    println!("  (1)+(2)+(4) are the residual O(n) the boundary pays per edit — dirty_lo needs");
    println!("  BOTH parses' intervals + graph closure, so double (1) and (4). Compare to");
    println!("  full_hist (today's per-edit O(n)). (3) is what a delta consumer pays per dirty");
    println!("  gen. graph+closure ((4), the Stage-1.1 addition) should stay same-order as (1);");
    println!("  if (1)+(2)+(4) dominate the budget, Stage 1.5 (incremental maintenance) is");
    println!("  warranted; if they are a small fraction of full_hist, Stage 1 already removes");
    println!("  the bulk of the per-edit boundary cost.");
    Ok(())
}
