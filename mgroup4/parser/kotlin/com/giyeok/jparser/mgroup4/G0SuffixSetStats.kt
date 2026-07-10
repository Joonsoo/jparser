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
// 캐노니컬라이제이션 규칙 (보고 §b — gen 정규화, 조건 클래스 근사)
// -----------------------------------------------------------------------------
//
// window 크기 n. 각 live shape 는 milestone chain (tip-most = shape.milestonePath,
// root = parent==null) + tipGroupId 로 구성. group 노드는 멤버로 완전히 펼쳐(explode)
// singleton shape 들로 환원한 뒤 처리 (병합이 관찰을 왜곡하지 않게 — 상위 지시).
//
//  1) 윈도 분할: chain 을 tip 에서부터 최대 (n-1) 개 milestone 노드까지가 "window
//     내부 노드", 그 위(root 쪽 전부)가 "out-of-window prefix". suffix 튜플 = window
//     내부 노드들의 (symbolId, pointer, gen오프셋) 열 + tipGroupId. depth 정의:
//     tipGroupId 자체가 depth-1 (n=1 이면 window 내부 milestone 노드 0개 → suffix =
//     tipGroupId 하나 = 현행 milestone group 과 동형).
//
//  2) prefix 버킷 키: out-of-window prefix 체인의 구조 identity —
//     각 노드의 (symbolId, pointer, gen오프셋) 을 root→prefix끝 순서로 나열. 같은 prefix
//     버킷에 떨어지는 shape 들의 suffix 튜플들을 한 상태의 "suffix-set" 으로 모은다.
//
//  3) gen 정규화 (정적 상태는 gen-무관해야 함): 모든 gen 값을 **현재 파스 gen(curGen)
//     기준 상대 오프셋** 으로 치환 — off(g) = curGen - g. 노드가 k gen 전에 부착됐다는
//     "gen-since-attachment" 패턴만 남는다. 이는 gen 간 상대 오프셋 패턴 (절대 위치
//     무관). prefix·suffix·milestone.gen 전부 이 규칙 적용. 오프셋이 window 밖에서도
//     커지지만 (prefix 는 오래된 노드), 구조가 같고 상대 오프셋 패턴이 같으면 같은 상태.
//     → 이 정규화로 "같은 모양이 여러 절대 gen 에 등장" 이 하나의 상태로 접힌다
//       (late-convergence 가 상태를 부풀리는지 = 포화 여부를 정직하게 재려면 필수).
//
//  4) 조건 분화 (두 모드):
//     (a) 조건 무시 — suffix 튜플에 condition 정보 없음.
//     (b) 조건 포함 — 각 suffix 튜플에 그 path 의 AcceptCondition 의 **구조 클래스**
//         (structural class) 를 붙인다. 값(gen/symbolId)이 아니라 형태:
//         condClass(cond) = 조건 트리를 순회하며 각 leaf 를 그 종류(NoLongerMatch/
//         Exists/Unless/OnlyIf/...)로, And/Or 를 자식 클래스의 정렬된 멀티셋으로 접은
//         문자열. 근사: leaf 의 symbolId 는 **포함**하되 startGen/endGen 은 gen 오프셋
//         으로 상대화(off). 이는 "어떤 심볼에 대한 어떤 종류의 조건이 걸렸나 + 그 span
//         이 현재로부터 몇 gen 전에 시작하나" 를 형태로 잡는다. 정확한 조건 템플릿
//         추출(생성기 remap)은 어렵기에 이 런타임 근사를 쓰고 방식을 명시.
//
//  5) 상태 캐논 = 한 prefix 버킷의 {정렬된 suffix 튜플 문자열} 집합 → 정렬 join.
//     전역 set 에 이 문자열을 넣고 distinct count 를 센다. 상태당 튜플 수도 집계.
//
// 병합(runtime packing) 이 켜진 상태에서 수집하되, group 은 멤버로 펼쳐 반영하므로
// 관찰이 왜곡되지 않는다 (병합 여부와 무관하게 같은 live path 집합을 본다).
//
// 파스 출력 무영향 (계측 전용, env MG4_G0_STATS 로 파서에서 게이트). 이 클래스 자체는
// 순수 수집기 — 파서는 hook 만 호출한다.

class G0SuffixSetStats(val n: Int) {
  // 전역 distinct 상태 (조건 무시 / 조건 포함). 메모리 절약 위해 canonical 문자열을
  // 128-bit 지문(두 개의 독립 64-bit 해시를 하나의 Long 으로 접음)으로만 보관 —
  // jquery 급 입력에서 수십만 상태의 full 문자열 누적은 -Xmx10g 로도 OOM (실측).
  // 충돌 확률: 50만 상태에서 64-bit 지문 충돌 기대 < 1e-8 → distinct count 에 무영향.
  private val statesNoCond = HashSet<Long>()
  private val statesWithCond = HashSet<Long>()

  // 상태당 suffix 튜플 수 분포 (조건 무시 기준 — 상태 구조 재료).
  // key = 튜플 수, value = 그 튜플 수를 가진 (상태,gen) 관찰 횟수. 상태 재관찰도 세므로
  // "가중" 분포 (한 상태가 여러 gen 살면 여러 번 셈) — 정성 재료용.
  private val tupleCountHist = HashMap<Int, Long>()
  // distinct 상태별 튜플 수 (평균/최대용, 상태 처음 볼 때만).
  private var distinctTupleSum = 0L
  private var distinctTupleMax = 0

  // 두 독립 seed 로 문자열을 접어 128-bit 지문을 하나의 Long 으로 (상위/하위 32bit 혼합).
  private fun fingerprint(s: String): Long {
    var h1 = 1125899906842597L // seed
    var h2 = -0x61c8864680b583ebL // 다른 seed (golden ratio 계열)
    for (i in s.indices) {
      val c = s[i].code
      h1 = 31 * h1 + c
      h2 = 0x100000001b3L * (h2 xor c.toLong()) // FNV-ish
    }
    // 두 해시를 섞어 하나의 64-bit 로 — 충돌은 두 해시가 동시에 충돌해야.
    return h1 xor java.lang.Long.rotateLeft(h2, 32)
  }

  // 관찰된 gen 수, 총 live shape (explode 후) 수.
  var gensObserved = 0L; private set
  var totalExplodedShapes = 0L; private set
  // 매 gen distinct 상태(조건무시) 수 누적 (상태/gen 평균).
  var perGenStateSum = 0L; private set
  var perGenStateMax = 0; private set

  // 포화 추적: 특정 gen 스냅샷마다 전역 distinct 상태 수를 기록 (증가 곡선).
  // 너무 촘촘하면 메모리 — 로그 간격으로 샘플.
  val saturationCurve = ArrayList<Pair<Long, Int>>() // (gensObserved, statesNoCond.size)
  private var nextSampleAt = 1L

  val distinctStatesNoCond: Int get() = statesNoCond.size
  val distinctStatesWithCond: Int get() = statesWithCond.size
  val meanTuplesPerState: Double get() = if (statesNoCond.isEmpty()) 0.0 else distinctTupleSum.toDouble() / statesNoCond.size
  val maxTuplesPerState: Int get() = distinctTupleMax
  val meanStatesPerGen: Double get() = if (gensObserved == 0L) 0.0 else perGenStateSum.toDouble() / gensObserved

  // gen 오프셋 정규화.
  private fun off(g: Int, curGen: Int): Int = curGen - g

  // 조건 구조 클래스 문자열 (gen 상대화). 형태만 — 값 중 symbolId 는 유지, gen 은 off.
  private fun condClass(cond: AcceptCondition, curGen: Int): String = when (cond) {
    Always -> "T"
    Never -> "F"
    is And -> {
      val parts = ArrayList<String>(cond.size)
      cond.forEach { parts.add(condClass(it, curGen)) }
      parts.sort()
      "&(${parts.joinToString(",")})"
    }
    is Or -> {
      val parts = ArrayList<String>(cond.size)
      cond.forEach { parts.add(condClass(it, curGen)) }
      parts.sort()
      "|(${parts.joinToString(",")})"
    }
    is NoLongerMatch -> "NLM${cond.symbolId}@${off(cond.startGen, curGen)}+${off(cond.minEndGen, curGen)}"
    is NeedLongerMatch -> "NDLM${cond.symbolId}@${off(cond.startGen, curGen)}+${off(cond.minEndGen, curGen)}"
    is Exists -> "EX${cond.symbolId}@${off(cond.startGen, curGen)}"
    is NotExists -> "NEX${cond.symbolId}@${off(cond.startGen, curGen)}"
    is Unless -> "UN${cond.symbolId}@${off(cond.startGen, curGen)}..${off(cond.endGen, curGen)}"
    is OnlyIf -> "OI${cond.symbolId}@${off(cond.startGen, curGen)}..${off(cond.endGen, curGen)}"
  }

  // chain 을 root..tip 순서 배열로. (tip-most = shape.milestonePath)
  private fun chainToList(tip: MilestonePath?): ArrayList<MilestonePath> {
    val rev = ArrayList<MilestonePath>()
    var cur = tip
    while (cur != null) { rev.add(cur); cur = cur.parent }
    rev.reverse()
    return rev
  }

  // group 노드를 멤버로 완전히 펼쳐 singleton shape 들로 환원. group 이 여러 개면
  // 카테시안 곱 (실측상 chain 당 group ≤ 1 이 압도적 — spec item 6 "2중 group 없음").
  // 방어적으로 다중 group 도 처리.
  private fun explodeAll(shape: PathShape): List<PathShape> {
    val chain = chainToList(shape.milestonePath)
    var gIdx = -1
    for (j in chain.indices) if (chain[j].groupMembers != null) { gIdx = j; break }
    if (gIdx < 0) return listOf(shape)
    // gIdx 위치 group 을 멤버로 펼친 뒤 재귀 (남은 group 도 처리).
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

  // 한 exploded singleton shape → (prefixKey, suffixTuple[, condClass]).
  // suffix = window 내부 (n-1) milestone 노드 (tip-most 부터) + tipGroupId.
  // prefix = 그 위 전부.
  private data class Split(val prefixKey: String, val suffixTuple: String)

  private fun splitShape(shape: PathShape, cond: AcceptCondition, curGen: Int, withCond: Boolean): Split {
    val chain = chainToList(shape.milestonePath) // index 0 = root-most
    val L = chain.size
    // window 내부 milestone 노드 수 = min(n-1, L). tip-most (n-1) 개.
    val windowNodes = if (n - 1 < L) n - 1 else L
    val prefixEnd = L - windowNodes // prefix = chain[0 until prefixEnd]
    // prefix 키 (root→prefix끝). gen 오프셋 정규화.
    val pk = StringBuilder()
    for (j in 0 until prefixEnd) {
      val node = chain[j]
      pk.append(node.milestone.symbolId).append('.').append(node.milestone.pointer)
        .append('@').append(off(node.milestone.gen, curGen))
      // observing 도 prefix identity 에 포함 (미래 조건 anchor — 상태 구분 요인).
      if (node.observingCondSymbolIds.isNotEmpty()) {
        pk.append('o').append(node.observingCondSymbolIds.joinToString("_"))
      }
      pk.append('|')
    }
    // suffix 튜플 (window 내부 milestone 노드 tip-most→root쪽 안정 순서 + tipGroupId).
    val st = StringBuilder()
    st.append("tg").append(shape.tipGroupId)
    for (j in prefixEnd until L) {
      val node = chain[j]
      st.append(';').append(node.milestone.symbolId).append('.').append(node.milestone.pointer)
        .append('@').append(off(node.milestone.gen, curGen))
      if (node.observingCondSymbolIds.isNotEmpty()) {
        st.append('o').append(node.observingCondSymbolIds.joinToString("_"))
      }
    }
    if (withCond) st.append("#").append(condClass(cond, curGen))
    return Split(pk.toString(), st.toString())
  }

  // 매 gen main root 의 pathMap (pre-filter, group 포함) 을 받아 상태들을 수집.
  fun observe(mainPathMap: Map<PathShape, AcceptCondition>, curGen: Int) {
    if (mainPathMap.isEmpty()) return
    gensObserved++
    // prefix 버킷 → suffix 튜플 집합 (조건 무시 / 조건 포함).
    val bucketsNoCond = HashMap<String, TreeSet<String>>()
    val bucketsWithCond = HashMap<String, TreeSet<String>>()
    for ((shape, cond) in mainPathMap) {
      for (ex in explodeAll(shape)) {
        totalExplodedShapes++
        val sNo = splitShape(ex, cond, curGen, withCond = false)
        bucketsNoCond.getOrPut(sNo.prefixKey) { TreeSet() }.add(sNo.suffixTuple)
        val sWith = splitShape(ex, cond, curGen, withCond = true)
        bucketsWithCond.getOrPut(sWith.prefixKey) { TreeSet() }.add(sWith.suffixTuple)
      }
    }
    // 각 버킷 = 한 suffix-set 상태. 캐논 = 정렬된 suffix 튜플 join.
    var perGenDistinct = 0
    for (tuples in bucketsNoCond.values) {
      val canon = fingerprint(tuples.joinToString("|"))
      if (statesNoCond.add(canon)) {
        distinctTupleSum += tuples.size
        if (tuples.size > distinctTupleMax) distinctTupleMax = tuples.size
      }
      tupleCountHist.merge(tuples.size, 1L) { a, b -> a + b }
      perGenDistinct++
    }
    for (tuples in bucketsWithCond.values) {
      statesWithCond.add(fingerprint(tuples.joinToString("|")))
    }
    perGenStateSum += perGenDistinct
    if (perGenDistinct > perGenStateMax) perGenStateMax = perGenDistinct
    // 포화 곡선 샘플 (지수 간격).
    if (gensObserved >= nextSampleAt) {
      saturationCurve.add(Pair(gensObserved, statesNoCond.size))
      nextSampleAt = (gensObserved * 2).coerceAtLeast(gensObserved + 1)
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
    sb.append("  distinctStates(noCond)=$distinctStatesNoCond distinctStates(withCond)=$distinctStatesWithCond\n")
    sb.append("  meanTuples/state=%.2f maxTuples/state=$maxTuplesPerState\n".format(meanTuplesPerState))
    sb.append("  meanStates/gen=%.2f maxStates/gen=$perGenStateMax\n".format(meanStatesPerGen))
    if (currentMilestoneGroups > 0) {
      sb.append("  vs current milestoneGroups=$currentMilestoneGroups : ")
      sb.append("noCond=%.3fx withCond=%.3fx\n".format(
        distinctStatesNoCond.toDouble() / currentMilestoneGroups,
        distinctStatesWithCond.toDouble() / currentMilestoneGroups))
    }
    sb.append("  tupleCountHist(weighted): ${tupleCountHistogramString()}\n")
    sb.append("  saturationCurve(gen->states): ${saturationCurve.joinToString(" ") { "${it.first}->${it.second}" }}")
    return sb.toString()
  }
}

private typealias TreeSet<T> = java.util.TreeSet<T>
