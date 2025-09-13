package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.TermSet
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import com.giyeok.jparser.mgroup3.proto.TermAction
import com.giyeok.jparser.proto.TermGroupProto

data class ParsingData(
  val data: Mgroup3ParserData,
  val termActions: Map<Int, List<Pair<TermSet, TermAction>>>,
) {

  companion object {
    fun termSetFrom(termGroup: TermGroupProto.TermGroup): TermSet {
      TODO()
    }

    fun fromProto(proto: Mgroup3ParserData): ParsingData {
      val termActions = proto.termActionsMap.mapValues { (_, termActions) ->
        termActions.actionsList
      }
      TODO()
    }
  }
}
