package com.giyeok.jparser.mgroup4

sealed class AcceptCondition: Comparable<AcceptCondition> {
  abstract fun neg(): AcceptCondition

  // 이 condition 이 reference 하는 모든 PathRoot. step3 새 cond root 찾기 / step6 prune 의
  // tree traversal 회피 + dirty-root evolve skip 의 기반.
  abstract val referencedRoots: RootSet

  // tree 안에 fromNextGen=true 인 NoLongerMatch/NeedLongerMatch 존재 여부. evolve 에서 매 step
  // 한 step 만에 false 로 변환되므로, true 면 무조건 evolve 해야 (skip 불가).
  abstract val hasFromNextGen: Boolean

  override fun compareTo(other: AcceptCondition): Int =
    this.toString().compareTo(other.toString())
}

// 한 AcceptCondition 이 참조하는 PathRoot 들의 집합. memory 절약 위해 size 별 다른 representation.
sealed class RootSet {
  abstract val size: Int
  abstract operator fun contains(root: PathRoot): Boolean
  abstract fun forEach(action: (PathRoot) -> Unit)
  // 다른 RootSet 과 union — disjoint 검사 같은 hot path 에서 사용.
  abstract fun isDisjoint(other: Set<PathRoot>): Boolean

  data object Empty: RootSet() {
    override val size: Int get() = 0
    override fun contains(root: PathRoot): Boolean = false
    override fun forEach(action: (PathRoot) -> Unit) {}
    override fun isDisjoint(other: Set<PathRoot>): Boolean = true
  }

  class Single(val root: PathRoot): RootSet() {
    override val size: Int get() = 1
    override fun contains(root: PathRoot): Boolean = this.root == root
    override fun forEach(action: (PathRoot) -> Unit) { action(root) }
    override fun isDisjoint(other: Set<PathRoot>): Boolean = root !in other
  }

  class Many(val set: Set<PathRoot>): RootSet() {
    override val size: Int get() = set.size
    override fun contains(root: PathRoot): Boolean = root in set
    override fun forEach(action: (PathRoot) -> Unit) { set.forEach(action) }
    override fun isDisjoint(other: Set<PathRoot>): Boolean {
      if (set.size <= other.size) {
        for (r in set) if (r in other) return false
      } else {
        for (r in other) if (r in set) return false
      }
      return true
    }
  }

  companion object {
    fun unionOf(parts: Array<AcceptCondition>): RootSet {
      var combined: HashSet<PathRoot>? = null
      var single: PathRoot? = null
      for (p in parts) {
        val r = p.referencedRoots
        when (r) {
          Empty -> {}
          is Single -> {
            if (combined != null) combined.add(r.root)
            else if (single == null) single = r.root
            else if (single != r.root) {
              combined = HashSet(); combined.add(single); combined.add(r.root); single = null
            }
          }
          is Many -> {
            if (combined == null) {
              combined = HashSet(r.set.size + (if (single != null) 1 else 0))
              if (single != null) combined.add(single)
              single = null
            }
            combined.addAll(r.set)
          }
        }
      }
      return when {
        combined != null -> Many(combined)
        single != null -> Single(single)
        else -> Empty
      }
    }
    fun unionOfPair(a: AcceptCondition, b: AcceptCondition): RootSet {
      val ra = a.referencedRoots
      val rb = b.referencedRoots
      return when {
        ra is Empty -> rb
        rb is Empty -> ra
        ra is Single && rb is Single -> if (ra.root == rb.root) ra else {
          val s = HashSet<PathRoot>(2); s.add(ra.root); s.add(rb.root); Many(s)
        }
        else -> {
          val s = HashSet<PathRoot>(ra.size + rb.size)
          ra.forEach { s.add(it) }
          rb.forEach { s.add(it) }
          Many(s)
        }
      }
    }

    // SmallAnd/SmallOr (size 2/3/4) 의 referencedRoots 계산용. nullable c, d 받음.
    fun unionOfChildren(
      a: AcceptCondition,
      b: AcceptCondition,
      c: AcceptCondition?,
      d: AcceptCondition?,
    ): RootSet {
      if (c == null) return unionOfPair(a, b)
      val children: Array<AcceptCondition> = if (d == null) arrayOf(a, b, c) else arrayOf(a, b, c, d)
      return unionOf(children)
    }
  }
}

data object Always: AcceptCondition() {
  override fun neg(): AcceptCondition = Never
  override val referencedRoots: RootSet get() = RootSet.Empty
  override val hasFromNextGen: Boolean get() = false
}

data object Never: AcceptCondition() {
  override fun neg(): AcceptCondition = Always
  override val referencedRoots: RootSet get() = RootSet.Empty
  override val hasFromNextGen: Boolean get() = false
}

// And: AND of multiple AcceptConditions. 자식 element 가 2/3/4 면 SmallAnd, 5+ 면 LargeAnd 로 표현.
// 둘 다 elements 가 canonical order (hashCode 오름차순 + tie-break by toString) 로 정렬되어 있음.
// 직접 instantiate 하지 말고 `And.from(...)` 으로.
sealed class And: AcceptCondition() {
  abstract val size: Int
  abstract fun elementAt(i: Int): AcceptCondition
  inline fun forEach(action: (AcceptCondition) -> Unit) {
    for (i in 0 until size) action(elementAt(i))
  }

  companion object {
    fun from(a: AcceptCondition, b: AcceptCondition): AcceptCondition {
      // Most common case — micro-optimized fast path.
      if (a == Never || b == Never) return Never
      if (a == Always) return b
      if (b == Always) return a
      if (a == b) return a
      // 둘 다 leaf 면 SmallAnd2 directly.
      if (a !is And && b !is And) {
        val (x, y) = canonicalPair(a, b)
        return SmallAnd(x, y, null, null)
      }
      return from(listOf(a, b))
    }

    fun from(conds: Collection<AcceptCondition>): AcceptCondition {
      val flat = ArrayList<AcceptCondition>(conds.size * 2)
      for (c in conds) {
        when (c) {
          Never -> return Never
          Always -> {}
          is SmallAnd -> c.forEach { flat.add(it) }
          is LargeAnd -> c.forEach { flat.add(it) }
          else -> flat.add(c)
        }
      }
      // dedup
      val deduped = if (flat.size <= 1) flat else flat.toCollection(LinkedHashSet()).toList()
      return when (deduped.size) {
        0 -> Always
        1 -> deduped[0]
        else -> {
          val arr = deduped.toTypedArray()
          canonicalSort(arr)
          when (arr.size) {
            2 -> SmallAnd(arr[0], arr[1], null, null)
            3 -> SmallAnd(arr[0], arr[1], arr[2], null)
            4 -> SmallAnd(arr[0], arr[1], arr[2], arr[3])
            else -> LargeAnd(arr)
          }
        }
      }
    }
  }
}

// SmallAnd: size 2/3/4. d != null → 4, c != null → 3, else 2.
class SmallAnd(
  val a: AcceptCondition,
  val b: AcceptCondition,
  val c: AcceptCondition?,
  val d: AcceptCondition?,
) : And() {
  override val size: Int get() = if (d != null) 4 else if (c != null) 3 else 2
  override fun elementAt(i: Int): AcceptCondition = when (i) {
    0 -> a; 1 -> b; 2 -> c!!; 3 -> d!!
    else -> throw IndexOutOfBoundsException(i)
  }

  private val _hash: Int = run {
    var h = HASH_SEED_AND
    h = h * 31 + a.hashCode()
    h = h * 31 + b.hashCode()
    if (c != null) h = h * 31 + c.hashCode()
    if (d != null) h = h * 31 + d.hashCode()
    h
  }
  override fun hashCode(): Int = _hash
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SmallAnd) return false
    return a == other.a && b == other.b && c == other.c && d == other.d
  }
  override fun toString(): String = "And(${(0 until size).joinToString(", ") { elementAt(it).toString() }})"

  override val referencedRoots: RootSet = RootSet.unionOfChildren(a, b, c, d)
  override val hasFromNextGen: Boolean = a.hasFromNextGen || b.hasFromNextGen ||
    (c?.hasFromNextGen == true) || (d?.hasFromNextGen == true)

  override fun neg(): AcceptCondition = Or.from(buildList(size) {
    add(a.neg()); add(b.neg())
    c?.let { add(it.neg()) }
    d?.let { add(it.neg()) }
  })
}

// LargeAnd: size >= 5. items canonical-sorted.
class LargeAnd(val items: Array<AcceptCondition>) : And() {
  override val size: Int get() = items.size
  override fun elementAt(i: Int): AcceptCondition = items[i]

  private val _hash: Int = run {
    var h = HASH_SEED_AND
    for (e in items) h = h * 31 + e.hashCode()
    h
  }
  override fun hashCode(): Int = _hash
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is LargeAnd) return false
    return items.contentEquals(other.items)
  }
  override fun toString(): String = "And(${items.joinToString(", ")})"

  override val referencedRoots: RootSet = RootSet.unionOf(items)
  override val hasFromNextGen: Boolean = items.any { it.hasFromNextGen }

  override fun neg(): AcceptCondition = Or.from(items.map { it.neg() })
}

// Or: 대칭 구조.
sealed class Or: AcceptCondition() {
  abstract val size: Int
  abstract fun elementAt(i: Int): AcceptCondition
  inline fun forEach(action: (AcceptCondition) -> Unit) {
    for (i in 0 until size) action(elementAt(i))
  }

  companion object {
    fun from(a: AcceptCondition, b: AcceptCondition): AcceptCondition {
      if (a == Always || b == Always) return Always
      if (a == Never) return b
      if (b == Never) return a
      if (a == b) return a
      if (a !is Or && b !is Or) {
        val (x, y) = canonicalPair(a, b)
        return SmallOr(x, y, null, null)
      }
      return from(listOf(a, b))
    }

    fun from(conds: Collection<AcceptCondition>): AcceptCondition {
      val flat = ArrayList<AcceptCondition>(conds.size * 2)
      for (c in conds) {
        when (c) {
          Always -> return Always
          Never -> {}
          is SmallOr -> c.forEach { flat.add(it) }
          is LargeOr -> c.forEach { flat.add(it) }
          else -> flat.add(c)
        }
      }
      val deduped = if (flat.size <= 1) flat else flat.toCollection(LinkedHashSet()).toList()
      return when (deduped.size) {
        0 -> Never
        1 -> deduped[0]
        else -> {
          val arr = deduped.toTypedArray()
          canonicalSort(arr)
          when (arr.size) {
            2 -> SmallOr(arr[0], arr[1], null, null)
            3 -> SmallOr(arr[0], arr[1], arr[2], null)
            4 -> SmallOr(arr[0], arr[1], arr[2], arr[3])
            else -> LargeOr(arr)
          }
        }
      }
    }
  }
}

class SmallOr(
  val a: AcceptCondition,
  val b: AcceptCondition,
  val c: AcceptCondition?,
  val d: AcceptCondition?,
) : Or() {
  override val size: Int get() = if (d != null) 4 else if (c != null) 3 else 2
  override fun elementAt(i: Int): AcceptCondition = when (i) {
    0 -> a; 1 -> b; 2 -> c!!; 3 -> d!!
    else -> throw IndexOutOfBoundsException(i)
  }

  private val _hash: Int = run {
    var h = HASH_SEED_OR
    h = h * 31 + a.hashCode()
    h = h * 31 + b.hashCode()
    if (c != null) h = h * 31 + c.hashCode()
    if (d != null) h = h * 31 + d.hashCode()
    h
  }
  override fun hashCode(): Int = _hash
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SmallOr) return false
    return a == other.a && b == other.b && c == other.c && d == other.d
  }
  override fun toString(): String = "Or(${(0 until size).joinToString(", ") { elementAt(it).toString() }})"

  override val referencedRoots: RootSet = RootSet.unionOfChildren(a, b, c, d)
  override val hasFromNextGen: Boolean = a.hasFromNextGen || b.hasFromNextGen ||
    (c?.hasFromNextGen == true) || (d?.hasFromNextGen == true)

  override fun neg(): AcceptCondition = And.from(buildList(size) {
    add(a.neg()); add(b.neg())
    c?.let { add(it.neg()) }
    d?.let { add(it.neg()) }
  })
}

class LargeOr(val items: Array<AcceptCondition>) : Or() {
  override val size: Int get() = items.size
  override fun elementAt(i: Int): AcceptCondition = items[i]

  private val _hash: Int = run {
    var h = HASH_SEED_OR
    for (e in items) h = h * 31 + e.hashCode()
    h
  }
  override fun hashCode(): Int = _hash
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is LargeOr) return false
    return items.contentEquals(other.items)
  }
  override fun toString(): String = "Or(${items.joinToString(", ")})"

  override val referencedRoots: RootSet = RootSet.unionOf(items)
  override val hasFromNextGen: Boolean = items.any { it.hasFromNextGen }

  override fun neg(): AcceptCondition = And.from(items.map { it.neg() })
}

// canonical ordering helpers — hashCode-sorted, tie-break by toString (rare path).
private const val HASH_SEED_AND: Int = 0x6B5F3A7
private const val HASH_SEED_OR: Int = 0x3D9F7B1

private fun canonicalPair(a: AcceptCondition, b: AcceptCondition): Pair<AcceptCondition, AcceptCondition> {
  val ha = a.hashCode()
  val hb = b.hashCode()
  return when {
    ha < hb -> Pair(a, b)
    ha > hb -> Pair(b, a)
    else -> {
      val cmp = a.toString().compareTo(b.toString())
      if (cmp <= 0) Pair(a, b) else Pair(b, a)
    }
  }
}

private fun canonicalSort(arr: Array<AcceptCondition>) {
  arr.sortWith { a, b ->
    val ha = a.hashCode()
    val hb = b.hashCode()
    if (ha != hb) Integer.compare(ha, hb) else a.toString().compareTo(b.toString())
  }
}

// leaf conditions — referencedRoots = Single(self root).
// NoLongerMatch/NeedLongerMatch: minEndGen = 이 조건이 인정하는 finish 의 최소 end gen.
//   longest 의 "본문보다 strictly 긴 매치만" 이 본문 end+1 로 표현된다 (이전의
//   fromNextGen 1-bit 를 명시적 end 경계로 대체 — 같은 end 의 finish 를 흡수해
//   span 이 어긋나는 버그 방지).
// Unless/OnlyIf: endGen = 감시 대상이 정확히 매치해야 하는 span 의 end gen.
//   bounded shape 이므로 endGen 의 finish 만 보고 resolve 된다.
data class NoLongerMatch(val symbolId: Int, val startGen: Int, val minEndGen: Int): AcceptCondition() {
  override fun neg(): AcceptCondition = NeedLongerMatch(symbolId, startGen, minEndGen)
  override val referencedRoots: RootSet = RootSet.Single(PathRoot(symbolId, startGen))
  override val hasFromNextGen: Boolean get() = false
}

data class NeedLongerMatch(val symbolId: Int, val startGen: Int, val minEndGen: Int):
  AcceptCondition() {
  override fun neg(): AcceptCondition = NoLongerMatch(symbolId, startGen, minEndGen)
  override val referencedRoots: RootSet = RootSet.Single(PathRoot(symbolId, startGen))
  override val hasFromNextGen: Boolean get() = false
}

data class NotExists(val symbolId: Int, val startGen: Int): AcceptCondition() {
  override fun neg(): AcceptCondition = Exists(symbolId, startGen)
  override val referencedRoots: RootSet = RootSet.Single(PathRoot(symbolId, startGen))
  override val hasFromNextGen: Boolean get() = false
}

data class Exists(val symbolId: Int, val startGen: Int): AcceptCondition() {
  override fun neg(): AcceptCondition = NotExists(symbolId, startGen)
  override val referencedRoots: RootSet = RootSet.Single(PathRoot(symbolId, startGen))
  override val hasFromNextGen: Boolean get() = false
}

data class Unless(val symbolId: Int, val startGen: Int, val endGen: Int): AcceptCondition() {
  override fun neg(): AcceptCondition = OnlyIf(symbolId, startGen, endGen)
  override val referencedRoots: RootSet = RootSet.Single(PathRoot(symbolId, startGen))
  override val hasFromNextGen: Boolean get() = false
}

data class OnlyIf(val symbolId: Int, val startGen: Int, val endGen: Int): AcceptCondition() {
  override fun neg(): AcceptCondition = Unless(symbolId, startGen, endGen)
  override val referencedRoots: RootSet = RootSet.Single(PathRoot(symbolId, startGen))
  override val hasFromNextGen: Boolean get() = false
}

// 입력이 모두 처리된 후, 이 condition이 true로 평가될 수 있는지 확인.
fun evaluateAcceptCondition(
  cond: AcceptCondition,
  condPathFins: Map<PathRoot, AcceptCondition>,
  activeCondPaths: Set<PathRoot>,
): Boolean = when (cond) {
  Always -> true
  Never -> false
  is And -> {
    var result = true
    cond.forEach { if (!evaluateAcceptCondition(it, condPathFins, activeCondPaths)) result = false }
    result
  }
  is Or -> {
    var result = false
    cond.forEach { if (evaluateAcceptCondition(it, condPathFins, activeCondPaths)) result = true }
    result
  }

  is NoLongerMatch -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null) {
      !evaluateAcceptCondition(finCond, condPathFins, activeCondPaths)
    } else {
      true
    }
  }

  is NeedLongerMatch -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null) evaluateAcceptCondition(finCond, condPathFins, activeCondPaths) else false
  }

  is NotExists -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null) !evaluateAcceptCondition(finCond, condPathFins, activeCondPaths) else true
  }

  is Exists -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null) evaluateAcceptCondition(finCond, condPathFins, activeCondPaths) else false
  }

  is Unless -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null) !evaluateAcceptCondition(finCond, condPathFins, activeCondPaths) else true
  }

  is OnlyIf -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null) evaluateAcceptCondition(finCond, condPathFins, activeCondPaths) else false
  }
}

var evolveTrace: Boolean = false

// condPathFins: 이번 step 에서 *이번 gen 에 끝나는* finish (eager — replaceAndProgress 류).
// lateCondPathFins: 이번 step 에서 발견됐지만 *직전 gen (gen-1) 에 끝난* finish —
//   cond path 가 이번 input 을 매치하지 못해 죽으면서 possible_finishes 로 등록한 것.
//   두 채널의 end gen 이 다르므로 분리해서 전달해야 bounded shape (Unless/OnlyIf) 와
//   longest (NoLongerMatch) 가 정확한 span 으로 discharge 된다.
fun evolveAcceptCondition(
  cond: AcceptCondition,
  condPathFins: Map<PathRoot, AcceptCondition>,
  lateCondPathFins: Map<PathRoot, AcceptCondition>,
  activeCondPaths: Set<PathRoot>,
  gen: Int,
): AcceptCondition {
  if (evolveTrace) {
    println("EV TOP  cond=${cond.toString().take(300)}")
  }
  val r = evolveAcceptCondition(cond, condPathFins, lateCondPathFins, activeCondPaths, gen, emptySet(), 0)
  if (evolveTrace) println("EV TOP-> ${r.toString().take(300)}")
  return r
}

private fun evolveAcceptCondition(
  cond: AcceptCondition,
  condPathFins: Map<PathRoot, AcceptCondition>,
  lateCondPathFins: Map<PathRoot, AcceptCondition>,
  activeCondPaths: Set<PathRoot>,
  gen: Int,
  visiting: Set<PathRoot>,
  depth: Int = 0,
): AcceptCondition {
  fun rec(c: AcceptCondition, v: Set<PathRoot> = visiting): AcceptCondition =
    evolveAcceptCondition(c, condPathFins, lateCondPathFins, activeCondPaths, gen, v, depth + 1)
  val indent = "  ".repeat(depth)
  if (evolveTrace) println("${indent}EV($depth) $cond visiting=$visiting")
  val result: AcceptCondition = when (cond) {
    Always -> cond
    Never -> cond
    is And -> {
      val list = ArrayList<AcceptCondition>(cond.size)
      cond.forEach { list.add(rec(it)) }
      And.from(list)
    }
    is Or -> {
      val list = ArrayList<AcceptCondition>(cond.size)
      cond.forEach { list.add(rec(it)) }
      Or.from(list)
    }

    // longest: 본문 end 이후 (minEndGen 이상) 에 끝나는 매치가 없어야 한다.
    // eager fin 의 end = gen, late fin 의 end = gen-1 — 각각 minEndGen 과 비교해
    // 정확한 end 의 finish 만 흡수한다. root 가 살아있는 동안은 흡수 후에도
    // pending 을 유지한다 (이전 구현은 fin 흡수 시 pending 을 버려서 이후
    // 매치 관찰 의무가 사라졌다).
    is NoLongerMatch -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      if (root in visiting) Always
      else {
        val parts = ArrayList<AcceptCondition>(3)
        val eagerFin = if (gen >= cond.minEndGen) condPathFins[root] else null
        val lateFin = if (gen - 1 >= cond.minEndGen) lateCondPathFins[root] else null
        if (eagerFin != null) parts.add(rec(eagerFin.neg(), visiting + root))
        if (lateFin != null) parts.add(rec(lateFin.neg(), visiting + root))
        if (root in activeCondPaths) parts.add(cond)
        if (parts.isEmpty()) Always else And.from(parts)
      }
    }

    is NeedLongerMatch -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      if (root in visiting) Always
      else {
        val parts = ArrayList<AcceptCondition>(3)
        val eagerFin = if (gen >= cond.minEndGen) condPathFins[root] else null
        val lateFin = if (gen - 1 >= cond.minEndGen) lateCondPathFins[root] else null
        if (eagerFin != null) parts.add(rec(eagerFin, visiting + root))
        if (lateFin != null) parts.add(rec(lateFin, visiting + root))
        if (root in activeCondPaths) parts.add(cond)
        if (parts.isEmpty()) Never else Or.from(parts)
      }
    }

    // lookahead: startGen 에서 시작하는 매치가 (end 무관하게) 존재하는지 — eager 와
    // late fin 모두 유효하다. root 가 살아있으면 pending 유지.
    is NotExists -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      if (root in visiting) cond
      else {
        val parts = ArrayList<AcceptCondition>(3)
        condPathFins[root]?.let { parts.add(rec(it.neg(), visiting + root)) }
        lateCondPathFins[root]?.let { parts.add(rec(it.neg(), visiting + root)) }
        if (root in activeCondPaths) parts.add(cond)
        if (parts.isEmpty()) Always else And.from(parts)
      }
    }

    is Exists -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      if (root in visiting) cond
      else {
        val parts = ArrayList<AcceptCondition>(3)
        condPathFins[root]?.let { parts.add(rec(it, visiting + root)) }
        lateCondPathFins[root]?.let { parts.add(rec(it, visiting + root)) }
        if (root in activeCondPaths) parts.add(cond)
        if (parts.isEmpty()) Never else Or.from(parts)
      }
    }

    // bounded (except/join): 정확히 (startGen, endGen) span 의 매치 여부로만 결정.
    // endGen 에 끝나는 finish 는 eager 로는 gen == endGen 에, late (죽으면서 등록)
    // 로는 gen == endGen+1 에 관찰된다. 그 외의 finish 는 다른 span — 무시.
    is Unless -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      if (root in visiting) cond
      else when {
        gen < cond.endGen -> cond
        gen == cond.endGen -> {
          val finCond = condPathFins[root]
          when {
            finCond != null -> rec(finCond.neg(), visiting + root)
            root in activeCondPaths -> cond // 죽으면서 등록되는 late fin 가능성 — 한 step 만 더 대기
            else -> Always
          }
        }
        gen == cond.endGen + 1 -> {
          val lateFin = lateCondPathFins[root]
          if (lateFin != null) rec(lateFin.neg(), visiting + root) else Always
        }
        else -> Always
      }
    }

    is OnlyIf -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      if (root in visiting) cond
      else when {
        gen < cond.endGen -> cond
        gen == cond.endGen -> {
          val finCond = condPathFins[root]
          when {
            finCond != null -> rec(finCond, visiting + root)
            root in activeCondPaths -> cond
            else -> Never
          }
        }
        gen == cond.endGen + 1 -> {
          val lateFin = lateCondPathFins[root]
          if (lateFin != null) rec(lateFin, visiting + root) else Never
        }
        else -> Never
      }
    }
  }
  if (evolveTrace) println("${indent}EV($depth)-> $result")
  return result
}
