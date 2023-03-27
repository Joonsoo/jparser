package com.giyeok.jparser.examples.naive.basics

import com.giyeok.jparser.GrammarHelper.{c, i, lookahead_except, lookahead_is, n, seq}
import com.giyeok.jparser.examples.naive.{GrammarWithExamples, StringExamples}
import com.giyeok.jparser.{Grammar, Symbols}

import scala.collection.immutable.ListMap

object TrickyLookaheads1 extends Grammar with GrammarWithExamples with StringExamples {
  // S = 'x' $$"abc" "abc"
  override val name = "Tricky lookaheads 1"
  val rules: RuleMap = ListMap(
    "S" -> List(
      seq(c('x'), lookahead_is(lookahead_is(i("abc"))), i("abc"))
    )
  )
  override val startSymbol: Symbols.Nonterminal = n("S")

  override val grammar: Grammar = this
  override val correctExamples: Set[String] = Set[String]("xabc")
  override val incorrectExamples: Set[String] = Set[String]()
}

object TrickyLookaheads2 extends Grammar with GrammarWithExamples with StringExamples {
  // S = 'x' $!"abc" "abc"
  override val name = "Tricky lookaheads 1"
  val rules: RuleMap = ListMap(
    "S" -> List(
      seq(c('x'), lookahead_is(lookahead_except(i("abc"))), i("abc"))
    )
  )
  override val startSymbol: Symbols.Nonterminal = n("S")

  override val grammar: Grammar = this
  override val correctExamples: Set[String] = Set[String]()
  override val incorrectExamples: Set[String] = Set[String]("xabc")
}

object TrickyLookaheadsTests1 {
  val tests: Set[GrammarWithExamples] = Set(
    TrickyLookaheads1,
    TrickyLookaheads2,
  )
}
