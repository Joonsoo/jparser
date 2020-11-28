package com.giyeok.jparser.nparser

import com.giyeok.jparser.{Grammar, NGrammar, ParseForest, ParseForestFunc, ParseResultTree, Symbols}
import com.giyeok.jparser.Symbols.Nonterminal
import org.scalatest.matchers.must.Matchers.have
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.collection.immutable.{ListMap, ListSet}

object TestUtil {
  def grammarFrom(grammarRules: (String, List[Symbols.Symbol])*): Grammar =
    grammarFrom(grammarRules.toList)

  def grammarFrom(grammarRules: List[(String, List[Symbols.Symbol])]): Grammar = {
    val rulesWithListSetRhs = grammarRules.map(pair => pair._1 -> ListSet(pair._2: _*))
    new Grammar {
      override val name: String = "Testing Grammar"
      override val rules: this.RuleMap = ListMap(rulesWithListSetRhs: _*)
      override val startSymbol: Nonterminal = Nonterminal(grammarRules.head._1)
    }
  }

  def parserFrom(grammarRules: List[(String, List[Symbols.Symbol])]): NaiveParser =
    new NaiveParser(NGrammar.fromGrammar(grammarFrom(grammarRules)))

  def parseToTree(grammarRules: (String, List[Symbols.Symbol])*): String => ParseResultTree.Node =
    parseToTree(grammarRules.toList)

  def parseToTree(grammarRules: List[(String, List[Symbols.Symbol])]): String => ParseResultTree.Node = {
    val parserFunc = parserFrom(grammarRules)
    (inputText: String) => parserFunc.parseToTree(inputText)
  }

  implicit class NaiveParserParseToTree(parser: NaiveParser) {
    def parseToForest(inputText: String): ParseForest = {
      val ctx = parser.parse(inputText).left.get
      new ParseTreeConstructor(ParseForestFunc)(parser.grammar)(
        ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct().get
    }

    def parseToTree(inputText: String): ParseResultTree.Node = {
      val forest = parseToForest(inputText)
      forest.trees should have size (1)
      forest.trees.head
    }
  }

}
