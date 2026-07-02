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
  // 생성될 Kotlin AST 클래스의 FQCN (예: com.giyeok.mulang.MulangAst).
  // 마지막 컴포넌트 = 클래스명, 그 앞 = 패키지. proto 패키지는 "<패키지>.<클래스명소문자>proto"
  // 대신 "<패키지>.ast" 규약을 쓴다 (기존 스냅샷과 호환).
  val astifierClass: String = "com.giyeok.jparser.mgroup3.generated.Ast",
  // Cargo.toml 의 crate 이름. 여러 문법의 crate 가 공존할 수 있도록 파라미터화.
  val crateName: String = "mgroup3-generated-parser",
  // parserdata 에서 grammar(NGrammar) 필드를 제거 — 파서 런타임(Kotlin/Rust 모두)은
  // 읽지 않는 디버그 정보라 배포 크기를 줄인다.
  val trimGrammar: Boolean = false,
) {
  val astifierPackage: String get() = astifierClass.substringBeforeLast('.')
  val astifierClassName: String get() = astifierClass.substringAfterLast('.')
  val protoPackage: String get() = "$astifierPackage.ast"
}

class CliUsageError(message: String) : RuntimeException(message)

object ArgsParser {
  private const val USAGE =
    "usage: GenCli <cdgFile> -parserdata <pb[.gz]> -proto <proto> -rust <dir> -kotlin <dir>" +
      " [-mgroup3-native <path>] [-astifierClass <fqcn>] [-crateName <name>] [-trimGrammar]"

  fun parse(argv: Array<String>): CliArgs {
    if (argv.isEmpty()) throw CliUsageError("missing positional <cdgFile>\n$USAGE")
    val cdgFile = Path(argv[0])

    var parserData: Path? = null
    var proto: Path? = null
    var rustDir: Path? = null
    var kotlinDir: Path? = null
    var mgroup3NativePath: String? = null
    var astifierClass: String? = null
    var crateName: String? = null
    var trimGrammar = false

    var i = 1
    while (i < argv.size) {
      val flag = argv[i]
      if (!flag.startsWith("-")) {
        throw CliUsageError("expected flag, got positional: $flag\n$USAGE")
      }
      // 값 없는 플래그.
      if (flag == "-trimGrammar") {
        trimGrammar = true
        i += 1
        continue
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
        "-astifierClass" -> astifierClass = value
        "-crateName" -> crateName = value
        else -> throw CliUsageError(
          "unknown flag: $flag; expected one of -parserdata|-proto|-rust|-kotlin|-mgroup3-native|" +
            "-astifierClass|-crateName|-trimGrammar\n$USAGE"
        )
      }
      i += 2
    }

    val astifier = astifierClass ?: "com.giyeok.jparser.mgroup3.generated.Ast"
    if (!astifier.contains('.')) {
      throw CliUsageError("-astifierClass must be a fully-qualified class name (got: $astifier)\n$USAGE")
    }

    return CliArgs(
      cdgFile = cdgFile,
      parserData = parserData ?: throw CliUsageError("missing -parserdata\n$USAGE"),
      proto = proto ?: throw CliUsageError("missing -proto\n$USAGE"),
      rustDir = rustDir ?: throw CliUsageError("missing -rust\n$USAGE"),
      kotlinDir = kotlinDir ?: throw CliUsageError("missing -kotlin\n$USAGE"),
      mgroup3NativePath = mgroup3NativePath ?: "../../mgroup3-native",
      astifierClass = astifier,
      crateName = crateName ?: "mgroup3-generated-parser",
      trimGrammar = trimGrammar,
    )
  }
}
