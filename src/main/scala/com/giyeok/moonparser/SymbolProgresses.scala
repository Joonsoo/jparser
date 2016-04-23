package com.giyeok.moonparser

trait SymbolProgresses {
    this: Parser =>

    import Symbols._
    import Inputs._
    import ParseTree._
    import Kernels._

    sealed trait SymbolProgress {
        val kernel: Kernel
        val derivedGen: Int
        val lastLiftedGen: Option[Int]
        val parsed: Option[ParseNode[Symbol]]

        val id = SymbolProgress.getId(this)

        def toShortString = s"(${kernel.toShortString}, $derivedGen-$lastLiftedGen, ${parsed map { _.toShortString }})"
        override def toString = toShortString
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

        assert(kernel.finishable == parsed.isDefined)

        def derive(grammar: Grammar, gen: Int): (Set[DeriveEdge], Set[Reverter]) = {
            def concretizeEdge(tmpl: EdgeTmpl): DeriveEdge = tmpl match {
                case SimpleEdgeTmpl(start, end) =>
                    SimpleEdge(SymbolProgress(start, gen), SymbolProgress(end, gen))
                case JoinEdgeTmpl(start, end, join, endJoinSwitched) =>
                    JoinEdge(new JoinSymbolProgress(start, gen, None, None), SymbolProgress(end, gen), SymbolProgress(join, gen), endJoinSwitched)
            }
            val (edgeTmpls, reverterTmpls) = kernel.derive(grammar)
            val edges: Set[DeriveEdge] = edgeTmpls map { concretizeEdge _ }
            val reverters: Set[Reverter] = reverterTmpls map {
                _ match {
                    case LiftTriggeredTempLiftBlockTmpl(trigger, target) =>
                        LiftTriggeredTempLiftBlockReverter(SymbolProgress(trigger, gen), SymbolProgress(target, gen))
                    case LiftTriggeredDeriveReverterTmpl(trigger, target) =>
                        LiftTriggeredDeriveReverter(SymbolProgress(trigger, gen), SimpleEdge(SymbolProgress(target.start, gen), SymbolProgress(target.end, gen)))
                    case ReservedLiftTriggeredLiftedNodeReverterTmpl(trigger) =>
                        ReservedLiftTriggeredNodeKillReverter(SymbolProgress(trigger, gen))
                    case ReservedAliveTriggeredLiftedNodeReverterTmpl(trigger) =>
                        ReservedAliveTriggeredNodeKillReverter(SymbolProgress(trigger, gen))
                }
            }
            (edges, reverters)
        }

        def lift(gen: Int, accepted: ParseNode[Symbol]): NonterminalSymbolProgress
    }

    case class AtomicSymbolProgress[T <: AtomicSymbol with Nonterm](kernel: AtomicNontermKernel[T], derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedSymbol[T]]) extends NonterminalSymbolProgress {
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
    }
}
