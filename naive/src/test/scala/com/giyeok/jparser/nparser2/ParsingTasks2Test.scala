package com.giyeok.jparser.nparser2

import com.giyeok.jparser.examples.basics.SimpleGrammar1
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser.ParsingContext.{Kernel => Kernel1}
import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc}
import org.scalatest.flatspec.AnyFlatSpec

class ParsingTasks2Test extends AnyFlatSpec {
  "nparser2.ParsingTasks" should "work" in {
    val grammar = NGrammar.fromGrammar(SimpleGrammar1)
    val parser = new NaiveParser2(grammar)
    val inputs = Inputs.fromString("abc")

    val hctx = parser.parse(inputs)
    println(hctx)
    println()

    val x = hctx.getOrElse(throw new IllegalStateException())

    val conditionsFinal = x.acceptConditionsTracker.evolves.map { p =>
      p._1 -> p._2.accepted(x.gen, x.parsingContext)
    }
    val kernels = x.history.map { ctx =>
      ctx.acceptConditions.filter(p => conditionsFinal(p._2)).keys
    }.map { kernels =>
      Kernels(kernels.map(k => Kernel1(k.symbolId, k.pointer, k.beginGen, k.endGen)).toSet)
    }

    val forest = new ParseTreeConstructor2(ParseForestFunc)(grammar)(inputs, kernels).reconstruct()
    println(forest.size)
    println(forest)
    println()
  }
}
