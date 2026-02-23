# mgroup2 vs mgroup3 분석

## 개요

| 항목 | mgroup2 | mgroup3 |
|------|---------|---------|
| **언어** | Scala | Kotlin |
| **파서 코드량** | 468줄 | 295줄 |
| **상태** | Production/안정 | 개발 중 (TODO 다수 존재) |
| **NaiveParser 의존** | 있음 (코드 재활용) | 없음 (완전 새로 작성) |

---

## 핵심 차이점

### 1. Conditional Symbol 처리 방식 (가장 큰 차이)

**mgroup2: GenActions 기반 간접 추적**

- 모든 경로(main + conditional)를 하나의 `paths: List[MilestoneGroupPath]`로 관리
- conditional symbol의 진행 여부를 판단할 때 `GenActions`에서 `progressedRootMilestones`, `progressedKgroups` 등을 검색해야 함
- `getProgressConditionOf()` 함수가 GenActions를 탐색해서 해당 milestone이 progress 되었는지 확인하는 복잡한 로직 수행
- `collectTrackings()`로 어떤 경로가 아직 필요한지 계산해서 불필요한 경로 제거

```scala
// mgroup2: GenActions에서 milestone 진행 조건 추출
def getProgressConditionOf(genActions: GenActions, milestone: Milestone): MilestoneAcceptCondition = {
  val groups = genActions.progressedRootMgroups
    .filter { pair => pair._1.gen == milestone.gen }
    .filter { pair => parserData.milestoneGroups(pair._1.groupId).contains(milestone.kernelTemplate) }
    .values.toSet
  val progressCondition = genActions.progressedRootMilestones.getOrElse(milestone, Never)
  MilestoneAcceptCondition.disjunct(groups + progressCondition)
}
```

**mgroup3: mainPaths / condPaths 명시적 분리**

- `mainPaths`와 `condPaths`를 `ParsingCtx`에서 명시적으로 분리
- conditional symbol이 finish 되었는지 직접 `condPaths`에서 확인 가능
- GenActions 탐색 과정이 불필요 → 코드 복잡도 대폭 감소

```kotlin
// mgroup3: 명시적으로 분리된 구조
data class ParsingCtx(
  val mainPaths: List<ParsingPath>,       // 메인 파싱 경로
  val condPaths: Map<PathRoot, List<ParsingPath>>,  // 조건 심볼별 별도 경로
)
```

### 2. AcceptCondition 타입 시스템

**mgroup2:**
- `MilestoneAcceptCondition` (milestone2 패키지에서 import)
- `Exists(symbolId, gen, checkFromNextGen)` — `checkFromNextGen` boolean 플래그로 lookahead 시점 제어
- milestone 기반: condition이 `Milestone` 객체를 직접 참조

**mgroup3:**
- 독립적인 `sealed class AcceptCondition` 계층 구조
- Longest 전용: `NoLongerMatch` / `NeedLongerMatch` (symbolId, startGen, endGen)
- Lookahead 전용: `NotExists` / `Exists` (symbolId, startGen)
- Except/Join 전용: `Unless` / `OnlyIf` (symbolId, startGen)
- `checkFromNextGen` 같은 상태 플래그 없이, endGen으로 시점을 명시적으로 표현

### 3. evolveAcceptCondition 로직 비교

**mgroup2:**
```scala
case condition: Exists =>
  val milestone = condition.milestone
  val moreTrackingNeeded = paths.exists(_.first == milestone)  // 전체 paths 탐색
  val progressCondition = getProgressConditionOf(genActions, milestone)  // GenActions 탐색
  val evolvedCondition = evolveAcceptCondition(paths, genActions, progressCondition)
  if (moreTrackingNeeded) {
    MilestoneAcceptCondition.disjunct(Set(condition, evolvedCondition))
  } else {
    evolvedCondition
  }
```

**mgroup3:**
```kotlin
is Exists -> {
  val root = PathRoot(cond.symbolId, cond.startGen)
  condPathFins[root]?.let {           // condPaths에서 직접 조회
    evolveAcceptCondition(it, condPathFins, activeCondPaths, gen)
  } ?: (if (root in activeCondPaths) cond else Never)  // 살아있는지 직접 확인
}
```

mgroup3는 `condPathFins`(해당 gen에서 finish된 조건 경로들)와 `activeCondPaths`(아직 살아있는 조건 경로들)를 직접 참조하므로 GenActions를 탐색할 필요가 없다.

### 4. 경로(Path) 데이터 구조

**mgroup2: `MilestoneGroupPath`**
```scala
case class MilestoneGroupPath(
  first: Milestone,              // 시작 마일스톤
  path: List[Milestone],         // 경로 히스토리 (역순 리스트)
  tip: MilestoneGroup,           // 현재 tip
  acceptCondition: MilestoneAcceptCondition
)
```
- `path`가 `List[Milestone]`이므로 pop 연산 시 새 리스트 생성
- `first`가 별도 필드 → conditional path도 main path와 동일한 구조

**mgroup3: `ParsingPath` + `MilestonePath` (linked list)**
```kotlin
data class ParsingPath(
  val milestonePath: MilestonePath?,  // null이면 starter path
  val tipGroupId: Int,
  val acceptCondition: AcceptCondition
)

data class MilestonePath(
  val gen: Int,
  val milestone: Kernel,
  val parent: MilestonePath?,          // linked list
  val observingCondSymbolIds: Set<Int>, // 이 엣지에서 관찰할 조건 심볼들
)
```
- Linked list 구조로 메모리 공유 가능
- `observingCondSymbolIds`를 각 엣지(MilestonePath)에 저장 → 필요한 conditional symbol을 엣지 단위로 추적
- mgroup2의 `collectTrackings()` 같은 별도 함수가 불필요해질 수 있음

### 5. Pended Accept Condition Kernels

**mgroup2에만 존재:**
- `pendedAcceptConditionKernels`라는 개념이 있어서, term action 적용 시 pended condition을 수집하고 병합하는 복잡한 로직이 존재 (MilestoneGroupParser.scala:210-251)
- mgroup3에서는 condPaths를 별도로 유지하므로 이 복잡한 병합 로직이 필요 없음

### 6. History / 파스 트리 복원

**mgroup2:**
- `HistoryEntry(untrimmedPaths, genActions)`를 매 gen마다 기록
- `kernelsHistory()` 함수(324-467줄)로 파스 트리 복원 가능
- GenActions의 각 action에 포함된 `TasksSummary2`를 활용

**mgroup3:**
- `ParsingCtx`에 `// TODO history` 주석만 존재
- 아직 history/파스 트리 복원 미구현

---

## mgroup3 현재 상태

### 구현 완료된 부분
- **파서 코어** (`Mgroup3Parser.kt`): 기본 파싱 루프, term action 적용, edge action 적용 로직 구현됨
- **AcceptCondition** (`AcceptCondition.kt`): 전체 타입 계층, `evaluateAcceptCondition`, `evolveAcceptCondition` 구현 완료
- **ParsingCtx / ParsingPath** (`ParsingCtx.kt`): 데이터 구조 정의 완료
- **파서 제너레이터** (`Mgroup3ParserGenerator.kt`): 핵심 생성 로직 존재
- **Proto 스키마** (`Mgroup3ParserData.proto`): 정의 완료
- **시각화 서버** (`Mgroup3VisualizeServer.kt`): 구현됨

### 미완성 / TODO 항목들

1. **`ParsingData.kt:15,22`** — `termSetFrom()`, `fromProto()` 함수가 `TODO()`로 남아있음
2. **`Mgroup3Parser.kt:118`** — condPath finish 처리 미구현:
   ```kotlin
   // TODO parsingActionsOut 중에 root에 대한 finish가 있는 경우,
   // finish condition들을 or로 묶어서 condPathFinishes에 넣어준다
   ```
3. **`Mgroup3Parser.kt:126`** — evolve된 accept condition 적용 후 Never인 path 제거 미구현:
   ```kotlin
   // TODO 새로운 path들에 대해 newAcceptConditions 적용해서 업데이트 - 하고 Never인 애들 날리기
   ```
4. **`Mgroup3Parser.kt:148,197`** — parsingActions에 gen 정보 부재
5. **`ParsingCtx.kt:9-10`** — condPaths 간 dependency 관리로 불필요한 path 정리하는 최적화 미구현
6. **`ParsingCtx.kt:12`** — history 미구현 (파스 트리 복원 불가)

### 치명적 미완성 사항

가장 중요한 미완성 부분은 **condPath finish 감지** (`Mgroup3Parser.kt:118`)이다. 이것이 구현되지 않으면 `condPathFinishes`가 항상 비어있게 되어 `evolveAcceptCondition`이 conditional symbol의 완료를 감지하지 못한다. 즉, **longest, lookahead, except, join 같은 conditional symbol이 있는 문법은 현재 제대로 파싱되지 않는다.**

---

## 설계 트레이드오프 요약

| 측면 | mgroup2 | mgroup3 |
|------|---------|---------|
| **추적 경로 수** | 적음 (하나의 리스트에서 관리) | 많아질 수 있음 (condPaths 별도 유지) |
| **코드 복잡도** | 높음 (GenActions 탐색, pended kernels 병합) | 낮음 (직접 참조) |
| **조건 진행 확인** | GenActions 간접 탐색 | condPaths 직접 조회 |
| **파스 트리 복원** | 구현됨 | 미구현 |
| **생태계 통합** | bibixPlugin, ktglue 등과 연동됨 | 독립적, 아직 통합 없음 |
