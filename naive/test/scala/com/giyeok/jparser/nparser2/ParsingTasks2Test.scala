package com.giyeok.jparser.nparser2

import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.nparser.{Kernel, ParseTreeConstructor2}
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser._
import com.giyeok.jparser.examples.naive.basics.MyPaper6_4
import com.giyeok.jparser.nparser2.NaiveParser2.ParsingHistoryContext
import com.giyeok.jparser.nparser2.utils.Utils.{kernelString, printDotGraph}
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


    inputs.foldLeft[Either[ParsingError, ParsingHistoryContext]](Right(parser.initialParsingHistoryContext)) { (cc, i) =>
      val ccc = cc.getOrElse(throw new IllegalStateException())
      println(s"Generation ${ccc.gen}")
      printDotGraph(grammar, ccc.parsingContext)
      ccc.parsingContext.acceptConditions.foreach { c =>
        println(s"${kernelString(grammar, c._1)} -> ${c._2}")
      }
      ccc.acceptConditionsTracker.evolves.foreach { e =>
        println(s"${e._1} -> ${e._2}")
      }
      cc flatMap (parser.parseStep(_, i))
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
    printDotGraph(grammar, x.parsingContext)

    val nn = parser.parseStep(x, Inputs.Character('*'))
    // println(nn)

    val kernels = x.historyKernels.map { kernels =>
      Kernels(kernels.map(k => Kernel(k.symbolId, k.pointer, k.beginGen, k.endGen)))
    }

    val forest = new ParseTreeConstructor2(ParseForestFunc)(grammar)(inputs, kernels).reconstruct()
    println(forest.size)
    println(forest)
    println()
  }
}
