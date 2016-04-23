package com.giyeok.moonparser

trait Reverters {
    this: Parser =>

    sealed trait Reverter {
        def toShortString: String = toString
    }

    sealed trait DeriveReverter extends Reverter {
        val targetEdge: SimpleEdge
    }
    sealed trait NodeKillReverter extends Reverter {
        val targetNode: Node
    }
    sealed trait TemporaryLiftBlockReverter extends Reverter {
        val targetNode: Node
    }

    sealed trait ReverterTrigger
    sealed trait MultiLiftTriggered extends ReverterTrigger {
        // 이 트리거가 "모두" 만족되어야 말동
        val triggers: Set[MultiLiftTriggered.Condition]
    }
    object MultiLiftTriggered {
        sealed trait Condition { val trigger: Node }
        case class Lift(trigger: Node) extends Condition
        case class Alive(trigger: Node) extends Condition
    }
    sealed trait LiftTriggered extends ReverterTrigger {
        val trigger: Node
    }
    sealed trait AliveTriggered extends ReverterTrigger {
        val trigger: Node
    }

    sealed trait ReservedReverter extends Reverter

    // lookahead_except에서 사용
    case class LiftTriggeredDeriveReverter(trigger: Node, targetEdge: SimpleEdge) extends LiftTriggered with DeriveReverter
    case class MultiTriggeredNodeKillReverter(triggers: Set[MultiLiftTriggered.Condition], targetNode: Node) extends MultiLiftTriggered with NodeKillReverter
    // except에서 사용
    case class LiftTriggeredTempLiftBlockReverter(trigger: Node, targetNode: Node) extends LiftTriggered with TemporaryLiftBlockReverter
    // longest match가 lift될 때 생성
    case class ReservedLiftTriggeredNodeKillReverter(trigger: Node) extends LiftTriggered with ReservedReverter
    case class LiftTriggeredNodeKillReverter(trigger: Node, targetNode: Node) extends LiftTriggered with NodeKillReverter
    // eager longest match가 lift될 때 생성
    case class ReservedAliveTriggeredNodeKillReverter(trigger: Node) extends LiftTriggered with ReservedReverter
    case class AliveTriggeredNodeKillReverter(trigger: Node, targetNode: Node) extends AliveTriggered with NodeKillReverter
}
