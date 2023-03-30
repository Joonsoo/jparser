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

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.LONGEST -> {
          val milestoneGen = if (template.longest.fromNextGen) gen else beginGen
          NotExists(MilestoneKt(template.longest.symbolId, 0, milestoneGen), true)
        }

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.ONLY_IF -> {
          val milestoneGen = if (template.onlyIf.fromNextGen) gen else beginGen
          OnlyIf(MilestoneKt(template.onlyIf.symbolId, 0, milestoneGen))
        }

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.UNLESS -> {
          val milestoneGen = if (template.unless.fromNextGen) gen else beginGen
          Unless(MilestoneKt(template.unless.symbolId, 0, milestoneGen))
        }

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

  fun prettyString(): String = when (this) {
    Always -> "Always"
    Never -> "Never"
    is And ->
      "And(${this.conditions.map { it.prettyString() }.sorted().joinToString(", ")})"

    is Or ->
      "Or(${this.conditions.map { it.prettyString() }.sorted().joinToString(", ")})"

    is Exists ->
      "Exists(${this.milestone.prettyString()}, ${this.checkFromNextGen})"

    is NotExists ->
      "NotExists(${this.milestone.prettyString()}, ${this.checkFromNextGen})"

    is OnlyIf ->
      "OnlyIf(${this.milestone.prettyString()})"

    is Unless ->
      "Unless(${this.milestone.prettyString()})"
  }


  abstract fun negation(): MilestoneAcceptConditionKt

  fun milestones(): Set<MilestoneKt> = when (this) {
    Always -> setOf()
    Never -> setOf()
    is And -> this.conditions.flatMap { it.milestones() }.toSet()
    is Or -> this.conditions.flatMap { it.milestones() }.toSet()
    is Exists -> setOf(this.milestone)
    is NotExists -> setOf(this.milestone)
    is OnlyIf -> setOf(this.milestone)
    is Unless -> setOf(this.milestone)
  }

  object Always: MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt = Never
  }

  object Never: MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt = Always
  }

  data class And(val conditions: List<MilestoneAcceptConditionKt>): MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      disjunct(*(conditions.map { it.negation() }.toTypedArray()))
  }

  data class Or(val conditions: List<MilestoneAcceptConditionKt>): MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      conjunct(*(conditions.map { it.negation() }.toTypedArray()))
  }

  data class Exists(val milestone: MilestoneKt, val checkFromNextGen: Boolean):
    MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      NotExists(milestone, checkFromNextGen)
  }

  data class NotExists(val milestone: MilestoneKt, val checkFromNextGen: Boolean):
    MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      Exists(milestone, checkFromNextGen)
  }

  data class OnlyIf(val milestone: MilestoneKt): MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      Unless(milestone)
  }

  data class Unless(val milestone: MilestoneKt): MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      OnlyIf(milestone)
  }
}
