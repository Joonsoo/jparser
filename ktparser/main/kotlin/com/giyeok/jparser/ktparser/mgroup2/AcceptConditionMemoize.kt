package com.giyeok.jparser.ktparser.mgroup2

class AcceptConditionMemoize {
  private val memoMap = mutableMapOf<MilestoneAcceptConditionKt, Boolean>()

  fun useMemo(condition: MilestoneAcceptConditionKt, func: () -> Boolean): Boolean {
    val existing = memoMap[condition]
    return if (existing != null) {
      existing
    } else {
      val newValue = func()
      memoMap[condition] = newValue
      newValue
    }
  }
}
