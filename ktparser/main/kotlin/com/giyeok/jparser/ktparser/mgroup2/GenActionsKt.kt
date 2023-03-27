package com.giyeok.jparser.ktparser.mgroup2

import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto

class GenActionsKt(
  val termActions: List<Pair<MilestoneGroupKt, MilestoneGroupParserDataProto.TermAction>>,
  val tipEdgeActions: List<Pair<Pair<MilestoneKt, MilestoneGroupKt>, MilestoneGroupParserDataProto.EdgeAction>>,
  val midEdgeActions: List<Pair<Pair<MilestoneKt, MilestoneKt>, MilestoneGroupParserDataProto.EdgeAction>>,
  val progressedMilestones: Map<MilestoneKt, MilestoneAcceptConditionKt>,
  val progressedMilestoneParentGens: Map<MilestoneKt, Set<Int>>,
  val progressedMgroups: Map<MilestoneGroupKt, MilestoneAcceptConditionKt>,
  val progressedMgroupParentGens: Map<MilestoneGroupKt, Set<Int>>,
) {
  companion object {
    val empty = GenActionsKt(listOf(), listOf(), listOf(), mapOf(), mapOf(), mapOf(), mapOf())
  }
}

class GenActionsKtBuilder {
  private val termActions =
    mutableListOf<Pair<MilestoneGroupKt, MilestoneGroupParserDataProto.TermAction>>()
  private val tipEdgeActions =
    mutableListOf<Pair<Pair<MilestoneKt, MilestoneGroupKt>, MilestoneGroupParserDataProto.EdgeAction>>()
  private val midEdgeActions =
    mutableListOf<Pair<Pair<MilestoneKt, MilestoneKt>, MilestoneGroupParserDataProto.EdgeAction>>()
  private val progressedMilestones = mutableMapOf<MilestoneKt, MilestoneAcceptConditionKt>()
  private val progressedMilestoneParentGens = mutableMapOf<MilestoneKt, MutableSet<Int>>()
  private val progressedMgroups = mutableMapOf<MilestoneGroupKt, MilestoneAcceptConditionKt>()
  private val progressedMgroupParentGen = mutableMapOf<MilestoneGroupKt, MutableSet<Int>>()

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

  fun addProgressedMilestoneGroup(
    milestoneGroup: MilestoneGroupKt,
    condition: MilestoneAcceptConditionKt
  ) {
    val existing = progressedMgroups[milestoneGroup]
    val merged =
      if (existing == null) condition else MilestoneAcceptConditionKt.disjunct(existing, condition)
    progressedMgroups[milestoneGroup] = merged
  }

  fun addProgressedMilestoneGroupParentGen(mgroup: MilestoneGroupKt, parentGen: Int) {
    progressedMgroupParentGen.getOrPut(mgroup) { mutableSetOf() }.add(parentGen)
  }

  fun addProgressedMilestone(milestone: MilestoneKt, condition: MilestoneAcceptConditionKt) {
    val existing = progressedMilestones[milestone]
    val merged =
      if (existing == null) condition else MilestoneAcceptConditionKt.disjunct(existing, condition)
    progressedMilestones[milestone] = merged
  }

  fun addProgressedMilestoneParentGen(milestone: MilestoneKt, parentGen: Int) {
    progressedMilestoneParentGens.getOrPut(milestone) { mutableSetOf() }.add(parentGen)
  }

  fun build(): GenActionsKt = GenActionsKt(
    termActions.toList(),
    tipEdgeActions.toList(),
    midEdgeActions.toList(),
    progressedMilestones.toMap(),
    progressedMilestoneParentGens.toMap(),
    progressedMgroups.toMap(),
    progressedMgroupParentGen.toMap(),
  )
}
