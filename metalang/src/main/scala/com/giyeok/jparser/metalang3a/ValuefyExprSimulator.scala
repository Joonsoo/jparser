package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.ParseResultTree.{BindNode, JoinNode, Node, SequenceNode}
import com.giyeok.jparser.Symbols.ShortStringSymbols
import com.giyeok.jparser.metalang3a.ValuefyExpr.{MatchNonterminal, Unbind, UnrollChoices}
import com.giyeok.jparser.metalang3a.ValuefyExprSimulator._
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor}
import com.giyeok.jparser._

class ValuefyExprSimulator(val ngrammar: NGrammar,
                           val startNonterminalName: String,
                           val nonterminalValuefyExprs: Map[String, UnrollChoices]) {
    def check(cond: Boolean, msg: String) = {
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

    def valuefy(parseNode: Node): Value =
        valuefy(parseNode,
            Unbind(Symbols.Start, Unbind(Symbols.Nonterminal(startNonterminalName),
                MatchNonterminal(startNonterminalName))))

    def printNodeStructure(parseNode: Node, indent: String = ""): Unit = parseNode match {
        case ParseResultTree.TerminalNode(start, input) => println(s"${indent}Terminal ${input}")
        case BindNode(symbol, body) =>
            println(s"${indent}Bind(${symbol.symbol.toShortString})")
            printNodeStructure(body, indent + "  ")
        case ParseResultTree.CyclicBindNode(start, end, symbol) =>
            println(s"${indent}Cyclic Bind")
        case JoinNode(symbol, body, join) =>
            println(s"${indent}Join(${symbol.symbol.toShortString})")
            printNodeStructure(body, indent + "  ")
            printNodeStructure(join, indent + "  ")
        case seq@SequenceNode(start, end, symbol, _children) =>
            println(s"${indent}Sequence(${symbol.symbol.toShortString})")
            seq.children.zipWithIndex.foreach { childIdx =>
                printNodeStructure(childIdx._1, indent + "  ")
            }
        case ParseResultTree.CyclicSequenceNode(start, end, symbol, pointer, _children) =>
            println(s"${indent}Cyclic Sequence")
    }

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
            check(bindedSymbol.symbol == symbol, "Invalid unbind")
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
            valuefy(body, choiceValuefyExpr)
        case ValuefyExpr.ConstructCall(className, params) =>
            ClassValue(className, params.map(valuefy(parseNode, _)))
        case ValuefyExpr.FuncCall(funcType, params) =>
            funcType match {
                case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.IsPresent => ???
                case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.IsEmpty => ???
                case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.Chr => ???
                case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.Str => ???
            }
        case ValuefyExpr.ArrayExpr(elems) =>
            val elemValues = elems.map(valuefy(parseNode, _))
            ArrayValue(elemValues)
        case ValuefyExpr.BinOp(op, lhs, rhs) => ???
        case ValuefyExpr.PreOp(op, expr) => ???
        case ValuefyExpr.ElvisOp(expr, ifNull) => ???
        case ValuefyExpr.TernaryOp(condition, ifTrue, ifFalse) => ???
        case literal: ValuefyExpr.Literal => literal match {
            case ValuefyExpr.NullLiteral => NullValue()
            case ValuefyExpr.BoolLiteral(value) => BoolValue(value)
            case ValuefyExpr.CharLiteral(value) => CharValue(value)
            case ValuefyExpr.StringLiteral(value) => StringValue(value)
        }
        case value: ValuefyExpr.EnumValue => ???
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

    case class NullValue() extends Value {
        override def prettyPrint(): String = "null"

        override def detailPrint(): String = "null"
    }

    case class BoolValue(value: Boolean) extends Value {
        override def prettyPrint(): String = s"$value"

        override def detailPrint(): String = s"Bool($value)"
    }

    case class CharValue(value: Char) extends Value {
        override def prettyPrint(): String = s"$value"

        override def detailPrint(): String = s"Char($value)"
    }

    case class StringValue(value: String) extends Value {
        override def prettyPrint(): String = "\"" + value + "\""

        override def detailPrint(): String = s"String($value)"
    }

}
