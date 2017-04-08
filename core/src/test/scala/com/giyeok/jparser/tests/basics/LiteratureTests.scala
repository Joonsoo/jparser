package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.tests.GrammarTestCases
import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.tests.StringSamples
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.jparser.tests.AmbiguousSamples

object Earley1970AE extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("E")

    val grammar = this
    val correctSamples = Set[String](
        "a+a*a"
    )
    val incorrectSamples = Set[String]()
}

object Knuth1965_24 extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "",
        "bbbbbaaaaa",
        "aaabaaabaaabbbbbbb",
        "ababbaba",
        "ababbbabaababababababababbabababaaabab"
    )
    val incorrectSamples = Set[String]()
}

object MyPaper1 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]("aab")
}

object MyPaper2 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]("abz")
}

object MyPaper2_1 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]("abz")
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]()
}

object MyPaper3 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]("12+34")
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]()
}

object MyPaper4 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
    val name = "MyPaper Grammar 4"
    val rules: RuleMap = ListMap(
        "A" -> ListSet(
            seq(n("A"), chars('a' to 'z')),
            chars('a' to 'z')
        )
    )
    val startSymbol = n("A")

    val grammar = this
    val correctSamples = Set[String]("ab")
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]()
}

object MyPaper5 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]("abc  def")
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]()
}

object MyPaper6_1 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]("ifx(ifx)abc()")
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]()
}

object MyPaper6_2 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]("ifx();")
    val incorrectSamples = Set[String]("if();")
    val ambiguousSamples = Set[String]()
}

object MyPaper6_3 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "let a b c;",
        "l a b c;let a b;",
        "a b c d;let x y;",
        "a b letx c d;let z y;",
        "let a b c d e f g;",
        "abc def;let xyz zyx;",
        "ab cd;let wx yz;"
    )
    val incorrectSamples = Set[String](
        "let let a;",
        "a b let c d;",
        "l a b c;",
        "a b c d;",
        "a b letx c d;"
    )
    val ambiguousSamples = Set[String]()
}

object MyPaper6_4 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "let a b+c;",
        "l+a+b+c;let a b;",
        "a+b+c+d;let x y;",
        "a+b+letx+c+d;let z y;",
        "let a b+c+d+e+f+g;",
        "abc+def;let xyz zyx;",
        "ab+cd;let wx yz;"
    )
    val incorrectSamples = Set[String](
        "ab+cd",
        "let let a;",
        "a b let c d;",
        "l a b c;",
        "a b c d;",
        "a b letx c d;"
    )
    val ambiguousSamples = Set[String]()
}

object MyPaper6_5 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
    val name = "MyPaper Grammar 6_5 (paper example)"
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "let a b+c;",
        "l+a+b+c;let a b;",
        "a+b+c+d;let x y;",
        "a+b+letx+c+d;let z y;",
        "let a b+c+d+e+f+g;",
        "abc+def;let xyz zyx;",
        "ab+cd;let wx yz;"
    )
    val incorrectSamples = Set[String](
        "ab+cd",
        "let let a;",
        "a b let c d;",
        "l a b c;",
        "a b c d;",
        "a b letx c d;"
    )
    val ambiguousSamples = Set[String]()
}

object MyPaper6_6 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "let a b+c;",
        "l+a+b+c;let a b;",
        "a+b+c+d;let x y;",
        "a+b+letx+c+d;let z y;",
        "let a b+c+d+e+f+g;",
        "abc+def;let xyz zyx;",
        "ab+cd;let wx yz;"
    )
    val incorrectSamples = Set[String](
        "ab+cd",
        "let let a;",
        "a b let c d;",
        "l a b c;",
        "a b c d;",
        "a b letx c d;"
    )
    val ambiguousSamples = Set[String]()
}

object MyPaper7_1 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set[String](
        "a",
        "b",
        "c"
    )
    val ambiguousSamples = Set[String]()
}

object MyPaper7_2 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "b",
        "c"
    )
    val incorrectSamples = Set[String](
        "a"
    )
    val ambiguousSamples = Set[String]()
}

object MyPaper8_1 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "b",
        "c"
    )
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String](
        "ab",
        "bc"
    )
}

object MyPaper8_2 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "b",
        "c",
        "ab",
        "bc",
        "abcdefg"
    )
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]()
}

object MyPaper8_3 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "b",
        "c",
        "ab",
        "bc",
        "abcdefg"
    )
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]()
}

object MyPaper9_1 extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "aaaaaa",
        "aaaaaaa",
        "aaaaaaaaaaa",
        "aaaaaaaaaaaa"
    )
    val incorrectSamples = Set[String](
        "",
        "a",
        "aa",
        "aaa",
        "aaaa",
        "aaaaa",
        "aaaaaaaa"
    )
}

object MyPaper9_2 extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("K")

    val grammar = this
    val correctSamples = Set[String](
        "aaaaaa",
        "aaaaaaa",
        "aaaaaaaaaaa",
        "aaaaaaaaaaaa"
    )
    val incorrectSamples = Set[String](
        "",
        "a",
        "aa",
        "aaa",
        "aaaa",
        "aaaaa",
        "aaaaaaaa"
    )
}

object MyPaper9_3 extends Grammar with GrammarTestCases with StringSamples {
    val name = "MyPaper Grammar 9_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("A"), longest(n("A")))
        ),
        "A" -> ListSet(
            c('a').plus
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
}

object MyPaper9_4 extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
}

object MyPaper9_5 extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]("aaabbc")
    val incorrectSamples = Set[String]()
}

object MyPaper9_6 extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "aaaaaaa",
        "aaaaaaaaa"
    )
    val incorrectSamples = Set[String]()
}

object MyPaper9_7 extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "aaaaaaa",
        "aaaaaaaaa"
    )
    val incorrectSamples = Set[String]()
}

object MyPaper10 extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "aaaa",
        "aaaaa"
    )
    val incorrectSamples = Set[String]()
}

object MyPaper10_1 extends Grammar with GrammarTestCases with StringSamples {
    val name = "MyPaper Grammar 10_1"
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "abcdefg",
        "abcdefghi"
    )
    val incorrectSamples = Set[String]()
}

object MyPaper10_2 extends Grammar with GrammarTestCases with StringSamples {
    val name = "MyPaper Grammar 10_2"
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "abcde"
    )
    val incorrectSamples = Set[String](
        "abcd"
    )
}

object MyPaper10_3 extends Grammar with GrammarTestCases with StringSamples {
    val name = "MyPaper Grammar 10_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            longest(seq(c('a'), longest(c('a').plus)))
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "aaaaaa"
    )
    val incorrectSamples = Set[String]()
}

object MyPaper10_4 extends Grammar with GrammarTestCases with StringSamples {
    val name = "MyPaper Grammar 10_4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(longest(c('a').plus), c('a'))
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set[String](
        "aaaaaa"
    )
}

object PaperTests {
    val tests: Set[GrammarTestCases] = Set(
        Earley1970AE,
        Knuth1965_24,
        MyPaper1,
        MyPaper2,
        MyPaper2_1,
        MyPaper3,
        MyPaper4,
        MyPaper5,
        MyPaper6_1,
        MyPaper6_2,
        MyPaper6_3,
        MyPaper6_4,
        MyPaper6_5, // paper example
        MyPaper6_6, // modified paper example
        MyPaper7_1,
        MyPaper7_2,
        MyPaper8_1,
        MyPaper8_2,
        MyPaper8_3,
        MyPaper9_1,
        MyPaper9_2,
        MyPaper9_3,
        MyPaper9_4,
        MyPaper9_5,
        MyPaper9_6,
        MyPaper9_7,
        MyPaper10,
        MyPaper10_1,
        MyPaper10_2,
        MyPaper10_3,
        MyPaper10_4
    )
}

class PaperTestSuite extends BasicParseTest(PaperTests.tests)

// grammars in Parsing Techniques book (http://dickgrune.com/Books/PTAPG_1st_Edition/BookBody.pdf)

object Fig6_6 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]("aabc")
}

object Fig7_4 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
    val name = "Parsing Techniques Grammar in Figure 7.4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(c('a'), n("S"), c('b')),
            seq(n("S"), c('a'), c('b')),
            seq(i("aaa"))
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]("aaaab")
}

object Fig7_8 extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "a+a",
        "a-a+a"
    )
    val incorrectSamples = Set[String]()
}

object Fig7_17 extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "aa",
        "a*a"
    )
    val incorrectSamples = Set[String]()
}

object Fig7_19 extends Grammar with GrammarTestCases with StringSamples {
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
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "p",
        "q",
        "pq"
    )
    val incorrectSamples = Set[String]()
}

object ParsingTechniquesTests {
    val tests: Set[GrammarTestCases] = Set(
        Fig7_4,
        Fig7_8,
        Fig7_19
    )
}

class ParsingTechniquesTestSuite extends BasicParseTest(ParsingTechniquesTests.tests)
