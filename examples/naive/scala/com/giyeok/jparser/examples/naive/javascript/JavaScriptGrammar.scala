package com.giyeok.jparser.examples.naive.javascript

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.examples.naive.{GrammarWithExamples, StringExamples}

import scala.collection.immutable.{ListMap, ListSet}

object JavaScript extends Grammar {
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
    "_Token" -> (List(whitespace, n("IdentifierName"), n("Punctuator"), n("NumericLiteral"), n("StringLiteral"))),
    "_Raw" -> List(n("RegularExpressionLiteral")),

    "Start" -> List(n("Program")),

    // A.1 Lexical Grammar
    "SourceCharacter" -> List(anychar),
    "InputElementDiv" -> List(
      n("WhiteSpace"), n("LineTerminator"), n("Comment"), n("Token"), n("DivPunctuator")
    ),
    "InputElementRegExp" -> List(
      n("WhiteSpace"), n("LineTerminator"), n("Comment"), n("Token"), n("RegularExpressionLiteral")
    ),
    "WhiteSpace" -> List(
      chars("\u0009\u000B\u000C\uFEFF"), unicode("Zs")
    ), // \u0020\u00A0  ->  already in Zs
    "LineTerminator" -> List(
      chars("\n\r\u2028\u2029")
    ),
    "LineTerminatorSequence" -> List(
      chars("\n\u2028\u2029"), lex(c('\r'), lookahead_except(c('\n'))), i("\r\n")
    ),
    "Comment" -> List(
      longest(n("MultiLineComment")),
      longest(n("SingleLineComment"))
    ),
    "MultiLineComment" -> List(
      expr(i("/*"), n("MultiLineCommentChars").opt, i("*/"))
    ),
    "MultiLineCommentChars" -> List(
      lex(n("MultiLineNotAsteriskChar"), n("MultiLineCommentChars").opt),
      lex(i("*"), n("PostAsteriskCommentChars").opt)
    ),
    "PostAsteriskCommentChars" -> List(
      lex(n("MultiLineNotForwardSlashOrAsteriskChar"), n("MultiLineCommentChars").opt),
      lex(i("*"), n("PostAsteriskCommentChars").opt)
    ),
    "MultiLineNotAsteriskChar" -> List(
      lex(n("SourceCharacter").butnot(i("*")))
    ),
    "MultiLineNotForwardSlashOrAsteriskChar" -> List(
      lex(n("SourceCharacter").butnot(i("/"), i("*")))
    ),
    "SingleLineComment" -> List(
      lex(i("//"), n("SingleLineCommentChars").opt)
    ),
    "SingleLineCommentChars" -> List(
      lex(n("SingleLineCommentChar"), n("SingleLineCommentChars").opt)
    ),
    "SingleLineCommentChar" -> List(
      n("SourceCharacter").butnot(n("LineTerminator"))
    ),
    "Token" -> List(
      longest(n("IdentifierName")),
      n("Punctuator"),
      longest(n("NumericLiteral")),
      longest(n("StringLiteral"))
    ),
    "Identifier" -> List(
      n("IdentifierName").butnot(n("ReservedWord"))
    ),
    "IdentifierName" -> List(
      lex(n("IdentifierStart")),
      lex(n("IdentifierName"), n("IdentifierPart"))
    ),
    "IdentifierStart" -> List(
      n("UnicodeLetter"),
      i("$"),
      i("_"),
      lex(i("\""), n("UnicodeEscapeSequence"))
    ),
    "IdentifierPart" -> List(
      n("IdentifierStart"),
      n("UnicodeCombiningMark"),
      n("UnicodeDigit"),
      n("UnicodeConnectorPunctuation"),
      chars("\u200C\u200D")
    ),
    "UnicodeLetter" -> List(
      unicode("Lu", "Ll", "Lt", "Lm", "Lo", "Nl")
    ),
    "UnicodeCombiningMark" -> List(
      unicode("Mn", "Mc")
    ),
    "UnicodeDigit" -> List(
      unicode("Nd")
    ),
    "UnicodeConnectorPunctuation" -> List(
      unicode("Pc")
    ),
    "ReservedWord" -> List(
      n("Keyword"),
      n("FutureReservedWord"),
      n("NullLiteral"),
      n("BooleanLiteral")
    ),
    "Keyword" -> List(
      i("break"), i("do"), i("instanceof"), i("typeof"),
      i("case"), i("else"), i("new"), i("var"),
      i("catch"), i("finally"), i("return"), i("void"),
      i("continue"), i("for"), i("switch"), i("while"),
      i("debugger"), i("function"), i("this"), i("with"),
      i("default"), i("if"), i("throw"),
      i("delete"), i("in"), i("try")
    ),
    "FutureReservedWord" -> List(
      i("class"), i("enum"), i("extends"), i("super"),
      i("const"), i("export"), i("import"),
      i("implements"), i("let"), i("private"), i("public"),
      i("interface"), i("package"), i("protected"), i("static"),
      i("yield")
    ),
    "Punctuator" -> List(
      i("{"), i("}"), i("("), i(")"), i("["), i("]"),
      i("."), i(";"), i(","), i("<"), i(">"), i("<="),
      i(">="), i("=="), i("!="), i("==="), i("!=="),
      i("+"), i("-"), i("*"), i("%"), i("++"), i("--"),
      i("<<"), i(">>"), i(">>>"), i("&"), i("|"), i("^"),
      i("!"), i("~"), i("&&"), i("||"), i("?"), i(":"),
      i("="), i("+="), i("-="), i("*="), i("%="), i("<<="),
      i(">>="), i(">>>="), i("&="), i("|="), i("^=")
    ),
    "DivPunctuator" -> List(
      i("/"), i("/=")
    ),
    "Literal" -> List(
      n("NullLiteral"),
      n("BooleanLiteral"),
      n("NumericLiteral"),
      n("StringLiteral"),
      n("RegularExpressionLiteral")
    ),
    "NullLiteral" -> List(
      i("null")
    ),
    "BooleanLiteral" -> List(
      i("true"),
      i("false")
    ),
    "NumericLiteral" -> List(
      n("DecimalLiteral"),
      n("HexIntegerLiteral")
    ),
    "DecimalLiteral" -> List(
      lex(n("DecimalIntegerLiteral"), i("."), n("DecimalDigits").opt, n("ExponentPart").opt),
      lex(i("."), n("DecimalDigits"), n("ExponentPart").opt),
      lex(n("DecimalIntegerLiteral"), n("ExponentPart").opt)
    ),
    "DecimalIntegerLiteral" -> List(
      i("0"),
      lex(n("NonZeroDigit"), n("DecimalDigits").opt)
    ),
    "DecimalDigits" -> List(
      n("DecimalDigit"),
      lex(n("DecimalDigits"), n("DecimalDigit"))
    ),
    "DecimalDigit" -> List(
      chars('0' to '9')
    ),
    "NonZeroDigit" -> List(
      chars('1' to '9')
    ),
    "ExponentPart" -> List(
      lex(n("ExponentIndicator"), n("SignedInteger"))
    ),
    "ExponentIndicator" -> List(
      chars("eE")
    ),
    "SignedInteger" -> List(
      n("DecimalDigits"),
      lex(i("+"), n("DecimalDigits")),
      lex(i("-"), n("DecimalDigits"))
    ),
    "HexIntegerLiteral" -> List(
      lex(i("0x"), n("HexDigit")),
      lex(i("0X"), n("HexDigit")),
      lex(n("HexIntegerLiteral"), n("HexDigit"))
    ),
    "HexDigit" -> List(
      chars('0' to '9', 'a' to 'f', 'A' to 'F')
    ),
    "StringLiteral" -> List(
      lex(i("\""), n("DoubleStringCharacters").opt, i("\"")),
      lex(i("'"), n("SingleStringCharacters").opt, i("'"))
    ),
    "DoubleStringCharacters" -> List(
      lex(n("DoubleStringCharacter"), n("DoubleStringCharacters").opt)
    ),
    "SingleStringCharacters" -> List(
      lex(n("SingleStringCharacter"), n("SingleStringCharacters").opt)
    ),
    "DoubleStringCharacter" -> List(
      n("SourceCharacter").butnot(chars("\"\\"), n("LineTerminator")),
      lex(i("\\"), n("EscapeSequence")),
      n("LineContinuation")
    ),
    "SingleStringCharacter" -> List(
      n("SourceCharacter").butnot(chars("'\\"), n("LineTerminator")),
      lex(i("\\"), n("EscapeSequence")),
      n("LineContinuation")
    ),
    "LineContinuation" -> List(
      lex(i("\\"), n("LineTerminatorSequence"))
    ),
    "EscapeSequence" -> List(
      n("CharacterEscapeSequence"),
      lex(i("0"), lookahead_except(n("DecimalDigit"))),
      n("HexEscapeSequence"),
      n("UnicodeEscapeSequence")
    ),
    "CharacterEscapeSequence" -> List(
      n("SingleEscapeCharacter"),
      n("NonEscapeCharacter")
    ),
    "SingleEscapeCharacter" -> List(
      chars("'\"\\bfnrtv")
    ),
    "NonEscapeCharacter" -> List(
      n("SourceCharacter").butnot(n("EscapeCharacter"), n("LineTerminator"))
    ),
    "EscapeCharacter" -> List(
      n("SingleEscapeCharacter"),
      n("DecimalDigit"),
      chars("xu")
    ),
    "HexEscapeSequence" -> List(
      lex(i("x"), n("HexDigit"), n("HexDigit"))
    ),
    "UnicodeEscapeSequence" -> List(
      lex(i("u"), n("HexDigit"), n("HexDigit"), n("HexDigit"), n("HexDigit"))
    ),
    "RegularExpressionLiteral" -> List(
      lex(i("/"), n("RegularExpressionBody"), i("/"), n("RegularExpressionFlags"))
    ),
    "RegularExpressionBody" -> List(
      lex(n("RegularExpressionFirstChar"), n("RegularExpressionChars"))
    ),
    "RegularExpressionChars" -> List(
      empty,
      lex(n("RegularExpressionChars"), n("RegularExpressionChar"))
    ),
    "RegularExpressionFirstChar" -> List(
      n("RegularExpressionNonTerminator").butnot(chars("*\\/[")),
      n("RegularExpressionBackslashSequence"),
      n("RegularExpressionClass")
    ),
    "RegularExpressionChar" -> List(
      n("RegularExpressionNonTerminator").butnot(chars("\\/[")),
      n("RegularExpressionBackslashSequence"),
      n("RegularExpressionClass")
    ),
    "RegularExpressionBackslashSequence" -> List(
      lex(i("\\"), n("RegularExpressionNonTerminator"))
    ),
    "RegularExpressionNonTerminator" -> List(
      n("SourceCharacter").butnot(n("LineTerminator"))
    ),
    "RegularExpressionClass" -> List(
      lex(i("["), n("RegularExpressionClassChars"), i("]"))
    ),
    "RegularExpressionClassChars" -> List(
      empty,
      lex(n("RegularExpressionClassChars"), n("RegularExpressionClassChar"))
    ),
    "RegularExpressionClassChar" -> List(
      n("RegularExpressionNonTerminator").butnot(chars("]\\")),
      n("RegularExpressionBackslashSequence")
    ),
    "RegularExpressionFlags" -> List(
      empty,
      lex(n("RegularExpressionFlags"), n("IdentifierPart"))
    ),

    // A.2 Number Conversions
    "StringNumericLiteral" -> List(
      n("StrWhiteSpace").opt,
      lex(n("StrWhiteSpace").opt, n("StrNumericLiteral"), n("StrWhiteSpace").opt)
    ),
    "StrWhiteSpace" -> List(
      lex(n("StrWhiteSpaceChar"), n("StrWhiteSpace").opt)
    ),
    "StrWhiteSpaceChar" -> List(
      n("WhiteSpace"),
      n("LineTerminator")
    ),
    "StrNumericLiteral" -> List(
      n("StrDecimalLiteral"),
      n("HexIntegerLiteral")
    ),
    "StrDecimalLiteral" -> List(
      n("StrUnsignedDecimalLiteral"),
      lex(i("+"), n("StrUnsignedDecimalLiteral")),
      lex(i("-"), n("StrUnsignedDecimalLiteral"))
    ),
    "StrUnsignedDecimalLiteral" -> List(
      token("Infinity"),
      lex(n("DecimalDigits"), i("."), n("DecimalDigits").opt, n("ExponentPart").opt),
      lex(i("."), n("DecimalDigits"), n("ExponentPart").opt),
      lex(n("DecimalDigits"), n("ExponentPart").opt)
    ),
    "DecimalDigits" -> List(
      n("DecimalDigit"),
      lex(n("DecimalDigits"), n("DecimalDigit"))
    ),
    "DecimalDigit" -> List(
      chars('0' to '9')
    ),
    "ExponentPart" -> List(
      lex(n("ExponentIndicator"), n("SignedInteger"))
    ),
    "ExponentIndicator" -> List(
      chars("eE")
    ),
    "SignedInteger" -> List(
      n("DecimalDigits"),
      lex(i("+"), n("DecimalDigits")),
      lex(i("-"), n("DecimalDigits"))
    ),
    "HexIntegerLiteral" -> List(
      lex(i("0x"), n("HexDigit")),
      lex(i("0X"), n("HexDigit")),
      lex(n("HexIntegerLiteral"), n("HexDigit"))
    ),
    "HexDigit" -> List(
      chars('0' to '9', 'a' to 'f', 'A' to 'F')
    ),

    // A.3 Expressions
    "PrimaryExpression" -> List(
      token("this"),
      token(n("Identifier")),
      token(n("Literal")),
      n("ArrayLiteral"),
      n("ObjectLiteral"),
      expr(token("("), n("Expression"), token(")"))
    ),
    "ArrayLiteral" -> List(
      expr(token("["), n("Elision").opt, token("]")),
      expr(token("["), n("ElementList"), token("]")),
      expr(token("["), n("ElementList"), token(","), n("Elision").opt, token("]"))
    ),
    "ElementList" -> List(
      expr(n("Elision").opt, n("AssignmentExpression")),
      expr(n("ElementList"), token(","), n("Elision").opt, n("AssignmentExpression"))
    ),
    "Elision" -> List(
      token(","),
      expr(n("Elision"), token(","))
    ),
    "ObjectLiteral" -> List(
      expr(token("{"), token("}")),
      expr(token("{"), n("PropertyNameAndValueList"), token("}")),
      expr(token("{"), n("PropertyNameAndValueList"), token(","), token("}"))
    ),
    "PropertyNameAndValueList" -> List(
      n("PropertyAssignment"),
      expr(n("PropertyNameAndValueList"), token(","), n("PropertyAssignment"))
    ),
    "PropertyAssignment" -> List(
      expr(n("PropertyName"), token(":"), n("AssignmentExpression")),
      expr(token("get"), n("PropertyName"), token("("), token(")"), token("{"), n("FunctionBody"), token("}")),
      expr(token("set"), n("PropertyName"), token("("), n("PropertySetParameterList"), token(")"), token("{"), n("FunctionBody"), token("}"))
    ),
    "PropertyName" -> List(
      token(n("IdentifierName")),
      token(n("StringLiteral")),
      token(n("NumericLiteral"))
    ),
    "PropertySetParameterList" -> List(
      token(n("Identifier"))
    ),
    "MemberExpression" -> List(
      n("PrimaryExpression"),
      n("FunctionExpression"),
      expr(n("MemberExpression"), token("["), n("Expression"), token("]")),
      expr(n("MemberExpression"), token("."), token(n("IdentifierName"))),
      expr(token("new"), n("MemberExpression"), n("Arguments"))
    ),
    "NewExpression" -> List(
      n("MemberExpression"),
      expr(token("new"), n("NewExpression"))
    ),
    "CallExpression" -> List(
      expr(n("MemberExpression"), n("Arguments")),
      expr(n("CallExpression"), n("Arguments")),
      expr(n("CallExpression"), token("["), n("Expression"), token("]")),
      expr(n("CallExpression"), token("."), token(n("IdentifierName")))
    ),
    "Arguments" -> List(
      expr(token("("), token(")")),
      expr(token("("), n("ArgumentList"), token(")"))
    ),
    "ArgumentList" -> List(
      n("AssignmentExpression"),
      expr(n("ArgumentList"), token(","), n("AssignmentExpression"))
    ),
    "LeftHandSideExpression" -> List(
      n("NewExpression"),
      n("CallExpression")
    ),
    "PostfixExpression" -> List(
      n("LeftHandSideExpression"),
      line(n("LeftHandSideExpression"), token("++")),
      line(n("LeftHandSideExpression"), token("--"))
    ),
    "UnaryExpression" -> List(
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
    "MultiplicativeExpression" -> List(
      n("UnaryExpression"),
      expr(n("MultiplicativeExpression"), token("*"), n("UnaryExpression")),
      expr(n("MultiplicativeExpression"), token("/"), n("UnaryExpression")),
      expr(n("MultiplicativeExpression"), token("%"), n("UnaryExpression"))
    ),
    "AdditiveExpression" -> List(
      n("MultiplicativeExpression"),
      expr(n("AdditiveExpression"), token("+"), n("MultiplicativeExpression")),
      expr(n("AdditiveExpression"), token("-"), n("MultiplicativeExpression"))
    ),
    "ShiftExpression" -> List(
      n("AdditiveExpression"),
      expr(n("ShiftExpression"), token("<<"), n("AdditiveExpression")),
      expr(n("ShiftExpression"), token(">>"), n("AdditiveExpression")),
      expr(n("ShiftExpression"), token(">>>"), n("AdditiveExpression"))
    ),
    "RelationalExpression" -> List(
      n("ShiftExpression"),
      expr(n("RelationalExpression"), token("<"), n("ShiftExpression")),
      expr(n("RelationalExpression"), token(">"), n("ShiftExpression")),
      expr(n("RelationalExpression"), token("<="), n("ShiftExpression")),
      expr(n("RelationalExpression"), token(">="), n("ShiftExpression")),
      expr(n("RelationalExpression"), token("instanceof"), n("ShiftExpression"))
    ),
    "RelationalExpressionNoIn" -> List(
      n("ShiftExpression"),
      expr(n("RelationalExpressionNoIn"), token("<"), n("ShiftExpression")),
      expr(n("RelationalExpressionNoIn"), token(">"), n("ShiftExpression")),
      expr(n("RelationalExpressionNoIn"), token("<="), n("ShiftExpression")),
      expr(n("RelationalExpressionNoIn"), token(">="), n("ShiftExpression")),
      expr(n("RelationalExpressionNoIn"), token("instanceof"), n("ShiftExpression"))
    ),
    "EqualityExpression" -> List(
      n("RelationalExpression"),
      expr(n("EqualityExpression"), token("=="), n("RelationalExpression")),
      expr(n("EqualityExpression"), token("!=="), n("RelationalExpression")),
      expr(n("EqualityExpression"), token("==="), n("RelationalExpression")),
      expr(n("EqualityExpression"), token("!=="), n("RelationalExpression"))
    ),
    "EqualityExpressionNoIn" -> List(
      n("RelationalExpressionNoIn"),
      expr(n("EqualityExpressionNoIn"), token("=="), n("RelationalExpressionNoIn")),
      expr(n("EqualityExpressionNoIn"), token("!=="), n("RelationalExpressionNoIn")),
      expr(n("EqualityExpressionNoIn"), token("==="), n("RelationalExpressionNoIn")),
      expr(n("EqualityExpressionNoIn"), token("!=="), n("RelationalExpressionNoIn"))
    ),
    "BitwiseANDExpression" -> List(
      n("EqualityExpression"),
      expr(n("BitwiseANDExpression"), token("&"), n("EqualityExpression"))
    ),
    "BitwiseANDExpressionNoIn" -> List(
      n("EqualityExpressionNoIn"),
      expr(n("BitwiseANDExpressionNoIn"), token("&"), n("EqualityExpressionNoIn"))
    ),
    "BitwiseXORExpression" -> List(
      n("BitwiseANDExpression"),
      expr(n("BitwiseXORExpression"), token("^"), n("BitwiseANDExpression"))
    ),
    "BitwiseXORExpressionNoIn" -> List(
      n("BitwiseANDExpressionNoIn"),
      expr(n("BitwiseXORExpressionNoIn"), token("^"), n("BitwiseANDExpressionNoIn"))
    ),
    "BitwiseORExpression" -> List(
      n("BitwiseXORExpression"),
      expr(n("BitwiseORExpression"), token("^"), n("BitwiseXORExpression"))
    ),
    "BitwiseORExpressionNoIn" -> List(
      n("BitwiseXORExpressionNoIn"),
      expr(n("BitwiseORExpressionNoIn"), token("^"), n("BitwiseXORExpressionNoIn"))
    ),
    "LogicalANDExpression" -> List(
      n("BitwiseORExpression"),
      expr(n("LogicalANDExpression"), token("&&"), n("BitwiseORExpression"))
    ),
    "LogicalANDExpressionNoIn" -> List(
      n("BitwiseORExpressionNoIn"),
      expr(n("LogicalANDExpressionNoIn"), token("&&"), n("BitwiseORExpressionNoIn"))
    ),
    "LogicalORExpression" -> List(
      n("LogicalANDExpression"),
      expr(n("LogicalORExpression"), token("||"), n("LogicalANDExpression"))
    ),
    "LogicalORExpressionNoIn" -> List(
      n("LogicalANDExpressionNoIn"),
      expr(n("LogicalORExpressionNoIn"), token("||"), n("LogicalANDExpressionNoIn"))
    ),
    "ConditionalExpression" -> List(
      n("LogicalORExpression"),
      expr(n("LogicalORExpression"), token("?"), n("AssignmentExpression"), token(":"), n("AssignmentExpression"))
    ),
    "ConditionalExpressionNoIn" -> List(
      n("LogicalORExpressionNoIn"),
      expr(n("LogicalORExpressionNoIn"), token("?"), n("AssignmentExpressionNoIn"), token(":"), n("AssignmentExpressionNoIn"))
    ),
    "AssignmentExpression" -> List(
      n("ConditionalExpression"),
      expr(n("LeftHandSideExpression"), token("="), n("AssignmentExpression")),
      expr(n("LeftHandSideExpression"), n("AssignmentOperator"), n("AssignmentExpression"))
    ),
    "AssignmentExpressionNoIn" -> List(
      n("ConditionalExpressionNoIn"),
      expr(n("LeftHandSideExpression"), token("="), n("AssignmentExpressionNoIn")),
      expr(n("LeftHandSideExpression"), n("AssignmentOperator"), n("AssignmentExpressionNoIn"))
    ),
    "AssignmentOperator" -> List(
      token("*="), token("/="), token("%="), token("+="), token("-="), token("<<="), token(">>="), token(">>>="), token("&="), token("^="), token("|=")
    ),
    "Expression" -> List(
      n("AssignmentExpression"),
      expr(n("Expression"), token(","), n("AssignmentExpression"))
    ),
    "ExpressionNoIn" -> List(
      n("AssignmentExpressionNoIn"),
      expr(n("ExpressionNoIn"), token(","), n("AssignmentExpressionNoIn"))
    ),

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
      n("DebuggerStatement")
    ),
    "Block" -> List(
      expr(token("{"), n("StatementList").opt, token("}"))
    ),
    "StatementList" -> List(
      n("Statement"),
      expr(n("StatementList"), n("Statement"))
    ),
    "VariableStatement" -> List(
      stmt(token("var"), n("VariableDeclarationList"))
    ),
    "VariableDeclarationList" -> List(
      n("VariableDeclaration"),
      expr(n("VariableDeclarationList"), token(","), n("VariableDeclaration"))
    ),
    "VariableDeclarationListNoIn" -> List(
      n("VariableDeclarationNoIn"),
      expr(n("VariableDeclarationListNoIn"), token(","), n("VariableDeclarationNoIn"))
    ),
    "VariableDeclaration" -> List(
      expr(token(n("Identifier")), n("Initialiser").opt)
    ),
    "VariableDeclarationNoIn" -> List(
      expr(token(n("Identifier")), n("InitialiserNoIn").opt)
    ),
    "Initialiser" -> List(
      expr(token("="), n("AssignmentExpression"))
    ),
    "InitialiserNoIn" -> List(
      expr(token("="), n("AssignmentExpressionNoIn"))
    ),
    "EmptyStatement" -> List(
      i(";")
    ),
    "ExpressionStatement" -> List(
      seq(lookahead_except(token("{"), seq(token("function"), n("WhiteSpace"))), stmt(n("Expression")))
    ),
    "IfStatement" -> List(
      expr(token("if"), token("("), n("Expression"), token(")"), n("Statement"), token("else"), n("Statement")),
      expr(token("if"), token("("), n("Expression"), token(")"), n("Statement"))
    ),
    "IterationStatement" -> List(
      stmt(token("do"), n("Statement"), token("while"), token("("), n("Expression"), token(")")),
      expr(token("while"), token("("), n("Expression"), token(")"), n("Statement")),
      expr(token("for"), token("("), n("ExpressionNoIn").opt, token(";"), n("Expression").opt, token(";"), n("Expression").opt, token(")"), n("Statement")),
      expr(token("for"), token("("), token("var"), n("VariableDeclarationListNoIn"), token(";"), n("Expression").opt, token(";"), n("Expression").opt, token(")"), n("Statement")),
      expr(token("for"), token("("), n("LeftHandSideExpression"), token("in"), n("Expression"), token(")"), n("Statement")),
      expr(token("for"), token("("), token("var"), n("VariableDeclarationNoIn"), token("in"), n("Expression"), token(")"), n("Statement"))
    ),
    "ContinueStatement" -> List(
      stmt(token("continue")),
      stmt(line(token("continue"), token(n("Identifier"))))
    ),
    "BreakStatement" -> List(
      stmt(token("break")),
      stmt(line(token("break"), token(n("Identifier"))))
    ),
    "ReturnStatement" -> List(
      stmt(token("return")),
      stmt(line(token("return"), n("Expression")))
    ),
    "WithStatement" -> List(
      expr(token("with"), token("("), n("Expression"), token(")"), n("Statement"))
    ),
    "SwitchStatement" -> List(
      expr(token("switch"), token("("), n("Expression"), token(")"), n("CaseBlock"))
    ),
    "CaseBlock" -> List(
      expr(token("{"), n("CaseClauses").opt, token("}")),
      expr(token("{"), n("CaseClauses").opt, n("DefaultClause"), n("CaseClauses").opt, token("}"))
    ),
    "CaseClauses" -> List(
      n("CaseClause"),
      expr(n("CaseClauses"), n("CaseClause"))
    ),
    "CaseClause" -> List(
      expr(token("case"), n("Expression"), token(":"), n("StatementList").opt)
    ),
    "DefaultClause" -> List(
      expr(token("default"), token(":"), n("StatementList").opt)
    ),
    "LabelledStatement" -> List(
      expr(token(n("Identifier")), token(":"), n("Statement"))
    ),
    "ThrowStatement" -> List(
      stmt(line(token("throw"), n("Expression")))
    ),
    "TryStatement" -> List(
      expr(token("try"), n("Block"), n("Catch")),
      expr(token("try"), n("Block"), n("Finally")),
      expr(token("try"), n("Block"), n("Catch"), n("Finally"))
    ),
    "Catch" -> List(
      expr(token("catch"), token("("), token(n("Identifier")), token(")"), n("Block"))
    ),
    "Finally" -> List(
      expr(token("finally"), n("Block"))
    ),
    "DebuggerStatement" -> List(
      stmt(token("debugger"))
    ),

    // A.5 Functions and Programs
    "FunctionDeclaration" -> List(
      expr(token("function"), token(n("Identifier")), token("("), n("FormalParameterList").opt, token(")"), token("{"), n("FunctionBody"), token("}"))
    ),
    "FunctionExpression" -> List(
      expr(token("function"), token(n("Identifier")).opt, token("("), n("FormalParameterList").opt, token(")"), token("{"), n("FunctionBody"), token("}"))
    ),
    "FormalParameterList" -> List(
      token(n("Identifier")),
      expr(n("FormalParameterList"), token(","), token(n("Identifier")))
    ),
    "FunctionBody" -> List(
      n("SourceElements").opt
    ),
    "Program" -> List(
      n("SourceElements").opt
    ),
    "SourceElements" -> List(
      n("SourceElement"),
      expr(n("SourceElements"), n("SourceElement"))
    ),
    "SourceElement" -> List(
      n("Statement"),
      n("FunctionDeclaration")
    ),

    // A.6 Universal Resource Identifier Character Classes
    "uri" -> List(
      n("uriCharacters").opt
    ),
    "uriCharacters" -> List(
      lex(n("uriCharacter"), n("uriCharacters").opt)
    ),
    "uriCharacter" -> List(
      n("uriReserved"),
      n("uriUnescaped"),
      n("uriEscaped")
    ),
    "uriReserved" -> List(
      chars(";/?:@&=+$,")
    ),
    "uriUnescaped" -> List(
      n("uriAlpha"),
      n("DecimalDigit"),
      n("uriMark")
    ),
    "uriEscaped" -> List(
      lex(i("%"), n("HexDigit"), n("HexDigit"))
    ),
    "uriAlpha" -> List(
      chars('a' to 'z', 'A' to 'Z')
    ),
    "uriMark" -> List(
      chars("-_.!~*'()")
    ),

    // A.7 Regular Expressions
    "Pattern" -> List(
      n("Disjunction")
    ),
    "Disjunction" -> List(
      n("Alternative"),
      lex(n("Alternative"), i("|"), n("Disjunction"))
    ),
    "Alternative" -> List(
      empty,
      lex(n("Alternative"), n("Term"))
    ),
    "Term" -> List(
      n("Assertion"),
      n("Atom"),
      lex(n("Atom"), n("Quantifier"))
    ),
    "Assertion" -> List(
      token("^"),
      token("$"),
      lex(token("\\"), token("b")),
      lex(token("\\"), token("B")),
      lex(token("("), token("?"), token("="), n("Disjunction"), token(")")),
      lex(token("("), token("?"), token("!"), n("Disjunction"), token(")"))
    ),
    "Quantifier" -> List(
      n("QuantifierPrefix"),
      lex(n("QuantifierPrefix"), token("?"))
    ),
    "QuantifierPrefix" -> List(
      token("*"),
      token("+"),
      token("?"),
      lex(token("{"), n("DecimalDigits"), token("}")),
      lex(token("{"), n("DecimalDigits"), token(","), token("}")),
      lex(token("{"), n("DecimalDigits"), token("}"))
    ),
    "Atom" -> List(
      n("PatternCharacter"),
      token("."),
      lex(token("\\"), n("AtomEscape")),
      n("CharacterClass"),
      lex(token("("), n("Disjunction"), token(")")),
      lex(token("("), token("?"), token(":"), n("Disjunction"), token(")"))
    ),
    "PatternCharacter" -> List(
      n("SourceCharacter").butnot(chars("^$\\.*+?()[]{}|"))
    ),
    "AtomEscape" -> List(
      n("DecimalEscape"),
      n("CharacterEscape"),
      n("CharacterClassEscape")
    ),
    "CharacterEscape" -> List(
      n("ControlEscape"),
      lex(token("c"), n("ControlLetter")),
      n("HexEscapeSequence"),
      n("UnicodeEscapeSequence"),
      n("IdentityEscape")
    ),
    "ControlEscape" -> List(
      chars("fnrtv")
    ),
    "ControlLetter" -> List(
      chars('a' to 'z', 'A' to 'Z')
    ),
    "IdentityEscape" -> List(
      n("SourceCharacter").butnot(n("IdentifierPart"), chars("\u200C\u200D"))
    ),
    "DecimalEscape" -> List(
      lex(n("DecimalIntegerLiteral"), lookahead_except(n("DecimalDigit")))
    ),
    "CharacterClassEscape" -> List(
      chars("dDsSwW")
    ),
    "CharacterClass" -> List(
      lex(token("["), lookahead_except(token("^")), n("ClassRanges"), token("]")),
      lex(token("["), token("^"), n("ClassRanges"), token("]"))
    ),
    "ClassRanges" -> List(
      empty,
      n("NonemptyClassRanges")
    ),
    "NonemptyClassRanges" -> List(
      n("ClassAtom"),
      lex(n("ClassAtom"), n("NonemptyClassRangesNoDash")),
      lex(n("ClassAtom"), token("-"), n("ClassAtom"), n("ClassRanges"))
    ),
    "NonemptyClassRangesNoDash" -> List(
      n("ClassAtom"),
      lex(n("ClassAtomNoDash"), n("NonemptyClassRangesNoDash")),
      lex(n("ClassAtomNoDash"), token("-"), n("ClassAtom"), n("ClassRanges"))
    ),
    "ClassAtom" -> List(
      token("-"),
      n("ClassAtomNoDash")
    ),
    "ClassAtomNoDash" -> List(
      n("SourceCharacter").butnot(chars("\\]-")),
      lex(token("\\"), n("ClassEscape"))
    ),
    "ClassEscape" -> List(
      n("DecimalEscape"),
      token("b"),
      n("CharacterEscape"),
      n("CharacterClassEscape")
    ),

    // A.8 JSON
    "JSONWhiteSpace" -> List(
      chars("\t\n\r ")
    ),
    "JSONString" -> List(
      lex(token("\""), n("JSONStringCharacters").opt, token("\""))
    ),
    "JSONStringCharacters" -> List(
      lex(n("JSONStringCharacter"), n("JSONStringCharacters").opt)
    ),
    "JSONStringCharacter" -> List(
      n("SourceCharacter").butnot(chars("\"\\\u0000\u001F")),
      lex(token("\\"), n("JSONEscapeSequence"))
    ),
    "JSONEscapeSequence" -> List(
      n("JSONEscapeCharacter"),
      n("UnicodeEscapeSequence")
    ),
    "JSONEscapeCharacter" -> List(
      chars("\"/\\bfnrt")
    ),
    "JSONNumber" -> List(
      lex(token("-").opt, n("DecimalIntegerLiteral"), n("JSONFraction").opt, n("ExponentPart").opt)
    ),
    "JSONFraction" -> List(
      lex(token("."), n("DecimalDigits"))
    ),
    "JSONNullLiteral" -> List(
      n("NullLiteral")
    ),
    "JSONBooleanLiteral" -> List(
      n("BooleanLiteral")
    ),
    "JSONText" -> List(
      n("JSONValue")
    ),
    "JSONValue" -> List(
      n("JSONNullLiteral"),
      n("JSONBooleanLiteral"),
      n("JSONObject"),
      n("JSONArray"),
      n("JSONString"),
      n("JSONNumber")
    ),
    "JSONObject" -> List(
      expr(token("{"), token("}")),
      expr(token("{"), n("JSONMemberList"), token("}"))
    ),
    "JSONMember" -> List(
      expr(n("JSONString"), token(":"), n("JSONValue"))
    ),
    "JSONMemberList" -> List(
      n("JSONMember"),
      expr(n("JSONMemberList"), token(","), n("JSONMember"))
    ),
    "JSONArray" -> List(
      expr(token("["), token("]")),
      expr(token("["), n("JSONElementList"), token("]"))
    ),
    "JSONElementList" -> List(
      n("JSONValue"),
      expr(n("JSONElementList"), token(","), n("JSONValue"))
    )
  )
}

object JavaScriptGrammarExamples1 extends GrammarWithExamples with StringExamples {
  val grammar: Grammar = JavaScript

  val correctExamples: Set[String] = List(
    "",
    "var x = 1;",
    "varx = 1;",
    "iff=1;",
    "a=b\nc=d;",
    "abc=  function(a){return a+1;}(1);",
    "console.log(function(a){return a+1;}(1));",
    "function x(a) { return a + 1; }",
    "{return a}",
    "var vara = function ifx(a){return(function(y){return (y+1);})(a)};").toSet
  val incorrectExamples: Set[String] = List().toSet
}
