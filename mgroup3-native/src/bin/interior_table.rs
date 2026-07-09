//! mgroup4 Phase 0 measurement probe P4: precompute size
//! estimate. Static analysis of parserdata (no parse). Spec:
//! `mgroup3/docs/mgroup4_bounded_interior_groups.md` §3 (P4).
//!
//! Question: for a bounded window n, how big is the extra precomputed transition
//! table mgroup4 needs — (depth-k interior group × completing child symbol) →
//! (fragment class / retain) — relative to the CURRENT tip-group transition
//! table? Reported as a multiple, for n=2 and n=3, per grammar.
//!
//! ==== Current tip-group transition table (baseline) ====
//! mgroup3 precomputes, per tip position:
//!   - term_actions[groupId] -> per-char transitions (the tip TERM table)
//!   - tip_edge_actions: (parentTemplate, tipGroupId) -> EdgeAction  (tip REDUCE table)
//!   - mid_edge_actions: (parentTemplate, tipKernel)  -> EdgeAction  (mid REDUCE table)
//! The reduce (child-completion) transitions are the tip_edge + mid_edge tables:
//! that is the direct analog of what mgroup4 must add per interior depth. We
//! report the tip TERM table size too for context, but the mgroup4 table is a
//! REDUCE table, so the primary baseline is |tip_edge_actions| + |mid_edge_actions|.
//!
//! ==== mgroup4 depth-k interior table ====
//! At interior depth k (k in 2..=n; k=1 is the tip, already handled), when a
//! child subtree below the interior node completes with some symbol, the
//! interior node — now a GROUP of milestones — must either fragment (members
//! transition differently) or stay merged. The transition is keyed by
//! (parent-of-interior, interior-group, completing-child-symbol). We enumerate
//! its reachable size two ways and report both (the spec asks the approximation
//! level to be stated):
//!
//!   (A) UPPER BOUND (all combos): every milestone group that can appear at an
//!       interior position (== every group that is an `append` target anywhere)
//!       paired with every distinct completing symbol (== every distinct symbol
//!       that keys a reduce edge action, i.e. can complete-and-propagate). This
//!       is the loose product cap: |interior-candidate groups| × |completing syms|.
//!
//!   (B) GRAMMAR-REACHABLE (tighter): a (interior-group, completing-symbol) pair
//!       is reachable only if some kernel in that group expects the completing
//!       symbol next (i.e. the group actually has a dot before that symbol, so a
//!       reduce of that symbol can land on it). This is derived from the
//!       milestone-group kernel membership crossed with the NGrammar's "what
//!       symbol sits right after the dot" for each kernel. When the parserdata
//!       carries the NGrammar (untrimmed) we compute (B); otherwise we fall back
//!       to (A) and say so.
//!
//! For window n, the table spans depths 2..=n = (n-1) depth levels; but the SAME
//! group can recur at different depths, so the distinct-entry count is per-depth
//! union'd (a group×symbol entry is the same regardless of which depth it sits
//! at — the reduce decision depends on the group and the child, not the absolute
//! depth). So mgroup4_table(n) == the (group×symbol) reduce entries, INDEPENDENT
//! of n beyond needing groups that CAN sit within depth<=n. We approximate "can
//! sit within depth n" as "all interior-candidate groups" (an upper bound; a
//! true depth-bounded reachability would need the parse-graph, out of scope for
//! a static probe — stated as a limitation). So we report the same core count
//! for n=2 and n=3 (both enumerate the full interior reduce table); the n
//! parameter only bounds the RUNTIME window, not the static table. We flag this.

use std::collections::{HashMap, HashSet};

use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::{EdgeAction, Mgroup3ParserData};
use prost::Message;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut args = std::env::args().skip(1);
    let data_path = args.next().expect("usage: interior_table <parserdata.pb>");
    let label = args.next().unwrap_or_else(|| "?".to_string());

    let data = Mgroup3ParserData::decode(std::fs::read(&data_path)?.as_slice())?;

    // ---- current tip-group transition tables ----
    let n_groups = data.milestone_groups.len();
    let n_term_group_tables = data.term_actions.len(); // groups with a term table
    let n_term_entries: usize = data
        .term_actions
        .values()
        .map(|tga| tga.actions.len())
        .sum();
    let n_tip_edge = data.tip_edge_actions.len();
    let n_mid_edge = data.mid_edge_actions.len();
    let tip_reduce_table = n_tip_edge + n_mid_edge;

    // ---- distinct completing symbols (symbols that can complete-and-propagate) ----
    // A reduce edge fires when a child subtree completes. In the edge-action
    // tables the "tip" side is the completing child (its (sym,ptr) is the node
    // being reduced). Collect distinct completing symbols from both edge tables.
    let mut completing_syms: HashSet<i32> = HashSet::new();
    for pair in &data.tip_edge_actions {
        // tip group id -> its member kernels' symbols are the completing children
        if let Some(mg) = data.milestone_groups.get(&pair.tip_group_id) {
            for k in &mg.kernels {
                completing_syms.insert(k.symbol_id);
            }
        }
    }
    for pair in &data.mid_edge_actions {
        if let Some(tip) = &pair.tip {
            completing_syms.insert(tip.symbol_id);
        }
    }

    // ---- interior-candidate groups (groups that can appear behind the tip) ----
    // A group can be interior if it is ever an `append` target (pushed onto the
    // chain and later covered by a deeper append) — in practice essentially every
    // group that appears as an append milestone group id. Collect them.
    let mut interior_groups: HashSet<i32> = HashSet::new();
    // from term actions (replace_and_append.append)
    for tga in data.term_actions.values() {
        for a in &tga.actions {
            if let Some(ta) = &a.term_action {
                for raa in &ta.replace_and_appends {
                    if let Some(app) = &raa.append {
                        interior_groups.insert(app.milestone_group_id);
                    }
                }
            }
        }
    }
    // from edge actions (append_milestone_groups)
    let mut collect_edge = |ea: &Option<EdgeAction>| {
        if let Some(ea) = ea {
            for app in &ea.append_milestone_groups {
                interior_groups.insert(app.milestone_group_id);
            }
        }
    };
    for pair in &data.tip_edge_actions {
        collect_edge(&pair.edge_action);
    }
    for pair in &data.mid_edge_actions {
        collect_edge(&pair.edge_action);
    }
    // Fallback: if none found (shouldn't happen), use all groups.
    if interior_groups.is_empty() {
        interior_groups.extend(data.milestone_groups.keys().copied());
    }

    let n_interior_candidates = interior_groups.len();
    let n_completing_syms = completing_syms.len();

    // ---- (A) upper bound: interior groups × completing symbols ----
    let upper_bound = n_interior_candidates * n_completing_syms;

    let reachable_pairs: usize;
    let reachable_available: bool;
    // ---- (B) reduce-table-reachable (tighter, grounded in the actual edge
    // tables) ----
    // The existing edge-action tables ALREADY enumerate the reduce transitions
    // at the tip: each entry keys on (parent kernel template, completing child)
    // where the child is a tip group (tip_edge) or a tip kernel (mid_edge). In
    // mgroup4 the interior node at depth k plays the role of this "parent", but
    // it is now a GROUP. So the reachable interior reduce table = the set of
    // (interior-group, completing-child) pairs such that the interior group
    // contains a kernel template that is a `parent` of some existing reduce edge
    // for that child. This is exact w.r.t. the compiled reduce table (no grammar
    // guess): we cross the edge-table's (parent, child) keys with "which interior
    // groups contain that parent kernel". Available whenever the tables are
    // present (always) — does not require the NGrammar.
    reachable_available = true;
    {
        // parent kernel template -> set of interior groups that contain it
        let mut groups_with_kernel: HashMap<(i32, i32), Vec<i32>> = HashMap::new();
        for &gid in &interior_groups {
            if let Some(mg) = data.milestone_groups.get(&gid) {
                for k in &mg.kernels {
                    groups_with_kernel
                        .entry((k.symbol_id, k.pointer))
                        .or_default()
                        .push(gid);
                }
            }
        }
        // distinct (interiorGroup, completingChildKey) pairs
        let mut pairs: HashSet<(i32, i64)> = HashSet::new();
        // tip_edge: child key = tip_group_id (negated to avoid collision with
        // mid_edge kernel keys)
        for pair in &data.tip_edge_actions {
            if let Some(parent) = &pair.parent {
                if let Some(gids) = groups_with_kernel.get(&(parent.symbol_id, parent.pointer)) {
                    let child_key = -(pair.tip_group_id as i64) - 1;
                    for &g in gids {
                        pairs.insert((g, child_key));
                    }
                }
            }
        }
        // mid_edge: child key = tip kernel (sym,ptr) packed positive
        for pair in &data.mid_edge_actions {
            if let (Some(parent), Some(tip)) = (&pair.parent, &pair.tip) {
                if let Some(gids) = groups_with_kernel.get(&(parent.symbol_id, parent.pointer)) {
                    let child_key = ((tip.symbol_id as i64) << 20) | (tip.pointer as i64 & 0xFFFFF);
                    for &g in gids {
                        pairs.insert((g, child_key));
                    }
                }
            }
        }
        reachable_pairs = pairs.len();
    }
    let _ = &data.grammar; // grammar not required for (B)

    // ---- report ----
    println!("=== P4 interior-table estimate: {} ===", label);
    println!("  milestone groups (total)           = {}", n_groups);
    println!("  --- current tip-group transition tables ---");
    println!("  term_actions: groups with table    = {}", n_term_group_tables);
    println!("  term_actions: total (grp,charclass) = {}", n_term_entries);
    println!("  tip_edge_actions (reduce)           = {}", n_tip_edge);
    println!("  mid_edge_actions (reduce)           = {}", n_mid_edge);
    println!("  => tip REDUCE table  = tip_edge+mid_edge = {}", tip_reduce_table);
    println!("  => tip TERM   table  = term entries      = {}", n_term_entries);
    println!("  --- mgroup4 interior reduce table (depth 2..=n) ---");
    println!("  interior-candidate groups           = {}", n_interior_candidates);
    println!("  distinct completing symbols         = {}", n_completing_syms);
    println!("  (A) upper bound = groups × syms      = {}", upper_bound);
    if reachable_available {
        println!("  (B) reduce-table-reachable (grp,child) = {}", reachable_pairs);
    } else {
        println!("  (B) reduce-table-reachable          = N/A");
    }
    println!("  --- multiples vs current tip REDUCE table ({}) ---", tip_reduce_table);
    let mult = |x: usize| if tip_reduce_table == 0 { f64::NAN } else { x as f64 / tip_reduce_table as f64 };
    println!("  (A) upper / tip-reduce      = {:.2}x", mult(upper_bound));
    if reachable_available {
        println!("  (B) reachable / tip-reduce  = {:.2}x", mult(reachable_pairs));
    }
    println!("  --- multiples vs current tip REDUCE+TERM ({}) ---", tip_reduce_table + n_term_entries);
    let denom_all = (tip_reduce_table + n_term_entries).max(1);
    println!("  (A) upper / (reduce+term)   = {:.2}x", upper_bound as f64 / denom_all as f64);
    if reachable_available {
        println!("  (B) reachable / (reduce+term) = {:.2}x", reachable_pairs as f64 / denom_all as f64);
    }
    println!("  NOTE: the depth-boundedness of n only limits the RUNTIME window; the");
    println!("  static reduce table is the same set of (group,sym) entries for n=2");
    println!("  and n=3 (a true depth<=n reachability needs the parse graph). The n=2");
    println!("  and n=3 static-table sizes reported here are thus identical = the full");
    println!("  interior reduce table. Per-depth replication (×(n-1)) is an alternative");
    println!("  upper reading if entries are stored separately per depth level:");
    println!("    n=2 (×1): {} (A) / {} (B)", upper_bound, if reachable_available { reachable_pairs.to_string() } else { "N/A".into() });
    println!("    n=3 (×2): {} (A) / {} (B)", upper_bound * 2, if reachable_available { (reachable_pairs * 2).to_string() } else { "N/A".into() });

    // JSON to stderr
    eprintln!(
        "JSON_BEGIN{{\"label\":\"{}\",\"groups\":{},\"tip_reduce\":{},\"tip_term\":{},\"interior_groups\":{},\"completing_syms\":{},\"upper_bound\":{},\"reachable\":{},\"reachable_available\":{},\"tip_edge\":{},\"mid_edge\":{}}}JSON_END",
        label, n_groups, tip_reduce_table, n_term_entries, n_interior_candidates, n_completing_syms,
        upper_bound, reachable_pairs, reachable_available, n_tip_edge, n_mid_edge
    );

    Ok(())
}
