package com.giyeok.jparser.milestone2

import com.giyeok.jparser.nparser.Kernel

case class TasksSummary2(
  addedKernels: Map[AcceptConditionTemplate, Set[Kernel]],
  // TODO progressedKernels는 이제 사용 안함. addedKernels로 통합됨
  progressedKernels: Set[Kernel],
) {
  def trimForSymbols(symbolIds: Set[Int]): TasksSummary2 = {
    TasksSummary2(
      addedKernels.view.mapValues(_.filter(kernel => symbolIds.contains(kernel.symbolId)))
        .filter(_._2.nonEmpty).toMap,
      progressedKernels.filter(kernel => symbolIds.contains(kernel.symbolId)),
    )
  }
}
