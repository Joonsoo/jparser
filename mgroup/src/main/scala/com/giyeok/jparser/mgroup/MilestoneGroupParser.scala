package com.giyeok.jparser.mgroup

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.Inputs.{CharacterTermGroupDesc, CharsGroup}
import com.giyeok.jparser.metalang3a.generated.ArrayExprAst
import com.giyeok.jparser.milestone._
import com.giyeok.jparser.utils.JavaCodeGenUtil.javaChar

class MilestoneGroupParser(val parser: MilestoneParser) {

  case class MilestoneGroup(commonAncestor: Milestone, children: List[MilestonePath], dependentPaths: List[MilestonePath])

  def commonAncestor(a: Milestone, b: Milestone): Milestone =
    if (a == b) a
    else if (a.gen < b.gen) commonAncestor(a, b.parent.get)
    else commonAncestor(a.parent.get, b)

  def groupMilestonePaths(paths: List[MilestonePath]): MilestoneGroup = {
    val (canonPaths, depPaths) = paths.partition(_.trackerId.isEmpty)
    val (head, tail) = (canonPaths.head, canonPaths.tail)
    val common = tail.foldLeft(head.tip) { (m, i) => commonAncestor(m, i.tip) }
    MilestoneGroup(common, canonPaths, depPaths)
  }

  def printContext(ctx: MilestoneParserContext): Unit = {
    if (ctx.paths.nonEmpty) {
      val group = groupMilestonePaths(ctx.paths.filter(_.trackerId.isEmpty))
      println(s"Common: ${group.commonAncestor.prettyString}")
      ctx.paths.foreach(t => println(t.prettyString))
    }
  }

  def traverse(queue: List[(MilestoneParserContext, String)]): Unit = queue match {
    case head +: rest =>
      val (ctx: MilestoneParserContext, inputSoFar: String) = head
      val termGroups = ctx.paths.map(path => parser.parserData.termActions(path.tip.kernelTemplate).map {
        _._1.asInstanceOf[CharacterTermGroupDesc]
      })
      println(s"*** Input so far: '$inputSoFar', termGroups=${termGroups.flatten.map(_.asInstanceOf[CharsGroup].chars.toList.min).map(c => s"'${javaChar(c)}'")}")
      printContext(ctx)
      // TODO terms에서 intersect들 분리하기
      var newRest = rest
      termGroups.flatten.map(_.asInstanceOf[CharsGroup].chars.toList.min).sorted.foreach { c =>
        println(s"***** proceeding from '$inputSoFar' + '${javaChar(c)}' => '$inputSoFar${javaChar(c)}'")
        val nextCtx = parser.proceed(ctx, Inputs.Character(c)).left.get
        printContext(nextCtx)
        if (nextCtx.gen < 5) {
          newRest :+= (nextCtx, inputSoFar + c)
        }
      }
      traverse(newRest)
    case Nil =>
  }

  def test(): Unit = {
    traverse(List((parser.initialCtx, "")))
  }
}

object MilestoneGroupParser {
  def main(args: Array[String]): Unit = {
    val milestoneParserData = MilestoneParserGen.generateMilestoneParserData(ArrayExprAst.ngrammar)
    val mgroupParser = new MilestoneGroupParser(new MilestoneParser(milestoneParserData))
    mgroupParser.test()
  }
}
