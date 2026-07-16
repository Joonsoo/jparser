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
//! parse's fingerprint sequence to find the convergence gen q*. The counters
//! (`SessionStats`) measure the reuse headroom (would-splice tail). Fingerprints
//! of BOTH the old and new document are computed under the SAME edit anchor `p`
//! (== the edit char index), exactly as the probe does, so the session's
//! convergence numbers reproduce the probe's band.
//!
//! I2 turns that reuse headroom into a SPLICE (design §2). When convergence is
//! detected at gen q* with q* < the last gen (eager-EOF-fold safe, §2.4), the
//! session:
//!   1. Verifies a one-shot STRUCTURAL full match (not just the fingerprint —
//!      collision guard, §2.1/§2.5): the OLD parse's live state at q*-delta,
//!      rebased into the new gen space via the split mapping (`rebase.rs`), must
//!      equal the freshly-parsed NEW state at q* exactly. On mismatch it abandons
//!      the splice and keeps parsing (correctness preserved, counter bumped).
//!   2. STOPS re-parsing the remaining gens. The final ctx and the spliced
//!      history are reconstructed once by rebasing the OLD parse's suffix
//!      (`GenRebase`, O(state) + O(history) once per edit) and gluing it onto the
//!      freshly-parsed prefix `[0..=q*]`.
//! The spliced result is stored exactly like a normal parse (outcome +
//! canonical_history + final ctx), so the consumer path (is_accepted /
//! kernels_history) is UNCHANGED and the NEXT edit naturally treats this spliced
//! result as its baseline — chained edits (splice-over-splice) just work, and the
//! differential oracle proves it.
//!
//! Threading: a session is a single-document, single-owner object. The parser
//! handle is shared read-only across sessions — either co-owned as
//! `Arc<Mgroup3Parser>` (Rust-native constructors) or borrowed as a raw pointer
//! the FFI caller owns (see `SessionParser`); one parser backs many document
//! sessions, none of which mutate it.

use std::cell::RefCell;
use std::rc::Rc;
use std::sync::Arc;

use crate::fingerprint::{fp_state, Fp, Variant};
use crate::history::History;
use crate::parser::record_cond::{active_intervals, dangerous_roots};
use crate::parser::{Mgroup3Parser, ParsingError};
use crate::parsing_ctx::{KtlibKernel, ParsingCtx};
use crate::path_root::PathRoot;
use crate::rebase::{paths_match_after_rebase, GenRebase};
use rustc_hash::{FxHashMap as HashMap, FxHashSet as HashSet};

/// Default checkpoint interval (gens). See design §1.2 — K/2 rewind ≈ the p90
/// convergence distance, so the rewind cost is on par with the convergence tail.
pub const DEFAULT_CHECKPOINT_INTERVAL: usize = 64;

/// A periodic parse-state checkpoint at a gen boundary. `ctx.gen_idx == at_gen`;
/// the state depends only on document chars with index `< at_gen`, so it is valid
/// to resume from while those chars are unchanged.
///
/// HISTORY-MARKER optimization (design §1.3): the stored `ctx` has its `history`
/// EMPTIED (`clone_without_history`). Deep-copying the full history into every
/// checkpoint would be O(N) per checkpoint → O(N²/K) for a full parse (the
/// dominant cost, and severe on real LSP-sized files). Instead the checkpoint
/// keeps only the gen marker (`at_gen`); on resume the session reconstructs the
/// required history prefix `[0..=at_gen]` from its retained `canonical_history`
/// via `History::prefix` (structure-sharing — Rc-bumps the unchanged chunks plus
/// a bounded straddle copy, not an O(at_gen) deep copy). The `Rc`/`Arc`-shared
/// path structure in `ctx` is only ref-counted, so a history-less checkpoint
/// clone is cheap.
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
    /// I2: `true` if this edit spliced (reused the old suffix instead of parsing
    /// it). Implies `last_convergence_gen.is_some()` and the structural guard
    /// passed.
    pub last_spliced: bool,
    /// I2: gens the splice avoided re-parsing (== would-splice tail when spliced).
    pub last_spliced_gens: usize,
    /// I2: `true` if convergence was detected but the splice was declined
    /// (structural guard failed, or q* == last gen so eager-EOF-fold safety
    /// blocked it). Each maps to a distinct decline reason (see `SpliceDecline`).
    pub last_splice_declined: Option<SpliceDecline>,
}

/// Why a detected convergence did NOT splice (design §2.4/§2.5 fallbacks).
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum SpliceDecline {
    /// q* == the last gen: splicing would reuse the eager-EOF-folded final gen,
    /// whose fold decision depends on `is_last_input`. Design §2.4 restricts
    /// splice to q* < last gen; the tail here is empty anyway (no reuse lost).
    LastGen,
    /// The one-shot structural full match failed (fingerprint collision, or an
    /// unexpected state mismatch). Extremely rare; the parse continues.
    StructuralMismatch,
}

/// Per-edit AST-reuse boundary (Stage 1 of the AST delta protocol — design
/// `mgroup3/docs/lsp_result_boundary.md` §2). After a SPLICE edit the session
/// guarantees which gens of the new `kernels_history` are the previous parse's
/// (verbatim, or shift-adjusted) so a consumer can avoid re-walking/-encoding
/// them. The contract, in the new gen space, with the previous parse's
/// `kernels_history` as `old` and the current one as `new`:
///
///   - `g < dirty_lo` → `new[g] == old[g]` (VERBATIM prefix reuse).
///   - `g > dirty_hi` → `new[g] == shift(old[g - delta], pivot, delta)` where
///     `shift` maps every kernel gen `k > pivot` to `k + delta` (SUFFIX reuse,
///     the same split rebase `rebase.rs` applies to the spliced history).
///   - `[dirty_lo, dirty_hi]` must be recomputed (the dirty window).
///
/// When `spliced == false` (no splice this edit — parse errored, convergence
/// declined, or no baseline) there is no reuse: `dirty_lo = 0` and `dirty_hi`
/// covers the last gen, so neither reuse rule applies to any gen.
///
/// `dirty_hi == qstar` (the convergence gen). `dirty_lo` is the safe prefix
/// boundary (§2, and the derivation on `compute_dirty_lo`): below it, every
/// prefix record's forward condition replay is trapped in the identical,
/// unshifted prefix, so its gated kernels match the old parse's exactly.
#[derive(Clone, Copy, Debug)]
pub struct EditReuse {
    /// Whether the last edit spliced (reused the old suffix). Reuse rules below
    /// hold only when true.
    pub spliced: bool,
    /// Edit anchor `p` — the edit's starting char index (== the gen-rebase pivot).
    pub pivot: i32,
    /// `delta = new_len - old_len` in chars (gens).
    pub delta: i32,
    /// Gen the incremental re-parse resumed from (rewind target; `<= pivot`).
    pub resume_gen: usize,
    /// Convergence gen `q*` in the NEW gen space (`Some` iff `spliced`).
    pub qstar: Option<usize>,
    /// Inclusive-exclusive lower dirty bound: gens `[0, dirty_lo)` are VERBATIM
    /// reusable from the old parse.
    pub dirty_lo: usize,
    /// Inclusive upper dirty bound (`== qstar` when spliced): gens
    /// `(dirty_hi, new_len)` are SHIFT-reusable from the old parse.
    pub dirty_hi: usize,
}

/// Raw per-edit reuse inputs captured during `edit()`; the `EditReuse` (its
/// `dirty_lo` needs the old + new active-interval maps — O(n)) is derived from
/// these LAZILY by `edit_reuse()` and cached, so an edit that the consumer never
/// queries pays nothing beyond the cheap `prev_history` Rc-bump.
struct LastEdit {
    spliced: bool,
    pivot: i32,
    delta: i32,
    resume_gen: usize,
    qstar: Option<usize>,
    /// The PREVIOUS parse's full history (the splice baseline), retained as a
    /// structure-sharing `History` clone (Rc bump — its prefix chunks are shared
    /// with the new `canonical_history`, so this is O(#chunks), not O(gen)).
    /// Source of the OLD parse's active-interval map + reference graph for
    /// `dirty_lo`.
    prev_history: History,
    /// The PREVIOUS parse's final ctx (history-less, but carries `paths`), so
    /// `dirty_lo` can recompute the OLD parse's end-of-input late fins (a source
    /// of reference-graph edges). `None` when there was no baseline. Cheap `Rc`
    /// clone captured before `finish_parse` overwrites `prev_final_ctx`.
    prev_final_ctx: Option<Rc<ParsingCtx>>,
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
    /// I2 splice counters.
    pub spliced: u64,
    /// Gens the splices avoided re-parsing (sum of `last_spliced_gens`).
    pub sum_spliced_gens: u64,
    /// Convergences that declined to splice because q* == last gen (§2.4).
    pub splice_declined_last_gen: u64,
    /// Convergences that declined to splice because the structural guard failed.
    pub splice_declined_structural: u64,
    /// Total nanoseconds spent rebasing final ctx + materializing spliced history.
    pub splice_rebase_nanos: u64,
}

/// How a session references its parser. The parser is read-only and shared
/// (`Send + Sync`, `core.rs:55`); a session never mutates it and never clones the
/// reference, so it only needs a `&Mgroup3Parser` that outlives the session.
///
/// - `Owned` — an `Arc` the session co-owns (the Rust-native constructors). The
///   parser lives at least as long as the session; sharing across sessions is via
///   `Arc::clone` by the caller.
/// - `Borrowed` — a raw pointer to a parser the FFI caller owns elsewhere (the box
///   from `mgroup3_parser_new*`). ONE parser handle can back MANY document
///   sessions this way (the mulang LSP shares a single handle across all open
///   documents). SAFETY CONTRACT: the borrowed parser MUST outlive every session
///   created from it — `mgroup3_session_destroy` all sessions before
///   `mgroup3_parser_free`. The FFI boundary (`ffi.rs`) upholds and documents this.
///   A borrowed parser is only dereferenced while a session method runs; the
///   session never stores a derived reference.
enum SessionParser {
    Owned(Arc<Mgroup3Parser>),
    Borrowed(*const Mgroup3Parser),
}

impl SessionParser {
    #[inline]
    fn get(&self) -> &Mgroup3Parser {
        match self {
            SessionParser::Owned(a) => a,
            // SAFETY: upheld by the `Borrowed` contract above — the pointee
            // outlives the session and is never mutated (parser is read-only,
            // Send + Sync). Non-null is guaranteed by the FFI constructor.
            SessionParser::Borrowed(p) => unsafe { &**p },
        }
    }
}

pub struct ParseSession {
    parser: SessionParser,
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
    /// parse errored. Held as a structure-sharing `History` (see `history.rs`):
    /// retaining it after a parse is an `Rc` bump (O(#chunks)), and `restore_ctx`'s
    /// `prefix` shares the unchanged chunks instead of deep-copying — so neither a
    /// full parse nor an edit pays the old O(gen) canonical-history copy.
    canonical_history: History,
    /// I2 splice source: the FINAL ctx of the last successful parse (the old
    /// parse whose suffix a splice reuses). `None` until the first success / after
    /// an error. Held as `Rc` so cloning it into `prev_*` is cheap (the heavy
    /// `paths` chains are `Rc`-shared, ref-counted only). Stored with an EMPTY
    /// `history` (via `clone_without_history`): its sole consumer, `finish_splice`
    /// → `GenRebase::ctx`, replaces the history with the freshly materialized
    /// spliced one and never reads this ctx's history — so retaining a copy would
    /// be pure O(gen) waste. The canonical suffix a splice reuses lives in
    /// `canonical_history`.
    prev_final_ctx: Option<Rc<ParsingCtx>>,
    /// Checkpoint interval in gens.
    interval: usize,
    /// Final outcome of the last parse.
    outcome: Option<ParseOutcome>,
    stats: SessionStats,
    totals: SessionTotals,
    /// Raw reuse inputs from the last `edit()` (Stage 1 AST-delta boundary).
    /// `None` after `parse_full` (no edit yet) or an errored edit.
    last_edit: Option<LastEdit>,
    /// Lazily-derived + cached `EditReuse` for `last_edit`. Invalidated (set
    /// `None`) at the start of every parse; recomputed on the first `edit_reuse()`.
    edit_reuse_cache: RefCell<Option<EditReuse>>,
    /// A/B-comparison only (`dbg_d0_dirty_lo`): the `dirty_lo` the pre-closure
    /// (Stage 1.0) D0-only formula would have produced for the last spliced edit,
    /// cached alongside the real (closure) `dirty_lo`. Not part of the contract.
    dbg_d0_dirty_lo: RefCell<Option<usize>>,
    /// If true, every edit is cross-checked against a full re-parse and any
    /// mismatch aborts loudly (design §2.5). Enabled by `MG3_SESSION_VERIFY=1`.
    verify: bool,
}

impl ParseSession {
    pub fn new(parser: Arc<Mgroup3Parser>) -> Self {
        Self::with_interval(parser, DEFAULT_CHECKPOINT_INTERVAL)
    }

    pub fn with_interval(parser: Arc<Mgroup3Parser>, interval: usize) -> Self {
        Self::from_parser(SessionParser::Owned(parser), interval)
    }

    /// FFI constructor: build a session over a BORROWED parser (a raw pointer the
    /// caller owns elsewhere — see `SessionParser::Borrowed` for the lifetime
    /// contract). Many sessions may share one borrowed parser. `parser` must be
    /// non-null and point to a live `Mgroup3Parser` for the session's whole life.
    ///
    /// # Safety
    /// The caller guarantees `parser` is non-null, valid, and outlives the
    /// returned session (and every value derived from it). The FFI layer
    /// (`mgroup3_session_new`/`mgroup3_session_destroy`) enforces the ordering.
    pub unsafe fn from_raw_parser(parser: *const Mgroup3Parser, interval: usize) -> Self {
        Self::from_parser(SessionParser::Borrowed(parser), interval)
    }

    fn from_parser(parser: SessionParser, interval: usize) -> Self {
        let interval = interval.max(1);
        let verify = std::env::var("MG3_SESSION_VERIFY").map(|v| v == "1").unwrap_or(false);
        Self {
            parser,
            doc: Vec::new(),
            prev_doc: Vec::new(),
            have_baseline: false,
            checkpoints: Vec::new(),
            canonical_history: History::new(),
            prev_final_ctx: None,
            interval,
            outcome: None,
            stats: SessionStats::default(),
            totals: SessionTotals::default(),
            last_edit: None,
            edit_reuse_cache: RefCell::new(None),
            dbg_d0_dirty_lo: RefCell::new(None),
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

    /// The last edit's AST-reuse boundary (Stage 1 of the AST delta protocol —
    /// see `EditReuse`). `None` after `parse_full` (no edit yet) or an errored
    /// edit. Computed LAZILY on first call (it needs the old + new
    /// active-interval maps, each an O(n) pass) and cached until the next parse,
    /// so an edit whose reuse boundary the consumer never queries pays nothing.
    pub fn edit_reuse(&self) -> Option<EditReuse> {
        if let Some(cached) = *self.edit_reuse_cache.borrow() {
            return Some(cached);
        }
        let le = self.last_edit.as_ref()?;
        let new_len = self.canonical_history.len();
        let reuse = if le.spliced {
            let qstar = le.qstar.expect("a spliced edit records its convergence gen");
            let resume = le.resume_gen as i32;
            let parser = self.parser.get();
            // Active-interval maps + DANGEROUS root sets (reference-graph
            // transitive closure) of both parses drive the safe prefix boundary.
            // The dangerous set closes the transitive gap: a prefix record's
            // forward replay reaches the edit region iff it references (directly
            // OR through a chain of finish conditions) a root active past resume.
            let old_iv = active_intervals(&le.prev_history);
            let new_iv = active_intervals(&self.canonical_history);
            // End-of-input late fins (extra reference-graph edges), recomputed
            // from each parse's final ctx `paths` — the same source the evaluator
            // uses. The NEW parse's final ctx is the current outcome.
            let new_end_late = match &self.outcome {
                Some(ParseOutcome::Ok(ctx)) => parser.end_of_input_late_fins(ctx),
                _ => HashMap::default(),
            };
            let old_end_late = le
                .prev_final_ctx
                .as_ref()
                .map(|c| parser.end_of_input_late_fins(c))
                .unwrap_or_default();
            let old_d = dangerous_roots(&le.prev_history, &old_end_late, &old_iv, resume);
            let new_d = dangerous_roots(&self.canonical_history, &new_end_late, &new_iv, resume);
            let dirty_lo = compute_dirty_lo(le.resume_gen, &old_iv, &new_iv, &old_d, &new_d);
            // A/B: the pre-closure (Stage 1.0) D0-only boundary — the dangerous
            // SEED sets alone, no transitive closure — cached for `dbg_d0_dirty_lo`.
            let d0_of = |iv: &HashMap<PathRoot, (i32, i32)>| -> HashSet<PathRoot> {
                iv.iter().filter_map(|(&r, &(_, la))| (la >= resume).then_some(r)).collect()
            };
            let d0_lo = compute_dirty_lo(
                le.resume_gen,
                &old_iv,
                &new_iv,
                &d0_of(&old_iv),
                &d0_of(&new_iv),
            );
            *self.dbg_d0_dirty_lo.borrow_mut() = Some(d0_lo);
            EditReuse {
                spliced: true,
                pivot: le.pivot,
                delta: le.delta,
                resume_gen: le.resume_gen,
                qstar: Some(qstar),
                dirty_lo,
                dirty_hi: qstar,
            }
        } else {
            // No splice → no reuse: dirty covers the whole history so neither the
            // verbatim (`g < dirty_lo`) nor the shift (`g > dirty_hi`) rule fires.
            EditReuse {
                spliced: false,
                pivot: le.pivot,
                delta: le.delta,
                resume_gen: le.resume_gen,
                qstar: None,
                dirty_lo: 0,
                dirty_hi: new_len.saturating_sub(1),
            }
        };
        *self.edit_reuse_cache.borrow_mut() = Some(reuse);
        Some(reuse)
    }

    /// A/B-comparison ONLY: the `dirty_lo` the pre-closure (Stage 1.0) D0-only
    /// formula would have produced for the last spliced edit. `None` unless the
    /// last edit spliced AND `edit_reuse()` has been called (which populates it).
    /// Used by the reuse oracle to quantify how much the transitive closure
    /// widened the dirty window; NOT part of the reuse contract.
    #[doc(hidden)]
    pub fn dbg_d0_dirty_lo(&self) -> Option<usize> {
        *self.dbg_d0_dirty_lo.borrow()
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
            Some(ParseOutcome::Ok(ctx)) => self.parser.get().is_accepted(ctx),
            _ => false,
        }
    }

    pub fn kernels_history(&self) -> Option<Vec<HashSet<KtlibKernel>>> {
        match &self.outcome {
            Some(ParseOutcome::Ok(ctx)) => Some(self.parser.get().kernels_history(ctx)),
            _ => None,
        }
    }

    pub fn error(&self) -> Option<&ParsingError> {
        match &self.outcome {
            Some(ParseOutcome::Err(e)) => Some(e),
            _ => None,
        }
    }

    /// The session's parser (borrowed or co-owned). Lets consumers (the FFI layer,
    /// generated-crate session entry points) run the same result-encoding /
    /// AST-walk pipeline over `outcome()` that `mgroup3_parser_parse` runs over a
    /// one-shot parse.
    pub fn parser(&self) -> &Mgroup3Parser {
        self.parser.get()
    }

    /// Encode the last parse outcome as `Mgroup3ParseResult` proto bytes — exactly
    /// what `mgroup3_parser_parse` returns for the same final text (accept +
    /// kernels_history, or a typed parse error). The session guarantees this is
    /// byte-identical to a full re-parse's encoding (design §3.1). Returns `None`
    /// only before any parse has run.
    pub fn encode_result(&self) -> Option<Vec<u8>> {
        let outcome = self.outcome.as_ref()?;
        let arg: Result<&ParsingCtx, &ParsingError> = match outcome {
            ParseOutcome::Ok(ctx) => Ok(ctx),
            ParseOutcome::Err(e) => Err(e),
        };
        Some(crate::parser::encode_parse_result(self.parser.get(), arg))
    }

    /// Current document text (chars).
    pub fn document(&self) -> &[char] {
        &self.doc
    }

    /// Full parse from gen 0. Establishes the checkpoint ring and the baseline.
    pub fn parse_full(&mut self, text: &str) -> &ParseOutcome {
        self.doc = text.chars().collect();
        self.stats = SessionStats::default();
        // A full parse establishes a new baseline; there is no prior parse to
        // reuse against, so clear the edit-reuse boundary.
        self.last_edit = None;
        *self.edit_reuse_cache.borrow_mut() = None;
        *self.dbg_d0_dirty_lo.borrow_mut() = None;
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

        // Retain the PREVIOUS parse's full history (the splice baseline) BEFORE
        // `reparse_incremental` overwrites `canonical_history`. Structure-sharing
        // clone → an Rc bump (its chunks are then shared with the new history),
        // not an O(gen) copy. Feeds the lazy `edit_reuse()` boundary; invalidate
        // its cache now.
        let old_canonical = self.canonical_history.clone();
        // Also snapshot the OLD final ctx (history-less, carries `paths`) for the
        // OLD parse's end-of-input late fins — a source of reference-graph edges
        // in `dirty_lo`. Cheap `Rc` clone, captured before `finish_parse`
        // overwrites `prev_final_ctx` with the NEW parse's ctx.
        let old_final_ctx = self.prev_final_ctx.clone();
        *self.edit_reuse_cache.borrow_mut() = None;
        *self.dbg_d0_dirty_lo.borrow_mut() = None;

        // Apply the edit to the document.
        let mut new_doc: Vec<char> =
            Vec::with_capacity(self.doc.len() - old_len_chars + new_chars.len());
        new_doc.extend_from_slice(&self.doc[..pos]);
        new_doc.extend_from_slice(&new_chars);
        new_doc.extend_from_slice(&self.doc[end..]);
        self.doc = new_doc;

        self.stats = SessionStats::default();
        self.stats.last_had_baseline = had_baseline;

        // Convergence detection compares the edited parse, gen by gen, against the
        // OLD document's per-gen strict fingerprints under THIS anchor. Those
        // fingerprints are produced LAZILY by a `BaselineWalker` inside
        // `reparse_incremental` (only the gens actually queried are computed), not
        // by an upfront full trace of the old document — see that method and
        // `BaselineWalker`. Detection is enabled only when we have a baseline (skip
        // on the first edit after an errored / absent parse).
        let ok = self.reparse_incremental(
            pos,
            anchor,
            delta,
            edit_end,
            had_baseline,
            &baseline_doc,
        );
        self.finish_parse(ok);

        // Record the Stage-1 reuse inputs for this edit (derived lazily by
        // `edit_reuse`). Only meaningful when the edit produced an Ok outcome;
        // an errored parse has no reusable kernels_history.
        if matches!(self.outcome, Some(ParseOutcome::Ok(_))) {
            self.last_edit = Some(LastEdit {
                spliced: self.stats.last_spliced,
                pivot: anchor,
                delta,
                resume_gen: self.stats.last_resume_gen,
                qstar: if self.stats.last_spliced {
                    self.stats.last_convergence_gen
                } else {
                    None
                },
                prev_history: old_canonical,
                prev_final_ctx: old_final_ctx,
            });
        } else {
            self.last_edit = None;
        }

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
            if self.stats.last_spliced {
                self.totals.spliced += 1;
                self.totals.sum_spliced_gens += self.stats.last_spliced_gens as u64;
            }
            match self.stats.last_splice_declined {
                Some(SpliceDecline::LastGen) => self.totals.splice_declined_last_gen += 1,
                Some(SpliceDecline::StructuralMismatch) => {
                    self.totals.splice_declined_structural += 1
                }
                None => {}
            }
        }

        if self.verify {
            self.verify_against_full_reparse();
        }

        self.outcome.as_ref().expect("outcome set")
    }

    // -- internals -----------------------------------------------------------

    /// After a parse, record the current document as the next baseline iff the
    /// parse succeeded. Also retains the final ctx as the next edit's splice
    /// source (I2). Its `history` equals `canonical_history` (both set by the
    /// reparse routines), so a splice can reuse either.
    fn finish_parse(&mut self, ok: bool) {
        if ok {
            self.prev_doc = self.doc.clone();
            self.have_baseline = true;
            if let Some(ParseOutcome::Ok(ctx)) = &self.outcome {
                // history-less clone: the splice source's history is never read
                // (see the field doc); the canonical suffix lives in
                // `canonical_history`. Avoids an O(gen) deep copy every edit.
                self.prev_final_ctx = Some(Rc::new(ctx.clone_without_history()));
            }
        } else {
            self.prev_doc.clear();
            self.have_baseline = false;
            self.prev_final_ctx = None;
        }
    }

    /// Make a history-less checkpoint snapshot of `ctx` (design §1.3): clone ctx but
    /// drop its history (the heavy field); the `Rc`/`Arc` path structure is merely
    /// ref-counted, so this is cheap. History is refilled from `canonical_history`
    /// on resume.
    fn snapshot(ctx: &ParsingCtx, at_gen: usize) -> Checkpoint {
        // history-less clone (no O(gen) history deep copy — the checkpoint's
        // history is refilled from `canonical_history` on resume).
        Checkpoint { at_gen, ctx: ctx.clone_without_history() }
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
            // `prefix(at + 1)` shares the unchanged chunks by Rc bump and copies
            // only the (≤ CHUNK) straddling remainder — O(#chunks + CHUNK), not
            // the old O(at) deep copy.
            cp.ctx.history = self.canonical_history.prefix(at + 1);
        }
        cp.ctx
    }

    /// Parse the whole document from gen 0, (re)building the checkpoint ring.
    /// Returns true on success.
    fn reparse_from_scratch(&mut self) -> bool {
        self.checkpoints.clear();
        self.canonical_history = History::new();
        let total = self.doc.len();
        let mut ctx = self.parser.get().init_ctx();
        self.checkpoints.push(Self::snapshot(&ctx, 0));
        self.stats.last_resume_gen = 0;
        let mut reparsed = 0usize;
        for (idx, &c) in self.doc.iter().enumerate() {
            match self.parser.get().parse_step(ctx, c, idx + 1 == total) {
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
        // Seal first so the clone is a pure Rc bump (no tail deep copy).
        ctx.history.seal();
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
            None => Checkpoint { at_gen: 0, ctx: self.parser.get().init_ctx() },
        }
    }

    /// The newest checkpoint at gen <= `target_old_gen`, as a `BaselineWalker`
    /// resume point over the OLD document. Same selection as `resume_checkpoint`
    /// but semantically distinct: this indexes the OLD gen space (baseline fps),
    /// not the NEW prefix. Called BEFORE the ring is truncated, so `self.checkpoints`
    /// still holds the OLD parse's ring — the returned checkpoint is cloned out so
    /// the truncation that follows cannot invalidate it. The checkpoint carries no
    /// history (see `Checkpoint`), which is exactly what the walker needs.
    fn old_resume_checkpoint(&self, target_old_gen: usize) -> Checkpoint {
        let mut chosen: Option<&Checkpoint> = None;
        for c in &self.checkpoints {
            if c.at_gen <= target_old_gen {
                chosen = Some(c);
            } else {
                break;
            }
        }
        match chosen {
            Some(c) => c.clone(),
            None => Checkpoint { at_gen: 0, ctx: self.parser.get().init_ctx() },
        }
    }

    /// Re-parse from the resume checkpoint to end of the (edited) document,
    /// rebuilding the checkpoint ring beyond the resume point and detecting
    /// convergence against the OLD document's per-gen strict fps under `anchor`.
    /// Those baseline fps are produced LAZILY by a `BaselineWalker` (only the gens
    /// actually queried are computed) instead of an upfront full trace of the old
    /// document — see `BaselineWalker`. On convergence at q* < last gen, SPLICE
    /// (I2): verify the structural full match (using the walker's live state at
    /// q*-delta) then stop parsing and reuse the old suffix. `old_doc` is the
    /// previous document (the walker's parse source + splice source).
    /// `had_baseline` gates convergence detection (false = no prior successful
    /// parse to compare against). Returns true on success.
    fn reparse_incremental(
        &mut self,
        pos: usize,
        anchor: i32,
        delta: i32,
        edit_end: usize,
        had_baseline: bool,
        old_doc: &[char],
    ) -> bool {
        let resume = self.resume_checkpoint(pos);
        let resume_gen = resume.at_gen;
        self.stats.last_resume_gen = resume_gen;

        // The gen-rebase mapping from OLD gen space to NEW: `g<=p → g`, `g>p → g+delta`.
        let rebase = GenRebase::new(anchor, delta);

        // Map a NEW gen `q` to the OLD gen it should align with (q - delta). Used to
        // index the baseline walker and, on a splice, the old suffix.
        let old_gen_of = |q: usize| -> Option<usize> {
            let pi = q as i64 - delta as i64;
            if pi < 0 {
                None
            } else {
                Some(pi as usize)
            }
        };

        // Splicing needs the old parse's full history + final ctx. They are the
        // retained `canonical_history` / `prev_final_ctx` (still the OLD parse's at
        // this point — overwritten only after this returns).
        let can_splice = had_baseline && self.prev_final_ctx.is_some();

        // Build the lazy baseline walker over the OLD document BEFORE truncating the
        // checkpoint ring below — its resume checkpoint is cloned from the OLD ring
        // (which the truncation is about to rebuild for the NEW parse). The earliest
        // gen the convergence loop can query is `og_min = old_gen_of(edit_end)` (the
        // first `g >= edit_end` maps there); the walker resumes from the newest OLD
        // checkpoint at gen <= og_min so it walks the least possible distance. The
        // walker needs NO history: `fp_state` and `paths_match_after_rebase` read
        // only `ctx.paths`, and `paths` evolution is independent of `ctx.history`
        // (the sole history read in `parse_step` — `prev_reported` — feeds only the
        // discarded report channels, never the live state). See `BaselineWalker`.
        let mut walker: Option<BaselineWalker> = if had_baseline {
            let og_min = old_gen_of(edit_end).unwrap_or(0);
            let cp = self.old_resume_checkpoint(og_min);
            Some(BaselineWalker::new(self.parser.get(), old_doc, anchor, cp))
        } else {
            None
        };

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

        let mut reparsed = 0usize;
        let mut spliced_at: Option<usize> = None; // new-gen q* where we spliced
        for idx in resume_gen..total {
            let c = self.doc[idx];
            match self.parser.get().parse_step(ctx, c, idx + 1 == total) {
                Ok(next) => {
                    ctx = next;
                    reparsed += 1;
                    let g = ctx.gen_idx as usize;

                    if let Some(w) = walker.as_mut() {
                        if convergence_gen.is_none() && g >= edit_end {
                            if let Some(af) = old_gen_of(g).and_then(|og| w.fp_at(og)) {
                                let bf = fp_state(&ctx, anchor, ctx.gen_idx, Variant::Strict);
                                if bf == af {
                                    convergence_gen = Some(g);
                                    // I2: attempt a splice. Eager-EOF-fold safety
                                    // (§2.4): only splice when q* < last gen, so the
                                    // reused suffix's `is_last_input` folds match.
                                    // `g == total` is the last gen (idx == total-1).
                                    if can_splice && g < total {
                                        // Structural full-match guard (§2.1): the OLD
                                        // live state at og (== q*-delta), rebased, must
                                        // equal the NEW live state at g. Blocks hash
                                        // collisions. The walker is standing exactly at
                                        // og (fp_at above advanced it there), so its
                                        // current live ctx IS that old state — no
                                        // separate re-parse needed.
                                        let guard_ok = w
                                            .current_ctx()
                                            .map(|os| paths_match_after_rebase(&rebase, os, &ctx))
                                            .unwrap_or(false);
                                        if guard_ok {
                                            spliced_at = Some(g);
                                            break;
                                        } else {
                                            self.stats.last_splice_declined =
                                                Some(SpliceDecline::StructuralMismatch);
                                        }
                                    } else if can_splice && g >= total {
                                        self.stats.last_splice_declined =
                                            Some(SpliceDecline::LastGen);
                                    }
                                }
                            }
                        } else if let Some(cg) = convergence_gen {
                            // stability: after q*, later gens should keep matching.
                            // (Only reached when a splice was NOT taken at q*.)
                            if g > cg && !rediverged {
                                if let Some(af) = old_gen_of(g).and_then(|og| w.fp_at(og)) {
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

        if let Some(qstar) = spliced_at {
            // I2 SPLICE: reuse the old suffix instead of parsing gens (qstar, total].
            self.finish_splice(qstar, delta, &rebase, ctx);
        } else {
            // No splice — the freshly-parsed ctx is the whole result. Seal first
            // so retaining the canonical history is a pure Rc bump.
            ctx.history.seal();
            self.canonical_history = ctx.history.clone();
            self.outcome = Some(ParseOutcome::Ok(ctx));
        }
        true
    }

    /// Materialize a spliced parse result at convergence gen `qstar` (new gen
    /// space). `new_ctx` is the freshly-parsed ctx AT `qstar` (its `history` is
    /// `[0..=qstar]`, new gen space). The old parse's suffix
    /// `canonical_history[qstar_old+1 ..]` (old gen space) is rebased via the split
    /// mapping and glued on; the OLD final ctx (`prev_final_ctx`) is rebased into
    /// the final live state. Design §2.2 (a)+(b): one O(state)+O(history) pass.
    fn finish_splice(
        &mut self,
        qstar: usize,
        delta: i32,
        rebase: &GenRebase,
        new_ctx: ParsingCtx,
    ) {
        let t0 = std::time::Instant::now();
        let qstar_old = (qstar as i32 - delta) as usize;

        // Segment A+B: the freshly-parsed history [0..=qstar] (new gen space).
        // new_ctx.history has exactly qstar+1 entries (gens 0..=qstar). Move it out
        // (no clone) — new_ctx is owned and only its history is reused here.
        debug_assert_eq!(new_ctx.history.len(), qstar + 1, "new_ctx history len at q*");
        let mut spliced: History = new_ctx.history;

        // Segment C: old history (qstar_old, ..] rebased into the new gen space.
        // The convergence gen itself (old qstar_old ≡ new qstar) is already covered
        // by segment B, so segment C starts at qstar_old + 1. Field-unit split
        // mapping (see rebase.rs) — prefix anchors (g<=p) stay, post-edit gens
        // (g>p) shift, WITHIN each rebased entry. BY DESIGN O(suffix): one
        // `rebase.entry` per suffix gen (the reused tail), appended onto the
        // freshly-parsed prefix's unsealed tail.
        let old_len = self.canonical_history.len();
        let seg_c_start = (qstar_old + 1).min(old_len);
        for i in seg_c_start..old_len {
            let e = self.canonical_history.get(i).expect("canonical history entry in splice range");
            spliced.push(rebase.entry(e));
        }
        // Full history length invariant: (qstar+1) + (old_len - (qstar_old+1))
        //   = delta + old_len = new_total + 1 (gens 0..=new_total). See finish_splice.
        debug_assert_eq!(
            spliced.len() as i32,
            old_len as i32 + delta,
            "spliced history length != old_len + delta"
        );

        // Retain the spliced history as the next edit's canonical source BEFORE it
        // is moved into the final ctx. Seal first so this clone is a pure Rc bump.
        spliced.seal();
        self.canonical_history = spliced.clone();

        // Final live state: rebase the OLD final ctx into the new gen space and
        // attach the spliced history. (`prev_final_ctx` is the OLD parse's final
        // ctx; still valid here — overwritten by finish_parse after we return.)
        let old_final = self.prev_final_ctx.as_ref().expect("prev_final_ctx for splice");
        let final_ctx = rebase.ctx(old_final, spliced);
        debug_assert_eq!(
            final_ctx.gen_idx,
            final_ctx.history.len() as i32 - 1,
            "final ctx gen_idx must be last history gen"
        );

        let spliced_gens = old_len.saturating_sub(qstar_old + 1);
        self.stats.last_spliced = true;
        self.stats.last_spliced_gens = spliced_gens;
        self.stats.last_splice_declined = None;
        self.totals.splice_rebase_nanos += t0.elapsed().as_nanos() as u64;

        self.outcome = Some(ParseOutcome::Ok(final_ctx));
    }

    /// Debug self-check (design §2.5): full re-parse of the current document and a
    /// byte-compare of accept + kernels_history. Panics with a dump on mismatch.
    fn verify_against_full_reparse(&self) {
        let text: String = self.doc.iter().collect();
        let full = self.parser.get().parse(&text);
        let (full_accept, full_hist) = match full {
            Ok(ctx) => (self.parser.get().is_accepted(&ctx), Some(self.parser.get().kernels_history(&ctx))),
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

/// Safe verbatim-prefix boundary `dirty_lo` for `EditReuse` (design §2 + the
/// Stage-1.1 transitive-closure fix). Returns the smallest gen from which a
/// prefix record's forward condition replay could reach `resume_gen` or beyond
/// (into the edited / re-parsed region) and thus produce different gated kernels
/// than the old parse; every gen below it is verbatim-reusable.
///
/// SOUNDNESS. A record's replay for a referenced root `r` scans finishes only
/// within `[from_gen, last_active(r)+1]` (record_cond.rs `any_absorbed_fin_true`)
/// and RECURSES into the finish conditions it meets, which may reference further
/// roots. A record's replay is trapped in the identical prefix `[0, resume_gen)`
/// (entries there are byte-identical — unchanged chars) iff NONE of the roots it
/// transitively touches is active at/after `resume_gen`. `dangerous_roots`
/// computes exactly that transitive-closure set `D` (seed = roots with
/// `last_active >= resume_gen`, propagated backward along finish-condition
/// reference edges). A prefix record at gen `g` references some `r ∈ D` only at
/// gens `g >= first_active(r)` (the reference-birth invariant, verified by the
/// oracle's birth probe — `birth_probe_*`), so the earliest possibly-unsafe gen
/// is `min first_active(r)` over `r ∈ D_old ∪ D_new`. Capped at `resume_gen`
/// (gens at/after it are re-parsed → already inside the dirty window).
///
/// Both parses' sets matter for the asymmetric case (a root whose replay crossed
/// into the old suffix but not the new, or vice versa). Suffix roots (born after
/// the pivot) always have `first_active > pivot >= resume_gen`, so they only ever
/// lower `lo` to at most the cap — harmless.
///
/// A dangerous root must have an interval entry (danger requires either activity
/// — `D0`, in the map — or an outgoing finish edge, which implies the root
/// finished hence was active). `debug_assert`ed; the release fallback stays sound
/// by treating a missing interval as "unbounded reference reach" → `dirty_lo = 0`.
fn compute_dirty_lo(
    resume_gen: usize,
    old_iv: &HashMap<PathRoot, (i32, i32)>,
    new_iv: &HashMap<PathRoot, (i32, i32)>,
    old_d: &HashSet<PathRoot>,
    new_d: &HashSet<PathRoot>,
) -> usize {
    let r = resume_gen as i32;
    let mut lo = r;
    let fold = |d: &HashSet<PathRoot>, iv: &HashMap<PathRoot, (i32, i32)>, lo: &mut i32| {
        for root in d {
            match iv.get(root) {
                Some(&(first_active, _last_active)) => *lo = (*lo).min(first_active),
                None => {
                    debug_assert!(
                        false,
                        "dangerous root {root:?} has no active interval — closure invariant broken"
                    );
                    *lo = 0; // conservative: cannot bound its reference reach
                }
            }
        }
    };
    fold(old_d, old_iv, &mut lo);
    fold(new_d, new_iv, &mut lo);
    lo.max(0) as usize
}

/// Lazy producer of the OLD document's per-gen STRICT fingerprints (Phase I1/I2
/// convergence baseline), replacing the old upfront `fingerprint_trace`.
///
/// ## Why lazy (the consumption sites are local and monotone)
///
/// The convergence loop (`reparse_incremental`) consumes baseline fps at exactly
/// three sites, and every one requests OLD gens that only ever *increase*:
///   1. convergence detection: `fp_at(og)` for `og = g - delta`, `g` scanned from
///      `edit_end` upward (monotone increasing).
///   2. redivergence stability (splice declined): same `g > q*`, still increasing.
///   3. the splice structural guard: the OLD live `paths` at the convergence gen
///      `og = q* - delta` — the walker is *already standing there* after the
///      detection query, so `current_ctx()` serves it with no extra work.
/// Measured convergence distance is tiny (median 0 / p90 22 gens), so the walk
/// advances only a handful of gens past the edit in the common case, versus the
/// old code which re-parsed the ENTIRE old document from gen 0 on every edit.
///
/// ## Why it needs NO history
///
/// `fp_state` and `paths_match_after_rebase` read only `ctx.paths`. `paths`
/// evolution in `parse_step` is independent of `ctx.history`: the single history
/// read there (`history.last()` → `prev_reported`) feeds only the report-channel
/// dedup filter of the pushed `HistoryEntry`, never the returned live `paths` /
/// `gen_idx`. So the walker resumes from a history-less checkpoint and advances
/// with an empty (then self-accumulated, discarded) history, and every fp / guard
/// result is byte-identical to what the upfront trace produced.
///
/// ## Checkpoint-ring lifetime
///
/// The resume checkpoint is cloned out of the OLD ring by the caller BEFORE the
/// ring is truncated/rebuilt for the NEW parse (see `old_resume_checkpoint`), so
/// the walker owns its resume state and the truncation cannot invalidate it. The
/// walker borrows only the (read-only, shared) parser and the caller's `old_doc`
/// slice — never the session — so it coexists with the session mutating its ring
/// during the re-parse loop.
struct BaselineWalker<'a> {
    parser: &'a Mgroup3Parser,
    old_doc: &'a [char],
    /// Fingerprint anchor `p` (== the edit char index), shared with the new parse.
    anchor: i32,
    /// Live ctx of the OLD parse, advanced up to (and standing at) `cur_gen`. Its
    /// `paths` are exact; its `history` is irrelevant scratch (see struct doc).
    /// `None` once a step has errored (an invariant violation — the OLD parse
    /// succeeded — handled defensively as "no baseline past here").
    ctx: Option<ParsingCtx>,
    /// Gen `ctx` currently sits at (== `ctx.gen_idx`). Monotone non-decreasing.
    cur_gen: usize,
}

impl<'a> BaselineWalker<'a> {
    fn new(parser: &'a Mgroup3Parser, old_doc: &'a [char], anchor: i32, resume: Checkpoint) -> Self {
        // The resume checkpoint's ctx is history-less (see `Checkpoint`) — exactly
        // what the walker needs. Its `paths`/`gen_idx` reproduce the OLD parse's
        // state at `resume.at_gen`, so advancing from here re-derives every later
        // OLD gen's `paths` identically to a from-gen-0 re-parse.
        BaselineWalker {
            parser,
            old_doc,
            anchor,
            ctx: Some(resume.ctx),
            cur_gen: resume.at_gen,
        }
    }

    /// STRICT fingerprint of the OLD parse's live state at OLD gen `og`. Advances
    /// the internal ctx forward from `cur_gen` to `og` (monotone: `og` must be
    /// `>= cur_gen`). Returns `None` if `og` is past the OLD document's last gen
    /// (mirrors the old `baseline_fp.get(og)` returning `None` past the vec end)
    /// or a step errored (invariant violation on a proven-good OLD parse — treated
    /// defensively as no baseline).
    fn fp_at(&mut self, og: usize) -> Option<Fp> {
        debug_assert!(
            og >= self.cur_gen,
            "BaselineWalker queried backwards: og={} < cur_gen={}",
            og,
            self.cur_gen
        );
        if og > self.old_doc.len() {
            return None;
        }
        let total = self.old_doc.len();
        while self.cur_gen < og {
            let ctx = self.ctx.take()?;
            let idx = self.cur_gen; // char index consumed to reach cur_gen+1
            match self.parser.parse_step(ctx, self.old_doc[idx], idx + 1 == total) {
                Ok(next) => {
                    debug_assert_eq!(
                        next.gen_idx as usize,
                        self.cur_gen + 1,
                        "BaselineWalker gen desync"
                    );
                    self.cur_gen += 1;
                    self.ctx = Some(next);
                }
                Err(_) => {
                    // The OLD parse succeeded (have_baseline), so a step error here
                    // is impossible; be defensive rather than panic in release.
                    self.ctx = None;
                    return None;
                }
            }
        }
        let ctx = self.ctx.as_ref()?;
        Some(fp_state(ctx, self.anchor, ctx.gen_idx, Variant::Strict))
    }

    /// The OLD parse's live ctx at the gen the walker last advanced to. After a
    /// `fp_at(og)` that returned `Some`, the walker stands exactly at `og`, so this
    /// is the OLD live state at `og` — the comparand for the splice structural
    /// guard (`paths_match_after_rebase` reads only its `paths`).
    fn current_ctx(&self) -> Option<&ParsingCtx> {
        self.ctx.as_ref()
    }
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

    #[test]
    fn splice_fires_and_is_correct() {
        // Insert "ab" mid-document (not at the end) so q* < last gen and a splice
        // actually fires. Output must byte-match a full re-parse.
        let parser = nested_repeat_parser();
        let mut s = ParseSession::with_interval(Arc::clone(&parser), 4);
        s.parse_full("abababababababab"); // 16 chars, valid
        s.edit(4, 0, "ab"); // -> 18 chars, still valid; suffix reused
        let st = s.stats();
        assert!(st.last_spliced, "expected splice to fire; stats={st:?}");
        assert!(st.last_spliced_gens > 0, "splice should reuse >0 gens; stats={st:?}");
        assert_eq!(st.last_splice_declined, None);
        assert_eq!(sess_str(&parser, &s), full(&parser, "ababababababababab"));
    }

    #[test]
    fn chained_splices_stay_correct() {
        // Splice-over-splice: many consecutive mid-document inserts. Each edit must
        // byte-match a full re-parse (the next edit's baseline is the prior splice).
        let parser = nested_repeat_parser();
        let mut s = ParseSession::with_interval(Arc::clone(&parser), 4);
        let mut doc: Vec<char> = "abababababababababab".chars().collect(); // 20
        s.parse_full(&doc.iter().collect::<String>());
        let mut splices = 0usize;
        for i in 0..12 {
            // Alternate insert positions in the interior (always even -> valid).
            let pos = 2 + (i % 6) * 2;
            let pos = pos.min(doc.len());
            doc.splice(pos..pos, "ab".chars());
            s.edit(pos, 0, "ab");
            let text: String = doc.iter().collect();
            assert_eq!(
                sess_str(&parser, &s),
                full(&parser, &text),
                "chained edit#{i} pos={pos} diverged"
            );
            if s.stats().last_spliced {
                splices += 1;
            }
        }
        assert!(splices > 0, "expected some splices across the chain");
    }

    /// The lazy `BaselineWalker` must produce, for every OLD gen, the SAME strict
    /// fingerprint an upfront from-gen-0 trace would — regardless of which
    /// checkpoint it resumes from. This is the core equivalence the lazy rewrite
    /// rests on (history-less resume + monotone forward walk == full trace).
    #[test]
    fn baseline_walker_matches_upfront_trace() {
        let parser = nested_repeat_parser();
        let doc: Vec<char> = "abababababababab".chars().collect(); // 16 gens
        let anchor = 6i32;

        // Reference: an upfront full trace from gen 0 (the old `fingerprint_trace`).
        let reference: Vec<Fp> = {
            let total = doc.len();
            let mut ctx = parser.init_ctx();
            let mut out = Vec::with_capacity(total + 1);
            out.push(fp_state(&ctx, anchor, ctx.gen_idx, Variant::Strict));
            for (idx, &c) in doc.iter().enumerate() {
                ctx = parser.parse_step(ctx, c, idx + 1 == total).expect("step");
                out.push(fp_state(&ctx, anchor, ctx.gen_idx, Variant::Strict));
            }
            out
        };

        // For several resume gens, drive a walker from a checkpoint at that gen and
        // query every OLD gen >= the resume gen; each must match the reference.
        for resume_gen in [0usize, 1, 4, 8, 12] {
            // Build the resume checkpoint by parsing the old doc up to resume_gen and
            // emptying its history (exactly what `snapshot` does).
            let mut ctx = parser.init_ctx();
            for (idx, &c) in doc.iter().enumerate().take(resume_gen) {
                ctx = parser.parse_step(ctx, c, idx + 1 == doc.len()).expect("step");
            }
            ctx.history = History::new();
            let cp = Checkpoint { at_gen: resume_gen, ctx };

            let mut w = BaselineWalker::new(&parser, &doc, anchor, cp);
            for og in resume_gen..=doc.len() {
                assert_eq!(
                    w.fp_at(og),
                    Some(reference[og]),
                    "walker fp mismatch at og={og} (resume_gen={resume_gen})"
                );
            }
            // Past the last gen must be None (mirrors old baseline_fp.get()).
            assert_eq!(w.fp_at(doc.len() + 1), None, "past-end fp must be None");
        }
    }

    /// Large interval + interior edit: the walker must resume from a DISTANT old
    /// checkpoint (gen 0, since interval > doc) and still walk to q* correctly so
    /// the splice fires and byte-matches. Guards the walker's checkpoint-lifetime
    /// handling (cloned before ring truncation) and resume-then-walk path.
    #[test]
    fn walker_resume_from_distant_checkpoint_splices() {
        let parser = nested_repeat_parser();
        // interval 1000 >> doc => the only checkpoint below the edit is gen 0, so
        // the walker resumes at gen 0 and walks all the way to q*.
        let mut s = ParseSession::with_interval(Arc::clone(&parser), 1000);
        s.parse_full("abababababababababab"); // 20 chars
        s.edit(4, 0, "ab"); // interior insert -> splice expected
        let st = s.stats();
        assert!(st.last_spliced, "expected splice with distant resume; stats={st:?}");
        assert_eq!(sess_str(&parser, &s), full(&parser, "ababababababababababab"));
    }
}
