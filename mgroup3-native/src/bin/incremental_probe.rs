//! Incremental-parsing convergence-distance probe. ALGORITHM-INVARIANT.
//! Spec: incremental-parsing track / Phase II (suffix reuse) feasibility.
//!
//! Read-only: it drives the UNMODIFIED parser (`parse_step`) twice per edit
//! (the original text A and the edited text B), canonicalizes the live parsing
//! state at each gen into a shift-invariant fingerprint, and measures how many
//! chars after the edit end the two parses re-align. The parser algorithm and
//! its output are untouched — this only reads `ctx.paths` and hashes it.
//!
//! ============================================================
//! THE QUESTION
//! ============================================================
//! mulang's LSP re-parses a bibix4/mulang file on every keystroke. Phase I =
//! prefix checkpoint resume (trivially sound: the parse state at any gen g <=
//! edit-start is byte-identical between the pre-edit and post-edit texts, since
//! the prefix chars are the same). Phase II = SUFFIX reuse: once the edited
//! parse B, after the edit, re-aligns with the original parse A modulo a uniform
//! gen shift (delta = new_len - old_len), everything from that gen onward — the
//! live state AND the recorded history — can be spliced from A instead of
//! recomputed. This probe measures the CONVERGENCE DISTANCE: how many chars past
//! the edit end before B's state == A's state (shift-adjusted).
//!
//! ============================================================
//! FINGERPRINT — GEN ANCHOR SCHEME (exactly the spec's scheme)
//! ============================================================
//! For an edit at position p (old length lo, new length ln, delta = ln - lo),
//! every gen value g appearing anywhere in a state is encoded:
//!   g <= p  ->  ('A', g)                  (prefix anchor — absolute; the region
//!                                           before the edit is identical in both
//!                                           parses, so its gens match on value)
//!   g >  p  ->  ('B', cur_gen - g)        (post-edit anchor — offset from the
//!                                           current gen; invariant under a
//!                                           uniform shift of the whole suffix)
//! With this encoding, "the gen-q state of parse B" and "the gen-(q-delta) state
//! of parse A" produce IDENTICAL fingerprints exactly when they are shift-
//! equivalent. cur_gen is the gen of the state being fingerprinted (for A that
//! is q-delta, for B that is q), so a B-anchored gen g in B maps to cur_gen_B-g
//! and the corresponding gen g-delta in A maps to cur_gen_A-(g-delta) =
//! (q-delta)-(g-delta) = q-g = cur_gen_B-g. Equal. QED.
//!
//! p is expressed in GEN units. gen_idx increments once per char (parse_step),
//! and after consuming the char at 0-based index i, ctx.gen_idx == i+1. An edit
//! that replaces chars [p_char, p_char+lo) inserts its new text starting at char
//! index p_char; the last prefix char shared by both texts is at index p_char-1,
//! i.e. gen p_char. So the fingerprint's p := p_char (a gen boundary): gens
//! <= p_char are prefix-anchored, gens > p_char are suffix-anchored.
//!
//! WHAT THE FINGERPRINT CONTAINS (the whole live structure):
//!  - the SET of roots (main + every watcher). Each root is itself
//!    (symbol_id, gen-encoded start_gen). main_root.start_gen is always 0
//!    (init_ctx_with_start), so main encodes to ('A',0) in both parses.
//!  - per root, its PathMap as a SORTED MULTISET of (shape, condition) entries:
//!      * shape = the milestone chain (each node: symbol_id, pointer,
//!        gen-encoded gen_idx, sorted observing_cond_symbol_ids) + tip_group_id.
//!      * condition = the full AcceptCondition structure: leaf kind + symbol_id
//!        + gen-encoded start_gen/(end_gen|min_end_gen where present); And/Or as
//!        a SORTED multiset of child fingerprints (canonical, order-free).
//!  Two fingerprint variants are computed in one pass:
//!   - SEMANTIC: everything above, EXCLUDING the report shadow (report_gen,
//!     milestone_report_gen on each node). This is what Phase II needs for
//!     live-state reuse.
//!   - STRICT: SEMANTIC + the report shadow, gen-encoded and folded in. Phase II
//!     can only splice the recorded HISTORY (not just live state) where strict
//!     matches; the gap between semantic and strict convergence is itself a
//!     design input (how much extra Phase II must recompute to reuse history).
//!
//! No-collision: the fingerprint is a canonical 128-bit dual hash (two
//! independent FxHash lanes over an order-canonical byte stream — same technique
//! as interior_merge.rs / the Phase 0 probes). Sorting every multiset before
//! folding makes the stream order-free; two lanes make an accidental match
//! astronomically unlikely for a convergence measurement.
//!
//! ============================================================
//! SELF-VERIFICATION (mandatory — numbers from a failing run are invalid)
//! ============================================================
//!  - IDENTITY: parse the same text twice (delta=0, p arbitrary). Every gen's
//!    fingerprint (both variants) must match between the two runs. (Also a
//!    determinism check on the fingerprint itself.)
//!  - NO-OP EDIT: replace one char with the SAME char (delta=0). Convergence
//!    must be immediate — the first gen at/after the edit end must already match.
//! A failure here aborts with a loud error; no matrix numbers are reported.
//!
//! ============================================================
//! CONVERGENCE MEASUREMENT
//! ============================================================
//! Parse A (original) -> per-gen fingerprint sequence fA[q].
//! Parse B (edited)   -> per-gen fingerprint sequence fB[q].
//! edit_end_B = p_char + ln   (first gen of B fully past the inserted text).
//! CONVERGENCE gen q* = min q >= edit_end_B such that fB[q] == fA[q - delta]
//!   (and q-delta is a valid gen of A, i.e. 0 <= q-delta <= len(A)).
//! CONVERGENCE DISTANCE = q* - edit_end_B (chars past the edit end).
//! STABILITY: after q*, check the next M=STABILITY_WINDOW gens AND the final gen
//! all still match (shift-adjusted). Any later mismatch => RE-DIVERGENCE flag
//! (itself a finding). Reported per variant (semantic / strict).
//!
//! ============================================================
//! BLOCKER ATTRIBUTION
//! ============================================================
//! For the late-converging cases, at the FIRST mismatching gen q (>= edit_end_B)
//! we decompose the semantic diff of fB[q] vs fA[q-delta] into which component
//! differs: (i) the watcher root SET (per-symbol_id counts of roots present in
//! one but not the other), (ii) main chain, (iii) condition anchors, (iv) — for
//! strict only — the report shadow. block-spanning watchers (longest family) are
//! the suspected driver; the per-symbol_id root delta names them.
//!
//! ============================================================
//! CLI
//! ============================================================
//!   incremental_probe <parserdata.pb> [--json] [--edits-per-file N]
//!                     [--stability M] [--attrib-topk K] [--file PATH]...
//! If no --file is given, the built-in mulang corpus list is used (absolute
//! paths). Each file gets an 8-ish edit matrix (positions {25,50,75}% x
//! {insert, delete, ident-substitute} + special positions inside line comment /
//! block comment / string literal where detectable).

use std::collections::{BTreeMap, HashSet};

use mgroup3_native::accept_condition::AcceptCondition;
use mgroup3_native::parser::Mgroup3Parser;
use mgroup3_native::parsing_ctx::{MilestonePath, ParsingCtx, PathShape};
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;
use std::rc::Rc;

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
    // order-sensitive polynomial fold (matches interior_merge.rs)
    acc.wrapping_mul(0x100000001b3).wrapping_add(node ^ (node >> 29))
}

/// A canonical 128-bit fingerprint value, two lanes.
type Fp = u128;

#[inline]
fn fp2(a: u64, b: u64) -> Fp {
    ((a as u128) << 64) | (b as u128)
}

#[inline]
fn lanes(fp: Fp) -> (u64, u64) {
    ((fp >> 64) as u64, fp as u64)
}

/// Fold a child fingerprint into a running (a,b) dual accumulator, order-sensitive.
#[inline]
fn fold_child(acc: (u64, u64), child: Fp) -> (u64, u64) {
    let (ca, cb) = lanes(child);
    (combine(acc.0, ca), combine(acc.1, cb))
}

/// Combine an unordered multiset of child fingerprints into one fingerprint:
/// sort the u128s (canonical), then order-sensitively fold. Order-free result.
fn fp_multiset(tag: u64, mut children: Vec<Fp>) -> Fp {
    children.sort_unstable();
    let mut acc = (fx(0xA11 ^ tag, &[children.len() as u64]), fx(0xB22 ^ tag, &[children.len() as u64]));
    for c in children {
        acc = fold_child(acc, c);
    }
    fp2(acc.0, acc.1)
}

// ---------------------------------------------------------------------------
// Gen anchor encoding
// ---------------------------------------------------------------------------

/// Encode a gen value under the (p, cur_gen) anchor scheme into a u64 that is
/// shift-invariant on the post-edit suffix. Namespace A/B by the top bit so an
/// A-anchor value never collides with a B-anchor value.
#[inline]
fn enc_gen(g: i32, p: i32, cur_gen: i32) -> u64 {
    if g <= p {
        // A-anchor: absolute. Offset by a large base to separate namespaces and
        // keep negatives (rare, defensive) distinct.
        (0x4000_0000u64) ^ ((g as i64 as u64) << 1)
    } else {
        // B-anchor: relative to the current gen. cur_gen - g.
        let rel = (cur_gen - g) as i64;
        (0x8000_0000_0000_0000u64) ^ ((rel as u64) << 1)
    }
}

// ---------------------------------------------------------------------------
// Fingerprinting a ParsingCtx state
// ---------------------------------------------------------------------------

/// Which report-shadow gens to include: SEMANTIC excludes them, STRICT includes.
#[derive(Copy, Clone, PartialEq, Eq)]
enum Variant {
    Semantic,
    Strict,
}

/// Fingerprint one AcceptCondition, gen-encoded. And/Or children are an
/// order-free multiset.
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

/// Fingerprint one milestone chain (root..tip order is irrelevant to correctness
/// as long as it is deterministic; we fold tip->root, order-sensitively, so the
/// chain LINEARIZATION is preserved — chains are sequences, not multisets).
fn fp_chain(path: &Option<Rc<MilestonePath>>, p: i32, cur: i32, variant: Variant) -> Fp {
    let mut acc = (fx(0xC1A0, &[0]), fx(0xC1A1, &[0]));
    let mut cur_node = path.clone();
    let mut len: u64 = 0;
    while let Some(node) = cur_node {
        len += 1;
        // sorted observing ids (deterministic; they are a set)
        let mut obs: Vec<u64> = node.observing_cond_symbol_ids.iter().map(|x| *x as u64).collect();
        obs.sort_unstable();
        let mut fields: Vec<u64> = Vec::with_capacity(4 + obs.len());
        fields.push(node.milestone.symbol_id as u64);
        fields.push(node.milestone.pointer as u64);
        fields.push(enc_gen(node.gen_idx, p, cur));
        fields.push(obs.len() as u64);
        fields.extend_from_slice(&obs);
        if variant == Variant::Strict {
            // report shadow: report_gen + milestone_report_gen, gen-encoded.
            fields.push(0xDEAD);
            fields.push(enc_gen(node.report_gen, p, cur));
            fields.push(enc_gen(node.milestone_report_gen, p, cur));
        }
        let na = fx(0x00E0, &fields);
        let nb = fx(0x00E1, &fields);
        acc = (combine(acc.0, na), combine(acc.1, nb));
        cur_node = node.parent.clone();
    }
    // fold in the length so chains of different lengths never coincide
    let la = combine(acc.0, fx(0x1E10, &[len]));
    let lb = combine(acc.1, fx(0x1E11, &[len]));
    fp2(la, lb)
}

/// Fingerprint one PathShape (chain + tip group id).
fn fp_shape(shape: &PathShape, p: i32, cur: i32, variant: Variant) -> Fp {
    let chain = fp_chain(&shape.milestone_path, p, cur, variant);
    let (ca, cb) = lanes(chain);
    let a = combine(ca, fx(0x71B0, &[shape.tip_group_id as u64]));
    let b = combine(cb, fx(0x71B1, &[shape.tip_group_id as u64]));
    fp2(a, b)
}

/// Fingerprint a root's PathMap: multiset of (shape, condition) entry fingerprints.
fn fp_pathmap(
    pm: &mgroup3_native::parsing_ctx::PathMap,
    p: i32,
    cur: i32,
    variant: Variant,
) -> Fp {
    let mut entries: Vec<Fp> = Vec::with_capacity(pm.len());
    for (shape, cond) in pm.iter() {
        let s = fp_shape(shape, p, cur, variant);
        let c = fp_cond(cond, p, cur);
        // combine shape+cond into one entry fingerprint (order-sensitive within
        // the pair, but the pair itself is a set member)
        let (sa, sb) = lanes(s);
        let (ca, cb) = lanes(c);
        entries.push(fp2(combine(sa, ca), combine(sb, cb)));
    }
    fp_multiset(0x00B4, entries)
}

/// Fingerprint a whole ParsingCtx state: multiset over roots of
/// (root-identity, pathmap) fingerprints.
fn fp_state(ctx: &ParsingCtx, p: i32, cur: i32, variant: Variant) -> Fp {
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

// ---------------------------------------------------------------------------
// Driving a parse and collecting per-gen fingerprints
// ---------------------------------------------------------------------------

/// Per-gen fingerprints for one text, plus enough state to attribute blockers
/// at a chosen gen. `p` and cur (=gen at that step) are baked in per gen.
struct ParseTrace {
    /// gen 0 (init) .. gen N. semantic[q] / strict[q].
    semantic: Vec<Fp>,
    strict: Vec<Fp>,
    /// how far the parse got (== chars consumed OK). If < text len, parse errored.
    gens_ok: usize,
    parse_error: Option<String>,
}

/// Drive parse over `chars`, fingerprinting each gen with anchor position `p`.
/// Returns the trace. Does NOT keep per-gen ctx (memory) — attribution re-runs
/// to the target gen on demand.
fn parse_trace(parser: &Mgroup3Parser, chars: &[char], p: i32) -> ParseTrace {
    let total = chars.len();
    let mut ctx = parser.init_ctx();
    let mut semantic = Vec::with_capacity(total + 1);
    let mut strict = Vec::with_capacity(total + 1);
    // gen 0 (initial state, before any char)
    semantic.push(fp_state(&ctx, p, ctx.gen_idx, Variant::Semantic));
    strict.push(fp_state(&ctx, p, ctx.gen_idx, Variant::Strict));
    let mut gens_ok = 0usize;
    let mut parse_error = None;
    for (idx, c) in chars.iter().enumerate() {
        match parser.parse_step(ctx, *c, idx + 1 == total) {
            Ok(next) => {
                ctx = next;
                let cur = ctx.gen_idx;
                semantic.push(fp_state(&ctx, p, cur, Variant::Semantic));
                strict.push(fp_state(&ctx, p, cur, Variant::Strict));
                gens_ok = idx + 1;
            }
            Err(e) => {
                parse_error = Some(format!("{:?}", e).chars().take(80).collect());
                break;
            }
        }
    }
    ParseTrace { semantic, strict, gens_ok, parse_error }
}

/// Re-run parse to a target gen and return the ctx there (for attribution).
fn ctx_at_gen(parser: &Mgroup3Parser, chars: &[char], target_gen: usize) -> Option<ParsingCtx> {
    let total = chars.len();
    let mut ctx = parser.init_ctx();
    if target_gen == 0 {
        return Some(ctx);
    }
    for (idx, c) in chars.iter().enumerate() {
        match parser.parse_step(ctx, *c, idx + 1 == total) {
            Ok(next) => {
                ctx = next;
                if idx + 1 == target_gen {
                    return Some(ctx);
                }
            }
            Err(_) => return None,
        }
    }
    None
}

// ---------------------------------------------------------------------------
// Edit model
// ---------------------------------------------------------------------------

#[derive(Clone)]
struct Edit {
    label: String,
    /// 0-based char index where the change is applied (== fingerprint anchor p).
    p_char: usize,
    /// old length (chars replaced) and new length (chars inserted).
    lo: usize,
    ln: usize,
    /// the edited text
    new_chars: Vec<char>,
}

impl Edit {
    fn delta(&self) -> i32 {
        self.ln as i32 - self.lo as i32
    }
    /// first gen of B fully past the inserted text.
    fn edit_end_b(&self) -> usize {
        self.p_char + self.ln
    }
}

/// Build an edit that inserts `ins` chars at char index p (lo=0, ln=ins.len()).
fn mk_insert(orig: &[char], p: usize, ins: &[char], label: &str) -> Edit {
    let mut new_chars = Vec::with_capacity(orig.len() + ins.len());
    new_chars.extend_from_slice(&orig[..p]);
    new_chars.extend_from_slice(ins);
    new_chars.extend_from_slice(&orig[p..]);
    Edit { label: label.to_string(), p_char: p, lo: 0, ln: ins.len(), new_chars }
}

/// Build an edit that deletes 1 char at index p (lo=1, ln=0).
fn mk_delete(orig: &[char], p: usize, label: &str) -> Edit {
    let mut new_chars = Vec::with_capacity(orig.len().saturating_sub(1));
    new_chars.extend_from_slice(&orig[..p]);
    if p + 1 <= orig.len() {
        new_chars.extend_from_slice(&orig[p + 1..]);
    }
    Edit { label: label.to_string(), p_char: p, lo: 1, ln: 0, new_chars }
}

/// Build an edit that replaces `lo` chars at index p with `rep` (label given).
fn mk_replace(orig: &[char], p: usize, lo: usize, rep: &[char], label: &str) -> Edit {
    let mut new_chars = Vec::with_capacity(orig.len() - lo + rep.len());
    new_chars.extend_from_slice(&orig[..p]);
    new_chars.extend_from_slice(rep);
    new_chars.extend_from_slice(&orig[p + lo..]);
    Edit { label: label.to_string(), p_char: p, lo, ln: rep.len(), new_chars }
}

/// Is char c part of an identifier?
fn is_ident_char(c: char) -> bool {
    c.is_alphanumeric() || c == '_'
}

/// Find the identifier span covering or nearest at/after char index `from`.
/// Returns (start, end) exclusive, or None.
fn find_ident_span(chars: &[char], from: usize) -> Option<(usize, usize)> {
    let n = chars.len();
    let mut i = from.min(n.saturating_sub(1));
    // scan forward to an ident char
    while i < n && !is_ident_char(chars[i]) {
        i += 1;
    }
    if i >= n {
        return None;
    }
    // if we're mid-identifier, back up to its start
    let mut s = i;
    while s > 0 && is_ident_char(chars[s - 1]) {
        s -= 1;
    }
    let mut e = i;
    while e < n && is_ident_char(chars[e]) {
        e += 1;
    }
    // require an alphabetic lead (a real identifier, not a number)
    if chars[s].is_alphabetic() || chars[s] == '_' {
        Some((s, e))
    } else {
        None
    }
}

/// Locate the interior of the first line comment `//...` at/after char `from`.
/// Returns a char index safely inside the comment text (not the `//`, not the
/// newline), or None.
fn find_line_comment_interior(chars: &[char], from: usize) -> Option<usize> {
    let n = chars.len();
    let mut i = from;
    while i + 1 < n {
        if chars[i] == '/' && chars[i + 1] == '/' {
            // interior = a couple chars past the //, before end-of-line
            let mut j = i + 2;
            // skip to some content char that isn't newline
            while j < n && chars[j] != '\n' {
                if !chars[j].is_whitespace() {
                    return Some(j + 1); // insert right after a content char
                }
                j += 1;
            }
            if j > i + 2 {
                return Some(i + 2);
            }
        }
        i += 1;
    }
    None
}

/// Locate the interior of the first block comment `/* ... */` at/after `from`.
fn find_block_comment_interior(chars: &[char], from: usize) -> Option<usize> {
    let n = chars.len();
    let mut i = from;
    while i + 1 < n {
        if chars[i] == '/' && chars[i + 1] == '*' {
            // interior a couple chars in, ensure there is a closing */ later
            let mut j = i + 2;
            let mut found_close = false;
            while j + 1 < n {
                if chars[j] == '*' && chars[j + 1] == '/' {
                    found_close = true;
                    break;
                }
                j += 1;
            }
            if found_close {
                return Some(i + 2);
            }
        }
        i += 1;
    }
    None
}

/// Locate the interior of the first double-quoted string literal at/after `from`.
fn find_string_interior(chars: &[char], from: usize) -> Option<usize> {
    let n = chars.len();
    let mut i = from;
    while i < n {
        if chars[i] == '"' {
            // interior = just after the opening quote, if there's a closing quote
            let mut j = i + 1;
            while j < n {
                if chars[j] == '"' {
                    // require at least one interior char
                    if j > i + 1 {
                        return Some(i + 1);
                    }
                    break;
                }
                if chars[j] == '\n' {
                    break;
                }
                j += 1;
            }
        }
        i += 1;
    }
    None
}

/// Build the edit matrix for one text.
fn build_edits(orig: &[char], edits_per_file: usize) -> Vec<Edit> {
    let n = orig.len();
    let mut edits: Vec<Edit> = Vec::new();
    if n < 8 {
        return edits;
    }
    let positions = [
        (n / 4, "25%"),
        (n / 2, "50%"),
        (3 * n / 4, "75%"),
    ];
    for (pos, pct) in positions {
        // 1-char insert: insert a letter 'x'
        edits.push(mk_insert(orig, pos, &['x'], &format!("insert@{}", pct)));
        // 1-char delete
        edits.push(mk_delete(orig, pos, &format!("delete@{}", pct)));
        // identifier substitution: rename the nearest identifier to a fresh name
        if let Some((s, e)) = find_ident_span(orig, pos) {
            let old: String = orig[s..e].iter().collect();
            // fresh name: prepend 'q' and append '9' (very unlikely to collide;
            // still a valid identifier). Keep it different from old.
            let mut repl: Vec<char> = Vec::new();
            repl.push('q');
            repl.extend(old.chars());
            repl.push('9');
            edits.push(mk_replace(orig, s, e - s, &repl, &format!("ident-subst@{}", pct)));
        }
    }
    // special positions
    if let Some(ci) = find_line_comment_interior(orig, 0) {
        edits.push(mk_insert(orig, ci, &['Z'], "insert-in-line-comment"));
    }
    if let Some(bi) = find_block_comment_interior(orig, 0) {
        edits.push(mk_insert(orig, bi, &['Z'], "insert-in-block-comment"));
    }
    if let Some(si) = find_string_interior(orig, 0) {
        edits.push(mk_insert(orig, si, &['Z'], "insert-in-string-literal"));
    }
    // cap
    if edits_per_file > 0 && edits.len() > edits_per_file {
        edits.truncate(edits_per_file);
    }
    edits
}

// ---------------------------------------------------------------------------
// Convergence computation
// ---------------------------------------------------------------------------

struct ConvResult {
    /// convergence distance (q* - edit_end_b), or None if never converged.
    dist: Option<usize>,
    /// the convergence gen q* in B, if any.
    conv_gen: Option<usize>,
    /// re-divergence: converged then a later gen mismatched.
    rediverged: bool,
    /// first mismatching gen q>=edit_end_b (for attribution); == conv_gen means
    /// converged immediately with no prior mismatch (then this is None).
    first_mismatch_gen: Option<usize>,
}

/// Compute convergence for one variant given A's and B's per-gen fingerprints.
/// fA indexed by A's gen, fB by B's gen. delta = ln - lo.
fn compute_convergence(
    fa: &[Fp],
    fb: &[Fp],
    delta: i32,
    edit_end_b: usize,
    a_gens_ok: usize,
    b_gens_ok: usize,
    stability: usize,
) -> ConvResult {
    // valid B gens: 0..=b_gens_ok. valid A gens: 0..=a_gens_ok.
    // For a B gen q, the matching A gen is q - delta.
    let map_a = |q: usize| -> Option<usize> {
        let a = q as i64 - delta as i64;
        if a >= 0 && (a as usize) <= a_gens_ok && (a as usize) < fa.len() {
            Some(a as usize)
        } else {
            None
        }
    };
    let matches = |q: usize| -> bool {
        if q >= fb.len() || q > b_gens_ok {
            return false;
        }
        match map_a(q) {
            Some(a) => fb[q] == fa[a],
            None => false,
        }
    };

    // find first convergence gen >= edit_end_b
    let mut conv_gen = None;
    let mut first_mismatch = None;
    let last_b = b_gens_ok.min(fb.len().saturating_sub(1));
    for q in edit_end_b..=last_b {
        if matches(q) {
            conv_gen = Some(q);
            break;
        } else if first_mismatch.is_none() && map_a(q).is_some() {
            first_mismatch = Some(q);
        }
    }

    let (dist, rediverged) = match conv_gen {
        Some(q) => {
            let dist = q - edit_end_b;
            // stability: check next `stability` gens AND the last gen.
            let mut rediv = false;
            let upper = (q + stability).min(last_b);
            for qq in (q + 1)..=upper {
                if map_a(qq).is_some() && !matches(qq) {
                    rediv = true;
                    break;
                }
            }
            // final gen check
            if !rediv && map_a(last_b).is_some() && !matches(last_b) {
                rediv = true;
            }
            (Some(dist), rediv)
        }
        None => (None, false),
    };

    ConvResult { dist, conv_gen, rediverged, first_mismatch_gen: first_mismatch }
}

// ---------------------------------------------------------------------------
// Blocker attribution
// ---------------------------------------------------------------------------

/// Decompose why fB[q] != fA[q-delta] at a given gen. Recomputes both ctxs.
struct Attribution {
    /// per-symbol_id: (count in B only, count in A only) for watcher roots.
    root_delta: BTreeMap<i32, (i64, i64)>,
    main_chain_differs: bool,
    cond_differs: bool,
    /// strict-only: report shadow differs (semantic matched but strict didn't).
    report_shadow_differs: bool,
}

/// Compute a per-root multiset fingerprint keyed by (symbol_id, gen-encoded start),
/// so we can diff which roots are present. Returns map root-key -> count.
fn root_multiset(ctx: &ParsingCtx, p: i32, cur: i32) -> BTreeMap<(i32, u64), usize> {
    let mut m: BTreeMap<(i32, u64), usize> = BTreeMap::new();
    for root in ctx.paths.keys() {
        *m.entry((root.symbol_id, enc_gen(root.start_gen, p, cur))).or_default() += 1;
    }
    m
}

fn attribute(
    parser: &Mgroup3Parser,
    a_chars: &[char],
    b_chars: &[char],
    p: i32,
    delta: i32,
    q_b: usize,
) -> Option<Attribution> {
    let q_a = (q_b as i64 - delta as i64) as usize;
    let ctx_b = ctx_at_gen(parser, b_chars, q_b)?;
    let ctx_a = ctx_at_gen(parser, a_chars, q_a)?;
    let cur_b = ctx_b.gen_idx;
    let cur_a = ctx_a.gen_idx;

    // (i) watcher root set delta by symbol_id
    let rb = root_multiset(&ctx_b, p, cur_b);
    let ra = root_multiset(&ctx_a, p, cur_a);
    let mut root_delta: BTreeMap<i32, (i64, i64)> = BTreeMap::new();
    let mut allkeys: HashSet<(i32, u64)> = HashSet::new();
    allkeys.extend(rb.keys().copied());
    allkeys.extend(ra.keys().copied());
    for k in allkeys {
        let cb = *rb.get(&k).unwrap_or(&0) as i64;
        let ca = *ra.get(&k).unwrap_or(&0) as i64;
        if cb != ca {
            let e = root_delta.entry(k.0).or_insert((0, 0));
            if cb > ca {
                e.0 += cb - ca;
            } else {
                e.1 += ca - cb;
            }
        }
    }

    // (ii) main chain: compare the main root's PathMap fingerprint (semantic)
    let main_b = ctx_b.paths.get(&ctx_b.main_root);
    let main_a = ctx_a.paths.get(&ctx_a.main_root);
    let main_chain_differs = match (main_b, main_a) {
        (Some(mb), Some(ma)) => {
            fp_pathmap(mb, p, cur_b, Variant::Semantic) != fp_pathmap(ma, p, cur_a, Variant::Semantic)
        }
        (None, None) => false,
        _ => true,
    };

    // (iii) condition anchors: compare a cond-only fingerprint of the whole state.
    // (Full semantic already differs; this isolates whether the condition trees
    // differ even when shapes match. We approximate by fingerprinting only the
    // conditions per root, multiset.)
    let cond_only = |ctx: &ParsingCtx, cur: i32| -> Fp {
        let mut roots: Vec<Fp> = Vec::new();
        for (root, pm) in ctx.paths.iter() {
            let rid = [root.symbol_id as u64, enc_gen(root.start_gen, p, cur)];
            let mut conds: Vec<Fp> = Vec::new();
            for cond in pm.values() {
                conds.push(fp_cond(cond, p, cur));
            }
            let cm = fp_multiset(0xC012, conds);
            let (ra_, rb_) = lanes(cm);
            roots.push(fp2(combine(fx(0x8C10, &rid), ra_), combine(fx(0x8C11, &rid), rb_)));
        }
        fp_multiset(0xC55E, roots)
    };
    let cond_differs = cond_only(&ctx_b, cur_b) != cond_only(&ctx_a, cur_a);

    // (iv) report shadow: does STRICT differ while SEMANTIC matches?
    let sem_b = fp_state(&ctx_b, p, cur_b, Variant::Semantic);
    let sem_a = fp_state(&ctx_a, p, cur_a, Variant::Semantic);
    let str_b = fp_state(&ctx_b, p, cur_b, Variant::Strict);
    let str_a = fp_state(&ctx_a, p, cur_a, Variant::Strict);
    let report_shadow_differs = (sem_b == sem_a) && (str_b != str_a);

    Some(Attribution { root_delta, main_chain_differs, cond_differs, report_shadow_differs })
}

// ---------------------------------------------------------------------------
// Distribution helpers
// ---------------------------------------------------------------------------

fn percentile(sorted: &[usize], pct: f64) -> Option<usize> {
    if sorted.is_empty() {
        return None;
    }
    let idx = ((pct / 100.0) * (sorted.len() as f64 - 1.0)).round() as usize;
    Some(sorted[idx.min(sorted.len() - 1)])
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let raw: Vec<String> = std::env::args().skip(1).collect();
    let json = raw.iter().any(|a| a == "--json");
    let mut edits_per_file = 0usize; // 0 = no cap
    let mut stability = 50usize;
    let mut attrib_topk = 6usize;
    let mut files: Vec<String> = Vec::new();
    let mut pos_iter = raw.iter().filter(|a| !a.starts_with("--"));
    let data_path = pos_iter
        .next()
        .cloned()
        .expect("usage: incremental_probe <parserdata.pb> [--json] [--edits-per-file N] [--stability M] [--file PATH]...");
    // flags with values
    {
        let mut it = raw.iter().peekable();
        while let Some(a) = it.next() {
            match a.as_str() {
                "--edits-per-file" => {
                    if let Some(v) = it.next() {
                        edits_per_file = v.parse().unwrap_or(0);
                    }
                }
                "--stability" => {
                    if let Some(v) = it.next() {
                        stability = v.parse().unwrap_or(50);
                    }
                }
                "--attrib-topk" => {
                    if let Some(v) = it.next() {
                        attrib_topk = v.parse().unwrap_or(6);
                    }
                }
                "--file" => {
                    if let Some(v) = it.next() {
                        files.push(v.clone());
                    }
                }
                _ => {}
            }
        }
    }

    if files.is_empty() {
        files = vec![
            "/Users/joonsoo/Documents/workspace/mulang/bibix4/main/bibix4-stdlib/jar.bbx".into(),
            "/Users/joonsoo/Documents/workspace/mulang/bibix4/main/bibix4-stdlib/cc.bbx".into(),
            "/Users/joonsoo/Documents/workspace/mulang/examples/ccgen.mu".into(),
            "/Users/joonsoo/Documents/workspace/mulang/examples/chain_boundaries.mu".into(),
        ];
    }

    eprintln!("[load] decoding parser data: {}", data_path);
    let proto = Mgroup3ParserData::decode(std::fs::read(&data_path)?.as_slice())?;
    let parser = Mgroup3Parser::new(proto);
    eprintln!("[load] parser ready");

    // =====================================================================
    // SELF-VERIFICATION
    // =====================================================================
    // Use the first available, non-trivially-sized corpus file for the checks.
    let mut selfverify_input: Option<Vec<char>> = None;
    for f in &files {
        if let Ok(s) = std::fs::read_to_string(f) {
            let c: Vec<char> = s.chars().collect();
            if c.len() >= 20 {
                selfverify_input = Some(c);
                break;
            }
        }
    }
    let sv_chars = selfverify_input.ok_or("no readable corpus file for self-verification")?;

    let mut sv_ok = true;
    let mut sv_notes: Vec<String> = Vec::new();

    // IDENTITY: parse the same text twice, delta=0, p arbitrary (mid-file).
    {
        let p = (sv_chars.len() / 2) as i32;
        let ta = parse_trace(&parser, &sv_chars, p);
        let tb = parse_trace(&parser, &sv_chars, p);
        if ta.parse_error.is_some() || tb.parse_error.is_some() {
            sv_ok = false;
            sv_notes.push(format!(
                "IDENTITY: parse error (a={:?} b={:?})",
                ta.parse_error, tb.parse_error
            ));
        } else {
            let n = ta.semantic.len().min(tb.semantic.len());
            let mut sem_all = true;
            let mut str_all = true;
            for q in 0..n {
                if ta.semantic[q] != tb.semantic[q] {
                    sem_all = false;
                }
                if ta.strict[q] != tb.strict[q] {
                    str_all = false;
                }
            }
            if !sem_all || !str_all {
                sv_ok = false;
                sv_notes.push(format!(
                    "IDENTITY: mismatch (semantic_all_eq={} strict_all_eq={})",
                    sem_all, str_all
                ));
            } else {
                sv_notes.push(format!("IDENTITY: OK ({} gens, semantic+strict all match)", n));
            }
        }
    }

    // NO-OP EDIT: replace one char with the SAME char (delta=0) mid-file.
    {
        let p = sv_chars.len() / 2;
        let same = sv_chars[p];
        let edit = mk_replace(&sv_chars, p, 1, &[same], "noop");
        let pa = p as i32;
        let ta = parse_trace(&parser, &sv_chars, pa);
        let tb = parse_trace(&parser, &edit.new_chars, pa);
        if ta.parse_error.is_some() || tb.parse_error.is_some() {
            sv_ok = false;
            sv_notes.push("NO-OP: parse error".to_string());
        } else {
            let conv = compute_convergence(
                &ta.semantic,
                &tb.semantic,
                0,
                edit.edit_end_b(),
                ta.gens_ok,
                tb.gens_ok,
                stability,
            );
            match conv.dist {
                Some(0) if !conv.rediverged => {
                    sv_notes.push("NO-OP: OK (immediate convergence, distance 0, stable)".to_string())
                }
                Some(d) => {
                    sv_ok = false;
                    sv_notes.push(format!(
                        "NO-OP: NOT immediate (distance {}, rediverged={})",
                        d, conv.rediverged
                    ));
                }
                None => {
                    sv_ok = false;
                    sv_notes.push("NO-OP: never converged".to_string());
                }
            }
        }
    }

    println!("=== SELF-VERIFICATION ===");
    for n in &sv_notes {
        println!("  {}", n);
    }
    println!("  overall: {}", if sv_ok { "PASS" } else { "*** FAIL — matrix numbers below are INVALID ***" });
    println!();

    // =====================================================================
    // MEASUREMENT MATRIX
    // =====================================================================
    #[derive(Clone)]
    struct Row {
        file: String,
        edit: String,
        p_char: usize,
        delta: i32,
        err_side: &'static str,
        parse_err: Option<String>,
        sem_dist: Option<usize>,
        sem_rediv: bool,
        str_dist: Option<usize>,
        str_rediv: bool,
        // for Phase I reference: reparse length from edit_end to file end.
        reparse_tail: usize,
    }

    let mut rows: Vec<Row> = Vec::new();
    // attribution buckets: symbol_id -> summed (B-only, A-only) counts over late cases
    let mut attrib_root: BTreeMap<i32, (i64, i64)> = BTreeMap::new();
    let mut attrib_main = 0u64;
    let mut attrib_cond = 0u64;
    let mut attrib_report = 0u64;
    let mut attrib_cases = 0u64;
    let mut attrib_examples: Vec<String> = Vec::new();

    // collect distances for distribution
    let mut sem_dists: Vec<usize> = Vec::new();
    let mut str_dists: Vec<usize> = Vec::new();
    let mut sem_never = 0u64;
    let mut str_never = 0u64;
    let mut sem_rediv_count = 0u64;
    let mut str_rediv_count = 0u64;

    for f in &files {
        let content = match std::fs::read_to_string(f) {
            Ok(c) => c,
            Err(e) => {
                eprintln!("[skip] cannot read {}: {}", f, e);
                continue;
            }
        };
        let orig: Vec<char> = content.chars().collect();
        let short = f.rsplit('/').next().unwrap_or(f).to_string();
        eprintln!("[file] {} ({} chars)", short, orig.len());

        // A trace once per (file, anchor p) — but p differs per edit, so trace per edit.
        let edits = build_edits(&orig, edits_per_file);
        for edit in &edits {
            let p = edit.p_char as i32;
            let ta = parse_trace(&parser, &orig, p);
            let tb = parse_trace(&parser, &edit.new_chars, p);
            // distinguish which side errored: A = original unparseable (corpus/
            // fixture concern), B = the edit broke the parse (expected for
            // boundary-agnostic keystroke edits). "AB" = both.
            let err_side = match (ta.parse_error.is_some(), tb.parse_error.is_some()) {
                (true, true) => "AB",
                (true, false) => "A",
                (false, true) => "B",
                (false, false) => "",
            };
            let parse_err = ta.parse_error.clone().or_else(|| tb.parse_error.clone());
            let reparse_tail = ta.gens_ok.saturating_sub(edit.edit_end_b());

            let (sem, strc) = if parse_err.is_some() {
                (
                    ConvResult { dist: None, conv_gen: None, rediverged: false, first_mismatch_gen: None },
                    ConvResult { dist: None, conv_gen: None, rediverged: false, first_mismatch_gen: None },
                )
            } else {
                let sem = compute_convergence(
                    &ta.semantic, &tb.semantic, edit.delta(), edit.edit_end_b(),
                    ta.gens_ok, tb.gens_ok, stability,
                );
                let strc = compute_convergence(
                    &ta.strict, &tb.strict, edit.delta(), edit.edit_end_b(),
                    ta.gens_ok, tb.gens_ok, stability,
                );
                (sem, strc)
            };

            // distribution accounting (only for parse-OK rows)
            if parse_err.is_none() {
                match sem.dist {
                    Some(d) => sem_dists.push(d),
                    None => sem_never += 1,
                }
                match strc.dist {
                    Some(d) => str_dists.push(d),
                    None => str_never += 1,
                }
                if sem.rediverged {
                    sem_rediv_count += 1;
                }
                if strc.rediverged {
                    str_rediv_count += 1;
                }

                // ATTRIBUTION for "late" semantic cases (distance > 0) or never-converged.
                let is_late = matches!(sem.dist, Some(d) if d > 0) || sem.dist.is_none() || sem.rediverged;
                if is_late {
                    // choose the gen to attribute: first semantic mismatch, else
                    // (if immediate-but-rediverged) the convergence gen+1.
                    let attr_gen = sem.first_mismatch_gen.or_else(|| sem.conv_gen.map(|g| g + 1));
                    if let Some(qb) = attr_gen {
                        if let Some(a) =
                            attribute(&parser, &orig, &edit.new_chars, p, edit.delta(), qb)
                        {
                            attrib_cases += 1;
                            for (sid, (b_only, a_only)) in a.root_delta {
                                let e = attrib_root.entry(sid).or_insert((0, 0));
                                e.0 += b_only;
                                e.1 += a_only;
                            }
                            if a.main_chain_differs {
                                attrib_main += 1;
                            }
                            if a.cond_differs {
                                attrib_cond += 1;
                            }
                            if a.report_shadow_differs {
                                attrib_report += 1;
                            }
                            if attrib_examples.len() < 12 {
                                attrib_examples.push(format!(
                                    "{}::{} @gen {} main={} cond={} reportOnly={}",
                                    short, edit.label, qb, a.main_chain_differs, a.cond_differs,
                                    a.report_shadow_differs,
                                ));
                            }
                        }
                    }
                }
            }

            rows.push(Row {
                file: short.clone(),
                edit: edit.label.clone(),
                p_char: edit.p_char,
                delta: edit.delta(),
                err_side,
                parse_err,
                sem_dist: sem.dist,
                sem_rediv: sem.rediverged,
                str_dist: strc.dist,
                str_rediv: strc.rediverged,
                reparse_tail,
            });
        }
    }

    // ---- table ----
    println!("=== CONVERGENCE MATRIX (distance = chars past edit-end until reuse) ===");
    println!(
        "{:<22} {:<26} {:>7} {:>6} {:>7} {:>9} {:>9} {:>9} {:>9}",
        "file", "edit", "p", "delta", "tail", "sem_dist", "sem_rdv", "str_dist", "str_rdv"
    );
    for r in &rows {
        let semd = match (r.parse_err.is_some(), r.sem_dist) {
            (true, _) => format!("ERR:{}", r.err_side),
            (false, Some(d)) => d.to_string(),
            (false, None) => "NEVER".to_string(),
        };
        let strd = match (r.parse_err.is_some(), r.str_dist) {
            (true, _) => format!("ERR:{}", r.err_side),
            (false, Some(d)) => d.to_string(),
            (false, None) => "NEVER".to_string(),
        };
        println!(
            "{:<22} {:<26} {:>7} {:>6} {:>7} {:>9} {:>9} {:>9} {:>9}",
            r.file,
            r.edit,
            r.p_char,
            r.delta,
            r.reparse_tail,
            semd,
            if r.sem_rediv { "yes" } else { "-" },
            strd,
            if r.str_rediv { "yes" } else { "-" },
        );
    }
    let err_a = rows.iter().filter(|r| r.err_side == "A" || r.err_side == "AB").count();
    let err_b = rows.iter().filter(|r| r.err_side == "B").count();
    println!(
        "  (ERR:A = ORIGINAL unparseable [fixture/corpus concern]; ERR:B = the EDIT broke the parse [expected for boundary-agnostic keystroke edits]; counts: ERR:A/AB={} ERR:B={})",
        err_a, err_b
    );
    println!();

    // ---- distribution summary ----
    let summarize = |name: &str, dists: &mut Vec<usize>, never: u64, rediv: u64| {
        dists.sort_unstable();
        let n = dists.len();
        let median = percentile(dists, 50.0);
        let p90 = percentile(dists, 90.0);
        let max = dists.last().copied();
        let mean = if n > 0 {
            dists.iter().sum::<usize>() as f64 / n as f64
        } else {
            0.0
        };
        let zero = dists.iter().filter(|&&d| d == 0).count();
        println!(
            "  {:<9} n={:<4} converged_immediately(d=0)={:<4} median={:?} mean={:.1} p90={:?} max={:?} never={} rediverged={}",
            name, n, zero, median, mean, p90, max, never, rediv
        );
    };
    println!("=== DISTANCE DISTRIBUTION (parse-OK edits only) ===");
    let mut sd = sem_dists.clone();
    let mut td = str_dists.clone();
    summarize("semantic", &mut sd, sem_never, sem_rediv_count);
    summarize("strict", &mut td, str_never, str_rediv_count);
    println!();

    // ---- blocker attribution ----
    println!("=== BLOCKER ATTRIBUTION (late/never/rediverged semantic cases) ===");
    println!("  cases analyzed: {}", attrib_cases);
    if attrib_cases > 0 {
        println!(
            "  first-mismatch component presence:  main_chain={} cond_anchors={} report_shadow_only={}",
            attrib_main, attrib_cond, attrib_report
        );
        println!("  watcher-root delta by symbol_id (B-only / A-only, summed over cases):");
        // sort by total magnitude
        let mut items: Vec<(i32, (i64, i64))> = attrib_root.iter().map(|(k, v)| (*k, *v)).collect();
        items.sort_by_key(|(_, (b, a))| -(b.abs() + a.abs()));
        for (sid, (b, a)) in items.iter().take(attrib_topk) {
            println!("    symbol_id {:>6}:  B-only=+{:<6} A-only=+{}", sid, b, a);
        }
        println!("  examples (first mismatching gen):");
        for ex in attrib_examples.iter().take(12) {
            println!("    {}", ex);
        }
    }
    println!();

    // ---- Phase I reference ----
    println!("=== PHASE I REFERENCE (reparse-tail length by edit position) ===");
    println!("  reparse tail = chars from edit-end to file end (what Phase I prefix-resume still recomputes).");
    let mut by_pct: BTreeMap<String, Vec<usize>> = BTreeMap::new();
    for r in &rows {
        if r.parse_err.is_some() {
            continue;
        }
        // bucket by the % marker embedded in the edit label
        let bucket = if r.edit.contains("25%") {
            "25%"
        } else if r.edit.contains("50%") {
            "50%"
        } else if r.edit.contains("75%") {
            "75%"
        } else {
            "special"
        };
        by_pct.entry(bucket.to_string()).or_default().push(r.reparse_tail);
    }
    for (b, v) in &by_pct {
        let mut vv = v.clone();
        vv.sort_unstable();
        let mean = vv.iter().sum::<usize>() as f64 / vv.len().max(1) as f64;
        println!(
            "  pos {:<8} n={:<3} mean_tail={:.0} median={:?} max={:?}",
            b, vv.len(), mean, percentile(&vv, 50.0), vv.last()
        );
    }
    println!();

    // ---- limitations ----
    println!("=== LIMITATIONS / APPROXIMATIONS ===");
    println!("  - Fingerprint is a 128-bit dual hash; convergence = fingerprint equality (no");
    println!("    byte-for-byte state compare). Collision probability negligible for this scale.");
    println!("  - Convergence is measured on the LIVE state (ctx.paths). History-splice");
    println!("    feasibility is proxied by the strict variant (report shadow folded in).");
    println!("  - Attribution recomputes ctx at the first-mismatch gen (2 extra parses/case).");
    println!("  - 'reparse tail' is a raw char count; it does not model per-step cost variation.");
    println!("  - Edits do not respect token boundaries (deliberately — LSP keystroke reality).");
    println!("  - Anchor p is the edit's char index; gens<=p prefix-anchored, gens>p suffix-anchored.");

    if json {
        // compact JSON summary on stderr
        sd.sort_unstable();
        td.sort_unstable();
        eprintln!(
            "JSON_BEGIN{{\"selfverify\":{},\"rows\":{},\"sem\":{{\"n\":{},\"median\":{:?},\"p90\":{:?},\"max\":{:?},\"never\":{},\"rediv\":{}}},\"str\":{{\"n\":{},\"median\":{:?},\"p90\":{:?},\"max\":{:?},\"never\":{},\"rediv\":{}}}}}JSON_END",
            sv_ok,
            rows.len(),
            sd.len(), percentile(&sd, 50.0), percentile(&sd, 90.0), sd.last(), sem_never, sem_rediv_count,
            td.len(), percentile(&td, 50.0), percentile(&td, 90.0), td.last(), str_never, str_rediv_count,
        );
    }

    Ok(())
}
