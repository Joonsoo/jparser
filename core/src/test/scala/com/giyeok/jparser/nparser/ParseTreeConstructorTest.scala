package com.giyeok.jparser.nparser

import com.giyeok.jparser.Symbols.{ExactChar, Nonterminal, Repeat, Sequence, Start}
import com.giyeok.jparser._
import com.giyeok.jparser.nparser.ParseTreeMatchers.{BindM, DontCare, JoinM, SeqM, TermM}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

import scala.collection.immutable.{ListMap, ListSet}

class ParseTreeConstructorTest extends AnyFlatSpec {
  private def parserFrom(grammarRules: List[(String, List[Symbols.Symbol])]): String => ParseResultTree.Node = {
    val rulesWithListSetRhs = grammarRules.map(pair => pair._1 -> ListSet(pair._2: _*))
    val grammar = new Grammar {
      override val name: String = "Testing Grammar"
      override val rules: this.RuleMap = ListMap(rulesWithListSetRhs: _*)
      override val startSymbol: Nonterminal = Nonterminal(grammarRules.head._1)
    }
    val ngrammar = NGrammar.fromGrammar(grammar)
    val parser = new NaiveParser(ngrammar)

    {
      inputText: String =>
        val ctx = parser.parse(inputText).left.get
        val forest = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal)
          .reconstruct().get

        forest.trees should have size (1)

        forest.trees.head
    }
  }

  "Terminal symbol" should "be constructed as bind and terminal node" in {
    val tree = parserFrom(List("A" -> List(ExactChar('a'))))("a")

    tree should BindM(Start, BindM(Nonterminal("A"), BindM(ExactChar('a'), TermM('a'))))
  }

  "Join symbol" should "be constructed as bind and join node" in {
    val tree = parserFrom(List(
      "A" -> List(Symbols.Join(Nonterminal("B"), Nonterminal("C"))),
      "B" -> List(ExactChar('a'), ExactChar('b')),
      "C" -> List(ExactChar('b'), ExactChar('c')),
    ))("b")

    tree should BindM(Start, BindM(Nonterminal("A"),
      BindM(Symbols.Join(Nonterminal("B"), Nonterminal("C")), JoinM(
        BindM(Nonterminal("B"), BindM(ExactChar('b'), TermM('b'))),
        BindM(Nonterminal("C"), BindM(ExactChar('b'), TermM('b'))),
      ))))
  }

  "Sequence" should "be constructed as sequence of bind nodes" in {
    val tree = parserFrom(List(
      "A" -> List(Sequence(Seq(Nonterminal("B"), Nonterminal("C")))),
      "B" -> List(Sequence(Seq(Nonterminal("C"))), Sequence(Seq())),
      "C" -> List(ExactChar('a'))
    ))("aa")

    tree should BindM(Start, BindM(Nonterminal("A"),
      BindM(Sequence(Seq(Nonterminal("B"), Nonterminal("C"))), SeqM(
        BindM(Nonterminal("B"), BindM(Sequence(Seq(Nonterminal("C"))),
          SeqM(BindM(Nonterminal("C"), BindM(ExactChar('a'), TermM('a')))))),
        BindM(Nonterminal("C"), BindM(ExactChar('a'), TermM('a')))
      ))))
  }

  "Repeat0" should "be constructed correctly" in {
    val repeatSymbol = Repeat(Nonterminal("B"), 0)
    val parser = parserFrom(List(
      "A" -> List(repeatSymbol),
      "B" -> List(ExactChar('a'), ExactChar('b'), ExactChar('c'))
    ))

    parser("") should BindM(Start, BindM(Nonterminal("A"), BindM(repeatSymbol, SeqM())))
    parser("a") should BindM(Start, BindM(Nonterminal("A"), BindM(repeatSymbol,
      BindM(repeatSymbol.repeatSeq, SeqM(
        BindM(repeatSymbol, SeqM()),
        BindM(Nonterminal("B"), BindM(ExactChar('a'), TermM('a'))))))))
    parser("ab") should BindM(Start, BindM(Nonterminal("A"),
      BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
        BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
          BindM(repeatSymbol, SeqM()),
          BindM(Nonterminal("B"), BindM(ExactChar('a'), TermM('a')))))),
        BindM(Nonterminal("B"), BindM(ExactChar('b'), TermM('b'))))))))
    parser("abc") should BindM(Start, BindM(Nonterminal("A"),
      BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
        BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
          BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
            BindM(repeatSymbol, SeqM()),
            BindM(Nonterminal("B"), BindM(ExactChar('a'), TermM('a')))))),
          BindM(Nonterminal("B"), BindM(ExactChar('b'), TermM('b')))))),
        BindM(Nonterminal("B"), BindM(ExactChar('c'), TermM('c'))))))))
  }

  "Repeat1" should "be constructed correctly" in {
    val repeatSymbol = Repeat(Nonterminal("B"), 1)
    val parser = parserFrom(List(
      "A" -> List(repeatSymbol),
      "B" -> List(ExactChar('a'), ExactChar('b'), ExactChar('c'))
    ))

    parser("a") should BindM(Start, BindM(Nonterminal("A"),
      BindM(repeatSymbol, BindM(Nonterminal("B"), BindM(ExactChar('a'), TermM('a'))))))
    parser("ab") should BindM(Start, BindM(Nonterminal("A"),
      BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
        BindM(repeatSymbol, BindM(Nonterminal("B"), BindM(ExactChar('a'), TermM('a')))),
        BindM(Nonterminal("B"), BindM(ExactChar('b'), TermM('b')))
      )))))
    parser("abc") should BindM(Start, BindM(Nonterminal("A"),
      BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
        BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
          BindM(repeatSymbol, BindM(Nonterminal("B"), BindM(ExactChar('a'), TermM('a')))),
          BindM(Nonterminal("B"), BindM(ExactChar('b'), TermM('b')))
        ))),
        BindM(Nonterminal("B"), BindM(ExactChar('c'), TermM('c')))
      )))))
  }
}
