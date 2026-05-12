# mgroup3 performance optimizations

mgroup3_claude 브랜치에서 진행한 mgroup3 parser 성능 최적화 기록.
다음 세션에서 이어서 작업 가능하도록 정리.

## Background

mgroup3 알고리즘은 mgroup2 와 같은 본질을 가지지만 자료구조가 다르다 — `PathMap = Map<PathShape, AcceptCondition>` 으로 path dedup 자동 처리.
이 dedup 이 ambiguous grammar (longest_join 같은) 에서 결정적 우위 — mgroup2/ktparser 가 path explode 로 OOM 나는 케이스를 mgroup3 가 ms 단위로 처리.
반면 dedup 의 hash/equals 비용으로 cond 가 active 한 평범한 case 에서 mgroup2 보다 느림 (1.5-2x).

벤치마크는 `ParserBenchmarkTaxonomyTest` 가 5 카테고리 (cfg_only, longest_only, lookahead_only, longest_join, cond_heavy) 로 측정. m3/kt ratio 가 주 지표.

## 적용된 최적화 (commit 순서)

1. **proto → plain Kotlin records** (3a4aee69)
   - hot path 의 `data.pathRootsMap[...]`, `term.replaceAndAppendsList` 등 protobuf accessor 가 매번 wrapper 만듦.
   - parser init 시 `ParserDataPlain` 에 한 번 변환. plain field access.
   - 효과: longest_join (size=1000) 19.97ms → 12.44ms (38% 빨라짐).

2. **main path / cond path 자료구조 통합** (6f6cd3da)
   - `mainPaths: PathMap` + `condPaths: Map<PathRoot, PathMap>` → 단일 `paths: Map<PathRoot, PathMap>`.
   - parseStep 의 step1+step2 가 단일 loop. step1b/2/6 분리 phase 사라짐.
   - 알고리즘 명료성 + 약간의 perf 효과.

3. **SmallAnd/SmallOr (size 2-4) + LargeAnd/LargeOr (size 5+)** (cddf5b6d)
   - `And(conds: Set)` 의 HashSet allocation 회피 — 보통 2 element 인 case 가 dominant.
   - `SmallAnd(a, b, c?, d?)` 는 fields only.
   - canonical ordering: hashCode 오름차순 + toString tie-break.
   - 효과: lookahead_only 1.39 → 1.07, cond_heavy 1.79 → 1.67.

4. **PathRoot inline value class (packed Long)** (4f0faf70)
   - `data class PathRoot(symbolId, startGen)` → `@JvmInline value class PathRoot(packed: Long)`.
   - equals/hashCode 가 Long 의 그것 — fast. allocation 없음 (boxed in Set/Map 시 정도).
   - 효과: 모든 cond-active 케이스 13-23% 빨라짐 (size=1000).

5. **AcceptCondition.referencedRoots + hasFromNextGen metadata** (7ca3ebed)
   - 각 condition node 가 자기 reference 하는 PathRoot 들의 `RootSet` (Empty/Single/Many) 와 fromNextGen flag 를 init 에 eager 계산.
   - step3 의 `extractRootsFromCond` 와 step6 의 `collectReferenced` 의 tree traversal 사라짐 (cond.referencedRoots.forEach 만).
   - `hasFromNextGen` 는 미사용 — (5) dirty-root skip 의 prerequisite (현재 폐기).
   - 효과: cfg_only 0.94 → 0.72, lookahead_only 1.24 → 1.08.

6. **activeCondRoots = nextPaths.keys 직접 view** (d516504e)
   - step5 evolve 직전 `nextPaths.keys.filterTo(HashSet()) { it != mainRoot }` 매 step new HashSet 할당.
   - leaf condition 의 root 가 mainRoot 가 될 일 거의 없고, 된다면 active 로 보는 게 옳음 → `nextPaths.keys` set view 그대로 사용.
   - 효과 (m3@1000): longest_only 8.66 → 7.27ms, longest_join 11.47 → 10.24ms, cond_heavy 9.86 → 8.83ms (m3/kt 1.53→1.44).

7. **mutable sets: LinkedHashSet → HashSet** (이번 commit)
   - `mutableSetOf<>()` 는 Kotlin stdlib 에서 LinkedHashSet 반환 — ordered iteration 보장 위한 doubly-linked list overhead.
   - parseStep 의 observingOut / newCondRoots / referencedRoots 는 lookup-only set — ordering 불필요.
   - `HashSet<>()` 로 직접 생성.
   - 단독으로는 cfg_only ratio 약간 개선 외엔 noise 수준 (set 이 보통 작아 linked-list 비용 미미).
   - **참고**: HashMap 들도 LinkedHashMap → HashMap 시도해봤으나 회귀 (longest_only/lookahead_only ~20% 느려짐). iteration order 가 evolve 의 cond merge / Or 결합 path 에 영향 — LinkedHashMap 유지.

## 측정 환경 메모

지금까지의 측정은 모두 노트북 (macOS) 에서 수행. 시스템 부하에 따라 동일 코드 측정값이 ±30% 변동 (cfg_only m2: 5.79ms ↔ 9.05ms). m3/kt ratio 비교가 단일 ms 보다 안정적. 폐기된 아이디어 중 효과가 ambiguous 했던 것들은 더 안정적인 환경 (데스크탑 / 서버 / 노트북 idle 상태) 에서 재측정 시 결과 다를 수 있음.

## 시도했지만 폐기한 아이디어

- **MilestonePath intern table** — intern table 의 lookup 비용이 dedup hash 비용보다 큼. miss rate 가 높아 회귀. (PathMap dedup 이 이미 가장 큰 dedup 위치라 추가 intern 효과 작음.)
- **Long packed edge action keys** — 효과 noise 안.
- **MilestonePath.referencedRoots cache (시간→공간)** — lookahead_only/cond_heavy 회귀. cache hit rate 가 낮은 듯. (1) 의 metadata cache 와 함께 다시 시도하면 다를 수도 있음.
- **PathRoot/Kernel 의 Long packing as edge action key** — 효과 noise.
- **Dirty-root evolve skip** (referencedRoots + hasFromNextGen 활용) — 매 step `affectedRoots` 계산 + `isSubsetOf` / `isDisjoint` 호출이 skip 으로 절감하는 비용보다 큼. 98/0 통과는 했으나 cfg_only / cond_heavy 약간 회귀. cond_heavy 의 step5 evolve 14% 인데 실제 skip 가능한 비율이 낮아 본전 못 뽑음.
- **Lazy ReferencedRootCollector** (18e20695 이후 재시도) — step3 incremental discovery 가 매 step `HashSet<PathRoot>` 를 만드는 것을 피하려고, referenced root 가 실제 생길 때만 set 을 생성하는 collector 로 변경. step3 자체는 30.3ms → 26.6ms 로 줄었지만 전체 profile 은 305ms → 321ms, `runParserBenchmark` size=1000 m3 median 은 7.688ms → 8.475ms 로 회귀. collector 분기/indirection 비용이 절감분보다 큼. 되돌림.
- **Same-step evolve IdentityHashMap memoize** (939000a2 이후 시도) — 같은 parseStep 안에서 동일 `AcceptCondition` 객체의 evolve 결과를 재사용하려고 `IdentityHashMap<AcceptCondition, AcceptCondition>` 를 추가. `runMgroup3ParserTest` 는 통과했지만 step5_evolve 가 23.3ms → 25.5ms 로 증가했고, `runParserBenchmark` size=1000 m3 median 이 7.216ms → 8.045ms 로 회귀. identity hit rate 가 낮고 map lookup 비용이 더 큼. 되돌림.
- **TermAction cache null sentinel** (939000a2 이후 시도) — nullable `termActionCache` 의 `containsKey` + `get` 이중 lookup 을 피하려고 `NoTermAction` sentinel 과 `HashMap<Long, Any>` 로 변경. `runMgroup3ParserTest` 는 통과했지만 `runParserBenchmark` size=1000 m3 median 이 7.216ms → 8.206ms 로 회귀. `findApplicableAction` 자체가 현재 주요 병목이 아니고, `Any` cast/sentinel 분기 비용이 절감분보다 큼. 되돌림.

## 현재 benchmark (size=1000, activeCondRoots fix 적용 후)

| case             | m2 (Scala) | kt (port) | m3       | m3/kt |
| ---------------- | ---------- | --------- | -------- | ----- |
| cfg_only         | 3.09ms     | 2.51ms    | 2.35ms   | 0.94x |
| longest_only     | 9.33ms     | 5.84ms    | 7.27ms   | 1.24x |
| lookahead_only   | 5.05ms     | 4.20ms    | 4.28ms   | 1.02x |
| longest_join     | OOM        | OOM       | 10.24ms  | —     |
| cond_heavy       | OOM        | 6.12ms    | 8.83ms   | 1.44x |

cfg_only / lookahead_only 는 m3 ≈ kt. longest_only / cond_heavy 가 1.24-1.44x — 남은 cost 분석 필요.

(taxonomy test 가 이제 `/tmp/jparser-benchmark-taxonomy.txt` 에도 결과 기록 — bibix4 stdout streaming 의 누락 회피.)

## 남은 개선 아이디어

### High priority (시도할만)

#### (3) step3 incremental discovery
- 지금 step3 의 새 cond root 수집은 `nextPaths.values.forEach { ... }` 전체 scan.
- applyTermAction / addPath 에서 새 condition 추가 시점에 그 condition.referencedRoots 를 outparam set 에 누적.
- (1) 의 metadata 가 있으면 cache hit 으로 빠르지만, scan 자체 비용도 절감 가능.
- 효과 marginal 일 수 있음 — 측정 후 결정.

#### (2) MilestonePath.observingReferencedRoots cache (재시도)
- 이전에 단독 시도 시 lookahead_only/cond_heavy 회귀.
- (1) 의 metadata 와 함께 적용하면 step6 의 chain walk 도 사라짐 — 결과 다를 수 있음.
- compact representation 신중 (Empty/Single/Small/Large).

### Medium priority

#### (6) Same-step evolve memoize
- `evolveCache = HashMap<AcceptCondition, AcceptCondition>` per parseStep.
- 같은 condition 이 여러 path 에 등장하면 한 번만 evolve.
- (5) 의 lite version — (5) 가 더 강력하지만 (6) 은 안전 / 단순.

#### applyTermAction body 자체의 hot spot 분석
- 최신 profile 의 24% 차지. 어떤 sub-method 가 dominant 인지 분해 필요.
- profile sample 늘려서 deeper trace.

### Low priority / 마무리

#### (4) Reference count 기반 step6 prune
- PathEntry 에 metadata 추가 + add/remove 시 ref count 갱신.
- 복잡도 vs 효과 안 좋음. 마지막 또는 skip.

#### HashMap initial capacity 힌트
- 매 step 의 mutable map/set 들이 default 16 capacity 로 시작.
- 일부에 8 또는 32 hint 주면 resize 줄어듦.
- 작은 효과.

#### Per-parser buffer pool
- 각 step 의 mutable map 들 (rootProgresses, observingOut 등) 을 매번 new 대신 clear 후 reuse.
- ctx 들어가는 자료구조 (history, nextPaths) 는 reuse 불가 — 그 외만.

## Debug / Tools

- `parser.enablePhaseTiming()` + `parser.reportPhaseTimers()` — parseStep 의 8 phase 별 시간.
- `parser.setTrace(gen)` — 특정 step 의 path/condition state dump.
- `evolveTrace` 전역 변수 — evolve 의 step-by-step 출력.
- `ParserBenchmarkTaxonomyTest` — 5 카테고리 m2/kt/m3 비교. `runParserBenchmarkTaxonomy` action 으로 실행.
- `Mgroup3ProfilingTest` — sampling profiler + phase timing. `runMgroup3Profile` action 으로 실행.
- `ParserBenchmarkTest` — 단일 grammar benchmark. `runParserBenchmark` action 으로 실행.
- 위 세 test 는 `runMgroup3Test` 의 exclude filter (`.*(Benchmark|Profiling).*`) 로 일반 test run 에서는 자동 skip — `@Disabled` 불필요.

## 최신 profile snapshot (commit 7ca3ebed 직전, cond_heavy)

```
phase timing: step1=34.5% step1b=9.6% step2=0.4% step3=21.5% step4=1.8%
              step5_evolve=14.4% step6_prune=9.9% step7_history=7.8%

Top exclusive (mgroup3 frames):
  parseStep                48%
  applyTermAction          24%
  applyEdgeAction           7%
  evolveAcceptCondition     4%
  parseStep$collectFromShape 3.6%
```

대부분 parseStep + applyTermAction body 자체. (1) 적용 후 step3/6 의 traversal 비용은 줄어들었을 것 — 다시 측정 필요.

## 파일 위치

- `mgroup3/parser/kotlin/com/giyeok/jparser/mgroup3/AcceptCondition.kt` — condition types, RootSet.
- `mgroup3/parser/kotlin/com/giyeok/jparser/mgroup3/ParsingCtx.kt` — ParsingCtx, PathShape, MilestonePath, PathRoot.
- `mgroup3/parser/kotlin/com/giyeok/jparser/mgroup3/Mgroup3Parser.kt` — parseStep, applyTermAction, applyEdgeAction, evolve.
- `mgroup3/parser/kotlin/com/giyeok/jparser/mgroup3/Mgroup3ParserDataPlain.kt` — protobuf wrapper.
- `mgroup3/test/kotlin/com/giyeok/jparser/mgroup3/ParserBenchmarkTaxonomyTest.kt` — 5-category benchmark.
- `mgroup3/test/kotlin/com/giyeok/jparser/mgroup3/Mgroup3ProfilingTest.kt` — profiler.
