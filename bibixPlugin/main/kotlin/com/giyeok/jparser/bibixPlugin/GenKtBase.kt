package com.giyeok.jparser.bibixPlugin

import com.giyeok.bibix.base.BooleanValue
import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.base.StringValue
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.metalang3.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3.codegen.KotlinOptCodeGen
import scala.collection.immutable.Set
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class AnalysisResult(
  val grammarAnalysis: ProcessedGrammar,
  val srcsRoot: Path,
  val astifierPath: Path,
  val parserDataFilePath: Path,
  val trimParserData: Boolean,
  val symbolsOfInterest: Set<Any>,
)

fun analyzeGrammar(context: BuildContext): AnalysisResult {
  val cdgDef = (context.arguments.getValue("cdgFile") as FileValue).file.readText()
  val astifierClassName =
    (context.arguments.getValue("astifierClassName") as StringValue).value.split('.')
  val packageName = astifierClassName.dropLast(1)
  val className = astifierClassName.last()
  val parserDataFileName = (context.arguments.getValue("parserDataFileName") as StringValue).value
  val trimParserData = (context.arguments.getValue("trimParserData") as BooleanValue).value

  context.progressLogger.logInfo("Analyzing grammar... (grammar size=${cdgDef.length})")
  val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdgDef, className)
  if (!grammarAnalysis.errors().isClear) {
    throw IllegalStateException("${grammarAnalysis.errors().errors()}")
  }

  val srcsDir = context.destDirectory.resolve("srcs")
  val targetDir = packageName.fold(srcsDir) { path, name -> path.resolve(name) }.absolute()
  try {
    targetDir.createDirectories()
  } catch (e: FileAlreadyExistsException) {
    // Ignore
  }
  val astFile = targetDir.resolve("$className.kt")

  context.progressLogger.logInfo("Starting codegen...")
  val codegen = KotlinOptCodeGen(grammarAnalysis)
  astFile.writeText(codegen.generate(className, packageName.joinToString(".")))

  val resourcesDir = context.destDirectory.resolve("resources")
  try {
    resourcesDir.createDirectories()
  } catch (e: FileAlreadyExistsException) {
    // Ignore
  }
  val parserDataFile = resourcesDir.resolve(parserDataFileName)

  return AnalysisResult(
    grammarAnalysis,
    srcsDir,
    astFile,
    parserDataFile,
    trimParserData,
    codegen.symbolsOfInterest(),
  )
}
