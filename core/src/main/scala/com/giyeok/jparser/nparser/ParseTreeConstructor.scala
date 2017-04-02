package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.ParseResult
import com.giyeok.jparser.ParseResultFunc
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.Symbols.Symbol
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.nparser.NGrammar._
import com.giyeok.jparser.nparser.Parser.ConditionAccumulate

class ParseTreeConstructor[R <: ParseResult](resultFunc: ParseResultFunc[R])(grammar: NGrammar)(input: Seq[Input], val history: Seq[Graph], conditionFate: ConditionAccumulate) {
    sealed trait KernelEdge { val start: Kernel }
    case class SimpleKernelEdge(start: Kernel, end: Kernel) extends KernelEdge
    case class JoinKernelEdge(start: Kernel, end: Kernel, join: Kernel) extends KernelEdge

    case class KernelGraph(nodes: Set[Kernel], edges: Set[KernelEdge]) {
        val edgesByStart: Map[Kernel, Set[KernelEdge]] = edges groupBy { _.start }
    }

    val finishes: Vector[KernelGraph] = {
        (history map { graph =>
            val filteredGraph = graph filterNode { node => conditionFate.of(node.condition) }
            val kernelNodes: Set[Kernel] = filteredGraph.nodes map { _.kernel }
            val kernelEdges: Set[KernelEdge] = filteredGraph.edges map {
                case SimpleEdge(start, end) => SimpleKernelEdge(start.kernel, end.kernel)
                case JoinEdge(start, end, join) => JoinKernelEdge(start.kernel, end.kernel, join.kernel)
            }
            KernelGraph(kernelNodes, kernelEdges)
        }).toVector
    }
    // TODO finishes의 node set을 symbolId 기준으로 정렬해 놓으면 더 빠르게 할 수 있을듯

    def reconstruct(): Option[R] = {
        reconstruct(Kernel(grammar.startSymbol, 0, 0, 0)(grammar.nsymbols(grammar.startSymbol)), input.length)
    }
    def reconstruct(kernel: Kernel, gen: Int): Option[R] = {
        if (finishes(gen).nodes contains kernel) Some(reconstruct(kernel, gen, Set())) else None
    }

    private def reconstruct(kernel: Kernel, gen: Int, traces: Set[(Int, Int)]): R = {
        println("reconstruct", kernel, gen, traces)
        assert(finishes(gen).nodes contains kernel)
        if (kernel.endGen != gen) {
            println("???")
        }
        assert(kernel.endGen == gen)

        def reconstruct0(child: Kernel, childGen: Int): R = {
            val newTraces: Set[(Int, Int)] =
                if ((kernel.beginGen, gen) != (child.beginGen, childGen)) Set()
                else traces + ((kernel.symbolId, kernel.pointer))
            reconstruct(child, childGen, newTraces)
        }

        kernel.symbol match {
            case symbol if traces contains ((kernel.symbolId, kernel.pointer)) =>
                println("cyclicBind?")
                resultFunc.cyclicBind(kernel.beginGen, gen, symbol.symbol)

            case Sequence(symbol, sequence) =>
                if (kernel.pointer == 0) {
                    assert(kernel.beginGen == kernel.endGen)
                    resultFunc.sequence(kernel.beginGen, kernel.endGen, symbol)
                } else {
                    val (symbolId, prevPointer) = (kernel.symbolId, kernel.pointer - 1)
                    val prevKernels = finishes(gen).nodes filter { kern =>
                        (kern.symbolId == symbolId) && (kern.pointer == prevPointer) && (kern.beginGen == kernel.beginGen)
                    }
                    val trees = prevKernels flatMap { prevKernel =>
                        val childKernel = Kernel(sequence(prevPointer), 1, prevKernel.endGen, gen)(grammar.nsymbols(sequence(prevPointer)))
                        if (finishes(gen).nodes contains childKernel) {
                            val precedingTree = reconstruct0(Kernel(kernel.symbolId, prevPointer, kernel.beginGen, prevKernel.endGen)(kernel.symbol), prevKernel.endGen)
                            val childTree = reconstruct0(childKernel, gen)
                            println(s"preceding: $precedingTree")
                            println(s"child: $childTree")
                            Some(resultFunc.append(precedingTree, childTree))
                        } else None
                    }
                    resultFunc.merge(trees)
                }

            case Join(symbol, body, join) =>
                assert(kernel.pointer == 1)
                val bodyKernel = Kernel(body, 1, kernel.beginGen, kernel.endGen)(grammar.nsymbols(body))
                val joinKernel = Kernel(join, 1, kernel.beginGen, kernel.endGen)(grammar.nsymbols(join))
                val bodyTree = reconstruct0(bodyKernel, kernel.endGen)
                val joinTree = reconstruct0(joinKernel, kernel.endGen)
                resultFunc.join(kernel.beginGen, kernel.endGen, symbol, bodyTree, joinTree)

            case Terminal(symbol) =>
                resultFunc.bind(kernel.beginGen, kernel.endGen, symbol,
                    resultFunc.terminal(kernel.beginGen, input(kernel.beginGen)))

            case symbol: NAtomicSymbol =>
                assert(kernel.pointer == 1)
                val prevKernel = Kernel(kernel.symbolId, 0, kernel.beginGen, kernel.beginGen)(kernel.symbol)
                assert(finishes(gen).edgesByStart(prevKernel) forall { _.isInstanceOf[SimpleKernelEdge] })
                val bodyKernels = finishes(gen).edgesByStart(prevKernel) collect {
                    case SimpleKernelEdge(_, end) if end.endGen == gen && end.isFinished => end
                }
                val bodyTrees = bodyKernels map { bodyKernel =>
                    reconstruct0(bodyKernel, kernel.endGen)
                }
                resultFunc.bind(kernel.beginGen, kernel.endGen, symbol.symbol, resultFunc.merge(bodyTrees))
        }
        //        def reconstruct0(child: Node, childGen: Int): R = {
        //            val newTraces = if ((node.beginGen, gen) == (child.beginGen, childGen)) (traces + node.symbolId) else Set[Int]()
        //            reconstruct(child, childGen, newTraces)
        //        }
        //
        //        node match {
        //            case SymbolKernel(symbolId, beginGen) if traces contains symbolId =>
        //                resultFunc.cyclicBind(beginGen, gen, grammar.nsymbols(symbolId).symbol)
        //            case SymbolKernel(symbolId, beginGen) =>
        //                grammar.nsymbols(symbolId) match {
        //                    case Terminal(terminalSymbol) =>
        //                        resultFunc.bind(beginGen, gen, terminalSymbol, resultFunc.terminal(beginGen, input(beginGen)))
        //                    case symbol: NSimpleDerivable =>
        //                        val merging = finishes(gen) filter { child =>
        //                            (symbol.produces contains child.symbolId) && (beginGen == child.beginGen)
        //                        } flatMap {
        //                            case child: SymbolKernel =>
        //                                Some(resultFunc.bind(beginGen, gen, symbol.symbol, reconstruct0(child, gen)))
        //                            case child: SequenceKernel =>
        //                                val sequenceSymbol = grammar.nsequences(child.symbolId)
        //                                if (sequenceSymbol.sequence.isEmpty) {
        //                                    // child node가 empty sequence인 경우
        //                                    Some(resultFunc.bind(beginGen, gen, symbol.symbol, resultFunc.sequence(child.beginGen, gen, sequenceSymbol.symbol)))
        //                                } else if (child.pointer + 1 == sequenceSymbol.sequence.length) {
        //                                    // empty가 아닌 경우
        //                                    val prevSeq = reconstruct0(child, child.endGen)
        //                                    val append = reconstruct0(SymbolKernel(sequenceSymbol.sequence.last, child.endGen), gen)
        //                                    Some(resultFunc.bind(beginGen, gen, symbol.symbol, resultFunc.append(prevSeq, append)))
        //                                } else {
        //                                    None
        //                                }
        //                        }
        //                        assert(!merging.isEmpty)
        //                        resultFunc.merge(merging).get
        //                    case Join(symbol, body, join) =>
        //                        resultFunc.bind(beginGen, gen, symbol,
        //                            resultFunc.join(beginGen, gen, symbol,
        //                                reconstruct0(SymbolKernel(body, beginGen), gen),
        //                                reconstruct0(SymbolKernel(join, beginGen), gen)))
        //                    case symbol: NLookaheadSymbol =>
        //                        resultFunc.bind(beginGen, gen, symbol.symbol, resultFunc.sequence(beginGen, gen, Symbols.Sequence(Seq())))
        //                }
        //            case SequenceKernel(sequenceId, 0, beginGen, endGen) =>
        //                resultFunc.sequence(beginGen, gen, grammar.nsequences(sequenceId).symbol)
        //            case SequenceKernel(sequenceId, pointer, beginGen, endGen) =>
        //                assert(gen == endGen)
        //                val childSymId = grammar.nsequences(sequenceId).sequence(pointer - 1)
        //                val merging = finishes(gen) flatMap {
        //                    case child: SymbolKernel if child.symbolId == childSymId =>
        //                        val prevSeq = SequenceKernel(sequenceId, pointer - 1, beginGen, child.beginGen)
        //                        if (finishes(gen) contains prevSeq) {
        //                            Some(resultFunc.append(reconstruct0(prevSeq, child.beginGen), reconstruct0(child, gen)))
        //                        } else {
        //                            None
        //                        }
        //                    case _ => None
        //                }
        //                if (merging.isEmpty) {
        //                    println(node)
        //                    println(grammar.nsequences(sequenceId).symbol)
        //                    println(grammar.nsymbols(childSymId).symbol)
        //                    println(merging)
        //                    println("??")
        //                }
        //                assert(!merging.isEmpty)
        //                resultFunc.merge(merging).get
        //        }
    }
}

//class CompactParseTreeConstructor[R <: ParseResult](resultFunc: ParseResultFunc[R])(grammar: CompactNGrammar)(input: Seq[Input], history: Seq[Set[Node]], conditionFate: ConditionFate)
//        extends ParseTreeConstructor(resultFunc)(grammar)(input, history, conditionFate) {
//    override protected def reconstruct(node: Node, gen: Int, traces: Set[Int]): R = {
//        resultFunc.sequence(0, 0, grammar.nsequences.values.head.symbol)
//    }
//}
