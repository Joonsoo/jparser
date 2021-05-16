package com.giyeok.jparser.parsergen.deprecated

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar._
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph, GraphUtil}

// Abstract Kernel
case class AKernel(symbolId: Int, pointer: Int) {
    def toReadableString(grammar: NGrammar, pointerString: String = "*"): String = {
        val symbols = grammar.symbolOf(symbolId) match {
            case atomicSymbol: NAtomicSymbol => Seq(atomicSymbol.symbol.toShortString)
            case NGrammar.NSequence(_, _, sequence) => sequence map { elemId =>
                grammar.symbolOf(elemId).symbol.toShortString
            }
        }
        (symbols.take(pointer) mkString " ") + pointerString + (symbols.drop(pointer) mkString " ")
    }
}

case class AKernelEdge(start: AKernel, end: AKernel) extends AbstractEdge[AKernel]

case class AKernelGraph(nodes: Set[AKernel], edges: Set[AKernelEdge], edgesByStart: Map[AKernel, Set[AKernelEdge]], edgesByEnd: Map[AKernel, Set[AKernelEdge]])
    extends AbstractGraph[AKernel, AKernelEdge, AKernelGraph] {

    def createGraph(nodes: Set[AKernel], edges: Set[AKernelEdge], edgesByStart: Map[AKernel, Set[AKernelEdge]], edgesByEnd: Map[AKernel, Set[AKernelEdge]]): AKernelGraph =
        AKernelGraph(nodes, edges, edgesByStart, edgesByEnd)

    def hasEdge(edge: AKernelEdge): Boolean = edges contains edge
}

object AKernelGraph {
    val empty = AKernelGraph(Set(), Set(), Map(), Map())
}

class GrammarAnalyzer(val grammar: NGrammar) {
    lazy val nullableSymbols: Set[Int] = {
        val initialNullables: Set[Int] = (grammar.nsequences filter {
            _._2.sequence.isEmpty
        }).keySet ++ (grammar.nsymbols filter {
            _._2.isInstanceOf[NLookaheadSymbol]
        }).keySet

        def recursion(cc: Set[Int]): Set[Int] = {
            val newNullableSeqs = (grammar.nsequences filter {
                _._2.sequence forall cc.contains
            }).keySet -- cc
            val newNullableSyms = (grammar.nsymbols filter {
                _._2 match {
                    case simpleDerive: NSimpleDerive => (simpleDerive.produces intersect cc).nonEmpty
                    case except: NExcept => cc contains except.body
                    case join: NJoin => (cc contains join.body) && (cc contains join.join)
                    case longest: NLongest => cc contains longest.body
                    case _: NLookaheadSymbol => true
                    case _: NTerminal => false
                }
            }).keySet -- cc
            if (newNullableSeqs.isEmpty && newNullableSyms.isEmpty) cc else recursion(cc ++ newNullableSeqs ++ newNullableSyms)
        }

        recursion(initialNullables)
    }

    def allAKernels(symbolId: Int): Set[AKernel] =
        grammar.symbolOf(symbolId) match {
            case _: NAtomicSymbol =>
                Set(AKernel(symbolId, 0))
            case NSequence(_, _, sequence) =>
                ((0 until sequence.length) map { index =>
                    AKernel(symbolId, index)
                }).toSet
        }

    def isNullable(symbolId: Int): Boolean =
        nullableSymbols contains symbolId

    def isZeroReachableAKernel(end: AKernel): Boolean =
        grammar.symbolOf(end.symbolId) match {
            case _: NAtomicSymbol => true
            case NSequence(_, _, sequence) =>
                sequence take end.pointer forall nullableSymbols.contains
        }

    lazy val deriveRelations: AKernelGraph = {
        def recursion(queue: List[AKernel], cc: AKernelGraph): AKernelGraph =
            queue match {
                case (kernel@AKernel(symbolId, pointer)) +: rest =>
                    // kernel은 derivable한 상태여야 함(atomic symbol이면 pointer==0, sequence이면 pointer<len(sequence))

                    def createEdge(end: AKernel) =
                        AKernelEdge(kernel, end)

                    val addingEdges: Set[AKernelEdge] = grammar.symbolOf(symbolId) match {
                        case NTerminal(_, _) =>
                            Set()
                        case NSequence(_, _, sequence) =>
                            // add (kernel -> Kernel(sequence(pointer), 0)) to cc
                            allAKernels(sequence(pointer)) map { end =>
                                AKernelEdge(kernel, end)
                            }
                        case simpleDerive: NSimpleDerive =>
                            simpleDerive.produces flatMap { produce =>
                                allAKernels(produce) map createEdge
                            }
                        case NExcept(_, _, body, except) =>
                            (allAKernels(except) ++ allAKernels(body)) map createEdge
                        case NJoin(_, _, body, join) =>
                            (allAKernels(body) ++ allAKernels(join)) map createEdge
                        case NLongest(_, _, body) =>
                            allAKernels(body) map { end => AKernelEdge(kernel, end) }
                        case lookahead: NLookaheadSymbol =>
                            (allAKernels(lookahead.emptySeqId) ++ allAKernels(lookahead.lookahead)) map { end =>
                                AKernelEdge(kernel, end)
                            }
                    }

                    var newKernels = Set[AKernel]()
                    val nextcc = addingEdges.foldLeft(cc) { (cc1, edge) =>
                        if (!cc1.hasEdge(edge)) {
                            newKernels += edge.end
                        }
                        cc1.addNode(edge.start).addNode(edge.end).addEdge(edge)
                    }
                    recursion((newKernels ++ rest).toList, nextcc)
                case List() => cc
            }

        val startKernel = AKernel(grammar.startSymbol, 0)
        recursion(List(startKernel), AKernelGraph.empty.addNode(startKernel))
    }

    // deriveRelations를 그래프라고 봤을 때,
    //  - deriversOf(S)는 AKernel(<start>, 0) --> AKernel(S, 0) 의 경로 사이에 있는 모든 노드들의 집합을 반환
    //  - derivablesOf(k)는 k에서 시작해서 도달 가능한 모든 노드들의 집합을 반환
    //  - surroundingsOf(S)는 deriveRelations에서 (k -> AKernel(S, 0))인 엣지를 뺸 그래프에서 AKernel(<start>, 0)에서 도달 가능한 모든 노드들의 집합을 반환

    val startKernel = AKernel(grammar.startSymbol, 0)

    def deriversOf(symbolId: Int): AKernelGraph =
        GraphUtil.pathsBetween[AKernel, AKernelEdge, AKernelGraph](deriveRelations, startKernel, AKernel(symbolId, 0))

    def zeroDeriversOf(symbolId: Int): AKernelGraph =
        GraphUtil.pathsBetweenWithEdgeConstraint[AKernel, AKernelEdge, AKernelGraph](deriveRelations, startKernel, AKernel(symbolId, 0)) { edge =>
            isZeroReachableAKernel(edge.end)
        }

    def derivablesOf(kernel: AKernel): AKernelGraph =
        GraphUtil.reachables[AKernel, AKernelEdge, AKernelGraph](deriveRelations, kernel)

    def surroundingsOf(symbolId: Int): AKernelGraph = {
        // symbolId와 관련된 모든 kernel을 지우고 start에서 도달 가능한 subgraph
        if (grammar.startSymbol == symbolId) deriveRelations else
            GraphUtil.reachables[AKernel, AKernelEdge, AKernelGraph](deriveRelations.removeNodes(allAKernels(symbolId)), startKernel)
    }

    def zeroReachablesFrom(kernel: AKernel): AKernelGraph =
    // TODO kernel이 sequence이고 following symbol이 nullable이면 그 뒤도 포함해주기
        GraphUtil.reachablesWithEdgeConstraint[AKernel, AKernelEdge, AKernelGraph](deriveRelations, kernel) { edge =>
            isZeroReachableAKernel(edge.end)
        }

    def zeroReachablePathsToTerminalsFrom(kernel: AKernel): Seq[Seq[AKernelEdge]] = {
        def recursion(last: AKernel, path: List[AKernelEdge], cc: Seq[Seq[AKernelEdge]]): Seq[Seq[AKernelEdge]] =
            if (grammar.symbolOf(last.symbolId).isInstanceOf[NTerminal]) path +: cc else
                (deriveRelations.edgesByStart(last) filter { edge => isZeroReachableAKernel(edge.end) } filterNot path.contains).foldLeft(cc) { (m, edge) =>
                    recursion(edge.end, path :+ edge, m)
                }

        recursion(kernel, List(), Seq())
    }

    def zeroReachablePathsBetween(start: AKernel, end: Set[AKernel]): Seq[Seq[AKernelEdge]] = {
        def recursion(last: AKernel, path: List[AKernelEdge], cc: Seq[Seq[AKernelEdge]]): Seq[Seq[AKernelEdge]] =
            if (end contains last) path +: cc else
                (deriveRelations.edgesByStart(last) filter { edge => isZeroReachableAKernel(edge.end) } filterNot path.contains).foldLeft(cc) { (m, edge) =>
                    recursion(edge.end, path :+ edge, m)
                }

        recursion(start, List(), Seq())
    }
}
