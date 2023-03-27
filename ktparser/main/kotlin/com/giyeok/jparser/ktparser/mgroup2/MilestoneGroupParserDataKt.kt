package com.giyeok.jparser.ktparser.mgroup2

import com.giyeok.jparser.ktlib.TermGroupUtil
import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto

class MilestoneGroupParserDataKt(val parserData: MilestoneGroupParserDataProto.MilestoneGroupParserData) {
  val grammar = parserData.grammar
  val startGroupId = parserData.startGroupId
  val initialTasksSummary = parserData.initialTasksSummary

  val milestoneGroups = parserData.milestoneGroupsList.associate { it.groupId to it.milestonesList }

  val termActionsByGroupId = parserData.termActionsList.associateBy { it.groupId }
  val tipEdgeProgressActions = parserData.tipEdgeActionsList.groupBy { it.start.symbolId }
  val tipEdgeRequiredSymbols = parserData.tipEdgeRequiredSymbolsList.groupBy { it.start.symbolId }
  val midEdgeProgressActions = parserData.midEdgeActionsList.groupBy { it.start.symbolId }
  val midEdgeRequiredSymbols = parserData.midEdgeRequiredSymbolsList.groupBy { it.start.symbolId }

  fun findTermAction(groupId: Int, input: Char): MilestoneGroupParserDataProto.TermAction? =
    termActionsByGroupId[groupId]!!.actionsList.find {
      TermGroupUtil.isMatch(it.termGroup, input)
    }?.termAction

  fun getTipEdgeProgressAction(
    start: MilestoneKt,
    end: Int
  ): MilestoneGroupParserDataProto.EdgeAction {
    return tipEdgeProgressActions.getValue(start.symbolId).find {
      it.start.pointer == start.pointer && it.end == end
    }!!.edgeAction
  }

  fun getTipEdgeRequiredSymbols(
    startSymbolId: Int,
    startPointer: Int,
    endGroupId: Int
  ): Collection<Int> {
    return tipEdgeRequiredSymbols.getValue(startSymbolId).find {
      it.start.pointer == startPointer && it.end == endGroupId
    }!!.requiredSymbolIdsList
  }

  fun getMidEdgeProgressAction(
    start: MilestoneKt,
    end: MilestoneKt
  ): MilestoneGroupParserDataProto.EdgeAction {
    // TODO 미리 인덱스 만들어서 성능 개선. 단 메모리 카피는 피해서
    return midEdgeProgressActions.getValue(start.symbolId).find {
      it.start.pointer == start.pointer &&
        it.end.symbolId == end.symbolId && it.end.pointer == end.pointer
    }!!.edgeAction
  }

  fun getMidEdgeRequiredSymbols(
    startSymbolId: Int,
    startPointer: Int,
    endSymbolId: Int,
    endPointer: Int
  ): Collection<Int> {
    // TODO 미리 인덱스 만들어서 성능 개선. 단 메모리 카피는 피해서
    return midEdgeRequiredSymbols.getValue(startSymbolId).find {
      it.start.pointer == startPointer &&
        it.end.symbolId == endSymbolId && it.end.pointer == endPointer
    }!!.requiredSymbolIdsList
  }

  fun milestonesOfGroup(groupId: Int): Collection<MilestoneParserDataProto.KernelTemplate> =
    milestoneGroups.getValue(groupId)

  fun doesGroupContainMilestone(groupId: Int, symbolId: Int, pointer: Int): Boolean {
    val milestones = milestonesOfGroup(groupId)
    return milestones.any { it.symbolId == symbolId && it.pointer == pointer }
  }
}
