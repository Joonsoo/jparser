package com.giyeok.jparser.bibixPlugin

import com.giyeok.bibix.base.*
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.metalang3.codegen.KotlinOptCodeGen
import com.giyeok.jparser.milestone2.MilestoneParserGen
import com.giyeok.jparser.nparser2.NaiveParser2
import com.giyeok.jparser.proto.`MilestoneParser2ProtobufConverter$`
import kotlin.io.path.*

class GenKtAstMilestone2 {
  fun build(context: BuildContext): BibixValue {
    val cdgDef = (context.arguments.getValue("cdgFile") as FileValue).file.readText()
    val astifierClassName = (context.arguments.getValue("astifierClassName") as StringValue).value.split('.')
    val packageName = astifierClassName.dropLast(1)
    val className = astifierClassName.last()
    val parserDataFileName = (context.arguments.getValue("parserDataFileName") as StringValue).value
    val trimParserData = (context.arguments.getValue("trimParserData") as BooleanValue).value

    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdgDef, className)

    val targetDir = packageName.fold(context.destDirectory) { path, name -> path.resolve(name) }.absolute()
    try {
      targetDir.createDirectories()
    } catch (e: FileAlreadyExistsException) {
      // Ignore
    }
    val astFile = targetDir.resolve("$className.kt")

    val codegen = KotlinOptCodeGen(grammarAnalysis)
    astFile.writeText(codegen.generate(className, packageName.joinToString(".")))

    val milestoneParserGen = MilestoneParserGen(NaiveParser2(grammarAnalysis.ngrammar()))
    val parserData0 = milestoneParserGen.parserData()
    val parserData = if (trimParserData) {
      parserData0.trimTasksSummariesForSymbols(codegen.symbolsOfInterest())
    } else {
      parserData0
    }
    val parserDataFile = context.destDirectory.resolve(parserDataFileName)
    parserDataFile.outputStream().buffered().use { stream ->
      `MilestoneParser2ProtobufConverter$`.`MODULE$`.toProto(parserData).writeTo(stream)
    }

    return ClassInstanceValue(
      "com.giyeok.jparser",
      "JParserData",
      mapOf(
        "astifier" to FileValue(astFile),
        "parserData" to FileValue(parserDataFile)
      ))
  }
}
