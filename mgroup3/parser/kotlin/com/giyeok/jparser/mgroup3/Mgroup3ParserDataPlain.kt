package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate
import com.giyeok.jparser.mgroup3.proto.AppendMilestoneGroup
import com.giyeok.jparser.mgroup3.proto.CondRootStarter as ProtoCondRootStarter
import com.giyeok.jparser.mgroup3.proto.EdgeAction
import com.giyeok.jparser.mgroup3.proto.FinishedKernelTemplate as ProtoFinishedKernelTemplate
import com.giyeok.jparser.mgroup3.proto.KernelTemplate
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import com.giyeok.jparser.mgroup3.proto.ParsingActions
import com.giyeok.jparser.mgroup3.proto.PathRootInfo
import com.giyeok.jparser.mgroup3.proto.ProgressedKernelTemplate as ProtoProgressedKernelTemplate
import com.giyeok.jparser.mgroup3.proto.TermAction
import com.giyeok.jparser.proto.TermGroupProto.TermGroup

typealias ProtoMilestoneGroup = Mgroup3ParserData.MilestoneGroup
typealias ProtoPossibleFinish = Mgroup3ParserData.PossibleFinish
typealias ProtoTermGroupAction = Mgroup3ParserData.TermGroupAction

// Hot path 에 등장하는 protobuf data 를 plain Kotlin 으로 한 번 변환한 view.
// protobuf 의 Map / List getter 가 매번 wrapper / lazy convert 하는 비용을 회피.
// parser instance init 시 한 번만 변환.

class ParserDataPlain(val proto: Mgroup3ParserData) {
  val startSymbolId: Int = proto.startSymbolId

  // 변환된 plain map / list.
  val pathRoots: Map<Int, PathRootInfoPlain> =
    proto.pathRootsMap.entries.associate { (k, v) -> k to PathRootInfoPlain(v) }

  val milestoneGroups: Map<Int, MilestoneGroupPlain> =
    proto.milestoneGroupsMap.entries.associate { (k, v) -> k to MilestoneGroupPlain(v) }

  // term actions: tipGroupId → list of (termGroup, termAction)
  val termActions: Map<Int, List<TermGroupActionPlain>> =
    proto.termActionsMap.entries.associate { (k, v) ->
      k to v.actionsList.map { TermGroupActionPlain(it) }
    }

  val tipEdgeActions: List<TipEdgeActionPair> =
    proto.tipEdgeActionsList.map { TipEdgeActionPair(it.parent, it.tipGroupId, EdgeActionPlain(it.edgeAction)) }

  val midEdgeActions: List<MidEdgeActionPair> =
    proto.midEdgeActionsList.map { MidEdgeActionPair(it.parent, it.tip, EdgeActionPlain(it.edgeAction)) }
}

class PathRootInfoPlain(proto: PathRootInfo) {
  val symbolId: Int = proto.symbolId
  val milestoneGroupId: Int = proto.milestoneGroupId
  val initialCondSymbolIds: List<Int> = proto.initialCondSymbolIdsList.toList()
  val selfFinishAcceptCondition: AcceptConditionTemplate? =
    if (proto.hasSelfFinishAcceptCondition()) proto.selfFinishAcceptCondition else null
  val parsingActions: ParsingActionsPlain? =
    if (proto.hasParsingActions()) ParsingActionsPlain(proto.parsingActions) else null
}

class MilestoneGroupPlain(proto: ProtoMilestoneGroup) {
  val possibleFinishes: List<PossibleFinishPlain> = proto.possibleFinishesList.map { PossibleFinishPlain(it) }
}

class PossibleFinishPlain(proto: ProtoPossibleFinish) {
  val symbolId: Int = proto.symbolId
  val acceptCondition: AcceptConditionTemplate = proto.acceptCondition
}

class TermGroupActionPlain(proto: ProtoTermGroupAction) {
  val termGroup: TermGroup = proto.termGroup
  val termAction: TermActionPlain = TermActionPlain(proto.termAction)
}

class TermActionPlain(proto: TermAction) {
  val replaceAndAppends: List<ReplaceAndAppendPlain> =
    proto.replaceAndAppendsList.map { ReplaceAndAppendPlain(it.replace, AppendMilestoneGroupPlain(it.append)) }
  val replaceAndProgresses: List<ReplaceAndProgressPlain> =
    proto.replaceAndProgressesList.map { ReplaceAndProgressPlain(it.replaceMilestoneGroupId, it.acceptCondition) }
  val parsingActions: ParsingActionsPlain? =
    if (proto.hasParsingActions()) ParsingActionsPlain(proto.parsingActions) else null
}

class ReplaceAndAppendPlain(val replace: KernelTemplate, val append: AppendMilestoneGroupPlain)
class ReplaceAndProgressPlain(val replaceMilestoneGroupId: Int, val acceptCondition: AcceptConditionTemplate)

class AppendMilestoneGroupPlain(proto: AppendMilestoneGroup) {
  val milestoneGroupId: Int = proto.milestoneGroupId
  val acceptCondition: AcceptConditionTemplate = proto.acceptCondition
  // proto list 그대로 reference — MilestonePath 의 observingCondSymbolIds 도 list 받음.
  val observingCondSymbolIds: List<Int> = proto.observingCondSymbolIdsList
  val condRootStarters: List<CondRootStarterPlain> = proto.condRootStartersList.map { CondRootStarterPlain(it) }
}

class CondRootStarterPlain(proto: ProtoCondRootStarter) {
  val symbolId: Int = proto.symbolId
  val milestoneGroupId: Int = proto.milestoneGroupId
}

class EdgeActionPlain(proto: EdgeAction) {
  val appendMilestoneGroups: List<AppendMilestoneGroupPlain> =
    proto.appendMilestoneGroupsList.map { AppendMilestoneGroupPlain(it) }
  val startNodeProgress: AcceptConditionTemplate? =
    if (proto.hasStartNodeProgress()) proto.startNodeProgress else null
  val parsingActions: ParsingActionsPlain? =
    if (proto.hasParsingActions()) ParsingActionsPlain(proto.parsingActions) else null
}

class ParsingActionsPlain(proto: ParsingActions) {
  val progressed: List<ProtoProgressedKernelTemplate> = proto.progressedList.toList()
  val finished: List<ProtoFinishedKernelTemplate> = proto.finishedList.toList()
}

class TipEdgeActionPair(val parent: KernelTemplate, val tipGroupId: Int, val edgeAction: EdgeActionPlain)
class MidEdgeActionPair(val parent: KernelTemplate, val tip: KernelTemplate, val edgeAction: EdgeActionPlain)
