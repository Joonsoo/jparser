package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.NGrammar
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.math.ceil
import kotlin.math.max

// ES5.1 코퍼스 파싱 비용 원인 진단 (H1 idiom성 스파이크 vs H2 직역 고유 고원).
// timing 은 측정하지 않는다 — step 구동으로 매 generation 의 live path shape 수 p_g 만
// 정확 계측 (부하 무관). 파서 코어/생성기는 수정하지 않음 — Es5CdgTest.parser() 캐시 재사용.
//
// 측정:
//   (1) per-generation 프로파일: p_g (total / main / watcher), 요약 통계 + 스파이크 지배도.
//   (2) 주석/트리비아 vs 코드 구간 분리 (렉시컬 프리스캔): 문자 점유율 vs 비용 점유율.
//   (3) 고-p generation 의 조성: tip milestone/chain/watcher root 히스토그램 (논터미널 이름).
//   (4) json2 주석 헤더 경계 전후 p_g 추이.
//
// 실행 (포그라운드): bibix4 runEs5DiagProfile   (ES5_DIAG=1, 큰 힙)
@EnabledIfEnvironmentVariable(named = "ES5_DIAG", matches = "1")
class Es5DiagProfileTest {
  private val nameCache = HashMap<Int, String>()
  private fun symName(grammar: NGrammar, id: Int): String = nameCache.getOrPut(id) {
    try {
      grammar.symbolOf(id).symbol().toShortString().let { if (it.length > 48) it.take(48) + "…" else it }
    } catch (e: Throwable) {
      "sym$id"
    }
  }

  // ---- 렉시컬 프리스캔: 각 문자를 (공백|주석)=트리비아 vs 코드 로 분류 ----
  // 문자열 리터럴('/"), // 주석, /* */ 주석 인식. regex 리터럴은 근사(regex 위치 휴리스틱).
  // 한계 (보고서에 명시):
  //   - regex vs division 은 직전 유의 토큰으로 근사; '}' 는 regex 문맥, ')'/']' 는 division 으로 처리.
  //   - 근사 오분류는 code 로 흡수될 뿐(regex 본문도 코드) 트리비아 점유율을 부풀리지 않는다.
  //   - ES5 이므로 템플릿 리터럴 없음.
  private fun classifyTrivia(src: String): BooleanArray {
    val n = src.length
    val triv = BooleanArray(n)
    var i = 0
    var prevSig = '\u0000'   // 직전 유의(코드) 문자 — regex 위치 판별용
    var prevWord = ""        // 직전 식별자 단어 — 키워드 뒤 regex 판별용
    val kwRegex = setOf(
      "return", "typeof", "instanceof", "in", "of", "new", "delete",
      "void", "do", "else", "case", "yield", "throw"
    )
    fun isWs(c: Char) = c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000C' || c == '\u000B'
    fun isIdentPart(c: Char) = c.isLetterOrDigit() || c == '_' || c == '$'
    fun regexPos(): Boolean {
      if (prevWord.isNotEmpty() && prevWord in kwRegex) return true
      return when (prevSig) {
        '\u0000', '(', ',', ';', '=', '{', '}', ':', '[',
        '!', '&', '|', '?', '+', '-', '*', '/', '<', '>', '^', '%', '~' -> true
        else -> false
      }
    }
    while (i < n) {
      val c = src[i]
      if (isWs(c)) { triv[i] = true; i++; continue }
      // // 줄 주석
      if (c == '/' && i + 1 < n && src[i + 1] == '/') {
        while (i < n && src[i] != '\n') { triv[i] = true; i++ }
        continue // \n 은 다음 루프에서 ws 로 표시. prevSig/prevWord 유지(주석=공백류).
      }
      // /* */ 블록 주석
      if (c == '/' && i + 1 < n && src[i + 1] == '*') {
        triv[i] = true; triv[i + 1] = true; i += 2
        while (i < n) {
          if (src[i] == '*' && i + 1 < n && src[i + 1] == '/') { triv[i] = true; triv[i + 1] = true; i += 2; break }
          triv[i] = true; i++
        }
        continue // prevSig/prevWord 유지
      }
      // 문자열 리터럴 (코드)
      if (c == '"' || c == '\'') {
        val q = c
        i++ // 여는 따옴표 (triv 기본 false)
        while (i < n) {
          val d = src[i]
          if (d == '\\') { i += 2; continue } // 이스케이프/라인컨티뉴에이션
          if (d == q) { i++; break }
          if (d == '\n' || d == '\r') break // 미종결 (정상 소스엔 없음)
          i++
        }
        prevSig = q; prevWord = "" // 문자열=값 → 뒤따르는 '/'는 division
        continue
      }
      // regex 리터럴 (근사)
      if (c == '/' && regexPos()) {
        i++ // 여는 '/'
        var inClass = false
        while (i < n) {
          val d = src[i]
          if (d == '\\') { i += 2; continue }
          if (d == '\n' || d == '\r') break
          if (d == '[') { inClass = true; i++; continue }
          if (d == ']') { inClass = false; i++; continue }
          if (d == '/' && !inClass) { i++; break }
          i++
        }
        while (i < n && isIdentPart(src[i])) i++ // flags
        prevSig = 'a'; prevWord = "" // regex=값 → division 문맥
        continue
      }
      // 일반 코드
      if (isIdentPart(c)) {
        val start = i
        while (i < n && isIdentPart(src[i])) i++
        prevWord = src.substring(start, i)
        prevSig = src[i - 1]
        continue
      } else {
        prevSig = c; prevWord = ""; i++
      }
    }
    return triv
  }

  private fun pct(sortedAsc: IntArray, p: Double): Int {
    if (sortedAsc.isEmpty()) return 0
    val rank = ceil(p / 100.0 * sortedAsc.size).toInt().coerceIn(1, sortedAsc.size)
    return sortedAsc[rank - 1]
  }

  // tip 심볼 이름 기준의 대략적 문법 가족 분류 (근사 — 원시 히스토그램이 근거).
  private fun classifyName(name: String): String = when {
    name.contains("RegularExpression") -> "regex/div"
    name.contains("Comment") -> "comment"
    name == "WS" || name == "WSNoNL" -> "WS"
    name.contains("Arguments") || name.contains("ArgumentList") -> "call-args"
    name.contains("Identifier") || name.contains("ReservedWord") -> "ident/kw"
    name.contains("StringLiteral") || name.contains("NumericLiteral") || name.contains("Literal") -> "literal"
    name.contains("Expression") -> "expr-cascade"
    name.contains("Statement") || name.contains("Block") || name.contains("Clause") ||
      name.contains("SourceElement") || name.contains("FunctionBody") || name.contains("Program") -> "stmt/struct"
    else -> "other"
  }

  // tip milestone 심볼 기준 가족. tip 이 other 면 chain 을 올라가며 첫 매칭 사용.
  private fun familyOf(grammar: NGrammar, shape: PathShape, root: PathRoot): String {
    var mp = shape.milestonePath ?: return classifyName(symName(grammar, root.symbolId))
    var depth = 0
    while (depth < 60) {
      val fam = classifyName(symName(grammar, mp.milestone.symbolId))
      if (fam != "other") return fam
      mp = mp.parent ?: break
      depth++
    }
    return "other"
  }

  private fun chainSig(grammar: NGrammar, shape: PathShape): String {
    val parts = ArrayList<String>()
    var mp = shape.milestonePath
    var depth = 0
    while (mp != null && depth < 40) {
      parts.add("${symName(grammar, mp.milestone.symbolId)}:${mp.milestone.pointer}")
      mp = mp.parent; depth++
    }
    parts.reverse()
    return parts.joinToString(">") + "|tip${shape.tipGroupId}"
  }

  @Test
  fun profile() {
    val dir = Path.of("es5-corpus")
    // 작은 → 큰 순 (json2 는 주석 헤더가 큰 대조군).
    val order = listOf("json2.js", "underscore-1.8.3.js", "jquery-1.12.4.js")
    val (grammar, parser) = Es5CdgTest.parser()

    println("=== ES5.1 코퍼스 비용 진단 프로파일 (ES5_DIAG) ===")
    println("start symbol id=${grammar.startSymbol()}")
    println()

    for (name in order) {
      val f = dir.resolve(name)
      if (!Files.exists(f)) { println("SKIP $name (파일 없음: ${f.toAbsolutePath()})"); continue }
      val src = f.readText()
      profileFile(grammar, parser, name, src)
      System.gc()
    }
  }

  private fun profileFile(grammar: NGrammar, parser: Mgroup3Parser, name: String, src: String) {
    val n = src.length
    println("========================================================================")
    println("### $name (${n} chars) ###")

    // ---------- Pass 1: per-generation p_g ----------
    val pTotal = IntArray(n)
    val pMain = IntArray(n)
    run {
      var ctx = parser.initCtx()
      for (idx in 0 until n) {
        ctx = parser.parseStep(ctx, src[idx], idx + 1 == n)
        val total = ctx.paths.values.sumOf { it.size }
        val main = ctx.paths[ctx.mainRoot]?.size ?: 0
        pTotal[idx] = total   // 배열 index idx == gen (idx+1) 을 소비한 문자 idx 에 대응
        pMain[idx] = main
      }
      val accepted = parser.isAccepted(ctx)
      println("accepted=$accepted")
    }
    System.gc()

    // ----- 요약 통계 (total / main / watcher) -----
    var sumTotal = 0L; var sumMain = 0L; var sumWatcher = 0L
    for (idx in 0 until n) { sumTotal += pTotal[idx]; sumMain += pMain[idx]; sumWatcher += (pTotal[idx] - pMain[idx]) }
    val sortedTotal = pTotal.copyOf().also { it.sort() }
    val sortedMain = pMain.copyOf().also { it.sort() }
    val meanTotal = sumTotal.toDouble() / n
    val meanMain = sumMain.toDouble() / n
    val meanWatcher = sumWatcher.toDouble() / n

    println("--- (1) per-generation 프로파일 요약 ---")
    println("           mean   median    p90    p99     max     sum(p)      비고")
    println("  total  %7.1f %7d %6d %6d %7d %11d".format(
      meanTotal, pct(sortedTotal, 50.0), pct(sortedTotal, 90.0), pct(sortedTotal, 99.0), sortedTotal.last(), sumTotal))
    println("  main   %7.1f %7d %6d %6d %7d %11d".format(
      meanMain, pct(sortedMain, 50.0), pct(sortedMain, 90.0), pct(sortedMain, 99.0), sortedMain.last(), sumMain))
    println("  watch  %7.1f %35s %11d  (mean watcher share=%.1f%%)".format(
      meanWatcher, "", sumWatcher, 100.0 * sumWatcher / max(1L, sumTotal)))

    // ----- 스파이크 지배도: 상위 1%/5% generation 이 sum(p) 에서 차지하는 비율 -----
    val desc = pTotal.sortedDescending()
    fun topShare(frac: Double): Triple<Int, Long, Double> {
      val k = ceil(frac * n).toInt().coerceIn(1, n)
      var s = 0L; for (j in 0 until k) s += desc[j]
      return Triple(k, s, s.toDouble() / max(1L, sumTotal))
    }
    val (k1, s1, sh1) = topShare(0.01)
    val (k5, s5, sh5) = topShare(0.05)
    println("  스파이크 지배: 상위1%%(%d gen) → sum 의 %.1f%%,  상위5%%(%d gen) → sum 의 %.1f%%"
      .format(k1, 100.0 * sh1, k5, 100.0 * sh5))
    // 균등분포 기준선: 상위 1% 는 정확히 1%, 상위 5% 는 5% 를 차지. 초과분이 스파이크 신호.
    println("  (균등이면 각각 1.0%%/5.0%%. 초과 배수: 1%%→%.1fx, 5%%→%.1fx)"
      .format(100.0 * sh1 / 1.0, 100.0 * sh5 / 5.0))

    // ---------- (2) 주석/트리비아 vs 코드 구간 ----------
    val triv = classifyTrivia(src)
    var trivChars = 0L; var codeChars = 0L; var trivCost = 0L; var codeCost = 0L
    for (idx in 0 until n) {
      if (triv[idx]) { trivChars++; trivCost += pTotal[idx] } else { codeChars++; codeCost += pTotal[idx] }
    }
    val meanTrivP = trivCost.toDouble() / max(1L, trivChars)
    val meanCodeP = codeCost.toDouble() / max(1L, codeChars)
    println("--- (2) 주석+공백(트리비아) vs 코드 구간 ---")
    println("            문자수   문자점유%   sum(p)      비용점유%   mean p_g")
    println("  트리비아 %8d   %6.1f   %11d   %6.1f   %8.1f".format(
      trivChars, 100.0 * trivChars / n, trivCost, 100.0 * trivCost / max(1L, sumTotal), meanTrivP))
    println("  코드     %8d   %6.1f   %11d   %6.1f   %8.1f".format(
      codeChars, 100.0 * codeChars / n, codeCost, 100.0 * codeCost / max(1L, sumTotal), meanCodeP))
    val trivCostShare = 100.0 * trivCost / max(1L, sumTotal)
    val trivCharShare = 100.0 * trivChars / n
    println("  → 트리비아 비용점유(%.1f%%) %s 문자점유(%.1f%%)  [%s]".format(
      trivCostShare, if (trivCostShare > trivCharShare) ">" else "≤", trivCharShare,
      if (trivCostShare > trivCharShare + 3.0) "H1 신호(주석발 비용 초과)"
      else if (trivCostShare < trivCharShare - 3.0) "H2 신호(코드발 비용 초과)" else "중립"))

    // ---------- (3) 고-p generation 조성 ----------
    val K = 20
    val topIdx = (0 until n).sortedByDescending { pTotal[it] }.take(K).toHashSet()
    val topIdxSorted = topIdx.sorted()
    val maxTop = topIdxSorted.last()
    val tipHist = HashMap<String, Int>()
    val famHist = HashMap<String, Int>()
    val chainHist = HashMap<String, Int>()
    val watcherRootHist = HashMap<String, Int>()
    var mainShapesAgg = 0L; var watcherShapesAgg = 0L
    run {
      var ctx = parser.initCtx()
      for (idx in 0..maxTop) {
        ctx = parser.parseStep(ctx, src[idx], idx + 1 == n)
        if (idx in topIdx) {
          val mainRoot = ctx.mainRoot
          for ((root, pm) in ctx.paths) {
            val isMain = root == mainRoot
            for ((shape, _) in pm) {
              if (isMain) {
                mainShapesAgg++
                val tip = shape.milestonePath
                val tipKey = if (tip != null)
                  "${symName(grammar, tip.milestone.symbolId)}:${tip.milestone.pointer}"
                else "<root ${symName(grammar, root.symbolId)}>"
                tipHist.merge(tipKey, 1, Int::plus)
                famHist.merge(familyOf(grammar, shape, root), 1, Int::plus)
                chainHist.merge(chainSig(grammar, shape), 1, Int::plus)
              } else {
                watcherShapesAgg++
                watcherRootHist.merge(symName(grammar, root.symbolId), 1, Int::plus)
              }
            }
          }
        }
      }
    }
    System.gc()

    println("--- (3) 고-p 조성 (상위 $K generation 집계; main shapes=$mainShapesAgg, watcher shapes=$watcherShapesAgg) ---")
    println("  top gens (gen: pTotal/pMain): " + topIdxSorted.sortedByDescending { pTotal[it] }.take(K)
      .joinToString(", ") { "${it + 1}:${pTotal[it]}/${pMain[it]}" })
    println("  [tip milestone (symbol:pointer) 상위 — main paths]")
    tipHist.entries.sortedByDescending { it.value }.take(18).forEach { (k, v) ->
      println("    %6d (%4.1f%%)  %s".format(v, 100.0 * v / max(1L, mainShapesAgg), k))
    }
    println("  [가족 분류 (tip 기준, 근사) — main paths]")
    famHist.entries.sortedByDescending { it.value }.forEach { (k, v) ->
      println("    %6d (%4.1f%%)  %s".format(v, 100.0 * v / max(1L, mainShapesAgg), k))
    }
    println("  [chain signature 상위 — main paths]")
    chainHist.entries.sortedByDescending { it.value }.take(8).forEach { (k, v) ->
      println("    x%-6d %s".format(v, if (k.length > 160) k.take(160) + "…" else k))
    }
    if (watcherShapesAgg > 0) {
      println("  [watcher cond-root 심볼 상위]")
      watcherRootHist.entries.sortedByDescending { it.value }.take(8).forEach { (k, v) ->
        println("    %6d (%4.1f%%)  %s".format(v, 100.0 * v / max(1L, watcherShapesAgg), k))
      }
    } else {
      println("  [watcher: 고-p gen 에서 watcher shape 0]")
    }

    // ---------- (4) json2 주석 헤더 경계 전후 추이 ----------
    if (name.startsWith("json2")) {
      val firstCode = triv.indexOfFirst { !it }
      println("--- (4) json2 주석 헤더 경계 추이 ---")
      println("  첫 코드 문자 index=$firstCode (line ${src.substring(0, firstCode.coerceAtLeast(0)).count { it == '\n' } + 1})")
      // 헤더 내부(트리비아) vs 헤더 직후 코드 구간 평균 p
      var hSum = 0L; var hCnt = 0L
      for (idx in 0 until firstCode) { hSum += pTotal[idx]; hCnt++ }
      println("  헤더 구간 mean p=%.1f (max=%d)".format(hSum.toDouble() / max(1L, hCnt),
        (0 until firstCode).maxOfOrNull { pTotal[it] } ?: 0))
      // 경계 전후 다운샘플 트레이스
      val lo = max(0, firstCode - 400)
      val hi = minOf(n - 1, firstCode + 400)
      val stride = max(1, (hi - lo) / 40)
      val sb = StringBuilder("  트레이스 [gen:pTotal] (경계=<<):\n    ")
      var col = 0
      var idx = lo
      while (idx <= hi) {
        val mark = if (idx == firstCode || (idx < firstCode && idx + stride > firstCode)) "<<" else ""
        sb.append("${idx + 1}:${pTotal[idx]}$mark ")
        if (++col % 8 == 0) sb.append("\n    ")
        idx += stride
      }
      println(sb.toString())
      // 주석 라인 경계(//... 끝의 개행)에서 스파이크가 나는지: 헤더 내 각 개행 직전/직후 p 비교
      var boundaryHi = 0; var interiorHi = 0; var boundaryCnt = 0; var interiorCnt = 0
      var bSum = 0L; var iSum = 0L
      for (idx in 0 until firstCode) {
        val atBoundary = src[idx] == '\n' || (idx + 1 < firstCode && src[idx + 1] == '\n')
        if (atBoundary) { bSum += pTotal[idx]; boundaryCnt++; if (pTotal[idx] > boundaryHi) boundaryHi = pTotal[idx] }
        else { iSum += pTotal[idx]; interiorCnt++; if (pTotal[idx] > interiorHi) interiorHi = pTotal[idx] }
      }
      println("  헤더 주석-라인 경계(개행 인접) mean p=%.1f (max=%d, n=%d)".format(
        bSum.toDouble() / max(1, boundaryCnt), boundaryHi, boundaryCnt))
      println("  헤더 주석 내부(비경계)       mean p=%.1f (max=%d, n=%d)".format(
        iSum.toDouble() / max(1, interiorCnt), interiorHi, interiorCnt))
    }
    println()
  }
}
