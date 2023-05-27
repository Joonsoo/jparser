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
          Exists(template.lookaheadIs.symbolId, gen, template.lookaheadIs.fromNextGen)

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.LOOKAHEAD_NOT ->
          NotExists(template.lookaheadNot.symbolId, gen, template.lookaheadNot.fromNextGen)

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.LONGEST -> {
          val milestoneGen = if (template.longest.fromNextGen) gen else beginGen
          NotExists(template.longest.symbolId, milestoneGen, true)
        }

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.ONLY_IF -> {
          val milestoneGen = if (template.onlyIf.fromNextGen) gen else beginGen
          OnlyIf(template.onlyIf.symbolId, milestoneGen)
        }

        MilestoneParserDataProto.AcceptConditionTemplate.ConditionCase.UNLESS -> {
          val milestoneGen = if (template.unless.fromNextGen) gen else beginGen
          Unless(template.unless.symbolId, milestoneGen)
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

    fun disjunct(
      cond1: MilestoneAcceptConditionKt,
      cond2: MilestoneAcceptConditionKt
    ): MilestoneAcceptConditionKt = when {
      cond1 == Always || cond2 == Always -> Always
      cond1 == Never && cond2 == Never -> Never
      cond1 == cond2 -> cond1
      cond1 is Or && cond2 is Or -> Or(cond1.conditions + cond2.conditions)
      cond1 is Or -> Or(cond1.conditions + cond2)
      cond2 is Or -> Or(cond2.conditions + cond1)
      else -> Or(listOf(cond1, cond2))
    }

    fun disjunctMulti(vararg conditions: MilestoneAcceptConditionKt): MilestoneAcceptConditionKt =
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
      "Exists(${this.symbolId}, ${this.gen}, ${this.checkFromNextGen})"

    is NotExists ->
      "NotExists(${this.symbolId}, ${this.gen}, ${this.checkFromNextGen})"

    is OnlyIf ->
      "OnlyIf(${this.symbolId}, ${this.gen})"

    is Unless ->
      "Unless(${this.symbolId}, ${this.gen})"
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

  object Always : MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt = Never
  }

  object Never : MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt = Always
  }

  data class And(val conditions: List<MilestoneAcceptConditionKt>) : MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      disjunctMulti(*(conditions.map { it.negation() }.toTypedArray()))
  }

  data class Or(val conditions: List<MilestoneAcceptConditionKt>) : MilestoneAcceptConditionKt() {
    override fun negation(): MilestoneAcceptConditionKt =
      conjunct(*(conditions.map { it.negation() }.toTypedArray()))
  }

  data class Exists(val symbolId: Int, val gen: Int, val checkFromNextGen: Boolean) :
    MilestoneAcceptConditionKt() {
    val milestone = MilestoneKt(symbolId, 0, gen)

    override fun negation(): MilestoneAcceptConditionKt = NotExists(symbolId, gen, checkFromNextGen)
  }

  data class NotExists(val symbolId: Int, val gen: Int, val checkFromNextGen: Boolean) :
    MilestoneAcceptConditionKt() {
    val milestone = MilestoneKt(symbolId, 0, gen)

    override fun negation(): MilestoneAcceptConditionKt = Exists(symbolId, gen, checkFromNextGen)
  }

  data class OnlyIf(val symbolId: Int, val gen: Int) : MilestoneAcceptConditionKt() {
    val milestone = MilestoneKt(symbolId, 0, gen)

    override fun negation(): MilestoneAcceptConditionKt = Unless(symbolId, gen)
  }

  data class Unless(val symbolId: Int, val gen: Int) : MilestoneAcceptConditionKt() {
    val milestone = MilestoneKt(symbolId, 0, gen)

    override fun negation(): MilestoneAcceptConditionKt = OnlyIf(symbolId, gen)
  }
}
