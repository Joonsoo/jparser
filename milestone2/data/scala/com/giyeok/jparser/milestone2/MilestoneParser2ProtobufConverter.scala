package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase
import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.proto.GrammarProtobufConverter.{convertNGrammarToProto, convertProtoToNGrammar}
import com.giyeok.jparser.proto.ProtoConverterUtil.{JavaListToScalaCollection, toScalaIntList}
import com.giyeok.jparser.proto.TermGroupProtobufConverter.{convertProtoToTermGroup, convertTermGroupToProto}
import com.google.protobuf.Empty

import scala.math.Ordering.comparatorToOrdering

object MilestoneParser2ProtobufConverter {
  implicit val kernelTemplateEdgeOrdering: Ordering[(KernelTemplate, KernelTemplate)] = comparatorToOrdering {
    (o1: (KernelTemplate, KernelTemplate), o2: (KernelTemplate, KernelTemplate)) =>
      if (o1._1 != o2._1) o1._1.compareTo(o2._1) else o1._2.compareTo(o2._2)
  }

  implicit val termGroupOrdering: Ordering[TermGroupDesc] = comparatorToOrdering {
    (o1: TermGroupDesc, o2: TermGroupDesc) =>
      o1.toShortString.compareTo(o2.toShortString)
  }

  def toProto(parserData: MilestoneParserData): MilestoneParserDataProto.Milestone2ParserData = {
    val builder = MilestoneParserDataProto.Milestone2ParserData.newBuilder()
      .setGrammar(convertNGrammarToProto(parserData.grammar))
      .setInitialTasksSummary(toProto(parserData.initialTasksSummary))

    // parserData.termActions 순서 고정
    parserData.termActions.toList.sortBy(_._1).foreach { termAction =>
      val termActionBuilder = builder.addTermActionsBuilder()
      termActionBuilder.setKernelTemplate(toProto(termAction._1))
      // termAction._2 순서 고정
      termAction._2.sortBy(p => p._1).foreach { action =>
        val actionBuilder = termActionBuilder.addActionsBuilder()
        actionBuilder
          .setTermGroup(convertTermGroupToProto(action._1))
          .setParsingAction(toProto(action._2.parsingAction))
        // action._2.pendedAcceptConditionKernels 순서 고정
        action._2.pendedAcceptConditionKernels.toList.sortBy(_._1).foreach { pair =>
          val pendedBuilder = actionBuilder.addPendedAcceptConditionKernelsBuilder()
          pendedBuilder.setKernelTemplate(toProto(pair._1))
          // pair._2._1 순서 고정
          pair._2._1.sortBy(_.milestone).foreach { appending =>
            pendedBuilder.addAppendingMilestones(toProto(appending))
          }
          pair._2._2.foreach { firstKernelProgressCondition =>
            pendedBuilder.setFirstKernelProgressCondition(toProto(firstKernelProgressCondition))
          }
        }
      }
    }

    // parserData.edgeProgressActions 순서 고정
    parserData.edgeProgressActions.toList.sortBy(_._1).foreach { edgeProgressAction =>
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
    // tasksSummary.addedKernels 순서 고정
    tasksSummary.addedKernels.toList.sortBy(_._1).foreach { pair =>
      val pairBuilder = builder.addAddedKernelsBuilder()
      pairBuilder.setAcceptCondition(toProto(pair._1))
      pair._2.foreach { kernel =>
        pairBuilder.addKernels(toProto(kernel))
      }
    }
    // tasksSummary.progressedKernels 순서 고정
    tasksSummary.progressedKernels.toList.sorted.foreach { kernel =>
      builder.addProgressedKernels(toProto(kernel))
    }
    //    tasksSummary.progressedStartKernel.foreach { kernel =>
    //      builder.setProgressedStartKernel(convertKernelToProto(kernel))
    //    }
    builder.build()
  }

  def toProto(kernel: Kernel): MilestoneParserDataProto.Kernel =
    MilestoneParserDataProto.Kernel.newBuilder()
      .setSymbolId(kernel.symbolId)
      .setPointer(kernel.pointer)
      .setBeginGen(kernel.beginGen)
      .setEndGen(kernel.endGen)
      .build()

  def toProto(kernelTemplate: KernelTemplate): MilestoneParserDataProto.KernelTemplate =
    MilestoneParserDataProto.KernelTemplate.newBuilder()
      .setSymbolId(kernelTemplate.symbolId)
      .setPointer(kernelTemplate.pointer)
      .build()

  def toProto(parsingAction: ParsingAction): MilestoneParserDataProto.ParsingAction2 = {
    val builder = MilestoneParserDataProto.ParsingAction2.newBuilder()
    // parsingAction.appendingMilestones 순서 고정
    parsingAction.appendingMilestones.sortBy(_.milestone).foreach { appending =>
      builder.addAppendingMilestones(toProto(appending))
    }
    parsingAction.startNodeProgressCondition.foreach { startProgress =>
      builder.setStartNodeProgressCondition(toProto(startProgress))
    }
    // parsingAction.lookaheadRequiringSymbols 순서 고정
    parsingAction.lookaheadRequiringSymbols.toList.sorted.foreach { symbolId =>
      builder.addLookaheadRequiringSymbolIds(symbolId)
    }
    builder.setTasksSummary(toProto(parsingAction.tasksSummary))
    builder.build()
  }

  def toProto(appendingMilestone: AppendingMilestone): MilestoneParserDataProto.AppendingMilestone2 = {
    MilestoneParserDataProto.AppendingMilestone2.newBuilder()
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
        // conditions는 항상 정렬되어 있음
        conditions.foreach { cond =>
          builder.getAndBuilder().addConditions(toProto(cond))
        }
      case OrTemplate(conditions) =>
        // conditions는 항상 정렬되어 있음
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
      case _ => throw new AssertionError("")
    }
    assert(builder.getConditionCase != ConditionCase.CONDITION_NOT_SET)
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
        { pair => pair.getKernelsList.toScalaSet(fromProto) }
      ),
      proto.getProgressedKernelsList.toScalaSet(fromProto),
      // if (proto.hasProgressedStartKernel) Some(convertProtoToKernel(proto.getProgressedStartKernel)) else None,
    )
  }

  def fromProto(proto: MilestoneParserDataProto.Kernel): Kernel = {
    Kernel(proto.getSymbolId, proto.getPointer, proto.getBeginGen, proto.getEndGen)
  }

  def fromProto(kernelTemplate: MilestoneParserDataProto.KernelTemplate): KernelTemplate = {
    KernelTemplate(kernelTemplate.getSymbolId, kernelTemplate.getPointer)
  }

  def fromProto(proto: MilestoneParserDataProto.Milestone2ParserData.TermActionPair.TermGroupAction): (TermGroupDesc, TermAction) = {
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

  def fromProto(proto: MilestoneParserDataProto.AppendingMilestone2): AppendingMilestone = {
    AppendingMilestone(
      fromProto(proto.getMilestone),
      fromProto(proto.getAcceptCondition)
    )
  }

  def fromProto(proto: MilestoneParserDataProto.ParsingAction2): ParsingAction = {
    ParsingAction(
      proto.getAppendingMilestonesList.toScalaList(fromProto),
      if (proto.hasStartNodeProgressCondition) Some(fromProto(proto.getStartNodeProgressCondition)) else None,
      proto.getLookaheadRequiringSymbolIdsList.toSet,
      fromProto(proto.getTasksSummary)
    )
  }

  def fromProto(proto: MilestoneParserDataProto.AcceptConditionTemplate): AcceptConditionTemplate = {
    proto.getConditionCase match {
      case MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.ALWAYS => AlwaysTemplate
      case MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.NEVER => NeverTemplate
      case MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.AND =>
        AndTemplate(proto.getAnd.getConditionsList.toScalaList(fromProto))
      case MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.OR =>
        OrTemplate(proto.getOr.getConditionsList.toScalaList(fromProto))
      case MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.LOOKAHEAD_IS =>
        LookaheadIsTemplate(proto.getLookaheadIs.getSymbolId, proto.getLookaheadIs.getFromNextGen)
      case MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.LOOKAHEAD_NOT =>
        LookaheadNotTemplate(proto.getLookaheadNot.getSymbolId, proto.getLookaheadNot.getFromNextGen)
      case MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.LONGEST =>
        LongestTemplate(proto.getLongest)
      case MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.ONLY_IF =>
        OnlyIfTemplate(proto.getOnlyIf)
      case MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.UNLESS =>
        UnlessTemplate(proto.getUnless)
    }
  }
}
