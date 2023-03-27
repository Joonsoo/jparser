package com.giyeok.jparser.milestone

import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.metalang3.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor}
import com.giyeok.jparser.utils.FileUtil.readFile
import org.scalatest.flatspec.AnyFlatSpec

class LexicalGrammarTest extends AnyFlatSpec {
  it should "parse lexical grammar" in {
    val gram = MetaLanguage3.analyzeGrammar(readFile("./examples/src/main/resources/lexical.cdg"))
    gram.ngrammar.nsymbols.toList.sortBy(_._1).foreach { s =>
      println(s"${s._1} -> ${s._2}")
    }
    val valuefier = ValuefyExprSimulator(gram)

    val sourceText = "void"
    val naiveParser = new NaiveParser(gram.ngrammar)
    val ctx = naiveParser.parse(sourceText).left.get
    val reconstructor = new ParseTreeConstructor(ParseForestFunc)(gram.ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal)
    val naiveTrees = reconstructor.reconstruct().get
    println(s"*** Naive: ${naiveTrees.trees.size}")
    naiveTrees.trees.foreach { tree =>
      println(valuefier.valuefyStart(tree))
    }

    val milestoneParserData = MilestoneParserGen.generateMilestoneParserData(gram.ngrammar)
    val parser = new MilestoneParser(milestoneParserData, verbose = true)

    val milestoneTrees = parser.parseAndReconstructToForest(sourceText).left.get
    println("** Parse done")

    println(s"*** Milestone: ${milestoneTrees.trees.size}")
    milestoneTrees.trees.foreach { tree =>
      println(valuefier.valuefyStart(tree))
    }
  }
}
