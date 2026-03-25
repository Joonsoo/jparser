package com.giyeok.jparser.ktparser.mgroup2

import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto
import java.nio.file.Path
import kotlin.io.path.writeText

class DebuggingLogBuilder {
  data class MilestoneLog(val symbolId: Int, val pointer: Int, val gen: Int)

  data class MilestoneGroupLog(val groupId: Int, val gen: Int)

  data class GroupMilestoneLog(val symbolId: Int, val pointer: Int)

  data class AcceptConditionLog(
    val type: String,
    val symbolId: Int? = null,
    val gen: Int? = null,
    val checkFromNextGen: Boolean? = null,
    val conditions: List<AcceptConditionLog>? = null,
  )

  data class PathLog(
    val first: MilestoneLog,
    val path: List<MilestoneLog>,
    val tip: MilestoneGroupLog,
    val acceptCondition: AcceptConditionLog,
    val pretty: String,
  )

  data class GroupSummaryLog(
    val groupId: Int,
    val milestoneCount: Int,
    val milestones: List<GroupMilestoneLog>,
  )

  data class ParseStepLog(
    val gen: Int,
    val input: String,
    val previousPathCount: Int,
    var collectedPaths: List<PathLog> = listOf(),
    var collectedGroupSummaries: List<GroupSummaryLog> = listOf(),
    var updatedPaths: List<PathLog> = listOf(),
    var trackings: List<MilestoneLog> = listOf(),
    var filteredPaths: List<PathLog> = listOf(),
  )

  private val parseSteps = mutableListOf<ParseStepLog>()

  fun beginParseStep(gen: Int, input: Char, previousPathCount: Int) {
    parseSteps.add(
      ParseStepLog(
        gen = gen,
        input = input.toString(),
        previousPathCount = previousPathCount,
      )
    )
  }

  fun recordCollectedPaths(paths: List<MilestoneGroupPathKt>, groupSummaries: List<GroupSummaryLog>) {
    parseSteps.lastOrNull()?.apply {
      collectedPaths = paths.map { it.toLog() }
      collectedGroupSummaries = groupSummaries
    }
  }

  fun recordConditionUpdated(paths: List<MilestoneGroupPathKt>) {
    parseSteps.lastOrNull()?.updatedPaths = paths.map { it.toLog() }
  }

  fun recordFiltered(trackings: Set<MilestoneKt>, paths: List<MilestoneGroupPathKt>) {
    parseSteps.lastOrNull()?.apply {
      this.trackings = trackings.sortedWith(compareBy({ it.gen }, { it.symbolId }, { it.pointer }))
        .map { it.toLog() }
      filteredPaths = paths.map { it.toLog() }
    }
  }

  fun snapshot(): List<ParseStepLog> = parseSteps.toList()

  fun toJsonString(): String = encodeValue(
    mapOf(
      "parseSteps" to parseSteps.map { step ->
        mapOf(
          "gen" to step.gen,
          "input" to step.input,
          "previousPathCount" to step.previousPathCount,
          "collectedPaths" to step.collectedPaths.map { it.toJsonMap() },
          "collectedGroupSummaries" to step.collectedGroupSummaries.map { it.toJsonMap() },
          "updatedPaths" to step.updatedPaths.map { it.toJsonMap() },
          "trackings" to step.trackings.map { it.toJsonMap() },
          "filteredPaths" to step.filteredPaths.map { it.toJsonMap() },
        )
      }
    )
  )

  fun writeTo(path: Path) {
    path.writeText(toJsonString())
  }

  private fun MilestoneKt.toLog(): MilestoneLog = MilestoneLog(symbolId, pointer, gen)

  private fun MilestoneGroupKt.toLog(): MilestoneGroupLog = MilestoneGroupLog(groupId, gen)

  companion object {
    fun groupSummary(
      groupId: Int,
      milestones: Collection<MilestoneParserDataProto.KernelTemplate>
    ): GroupSummaryLog = GroupSummaryLog(
      groupId = groupId,
      milestoneCount = milestones.size,
      milestones = milestones.map { GroupMilestoneLog(it.symbolId, it.pointer) },
    )
  }

  private fun MilestoneAcceptConditionKt.toLog(): AcceptConditionLog = when (this) {
    MilestoneAcceptConditionKt.Always -> AcceptConditionLog(type = "Always")
    MilestoneAcceptConditionKt.Never -> AcceptConditionLog(type = "Never")
    is MilestoneAcceptConditionKt.And ->
      AcceptConditionLog(
        type = "And",
        conditions = conditions.map { it.toLog() }
      )

    is MilestoneAcceptConditionKt.Or ->
      AcceptConditionLog(
        type = "Or",
        conditions = conditions.map { it.toLog() }
      )

    is MilestoneAcceptConditionKt.Exists ->
      AcceptConditionLog("Exists", symbolId, gen, checkFromNextGen)

    is MilestoneAcceptConditionKt.NotExists ->
      AcceptConditionLog("NotExists", symbolId, gen, checkFromNextGen)

    is MilestoneAcceptConditionKt.OnlyIf ->
      AcceptConditionLog("OnlyIf", symbolId, gen)

    is MilestoneAcceptConditionKt.Unless ->
      AcceptConditionLog("Unless", symbolId, gen)
  }

  private fun MilestoneGroupPathKt.toLog(): PathLog {
    val milestones = mutableListOf<MilestoneLog>()

    fun traverse(pathList: PathList) {
      when (pathList) {
        is PathList.Cons -> {
          milestones.add(pathList.milestone.toLog())
          traverse(pathList.parent)
        }

        PathList.Nil -> {
          // do nothing
        }
      }
    }

    traverse(path)

    return PathLog(
      first = first.toLog(),
      path = milestones.reversed(),
      tip = tip.toLog(),
      acceptCondition = acceptCondition.toLog(),
      pretty = prettyString(),
    )
  }

  private fun MilestoneLog.toJsonMap(): Map<String, Any> = mapOf(
    "symbolId" to symbolId,
    "pointer" to pointer,
    "gen" to gen,
  )

  private fun MilestoneGroupLog.toJsonMap(): Map<String, Any> = mapOf(
    "groupId" to groupId,
    "gen" to gen,
  )

  private fun AcceptConditionLog.toJsonMap(): Map<String, Any?> = buildMap {
    put("type", type)
    symbolId?.let { put("symbolId", it) }
    gen?.let { put("gen", it) }
    checkFromNextGen?.let { put("checkFromNextGen", it) }
    conditions?.let { put("conditions", it.map { cond -> cond.toJsonMap() }) }
  }

  private fun PathLog.toJsonMap(): Map<String, Any> = mapOf(
    "first" to first.toJsonMap(),
    "path" to path.map { it.toJsonMap() },
    "tip" to tip.toJsonMap(),
    "acceptCondition" to acceptCondition.toJsonMap(),
    "pretty" to pretty,
  )

  private fun GroupSummaryLog.toJsonMap(): Map<String, Any> = mapOf(
    "groupId" to groupId,
    "milestoneCount" to milestoneCount,
    "milestones" to milestones.map { milestone ->
      mapOf(
        "symbolId" to milestone.symbolId,
        "pointer" to milestone.pointer,
      )
    },
  )

  private fun encodeValue(value: Any?, indent: String = ""): String = when (value) {
    null -> "null"
    is String -> "\"${escape(value)}\""
    is Number, is Boolean -> value.toString()
    is Map<*, *> -> encodeMap(value, indent)
    is Iterable<*> -> encodeList(value.toList(), indent)
    else -> "\"${escape(value.toString())}\""
  }

  private fun encodeMap(map: Map<*, *>, indent: String): String {
    if (map.isEmpty()) return "{}"
    val nextIndent = "$indent  "
    return map.entries.joinToString(
      prefix = "{\n",
      postfix = "\n$indent}",
      separator = ",\n"
    ) { (key, value) ->
      "$nextIndent\"${escape(key.toString())}\": ${encodeValue(value, nextIndent)}"
    }
  }

  private fun encodeList(list: List<*>, indent: String): String {
    if (list.isEmpty()) return "[]"
    val nextIndent = "$indent  "
    return list.joinToString(
      prefix = "[\n",
      postfix = "\n$indent]",
      separator = ",\n"
    ) { value ->
      "$nextIndent${encodeValue(value, nextIndent)}"
    }
  }

  private fun escape(value: String): String = buildString {
    value.forEach { ch ->
      when (ch) {
        '\\' -> append("\\\\")
        '"' -> append("\\\"")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        else -> append(ch)
      }
    }
  }
}
