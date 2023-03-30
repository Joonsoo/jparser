package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone2.{AcceptConditionTemplate, AlwaysTemplate, AndTemplate, LongestTemplate, LookaheadIsTemplate, LookaheadNotTemplate, MilestoneParser2ProtobufConverter, NeverTemplate, OnlyIfTemplate, OrTemplate, UnlessTemplate}
import org.scalatest.flatspec.AnyFlatSpec
import MilestoneParser2ProtobufConverter.termGroupOrdering

class ProtoMarshallingTests extends AnyFlatSpec {
  def assertEquals(g1: NGrammar, g2: NGrammar): Unit = {
    assert(g1.nsymbols == g2.nsymbols)
    assert(g1.nsequences == g2.nsequences)
    assert(g1.startSymbol == g2.startSymbol)
  }

  def assertEquals(action1: EdgeAction, action2: EdgeAction): Unit = {
    assert(action1.appendingMilestoneGroups.sorted == action2.appendingMilestoneGroups.sorted)
    assert(action1.startNodeProgress == action2.startNodeProgress)
    assert(action1.lookaheadRequiringSymbols == action2.lookaheadRequiringSymbols)
    assert(action1.tasksSummary == action2.tasksSummary)
  }

  def assertEquals(action1: TermAction, action2: TermAction): Unit = {
    assert(action1.appendingMilestoneGroups.sorted == action2.appendingMilestoneGroups.sorted)
    assert(action1.startNodeProgress.sortBy(_._1) == action2.startNodeProgress.sortBy(_._1))
    assert(action1.lookaheadRequiringSymbols == action2.lookaheadRequiringSymbols)
    assert(action1.tasksSummary == action2.tasksSummary)
    assert(action1.pendedAcceptConditionKernels.keySet == action2.pendedAcceptConditionKernels.keySet)
    action1.pendedAcceptConditionKernels.keys.foreach { pendedBase =>
      val pended1 = action1.pendedAcceptConditionKernels(pendedBase)
      val pended2 = action2.pendedAcceptConditionKernels(pendedBase)
      assert(pended1._1.sorted == pended2._1.sorted)
      assert(pended1._2 == pended2._2)
    }
  }

  def assertEquals(data1: MilestoneGroupParserData, data2: MilestoneGroupParserData): Unit = {
    assertEquals(data1.grammar, data2.grammar)
    assert(data1.startGroupId == data2.startGroupId)
    assert(data1.initialTasksSummary == data2.initialTasksSummary)
    assert(data1.termActions.keySet == data2.termActions.keySet)
    data1.termActions.keys.foreach { tip =>
      val termActions1 = data1.termActions(tip).sortBy(_._1)
      val termActions2 = data2.termActions(tip).sortBy(_._1)
      termActions1.zip(termActions2).foreach { case (pair1, pair2) =>
        assert(pair1._1 == pair2._1)
        assertEquals(pair1._2, pair2._2)
      }
    }
    assert(data1.tipEdgeProgressActions.keySet == data2.tipEdgeProgressActions.keySet)
    data1.tipEdgeProgressActions.keys.foreach { edge =>
      assertEquals(data1.tipEdgeProgressActions(edge), data2.tipEdgeProgressActions(edge))
    }
    assert(data1.tipEdgeRequiredSymbols == data2.tipEdgeRequiredSymbols)
    assert(data1.midEdgeProgressActions.keySet == data2.midEdgeProgressActions.keySet)
    data1.midEdgeProgressActions.keys.foreach { edge =>
      assertEquals(data1.midEdgeProgressActions(edge), data2.midEdgeProgressActions(edge))
    }
    assert(data1.midEdgeRequiredSymbols == data2.midEdgeRequiredSymbols)
  }

  "bibix-simple grammar" should "work" in {
    val grammar = new String(getClass.getResourceAsStream("/bibix-simple.cdg").readAllBytes())

    val analysis = MetaLanguage3.analyzeGrammar(grammar)
    val parserData = new MilestoneGroupParserGen(analysis.ngrammar).parserData()

    val proto = MilestoneGroupParserDataProtobufConverter.toProto(parserData)
    val unmarshalled = MilestoneGroupParserDataProtobufConverter.fromProto(proto)

    assertEquals(parserData, unmarshalled)
  }

  "bibix grammar" should "work" in {
    val grammar = new String(getClass.getResourceAsStream("/bibix2/grammar.cdg").readAllBytes())

    val analysis = MetaLanguage3.analyzeGrammar(grammar)
    val parserData = new MilestoneGroupParserGen(analysis.ngrammar).parserData()

    val proto = MilestoneGroupParserDataProtobufConverter.toProto(parserData)
    val unmarshalled = MilestoneGroupParserDataProtobufConverter.fromProto(proto)

    assertEquals(parserData, unmarshalled)
  }

  "accept condition marshalling" should "work" in {
    def check(condition: AcceptConditionTemplate): Unit = {
      val proto = MilestoneParser2ProtobufConverter.toProto(condition)
      val unmarshalled = MilestoneParser2ProtobufConverter.fromProto(proto)
      assert(condition == unmarshalled)
    }

    check(AlwaysTemplate)
    check(NeverTemplate)
    check(AndTemplate(List(LookaheadIsTemplate(123, true), OnlyIfTemplate(456, true))))
    check(OrTemplate(List(LookaheadIsTemplate(123, true), OnlyIfTemplate(456, true))))
    check(LookaheadIsTemplate(123, true))
    check(LookaheadIsTemplate(123, false))
    check(LookaheadNotTemplate(123, true))
    check(LookaheadNotTemplate(123, false))
    check(LongestTemplate(234, true))
    check(LongestTemplate(234, false))
    check(OnlyIfTemplate(345, true))
    check(OnlyIfTemplate(345, false))
    check(UnlessTemplate(456, true))
    check(UnlessTemplate(456, false))
  }
}
