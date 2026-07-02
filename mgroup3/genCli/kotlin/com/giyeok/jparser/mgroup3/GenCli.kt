package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import kotlin.io.path.readText
import kotlin.system.exitProcess

fun main(argv: Array<String>) {
  val args = try {
    ArgsParser.parse(argv)
  } catch (e: CliUsageError) {
    System.err.println(e.message)
    exitProcess(2)
  }

  val cdg = args.cdgFile.readText()
  val processed = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
  if (!processed.errors().isClear) {
    System.err.println("Grammar analysis errors:\n${processed.errors().errors()}")
    exitProcess(3)
  }

  Stage1ParserData.run(processed, args.parserData, trimGrammar = args.trimGrammar)

  val schema = SchemaBuilder.build(processed, packageName = args.protoPackage)
  Stage2ProtoEmit.run(schema, args.proto)
  Stage3KotlinEmit.run(
    processed, schema, args.kotlinDir,
    className = args.astifierClassName, pkg = args.astifierPackage,
  )
  Stage4RustEmit.run(processed, schema, args.rustDir, args.mgroup3NativePath, crateName = args.crateName)
}
