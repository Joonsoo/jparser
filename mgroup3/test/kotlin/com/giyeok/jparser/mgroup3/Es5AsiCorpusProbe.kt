package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

// ASI 코퍼스 프로브 (Es5CdgTest 와 독립).
//
// 목적: semicolon-free 로 작성된 실제 배포 JS 파일들이
//   (a) BASE ES5.1 문법 (ASI 미인코딩) 에서 REJECT 되는지 — first-failure 위치가 개행
//       문장 경계인지까지 확인 — 그리고 나중에
//   (b) ASI 인코딩 문법에서 ACCEPT 되는지
// 를 같은 드라이버로 확인한다.
//
// 전부 env 로 매개변수화 — 문법/코퍼스 디렉토리/기대 판정을 바꿔가며 재실행:
//   ES5_ASI_PROBE=1              (필수 게이트)
//   ES5_ASI_GRAMMAR=<path>       기본 examples/metalang3/resources/es5/grammar.cdg  (BASE)
//   ES5_ASI_START=<symbol>       기본 Program
//   ES5_ASI_DIR=<dir>            기본 es5-corpus-asi
//   ES5_ASI_EXPECT=reject|accept 기본 reject (BASE 문법 기준)
//   ES5_ASI_REPAIR=1             REJECT 인 파일에 대해 "세미콜론만 넣으면 통과하는가" 검증
//   ES5_ASI_REPAIR_CAP=<n>       repair 삽입 상한 (기본 5000)
//
// 실행: bibix4 runEs5AsiCorpusProbe          (BASE 문법 x es5-corpus-asi, 전부 REJECT 기대)
class Es5AsiCorpusProbe {
  private fun env(name: String, default: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

  private data class Verdict(val accepted: Boolean, val detail: String)

  @EnabledIfEnvironmentVariable(named = "ES5_ASI_PROBE", matches = "1")
  @Test
  fun probe() {
    val grammarPath = env("ES5_ASI_GRAMMAR", "examples/metalang3/resources/es5/grammar.cdg")
    val startSymbol = env("ES5_ASI_START", "Program")
    val dir = Path.of(env("ES5_ASI_DIR", "es5-corpus-asi"))
    val expectAccept = env("ES5_ASI_EXPECT", "reject").lowercase() == "accept"

    println("=== ES5 ASI corpus probe ===")
    println("grammar : $grammarPath  (start=$startSymbol)")
    println("corpus  : ${dir.toAbsolutePath()}")
    println("expect  : ${if (expectAccept) "ACCEPT" else "REJECT"} for every file")
    println()

    val cdg = Path.of(grammarPath).readText()
    val grammar = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, startSymbol).ngrammar()
    val parser = Mgroup3Parser(Mgroup3ParserGenerator(grammar).generate())

    val files = Files.list(dir).filter { it.toString().endsWith(".js") }
      .toList().sortedBy { Files.size(it) }
    check(files.isNotEmpty()) { "no .js files in ${dir.toAbsolutePath()}" }

    val repair = env("ES5_ASI_REPAIR", "0") == "1"
    val repairCap = env("ES5_ASI_REPAIR_CAP", "5000").toInt()
    val repairBudgetMs = env("ES5_ASI_REPAIR_BUDGET_MS", "900000").toLong()

    var mismatches = 0
    for (f in files) {
      val src = f.readText()
      val t0 = System.nanoTime()
      val v = runFile(parser, src)
      val ms = (System.nanoTime() - t0) / 1e6
      val tag = if (v.accepted == expectAccept) "OK " else "!! "
      if (v.accepted != expectAccept) mismatches++
      println("$tag${f.fileName}  ${src.length} chars  %.0fms  -> ${if (v.accepted) "ACCEPT" else "REJECT"}".format(ms))
      if (v.detail.isNotEmpty()) println(v.detail)
      if (repair && !v.accepted) println(repairReport(parser, src, repairCap, repairBudgetMs))
      println()
    }
    assertTrue(mismatches == 0) {
      "$mismatches file(s) disagreed with expected verdict (${if (expectAccept) "ACCEPT" else "REJECT"})"
    }
  }

  private fun runFile(parser: Mgroup3Parser, src: String): Verdict {
    var ctx = parser.initCtx()
    var peak = 0
    try {
      for ((idx, c) in src.withIndex()) {
        ctx = parser.parseStep(ctx, c, idx + 1 == src.length)
        val shapes = ctx.paths.values.sumOf { it.size }
        if (shapes > peak) peak = shapes
        if (shapes > 200000) {
          return Verdict(false, "  ABORT state blowup at offset $idx (shapes=$shapes)\n" + context(src, idx))
        }
      }
    } catch (e: ParsingError) {
      val loc = when (e) {
        is ParsingError.UnexpectedInput -> e.loc
        is ParsingError.UnexpectedEof -> e.loc
      }
      // ParsingError 의 locLine/locCol 은 0-based — 1-based 로도 같이 찍는다.
      val pos1 = "line ${lineOf(src, loc)}, col ${loc - src.lastIndexOf('\n', loc - 1)}"
      val head = when (e) {
        is ParsingError.UnexpectedInput ->
          "  ParsingError.UnexpectedInput at offset ${e.loc} ($pos1; raw 0-based ${e.locLine}/${e.locCol}), actual=${quote(e.actual)}"

        is ParsingError.UnexpectedEof ->
          "  ParsingError.UnexpectedEof at offset ${e.loc} ($pos1; raw 0-based ${e.locLine}/${e.locCol})"
      }
      val expected = e.let {
        when (it) {
          is ParsingError.UnexpectedInput -> it.expected.toString()
          is ParsingError.UnexpectedEof -> it.expected.toString()
        }
      }.let { if (it.length > 220) it.take(220) + "..." else it }
      return Verdict(
        false,
        head + "\n  expected=" + expected + "\n" + context(src, loc) + "\n" + asiDiagnosis(src, loc)
      )
    }
    val ok = parser.isAccepted(ctx)
    if (ok) return Verdict(true, "  peakShapes=$peak")
    // 입력은 전부 소비했지만 시작 심볼이 안 끝남 — EOF 경계 실패 (파일 끝 세미콜론 부재 등).
    return Verdict(
      false,
      "  consumed all ${src.length} chars but start symbol not finished (isAccepted=false); peakShapes=$peak\n" +
        context(src, src.length - 1)
    )
  }

  private fun quote(c: Char): String = when (c) {
    '\n' -> "'\\n'"
    '\r' -> "'\\r'"
    '\t' -> "'\\t'"
    else -> "'$c'"
  }

  // 실패 지점 주변 소스: 직전 줄 + 실패 줄 + caret.
  private fun context(src: String, loc: Int): String {
    val p = loc.coerceIn(0, maxOf(0, src.length - 1))
    val lineStart = src.lastIndexOf('\n', p - 1).let { if (it < 0) 0 else it + 1 }
    val lineEnd = src.indexOf('\n', p).let { if (it < 0) src.length else it }
    val lineNo = src.substring(0, lineStart).count { it == '\n' } + 1
    val prevStart = if (lineStart == 0) 0 else
      src.lastIndexOf('\n', lineStart - 2).let { if (it < 0) 0 else it + 1 }
    val sb = StringBuilder()
    if (prevStart < lineStart) {
      sb.append("  ${lineNo - 1} | ${src.substring(prevStart, lineStart - 1)}\n")
    }
    sb.append("  $lineNo | ${src.substring(lineStart, lineEnd)}\n")
    sb.append("  ${" ".repeat(lineNo.toString().length)} | ${" ".repeat(p - lineStart)}^")
    return sb.toString()
  }

  // ASI 원인 판정: 실패 오프셋과 직전 non-whitespace 문자 사이에 LineTerminator 가 있는가.
  // (주석이 끼면 직전 non-ws 는 '/' 가 되므로 raw gap 도 같이 덤프해 육안 확인.)
  private fun asiDiagnosis(src: String, loc: Int): String {
    val p = loc.coerceIn(0, maxOf(0, src.length - 1))
    var i = p - 1
    while (i >= 0 && (src[i] == ' ' || src[i] == '\t' || src[i] == '\n' || src[i] == '\r')) i--
    val gap = src.substring(i + 1, p)
    val nlInGap = gap.contains('\n')
    val prevChar = if (i >= 0) quote(src[i]) else "<BOF>"
    return "  ASI-check: prev non-WS char=$prevChar, gap=${gap.replace("\n", "\\n").replace("\t", "\\t")}" +
      ", newline between prev token and failure: $nlInGap" +
      ", offending token is '}': ${p < src.length && src[p] == '}'}" +
      "  => ES5.1 7.9.1 rule-1 clause ${clauseOf(src, p)}"
  }

  // ES5.1 7.9.1 rule 1 의 어느 절이 이 위치의 삽입을 정당화하는가.
  //   (a) offending token 이 직전 토큰과 LineTerminator 로 분리됨
  //       — 7.4 에 따라 LineTerminator 를 포함한 MultiLineComment 도 LineTerminator 로 친다
  //   (b) offending token 이 '}'
  //   (eof) 입력 끝 (7.9.1 rule 2)
  // 어느 것도 아니면 "NONE" — 그 삽입은 ASI 로 설명되지 않는다 (= 문법 밖의 이유).
  private fun clauseOf(src: String, pos: Int): String {
    if (pos >= src.length) return "(eof: rule 2)"
    val clauseB = src[pos] == '}'
    val clauseA = lineTerminatorPrecedes(src, pos)
    return when {
      clauseA && clauseB -> "(a)+(b)"
      clauseA -> "(a) newline-separated"
      clauseB -> "(b) offending token '}'"
      else -> "NONE  <-- not explainable by ASI"
    }
  }

  // pos 앞의 whitespace/주석 구간에 LineTerminator 가 있는가.
  // 뒤로 훑으며 WS 를 건너뛰고, '*/' 를 만나면 그 주석 전체를 건너뛰되
  // 주석 본문에 개행이 있으면 7.4 에 따라 LineTerminator 로 간주한다.
  // (LineComment 는 항상 개행으로 끝나므로 WS 스킵 단계에서 자연히 잡힌다.)
  private fun lineTerminatorPrecedes(src: String, pos: Int): Boolean {
    var i = pos - 1
    while (i >= 0) {
      val c = src[i]
      if (c == '\n' || c == '\r') return true
      if (c == ' ' || c == '\t') {
        i--; continue
      }
      // 블록 주석 끝 '*/' 이면 시작 '/*' 까지 되감는다.
      if (c == '/' && i - 1 >= 0 && src[i - 1] == '*') {
        val open = src.lastIndexOf("/*", i - 2)
        if (open < 0) return false
        if (src.substring(open, i + 1).contains('\n')) return true
        i = open - 1
        continue
      }
      return false
    }
    return false
  }

  private fun isIdPart(c: Char): Boolean =
    (c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') || c == '$' || c == '_'

  // 실패한 "문자" 위치 -> 실패한 "토큰"의 시작 위치.
  // 7.9.1 은 offending *token* 앞에 ';' 를 넣는다. 그런데 파서는 문자 단위로 죽으므로
  // 예를 들어 `x\nif (c) ...` 는 'i' 를 in/instanceof 시작으로 받아들인 뒤 'f' 에서 죽는다.
  // 이때 삽입 지점은 'f' 가 아니라 'i' 다. IdentifierPart 런의 시작으로 되감아 그걸 복원한다.
  private fun tokenStart(src: String, loc: Int): Int {
    if (loc >= src.length || !isIdPart(src[loc])) return loc
    var j = loc
    while (j > 0 && isIdPart(src[j - 1])) j--
    return j
  }

  // 원본 + 삽입지점들 -> 수리된 문자열, 그리고 수리본 오프셋 -> 원본 오프셋 매핑.
  private fun applyInserts(src: String, inserts: List<Int>): Pair<String, IntArray> {
    val sb = StringBuilder(src.length + inserts.size)
    val map = IntArray(src.length + inserts.size)
    var k = 0
    for (i in 0..src.length) {
      // 이 위치에 삽입이 있으면 먼저 ';' 를 낸다 (원본 오프셋 i 로 매핑).
      while (k < inserts.size && inserts[k] == i) {
        map[sb.length] = i; sb.append(';'); k++
      }
      if (i < src.length) {
        map[sb.length] = i; sb.append(src[i])
      }
    }
    return Pair(sb.toString(), map)
  }

  // "세미콜론만 넣으면 통과하는가" — BASE 문법이 이 파일을 거부하는 유일한 이유가 ASI 인지의 검증.
  // 매 라운드: 현재 수리본을 처음부터 클린 파싱 -> 실패하면 그 토큰 시작 앞에 ';' 를 하나 더 등록.
  // (클린 재파싱인 이유: ParsingCtx.history 가 세대 간 공유되는 가변 ArrayList 라 되감기가 안전하지 않다.)
  // 끝까지 소비했는데 미완이면 7.9.1 rule 2 로 EOF 에 ';' 를 붙여 재시도.
  // 마지막에 삽입 지점 전부를 7.9.1 rule 1 절 (a)/(b) 로 분류 — NONE 이 하나라도 있으면 ASI 로 설명 불가.
  private fun repairReport(parser: Mgroup3Parser, src: String, cap: Int, budgetMs: Long): String {
    val inserts = sortedSetOf<Int>()
    var bail: String? = null
    var accepted = false
    var eofSemi = false
    var furthest = 0
    var rounds = 0
    val t0 = System.nanoTime()

    while (true) {
      if (inserts.size >= cap) {
        bail = "insertion cap=$cap reached"; break
      }
      val elapsed = (System.nanoTime() - t0) / 1_000_000
      if (elapsed > budgetMs) {
        bail = "time budget ${budgetMs}ms exhausted"; break
      }
      rounds++
      val (cur, map) = applyInserts(src, inserts.toList())
      val failCur: Int = try {
        val ctx = parser.parse(cur)
        if (parser.isAccepted(ctx)) {
          accepted = true; furthest = src.length; break
        }
        // 전부 소비했지만 시작 심볼 미완 -> rule 2.
        eofSemi = true
        accepted = try {
          parser.isAccepted(parser.parse("$cur;"))
        } catch (e: ParsingError) {
          false
        }
        furthest = src.length
        break
      } catch (e: ParsingError) {
        when (e) {
          is ParsingError.UnexpectedInput -> e.loc
          is ParsingError.UnexpectedEof -> e.loc
        }
      }
      val origFail = map[failCur.coerceIn(0, cur.length - 1)]
      if (origFail > furthest) furthest = origFail
      val ins = tokenStart(src, origFail)
      if (!inserts.add(ins)) {
        bail = "no progress: ';' already inserted at original offset $ins (fail at $origFail)"; break
      }
    }

    val sb = StringBuilder()
    sb.append("  --- ASI repair probe ---\n")
    val byClause = LinkedHashMap<String, Int>()
    val illegal = ArrayList<Int>()
    for (o in inserts) {
      val cl = clauseOf(src, o)
      byClause.merge(cl, 1) { a, b -> a + b }
      if (cl.startsWith("NONE")) illegal.add(o)
    }
    sb.append(
      "  rounds=$rounds  semicolons inserted=${inserts.size}${if (eofSemi) " (+1 at EOF, rule 2)" else ""}" +
        "  reached offset $furthest/${src.length} (%.1f%%)  %.1fs\n"
          .format(100.0 * furthest / src.length, (System.nanoTime() - t0) / 1e9)
    )
    if (bail != null) sb.append("  INCOMPLETE: $bail\n")
    sb.append("  repaired program accepted by THIS grammar: $accepted\n")
    sb.append("  insertion sites by ES5.1 7.9.1 clause: $byClause\n")
    if (illegal.isNotEmpty()) {
      sb.append("  !! ${illegal.size} insertion(s) NOT explainable by ASI, first offsets=${illegal.take(5)}\n")
      for (o in illegal.take(3)) sb.append(context(src, o)).append('\n')
    } else {
      sb.append("  => every insertion is a legal ES5.1 7.9.1 ASI site\n")
    }
    val firstFew = inserts.take(4)
    if (firstFew.isNotEmpty()) {
      sb.append("  first insertion sites:\n")
      for (o in firstFew) {
        sb.append("    offset $o (line ${lineOf(src, o)}) before ${quote(src[o])}  ${clauseOf(src, o)}\n")
      }
    }
    return sb.toString().trimEnd()
  }

  private fun lineOf(src: String, off: Int): Int =
    src.substring(0, off.coerceIn(0, src.length)).count { it == '\n' } + 1
}
