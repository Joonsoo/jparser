package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.generated.Ast
import com.giyeok.jparser.mgroup3.generated.AstProtoBinding
import com.giyeok.jparser.mgroup3.generated.ast.ParseDelta as PParseDelta
import com.giyeok.jparser.mgroup3.generated.ast.ParseResult as PParseResult
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.lang.foreign.MemorySegment
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText

/**
 * AST-delta differential gate (Stage 3a — Kotlin consumer). Drives fuzzing-style
 * edit sequences through the generated delta session FFI
 * (`mgroup3_gen_delta_session_*` / `mgroup3_gen_session_edit_delta`) and checks
 * that the JVM consumer `AstProtoBinding.DeltaSession` — accumulating `applyDelta`
 * (kind=1, ParseDelta) and `initFromFull` (kind=0, ParseResult fallback) — stays
 * structurally AND span-identical to a fresh full parse of the edited text.
 *
 * The span check is exact: both the delta-accumulated tree and the full-parse tree
 * are re-encoded through the SAME `AstProtoBinding.toProto` (canonical children-first
 * ids + start/end on every node), so byte equality ⟺ identical structure + spans.
 *
 * Reuses the codegen + cargo cache dir of [Mgroup3GenAstFfiTest] (same crate). cargo
 * build needed, so env-gated. Run: `bibix4 runMgroup3GenDeltaTest`.
 */
@EnabledIfEnvironmentVariable(named = "MGROUP3_GEN_FFI", matches = "1")
class Mgroup3GenDeltaFfiTest {
  // Edit actions applied against the CURRENT document (positions are recomputed
  // per step). Inserts land inside a non-keyword identifier (validity-preserving,
  // so they parse + typically splice -> delta). One whole-document replacement
  // forces the non-splice fallback (kind=0).
  private sealed interface Action
  private data class InsertInIdent(val fraction: Double) : Action
  private data class ReplaceAll(val newDoc: String) : Action

  private val baseDoc = "module M { foo = Bar(int x) | Baz\n  attributes (int z) }"

  private val actions: List<Action> = listOf(
    InsertInIdent(0.50),
    InsertInIdent(0.20),
    InsertInIdent(0.80),
    InsertInIdent(0.35),
    InsertInIdent(0.65),
    // whole-document swap to a different valid doc — expected non-splice fallback.
    ReplaceAll("module N { a = Pfoo(str s, int u) | Q\n  attributes (int t) }"),
    InsertInIdent(0.40),
    InsertInIdent(0.60),
    InsertInIdent(0.25),
    InsertInIdent(0.75),
    InsertInIdent(0.90),
  )

  private val keywords = setOf("module", "attributes")

  @Test
  fun deltaAccumulationMatchesFullParse() {
    val cdg = Path("examples/metalang3/resources/asdl/grammar.cdg").readText()
    val processed = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "AsdlGrammar")
    check(processed.errors().isClear) { "grammar analysis errors: ${processed.errors().errors()}" }

    // Share codegen + cargo cache with Mgroup3GenAstFfiTest (identical crate).
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

    var totalDelta = 0
    var totalFallback = 0
    for (release in listOf(false, true)) {
      val profile = if (release) "release" else "debug"
      cargoBuild(rustDir, outDir, release)
      val dylib = findDylib(rustDir, profile)

      GeneratedAstNativeBridge(dylib).use { bridge ->
        val parser = bridge.newParserFromFile(parserDataPath)
        val session = bridge.sessionDeltaNew(parser)
        try {
          val binding = AstProtoBinding.DeltaSession()

          // baseline: full parse installs version 1.
          val baseBytes = bridge.sessionDeltaParseFull(session, baseDoc)
          binding.initFromFull(PParseResult.parseFrom(baseBytes))
          assertEquals(1, binding.version, "[$profile] baseline version")
          verifyAgainstFull(bridge, parser, binding, baseDoc, profile, "baseline")

          var doc = baseDoc
          var deltaCount = 0
          var fallbackCount = 0
          for ((i, action) in actions.withIndex()) {
            val edit = computeEdit(doc, action) ?: continue
            val (pos, oldLen, newText) = edit
            val newDoc = doc.substring(0, pos) + newText + doc.substring(pos + oldLen)

            val r = bridge.sessionEditDelta(session, pos.toLong(), oldLen.toLong(), newText)
            val expectedVersion = binding.version + 1
            if (r.isDelta) {
              binding.applyDelta(PParseDelta.parseFrom(r.bytes))
              deltaCount++
            } else {
              binding.initFromFull(PParseResult.parseFrom(r.bytes))
              fallbackCount++
            }
            assertEquals(expectedVersion, binding.version, "[$profile] version advance edit#$i")
            doc = newDoc
            verifyAgainstFull(bridge, parser, binding, doc, profile, "edit#$i kind=${if (r.isDelta) "delta" else "full"}")
          }

          println("DELTA[$profile]  edits=${deltaCount + fallbackCount} delta=$deltaCount fallback=$fallbackCount finalVersion=${binding.version}")
          assertTrue(deltaCount > 0) { "[$profile] no delta (kind=1) edits exercised — path untested" }
          assertTrue(fallbackCount > 0) { "[$profile] no fallback (kind=0) edits exercised — path untested" }
          totalDelta += deltaCount
          totalFallback += fallbackCount
        } finally {
          bridge.sessionDeltaDestroy(session)
          bridge.freeParser(parser)
        }
      }
    }
    assertTrue(totalDelta > 0 && totalFallback > 0) {
      "both delta and fallback paths must be exercised (delta=$totalDelta fallback=$totalFallback)"
    }
  }

  /** delta-accumulated tree == fresh full parse of `doc` (structure + spans). */
  private fun verifyAgainstFull(
    bridge: GeneratedAstNativeBridge,
    parser: MemorySegment,
    binding: AstProtoBinding.DeltaSession,
    doc: String,
    profile: String,
    at: String,
  ) {
    val fullTree = AstProtoBinding.fromProtoBytes(bridge.parseAst(parser, doc))
    val deltaTree = binding.root
    // structure (readable diff; nodeId/start/end normalized out).
    assertEquals(
      normalize(fullTree.toShortString()),
      normalize(deltaTree.toShortString()),
      "[$profile] $at: structure mismatch for doc=${doc.replace("\n", "\\n")}",
    )
    // structure + spans, exact: re-encode both through the same canonical encoder.
    assertArrayEquals(
      AstProtoBinding.toProtoBytes(fullTree),
      AstProtoBinding.toProtoBytes(deltaTree),
      "[$profile] $at: structure/span mismatch for doc=${doc.replace("\n", "\\n")}",
    )
  }

  private fun normalize(shortString: String): String =
    shortString.replace(Regex(", nodeId=\\d+, start=\\d+, end=\\d+"), "")

  /** (pos, oldLen, newText) in CHAR offsets (== code points; asdl docs are ASCII). */
  private fun computeEdit(doc: String, action: Action): Triple<Int, Int, String>? = when (action) {
    is ReplaceAll -> Triple(0, doc.length, action.newDoc)
    is InsertInIdent -> {
      val from = (doc.length * action.fraction).toInt()
      val span = findIdent(doc, from) ?: findIdent(doc, 0)
      span?.let { (s, e) -> Triple((s + 1).coerceAtMost(e), 0, "q") }
    }
  }

  /** First non-keyword identifier span at/after `from`, else null. */
  private fun findIdent(doc: String, from: Int): Pair<Int, Int>? {
    val n = doc.length
    var i = from.coerceIn(0, if (n == 0) 0 else n - 1)
    while (i < n) {
      while (i < n && !isIdentChar(doc[i])) i++
      if (i >= n) return null
      var s = i
      while (s > 0 && isIdentChar(doc[s - 1])) s--
      var e = i
      while (e < n && isIdentChar(doc[e])) e++
      val word = doc.substring(s, e)
      if ((doc[s].isLetter() || doc[s] == '_') && word !in keywords) return s to e
      i = e
    }
    return null
  }

  private fun isIdentChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

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
