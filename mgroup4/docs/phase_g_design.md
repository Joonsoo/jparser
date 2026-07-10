# mgroup4 Phase G1 — generator-native 설계 문서 (두 아키텍처 비교와 권고)

작성: 2026-07-10. 상태: **설계 (구현 전).** 상위 세션 리뷰 후 구현 승인 대상.
전제 문서 (필독 순):
- `phase_g_plan.md` §6 (G0 v2 정정 — 설계의 실측 기반), §1 (시간 예측), §5.v1 (기각된 v1 정체성 실수).
- `phase_b_plan.md` §5 (런타임 packing 이 Rust 에서 기각된 원인 — lazy 캐시가 뒤집어야 할 비용 구조).
- `../../mgroup3/docs/mgroup4_phase_a_design.md` §0 (코드 지도), §3 (기록-시-확장 보고 불변 전략).
- 코드: `parser/kotlin/.../G0SuffixSetStats.kt` (상태 정체성의 실행 가능한 정의),
  `Mgroup4Parser.kt` (런타임 packing 구현 — lazy 캐시의 기반),
  `../../mgroup3/gen/kotlin/.../Mgroup3ParserGenerator.kt` (현행 생성기, 읽기 전용).

이 문서는 **설계만** 담는다. mgroup3(-native) 는 영원히 읽기 전용. 상태 정체성에
gen 을 넣지 않는다 (v1 의 실수 — gen 은 런타임 바인딩). 모든 설계는 기존
`runMgroup4DiffTest` 차등 오라클 (kernels-history byte-parity) 를 통과할 수 있어야 한다.

---

## 0. 실측 요약 — 설계가 딛는 지반 (phase_g_plan §6 재정리)

Phase G 는 "런타임 path 비교·folding·dedup 기계를 없애고 parserdata 가 결정화를
담는다" 는 원안(§0)으로 시작했으나, G0 v1 이 킬 게이트 발화로 종료 → v2 정정으로
재개됐다. **v2 가 이 문서의 모든 수치 기반이다:**

1. **상태 정체성 = kernel 튜플 집합.** 상태 = (공유 prefix 인스턴스) 아래
   window(길이 ≤ n)의 milestone 노드들의 `(symbolId, pointer[, observing])` 열 +
   `tipGroupId`. **gen 은 완전 제외** (G0SuffixSetStats.kt:42-52 변형 A). gen
   동등/순서 패턴(변형 B)조차 상태를 거의 안 가른다 (B−A ≤ 2 상태 실측, §6.2).
   조건 클래스를 넣으면 +20~40%.

2. **상태 수 (동적 하한, 현행 milestone group 대비 배수)**: jquery n=2 = 3,047
   상태 = 1.67×(noCond)/2.10×(withCond), n=3 4.84×/5.92×, n=4 9.59×/11.36×.
   킬 게이트 1 (n=2 에서 ~10×) 통과. n=2~3 이 스위트 스폿, n=4 는 es5 에서 경계.

3. **상태당 재사용 ~96 gen** (jquery n=2: 293k gen / 3,047 상태). **로그형 포화**
   (입력 15.5× 에 상태 3.5× — 강한 sublinear, v1 의 발산 꼬리 소멸). 이 재사용율이
   lazy 물질화의 경제성 근거 — 상태 구성 비용이 96회로 상각된다.

4. **시간 예측 (es5 heavies, Rust)**: speedup = 1/(1−L·(1−1/R)). 실효 L 은 runtime
   packing 실측 하한 0.29 ~ 회귀 상한 0.48~0.56 사이. es5 n=2~4 에서 **1.1~1.3×**,
   ∞ 1.3~1.5×. mulang 은 R 이 condition 제약으로 1.2~1.4 에 묶여 1.1~1.2×.

5. **동적 하한 vs 정적 열거**: G0 이 잰 것은 실코퍼스에서 실제로 밟은 상태 (동적
   하한). **전체 정적 생성기는 미도달 전이까지 열거해 이보다 크다**; lazy 는 도달분만
   만들어 이 하한이 곧 실비용이다. 이 비대칭이 아키텍처 선택의 핵심 인자다 (§1).

6. **핵심 팩터링 확인 (코드 검증)**: 현행 parserdata 의 모든 런타임 조회는 **템플릿
   identity 로만** 키잉된다 — term action 은 `tipGroupId`, tip edge 는
   `(KernelTemplatePair, tipGroupId)`, mid edge 는 `(KernelTemplatePair,
   KernelTemplatePair)` (Mgroup4Parser.kt:109-121, 372, 500). **gen 은 어느 키에도
   안 들어간다.** 이것이 (b) lazy 안이 기존 pb 를 신규 포맷 없이 재사용할 수 있는
   구조적 근거 (§0.2 상세).

---

## 0.5 두 아키텍처의 공통 상태 모델

두 안 모두 같은 상태 정체성 (v2) 을 쓴다. 차이는 **언제·어디서 상태를
물질화·저장하는가** 뿐이다.

- **상태 (State)**: `n`-window 안 milestone 슬롯의 정렬된 `(symbolId, pointer)`
  튜플 집합 + `tipGroupId` + (조건 템플릿 클래스). gen·multiplicity 제외.
  ≡ G0SuffixSetStats 변형 A(+withCond). n=1 이면 State ≡ 현행 `tipGroupId` (milestone
  group) — 완전 하위호환.

- **런타임 행 (Row)**: `(공유 prefix 포인터, State id, 멤버별 gen/보고 바인딩)`.
  gen 은 여기 산다 (조건 anchoring·보고 좌표). 오늘날 "같은 milestone group 아래
  여러 live path" 가 여러 Row 인 것과 동형.

- **전이 (Transition)**: `(State, 입력 클래스)` → 다음 상태(들) + 방출할 액션 참조.
  입력 클래스 = 현행 term group (문자 → term action 결정). 전이 종류 5가지:
  - **(T1) term/descend**: window 안 각 멤버가 `replaceAndAppends` 를 타 tip 에 새
    노드 부착 → State 의 window 가 한 칸 슬라이드, 새 tipGroupId. suffix-set 전이
    하나가 여러 멤버의 descend 를 흡수 (튜플이 늘어남).
  - **(T2) window 내 reduce**: `replaceAndProgresses` 가 tip group 을 자기 완성,
    reduce 가 window 안에서 멈춤 (tip/mid edge append). State 안에서 흡수 (사전열거).
  - **(T3) window 경계 reduce**: reduce 가 window 를 넘어 out-of-window prefix
    노드로 pop. `(prefix 노드 템플릿 × State)` 키의 경계 edge 전이 — prefix 는 Row
    가 든다.
  - **(T4) window-exit 분열**: descend 로 window 안 group 노드가 depth > n 으로
    밀려 out-of-window 가 되면 멤버별 singleton 으로 분열 (State 갈라짐).
  - **(T5) reduce 시 멤버 분열**: reduce 가 window 안 group 노드에 도달, 멤버별
    (mid)edge action 이 갈리면 그 시점 분열. 같으면 group 유지.

---

## 1. 아키텍처 (a) — 전체 정적 생성기 (원안)

### 1.1 개요

suffix-set 상태를 **생성 시점에 closure 로 전부 열거**하고, mgroup4 전용 신규
parserdata (mgroup3 schema 무변경) 를 방출. 런타임은 순수 테이블 조회 — merge/fold/
verdict 기계 전무. 현행 `Mgroup3ParserGenerator` 의 kernel-set closure worklist
(milestoneGroups BiMap + termActions/tipEdgeActions/midEdgeActions 워크리스트,
Mgroup3ParserGenerator.kt:53-105) 를 **suffix-set 로 일반화**한다.

### 1.2 상태 표현과 id 부여

- **State id 부여**: 현행 `milestoneGroupIdOfKernelTemplates`
  (Mgroup3ParserGenerator.kt:182-190) 의 BiMap 패턴을 그대로 승격.
  `HashBiMap<Int, SuffixSetState>`. `SuffixSetState` = `List<WindowSlot>` +
  `tipGroupId` + `condClass`, 정렬 정규형. n=1 이면 `WindowSlot` 리스트 비어
  `tipGroupId` 만 = 현행 group id 와 1:1 (하위호환 검증 지점).
- **WindowSlot** = 그 window 위치에 올 수 있는 kernel 템플릿들의 집합 (= 이미
  milestone group). window 슬롯이 group 인 이유: 한 위치에 여러 멤버가 상관돼
  살 수 있다 (suffix-set 의 "열 상관"). 슬롯 간 상관 (어떤 슬롯 조합이 함께
  등장하는가) 은 튜플 집합의 **집합** 성으로 표현 — 슬롯별 독립 group 이 아니라
  전체 튜플의 집합이 상태.

### 1.3 전이 테이블 구조 (종류별)

| 전이 | 키 | 값 | 비고 |
|---|---|---|---|
| T1 term/descend | `(State, termGroup)` | `List<(replaceKernel 템플릿, 새 State id, condTemplate, observing, condRootStarters)>` | fork 는 여러 항목 |
| T2 window 내 reduce | `(State, replaceMgroupId)` | window 안 append 결과 State id + condTemplate | 사전열거 |
| T3 window 경계 reduce | `(prefixNodeTemplate, State, replaceMgroupId)` | 경계 edge action (현행 tip/mid edge 의 State화) | prefix 는 Row |
| T4 window-exit 분열 | `(State, termGroup)` 의 T1 변형 | 멤버별 분열 후 각 State 로 T1 | 정적 예측 가능 |
| T5 reduce 멤버 분열 | T3 안에 흡수 | 멤버별 edge 조회가 갈리면 State 갈래 열거 | |

- **조건 템플릿**은 각 전이 항목에 붙되 gen 은 태그(CURR/MID/NEXT/GRAND)로 —
  현행 `AcceptConditionTemplate` 재사용. 상태 정체성에 들어가는 것은 조건의 **구조
  클래스** (G0 의 condClass), 런타임 값이 아니다.
- **보고 메타데이터**는 현행 `ParsingActions` (finished/progressed/added 템플릿)
  구조를 전이 항목에 그대로 부착. 상태 내 표현 = 멤버별 보고 좌표 태그 배열.

### 1.4 생성 알고리즘 (현행 closure 기계의 일반화)

현행 worklist (Mgroup3ParserGenerator.kt:53-105) 의 각 단계를 suffix-set 로 승격:

1. **초기 State**: start symbol 의 root path → `SuffixSetState(window=[], tip=rootGroup)`.
2. **워크리스트 루프**: 미처리 State 마다:
   - 그 State 의 **모든 멤버 튜플**에 대해 term group 별 `derivedFrom` +
     `progressedFrom` (현행 `genMgroupTermActions` :335-442 재사용) 로 term action
     생성. 단 결과를 milestone group 하나가 아니라 **suffix-set 전이**로 조립 —
     여러 멤버의 descend 결과 튜플을 모아 새 State 를 `stateIdOf` 로 등록.
   - window-내 reduce 는 T2 로 흡수, window-경계 reduce 는 T3 (현행
     genTipEdgeAction/genMidEdgeAction 재사용, prefix 템플릿 축을 키에 추가).
   - window-exit 는 T4 로 사전열거 (n-depth 넘는 descend 판정 정적).
3. **닫힘 조건**: 새 State·전이가 안 생기면 종료. 현행 `possibleTipEdges`/
   `possibleMidEdges` 재사용 (경계 edge 후보 열거).

**재사용 가능 범위** (코드 조사 결과): `GenParsingTask.derivedFrom`/`progressedFrom`
(그래프 closure) 와 `genMgroupTermActions`/`genTipEdgeAction`/`genMidEdgeAction`
(단일 group 액션 생성) 은 **거의 그대로 재사용** — 이들은 이미 "한 group 의 term/
edge 동작" 을 낸다. 일반화가 새로 하는 일은 **그 결과를 suffix-set 로 조립·닫는
바깥 루프** (worklist + stateIdOf + 전이 테이블 조립) 뿐. 보고 레이어
(`fillParsingActions`/`emitAddedKernels`/barrier·remap) 는 **무변경 재사용** — 상태
정체성이 아니라 전이 항목의 부착 메타데이터이므로.

### 1.5 정적 closure 크기 리스크 (원안의 최대 위험)

- **G0 은 동적 하한.** 정적 열거는 **미도달 전이·미도달 State** 를 포함한다.
  얼마나 클지 추정 방법:
  1. **상한 프로브 (구현 전 추정)**: 생성기를 만들기 전, `Mgroup3ParserGenerator`
     의 closure 워크리스트를 suffix-set 로 돌리되 **전이 테이블은 안 방출하고
     State·전이 카운트만** 세는 dry-run 프로브 (G0StaticProbe 를 파서-런타임이
     아니라 **생성기-closure** 로 재작성). 이게 진짜 정적 상한.
  2. **하한 대비 배수 관측**: 동적(G0) 대 정적(프로브) 비를 소형 문법(asdl,
     구조 12종)에서 먼저 재 — es5 로 외삽. asdl 은 G0 에서 이미 포화(0.84×)라
     정적/동적 갭 작을 것; es5 는 late-convergence 90.5% 라 갭이 클 위험.
  3. **킬 기준**: 정적 State 수가 동적 하한의 ~5× 초과하거나 전이 테이블이
     현행 pb 의 ~10× 초과하면 (a) 중단 → (b) 로 전환.
- **위험 신호**: late-convergence 90.5% (phase_b §1) 는 "어떤 형제 쌍이 언제
  수렴하는지 입력 의존" — 정적 열거는 **모든 가능한 수렴 조합** 을 State 로 열어야
  해 조합 폭발 위험. lazy 는 실제 밟은 조합만 만든다. **이것이 (b) 우위의 핵심.**

---

## 2. 아키텍처 (b) — lazy 물질화 (regex lazy-DFA 방식) ★권고안

### 2.1 개요

전체 정적 생성기 **없이**, 런타임이 상태를 **처음 만날 때 구성·캐시**하고,
`(State × 입력 클래스)` 전이도 **처음 계산 시 캐시**한다. 워밍업 후에는 순수 테이블
조회 — generator-native 와 같은 런타임이 되지만 **미도달 상태는 안 만든다** (동적
하한 = 실비용). 핵심 주장: **신규 parserdata 포맷·생성기가 불필요할 수 있다** —
기존 pb 를 그대로 로드하고 suffix-set 상태를 그 위에서 합성.

### 2.2 기존 pb 재사용의 성립 조건 (정밀 조사)

lazy 안이 성립하려면 "State 를 만날 때, 기존 pb 의 per-group 액션들을 합성해
suffix-set 전이를 만들 수 있어야" 한다. 코드 조사 결과 **세 축 모두 성립한다**:

1. **term/descend (T1) 합성**: State 의 각 멤버 튜플은 window 슬롯별 kernel
   템플릿. 각 window 위치의 tip 쪽 group 에 대해 `plain.termActions[tipGroupId]`
   (기존 pb) 를 조회 → `replaceAndAppends` 적용. **성립**: term action 은
   `tipGroupId` 로만 키잉 (gen 무관, Mgroup4Parser.kt:239). suffix-set 전이 =
   현행 `applyTermAction` 이 이미 하는 일을 "State 의 대표 Row 에서 한 번 계산해
   캐시" 하는 것.

2. **window 내 reduce 합성 (T2/T5)**: reduce 가 window 안 group 노드에 도달하면
   멤버별 `tipEdgeActionsMap`/`midEdgeActionsMap` (기존 pb, 템플릿 키) 조회 → 멤버별
   결과가 같으면 State 유지, 다르면 State 갈래. **성립**: 현행 `memberSingletonsForEdge`
   + `applyEdgeAction` (Mgroup4Parser.kt:369-396, 497-524) 이 이미 이 멤버별 조회를
   런타임에 한다. lazy 는 이 결과를 `(State, replaceMgroupId)` 키로 **캐시**.

3. **조건 템플릿 합성**: 각 append 의 `acceptCondition` 은 gen 태그 템플릿
   (`AppendMilestoneGroupPlain.acceptCondition`). State 캐노니컬라이즈는 이 템플릿의
   **구조 클래스** (G0 condClass) 로 하고, 런타임 값 바인딩 (`toAcceptCondition`)
   은 Row 가 gen 을 넣어 resolve. **성립**: 조건 값이 gen 이라 상태에 안 들어가고,
   condClass 가 다르면 State 가 갈리므로 (H5 "동일 condition 병합" 과 동형) —
   State 안에서 조건은 균일.

4. **rea 의 멤버 귀속 (가장 미묘한 축)**: State 가 여러 멤버 튜플을 가질 때, 한
   term action 이 멤버마다 다른 `replaceAndAppends` 를 낼 수 있다. lazy 전이 캐시는
   "State 의 각 멤버 → 각 멤버의 rea 결과" 를 **멤버 귀속을 보존한 채** 합성해야
   보고 확장(§2.6) 이 정확하다. **성립 조건**: 멤버는 정의상 서로 다른 kernel 이라
   `plain.termActions` 조회가 멤버별 (같은 tipGroupId 를 공유하는 window 슬롯이면
   같은 term action, 다르면 다름). 이 귀속은 현행 `foldGroup`/`memberSingletonsForEdge`
   의 병렬 배열 (groupMembers / groupMemberReportGens) 이 이미 유지 — lazy State 캐시
   엔트리도 같은 병렬 배열 구조를 든다.

**결론: 기존 pb 로 충분하다. 신규 포맷·생성기 불필요.** 이것이 (b) 의 구현 부담을
극적으로 줄인다 — G2(생성기)·신규 proto 스키마 전체가 사라진다.

### 2.3 상태 캐노니컬라이즈 → id 매핑

- G0SuffixSetStats 의 정의를 **재사용**하되 (프로브가 아니라) 캐노니컬 문자열 →
  `Int` id 의 `HashMap<CanonKey, Int>` 로. `CanonKey` = 정렬된 튜플 집합 + tipGroupId
  + condClass (변형 A + withCond). gen 제외.
- **주의**: G0SuffixSetStats 는 `Long` 지문(fingerprint)으로 distinct 를 셌지만,
  lazy 캐시는 **실제 State 객체를 저장**해야 하므로 지문이 아니라 구조적 CanonKey
  (충돌 없는 정확 키). 지문 충돌은 프로브에선 무해했으나 캐시에선 오답 — 정확 키 필수.
- id 부여 = 첫 관찰 시 `stateIdCounter++`. Row 는 `(prefix 포인터, State id, gen
  바인딩 배열)`.

### 2.4 전이 캐시 키/구조

- **term 전이 캐시**: `HashMap<Long, TransitionEntry>` key = `(stateId << 32) |
  termGroupIndex`. `TransitionEntry` = `List<(newStateId, replaceTemplate,
  condTemplate, observing, condRootStarters)>`. 현행 `termActionCache`
  (Mgroup4Parser.kt:235, `(tipGroupId, input)` 키) 의 State-레벨 승격판.
- **경계 reduce 전이 캐시**: `HashMap<(prefixTemplate, stateId, replaceMgroupId),
  BoundaryEntry>`. prefix 템플릿이 키에 들어가는 건 T3 가 prefix 노드에 의존하기
  때문 — 그러나 prefix 템플릿 종류가 유계(문법 크기)라 캐시 폭발 없음.
- **캐시 미스 = 첫 조우**: 위 §2.2 합성으로 엔트리를 만들어 넣고 반환. 미스 비용 =
  1회 합성 (현행 per-gen 재파티션의 "상태당 1회" 판 — phase_g_plan §6.3 의
  "per-gen 병합을 상태당 1회로" 가 정확히 이것).

### 2.5 캐시 수명/크기 관리

- **파스 간 공유 (권고)**: 캐시는 **파서 인스턴스 수준** (파스 간 공유). State
  공간이 로그 포화(§0 항목3)라 여러 파스에 걸쳐 워밍업이 상각된다 — 특히 워처
  프로덕션(bibix4/mulang)은 같은 문법으로 수천 파스라 공유 이득 큼.
- **크기**: jquery n=2 3,047 State × (전이 캐시 상태당 ~수개 term group) → 수만
  엔트리. n=4 도 ~3만 State (9.6×) — 수십 MB 규모, 관리 가능. **상한 가드**: State
  수가 예산 초과 시 LRU 축출 (regex lazy-DFA 의 캐시 클리어 정책과 동형) — 축출돼도
  재구성 가능하므로 정확성 불변.
- **스레드 안전** (Rust): 파스 간 공유 캐시는 `RwLock`/`DashMap` 또는 파스별
  로컬 캐시 + 종료 시 머지. 프로토타입은 파서 인스턴스 단일 스레드 가정.

### 2.6 보고 parity — 기록-시-확장 재사용

- **Phase A §3.2 의 "파스는 group, 보고는 멤버별 확장" 을 그대로 재사용.** State
  는 멤버별 보고 좌표 배열 (groupMemberReportGens 와 동형) 을 Row 에 든다. app 을
  HistoryEntry 에 쌓을 때 group 노드를 참조하는 app 이면 멤버별로 복제 (rep 좌표만
  다르게).
- **결정적 사실 (Phase A 실측)**: 기록-시-확장은 **실측 no-op** (fold 시점 보고
  좌표가 멤버 불변 — rejectReportCoordDiff=0, mgroup4_phase_a_design §7.2). lazy
  안도 같은 State 정체성·같은 병합 조건이라 이 no-op 이 유지된다 — **보고 확장이
  이득을 잠식하지 않는다**는 A 의 결론이 lazy 에 그대로 적용.
- **kernelsHistory 무변경**: 현행 lazy 해석 (Mgroup4Parser.kt:1510-1579) 은 app 의
  rt*/rep* 바인딩만 본다 — State 캐시를 몰라도 된다. RecordConditionEvaluator 도
  조건만 본다 (State 무관). **보고 레이어 전체가 lazy 안에서 무변경.**

### 2.7 Kotlin 프로토타입 → Rust 이식 경로

- **Kotlin 프로토타입**: 현행 `Mgroup4Parser` 를 확장 — `mergeInteriorGroups` 의
  per-gen 재파티션을 State 캐시 조회로 대체. 병합 verdict/fold 기계는 **첫 조우
  시에만** 돌고 결과를 캐시. 게이트 = `runMgroup4DiffTest` byte-parity (무변경).
- **Rust 이식**: **mgroup4-native crate 가 이미 존재** (parser/core.rs 등, full
  mirror). lazy 캐시는 `HashMap`/`FxHashMap` + `OnceCell` 로 이식 — Phase B 에서
  이식된 group 기계 (foldGroup/memberSingletons 대응) 를 캐시-백드로 재배선.
  Phase B 가 죽인 per-gen 병합 패스가 캐시 히트로 사라지는 게 핵심 (§4 회생 조건).

---

## 3. 비교표와 권고

### 3.1 비교표

| 기준 | (a) 전체 정적 생성기 | (b) lazy 물질화 ★ |
|---|---|---|
| **구현 공수** | 큼 — 신규 proto 스키마 + suffix-set 생성기(closure 일반화) + 신규 런타임 소비자 + Rust 이식. G2+G3+G5 전부. | 작음 — 기존 pb 재사용, 생성기·proto 무. 현행 packing 을 캐시-백드로 전환 + Rust 재배선. |
| **정확성 리스크 (보고 parity)** | 중~상 — 생성기의 barrier/remap 보고 parity 를 suffix-set 로 재현해야 (phase_g_plan §3 리스크2 "가장 까다로운 컴포넌트"). | 낮음 — 보고 레이어 전체 무변경 (§2.6). 기록-시-확장 no-op 실측 재사용. |
| **콜드 성능** | 최적 — 로드 후 즉시 순수 테이블 (워밍업 0). | 워밍업 있음 — 첫 파스 초반 캐시 미스. 로그 포화라 급속 상각(~96 gen/State). |
| **웜 성능** | 순수 테이블 조회. | 워밍업 후 순수 테이블 조회 (동등). 예측 이득 동일 (§0 항목4). |
| **캐시 미스 비용** | 없음 (정적). | 상태당 1회 합성 (현행 재파티션의 1/96 상각). Phase B 회귀 주범(per-gen 병합)이 사라지는 구조. |
| **Rust 이식성** | 신규 crate 소비자 필요. | 기존 mgroup4-native 재배선 — 이식 자산 최대 재사용. |
| **parserdata 호환** | 신규 포맷 (mgroup3 무변경이나 mgroup4 신규 pb 필요 — 재생성·배포). | **기존 pb 그대로** (§2.2) — 배포 스왑 불필요. |
| **n 유연성** | n 이 생성 파라미터 — n 바꾸면 pb 재생성. | n 이 순수 런타임 파라미터 (현행 MG4_INTERIOR_N 그대로) — 재생성 불필요. |
| **정적 폭발 리스크** | **높음** — late-convergence 90.5% 조합을 전부 열거 (미도달 포함). §1.5 킬 위험. | 없음 — 도달분만 (동적 하한 = 실비용). |

### 3.2 권고 — (b) lazy 물질화

**권고안: (b) lazy 물질화.** 근거:

1. **구현 부담 극감**: §2.2 조사로 **기존 pb 로 충분**함이 확정 — 신규 proto 스키마·
   suffix-set 생성기 (phase_g_plan §3 리스크2 의 "가장 까다로운 보고 parity 재현")
   전체가 사라진다. G2(생성기)·G3(신규 소비자) 가 "현행 packing 을 캐시-백드로 전환"
   한 작업으로 축소.
2. **정적 폭발 회피**: G0 이 잰 것은 동적 하한이고, late-convergence 90.5% 는 정적
   열거를 조합 폭발시킬 위험 (§1.5). lazy 는 이 위험이 구조적으로 없다.
3. **보고 parity 리스크 최소**: 보고 레이어 무변경 + 기록-시-확장 no-op 실측 재사용.
   parity 게이트 통과 경로가 가장 짧다.
4. **Phase B 회귀 직격**: Phase B 가 죽은 건 per-gen 병합 패스의 상수 비용
   (phase_b §5.2). lazy 는 그걸 "상태당 1회"로 바꾸는 정확히 그 수술 (phase_g_plan
   §6.3). Phase B 를 죽인 원인이 제거된 조건에서 §0 예측을 검증하게 된다.
5. **n·parserdata 유연성**: n 이 런타임 파라미터로 남고 기존 pb 재사용 — 배포·운영
   부담 없음.

### 3.3 (a) 의 회생 조건

- lazy 캐시 미스 비용이 예상보다 커 워밍업이 상각 안 되는 워크로드 (짧은 입력
  다수 + 낮은 State 재사용) 가 지배적일 때 — 정적 열거의 워밍업 0 이 유리.
- §1.5 상한 프로브에서 정적 State 수가 동적 하한의 ~2× 이내로 작게 닫힘이 확인되고
  (late-convergence 우려가 문법 특성상 과대평가로 판명), 콜드 성능이 실제 병목일 때.
- lazy 프로토타입에서 캐시 자료구조의 동시성·수명 관리가 Rust 에서 예상외로 비싸
  순수 테이블 조회의 단순성이 이길 때.

---

## 4. 선택안 (b) 의 단계별 구현 계획

각 단계 = 게이트 그린으로 종료. 게이트는 기존 `runMgroup4DiffTest` 차등 오라클
(G1 = mgroup4(n=1) ≡ mgroup3 byte-identical, G2 = mgroup4(n∈{2,4,6}) ≡ n=1) +
필요 시 확장. 각 단계 후 메인 세션 리뷰 + 커밋.

### G-b0 — State 캐노니컬라이즈 id 매핑 (난이도: 중)

- G0SuffixSetStats 의 캐논 정의를 **정확 키** (지문 아님) `CanonKey` 로 승격,
  `HashMap<CanonKey, Int>` State id 부여. 아직 캐시·전이 없음 — **매 gen 현행
  packing 결과를 캐노니컬라이즈해 id 만 부여하고 검증**.
- **게이트**: `runMgroup4DiffTest` 전부 그린 (계측만 추가, 파스 무영향). +
  단위 검증: 같은 live path 집합이 gen 무관하게 같은 id 를 받는가 (G0 의
  canonicalizationUnitCheck 승격), n=1 State ≡ tipGroupId 1:1.

### G-b1 — term 전이 캐시 (T1) + 캐시 히트 경로 (난이도: 상)

- §2.4 term 전이 캐시 도입. 첫 조우 시 §2.2 합성 (현행 applyTermAction 을 State
  대표 Row 에서 1회) → 캐시. 히트 시 캐시 엔트리로 Row 만 전개 (gen 바인딩).
- 병합 verdict/fold 기계는 **캐시 미스 시에만** 실행. 이후 히트는 재파티션 스킵.
- **게이트**: `runMgroup4DiffTest` byte-parity (전 n). + 캐시 히트율·미스 비용
  카운터 (mg4 stats 확장) — 워밍업 상각 신호.
- 난이도 상: 캐시 히트 경로가 현행 per-gen 재파티션과 **정확히 같은 State 를
  내는지** (Row→State 전개가 fold 와 byte-동일) 가 핵심. 여기서 parity 가 깨지면
  오라클이 즉시 잡는다.

### G-b2 — 경계 reduce 전이 캐시 (T3/T5) + window-exit (T4) (난이도: 상)

- reduce 가 window 를 넘거나 group 노드에 도달할 때의 멤버별 edge 조회를
  `(prefixTemplate, State, replaceMgroupId)` 캐시로. 첫 조우 시 현행
  memberSingletonsForEdge + applyEdgeAction 합성 → 캐시. window-exit 분열도 전이
  캐시 엔트리로.
- **게이트**: `runMgroup4DiffTest` byte-parity. reduceSplits/windowExitSplits
  카운터가 현행(비캐시) 과 일치 (독립 검증).

### G-b3 — 파스 간 캐시 공유 + 수명 관리 (난이도: 중)

- §2.5 캐시를 파서 인스턴스 수준으로 (파스 간 공유). LRU 상한 가드.
- **게이트**: 동일 파서로 다중 파스 → 각 파스가 단일-파스 캐시와 byte-identical
  (공유가 오염 안 냄). 캐시 클리어 후 재구성도 identical (축출 정확성).

### G-b4 — Kotlin 측정 (난이도: 중, 킬 게이트 2)

- §5 성능 검증 (Kotlin 신호). realized R, 캐시 히트율, 미스 비용 분리.
- **킬 게이트 2** (phase_g_plan §1): realized R 이 ceiling 을 크게 밑돌거나
  (<70%), L-예측 기준 Rust 기대 <1.1× 면 G-b5(Rust) 전 중단·보고.

### G-b5 — Rust 이식 + 실측 (난이도: 상, G-b4 통과 시에만)

- mgroup4-native 재배선 (§2.7). Phase B 이식 자산 (group 기계) 을 캐시-백드로.
- **게이트**: Rust parser_diff fixture byte-identical (n∈{1,2,4,6}) + Kotlin 교차.
  최종 시간 판정 (§5, es5 heavies, 예측 대비).

**공수 추정**: G-b0 0.5일, G-b1 2~2.5일 (캐시 히트 parity 관건), G-b2 2일,
G-b3 1일, G-b4 0.5~1일. **Kotlin 합 6~7일.** G-b5 (Rust) 별도 2~3일. (a) 대비
proto+생성기(≈4~5일) 절감.

---

## 5. 성능 검증 계획

Phase B 벤치 방법론 교훈 (phase_b §5.2) 반영:

### 5.1 Kotlin 신호 (파스 결과 무영향 카운터)

- **realized R** = base/merged shape ratio (현행 recordShapeStats 재사용,
  Mgroup4Parser.kt:1436). G0 ceiling 대비 실현율.
- **캐시 히트율·미스 비용**: 상태당 미스 1회 상각이 실제로 성립하는지 —
  히트/미스 카운터 + 미스 self-time (mergeProfile 패턴 재사용).
- **워밍업 곡선**: 파스 초반 미스율 → 포화까지 gen 수 (G0 saturationCurve 와 대조).

### 5.2 Rust 실측 (최종 판정)

- **대상**: es5 heavies (jquery/json2/underscore) + mulang 회귀 (ccgen/jar.bbx).
  기존 `time_parse` 바이너리 패턴 (mgroup4-native/src/bin/).
- **방법론 (phase_b §5.2 교훈 필수 준수)**:
  - **JVM 분리 측정** — Kotlin 신호와 Rust 실측을 절대 한 프로세스에 섞지 않는다
    (Phase A 의 "n=6 1.32× 느림" 이 단일-JVM interleave 편향이었던 교훈).
  - **유휴 확인** — 경쟁 프로세스 부재 확인 후 측정 (Phase B 1차 측정이 mulang
    워처와 겹쳐 오염). 셀당 9~12회, 2라운드 인터리브, sd·드리프트 보고.
  - **콜드/웜 분리** — lazy 는 워밍업이 있으므로 첫 파스(콜드) 와 재파스(웜) 를
    **분리 보고**. 프로덕션 관련성은 웜 (워처 다중 파스); 콜드는 단발 파스 하한.
- **채택 기준**: es5 heavies 웜 n=4 ≥1.2× (§0 예측 하단), mulang 비회귀. 예측
  검증이 목적 — 이 트랙은 성능 도박이 아니라 실측 검증 (phase_g_plan §3 리스크3).

---

## 6. 리스크와 미결정

### R1 — 캐시 히트 경로의 parity (최대 리스크)

캐시 엔트리에서 Row 를 전개해 만든 State 가 현행 per-gen fold 와 **byte-동일**해야.
전개 순서·보고 좌표 귀속이 fold 와 어긋나면 kernelsHistory 가 깨진다.
- **완화**: G-b1 에서 캐시 히트 경로를 현행 fold 와 **병렬 실행·대조**하는 임시
  검증 모드 (양쪽 State 를 equals 로 비교). 그린 확인 후 병렬 검증 제거.

### R2 — 워밍업이 상각 안 되는 워크로드

짧은 입력 다수 + 낮은 State 재사용이면 미스 비용이 지배 → (a) 회생 조건.
- **완화**: G-b4 에서 콜드/웜 분리 측정. 파스 간 공유(G-b3)가 이를 결정적으로
  완화하는지 확인 (워처 워크로드가 주 타깃).

### R3 — 캐시 자료구조 비용 (Rust)

파스 간 공유 캐시의 동시성/해시 비용이 순수 테이블 조회보다 비싸면 이득 잠식.
- **완화**: 프로토타입 단일 스레드 로컬 캐시부터. 공유는 FxHashMap + 파스별 로컬
  + 종료 머지 또는 읽기 다수 RwLock. Phase B 의 mimalloc·scratch 재사용 유지.

### R4 — condClass 근사의 정확성

State 정체성에 조건 **구조 클래스**를 쓰는데, 두 다른 조건이 같은 클래스로 접히면
안 된다 (병합은 조건 동일 시만 — H5). G0 은 지문 근사였으나 캐시는 정확 키 필요.
- **완화**: condClass 를 조건 **전체 템플릿** (gen 태그 포함, 값 제외) 로 — H5
  "동일 condition 병합" 과 정확히 일치. gen 값만 Row 로. G-b1 오라클이 강제.

### R5 — 미결정 목록 (상위 세션 확인 필요)

- **State condClass 의 정밀도**: 구조 클래스(G0) vs 전체 조건 템플릿. R4 는 후자
  권고 — 확인 필요.
- **캐시 수명**: 파서 인스턴스 공유(권고) vs 파스별. 워처 워크로드 이득 크기가
  결정 인자 — G-b3/G-b4 측정 후 확정.
- **워처 경로**: Phase A 는 main 만 group. lazy 도 main 우선 (제약). 워처 일반화는
  mulang 이득의 관건이나 별도 (Phase B backlog §3.1 과 동일 위치).
- **n 기본값**: G0 스위트 스폿 n=2~3, es5 는 n=4 경계. 측정 후 문법별 확정.

---

## 7. 예상 공수 요약

| 안 | Kotlin | Rust | proto/생성기 | 합 | 주 리스크 |
|---|---|---|---|---|---|
| (a) 전체 정적 | ~3일(런타임) | ~2~3일 | ~4~5일 | **~9~11일** | 정적 폭발 + 보고 parity 재현 |
| **(b) lazy ★** | ~6~7일 | ~2~3일 | 0일 | **~8~10일** | 캐시 히트 parity + 워밍업 상각 |

(b) 는 공수 총량이 비슷해 보이나 **리스크 프로파일이 유리**하다: proto/생성기의
보고 parity 재현 (리포에서 가장 까다로운 컴포넌트) 이 0 이고, 정적 폭발 위험이
구조적으로 없으며, 실패해도 기존 pb·mgroup3 무손상 (배포 영향 0). Kotlin 공수는
크지만 전부 기존 코드 (Mgroup4Parser) 위의 캐시-백드 전환이라 신규 컴포넌트가 없다.

---

## 8. 요약 — 상위 세션이 확인할 설계 결정

1. **아키텍처 (b) lazy 채택** — 기존 pb 재사용이 §2.2 조사로 성립 확정. 신규
   proto·생성기 불필요. (a) 의 회생 조건 (§3.3) 은 콜드 병목·정적 소폭 닫힘·Rust
   캐시 고비용 세 경우.
2. **State condClass = 전체 조건 템플릿 (gen 태그 포함, 값 제외)** 권고 (R4) —
   구조 클래스 근사(G0)가 아니라 정확 키. H5 "동일 condition 병합" 과 일치.
3. **캐시 수명 = 파서 인스턴스 공유** 권고 (§2.5) — 워처 다중 파스 상각. LRU 가드.
   파스별 로컬은 반대안.
4. **게이트 = 기존 runMgroup4DiffTest byte-parity 무변경 + 캐시 히트 병렬 검증
   모드** (R1). 새 golden 불요 — 출력 불변이 정체성.
5. **성능 판정 = Rust 웜 실측 (JVM 분리·유휴·콜드/웜 분리)**, es5 heavies n=4
   ≥1.2× + mulang 비회귀. 예측 검증 성격 (도박 아님).
