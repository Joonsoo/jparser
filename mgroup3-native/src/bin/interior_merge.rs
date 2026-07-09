//! mgroup4 Phase 0 measurement probe: interior-merge
//! ceiling (P1), fork-depth distribution (P2), and window-exit-vs-pop (P3).
//! Spec: `mgroup3/docs/mgroup4_bounded_interior_groups.md` §3.
//!
//! Read-only: it drives the UNMODIFIED parser (`parse_step`) and re-aggregates
//! the live path shapes at each gen. The parser algorithm and its output are
//! untouched — this only counts.
//!
//! ==== ADJUSTED MODE (mgroup4_phase_a_design.md §H5, §2.1) ====
//! CLI: `interior_merge <parserdata.pb> <input.txt> [--adjusted]`. Default
//! (no flag) mode's output is BYTE-IDENTICAL to the original probe (same
//! merge rule, same ns={1,2,3,4,inf}) — reproducibility with the recorded
//! Phase 0 numbers is preserved.
//!
//! `--adjusted` narrows the ceiling to what a real runtime merge could do:
//!   1. Per-node fold now includes `observing_cond_symbol_ids` (the Rust
//!      mirror of Kotlin's `MilestonePath.observingCondSymbolIds`) — §H5(b):
//!      "노드의 gen 과 observingCondSymbolIds 는 동일해야 함" even at the
//!      masked (wildcard) position.
//!   2. The wildcard mask at the differing position is narrowed: only
//!      (symbol_id, pointer) are wildcarded there. gen_idx and
//!      observing_cond_symbol_ids AT THAT POSITION remain part of the
//!      invariant key, so members must match on those even where they're
//!      allowed to differ on (symbol_id, pointer). This is §H5(b)'s "마스크
//!      되는 노드의 milestone Kernel (symbolId, pointer) 만 상이 허용".
//!   3. The shape's `AcceptCondition` (the `PathMap` value, i.e. `ctx.paths
//!      [root]`'s value for this shape) is folded into the key at the WHOLE
//!      shape level (condition is not a per-node property — it's attached to
//!      the shape as a whole, `PathMap = Map<PathShape, AcceptCondition>`).
//!      Members only merge if their conditions are structurally equal.
//!      `AcceptCondition` derives `PartialEq + Eq + Hash` structurally (see
//!      `accept_condition/mod.rs`) — no `Rc`/interior mutability inside the
//!      enum, so this is genuine structural equivalence, NOT a pointer-
//!      identity approximation. (The task brief allowed a pointer-identity
//!      fallback if structural Eq/Hash weren't available or were expensive;
//!      they're available and cheap here, so we use them directly — this
//!      section documents that no conservative approximation was needed.)
//! n list is extended in adjusted mode: ns = {1,2,3,4,6,8,inf}. P3 is not
//! run/reported in adjusted mode (P1/P2 only, per task brief). main/watcher/
//! all bucket split is unchanged.
//!
//! ==== Terminology (must line up with shape_integral's "shape") ====
//! A live path is `root → m_1 → m_2 → ... → m_L → tipGroup(id)`. Each live
//! (root, PathShape) entry is ONE shape (== what shape_integral sums via
//! `ctx.paths.values().map(|m| m.len()).sum()`).
//!
//! DEPTH is distance from the tip:
//!   - depth 1  = the tip group (tip_group_id). This is the "n=1 window":
//!                current mgroup3 already groups here, so n=1 == base count.
//!   - depth 2  = the tip-most milestone (chain[L-1] in root..tip order)
//!   - depth d  = the milestone at chain index (L - (d-1)) for d in 2..=L+1
//!   - depth L+1 = the root-most milestone.
//! A path of L milestones has window positions at depths 1..=L+1.
//!
//! Node identity for masking = the full runtime triple (symbol_id, pointer,
//! gen_idx) — exactly what distinguishes two PathShapes' chains. Two shapes
//! are "identical outside position d" iff their tip_group_id, length, and every
//! chain node except depth d are byte-equal.
//!
//! ==== P1 merge rule (k=1, greedy, well-defined) ====
//! For a fixed depth d, "wildcard-key = signature with the depth-d element
//! replaced by a sentinel" is an equivalence relation; its size>=2 classes are
//! exactly the sets of shapes that differ only at depth d. To avoid double-
//! counting shapes reachable at more than one d, we consume greedily:
//!   for d = 2..=n (ascending):
//!     partition the not-yet-merged shapes of this gen by their wildcard-d key;
//!     every class of size >= 2 is a confirmed merge group at depth d — mark
//!     its members merged (consumed) so a later d cannot re-merge them.
//! d starts at 2 (the tip group at d=1 is already the current mgroup3 grouping;
//! shapes are distinct PathShapes, so d=1 is never a merge position).
//! merged_count(n) = base_count - sum(group_size - 1) over confirmed groups.
//! n=inf uses d = 2..=(max L+1 over all live shapes) = all interior depths.
//!
//! ==== main vs watcher split ====
//! main root = (symbol_id, start_gen) == ctx.main_root; every other root is a
//! watcher (bounded or lookahead, per lookahead_cond_symbol_ids). Merge is done
//! WITHIN each flavor bucket (main-only, watcher-only) — §1 of the spec: this is
//! interior merge inside a flavor, not across main/watcher (that axis was the
//! sharing_ceiling study, ceiling 1.00x). We report per-flavor and combined.
//!
//! ==== P2 (fork-depth histogram) ====
//! For every confirmed merge group (at any n up to inf), record the differing
//! position's depth d, weighted by (group_size - 1) (the number of shapes that
//! collapse). Gen-weighted (summed over all gens). Coverage("n<=k") =
//! collapses whose depth <= k  /  total collapses at n=inf.
//!
//! ==== P3 (window-exit vs pop) — approximate, heuristic documented below ====
//! A merge group is born at some gen with a set of member shapes differing at
//! depth d. As the parse advances the window slides tip-ward: the differing
//! interior node's depth grows by 1 whenever the chain grows below it. We
//! APPROXIMATE per-gen (no cross-gen shape-identity tracking — too costly at
//! jquery scale; the runtime exposes no stable node ids across gens).
//!
//! Matching key R = the group's ROOT-SIDE context: root identity + the chain
//! nodes STRICTLY DEEPER (root-ward) than the differing node — prefix hash
//! pre[idx], NO tip, NO length, NO idx. Why R is the right invariant: for a
//! group at depth d on a chain of length L, idx = L-(d-1). If the chain grows
//! by one (L'=L+1) and the node slides to depth d+1, idx' = L'-d = idx — the
//! prefix [0..idx) is byte-identical. Same for persistence (L,d unchanged) and
//! for tip-ward shrink (L'=L-1, d'=d-1 -> idx'=idx). The tip group MUST NOT be
//! in R: expand steps change the tip group id, which would break the match on
//! exactly the transitions we need to observe (that was a measured bias:
//! exits were systematically under-counted before this fix).
//!
//! Per-gen transition classification, for each PREVIOUS-gen group (d, R),
//! against the current gen's groups indexed as R -> {depths}:
//!   - (d+1) in cur[R]            -> EXIT at boundary d (node slid deeper)
//!   - else (d) or (d-1) in cur[R] -> PERSIST (same/shallower depth; NOT an
//!                                   event — excluded from the denominator;
//!                                   counting it as pop would dilute exit-rate
//!                                   by the group's residence time)
//!   - else                        -> POP (group dissolved before exiting)
//! exit-rate(boundary d) = exits_d / (exits_d + pops_d). This is a per-gen
//! estimate, not a full lifetime trace. False-positive exits remain possible
//! (a different fork of the same R coincidentally sitting at d+1) — approximate.

use std::collections::{HashMap, HashSet};

use mgroup3_native::parser::Mgroup3Parser;
use mgroup3_native::parsing_ctx::{ParsingCtx, PathShape};
use mgroup3_native::path_root::PathRoot;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

/// A compact per-shape signature: tip group + chain nodes in root..tip order.
/// depth of chain element at vec index j (0-based, root..tip) is (L - j) + 1
/// where L = chain.len(). Tip milestone (j=L-1) has depth 2.
///
/// Keys are EXACT 128-bit values (no data loss, no collision risk in practice):
/// per node we fold (symbol_id, pointer, gen_idx) with two independent seeds and
/// keep left-prefix and right-suffix cumulative hashes so any wildcard-d key is
/// computed in O(1) from (prefix[idx], suffix[idx+1], L, tip, idx). Position and
/// length are part of the key, so different-length or shifted shapes never
/// collide. Two 64-bit lanes (u128) make an accidental merge astronomically
/// unlikely — this is a ceiling measurement, so we keep it effectively exact.
///
/// ADJUSTED MODE additionally keeps, per node, a SPLIT fold: `tmpl_pre`/
/// `tmpl_suf` cover only (symbol_id, pointer) — the part that's allowed to
/// differ at the wildcarded position — and `inv_pre`/`inv_suf` cover
/// (gen_idx, observing_cond_symbol_ids) — the part that must match even at
/// the wildcarded position (mgroup4_phase_a_design.md §H5(b)). The base-mode
/// `pre`/`suf` fields fold the full node triple as before (untouched — base
/// mode's wildcard_key/root_side_key logic and byte output are unchanged).
/// The shape-level `AcceptCondition` (adjusted-mode only) is folded into
/// `cond_hash`, structurally (derived Hash/Eq on `AcceptCondition` — no
/// Rc/interior mutability in the enum, so this is exact structural
/// equivalence, not a pointer-identity approximation).
#[derive(Clone)]
struct ShapeSig {
    tip: i32,
    chain_len: usize,
    /// prefix cumulative hash: pre[j] = fold of nodes [0..j) (two lanes)
    pre: Vec<(u64, u64)>, // len = L+1
    /// suffix cumulative hash: suf[j] = fold of nodes [j..L) (two lanes)
    suf: Vec<(u64, u64)>, // len = L+1
    /// adjusted-mode only: template-only (symbol_id, pointer) prefix/suffix folds
    tmpl_pre: Vec<(u64, u64)>,
    tmpl_suf: Vec<(u64, u64)>,
    /// adjusted-mode only: invariant (gen_idx, observing) prefix/suffix folds
    inv_pre: Vec<(u64, u64)>,
    inv_suf: Vec<(u64, u64)>,
    /// adjusted-mode only: structural hash of the shape's AcceptCondition
    /// (the PathMap value for this shape) — folded into every wildcard key so
    /// members only merge when their conditions are structurally equal.
    cond_hash: (u64, u64),
}

#[inline]
fn mix(seed: u64, sy: i32, p: i32, g: i32) -> u64 {
    use std::hash::{Hash, Hasher};
    use rustc_hash::FxHasher;
    let mut h = FxHasher::default();
    seed.hash(&mut h);
    sy.hash(&mut h);
    p.hash(&mut h);
    g.hash(&mut h);
    h.finish()
}

/// Mix a variable-length slice (used for observing_cond_symbol_ids) into a hash.
#[inline]
fn mix_slice(seed: u64, ids: &[i32]) -> u64 {
    use std::hash::{Hash, Hasher};
    use rustc_hash::FxHasher;
    let mut h = FxHasher::default();
    seed.hash(&mut h);
    ids.len().hash(&mut h);
    for id in ids {
        id.hash(&mut h);
    }
    h.finish()
}

/// Structural hash of an `AcceptCondition`, two independent lanes. Uses the
/// derived `Hash` impl directly (see accept_condition/mod.rs: plain enum, no
/// Rc/OnceCell inside) — genuine structural equivalence.
#[inline]
fn cond_hash(cond: &mgroup3_native::accept_condition::AcceptCondition) -> (u64, u64) {
    use std::hash::{Hash, Hasher};
    use rustc_hash::FxHasher;
    let mut h0 = FxHasher::default();
    0x5555u64.hash(&mut h0);
    cond.hash(&mut h0);
    let mut h1 = FxHasher::default();
    0x6666u64.hash(&mut h1);
    cond.hash(&mut h1);
    (h0.finish(), h1.finish())
}

#[inline]
fn combine(acc: u64, node: u64) -> u64 {
    // order-sensitive polynomial fold
    acc.wrapping_mul(0x100000001b3).wrapping_add(node ^ (node >> 29))
}

impl ShapeSig {
    /// `root`: the PathRoot owning this shape. Its identity is folded into the
    /// prefix-hash base pre[0], so both the P1 wildcard keys and the P3
    /// root_side key are ROOT-SCOPED: shapes of different roots never merge
    /// (mgroup4's interior group merges paths of ONE root), and P3 tracking
    /// never matches across roots.
    ///
    /// `cond`: the shape's AcceptCondition (PathMap value). Only consulted in
    /// adjusted mode (folded into `cond_hash`); base mode ignores it entirely
    /// so base-mode output is unaffected by which conditions are attached.
    fn of(
        shape: &PathShape,
        root: &PathRoot,
        cond: &mgroup3_native::accept_condition::AcceptCondition,
    ) -> ShapeSig {
        // collect chain root..tip: (symbol_id, pointer, gen_idx, observing)
        let mut nodes: Vec<(i32, i32, i32, std::sync::Arc<[i32]>)> = Vec::new();
        let mut cur = shape.milestone_path.clone();
        while let Some(m) = cur {
            nodes.push((
                m.milestone.symbol_id,
                m.milestone.pointer,
                m.gen_idx,
                m.observing_cond_symbol_ids.clone(),
            ));
            cur = m.parent.clone();
        }
        nodes.reverse(); // root..tip
        let l = nodes.len();
        let mut pre = vec![(0u64, 0u64); l + 1];
        let mut suf = vec![(0u64, 0u64); l + 1];
        let mut tmpl_pre = vec![(0u64, 0u64); l + 1];
        let mut tmpl_suf = vec![(0u64, 0u64); l + 1];
        let mut inv_pre = vec![(0u64, 0u64); l + 1];
        let mut inv_suf = vec![(0u64, 0u64); l + 1];
        // root identity as the prefix-hash base (independent lane seeds)
        pre[0] = (
            mix(0x3333, root.symbol_id, root.start_gen, 0),
            mix(0x4444, root.symbol_id, root.start_gen, 0),
        );
        tmpl_pre[0] = pre[0];
        inv_pre[0] = (
            mix(0x7777, root.symbol_id, root.start_gen, 0),
            mix(0x8888, root.symbol_id, root.start_gen, 0),
        );
        for j in 0..l {
            let (sy, p, g, ref obs) = nodes[j];
            let a = mix(0x1111, sy, p, g);
            let b = mix(0x2222, sy, p, g);
            pre[j + 1] = (combine(pre[j].0, a), combine(pre[j].1, b));
            // template-only (symbol_id, pointer) — no gen, no observing
            let ta = mix(0x1313, sy, p, 0);
            let tb = mix(0x2323, sy, p, 0);
            tmpl_pre[j + 1] = (combine(tmpl_pre[j].0, ta), combine(tmpl_pre[j].1, tb));
            // invariant (gen_idx, observing) — no symbol/pointer
            let ia = combine(mix(0x1717, 0, 0, g), mix_slice(0x1818, obs));
            let ib = combine(mix(0x2727, 0, 0, g), mix_slice(0x2828, obs));
            inv_pre[j + 1] = (combine(inv_pre[j].0, ia), combine(inv_pre[j].1, ib));
        }
        for j in (0..l).rev() {
            let (sy, p, g, ref obs) = nodes[j];
            let a = mix(0x1111, sy, p, g);
            let b = mix(0x2222, sy, p, g);
            suf[j] = (combine(suf[j + 1].0, a), combine(suf[j + 1].1, b));
            let ta = mix(0x1313, sy, p, 0);
            let tb = mix(0x2323, sy, p, 0);
            tmpl_suf[j] = (combine(tmpl_suf[j + 1].0, ta), combine(tmpl_suf[j + 1].1, tb));
            let ia = combine(mix(0x1717, 0, 0, g), mix_slice(0x1818, obs));
            let ib = combine(mix(0x2727, 0, 0, g), mix_slice(0x2828, obs));
            inv_suf[j] = (combine(inv_suf[j + 1].0, ia), combine(inv_suf[j + 1].1, ib));
        }
        ShapeSig {
            tip: shape.tip_group_id,
            chain_len: l,
            pre,
            suf,
            tmpl_pre,
            tmpl_suf,
            inv_pre,
            inv_suf,
            cond_hash: cond_hash(cond),
        }
    }

    /// number of window positions = L milestones + 1 tip group
    fn max_depth(&self) -> usize {
        self.chain_len + 1
    }

    /// The chain vec index for a given depth d>=2, or None if out of range.
    fn chain_index_for_depth(&self, d: usize) -> Option<usize> {
        let l = self.chain_len;
        if d < 2 || d > l + 1 {
            return None;
        }
        Some(l - (d - 1))
    }

    /// Exact 128-bit wildcard-d key: everything except the depth-d node, plus
    /// (tip, length, differing-position idx). Two lanes folded into u128.
    ///
    /// BASE mode (adjusted=false): unchanged from the original probe — the
    /// entire node at idx (symbol_id, pointer, gen_idx) is wildcarded away,
    /// and condition is not consulted at all.
    ///
    /// ADJUSTED mode (adjusted=true): only (symbol_id, pointer) at idx are
    /// wildcarded; gen_idx and observing_cond_symbol_ids AT idx are folded
    /// back in via inv_pre[idx]/inv_suf[idx+1] (mgroup4_phase_a_design.md
    /// §H5(b): masked node's gen/observing must still match). The shape's
    /// AcceptCondition (cond_hash) is folded in at the whole-shape level
    /// (§H5(a): members only merge when their condition matches).
    fn wildcard_key(&self, d: usize, adjusted: bool) -> Option<u128> {
        let idx = self.chain_index_for_depth(d)?;
        if !adjusted {
            // prefix [0..idx) + suffix (idx+1..L)  (the node at idx is the wildcard)
            let (pa, pb) = self.pre[idx];
            let (sa, sb) = self.suf[idx + 1];
            let lane0 = combine(combine(combine(pa, sa), self.tip as u64), self.chain_len as u64);
            let lane1 =
                combine(combine(combine(pb, sb), (self.tip as u64) ^ 0x9e3779b9), idx as u64);
            return Some(((lane0 as u128) << 64) | (lane1 as u128));
        }
        // adjusted: template-only prefix/suffix around idx (wildcards
        // symbol_id/pointer at idx), PLUS the invariant (gen,observing)
        // prefix/suffix INCLUDING idx (idx's gen/observing must match), PLUS
        // the shape-level condition hash.
        let (tpa, tpb) = self.tmpl_pre[idx];
        let (tsa, tsb) = self.tmpl_suf[idx + 1];
        let (ipa, ipb) = self.inv_pre[idx + 1]; // includes node idx's (gen,observing)
        let (isa, isb) = self.inv_suf[idx]; // includes node idx's (gen,observing)
        let (ca, cb) = self.cond_hash;
        let lane0 = combine(
            combine(combine(combine(combine(tpa, tsa), ipa), isa), ca),
            combine(self.tip as u64, self.chain_len as u64),
        );
        let lane1 = combine(
            combine(combine(combine(combine(tpb, tsb), ipb), isb), cb),
            combine((self.tip as u64) ^ 0x9e3779b9, idx as u64),
        );
        Some(((lane0 as u128) << 64) | (lane1 as u128))
    }

    /// The "root-side" (out-of-window, depths > d) exact key for P3 matching:
    /// root identity (via pre[0]) + the chain nodes [0..idx) (deeper than depth
    /// d toward the root). Deliberately EXCLUDES the tip group, chain length,
    /// and idx: on an expand step the tip group id changes and L grows while
    /// idx stays constant (idx = L-(d-1); L+1,d+1 -> same idx), so pre[idx]
    /// alone is the invariant across grow (exit), persist, and tip-ward shrink.
    /// Including the tip broke the exit match on precisely the expand
    /// transitions we need to observe (systematic exit under-count — fixed).
    /// P3 is not run in adjusted mode, so this stays base-mode-only (uses the
    /// full-node `pre`, unchanged).
    fn root_side_key(&self, d: usize) -> Option<u128> {
        let idx = self.chain_index_for_depth(d)?;
        let (pa, pb) = self.pre[idx];
        Some(((pa as u128) << 64) | (pb as u128))
    }
}

/// Per-gen aggregation for one flavor bucket (a list of shape sigs).
/// Runs the greedy P1 merge and returns, for each n in `ns`, the merged shape
/// count; also accumulates the P2 depth histogram (via `depth_collapse`) at
/// n=inf granularity (every depth), and returns the confirmed groups at n=inf
/// for P3.
struct MergeResult {
    base: usize,
    /// merged count per requested n (same order as `ns`)
    merged: Vec<usize>,
    /// depth -> collapses (group_size - 1) summed, at n=inf (all depths)
    depth_collapse: HashMap<usize, usize>,
}

/// Ascending list of n values to evaluate. `usize::MAX` == infinity.
/// `adjusted`: see `ShapeSig::wildcard_key` — narrows the merge key to what a
/// real runtime merge could do (condition equality, observing/gen invariant
/// at the masked position). Base mode (adjusted=false) is byte-identical to
/// the original probe.
fn merge_greedy(sigs: &[ShapeSig], ns: &[usize], adjusted: bool) -> (MergeResult, Vec<MergeGroup>) {
    let base = sigs.len();
    // max depth across all shapes (for n=inf).
    let global_max_depth = sigs.iter().map(|s| s.max_depth()).max().unwrap_or(1);

    // For merged counts at each n we run the greedy consuming pass ONCE up to
    // the largest finite depth needed (== max n that's finite, or global_max
    // for inf), and record, per depth d, how many collapses happen. Because the
    // greedy consumes in ascending d, the collapses at depth d are independent
    // of whether we stop at n=d or continue — so merged(n) = base - sum_{d<=n}
    // collapses_at_d. That lets us derive every n from one pass.
    let max_d_needed = ns
        .iter()
        .map(|&n| if n == usize::MAX { global_max_depth } else { n.min(global_max_depth) })
        .max()
        .unwrap_or(1);

    // consumed[i] = true once shape i has been merged into a group.
    let mut consumed = vec![false; sigs.len()];
    let mut collapses_at_depth: Vec<usize> = vec![0; global_max_depth + 2];
    let mut depth_collapse: HashMap<usize, usize> = HashMap::new();
    let mut groups: Vec<MergeGroup> = Vec::new();

    for d in 2..=max_d_needed {
        // partition not-yet-consumed shapes by wildcard-d key (exact u128)
        let mut buckets: HashMap<u128, Vec<usize>> = HashMap::default();
        for (i, s) in sigs.iter().enumerate() {
            if consumed[i] {
                continue;
            }
            if let Some(k) = s.wildcard_key(d, adjusted) {
                buckets.entry(k).or_default().push(i);
            }
        }
        for (_key, members) in buckets {
            if members.len() >= 2 {
                let collapse = members.len() - 1;
                collapses_at_depth[d] += collapse;
                *depth_collapse.entry(d).or_default() += collapse;
                // the group's root-side (out-of-window) context for P3 matching:
                // any member's root_side_key(d) is identical (they differ only at
                // depth d, which is excluded from root_side). P3 is base-mode only.
                let root_side = sigs[members[0]].root_side_key(d).unwrap_or(0);
                for &i in &members {
                    consumed[i] = true;
                }
                groups.push(MergeGroup { depth: d, size: members.len(), root_side });
            }
        }
    }

    // Now derive merged(n) = base - sum_{d=2..=n} collapses_at_depth[d].
    let mut merged = Vec::with_capacity(ns.len());
    for &n in ns {
        let cap = if n == usize::MAX { global_max_depth } else { n.min(global_max_depth) };
        let mut removed = 0usize;
        for d in 2..=cap {
            removed += collapses_at_depth.get(d).copied().unwrap_or(0);
        }
        merged.push(base - removed);
    }

    (MergeResult { base, merged, depth_collapse }, groups)
}

/// A confirmed merge group (for P3 tracking).
#[derive(Clone)]
struct MergeGroup {
    depth: usize,
    #[allow(dead_code)]
    size: usize,
    /// exact root-side (out-of-window) context key; invariant as the window
    /// slides tipward. Used by P3 to match "the same group" one gen later.
    root_side: u128,
}

fn is_main(root: &PathRoot, main_root: &PathRoot) -> bool {
    root.symbol_id == main_root.symbol_id && root.start_gen == main_root.start_gen
}

/// Collect per-flavor shape sigs at the current ctx.
/// Returns (main_sigs, watcher_sigs, all_sigs). `cond` is threaded through to
/// `ShapeSig::of` for adjusted-mode's condition-equality key; base mode
/// ignores it, so passing it unconditionally doesn't affect base output.
fn collect_sigs(
    ctx: &ParsingCtx,
) -> (Vec<ShapeSig>, Vec<ShapeSig>, Vec<ShapeSig>) {
    let main_root = ctx.main_root;
    let mut main_sigs = Vec::new();
    let mut watcher_sigs = Vec::new();
    let mut all_sigs = Vec::new();
    for (root, pm) in ctx.paths.iter() {
        let mainp = is_main(root, &main_root);
        for (shape, cond) in pm.iter() {
            let sig = ShapeSig::of(shape, root, cond);
            if mainp {
                main_sigs.push(sig.clone());
            } else {
                watcher_sigs.push(sig.clone());
            }
            all_sigs.push(sig);
        }
    }
    (main_sigs, watcher_sigs, all_sigs)
}

/// Accumulator over the whole parse.
#[derive(Default)]
struct Accum {
    // integrals of merged counts, per n index
    sum_merged: Vec<u64>,
    // peaks of merged counts, per n index
    peak_merged: Vec<usize>,
    sum_base: u64,
    peak_base: usize,
    gens: u64,
    // P2 depth histogram (collapses summed over gens)
    depth_hist: HashMap<usize, u64>,
    total_collapse_inf: u64,
}

impl Accum {
    fn new(nn: usize) -> Self {
        Accum {
            sum_merged: vec![0; nn],
            peak_merged: vec![0; nn],
            sum_base: 0,
            peak_base: 0,
            gens: 0,
            depth_hist: HashMap::new(),
            total_collapse_inf: 0,
        }
    }
    fn add(&mut self, r: &MergeResult) {
        self.sum_base += r.base as u64;
        self.peak_base = self.peak_base.max(r.base);
        for (i, &m) in r.merged.iter().enumerate() {
            self.sum_merged[i] += m as u64;
            self.peak_merged[i] = self.peak_merged[i].max(m);
        }
        for (&d, &c) in r.depth_collapse.iter() {
            *self.depth_hist.entry(d).or_default() += c as u64;
            self.total_collapse_inf += c as u64;
        }
        self.gens += 1;
    }
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // usage: interior_merge <parserdata.pb> <input.txt> [--adjusted]
    // `--adjusted` may appear as the 3rd positional arg or anywhere after the
    // two required paths (both accepted; task brief said "third arg or
    // --adjusted flag").
    let raw_args: Vec<String> = std::env::args().skip(1).collect();
    let adjusted = raw_args.iter().any(|a| a == "--adjusted" || a == "adjusted");
    let mut positional = raw_args.iter().filter(|a| a.as_str() != "--adjusted" && a.as_str() != "adjusted");
    let data_path = positional
        .next()
        .expect("usage: interior_merge <parserdata.pb> <input.txt> [--adjusted]")
        .clone();
    let input_path = positional
        .next()
        .expect("usage: interior_merge <parserdata.pb> <input.txt> [--adjusted]")
        .clone();

    let data = Mgroup3ParserData::decode(std::fs::read(&data_path)?.as_slice())?;
    let parser = Mgroup3Parser::new(data);
    let input = std::fs::read_to_string(&input_path)?;
    let chars: Vec<char> = input.chars().collect();
    let total = chars.len();

    // n values: base mode {1,2,3,4,inf} (unchanged — byte-identical to the
    // original probe). Adjusted mode extends to {1,2,3,4,6,8,inf} per the
    // task brief (the extra n=6/8 points matter for the es5-heavy tail, see
    // mgroup4_bounded_interior_groups.md §6.7).
    let (ns, n_labels): (Vec<usize>, Vec<&str>) = if adjusted {
        (vec![1, 2, 3, 4, 6, 8, usize::MAX], vec!["1", "2", "3", "4", "6", "8", "inf"])
    } else {
        (vec![1, 2, 3, 4, usize::MAX], vec!["1", "2", "3", "4", "inf"])
    };

    let mut acc_main = Accum::new(ns.len());
    let mut acc_watch = Accum::new(ns.len());
    let mut acc_all = Accum::new(ns.len());

    // P3 state (see file header for the heuristic): previous gen's groups
    // indexed as root_side R -> {depths}. Per-gen classification of each
    // prev (d, R): (d+1) in cur[R] = EXIT; (d) or (d-1) in cur[R] = PERSIST
    // (not an event, excluded); else POP. NOT run in adjusted mode (task
    // brief: "P3 로직은 adjusted 모드에서 실행/보고 불필요") — all maps stay
    // empty and the P3 sections below print zeros/skip.
    let mut p3_exit: HashMap<usize, u64> = HashMap::new(); // boundary depth -> exits
    let mut p3_pop: HashMap<usize, u64> = HashMap::new(); // depth -> pops
    let mut p3_persist: HashMap<usize, u64> = HashMap::new(); // depth -> persists (excluded from rate)
    // exits where cur[R] ALSO contained (d) or (d-1): the group may in fact have
    // persisted while a DIFFERENT fork with the same root-side sat at d+1. These
    // are the acknowledged false-positive candidates; reported separately so the
    // exit-rate can be bracketed: high = e/(e+p), low = (e-ambig)/((e-ambig)+p).
    let mut p3_exit_ambig: HashMap<usize, u64> = HashMap::new();
    // previous gen's groups (all bucket): root_side -> set of depths
    let mut prev_groups: HashMap<u128, HashSet<usize>> = HashMap::new();

    let mut ctx = parser.init_ctx();
    for (idx, c) in chars.iter().enumerate() {
        ctx = match parser.parse_step(ctx, *c, idx + 1 == total) {
            Ok(ctx) => ctx,
            Err(e) => {
                eprintln!("PARSE ERROR at {}: {:?}", idx, e);
                return Ok(());
            }
        };
        let (main_sigs, watch_sigs, all_sigs) = collect_sigs(&ctx);

        let (rm, _gm) = merge_greedy(&main_sigs, &ns, adjusted);
        acc_main.add(&rm);
        let (rw, _gw) = merge_greedy(&watch_sigs, &ns, adjusted);
        acc_watch.add(&rw);
        let (ra, ga) = merge_greedy(&all_sigs, &ns, adjusted);
        acc_all.add(&ra);

        if !adjusted {
            // ---- P3 per-gen transition estimate (all bucket; heuristic in header) ----
            // Index THIS gen's groups by root_side R -> {depths}. For each PREVIOUS
            // gen group (d, R):
            //   (d+1) in cur[R]           -> EXIT   (differing node slid deeper)
            //   (d) or (d-1) in cur[R]    -> PERSIST (same/shallower depth; not an
            //                                event — counting it as pop would dilute
            //                                the exit-rate by the residence time)
            //   otherwise                 -> POP    (group dissolved)
            // Note groups have depth >= 2, so the (d-1) probe is inert for d == 2.
            let mut cur_groups: HashMap<u128, HashSet<usize>> = HashMap::new();
            for g in &ga {
                cur_groups.entry(g.root_side).or_default().insert(g.depth);
            }
            for (r, depths) in &prev_groups {
                let cur = cur_groups.get(r);
                for &d in depths {
                    match cur {
                        Some(ds) if ds.contains(&(d + 1)) => {
                            *p3_exit.entry(d).or_default() += 1;
                            // ambiguous: also matched a persist depth — the "exit"
                            // may be a different fork of the same root-side context.
                            if ds.contains(&d) || ds.contains(&(d.saturating_sub(1))) {
                                *p3_exit_ambig.entry(d).or_default() += 1;
                            }
                        }
                        Some(ds) if ds.contains(&d) || ds.contains(&(d.saturating_sub(1))) => {
                            *p3_persist.entry(d).or_default() += 1;
                        }
                        _ => {
                            *p3_pop.entry(d).or_default() += 1;
                        }
                    }
                }
            }
            prev_groups = cur_groups;
        }

        let _ = idx;
    }

    // ---- Emit human-readable + JSON ----
    let emit = |name: &str, acc: &Accum| {
        println!("\n=== {} ===", name);
        println!(
            "  base: peak={} mean={:.1}",
            acc.peak_base,
            acc.sum_base as f64 / acc.gens.max(1) as f64
        );
        println!("  n  | peak | mean | peakReduc(n=1/n) | meanReduc");
        let base_peak = acc.peak_merged[0]; // n=1 == base
        let base_mean = acc.sum_merged[0] as f64 / acc.gens.max(1) as f64;
        for (i, lbl) in n_labels.iter().enumerate() {
            let peak = acc.peak_merged[i];
            let mean = acc.sum_merged[i] as f64 / acc.gens.max(1) as f64;
            let pr = base_peak as f64 / peak.max(1) as f64;
            let mr = base_mean / mean.max(1.0);
            println!(
                "  {:>3}| {:>5}| {:>6.1}| {:>16.3}| {:.3}",
                lbl, peak, mean, pr, mr
            );
        }
    };
    println!("input: {} chars", total);
    emit("MAIN", &acc_main);
    emit("WATCHER", &acc_watch);
    emit("ALL", &acc_all);

    // P2 depth histogram (from ALL bucket, gen-weighted collapses at n=inf)
    println!("\n=== P2 fork-depth histogram (ALL bucket, gen-weighted collapses at n=inf) ===");
    let mut depths: Vec<usize> = acc_all.depth_hist.keys().copied().collect();
    depths.sort();
    let tot = acc_all.total_collapse_inf.max(1);
    let mut cum = 0u64;
    for d in &depths {
        let c = acc_all.depth_hist[d];
        cum += c;
        println!(
            "  depth {:>3}: collapses={:>10} ({:>5.1}%)  cum {:>5.1}%",
            d,
            c,
            100.0 * c as f64 / tot as f64,
            100.0 * cum as f64 / tot as f64
        );
    }
    // coverage n<=2/3/4
    let cover = |k: usize| -> f64 {
        let s: u64 = acc_all.depth_hist.iter().filter(|&(&d, _)| d <= k).map(|(_, &c)| c).sum();
        100.0 * s as f64 / tot as f64
    };
    println!("  coverage by max depth (fraction of all n=inf collapses):");
    println!("    n<=2 (depth<=2): {:.1}%", cover(2));
    println!("    n<=3 (depth<=3): {:.1}%", cover(3));
    println!("    n<=4 (depth<=4): {:.1}%", cover(4));

    // P3 window-exit vs pop — SKIPPED in adjusted mode (task brief: P1/P2 only).
    if adjusted {
        println!("\n=== P3 window-exit vs pop: SKIPPED in adjusted mode (P1/P2 only) ===");
    } else {
        println!("\n=== P3 window-exit vs pop (ALL bucket, per-gen transition estimate) ===");
        println!("  heuristic (see file header): prev group (d,R) vs cur groups R->{{depths}}:");
        println!("  (d+1) in cur[R] = EXIT; (d)/(d-1) in cur[R] = PERSIST (excluded);");
        println!("  else POP. exit-rate = exits/(exits+pops). Approximate: per-gen, and");
        println!("  a different fork of the same R at d+1 can false-positive an exit.");
        let mut p3_depths: HashSet<usize> = HashSet::new();
        p3_depths.extend(p3_exit.keys());
        p3_depths.extend(p3_pop.keys());
        p3_depths.extend(p3_persist.keys());
        let mut p3d: Vec<usize> = p3_depths.into_iter().collect();
        p3d.sort();
        let mut tot_exit = 0u64;
        let mut tot_pop = 0u64;
        let mut tot_persist = 0u64;
        println!("  depth | exits (ambig) | pops | persists | rate-high | rate-low");
        println!("  (ambig = cur[R] also had a persist depth: possibly a different fork");
        println!("   at d+1, not a true exit. high = e/(e+p); low = (e-ambig)/((e-ambig)+p))");
        for d in &p3d {
            let e = p3_exit.get(d).copied().unwrap_or(0);
            let a = p3_exit_ambig.get(d).copied().unwrap_or(0);
            let p = p3_pop.get(d).copied().unwrap_or(0);
            let ps = p3_persist.get(d).copied().unwrap_or(0);
            tot_exit += e;
            tot_pop += p;
            tot_persist += ps;
            let hi = 100.0 * e as f64 / (e + p).max(1) as f64;
            let lo = 100.0 * (e - a) as f64 / ((e - a) + p).max(1) as f64;
            println!(
                "  {:>5} | {:>6} ({:>6}) | {:>4} | {:>8} | {:>8.1}% | {:.1}%",
                d, e, a, p, ps, hi, lo
            );
        }
        println!(
            "  aggregate: exits={} pops={} persists={} overall rate-high={:.1}%",
            tot_exit,
            tot_pop,
            tot_persist,
            100.0 * tot_exit as f64 / (tot_exit + tot_pop).max(1) as f64
        );
        for &n in &[2usize, 3, 4] {
            // window-exit at boundary n = a group whose differing node sits at depth
            // n this gen and moves to depth n+1 next gen (exits the size-n window).
            let e = p3_exit.get(&n).copied().unwrap_or(0);
            let a = p3_exit_ambig.get(&n).copied().unwrap_or(0);
            let p = p3_pop.get(&n).copied().unwrap_or(0);
            let ps = p3_persist.get(&n).copied().unwrap_or(0);
            let hi = 100.0 * e as f64 / (e + p).max(1) as f64;
            let lo = 100.0 * (e - a) as f64 / ((e - a) + p).max(1) as f64;
            println!(
                "  boundary n={}: window-exit rate = {:.1}% high / {:.1}% low (exits {} ambig {} / pops {} / persists {})",
                n, hi, lo, e, a, p, ps
            );
        }
    }

    // ---- machine-readable JSON on stderr (so stdout stays human) ----
    let json = build_json(
        total, &n_labels, &acc_main, &acc_watch, &acc_all, &p3_exit, &p3_pop, &p3_persist,
        &p3_exit_ambig,
    );
    eprintln!("JSON_BEGIN{}JSON_END", json);

    Ok(())
}

#[allow(clippy::too_many_arguments)]
fn build_json(
    total: usize,
    n_labels: &[&str],
    m: &Accum,
    w: &Accum,
    a: &Accum,
    p3_exit: &HashMap<usize, u64>,
    p3_pop: &HashMap<usize, u64>,
    p3_persist: &HashMap<usize, u64>,
    p3_exit_ambig: &HashMap<usize, u64>,
) -> String {
    let acc_json = |acc: &Accum| -> String {
        let gens = acc.gens.max(1) as f64;
        let mut per_n = Vec::new();
        for (i, lbl) in n_labels.iter().enumerate() {
            per_n.push(format!(
                "{{\"n\":\"{}\",\"peak\":{},\"mean\":{:.3}}}",
                lbl,
                acc.peak_merged[i],
                acc.sum_merged[i] as f64 / gens
            ));
        }
        let mut dh: Vec<usize> = acc.depth_hist.keys().copied().collect();
        dh.sort();
        let depth_json: Vec<String> = dh
            .iter()
            .map(|d| format!("{{\"depth\":{},\"collapses\":{}}}", d, acc.depth_hist[d]))
            .collect();
        format!(
            "{{\"base_peak\":{},\"base_mean\":{:.3},\"per_n\":[{}],\"depth_hist\":[{}],\"total_collapse_inf\":{}}}",
            acc.peak_base,
            acc.sum_base as f64 / gens,
            per_n.join(","),
            depth_json.join(","),
            acc.total_collapse_inf
        )
    };
    let mut p3d: Vec<usize> = HashSet::<usize>::from_iter(
        p3_exit
            .keys()
            .copied()
            .chain(p3_pop.keys().copied())
            .chain(p3_persist.keys().copied()),
    )
    .into_iter()
    .collect();
    p3d.sort();
    let p3_json: Vec<String> = p3d
        .iter()
        .map(|d| {
            format!(
                "{{\"depth\":{},\"exits\":{},\"pops\":{},\"persists\":{},\"exit_ambig\":{}}}",
                d,
                p3_exit.get(d).copied().unwrap_or(0),
                p3_pop.get(d).copied().unwrap_or(0),
                p3_persist.get(d).copied().unwrap_or(0),
                p3_exit_ambig.get(d).copied().unwrap_or(0)
            )
        })
        .collect();
    format!(
        "{{\"chars\":{},\"main\":{},\"watcher\":{},\"all\":{},\"p3\":[{}]}}",
        total,
        acc_json(m),
        acc_json(w),
        acc_json(a),
        p3_json.join(",")
    )
}
