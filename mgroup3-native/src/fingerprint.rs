//! Shift-invariant fingerprint of a live parsing state (`ParsingCtx`).
//! ALGORITHM-INVARIANT — read-only over `ctx.paths`; the parser core is untouched.
//!
//! This is the *executable definition* of the convergence fingerprint used by
//! the incremental-parsing track. It was first written inside
//! `src/bin/incremental_probe.rs` (which keeps its own private copy so the probe
//! binary stays byte-for-byte as documented); this module lifts the same
//! encoding into the library so `session.rs` can reuse it. The two copies are
//! kept intentionally identical: the session's per-gen fingerprints must equal
//! the probe's for the probe's measured convergence band (`incremental_parsing.md`)
//! to be the session's expected band.
//!
//! ============================================================
//! GEN ANCHOR SCHEME (edit-position aware)
//! ============================================================
//! For an edit at position `p` (a gen boundary — `p == p_char`, the char index
//! where the change starts), every gen value `g` appearing anywhere in a state
//! is encoded:
//!   g <= p  ->  A-anchor: absolute       (the prefix before the edit is
//!                                          identical in both parses, so its gens
//!                                          match on value)
//!   g >  p  ->  B-anchor: cur_gen - g     (offset from the current gen —
//!                                          invariant under a uniform shift of the
//!                                          whole post-edit suffix)
//! With this encoding, "the gen-q state of parse B" and "the gen-(q-delta) state
//! of parse A" produce IDENTICAL fingerprints exactly when they are shift-
//! equivalent. `cur` is the gen of the state being fingerprinted.
//!
//! WHAT THE FINGERPRINT CONTAINS:
//!  - the SET of roots (main + every watcher): (symbol_id, gen-encoded start_gen).
//!  - per root, its PathMap as a SORTED MULTISET of (shape, condition) entries:
//!      * shape = milestone chain (each node: symbol_id, pointer, gen-encoded
//!        gen_idx, sorted observing_cond_symbol_ids) + tip_group_id.
//!      * condition = full AcceptCondition (leaf kind + symbol_id + gen-encoded
//!        gens; And/Or as a SORTED order-free multiset).
//!  Two variants:
//!   - SEMANTIC: everything above, EXCLUDING the report shadow.
//!   - STRICT:   SEMANTIC + the report shadow (report_gen, milestone_report_gen),
//!               gen-encoded. History-splice reuse (Phase I2) needs strict; the
//!               session's convergence counters (I1) measure strict.
//!
//! The fingerprint is a canonical 128-bit dual hash (two independent FxHash lanes
//! over an order-canonical byte stream; every multiset sorted before folding).

use std::rc::Rc;

use crate::accept_condition::AcceptCondition;
use crate::parsing_ctx::{MilestonePath, ParsingCtx, PathMap, PathShape};

// ---------------------------------------------------------------------------
// Hashing primitives (two independent lanes -> u128 canonical fingerprint)
// ---------------------------------------------------------------------------

#[inline]
fn fx(seed: u64, bytes: &[u64]) -> u64 {
    use rustc_hash::FxHasher;
    use std::hash::{Hash, Hasher};
    let mut h = FxHasher::default();
    seed.hash(&mut h);
    for b in bytes {
        b.hash(&mut h);
    }
    h.finish()
}

#[inline]
fn combine(acc: u64, node: u64) -> u64 {
    // order-sensitive polynomial fold (matches interior_merge.rs / the probe)
    acc.wrapping_mul(0x100000001b3).wrapping_add(node ^ (node >> 29))
}

/// A canonical 128-bit fingerprint value, two lanes.
pub type Fp = u128;

#[inline]
fn fp2(a: u64, b: u64) -> Fp {
    ((a as u128) << 64) | (b as u128)
}

#[inline]
fn lanes(fp: Fp) -> (u64, u64) {
    ((fp >> 64) as u64, fp as u64)
}

#[inline]
fn fold_child(acc: (u64, u64), child: Fp) -> (u64, u64) {
    let (ca, cb) = lanes(child);
    (combine(acc.0, ca), combine(acc.1, cb))
}

/// Combine an unordered multiset of child fingerprints into one: sort (canonical),
/// then order-sensitively fold. Order-free result.
fn fp_multiset(tag: u64, mut children: Vec<Fp>) -> Fp {
    children.sort_unstable();
    let mut acc = (
        fx(0xA11 ^ tag, &[children.len() as u64]),
        fx(0xB22 ^ tag, &[children.len() as u64]),
    );
    for c in children {
        acc = fold_child(acc, c);
    }
    fp2(acc.0, acc.1)
}

// ---------------------------------------------------------------------------
// Gen anchor encoding
// ---------------------------------------------------------------------------

/// Encode a gen value under the (p, cur) anchor scheme into a u64 that is
/// shift-invariant on the post-edit suffix.
#[inline]
fn enc_gen(g: i32, p: i32, cur: i32) -> u64 {
    if g <= p {
        (0x4000_0000u64) ^ ((g as i64 as u64) << 1)
    } else {
        let rel = (cur - g) as i64;
        (0x8000_0000_0000_0000u64) ^ ((rel as u64) << 1)
    }
}

// ---------------------------------------------------------------------------
// Fingerprinting a ParsingCtx state
// ---------------------------------------------------------------------------

/// Which report-shadow gens to include: SEMANTIC excludes them, STRICT includes.
#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub enum Variant {
    Semantic,
    Strict,
}

fn fp_cond(cond: &AcceptCondition, p: i32, cur: i32) -> Fp {
    use AcceptCondition::*;
    match cond {
        Always => fp2(fx(0xC0, &[1]), fx(0xC1, &[1])),
        Never => fp2(fx(0xC0, &[2]), fx(0xC1, &[2])),
        NoLongerMatch { symbol_id, start_gen, min_end_gen } => {
            let v = [3, *symbol_id as u64, enc_gen(*start_gen, p, cur), enc_gen(*min_end_gen, p, cur)];
            fp2(fx(0xC0, &v), fx(0xC1, &v))
        }
        NeedLongerMatch { symbol_id, start_gen, min_end_gen } => {
            let v = [4, *symbol_id as u64, enc_gen(*start_gen, p, cur), enc_gen(*min_end_gen, p, cur)];
            fp2(fx(0xC0, &v), fx(0xC1, &v))
        }
        NotExists { symbol_id, start_gen } => {
            let v = [5, *symbol_id as u64, enc_gen(*start_gen, p, cur)];
            fp2(fx(0xC0, &v), fx(0xC1, &v))
        }
        Exists { symbol_id, start_gen } => {
            let v = [6, *symbol_id as u64, enc_gen(*start_gen, p, cur)];
            fp2(fx(0xC0, &v), fx(0xC1, &v))
        }
        Unless { symbol_id, start_gen, end_gen } => {
            let v = [7, *symbol_id as u64, enc_gen(*start_gen, p, cur), enc_gen(*end_gen, p, cur)];
            fp2(fx(0xC0, &v), fx(0xC1, &v))
        }
        OnlyIf { symbol_id, start_gen, end_gen } => {
            let v = [8, *symbol_id as u64, enc_gen(*start_gen, p, cur), enc_gen(*end_gen, p, cur)];
            fp2(fx(0xC0, &v), fx(0xC1, &v))
        }
        And { items } => {
            let children: Vec<Fp> = items.iter().map(|c| fp_cond(c, p, cur)).collect();
            fp_multiset(0xA0D1, children)
        }
        Or { items } => {
            let children: Vec<Fp> = items.iter().map(|c| fp_cond(c, p, cur)).collect();
            fp_multiset(0x0121, children)
        }
    }
}

fn fp_chain(path: &Option<Rc<MilestonePath>>, p: i32, cur: i32, variant: Variant) -> Fp {
    let mut acc = (fx(0xC1A0, &[0]), fx(0xC1A1, &[0]));
    let mut cur_node = path.clone();
    let mut len: u64 = 0;
    while let Some(node) = cur_node {
        len += 1;
        let mut obs: Vec<u64> = node.observing_cond_symbol_ids.iter().map(|x| *x as u64).collect();
        obs.sort_unstable();
        let mut fields: Vec<u64> = Vec::with_capacity(4 + obs.len());
        fields.push(node.milestone.symbol_id as u64);
        fields.push(node.milestone.pointer as u64);
        fields.push(enc_gen(node.gen_idx, p, cur));
        fields.push(obs.len() as u64);
        fields.extend_from_slice(&obs);
        if variant == Variant::Strict {
            fields.push(0xDEAD);
            fields.push(enc_gen(node.report_gen, p, cur));
            fields.push(enc_gen(node.milestone_report_gen, p, cur));
        }
        let na = fx(0x00E0, &fields);
        let nb = fx(0x00E1, &fields);
        acc = (combine(acc.0, na), combine(acc.1, nb));
        cur_node = node.parent.clone();
    }
    let la = combine(acc.0, fx(0x1E10, &[len]));
    let lb = combine(acc.1, fx(0x1E11, &[len]));
    fp2(la, lb)
}

fn fp_shape(shape: &PathShape, p: i32, cur: i32, variant: Variant) -> Fp {
    let chain = fp_chain(&shape.milestone_path, p, cur, variant);
    let (ca, cb) = lanes(chain);
    let a = combine(ca, fx(0x71B0, &[shape.tip_group_id as u64]));
    let b = combine(cb, fx(0x71B1, &[shape.tip_group_id as u64]));
    fp2(a, b)
}

fn fp_pathmap(pm: &PathMap, p: i32, cur: i32, variant: Variant) -> Fp {
    let mut entries: Vec<Fp> = Vec::with_capacity(pm.len());
    for (shape, cond) in pm.iter() {
        let s = fp_shape(shape, p, cur, variant);
        let c = fp_cond(cond, p, cur);
        let (sa, sb) = lanes(s);
        let (ca, cb) = lanes(c);
        entries.push(fp2(combine(sa, ca), combine(sb, cb)));
    }
    fp_multiset(0x00B4, entries)
}

/// Fingerprint a whole `ParsingCtx` live state under the (p, cur) anchor scheme.
/// `p` is the edit position (char index / gen boundary); for a full parse with no
/// edit, pass `p = i32::MAX` so every gen is prefix-anchored (absolute).
/// `cur` is the gen of `ctx` (== `ctx.gen_idx`).
pub fn fp_state(ctx: &ParsingCtx, p: i32, cur: i32, variant: Variant) -> Fp {
    let mut roots: Vec<Fp> = Vec::with_capacity(ctx.paths.len());
    for (root, pm) in ctx.paths.iter() {
        let rid = [root.symbol_id as u64, enc_gen(root.start_gen, p, cur)];
        let ra = fx(0x87A0, &rid);
        let rb = fx(0x87A1, &rid);
        let pmfp = fp_pathmap(pm, p, cur, variant);
        let (pa, pb) = lanes(pmfp);
        roots.push(fp2(combine(ra, pa), combine(rb, pb)));
    }
    fp_multiset(0x005E, roots)
}
