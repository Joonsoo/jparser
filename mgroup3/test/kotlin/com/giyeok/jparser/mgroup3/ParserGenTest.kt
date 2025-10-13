package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserKt
import com.giyeok.jparser.kttestutils.AstifySimulator
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup2.MilestoneGroupParserDataProtobufConverter
import com.giyeok.jparser.mgroup2.MilestoneGroupParserGen
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText

class ParserGenTest {
  @Test
  fun testPlainGrammar() {
    val cdg = """
      Expr = Term WS '+' WS Expr
           | Term
      Term = Factor WS '*' WS Term
           | Factor
      Factor = '0-9'
           | '(' WS Expr WS ')'
      WS = ' '*
    """.trimIndent()

    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()

    val gen = Mgroup3ParserGenerator(grammar)
    val generated = gen.generate()
    println(generated)
  }

  @Test
  fun testCmakeDebug() {
    val cdg = Path("examples/metalang3/resources/cmake/cmake_debug.cdg").readText()

    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()

    // cmake 문법에서 !bracket_open 이 있기 때문에 [[]]나 [=[]=]는 unquoted_argument가 되면 안되는데 잘 처리되지 않는 문제가 있음
    // -> mgroup3 만드는 이유
    val testSrc = "abc"

    val mg2data = MilestoneGroupParserGen(grammar).parserData()
    val mg2datapb = MilestoneGroupParserDataProtobufConverter.toProto(mg2data)
    val mg2parser = MilestoneGroupParserKt(mg2datapb).setVerbose()
    val mg2parsed = mg2parser.parse(testSrc)
    val mg2history = mg2parser.kernelsHistory(mg2parsed)
    val astifier = AstifySimulator(grammarAnalysis, mg2history, testSrc)
//    val ast = astifier.simulateAst()
//    println(ast)

    val gen = Mgroup3ParserGenerator(grammar)
    val generated = gen.generate()
//    println(generated)

    val parser = Mgroup3Parser(generated)
    val parsed = parser.parse(testSrc)
    println(parsed)
  }
}
