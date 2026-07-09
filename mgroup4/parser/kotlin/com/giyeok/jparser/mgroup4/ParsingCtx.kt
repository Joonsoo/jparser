package com.giyeok.jparser.mgroup4

// Path 의 "모양" — graph 상의 위치 (milestonePath, tipGroupId). acceptCondition 은 PathMap 의 value 에서 분리.
class PathShape(
  val milestonePath: MilestonePath?,
  val tipGroupId: Int,
) {
  private var _hashCode: Int = 0
  private var _hashCodeComputed: Boolean = false
  override fun hashCode(): Int {
    if (!_hashCodeComputed) {
      _hashCode = 31 * (milestonePath?.hashCode() ?: 0) + tipGroupId
      _hashCodeComputed = true
    }
    return _hashCode
  }
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PathShape) return false
    return tipGroupId == other.tipGroupId && milestonePath == other.milestonePath
  }
  override fun toString(): String = "PathShape(milestonePath=$milestonePath, tipGroupId=$tipGroupId)"
}

// PathShape → 그 shape 에 도달한 acceptCondition (여러 source 가 같은 shape 에 도달하면 Or 로 합쳐짐).
typealias PathMap = Map<PathShape, AcceptCondition>

data class ParsingCtx(
  val gen: Int,
  val line: Int,
  val col: Int,
  val mainRoot: PathRoot,
  // 모든 path — main path 와 cond path 가 같은 map 안에 있음.
  // mainRoot key 는 항상 존재 (main path); 그 외 key 는 살아있는 cond root.
  val paths: Map<PathRoot, PathMap>,
  // 매 step 마다 발생한 actions 를 저장 (parse tree 복원에 쓰일 수 있음).
  val history: List<HistoryEntry>,
  // history 의 모든 entry.activeCondPaths 의 누적 union. step 마다 새 active 만 추가하여 O(1) amortized.
  val everSeenCondRoots: MutableSet<PathRoot> = mutableSetOf(),
  // 보고 전용 root anchor — same-input 적용으로 시작한 cond root 는 실제 span 이
  // (생성 gen - 1) 부터이므로 (m2 의 pended root 는 생성 gen 부터 span), 보고 좌표의
  // 기준 gen 을 따로 둔다. 키가 없으면 root.startGen. 런타임(조건 anchoring)은 불변.
  val rootReportGens: MutableMap<PathRoot, Int> = mutableMapOf(),
) {
  // 편의 view — main path 만, cond path 들만 (디버그/print 용).
  val mainPaths: PathMap get() = paths[mainRoot] ?: emptyMap()
  val condPaths: Map<PathRoot, PathMap> get() = paths.filterKeys { it != mainRoot }
}

data class HistoryEntry(
  // 이 step 에 적용된 parsing action 들 (템플릿 참조 + gen 바인딩) — kernels_history 가
  // lazy 하게 해석한다. mgroup2 의 genActions 대응: 파스 핫패스에서 record 물질화를
  // 피하기 위해 액션당 작은 튜플 하나만 기록 (동일 적용은 dedup).
  val actionApplications: List<ActionApplication> = emptyList(),
  // 액션 템플릿 밖에서 생기는 record 들 (root progress 의 finish / 보고용 root ptr0).
  val finishedKernels: List<FinishedKernelRecord> = emptyList(),
  val addedKernels: List<AddedKernelRecord> = emptyList(),
  // 이 step 에서 finish 된 cond path 들의 root → finish accept condition (Or 로 묶임).
  // end gen = 이 entry 의 gen (eager — replaceAndProgress 류).
  val condPathFinishes: Map<PathRoot, AcceptCondition> = emptyMap(),
  // 이 step 에서 죽은 cond path 의 possible-finish — end gen 이 직전 gen (entry gen - 1).
  // bounded (Unless/OnlyIf) 와 longest (NoLongerMatch) 의 정확한 span discharge 를 위해
  // eager 채널과 분리 기록한다.
  val lateCondPathFinishes: Map<PathRoot, AcceptCondition> = emptyMap(),
  // 이 step 후 살아남은 cond path 들의 root.
  val activeCondPaths: Set<PathRoot> = emptySet(),
  // 이 step 에서 main root (start symbol) 가 progress(=전체 입력 매치 완료) 한 경우의
  // accept condition (Or 로 묶임). isAccepted 는 이것만 평가한다 —
  // finishedKernels 는 보고(kernelsHistory) 전용 채널.
  val mainRootFinish: AcceptCondition? = null,
  // 보고 대상 cond root 들 — mgroup2 의 trackings 와 같은 규칙 (조건이 참조하는 root +
  // observing cond symbol 의 parent-gen anchor). 런타임 생존 규칙(step 6 의 referencedRoots,
  // tip-gen anchor 포함)보다 좁다. record 는 저장 시점에 이 집합(이번/직전 entry)으로 필터됨.
  val reportedCondRoots: Set<PathRoot> = emptySet(),
)

// 한 parsing action 의 적용 — 템플릿 참조와 gen 바인딩만 저장 (보고 전용).
// rt*: 런타임 바인딩 (조건 resolve 용 — cond root anchoring 과 일치해야 함).
// rep*: 보고 좌표 바인딩 (m2 의 genMap 대응 — kernel begin/end 해석용).
// next 는 양쪽 공통 (이번 step 의 gen).
// condition: 이 적용을 구동한 runtime 조건 — edge action 은 진행을 일으킨 combined
// (mgroup2 kernelsHistory 가 edge summary 전체를 progressedKgroups/Kernels 의
// 조건으로 게이팅하는 것에 대응). term action 은 m2 와 같이 게이트 없음 (Always).
data class ActionApplication(
  val actions: ParsingActionsPlain,
  val root: PathRoot,
  val rtCurr: Int,
  val rtMid: Int,
  val next: Int,
  val rtGrand: Int,
  val repCurr: Int,
  val repMid: Int,
  val repGrand: Int,
  val condition: AcceptCondition = Always,
)

data class FinishedKernelRecord(
  val kernel: Kernel,
  val condition: AcceptCondition,
  // 이 record 를 만든 action 이 속한 path 의 root — 보고 필터용.
  val root: PathRoot,
)

// 보고 전용 kernel record — begin/end 가 모두 명시됨 (entry gen 과 무관할 수 있음).
data class AddedKernelRecord(
  val symbolId: Int,
  val pointer: Int,
  val beginGen: Int,
  val endGen: Int,
  val condition: AcceptCondition,
  val root: PathRoot,
)

// PathRoot 를 packed Long inline value class 로 — hot path 에 매 leaf condition / map key / set element 마다
// 등장. data class 의 매번 hashCode 계산 + 새 instance allocation 회피. equals/hashCode 는 Long 의 그것 — fast.
@JvmInline
value class PathRoot private constructor(val packed: Long) {
  val symbolId: Int get() = (packed shr 32).toInt()
  val startGen: Int get() = packed.toInt()
  override fun toString(): String = "PathRoot(symbolId=$symbolId, startGen=$startGen)"

  companion object {
    operator fun invoke(symbolId: Int, startGen: Int): PathRoot =
      PathRoot((symbolId.toLong() shl 32) or (startGen.toLong() and 0xFFFFFFFFL))
  }
}

// MilestonePath: linked list 형태로 graph 의 path 를 나타냄.
// (start -> a -> b -> X) 의 경로는 MilestonePath(b, MilestonePath(a, MilestonePath(start, null))) 와 같이 표현.
// 한 인스턴스가 곧 한 엣지에 해당.
// observingCondSymbolIds 는 이 엣지에 포함된 그래프가 추후 추적해야 할 cond symbol root 들.
//
// 변경 사항: 더 이상 data class 가 아님. hashCode 를 lazy 캐싱 (immutable 이므로 안전).
// 이유: data class hashCode 는 매 호출마다 deep 계산 → PathMap 으로 자료구조를 바꾸면서 hash 비용이 dominate 할 위험.
class MilestonePath(
  val gen: Int,
  val milestone: Kernel,
  val parent: MilestonePath?,
  // proto list 그대로 reference — `toSet()` 으로 매 path 생성마다 새 HashSet 만드는 걸 회피.
  // 단순히 iterate 만 하고 (lookup 안 함) 같은 proto 의 같은 ordering 가 같음 비교에서 안전.
  val observingCondSymbolIds: List<Int>,
  // === 보고(kernels_history) 전용 shadow gen — 런타임 바인딩/조건 resolve 에는 사용 금지 ===
  // reportGen: 이 노드 위의 tip group 이 마지막으로 (재)부착된 gen.
  //   mgroup2 의 tip MilestoneGroupKt.gen 대응 — edge action append 마다 현재 gen 으로 갱신.
  //   (런타임 gen 은 처음 부착 gen 에 고정 — 조건 anchoring 과 한 몸이므로.)
  // milestoneReportGen: 이 노드의 milestone kernel 의 mgroup2 식 gen.
  //   descend(replaceAndAppend) 시점에 직전 tip 의 reportGen 을 물려받는다
  //   (m2 의 replace milestone 이 tip.gen 을 유지하는 것에 대응).
  val reportGen: Int,
  val milestoneReportGen: Int,
  // === mgroup4 interior milestone group (§1.1 (B)) ===
  // null = singleton 노드 (현행). non-null = 이 노드가 group — `milestone` 은 대표 멤버
  // (정렬 첫), groupMembers 는 정렬된 전 멤버 (canonical: compareBy(symbolId, pointer, gen)).
  // 멤버는 gen(런타임 anchor)/observingCondSymbolIds 가 동일해야 병합 가능 (상위 수정 지시 1).
  val groupMembers: List<Kernel>? = null,
  // 멤버별 milestoneReportGen — groupMembers 와 병렬 (같은 index). singleton 이면 null.
  // equals/hashCode 에서 제외 (reportGen 제외 규칙의 연장 — §1.4/R4).
  // ★ A2 지뢰: 멤버 shape 를 원본 배열로 캐시하던 A1 crutch (groupMemberShapes) 는
  // 제거됐다. A2 에서 group 이 여러 gen 을 살며 tip-side 로 체인이 자라므로, 원본
  // shape 를 통째로 되돌리는 방식은 성립하지 않는다 (group 위에 새 노드가 쌓임).
  // 대신 분열은 group 노드 자리의 (milestone[i], milestoneReportGen[i]) 로 멤버
  // singleton 을 만들고, tip-side 노드는 parent 만 바꿔 복사해 재구성한다
  // (explodeShapeReconstruct / memberSingletonsForEdge). 이 재구성이 byte-exact
  // 이려면 fold 시점에 window 노드들의 보고 좌표가 멤버 간 동일해야 한다 (병합 거부
  // 카운터 rejectReportCoordDiff 로 강제 — Mgroup4Parser.mergeVerdictAtDepth).
  val groupMemberReportGens: IntArray? = null,
) {
  private var _hashCode: Int = 0
  private var _hashCodeComputed: Boolean = false

  // === A4 병합 파티션 가속 캐시 (Mgroup4Parser.mergeInteriorGroups 전용) ===
  // node-local hash 와 root→this 누적(prefix) hash 를 노드에 lazy 캐시. MilestonePath 는
  // immutable 이고 체인이 gen 간 공유되므로 (tip 만 term descend 로 자람), 한 번 계산한
  // 값이 이후 gen 에서도 유효 → 재파티션의 per-shape 버킷 해시 비용을 O(체인전체) →
  // O(window) 로 낮춘다 (Rust interior_merge.rs 의 pre/suf 누적 해시 패턴의 Kotlin판).
  //
  // ★ 함정/무효화 계약: 이 두 해시는 **node-local identity** (gen, group/milestone,
  // observingCondSymbolIds) 만 접는다 — reportGen/milestoneReportGen 은 제외 (equals/
  // nodeLocalEquals 계약과 정확히 일치; 포함하면 병합 판정이 보고 좌표로 갈려 틀린다).
  // 노드가 immutable 이라 무효화는 필요 없다. groupMembers 는 fold 시점에 확정돼 이후
  // 안 바뀌므로 group 노드에 대해서도 안전. prefixHash 는 parent 의 prefixHash 에만
  // 의존 → parent 가 공유되면 그 캐시도 공유돼 누적 계산이 O(1) amortized.
  private var _nodeLocalHash: Int = 0
  private var _nodeLocalHashComputed: Boolean = false
  private var _prefixHash: Int = 0
  private var _prefixHashComputed: Boolean = false
  // root..this 의 노드 수 (this 포함) — chainToList 없이 length 를 O(1) amortized 로.
  private var _chainDepth: Int = 0

  // root 부터 이 노드까지의 노드 수 (this 포함). parent 캐시 재사용 (immutable).
  fun chainDepthCached(): Int {
    if (_chainDepth == 0) {
      _chainDepth = (parent?.chainDepthCached() ?: 0) + 1
    }
    return _chainDepth
  }

  // node-local(비재귀) hash — nodeLocalEquals 계약과 정합 (gen·milestone/group·observing).
  fun nodeLocalHashCached(): Int {
    if (!_nodeLocalHashComputed) {
      var h = gen
      h = 31 * h + (groupMembers?.hashCode() ?: milestone.hashCode())
      h = 31 * h + observingCondSymbolIds.hashCode()
      _nodeLocalHash = h
      _nodeLocalHashComputed = true
    }
    return _nodeLocalHash
  }

  // root→this 누적 rolling hash: prefixHash = 31*parent.prefixHash + nodeLocalHash(this).
  // "root..(이 노드 포함)" 구간의 node-local hash 를 순서대로 접은 값 — 버킷 키의 prefix
  // 성분을 O(1) 로 준다 (parent 캐시 재사용). seed=1 은 mergeInteriorGroups 의 h=1L 과 정렬.
  fun prefixHashCached(): Int {
    if (!_prefixHashComputed) {
      val parentPrefix = parent?.prefixHashCached() ?: 1
      _prefixHash = 31 * parentPrefix + nodeLocalHashCached()
      _prefixHashComputed = true
    }
    return _prefixHash
  }

  override fun hashCode(): Int {
    if (!_hashCodeComputed) {
      var h = gen
      // group 이면 대표 milestone 대신 멤버 배열 해시 (정렬돼 있어 order 안정).
      // singleton 은 현행 (milestone.hashCode) 과 비트동일.
      h = 31 * h + (groupMembers?.hashCode() ?: milestone.hashCode())
      h = 31 * h + (parent?.hashCode() ?: 0)
      h = 31 * h + observingCondSymbolIds.hashCode()
      _hashCode = h
      _hashCodeComputed = true
    }
    return _hashCode
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MilestonePath) return false
    if (gen != other.gen) return false
    // group 여부가 다르면 다른 shape (한쪽 null, 한쪽 non-null 은 milestone 만 비교하면
    // 대표 멤버가 같을 때 오병합). group 이면 멤버 내용 비교 (정렬됨), singleton 은 milestone.
    if (groupMembers == null) {
      if (other.groupMembers != null) return false
      if (milestone != other.milestone) return false
    } else {
      if (other.groupMembers == null) return false
      if (groupMembers != other.groupMembers) return false
    }
    // report gen 들은 identity 에서 제외 — 포함하면 (m2 는 구분하는) 보고 좌표 변형마다
    // path 가 분리되어 모호한 문법에서 경로 폭발 (mulang 에서 OOM 실측).
    // 같은 shape 로 병합되면 먼저 도착한 인스턴스의 보고 좌표가 유지된다 (근사).
    // groupMemberReportGens 도 같은 이유로 제외 (§1.4).
    if (observingCondSymbolIds != other.observingCondSymbolIds) return false
    // parent 비교는 가장 무거우므로 마지막. immutable + sharing 으로 reference equality 가 자주 성립.
    return parent == other.parent
  }

  fun copy(
    gen: Int = this.gen,
    milestone: Kernel = this.milestone,
    parent: MilestonePath? = this.parent,
    observingCondSymbolIds: List<Int> = this.observingCondSymbolIds,
    reportGen: Int = this.reportGen,
    milestoneReportGen: Int = this.milestoneReportGen,
    groupMembers: List<Kernel>? = this.groupMembers,
    groupMemberReportGens: IntArray? = this.groupMemberReportGens,
  ): MilestonePath = MilestonePath(
    gen, milestone, parent, observingCondSymbolIds, reportGen, milestoneReportGen,
    groupMembers, groupMemberReportGens,
  )

  val isGroup: Boolean get() = groupMembers != null

  override fun toString(): String =
    "MilestonePath(gen=$gen, milestone=$milestone, parent=$parent, observingCondSymbolIds=$observingCondSymbolIds, " +
      "reportGen=$reportGen, milestoneReportGen=$milestoneReportGen" +
      (if (groupMembers != null) ", groupMembers=$groupMembers" else "") + ")"
}

data class Kernel(val symbolId: Int, val pointer: Int, val gen: Int) {
  val kernelTemplate: KernelTemplatePair get() = KernelTemplatePair(symbolId, pointer)
}

data class KernelTemplatePair(val symbolId: Int, val pointer: Int)

// PathMap 에 path 를 추가/병합하는 helper. 이미 같은 shape 가 있으면 acceptCondition 을 Or 로 결합.
fun MutableMap<PathShape, AcceptCondition>.addPath(shape: PathShape, cond: AcceptCondition) {
  if (cond == Never) return
  val existing = this[shape]
  this[shape] = if (existing == null) cond else Or.from(existing, cond)
}

// 디버그/테스트용 — PathShape + AcceptCondition 의 (튜플 이라기엔 좀 더 의미있는) view.
// 기존 코드의 path.tipGroupId / path.acceptCondition / path.milestonePath 접근 호환.
val Map.Entry<PathShape, AcceptCondition>.tipGroupId: Int get() = key.tipGroupId
val Map.Entry<PathShape, AcceptCondition>.milestonePath: MilestonePath? get() = key.milestonePath
val Map.Entry<PathShape, AcceptCondition>.acceptCondition: AcceptCondition get() = value
