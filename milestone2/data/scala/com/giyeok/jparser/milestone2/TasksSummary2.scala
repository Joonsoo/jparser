package com.giyeok.jparser.milestone2

import com.giyeok.jparser.nparser.Kernel

case class TasksSummary2(
  addedKernels: Map[AcceptConditionTemplate, Set[Kernel]],
  progressedKernels: Set[Kernel],
  progressedStartKernel: Option[Kernel],
) {
  def trimForSymbols(symbolIds: Set[Int]): TasksSummary2 = {
    TasksSummary2(
      addedKernels.view.mapValues(_.filter(kernel => symbolIds.contains(kernel.symbolId)))
        .filter(_._2.nonEmpty).toMap,
      progressedKernels.filter(kernel => symbolIds.contains(kernel.symbolId)),
      progressedStartKernel
    )
  }
}
