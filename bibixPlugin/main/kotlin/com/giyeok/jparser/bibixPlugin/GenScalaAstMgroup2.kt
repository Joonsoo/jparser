package com.giyeok.jparser.bibixPlugin

import com.giyeok.bibix.base.*
import com.giyeok.jparser.mgroup2.`MilestoneGroupParserDataProtobufConverter$`
import com.giyeok.jparser.mgroup2.MilestoneGroupParserGen
import kotlin.io.path.outputStream

class GenScalaAstMgroup2 {
  fun build(context: BuildContext): BibixValue {
    context.progressLogger.logInfo("GenScalaAstMgroup2 started (dest=${context.destDirectory})")

    val genResult = generateScalaAst(context)

    context.progressLogger.logInfo("Starting parsergen...")
    val milestoneParserGen = MilestoneGroupParserGen(genResult.grammarAnalysis.ngrammar())
    val parserData = milestoneParserGen.parserData()

    context.progressLogger.logInfo("Writing parser data...")
    genResult.parserDataFile.outputStream().buffered().use { stream ->
      `MilestoneGroupParserDataProtobufConverter$`.`MODULE$`.toProto(parserData).writeTo(stream)
    }

    return genResult.bibixValue()
  }
}
