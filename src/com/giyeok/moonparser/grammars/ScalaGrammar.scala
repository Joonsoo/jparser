package com.giyeok.moonparser.grammars

import scala.collection.immutable.ListMap

import com.giyeok.moonparser.DefItem
import com.giyeok.moonparser.Grammar

object ScalaGrammar extends Grammar {
	private val delimiter = List[DefItem](n("WhiteSpace"), n("LineTerminator"), n("Comment"))
	private val oneline = List[DefItem](n("WhiteSpace"), n("Comment"))
	
	def expr(seq: DefItem*) = sequence(delimiter, seq:_*)
	def lex(seq: DefItem*) = sequence(seq:_*)
	def line(seq: DefItem*) = sequence(oneline, seq:_*)
	
	override val name = "Scala"
	// http://www.scala-lang.org/docu/files/ScalaReference.pdf
	override val rules = ListMap(
		// lexical syntax
		"upper" -> List(c('A', 'Z'), c("$_"), unicode_categories("Lu")),
		"lower" -> List(c('a', 'z'), unicode_categories("Ll")),
		"letter" -> List(n("upper"), n("lower"), unicode_categories("Lo", "Lt", "Nl")),
		"digit" -> List(c('0', '9')),
		"opchar" -> List(oneof(c('\u0020', '\u007F'), unicode_categories("Sm, So")).except(c("()[]{}.,"))),
		
		"op" -> List(n("opchar").plus),
		"varid" -> List(seq(n("lower"), n("idrest"))),
		"plainid" -> List(seq(n("upper"), n("idrest")), n("varid"), n("op")),
		"id" -> List(n("plainid"), seq(i("`"), n("stringLit"), i("`"))),
		"idrest" -> List(seq(oneof(n("letter"), n("digit")).star, seq(i("_"), n("op")).opt)),
		
		"integerLiteral" -> List(seq(oneof(n("decimalNumber"), n("hexNumeral"), n("octalNumber")), c("Ll").opt)),
		"decimalNumeral" -> List(i("0"), seq(n("nonZeroDigit"), n("digit").star)),
		"hexNumeral" -> List(seq(i("0x"), n("hexDigit").plus)),
		"octalNumeral" -> List(seq(i("0"), n("octalDigit").plus)),
		"digit" -> List(i("0"), n("nonZeroDigit")),
		"nonZeroDigit" -> List(c('1', '9')),
		"octalDigit" -> List(c('0', '7')),
		
		"floatingPointLiteral" -> List(
			seq(n("digit").plus, i("."), n("digit").star, n("exponentPart").opt, n("floatType").opt),
			seq(i("."), n("digit").plus, n("exponentPart").opt, n("floatType").opt),
			// modified not to be ambiguous
			seq(n("digit").plus, n("exponentPart"), n("floatType")),
			seq(n("digit").plus, n("exponentPart")),
			seq(n("digit").plus, n("floatType"))
		),
		"exponentPart" -> List(seq(c("Ee"), c("+-").opt, n("digit").plus)),
		"floatType" -> List(c("FfDd")),
		"booleanLiteral" -> List(i("true"), i("false")),
		"characterLiteral" -> List(
			seq(i("`"), n("printableChar"), i("`")), 
			seq(i("`"), n("charEscapeSeq"), i("`"))
		),
		"stringLiteral" -> List(
			seq(i("\""), n("stringElement").star, i("\"")),
			seq(i("\"\"\""), n("multiLineChars"), i("\"\"\"")) 
		),
		"stringElement" -> List(
			n("printableCharNoDoubleQuote"),
			n("charEscapeSeq")
		),
		"multiLineChars" -> List(
			seq(seq(i("\"").opt, i("\"").opt, n("charNoDoubleQuote")).star, i("\"").star)
		),
		"symbolLiteral" -> List(
			seq(i("'"), n("plainid"))
		),
		"comment" -> List(
			seq(i("/*"), seq(c(), lookahead_except(i("*/"))).star, i("*/")),
			seq(i("//"), c().butnot(n("nl")).star)
		),
		"nl" -> List(
			// TODO
		),
		"semi" -> List(i(";"), n("nl").plus),
		
		// context-free syntax
		"Literal" -> List(
			seq(i("-").opt, n("integerLiteral")),
			seq(i("-").opt, n("floatingPointLiteral")),
			n("booleanLiteral"),
			n("characterLiteral"),
			n("stringLiteral"),
			n("symbolLiteral"),
			i("null")
		),
		"QualId" -> List(seq(n("id"), seq(i("."), n("id")).star)),
		"ids" -> List(seq(n("id"), seq(i(","), n("id")).star)),
		"Path" -> List(n("StableId"), seq(seq(n("id"), i(".")).opt, i("this"))),
		"StableId" -> List(
			n("id"), 
			seq(n("Path"), i("."), n("id")),
			seq(seq(n("id"), i(".")).opt, i("super"), n("ClassQualifier").opt, i("."), n("id"))
		),
		"ClassQualifier" -> List(seq(i("["), n("id"), i("]"))),
		"Type" -> List(
			seq(n("FunctionArgTypes"), i("=>"), n("Type")),
			seq(n("InfixType"), n("ExistentialClause").opt)
		),
		"FunctionArgTypes" -> List(
			n("InfixType"),
			seq(i("("), seq(n("ParamType"), seq(i(","), n("ParamType")).star).opt, i(")"))
		),
		"ExistentialClause" -> List(
			seq(i("forSome"), i("{"), n("ExistentialDcl"), seq(n("semi"), n("ExistentialDcl")).star, i("}"))
		),
		"ExistentialDcl" -> List(
			seq(i("type"), n("TypeDcl")),
			seq(i("val"), n("ValDcl"))
		),
		"InfixType" -> List(
			seq(n("CompoundType"), seq(n("id"), n("nl").opt, n("CompoundType")).star)
		),
		"CompoundType" -> List(
			seq(n("AnnotType"), seq(i("with"), n("AnnotType")).star, n("Refinement").opt),
			n("Refinement")
		),
		"AnnotType" -> List(
			seq(n("SimpleType"), n("Annotation").star)
		)
		// TODO finish this
	)
	override val startSymbol = "CompilationUnit"
}
