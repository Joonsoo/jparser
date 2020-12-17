package com.giyeok.jparser.parsergen.try2

import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.metalang3a.generated.ArrayExprAst
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always}
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser.ParsingContext.Kernel
import com.giyeok.jparser.nparser.{AcceptCondition, ParseTreeConstructor2}
import com.giyeok.jparser.parsergen.try2.Try2.{KernelTemplate, PrecomputedParserData, TasksSummary}
import com.giyeok.jparser.{Inputs, NGrammar, ParseForest, ParseForestFunc}

object Try2Parser {
  def reconstructParseTree(grammar: NGrammar, finalCtx: Try2ParserContext, input: Seq[Input]): Option[ParseForest] = {
    val kernels = finalCtx.actionsHistory.map(gen => Kernels(gen.flatMap {
      case TermAction(beginGen, midGen, endGen, summary) =>
        def genOf(gen: Int) = gen match {
          case 0 => beginGen
          case 1 => midGen
          case 2 => endGen
        }

        (summary.finishedKernels ++ summary.progressedKernels.map(_._1)).map(_.kernel).map { kernel =>
          Kernel(kernel.symbolId, kernel.pointer, genOf(kernel.beginGen), genOf(kernel.endGen))
        }
      case EdgeAction(parentBeginGen, beginGen, midGen, endGen, summary, condition) =>
        // TODO accept condition으로 필터링
        def genOf(gen: Int) = gen match {
          case 0 => parentBeginGen
          case 1 => beginGen
          case 2 => midGen
          case 3 => endGen
        }

        (summary.finishedKernels ++ summary.progressedKernels.map(_._1)).map(_.kernel).map { kernel =>
          Kernel(kernel.symbolId, kernel.pointer, genOf(kernel.beginGen), genOf(kernel.endGen))
        }
    }.toSet))
    new ParseTreeConstructor2(ParseForestFunc)(grammar)(input, kernels).reconstruct()
  }

  def main(args: Array[String]): Unit = {
    //    val parserData = Try2.precomputedParserData(ExpressionGrammar.ngrammar)
    //    new Try2Parser(parserData).parse("1*2+34")
    val grammar = ArrayExprAst.ngrammar
    val valuefier = ArrayExprAst.matchStart _
    val input = Inputs.fromString("[a,a,a]")

    val parserData = Try2.precomputedParserData(grammar)
    val finalCtx = new Try2Parser(parserData).parse(input)
    val parseTree = reconstructParseTree(grammar, finalCtx, input).get.trees.head
    val ast = valuefier(parseTree)
    println(ast)
  }
}

class Try2Parser(val parserData: PrecomputedParserData) {
  def initialCtx: Try2ParserContext = Try2ParserContext(
    List(Milestone(None, parserData.grammar.startSymbol, 0, 0, Always)),
    List(List(TermAction(0, 0, 0, parserData.byStart))))

  def parse(inputSeq: Seq[Inputs.Input]): Try2ParserContext = {
    println("=== initial")
    initialCtx.tips.foreach(t => println(t.prettyString))
    inputSeq.zipWithIndex.foldLeft(initialCtx) { (m, i) =>
      val (nextInput, gen0) = i
      val gen = gen0 + 1
      val next = proceed(m, gen, nextInput)
      println(s"=== $gen $nextInput")
      next.tips.foreach(t => println(t.prettyString))
      next
    }
  }

  def parse(input: String): Try2ParserContext = parse(Inputs.fromString(input))

  private class ProceedProcessor(var genActions: List[GenAction] = List()) {
    def proceed(ctx: Try2ParserContext, gen: Int, input: Inputs.Input): List[Milestone] = ctx.tips.flatMap { tip =>
      val parentGen = tip.parent.map(_.gen).getOrElse(0)
      val termActions = parserData.termActions(tip.kernelTemplate)
      termActions.find(_._1.contains(input)) match {
        case Some((_, action)) =>
          genActions +:= TermAction(parentGen, tip.gen, gen, action.tasksSummary)
          // action.appendingMilestones를 뒤에 덧붙인다
          val appended = action.appendingMilestones.map { appending =>
            val (kernelTemplate, acceptCondition) = appending
            Milestone(Some(tip), kernelTemplate.symbolId, kernelTemplate.pointer, gen,
              AcceptCondition.conjunct(tip.acceptCondition, acceptCondition))
          }
          // action.startNodeProgressConditions가 비어있지 않으면 tip을 progress 시킨다
          val reduced = progressTip(tip, gen, action.startNodeProgressConditions)
          appended ++ reduced
        case None => List()
      }
    }

    private def progressTip(tip: Milestone, gen: Int, acceptConditions: List[AcceptCondition]): List[Milestone] =
      acceptConditions.flatMap { condition =>
        // (tip.parent-tip) 사이의 엣지에 대한 edge action 실행
        tip.parent match {
          case Some(parent) =>
            val edgeAction = parserData.edgeProgressActions((parent.kernelTemplate, tip.kernelTemplate))
            genActions +:= EdgeAction(parent.parent.map(_.gen).getOrElse(0), parent.gen, tip.gen, gen, edgeAction.tasksSummary, condition)
            // tip은 지워지고 tip.parent - edgeAction.appendingMilestones 가 추가됨
            val appended = edgeAction.appendingMilestones.map(appending =>
              Milestone(Some(parent), appending._1.symbolId, appending._1.pointer, gen,
                AcceptCondition.conjunct(tip.acceptCondition, appending._2, condition))
            )
            // edgeAction.startNodeProgressConditions에 대해 위 과정 반복 수행
            val propagated = progressTip(parent, gen, edgeAction.startNodeProgressConditions)
            appended ++ propagated
          case None =>
            // 파싱 종료
            // TODO 어떻게 처리하지?
            List()
        }
      }
  }

  def proceed(ctx: Try2ParserContext, gen: Int, input: Inputs.Input): Try2ParserContext = {
    val processor = new ProceedProcessor()
    val milestones = processor.proceed(ctx, gen, input)
    Try2ParserContext(milestones, ctx.actionsHistory :+ processor.genActions)
  }
}

case class Milestone(parent: Option[Milestone], symbolId: Int, pointer: Int, gen: Int, acceptCondition: AcceptCondition) {
  def kernelTemplate: KernelTemplate = KernelTemplate(symbolId, pointer)

  private def myself = s"($symbolId $pointer $gen ${acceptCondition})"

  def prettyString: String = parent match {
    case Some(value) => s"${value.prettyString} $myself"
    case None => myself
  }
}

sealed trait GenAction

case class TermAction(beginGen: Int, midGen: Int, endGen: Int, summary: TasksSummary) extends GenAction

case class EdgeAction(parentBeginGen: Int, beginGen: Int, midGen: Int, endGen: Int, summary: TasksSummary, condition: AcceptCondition) extends GenAction

// TODO edge action - 체인 관계를 어떻게..?

case class Try2ParserContext(tips: List[Milestone], actionsHistory: List[List[GenAction]])
