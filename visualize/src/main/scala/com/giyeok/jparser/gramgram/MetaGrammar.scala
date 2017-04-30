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
            Sequence(Seq(n("ws*"), n("Rules"), n("ws*")), Seq(1))
        ),
        "Rules" -> ListSet(
            Sequence(Seq(n("Rules"), longest(n("ws").except(c('\n')).star), c('\n'), n("ws*"), n("Rule")), Seq(0, 4)),
            n("Rule")
        ),
        "Rule" -> ListSet(
            seqWS(n("ws*"), n("Nonterminal"), c('='), n("RHSs"))
        ),
        "RHSs" -> ListSet(
            seqWS(n("ws*"), n("RHSs"), c('|'), n("RHS")),
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
            seqWS(n("ws+"), n("SymbolSeq"), n("Symbol")),
            seqWS(n("ws+"), n("Symbol"), n("Symbol"))
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
            seqWS(n("ws*"), c('('), n("Either"), c(')'))
        ),
        "Terminal" -> ListSet(
            c('.'), // anychar
            seq(c('\''), n("char"), c('\'')),
            seq(c('{'), c('-').opt, oneof(n("char").except(c('-')), seq(n("char"), c('-'), n("char"))).plus, c('}'))
        ),
        "String" -> ListSet(
            seq(c('\"'), n("stringChar").star, c('\"'))
        ),
        "Nonterminal" -> ListSet(
            c(('a' to 'z').toSet ++ ('A' to 'Z').toSet ++ ('0' to '9').toSet + '_').plus,
            seq(c('`'), n("nontermNameChar").star, c('`'))
        ),
        "Repeat0" -> ListSet(
            seqWS(n("ws*"), n("Symbol"), c('*'))
        ),
        "Repeat1" -> ListSet(
            seqWS(n("ws*"), n("Symbol"), c('+'))
        ),
        "Optional" -> ListSet(
            seqWS(n("ws*"), n("Symbol"), c('?'))
        ),
        "Proxy" -> ListSet(
            seqWS(n("ws*"), c('['), n("Sequence"), c(']'))
        ),
        "Either" -> ListSet(
            seqWS(n("ws*"), n("Either"), c('|'), n("Symbol")),
            n("Symbol")
        ),
        "Intersection" -> ListSet(
            seqWS(n("ws*"), n("Symbol"), c('&'), n("Symbol"))
        ),
        "Exclusion" -> ListSet(
            seqWS(n("ws*"), n("Symbol"), c('-'), n("Symbol"))
        ),
        "Lookahead" -> ListSet(
            seqWS(n("ws*"), c('~'), n("Symbol"))
        ),
        "LookaheadNot" -> ListSet(
            seqWS(n("ws*"), c('!'), n("Symbol"))
        ),
        "Longest" -> ListSet(
            seqWS(n("ws*"), c('<'), n("Symbol"), c('>'))
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
        "nontermNameChar" -> ListSet(
            anychar.except(c("\\`".toSet)),
            seq(c('\\'), c("nrbt`".toSet)),
            seq(c('\\'), c('u'), c("0123456789abcdefABCDEF".toSet).repeat(4, 4))
        ),
        "ws" -> ListSet(
            chars(" \t\n\r")
        ),
        "ws*" -> ListSet(
            longest(n("ws").star)
        ),
        "ws+" -> ListSet(
            longest(n("ws").plus)
        )
    )
    val startSymbol: Nonterminal = n("Grammar")
}
