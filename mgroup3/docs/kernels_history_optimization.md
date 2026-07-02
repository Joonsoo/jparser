# kernels_history 최적화 — replay 재생을 leaf-직접 조회 + 메모로 대체

작성: 2026-07-02. 이 문서는 다음 세션이 이 작업을 이어받기 위한 설계/실행 문서다.

## 0. 결과 (2026-07-02 구현 완료)

구현: Kotlin `RecordConditionEvaluator.kt` + Rust `parser/record_cond.rs` (커밋
3ebed5ef, d4ba5ae5). 설계 §3 그대로 + 두 가지 추가 미묘함:
- **pending-drop**: replay 는 root 가 비활성인 step 에서 leaf 를 즉시 확정하므로,
  fin 스캔 창을 root 활성 구간 기준 [fromGen, lastActive+1] 로 제한해야 동치
  (everSeenCondRoots 의 no-respawn 규칙이 활성 구간 연속성을 보장).
- **visiting**: NLM/NeedLM 은 둘 다 Always (쌍대 아님), 나머지 leaf 는
  `evalCond(cond, fromGen+1, ∅)` 로 환원.

검증: `MG3_RECORD_COND_DIFF=1` (양쪽 병행 실행 + 불일치 throw) 로 Kotlin 전
스위트 (127/0 + corner + advanced + m2 parity) 와 cargo test 전부 그린 —
golden 재생성 없음. mulang `parser.generate` + `parser.test:test` 그린
(testMlc 실패는 기존 이슈).

성능 (release, mulang 문법, bibix4 stdlib 코퍼스): kernels_history
junit.bbx 4.4s→0.30s, ktjvm 6.9s→0.28s, cc 4.6s→0.67s, maven 3.4s→0.78s.
parser_diff (release) wall 4.5s.

**단, 1차 목표 (native 가 mg2 를 이긴다) 는 미달**: --profile-startup
Phase 1+2 = 35.5s (구 native 40–44s, mg2 19.1s). hist 제거 후 남은 병목은
**파스 자체** — 특히 jar.bbx (4.7KB) 가 parse 15s (json.bbx 3.8KB 는 67ms —
입력 내용에 따른 파스 핫패스 병리). cc.bbx 5.5s, jparser build.bbx4 5.1s.
다음 arc 는 파스 페이즈의 병리 입력 프로파일링 (time_parse 로 재현 가능:
`tests/fixtures/parser_generated/mulang/data.pb` + jar.bbx).

### 0.1 파스 페이즈 병목의 근본 원인 (2026-07-03 분석 — 도구: profile_steps/symdump)

**jar.bbx 병리는 mg2 도 공유한다** (mg2 warm: parse 8.1s + kernelsHistory 8.3s
= 16.4s — mg2 의 19.1s wall 자체가 jar.bbx 스트래글러 지배). 즉 mg3 알고리즘
고유 결함이 아니라 문법×입력의 상태 폭발이고, mg3 는 그걸 증폭한다.

**폭발 메커니즘 (profile_steps 실측, jar.bbx peak gen 3731)**:
- step 시간은 live path shape 수에 비례. peak 19,450 shapes (mg2 는 같은
  지점 893 — 22×; main root 만 3,024 vs 68 — 44×). 1000+ shapes 인 스텝들이
  전체 parse 시간의 ~85%.
- shape 구성: main root 3,024 + **트레일링 람다 워처들** — `cached("..") {`
  (gen 2242) 의 `Longest(CallChain_+)`(sym792) / AddExpr·MulExpr 가드
  (sym1225/1227) 가 인접 anchor gen 별로 각각 1,512 shapes, 중첩된
  `artifact.build(outJar) {` (gen 2920) 몫이 각각 756. 워처는 블록이 닫힐
  때까지 (1,500+ gens) 살아서 블록 내용을 main path 와 중복 시뮬레이션한다.
- main root 3,024 의 fork 구조 (체인 diff 로 확정): 파일 최상위 `WS Import…`
  ptr2/3 (×2, gen 1 부터 생존!) × 트레일링-람다 CallChain 지속 여부 (×2 @2243)
  × 중첩 CallChain 반복 구조 (×3 @2927) × 현재 위치 로컬 스택 (×~40 —
  인자괄호/문자열/`${...}` 인터폴레이션/CallExpr-except-Digits/수식 레벨들,
  대부분 nullable WS 경계의 `ptr N / N+1` 쌍). 즉 **오래 사는 소수의 fork 가
  전부 체인 identity 에 곱셈으로 박힌다** (gen/observing 축 아님 —
  full=noGen=noObs=3024 확인).
- 느린 구간이 인터폴레이션 문자열 내부에 몰리는 이유: `"..${a}..${b}"` 가
  체인 깊이를 43-48 로 밀고 로컬 fork ×40 을 만들며, 그 비용이
  (main + 워처 6개) 전부에 복제되기 때문.

**mg2 가 22× 적게 유지하는 이유 (구조 차이)**: mg2 는 pended 워처 시동을
step 전체에서 KernelTemplate 단위로 dedup 해 depth-1 fresh path 로 시작하고
(MilestoneGroupParserKt.kt:409-448), tracking 필터가 조건이 참조하지 않게 된
first-milestone 의 path 를 통째로 버린다 (:504-507). 반면 mg3 는 root→tip
전체 체인을 shape identity 로 열거한다. 대신 mg2 는 kernelsHistory 가 8.3s
(mg3 는 이제 0.5s) — **jar.bbx 총합은 이미 동급** (mg3 15.5s vs mg2 16.4s),
그 외 파일은 mg3 가 우세. 남은 wall 격차 (35.5s vs 19.1s) 는 parse CPU 총량
(serial 78s vs ~26s) 이 병렬 경합에서 스트래글러를 부풀리는 것 + walk/encode
(~3s @jar.bbx) 몫.

**다음 arc 후보**: (a) 오래 사는 fork 의 공유/병합 — 특히 nullable WS 경계
ptr 쌍과 파일-레벨 fork 는 보고 좌표 외에 차이가 없어 병합 여지, (b) 워처가
main 과 같은 내용을 재파싱하는 중복 제거 (mg2 식 시동 dedup), (c) walk/encode
후속. mg2 도 같은 병리를 공유하므로 (a)/(b) 가 풀리면 mg2 를 크게 이긴다.

### 0.2 Phase A 결과 — ptr 쌍의 근본 원인 확정 (2026-07-03)

계측 도구: `Mgroup2VsMgroup3PathsTest` (`bibix4 runMgroup3PathsDiffTest`) —
같은 NGrammar 로 m2/m3 파서를 만들어 step 구동, live 체인과 생성기 템플릿
인벤토리를 나란히 덤프. 4줄 문법 (`S = A WS B; A = 'a'+; B = '(' WS A WS ')';
WS = ' '*`, 입력 "aa(aaa)") 에서 재현: m3 6 shapes (S:1/S:2 × B:1/B:2 쌍)
vs m2 2 paths (:2 만). 실공백 대조 입력에서는 그 경계의 쌍이 사라짐 —
**zero-width nullable 경계 한정** 확정.

**핵심 실측**: milestone group 인벤토리는 m2/m3 완전 동일 (group2 =
{S:1,S:2,A-rep:1}, group5 = {B:1,B:2} 양쪽 같음 — 쌍이 "그룹 멤버"로 함께
있는 것 자체는 m2 도 동일하고 비용도 없음). 갈라지는 곳은 **term action 의
replace 귀속** 하나:
- m2: `S:1→group3(WS 계속)`, `S:2→group5(B)` — 각 서브트리가 정확히 한 dot 에.
- m3: 위에 더해 **`S:1→group5` 잉여** — 런타임에 같은 tip group 에서 두 템플릿이
  모두 발화해 체인이 dot 변형별로 복제된다 (레벨마다 ×2).

**원인 코드**: `Mgroup3ParserGenerator.genMgroupTermActions` (:368-376) 가
tip group 의 각 milestone P 에 대해 `GenParsingGraph.reachablesFrom(P, ...)`
(:116-135) 로 appending 을 귀속시키는데, reachablesFrom 이
`progressedNodes[next]` / `derivePhaseProgressedNodes[next]` (:126-128) 를
따라간다 — P:1 에서 자신의 zero-width progress 쌍둥이 P:2 로 건너가 P:2 의
서브트리까지 P:1 에 귀속. m2 는 그룹 멤버별 개별 시뮬레이션
(MilestoneGroupParserGen.scala:231-241) + naive 그래프 reachability 를 쓰는데,
naive 그래프에서 progress 는 쌍둥이를 **부모의 형제로** 붙일 뿐 pre→post edge
가 없어 귀속이 유일하다.

**Phase B 수정 후보 (최소)**: reachablesFrom 에서 **시작 노드의 progress
링크만 따라가지 않기** (start 의 progressedNodes/derivePhaseProgressedNodes
제외; 더 깊은 노드의 progress-follow 는 유지해야 함 — appended group 의 쌍둥이
멤버십 {B:1,B:2} 는 m2 도 동일하며 그 경로로 수집됨). edge 쪽 호출 (:595) 은
parent 가 barrier 라 자기 progress 링크가 없어 무영향. 검증 주의점: (i) 쌍둥이
조건이 달라 다른 그룹에 갈라진 경우의 커버리지 (m2 도 조건별 그룹 분리 —
동형), (ii) 다단 zero-width 쌍둥이 (P:1→P:2→P:3), (iii) 초기 그룹. 성공 지표:
micro 6→2 shapes, 이후 jar.bbx profile_steps (peak 19,450 → mg2 893 급),
127/0 + m2 parity + parser_diff golden.

## 1. 목표와 배경

**최종 목표**: mulang 프로젝트의 bibix4 가 빌드스크립트(.bbx4/.bbx, mulang 문법) 파싱을
mgroup3 Rust 파서(FFI)로 하는데, 현재 `kernels_history` 재구성이 파싱 비용의 50~70% 를
차지해 mg2(Kotlin) 대비 wall-clock 이득이 없다. 이 재구성을 최적화해 native 경로가
이기게 만드는 것이 1차 목표의 마지막 관문이다.

**실측 (병렬 20파일, jparser 프로젝트 빌드스크립트 기준)**:
- mg2 (Kotlin): parse phase 19.1s
- native (Rust): 40–44s ← 역전 실패
- phase 분해 (mgroup3-native `time_parse` bin, 커밋됨):
  - junit.bbx (8KB): parse 2.0s + **kernels_history 4.4s**
  - ktjvm.bbx (16KB): parse 3.2s + **hist 6.9s**
  - cc.bbx: 5.3s + 4.6s / maven.bbx: 3.2s + 3.4s (gens 24k, kernels 456만)
- parser_diff (cargo test) wall: 이 회귀 전 ~7s → 현재 ~84–117s.
- hist 가 수백 ms 급으로 떨어지면 병렬 wall ~5s 예상 → mg2 대비 ~4×.

**통합 상태 (이미 완료, 이 작업의 전제)**: jparser `a9e35ccf` + mulang `daa3092`.
mulang 의 `bibix4 parser.generate` 가 crate/dylib/바인딩 전부 생성, MulangParser 는
native 우선 + mg2 fallback, 실코퍼스 차분 게이트(`NativeParserDiffTest`) 그린.
mulang 의 `bibix.deps` 는 개발 중 `jp = local("../jparser")`.

## 2. 현재 구조 (무엇이 왜 느린가)

### record 채널 (파스 핫패스가 남기는 것)
`HistoryEntry` (Kotlin `ParsingCtx.kt` / Rust `parsing_ctx.rs`):
- `actionApplications: List<ActionApplication>` — 템플릿 참조(ParsingActionsPlain) +
  런타임 바인딩(rtCurr/rtMid/next/rtGrand) + 보고 바인딩(repCurr/repMid/repGrand) +
  구동 조건. kernel 은 여기서 lazy 하게 물질화된다.
- `finishedKernels` / `addedKernels` — 템플릿 밖 record (root progress 등), 조건 포함.
- `condPathFinishes: Map<PathRoot, AcceptCondition>` — 이 step(=gen)에 **끝나는** finish
  (eager 채널, end == gen).
- `lateCondPathFinishes` — 죽는 cond path 가 등록한 possible-finish (end == gen-1, late 채널).
- `activeCondPaths`, `mainRootFinish`, `reportedCondRoots`.

### kernelsHistory 의 판정 루프
`Mgroup3Parser.kt:968` (Rust 미러: `mgroup3-native/src/parser/core.rs` ~line 820,
`kernels_history`): 매 gen 의 매 application 에 대해
1. `app.condition` 게이트 평가 (:980)
2. `pa.finished[*]` 의 조건을 rt 바인딩으로 물질화 → 평가 → 통과 시 rep 바인딩 좌표로 방출
3. `pa.added[*]` 동일
4. `entry.finishedKernels` / `addedKernels` 동일

### 병목: evaluateRecordCondition (Kotlin :911 / Rust core.rs `evaluate_record_condition` ~:777)
```kotlin
var c = cond
for (g in recordGen until history.size) {       // record 시점부터 입력 끝까지 전 step
  c = evolveAcceptCondition(c, entry.condPathFinishes, entry.lateCondPathFinishes,
                            entry.activeCondPaths, g)
}
// 잔여 조건은 evaluateAtEndOfInput 으로 확정 (+ endOfInputLateFins 가상 late step)
```
**record 하나의 조건마다 파스 잔여 전체를 재생** → 총비용 O(records × gens) 준-이차.
maven.bbx: 456만 kernel × 최대 2.4만 step. 런타임 파싱은 같은 evolve 를 live 조건당
step 에 1회(증분)만 하므로 이 비용이 없다 — 보고 레이어만 지수적으로 중복 계산.

### 왜 재생으로 바뀌었나 (되돌리면 안 되는 의미론)
커밋 e46ed96a / f20d1d56 의 exact-span 의미론 때문. 이전의 "최종 상태 조회" 방식
(root 가 언젠가 finish 했나 — inverted index O(1))은 아래에서 오답:
- `Unless/OnlyIf(sym, b, e)`: **정확히 span (b,e)** 의 매치 여부만. finish 는
  gen e 의 eager 채널 또는 gen e+1 의 late 채널에서만 관찰돼야 함. 다른 span 흡수 금지.
- `NoLongerMatch(sym, s, minEnd)`: end ≥ minEnd 인 finish 만 위반. 흡수한 finish 의
  **그 자신의 조건**을 재귀 평가해야 함 (finish 조건이 또 조건을 참조).
- `evaluateAtEndOfInput` (:939): 재생 후 잔여 leaf 는 "끝까지 finish 없음 + root 미완"
  → NoLongerMatch/NotExists/Unless true, 반대쪽 false.
- `endOfInputLateFins` (:884): 입력 끝에서 살아있는 root 들의 마지막-gen zero-width
  possible-finish 를 가상 late step 으로 한 번 더 evolve.

이 의미론은 **그대로 유지**해야 한다 (m2 golden byte-exact 의 근거).

## 3. 최적화 설계 — 같은 답을 재생 없이

핵심 관찰: 재생 루프가 leaf 별로 실제 소비하는 정보는 극히 국소적이다.

### 3.1 leaf 별 직접 평가 (재귀 함수 `evalCond(cond, recordGen) -> Bool`)
- `Always/Never/And/Or`: 자명 (재귀 + bool 결합. evolve 의 중간 단순화와 결과 동일).
- `Unless(sym, b, e)`: 정확히 두 곳만 조회 —
  - eager: `history[e].condPathFinishes[(sym,b)]` — 있으면 `!evalCond(fin, e)`
  - 없으면 late: `history[e+1].lateCondPathFinishes[(sym,b)]` — 있으면 `!evalCond(fin, e+1)`
  - (e+1 == history.size 인 경우 late 는 `endOfInputLateFins[(sym,b)]`)
  - 둘 다 없으면 true. `OnlyIf` 는 부정형.
  - evolve 의 "root active 면 한 step 대기" 로직은 e+1 조회로 자연 대체된다.
- `NoLongerMatch(sym, s, minEnd)`: 위반 = 아래 중 하나라도 조건이 참으로 평가되는 finish:
  - eager: gen g 의 `condPathFinishes[(sym,s)]`, g ≥ minEnd
  - late: gen g 의 `lateCondPathFinishes[(sym,s)]`, g-1 ≥ minEnd
  - end-of-input 가상 late: `endOfInputLateFins[(sym,s)]` (end == 마지막 gen ≥ minEnd 일 때)
  - 위반 없으면 true. `NeedLongerMatch` 는 부정형.
- `NotExists/Exists(sym, s)`: end 무관 — 해당 root 의 어떤 finish 든 조건이 참이면.

### 3.2 ⚠️ recordGen 클램프 (재생과 동치가 되기 위한 핵심 미묘함)
재생은 `g ∈ [recordGen, end]` 의 fin 만 본다. 따라서 직접 평가의 스캔 범위는:
- NoLongerMatch: eager 는 `g ≥ max(recordGen, minEnd)`, late 는 `g ≥ recordGen && g-1 ≥ minEnd`.
- NotExists/Exists: `g ≥ recordGen`.
- Unless/OnlyIf: `e ≥ recordGen` 이어야 eager 조회 유효 (`e < recordGen` 이면 재생 첫
  스텝에서 이미 `gen > endGen+1 → Always/Never` 경로 — 동일하게 처리. 정확한 경계는
  evolve 의 `gen == endGen` / `endGen+1` 분기와 맞출 것 — evolve 코드를 진실로 삼아라).
이 클램프를 빼먹으면 golden 이 달라진다. **evolve (`AcceptCondition.kt:513-646`) 의
분기 하나하나와 대조하며 작성할 것.**

### 3.3 inverted index
NoLongerMatch/NotExists/Exists 는 root 의 fin 존재 gen 을 스캔해야 하므로, kernelsHistory
시작 시 한 번:
```
eagerFinGens:  Map<PathRoot, SortedList<gen>>   // entry.condPathFinishes 의 키를 gen 별로
lateFinGens:   Map<PathRoot, SortedList<gen>>
```
을 구축 (O(총 fin 수)). range 조회는 이진탐색. Unless/OnlyIf 는 인덱스 불필요 (직접 두 entry 조회).

### 3.4 메모이제이션
`evalCond` 를 `(condition, effectiveRecordGen)` 키로 메모. 대부분의 leaf 는 절대 gen 을
담고 있어 recordGen 무관 → 정규화: leaf 평가 전에 recordGen 을 위 클램프로 반영한
"유효 시작 gen" 으로 줄여서 키 폭발 방지 (예: Unless 는 recordGen ≤ e 여부만 유효).
- Kotlin: AcceptCondition 은 data class (equals/hashCode ✓) — HashMap 메모.
- Rust: `AcceptCondition` 은 `derive(Hash, Eq)` ✓ (`accept_condition/mod.rs:24`) — FxHashMap.
- fin 조건의 재귀 평가도 같은 메모를 공유 — 수백만 record 가 소수의 조건을 공유하므로
  계산량이 붕괴한다. 메모는 kernelsHistory 호출당 1개 (ctx 별).

### 3.5 isAccepted 도 동일 경로
`isAccepted` (:870) 는 마지막 entry 의 mainRootFinish 를 같은 함수로 평가 — 새 구현으로
자동 수혜 (mulang ccgen.mu 의 Kotlin 50s 중 일부도 여기).

## 4. 구현 순서 (권장)

1. **Kotlin 먼저** (`Mgroup3Parser.kt`): `evaluateRecordCondition` 을 새 구현으로 교체
   (기존 재생 버전은 디버그 대조용으로 임시 보존 가능 — env/flag 로 차분 모드).
2. **동작 불변 검증** — 이 최적화는 golden 을 바꾸지 않아야 한다:
   - `bibix4 runMgroup3Test` (127/0), `runMgroup3CornerCaseTest`, `runMgroup3AdvancedTest`
   - `bibix4 runMgroup3HistoryDiffTest` (m2 parity 2/2)
   - fixture 재생성 **불필요** — 기존 golden 과 그대로 일치해야 정상.
3. **Rust 미러** (`mgroup3-native/src/parser/core.rs` 의 `evaluate_record_condition` 계열):
   - `cd mgroup3-native && cargo test` — parser_diff 가 기존 golden 과 일치 + wall 이
     ~84s → 한 자릿수로 떨어지는지가 성능 지표.
4. **mulang 재검증**: `cd ../mulang && bibix4 parser.generate` (dylib 재빌드) →
   `bibix4 parser.test:test` (NativeParserDiffTest — testMlc 실패는 기존 이슈, 무시) →
   uberjar 재빌드 후 프로파일:
   ```
   bibix4 bibix4.cliUberJar   # jar 경로는 출력의 dest= 에서
   cd ../jparser && java --enable-native-access=ALL-UNNAMED -jar <jar> --profile-startup mgroup3.parser
   ```
   "Phase 1+2 parse" 가 19.1s(mg2) 대비 유의미하게 작아야 성공.
5. 필요시 walk/encode 쪽 후속 프로파일 (hist 제거 후 다음 병목 확인 — time_parse 로 분해).

## 5. 측정 도구
- `mgroup3-native/src/bin/time_parse.rs`: `time_parse <parserdata.pb> <input> [iters]` —
  load/parse/is_accepted/kernels_history 분해. parserdata 는
  `java -jar <genCliJar> ../mulang/grammar/mulang.cdg -parserdata /tmp/pd.pb -proto ... -rust ... -kotlin ... -mgroup3-native $PWD/mgroup3-native` 로 생성 (genCliJar 는 `bibix4 mgroup3.genCliJar`, **경로 추출 후 `[ -s ... ]` 가드 필수** — 실패 시 빈 경로로 구버전 jar 를 조용히 실행하는 함정).
- `/tmp/natdbg/TestConc2.java` (세션 산출물, 없으면 재작성): mulang uberjar classpath 로
  NativeMulangParser 직접 serial/parallel 측정.

## 6. 관련 파일
- Kotlin: `mgroup3/parser/kotlin/com/giyeok/jparser/mgroup3/Mgroup3Parser.kt`
  (evaluateRecordCondition :911, kernelsHistory :968, endOfInputLateFins :884,
  evaluateAtEndOfInput :939), `AcceptCondition.kt` (evolve :513 — 의미론의 진실),
  `ParsingCtx.kt` (record 구조).
- Rust: `mgroup3-native/src/parser/core.rs` (kernels_history / evaluate_record_condition /
  end_of_input_late_fins), `mgroup3-native/src/accept_condition/eval.rs` (evolve),
  `parsing_ctx.rs`.
- 주의: Mgroup3Parser(Rust) 는 Send+Sync 를 유지해야 한다 (bibix4 병렬 파싱이 FFI 핸들
  공유 — 컴파일 타임 assertion 이 core.rs 에 있음). 메모/인덱스는 kernels_history 호출
  로컬로 만들 것 (파서 구조체에 공유 mutable 캐시 금지 — 이전에 락 경합으로 40s 실측).
