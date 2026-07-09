package com.giyeok.jparser.mgroup4

// 검증용 스위치: direct 평가와 기존 replay 평가를 병행 실행해 불일치 시 throw.
// 테스트/디버그 전용 — 성능 측정 시에는 꺼져 있어야 한다.
var mg3RecordCondDiffCheck: Boolean =
  System.getenv("MG3_RECORD_COND_DIFF") != null || System.getProperty("mg3.recordCondDiff") != null

// record 조건 평가 — evaluateRecordCondition 의 step-by-step replay 와 같은 답을
// 재생 없이 leaf-직접 조회로 계산한다 (replay 는 record 마다 파스 잔여 전체를
// evolve 재생해 O(records × gens) — 이 클래스가 그 비용을 제거한다).
//
// 의미론의 진실은 evolveAcceptCondition (AcceptCondition.kt) 이다. 각 leaf 의 규칙은
// evolve 분기에서 유도됐다 (mgroup3/docs/kernels_history_optimization.md):
//  - Unless/OnlyIf: 정확히 endGen 의 finish 만 — eager 는 gen == endGen 에서,
//    late 는 gen == endGen+1 에서 관찰. eager 가 없으면 root 가 endGen 에
//    살아있을 때만 한 step 대기 (evolve 의 activeCondPaths 분기 그대로).
//  - NoLongerMatch/NeedLongerMatch: end >= minEndGen 인 finish 만 흡수
//    (eager fin 의 end == gen, late fin 의 end == gen-1).
//  - NotExists/Exists: end 무관 — 두 채널의 모든 finish.
//  - 흡수 창: replay 에서 leaf 는 root 가 죽는 step 에서 pending 을 떨구고 확정되므로,
//    fin 스캔은 root 활성 구간 기준 [fromGen, lastActive+1] 로 제한된다
//    (lastActive+1 step 의 fin 은 그 step 에 죽으면서 등록된 것 — 흡수됨).
//    fromGen 에 root 가 비활성이면 fromGen step 의 fin 만 흡수하고 확정.
//    everSeenCondRoots 규칙(한 번 등장한 root 는 재시동 안 됨)에 의해 활성 구간은
//    연속이고, 죽은 뒤의 fin 은 존재하지 않는다.
//  - 흡수한 finish 의 조건은 그 관찰 gen 에서 재귀 평가 (fin 조건이 또 조건 참조).
//  - visiting (같은 step 안에서 자기 root 로 재귀 도달): NoLongerMatch/NeedLongerMatch 는
//    둘 다 Always (evolve 그대로 — 쌍대가 아님에 주의), 나머지는 이번 step 소비만
//    건너뛰고 다음 step 부터 fresh 재평가 (= evalCond(cond, fromGen+1, ∅)).
//  - 입력 끝: 실제 step 이후의 가상 late step (gen == history.size, endLateFins) 을
//    거친 뒤 잔여 leaf 는 NoLongerMatch/NotExists/Unless true, 쌍대 false.
//
// 인스턴스는 kernelsHistory/isAccepted 호출당 하나 — 파서 인스턴스에 공유 mutable
// 캐시를 두지 않는다 (Rust 미러의 Send+Sync / 락 경합 규칙과 동일).
class RecordConditionEvaluator(
  private val history: List<HistoryEntry>,
  private val endLateFins: Map<PathRoot, AcceptCondition>,
) {
  private val historySize = history.size

  // inverted index: root → fin 이 존재하는 gen (history 순회 순서 그대로 오름차순).
  private val eagerFinGens = HashMap<PathRoot, ArrayList<Int>>()
  private val lateFinGens = HashMap<PathRoot, ArrayList<Int>>()

  // root 별 activeCondPaths 등장 구간 [firstActive, lastActive] (연속 — 위 주석 참조).
  private val firstActive = HashMap<PathRoot, Int>()
  private val lastActive = HashMap<PathRoot, Int>()

  init {
    for (g in history.indices) {
      val entry = history[g]
      for (root in entry.condPathFinishes.keys) eagerFinGens.getOrPut(root) { ArrayList() }.add(g)
      for (root in entry.lateCondPathFinishes.keys) lateFinGens.getOrPut(root) { ArrayList() }.add(g)
      for (root in entry.activeCondPaths) {
        if (root !in firstActive) firstActive[root] = g
        lastActive[root] = g
      }
    }
  }

  private data class MemoKey(val cond: AcceptCondition, val gen: Int, val visiting: Set<PathRoot>)

  private val memo = HashMap<MemoKey, Boolean>()

  fun evaluate(cond: AcceptCondition, recordGen: Int): Boolean {
    val result = evalCond(cond, recordGen, emptySet())
    if (mg3RecordCondDiffCheck) {
      val replayed = evaluateReplay(cond, recordGen)
      check(result == replayed) {
        "RecordConditionEvaluator mismatch: direct=$result replay=$replayed recordGen=$recordGen cond=$cond"
      }
    }
    return result
  }

  private fun evalCond(cond: AcceptCondition, fromGen: Int, visiting: Set<PathRoot>): Boolean {
    when (cond) {
      Always -> return true
      Never -> return false
      else -> {}
    }
    // visiting 이 있으면 fromGen step 의 소비 여부가 달라지므로 정규화 불가.
    val effGen = if (visiting.isEmpty()) normalizedGen(cond, fromGen) else fromGen
    val key = MemoKey(cond, effGen, visiting)
    memo[key]?.let { return it }
    val result = compute(cond, effGen, visiting)
    memo[key] = result
    return result
  }

  // 답이 같은 범위 안에서 fromGen 을 대표 gen 으로 clamp — 메모 키 폭발 방지.
  // (record 들은 같은 조건을 서로 다른 recordGen 에서 참조하는 일이 많다.)
  //  - Unless/OnlyIf: endGen 이전에는 어떤 step 도 소비하지 않으므로 (evolve 의
  //    gen < endGen 분기) fromGen <= endGen 이 전부 같은 답. endGen+2 이상도 동일.
  //  - NLM/NeedLM: 활성 구간 안에서는 minEndGen 클램프 이전의 fin 이 무시되므로
  //    fromGen <= minEndGen 이 같은 답 (스캔 창 상한은 lastActive+1 로 fromGen 무관).
  //    비활성 fromGen 은 그 step 의 fin 소비 여부가 fromGen 자체에 달려 있어 정규화 없음.
  //  - NotExists/Exists 와 composite: fromGen 자체가 스캔 시작 — 정규화 없음.
  private fun normalizedGen(cond: AcceptCondition, g: Int): Int = when (cond) {
    is Unless -> normalizedBoundedGen(g, cond.endGen)
    is OnlyIf -> normalizedBoundedGen(g, cond.endGen)
    is NoLongerMatch -> normalizedLongestGen(PathRoot(cond.symbolId, cond.startGen), g, cond.minEndGen)
    is NeedLongerMatch -> normalizedLongestGen(PathRoot(cond.symbolId, cond.startGen), g, cond.minEndGen)
    else -> g
  }

  private fun normalizedBoundedGen(g: Int, endGen: Int): Int =
    if (g <= endGen) endGen else minOf(g, endGen + 2)

  private fun normalizedLongestGen(root: PathRoot, g: Int, minEndGen: Int): Int {
    val fa = firstActive[root] ?: return g
    val la = lastActive[root]!!
    if (g < fa || g > la) return g
    return if (g <= minEndGen) minOf(minEndGen, la) else g
  }

  private fun compute(cond: AcceptCondition, fromGen: Int, visiting: Set<PathRoot>): Boolean = when (cond) {
    Always -> true
    Never -> false
    is And -> {
      var result = true
      for (i in 0 until cond.size) {
        if (!evalCond(cond.elementAt(i), fromGen, visiting)) {
          result = false
          break
        }
      }
      result
    }
    is Or -> {
      var result = false
      for (i in 0 until cond.size) {
        if (evalCond(cond.elementAt(i), fromGen, visiting)) {
          result = true
          break
        }
      }
      result
    }

    is NoLongerMatch -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      // evolve: visiting 중 자기 root 재도달 시 NLM/NeedLM 둘 다 Always.
      if (root in visiting) true
      else !anyAbsorbedFinTrue(root, fromGen, cond.minEndGen, cond.minEndGen + 1, visiting)
    }
    is NeedLongerMatch -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      if (root in visiting) true
      else anyAbsorbedFinTrue(root, fromGen, cond.minEndGen, cond.minEndGen + 1, visiting)
    }
    is NotExists -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      if (root in visiting) evalCond(cond, fromGen + 1, emptySet())
      else !anyAbsorbedFinTrue(root, fromGen, Int.MIN_VALUE, Int.MIN_VALUE, visiting)
    }
    is Exists -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      if (root in visiting) evalCond(cond, fromGen + 1, emptySet())
      else anyAbsorbedFinTrue(root, fromGen, Int.MIN_VALUE, Int.MIN_VALUE, visiting)
    }

    is Unless -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      if (root in visiting) evalCond(cond, fromGen + 1, emptySet())
      else {
        val fin = boundedFin(root, fromGen, cond.endGen)
        if (fin == null) true
        else !evalCond(fin.cond, fin.gen, finVisiting(fin.gen, fromGen, visiting, root))
      }
    }
    is OnlyIf -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      if (root in visiting) evalCond(cond, fromGen + 1, emptySet())
      else {
        val fin = boundedFin(root, fromGen, cond.endGen)
        if (fin == null) false
        else evalCond(fin.cond, fin.gen, finVisiting(fin.gen, fromGen, visiting, root))
      }
    }
  }

  private class BoundedFin(val cond: AcceptCondition, val gen: Int)

  // Unless/OnlyIf 가 소비하는 finish 와 그 관찰 gen — evolve 의 bounded 분기 그대로:
  //  - fromGen > endGen+1: 소비 시점을 지남 (없음 — Always/Never 확정)
  //  - fromGen == endGen+1: late 채널만 (그 step 의 lateCondPathFinishes,
  //    step == history.size 면 입력 끝 가상 late = endLateFins)
  //  - fromGen <= endGen: endGen 의 eager → 없으면 root 가 endGen 에 살아있을 때만
  //    endGen+1 의 late 로 한 step 대기. endGen 이 실제 step 밖이면 관찰 불가.
  private fun boundedFin(root: PathRoot, fromGen: Int, endGen: Int): BoundedFin? {
    if (fromGen > endGen + 1) return null
    if (fromGen == endGen + 1) return lateFinAt(root, endGen + 1)
    if (endGen >= historySize) return null
    val eager = history[endGen].condPathFinishes[root]
    if (eager != null) return BoundedFin(eager, endGen)
    if (root !in history[endGen].activeCondPaths) return null
    return lateFinAt(root, endGen + 1)
  }

  private fun lateFinAt(root: PathRoot, g: Int): BoundedFin? {
    val fin = when {
      g < historySize -> history[g].lateCondPathFinishes[root]
      g == historySize -> endLateFins[root]
      else -> null
    }
    return if (fin == null) null else BoundedFin(fin, g)
  }

  // NLM/NeedLM/NotExists/Exists 가 흡수하는 fin 들 중 조건이 참으로 평가되는 것이
  // 있는지. eagerMinGen/lateMinGen 은 minEndGen 클램프 (eager fin end == gen,
  // late fin end == gen-1 → late 는 minEndGen+1 부터). NotExists/Exists 는 클램프 없음.
  private fun anyAbsorbedFinTrue(
    root: PathRoot,
    fromGen: Int,
    eagerMinGen: Int,
    lateMinGen: Int,
    visiting: Set<PathRoot>,
  ): Boolean {
    // 흡수 창 상한: fromGen 에 root 활성이면 lastActive+1 (죽는 step 의 fin 까지),
    // 비활성이면 fromGen step 만 (replay 는 그 step 에서 pending 을 떨구고 확정).
    val fa = firstActive[root]
    val windowEnd =
      if (fa == null || fromGen < fa || fromGen > lastActive[root]!!) fromGen
      else minOf(lastActive[root]!! + 1, historySize)
    val realEnd = minOf(windowEnd, historySize - 1)

    eagerFinGens[root]?.let { gens ->
      var i = lowerBound(gens, maxOf(fromGen, eagerMinGen))
      while (i < gens.size && gens[i] <= realEnd) {
        val g = gens[i]
        val fin = history[g].condPathFinishes[root]!!
        if (evalCond(fin, g, finVisiting(g, fromGen, visiting, root))) return true
        i++
      }
    }
    lateFinGens[root]?.let { gens ->
      var i = lowerBound(gens, maxOf(fromGen, lateMinGen))
      while (i < gens.size && gens[i] <= realEnd) {
        val g = gens[i]
        val fin = history[g].lateCondPathFinishes[root]!!
        if (evalCond(fin, g, finVisiting(g, fromGen, visiting, root))) return true
        i++
      }
    }
    // 입력 끝 가상 late step (gen == historySize).
    if (windowEnd >= historySize && historySize >= maxOf(fromGen, lateMinGen)) {
      val fin = endLateFins[root]
      if (fin != null && evalCond(fin, historySize, finVisiting(historySize, fromGen, visiting, root))) return true
    }
    return false
  }

  // fin 조건 재귀 평가의 visiting: 같은 step (fromGen) 에서의 흡수면 기존 visiting 에
  // 누적, 이후 step 이면 replay 의 step 별 fresh visiting 에 대응해 {root} 만.
  private fun finVisiting(obsGen: Int, fromGen: Int, visiting: Set<PathRoot>, root: PathRoot): Set<PathRoot> =
    if (obsGen == fromGen) visiting + root else setOf(root)

  private fun lowerBound(list: ArrayList<Int>, key: Int): Int {
    var lo = 0
    var hi = list.size
    while (lo < hi) {
      val mid = (lo + hi) ushr 1
      if (list[mid] < key) lo = mid + 1 else hi = mid
    }
    return lo
  }

  // === 이하 검증용 replay 구현 (기존 evaluateRecordCondition 그대로) ===

  // record 가 생성된 시점(recordGen)부터 매 step 의 evolve 를 재생한 뒤 최종 평가.
  fun evaluateReplay(cond: AcceptCondition, recordGen: Int): Boolean {
    var c = cond
    for (g in recordGen until history.size) {
      if (c == Always) return true
      if (c == Never) return false
      val entry = history[g]
      c = evolveAcceptCondition(c, entry.condPathFinishes, entry.lateCondPathFinishes, entry.activeCondPaths, g)
    }
    if (c == Always) return true
    if (c == Never) return false
    // 가상 late step: 입력 끝에서 살아있던 root 들의 마지막-gen zero-width finish 들.
    if (endLateFins.isNotEmpty()) {
      c = evolveAcceptCondition(c, emptyMap(), endLateFins, emptySet(), history.size)
    }
    return evaluateAtEndOfInput(c)
  }

  // replay 를 마지막 entry 까지 마친 뒤 남은 residual 조건의 입력-끝 평가.
  private fun evaluateAtEndOfInput(c: AcceptCondition): Boolean = when (c) {
    Always -> true
    Never -> false
    is And -> {
      var result = true
      c.forEach { if (!evaluateAtEndOfInput(it)) result = false }
      result
    }
    is Or -> {
      var result = false
      c.forEach { if (evaluateAtEndOfInput(it)) result = true }
      result
    }
    is NoLongerMatch -> true
    is NeedLongerMatch -> false
    is NotExists -> true
    is Exists -> false
    is Unless -> true
    is OnlyIf -> false
  }
}
