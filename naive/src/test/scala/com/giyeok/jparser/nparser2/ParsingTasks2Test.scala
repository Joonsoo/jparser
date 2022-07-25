package com.giyeok.jparser.nparser2

import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser.ParsingContext.{Kernel => Kernel1}
import com.giyeok.jparser._
import com.giyeok.jparser.examples.basics.MyPaper6_4
import com.giyeok.jparser.nparser2.NaiveParser2.ParsingHistoryContext
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.immutable.ListMap

class ParsingTasks2Test extends AnyFlatSpec {
  private val grammar1 = new Grammar {
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
  }

  "nparser2.ParsingTasks" should "work" in {
    val grammar = NGrammar.fromGrammar(MyPaper6_4.grammar)
    val parser = new NaiveParser2(grammar)
    val inputs = Inputs.fromString("let a b+c;")


    inputs.zipWithIndex.foldLeft[Either[ParsingError, ParsingHistoryContext]](Right(parser.initialParsingHistoryContext)) { (cc, i) =>
      val ccc = cc.getOrElse(throw new IllegalStateException())
      println(s"Generation ${i._2}")
      printDotGraph(grammar, ccc.parsingContext.graph)
      ccc.parsingContext.acceptConditions.foreach { c =>
        println(s"${kernelString(grammar, c._1)} -> ${c._2}")
      }
      ccc.acceptConditionsTracker.evolves.foreach { e =>
        println(s"${e._1} -> ${e._2}")
      }
      cc flatMap (parser.parseStep(i._2, _, i._1))
    }

    //    val ctx1 = parser.parseStep(0, parser.initialParsingHistoryContext, Inputs.Character('1'))
    //    println(ctx1)
    //
    //    println(ctx1)
    val hctx = parser.parse(inputs)
    // println(hctx)
    println()
    if (hctx.isLeft) {
      println(hctx)
      throw new IllegalStateException()
    }

    val x = hctx.getOrElse(throw new IllegalStateException())

    println(x.gen)
    // printDotGraph(grammar, x.history.last.graph)
    printDotGraph(grammar, x.parsingContext.graph)

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
