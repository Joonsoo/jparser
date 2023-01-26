package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.examples.basics.MyPaper6_4
import com.giyeok.jparser.nparser2.utils.Utils.printDotGraph
import org.scalatest.flatspec.AnyFlatSpec

class DeriveMGroupTest extends AnyFlatSpec {
  "asdf" should "work" in {
    val grammar = NGrammar.fromGrammar(MyPaper6_4.grammar)

    val parserGen = new MGroupParserGen(grammar)

    val (initialCtx, initialMGroup) = parserGen.deriveMGroup(0, Set(parserGen.startKernelTmpl))
    println(initialMGroup)

    printDotGraph(grammar, initialCtx.graph)

    val x = parserGen.termActionsFrom(0, initialMGroup, initialCtx)
    println(x)
  }
}
