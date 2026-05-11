package com.giyeok.jparser.mgroup3

sealed class AcceptCondition: Comparable<AcceptCondition> {
  abstract fun neg(): AcceptCondition

  override fun compareTo(other: AcceptCondition): Int =
    this.toString().compareTo(other.toString())
}

data object Always: AcceptCondition() {
  override fun neg(): AcceptCondition = Never
}

data object Never: AcceptCondition() {
  override fun neg(): AcceptCondition = Always
}

data class And(val conds: Set<AcceptCondition>): AcceptCondition() {
  companion object {
    fun from(a: AcceptCondition, b: AcceptCondition): AcceptCondition =
      from(setOf(a, b))

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

  override fun neg(): AcceptCondition = Or.from(conds.map { it.neg() }.toSet())
}

data class Or(val conds: Set<AcceptCondition>): AcceptCondition() {
  companion object {
    fun from(a: AcceptCondition, b: AcceptCondition): AcceptCondition =
      from(setOf(a, b))

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

  override fun neg(): AcceptCondition = And.from(conds.map { it.neg() }.toSet())
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

// 입력이 모두 처리된 후, 이 condition이 true로 평가될 수 있는지 확인
// condPathFins는 마지막에 finish된 cond path들의 finish condition map
// activeCondPaths는 입력 끝까지 살아남은 cond path들
fun evaluateAcceptCondition(
  cond: AcceptCondition,
  condPathFins: Map<PathRoot, AcceptCondition>,
  activeCondPaths: Set<PathRoot>,
): Boolean = when (cond) {
  Always -> true
  Never -> false
  is And -> cond.conds.all { evaluateAcceptCondition(it, condPathFins, activeCondPaths) }
  is Or -> cond.conds.any { evaluateAcceptCondition(it, condPathFins, activeCondPaths) }

  is NoLongerMatch -> {
    // longest: 더 길게 매치되는 경우가 없으면 true
    val root = PathRoot(cond.symbolId, cond.startGen)
    // 이미 finish된 적이 있다면 그 finish condition이 false로 나와야 true (즉 더 긴 매치는 invalid)
    // 그러나 finish 후에도 cond path가 더 살아남아 있을 수 있는데 이 경우 evolveAcceptCondition에서
    // 이미 처리된 후의 결과가 들어옴. 여기서는 단순히 마지막 상태에서 평가
    val finCond = condPathFins[root]
    if (finCond != null) {
      !evaluateAcceptCondition(finCond, condPathFins, activeCondPaths)
    } else {
      // active이거나 이미 사라졌거나 모두 더 긴 매치가 없는 것으로 간주
      true
    }
  }

  is NeedLongerMatch -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null) {
      evaluateAcceptCondition(finCond, condPathFins, activeCondPaths)
    } else {
      false
    }
  }

  is NotExists -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null) {
      !evaluateAcceptCondition(finCond, condPathFins, activeCondPaths)
    } else {
      // 한 번도 finish되지 않았으므로 lookahead-not 성립
      true
    }
  }

  is Exists -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null) {
      evaluateAcceptCondition(finCond, condPathFins, activeCondPaths)
    } else {
      false
    }
  }

  is Unless -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null) {
      !evaluateAcceptCondition(finCond, condPathFins, activeCondPaths)
    } else {
      true
    }
  }

  is OnlyIf -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null) {
      evaluateAcceptCondition(finCond, condPathFins, activeCondPaths)
    } else {
      false
    }
  }
}

// step별로 cond condition을 진화시킴.
// condPathFins: 이번 step까지 누적된 finish condition map (모든 종류 포함)
// activeCondPaths: 이번 step 후에 살아남은 cond path들의 root
// gen: 이번 step의 gen
fun evolveAcceptCondition(
  cond: AcceptCondition,
  condPathFins: Map<PathRoot, AcceptCondition>,
  activeCondPaths: Set<PathRoot>,
  gen: Int,
): AcceptCondition = evolveAcceptCondition(cond, condPathFins, activeCondPaths, gen, emptySet())

// visiting: 현재 expansion 경로 상에서 finCond 로 대체된 PathRoot 들의 집합.
// 같은 root 가 다시 등장하면 무한 recursion 방지 위해 condition 그대로 유지.
private fun evolveAcceptCondition(
  cond: AcceptCondition,
  condPathFins: Map<PathRoot, AcceptCondition>,
  activeCondPaths: Set<PathRoot>,
  gen: Int,
  visiting: Set<PathRoot>,
): AcceptCondition = when (cond) {
  Always -> cond
  Never -> cond
  is And -> And.from(cond.conds.map {
    evolveAcceptCondition(it, condPathFins, activeCondPaths, gen, visiting)
  }.toSet())

  is Or -> Or.from(cond.conds.map {
    evolveAcceptCondition(it, condPathFins, activeCondPaths, gen, visiting)
  }.toSet())

  is NoLongerMatch -> {
    if (gen == cond.endGen) {
      cond
    } else {
      val root = PathRoot(cond.symbolId, cond.startGen)
      val finCond = condPathFins[root]
      if (finCond != null && root !in visiting) {
        // 더 긴 매치가 있었다면 그 condition의 negation을 적용
        evolveAcceptCondition(finCond.neg(), condPathFins, activeCondPaths, gen, visiting + root)
      } else if (root in activeCondPaths) {
        cond
      } else if (finCond != null) {
        // cycle: finCond 가 자기 자신을 참조. condition 유지하여 다음 step 에서 재시도.
        cond
      } else {
        Always
      }
    }
  }

  is NeedLongerMatch -> {
    if (gen == cond.endGen) {
      cond
    } else {
      val root = PathRoot(cond.symbolId, cond.startGen)
      val finCond = condPathFins[root]
      if (finCond != null && root !in visiting) {
        evolveAcceptCondition(finCond, condPathFins, activeCondPaths, gen, visiting + root)
      } else if (root in activeCondPaths) {
        cond
      } else if (finCond != null) {
        cond
      } else {
        Never
      }
    }
  }

  is NotExists -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null && root !in visiting) {
      evolveAcceptCondition(finCond.neg(), condPathFins, activeCondPaths, gen, visiting + root)
    } else if (root in activeCondPaths) {
      cond
    } else if (finCond != null) {
      cond
    } else {
      Always
    }
  }

  is Exists -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null && root !in visiting) {
      evolveAcceptCondition(finCond, condPathFins, activeCondPaths, gen, visiting + root)
    } else if (root in activeCondPaths) {
      cond
    } else if (finCond != null) {
      cond
    } else {
      Never
    }
  }

  is Unless -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null && root !in visiting) {
      evolveAcceptCondition(finCond.neg(), condPathFins, activeCondPaths, gen, visiting + root)
    } else if (root in activeCondPaths) {
      // 아직 finish 가능성이 살아 있음. 유지.
      cond
    } else if (finCond != null) {
      cond
    } else {
      // 더 이상 except의 가능성이 없으므로 Always
      Always
    }
  }

  is OnlyIf -> {
    val root = PathRoot(cond.symbolId, cond.startGen)
    val finCond = condPathFins[root]
    if (finCond != null && root !in visiting) {
      evolveAcceptCondition(finCond, condPathFins, activeCondPaths, gen, visiting + root)
    } else if (root in activeCondPaths) {
      // 아직 finish 가능성이 살아 있음. 유지.
      cond
    } else if (finCond != null) {
      cond
    } else {
      Never
    }
  }
}
