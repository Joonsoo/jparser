package com.giyeok.jparser.nparser

import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.nparser.Parser.WrappedContext
import com.giyeok.jparser.nparser.Parser.ProceedDetail
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.EligCondition._
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

object DerivationPreprocessor {
    case class Preprocessed(baseNode: Node, context: Context, baseFinishes: Seq[(Condition, Option[Int])], baseProgresses: Seq[Condition]) {
        def updateContext(newContext: Context) = Preprocessed(baseNode, newContext, baseFinishes, baseProgresses)
        def addBaseFinish(condition: Condition, lastSymbol: Option[Int]) = Preprocessed(baseNode, context, (condition, lastSymbol) +: baseFinishes, baseProgresses)
        def addBaseProgress(condition: Condition) = Preprocessed(baseNode, context, baseFinishes, condition +: baseProgresses)

        def shiftGen(gen: Int): Preprocessed = {
            Preprocessed(baseNode.shiftGen(gen), context.shiftGen(gen), baseFinishes, baseProgresses)
        }
    }
}

class DerivationPreprocessor(val grammar: NGrammar) extends ParsingTasks {
    private val symbolDerivations = scala.collection.mutable.Map[Int, Preprocessed]()
    private val sequenceDerivations = scala.collection.mutable.Map[(Int, Int), Preprocessed]()

    private val symbolTermNodes = scala.collection.mutable.Map[Int, Set[SymbolNode]]()
    private val sequenceTermNodes = scala.collection.mutable.Map[(Int, Int), Set[SymbolNode]]()

    @tailrec final def recNoBase(baseNode: Node, nextGen: Int, tasks: List[Task], cc: Preprocessed): Preprocessed =
        tasks match {
            case FinishTask(`baseNode`, condition, lastSymbol) +: rest =>
                recNoBase(baseNode, nextGen, rest, cc.addBaseFinish(condition, lastSymbol))
            case ProgressTask(`baseNode`, condition) +: rest =>
                recNoBase(baseNode, nextGen, rest, cc.addBaseProgress(condition))
            case task +: rest =>
                val (newContext, newTasks) = process(nextGen, task, cc.context)
                recNoBase(baseNode, nextGen, newTasks ++: rest, cc.updateContext(newContext))
            case List() => cc
        }

    def symbolDerivationOf(symbolId: Int): Preprocessed = {
        symbolDerivations get symbolId match {
            case Some(preprocessed) => preprocessed
            case None =>
                val baseNode = SymbolNode(symbolId, -1)
                val initialPreprocessed = Preprocessed(baseNode, Context(Graph(Set(baseNode), Set()), Results(), Results()), Seq(), Seq())
                val preprocessed = recNoBase(baseNode, 0, List(DeriveTask(baseNode)), initialPreprocessed)
                symbolDerivations(symbolId) = preprocessed
                preprocessed
        }
    }
    def sequenceDerivationOf(sequenceId: Int, pointer: Int): Preprocessed = {
        sequenceDerivations get (sequenceId, pointer) match {
            case Some(baseNodeAndDerivation) => baseNodeAndDerivation
            case None =>
                val baseNode = SequenceNode(sequenceId, pointer, -1, -1)
                val initialPreprocessed = Preprocessed(baseNode, Context(Graph(Set(baseNode), Set()), Results(baseNode -> Set[Condition]()), Results()), Seq(), Seq())
                val preprocessed = recNoBase(baseNode, 0, List(DeriveTask(baseNode)), initialPreprocessed)
                sequenceDerivations((sequenceId, pointer)) = preprocessed
                preprocessed
        }
    }

    def derivationOf(node: Node): Preprocessed = {
        node match {
            case SymbolNode(symbolId, _) => symbolDerivationOf(symbolId)
            case SequenceNode(sequenceId, pointer, _, _) => sequenceDerivationOf(sequenceId, pointer)
        }
    }

    def symbolTermNodesOf(symbolId: Int): Set[SymbolNode] = {
        symbolTermNodes get symbolId match {
            case Some(termNodes) => termNodes
            case None =>
                val termNodes: Set[SymbolNode] = symbolDerivationOf(symbolId).context.graph.nodes collect {
                    case node @ SymbolNode(symbolId, _) if grammar.nsymbols(symbolId).isInstanceOf[NGrammar.Terminal] => node
                }
                symbolTermNodes(symbolId) = termNodes
                termNodes
        }
    }
    def sequenceTermNodesOf(sequenceId: Int, pointer: Int): Set[SymbolNode] = {
        sequenceTermNodes get (sequenceId, pointer) match {
            case Some(termNodes) => termNodes
            case None =>
                val termNodes: Set[SymbolNode] = sequenceDerivationOf(sequenceId, pointer).context.graph.nodes collect {
                    case node @ SymbolNode(symbolId, beginGen) if grammar.nsymbols(symbolId).isInstanceOf[NGrammar.Terminal] => node
                }
                sequenceTermNodes((sequenceId, pointer)) = termNodes
                termNodes
        }
    }
    def termNodesOf(node: Node): Set[SymbolNode] = {
        node match {
            case node @ SymbolNode(symbolId, _) => symbolTermNodesOf(symbolId)
            case node @ SequenceNode(sequenceId, pointer, _, _) => sequenceTermNodesOf(sequenceId, pointer)
        }
    }
}

class PreprocessedParser(val grammar: NGrammar, val derivation: DerivationPreprocessor) extends Parser[DeriveTipsWrappedContext] with ParsingTasks {
    def this(grammar: NGrammar) = this(grammar, new DerivationPreprocessor(grammar))

    assert(grammar == derivation.grammar)

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
        new DeriveTipsWrappedContext(0, finalCtx, Set(startNode), List(), List(), (finalCtx.finishes.conditions map { c => (c -> c) }).toMap)
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

    @tailrec final def recNoDerive(nextGen: Int, tasks: List[Task], context: Context, deriveTips: Set[Node]): (Context, Set[Node]) =
        tasks match {
            case DeriveTask(deriveTip: SequenceNode) +: rest =>
                // context에 deriveTip의 finish task 추가
                val preprocessed = derivation.derivationOf(deriveTip)
                assert(preprocessed.baseFinishes.isEmpty)
                val immediateProgresses = preprocessed.baseProgresses map { condition => ProgressTask(deriveTip, condition.shiftGen(nextGen)) }
                recNoDerive(nextGen, immediateProgresses ++: rest, context.updateFinishes(_.merge(preprocessed.context.finishes.shiftGen(nextGen))), deriveTips + deriveTip)
            case task +: rest =>
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
            Right(UnexpectedInput(input))
        } else {
            // 1. Expand
            // expandingDeriveTips에 있는 것들은 그래프에 expand
            // 있든 없든 finishes는 항상 expand
            val expandedCtx = expand(ctx, expandingDeriveTips)
            // 2. Lift
            val (liftedCtx, newDeriveTips) = recNoDerive(nextGen, termFinishes, expandedCtx.emptyFinishes, Set())
            // 3. Trimming
            // TODO trimStarts에서 (liftedCtx.finishes.conditionNodes) 랑 (liftedCtx.progresses.conditionNodes) 로 충분한지 확인
            val trimStarts = (Set(startNode) ++ (liftedCtx.finishes.conditionNodes) ++ (liftedCtx.progresses.conditionNodes)) intersect liftedCtx.graph.nodes
            val trimmedCtx: Context = trim(liftedCtx, trimStarts, newDeriveTips)
            // 4. Revert
            val revertedCtx: Context = revert(nextGen, trimmedCtx, trimmedCtx.finishes, trimmedCtx.graph.nodes)
            // 5. Condition Fate
            val conditionFateNext = {
                val evaluated = wctx.conditionFate mapValues { _.evaluate(nextGen, trimmedCtx.finishes, trimmedCtx.graph.nodes) }
                val newConditions = (revertedCtx.finishes.conditions map { c => (c -> c) }).toMap
                (evaluated ++ newConditions) filter { _._2 != False }
            }
            val nextDeriveTips = newDeriveTips intersect revertedCtx.graph.nodes // deriveTip 중에 trimStarts에서 도달 불가능하거나 exclude로 제거되는 노드가 있을 수 있음
            val nextCtx = wctx.proceed(nextGen, revertedCtx, nextDeriveTips, input, conditionFateNext)
            Left((ProceedDetail(ctx, expandedCtx, liftedCtx, trimmedCtx, revertedCtx), nextCtx))
        }
    }
}

class SlicedDerivationPreprocessor(grammar: NGrammar) extends DerivationPreprocessor(grammar) {
    // TODO slice가 TermGroupDesc->Preprocessed가 아니고 Preprocessed + finish 혹은 progress task를 만들기 위한 정보가 되어야 할듯
    private val symbolSliced = scala.collection.mutable.Map[Int, Map[TermGroupDesc, Preprocessed]]()
    private val sequenceSliced = scala.collection.mutable.Map[(Int, Int), Map[TermGroupDesc, Preprocessed]]()

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

    def slice(derivation: Preprocessed, termNodes: Set[SymbolNode]): Map[TermGroupDesc, Preprocessed] = {
        val terminals = termNodes map { node => grammar.nsymbols(node.symbolId).asInstanceOf[NGrammar.Terminal].symbol }
        val termGroups = termGroupsOf(terminals)
        (termGroups map { termGroup => (termGroup -> derivation) }).toMap
    }
    def symbolSliceOf(symbolId: Int): Map[TermGroupDesc, Preprocessed] = {
        symbolSliced get symbolId match {
            case Some(slicedMap) => slicedMap
            case None =>
                val slicedMap = slice(symbolDerivationOf(symbolId), symbolTermNodesOf(symbolId))
                symbolSliced(symbolId) = slicedMap
                slicedMap
        }
    }
    def sequenceSliceOf(sequenceId: Int, pointer: Int): Map[TermGroupDesc, Preprocessed] = {
        sequenceSliced get (sequenceId, pointer) match {
            case Some(slicedMap) => slicedMap
            case None =>
                val slicedMap = slice(sequenceDerivationOf(sequenceId, pointer), sequenceTermNodesOf(sequenceId, pointer))
                sequenceSliced((sequenceId, pointer)) = slicedMap
                slicedMap
        }
    }
    def sliceOf(node: Node, input: Input): Option[Preprocessed] = {
        val slicedMap = node match {
            case SymbolNode(symbolId, _) => symbolSliceOf(symbolId)
            case SequenceNode(sequenceId, pointer, _, _) => sequenceSliceOf(sequenceId, pointer)
        }
        assert((slicedMap filter { _._1 contains input }).size <= 1)
        slicedMap find { _._1 contains input } map { _._2 }
    }
}

trait DerivationCompactable {
    // TODO symbol node의 id로 음수를 넣어서 음수는 압축된 여러 symbol node
    def compact(context: Context, baseNode: Node): Context = {
        ???
    }
}

class PreprocessedPrefinishedParser(grammar: NGrammar, derivation: SlicedDerivationPreprocessor) extends PreprocessedParser(grammar, derivation) {
    def this(grammar: NGrammar) = this(grammar, new SlicedDerivationPreprocessor(grammar))

    assert(grammar == derivation.grammar)

    override def proceedDetail(wctx: DeriveTipsWrappedContext, input: Input): Either[(ProceedDetail, DeriveTipsWrappedContext), ParsingError] = {
        // TODO PreprocessedParser 하고 거의 똑같이 하되, expand가 달라지고, terminal node에 대해서가 아니라 deriveTip들에 대한 finish나 progress task를 실행한다
        ???
    }
}
