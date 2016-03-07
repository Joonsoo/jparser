package com.giyeok.moonparser.grammars

import scala.collection.immutable.ListMap
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Symbols.Symbol
import com.giyeok.moonparser.SymbolHelper._
import scala.collection.immutable.ListSet

object ScalaGrammar extends Grammar {

    private val delimiter = Set[Symbol](n("WhiteSpace"), n("LineTerminator"), n("Comment"))
    private val oneline = Set[Symbol](n("WhiteSpace"), n("Comment"))

    def expr(s: Symbol*) = seq(delimiter, s: _*)
    def lex(s: Symbol*) = seq(s: _*)
    def line(s: Symbol*) = seq(oneline, s: _*)

    override val name = "Scala"
    // http://www.scala-lang.org/docu/files/ScalaReference.pdf
    override val rules: RuleMap = ListMap(
        // lexical syntax
        "upper" -> ListSet(c('A', 'Z'), chars("$_"), unicode("Lu")),
        "lower" -> ListSet(c('a', 'z'), unicode("Ll")),
        "letter" -> ListSet(n("upper"), n("lower"), unicode("Lo", "Lt", "Nl")),
        "digit" -> ListSet(c('0', '9')),
        "opchar" -> ListSet(oneof(c('\u0020', '\u007F'), unicode("Sm, So")).except(chars("()[]{}.,"))),

        "op" -> ListSet(n("opchar").plus),
        "varid" -> ListSet(seq(n("lower"), n("idrest"))),
        "plainid" -> ListSet(seq(n("upper"), n("idrest")), n("varid"), n("op")),
        "id" -> ListSet(n("plainid"), seq(i("`"), n("stringLit"), i("`"))),
        "idrest" -> ListSet(seq(oneof(n("letter"), n("digit")).star, seq(i("_"), n("op")).opt)),

        "integerLiteral" -> ListSet(seq(oneof(n("decimalNumber"), n("hexNumeral"), n("octalNumber")), chars("Ll").opt)),
        "decimalNumeral" -> ListSet(i("0"), seq(n("nonZeroDigit"), n("digit").star)),
        "hexNumeral" -> ListSet(seq(i("0x"), n("hexDigit").plus)),
        "octalNumeral" -> ListSet(seq(i("0"), n("octalDigit").plus)),
        "digit" -> ListSet(i("0"), n("nonZeroDigit")),
        "nonZeroDigit" -> ListSet(c('1', '9')),
        "octalDigit" -> ListSet(c('0', '7')),

        "floatingPointLiteral" -> ListSet(
            seq(n("digit").plus, i("."), n("digit").star, n("exponentPart").opt, n("floatType").opt),
            seq(i("."), n("digit").plus, n("exponentPart").opt, n("floatType").opt),
            // modified not to be ambiguous
            seq(n("digit").plus, n("exponentPart"), n("floatType")),
            seq(n("digit").plus, n("exponentPart")),
            seq(n("digit").plus, n("floatType"))),
        "exponentPart" -> ListSet(seq(chars("Ee"), chars("+-").opt, n("digit").plus)),
        "floatType" -> ListSet(chars("FfDd")),
        "booleanLiteral" -> ListSet(i("true"), i("false")),
        "characterLiteral" -> ListSet(
            seq(i("`"), n("printableChar"), i("`")),
            seq(i("`"), n("charEscapeSeq"), i("`"))),
        "stringLiteral" -> ListSet(
            seq(i("\""), n("stringElement").star, i("\"")),
            seq(i("\"\"\""), n("multiLineChars"), i("\"\"\""))),
        "stringElement" -> ListSet(
            n("printableCharNoDoubleQuote"),
            n("charEscapeSeq")),
        "multiLineChars" -> ListSet(
            seq(seq(i("\"").opt, i("\"").opt, n("charNoDoubleQuote")).star, i("\"").star)),
        "symbolLiteral" -> ListSet(
            seq(i("'"), n("plainid"))),
        "comment" -> ListSet(
            seq(i("/*"), seq(c, lookahead_except(i("*/"))).star, i("*/")),
            seq(i("//"), c.butnot(n("nl")).star)),
        "nl" -> ListSet(// TODO
        ),
        "semi" -> ListSet(i(";"), n("nl").plus),

        // context-free syntax
        "Literal" -> ListSet(
            seq(i("-").opt, n("integerLiteral")),
            seq(i("-").opt, n("floatingPointLiteral")),
            n("booleanLiteral"),
            n("characterLiteral"),
            n("stringLiteral"),
            n("symbolLiteral"),
            i("null")),
        "QualId" -> ListSet(seq(n("id"), seq(i("."), n("id")).star)),
        "ids" -> ListSet(seq(n("id"), seq(i(","), n("id")).star)),
        "Path" -> ListSet(n("StableId"), seq(seq(n("id"), i(".")).opt, i("this"))),
        "StableId" -> ListSet(
            n("id"),
            seq(n("Path"), i("."), n("id")),
            seq(seq(n("id"), i(".")).opt, i("super"), n("ClassQualifier").opt, i("."), n("id"))),
        "ClassQualifier" -> ListSet(seq(i("["), n("id"), i("]"))),
        "Type" -> ListSet(
            seq(n("FunctionArgTypes"), i("=>"), n("Type")),
            seq(n("InfixType"), n("ExistentialClause").opt)),
        "FunctionArgTypes" -> ListSet(
            n("InfixType"),
            seq(i("("), seq(n("ParamType"), seq(i(","), n("ParamType")).star).opt, i(")"))),
        "ExistentialClause" -> ListSet(
            seq(i("forSome"), i("{"), n("ExistentialDcl"), seq(n("semi"), n("ExistentialDcl")).star, i("}"))),
        "ExistentialDcl" -> ListSet(
            seq(i("type"), n("TypeDcl")),
            seq(i("val"), n("ValDcl"))),
        "InfixType" -> ListSet(
            seq(n("CompoundType"), seq(n("id"), n("nl").opt, n("CompoundType")).star)),
        "CompoundType" -> ListSet(
            seq(n("AnnotType"), seq(i("with"), n("AnnotType")).star, n("Refinement").opt),
            n("Refinement")),
        "AnnotType" -> ListSet(
            seq(n("SimpleType"), n("Annotation").star)) // TODO finish this
    )
    override val startSymbol = n("CompilationUnit")
}
