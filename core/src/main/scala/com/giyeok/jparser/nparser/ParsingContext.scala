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

        override val hashCode: Int = (kernel, condition).hashCode()
    }

    case class Edge(start: Node, end: Node) {
        override val hashCode: Int = (start, end).hashCode()
    }

    case class Graph(nodes: Set[Node], edges: Set[Edge], edgesByStart: Map[Node, Set[Edge]], edgesByEnd: Map[Node, Set[Edge]]) {
        // assert(edgesByStart.keySet == nodes && edgesByDest.keySet == nodes)
        // assert((edgesByStart flatMap { _._2 }).toSet subsetOf edges)
        // assert((edgesByDest flatMap { _._2 }).toSet subsetOf edges)
        def addNode(node: Node): Graph =
            if (nodes contains node) this else Graph(nodes + node, edges, edgesByStart + (node -> Set()), edgesByEnd + (node -> Set()))
        def addEdge(edge: Edge): Graph = {
            val edgesByStart1 = edgesByStart.updated(edge.start, edgesByStart(edge.start) + edge)
            val edgesByDest1 = edgesByEnd.updated(edge.end, edgesByEnd(edge.end) + edge)
            Graph(nodes, edges + edge, edgesByStart1, edgesByDest1)
        }
        def removeNodes(removingNodes: Set[Node]): Graph = {
            val removingEdges = removingNodes.foldLeft(Set[Edge]()) { (cc, node) => cc ++ edgesByStart(node) ++ edgesByEnd(node) }
            val newEdgesByStart = (edgesByStart -- removingNodes) mapValues { _ -- removingEdges }
            val newEdgesByDest = (edgesByEnd -- removingNodes) mapValues { _ -- removingEdges }
            Graph(nodes -- removingNodes, edges -- removingEdges, newEdgesByStart, newEdgesByDest)
        }
        def removeNode(removingNode: Node): Graph = {
            val removingEdges = edgesByStart(removingNode) ++ edgesByEnd(removingNode)
            val newEdgesByStart = (edgesByStart - removingNode) mapValues { _ -- removingEdges }
            val newEdgesByDest = (edgesByEnd - removingNode) mapValues { _ -- removingEdges }
            Graph(nodes - removingNode, edges -- removingEdges, newEdgesByStart, newEdgesByDest)
        }
        def removeEdge(edge: Edge): Graph =
            Graph(nodes, edges - edge,
                edgesByStart + (edge.start -> (edgesByStart(edge.start) - edge)),
                edgesByEnd + (edge.end -> (edgesByEnd(edge.end) - edge)))
        def mapNode(nodeFunc: Node => Node): Graph = {
            val newNodesMap: Map[Node, Node] = (nodes map { node => node -> nodeFunc(node) }).toMap
            val newNodes: Set[Node] = newNodesMap.values.toSet
            val newEdgesMap: Map[Edge, Edge] = (edges map { edge =>
                edge -> Edge(newNodesMap(edge.start), newNodesMap(edge.end))
            }).toMap
            val newEdgesByStart = edgesByStart map { kv =>
                newNodesMap(kv._1) -> (kv._2 map newEdgesMap)
            }
            val newEdgesByEnd = edgesByEnd map { kv =>
                newNodesMap(kv._1) -> (kv._2 map newEdgesMap)
            }
            Graph(newNodes, newEdgesMap.values.toSet, newEdgesByStart, newEdgesByEnd)
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
            val newEdgesByStart = edgesByStart collect {
                case (node, edges) if nodesPredMap(node) => node -> (edges intersect newEdges)
            }
            val newEdgesByEnd = edgesByEnd collect {
                case (node, edges) if nodesPredMap(node) => node -> (edges intersect newEdges)
            }
            Graph(newNodes, newEdges, newEdgesByStart, newEdgesByEnd)
        }
        def merge(other: Graph): Graph = {
            def mergeEdgesMap(map: Map[Node, Set[Edge]], merging: Map[Node, Set[Edge]]): Map[Node, Set[Edge]] =
                merging.foldLeft(map) { (cc, i) =>
                    val (node, edges) = i
                    if (!(cc contains node)) cc + (node -> edges) else cc + (node -> (cc(node) ++ edges))
                }
            Graph(nodes ++ other.nodes, edges ++ other.edges, mergeEdgesMap(edgesByStart, other.edgesByStart), mergeEdgesMap(edgesByEnd, other.edgesByEnd))
        }
    }
    object Graph {
        def apply(nodes: Set[Node], edges: Set[Edge]): Graph = {
            val _edgesByStart: Map[Node, Set[Edge]] = edges groupBy { _.start }
            val _edgesByEnd: Map[Node, Set[Edge]] = edges groupBy { _.end }
            val edgesByStart = _edgesByStart ++ ((nodes -- _edgesByStart.keySet) map { _ -> Set[Edge]() })
            val edgesByEnd = _edgesByEnd ++ ((nodes -- _edgesByEnd.keySet) map { _ -> Set[Edge]() })
            Graph(nodes, edges, edgesByStart, edgesByEnd)
        }
    }
}
