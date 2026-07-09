# 워처-main 시뮬레이션 공유 — 설계 조사 + 상한 실측 (결론: 구현 비추천)

작성: 2026-07-09. 이 문서는 "워처(cond path)가 main path 와 같은 입력 구간을
중복 시뮬레이션한다"는 관찰로부터 출발한 "워처-main 시뮬레이션 공유" 아키텍처
개선의 설계 조사 + **구현 가치의 정량 근거**를 담는다.

배경: `mgroup3/docs/kernels_history_optimization.md` §0.1–0.4 (파스 상태 폭발
분석, Phase B replace 귀속 수정, Phase C anchor dedup), `watcher_anchor_dedup.md`
(bounded dot-anchor-only), `algorithm.md` §3–4 (런타임 구조/parseStep 7-phase).

## 0. 결론 (먼저)

**공유 상한 실측 결과 1.5× 임계에 크게 미달 (1.000–1.003×) → 구현 비추천.**

원래 가설 — "워처 체인은 조건 심볼 아래 suffix 가 main 체인의 대응 suffix 와
구조적으로 동일하게 진화한다" — 은 **실측으로 반박됐다**. 세 코퍼스
(jar/cc/maven.bbx) 전 구간에서:

- live shape 를 "구조 suffix" (gen-relative 및 gen-free) 로 정규화했을 때
  distinct suffix 수 / 총 shape 수 = **99.7–100.0%** (dedup factor 1.000–1.003×).
- peak gen 에서 **워처 shape 의 0.0% 만이 main shape 와 구조가 일치** (jar 1282
  distinct 워처 suffix 중 0개, cc 1125 중 0개, maven 813 중 0개).
- 런타임 PathShape identity (파서가 실제로 map key 로 쓰는 것) 기준 dedup 도
  **정확히 1.000×** — peak step 의 1519/1333/1021 shape 전부가 서로 다른
  PathShape 다. 즉 **공유할 중복 work 가 없다.**

핵심 원인 (§3): 워처는 main 의 suffix 를 재파싱하는 게 아니라, main 이 **tip
group 으로 추상화해 버린** longest/lookahead body 의 derivation subtree 를
명시적으로 전개한다. main 체인에는 조건 심볼 노드(예 sym1227) 가 **아예
없다** — 워처가 하는 일은 main 이 하지 않는 일이다. 워처 shape 수가 main shape
수를 곱셈으로 미러링하는 것(§3.2)은 사실이지만, 그건 "같은 작업의 복제"가
아니라 "main 의 각 모호성 fork 아래에서 서로 다른 body 를 각각 전개"하는 것.

"조건 평가용 sub-parsing 을 명시적 role 로 표시" (사용자 선호 방향) 자체는
저렴하고 무해하지만, 그것만으로는 절약이 생기지 않는다 — 절약의 원천인
"중복 시뮬레이션"이 실측상 존재하지 않기 때문. 명시적 role 표시가 값을 갖는
별도 용도는 §6.

제안 설계 vs mg2 실제 동작의 관계 판정: **부분 겹침** (§1.4).

## 1. mgroup2 의 기존 동작 — "약간 비슷"의 정체와 복잡성 원천

계측/분석 근거: `ktparser/main/kotlin/.../mgroup2/MilestoneGroupParserKt.kt`,
Scala 원본 `mgroup2/.../MilestoneGroupParser.scala`,
`milestone2/.../MilestoneParserGen.scala`.

### 1.1 mg2 의 워처 = main 과 같은 list 안의, cond 심볼에 rooted 된 일반 path

mg2 `MilestoneGroupPath` = `(first, path, tip, acceptCondition)`
(`ParsingContext.kt:11-27`). `first` 가 root 심볼. live context 의 path 는 두
종류: `first == initialMilestone` (main) vs `first == Milestone(condSym,0,gen)`
(워처). **워처는 별도 서브파서가 아니라 `ctx.paths` 안의 추가 root** 로, main
과 **완전히 같은** `applyTermAction`/`progressTip` 코드로 전진한다
(`MilestoneGroupParserKt.kt:412-448`; Scala `:228-253`). 이 구조는 mgroup3 와
동일하다 (mg3 `paths: Map<PathRoot, PathMap>`, main/cond 구분은 key==mainRoot
뿐 — `ParsingCtx.kt:33-47`).

### 1.2 mg2 가 실제로 "공유/절약"한 것 (세 가지 — 전부 시뮬레이션 공유가 아님)

1. **파생 엔진/루프 통합 (공유가 아니라 단일화)**: 워처와 main 이 한 루프,
   한 코드 경로. → mg3 도 동일 (`Mgroup3Parser.kt:490-534` step1+2 통합 루프).
2. **워처 시동의 step-전체 dedup** (`MilestoneGroupParserKt.kt:409-448`): 이번
   step 에 여러 path 가 같은 root `KernelTemplate` 워처를 요구하면, path 생성
   **전에** `pendedAppendings`/`pendedProgressConditions` 로 coalesce 해 그
   `(symbol, gen)` 당 **fresh depth-1 워처 root 하나만** 만든다. appendings 는
   union, progress 조건은 disjunct. → 이것이 사용자가 말한 "약간 비슷한 것".
   시동 **dedup** 이지 진화(evolution) 공유가 아니다. mg3 는 everSeen 규칙으로
   같은 효과 (`Mgroup3Parser.kt:561-564, 638-643`: 이미 살아있거나 본 적
   있으면 skip).
3. **tracking-filter 재사용** (`MilestoneGroupParserKt.kt:504-507`): 한 번
   시동된 워처 root 는 살아있는 조건이 참조하는 한 gen 을 넘어 지속 → 워처
   하나가 여러 조건/여러 gen 을 재시동 없이 서빙. → mg3 step6 referencedRoots
   (`Mgroup3Parser.kt:784-824`) 가 대응.

### 1.3 왜 mg2 도 워처 중복 비용을 낸다 (jar.bbx mg2 parse 11.2s) — reconcile

mg2 가 dedup 한 것은 **시동**뿐. 일단 만들어진 워처 root 는 매 gen main 과
독립적으로 subtree 전체를 재파생한다 (`MilestoneGroupParserKt.kt:412-433`).
mg2 는 워처 S over span [i..j] 가 main(또는 다른 워처)이 이미 한 work 를
재시뮬레이션하는 걸 인지하지 못한다 — 둘은 코드만 공유하는 독립 root 이기
때문. 11.2s 는 중복 시뮬레이션이 **제거된 적 없음**을 확증한다. **이 관찰이
바로 이번 조사의 출발점이었고, §2 의 실측이 그 "중복"의 실체를 정량화한다.**

### 1.4 판정 — 제안 설계 vs mg2 실제 동작: **부분 겹침**

- **겹치는 부분**: (a) 워처를 별도 root 로 두고 main 과 같은 엔진으로 돌리는
  구조 (mg2=mg3 동일), (b) 워처 시동 dedup (mg2 KernelTemplate coalesce ≈
  mg3 everSeen/span-정규화). 이 둘은 mg2 와 mg3 가 **이미 공유하는 기성
  메커니즘**이고, 제안 설계가 새로 하려던 것과 같은 부류다.
- **다른 부분**: 제안 설계의 핵심 — 워처의 **진행(evolution)을 shared sim
  table 로 물질화해 재사용** — 은 mg2 가 **한 적이 없다**. mg2 의 복잡성은
  "조건↔경로 얽힘"(§1.5)에서 왔지 "sim 공유"에서 온 게 아니다. 따라서 "mg2 가
  sim 공유를 하다 복잡해서 버렸다"는 프레임은 부정확하다 — mg2 는 sim 공유를
  시도조차 안 했다. **제안 설계는 mg2 가 안 가 본 방향이다.**
- 함의: mg2 복잡성 회피 체크리스트(§5)에 설계를 억지로 꿰맞출 이유가 없다.
  단, mg2 복잡성의 진짜 원천(§1.5)은 어떤 워처 관련 작업에도 재발할 수 있으니
  참고 체크리스트로만 사용.

### 1.5 mg2 복잡성의 진짜 원천 (참고 — sim 공유와 무관)

`getProgressConditionOf` 를 통한 **조건↔경로 상호의존**
(`MilestoneGroupParserKt.kt:192-206`): `NotExists(S,g)` 는 직접 평가되지 않고
워처 root `Milestone(S,0,g)` 의 progress 조건을 조회 → 재귀 evolve. 조건의 참
값이 어떤 워처가 존재/진행했나에 달렸고, 어떤 워처를 살릴지가 어떤 조건이
live 인가에 달린 **양방향 결합**. 여기서 파생: `moreTrackingNeeded` 의
evolve-vs-collapse 분기(`:234-264`, 워처 소멸 시 Never/Always 로 조용히 해소 —
실제 오답 버그 이력), `collectTrackings` 의 tip/mid-edge/조건-milestone 3-소스
traversal(`:326-383`, Scala 에 "이게 맞나" TODO 2건), `untrimmedPaths` +
`isEventuallyAccepted` 의 **조건 2회 평가** (parse 중 evolve + history 사후
sweep, `:539-559`). **mgroup3 는 이 얽힘을 조건 대수의 명시적 startGen tag +
per-shape AcceptCondition 분리로 이미 풀었다** (algorithm.md §7). 이번 sim
공유 설계는 이 원천과 무관 — 다른 부류.

## 2. 공유 상한 실측 (구현 가치의 정량 근거 — 핵심)

계측 도구: `mgroup3-native/src/bin/sharing_ceiling.rs` (본 조사용 임시 binary,
랜딩 안 함). 세 정규화로 live shape 를 키로 접어 distinct / total 비율 산출:

- **gen-relative**: milestone 체인의 `(symbolId:pointer@(nodeGen-rootStartGen))`
  + tipGroup. 두 shape 가 같은 키면 root startGen 만큼 offset 을 뺀 시뮬레이션이
  byte-동일 — 가장 강하고 안전한 공유 키.
- **gen-free**: `(symbolId:pointer)` 체인 + tipGroup, gen 전부 제거. 공유
  상한(upper bound) — 서로 다른 anchor 가 실제로는 다른 미래를 가질 수 있음을
  무시하므로 실제보다 후하다.
- **runtime PathShape identity**: 파서가 실제 PathMap key 로 쓰는 그대로
  (`parsing_ctx.rs:217-264`). "중복 work" 의 정직한 측정.

측정 자산: parserdata `perfmeasure/mulang-mg3.pb`, 입력
`perfmeasure/bbxfiles/{jar,cc,maven}.bbx`. release build. 측정 중 동시
cargo/rustc 없음 확인.

### 2.1 whole-parse cost-model integral (step 비용 ∝ shapes → Σdistinct/Σtotal)

| 코퍼스 | 입력(chars) | Σshapes | 상한(gen-rel) | 상한(gen-free) | peak gen: total→distinct |
|---|---|---|---|---|---|
| jar.bbx | 4,757 | 986,038 | **1.000×** (99.9% 유지) | 1.000× (99.9%) | 3277: 1519→1518 (1.001×) |
| cc.bbx | 18,083 | 760,488 | **1.002×** (99.8%) | 1.003× (99.7%) | 4075: 1333→1333 (1.000×) |
| maven.bbx | 24,264 | 836,325 | **1.000×** (100.0%) | 1.001× (99.9%) | 4476: 1021→1021 (1.000×) |

### 2.2 peak gen 상세 (owner flavor + cross-owner 일치)

| 코퍼스 | peak total | main | bounded 워처 | lookahead 워처 | 워처 suffix 중 main 과 일치 | runtime PathShape dedup |
|---|---|---|---|---|---|---|
| jar | 1519 | 236 | 1105 | 178 | **0 / 1282 (0.0%)** | **1.000×** (distinct tipGroup 25) |
| cc | 1333 | 208 | 969 | 156 | **0 / 1125 (0.0%)** | **1.000×** (tipGroup 12) |
| maven | 1021 | 208 | 761 | 52 | **0 / 813 (0.0%)** | **1.000×** (tipGroup 12) |

### 2.3 adjacent-anchor 동일성 (같은 cond 심볼 인접 anchor)

같은 조건 심볼이 여러 anchor 로 살아있을 때, 그 shape 집합이 anchor 간
동일한가 (Phase C 의 인접-anchor 중복이 실은 같은 시뮬레이션인가):

- jar peak sym1227 anchors [2242,2918,3276,3216]: relEq=**false**, freeEq=**false**,
  freeMax/Union=**0.64**. sym1225/792/820 도 전부 freeEq=false (0.64–0.67).
- cc/maven sym483 2 anchors: freeEq=false, freeMax/Union=0.75.

즉 인접 anchor 조차 gen-free 로도 shape 집합이 다르다 (겹침 50–75%). 각 anchor
는 입력의 다른 지점에서 시작해 다른 span 을 소비했으므로 깊은 체인이 갈린다.
Phase C 가 이미 anchor 수를 심볼당 1개로 (m2 동급) 줄였고, 남은 인접 anchor 는
서로 다른 파스 해석이라 공유 불가.

## 3. 왜 공유가 실패하는가 (구조적 원인)

### 3.1 워처 체인은 main 이 추상화한 subtree 를 전개한다

sharing_ceiling 의 체인 덤프 (jar peak, sym1227 = `<AddExpr>` body 의 longest):

```
워처 root sym1227@2242 (236 distinct gen-free 체인):
  W 1227:0/773:2/798:1/670:1/673:1/675:1/967:1/969:1/691:6/773:2/.../7:1/12:1/^177
main 체인 중 sym1227 노드를 포함하는 것: 0개  ("no main chain contains a sym1227 node")
```

main 은 longest body(sym1227) 로 들어갈 때 그 subtree 를 **tip group 으로
추상화** (`algorithm.md §1`: milestone group = 동치 상태 축약). main 체인에는
1227 노드가 없다. 워처는 정확히 그 추상화된 body 를 명시적으로 전개하는
존재다 — **main 이 하지 않는 일**. 따라서 워처 체인과 main 체인 사이엔 공유할
공통 suffix 가 원천적으로 없다.

### 3.2 워처 shape 수 = main shape 수 미러링은 "복제"가 아님

워처 sym1227@2242 의 236 distinct 체인 = main 의 236 shape 와 같은 수
(kernels_history_optimization.md §0.3 의 "×236" 구조 모호성). 그러나 이것은
같은 작업의 236배 복제가 아니라, **main 의 236개 모호성 fork 각각의 문맥
아래에서 longest body 를 각각 전개**한 236개의 서로 다른 시뮬레이션이다
(runtime PathShape 1.000× 가 이를 확증). main fork 를 공유해도 그 아래 워처
전개는 fork 마다 다르므로 접히지 않는다.

### 3.3 "91% 중복 ActionApplication" 은 보고 채널 현상이지 live sim 아님

kernels_history 최적화의 91% 중복 실측은 **HistoryEntry.actionApplications**
(보고용 lazy 기록) 의 중복이었고, 이미 `apps_dedup` (core.rs:722) +
reportedCondRoots 필터로 접혔다. 이는 gen 마다 같은 템플릿+바인딩이 여러 path
에서 기록되는 **보고 레이어** 잉여지, live shape 의 중복이 아니다. live shape
는 §2.2 대로 전부 distinct. → 간접 증거는 sim 공유의 근거가 되지 못한다.

## 4. 설계 대안 비교 (각 대안을 그 자체 복잡성 기준으로 평가)

절약 원천이 실측상 부재(§2)하므로 아래는 전부 **이득 상한 <1.01×** 를
공유한다. mg2 렌즈로 배제하지 않고, 각자의 복잡성 리스크를 구체 기준(신설
상태 규칙 수, 검증 가능성, 기존 불변식 충돌 지점 수)으로 평가:

| 대안 | 공유 단위 | 이득 상한 | 신설 상태 규칙 | 기존 불변식 충돌 지점 | 판정 |
|---|---|---|---|---|---|
| **A. suffix hash-consing (DAG)** | 조건 심볼 이하 체인 suffix | <1.01× (§2.1) | suffix 노드 refcount + owner overlay | MilestonePath Eq/Hash(gen 포함), PathShape key, condPathFinishes anchor 좌표(3), kernels_history rep 바인딩(4) | 이득 없음 → 기각 |
| **B. (sym,startGen,flavor) 공유 sim table + per-owner overlay** | 워처 root 시뮬레이션 전체 | <1.01× (anchor 마다 shape 집합 다름 §2.3) | table 생명주기, overlay 병합, flavor 분기 | evolve 의 activeCondRoots=nextPaths.keys(5), everSeen(6), Phase C anchor(7) | anchor 별 sim 이 실제로 다름 → 기각 |
| **C. main fork 공유 후 워처를 fork-상대로 지연 전개** | main 236 fork | <1.01× (fork 아래 워처가 fork 마다 다름 §3.2) | 지연 전개 트리거, fork 소멸 시 워처 GC | rootProgresses/finishesOut per-root(8) | fork 아래 전개 불가분 → 기각 |
| **D. 명시적 role 태그만 (사용자 선호)** | (공유 안 함, role 표시만) | 0× 성능 | role enum on PathRoot | 없음 (report 전용) | 성능엔 무의미, §6 용도로만 |

세 대안(A/B/C) 모두 신설 상태 규칙 2–3개 + 출력 불변(golden byte-identical)을
위해 조정할 기존 불변식 충돌 지점 3–4개를 요구하는데, **그 대가로 얻는
성능이 <1.01×** 다. 검증 가능성 측면에서도 A/B/C 는 "공유 후에도 per-owner
좌표 기록이 동일한가"를 매 조건 flavor(bounded term MID/edge GRAND,
lookahead 드리프트 쌍)마다 증명해야 하는데(algorithm.md §3.2, watcher_anchor_dedup.md §5),
이는 950f1b9d arc 에서 실측된 고위험 영역(mut def/a^n b^n c^n 다세대
lookahead 파손)이다. **비용 대비 전면 비추천.**

## 5. (참고) mg2 복잡성 함정 회피 체크리스트 — 향후 워처 작업 공통

§1.4 판정대로 이번 설계는 mg2 와 다른 부류지만, 어떤 워처 관련 최적화에도
아래는 유효:
1. 조건을 "경로 population 포인터"로 만들지 말 것 — mg3 는 startGen tag +
   per-shape AcceptCondition 으로 이미 self-contained (지킬 것).
2. 워처 소멸 시 조건을 Never/Always 로 접는 분기를 신설하지 말 것 (mg2 오답
   원천). mg3 는 evolve 의 both-null 경로로 처리 (algorithm.md §5).
3. 조건 2회 평가 재도입 금지 (kernels_history 는 이미 leaf-직접 조회로 sweep
   제거 — kernels_history_optimization.md §3).
4. anchor 를 지울 땐 "그 key 를 참조하는 조건이 없다"가 전제 (zombie 흡수 오답
   — watcher_anchor_dedup.md §5).

## 6. "조건 평가용 sub-parsing 명시적 role 표시" — 성능 외 값

사용자 선호 방향. 성능 절약은 없으나(§4-D) 아래엔 값:
- **진단/프로파일링**: root 를 main/bounded-watcher/lookahead-watcher 로
  분류(현재는 lookaheadCondSymbols 조회로 유추, sharing_ceiling.rs 가 그리
  함) — profile_steps 출력 가독성 향상.
- **step6 규칙 문서화**: bounded=dot-only vs lookahead=3-anchor 가 flavor 에
  달렸으므로 (core.rs:668-687), role 을 PathRoot 에 명시하면 그 분기가 자기
  설명적이 됨. 단 proto/generator 불변 유지하려면 런타임 파생 role 로.
- 이건 별도 소규모 정리 작업(리스크 낮음, golden 불변)이지 성능 arc 아님.

## 7. Rust-only vs 양쪽 — 무의미 (구현 안 하므로)

원 과제의 Rust-only 우선 판단: 출력 불변이면 Kotlin 레퍼런스 유지 + Rust 만
최적화가 스코프를 줄인다는 전례(P1/P3)는 유효하나, **공유 자체를 구현하지
않으므로 해당 없음**. 만약 향후 <1.01× 를 감수하고라도 진행한다면: sharing
은 순수 런타임 최적화(generator/proto 불변 가능 — 워처 shape 은 이미 런타임
전개물)라 Rust-only 가 맞다. 단 그 경우에도 §4 의 충돌 지점 3–4개를 Rust 에서
golden byte-identical 로 통과시켜야 하고, 이득이 없으므로 권하지 않는다.

## 8. 단계별 계획 + 중단 기준 (Stage 0 에서 이미 중단)

- **Stage 0 (완료 — 본 문서)**: 공유 상한 실측. 게이트: 상한 ≥ 1.5× 이면 진행,
  미만이면 중단. **실측 1.000–1.003× → 중단.** 변경 파일: 없음(계측 임시
  binary 만, 랜딩 안 함).
- **Stage 1 이하 (미실행)**: 원 계획은 (1) 명시적 role 태그 + shared shape
  table 골격, (2) bounded 워처 suffix hash-consing, (3) finish 이벤트 owner별
  분배, (4) kernels_history 좌표 불변 검증, (5) step6/everSeen 통합. 각 단계
  게이트 = 전체 스위트 + parser_diff golden byte-identical + m2 parity +
  11코퍼스 fingerprint + bibix4 runMgroup3NativeTest + mulang 55/55. **Stage 0
  게이트에서 중단되어 실행하지 않음.**

향후 재검토 트리거: 문법이 크게 바뀌어 워처가 main 체인의 실제 suffix 를
전개하게 되는 경우(현재 mulang 은 아님), 또는 sharing_ceiling 상한이 다른
문법/입력에서 ≥1.5× 로 측정되는 경우에 한해 Stage 1 재개.

## 9. 남은 실효 최적화 방향 (이 arc 밖 — 참고)

sim 공유가 막혔으므로 파스 비용의 실질 절감은 다른 트랙:
- **문법 트랙** (`mgroup3/docs/mulang_grammar_ambiguity.md`): jar.bbx 비용의 더
  큰 구조적 원인은 블록-스팬 longest 두 가족(`<CallChain_+>`, `<AddExpr>`)의
  워처 자체 — 문법 개선이 성사되면 워처가 사라지므로 sim 공유보다 효과 큼
  (단 언어 설계 결정 필요, 사용자가 언어 변경 배제 시 불가).
- **walk/encode 후속** (kernels_history_optimization.md §0.4 잔여).

## 10. 계측 코드 요약 (diff 요약만 — 랜딩 안 함)

신규 `mgroup3-native/src/bin/sharing_ceiling.rs` (~250줄, 본 조사 전용):
- `chain_nodes` / `key_gen_relative` / `key_gen_free`: shape → 구조 suffix 키.
- whole-parse integral: 매 gen ceiling_at 누적, Σdistinct/Σtotal.
- peak dump: owner flavor 분류(lookahead_cond_symbol_ids 조회), cross-owner
  일치율, adjacent-anchor gen-free 동일성, runtime PathShape identity dedup.
- `MG3_SHOW_ROOT`: 특정 cond 심볼의 gen-free 체인 vs 그 심볼 노드를 포함하는
  main 체인 덤프 (§3.1 의 "main 에 1227 노드 없음" 확증).
기존 코드 변경 0 (읽기 전용 계측). worktree 에만 존재.
