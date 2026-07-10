package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins the structural "anychar single-char cond symbol" detection that eager EOF
 * resolution folds against (`ParserDataPlain.eofCondSymbols`).
 *
 * The detection criterion is sound only because of a generator invariant
 * (`Mgroup3ParserGenerator.kt:410-412`: `replaceAndProgresses` = milestones
 * progressed *to their own end*). If a refactor of the generator ever broke that
 * invariant, the criterion could start over- or under-detecting; this test is the
 * canary. It fixes:
 *   - mulang (a real grammar with an `EOF = !.` rule): exactly one symbol, and
 *     that symbol is genuinely AnyChar (single term action over all characters).
 *   - structure-only grammars (no `!.`): the empty set.
 *
 * mulang's `data.pb` is a gitignored generated fixture (~110MB); when it is
 * absent (fresh clone that has not run `runMgroup3FixtureGen`) the mulang case is
 * skipped, but the committed structure fixtures always run.
 */
class EofCondSymbolsDetectionTest {
  private fun nativeRoot(): File {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
      val candidate = File(dir, "mgroup3-native")
      if (candidate.isDirectory) return candidate
      dir = dir.parentFile
    }
    error("could not locate mgroup3-native/ from cwd=${System.getProperty("user.dir")}")
  }

  private fun loadPlain(relPath: String): ParserDataPlain? {
    val pb = File(nativeRoot(), relPath)
    if (!pb.exists()) return null
    return ParserDataPlain(Mgroup3ParserData.parseFrom(pb.readBytes()))
  }

  /** True iff sym's starter group is a single term action over *all* characters —
   *  the structural signature of AnyChar (the `.` in `EOF = !.`). */
  private fun isAnyCharSymbol(plain: ParserDataPlain, sym: Int): Boolean {
    val info = plain.pathRoots[sym] ?: return false
    val actions = plain.termActions[info.milestoneGroupId] ?: return false
    if (actions.size != 1) return false
    val tg = actions[0].termGroup
    if (!tg.hasAllCharsExcluding()) return false
    val excl = tg.allCharsExcluding.excluding
    return excl.unicodeCategoriesCount == 0 && excl.chars.isEmpty()
  }

  @Test
  fun mulangHasExactlyOneAnycharEofSymbol() {
    val plain = loadPlain("tests/fixtures/parser_generated/mulang/data.pb")
    assumeTrue(
      plain != null,
      "mulang generated fixture missing — run `bibix4 runMgroup3FixtureGen` to populate",
    )
    plain!!

    // Fixed set (measured against the committed mulang grammar): exactly sym20.
    assertEquals(
      setOf(20), plain.eofCondSymbols,
      "mulang eofCondSymbols drifted from the pinned {20}",
    )

    // Cross-check that the detected id is *really* an AnyChar symbol (not just an
    // id coincidence): its data must carry the all-characters term group.
    assertTrue(
      isAnyCharSymbol(plain, 20),
      "sym20 was detected as an eof cond symbol but is not structurally AnyChar",
    )
  }

  @Test
  fun structureOnlyGrammarsDetectNoEofSymbols() {
    // Committed structure fixtures contain no `!.` / EOF rule → empty set.
    for (case in listOf("simple_sequence", "repeat0", "nested_repeat", "choice", "optional")) {
      val plain = loadPlain("tests/fixtures/parser/$case/data.pb")
      assertTrue(plain != null, "committed fixture $case/data.pb missing")
      assertEquals(
        emptySet<Int>(), plain!!.eofCondSymbols,
        "$case unexpectedly detected eof cond symbols",
      )
    }
  }
}
