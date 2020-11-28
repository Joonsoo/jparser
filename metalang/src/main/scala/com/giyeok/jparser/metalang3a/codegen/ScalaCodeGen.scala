package com.giyeok.jparser.metalang3a.codegen

import com.giyeok.jparser.Inputs.CharsGrouping
import com.giyeok.jparser.NGrammar.{NNonterminal, NStart}
import com.giyeok.jparser.ParseResultTree.{BindNode, Node, SequenceNode}
import com.giyeok.jparser.metalang2.ScalaDefGenerator.javaChar
import com.giyeok.jparser.metalang3a.MetaLanguage3.{ProcessedGrammar, check}
import com.giyeok.jparser.metalang3a.Type.{ArrayOf, StringType}
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGen.{CodeBlob, ExprBlob, Options}
import com.giyeok.jparser.metalang3a.{ClassHierarchyTree, Type, ValuefyExpr}
import com.giyeok.jparser.{NGrammar, Symbols}

object ScalaCodeGen {

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

  case class CodeBlob(code: String, required: Set[String]) {
    def indent(width: Int = 2): CodeBlob =
      copy(code.linesIterator.toList.map(line => (" " * width) + line).mkString("\n"))

    def +(other: CodeBlob): CodeBlob = CodeBlob(code + "\n" + other.code, required ++ other.required)

    def wrap(pre: CodeBlob, post: CodeBlob, requiredImports: Set[String]): CodeBlob =
      CodeBlob(pre.code + "\n" + indent().code + "\n" + post.code,
        requiredImports ++ pre.required ++ post.required)

    def wrap(pre: String, post: String): CodeBlob = copy(pre + "\n" + indent().code + "\n" + post)

    def wrapBrace(): CodeBlob = wrap(CodeBlob("{", Set()), CodeBlob("}", Set()), Set())
  }

  case class ExprBlob(prepares: List[String], result: String, required: Set[String]) {
    def preparesCode = prepares.mkString("\n")

    def withBraceIfNeeded: String =
      if (prepares.isEmpty) result else "{\n" + prepares.map("  " + _).mkString("\n") + "\n}"
  }

  object ExprBlob {
    def code(code: String) = ExprBlob(List(), code, Set())
  }

}

class ScalaCodeGen(val analysis: ProcessedGrammar, val options: Options = Options()) {
  private def escapeString(str: String): String = s""""$str""""

  private def escapeChar(c: Char): String = s"'${javaChar(c)}'"

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
        "com.giyeok.jparser.ParseForestFunc",
        "com.giyeok.jparser.nparser.Parser",
        "com.giyeok.jparser.nparser.NaiveParser",
        "com.giyeok.jparser.nparser.ParseTreeConstructor"))
  }

  def classDefs(classHierarchy: ClassHierarchyTree): CodeBlob = {
    if (classHierarchy.allTypes.isEmpty) CodeBlob("", Set()) else
      classHierarchy.allTypes.values.map { cls =>
        if (cls.subclasses.isEmpty) {
          // concrete type
          val params = analysis.classParamTypes.getOrElse(cls.className, List()).map { param =>
            val paramType = typeDescStringOf(param._2)
            CodeBlob(s"${param._1}: ${paramType.code}", paramType.required)
          }
          val supers = if (cls.superclasses.isEmpty) "" else s" extends ${cls.superclasses.mkString(" with ")}"
          CodeBlob(s"case class ${cls.className}(${params.map(_.code).mkString(", ")})$supers",
            params.flatMap(_.required).toSet)
        } else {
          // abstract type
          val supers = if (cls.superclasses.isEmpty) "" else s" extends ${cls.superclasses.mkString(" with ")}"
          CodeBlob(s"sealed trait ${cls.className}$supers", Set())
        }
      }.reduce(_ + _)
  }

  private def typeDescStringOf(typ: Type, context: Option[String] = None): CodeBlob = typ match {
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
    case Type.NullType => CodeBlob("Nothing", Set())
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
    val returnType = typeDescStringOf(analysis.nonterminalTypes(analysis.startNonterminalName))
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
    val body = valuefyExprToCodeWithCoercion(valuefyExpr, "node", analysis.nonterminalTypes(nonterminal))
    val returnType = typeDescStringOf(analysis.nonterminalTypes(nonterminal), Some(s"return type of nonterminal $nonterminal"))
    val bodyCode = if (body.prepares.isEmpty) {
      CodeBlob(body.result, body.required)
    } else {
      CodeBlob(body.preparesCode + "\n" + body.result, body.required).wrapBrace()
    }
    CodeBlob(s"def ${nonterminalMatchFuncName(nonterminal)}(node: Node): ${returnType.code} = " + bodyCode.code,
      body.required ++ returnType.required ++ bodyCode.required + "com.giyeok.jparser.ParseResultTree.Node")
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
      // TODO
      ExprBlob(List(), s"$valuefyExpr", Set())
    case ValuefyExpr.JoinCond(condProcessor) =>
      // TODO
      ExprBlob(List(), s"$valuefyExpr", Set())
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
        elemProcessorCode.required + "com.giyeok.jparser.nparser.RepeatUtils.unrollRepeat0")
    case ValuefyExpr.UnrollRepeatFromOne(elemProcessor) =>
      val unrolledVar = newVar()
      val elemProcessorCode = valuefyExprToCode(elemProcessor, "elem")
      ExprBlob(
        List(s"val $unrolledVar = unrollRepeat1($inputName).map { elem =>") ++
          elemProcessorCode.prepares :+
          addCoercion(elemProcessorCode.result, typeOf(valuefyExpr).asInstanceOf[Type.ArrayOf].elemType, typeOf(elemProcessor)) :+
          "}",
        unrolledVar,
        elemProcessorCode.required + "com.giyeok.jparser.nparser.RepeatUtils.unrollRepeat1")
    case ValuefyExpr.UnrollChoices(choices) =>
      val bindedVar = newVar()
      val bodyVar = newVar()
      val requiredType = typeOf(valuefyExpr)
      val casesExpr = choices.map { choice =>
        val choiceId = analysis.ngrammar.idOf(choice._1)
        val bodyCode = valuefyExprToCodeWithCoercion(choice._2, bodyVar, requiredType)
        val caseBody =
          if (bodyCode.prepares.isEmpty) bodyCode.result
          else (bodyCode.prepares :+ bodyCode.result).map("  " + _) mkString ("\n")
        CodeBlob(s"case $choiceId =>\n$caseBody", bodyCode.required)
      }.toList.reduce(_ + _)
      ExprBlob(List(
        s"val BindNode($bindedVar, $bodyVar) = $inputName"
      ), casesExpr.wrap(s"$bindedVar.id match {", "}").code, casesExpr.required)
    case ValuefyExpr.ConstructCall(className, params) =>
      val paramCodes = params.zipWithIndex.map { case (param, index) =>
        valuefyExprToCodeWithCoercion(param, inputName, analysis.classParamTypes(className)(index)._2)
      }
      ExprBlob(paramCodes.flatMap(_.prepares),
        s"$className(${paramCodes.map(_.result).mkString(", ")})",
        paramCodes.flatMap(_.required).toSet)
    case ValuefyExpr.FuncCall(funcType, params) =>
      // TODO coercion
      funcType match {
        case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.IsPresent =>
          val paramCodes = params.map(valuefyExprToCode(_, inputName))
          // TODO 제대로 다시 구현
          ExprBlob(paramCodes.flatMap(_.prepares),
            paramCodes.map(_.result + ".toString").mkString(" + "),
            paramCodes.flatMap(_.required).toSet)
        case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.IsEmpty => ???
        case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.Chr =>
          check(params.size == 1, "chr function only can have exactly one parameter")
          val paramCode = valuefyExprToCode(params.head, inputName)
          ExprBlob(paramCode.prepares, paramCode.result + ".toString.charAt(0)", paramCode.required)
        case com.giyeok.jparser.metalang3a.ValuefyExpr.FuncType.Str =>
          val paramCodes = params.map(valuefyExprToCode(_, inputName))
          ExprBlob(paramCodes.flatMap(_.prepares),
            paramCodes.map(_.result + ".toString").mkString(" + "),
            paramCodes.flatMap(_.required).toSet)
      }
    case ValuefyExpr.ArrayExpr(elems) =>
      val elemType = typeOf(valuefyExpr).asInstanceOf[Type.ArrayOf].elemType
      val elemCodes = elems.map(valuefyExprToCodeWithCoercion(_, inputName, elemType))
      ExprBlob(elemCodes.flatMap(_.prepares),
        s"List(${elemCodes.map(_.result).mkString(", ")})",
        elemCodes.flatMap(_.required).toSet)
    case ValuefyExpr.BinOp(op, lhs, rhs) =>
      // TODO coercion
      val lhsCode = valuefyExprToCode(lhs, inputName)
      val rhsCode = valuefyExprToCode(rhs, inputName)
      val lhsType = analysis.typeInferer.typeOfValuefyExpr(lhs).get
      val rhsType = analysis.typeInferer.typeOfValuefyExpr(rhs).get
      val opExpr = op match {
        case com.giyeok.jparser.metalang3a.ValuefyExpr.BinOpType.ADD =>
          (lhsType, rhsType) match {
            case (StringType, StringType) => s"${lhsCode.result} + ${rhsCode.result}"
            case (ArrayOf(_), ArrayOf(_)) => s"${lhsCode.result} ++ ${rhsCode.result}"
          }
        case com.giyeok.jparser.metalang3a.ValuefyExpr.BinOpType.EQ => ???
        case com.giyeok.jparser.metalang3a.ValuefyExpr.BinOpType.NE => ???
        case com.giyeok.jparser.metalang3a.ValuefyExpr.BinOpType.BOOL_AND => ???
        case com.giyeok.jparser.metalang3a.ValuefyExpr.BinOpType.BOOL_OR => ???
      }
      ExprBlob(lhsCode.prepares ++ rhsCode.prepares, opExpr, lhsCode.required ++ rhsCode.required)
    case ValuefyExpr.PreOp(op, expr) =>
      // TODO
      ExprBlob(List(), s"$valuefyExpr", Set())
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
      // TODO
      ExprBlob(List(), s"$valuefyExpr", Set())
    case literal: ValuefyExpr.Literal =>
      literal match {
        case ValuefyExpr.NullLiteral => ExprBlob.code("None")
        case ValuefyExpr.BoolLiteral(value) => ExprBlob.code(s"$value")
        case ValuefyExpr.CharLiteral(value) => ExprBlob.code(s"'$value'") // TODO escape
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

  def generateParser(className: String, mainFuncExamples: Option[List[String]] = None): String = {
    val ngrammarDefCode = ngrammarDef(analysis.ngrammar)
    val classDefsCode = classDefs(analysis.classRelations.toHierarchy)
    val enumDefsCode = enumDefs(analysis.enumValuesMap)
    // TODO nontermMatchCodes에서 AST에서 쓰이지 않는 nonterminal은 제외
    val nontermMatchCodes = analysis.nonterminalTypes.keys.map(nonterminalMatchFunc).toList
    val startMatchCode = matchStartFunc()
    val startType = typeDescStringOf(analysis.nonterminalTypes(analysis.startNonterminalName))

    val allImports = (ngrammarDefCode.required ++
      classDefsCode.required ++
      enumDefsCode.required ++
      nontermMatchCodes.flatMap(_.required) ++
      startMatchCode.required ++
      startType.required).toList.sorted
    val mainFunc = mainFuncExamples match {
      case Some(examples) =>
        "\n\n  def main(args: Array[String]): Unit = {\n" +
          examples.map(example => "    println(parseAst(\"" + example + "\"))").mkString("\n") +
          "\n  }"
      case None => ""
    }
    s"""${allImports.map(pkg => s"import $pkg").mkString("\n")}
       |
       |object $className {
       |  val ngrammar = ${ngrammarDefCode.indent().code}
       |
       |${classDefsCode.indent().code}
       |${enumDefsCode.indent().code}
       |
       |${nontermMatchCodes.map(_.indent().code).mkString("\n\n")}
       |
       |${startMatchCode.indent().code}
       |
       |  val naiveParser = new NaiveParser(ngrammar)
       |
       |  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
       |    naiveParser.parse(text)
       |
       |  def parseAst(text: String): Either[${startType.code}, ParsingErrors.ParsingError] = parse(text) match {
       |    case Left(ctx) =>
       |      val tree = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
       |      tree match {
       |        case Some(forest) if forest.trees.size == 1 => Left(matchStart(forest.trees.head))
       |        case Some(forest) => Right(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size))
       |        case None => ???
       |      }
       |    case Right(error) => Right(error)
       |  }$mainFunc
       |}
       |""".stripMargin
  }
}
