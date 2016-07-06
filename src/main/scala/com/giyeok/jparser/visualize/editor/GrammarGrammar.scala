package com.giyeok.jparser.visualize.editor

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.jparser.Symbols._

object GrammarGrammar extends Grammar {
    val whitespace = chars(" \t\n\r").star
    val inlineWS: Set[Symbol] = Set(chars(" \t"))
    val name = "Grammar Notation"
    val rules: RuleMap = ListMap(
        "Grammar" -> ListSet(
            seq(whitespace,
                n("Rules"),
                whitespace)),
        "Rules" -> ListSet(
            seq(n("Rules"), whitespace, n("NontermDef")),
            n("NontermDef")),
        "NontermDef" -> ListSet(
            seq(inlineWS, n("NontermName"), c('='), n("Productions"))),
        "Productions" -> ListSet(
            n("Production"),
            seq(n("Productions"), whitespace, c('|'), chars(" \t").star, n("Production"))),
        "Production" -> ListSet(
            i("<empty>"),
            n("Symbols")),
        "Symbols" -> ListSet(
            n("Symbol"),
            seq(inlineWS, n("Symbols"), n("Symbol"))),
        "Symbol" -> ListSet(
            n("NontermName"),
            n("LongestName"),
            n("LookaheadName"),
            n("LookaheadExName"),
            n("IntersectionName"),
            n("ExclusionName"),
            n("Terminal")),
        "NontermName" -> ListSet(
            longest(oneof(
                oneof(chars('a' to 'z', 'A' to 'Z', '0' to '9')).plus,
                seq(c('`'), oneof(chars('a' to 'z', 'A' to 'Z', '0' to '9'), chars("+*[]-")).plus)))),
        "LongestName" -> ListSet(
            longest(seq(inlineWS, c('L'), c('('), n("Symbol"), c(')')))),
        "LookaheadName" -> ListSet(
            longest(seq(inlineWS, i("la"), c('('), n("Symbol"), c(')')))),
        "LookaheadExName" -> ListSet(
            longest(seq(inlineWS, i("ix"), c('('), n("Symbol"), c(')')))),
        "IntersectionName" -> ListSet(
            longest(seq(inlineWS, n("Symbol"), c('&'), n("Symbol")))),
        "ExclusionName" -> ListSet(
            longest(seq(inlineWS, n("Symbol"), c('-'), n("Symbol")))),
        "Terminal" -> ListSet(
            seq(c('\''), anychar, c('\'')),
            n("TerminalRange"),
            n("TerminalSet")),
        "TerminalRange" -> ListSet(
            seq(c('['), seq(anychar, c('-'), anychar).plus, c(']'))),
        "TerminalSet" -> ListSet(
            seq(c('{'), anychar.except(c('-')).plus, c('}'))))
    val startSymbol = n("Grammar")
}
