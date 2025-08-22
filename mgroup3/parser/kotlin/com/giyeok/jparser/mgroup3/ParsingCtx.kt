package com.giyeok.jparser.mgroup3

data class ParsingCtx(
  val gen: Int,
  val mainRoot: PathRoot,
  val mainPaths: List<ParsingPath>,
  val condPaths: Map<PathRoot, List<ParsingPath>>,
  // TODO history
)

data class ParsingPath(
  // val root: PathRoot, // ParsingCtx를 통해서 알 수 있음

  // milestonePath == null이라는 것은 starter path인 경우, 즉 root가 tipGroupId에 포함되어 있는 경우를 나타냄
  // 이 때는 TermAction 실행시 TermAction의 replace 하나로 구성된 MilestonePath로 변경해야 한다
  val milestonePath: MilestonePath?,
  val tipGroupId: Int,
  // val endGen: Int,  // ParsingCtx를 통해서 알 수 있음
  val acceptCondition: AcceptCondition
)

data class PathRoot(val symbolId: Int, val startGen: Int)

// start -> a -> b -> X 의 경로는
// MilestonePath(b, MilestonePath(a, MilestonePath(start, null))) 와 같이 표현된다.
// 그래서 MilestonePath 하나는 엣지 하나를 나타내는 것과 같다.
// observingCondRoots는 해당 엣지에 포함된 그래프가 추후에 필요하게 될 지도 모를 모든 cond symbol root들을 나타낸다.
// start -> a 사이의 observingCondRoots는 MilestonePath(start, null)에 저장되고,
// a -> b 사이의 observingCondRoots는 MilestonePath(a, MilestonePath(start, null))에 저장되는 식.
data class MilestonePath(
  val gen: Int,
  val milestone: Kernel,
  val parent: MilestonePath?,
  // observingCondSymbolIds.map { PathRoot(it, gen) } 을 추적해야 함
  val observingCondSymbolIds: Set<Int>,
)

data class Kernel(val symbolId: Int, val pointer: Int, val gen: Int)
