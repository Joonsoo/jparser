package com.giyeok.jparser.examples

import scala.collection.immutable.ListMap
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Symbols.Symbol
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.Symbols._
import scala.collection.immutable.ListSet

object JavaScriptGrammar extends Grammar {
    private val whitespace = oneof(n("WhiteSpace"), n("LineTerminator"), n("Comment")).star
    private val oneline = oneof(n("WhiteSpace"), n("Comment")).star

    def expr(s: Symbol*) = seqWS(whitespace, s: _*)

    def lex(s: Symbol*) = seq(s: _*)

    def line(s: Symbol*) = seqWS(oneline, s: _*)

    val lineend = {
        val semicolon = i(";")
        val alternative = oneof(
            seq((n("WhiteSpace").except(n("LineTerminator"))).star, n("LineTerminator")),
            seq(n("WhiteSpace").star, lookahead_is(i("}")))
        )
        oneof(semicolon, seq(lookahead_except(semicolon), alternative))
    }

    def stmt(s: Symbol*) = longest(expr((s.toSeq :+ lineend): _*))

    def token(s: String) = i(s).join(n("Token"))

    def token(s: Symbol) = s.join(n("Token"))

    val name = "JavaScript"
    val startSymbol = n("Start")
    val rules: RuleMap = ListMap(
        "_Token" -> (ListSet(whitespace, n("IdentifierName"), n("Punctuator"), n("NumericLiteral"), n("StringLiteral"))),
        "_Raw" -> ListSet(n("RegularExpressionLiteral")),

        "Start" -> ListSet(n("Program")),

        // A.1 Lexical Grammar
        "SourceCharacter" -> ListSet(anychar),
        "InputElementDiv" -> ListSet(
            n("WhiteSpace"), n("LineTerminator"), n("Comment"), n("Token"), n("DivPunctuator")
        ),
        "InputElementRegExp" -> ListSet(
            n("WhiteSpace"), n("LineTerminator"), n("Comment"), n("Token"), n("RegularExpressionLiteral")
        ),
        "WhiteSpace" -> ListSet(
            chars("\u0009\u000B\u000C\uFEFF"), unicode("Zs")
        ), // \u0020\u00A0  ->  already in Zs
        "LineTerminator" -> ListSet(
            chars("\n\r\u2028\u2029")
        ),
        "LineTerminatorSequence" -> ListSet(
            chars("\n\u2028\u2029"), lex(c('\r'), lookahead_except(c('\n'))), i("\r\n")
        ),
        "Comment" -> ListSet(
            longest(n("MultiLineComment")),
            longest(n("SingleLineComment"))
        ),
        "MultiLineComment" -> ListSet(
            expr(i("/*"), n("MultiLineCommentChars").opt, i("*/"))
        ),
        "MultiLineCommentChars" -> ListSet(
            lex(n("MultiLineNotAsteriskChar"), n("MultiLineCommentChars").opt),
            lex(i("*"), n("PostAsteriskCommentChars").opt)
        ),
        "PostAsteriskCommentChars" -> ListSet(
            lex(n("MultiLineNotForwardSlashOrAsteriskChar"), n("MultiLineCommentChars").opt),
            lex(i("*"), n("PostAsteriskCommentChars").opt)
        ),
        "MultiLineNotAsteriskChar" -> ListSet(
            lex(n("SourceCharacter").butnot(i("*")))
        ),
        "MultiLineNotForwardSlashOrAsteriskChar" -> ListSet(
            lex(n("SourceCharacter").butnot(i("/"), i("*")))
        ),
        "SingleLineComment" -> ListSet(
            lex(i("//"), n("SingleLineCommentChars").opt)
        ),
        "SingleLineCommentChars" -> ListSet(
            lex(n("SingleLineCommentChar"), n("SingleLineCommentChars").opt)
        ),
        "SingleLineCommentChar" -> ListSet(
            n("SourceCharacter").butnot(n("LineTerminator"))
        ),
        "Token" -> ListSet(
            longest(n("IdentifierName")),
            n("Punctuator"),
            longest(n("NumericLiteral")),
            longest(n("StringLiteral"))
        ),
        "Identifier" -> ListSet(
            n("IdentifierName").butnot(n("ReservedWord"))
        ),
        "IdentifierName" -> ListSet(
            lex(n("IdentifierStart")),
            lex(n("IdentifierName"), n("IdentifierPart"))
        ),
        "IdentifierStart" -> ListSet(
            n("UnicodeLetter"),
            i("$"),
            i("_"),
            lex(i("\""), n("UnicodeEscapeSequence"))
        ),
        "IdentifierPart" -> ListSet(
            n("IdentifierStart"),
            n("UnicodeCombiningMark"),
            n("UnicodeDigit"),
            n("UnicodeConnectorPunctuation"),
            chars("\u200C\u200D")
        ),
        "UnicodeLetter" -> ListSet(
            unicode("Lu", "Ll", "Lt", "Lm", "Lo", "Nl")
        ),
        "UnicodeCombiningMark" -> ListSet(
            unicode("Mn", "Mc")
        ),
        "UnicodeDigit" -> ListSet(
            unicode("Nd")
        ),
        "UnicodeConnectorPunctuation" -> ListSet(
            unicode("Pc")
        ),
        "ReservedWord" -> ListSet(
            n("Keyword"),
            n("FutureReservedWord"),
            n("NullLiteral"),
            n("BooleanLiteral")
        ),
        "Keyword" -> ListSet(
            i("break"), i("do"), i("instanceof"), i("typeof"),
            i("case"), i("else"), i("new"), i("var"),
            i("catch"), i("finally"), i("return"), i("void"),
            i("continue"), i("for"), i("switch"), i("while"),
            i("debugger"), i("function"), i("this"), i("with"),
            i("default"), i("if"), i("throw"),
            i("delete"), i("in"), i("try")
        ),
        "FutureReservedWord" -> ListSet(
            i("class"), i("enum"), i("extends"), i("super"),
            i("const"), i("export"), i("import"),
            i("implements"), i("let"), i("private"), i("public"),
            i("interface"), i("package"), i("protected"), i("static"),
            i("yield")
        ),
        "Punctuator" -> ListSet(
            i("{"), i("}"), i("("), i(")"), i("["), i("]"),
            i("."), i(";"), i(","), i("<"), i(">"), i("<="),
            i(">="), i("=="), i("!="), i("==="), i("!=="),
            i("+"), i("-"), i("*"), i("%"), i("++"), i("--"),
            i("<<"), i(">>"), i(">>>"), i("&"), i("|"), i("^"),
            i("!"), i("~"), i("&&"), i("||"), i("?"), i(":"),
            i("="), i("+="), i("-="), i("*="), i("%="), i("<<="),
            i(">>="), i(">>>="), i("&="), i("|="), i("^=")
        ),
        "DivPunctuator" -> ListSet(
            i("/"), i("/=")
        ),
        "Literal" -> ListSet(
            n("NullLiteral"),
            n("BooleanLiteral"),
            n("NumericLiteral"),
            n("StringLiteral"),
            n("RegularExpressionLiteral")
        ),
        "NullLiteral" -> ListSet(
            i("null")
        ),
        "BooleanLiteral" -> ListSet(
            i("true"),
            i("false")
        ),
        "NumericLiteral" -> ListSet(
            n("DecimalLiteral"),
            n("HexIntegerLiteral")
        ),
        "DecimalLiteral" -> ListSet(
            lex(n("DecimalIntegerLiteral"), i("."), n("DecimalDigits").opt, n("ExponentPart").opt),
            lex(i("."), n("DecimalDigits"), n("ExponentPart").opt),
            lex(n("DecimalIntegerLiteral"), n("ExponentPart").opt)
        ),
        "DecimalIntegerLiteral" -> ListSet(
            i("0"),
            lex(n("NonZeroDigit"), n("DecimalDigits").opt)
        ),
        "DecimalDigits" -> ListSet(
            n("DecimalDigit"),
            lex(n("DecimalDigits"), n("DecimalDigit"))
        ),
        "DecimalDigit" -> ListSet(
            chars('0' to '9')
        ),
        "NonZeroDigit" -> ListSet(
            chars('1' to '9')
        ),
        "ExponentPart" -> ListSet(
            lex(n("ExponentIndicator"), n("SignedInteger"))
        ),
        "ExponentIndicator" -> ListSet(
            chars("eE")
        ),
        "SignedInteger" -> ListSet(
            n("DecimalDigits"),
            lex(i("+"), n("DecimalDigits")),
            lex(i("-"), n("DecimalDigits"))
        ),
        "HexIntegerLiteral" -> ListSet(
            lex(i("0x"), n("HexDigit")),
            lex(i("0X"), n("HexDigit")),
            lex(n("HexIntegerLiteral"), n("HexDigit"))
        ),
        "HexDigit" -> ListSet(
            chars('0' to '9', 'a' to 'f', 'A' to 'F')
        ),
        "StringLiteral" -> ListSet(
            lex(i("\""), n("DoubleStringCharacters").opt, i("\"")),
            lex(i("'"), n("SingleStringCharacters").opt, i("'"))
        ),
        "DoubleStringCharacters" -> ListSet(
            lex(n("DoubleStringCharacter"), n("DoubleStringCharacters").opt)
        ),
        "SingleStringCharacters" -> ListSet(
            lex(n("SingleStringCharacter"), n("SingleStringCharacters").opt)
        ),
        "DoubleStringCharacter" -> ListSet(
            n("SourceCharacter").butnot(chars("\"\\"), n("LineTerminator")),
            lex(i("\\"), n("EscapeSequence")),
            n("LineContinuation")
        ),
        "SingleStringCharacter" -> ListSet(
            n("SourceCharacter").butnot(chars("'\\"), n("LineTerminator")),
            lex(i("\\"), n("EscapeSequence")),
            n("LineContinuation")
        ),
        "LineContinuation" -> ListSet(
            lex(i("\\"), n("LineTerminatorSequence"))
        ),
        "EscapeSequence" -> ListSet(
            n("CharacterEscapeSequence"),
            lex(i("0"), lookahead_except(n("DecimalDigit"))),
            n("HexEscapeSequence"),
            n("UnicodeEscapeSequence")
        ),
        "CharacterEscapeSequence" -> ListSet(
            n("SingleEscapeCharacter"),
            n("NonEscapeCharacter")
        ),
        "SingleEscapeCharacter" -> ListSet(
            chars("'\"\\bfnrtv")
        ),
        "NonEscapeCharacter" -> ListSet(
            n("SourceCharacter").butnot(n("EscapeCharacter"), n("LineTerminator"))
        ),
        "EscapeCharacter" -> ListSet(
            n("SingleEscapeCharacter"),
            n("DecimalDigit"),
            chars("xu")
        ),
        "HexEscapeSequence" -> ListSet(
            lex(i("x"), n("HexDigit"), n("HexDigit"))
        ),
        "UnicodeEscapeSequence" -> ListSet(
            lex(i("u"), n("HexDigit"), n("HexDigit"), n("HexDigit"), n("HexDigit"))
        ),
        "RegularExpressionLiteral" -> ListSet(
            lex(i("/"), n("RegularExpressionBody"), i("/"), n("RegularExpressionFlags"))
        ),
        "RegularExpressionBody" -> ListSet(
            lex(n("RegularExpressionFirstChar"), n("RegularExpressionChars"))
        ),
        "RegularExpressionChars" -> ListSet(
            empty,
            lex(n("RegularExpressionChars"), n("RegularExpressionChar"))
        ),
        "RegularExpressionFirstChar" -> ListSet(
            n("RegularExpressionNonTerminator").butnot(chars("*\\/[")),
            n("RegularExpressionBackslashSequence"),
            n("RegularExpressionClass")
        ),
        "RegularExpressionChar" -> ListSet(
            n("RegularExpressionNonTerminator").butnot(chars("\\/[")),
            n("RegularExpressionBackslashSequence"),
            n("RegularExpressionClass")
        ),
        "RegularExpressionBackslashSequence" -> ListSet(
            lex(i("\\"), n("RegularExpressionNonTerminator"))
        ),
        "RegularExpressionNonTerminator" -> ListSet(
            n("SourceCharacter").butnot(n("LineTerminator"))
        ),
        "RegularExpressionClass" -> ListSet(
            lex(i("["), n("RegularExpressionClassChars"), i("]"))
        ),
        "RegularExpressionClassChars" -> ListSet(
            empty,
            lex(n("RegularExpressionClassChars"), n("RegularExpressionClassChar"))
        ),
        "RegularExpressionClassChar" -> ListSet(
            n("RegularExpressionNonTerminator").butnot(chars("]\\")),
            n("RegularExpressionBackslashSequence")
        ),
        "RegularExpressionFlags" -> ListSet(
            empty,
            lex(n("RegularExpressionFlags"), n("IdentifierPart"))
        ),

        // A.2 Number Conversions
        "StringNumericLiteral" -> ListSet(
            n("StrWhiteSpace").opt,
            lex(n("StrWhiteSpace").opt, n("StrNumericLiteral"), n("StrWhiteSpace").opt)
        ),
        "StrWhiteSpace" -> ListSet(
            lex(n("StrWhiteSpaceChar"), n("StrWhiteSpace").opt)
        ),
        "StrWhiteSpaceChar" -> ListSet(
            n("WhiteSpace"),
            n("LineTerminator")
        ),
        "StrNumericLiteral" -> ListSet(
            n("StrDecimalLiteral"),
            n("HexIntegerLiteral")
        ),
        "StrDecimalLiteral" -> ListSet(
            n("StrUnsignedDecimalLiteral"),
            lex(i("+"), n("StrUnsignedDecimalLiteral")),
            lex(i("-"), n("StrUnsignedDecimalLiteral"))
        ),
        "StrUnsignedDecimalLiteral" -> ListSet(
            token("Infinity"),
            lex(n("DecimalDigits"), i("."), n("DecimalDigits").opt, n("ExponentPart").opt),
            lex(i("."), n("DecimalDigits"), n("ExponentPart").opt),
            lex(n("DecimalDigits"), n("ExponentPart").opt)
        ),
        "DecimalDigits" -> ListSet(
            n("DecimalDigit"),
            lex(n("DecimalDigits"), n("DecimalDigit"))
        ),
        "DecimalDigit" -> ListSet(
            chars('0' to '9')
        ),
        "ExponentPart" -> ListSet(
            lex(n("ExponentIndicator"), n("SignedInteger"))
        ),
        "ExponentIndicator" -> ListSet(
            chars("eE")
        ),
        "SignedInteger" -> ListSet(
            n("DecimalDigits"),
            lex(i("+"), n("DecimalDigits")),
            lex(i("-"), n("DecimalDigits"))
        ),
        "HexIntegerLiteral" -> ListSet(
            lex(i("0x"), n("HexDigit")),
            lex(i("0X"), n("HexDigit")),
            lex(n("HexIntegerLiteral"), n("HexDigit"))
        ),
        "HexDigit" -> ListSet(
            chars('0' to '9', 'a' to 'f', 'A' to 'F')
        ),

        // A.3 Expressions
        "PrimaryExpression" -> ListSet(
            token("this"),
            token(n("Identifier")),
            token(n("Literal")),
            n("ArrayLiteral"),
            n("ObjectLiteral"),
            expr(token("("), n("Expression"), token(")"))
        ),
        "ArrayLiteral" -> ListSet(
            expr(token("["), n("Elision").opt, token("]")),
            expr(token("["), n("ElementList"), token("]")),
            expr(token("["), n("ElementList"), token(","), n("Elision").opt, token("]"))
        ),
        "ElementList" -> ListSet(
            expr(n("Elision").opt, n("AssignmentExpression")),
            expr(n("ElementList"), token(","), n("Elision").opt, n("AssignmentExpression"))
        ),
        "Elision" -> ListSet(
            token(","),
            expr(n("Elision"), token(","))
        ),
        "ObjectLiteral" -> ListSet(
            expr(token("{"), token("}")),
            expr(token("{"), n("PropertyNameAndValueList"), token("}")),
            expr(token("{"), n("PropertyNameAndValueList"), token(","), token("}"))
        ),
        "PropertyNameAndValueList" -> ListSet(
            n("PropertyAssignment"),
            expr(n("PropertyNameAndValueList"), token(","), n("PropertyAssignment"))
        ),
        "PropertyAssignment" -> ListSet(
            expr(n("PropertyName"), token(":"), n("AssignmentExpression")),
            expr(token("get"), n("PropertyName"), token("("), token(")"), token("{"), n("FunctionBody"), token("}")),
            expr(token("set"), n("PropertyName"), token("("), n("PropertySetParameterList"), token(")"), token("{"), n("FunctionBody"), token("}"))
        ),
        "PropertyName" -> ListSet(
            token(n("IdentifierName")),
            token(n("StringLiteral")),
            token(n("NumericLiteral"))
        ),
        "PropertySetParameterList" -> ListSet(
            token(n("Identifier"))
        ),
        "MemberExpression" -> ListSet(
            n("PrimaryExpression"),
            n("FunctionExpression"),
            expr(n("MemberExpression"), token("["), n("Expression"), token("]")),
            expr(n("MemberExpression"), token("."), token(n("IdentifierName"))),
            expr(token("new"), n("MemberExpression"), n("Arguments"))
        ),
        "NewExpression" -> ListSet(
            n("MemberExpression"),
            expr(token("new"), n("NewExpression"))
        ),
        "CallExpression" -> ListSet(
            expr(n("MemberExpression"), n("Arguments")),
            expr(n("CallExpression"), n("Arguments")),
            expr(n("CallExpression"), token("["), n("Expression"), token("]")),
            expr(n("CallExpression"), token("."), token(n("IdentifierName")))
        ),
        "Arguments" -> ListSet(
            expr(token("("), token(")")),
            expr(token("("), n("ArgumentList"), token(")"))
        ),
        "ArgumentList" -> ListSet(
            n("AssignmentExpression"),
            expr(n("ArgumentList"), token(","), n("AssignmentExpression"))
        ),
        "LeftHandSideExpression" -> ListSet(
            n("NewExpression"),
            n("CallExpression")
        ),
        "PostfixExpression" -> ListSet(
            n("LeftHandSideExpression"),
            line(n("LeftHandSideExpression"), token("++")),
            line(n("LeftHandSideExpression"), token("--"))
        ),
        "UnaryExpression" -> ListSet(
            n("PostfixExpression"),
            expr(token("delete"), n("UnaryExpression")),
            expr(token("void"), n("UnaryExpression")),
            expr(token("typeof"), n("UnaryExpression")),
            expr(token("++"), n("UnaryExpression")),
            expr(token("--"), n("UnaryExpression")),
            expr(token("+"), n("UnaryExpression")),
            expr(token("-"), n("UnaryExpression")),
            expr(token("~"), n("UnaryExpression")),
            expr(token("!"), n("UnaryExpression"))
        ),
        "MultiplicativeExpression" -> ListSet(
            n("UnaryExpression"),
            expr(n("MultiplicativeExpression"), token("*"), n("UnaryExpression")),
            expr(n("MultiplicativeExpression"), token("/"), n("UnaryExpression")),
            expr(n("MultiplicativeExpression"), token("%"), n("UnaryExpression"))
        ),
        "AdditiveExpression" -> ListSet(
            n("MultiplicativeExpression"),
            expr(n("AdditiveExpression"), token("+"), n("MultiplicativeExpression")),
            expr(n("AdditiveExpression"), token("-"), n("MultiplicativeExpression"))
        ),
        "ShiftExpression" -> ListSet(
            n("AdditiveExpression"),
            expr(n("ShiftExpression"), token("<<"), n("AdditiveExpression")),
            expr(n("ShiftExpression"), token(">>"), n("AdditiveExpression")),
            expr(n("ShiftExpression"), token(">>>"), n("AdditiveExpression"))
        ),
        "RelationalExpression" -> ListSet(
            n("ShiftExpression"),
            expr(n("RelationalExpression"), token("<"), n("ShiftExpression")),
            expr(n("RelationalExpression"), token(">"), n("ShiftExpression")),
            expr(n("RelationalExpression"), token("<="), n("ShiftExpression")),
            expr(n("RelationalExpression"), token(">="), n("ShiftExpression")),
            expr(n("RelationalExpression"), token("instanceof"), n("ShiftExpression"))
        ),
        "RelationalExpressionNoIn" -> ListSet(
            n("ShiftExpression"),
            expr(n("RelationalExpressionNoIn"), token("<"), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), token(">"), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), token("<="), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), token(">="), n("ShiftExpression")),
            expr(n("RelationalExpressionNoIn"), token("instanceof"), n("ShiftExpression"))
        ),
        "EqualityExpression" -> ListSet(
            n("RelationalExpression"),
            expr(n("EqualityExpression"), token("=="), n("RelationalExpression")),
            expr(n("EqualityExpression"), token("!=="), n("RelationalExpression")),
            expr(n("EqualityExpression"), token("==="), n("RelationalExpression")),
            expr(n("EqualityExpression"), token("!=="), n("RelationalExpression"))
        ),
        "EqualityExpressionNoIn" -> ListSet(
            n("RelationalExpressionNoIn"),
            expr(n("EqualityExpressionNoIn"), token("=="), n("RelationalExpressionNoIn")),
            expr(n("EqualityExpressionNoIn"), token("!=="), n("RelationalExpressionNoIn")),
            expr(n("EqualityExpressionNoIn"), token("==="), n("RelationalExpressionNoIn")),
            expr(n("EqualityExpressionNoIn"), token("!=="), n("RelationalExpressionNoIn"))
        ),
        "BitwiseANDExpression" -> ListSet(
            n("EqualityExpression"),
            expr(n("BitwiseANDExpression"), token("&"), n("EqualityExpression"))
        ),
        "BitwiseANDExpressionNoIn" -> ListSet(
            n("EqualityExpressionNoIn"),
            expr(n("BitwiseANDExpressionNoIn"), token("&"), n("EqualityExpressionNoIn"))
        ),
        "BitwiseXORExpression" -> ListSet(
            n("BitwiseANDExpression"),
            expr(n("BitwiseXORExpression"), token("^"), n("BitwiseANDExpression"))
        ),
        "BitwiseXORExpressionNoIn" -> ListSet(
            n("BitwiseANDExpressionNoIn"),
            expr(n("BitwiseXORExpressionNoIn"), token("^"), n("BitwiseANDExpressionNoIn"))
        ),
        "BitwiseORExpression" -> ListSet(
            n("BitwiseXORExpression"),
            expr(n("BitwiseORExpression"), token("^"), n("BitwiseXORExpression"))
        ),
        "BitwiseORExpressionNoIn" -> ListSet(
            n("BitwiseXORExpressionNoIn"),
            expr(n("BitwiseORExpressionNoIn"), token("^"), n("BitwiseXORExpressionNoIn"))
        ),
        "LogicalANDExpression" -> ListSet(
            n("BitwiseORExpression"),
            expr(n("LogicalANDExpression"), token("&&"), n("BitwiseORExpression"))
        ),
        "LogicalANDExpressionNoIn" -> ListSet(
            n("BitwiseORExpressionNoIn"),
            expr(n("LogicalANDExpressionNoIn"), token("&&"), n("BitwiseORExpressionNoIn"))
        ),
        "LogicalORExpression" -> ListSet(
            n("LogicalANDExpression"),
            expr(n("LogicalORExpression"), token("||"), n("LogicalANDExpression"))
        ),
        "LogicalORExpressionNoIn" -> ListSet(
            n("LogicalANDExpressionNoIn"),
            expr(n("LogicalORExpressionNoIn"), token("||"), n("LogicalANDExpressionNoIn"))
        ),
        "ConditionalExpression" -> ListSet(
            n("LogicalORExpression"),
            expr(n("LogicalORExpression"), token("?"), n("AssignmentExpression"), token(":"), n("AssignmentExpression"))
        ),
        "ConditionalExpressionNoIn" -> ListSet(
            n("LogicalORExpressionNoIn"),
            expr(n("LogicalORExpressionNoIn"), token("?"), n("AssignmentExpressionNoIn"), token(":"), n("AssignmentExpressionNoIn"))
        ),
        "AssignmentExpression" -> ListSet(
            n("ConditionalExpression"),
            expr(n("LeftHandSideExpression"), token("="), n("AssignmentExpression")),
            expr(n("LeftHandSideExpression"), n("AssignmentOperator"), n("AssignmentExpression"))
        ),
        "AssignmentExpressionNoIn" -> ListSet(
            n("ConditionalExpressionNoIn"),
            expr(n("LeftHandSideExpression"), token("="), n("AssignmentExpressionNoIn")),
            expr(n("LeftHandSideExpression"), n("AssignmentOperator"), n("AssignmentExpressionNoIn"))
        ),
        "AssignmentOperator" -> ListSet(
            token("*="), token("/="), token("%="), token("+="), token("-="), token("<<="), token(">>="), token(">>>="), token("&="), token("^="), token("|=")
        ),
        "Expression" -> ListSet(
            n("AssignmentExpression"),
            expr(n("Expression"), token(","), n("AssignmentExpression"))
        ),
        "ExpressionNoIn" -> ListSet(
            n("AssignmentExpressionNoIn"),
            expr(n("ExpressionNoIn"), token(","), n("AssignmentExpressionNoIn"))
        ),

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
            n("DebuggerStatement")
        ),
        "Block" -> ListSet(
            expr(token("{"), n("StatementList").opt, token("}"))
        ),
        "StatementList" -> ListSet(
            n("Statement"),
            expr(n("StatementList"), n("Statement"))
        ),
        "VariableStatement" -> ListSet(
            stmt(token("var"), n("VariableDeclarationList"))
        ),
        "VariableDeclarationList" -> ListSet(
            n("VariableDeclaration"),
            expr(n("VariableDeclarationList"), token(","), n("VariableDeclaration"))
        ),
        "VariableDeclarationListNoIn" -> ListSet(
            n("VariableDeclarationNoIn"),
            expr(n("VariableDeclarationListNoIn"), token(","), n("VariableDeclarationNoIn"))
        ),
        "VariableDeclaration" -> ListSet(
            expr(token(n("Identifier")), n("Initialiser").opt)
        ),
        "VariableDeclarationNoIn" -> ListSet(
            expr(token(n("Identifier")), n("InitialiserNoIn").opt)
        ),
        "Initialiser" -> ListSet(
            expr(token("="), n("AssignmentExpression"))
        ),
        "InitialiserNoIn" -> ListSet(
            expr(token("="), n("AssignmentExpressionNoIn"))
        ),
        "EmptyStatement" -> ListSet(
            i(";")
        ),
        "ExpressionStatement" -> ListSet(
            seq(lookahead_except(token("{"), seq(token("function"), n("WhiteSpace"))), stmt(n("Expression")))
        ),
        "IfStatement" -> ListSet(
            expr(token("if"), token("("), n("Expression"), token(")"), n("Statement"), token("else"), n("Statement")),
            expr(token("if"), token("("), n("Expression"), token(")"), n("Statement"))
        ),
        "IterationStatement" -> ListSet(
            stmt(token("do"), n("Statement"), token("while"), token("("), n("Expression"), token(")")),
            expr(token("while"), token("("), n("Expression"), token(")"), n("Statement")),
            expr(token("for"), token("("), n("ExpressionNoIn").opt, token(";"), n("Expression").opt, token(";"), n("Expression").opt, token(")"), n("Statement")),
            expr(token("for"), token("("), token("var"), n("VariableDeclarationListNoIn"), token(";"), n("Expression").opt, token(";"), n("Expression").opt, token(")"), n("Statement")),
            expr(token("for"), token("("), n("LeftHandSideExpression"), token("in"), n("Expression"), token(")"), n("Statement")),
            expr(token("for"), token("("), token("var"), n("VariableDeclarationNoIn"), token("in"), n("Expression"), token(")"), n("Statement"))
        ),
        "ContinueStatement" -> ListSet(
            stmt(token("continue")),
            stmt(line(token("continue"), token(n("Identifier"))))
        ),
        "BreakStatement" -> ListSet(
            stmt(token("break")),
            stmt(line(token("break"), token(n("Identifier"))))
        ),
        "ReturnStatement" -> ListSet(
            stmt(token("return")),
            stmt(line(token("return"), n("Expression")))
        ),
        "WithStatement" -> ListSet(
            expr(token("with"), token("("), n("Expression"), token(")"), n("Statement"))
        ),
        "SwitchStatement" -> ListSet(
            expr(token("switch"), token("("), n("Expression"), token(")"), n("CaseBlock"))
        ),
        "CaseBlock" -> ListSet(
            expr(token("{"), n("CaseClauses").opt, token("}")),
            expr(token("{"), n("CaseClauses").opt, n("DefaultClause"), n("CaseClauses").opt, token("}"))
        ),
        "CaseClauses" -> ListSet(
            n("CaseClause"),
            expr(n("CaseClauses"), n("CaseClause"))
        ),
        "CaseClause" -> ListSet(
            expr(token("case"), n("Expression"), token(":"), n("StatementList").opt)
        ),
        "DefaultClause" -> ListSet(
            expr(token("default"), token(":"), n("StatementList").opt)
        ),
        "LabelledStatement" -> ListSet(
            expr(token(n("Identifier")), token(":"), n("Statement"))
        ),
        "ThrowStatement" -> ListSet(
            stmt(line(token("throw"), n("Expression")))
        ),
        "TryStatement" -> ListSet(
            expr(token("try"), n("Block"), n("Catch")),
            expr(token("try"), n("Block"), n("Finally")),
            expr(token("try"), n("Block"), n("Catch"), n("Finally"))
        ),
        "Catch" -> ListSet(
            expr(token("catch"), token("("), token(n("Identifier")), token(")"), n("Block"))
        ),
        "Finally" -> ListSet(
            expr(token("finally"), n("Block"))
        ),
        "DebuggerStatement" -> ListSet(
            stmt(token("debugger"))
        ),

        // A.5 Functions and Programs
        "FunctionDeclaration" -> ListSet(
            expr(token("function"), token(n("Identifier")), token("("), n("FormalParameterList").opt, token(")"), token("{"), n("FunctionBody"), token("}"))
        ),
        "FunctionExpression" -> ListSet(
            expr(token("function"), token(n("Identifier")).opt, token("("), n("FormalParameterList").opt, token(")"), token("{"), n("FunctionBody"), token("}"))
        ),
        "FormalParameterList" -> ListSet(
            token(n("Identifier")),
            expr(n("FormalParameterList"), token(","), token(n("Identifier")))
        ),
        "FunctionBody" -> ListSet(
            n("SourceElements").opt
        ),
        "Program" -> ListSet(
            n("SourceElements").opt
        ),
        "SourceElements" -> ListSet(
            n("SourceElement"),
            expr(n("SourceElements"), n("SourceElement"))
        ),
        "SourceElement" -> ListSet(
            n("Statement"),
            n("FunctionDeclaration")
        ),

        // A.6 Universal Resource Identifier Character Classes
        "uri" -> ListSet(
            n("uriCharacters").opt
        ),
        "uriCharacters" -> ListSet(
            lex(n("uriCharacter"), n("uriCharacters").opt)
        ),
        "uriCharacter" -> ListSet(
            n("uriReserved"),
            n("uriUnescaped"),
            n("uriEscaped")
        ),
        "uriReserved" -> ListSet(
            chars(";/?:@&=+$,")
        ),
        "uriUnescaped" -> ListSet(
            n("uriAlpha"),
            n("DecimalDigit"),
            n("uriMark")
        ),
        "uriEscaped" -> ListSet(
            lex(i("%"), n("HexDigit"), n("HexDigit"))
        ),
        "uriAlpha" -> ListSet(
            chars('a' to 'z', 'A' to 'Z')
        ),
        "uriMark" -> ListSet(
            chars("-_.!~*'()")
        ),

        // A.7 Regular Expressions
        "Pattern" -> ListSet(
            n("Disjunction")
        ),
        "Disjunction" -> ListSet(
            n("Alternative"),
            lex(n("Alternative"), i("|"), n("Disjunction"))
        ),
        "Alternative" -> ListSet(
            empty,
            lex(n("Alternative"), n("Term"))
        ),
        "Term" -> ListSet(
            n("Assertion"),
            n("Atom"),
            lex(n("Atom"), n("Quantifier"))
        ),
        "Assertion" -> ListSet(
            token("^"),
            token("$"),
            lex(token("\\"), token("b")),
            lex(token("\\"), token("B")),
            lex(token("("), token("?"), token("="), n("Disjunction"), token(")")),
            lex(token("("), token("?"), token("!"), n("Disjunction"), token(")"))
        ),
        "Quantifier" -> ListSet(
            n("QuantifierPrefix"),
            lex(n("QuantifierPrefix"), token("?"))
        ),
        "QuantifierPrefix" -> ListSet(
            token("*"),
            token("+"),
            token("?"),
            lex(token("{"), n("DecimalDigits"), token("}")),
            lex(token("{"), n("DecimalDigits"), token(","), token("}")),
            lex(token("{"), n("DecimalDigits"), token("}"))
        ),
        "Atom" -> ListSet(
            n("PatternCharacter"),
            token("."),
            lex(token("\\"), n("AtomEscape")),
            n("CharacterClass"),
            lex(token("("), n("Disjunction"), token(")")),
            lex(token("("), token("?"), token(":"), n("Disjunction"), token(")"))
        ),
        "PatternCharacter" -> ListSet(
            n("SourceCharacter").butnot(chars("^$\\.*+?()[]{}|"))
        ),
        "AtomEscape" -> ListSet(
            n("DecimalEscape"),
            n("CharacterEscape"),
            n("CharacterClassEscape")
        ),
        "CharacterEscape" -> ListSet(
            n("ControlEscape"),
            lex(token("c"), n("ControlLetter")),
            n("HexEscapeSequence"),
            n("UnicodeEscapeSequence"),
            n("IdentityEscape")
        ),
        "ControlEscape" -> ListSet(
            chars("fnrtv")
        ),
        "ControlLetter" -> ListSet(
            chars('a' to 'z', 'A' to 'Z')
        ),
        "IdentityEscape" -> ListSet(
            n("SourceCharacter").butnot(n("IdentifierPart"), chars("\u200C\u200D"))
        ),
        "DecimalEscape" -> ListSet(
            lex(n("DecimalIntegerLiteral"), lookahead_except(n("DecimalDigit")))
        ),
        "CharacterClassEscape" -> ListSet(
            chars("dDsSwW")
        ),
        "CharacterClass" -> ListSet(
            lex(token("["), lookahead_except(token("^")), n("ClassRanges"), token("]")),
            lex(token("["), token("^"), n("ClassRanges"), token("]"))
        ),
        "ClassRanges" -> ListSet(
            empty,
            n("NonemptyClassRanges")
        ),
        "NonemptyClassRanges" -> ListSet(
            n("ClassAtom"),
            lex(n("ClassAtom"), n("NonemptyClassRangesNoDash")),
            lex(n("ClassAtom"), token("-"), n("ClassAtom"), n("ClassRanges"))
        ),
        "NonemptyClassRangesNoDash" -> ListSet(
            n("ClassAtom"),
            lex(n("ClassAtomNoDash"), n("NonemptyClassRangesNoDash")),
            lex(n("ClassAtomNoDash"), token("-"), n("ClassAtom"), n("ClassRanges"))
        ),
        "ClassAtom" -> ListSet(
            token("-"),
            n("ClassAtomNoDash")
        ),
        "ClassAtomNoDash" -> ListSet(
            n("SourceCharacter").butnot(chars("\\]-")),
            lex(token("\\"), n("ClassEscape"))
        ),
        "ClassEscape" -> ListSet(
            n("DecimalEscape"),
            token("b"),
            n("CharacterEscape"),
            n("CharacterClassEscape")
        ),

        // A.8 JSON
        "JSONWhiteSpace" -> ListSet(
            chars("\t\n\r ")
        ),
        "JSONString" -> ListSet(
            lex(token("\""), n("JSONStringCharacters").opt, token("\""))
        ),
        "JSONStringCharacters" -> ListSet(
            lex(n("JSONStringCharacter"), n("JSONStringCharacters").opt)
        ),
        "JSONStringCharacter" -> ListSet(
            n("SourceCharacter").butnot(chars("\"\\\u0000\u001F")),
            lex(token("\\"), n("JSONEscapeSequence"))
        ),
        "JSONEscapeSequence" -> ListSet(
            n("JSONEscapeCharacter"),
            n("UnicodeEscapeSequence")
        ),
        "JSONEscapeCharacter" -> ListSet(
            chars("\"/\\bfnrt")
        ),
        "JSONNumber" -> ListSet(
            lex(token("-").opt, n("DecimalIntegerLiteral"), n("JSONFraction").opt, n("ExponentPart").opt)
        ),
        "JSONFraction" -> ListSet(
            lex(token("."), n("DecimalDigits"))
        ),
        "JSONNullLiteral" -> ListSet(
            n("NullLiteral")
        ),
        "JSONBooleanLiteral" -> ListSet(
            n("BooleanLiteral")
        ),
        "JSONText" -> ListSet(
            n("JSONValue")
        ),
        "JSONValue" -> ListSet(
            n("JSONNullLiteral"),
            n("JSONBooleanLiteral"),
            n("JSONObject"),
            n("JSONArray"),
            n("JSONString"),
            n("JSONNumber")
        ),
        "JSONObject" -> ListSet(
            expr(token("{"), token("}")),
            expr(token("{"), n("JSONMemberList"), token("}"))
        ),
        "JSONMember" -> ListSet(
            expr(n("JSONString"), token(":"), n("JSONValue"))
        ),
        "JSONMemberList" -> ListSet(
            n("JSONMember"),
            expr(n("JSONMemberList"), token(","), n("JSONMember"))
        ),
        "JSONArray" -> ListSet(
            expr(token("["), token("]")),
            expr(token("["), n("JSONElementList"), token("]"))
        ),
        "JSONElementList" -> ListSet(
            n("JSONValue"),
            expr(n("JSONElementList"), token(","), n("JSONValue"))
        )
    )
}
