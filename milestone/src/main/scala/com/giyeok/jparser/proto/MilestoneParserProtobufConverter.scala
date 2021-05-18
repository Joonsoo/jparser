package com.giyeok.jparser.proto

import com.giyeok.jparser.milestone
import com.giyeok.jparser.milestone._
import com.giyeok.jparser.proto.GrammarProtobufConverter.{convertNGrammarToProto, convertProtoToNGrammar}
import com.giyeok.jparser.proto.MilestoneParserDataProto.MilestoneParserData.TermActionPair.TermGroupAction
import com.giyeok.jparser.proto.MilestoneParserDataProto.MilestoneParserData.{DerivedGraphPair, EdgeProgressActionPair, TermActionPair}
import com.giyeok.jparser.proto.MilestoneParserDataProto.ParsingAction.AppendingMilestonePair
import com.giyeok.jparser.proto.MilestoneParserDataProto.ParsingAction.AppendingMilestonePair.Dependent
import com.giyeok.jparser.proto.MilestoneParserDataProto.TasksSummary.ProgressedKernelPair
import com.giyeok.jparser.proto.NaiveParserProtobufConverter._
import com.giyeok.jparser.proto.ProtoConverterUtil.JavaListToScalaCollection
import com.giyeok.jparser.proto.TermGroupProtobufConverter.{convertProtoToTermGroup, convertTermGroupToProto}

import scala.jdk.CollectionConverters.{IterableHasAsJava, SeqHasAsJava}

object MilestoneParserProtobufConverter {
  def convertKernelTemplateToProto(kernelTemplate: KernelTemplate): MilestoneParserDataProto.KernelTemplate =
    MilestoneParserDataProto.KernelTemplate.newBuilder()
      .setSymbolId(kernelTemplate.symbolId)
      .setPointer(kernelTemplate.pointer).build()

  def convertProtoToKernelTemplate(proto: MilestoneParserDataProto.KernelTemplate): KernelTemplate =
    KernelTemplate(proto.getSymbolId, proto.getPointer)

  def convertTasksSummaryToProto(tasksSummary: TasksSummary): MilestoneParserDataProto.TasksSummary =
    MilestoneParserDataProto.TasksSummary.newBuilder()
      .addAllProgressedKernels(tasksSummary.progressedKernels.map(pk =>
        ProgressedKernelPair.newBuilder()
          .setNode(convertNodeToProto(pk._1))
          .setCondition(convertAcceptConditionToProto(pk._2)).build()).asJava)
      .addAllFinishedKernels(tasksSummary.finishedKernels.map(convertNodeToProto).asJava).build()

  def convertProtoToTasksSummary(proto: MilestoneParserDataProto.TasksSummary): TasksSummary =
    TasksSummary(proto.getProgressedKernelsList.toScalaList(pk =>
      convertProtoToNode(pk.getNode) -> convertProtoToAcceptCondition(pk.getCondition)),
      proto.getFinishedKernelsList.toScalaList(convertProtoToNode))

  def convertParsingActionToProto(parsingAction: ParsingAction): MilestoneParserDataProto.ParsingAction =
    MilestoneParserDataProto.ParsingAction.newBuilder()
      .addAllAppendingMilestones(parsingAction.appendingMilestones.map(am =>
        AppendingMilestonePair.newBuilder()
          .setKernelTemplate(convertKernelTemplateToProto(am.milestone))
          .setCondition(convertAcceptConditionToProto(am.acceptCondition))
          .addAllDependents(am.dependents.map { dep =>
            Dependent.newBuilder()
              .setStartMilestone(convertKernelTemplateToProto(dep._1))
              .setEndMilestone(convertKernelTemplateToProto(dep._2))
              .setCondition(convertAcceptConditionToProto(dep._3)).build()
          }.asJava).build()).asJava)
      .setTasksSummary(convertTasksSummaryToProto(parsingAction.tasksSummary))
      .addAllStartNodeProgressConditions(parsingAction.startNodeProgressConditions.map(convertAcceptConditionToProto).asJava)
      .setGraphBetween(convertGraphToProto(parsingAction.graphBetween)).build()

  def convertProtoToParsingAction(proto: MilestoneParserDataProto.ParsingAction): ParsingAction =
    milestone.ParsingAction(
      appendingMilestones = proto.getAppendingMilestonesList.toScalaList(pair =>
        AppendingMilestone(
          convertProtoToKernelTemplate(pair.getKernelTemplate),
          convertProtoToAcceptCondition(pair.getCondition),
          pair.getDependentsList.toScalaList { dependent =>
            (convertProtoToKernelTemplate(dependent.getStartMilestone), convertProtoToKernelTemplate(dependent.getEndMilestone), convertProtoToAcceptCondition(dependent.getCondition))
          })),
      tasksSummary = convertProtoToTasksSummary(proto.getTasksSummary),
      startNodeProgressConditions = proto.getStartNodeProgressConditionsList.toScalaList(convertProtoToAcceptCondition),
      graphBetween = convertProtoToGraph(proto.getGraphBetween))

  def convertMilestoneParserDataToProto(data: MilestoneParserData): MilestoneParserDataProto.MilestoneParserData =
    MilestoneParserDataProto.MilestoneParserData.newBuilder()
      .setGrammar(convertNGrammarToProto(data.grammar))
      .setByStart(convertTasksSummaryToProto(data.byStart))
      .addAllTermActions(data.termActions.toList.map(pair =>
        TermActionPair.newBuilder()
          .setKernelTemplate(convertKernelTemplateToProto(pair._1))
          .addAllActions(pair._2.map(action =>
            TermGroupAction.newBuilder()
              .setTermGroup(convertTermGroupToProto(action._1))
              .setParsingAction(convertParsingActionToProto(action._2)).build()
          ).asJava).build()).asJava)
      .addAllEdgeProgressActions(data.edgeProgressActions.map(pair =>
        EdgeProgressActionPair.newBuilder()
          .setStartKernelTemplate(convertKernelTemplateToProto(pair._1._1))
          .setEndKernelTemplate(convertKernelTemplateToProto(pair._1._2))
          .setParsingAction(convertParsingActionToProto(pair._2)).build()).asJava)
      .addAllDerivedGraphs(data.derivedGraph.map(pair =>
        DerivedGraphPair.newBuilder()
          .setKernelTemplate(convertKernelTemplateToProto(pair._1))
          .setGraph(convertGraphToProto(pair._2))
          .build()).asJava)
      .build()

  def convertProtoToMilestoneParserData(proto: MilestoneParserDataProto.MilestoneParserData): MilestoneParserData =
    milestone.MilestoneParserData(
      grammar = convertProtoToNGrammar(proto.getGrammar),
      byStart = convertProtoToTasksSummary(proto.getByStart),
      termActions = proto.getTermActionsList.toScalaMap(
        pair => convertProtoToKernelTemplate(pair.getKernelTemplate),
        pair => pair.getActionsList.toScalaList { action =>
          convertProtoToTermGroup(action.getTermGroup) -> convertProtoToParsingAction(action.getParsingAction)
        }),
      edgeProgressActions = proto.getEdgeProgressActionsList.toScalaMap(
        pair => convertProtoToKernelTemplate(pair.getStartKernelTemplate) -> convertProtoToKernelTemplate(pair.getEndKernelTemplate),
        pair => convertProtoToParsingAction(pair.getParsingAction)),
      derivedGraph = proto.getDerivedGraphsList.toScalaMap(
        pair => convertProtoToKernelTemplate(pair.getKernelTemplate),
        pair => convertProtoToGraph(pair.getGraph))
    )
}
