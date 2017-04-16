package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.NGrammar.NAtomicSymbol
import com.giyeok.jparser.nparser.NGrammar.NSymbol
import com.giyeok.jparser.nparser.NGrammar.NSequence

object ParsingContext {
    object Kernel {
        def lastPointerOf(symbol: NSymbol): Int = symbol match {
            case _: NAtomicSymbol => 1
            case NSequence(_, seq) => seq.length
        }
    }
    case class Kernel(symbolId: Int, pointer: Int, beginGen: Int, endGen: Int)(val symbol: NSymbol) {
        def shiftGen(gen: Int): Kernel = Kernel(symbolId, pointer, beginGen + gen, endGen + gen)(symbol)

        def isFinished: Boolean =
            pointer == Kernel.lastPointerOf(symbol) ensuring (0 <= pointer && pointer <= Kernel.lastPointerOf(symbol))

        override def toString: String = s"Kernel($symbolId, ${symbol.symbol.toShortString}, $pointer, $beginGen..$endGen)"
    }
    case class Node(kernel: Kernel, condition: AcceptCondition) {
        def shiftGen(gen: Int) = Node(kernel.shiftGen(gen), condition.shiftGen(gen))
    }

    case class Edge(start: Node, end: Node)

    case class Graph(nodes: Set[Node], edges: Set[Edge], edgesByStart: Map[Node, Set[Edge]], edgesByDest: Map[Node, Set[Edge]]) {
        // assert(edgesByStart.keySet == nodes && edgesByDest.keySet == nodes)
        // assert((edgesByStart flatMap { _._2 }).toSet subsetOf edges)
        // assert((edgesByDest flatMap { _._2 }).toSet subsetOf edges)
        def addNode(node: Node): Graph =
            if (nodes contains node) this else Graph(nodes + node, edges, edgesByStart + (node -> Set()), edgesByDest + (node -> Set()))
        def addEdge(edge: Edge): Graph = {
            val edgesByStart1 = edgesByStart.updated(edge.start, edgesByStart(edge.start) + edge)
            val edgesByDest1 = edgesByDest.updated(edge.end, edgesByDest(edge.end) + edge)
            Graph(nodes, edges + edge, edgesByStart1, edgesByDest1)
        }
        def removeNodes(removing: Set[Node]): Graph = {
            removing.foldLeft(this) { _ removeNode _ }
        }
        def removeNode(removing: Node): Graph = {
            val removedEdges = edges -- edgesByStart(removing) -- edgesByDest(removing)
            //            val (removedEdgesByStart, removedEdgesByDest) = {
            //                val edgesByDest0 = edgesByStart(removing).foldLeft(edgesByDest) { (ccDest, edge) =>
            //                    edge match {
            //                        case SimpleEdge(_, end) =>
            //                            ccDest + (end -> (ccDest(end) - edge))
            //                        case JoinEdge(_, end, join) =>
            //                            ccDest + (end -> (ccDest(end) - edge)) + (join -> (ccDest(join) - edge))
            //                    }
            //                }
            //                edgesByDest(removing).foldLeft((edgesByStart, edgesByDest0)) { (cc, edge) =>
            //                    val (ccStart, ccDest) = cc
            //                    edge match {
            //                        case edge @ SimpleEdge(start, _) =>
            //                            (ccStart + (start -> (ccStart(start) - edge)), ccDest)
            //                        case edge @ JoinEdge(start, `removing`, other) =>
            //                            (ccStart + (start -> (ccStart(start) - edge)), ccDest + (other -> (ccDest(other) - edge)))
            //                        case edge @ JoinEdge(start, other, _) =>
            //                            (ccStart + (start -> (ccStart(start) - edge)), ccDest + (other -> (ccDest(other) - edge)))
            //                    }
            //                }
            //            }
            Graph(nodes - removing, removedEdges)
        }
        def removeEdge(edge: Edge): Graph =
            Graph(nodes, edges - edge,
                edgesByStart + (edge.start -> (edgesByStart(edge.start) - edge)),
                edgesByDest + (edge.end -> (edgesByDest(edge.end) - edge)))
        def removeEdges(edges: Set[Edge]): Graph =
            edges.foldLeft(this) { _ removeEdge _ }
        def mapNode(nodeFunc: Node => Node): Graph = {
            val newNodesMap: Map[Node, Node] = (nodes map { node => node -> nodeFunc(node) }).toMap
            val newNodes: Set[Node] = newNodesMap.values.toSet
            val newEdges: Set[Edge] = edges map { edge =>
                Edge(newNodesMap(edge.start), newNodesMap(edge.end))
            }
            Graph(newNodes, newEdges)
        }
        def replaceNode(original: Node, replaced: Node): Graph = {
            if (!(nodes contains original)) this else {
                // assert(!(nodes contains replaced))
                this mapNode {
                    case `original` => replaced
                    case other => other
                }
            }
        }
        def filterNode(nodePred: Node => Boolean): Graph = {
            val nodesPredMap: Map[Node, Boolean] = (nodes map { node => node -> nodePred(node) }).toMap
            val newNodes: Set[Node] = (nodesPredMap filter { _._2 }).keySet
            val newEdges: Set[Edge] = edges filter { edge =>
                nodesPredMap(edge.start) && nodesPredMap(edge.end)
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
            edges foreach { edge =>
                edgesByStart += edge.start -> (edgesByStart(edge.start) + edge)
                edgesByDest += edge.end -> (edgesByDest(edge.end) + edge)
            }
            Graph(nodes, edges, edgesByStart, edgesByDest)
        }
    }
}
