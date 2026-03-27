package com.giyeok.jparser.ktparser.mgroup2

import com.google.gson.stream.JsonWriter
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto
import java.nio.file.Path
import java.io.StringWriter
import java.io.Writer
import kotlin.io.path.bufferedWriter

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

  fun toJsonString(): String {
    val writer = StringWriter()
    writeTo(writer)
    return writer.toString()
  }

  fun writeTo(writer: Writer) {
    JsonWriter(writer).use { json ->
      json.setIndent("  ")
      json.beginObject()
      json.name("parseSteps")
      json.beginArray()
      parseSteps.forEach { step -> json.value(step) }
      json.endArray()
      json.endObject()
    }
  }

  fun writeTo(path: Path) {
    path.bufferedWriter().use { writer ->
      writeTo(writer)
    }
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

  private fun JsonWriter.value(step: ParseStepLog) {
    beginObject()
    name("gen").value(step.gen.toLong())
    name("input").value(step.input)
    name("previousPathCount").value(step.previousPathCount.toLong())
    name("collectedPaths")
    beginArray()
    step.collectedPaths.forEach { valuePrettyOnly(it) }
    endArray()
    name("collectedGroupSummaries")
    beginArray()
    step.collectedGroupSummaries.forEach { value(it) }
    endArray()
    name("updatedPaths")
    beginArray()
    step.updatedPaths.forEach { value(it) }
    endArray()
    name("trackings")
    beginArray()
    step.trackings.forEach { value(it) }
    endArray()
    name("filteredPaths")
    beginArray()
    step.filteredPaths.forEach { value(it) }
    endArray()
    endObject()
  }

  private fun JsonWriter.value(milestone: MilestoneLog) {
    beginObject()
    name("symbolId").value(milestone.symbolId.toLong())
    name("pointer").value(milestone.pointer.toLong())
    name("gen").value(milestone.gen.toLong())
    endObject()
  }

  private fun JsonWriter.value(milestoneGroup: MilestoneGroupLog) {
    beginObject()
    name("groupId").value(milestoneGroup.groupId.toLong())
    name("gen").value(milestoneGroup.gen.toLong())
    endObject()
  }

  private fun JsonWriter.value(condition: AcceptConditionLog) {
    beginObject()
    name("type").value(condition.type)
    condition.symbolId?.let { name("symbolId").value(it.toLong()) }
    condition.gen?.let { name("gen").value(it.toLong()) }
    condition.checkFromNextGen?.let { name("checkFromNextGen").value(it) }
    condition.conditions?.let { conditions ->
      name("conditions")
      beginArray()
      conditions.forEach { value(it) }
      endArray()
    }
    endObject()
  }

  private fun JsonWriter.value(path: PathLog) {
    beginObject()
    name("first")
    value(path.first)
    name("path")
    beginArray()
    path.path.forEach { value(it) }
    endArray()
    name("tip")
    value(path.tip)
    name("acceptCondition")
    value(path.acceptCondition)
    name("pretty").value(path.pretty)
    endObject()
  }

  private fun JsonWriter.valuePrettyOnly(path: PathLog) {
    beginObject()
    // name("first")
    // value(path.first)
    // name("path")
    // beginArray()
    // path.path.forEach { value(it) }
    // endArray()
    // name("tip")
    // value(path.tip)
    // name("acceptCondition")
    // value(path.acceptCondition)
    name("pretty").value(path.pretty)
    endObject()
  }

  private fun JsonWriter.value(groupSummary: GroupSummaryLog) {
    beginObject()
    name("groupId").value(groupSummary.groupId.toLong())
    name("milestoneCount").value(groupSummary.milestoneCount.toLong())
    name("milestones")
    beginArray()
    groupSummary.milestones.forEach { milestone ->
      beginObject()
      name("symbolId").value(milestone.symbolId.toLong())
      name("pointer").value(milestone.pointer.toLong())
      endObject()
    }
    endArray()
    endObject()
  }
}
