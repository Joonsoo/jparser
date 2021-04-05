package com.giyeok.jparser.cli

import com.giyeok.jparser.metalang3a.MetaLanguage3
import com.giyeok.jparser.metalang3a.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGen
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGen.CodeBlob
import com.giyeok.jparser.parsergen.milestone.MilestoneParserGen
import com.giyeok.jparser.parsergen.proto.MilestoneParserProtobufConverter
import com.giyeok.jparser.utils.FileUtil.{readFile, writeFile}
import picocli.CommandLine
import picocli.CommandLine.{Command, Option}

import java.io.File
import java.util.Base64

@Command(name = "jparser", version = Array("1.0.0"))
class Main extends Runnable {
  @Option(names = Array("--input"), required = true)
  var input: File = new File("")

  @Option(names = Array("--targetDir"), required = true)
  var targetDir: File = new File("")

  @Option(names = Array("--package"))
  var packageName: String = "com.giyeok.jparser"

  @Option(names = Array("--grammarName"))
  var grammarName: String = "GeneratedGrammar"

  @Option(names = Array("--parsertype"))
  var parserType: ParserType.Value = ParserType.NaiveParser

  @Option(names = Array("--grammarProtoPath"))
  var grammarProtoPath: String = ""

  override def run(): Unit = {
    val analysis = MetaLanguage3.analyzeGrammar(readFile(input), grammarName)
    val codegen = new ScalaCodeGen(analysis)

    generateMilestoneParser(analysis, codegen)
  }

  def generateNaiveParser(analysis: ProcessedGrammar, codegen: ScalaCodeGen): Unit = {

  }

  def generateMilestoneParser(analysis: ProcessedGrammar, codegen: ScalaCodeGen): Unit = {
    val milestoneParserData = MilestoneParserGen.generateMilestoneParserData(analysis.ngrammar)
    val milestoneParserDataBase64 = Base64.getEncoder.encodeToString(
      MilestoneParserProtobufConverter.convertMilestoneParserDataToProto(milestoneParserData).toByteArray)

    val startType = codegen.typeDescStringOf(analysis.nonterminalTypes(analysis.startNonterminalName))

    val milestoneParserDef = CodeBlob(
      s"""|val milestoneParserData =
          |  MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(
          |    MilestoneParserDataProto.MilestoneParserData.parseFrom(
          |      Base64.getDecoder.decode("$milestoneParserDataBase64")))
          |
          |val milestoneParser = new MilestoneParser(milestoneParserData)
          |
          |def parse(text: String): Either[ParseForest, ParsingErrors.ParsingError] =
          |  milestoneParser.parseAndReconstructToForest(text)
          |
          |def parseAst(text: String): Either[${startType.code}, ParsingErrors.ParsingError] =
          |  parse(text) match {
          |    case Left(forest) => matchStart(forest.trees.head)
          |    case Right(error) => Right(error)
          |  }
          |""",
      Set(
        "com.giyeok.jparser.parsergen.proto.MilestoneParserDataProto",
        "com.giyeok.jparser.parsergen.proto.MilestoneParserProtobufConverter",
        "com.giyeok.jparser.parsergen.milestone.MilestoneParser",
        "com.giyeok.jparser.parsergen.milestone.MilestoneParserContext",
        "com.giyeok.jparser.ParseForest"
      ))

    val targetFilePath = new File(targetDir, s"${packageName.split('.').mkString("/")}/$grammarName.scala")
    val code = codegen.generateParser(grammarName, codegen.inlinedProtoNGrammar(), milestoneParserDef, None)

    writeFile(targetFilePath, code)
  }
}

object ParserType extends Enumeration {
  val NaiveParser, MilestoneParser, MilestoneGroupParser = Value
}

object Main {
  def main(args: Array[String]): Unit = {
    new CommandLine(new Main()).execute(args: _*)
  }
}
