package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.metalang3.codegen.KotlinOptCodeGen
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess

// CLI usage:
//   java -jar genCliJar <cdgFile> \
//     -astifierClass <FQCN> \
//     -parserdata <pb-out> \
//     -srcs <srcsDir-out> \
//     [-trim]
//
// 기능은 bibixPlugin 의 GenKtAstMgroup2 와 동일하지만 bibix BuildContext 의존성을
// 끊고 stand-alone jar 로 실행하도록 옮긴 것. mgroup3.GenCli 의 Stage1+Stage3 와
// 같은 구조를 따른다.

private data class CliArgs(
  val cdgFile: Path,
  val astifierClassName: String,
  val parserDataOut: Path,
  val srcsDir: Path,
  val trim: Boolean,
)

private const val USAGE =
  "usage: GenCli <cdgFile> -astifierClass <FQCN> -parserdata <pb> -srcs <dir> [-trim]"

private fun parseArgs(argv: Array<String>): CliArgs {
  if (argv.isEmpty()) error("missing positional <cdgFile>\n$USAGE")
  val cdg = Path(argv[0])
  var astifier: String? = null
  var parserData: Path? = null
  var srcs: Path? = null
  var trim = false
  var i = 1
  while (i < argv.size) {
    val flag = argv[i]
    when (flag) {
      "-trim" -> { trim = true; i += 1 }
      else -> {
        if (i + 1 >= argv.size) error("flag $flag requires a value\n$USAGE")
        val v = argv[i + 1]
        when (flag) {
          "-astifierClass" -> astifier = v
          "-parserdata" -> parserData = Path(v)
          "-srcs" -> srcs = Path(v)
          else -> error("unknown flag: $flag\n$USAGE")
        }
        i += 2
      }
    }
  }
  return CliArgs(
    cdgFile = cdg,
    astifierClassName = astifier ?: error("missing -astifierClass\n$USAGE"),
    parserDataOut = parserData ?: error("missing -parserdata\n$USAGE"),
    srcsDir = srcs ?: error("missing -srcs\n$USAGE"),
    trim = trim,
  )
}

fun main(argv: Array<String>) {
  val args = try {
    parseArgs(argv)
  } catch (e: IllegalStateException) {
    System.err.println(e.message)
    exitProcess(2)
  }

  val cdg = args.cdgFile.readText()

  // astifierClassName 을 패키지/클래스로 쪼갠다. (예: com.foo.bar.Ast → 패키지 com.foo.bar, 클래스 Ast)
  val parts = args.astifierClassName.split('.')
  val packageName = parts.dropLast(1).joinToString(".")
  val className = parts.last()

  val processed = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, className)
  if (!processed.errors().isClear) {
    System.err.println("Grammar analysis errors:\n${processed.errors().errors()}")
    exitProcess(3)
  }

  // 1) Kotlin AST 소스 생성
  val codegen = KotlinOptCodeGen(processed)
  val astSrc = codegen.generate(className, packageName)
  val pkgDir = args.srcsDir.resolve(packageName.replace('.', '/'))
  pkgDir.createDirectories()
  pkgDir.resolve("$className.kt").writeText(astSrc)

  // 2) parser data 생성 + (optional) trim + protobuf 직렬화
  val parserGen = MilestoneGroupParserGen(processed.ngrammar())
  val rawData = parserGen.parserData()
  val data = if (args.trim) {
    rawData.trimTasksSummariesForSymbols(codegen.symbolsOfInterest())
  } else {
    rawData
  }
  args.parserDataOut.parent?.createDirectories()
  args.parserDataOut.outputStream().buffered().use { os ->
    `MilestoneGroupParserDataProtobufConverter$`.`MODULE$`.toProto(data).writeTo(os)
  }
}
