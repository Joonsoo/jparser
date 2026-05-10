package com.giyeok.jparser.mgroup3

data class ParsingCtx(
  val gen: Int,
  val line: Int,
  val col: Int,
  val mainRoot: PathRoot,
  val mainPaths: List<ParsingPath>,
  // 살아있는 condition path들. 같은 PathRoot에 여러 ParsingPath가 들어있을 수 있다.
  val condPaths: Map<PathRoot, List<ParsingPath>>,
  // 매 step마다 발생한 actions를 저장 (parse tree 복원에 쓰일 수 있음)
  // 0번째 entry는 initialCtx에서 발생한 actions, 이후 i번째 entry는 i번째 input을 처리하면서 발생한 actions
  val history: List<HistoryEntry>,
)

data class HistoryEntry(
  // 이 step에서 발생한 모든 finished kernel들 (condition별로 묶을 수도 있지만 단순히 list로 둠)
  val finishedKernels: List<FinishedKernelRecord>,
  // 이 step에서 발생한 progressed kernel들
  val progressedKernels: List<ProgressedKernelRecord>,
  // 이 step에서 finish된 cond path들의 root → finish accept condition (Or로 묶인)
  // 후속 evolve나 isAccepted 시 활용
  val condPathFinishes: Map<PathRoot, AcceptCondition> = emptyMap(),
  // 이 step 후에 살아남은 cond path들의 root (active 여부 판단용)
  val activeCondPaths: Set<PathRoot> = emptySet(),
)

// finished는 (symbolId, pointer, startGen)이 condition 하에서 finish됨을 나타냄
// 실제 finishGen은 ParsingCtx.gen에서 추정 가능
data class FinishedKernelRecord(
  val kernel: Kernel,
  val condition: AcceptCondition,
)

// progressed는 (symbolId, pointer, startGen, midGen)에서 (symbolId, pointer+1, startGen, gen=endGen)으로 진행됨을 나타냄
// kernelsHistory에서 두 kernel을 더해야 함:
//   (symbolId, pointer, startGen, midGen) - progress 전 상태 (이미 finish됐을 수도 있음)
//   (symbolId, pointer+1, startGen, endGen) - progress 후 상태
data class ProgressedKernelRecord(
  val symbolId: Int,
  val pointer: Int,
  val startGen: Int,
  val midGen: Int,
  val endGen: Int,
)

data class ParsingPath(
  // milestonePath == null이라는 것은 starter path인 경우, 즉 root가 tipGroupId에 포함되어 있는 경우를 나타냄
  // 이 때는 TermAction 실행시 TermAction의 replace 하나로 구성된 MilestonePath로 변경해야 한다
  val milestonePath: MilestonePath?,
  val tipGroupId: Int,
  val acceptCondition: AcceptCondition,
)

data class PathRoot(val symbolId: Int, val startGen: Int)

// start -> a -> b -> X 의 경로는
// MilestonePath(b, MilestonePath(a, MilestonePath(start, null))) 와 같이 표현된다.
// 그래서 MilestonePath 하나는 엣지 하나를 나타내는 것과 같다.
// observingCondRoots는 해당 엣지에 포함된 그래프가 추후에 필요하게 될 지도 모를 모든 cond symbol root들을 나타낸다.
data class MilestonePath(
  // 이 milestone이 만들어진 시점의 gen (즉, milestone.gen과 같음)
  val gen: Int,
  // 이 milestone에 해당하는 kernel
  val milestone: Kernel,
  val parent: MilestonePath?,
  // 이 milestone이 다음으로 진행하기 위해서 추적해야 하는 cond symbol들의 root
  // (path가 만들어진 gen 시점에 추적되는 cond symbol들)
  val observingCondSymbolIds: Set<Int>,
)

data class Kernel(val symbolId: Int, val pointer: Int, val gen: Int) {
  val kernelTemplate: KernelTemplatePair get() = KernelTemplatePair(symbolId, pointer)
}

data class KernelTemplatePair(val symbolId: Int, val pointer: Int)
