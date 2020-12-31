package com.giyeok.jparser.parsergen.mgroup

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.Inputs.{CharacterTermGroupDesc, CharsGroup}
import com.giyeok.jparser.metalang3a.generated.{ArrayExprAst, ExpressionGrammarAst}
import com.giyeok.jparser.parsergen.milestone.{MilestoneParser, MilestoneParserContext, MilestoneParserGen}
import com.giyeok.jparser.utils.JavaCodeGenUtil.javaChar

class MGroupParser(val parser: MilestoneParser) {
  def traverse(queue: List[(MilestoneParserContext, String)]): Unit = queue match {
    case head +: rest =>
      val (ctx: MilestoneParserContext, inputSoFar: String) = head
      val termGroups = ctx.paths.map(path => parser.parserData.termActions(path.tip.kernelTemplate).map {
        _._1.asInstanceOf[CharacterTermGroupDesc]
      })
      println(s"*** Input so far: ${"\""}$inputSoFar${"\""}, termGroups=${termGroups.flatten.map(_.asInstanceOf[CharsGroup].chars.toList.min).map(c => s"'${javaChar(c)}'")}")
      // TODO terms에서 intersect들 분리하기
      var newRest = rest
      termGroups.flatten.map(_.asInstanceOf[CharsGroup].chars.toList.min).sorted.foreach { c =>
        println(s"***** proceeding from ${"\""}$inputSoFar${"\""} + '${javaChar(c)}'")
        val nextCtx = parser.proceed(ctx, Inputs.Character(c))
        nextCtx.paths.foreach(t => println(t.prettyString))
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

object MGroupParser {
  def main(args: Array[String]): Unit = {
    val milestoneParserData = MilestoneParserGen.generateMilestoneParserData(ArrayExprAst.ngrammar)
    val mgroupParser = new MGroupParser(new MilestoneParser(milestoneParserData))
    mgroupParser.test()
  }
}