//! Incremental parsing session — an ADDITIVE layer over the unmodified parser.
//!
//! `ParseSession` keeps a document (as `Vec<char>`) and a ring of periodic
//! parse-state checkpoints so that an `edit` can resume parsing from the latest
//! checkpoint at or before the edit position instead of re-parsing from gen 0
//! (Phase I0 — prefix resume). The parser core is untouched: the session only
//! *calls* `parse_step` / `is_accepted` / `kernels_history`, never modifies them.
//!
//! CORRECTNESS CONTRACT: after any `parse_full`/`edit`, the session's
//! `kernels_history()` and `is_accepted()` are byte-identical to a full re-parse
//! of the current document text. Enforced by `tests/session_diff.rs` (seeded
//! fuzzing differential oracle) and, at runtime, by the `MG3_SESSION_VERIFY`
//! self-check.
//!
//! I1 adds per-gen STRICT fingerprints (`fingerprint.rs`) and convergence
//! detection: each `edit` re-parse is compared, gen by gen, to the previous
//! parse's fingerprint sequence to find the convergence gen q* — WITHOUT splicing
//! (Phase I2). The counters (`SessionStats`) measure the reuse headroom
//! (would-splice tail) a future splice would realize. Fingerprints of BOTH the
//! old and new document are computed under the SAME edit anchor `p` (== the edit
//! char index), exactly as the probe does, so the session's convergence numbers
//! reproduce the probe's band.
//!
//! Threading: a session is a single-document, single-owner object. The parser
//! handle is shared as `Arc<Mgroup3Parser>`.

use std::sync::Arc;

use crate::fingerprint::{fp_state, Fp, Variant};
use crate::parser::{Mgroup3Parser, ParsingError};
use crate::parsing_ctx::{KtlibKernel, ParsingCtx};
use rustc_hash::FxHashSet as HashSet;

/// Default checkpoint interval (gens). See design §1.2 — K/2 rewind ≈ the p90
/// convergence distance, so the rewind cost is on par with the convergence tail.
pub const DEFAULT_CHECKPOINT_INTERVAL: usize = 64;

/// A periodic parse-state checkpoint at a gen boundary. `ctx.gen_idx == at_gen`;
/// the state depends only on document chars with index `< at_gen`, so it is valid
/// to resume from while those chars are unchanged.
///
/// HISTORY-MARKER optimization (design §1.3): the stored `ctx` has its `history`
/// EMPTIED. Cloning the full `history: Vec<HistoryEntry>` into every checkpoint
/// would be O(N) per checkpoint → O(N²/K) for a full parse (the dominant cost, and
/// severe on real LSP-sized files). Instead the checkpoint keeps only the gen
/// marker (`at_gen`); on resume the session reconstructs the required history
/// prefix `[0..=at_gen]` from its retained `canonical_history` (one O(at_gen) clone
/// per edit, not per checkpoint). The `Rc`/`Arc`-shared path structure in `ctx`
/// is only ref-counted, so a history-less checkpoint clone is cheap.
#[derive(Clone)]
struct Checkpoint {
    at_gen: usize,
    /// ctx snapshot with `history` emptied (see above). `history` is refilled from
    /// `canonical_history` on resume.
    ctx: ParsingCtx,
}

/// Result of a parse (full or incremental). Mirrors what a full re-parse would
/// produce, so a consumer sees the same thing whether or not the session was used.
pub enum ParseOutcome {
    /// Parse reached end of input. Holds the final ctx for lazy result access.
    Ok(ParsingCtx),
    /// Parse errored at some gen (same error `Mgroup3Parser::parse` would return).
    Err(ParsingError),
}

/// Convergence / reuse counters exposed after each `edit` (Phase I1 — detection
/// only, no splice). Distances are in gens (== chars).
#[derive(Clone, Copy, Debug, Default)]
pub struct SessionStats {
    /// Most recent edit's convergence distance q* - edit_end (chars past the edit
    /// end before the edited parse's strict state re-aligned with the previous
    /// parse's, shift-adjusted). `None` = never converged this edit (or no
    /// previous parse to compare against, or the parse errored).
    pub last_convergence_distance: Option<usize>,
    /// Convergence gen q* in the edited parse, if any.
    pub last_convergence_gen: Option<usize>,
    /// "Would-splice" tail: gens after q* that were re-parsed but *could* have been
    /// spliced from the previous parse. The reuse headroom a future Phase I2 splice
    /// would recover.
    pub last_would_splice_tail: Option<usize>,
    /// Gens actually re-parsed this edit (from the resume checkpoint to end).
    pub last_reparsed_gens: usize,
    /// Gen of the checkpoint the edit resumed from (rewind target).
    pub last_resume_gen: usize,
    /// `true` if convergence was detected but a later gen diverged again (the probe
    /// measured 0 of these; a nonzero here is a finding to report).
    pub last_rediverged: bool,
    /// `true` if there was a previous successful parse to compare against.
    pub last_had_baseline: bool,
}

/// Aggregate counters over the session lifetime (for bench/reporting).
#[derive(Clone, Copy, Debug, Default)]
pub struct SessionTotals {
    pub edits: u64,
    /// Edits with a previous parse to compare against (denominator for convergence).
    pub comparable_edits: u64,
    pub converged: u64,
    pub never_converged: u64,
    pub rediverged: u64,
    pub sum_convergence_distance: u64,
    pub sum_would_splice_tail: u64,
    pub sum_reparsed_gens: u64,
}

pub struct ParseSession {
    parser: Arc<Mgroup3Parser>,
    /// Current document text.
    doc: Vec<char>,
    /// Previous document text (the parse the next edit compares against). Empty
    /// until the first successful parse.
    prev_doc: Vec<char>,
    /// Whether `prev_doc` reflects a successful parse we can use as a convergence
    /// baseline.
    have_baseline: bool,
    /// Periodic checkpoints (history-less), strictly increasing in `at_gen`. gen 0
    /// (init_ctx) is always present after a successful parse.
    checkpoints: Vec<Checkpoint>,
    /// Full history of the last successful parse — the source for reconstructing a
    /// checkpoint's history prefix on resume (see `Checkpoint`). Empty if the last
    /// parse errored.
    canonical_history: Vec<crate::parsing_ctx::HistoryEntry>,
    /// Checkpoint interval in gens.
    interval: usize,
    /// Final outcome of the last parse.
    outcome: Option<ParseOutcome>,
    stats: SessionStats,
    totals: SessionTotals,
    /// If true, every edit is cross-checked against a full re-parse and any
    /// mismatch aborts loudly (design §2.5). Enabled by `MG3_SESSION_VERIFY=1`.
    verify: bool,
}

impl ParseSession {
    pub fn new(parser: Arc<Mgroup3Parser>) -> Self {
        Self::with_interval(parser, DEFAULT_CHECKPOINT_INTERVAL)
    }

    pub fn with_interval(parser: Arc<Mgroup3Parser>, interval: usize) -> Self {
        let interval = interval.max(1);
        let verify = std::env::var("MG3_SESSION_VERIFY").map(|v| v == "1").unwrap_or(false);
        Self {
            parser,
            doc: Vec::new(),
            prev_doc: Vec::new(),
            have_baseline: false,
            checkpoints: Vec::new(),
            canonical_history: Vec::new(),
            interval,
            outcome: None,
            stats: SessionStats::default(),
            totals: SessionTotals::default(),
            verify,
        }
    }

    pub fn checkpoint_interval(&self) -> usize {
        self.interval
    }

    pub fn stats(&self) -> SessionStats {
        self.stats
    }

    /// Alias for `stats()` (design names the accessor `session_stats`).
    pub fn session_stats(&self) -> SessionStats {
        self.stats
    }

    pub fn totals(&self) -> SessionTotals {
        self.totals
    }

    /// Number of checkpoints currently held (memory probe).
    pub fn checkpoint_count(&self) -> usize {
        self.checkpoints.len()
    }

    /// Rough resident bytes of all checkpoint histories (the dominant cost — see
    /// design §1.3). Counts each snapshot's `HistoryEntry` vec backbone
    /// (`len * size_of::<HistoryEntry>()`); the `Rc`-shared path structure is NOT
    /// counted (only ref-counted, shared with the live ctx). An order-of-magnitude
    /// figure for reporting, not allocator-exact.
    ///
    /// With the history-marker optimization, checkpoints hold NO history, so their
    /// heavy cost is gone; the retained `canonical_history` (one full history) is
    /// the session's dominant resident structure and is what this reports.
    pub fn approx_checkpoint_bytes(&self) -> usize {
        let per_entry = std::mem::size_of::<crate::parsing_ctx::HistoryEntry>();
        // checkpoints carry emptied history; the canonical history is the real cost.
        self.canonical_history.len() * per_entry
    }

    /// Number of `HistoryEntry` values in the retained canonical history.
    pub fn canonical_history_len(&self) -> usize {
        self.canonical_history.len()
    }

    pub fn outcome(&self) -> Option<&ParseOutcome> {
        self.outcome.as_ref()
    }

    pub fn is_accepted(&self) -> bool {
        match &self.outcome {
            Some(ParseOutcome::Ok(ctx)) => self.parser.is_accepted(ctx),
            _ => false,
        }
    }

    pub fn kernels_history(&self) -> Option<Vec<HashSet<KtlibKernel>>> {
        match &self.outcome {
            Some(ParseOutcome::Ok(ctx)) => Some(self.parser.kernels_history(ctx)),
            _ => None,
        }
    }

    pub fn error(&self) -> Option<&ParsingError> {
        match &self.outcome {
            Some(ParseOutcome::Err(e)) => Some(e),
            _ => None,
        }
    }

    /// Current document text (chars).
    pub fn document(&self) -> &[char] {
        &self.doc
    }

    /// Full parse from gen 0. Establishes the checkpoint ring and the baseline.
    pub fn parse_full(&mut self, text: &str) -> &ParseOutcome {
        self.doc = text.chars().collect();
        self.stats = SessionStats::default();
        let ok = self.reparse_from_scratch();
        self.finish_parse(ok);
        self.outcome.as_ref().expect("outcome set")
    }

    /// Apply an edit and re-parse incrementally (Phase I0 prefix resume + I1
    /// convergence detection). `pos`/`old_len`/`new_text` are char (code point)
    /// units — the same model as the probe's `Edit`.
    pub fn edit(&mut self, pos: usize, old_len: usize, new_text: &str) -> &ParseOutcome {
        let new_chars: Vec<char> = new_text.chars().collect();
        let pos = pos.min(self.doc.len());
        let end = (pos + old_len).min(self.doc.len());
        let old_len_chars = end - pos;
        let delta = new_chars.len() as i32 - old_len_chars as i32;
        let edit_end = pos + new_chars.len();
        let anchor = pos as i32; // fingerprint anchor p (gen boundary)

        // The baseline (old) document to compare against.
        let baseline_doc = std::mem::take(&mut self.prev_doc);
        let had_baseline = self.have_baseline;

        // Apply the edit to the document.
        let mut new_doc: Vec<char> =
            Vec::with_capacity(self.doc.len() - old_len_chars + new_chars.len());
        new_doc.extend_from_slice(&self.doc[..pos]);
        new_doc.extend_from_slice(&new_chars);
        new_doc.extend_from_slice(&self.doc[end..]);
        self.doc = new_doc;

        self.stats = SessionStats::default();
        self.stats.last_had_baseline = had_baseline;

        // Compute the baseline (A) per-gen strict fingerprints under THIS anchor,
        // exactly as the probe: a full fingerprint trace of the old document. Only
        // needed when we have a baseline (skip on the first edit after an errored /
        // absent parse).
        let baseline_fp: Option<Vec<Fp>> = if had_baseline {
            Some(self.fingerprint_trace(&baseline_doc, anchor))
        } else {
            None
        };

        let ok = self.reparse_incremental(pos, anchor, delta, edit_end, baseline_fp.as_deref());
        self.finish_parse(ok);

        self.totals.edits += 1;
        self.totals.sum_reparsed_gens += self.stats.last_reparsed_gens as u64;
        if self.stats.last_had_baseline && matches!(self.outcome, Some(ParseOutcome::Ok(_))) {
            self.totals.comparable_edits += 1;
            match self.stats.last_convergence_distance {
                Some(d) => {
                    self.totals.converged += 1;
                    self.totals.sum_convergence_distance += d as u64;
                    if let Some(t) = self.stats.last_would_splice_tail {
                        self.totals.sum_would_splice_tail += t as u64;
                    }
                }
                None => self.totals.never_converged += 1,
            }
            if self.stats.last_rediverged {
                self.totals.rediverged += 1;
            }
        }

        if self.verify {
            self.verify_against_full_reparse();
        }

        self.outcome.as_ref().expect("outcome set")
    }

    // -- internals -----------------------------------------------------------

    /// After a parse, record the current document as the next baseline iff the
    /// parse succeeded.
    fn finish_parse(&mut self, ok: bool) {
        if ok {
            self.prev_doc = self.doc.clone();
            self.have_baseline = true;
        } else {
            self.prev_doc.clear();
            self.have_baseline = false;
        }
    }

    /// Make a history-less checkpoint snapshot of `ctx` (design §1.3): clone ctx but
    /// drop its history (the heavy field); the `Rc`/`Arc` path structure is merely
    /// ref-counted, so this is cheap. History is refilled from `canonical_history`
    /// on resume.
    fn snapshot(ctx: &ParsingCtx, at_gen: usize) -> Checkpoint {
        let mut c = ctx.clone();
        c.history = Vec::new();
        Checkpoint { at_gen, ctx: c }
    }

    /// Restore a resumable ctx from a checkpoint: refill its `history` prefix
    /// `[0..=at_gen]` from `canonical_history`. `parse_step` only reads
    /// `history.last()` (verified against the parser core), so the prefix must be
    /// present for the FINAL `kernels_history`/`is_accepted` (which index the whole
    /// history by absolute gen) and its last element must be the entry for `at_gen`.
    fn restore_ctx(&self, mut cp: Checkpoint) -> ParsingCtx {
        let at = cp.at_gen;
        // canonical_history[0..=at] are gens 0..=at (unchanged prefix). If the
        // canonical history is shorter than expected (only when resuming from a
        // fresh init at gen 0 with no baseline), fall back to the checkpoint's own
        // (empty) history — gen 0's init ctx already carries its single entry via
        // init_ctx below.
        if at < self.canonical_history.len() {
            cp.ctx.history = self.canonical_history[0..=at].to_vec();
        }
        cp.ctx
    }

    /// Parse the whole document from gen 0, (re)building the checkpoint ring.
    /// Returns true on success.
    fn reparse_from_scratch(&mut self) -> bool {
        self.checkpoints.clear();
        self.canonical_history.clear();
        let total = self.doc.len();
        let mut ctx = self.parser.init_ctx();
        self.checkpoints.push(Self::snapshot(&ctx, 0));
        self.stats.last_resume_gen = 0;
        let mut reparsed = 0usize;
        for (idx, &c) in self.doc.iter().enumerate() {
            match self.parser.parse_step(ctx, c, idx + 1 == total) {
                Ok(next) => {
                    ctx = next;
                    reparsed += 1;
                    let g = ctx.gen_idx as usize;
                    if g % self.interval == 0 {
                        self.checkpoints.push(Self::snapshot(&ctx, g));
                    }
                }
                Err(e) => {
                    self.stats.last_reparsed_gens = reparsed;
                    self.outcome = Some(ParseOutcome::Err(e));
                    return false;
                }
            }
        }
        self.stats.last_reparsed_gens = reparsed;
        // Retain the full history as the canonical source for checkpoint restore.
        self.canonical_history = ctx.history.clone();
        self.outcome = Some(ParseOutcome::Ok(ctx));
        true
    }

    /// Find the latest checkpoint at gen <= pos (prefix unchanged there).
    fn resume_checkpoint(&self, pos: usize) -> Checkpoint {
        let mut chosen: Option<&Checkpoint> = None;
        for c in &self.checkpoints {
            if c.at_gen <= pos {
                chosen = Some(c);
            } else {
                break;
            }
        }
        match chosen {
            Some(c) => c.clone(),
            None => Checkpoint { at_gen: 0, ctx: self.parser.init_ctx() },
        }
    }

    /// Re-parse from the resume checkpoint to end of the (edited) document,
    /// rebuilding the checkpoint ring beyond the resume point and detecting
    /// convergence against `baseline_fp` (A's per-gen strict fps under `anchor`).
    /// Returns true on success.
    fn reparse_incremental(
        &mut self,
        pos: usize,
        anchor: i32,
        delta: i32,
        edit_end: usize,
        baseline_fp: Option<&[Fp]>,
    ) -> bool {
        let resume = self.resume_checkpoint(pos);
        let resume_gen = resume.at_gen;
        self.stats.last_resume_gen = resume_gen;

        // Drop checkpoints strictly beyond the resume gen (they belong to the old
        // parse); keep [0, resume_gen].
        self.checkpoints.retain(|c| c.at_gen <= resume_gen);
        // Ensure the resume gen is present as the ring tail (it always is, since
        // resume came from the ring or is gen 0 which we re-insert).
        if self.checkpoints.last().map(|c| c.at_gen) != Some(resume_gen) {
            self.checkpoints.retain(|c| c.at_gen < resume_gen);
            self.checkpoints.push(Self::snapshot(&resume.ctx, resume_gen));
        }

        let total = self.doc.len();
        // Rebuild the resumable ctx's history prefix from the canonical history.
        let mut ctx = self.restore_ctx(resume);

        // Convergence detection state (only when we have a baseline).
        let mut convergence_gen: Option<usize> = None;
        let mut rediverged = false;

        // B's strict fp at gen q, compared to A's fp at gen q-delta.
        let baseline_at = |q: usize| -> Option<Fp> {
            let bf = baseline_fp?;
            let pi = q as i64 - delta as i64;
            if pi < 0 {
                return None;
            }
            bf.get(pi as usize).copied()
        };

        let mut reparsed = 0usize;
        for idx in resume_gen..total {
            let c = self.doc[idx];
            match self.parser.parse_step(ctx, c, idx + 1 == total) {
                Ok(next) => {
                    ctx = next;
                    reparsed += 1;
                    let g = ctx.gen_idx as usize;

                    if baseline_fp.is_some() {
                        if convergence_gen.is_none() && g >= edit_end {
                            if let Some(af) = baseline_at(g) {
                                let bf = fp_state(&ctx, anchor, ctx.gen_idx, Variant::Strict);
                                if bf == af {
                                    convergence_gen = Some(g);
                                }
                            }
                        } else if let Some(cg) = convergence_gen {
                            // stability: after q*, later gens should keep matching.
                            if g > cg && !rediverged {
                                if let Some(af) = baseline_at(g) {
                                    let bf = fp_state(&ctx, anchor, ctx.gen_idx, Variant::Strict);
                                    if bf != af {
                                        rediverged = true;
                                    }
                                }
                            }
                        }
                    }

                    if g % self.interval == 0 {
                        self.checkpoints.push(Self::snapshot(&ctx, g));
                    }
                }
                Err(e) => {
                    self.stats.last_reparsed_gens = reparsed;
                    self.outcome = Some(ParseOutcome::Err(e));
                    return false;
                }
            }
        }
        self.stats.last_reparsed_gens = reparsed;

        let gens_ok = total;
        self.stats.last_convergence_gen = convergence_gen;
        self.stats.last_convergence_distance = convergence_gen.map(|q| q - edit_end);
        self.stats.last_would_splice_tail = convergence_gen.map(|q| gens_ok.saturating_sub(q));
        self.stats.last_rediverged = rediverged;
        // Retain the full history as the canonical source for the next edit's
        // checkpoint restore.
        self.canonical_history = ctx.history.clone();
        self.outcome = Some(ParseOutcome::Ok(ctx));
        true
    }

    /// Fingerprint every gen of `chars` under `anchor` (STRICT variant). Drives a
    /// fresh parse over `chars` from `init_ctx` — the same as the probe's
    /// `parse_trace`, restricted to strict. Returns fp[0..=gens_ok]. If the parse
    /// errors, the returned vec ends at the last OK gen.
    fn fingerprint_trace(&self, chars: &[char], anchor: i32) -> Vec<Fp> {
        let total = chars.len();
        let mut ctx = self.parser.init_ctx();
        let mut out: Vec<Fp> = Vec::with_capacity(total + 1);
        out.push(fp_state(&ctx, anchor, ctx.gen_idx, Variant::Strict));
        for (idx, &c) in chars.iter().enumerate() {
            match self.parser.parse_step(ctx, c, idx + 1 == total) {
                Ok(next) => {
                    ctx = next;
                    out.push(fp_state(&ctx, anchor, ctx.gen_idx, Variant::Strict));
                }
                Err(_) => break,
            }
        }
        out
    }

    /// Debug self-check (design §2.5): full re-parse of the current document and a
    /// byte-compare of accept + kernels_history. Panics with a dump on mismatch.
    fn verify_against_full_reparse(&self) {
        let text: String = self.doc.iter().collect();
        let full = self.parser.parse(&text);
        let (full_accept, full_hist) = match full {
            Ok(ctx) => (self.parser.is_accepted(&ctx), Some(self.parser.kernels_history(&ctx))),
            Err(_) => (false, None),
        };
        let sess_accept = self.is_accepted();
        let sess_hist = self.kernels_history();

        let hist_match = match (&sess_hist, &full_hist) {
            (Some(a), Some(b)) => hists_equal(a, b),
            (None, None) => true,
            _ => false,
        };
        let outcome_match = matches!(
            (&self.outcome, &full_hist),
            (Some(ParseOutcome::Ok(_)), Some(_)) | (Some(ParseOutcome::Err(_)), None)
        );
        if sess_accept != full_accept || !hist_match || !outcome_match {
            panic!(
                "MG3_SESSION_VERIFY mismatch: accept sess={} full={}, hist_match={}, outcome_match={}\n  doc_len={}\n  doc_head={:?}",
                sess_accept,
                full_accept,
                hist_match,
                outcome_match,
                self.doc.len(),
                self.doc.iter().take(120).collect::<String>(),
            );
        }
    }
}

fn hists_equal(a: &[HashSet<KtlibKernel>], b: &[HashSet<KtlibKernel>]) -> bool {
    a.len() == b.len() && a.iter().zip(b.iter()).all(|(x, y)| x == y)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
    use prost::Message;
    use std::path::PathBuf;

    /// Load a committed small-grammar fixture parser. `('a' 'b')+`.
    fn nested_repeat_parser() -> Arc<Mgroup3Parser> {
        let p = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .join("tests/fixtures/parser/nested_repeat/data.pb");
        let bytes = std::fs::read(&p).expect("read fixture data.pb");
        let data = Mgroup3ParserData::decode(bytes.as_slice()).expect("decode fixture");
        Arc::new(Mgroup3Parser::new(data))
    }

    fn serialize(parser: &Mgroup3Parser, outcome: &ParseOutcome) -> String {
        match outcome {
            ParseOutcome::Err(_) => "REJECTED".to_string(),
            ParseOutcome::Ok(ctx) => {
                if !parser.is_accepted(ctx) {
                    return "REJECTED".to_string();
                }
                let hist = parser.kernels_history(ctx);
                let mut out = String::from("ACCEPTED");
                for (g, kernels) in hist.iter().enumerate() {
                    let mut ks: Vec<_> = kernels.iter().copied().collect();
                    ks.sort_by_key(|k| (k.symbol_id, k.pointer, k.begin_gen, k.end_gen));
                    out.push_str(&format!("|g{}:", g));
                    for k in ks {
                        out.push_str(&format!("{},{},{},{};", k.symbol_id, k.pointer, k.begin_gen, k.end_gen));
                    }
                }
                out
            }
        }
    }

    fn full(parser: &Mgroup3Parser, text: &str) -> String {
        match parser.parse(text) {
            Ok(ctx) => serialize(parser, &ParseOutcome::Ok(ctx)),
            Err(e) => serialize(parser, &ParseOutcome::Err(e)),
        }
    }

    fn sess_str(parser: &Mgroup3Parser, s: &ParseSession) -> String {
        match s.outcome() {
            Some(o) => serialize(parser, o),
            None => "REJECTED".to_string(),
        }
    }

    #[test]
    fn parse_full_matches_direct_parse() {
        let parser = nested_repeat_parser();
        for text in ["ab", "abab", "ababab", "", "a", "aba"] {
            let mut s = ParseSession::with_interval(Arc::clone(&parser), 2);
            s.parse_full(text);
            assert_eq!(sess_str(&parser, &s), full(&parser, text), "text={:?}", text);
        }
    }

    #[test]
    fn edit_matches_full_reparse_small_interval() {
        // Small interval (K=2) forces checkpoint creation & resume every 2 gens.
        let parser = nested_repeat_parser();
        // start from a valid "ababab", apply edits, each time compare to full reparse.
        let mut s = ParseSession::with_interval(Arc::clone(&parser), 2);
        s.parse_full("ababab");
        let mut doc: Vec<char> = "ababab".chars().collect();

        // edit script: (pos, old_len, new)
        let edits: &[(usize, usize, &str)] = &[
            (2, 0, "ab"),   // insert "ab" mid -> abababab
            (0, 2, ""),     // delete leading "ab" -> ababab
            (6, 0, "ab"),   // append at end -> abababab
            (3, 1, "b"),    // replace 'a'->'b' (breaks parse) -> abbbabab? check err path
            (3, 1, "a"),    // fix back
        ];
        for (i, &(pos, old, new)) in edits.iter().enumerate() {
            // mirror
            let pos = pos.min(doc.len());
            let end = (pos + old).min(doc.len());
            let ins: Vec<char> = new.chars().collect();
            doc.splice(pos..end, ins);
            s.edit(pos, old, new);
            let text: String = doc.iter().collect();
            assert_eq!(
                sess_str(&parser, &s),
                full(&parser, &text),
                "edit#{} pos={} old={} new={:?} doc={:?}",
                i, pos, old, new, text
            );
        }
    }

    #[test]
    fn checkpoint_resume_equivalence_all_positions() {
        // For every edit position, a prefix-resume must equal a from-scratch parse.
        let parser = nested_repeat_parser();
        let base = "abababababab"; // 12 chars, valid
        for interval in [1usize, 2, 3, 64] {
            for pos in 0..=base.len() {
                let mut s = ParseSession::with_interval(Arc::clone(&parser), interval);
                s.parse_full(base);
                // insert "ab" at pos (keeps validity only at even positions; either
                // way the differential must hold).
                let mut doc: Vec<char> = base.chars().collect();
                let ins: Vec<char> = "ab".chars().collect();
                doc.splice(pos..pos, ins);
                s.edit(pos, 0, "ab");
                let text: String = doc.iter().collect();
                assert_eq!(
                    sess_str(&parser, &s),
                    full(&parser, &text),
                    "interval={} pos={} doc={:?}",
                    interval, pos, text
                );
                // resume gen must be <= pos (prefix invariant).
                assert!(
                    s.stats().last_resume_gen <= pos,
                    "resume gen {} > pos {}",
                    s.stats().last_resume_gen, pos
                );
            }
        }
    }

    #[test]
    fn noop_edit_converges_immediately() {
        // Replacing a char with itself: convergence distance must be 0 (I1).
        let parser = nested_repeat_parser();
        let mut s = ParseSession::with_interval(Arc::clone(&parser), 2);
        s.parse_full("abababab");
        // replace char at index 4 ('a') with 'a' (no-op text, delta 0).
        s.edit(4, 1, "a");
        assert_eq!(
            s.stats().last_convergence_distance,
            Some(0),
            "no-op edit should converge immediately"
        );
        assert!(!s.stats().last_rediverged);
        // and still correct
        assert_eq!(sess_str(&parser, &s), full(&parser, "abababab"));
    }

    #[test]
    fn convergence_detected_on_valid_edit() {
        // A parse-preserving edit should be detected as converging (finite distance).
        let parser = nested_repeat_parser();
        let mut s = ParseSession::with_interval(Arc::clone(&parser), 4);
        s.parse_full("abababababab");
        // insert "ab" at position 4 (stays valid) -> convergence expected.
        s.edit(4, 0, "ab");
        assert!(
            s.stats().last_convergence_distance.is_some(),
            "valid structural edit should converge; stats={:?}",
            s.stats()
        );
        assert_eq!(sess_str(&parser, &s), full(&parser, "ababababababab"));
    }
}
