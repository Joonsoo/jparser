package com.giyeok.jparser.mgroup3

import java.nio.file.Path
import kotlin.io.path.Path

data class CliArgs(
  val cdgFile: Path,
  val parserData: Path,
  val proto: Path,
  val rustDir: Path,
  val kotlinDir: Path,
  val mgroup3NativePath: String = "../../mgroup3-native",
)

class CliUsageError(message: String) : RuntimeException(message)

object ArgsParser {
  private const val USAGE =
    "usage: GenCli <cdgFile> -parserdata <pb> -proto <proto> -rust <dir> -kotlin <dir> [-mgroup3-native <path>]"

  fun parse(argv: Array<String>): CliArgs {
    if (argv.isEmpty()) throw CliUsageError("missing positional <cdgFile>\n$USAGE")
    val cdgFile = Path(argv[0])

    var parserData: Path? = null
    var proto: Path? = null
    var rustDir: Path? = null
    var kotlinDir: Path? = null
    var mgroup3NativePath: String? = null

    var i = 1
    while (i < argv.size) {
      val flag = argv[i]
      if (!flag.startsWith("-")) {
        throw CliUsageError("expected flag, got positional: $flag\n$USAGE")
      }
      if (i + 1 >= argv.size) {
        throw CliUsageError("flag $flag requires a value\n$USAGE")
      }
      val value = argv[i + 1]
      when (flag) {
        "-parserdata" -> parserData = Path(value)
        "-proto" -> proto = Path(value)
        "-rust" -> rustDir = Path(value)
        "-kotlin" -> kotlinDir = Path(value)
        "-mgroup3-native" -> mgroup3NativePath = value
        else -> throw CliUsageError(
          "unknown flag: $flag; expected one of -parserdata|-proto|-rust|-kotlin|-mgroup3-native\n$USAGE"
        )
      }
      i += 2
    }

    return CliArgs(
      cdgFile = cdgFile,
      parserData = parserData ?: throw CliUsageError("missing -parserdata\n$USAGE"),
      proto = proto ?: throw CliUsageError("missing -proto\n$USAGE"),
      rustDir = rustDir ?: throw CliUsageError("missing -rust\n$USAGE"),
      kotlinDir = kotlinDir ?: throw CliUsageError("missing -kotlin\n$USAGE"),
      mgroup3NativePath = mgroup3NativePath ?: "../../mgroup3-native",
    )
  }
}
