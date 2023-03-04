package com.giyeok.jparser.milestone2

import com.giyeok.jparser.proto.MilestoneParserDataProto.Milestone2ParserData
import org.scalatest.flatspec.AnyFlatSpec

import java.io.{File, FileInputStream}

class DiffTest extends AnyFlatSpec {

  def assertSameParsingAction(a: ParsingAction, b: ParsingAction): Unit = {
    assert(a.appendingMilestones.toSet == b.appendingMilestones.toSet)
    assert(a.startNodeProgressCondition == b.startNodeProgressCondition)
    assert(a.lookaheadRequiringSymbols == b.lookaheadRequiringSymbols)
    assert(a.tasksSummary == b.tasksSummary)
  }

  it should "identical" in {
    val fromOld = MilestoneParser2ProtobufConverter.fromProto(
      Milestone2ParserData.parseFrom(
        new FileInputStream(new File("metalang/ast/resources/cdglang3-parserdata.pb"))))
    val fromNew = MilestoneParser2ProtobufConverter.fromProto(
      Milestone2ParserData.parseFrom(
        new FileInputStream(new File("bbxbuild/objects/abc/resources/cdglang3-parserdata.pb"))))

    assert(fromOld.grammar.nsymbols == fromNew.grammar.nsymbols)
    assert(fromOld.grammar.nsequences == fromNew.grammar.nsequences)
    assert(fromOld.grammar.startSymbol == fromNew.grammar.startSymbol)

    assert(fromOld.initialTasksSummary == fromNew.initialTasksSummary)

    assert(fromOld.termActions.keySet == fromNew.termActions.keySet)
    fromOld.termActions.keySet.foreach { termActionKey =>
      val oldActions = fromOld.termActions(termActionKey).toMap
      val newActions = fromNew.termActions(termActionKey).toMap

      assert(oldActions.keySet == newActions.keySet)
      oldActions.keySet.foreach { termGroup =>
        val oldAction = oldActions(termGroup)
        val newAction = newActions(termGroup)

        assertSameParsingAction(oldAction.parsingAction, newAction.parsingAction)
        assert(oldAction.pendedAcceptConditionKernels == newAction.pendedAcceptConditionKernels)
      }
    }

    assert(fromOld.edgeProgressActions.keySet == fromNew.edgeProgressActions.keySet)
    fromOld.edgeProgressActions.keySet.foreach { edge =>
      val oldAction = fromOld.edgeProgressActions(edge)
      val newAction = fromNew.edgeProgressActions(edge)

      assertSameParsingAction(oldAction.parsingAction, newAction.parsingAction)
      assert(oldAction.requiredSymbols == newAction.requiredSymbols)
    }
  }
}

// 350,5 -> 20,1
// 431,1 -> 20,1
// 431,1에서 /가 나오면 20,1가 append