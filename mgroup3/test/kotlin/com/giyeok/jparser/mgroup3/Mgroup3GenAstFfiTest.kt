package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.mgroup3.generated.Ast
import com.giyeok.jparser.mgroup3.generated.AstProtoBinding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText

/**
 * Phase B FFI end-to-end (Rust↔Kotlin 교차 검증):
 *   1. GenCli 스테이지를 라이브러리로 호출해 asdl 의 parserdata + Rust crate 생성
 *   2. `cargo build --features ffi` 로 cdylib 빌드 (안정된 캐시 디렉토리 사용)
 *   3. FFM([GeneratedAstNativeBridge])으로 로드: Rust 가 parse→walk→encode 한
 *      ast.proto ParseResult 바이트를 받아
 *   4. Kotlin 의 생성된 [AstProtoBinding] 으로 decode → Kotlin walk 결과와 비교.
 *
 * cargo 빌드가 필요하므로 기본 스위트에서는 제외 (env gate).
 * 실행: `bibix4 runMgroup3GenAstFfiTest`
 */
@EnabledIfEnvironmentVariable(named = "MGROUP3_GEN_FFI", matches = "1")
class Mgroup3GenAstFfiTest {
  private val inputs = listOf(
    "module M { foo = Bar(int x) | Baz }",
    "module M { a = (x y) }",
    "module Mod { foo = Bar(int x, str y) | Baz | Qux\n  attributes (int z) }",
    "module M {\n  -- comment here\n  foo = Bar(int x)\n}",
  )

  @Test
  fun rustFfiAstMatchesKotlinWalk() {
    val cdg = Path("examples/metalang3/resources/asdl/grammar.cdg").readText()
    val processed = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "AsdlGrammar")
    check(processed.errors().isClear) { "grammar analysis errors: ${processed.errors().errors()}" }

    // 1. codegen — 안정 캐시 디렉토리 (cargo incremental 재사용).
    val outDir = Path(System.getProperty("java.io.tmpdir"), "jparser-gen-ffi-asdl")
    Files.createDirectories(outDir)
    val parserDataPath = outDir.resolve("parserdata.pb")
    val rustDir = outDir.resolve("rust")
    Stage1ParserData.run(processed, parserDataPath)
    val schema = SchemaBuilder.build(processed, packageName = "com.giyeok.jparser.mgroup3.generated.ast")
    Stage4RustEmit.run(
      processed, schema, rustDir,
      mgroup3NativePath = Path("mgroup3-native").toAbsolutePath().toString(),
    )

    // 2. cargo build --features ffi
    val cargoLog = outDir.resolve("cargo-build.log").toFile()
    val proc = ProcessBuilder("cargo", "build", "--features", "ffi")
      .directory(rustDir.toFile())
      .redirectOutput(cargoLog)
      .redirectErrorStream(true)
      .start()
    val exit = proc.waitFor()
    check(exit == 0) { "cargo build failed (exit=$exit):\n${cargoLog.readText().takeLast(4000)}" }
    val dylib = findDylib(rustDir)

    // Kotlin 측 기준: 같은 parserdata 의미의 파서로 walk.
    val kotlinParser = Mgroup3Parser(Mgroup3ParserGenerator(processed.ngrammar()).generate())

    // 3+4. FFI parse→walk→encode 결과를 decode 해 Kotlin walk 와 비교.
    GeneratedAstNativeBridge(dylib).use { bridge ->
      val parser = bridge.newParserFromFile(parserDataPath)
      try {
        for (input in inputs) {
          val rustBytes = bridge.parseAst(parser, input)
          val decoded = AstProtoBinding.fromProtoBytes(rustBytes)

          val ctx = kotlinParser.parseOrThrow(input)
          val kotlinAst = Ast(input, kotlinParser.kernelsHistory(ctx)).matchStart()

          // nodeId 는 walk(IdIssuer)과 encoder 가 독립적으로 할당 — optional 필드의
          // data class toString 폴백으로 shortString 에 샐 수 있어 정규화 후 비교.
          val expected = normalize(kotlinAst.toShortString())
          val actual = normalize(decoded.toShortString())
          println("FFI  ${input.replace("\n", "\\n")}\n  => $actual")
          assertEquals(expected, actual, "Rust FFI AST != Kotlin walk AST: $input")
          assertEquals(kotlinAst.start, decoded.start)
          assertEquals(kotlinAst.end, decoded.end)
        }
      } finally {
        bridge.freeParser(parser)
      }
    }
  }

  private fun normalize(shortString: String): String =
    shortString.replace(Regex(", nodeId=\\d+, start=\\d+, end=\\d+"), "")

  private fun findDylib(rustDir: Path): Path {
    val base = rustDir.resolve("target").resolve("debug")
    for (name in listOf(
      "libmgroup3_generated_parser.dylib",
      "libmgroup3_generated_parser.so",
      "mgroup3_generated_parser.dll",
    )) {
      val p = base.resolve(name)
      if (Files.exists(p)) return p
    }
    error("generated parser dylib not found under $base")
  }
}
