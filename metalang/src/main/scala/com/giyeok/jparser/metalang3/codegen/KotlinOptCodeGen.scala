package com.giyeok.jparser.metalang3.codegen

import com.giyeok.jparser.NGrammar.{NNonterminal, NRepeat, NStart}
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.metalang3.MetaLanguage3.{ProcessedGrammar, check}
import com.giyeok.jparser.metalang3.{ClassHierarchyItem, ClassRelationCollector, Type, ValuefyExpr}
import com.giyeok.jparser.utils.JavaCodeGenUtil.javaChar

// Kernel set list -> AST 변환하는 코드 생성 코드
// ScalaCodeGen은 parse tree를 받아서 AST로 변환
// Scala vs Kotlin 외에도 이런 차이도 있음
class KotlinOptCodeGen(val analysis: ProcessedGrammar) {
  private var varId: Int = 0

  def newVar(): String = {
    varId += 1
    s"var$varId"
  }

  def typeDescStringOf(typ: Type, context: Option[String] = None): CodeBlob = typ match {
    case Type.NodeType => CodeBlob("Node", Set("com.giyeok.jparser.ParseResultTree.Node"))
    case Type.ClassType(name) => CodeBlob(name, Set())
    case Type.OptionalOf(typ) =>
      val elemType = typeDescStringOf(typ, context)
      CodeBlob(s"${elemType.code}?", elemType.required)
    case Type.ArrayOf(typ) =>
      val elemType = typeDescStringOf(typ, context)
      CodeBlob(s"List<${elemType.code}>", elemType.required)
    case unionType: Type.UnionOf =>
      analysis.reduceUnionType(unionType) match {
        case reducedType: Type.UnionOf =>
          val unionTypeNames = reducedType.types.map(Type.readableNameOf)
          val contextText = context.map(ctx => s" around $ctx").getOrElse("")
          throw new Exception(s"Union type of {${unionTypeNames.mkString(", ")}} is not supported$contextText")
        case reducedType => typeDescStringOf(reducedType)
      }
    case Type.EnumType(enumName) => CodeBlob(s"$enumName", Set())
    case Type.UnspecifiedEnumType(uniqueId) => CodeBlob(s"${analysis.shortenedEnumTypesMap(uniqueId)}", Set())
    case Type.NullType => CodeBlob("Unit?", Set())
    case Type.AnyType => CodeBlob("Any", Set())
    case Type.BoolType => CodeBlob("Boolean", Set())
    case Type.CharType => CodeBlob("Char", Set())
    case Type.StringType => CodeBlob("String", Set())
    case Type.NothingType => CodeBlob("Nothing", Set())
  }

  def classDefs(classRelations: ClassRelationCollector): CodeBlob = {
    val classHierarchy = classRelations.toHierarchy
    classHierarchy.allTypes.values.map(classDef(_)).fold(CodeBlob("", Set()))(_ + _)
  }

  def classDef(cls: ClassHierarchyItem): CodeBlob = {
    val supers = (cls.superclasses.toList.sorted :+ "AstNode").mkString(", ")
    val params = analysis.classParamTypes.getOrElse(cls.className, List()).map { param =>
      val paramType = typeDescStringOf(param._2, context = Some(s"parameter type of ${cls.className}.${param._1}"))
      CodeBlob(s"  val ${param._1}: ${paramType.code},\n", paramType.required)
    }
    if (cls.subclasses.isEmpty) {
      // Concrete type
      CodeBlob(
        s"""data class ${cls.className}(
           |${params.map(_.code).mkString("\n")},
           |override val symbolId: Int,
           |override val start: Int,
           |override val end: Int,
           |): $supers""".stripMargin,
        params.flatMap(_.required).toSet)
    } else {
      // Abstract type
      CodeBlob(s"sealed interface ${cls.className}: $supers", Set())
    }
  }

  def enumDefs(enumValuesMap: Map[String, Set[String]]): CodeBlob = {
    if (enumValuesMap.isEmpty) CodeBlob("", Set()) else {
      enumValuesMap.keySet.toList.sorted.map { enumName =>
        val members = enumValuesMap(enumName).toList.sorted
        CodeBlob(s"enum class $enumName { ${members.mkString(", ")} }", Set())
      }.reduce(_ + _)
    }
  }

  private def nonterminalMatchFuncName(nonterminal: String) =
    s"match${nonterminal.substring(0, 1).toUpperCase}${nonterminal.substring(1)}"

  def matchStartFunc(): CodeBlob = {
    val startSymbol = analysis.ngrammar.symbolOf(analysis.ngrammar.nsymbols(analysis.ngrammar.startSymbol).asInstanceOf[NStart].produce).asInstanceOf[NNonterminal]
    val returnType = typeDescStringOf(analysis.nonterminalTypes(analysis.startNonterminalName), context = Some(s"return type of ${analysis.startNonterminalName}"))
    CodeBlob(
      s"""fun matchStart(): ${returnType.code} {
         |  val lastGen = inputs.size
         |  val kernel = history[lastGen]
         |    .filter { it.symbolId() == $startSymbol && it.endGen() == lastGen }
         |    .checkSingle()
         |  return ${nonterminalMatchFuncName(startSymbol.symbol.name)}(kernel)
         |}""".stripMargin,
      returnType.required)
  }

  // returns: (Generated CodeBlob, Set of nonterminals required)
  def nonterminalMatchFunc(nonterminal: String): (CodeBlob, Set[String]) = {
    val valuefyExpr = analysis.nonterminalValuefyExprs(nonterminal)
    val body = unrollChoicesExpr(valuefyExpr.choices, "beginGen", "endGen")
    val returnType = typeDescStringOf(analysis.nonterminalTypes(nonterminal), Some(s"return type of nonterminal $nonterminal"))
    val codeBlob = CodeBlob(
      s"""fun ${nonterminalMatchFuncName(nonterminal)}(beginGen: Int, endGen: Int): ${returnType.code} {
         |${(body.prepares :+ s"return ${body.result}").mkString("\n")}
         |}""".stripMargin,
      body.required ++ returnType.required ++ body.required)
    (codeBlob, Set())
  }

  def unrollChoicesExpr(choicesMap: Map[Symbols.Symbol, ValuefyExpr], beginGen: String, endGen: String): ExprBlob = {
    // choices가 하나인 경우 체크 없이 바로 다음 단계로
    if (choicesMap.size == 1) {
      val (choiceSymbol, choiceExpr) = choicesMap.head
      valuefyExprToCode(choiceExpr, beginGen, endGen, choiceSymbol)
    } else {
      val choiceSymbols = choicesMap.keys.toList.sortBy(analysis.ngrammar.idOf)
      val choiceVars = choiceSymbols.map(_ => newVar())
      val choices = choiceVars.zip(choiceSymbols)

      val tryCodes = choices.map { pair =>
        val (varName, choiceSymbol) = pair
        val symbolId = analysis.ngrammar.idOf(choiceSymbol)
        val lastPointer = analysis.ngrammar.lastPointerOf(symbolId)
        s"val $varName = history[$endGen].findByBeginGenOpt($symbolId, $lastPointer, $beginGen)"
      }

      val assertCode = s"check(hasSingleTrue(${choiceVars.map(_ + " != null").mkString(", ")}))"

      val v = newVar()
      var requires = Set[String]()
      val choiceCodes = choices.zipWithIndex.flatMap { pair =>
        val ((varName, choiceSymbol), index) = pair
        val choiceExpr = choicesMap(choiceSymbol)
        val exprCode = valuefyExprToCode(choiceExpr, beginGen, endGen, choiceSymbol)
        val isLast = choices.size - 1 == index
        val condition = if (isLast) "else" else s"$varName != null"
        if (exprCode.prepares.isEmpty) {
          requires ++= exprCode.required
          List(s"$condition -> ${exprCode.result}")
        } else {
          s"$condition -> {" +: exprCode.prepares :+ exprCode.result :+ "}"
        }
      }

      ExprBlob(tryCodes ++ List(assertCode, s"val $v = when {") ++ choiceCodes ++ List("}"), v, requires)
    }
  }

  private def typeOf(expr: ValuefyExpr): Type = analysis.typeInferer.typeOfValuefyExpr(expr).get

  def valuefyExprToCode(valuefyExpr: ValuefyExpr, beginGen: String, endGen: String, symbol: Symbols.Symbol): ExprBlob = valuefyExpr match {
    case ValuefyExpr.InputNode =>
      // 부분적으로 parse tree reconstruct해서 반환
      ???
    case ValuefyExpr.MatchNonterminal(nonterminalName) =>
      val v = newVar()
      ExprBlob(List(s"val $v = ${nonterminalMatchFuncName(nonterminalName)}($beginGen, $endGen)"), v, Set())
    case ValuefyExpr.Unbind(symbol, expr) =>
      // 불필요한 체크는 스킵 - 다 스킵해버려도 되나?
      //      val v = newVar()
      //      val symbolId = analysis.ngrammar.idOf(symbol)
      //      val lastPointer = analysis.ngrammar.lastPointerOf(symbolId)
      //      val afterCheck = valuefyExprToCode(expr, beginGen, endGen, symbol)
      //      ExprBlob(List(s"val $v = history[$endGen]",
      //        s"  .filter { it.symbolId() == $symbolId && it.pointer == $lastPointer && it.beginGen == $beginGen }",
      //        "  .checkSingle()"
      //      ) ++ afterCheck.prepares, afterCheck.result, afterCheck.required)
      valuefyExprToCode(expr, beginGen, endGen, symbol)
    case ValuefyExpr.JoinBody(bodyProcessor) => ???
    case ValuefyExpr.JoinCond(condProcessor) => ???
    case ValuefyExpr.SeqElemAt(index, expr) =>
      val sequenceId = analysis.ngrammar.idOf(symbol)
      val sequence = analysis.ngrammar.nsequences(sequenceId)
      val seqVar = newVar()
      val getSequenceElems = s"val $seqVar = getSequenceElems(history, $sequenceId, listOf(${sequence.sequence.mkString(",")}), $beginGen, $endGen)"
      val elemValuefy = valuefyExprToCode(expr, s"$seqVar[$index].first", s"$seqVar[$index].second", sequence.symbol.seq(index))
      ExprBlob(getSequenceElems +: elemValuefy.prepares, elemValuefy.result, elemValuefy.required)
    case ValuefyExpr.UnrollRepeatFromZero(elemProcessor) =>
      val v = newVar()
      val symbolId = analysis.ngrammar.idOf(symbol)
      val repeat = analysis.ngrammar.symbolOf(symbolId).asInstanceOf[NRepeat]
      val itemSymId = analysis.ngrammar.idOf(repeat.symbol.sym)
      val elemProcessorCode = valuefyExprToCode(elemProcessor, "k.first", "k.second", repeat.symbol.sym)
      ExprBlob(
        List(s"val $v = unrollRepeat0(history, $symbolId, $itemSymId, ${repeat.baseSeq}, ${repeat.repeatSeq}, $beginGen, $endGen).map { k ->") ++
          elemProcessorCode.prepares ++
          List(elemProcessorCode.result, "}"),
        v,
        elemProcessorCode.required)
    case ValuefyExpr.UnrollRepeatFromOne(elemProcessor) =>
      val v = newVar()
      val symbolId = analysis.ngrammar.idOf(symbol)
      val repeat = analysis.ngrammar.symbolOf(symbolId).asInstanceOf[NRepeat]
      val itemSymId = analysis.ngrammar.idOf(repeat.symbol.sym)
      val elemProcessorCode = valuefyExprToCode(elemProcessor, "k.first", "k.second", repeat.symbol.sym)
      ExprBlob(
        List(s"val $v = unrollRepeat1(history, $symbolId, $itemSymId, ${repeat.baseSeq}, ${repeat.repeatSeq}, $beginGen, $endGen).map { k ->") ++
          elemProcessorCode.prepares ++
          List(elemProcessorCode.result, "}"),
        v,
        elemProcessorCode.required)
    case ValuefyExpr.UnrollChoices(choices) =>
      unrollChoicesExpr(choices, beginGen, endGen)
    case ValuefyExpr.ConstructCall(className, params) =>
      val paramCodes = params.zipWithIndex.map { case (param, index) =>
        valuefyExprToCode(param, beginGen, endGen, symbol)
      }
      val v = newVar()
      val construct = s"val $v = $className(${paramCodes.map(_.result).mkString(", ")}, nextId(), $beginGen, $endGen)"
      ExprBlob(paramCodes.flatMap(_.prepares) :+ construct, v, paramCodes.flatMap(_.required).toSet)
    case ValuefyExpr.FuncCall(funcType, params) =>
      ExprBlob(List(), s"TODO_funccall($funcType)", Set())
    case ValuefyExpr.ArrayExpr(elems) =>
      val elemType = typeOf(valuefyExpr).asInstanceOf[Type.ArrayOf].elemType
      val elemCodes = elems.map(valuefyExprToCode(_, beginGen, endGen, symbol))
      ExprBlob(elemCodes.flatMap(_.prepares),
        s"listOf(${elemCodes.map(_.result).mkString(", ")})",
        elemCodes.flatMap(_.required).toSet)
    case ValuefyExpr.BinOp(op, lhs, rhs) =>
      val lhsCode = valuefyExprToCode(lhs, beginGen, endGen, symbol)
      val rhsCode = valuefyExprToCode(rhs, beginGen, endGen, symbol)
      val opExpr = op match {
        case com.giyeok.jparser.metalang3.ValuefyExpr.BinOpType.ADD =>
          (typeOf(lhs), typeOf(rhs)) match {
            case (Type.StringType, Type.StringType) => s"${lhsCode.result} + ${rhsCode.result}"
            case (Type.ArrayOf(_), Type.ArrayOf(_)) => s"${lhsCode.result} + ${rhsCode.result}"
          }
        case com.giyeok.jparser.metalang3.ValuefyExpr.BinOpType.EQ =>
          check(typeOf(lhs) == typeOf(rhs), "lhs and rhs of == must be same type")
          s"${lhsCode.result} == ${rhsCode.result}"
        case com.giyeok.jparser.metalang3.ValuefyExpr.BinOpType.NE =>
          check(typeOf(lhs) == typeOf(rhs), "lhs and rhs of == must be same type")
          s"${lhsCode.result} != ${rhsCode.result}"
        case com.giyeok.jparser.metalang3.ValuefyExpr.BinOpType.BOOL_AND =>
          (typeOf(lhs), typeOf(rhs)) match {
            case (Type.BoolType, Type.BoolType) => s"${lhsCode.result} && ${rhsCode.result}"
          }
        case com.giyeok.jparser.metalang3.ValuefyExpr.BinOpType.BOOL_OR =>
          (typeOf(lhs), typeOf(rhs)) match {
            case (Type.BoolType, Type.BoolType) => s"${lhsCode.result} || ${rhsCode.result}"
          }
      }
      ExprBlob(lhsCode.prepares ++ rhsCode.prepares, opExpr, lhsCode.required ++ rhsCode.required)
    case ValuefyExpr.PreOp(op, expr) => ???
    case ValuefyExpr.ElvisOp(expr, ifNull) => ???
    case ValuefyExpr.TernaryOp(condition, ifTrue, ifFalse) => ???
    case literal: ValuefyExpr.Literal =>
      literal match {
        case ValuefyExpr.NullLiteral => ExprBlob.code("null")
        case ValuefyExpr.BoolLiteral(value) => ExprBlob.code(s"$value")
        case ValuefyExpr.CharLiteral(value) => ExprBlob.code(s"'${javaChar(value)}'") // TODO escape
        case ValuefyExpr.CharFromTerminalLiteral =>
          ExprBlob(List(), "TODO(char)", Set())
        case ValuefyExpr.StringLiteral(value) => ExprBlob.code("\"" + value + "\"") // TODO escape
      }
    case enumValue: ValuefyExpr.EnumValue =>
      enumValue match {
        case ValuefyExpr.CanonicalEnumValue(enumName, enumValue) => ExprBlob.code(s"$enumName.$enumValue")
        case ValuefyExpr.ShortenedEnumValue(unspecifiedEnumTypeId, enumValue) =>
          val enumName = analysis.shortenedEnumTypesMap(unspecifiedEnumTypeId)
          ExprBlob.code(s"$enumName.$enumValue")
      }
    case _ => ???
  }
}
