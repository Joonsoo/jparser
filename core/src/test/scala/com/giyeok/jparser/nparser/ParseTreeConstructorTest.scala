package com.giyeok.jparser.nparser

import com.giyeok.jparser.Symbols.{ExactChar, Nonterminal, OneOf, Proxy, Repeat, Sequence, Start}
import com.giyeok.jparser._
import com.giyeok.jparser.nparser.ParseTreeMatchers.{BindM, DontCare, JoinM, SeqM, TermM}
import com.giyeok.jparser.nparser.TestUtil.{grammarFrom, parseToTree}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

import scala.collection.immutable.ListSet

class ParseTreeConstructorTest extends AnyFlatSpec {
  "Terminal symbol" should "be constructed as bind and terminal node" in {
    val tree = parseToTree("A" -> List(ExactChar('a')))("a")

    tree should BindM(Start, BindM(Nonterminal("A"), BindM(ExactChar('a'), TermM('a'))))
  }

  "Join symbol" should "be constructed as bind and join node" in {
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

  "Sequence" should "be constructed as sequence of bind nodes" in {
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

  "Repeat0" should "be constructed correctly" in {
    val repeatSymbol = Repeat(Nonterminal("B"), 0)
    val parser = parseToTree(
      "A" -> List(repeatSymbol),
      "B" -> List(ExactChar('a'), ExactChar('b'), ExactChar('c'))
    )

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

  "Empty seq" should "be constructed correctly" in {
    parseToTree("A" -> List(Sequence()))("") should BindM(Start, BindM(Nonterminal("A"),
      SeqM()))
  }

  "Proxy empty seq" should "be constructed correctly" in {
    parseToTree("A" -> List(Proxy(Sequence())))("") should BindM(Start, BindM(Nonterminal("A"),
      BindM(Proxy(Sequence()), SeqM())))
  }

  "Plain seq" should "be constructed correctly" in {
    parseToTree("A" -> List(Sequence(ExactChar('a'), ExactChar('b'), ExactChar('c'))))("abc") should BindM(Start,
      BindM(Nonterminal("A"), BindM(Sequence(ExactChar('a'), ExactChar('b'), ExactChar('c')), SeqM(
        BindM(ExactChar('a'), TermM('a')),
        BindM(ExactChar('b'), TermM('b')),
        BindM(ExactChar('c'), TermM('c')),
      ))))
  }

  "Optional" should "be constructed correctly" in {
    val parser = parseToTree(
      "A" -> List(OneOf(ListSet(ExactChar('a'), Proxy(Sequence())))))

    parser("") should BindM(Start, BindM(Nonterminal("A"),
      BindM(OneOf(ListSet(ExactChar('a'), Proxy(Sequence()))), BindM(Proxy(Sequence()), SeqM()))))
    parser("a") should BindM(Start, BindM(Nonterminal("A"),
      BindM(OneOf(ListSet(ExactChar('a'), Proxy(Sequence()))), BindM(ExactChar('a'), TermM('a')))))
  }
}
