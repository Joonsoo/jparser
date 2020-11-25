package com.giyeok.jparser.metalang3.valueify

import com.giyeok.jparser.{NGrammar, ParseForestFunc, ParsingErrors}
import com.giyeok.jparser.ParseResultTree.{BindNode, JoinNode, Node, SequenceNode}
import com.giyeok.jparser.metalang3.AnalysisResult
import com.giyeok.jparser.metalang3.valueify.ValueifyInterpreter._
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor}

// TODO ParseResultTree.Node, ValueifyExpr 받아서 결과물 반환
class ValueifyInterpreter(val analysis: AnalysisResult) {

    def check(cond: Boolean, msg: String): Unit = {
        // TODO
    }

    def valueify(sourceText: String): Either[Value, ParsingErrors.ParsingError] =
        new NaiveParser(analysis.ngrammar).parse(sourceText) match {
            case Left(ctx) =>
                val tree = new ParseTreeConstructor(ParseForestFunc)(analysis.ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
                tree match {
                    case Some(forest) if forest.trees.size == 1 =>
                        Left(valueify(forest.trees.head, analysis.valueifyExprsMap(analysis.startSymbolName)))
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

    def valueify(node: Node, valueifyExpr: ValueifyExpr): Value = valueifyExpr match {
        case InputNode(_) => NodeValue(node)
        case MatchNonterminal(nonterminalName, expr, _) =>
            val e = valueify(node, expr)
            assert(e.isInstanceOf[NodeValue])
            val matcher = analysis.valueifyExprsMap(nonterminalName)
            valueify(e.asInstanceOf[NodeValue].astNode, matcher)
        case Unbind(symbol, expr) =>
            val BindNode(bindSymbol, body) = node
            check(bindSymbol.id == analysis.symbolOf(???).id, "")
            valueify(body, expr)
        case JoinBodyOf(joinSymbol, joinExpr, bodyProcessorExpr, _) =>
            //            val JoinNode(bindSymbol, body, _) = node
            //            check(bindSymbol.id == analysis.symbolOf(joinSymbol).id, "")
            //            valueify(body, joinExpr)
            ???
        case JoinCondOf(joinSymbol, joinExpr, condProcessorExpr, resultType) =>
            //            val JoinNode(bindSymbol, _, cond) = node
            //            check(bindSymbol.id == analysis.symbolOf(joinSymbol).id, "")
            //            valueify(cond, joinExpr)
            ???
        case SeqElemAt(expr, index, resultType) =>
            valueify(node.asInstanceOf[SequenceNode].children(index), expr)
        case UnrollRepeat(minimumRepeat, arrayExpr, elemProcessExpr) =>
            ???
        case UnrollChoices(choiceExpr, map, resultType) =>
            val BindNode(bindSymbol, body) = node
            val mappers = map.map(m => m._1 match {
                case AstSymbolChoice(symbol) =>
                    analysis.symbolsMap(symbol).id -> m._2
                case GrammarSymbolChoice(symbol) =>
                    analysis.ngrammar.findSymbol(symbol).get._1 -> m._2
            })
            bindSymbol.id
            ???
        case NamedConstructCall(className, params) =>
            ClassValue(node, className, params.map(p => valueify(node, p._2)))
        case UnnamedConstructCall(className, params) =>
            ClassValue(node, className, params.map(p => valueify(node, p)))
        case FuncCall(funcName, params, resultType) =>
            ???
        case ArrayExpr(elems, resultType) =>
            ???
        case PrefixOp(prefixOpType, expr, resultType) =>
            ???
        case BinOp(op, lhs, rhs, resultType) =>
            ???
        case ElvisOp(expr, ifNull) =>
            ???
        case TernaryExpr(condition, ifTrue, ifFalse, resultType) =>
            ???
        case literal: Literal =>
            literal match {
                case NullLiteral => NullValue(node)
                case BoolLiteral(value) => BoolValue(node, value)
                case CharLiteral(value) => CharValue(node, value)
                case StringLiteral(value) => StringValue(node, value)
            }
        case value: EnumValue => ???
    }
}

object ValueifyInterpreter {

    case class ValueifyException(msg: String) extends Exception

    abstract sealed class Value {
        val astNode: Node

        def prettyPrint(): String
    }

    case class NodeValue(override val astNode: Node) extends Value {
        override def prettyPrint(): String = astNode.sourceText
    }

    case class ClassValue(override val astNode: Node,
                          className: String, args: List[Value]) extends Value {
        override def prettyPrint(): String = s"$className(${args.map(_.prettyPrint()).mkString(",")})"
    }

    case class ArrayValue(override val astNode: Node, elems: List[Value]) extends Value {
        override def prettyPrint(): String = s"[${elems.map(_.prettyPrint()).mkString(",")}]"
    }

    case class EnumValue(override val astNode: Node, enumType: String, enumValue: String) extends Value {
        override def prettyPrint(): String = s"%$enumType.$enumValue"
    }

    case class NullValue(override val astNode: Node) extends Value {
        override def prettyPrint(): String = "null"
    }

    case class BoolValue(override val astNode: Node, value: Boolean) extends Value {
        override def prettyPrint(): String = s"$value"
    }

    case class CharValue(override val astNode: Node, value: Char) extends Value {
        override def prettyPrint(): String = s"$value"
    }

    case class StringValue(override val astNode: Node, value: String) extends Value {
        override def prettyPrint(): String = "\"" + value + "\""
    }

}
