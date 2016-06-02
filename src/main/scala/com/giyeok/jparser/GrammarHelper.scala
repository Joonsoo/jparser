package com.giyeok.jparser

import scala.collection.immutable.NumericRange
import scala.collection.immutable.ListSet
import scala.collection.immutable.ListMap

object GrammarHelper {
    import Symbols._
    import utils.UnicodeUtil

    private def charSymbol(set: Set[Char]): Terminal =
        if (set.size == 1) ExactChar(set.iterator.next) else Chars(set)

    def e = Empty
    def n(name: String) = Nonterminal(name)
    def i(string: String) = Sequence(string.toCharArray() map { c => ExactChar(c) }, Set())
    def anychar = AnyChar
    def c(char: Char) = ExactChar(char)
    def c(chars: Char*) = charSymbol(chars.toSet)
    def c(chars: Set[Char]) = charSymbol(chars)
    def chars(range: NumericRange[Char]) = charSymbol(range.toSet)
    def chars(ranges: NumericRange[Char]*) = charSymbol(ranges.toSet.flatten)
    def chars(chars: String) = charSymbol(chars.toCharArray().toSet)
    def unicode(categories: String*): Terminals.Unicode = unicode(categories toSet)
    def unicode(categories: Set[String]) = Terminals.Unicode(UnicodeUtil.categoryNamesToCodes(categories))
    // def virtual(name: String) = VirtualInputElem(name)
    def seq(seq: Seq[Symbol], whitespace: Set[Symbol]) = Sequence(seq, whitespace)
    def seq(seq: Symbol*) = Sequence(seq, Set())
    def seq(whitespace: Set[Symbol], seq: Symbol*) = Sequence(seq, whitespace)
    def ws(set: Symbol*): Set[Symbol] = Set[Symbol](set: _*)
    def oneof(items: Symbol*) = OneOf(items toSet)
    def oneof(items: Set[Symbol]) = OneOf(items)
    def lookahead_is(lookahead: Symbol) = LookaheadIs(lookahead)
    def lookahead_is(lookaheads: Symbol*) = LookaheadIs(oneof(lookaheads.toSet))
    def lookahead_except(except: Symbol) = LookaheadExcept(except)
    def lookahead_except(except: Symbol*) = LookaheadExcept(oneof(except.toSet))
    def longest(sym: Symbol) = Longest(sym)
    def elongest(sym: Symbol) = EagerLongest(sym)
    def join(sym: Symbol, join: Symbol) = new Join(sym, join)

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
            if (count == 0) Empty else Sequence((0 until count).toSeq map { _ => sym }, Set())
        }).toSet)
        def repeat(lower: Int): Repeat = Repeat(sym, lower)

        // optional
        def opt = repeat(0, 1)
        def question = opt

        // more than zero
        def asterisk = repeat(0)
        def star = asterisk

        // more than once
        def plus = repeat(1)
    }
    implicit class GrammarElementBackupable(sym: Symbol) {
        def backup(backup: Symbol): Backup = new Backup(sym, backup)
    }
    implicit class GrammarElementJoinable(sym: Symbol) {
        def join(joinWith: Symbol): Join = new Join(sym, joinWith)
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
