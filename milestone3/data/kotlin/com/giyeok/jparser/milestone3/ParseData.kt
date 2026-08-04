package com.giyeok.jparser.milestone3

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.milestone3.proto.Milestone3ParserDataProto
import com.giyeok.jparser.proto.GrammarProto

data class ParseData(
  val grammar: GrammarProto.NGrammar,
  val initialWatcherRootSymbols: List<Int>,
  val termActions: Map<KernelTemplate, List<Pair<Inputs.TermGroupDesc, TermAction>>>,
  val edgeActions: Map<Pair<KernelTemplate, KernelTemplate>, ParseAction>,
) {
  companion object {
    fun fromProto(proto: Milestone3ParserDataProto.Milestone3ParserData): ParseData {
      TODO()
    }
  }

  val startSymbol = grammar.startSymbol
}

data class KernelTemplate(
  val symbolId: Int,
  val pointer: Int,
)

data class TermAction(
  val parsingAction: ParseAction,
  val watchers: List<Milestone3ParserDataProto.WatcherTemplate>,
)

data class ParseAction(
  val appends: List<AppendMilestone>,
  val tipProgress: M3AcceptConditionTemplate?,
  // watcherRootSymbols 는 원래 appends에 있는 것들의 합집합
  val watcherRootSymbols: List<Int>,
)

data class AppendMilestone(
  val appendKernelTemplate: KernelTemplate,
  val appendAcceptCondition: M3AcceptConditionTemplate,
  val edgeAcceptCondition: M3AcceptConditionTemplate,
  // val watcherRootSymbols: List<Int>,
)

sealed class M3AcceptConditionTemplate

data object AlwaysTemplate: M3AcceptConditionTemplate()

