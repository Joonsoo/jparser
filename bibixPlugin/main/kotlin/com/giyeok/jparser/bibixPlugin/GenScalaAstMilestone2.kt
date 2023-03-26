package com.giyeok.jparser.bibixPlugin

import com.giyeok.bibix.base.*
import com.giyeok.jparser.milestone2.`MilestoneParser2ProtobufConverter$`
import com.giyeok.jparser.milestone2.MilestoneParserGen
import kotlin.io.path.*

class GenScalaAstMilestone2 {
  fun build(context: BuildContext): BibixValue {
    context.progressLogger.logInfo("GenScalaAstMilestone2 started (dest=${context.destDirectory})")

    val genResult = generateScalaAst(context)

    context.progressLogger.logInfo("Starting parsergen...")
    val milestoneParserGen = MilestoneParserGen(genResult.grammarAnalysis.ngrammar())
    val parserData = milestoneParserGen.parserData()

    context.progressLogger.logInfo("Writing parser data...")
    genResult.parserDataFile.outputStream().buffered().use { stream ->
      `MilestoneParser2ProtobufConverter$`.`MODULE$`.toProto(parserData).writeTo(stream)
    }

    return ClassInstanceValue(
      "com.giyeok.jparser",
      "JParserData",
      mapOf(
        "srcsRoot" to DirectoryValue(genResult.srcsRoot),
        "astifier" to FileValue(genResult.astifierPath),
        "parserData" to FileValue(genResult.parserDataFile)
      )
    )
  }
}
