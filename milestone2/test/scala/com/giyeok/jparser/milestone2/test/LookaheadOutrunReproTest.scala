package com.giyeok.jparser.milestone2.test

import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone2.{MilestoneParser, MilestoneParserGen}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser2.NaiveParser2
import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc}
import org.scalatest.flatspec.AnyFlatSpec

// bug-B (negative lookahead discharge) 재현 핀. 실행: bibix4 runMilestone2LookaheadReproTest
//
// 최소 재현(mgroup3/docs/watcher_anchor_dedup.md §9.1): "fn();"에서 watcher("fn"@0)는
// gen 2에 완성·소멸하는데, interior kernel [Program,1]의 NotExists 조건은 seq dot이
// 그 kernel을 통과하는 gen 4에야 경로 위에 물질화된다. per-step 관측만 읽는 구현은
// "이번 step finish 없음 + root 비활성 = 공허 참"으로 오수락한다. naive2가 oracle이다
// (차트를 버리지 않으므로 REJECT).
//
// 상태 기록: 11c30887이 이 계보에 넣었던 수정 짝(누적 채널 seenProgressedRootMilestones
// + isLookaheadWatcherRoot 생존 필터)은 mgroup2×mulang 메모리 폭발(12GB+, 2026-07-31)
// 때문에 cab1092e에서 되돌려졌다. 재설계된 수정이 랜딩될 때까지 fn(); 케이스는
// RED(버그 재현)가 의도된 상태이고, 이 테스트가 그 재설계의 완료 판정 기준이다.
// gn(); 케이스(watcher 매치 실패 → NotExists가 정당하게 참)는 언제나 GREEN이어야 한다.
//
// 주의: gen>0에 anchor된 guard(`{fn();}` 류)는 별개 잔여 bug-B-partial(watcher 미시동)라
// 여기서 다루지 않는다 — mgroup3/test의 Mgroup3ParserKnownIssuesTest 참고.
class LookaheadOutrunReproTest extends AnyFlatSpec {
  private val grammarText =
    """Program = !"fn" Expression ';'
      |Expression = "fn" "()" | "gn" "()"
      |""".stripMargin

  private lazy val grammar: NGrammar = MetaLanguage3.analyzeGrammar(grammarText).ngrammar
  private lazy val parserData = new MilestoneParserGen(grammar).parserData()

  private def naive2Accepts(inputs: List[Inputs.Input]): Boolean = {
    val parser = new NaiveParser2(grammar)
    parser.parse(inputs) match {
      case Left(_) => false
      case Right(ctx) =>
        parser.parseTreeReconstructor2(ParseForestFunc, ctx).reconstruct().isDefined
    }
  }

  private def milestone2Accepts(inputs: List[Inputs.Input]): Boolean = {
    val parser = new MilestoneParser(parserData)
    parser.parse(inputs) match {
      case Left(_) => false
      case Right(ctx) =>
        new ParseTreeConstructor2(ParseForestFunc)(grammar)(inputs, parser.kernelsHistory(ctx).map(Kernels))
          .reconstruct().isDefined
    }
  }

  "fn(); with guarded body outrunning the lookahead" should "be REJECTed in agreement with naive2 (bug B pin)" in {
    val inputs = Inputs.fromString("fn();")
    assert(!naive2Accepts(inputs), "naive2 oracle이 fn();를 수락 — 테스트 전제가 깨졌다")
    assert(!milestone2Accepts(inputs),
      "bug B 재현: milestone2가 fn();를 오수락했다 — cab1092e revert 이후 재설계 랜딩까지는 RED가 의도된 상태다")
  }

  "gn(); where the watcher never matches" should "be ACCEPTed by both parsers" in {
    val inputs = Inputs.fromString("gn();")
    assert(naive2Accepts(inputs), "naive2가 gn();를 거부 — 문법 전제가 깨졌다")
    assert(milestone2Accepts(inputs),
      "milestone2가 gn();를 거부 — NotExists의 정상(공허 참) 해소가 깨졌다")
  }
}
