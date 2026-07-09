package com.giyeok.jparser.mgroup4

import com.giyeok.jparser.mgroup3.Mgroup3Parser
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import java.io.File

/**
 * A4 정식 시간 측정 러너 (별도 JVM 프로세스용 — JIT 순서 편향 제거).
 *
 * 한 JVM = 한 (target, n) 설정. interleave 단일-JVM 방식(Mgroup4DifferentialTest 의
 * es5Json2WallClockSignal)이 아니라, 매 설정을 fresh JVM 으로 돌린다. 실행 인자는
 * 전부 env var 로 (bibix action 이 런타임 인자를 못 받으므로 dumpMgroup4Classpath 로
 * classpath 만 내보내고 스크립트가 java 를 직접 spawn).
 *
 * env:
 *   MG4_BENCH_TARGET   = json2 | jquery | underscore | chain_boundaries | ccgen
 *   MG4_INTERIOR_N     = n (Mgroup4Parser 가 이미 이 env 를 읽어 전역 오버라이드)
 *   MG4_BENCH_ENGINE   = mg4 (기본) | mg3  (mg3 = 회귀 기준: mgroup3 파서 직접)
 *   MG4_BENCH_WARMUP   = 웜업 파스 횟수 (기본 2)
 *   MG4_BENCH_RUNS     = 측정 파스 횟수 (기본 5, 중앙값)
 *   MG4_MERGE_PROFILE  = set 이면 병합-패스 세부 타이머 출력 (측정과 분리 실행 권장)
 *   MG4_SHAPE_STATS    = set 이면 realized ratio 출력
 *
 * 파스 시간은 순수 parse() 만 (parserdata 로드/warmup 제외). phase timing 은 마지막
 * 측정 run 에서만 켜서 (nanoTime 오버헤드가 실측을 오염 안 하도록) 자기시간 % 를 뽑는다.
 */
object Mgroup4Bench {
  private const val ES5_PB =
    "/private/tmp/claude-501/-Users-joonsoo-Documents-workspace-jparser/" +
      "24b37725-2f65-49f2-9454-d72679e67d3f/scratchpad/mgroup4-phase0/es5-mg3.pb"
  private const val MULANG_PB = "mgroup3-native/tests/fixtures/parser_generated/mulang/data.pb"

  private fun median(xs: List<Double>): Double = xs.sorted()[xs.size / 2]

  private data class Target(val pbPath: String, val corpusPath: String)

  private fun resolveTarget(name: String): Target = when (name) {
    "json2" -> Target(ES5_PB, "es5-corpus/json2.js")
    "jquery" -> Target(ES5_PB, "es5-corpus/jquery-1.12.4.js")
    "underscore" -> Target(ES5_PB, "es5-corpus/underscore-1.8.3.js")
    "chain_boundaries" -> Target(MULANG_PB, "../mulang/examples/chain_boundaries.mu")
    "ccgen" -> Target(MULANG_PB, "../mulang/examples/ccgen.mu")
    else -> error("unknown MG4_BENCH_TARGET=$name")
  }

  @JvmStatic
  fun main(args: Array<String>) {
    val targetName = System.getenv("MG4_BENCH_TARGET") ?: "json2"
    val n = (System.getenv("MG4_INTERIOR_N") ?: "1").toInt()
    val engine = System.getenv("MG4_BENCH_ENGINE") ?: "mg4"
    val warmup = (System.getenv("MG4_BENCH_WARMUP") ?: "2").toInt()
    val runs = (System.getenv("MG4_BENCH_RUNS") ?: "5").toInt()

    val target = resolveTarget(targetName)
    val pb = File(target.pbPath)
    check(pb.exists()) { "parserdata not found: ${pb.absolutePath}" }
    val corpus = File(target.corpusPath)
    check(corpus.exists()) { "corpus not found: ${corpus.absolutePath}" }
    val src = corpus.readText()

    val loadT0 = System.nanoTime()
    val data = Mgroup3ParserData.parseFrom(pb.readBytes())
    val loadMs = (System.nanoTime() - loadT0) / 1e6

    if (engine == "mg3") {
      // 회귀 기준 — mgroup3 파서 직접 (n 무의미). realized ratio 없음.
      val p = Mgroup3Parser(data)
      repeat(warmup) { check(p.isAccepted(p.parse(src))) }
      val gcBetween = System.getenv("MG4_BENCH_NO_GC") == null
      val times = (1..runs).map {
        if (gcBetween) { System.gc(); Thread.sleep(50) }
        val t0 = System.nanoTime()
        val ctx = p.parse(src)
        val ok = p.isAccepted(ctx)
        val ms = (System.nanoTime() - t0) / 1e6
        check(ok); ms
      }
      println(
        "[MG4-BENCH] target=$targetName engine=mg3 n=- chars=${src.length} " +
          "load=${"%.0f".format(loadMs)}ms parse_median=${"%.1f".format(median(times))}ms " +
          "parse_min=${"%.1f".format(times.min())}ms runs=$times"
      )
      return
    }

    val shapeStats = mg4ShapeStatsEnabled
    val mergeProfile = mg4MergeProfileEnabled

    // 웜업 — parserdata 재사용, 매 파스 새 파서 (파서는 termActionCache 등 상태를 갖지만
    // parse() 는 매번 fresh ctx 라 반복 안전; 새 파서로 cold cache 편향도 방지).
    repeat(warmup) {
      val p = Mgroup4Parser(data, interiorGroupMaxDepth = n)
      check(p.isAccepted(p.parse(src)))
    }

    // 측정 — 순수 parse() (isAccepted 는 시간 밖). 매 run fresh 파서.
    // 매 run 전 System.gc() 로 heap 을 청소해 "이전 run 이 채운 old-gen 때문에 이번
    // run 중간에 GC 가 터지는" 누적 편향을 제거한다 (n=1/n=6 이 live-set 크기가 달라
    // GC 스케줄이 run 순서에 따라 코인플립하던 문제). 이렇게 하면 측정값이 순수 compute
    // 에 수렴 — GC 는 각 run 시작 전 (타이밍 밖) 에만 돈다. gcBetween=false 로 끌 수 있음.
    val gcBetween = System.getenv("MG4_BENCH_NO_GC") == null
    val times = ArrayList<Double>(runs)
    var lastShapeRatio = 0.0
    for (r in 1..runs) {
      if (gcBetween) { System.gc(); Thread.sleep(50) }
      val p = Mgroup4Parser(data, interiorGroupMaxDepth = n)
      if (shapeStats) p.resetMg4Stats()
      val t0 = System.nanoTime()
      val ctx = p.parse(src)
      val ms = (System.nanoTime() - t0) / 1e6
      check(p.isAccepted(ctx))
      times.add(ms)
      if (shapeStats && r == runs) {
        val base = p.mg4BaseShapeSum.toDouble()
        val merged = p.mg4MergedShapeSum.toDouble()
        lastShapeRatio = if (merged > 0) base / merged else 1.0
      }
    }

    val med = median(times)
    val sb = StringBuilder(
      "[MG4-BENCH] target=$targetName engine=mg4 n=$n chars=${src.length} " +
        "load=${"%.0f".format(loadMs)}ms parse_median=${"%.1f".format(med)}ms " +
        "parse_min=${"%.1f".format(times.min())}ms runs=${times.map { "%.0f".format(it) }}"
    )
    if (shapeStats) sb.append(" realizedRatio=${"%.3f".format(lastShapeRatio)}")
    println(sb.toString())

    // phase/merge 자기시간 — 별도 프로파일 run (nanoTime 오버헤드로 실측 오염 방지).
    // 측정값(위)과 분리해서, 병합 패스가 전체의 몇 % 인지 귀속만 뽑는다.
    if (mergeProfile) {
      val p = Mgroup4Parser(data, interiorGroupMaxDepth = n)
      p.enablePhaseTiming()
      p.resetMergeTimers()
      val ctx = p.parse(src)
      check(p.isAccepted(ctx))
      println("[MG4-BENCH-PROFILE] target=$targetName n=$n ${p.reportPhaseTimers()}")
      println("[MG4-BENCH-PROFILE] target=$targetName n=$n ${p.reportMergeTimers()}")
    }
  }
}
