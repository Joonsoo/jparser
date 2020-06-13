package com.giyeok.jparser.metalang3.codegen

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.Nonterminal
import com.giyeok.jparser.metalang3._

class ScalaGen {

    case class CodeBlock(code: String, requirements: Set[String]) {
        def append(codeBlock: CodeBlock): CodeBlock =
            CodeBlock(code + "\n" + codeBlock.code, requirements ++ codeBlock.requirements)

        def addRequirement(requirement: String): CodeBlock = CodeBlock(code, requirements + requirement)

        def removeRequirement(requirement: String): CodeBlock = CodeBlock(code, requirements - requirement)
    }

    private var _argNum = 0

    private def nextArgName(): String = {
        _argNum += 1
        "v" + _argNum
    }

    case class ValueifierCode(codes: List[String], result: String, requirements: Set[String])

    private def firstCharUpperCase(name: String) = s"${name.charAt(0).toUpper}${name.substring(1)}"

    private def matchFuncNameForNonterminal(nonterminal: Nonterminal): String =
        s"match${firstCharUpperCase(nonterminal.name.name.sourceText)}"

    // TODO
    private def symbolIdOf(symbol: MetaGrammar3Ast.Symbol): Int = 1

    def valueifyExprCode(expr: ValueifyExpr, inputName: String): ValueifierCode = expr match {
        case InputNode => ValueifierCode(List(), inputName, Set())
        case MatchNonterminal(nonterminal, expr) =>
            val e = valueifyExprCode(expr, inputName)
            val v = nextArgName()
            val matchFunc = matchFuncNameForNonterminal(nonterminal)
            ValueifierCode(e.codes :+ s"val $v = $matchFunc(${e.result})", v, e.requirements)
        case SeqElemAt(expr, index) =>
            val e = valueifyExprCode(expr, inputName)
            val v = nextArgName()
            ValueifierCode(e.codes :+ s"val $v = ${e.result}.asInstanceOf[SequenceNode].children($index)", v, e.requirements)
        case Unbind(symbol, expr) =>
            val e = valueifyExprCode(expr, inputName)
            val v1 = nextArgName()
            val v2 = nextArgName()
            val bindedSymbolId = symbolIdOf(symbol)
            ValueifierCode(e.codes ++ List(
                s"val BindNode($v1, $v2) = ${e.result}",
                s"assert($v1.id == $bindedSymbolId)"
            ), v2, e.requirements)
        case JoinBodyOf(expr) => ???
        case JoinCondOf(expr) => ???
        case ExceptBodyOf(expr) => ???
        case ExceptCondOf(expr) => ???
        case UnrollOptional(expr) => ???
        case UnrollRepeatFromZero(expr) => ???
        case UnrollRepeatFromOne(expr) => ???
        case UnrollLongest(expr) => ???
        case UnrollChoices(map) => ???
        case NamedConstructCall(className, params) =>
            val paramCodes = params.map { param => valueifyExprCode(param._2._1, inputName) }
            val codes = paramCodes.foldLeft(List[String]()) { (m, i) => m ++ i.codes }
            val args = paramCodes.map(_.result)
            val createStmt = s"${className.astNode.sourceText}(${args.mkString(", ")})" // TODO
            ValueifierCode(codes, createStmt, paramCodes.foldLeft(Set[String]()) { (m, i) => m ++ i.requirements })
        case UnnamedConstructCall(className, params) => ???
        case FuncCall(funcName, params) => ???
        case ArrayExpr(elems) => ???
        case PrefixOp(prefixOpType, expr, exprType) => ???
        case BinOp(op, lhs, rhs, lhsType, rhsType) => ???
        case ElvisOp(expr, ifNull) => ???
        case TernateryExpr(condition, ifTrue, ifFalse, conditionType) => ???
        case literal: Literal =>
            literal match {
                case NullLiteral => ValueifierCode(List(), "null", Set())
                case BoolLiteral(value) => ValueifierCode(List(), s"$value", Set())
                case CharLiteral(value) => ???
                case StringLiteral(value) => ???
            }
        case value: EnumValue => ???
    }
}
