package com.giyeok.jparser.mgroup4

// =============================================================================
// mgroup4 Phase G-b0 — State 캐노니컬라이즈 → 정확 키 id 브리지 (계측 전용)
// =============================================================================
//
// 정본: mgroup4/docs/phase_g_design.md §0.5 (상태 모델), §2.3 (캐노니컬라이즈 → id
// 매핑), §4 G-b0, §6 R4. 이 파일은 파스 출력에 무영향인 **계측 전용** 브리지다.
//
// G0SuffixSetStats 는 상태를 128-bit **지문(Long)** 으로 세어 distinct 만 셌다.
// lazy 캐시(G-b1~)는 State 를 **실제 키**로 저장·조회해야 하므로 지문(충돌 가능)이
// 아니라 **충돌 없는 정확 구조 키(CanonKey)** 로 승격한다 (설계 §2.3 주의). 지문 충돌은
// 프로브에선 무해했으나 캐시에선 오답이 된다.
//
// -----------------------------------------------------------------------------
// State 정체성 (설계 §0.5 변형 A + withCond, R4 정밀도)
// -----------------------------------------------------------------------------
//  - window 노드들의 (symbolId, pointer[, observing]) 튜플의 **정렬된 집합** + tipGroupId.
//    gen·multiplicity 제외 (G0SuffixSetStats 변형 A 와 동형).
//  - 조건: **전체 조건 템플릿** (R4 — G0 의 구조 클래스 근사가 아님). AcceptCondition 의
//    절대 gen 값을 **anchor(=curGen) 기준 상대 오프셋**으로 정규화한 형태. 값(절대 gen)만
//    벗기고 구조·symbolId·상대 gen 관계는 전부 보존한다. H5 "동일 condition 병합" 과
//    정확히 일치 — 같은 State 안에서 curGen 이 공통이므로 오프셋 정규화가 절대-조건 동일과
//    같다 (mergeVerdictAtDepth 의 a.cond != b.cond reject 와 동형).
//
// ★ 함정 (캐시 키 정확성 — G-b1 이 여기 의존): CanonKey 는 반드시 **정확 키**여야 한다.
//    지문으로 축약하면 서로 다른 State 가 같은 id 로 접혀 캐시가 오답을 낸다. 그래서
//    CanonKey 는 정렬된 불변 데이터(튜플 리스트 + 조건 템플릿 문자열)를 그대로 들고
//    equals/hashCode 를 구조적으로 구현한다.
//
// ★ 함정 (조건 gen 정규화 앵커): anchor 는 반드시 State 를 관찰하는 gen (curGen) 으로
//    고정한다. window 노드 gen 이 아니라 curGen 인 이유: 조건 leaf 의 gen 은 window 노드
//    gen 과 무관한 앵커(예: 과거 fork 지점, 미래 span end)를 가리킬 수 있어, window 노드
//    gen 을 기준으로 하면 조건 앵커를 정규화할 공통 기준이 없다. curGen 은 모든 조건 leaf 에
//    걸쳐 유일한 공통 앵커다 (v1 이 gen 오프셋을 상태에 넣어 발산한 실수 — G0SuffixSetStats.kt:20-25 —
//    를 여기서 되풀이하지 않으려면 조건 오프셋도 curGen 기준 상대여야 하고, 정규화 후에도
//    "발산 꼬리" 가 없어야 한다: 오프셋은 조건이 참조하는 span 길이에 유계이지 스캔 길이에
//    비례하지 않는다 — 조건은 유한 span 안에서만 살아있으므로).

// 조건 템플릿의 gen 오프셋 정규화 (R4). AcceptCondition 의 절대 gen g 를 anchor 기준
// 상대 오프셋 (anchor - g) 으로 치환한 문자열. 값은 벗기되 구조/symbolId/상대 gen 관계
// 보존. leaf 종류·symbolId·상대 gen 을 전부 담아 "구조 클래스" (G0) 보다 정밀하다.
//
// 정규화 규칙 (정의 — 이 주석이 계약):
//   - anchor = State 를 관찰하는 gen (curGen). 모든 leaf gen 을 off = anchor - gen 으로.
//   - leaf: 종류 태그 + symbolId + (관련 gen 오프셋들). NoLongerMatch/NeedLongerMatch 는
//     startGen·minEndGen 두 오프셋, Unless/OnlyIf 는 startGen·endGen, Exists/NotExists 는
//     startGen 하나. And/Or 는 자식 템플릿의 **정렬 멀티셋** (canonical — 순서 무관).
//   - Always/Never 는 T/F.
private fun condTemplate(cond: AcceptCondition, anchor: Int): String = when (cond) {
  Always -> "T"
  Never -> "F"
  is And -> {
    val parts = ArrayList<String>(cond.size)
    cond.forEach { parts.add(condTemplate(it, anchor)) }
    parts.sort()
    "&(${parts.joinToString(",")})"
  }
  is Or -> {
    val parts = ArrayList<String>(cond.size)
    cond.forEach { parts.add(condTemplate(it, anchor)) }
    parts.sort()
    "|(${parts.joinToString(",")})"
  }
  // longest: startGen·minEndGen 을 상대 오프셋으로 (본문 end+1 경계 보존).
  is NoLongerMatch -> "NLM${cond.symbolId}@${anchor - cond.startGen}:${anchor - cond.minEndGen}"
  is NeedLongerMatch -> "NDLM${cond.symbolId}@${anchor - cond.startGen}:${anchor - cond.minEndGen}"
  // lookahead: startGen 하나.
  is Exists -> "EX${cond.symbolId}@${anchor - cond.startGen}"
  is NotExists -> "NEX${cond.symbolId}@${anchor - cond.startGen}"
  // bounded (except/join): startGen·endGen span.
  is Unless -> "UN${cond.symbolId}@${anchor - cond.startGen}:${anchor - cond.endGen}"
  is OnlyIf -> "OI${cond.symbolId}@${anchor - cond.startGen}:${anchor - cond.endGen}"
}

// State 의 **정확 구조 키** — 정렬된 window 튜플 집합 + tipGroupId + 조건 전체 템플릿.
// gen·multiplicity 제외. equals/hashCode 는 구조적 (지문 아님 — 충돌 없음).
//
// tuples: 이 State (= 한 버킷) 안의 window 슬롯 튜플들의 정렬된 distinct 리스트. 여러 Row 가
//   같은 window 튜플을 공유하면 집합화로 흡수 (multiplicity 제외). window 노드가 없는
//   (n=1) State 는 튜플이 비고 tipGroupId 만 정체성 (§0.5 "n=1 State ≡ tipGroupId").
// tipGroupId: 이 State 의 tip milestone group id — State 정체성의 축 (흡수 대상 아님).
// condTemplate: 이 State 의 조건 전체 템플릿 (버킷이 조건별로 갈리므로 State 내 균일).
class CanonKey(
  private val tuples: List<String>,        // 정렬된 window 튜플 (문자열; distinct)
  private val tipGroupId: Int,
  private val condTemplate: String,
) {
  private val _hash: Int = run {
    var h = 1
    for (t in tuples) h = 31 * h + t.hashCode()
    h = 31 * h + tipGroupId
    h = 31 * h + condTemplate.hashCode()
    h
  }
  override fun hashCode(): Int = _hash
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CanonKey) return false
    return tipGroupId == other.tipGroupId &&
      condTemplate == other.condTemplate &&
      tuples == other.tuples
  }
  override fun toString(): String =
    "State(tg=$tipGroupId, cond=$condTemplate, tuples=[${tuples.joinToString(";")}])"
}

// State 캐노니컬라이저 + id 부여기. G0SuffixSetStats 의 캐논 정의(변형 A + withCond)를
// **정확 키**로 승격하고, gen-무관 id 를 첫 관찰 시 부여한다. 계측 전용 (파스 무영향).
//
// 사용: 파서 생성 후 setStateBridge() 로 주입. observe() 는 매 gen parseStep 이 호출
//   (mainPathsEvolved, post-merge, pre-filter — G0SuffixSetStats.observe 와 같은 지점).
class G0StateBridge(val n: Int) {
  // 정확 키 → State id (첫 관찰 순 부여). HashMap 이지만 CanonKey 가 정확 키라 충돌 없음.
  private val stateIds = HashMap<CanonKey, Int>()
  private var stateIdCounter = 0

  // 검증용 관찰 카운터 (파스 무영향).
  var gensObserved = 0L; private set
  var totalRowsObserved = 0L; private set

  // (검증 (ii)) n=1 에서 State ↔ tipGroupId 1:1 확인용: 각 State id 가 어떤 tipGroupId
  // 집합에서 왔는지, 각 tipGroupId 가 어떤 State id 집합으로 갔는지 추적.
  private val stateIdToTipGroups = HashMap<Int, HashSet<Int>>()
  private val tipGroupToStateIds = HashMap<Int, HashSet<Int>>()

  val distinctStates: Int get() = stateIds.size

  // 튜플 캐노니컬라이즈 helper (G0SuffixSetStats 와 동일 규칙 — group explode 후 window 분할).
  private fun chainToList(tip: MilestonePath?): ArrayList<MilestonePath> {
    val rev = ArrayList<MilestonePath>()
    var cur = tip
    while (cur != null) { rev.add(cur); cur = cur.parent }
    rev.reverse()
    return rev
  }

  // group 노드를 멤버로 완전히 펼쳐 singleton shape 들로 환원 (G0SuffixSetStats.explodeAll 동형).
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

  // 한 explode 된 singleton shape 을 (bucketKey, window 튜플, tipGroupId, cond) 로.
  private class ExplodedRow(
    val windowTuple: String?,   // null = window 노드 없음 (n=1 또는 짧은 체인)
    val tipGroupId: Int,
    val cond: AcceptCondition,
  )

  // State 버킷 키 = (구체 prefix 인스턴스, tipGroupId, 조건 템플릿). 설계 §1.2:
  // SuffixSetState = List<WindowSlot> + tipGroupId + condClass. tipGroupId·조건은 State
  // 정체성의 **축**이지 흡수 대상이 아니다 — 같은 prefix 라도 tip/조건이 다르면 다른 State.
  // window 노드들(형제 상관)만 슬롯 집합으로 모인다.
  //
  // ★ 함정 (n=1 함수성): 초기 오구현은 버킷을 prefix 만으로 잡아 tipGroupId 를 튜플 집합에
  //   흡수했다 — 그러면 같은 prefix 아래 서로 다른 tip 을 가진 Row 가 한 State 로 접혀
  //   n=1 State↔tipGroup 함수성이 깨졌다 (§0.5 "n=1 State ≡ tipGroupId" 위반). tipGroupId 를
  //   버킷 키에 넣어야 §1.2 정의와 정합.
  private data class BucketKey(
    val prefix: MilestonePath?,
    val tipGroupId: Int,
    val condTemplate: String,
  )

  // 매 gen main root 의 pathMap (post-merge, pre-filter) 을 받아 State id 를 부여·검증.
  // curGen 은 조건 gen 오프셋 정규화의 anchor (R4). window 노드/멤버 gen 은 여전히 제외.
  fun observe(mainPathMap: Map<PathShape, AcceptCondition>, curGen: Int) {
    if (mainPathMap.isEmpty()) return
    gensObserved++
    // 버킷 = (구체 prefix 인스턴스, tipGroupId, 조건 템플릿). 한 버킷 = 한 관찰된 State.
    // 서로 다른 살아있는 prefix/tip/조건은 다른 State 이고, 버킷 안 window 튜플들만 슬롯
    // 집합으로 모인다 (형제 상관 — suffix-set 의 "열 상관").
    val buckets = HashMap<BucketKey, ArrayList<ExplodedRow>>()
    for ((shape, cond) in mainPathMap) {
      val ct = condTemplate(cond, curGen)
      for (ex in explodeAll(shape)) {
        totalRowsObserved++
        val chain = chainToList(ex.milestonePath)
        val L = chain.size
        val windowNodes = if (n - 1 < L) n - 1 else L
        val prefixEnd = L - windowNodes
        val bucketPrefix: MilestonePath? = if (prefixEnd == 0) null else chain[prefixEnd - 1]
        // window 튜플 — window 노드가 정확히 (windowNodes) 개. window 노드들을
        // (symbolId.pointer[obs]) 를 ';' 로 이은 하나의 문자열 튜플로 (gen-free).
        val sb = StringBuilder()
        var hasWindow = false
        for (j in prefixEnd until L) {
          val node = chain[j]
          if (hasWindow) sb.append(';')
          sb.append(node.milestone.symbolId).append('.').append(node.milestone.pointer)
          if (node.observingCondSymbolIds.isNotEmpty()) {
            sb.append('o').append(node.observingCondSymbolIds.joinToString("_"))
          }
          hasWindow = true
        }
        val windowTuple = if (hasWindow) sb.toString() else null
        val bk = BucketKey(bucketPrefix, ex.tipGroupId, ct)
        buckets.getOrPut(bk) { ArrayList() }.add(ExplodedRow(windowTuple, ex.tipGroupId, cond))
      }
    }
    // 각 버킷 = 한 관찰된 State. window 튜플 정렬 집합 + tipGroupId + 조건 템플릿으로
    // CanonKey 를 만들어 id 부여.
    for ((bk, rows) in buckets) {
      // 버킷 안 window 튜플의 정렬된 distinct 집합 (multiplicity 제외 — 같은 튜플의 다른-gen
      // 중복 인스턴스는 집합화로 흡수). n=1 이면 튜플이 없어 빈 집합 → State = (tip, 조건).
      val tupSet = java.util.TreeSet<String>()
      for (r in rows) if (r.windowTuple != null) tupSet.add(r.windowTuple)
      val key = CanonKey(
        tuples = ArrayList(tupSet),
        tipGroupId = bk.tipGroupId,
        condTemplate = bk.condTemplate,
      )
      val id = stateIds.getOrPut(key) { stateIdCounter++ }
      // 검증 추적 (n=1 1:1) — 각 Row 의 tipGroupId 를 이 State id 에 연결.
      val tgSet = stateIdToTipGroups.getOrPut(id) { HashSet() }
      for (r in rows) {
        tgSet.add(r.tipGroupId)
        tipGroupToStateIds.getOrPut(r.tipGroupId) { HashSet() }.add(id)
      }
    }
  }

  // 검증 (ii): n=1 에서 State ↔ tipGroupId 1:1 인가. n=1 이면 window 노드가 없어 State
  // 정체성 = (tipGroupId, 조건 템플릿). 조건이 갈리면 한 tipGroupId 가 여러 State 로 갈 수
  // 있으므로 "1:1" 은 조건 무시 축에서만 성립한다 — 여기서는 각 State 가 정확히 하나의
  // tipGroupId 에서 왔는지 (State→tipGroup 함수성) 만 강제한다 (역은 조건으로 1:다 가능).
  // 반환: (위반 State 수, 첫 위반 설명).
  fun checkN1StateToTipFunctional(): Pair<Int, String?> {
    var violations = 0
    var firstMsg: String? = null
    for ((id, tgs) in stateIdToTipGroups) {
      if (tgs.size != 1) {
        violations++
        if (firstMsg == null) firstMsg = "State id=$id maps to ${tgs.size} tipGroups: ${tgs.take(5)}"
      }
    }
    return Pair(violations, firstMsg)
  }

  fun report(label: String, currentMilestoneGroups: Int): String {
    val sb = StringBuilder()
    sb.append("[G-b0-BRIDGE] $label n=$n:\n")
    sb.append("  gensObserved=$gensObserved totalRows=$totalRowsObserved distinctStates=$distinctStates\n")
    if (currentMilestoneGroups > 0) {
      sb.append("  vs current milestoneGroups=$currentMilestoneGroups : %.3fx\n"
        .format(distinctStates.toDouble() / currentMilestoneGroups))
    }
    if (n == 1) {
      val (v, msg) = checkN1StateToTipFunctional()
      sb.append("  n=1 State->tipGroup functional: ${if (v == 0) "OK" else "VIOLATIONS=$v ($msg)"}\n")
    }
    return sb.toString()
  }
}
