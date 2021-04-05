package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.examples.metalang3.MetaLang3Grammar
import com.giyeok.jparser.metalang3a.MetaLanguage3.{ProcessedGrammar, writeScalaParserCode}
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGen
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGen.InlineProtoDef
import com.giyeok.jparser.proto.GrammarProtobufConverter
import com.giyeok.jparser.utils.FileUtil.readFile

import java.io.{BufferedOutputStream, File, FileOutputStream}

object Grammars {
  private def generateScalaParserCode(name: String, grammarDef: String, examples: List[String] = null,
                                      options: ScalaCodeGen.Options = ScalaCodeGen.Options()): ProcessedGrammar = {
    println(s"Generating $name...")
    val analysis = writeScalaParserCode(grammarDef, name,
      "com.giyeok.jparser.metalang3a.generated",
      new File("./metalang/src/main/scala"),
      Option(examples), options = options)
    println(s"$name generated!")
    analysis
  }

  def generateMetaLang3Ast(): Unit =
    generateScalaParserCode("MetaLang3Ast", MetaLang3Grammar.inMetaLang3.grammar,
      examples = List("A = B C 'd' 'e'*"),
      options = ScalaCodeGen.Options(grammarDefType = InlineProtoDef))

  def generateMlProtoAst(): Unit = generateScalaParserCode("MlProto",
    readFile("./examples/src/main/resources/mlproto/mlproto.cdg"),
    examples = List("module Conv2d<>(inChannels: Int) {}"))

  def generateAutoDbAst(): Unit = generateScalaParserCode("AutoDbAst",
    readFile("./examples/src/main/resources/autodb/autodb.cdg"),
    examples = List(
      """database DartFriends {
        |  entity User(
        |    userId: Long as UserId,
        |    firebaseUid: String?,
        |    userDetail: Ref(UserDetail),
        |  ) key(userId), unique(loginName)
        |
        |  entity UserDetail(
        |    user: Ref(User),
        |    version: Long as UserDetailVersion,
        |    profileImage: String?,
        |    homeShop: Ref(Shop)?
        |  ) key(user)
        |
        |  entity Following(
        |    follower: Ref(User),
        |    following: Ref(User),
        |  ) key(follower, following)
        |}""".stripMargin))

  def generateArrayExprAst(): Unit = generateScalaParserCode("ArrayExprAst",
    """E:Expr = 'a' {Literal(value=$0)} | A
      |A = '[' WS E (WS ',' WS E)* WS ']' {Arr(elems=[$2]+$3)}
      |WS = ' '*
      |""".stripMargin,
    examples = List("[a]"))

  def generateLongestMatch(): Unit = generateScalaParserCode("LongestMatchAst",
    """S = Token*
      |Token = <Id | Num | WS>
      |Id = 'a-zA-Z'+
      |Num = '0-9'+
      |WS = ' \n\r\t'+
      |""".stripMargin
  )

  def generateExceptMatch(): Unit = generateScalaParserCode("ExceptMatchAst",
    """S = Token*
      |Token: Token = <Id | Kw | Num | WS>
      |Id = Name-Kw {Id(name=str($0))}
      |Kw = "if" {Keyword(typ:%Kw=%IF)} | "else" {Keyword(%ELSE)}
      |Name = 'a-zA-Z'+
      |Num = '0-9'+ {Number(value=str($0))}
      |WS = ' \n\r\t'+ {WS()}
      |""".stripMargin)

  def generateExpressionGrammar(): Unit = generateScalaParserCode("ExpressionGrammarAst",
    """Expression: Expression = Term
      |    | Expression WS '+' WS Term {BinOp(op=$2, lhs=$0, rhs=$4)}
      |Term: Term = Factor
      |    | Term WS '*' WS Factor {BinOp($2, $0, $4)}
      |Factor: Factor = Number
      |    | Variable
      |    | '(' WS Expression WS ')' {Paren(expr=$2)}
      |Number: Number = '0' {Integer(value="0")}
      |    | '1-9' '0-9'* {Integer(str($0) + str($1))}
      |Variable = <'A-Za-z'+> {Variable(name=$0)}
      |WS = ' '*
      |""".stripMargin)

  def generateProto2DefinitionAst(): Unit = {
    val analysis = generateScalaParserCode("Proto2DefinitionAst", readFile("./examples/src/main/resources/proto2.cdg"))
    val grammarProto = GrammarProtobufConverter.convertNGrammarToProto(analysis.ngrammar)
    println(grammarProto)
    println(grammarProto.getSerializedSize)

    val writer = new BufferedOutputStream(new FileOutputStream("Proto2DefinitionGrammar.pb"))
    grammarProto.writeTo(writer)
    writer.close()
  }

  def generateProto3DefinitionAst(): Unit = generateScalaParserCode("Proto3DefinitionAst",
    readFile("./examples/src/main/resources/proto3.cdg"))

  def main(args: Array[String]): Unit = {
    //    generateArrayExprAst()
    //    generateAutoDbAst()
    generateMetaLang3Ast()
    //    generateMlProtoAst()
    //    generateLongestMatch()
    //    generateExceptMatch()
    //    generateExpressionGrammar()
    //    generateProto3DefinitionAst()
    //    generateProto2DefinitionAst()
    //    generateScalaParserCode("AutodbSchema1Ast", readFile("./examples/src/main/resources/autodb/autodb_schema1.cdg"))
  }
}
