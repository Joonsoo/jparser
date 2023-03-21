package com.giyeok.jparser.bibixPlugin

import com.giyeok.bibix.base.*
import com.giyeok.jparser.mgroup2.`MilestoneGroupParserDataProtobufConverter$`
import com.giyeok.jparser.mgroup2.MilestoneGroupParserGen
import kotlin.io.path.*

class GenKtAstMgroup2 {
  fun build(context: BuildContext): BibixValue {
    context.progressLogger.logInfo("GenKtAstMgroup2 started (dest=${context.destDirectory})")

    val analysisResult = analyzeGrammar(context)

    context.progressLogger.logInfo("Starting parsergen...")
    val milestoneParserGen = MilestoneGroupParserGen(analysisResult.grammarAnalysis.ngrammar())
    val parserData0 = milestoneParserGen.parserData()
    val parserData = if (analysisResult.trimParserData) {
      parserData0.trimTasksSummariesForSymbols(analysisResult.symbolsOfInterest)
    } else {
      parserData0
    }

    context.progressLogger.logInfo("Writing parser data...")
    analysisResult.parserDataFilePath.outputStream().buffered().use { stream ->
      `MilestoneGroupParserDataProtobufConverter$`.`MODULE$`.toProto(parserData).writeTo(stream)
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
