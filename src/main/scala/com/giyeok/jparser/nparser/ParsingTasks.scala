package com.giyeok.jparser.nparser

import ParsingContext._
import EligCondition._
import NGrammar._

trait ParsingTasks {
    sealed trait Task
    case class DeriveTask(node: Node) extends Task
    case class FinishTask(node: Node, condition: Condition, lastSymbol: Option[Int]) extends Task
    case class ProgressTask(node: SequenceNode, condition: Condition) extends Task
}

trait DeriveTasks extends ParsingTasks {
    val grammar: NGrammar

    def deriveTask(nextGen: Int, task: DeriveTask, cc: Context): (Context, Seq[Task]) = {
        task.node match {
            case TermNode(_, _) => (cc, Seq()) // nothing to do
            case SymbolNode(symbolId, _) =>
                grammar.nsymbols(symbolId) match {
                    case Start(produces) => ???
                    case Nonterminal(_, produces) => ???
                    case OneOf(_, produces) => ???
                    case Proxy(_, produce) => ???
                    case Repeat(_, produces) => ???
                    case Join(_, body, join) => ???
                    case Except(_, body, except) => ???
                    case Longest(_, body) => ???
                    case EagerLongest(_, body) => ???
                    case LookaheadIs(_, lookahead) => ???
                    case LookaheadExcept(_, lookahead) => ???
                    case _ =>
                        // must not happen
                        assert(false)
                        ???
                }
            case SequenceNode(symbolId, pointer, _, _) =>
                grammar.nsymbols(symbolId) match {
                    case Sequence(_, sequence) =>
                        // sequence(pointer) 로 symbol node 만들기
                        ???
                    case _ =>
                        // must not happen
                        assert(false)
                        ???
                }
        }
    }
}

trait LiftTasks extends ParsingTasks {
    def finishTask(nextGen: Int, task: FinishTask, cc: Context): (Context, Seq[Task]) = ???
    def progressTask(nextGen: Int, task: FinishTask, cc: Context): (Context, Seq[Task]) = ???
}
