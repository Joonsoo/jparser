package com.giyeok.jparser.proto

import com.giyeok.jparser.fast.GraphNoIndex
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.ParsingContext.{Edge, Node}
import com.giyeok.jparser.nparser.proto.NaiveParserProto
import com.giyeok.jparser.nparser.proto.NaiveParserProto.AcceptCondition.AcceptConditionCase
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
import com.giyeok.jparser.proto.GrammarProto.Empty
import com.giyeok.jparser.proto.ProtoConverterUtil.JavaListToScalaCollection

import scala.jdk.CollectionConverters.{SeqHasAsJava, SetHasAsJava}

object NaiveParserProtobufConverter {
  def convertKernelToProto(kernel: Kernel): NaiveParserProto.Kernel =
    NaiveParserProto.Kernel.newBuilder()
      .setSymbolId(kernel.symbolId).setPointer(kernel.pointer).setBeginGen(kernel.beginGen).setEndGen(kernel.endGen).build()

  def convertProtoToKernel(proto: NaiveParserProto.Kernel): Kernel =
    Kernel(proto.getSymbolId, proto.getPointer, proto.getBeginGen, proto.getEndGen)

  def convertAcceptConditionToProto(acceptCondition: AcceptCondition): NaiveParserProto.AcceptCondition = acceptCondition match {
    case AcceptCondition.Always =>
      NaiveParserProto.AcceptCondition.newBuilder().setAlways(Empty.getDefaultInstance).build()
    case AcceptCondition.Never =>
      NaiveParserProto.AcceptCondition.newBuilder().setNever(Empty.getDefaultInstance).build()
    case AcceptCondition.And(conditions) =>
      NaiveParserProto.AcceptCondition.newBuilder()
        .setAndConditions(NaiveParserProto.AndConditions.newBuilder()
          .addAllConditions(conditions.map(convertAcceptConditionToProto).asJava)).build()
    case AcceptCondition.Or(conditions) =>
      NaiveParserProto.AcceptCondition.newBuilder()
        .setOrConditions(NaiveParserProto.OrConditions.newBuilder()
          .addAllConditions(conditions.map(convertAcceptConditionToProto).asJava)).build()
    case AcceptCondition.NotExists(beginGen, endGen, symbolId) =>
      NaiveParserProto.AcceptCondition.newBuilder()
        .setNotExists(NaiveParserProto.NotExists.newBuilder()
          .setBeginGen(beginGen).setEndGen(endGen).setSymbolId(symbolId)).build()
    case AcceptCondition.Exists(beginGen, endGen, symbolId) =>
      NaiveParserProto.AcceptCondition.newBuilder()
        .setExists(NaiveParserProto.Exists.newBuilder()
          .setBeginGen(beginGen).setEndGen(endGen).setSymbolId(symbolId)).build()
    case AcceptCondition.Unless(beginGen, endGen, symbolId) =>
      NaiveParserProto.AcceptCondition.newBuilder()
        .setUnless(NaiveParserProto.Unless.newBuilder()
          .setBeginGen(beginGen).setEndGen(endGen).setSymbolId(symbolId)).build()
    case AcceptCondition.OnlyIf(beginGen, endGen, symbolId) =>
      NaiveParserProto.AcceptCondition.newBuilder()
        .setOnlyIf(NaiveParserProto.OnlyIf.newBuilder()
          .setBeginGen(beginGen).setEndGen(endGen).setSymbolId(symbolId)).build()
  }

  def convertProtoToAcceptCondition(proto: NaiveParserProto.AcceptCondition): AcceptCondition = proto.getAcceptConditionCase match {
    case AcceptConditionCase.ALWAYS => Always
    case AcceptConditionCase.NEVER => Never
    case AcceptConditionCase.AND_CONDITIONS =>
      And(proto.getAndConditions().getConditionsList.toScalaSet(convertProtoToAcceptCondition))
    case AcceptConditionCase.OR_CONDITIONS =>
      Or(proto.getOrConditions().getConditionsList.toScalaSet(convertProtoToAcceptCondition))
    case AcceptConditionCase.NOT_EXISTS =>
      val notExists = proto.getNotExists
      NotExists(notExists.getBeginGen, notExists.getEndGen, notExists.getSymbolId)
    case AcceptConditionCase.EXISTS =>
      val exists = proto.getExists
      Exists(exists.getBeginGen, exists.getEndGen, exists.getSymbolId)
    case AcceptConditionCase.UNLESS =>
      val unless = proto.getUnless
      Unless(unless.getBeginGen, unless.getEndGen, unless.getSymbolId)
    case AcceptConditionCase.ONLY_IF =>
      val onlyif = proto.getOnlyIf
      OnlyIf(onlyif.getBeginGen, onlyif.getEndGen, onlyif.getSymbolId)
  }

  def convertNodeToProto(node: Node): NaiveParserProto.Node =
    NaiveParserProto.Node.newBuilder()
      .setKernel(convertKernelToProto(node.kernel))
      .setCondition(convertAcceptConditionToProto(node.condition)).build()

  def convertProtoToNode(proto: NaiveParserProto.Node): Node =
    Node(convertProtoToKernel(proto.getKernel), convertProtoToAcceptCondition(proto.getCondition))

  def convertGraphToProto(graph: GraphNoIndex): NaiveParserProto.Graph = {
    val nodes = graph.nodes.toList.sortBy(_.kernel.tuple)
    NaiveParserProto.Graph.newBuilder()
      .addAllNodes(nodes.map(convertNodeToProto).asJava)
      .addAllEdges(graph.edges.map(edge => NaiveParserProto.Graph.Edge.newBuilder()
        .setStartIdx(nodes.indexOf(edge.start))
        .setEndIdx(nodes.indexOf(edge.end))
        .build()).asJava)
      .build()
  }

  def convertProtoToGraph(proto: NaiveParserProto.Graph): GraphNoIndex = {
    val nodes = proto.getNodesList.toScalaList(convertProtoToNode)
    GraphNoIndex(nodes.toSet, proto.getEdgesList.toScalaSet(edge =>
      Edge(nodes(edge.getStartIdx), nodes(edge.getEndIdx))))
  }
}
