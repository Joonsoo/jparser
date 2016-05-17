package com.giyeok.jparser.deprecated

import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.Inputs._
import com.giyeok.jparser.ParseTree._
import com.giyeok.jparser.Kernels._
import com.giyeok.jparser.Derivations
import com.giyeok.jparser.Grammar

trait SymbolProgresses {
    this: Parser =>

    sealed trait SymbolProgress {
        val kernel: Kernel
        val derivedGen: Int
        val lastLiftedGen: Option[Int]
        val parsed: Option[ParseNode[Symbol]]

        val id = SymbolProgress.getId(this)

        def toShortString = s"($id, ${kernel.toShortString}, $derivedGen-$lastLiftedGen)"
        override def toString = s"($id, ${kernel.toShortString}, $derivedGen-$lastLiftedGen, ${parsed map { _.toShortString }})"
    }
    object SymbolProgress {
        private var cache: Map[SymbolProgress, Int] = Map()
        private var counter = 0
        def getId(sp: SymbolProgress): Int = {
            cache get sp match {
                case Some(i) => i
                case None =>
                    counter += 1
                    cache += ((sp, counter))
                    counter
            }
        }

        def apply(kernel: NontermKernel[Nonterm], gen: Int): NonterminalSymbolProgress = kernel match {
            case kernel: JoinKernel => new JoinSymbolProgress(kernel, gen, None, None)
            case kernel: AtomicNontermKernel[_] => AtomicSymbolProgress(kernel, gen, None, None)
            case kernel: NonAtomicNontermKernel[_] => NonAtomicSymbolProgress(kernel, gen, None, ParsedSymbolsSeq(kernel.symbol, List(), List()))
        }
        def apply(kernel: Kernel, gen: Int): SymbolProgress = kernel match {
            case EmptyKernel => EmptySymbolProgress(gen)
            case kernel: TerminalKernel => TerminalSymbolProgress(kernel, gen, None, None)
            case kernel: NontermKernel[Nonterm] => SymbolProgress(kernel, gen)
        }
    }

    case class EmptySymbolProgress(derivedGen: Int) extends SymbolProgress {
        val kernel = EmptyKernel
        val lastLiftedGen = Some(derivedGen)
        val parsed = Some(ParsedEmpty(Empty))
    }

    case class TerminalSymbolProgress(kernel: TerminalKernel, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedTerminal]) extends SymbolProgress {
        def proceedTerminal(gen: Int, next: Input): Option[TerminalSymbolProgress] =
            if (kernel.symbol accept next) Some(TerminalSymbolProgress(kernel.lifted, derivedGen, Some(gen), Some(ParsedTerminal(kernel.symbol, next))))
            else None
    }

    trait NonterminalSymbolProgress extends SymbolProgress {
        val kernel: NontermKernel[Nonterm]

        def derive(grammar: Grammar, gen: Int): (Set[DeriveEdge], Set[Reverter]) = {
            import Derivations._
            kernel.derive(grammar) match {
                case EmptyDerivation => (Set[DeriveEdge](), Set[Reverter]())
                case SymbolDerivation(derives) =>
                    (derives map { k => SimpleEdge(this, SymbolProgress(k, gen)) }, Set[Reverter]())
                case JoinDerivation(derive, join) =>
                    assert(this.isInstanceOf[JoinSymbolProgress])
                    val self = this.asInstanceOf[JoinSymbolProgress]
                    (Set(
                        JoinEdge(self, SymbolProgress(derive, gen), SymbolProgress(join, gen), false),
                        JoinEdge(self, SymbolProgress(join, gen), SymbolProgress(derive, gen), true)),
                        Set[Reverter]())
                case TempLiftBlockableDerivation(derive, blockTrigger) =>
                    (Set(SimpleEdge(this, SymbolProgress(derive, gen))),
                        Set(LiftTriggeredTempLiftBlockReverter(SymbolProgress(blockTrigger, gen), this)))
                case RevertableDerivation(derive, revertTrigger) =>
                    val deriveEdge = SimpleEdge(this, SymbolProgress(derive, gen))
                    (Set(deriveEdge),
                        Set(MultiTriggeredDeriveReverter(Set(TriggerIfLift(SymbolProgress(revertTrigger, gen))), deriveEdge)))
                case DeriveRevertableDerivation(derive, deriveRevertTrigger) =>
                    val deriveEdge = SimpleEdge(this, SymbolProgress(derive, gen))
                    val revertableDeriveNode = SymbolProgress(deriveRevertTrigger, gen)
                    (Set(deriveEdge, SimpleEdge(this, revertableDeriveNode)),
                        Set(MultiTriggeredDeriveReverter(Set(TriggerIfLift(revertableDeriveNode)), deriveEdge)))
                case ReservedLiftTriggeredLiftRevertableDerivation(derive) =>
                    (Set(SimpleEdge(this, SymbolProgress(derive, gen))),
                        Set(ReservedLiftTriggeredLiftedNodeReverter(this)))
                case ReservedAliveTriggeredLiftRevertableDerivation(derive) =>
                    (Set(SimpleEdge(this, SymbolProgress(derive, gen))),
                        Set(ReservedAliveTriggeredLiftedNodeReverter(this)))
            }
        }

        def lift(gen: Int, accepted: ParseNode[Symbol]): NonterminalSymbolProgress
    }

    case class AtomicSymbolProgress[T <: AtomicSymbol with Nonterm](kernel: AtomicNontermKernel[T], derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedSymbol[T]]) extends NonterminalSymbolProgress {
        assert(kernel.finishable == parsed.isDefined)

        def lift(gen: Int, accepted: ParseNode[Symbol]) = AtomicSymbolProgress[T](kernel.lifted, derivedGen, Some(gen), Some(ParsedSymbol[T](kernel.symbol, accepted)))
    }

    class JoinSymbolProgress(kernel: JoinKernel, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedSymbolJoin]) extends AtomicSymbolProgress[Join](kernel, derivedGen, lastLiftedGen, parsed) {
        override def lift(gen: Int, accepted: ParseNode[Symbol]) = {
            throw new AssertionError("JoinSymbolProgress.lift should not be called")
        }
        def liftJoin(gen: Int, accepted: ParseNode[Symbol], constraint: ParseNode[Symbol]): JoinSymbolProgress =
            new JoinSymbolProgress(kernel.lifted, derivedGen, Some(gen), Some(new ParsedSymbolJoin(kernel.symbol, accepted, constraint)))
    }

    case class NonAtomicSymbolProgress[T <: NonAtomicSymbol with Nonterm](kernel: NonAtomicNontermKernel[T], derivedGen: Int, lastLiftedGen: Option[Int], _parsed: ParsedSymbolsSeq[T]) extends NonterminalSymbolProgress {
        val parsed = if (kernel.finishable) Some(_parsed) else None

        def lift(gen: Int, accepted: ParseNode[Symbol]) = {
            val (nextKernel, nextParsed) = kernel.lifted(_parsed, accepted)
            NonAtomicSymbolProgress(nextKernel, derivedGen, Some(gen), nextParsed)
        }

        override def toShortString = s"($id, ${kernel.toShortString}, $derivedGen-$lastLiftedGen)"
        override def toString = s"($id, ${kernel.toShortString}, $derivedGen-$lastLiftedGen, ${_parsed.children map { _.toShortString }})"
    }
}
