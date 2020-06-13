package com.giyeok.jparser.metalang3.codegen

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.{EnumTypeName, Nonterminal, TypeName}
import com.giyeok.jparser.metalang3.ConcreteType.{ArrayOf, StringType}
import com.giyeok.jparser.metalang3.ValueifyGen.IllegalGrammar
import com.giyeok.jparser.metalang3._

class ScalaGen(val analysis: AnalysisResult) {
    private var _argNum = 0

    private def newVarName(): String = {
        _argNum += 1
        "v" + _argNum
    }

    case class ValueifierCode(codes: List[String], result: String, requirements: Set[String])

    private def firstCharUpperCase(name: String) = s"${name.charAt(0).toUpper}${name.substring(1)}"

    def matchFuncNameForNonterminal(nonterminal: Nonterminal): String =
        s"match${firstCharUpperCase(nonterminal.name.name.sourceText)}"

    private def typeNameString(typeName: TypeName): String = typeName.name.sourceText

    private def enumNameString(typeName: EnumTypeName): String = typeName.name.sourceText

    def typeDescStringOf(concreteType: ConcreteType): String = concreteType match {
        case ConcreteType.NodeType => "Node"
        case ConcreteType.ClassType(name) => typeNameString(name)
        case ConcreteType.OptionalOf(typ) => s"Option[${typeDescStringOf(typ)}]"
        case ArrayOf(elemType) => s"List[${typeDescStringOf(elemType)}]"
        case ConcreteType.EnumType(enumName) => enumNameString(enumName)
        case ConcreteType.NullType => "Nothing"
        case ConcreteType.BoolType => "Boolean"
        case ConcreteType.CharType => "Char"
        case ConcreteType.StringType => "String"
        case ConcreteType.UnionOf(types) =>
            throw IllegalGrammar(s"Union type is not supported: Union(${types.map(typeDescStringOf).mkString(", ")})")
    }

    private def escapeString(str: String): String = s""""$str""""

    private def collectCodesFrom(codes: List[ValueifierCode]) = codes.foldLeft(List[String]())(_ ++ _.codes)

    private def collectRequirementsFrom(codes: List[ValueifierCode]) = codes.foldLeft(Set[String]())(_ ++ _.requirements)

    def valueifyExprCode(expr: ValueifyExpr, inputName: String): ValueifierCode = expr match {
        case InputNode => ValueifierCode(List(), inputName, Set())
        case MatchNonterminal(nonterminal, expr) =>
            val e = valueifyExprCode(expr, inputName)
            val v = newVarName()
            val matchFunc = matchFuncNameForNonterminal(nonterminal)
            ValueifierCode(e.codes :+ s"val $v = $matchFunc(${e.result})", v, e.requirements)
        case SeqElemAt(expr, index) =>
            val e = valueifyExprCode(expr, inputName)
            val v = newVarName()
            ValueifierCode(e.codes :+ s"val $v = ${e.result}.asInstanceOf[SequenceNode].children($index)", v, e.requirements)
        case Unbind(symbol, expr) =>
            val e = valueifyExprCode(expr, inputName)
            val v1 = newVarName()
            val v2 = newVarName()
            val bindedSymbolId = analysis.symbolOf(symbol)
            ValueifierCode(e.codes ++ List(
                s"val BindNode($v1, $v2) = ${e.result}",
                s"assert($v1.id == $bindedSymbolId)"
            ), v2, e.requirements)
        case JoinBodyOf(expr) => ???
        case JoinCondOf(expr) => ???
        case ExceptBodyOf(expr) => ???
        case ExceptCondOf(expr) => ???
        case UnrollRepeatFromZero(expr) => ???
        case UnrollRepeatFromOne(expr) => ???
        case UnrollChoices(mappings) =>
            val symbol = newVarName()
            val body = newVarName()
            val v = newVarName()
            var codes = List(
                s"val BindNode($symbol, $body) = $inputName",
                s"val $v = $symbol.id match {"
            )
            var requirements = Set[String]()
            mappings.foreach { mapping =>
                val symbol = mapping._1 match {
                    case InPlaceSequenceChoice(inPlaceSequence) => analysis.symbolOf(inPlaceSequence)
                    case SymbolChoice(symbol) => analysis.symbolOf(symbol)
                    case RightHandSideChoice(rhs) => analysis.symbolOf(rhs)
                    case EmptySeqChoice => analysis.emptySymbol()
                }
                val vMapper = valueifyExprCode(mapping._2, body)
                codes :+= s"case ${symbol.id} => // ${symbol.symbol.toShortString}"
                codes ++= vMapper.codes
                codes :+= vMapper.result
                requirements ++= vMapper.requirements
            }
            codes :+= "}"
            ValueifierCode(codes, v, requirements)
        case NamedConstructCall(className, params) =>
            val vParams = params.map { param => valueifyExprCode(param._2._1, inputName) }
            val createExpr = s"${className.astNode.sourceText}(${vParams.map(_.result).mkString(", ")})"
            ValueifierCode(collectCodesFrom(vParams), createExpr, collectRequirementsFrom(vParams))
        case UnnamedConstructCall(className, params) =>
            val vParams = params.map { param => valueifyExprCode(param._1, inputName) }
            val createExpr = s"${className.astNode.sourceText}(${vParams.map(_.result).mkString(", ")})"
            ValueifierCode(collectCodesFrom(vParams), createExpr, collectRequirementsFrom(vParams))
        case FuncCall(funcName, params) =>
            val scalaFuncName = funcName.name.sourceText match {
                case "isempty" => "ASTUtils.isEmpty"
                case "ispresent" => "ASTUtils.isPresent"
                case "chr" => "ASTUtils.toChar"
                case "str" => "ASTUtils.toString"
            }
            val vParams = params.map { param => valueifyExprCode(param._1, inputName) }
            val args = vParams.map(_.result)
            val callExpr = s"$scalaFuncName(${args.mkString(", ")})"
            ValueifierCode(collectCodesFrom(vParams), callExpr, collectRequirementsFrom(vParams) + scalaFuncName)
        case ArrayExpr(elems) =>
            val vElems = elems.map(valueifyExprCode(_, inputName))
            ValueifierCode(collectCodesFrom(vElems), s"List(${vElems.map(_.result).mkString(", ")})", collectRequirementsFrom(vElems))
        case PrefixOp(prefixOpType, expr, exprType) =>
            val vExpr = valueifyExprCode(expr, inputName)
            val result = prefixOpType match {
                case com.giyeok.jparser.metalang3.PreOp.NOT => s"! ${vExpr.result}"
            }
            ValueifierCode(vExpr.codes, result, vExpr.requirements)
        case BinOp(op, lhs, rhs, lhsType, rhsType) =>
            val vLhs = valueifyExprCode(lhs, inputName)
            val vRhs = valueifyExprCode(rhs, inputName)
            val result = op match {
                case com.giyeok.jparser.metalang3.Op.ADD =>
                    val vLhsType = analysis.mostSpecificSuperTypeOf(analysis.concreteTypeOf(lhsType))
                    val vRhsType = analysis.mostSpecificSuperTypeOf(analysis.concreteTypeOf(rhsType))
                    (vLhsType, vRhsType) match {
                        case (StringType, StringType) => s"${vLhs.result} + ${vRhs.result}"
                        case (_: ArrayOf, _: ArrayOf) => s"${vLhs.result} ++ ${vRhs.result}"
                        case _ => throw IllegalGrammar(s"Cannot add ${vLhsType} and ${vRhsType}")
                    }
                case com.giyeok.jparser.metalang3.Op.EQ => s"${vLhs.result} == ${vRhs.result}"
                case com.giyeok.jparser.metalang3.Op.NE => s"${vLhs.result} != ${vRhs.result}"
                case com.giyeok.jparser.metalang3.Op.BOOL_AND => s"${vLhs.result} && ${vRhs.result}"
                case com.giyeok.jparser.metalang3.Op.BOOL_OR => s"${vLhs.result} || ${vRhs.result}"
            }
            ValueifierCode(vLhs.codes ++ vRhs.codes, result, vLhs.requirements ++ vRhs.requirements)
        case ElvisOp(expr, ifNull) =>
            val vExpr = valueifyExprCode(expr, inputName)
            val vIfNull = valueifyExprCode(ifNull, inputName)
            val v = newVarName()
            val assign = s"val $v = ${vExpr.result}.getOrElse(${vIfNull.result})"
            ValueifierCode(vExpr.codes ++ vIfNull.codes :+ assign, v, vExpr.requirements ++ vIfNull.requirements)
        case TernaryExpr(condition, ifTrue, ifFalse, conditionType) => ???
        case literal: Literal =>
            literal match {
                case NullLiteral => ValueifierCode(List(), "None", Set())
                case BoolLiteral(value) => ValueifierCode(List(), s"$value", Set())
                case CharLiteral(value) => ???
                case StringLiteral(value) => ValueifierCode(List(), escapeString(value), Set())
            }
        case value: EnumValue => ???
    }
}
