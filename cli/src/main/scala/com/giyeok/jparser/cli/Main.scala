package com.giyeok.jparser.cli

import com.giyeok.jparser.metalang3a.MetaLanguage3
import com.giyeok.jparser.metalang3a.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGen
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGen.CodeBlob
import com.giyeok.jparser.milestone.MilestoneParserGen
import com.giyeok.jparser.proto.MilestoneParserProtobufConverter
import com.giyeok.jparser.utils.FileUtil.{readFile, writeFile}
import picocli.CommandLine
import picocli.CommandLine.{Command, Option}

import java.io.File
import java.util.Base64

// example args: --input ./examples/src/main/resources/autodb/autodb_schema1.cdg --targetDir /home/joonsoo/Documents/workspace/autodb/autodb-ast/src/main/scala --milestoneParserDataPath /home/joonsoo/Documents/workspace/autodb/autodb-ast/src/main/resources/autodbschemadata.pb --grammarName AutodbSchema1Grammar --packageName com.giyeok.autodb
@Command(name = "jparser", version = Array("1.0.0"))
class Main extends Runnable {
  @Option(names = Array("--input"), required = true)
  var input: File = new File("")

  @Option(names = Array("--targetDir"), required = true)
  var targetDir: File = new File("")

  @Option(names = Array("--packageName"))
  var packageName: String = "com.giyeok.jparser"

  @Option(names = Array("--grammarName"))
  var grammarName: String = "GeneratedGrammar"

  @Option(names = Array("--parsertype"))
  var parserType: ParserType.Value = ParserType.NaiveParser

  @Option(names = Array("--grammarProtoPath"))
  var grammarProtoPath: File = _

  @Option(names = Array("--milestoneParserDataPath"))
  var milestoneParserDataPath: File = _

  override def run(): Unit = {
    println("Starting...")
    val analysis = MetaLanguage3.analyzeGrammar(readFile(input), grammarName)
    println("Analysis done")
    if (!analysis.errors.isClear) {
      throw new Exception(s"Grammar error: ${analysis.errors}")
    }
    val codegen = new ScalaCodeGen(analysis)

    println("Generating parser...")
    generateMilestoneParser(analysis, codegen)
    println("Done")
  }

  def generateNaiveParser(analysis: ProcessedGrammar, codegen: ScalaCodeGen): Unit = {

  }

  def generateMilestoneParser(analysis: ProcessedGrammar, codegen: ScalaCodeGen): Unit = {
    val startType = codegen.typeDescStringOf(analysis.nonterminalTypes(analysis.startNonterminalName))

    val milestoneParserDataDef = if (milestoneParserDataPath == null) {
      //      val milestoneParserDataBase64 = Base64.getEncoder.encodeToString(milestoneParserDataProto)
      //      CodeBlob(
      //        s"""val milestoneParserData: MilestoneParserData =
      //           |  MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(
      //           |    MilestoneParserDataProto.MilestoneParserData.parseFrom(
      //           |      Base64.getDecoder.decode("$milestoneParserDataBase64")))""".stripMargin,
      //        Set("java.util.Base64",
      //          "com.giyeok.jparser.parsergen.milestone.MilestoneParserData"))
      ???
    } else {
      CodeBlob(
        s"""val milestoneParserData = MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(
           |  MilestoneParserDataProto.MilestoneParserData.parseFrom(readFileBytes(${codegen.escapeString(milestoneParserDataPath.getPath)})))""".stripMargin,
        Set("com.giyeok.jparser.utils.FileUtil.readFileBytes"))
    }
    val milestoneParserDef = CodeBlob(
      s"""${milestoneParserDataDef.indent().code}
         |
         |val milestoneParser = new MilestoneParser(milestoneParserData)
         |
         |def parse(text: String): Either[ParseForest, ParsingErrors.ParsingError] =
         |  milestoneParser.parseAndReconstructToForest(text)
         |
         |def parseAst(text: String): Either[${startType.code}, ParsingErrors.ParsingError] =
         |  parse(text) match {
         |    case Left(forest) => Left(matchStart(forest.trees.head))
         |    case Right(error) => Right(error)
         |  }
         |""",
      milestoneParserDataDef.required ++ Set(
        "com.giyeok.jparser.proto.MilestoneParserDataProto",
        "com.giyeok.jparser.proto.MilestoneParserProtobufConverter",
        "com.giyeok.jparser.milestone.MilestoneParser",
        "com.giyeok.jparser.milestone.MilestoneParserContext",
        "com.giyeok.jparser.ParseForest",
        "com.giyeok.jparser.ParsingErrors"
      ))

    val code = (if (packageName.isEmpty) "" else s"package $packageName\n\n") +
      codegen.generateParser(grammarName, codegen.inlinedProtoNGrammar(), milestoneParserDef, None)

    val targetFilePath = new File(targetDir, s"${packageName.split('.').mkString("/")}/$grammarName.scala")

    writeFile(targetFilePath, code)
    println("Code generated. Generating milestone parser data...")

    val milestoneParserData = MilestoneParserGen.generateMilestoneParserData(analysis.ngrammar)
    val milestoneParserDataProto = MilestoneParserProtobufConverter.convertMilestoneParserDataToProto(milestoneParserData).toByteArray

    writeFile(milestoneParserDataPath, milestoneParserDataProto)
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
