package com.giyeok.jparser.milestone2

import scala.collection.MapView

case class ParsingContext(
  gen: Int,
  paths: List[MilestonePath],
  history: List[HistoryEntry],
  // 지금까지(이전 세대들에서) 관찰된 root milestone progress의 누적 기록.
  // (root milestone -> 관찰 조건; 관찰 세대부터 매 세대 evolve되어 "현재 세대 직전" 좌표계)
  //
  // Exists/NotExists(unbounded lookahead)는 진릿값이 (symbol, gen)만의 함수 —
  // "gen에서 시작하는 symbol 매치가 (end 무관하게) 존재하는가" — 이므로 조건 인스턴스가
  // *언제* 물질화됐는지와 무관하다. 그런데 milestone 계열에서 조건은 dot이 조건부 kernel을
  // 지나는 세대에 비로소 물질화되므로, 감시 대상 watcher가 이미 완성·소멸한 *뒤*에 태어나는
  // leaf가 정상적으로 존재한다 (예: `Program = !"fn" Expression ';'`, `Expression = "fn" "()"`,
  // 입력 "fn();" — watcher는 gen 2에 끝나지만 `!"fn"` leaf는 gen 4에 물질화됨).
  // 그 때 현재 세대의 genActions만 보면 "매치 없음"으로 오해소되어 lookahead가 사라진다.
  // bounded(OnlyIf/Unless)와 longest(checkFromNextGen)는 정확한 span의 progress만
  // discharge에 써야 하므로 이 기록을 보지 않는다.
  seenProgressedRootMilestones: Map[Milestone, MilestoneAcceptCondition] = Map())

// path는 가장 뒤에 것이 가장 앞에 옴. first는 언제나 path.last와 동일
case class MilestonePath(first: Milestone, path: List[Milestone], acceptCondition: MilestoneAcceptCondition) {
  def prettyString: String = {
    val milestones = path.reverse.map(milestone => s"${milestone.symbolId} ${milestone.pointer} ${milestone.gen}")
    s"${milestones.mkString(" -> ")} ($acceptCondition)"
  }

  def tip: Milestone = path.head

  def tipParent: Option[Milestone] = path.drop(1).headOption

  def append(newTip: Milestone, newAcceptCondition: MilestoneAcceptCondition): MilestonePath =
    MilestonePath(first, newTip +: path, newAcceptCondition)

  def pop(newAcceptCondition: MilestoneAcceptCondition): MilestonePath =
    MilestonePath(first, path.drop(1), newAcceptCondition)
}

object MilestonePath {
  def apply(milestone: Milestone): MilestonePath =
    MilestonePath(milestone, List(milestone), Always)
}

case class Milestone(symbolId: Int, pointer: Int, gen: Int) {
  def kernelTemplate: KernelTemplate = KernelTemplate(symbolId, pointer)
}

object Milestone {
  def apply(template: KernelTemplate, gen: Int): Milestone =
    Milestone(template.symbolId, template.pointer, gen)
}

// TODO TermAction하고 EdgeAction에 ID를 붙이는게 좋을까?
case class GenActions(
  termActions: List[(Milestone, TermAction)],
  edgeActions: List[((Milestone, Milestone), EdgeAction)],
  // (milestone, parentGen) -> parent gen
  // milestone과 parentGen을 조합하면 커널이 되기 때문에 이름이 progressedKernels
  progressedKernels: Map[(Milestone, Int), MilestoneAcceptCondition],
  progressedRootMilestones: Map[Milestone, MilestoneAcceptCondition],
)

case class HistoryEntry(untrimmedPaths: List[MilestonePath], genActions: GenActions)
