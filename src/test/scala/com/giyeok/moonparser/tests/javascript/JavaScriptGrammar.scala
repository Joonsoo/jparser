package com.giyeok.moonparser.tests.javascript

import scala.collection.immutable.ListMap
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Symbols.Symbol
import com.giyeok.moonparser.GrammarHelper._
import com.giyeok.moonparser.Symbols._
import scala.collection.immutable.ListSet

object JavaScriptGrammar extends Grammar {

    private val whitespace = ListSet[Symbol](n("WhiteSpace"), n("LineTerminator"), n("Comment"))
    private val oneline = Set[Symbol](n("WhiteSpace"), n("Comment"))

    def expr(s: Symbol*) = seq(s toList, whitespace)
    def lex(s: Symbol*) = seq(s: _*)
    def line(s: Symbol*) = seq(oneline, s: _*)
    def longest(s: Symbol) = seq(s.star, lookahead_except(s))
    val lineend = seq(longest(oneof(whitespace)), i(";")).backup(seq(longest(oneof(whitespace)), n("LineTerminator")))
    def stmt(s: Symbol*) = seq(expr(s: _*), lineend)

    val name = "JavaScript"
    val startSymbol = n("Start")
    val rules: RuleMap = ListMap(
        "_Token" -> (whitespace ++ ListSet(n("IdentifierName"), n("Punctuator"), n("NumericLiteral"), n("StringLiteral"))),
        "_Raw" -> ListSet(n("RegularExpressionLiteral")),

        "Start" -> ListSet(n("Program")),

        // A.1 Lexical Grammar
        "SourceCharacter" -> ListSet(c),
        "InputElementDiv" -> ListSet(
            n("WhiteSpace"), n("LineTerminator"), n("Comment"), n("Token"), n("DivPunctuator")),
        "InputElementRegExp" -> ListSet(
            n("WhiteSpace"), n("LineTerminator"), n("Comment"), n("Token"), n("RegularExpressionLiteral")),
        "WhiteSpace" -> ListSet(
            chars("\u0009\u000B\u000C\uFEFF"), unicode("Zs")), // \u0020\u00A0  ->  already in Zs
        "LineTerminator" -> ListSet(
            chars("\n\r\u2028\u2029")),
        "LineTerminatorSequence" -> ListSet(
            chars("\n\u2028\u2029"), lex(c('\r'), lookahead_except(c('\n'))), i("\r\n")),
        "Comment" -> ListSet(
            n("MultiLineComment"), n("SingleLineComment")),
        "MultiLineComment" -> ListSet(
            expr(i("/*"), n("MultiLineCommentChars").opt, i("*/"))),
        "MultiLineCommentChars" -> ListSet(
            lex(n("MultiLineNotAsteriskChar"), n("MultiLineCommentChars").opt),
            lex(i("*"), n("PostAsteriskCommentChars").opt)),
        "PostAsteriskCommentChars" -> ListSet(
            lex(n("MultiLineNotForwardSlashOrAsteriskChar"), n("MultiLineCommentChars").opt),
            lex(i("*"), n("PostAsteriskCommentChars").opt)),
        "MultiLineNotAsteriskChar" -> ListSet(
            lex(n("SourceCharacter").butnot(i("*")))),
        "MultiLineNotForwardSlashOrAsteriskChar" -> ListSet(
            lex(n("SourceCharacter").butnot(i("/"), i("*")))),
        "SingleLineComment" -> ListSet(
            lex(i("//"), n("SingleLineCommentChars").opt)),
        "SingleLineCommentChars" -> ListSet(
            lex(n("SingleLineCommentChar"), n("SingleLineCommentChars").opt)),
        "SingleLineCommentChar" -> ListSet(
            n("SourceCharacter").butnot(n("LineTerminator"))),
        "Token" -> ListSet(
            n("IdentifierName"),
            n("Punctuator"),
            n("NumericLiteral"),
            n("StringLiteral")),
        "Identifier" -> ListSet(
            elongest(n("IdentifierName")).butnot(n("ReservedWord"))),
        "IdentifierName" -> ListSet(
            lex(n("IdentifierStart")),
            lex(n("IdentifierName"), n("IdentifierPart"))),
        "IdentifierStart" -> ListSet(
            n("UnicodeLetter"),
            i("$"),
            i("_"),
            lex(i("\""), n("UnicodeEscapeSequence"))),
        "IdentifierPart" -> ListSet(
            n("IdentifierStart"),
            n("UnicodeCombiningMark"),
            n("UnicodeDigit"),
            n("UnicodeConnectorPunctuation"),
            chars("\u200C\u200D")),
        "UnicodeLetter" -> ListSet(
            unicode("Lu", "Ll", "Lt", "Lm", "Lo", "Nl")),
        "UnicodeCombiningMark" -> ListSet(
            unicode("Mn", "Mc")),
        "UnicodeDigit" -> ListSet(
            unicode("Nd")),
        "UnicodeConnectorPunctuation" -> ListSet(
            unicode("Pc")),
        "ReservedWord" -> ListSet(
            n("Keyword"),
            n("FutureReservedWord"),
            n("NullLiteral"),
            n("BooleanLiteral")),
        "Keyword" -> ListSet(
            i("break"), i("do"), i("instanceof"), i("typeof"),
            i("case"), i("else"), i("new"), i("var"),
            i("catch"), i("finally"), i("return"), i("void"),
            i("continue"), i("for"), i("switch"), i("while"),
            i("debugger"), i("function"), i("this"), i("with"),
            i("default"), i("if"), i("throw"),
            i("delete"), i("in"), i("try")),
        "FutureReservedWord" -> ListSet(
            i("class"), i("enum"), i("extends"), i("super"),
            i("const"), i("export"), i("import"),
            i("implements"), i("let"), i("private"), i("public"),
            i("interface"), i("package"), i("protected"), i("static"),
            i("yield")),
        "Punctuator" -> ListSet(
            i("{"), i("}"), i("("), i(")"), i("["), i("]"),
            i("."), i(";"), i(","), i("<"), i(">"), i("<="),
            i(">="), i("=="), i("!="), i("==="), i("!=="),
            i("+"), i("-"), i("*"), i("%"), i("++"), i("--"),
            i("<<"), i(">>"), i(">>>"), i("&"), i("|"), i("^"),
            i("!"), i("~"), i("&&"), i("||"), i("?"), i(":"),
            i("="), i("+="), i("-="), i("*="), i("%="), i("<<="),
            i(">>="), i(">>>="), i("&="), i("|="), i("^=")),
        "DivPunctuator" -> ListSet(
            i("/"), i("/=")),
        "Literal" -> ListSet(
            n("NullLiteral"),
            n("BooleanLiteral"),
            n("NumericLiteral"),
            n("StringLiteral"),
            n("RegularExpressionLiteral")),
        "NullLiteral" -> ListSet(
            i("null")),
        "BooleanLiteral" -> ListSet(
            i("true"),
            i("false")),
        "NumericLiteral" -> ListSet(
            n("DecimalLiteral"),
            n("HexIntegerLiteral")),
        "DecimalLiteral" -> ListSet(
            lex(n("DecimalIntegerLiteral"), i("."), n("DecimalDigits").opt, n("ExponentPart").opt),
            lex(i("."), n("DecimalDigits"), n("ExponentPart").opt),
            lex(n("DecimalIntegerLiteral"), n("ExponentPart").opt)),
        "DecimalIntegerLiteral" -> ListSet(
            i("0"),
            lex(n("NonZeroDigit"), n("DecimalDigits").opt)),
        "DecimalDigits" -> ListSet(
            n("DecimalDigit"),
            lex(n("DecimalDigits"), n("DecimalDigit"))),
        "DecimalDigit" -> ListSet(
            chars('0' to '9')),
        "NonZeroDigit" -> ListSet(
            chars('1' to '9')),
        "ExponentPart" -> ListSet(
            lex(n("ExponentIndicator"), n("SignedInteger"))),
        "ExponentIndicator" -> ListSet(
            chars("eE")),
        "SignedInteger" -> ListSet(
            n("DecimalDigits"),
            lex(i("+"), n("DecimalDigits")),
            lex(i("-"), n("DecimalDigits"))),
        "HexIntegerLiteral" -> ListSet(
            lex(i("0x"), n("HexDigit")),
            lex(i("0X"), n("HexDigit")),
            lex(n("HexIntegerLiteral"), n("HexDigit"))),
        "HexDigit" -> ListSet(
            chars('0' to '9', 'a' to 'f', 'A' to 'F')),
        "StringLiteral" -> ListSet(
            lex(i("\""), n("DoubleStringCharacters").opt, i("\"")),
            lex(i("'"), n("SingleStringCharacters").opt, i("'"))),
        "DoubleStringCharacters" -> ListSet(
            lex(n("DoubleStringCharacter"), n("DoubleStringCharacters").opt)),
        "SingleStringCharacters" -> ListSet(
            lex(n("SingleStringCharacter"), n("SingleStringCharacters").opt)),
        "DoubleStringCharacter" -> ListSet(
            n("SourceCharacter").butnot(chars("\"\\"), n("LineTerminator")),
            lex(i("\\"), n("EscapeSequence")),
            n("LineContinuation")),
        "SingleStringCharacter" -> ListSet(
            n("SourceCharacter").butnot(chars("'\\"), n("LineTerminator")),
            lex(i("\\"), n("EscapeSequence")),
            n("LineContinuation")),
        "LineContinuation" -> ListSet(
            lex(i("\\"), n("LineTerminatorSequence"))),
        "EscapeSequence" -> ListSet(
            n("CharacterEscapeSequence"),
            lex(i("0"), lookahead_except(n("DecimalDigit"))),
            n("HexEscapeSequence"),
            n("UnicodeEscapeSequence")),
        "CharacterEscapeSequence" -> ListSet(
            n("SingleEscapeCharacter"),
            n("NonEscapeCharacter")),
        "SingleEscapeCharacter" -> ListSet(
            chars("'\"\\bfnrtv")),
        "NonEscapeCharacter" -> ListSet(
            n("SourceCharacter").butnot(n("EscapeCharacter"), n("LineTerminator"))),
        "EscapeCharacter" -> ListSet(
            n("SingleEscapeCharacter"),
            n("DecimalDigit"),
            chars("xu")),
        "HexEscapeSequence" -> ListSet(
            lex(i("x"), n("HexDigit"), n("HexDigit"))),
        "UnicodeEscapeSequence" -> ListSet(
            lex(i("u"), n("HexDigit"), n("HexDigit"), n("HexDigit"), n("HexDigit"))),
        "RegularExpressionLiteral" -> ListSet(
            lex(i("/"), n("RegularExpressionBody"), i("/"), n("RegularExpressionFlags"))),
        "RegularExpressionBody" -> ListSet(
            lex(n("RegularExpressionFirstChar"), n("RegularExpressionChars"))),
        "RegularExpressionChars" -> ListSet(
            e,
            lex(n("RegularExpressionChars"), n("RegularExpressionChar"))),
        "RegularExpressionFirstChar" -> ListSet(
            n("RegularExpressionNonTerminator").butnot(chars("*\\/[")),
            n("RegularExpressionBackslashSequence"),
            n("RegularExpressionClass")),
        "RegularExpressionChar" -> ListSet(
            n("RegularExpressionNonTerminator").butnot(chars("\\/[")),
            n("RegularExpressionBackslashSequence"),
            n("RegularExpressionClass")),
        "RegularExpressionBackslashSequence" -> ListSet(
            lex(i("\\"), n("RegularExpressionNonTerminator"))),
        "RegularExpressionNonTerminator" -> ListSet(
            n("SourceCharacter").butnot(n("LineTerminator"))),
        "RegularExpressionClass" -> ListSet(
            lex(i("["), n("RegularExpressionClassChars"), i("]"))),
        "RegularExpressionClassChars" -> ListSet(
            e,
            lex(n("RegularExpressionClassChars"), n("RegularExpressionClassChar"))),
        "RegularExpressionClassChar" -> ListSet(
            n("RegularExpressionNonTerminator").butnot(chars("]\\")),
            n("RegularExpressionBackslashSequence")),
        "RegularExpressionFlags" -> ListSet(
            e,
            lex(n("RegularExpressionFlags"), n("IdentifierPart"))),

        // A.2 Number Conversions
        "StringNumericLiteral" -> ListSet(
            n("StrWhiteSpace").opt,
            lex(n("StrWhiteSpace").opt, n("StrNumericLiteral"), n("StrWhiteSpace").opt)),
        "StrWhiteSpace" -> ListSet(
            lex(n("StrWhiteSpaceChar"), n("StrWhiteSpace").opt)),
        "StrWhiteSpaceChar" -> ListSet(
            n("WhiteSpace"),
            n("LineTerminator")),
        "StrNumericLiteral" -> ListSet(
            n("StrDecimalLiteral"),
            n("HexIntegerLiteral")),
        "StrDecimalLiteral" -> ListSet(
            n("StrUnsignedDecimalLiteral"),
            lex(i("+"), n("StrUnsignedDecimalLiteral")),
            lex(i("-"), n("StrUnsignedDecimalLiteral"))),
        "StrUnsignedDecimalLiteral" -> ListSet(
            i("Infinity"),
            lex(n("DecimalDigits"), i("."), n("DecimalDigits").opt, n("ExponentPart").opt),
            lex(i("."), n("DecimalDigits"), n("ExponentPart").opt),
            lex(n("DecimalDigits"), n("ExponentPart").opt)),
        "DecimalDigits" -> ListSet(
            n("DecimalDigit"),
            lex(n("DecimalDigits"), n("DecimalDigit"))),
        "DecimalDigit" -> ListSet(
            chars('0' to '9')),
        "ExponentPart" -> ListSet(
            lex(n("ExponentIndicator"), n("SignedInteger"))),
        "ExponentIndicator" -> ListSet(
            chars("eE")),
        "SignedInteger" -> ListSet(
            n("DecimalDigits"),
            lex(i("+"), n("DecimalDigits")),
            lex(i("-"), n("DecimalDigits"))),
        "HexIntegerLiteral" -> ListSet(
            lex(i("0x"), n("HexDigit")),
            lex(i("0X"), n("HexDigit")),
            lex(n("HexIntegerLiteral"), n("HexDigit"))),
        "HexDigit" -> ListSet(
            chars('0' to '9', 'a' to 'f', 'A' to 'F')),

        // A.3 Expressions
        "PrimaryExpression" -> ListSet(
            i("this"),
            n("Identifier"),
            n("Literal"),
            n("ArrayLiteral"),
            n("ObjectLiteral"),
            expr(i("("), n("Expression"), i(")"))),
        "ArrayLiteral" -> ListSet(
            expr(i("["), n("Elision").opt, i("]")),
            expr(i("["), n("ElementList"), i("]")),
            expr(i("["), n("ElementList"), i(","), n("Elision").opt, i("]"))),
        "ElementList" -> ListSet(
            expr(n("Elision").opt, n("AssignmentExpression")),
            expr(n("ElementList"), i(","), n("Elision").opt, n("AssignmentExpression"))),
        "Elision" -> ListSet(
            i(","),
            expr(n("Elision"), i(","))),
        "ObjectLiteral" -> ListSet(
            expr(i("{"), i("}")),
            expr(i("{"), n("PropertyNameAndValueList"), i("}")),
            expr(i("{"), n("PropertyNameAndValueList"), i(","), i("}"))),
        "PropertyNameAndValueList" -> ListSet(
            n("PropertyAssignment"),
            expr(n("PropertyNameAndValueList"), i(","), n("PropertyAssignment"))),
        "PropertyAssignment" -> ListSet(
            expr(n("PropertyName"), i(":"), n("AssignmentExpression")),
            expr(i("get"), n("PropertyName"), i("("), i(")"), i("{"), n("FunctionBody"), i("}")),
            expr(i("set"), n("PropertyName"), i("("), n("PropertySetParameterList"), i(")"), i("{"), n("FunctionBody"), i("}"))),
        "PropertyName" -> ListSet(
            n("IdentifierName"),
            n("StringLiteral"),
            n("NumericLiteral")),
        "PropertySetParameterList" -> ListSet(
            n("Identifier")),
        "MemberExpression" -> ListSet(
            n("PrimaryExpression"),
            n("FunctionExpression"),
            expr(n("MemberExpression"), i("["), n("Expression"), i("]")),
            expr(n("MemberExpression"), i("."), n("IdentifierName")),
            expr(i("new"), n("MemberExpression"), n("Arguments"))),
        "NewExpression" -> ListSet(
            n("MemberExpression"),
            expr(i("new"), n("NewExpression"))),
        "CallExpression" -> ListSet(
            expr(n("MemberExpression"), n("Arguments")),
            expr(n("CallExpression"), n("Arguments")),
            expr(n("CallExpression"), i("["), n("Expression"), i("]")),
            expr(n("CallExpression"), i("."), n("IdentifierName"))),
        "Arguments" -> ListSet(
            expr(i("("), i(")")),
            expr(i("("), n("ArgumentList"), i(")"))),
        "ArgumentList" -> ListSet(
            n("AssignmentExpression"),
            expr(n("ArgumentList"), i(","), n("AssignmentExpression"))),
        "LeftHandSideExpression" -> ListSet(
            n("NewExpression"),
            n("CallExpression")),
        "PostfixExpression" -> ListSet(
            n("LeftHandSideExpression"),
            line(n("LeftHandSideExpression"), i("++")),
            line(n("LeftHandSideExpression"), i("--"))),
        "UnaryExpression" -> ListSet(
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
        "MultiplicativeExpression" -> ListSet(
            n("UnaryExpression"),
            expr(n("MultiplicativeExpression"), i("*"), n("UnaryExpression")),
            expr(n("MultiplicativeExpression"), i("/"), n("UnaryExpression")),
            expr(n("MultiplicativeExpression"), i("%"), n("UnaryExpression"))),
        "AdditiveExpression" -> ListSet(
            n("MultiplicativeExpression"),
            expr(n("AdditiveExpression"), i("+"), n("MultiplicativeExpression")),
            expr(n("AdditiveExpression"), i("-"), n("MultiplicativeExpression"))),
        "ShiftExpression" -> ListSet(
            n("AdditiveExpression"),
            expr(n("ShiftExpression"), i("<<"), n("AdditiveExpression")),
            expr(n("ShiftExpression"), i(">>"), n("AdditiveExpression")),
            expr(n("ShiftExpression"), i(">>>"), n("AdditiveExpression"))),
        "RelationalExpression" -> ListSet(
            n("ShiftExpression"),
            expr(n("RelationalExpression"), i("<"), n("ShiftExpression")),
            expr(n("RelationalExpression"), i(">"), n("ShiftExpression")),
            expr(n("RelationalExpression"), i("<="), n("ShiftExpression")),
            expr(n("RelationalExpression"), i(">="), n("ShiftExpression")),
            expr(n("RelationalExpression"), i("instanceof"), n("ShiftExpression"))),
        "RelationalExpressionNoIn" -> ListSet(
            n("ShiftExpression"),
            expr(n("RelationalExpressionNoIn"), i("<"), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), i(">"), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), i("<="), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), i(">="), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), i("instanceof"), n("ShiftExpression"))),
        "EqualityExpression" -> ListSet(
            n("RelationalExpression"),
            expr(n("EqualityExpression"), i("=="), n("RelationalExpression")),
            expr(n("EqualityExpression"), i("!=="), n("RelationalExpression")),
            expr(n("EqualityExpression"), i("==="), n("RelationalExpression")),
            expr(n("EqualityExpression"), i("!=="), n("RelationalExpression"))),
        "EqualityExpressionNoIn" -> ListSet(
            n("RelationalExpressionNoIn"),
            expr(n("EqualityExpressionNoIn"), i("=="), n("RelationalExpressionNoIn")),
            expr(n("EqualityExpressionNoIn"), i("!=="), n("RelationalExpressionNoIn")),
            expr(n("EqualityExpressionNoIn"), i("==="), n("RelationalExpressionNoIn")),
            expr(n("EqualityExpressionNoIn"), i("!=="), n("RelationalExpressionNoIn"))),
        "BitwiseANDExpression" -> ListSet(
            n("EqualityExpression"),
            expr(n("BitwiseANDExpression"), i("&"), n("EqualityExpression"))),
        "BitwiseANDExpressionNoIn" -> ListSet(
            n("EqualityExpressionNoIn"),
            expr(n("BitwiseANDExpressionNoIn"), i("&"), n("EqualityExpressionNoIn"))),
        "BitwiseXORExpression" -> ListSet(
            n("BitwiseANDExpression"),
            expr(n("BitwiseXORExpression"), i("^"), n("BitwiseANDExpression"))),
        "BitwiseXORExpressionNoIn" -> ListSet(
            n("BitwiseANDExpressionNoIn"),
            expr(n("BitwiseXORExpressionNoIn"), i("^"), n("BitwiseANDExpressionNoIn"))),
        "BitwiseORExpression" -> ListSet(
            n("BitwiseXORExpression"),
            expr(n("BitwiseORExpression"), i("^"), n("BitwiseXORExpression"))),
        "BitwiseORExpressionNoIn" -> ListSet(
            n("BitwiseXORExpressionNoIn"),
            expr(n("BitwiseORExpressionNoIn"), i("^"), n("BitwiseXORExpressionNoIn"))),
        "LogicalANDExpression" -> ListSet(
            n("BitwiseORExpression"),
            expr(n("LogicalANDExpression"), i("&&"), n("BitwiseORExpression"))),
        "LogicalANDExpressionNoIn" -> ListSet(
            n("BitwiseORExpressionNoIn"),
            expr(n("LogicalANDExpressionNoIn"), i("&&"), n("BitwiseORExpressionNoIn"))),
        "LogicalORExpression" -> ListSet(
            n("LogicalANDExpression"),
            expr(n("LogicalORExpression"), i("||"), n("LogicalANDExpression"))),
        "LogicalORExpressionNoIn" -> ListSet(
            n("LogicalANDExpressionNoIn"),
            expr(n("LogicalORExpressionNoIn"), i("||"), n("LogicalANDExpressionNoIn"))),
        "ConditionalExpression" -> ListSet(
            n("LogicalORExpression"),
            expr(n("LogicalORExpression"), i("?"), n("AssignmentExpression"), i(":"), n("AssignmentExpression"))),
        "ConditionalExpressionNoIn" -> ListSet(
            n("LogicalORExpressionNoIn"),
            expr(n("LogicalORExpressionNoIn"), i("?"), n("AssignmentExpressionNoIn"), i(":"), n("AssignmentExpressionNoIn"))),
        "AssignmentExpression" -> ListSet(
            n("ConditionalExpression"),
            expr(n("LeftHandSideExpression"), i("="), n("AssignmentExpression")),
            expr(n("LeftHandSideExpression"), n("AssignmentOperator"), n("AssignmentExpression"))),
        "AssignmentExpressionNoIn" -> ListSet(
            n("ConditionalExpressionNoIn"),
            expr(n("LeftHandSideExpression"), i("="), n("AssignmentExpressionNoIn")),
            expr(n("LeftHandSideExpression"), n("AssignmentOperator"), n("AssignmentExpressionNoIn"))),
        "AssignmentOperator" -> ListSet(
            i("*="), i("/="), i("%="), i("+="), i("-="), i("<<="), i(">>="), i(">>>="), i("&="), i("^="), i("|=")),
        "Expression" -> ListSet(
            n("AssignmentExpression"),
            expr(n("Expression"), i(","), n("AssignmentExpression"))),
        "ExpressionNoIn" -> ListSet(
            n("AssignmentExpressionNoIn"),
            expr(n("ExpressionNoIn"), i(","), n("AssignmentExpressionNoIn"))),

        // A.4 Statements
        "Statement" -> ListSet(
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
        "Block" -> ListSet(
            expr(i("{"), n("StatementList").opt, i("}"))),
        "StatementList" -> ListSet(
            n("Statement"),
            expr(n("StatementList"), n("Statement"))),
        "VariableStatement" -> ListSet(
            stmt(i("var"), n("VariableDeclarationList"))),
        "VariableDeclarationList" -> ListSet(
            n("VariableDeclaration"),
            expr(n("VariableDeclarationList"), i(","), n("VariableDeclaration"))),
        "VariableDeclarationListNoIn" -> ListSet(
            n("VariableDeclarationNoIn"),
            expr(n("VariableDeclarationListNoIn"), i(","), n("VariableDeclarationNoIn"))),
        "VariableDeclaration" -> ListSet(
            expr(n("Identifier"), n("Initialiser").opt)),
        "VariableDeclarationNoIn" -> ListSet(
            expr(n("Identifier"), n("InitialiserNoIn").opt)),
        "Initialiser" -> ListSet(
            expr(i("="), n("AssignmentExpression"))),
        "InitialiserNoIn" -> ListSet(
            expr(i("="), n("AssignmentExpressionNoIn"))),
        "EmptyStatement" -> ListSet(
            stmt()),
        "ExpressionStatement" -> ListSet(
            stmt(lookahead_except(i("{"), seq(i("function"), n("WhiteSpace"))), n("Expression"))),
        "IfStatement" -> ListSet(
            expr(i("if"), i("("), n("Expression"), i(")"), n("Statement"), i("else"), n("Statement")),
            expr(i("if"), i("("), n("Expression"), i(")"), n("Statement"))),
        "IterationStatement" -> ListSet(
            stmt(i("do"), n("Statement"), i("while"), i("("), n("Expression"), i(")")),
            expr(i("while"), i("("), n("Expression"), i(")"), n("Statement")),
            expr(i("for"), i("("), n("ExpressionNoIn").opt, i(";"), n("Expression").opt, i(";"), n("Expression").opt, i(")"), n("Statement")),
            expr(i("for"), i("("), i("var"), n("VariableDeclarationListNoIn"), i(";"), n("Expression").opt, i(";"), n("Expression").opt, i(")"), n("Statement")),
            expr(i("for"), i("("), n("LeftHandSideExpression"), i("in"), n("Expression"), i(")"), n("Statement")),
            expr(i("for"), i("("), i("var"), n("VariableDeclarationNoIn"), i("in"), n("Expression"), i(")"), n("Statement"))),
        "ContinueStatement" -> ListSet(
            stmt(i("continue")),
            stmt(line(i("continue"), n("Identifier")))),
        "BreakStatement" -> ListSet(
            stmt(i("break")),
            stmt(line(i("break"), n("Identifier")))),
        "ReturnStatement" -> ListSet(
            stmt(i("return")),
            stmt(line(i("return"), n("Expression")))),
        "WithStatement" -> ListSet(
            expr(i("with"), i("("), n("Expression"), i(")"), n("Statement"))),
        "SwitchStatement" -> ListSet(
            expr(i("switch"), i("("), n("Expression"), i(")"), n("CaseBlock"))),
        "CaseBlock" -> ListSet(
            expr(i("{"), n("CaseClauses").opt, i("}")),
            expr(i("{"), n("CaseClauses").opt, n("DefaultClause"), n("CaseClauses").opt, i("}"))),
        "CaseClauses" -> ListSet(
            n("CaseClause"),
            expr(n("CaseClauses"), n("CaseClause"))),
        "CaseClause" -> ListSet(
            expr(i("case"), n("Expression"), i(":"), n("StatementList").opt)),
        "DefaultClause" -> ListSet(
            expr(i("default"), i(":"), n("StatementList").opt)),
        "LabelledStatement" -> ListSet(
            expr(n("Identifier"), i(":"), n("Statement"))),
        "ThrowStatement" -> ListSet(
            stmt(line(i("throw"), n("Expression")))),
        "TryStatement" -> ListSet(
            expr(i("try"), n("Block"), n("Catch")),
            expr(i("try"), n("Block"), n("Finally")),
            expr(i("try"), n("Block"), n("Catch"), n("Finally"))),
        "Catch" -> ListSet(
            expr(i("catch"), i("("), n("Identifier"), i(")"), n("Block"))),
        "Finally" -> ListSet(
            expr(i("finally"), n("Block"))),
        "DebuggerStatement" -> ListSet(
            stmt(i("debugger"))),

        // A.5 Functions and Programs
        "FunctionDeclaration" -> ListSet(
            expr(i("function"), n("Identifier"), i("("), n("FormalParameterList").opt, i(")"), i("{"), n("FunctionBody"), i("}"))),
        "FunctionExpression" -> ListSet(
            expr(i("function"), n("Identifier").opt, i("("), n("FormalParameterList").opt, i(")"), i("{"), n("FunctionBody"), i("}"))),
        "FormalParameterList" -> ListSet(
            n("Identifier"),
            expr(n("FormalParameterList"), i(","), n("Identifier"))),
        "FunctionBody" -> ListSet(
            n("SourceElements").opt),
        "Program" -> ListSet(
            n("SourceElements").opt),
        "SourceElements" -> ListSet(
            n("SourceElement"),
            expr(n("SourceElements"), n("SourceElement"))),
        "SourceElement" -> ListSet(
            n("Statement"),
            n("FunctionDeclaration")),

        // A.6 Universal Resource Identifier Character Classes
        "uri" -> ListSet(
            n("uriCharacters").opt),
        "uriCharacters" -> ListSet(
            lex(n("uriCharacter"), n("uriCharacters").opt)),
        "uriCharacter" -> ListSet(
            n("uriReserved"),
            n("uriUnescaped"),
            n("uriEscaped")),
        "uriReserved" -> ListSet(
            chars(";/?:@&=+$,")),
        "uriUnescaped" -> ListSet(
            n("uriAlpha"),
            n("DecimalDigit"),
            n("uriMark")),
        "uriEscaped" -> ListSet(
            lex(i("%"), n("HexDigit"), n("HexDigit"))),
        "uriAlpha" -> ListSet(
            chars('a' to 'z', 'A' to 'Z')),
        "uriMark" -> ListSet(
            chars("-_.!~*'()")),

        // A.7 Regular Expressions
        "Pattern" -> ListSet(
            n("Disjunction")),
        "Disjunction" -> ListSet(
            n("Alternative"),
            lex(n("Alternative"), i("|"), n("Disjunction"))),
        "Alternative" -> ListSet(
            e,
            lex(n("Alternative"), n("Term"))),
        "Term" -> ListSet(
            n("Assertion"),
            n("Atom"),
            lex(n("Atom"), n("Quantifier"))),
        "Assertion" -> ListSet(
            i("^"),
            i("$"),
            lex(i("\\"), i("b")),
            lex(i("\\"), i("B")),
            lex(i("("), i("?"), i("="), n("Disjunction"), i(")")),
            lex(i("("), i("?"), i("!"), n("Disjunction"), i(")"))),
        "Quantifier" -> ListSet(
            n("QuantifierPrefix"),
            lex(n("QuantifierPrefix"), i("?"))),
        "QuantifierPrefix" -> ListSet(
            i("*"),
            i("+"),
            i("?"),
            lex(i("{"), n("DecimalDigits"), i("}")),
            lex(i("{"), n("DecimalDigits"), i(","), i("}")),
            lex(i("{"), n("DecimalDigits"), i("}"))),
        "Atom" -> ListSet(
            n("PatternCharacter"),
            i("."),
            lex(i("\\"), n("AtomEscape")),
            n("CharacterClass"),
            lex(i("("), n("Disjunction"), i(")")),
            lex(i("("), i("?"), i(":"), n("Disjunction"), i(")"))),
        "PatternCharacter" -> ListSet(
            n("SourceCharacter").butnot(chars("^$\\.*+?()[]{}|"))),
        "AtomEscape" -> ListSet(
            n("DecimalEscape"),
            n("CharacterEscape"),
            n("CharacterClassEscape")),
        "CharacterEscape" -> ListSet(
            n("ControlEscape"),
            lex(i("c"), n("ControlLetter")),
            n("HexEscapeSequence"),
            n("UnicodeEscapeSequence"),
            n("IdentityEscape")),
        "ControlEscape" -> ListSet(
            chars("fnrtv")),
        "ControlLetter" -> ListSet(
            chars('a' to 'z', 'A' to 'Z')),
        "IdentityEscape" -> ListSet(
            n("SourceCharacter").butnot(n("IdentifierPart"), chars("\u200C\u200D"))),
        "DecimalEscape" -> ListSet(
            lex(n("DecimalIntegerLiteral"), lookahead_except(n("DecimalDigit")))),
        "CharacterClassEscape" -> ListSet(
            chars("dDsSwW")),
        "CharacterClass" -> ListSet(
            lex(i("["), lookahead_except(i("^")), n("ClassRanges"), i("]")),
            lex(i("["), i("^"), n("ClassRanges"), i("]"))),
        "ClassRanges" -> ListSet(
            e,
            n("NonemptyClassRanges")),
        "NonemptyClassRanges" -> ListSet(
            n("ClassAtom"),
            lex(n("ClassAtom"), n("NonemptyClassRangesNoDash")),
            lex(n("ClassAtom"), i("-"), n("ClassAtom"), n("ClassRanges"))),
        "NonemptyClassRangesNoDash" -> ListSet(
            n("ClassAtom"),
            lex(n("ClassAtomNoDash"), n("NonemptyClassRangesNoDash")),
            lex(n("ClassAtomNoDash"), i("-"), n("ClassAtom"), n("ClassRanges"))),
        "ClassAtom" -> ListSet(
            i("-"),
            n("ClassAtomNoDash")),
        "ClassAtomNoDash" -> ListSet(
            n("SourceCharacter").butnot(chars("\\]-")),
            lex(i("\\"), n("ClassEscape"))),
        "ClassEscape" -> ListSet(
            n("DecimalEscape"),
            i("b"),
            n("CharacterEscape"),
            n("CharacterClassEscape")),

        // A.8 JSON
        "JSONWhiteSpace" -> ListSet(
            chars("\t\n\r ")),
        "JSONString" -> ListSet(
            lex(i("\""), n("JSONStringCharacters").opt, i("\""))),
        "JSONStringCharacters" -> ListSet(
            lex(n("JSONStringCharacter"), n("JSONStringCharacters").opt)),
        "JSONStringCharacter" -> ListSet(
            n("SourceCharacter").butnot(chars("\"\\\u0000\u001F")),
            lex(i("\\"), n("JSONEscapeSequence"))),
        "JSONEscapeSequence" -> ListSet(
            n("JSONEscapeCharacter"),
            n("UnicodeEscapeSequence")),
        "JSONEscapeCharacter" -> ListSet(
            chars("\"/\\bfnrt")),
        "JSONNumber" -> ListSet(
            lex(i("-").opt, n("DecimalIntegerLiteral"), n("JSONFraction").opt, n("ExponentPart").opt)),
        "JSONFraction" -> ListSet(
            lex(i("."), n("DecimalDigits"))),
        "JSONNullLiteral" -> ListSet(
            n("NullLiteral")),
        "JSONBooleanLiteral" -> ListSet(
            n("BooleanLiteral")),
        "JSONText" -> ListSet(
            n("JSONValue")),
        "JSONValue" -> ListSet(
            n("JSONNullLiteral"),
            n("JSONBooleanLiteral"),
            n("JSONObject"),
            n("JSONArray"),
            n("JSONString"),
            n("JSONNumber")),
        "JSONObject" -> ListSet(
            expr(i("{"), i("}")),
            expr(i("{"), n("JSONMemberList"), i("}"))),
        "JSONMember" -> ListSet(
            expr(n("JSONString"), i(":"), n("JSONValue"))),
        "JSONMemberList" -> ListSet(
            n("JSONMember"),
            expr(n("JSONMemberList"), i(","), n("JSONMember"))),
        "JSONArray" -> ListSet(
            expr(i("["), i("]")),
            expr(i("["), n("JSONElementList"), i("]"))),
        "JSONElementList" -> ListSet(
            n("JSONValue"),
            expr(n("JSONElementList"), i(","), n("JSONValue"))))
}
