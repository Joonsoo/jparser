//! Fuzzing oracle for the Stage-1 AST-delta reuse boundary (`EditReuse`).
//!
//! Design: `mgroup3/docs/lsp_result_boundary.md` §2. After each edit the session
//! exposes `edit_reuse()` — for a SPLICE edit it promises which gens of the new
//! `kernels_history` are the previous parse's, verbatim or shift-adjusted. This
//! test proves that promise exhaustively:
//!
//!   * capture `old = kernels_history` BEFORE every edit,
//!   * after the edit take `reuse = edit_reuse()` and a fresh `KernelsQuery`,
//!   * assert, for EVERY gen (no sampling):
//!       - `g < reuse.dirty_lo`  ⇒  `query.at(g) == old[g]`           (verbatim)
//!       - `g > reuse.dirty_hi`  ⇒  `query.at(g) == shift(old[g-δ])`  (suffix)
//!
//! If a counterexample is found the test STOPS with a full dump (document, edit,
//! gen, both kernel sets) — per the task, a break means the `dirty_lo` formula is
//! unsound and must be revisited, NOT that the assertion should be weakened.
//!
//! Two grammars: the committed `nested_repeat` (`('a' 'b')+`) fixture, and the
//! real mulang grammar (parserdata resolved like `session_diff`, gracefully
//! skipped when absent). Statistics (splice ratio, dirty-window size,
//! prefix-expansion `resume_gen - dirty_lo`) are printed for the design review.

use std::path::PathBuf;
use std::sync::Arc;

use mgroup3_native::parser::Mgroup3Parser;
use mgroup3_native::parsing_ctx::{KtlibKernel, ParsingCtx};
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use mgroup3_native::session::{EditReuse, ParseOutcome, ParseSession};
use prost::Message;
use rustc_hash::FxHashSet as HashSet;

// ---------------------------------------------------------------------------
// Deterministic PRNG (xorshift64*) — mirrors session_diff.
// ---------------------------------------------------------------------------
struct Rng(u64);
impl Rng {
    fn new(seed: u64) -> Self {
        Rng(seed | 1)
    }
    fn next_u64(&mut self) -> u64 {
        let mut x = self.0;
        x ^= x >> 12;
        x ^= x << 25;
        x ^= x >> 27;
        self.0 = x;
        x.wrapping_mul(0x2545F4914F6CDD1D)
    }
    fn below(&mut self, n: usize) -> usize {
        if n == 0 { 0 } else { (self.next_u64() % n as u64) as usize }
    }
}

// ---------------------------------------------------------------------------
// Parser / corpus resolution (mirrors session_diff.rs).
// ---------------------------------------------------------------------------
fn nested_repeat_parser() -> Arc<Mgroup3Parser> {
    let p = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("tests/fixtures/parser/nested_repeat/data.pb");
    let bytes = std::fs::read(&p).expect("read nested_repeat data.pb");
    let data = Mgroup3ParserData::decode(bytes.as_slice()).expect("decode nested_repeat fixture");
    Arc::new(Mgroup3Parser::new(data))
}

fn read_parserdata_bytes() -> Option<Vec<u8>> {
    let path = resolve_parserdata_path()?;
    let raw = std::fs::read(&path).ok()?;
    if path.extension().and_then(|e| e.to_str()) == Some("gz") {
        use flate2::read::GzDecoder;
        use std::io::Read;
        let mut buf = Vec::new();
        GzDecoder::new(raw.as_slice()).read_to_end(&mut buf).ok()?;
        Some(buf)
    } else {
        Some(raw)
    }
}

fn resolve_parserdata_path() -> Option<PathBuf> {
    if let Ok(p) = std::env::var("MG3_MULANG_PARSERDATA") {
        let pb = PathBuf::from(p);
        if pb.exists() {
            return Some(pb);
        }
    }
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

fn mulang_parser() -> Option<Arc<Mgroup3Parser>> {
    let bytes = read_parserdata_bytes()?;
    let data = Mgroup3ParserData::decode(bytes.as_slice()).expect("decode mulang parserdata");
    Some(Arc::new(Mgroup3Parser::new(data)))
}

fn corpus_files() -> Vec<PathBuf> {
    let root = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("tests/fixtures/session");
    let mut out = Vec::new();
    if let Ok(rd) = std::fs::read_dir(&root) {
        for e in rd.flatten() {
            let p = e.path();
            if p.extension().and_then(|x| x.to_str()).map(|x| x == "mu" || x == "bbx").unwrap_or(false) {
                out.push(p);
            }
        }
    }
    out.sort();
    out
}

// ---------------------------------------------------------------------------
// Edit helpers (mirror session_diff.rs).
// ---------------------------------------------------------------------------
fn apply(doc: &mut Vec<char>, pos: usize, old_len: usize, new_text: &str) {
    let pos = pos.min(doc.len());
    let end = (pos + old_len).min(doc.len());
    let ins: Vec<char> = new_text.chars().collect();
    doc.splice(pos..end, ins);
}

fn is_ident_char(c: char) -> bool {
    c.is_alphanumeric() || c == '_'
}

fn find_ident_span(chars: &[char], from: usize) -> Option<(usize, usize)> {
    let n = chars.len();
    let mut i = from.min(n.saturating_sub(1));
    while i < n && !is_ident_char(chars[i]) {
        i += 1;
    }
    if i >= n {
        return None;
    }
    let mut s = i;
    while s > 0 && is_ident_char(chars[s - 1]) {
        s -= 1;
    }
    let mut e = i;
    while e < n && is_ident_char(chars[e]) {
        e += 1;
    }
    if chars[s].is_alphabetic() || chars[s] == '_' {
        Some((s, e))
    } else {
        None
    }
}

// ---------------------------------------------------------------------------
// The reuse contract check — the heart of the oracle.
// ---------------------------------------------------------------------------

/// `shift(k, pivot, delta)`: the same split rebase `rebase.rs` applies — a kernel
/// gen strictly greater than the pivot shifts by delta, else stays.
fn shift_gen(g: i32, pivot: i32, delta: i32) -> i32 {
    if g > pivot { g + delta } else { g }
}

fn shift_kernels(set: &HashSet<KtlibKernel>, pivot: i32, delta: i32) -> HashSet<KtlibKernel> {
    set.iter()
        .map(|k| KtlibKernel {
            symbol_id: k.symbol_id,
            pointer: k.pointer,
            begin_gen: shift_gen(k.begin_gen, pivot, delta),
            end_gen: shift_gen(k.end_gen, pivot, delta),
        })
        .collect()
}

fn ksorted(s: &HashSet<KtlibKernel>) -> Vec<KtlibKernel> {
    let mut v: Vec<_> = s.iter().copied().collect();
    v.sort_by_key(|k| (k.symbol_id, k.pointer, k.begin_gen, k.end_gen));
    v
}

/// Per-spliced-edit statistics gathered for the design review.
#[derive(Clone, Copy)]
struct ReuseStat {
    dirty_size: usize,          // dirty_hi - dirty_lo + 1
    prefix_expansion: usize,    // resume_gen - dirty_lo (closure, Stage 1.1)
    prefix_expansion_d0: usize, // resume_gen - dirty_lo_d0 (D0-only, Stage 1.0)
    dirty_lo_zero: bool,
    verbatim_gens: usize,       // gens proven verbatim-reusable (== dirty_lo)
    suffix_gens: usize,         // gens proven shift-reusable
}

/// Verify the `EditReuse` contract for one edit and return a stat if it spliced.
/// PANICS (stops the run) on the first counterexample, with a full dump.
fn check_edit_reuse(
    label: &str,
    edit_no: usize,
    edit_desc: &str,
    parser: &Mgroup3Parser,
    session: &ParseSession,
    old: Option<&Vec<HashSet<KtlibKernel>>>,
    reuse: EditReuse,
) -> Option<ReuseStat> {
    if !reuse.spliced {
        return None;
    }
    let old = old.expect("a spliced edit must have had an Ok baseline to capture `old` from");
    // The current (post-edit) parse must be Ok for a splice.
    let ctx: &ParsingCtx = match session.outcome() {
        Some(ParseOutcome::Ok(c)) => c,
        _ => panic!("{label} edit#{edit_no}: spliced but current outcome is not Ok"),
    };
    let query = parser.kernels_query(ctx);
    let new_len = query.num_gens();
    let delta = reuse.delta;
    let pivot = reuse.pivot;

    // Length invariant: new_len == old_len + delta.
    assert_eq!(
        new_len as i32,
        old.len() as i32 + delta,
        "{label} edit#{edit_no} ({edit_desc}): new_len {new_len} != old_len {} + delta {delta}",
        old.len()
    );

    // --- verbatim prefix: g < dirty_lo ⇒ query.at(g) == old[g] ---
    let mut verbatim_gens = 0usize;
    for g in 0..reuse.dirty_lo {
        let got = query.at(g);
        let want = &old[g];
        if &got != want {
            panic!(
                "{}\nVERBATIM-PREFIX REUSE VIOLATED\n  {label} edit#{edit_no} ({edit_desc})\n  \
                 reuse = {reuse:?}\n  gen g={g} (< dirty_lo={})\n  new (query.at):\n{}\n  \
                 old[g]:\n{}\n  doc_head={:?}",
                "=== COUNTEREXAMPLE (dirty_lo formula insufficient) ===",
                reuse.dirty_lo,
                dump_kernels(&got),
                dump_kernels(want),
                session.document().iter().take(160).collect::<String>(),
            );
        }
        verbatim_gens += 1;
    }

    // --- shift suffix: g > dirty_hi ⇒ query.at(g) == shift(old[g-delta]) ---
    let mut suffix_gens = 0usize;
    for g in (reuse.dirty_hi + 1)..new_len {
        let og_i = g as i64 - delta as i64;
        assert!(
            og_i >= 0 && (og_i as usize) < old.len(),
            "{label} edit#{edit_no}: suffix gen g={g} maps to out-of-range old gen {og_i}"
        );
        let og = og_i as usize;
        let got = query.at(g);
        let want = shift_kernels(&old[og], pivot, delta);
        if got != want {
            panic!(
                "{}\nSUFFIX SHIFT REUSE VIOLATED\n  {label} edit#{edit_no} ({edit_desc})\n  \
                 reuse = {reuse:?}\n  new gen g={g} ↔ old gen og={og}\n  new (query.at):\n{}\n  \
                 shift(old[og]):\n{}\n  doc_head={:?}",
                "=== COUNTEREXAMPLE (suffix reuse unsound) ===",
                dump_kernels(&got),
                dump_kernels(&want),
                session.document().iter().take(160).collect::<String>(),
            );
        }
        suffix_gens += 1;
    }

    // A/B: the pre-closure (Stage 1.0) D0-only dirty_lo. The closure can only
    // LOWER dirty_lo (D ⊇ D0), so its prefix expansion is >= the D0-only one.
    let d0_lo = session
        .dbg_d0_dirty_lo()
        .expect("spliced edit_reuse should populate d0 dirty_lo");
    assert!(
        reuse.dirty_lo <= d0_lo,
        "{label} edit#{edit_no}: closure dirty_lo {} > D0-only {} (closure must never raise it)",
        reuse.dirty_lo, d0_lo
    );

    let dirty_hi = reuse.dirty_hi;
    let dirty_lo = reuse.dirty_lo;
    Some(ReuseStat {
        dirty_size: dirty_hi + 1 - dirty_lo,
        prefix_expansion: reuse.resume_gen.saturating_sub(dirty_lo),
        prefix_expansion_d0: reuse.resume_gen.saturating_sub(d0_lo),
        dirty_lo_zero: dirty_lo == 0,
        verbatim_gens,
        suffix_gens,
    })
}

fn dump_kernels(s: &HashSet<KtlibKernel>) -> String {
    let mut out = String::new();
    for k in ksorted(s) {
        out.push_str(&format!("    {} {} {} {}\n", k.symbol_id, k.pointer, k.begin_gen, k.end_gen));
    }
    out
}

// ---------------------------------------------------------------------------
// Statistics aggregation + reporting.
// ---------------------------------------------------------------------------
#[derive(Default)]
struct Aggregate {
    edits: usize,
    spliced: usize,
    dirty_lo_zero: usize,
    dirty_sizes: Vec<usize>,
    prefix_expansions: Vec<usize>,
    prefix_expansions_d0: Vec<usize>,
    widened: usize, // spliced edits where closure lowered dirty_lo below D0-only
    total_verbatim: usize,
    total_suffix: usize,
}
impl Aggregate {
    fn record(&mut self, stat: Option<ReuseStat>) {
        self.edits += 1;
        if let Some(s) = stat {
            self.spliced += 1;
            if s.dirty_lo_zero {
                self.dirty_lo_zero += 1;
            }
            if s.prefix_expansion > s.prefix_expansion_d0 {
                self.widened += 1;
            }
            self.dirty_sizes.push(s.dirty_size);
            self.prefix_expansions.push(s.prefix_expansion);
            self.prefix_expansions_d0.push(s.prefix_expansion_d0);
            self.total_verbatim += s.verbatim_gens;
            self.total_suffix += s.suffix_gens;
        }
    }
    fn report(&self, label: &str) {
        let pct = |a: usize, b: usize| if b == 0 { 0.0 } else { 100.0 * a as f64 / b as f64 };
        let stat = |v: &mut Vec<usize>| -> (usize, usize, usize) {
            if v.is_empty() {
                return (0, 0, 0);
            }
            v.sort_unstable();
            let median = v[v.len() / 2];
            let p90 = v[((v.len() as f64 - 1.0) * 0.9).round() as usize];
            let max = *v.last().unwrap();
            (median, p90, max)
        };
        let mut ds = self.dirty_sizes.clone();
        let mut pe = self.prefix_expansions.clone();
        let mut pe0 = self.prefix_expansions_d0.clone();
        let (ds_med, ds_p90, ds_max) = stat(&mut ds);
        let (pe_med, pe_p90, pe_max) = stat(&mut pe);
        let (pe0_med, pe0_p90, pe0_max) = stat(&mut pe0);
        eprintln!(
            "[reuse_oracle] {label}: edits={} spliced={} ({:.1}%) dirty_lo==0: {} ({:.1}% of spliced)",
            self.edits, self.spliced, pct(self.spliced, self.edits),
            self.dirty_lo_zero, pct(self.dirty_lo_zero, self.spliced),
        );
        eprintln!(
            "[reuse_oracle] {label}: dirty-window size (gens) median={ds_med} p90={ds_p90} max={ds_max}",
        );
        eprintln!(
            "[reuse_oracle] {label}: prefix-expansion AFTER (closure, Stage 1.1)  median={pe_med} p90={pe_p90} max={pe_max}",
        );
        eprintln!(
            "[reuse_oracle] {label}: prefix-expansion BEFORE (D0-only, Stage 1.0) median={pe0_med} p90={pe0_p90} max={pe0_max}",
        );
        eprintln!(
            "[reuse_oracle] {label}: closure widened dirty window on {}/{} spliced edits ({:.1}%)",
            self.widened, self.spliced, pct(self.widened, self.spliced),
        );
        eprintln!(
            "[reuse_oracle] {label}: total gens proven reusable: verbatim={} suffix={}",
            self.total_verbatim, self.total_suffix,
        );
    }
}

// ---------------------------------------------------------------------------
// Grammar 1 — nested_repeat: structured interior edits (splice-heavy).
// ---------------------------------------------------------------------------
#[test]
fn reuse_oracle_nested_repeat() {
    let parser = nested_repeat_parser();
    let mut agg = Aggregate::default();

    // A valid `('a' 'b')+` document; interior "ab" inserts/deletes keep validity
    // and splice. A few random-position (still even) inserts vary the pivot.
    let mut doc: Vec<char> = "ab".repeat(120).chars().collect(); // 240 chars
    let mut session = ParseSession::new(Arc::clone(&parser));
    session.parse_full(&doc.iter().collect::<String>());
    assert!(session.is_accepted(), "nested_repeat baseline must parse");

    let mut rng = Rng::new(0x9E37_79B9);
    for i in 0..80 {
        // Rotate: mostly interior "ab" inserts at even positions, some deletes.
        let n = doc.len();
        let (pos, old_len, new_text): (usize, usize, String) = if i % 5 == 4 && n >= 6 {
            // delete an interior "ab" pair at an even position
            let even_pairs = n / 2;
            let pair = 1 + rng.below(even_pairs.saturating_sub(2)).max(0);
            (pair * 2, 2, String::new())
        } else {
            // insert "ab" at an even interior position
            let even_pairs = (n / 2).max(1);
            let pair = 1 + rng.below(even_pairs);
            ((pair * 2).min(n), 0, "ab".to_string())
        };

        let old_hist = session.kernels_history();
        apply(&mut doc, pos, old_len, &new_text);
        session.edit(pos, old_len, &new_text);
        let desc = format!("pos={pos} old_len={old_len} new={new_text:?}");
        let reuse = session.edit_reuse();

        if let Some(reuse) = reuse {
            let stat = check_edit_reuse(
                "nested_repeat", i, &desc, &parser, &session, old_hist.as_ref(), reuse,
            );
            agg.record(stat);
        }
    }
    agg.report("nested_repeat");
    assert!(
        agg.spliced > 0,
        "nested_repeat: no splice fired — the oracle exercised no reuse (check the edit script)"
    );
}

// ---------------------------------------------------------------------------
// Grammar 2 — mulang: structured ident substitution (splice-heavy) + random.
// ---------------------------------------------------------------------------
#[test]
fn reuse_oracle_mulang() {
    let Some(parser) = mulang_parser() else {
        eprintln!(
            "note: reuse_oracle_mulang skipped — no mulang mg3 parserdata found. Set \
             MG3_MULANG_PARSERDATA or populate ~/.cache/mulang-native/*/mulang-mg3-parserdata.pb.gz."
        );
        return;
    };

    let heavy = std::env::var("MG3_SESSION_HEAVY").map(|v| v == "1").unwrap_or(false);
    const MAX_CHARS: usize = 8_000;
    const EDITS_PER_FILE: usize = 30;

    let mut agg = Aggregate::default();
    let mut checked_files = 0usize;

    for file in corpus_files() {
        let content = std::fs::read_to_string(&file).expect("read corpus file");
        let short = file.file_name().unwrap().to_string_lossy().into_owned();
        let n = content.chars().count();
        if n > MAX_CHARS && !heavy {
            eprintln!("[reuse_oracle] {short} skipped (>{MAX_CHARS} chars; set MG3_SESSION_HEAVY=1)");
            continue;
        }

        let mut session = ParseSession::new(Arc::clone(&parser));
        session.parse_full(&content);
        if !matches!(session.outcome(), Some(ParseOutcome::Ok(ctx)) if parser.is_accepted(ctx)) {
            eprintln!("[reuse_oracle] {short} skipped (clean parse not accepted)");
            continue;
        }
        checked_files += 1;

        let mut doc: Vec<char> = content.chars().collect();
        // Structured parse-preserving ident substitutions at rotating interior
        // fractions (guaranteed splices — exercise both prefix-anchored and
        // post-edit roots in the split), interleaved with occasional random
        // single-char inserts near the front/middle/end (robustness + the
        // not-spliced path).
        let fracs = [
            (1usize, 6usize), (1, 3), (1, 2), (2, 3), (5, 6), (1, 4), (3, 4), (2, 5),
        ];
        let mut rng = Rng::new(0xC0FFEE ^ (short.len() as u64).wrapping_mul(0x100000001B3));
        for i in 0..EDITS_PER_FILE {
            let n = doc.len();
            let (pos, old_len, new_text): (usize, usize, String) = if i % 4 == 3 {
                // random single-char insert distributed across the document
                let bucket = rng.below(3);
                let base = match bucket {
                    0 => 0,
                    1 => n / 2,
                    _ => n.saturating_sub(1),
                };
                let choices = ['x', ' ', '_', '9'];
                (base, 0, choices[rng.below(choices.len())].to_string())
            } else {
                let (num, den) = fracs[i % fracs.len()];
                let approx = n * num / den;
                match find_ident_span(&doc, approx) {
                    Some((s, e)) => {
                        let old: String = doc[s..e].iter().collect();
                        (s, e - s, format!("q{}9", old))
                    }
                    None => (n / 2, 0, "x".to_string()),
                }
            };

            let old_hist = session.kernels_history();
            apply(&mut doc, pos, old_len, &new_text);
            session.edit(pos, old_len, &new_text);
            let desc = format!("pos={pos} old_len={old_len} new={new_text:?} doc_len={}", doc.len());
            if let Some(reuse) = session.edit_reuse() {
                let stat = check_edit_reuse(
                    &short, i, &desc, &parser, &session, old_hist.as_ref(), reuse,
                );
                agg.record(stat);
            }
        }
        // Per-file splice sanity: structured ident edits on an accepted mulang
        // file must produce SOME splices (else the oracle exercised no reuse).
        let t = session.totals();
        eprintln!(
            "[reuse_oracle] {short}: edits={} spliced(session)={} declined_structural={}",
            t.edits, t.spliced, t.splice_declined_structural,
        );
    }

    assert!(checked_files >= 1, "no corpus file usable for the mulang reuse oracle");
    agg.report("mulang");
    assert!(
        agg.spliced > 0,
        "mulang: no splice fired across the corpus — reuse boundary exercised nothing"
    );
}

// ---------------------------------------------------------------------------
// Item (b) contract: query.at(g) is byte-identical to kernels_history(ctx)[g].
// ---------------------------------------------------------------------------
fn assert_query_matches_history(parser: &Mgroup3Parser, ctx: &ParsingCtx, label: &str) {
    let full = parser.kernels_history(ctx);
    let query = parser.kernels_query(ctx);
    assert_eq!(query.num_gens(), full.len(), "{label}: num_gens != history len");
    for (g, want) in full.iter().enumerate() {
        let got = query.at(g);
        assert_eq!(
            &got, want,
            "{label}: query.at({g}) != kernels_history[{g}]\n  got:\n{}\n  want:\n{}",
            dump_kernels(&got), dump_kernels(want)
        );
    }
    // Out-of-range gen is the empty set.
    assert!(query.at(full.len()).is_empty(), "{label}: past-end query must be empty");
}

#[test]
fn query_matches_kernels_history() {
    // nested_repeat (always available).
    let parser = nested_repeat_parser();
    for text in ["ab", "abab", "ababababab", "abababababababababab"] {
        let ctx = parser.parse(text).expect("parse nested_repeat");
        assert_query_matches_history(&parser, &ctx, &format!("nested_repeat {text:?}"));
    }

    // mulang (if available).
    if let Some(parser) = mulang_parser() {
        for file in corpus_files() {
            let n = std::fs::metadata(&file).map(|m| m.len()).unwrap_or(0);
            let heavy = std::env::var("MG3_SESSION_HEAVY").map(|v| v == "1").unwrap_or(false);
            if n > 8_000 && !heavy {
                continue;
            }
            let content = std::fs::read_to_string(&file).expect("read corpus");
            if let Ok(ctx) = parser.parse(&content) {
                let short = file.file_name().unwrap().to_string_lossy().into_owned();
                assert_query_matches_history(&parser, &ctx, &format!("mulang {short}"));
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Item 4: reference-birth invariant probe.
//
// `compute_dirty_lo` assumes a record's condition only references roots ALREADY
// born by the record's gen (`first_active(r) <= record_gen`); the dangerous-set
// closure is sound under that assumption (no dirty_lo margin needed). This test
// enables the evaluator-level probe (`birth_probe_*`), drives kernels_history
// over fresh parses AND over spliced session histories for both grammars, and
// asserts the max observed skew `first_active(referenced_root) - record_gen` is
// <= 0. A positive value would mean a record references a root born later than
// itself (e.g. a NEXT-anchored lookahead) — the invariant fails and dirty_lo
// would need exactly that skew subtracted.
// ---------------------------------------------------------------------------
fn drive_kernels_over_edits(parser: &Arc<Mgroup3Parser>, base: &str) {
    // Fresh parse.
    if let Ok(ctx) = parser.parse(base) {
        let _ = parser.kernels_history(&ctx);
    }
    // Session with a chain of structured edits — exercises spliced histories.
    let mut session = ParseSession::new(Arc::clone(parser));
    session.parse_full(base);
    if !matches!(session.outcome(), Some(ParseOutcome::Ok(_))) {
        return;
    }
    let _ = session.kernels_history();
    let mut doc: Vec<char> = base.chars().collect();
    let fracs = [(1usize, 3usize), (1, 2), (2, 3), (1, 4), (3, 4)];
    for i in 0..fracs.len() * 3 {
        let n = doc.len();
        let (num, den) = fracs[i % fracs.len()];
        let approx = n * num / den;
        let (s, e, repl) = match find_ident_span(&doc, approx) {
            Some((s, e)) => {
                let old: String = doc[s..e].iter().collect();
                (s, e, format!("q{}9", old))
            }
            None => (approx.min(n), approx.min(n), "x".to_string()),
        };
        apply(&mut doc, s, e - s, &repl);
        session.edit(s, e - s, &repl);
        // kernels_history drives evaluate() over every record of this parse.
        let _ = session.kernels_history();
    }
}

#[test]
fn reference_birth_invariant() {
    use mgroup3_native::parser::record_cond::{
        birth_probe_disable, birth_probe_enable, birth_probe_max_skew,
    };

    birth_probe_enable();

    // nested_repeat: several valid docs of varying length (fresh + spliced).
    let nr = nested_repeat_parser();
    for reps in [1usize, 2, 5, 10, 60] {
        drive_kernels_over_edits(&nr, &"ab".repeat(reps));
    }

    // mulang corpus (fresh + spliced session edits) if available.
    let heavy = std::env::var("MG3_SESSION_HEAVY").map(|v| v == "1").unwrap_or(false);
    if let Some(parser) = mulang_parser() {
        for file in corpus_files() {
            let n = std::fs::metadata(&file).map(|m| m.len()).unwrap_or(0);
            if n > 8_000 && !heavy {
                continue;
            }
            let content = std::fs::read_to_string(&file).expect("read corpus");
            drive_kernels_over_edits(&parser, &content);
        }
    }

    let skew = birth_probe_max_skew();
    birth_probe_disable();

    eprintln!(
        "[reuse_oracle] reference-birth: max skew (first_active(ref) - record_gen) = {skew} \
         (<= 0 ⇒ invariant holds; margin = {})",
        if skew == i32::MIN { 0 } else { (-skew).max(0) }
    );
    assert!(
        skew <= 0,
        "reference-birth invariant VIOLATED by {skew} gens — a record references a root born \
         {skew} gens later than itself. compute_dirty_lo must subtract this skew as a margin \
         (see the doc comment). Do NOT ship dirty_lo without the margin."
    );
}
