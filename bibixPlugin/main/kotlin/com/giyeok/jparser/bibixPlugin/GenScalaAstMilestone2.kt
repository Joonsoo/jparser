package com.giyeok.jparser.bibixPlugin

import com.giyeok.bibix.base.*
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.metalang3.codegen.ScalaCodeGen
import com.giyeok.jparser.metalang3.codegen.ScalaCodeGen.`InlineSourceDef$`
import com.giyeok.jparser.milestone2.`MilestoneParser2ProtobufConverter$`
import com.giyeok.jparser.milestone2.MilestoneParserGen
import kotlin.io.path.*

class GenScalaAstMilestone2 {
  fun build(context: BuildContext): BibixValue {
    context.progressLogger.logInfo("GenScalaAstMilestone2 started (dest=${context.destDirectory})")
    val cdgDef = (context.arguments.getValue("cdgFile") as FileValue).file.readText()
    val astifierClassName =
      (context.arguments.getValue("astifierClassName") as StringValue).value.split('.')
    val packageName = astifierClassName.dropLast(1)
    val className = astifierClassName.last()
    val parserDataFileName = (context.arguments.getValue("parserDataFileName") as StringValue).value

    context.progressLogger.logInfo("Analyzing grammar... (grammar size=${cdgDef.length})")
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdgDef, className)

    val srcsDir = context.destDirectory.resolve("srcs")
    val targetDir = packageName.fold(srcsDir) { path, name -> path.resolve(name) }.absolute()
    try {
      targetDir.createDirectories()
    } catch (e: FileAlreadyExistsException) {
      // Ignore
    }
    val astFile = targetDir.resolve("$className.scala")
    val codegen = ScalaCodeGen(
      grammarAnalysis, ScalaCodeGen.Options(
        false,
        false,
        true,
        true,
        ScalaCodeGen.`AuxTrait$`.`MODULE$`.WithIdAndParseNode(),
        `InlineSourceDef$`.`MODULE$`,
        false
      )
    )
    val code = (if (packageName.isEmpty()) "" else "package ${packageName.joinToString(".")}\n\n") +
      codegen.generateParser(className)
    astFile.writeText(code)

    context.progressLogger.logInfo("Starting parsergen...")
    val milestoneParserGen = MilestoneParserGen(grammarAnalysis.ngrammar())
    val parserData = milestoneParserGen.parserData()

    val resourcesDir = context.destDirectory.resolve("resources")
    try {
      resourcesDir.createDirectories()
    } catch (e: FileAlreadyExistsException) {
      // Ignore
    }
    val parserDataFile = resourcesDir.resolve(parserDataFileName)
    context.progressLogger.logInfo("Writing parser data...")
    parserDataFile.outputStream().buffered().use { stream ->
      `MilestoneParser2ProtobufConverter$`.`MODULE$`.toProto(parserData).writeTo(stream)
    }

    return ClassInstanceValue(
      "com.giyeok.jparser",
      "JParserData",
      mapOf(
        "srcsRoot" to DirectoryValue(srcsDir),
        "astifier" to FileValue(astFile),
        "parserData" to FileValue(parserDataFile)
      )
    )
  }
}
