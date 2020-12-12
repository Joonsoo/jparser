package com.giyeok.jparser.metalang3a

import java.io.File

import com.giyeok.jparser.examples.metalang3.MetaLang3Grammar
import com.giyeok.jparser.metalang3a.MetaLanguage3.writeScalaParserCode

import scala.io.Source

object Grammars {
  private def generate(name: String)(func: => Unit): Unit = {
    println(s"Generating $name...")
    func
    println(s"$name generated!")
  }

  def generateMetaLang3Ast(): Unit = generate("MetaLang3Ast") {
    writeScalaParserCode(MetaLang3Grammar.inMetaLang3.grammar, "MetaLang3Ast",
      "com.giyeok.jparser.metalang3a.generated", new File("./metalang/src/main/scala"),
      mainFuncExamples = Some(List("A = B C 'd' 'e'*")))
  }

  def readFile(path: String): String = {
    val file = new File(path)
    val source = Source.fromFile(file)
    try source.mkString finally source.close()
  }

  def generateMlProtoAst(): Unit = generate("MlProto") {
    writeScalaParserCode(readFile("./examples/src/main/resources/mlproto/mlproto.cdg"),
      "MlProtoAst",
      "com.giyeok.jparser.metalang3a.generated", new File("./metalang/src/main/scala"),
      mainFuncExamples = Some(List("module Conv2d<>(inChannels: Int) {}")))
  }

  def generateAutoDbAst(): Unit = generate("AutoDbAst") {
    writeScalaParserCode(readFile("./examples/src/main/resources/autodb/autodb.cdg"),
      "AutoDbAst",
      "com.giyeok.jparser.metalang3a.generated", new File("./metalang/src/main/scala"),
      mainFuncExamples = Some(List(
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
          |}""".stripMargin)))
  }

  def generateArrayExprAst(): Unit = generate("ArrayExprAst") {
    writeScalaParserCode(
      """E:Expr = 'a' {Literal(value=$0)} | A
        |A = '[' WS E (WS ',' WS E)* WS ']' {Arr(elems=[$2]+$3)}
        |WS = ' '*
        |""".stripMargin,
      "ArrayExprAst", "com.giyeok.jparser.metalang3a.generated", new File("./metalang/src/main/scala"),
      mainFuncExamples = Some(List("[a]")))
  }

  def main(args: Array[String]): Unit = {
    generateArrayExprAst()
    //    generateAutoDbAst()
    //    generateMetaLang3Ast()
    //    generateMlProtoAst()
  }
}
