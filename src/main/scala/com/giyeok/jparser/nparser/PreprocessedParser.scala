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
                val baseNode = SymbolNode(symbolId, 0)
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

    def termNodesOf(node: Node): Option[Set[SymbolNode]] = {
        node match {
            case SymbolNode(symbolId, beginGen) =>
                ???
            case SequenceNode(sequenceId, pointer, _, endGen) =>
                ???
        }
    }
    def finishableTermNodesOf(node: Node, input: Input): Option[Set[SymbolNode]] = {
        termNodesOf(node) map {
            _ filter { node =>
                grammar.nsymbols(node.symbolId).asInstanceOf[NGrammar.Terminal].symbol accept input
            }
        }
    }
}

class PrefinishedDerivationPreprocessor(grammar: NGrammar) extends DerivationPreprocessor(grammar) {
    def derivationOf(node: Node, input: Input): Context = {
        ???
    }
}

trait DerivationCompactable {
    // TODO symbol node의 id로 음수를 넣어서 음수는 압축된 여러 symbol node
    def compact(context: Context, baseNode: Node): Context = {
        ???
    }
}

class PreprocessedParser(val grammar: NGrammar, val derivation: DerivationPreprocessor) extends Parser[DeriveTipsWrappedContext] with ParsingTasks {
    assert(grammar == derivation.grammar)

    val initialContext: DeriveTipsWrappedContext = {
        val ctx = Context(Graph(Set(startNode), Set()), Results(), derivation.derivationOf(startNode)._2.finishes)
        new DeriveTipsWrappedContext(0, ctx, Set(startNode), List(), List(), Map())
    }

    def expand(ctx: Context, nodeAndDerivation: Set[(Node, (Node, Context))]): Context = {
        // TODO nodeAndDerivation에서 _1를 _2로 매핑시키고 그래프 전체를 ctx에 복사해 넣어 반환한다
        ???
    }

    def recNoDerive(nextGen: Int, tasks: List[Task], context: Context, deriveTips: Set[Node]): (Context, Set[Node]) =
        tasks match {
            case DeriveTask(deriveTip) +: rest =>
                // TODO context에 derivation.derivationOf(deriveTip).finishes 추가
                recNoDerive(nextGen, rest, context, deriveTips + deriveTip)
            case task +: rest =>
                val (newContext, newTasks) = process(nextGen, task, context)
                recNoDerive(nextGen, newTasks ++: rest, newContext, deriveTips)
            case List() => (context, deriveTips)
        }

    def proceedDetail(wctx: DeriveTipsWrappedContext, input: Input): Either[(ProceedDetail, DeriveTipsWrappedContext), ParsingError] = {
        val (ctx, gen, nextGen) = (wctx.ctx, wctx.gen, wctx.nextGen)
        val terms = wctx.deriveTips flatMap { tip => derivation.finishableTermNodesOf(tip, input) map { tip -> _ } }
        if (terms.isEmpty) {
            Right(UnexpectedInput(input))
        } else {
            // 1. Expand
            val expandedCtx = expand(ctx, terms map { kv => (kv._1, derivation.derivationOf(kv._1)) })
            val termFinishes = (terms flatMap { _._2 }).toList map { FinishTask(_, True, None) }
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

class PreprocessedPreliftedParser(grammar: NGrammar, derivation: DerivationPreprocessor) extends PreprocessedParser(grammar, derivation) {
    assert(grammar == derivation.grammar)

    override def proceedDetail(wctx: DeriveTipsWrappedContext, input: Input): Either[(ProceedDetail, DeriveTipsWrappedContext), ParsingError] = {
        // TODO PreprocessedParser 하고 거의 똑같이 하되, expand가 달라지고, terminal node에 대해서가 아니라 deriveTip들에 대한 finish나 progress task를 실행한다
        ???
    }
}
