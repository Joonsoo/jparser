# mgroup4 Phase A — bounded interior milestone groups 설계·구현계획

작성: 2026-07-10. 상태: **설계 문서 (구현 전).** 상위 세션 리뷰 후 구현 승인 대상.
전제 문서: `mgroup4_bounded_interior_groups.md` (§2 아이디어, §6.2 병합 정의,
§6.7 판정 갱신 = Phase A 확정 스코프), `kernels_history_optimization.md`
(엔진 구조·최적화 이력). 이 문서는 **코드 라인 참조로 가설을 검증**한 결과와
그에 기반한 표현/알고리즘/보고 불변/단계 계획/측정/리스크를 담는다.

Phase A 형태 (§6.7 확정): **n 을 런타임 파라미터로 (n=1 ≡ 현행 비트동일), main path
우선 (워처 경로 현행 유지), Kotlin 프로토타입, 성능 기준 es5 heavies + chain_boundaries.**

---

## 0. 현 코드 지도 (라인 참조)

### 0.1 path / tip group 데이터 구조

핵심 파일: `mgroup3/parser/kotlin/com/giyeok/jparser/mgroup3/ParsingCtx.kt`.

- **live path 의 표현**은 `(PathRoot) → PathMap` 이다. `ParsingCtx.paths:
  Map<PathRoot, PathMap>` (ParsingCtx.kt:35). `PathRoot` 하나가 main root 이고
  (`ctx.mainRoot`), 나머지는 watcher(cond) root. `PathMap = Map<PathShape,
  AcceptCondition>` (ParsingCtx.kt:26) — 한 root 아래의 살아있는 path 들을
  shape→condition 으로 담는다. 같은 shape 로 도달한 서로 다른 condition 은
  `addPath` (ParsingCtx.kt:201) 가 `Or.from` 으로 합친다.

- **PathShape** = `(milestonePath: MilestonePath?, tipGroupId: Int)`
  (ParsingCtx.kt:4-23). `milestonePath` 가 root→tip 의 **interior 체인**이고
  (nullable = 체인 비어 있음 = root 직속 tip group), `tipGroupId` 가 tip 의
  milestone **group** id. **즉 현행에서 group 은 tip 한 자리만** — interior 체인
  노드(`MilestonePath.milestone`)는 전부 singleton `Kernel` 이다. 이것이
  mgroup4 가 확장하려는 지점이다.

- **MilestonePath** (ParsingCtx.kt:135-192): linked list. 한 인스턴스 = 한 엣지.
  필드: `gen`(런타임 부착 gen — 조건 anchoring 과 한 몸, 불변), `milestone:
  Kernel`(이 노드의 singleton milestone), `parent: MilestonePath?`,
  `observingCondSymbolIds: List<Int>`(이 엣지가 추적할 cond symbol),
  그리고 **보고 전용 shadow gen** `reportGen`/`milestoneReportGen`.

- **shape 의 정의 (Phase 0 probe 와 일치)**: `interior_merge.rs:9-27` 이 확정한
  용어 — live path = `root → m_1 → ... → m_L → tipGroup(id)`. depth 는 tip 기준:
  depth 1 = tip group (= n=1 window = 현행 grouping), depth d≥2 = 체인 index
  `L-(d-1)` 의 milestone. L 개 milestone path 는 window position depth 1..L+1.

### 0.2 equality / dedup 계약 (OOM 지뢰 — 최우선 리스크)

- `PathShape.equals` (ParsingCtx.kt:17-21): `tipGroupId` 동일 AND `milestonePath`
  동일. `hashCode` (10-16): `31*milestonePath.hashCode() + tipGroupId`, lazy 캐싱.
- `MilestonePath.equals` (ParsingCtx.kt:167-178): `gen` + `milestone` +
  `observingCondSymbolIds` + `parent`(재귀) 동일. **`reportGen`/`milestoneReportGen`
  은 의도적으로 equals/hashCode 에서 제외** (172-174 주석: 포함하면 보고 좌표
  변형마다 path 가 분리돼 "mulang 에서 OOM 실측"). 같은 shape 로 병합되면 먼저
  도착한 인스턴스의 보고 좌표가 유지된다(근사).
- 이 equality 가 `PathMap`(HashMap) 의 dedup 키다. **표현을 바꿀 때 이 계약을
  깨면 (a) 병합이 안 되거나 (b) 보고 좌표가 path 를 갈라 OOM.** mgroup4 의 group
  표현은 이 계약과 정확히 호환돼야 한다 (§1.4).

### 0.3 term/edge action 적용 흐름 (fork 발생 지점)

파일: `Mgroup3Parser.kt`. 한 step (`parseStep`, :454) 은:

1. **step 1+2** (:490-534): `ctx.paths` 의 매 (root, shape) 에 대해
   `findApplicableAction(shape, input)` (:183) 로 tip group 의 term action 을
   찾아 `applyTermAction` (:197) 적용. main·cond 같은 loop.

2. **`applyTermAction`** 이 두 종류의 전이를 만든다:
   - **`replaceAndAppends`** (:229-255) — **descend/expand**. 각 rea 마다
     `replaceKernel = Kernel(rea.replace.symbolId, rea.replace.pointer, parentGen)`
     을 만들고, 새 `MilestonePath`(:235, parent = oldShape.milestonePath)를 쌓아
     체인을 한 칸 늘리고 새 tip group `rea.append.milestoneGroupId` 을 붙인다.
     → `nextPathsOut.addPath(PathShape(newMilestonePath, tipGroupId), combined)`
     (:245). **★ 이것이 fork 의 주 발생 지점** (§H1/H2 검증 참조).
   - **`replaceAndProgresses`** (:257-307) — **reduce/progress**. tip group 이
     자기 완성(progress)돼 부모로 전파. `parentPath == null`(root 직속)이면 root
     finish 기록 (:263-278), 아니면 `tipEdgeActionsMap` 조회 후 `applyEdgeAction`
     (:285) — tip 을 pop 하고 부모 엣지 액션 적용.

3. **`applyEdgeAction`** (:315-430):
   - `appendMilestoneGroups` (:355-377) — parent 노드는 유지(`parentPath.copy` —
     체인 성장 없음, tip group 만 교체 :362-368) 후 새 tip group append.
   - `startNodeProgress` (:379-429) — 이 엣지도 완성돼 grandParent 로 재귀 전파.
     `grandParent == null` 이면 root finish, 아니면 `midEdgeActionsMap` 조회 후
     `applyEdgeAction` 재귀 (:406). **이것이 reduce 체인** (자식 완료가 여러
     레벨 pop 을 일으킴).

**엣지 액션 조회 키 (H3 검증에 결정적)**:
- `tipEdgeActionsMap` (:56-59): key = `(KernelTemplatePair(parent.symbolId,
  parent.pointer), tipGroupId)`.
- `midEdgeActionsMap` (:62-68): key = `(parentKernelTemplate, tipKernelTemplate)`.
- **둘 다 gen 을 키에 넣지 않는다** — `KernelTemplatePair = (symbolId, pointer)`
  (ParsingCtx.kt:198). 즉 엣지 액션은 노드의 **템플릿 identity 로만** 결정되고
  gen 은 무관. → interior group 멤버가 같은 `(symbolId, pointer)` 를 공유하면
  같은 엣지 액션을 탄다 (§H3).

### 0.4 condition / 보고 바인딩이 path 에 붙는 방식

- **condition** 은 shape 에 붙지 않는다 — `PathMap` 의 **value** (`AcceptCondition`)
  다. shape 별로 다른 condition 을 가질 수 있고, 같은 shape 는 `Or` 로 합쳐진다.
  condition 은 `referencedRoots` (AcceptCondition.kt:8) 로 어떤 cond root 를
  참조하는지 안다.
- condition anchoring gen 은 **런타임 바인딩** (parentGen/grandGen 등,
  `applyTermAction` :214-215) 으로 resolve — `MilestonePath.gen` 이 그 anchor.
  이 gen 은 **불변** (조건 anchoring 과 한 몸).
- **보고 바인딩**은 두 종류:
  - `ActionApplication` (ParsingCtx.kt:84-95) — 매 action 적용마다 저장되는
    lazy record. `actions`(ParsingActionsPlain 참조) + `root` + rt*(런타임
    바인딩, 조건 resolve) + rep*(보고 좌표) + `condition`(구동 조건). parseStep
    이 이걸 `appsByGroup` 에 모아 HistoryEntry 에 저장.
  - `MilestonePath.reportGen`/`milestoneReportGen` — shadow 필드 (equals 제외).
    term descend 시 새 tip 부착 gen 을, edge append 시 갱신 gen 을 기록.
- **보고 해석**은 `kernelsHistory` (:925-994) 가 한다 — HistoryEntry 의 각 app 을
  lazy 해석: 조건은 rt* 로 resolve, 좌표는 rep* 로 resolve. `RecordConditionEvaluator`
  (조건 평가 leaf-직접 조회) 가 evolve replay 없이 같은 답을 낸다.

### 0.5 정리 — mgroup4 가 건드리는 것과 안 건드리는 것

| 계층 | 현행 | mgroup4 Phase A |
|---|---|---|
| interior 체인 노드 | singleton `Kernel` (MilestonePath.milestone) | 마지막 n 개는 **group** 허용 |
| tip group | `tipGroupId: Int` (이미 group) | 불변 |
| condition | PathMap value, shape 무관 | **멤버별 동일할 때만 병합** (§H5) |
| 보고 (ActionApplication/reportGen) | app 당 lazy record | **분열/기록 시 소급 생성** (§3) |
| kernelsHistory 출력 | m2-parity byte-exact | **완전 불변** (correctness gate) |
| equality/hash | reportGen 제외 | **동일 계약 유지** (§1.4) |

---

## 검증된/기각된 가설 (H1-H5)

### H1 — path = [root, singleton 체인, tip group]; term rea 가 체인 성장; fork 는 tip group 멤버 descend

**검증 (부분 수정 필요).** path 구조는 정확 (§0.1). "term rea 가 체인을 성장"도
정확 (`applyTermAction` :235 새 MilestonePath). 그러나 **"fork 는 tip group 의
여러 멤버가 각자 descend 할 때"는 부정확** — tip group 은 런타임에 멤버로 펼쳐지지
않는다. tip group id 는 정적 id 이고, 그 group 에 대한 term action
(`plain.termActions[tipGroupId]`)이 **여러 `replaceAndAppends` 를 담고 있어**
한 shape 가 여러 새 shape 로 갈라진다 (:229 loop). 즉 **fork = 한 (root,shape)
에서 term action 의 replaceAndAppends 가 2개 이상 매치될 때, 각 rea 가 별도
new-shape 를 `nextPathsOut` 에 추가** (:245). 형제 path 들은 prefix(oldShape) +
새 노드(replaceKernel) + 새 tip group 만 다르다.

→ **수정된 H1**: fork 는 `applyTermAction` 의 rea loop 에서 한 부모 shape 가 여러
자식 shape 로 갈라지는 지점(:229-255)에서, 그리고 (드물게) `applyEdgeAction` 의
appendMilestoneGroups loop(:355)에서 발생한다.

### H2 — 같은 append 대상끼리는 형제 path; 생성 시점에 병합 그룹을 공짜로 안다; rea 를 append 대상별 pre-group

**부분 검증 — 중요한 재해석 필요.** "생성 시점에 형제를 안다"는 **맞다**: 한 부모
shape 의 rea loop 안에서 갈라지는 모든 자식은 정의상 형제(prefix 동일)다. 그러나
**"append 대상(newTip)이 같은 멤버끼리 병합"이라는 축은 틀렸다.** Phase 0 이 잰
병합은 (§6.2 병합 정의) *같은 root·같은 길이·window 밖 노드 전부 동일·window 안
정확히 한 위치만 상이*다. 병합되는 형제들은 **tip group 은 오히려 다를 수 있고
(각 rea 가 다른 `append.milestoneGroupId`), 새로 쌓은 milestone 노드
(`replaceKernel`)가 상이 노드**다.

또한 **fork 는 한 term action 안에서만 생기지 않는다**: Phase 0 의 depth-2 fork
(80.7%, es5 실측 §5.1)는 "직전 gen 에 서로 다른 노드로 갈라진 두 형제 shape 가,
이번 gen 에 **같은 term action 을 타서** 같은 새 tip 노드를 append — 그래서 tip
쪽 노드는 같아지고 직전에 갈라진 노드(now depth 2)만 다른" 상태다. 즉 **병합
그룹은 "직전에 갈라진 형제가 이후 같은 전이를 공유하며 나란히 자란" 결과**이지
"한 rea loop 의 산물"이 아니다.

→ **H2 재설계 (§2.1)**: 생성 지점 pre-grouping 은 **불충분**하다. 병합은
**per-gen, per-root 로 "지금 살아있는 shape 들 중 window 안 한 위치만 다른
형제"를 묶는 것** — Phase 0 probe(`merge_greedy`)가 하는 것과 같은 연산을 파서
런타임의 자료구조로 하는 것이다. 다만 **fork 를 매 gen 재탐색할 필요는 없다**:
group 은 한번 형성되면 같은 전이를 공유하는 한 유지되고 (§H3), 새 fork 만 병합
후보다. 프로토타입은 "매 gen nextPaths 를 window-키로 재파티션"(단순, 정확)으로
시작하고, 증분화는 최적화로 미룬다.

### H3 — reduce 시 멤버별 edge action 이 갈리면 분열, 같으면 유지; 멤버별 조회는 런타임 반복

**검증.** 엣지 액션 조회 키는 노드의 `(symbolId, pointer)` 템플릿뿐 (§0.3, gen
무관). interior group 멤버가 **같은 tip 노드(방금 append 된)와 group 을
공유하지만 group 화된 interior 노드에서 (symbolId,pointer)가 다르면**, reduce 가
그 interior 노드까지 pop 해 내려갈 때 (mid edge 조회, :401-402) 멤버별로 다른
midEdgeAction 을 타 **그 시점 분열**한다. group 안 멤버가 같은 템플릿이면(불가능
— 멤버는 정의상 다른 노드) 문제 없음. reduce 가 group 노드까지 내려가기 전에
멈추면 group 유지. → 멤버별 midEdge 조회를 **런타임 반복**으로 (프로토타입)
수행하는 것 타당. precompute 는 Phase B.

### H4 — window-exit: append 로 group 노드가 tip 에서 n+1 번째가 되면 지연 분열

**검증 (구조적 확정).** 체인은 term rea(:235)로만 자란다 (edge append 는 tip
group 만 교체, 체인 성장 없음 — :362 copy). group 노드가 depth d 일 때 term
descend 가 일어나면 tip 쪽에 새 노드가 쌓여 group 노드가 depth d+1 로 밀린다.
depth 가 n 을 넘으면 (n+1) window 밖 → **멤버별로 분열**해야 한다. Phase 0 P2
(depth 분포)와 P3(window-exit)이 이 사건의 빈도를 이미 쟀다. es5 는 depth-2
지배(80.7%)라 n=4 window 에서 대부분 window-exit 전에 pop.

### H5 — per-path 부착 상태(condition/보고)가 병합 제약; 부착 상태 동일할 때만 병합

**검증 — "부착 상태"의 정체 확정.** shape 에 붙는 것은:
1. **condition** — PathMap value. 멤버별로 다를 수 있다 (다른 rea 는 다른
   `append.acceptCondition`, :230). **병합은 condition 이 동일할 때만 안전** —
   다르면 group 을 하나의 (root, shape) 로 접을 수 없다 (evolve/평가가 shape 당
   하나의 condition 을 가정). 단 condition 이 다른 멤버는 **애초에 Phase 0
   probe 가 병합으로 세지 않았다** (probe 는 shape 만 봄) — 즉 §6.2 ceiling 은
   condition 을 무시한 상한이고, **여기서 실현율이 깎일 수 있다** (리스크 §6).
2. **observingCondSymbolIds** (MilestonePath 필드, equals 포함) — 멤버별로
   다르면 equals 가 이미 갈라 놓는다.
3. **보고 shadow gen** (reportGen/milestoneReportGen) — equals 제외라 병합에
   제약 없음. 단 **분열 시 멤버별 보고 좌표를 복원**해야 한다 (§3 — 가장 어려움).

→ **H5 정책 (프로토타입, 보수적)**: **group 은 (a) 동일 condition, (b) 동일
observingCondSymbolIds, (c) window 안 정확히 한 위치만 상이한 멤버들끼리만 형성.**
condition 이 다르면 병합 안 함 (별도 shape 유지). 이 정책의 실현율 손실은 A1
측정으로 정량화 (§5). condition 이 병합을 얼마나 깎는지가 Phase A 의 핵심
미지수다 — Phase 0 은 이걸 재지 않았다.

---

## 1. 표현 설계

### 1.1 interior group 노드 — singleton 통합 타입

`MilestonePath.milestone: Kernel` 을 **멤버 집합**으로 일반화한다. 두 대안:

- **(A) 별도 타입** — `MilestonePath.milestone` 을 `Kernel` 에서
  `MilestoneNode`(sealed: `Single(Kernel)` | `Group(members)`) 로. 명시적이나
  전 호출처(edge 조회 :401, kernelsHistory 좌표 등)를 다 고쳐야 함.
- **(B) 통합 — group 을 항상 집합으로** — `milestone: Kernel` → `members:
  List<Kernel>` (또는 정렬된 배열). n=1 이면 항상 size-1. Single 은 size-1 group.

**결정: (B) 변형 — 단, n=1 제로코스트를 위해 "size-1 은 기존 필드 그대로"**.
구체적으로 `MilestonePath` 에 `milestone: Kernel`(대표 멤버, 항상 존재) 를
유지하고, **선택적** `groupMembers: List<Kernel>?`(null = singleton) 을 추가.
- n=1 (프로토타입 기본이 아닌 A/B 옵션): `groupMembers` 항상 null →
  기존 코드 경로 비트동일 (제로코스트 하위호환, 절대제약 2).
- n≥2: group 노드면 `groupMembers = [m_0, m_1, ...]`(정렬), `milestone = m_0`
  (대표 — 좌표/템플릿 조회의 "아무 멤버"로 쓰되 group-aware 경로에선 전 멤버 순회).

이유: (A)는 sealed match 를 전 좌표 계산에 강제해 diff 가 큼. (B)는 hot 경로
(term descend, tip group 조회)가 group 을 안 건드리므로 (interior 노드는 tip 이
아님) 대부분 unchanged. group 을 실제로 펼치는 곳은 (i) reduce 가 group 노드까지
내려갈 때 (mid edge, :401), (ii) window-exit 분열, (iii) 보고 해석 — 세 군데뿐.

### 1.2 멤버 집합 표현

- **정렬된 `Array<Kernel>` 또는 `List<Kernel>`** (canonical order:
  `compareBy(symbolId, pointer, gen)`). 정렬은 equality/hash 안정성에 필수
  (§1.4). 멤버 수는 Phase 0 상 작다 (k=1 병합이므로 window 안 상이 노드 1개 —
  group 크기는 fork 폭 = 대개 2~8, §0.2 kernels_history §0.3 "타워 ×5").
- **멤버당 condition/보고 좌표**: §H5 정책상 group 은 **동일 condition** 일 때만
  형성하므로 멤버는 condition 을 개별로 안 든다. 그러나 **보고 좌표
  (reportGen/milestoneReportGen)는 멤버별로 다를 수 있다** → group 노드는
  **멤버별 보고 좌표 배열**을 (equals 제외 shadow 로) 병렬 보관해야 분열 시
  복원 가능 (§3). 프로토타입: `groupMembers: Array<Kernel>` 와 병렬 배열
  `groupMemberReportGens: IntArray?`(milestoneReportGen 멤버별), `reportGen`
  (tip 부착 gen — 대개 멤버 공통, 같은 term action 이므로).

### 1.3 n-window 추적

**결정: window 는 명시 필드 없이 체인 위치로 계산.** MilestonePath 는 이미 tip
에서의 depth 를 O(1) 로 모른다(linked list) — 그러나 **group 노드는 자기 depth 를
알 필요가 있다** (window-exit 판정). 두 방법:
- (a) group 노드에 `depthFromTipAtFormation` 저장 + 매 descend 시 +1 — 갱신
  비용.
- (b) **window-exit 를 depth 로 판정하지 않고 "체인 성장 이벤트"로 판정**:
  group 노드가 있는 shape 에 term descend(체인 +1)가 일어나면, 그 순간 새 tip
  으로부터 group 노드까지의 거리를 계산(짧은 체인이라 저렴) — n 초과면 분열.

**결정: (b) — group 노드 참조를 shape 에 캐시.** `PathShape` 는 이미
milestonePath 를 들고 있으므로, group 을 포함한 노드로의 참조를 파서가 descend
시점에 O(체인길이) 로 찾는다(체인은 짧음, kernels §0.1 "43-48 깊이"는 극단, 평균
훨씬 짧음). n 이 작아(4~6) window 밖 노드는 즉시 분열되므로 **살아있는 group 은
항상 tip 근처 n개 안** — 순회 비용 유계.

### 1.4 equality / hash 처리 (OOM 지뢰 대응)

**절대 제약 5: 기존 dedup 의미 불변.** group 표현이 equals 에 정확히 반영돼야
"같은 group = 같은 shape" 로 dedup 되고, "다른 group ≠" 로 분리된다.

- `MilestonePath.equals` (ParsingCtx.kt:167) 에 group 멤버 비교 추가:
  singleton 은 `milestone` 비교(현행), group 은 `groupMembers` **내용 비교**
  (정렬돼 있으므로 `contentEquals`). **`groupMemberReportGens` 는 equals/hash
  제외** — 현행 reportGen 제외 규칙(:172-174)의 연장. 이유 동일: 보고 좌표가
  멤버 사이에서 갈리면 path 폭발/OOM.
- `hashCode`: singleton 은 현행, group 은 멤버 배열 해시 (order 안정 — 정렬됨).
- **불변식**: n=1 이면 groupMembers=null → equals/hash 가 현행과 **비트동일**
  (제로코스트 게이트). 이건 A0 에서 전 스위트로 검증.

**리스크**: group 멤버의 "동일 condition" 판정이 shape equality 밖(PathMap
value)에 있으므로, **같은 shape 인데 condition 다른** 멤버를 group 에 넣으면 안
됨 — 병합은 PathMap 을 순회하며 `(shape 형제 && condition 동일)` 을 확인 후에만
group 생성 (§2.1). 이 순서를 어기면 조건 평가가 틀린다 (correctness gate 로 잡힘).

---

## 2. 알고리즘 변경점

각 항목: [관련 함수 / 수정 범위 / H 검증 반영].

### 2.1 병합 형성 — per-gen, per-root (H2 재설계 반영)

**H2 검증 결과, 생성 지점 pre-grouping 은 불충분** (형제가 여러 gen·여러 action
에 걸쳐 나란히 자라 만들어짐). 대신 **step 5 evolve 직후·step 6 prune 전**에
main root 의 evolved PathMap 을 대상으로 병합 패스를 넣는다:

```
mergeInteriorGroups(mainPathMap: Map<PathShape, AcceptCondition>, n): Map<PathShape, AcceptCondition>
  // Phase 0 merge_greedy 의 런타임판. d=2..n greedy:
  //  - 아직 병합 안 된 shape 들을 (condition, observingCondSymbolIds,
  //    window-밖-노드-전부, tip, length, idx) 를 키로, window 안 depth-d
  //    노드만 wildcard 로 파티션.
  //  - size>=2 클래스 = 병합 그룹. depth-d 노드들을 group 으로 접어 한
  //    PathShape 로 만들고 condition 은 (동일하므로) 그대로.
  //  - 접힌 멤버 consume (다음 d 재병합 방지).
```

- **위치**: `parseStep` step 5 (:762-767) 뒤, main root 만 (`ctx.mainRoot`).
  워처는 현행 유지 (절대 제약 3 — main path 만 group 허용).
- **키에 condition 포함** (§H5): condition 이 다르면 다른 파티션 → 병합 안 됨.
  이것이 실현율을 깎는 지점 — A1 에서 측정.
- **프로토타입은 매 gen 전체 재파티션** (단순·정확). 증분화(직전 group 유지 +
  새 fork 만 병합)는 A2 이후 최적화.
- n=1 이면 이 패스는 no-op (d=2..1 = 빈 범위) → 현행 경로.

### 2.2 reduce 시 분열/유지 (H3)

`applyTermAction` 의 replaceAndProgresses(:257) / `applyEdgeAction`(:315) 이
**group 노드를 만나면**:
- reduce 가 tip group progress → tipEdge(:280) 는 tip group id 로만 조회 (group
  노드 안 건드림) → **group 유지**.
- reduce 가 startNodeProgress 로 parent 를 pop 하고 **parent 가 group 노드**면
  (:401 midEdge 조회 시 `parentPath.milestone.kernelTemplate` 이 멤버마다 다름)
  → **멤버별로 midEdge 를 조회, 다르면 분열**. 구현: parent 가 group 이면 멤버
  loop 을 돌며 각 멤버의 `(grandParent.template, member.template)` midEdge 를
  조회 → 결과가 같은 멤버는 같은 자식 shape(재병합 가능), 다른 멤버는 분리.
- **수정 범위**: `applyEdgeAction` 의 grandParent pop 분기(:400-427)에 "parent 가
  group 이면 멤버별 순회" 추가. tip progress 분기(:280)는 group 무영향.

### 2.3 window-exit 지연 분열 (H4)

`applyTermAction` 의 replaceAndAppends(:229) 로 체인이 자랄 때, oldShape 에
group 노드가 있고 그 노드가 새 체인에서 depth > n 이 되면 **descend 전에 분열**:
group 을 멤버별 singleton 체인으로 풀어 각각 descend. 구현: `applyTermAction`
진입 시 oldShape 에 window-밖 group 이 있는지 검사(§1.3 (b)) → 있으면 멤버별로
oldShape 를 펼쳐 각각에 대해 rea 적용. n 이 작아 이 검사는 tip 근처 n칸만 봄.

### 2.4 부착 상태 정책 (H5)

§H5 결정 반영: 병합 키에 `condition` + `observingCondSymbolIds` 포함 (§2.1).
group 형성은 **셋 다 동일 + window 한 위치만 상이**일 때만. 보고 좌표
(milestoneReportGen)는 멤버별로 병렬 보관하되 equality 제외 (§1.2/1.4).

---

## 3. 보고 / 골든 불변 전략 (가장 어려운 부분)

**목표 (절대 제약 1·4)**: kernelsHistory 출력이 현행과 **완전 동일**. group 은
내부 표현일 뿐. fixture/golden 재생성 불요.

### 3.1 문제의 핵심

현행 보고는 `ActionApplication` (매 action 적용마다, 파스 핫패스에서 :224/:345)
을 통해 흐른다. app 은 `root` + rt/rep 바인딩 + condition 을 담고, kernelsHistory
(:936)가 lazy 해석한다. **group 으로 병합하면, 병합된 멤버들이 각자 냈어야 할
app 이 하나로 접힌다 — 그대로 두면 멤버별 보고 좌표(kernel begin/end)가 사라져
kernelsHistory 출력이 달라진다.**

구체적으로 위험한 지점 세 곳:
1. **term descend app** (:224): 새 tip 부착 시 app 기록. group 멤버가 같은 term
   action 을 타면 (병합의 전제 — §H2) app 의 `actions`(ParsingActionsPlain)와
   rt/rep 바인딩이 **멤버 공통**이므로 app 하나로 접혀도 무손실. **단 rep 좌표가
   `reportParentGen`(:218)/`reportGrandGen`(:219)을 참조하고, 이건 멤버별
   milestoneReportGen 에서 옴** → 멤버별로 다르면 app 을 멤버별로 내야 함.
2. **reduce/edge app** (:345): parent 가 group 이면 멤버별 midEdge 가 달라 app
   도 멤버별 (이미 §2.2 에서 분열하므로 각자 정상 기록).
3. **reportGen shadow 좌표**: 병합된 group 이 유지되는 동안, 다음 gen 들의 app
   이 이 group 노드의 milestoneReportGen 을 참조할 수 있다 → 멤버별 좌표 필요.

### 3.2 전략 — "보고는 멤버별로, 파스는 group 으로" (분열-소급 아님, 기록-시-확장)

**결정: group 을 만들 때 멤버별 보고 좌표를 group 노드에 보존하고, app 기록
시점에 group 을 보고 목적으로만 펼친다 (파스 상태는 group 유지).** 즉:

- **파스 상태**(nextPaths/condition/evolve)는 group 하나로 — 이것이 성능 이득.
- **ActionApplication 기록**은, group 노드를 참조하는 app 이 생길 때 **멤버별로
  app 을 복제** (rep 좌표만 멤버별 milestoneReportGen 으로 다르게, 나머지 동일).
  app 은 HistoryEntry 에 쌓이는 가벼운 튜플이고 (:843 dedup 됨), **개수가
  현행과 같아진다** → kernelsHistory 출력 불변.

이 전략의 근거: 이득의 원천(§2.3 doc §2.2)은 **파스 시뮬레이션 비용**(evolve,
term action 조회, condition 결합)이지 app 기록이 아니다. app 은 이미 per-gen
dedup(:843 LinkedHashSet) 되고 개수가 shape 수보다 적다. **app 을 멤버별로
내도 파스 shape 수는 group 으로 접힌 채** — 시간 프록시(§6.0 shapes-선형)는
shape 수이므로 이득 보존.

### 3.3 구현 지점

- **term descend** (:224): oldShape 가 group 을 포함하고, 새 app 의 rep 좌표가
  그 group 노드의 milestoneReportGen 에 의존하면 (reportGrandGen 등) → group
  멤버 수만큼 app 을 낸다 (rep 만 다르게). group 이 rep 에 무관하면 app 하나.
- **reduce/edge** (:345): §2.2 에서 group parent 는 이미 멤버별 분열 → 각 분열
  경로가 자기 app 을 정상 기록. 추가 작업 없음.
- **`reportGen`(tip 부착 gen)**: 같은 term action 은 같은 gen 에 부착하므로 멤버
  공통 — 문제 없음. **`milestoneReportGen`(replace milestone 의 m2 gen)만
  멤버별** (:242 = reportParentGen = 직전 tip 의 reportGen — 멤버가 직전에
  다른 노드였으면 다를 수 있음).

### 3.4 검증 — RecordConditionEvaluator / ActionApplication 상호작용

- `RecordConditionEvaluator` (조건 평가)는 **condition 만** 본다 (shape/group 무관).
  group 이 동일 condition 일 때만 형성되므로(§H5) evaluator 는 group 을 몰라도
  됨 — **evaluator 수정 불요**. 이게 §H5 "동일 condition 병합"의 큰 이점.
- **골든 불변 게이트**: `MG3_RECORD_COND_DIFF=1` (RecordConditionEvaluator.kt:5)
  + `Mgroup2VsMgroup3HistoryTest`(astWalkEquivalence :297) + 전 스위트가 **임의
  n 에서** kernelsHistory byte-exact 여야 한다. app 개수·좌표가 정확히 보존되면
  통과.

### 3.5 대안 (기록-시-확장이 부족할 경우)

만약 milestoneReportGen 이 group 멤버 간 자주 달라 app 복제가 과해지면(이득
잠식) → **소급 생성(retro)**: group 을 유지하되 보고 좌표는 group 노드에 멤버별
배열로만 보관, kernelsHistory 해석 시점에 (파스 종료 후) group 을 펼쳐 좌표 복원.
단 이건 kernelsHistory 를 group-aware 로 만들어 복잡도↑ — A3 측정 후 필요 시.

---

## 4. 단계별 구현 계획

각 단계는 **컴파일되고 전 게이트 그린**인 상태로 끝난다. 게이트 =
runMgroup3Test (139/0, 12 skip) + runMgroup3ParserTest (17/17) +
runMgroup3HistoryDiffTest (m2 parity, `MG3_RECORD_COND_DIFF=1`) + cargo
parser_diff golden byte-identical. **Rust 무변경이므로 cargo 는 fixture 재생성
없이 그대로 그린이어야 정상** (출력 불변).

### A0 — 표현 도입 + n=1 비트동일 (난이도: 중)

- `MilestonePath` 에 `groupMembers`/`groupMemberReportGens` 옵션 필드 추가
  (§1.1 (B)), equals/hash 에 group 반영 (§1.4), 런타임 파라미터 `n` 을
  `Mgroup3Parser` 에 도입 (기본 1).
- **병합/분열 로직은 아직 없음** — n 무관하게 groupMembers 항상 null.
- **검증**: 전 스위트 그린 + **n=1 비트동일** (groupMembers=null 경로가 현행과
  동일함을 전 스위트로). 새 필드가 hot path 에 no-op 임을 phaseTiming 으로 확인.
- 검증방법: 전 게이트. equality 계약 회귀 없음 확인 (PathMap dedup 크기 불변).

### A1 — 생성 지점 병합 (mergeInteriorGroups) + 보고 확장 (난이도: 상)

- §2.1 병합 패스 (step 5 뒤, main root, condition+observing 키) 구현.
- §3.2 기록-시-확장 (term descend app 멤버별 rep) 구현.
- **아직 window-exit/reduce 분열 없음** → group 은 형성되자마자 다음 gen 에
  descend/reduce 를 만나면 **즉시 전 멤버 분열** (보수적 — group 이 한 gen 만
  삶). 이 단계 목표는 "group 을 만들고 즉시 풀어도 출력 불변".
- **검증**: 전 게이트 그린 (n∈{1,2,4,6} 전부). **A1 은 성능 이득이 거의 없다**
  (group 이 1 gen 만 삶) — correctness 검증 단계. **여기서 §H5 실현율(condition
  이 병합을 얼마나 깎는지)을 mean-shape 카운터로 처음 측정** (§5).
- 난이도 상: 보고 확장이 출력 불변을 지키는지가 핵심. `MG3_RECORD_COND_DIFF=1`
  + astWalkEquivalence 로 촘촘히.

### A2 — reduce 시 group 유지/분열 (난이도: 상)

- §2.2: reduce 가 group 노드까지 안 내려가면 group 유지, mid edge 에서 멤버별
  갈리면 그때 분열. → group 이 **여러 gen 을 산다** (첫 실질 이득).
- **검증**: 전 게이트 (임의 n). 여기서 mean-shape 감소가 처음 나타남 — Phase 0
  ceiling(§6.7) 대비 실현율 측정.

### A3 — window-exit 지연 분열 (난이도: 중)

- §2.3: 체인 성장으로 group 이 window(n) 밖으로 밀리면 descend 전 분열.
- **검증**: 전 게이트. n∈{4,6} 에서 Phase 0 depth 분포(§6.3)와 일치하는 분열
  빈도 확인.

### A4 — 측정 + 튜닝 (난이도: 중)

- §5 성능 측정. es5 heavies (jquery/json2 n=6) + chain_boundaries (n=4) 실측
  mean-shape 감소 → ceiling 실현율. **킬 기준(§6.7): es5 heavies n=6 실측
  <1.4× (실현율 <50%) 면 Phase B 진입 전 중단·분석.**
- 회귀 확인: jar.bbx/ccgen (mulang) 에서 n=6 도 정확성 그린 + 시간 회귀 없음.

**총 작업량 추정**: A0 0.5일, A1 1.5~2일(보고 확장이 관건), A2 1.5일, A3 1일,
A4 0.5~1일. **합 5~6일** (프로토타입 — Rust 미러·precompute 는 Phase B 별도).
불확실성의 대부분은 A1 의 보고 불변(§3)과 §H5 실현율(측정 전엔 미지).

---

## 5. 성능 측정 계획

### 5.1 Kotlin mean-shape 카운터 (파스 결과 무영향)

- `Mgroup3Parser` 에 opt-in 카운터 (phaseTiming :20 패턴 재사용): 매 gen 후
  `ctx.paths[mainRoot].size`(병합 후 shape 수) 와 "가상 base"(병합 안 했을 때
  멤버 총수 — group.members 합)를 누적. mean = Σshape/gens, ratio = base/merged.
- **Phase 0 probe(interior_merge.rs) 와 같은 mean 정의** (§6.2: mean 이 시간
  프록시, corr 0.997) → ceiling(§6.7)과 직접 비교 가능.
- 파스 출력·kernelsHistory 에 무영향 (카운터만) — 게이트 그린 유지.

### 5.2 es5 parserdata 준비 (Phase 0 재현)

REPRO.md 대로:
- **es5-mg3.pb 는 이미 있음**: `scratchpad/mgroup4-phase0/es5-mg3.pb` (106MB).
  재생성 시 REPRO.md §Parserdata (genCliJar + es5/grammar.cdg, Stage3 crash 무관
  — Stage1 이 pb 완성).
- **코퍼스**: `es5-corpus/` (jquery-1.12.4.js, json2.js, underscore-1.8.3.js) —
  이미 있음.
- **Kotlin 측정 실행**: es5-mg3.pb 를 `Mgroup3Parser` 로 로드 (Es5CdgTest.kt
  패턴) → 각 코퍼스 n∈{1,4,6} 파스 → 5.1 카운터.

### 5.3 대상과 기대

| 대상 | n | Phase 0 ceiling (mean, §6.7) | 실현율 게이트 |
|---|---|---|---|
| jquery.js | 6 | 1.96× | >50% (kill: <1.4×) |
| json2.js | 6 | 1.85× | >50% |
| chain_boundaries.mu (main) | 4 | 1.58× (ALL) / 1.80× (main) | — |
| jar.bbx / ccgen.mu | 6 | 회귀 확인용 (1.29~1.39×) | 회귀 없음 + 정확성 |

- **es5 소입력 실측 (이 문서 작성 중 probe 재확인)**: `var x=a+b*c+...` 67자에서
  main mean r@n4 2.15× / peak 70→14. depth-2 80.7%, watcher 이득 0 (mean 1.5) —
  §6.7 "이득 전체가 main" 재확인.
- **A1 에서 condition-깎임 측정**: 5.1 카운터의 병합률을 Phase 0 shape-only
  ceiling 과 비교 → 그 격차가 §H5 condition 제약의 실현율 손실.

### 5.4 회귀 확인용

- jar.bbx (bibix4 stdlib, 최악), ccgen.mu — mulang fixture pb
  (`mgroup3-native/tests/fixtures/parser_generated/mulang/data.pb`, 이미 신선).
  n=6 에서 정확성 그린 + 파스 시간 비회귀(group 유지 오버헤드가 이득 잠식 안 함).

---

## 6. 리스크와 미결정

### R1 — condition 이 실현율을 깎음 (§H5, 최대 미지수)

Phase 0 ceiling 은 shape-only (condition 무시). **동일 condition 병합 정책은
condition 이 멤버별로 다른 fork 를 병합에서 배제** → 실현율 < ceiling.
- **완화**: A1 에서 즉시 측정 (5.3). 격차가 크면 (예: 실현율 <50%) →
  "condition 도 group 에 멤버별 보관 + evaluate 를 group-aware 로" 대안 검토
  (복잡도 큼 — Phase B 후보). 작으면 (조건이 대개 형제 간 동일) 정책 유지.
- **왜 작을 수 있나**: depth-2 fork(지배적, 80.7%)는 "직전에 갈라진 노드"가
  상이 노드 — 그 노드의 condition 이 tip 쪽에 이미 흡수/동일화됐을 가능성 (같은
  term action 을 탄 형제이므로). A1 측정으로만 확정.

### R2 — 분열 시 보고 소급 (§3, 구현 난관)

기록-시-확장(§3.2)이 milestoneReportGen 멤버 차이로 app 을 과복제하면 이득 잠식.
- **완화**: A1 에서 app 복제율 측정. 과하면 §3.5 소급 생성(kernelsHistory
  group-aware)으로 전환. 단 그건 복잡도↑ — 프로토타입은 기록-시-확장 우선.
- **최악**: milestoneReportGen 이 거의 항상 멤버별로 다르면 → app 이 멤버별로
  다 나와 파스는 접혀도 보고가 안 접힘. 이 경우도 **시간 이득은 파스 shape
  감소로 여전히 존재** (app 기록은 shape 순회 아님) — 이득이 0 은 아님.

### R3 — group 유지 오버헤드

group 파티션(§2.1 매 gen 재파티션)·멤버 순회(reduce/window-exit)가 병합 이득을
먹을 수 있다. Phase 0 §6.6 "실측 기대 ~1.15~1.25×"는 이 오버헤드 반영 추정.
- **완화**: 프로토타입은 정확성 우선(매 gen 재파티션). A2 후 증분화(직전 group
  재사용). 5.4 회귀 확인으로 오버헤드가 이득을 역전 안 하는지 감시.

### R4 — equality/OOM 지뢰 (절대 제약 5)

group 을 equals 에 잘못 반영하면 (예: 보고 좌표 포함) mulang 경로 폭발 OOM
(현행이 reportGen 제외로 회피한 바로 그 함정, ParsingCtx.kt:172-174).
- **완화**: §1.4 — groupMemberReportGens 를 equals/hash 에서 제외. A0 에서 n=1
  비트동일 + PathMap dedup 크기 불변으로 회귀 감시.

### R5 — 미결정 목록

- **매 gen 재파티션 vs 증분 병합**: 프로토타입 전자, A2 후 후자 검토.
- **group 표현 (B) 의 대표 멤버 `milestone`**: reduce 에서 group parent 를 멤버
  순회로 풀 때 대표 멤버로 무엇을 쓸지 — 정렬 첫 멤버. 좌표 계산이 대표에
  의존하면 안 됨 (전 멤버 순회 필수) — A2 에서 확인.
- **워처 경로**: Phase A 는 main 만 (제약 3). 워처 일반화는 mulang 이득의 관건
  이나 B 후보 (§6.7).
- **n 기본값**: 프로토타입은 측정용으로 노출만, 기본 결정은 A4 측정 후.

### R6 — fork 가 생성 지점에서 안 잡히는 발견 (§H2 재해석)

**중요 발견**: 상위 세션 추정 H2("생성 지점 pre-group 으로 공짜")는 **부분
기각**. 병합은 생성 지점 한 곳이 아니라 "여러 gen 걸쳐 나란히 자란 형제"의
per-gen 재집계 (§2.1). 이는 Phase 0 probe 가 이미 그렇게 쟀고(merge_greedy),
알고리즘이 그 연산을 런타임 자료구조로 재현하면 됨 — **설계상 해결됨** (생성
지점 최적화는 증분화로 미룸). 이 재해석이 A1 을 "생성 지점 훅"이 아니라 "step 5
뒤 병합 패스"로 배치한 이유다.

---

## 7. Phase A 실행 결과 (2026-07-10 완주 — 사후 기록)

A0~A4 전 단계 완료. 구현은 사용자 지시로 **별도 모듈 `mgroup4/`** (mgroup3 소스
무변경 — 타 프로젝트 참조 보호). 파서 런타임 6파일 포크 (`Mgroup4Parser` 등,
AcceptCondition↔PathRoot 결합 클러스터 포함), proto/generator/parserdata 는
mgroup3 공유. 게이트는 차등 오라클로 강화: **G1 = mgroup4(n=1) ≡ mgroup3
kernelsHistory byte-identical, G2 = mgroup4(n∈{2,4,6}) ≡ mgroup4(n=1)** —
구조 문법 12종 + asdl 4입력 + chain_boundaries.mu + es5 json2.js, 전부 그린
(`runMgroup4DiffTest`). mgroup3 스위트 139/0 + 17/17, fixture 오염 없음.

### 7.1 실측 결과 (Mgroup4Bench — JVM 분리, 8g, 웜업 후 중앙값)

| 파일 | n | 파스 시간 | n=1 대비 | realized shape ratio |
|---|--:|--:|--:|--:|
| json2.js | 1→6 | 2401→1143ms | **2.10×** | 1.743 (조정 ceiling 의 97%) |
| jquery-1.12.4.js | 1→6 | 40.3s→18.1s | **2.23×** | 1.776 |
| ccgen.mu (회귀) | 1→6 | 5114→2748ms | 1.86× | 1.238 |
| chain_boundaries.mu | 1→4 | 233→211ms | 1.10× | 1.444 |

- **§6.7 킬 기준 (es5 heavies n=6 시간 ≥1.25×) 큰 폭 통과.** n=1 은 mgroup3 대비
  회귀 없음 (json2 2.5% 이내). 시간 이득이 shape ratio 를 **초과** — 큰 live-set
  의 메모리 트래픽/GC 압력이 shape 감소로 복리 완화 (입력 클수록 격차 확대).
- 시간 분해: 지배 비용은 step6_prune (파스의 45-65%, 파서 본체) — n 증가의 이득
  원천. 병합 패스 자기시간은 최적화 후 3.9~14%.

### 7.2 단계별 핵심 발견

- **A1**: per-gen 재파티션이 조정 ceiling 에 구성적으로 도달 (json2 n6 realized
  1.803 vs 조정 ceiling main 1.80 — Rust probe 와 독립 교차검증 일치). **H5 확정:
  es5 는 condition 병합 거부 0건** (R1 해소), mulang 은 condition 이 주 차단
  요인이나 실현율 ~95%. 발견 버그 1건: MilestonePath.equals 의 parent 재귀가
  tip-side 노드 비교에 diff 를 끌어들여 d≥3 병합 무산 → node-local 비교로 수정.
- **A2+A3**: group 다중 gen 수명 + reduce 분열 + window-exit. 보고 확장 (§3.2)
  은 실측상 no-op (fold 시점 보고 좌표가 멤버 불변 — rejectReportCoordDiff=0).
  §3.5 소급 해석 불요. **병합 기원: creation-mergeable 9.5% vs late-convergence
  90.5%** (json2 n6) — parserdata 사전 그룹핑만으론 병합의 ~10% 만 잡힘, **런타임
  packing 이 본질적 구성요소** (Phase B 설계의 결정 데이터).
- **A4**: "n=6 이 1.32× 느림" 신호는 단일-JVM interleave 측정 편향으로 판명 (JVM
  분리 측정에서 반전). 병합 패스 귀속: 전-체인 재해싱 60% + chainToList 할당 24%
  → **prefix/suffix 누적 해시 메모이즈** (immutable 체인의 gen 간 공유 이용,
  interior_merge.rs pre/suf 패턴 역이식) + 지연 물질화로 자기시간 390→183ms.
  증분 버킷 유지는 불요 판정 (복잡도 대비 이득 없음).

### 7.3 Phase B 시사점

1. **아키텍처**: 런타임 packing (재파티션 + 캐시 해시) 이 주 기제, parserdata
   그룹 테이블 (P4) 은 reduce 분열/유지 판정 가속용 보조. "전이 사전열거로 병합
   대체" 그림은 late-convergence 90% 데이터로 기각.
2. **Rust 이식**: 메모이즈 패턴은 probe 와 동형이라 이식 용이 (OnceCell 캐시 또는
   pre/suf 벡터). GC 부재로 시간 이득은 compute-순 (≈shape ratio 1.7×+) 에 수렴
   전망 — JVM 의 GC 복리 이득은 빠지지만 heavy 에서 1.7×+ 유지 예상.
3. **잔여 스코프**: 워처 경로 일반화 (mulang 이득의 관건 — Phase A 는 main 만),
   2중 group 체인 (skipExistingGroup=62,808/json2 n6 — 남은 병합 여지), n 기본값
   (es5 류 6~8, mulang 류 4).

## 8. 요약 — 핵심 설계 결정

1. **표현**: `MilestonePath.milestone` 유지 + 옵션 `groupMembers`(§1.1 (B)),
   보고 좌표 멤버별 배열은 equals 제외 (§1.4). n=1 → groupMembers=null 비트동일.
2. **병합**: step 5 evolve 뒤 main root 만, per-gen 재파티션, 키 = window-밖
   노드 + condition + observing (§2.1, §H5). Phase 0 merge_greedy 의 런타임판.
3. **분열**: reduce mid-edge 멤버 상이(§2.2), window-exit 체인 성장(§2.3).
4. **보고 불변**: 파스는 group, app 기록은 멤버별 확장 (§3.2) — condition 동일
   병합이라 RecordConditionEvaluator 무수정. 골든 재생성 불요.
5. **단계**: A0(표현+n=1) → A1(병합+보고, correctness) → A2(reduce 분열,
   첫 이득) → A3(window-exit) → A4(측정). 총 5~6일.
6. **핵심 미지수**: §H5 condition 이 실현율을 얼마나 깎는지 (A1 측정) 와 §3 보고
   확장 비용 (R2). 킬 기준 = es5 n=6 <1.4×.
