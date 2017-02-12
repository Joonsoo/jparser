package com.giyeok.jparser

import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import scala.collection.immutable.NumericRange

object GrammarHelper {
    import Symbols._
    import utils.UnicodeUtil

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
        val (insertedSeq, contentIds) = insert(seq, (Seq(), Seq()))
        Sequence(insertedSeq, contentIds)
    }
    def seqWS(between: Set[Symbol], seq: Symbol*): Sequence = seqWS(oneof(between), seq: _*)
    def ws(set: Symbol*): Set[Symbol] = Set[Symbol](set: _*)
    def oneof(items: Symbol*) = OneOf(items.toSet)
    def oneof(items: Set[Symbol]) = OneOf(items)
    def lookahead_is(lookahead: Symbol) = LookaheadIs(proxyIfNeeded(lookahead))
    def lookahead_is(lookaheads: Symbol*) = LookaheadIs(oneof(lookaheads.toSet))
    def lookahead_except(except: Symbol) = LookaheadExcept(proxyIfNeeded(except))
    def lookahead_except(except: Symbol*) = LookaheadExcept(oneof(except.toSet))
    def longest(sym: Symbol) = Longest(sym)
    def elongest(sym: Symbol) = EagerLongest(sym)
    def join(sym: Symbol, join: Symbol) = new Join(proxyIfNeeded(sym), proxyIfNeeded(join))
    def proxyIfNeeded(sym: Symbol): AtomicSymbol = sym match {
        case sym: Sequence => Proxy.of(sym)
        case sym: AtomicSymbol => sym
    }

    // def lgst(t: Terminal) = seq(t, LookaheadExcept(t))

    implicit class GrammarElementExcludable(sym: Symbol) {
        def except(e: Symbol) = e match {
            case e: AtomicSymbol => Except(sym, e)
            case e: Sequence => Except(sym, Proxy(e))
        }
        def butnot(e: Symbol) = except(e)
        def butnot(e: Symbol*) = except(oneof(e.toSet))
    }
    implicit class GrammarElementRepeatable(sym: Symbol) {
        def repeat(lower: Int, upper: Int): OneOf = OneOf(((lower to upper) map { count =>
            Sequence(((0 until count) map { _ => sym }) map proxyIfNeeded)
        }).toSet)
        def repeat(lower: Int): Repeat = Repeat(proxyIfNeeded(sym), lower)

        // optional
        def opt = repeat(0, 1)
        def question = opt

        // more than zero
        def asterisk = repeat(0)
        def star = asterisk

        // more than once
        def plus = repeat(1)
    }
    implicit class GrammarElementJoinable(sym: Symbol) {
        def join(joinWith: Symbol): Join = new Join(proxyIfNeeded(sym), proxyIfNeeded(joinWith))
    }

    implicit class GrammarMergeable(rules: Grammar#RuleMap) {
        def merge(other: Grammar#RuleMap): Grammar#RuleMap = {
            // Merge or replace items of `rules` to items of `other`
            val mutableRules = scala.collection.mutable.ListMap[String, ListSet[Symbols.Symbol]](rules.toSeq: _*)
            other foreach { item => mutableRules += item }
            ListMap(mutableRules.toSeq: _*)
        }
    }
}
