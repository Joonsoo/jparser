package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.Kernel
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Test
import java.io.File

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
  private data class Case(val name: String, val grammar: String, val inputs: List<Pair<String, Boolean>>)

  private val corpus: List<Case> = listOf(
    Case("single_char", "Grammar = 'a'", listOf(
      "a" to true, "b" to false, "" to false, "aa" to false,
    )),
    Case("char_range", "Grammar = '0-9'", listOf(
      "0" to true, "5" to true, "9" to true, "a" to false, "" to false,
    )),
    Case("escaped_char", "Grammar = '+\\-'", listOf(
      "+" to true, "-" to true, "*" to false,
    )),
    Case("simple_sequence", "Grammar = 'a' 'b' 'c'", listOf(
      "abc" to true, "ab" to false, "abcd" to false, "a" to false, "" to false,
    )),
    Case("longer_sequence", "Grammar = 'a' 'b' 'c' 'd' 'e'", listOf(
      "abcde" to true, "abcd" to false, "abcdef" to false,
    )),
    Case("choice", "Grammar = 'a' | 'b'", listOf(
      "a" to true, "b" to true, "c" to false, "ab" to false,
    )),
    Case("multi_choice", "Grammar = 'a' | 'b' | 'c' | 'd'", listOf(
      "a" to true, "b" to true, "c" to true, "d" to true, "e" to false,
    )),
    Case("repeat0", "Grammar = 'a'*", listOf(
      "" to true, "a" to true, "aa" to true, "aaaaa" to true, "ab" to false,
    )),
    Case("repeat1", "Grammar = 'a'+", listOf(
      "" to false, "a" to true, "aa" to true, "aaaaa" to true,
    )),
    Case("optional", "Grammar = 'a'?", listOf(
      "" to true, "a" to true, "aa" to false,
    )),
    Case("sequence_with_repeat", "Grammar = 'a' 'b'* 'c'", listOf(
      "ac" to true, "abc" to true, "abbbc" to true, "ab" to false, "abca" to false,
    )),
    Case("nested_choice", "Grammar = ('a' | 'b') ('c' | 'd')", listOf(
      "ac" to true, "ad" to true, "bc" to true, "bd" to true,
      "ab" to false, "cd" to false, "" to false,
    )),
    Case("nested_repeat", "Grammar = ('a' 'b')+", listOf(
      "ab" to true, "abab" to true, "ababab" to true,
      "" to false, "a" to false, "aba" to false,
    )),
  )

  @Test
  fun generate() {
    val outDir = resolveOutputDir()
    outDir.mkdirs()
    var totalInputs = 0
    for (case in corpus) {
      val caseDir = File(outDir, case.name)
      caseDir.mkdirs()
      val parser = makeParser(case.grammar)
      File(caseDir, "data.pb").writeBytes(parser.data.toByteArray())
      File(caseDir, "grammar.txt").writeText(case.grammar)
      val inputsDir = File(caseDir, "inputs")
      inputsDir.mkdirs()
      for ((idx, pair) in case.inputs.withIndex()) {
        val (input, expectedAccept) = pair
        val slug = slug(input)
        val inputDir = File(inputsDir, "${idx.toString().padStart(2, '0')}_$slug")
        inputDir.mkdirs()
        File(inputDir, "input.txt").writeText(input)
        val golden = StringBuilder()
        try {
          val ctx = parser.parse(input)
          val accepted = parser.isAccepted(ctx)
          if (accepted != expectedAccept) {
            error("case=${case.name} input=${input}: parser disagrees with expected (parser=$accepted, expected=$expectedAccept)")
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
            error("case=${case.name} input=${input}: parser threw $e but expected accept")
          }
          golden.append("REJECTED\n")
        }
        File(inputDir, "golden.txt").writeText(golden.toString())
        totalInputs += 1
      }
    }
    println("ParserFixtureGenTest wrote ${corpus.size} cases / $totalInputs inputs to $outDir")
  }

  private fun makeParser(cdg: String, startName: String = "Grammar"): Mgroup3Parser {
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

  private fun slug(input: String): String {
    if (input.isEmpty()) return "empty"
    val safe = input.replace(Regex("[^A-Za-z0-9]"), "_")
    return if (safe.length <= 16) safe else safe.substring(0, 16)
  }

  private fun resolveOutputDir(): File {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
      val candidate = File(dir, "mgroup3-native")
      if (candidate.isDirectory) {
        return File(candidate, "tests/fixtures/parser")
      }
      dir = dir.parentFile
    }
    error("could not locate mgroup3-native/ directory from cwd=${System.getProperty("user.dir")}")
  }
}
