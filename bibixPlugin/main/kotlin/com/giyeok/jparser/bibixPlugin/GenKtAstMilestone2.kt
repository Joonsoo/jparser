package com.giyeok.jparser.bibixPlugin

import com.giyeok.bibix.base.*
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.metalang3.codegen.KotlinOptCodeGen
import com.giyeok.jparser.milestone2.`MilestoneParser2ProtobufConverter$`
import com.giyeok.jparser.milestone2.MilestoneParserGen
import kotlin.io.path.*

class GenKtAstMilestone2 {
  fun build(context: BuildContext): BibixValue {
    context.progressLogger.logInfo("GenKtAstMilestone2 started (dest=${context.destDirectory})")

    val analysisResult = analyzeGrammar(context)

    context.progressLogger.logInfo("Starting parsergen...")
    val milestoneParserGen = MilestoneParserGen(analysisResult.grammarAnalysis.ngrammar())
    val parserData0 = milestoneParserGen.parserData()
    val parserData = if (analysisResult.trimParserData) {
      parserData0.trimTasksSummariesForSymbols(analysisResult.symbolsOfInterest)
    } else {
      parserData0
    }

    context.progressLogger.logInfo("Writing parser data...")
    analysisResult.parserDataFilePath.outputStream().buffered().use { stream ->
      `MilestoneParser2ProtobufConverter$`.`MODULE$`.toProto(parserData).writeTo(stream)
    }

    return ClassInstanceValue(
      "com.giyeok.jparser",
      "JParserData",
      mapOf(
        "srcsRoot" to DirectoryValue(analysisResult.srcsRoot),
        "astifier" to FileValue(analysisResult.astifierPath),
        "parserData" to FileValue(analysisResult.parserDataFilePath)
      )
    )
  }
}
