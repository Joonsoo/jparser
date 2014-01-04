package com.giyeok.moonparser.grammars

import scala.collection.immutable.ListMap

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Symbols.Symbol
import com.giyeok.moonparser.SymbolHelper._

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
        "upper" -> Set(c('A', 'Z'), c("$_"), unicode("Lu")),
        "lower" -> Set(c('a', 'z'), unicode("Ll")),
        "letter" -> Set(n("upper"), n("lower"), unicode("Lo", "Lt", "Nl")),
        "digit" -> Set(c('0', '9')),
        "opchar" -> Set(oneof(c('\u0020', '\u007F'), unicode("Sm, So")).except(c("()[]{}.,"))),

        "op" -> Set(n("opchar").plus),
        "varid" -> Set(seq(n("lower"), n("idrest"))),
        "plainid" -> Set(seq(n("upper"), n("idrest")), n("varid"), n("op")),
        "id" -> Set(n("plainid"), seq(i("`"), n("stringLit"), i("`"))),
        "idrest" -> Set(seq(oneof(n("letter"), n("digit")).star, seq(i("_"), n("op")).opt)),

        "integerLiteral" -> Set(seq(oneof(n("decimalNumber"), n("hexNumeral"), n("octalNumber")), c("Ll").opt)),
        "decimalNumeral" -> Set(i("0"), seq(n("nonZeroDigit"), n("digit").star)),
        "hexNumeral" -> Set(seq(i("0x"), n("hexDigit").plus)),
        "octalNumeral" -> Set(seq(i("0"), n("octalDigit").plus)),
        "digit" -> Set(i("0"), n("nonZeroDigit")),
        "nonZeroDigit" -> Set(c('1', '9')),
        "octalDigit" -> Set(c('0', '7')),

        "floatingPointLiteral" -> Set(
            seq(n("digit").plus, i("."), n("digit").star, n("exponentPart").opt, n("floatType").opt),
            seq(i("."), n("digit").plus, n("exponentPart").opt, n("floatType").opt),
            // modified not to be ambiguous
            seq(n("digit").plus, n("exponentPart"), n("floatType")),
            seq(n("digit").plus, n("exponentPart")),
            seq(n("digit").plus, n("floatType"))),
        "exponentPart" -> Set(seq(c("Ee"), c("+-").opt, n("digit").plus)),
        "floatType" -> Set(c("FfDd")),
        "booleanLiteral" -> Set(i("true"), i("false")),
        "characterLiteral" -> Set(
            seq(i("`"), n("printableChar"), i("`")),
            seq(i("`"), n("charEscapeSeq"), i("`"))),
        "stringLiteral" -> Set(
            seq(i("\""), n("stringElement").star, i("\"")),
            seq(i("\"\"\""), n("multiLineChars"), i("\"\"\""))),
        "stringElement" -> Set(
            n("printableCharNoDoubleQuote"),
            n("charEscapeSeq")),
        "multiLineChars" -> Set(
            seq(seq(i("\"").opt, i("\"").opt, n("charNoDoubleQuote")).star, i("\"").star)),
        "symbolLiteral" -> Set(
            seq(i("'"), n("plainid"))),
        "comment" -> Set(
            seq(i("/*"), seq(c, lookahead_except(i("*/"))).star, i("*/")),
            seq(i("//"), c.butnot(n("nl")).star)),
        "nl" -> Set( // TODO
        ),
        "semi" -> Set(i(";"), n("nl").plus),

        // context-free syntax
        "Literal" -> Set(
            seq(i("-").opt, n("integerLiteral")),
            seq(i("-").opt, n("floatingPointLiteral")),
            n("booleanLiteral"),
            n("characterLiteral"),
            n("stringLiteral"),
            n("symbolLiteral"),
            i("null")),
        "QualId" -> Set(seq(n("id"), seq(i("."), n("id")).star)),
        "ids" -> Set(seq(n("id"), seq(i(","), n("id")).star)),
        "Path" -> Set(n("StableId"), seq(seq(n("id"), i(".")).opt, i("this"))),
        "StableId" -> Set(
            n("id"),
            seq(n("Path"), i("."), n("id")),
            seq(seq(n("id"), i(".")).opt, i("super"), n("ClassQualifier").opt, i("."), n("id"))),
        "ClassQualifier" -> Set(seq(i("["), n("id"), i("]"))),
        "Type" -> Set(
            seq(n("FunctionArgTypes"), i("=>"), n("Type")),
            seq(n("InfixType"), n("ExistentialClause").opt)),
        "FunctionArgTypes" -> Set(
            n("InfixType"),
            seq(i("("), seq(n("ParamType"), seq(i(","), n("ParamType")).star).opt, i(")"))),
        "ExistentialClause" -> Set(
            seq(i("forSome"), i("{"), n("ExistentialDcl"), seq(n("semi"), n("ExistentialDcl")).star, i("}"))),
        "ExistentialDcl" -> Set(
            seq(i("type"), n("TypeDcl")),
            seq(i("val"), n("ValDcl"))),
        "InfixType" -> Set(
            seq(n("CompoundType"), seq(n("id"), n("nl").opt, n("CompoundType")).star)),
        "CompoundType" -> Set(
            seq(n("AnnotType"), seq(i("with"), n("AnnotType")).star, n("Refinement").opt),
            n("Refinement")),
        "AnnotType" -> Set(
            seq(n("SimpleType"), n("Annotation").star)) // TODO finish this
            )
    override val startSymbol = "CompilationUnit"
}
