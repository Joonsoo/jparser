package com.giyeok.jparser.examples1

import com.giyeok.jparser.GrammarHelper._

import scala.collection.immutable.{ListMap, ListSet}

object LookaheadExceptGrammar1 extends GrammarWithStringSamples {
    val name = "LookaheadExceptGrammar1 - longest match"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(
            seq(chars('a' to 'z').star, lookahead_except(chars('a' to 'z'))),
            chars(" ")))
    val start = "S"

    val validInputs = Set(
        "abc",
        "abc def")
    val invalidInputs = Set()
}

object LookaheadExceptGrammar2 extends GrammarWithStringSamples {
    val name = "LookaheadExceptGrammar2 - longest match"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(
            seq(chars('a' to 'z').star, lookahead_except(seq(chars('a' to 'z')))),
            chars(" ")))
    val start = "S"

    val validInputs = Set(
        "abc",
        "abc def")
    val invalidInputs = Set()
}

object LookaheadExceptGrammar3 extends GrammarWithStringSamples {
    val name = "LookaheadExceptGrammar3 - longest match"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(seq(chars('a' to 'z').plus, lookahead_except(chars('a' to 'z'))), chars(" ")))
    val start = "S"

    val validInputs = Set(
        "abc",
        "abc def")
    val invalidInputs = Set()
}

object LookaheadExceptGrammar3_1 extends GrammarWithStringSamples {
    val name = "LookaheadExceptGrammar3_1 - longest match"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("T").star),
        "T" -> ListSet(seq(oneof(n("I").except(n("K")), n("K")), lookahead_except(chars('a' to 'z'))), n("WS")),
        "I" -> ListSet(chars('a' to 'z').plus),
        "K" -> ListSet(i("if"), i("for")),
        "WS" -> ListSet(chars(" ")))
    val start = "S"

    val validInputs = Set(
        "abc",
        "abc def",
        "if abc",
        "ifk ifk if ifk",
        "if",
        "ifk")
    val invalidInputs = Set()
}

object LookaheadExceptGrammar3_2 extends GrammarWithStringSamples {
    val name = "LookaheadExceptGrammar3_2 - longest match"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("T").star),
        "T" -> ListSet(seq(oneof(n("I"), n("K")), lookahead_except(chars('a' to 'z'))), n("WS")),
        "I" -> ListSet(chars('a' to 'z').plus.except(n("K"))),
        "K" -> ListSet(i("if"), i("for")),
        "WS" -> ListSet(chars(" ")))
    val start = "S"

    val validInputs = Set(
        "abc",
        "abc def",
        "if abc",
        "ifk ifk if ifk",
        "if",
        "ifk")
    val invalidInputs = Set()
}

object LookaheadExceptGrammar4 extends GrammarWithStringSamples {
    val name = "LookaheadExceptGrammar4 - longest match"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(
            n("Id"),
            n("Num"),
            n("WS"),
            n("Str"),
            chars("()+-*/.,")),
        "Id" -> ListSet(
            seq(chars('a' to 'z'), chars('a' to 'z', '0' to '9').star, lookahead_except(chars('a' to 'z', '0' to '9')))),
        "Num" -> ListSet(
            seq(chars('0' to '9'), lookahead_except(chars('0' to '9')))),
        "WS" -> ListSet(
            chars(" \t\n")),
        "Str" -> ListSet(
            seq(c('"'), chars('a' to 'z', 'A' to 'Z', '0' to '9', ' ' to ' ').star, c('"'))))
    val start = "S"

    val validInputs = Set(
        "abc",
        "abc def")
    val invalidInputs = Set()
}

object LookaheadExceptGrammar5 extends GrammarWithStringSamples {
    val name = "LookaheadExceptGrammar5 - longest match"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(
            n("Id"),
            n("Num"),
            n("WS"),
            n("Str"),
            chars("()+-*/.,")),
        "Id" -> ListSet(
            seq(chars('a' to 'z'), chars('a' to 'z').star, lookahead_except(chars('a' to 'z')))),
        "Num" -> ListSet(
            seq(chars('0' to '9'), lookahead_except(chars('0' to '9')))),
        "WS" -> ListSet(
            chars(" \t\n")),
        "Str" -> ListSet(
            seq(c('"'), chars('a' to 'z', 'A' to 'Z', '0' to '9', ' ' to ' ').star, c('"'))))
    val start = "S"

    val validInputs = Set(
        "abc",
        "abc def")
    val invalidInputs = Set()
}

object LookaheadExceptGrammar6 extends GrammarWithStringSamples {
    val name = "LookaheadExceptGrammar6 - longest match"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(
            n("Id"),
            n("Num"),
            n("WS"),
            n("Str"),
            chars("()+-*/.,")),
        "Id" -> ListSet(
            seq(chars('a' to 'z').star, chars('a' to 'z'), lookahead_except(chars('a' to 'z')))),
        "Num" -> ListSet(
            seq(chars('0' to '9'), lookahead_except(chars('0' to '9')))),
        "WS" -> ListSet(
            chars(" \t\n")),
        "Str" -> ListSet(
            seq(c('"'), chars('a' to 'z', 'A' to 'Z', '0' to '9', ' ' to ' ').star, c('"'))))
    val start = "S"

    val validInputs = Set(
        "abc",
        "abc def")
    val invalidInputs = Set()
}

object LookaheadExceptGrammar7 extends GrammarWithStringSamples {
    val name = "LookaheadExceptGrammar7"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(
            seq(chars('a' to 'z').star, lookahead_except(c(' '))),
            c(' ')))
    val start = "S"

    val validInputs = Set(
        "abc",
        "abc")
    val invalidInputs = Set()
}

object LookaheadExceptGrammar9 extends GrammarWithStringSamples {
    val name = "LookaheadExceptGrammar9"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("Token").star),
        "Token" -> ListSet(
            n("Name"),
            n("Keyword"),
            chars(" ()")),
        "Word" -> ListSet(
            seq(n("FirstChar"), n("SecondChar").star, lookahead_except(n("SecondChar")))),
        "Name" -> ListSet(
            n("Word").except(n("Keyword"))),
        "Keyword" -> ListSet(
            i("var"),
            i("if")),
        "FirstChar" -> ListSet(
            chars('a' to 'z', 'A' to 'Z')),
        "SecondChar" -> ListSet(
            chars('a' to 'z', 'A' to 'Z', '0' to '9')))
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
}

object LookaheadGrammars extends ExampleGrammarSet {
    // Grammar 1, 2, 7 are double-* ambiguous language
    val examples = Set(
        LookaheadExceptGrammar1.toPair,
        LookaheadExceptGrammar2.toPair,
        LookaheadExceptGrammar3.toPair,
        LookaheadExceptGrammar3_1.toPair,
        LookaheadExceptGrammar3_2.toPair,
        LookaheadExceptGrammar4.toPair,
        LookaheadExceptGrammar5.toPair,
        LookaheadExceptGrammar6.toPair,
        LookaheadExceptGrammar7.toPair,
        LookaheadExceptGrammar9.toPair
    )
}
