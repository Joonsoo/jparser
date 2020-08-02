package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.ParseResultTree.{BindNode, JoinNode, Node, SequenceNode, TerminalNode}
import com.giyeok.jparser.Symbols.ShortStringSymbols
import com.giyeok.jparser._
import com.giyeok.jparser.metalang3a.ValuefyExpr.{MatchNonterminal, Unbind, UnrollChoices}
import com.giyeok.jparser.metalang3a.ValuefyExprSimulator._
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor}

class ValuefyExprSimulator(val ngrammar: NGrammar,
                           val startNonterminalName: String,
                           val nonterminalValuefyExprs: Map[String, UnrollChoices],
                           val enumTypesMap: Map[Int, String]) {
    def check(cond: Boolean, msg: => String) = {
        if (!cond) throw new Exception(msg)
    }

    def parse(sourceText: String): Either[Node, ParsingErrors.ParsingError] = new NaiveParser(ngrammar).parse(sourceText) match {
        case Left(ctx) =>
            val tree = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
            tree match {
                case Some(forest) if forest.trees.size == 1 =>
                    Left(forest.trees.head)
                case Some(forest) =>
                    Right(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size))
                case None =>
                    val expectedTerms = ctx.nextGraph.nodes.flatMap { node =>
                        node.kernel.symbol match {
                            case NGrammar.NTerminal(_, term) => Some(term)
                            case _ => None
                        }
                    }
                    Right(ParsingErrors.UnexpectedEOF(expectedTerms, sourceText.length))
            }
        case Right(error) => Right(error)
    }

    def valuefy(sourceText: String): Either[Value, ParsingErrors.ParsingError] =
        parse(sourceText) match {
            case Left(tree) => Left(valuefy(tree))
            case Right(error) => Right(error)
        }

    def startValuefyExpr: ValuefyExpr = Unbind(Symbols.Start,
        Unbind(Symbols.Nonterminal(startNonterminalName),
            MatchNonterminal(startNonterminalName)))

    def valuefy(parseNode: Node): Value = valuefy(parseNode, startValuefyExpr)

    private def unrollRepeat1(node: Node): List[Node] = {
        val BindNode(repeat: NGrammar.NRepeat, body) = node
        body match {
            case BindNode(symbol, repeating: SequenceNode) if symbol.id == repeat.repeatSeq =>
                assert(symbol.id == repeat.repeatSeq)
                val s = repeating.children(1)
                val r = unrollRepeat1(repeating.children(0))
                r :+ s
            case base =>
                List(base)
        }
    }

    private def unrollRepeat0(node: Node): List[Node] = {
        val BindNode(repeat: NGrammar.NRepeat, body) = node
        body match {
            case BindNode(symbol, repeating: SequenceNode) =>
                assert(symbol.id == repeat.repeatSeq)
                val s = repeating.children(1)
                val r = unrollRepeat0(repeating.children(0))
                r :+ s
            case SequenceNode(_, _, symbol, emptySeq) =>
                assert(symbol.id == repeat.baseSeq)
                assert(emptySeq.isEmpty)
                List()
        }
    }

    def valuefy(parseNode: Node, valuefyExpr: ValuefyExpr): Value = valuefyExpr match {
        case ValuefyExpr.InputNode => NodeValue(parseNode)
        case ValuefyExpr.MatchNonterminal(nonterminalName) =>
            val BindNode(bindedSymbol, body) = parseNode
            val nonterminalValuefyExpr = nonterminalValuefyExprs(nonterminalName)
            val matcher = nonterminalValuefyExpr.choices(bindedSymbol.symbol)
            valuefy(body, matcher)
        case Unbind(symbol, expr) =>
            val BindNode(bindedSymbol, body) = parseNode
            check(bindedSymbol.symbol == symbol, s"Invalid unbind expected: ${symbol.toShortString}, actual: ${bindedSymbol.symbol.toShortString}")
            valuefy(body, expr)
        case ValuefyExpr.JoinBody(joinSymbol, bodyProcessor) =>
            val JoinNode(bindedSymbol, body, _) = parseNode
            check(bindedSymbol.symbol == joinSymbol, "Invalid unbind join body")
            valuefy(body, bodyProcessor)
        case ValuefyExpr.JoinCond(joinSymbol, bodyProcessor) =>
            val JoinNode(bindedSymbol, _, join) = parseNode
            check(bindedSymbol.symbol == joinSymbol, "Invalid unbind join cond")
            valuefy(join, bodyProcessor)
        case ValuefyExpr.SeqElemAt(index, expr) =>
            check(parseNode.isInstanceOf[SequenceNode], s"Expected sequence, but actual=unbind(${parseNode.asInstanceOf[BindNode].symbol.symbol.toShortString})")
            val referredElem = parseNode.asInstanceOf[SequenceNode].children(index)
            valuefy(referredElem, expr)
        case ValuefyExpr.UnrollRepeatFromZero(elemProcessor) =>
            val elemNodes = unrollRepeat0(parseNode)
            val elemValues = elemNodes.map(valuefy(_, elemProcessor))
            ArrayValue(elemValues)
        case ValuefyExpr.UnrollRepeatFromOne(elemProcessor) =>
            val elemNodes = unrollRepeat1(parseNode)
            val elemValues = elemNodes.map(valuefy(_, elemProcessor))
            ArrayValue(elemValues)
        case ValuefyExpr.UnrollChoices(choices) =>
            val BindNode(bindedSymbol, body) = parseNode
            val choiceValuefyExpr = choices(bindedSymbol.symbol)
            valuefy(parseNode, choiceValuefyExpr)
        case ValuefyExpr.ConstructCall(className, params) =>
            ClassValue(className, params.map(valuefy(parseNode, _)))
        case ValuefyExpr.FuncCall(funcType, params) =>
            funcType match {
                case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.IsPresent => ???
                case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.IsEmpty => ???
                case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.Chr => ???
                case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.Str =>
                    val vParams = params.map(valuefy(parseNode, _))

                    def stringify(value: Value): String = value match {
                        case NodeValue(astNode) => astNode.sourceText
                        case ClassValue(className, args) => s"$className(${args.map(stringify).mkString(",")})"
                        case ArrayValue(elems) => s"[${elems.map(stringify).mkString(",")}]"
                        case EnumValue(enumType, enumValue) => s"%$enumType.$enumValue"
                        case NullValue => "null"
                        case BoolValue(value) => value.toString
                        case CharValue(value) => value.toString
                        case StringValue(value) => "\"" + value + "\""
                    }

                    StringValue(vParams.map(stringify).mkString)
            }
        case ValuefyExpr.ArrayExpr(elems) =>
            val elemValues = elems.map(valuefy(parseNode, _))
            ArrayValue(elemValues)
        case ValuefyExpr.BinOp(op, lhs, rhs) =>
            val lhsValue = valuefy(parseNode, lhs)
            val rhsValue = valuefy(parseNode, rhs)
            op match {
                case com.giyeok.jparser.metalang3a.ValuefyExpr.BinOpType.ADD =>
                    (lhsValue, rhsValue) match {
                        case (StringValue(lhsValue), StringValue(rhsValue)) => StringValue(lhsValue + rhsValue)
                        case (ArrayValue(lhsElems), ArrayValue(rhsElems)) => ArrayValue(lhsElems ++ rhsElems)
                        case _ =>
                            throw new Exception(s"tried ${lhsValue.prettyPrint()} + ${rhsValue.prettyPrint()}")
                        // TODO exception
                    }
                case com.giyeok.jparser.metalang3a.ValuefyExpr.BinOpType.EQ => ???
                case com.giyeok.jparser.metalang3a.ValuefyExpr.BinOpType.NE => ???
                case com.giyeok.jparser.metalang3a.ValuefyExpr.BinOpType.BOOL_AND => ???
                case com.giyeok.jparser.metalang3a.ValuefyExpr.BinOpType.BOOL_OR => ???
            }
        case ValuefyExpr.PreOp(op, expr) => ???
        case ValuefyExpr.ElvisOp(expr, ifNull) =>
            val exprValue = valuefy(parseNode, expr)
            if (exprValue == NullValue) valuefy(parseNode, ifNull) else exprValue
        case ValuefyExpr.TernaryOp(condition, ifTrue, ifFalse) => ???
        case literal: ValuefyExpr.Literal => literal match {
            case ValuefyExpr.NullLiteral => NullValue
            case ValuefyExpr.BoolLiteral(value) => BoolValue(value)
            case ValuefyExpr.CharLiteral(value) => CharValue(value)
            case ValuefyExpr.CharFromTerminalLiteral =>
                CharValue(parseNode.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)
            case ValuefyExpr.StringLiteral(value) => StringValue(value)
        }
        case value: ValuefyExpr.EnumValue => value match {
            case ValuefyExpr.CanonicalEnumValue(enumName, enumValue) => EnumValue(enumName, enumValue)
            case ValuefyExpr.ShortenedEnumValue(unspecifiedEnumTypeId, enumValue) =>
                EnumValue(enumTypesMap(unspecifiedEnumTypeId), enumValue)
        }
    }
}

object ValuefyExprSimulator {

    abstract sealed class Value {
        def prettyPrint(): String

        def detailPrint(): String
    }

    case class NodeValue(astNode: Node) extends Value {
        override def prettyPrint(): String = astNode.sourceText

        override def detailPrint(): String = s"node(${astNode.sourceText})"
    }

    case class ClassValue(className: String, args: List[Value]) extends Value {
        override def prettyPrint(): String = s"$className(${args.map(_.prettyPrint()).mkString(",")})"

        override def detailPrint(): String = prettyPrint()
    }

    case class ArrayValue(elems: List[Value]) extends Value {
        override def prettyPrint(): String = s"[${elems.map(_.prettyPrint()).mkString(",")}]"

        override def detailPrint(): String = prettyPrint()
    }

    case class EnumValue(enumType: String, enumValue: String) extends Value {
        override def prettyPrint(): String = s"%$enumType.$enumValue"

        override def detailPrint(): String = prettyPrint()
    }

    case object NullValue extends Value {
        override def prettyPrint(): String = "null"

        override def detailPrint(): String = "null"
    }

    case class BoolValue(value: Boolean) extends Value {
        override def prettyPrint(): String = s"$value"

        override def detailPrint(): String = s"Bool($value)"
    }

    case class CharValue(value: Char) extends Value {
        override def prettyPrint(): String = s"'$value'"

        override def detailPrint(): String = s"Char($value)"
    }

    case class StringValue(value: String) extends Value {
        override def prettyPrint(): String = "\"" + value + "\""

        override def detailPrint(): String = s"String($value)"
    }

}
