package com.giyeok.jparser.milestone2.test

import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone2.{MilestoneParser, MilestoneParserGen}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser2.NaiveParser2
import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc}
import org.scalatest.flatspec.AnyFlatSpec

import java.io.File
import java.nio.file.Path
import scala.reflect.io.Path

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

  // ---- Exists 쌍둥이: Program = ^"fn" Expression ';' (positive lookahead) ----
  //
  // 위 bug B와 같은 "배달(경로 위 물질화 gen 4) vs 관측(watcher 완성 gen 2)" 갭을
  // Exists 방향에서 핀한다. 증거 소실 후 평가는 NotExists에서는 공허 참(오수락),
  // Exists에서는 거짓(오거부)으로 기운다.
  //
  // 실측(2026-07-31): fn(); 케이스는 현재 RED — 늦은 배달이 유일한 배달 채널인 현
  // 구현에서 Exists(fn@0)가 gen 4에야 경로에 물질화되고, gen 2의 완성 관측은 이미
  // 버려진 뒤라 거짓으로 평가되어 오거부한다 (bug B의 쌍대). old_tip_accept_condition
  // (이른 배달) 재설계는 이 케이스까지 GREEN이어야 완료다 — 이른 배달로 gen 2에
  // 해소되는 것과, 늦은 사본 재평가가 해소된 조건을 다시 뒤집지 않는 것 둘 다 필요.
  private val posGrammarText =
    """Program = ^"fn" Expression ';'
      |Expression = "fn" "()" | "gn" "()"
      |""".stripMargin

  private lazy val posGrammar: NGrammar = MetaLanguage3.analyzeGrammar(posGrammarText).ngrammar
  private lazy val posParserData = new MilestoneParserGen(posGrammar).parserData()

  private def posNaive2Accepts(inputs: List[Inputs.Input]): Boolean = {
    val parser = new NaiveParser2(posGrammar)

    parser.parse(inputs) match {
      case Left(_) => false
      case Right(ctx) =>
        parser.parseTreeReconstructor2(ParseForestFunc, ctx).reconstruct().isDefined
    }
  }

  private def posMilestone2Accepts(inputs: List[Inputs.Input]): Boolean = {
    val parser = new MilestoneParser(posParserData)
    parser.parse(inputs) match {
      case Left(_) => false
      case Right(ctx) =>
        new ParseTreeConstructor2(ParseForestFunc)(posGrammar)(inputs, parser.kernelsHistory(ctx).map(Kernels))
          .reconstruct().isDefined
    }
  }

  "fn(); where the positive lookahead is satisfied" should "be ACCEPTed in agreement with naive2 (Exists twin pin)" in {
    val inputs = Inputs.fromString("fn();")
    assert(posNaive2Accepts(inputs), "naive2 oracle이 ^\"fn\" 문법에서 fn();를 거부 — 테스트 전제가 깨졌다")
    assert(posMilestone2Accepts(inputs),
      "milestone2가 fn();를 오거부했다 — 해소된 Exists 조건이 늦은 배달 사본에 의해 뒤집힌 것")
  }

  "gn(); where the positive lookahead fails" should "be REJECTed by both parsers" in {
    val inputs = Inputs.fromString("gn();")
    assert(!posNaive2Accepts(inputs), "naive2가 ^\"fn\" 문법에서 gn();를 수락 — 문법 전제가 깨졌다")
    assert(!posMilestone2Accepts(inputs),
      "milestone2가 gn();를 오수락했다 — Exists 미충족이 경로에 반영되지 않은 것")
  }

  // ---- 혼합 문법: lookahead 종류가 다른 RHS + lookahead 없는 RHS 동거 ----
  //
  // 같은 Expression 서브파스가 서로 다른 gate(^"fn" / !"gn" / 없음)를 가진 세 alternative
  // 아래에서 공유된다. derive-origin 조건의 배달 스코프(창·흐름)와 chain별 disjunction
  // (Or-of-Ands) 분석용: 어느 종결자(';' '!' '.')로 끝나느냐가 어느 alternative가
  // 시도되었는지를 결정하고, gate가 그 alternative의 chain에만 실려야 정답과 일치한다.
  private val mixGrammarText =
    """Program = ^"fn" Expression ';' | !"gn" Expression '!' | Expression '.'
      |Expression = "fn" "()" | "gn" "()"
      |""".stripMargin

  private lazy val mixGrammar: NGrammar = MetaLanguage3.analyzeGrammar(mixGrammarText).ngrammar
  private lazy val mixParserData = new MilestoneParserGen(mixGrammar).parserData()

  private def mixNaive2Accepts(inputs: List[Inputs.Input]): Boolean = {
    val parser = new NaiveParser2(mixGrammar)
    parser.parse(inputs) match {
      case Left(_) => false
      case Right(ctx) =>
        parser.parseTreeReconstructor2(ParseForestFunc, ctx).reconstruct().isDefined
    }
  }

  private def mixMilestone2Accepts(inputs: List[Inputs.Input]): Boolean = {
    val parser = new MilestoneParser(mixParserData)
    parser.parse(inputs) match {
      case Left(_) => false
      case Right(ctx) =>
        new ParseTreeConstructor2(ParseForestFunc)(mixGrammar)(inputs, parser.kernelsHistory(ctx).map(Kernels))
          .reconstruct().isDefined
    }
  }

  "mixed grammar, lookahead-free alternative (.)" should "ACCEPT both fn(). and gn()." in {
    val fnInputs = Inputs.fromString("fn().")
    val gnInputs = Inputs.fromString("gn().")
    assert(mixNaive2Accepts(fnInputs) && mixNaive2Accepts(gnInputs), "naive2가 gate 없는 alternative를 거부 — 문법 전제가 깨졌다")
    assert(mixMilestone2Accepts(fnInputs), "milestone2가 fn().를 거부 — gate 없는 chain이 다른 alternative의 gate에 오염된 것")
    assert(mixMilestone2Accepts(gnInputs), "milestone2가 gn().를 거부 — gate 없는 chain이 다른 alternative의 gate에 오염된 것")
  }

  "mixed grammar, ^fn alternative (;)" should "ACCEPT fn(); and REJECT gn(); in agreement with naive2" in {
    val fnInputs = Inputs.fromString("fn();")
    val gnInputs = Inputs.fromString("gn();")
    assert(mixNaive2Accepts(fnInputs) && !mixNaive2Accepts(gnInputs), "naive2 oracle이 ^fn alternative에서 어긋남 — 문법 전제가 깨졌다")
    assert(!mixMilestone2Accepts(gnInputs), "milestone2가 gn();를 오수락 — Exists 미충족이 chain에 반영되지 않은 것")
    assert(mixMilestone2Accepts(fnInputs),
      "milestone2가 fn();를 오거부 — Exists 늦은 물질화 (단독 ^fn 문법의 Exists twin pin과 같은 원인)")
  }

  "mixed grammar, !gn alternative (!)" should "ACCEPT fn()! and REJECT gn()! in agreement with naive2" in {
    val fnInputs = Inputs.fromString("fn()!")
    val gnInputs = Inputs.fromString("gn()!")
    assert(mixNaive2Accepts(fnInputs) && !mixNaive2Accepts(gnInputs), "naive2 oracle이 !gn alternative에서 어긋남 — 문법 전제가 깨졌다")
    assert(mixMilestone2Accepts(fnInputs), "milestone2가 fn()!를 거부 — NotExists의 정상(공허 참) 해소가 깨졌다")
    assert(!mixMilestone2Accepts(gnInputs),
      "milestone2가 gn()!를 오수락 — NotExists 늦은 물질화 (단독 !fn 문법의 bug B pin과 같은 원인)")
  }
}
