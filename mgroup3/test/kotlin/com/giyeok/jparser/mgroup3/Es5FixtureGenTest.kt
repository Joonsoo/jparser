package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText

// ES5.1 문법(examples/metalang3/resources/es5/grammar.cdg, start=Program)의
// Mgroup3ParserData(data.pb)를 생성해 Rust 런타임(mgroup3-native)이 로드하게 한다.
// ParserFixtureGenTest 와 동일한 생성 경로(analyzeGrammar → Mgroup3ParserGenerator
// → data.toByteArray)를 재사용하되, mulang 등 다른 대형 픽스처는 재생성하지 않는다.
// 산출: mgroup3-native/tests/fixtures/parser_generated/es5/data.pb (+ grammar.txt)
// 실행: bibix4 runEs5FixtureGen  (ES5_FIXTURE=1)
@EnabledIfEnvironmentVariable(named = "ES5_FIXTURE", matches = "1")
class Es5FixtureGenTest {
  @Test
  fun generate() {
    val cdgPath = Path.of("examples/metalang3/resources/es5/grammar.cdg")
    val cdg = cdgPath.readText()
    val t0 = System.nanoTime()
    val grammar = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Program").ngrammar()
    val data = Mgroup3ParserGenerator(grammar).generate()
    val genMs = (System.nanoTime() - t0) / 1e6

    val nativeRoot = resolveNativeRoot()
    val outDir = File(nativeRoot, "tests/fixtures/parser_generated/es5")
    outDir.mkdirs()
    val pb = File(outDir, "data.pb")
    pb.writeBytes(data.toByteArray())
    File(outDir, "grammar.txt").writeText(cdg)
    println("Es5FixtureGenTest: wrote ${pb.absolutePath} (${pb.length()} bytes, gen=%.0fms)".format(genMs))
  }

  private fun resolveNativeRoot(): File {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
      val candidate = File(dir, "mgroup3-native")
      if (candidate.isDirectory) return candidate
      dir = dir.parentFile
    }
    error("could not locate mgroup3-native/ from cwd=${System.getProperty("user.dir")}")
  }
}
