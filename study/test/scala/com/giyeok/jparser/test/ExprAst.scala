package com.giyeok.jparser.test

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.ParseResultTree.BindNode
import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.ParseResultTree.SequenceNode
import com.giyeok.jparser.ParseResultTree.TerminalNode
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.milestone.MilestoneParser
import com.giyeok.jparser.milestone.MilestoneParserContext
import com.giyeok.jparser.nparser.ParseTreeUtil
import com.giyeok.jparser.proto.GrammarProto
import com.giyeok.jparser.proto.GrammarProtobufConverter
import com.giyeok.jparser.proto.MilestoneParserDataProto
import com.giyeok.jparser.proto.MilestoneParserProtobufConverter
import com.giyeok.jparser.utils.FileUtil.readFileBytes
import java.util.Base64

object ExprAst {

  sealed trait WithParseNode { val parseNode: Node }
  case class BinOp(op: Op.Value, lhs: Term, rhs: Expr)(override val parseNode: Node) extends Term with WithParseNode
  sealed trait Expr extends WithParseNode
  sealed trait Factor extends Term with WithParseNode
  case class Number(value: String)(override val parseNode: Node) extends Factor with WithParseNode
  case class Paren(body: Expr)(override val parseNode: Node) extends Factor with WithParseNode
  sealed trait Term extends Expr with WithParseNode
  object Op extends Enumeration { val Add, Mul = Value }

  def matchExpr(node: Node): Expr = {
    val BindNode(v1, v2) = node
    val v12 = v1.id match {
      case 3 =>
        val v3 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v4, v5) = v3
        assert(v4.id == 4)
        val v6 = v2.asInstanceOf[SequenceNode].children(4)
        val BindNode(v7, v8) = v6
        assert(v7.id == 2)
        BinOp(Op.Add, matchTerm(v5), matchExpr(v8))(v2)
      case 21 =>
        val v9 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v10, v11) = v9
        assert(v10.id == 4)
        matchTerm(v11)
    }
    v12
  }

  def matchFactor(node: Node): Factor = {
    val BindNode(v13, v14) = node
    val v21 = v13.id match {
      case 7 =>
        val v15 = v14.asInstanceOf[SequenceNode].children.head
        val BindNode(v16, v17) = v15
        assert(v16.id == 8)
        Number(v17.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString)(v14)
      case 9 =>
        val v18 = v14.asInstanceOf[SequenceNode].children(2)
        val BindNode(v19, v20) = v18
        assert(v19.id == 2)
        Paren(matchExpr(v20))(v14)
    }
    v21
  }

  def matchTerm(node: Node): Term = {
    val BindNode(v22, v23) = node
    val v33 = v22.id match {
      case 5 =>
        val v24 = v23.asInstanceOf[SequenceNode].children.head
        val BindNode(v25, v26) = v24
        assert(v25.id == 6)
        val v27 = v23.asInstanceOf[SequenceNode].children(4)
        val BindNode(v28, v29) = v27
        assert(v28.id == 4)
        BinOp(Op.Mul, matchFactor(v26), matchTerm(v29))(v23)
      case 19 =>
        val v30 = v23.asInstanceOf[SequenceNode].children.head
        val BindNode(v31, v32) = v30
        assert(v31.id == 6)
        matchFactor(v32)
    }
    v33
  }

  def matchStart(node: Node): Expr = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchExpr(body)
  }

    val milestoneParserData = MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(
      MilestoneParserDataProto.MilestoneParserData.parseFrom(readFileBytes("/home/joonsoo/Documents/workspace/jparser/fast/src/test/resources/parserdata.pb")))

val milestoneParser = new MilestoneParser(milestoneParserData)

def parse(text: String): Either[ParseForest, ParsingErrors.ParsingError] =
  milestoneParser.parseAndReconstructToForest(text)

def parseAst(text: String): Either[Expr, ParsingErrors.ParsingError] =
  parse(text) match {
    case Left(forest) => Left(matchStart(forest.trees.head))
    case Right(error) => Right(error)
  }


}
