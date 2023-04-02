package com.giyeok.jparser.metalang3.codegen

import com.giyeok.jparser.Inputs.CharsGrouping
import com.giyeok.jparser.NGrammar.{NNonterminal, NStart}
import com.giyeok.jparser.metalang3.MetaLanguage3.{ProcessedGrammar, check}
import com.giyeok.jparser.metalang3.codegen.ScalaCodeGen.{AuxTrait, Options}
import com.giyeok.jparser.metalang3.{ClassHierarchyItem, ClassRelationCollector, Type, ValuefyExpr}
import com.giyeok.jparser.proto.GrammarProtobufConverter
import com.giyeok.jparser.utils.JavaCodeGenUtil.javaChar
import com.giyeok.jparser.{NGrammar, Symbols}

import java.util.Base64
import scala.annotation.tailrec

object ScalaCodeGen {

  object AuxTrait extends Enumeration {
    // WithParseNode가 설정되면 모든 ast node 클래스가 WithParseNode trait을 상속받고 parse node 정보를 저장한다
    // WithIdAndParseNode가 설정되면 모든 ast node 클래스에 unique id가 배정된다
    val NoAuxTrait, WithParseNode, WithIdAndParseNode = Value
  }

  /**
   *
   * @param useNull         (미구현) true이면 Option 대신 그냥 값+null을 사용한다.
   * @param looseSuperType  TODO 기본 false
   * @param assertBindTypes Unbind할 때 unbind된 심볼의 타입을 체크한다. 기본 true
   * @param symbolComments  human readable한 심볼 설명 코멘트를 추가한다. 기본 true
   * @param auxTrait        어떤 aux trait을 사용할 지. 기본 WithParseNode
   */
  case class Options(useNull: Boolean = false,
    looseSuperType: Boolean = false,
    assertBindTypes: Boolean = true,
    symbolComments: Boolean = true,
    auxTrait: AuxTrait.Value = AuxTrait.WithIdAndParseNode,
    grammarDefType: GrammarDefType = InlineSourceDef,
    emitNGrammar: Boolean = false)

  sealed trait GrammarDefType

  object InlineSourceDef extends GrammarDefType

  object InlineProtoDef extends GrammarDefType

  case class FileProtoDef(filePath: String) extends GrammarDefType
}

class ScalaCodeGen(val analysis: ProcessedGrammar, val options: Options = Options()) {
  def escapeString(str: String): String = "\"" + str.toCharArray.map(javaChar).mkString("") + "\""

  def escapeChar(c: Char): String = s"'${javaChar(c)}'"

  def ngrammarDef(ngrammar: NGrammar): CodeBlob = {
    def symbolString(symbol: Symbols.Symbol): String = symbol match {
      case Symbols.Nonterminal(name) => s"Symbols.Nonterminal(${escapeString(name)})"
      case Symbols.OneOf(syms) => s"Symbols.OneOf(ListSet(${syms map symbolString mkString ","}))"
      case Symbols.Repeat(sym, lower) => s"Symbols.Repeat(${symbolString(sym)}, $lower)"
      case Symbols.Except(sym, except) => s"Symbols.Except(${symbolString(sym)}, ${symbolString(except)})"
      case Symbols.LookaheadIs(sym) => s"Symbols.LookaheadIs(${symbolString(sym)})"
      case Symbols.LookaheadExcept(sym) => s"Symbols.LookaheadExcept(${symbolString(sym)})"
      case Symbols.Proxy(sym) => s"Symbols.Proxy(${symbolString(sym)})"
      case Symbols.Join(sym, join) => s"Symbols.Join(${symbolString(sym)}, ${symbolString(join)})"
      case Symbols.Longest(sym) => s"Symbols.Longest(${symbolString(sym)})"
      case Symbols.ExactChar(c) => s"Symbols.ExactChar(${escapeChar(c)})"
      case Symbols.Any => "Symbols.Any"
      case Symbols.AnyChar => "Symbols.AnyChar"
      case Symbols.Chars(chars) =>
        val groups = chars.groups
        val (singles, doubles) = groups partition { g => g._1 == g._2 }
        val singlesString = if (singles.isEmpty) None else {
          Some(s"Set(${singles map (_._1) map escapeChar mkString ","})")
        }
        val doublesString = if (doubles.isEmpty) None else {
          val g = doubles map { g => s"(${escapeChar(g._1)} to ${escapeChar(g._2)}).toSet" }
          Some(g mkString " ++ ")
        }
        s"Symbols.Chars(${List(singlesString, doublesString).flatten mkString " ++ "})"
      case Symbols.Sequence(seq) => s"Symbols.Sequence(Seq(${seq map symbolString mkString ","}))"
    }

    def intSetString(s: Set[Int]) = s"Set(${s mkString ","})"

    def intSeqString(s: Seq[Int]) = s"Seq(${s mkString ","})"

    def nAtomicSymbolString(nsymbol: NGrammar.NAtomicSymbol): String = nsymbol match {
      case NGrammar.NTerminal(id, symbol) =>
        s"NGrammar.NTerminal($id, ${symbolString(symbol)})"
      case NGrammar.NStart(id, produce) =>
        s"NGrammar.NStart($id, $produce)"
      case NGrammar.NNonterminal(id, symbol, produces) =>
        s"NGrammar.NNonterminal($id, ${symbolString(symbol)}, ${intSetString(produces)})"
      case NGrammar.NOneOf(id, symbol, produces) =>
        s"NGrammar.NOneOf($id, ${symbolString(symbol)}, ${intSetString(produces)})"
      case NGrammar.NProxy(id, symbol, produce) =>
        s"NGrammar.NProxy($id, ${symbolString(symbol)}, $produce)"
      case NGrammar.NRepeat(id, symbol, baseSeq, repeatSeq) =>
        s"NGrammar.NRepeat($id, ${symbolString(symbol)}, $baseSeq, $repeatSeq)"
      case NGrammar.NExcept(id, symbol, body, except) =>
        s"NGrammar.NExcept($id, ${symbolString(symbol)}, $body, $except)"
      case NGrammar.NJoin(id, symbol, body, join) =>
        s"NGrammar.NJoin($id, ${symbolString(symbol)}, $body, $join)"
      case NGrammar.NLongest(id, symbol, body) =>
        s"NGrammar.NLongest($id, ${symbolString(symbol)}, $body)"
      case NGrammar.NLookaheadIs(id, symbol, emptySeqId, lookahead) =>
        s"NGrammar.NLookaheadIs($id, ${symbolString(symbol)}, $emptySeqId, $lookahead)"
      case NGrammar.NLookaheadExcept(id, symbol, emptySeqId, lookahead) =>
        s"NGrammar.NLookaheadExcept($id, ${symbolString(symbol)}, $emptySeqId, $lookahead)"
    }

    def nSequenceString(nseq: NGrammar.NSequence): String =
      s"NGrammar.NSequence(${nseq.id}, ${symbolString(nseq.symbol)}, ${intSeqString(nseq.sequence)})"

    val nsymbolsString = ngrammar.nsymbols.toList.sortBy(_._1) flatMap { s =>
      List( // s"// ${s._2.symbol.toShortString}",
        s"${s._1} -> ${nAtomicSymbolString(s._2)}")
    }
    val nseqsString = ngrammar.nsequences.toList.sortBy(_._1) flatMap { s =>
      List( // s"// ${s._2.symbol.toShortString}",
        s"${s._1} -> ${nSequenceString(s._2)}")
    }
    CodeBlob(
      s"""new NGrammar(
         |  Map(${nsymbolsString mkString ",\n"}),
         |  Map(${nseqsString mkString ",\n"}),
         |  ${ngrammar.startSymbol})""".stripMargin,
      Set("scala.collection.immutable.ListSet",
        "com.giyeok.jparser.NGrammar",
        "com.giyeok.jparser.Symbols",
        "com.giyeok.jparser.ParsingErrors",
        "com.giyeok.jparser.nparser.Parser",
        "com.giyeok.jparser.nparser.NaiveParser"))
  }

  def classDefs(classRelation: ClassRelationCollector): CodeBlob = {
    val classHierarchy = classRelation.toHierarchy

    val baseCodeBlob = options.auxTrait match {
      case AuxTrait.NoAuxTrait => CodeBlob("", Set())
      case AuxTrait.WithParseNode => CodeBlob("sealed trait WithParseNode { val parseNode: Node }", Set())
      case AuxTrait.WithIdAndParseNode => CodeBlob("sealed trait WithIdAndParseNode { val id: Int; val parseNode: Node }", Set())
    }
    if (classHierarchy.allTypes.isEmpty) baseCodeBlob else {
      def classDefinitionCode(cls: ClassHierarchyItem): CodeBlob = {
        val supers = if (cls.superclasses.isEmpty) {
          options.auxTrait match {
            case AuxTrait.NoAuxTrait => ""
            case AuxTrait.WithParseNode => " extends WithParseNode"
            case AuxTrait.WithIdAndParseNode => " extends WithIdAndParseNode"
          }
        } else {
          val superclasses = cls.superclasses.toList.sorted ++ (options.auxTrait match {
            case AuxTrait.NoAuxTrait => List()
            case AuxTrait.WithParseNode => List("WithParseNode")
            case AuxTrait.WithIdAndParseNode => List("WithIdAndParseNode")
          })
          s" extends ${superclasses.mkString(" with ")}"
        }
        if (cls.subclasses.isEmpty) {
          // concrete type
          val params = analysis.classParamTypes.getOrElse(cls.className, List()).map { param =>
            val paramType = typeDescStringOf(param._2, context = Some(s"parameter type of ${cls.className}.${param._1}"))
            CodeBlob(s"${param._1}: ${paramType.code}", paramType.required)
          }
          val parseNodeVal = options.auxTrait match {
            case AuxTrait.NoAuxTrait => ""
            case AuxTrait.WithParseNode => "(override val parseNode: Node)"
            case AuxTrait.WithIdAndParseNode => "(override val id: Int, override val parseNode: Node)"
          }
          CodeBlob(s"case class ${cls.className}(${params.map(_.code).mkString(", ")})$parseNodeVal$supers",
            params.flatMap(_.required).toSet)
        } else {
          // abstract type
          CodeBlob(s"sealed trait ${cls.className}$supers", Set())
        }
      }

      (baseCodeBlob +: classHierarchy.allTypes.values.toList.sorted.map(classDefinitionCode)).reduce(_ + _)
    }
  }

  def typeDescStringOf(typ: Type, context: Option[String] = None): CodeBlob = typ match {
    case Type.NodeType => CodeBlob("Node", Set("com.giyeok.jparser.ParseResultTree.Node"))
    case Type.ClassType(name) => CodeBlob(name, Set())
    case Type.OptionalOf(typ) =>
      val elemType = typeDescStringOf(typ, context)
      CodeBlob(s"Option[${elemType.code}]", elemType.required)
    case Type.ArrayOf(typ) =>
      val elemType = typeDescStringOf(typ, context)
      CodeBlob(s"List[${elemType.code}]", elemType.required)
    case unionType: Type.UnionOf =>
      analysis.reduceUnionType(unionType) match {
        case reducedType: Type.UnionOf =>
          val unionTypeNames = reducedType.types.map(Type.readableNameOf)
          val contextText = context.map(ctx => s" around $ctx").getOrElse("")
          throw new Exception(s"Union type of {${unionTypeNames.mkString(", ")}} is not supported$contextText")
        case reducedType => typeDescStringOf(reducedType)
      }
    case Type.EnumType(enumName) => CodeBlob(s"$enumName.Value", Set())
    case Type.UnspecifiedEnumType(uniqueId) => CodeBlob(s"${analysis.shortenedEnumTypesMap(uniqueId)}.Value", Set())
    case Type.NullType => CodeBlob("Option[Nothing]", Set())
    case Type.AnyType => CodeBlob("Any", Set())
    case Type.BoolType => CodeBlob("Boolean", Set())
    case Type.CharType => CodeBlob("Char", Set())
    case Type.StringType => CodeBlob("String", Set())
    case Type.NothingType => CodeBlob("Nothing", Set())
  }

  def enumDefs(enumValuesMap: Map[String, Set[String]]): CodeBlob = {
    if (enumValuesMap.isEmpty) CodeBlob("", Set()) else enumValuesMap.keySet.toList.sorted.map { enumName =>
      val members = enumValuesMap(enumName).toList.sorted
      CodeBlob(s"object $enumName extends Enumeration { val ${members.mkString(", ")} = Value }", Set())
    }.reduce(_ + _)
  }

  def matchStartFunc(): CodeBlob = {
    val startSymbol = analysis.ngrammar.symbolOf(analysis.ngrammar.nsymbols(analysis.ngrammar.startSymbol).asInstanceOf[NStart].produce).asInstanceOf[NNonterminal]
    val returnType = typeDescStringOf(analysis.nonterminalTypes(analysis.startNonterminalName), context = Some(s"return type of ${analysis.startNonterminalName}"))
    CodeBlob(
      s"""def matchStart(node: Node): ${returnType.code} = {
         |  val BindNode(start, BindNode(_, body)) = node
         |  assert(start.id == ${analysis.ngrammar.startSymbol})
         |  ${nonterminalMatchFuncName(startSymbol.symbol.name)}(body)
         |}""".stripMargin,
      returnType.required ++ Set(
        "com.giyeok.jparser.ParseResultTree.Node",
        "com.giyeok.jparser.ParseResultTree.BindNode"))
  }

  def nonterminalMatchFunc(nonterminal: String): CodeBlob = {
    val valuefyExpr = analysis.nonterminalValuefyExprs(nonterminal)
    val body = unrollChoicesExpr(valuefyExpr.choices, "node", analysis.nonterminalTypes(nonterminal))
    val returnType = typeDescStringOf(analysis.nonterminalTypes(nonterminal), Some(s"return type of nonterminal $nonterminal"))
    val bodyCode = if (body.prepares.isEmpty) {
      CodeBlob(body.result, body.required)
    } else {
      CodeBlob(body.prepares.mkString("\n") + "\n" + body.result, body.required).wrapBrace()
    }
    CodeBlob(s"def ${nonterminalMatchFuncName(nonterminal)}(node: Node): ${returnType.code} = " + bodyCode.code,
      body.required ++ returnType.required ++ bodyCode.required + "com.giyeok.jparser.ParseResultTree.Node")
  }

  private def unrollChoicesExpr(choices: Map[Symbols.Symbol, ValuefyExpr], inputName: String, requiredType: Type): ExprBlob = {
    val bindedVar = newVar()
    val bodyVar = newVar()
    val casesExpr = choices.toList.sortBy(choice => analysis.ngrammar.idOf(choice._1)).map { choice =>
      val choiceId = analysis.ngrammar.idOf(choice._1)
      val bodyCode = valuefyExprToCodeWithCoercion(choice._2, bodyVar, requiredType)
      val caseBody =
        if (bodyCode.prepares.isEmpty) bodyCode.result
        else (bodyCode.prepares :+ bodyCode.result).map("  " + _) mkString ("\n")
      CodeBlob(s"case $choiceId =>\n$caseBody", bodyCode.required)
    }.toList.reduce(_ + _)
    val unrolledVar = newVar()
    ExprBlob(List(
      s"val BindNode($bindedVar, $bodyVar) = $inputName",
      s"val $unrolledVar = " + casesExpr.wrap(s"$bindedVar.id match {", "}").code
    ), unrolledVar, casesExpr.required)
  }

  private def nonterminalMatchFuncName(nonterminal: String) =
    s"match${nonterminal.substring(0, 1).toUpperCase}${nonterminal.substring(1)}"

  private var counter = 0

  private def newVar(): String = {
    counter += 1
    s"v$counter"
  }

  private def typeOf(expr: ValuefyExpr): Type = analysis.typeInferer.typeOfValuefyExpr(expr).get

  private def addCoercion(expr: String, requiredType: Type, actualType: Type): String =
    (requiredType, actualType) match {
      case (requiredType, actualType) if requiredType == actualType => expr
      case (Type.OptionalOf(optType), actual) if optType == actual => s"Some($expr)"
      case _ => expr // TODO
    }

  private def addCoercion(expr: ExprBlob, requiredType: Type, actualType: Type): ExprBlob =
    expr.copy(result = addCoercion(expr.result, requiredType, actualType))

  private def valuefyExprToCodeWithCoercion(valuefyExpr: ValuefyExpr, inputName: String, requiredType: Type): ExprBlob =
    addCoercion(valuefyExprToCode(valuefyExpr, inputName), requiredType, typeOf(valuefyExpr))

  def funcCallToCode(funcCall: ValuefyExpr.FuncCall, inputName: String): ExprBlob = {
    val ValuefyExpr.FuncCall(funcType, params) = funcCall
    // TODO coercion
    funcType match {
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.IsPresent =>
        check(params.size == 1, "ispresent function only can have exactly one parameter")
        val param = valuefyExprToCode(params.head, inputName)

        @tailrec def isPresentCode(paramType: Type): String = paramType match {
          case Type.NodeType => s"${param.result}.sourceText.nonEmpty"
          case Type.ArrayOf(_) => s"${param.result}.nonEmpty"
          case Type.OptionalOf(_) => s"${param.result}.isDefined"
          case Type.StringType => s"${param.result}.nonEmpty"
          case unionType: Type.UnionOf =>
            val reducedType = analysis.reduceUnionType(unionType)
            check(!reducedType.isInstanceOf[Type.UnionOf], "union type not supported in ispresent function")
            isPresentCode(reducedType)
          // 그 외 타입은 지원하지 않음
        }

        ExprBlob(param.prepares, isPresentCode(typeOf(params.head)), param.required)
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.IsEmpty =>
        check(params.size == 1, "isempty function only can have exactly one parameter")
        val param = valuefyExprToCode(params.head, inputName)

        @tailrec def isEmptyCode(paramType: Type): String = paramType match {
          case Type.NodeType => s"${param.result}.sourceText.isEmpty"
          case Type.ArrayOf(_) => s"${param.result}.isEmpty"
          case Type.OptionalOf(_) => s"${param.result}.isEmpty"
          case Type.StringType => s"${param.result}.isEmpty"
          case unionType: Type.UnionOf =>
            val reducedType = analysis.reduceUnionType(unionType)
            check(!reducedType.isInstanceOf[Type.UnionOf], "union type not supported in isempty function")
            isEmptyCode(reducedType)
          // 그 외 타입은 지원하지 않음
        }

        ExprBlob(param.prepares, isEmptyCode(typeOf(params.head)), param.required)
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.Chr =>
        check(params.size == 1, "chr function only can have exactly one parameter")
        val paramCode = valuefyExprToCode(params.head, inputName)
        val result = typeOf(params.head) match {
          case Type.NodeType => s"${paramCode.result}.sourceText.charAt(0)"
          case Type.CharType => paramCode.result
        }
        ExprBlob(paramCode.prepares, result, paramCode.required)
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.Str =>
        val paramCodes = params.map(valuefyExprToCode(_, inputName))

        def toStringCode(input: String, inputType: Type): String = inputType match {
          case Type.NodeType => s"$input.sourceText"
          case Type.ArrayOf(elemType) => s"""$input.map(x => ${toStringCode("x", elemType)}).mkString("")"""
          case Type.OptionalOf(valueType) =>
            s"""$input.map(x => ${toStringCode("x", valueType)}).getOrElse("")"""
          case Type.BoolType => s"$input.toString"
          case Type.CharType => s"$input.toString"
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

  def valuefyExprToCode(valuefyExpr: ValuefyExpr, inputName: String): ExprBlob = valuefyExpr match {
    case ValuefyExpr.InputNode => ExprBlob(List(), inputName, Set())
    case ValuefyExpr.MatchNonterminal(nonterminalName) =>
      ExprBlob(List(), s"${nonterminalMatchFuncName(nonterminalName)}($inputName)", Set())
    case ValuefyExpr.Unbind(symbol, expr) =>
      val bindedVar = newVar()
      val bodyVar = newVar()
      val exprCode = valuefyExprToCode(expr, bodyVar)
      ExprBlob(List(
        s"val BindNode($bindedVar, $bodyVar) = $inputName",
        s"assert($bindedVar.id == ${analysis.ngrammar.idOf(symbol)})"
      ) ++ exprCode.prepares, exprCode.result, exprCode.required)
    case ValuefyExpr.JoinBody(bodyProcessor) =>
      val bodyVar = newVar()
      val bodyProcessorCode = valuefyExprToCode(bodyProcessor, bodyVar)
      ExprBlob(s"val JoinNode(_, $bodyVar, _) = $inputName" +: bodyProcessorCode.prepares,
        bodyProcessorCode.result,
        bodyProcessorCode.required + "com.giyeok.jparser.ParseResultTree.JoinNode")
    case ValuefyExpr.JoinCond(condProcessor) =>
      val condVar = newVar()
      val bodyProcessorCode = valuefyExprToCode(condProcessor, condVar)
      ExprBlob(s"val JoinNode(_, _, $condVar) = $inputName" +: bodyProcessorCode.prepares,
        bodyProcessorCode.result,
        bodyProcessorCode.required + "com.giyeok.jparser.ParseResultTree.JoinNode")
    case ValuefyExpr.SeqElemAt(index, expr) =>
      val elemVar = newVar()
      val exprCode = valuefyExprToCode(expr, elemVar)
      val indexer = if (index == 0) ".head" else s"($index)"
      ExprBlob(
        s"val $elemVar = $inputName.asInstanceOf[SequenceNode].children$indexer" +: exprCode.prepares,
        exprCode.result,
        exprCode.required + "com.giyeok.jparser.ParseResultTree.SequenceNode")
    case ValuefyExpr.UnrollRepeatFromZero(elemProcessor) =>
      val unrolledVar = newVar()
      val elemProcessorCode = valuefyExprToCode(elemProcessor, "elem")
      ExprBlob(
        List(s"val $unrolledVar = unrollRepeat0($inputName).map { elem =>") ++
          elemProcessorCode.prepares :+
          addCoercion(elemProcessorCode.result, typeOf(valuefyExpr).asInstanceOf[Type.ArrayOf].elemType, typeOf(elemProcessor)) :+
          "}",
        unrolledVar,
        elemProcessorCode.required + "com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat0")
    case ValuefyExpr.UnrollRepeatFromZeroNoUnbind(repeatSymbol, elemProcessor) =>
      val unrolledVar = newVar()
      val elemProcessorCode = valuefyExprToCode(elemProcessor, "elem")
      val repeatSeqId = analysis.ngrammar.idOf(repeatSymbol.repeatSeq)
      val baseSeqId = analysis.ngrammar.idOf(repeatSymbol.baseSeq)
      ExprBlob(
        List(s"val $unrolledVar = unrollRepeat0NoUnbind(repeatSeqId, baseSeqId, $inputName).map { elem =>") ++
          elemProcessorCode.prepares :+
          addCoercion(elemProcessorCode.result, typeOf(valuefyExpr).asInstanceOf[Type.ArrayOf].elemType, typeOf(elemProcessor)) :+
          "}",
        unrolledVar,
        elemProcessorCode.required + "com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat0NoUnbind")
    case ValuefyExpr.UnrollRepeatFromOne(elemProcessor) =>
      val unrolledVar = newVar()
      val elemProcessorCode = valuefyExprToCode(elemProcessor, "elem")
      ExprBlob(
        List(s"val $unrolledVar = unrollRepeat1($inputName).map { elem =>") ++
          elemProcessorCode.prepares :+
          addCoercion(elemProcessorCode.result, typeOf(valuefyExpr).asInstanceOf[Type.ArrayOf].elemType, typeOf(elemProcessor)) :+
          "}",
        unrolledVar,
        elemProcessorCode.required + "com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat1")
    case ValuefyExpr.UnrollRepeatFromOneNoUnbind(repeatSymbol, elemProcessor) =>
      val unrolledVar = newVar()
      val elemProcessorCode = valuefyExprToCode(elemProcessor, "elem")
      val repeatSeqId = analysis.ngrammar.idOf(repeatSymbol.repeatSeq)
      val baseSeqId = analysis.ngrammar.idOf(repeatSymbol.baseSeq)
      ExprBlob(
        List(s"val $unrolledVar = unrollRepeat1NoUnbind($repeatSeqId, $baseSeqId, $inputName).map { elem =>") ++
          elemProcessorCode.prepares :+
          addCoercion(elemProcessorCode.result, typeOf(valuefyExpr).asInstanceOf[Type.ArrayOf].elemType, typeOf(elemProcessor)) :+
          "}",
        unrolledVar,
        elemProcessorCode.required + "com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat1NoUnbind")
    case ValuefyExpr.UnrollChoices(choices) =>
      unrollChoicesExpr(choices, inputName, typeOf(valuefyExpr))
    case ValuefyExpr.ConstructCall(className, params) =>
      val paramCodes = params.zipWithIndex.map { case (param, index) =>
        valuefyExprToCodeWithCoercion(param, inputName, analysis.classParamTypes(className)(index)._2)
      }
      val constructorExpr = s"$className(${paramCodes.map(_.result).mkString(", ")})" + (
        options.auxTrait match {
          case AuxTrait.NoAuxTrait => ""
          case AuxTrait.WithParseNode => s"($inputName)"
          case AuxTrait.WithIdAndParseNode => s"(nextId(), $inputName)"
        })
      ExprBlob(paramCodes.flatMap(_.prepares), constructorExpr, paramCodes.flatMap(_.required).toSet)
    case funcCall: ValuefyExpr.FuncCall =>
      funcCallToCode(funcCall, inputName)
    case ValuefyExpr.ArrayExpr(elems) =>
      val elemType = typeOf(valuefyExpr).asInstanceOf[Type.ArrayOf].elemType
      val elemCodes = elems.map(valuefyExprToCodeWithCoercion(_, inputName, elemType))
      ExprBlob(elemCodes.flatMap(_.prepares),
        s"List(${elemCodes.map(_.result).mkString(", ")})",
        elemCodes.flatMap(_.required).toSet)
    case ValuefyExpr.BinOp(op, lhs, rhs) =>
      val lhsCode = valuefyExprToCode(lhs, inputName)
      val rhsCode = valuefyExprToCode(rhs, inputName)
      val opExpr = op match {
        case com.giyeok.jparser.metalang3.ValuefyExpr.BinOpType.ADD =>
          (typeOf(lhs), typeOf(rhs)) match {
            case (Type.StringType, Type.StringType) => s"${lhsCode.result} + ${rhsCode.result}"
            case (Type.ArrayOf(_), Type.ArrayOf(_)) => s"${lhsCode.result} ++ ${rhsCode.result}"
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
          val exprCode = valuefyExprToCodeWithCoercion(expr, inputName, Type.BoolType)
          val resultVar = newVar()
          ExprBlob(exprCode.prepares :+ s"val $resultVar = !${exprCode.result}", resultVar, exprCode.required)
      }
    case ValuefyExpr.ElvisOp(expr, ifNull) =>
      val exprVar = newVar()
      val requiredType = typeOf(valuefyExpr)
      val exprCode = valuefyExprToCodeWithCoercion(expr, inputName, requiredType)
      val ifNullCode = valuefyExprToCodeWithCoercion(ifNull, inputName, requiredType)
      ExprBlob(
        exprCode.prepares :+ s"val $exprVar = ${exprCode.result}",
        s"if ($exprVar.isDefined) $exprVar.get else ${ifNullCode.withBraceIfNeeded}",
        exprCode.required ++ ifNullCode.required)
    case ValuefyExpr.TernaryOp(condition, ifTrue, ifFalse) =>
      val conditionCode = valuefyExprToCode(condition, inputName)
      val resultType = typeOf(valuefyExpr)
      val thenCode = valuefyExprToCodeWithCoercion(ifTrue, inputName, resultType)
      val elseCode = valuefyExprToCodeWithCoercion(ifFalse, inputName, resultType)
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
        case ValuefyExpr.NullLiteral => ExprBlob.code("None")
        case ValuefyExpr.BoolLiteral(value) => ExprBlob.code(s"$value")
        case ValuefyExpr.CharLiteral(value) => ExprBlob.code(s"'${javaChar(value)}'") // TODO escape
        case ValuefyExpr.CharFromTerminalLiteral =>
          ExprBlob(List(),
            s"$inputName.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char",
            Set("com.giyeok.jparser.ParseResultTree.TerminalNode",
              "com.giyeok.jparser.Inputs"))
        case ValuefyExpr.StringLiteral(value) => ExprBlob.code("\"" + value + "\"") // TODO escape
      }
    case enumValue: ValuefyExpr.EnumValue =>
      enumValue match {
        case ValuefyExpr.CanonicalEnumValue(enumName, enumValue) => ExprBlob.code(s"$enumName.$enumValue")
        case ValuefyExpr.ShortenedEnumValue(unspecifiedEnumTypeId, enumValue) =>
          val enumName = analysis.shortenedEnumTypesMap(unspecifiedEnumTypeId)
          ExprBlob.code(s"$enumName.$enumValue")
      }
  }

  def reachableNonterminalMatchFuncs(valuefyExpr: ValuefyExpr, cc: Set[String]): Set[String] = valuefyExpr match {
    case ValuefyExpr.MatchNonterminal(nonterminalName) =>
      if (cc contains nonterminalName) cc
      else reachableNonterminalMatchFuncs(analysis.nonterminalValuefyExprs(nonterminalName), cc + nonterminalName)
    case ValuefyExpr.Unbind(_, expr) => reachableNonterminalMatchFuncs(expr, cc)
    case ValuefyExpr.JoinBody(bodyProcessor) => reachableNonterminalMatchFuncs(bodyProcessor, cc)
    case ValuefyExpr.JoinCond(condProcessor) => reachableNonterminalMatchFuncs(condProcessor, cc)
    case ValuefyExpr.SeqElemAt(_, expr) => reachableNonterminalMatchFuncs(expr, cc)
    case ValuefyExpr.UnrollRepeatFromZero(elemProcessor) =>
      reachableNonterminalMatchFuncs(elemProcessor, cc)
    case ValuefyExpr.UnrollRepeatFromZeroNoUnbind(repeatSymbol, elemProcessor) =>
      reachableNonterminalMatchFuncs(elemProcessor, cc)
    case ValuefyExpr.UnrollRepeatFromOne(elemProcessor) =>
      reachableNonterminalMatchFuncs(elemProcessor, cc)
    case ValuefyExpr.UnrollRepeatFromOneNoUnbind(repeatSymbol, elemProcessor) =>
      reachableNonterminalMatchFuncs(elemProcessor, cc)
    case ValuefyExpr.UnrollChoices(choices) =>
      choices.values.foldLeft(cc) { (m, choiceExpr) => reachableNonterminalMatchFuncs(choiceExpr, m) }
    case ValuefyExpr.ConstructCall(_, params) =>
      params.foldLeft(cc) { (m, paramExpr) => reachableNonterminalMatchFuncs(paramExpr, m) }
    case ValuefyExpr.FuncCall(_, params) =>
      params.foldLeft(cc) { (m, paramExpr) => reachableNonterminalMatchFuncs(paramExpr, m) }
    case ValuefyExpr.ArrayExpr(elems) =>
      elems.foldLeft(cc) { (m, elemExpr) => reachableNonterminalMatchFuncs(elemExpr, m) }
    case ValuefyExpr.BinOp(_, lhs, rhs) =>
      reachableNonterminalMatchFuncs(lhs, reachableNonterminalMatchFuncs(rhs, cc))
    case ValuefyExpr.PreOp(_, expr) =>
      reachableNonterminalMatchFuncs(expr, cc)
    case ValuefyExpr.ElvisOp(expr, ifNull) =>
      reachableNonterminalMatchFuncs(expr, reachableNonterminalMatchFuncs(ifNull, cc))
    case ValuefyExpr.TernaryOp(condition, ifTrue, ifFalse) =>
      reachableNonterminalMatchFuncs(condition,
        reachableNonterminalMatchFuncs(ifTrue,
          reachableNonterminalMatchFuncs(ifFalse, cc)))
    case ValuefyExpr.InputNode | _: ValuefyExpr.Literal | _: ValuefyExpr.EnumValue => cc
  }

  def generateParser(className: String, ngrammarDefCode: CodeBlob, parserDef: CodeBlob, mainFunc: Option[CodeBlob]): String = {
    val classDefsCode = classDefs(analysis.classRelations)
    val enumDefsCode = enumDefs(analysis.enumValuesMap)
    val reachableNonterms = reachableNonterminalMatchFuncs(
      analysis.nonterminalValuefyExprs(analysis.startNonterminalName), Set(analysis.startNonterminalName))
    val nontermMatchCodes = reachableNonterms.toList.sorted.map(nonterminalMatchFunc)
    val startMatchCode = matchStartFunc()
    val startType = typeDescStringOf(analysis.nonterminalTypes(analysis.startNonterminalName), context = Some(s"nonterminal type of ${analysis.startNonterminalName}"))

    val allImports = (ngrammarDefCode.required ++
      classDefsCode.required ++
      enumDefsCode.required ++
      nontermMatchCodes.flatMap(_.required) ++
      startMatchCode.required ++
      startType.required ++
      parserDef.required ++
      mainFunc.map(_.required).getOrElse(Set()) +
      "com.giyeok.jparser.nparser.ParseTreeUtil" +
      "com.giyeok.jparser.NGrammar").toList.sorted
    val ngrammarDefString =
      if (options.emitNGrammar) s"  val ngrammar: NGrammar = ${ngrammarDefCode.indent().code}\n" else ""
    s"""${allImports.map(pkg => s"import $pkg").mkString("\n")}
       |import $className._
       |
       |object $className {
       |$ngrammarDefString
       |${classDefsCode.indent().code}
       |${enumDefsCode.indent().code}
       |
       |${parserDef.indent().code}
       |${mainFunc.map(_.indent().code).getOrElse("")}
       |}
       |
       |class $className {
       |  private var idCounter = 0
       |
       |  def nextId(): Int = {
       |    idCounter += 1
       |    idCounter
       |  }
       |
       |${startMatchCode.indent().code}
       |${nontermMatchCodes.map(_.indent().code).mkString("\n\n")}
       |}
       |""".stripMargin
  }

  def generateParser(className: String, mainFuncExamples: Option[List[String]] = None): String = {
    val grammarDef = options.grammarDefType match {
      case ScalaCodeGen.InlineSourceDef => ngrammarDef(analysis.ngrammar)
      case ScalaCodeGen.InlineProtoDef => inlinedProtoNGrammar()
      case ScalaCodeGen.FileProtoDef(filePath) => fileProtoGrammar(filePath)
    }
    generateParser(className, grammarDef, naiveParserDef(), mainFuncExamples.map(mainFuncBlob))
  }

  def generateParser(className: String): String = {
    generateParser(className, CodeBlob("", Set()), CodeBlob("", Set()), None)
  }

  def inlinedProtoNGrammar(): CodeBlob = {
    val grammarProto = GrammarProtobufConverter.convertNGrammarToProto(analysis.ngrammar)
    val grammarProtoBase64 = Base64.getEncoder.encodeToString(grammarProto.toByteArray)

    CodeBlob(
      s"""GrammarProtobufConverter.convertProtoToNGrammar(GrammarProto.NGrammar.parseFrom(Base64.getDecoder.decode(
         |  \"$grammarProtoBase64\")))""".stripMargin,
      Set("com.giyeok.jparser.proto.GrammarProto",
        "com.giyeok.jparser.proto.GrammarProtobufConverter",
        "java.util.Base64"))
  }

  def fileProtoGrammar(filePath: String): CodeBlob = CodeBlob(
    s"""Using(new FileInputStream(s"${escapeString(filePath)}")) { inputStream =>
       |  GrammarProtobufConverter.convertProtoToNGrammar(GrammarProto.NGrammar.parseFrom(inputStream))
       |}.get""".stripMargin,
    Set("java.io.FileInputStream", "scala.util.Using",
      "com.giyeok.jparser.proto.GrammarProto",
      "com.giyeok.jparser.proto.GrammarProtobufConverter"))

  def naiveParserDef(): CodeBlob = {
    val startType = typeDescStringOf(analysis.nonterminalTypes(analysis.startNonterminalName), context = Some(s"nonterminal type of ${analysis.startNonterminalName}"))

    CodeBlob(
      s"""val naiveParser = new NaiveParser(ngrammar)
         |
         |def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
         |  naiveParser.parse(text)
         |
         |def parseAst(text: String): Either[${startType.code}, ParsingErrors.ParsingError] =
         |  ParseTreeUtil.parseAst(naiveParser, text, matchStart)
         |""".stripMargin,
      Set("com.giyeok.jparser.nparser.ParseTreeUtil",
        "com.giyeok.jparser.nparser.NaiveParser",
        "com.giyeok.jparser.nparser.Parser",
        "com.giyeok.jparser.ParsingErrors")
    )
  }

  def mainFuncBlob(examples: List[String]): CodeBlob = CodeBlob(
    s"""def main(args: Array[String]): Unit = {
       |  ${examples.map(example => "    println(parseAst(" + escapeString(example) + "))").mkString("\n")}
       |}""",
    Set()
  )
}
