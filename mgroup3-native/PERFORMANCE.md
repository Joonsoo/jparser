# Performance — findings & open work

Last updated after step 6.6 (FxHash switch) on branch `mg3native-claude`.

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
- **Single change — std `HashMap` → `rustc_hash::FxHashMap` everywhere** —
  brought Rust to **1.16× to 1.77× *faster*** than Kotlin on every measured
  workload. Standalone CLI shows 2-3× per-input speedup.
- Profile still shows `evaluate_with_history` / `collect_finishes_at_or_after`
  as the top hot spot, but no longer dominated by hashing. Further work is
  algorithmic (memoize / invert the cond-path-finishes index) rather than
  data-structure-substitution.

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
| (06_match still measuring as of doc write) | | | | |

Standalone CLI (Rust only) shows the same speedup from FxHash without any
JVM in the path:

| Input | Before FxHash | After FxHash | Speedup |
|---|---|---|---|
| 09_exprs | 3620 ms | 1280 ms | 2.8× |
| 07_try_let | 2420 ms | 1140 ms | 2.1× |
| 06_match | 4720 ms | 1520 ms | 3.1× |

Bonus measurement: `parser_diff` integration test (14 cases / 75 inputs
including all of mulang) dropped from ~74 s to ~19 s on release.

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

These are the obvious follow-ups, in priority order. Numbers are estimated
based on the pre-FxHash profile and likely remain the hot spots after
FxHash (just less dominant).

### 1. Cache `kernels_history` finishes by root (highest ROI)

`collect_finishes_at_or_after(history, root, min_gen)` walks the entire
`history` Vec on every call. `evaluate_with_history` recurses through
every leaf in every finished kernel's condition, so this is called O(N ×
M) times for a parse of N gens with M leaves per condition. Each call is
O(N) over history.

Fix: at the start of `kernels_history`, build an inverted index `HashMap<PathRoot,
Vec<(gen_idx, &AcceptCondition)>>` from the history once. Then
`collect_finishes_at_or_after` becomes O(1) lookup + O(k) filter, where
`k` is the number of finishes for that root.

Expected payoff: another 2-3× on `kernels_history`-dominated inputs.

### 2. Memoize `evaluate_with_history` (medium ROI)

A given (condition, history-tail) pair is evaluated multiple times if the
same leaf appears in multiple kernel conditions, or if `kernels_history`
loops include redundant subtrees. Memoize by `(condition pointer, history
length)`.

Expected payoff: workload-dependent. Larger savings on inputs where
condition trees overlap a lot (left-recursive grammars).

### 3. Skip protobuf encode when caller only wants accept/reject (medium ROI)

`mgroup3_parser_parse` currently always calls `encode_parse_result`, which
runs `kernels_history` + protobuf encode. For benchmarks and "did this
parse succeed?" callers, that's wasted work. Add a sibling FFI
`mgroup3_parser_parse_accept_only(...) -> i32` that returns 0/1/error
without producing any output buffer. The benchmark already shows the
difference: Kotlin `parse-only` is faster than `parse+kernels_history` by
roughly 5× on big inputs.

Expected payoff: makes the FFM `Mgroup3NativeParser.isAccepted(input)` use
case match the Kotlin one in cost.

### 4. Vec<AcceptCondition> → Rc<AcceptCondition> sharing in And/Or (low ROI, big change)

Currently `And { items: Vec<AcceptCondition> }` deep-clones children on
every `and_from` / `or_from` / `neg` / `evolve` recursion. Switching to
`Vec<Rc<AcceptCondition>>` would let `evolve` reuse subtrees that didn't
change. But this is a larger refactor (touches every `match` arm,
`Display`, `parse`, equality), and the profile doesn't currently flag
clone overhead as dominant. **Defer until evidence demands it.**

### 5. Reusing the term_action_cache across parses (very small ROI)

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
