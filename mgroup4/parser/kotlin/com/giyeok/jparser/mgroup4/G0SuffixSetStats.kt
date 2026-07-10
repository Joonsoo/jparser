package com.giyeok.jparser.mgroup4

// =============================================================================
// mgroup4 Phase G0 — suffix-set 상태 공간 정적 타당성 프로브 (동적 하한 수집)
// =============================================================================
//
// 목적 (phase_g_plan.md §0/§1/§2 G0 행): generator-native mgroup4 의 목표 아키텍처는
// "상태 = 공유 prefix + suffix-set 정적 id" 다. suffix-set = 길이 ≤ n 의 상관된
// milestone-path-suffix 튜플들의 집합. n=1 ≡ 현행 milestone group.
// 질문: 이 suffix-set 을 생성기가 사전 열거하면 상태/테이블이 폭발하는가?
//
// 이 클래스는 **동적 하한** 을 잰다: generator-native 런타임이 실제로 밟게 될 상태들의
// 하한 = "현행 파서의 live path 들을 공유 prefix 로 묶었을 때, 각 묶음의 window-suffix
// 집합". 매 gen main root 의 live shape 들을 아래 규칙으로 캐노니컬라이즈해 전역 set 에
// 수집하고, 파스 끝에 distinct 상태 수를 보고한다.
//
// -----------------------------------------------------------------------------
// 캐노니컬라이제이션 규칙 (v2 — 상위 세션 리뷰 반영: gen 을 상태 정체성에서 완전 제거)
// -----------------------------------------------------------------------------
//
// v1 의 결함: gen 을 "현재 gen 대비 원값 오프셋" (off = curGen − g) 으로 상태 정체성에
// 포함했었다. window 노드는 긴 스캔 중 tip 근처에 오래 머물 수 있어 오프셋이 무한히
// 자라고, 같은 구조적 설정이 매 gen 새 상태로 집계됐다 (실측 "gen 당 ~0.5 신규 상태"
// 꼬리의 원인). 현행 parserdata 처럼 gen 은 전부 런타임 바인딩 — 정적 상태 정체성에
// 들어가면 안 된다.
//
// window 크기 n. 각 live shape 는 milestone chain (tip-most = shape.milestonePath,
// root = parent==null) + tipGroupId. group 노드는 멤버로 완전히 펼쳐(explode) singleton
// shape 들로 환원한 뒤 처리 (병합이 관찰을 왜곡하지 않게).
//
//  1) 윈도 분할: chain 의 tip-most (n-1) 개 milestone 노드 = window 내부, 그 위(root
//     쪽 전부) = out-of-window prefix. n=1 이면 window 내부 milestone 노드 0개 →
//     suffix = tipGroupId 하나 = 현행 milestone group 동형 (대조군).
//
//  2) 버킷팅 (상태 정체성 아님): 같은 **구체(concrete) prefix 인스턴스** 를 공유하는
//     shape 들이 한 버킷 = 한 "관찰된 suffix-set 상태" 의 멤버. 버킷 키 = prefix 의
//     마지막 노드 (MilestonePath equals — gen 포함한 인스턴스 동일성). 목표 아키텍처의
//     런타임 행 (prefix 포인터) 에 대응 — 서로 다른 살아있는 prefix 는 다른 행이고,
//     행이 가리키는 **정적 상태 id** 만 gen-무관 캐논으로 센다.
//
//  3) 상태 정체성 — 변형 2종:
//     [변형 A (주)] 튜플 = window 노드들의 (symbolId, pointer[, observing]) 열 +
//       tipGroupId. gen 성분 완전 제외. 상태 = 정렬된 튜플 **집합** (같은 kernel-패턴
//       튜플의 다른-gen 중복 인스턴스는 집합화로 자연 흡수 — multiplicity 도 정체성에서
//       제외. 오늘날 같은 group 아래 여러 path 가 런타임 행인 것과 동형).
//     [변형 B (참고)] A + "gen-동등 패턴": 상태 내 모든 window 슬롯의 gen 값들
//       (슬롯당 2개 — 노드 부착 gen, milestone kernel gen) 을 **상태 내 등수(rank)** 로
//       치환한 패턴. 원값이 아니라 "어떤 슬롯들이 같은 gen 을 공유하는가 + 상대
//       신구(新舊) 순서" 만 남는다 (순수 동등-파티션보다 약간 세밀한 상한 근사 —
//       순서까지 구분. 즉 |B| ≥ |순수동등| ≥ |A|). A↔B 격차 = gen 패턴 분화 기여의 상한.
//       B 에서 같은 kernel-패턴의 다른-gen 인스턴스는 구분되어 남는다 (rank 열이 다름).
//
//  4) 조건 분화 (두 모드, A/B 각각):
//     (a) 조건 무시 — 튜플에 condition 정보 없음.
//     (b) 조건 포함 — 각 튜플에 그 path 의 AcceptCondition 의 **구조 클래스** 를 붙임:
//         leaf = 종류(NoLongerMatch/Exists/Unless/OnlyIf/...) + 대상 symbolId (gen 값
//         제외 — v2), And/Or = 자식 클래스의 정렬 멀티셋. 값이 아닌 형태. 조건의 gen
//         들은 B 의 동등-파티션에도 넣지 않는다 (window 슬롯 gen 만) — 근사 명시.
//
//  5) 상태 캐논 = 버킷의 {정렬된 튜플 (집합 | B 는 rank열 포함 시퀀스)} 문자열 →
//     128-bit 지문(Long) 으로 전역 set 에 수집 (메모리 — 수십만 상태 규모).
//
// 파스 출력 무영향 (계측 전용, env MG4_G0_STATS 로 파서에서 게이트).

class G0SuffixSetStats(
  val n: Int,
  // 포화 곡선의 선형 샘플링용 예상 총 gen 수 (= 입력 길이). 0 이면 지수 샘플만.
  val expectedTotalGens: Int = 0,
) {
  // 전역 distinct 상태 — 변형 A/B × 조건 무시/포함.
  private val statesANoCond = HashSet<Long>()
  private val statesAWithCond = HashSet<Long>()
  private val statesBNoCond = HashSet<Long>()
  private val statesBWithCond = HashSet<Long>()

  // 상태당 suffix 튜플 수 분포 (변형 A 조건무시 기준 — 상태 구조 재료).
  // 상태 재관찰도 세는 "가중" 분포 (한 상태가 여러 gen 살면 여러 번 셈).
  private val tupleCountHist = HashMap<Int, Long>()
  private var distinctTupleSum = 0L
  private var distinctTupleMax = 0

  var gensObserved = 0L; private set
  var totalExplodedShapes = 0L; private set
  var perGenStateSum = 0L; private set
  var perGenStateMax = 0; private set

  // 포화 곡선 (변형 A 조건무시): 지수 간격 + (expectedTotalGens 있으면) 5% 선형 간격.
  val saturationCurve = ArrayList<Pair<Long, Int>>()
  private var nextSampleAt = 1L
  val linearCurve = ArrayList<Pair<Long, Int>>()
  private val linearStep = if (expectedTotalGens > 0) (expectedTotalGens / 20).coerceAtLeast(1).toLong() else 0L
  private var nextLinearAt = if (linearStep > 0) linearStep else Long.MAX_VALUE

  val distinctStatesANoCond: Int get() = statesANoCond.size
  val distinctStatesAWithCond: Int get() = statesAWithCond.size
  val distinctStatesBNoCond: Int get() = statesBNoCond.size
  val distinctStatesBWithCond: Int get() = statesBWithCond.size
  val meanTuplesPerState: Double get() = if (statesANoCond.isEmpty()) 0.0 else distinctTupleSum.toDouble() / statesANoCond.size
  val maxTuplesPerState: Int get() = distinctTupleMax
  val meanStatesPerGen: Double get() = if (gensObserved == 0L) 0.0 else perGenStateSum.toDouble() / gensObserved

  // 두 독립 seed 로 문자열을 접어 128-bit 지문을 하나의 Long 으로.
  // 충돌 확률: 100만 상태에서 64-bit 지문 충돌 기대 << 1e-6 → distinct count 무영향.
  private fun fingerprint(s: String): Long {
    var h1 = 1125899906842597L
    var h2 = -0x61c8864680b583ebL
    for (i in s.indices) {
      val c = s[i].code
      h1 = 31 * h1 + c
      h2 = 0x100000001b3L * (h2 xor c.toLong())
    }
    return h1 xor java.lang.Long.rotateLeft(h2, 32)
  }

  // 조건 구조 클래스 (v2: gen-free) — leaf 종류 + symbolId 만, And/Or 는 정렬 멀티셋.
  private fun condClass(cond: AcceptCondition): String = when (cond) {
    Always -> "T"
    Never -> "F"
    is And -> {
      val parts = ArrayList<String>(cond.size)
      cond.forEach { parts.add(condClass(it)) }
      parts.sort()
      "&(${parts.joinToString(",")})"
    }
    is Or -> {
      val parts = ArrayList<String>(cond.size)
      cond.forEach { parts.add(condClass(it)) }
      parts.sort()
      "|(${parts.joinToString(",")})"
    }
    is NoLongerMatch -> "NLM${cond.symbolId}"
    is NeedLongerMatch -> "NDLM${cond.symbolId}"
    is Exists -> "EX${cond.symbolId}"
    is NotExists -> "NEX${cond.symbolId}"
    is Unless -> "UN${cond.symbolId}"
    is OnlyIf -> "OI${cond.symbolId}"
  }

  private fun chainToList(tip: MilestonePath?): ArrayList<MilestonePath> {
    val rev = ArrayList<MilestonePath>()
    var cur = tip
    while (cur != null) { rev.add(cur); cur = cur.parent }
    rev.reverse()
    return rev
  }

  // group 노드를 멤버로 완전히 펼쳐 singleton shape 들로 환원 (재귀 — 다중 group 방어).
  private fun explodeAll(shape: PathShape): List<PathShape> {
    val chain = chainToList(shape.milestonePath)
    var gIdx = -1
    for (j in chain.indices) if (chain[j].groupMembers != null) { gIdx = j; break }
    if (gIdx < 0) return listOf(shape)
    val groupNode = chain[gIdx]
    val members = groupNode.groupMembers!!
    val reportGens = groupNode.groupMemberReportGens
    val out = ArrayList<PathShape>()
    for (i in members.indices) {
      val memberNode = MilestonePath(
        gen = groupNode.gen,
        milestone = members[i],
        parent = groupNode.parent,
        observingCondSymbolIds = groupNode.observingCondSymbolIds,
        reportGen = groupNode.reportGen,
        milestoneReportGen = reportGens?.get(i) ?: groupNode.milestoneReportGen,
        groupMembers = null,
        groupMemberReportGens = null,
      )
      var node = memberNode
      for (j in gIdx + 1 until chain.size) node = chain[j].copy(parent = node)
      out.addAll(explodeAll(PathShape(node, shape.tipGroupId)))
    }
    return out
  }

  // 버킷 멤버 한 항목: 조건 무시/포함 튜플 문자열 (gen-free) + window 슬롯 gen 열.
  private class Entry(val tNo: String, val tCond: String, val gens: IntArray)

  // 변형 B 캐논: (튜플, gen열) 목록 → 정확중복 제거 → 상태 내 전체 gen 값을 등수(rank)
  // 로 치환 → (튜플, rank열) 정렬 → join. rank 는 상태 내 distinct gen 의 오름차순 index
  // — 동등성과 상대 순서만 남는다 (원값/오프셋 아님).
  private fun canonB(entries: List<Entry>, useCond: Boolean): String {
    // 정확중복 (같은 튜플 + 같은 gen열 = 같은 런타임 행의 재관찰) 제거.
    val seen = HashSet<String>()
    val ded = ArrayList<Entry>(entries.size)
    for (e in entries) {
      val t = if (useCond) e.tCond else e.tNo
      if (seen.add(t + "~" + e.gens.contentToString())) ded.add(e)
    }
    // 상태 내 등장 gen 전체의 rank map.
    val distinct = java.util.TreeSet<Int>()
    for (e in ded) for (g in e.gens) distinct.add(g)
    val rank = HashMap<Int, Int>(distinct.size * 2)
    var r = 0
    for (g in distinct) rank[g] = r++
    // (튜플, rank열) 로 치환 후 정렬 (튜플 사전순 → rank열 사전순).
    val ranked = ArrayList<Pair<String, IntArray>>(ded.size)
    for (e in ded) {
      val t = if (useCond) e.tCond else e.tNo
      val rs = IntArray(e.gens.size)
      for (i in e.gens.indices) rs[i] = rank[e.gens[i]]!!
      ranked.add(Pair(t, rs))
    }
    ranked.sortWith { a, b ->
      val c = a.first.compareTo(b.first)
      if (c != 0) c
      else {
        val x = a.second; val y = b.second
        var res = 0
        val m = if (x.size < y.size) x.size else y.size
        for (i in 0 until m) if (x[i] != y[i]) { res = x[i] - y[i]; break }
        if (res != 0) res else x.size - y.size
      }
    }
    return ranked.joinToString("|") { "${it.first}~${it.second.joinToString(",")}" }
  }

  // 매 gen main root 의 pathMap (post-merge, pre-filter) 을 받아 상태들을 수집.
  // curGen 은 상태 정체성에 사용하지 않는다 (v2) — API 호환용.
  fun observe(mainPathMap: Map<PathShape, AcceptCondition>, @Suppress("UNUSED_PARAMETER") curGen: Int) {
    if (mainPathMap.isEmpty()) return
    gensObserved++
    // 버킷 키 = 구체 prefix 인스턴스 (마지막 prefix 노드; 전체 chain 이 window 안이면 null).
    val buckets = HashMap<MilestonePath?, ArrayList<Entry>>()
    for ((shape, cond) in mainPathMap) {
      for (ex in explodeAll(shape)) {
        totalExplodedShapes++
        val chain = chainToList(ex.milestonePath)
        val L = chain.size
        val windowNodes = if (n - 1 < L) n - 1 else L
        val prefixEnd = L - windowNodes
        val bucketKey: MilestonePath? = if (prefixEnd == 0) null else chain[prefixEnd - 1]
        // suffix 튜플 (gen-free): tipGroupId + window 노드들의 (symbolId, pointer[, obs]).
        val sb = StringBuilder()
        sb.append("tg").append(ex.tipGroupId)
        val gens = IntArray(windowNodes * 2)
        for (j in prefixEnd until L) {
          val node = chain[j]
          sb.append(';').append(node.milestone.symbolId).append('.').append(node.milestone.pointer)
          if (node.observingCondSymbolIds.isNotEmpty()) {
            sb.append('o').append(node.observingCondSymbolIds.joinToString("_"))
          }
          val k = (j - prefixEnd) * 2
          gens[k] = node.gen               // 노드 부착 gen
          gens[k + 1] = node.milestone.gen // milestone kernel gen
        }
        val tNo = sb.toString()
        val tCond = tNo + "#" + condClass(cond)
        buckets.getOrPut(bucketKey) { ArrayList() }.add(Entry(tNo, tCond, gens))
      }
    }
    var perGenDistinct = 0
    for (entries in buckets.values) {
      // 변형 A: 정렬된 튜플 집합 (중복/multiplicity 자연 흡수).
      val tupSetNo = java.util.TreeSet<String>()
      val tupSetCond = java.util.TreeSet<String>()
      for (e in entries) { tupSetNo.add(e.tNo); tupSetCond.add(e.tCond) }
      val aCanon = fingerprint(tupSetNo.joinToString("|"))
      if (statesANoCond.add(aCanon)) {
        distinctTupleSum += tupSetNo.size
        if (tupSetNo.size > distinctTupleMax) distinctTupleMax = tupSetNo.size
      }
      tupleCountHist.merge(tupSetNo.size, 1L) { a, b -> a + b }
      statesAWithCond.add(fingerprint(tupSetCond.joinToString("|")))
      // 변형 B: A + gen 동등/순서 패턴 (rank 열).
      statesBNoCond.add(fingerprint(canonB(entries, useCond = false)))
      statesBWithCond.add(fingerprint(canonB(entries, useCond = true)))
      perGenDistinct++
    }
    perGenStateSum += perGenDistinct
    if (perGenDistinct > perGenStateMax) perGenStateMax = perGenDistinct
    // 포화 곡선 (변형 A 조건무시) — 지수 + 선형 샘플.
    if (gensObserved >= nextSampleAt) {
      saturationCurve.add(Pair(gensObserved, statesANoCond.size))
      nextSampleAt = (gensObserved * 2).coerceAtLeast(gensObserved + 1)
    }
    if (gensObserved >= nextLinearAt) {
      linearCurve.add(Pair(gensObserved, statesANoCond.size))
      nextLinearAt += linearStep
    }
  }

  fun tupleCountHistogramString(): String {
    val keys = tupleCountHist.keys.sorted()
    return keys.joinToString(", ") { "${it}t:${tupleCountHist[it]}" }
  }

  fun report(label: String, currentMilestoneGroups: Int): String {
    val sb = StringBuilder()
    sb.append("[G0-STATS] $label n=$n:\n")
    sb.append("  gensObserved=$gensObserved totalExplodedShapes=$totalExplodedShapes ")
    sb.append("meanShapes/gen=%.2f\n".format(if (gensObserved > 0) totalExplodedShapes.toDouble() / gensObserved else 0.0))
    sb.append("  [A] distinctStates noCond=$distinctStatesANoCond withCond=$distinctStatesAWithCond\n")
    sb.append("  [B] distinctStates noCond=$distinctStatesBNoCond withCond=$distinctStatesBWithCond\n")
    sb.append("  meanTuples/state(A)=%.2f maxTuples/state(A)=$maxTuplesPerState\n".format(meanTuplesPerState))
    sb.append("  meanStates/gen=%.2f maxStates/gen=$perGenStateMax\n".format(meanStatesPerGen))
    if (currentMilestoneGroups > 0) {
      sb.append("  vs current milestoneGroups=$currentMilestoneGroups : ")
      sb.append("A: %.3fx / %.3fx (noCond/withCond)  B: %.3fx / %.3fx\n".format(
        distinctStatesANoCond.toDouble() / currentMilestoneGroups,
        distinctStatesAWithCond.toDouble() / currentMilestoneGroups,
        distinctStatesBNoCond.toDouble() / currentMilestoneGroups,
        distinctStatesBWithCond.toDouble() / currentMilestoneGroups))
    }
    sb.append("  tupleCountHist(A,weighted): ${tupleCountHistogramString()}\n")
    sb.append("  saturationCurveExp(A,gen->states): ${saturationCurve.joinToString(" ") { "${it.first}->${it.second}" }}\n")
    sb.append("  saturationCurveLin(A,gen->states): ${linearCurve.joinToString(" ") { "${it.first}->${it.second}" }}")
    return sb.toString()
  }
}
