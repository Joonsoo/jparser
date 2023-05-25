package com.giyeok.jparser.ktparser.mgroup2

import com.giyeok.jparser.ktparser.mgroup2.MilestoneAcceptConditionKt.Companion.disjunct
import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto

class GenActionsKt(
  val termActions: List<Pair<MilestoneGroupKt, MilestoneGroupParserDataProto.TermAction>>,
  val tipEdgeActions: List<Pair<Pair<MilestoneKt, MilestoneGroupKt>, MilestoneGroupParserDataProto.EdgeAction>>,
  val midEdgeActions: List<Pair<Pair<MilestoneKt, MilestoneKt>, MilestoneGroupParserDataProto.EdgeAction>>,
  val progressedKernels: Map<Pair<MilestoneKt, Int>, MilestoneAcceptConditionKt>,
  val progressedKgroups: Map<Pair<MilestoneGroupKt, Int>, MilestoneAcceptConditionKt>,
) {
  companion object {
    val empty = GenActionsKt(listOf(), listOf(), listOf(), mapOf(), mapOf())
  }

  val progressedMilestones: Map<MilestoneKt, MilestoneAcceptConditionKt> =
    progressedKernels.toList().groupBy { it.first.first }
      .mapValues { (_, pairs) ->
        disjunct(*pairs.map { it.second }.toTypedArray())
      }

  fun milestoneProgressConditionOf(milestone: MilestoneKt): MilestoneAcceptConditionKt =
    progressedMilestones[milestone] ?: MilestoneAcceptConditionKt.Never
}

class GenActionsKtBuilder {
  private val termActions =
    mutableListOf<Pair<MilestoneGroupKt, MilestoneGroupParserDataProto.TermAction>>()
  private val tipEdgeActions =
    mutableListOf<Pair<Pair<MilestoneKt, MilestoneGroupKt>, MilestoneGroupParserDataProto.EdgeAction>>()
  private val midEdgeActions =
    mutableListOf<Pair<Pair<MilestoneKt, MilestoneKt>, MilestoneGroupParserDataProto.EdgeAction>>()
  private val progressedKernels = mutableMapOf<Pair<MilestoneKt, Int>, MilestoneAcceptConditionKt>()
  private val progressedKgroups =
    mutableMapOf<Pair<MilestoneGroupKt, Int>, MilestoneAcceptConditionKt>()

  fun addTermActions(
    milestoneGroup: MilestoneGroupKt,
    termAction: MilestoneGroupParserDataProto.TermAction
  ) {
    termActions.add(Pair(milestoneGroup, termAction))
  }

  fun addTipEdgeAction(
    tipParent: MilestoneKt,
    replacedTip: MilestoneGroupKt,
    edgeAction: MilestoneGroupParserDataProto.EdgeAction
  ) {
    tipEdgeActions.add(Pair(Pair(tipParent, replacedTip), edgeAction))
  }

  fun addMidEdgeAction(
    start: MilestoneKt,
    end: MilestoneKt,
    edgeAction: MilestoneGroupParserDataProto.EdgeAction
  ) {
    midEdgeActions.add(Pair(Pair(start, end), edgeAction))
  }

  fun addProgressedKernelGroup(
    milestoneGroup: MilestoneGroupKt,
    parentGen: Int,
    condition: MilestoneAcceptConditionKt
  ) {
    val existing = progressedKgroups[Pair(milestoneGroup, parentGen)]
    val merged =
      if (existing == null) condition else MilestoneAcceptConditionKt.disjunct(existing, condition)
    progressedKgroups[Pair(milestoneGroup, parentGen)] = merged
  }

  fun addProgressedKernel(
    milestone: MilestoneKt,
    parentGen: Int,
    condition: MilestoneAcceptConditionKt
  ) {
    val existing = progressedKernels[Pair(milestone, parentGen)]
    val merged =
      if (existing == null) condition else MilestoneAcceptConditionKt.disjunct(existing, condition)
    progressedKernels[Pair(milestone, parentGen)] = merged
  }

  fun build(): GenActionsKt = GenActionsKt(
    termActions.toList(),
    tipEdgeActions.toList(),
    midEdgeActions.toList(),
    progressedKernels.toMap(),
    progressedKgroups.toMap(),
  )
}
