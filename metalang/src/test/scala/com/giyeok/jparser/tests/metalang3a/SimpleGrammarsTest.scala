package com.giyeok.jparser.tests.metalang3a

import com.giyeok.jparser.Symbols.{ExactChar, Nonterminal, OneOf, Proxy, Repeat, Sequence, Start}
import com.giyeok.jparser.metalang3a.MetaLanguage3
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParseTreeMatchers._
import com.giyeok.jparser.nparser.TestUtil.{NaiveParserParseToTree, grammarFrom}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.collection.immutable.ListSet

class SimpleGrammarsTest extends AnyFlatSpec {
  "MetaLang3" should "parse simple grammars" in {
    val grammar = MetaLanguage3.analyzeGrammar(
      """A = B C D E F
        |B = 'b'
        |C = 'c'*
        |D = 'd'+
        |E = 'e'?
        |F = "str"
        |""".stripMargin)

    grammar.grammar.rules shouldBe grammarFrom(
      "A" -> List(Sequence(Nonterminal("B"), Nonterminal("C"), Nonterminal("D"), Nonterminal("E"), Nonterminal("F"))),
      "B" -> List(Sequence(ExactChar('b'))),
      "C" -> List(Sequence(Repeat(ExactChar('c'), 0))),
      "D" -> List(Sequence(Repeat(ExactChar('d'), 1))),
      "E" -> List(Sequence(OneOf(ListSet(ExactChar('e'), Proxy(Sequence()))))),
      "F" -> List(Sequence(Proxy(Sequence(ExactChar('s'), ExactChar('t'), ExactChar('r'))))),
    ).rules

    new NaiveParser(grammar.ngrammar).parseToTree("bcdestr") should BindM(Start,
      BindM(Nonterminal("A"), BindM(SeqM(
        BindM(Nonterminal("B"), BindM(Sequence(ExactChar('b')), SeqM(BindM(ExactChar('b'), TermM('b'))))),
        BindM(Nonterminal("C"), BindM(Sequence(Repeat(ExactChar('c'), 0)), SeqM(BindM(Repeat(ExactChar('c'), 0), DontCare)))),
        BindM(Nonterminal("D"), BindM(Sequence(Repeat(ExactChar('d'), 1)), SeqM(BindM(Repeat(ExactChar('d'), 1), DontCare)))),
        BindM(Nonterminal("E"), BindM(Sequence(OneOf(ListSet(ExactChar('e'), Proxy(Sequence())))),
          SeqM(BindM(OneOf(ListSet(ExactChar('e'), Proxy(Sequence()))), BindM(ExactChar('e'), TermM('e')))))),
        DontCare, // BindM(Nonterminal("F"), BindM(Sequence(Seq(ExactChar('s'), ExactChar('t'), ExactChar('r'))), DontCare)),
      ))))
  }

  "MetaLang3" should "parse empty sequence" in {
    val grammar = MetaLanguage3.analyzeGrammar(
      """A = #
        |""".stripMargin)

    grammar.grammar.rules shouldBe grammarFrom(
      "A" -> List(Sequence(Proxy(Sequence())))
    ).rules

    new NaiveParser(grammar.ngrammar).parseToTree("") should BindM(Start,
      BindM(Nonterminal("A"),
        BindM(Sequence(Proxy(Sequence())), SeqM(BindM(Proxy(Sequence()), SeqM())))))
  }

  "MetaLang3" should "parse double empty sequence" in {
    val grammar = MetaLanguage3.analyzeGrammar(
      """A = # #
        |""".stripMargin)

    grammar.grammar.rules shouldBe grammarFrom(
      "A" -> List(Sequence(Proxy(Sequence()), Proxy(Sequence())))
    ).rules

    new NaiveParser(grammar.ngrammar).parseToTree("") should BindM(Start,
      BindM(Nonterminal("A"), BindM(
        Sequence(Proxy(Sequence()), Proxy(Sequence())), SeqM(
          BindM(Proxy(Sequence()), DontCare), BindM(Proxy(Sequence()), DontCare)))))
  }
}
