package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto
import com.giyeok.jparser.milestone2.{KernelTemplate, MilestoneParser2ProtobufConverter}
import com.giyeok.jparser.proto.GrammarProtobufConverter.{convertNGrammarToProto, convertProtoToNGrammar}
import MilestoneParser2ProtobufConverter.kernelTemplateEdgeOrdering
import MilestoneParser2ProtobufConverter.termGroupOrdering
import com.giyeok.jparser.proto.ProtoConverterUtil.JavaListToScalaCollection
import com.giyeok.jparser.proto.TermGroupProtobufConverter.{convertProtoToTermGroup, convertTermGroupToProto}

import scala.math.Ordering.comparatorToOrdering

object MilestoneGroupParserDataProtobufConverter {
  implicit val tipEdgeOrdering: Ordering[(KernelTemplate, Int)] = comparatorToOrdering {
    (o1: (KernelTemplate, Int), o2: (KernelTemplate, Int)) =>
      if (o1._1 != o2._1) o1._1.compareTo(o2._1) else o1._2 - o2._2
  }

  implicit val lookaheadRequiresOrdering: Ordering[LookaheadRequires] = comparatorToOrdering {
    (o1: LookaheadRequires, o2: LookaheadRequires) =>
      if (o1.symbolId != o2.symbolId) o1.symbolId - o2.symbolId else o1.groupId - o2.groupId
  }

  def toProto(parserData: MilestoneGroupParserData): MilestoneGroupParserDataProto.MilestoneGroupParserData = {
    val builder = MilestoneGroupParserDataProto.MilestoneGroupParserData.newBuilder()
      .setGrammar(convertNGrammarToProto(parserData.grammar))
      .setStartGroupId(parserData.startGroupId)
      .setInitialTasksSummary(MilestoneParser2ProtobufConverter.toProto(parserData.initialTasksSummary))

    parserData.milestoneGroups.toList.sortBy(_._1).foreach { case (groupId, milestones) =>
      val groupBuilder = builder.addMilestoneGroupsBuilder()
      groupBuilder.setGroupId(groupId)
      milestones.toList.sorted.foreach { kernel =>
        groupBuilder.addMilestones(MilestoneParser2ProtobufConverter.toProto(kernel))
      }
    }

    parserData.termActions.toList.sortBy(_._1).foreach { case (groupId, termActions) =>
      val termActionsBuilder = builder.addTermActionsBuilder()
      termActionsBuilder.setGroupId(groupId)
      termActions.sortBy(_._1).foreach { termAction =>
        val termActionBuilder = termActionsBuilder.addActionsBuilder()
        termActionBuilder.setTermGroup(convertTermGroupToProto(termAction._1))
        termActionBuilder.setTermAction(toProto(termAction._2))
      }
    }

    parserData.tipEdgeProgressActions.toList.sortBy(_._1).foreach { case ((start, end), edgeAction) =>
      val tipEdgeBuilder = builder.addTipEdgeActionsBuilder()
      tipEdgeBuilder.setStart(MilestoneParser2ProtobufConverter.toProto(start))
      tipEdgeBuilder.setEnd(end)
      tipEdgeBuilder.setEdgeAction(toProto(edgeAction))
    }

    parserData.tipEdgeRequiredSymbols.toList.sortBy(_._1).foreach { case ((start, end), requiredSymbols) =>
      val tipEdgeBuilder = builder.addTipEdgeRequiredSymbolsBuilder()
      tipEdgeBuilder.setStart(MilestoneParser2ProtobufConverter.toProto(start))
      tipEdgeBuilder.setEnd(end)
      requiredSymbols.toList.sorted.foreach { symbolId =>
        tipEdgeBuilder.addRequiredSymbolIds(symbolId)
      }
    }

    parserData.midEdgeProgressActions.toList.sortBy(_._1).foreach { case ((start, end), edgeAction) =>
      val midEdgeBuilder = builder.addMidEdgeActionsBuilder()
      midEdgeBuilder.setStart(MilestoneParser2ProtobufConverter.toProto(start))
      midEdgeBuilder.setEnd(MilestoneParser2ProtobufConverter.toProto(end))
      midEdgeBuilder.setEdgeAction(toProto(edgeAction))
    }

    parserData.midEdgeRequiredSymbols.toList.sortBy(_._1).foreach { case ((start, end), requiredSymbols) =>
      val midEdgeBuilder = builder.addMidEdgeRequiredSymbolsBuilder()
      midEdgeBuilder.setStart(MilestoneParser2ProtobufConverter.toProto(start))
      midEdgeBuilder.setEnd(MilestoneParser2ProtobufConverter.toProto(end))
      requiredSymbols.toList.sorted.foreach { symbolId =>
        midEdgeBuilder.addRequiredSymbolIds(symbolId)
      }
    }

    builder.build()
  }

  def toProto(termAction: TermAction): MilestoneGroupParserDataProto.TermAction = {
    val builder = MilestoneGroupParserDataProto.TermAction.newBuilder()
    termAction.appendingMilestoneGroups.sortBy(_._1).foreach { appending =>
      val appendingBuilder = builder.addAppendingMilestoneGroupsBuilder()
      appendingBuilder.setReplace(MilestoneParser2ProtobufConverter.toProto(appending._1))
      appendingBuilder.setAppend(toProto(appending._2))
    }
    termAction.startNodeProgress.sortBy(_._1).foreach { startNodeProgress =>
      val startNodeProgressBuilder = builder.addStartNodeProgressesBuilder()
      startNodeProgressBuilder.setReplaceGroupId(startNodeProgress._1)
      startNodeProgressBuilder.setAcceptCondition(MilestoneParser2ProtobufConverter.toProto(startNodeProgress._2))
    }
    termAction.lookaheadRequiringSymbols.toList.sorted.foreach { lookaheadRequires =>
      builder.addLookaheadRequiringSymbols(toProto(lookaheadRequires))
    }
    builder.setTasksSummary(MilestoneParser2ProtobufConverter.toProto(termAction.tasksSummary))
    termAction.pendedAcceptConditionKernels.toList.sortBy(_._1).foreach { case (start, (appendings, condition)) =>
      val pendedBuilder = builder.addPendedAcceptConditionKernelsBuilder()
      pendedBuilder.setKernelTemplate(MilestoneParser2ProtobufConverter.toProto(start))
      appendings.sortBy(_.groupId).foreach { appending =>
        pendedBuilder.addAppendings(toProto(appending))
      }
      condition match {
        case Some(condition) =>
          pendedBuilder.setFirstKernelProgressCondition(MilestoneParser2ProtobufConverter.toProto(condition))
        case None => // do nothing
      }
    }
    builder.build()
  }

  def toProto(edgeAction: EdgeAction): MilestoneGroupParserDataProto.EdgeAction = {
    val builder = MilestoneGroupParserDataProto.EdgeAction.newBuilder()
    edgeAction.appendingMilestoneGroups.sortBy(_.groupId).foreach { appending =>
      builder.addAppendingMilestoneGroups(toProto(appending))
    }
    edgeAction.startNodeProgress match {
      case Some(startNodeProgress) =>
        builder.setStartNodeProgress(MilestoneParser2ProtobufConverter.toProto(startNodeProgress))
      case None => // do nothing
    }
    edgeAction.lookaheadRequiringSymbols.toList.sorted.foreach { lookaheadRequires =>
      builder.addLookaheadRequiringSymbols(toProto(lookaheadRequires))
    }
    builder.setTasksSummary(MilestoneParser2ProtobufConverter.toProto(edgeAction.tasksSummary))
    builder.build()
  }

  def toProto(appendingMilestoneGroup: AppendingMilestoneGroup): MilestoneGroupParserDataProto.AppendingMilestoneGroup = {
    val builder = MilestoneGroupParserDataProto.AppendingMilestoneGroup.newBuilder()
    builder.setGroupId(appendingMilestoneGroup.groupId)
    builder.setAcceptCondition(MilestoneParser2ProtobufConverter.toProto(appendingMilestoneGroup.acceptCondition))
    builder.build()
  }

  def toProto(lookaheadRequires: LookaheadRequires): MilestoneGroupParserDataProto.LookaheadRequires = {
    val builder = MilestoneGroupParserDataProto.LookaheadRequires.newBuilder()
    builder.setSymbolId(lookaheadRequires.symbolId)
    builder.setGroupId(lookaheadRequires.groupId)
    builder.build()
  }

  def fromProto(proto: MilestoneGroupParserDataProto.MilestoneGroupParserData): MilestoneGroupParserData = {
    MilestoneGroupParserData(
      convertProtoToNGrammar(proto.getGrammar),
      proto.getStartGroupId,
      MilestoneParser2ProtobufConverter.fromProto(proto.getInitialTasksSummary),
      proto.getMilestoneGroupsList.toScalaMap(
        { pair => pair.getGroupId },
        { pair => pair.getMilestonesList.toScalaSet(MilestoneParser2ProtobufConverter.fromProto) }
      ),
      termActions = proto.getTermActionsList.toScalaMap(
        { pair => pair.getGroupId },
        { pair =>
          pair.getActionsList.toScalaList { actionPair =>
            convertProtoToTermGroup(actionPair.getTermGroup) -> fromProto(actionPair.getTermAction)
          }
        }
      ),
      tipEdgeProgressActions = proto.getTipEdgeActionsList.toScalaMap(
        { pair => MilestoneParser2ProtobufConverter.fromProto(pair.getStart) -> pair.getEnd },
        { pair => fromProto(pair.getEdgeAction) }
      ),
      tipEdgeRequiredSymbols = proto.getTipEdgeRequiredSymbolsList.toScalaMap(
        { pair => MilestoneParser2ProtobufConverter.fromProto(pair.getStart) -> pair.getEnd },
        { pair => pair.getRequiredSymbolIdsList.toScalaSet(x => x) }
      ),
      midEdgeProgressActions = proto.getMidEdgeActionsList.toScalaMap(
        { pair => MilestoneParser2ProtobufConverter.fromProto(pair.getStart) -> MilestoneParser2ProtobufConverter.fromProto(pair.getEnd) },
        { pair => fromProto(pair.getEdgeAction) }
      ),
      midEdgeRequiredSymbols = proto.getMidEdgeRequiredSymbolsList.toScalaMap(
        { pair => MilestoneParser2ProtobufConverter.fromProto(pair.getStart) -> MilestoneParser2ProtobufConverter.fromProto(pair.getEnd) },
        { pair => pair.getRequiredSymbolIdsList.toScalaSet(x => x) }
      ),
    )
  }

  def fromProto(proto: MilestoneGroupParserDataProto.TermAction): TermAction = {
    TermAction(
      appendingMilestoneGroups = proto.getAppendingMilestoneGroupsList.toScalaList { appending =>
        MilestoneParser2ProtobufConverter.fromProto(appending.getReplace) -> fromProto(appending.getAppend)
      },
      startNodeProgress = proto.getStartNodeProgressesList.toScalaList { progress =>
        progress.getReplaceGroupId -> MilestoneParser2ProtobufConverter.fromProto(progress.getAcceptCondition)
      },
      lookaheadRequiringSymbols = proto.getLookaheadRequiringSymbolsList.toScalaSet(fromProto),
      tasksSummary = MilestoneParser2ProtobufConverter.fromProto(proto.getTasksSummary),
      pendedAcceptConditionKernels = proto.getPendedAcceptConditionKernelsList.toScalaMap(
        { pended => MilestoneParser2ProtobufConverter.fromProto(pended.getKernelTemplate) },
        { pended =>
          val appendings = pended.getAppendingsList.toScalaList(fromProto)
          val condition = if (!pended.hasFirstKernelProgressCondition) None else {
            Some(MilestoneParser2ProtobufConverter.fromProto(pended.getFirstKernelProgressCondition))
          }
          (appendings, condition)
        }
      )
    )
  }

  def fromProto(proto: MilestoneGroupParserDataProto.EdgeAction): EdgeAction = {
    EdgeAction(
      appendingMilestoneGroups = proto.getAppendingMilestoneGroupsList.toScalaList(fromProto),
      startNodeProgress = if (proto.hasStartNodeProgress) Some(MilestoneParser2ProtobufConverter.fromProto(proto.getStartNodeProgress)) else None,
      lookaheadRequiringSymbols = proto.getLookaheadRequiringSymbolsList.toScalaSet(fromProto),
      tasksSummary = MilestoneParser2ProtobufConverter.fromProto(proto.getTasksSummary),
    )
  }

  def fromProto(proto: MilestoneGroupParserDataProto.AppendingMilestoneGroup): AppendingMilestoneGroup = {
    AppendingMilestoneGroup(proto.getGroupId, MilestoneParser2ProtobufConverter.fromProto(proto.getAcceptCondition))
  }

  def fromProto(proto: MilestoneGroupParserDataProto.LookaheadRequires): LookaheadRequires = {
    LookaheadRequires(proto.getSymbolId, proto.getGroupId)
  }
}
