package com.giyeok.jparser.ktparser.mgroup2

import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto

sealed class MilestoneAcceptConditionKt {
  companion object {
    fun reify(
      template: MilestoneParserDataProto.AcceptConditionTemplate,
      beginGen: Int,
      gen: Int,
    ): MilestoneAcceptConditionKt =
      when (template.conditionCase) {
        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.ALWAYS -> Always

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.NEVER -> Never

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.AND ->
          And(template.and.conditionsList.map { reify(it, beginGen, gen) })

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.OR ->
          Or(template.and.conditionsList.map { reify(it, beginGen, gen) })

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.LOOKAHEAD_IS ->
          Exists(
            MilestoneKt(template.lookaheadIs.symbolId, 0, gen),
            template.lookaheadIs.fromNextGen
          )

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.LOOKAHEAD_NOT ->
          NotExists(
            MilestoneKt(template.lookaheadNot.symbolId, 0, gen),
            template.lookaheadNot.fromNextGen
          )

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.LONGEST ->
          NotExists(MilestoneKt(template.longest, 0, beginGen), true)

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.ONLY_IF ->
          OnlyIf(MilestoneKt(template.onlyIf, 0, beginGen))

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.UNLESS ->
          Unless(MilestoneKt(template.unless, 0, beginGen))

        else -> throw AssertionError("")
      }

    fun conjunct(vararg conditions: MilestoneAcceptConditionKt): MilestoneAcceptConditionKt =
      if (conditions.contains(Never)) {
        Never
      } else {
        val filtered = conditions.filter { it != Always }
        if (filtered.isEmpty()) {
          Always
        } else {
          if (filtered.size == 1) {
            filtered.first()
          } else {
            val elems = filtered.flatMap {
              when (it) {
                is And -> it.conditions
                else -> listOf(it)
              }
            }
            And(elems)
          }
        }
      }

    fun disjunct(vararg conditions: MilestoneAcceptConditionKt): MilestoneAcceptConditionKt =
      if (conditions.contains(Always)) {
        Always
      } else {
        val filtered = conditions.filter { it != Never }
        if (filtered.isEmpty()) {
          Never
        } else {
          if (filtered.size == 1) {
            filtered.first()
          } else {
            val elems = filtered.flatMap {
              when (it) {
                is Or -> it.conditions
                else -> listOf(it)
              }
            }
            Or(elems)
          }
        }
      }
  }

  abstract fun negation(): MilestoneAcceptConditionKt

  object Always : MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt = Never
  }

  object Never : MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt = Always
  }

  data class And(val conditions: List<MilestoneAcceptConditionKt>) : MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      disjunct(*(conditions.map { it.negation() }.toTypedArray()))
  }

  data class Or(val conditions: List<MilestoneAcceptConditionKt>) : MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      conjunct(*(conditions.map { it.negation() }.toTypedArray()))
  }

  data class Exists(val milestone: MilestoneKt, val checkFromNextGen: Boolean) :
    MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      NotExists(milestone, checkFromNextGen)
  }

  data class NotExists(val milestone: MilestoneKt, val checkFromNextGen: Boolean) :
    MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      Exists(milestone, checkFromNextGen)
  }

  data class OnlyIf(val milestone: MilestoneKt) : MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      Unless(milestone)
  }

  data class Unless(val milestone: MilestoneKt) : MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      OnlyIf(milestone)
  }
}
