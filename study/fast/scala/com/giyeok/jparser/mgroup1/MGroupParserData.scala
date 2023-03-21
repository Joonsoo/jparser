package com.giyeok.jparser.mgroup1

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.KernelTemplate

case class MGroupParserData(
  grammar: NGrammar,
  startGroupId: Int,
  termActions: Map[Int, List[(TermGroupDesc, ParsingAction)]],
  edgeActions: Map[(Int, Int), ParsingAction],
  mgroups: Map[Int, MilestoneGroup],
)

// shadowMilestones는 accept condition을 위해 추가되는 마일스톤들.
// 기본적으로는 동일하게 처리되지만 처리 가능한 terminal 종류가 바뀜
case class MilestoneGroup(milestones: Set[KernelTemplate], shadowMilestones: Set[KernelTemplate])

case class ParsingAction(
  prevReplacingId: Int,
  // prev가 replace된 다음 적용할 progress action들(각 슬롯에 대한 accept condition들)
  appendingId: Int,
)
