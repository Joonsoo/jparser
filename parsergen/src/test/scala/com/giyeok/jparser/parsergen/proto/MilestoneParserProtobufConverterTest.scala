package com.giyeok.jparser.parsergen.proto

import com.giyeok.jparser.parsergen.milestone.MilestoneParserData
import com.giyeok.jparser.proto.ProtoConverterUtil.JavaListToScalaCollection
import com.google.protobuf.CodedInputStream
import org.scalatest.flatspec.AnyFlatSpec

import java.io.FileInputStream

class MilestoneParserProtobufConverterTest extends AnyFlatSpec {
  "Big data" should "be read fine" in {
    val protoData = MilestoneParserDataProto.MilestoneParserData.parseFrom(CodedInputStream.newInstance(
      new FileInputStream("C:\\Users\\Joonsoo\\workspace\\javazero\\src\\main\\resources\\javaparserdata.pb")))
    println("TermActions: " + protoData.getTermActionsCount)
    println("TermActionsCount.actionsCount" + protoData.getTermActionsList.toScalaList(x => x.getActionsCount))
    println("TermActionsCount.actionsCountSum" + protoData.getTermActionsList.toScalaList(x => x.getActionsCount).sum)
    println("EdgeProgressActions: " + protoData.getEdgeProgressActionsCount)
    println("DerivedGraphs: " + protoData.getDerivedGraphsCount)
    println()
    val milestoneParserData: MilestoneParserData =
      MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(protoData)
    println(milestoneParserData.grammar.nsymbols.size)
  }
}
