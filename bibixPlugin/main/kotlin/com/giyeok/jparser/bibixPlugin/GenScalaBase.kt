package com.giyeok.jparser.bibixPlugin

import com.giyeok.bibix.base.*
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.metalang3.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3.codegen.ScalaCodeGen
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class ScalaAstCodeGenResult(
  val grammarAnalysis: ProcessedGrammar,
  val srcsRoot: Path,
  val astifierPath: Path,
  val resourcesDir: Path,
  val parserDataFile: Path,
) {
  fun bibixValue(): ClassInstanceValue = ClassInstanceValue(
    "com.giyeok.jparser",
    "JParserData",
    mapOf(
      "srcsRoot" to DirectoryValue(srcsRoot),
      "astifier" to FileValue(astifierPath),
      "parserData" to FileValue(parserDataFile)
    )
  )
}

fun generateScalaAst(context: BuildContext): ScalaAstCodeGenResult {
  val cdgDef = (context.arguments.getValue("cdgFile") as FileValue).file.readText()
  val astifierClassName =
    (context.arguments.getValue("astifierClassName") as StringValue).value.split('.')
  val packageName = astifierClassName.dropLast(1)
  val className = astifierClassName.last()

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
      ScalaCodeGen.`InlineSourceDef$`.`MODULE$`,
      false
    )
  )
  val code = (if (packageName.isEmpty()) "" else "package ${packageName.joinToString(".")}\n\n") +
    codegen.generateParser(className)
  astFile.writeText(code)

  val parserDataFileName = (context.arguments.getValue("parserDataFileName") as StringValue).value
  val resourcesDir = context.destDirectory.resolve("resources")
  try {
    resourcesDir.createDirectories()
  } catch (e: FileAlreadyExistsException) {
    // Ignore
  }
  val parserDataFile = resourcesDir.resolve(parserDataFileName)

  return ScalaAstCodeGenResult(
    grammarAnalysis,
    srcsDir,
    astFile,
    resourcesDir,
    parserDataFile
  )
}
