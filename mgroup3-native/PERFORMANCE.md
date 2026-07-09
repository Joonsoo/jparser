# Performance — findings & open work

Last updated after the inverted-index + Ord-canonical_sort follow-ups on
branch `mg3native-claude`.

This document captures what was learned about mgroup3-native's wall-clock
performance vs the Kotlin reference, why earlier numbers were misleading,
what was fixed, and what's still on the table. Pick this up at the top when
restarting performance work.

## TL;DR

- **Initial benchmark looked catastrophic** (kt/rs 0.07–0.42×, i.e. Rust
  5-14× slower than Kotlin) but was measurement-bug. Kotlin's `parse()` was
  timed alone; Rust's `parse()` always materialized `kernels_history` +
  encoded protobuf because that work lives inside the FFI `encode_parse_result`.
- **Fair benchmark** (both sides materialize `kernels_history`) on the
  unmodified code: kt/rs 0.54–0.76×, i.e. Rust ~1.3-1.9× slower.
- **Step 6.6 (FxHash, commit `da9d2999`)** brought Rust to **1.16× to
  2.46× *faster*** than Kotlin (FFM channel). Standalone CLI 2-3×.
- **Step 6.7 (inverted index for `cond_path_finishes`, commit `05a1f249`)**
  collapsed `collect_finishes_at_or_after` from O(N) history-scan-per-leaf
  to O(1)+O(k). `parser_diff` integration test 19s → 7.15s.
- **Step 6.8 (canonical_sort uses derived `Ord`, commit `1757131d`)** —
  the post-6.7 profile flagged `Display::fmt`+`RawVec::reserve` inside
  `and_from`'s `sort_by(|a,b| a.to_string().cmp(...))`. Adding
  `PartialOrd/Ord` derives on `AcceptCondition` and switching to
  `children.sort()` eliminated the per-comparison String allocation.
  `parser_diff` 7.15s → 3.82s on top of 6.7.
- **End state (FFM JVM channel):** Rust **1.44× to 10.26× faster** than
  Kotlin across mulang inputs. Standalone CLI 540–620 ms vs 3620–4720
  ms pre-FxHash (4–9× cumulative on big inputs).
- Memoizing `evaluate_with_history` is **not pursued** — after 6.7 it is
  no longer dominant in the profile.

## Timeline & numbers

### Pre-fix profile

`sample` of `target/release/dump_result mulang/data.pb 06_match.mu` for 3
seconds (commit `b08df248`, before FxHash):

```
Sort by top of stack:
  evaluate_with_history / collect_finishes_at_or_after   1007
  DefaultHasher::write (SipHash)                          822
  hash_one                                                676
```

Bottleneck: `cond_path_finishes` is a `HashMap<PathRoot, AcceptCondition>`
inside every `HistoryEntry`. Each leaf condition in `kernels_history`
recursively calls `collect_finishes_at_or_after`, which walks every history
entry and does `entry.cond_path_finishes.get(&root)` per entry. `O(history
length × leaves per condition × hash cost)`. SipHash on a 64-bit
`PathRoot` is ~30 cycles; doing it billions of times dominates.

### Bench numbers — single change effects

All measurements on mulang fixtures (`tests/fixtures/parser_generated/mulang/`)
using `Mgroup3NativeBenchmarkTest.kt` with 30 measure rounds + 10 warmup,
release build, GC + 5ms sleep between rounds. Both sides materialize
`kernels_history` (this was the bug fix referenced in TL;DR — see
`Mgroup3NativeBenchmarkTest.kt` for the fair-comparison invocation).

| Input | chars | Kotlin median | Rust median | kt/rs ratio |
|---|---|---|---|---|
| **Phase 1: unfair (Kotlin parse-only vs Rust parse+history+proto)** | | | | |
| annotation_defs | 118 | 3.123 ms | 7.360 ms | 0.42× |
| 09_exprs | 1374 | 466.520 ms | 3472.477 ms | 0.13× |
| 07_try_let | 1495 | 408.921 ms | 2182.455 ms | 0.19× |
| 06_match | 1843 | 329.049 ms | 4607.956 ms | 0.07× |
| **Phase 2: fair, no FxHash** | | | | |
| annotation_defs | 118 | 7.623 ms | 10.046 ms | 0.76× |
| 09_exprs | 1374 | 1875.582 ms | 3455.855 ms | 0.54× |
| 07_try_let | 1495 | 1517.581 ms | 2181.135 ms | 0.70× |
| **Phase 3: fair + FxHash (commit `da9d2999`)** | | | | |
| annotation_defs | 118 | 6.417 ms | 5.541 ms | **1.16×** |
| 09_exprs | 1374 | 1825.070 ms | 1029.465 ms | **1.77×** |
| 07_try_let | 1495 | 1545.851 ms | 905.662 ms | **1.71×** |
| 06_match | 1843 | 3150.976 ms | 1283.389 ms | **2.46×** |
| **Phase 4: fair + FxHash + inverted-index + Ord-sort (commit `1757131d`)** | | | | |
| annotation_defs | 118 | 6.051 ms | 4.426 ms | **1.44×** |
| 09_exprs | 1374 | 1966.519 ms | 338.506 ms | **6.05×** |
| 07_try_let | 1495 | 1496.359 ms | 339.770 ms | **4.46×** |
| 06_match | 1843 | 3152.894 ms | 312.000 ms | **10.26×** |

Headline: **Rust beats the Kotlin reference 1.44–10.26× via FFM**, with
the lead widening sharply on larger workloads. The big-input wins come
from the inverted index (no more O(history) scans per leaf) and from
removing String allocations in `and_from`'s sort.

Standalone CLI (Rust only, no JVM in the path):

| Input | Pre-FxHash | Post-FxHash | Post-invIndex | Post-Ord | Cumulative |
|---|---|---|---|---|---|
| 09_exprs | 3620 ms | 1280 ms | 900 ms | 590 ms | **6.1×** |
| 07_try_let | 2420 ms | 1140 ms | 930 ms | 620 ms | **3.9×** |
| 06_match | 4720 ms | 1520 ms | 850 ms | 540 ms | **8.7×** |

`parser_diff` integration test (14 cases / 75 inputs including mulang):
~74 s → ~19 s → 7.15 s → **3.82 s** on release.

## Why the unfair benchmark was misleading

`Mgroup3NativeBenchmarkTest.kt` originally measured:

```kotlin
ktTimes[i] = measureNanoTime { ktParser.parse(input.text) }
rsTimes[i] = measureNanoTime { native.parse(input.text) }
```

These look symmetrical but `native.parse(input)` is the FFM facade, which
under the hood:

1. Calls Rust `mgroup3_parser_parse`.
2. Inside that, `encode_parse_result` is invoked, which **calls
   `kernels_history(ctx)`** and protobuf-encodes the result.
3. Returns bytes to Kotlin, which decodes them back into `List<KernelSet>`.

Kotlin's `Mgroup3Parser.parse(text)` does not materialize the history. So
the original benchmark was timing apples (`parse`) against oranges (`parse
+ kernels_history + encode + decode`).

The fix is one line: add `ktParser.kernelsHistory(ctx)` to the Kotlin path
inside the timed block. After that, kt and rs do equivalent work and the
ratio jumps from 0.07–0.42× to 0.54–0.76×.

## Why FxHash mattered so much

mgroup3-native uses `HashMap` / `HashSet` keyed by small fixed types:

- `PathRoot` — packed `(i32, i32)`, 8 bytes.
- `KernelTemplatePair` — `(i32, i32)`, 8 bytes.
- `PathShape` — `Option<Rc<MilestonePath>> + i32`, with a precomputed
  `u64` hash cached on the value.

Rust's default `HashMap` uses **SipHash-1-3** for DoS resistance. SipHash
processes a 64-bit input in ~30 cycles. **FxHash** processes the same
input in ~3-4 cycles (`rotate + xor + multiply` per word). For our keys
that's a 5-8× per-hash speedup, which compounds across the millions of
lookups in `evaluate_with_history`, `term_action_cache`, `paths`,
`cond_path_finishes`, etc.

Tradeoff: FxHash is not DoS-resistant. Acceptable here because parser
inputs come from a generator, not an untrusted network source. The Kotlin
reference's `HashMap` is `java.util.HashMap`, which uses a fast `int`
hash (Java's `hashCode()` then mix), so this change just removes a Rust
penalty that didn't exist on the JVM side.

Implementation: `use rustc_hash::{FxHashMap as HashMap, FxHashSet as
HashSet}` in every hot file. `HashMap::new()` → `HashMap::default()`,
`HashMap::with_capacity(n)` → `HashMap::with_capacity_and_hasher(n,
Default::default())`. Mechanical only — no algorithm change.

## What's still on the table (next session)

### Done in this round

- **Inverted index for `cond_path_finishes`** — landed in `05a1f249`.
  `HistoryIndex::build` runs once per `is_accepted` / `kernels_history`,
  and `evaluate_with_history` now uses `partition_point` over a sorted
  per-root list instead of scanning history.
- **Ord-based canonical_sort** — landed in `1757131d`. Derive
  `PartialOrd/Ord` on `AcceptCondition`; switch `canonical_sort` to
  `children.sort()`. Removes `to_string()` allocation per comparison.

### Load-cost: rkyv cache for parserdata — landed

The per-process fixed cost paid *before* the first parse was dominated by
**prost decode** of the parserdata proto (mulang: 6.8 MB gz → 110 MB proto).
bibix4 pays this on every CLI invocation. We now cache the *`from_proto`
result* (`ParserDataPlain`) as a zero-copy rkyv archive in a sibling
`<parserdata>.pb.gz.rkyv` file and restore it directly on warm loads,
bypassing gunzip + prost decode entirely.

Design:
- rkyv 0.8, deriving `Archive`/`Serialize`/`Deserialize` on every type
  reachable from `ParserDataPlain` (plain structs in `parser_data.rs` +
  the embedded prost templates, whose derives are injected via
  `build.rs` `type_attribute`). `Arc<…>` fields use rkyv's shared-pointer
  dedup, so the restored graph shares exactly like `from_proto`.
- **Warm load is mmap-based (cache format v2).** The 64-byte cache header is
  padded so the payload starts at a 16-aligned offset; the loader `mmap`s the
  file and hands the payload slice straight to rkyv `access_unchecked` (mmap
  base is page-aligned ⇒ payload is 64-aligned in memory). No `fs::read`, no
  `AlignedVec` copy. The archive is faulted in lazily as `deserialize` walks it.
- Cache header (64 B, LE): magic (`MG3RKYV2`) + `PLAIN_SCHEMA_VERSION` + flags +
  xxh3 of the source file + xxh3 of the payload + payload_len + reserved pad.
  Version/source-hash mismatch, absence, degenerate/short file, or corruption
  → silent fall back to the proto path + best-effort cache rewrite
  (tempfile + atomic rename; skipped if the dir is read-only).
- **Derived data is not archived.** `transitive_initial_cond_symbols` is a map
  computed by `from_proto` from `path_roots`; it carries `#[rkyv(with = Skip)]`
  and is recomputed on load via `ParserDataPlain::recompute_derived`
  (sub-ms). Keeps the on-disk archive free of derived state. Size effect is
  negligible here (mulang: 1,816 bytes = 0.0006% of the archive) — see the
  breakdown below for why: the archive is dominated by `added` kernel templates,
  not this map.
- **Validation choice — measured & demoted.** Payload xxh3 verification is now
  **opt-in** (`MG3_CACHE_VERIFY_PAYLOAD=1` env, or `FORCE_VERIFY_PAYLOAD`
  const); the default gate is source-hash + version only. Rationale: the
  payload hash walks all 300 MB of pages, defeating mmap's lazy-fault win and
  ~doubling warm load (≈110 ms → ≈210–265 ms measured). Integrity is instead
  guaranteed by (1) atomic rename (no partial writes are ever observed) and
  (2) the source-hash+version gate (stale/mismatched caches rejected). The
  cache is a trusted same-crate artifact. bytecheck (`rkyv::access`) is still
  wired via `VERIFY_WITH_BYTECHECK` for the paranoid path.

**Archive size breakdown** (`target/release/archive_breakdown <pb.gz>`,
mulang, 309 MB total). Per top-level field, serialized in isolation:

| field | size | share |
|---|---|---|
| `term_actions` | 128.5 MB | 41.6% |
| `tip_edge_actions` | 104.7 MB | 33.9% |
| `mid_edge_actions` | 75.6 MB | 24.5% |
| `milestone_groups` | 0.22 MB | 0.07% |
| `path_roots` | 0.07 MB | 0.02% |
| `transitive_initial_cond_symbols` (Skip'd) | 1.8 KB | 0.0006% |
| `lookahead_cond_symbols` | 60 B | ~0 |

Drilling into the three big collections, the dominant cost is the
`parsing_actions.added` kernel-template lists: **142.5 MB** across the edge
actions + **95.9 MB** across the term actions = **~238 MB (77%)** of the whole
archive (~5.6 M `AddedKernelTemplate`s). `added` is used only to materialize
`kernels_history` (reporting), never for accept/reject — but it *is* part of
the observable output the equivalence test pins, so it cannot be dropped
without changing semantics. This is why trimming the derived map moves nothing:
the size lives in `added`, which is real (if reporting-only) content.

Measured (release, Apple Silicon, mulang `mulang-mg3-parserdata.pb.gz`,
`target/release/time_load … 5`; measured with no concurrent cargo/rustc):

| path | cost |
|---|---|
| **proto total** (read + gunzip + prost_decode + plain+index) | **~510–580 ms** (prost_decode alone ~410–470 ms) |
| **rkyv cold** (cache miss → proto path + write 309 MB cache) | ~0.66–0.90 s |
| **rkyv warm — v1** (fs::read + payload xxh3 + AlignedVec copy + deserialize) | ~140–380 ms |
| **rkyv warm — v2** (mmap + source-hash gate + access + deserialize + recompute) | **~108–135 ms** (steady, page-cache hot) |

v2 warm breakdown (steady state): `mmap` ≈ **25–45 µs**, source-hash ≈
**190–250 µs**, `access_unchecked` ≈ **0–42 ns**, `deserialize` (rebuild owned
`ParserDataPlain`) ≈ **99–150 ms** (now the *entire* warm cost), recompute of
the derived map ≈ **8–20 µs**. The v1 costs that mmap eliminated — fs::read of
309 MB (~45–56 ms), payload xxh3 (~12–21 ms), AlignedVec copy (~10–38 ms) —
are gone.

Net: warm load is now **~4–4.9× faster** than the proto path (was ~2× in v1).
Equivalence is covered by `tests/cache_equivalence.rs` (proto-built vs
cold-cache vs warm-cache parsers produce identical `is_accepted` +
`kernels_history` on all committed fixtures, plus degenerate-cache fallback
tests for the mmap path; cache files go to a tempdir, never the fixture tree).
Reproduce with `cargo build --release --bin time_load` then
`target/release/time_load <parserdata.pb.gz> <iters>`.

**Zero-copy lower bound (Task 4, exploratory).** If the parser ran directly off
`ArchivedParserDataPlain` with no `deserialize`, the fixed warm cost would be
just mmap + source-hash gate + `access_unchecked` + touching a few fields:
measured at **~240–300 µs** (`time_load`'s `WARM*` line). That is ~**400×**
below the current ~110 ms warm path and ~**2000×** below the ~530 ms proto
path — i.e. `deserialize` *is* the whole warm cost, and eliminating it is the
only remaining big win. Doing so means rewriting the parser hot path to read
`Archived*` types (endian-wrapped scalars, `ArchivedVec`/`ArchivedHashMap`,
`ArchivedArc`) instead of owned ones; that is a large, semantics-sensitive
refactor and remains out of scope. The µs-level lower bound quantifies the
prize.

FFI: `mgroup3_parser_new_from_file_cached(path, err)` (the original
`mgroup3_parser_new_from_file` is unchanged). Rust: `Mgroup3Parser::from_plain`
+ `parser_cache::{load_cached, write_cache, load_plain_from_file}` +
`ParserDataPlain::recompute_derived`.

Still open here: `deserialize` (~100–150 ms rebuilding the owned graph)
dominates the warm path — see the zero-copy lower bound above for the ceiling.
The archive is ~2.8× the uncompressed proto (309 MB vs 110 MB), almost all of
it the reporting-only `added` kernel templates.

### Dropped

- **Memoizing `evaluate_with_history`** — after the inverted index lands,
  the function no longer dominates the profile and the inputs we measured
  don't show obviously redundant subtree evaluations. Skip unless a
  future workload re-promotes it.

### Still open

#### 1. Skip protobuf encode when caller only wants accept/reject (medium ROI)

`mgroup3_parser_parse` currently always calls `encode_parse_result`, which
runs `kernels_history` + protobuf encode. For benchmarks and "did this
parse succeed?" callers, that's wasted work. Add a sibling FFI
`mgroup3_parser_parse_accept_only(...) -> i32` that returns 0/1/error
without producing any output buffer.

Expected payoff: makes the FFM `Mgroup3NativeParser.isAccepted(input)` use
case match the Kotlin one in cost, and matters most for short inputs
where `kernels_history` + protobuf is a relatively big slice.

#### 2. Vec<AcceptCondition> → Rc<AcceptCondition> sharing in And/Or (low ROI, big change)

Currently `And { items: Vec<AcceptCondition> }` deep-clones children on
every `and_from` / `or_from` / `neg` / `evolve` recursion. Switching to
`Vec<Rc<AcceptCondition>>` would let `evolve` reuse subtrees that didn't
change. But this is a larger refactor (touches every `match` arm,
`Display`, `parse`, equality, and the new `Ord` derive). The current
profile doesn't flag clone overhead as dominant. **Defer until evidence
demands it.**

#### 3. Reusing the term_action_cache across parses (very small ROI)

`Mgroup3Parser` clears its `RefCell<HashMap>` cache between parses via
construction. But the cache is keyed by `(tip_group_id, char)`, which is
parser-data-specific (not parse-instance-specific). For benchmark loops
that parse the same data many times, the cache survives between calls in
the current implementation already — this is a non-issue.

## Reproducing the measurements

The benchmark is gated and runs from bibix4:

```sh
# Make sure mulang fixtures exist (regenerate if absent).
/Users/joonsoo/Documents/apps/bibix4/bibix4 runMgroup3FixtureGen

# Build cdylib (release) and run the FFM benchmark.
/Users/joonsoo/Documents/apps/bibix4/bibix4 runMgroup3NativeBenchmark
```

Standalone CLI:

```sh
cd mgroup3-native
cargo build --release
for f in 01_annotation_defs 09_exprs 07_try_let 06_match; do
  /usr/bin/time -p target/release/dump_result \
    tests/fixtures/parser_generated/mulang/data.pb \
    tests/fixtures/parser_generated/mulang/inputs/$f/input.txt > /dev/null
done
```

Profile:

```sh
target/release/dump_result tests/fixtures/parser_generated/mulang/data.pb \
  tests/fixtures/parser_generated/mulang/inputs/06_match/input.txt > /dev/null &
PID=$!; sleep 1; sample $PID 3 1 -mayDie; wait $PID
```

`Mgroup3NativeBenchmarkTest.kt` is the canonical JVM-side measurement
harness. It uses `measureNanoTime` + 30 measure rounds + 10 warmup, the
same convention as `ParserBenchmarkTest.kt`.

## Notes & caveats

- **Channel overhead is real on tiny inputs.** For `annotation_defs` (118
  chars), JVM ↔ FFM round-trip + protobuf is the bulk of the latency, so
  the ratio is much closer to 1 than on larger inputs. This is expected
  and acceptable.
- **GC affects Kotlin variance.** Even with `System.gc()` between rounds,
  Kotlin standard deviation is noticeably higher than Rust's. The medians
  are what to report.
- **Mulang grammar uses pure CFG features** (no `lookahead`/`except`/`join`).
  Boolean-grammar workloads (e.g. mulang's full grammar with the cond
  paths) may have a different ratio profile. Worth re-measuring once a
  conditional-heavy input enters the corpus.
- **Don't trust "Rust is X× slower than ANTLR4" claims without verifying
  the comparison setup.** Our own benchmark showed Rust 14× slower than
  Kotlin until we noticed the asymmetry. Always sanity-check that both
  sides do the same work.
