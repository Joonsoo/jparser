package com.giyeok.jparser.utils

trait AbstractEdge[N] {
    val start: N
    val end: N
}

case class SimpleEdge[N](start: N, end: N) extends AbstractEdge[N]

trait AbstractGraph[N, E <: AbstractEdge[N], Self <: AbstractGraph[N, E, _]] {
    val nodes: Set[N]
    val edges: Set[E]
    val edgesByStart: Map[N, Set[E]]
    val edgesByEnd: Map[N, Set[E]]

    def createGraph(nodes: Set[N], edges: Set[E], edgesByStart: Map[N, Set[E]], edgesByEnd: Map[N, Set[E]]): Self

    def addNode(node: N): Self =
        if (nodes contains node) this.asInstanceOf[Self]
        else createGraph(nodes + node, edges, edgesByStart + (node -> Set()), edgesByEnd + (node -> Set()))

    def addEdge(edge: E): Self = {
        val edgesByStart1 = edgesByStart.updated(edge.start, edgesByStart(edge.start) + edge)
        val edgesByDest1 = edgesByEnd.updated(edge.end, edgesByEnd(edge.end) + edge)
        createGraph(nodes, edges + edge, edgesByStart1, edgesByDest1)
    }

    def removeNodes(removingNodes: Set[N]): Self = {
        val removingEdges = removingNodes.foldLeft(Set[E]()) { (cc, node) => cc ++ edgesByStart(node) ++ edgesByEnd(node) }
        val newEdgesByStart = (edgesByStart -- removingNodes) mapValues {
            _ -- removingEdges
        }
        val newEdgesByDest = (edgesByEnd -- removingNodes) mapValues {
            _ -- removingEdges
        }
        createGraph(nodes -- removingNodes, edges -- removingEdges, newEdgesByStart, newEdgesByDest)
    }

    def removeNode(removingNode: N): Self = {
        val removingEdges = edgesByStart(removingNode) ++ edgesByEnd(removingNode)
        val newEdgesByStart = (edgesByStart - removingNode) mapValues {
            _ -- removingEdges
        }
        val newEdgesByDest = (edgesByEnd - removingNode) mapValues {
            _ -- removingEdges
        }
        createGraph(nodes - removingNode, edges -- removingEdges, newEdgesByStart, newEdgesByDest)
    }

    def removeEdge(edge: E): Self =
        createGraph(nodes, edges - edge,
            edgesByStart + (edge.start -> (edgesByStart(edge.start) - edge)),
            edgesByEnd + (edge.end -> (edgesByEnd(edge.end) - edge)))

    def filterNode(nodePred: N => Boolean): Self = {
        val nodesPredMap: Map[N, Boolean] = (nodes map { node => node -> nodePred(node) }).toMap
        val newNodes: Set[N] = (nodesPredMap filter {
            _._2
        }).keySet
        val newEdges: Set[E] = edges filter { edge =>
            nodesPredMap(edge.start) && nodesPredMap(edge.end)
        }
        val newEdgesByStart = edgesByStart collect {
            case (node, edges) if nodesPredMap(node) => node -> (edges intersect newEdges)
        }
        val newEdgesByEnd = edgesByEnd collect {
            case (node, edges) if nodesPredMap(node) => node -> (edges intersect newEdges)
        }
        createGraph(newNodes, newEdges, newEdgesByStart, newEdgesByEnd)
    }

    def merge(other: Self): Self = {
        def mergeEdgesMap(map: Map[N, Set[E]], merging: Map[N, Set[E]]): Map[N, Set[E]] =
            merging.foldLeft(map) { (cc, i) =>
                val (node, edges) = i
                if (!(cc contains node)) cc + (node -> edges) else cc + (node -> (cc(node) ++ edges))
            }

        createGraph(nodes ++ other.nodes, edges ++ other.edges, mergeEdgesMap(edgesByStart, other.edgesByStart), mergeEdgesMap(edgesByEnd, other.edgesByEnd))
    }
}

object AbstractGraph {
    def apply[N, E <: AbstractEdge[N], G <: AbstractGraph[N, E, G]](nodes: Set[N], edges: Set[E], createGraphFunc: (Set[N], Set[E], Map[N, Set[E]], Map[N, Set[E]]) => G): G = {
        val _edgesByStart: Map[N, Set[E]] = edges groupBy { edge => edge.start }
        val _edgesByEnd: Map[N, Set[E]] = edges groupBy { edge => edge.end }
        val edgesByStart = _edgesByStart ++ ((nodes -- _edgesByStart.keySet) map { node => node -> Set[E]() })
        val edgesByEnd = _edgesByEnd ++ ((nodes -- _edgesByEnd.keySet) map { node => node -> Set[E]() })
        createGraphFunc(nodes, edges, edgesByStart, edgesByEnd)
    }
}
