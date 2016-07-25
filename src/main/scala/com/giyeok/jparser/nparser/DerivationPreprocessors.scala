package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.DerivationPreprocessor.Preprocessed
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.EligCondition.Condition
import com.giyeok.jparser.nparser.EligCondition.True
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.Inputs.Input
import scala.annotation.tailrec

class OnDemandDerivationPreprocessor(val grammar: NGrammar) extends DerivationPreprocessor with ParsingTasks {
    private val symbolDerivations = scala.collection.mutable.Map[Int, Preprocessed]()
    private val sequenceDerivations = scala.collection.mutable.Map[(Int, Int), Preprocessed]()

    private val symbolTermNodes = scala.collection.mutable.Map[Int, Set[SymbolNode]]()
    private val sequenceTermNodes = scala.collection.mutable.Map[(Int, Int), Set[SymbolNode]]()

    @tailrec private def recNoBase(baseNode: Node, nextGen: Int, tasks: List[Task], cc: Preprocessed): Preprocessed =
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
}

class OnDemandSlicedDerivationPreprocessor(grammar: NGrammar) extends OnDemandDerivationPreprocessor(grammar) with SlicedDerivationPreprocessor {
    // slice는 TermGroupDesc -> Preprocessed + new derive tips, finish나 progress는 baseNode에 대해서만 수행한다
    private val symbolSliced = scala.collection.mutable.Map[Int, Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])]]()
    private val sequenceSliced = scala.collection.mutable.Map[(Int, Int), Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])]]()

    @tailrec private def recNoBaseNoDerive(baseNode: Node, nextGen: Int, tasks: List[Task], cc: Preprocessed, deriveTips: Set[SequenceNode]): (Preprocessed, Set[SequenceNode]) =
        tasks match {
            case DeriveTask(deriveTip: SequenceNode) +: rest =>
                // context에 deriveTip의 finish task 추가
                val preprocessed = derivationOf(deriveTip)
                assert(preprocessed.baseFinishes.isEmpty)
                val immediateProgresses = preprocessed.baseProgresses map { condition => ProgressTask(deriveTip, condition.shiftGen(nextGen)) }
                val ncc = cc.updateContext(cc.context.updateFinishes(_.merge(preprocessed.context.finishes.shiftGen(nextGen))))
                recNoBaseNoDerive(baseNode, nextGen, immediateProgresses ++: rest, ncc, deriveTips + deriveTip)
            case FinishTask(`baseNode`, condition, lastSymbol) +: rest =>
                recNoBaseNoDerive(baseNode, nextGen, rest, cc.addBaseFinish(condition, lastSymbol), deriveTips)
            case ProgressTask(`baseNode`, condition) +: rest =>
                recNoBaseNoDerive(baseNode, nextGen, rest, cc.addBaseProgress(condition), deriveTips)
            case task +: rest =>
                assert(!task.isInstanceOf[DeriveTask])
                val (newContext, newTasks) = process(nextGen, task, cc.context)
                recNoBaseNoDerive(baseNode, nextGen, newTasks ++: rest, cc.updateContext(newContext), deriveTips)
            case List() =>
                (cc, deriveTips)
        }

    private def slice(derivation: Preprocessed, termNodes: Set[SymbolNode]): Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])] = {
        val terminals = termNodes map { node => grammar.nsymbols(node.symbolId).asInstanceOf[NGrammar.Terminal].symbol }
        val termGroups = termGroupsOf(terminals)
        (termGroups map { termGroup =>
            val finishables = finishableTermNodes(derivation.context, 0, termGroup)
            val finishTasks = finishables.toList map { FinishTask(_, True, None) }
            val cc = Preprocessed(derivation.baseNode, derivation.context.emptyFinishes, Seq(), Seq())
            val (newPreprocessed, newDeriveTips) = recNoBaseNoDerive(derivation.baseNode, 1, finishTasks, cc, Set())
            val trimStarts = (Set(derivation.baseNode)) ++ (derivation.context.finishes.conditionNodes) ++ (derivation.context.progresses.conditionNodes)
            val trimmedContext = trim(newPreprocessed.context, trimStarts, newDeriveTips.asInstanceOf[Set[Node]])
            val sequenceNodes = trimmedContext.graph.nodes collect { case n: SequenceNode => n }
            val sliced = (newPreprocessed.updateContext(trimmedContext), newDeriveTips intersect sequenceNodes)
            (termGroup -> sliced)
        }).toMap
    }
    def symbolSliceOf(symbolId: Int): Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])] = {
        symbolSliced get symbolId match {
            case Some(slicedMap) => slicedMap
            case None =>
                val slicedMap = slice(symbolDerivationOf(symbolId), symbolTermNodesOf(symbolId))
                symbolSliced(symbolId) = slicedMap
                slicedMap
        }
    }
    def sequenceSliceOf(sequenceId: Int, pointer: Int): Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])] = {
        sequenceSliced get (sequenceId, pointer) match {
            case Some(slicedMap) => slicedMap
            case None =>
                val slicedMap = slice(sequenceDerivationOf(sequenceId, pointer), sequenceTermNodesOf(sequenceId, pointer))
                sequenceSliced((sequenceId, pointer)) = slicedMap
                slicedMap
        }
    }
}
