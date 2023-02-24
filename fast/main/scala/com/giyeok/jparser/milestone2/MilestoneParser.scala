package com.giyeok.jparser.milestone2

import com.giyeok.jparser.{Inputs, Symbols}
import com.giyeok.jparser.ParsingErrors.{ParsingError, UnexpectedInput}
import com.giyeok.jparser.fast.KernelTemplate

import scala.collection.mutable

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
        NotExists(Milestone(symbolId, 0, beginGen), checkFromNextGen = true)
      case OnlyIfTemplate(symbolId) =>
        OnlyIf(Milestone(symbolId, 0, beginGen))
      case UnlessTemplate(symbolId) =>
        Unless(Milestone(symbolId, 0, beginGen))
    }

  def applyParsingAction(path: MilestonePath, gen: Int, action: ParsingAction, actionsCollector: GenActionsBuilder): List[MilestonePath] = {
    val tip = path.tip
    val appended = action.appendingMilestones.map { appending =>
      val newCondition = reifyCondition(appending.acceptCondition, tip.gen, gen)
      val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))
      path.append(Milestone(appending.milestone, gen), condition)
    }
    // apply edge actions to path
    val reduced: List[MilestonePath] = action.startNodeProgressCondition match {
      case Some(startNodeProgressCondition) =>
        path.tipParent match {
          case Some(tipParent) =>
            val newCondition = reifyCondition(startNodeProgressCondition, tip.gen, gen)
            val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))
            val edgeAction = parserData.edgeProgressActions(tipParent.kernelTemplate -> tip.kernelTemplate)
            // TODO add parse action (tipParent -> tip, edgeAction)
            actionsCollector.edgeActions += ((tipParent -> tip, edgeAction))
            applyParsingAction(path.pop(condition), gen, edgeAction.parsingAction, actionsCollector)
          case None => List()
        }
      case None => List()
    }
    appended ++ reduced
  }

  def collectTrackings(paths: List[MilestonePath]): Set[Milestone] =
    paths.flatMap { path =>
      def traverse(tip: Milestone, rest: List[Milestone]): Set[Milestone] =
        rest match {
          case Nil => Set()
          case parent +: next =>
            val action = parserData.edgeProgressActions(parent.kernelTemplate -> tip.kernelTemplate)
            action.requiredSymbols.map(Milestone(_, 0, parent.gen)) ++ traverse(parent, next)
        }

      traverse(path.path.head, path.path.tail)
    }.toSet

  def expectedInputsOf(ctx: ParsingContext): Set[Symbols.Terminal] = {
    // TODO
    Set()
  }

  def evolveAcceptCondition(
    paths: List[MilestonePath],
    genActions: GenActions,
    condition: MilestoneAcceptCondition
  ): MilestoneAcceptCondition = {
    // TODO implement
    condition match {
      case Always => Always
      case Never => Never
      case And(conditions) =>
        MilestoneAcceptCondition.conjunct(conditions.map(evolveAcceptCondition(paths, genActions, _)).toSet)
      case Or(conditions) =>
        MilestoneAcceptCondition.disjunct(conditions.map(evolveAcceptCondition(paths, genActions, _)).toSet)
      case Exists(milestone) => ???
      case NotExists(milestone, true) => NotExists(milestone, false)
      case NotExists(milestone, false) => ???
      case OnlyIf(milestone) => ???
      case Unless(milestone) => ???
    }
  }

  def parseStep(ctx: ParsingContext, input: Inputs.Input): Either[ParsingError, ParsingContext] = {
    val gen = ctx.gen + 1
    val actionsCollector = new GenActionsBuilder()
    val newPaths: List[MilestonePath] = ctx.paths.flatMap { path =>
      val termAction = parserData.termActions(KernelTemplate(path.tip.symbolId, path.tip.pointer))
        .find(_._1.contains(input))
      termAction match {
        case Some((_, action)) =>
          // TODO add parse action (path.tip, action)
          actionsCollector.termActions += ((path.tip, action))
          val fac = action.forAcceptConditions.flatMap { case (first, appendings) =>
            appendings.map { appending =>
              val condition = reifyCondition(appending.acceptCondition, ctx.gen, gen)
              MilestonePath(Milestone(first, ctx.gen)).append(Milestone(appending.milestone, gen), condition)
            }
          }
          applyParsingAction(path, gen, action.parsingAction, actionsCollector) ++ fac
        case None => List()
      }
    }
    if (verbose) {
      newPaths.foreach(path => println(path.prettyString))
    }
    if (!newPaths.exists(_.first == initialMilestone)) {
      // start symbol에 의한 path가 없으면 해당 input이 invalid하다는 뜻
      Left(UnexpectedInput(input, expectedInputsOf(ctx), gen))
    } else {
      val genActions = actionsCollector.build()
      // first가 (start symbol, 0, 0)이거나 현재 존재하는 엣지의 trackingMilestones인 경우만 제외하고 모두 제거
      val trackings = collectTrackings(newPaths)
      if (verbose) {
        println(s"trackings: $trackings")
      }

      // newPaths와 수행된 액션을 바탕으로 condition evaluate
      // TODO 반복이 필요할까?
      val newPathsUpdated = newPaths
        .map(path => path.copy(acceptCondition = evolveAcceptCondition(newPaths, genActions, path.acceptCondition)))
        .filter(_.acceptCondition != Never)

      val newPathsFiltered = newPathsUpdated
        .filter(path => path.first == initialMilestone || trackings.contains(path.first))
      if (verbose) {
        newPathsFiltered.foreach(path => println(path.prettyString))
      }

      if (!newPathsFiltered.exists(_.first == initialMilestone)) {
        Left(UnexpectedInput(input, expectedInputsOf(ctx), gen))
      } else {
        Right(ParsingContext(gen, newPathsFiltered, genActions +: ctx.actionsHistory))
      }
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
          parseStep(currCtx, nextInput)
        case error => error
      }
    }
  }
}

case class ParsingContext(gen: Int, paths: List[MilestonePath], actionsHistory: List[GenActions])

// path는 가장 뒤에 것이 가장 앞에 옴. first는 언제나 path.last와 동일
case class MilestonePath(first: Milestone, path: List[Milestone], acceptCondition: MilestoneAcceptCondition) {
  def prettyString: String = {
    val milestones = path.reverse.map(milestone => s"${milestone.symbolId} ${milestone.pointer} ${milestone.gen}")
    s"${milestones.mkString(" -> ")} ($acceptCondition)"
  }

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

// TODO TermAction하고 EdgeAction에 ID를 붙이는게 좋을까?
case class GenActions(
  termActions: List[(Milestone, TermAction)],
  edgeActions: List[((Milestone, Milestone), EdgeAction)],
)

class GenActionsBuilder {
  val termActions: mutable.ListBuffer[(Milestone, TermAction)] = mutable.ListBuffer()
  val edgeActions: mutable.ListBuffer[((Milestone, Milestone), EdgeAction)] = mutable.ListBuffer()

  def build() = GenActions(termActions.toList, edgeActions.toList)
}
