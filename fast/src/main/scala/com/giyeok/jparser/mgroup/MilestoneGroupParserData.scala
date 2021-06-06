package com.giyeok.jparser.mgroup

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.{GraphNoIndex, TasksSummary}
import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition

case class MilestoneGroupParserData(grammar: NGrammar,
                                    // mgroup 파서에서 startMgroup은 {(Start, 0, 0..0)}의 mgroup이 아니라
                                    // (Start, 0, 0..0)에서 도달 가능한 longest, join, except, lookahead 심볼이 참조하는
                                    // 심볼들의 노드도 포함해야 함
                                    startMilestoneGroup: Int,
                                    emptyMilestoneGroup: Int,
                                    byStart: TasksSummary,
                                    // mgroup ID -> mgroup definition
                                    milestoneGroups: Map[Int, MilestoneGroup],
                                    // mgroup ID -> term group desc -> term action
                                    termActions: Map[Int, List[(TermGroupDesc, ParsingAction)]],
                                    // (edge start mgroup ID -> edge end mgroup ID) -> edge action
                                    edgeActions: Map[(Int, Int), ParsingAction],
                                    // mgroup ID -> milestone drop action
                                    milestoneDropActions: Map[Int, MilestoneDropActions],
                                    derivedGraph: Map[Int, GraphNoIndex])

case class Milestone(symbolId: Int, pointer: Int, acceptConditionSlot: Int) extends Ordered[Milestone] {
  override def compare(that: Milestone): Int = {
    if (this.symbolId != that.symbolId) this.symbolId - that.symbolId
    else if (this.pointer != that.pointer) this.pointer - that.pointer
    else this.acceptConditionSlot - that.acceptConditionSlot
  }
}

case class ParsingAction(appendingAction: Option[AppendingAction],
                         progress: Option[StepProgress])

case class AppendingAction(replacement: Option[StepReplacement],
                           appendingMGroup: Int,
                           acceptConditions: List[AcceptCondition])

// MilestoneGroup은 milestone들의 set과 accept condition slot들로 정의된다
// 각 milestone은 특정 accept condition slot에 지정된 accept condition을 따라간다.
case class MilestoneGroup(/* 이 MGruop에 속하는 milestone 집합. Milestone 클래스에 정의된 순서대로 들어가야 함 */
                          milestones: List[Milestone],
                          // 이 MGroup에 accept condition 슬롯이 몇개가 있는지.
                          // milestones의 acceptConditionSlot이 이 범위를 넘을 수 없다.
                          acceptConditionSlots: Int)

// TermAction은 group M에 terminal X가 들어오면 group M에서 일부 milestone이 탈락한 group M'으로 치환되고 M이나 M'과 accept
// condition이 관련이 없는 새로운 group N이 뒤에 붙는다.
case class TermAction(appendAction: Option[TermActionAppendingAction],
                      // tipProgress가 Some이면 tip에 StepReplacement을 적용한 다음 (tip.parent -> tip) 엣지에 대해 edge action 적용
                      tipProgress: Option[StepProgress])

// TermActionAppendingAction은 EdgeAction과 달리 accept condition을 승계받지 않기 때문에 succedingSlot은 없음
case class TermActionAppendingAction(tipReplacement: Option[StepReplacement],
                                     appendingMGroup: Int,
                                     // `acceptConditions`의 길이와 `appendingMGroup`의 `acceptConditionSlots`는 일치해야 함
                                     // TermAction의 appending action은 accept condition을 승계해올 필요가 없기 때문에 EdgeActionAppendingAction.acceptConditions와 다름
                                     acceptConditions: List[AcceptCondition])

case class StepReplacement(mgroup: Int,
                           // replaceTipTo를 적용할 때 기존의 accept condition slot 중 어떤 것을 가져올지
                           succeedingAcceptConditionSlots: List[Int])

// tip progress의 acceptConditions는 slot succession이 들어갈 수가 없을듯?
case class StepProgress(tipReplacement: Int,
                        acceptConditions: List[AcceptCondition])

// (group M) -> (group N) 엣지에 EdgeAction이 적용되면
// (gruop M)에서 일부 millestone이 탈락한 (group M')과 (group N)에서 일부 milestone이 탈락한 (group M') -> (group N')으로 치환되고
// (group N')의 milestone들이 progress되는 것.
// 실제 파싱 과정에서는 (group N')이 쓰일 일은 없으므로 (group M')으로 치환하고 다음의 두가지 과정이 실행됨.
// 1. N'이 progress되면서 N'의 accept condition을 승계하는 (group N'')이 (group M') 뒤에 붙거나 (appending)
// 2. (group M')이 progress되면서 EdgeAction이 연쇄적으로 추가 발생하거나
//  - M'의 accept condition slot 각각에 (N'의 accept condition 중 어떤 것을 승계+새로 추가될 accept condition) 지정
// - 1과 2는 동시에 일어날 수 있음.
// - 1이 여러개 생길 필요는 전혀 없을까?
// 주의) M -> N의 엣지가 있다고 해서 M의 milestone이 모두 N의 milestone으로 도달 가능한 것은 아님
case class EdgeAction(
                       // appending에서 succeedingSlot은 N에서의 slot index. M이 아님을 주의
                       appending: Option[EdgeActionAppendingAction],

                       /**
                        * (parent -> child)에서 child가 progress되는 액션.
                        * child는 이번 progress로 인해 finish되는 milestone들만 남기고,
                        * parent의 acceptCondition slot들을 어떻게 만들어야 하는지의 정보를 포함하고 있음.
                        * `acceptConditions.succeedingSlot`는 child의 slot 중 어떤 것들을 승계해야 하는지 가리킴
                        *
                        * ParentProgress의 parentReplacement는 parent의 milestone 중에서,
                        * child가 progress됨으로 해서 finish되는 milestone들만 포함.
                        */
                       parentProgress: Option[StepProgress])

case class EdgeActionAppendingAction(
                                      // AppendingAction의 parentReplacement는 parent의 milestone 중에서,
                                      // child의 milestone 중 최소 한개 이상으로 reachable한 것들 중에서도,
                                      // appending하는 것들로 reachable한, 즉 앞으로도 필요한 milestone만 포함.
                                      parentReplacement: Option[StepReplacement],
                                      appendingMGroup: Int,
                                      // `acceptConditions`가 가리키는 succeedingSlot은 N'에서의 slot index
                                      acceptConditions: List[AcceptConditionSuccession])

// slot number가 0이면 Always로 간주?
// succedingSlot과 newCondition은 OR(disjunct)로 연결
case class AcceptConditionSuccession(successionExpr: AcceptCondition)
// succeedingSlot: AcceptCondition, newCondition: AcceptCondition

// MGroup 중에서 accept condition slot의 evaluation 결과 Never가 된 경우 어떻게 해야하는지 처리
// -> Never가 된 accept condition slot을 가리키는 모든 마일스톤을 제거한 다른 mgroup으로 치환
// dropping의 key는 accept condition slot index의 set
case class MilestoneDropActions(dropping: Map[Set[Int], StepReplacement])
