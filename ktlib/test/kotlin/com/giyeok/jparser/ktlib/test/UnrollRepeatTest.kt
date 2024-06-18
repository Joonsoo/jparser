package com.giyeok.jparser.ktlib.test

import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.ktlib.getSequenceElems
import com.giyeok.jparser.ktlib.hasSingleTrue
import com.giyeok.jparser.ktlib.unrollRepeat0
import com.giyeok.jparser.ktlib.unrollRepeat1
import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserKt
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup2.MilestoneGroupParserDataProtobufConverter
import com.giyeok.jparser.mgroup2.MilestoneGroupParserGen
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UnrollRepeatTest {
  @Test
  fun testRepeat0() {
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar("A = 'a'*", "")

    val analysis = MilestoneGroupParserGen(grammarAnalysis.ngrammar())

    val parser =
      MilestoneGroupParserKt(MilestoneGroupParserDataProtobufConverter.toProto(analysis.parserData()))

    val parsed = parser.parse("aaaa")
    val history = parser.kernelsHistory(parsed)

    assertThat(unrollRepeat0(history, 3, 6, 4, 5, 0, 4))
      .isEqualTo(unrollRepeat0or(history, 3, 6, 4, 5, 0, 4))
  }

  @Test
  fun testRepeat1() {
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar("A = 'a'+", "")

    val analysis = MilestoneGroupParserGen(grammarAnalysis.ngrammar())

    val parser =
      MilestoneGroupParserKt(MilestoneGroupParserDataProtobufConverter.toProto(analysis.parserData()))

    val parsed = parser.parse("aaaa")
    val history = parser.kernelsHistory(parsed)

    assertThat(unrollRepeat1(history, 3, 4, 4, 5, 0, 4))
      .isEqualTo(unrollRepeat1or(history, 3, 4, 4, 5, 0, 4))
  }
}

fun unrollRepeat0or(
  history: List<KernelSet>,
  symbolId: Int,
  itemSymId: Int,
  baseSeq: Int,
  repeatSeq: Int,
  beginGen: Int,
  endGen: Int,
): List<Pair<Int, Int>> {
  val base = history[endGen].findByBeginGenOpt(baseSeq, 0, beginGen)
  val repeat = history[endGen].findByBeginGenOpt(repeatSeq, 2, beginGen)
  check(hasSingleTrue(base != null, repeat != null))
  return if (base != null) {
    listOf()
  } else {
    val seq = getSequenceElems(history, repeatSeq, listOf(symbolId, itemSymId), beginGen, endGen)
    val repeating = seq.first()
    val item = seq[1]
    unrollRepeat0or(
      history,
      symbolId,
      itemSymId,
      baseSeq,
      repeatSeq,
      repeating.first,
      repeating.second,
    ) + item
  }
}

fun unrollRepeat1or(
  history: List<KernelSet>,
  symbolId: Int,
  itemSymId: Int,
  baseSeq: Int,
  repeatSeq: Int,
  beginGen: Int,
  endGen: Int,
): List<Pair<Int, Int>> {
  val base = history[endGen].findByBeginGenOpt(baseSeq, 1, beginGen)
  val repeat = history[endGen].findByBeginGenOpt(repeatSeq, 2, beginGen)
  check(hasSingleTrue(base != null, repeat != null))
  return if (base != null) {
    val baseItem = history[endGen].findByBeginGen(itemSymId, 1, beginGen)
    listOf(baseItem.beginGen to baseItem.endGen)
  } else {
    val seq = getSequenceElems(history, repeatSeq, listOf(symbolId, itemSymId), beginGen, endGen)
    val repeating = seq.first()
    val item = seq[1]
    unrollRepeat1or(
      history,
      symbolId,
      itemSymId,
      baseSeq,
      repeatSeq,
      repeating.first,
      repeating.second,
    ) + item
  }
}
