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
    private val derivations = scala.collection.mutable.Map[Node, Context]()

    // derivationOf, finishalbeTerms, derivationOf에서 반환하는 내용은 모두 적당히 shiftGen이 된 상태로 준다

    def derivationOf(node: Node): Context = {
        derivations get node match {
            case Some(derivation) => derivation
            case None =>
                ???
        }
    }

    def finishableTerms(node: Node): Option[Set[SymbolNode]] = {
        ???
    }

    def derivationOf(node: Node, input: Input): Context = {
        ???
    }
}

class PreprocessedParser(val grammar: NGrammar, val derivation: DerivationPreprocessor) extends Parser[DeriveTipsWrappedContext] with ParsingTasks {
    assert(grammar == derivation.grammar)

    val initialContext: DeriveTipsWrappedContext = {
        val ctx = Context(Graph(Set(startNode), Set()), Results(), derivation.derivationOf(startNode).finishes)
        new DeriveTipsWrappedContext(0, ctx, Set(startNode), List(), List(), Map())
    }

    def expand(ctx: Context, nodeAndDerivation: Set[(Node, Context)]): Context = {
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
        val terms = wctx.deriveTips flatMap { tip => derivation.finishableTerms(tip) map { tip -> _ } }
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
        ???
    }
}
