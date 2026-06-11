package com.giyeok.jparser.nparser2

import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser._
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor, ParseTreeConstructor2}
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.immutable.ListMap

// NaiveParser(v1)와 NaiveParser2 사이에서 발견된 두 가지 의미 차이의 회귀 테스트.
// (2026-06-12, 코드 검증으로 확인. 기준 의미는 paper의 Match 의미론)
//
// D1. 공유 nullable sequence의 늦은 파생:
//   두 nonterminal이 같은 RHS sequence를 공유하고(같은 sequence id), 그 원소가
//   모두 nullable이어서 같은 세대에서 두 단계 이상 progress된 뒤에 다른 파생자가
//   같은 sequence를 파생하면, v1의 updatedNodesMap은 한 단계 전 노드만 기록하므로
//   체인 끝의 final 노드를 놓친다. NaiveParser2의 addNext(체인 전체 순회)가 올바른
//   동작. v1은 D1 문법에서 여전히 잘못된 결과를 낼 수 있음(알려진 버그, 미수정).
//
// D2. gen 0 조건 평가:
//   gen 0에서 progress된 nullable join/except/lookahead가 만드는 endGen=0 조건
//   (OnlyIf/Unless/Exists/NotExists)은 gen 0에 평가되어야 한다. 평가가 gen 1로
//   밀리면 Unless/OnlyIf는 무조건 Always/Never로 퇴화하고 Exists/NotExists는
//   gen 0에서 끝난 매치를 놓친다. NaiveParser2.initialParsingHistoryContext가
//   v1의 initialContext처럼 gen 0 평가/트리밍을 수행하도록 수정됨.
//
// 참고: mgroup2는 이 코너 케이스들에서 자체 문제가 있어 교차 검증 기준이 될 수 없다.
//   - 빈 입력을 수용하지 못함 (D1 문법, input "")
//   - nullable join/except에서 파서 생성기가 MatchError로 실패
//     (milestone2 ParserGenBase2.conditionToTemplateForTaskSummary가 OnlyIf/Unless 미처리)
//   - gen 0에서 성립한 lookahead를 놓침 (D2-lookahead에서 수정 전 NaiveParser2와 동일한 오동작)
class NaiveParser2CornerCaseTests extends AnyFlatSpec {
  private def grammarOf(name0: String, rules0: ListMap[String, List[Symbols.Symbol]], start: String): NGrammar = {
    val g = new Grammar {
      override val name: String = name0
      override val rules: RuleMap = rules0
      override val startSymbol: Symbols.Nonterminal = Symbols.Nonterminal(start)
    }
    NGrammar.fromGrammar(g)
  }

  private def naive1Accepts(grammar: NGrammar, source: String): Boolean = {
    val parser = new NaiveParser(grammar)
    val inputs = Inputs.fromString(source)
    parser.parse(source) match {
      case Left(ctx) =>
        new ParseTreeConstructor(ParseForestFunc)(grammar)(inputs, ctx.history, ctx.conditionFinal)
          .reconstruct().exists(_.trees.nonEmpty)
      case Right(_) => false
    }
  }

  private def naive2Accepts(grammar: NGrammar, source: String): Boolean = {
    val parser = new NaiveParser2(grammar)
    val inputs = Inputs.fromString(source)
    parser.parse(inputs) match {
      case Right(ctx) =>
        parser.parseTreeReconstructor2(ParseForestFunc, ctx).reconstruct().exists(_.trees.nonEmpty)
      case Left(_) => false
    }
  }

  // D1: N1과 N2가 같은 sequence (A B)를 공유. A, B 모두 nullable.
  private val d1 = grammarOf("D1", ListMap(
    "S" -> List(seq(n("N1"), c('x')), n("N2")),
    "N1" -> List(seq(n("A"), n("B"))),
    "N2" -> List(seq(n("A"), n("B"))),
    "A" -> List(empty, c('a')),
    "B" -> List(empty, c('b'))
  ), "S")

  // D1 변형: alternative 순서 반대 (derive 순서 비결정성 대비)
  private val d1r = grammarOf("D1r", ListMap(
    "S" -> List(n("N2"), seq(n("N1"), c('x'))),
    "N1" -> List(seq(n("A"), n("B"))),
    "N2" -> List(seq(n("A"), n("B"))),
    "A" -> List(empty, c('a')),
    "B" -> List(empty, c('b'))
  ), "S")

  "NaiveParser2" should "accept nullable shared-sequence matches regardless of derive order (D1)" in {
    assert(naive2Accepts(d1, ""))
    assert(naive2Accepts(d1, "x"))
    assert(naive2Accepts(d1r, ""))
    assert(naive2Accepts(d1r, "x"))
  }

  // D2-join: A & B가 gen 0에서 ε 매치 (A, B 모두 nullable)
  private val d2join = grammarOf("D2join", ListMap(
    "S" -> List(seq(Symbols.Join(n("A"), n("B")), c('x'))),
    "A" -> List(empty, c('a')),
    "B" -> List(empty, c('b'))
  ), "S")

  // D2-lookahead: ^A가 gen 0에서 성립 (A nullable)
  private val d2la = grammarOf("D2la", ListMap(
    "S" -> List(seq(lookahead_is(n("A")), c('x'))),
    "A" -> List(empty, c('a'))
  ), "S")

  // D2-except: A - B에서 B가 gen 0의 ε 매치를 차단해야 함
  private val d2except = grammarOf("D2except", ListMap(
    "S" -> List(seq(Symbols.Except(n("A"), n("B")), c('x'))),
    "A" -> List(empty, c('a')),
    "B" -> List(empty)
  ), "S")

  it should "evaluate gen-0 accept conditions in the initial context (D2)" in {
    assert(naive2Accepts(d2join, "x"))
    assert(naive2Accepts(d2la, "x"))
    assert(!naive2Accepts(d2except, "x"))
  }

  it should "agree with NaiveParser(v1) on the D2 cases" in {
    assert(naive1Accepts(d2join, "x"))
    assert(naive1Accepts(d2la, "x"))
    assert(!naive1Accepts(d2except, "x"))
  }
}
