package com.giyeok.jparser.examples1

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._

import scala.collection.immutable.{ListMap, ListSet}

object Earley1970AE extends GrammarWithStringSamples {
    val name = "Earley 1970 Grammar AE"
    val rules: RuleMap = ListMap(
        "E" -> ListSet(
            n("T"),
            seq(n("E"), c('+'), n("T"))
        ),
        "T" -> ListSet(
            n("P"),
            seq(n("T"), c('*'), n("P"))
        ),
        "P" -> ListSet(
            c('a')
        )
    )
    val start = "E"

    val validInputs = Set(
        "a+a*a"
    )
    val invalidInputs = Set()
}

object Knuth1965_24 extends GrammarWithStringSamples {
    val name = "Knuth 1965 Grammar 24"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            empty,
            seq(c('a'), n("A"), c('b'), n("S")),
            seq(c('b'), n("B"), c('a'), n("S"))
        ),
        "A" -> ListSet(
            empty,
            seq(c('a'), n("A"), c('b'), n("A"))
        ),
        "B" -> ListSet(
            empty,
            seq(c('b'), n("B"), c('a'), n("B"))
        )
    )
    val start = "S"

    val validInputs = Set(
        "",
        "bbbbbaaaaa",
        "aaabaaabaaabbbbbbb",
        "ababbaba",
        "ababbbabaababababababababbabababaaabab")
    val invalidInputs = Set()
}

object MyPaper1 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("N").plus
        ),
        "N" -> ListSet(
            n("A"),
            n("B")
        ),
        "A" -> ListSet(
            c('a').plus
        ),
        "B" -> ListSet(
            c('b')
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
    override val ambiguousInputs = Set("aab")
}

object MyPaper2 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            oneof(n("A"), n("Z")).plus
        ),
        "A" -> ListSet(
            chars('a' to 'b').plus
        ),
        "Z" -> ListSet(
            c('z')
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
    override val ambiguousInputs = Set("abz")
}

object MyPaper2_1 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 2_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            oneof(n("A"), n("Z")).plus
        ),
        "A" -> ListSet(
            seq(chars('a' to 'b').plus, lookahead_is(n("Z")))
        ),
        "Z" -> ListSet(
            c('z')
        )
    )
    val start = "S"

    val validInputs = Set("abz")
    val invalidInputs = Set()
    override val ambiguousInputs = Set()
}

object MyPaper3 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("T"),
            seq(n("S"), n("T"))
        ),
        "T" -> ListSet(
            longest(n("N")),
            longest(n("P"))
        ),
        "N" -> ListSet(
            chars('0' to '9'),
            seq(n("N"), chars('0' to '9'))
        ),
        "P" -> ListSet(
            i("+"),
            i("++")
        )
    )
    val start = "S"

    val validInputs = Set("12+34")
    val invalidInputs = Set()
    override val ambiguousInputs = Set()
}

object MyPaper4 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 4"
    val rules: RuleMap = ListMap(
        "A" -> ListSet(
            seq(n("A"), chars('a' to 'z')),
            chars('a' to 'z')
        )
    )
    val start = "A"

    val validInputs = Set("ab")
    val invalidInputs = Set()
    override val ambiguousInputs = Set()
}

object MyPaper5 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 5"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("T").star
        ),
        "T" -> ListSet(
            n("N"),
            n("W")
        ),
        "N" -> ListSet(
            seq(n("A"), lookahead_except(n("A")))
        ),
        "A" -> ListSet(
            seq(chars('a' to 'z'), n("A")),
            chars('a' to 'z')
        ),
        "W" -> ListSet(
            seq(c(' ').plus, lookahead_except(c(' ')))
        )
    )
    val start = "S"

    val validInputs = Set("abc  def")
    val invalidInputs = Set()
    override val ambiguousInputs = Set()
}

object MyPaper6_1 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 6_1"
    val rules: RuleMap = ListMap(
        "Name" -> ListSet(
            longest(chars('a' to 'z').plus)
        ),
        "Id" -> ListSet(
            n("Name").except(n("Kw"))
        ),
        "Kw" -> ListSet(
            n("If"),
            n("Boolean")
        ),
        "If" -> ListSet(
            i("if").join(n("Name"))
        ),
        "Boolean" -> ListSet(
            i("true").join(n("Name")),
            i("false").join(n("Name"))
        ),
        "Stmt" -> ListSet(
            n("IfStmt"),
            n("FuncCall")
        ),
        "IfStmt" -> ListSet(
            seq(n("If"), c('('), n("Expr"), c(')'), n("Stmt"), n("ElseOpt"))
        ),
        "ElseOpt" -> ListSet(
            empty,
            seq(i("else").join(n("Name")), n("Stmt"))
        ),
        "FuncCall" -> ListSet(
            seq(n("Id"), c('('), n("ParamsOpt"), c(')'))
        ),
        "ParamsOpt" -> ListSet(
            empty,
            n("Params")
        ),
        "Params" -> ListSet(
            n("Expr"),
            seq(n("Params"), c(','), n("Expr"))
        ),
        "Expr" -> ListSet(
            n("Boolean"),
            n("Id")
        ),
        "S" -> ListSet(
            n("Stmt").star
        )
    )
    val start = "S"

    val validInputs = Set("ifx(ifx)abc()")
    val invalidInputs = Set()
    override val ambiguousInputs = Set()
}

object MyPaper6_2 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 6_2"
    val rules: RuleMap = ListMap(
        "Name" -> ListSet(
            longest(chars('a' to 'z').plus)
        ),
        "Id" -> ListSet(
            n("Name").except(n("Kw"))
        ),
        "Kw" -> ListSet(
            i("if").join(n("Name"))
        ),
        "Stmt" -> ListSet(
            n("Assign"),
            n("FuncCall")
        ),
        "Assign" -> ListSet(
            seq(n("Id"), c('='), n("Expr"), c(';'))
        ),
        "FuncCall" -> ListSet(
            seq(n("Id"), c('('), c(')'), c(';')),
            seq(n("Id"), c('('), n("Params"), c(')'), c(';'))
        ),
        "Params" -> ListSet(
            n("Expr"),
            seq(n("Params"), c(','), n("Expr"))
        ),
        "Expr" -> ListSet(
            n("Id")
        ),
        "S" -> ListSet(
            n("Stmt").star
        )
    )
    val start = "S"

    val validInputs = Set("ifx();")
    val invalidInputs = Set("if();")
    override val ambiguousInputs = Set()
}

object MyPaper6_3 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 6_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Stmt").plus
        ),
        "Stmt" -> ListSet(
            n("LetStmt"),
            n("ExprStmt")
        ),
        "LetStmt" -> ListSet(
            seq(n("Let"), c(' '), n("Id"), c(' '), n("Expr"), c(';'))
        ),
        "ExprStmt" -> ListSet(
            seq(n("Expr"), c(';'), lookahead_is(n("LetStmt")))
        ),
        "Let" -> ListSet(
            n("Let0").join(n("Name"))
        ),
        "Let0" -> ListSet(
            i("let")
        ),
        "Name" -> ListSet(
            longest(chars('a' to 'z').plus)
        ),
        "Id" -> ListSet(
            n("Name").except(n("Let"))
        ),
        "Expr" -> ListSet(
            seq(n("Expr"), c(' '), n("Id")),
            n("Id")
        )
    )
    val start = "S"

    val validInputs = Set(
        "let a b c;",
        "l a b c;let a b;",
        "a b c d;let x y;",
        "a b letx c d;let z y;",
        "let a b c d e f g;",
        "abc def;let xyz zyx;",
        "ab cd;let wx yz;"
    )
    val invalidInputs = Set(
        "let let a;",
        "a b let c d;",
        "l a b c;",
        "a b c d;",
        "a b letx c d;"
    )
    override val ambiguousInputs = Set()
}

object MyPaper6_4 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 6_4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Stmt").plus
        ),
        "Stmt" -> ListSet(
            n("LetStmt"),
            n("ExprStmt")
        ),
        "LetStmt" -> ListSet(
            seq(n("Let"), c(' '), n("Id"), c(' '), n("Expr"), c(';'))
        ),
        "ExprStmt" -> ListSet(
            seq(n("Expr"), c(';'), lookahead_is(n("LetStmt")))
        ),
        "Let" -> ListSet(
            n("Let0").join(n("Name"))
        ),
        "Let0" -> ListSet(
            i("let")
        ),
        "Name" -> ListSet(
            longest(chars('a' to 'z').plus)
        ),
        "Id" -> ListSet(
            n("Name").except(n("Let"))
        ),
        "Token" -> ListSet(
            n("Id"),
            c('+')
        ),
        "Expr" -> ListSet(
            n("Token").plus
        )
    )
    val start = "S"

    val validInputs = Set(
        "let a b+c;",
        "l+a+b+c;let a b;",
        "a+b+c+d;let x y;",
        "a+b+letx+c+d;let z y;",
        "let a b+c+d+e+f+g;",
        "abc+def;let xyz zyx;",
        "ab+cd;let wx yz;"
    )
    val invalidInputs = Set(
        "ab+cd",
        "let let a;",
        "a b let c d;",
        "l a b c;",
        "a b c d;",
        "a b letx c d;"
    )
    override val ambiguousInputs = Set()
}

object MyPaper6_5 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 6_5"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Stmt").plus
        ),
        "Stmt" -> ListSet(
            n("LetStmt"),
            n("ExprStmt")
        ),
        "LetStmt" -> ListSet(
            seq(n("Let"), c(' '), n("Id"), c(' '), n("Expr"), c(';'))
        ),
        "ExprStmt" -> ListSet(
            seq(n("Expr"), c(';'), lookahead_is(n("LetStmt")))
        ),
        "Let" -> ListSet(
            n("Let0").join(n("Name"))
        ),
        "Let0" -> ListSet(
            i("let")
        ),
        "Name" -> ListSet(
            longest(n("[a-z]").plus)
        ),
        "Id" -> ListSet(
            n("Name").except(n("Let"))
        ),
        "Token" -> ListSet(
            n("Id"),
            c('+')
        ),
        "Expr" -> ListSet(
            n("Token").plus
        ),
        "[a-z]" -> ListSet(
            c('l'),
            chars('a' to 'k'),
            chars('m' to 'z')
        )
    )
    val start = "S"

    val validInputs = Set(
        "let a b+c;",
        "l+a+b+c;let a b;",
        "a+b+c+d;let x y;",
        "a+b+letx+c+d;let z y;",
        "let a b+c+d+e+f+g;",
        "abc+def;let xyz zyx;",
        "ab+cd;let wx yz;"
    )
    val invalidInputs = Set(
        "ab+cd",
        "let let a;",
        "a b let c d;",
        "l a b c;",
        "a b c d;",
        "a b letx c d;"
    )
    override val ambiguousInputs = Set()
}

object MyPaper6_6 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 6_6"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Stmt").plus
        ),
        "Stmt" -> ListSet(
            n("LetStmt"),
            n("ExprStmt")
        ),
        "LetStmt" -> ListSet(
            seq(n("Let0"), c(' '), n("Id"), c(' '), n("Expr"), c(';'))
        ),
        "ExprStmt" -> ListSet(
            seq(n("Expr"), c(';'), lookahead_is(n("LetStmt")))
        ),
        "Let0" -> ListSet(
            i("let")
        ),
        "Name" -> ListSet(
            longest(n("[a-z]").plus)
        ),
        "Id" -> ListSet(
            n("Name").except(n("Let0"))
        ),
        "Token" -> ListSet(
            n("Id"),
            c('+')
        ),
        "Expr" -> ListSet(
            n("Token").plus
        ),
        "[a-z]" -> ListSet(
            c('l'),
            chars('a' to 'k'),
            chars('m' to 'z')
        )
    )
    val start = "S"

    val validInputs = Set(
        "let a b+c;",
        "l+a+b+c;let a b;",
        "a+b+c+d;let x y;",
        "a+b+letx+c+d;let z y;",
        "let a b+c+d+e+f+g;",
        "abc+def;let xyz zyx;",
        "ab+cd;let wx yz;"
    )
    val invalidInputs = Set(
        "ab+cd",
        "let let a;",
        "a b let c d;",
        "l a b c;",
        "a b c d;",
        "a b letx c d;"
    )
    override val ambiguousInputs = Set()
}

object MyPaper7_1 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 7_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("A").except(n("B")).except(n("C"))
        ),
        "A" -> ListSet(
            chars("abc")
        ),
        "B" -> ListSet(
            chars("ac")
        ),
        "C" -> ListSet(
            chars("bc")
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set(
        "a",
        "b",
        "c"
    )
    override val ambiguousInputs = Set()
}

object MyPaper7_2 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 7_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("A").except(n("B").except(n("C")))
        ),
        "A" -> ListSet(
            chars("abc")
        ),
        "B" -> ListSet(
            chars("ac")
        ),
        "C" -> ListSet(
            chars("bc")
        )
    )
    val start = "S"

    val validInputs = Set(
        "b",
        "c"
    )
    val invalidInputs = Set(
        "a"
    )
    override val ambiguousInputs = Set()
}

object MyPaper8_1 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 8_1 (ambiguous)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("N"),
            seq(n("S"), n("N"))
        ),
        "N" -> ListSet(
            chars('a' to 'z'),
            seq(n("N"), chars('a' to 'z'))
        )
    )
    val start = "S"

    val validInputs = Set(
        "b",
        "c"
    )
    val invalidInputs = Set()
    override val ambiguousInputs = Set(
        "ab",
        "bc"
    )
}

object MyPaper8_2 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 8_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("L"),
            seq(n("S"), n("L"))
        ),
        "L" -> ListSet(
            seq(n("N"), lookahead_except(n("N")))
        ),
        "N" -> ListSet(
            chars('a' to 'z'),
            seq(n("N"), chars('a' to 'z'))
        )
    )
    val start = "S"

    val validInputs = Set(
        "b",
        "c",
        "ab",
        "bc",
        "abcdefg"
    )
    val invalidInputs = Set()
    override val ambiguousInputs = Set()
}

object MyPaper8_3 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 8_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            longest(n("N")),
            seq(n("S"), longest(n("N")))
        ),
        "N" -> ListSet(
            chars('a' to 'z'),
            seq(n("N"), chars('a' to 'z'))
        )
    )
    val start = "S"

    val validInputs = Set(
        "b",
        "c",
        "ab",
        "bc",
        "abcdefg"
    )
    val invalidInputs = Set()
    override val ambiguousInputs = Set()
}

object MyPaper9_1 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 9_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(c('a'), longest(n("A"))),
            seq(c('a'), c('a'), longest(n("A")))
        ),
        "A" -> ListSet(
            i("aaaaa").plus
        )
    )
    val start = "S"

    val validInputs = Set(
        "aaaaaa",
        "aaaaaaa",
        "aaaaaaaaaaa",
        "aaaaaaaaaaaa"
    )
    val invalidInputs = Set(
        "",
        "a",
        "aa",
        "aaa",
        "aaaa",
        "aaaaa",
        "aaaaaaaa"
    )
}

object MyPaper9_2 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 9_2"
    val rules: RuleMap = ListMap(
        "K" -> ListSet(
            n("S"),
            seq(c('a'), c('a'), c('a'), n("S"))
        ),
        "S" -> ListSet(
            seq(c('a'), longest(n("A"))),
            seq(c('a'), c('a'), longest(n("A")))
        ),
        "A" -> ListSet(
            i("aaaaa").plus
        )
    )
    val start = "K"

    val validInputs = Set(
        "aaaaaa",
        "aaaaaaa",
        "aaaaaaaaaaa",
        "aaaaaaaaaaaa"
    )
    val invalidInputs = Set(
        "",
        "a",
        "aa",
        "aaa",
        "aaaa",
        "aaaaa",
        "aaaaaaaa"
    )
}

object MyPaper9_3 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 9_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("A"), longest(n("A")))
        ),
        "A" -> ListSet(
            c('a').plus
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
}

object MyPaper9_4 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 9_4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("A"), longest(n("B")))
        ),
        "A" -> ListSet(
            c('a').plus
        ),
        "B" -> ListSet(
            c('b').plus
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
}

object MyPaper9_5 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 9_5"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("T+")
        ),
        "T+" -> ListSet(
            seq(n("T+"), n("T")),
            n("T")
        ),
        "T" -> ListSet(
            longest(c('a').plus),
            longest(c('b').plus),
            longest(c('c').plus)
        )
    )
    val start = "S"

    val validInputs = Set("aaabbc")
    val invalidInputs = Set()
}

object MyPaper9_6 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 9_6"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("A"), n("B"))
        ),
        "A" -> ListSet(
            i("aa"),
            i("aaaa")
        ),
        "B" -> ListSet(
            n("C").plus
        ),
        "C" -> ListSet(
            i("aaaaa")
        )
    )
    val start = "S"

    val validInputs = Set(
        "aaaaaaa",
        "aaaaaaaaa"
    )
    val invalidInputs = Set()
}

object MyPaper9_7 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 9_7"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("A"), longest(n("B")))
        ),
        "A" -> ListSet(
            i("aa"),
            i("aaaa")
        ),
        "B" -> ListSet(
            n("C").plus
        ),
        "C" -> ListSet(
            i("aaaaa")
        )
    )
    val start = "S"

    val validInputs = Set(
        "aaaaaaa",
        "aaaaaaaaa"
    )
    val invalidInputs = Set()
}

object MyPaper10 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 10"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            longest(longest(n("A")).plus)
        ),
        "A" -> ListSet(
            c('a').plus,
            c('b').plus
        )
    )
    val start = "S"

    val validInputs = Set(
        "aaaaaaa",
        "aaaaaaaaa"
    )
    val invalidInputs = Set()
}

object MyPaper10_1 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 10_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(longest(longest(longest(n("A"))).plus))
        ),
        "A" -> ListSet(
            c('a').plus,
            c('b').plus
        )
    )
    val start = "S"

    val validInputs = Set(
        "aaaaaaa",
        "aaaaaaaaa"
    )
    val invalidInputs = Set()
}

object MyPaper11_1 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 11_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("A"), n("Gamma"))
        ),
        "A" -> ListSet(
            n("X"),
            n("Y")
        ),
        "X" -> ListSet(
            longest(seq(i("ab"), i("cd").star))
        ),
        "Y" -> ListSet(
            i("ab")
        ),
        "Gamma" -> ListSet(
            i("cdefg"),
            i("efghi")
        )
    )
    val start = "S"

    val validInputs = Set(
        "abcdefg",
        "abcdefghi"
    )
    val invalidInputs = Set()
}

object MyPaper11_2 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 11_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("A"), n("Gamma"))
        ),
        "A" -> ListSet(
            n("X"),
            n("Y")
        ),
        "X" -> ListSet(
            longest(seq(c('a'), c('b').star))
        ),
        "Y" -> ListSet(
            i("ab")
        ),
        "Gamma" -> ListSet(
            i("bcd"),
            i("cde")
        )
    )
    val start = "S"

    val validInputs = Set(
        "abcde"
    )
    val invalidInputs = Set(
        "abcd"
    )
}

object MyPaper12_1 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 12_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            longest(seq(c('a'), longest(c('a').plus)))
        )
    )
    val start = "S"

    val validInputs = Set(
        "aaaaaa"
    )
    val invalidInputs = Set()
}

object MyPaper12_2 extends GrammarWithStringSamples {
    val name = "MyPaper Grammar 12_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(longest(c('a').plus), c('a'))
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set(
        "aaaaaa"
    )
}

object PaperSamples extends ExampleGrammarSet {
    val examples = Set(
        Earley1970AE.toPair,
        Knuth1965_24.toPair,
        MyPaper1.toPair,
        MyPaper2.toPair,
        MyPaper2_1.toPair,
        MyPaper3.toPair,
        MyPaper4.toPair,
        MyPaper5.toPair,
        MyPaper6_1.toPair,
        MyPaper6_2.toPair,
        MyPaper6_3.toPair,
        MyPaper6_4.toPair,
        MyPaper6_5.toPair, // paper example
        MyPaper6_6.toPair, // modified paper example
        MyPaper7_1.toPair,
        MyPaper7_2.toPair,
        MyPaper8_1.toPair,
        MyPaper8_2.toPair,
        MyPaper8_3.toPair,
        MyPaper9_1.toPair,
        MyPaper9_2.toPair,
        MyPaper9_3.toPair,
        MyPaper9_4.toPair,
        MyPaper9_5.toPair,
        MyPaper9_6.toPair,
        MyPaper9_7.toPair,
        MyPaper10.toPair,
        MyPaper10_1.toPair,
        MyPaper11_1.toPair,
        MyPaper11_2.toPair,
        MyPaper12_1.toPair,
        MyPaper12_2.toPair
    )
}

// grammars in Parsing Techniques book (http://dickgrune.com/Books/PTAPG_1st_Edition/BookBody.pdf)

object Fig6_6 extends GrammarWithStringSamples {
    val name = "Parsing Techniques Grammar in Figure 6.6"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("D"), n("C")),
            seq(n("A"), n("B"))
        ),
        "A" -> ListSet(
            c('a'),
            seq(c('a'), n("A"))
        ),
        "B" -> ListSet(
            i("bc"),
            seq(c('b'), n("B"), c('c'))
        ),
        "D" -> ListSet(
            i("ab"),
            seq(c('a'), n("D"), c('b'))
        ),
        "C" -> ListSet(
            c('c'),
            seq(c('c'), n("C"))
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
    override val ambiguousInputs = Set("aabc")
}

object Fig7_4 extends GrammarWithStringSamples {
    val name = "Parsing Techniques Grammar in Figure 7.4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(c('a'), n("S"), c('b')),
            seq(n("S"), c('a'), c('b')),
            seq(i("aaa"))
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
    override val ambiguousInputs = Set("aaaab")
}

object Fig7_8 extends GrammarWithStringSamples {
    val name = "Parsing Techniques Grammar in Figure 7.8"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("E")
        ),
        "E" -> ListSet(
            seq(n("E"), n("Q"), n("F")),
            n("F")
        ),
        "F" -> ListSet(
            c('a')
        ),
        "Q" -> ListSet(
            c('+'),
            c('-')
        )
    )
    val start = "S"

    val validInputs = Set(
        "a+a",
        "a-a+a"
    )
    val invalidInputs = Set()
}

object Fig7_17 extends GrammarWithStringSamples {
    val name = "Parsing Techniques Grammar in Figure 7.17"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("E")
        ),
        "E" -> ListSet(
            seq(n("E"), n("Q"), n("F")),
            n("F")
        ),
        "F" -> ListSet(
            c('a')
        ),
        "Q" -> ListSet(
            c('*'),
            c('/'),
            seq()
        )
    )
    val start = "S"

    val validInputs = Set(
        "aa",
        "a*a"
    )
    val invalidInputs = Set()
}

object Fig7_19 extends GrammarWithStringSamples {
    val name = "Parsing Techniques Grammar in Figure 7.19"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("A"),
            seq(n("A"), n("B")),
            n("B")
        ),
        "A" -> ListSet(
            n("C")
        ),
        "B" -> ListSet(
            n("D")
        ),
        "C" -> ListSet(
            c('p')
        ),
        "D" -> ListSet(
            c('q')
        )
    )
    val start = "S"

    val validInputs = Set(
        "p",
        "q",
        "pq"
    )
    val invalidInputs = Set()
}

object ParsingTechniquesTests extends ExampleGrammarSet {
    val examples = Set(
        Fig7_4.toPair,
        Fig7_8.toPair,
        Fig7_19.toPair
    )
}
