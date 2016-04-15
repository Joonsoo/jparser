package com.giyeok.moonparser

import com.giyeok.moonparser.ParseTree._
import com.giyeok.moonparser.Symbols._

trait GraphDataStructure {
    this: Parser =>

    type Node = SymbolProgress
    type TerminalNode = SymbolProgressTerminal
    type NonterminalNode = SymbolProgressNonterminal

    sealed abstract class DeriveEdge {
        val start: NonterminalNode
        val end: Node
        val nodes = Set(start, end)

        def endTo(end: Node): Boolean = (this.end == end)

        def toShortString: String
        override def toString = toShortString
    }
    case class SimpleEdge(start: NonterminalNode, end: Node) extends DeriveEdge {
        def toShortString = s"${start.toShortString} -> ${end.toShortString}"
    }
    case class JoinEdge(start: JoinProgress, end: Node, constraint: Node, endConstraintReversed: Boolean) extends DeriveEdge {
        override val nodes = Set(start, end, constraint)
        override def endTo(end: Node): Boolean = super.endTo(end) || end == this.constraint
        def toShortString = s"${start.toShortString} -> ${end.toShortString} & ${constraint.toShortString}${if (endConstraintReversed) " (reverse)" else ""}"
    }

    sealed trait Reverter {
        def toShortString: String = toString
    }
    // PreReverter는 derive나 lift시에 SymbolProgress가 리턴하는 Reverter
    sealed trait PreReverter extends Reverter
    // WorkingReverter는 PreReverter를 가공해서 실제 ParsingContext가 갖고 있는 Reverter.
    //  - 결국 종류는 LiftTriggeredDeriveReverter, MultiLiftTriggeredNodeKillReverter 두 개밖에 없다
    sealed trait WorkingReverter extends Reverter

    sealed trait DeriveReverter extends PreReverter with WorkingReverter {
        val targetEdge: SimpleEdge
    }
    sealed trait LiftReverter extends PreReverter {
        val targetLifting: NontermLifting
        def withNewTargetLifting(newTargetLifting: NontermLifting): LiftReverter
    }
    sealed trait NodeKillReverter extends WorkingReverter {
        val targetNode: Node
    }
    sealed trait TemporaryLiftBlockReverter extends PreReverter with WorkingReverter {
        val targetNode: Node
    }

    sealed trait Triggered extends Reverter
    sealed trait ConditionalTrigger extends Triggered
    sealed trait LiftTriggered extends ConditionalTrigger {
        val trigger: Node
    }

    sealed trait ReverterTrigger { val trigger: Node }
    case class LiftTrigger(trigger: Node) extends ReverterTrigger
    case class AliveTrigger(trigger: Node) extends ReverterTrigger
    sealed trait MultiLiftTriggered extends ConditionalTrigger {
        // 이 트리거가 "모두" 만족되어야 말동
        val triggers: Set[ReverterTrigger]
    }
    sealed trait AliveTriggered extends Triggered {
        val trigger: Node
    }

    case class LiftTriggeredDeriveReverter(trigger: Node, targetEdge: SimpleEdge) extends LiftTriggered with DeriveReverter
    case class LiftTriggeredLiftReverter(trigger: Node, targetLifting: NontermLifting) extends LiftTriggered with LiftReverter {
        def withNewTargetLifting(newTargetLifting: NontermLifting): LiftReverter = LiftTriggeredLiftReverter(trigger, newTargetLifting)
    }
    case class AliveTriggeredLiftReverter(trigger: Node, targetLifting: NontermLifting) extends AliveTriggered with LiftReverter {
        def withNewTargetLifting(newTargetLifting: NontermLifting): LiftReverter = AliveTriggeredLiftReverter(trigger, newTargetLifting)
    }
    case class MultiTriggeredNodeKillReverter(triggers: Set[ReverterTrigger], targetNode: Node) extends MultiLiftTriggered with NodeKillReverter

    case class LiftTriggeredTemporaryLiftBlockReverter(trigger: Node, targetNode: Node) extends LiftTriggered with TemporaryLiftBlockReverter

    implicit class AugEdges(edges: Set[DeriveEdge]) {
        def simpleEdges: Set[SimpleEdge] = edges collect { case e: SimpleEdge => e }

        def incomingEdgesOf(node: Node): Set[DeriveEdge] = edges filter { _.end == node }
        def incomingSimpleEdgesOf(node: Node): Set[SimpleEdge] = simpleEdges filter { _.end == node }
        def outgoingEdgesOf(node: Node): Set[DeriveEdge] = edges filter { _.start == node }
        def outgoingSimpleEdgesOf(node: Node): Set[SimpleEdge] = simpleEdges filter { _.start == node }

        def rootsOf(node: Node): Set[DeriveEdge] = {
            def trackRoots(queue: List[SymbolProgress], cc: Set[DeriveEdge]): Set[DeriveEdge] =
                queue match {
                    case node +: rest =>
                        val incomings = incomingEdgesOf(node) -- cc
                        trackRoots(rest ++ (incomings.toList map { _.start }), cc ++ incomings)
                    case List() => cc
                }
            trackRoots(List(node), Set())
        }
    }

}
