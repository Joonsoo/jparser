//! [INSTRUMENTATION — do not land] Sharing-ceiling profiler for the
//! "watcher-main simulation sharing" design study.
//!
//! Hypothesis: a watcher (cond-path) chain, below its cond root, evolves
//! structurally identically to the corresponding suffix of some main-path
//! chain (and to other watchers of the same cond symbol at nearby anchors).
//! If so, the per-step cost (∝ live shapes) can be collapsed onto a shared
//! simulation table keyed by the *structural suffix*.
//!
//! This binary quantifies the ceiling: at a chosen gen (peak by default, or
//! MG3_DUMP_AT), it normalizes every live shape to a gen-relative structural
//! key and reports:
//!   - total shapes
//!   - distinct structural suffixes (= min shapes a shared table would hold)
//!   - the implied dedup factor and the step-cost improvement ceiling.
//!
//! Two normalizations:
//!   A. GEN-RELATIVE: milestone chain of (symbol:pointer) with each node's gen
//!      rebased to (gen - root.start_gen), plus tip group. Two shapes with the
//!      same key are byte-identical simulations up to the root's start offset —
//!      the strongest, safest sharing key.
//!   B. GEN-FREE: (symbol:pointer) chain + tip group, gens dropped entirely.
//!      Upper bound on sharing (ignores that different anchors may have
//!      genuinely different downstream futures); reported for contrast.
//!
//! Also reports the split by "flavor": main root vs bounded watchers vs
//! lookahead watchers (via the parser's lookahead_cond_symbols set), and the
//! cross-owner match rate (does a watcher suffix coincide with a main suffix?).
//!
//! Usage: sharing_ceiling <parserdata.pb> <input.txt>
//!   MG3_DUMP_AT=<gen>  dump at this gen instead of the shapes-peak gen.
//!   MG3_CEIL_ALLGENS=1 print a per-gen ceiling table over the whole parse
//!                      (cost-model integral), not just the peak.

use std::collections::{HashMap, HashSet};

use mgroup4_native::parser::Mgroup4Parser;
use mgroup4_native::parsing_ctx::{MilestonePath, ParsingCtx};
use mgroup4_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

/// Walk a milestone chain tip->root, emit (symbol_id, pointer, gen_idx) list
/// ordered root..tip. gen_idx is the *runtime* node gen (m.gen_idx of the node,
/// i.e. where the tip group above it attached).
fn chain_nodes(mp: &Option<std::rc::Rc<MilestonePath>>) -> Vec<(i32, i32, i32)> {
    let mut out = Vec::new();
    let mut cur = mp.clone();
    while let Some(m) = cur {
        out.push((m.milestone.symbol_id, m.milestone.pointer, m.gen_idx));
        cur = m.parent.clone();
    }
    out.reverse();
    out
}

/// Gen-relative structural key of a shape, rebased to `base` (root.start_gen).
fn key_gen_relative(mp: &Option<std::rc::Rc<MilestonePath>>, tip: i32, base: i32) -> String {
    let mut parts = Vec::new();
    for (s, p, g) in chain_nodes(mp) {
        parts.push(format!("{}:{}@{}", s, p, g - base));
    }
    parts.push(format!("^{}", tip));
    parts.join("/")
}

/// Gen-free structural key of a shape (upper bound on sharing).
fn key_gen_free(mp: &Option<std::rc::Rc<MilestonePath>>, tip: i32) -> String {
    let mut parts = Vec::new();
    for (s, p, _) in chain_nodes(mp) {
        parts.push(format!("{}:{}", s, p));
    }
    parts.push(format!("^{}", tip));
    parts.join("/")
}

struct GenCeiling {
    gen_idx: usize,
    total: usize,
    distinct_rel: usize,
    distinct_free: usize,
}

fn ceiling_at(ctx: &ParsingCtx) -> GenCeiling {
    let mut total = 0usize;
    let mut rel: HashSet<String> = HashSet::new();
    let mut free: HashSet<String> = HashSet::new();
    for (root, pm) in ctx.paths.iter() {
        let base = root.start_gen;
        for shape in pm.keys() {
            total += 1;
            rel.insert(key_gen_relative(&shape.milestone_path, shape.tip_group_id, base));
            free.insert(key_gen_free(&shape.milestone_path, shape.tip_group_id));
        }
    }
    GenCeiling {
        gen_idx: ctx.gen_idx as usize,
        total,
        distinct_rel: rel.len(),
        distinct_free: free.len(),
    }
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut args = std::env::args().skip(1);
    let data_path = args.next().expect("usage: sharing_ceiling <parserdata.pb> <input.txt>");
    let input_path = args.next().expect("usage: sharing_ceiling <parserdata.pb> <input.txt>");

    let data = Mgroup3ParserData::decode(std::fs::read(&data_path)?.as_slice())?;
    let lookahead: HashSet<i32> = data.lookahead_cond_symbol_ids.iter().copied().collect();
    let parser = Mgroup4Parser::new(data);
    let input = std::fs::read_to_string(&input_path)?;
    let chars: Vec<char> = input.chars().collect();
    let total_chars = chars.len();

    // ---- Whole-parse cost-model integral ----
    let all_gens = std::env::var_os("MG3_CEIL_ALLGENS").is_some();
    {
        let mut ctx = parser.init_ctx();
        let mut sum_total = 0usize;
        let mut sum_rel = 0usize;
        let mut sum_free = 0usize;
        let mut peak = GenCeiling { gen_idx: 0, total: 0, distinct_rel: 0, distinct_free: 0 };
        let mut rows: Vec<GenCeiling> = Vec::new();
        for (idx, c) in chars.iter().enumerate() {
            ctx = match parser.parse_step(ctx, *c, idx + 1 == total_chars) {
                Ok(ctx) => ctx,
                Err(e) => {
                    println!("PARSE ERROR at {}: {:?}", idx, e);
                    return Ok(());
                }
            };
            let g = ceiling_at(&ctx);
            sum_total += g.total;
            sum_rel += g.distinct_rel;
            sum_free += g.distinct_free;
            if g.total > peak.total {
                peak = GenCeiling { gen_idx: g.gen_idx, total: g.total, distinct_rel: g.distinct_rel, distinct_free: g.distinct_free };
            }
            if all_gens { rows.push(g); }
        }
        println!("=== whole-parse cost-model integral ({} chars) ===", total_chars);
        println!("  Σ shapes             = {}", sum_total);
        println!("  Σ distinct (rel)     = {}  -> ceiling {:.3}x  (retain {:.1}%)",
            sum_rel, sum_total as f64 / sum_rel.max(1) as f64, 100.0 * sum_rel as f64 / sum_total.max(1) as f64);
        println!("  Σ distinct (genfree) = {}  -> ceiling {:.3}x  (retain {:.1}%)",
            sum_free, sum_total as f64 / sum_free.max(1) as f64, 100.0 * sum_free as f64 / sum_total.max(1) as f64);
        println!("  peak gen {}: total={} distinctRel={} ({:.3}x) distinctFree={} ({:.3}x)",
            peak.gen_idx, peak.total, peak.distinct_rel,
            peak.total as f64 / peak.distinct_rel.max(1) as f64,
            peak.distinct_free, peak.total as f64 / peak.distinct_free.max(1) as f64);
        if all_gens {
            println!("\n  per-gen (gens with >100 shapes):");
            for r in rows.iter().filter(|r| r.total > 100) {
                println!("    gen {:>5}: total={:>5} rel={:>5} ({:.2}x) free={:>5} ({:.2}x)",
                    r.gen_idx, r.total, r.distinct_rel, r.total as f64 / r.distinct_rel.max(1) as f64,
                    r.distinct_free, r.total as f64 / r.distinct_free.max(1) as f64);
            }
        }
    }

    // ---- Detailed dump at the chosen gen ----
    let dump_at: Option<usize> = std::env::var("MG3_DUMP_AT").ok().and_then(|v| v.parse().ok());
    let target_gen = {
        let mut ctx = parser.init_ctx();
        let mut best = (0usize, 0usize);
        for (idx, c) in chars.iter().enumerate() {
            ctx = parser.parse_step(ctx, *c, idx + 1 == total_chars).expect("parse");
            let shapes: usize = ctx.paths.values().map(|m| m.len()).sum();
            if let Some(g) = dump_at {
                if idx + 1 == g { best = (g, shapes); break; }
            } else if shapes > best.1 {
                best = (idx + 1, shapes);
            }
        }
        best.0
    };

    let mut ctx = parser.init_ctx();
    for (idx, c) in chars.iter().enumerate() {
        ctx = parser.parse_step(ctx, *c, idx + 1 == total_chars).expect("parse");
        if idx + 1 == target_gen { break; }
    }

    println!("\n=== detailed dump at gen {} ===", target_gen);
    let main_root = ctx.main_root;

    let mut total = 0usize;
    let mut main_shapes = 0usize;
    let mut bounded_shapes = 0usize;
    let mut lookahead_shapes = 0usize;
    let mut rel_count: HashMap<String, usize> = HashMap::new();
    let mut main_rel: HashSet<String> = HashSet::new();
    let mut watcher_rel: HashSet<String> = HashSet::new();
    let mut per_symbol_anchor_sets: HashMap<i32, Vec<(i32, HashSet<String>)>> = HashMap::new();

    for (root, pm) in ctx.paths.iter() {
        let base = root.start_gen;
        let mainp = root.symbol_id == main_root.symbol_id && root.start_gen == main_root.start_gen;
        let looka = lookahead.contains(&root.symbol_id);
        let mut this_set: HashSet<String> = HashSet::new();
        for shape in pm.keys() {
            total += 1;
            let k = key_gen_relative(&shape.milestone_path, shape.tip_group_id, base);
            *rel_count.entry(k.clone()).or_default() += 1;
            if mainp {
                main_shapes += 1;
                main_rel.insert(k.clone());
            } else {
                watcher_rel.insert(k.clone());
                this_set.insert(k.clone());
                if looka { lookahead_shapes += 1; } else { bounded_shapes += 1; }
            }
        }
        if !mainp {
            per_symbol_anchor_sets.entry(root.symbol_id).or_default().push((base, this_set));
        }
    }

    let distinct_rel = rel_count.len();
    println!("  total shapes          = {}", total);
    println!("    main owner          = {}", main_shapes);
    println!("    bounded watchers    = {}", bounded_shapes);
    println!("    lookahead watchers  = {}", lookahead_shapes);
    println!("  distinct rel suffixes = {}  -> ceiling {:.3}x (retain {:.1}%)",
        distinct_rel, total as f64 / distinct_rel.max(1) as f64,
        100.0 * distinct_rel as f64 / total.max(1) as f64);

    let watcher_matching_main = watcher_rel.iter().filter(|k| main_rel.contains(*k)).count();
    println!("  distinct watcher suffixes = {}", watcher_rel.len());
    println!("    of which also a MAIN suffix = {} ({:.1}%)",
        watcher_matching_main,
        100.0 * watcher_matching_main as f64 / watcher_rel.len().max(1) as f64);

    // rebuild per-symbol anchor sets using GEN-FREE keys (the true structural test:
    // do two anchors of the same cond symbol simulate the same (symbol:pointer)-chain
    // shapes, regardless of where they started?)
    let mut per_symbol_free: HashMap<i32, Vec<(i32, HashSet<String>)>> = HashMap::new();
    for (root, pm) in ctx.paths.iter() {
        if root.symbol_id == main_root.symbol_id && root.start_gen == main_root.start_gen { continue; }
        let mut s: HashSet<String> = HashSet::new();
        for shape in pm.keys() { s.insert(key_gen_free(&shape.milestone_path, shape.tip_group_id)); }
        per_symbol_free.entry(root.symbol_id).or_default().push((root.start_gen, s));
    }
    println!("\n  adjacent-anchor identity (cond symbols with >1 live anchor):");
    println!("    (relEq = gen-rebased suffix sets equal; freeEq = gen-FREE structural sets equal; freeJaccard = overlap)");
    let mut syms: Vec<_> = per_symbol_anchor_sets.iter().filter(|(_, v)| v.len() > 1).collect();
    syms.sort_by_key(|(s, _)| **s);
    for (sym, anchors) in syms {
        let first = &anchors[0].1;
        let all_equal = anchors.iter().all(|(_, s)| s == first);
        let gens: Vec<i32> = anchors.iter().map(|(g, _)| *g).collect();
        // gen-free comparison
        let free = &per_symbol_free[sym];
        let free_first = &free[0].1;
        let free_equal = free.iter().all(|(_, s)| s == free_first);
        // jaccard of union vs max, gen-free
        let mut uni: HashSet<&String> = HashSet::new();
        let mut max_size = 0;
        for (_, s) in free { for k in s { uni.insert(k); } max_size = max_size.max(s.len()); }
        let jacc = if uni.is_empty() { 0.0 } else { max_size as f64 / uni.len() as f64 };
        println!("    sym{} anchors {:?}: {} anchors, shapes-each≈{}, relEq={} freeEq={} freeMax/Union={:.2}",
            sym, gens, anchors.len(), first.len(), all_equal, free_equal, jacc);
    }

    let mut dup: Vec<(&String, &usize)> = rel_count.iter().filter(|(_, c)| **c > 1).collect();
    dup.sort_by(|a, b| b.1.cmp(a.1));
    println!("\n  top-10 most-shared suffixes (count = live shapes collapsing to 1):");
    for (k, c) in dup.iter().take(10) {
        let show = if k.len() > 90 { format!("{}…", &k[..90]) } else { (*k).clone() };
        println!("    x{:<4} {}", c, show);
    }

    // ---- work-level dedup: the step cost is one apply_term_action per live shape.
    // If two shapes (from any owners) have the SAME PathShape (milestone_path +
    // tip_group), the term-action application is byte-identical work (input is the
    // same). Count distinct PathShape (structural identity as the runtime uses it)
    // across all owners — this is the max collapse a "shared shape table" could get,
    // and it is the honest measure of duplicate WORK (not just suffix similarity). ----
    {
        let mut shape_keys: HashSet<u64> = HashSet::new();
        let mut n = 0usize;
        // also: same tip_group_id across owners? (term lookup is by tip only)
        let mut tip_keys: HashSet<i32> = HashSet::new();
        for (_root, pm) in ctx.paths.iter() {
            for shape in pm.keys() {
                n += 1;
                // hash the runtime PathShape identity (what the PathMap keys on)
                use std::hash::{Hash, Hasher};
                let mut h = std::collections::hash_map::DefaultHasher::new();
                shape.hash(&mut h);
                shape_keys.insert(h.finish());
                tip_keys.insert(shape.tip_group_id);
            }
        }
        println!("\n  work-level dedup (runtime PathShape identity across ALL owners):");
        println!("    total shapes = {}, distinct PathShape = {} ({:.3}x), distinct tipGroup = {}",
            n, shape_keys.len(), n as f64 / shape_keys.len().max(1) as f64, tip_keys.len());
    }

    // ---- structural inspection: sample full chains of a watcher root and main ----
    // MG3_SHOW_ROOT=<symId> dumps 3 gen-free chains for that cond root, and 3 for
    // main whose chain CONTAINS symId (the descent point), to eyeball whether a
    // watcher chain is a suffix of a main chain.
    if let Ok(want) = std::env::var("MG3_SHOW_ROOT") {
        let want: i32 = want.parse().unwrap();
        println!("\n  === chains for cond sym{} (gen-free, root..tip) ===", want);
        for (root, pm) in ctx.paths.iter() {
            if root.symbol_id == want {
                let mut ks: Vec<String> = pm.keys()
                    .map(|s| key_gen_free(&s.milestone_path, s.tip_group_id)).collect();
                ks.sort(); ks.dedup();
                println!("  root@{}: {} distinct gen-free chains", root.start_gen, ks.len());
                for k in ks.iter().take(3) { println!("    W {}", k); }
            }
        }
        println!("  === main chains that contain a sym{} node (descent points) ===", want);
        let mp_pm = &ctx.paths[&main_root];
        let mut shown = 0;
        for shape in mp_pm.keys() {
            let nodes = chain_nodes(&shape.milestone_path);
            if nodes.iter().any(|(s, _, _)| *s == want) {
                let k = key_gen_free(&shape.milestone_path, shape.tip_group_id);
                println!("    M {}", k);
                shown += 1;
                if shown >= 3 { break; }
            }
        }
        if shown == 0 { println!("    (no main chain contains a sym{} node)", want); }
    }

    Ok(())
}
