package com.giyeok.jparser.mgroup3

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
) {
  // 편의 view — main path 만, cond path 들만 (디버그/print 용).
  val mainPaths: PathMap get() = paths[mainRoot] ?: emptyMap()
  val condPaths: Map<PathRoot, PathMap> get() = paths.filterKeys { it != mainRoot }
}

data class HistoryEntry(
  val finishedKernels: List<FinishedKernelRecord>,
  val progressedKernels: List<ProgressedKernelRecord>,
  // 이 step 에서 finish 된 cond path 들의 root → finish accept condition (Or 로 묶임).
  val condPathFinishes: Map<PathRoot, AcceptCondition> = emptyMap(),
  // 이 step 후 살아남은 cond path 들의 root.
  val activeCondPaths: Set<PathRoot> = emptySet(),
)

data class FinishedKernelRecord(
  val kernel: Kernel,
  val condition: AcceptCondition,
)

data class ProgressedKernelRecord(
  val symbolId: Int,
  val pointer: Int,
  val startGen: Int,
  val midGen: Int,
  val endGen: Int,
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
) {
  private var _hashCode: Int = 0
  private var _hashCodeComputed: Boolean = false

  override fun hashCode(): Int {
    if (!_hashCodeComputed) {
      var h = gen
      h = 31 * h + milestone.hashCode()
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
    if (milestone != other.milestone) return false
    if (observingCondSymbolIds != other.observingCondSymbolIds) return false
    // parent 비교는 가장 무거우므로 마지막. immutable + sharing 으로 reference equality 가 자주 성립.
    return parent == other.parent
  }

  fun copy(
    gen: Int = this.gen,
    milestone: Kernel = this.milestone,
    parent: MilestonePath? = this.parent,
    observingCondSymbolIds: List<Int> = this.observingCondSymbolIds,
  ): MilestonePath = MilestonePath(gen, milestone, parent, observingCondSymbolIds)

  override fun toString(): String =
    "MilestonePath(gen=$gen, milestone=$milestone, parent=$parent, observingCondSymbolIds=$observingCondSymbolIds)"
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
