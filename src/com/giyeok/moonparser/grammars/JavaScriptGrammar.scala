package com.giyeok.moonparser.grammars

import scala.collection.immutable.ListMap
import com.giyeok.moonparser.DefItem
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Nonterminal
import com.giyeok.moonparser.Sequence
import com.giyeok.moonparser.dynamic.TokenParserInput
import com.giyeok.moonparser.dynamic.Parser
import com.giyeok.moonparser.ParserInput
import com.giyeok.moonparser.dynamic.ParseResult
import com.giyeok.moonparser.dynamic.BlackboxParser

object JavaScriptParser {
	def getTokenizer(source: ParserInput) =
		TokenParserInput.fromGrammar(JavaScriptGrammar, "_Token", "_Raw", source)
	def getParser(source: ParserInput) =
		new Parser(JavaScriptGrammar, getTokenizer(source))
	def getBlackboxParser = 
		new BlackboxParser {
			// === parser ===
			def parse(input: ParserInput): ParseResult = {
				val parser = getParser(input)
				parser.parseAll()
				parser.result
			}
			def parse(input: String): ParseResult = parse(ParserInput.fromString(input))
		}
}

object JavaScriptGrammar extends Grammar {
	private val whitespace = List[DefItem](n("WhiteSpace"), n("LineTerminator"), n("Comment"))
	private val oneline = List[DefItem](n("WhiteSpace"), n("Comment"))
	
	def expr(seq: DefItem*) = Sequence(seq toList, whitespace)
	def lex(seq: DefItem*) = sequence(seq:_*)
	def line(seq: DefItem*) = sequence(oneline, seq:_*)
	
	override val name = "JavaScript"
	override val rules = ListMap(
		"_Token" -> (whitespace ++ List(n("IdentifierName"), n("Punctuator"), n("NumericLiteral"), n("StringLiteral"))),
		"_Raw" -> List(n("RegularExpressionLiteral")),
		
		"Start" -> List(seq(oneof(whitespace).star, n("Program"), oneof(whitespace).star)),
		
		// A.1 Lexical Grammar
		"SourceCharacter" -> List(c()),
		"InputElementDiv" -> List(
			n("WhiteSpace"), n("LineTerminator"), n("Comment"), n("Token"), n("DivPunctuator")),
		"InputElementRegExp" -> List(
			n("WhiteSpace"), n("LineTerminator"), n("Comment"), n("Token"), n("RegularExpressionLiteral")),
		"WhiteSpace" -> List(
			c("\u0009\u000B\u000C\uFEFF"), unicode_categories("Zs")),		// \u0020\u00A0  ->  already in Zs
		"LineTerminator" -> List(
			c("\n\r\u2028\u2029")),
		"LineTerminatorSequence" -> List(
			c("\n\u2028\u2029"), lex(c("\r"), lookahead_except(c("\n"))), i("\r\n")),
		"Comment" -> List(
			n("MultiLineComment"), n("SingleLineComment")),
		"MultiLineComment" -> List(
			expr(i("/*"), n("MultiLineCommentChars").opt, i("*/"))),
		"MultiLineCommentChars" -> List(
			lex(n("MultiLineNotAsteriskChar"), n("MultiLineCommentChars").opt), 
			lex(i("*"), n("PostAsteriskCommentChars").opt)),
		"PostAsteriskCommentChars" -> List(
			lex(n("MultiLineNotForwardSlashOrAsteriskChar"), n("MultiLineCommentChars").opt),
			lex(i("*"), n("PostAsteriskCommentChars").opt)),
		"MultiLineNotAsteriskChar" -> List(
			lex(n("SourceCharacter").butnot(i("*")))),
		"MultiLineNotForwardSlashOrAsteriskChar" -> List(
			lex(n("SourceCharacter").butnot(i("/"), i("*")))),
		"SingleLineComment" -> List(
			lex(i("//"), n("SingleLineCommentChars").opt)),
		"SingleLineCommentChars" -> List(
			lex(n("SingleLineCommentChar"), n("SingleLineCommentChars").opt)),
		"SingleLineCommentChar" -> List(
			n("SourceCharacter").butnot(n("LineTerminator"))),
		"Token" -> List(
			n("IdentifierName"),
			n("Punctuator"),
			n("NumericLiteral"),
			n("StringLiteral")),
		"Identifier" -> List(
			n("IdentifierName").butnot(n("ReservedWord"))),
		"IdentifierName" -> List(
			n("IdentifierStart"),
			lex(n("IdentifierName"), n("IdentifierPart"))),
		"IdentifierStart" -> List(
			n("UnicodeLetter"),
			i("$"),
			i("_"),
			lex(i("_"), n("UnicodeEscapeSequence"))),
		"IdentifierPart" -> List(
			n("IdentifierStart"),
			n("UnicodeCombiningMark"),
			n("UnicodeDigit"),
			n("UnicodeConnectorPunctuation"),
			c("\u200C\u200D")),
		"UnicodeLetter" -> List(
			unicode_categories("Lu", "Ll", "Lt", "Lm", "Lo", "Nl")),
		"UnicodeCombiningMark" -> List(
			unicode_categories("Mn", "Mc")),
		"UnicodeDigit" -> List(
			unicode_categories("Nd")),
		"UnicodeConnectorPunctuation" -> List(
			unicode_categories("Pc")),
		"ReservedWord" -> List(
			n("Keyword"),
			n("FutureReservedWord"),
			n("NullLiteral"),
			n("BooleanLiteral")),
		"Keyword" -> List(
			i("break"), i("do"), i("instanceof"), i("typeof"),
			i("case"), i("else"), i("new"), i("var"),
			i("catch"), i("finally"), i("return"), i("void"),
			i("continue"), i("for"), i("switch"), i("while"),
			i("debugger"), i("function"), i("this"), i("with"),
			i("default"), i("if"), i("throw"),
			i("delete"), i("in"), i("try")),
		"FutureReservedWord" -> List(
			i("class"), i("enum"), i("extends"), i("super"),
			i("const"), i("export"), i("import"),
			i("implements"), i("let"), i("private"), i("public"),
			i("interface"), i("package"), i("protected"), i("static"),
			i("yield")),
		"Punctuator" -> List(
			i("{"), i("}"), i("("), i(")"), i("["), i("]"),
			i("."), i(";"), i(","), i("<"), i(">"), i("<="),
			i(">="), i("=="), i("!="), i("==="), i("!=="),
			i("+"), i("-"), i("*"), i("%"), i("++"), i("--"),
			i("<<"), i(">>"), i(">>>"), i("&"), i("|"), i("^"),
			i("!"), i("~"), i("&&"), i("||"), i("?"), i(":"),
			i("="), i("+="), i("-="), i("*="), i("%="), i("<<="),
			i(">>="), i(">>>="), i("&="), i("|="), i("^=")),
		"DivPunctuator" -> List(
			i("/"), i("/=")),
		"Literal" -> List(
			n("NullLiteral"),
			n("BooleanLiteral"),
			n("NumericLiteral"),
			n("StringLiteral"),
			n("RegularExpressionLiteral")),
		"NullLiteral" -> List(
			i("null")),
		"BooleanLiteral" -> List(
			i("true"),
			i("false")),
		"NumericLiteral" -> List(
			n("DecimalLiteral"),
			n("HexIntegerLiteral")),
		"DecimalLiteral" -> List(
			lex(n("DecimalIntegerLiteral"), i("."), n("DecimalDigits").opt, n("ExponentPart").opt),
			lex(i("."), n("DecimalDigits"), n("ExponentPart").opt),
			lex(n("DecimalIntegerLiteral"), n("ExponentPart").opt)),
		"DecimalIntegerLiteral" -> List(
			i("0"),
			lex(n("NonZeroDigit"), n("DecimalDigits").opt)),
		"DecimalDigits" -> List(
			n("DecimalDigit"),
			lex(n("DecimalDigits"), n("DecimalDigit"))),
		"DecimalDigit" -> List(
			c("0123456789")),
		"NonZeroDigit" -> List(
			c("123456789")),
		"ExponentPart" -> List(
			lex(n("ExponentIndicator"), n("SignedInteger"))),
		"ExponentIndicator" -> List(
			c("eE")),
		"SignedInteger" -> List(
			n("DecimalDigits"),
			lex(i("+"), n("DecimalDigits")),
			lex(i("-"), n("DecimalDigits"))),
		"HexIntegerLiteral" -> List(
			lex(i("0x"), n("HexDigit")),
			lex(i("0X"), n("HexDigit")),
			lex(n("HexIntegerLiteral"), n("HexDigit"))),
		"HexDigit" -> List(
			c("0123456789abcdefABCDEF")),
		"StringLiteral" -> List(
			lex(i("\""), n("DoubleStringCharacters").opt, i("\"")),
			lex(i("'"), n("SingleStringCharacters").opt, i("'"))),
		"DoubleStringCharacters" -> List(
			lex(n("DoubleStringCharacter"), n("DoubleStringCharacters").opt)),
		"SingleStringCharacters" -> List(
			lex(n("SingleStringCharacter"), n("SingleStringCharacters").opt)),
		"DoubleStringCharacter" -> List(
			n("SourceCharacter").butnot(c("\"\\"), n("LineTerminator")),
			lex(i("\\"), n("EscapeSequence")),
			n("LineContinuation")),
		"SingleStringCharacter" -> List(
			n("SourceCharacter").butnot(c("'\\"), n("LineTerminator")),
			lex(i("\\"), n("EscapeSequence")),
			n("LineContinuation")),
		"LineContinuation" -> List(
			lex(i("\\"), n("LineTerminatorSequence"))),
		"EscapeSequence" -> List(
			n("CharacterEscapeSequence"),
			lex(i("0"), lookahead_except(n("DecimalDigit"))),
			n("HexEscapeSequence"),
			n("UnicodeEscapeSequence")),
		"CharacterEscapeSequence" -> List(
			n("SingleEscapeCharacter"),
			n("NonEscapeCharacter")),
		"SingleEscapeCharacter" -> List(
			c("'\"\\bfnrtv")),
		"NonEscapeCharacter" -> List(
			n("SourceCharacter").butnot(n("EscapeCharacter"), n("LineTerminator"))),
		"EscapeCharacter" -> List(
			n("SingleEscapeCharacter"),
			n("DecimalDigit"),
			c("xu")),
		"HexEscapeSequence" -> List(
			lex(i("x"), n("HexDigit"), n("HexDigit"))),
		"UnicodeEscapeSequence" -> List(
			lex(i("u"), n("HexDigit"), n("HexDigit"), n("HexDigit"), n("HexDigit"))),
		"RegularExpressionLiteral" -> List(
			lex(i("/"), n("RegularExpressionBody"), i("/"), n("RegularExpressionFlags"))),
		"RegularExpressionBody" -> List(
			lex(n("RegularExpressionFirstChar"), n("RegularExpressionChars"))),
		"RegularExpressionChars" -> List(
			lex(),
			lex(n("RegularExpressionChars"), n("RegularExpressionChar"))),
		"RegularExpressionFirstChar" -> List(
			n("RegularExpressionNonTerminator").butnot(c("*\\/[")),
			n("RegularExpressionBackslashSequence"),
			n("RegularExpressionClass")),
		"RegularExpressionChar" -> List(
			n("RegularExpressionNonTerminator").butnot(c("\\/[")),
			n("RegularExpressionBackslashSequence"),
			n("RegularExpressionClass")),
		"RegularExpressionBackslashSequence" -> List(
			lex(i("\\"), n("RegularExpressionNonTerminator"))),
		"RegularExpressionNonTerminator" -> List(
			n("SourceCharacter").butnot(n("LineTerminator"))),
		"RegularExpressionClass" -> List(
			lex(i("["), n("RegularExpressionClassChars"), i("]"))),
		"RegularExpressionClassChars" -> List(
			lex(),
			lex(n("RegularExpressionClassChars"), n("RegularExpressionClassChar"))),
		"RegularExpressionClassChar" -> List(
			n("RegularExpressionNonTerminator").butnot(c("]\\")),
			n("RegularExpressionBackslashSequence")),
		"RegularExpressionFlags" -> List(
			lex(),
			lex(n("RegularExpressionFlags"), n("IdentifierPart"))),
		
		// A.2 Number Conversions
		"StringNumericLiteral" -> List(
			n("StrWhiteSpace").opt,
			lex(n("StrWhiteSpace").opt, n("StrNumericLiteral"), n("StrWhiteSpace").opt)),
		"StrWhiteSpace" -> List(
			lex(n("StrWhiteSpaceChar"), n("StrWhiteSpace").opt)),
		"StrWhiteSpaceChar" -> List(
			n("WhiteSpace"),
			n("LineTerminator")),
		"StrNumericLiteral" -> List(
			n("StrDecimalLiteral"),
			n("HexIntegerLiteral")),
		"StrDecimalLiteral" -> List(
			n("StrUnsignedDecimalLiteral"),
			lex(i("+"), n("StrUnsignedDecimalLiteral")),
			lex(i("-"), n("StrUnsignedDecimalLiteral"))),
		"StrUnsignedDecimalLiteral" -> List(
			i("Infinity"),
			lex(n("DecimalDigits"), i("."), n("DecimalDigits").opt, n("ExponentPart").opt),
			lex(i("."), n("DecimalDigits"), n("ExponentPart").opt),
			lex(n("DecimalDigits"), n("ExponentPart").opt)),
		"DecimalDigits" -> List(
			n("DecimalDigit"),
			lex(n("DecimalDigits"), n("DecimalDigit"))),
		"DecimalDigit" -> List(
			c("0123456789")),
		"ExponentPart" -> List(
			lex(n("ExponentIndicator"), n("SignedInteger"))),
		"ExponentIndicator" -> List(
			c("eE")),
		"SignedInteger" -> List(
			n("DecimalDigits"),
			lex(i("+"), n("DecimalDigits")),
			lex(i("-"), n("DecimalDigits"))),
		"HexIntegerLiteral" -> List(
			lex(i("0x"), n("HexDigit")),
			lex(i("0X"), n("HexDigit")),
			lex(n("HexIntegerLiteral"), n("HexDigit"))),
		"HexDigit" -> List(
			c("0123456789abcdefABCDEF")),
		
		// A.3 Expressions
		"PrimaryExpression" -> List(
			i("this"),
			n("Identifier"),
			n("Literal"),
			n("ArrayLiteral"),
			n("ObjectLiteral"),
			expr(i("("), n("Expression"), i(")"))),
		"ArrayLiteral" -> List(
			expr(i("["), n("Elision").opt, i("]")),
			expr(i("["), n("ElementList"), i("]")),
			expr(i("["), n("ElementList"), i(","), n("Elision").opt, i("]"))),
		"ElementList" -> List(
			expr(n("Elision").opt, n("AssignmentExpression")),
			expr(n("ElementList"), i(","), n("Elision").opt, n("AssignmentExpression"))),
		"Elision" -> List(
			i(","),
			expr(n("Elision"), i(","))),
		"ObjectLiteral" -> List(
			expr(i("{"), i("}")),
			expr(i("{"), n("PropertyNameAndValueList"), i("}")),
			expr(i("{"), n("PropertyNameAndValueList"), i(","), i("}"))),
		"PropertyNameAndValueList" -> List(
			n("PropertyAssignment"),
			expr(n("PropertyNameAndValueList"), i(","), n("PropertyAssignment"))),
		"PropertyAssignment" -> List(
			expr(n("PropertyName"), i(":"), n("AssignmentExpression")),
			expr(i("get"), n("PropertyName"), i("("), i(")"), i("{"), n("FunctionBody"), i("}")),
			expr(i("set"), n("PropertyName"), i("("), n("PropertySetParameterList"), i(")"), i("{"), n("FunctionBody"), i("}"))),
		"PropertyName" -> List(
			n("IdentifierName"),
			n("StringLiteral"),
			n("NumericLiteral")),
		"PropertySetParameterList" -> List(
			n("Identifier")),
		"MemberExpression" -> List(
			n("PrimaryExpression"),
			n("FunctionExpression"),
			expr(n("MemberExpression"), i("["), n("Expression"), i("]")),
			expr(n("MemberExpression"), i("."), n("IdentifierName")),
			expr(i("new"), n("MemberExpression"), n("Arguments"))),
		"NewExpression" -> List(
			n("MemberExpression"),
			expr(i("new"), n("NewExpression"))),
		"CallExpression" -> List(
			expr(n("MemberExpression"), n("Arguments")),
			expr(n("CallExpression"), n("Arguments")),
			expr(n("CallExpression"), i("["), n("Expression"), i("]")),
			expr(n("CallExpression"), i("."), n("IdentifierName"))),
		"Arguments" -> List(
			expr(i("("), i(")")),
			expr(i("("), n("ArgumentList"), i(")"))),
		"ArgumentList" -> List(
			n("AssignmentExpression"),
			expr(n("ArgumentList"), i(","), n("AssignmentExpression"))),
		"LeftHandSideExpression" -> List(
			n("NewExpression"),
			n("CallExpression")),
		"PostfixExpression" -> List(
			n("LeftHandSideExpression"),
			line(n("LeftHandSideExpression"), i("++")),
			line(n("LeftHandSideExpression"), i("--"))),
		"UnaryExpression" -> List(
			n("PostfixExpression"),
			expr(i("delete"), n("UnaryExpression")),
			expr(i("void"), n("UnaryExpression")),
			expr(i("typeof"), n("UnaryExpression")),
			expr(i("++"), n("UnaryExpression")),
			expr(i("--"), n("UnaryExpression")),
			expr(i("+"), n("UnaryExpression")),
			expr(i("-"), n("UnaryExpression")),
			expr(i("~"), n("UnaryExpression")),
			expr(i("!"), n("UnaryExpression"))),
		"MultiplicativeExpression" -> List(
			n("UnaryExpression"),
			expr(n("MultiplicativeExpression"), i("*"), n("UnaryExpression")),
			expr(n("MultiplicativeExpression"), i("/"), n("UnaryExpression")),
			expr(n("MultiplicativeExpression"), i("%"), n("UnaryExpression"))),
		"AdditiveExpression" -> List(
			n("MultiplicativeExpression"),
			expr(n("AdditiveExpression"), i("+"), n("MultiplicativeExpression")),
			expr(n("AdditiveExpression"), i("-"), n("MultiplicativeExpression"))),
		"ShiftExpression" -> List(
			n("AdditiveExpression"),
			expr(n("ShiftExpression"), i("<<"), n("AdditiveExpression")),
			expr(n("ShiftExpression"), i(">>"), n("AdditiveExpression")),
			expr(n("ShiftExpression"), i(">>>"), n("AdditiveExpression"))),
		"RelationalExpression" -> List(
			n("ShiftExpression"),
			expr(n("RelationalExpression"), i("<"), n("ShiftExpression")),
			expr(n("RelationalExpression"), i(">"), n("ShiftExpression")),
			expr(n("RelationalExpression"), i("<="), n("ShiftExpression")),
			expr(n("RelationalExpression"), i(">="), n("ShiftExpression")),
			expr(n("RelationalExpression"), i("instanceof"), n("ShiftExpression"))),
		"RelationalExpressionNoIn" -> List(
			n("ShiftExpression"),
			expr(n("RelationalExpressionNoIn"), i("<"), n("ShiftExpression")),
			expr(n("RelationalExpressionNoIn"), i(">"), n("ShiftExpression")),
			expr(n("RelationalExpressionNoIn"), i("<="), n("ShiftExpression")),
			expr(n("RelationalExpressionNoIn"), i(">="), n("ShiftExpression")),
			expr(n("RelationalExpressionNoIn"), i("instanceof"), n("ShiftExpression"))),
		"EqualityExpression" -> List(
			n("RelationalExpression"),
			expr(n("EqualityExpression"), i("=="), n("RelationalExpression")),
			expr(n("EqualityExpression"), i("!=="), n("RelationalExpression")),
			expr(n("EqualityExpression"), i("==="), n("RelationalExpression")),
			expr(n("EqualityExpression"), i("!=="), n("RelationalExpression"))),
		"EqualityExpressionNoIn" -> List(
			n("RelationalExpressionNoIn"),
			expr(n("EqualityExpressionNoIn"), i("=="), n("RelationalExpressionNoIn")),
			expr(n("EqualityExpressionNoIn"), i("!=="), n("RelationalExpressionNoIn")),
			expr(n("EqualityExpressionNoIn"), i("==="), n("RelationalExpressionNoIn")),
			expr(n("EqualityExpressionNoIn"), i("!=="), n("RelationalExpressionNoIn"))),
		"BitwiseANDExpression" -> List(
			n("EqualityExpression"),
			expr(n("BitwiseANDExpression"), i("&"), n("EqualityExpression"))),
		"BitwiseANDExpressionNoIn" -> List(
			n("EqualityExpressionNoIn"),
			expr(n("BitwiseANDExpressionNoIn"), i("&"), n("EqualityExpressionNoIn"))),
		"BitwiseXORExpression" -> List(
			n("BitwiseANDExpression"),
			expr(n("BitwiseXORExpression"), i("^"), n("BitwiseANDExpression"))),
		"BitwiseXORExpressionNoIn" -> List(
			n("BitwiseANDExpressionNoIn"),
			expr(n("BitwiseXORExpressionNoIn"), i("^"), n("BitwiseANDExpressionNoIn"))),
		"BitwiseORExpression" -> List(
			n("BitwiseXORExpression"),
			expr(n("BitwiseORExpression"), i("^"), n("BitwiseXORExpression"))),
		"BitwiseORExpressionNoIn" -> List(
			n("BitwiseXORExpressionNoIn"),
			expr(n("BitwiseORExpressionNoIn"), i("^"), n("BitwiseXORExpressionNoIn"))),
		"LogicalANDExpression" -> List(
			n("BitwiseORExpression"),
			expr(n("LogicalANDExpression"), i("&&"), n("BitwiseORExpression"))),
		"LogicalANDExpressionNoIn" -> List(
			n("BitwiseORExpressionNoIn"),
			expr(n("LogicalANDExpressionNoIn"), i("&&"), n("BitwiseORExpressionNoIn"))),
		"LogicalORExpression" -> List(
			n("LogicalANDExpression"),
			expr(n("LogicalORExpression"), i("||"), n("LogicalANDExpression"))),
		"LogicalORExpressionNoIn" -> List(
			n("LogicalANDExpressionNoIn"),
			expr(n("LogicalORExpressionNoIn"), i("||"), n("LogicalANDExpressionNoIn"))),
		"ConditionalExpression" -> List(
			n("LogicalORExpression"),
			expr(n("LogicalORExpression"), i("?"), n("AssignmentExpression"), i(":"), n("AssignmentExpression"))),
		"ConditionalExpressionNoIn" -> List(
			n("LogicalORExpressionNoIn"),
			expr(n("LogicalORExpressionNoIn"), i("?"), n("AssignmentExpressionNoIn"), i(":"), n("AssignmentExpressionNoIn"))),
		"AssignmentExpression" -> List(
			n("ConditionalExpression"),
			expr(n("LeftHandSideExpression"), i("="), n("AssignmentExpression")),
			expr(n("LeftHandSideExpression"), n("AssignmentOperator"), n("AssignmentExpression"))),
		"AssignmentExpressionNoIn" -> List(
			n("ConditionalExpressionNoIn"),
			expr(n("LeftHandSideExpression"), i("="), n("AssignmentExpressionNoIn")),
			expr(n("LeftHandSideExpression"), n("AssignmentOperator"), n("AssignmentExpressionNoIn"))),
		"AssignmentOperator" -> List(
			i("*="), i("/="), i("%="), i("+="), i("-="), i("<<="), i(">>="), i(">>>="), i("&="), i("^="), i("|=")),
		"Expression" -> List(
			n("AssignmentExpression"),
			expr(n("Expression"), i(","), n("AssignmentExpression"))),
		"ExpressionNoIn" -> List(
			n("AssignmentExpressionNoIn"),
			expr(n("ExpressionNoIn"), i(","), n("AssignmentExpressionNoIn"))),
		
		// A.4 Statements
		"Statement" -> List(
			n("Block"),
			n("VariableStatement"),
			n("EmptyStatement"),
			n("ExpressionStatement"),
			n("IfStatement"),
			n("IterationStatement"),
			n("ContinueStatement"),
			n("BreakStatement"),
			n("ReturnStatement"),
			n("WithStatement"),
			n("LabelledStatement"),
			n("SwitchStatement"),
			n("ThrowStatement"),
			n("TryStatement"),
			n("DebuggerStatement")),
		"Block" -> List(
			expr(i("{"), n("StatementList").opt, i("}"))),
		"StatementList" -> List(
			n("Statement"),
			expr(n("StatementList"), n("Statement"))),
		"VariableStatement" -> List(
			expr(i("var"), n("VariableDeclarationList"), i(";"))),
		"VariableDeclarationList" -> List(
			n("VariableDeclaration"),
			expr(n("VariableDeclarationList"), i(","), n("VariableDeclaration"))),
		"VariableDeclarationListNoIn" -> List(
			n("VariableDeclarationNoIn"),
			expr(n("VariableDeclarationListNoIn"), i(","), n("VariableDeclarationNoIn"))),
		"VariableDeclaration" -> List(
			expr(n("Identifier"), n("Initialiser").opt)),
		"VariableDeclarationNoIn" -> List(
			expr(n("Identifier"), n("InitialiserNoIn").opt)),
		"Initialiser" -> List(
			expr(i("="), n("AssignmentExpression"))),
		"InitialiserNoIn" -> List(
			expr(i("="), n("AssignmentExpressionNoIn"))),
		"EmptyStatement" -> List(
			i(";")),
		"ExpressionStatement" -> List(
			expr(lookahead_except(i("{"), seq(i("function"), n("WhiteSpace"))), n("Expression"), i(";"))),
		"IfStatement" -> List(
			expr(i("if"), i("("), n("Expression"), i(")"), n("Statement"), i("else"), n("Statement")),
			expr(i("if"), i("("), n("Expression"), i(")"), n("Statement"))),
		"IterationStatement" -> List(
			expr(i("do"), n("Statement"), i("while"), i("("), n("Expression"), i(")"), i(";")),
			expr(i("while"), i("("), n("Expression"), i(")"), n("Statement")),
			expr(i("for"), i("("), n("ExpressionNoIn").opt, i(";"), n("Expression").opt, i(";"), n("Expression").opt, i(")"), n("Statement")),
			expr(i("for"), i("("), i("var"), n("VariableDeclarationListNoIn"), i(";"), n("Expression").opt, i(";"), n("Expression").opt, i(")"), n("Statement")),
			expr(i("for"), i("("), n("LeftHandSideExpression"), i("in"), n("Expression"), i(")"), n("Statement")),
			expr(i("for"), i("("), i("var"), n("VariableDeclarationNoIn"), i("in"), n("Expression"), i(")"), n("Statement"))),
		"ContinueStatement" -> List(
			expr(i("continue"), i(";")),
			expr(line(i("continue"), n("Identifier")), i(";"))),
		"BreakStatement" -> List(
			expr(i("break"), i(";")),
			expr(line(i("break"), n("Identifier")), i(";"))),
		"ReturnStatement" -> List(
			expr(i("return"), i(";")),
			expr(line(i("return"), n("Expression")), i(";"))),
		"WithStatement" -> List(
			expr(i("with"), i("("), n("Expression"), i(")"), n("Statement"))),
		"SwitchStatement" -> List(
			expr(i("switch"), i("("), n("Expression"), i(")"), n("CaseBlock"))),
		"CaseBlock" -> List(
			expr(i("{"), n("CaseClauses").opt, i("}")),
			expr(i("{"), n("CaseClauses").opt, n("DefaultClause"), n("CaseClauses").opt, i("}"))),
		"CaseClauses" -> List(
			n("CaseClause"),
			expr(n("CaseClauses"), n("CaseClause"))),
		"CaseClause" -> List(
			expr(i("case"), n("Expression"), i(":"), n("StatementList").opt)),
		"DefaultClause" -> List(
			expr(i("default"), i(":"), n("StatementList").opt)),
		"LabelledStatement" -> List(
			expr(n("Identifier"), i(":"), n("Statement"))),
		"ThrowStatement" -> List(
			expr(line(i("throw"), n("Expression")), i(";"))),
		"TryStatement" -> List(
			expr(i("try"), n("Block"), n("Catch")),
			expr(i("try"), n("Block"), n("Finally")),
			expr(i("try"), n("Block"), n("Catch"), n("Finally"))),
		"Catch" -> List(
			expr(i("catch"), i("("), n("Identifier"), i(")"), n("Block"))),
		"Finally" -> List(
			expr(i("finally"), n("Block"))),
		"DebuggerStatement" -> List(
			expr(i("debugger"), i(";"))),
		
		// A.5 Functions and Programs
		"FunctionDeclaration" -> List(
			expr(i("function"), n("Identifier"), i("("), n("FormalParameterList").opt, i(")"), i("{"), n("FunctionBody"), i("}"))),
		"FunctionExpression" -> List(
			expr(i("function"), n("Identifier").opt, i("("), n("FormalParameterList").opt, i(")"), i("{"), n("FunctionBody"), i("}"))),
		"FormalParameterList" -> List(
			n("Identifier"),
			expr(n("FormalParameterList"), i(","), n("Identifier"))),
		"FunctionBody" -> List(
			n("SourceElements").opt),
		"Program" -> List(
			n("SourceElements").opt),
		"SourceElements" -> List(
			n("SourceElement"),
			expr(n("SourceElements"), n("SourceElement"))),
		"SourceElement" -> List(
			n("Statement"),
			n("FunctionDeclaration")),
		
		// A.6 Universal Resource Identifier Character Classes
		"uri" -> List(
			n("uriCharacters").opt),
		"uriCharacters" -> List(
			lex(n("uriCharacter"), n("uriCharacters").opt)),
		"uriCharacter" -> List(
			n("uriReserved"),
			n("uriUnescaped"),
			n("uriEscaped")),
		"uriReserved" -> List(
			c(";/?:@&=+$,")),
		"uriUnescaped" -> List(
			n("uriAlpha"),
			n("DecimalDigit"),
			n("uriMark")),
		"uriEscaped" -> List(
			lex(i("%"), n("HexDigit"), n("HexDigit"))),
		"uriAlpha" -> List(
			c("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")),
		"uriMark" -> List(
			c("-_.!~*'()")),
		
		// A.7 Regular Expressions
		"Pattern" -> List(
			n("Disjunction")),
		"Disjunction" -> List(
			n("Alternative"),
			lex(n("Alternative"), i("|"), n("Disjunction"))),
		"Alternative" -> List(
			lex(),
			lex(n("Alternative"), n("Term"))),
		"Term" -> List(
			n("Assertion"),
			n("Atom"),
			lex(n("Atom"), n("Quantifier"))),
		"Assertion" -> List(
			i("^"),
			i("$"),
			lex(i("\\"), i("b")),
			lex(i("\\"), i("B")),
			lex(i("("), i("?"), i("="), n("Disjunction"), i(")")),
			lex(i("("), i("?"), i("!"), n("Disjunction"), i(")"))),
		"Quantifier" -> List(
			n("QuantifierPrefix"),
			lex(n("QuantifierPrefix"), i("?"))),
		"QuantifierPrefix" -> List(
			i("*"),
			i("+"),
			i("?"),
			lex(i("{"), n("DecimalDigits"), i("}")),
			lex(i("{"), n("DecimalDigits"), i(","), i("}")),
			lex(i("{"), n("DecimalDigits"), i("}"))),
		"Atom" -> List(
			n("PatternCharacter"),
			i("."),
			lex(i("\\"), n("AtomEscape")),
			n("CharacterClass"),
			lex(i("("), n("Disjunction"), i(")")),
			lex(i("("), i("?"), i(":"), n("Disjunction"), i(")"))),
		"PatternCharacter" -> List(
			n("SourceCharacter").butnot(c("^$\\.*+?()[]{}|"))),
		"AtomEscape" -> List(
			n("DecimalEscape"),
			n("CharacterEscape"),
			n("CharacterClassEscape")),
		"CharacterEscape" -> List(
			n("ControlEscape"),
			lex(i("c"), n("ControlLetter")),
			n("HexEscapeSequence"),
			n("UnicodeEscapeSequence"),
			n("IdentityEscape")),
		"ControlEscape" -> List(
			c("fnrtv")),
		"ControlLetter" -> List(
			c("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")),
		"IdentityEscape" -> List(
			n("SourceCharacter").butnot(n("IdentifierPart"), c("\u200C\u200D"))),
		"DecimalEscape" -> List(
			lex(n("DecimalIntegerLiteral"), lookahead_except(n("DecimalDigit")))),
		"CharacterClassEscape" -> List(
			c("dDsSwW")),
		"CharacterClass" -> List(
			lex(i("["), lookahead_except(i("^")), n("ClassRanges"), i("]")),
			lex(i("["), i("^"), n("ClassRanges"), i("]"))),
		"ClassRanges" -> List(
			lex(),
			n("NonemptyClassRanges")),
		"NonemptyClassRanges" -> List(
			n("ClassAtom"),
			lex(n("ClassAtom"), n("NonemptyClassRangesNoDash")),
			lex(n("ClassAtom"), i("-"), n("ClassAtom"), n("ClassRanges"))),
		"NonemptyClassRangesNoDash" -> List(
			n("ClassAtom"),
			lex(n("ClassAtomNoDash"), n("NonemptyClassRangesNoDash")),
			lex(n("ClassAtomNoDash"), i("-"), n("ClassAtom"), n("ClassRanges"))),
		"ClassAtom" -> List(
			i("-"),
			n("ClassAtomNoDash")),
		"ClassAtomNoDash" -> List(
			n("SourceCharacter").butnot(c("\\]-")),
			lex(i("\\"), n("ClassEscape"))),
		"ClassEscape" -> List(
			n("DecimalEscape"),
			i("b"),
			n("CharacterEscape"),
			n("CharacterClassEscape")),
		
		// A.8 JSON
		"JSONWhiteSpace" -> List(
			c("\t\n\r ")),
		"JSONString" -> List(
			lex(i("\""), n("JSONStringCharacters").opt, i("\""))),
		"JSONStringCharacters" -> List(
			lex(n("JSONStringCharacter"), n("JSONStringCharacters").opt)),
		"JSONStringCharacter" -> List(
			n("SourceCharacter").butnot(c("\"\\\u0000\u001F")),
			lex(i("\\"), n("JSONEscapeSequence"))),
		"JSONEscapeSequence" -> List(
			n("JSONEscapeCharacter"),
			n("UnicodeEscapeSequence")),
		"JSONEscapeCharacter" -> List(
			c("\"/\\bfnrt")),
		"JSONNumber" -> List(
			lex(i("-").opt, n("DecimalIntegerLiteral"), n("JSONFraction").opt, n("ExponentPart").opt)),
		"JSONFraction" -> List(
			lex(i("."), n("DecimalDigits"))),
		"JSONNullLiteral" -> List(
			n("NullLiteral")),
		"JSONBooleanLiteral" -> List(
			n("BooleanLiteral")),
		"JSONText" -> List(
			n("JSONValue")),
		"JSONValue" -> List(
			n("JSONNullLiteral"),
			n("JSONBooleanLiteral"),
			n("JSONObject"),
			n("JSONArray"),
			n("JSONString"),
			n("JSONNumber")),
		"JSONObject" -> List(
			expr(i("{"), i("}")),
			expr(i("{"), n("JSONMemberList"), i("}"))),
		"JSONMember" -> List(
			expr(n("JSONString"), i(":"), n("JSONValue"))),
		"JSONMemberList" -> List(
			n("JSONMember"),
			expr(n("JSONMemberList"), i(","), n("JSONMember"))),
		"JSONArray" -> List(
			expr(i("["), i("]")),
			expr(i("["), n("JSONElementList"), i("]"))),
		"JSONElementList" -> List(
			n("JSONValue"),
			expr(n("JSONElementList"), i(","), n("JSONValue")))
	)
	override val startSymbol: String = "Start"
}
