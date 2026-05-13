package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.Kernel
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Captures parser fixtures consumed by the Rust port at
 * `mgroup3-native/tests/parser_diff.rs`.
 *
 * For each `Case`, dumps:
 *   - `data.pb`     — raw Mgroup3ParserData proto bytes
 *   - `grammar.txt` — the grammar source (for debugging diffs)
 *   - per input: `inputs/{idx}_{slug}/input.txt`, `golden.txt`
 *
 * `golden.txt` is either `REJECTED\n` or `ACCEPTED\n<kernels-history>` where
 * each gen block lists kernels sorted by (symbolId, pointer, beginGen, endGen).
 *
 * Run once when the corpus changes. The output is committed so cargo test
 * works without rerunning bibix4.
 */
class ParserFixtureGenTest {
  /** Inline input text. */
  private data class InlineInput(val text: String, val slug: String, val expectedAccept: Boolean)
  /** Input read from a file. `slug` defaults to the file name without extension. */
  private data class FileInput(val path: String, val expectedAccept: Boolean, val slug: String? = null) {
    val effectiveSlug: String get() = slug ?: File(path).nameWithoutExtension
  }

  private data class Case(
    val name: String,
    /** Inline grammar source, or null if [grammarPath] is set. */
    val grammar: String? = null,
    /** Path to grammar `.cdg`, or null if [grammar] is set. */
    val grammarPath: String? = null,
    val startName: String = "Grammar",
    val inlineInputs: List<InlineInput> = emptyList(),
    val fileInputs: List<FileInput> = emptyList(),
    /**
     * If true, the generated fixture goes under `tests/fixtures/parser_generated/`
     * (gitignored — regenerate locally). Otherwise it goes under
     * `tests/fixtures/parser/` (committed to git).
     *
     * Use this for cases that produce large data.pb / golden.txt files
     * (typically anything driven by a real grammar file rather than a tiny
     * inline grammar).
     */
    val generatedOnly: Boolean = false,
  )

  private val corpus: List<Case> = listOf(
    Case("single_char", grammar = "Grammar = 'a'", inlineInputs = listOf(
      InlineInput("a", "a", true), InlineInput("b", "b", false),
      InlineInput("", "empty", false), InlineInput("aa", "aa", false),
    )),
    Case("char_range", grammar = "Grammar = '0-9'", inlineInputs = listOf(
      InlineInput("0", "0", true), InlineInput("5", "5", true), InlineInput("9", "9", true),
      InlineInput("a", "a", false), InlineInput("", "empty", false),
    )),
    Case("escaped_char", grammar = "Grammar = '+\\-'", inlineInputs = listOf(
      InlineInput("+", "plus", true), InlineInput("-", "minus", true), InlineInput("*", "star", false),
    )),
    Case("simple_sequence", grammar = "Grammar = 'a' 'b' 'c'", inlineInputs = listOf(
      InlineInput("abc", "abc", true), InlineInput("ab", "ab", false),
      InlineInput("abcd", "abcd", false), InlineInput("a", "a", false), InlineInput("", "empty", false),
    )),
    Case("longer_sequence", grammar = "Grammar = 'a' 'b' 'c' 'd' 'e'", inlineInputs = listOf(
      InlineInput("abcde", "abcde", true), InlineInput("abcd", "abcd", false),
      InlineInput("abcdef", "abcdef", false),
    )),
    Case("choice", grammar = "Grammar = 'a' | 'b'", inlineInputs = listOf(
      InlineInput("a", "a", true), InlineInput("b", "b", true),
      InlineInput("c", "c", false), InlineInput("ab", "ab", false),
    )),
    Case("multi_choice", grammar = "Grammar = 'a' | 'b' | 'c' | 'd'", inlineInputs = listOf(
      InlineInput("a", "a", true), InlineInput("b", "b", true),
      InlineInput("c", "c", true), InlineInput("d", "d", true),
      InlineInput("e", "e", false),
    )),
    Case("repeat0", grammar = "Grammar = 'a'*", inlineInputs = listOf(
      InlineInput("", "empty", true), InlineInput("a", "a", true), InlineInput("aa", "aa", true),
      InlineInput("aaaaa", "aaaaa", true), InlineInput("ab", "ab", false),
    )),
    Case("repeat1", grammar = "Grammar = 'a'+", inlineInputs = listOf(
      InlineInput("", "empty", false), InlineInput("a", "a", true),
      InlineInput("aa", "aa", true), InlineInput("aaaaa", "aaaaa", true),
    )),
    Case("optional", grammar = "Grammar = 'a'?", inlineInputs = listOf(
      InlineInput("", "empty", true), InlineInput("a", "a", true), InlineInput("aa", "aa", false),
    )),
    Case("sequence_with_repeat", grammar = "Grammar = 'a' 'b'* 'c'", inlineInputs = listOf(
      InlineInput("ac", "ac", true), InlineInput("abc", "abc", true),
      InlineInput("abbbc", "abbbc", true), InlineInput("ab", "ab", false),
      InlineInput("abca", "abca", false),
    )),
    Case("nested_choice", grammar = "Grammar = ('a' | 'b') ('c' | 'd')", inlineInputs = listOf(
      InlineInput("ac", "ac", true), InlineInput("ad", "ad", true),
      InlineInput("bc", "bc", true), InlineInput("bd", "bd", true),
      InlineInput("ab", "ab", false), InlineInput("cd", "cd", false), InlineInput("", "empty", false),
    )),
    Case("nested_repeat", grammar = "Grammar = ('a' 'b')+", inlineInputs = listOf(
      InlineInput("ab", "ab", true), InlineInput("abab", "abab", true),
      InlineInput("ababab", "ababab", true),
      InlineInput("", "empty", false), InlineInput("a", "a", false), InlineInput("aba", "aba", false),
    )),
    // mulang.cdg + accepted example files. class.mu and ccgen.mu are excluded
    // because they hit a known grammar issue and an OOM respectively. The
    // generated artifacts are large (data.pb ~41MB, golden.txt totals ~50MB)
    // so this case goes to the gitignored generated/ directory; regenerate
    // locally via `bibix4 runMgroup3FixtureGen`.
    Case(
      name = "mulang",
      grammarPath = "../mulang/grammar/mulang.cdg",
      startName = "CompileUnit",
      generatedOnly = true,
      fileInputs = listOf(
        FileInput("../mulang/examples/string_lit.mu", true),
        FileInput("../mulang/examples/annotation_defs.mu", true),
        FileInput("../mulang/examples/muast.mu", true),
        FileInput("../mulang/examples/def_sigs.mu", true),
        FileInput("../mulang/examples/oneof.mu", true),
        FileInput("../mulang/examples/class_init.mu", true),
        FileInput("../mulang/examples/match.mu", true),
        FileInput("../mulang/examples/try_let.mu", true),
        FileInput("../mulang/examples/annotation_processor.mu", true),
        FileInput("../mulang/examples/exprs.mu", true),
        FileInput("../mulang/examples/if_let.mu", true),
        FileInput("../mulang/examples/rbln_examples.mu", true),
        FileInput("../mulang/examples/cpp_ast.mu", true),
      ),
    ),
  )

  @Test
  fun generate() {
    val nativeRoot = resolveNativeRoot()
    val committedDir = File(nativeRoot, "tests/fixtures/parser")
    val generatedDir = File(nativeRoot, "tests/fixtures/parser_generated")
    committedDir.mkdirs()
    generatedDir.mkdirs()
    var totalInputs = 0
    var skippedCases = 0
    for (case in corpus) {
      val grammarSource = resolveGrammarSource(case)
      if (grammarSource == null) {
        println("[skip] case=${case.name}: grammar source missing (path=${case.grammarPath})")
        skippedCases += 1
        continue
      }
      val outDir = if (case.generatedOnly) generatedDir else committedDir
      val caseDir = File(outDir, case.name)
      caseDir.mkdirs()
      val parser = makeParser(grammarSource, case.startName)
      File(caseDir, "data.pb").writeBytes(parser.data.toByteArray())
      File(caseDir, "grammar.txt").writeText(grammarSource)
      val inputsDir = File(caseDir, "inputs")
      inputsDir.mkdirs()

      val allInputs: List<Triple<String, String, Boolean>> =
        case.inlineInputs.map { Triple(it.text, it.slug, it.expectedAccept) } +
          case.fileInputs.mapNotNull { fi ->
            val p = Path(fi.path)
            if (!p.exists()) {
              println("[skip] case=${case.name} file=${fi.path}: not found")
              null
            } else {
              Triple(p.readText(), fi.effectiveSlug, fi.expectedAccept)
            }
          }

      for ((idx, triple) in allInputs.withIndex()) {
        val (input, slug, expectedAccept) = triple
        val inputDir = File(inputsDir, "${idx.toString().padStart(2, '0')}_${sanitize(slug)}")
        inputDir.mkdirs()
        File(inputDir, "input.txt").writeText(input)
        val golden = StringBuilder()
        try {
          val ctx = parser.parse(input)
          val accepted = parser.isAccepted(ctx)
          if (accepted != expectedAccept) {
            error("case=${case.name} input slug=${slug}: parser disagrees with expected (parser=$accepted, expected=$expectedAccept)")
          }
          if (!accepted) {
            golden.append("REJECTED\n")
          } else {
            golden.append("ACCEPTED\n")
            val hist = parser.kernelsHistory(ctx)
            for ((gen, kernelSet) in hist.withIndex()) {
              golden.append("# gen $gen\n")
              val sorted = kernelSet.kernels.sortedWith(kernelCompare())
              for (k in sorted) {
                golden.append("${k.symbolId} ${k.pointer} ${k.beginGen} ${k.endGen}\n")
              }
            }
          }
        } catch (e: ParsingError) {
          if (expectedAccept) {
            error("case=${case.name} input slug=${slug}: parser threw $e but expected accept")
          }
          golden.append("REJECTED\n")
        }
        File(inputDir, "golden.txt").writeText(golden.toString())
        totalInputs += 1
      }
    }
    println("ParserFixtureGenTest wrote ${corpus.size - skippedCases} cases / $totalInputs inputs (committed=$committedDir, generated=$generatedDir, skipped=$skippedCases cases)")
  }

  private fun resolveGrammarSource(case: Case): String? {
    if (case.grammar != null) return case.grammar
    val p = case.grammarPath ?: return null
    val path = Path(p)
    return if (path.exists()) path.readText() else null
  }

  private fun makeParser(cdg: String, startName: String): Mgroup3Parser {
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, startName)
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    return Mgroup3Parser(gen.generate())
  }

  private fun kernelCompare() = Comparator<Kernel> { a, b ->
    var c = a.symbolId.compareTo(b.symbolId); if (c != 0) return@Comparator c
    c = a.pointer.compareTo(b.pointer); if (c != 0) return@Comparator c
    c = a.beginGen.compareTo(b.beginGen); if (c != 0) return@Comparator c
    a.endGen.compareTo(b.endGen)
  }

  private fun sanitize(slug: String): String {
    val safe = slug.replace(Regex("[^A-Za-z0-9]"), "_")
    return if (safe.length <= 24) safe else safe.substring(0, 24)
  }

  private fun resolveNativeRoot(): File {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
      val candidate = File(dir, "mgroup3-native")
      if (candidate.isDirectory) {
        return candidate
      }
      dir = dir.parentFile
    }
    error("could not locate mgroup3-native/ directory from cwd=${System.getProperty("user.dir")}")
  }
}
