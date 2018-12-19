package com.giyeok.jparser.examples1

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._

import scala.collection.immutable.{ListMap, ListSet}

object JoinGrammar1 extends GrammarWithStringSamples {
    def token(sym: Symbol): Symbol = sym.join(n("Token"))
    def expr(syms: Symbol*): Symbol = seqWS(token(n("Whitespace")).opt, syms: _*)

    val name = "JoinGrammar1"

    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            expr(token(i("var")), n("Id"), token(i("=")), n("Expr"))
        ),
        "Id" -> ListSet(
            token(chars('a' to 'z', 'A' to 'Z').plus)
        ),
        "Expr" -> ListSet(
            n("Number"),
            expr(n("Number"), token(chars("+-*/")), n("Expr")),
            expr(token(i("(")), n("Expr"), token(i(")")))
        ),
        "Number" -> ListSet(
            token(seq(chars('1' to '9'), chars('0' to '9').plus))
        ),
        "Token" -> ListSet(
            seq(n("NumChar").plus, lookahead_except(n("NumChar"))),
            seq(n("ChChar").plus, lookahead_except(n("ChChar"))),
            n("PuncChar"),
            seq(n("WsChar").plus, lookahead_except(n("WsChar")))
        ),
        "NumChar" -> ListSet(chars('0' to '9')),
        "ChChar" -> ListSet(chars('A' to 'Z', 'a' to 'z')),
        "PuncChar" -> ListSet(chars("=+-*/()")),
        "WsChar" -> ListSet(chars(" \t\n")),
        "Whitespace" -> ListSet(chars(" \t\n").plus)
    )
    val start = "S"

    val validInputs = Set(
        "var xyz = (123 + 456)"
    )
    val invalidInputs = Set()
}

object JoinGrammar2 extends GrammarWithStringSamples {
    val name = "JoinGrammar2 (nullable case)"

    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("A").join(n("B"))
        ),
        "A" -> ListSet(
            chars('a' to 'z').star
        ),
        "B" -> ListSet(
            chars('b' to 'y').star
        )
    )
    val start = "S"

    val validInputs = Set(
        "bcd"
    )
    val invalidInputs = Set(
        "bca"
    )
}

object JoinGrammar3 extends GrammarWithStringSamples {
    val name = "JoinGrammar3 (a^n b^n c^n)"

    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("L1").join(n("L2"))
        ),
        "L1" -> ListSet(
            seq(n("A"), n("P"))
        ),
        "A" -> ListSet(
            seq(c('a'), n("A"), c('b')),
            empty
        ),
        "P" -> ListSet(
            seq(c('c'), n("P")),
            empty
        ),
        "L2" -> ListSet(
            seq(n("Q"), n("C"))
        ),
        "Q" -> ListSet(
            seq(c('a'), n("Q")),
            empty
        ),
        "C" -> ListSet(
            seq(c('b'), n("C"), c('c')),
            empty
        )
    )
    val start = "S"

    val validInputs = Set(
        "", "abc", "aabbcc", "aaabbbccc"
    )
    val invalidInputs = Set(
        "aaabbb"
    )
}

object JoinGrammar3_1 extends GrammarWithStringSamples {
    val name = "JoinGrammar3_1 (a^n b^n c^n)"

    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("P").join(n("Q"))
        ),
        "P" -> ListSet(
            seq(n("A"), c('c').plus)
        ),
        "A" -> ListSet(
            i("ab"),
            seq(c('a'), n("A"), c('b'))
        ),
        "Q" -> ListSet(
            seq(c('a').plus, n("B"))
        ),
        "B" -> ListSet(
            i("bc"),
            seq(c('b'), n("B"), c('c'))
        )
    )
    val start = "S"

    val validInputs = Set(
        "abc", "aabbcc", "aaabbbccc"
    )
    val invalidInputs = Set(
        "aaabbb",
        ("a" * 4) + ("b" * 3) + ("c" * 3),
        ("a" * 3) + ("b" * 4) + ("c" * 3),
        ("a" * 3) + ("b" * 3) + ("c" * 4),
        ("a" * 2) + ("b" * 3) + ("c" * 3),
        ("a" * 3) + ("b" * 2) + ("c" * 3),
        ("a" * 3) + ("b" * 3) + ("c" * 2)
    )
}

object JoinGrammars extends ExampleGrammarSet {
    val examples = Set(
        JoinGrammar1.toPair,
        JoinGrammar2.toPair,
        JoinGrammar3.toPair,
        JoinGrammar3_1.toPair
    )
}
