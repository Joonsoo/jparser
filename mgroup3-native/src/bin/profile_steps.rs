//! Per-step parse profiler: drive `parse_step` one char at a time and report
//! where the wall time concentrates, together with parser-state size metrics
//! (live cond roots / path shapes / condition tree sizes) so pathological
//! inputs can be attributed to a mechanism, not just a region.
//!
//! Usage: profile_steps <parserdata.pb> <input.txt> [topN]

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

struct Row {
    gen_idx: usize,
    ch: char,
    micros: u128,
    roots: usize,
    shapes: usize,
    cond_nodes: usize,
    max_cond: usize,
    apps: usize,
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut args = std::env::args().skip(1);
    let data_path = args
        .next()
        .expect("usage: profile_steps <parserdata.pb> <input.txt> [topN]");
    let input_path = args
        .next()
        .expect("usage: profile_steps <parserdata.pb> <input.txt> [topN]");
    let top_n: usize = args.next().map(|s| s.parse().unwrap()).unwrap_or(25);

    let data_bytes = std::fs::read(&data_path)?;
    let data = Mgroup3ParserData::decode(data_bytes.as_slice())?;
    let parser = Mgroup3Parser::new(data);
    let input = std::fs::read_to_string(&input_path)?;
    let chars: Vec<char> = input.chars().collect();
    let total = chars.len();
    println!("input: {} chars", total);

    let mut ctx = parser.init_ctx();
    let mut rows: Vec<Row> = Vec::with_capacity(total);
    let t_all = Instant::now();
    for (idx, c) in chars.iter().enumerate() {
        let t = Instant::now();
        ctx = match parser.parse_step(ctx, *c, idx + 1 == total) {
            Ok(ctx) => ctx,
            Err(e) => {
                println!("PARSE ERROR at {}: {:?}", idx, e);
                return Ok(());
            }
        };
        let micros = t.elapsed().as_micros();
        let roots = ctx.paths.len();
        let shapes: usize = ctx.paths.values().map(|m| m.len()).sum();
        let mut cond_nodes = 0usize;
        let mut max_cond = 0usize;
        for pm in ctx.paths.values() {
            for cond in pm.values() {
                let s = cond_size(cond);
                cond_nodes += s;
                max_cond = max_cond.max(s);
            }
        }
        let apps = ctx
            .history
            .last()
            .map(|e| e.action_applications.len())
            .unwrap_or(0);
        rows.push(Row {
            gen_idx: idx + 1,
            ch: *c,
            micros,
            roots,
            shapes,
            cond_nodes,
            max_cond,
            apps,
        });
    }
    println!("total parse: {:?}", t_all.elapsed());

    // Deciles of cumulative time over the input.
    let sum: u128 = rows.iter().map(|r| r.micros).sum();
    println!("\ncumulative time by input decile (each = 10% of chars):");
    let mut acc = 0u128;
    let mut decile_start = 0usize;
    for d in 1..=10 {
        let end = total * d / 10;
        let part: u128 = rows[decile_start..end].iter().map(|r| r.micros).sum();
        acc += part;
        println!(
            "  {:3}%..{:3}%: {:8.1}ms  (cum {:5.1}%)",
            (d - 1) * 10,
            d * 10,
            part as f64 / 1000.0,
            acc as f64 * 100.0 / sum as f64
        );
        decile_start = end;
    }

    // Top-N slowest steps with input context.
    let mut sorted: Vec<&Row> = rows.iter().collect();
    sorted.sort_by(|a, b| b.micros.cmp(&a.micros));
    println!("\ntop {} slowest steps:", top_n);
    println!(
        "  {:>6} {:>4} {:>10} {:>6} {:>7} {:>10} {:>8} {:>6}  context",
        "gen", "char", "ms", "roots", "shapes", "condNodes", "maxCond", "apps"
    );
    for r in sorted.iter().take(top_n) {
        let lo = r.gen_idx.saturating_sub(30);
        let hi = (r.gen_idx + 10).min(total);
        let context: String = chars[lo..hi]
            .iter()
            .map(|c| if *c == '\n' { '\u{23CE}' } else { *c })
            .collect();
        println!(
            "  {:>6} {:>4} {:>10.2} {:>6} {:>7} {:>10} {:>8} {:>6}  |{}|",
            r.gen_idx,
            format!("{:?}", r.ch).trim_matches('\''),
            r.micros as f64 / 1000.0,
            r.roots,
            r.shapes,
            r.cond_nodes,
            r.max_cond,
            r.apps,
            context
        );
    }

    // Peak-state dump: at the step with the most shapes, attribute shapes to
    // roots and inspect their structure (milestone-path depth, tip groups).
    // MG3_DUMP_AT=<gen> 으로 peak 대신 특정 gen 을 덤프.
    let dump_at: Option<usize> =
        std::env::var("MG3_DUMP_AT").ok().and_then(|v| v.parse().ok());
    if let Some(peak) = rows
        .iter()
        .filter(|r| dump_at.map_or(true, |g| r.gen_idx == g))
        .max_by_key(|r| r.shapes)
    {
        println!(
            "\nre-running to dump state at peak gen {} ({} shapes)...",
            peak.gen_idx, peak.shapes
        );
        let mut ctx = parser.init_ctx();
        for (idx, c) in chars.iter().enumerate() {
            ctx = parser
                .parse_step(ctx, *c, idx + 1 == total)
                .expect("parse error");
            if idx + 1 == peak.gen_idx {
                break;
            }
        }
        let mut per_root: Vec<(String, usize)> = ctx
            .paths
            .iter()
            .map(|(root, pm)| {
                (
                    format!("sym{}@{}", root.symbol_id, root.start_gen),
                    pm.len(),
                )
            })
            .collect();
        per_root.sort_by(|a, b| b.1.cmp(&a.1));
        println!("shapes per root (top 10):");
        for (root, n) in per_root.iter().take(10) {
            println!("  {:>18}: {}", root, n);
        }
        // For the biggest root: depth histogram + tip group histogram + samples.
        if let Some((root, _)) = ctx.paths.iter().max_by_key(|(_, pm)| pm.len()) {
            let pm = &ctx.paths[root];
            let mut depth_hist: std::collections::BTreeMap<usize, usize> = Default::default();
            let mut tip_hist: std::collections::BTreeMap<i32, usize> = Default::default();
            let mut milestone_hist: std::collections::BTreeMap<(i32, i32), usize> =
                Default::default();
            for shape in pm.keys() {
                let mut depth = 0;
                let mut mp = shape.milestone_path.clone();
                if let Some(m) = &mp {
                    *milestone_hist
                        .entry((m.milestone.symbol_id, m.milestone.pointer))
                        .or_default() += 1;
                }
                while let Some(m) = mp {
                    depth += 1;
                    mp = m.parent.clone();
                }
                *depth_hist.entry(depth).or_default() += 1;
                *tip_hist.entry(shape.tip_group_id).or_default() += 1;
            }
            // 어떤 identity 축이 체인 곱셈 인자인지: 전체 identity vs gen 무시 vs
            // observingCondSymbolIds 무시로 distinct 체인 수 비교.
            let mut full_keys = std::collections::HashSet::new();
            let mut no_gen_keys = std::collections::HashSet::new();
            let mut no_obs_keys = std::collections::HashSet::new();
            let mut no_gen_no_obs_keys = std::collections::HashSet::new();
            for shape in pm.keys() {
                let mut full = vec![shape.tip_group_id.to_string()];
                let mut no_gen = full.clone();
                let mut no_obs = full.clone();
                let mut no_gen_no_obs = full.clone();
                let mut mp = shape.milestone_path.clone();
                while let Some(m) = mp {
                    let k = &m.milestone;
                    full.push(format!(
                        "{}:{}:{}g{}o{:?}",
                        k.symbol_id, k.pointer, k.gen_idx, m.gen_idx, m.observing_cond_symbol_ids
                    ));
                    no_gen.push(format!(
                        "{}:{}o{:?}",
                        k.symbol_id, k.pointer, m.observing_cond_symbol_ids
                    ));
                    no_obs.push(format!(
                        "{}:{}:{}g{}",
                        k.symbol_id, k.pointer, k.gen_idx, m.gen_idx
                    ));
                    no_gen_no_obs.push(format!("{}:{}", k.symbol_id, k.pointer));
                    mp = m.parent.clone();
                }
                full_keys.insert(full.join("/"));
                no_gen_keys.insert(no_gen.join("/"));
                no_obs_keys.insert(no_obs.join("/"));
                no_gen_no_obs_keys.insert(no_gen_no_obs.join("/"));
            }
            println!(
                "  distinct chains: full={} noGen={} noObs={} noGenNoObs={}",
                full_keys.len(),
                no_gen_keys.len(),
                no_obs_keys.len(),
                no_gen_no_obs_keys.len()
            );
            if std::env::var_os("MG3_DUMP_CHAINS").is_some() {
                for k in &full_keys {
                    println!("CHAIN {}", k);
                }
            }
            let mut samples: Vec<&String> = full_keys.iter().collect();
            samples.sort();
            println!("  sample chains (3):");
            for s in samples.iter().take(2) {
                println!("    {}", &s[..s.len().min(4000)]);
            }
            println!("biggest root sym{}@{}:", root.symbol_id, root.start_gen);
            println!("  path depth histogram: {:?}", depth_hist);
            println!("  distinct tip groups: {} (top: {:?})", tip_hist.len(), {
                let mut v: Vec<_> = tip_hist.iter().collect();
                v.sort_by(|a, b| b.1.cmp(a.1));
                v.into_iter().take(8).collect::<Vec<_>>()
            });
            println!(
                "  distinct tip milestones (sym,ptr): {} (top: {:?})",
                milestone_hist.len(),
                {
                    let mut v: Vec<_> = milestone_hist.iter().collect();
                    v.sort_by(|a, b| b.1.cmp(a.1));
                    v.into_iter().take(10).collect::<Vec<_>>()
                }
            );
        }
    }

    // Aggregate state-size correlation: bucket steps by shapes count.
    println!("\ntime vs live shapes (bucketed):");
    let buckets = [0usize, 10, 20, 50, 100, 200, 500, 1000, usize::MAX];
    for w in buckets.windows(2) {
        let (lo, hi) = (w[0], w[1]);
        let in_bucket: Vec<&Row> = rows
            .iter()
            .filter(|r| r.shapes >= lo && r.shapes < hi)
            .collect();
        if in_bucket.is_empty() {
            continue;
        }
        let t: u128 = in_bucket.iter().map(|r| r.micros).sum();
        println!(
            "  shapes {:>5}..{:<7} steps={:>6} total={:>9.1}ms avg={:>8.2}ms",
            lo,
            if hi == usize::MAX {
                "inf".to_string()
            } else {
                hi.to_string()
            },
            in_bucket.len(),
            t as f64 / 1000.0,
            t as f64 / 1000.0 / in_bucket.len() as f64
        );
    }
    Ok(())
}
