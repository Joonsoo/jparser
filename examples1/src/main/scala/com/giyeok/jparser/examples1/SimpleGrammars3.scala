package com.giyeok.jparser.examples1

import com.giyeok.jparser.GrammarHelper._

import scala.collection.immutable.{ListMap, ListSet}

object SimpleGrammar5 extends GrammarWithStringSamples {
    val name = "Simple Grammar 5"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A"), n("B"))),
        "A" -> ListSet(i("a"), empty),
        "B" -> ListSet(i("b"), empty)
    )
    val start = "S"

    val validInputs = Set("", "a", "b", "ab")
    val invalidInputs = Set("aa")
}

object SimpleGrammar6 extends GrammarWithStringSamples {
    val name = "Simple Grammar 6"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A"), n("C"))),
        "A" -> ListSet(seq(n("B"), i("a").star)),
        "B" -> ListSet(i("b"), empty),
        "C" -> ListSet(seq(n("B"), i("c").star))
    )
    val start = "S"

    val validInputs = Set("", "ab", "c", "ccc", "abc", "aa", "aaabccc")
    val invalidInputs = Set("cb")
}

object SimpleGrammar7_1 extends GrammarWithStringSamples {
    val name = "Simple Grammar 7-1 (right associative)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Exp")
        ),
        "Exp0" -> ListSet(
            n("Id"),
            n("Num"),
            seq(i("("), n("Num"), i(")"))
        ),
        "Exp1" -> ListSet(
            n("Exp0"),
            seq(n("Exp0"), i("*"), n("Exp1"))
        ),
        "Exp2" -> ListSet(
            n("Exp1"),
            seq(n("Exp1"), i("+"), n("Exp2"))
        ),
        "Exp" -> ListSet(
            n("Exp2")
        ),
        "Id" -> ListSet(
            longest(chars('a' to 'z').plus)
        ),
        "Num" -> ListSet(
            longest(chars('0' to '9').plus)
        )
    )
    val start = "S"

    val validInputs = Set(
        "1+2+3",
        "123+456+abc+sdf+123+wer+aasdfwer+123123",
        "123*456*abc*sdf*123*wer*aasdfwer*123123",
        "123+456*abc+sdf*123+wer*aasdfwer*123123"
    )
    val invalidInputs = Set("")
}

object SimpleGrammar7_2 extends GrammarWithStringSamples {
    val name = "Simple Grammar 7-2 (left associative)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Exp")
        ),
        "Exp0" -> ListSet(
            n("Id"),
            n("Num"),
            seq(i("("), n("Num"), i(")"))
        ),
        "Exp1" -> ListSet(
            n("Exp0"),
            seq(n("Exp1"), i("*"), n("Exp0"))
        ),
        "Exp2" -> ListSet(
            n("Exp1"),
            seq(n("Exp2"), i("+"), n("Exp1"))
        ),
        "Exp" -> ListSet(
            n("Exp2")
        ),
        "Id" -> ListSet(
            longest(chars('a' to 'z').plus)
        ),
        "Num" -> ListSet(
            longest(chars('0' to '9').plus)
        )
    )
    val start = "S"

    val validInputs = Set(
        "1+2+3",
        "123+456+abc+sdf+123+wer+aasdfwer+123123",
        "123*456*abc*sdf*123*wer*aasdfwer*123123",
        "123+456*abc+sdf*123+wer*aasdfwer*123123"
    )
    val invalidInputs = Set("")
}

object SimpleGrammar8 extends GrammarWithStringSamples {
    val name = "Simple Grammar 8"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("A").star
        ),
        "A" -> ListSet(
            seq(c('('), n("A"), c(')')),
            c('0')
        )
    )
    val start = "S"

    val validInputs = Set(
        "(((0)))(((0)))",
        "(0)(0)(0)",
        ""
    )
    val invalidInputs = Set()
}

object SimpleGrammar8_1 extends GrammarWithStringSamples {
    val name = "Simple Grammar 8-1 (ambiguous)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(c('('), n("S"), c(')')),
            seq(i("("), n("S"), i(")")),
            c('0')
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
}

object SimpleGrammar8_2 extends GrammarWithStringSamples {
    val name = "Simple Grammar 8-2 (ambiguous)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(c('a').opt, c('a').opt, c('a').opt, c('a').opt)
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
    override val ambiguousInputs = Set(
        "a"
    )
}

object SimpleGrammar9 extends GrammarWithStringSamples {
    val name = "Simple Grammar 9 (Infinitely ambiguous)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            empty,
            seq(n("S"), n("A"))
        ),
        "A" -> ListSet(
            empty,
            seq(n("A"), c('a'))
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
}

object SimpleGrammar9_1 extends GrammarWithStringSamples {
    val name = "Simple Grammar 9_1 (ambiguous)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("A"),
            seq(n("S"), n("A"))
        ),
        "A" -> ListSet(
            c('a'),
            seq(n("A"), c('a'))
        )
    )
    val start = "S"

    val validInputs = Set("a")
    val invalidInputs = Set()
    override val ambiguousInputs = Set("aaa")
}

object SimpleGrammar9_2 extends GrammarWithStringSamples {
    val name = "Simple Grammar 9_2 (ambiguous)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("S"), n("S")),
            c('a')
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
    override val ambiguousInputs = Set("aaa")
}

object SimpleGrammar10 extends GrammarWithStringSamples {
    val name = "Simple Grammar 10 (Ambiguous Reverter)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("A"), c('0'))
        ),
        "A" -> ListSet(
            longest(chars('a' to 'z').star),
            longest(chars('b' to 'z').star)
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
}

object AsteriskNullable extends GrammarWithStringSamples {
    val name = "*-nullable"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(c('a').opt.star)
    )
    val start = "S"

    val validInputs = Set("")
    val invalidInputs = Set()
    override val ambiguousInputs = Set("aa")
}

object SimpleGrammars3 extends ExampleGrammarSet {
    val examples = Set(
        SimpleGrammar5.toPair,
        SimpleGrammar6.toPair,
        SimpleGrammar7_1.toPair,
        SimpleGrammar7_2.toPair,
        SimpleGrammar8.toPair,
        SimpleGrammar8_1.toPair,
        SimpleGrammar8_2.toPair,
        SimpleGrammar9.toPair,
        SimpleGrammar9_1.toPair,
        SimpleGrammar9_2.toPair,
        SimpleGrammar10.toPair,
        AsteriskNullable.toPair)
}
