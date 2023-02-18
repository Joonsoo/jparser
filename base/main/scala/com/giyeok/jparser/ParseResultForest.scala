package com.giyeok.jparser

import com.giyeok.jparser.NGrammar.{NJoin, NSequence, NSymbol}

case class ParseForest(trees: Seq[ParseResultTree.Node]) extends ParseResult

object ParseForestFunc extends ParseResultFunc[ParseForest] {

  import ParseResultTree._

  def terminal(location: Int, input: Inputs.Input): ParseForest =
    ParseForest(Seq(TerminalNode(location, input)))

  def bind(left: Int, right: Int, symbol: NSymbol, body: ParseForest): ParseForest =
    ParseForest(body.trees map { b => BindNode(symbol, b) })

  def cyclicBind(left: Int, right: Int, symbol: NSymbol): ParseForest =
    ParseForest(Seq(CyclicBindNode(left, right, symbol)))

  def join(left: Int, right: Int, symbol: NJoin, body: ParseForest, constraint: ParseForest): ParseForest = {
    // body와 join의 tree 각각에 대한 조합을 추가한다
    ParseForest(body.trees flatMap { b => constraint.trees map { c => BindNode(symbol, JoinNode(symbol, b, c)) } })
  }

  def sequence(left: Int, right: Int, symbol: NSequence, pointer: Int): ParseForest =
    if (pointer == 0) {
      ParseForest(Seq(SequenceNode(left, right, symbol, List())))
    } else {
      ParseForest(Seq(CyclicSequenceNode(left, right, symbol, pointer, List())))
    }

  def append(sequence: ParseForest, child: ParseForest): ParseForest = {
    assert(sequence.trees forall { n => n.isInstanceOf[SequenceNode] || n.isInstanceOf[CyclicSequenceNode] })
    // sequence의 tree 각각에 child 각각을 추가한다
    ParseForest(sequence.trees flatMap { sequenceNode =>
      child.trees map { c =>
        sequenceNode match {
          case seq: SequenceNode => seq.append(c)
          case seq: CyclicSequenceNode => seq.append(c)
          case _ => ??? // cannot happen
        }
      }
    })
  }

  def merge(base: ParseForest, merging: ParseForest) =
    ParseForest(base.trees ++ merging.trees)
}

object ParseResultTree {

  import Inputs._

  sealed trait Node {
    val start: Inputs.Location
    val end: Inputs.Location

    def range: (Inputs.Location, Inputs.Location) = (start, end)

    def sourceText: String = this match {
      case TerminalNode(_, input) => input.toRawString
      case BindNode(_, body) => body.sourceText
      case JoinNode(_, body, _) => body.sourceText
      case seq: SequenceNode => seq.children.map(_.sourceText).mkString
      case _: CyclicBindNode | _: CyclicSequenceNode =>
        throw new Exception("Cyclic bind")
    }
  }

  case class TerminalNode(start: Inputs.Location, input: Input) extends Node {
    val end: Inputs.Location = start + 1
  }

  case class BindNode(symbol: NSymbol, body: Node) extends Node {
    val start: Inputs.Location = body.start
    val end: Inputs.Location = body.end
  }

  case class CyclicBindNode(start: Inputs.Location, end: Inputs.Location, symbol: NSymbol) extends Node

  case class JoinNode(symbol: NJoin, body: Node, join: Node) extends Node {
    val start: Inputs.Location = body.start
    val end: Inputs.Location = body.end
  }

  case class SequenceNode(start: Inputs.Location, end: Inputs.Location, symbol: NSequence, _children: List[Node]) extends Node {
    // _children: reverse of all children

    def append(child: Node): SequenceNode = {
      SequenceNode(start, child.end, symbol, child +: _children)
    }

    lazy val childrenAll = _children.reverse
    lazy val children = childrenAll
  }

  case class CyclicSequenceNode(start: Inputs.Location, end: Inputs.Location, symbol: NSequence, pointer: Int, _children: List[Node]) extends Node {
    def append(child: Node): CyclicSequenceNode = {
      CyclicSequenceNode(start, child.end, symbol, pointer, child +: _children)
    }

    lazy val childrenAll = _children.reverse
    lazy val children = childrenAll
  }

  object HorizontalTreeStringSeqUtil {
    def merge(list: Seq[(Int, Seq[String])]): (Int, Seq[String]) = {
      val maxSize: Int = (list maxBy {
        _._2.size
      })._2.size
      val fittedList = list map { c =>
        if (c._2.length < maxSize) (c._1, c._2 ++ Seq.fill(maxSize - c._2.size)(" " * c._1))
        else c
      }
      val mergedList = fittedList.tail.foldLeft(fittedList.head) { (memo, e) =>
        (memo._1 + 1 + e._1, memo._2 zip e._2 map { t => t._1 + " " + t._2 })
      }
      mergedList ensuring (mergedList._2 forall {
        _.length == mergedList._1
      })
    }
  }
}
