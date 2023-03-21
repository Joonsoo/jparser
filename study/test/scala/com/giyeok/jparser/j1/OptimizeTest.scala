package com.giyeok.jparser.j1

import com.giyeok.jparser.proto.MilestoneParserDataProto
import org.scalatest.flatspec.AnyFlatSpec

import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream
import scala.jdk.CollectionConverters.ListHasAsScala

class OptimizeTest extends AnyFlatSpec {
  it should "work" in {
    val milestoneParserData = MilestoneParserDataProto.MilestoneParserData.parseFrom(
      new GZIPInputStream(new BufferedInputStream(getClass.getResourceAsStream("/j1_parserdata.pb.gz"))))
    println(s"Total: ${milestoneParserData.getSerializedSize}")
    println(s"grammar: ${milestoneParserData.getGrammar.getSerializedSize}")
    println(s"byStart: ${milestoneParserData.getByStart.getSerializedSize}")
    println(s"term actions: ${milestoneParserData.getTermActionsCount}")
    println(milestoneParserData.getTermActionsList.asScala.map(_.getSerializedSize).sum)
    println(s"edge progress actions: ${milestoneParserData.getEdgeProgressActionsCount}")
    println(milestoneParserData.getEdgeProgressActionsList.asScala.map(_.getSerializedSize).sum)
    println(milestoneParserData.getEdgeProgressActionsList.asScala.map(_.getParsingAction.getTasksSummary.getSerializedSize).sum)
    println(milestoneParserData.getEdgeProgressActionsList.asScala.map(_.getParsingAction.getStartNodeProgressConditionsList.asScala.map(_.getSerializedSize).sum).sum)
    println(milestoneParserData.getEdgeProgressActionsList.asScala.map(_.getParsingAction.getGraphBetween.getSerializedSize).sum)
    println(s"derived graphs: ${milestoneParserData.getDerivedGraphsCount}")
    println(milestoneParserData.getDerivedGraphsList.asScala.map(_.getSerializedSize).sum)

    val parsed = J1Grammar.parseAst(
      """
        |package abc.def.ghi;
        |
        |class Hello {
        |  public static void main(String[] args) {
        |    System.out.println("Hello!");
        |  }
        |}
        |""".stripMargin)
    println(parsed)
  }
}
