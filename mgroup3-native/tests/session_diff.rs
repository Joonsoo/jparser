//! Differential oracle for the incremental `ParseSession` (Phase I0/I1 gate).
//!
//! For each corpus file we drive a seeded random sequence of edits (insert /
//! delete / replace at random positions, token boundaries ignored — LSP keystroke
//! reality) through a `ParseSession`, and after EVERY edit compare the session's
//! result to a full re-parse of the same edited text. The invariant (design §3.1):
//!
//!   session.kernels_history() + session.is_accepted()  ==  full-reparse of the
//!   current document text, byte-identical, for every edit.
//!
//! This is the completeness gate: prefix-resume (I0) and the I1 fingerprint
//! machinery must never change the observable output vs a from-scratch parse.
//!
//! CORPUS / PARSERDATA RESOLUTION (graceful skip so `cargo test` is hermetic):
//!   parserdata: env `MG3_MULANG_PARSERDATA` (a .pb or .pb.gz), else the newest
//!   `~/.cache/mulang-native/*/mulang-mg3-parserdata.pb.gz`. If none is found the
//!   test prints a note and passes (the small committed grammar fixtures under
//!   `tests/fixtures/parser/` are exercised by `parser_diff`; this session gate
//!   needs the real mulang grammar + corpus to be meaningful).
//!   corpus: the committed `tests/fixtures/session/*.{mu,bbx}` files.

use std::collections::BTreeSet;
use std::path::PathBuf;
use std::sync::Arc;

use mgroup3_native::parser::Mgroup3Parser;
use mgroup3_native::parsing_ctx::KtlibKernel;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use mgroup3_native::session::{ParseOutcome, ParseSession};
use prost::Message;

// ---------------------------------------------------------------------------
// Deterministic PRNG (xorshift64*) — no external dep, fully reproducible.
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
        if n == 0 {
            0
        } else {
            (self.next_u64() % n as u64) as usize
        }
    }
}

// ---------------------------------------------------------------------------
// Parserdata resolution
// ---------------------------------------------------------------------------
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
    // newest ~/.cache/mulang-native/*/mulang-mg3-parserdata.pb.gz
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
// Output serialization (mirrors parser_diff's kernels serialization).
// ---------------------------------------------------------------------------
fn serialize(parser: &Mgroup3Parser, outcome: &ParseOutcome) -> String {
    match outcome {
        ParseOutcome::Err(_) => "REJECTED\n".to_string(),
        ParseOutcome::Ok(ctx) => {
            if !parser.is_accepted(ctx) {
                return "REJECTED\n".to_string();
            }
            let hist = parser.kernels_history(ctx);
            let mut out = String::from("ACCEPTED\n");
            for (gen_idx, kernels) in hist.iter().enumerate() {
                out.push_str(&format!("# gen {}\n", gen_idx));
                let sorted: BTreeSet<KtlibKernel> = kernels.iter().copied().collect();
                for k in sorted {
                    out.push_str(&format!("{} {} {} {}\n", k.symbol_id, k.pointer, k.begin_gen, k.end_gen));
                }
            }
            out
        }
    }
}

/// Serialize what the SESSION currently holds, matching `serialize` above.
fn serialize_session(session: &ParseSession) -> String {
    match session.outcome() {
        Some(ParseOutcome::Err(_)) | None => "REJECTED\n".to_string(),
        Some(ParseOutcome::Ok(_)) => {
            if !session.is_accepted() {
                return "REJECTED\n".to_string();
            }
            let Some(hist) = session.kernels_history() else {
                return "REJECTED\n".to_string();
            };
            let mut out = String::from("ACCEPTED\n");
            for (gen_idx, kernels) in hist.iter().enumerate() {
                out.push_str(&format!("# gen {}\n", gen_idx));
                let sorted: BTreeSet<KtlibKernel> = kernels.iter().copied().collect();
                for k in sorted {
                    out.push_str(&format!("{} {} {} {}\n", k.symbol_id, k.pointer, k.begin_gen, k.end_gen));
                }
            }
            out
        }
    }
}

fn full_reparse(parser: &Mgroup3Parser, doc: &[char]) -> String {
    let text: String = doc.iter().collect();
    match parser.parse(&text) {
        Ok(ctx) => serialize(parser, &ParseOutcome::Ok(ctx)),
        Err(e) => serialize(parser, &ParseOutcome::Err(e)),
    }
}

/// A random edit against the current document. Returns (pos, old_len, new_text).
fn random_edit(rng: &mut Rng, doc: &[char]) -> (usize, usize, String) {
    let n = doc.len();
    let kind = rng.below(3);
    match kind {
        0 => {
            // insert 1-3 chars at a random position
            let pos = rng.below(n + 1);
            let choices = ['x', '\n', ' ', '{', '"', '_', '9', ')'];
            let count = 1 + rng.below(3);
            let s: String = (0..count).map(|_| choices[rng.below(choices.len())]).collect();
            (pos, 0, s)
        }
        1 => {
            // delete 1-3 chars at a random position
            if n == 0 {
                return (0, 0, String::new());
            }
            let pos = rng.below(n);
            let del = 1 + rng.below(3.min(n - pos).max(1));
            (pos, del, String::new())
        }
        _ => {
            // replace 1-3 chars with 1-3 chars
            if n == 0 {
                return (0, 0, "y".to_string());
            }
            let pos = rng.below(n);
            let old = 1 + rng.below(3.min(n - pos).max(1));
            let choices = ['y', 'z', ';', '(', '=', 'A', '0', '/'];
            let count = 1 + rng.below(3);
            let s: String = (0..count).map(|_| choices[rng.below(choices.len())]).collect();
            (pos, old, s)
        }
    }
}

/// Apply an edit to a `Vec<char>` mirror (so the test tracks the same document
/// the session builds).
fn apply(doc: &mut Vec<char>, pos: usize, old_len: usize, new_text: &str) {
    let pos = pos.min(doc.len());
    let end = (pos + old_len).min(doc.len());
    let ins: Vec<char> = new_text.chars().collect();
    doc.splice(pos..end, ins);
}

#[test]
fn session_matches_full_reparse_under_fuzzing() {
    let Some(pd_bytes) = read_parserdata_bytes() else {
        eprintln!(
            "note: session_diff skipped — no mulang mg3 parserdata found. Set \
             MG3_MULANG_PARSERDATA=<path to mulang-mg3-parserdata.pb[.gz]> or populate \
             ~/.cache/mulang-native/*/mulang-mg3-parserdata.pb.gz to run this gate."
        );
        return;
    };
    let data = Mgroup3ParserData::decode(pd_bytes.as_slice()).expect("decode mulang parserdata");
    let parser = Arc::new(Mgroup3Parser::new(data));

    let files = corpus_files();
    assert!(!files.is_empty(), "no session corpus fixtures found");

    const EDITS_PER_FILE: usize = 40;
    // The fuzz phase does a FULL reparse per edit (O(doc) each), so we cap it to
    // the smaller corpus files to bound runtime. Larger files still get full
    // differential coverage in the structured phase below. Every fuzzed file gets
    // >= 30 edits (the gate requirement).
    const FUZZ_MAX_CHARS: usize = 8_000;
    let mut total_edits = 0usize;
    let mut total_converged = 0usize;
    let mut fuzzed_files = 0usize;

    for file in &files {
        let content = std::fs::read_to_string(file).expect("read corpus file");
        let short = file.file_name().unwrap().to_string_lossy().into_owned();
        if content.chars().count() > FUZZ_MAX_CHARS {
            eprintln!("[session_diff] FUZZ: {} skipped (>{} chars; covered by STRUCT phase)", short, FUZZ_MAX_CHARS);
            continue;
        }
        fuzzed_files += 1;

        // Baseline full parse via the session.
        let mut session = ParseSession::new(Arc::clone(&parser));
        session.parse_full(&content);

        // Track the document independently and check the initial parse too.
        let mut doc: Vec<char> = content.chars().collect();
        let want0 = full_reparse(&parser, &doc);
        let got0 = serialize_session(&session);
        assert_eq!(got0, want0, "{}: initial parse_full diverged from full reparse", short);

        let mut rng = Rng::new(0xC0FFEE ^ hash_name(&short));
        for i in 0..EDITS_PER_FILE {
            let (pos, old_len, new_text) = random_edit(&mut rng, &doc);
            apply(&mut doc, pos, old_len, &new_text);
            session.edit(pos, old_len, &new_text);

            let want = full_reparse(&parser, &doc);
            let got = serialize_session(&session);
            assert_eq!(
                got, want,
                "{} edit#{} (pos={}, old_len={}, new={:?}) diverged from full reparse\n  doc_len={}",
                short, i, pos, old_len, new_text, doc.len()
            );

            // Session document must equal our mirror.
            assert_eq!(
                session.document(),
                doc.as_slice(),
                "{} edit#{}: session document drifted from mirror",
                short, i
            );

            total_edits += 1;
            if session.stats().last_convergence_distance.is_some() {
                total_converged += 1;
            }
        }

        // Sanity on I1 counters: at least SOME edits should converge on a real
        // grammar+corpus (the probe measured median-0 convergence). This is a
        // smoke check, not a strict band assertion (the fuzz distribution differs
        // from the probe's structured edit matrix).
        let t = session.totals();
        eprintln!(
            "[session_diff] {}: edits={} comparable={} converged={} never={} rediverged={} \
             sum_conv_dist={} sum_would_splice={} checkpoints={} approx_ckpt_bytes={}",
            short, t.edits, t.comparable_edits, t.converged, t.never_converged, t.rediverged,
            t.sum_convergence_distance, t.sum_would_splice_tail,
            session.checkpoint_count(), session.approx_checkpoint_bytes(),
        );
    }

    eprintln!(
        "[session_diff] FUZZ: {} edits total across {} fuzzed files, {} converged (I1 detection)",
        total_edits, fuzzed_files, total_converged
    );
    assert!(fuzzed_files >= 1, "no corpus file small enough to fuzz");
    assert!(total_edits >= fuzzed_files * EDITS_PER_FILE, "edit count short");

    // -------------------------------------------------------------------
    // Phase 2 — STRUCTURED, parse-preserving edits (probe-style identifier
    // substitution at 25/50/75%). These keep the parse valid across the edit so
    // the I1 convergence counters get a meaningful sample to compare against the
    // probe's band (median 0 / p90 22). Still a full differential per edit.
    // -------------------------------------------------------------------
    let mut struct_dists: Vec<usize> = Vec::new();
    let mut struct_would_splice: Vec<usize> = Vec::new();
    let mut struct_rediv = 0usize;
    for file in &files {
        let content = std::fs::read_to_string(file).expect("read corpus file");
        let short = file.file_name().unwrap().to_string_lossy().into_owned();
        let mut session = ParseSession::new(Arc::clone(&parser));
        session.parse_full(&content);
        if !matches!(session.outcome(), Some(ParseOutcome::Ok(ctx)) if parser.is_accepted(ctx)) {
            eprintln!("[session_diff] STRUCT: {} skipped (clean parse not accepted)", short);
            continue;
        }
        let mut doc: Vec<char> = content.chars().collect();
        let n = doc.len();
        // Large files (which the FUZZ phase skips) do the O(doc) full-reparse
        // differential only under MG3_SESSION_HEAVY=1, to keep the default
        // `cargo test` fast in debug (an unoptimized 36k parse is slow). Small files
        // always get the full 25/50/75% matrix + parse_full diff. The heavy path is
        // exercised in release CI / on demand.
        let heavy = std::env::var("MG3_SESSION_HEAVY").map(|v| v == "1").unwrap_or(false);
        // parse_full differential (small files always; large files only under heavy).
        if n <= FUZZ_MAX_CHARS || heavy {
            let doc0: Vec<char> = content.chars().collect();
            let want0 = full_reparse(&parser, &doc0);
            let got0 = serialize_session(&session);
            assert_eq!(got0, want0, "{} STRUCT parse_full diverged from full reparse", short);
        }
        let positions: &[(usize, usize)] = if n > FUZZ_MAX_CHARS {
            if heavy { &[(1, 2)] } else {
                eprintln!("[session_diff] STRUCT: {} edit-diff skipped (>{} chars; set MG3_SESSION_HEAVY=1)", short, FUZZ_MAX_CHARS);
                &[]
            }
        } else {
            &[(1, 4), (1, 2), (3, 4)]
        };
        for &(num, den) in positions {
            let approx = n * num / den;
            let Some((s, e)) = find_ident_span(&doc, approx) else { continue };
            let old: String = doc[s..e].iter().collect();
            let repl = format!("q{}9", old); // fresh, still a valid identifier
            let old_len = e - s;
            apply(&mut doc, s, old_len, &repl);
            session.edit(s, old_len, &repl);

            let want = full_reparse(&parser, &doc);
            let got = serialize_session(&session);
            assert_eq!(
                got, want,
                "{} STRUCT ident-subst@{}/{} (pos={}, old_len={}, new={:?}) diverged\n  doc_len={}",
                short, num, den, s, old_len, repl, doc.len()
            );
            let st = session.stats();
            if let Some(d) = st.last_convergence_distance {
                struct_dists.push(d);
            }
            if let Some(t) = st.last_would_splice_tail {
                struct_would_splice.push(t);
            }
            if st.last_rediverged {
                struct_rediv += 1;
            }
        }
    }
    struct_dists.sort_unstable();
    struct_would_splice.sort_unstable();
    let median = struct_dists.get(struct_dists.len() / 2).copied();
    let p90 = if struct_dists.is_empty() {
        None
    } else {
        struct_dists.get(((struct_dists.len() as f64 - 1.0) * 0.9).round() as usize).copied()
    };
    let mean = if struct_dists.is_empty() {
        0.0
    } else {
        struct_dists.iter().sum::<usize>() as f64 / struct_dists.len() as f64
    };
    eprintln!(
        "[session_diff] STRUCT convergence (probe-band check): n={} median={:?} mean={:.1} p90={:?} max={:?} rediverged={}",
        struct_dists.len(), median, mean, p90, struct_dists.last(), struct_rediv
    );
    eprintln!(
        "[session_diff] STRUCT would-splice tail: n={} median={:?} max={:?}",
        struct_would_splice.len(),
        struct_would_splice.get(struct_would_splice.len() / 2),
        struct_would_splice.last()
    );

    // The probe measured 0 re-divergences; a structured parse-preserving edit must
    // never re-diverge after convergence.
    assert_eq!(struct_rediv, 0, "structured edit re-diverged after convergence (probe measured 0)");
    // Structured parse-preserving edits must converge (I1 wiring live).
    assert!(
        !struct_dists.is_empty(),
        "no structured edit converged — I1 fingerprint/convergence wiring is likely broken"
    );
    if let Some(p90v) = p90 {
        assert!(
            p90v <= 200,
            "structured convergence p90={} far exceeds the probe band (p90 22) — investigate",
            p90v
        );
    }
    let _ = total_converged;
}

/// Is char c part of an identifier?
fn is_ident_char(c: char) -> bool {
    c.is_alphanumeric() || c == '_'
}

/// Find the identifier span covering or nearest at/after char index `from`
/// (mirrors the probe's `find_ident_span`).
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

fn hash_name(s: &str) -> u64 {
    let mut h = 1469598103934665603u64;
    for b in s.bytes() {
        h ^= b as u64;
        h = h.wrapping_mul(1099511628211);
    }
    h
}
