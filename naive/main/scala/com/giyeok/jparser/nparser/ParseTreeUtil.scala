package com.giyeok.jparser.nparser

import com.giyeok.jparser.ParseResultTree.{BindNode, Node, SequenceNode}
import com.giyeok.jparser.nparser.Parser.NaiveContext
import com.giyeok.jparser.{NGrammar, ParseForest, ParseForestFunc, ParsingErrors, Symbols}

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

  def unrollRepeat1NoUnbind(repeat: NGrammar.NRepeat, node: Node): List[Node] =
    unrollRepeat1NoUnbind(repeat.repeatSeq, repeat.baseSeq, node)

  def unrollRepeat1NoUnbind(repeatSeqId: Int, baseSymbolId: Int, node: Node): List[Node] = {
    node match {
      case BindNode(symbol, repeating: SequenceNode) if symbol.id == repeatSeqId =>
        val s = repeating.children(1)
        val r = unrollRepeat1(repeating.children.head)
        r :+ s
      case base =>
        // TODO check baseSymbolId
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

  def unrollRepeat0NoUnbind(repeat: NGrammar.NRepeat, node: Node): List[Node] =
    unrollRepeat0NoUnbind(repeat.repeatSeq, repeat.baseSeq, node)

  def unrollRepeat0NoUnbind(repeatSeqId: Int, baseSeqId: Int, node: Node): List[Node] = {
    val BindNode(bodySymbol, bodyNode) = node
    if (bodySymbol.id == repeatSeqId) {
      val bodySeq = bodyNode.asInstanceOf[SequenceNode]
      unrollRepeat0(bodySeq.children.head) :+ bodySeq.children(1)
    } else {
      assert(bodySymbol.id == baseSeqId)
      List()
    }
  }

  def reconstructTree(ngrammar: NGrammar, ctx: NaiveContext): Option[ParseForest] =
    new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()

  def parseAst[T](ngrammar: NGrammar, ctx: NaiveContext, matchStart: Node => T): Either[T, ParsingErrors.ParsingError] =
    reconstructTree(ngrammar, ctx) match {
      case Some(forest) if forest.trees.size == 1 => Left(matchStart(forest.trees.head))
      case Some(forest) => Right(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size))
      case None =>
        Right(ParsingErrors.UnexpectedEOF(expectedTermsFrom(ngrammar, ctx), ctx.gen))
    }

  def parseAst[T](parser: NaiveParser, text: String, matchStart: Node => T): Either[T, ParsingErrors.ParsingError] =
    parser.parse(text) match {
      case Left(ctx) => parseAst(parser.grammar, ctx, matchStart)
      case Right(error) => Right(error)
    }

  def expectedTermsFrom(ngrammar: NGrammar, ctx: NaiveContext): Set[Symbols.Terminal] = ctx.nextGraph.nodes.flatMap { node =>
    ngrammar.nsymbols.get(node.kernel.symbolId) match {
      case Some(NGrammar.NTerminal(_, term)) => Some(term)
      case _ => None
    }
  }
}
