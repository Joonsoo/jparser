package com.giyeok.jparser.nparser

import com.giyeok.jparser.Inputs.Character
import com.giyeok.jparser.ParseResultTree.{BindNode, JoinNode, TerminalNode}
import com.giyeok.jparser.{ParseResultTree, Symbols}
import org.scalatest.matchers.{MatchResult, Matcher}

object ParseTreeMatchers {

  sealed abstract class TreeMatcher extends Matcher[ParseResultTree.Node]

  case class TermM(expectedChar: Char) extends TreeMatcher {
    override def apply(left: ParseResultTree.Node): MatchResult = left match {
      case TerminalNode(_, Character(actualChar)) if actualChar == expectedChar =>
        MatchResult(matches = true, "", "")
      case _ =>
        MatchResult(matches = false, s"Term($expectedChar) did not match to $left", "error")
    }
  }

  case class BindM(expectedSymbol: Symbols.Symbol, expectedBody: TreeMatcher) extends TreeMatcher {
    override def apply(left: ParseResultTree.Node): MatchResult = left match {
      case BindNode(actualSymbol, actualBody) if actualSymbol.symbol == expectedSymbol =>
        expectedBody(actualBody)
      case BindNode(actualSymbol, _) => MatchResult(matches = false,
        s"Bind did not match, expected=${expectedSymbol.toShortString}, actual=${actualSymbol.symbol.toShortString}",
        "")
      case _ => MatchResult(matches = false,
        s"Bind did not match, expected=${expectedSymbol.toShortString}",
        "")
    }
  }

  case class JoinM(expectedBody: TreeMatcher, expectedJoin: TreeMatcher) extends TreeMatcher {
    override def apply(left: ParseResultTree.Node): MatchResult = left match {
      case JoinNode(actualBody, actualJoin)
        if expectedBody(actualBody).matches && expectedJoin(actualJoin).matches =>
        MatchResult(matches = true, "", "")
      case _ => MatchResult(matches = false, "join not matched", "join not matched")
    }
  }

  case class SeqM(expected: List[TreeMatcher]) extends TreeMatcher {
    override def apply(left: ParseResultTree.Node): MatchResult = left match {
      case actual: ParseResultTree.SequenceNode
        if actual.children.size == expected.size &&
          actual.children.zip(expected).forall(pair => pair._2(pair._1).matches) =>
        MatchResult(matches = true, "", "")
      case actual: ParseResultTree.SequenceNode if actual.children.size != expected.size => MatchResult(matches = false,
        s"Seq match failed, expectedLength=${expected.size}, actualLength=${actual.children.size}",
        "")
      case actual: ParseResultTree.SequenceNode =>
        val mismatch = actual.children.zip(expected).map(pair => pair._2(pair._1).matches).zipWithIndex
          .filter(!_._1).map(_._2)
        MatchResult(matches = false,
          s"Seq match failed, mismatched at $mismatch",
          "")
      case actual => MatchResult(matches = false,
        s"Seq match failed, actual=$actual",
        "")
    }
  }

  object SeqM {
    def apply(expected: TreeMatcher*): SeqM = new SeqM(expected.toList)
  }

  object DontCare extends TreeMatcher {
    override def apply(left: ParseResultTree.Node): MatchResult = MatchResult(matches = true, "", "")
  }

}
