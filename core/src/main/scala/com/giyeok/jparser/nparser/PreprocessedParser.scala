package com.giyeok.jparser.nparser

import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.nparser.Parser.WrappedContext
import com.giyeok.jparser.nparser.Parser.ProceedDetail
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.Parser.DeriveTipsWrappedContext
import com.giyeok.jparser.Symbols.Terminal
import com.giyeok.jparser.Symbols.Terminals.CharacterTerminal
import com.giyeok.jparser.Symbols.Terminals.VirtualTerminal
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.Inputs.VirtualTermGroupDesc
import DerivationPreprocessor.Preprocessed
import scala.annotation.tailrec
import com.giyeok.jparser.nparser.Parser.ConditionFate

object DerivationPreprocessor {
    case class Preprocessed(baseNode: Node, context: Context, baseFinishes: Seq[(AcceptCondition, Option[Int])], baseProgresses: Seq[AcceptCondition]) {
        // assert(context.graph.nodes contains baseNode)
        def updateContext(newContext: Context) = Preprocessed(baseNode, newContext, baseFinishes, baseProgresses)
        def addBaseFinish(condition: AcceptCondition, lastSymbol: Option[Int]) = Preprocessed(baseNode, context, (condition, lastSymbol) +: baseFinishes, baseProgresses)
        def addBaseProgress(condition: AcceptCondition) = Preprocessed(baseNode, context, baseFinishes, condition +: baseProgresses)

        def shiftGen(gen: Int): Preprocessed = {
            Preprocessed(baseNode.shiftGen(gen), context.shiftGen(gen), baseFinishes, baseProgresses)
        }
    }
}

trait DerivationPreprocessor {
    val grammar: NGrammar

    def symbolDerivationOf(symbolId: Int): Preprocessed
    def sequenceDerivationOf(sequenceId: Int, pointer: Int): Preprocessed

    def derivationOf(node: Node): Preprocessed = {
        node match {
            case SymbolNode(symbolId, _) => symbolDerivationOf(symbolId)
            case SequenceNode(sequenceId, pointer, _, _) => sequenceDerivationOf(sequenceId, pointer)
        }
    }

    def symbolTermNodesOf(symbolId: Int): Set[SymbolNode]
    def sequenceTermNodesOf(sequenceId: Int, pointer: Int): Set[SymbolNode]

    def termNodesOf(node: Node): Set[SymbolNode] = {
        node match {
            case node @ SymbolNode(symbolId, _) => symbolTermNodesOf(symbolId)
            case node @ SequenceNode(sequenceId, pointer, _, _) => sequenceTermNodesOf(sequenceId, pointer)
        }
    }
}

trait SlicedDerivationPreprocessor extends DerivationPreprocessor {
    def termGroupsOf(terminals: Set[Terminal]): Set[TermGroupDesc] = {
        val charTerms: Set[CharacterTermGroupDesc] = terminals collect { case x: CharacterTerminal => TermGroupDesc.descOf(x) }
        val virtTerms: Set[VirtualTermGroupDesc] = terminals collect { case x: VirtualTerminal => TermGroupDesc.descOf(x) }

        def sliceTermGroups(termGroups: Set[CharacterTermGroupDesc]): Set[CharacterTermGroupDesc] = {
            val charIntersects: Set[CharacterTermGroupDesc] = termGroups flatMap { term1 =>
                termGroups collect {
                    case term2 if term1 != term2 => term1 intersect term2
                } filterNot { _.isEmpty }
            }
            val essentials = (termGroups map { g => charIntersects.foldLeft(g) { _ - _ } }) filterNot { _.isEmpty }
            val intersections = if (charIntersects.isEmpty) Set() else sliceTermGroups(charIntersects)
            essentials ++ intersections
        }
        val charTermGroups = sliceTermGroups(charTerms)

        val virtIntersects: Set[VirtualTermGroupDesc] = virtTerms flatMap { term1 =>
            virtTerms collect {
                case term2 if term1 != term2 => term1 intersect term2
            } filterNot { _.isEmpty }
        }
        val virtTermGroups = (virtTerms map { term =>
            virtIntersects.foldLeft(term) { _ - _ }
        }) ++ virtIntersects

        (charTermGroups ++ virtTermGroups) filterNot { _.isEmpty }
    }

    // slice는 Preprocessed와 Preprocessed.context.graph에 포함된 새 derivable node들의 셋으로 구성된다
    def symbolSliceOf(symbolId: Int): Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])]
    def sequenceSliceOf(sequenceId: Int, pointer: Int): Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])]

    def sliceOf(node: Node, input: Input): Option[(Preprocessed, Set[SequenceNode])] = {
        val slicedMap = node match {
            case SymbolNode(symbolId, _) => symbolSliceOf(symbolId)
            case SequenceNode(sequenceId, pointer, _, _) => sequenceSliceOf(sequenceId, pointer)
        }
        // assert((slicedMap filter { _._1 contains input }).size <= 1)
        slicedMap find { _._1 contains input } map { _._2 }
    }
}

class PreprocessedParser(val grammar: NGrammar, val derivation: DerivationPreprocessor) extends Parser[DeriveTipsWrappedContext] with ParsingTasks {
    // assert(grammar == derivation.grammar)

    val initialContext: DeriveTipsWrappedContext = {
        // derivationOf(startNode).baseFinishes 처리
        val preprocessed = derivation.derivationOf(startNode)
        assert(preprocessed.baseProgresses.isEmpty)

        // preprocessed에 있는 finishes에서 baseNode만 startNode로 치환 - 어차피 gen은 0이므로 shiftGen은 따로 하지 않아도 된다
        val initialFinishes = preprocessed.context.finishes.replaceNode(preprocessed.baseNode, startNode)

        // baseFinishTasks는 finishes에 항목만 추가하고 끝남
        val baseFinishTasks = preprocessed.baseFinishes.toList map { cs => FinishTask(startNode, cs._1, cs._2) }

        val ctx = rec(0, baseFinishTasks, Context(Graph(Set(startNode), Set()), Results(), initialFinishes))
        val finalCtx = revert(0, ctx, ctx.finishes, ctx.graph.nodes)
        new DeriveTipsWrappedContext(0, finalCtx, Set(startNode), List(), List(), ConditionFate((finalCtx.finishes.conditions map { c => (c -> c) }).toMap))
    }

    def expand(ctx: Context, expanding: Map[Node, Preprocessed]): Context = {
        expanding.foldLeft(ctx) { (cc, kv) =>
            val (deriveTip, preprocessed) = kv
            def replaceNode(context: Context, original: Node, replaced: Node): Context =
                original match {
                    case original: SequenceNode =>
                        Context(context.graph.replaceNode(original, replaced), context.progresses.replaceNode(original, replaced.asInstanceOf[SequenceNode]), context.finishes.replaceNode(original, replaced))
                    case _ =>
                        Context(context.graph.replaceNode(original, replaced), context.progresses, context.finishes.replaceNode(original, replaced))
                }
            cc.merge(replaceNode(preprocessed.context, preprocessed.baseNode, deriveTip))
        }
    }

    @tailrec final def recNoDerive(nextGen: Int, tasks: List[Task], context: Context, deriveTips: Set[SequenceNode]): (Context, Set[SequenceNode]) =
        tasks match {
            case DeriveTask(deriveTip: SequenceNode) +: rest =>
                // context에 deriveTip의 finish task 추가
                val preprocessed = derivation.derivationOf(deriveTip)
                assert(preprocessed.baseFinishes.isEmpty)
                val immediateProgresses = preprocessed.baseProgresses map { condition => ProgressTask(deriveTip, condition.shiftGen(nextGen)) }
                recNoDerive(nextGen, immediateProgresses ++: rest, context.updateFinishes(_.merge(preprocessed.context.finishes.shiftGen(nextGen))), deriveTips + deriveTip)
            case task +: rest =>
                // assert(!task.isInstanceOf[DeriveTask])
                val (newContext, newTasks) = process(nextGen, task, context)
                recNoDerive(nextGen, newTasks ++: rest, newContext, deriveTips)
            case List() => (context, deriveTips)
        }

    def proceedDetail(wctx: DeriveTipsWrappedContext, input: Input): Either[(ProceedDetail, DeriveTipsWrappedContext), ParsingError] = {
        val (ctx, gen, nextGen, deriveTips) = (wctx.ctx, wctx.gen, wctx.nextGen, wctx.deriveTips)
        // finishable term node를 포함한 deriveTip -> term node set
        val (termFinishes, expandingDeriveTips) = deriveTips.foldLeft((List[Task](), Map[Node, Preprocessed]())) { (cc, tip) =>
            val (tasks, expanding) = cc

            val termNodes = derivation.termNodesOf(tip) filter { node =>
                grammar.nsymbols(node.symbolId).asInstanceOf[NGrammar.Terminal].symbol accept input
            }
            if (termNodes.isEmpty) cc
            else ((termNodes map { node => FinishTask(node.shiftGen(gen), True, None) }) ++: tasks,
                expanding + (tip -> derivation.derivationOf(tip).shiftGen(gen)))
        }
        if (termFinishes.isEmpty) {
            Right(UnexpectedInput(input, nextGen))
        } else {
            // 1. Expand
            // expandingDeriveTips에 있는 것들은 그래프에 expand
            // 있든 없든 finishes는 항상 expand
            val expandedCtx = expand(ctx, expandingDeriveTips)
            // 2. Lift
            val (liftedCtx, newDeriveTips0) = recNoDerive(nextGen, termFinishes, expandedCtx.emptyFinishes, Set())
            val newDeriveTips = newDeriveTips0.asInstanceOf[Set[Node]]
            // 3. Trimming
            // TODO trimStarts에서 (liftedCtx.finishes.conditionNodes) 랑 (liftedCtx.progresses.conditionNodes) 로 충분한지 확인
            val trimStarts = (Set(startNode) ++ (liftedCtx.finishes.conditionNodes) ++ (liftedCtx.progresses.conditionNodes)) intersect liftedCtx.graph.nodes
            val trimmedCtx: Context = trim(liftedCtx, trimStarts, newDeriveTips)
            // 4. Revert
            val revertedCtx: Context = revert(nextGen, trimmedCtx, trimmedCtx.finishes, trimmedCtx.graph.nodes)
            // 5. Condition Fate
            val conditionFateNext = {
                val evaluated = wctx.conditionFate.unfixed map { kv => kv._1 -> kv._2.evaluate(nextGen, trimmedCtx.finishes, trimmedCtx.graph.nodes) }
                val newConditions = (revertedCtx.finishes.conditions map { c => (c -> c) }).toMap
                (evaluated ++ newConditions) // filter { _._2 != False }
            }
            val nextDeriveTips = newDeriveTips intersect revertedCtx.graph.nodes // deriveTip 중에 trimStarts에서 도달 불가능하거나 exclude로 제거되는 노드가 있을 수 있음
            val nextCtx = wctx.proceed(nextGen, revertedCtx, nextDeriveTips, input, conditionFateNext)
            Left((ProceedDetail(ctx, expandedCtx, liftedCtx, trimmedCtx, revertedCtx), nextCtx))
        }
    }
}

class SlicedPreprocessedParser(grammar: NGrammar, override val derivation: SlicedDerivationPreprocessor) extends PreprocessedParser(grammar, derivation) {
    // assert(grammar == derivation.grammar)

    override def proceedDetail(wctx: DeriveTipsWrappedContext, input: Input): Either[(ProceedDetail, DeriveTipsWrappedContext), ParsingError] = {
        val (ctx, gen, nextGen, deriveTips) = (wctx.ctx, wctx.gen, wctx.nextGen, wctx.deriveTips)
        // finishable term node를 포함한 deriveTip -> term node set
        val (initialFinishes, expandingDeriveTips) = deriveTips.foldLeft((List[Task](), Map[Node, Preprocessed]())) { (cc, tip) =>
            derivation.sliceOf(tip, input) match {
                case Some((preprocessed0, newDeriveTips0)) =>
                    val (tasks, expanding) = cc
                    val (preprocessed, newDeriveTips) = (preprocessed0.shiftGen(gen), newDeriveTips0 map { _.shiftGen(gen) })
                    val newTasks: List[Task] =
                        (preprocessed.baseFinishes.toList map { cs => FinishTask(tip, cs._1.shiftGen(gen), cs._2) }) ++
                            (preprocessed.baseProgresses.toList map { c => ProgressTask(tip.asInstanceOf[SequenceNode], c.shiftGen(gen)) }) ++
                            (newDeriveTips.toList map { DeriveTask(_) })
                    (newTasks ++: tasks, expanding + (tip -> preprocessed))
                case None => cc
            }
        }
        if (initialFinishes.isEmpty) {
            Right(UnexpectedInput(input, nextGen))
        } else {
            // 1. Expand
            // expandingDeriveTips에 있는 것들은 그래프에 expand
            // 있든 없든 finishes는 항상 expand
            val expandedCtx = expand(ctx.emptyFinishes, expandingDeriveTips)
            // 2. Lift
            val (liftedCtx, newDeriveTips0) = recNoDerive(nextGen, initialFinishes, expandedCtx, Set())
            val newDeriveTips = newDeriveTips0.asInstanceOf[Set[Node]]
            // 3. Trimming
            // TODO trimStarts에서 (liftedCtx.finishes.conditionNodes) 랑 (liftedCtx.progresses.conditionNodes) 로 충분한지 확인
            val trimStarts = (Set(startNode) ++ (liftedCtx.finishes.conditionNodes) ++ (liftedCtx.progresses.conditionNodes)) intersect liftedCtx.graph.nodes
            val trimmedCtx: Context = trim(liftedCtx, trimStarts, newDeriveTips)
            // 4. Revert
            val revertedCtx: Context = revert(nextGen, trimmedCtx, trimmedCtx.finishes, trimmedCtx.graph.nodes)
            // 5. Condition Fate
            val conditionFateNext = {
                val evaluated = wctx.conditionFate.unfixed map { kv => kv._1 -> kv._2.evaluate(nextGen, trimmedCtx.finishes, trimmedCtx.graph.nodes) }
                val newConditions = (revertedCtx.finishes.conditions map { c => (c -> c) }).toMap
                (evaluated ++ newConditions) // filter { _._2 != False }
            }
            val nextDeriveTips = newDeriveTips intersect revertedCtx.graph.nodes // deriveTip 중에 trimStarts에서 도달 불가능하거나 exclude로 제거되는 노드가 있을 수 있음
            val nextCtx = wctx.proceed(nextGen, revertedCtx, nextDeriveTips, input, conditionFateNext)
            Left((ProceedDetail(ctx, expandedCtx, liftedCtx, trimmedCtx, revertedCtx), nextCtx))
        }
    }
}
