package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.mgroup3.generated.Ast
import com.giyeok.jparser.mgroup3.generated.AstProtoBinding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.lang.foreign.MemorySegment
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText

/**
 * Phase B FFI end-to-end (Rust↔Kotlin 교차 검증):
 *   1. GenCli 스테이지를 라이브러리로 호출해 asdl 의 parserdata + Rust crate 생성
 *   2. `cargo build --features ffi` 로 cdylib 빌드 — debug 와 release 양쪽
 *      (release 는 최적화/overflow 처리 차이로 동작이 달라질 수 있는 경로 확인용)
 *   3. FFM([GeneratedAstNativeBridge])으로 로드: Rust 가 parse→walk→encode 한
 *      ast.proto ParseResult 바이트를 받아
 *   4. Kotlin 의 생성된 [AstProtoBinding] 으로 decode → Kotlin walk 결과와 비교.
 *   5. 거부 입력의 에러 경로 (code 6=parse 중 거부, 7=완주 후 미수락) 와
 *      에러 후 파서 핸들 재사용을 확인.
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

  // 입력 → 기대 에러 코드. 6 = MGROUP3_GEN_ERR_PARSE (parse 도중 UnexpectedInput),
  // 7 = MGROUP3_GEN_ERR_REJECTED (끝까지 소비했지만 미수락 — 불완전 prefix, 빈 입력).
  // 빈 입력은 브릿지의 NULL ptr + len 0 경로도 함께 검증한다.
  private val rejectedInputs = listOf(
    "@@@" to 6,
    "module M { foo = Bar(int x) } trailing" to 6,
    "module M {" to 7,
    "" to 7,
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

    // Kotlin 측 기준: 같은 parserdata 의미의 파서로 walk.
    val kotlinParser = Mgroup3Parser(Mgroup3ParserGenerator(processed.ngrammar()).generate())

    for (release in listOf(false, true)) {
      val profile = if (release) "release" else "debug"
      cargoBuild(rustDir, outDir, release)
      val dylib = findDylib(rustDir, profile)

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
            println("FFI[$profile]  ${input.replace("\n", "\\n")}\n  => $actual")
            assertEquals(expected, actual, "[$profile] Rust FFI AST != Kotlin walk AST: $input")
            assertEquals(kotlinAst.start, decoded.start)
            assertEquals(kotlinAst.end, decoded.end)
          }

          for ((input, expectedCode) in rejectedInputs) {
            assertRejected(bridge, parser, input, expectedCode, profile)
          }
          // 에러 경로가 파서 핸들을 오염시키지 않았는지 — 정상 입력 재시도.
          bridge.parseAst(parser, inputs[0])
        } finally {
          bridge.freeParser(parser)
        }
      }
    }
  }

  private fun normalize(shortString: String): String =
    shortString.replace(Regex(", nodeId=\\d+, start=\\d+, end=\\d+"), "")

  private fun assertRejected(
    bridge: GeneratedAstNativeBridge,
    parser: MemorySegment,
    input: String,
    expectedCode: Int,
    profile: String,
  ) {
    val e = assertThrows<GeneratedAstParseException> {
      bridge.parseAst(parser, input)
    }
    println("FFI[$profile]  rejected ${if (input.isEmpty()) "(empty)" else input} => code=${e.code}")
    assertEquals(expectedCode, e.code, "[$profile] wrong error code for input: $input")
  }

  private fun cargoBuild(rustDir: Path, outDir: Path, release: Boolean) {
    val args = buildList {
      addAll(listOf("cargo", "build", "--features", "ffi"))
      if (release) add("--release")
    }
    val cargoLog = outDir.resolve("cargo-build${if (release) "-release" else ""}.log").toFile()
    val proc = ProcessBuilder(args)
      .directory(rustDir.toFile())
      .redirectOutput(cargoLog)
      .redirectErrorStream(true)
      .start()
    val exit = proc.waitFor()
    check(exit == 0) { "cargo build (release=$release) failed (exit=$exit):\n${cargoLog.readText().takeLast(4000)}" }
  }

  private fun findDylib(rustDir: Path, profile: String): Path {
    val base = rustDir.resolve("target").resolve(profile)
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
