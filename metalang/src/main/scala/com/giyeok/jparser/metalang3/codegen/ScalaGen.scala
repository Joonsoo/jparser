package com.giyeok.jparser.metalang3.codegen

import com.giyeok.jparser.metalang3.MetaLanguage3.IllegalGrammar
import com.giyeok.jparser.metalang3.codegen.ScalaGen.Options
import com.giyeok.jparser.metalang3.types.{ConcreteType, TypeFunc}
import com.giyeok.jparser.metalang3.valueify._
import com.giyeok.jparser.metalang3.{valueify, _}

// TODO codegen options
// - null vs Option
// - mostSpecificSuperTypeOf 옵션(extensive하지 않은 super type을 허용할지)

object ScalaGen {

    /**
     *
     * @param useNull              (미구현) true이면 Option 대신 그냥 값+null을 사용한다.
     * @param looseSuperType       TODO 기본 false
     * @param assertBindTypes      Unbind할 때 unbind된 심볼의 타입을 체크한다. 기본 true
     * @param symbolComments       human readable한 심볼 설명 코멘트를 추가한다. 기본 true
     * @param astNodesInAllClasses 모든 클래스에 astNode를 기본으로 포함시킨다. 기본 true
     */
    case class Options(useNull: Boolean = false,
                       looseSuperType: Boolean = false,
                       assertBindTypes: Boolean = true,
                       symbolComments: Boolean = true,
                       astNodesInAllClasses: Boolean = true)

}

// TODO 값이 가는 곳이 OptionalType인데 주는 값은 아니면, Some(**)로 넣어주도록
// - 값을 어딘가에 넘겨주는 경우에만.
// - NamedConstructCall, UnnamedConstructCall, FuncCall, ArrayExpr, PrefixOp, BinOp, ElvisOp, TernaryExpr
// - UnrollChoices에서도 resultType은 optional인데 case 안의 타입이 아닌 경우 Some(**)
class ScalaGen(val analysis: AnalysisResult, val options: Options = Options()) {
    private var _argNum = 0

    private def newVarName(): String = {
        _argNum += 1
        "v" + _argNum
    }

    case class ValueifierCode(codes: List[String], result: String, requirements: Set[String])

    case class CodeBlock(codes: List[String])

    private def firstCharUpperCase(name: String) = s"${name.charAt(0).toUpper}${name.substring(1)}"

    def matchFuncNameForNonterminal(nonterminalName: String): String =
        s"match${firstCharUpperCase(nonterminalName)}"

    def typeDescStringOf(concreteType: ConcreteType): String = concreteType match {
        case ConcreteType.NodeType => "Node"
        case ConcreteType.ClassType(name) => name
        case ConcreteType.OptionalOf(typ) => s"Option[${typeDescStringOf(typ)}]"
        case ConcreteType.ArrayOf(elemType) => s"List[${typeDescStringOf(elemType)}]"
        case ConcreteType.EnumType(enumName) => enumName
        case ConcreteType.NullType => "Nothing"
        case ConcreteType.BoolType => "Boolean"
        case ConcreteType.CharType => "Char"
        case ConcreteType.StringType => "String"
        case ConcreteType.UnionOf(types) =>
            throw IllegalGrammar(s"Union type is not supported: Union(${types.map(typeDescStringOf).mkString(", ")})")
    }

    private def escapeChar(char: Char): String = s"'$char'"

    private def escapeString(str: String): String = s""""$str""""

    private def collectCodesFrom(codes: List[ValueifierCode]) = codes.foldLeft(List[String]())(_ ++ _.codes)

    private def collectRequirementsFrom(codes: List[ValueifierCode]) = codes.foldLeft(Set[String]())(_ ++ _.requirements)

    private def realTypeOf(typeFunc: TypeFunc): ConcreteType =
        analysis.mostSpecificSuperTypeOf(analysis.concreteTypeOf(typeFunc), options.looseSuperType)

    def valueifyExprCode(expr: ValueifyExpr, inputName: String): ValueifierCode = expr match {
        case InputNode(_) => ValueifierCode(List(), inputName, Set())
        case MatchNonterminal(nonterminal, expr, _) =>
            val e = valueifyExprCode(expr, inputName)
            val v = newVarName()
            val matchFunc = matchFuncNameForNonterminal(nonterminal)
            ValueifierCode(e.codes :+ s"val $v = $matchFunc(${e.result})", v, e.requirements)
        case SeqElemAt(expr, index, _) =>
            val e = valueifyExprCode(expr, inputName)
            val v = newVarName()
            ValueifierCode(e.codes :+ s"val $v = ${e.result}.asInstanceOf[SequenceNode].children($index)",
                v, e.requirements + "jparser.SequenceNode")
        case Unbind(symbol, expr) =>
            val e = valueifyExprCode(expr, inputName)
            val v1 = newVarName()
            val v2 = newVarName()
            val bindedSymbol = analysis.symbolOf(symbol)
            ValueifierCode(e.codes ++
                List(s"val BindNode($v1, $v2) = ${e.result}" + (if (options.symbolComments) s" // id=${bindedSymbol.id} ${bindedSymbol.symbol.toShortString}" else "")) ++
                (if (options.assertBindTypes) List(s"assert($v1.id == ${bindedSymbol.id})") else List())
                , v2, e.requirements + "jparser.BindNode")
        case JoinBodyOf(joinSymbol, joinExpr, bodyProcessorExpr, _) =>
            val join = valueifyExprCode(joinExpr, inputName)
            val v1 = newVarName()
            val v2 = newVarName()
            val processor = valueifyExprCode(bodyProcessorExpr, v2)
            val symbol = analysis.symbolOf(joinSymbol)
            ValueifierCode(join.codes ++
                List(s"val JoinNode($v1, $v2, _) = ${join.result}") ++
                (if (options.assertBindTypes) List(s"assert($v1.id == ${symbol.id})") else List()) ++
                processor.codes, processor.result, join.requirements ++ processor.requirements + "jparser.JoinNode")
        case JoinCondOf(joinSymbol, joinExpr, condProcessorExpr, _) =>
            val join = valueifyExprCode(joinExpr, inputName)
            val v1 = newVarName()
            val v2 = newVarName()
            val processor = valueifyExprCode(condProcessorExpr, v2)
            val symbol = analysis.symbolOf(joinSymbol)
            ValueifierCode(join.codes ++
                List(s"val JoinNode($v1, _, $v2) = ${join.result}") ++
                (if (options.assertBindTypes) List(s"assert($v1.id == ${symbol.id})") else List()) ++
                processor.codes, processor.result, join.requirements ++ processor.requirements + "jparser.JoinNode")
        case UnrollRepeat(minimumRepeat, arrayExpr, elemProcessExpr, _) =>
            // inputName을 repeatSymbol로 unroll repeat하고, 각각을 expr로 가공
            val arrayExprCode = valueifyExprCode(arrayExpr, inputName)
            val elemProcessCode = valueifyExprCode(elemProcessExpr, "elem")
            val funcName = s"ASTUtils.unrollRepeat$minimumRepeat"
            val v = newVarName()
            ValueifierCode(
                arrayExprCode.codes ++
                    List(s"val $v = $funcName(${arrayExprCode.result}) map { elem =>") ++
                    elemProcessCode.codes ++
                    List(elemProcessCode.result,
                        "}"),
                v, arrayExprCode.requirements ++ elemProcessCode.requirements + funcName)
        case UnrollChoices(choiceExpr, mappings, resultType) =>
            val vChoiceExpr = valueifyExprCode(choiceExpr, inputName)
            val symbol = newVarName()
            val body = newVarName()
            val v = newVarName()
            var codes = vChoiceExpr.codes ++ List(
                s"val BindNode($symbol, $body) = ${vChoiceExpr.result}",
                s"val $v = $symbol.id match {"
            )
            var requirements = vChoiceExpr.requirements
            mappings.foreach { mapping =>
                val symbol = mapping._1 match {
                    case AstSymbolChoice(symbol) => analysis.symbolOf(symbol)
                    case GrammarSymbolChoice(symbol) => analysis.ngrammar.findSymbol(symbol).get._2
                }
                val vMapper = valueifyExprCode(mapping._2, body)
                codes :+= s"case ${symbol.id} =>" + (if (options.symbolComments) s" // ${symbol.symbol.toShortString}" else "")
                codes ++= vMapper.codes
                codes :+= vMapper.result
                requirements ++= vMapper.requirements
            }
            codes :+= "}"
            ValueifierCode(codes, v, requirements)
        case NamedConstructCall(className, params, _) =>
            val vParams = params.map { param => valueifyExprCode(param._2, inputName) }
            val createExpr = s"$className(${vParams.map(_.result).mkString(", ")})"
            ValueifierCode(collectCodesFrom(vParams), createExpr, collectRequirementsFrom(vParams))
        case UnnamedConstructCall(className, params, _) =>
            val vParams = params.map { param => valueifyExprCode(param, inputName) }
            val createExpr = s"$className(${vParams.map(_.result).mkString(", ")})"
            ValueifierCode(collectCodesFrom(vParams), createExpr, collectRequirementsFrom(vParams))
        case FuncCall(funcName, params, _) =>
            val scalaFuncName = funcName match {
                case "isempty" => "ASTUtils.isEmpty"
                case "ispresent" => "ASTUtils.isPresent"
                case "chr" => "ASTUtils.toChar"
                case "str" => "ASTUtils.toString"
            }
            val vParams = params.map { param => valueifyExprCode(param, inputName) }
            val args = vParams.map(_.result)
            val callExpr = s"$scalaFuncName(${args.mkString(", ")})"
            ValueifierCode(collectCodesFrom(vParams), callExpr, collectRequirementsFrom(vParams) + scalaFuncName)
        case ArrayExpr(elems, _) =>
            val vElems = elems.map(valueifyExprCode(_, inputName))
            ValueifierCode(collectCodesFrom(vElems), s"List(${vElems.map(_.result).mkString(", ")})", collectRequirementsFrom(vElems))
        case PrefixOp(prefixOpType, expr, _) =>
            val vExpr = valueifyExprCode(expr, inputName)
            val result = prefixOpType match {
                case PreOp.NOT => s"! ${vExpr.result}"
            }
            ValueifierCode(vExpr.codes, result, vExpr.requirements)
        case BinOp(op, lhs, rhs, _) =>
            val vLhs = valueifyExprCode(lhs, inputName)
            val vRhs = valueifyExprCode(rhs, inputName)
            val result = op match {
                case Op.ADD =>
                    val vLhsType = realTypeOf(lhs.resultType)
                    val vRhsType = realTypeOf(rhs.resultType)
                    (vLhsType, vRhsType) match {
                        case (ConcreteType.StringType, ConcreteType.StringType) => s"${vLhs.result} + ${vRhs.result}"
                        case (_: ConcreteType.ArrayOf, _: ConcreteType.ArrayOf) => s"${vLhs.result} ++ ${vRhs.result}"
                        case _ => throw IllegalGrammar(s"Cannot add ${vLhsType} and ${vRhsType}")
                    }
                case valueify.Op.EQ => s"${vLhs.result} == ${vRhs.result}"
                case valueify.Op.NE => s"${vLhs.result} != ${vRhs.result}"
                case valueify.Op.BOOL_AND => s"${vLhs.result} && ${vRhs.result}"
                case valueify.Op.BOOL_OR => s"${vLhs.result} || ${vRhs.result}"
            }
            val v = newVarName()
            ValueifierCode(vLhs.codes ++ vRhs.codes :+ s"val $v = $result", v, vLhs.requirements ++ vRhs.requirements)
        case ElvisOp(expr, ifNull, _) =>
            val vExpr = valueifyExprCode(expr, inputName)
            val vIfNull = valueifyExprCode(ifNull, inputName)
            val v = newVarName()
            val assign = s"val $v = ${vExpr.result}.getOrElse(${vIfNull.result})"
            ValueifierCode(vExpr.codes ++ vIfNull.codes :+ assign, v, vExpr.requirements ++ vIfNull.requirements)
        case TernaryExpr(condition, ifTrue, ifFalse, _) =>
            val vCondition = valueifyExprCode(condition, inputName)
            val vIfTrue = valueifyExprCode(ifTrue, inputName)
            val vIfFalse = valueifyExprCode(ifFalse, inputName)
            val v = newVarName()
            val ternaryStmt = s"val $v = if (${vCondition.result}) ${vIfTrue.result} else ${vIfFalse.result}"
            ValueifierCode(vCondition.codes ++ vIfTrue.codes ++ vIfFalse.codes :+ ternaryStmt, v,
                vCondition.requirements ++ vIfTrue.requirements ++ vIfFalse.requirements)
        case literal: Literal =>
            literal match {
                case NullLiteral => ValueifierCode(List(), "None", Set())
                case BoolLiteral(value) => ValueifierCode(List(), s"$value", Set())
                case CharLiteral(value) => ValueifierCode(List(), escapeChar(value), Set())
                case StringLiteral(value) => ValueifierCode(List(), escapeString(value), Set())
            }
        case value: EnumValue => value match {
            case CanonicalEnumValue(name, value, _) => ???
            case ShortenedEnumValue(value, _) => ???
        }
    }

    def matchFuncFor(nonterminalName: String, valueifyExpr: ValueifyExpr): CodeBlock = {
        val exprCode = valueifyExprCode(valueifyExpr, "input")
        val returnTypeString = typeDescStringOf(realTypeOf(valueifyExpr.resultType))
        CodeBlock(List(s"def ${matchFuncNameForNonterminal(nonterminalName)}(input: Node): $returnTypeString = {") ++
            exprCode.codes ++ List(s"${exprCode.result}", "}"))
    }
}
