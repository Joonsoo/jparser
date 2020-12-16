package com.giyeok.jparser.nparser

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.{NAtomicSymbol, NSequence, NSymbol}
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always}
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

object ParsingContext {

    object Kernel {
        def lastPointerOf(symbol: NSymbol): Int = symbol match {
            case _: NAtomicSymbol => 1
            case NSequence(_, _, seq) => seq.length
        }
    }

    case class Kernel(symbolId: Int, pointer: Int, beginGen: Int, endGen: Int) {
        def shiftGen(gen: Int): Kernel = Kernel(symbolId, pointer, beginGen + gen, endGen + gen)

        def initial: Kernel = if (pointer == 0) this else Kernel(symbolId, 0, beginGen, beginGen)

        def tuple: (Int, Int, Int, Int) = (symbolId, pointer, beginGen, endGen)

        def isFinal(grammar: NGrammar): Boolean = {
            val symbol = grammar.symbolOf(symbolId)
            pointer == Kernel.lastPointerOf(symbol) ensuring (0 <= pointer && pointer <= Kernel.lastPointerOf(symbol))
        }

        override def toString: String = s"Kernel($symbolId, $pointer, $beginGen..$endGen)"
    }

    case class Node(kernel: Kernel, condition: AcceptCondition) {
        def shiftGen(gen: Int) = Node(kernel.shiftGen(gen), condition.shiftGen(gen))

        def isInitial = kernel.pointer == 0

        def initial: Node = if (isInitial) this else Node(kernel.initial, Always)

        override val hashCode: Int = (kernel, condition).hashCode()
    }

    case class Edge(start: Node, end: Node, actual: Boolean) extends AbstractEdge[Node] {
        override val hashCode: Int = (start, end).hashCode()
    }

    case class Graph(nodes: Set[Node], edges: Set[Edge], edgesByStart: Map[Node, Set[Edge]], edgesByEnd: Map[Node, Set[Edge]]) extends AbstractGraph[Node, Edge, Graph] {
        // assert(edgesByStart.keySet == nodes && edgesByDest.keySet == nodes)
        // assert((edgesByStart flatMap { _._2 }).toSet subsetOf edges)
        // assert((edgesByDest flatMap { _._2 }).toSet subsetOf edges)
        override def createGraph(nodes: Set[Node], edges: Set[Edge], edgesByStart: Map[Node, Set[Edge]], edgesByEnd: Map[Node, Set[Edge]]): Graph =
            Graph(nodes, edges, edgesByStart, edgesByEnd)

        def mapNode(nodeFunc: Node => Node): Graph = {
            val newNodesMap: Map[Node, Node] = (nodes map { node => node -> nodeFunc(node) }).toMap
            val newNodes: Set[Node] = newNodesMap.values.toSet
            val newEdgesMap: Map[Edge, Edge] = (edges map { edge =>
                edge -> Edge(newNodesMap(edge.start), newNodesMap(edge.end), edge.actual)
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

        def conditionsOf(kernel: Kernel): Set[AcceptCondition] =
            nodes collect { case Node(`kernel`, condition) => condition }
    }

    object Graph {
        def apply(nodes: Set[Node], edges: Set[Edge]): Graph =
            AbstractGraph(nodes, edges, Graph.apply)
    }

    private def reachableProceedingNodesFrom(graph: ParsingContext.Graph, queue: List[Node], visited: Set[Node], cc: Set[Node]): Set[Node] =
        queue match {
            case head +: rest =>
                val nextNodes = graph.edgesByStart.getOrElse(head, Set()) map {
                    _.end
                }
                val (proceedingNodes, skippingNodes) = nextNodes partition { node => node.kernel.beginGen != node.kernel.endGen }
                reachableProceedingNodesFrom(graph, rest ++ (skippingNodes -- visited).toList, visited ++ nextNodes, cc ++ proceedingNodes)
            case List() =>
                cc
        }

    def reduced(graph: ParsingContext.Graph): ParsingContext.Graph = {
        def rec(queue: List[Node], cc: ParsingContext.Graph): ParsingContext.Graph =
            queue match {
                case head +: rest =>
                    val addingNodes = reachableProceedingNodesFrom(graph, List(head), Set(head), Set())
                    val cc1 = addingNodes.foldLeft(cc) { (m, i) =>
                        m.addNode(i).addEdge(Edge(head, i, actual = true))
                    }
                    rec(rest ++ (addingNodes -- cc.nodes).toList, cc1)
                case List() => cc
            }

        val startingNodes = graph.nodes.filter(graph.edgesByEnd(_).isEmpty)
        rec(startingNodes.toList, ParsingContext.Graph(startingNodes, Set()))
    }
}
