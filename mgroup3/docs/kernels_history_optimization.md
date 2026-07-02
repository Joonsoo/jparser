# kernels_history 최적화 — replay 재생을 leaf-직접 조회 + 메모로 대체

작성: 2026-07-02. 이 문서는 다음 세션이 이 작업을 이어받기 위한 설계/실행 문서다.

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
