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
