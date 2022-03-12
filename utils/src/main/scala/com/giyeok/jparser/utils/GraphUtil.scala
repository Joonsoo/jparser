package com.giyeok.jparser.utils

import com.giyeok.jparser.graph.{AbstractEdge, AbstractGraph}

object GraphUtil {
  // node로부터 시작해서 도달 가능한 노드/엣지로 이루어진 sub graph
  def reachables[N, E <: AbstractEdge[N], G <: AbstractGraph[N, E, G]](graph: G, start: N): G = {
    val builder = graph.newBuilder

    def recursion(queue: List[N]): G =
      queue match {
        case head +: rest =>
          var newQueue = List[N]()
          graph.edgesByStart(head) foreach { outgoing =>
            if (!(builder hasNode outgoing.end)) {
              newQueue +:= outgoing.end
            }
            builder.addEdgeAndNode(outgoing)
          }
          recursion(rest ++ newQueue)
        case List() =>
          builder.result()
      }

    recursion(List(start))
  }

  def reachablesWithEdgeConstraint[N, E <: AbstractEdge[N], G <: AbstractGraph[N, E, G]](graph: G, start: N)(pred: E => Boolean): G = {
    val builder = graph.newBuilder

    def recursion(queue: List[N]): G =
      queue match {
        case head +: rest =>
          var newQueue = List[N]()
          graph.edgesByStart(head) filter pred foreach { outgoing =>
            if (!(builder hasNode outgoing.end)) {
              newQueue +:= outgoing.end
            }
            builder.addEdgeAndNode(outgoing)
          }
          recursion(rest ++ newQueue)
        case List() =>
          builder.result()
      }

    recursion(List(start))
  }

  // start에서 시작해서 end로 도착 가능한 모든 path에 속한 노드/엣지로 이루어진 sub graph
  def pathsBetween[N, E <: AbstractEdge[N], G <: AbstractGraph[N, E, G]](graph: G, start: N, end: N): G = {
    val builder = graph.newBuilder

    def recursion(path: Set[E], last: N): Unit = {
      if (last == end) {
        path foreach { edge => builder.addEdgeAndNode(edge) }
      } else {
        graph.edgesByStart(last) -- path foreach { edge =>
          recursion(path + edge, edge.end)
        }
      }
    }

    recursion(Set(), start)
    builder.result()
  }

  def pathsBetweenWithEdgeConstraint[N, E <: AbstractEdge[N], G <: AbstractGraph[N, E, G]](graph: G, start: N, end: N)(pred: E => Boolean): G = {
    val builder = graph.newBuilder

    def recursion(path: Set[E], last: N): Unit = {
      if (last == end) {
        path foreach { edge => builder.addEdgeAndNode(edge) }
      } else {
        (graph.edgesByStart(last) -- path) filter pred foreach { edge =>
          recursion(path + edge, edge.end)
        }
      }
    }

    recursion(Set(), start)
    builder.result()
  }
}
