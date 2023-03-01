package com.giyeok.jparser.proto

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.fast.{KernelTemplate, TasksSummary2}
import com.giyeok.jparser.milestone2._
import com.giyeok.jparser.proto.GrammarProtobufConverter.{convertNGrammarToProto, convertProtoToNGrammar}
import com.giyeok.jparser.proto.MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase
import com.giyeok.jparser.proto.MilestoneParserDataProto.{AppendingMilestone2, Milestone2ParserData, ParsingAction2}
import com.giyeok.jparser.proto.NaiveParserProtobufConverter.{convertKernelToProto, convertProtoToKernel}
import com.giyeok.jparser.proto.ProtoConverterUtil.{JavaListToScalaCollection, toScalaIntList}
import com.giyeok.jparser.proto.TermGroupProtobufConverter.{convertProtoToTermGroup, convertTermGroupToProto}
import com.google.protobuf.Empty

object MilestoneParser2ProtobufConverter {
  def toProto(parserData: MilestoneParserData): MilestoneParserDataProto.Milestone2ParserData = {
    val builder = MilestoneParserDataProto.Milestone2ParserData.newBuilder()
      .setGrammar(convertNGrammarToProto(parserData.grammar))
      .setInitialTasksSummary(toProto(parserData.initialTasksSummary))

    parserData.termActions.foreach { termAction =>
      val termActionBuilder = builder.addTermActionsBuilder()
      termActionBuilder.setKernelTemplate(toProto(termAction._1))
      termAction._2.foreach { action =>
        val actionBuilder = termActionBuilder.addActionsBuilder()
        actionBuilder
          .setTermGroup(convertTermGroupToProto(action._1))
          .setParsingAction(toProto(action._2.parsingAction))
        action._2.pendedAcceptConditionKernels.foreach { pair =>
          val pendedBuilder = actionBuilder.addPendedAcceptConditionKernelsBuilder()
          pendedBuilder.setKernelTemplate(toProto(pair._1))
          pair._2._1.foreach { appending =>
            pendedBuilder.addAppendingMilestones(toProto(appending))
          }
          pair._2._2.foreach { firstKernelProgressCondition =>
            pendedBuilder.setFirstKernelProgressCondition(toProto(firstKernelProgressCondition))
          }
        }
      }
    }

    parserData.edgeProgressActions.foreach { edgeProgressAction =>
      val edgeActionBuilder = builder.addEdgeActionsBuilder()
      edgeActionBuilder.setStart(toProto(edgeProgressAction._1._1))
      edgeActionBuilder.setEnd(toProto(edgeProgressAction._1._2))
      edgeActionBuilder.setParsingAction(toProto(edgeProgressAction._2.parsingAction))
      edgeProgressAction._2.requiredSymbols.foreach(edgeActionBuilder.addRequiredSymbolIds)
    }

    builder.build()
  }

  def toProto(tasksSummary: TasksSummary2): MilestoneParserDataProto.TasksSummary2 = {
    val builder = MilestoneParserDataProto.TasksSummary2.newBuilder()
    tasksSummary.addedKernels.foreach { pair =>
      val pairBuilder = builder.addAddedKernelsBuilder()
      pairBuilder.setAcceptCondition(toProto(pair._1))
      pair._2.foreach { kernel =>
        pairBuilder.addKernels(convertKernelToProto(kernel))
      }
    }
    tasksSummary.progressedKernels.foreach { kernel =>
      builder.addProgressedKernels(convertKernelToProto(kernel))
    }
    tasksSummary.progressedStartKernel.foreach { kernel =>
      builder.setProgressedStartKernel(convertKernelToProto(kernel))
    }
    builder.build()
  }

  def toProto(kernelTemplate: KernelTemplate): MilestoneParserDataProto.KernelTemplate =
    MilestoneParserDataProto.KernelTemplate.newBuilder()
      .setSymbolId(kernelTemplate.symbolId)
      .setPointer(kernelTemplate.pointer)
      .build()

  def toProto(parsingAction: ParsingAction): MilestoneParserDataProto.ParsingAction2 = {
    val builder = MilestoneParserDataProto.ParsingAction2.newBuilder()
    parsingAction.appendingMilestones.foreach { appending =>
      builder.addAppendingMilestones(toProto(appending))
    }
    parsingAction.startNodeProgressCondition.foreach { startProgress =>
      builder.setStartNodeProgressCondition(toProto(startProgress))
    }
    parsingAction.lookaheadRequiringSymbols.foreach { symbolId =>
      builder.addLookaheadRequiringSymbolIds(symbolId)
    }
    builder.setTasksSummary(toProto(parsingAction.tasksSummary))
    builder.build()
  }

  def toProto(appendingMilestone: AppendingMilestone): MilestoneParserDataProto.AppendingMilestone2 = {
    AppendingMilestone2.newBuilder()
      .setMilestone(toProto(appendingMilestone.milestone))
      .setAcceptCondition(toProto(appendingMilestone.acceptCondition))
      .build()
  }

  def toProto(condition: AcceptConditionTemplate): MilestoneParserDataProto.AcceptConditionTemplate = {
    val builder = MilestoneParserDataProto.AcceptConditionTemplate.newBuilder()
    condition match {
      case AlwaysTemplate =>
        builder.setAlways(Empty.getDefaultInstance)
      case NeverTemplate =>
        builder.setNever(Empty.getDefaultInstance)
      case AndTemplate(conditions) =>
        conditions.foreach { cond =>
          builder.getAndBuilder().addConditions(toProto(cond))
        }
      case OrTemplate(conditions) =>
        conditions.foreach { cond =>
          builder.getOrBuilder().addConditions(toProto(cond))
        }
      case LookaheadIsTemplate(symbolId, fromNextGen) =>
        builder.getLookaheadIsBuilder
          .setSymbolId(symbolId)
          .setFromNextGen(fromNextGen)
      case LookaheadNotTemplate(symbolId, fromNextGen) =>
        builder.getLookaheadNotBuilder
          .setSymbolId(symbolId)
          .setFromNextGen(fromNextGen)
      case LongestTemplate(symbolId) =>
        builder.setLongest(symbolId)
      case OnlyIfTemplate(symbolId) =>
        builder.setOnlyIf(symbolId)
      case UnlessTemplate(symbolId) =>
        builder.setUnless(symbolId)
    }
    builder.build()
  }

  def fromProto(proto: MilestoneParserDataProto.Milestone2ParserData): MilestoneParserData = {
    MilestoneParserData(
      convertProtoToNGrammar(proto.getGrammar),
      fromProto(proto.getInitialTasksSummary),
      proto.getTermActionsList.toScalaMap(
        { pair => fromProto(pair.getKernelTemplate) },
        { pair => pair.getActionsList.toScalaList(fromProto) }
      ),
      proto.getEdgeActionsList.toScalaMap(
        { pair => fromProto(pair.getStart) -> fromProto(pair.getEnd) },
        { pair => EdgeAction(fromProto(pair.getParsingAction), pair.getRequiredSymbolIdsList.toSet) }
      )
    )
  }

  def fromProto(proto: MilestoneParserDataProto.TasksSummary2): TasksSummary2 = {
    TasksSummary2(
      proto.getAddedKernelsList.toScalaMap(
        { pair => fromProto(pair.getAcceptCondition) },
        { pair => pair.getKernelsList.toScalaSet(convertProtoToKernel) }
      ),
      proto.getProgressedKernelsList.toScalaSet(convertProtoToKernel),
      if (proto.hasProgressedStartKernel) Some(convertProtoToKernel(proto.getProgressedStartKernel)) else None,
    )
  }

  def fromProto(kernelTemplate: MilestoneParserDataProto.KernelTemplate): KernelTemplate = {
    KernelTemplate(kernelTemplate.getSymbolId, kernelTemplate.getPointer)
  }

  def fromProto(proto: Milestone2ParserData.TermActionPair.TermGroupAction): (TermGroupDesc, TermAction) = {
    val termGroupDesc = convertProtoToTermGroup(proto.getTermGroup)
    val termAction = TermAction(
      fromProto(proto.getParsingAction),
      proto.getPendedAcceptConditionKernelsList.toScalaMap(
        { pair => fromProto(pair.getKernelTemplate) },
        { pair =>
          (
            pair.getAppendingMilestonesList.toScalaList(fromProto),
            if (pair.hasFirstKernelProgressCondition) Some(fromProto(pair.getFirstKernelProgressCondition)) else None
          )
        }
      )
    )
    termGroupDesc -> termAction
  }

  def fromProto(proto: AppendingMilestone2): AppendingMilestone = {
    AppendingMilestone(
      fromProto(proto.getMilestone),
      fromProto(proto.getAcceptCondition)
    )
  }

  def fromProto(proto: ParsingAction2): ParsingAction = {
    ParsingAction(
      proto.getAppendingMilestonesList.toScalaList(fromProto),
      if (proto.hasStartNodeProgressCondition) Some(fromProto(proto.getStartNodeProgressCondition)) else None,
      proto.getLookaheadRequiringSymbolIdsList.toSet,
      fromProto(proto.getTasksSummary)
    )
  }

  def fromProto(proto: MilestoneParserDataProto.AcceptConditionTemplate): AcceptConditionTemplate = {
    proto.getConditionCase match {
      case ConditionCase.ALWAYS => AlwaysTemplate
      case ConditionCase.NEVER => NeverTemplate
      case ConditionCase.AND => AndTemplate(proto.getAnd.getConditionsList.toScalaList(fromProto))
      case ConditionCase.OR => OrTemplate(proto.getAnd.getConditionsList.toScalaList(fromProto))
      case ConditionCase.LOOKAHEAD_IS =>
        LookaheadIsTemplate(proto.getLookaheadIs.getSymbolId, proto.getLookaheadIs.getFromNextGen)
      case ConditionCase.LOOKAHEAD_NOT =>
        LookaheadNotTemplate(proto.getLookaheadNot.getSymbolId, proto.getLookaheadNot.getFromNextGen)
      case ConditionCase.LONGEST => LongestTemplate(proto.getLongest)
      case ConditionCase.ONLY_IF => OnlyIfTemplate(proto.getOnlyIf)
      case ConditionCase.UNLESS => UnlessTemplate(proto.getUnless)
    }
  }
}
