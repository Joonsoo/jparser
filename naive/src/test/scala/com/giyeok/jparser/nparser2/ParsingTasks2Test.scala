package com.giyeok.jparser.nparser2

import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser.ParsingContext.{Kernel => Kernel1}
import com.giyeok.jparser._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.immutable.ListMap

class ParsingTasks2Test extends AnyFlatSpec {
  "nparser2.ParsingTasks" should "work" in {
    val grammar = NGrammar.fromGrammar(new Grammar {
      // Expr = Term WS '+' WS Expr
      //      | Term
      // Term = Factor WS '*' WS Term
      //      | Factor
      // Factor = '0-9'
      //      | '(' WS Expr WS ')'
      // WS = ' '*
      override val name: String = "ExprGrammar"
      override val rules: RuleMap = ListMap(
        "Expr" -> List(
          seq(n("Term"), n("WS"), c('+'), n("WS"), n("Expr")),
          n("Term")
        ),
        "Term" -> List(
          seq(n("Factor"), n("WS"), c('*'), n("WS"), n("Term")),
          n("Factor")
        ),
        "Factor" -> List(
          chars('0' to '9'),
          seq(c('('), n("WS"), n("Expr"), n("WS"), c(')'))
        ),
        "WS" -> List(
          c(' ').repeat(0)
        )
      )
      override val startSymbol: Symbols.Nonterminal = Symbols.Nonterminal("Expr")
    })
    val parser = new NaiveParser2(grammar)
    val inputs = Inputs.fromString("1+2*3+4*(5+6)*(7)")

    //    val ctx1 = parser.parseStep(0, parser.initialParsingHistoryContext, Inputs.Character('1'))
    //    println(ctx1)
    //
    //    println(ctx1)
    val hctx = parser.parse(inputs)
    // println(hctx)
    println()

    val x = hctx.getOrElse(throw new IllegalStateException())

    println(x.gen)
    printDotGraph(grammar, x.history.last.graph)

    val nn = parser.parseStep(x.gen, x, Inputs.Character('*'))
    // println(nn)

    val kernels = x.historyKernels.map { kernels =>
      Kernels(kernels.map(k => Kernel1(k.symbolId, k.pointer, k.beginGen, k.endGen)))
    }

    val forest = new ParseTreeConstructor2(ParseForestFunc)(grammar)(inputs, kernels).reconstruct()
    println(forest.size)
    println(forest)
    println()
  }

  def printDotGraph(grammar: NGrammar, graph: KernelGraph): Unit = {
    val nodeIds = graph.nodes.zipWithIndex.toMap
    println("digraph X {")
    graph.nodes.foreach { node =>
      val nodeId = nodeIds(node)
      val symbolSeq = (grammar.symbolOf(node.symbolId).symbol match {
        case symbol: Symbols.AtomicSymbol =>
          List(symbol)
        case Symbols.Sequence(seq) =>
          seq.toList
      }) map (_.toShortString)
      val kernelPointerString = symbolSeq.take(node.pointer) ++ List("&bull;") ++ symbolSeq.drop(node.pointer)
      val label = s"(${node.symbolId} ${kernelPointerString.mkString(" ")}, ${node.beginGen}..${node.endGen})"
      val labelString = "\"" + label + "\""
      println(s"  ${nodeId} [label=$labelString];")
    }
    graph.edges.foreach { edge =>
      println(s"  ${nodeIds(edge.start)} -> ${nodeIds(edge.end)};")
    }
    println("}")
  }
}
