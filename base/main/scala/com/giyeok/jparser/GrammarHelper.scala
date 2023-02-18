package com.giyeok.jparser

import com.giyeok.jparser.Symbols.{AnyChar, AtomicSymbol, Chars, ExactChar, Except, Join, Longest, LookaheadExcept, LookaheadIs, Nonterminal, OneOf, Proxy, Repeat, Sequence, Symbol, Terminal, Terminals}
import com.giyeok.jparser.unicode.UnicodeUtil

import scala.collection.immutable.{ListMap, ListSet, NumericRange}

object GrammarHelper {
    private def charSymbol(set: Set[Char]): Terminal =
        if (set.size == 1) ExactChar(set.iterator.next) else Chars(set)

    def empty = Sequence(Seq())
    def n(name: String) = Nonterminal(name)
    def i(string: String) = Sequence(string.toCharArray map { c => ExactChar(c) })
    def anychar = AnyChar
    def c(char: Char) = ExactChar(char)
    def c(chars: Char*) = charSymbol(chars.toSet)
    def c(chars: Set[Char]) = charSymbol(chars)
    def chars(range: NumericRange[Char]) = charSymbol(range.toSet)
    def chars(ranges: NumericRange[Char]*) = charSymbol(ranges.toSet.flatten)
    def chars(chars: String) = charSymbol(chars.toCharArray.toSet)
    def unicode(categories: String*): Terminals.Unicode = unicode(categories.toSet)
    def unicode(categories: Set[String]) = Terminals.Unicode(UnicodeUtil.categoryNamesToCodes(categories))
    // def virtual(name: String) = VirtualInputElem(name)
    def seq(seq: Symbol*) = Sequence(seq.toSeq map proxyIfNeeded)
    // def seq(seq: Seq[Symbol], whitespace: Set[Symbol]) = Sequence(seq map { proxyIfNeeded _ }, whitespace map { proxyIfNeeded _ })
    // def seq(whitespace: Set[Symbol], seq: Symbol*) = Sequence(seq.toSeq map { proxyIfNeeded _ }, whitespace map { proxyIfNeeded _ })
    def seqWS(between: Symbol, seq: Symbol*): Sequence = {
        if (seq.isEmpty) Sequence(Seq())
        val atomicBetween = proxyIfNeeded(between)
        def insert(seq: Seq[Symbol], cc: (Seq[AtomicSymbol], Seq[Int])): (Seq[AtomicSymbol], Seq[Int]) =
            (seq.head, seq.tail) match {
                case (head, Seq()) => (cc._1 :+ proxyIfNeeded(head), cc._2 :+ cc._1.length)
                case (head, tail) =>
                    insert(tail, (cc._1 :+ proxyIfNeeded(head) :+ atomicBetween, cc._2 :+ cc._1.length))
            }
        val (insertedSeq, contentIds) = insert(seq.toSeq, (Seq(), Seq()))
        Sequence(insertedSeq)
    }
    def seqWS(between: Set[Symbol], seq: Symbol*): Sequence = seqWS(oneof(between), seq: _*)
    def oneof(items: Symbol*) = OneOf(ListSet[AtomicSymbol](items map proxyIfNeeded: _*))
    def oneof(items: Set[Symbol]) = OneOf(ListSet[AtomicSymbol]((items map proxyIfNeeded).toSeq: _*))
    def lookahead_is(lookahead: Symbol) = LookaheadIs(proxyIfNeeded(lookahead))
    def lookahead_is(lookaheads: Symbol*) = LookaheadIs(oneof(lookaheads.toSet))
    def lookahead_except(except: Symbol) = LookaheadExcept(proxyIfNeeded(except))
    def lookahead_except(except: Symbol*) = LookaheadExcept(oneof(except.toSet))
    def longest(sym: Symbol) = Longest(proxyIfNeeded(sym))
    def join(sym: Symbol, join: Symbol) = new Join(proxyIfNeeded(sym), proxyIfNeeded(join))
    def proxyIfNeeded(sym: Symbol): AtomicSymbol = sym match {
        case sym: Sequence => Proxy(sym)
        case sym: AtomicSymbol => sym
    }

    // def lgst(t: Terminal) = seq(t, LookaheadExcept(t))

    implicit class GrammarElementExcludable(_sym: Symbol) {
        private val sym = proxyIfNeeded(_sym)
        def except(e: Symbol) = Except(proxyIfNeeded(sym), proxyIfNeeded(e))
        def butnot(e: Symbol): Except = except(e)
        def butnot(e: Symbol*): Except = except(oneof(e.toSet))
    }
    implicit class GrammarElementRepeatable(_sym: Symbol) {
        private val sym = proxyIfNeeded(_sym)
        def repeat(lower: Int, upper: Int): OneOf = OneOf(ListSet(
            (lower to upper) map { count =>
                if (count == 1) sym else Proxy(Sequence((0 until count) map { _ => sym }))
            }: _*))
        def repeat(lower: Int): Repeat = Repeat(sym, lower)

        // optional
        def opt: OneOf = repeat(0, 1)
        def question: OneOf = opt

        // more than zero
        def asterisk: Repeat = repeat(0)
        def star: Repeat = asterisk

        // more than once
        def plus: Repeat = repeat(1)
    }
    implicit class GrammarElementJoinable(sym: Symbol) {
        def join(joinWith: Symbol): Join = Join(proxyIfNeeded(sym), proxyIfNeeded(joinWith))
    }

    implicit class GrammarMergeable(rules: Grammar#RuleMap) {
        def merge(other: Grammar#RuleMap): Grammar#RuleMap = {
            // Merge or replace items of `rules` to items of `other`
            val mutableRules = scala.collection.mutable.ListMap[String, List[Symbols.Symbol]](rules.toSeq: _*)
            other foreach { item => mutableRules += item }
            ListMap(mutableRules.toSeq: _*)
        }
    }
}
