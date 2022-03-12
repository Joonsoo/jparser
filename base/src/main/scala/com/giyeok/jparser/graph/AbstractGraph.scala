package com.giyeok.jparser.graph

import scala.collection.mutable

trait AbstractEdge[N] {
    val start: N
    val end: N
}

case class SimpleEdge[N](start: N, end: N) extends AbstractEdge[N]

class GraphBuilder[N, E <: AbstractEdge[N], +G <: AbstractGraph[N, E, G]](val createGraph: (Set[N], Set[E], Map[N, Set[E]], Map[N, Set[E]]) => G) {
    private val nodes = mutable.Set[N]()
    private val edges = mutable.Set[E]()
    private val edgesByStart = mutable.Map[N, Set[E]]()
    private val edgesByEnd = mutable.Map[N, Set[E]]()

    def result(): G = createGraph(nodes.toSet, edges.toSet, edgesByStart.toMap, edgesByEnd.toMap)

    def addNode(node: N): Unit = {
        if (!(nodes contains node)) {
            nodes.add(node)
            edgesByStart += node -> Set()
            edgesByEnd += node -> Set()
        }
    }

    def addEdge(edge: E): Unit = {
        if (!(edges contains edge)) {
            if (!(nodes contains edge.start) || !(nodes contains edge.end)) {
                throw new Exception("node is not added")
            }
            edges.add(edge)
            edgesByStart(edge.start) = edgesByStart(edge.start) + edge
            edgesByEnd(edge.end) = edgesByEnd(edge.end) + edge
        }
    }

    def addEdgeAndNode(edge: E): Unit = {
        addNode(edge.start)
        addNode(edge.end)
        addEdge(edge)
    }

    def hasNode(node: N): Boolean = nodes contains node

    def hasEdge(edge: E): Boolean = edges contains edge
}

trait AbstractGraph[N, E <: AbstractEdge[N], +Self <: AbstractGraph[N, E, Self]] {
    def newBuilder = new GraphBuilder[N, E, Self](createGraph)

    val nodes: Set[N]
    val edges: Set[E]
    val edgesByStart: Map[N, Set[E]]
    val edgesByEnd: Map[N, Set[E]]

    def createGraph(nodes: Set[N], edges: Set[E], edgesByStart: Map[N, Set[E]], edgesByEnd: Map[N, Set[E]]): Self

    def addNode(node: N): Self =
        if (nodes contains node) this.asInstanceOf[Self]
        else createGraph(nodes + node, edges, edgesByStart + (node -> Set()), edgesByEnd + (node -> Set()))

    // 만약 edge.start나 edge.end가 nodes에 없으면 추가해준다
    def addEdgeSafe(edge: E): Self =
        addNode(edge.start).addNode(edge.end).addEdge(edge)

    def addEdge(edge: E): Self = {
        val edgesByStart1 = edgesByStart.updated(edge.start, edgesByStart(edge.start) + edge)
        val edgesByDest1 = edgesByEnd.updated(edge.end, edgesByEnd(edge.end) + edge)
        createGraph(nodes, edges + edge, edgesByStart1, edgesByDest1)
    }

    def removeNodes(removingNodes: Set[N]): Self = {
        val removingEdges = removingNodes.foldLeft(Set[E]()) { (cc, node) => cc ++ edgesByStart(node) ++ edgesByEnd(node) }
        val newEdgesByStart = (edgesByStart -- removingNodes).view.mapValues {
            _ -- removingEdges
        }
        val newEdgesByDest = (edgesByEnd -- removingNodes).view.mapValues {
            _ -- removingEdges
        }
        createGraph(nodes -- removingNodes, edges -- removingEdges, newEdgesByStart.toMap, newEdgesByDest.toMap)
    }

    def removeNode(removingNode: N): Self = {
        val removingEdges = edgesByStart(removingNode) ++ edgesByEnd(removingNode)
        val newEdgesByStart = (edgesByStart - removingNode).view.mapValues {
            _ -- removingEdges
        }
        val newEdgesByDest = (edgesByEnd - removingNode).view.mapValues {
            _ -- removingEdges
        }
        createGraph(nodes - removingNode, edges -- removingEdges, newEdgesByStart.toMap, newEdgesByDest.toMap)
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
        createGraph(newNodes, newEdges, newEdgesByStart.toMap, newEdgesByEnd.toMap)
    }

    def merge[G <: AbstractGraph[N, E, G]](other: G): Self = {
        def mergeEdgesMap(map: Map[N, Set[E]], merging: Map[N, Set[E]]): Map[N, Set[E]] =
            ((map.keySet ++ merging.keySet) map { f =>
                f -> (map.getOrElse(f, Set()) ++ merging.getOrElse(f, Set()))
            }).toMap

        createGraph(nodes ++ other.nodes, edges ++ other.edges,
            mergeEdgesMap(edgesByStart, other.edgesByStart),
            mergeEdgesMap(edgesByEnd, other.edgesByEnd))
    }

    def reachableBetween(start: N, end: N): Boolean = {
        var visited: Set[N] = Set(start)

        def recursion(current: N): Boolean = {
            if (current == end) true else {
                visited += current
                ((edgesByStart(current) map { e => e.end }) -- visited) exists recursion
            }
        }

        recursion(start)
    }

    def reachableNodesBetween(starts: Set[N], end: N): Set[N] =
        starts.filter(reachableBetween(_, end))

    def reachableBetween(starts: Set[N], end: N): Boolean =
        starts.exists(reachableBetween(_, end))

    def reachableBetween(start: N, ends: Set[N]): Boolean =
        ends.exists(reachableBetween(start, _))

    def reachableGraphBetween(start: N, end: N): Self = reachableGraphBetween(start, Set(end))

    def reachableGraphBetween(start: N, ends: Set[N]):Self = {
        var visited: Set[N] = Set(start)

        def recursion(path: List[E], cc: Self): Self = path match {
            case visiting +: _ if ends.contains(visiting.end) =>
                path.foldLeft(cc) { (m, i) => m.addEdgeSafe(i) }
            case visiting +: _ =>
                if (visited.contains(visiting.end)) cc else {
                    visited += visiting.end
                    edgesByStart(visiting.end).foldLeft(cc) { (m, i) => recursion(i +: path, m) }
                }
        }

        val initialGraph = createGraph(Set(), Set(), Map(), Map())
        edgesByStart(start).foldLeft(initialGraph) { (m, i) =>
            if (i.end == start) m.addNode(start).addEdge(i) else recursion(List(i), m)
        }
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
