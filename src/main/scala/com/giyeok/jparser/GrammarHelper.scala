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

    implicit class GrammarElementExcludable(self: Symbol) {
        def except(e: Symbol) = self match {
            case _: Terminal | _: Nonterminal | _: Sequence | _: OneOf | _: Except | _: Repeat | _: Join | _: Longest | _: EagerLongest =>
                Except(self, e)
            case _ => throw GrammarDefinitionException("Applied butnot to the items that cannot be")
        }
        def butnot(e: Symbol) = except(e)
        def butnot(e: Symbol*) = except(oneof(e.toSet))
    }
    implicit class GrammarElementRepeatable(self: Symbol) {
        def checking[T <: Symbol](r: => T): T = self match {
            case _: Terminal | _: Nonterminal | _: Sequence | _: OneOf | _: Repeat | _: Join => r
            case _ => throw new Exception("Applied repeat to the items that cannot be")
        }
        def repeat(lower: Int, upper: Int): Repeat = checking { RepeatBounded(self, lower, upper) }
        def repeat(lower: Int): Repeat = checking { RepeatUnbounded(self, lower) }

        // optional
        def opt = repeat(0, 1)
        def question = opt

        // more than zero
        def asterisk = repeat(0)
        def star = asterisk

        // more than once
        def plus = repeat(1)
    }
    implicit class GrammarElementBackupable(self: Symbol) {
        def backup(backup: Symbol): Backup = self match {
            case _: Nonterminal | _: Sequence =>
                // NOTE consider which elements are deserved to be backed up
                new Backup(self, backup)
            case _ => throw GrammarDefinitionException("Applied backup to the items that cannot be")
        }
    }
    implicit class GrammarElementJoinable(self: Symbol) {
        def join(joinWith: Symbol): Join = new Join(self, joinWith)
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
