package com.giyeok.jparser.nparser2.utils

import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.nparser2.KernelGraph
import com.giyeok.jparser.{NGrammar, Symbols}

object Utils {
  def kernelString(grammar: NGrammar, kernel: Kernel): String = {
    val symbolSeq = (grammar.symbolOf(kernel.symbolId).symbol match {
      case symbol: Symbols.AtomicSymbol =>
        List(symbol)
      case Symbols.Sequence(seq) =>
        seq.toList
    }) map (_.toShortString)
    val kernelPointerString = symbolSeq.take(kernel.pointer) ++ List("&bull;") ++ symbolSeq.drop(kernel.pointer)
    s"(${kernel.symbolId} ${kernelPointerString.mkString(" ")}, ${kernel.beginGen}..${kernel.endGen})"
  }

  def printDotGraph(grammar: NGrammar, graph: KernelGraph): Unit = {
    val nodeIds = graph.nodes.zipWithIndex.toMap
    println("digraph X {")
    graph.nodes.foreach { node =>
      val nodeId = nodeIds(node)
      val label = kernelString(grammar, node)
      val labelString = "\"" + label + "\""
      println(s"  ${nodeId} [label=$labelString];")
    }
    graph.edges.foreach { edge =>
      println(s"  ${nodeIds(edge.start)} -> ${nodeIds(edge.end)};")
    }
    println("}")
  }
}
