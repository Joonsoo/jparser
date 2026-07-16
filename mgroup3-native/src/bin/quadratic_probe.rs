//! THROWAWAY MEASUREMENT PROBE — is `ParseSession::edit` O(n) per edit (=> O(n^2)
//! to type a whole file), even when the actual re-parse work is O(1)?
//!
//! Read-only: it only *drives* the session's public API and *replicates* the
//! individual clone/rebase operations on the session's own live data to attribute
//! cost. No library source is modified.
//!
//! THE EXPERIMENT
//!   For a sweep of document lengths n, build a synthetic-but-real-grammar
//!   document, `parse_full` it, then time a SINGLE small edit (insert one char
//!   inside a comment — the most trivially-reusable edit there is):
//!     * near the END   — prefix-resume should re-parse only the short tail.
//!     * near the START — the splice should reuse almost the whole suffix.
//!   In both cases the PARSING work (reparsed_gens) is ~constant in n. If wall
//!   time still grows linearly in n, the growth is pure copy overhead.
//!
//! ATTRIBUTION (replicated on the session's own final ctx, same sizes):
//!   * `finish_parse`  session.rs:464   `Rc::new(ctx.clone())`        — deep, incl. history
//!   * `reparse_*`     session.rs:530/735 `ctx.history.clone()`       — deep
//!   * `finish_splice` session.rs:784   `spliced.clone()`             — deep
//!   * `restore_ctx`   session.rs:496   `canonical_history[0..=at].to_vec()`
//!   * `finish_splice` session.rs:772   `rebase.entry(..)` per entry  — BY DESIGN
//!   * `rebase.ctx`                                                   — BY DESIGN, O(state)
//!   * `Checkpoint::snapshot` session.rs:477 (history-less ctx clone) — confirm cheap
//!   plus the session's own `totals().splice_rebase_nanos` (the real finish_splice cost).
//!
//! Usage:
//!   quadratic_probe <parserdata.pb|.pb.gz> [--sizes 1,2,4,8,16] [--reps 7]
//! `--sizes` are in units of ~1000 chars.
//!
//! MG3_SESSION_VERIFY MUST be unset/0 (it full-re-parses every edit).

use std::hint::black_box;
use std::sync::Arc;
use std::time::{Duration, Instant};

use mgroup3_native::history::History;
use mgroup3_native::parser::Mgroup3Parser;
use mgroup3_native::parsing_ctx::ParsingCtx;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use mgroup3_native::rebase::GenRebase;
use mgroup3_native::session::{ParseOutcome, ParseSession, SessionStats};
use prost::Message;

// ---------------------------------------------------------------------------
// Loading
// ---------------------------------------------------------------------------

fn load_parser(path: &str) -> Result<Mgroup3Parser, Box<dyn std::error::Error>> {
    let raw = std::fs::read(path)?;
    let bytes = if path.ends_with(".gz") {
        use flate2::read::GzDecoder;
        use std::io::Read;
        let mut buf = Vec::new();
        GzDecoder::new(raw.as_slice()).read_to_end(&mut buf)?;
        buf
    } else {
        raw
    };
    let data = Mgroup3ParserData::decode(bytes.as_slice())?;
    Ok(Mgroup3Parser::new(data))
}

// ---------------------------------------------------------------------------
// Synthetic mulang document of a controlled length
// ---------------------------------------------------------------------------

/// One repeatable top-level block. Contains a line comment we can type into.
fn block(i: usize) -> String {
    format!(
        "// note {i} ....\nclass C{i} {{\n  fieldA{i}: string\n  fieldB{i}: bool\n}}\n\n"
    )
}

/// Build a document of at least `target_chars` chars out of repeated blocks.
/// Returns (text, char index of the interior of the FIRST block's comment,
/// char index of the interior of the LAST block's comment).
fn make_doc(target_chars: usize) -> (String, usize, usize) {
    let header = "namespace probe.gen\n\n".to_string();
    let mut s = header.clone();
    let mut i = 0usize;
    // marker char offsets (in chars) of the "...." inside each block's comment.
    let mut first_mark = 0usize;
    let mut last_mark = 0usize;
    while s.chars().count() < target_chars {
        let b = block(i);
        let base = s.chars().count();
        // "// note {i} ....\n" — put the cursor just before the final '.'
        let comment_dot = b.chars().position(|c| c == '.').expect("dot in comment");
        let mark = base + comment_dot + 2; // inside the "...." run
        if i == 0 {
            first_mark = mark;
        }
        last_mark = mark;
        s.push_str(&b);
        i += 1;
    }
    (s, first_mark, last_mark)
}

// ---------------------------------------------------------------------------
// Timing helpers
// ---------------------------------------------------------------------------

fn median(mut v: Vec<Duration>) -> Duration {
    v.sort_unstable();
    v[v.len() / 2]
}

fn ms(d: Duration) -> f64 {
    d.as_secs_f64() * 1000.0
}

/// Run `reps` single-char inserts at (moving) position `pos_of(k)`, timing each
/// edit in isolation. Returns (per-edit durations, per-edit stats, per-edit
/// splice_rebase_nanos delta).
fn drive_edits(
    session: &mut ParseSession,
    pos: usize,
    reps: usize,
) -> (Vec<Duration>, Vec<SessionStats>, Vec<u64>) {
    let mut times = Vec::new();
    let mut stats = Vec::new();
    let mut rebase_ns = Vec::new();
    for k in 0..reps {
        let before = session.totals().splice_rebase_nanos;
        let t0 = Instant::now();
        let outcome = session.edit(pos + k, 0, "Z");
        let ok = matches!(outcome, ParseOutcome::Ok(_));
        let dt = t0.elapsed();
        assert!(ok, "edit {k} at pos {pos} errored");
        times.push(dt);
        stats.push(session.stats());
        rebase_ns.push(session.totals().splice_rebase_nanos - before);
    }
    (times, stats, rebase_ns)
}

// ---------------------------------------------------------------------------
// Attribution: replicate each suspect operation on the session's own final ctx
// ---------------------------------------------------------------------------

struct Attrib {
    hist_len: usize,
    /// session.rs:464 — `Rc::new(ctx.clone())` in finish_parse (deep, incl history).
    ctx_clone: Duration,
    /// session.rs:477 — `Checkpoint::snapshot` = ctx.clone() then history dropped.
    /// Modeled as cloning a ctx whose history is already empty.
    snapshot_clone: Duration,
    /// session.rs:530 / :735 / :784 — `history.clone()` (deep).
    hist_clone: Duration,
    /// session.rs:496 — `canonical_history[0..=at].to_vec()` for at = resume gen.
    hist_prefix_clone: Duration,
    /// session.rs:772 — `rebase.entry(..)` over the whole history (BY DESIGN).
    rebase_entries: Duration,
    /// rebase.ctx over the final live state (BY DESIGN, O(state)).
    rebase_ctx: Duration,
    /// One parse_step at the tail of the document (the unit of real parse work).
    parse_step_each: Duration,
}

fn attribute(parser: &Mgroup3Parser, ctx: &ParsingCtx, resume_gen: usize, doc: &[char]) -> Attrib {
    let hist_len = ctx.history.len();
    let rebase = GenRebase::new((doc.len() / 2) as i32, 1);

    // --- deep ctx clone (finish_parse:464) ---
    let t = Instant::now();
    for _ in 0..3 {
        black_box(ctx.clone());
    }
    let ctx_clone = t.elapsed() / 3;

    // --- history-less ctx clone (snapshot via clone_without_history) ---
    let t = Instant::now();
    for _ in 0..3 {
        black_box(ctx.clone_without_history());
    }
    let snapshot_clone = t.elapsed() / 3;

    // --- canonical history retain: seal-then-clone (Rc bump, :531/:740/:786) ---
    let t = Instant::now();
    for _ in 0..3 {
        let mut h = ctx.history.clone();
        h.seal();
        black_box(h.clone());
    }
    let hist_clone = t.elapsed() / 3;

    // --- history prefix share (restore_ctx: canonical.prefix(at+1)) ---
    let at = resume_gen.min(hist_len.saturating_sub(1));
    let t = Instant::now();
    for _ in 0..3 {
        black_box(ctx.history.prefix(at + 1));
    }
    let hist_prefix_clone = t.elapsed() / 3;

    // --- rebase.entry over the whole history (finish_splice:772, BY DESIGN) ---
    let t = Instant::now();
    for _ in 0..3 {
        let v: Vec<_> = ctx.history.iter().map(|e| rebase.entry(e)).collect();
        black_box(v);
    }
    let rebase_entries = t.elapsed() / 3;

    // --- rebase.ctx over live state (BY DESIGN, O(state)) ---
    let t = Instant::now();
    for _ in 0..3 {
        black_box(rebase.ctx(ctx, History::new()));
    }
    let rebase_ctx = t.elapsed() / 3;

    // --- one parse_step (real parse work unit) ---
    // Re-derive a ctx a few gens short of the end and step it.
    let steps = 16usize.min(doc.len());
    let mut probe_ctx = parser.init_ctx();
    let total = doc.len();
    for (idx, &c) in doc.iter().enumerate().take(total - steps) {
        probe_ctx = parser.parse_step(probe_ctx, c, idx + 1 == total).expect("probe step");
    }
    let t = Instant::now();
    for idx in (total - steps)..total {
        probe_ctx = parser.parse_step(probe_ctx, doc[idx], idx + 1 == total).expect("probe step");
    }
    let parse_step_each = t.elapsed() / steps as u32;
    black_box(probe_ctx);

    Attrib {
        hist_len,
        ctx_clone,
        snapshot_clone,
        hist_clone,
        hist_prefix_clone,
        rebase_entries,
        rebase_ctx,
        parse_step_each,
    }
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

fn main() -> Result<(), Box<dyn std::error::Error>> {
    if std::env::var("MG3_SESSION_VERIFY").map(|v| v == "1").unwrap_or(false) {
        eprintln!("*** MG3_SESSION_VERIFY=1 — every edit does a full re-parse. UNSET IT. ***");
        std::process::exit(2);
    }

    let raw: Vec<String> = std::env::args().skip(1).collect();
    let mut sizes: Vec<usize> = vec![1, 2, 4, 8, 16, 32];
    let mut reps = 7usize;
    let mut it = raw.iter().peekable();
    let mut data_path: Option<String> = None;
    while let Some(a) = it.next() {
        match a.as_str() {
            "--sizes" => {
                if let Some(v) = it.next() {
                    sizes = v.split(',').filter_map(|s| s.trim().parse().ok()).collect();
                }
            }
            "--reps" => {
                if let Some(v) = it.next() {
                    reps = v.parse().unwrap_or(7);
                }
            }
            s if !s.starts_with("--") && data_path.is_none() => data_path = Some(s.to_string()),
            _ => {}
        }
    }
    let data_path = data_path.expect("usage: quadratic_probe <parserdata.pb[.gz]> [--sizes k,..] [--reps N]");

    eprintln!("[load] {}", data_path);
    let t0 = Instant::now();
    let parser = Arc::new(load_parser(&data_path)?);
    eprintln!("[load] ready in {:?}", t0.elapsed());

    println!("=== SETUP ===");
    println!("  checkpoint interval K = {}", mgroup3_native::session::DEFAULT_CHECKPOINT_INTERVAL);
    println!("  reps per cell = {} (median reported)", reps);
    println!("  MG3_SESSION_VERIFY = off");
    println!();

    struct Row {
        n: usize,
        full_ms: f64,
        end_ms: f64,
        end_stats: SessionStats,
        end_rebase_ms: f64,
        start_ms: f64,
        start_stats: SessionStats,
        start_rebase_ms: f64,
        checkpoints: usize,
        attrib: Attrib,
    }
    let mut rows: Vec<Row> = Vec::new();

    for &kilo in &sizes {
        let target = kilo * 1000;
        let (text, first_mark, last_mark) = make_doc(target);
        let n = text.chars().count();

        // ---- baseline full parse ----
        let mut s_end = ParseSession::new(Arc::clone(&parser));
        let t = Instant::now();
        let ok = matches!(s_end.parse_full(&text), ParseOutcome::Ok(_));
        let full_ms = ms(t.elapsed());
        if !ok {
            println!("n={n}: FULL PARSE ERRORED — doc generator produces invalid mulang");
            continue;
        }
        let accepted = s_end.is_accepted();
        if !accepted {
            eprintln!("[warn] n={n}: parses without error but is_accepted=false");
        }

        // ---- edit near the END ----
        let (end_times, end_stats, end_rebase) = drive_edits(&mut s_end, last_mark, reps);

        // ---- edit near the START (fresh session) ----
        let mut s_start = ParseSession::new(Arc::clone(&parser));
        assert!(matches!(s_start.parse_full(&text), ParseOutcome::Ok(_)));
        let (start_times, start_stats, start_rebase) = drive_edits(&mut s_start, first_mark, reps);

        // ---- attribution on the END session's final state ----
        let doc: Vec<char> = s_end.document().to_vec();
        let ctx = match s_end.outcome() {
            Some(ParseOutcome::Ok(c)) => c,
            _ => unreachable!(),
        };
        let resume_gen = s_end.stats().last_resume_gen;
        let attrib = attribute(&parser, ctx, resume_gen, &doc);
        let checkpoints = s_end.checkpoint_count();

        let mid = end_stats.len() / 2;
        rows.push(Row {
            n,
            full_ms,
            end_ms: ms(median(end_times)),
            end_stats: end_stats[mid],
            end_rebase_ms: end_rebase[mid] as f64 / 1e6,
            start_ms: ms(median(start_times)),
            start_stats: start_stats[mid],
            start_rebase_ms: start_rebase[mid] as f64 / 1e6,
            checkpoints,
            attrib,
        });
        eprintln!("[done] n={n}");
    }

    // -----------------------------------------------------------------------
    println!("=== PER-EDIT WALL TIME vs n (insert 1 char in a comment) ===");
    println!("  reparse = last_reparsed_gens (the ACTUAL parsing work). Constant => any time");
    println!("  growth is copy overhead, not parsing.");
    println!(
        "{:>7} {:>9} | {:>9} {:>8} {:>7} {:>8} {:>8} | {:>9} {:>8} {:>7} {:>8} {:>8}",
        "n", "full_ms", "END_ms", "reparse", "resume", "spliced", "spl_gens", "START_ms", "reparse",
        "resume", "spliced", "spl_gens"
    );
    for r in &rows {
        println!(
            "{:>7} {:>9.1} | {:>9.3} {:>8} {:>7} {:>8} {:>8} | {:>9.3} {:>8} {:>7} {:>8} {:>8}",
            r.n,
            r.full_ms,
            r.end_ms,
            r.end_stats.last_reparsed_gens,
            r.end_stats.last_resume_gen,
            if r.end_stats.last_spliced { "yes" } else { "-" },
            r.end_stats.last_spliced_gens,
            r.start_ms,
            r.start_stats.last_reparsed_gens,
            r.start_stats.last_resume_gen,
            if r.start_stats.last_spliced { "yes" } else { "-" },
            r.start_stats.last_spliced_gens,
        );
    }
    println!();

    // scaling ratios
    println!("=== SCALING (ratio vs the smallest n) ===");
    if let Some(base) = rows.first() {
        println!("{:>7} {:>8} {:>10} {:>10} {:>12}", "n", "n/n0", "END x", "START x", "reparsed_end");
        for r in &rows {
            println!(
                "{:>7} {:>8.2} {:>10.2} {:>10.2} {:>12}",
                r.n,
                r.n as f64 / base.n as f64,
                r.end_ms / base.end_ms,
                r.start_ms / base.start_ms,
                r.end_stats.last_reparsed_gens,
            );
        }
    }
    println!();

    println!("=== COST ATTRIBUTION (measured by replicating each op on the session's own ctx) ===");
    println!(
        "{:>7} {:>8} | {:>9} {:>9} {:>9} {:>9} | {:>9} {:>9} | {:>9}",
        "n", "hist", "ctx.clone", "hist.clon", "hist[..at]", "snapshot", "rebase_e", "rebase_ctx",
        "step_each"
    );
    println!(
        "{:>7} {:>8} | {:>9} {:>9} {:>9} {:>9} | {:>9} {:>9} | {:>9}",
        "", "", "s.rs:464", "s.rs:735", "s.rs:496", "s.rs:477", "s.rs:772", "(design)", "us"
    );
    for r in &rows {
        let a = &r.attrib;
        println!(
            "{:>7} {:>8} | {:>9.3} {:>9.3} {:>9.3} {:>9.3} | {:>9.3} {:>9.3} | {:>9.3}",
            r.n,
            a.hist_len,
            ms(a.ctx_clone),
            ms(a.hist_clone),
            ms(a.hist_prefix_clone),
            ms(a.snapshot_clone),
            ms(a.rebase_entries),
            ms(a.rebase_ctx),
            a.parse_step_each.as_secs_f64() * 1e6,
        );
    }
    println!("  (all ms, median of 3)");
    println!();

    // NOTE: both END and START edits take the SPLICE path (see `spliced` above).
    // END: q* lands ~56 gens from the end, so finish_splice's rebase loop is tiny,
    //      but restore_ctx cloned a near-full history prefix (resume_gen ~ n).
    // START: q* lands right after the edit, so finish_splice rebases ~n entries
    //      (BY DESIGN per the design doc) — but still clones on top of that.
    println!("=== END-EDIT BUDGET (splice at q*~n; restore_ctx prefix is the O(n) part) ===");
    println!(
        "{:>7} {:>9} {:>10} | {:>9} {:>10} {:>10} {:>10} | {:>9} {:>7}",
        "n", "END_ms", "splice_ms", "steps", "hist[0..at]", "spliced.cl", "ctx.clone", "modeled",
        "cover%"
    );
    println!("                       |   (real)   s.rs:496    s.rs:784    s.rs:464");
    for r in &rows {
        let a = &r.attrib;
        let steps = a.parse_step_each.as_secs_f64() * 1000.0 * r.end_stats.last_reparsed_gens as f64;
        let modeled = steps + ms(a.hist_prefix_clone) + ms(a.hist_clone) + ms(a.ctx_clone);
        println!(
            "{:>7} {:>9.3} {:>10.3} | {:>9.3} {:>10.3} {:>10.3} {:>10.3} | {:>9.3} {:>6.0}%",
            r.n,
            r.end_ms,
            r.end_rebase_ms,
            steps,
            ms(a.hist_prefix_clone),
            ms(a.hist_clone),
            ms(a.ctx_clone),
            modeled,
            100.0 * modeled / r.end_ms
        );
    }
    println!("  => real parsing work share at the largest n:");
    if let Some(r) = rows.last() {
        let steps = r.attrib.parse_step_each.as_secs_f64() * 1000.0
            * r.end_stats.last_reparsed_gens as f64;
        println!("     steps {:.3} ms / {:.3} ms = {:.2}%", steps, r.end_ms, 100.0 * steps / r.end_ms);
    }
    println!();

    println!("=== START-EDIT BUDGET (splice path; finish_splice rebases ~n entries) ===");
    println!("  splice_ms = the session's OWN totals().splice_rebase_nanos for that edit, i.e.");
    println!("  the real measured finish_splice cost = rebase_e[DESIGN] + spliced.clone()[REDUNDANT]");
    println!("                                        + rebase_ctx[DESIGN].");
    println!(
        "{:>7} {:>9} {:>10} | {:>9} {:>10} {:>10} {:>10} | {:>10} {:>7}",
        "n", "START_ms", "splice_ms", "rebase_e", "spliced.cl", "rebase_ctx", "ctx.clone",
        "redundant", "redun%"
    );
    println!("                       |  s.rs:772   s.rs:784    (design)    s.rs:464");
    for r in &rows {
        let a = &r.attrib;
        // redundant = spliced.clone() (:784) + finish_parse's ctx.clone() (:464)
        let redundant = ms(a.hist_clone) + ms(a.ctx_clone);
        println!(
            "{:>7} {:>9.3} {:>10.3} | {:>9.3} {:>10.3} {:>10.3} {:>10.3} | {:>10.3} {:>6.0}%",
            r.n,
            r.start_ms,
            r.start_rebase_ms,
            ms(a.rebase_entries),
            ms(a.hist_clone),
            ms(a.rebase_ctx),
            ms(a.ctx_clone),
            redundant,
            100.0 * redundant / r.start_ms,
        );
    }
    println!();

    println!("=== CHECKPOINT CHECK (is the history-marker optimization holding? s.rs:72-78) ===");
    println!(
        "{:>7} {:>12} {:>14} {:>14}",
        "n", "cps", "snapshot_ms", "vs ctx.clone"
    );
    for r in &rows {
        println!(
            "{:>7} {:>12} {:>14.4} {:>13.0}x",
            r.n,
            r.checkpoints,
            ms(r.attrib.snapshot_clone),
            ms(r.attrib.ctx_clone) / ms(r.attrib.snapshot_clone).max(1e-9),
        );
    }
    println!("  snapshot = history-less ctx clone. Cheap & ~flat => checkpoints are NOT the problem.");
    println!();

    println!("=== TYPING A WHOLE FILE (extrapolated) ===");
    println!("  sum over k=1..n of per-edit(k) ~= n * mean(per-edit). If per-edit is linear in n,");
    println!("  this is O(n^2). Reported: per-edit-ms / n (should be CONSTANT if per-edit is O(1)).");
    println!("{:>7} {:>14} {:>14}", "n", "END_ms/n(*1e3)", "START_ms/n(*1e3)");
    for r in &rows {
        println!(
            "{:>7} {:>14.4} {:>14.4}",
            r.n,
            1000.0 * r.end_ms / r.n as f64,
            1000.0 * r.start_ms / r.n as f64
        );
    }

    Ok(())
}
