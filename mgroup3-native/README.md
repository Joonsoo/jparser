# mgroup3-native

Rust port of the mgroup3 parser runtime. The Kotlin reference implementation
under `../mgroup3/parser/kotlin/` remains the source of truth for algorithm
semantics; this crate exists to give mgroup3 a path to native performance
without losing the JVM front-end.

## Status

Branch: `mg3native-claude` (forked from `mgroup3_claude`).

| Step | Topic | Commits | Status |
|------|-------|---------|--------|
| 1 | Rust crate skeleton, prost-generated `Mgroup3ParserData` types | `963fb95d` | done |
| 2 | `AcceptCondition` enum, canonical sort, `neg`, `evaluate`, `evolve`, Display-roundtrip parser | `9db759cd`, `8fe2582b` | done |
| 3.1 | `ParsingCtx` data types (Kernel, MilestonePath, PathShape, HistoryEntry, ...) | `96d7637f` | done |
| 3.2 | `ParserDataPlain` mirror of `Mgroup3ParserDataPlain.kt` | `54594ac6` | done |
| 3.3 | `term_group::is_match` + Java unicode category mapping | `b2b4510f` | done |
| 3.4 | `AcceptConditionTemplate` → `AcceptCondition` builder | `a5932241` | done |
| 3.5 | `Mgroup3Parser` struct, `init_ctx`, `cond_paths_for`, `find_applicable_action`, `expected_inputs_of` | `debb803a` | done |
| 3.6 | `parse_step` (7 phases), `apply_term_action`, `apply_edge_action`, `parse`, `is_accepted`, `kernels_history`, `evaluate_with_history` | `fa1d47b3` | done |
| 3.7 | `ParserFixtureGenTest.kt` + `parser_diff.rs` — 13 grammars / 59 inputs all green | `d546850a` | done |
| 4 (Phase A) | Result serialization (Rust → Kotlin via protobuf) | — | in progress |
| 4 (channel) | JNI or JEP 442 FFM — undecided | — | future |
| 5 (Phase B) | Grammar-specific AST schemas — Rust does reconstruction | — | future |

Test totals at end of Step 3.7:
- 124 Rust unit tests
- 94 AcceptCondition fixture blocks (Kotlin↔Rust)
- 59 parser fixture inputs across 13 grammars (Kotlin↔Rust)

## What's ported

The Rust runtime can parse any grammar that the Kotlin `Mgroup3Parser` parses,
and produces the same `List<KernelSet>` semantically. Differential testing
exercises this via golden files written by Kotlin and read by Rust
(`tests/parser_diff.rs`).

Public API surface (`mgroup3_native::parser::Mgroup3Parser`):

```rust
pub fn new(data: Mgroup3ParserData) -> Self
pub fn init_ctx(&self) -> ParsingCtx
pub fn init_ctx_with_start(&self, start_symbol_id: i32) -> ParsingCtx
pub fn parse(&self, text: &str) -> Result<ParsingCtx, ParsingError>
pub fn parse_step(&self, ctx: ParsingCtx, input: char, is_last_input: bool)
    -> Result<ParsingCtx, ParsingError>
pub fn is_accepted(&self, ctx: &ParsingCtx) -> bool
pub fn expected_inputs_of(&self, ctx: &ParsingCtx) -> TermSet
pub fn kernels_history(&self, ctx: &ParsingCtx)
    -> Vec<HashSet<KtlibKernel>>
pub fn cond_paths_for(&self, sym_ids: &[i32], gen_idx: i32)
    -> HashMap<PathRoot, PathMap>
pub fn find_applicable_action(&self, shape: &PathShape, input: char)
    -> Option<Rc<TermActionPlain>>
```

Plus the standalone `accept_condition` module with `and_from`/`or_from`/`neg`/
`evolve_accept_condition`/`evaluate_accept_condition`.

## What's intentionally NOT here

- Diagnostic prints (`setTrace`, `verbose`, `evolveTrace`).
- Phase timing (`enablePhaseTiming`, `phaseNanos`).
- `parseOrThrow` (compose on Kotlin side).
- AST reconstruction (deferred to Phase B; today the Kotlin side does this).
- JNI / FFM bindings (deferred to a later step; see "Channel" below).
- Multi-threading (single-threaded parser contract, matches Kotlin).

## Module layout

```
mgroup3-native/
├── Cargo.toml             # prost 0.14, bytes, unicode-general-category
├── build.rs               # prost-build for *.proto under ../{mgroup3/schema,base}/proto
└── src/
    ├── lib.rs             # module tree
    ├── path_root.rs       # PathRoot { symbol_id, start_gen }
    ├── parsing_ctx.rs     # Kernel, MilestonePath (Rc + OnceCell hash), PathShape,
    │                      #   HistoryEntry, ParsingCtx, KtlibKernel, add_path
    ├── parser_data.rs     # ParserDataPlain + 13 plain wrappers + transitive
    │                      #   initial cond-symbol closure
    ├── term_group.rs      # is_match(TermGroup, char) + Java getType() mapping
    │                      #   + TermSet stub
    ├── accept_condition/  # Step 2 — sealed enum + Display/parse/eval/evolve
    │   ├── mod.rs         #   AcceptCondition enum + referenced_roots()
    │   ├── build.rs       #   and_from / or_from / dedup / canonical sort
    │   ├── canonical.rs   #   sort by Display key
    │   ├── display.rs     #   Display matching Kotlin's data-class format
    │   ├── eval.rs        #   evaluate_accept_condition + evolve_accept_condition
    │   ├── neg.rs         #   logical negation (De Morgan for And/Or)
    │   ├── parse.rs       #   recursive-descent parser for Display roundtrip
    │   └── root_set.rs    #   RootSet { Empty, Single, Many(BTreeSet) }
    ├── parser/            # Step 3 — actual parser
    │   ├── mod.rs         #   exports + ParsingError enum
    │   ├── core.rs        #   Mgroup3Parser struct + 7-phase parse_step +
    │   │                  #     apply_term_action + apply_edge_action +
    │   │                  #     is_accepted + kernels_history
    │   ├── template.rs    #   AcceptConditionTemplate → AcceptCondition with
    │   │                  #     resolve_gen for the 4 reference points
    │   └── eval_with_history.rs  # evaluate_with_history (history-aware variant
    │                             #   that lives off Mgroup3Parser.kt:724-804)
    └── proto/             # generated by prost (transparent re-export)
```

Tests:

```
tests/
├── accept_condition_diff.rs  # reads fixtures/accept_condition.txt,
│                             #   asserts 94 blocks via enum equality
├── parser_diff.rs            # walks fixtures/parser/<case>/ AND
│                             #   fixtures/parser_generated/<case>/, runs parse(),
│                             #   serializes kernels_history, byte-compares
│                             #   against Kotlin-written golden.txt
└── fixtures/
    ├── accept_condition.txt           # committed
    ├── parser/<case>/...              # committed (small inline grammars)
    └── parser_generated/<case>/...    # gitignored (large grammars like
                                       #   mulang.cdg; regenerate locally
                                       #   via `bibix4 runMgroup3FixtureGen`)
```

## Running

Most commands are run from this directory.

### Tests (Rust only)
```sh
cargo test
```
Runs lib tests + accept_condition_diff + parser_diff. No JVM dependency, no
network — fixtures are checked in.

### Regenerating fixtures (requires Kotlin/bibix4)
Fixtures are written by Kotlin tests that drive `Mgroup3ParserGenerator` and
`Mgroup3Parser`. Two scopes:

- **Committed fixtures** under `tests/fixtures/parser/<case>/` — small
  inline grammars. Regenerate whenever the Kotlin semantics change so the
  goldens match. Drive via:
  ```sh
  /Users/joonsoo/Documents/apps/bibix4/bibix4 runMgroup3FixtureGen
  ```

- **Generated-only fixtures** under `tests/fixtures/parser_generated/<case>/`
  — large grammars like `mulang.cdg` whose `data.pb` and golden text run
  into tens of MB. Gitignored. The same `runMgroup3FixtureGen` action
  populates them; without that step the Rust `parser_diff` test still
  passes against the committed cases and prints a note about the missing
  generated dir.

Running the broader `runMgroup3Test` action also drives both fixture
generators along with the rest of the mgroup3 JUnit suite.

### Sanity binary
```sh
cargo run --bin sanity -- <some-parserdata.pb>
```
Prints top-level counts from a parser-data file. Useful for verifying a `.pb`
isn't malformed before plugging it into a fixture.

## Phase A — Result transfer (in progress)

### Goal
Give Kotlin a runtime path to receive `kernels_history` from Rust. Today the
fixture diff goes through text golden files. Phase A introduces a protobuf
result schema that:
- Carries the same `kernels_history` shape (one set of kernels per generation).
- Carries parse errors in a typed `oneof`.
- Is grammar-agnostic (Phase B will add grammar-specific AST messages as an
  additional oneof arm; existing decoders won't break).

### New schema — `mgroup3/schema/proto/Mgroup3ParserResult.proto`

```proto
message KernelMsg     { int32 symbol_id; int32 pointer; int32 begin_gen; int32 end_gen; }
message KernelGen     { repeated KernelMsg kernels; }     // sorted on encode
message KernelsHistoryMsg { repeated KernelGen entries; }
message ParseErrorMsg {
  message UnexpectedInput { ... expected_term_groups; int32 actual_codepoint; }
  message UnexpectedEof   { ... expected_term_groups; }
  oneof error { unexpected_input; unexpected_eof; }
}
message Mgroup3ParseResult {
  bool accepted = 1;
  oneof outcome { KernelsHistoryMsg history = 2; ParseErrorMsg error = 3; }
}
```

### New Rust modules
- `src/parser/result.rs` — `encode_parse_result(parser, Result<&ParsingCtx, &ParsingError>) -> Vec<u8>`.
- `src/bin/dump_result.rs` — CLI binary `dump_result <parserdata.pb> <input>` writing bytes to stdout.

### New Kotlin target — `mgroup3.nativeParser`
Separate from `mgroup3.parser` (which stays untouched as the reference
implementation). Lives at `mgroup3/nativeParser/kotlin/`.

```kotlin
sealed class Mgroup3NativeOutcome {
  data class Accepted(val history: List<KernelSet>) : Mgroup3NativeOutcome()
  data class Rejected(val error: ParsingError) : Mgroup3NativeOutcome()
}

fun decodeMgroup3ParseResult(bytes: ByteArray): Mgroup3NativeOutcome
fun Mgroup3NativeOutcome.kernelsHistoryOrThrow(): List<KernelSet>
```

### Channel — undecided
Phase A is channel-agnostic. Two candidates carry equal weight; the choice
lands in a follow-up step:
- **JNI** via `jni-rs`. Proven, ~20-30 ns/call. Needs hand-written glue and
  per-platform `.dylib`/`.so`/`.dll` packaging.
- **JEP 442 / FFM API** (Java 22+). Kotlin calls `extern "C"` Rust functions
  directly via `java.lang.foreign.*`. No JNI glue. Requires confirming the
  JVM target.

The interim Phase A verification uses `cargo run --bin dump_result` as a
subprocess. Throwaway — bridge code stays channel-agnostic.

### Verification
- **D1** — Rust unit tests on `encode_parse_result` for accept / UnexpectedInput
  / UnexpectedEof.
- **D2** — `Mgroup3NativeResultBridgeTest.kt`. For every fixture under
  `tests/fixtures/parser/<case>/`, shell out to `cargo run --bin dump_result`,
  decode bytes via the bridge, deep-compare `List<KernelSet>` against Kotlin
  reference. Env-var-gated (`MGROUP3_NATIVE_BRIDGE=1`).
- **D3** — feed bridge output into `CmakeDebugAst.matchStart()`; assert AST
  reconstruction works end-to-end.

## Phase B — Grammar-specific AST (future)

When mgroup3 wants the Rust side to do AST reconstruction itself:
1. Generator emits a per-grammar `.proto` file for the AST.
2. Generator emits a per-grammar Rust crate that builds the AST from
   `kernels_history` and encodes it as proto bytes.
3. `Mgroup3ParseResult.outcome` gains a new `ast` arm. Phase A decoders ignore
   it; Phase B-aware decoders branch on it to materialize typed AST instances
   directly.

The Phase A envelope is already forward-compatible (proto3 oneof rules).

## Design principles (carried throughout)

1. **Algorithm semantics from Kotlin, code style from Rust.** Mirror the
   Kotlin algorithm faithfully (especially the `else if` ordering in
   `evolve_accept_condition` and the gen-resolution table in
   `apply_term_action` / `apply_edge_action`). Diverge freely on
   implementation details (data structures, hashing, ordering, builder APIs).
2. **Semantic verification at the boundary, not byte equality.** Differential
   tests compare parsed enums and `Set<Kernel>` per generation. Internal
   choices like Rust's canonical sort key or `gen` field rename to `gen_idx`
   (reserved-word avoidance) don't leak.
3. **No JVM hash matching.** Rust uses its own canonical-sort key (currently
   `Display` string). Two encoders agree because both canonicalize.
4. **`Vec<AcceptCondition>` over `SmallVec`.** Recursive type can't inline
   into `SmallVec<[T; N]>`. Plain Vec keeps the enum simple; profile before
   reaching for `Rc` sharing.
5. **Single-threaded contract.** `RefCell<HashMap<...>>` for the term-action
   cache; document not-thread-safe in the parser's doc comment.

## Pointers

- Kotlin reference: `../mgroup3/parser/kotlin/com/giyeok/jparser/mgroup3/Mgroup3Parser.kt`
- Algorithm doc: `../mgroup3/docs/algorithm.md`
- bibix4 build entry points: `../build.bbx4`
  - `runMgroup3Test` — full mgroup3 JUnit suite (includes fixture generators)
  - `runMgroup3ParserTest` — only `Mgroup3ParserTest`
- Plan history: `~/.claude/plans/mgroup3-c-rust-fuzzy-crystal.md`
