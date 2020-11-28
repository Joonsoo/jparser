package com.giyeok.jparser.nparser

import com.giyeok.jparser.{NGrammar, ParseForestFunc, ParsingErrors}
import com.giyeok.jparser.ParseResultTree.{BindNode, Node, SequenceNode}
import com.giyeok.jparser.nparser.Parser.NaiveContext

object ParseTreeUtil {
  def unrollRepeat1(node: Node): List[Node] = {
    val BindNode(repeat: NGrammar.NRepeat, body) = node
    body match {
      case BindNode(symbol, repeating: SequenceNode) if symbol.id == repeat.repeatSeq =>
        assert(symbol.id == repeat.repeatSeq)
        val s = repeating.children(1)
        val r = unrollRepeat1(repeating.children.head)
        r :+ s
      case base =>
        List(base)
    }
  }

  def unrollRepeat0(node: Node): List[Node] = {
    val BindNode(repeat: NGrammar.NRepeat, BindNode(bodySymbol, bodyNode)) = node
    if (bodySymbol.id == repeat.repeatSeq) {
      val bodySeq = bodyNode.asInstanceOf[SequenceNode]
      unrollRepeat0(bodySeq.children.head) :+ bodySeq.children(1)
    } else {
      assert(bodySymbol.id == repeat.baseSeq)
      List()
    }
  }

  def parseAst[T](ngrammar: NGrammar, ctx: NaiveContext, matchStart: Node => T): Either[T, ParsingErrors.ParsingError] =
    new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct() match {
      case Some(forest) if forest.trees.size == 1 => Left(matchStart(forest.trees.head))
      case Some(forest) => Right(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size))
      case None =>
        val expectedTerms = ctx.nextGraph.nodes.flatMap { node =>
          node.kernel.symbol match {
            case NGrammar.NTerminal(_, term) => Some(term)
            case _ => None
          }
        }
        Right(ParsingErrors.UnexpectedEOF(expectedTerms, ctx.gen))
    }

  def parseAst[T](parser: NaiveParser, text: String, matchStart: Node => T): Either[T, ParsingErrors.ParsingError] =
    parser.parse(text) match {
      case Left(ctx) => parseAst(parser.grammar, ctx, matchStart)
      case Right(error) => Right(error)
    }
}
