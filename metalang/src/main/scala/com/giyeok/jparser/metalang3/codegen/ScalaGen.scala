package com.giyeok.jparser.metalang3.codegen

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.{EnumTypeName, Nonterminal, TypeName}
import com.giyeok.jparser.metalang3.MetaLanguage3.IllegalGrammar
import com.giyeok.jparser.metalang3.types.ConcreteType
import com.giyeok.jparser.metalang3.valueify._
import com.giyeok.jparser.metalang3.{valueify, _}

// TODO codegen options
// - null vs Option
// - mostSpecificSuperTypeOf 옵션(extensive하지 않은 super type을 허용할지)
class ScalaGen(val analysis: AnalysisResult) {
    private var _argNum = 0

    private def newVarName(): String = {
        _argNum += 1
        "v" + _argNum
    }

    case class ValueifierCode(codes: List[String], result: String, requirements: Set[String])

    case class CodeBlock(codes: List[String])

    private def firstCharUpperCase(name: String) = s"${name.charAt(0).toUpper}${name.substring(1)}"

    def matchFuncNameForNonterminal(nonterminal: Nonterminal): String =
        s"match${firstCharUpperCase(nonterminal.name.name.sourceText)}"

    private def typeNameString(typeName: TypeName): String = typeName.name.sourceText

    private def enumNameString(typeName: EnumTypeName): String = typeName.name.sourceText

    def typeDescStringOf(concreteType: ConcreteType): String = concreteType match {
        case ConcreteType.NodeType => "Node"
        case ConcreteType.ClassType(name) => typeNameString(name)
        case ConcreteType.OptionalOf(typ) => s"Option[${typeDescStringOf(typ)}]"
        case ConcreteType.ArrayOf(elemType) => s"List[${typeDescStringOf(elemType)}]"
        case ConcreteType.EnumType(enumName) => enumNameString(enumName)
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

    // TODO 값이 가는 곳이 OptionalType인데 주는 값이 아니면, Some(**)로 넣어주도록
    def valueifyExprCode(expr: ValueifyExpr, inputName: String): ValueifierCode = expr match {
        case InputNode => ValueifierCode(List(), inputName, Set())
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
            ValueifierCode(e.codes ++ List(
                s"val BindNode($v1, $v2) = ${e.result}",
                s"assert($v1.id == ${bindedSymbol.id})"
            ), v2, e.requirements + "jparser.BindNode")
        case JoinBodyOf(joinSymbol, joinExpr, bodyProcessorExpr, _) =>
            val join = valueifyExprCode(joinExpr, inputName)
            val v1 = newVarName()
            val v2 = newVarName()
            val processor = valueifyExprCode(bodyProcessorExpr, v2)
            val symbol = analysis.symbolOf(joinSymbol)
            ValueifierCode(join.codes ++ List(
                s"val JoinNode($v1, $v2, _) = ${join.result}",
                s"assert($v1.id == ${symbol.id})"
            ) ++ processor.codes, processor.result, join.requirements ++ processor.requirements + "jparser.JoinNode")
        case JoinCondOf(joinSymbol, joinExpr, condProcessorExpr, _) =>
            ???
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
        case UnrollChoices(choiceExpr, mappings, _) =>
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
        case NamedConstructCall(className, params, _) =>
            val vParams = params.map { param => valueifyExprCode(param._2, inputName) }
            val createExpr = s"${className.astNode.sourceText}(${vParams.map(_.result).mkString(", ")})"
            ValueifierCode(collectCodesFrom(vParams), createExpr, collectRequirementsFrom(vParams))
        case UnnamedConstructCall(className, params, _) =>
            val vParams = params.map { param => valueifyExprCode(param, inputName) }
            val createExpr = s"${className.astNode.sourceText}(${vParams.map(_.result).mkString(", ")})"
            ValueifierCode(collectCodesFrom(vParams), createExpr, collectRequirementsFrom(vParams))
        case FuncCall(funcName, params, _) =>
            val scalaFuncName = funcName.name.sourceText match {
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
                    val vLhsType = analysis.mostSpecificSuperTypeOf(analysis.concreteTypeOf(lhs.resultType))
                    val vRhsType = analysis.mostSpecificSuperTypeOf(analysis.concreteTypeOf(rhs.resultType))
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

    def matchFuncFor(nonterminal: Nonterminal, valueifyExpr: ValueifyExpr): CodeBlock = {
        val exprCode = valueifyExprCode(valueifyExpr, "input")
        val returnTypeString = typeDescStringOf(analysis.mostSpecificSuperTypeOf(analysis.concreteTypeOf(valueifyExpr.resultType)))
        CodeBlock(List(s"def ${matchFuncNameForNonterminal(nonterminal)}(input: Node): $returnTypeString = {") ++
            exprCode.codes ++ List(s"${exprCode.result}", "}"))
    }
}
