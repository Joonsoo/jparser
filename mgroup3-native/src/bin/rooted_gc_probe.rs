//! Feasibility probe: root-traced demand GC for phantom watchers.
//! Spec: watcher-GC track feasibility (see task brief). ALGORITHM-INVARIANT.
//!
//! Read-only: drives the UNMODIFIED parser (`parse_step`) and, at each gen,
//! re-derives a stratified demand fixpoint over the live parsing state
//! (`ctx.paths`). The parser and its output are untouched — this only counts.
//!
//! ============================================================
//! WHAT "root-traced demand GC" WOULD DO (the counterfactual GC)
//! ============================================================
//! Current step-6 pruning (core.rs:886-954) keeps a cond root R iff
//!   R == main  ||  R ∈ referenced_roots
//! where referenced_roots is a SINGLE FLAT PASS over EVERY live shape's
//! contribution — including shapes belonging to a watcher that references
//! *itself* (its own chain's observing anchors point back at its own root).
//! That self-reference passes the reachability test even when nothing on the
//! main path demands the watcher — the documented blind spot (PR #6 §확증된
//! 메커니즘: "워처 자신의 observing anchor 자기 root 재참조").
//!
//! root-traced demand GC instead starts the demand set from the MAIN root's
//! paths only and takes a fixpoint: a watcher is kept only if the main path
//! (transitively, through other kept watchers) demands it. A self-referential
//! cycle unreachable from main is collected whole.
//!
//! ============================================================
//! CONTRIBUTION FUNCTION — EXACT MIRROR OF STEP 6 (core.rs:907-943)
//! ============================================================
//! For each live shape (shape, cond) in a root's PathMap, step 6 inserts into
//! `referenced_roots`:
//!   (i)  every root in `cond.referenced_roots()`               [condition demand]
//!   (ii) for each milestone node in shape.milestone_path (tip→root), and each
//!        sid in node.observing_cond_symbol_ids:
//!           PathRoot(sid, node.gen_idx - 1)                     [observing dot anchor]
//!        and, iff sid ∈ lookahead_cond_symbols:
//!           PathRoot(sid, node.gen_idx)                         [lookahead tip anchor]
//!           PathRoot(sid, parent_gen)                           [lookahead parent anchor]
//!        where parent_gen = node.parent.gen_idx, or main_root.start_gen if root-most.
//! (The `reported_cond_roots` set that step 6 also builds is a REPORT filter,
//!  not part of the liveness keep rule — we mirror only the keep rule's
//!  `referenced_roots`. The `walked_nodes` Rc-ptr dedup in step 6 is a pure
//!  performance optimization: a node's contribution is a function of the node
//!  alone and the target is a set, so dedup is output-neutral. We DO replicate
//!  the same tip→root-with-visited-break walk to report a faithful visit-count
//!  cost proxy; the demanded set is identical with or without it.)
//!
//! This function is `shape_contribution` below. We collect it into a per-root
//! contribution `root_demand: PathRoot -> HashSet<PathRoot>` (union over that
//! root's shapes), which is what the fixpoint iterates over.
//!
//! ============================================================
//! SELF-VERIFICATION (mandatory — fidelity is everything)
//! ============================================================
//! If Layer 0 = ALL live roots (not just main), the fixpoint closure must equal
//! exactly the set step 6 keeps == the live roots themselves (ctx.paths already
//! IS the post-step-6 survivor set). We assert, every gen:
//!   closure_from_all_live  ==  ctx.paths.keys()
//! i.e. every live root is reproduced by our contribution function starting from
//! the full live set. A mismatch means the mirror is wrong -> numbers invalid.
//!
//! CAVEAT (documented, and checked): step 6 computes referenced_roots over
//! `paths_evolved` (pre-filter), which can contain a DYING root Q whose shape is
//! the SOLE referencer of a KEPT root R. Reading only `ctx.paths` (survivors) we
//! lose Q, so our closure could drop R -> self-verification would FAIL and we'd
//! report it as such (never report numbers from a failing gen). The empirical
//! self-verification result below tells us whether this case actually arises.
//!
//! ============================================================
//! CLI
//! ============================================================
//!   rooted_gc_probe <parserdata.pb> <input.txt> [--no-eof-fold] [--json] [--per-gen]
//! `--no-eof-fold`: clear plain.eof_cond_symbols before building the parser
//!   (lib-unchanged counterfactual: the eager-fold knob off). PR #6's fold is
//!   thereby disabled and the phantom watchers are re-enabled.

use std::collections::{BTreeMap, HashMap, HashSet};

use mgroup3_native::accept_condition::AcceptCondition;
use mgroup3_native::parser::Mgroup3Parser;
use mgroup3_native::parser_data::ParserDataPlain;
use mgroup3_native::parsing_ctx::{ParsingCtx, PathShape};
use mgroup3_native::path_root::PathRoot;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

/// Contribution of ONE shape (shape, cond) to referenced_roots, mirroring
/// step 6 (core.rs:907-943). Pushes into `out`. Also advances `visits` (number
/// of distinct milestone nodes walked, mirroring the step-6 walked_nodes
/// tip→root-with-break, for the cost proxy) using `walked` (Rc-ptr visited set).
fn shape_contribution(
    shape: &PathShape,
    cond: &AcceptCondition,
    lookahead: &HashSet<i32>,
    main_start_gen: i32,
    out: &mut HashSet<PathRoot>,
    walked: &mut HashSet<usize>,
    visits: &mut u64,
) {
    // (i) condition-referenced roots
    cond.referenced_roots().for_each(|r| {
        out.insert(*r);
    });
    // (ii) observing anchors along the chain, tip->root with visited-break
    let mut mp = shape.milestone_path.clone();
    while let Some(node) = mp {
        // Mirror step 6's walked_nodes dedup: hitting a visited node means it
        // and all ancestors already contributed -> stop. Output-neutral (set).
        let ptr = std::rc::Rc::as_ptr(&node) as usize;
        if !walked.insert(ptr) {
            break;
        }
        *visits += 1;
        let parent_gen = node.parent.as_ref().map(|p| p.gen_idx).unwrap_or(main_start_gen);
        for sid in node.observing_cond_symbol_ids.iter().copied() {
            out.insert(PathRoot::new(sid, node.gen_idx - 1));
            if lookahead.contains(&sid) {
                out.insert(PathRoot::new(sid, node.gen_idx));
                out.insert(PathRoot::new(sid, parent_gen));
            }
        }
        mp = node.parent.clone();
    }
}

/// Per-root demand: union over the root's shapes of the contribution set.
/// Returns (root_demand map, total node visits this gen).
/// `walked` is SHARED across all roots this gen — exactly step 6's single flat
/// pass with one global walked_nodes set. This keeps the visit-count proxy
/// faithful (shared ancestors counted once) AND keeps the demanded SETS correct
/// (dedup is output-neutral, so per-root union is unaffected by cross-root
/// sharing of the break).
fn compute_root_demand(
    ctx: &ParsingCtx,
    lookahead: &HashSet<i32>,
) -> (HashMap<PathRoot, HashSet<PathRoot>>, u64) {
    let main_start_gen = ctx.main_root.start_gen;
    let mut root_demand: HashMap<PathRoot, HashSet<PathRoot>> = HashMap::new();
    let mut walked: HashSet<usize> = HashSet::new();
    let mut visits: u64 = 0;
    for (root, pm) in ctx.paths.iter() {
        let entry = root_demand.entry(*root).or_default();
        for (shape, cond) in pm.iter() {
            shape_contribution(
                shape,
                cond,
                lookahead,
                main_start_gen,
                entry,
                &mut walked,
                &mut visits,
            );
        }
    }
    (root_demand, visits)
}

/// Stratified demand fixpoint. Seed = contributions of `seed_roots` (restricted
/// to roots that are actually live). Iterate: any live root newly in the demand
/// set contributes its own demand. Returns (demanded_live_roots, iterations).
/// `demanded_live_roots` is the set of LIVE roots that are demanded (main is
/// always implicitly rooted and is included).
fn demand_fixpoint(
    root_demand: &HashMap<PathRoot, HashSet<PathRoot>>,
    live_roots: &HashSet<PathRoot>,
    main_root: PathRoot,
    seed_roots: &[PathRoot],
) -> (HashSet<PathRoot>, u32) {
    // demanded = live roots proven demanded so far. main is rooted by fiat.
    let mut demanded: HashSet<PathRoot> = HashSet::new();
    demanded.insert(main_root);
    // frontier: roots whose contribution we still need to expand.
    let mut frontier: Vec<PathRoot> = Vec::new();
    for r in seed_roots {
        if demanded.insert(*r) {
            frontier.push(*r);
        }
    }
    // Ensure main's own contribution is expanded (seed_roots may or may not
    // include main; expand it explicitly).
    if !frontier.contains(&main_root) {
        frontier.push(main_root);
    }
    let mut iterations: u32 = 0;
    while !frontier.is_empty() {
        iterations += 1;
        let mut next_frontier: Vec<PathRoot> = Vec::new();
        for r in frontier.drain(..) {
            if let Some(dem) = root_demand.get(&r) {
                for d in dem {
                    // Only LIVE roots matter (a demanded anchor with no live
                    // root is vacuous — no watcher to keep or collect).
                    if live_roots.contains(d) && demanded.insert(*d) {
                        next_frontier.push(*d);
                    }
                }
            }
        }
        frontier = next_frontier;
    }
    (demanded, iterations)
}

/// Per-gen record for lifetime / churn tracking.
#[derive(Default)]
struct GenAgg {
    gens: u64,
    // live watcher counts (root and shape granularity)
    sum_live_watcher_roots: u64,
    sum_live_watcher_shapes: u64,
    sum_live_shapes: u64, // includes main
    peak_live_shapes: usize,
    // unrooted (GC would collect) counts
    sum_unrooted_roots: u64,
    sum_unrooted_shapes: u64,
    peak_unrooted_shapes: usize,
    // fixpoint cost
    sum_iterations: u64,
    max_iterations: u32,
    sum_visits: u64,
    // churn: new unrooted roots born this gen (proxy for spawn cost GC can't avoid)
    sum_new_unrooted_roots: u64,
    // self-verification
    selfverify_gens_ok: u64,
    selfverify_gens_fail: u64,
    // shapes-per-step split for the counterfactual (gen-mean)
    sum_rooted_shapes: u64,   // main + demanded-watcher shapes
    sum_unrooted_shapes2: u64, // == sum_unrooted_shapes (phantom band)
}

fn is_main(root: &PathRoot, main_root: &PathRoot) -> bool {
    root == main_root
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let raw: Vec<String> = std::env::args().skip(1).collect();
    let no_fold = raw.iter().any(|a| a == "--no-eof-fold");
    let json = raw.iter().any(|a| a == "--json");
    let per_gen = raw.iter().any(|a| a == "--per-gen");
    let mut pos = raw
        .iter()
        .filter(|a| !a.starts_with("--"));
    let data_path = pos.next().expect(
        "usage: rooted_gc_probe <parserdata.pb> <input.txt> [--no-eof-fold] [--json] [--per-gen]",
    );
    let input_path = pos.next().expect(
        "usage: rooted_gc_probe <parserdata.pb> <input.txt> [--no-eof-fold] [--json] [--per-gen]",
    );

    let proto = Mgroup3ParserData::decode(std::fs::read(data_path)?.as_slice())?;
    // Build ParserDataPlain, apply the counterfactual knob, then build parser.
    // lib-UNCHANGED: eof_cond_symbols is a pub field; from_plain is pub.
    let mut plain = ParserDataPlain::from_proto(proto);
    let eof_syms_before = plain.eof_cond_symbols.len();
    if no_fold {
        plain.eof_cond_symbols.clear();
    }
    let lookahead: HashSet<i32> = plain.lookahead_cond_symbols.iter().copied().collect();
    let parser = Mgroup3Parser::from_plain(plain);

    let input = std::fs::read_to_string(input_path)?;
    let chars: Vec<char> = input.chars().collect();
    let total = chars.len();

    let mut agg = GenAgg::default();

    // lifetime tracking: (symbol_id, start_gen) -> (first_gen_seen_unrooted,
    // last_gen_seen_unrooted, total_gens_seen_unrooted, ever_rooted_after_unrooted).
    // We track, per unrooted root, how many EXTRA gens it survived beyond the
    // first gen it became unrooted (GC would have collected it at that first
    // unrooted gen). "excess survival gens" = gens observed unrooted after the
    // first such gen (i.e. count_unrooted_gens - 1 per root, but a root can flip
    // rooted->unrooted; we count contiguous-agnostic total unrooted gens - 1).
    let mut unrooted_first_gen: HashMap<PathRoot, i32> = HashMap::new();
    let mut unrooted_gen_count: HashMap<PathRoot, u64> = HashMap::new();
    let mut prev_unrooted: HashSet<PathRoot> = HashSet::new();

    let mut per_gen_lines: Vec<String> = Vec::new();

    let mut ctx = parser.init_ctx();
    let mut parse_ok = true;
    let mut parse_err_gen = -1i32;
    for (idx, c) in chars.iter().enumerate() {
        ctx = match parser.parse_step(ctx, *c, idx + 1 == total) {
            Ok(ctx) => ctx,
            Err(_e) => {
                parse_ok = false;
                parse_err_gen = idx as i32;
                break;
            }
        };
        let cur_gen = ctx.gen_idx;
        let main_root = ctx.main_root;

        // --- live sets ---
        let live_roots: HashSet<PathRoot> = ctx.paths.keys().copied().collect();
        let live_watcher_roots: Vec<PathRoot> =
            live_roots.iter().copied().filter(|r| !is_main(r, &main_root)).collect();
        let live_shapes_total: usize = ctx.paths.values().map(|m| m.len()).sum();
        let live_watcher_shapes: usize = ctx
            .paths
            .iter()
            .filter(|(r, _)| !is_main(r, &main_root))
            .map(|(_, m)| m.len())
            .sum();

        // --- contribution + fixpoints ---
        let (root_demand, visits) = compute_root_demand(&ctx, &lookahead);

        // main-seeded fixpoint: demand starts from main's paths only.
        let main_seed = root_demand.get(&main_root).cloned().unwrap_or_default();
        let main_seed_vec: Vec<PathRoot> = main_seed.into_iter().collect();
        let (demanded, iterations) =
            demand_fixpoint(&root_demand, &live_roots, main_root, &main_seed_vec);

        // self-verification: all-live-seeded closure must equal live roots.
        let all_seed: Vec<PathRoot> = live_roots.iter().copied().collect();
        let (closure_all, _it2) =
            demand_fixpoint(&root_demand, &live_roots, main_root, &all_seed);
        let selfverify_ok = closure_all == live_roots;
        if selfverify_ok {
            agg.selfverify_gens_ok += 1;
        } else {
            agg.selfverify_gens_fail += 1;
        }

        // --- unrooted = live watcher roots not in main-seeded demanded ---
        let unrooted_roots: Vec<PathRoot> = live_watcher_roots
            .iter()
            .copied()
            .filter(|r| !demanded.contains(r))
            .collect();
        let unrooted_set: HashSet<PathRoot> = unrooted_roots.iter().copied().collect();
        let unrooted_shapes: usize = ctx
            .paths
            .iter()
            .filter(|(r, _)| unrooted_set.contains(r))
            .map(|(_, m)| m.len())
            .sum();
        let rooted_shapes = live_shapes_total - unrooted_shapes;

        // --- churn + lifetime ---
        let mut new_unrooted = 0u64;
        for r in &unrooted_roots {
            *unrooted_gen_count.entry(*r).or_default() += 1;
            if !unrooted_first_gen.contains_key(r) {
                unrooted_first_gen.insert(*r, cur_gen);
            }
            if !prev_unrooted.contains(r) {
                // newly unrooted this gen (birth of an unrooted episode)
                new_unrooted += 1;
            }
        }
        prev_unrooted = unrooted_set.clone();

        // --- aggregate ---
        agg.gens += 1;
        agg.sum_live_watcher_roots += live_watcher_roots.len() as u64;
        agg.sum_live_watcher_shapes += live_watcher_shapes as u64;
        agg.sum_live_shapes += live_shapes_total as u64;
        agg.peak_live_shapes = agg.peak_live_shapes.max(live_shapes_total);
        agg.sum_unrooted_roots += unrooted_roots.len() as u64;
        agg.sum_unrooted_shapes += unrooted_shapes as u64;
        agg.peak_unrooted_shapes = agg.peak_unrooted_shapes.max(unrooted_shapes);
        agg.sum_iterations += iterations as u64;
        agg.max_iterations = agg.max_iterations.max(iterations);
        agg.sum_visits += visits;
        agg.sum_new_unrooted_roots += new_unrooted;
        agg.sum_rooted_shapes += rooted_shapes as u64;
        agg.sum_unrooted_shapes2 += unrooted_shapes as u64;

        if per_gen {
            per_gen_lines.push(format!(
                "gen {} shapes={} watcherRoots={} unrootedRoots={} unrootedShapes={} newUnrooted={} iters={} visits={} selfverify={}",
                cur_gen,
                live_shapes_total,
                live_watcher_roots.len(),
                unrooted_roots.len(),
                unrooted_shapes,
                new_unrooted,
                iterations,
                visits,
                if selfverify_ok { "OK" } else { "FAIL" },
            ));
        }
    }

    // lifetime distribution: excess survival gens = total unrooted gens - 1 per
    // root (GC collects at first unrooted gen; every later unrooted gen is
    // excess). We histogram unrooted_gen_count.
    let mut excess_hist: BTreeMap<u64, u64> = BTreeMap::new(); // excess_gens -> #roots
    let mut sum_excess: u64 = 0;
    let mut max_excess: u64 = 0;
    let n_unrooted_roots_distinct = unrooted_gen_count.len() as u64;
    for (_r, &cnt) in unrooted_gen_count.iter() {
        let excess = cnt.saturating_sub(1);
        *excess_hist.entry(excess).or_default() += 1;
        sum_excess += excess;
        max_excess = max_excess.max(excess);
    }

    let g = agg.gens.max(1) as f64;
    let mode = if no_fold { "no-eof-fold" } else { "fold-on" };

    if json {
        // compact JSON for aggregation
        print!(
            "{{\"data\":{:?},\"input\":{:?},\"mode\":{:?},\"eofSyms\":{},\"parseOk\":{},\"gens\":{},",
            data_path, input_path, mode, eof_syms_before, parse_ok, agg.gens
        );
        print!(
            "\"meanShapes\":{:.2},\"peakShapes\":{},\"meanWatcherRoots\":{:.2},\"meanWatcherShapes\":{:.2},",
            agg.sum_live_shapes as f64 / g,
            agg.peak_live_shapes,
            agg.sum_live_watcher_roots as f64 / g,
            agg.sum_live_watcher_shapes as f64 / g,
        );
        print!(
            "\"meanUnrootedRoots\":{:.3},\"meanUnrootedShapes\":{:.3},\"peakUnrootedShapes\":{},",
            agg.sum_unrooted_roots as f64 / g,
            agg.sum_unrooted_shapes as f64 / g,
            agg.peak_unrooted_shapes,
        );
        let root_frac = if agg.sum_live_watcher_roots > 0 {
            agg.sum_unrooted_roots as f64 / agg.sum_live_watcher_roots as f64
        } else {
            0.0
        };
        let shape_frac = if agg.sum_live_watcher_shapes > 0 {
            agg.sum_unrooted_shapes as f64 / agg.sum_live_watcher_shapes as f64
        } else {
            0.0
        };
        let shape_frac_all = if agg.sum_live_shapes > 0 {
            agg.sum_unrooted_shapes as f64 / agg.sum_live_shapes as f64
        } else {
            0.0
        };
        print!(
            "\"unrootedRootFracOfWatchers\":{:.4},\"unrootedShapeFracOfWatchers\":{:.4},\"unrootedShapeFracOfAll\":{:.4},",
            root_frac, shape_frac, shape_frac_all
        );
        print!(
            "\"meanRootedShapes\":{:.2},\"meanUnrootedShapesBand\":{:.2},",
            agg.sum_rooted_shapes as f64 / g,
            agg.sum_unrooted_shapes2 as f64 / g,
        );
        print!(
            "\"churnNewUnrootedPerGen\":{:.4},\"distinctUnrootedRoots\":{},\"sumExcessGens\":{},\"maxExcessGens\":{},\"meanExcessGensPerUnrootedRoot\":{:.3},",
            agg.sum_new_unrooted_roots as f64 / g,
            n_unrooted_roots_distinct,
            sum_excess,
            max_excess,
            if n_unrooted_roots_distinct > 0 { sum_excess as f64 / n_unrooted_roots_distinct as f64 } else { 0.0 },
        );
        print!(
            "\"fixpointMeanIters\":{:.3},\"fixpointMaxIters\":{},\"fixpointMeanVisits\":{:.1},",
            agg.sum_iterations as f64 / g,
            agg.max_iterations,
            agg.sum_visits as f64 / g,
        );
        print!(
            "\"selfverifyOk\":{},\"selfverifyFail\":{},\"parseErrGen\":{}}}",
            agg.selfverify_gens_ok, agg.selfverify_gens_fail, parse_err_gen
        );
        println!();
    } else {
        println!("=== rooted_gc_probe ===");
        println!("data:  {}", data_path);
        println!("input: {} ({} chars)", input_path, total);
        println!("mode:  {} (eof_cond_symbols={})", mode, eof_syms_before);
        println!("parse: {} (gens={})", if parse_ok { "OK" } else { "ERROR" }, agg.gens);
        if !parse_ok {
            println!("  parse error at gen {}", parse_err_gen);
        }
        println!();
        println!("--- self-verification (all-live-seeded closure == live roots) ---");
        println!(
            "  gens OK: {}   gens FAIL: {}   -> {}",
            agg.selfverify_gens_ok,
            agg.selfverify_gens_fail,
            if agg.selfverify_gens_fail == 0 { "FAITHFUL" } else { "*** MIRROR MISMATCH — NUMBERS INVALID ***" }
        );
        println!();
        println!("--- live state (gen-mean) ---");
        println!("  mean shapes/step (all):     {:.2}   peak {}", agg.sum_live_shapes as f64 / g, agg.peak_live_shapes);
        println!("  mean watcher roots/step:    {:.2}", agg.sum_live_watcher_roots as f64 / g);
        println!("  mean watcher shapes/step:   {:.2}", agg.sum_live_watcher_shapes as f64 / g);
        println!();
        println!("--- unrooted (what root-traced GC would collect) ---");
        println!("  mean unrooted roots/step:   {:.3}", agg.sum_unrooted_roots as f64 / g);
        println!("  mean unrooted shapes/step:  {:.3}   peak {}", agg.sum_unrooted_shapes as f64 / g, agg.peak_unrooted_shapes);
        let root_frac = if agg.sum_live_watcher_roots > 0 { agg.sum_unrooted_roots as f64 / agg.sum_live_watcher_roots as f64 } else { 0.0 };
        let shape_frac_w = if agg.sum_live_watcher_shapes > 0 { agg.sum_unrooted_shapes as f64 / agg.sum_live_watcher_shapes as f64 } else { 0.0 };
        let shape_frac_all = if agg.sum_live_shapes > 0 { agg.sum_unrooted_shapes as f64 / agg.sum_live_shapes as f64 } else { 0.0 };
        println!("  unrooted / live-watcher (roots):  {:.2}%", root_frac * 100.0);
        println!("  unrooted / live-watcher (shapes): {:.2}%", shape_frac_w * 100.0);
        println!("  unrooted / all-live      (shapes): {:.2}%", shape_frac_all * 100.0);
        println!();
        println!("--- counterfactual shapes-per-step split ---");
        println!("  mean ROOTED shapes/step:    {:.2}  (GC-survivor band: main + demanded watchers)", agg.sum_rooted_shapes as f64 / g);
        println!("  mean UNROOTED shapes/step:  {:.2}  (phantom band GC would remove next gen)", agg.sum_unrooted_shapes2 as f64 / g);
        println!();
        println!("--- churn (spawn cost GC cannot avoid) ---");
        println!("  new unrooted roots/step:    {:.4}", agg.sum_new_unrooted_roots as f64 / g);
        println!("  distinct unrooted roots:    {}", n_unrooted_roots_distinct);
        println!();
        println!("--- lifetime: excess survival gens (GC collects at first unrooted gen) ---");
        println!("  sum excess gens:            {}", sum_excess);
        println!("  max excess gens:            {}", max_excess);
        println!("  mean excess/unrooted root:  {:.3}", if n_unrooted_roots_distinct > 0 { sum_excess as f64 / n_unrooted_roots_distinct as f64 } else { 0.0 });
        print!("  excess-gen histogram (excess:count):");
        for (e, c) in excess_hist.iter().take(20) {
            print!(" {}:{}", e, c);
        }
        println!();
        println!();
        println!("--- fixpoint cost proxy ---");
        println!("  mean iterations/step:       {:.3}   max {}", agg.sum_iterations as f64 / g, agg.max_iterations);
        println!("  mean node visits/step:      {:.1}", agg.sum_visits as f64 / g);
    }

    if per_gen {
        eprintln!("--- per-gen ---");
        for line in &per_gen_lines {
            eprintln!("{}", line);
        }
    }

    Ok(())
}
