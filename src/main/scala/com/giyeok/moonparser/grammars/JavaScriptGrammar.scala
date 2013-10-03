package com.giyeok.moonparser.grammars

import scala.collection.immutable.ListMap
import com.giyeok.moonparser.GrElems._
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.dynamic.Parser
import com.giyeok.moonparser.ParserInputs._
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
    private val whitespace = Set[GrElem](n("WhiteSpace"), n("LineTerminator"), n("Comment"))
    private val oneline = Set[GrElem](n("WhiteSpace"), n("Comment"))

    def expr(s: GrElem*) = Sequence(s toList, whitespace)
    def lex(s: GrElem*) = seq(s: _*)
    def line(s: GrElem*) = seq(oneline, s: _*)

    override val name = "JavaScript"
    override val rules: RuleMap = ListMap(
        "_Token" -> (whitespace ++ Set(n("IdentifierName"), n("Punctuator"), n("NumericLiteral"), n("StringLiteral"))),
        "_Raw" -> Set(n("RegularExpressionLiteral")),

        "Start" -> Set(seq(oneof(whitespace).star, n("Program"), oneof(whitespace).star)),

        // A.1 Lexical Grammar
        "SourceCharacter" -> Set(c),
        "InputElementDiv" -> Set(
            n("WhiteSpace"), n("LineTerminator"), n("Comment"), n("Token"), n("DivPunctuator")),
        "InputElementRegExp" -> Set(
            n("WhiteSpace"), n("LineTerminator"), n("Comment"), n("Token"), n("RegularExpressionLiteral")),
        "WhiteSpace" -> Set(
            c("\u0009\u000B\u000C\uFEFF"), unicode("Zs")), // \u0020\u00A0  ->  already in Zs
        "LineTerminator" -> Set(
            c("\n\r\u2028\u2029")),
        "LineTerminatorSequence" -> Set(
            c("\n\u2028\u2029"), lex(c("\r"), lookahead_except(c("\n"))), i("\r\n")),
        "Comment" -> Set(
            n("MultiLineComment"), n("SingleLineComment")),
        "MultiLineComment" -> Set(
            expr(i("/*"), n("MultiLineCommentChars").opt, i("*/"))),
        "MultiLineCommentChars" -> Set(
            lex(n("MultiLineNotAsteriskChar"), n("MultiLineCommentChars").opt),
            lex(i("*"), n("PostAsteriskCommentChars").opt)),
        "PostAsteriskCommentChars" -> Set(
            lex(n("MultiLineNotForwardSlashOrAsteriskChar"), n("MultiLineCommentChars").opt),
            lex(i("*"), n("PostAsteriskCommentChars").opt)),
        "MultiLineNotAsteriskChar" -> Set(
            lex(n("SourceCharacter").butnot(i("*")))),
        "MultiLineNotForwardSlashOrAsteriskChar" -> Set(
            lex(n("SourceCharacter").butnot(i("/"), i("*")))),
        "SingleLineComment" -> Set(
            lex(i("//"), n("SingleLineCommentChars").opt)),
        "SingleLineCommentChars" -> Set(
            lex(n("SingleLineCommentChar"), n("SingleLineCommentChars").opt)),
        "SingleLineCommentChar" -> Set(
            n("SourceCharacter").butnot(n("LineTerminator"))),
        "Token" -> Set(
            n("IdentifierName"),
            n("Punctuator"),
            n("NumericLiteral"),
            n("StringLiteral")),
        "Identifier" -> Set(
            n("IdentifierName").butnot(n("ReservedWord"))),
        "IdentifierName" -> Set(
            n("IdentifierStart"),
            lex(n("IdentifierName"), n("IdentifierPart"))),
        "IdentifierStart" -> Set(
            n("UnicodeLetter"),
            i("$"),
            i("_"),
            lex(i("_"), n("UnicodeEscapeSequence"))),
        "IdentifierPart" -> Set(
            n("IdentifierStart"),
            n("UnicodeCombiningMark"),
            n("UnicodeDigit"),
            n("UnicodeConnectorPunctuation"),
            c("\u200C\u200D")),
        "UnicodeLetter" -> Set(
            unicode("Lu", "Ll", "Lt", "Lm", "Lo", "Nl")),
        "UnicodeCombiningMark" -> Set(
            unicode("Mn", "Mc")),
        "UnicodeDigit" -> Set(
            unicode("Nd")),
        "UnicodeConnectorPunctuation" -> Set(
            unicode("Pc")),
        "ReservedWord" -> Set(
            n("Keyword"),
            n("FutureReservedWord"),
            n("NullLiteral"),
            n("BooleanLiteral")),
        "Keyword" -> Set(
            i("break"), i("do"), i("instanceof"), i("typeof"),
            i("case"), i("else"), i("new"), i("var"),
            i("catch"), i("finally"), i("return"), i("void"),
            i("continue"), i("for"), i("switch"), i("while"),
            i("debugger"), i("function"), i("this"), i("with"),
            i("default"), i("if"), i("throw"),
            i("delete"), i("in"), i("try")),
        "FutureReservedWord" -> Set(
            i("class"), i("enum"), i("extends"), i("super"),
            i("const"), i("export"), i("import"),
            i("implements"), i("let"), i("private"), i("public"),
            i("interface"), i("package"), i("protected"), i("static"),
            i("yield")),
        "Punctuator" -> Set(
            i("{"), i("}"), i("("), i(")"), i("["), i("]"),
            i("."), i(";"), i(","), i("<"), i(">"), i("<="),
            i(">="), i("=="), i("!="), i("==="), i("!=="),
            i("+"), i("-"), i("*"), i("%"), i("++"), i("--"),
            i("<<"), i(">>"), i(">>>"), i("&"), i("|"), i("^"),
            i("!"), i("~"), i("&&"), i("||"), i("?"), i(":"),
            i("="), i("+="), i("-="), i("*="), i("%="), i("<<="),
            i(">>="), i(">>>="), i("&="), i("|="), i("^=")),
        "DivPunctuator" -> Set(
            i("/"), i("/=")),
        "Literal" -> Set(
            n("NullLiteral"),
            n("BooleanLiteral"),
            n("NumericLiteral"),
            n("StringLiteral"),
            n("RegularExpressionLiteral")),
        "NullLiteral" -> Set(
            i("null")),
        "BooleanLiteral" -> Set(
            i("true"),
            i("false")),
        "NumericLiteral" -> Set(
            n("DecimalLiteral"),
            n("HexIntegerLiteral")),
        "DecimalLiteral" -> Set(
            lex(n("DecimalIntegerLiteral"), i("."), n("DecimalDigits").opt, n("ExponentPart").opt),
            lex(i("."), n("DecimalDigits"), n("ExponentPart").opt),
            lex(n("DecimalIntegerLiteral"), n("ExponentPart").opt)),
        "DecimalIntegerLiteral" -> Set(
            i("0"),
            lex(n("NonZeroDigit"), n("DecimalDigits").opt)),
        "DecimalDigits" -> Set(
            n("DecimalDigit"),
            lex(n("DecimalDigits"), n("DecimalDigit"))),
        "DecimalDigit" -> Set(
            c("0123456789")),
        "NonZeroDigit" -> Set(
            c("123456789")),
        "ExponentPart" -> Set(
            lex(n("ExponentIndicator"), n("SignedInteger"))),
        "ExponentIndicator" -> Set(
            c("eE")),
        "SignedInteger" -> Set(
            n("DecimalDigits"),
            lex(i("+"), n("DecimalDigits")),
            lex(i("-"), n("DecimalDigits"))),
        "HexIntegerLiteral" -> Set(
            lex(i("0x"), n("HexDigit")),
            lex(i("0X"), n("HexDigit")),
            lex(n("HexIntegerLiteral"), n("HexDigit"))),
        "HexDigit" -> Set(
            c("0123456789abcdefABCDEF")),
        "StringLiteral" -> Set(
            lex(i("\""), n("DoubleStringCharacters").opt, i("\"")),
            lex(i("'"), n("SingleStringCharacters").opt, i("'"))),
        "DoubleStringCharacters" -> Set(
            lex(n("DoubleStringCharacter"), n("DoubleStringCharacters").opt)),
        "SingleStringCharacters" -> Set(
            lex(n("SingleStringCharacter"), n("SingleStringCharacters").opt)),
        "DoubleStringCharacter" -> Set(
            n("SourceCharacter").butnot(c("\"\\"), n("LineTerminator")),
            lex(i("\\"), n("EscapeSequence")),
            n("LineContinuation")),
        "SingleStringCharacter" -> Set(
            n("SourceCharacter").butnot(c("'\\"), n("LineTerminator")),
            lex(i("\\"), n("EscapeSequence")),
            n("LineContinuation")),
        "LineContinuation" -> Set(
            lex(i("\\"), n("LineTerminatorSequence"))),
        "EscapeSequence" -> Set(
            n("CharacterEscapeSequence"),
            lex(i("0"), lookahead_except(n("DecimalDigit"))),
            n("HexEscapeSequence"),
            n("UnicodeEscapeSequence")),
        "CharacterEscapeSequence" -> Set(
            n("SingleEscapeCharacter"),
            n("NonEscapeCharacter")),
        "SingleEscapeCharacter" -> Set(
            c("'\"\\bfnrtv")),
        "NonEscapeCharacter" -> Set(
            n("SourceCharacter").butnot(n("EscapeCharacter"), n("LineTerminator"))),
        "EscapeCharacter" -> Set(
            n("SingleEscapeCharacter"),
            n("DecimalDigit"),
            c("xu")),
        "HexEscapeSequence" -> Set(
            lex(i("x"), n("HexDigit"), n("HexDigit"))),
        "UnicodeEscapeSequence" -> Set(
            lex(i("u"), n("HexDigit"), n("HexDigit"), n("HexDigit"), n("HexDigit"))),
        "RegularExpressionLiteral" -> Set(
            lex(i("/"), n("RegularExpressionBody"), i("/"), n("RegularExpressionFlags"))),
        "RegularExpressionBody" -> Set(
            lex(n("RegularExpressionFirstChar"), n("RegularExpressionChars"))),
        "RegularExpressionChars" -> Set(
            lex(),
            lex(n("RegularExpressionChars"), n("RegularExpressionChar"))),
        "RegularExpressionFirstChar" -> Set(
            n("RegularExpressionNonTerminator").butnot(c("*\\/[")),
            n("RegularExpressionBackslashSequence"),
            n("RegularExpressionClass")),
        "RegularExpressionChar" -> Set(
            n("RegularExpressionNonTerminator").butnot(c("\\/[")),
            n("RegularExpressionBackslashSequence"),
            n("RegularExpressionClass")),
        "RegularExpressionBackslashSequence" -> Set(
            lex(i("\\"), n("RegularExpressionNonTerminator"))),
        "RegularExpressionNonTerminator" -> Set(
            n("SourceCharacter").butnot(n("LineTerminator"))),
        "RegularExpressionClass" -> Set(
            lex(i("["), n("RegularExpressionClassChars"), i("]"))),
        "RegularExpressionClassChars" -> Set(
            lex(),
            lex(n("RegularExpressionClassChars"), n("RegularExpressionClassChar"))),
        "RegularExpressionClassChar" -> Set(
            n("RegularExpressionNonTerminator").butnot(c("]\\")),
            n("RegularExpressionBackslashSequence")),
        "RegularExpressionFlags" -> Set(
            lex(),
            lex(n("RegularExpressionFlags"), n("IdentifierPart"))),

        // A.2 Number Conversions
        "StringNumericLiteral" -> Set(
            n("StrWhiteSpace").opt,
            lex(n("StrWhiteSpace").opt, n("StrNumericLiteral"), n("StrWhiteSpace").opt)),
        "StrWhiteSpace" -> Set(
            lex(n("StrWhiteSpaceChar"), n("StrWhiteSpace").opt)),
        "StrWhiteSpaceChar" -> Set(
            n("WhiteSpace"),
            n("LineTerminator")),
        "StrNumericLiteral" -> Set(
            n("StrDecimalLiteral"),
            n("HexIntegerLiteral")),
        "StrDecimalLiteral" -> Set(
            n("StrUnsignedDecimalLiteral"),
            lex(i("+"), n("StrUnsignedDecimalLiteral")),
            lex(i("-"), n("StrUnsignedDecimalLiteral"))),
        "StrUnsignedDecimalLiteral" -> Set(
            i("Infinity"),
            lex(n("DecimalDigits"), i("."), n("DecimalDigits").opt, n("ExponentPart").opt),
            lex(i("."), n("DecimalDigits"), n("ExponentPart").opt),
            lex(n("DecimalDigits"), n("ExponentPart").opt)),
        "DecimalDigits" -> Set(
            n("DecimalDigit"),
            lex(n("DecimalDigits"), n("DecimalDigit"))),
        "DecimalDigit" -> Set(
            c("0123456789")),
        "ExponentPart" -> Set(
            lex(n("ExponentIndicator"), n("SignedInteger"))),
        "ExponentIndicator" -> Set(
            c("eE")),
        "SignedInteger" -> Set(
            n("DecimalDigits"),
            lex(i("+"), n("DecimalDigits")),
            lex(i("-"), n("DecimalDigits"))),
        "HexIntegerLiteral" -> Set(
            lex(i("0x"), n("HexDigit")),
            lex(i("0X"), n("HexDigit")),
            lex(n("HexIntegerLiteral"), n("HexDigit"))),
        "HexDigit" -> Set(
            c("0123456789abcdefABCDEF")),

        // A.3 Expressions
        "PrimaryExpression" -> Set(
            i("this"),
            n("Identifier"),
            n("Literal"),
            n("ArrayLiteral"),
            n("ObjectLiteral"),
            expr(i("("), n("Expression"), i(")"))),
        "ArrayLiteral" -> Set(
            expr(i("["), n("Elision").opt, i("]")),
            expr(i("["), n("ElementList"), i("]")),
            expr(i("["), n("ElementList"), i(","), n("Elision").opt, i("]"))),
        "ElementList" -> Set(
            expr(n("Elision").opt, n("AssignmentExpression")),
            expr(n("ElementList"), i(","), n("Elision").opt, n("AssignmentExpression"))),
        "Elision" -> Set(
            i(","),
            expr(n("Elision"), i(","))),
        "ObjectLiteral" -> Set(
            expr(i("{"), i("}")),
            expr(i("{"), n("PropertyNameAndValueList"), i("}")),
            expr(i("{"), n("PropertyNameAndValueList"), i(","), i("}"))),
        "PropertyNameAndValueList" -> Set(
            n("PropertyAssignment"),
            expr(n("PropertyNameAndValueList"), i(","), n("PropertyAssignment"))),
        "PropertyAssignment" -> Set(
            expr(n("PropertyName"), i(":"), n("AssignmentExpression")),
            expr(i("get"), n("PropertyName"), i("("), i(")"), i("{"), n("FunctionBody"), i("}")),
            expr(i("set"), n("PropertyName"), i("("), n("PropertySetParameterList"), i(")"), i("{"), n("FunctionBody"), i("}"))),
        "PropertyName" -> Set(
            n("IdentifierName"),
            n("StringLiteral"),
            n("NumericLiteral")),
        "PropertySetParameterList" -> Set(
            n("Identifier")),
        "MemberExpression" -> Set(
            n("PrimaryExpression"),
            n("FunctionExpression"),
            expr(n("MemberExpression"), i("["), n("Expression"), i("]")),
            expr(n("MemberExpression"), i("."), n("IdentifierName")),
            expr(i("new"), n("MemberExpression"), n("Arguments"))),
        "NewExpression" -> Set(
            n("MemberExpression"),
            expr(i("new"), n("NewExpression"))),
        "CallExpression" -> Set(
            expr(n("MemberExpression"), n("Arguments")),
            expr(n("CallExpression"), n("Arguments")),
            expr(n("CallExpression"), i("["), n("Expression"), i("]")),
            expr(n("CallExpression"), i("."), n("IdentifierName"))),
        "Arguments" -> Set(
            expr(i("("), i(")")),
            expr(i("("), n("ArgumentList"), i(")"))),
        "ArgumentList" -> Set(
            n("AssignmentExpression"),
            expr(n("ArgumentList"), i(","), n("AssignmentExpression"))),
        "LeftHandSideExpression" -> Set(
            n("NewExpression"),
            n("CallExpression")),
        "PostfixExpression" -> Set(
            n("LeftHandSideExpression"),
            line(n("LeftHandSideExpression"), i("++")),
            line(n("LeftHandSideExpression"), i("--"))),
        "UnaryExpression" -> Set(
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
        "MultiplicativeExpression" -> Set(
            n("UnaryExpression"),
            expr(n("MultiplicativeExpression"), i("*"), n("UnaryExpression")),
            expr(n("MultiplicativeExpression"), i("/"), n("UnaryExpression")),
            expr(n("MultiplicativeExpression"), i("%"), n("UnaryExpression"))),
        "AdditiveExpression" -> Set(
            n("MultiplicativeExpression"),
            expr(n("AdditiveExpression"), i("+"), n("MultiplicativeExpression")),
            expr(n("AdditiveExpression"), i("-"), n("MultiplicativeExpression"))),
        "ShiftExpression" -> Set(
            n("AdditiveExpression"),
            expr(n("ShiftExpression"), i("<<"), n("AdditiveExpression")),
            expr(n("ShiftExpression"), i(">>"), n("AdditiveExpression")),
            expr(n("ShiftExpression"), i(">>>"), n("AdditiveExpression"))),
        "RelationalExpression" -> Set(
            n("ShiftExpression"),
            expr(n("RelationalExpression"), i("<"), n("ShiftExpression")),
            expr(n("RelationalExpression"), i(">"), n("ShiftExpression")),
            expr(n("RelationalExpression"), i("<="), n("ShiftExpression")),
            expr(n("RelationalExpression"), i(">="), n("ShiftExpression")),
            expr(n("RelationalExpression"), i("instanceof"), n("ShiftExpression"))),
        "RelationalExpressionNoIn" -> Set(
            n("ShiftExpression"),
            expr(n("RelationalExpressionNoIn"), i("<"), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), i(">"), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), i("<="), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), i(">="), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), i("instanceof"), n("ShiftExpression"))),
        "EqualityExpression" -> Set(
            n("RelationalExpression"),
            expr(n("EqualityExpression"), i("=="), n("RelationalExpression")),
            expr(n("EqualityExpression"), i("!=="), n("RelationalExpression")),
            expr(n("EqualityExpression"), i("==="), n("RelationalExpression")),
            expr(n("EqualityExpression"), i("!=="), n("RelationalExpression"))),
        "EqualityExpressionNoIn" -> Set(
            n("RelationalExpressionNoIn"),
            expr(n("EqualityExpressionNoIn"), i("=="), n("RelationalExpressionNoIn")),
            expr(n("EqualityExpressionNoIn"), i("!=="), n("RelationalExpressionNoIn")),
            expr(n("EqualityExpressionNoIn"), i("==="), n("RelationalExpressionNoIn")),
            expr(n("EqualityExpressionNoIn"), i("!=="), n("RelationalExpressionNoIn"))),
        "BitwiseANDExpression" -> Set(
            n("EqualityExpression"),
            expr(n("BitwiseANDExpression"), i("&"), n("EqualityExpression"))),
        "BitwiseANDExpressionNoIn" -> Set(
            n("EqualityExpressionNoIn"),
            expr(n("BitwiseANDExpressionNoIn"), i("&"), n("EqualityExpressionNoIn"))),
        "BitwiseXORExpression" -> Set(
            n("BitwiseANDExpression"),
            expr(n("BitwiseXORExpression"), i("^"), n("BitwiseANDExpression"))),
        "BitwiseXORExpressionNoIn" -> Set(
            n("BitwiseANDExpressionNoIn"),
            expr(n("BitwiseXORExpressionNoIn"), i("^"), n("BitwiseANDExpressionNoIn"))),
        "BitwiseORExpression" -> Set(
            n("BitwiseXORExpression"),
            expr(n("BitwiseORExpression"), i("^"), n("BitwiseXORExpression"))),
        "BitwiseORExpressionNoIn" -> Set(
            n("BitwiseXORExpressionNoIn"),
            expr(n("BitwiseORExpressionNoIn"), i("^"), n("BitwiseXORExpressionNoIn"))),
        "LogicalANDExpression" -> Set(
            n("BitwiseORExpression"),
            expr(n("LogicalANDExpression"), i("&&"), n("BitwiseORExpression"))),
        "LogicalANDExpressionNoIn" -> Set(
            n("BitwiseORExpressionNoIn"),
            expr(n("LogicalANDExpressionNoIn"), i("&&"), n("BitwiseORExpressionNoIn"))),
        "LogicalORExpression" -> Set(
            n("LogicalANDExpression"),
            expr(n("LogicalORExpression"), i("||"), n("LogicalANDExpression"))),
        "LogicalORExpressionNoIn" -> Set(
            n("LogicalANDExpressionNoIn"),
            expr(n("LogicalORExpressionNoIn"), i("||"), n("LogicalANDExpressionNoIn"))),
        "ConditionalExpression" -> Set(
            n("LogicalORExpression"),
            expr(n("LogicalORExpression"), i("?"), n("AssignmentExpression"), i(":"), n("AssignmentExpression"))),
        "ConditionalExpressionNoIn" -> Set(
            n("LogicalORExpressionNoIn"),
            expr(n("LogicalORExpressionNoIn"), i("?"), n("AssignmentExpressionNoIn"), i(":"), n("AssignmentExpressionNoIn"))),
        "AssignmentExpression" -> Set(
            n("ConditionalExpression"),
            expr(n("LeftHandSideExpression"), i("="), n("AssignmentExpression")),
            expr(n("LeftHandSideExpression"), n("AssignmentOperator"), n("AssignmentExpression"))),
        "AssignmentExpressionNoIn" -> Set(
            n("ConditionalExpressionNoIn"),
            expr(n("LeftHandSideExpression"), i("="), n("AssignmentExpressionNoIn")),
            expr(n("LeftHandSideExpression"), n("AssignmentOperator"), n("AssignmentExpressionNoIn"))),
        "AssignmentOperator" -> Set(
            i("*="), i("/="), i("%="), i("+="), i("-="), i("<<="), i(">>="), i(">>>="), i("&="), i("^="), i("|=")),
        "Expression" -> Set(
            n("AssignmentExpression"),
            expr(n("Expression"), i(","), n("AssignmentExpression"))),
        "ExpressionNoIn" -> Set(
            n("AssignmentExpressionNoIn"),
            expr(n("ExpressionNoIn"), i(","), n("AssignmentExpressionNoIn"))),

        // A.4 Statements
        "Statement" -> Set(
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
        "Block" -> Set(
            expr(i("{"), n("StatementList").opt, i("}"))),
        "StatementList" -> Set(
            n("Statement"),
            expr(n("StatementList"), n("Statement"))),
        "VariableStatement" -> Set(
            expr(i("var"), n("VariableDeclarationList"), i(";"))),
        "VariableDeclarationList" -> Set(
            n("VariableDeclaration"),
            expr(n("VariableDeclarationList"), i(","), n("VariableDeclaration"))),
        "VariableDeclarationListNoIn" -> Set(
            n("VariableDeclarationNoIn"),
            expr(n("VariableDeclarationListNoIn"), i(","), n("VariableDeclarationNoIn"))),
        "VariableDeclaration" -> Set(
            expr(n("Identifier"), n("Initialiser").opt)),
        "VariableDeclarationNoIn" -> Set(
            expr(n("Identifier"), n("InitialiserNoIn").opt)),
        "Initialiser" -> Set(
            expr(i("="), n("AssignmentExpression"))),
        "InitialiserNoIn" -> Set(
            expr(i("="), n("AssignmentExpressionNoIn"))),
        "EmptyStatement" -> Set(
            i(";")),
        "ExpressionStatement" -> Set(
            expr(lookahead_except(i("{"), seq(i("function"), n("WhiteSpace"))), n("Expression"), i(";"))),
        "IfStatement" -> Set(
            expr(i("if"), i("("), n("Expression"), i(")"), n("Statement"), i("else"), n("Statement")),
            expr(i("if"), i("("), n("Expression"), i(")"), n("Statement"))),
        "IterationStatement" -> Set(
            expr(i("do"), n("Statement"), i("while"), i("("), n("Expression"), i(")"), i(";")),
            expr(i("while"), i("("), n("Expression"), i(")"), n("Statement")),
            expr(i("for"), i("("), n("ExpressionNoIn").opt, i(";"), n("Expression").opt, i(";"), n("Expression").opt, i(")"), n("Statement")),
            expr(i("for"), i("("), i("var"), n("VariableDeclarationListNoIn"), i(";"), n("Expression").opt, i(";"), n("Expression").opt, i(")"), n("Statement")),
            expr(i("for"), i("("), n("LeftHandSideExpression"), i("in"), n("Expression"), i(")"), n("Statement")),
            expr(i("for"), i("("), i("var"), n("VariableDeclarationNoIn"), i("in"), n("Expression"), i(")"), n("Statement"))),
        "ContinueStatement" -> Set(
            expr(i("continue"), i(";")),
            expr(line(i("continue"), n("Identifier")), i(";"))),
        "BreakStatement" -> Set(
            expr(i("break"), i(";")),
            expr(line(i("break"), n("Identifier")), i(";"))),
        "ReturnStatement" -> Set(
            expr(i("return"), i(";")),
            expr(line(i("return"), n("Expression")), i(";"))),
        "WithStatement" -> Set(
            expr(i("with"), i("("), n("Expression"), i(")"), n("Statement"))),
        "SwitchStatement" -> Set(
            expr(i("switch"), i("("), n("Expression"), i(")"), n("CaseBlock"))),
        "CaseBlock" -> Set(
            expr(i("{"), n("CaseClauses").opt, i("}")),
            expr(i("{"), n("CaseClauses").opt, n("DefaultClause"), n("CaseClauses").opt, i("}"))),
        "CaseClauses" -> Set(
            n("CaseClause"),
            expr(n("CaseClauses"), n("CaseClause"))),
        "CaseClause" -> Set(
            expr(i("case"), n("Expression"), i(":"), n("StatementList").opt)),
        "DefaultClause" -> Set(
            expr(i("default"), i(":"), n("StatementList").opt)),
        "LabelledStatement" -> Set(
            expr(n("Identifier"), i(":"), n("Statement"))),
        "ThrowStatement" -> Set(
            expr(line(i("throw"), n("Expression")), i(";"))),
        "TryStatement" -> Set(
            expr(i("try"), n("Block"), n("Catch")),
            expr(i("try"), n("Block"), n("Finally")),
            expr(i("try"), n("Block"), n("Catch"), n("Finally"))),
        "Catch" -> Set(
            expr(i("catch"), i("("), n("Identifier"), i(")"), n("Block"))),
        "Finally" -> Set(
            expr(i("finally"), n("Block"))),
        "DebuggerStatement" -> Set(
            expr(i("debugger"), i(";"))),

        // A.5 Functions and Programs
        "FunctionDeclaration" -> Set(
            expr(i("function"), n("Identifier"), i("("), n("FormalParameterList").opt, i(")"), i("{"), n("FunctionBody"), i("}"))),
        "FunctionExpression" -> Set(
            expr(i("function"), n("Identifier").opt, i("("), n("FormalParameterList").opt, i(")"), i("{"), n("FunctionBody"), i("}"))),
        "FormalParameterList" -> Set(
            n("Identifier"),
            expr(n("FormalParameterList"), i(","), n("Identifier"))),
        "FunctionBody" -> Set(
            n("SourceElements").opt),
        "Program" -> Set(
            n("SourceElements").opt),
        "SourceElements" -> Set(
            n("SourceElement"),
            expr(n("SourceElements"), n("SourceElement"))),
        "SourceElement" -> Set(
            n("Statement"),
            n("FunctionDeclaration")),

        // A.6 Universal Resource Identifier Character Classes
        "uri" -> Set(
            n("uriCharacters").opt),
        "uriCharacters" -> Set(
            lex(n("uriCharacter"), n("uriCharacters").opt)),
        "uriCharacter" -> Set(
            n("uriReserved"),
            n("uriUnescaped"),
            n("uriEscaped")),
        "uriReserved" -> Set(
            c(";/?:@&=+$,")),
        "uriUnescaped" -> Set(
            n("uriAlpha"),
            n("DecimalDigit"),
            n("uriMark")),
        "uriEscaped" -> Set(
            lex(i("%"), n("HexDigit"), n("HexDigit"))),
        "uriAlpha" -> Set(
            c("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")),
        "uriMark" -> Set(
            c("-_.!~*'()")),

        // A.7 Regular Expressions
        "Pattern" -> Set(
            n("Disjunction")),
        "Disjunction" -> Set(
            n("Alternative"),
            lex(n("Alternative"), i("|"), n("Disjunction"))),
        "Alternative" -> Set(
            lex(),
            lex(n("Alternative"), n("Term"))),
        "Term" -> Set(
            n("Assertion"),
            n("Atom"),
            lex(n("Atom"), n("Quantifier"))),
        "Assertion" -> Set(
            i("^"),
            i("$"),
            lex(i("\\"), i("b")),
            lex(i("\\"), i("B")),
            lex(i("("), i("?"), i("="), n("Disjunction"), i(")")),
            lex(i("("), i("?"), i("!"), n("Disjunction"), i(")"))),
        "Quantifier" -> Set(
            n("QuantifierPrefix"),
            lex(n("QuantifierPrefix"), i("?"))),
        "QuantifierPrefix" -> Set(
            i("*"),
            i("+"),
            i("?"),
            lex(i("{"), n("DecimalDigits"), i("}")),
            lex(i("{"), n("DecimalDigits"), i(","), i("}")),
            lex(i("{"), n("DecimalDigits"), i("}"))),
        "Atom" -> Set(
            n("PatternCharacter"),
            i("."),
            lex(i("\\"), n("AtomEscape")),
            n("CharacterClass"),
            lex(i("("), n("Disjunction"), i(")")),
            lex(i("("), i("?"), i(":"), n("Disjunction"), i(")"))),
        "PatternCharacter" -> Set(
            n("SourceCharacter").butnot(c("^$\\.*+?()[]{}|"))),
        "AtomEscape" -> Set(
            n("DecimalEscape"),
            n("CharacterEscape"),
            n("CharacterClassEscape")),
        "CharacterEscape" -> Set(
            n("ControlEscape"),
            lex(i("c"), n("ControlLetter")),
            n("HexEscapeSequence"),
            n("UnicodeEscapeSequence"),
            n("IdentityEscape")),
        "ControlEscape" -> Set(
            c("fnrtv")),
        "ControlLetter" -> Set(
            c("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")),
        "IdentityEscape" -> Set(
            n("SourceCharacter").butnot(n("IdentifierPart"), c("\u200C\u200D"))),
        "DecimalEscape" -> Set(
            lex(n("DecimalIntegerLiteral"), lookahead_except(n("DecimalDigit")))),
        "CharacterClassEscape" -> Set(
            c("dDsSwW")),
        "CharacterClass" -> Set(
            lex(i("["), lookahead_except(i("^")), n("ClassRanges"), i("]")),
            lex(i("["), i("^"), n("ClassRanges"), i("]"))),
        "ClassRanges" -> Set(
            lex(),
            n("NonemptyClassRanges")),
        "NonemptyClassRanges" -> Set(
            n("ClassAtom"),
            lex(n("ClassAtom"), n("NonemptyClassRangesNoDash")),
            lex(n("ClassAtom"), i("-"), n("ClassAtom"), n("ClassRanges"))),
        "NonemptyClassRangesNoDash" -> Set(
            n("ClassAtom"),
            lex(n("ClassAtomNoDash"), n("NonemptyClassRangesNoDash")),
            lex(n("ClassAtomNoDash"), i("-"), n("ClassAtom"), n("ClassRanges"))),
        "ClassAtom" -> Set(
            i("-"),
            n("ClassAtomNoDash")),
        "ClassAtomNoDash" -> Set(
            n("SourceCharacter").butnot(c("\\]-")),
            lex(i("\\"), n("ClassEscape"))),
        "ClassEscape" -> Set(
            n("DecimalEscape"),
            i("b"),
            n("CharacterEscape"),
            n("CharacterClassEscape")),

        // A.8 JSON
        "JSONWhiteSpace" -> Set(
            c("\t\n\r ")),
        "JSONString" -> Set(
            lex(i("\""), n("JSONStringCharacters").opt, i("\""))),
        "JSONStringCharacters" -> Set(
            lex(n("JSONStringCharacter"), n("JSONStringCharacters").opt)),
        "JSONStringCharacter" -> Set(
            n("SourceCharacter").butnot(c("\"\\\u0000\u001F")),
            lex(i("\\"), n("JSONEscapeSequence"))),
        "JSONEscapeSequence" -> Set(
            n("JSONEscapeCharacter"),
            n("UnicodeEscapeSequence")),
        "JSONEscapeCharacter" -> Set(
            c("\"/\\bfnrt")),
        "JSONNumber" -> Set(
            lex(i("-").opt, n("DecimalIntegerLiteral"), n("JSONFraction").opt, n("ExponentPart").opt)),
        "JSONFraction" -> Set(
            lex(i("."), n("DecimalDigits"))),
        "JSONNullLiteral" -> Set(
            n("NullLiteral")),
        "JSONBooleanLiteral" -> Set(
            n("BooleanLiteral")),
        "JSONText" -> Set(
            n("JSONValue")),
        "JSONValue" -> Set(
            n("JSONNullLiteral"),
            n("JSONBooleanLiteral"),
            n("JSONObject"),
            n("JSONArray"),
            n("JSONString"),
            n("JSONNumber")),
        "JSONObject" -> Set(
            expr(i("{"), i("}")),
            expr(i("{"), n("JSONMemberList"), i("}"))),
        "JSONMember" -> Set(
            expr(n("JSONString"), i(":"), n("JSONValue"))),
        "JSONMemberList" -> Set(
            n("JSONMember"),
            expr(n("JSONMemberList"), i(","), n("JSONMember"))),
        "JSONArray" -> Set(
            expr(i("["), i("]")),
            expr(i("["), n("JSONElementList"), i("]"))),
        "JSONElementList" -> Set(
            n("JSONValue"),
            expr(n("JSONElementList"), i(","), n("JSONValue"))))
    override val startSymbol: String = "Start"
}
