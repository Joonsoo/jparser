package com.giyeok.jparser.metalang3.codegen

import com.giyeok.jparser.NGrammar.{NNonterminal, NRepeat, NStart}
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.metalang3.MetaLanguage3.{ProcessedGrammar, check}
import com.giyeok.jparser.metalang3.{ClassHierarchyItem, ClassRelationCollector, Type, ValuefyExpr}
import com.giyeok.jparser.utils.JavaCodeGenUtil.{isPrintableChar, javaChar}

import java.io.StringWriter
import scala.annotation.tailrec

// Kernel set list -> AST 변환하는 코드 생성 코드
// ScalaCodeGen은 parse tree를 받아서 AST로 변환
// Scala vs Kotlin 외에도 이런 차이도 있음
class KotlinOptCodeGen(val analysis: ProcessedGrammar) {
  private var varId: Int = 0

  def newVar(): String = {
    varId += 1
    s"var$varId"
  }

  var _requiredNonterms: Set[String] = Set()

  var _symbolsOfInterest: Set[Int] = Set()

  def symbolsOfInterest: Set[Int] = _symbolsOfInterest

  def escapeChar(c: Char): String = c match {
    case '\b' => "\\b"
    case '\n' => "\\n"
    case '\r' => "\\r"
    case '\t' => "\\t"
    case '\\' => "\\\\"
    case '\'' => "\\'"
    case '$' => "\\$"
    case c if !isPrintableChar(c) && c.toInt < 65536 =>
      val c1 = (c.toInt >> 8) % 256
      val c2 = c.toInt % 256
      val hexChars = "0123456789abcdef"
      s"\\u${hexChars(c1 >> 4)}${hexChars(c1 & 15)}${hexChars(c2 >> 4)}${hexChars(c2 & 15)}"
    case c => c.toString
  }

  def escapeString(s: String): String = s.flatMap(escapeChar)

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
      CodeBlob(s"  val ${param._1}: ${paramType.code}", paramType.required)
    }
    if (cls.subclasses.isEmpty) {
      // Concrete type
      CodeBlob(
        s"""data class ${cls.className}(
           |${params.map(_.code + ",").mkString("\n")}
           |  override val nodeId: Int,
           |  override val start: Int,
           |  override val end: Int,
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
    _requiredNonterms += startSymbol.symbol.name
    CodeBlob(
      s"""fun matchStart(): ${returnType.code} {
         |  val lastGen = inputs.size
         |  val kernel = history[lastGen]
         |    .filter { it.symbolId() == ${startSymbol.id} && it.pointer() == 1 && it.endGen() == lastGen }
         |    .checkSingle()
         |  return ${nonterminalMatchFuncName(startSymbol.symbol.name)}(kernel.beginGen(), kernel.endGen())
         |}""".stripMargin,
      returnType.required)
  }

  // returns: (Generated CodeBlob, Set of nonterminals required)
  def nonterminalMatchFunc(nonterminal: String): CodeBlob = {
    val valuefyExpr = analysis.nonterminalValuefyExprs(nonterminal)
    val body = unrollChoicesExpr(valuefyExpr.choices, "beginGen", "endGen")
    val returnType = typeDescStringOf(analysis.nonterminalTypes(nonterminal), Some(s"return type of nonterminal $nonterminal"))
    val codeBlob = CodeBlob(
      s"""fun ${nonterminalMatchFuncName(nonterminal)}(beginGen: Int, endGen: Int): ${returnType.code} {
         |${(body.prepares :+ s"return ${body.result}").mkString("\n")}
         |}""".stripMargin,
      body.required ++ returnType.required ++ body.required)
    codeBlob
  }

  def unrollChoicesExpr(choicesMap: Map[Symbols.Symbol, ValuefyExpr], beginGen: String, endGen: String): ExprBlob = {
    // choices가 하나인 경우 체크 없이 바로 다음 단계로
    if (choicesMap.size == 1) {
      val (choiceSymbol, choiceExpr) = choicesMap.head
      valuefyExprToCode(choiceExpr, beginGen, endGen, choiceSymbol, SequenceVarName(None))
    } else {
      val choiceSymbols = choicesMap.keys.toList.sortBy(analysis.ngrammar.idOf)
      val choiceVars = choiceSymbols.map(_ => newVar())
      val choices = choiceVars.zip(choiceSymbols)

      val tryCodes = choices.map { pair =>
        val (varName, choiceSymbol) = pair
        val symbolId = analysis.ngrammar.idOf(choiceSymbol)
        val lastPointer = analysis.ngrammar.lastPointerOf(symbolId)
        _symbolsOfInterest += symbolId
        s"val $varName = history[$endGen].findByBeginGenOpt($symbolId, $lastPointer, $beginGen)"
      }

      val assertCode = s"check(hasSingleTrue(${choiceVars.map(_ + " != null").mkString(", ")}))"

      val v = newVar()
      var requires = Set[String]()
      val choiceCodes = choices.zipWithIndex.flatMap { pair =>
        val ((varName, choiceSymbol), index) = pair
        val choiceExpr = choicesMap(choiceSymbol)
        val exprCode = valuefyExprToCode(choiceExpr, beginGen, endGen, choiceSymbol, SequenceVarName(None))
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

  case class SequenceVarName(var name: Option[String])

  // symbol은 현재 처리중인 심볼
  // sequenceVar는 현재 처리중인 심볼이 sequence인 경우 혹시 앞에서 getSequenceElems를 한 결과를 가진게 있으면 재사용할 수 있게 그 이름을 전달
  def valuefyExprToCode(valuefyExpr: ValuefyExpr, beginGen: String, endGen: String, symbol: Symbols.Symbol, sequenceVarName: SequenceVarName): ExprBlob = valuefyExpr match {
    case ValuefyExpr.InputNode =>
      // 부분적으로 parse tree reconstruct해서 반환
      ???
    case ValuefyExpr.MatchNonterminal(nonterminalName) =>
      val v = newVar()
      _requiredNonterms += nonterminalName
      ExprBlob(List(s"val $v = ${nonterminalMatchFuncName(nonterminalName)}($beginGen, $endGen)"), v, Set())
    case ValuefyExpr.Unbind(symbol, expr) =>
      // Unbind 체크는 파싱이 잘 됐으면 불필요하므로 여기서는 스킵
      valuefyExprToCode(expr, beginGen, endGen, symbol, sequenceVarName)
    case ValuefyExpr.JoinBody(bodyProcessor) =>
      val joinSymbol = symbol.asInstanceOf[Symbols.Join]
      valuefyExprToCode(bodyProcessor, beginGen, endGen, joinSymbol.sym, SequenceVarName(None))
    case ValuefyExpr.JoinCond(condProcessor) =>
      val joinSymbol = symbol.asInstanceOf[Symbols.Join]
      valuefyExprToCode(condProcessor, beginGen, endGen, joinSymbol.join, SequenceVarName(None))
    case ValuefyExpr.SeqElemAt(index, expr) =>
      val sequenceId = analysis.ngrammar.idOf(symbol)
      val sequence = analysis.ngrammar.nsequences(sequenceId)
      sequenceVarName.name match {
        case Some(seqVar) =>
          _symbolsOfInterest += sequence.sequence(index)
          valuefyExprToCode(expr, s"$seqVar[$index].first", s"$seqVar[$index].second", sequence.symbol.seq(index), sequenceVarName)
        case None =>
          val seqVar = newVar()
          _symbolsOfInterest += sequenceId
          _symbolsOfInterest ++= sequence.sequence
          val getSequenceElems = s"val $seqVar = getSequenceElems(history, $sequenceId, listOf(${sequence.sequence.mkString(",")}), $beginGen, $endGen)"
          sequenceVarName.name = Some(seqVar)
          val elemValuefy = valuefyExprToCode(expr, s"$seqVar[$index].first", s"$seqVar[$index].second", sequence.symbol.seq(index), sequenceVarName)
          ExprBlob(getSequenceElems +: elemValuefy.prepares, elemValuefy.result, elemValuefy.required)
      }
    case ValuefyExpr.UnrollRepeatFromZero(elemProcessor) =>
      val v = newVar()
      val symbolId = analysis.ngrammar.idOf(symbol)
      val repeat = analysis.ngrammar.symbolOf(symbolId).asInstanceOf[NRepeat]
      val itemSymId = analysis.ngrammar.idOf(repeat.symbol.sym)
      val elemProcessorCode = valuefyExprToCode(elemProcessor, "k.first", "k.second", repeat.symbol.sym, SequenceVarName(None))
      _symbolsOfInterest ++= Set(symbolId, itemSymId, repeat.baseSeq, repeat.repeatSeq)
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
      val elemProcessorCode = valuefyExprToCode(elemProcessor, "k.first", "k.second", repeat.symbol.sym, SequenceVarName(None))
      _symbolsOfInterest ++= Set(symbolId, itemSymId, repeat.baseSeq, repeat.repeatSeq)
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
        valuefyExprToCode(param, beginGen, endGen, symbol, sequenceVarName)
      }
      val v = newVar()
      val construct = s"val $v = $className(${paramCodes.map(_.result + ", ").mkString("")}nextId(), $beginGen, $endGen)"
      ExprBlob(paramCodes.flatMap(_.prepares) :+ construct, v, paramCodes.flatMap(_.required).toSet)
    case ValuefyExpr.FuncCall(funcType, params) =>
      funcCallToCode(funcType, params, beginGen, endGen, symbol, sequenceVarName)
    case ValuefyExpr.ArrayExpr(elems) =>
      val elemType = typeOf(valuefyExpr).asInstanceOf[Type.ArrayOf].elemType
      val elemCodes = elems.map(valuefyExprToCode(_, beginGen, endGen, symbol, sequenceVarName))
      ExprBlob(elemCodes.flatMap(_.prepares),
        s"listOf(${elemCodes.map(_.result).mkString(", ")})",
        elemCodes.flatMap(_.required).toSet)
    case ValuefyExpr.BinOp(op, lhs, rhs) =>
      val lhsCode = valuefyExprToCode(lhs, beginGen, endGen, symbol, sequenceVarName)
      val rhsCode = valuefyExprToCode(rhs, beginGen, endGen, symbol, sequenceVarName)
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
    case ValuefyExpr.PreOp(op, expr) =>
      op match {
        case com.giyeok.jparser.metalang3.ValuefyExpr.PreOpType.NOT =>
          check(typeOf(expr) == Type.BoolType, "not can be applied only to boolean expression")
          val exprCode = valuefyExprToCode(expr, beginGen, endGen, symbol, sequenceVarName)
          val resultVar = newVar()
          ExprBlob(exprCode.prepares :+ s"val $resultVar = !${exprCode.result}", resultVar, exprCode.required)
      }
    case ValuefyExpr.ElvisOp(expr, ifNull) =>
      val exprVar = newVar()
      val exprCode = valuefyExprToCode(expr, beginGen, endGen, symbol, sequenceVarName)
      val ifNullCode = valuefyExprToCode(ifNull, beginGen, endGen, symbol, sequenceVarName)
      ExprBlob(
        exprCode.prepares :+ s"val $exprVar = ${exprCode.result}",
        s"($exprVar ?: ${ifNullCode.withBraceIfNeeded})",
        exprCode.required ++ ifNullCode.required)
    case ValuefyExpr.TernaryOp(condition, ifTrue, ifFalse) =>
      val conditionCode = valuefyExprToCode(condition, beginGen, endGen, symbol, sequenceVarName)
      val resultType = typeOf(valuefyExpr)
      val thenCode = valuefyExprToCode(ifTrue, beginGen, endGen, symbol, sequenceVarName)
      val elseCode = valuefyExprToCode(ifFalse, beginGen, endGen, symbol, sequenceVarName)
      val resultVar = newVar()
      ExprBlob(conditionCode.prepares ++
        List(s"val $resultVar = if (${conditionCode.result}) {") ++
        thenCode.prepares ++
        List(thenCode.result, "} else {") ++
        elseCode.prepares ++ List(elseCode.result, "}"),
        resultVar,
        conditionCode.required ++ thenCode.required ++ elseCode.required)
    case literal: ValuefyExpr.Literal =>
      literal match {
        case ValuefyExpr.NullLiteral => ExprBlob.code("null")
        case ValuefyExpr.BoolLiteral(value) => ExprBlob.code(s"$value")
        case ValuefyExpr.CharLiteral(value) => ExprBlob.code(s"'${escapeChar(value)}'") // TODO escape
        case ValuefyExpr.CharFromTerminalLiteral =>
          ExprBlob(List(), s"(inputs[$beginGen] as Inputs.Character).char()", Set())
        case ValuefyExpr.StringLiteral(value) => ExprBlob.code("\"" + escapeString(value) + "\"") // TODO escape
      }
    case enumValue: ValuefyExpr.EnumValue =>
      enumValue match {
        case ValuefyExpr.CanonicalEnumValue(enumName, enumValue) => ExprBlob.code(s"$enumName.$enumValue")
        case ValuefyExpr.ShortenedEnumValue(unspecifiedEnumTypeId, enumValue) =>
          val enumName = analysis.shortenedEnumTypesMap(unspecifiedEnumTypeId)
          ExprBlob.code(s"$enumName.$enumValue")
      }
  }

  def funcCallToCode(funcType: ValuefyExpr.FuncType.Value, params: List[ValuefyExpr], beginGen: String, endGen: String, symbol: Symbols.Symbol, sequenceVarName: SequenceVarName): ExprBlob = {
    funcType match {
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.IsPresent =>
        check(params.size == 1, "ispresent function only can have exactly one parameter")
        val param = valuefyExprToCode(params.head, beginGen, endGen, symbol, sequenceVarName)

        @tailrec def isPresentCode(paramType: Type): String = paramType match {
          // case Type.NodeType => s"${param.result}.sourceText.nonEmpty"
          case Type.ArrayOf(_) => s"${param.result}.isNotEmpty()"
          case Type.OptionalOf(_) => s"${param.result} != null"
          case Type.StringType => s"${param.result}.isNotEmpty()"
          case unionType: Type.UnionOf =>
            val reducedType = analysis.reduceUnionType(unionType)
            check(!reducedType.isInstanceOf[Type.UnionOf], "union type not supported in ispresent function")
            isPresentCode(reducedType)
          // 그 외 타입은 지원하지 않음
        }

        ExprBlob(param.prepares, isPresentCode(typeOf(params.head)), param.required)
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.IsEmpty =>
        check(params.size == 1, "isempty function only can have exactly one parameter")
        val param = valuefyExprToCode(params.head, beginGen, endGen, symbol, sequenceVarName)

        @tailrec def isEmptyCode(paramType: Type): String = paramType match {
          // case Type.NodeType => s"${param.result}.TODO"
          case Type.ArrayOf(_) => s"${param.result}.isEmpty()"
          case Type.OptionalOf(_) => s"${param.result} == null"
          case Type.StringType => s"${param.result}.isEmpty()"
          case unionType: Type.UnionOf =>
            val reducedType = analysis.reduceUnionType(unionType)
            check(!reducedType.isInstanceOf[Type.UnionOf], "union type not supported in isempty function")
            isEmptyCode(reducedType)
          // 그 외 타입은 지원하지 않음
        }

        ExprBlob(param.prepares, isEmptyCode(typeOf(params.head)), param.required)
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.Chr =>
        check(params.size == 1, "chr function only can have exactly one parameter")
        val paramCode = valuefyExprToCode(params.head, beginGen, endGen, symbol, sequenceVarName)
        val result = typeOf(params.head) match {
          // case Type.NodeType => s"${paramCode.result}.TODO()"
          case Type.CharType => paramCode.result
        }
        ExprBlob(paramCode.prepares, result, paramCode.required)
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.Str =>
        val paramCodes = params.map(valuefyExprToCode(_, beginGen, endGen, symbol, sequenceVarName))

        def toStringCode(input: String, inputType: Type): String = inputType match {
          // case Type.NodeType => s"$input.TODO()"
          case Type.ArrayOf(elemType) => s"""$input.joinToString("") { ${toStringCode("it", elemType)} }"""
          case Type.OptionalOf(valueType) =>
            s"""($input?.let { ${toStringCode("it", valueType)} } ?: "")"""
          case Type.BoolType => s"$input.toString()"
          case Type.CharType => s"$input.toString()"
          case Type.StringType => input
          case unionType: Type.UnionOf =>
            val reducedType = analysis.reduceUnionType(unionType)
            check(!reducedType.isInstanceOf[Type.UnionOf], "union type not supported in str function")
            toStringCode(input, reducedType)
        }

        ExprBlob(paramCodes.flatMap(_.prepares),
          paramCodes.zip(params).map { case (paramCode, param) =>
            val paramType = typeOf(param)
            toStringCode(paramCode.result, paramType)
          }.mkString(" + "),
          paramCodes.flatMap(_.required).toSet)
    }
  }

  def generate(pkgName: Option[String] = None): String = {
    val writer = new StringWriter()

    writer.write(classDefs(analysis.classRelations).code)
    writer.write("\n\n")

    var visitedNonterms = Set[String]()
    writer.write(matchStartFunc().code)
    writer.write("\n\n")
    while ((_requiredNonterms -- visitedNonterms).nonEmpty) {
      val next = (_requiredNonterms -- visitedNonterms).head
      writer.write(nonterminalMatchFunc(next).code)
      writer.write("\n\n")
      visitedNonterms += next
    }

    writer.toString
  }
}
