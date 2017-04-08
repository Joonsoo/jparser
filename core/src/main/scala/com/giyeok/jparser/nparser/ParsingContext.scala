package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.NGrammar.NAtomicSymbol
import com.giyeok.jparser.nparser.NGrammar.NSymbol
import com.giyeok.jparser.nparser.NGrammar.NSequence

object ParsingContext {
    case class Kernel(symbolId: Int, pointer: Int, beginGen: Int, endGen: Int)(val symbol: NSymbol) {
        def shiftGen(gen: Int): Kernel = Kernel(symbolId, pointer, beginGen + gen, endGen + gen)(symbol)

        def lastPointer: Int = symbol match {
            case _: NAtomicSymbol => 1
            case NSequence(_, seq) => seq.length
        }
        def isFinished: Boolean =
            pointer == lastPointer ensuring (0 <= pointer && pointer <= lastPointer)

        override def toString: String = s"Kernel($symbolId, ${symbol.symbol.toShortString}, $pointer, $beginGen..$endGen)"
    }
    case class Node(kernel: Kernel, condition: AcceptCondition) {
        def shiftGen(gen: Int) = Node(kernel.shiftGen(gen), condition.shiftGen(gen))
    }

    sealed trait Edge { val start: Node; val end: Node }
    case class SimpleEdge(start: Node, end: Node) extends Edge
    case class JoinEdge(start: Node, end: Node, join: Node) extends Edge

    case class Graph(nodes: Set[Node], edges: Set[Edge], edgesByStart: Map[Node, Set[Edge]], edgesByDest: Map[Node, Set[Edge]]) {
        // assert(edgesByStart.keySet == nodes && edgesByDest.keySet == nodes)
        // assert((edgesByStart flatMap { _._2 }).toSet subsetOf edges)
        // assert((edgesByDest flatMap { _._2 }).toSet subsetOf edges)
        def finishedNodes: Set[Node] = nodes filter { _.kernel.isFinished }
        def addNode(node: Node): Graph =
            if (nodes contains node) this else Graph(nodes + node, edges, edgesByStart + (node -> Set()), edgesByDest + (node -> Set()))
        def addEdge(edge: Edge): Graph = edge match {
            case SimpleEdge(start, end) =>
                val edgesByStart1 = edgesByStart.updated(start, edgesByStart(start) + edge)
                val edgesByDest1 = edgesByDest.updated(end, edgesByDest(end) + edge)
                Graph(nodes, edges + edge, edgesByStart1, edgesByDest1)
            case JoinEdge(start, end, join) =>
                Graph(nodes, edges + edge, edgesByStart.updated(start, edgesByStart(start) + edge), edgesByDest.updated(end, edgesByDest(end) + edge).updated(join, edgesByDest(join) + edge))
        }
        def removeNodes(removing: Set[Node]): Graph = {
            removing.foldLeft(this) { _ removeNode _ }
        }
        def removeNode(removing: Node): Graph = {
            val removedEdges = edges -- edgesByStart(removing) -- edgesByDest(removing)
            val (removedEdgesByStart, removedEdgesByDest) = {
                val edgesByDest0 = edgesByStart(removing).foldLeft(edgesByDest) { (ccDest, edge) =>
                    edge match {
                        case SimpleEdge(_, end) =>
                            ccDest + (end -> (ccDest(end) - edge))
                        case JoinEdge(_, end, join) =>
                            ccDest + (end -> (ccDest(end) - edge)) + (join -> (ccDest(join) - edge))
                    }
                }
                edgesByDest(removing).foldLeft((edgesByStart, edgesByDest0)) { (cc, edge) =>
                    val (ccStart, ccDest) = cc
                    edge match {
                        case edge @ SimpleEdge(start, _) =>
                            (ccStart + (start -> (ccStart(start) - edge)), ccDest)
                        case edge @ JoinEdge(start, `removing`, other) =>
                            (ccStart + (start -> (ccStart(start) - edge)), ccDest + (other -> (ccDest(other) - edge)))
                        case edge @ JoinEdge(start, other, _) =>
                            (ccStart + (start -> (ccStart(start) - edge)), ccDest + (other -> (ccDest(other) - edge)))
                    }
                }
            }
            Graph(nodes - removing, removedEdges, removedEdgesByStart - removing, removedEdgesByDest - removing)
        }
        def removeEdge(edge: Edge): Graph =
            edge match {
                case edge @ SimpleEdge(start, end) =>
                    Graph(nodes, edges - edge, edgesByStart + (start -> (edgesByStart(start) - edge)), edgesByDest + (end -> (edgesByDest(end) - edge)))
                case edge @ JoinEdge(start, end, join) =>
                    Graph(nodes, edges - edge, edgesByStart + (start -> (edgesByStart(start) - edge)), edgesByDest + (end -> (edgesByDest(end) - edge)) + (join -> (edgesByDest(join) - edge)))
            }
        def removeEdges(edges: Set[Edge]): Graph =
            edges.foldLeft(this) { _ removeEdge _ }
        def shiftGen(gen: Int): Graph = {
            def shiftGen(edge: Edge, gen: Int): Edge = edge match {
                case SimpleEdge(start, end) => SimpleEdge(start.shiftGen(gen), end.shiftGen(gen))
                case JoinEdge(start, end, join) => JoinEdge(start.shiftGen(gen), end.shiftGen(gen), join.shiftGen(gen))
            }
            Graph(nodes map { _.shiftGen(gen) }, edges map { shiftGen(_, gen) },
                edgesByStart map { kv => (kv._1 shiftGen gen) -> (kv._2 map { shiftGen(_, gen) }) },
                edgesByDest map { kv => (kv._1 shiftGen gen) -> (kv._2 map { shiftGen(_, gen) }) })
        }
        def replaceNode(original: Node, replaced: Node): Graph = {
            if (!(nodes contains original)) this else {
                // assert(!(nodes contains replaced))
                def replace(node: Node) = if (node == original) replaced else node
                val replacedNodes = nodes - original + replaced
                var replacedEdges = edges
                var replacedEdgesByStart = edgesByStart - original + (replaced -> Set[Edge]())
                var replacedEdgesByDest = edgesByDest - original + (replaced -> Set[Edge]())
                (edgesByStart(original) ++ edgesByDest(original)) foreach {
                    case edge @ SimpleEdge(start, end) =>
                        val (rstart, rend) = (replace(start), replace(end))
                        val replacedEdge = SimpleEdge(rstart, rend)
                        replacedEdges = replacedEdges - edge + replacedEdge
                        replacedEdgesByStart += (rstart -> (replacedEdgesByStart(rstart) - edge + replacedEdge))
                        replacedEdgesByDest += (rend -> (replacedEdgesByDest(rend) - edge + replacedEdge))
                    case edge @ JoinEdge(start, end, join) =>
                        val (rstart, rend, rjoin) = (replace(start), replace(end), replace(join))
                        val replacedEdge = JoinEdge(rstart, rend, rjoin)
                        replacedEdges = replacedEdges - edge + replacedEdge
                        replacedEdgesByStart += (rstart -> (replacedEdgesByStart(rstart) - edge + replacedEdge))
                        replacedEdgesByDest += (rend -> (replacedEdgesByDest(rend) - edge + replacedEdge))
                        replacedEdgesByDest += (rjoin -> (replacedEdgesByDest(rjoin) - edge + replacedEdge))
                }
                Graph(replacedNodes, replacedEdges, replacedEdgesByStart, replacedEdgesByDest)
            }
        }
        def mapNode(nodeFunc: Node => Node): Graph = {
            val newNodesMap: Map[Node, Node] = (nodes map { node => node -> nodeFunc(node) }).toMap
            val newNodes: Set[Node] = newNodesMap.values.toSet
            val newEdges: Set[Edge] = edges map {
                case SimpleEdge(start, end) =>
                    SimpleEdge(newNodesMap(start), newNodesMap(end))
                case JoinEdge(start, end, join) =>
                    JoinEdge(newNodesMap(start), newNodesMap(end), newNodesMap(join))
            }
            Graph(newNodes, newEdges)
        }
        def filterNode(nodePred: Node => Boolean): Graph = {
            val nodesPredMap: Map[Node, Boolean] = (nodes map { node => node -> nodePred(node) }).toMap
            val newNodes: Set[Node] = (nodesPredMap filter { _._2 }).keySet
            val newEdges: Set[Edge] = edges filter {
                case SimpleEdge(start, end) => nodesPredMap(start) && nodesPredMap(end)
                case JoinEdge(start, end, join) => nodesPredMap(start) && nodesPredMap(end) && nodesPredMap(join)
            }
            Graph(newNodes, newEdges)
        }
        def merge(other: Graph): Graph = {
            def mergeEdgesMap(map: Map[Node, Set[Edge]], merging: Map[Node, Set[Edge]]): Map[Node, Set[Edge]] =
                merging.foldLeft(map) { (cc, i) =>
                    val (node, edges) = i
                    if (!(cc contains node)) cc + (node -> edges) else cc + (node -> (cc(node) ++ edges))
                }
            Graph(nodes ++ other.nodes, edges ++ other.edges, mergeEdgesMap(edgesByStart, other.edgesByStart), mergeEdgesMap(edgesByDest, other.edgesByDest))
        }
    }
    object Graph {
        def apply(nodes: Set[Node], edges: Set[Edge]): Graph = {
            var edgesByStart: Map[Node, Set[Edge]] = (nodes map { n => n -> Set[Edge]() }).toMap
            var edgesByDest: Map[Node, Set[Edge]] = (nodes map { n => n -> Set[Edge]() }).toMap
            edges foreach {
                case edge @ SimpleEdge(start, end) =>
                    edgesByStart += start -> (edgesByStart(start) + edge)
                    edgesByDest += end -> (edgesByDest(end) + edge)
                case edge @ JoinEdge(start, end, join) =>
                    edgesByStart += start -> (edgesByStart(start) + edge)
                    edgesByDest += end -> (edgesByDest(end) + edge)
                    edgesByDest += join -> (edgesByDest(join) + edge)
            }
            Graph(nodes, edges, edgesByStart, edgesByDest)
        }
    }
}
