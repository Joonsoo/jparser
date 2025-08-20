package com.giyeok.jparser.mgroup3

sealed class AcceptCondition {
  abstract fun neg(): AcceptCondition
}

data object Always: AcceptCondition() {
  override fun neg(): AcceptCondition = Never
}

data object Never: AcceptCondition() {
  override fun neg(): AcceptCondition = Always
}

data class And(val conds: Set<AcceptCondition>): AcceptCondition() {
  companion object {
    fun from(conds: Set<AcceptCondition>): AcceptCondition =
      if (Never in conds) {
        Never
      } else {
        val filtered = conds.filter { it != Always }
        if (filtered.isEmpty()) {
          Always
        } else {
          if (filtered.size == 1) {
            filtered.first()
          } else {
            val elems = filtered.flatMap {
              when (it) {
                is And -> it.conds
                else -> listOf(it)
              }
            }
            And(elems.toSet())
          }
        }
      }
  }

  override fun neg(): AcceptCondition = Or(conds.map { it.neg() }.toSet())
}

data class Or(val conds: Set<AcceptCondition>): AcceptCondition() {
  companion object {
    fun from(conds: Set<AcceptCondition>): AcceptCondition =
      if (Always in conds) {
        Always
      } else {
        val filtered = conds.filter { it != Never }
        if (filtered.isEmpty()) {
          Never
        } else {
          if (filtered.size == 1) {
            filtered.first()
          } else {
            val elems = filtered.flatMap {
              when (it) {
                is Or -> it.conds
                else -> listOf(it)
              }
            }
            Or(elems.toSet())
          }
        }
      }
  }

  override fun neg(): AcceptCondition = And(conds.map { it.neg() }.toSet())
}

// evolve:
//   이번 gen이 endGen과 같으면 그대로 유지하고,
//   그렇지 않은 경우
//   cond paths의 (symbolId, startGen) 심볼이 finish되었으면 finish된 조건의 neg()로 치환
//   그렇지 않으면
//   cond paths에 (symbolId, startGen) 심볼에서 시작하는 경로가 살아남아 있으면 계속 유지하고,
//   사라지면 Always로 치환
data class NoLongerMatch(val symbolId: Int, val startGen: Int, val endGen: Int): AcceptCondition() {
  override fun neg(): AcceptCondition = NeedLongerMatch(symbolId, startGen, endGen)
}

// evolve:
//   이번 gen이 endGen과 같으면 그대로 유지하고,
//   그렇지 않은 경우
//   cond paths의 (symbolId, startGen) 심볼이 finish되었으면 finish된 조건으로 치환
//   그렇지 않으면
//   cond paths에 (symbolId, startGen) 심볼에서 시작하는 경로가 살아남아 있으면 계속 유지하고,
//   사라지면 Never로 치환
data class NeedLongerMatch(val symbolId: Int, val startGen: Int, val endGen: Int):
  AcceptCondition() {
  override fun neg(): AcceptCondition = NoLongerMatch(symbolId, startGen, endGen)
}

// evolve:
//   cond paths의 (symbolId, startGen) 심볼이 finish되었으면 finish된 조건의 neg()로 치환
//   그렇지 않으면,
//   cond paths에 (symbolId, startGen) 심볼에서 시작하는 경로가 살아남아 있으면 계속 유지하고,
//   사라지면 Always로 치환
data class NotExists(val symbolId: Int, val startGen: Int): AcceptCondition() {
  override fun neg(): AcceptCondition = Exists(symbolId, startGen)
}

// evolve:
//   cond paths의 (symbolId, startGen) 심볼이 finish되었으면 finish된 조건으로 치환
//   그렇지 않으면,
//   cond paths에 (symbolId, startGen) 심볼에서 시작하는 경로가 살아남아 있으면 계속 유지하고,
//   사라지면 Never로 치환
data class Exists(val symbolId: Int, val startGen: Int): AcceptCondition() {
  override fun neg(): AcceptCondition = NotExists(symbolId, startGen)
}

// evolve:
//   cond paths의 (symbolId, startGen) 심볼이 finish되었으면 finish된 조건의 neg()로 치환
//   그렇지 않으면, Always로 치환
data class Unless(val symbolId: Int, val startGen: Int): AcceptCondition() {
  override fun neg(): AcceptCondition = OnlyIf(symbolId, startGen)
}

// evolve:
//   cond paths의 (symbolId, startGen) 심볼이 finish되었으면 finish된 조건으로 치환
//   그렇지 않으면, Never로 치환
data class OnlyIf(val symbolId: Int, val startGen: Int): AcceptCondition() {
  override fun neg(): AcceptCondition = Unless(symbolId, startGen)
}

fun evaluateAcceptCondition(
  cond: AcceptCondition,
  condPathFins: Map<PathRoot, AcceptCondition>
): Boolean = when (cond) {
  Always -> true
  Never -> false
  is And -> cond.conds.all { evaluateAcceptCondition(it, condPathFins) }
  is Or -> cond.conds.any { evaluateAcceptCondition(it, condPathFins) }

  is NoLongerMatch -> true
  is NeedLongerMatch -> false

  is NotExists -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    condPathFins[root]?.let { !evaluateAcceptCondition(it, condPathFins) } ?: false
  }

  is Exists -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    condPathFins[root]?.let { evaluateAcceptCondition(it, condPathFins) } ?: true
  }

  is Unless -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    condPathFins[root]?.let { !evaluateAcceptCondition(it, condPathFins) } ?: false
  }

  is OnlyIf -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    condPathFins[root]?.let { evaluateAcceptCondition(it, condPathFins) } ?: true
  }
}

fun evolveAcceptCondition(
  cond: AcceptCondition,
  condPathFins: Map<PathRoot, AcceptCondition>,
  activeCondPaths: Set<PathRoot>,
  gen: Int,
): AcceptCondition = when (cond) {
  Always -> cond
  Never -> cond
  is And -> And.from(cond.conds.map {
    evolveAcceptCondition(it, condPathFins, activeCondPaths, gen)
  }.toSet())

  is Or -> Or.from(cond.conds.map {
    evolveAcceptCondition(it, condPathFins, activeCondPaths, gen)
  }.toSet())

  is NoLongerMatch -> {
    if (gen == cond.endGen) {
      cond
    } else {
      val root = PathRoot(cond.symbolId, cond.startGen)
      condPathFins[root]?.neg() ?: (if (root in activeCondPaths) cond else Always)
    }
  }

  is NeedLongerMatch -> {
    if (gen == cond.endGen) {
      cond
    } else {
      val root = PathRoot(cond.symbolId, cond.startGen)
      condPathFins[root] ?: (if (root in activeCondPaths) cond else Never)
    }
  }

  is NotExists -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    condPathFins[root]?.neg() ?: (if (root in activeCondPaths) cond else Always)
  }

  is Exists -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    condPathFins[root] ?: (if (root in activeCondPaths) cond else Never)
  }

  is Unless -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    condPathFins[root]?.neg() ?: Always
  }

  is OnlyIf -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    condPathFins[root] ?: Never
  }
}
