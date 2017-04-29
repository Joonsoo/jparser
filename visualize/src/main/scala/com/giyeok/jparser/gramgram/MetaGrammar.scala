package com.giyeok.jparser.gramgram

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.ParseResultTree
import com.giyeok.jparser.ParseResultTree._
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.gramgram.GrammarGrammar.RuleMap

object MetaGrammar extends Grammar {
    val name = "Meta Grammar"
    val rules: RuleMap = ListMap(
        "Grammar" -> ListSet(
            seq(n("ws").star, n("Rules"), n("ws").star)
        ),
        "Rules" -> ListSet(
            seq(n("Rules"), longest(n("ws").except(c('\n')).star), c('\n'), n("ws").star, n("Rule")),
            n("Rule")
        ),
        "Rule" -> ListSet(
            seq(n("Nonterminal"), n("ws").star, c('='), n("ws").star, n("RHSs"))
        ),
        "RHSs" -> ListSet(
            seq(n("RHSs"), n("ws").star, c('|'), n("ws").star, n("RHS")),
            n("RHS")
        ),
        "RHS" -> ListSet(
            n("Symbol"),
            empty,
            n("SymbolSeq") // Symbol이 한개일 때는 RHS로 하지 않음
        ),
        "Sequence" -> ListSet(
            empty,
            n("Symbol"),
            n("SymbolSeq")
        ),
        "SymbolSeq" -> ListSet(
            // Symbol 2개 이상
            seq(n("SymbolSeq"), n("ws").plus, n("Symbol")),
            seq(n("Symbol"), n("ws").plus, n("Symbol"))
        ),
        "Symbol" -> ListSet(
            n("Terminal"),
            n("String"),
            n("Nonterminal"),
            n("Repeat0"),
            n("Repeat1"),
            n("Optional"),
            n("Proxy"),
            n("Intersection"),
            n("Exclusion"),
            n("Lookahead"),
            n("LookaheadNot"),
            n("Longest"),
            seq(c('('), n("ws").star, n("Either"), n("ws").star, c(')'))
        ),
        "Terminal" -> ListSet(
            seq(c('\''), n("char"), c('\'')),
            seq(c('{'), c('-').opt, oneof(n("char").except(c('-')), seq(n("char"), c('-'), n("char"))).plus, c('}'))
        ),
        "String" -> ListSet(
            seq(c('\"'), n("stringChar").star, c('\"'))
        ),
        "Nonterminal" -> ListSet(
            c(('a' to 'z').toSet ++ ('A' to 'Z').toSet + '_').plus
        ),
        "Repeat0" -> ListSet(
            seq(n("Symbol"), c('*'))
        ),
        "Repeat1" -> ListSet(
            seq(n("Symbol"), c('+'))
        ),
        "Optional" -> ListSet(
            seq(n("Symbol"), c('?'))
        ),
        "Proxy" -> ListSet(
            seq(c('['), n("ws").star, n("Sequence"), n("ws").star, c(']'))
        ),
        "Either" -> ListSet(
            seq(n("Either"), n("ws").star, c('|'), n("ws").star, n("Symbol")),
            n("Symbol")
        ),
        "Intersection" -> ListSet(
            seq(n("Symbol"), n("ws").star, c('&'), n("ws").star, n("Symbol"))
        ),
        "Exclusion" -> ListSet(
            seq(n("Symbol"), n("ws").star, c('-'), n("ws").star, n("Symbol"))
        ),
        "Lookahead" -> ListSet(
            seq(c('~'), n("ws").star, n("Symbol"))
        ),
        "LookaheadNot" -> ListSet(
            seq(c('!'), n("ws").star, n("Symbol"))
        ),
        "Longest" -> ListSet(
            seq(c('<'), n("ws").star, n("Symbol"), n("ws").star, c('>'))
        ),
        "char" -> ListSet(
            anychar.except(c('\\')),
            seq(c('\\'), c("nrbt\"\'\\".toSet)),
            seq(c('\\'), c('u'), c("0123456789abcdefABCDEF".toSet).repeat(4, 4))
        ),
        "stringChar" -> ListSet(
            anychar.except(c("\\\"".toSet)),
            seq(c('\\'), c("nrbt\"\'\\".toSet)),
            seq(c('\\'), c('u'), c("0123456789abcdefABCDEF".toSet).repeat(4, 4))
        ),
        "ws" -> ListSet(
            chars(" \t\n\r")
        )
    )
    val startSymbol: Nonterminal = n("Grammar")
}
