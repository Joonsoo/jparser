//! Corpus A/B harness for kernels_history: reports per-file median hist time and
//! a deterministic content hash of the full output, so the SAME binary run
//! against a pre-change and a post-change build lets us byte-compare output and
//! read the speedup. (There is a single `kernels_history` — the P1 dedup is
//! integrated in-place — so cross-build comparison is the A/B, not two fns.)
//!
//! Usage: hist_ab <parserdata.pb> <input1.bbx> [input2.bbx ...] [--iters N]

use std::time::Instant;

use mgroup4_native::parser::Mgroup4Parser;
use mgroup4_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn median(mut v: Vec<f64>) -> f64 {
    v.sort_by(|a, b| a.partial_cmp(b).unwrap());
    v[v.len() / 2]
}

// FNV-1a 64 over the deterministically-sorted output — a stable fingerprint of
// the exact kernel set per gen. Two runs match iff the output is byte-identical.
fn fingerprint<S: std::hash::BuildHasher>(
    hist: &[std::collections::HashSet<mgroup4_native::parsing_ctx::KtlibKernel, S>],
) -> u64 {
    let mut h: u64 = 0xcbf29ce484222325;
    let mix = |x: i32, h: &mut u64| {
        for b in x.to_le_bytes() {
            *h ^= b as u64;
            *h = h.wrapping_mul(0x100000001b3);
        }
    };
    for ks in hist {
        let mut v: Vec<_> = ks.iter().copied().collect();
        v.sort();
        mix(v.len() as i32, &mut h);
        for k in v {
            mix(k.symbol_id, &mut h);
            mix(k.pointer, &mut h);
            mix(k.begin_gen, &mut h);
            mix(k.end_gen, &mut h);
        }
        mix(-1, &mut h); // gen separator
    }
    h
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut args: Vec<String> = std::env::args().skip(1).collect();
    let mut iters = 7usize;
    if let Some(pos) = args.iter().position(|a| a == "--iters") {
        iters = args[pos + 1].parse().unwrap();
        args.drain(pos..pos + 2);
    }
    let data_path = args.remove(0);

    let data = Mgroup3ParserData::decode(std::fs::read(&data_path)?.as_slice())?;
    let parser = Mgroup4Parser::new(data);

    let mut combined: u64 = 0xcbf29ce484222325;
    for input_path in &args {
        let input = std::fs::read_to_string(input_path)?;
        let ctx = parser.parse(&input).map_err(|e| format!("{:?}", e))?;
        // warm
        let hist = parser.kernels_history(&ctx);
        let fp = fingerprint(&hist);
        let total_kernels: usize = hist.iter().map(|ks| ks.len()).sum();
        let gens = hist.len();
        drop(hist);

        let mut times = Vec::new();
        for _ in 0..iters {
            let t = Instant::now();
            let h = parser.kernels_history(&ctx);
            let d = t.elapsed().as_secs_f64() * 1000.0;
            std::hint::black_box(h.len());
            times.push(d);
        }
        let name = std::path::Path::new(input_path)
            .file_name()
            .unwrap()
            .to_string_lossy();
        println!(
            "{:<14} gens={:<5} kernels={:<8} hist_median={:>8.2}ms  fp={:016x}",
            name, gens, total_kernels, median(times), fp
        );
        for b in fp.to_le_bytes() {
            combined ^= b as u64;
            combined = combined.wrapping_mul(0x100000001b3);
        }
    }
    println!("COMBINED_FP={:016x}", combined);
    Ok(())
}
