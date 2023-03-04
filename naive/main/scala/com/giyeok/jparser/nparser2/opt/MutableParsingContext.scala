package com.giyeok.jparser.nparser2.opt

import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.nparser2.{Edge, KernelGraph, ParsingContext}

import scala.collection.mutable

class MutableKernelGraph(
  private val nodes: mutable.Set[Kernel],
  private val edges: mutable.Set[Edge],
  private val _edgesByStart: mutable.Map[Kernel, mutable.Set[Edge]],
  private val _edgesByEnd: mutable.Map[Kernel, mutable.Set[Edge]]) {

  def toKernelGraph: KernelGraph = new KernelGraph(
    nodes.toSet,
    edges.toSet,
    _edgesByStart.toMap.view.mapValues(_.toSet).toMap,
    _edgesByEnd.toMap.view.mapValues(_.toSet).toMap)

  def containsNode(node: Kernel): Boolean = nodes.contains(node)

  def addNode(newNode: Kernel): MutableKernelGraph = {
    nodes += newNode
    _edgesByStart.getOrElseUpdate(newNode, mutable.Set())
    _edgesByEnd.getOrElseUpdate(newNode, mutable.Set())
    this
  }

  def addEdge(newEdge: Edge): MutableKernelGraph = {
    if (!nodes.contains(newEdge.start) || !nodes.contains(newEdge.end)) {
      throw new Exception("addEdge")
    }
    edges += newEdge
    _edgesByStart(newEdge.start) += newEdge
    _edgesByEnd(newEdge.end) += newEdge
    this
  }

  def edgesByStart(node: Kernel): mutable.Set[Edge] = _edgesByStart(node)

  def edgesByEnd(node: Kernel): mutable.Set[Edge] = _edgesByEnd(node)
}

object MutableKernelGraph {
  def apply(kernelGraph: KernelGraph): MutableKernelGraph =
    new MutableKernelGraph(
      kernelGraph.nodes.to(mutable.Set),
      kernelGraph.edges.to(mutable.Set),
      kernelGraph.edgesByStart.view.mapValues(_.to(mutable.Set)).to(mutable.Map),
      kernelGraph.edgesByEnd.view.mapValues(_.to(mutable.Set)).to(mutable.Map))
}

class MutableParsingContext(
  val graph: MutableKernelGraph,
  val acceptConditions: mutable.Map[Kernel, AcceptCondition]) {

  def toParsingContext: ParsingContext = ParsingContext(graph.toKernelGraph, acceptConditions.toMap)
}

object MutableParsingContext {
  def apply(parsingContext: ParsingContext): MutableParsingContext =
    new MutableParsingContext(
      MutableKernelGraph(parsingContext.graph),
      parsingContext.acceptConditions.to(mutable.Map))
}