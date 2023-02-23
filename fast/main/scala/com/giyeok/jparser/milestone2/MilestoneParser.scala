package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ParsingErrors.{ParsingError, UnexpectedInput}
import com.giyeok.jparser.fast.KernelTemplate

class MilestoneParser(val parserData: MilestoneParserData) {
  private var verbose = false

  def setVerbose(): MilestoneParser = {
    verbose = true
    this
  }

  val initialMilestone: Milestone = Milestone(parserData.grammar.startSymbol, 0, 0)

  def initialCtx: ParsingContext =
    ParsingContext(0, List(MilestonePath(initialMilestone)), List())

  def reifyCondition(template: AcceptConditionTemplate, beginGen: Int, gen: Int): MilestoneAcceptCondition =
    template match {
      case AlwaysTemplate => Always
      case NeverTemplate => Never
      case AndTemplate(conditions) =>
        And(conditions.map(reifyCondition(_, beginGen, gen)).distinct)
      case OrTemplate(conditions) =>
        Or(conditions.map(reifyCondition(_, beginGen, gen)).distinct)
      case ExistsTemplate(symbolId) =>
        Exists(Milestone(symbolId, 0, gen))
      case NotExistsTemplate(symbolId) =>
        NotExists(Milestone(symbolId, 0, gen), checkFromNextGen = false)
      case LongestTemplate(symbolId) =>
        NotExists(Milestone(symbolId, 0, beginGen), checkFromNextGen = false)
      case OnlyIfTemplate(symbolId) =>
        OnlyIf(Milestone(symbolId, 0, beginGen))
      case UnlessTemplate(symbolId) =>
        Unless(Milestone(symbolId, 0, beginGen))
    }

  def applyParsingAction(path: MilestonePath, gen: Int, action: ParsingAction): List[MilestonePath] = {
    val tip = path.tip
    val appended = action.appendingMilestones.map { appending =>
      val condition = reifyCondition(appending.acceptCondition, tip.gen, gen)
      path.append(Milestone(appending.milestone, gen), condition)
    }
    // TODO 혹시 다른 path에서 forAcceptConditions의 key에 해당하는 path들을 이미 만든게 있으면 재사용
    val forAcceptConditions = action.forAcceptConditions.flatMap { case (root, nexts) =>
      val parent = Milestone(root.symbolId, root.pointer, tip.gen)
      nexts.map { case AppendingMilestone(nextTemplate, conditionTemplate) =>
        val next = Milestone(nextTemplate.symbolId, nextTemplate.pointer, gen)
        val condition = reifyCondition(conditionTemplate, tip.gen, gen)
        MilestonePath(parent, List(next, parent), condition)
      }
    }
    // apply edge actions to path
    val reduced: List[MilestonePath] = action.startNodeProgressCondition match {
      case Some(startNodeProgressCondition) =>
        path.tipParent match {
          case Some(tipParent) =>
            val edgeAction = parserData.edgeProgressActions(tipParent.kernelTemplate -> tip.kernelTemplate)
            val condition = reifyCondition(startNodeProgressCondition, tipParent.gen, gen)
            applyParsingAction(path.pop(condition), gen, edgeAction)
          case None => List()
        }
      case None => List()
    }
    appended ++ forAcceptConditions ++ reduced
  }

  def parseStep(ctx: ParsingContext, gen: Int, input: Inputs.Input): Either[ParsingError, ParsingContext] = {
    val newPaths: List[MilestonePath] = ctx.paths.flatMap { path =>
      val termAction = parserData.termActions(KernelTemplate(path.tip.symbolId, path.tip.pointer))
        .find(_._1.contains(input))
      termAction match {
        case Some((_, action)) =>
          applyParsingAction(path, gen, action)
        case None => List()
      }
    }
    if (!newPaths.exists(_.first == initialMilestone)) {
      // start symbol에 의한 path가 없으면 해당 input이 invalid하다는 뜻
      val expectedInputs = ???
      Left(UnexpectedInput(input, expectedInputs, gen))
    } else {
      // TODO start symbol에서 시작한 path가 아닌데 다른 path에서 accept condition으로 참조하지 않고 있으면 삭제
      Right(ParsingContext(gen, newPaths, List()))
    }
  }

  def parse(inputSeq: Seq[Inputs.Input]): Either[ParsingError, ParsingContext] = {
    if (verbose) {
      println("=== initial")
      initialCtx.paths.foreach(t => println(s"${t.prettyString}"))
    }
    inputSeq.foldLeft[Either[ParsingError, ParsingContext]](Right(initialCtx)) { (m, nextInput) =>
      m match {
        case Right(currCtx) =>
          if (verbose) {
            println(s"=== ${currCtx.gen} $nextInput ${currCtx.paths.size}")
          }
          parseStep(currCtx, currCtx.gen + 1, nextInput)
        case error => error
      }
    }
  }
}

case class ParsingContext(gen: Int, paths: List[MilestonePath], actionsHistory: List[GenAction])

// path는 가장 뒤에 것이 가장 앞에 옴. first는 언제나 path.last와 동일
case class MilestonePath(first: Milestone, path: List[Milestone], acceptCondition: MilestoneAcceptCondition) {
  def prettyString: String = s"${path.reverse} ($acceptCondition)"

  def tip: Milestone = path.head

  def tipParent: Option[Milestone] = path.drop(1).headOption

  def append(newTip: Milestone, newAcceptCondition: MilestoneAcceptCondition): MilestonePath =
    MilestonePath(first, newTip +: path, newAcceptCondition)

  def pop(newAcceptCondition: MilestoneAcceptCondition): MilestonePath =
    MilestonePath(first, path.drop(1), newAcceptCondition)
}

object MilestonePath {
  def apply(milestone: Milestone): MilestonePath =
    MilestonePath(milestone, List(milestone), Always)
}

case class Milestone(symbolId: Int, pointer: Int, gen: Int) {
  def kernelTemplate = KernelTemplate(symbolId, pointer)
}

object Milestone {
  def apply(template: KernelTemplate, gen: Int): Milestone =
    Milestone(template.symbolId, template.pointer, gen)
}

sealed class MilestoneAcceptCondition

case object Always extends MilestoneAcceptCondition

case object Never extends MilestoneAcceptCondition

case class And(conditions: List[MilestoneAcceptCondition]) extends MilestoneAcceptCondition

case class Or(conditions: List[MilestoneAcceptCondition]) extends MilestoneAcceptCondition

case class Exists(milestone: Milestone) extends MilestoneAcceptCondition

case class NotExists(milestone: Milestone, checkFromNextGen: Boolean) extends MilestoneAcceptCondition

case class OnlyIf(milestone: Milestone) extends MilestoneAcceptCondition

case class Unless(milestone: Milestone) extends MilestoneAcceptCondition

// TODO
case class GenAction()
