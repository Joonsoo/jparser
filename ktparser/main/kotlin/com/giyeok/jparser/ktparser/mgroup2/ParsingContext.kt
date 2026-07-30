package com.giyeok.jparser.ktparser.mgroup2

import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto

data class ParsingContextKt(
  val gen: Int,
  val paths: List<MilestoneGroupPathKt>,
  val history: HistoryEntryList,
)

data class MilestoneGroupPathKt(
  val first: MilestoneKt,
  val path: PathList,
  val tip: MilestoneGroupKt,
  val acceptCondition: MilestoneAcceptConditionKt,
) {
  fun replaceAndAppend(
    replace: MilestoneParserDataProto.KernelTemplate,
    append: MilestoneGroupKt,
    condition: MilestoneAcceptConditionKt,
  ): MilestoneGroupPathKt =
    MilestoneGroupPathKt(
      first,
      PathList.Cons(MilestoneKt(replace.symbolId, replace.pointer, tip.gen), path),
      append,
      condition
    )

  fun prettyString(): String {
    val strings = mutableListOf<String>()
    fun traverse(pathList: PathList) {
      when (pathList) {
        is PathList.Cons -> {
          strings.add(pathList.milestone.prettyString())
          traverse(pathList.parent)
        }

        PathList.Nil -> {
          // do nothing
        }
      }
    }
    traverse(path)

    val pathString = strings.reversed().joinToString(" -> ")
    return "$pathString -> [${tip.groupId}] ${tip.gen} (${acceptCondition.prettyString()})"
  }
}

sealed class PathList {
  object Nil : PathList()

  data class Cons(val milestone: MilestoneKt, val parent: PathList) : PathList()
}

sealed class HistoryEntryList {
  data class Nil(val initialEntry: HistoryEntryKt) : HistoryEntryList()

  data class Cons(val historyEntry: HistoryEntryKt, val parent: HistoryEntryList) :
    HistoryEntryList()

  fun toList(): List<HistoryEntryKt> {
    val builder = mutableListOf<HistoryEntryKt>()
    var curr = this
    while (curr is Cons) {
      builder.add(curr.historyEntry)
      curr = curr.parent
    }
    builder.add((curr as Nil).initialEntry)
    return builder.reversed()
  }
}

data class MilestoneKt(val symbolId: Int, val pointer: Int, val gen: Int) {
  fun prettyString(): String = "$symbolId $pointer $gen"
}

data class MilestoneGroupKt(val groupId: Int, val gen: Int)

data class HistoryEntryKt(
  val untrimmedPaths: List<MilestoneGroupPathKt>,
  val genActions: GenActionsKt
)
