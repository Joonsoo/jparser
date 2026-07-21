package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.NGrammar
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

// 논문 §7/§10 ES5.1 코퍼스 측정: 파일별 wall-clock (warmup 1 + median of 3) +
// peak live state (전체 path shape peak; main root vs watcher/cond root 분리).
// peak 는 부하와 무관한 정확값. wall-clock 은 이 머신이 idle 이 아니라 참고치.
// 파서는 Es5CdgTest.parser() 의 캐시 인스턴스 재사용 (ES5 문법 in-process 생성).
// 실행: bibix4 runEs5CorpusMeasure  (ES5_MEASURE=1, 큰 힙 필요)
@EnabledIfEnvironmentVariable(named = "ES5_MEASURE", matches = "1")
class Es5CorpusMeasureTest {
  private inline fun timeMs(block: () -> Unit): Double {
    val t0 = System.nanoTime()
    block()
    return (System.nanoTime() - t0) / 1e6
  }

  private fun symName(grammar: NGrammar, id: Int): String = try {
    grammar.symbolOf(id).symbol().toShortString().let { if (it.length > 60) it.take(60) + "…" else it }
  } catch (e: Throwable) {
    "sym$id"
  }

  private fun loadAvg(): Double = ManagementFactory.getOperatingSystemMXBean().systemLoadAverage

  @Test
  fun measure() {
    val dir = Path.of("es5-corpus")
    // 작은 파일부터 (메모리 압박 하에서 큰 파일 실패해도 앞 결과는 확보).
    val files = Files.list(dir).filter { it.toString().endsWith(".js") }
      .toList().sortedBy { Files.size(it) }
    check(files.isNotEmpty()) { "no .js files in ${dir.toAbsolutePath()}" }

    val (grammar, parser) = Es5CdgTest.parser()

    println("=== ES5.1 corpus measurement ===")
    println("files: ${files.map { "${it.fileName}(${Files.size(it)}B)" }}")
    println("system load avg (start): %.2f".format(loadAvg()))
    println()

    for (f in files) {
      val src = f.readText()
      System.gc()

      // ---- wall-clock: warmup 1 + median of 3 (clean parse, no per-step instrumentation) ----
      val warm = timeMs { parser.parse(src) }
      System.gc()
      val runs = DoubleArray(3)
      for (i in 0 until 3) {
        runs[i] = timeMs { parser.parse(src) }
        System.gc()
      }
      runs.sort()
      val median = runs[1]

      // ---- peak live state (exact, load-independent): instrumented step drive ----
      var ctx = parser.initCtx()
      var peakTotal = 0
      var peakMain = 0
      var peakWatcher = 0
      var peakGen = -1
      var peakPaths: Map<PathRoot, PathMap>? = null
      var peakMainRoot: PathRoot? = null
      for ((idx, c) in src.withIndex()) {
        ctx = parser.parseStep(ctx, c, idx + 1 == src.length)
        val total = ctx.paths.values.sumOf { it.size }
        if (total > peakTotal) {
          peakTotal = total
          val main = ctx.paths[ctx.mainRoot]?.size ?: 0
          peakMain = main
          peakWatcher = total - main
          peakGen = idx + 1
          peakPaths = ctx.paths
          peakMainRoot = ctx.mainRoot
        }
      }
      val accepted = parser.isAccepted(ctx)

      // peak 지점 source window + line
      val at = peakGen.coerceIn(0, src.length)
      val line = src.substring(0, at).count { it == '\n' } + 1
      val wStart = maxOf(0, at - 90)
      val window = src.substring(wStart, at).replace("\n", "\\n").replace("\t", "\\t")

      // peak 시점 watcher 구성 — cond root symbol 별 shape 수 상위.
      val watcherBySym = HashMap<Int, Int>()
      peakPaths?.forEach { (root, pm) ->
        if (root != peakMainRoot) watcherBySym.merge(root.symbolId, pm.size, Int::plus)
      }
      val topWatchers = watcherBySym.entries.sortedByDescending { it.value }.take(8)
        .joinToString("; ") { (sid, cnt) -> "${symName(grammar, sid)}=$cnt" }
      val watcherRootCount = watcherBySym.size

      println("--- ${f.fileName} (${src.length} chars) ---")
      println("  wall-clock: warmup=%.0fms, runs(sorted)=[%.0f, %.0f, %.0f]ms, median=%.0fms  (loadAvg=%.2f)"
        .format(warm, runs[0], runs[1], runs[2], median, loadAvg()))
      println("  accepted=$accepted")
      println("  peak live shapes=$peakTotal (main=$peakMain, watcher=$peakWatcher) at gen=$peakGen (line $line)")
      println("  peak watcher cond-roots=$watcherRootCount; top: $topWatchers")
      println("  peak source window: …$window⟨HERE⟩")
      println("  CSV ${f.fileName},${src.length},%.1f,%.1f,%.1f,$peakTotal,$peakMain,$peakWatcher,$peakGen,$accepted"
        .format(median, runs[0], runs[2]))
      println()

      ctx = parser.initCtx() // drop history reference
      peakPaths = null
      System.gc()
    }
    println("system load avg (end): %.2f".format(loadAvg()))
  }
}
