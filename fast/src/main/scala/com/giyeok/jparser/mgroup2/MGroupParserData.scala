package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.nparser2.Kernel

case class MGroupParserData(
  grammar: NGrammar,
  startGroupId: Int,
  termActions: Map[Int, List[(TermGroupDesc, ParsingAction)]],
  edgeActions: Map[(Int, Int), ParsingAction],
  mgroups: Map[Int, MilestoneGroup],
)

case class MilestoneGroup(milestones: Set[Kernel])

case class ParsingAction(
  hingeReplacingId: Int,
  appendingId: Int,
)
