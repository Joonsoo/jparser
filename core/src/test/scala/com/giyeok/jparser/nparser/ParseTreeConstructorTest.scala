package com.giyeok.jparser.nparser

import com.giyeok.jparser.Symbols.{ExactChar, Nonterminal, OneOf, Proxy, Repeat, Sequence, Start}
import com.giyeok.jparser._
import com.giyeok.jparser.nparser.ParseTreeMatchers.{BindM, DontCare, JoinM, SeqM, TermM}
import com.giyeok.jparser.nparser.TestUtil.{grammarFrom, parseToTree}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

import scala.collection.immutable.ListSet

class ParseTreeConstructorTest extends AnyFlatSpec {
  "ParseTreeConstructor" should "construct Terminal symbol grammar to bind and terminal node" in {
    val tree = parseToTree("A" -> List(ExactChar('a')))("a")

    tree should BindM(Start, BindM(Nonterminal("A"), BindM(ExactChar('a'), TermM('a'))))
  }

  "ParseTreeConstructor" should "construct Join symbol grammar to bind and join node" in {
    val tree = parseToTree(
      "A" -> List(Symbols.Join(Nonterminal("B"), Nonterminal("C"))),
      "B" -> List(ExactChar('a'), ExactChar('b')),
      "C" -> List(ExactChar('b'), ExactChar('c')),
    )("b")

    tree should BindM(Start, BindM(Nonterminal("A"),
      BindM(Symbols.Join(Nonterminal("B"), Nonterminal("C")), JoinM(
        BindM(Nonterminal("B"), BindM(ExactChar('b'), TermM('b'))),
        BindM(Nonterminal("C"), BindM(ExactChar('b'), TermM('b'))),
      ))))
  }

  "ParseTreeConstructor" should "construct Sequence to sequence of bind nodes" in {
    val tree = parseToTree(
      "A" -> List(Sequence(Seq(Nonterminal("B"), Nonterminal("C")))),
      "B" -> List(Sequence(Seq(Nonterminal("C"))), Sequence(Seq())),
      "C" -> List(ExactChar('a'))
    )("aa")

    tree should BindM(Start, BindM(Nonterminal("A"),
      BindM(Sequence(Seq(Nonterminal("B"), Nonterminal("C"))), SeqM(
        BindM(Nonterminal("B"), BindM(Sequence(Seq(Nonterminal("C"))),
          SeqM(BindM(Nonterminal("C"), BindM(ExactChar('a'), TermM('a')))))),
        BindM(Nonterminal("C"), BindM(ExactChar('a'), TermM('a')))
      ))))
  }

  "ParseTreeConstructor" should "construct Repeat0 correctly" in {
    val repeatSymbol = Repeat(Nonterminal("B"), 0)
    val parser = parseToTree(
      "A" -> List(repeatSymbol),
      "B" -> List(ExactChar('a'), ExactChar('b'), ExactChar('c'))
    )

    repeatSymbol.baseSeq shouldBe Sequence()
    parser("") should BindM(Start, BindM(Nonterminal("A"),
      BindM(repeatSymbol, BindM(Sequence(), SeqM()))))
    parser("a") should BindM(Start, BindM(Nonterminal("A"),
      BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
        BindM(repeatSymbol, BindM(Sequence(), SeqM())),
        BindM(Nonterminal("B"), BindM(ExactChar('a'), TermM('a'))))))))
    parser("ab") should BindM(Start, BindM(Nonterminal("A"),
      BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
        BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
          BindM(repeatSymbol, BindM(Sequence(), SeqM())),
          BindM(Nonterminal("B"), BindM(ExactChar('a'), TermM('a')))))),
        BindM(Nonterminal("B"), BindM(ExactChar('b'), TermM('b'))))))))
    parser("abc") should BindM(Start, BindM(Nonterminal("A"),
      BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
        BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
          BindM(repeatSymbol, BindM(repeatSymbol.repeatSeq, SeqM(
            BindM(repeatSymbol, BindM(Sequence(), SeqM())),
            BindM(Nonterminal("B"), BindM(ExactChar('a'), TermM('a')))))),
          BindM(Nonterminal("B"), BindM(ExactChar('b'), TermM('b')))))),
        BindM(Nonterminal("B"), BindM(ExactChar('c'), TermM('c'))))))))
  }

  "ParseTreeConstructor" should "construct Repeat1 correctly" in {
    val repeatSymbol = Repeat(Nonterminal("B"), 1)
    val parser = parseToTree(
      "A" -> List(repeatSymbol),
      "B" -> List(ExactChar('a'), ExactChar('b'), ExactChar('c'))
    )

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

  "ParseTreeConstructor" should "construct Empty seq correctly" in {
    parseToTree("A" -> List(Sequence()))("") should BindM(Start, BindM(Nonterminal("A"),
      BindM(Sequence(), SeqM())))
  }

  "ParseTreeConstructor" should "construct Empty seq after a correctly" in {
    parseToTree("A" -> List(Sequence(ExactChar('a'), Proxy(Sequence()))))("a") should BindM(Start,
      BindM(Nonterminal("A"), BindM(Sequence(ExactChar('a'), Proxy(Sequence())), SeqM(
        BindM(ExactChar('a'), TermM('a')),
        BindM(Proxy(Sequence()),
          BindM(Sequence(), SeqM())),
      ))))
  }

  "ParseTreeConstructor" should "construct Proxy empty seq correctly" in {
    parseToTree("A" -> List(Proxy(Sequence())))("") should BindM(Start, BindM(Nonterminal("A"),
      BindM(Proxy(Sequence()), BindM(Sequence(), SeqM()))))
  }

  "ParseTreeConstructor" should "construct Single seq correctly" in {
    parseToTree("A" -> List(Sequence(ExactChar('a'))))("a") should BindM(Start, BindM(Nonterminal("A"),
      BindM(Sequence(ExactChar('a')), SeqM(BindM(ExactChar('a'), TermM('a'))))))
  }

  "ParseTreeConstructor" should "construct Plain seq correctly" in {
    parseToTree("A" -> List(Sequence(ExactChar('a'), ExactChar('b'), ExactChar('c'))))("abc") should BindM(Start,
      BindM(Nonterminal("A"), BindM(Sequence(ExactChar('a'), ExactChar('b'), ExactChar('c')), SeqM(
        BindM(ExactChar('a'), TermM('a')),
        BindM(ExactChar('b'), TermM('b')),
        BindM(ExactChar('c'), TermM('c')),
      ))))
  }

  "ParseTreeConstructor" should "construct Optional correctly" in {
    val parser = parseToTree(
      "A" -> List(OneOf(ListSet(ExactChar('a'), Proxy(Sequence())))))

    parser("") should BindM(Start, BindM(Nonterminal("A"),
      BindM(OneOf(ListSet(ExactChar('a'), Proxy(Sequence()))), BindM(Proxy(Sequence()), DontCare))))
    parser("a") should BindM(Start, BindM(Nonterminal("A"),
      BindM(OneOf(ListSet(ExactChar('a'), Proxy(Sequence()))), BindM(ExactChar('a'), TermM('a')))))
  }
}
