package com.giyeok.moonparser

trait Reverters {
    this: Parser =>

    sealed trait Reverter {
        def toShortString: String = toString
    }

    // 실제 하는 액션의 종류 - DeriveReverter, NodeKillReverter, TempLiftBlock, Reserved
    sealed trait DeriveReverter extends Reverter {
        val targetEdge: SimpleEdge
    }
    sealed trait NodeKillReverter extends Reverter {
        val targetNode: Node
    }
    sealed trait TempLiftBlockReverter extends Reverter {
        val targetNode: Node
    }
    sealed trait ReservedReverter extends Reverter {
        val node: Node
    }

    // Reverter가 activate되는 트리거의 종류 - Lift, Multi
    sealed trait ReverterTrigger extends Reverter
    sealed trait LiftTriggered extends ReverterTrigger {
        val trigger: Node
    }
    sealed trait MultiTriggered extends ReverterTrigger {
        // 이 트리거가 "모두" 만족되어야 말동
        val triggers: Set[TriggerCondition]
    }
    sealed trait TriggerCondition { val trigger: Node }
    case class TriggerIfLift(trigger: Node) extends TriggerCondition
    case class TriggerIfAlive(trigger: Node) extends TriggerCondition

    // lookahead_except, backup에서 사용
    case class LiftTriggeredDeriveReverter(trigger: Node, targetEdge: SimpleEdge) extends LiftTriggered with DeriveReverter
    // except에서 사용
    case class LiftTriggeredTempLiftBlockReverter(trigger: Node, targetNode: Node) extends LiftTriggered with TempLiftBlockReverter
    // longest match가 lift될 때 생성
    case class ReservedLiftTriggeredLiftedNodeReverter(trigger: Node) extends LiftTriggered with ReservedReverter { val node = trigger }
    // eager longest match가 lift될 때 생성
    case class ReservedAliveTriggeredLiftedNodeReverter(trigger: Node) extends LiftTriggered with ReservedReverter { val node = trigger }
    // ReservedNodeReverter는 조건이 맞으면
    //  - MultiTriggeredNodeKillReverter를 만들 수도 있고
    //  - DeriveReverter를 만들 수도 있을듯

    // derive reverter에서 파생됨
    case class MultiTriggeredNodeKillReverter(triggers: Set[TriggerCondition], targetNode: Node) extends MultiTriggered with NodeKillReverter
}
