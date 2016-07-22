package com.giyeok.jparser.nparser

import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.nparser.Parser.WrappedContext
import com.giyeok.jparser.nparser.Parser.ProceedDetail
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.EligCondition._
import com.giyeok.jparser.nparser.Parser.DeriveTipsWrappedContext
import com.giyeok.jparser.Symbols.Terminal

class DerivationPreprocessor(val grammar: NGrammar) extends ParsingTasks {
    private val symbolDerivations = scala.collection.mutable.Map[Int, (SymbolNode, Context)]()
    private val sequenceDerivations = scala.collection.mutable.Map[(Int, Int), (SequenceNode, Context)]()

    private val symbolTermNodes = scala.collection.mutable.Map[Int, Set[SymbolNode]]()
    private val sequenceTermNodes = scala.collection.mutable.Map[(Int, Int), Set[SymbolNode]]()

    // derivationOf, finishalbeTerms, derivationOf에서 반환하는 내용은 모두 적당히 shiftGen이 된 상태로 준다

    def symbolDerivationOf(symbolId: Int): (SymbolNode, Context) = {
        symbolDerivations get symbolId match {
            case Some(baseNodeAndDerivation) => baseNodeAndDerivation
            case None =>
                val baseNode = SymbolNode(symbolId, -1)
                val derivation = rec(0, List(DeriveTask(baseNode)), Context(Graph(Set(baseNode), Set(), Map(), Map()), Results(), Results()))
                symbolDerivations(symbolId) = (baseNode, derivation)
                (baseNode, derivation)
        }
    }
    def sequenceDerivationOf(sequenceId: Int, pointer: Int): (SequenceNode, Context) = {
        sequenceDerivations get (sequenceId, pointer) match {
            case Some(baseNodeAndDerivation) => baseNodeAndDerivation
            case None =>
                val baseNode = SequenceNode(sequenceId, 0, -1, -1)
                val derivation = rec(0, List(DeriveTask(baseNode)), Context(Graph(Set(baseNode), Set(), Map(), Map()), Results(), Results()))
                sequenceDerivations((sequenceId, pointer)) = (baseNode, derivation)
                (baseNode, derivation)
        }
    }

    def derivationOf(node: Node): (Node, Context) = {
        node match {
            case SymbolNode(symbolId, beginGen) =>
                val (baseNode, derivation) = symbolDerivationOf(symbolId)
                (baseNode.shiftGen(beginGen), derivation.shiftGen(beginGen))
            case SequenceNode(sequenceId, pointer, _, endGen) =>
                val (baseNode, derivation) = sequenceDerivationOf(sequenceId, pointer)
                (baseNode.shiftGen(endGen), derivation.shiftGen(endGen))
        }
    }

    def termNodesOf(node: Node): Set[SymbolNode] = {
        node match {
            case SymbolNode(symbolId, beginGen) =>
                symbolTermNodes get symbolId match {
                    case Some(termNodes) => termNodes map { _.shiftGen(beginGen) }
                    case None =>
                        val termNodes: Set[SymbolNode] = symbolDerivationOf(symbolId)._2.graph.nodes collect {
                            case node @ SymbolNode(symbolId, beginGen) if grammar.nsymbols(symbolId).isInstanceOf[NGrammar.Terminal] => node
                        }
                        symbolTermNodes(symbolId) = termNodes
                        termNodes map { _.shiftGen(beginGen) }
                }
            case SequenceNode(sequenceId, pointer, _, endGen) =>
                sequenceTermNodes get (sequenceId, pointer) match {
                    case Some(termNodes) => termNodes map { _.shiftGen(endGen) }
                    case None =>
                        val termNodes: Set[SymbolNode] = sequenceDerivationOf(sequenceId, pointer)._2.graph.nodes collect {
                            case node @ SymbolNode(symbolId, beginGen) if grammar.nsymbols(symbolId).isInstanceOf[NGrammar.Terminal] => node
                        }
                        sequenceTermNodes((sequenceId, pointer)) = termNodes
                        termNodes map { _.shiftGen(endGen) }
                }
        }
    }
    def finishableTermNodesOf(node: Node, input: Input): Set[SymbolNode] =
        termNodesOf(node) filter { node => grammar.nsymbols(node.symbolId).asInstanceOf[NGrammar.Terminal].symbol accept input }
}

class PreprocessedParser(val grammar: NGrammar, val derivation: DerivationPreprocessor) extends Parser[DeriveTipsWrappedContext] with ParsingTasks {
    def this(grammar: NGrammar) = this(grammar, new DerivationPreprocessor(grammar))

    assert(grammar == derivation.grammar)

    val initialContext: DeriveTipsWrappedContext = {
        val ctx = Context(Graph(Set(startNode), Set()), Results(), derivation.derivationOf(startNode)._2.finishes)
        new DeriveTipsWrappedContext(0, ctx, Set(startNode), List(), List(), Map())
    }

    def expand(ctx: Context, nodeAndDerivation: Set[(Node, (Node, Context))]): Context = {
        nodeAndDerivation.foldLeft(ctx) { (cc, kv) =>
            val (deriveTip, (baseNode, derivation)) = kv
            // derivation에서 baseNode를 deriveTip으로 replace해서 ctx에 추가
            cc.merge(derivation.replaceNode(baseNode, deriveTip))
        }
    }

    def recNoDerive(nextGen: Int, tasks: List[Task], context: Context, deriveTips: Set[Node]): (Context, Set[Node]) =
        tasks match {
            case DeriveTask(deriveTip: SequenceNode) +: rest =>
                // context에 deriveTip의 finish task 추가
                val (baseNode, preprocessed) = derivation.derivationOf(deriveTip)
                val immediateFinishes = preprocessed.finishes.of(baseNode).getOrElse(Set()) map { ProgressTask(deriveTip, _) }
                recNoDerive(nextGen, immediateFinishes ++: rest, context, deriveTips + deriveTip)
            case task +: rest =>
                val (newContext, newTasks) = process(nextGen, task, context)
                recNoDerive(nextGen, newTasks ++: rest, newContext, deriveTips)
            case List() => (context, deriveTips)
        }

    def proceedDetail(wctx: DeriveTipsWrappedContext, input: Input): Either[(ProceedDetail, DeriveTipsWrappedContext), ParsingError] = {
        val (ctx, gen, nextGen) = (wctx.ctx, wctx.gen, wctx.nextGen)
        val terms: Set[(Node, Set[SymbolNode])] = wctx.deriveTips map { tip => tip -> derivation.finishableTermNodesOf(tip, input) }
        val termFinishes = (terms flatMap { _._2 }).toList map { FinishTask(_, True, None) }
        if (termFinishes.isEmpty) {
            Right(UnexpectedInput(input))
        } else {
            // 1. Expand
            val expandedCtx = expand(ctx, terms map { kv => (kv._1, derivation.derivationOf(kv._1)) })
            // 2. Lift
            val (liftedCtx, newDeriveTips) = recNoDerive(nextGen, termFinishes, ctx.emptyFinishes, Set())
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
            val nextCtx = wctx.proceed(nextGen, revertedCtx, newDeriveTips, input, conditionFateNext)
            Left((ProceedDetail(ctx, expandedCtx, liftedCtx, trimmedCtx, revertedCtx), nextCtx))
        }
    }
}

trait DerivationCompactable {
    // TODO symbol node의 id로 음수를 넣어서 음수는 압축된 여러 symbol node
    def compact(context: Context, baseNode: Node): Context = {
        ???
    }
}

class PrefinishedDerivationPreprocessor(grammar: NGrammar) extends DerivationPreprocessor(grammar) {
    def derivationOf(node: Node, input: Input): Context = {
        ???
    }
}

class PreprocessedPrefinishedParser(grammar: NGrammar, derivation: PrefinishedDerivationPreprocessor) extends PreprocessedParser(grammar, derivation) {
    def this(grammar: NGrammar) = this(grammar, new PrefinishedDerivationPreprocessor(grammar))

    assert(grammar == derivation.grammar)

    override def proceedDetail(wctx: DeriveTipsWrappedContext, input: Input): Either[(ProceedDetail, DeriveTipsWrappedContext), ParsingError] = {
        // TODO PreprocessedParser 하고 거의 똑같이 하되, expand가 달라지고, terminal node에 대해서가 아니라 deriveTip들에 대한 finish나 progress task를 실행한다
        ???
    }
}
