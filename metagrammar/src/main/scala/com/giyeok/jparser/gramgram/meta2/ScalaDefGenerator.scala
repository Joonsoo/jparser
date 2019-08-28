package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.Inputs.CharsGrouping
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.gramgram.meta2.Analyzer.Analysis
import com.giyeok.jparser.gramgram.meta2.TypeDependenceGraph.SymbolNode
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar.{NStart, NSymbol}

class ScalaDefGenerator(val analysis: Analysis) {
    // TODO
    private def escapeString(str: String): String = s""""$str""""

    private def escapeChar(c: Char): String = {
        // TODO
        val escaped = c
        s"""'$escaped'"""
    }

    private def symbolString(symbol: Symbols.Symbol): String = symbol match {
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
        case Symbols.Sequence(seq, _) => s"Symbols.Sequence(Seq(${seq map symbolString mkString ","}))"
    }

    def grammarDefBody(grammarName: String): String = {
        val g = analysis.grammar(grammarName)

        val ruleString = g.rules.toList.map { r =>
            s"""${escapeString(r._1)} -> ListSet(
               |  ${r._2 map symbolString mkString ",\n  "}
               |)""".stripMargin
        }
        s"""  val name: String = ${escapeString(grammarName)}
           |  val startSymbol: Symbols.Nonterminal = Symbols.Nonterminal(${escapeString(g.startSymbol.name)})
           |  val rules: RuleMap = ListMap(
           |    ${ruleString mkString ",\n"}
           |  )""".stripMargin
    }

    def grammarDef(grammarName: String, includeAstifiers: Boolean): String = {
        s"""new Grammar {
           |  ${grammarDefBody(grammarName)}
           |}""".stripMargin
    }

    private def intSetString(s: Set[Int]) = s"Set(${s mkString ","})"

    private def intSeqString(s: Seq[Int]) = s"Seq(${s mkString ","})"

    private def nAtomicSymbolString(nsymbol: NGrammar.NAtomicSymbol): String = nsymbol match {
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

    private def nSequenceString(nseq: NGrammar.NSequence): String =
        s"NGrammar.NSequence(${nseq.id}, ${symbolString(nseq.symbol)}, ${intSeqString(nseq.sequence)})"

    def ngrammarDef(): String = {
        val nsymbolsString = analysis.ngrammar.nsymbols.toList.sortBy(_._1) map { s =>
            s"${s._1} -> ${nAtomicSymbolString(s._2)}"
        }
        val nseqsString = analysis.ngrammar.nsequences.toList.sortBy(_._1) map { s =>
            s"${s._1} -> ${nSequenceString(s._2)}"
        }
        s"""new NGrammar(
           |  Map(${nsymbolsString mkString ",\n"}),
           |  Map(${nseqsString mkString ",\n"}),
           |  ${analysis.ngrammar.startSymbol})""".stripMargin
    }

    def typeSpecToString(typeSpec: TypeSpec): String = typeSpec match {
        case ParseNodeType => "Node"
        case ClassType(className) => className
        case ArrayType(elemType) =>
            s"List[${typeSpecToString(elemType)}]"
        case OptionalType(valueType) =>
            s"Option[${typeSpecToString(valueType)}]"
        case _: UnionType | _: UnionNodeType | _: ArrayConcatNodeType =>
            throw new Exception(s"Union type is not supported: $typeSpec")
    }

    def classDefsList(): List[String] = analysis.classDefs map { d =>
        val supers = if (d.supers.isEmpty) "" else {
            s" extends ${d.supers mkString " with "}"
        }
        if (d.isAbstract) {
            s"sealed trait ${d.name}$supers"
        } else {
            val params = d.params map { p =>
                s"${p.name}:${typeSpecToString(p.typ)}"
            } mkString ", "
            s"case class ${d.name}($params)$supers"
        }
    }

    case class CodeBlock(code: String, requirements: Set[String]) {
        def append(codeBlock: CodeBlock): CodeBlock =
            CodeBlock(code + "\n" + codeBlock.code, requirements ++ codeBlock.requirements)

        def addRequirement(requirement: String): CodeBlock = CodeBlock(code, requirements + requirement)

        def removeRequirement(requirement: String): CodeBlock = CodeBlock(code, requirements - requirement)
    }

    def classDefs(): CodeBlock = CodeBlock(classDefsList() mkString "\n", Set())

    private var _argNum = 0

    private def nextArgName(): String = {
        _argNum += 1
        "v" + _argNum
    }

    case class GenAstifierString(prepare: List[String], result: String, requirements: Set[String])

    private def astifierString(expr: AstifierExpr, argName: String): GenAstifierString = expr match {
        case ThisNode => GenAstifierString(List(), argName, Set())
        case Unbinder(expr, symbol) =>
            // TODO symbol 타입에 맞춰서 match** 함수 호출하는 경우 처리
            val bindedSymbolId = analysis.ngrammar.idOf(symbol)
            val e = astifierString(expr, argName)
            if (symbol.isInstanceOf[Symbols.Nonterminal]) {
                val v1 = nextArgName()
                val v2 = nextArgName()
                val v3 = nextArgName()
                val matchFunc = matchFuncName(symbol)
                GenAstifierString(e.prepare ++ List(
                    s"val BindNode($v1, $v2) = ${e.result}",
                    s"assert($v1.id == $bindedSymbolId)",
                    s"val $v3 = $matchFunc($v2)"),
                    v3, e.requirements)
            } else {
                val v1 = nextArgName()
                val v2 = nextArgName()
                GenAstifierString(e.prepare ++ List(
                    s"val BindNode($v1, $v2) = ${e.result}",
                    s"assert($v1.id == $bindedSymbolId)"),
                    v2, e.requirements)
            }
        case SeqRef(expr, idx) =>
            val e = astifierString(expr, argName)
            val v = nextArgName()
            GenAstifierString(e.prepare :+ s"val $v = ${e.result}.asInstanceOf[SequenceNode].children($idx)", v, e.requirements)
        case UnrollMapper(boundType, referrer, target, mapFn) =>
            // TODO boundType에 따라서 unrollRepeat0를 다른 함수로 바꿔야 함
            val referrerAstifier = astifierString(referrer, argName)
            val targetAstifier = astifierString(target, "n")
            val mapFnAstifier = astifierString(mapFn, targetAstifier.result)
            val requiredFeatures = referrerAstifier.requirements ++ targetAstifier.requirements ++ mapFnAstifier.requirements
            val v = nextArgName()
            boundType match {
                case BoundType.Sequence =>
                    ???
                case BoundType.Choice =>
                    ???
                case BoundType.Repeat0 =>
                    GenAstifierString(referrerAstifier.prepare ++
                        List(s"val $v = unrollRepeat0(${referrerAstifier.result}) map { n =>") ++
                        targetAstifier.prepare ++
                        mapFnAstifier.prepare ++
                        List(mapFnAstifier.result, "}"),
                        v, requiredFeatures + "unrollRepeat0")
                case BoundType.Repeat1 =>
                    GenAstifierString(referrerAstifier.prepare ++
                        List(s"val $v = unrollRepeat1(${referrerAstifier.result}) map { n =>") ++
                        targetAstifier.prepare ++
                        mapFnAstifier.prepare ++
                        List(mapFnAstifier.result, "}"),
                        v, requiredFeatures + "unrollRepeat1")
                case BoundType.Optional =>
                    ???
                case BoundType.Paren =>
                    ???
                case BoundType.Longest =>
                    ???
            }
        case UnrollChoices(choiceSymbols) =>
            // TODO
            ???
        case CreateObj(className, args) =>
            val argStrings = args map (astifierString(_, argName))
            val v = nextArgName()
            GenAstifierString(
                (argStrings flatMap (_.prepare)) :+
                    s"val $v = $className(${argStrings map (_.result) mkString ","})",
                v, argStrings.flatMap(_.requirements).toSet)
        case CreateList(elems) =>
            val elemStrings = elems map (astifierString(_, argName))
            val v = nextArgName()
            GenAstifierString(
                (elemStrings flatMap (_.prepare)) :+
                    s"val $v = List(${elemStrings map (_.result) mkString ","})",
                v, elemStrings.flatMap(_.requirements).toSet)
        case ConcatList(lhs, rhs) =>
            val ln = astifierString(lhs, argName)
            val rn = astifierString(rhs, argName)
            val v = nextArgName()
            GenAstifierString((ln.prepare ++ rn.prepare) :+
                s"val $v = ${ln.result} ++ ${rn.result}",
                v, ln.requirements ++ rn.requirements)
    }

    private def matchFuncName(symbol: Symbols.Symbol): String = symbol match {
        case Symbols.Nonterminal(nonterm) =>
            s"match${nonterm.substring(0, 1).toUpperCase}${nonterm.substring(1)}"
    }

    def astifierDefs(): CodeBlock = {
        val astifierStrings = analysis.astifiers map { a =>
            val (lhs, rhs) = a
            val lhsNonterm = Symbols.Nonterminal(lhs)
            val funcName = matchFuncName(lhsNonterm)

            val returnType = analysis.typeDependenceGraph.inferType(SymbolNode(lhsNonterm))
            val returnTypeSpec = returnType.fixedType.getOrElse(returnType.inferredTypes.head)

            val rr = rhs map { r =>
                // TODO unreachable symbol이 있으면 없는 심볼에 대해서 ngrammar.idOf 실행해서 오류 생길 수 있음
                val symbolId = analysis.ngrammar.idOf(r._1)
                val gen = astifierString(r._2, "body")
                symbolId -> (((gen.prepare :+ gen.result) mkString "\n"), gen.requirements)
            }
            CodeBlock(
                s"""def $funcName(node: Node): ${typeSpecToString(returnTypeSpec)} = {
                   |  val BindNode(symbol, body) = node
                   |  symbol.id match {
                   |    ${rr map { c => s"case ${c._1} =>\n${c._2._1}" } mkString "\n"}
                   |  }
                   |}""".stripMargin, (rr flatMap (_._2._2)).toSet)
        }
        astifierStrings.foldLeft(CodeBlock("", Set("jparser.ParseResultTree.Node", "jparser.ParseResultTree.BindNode"))) {
            _.append(_)
        }
    }

    private def startSymbol(): NSymbol =
        analysis.ngrammar.symbolOf(analysis.ngrammar.nsymbols(analysis.ngrammar.startSymbol).asInstanceOf[NStart].produce)

    def sourceTextOf(): CodeBlock = CodeBlock(
        s"""def sourceTextOf(node: Node): String = node match {
           |  case TerminalNode(input) => input.toRawString
           |  case BindNode(_, body) => sourceTextOf(body)
           |  case JoinNode(body, _) => sourceTextOf(body)
           |  case seq: SequenceNode => seq.children map sourceTextOf mkString ""
           |  case _ => throw new Exception("Cyclic bind")
           |}""".stripMargin,
        Set("jparser.ParseResultTree.Node", "jparser.ParseResultTree.TerminalNode", "jparser.ParseResultTree.BindNode",
            "jparser.ParseResultTree.JoinNode", "jparser.ParseResultTree.SequenceNode", "jparser.Inputs.InputToShortString"))

    def unrollRepeat0(): CodeBlock = CodeBlock(
        s"""private def unrollRepeat0(node: Node): List[Node] = {
           |  val BindNode(repeat: NGrammar.NRepeat, body) = node
           |  body match {
           |    case BindNode(symbol, repeating: SequenceNode) =>
           |      assert(symbol.id == repeat.repeatSeq)
           |      val s = repeating.children(1)
           |      val r = unrollRepeat0(repeating.children(0))
           |      r :+ s
           |    case SequenceNode(symbol, emptySeq) =>
           |      assert(symbol.id == repeat.baseSeq)
           |      assert(emptySeq.isEmpty)
           |      List()
           |  }
           |}""".stripMargin,
        Set("jparser.ParseResultTree.Node", "jparser.ParseResultTree.BindNode", "jparser.ParseResultTree.SequenceNode",
            "jparser.nparser.NGrammar"))

    def matchStart(): CodeBlock = {
        val funcName = matchFuncName(startSymbol().symbol)

        val returnType = analysis.typeDependenceGraph.inferType(SymbolNode(startSymbol().symbol))
        val returnTypeSpec = returnType.fixedType.getOrElse(returnType.inferredTypes.head)

        CodeBlock(
            s"""def matchStart(node: Node): ${typeSpecToString(returnTypeSpec)} = {
               |  val BindNode(start, BindNode(startNonterm, body)) = node
               |  assert(start.id == ${analysis.ngrammar.startSymbol})
               |  assert(startNonterm.id == ${startSymbol().id})
               |  $funcName(body)
               |}""".stripMargin,
            Set("jparser.ParseResultTree.Node", "jparser.ParseResultTree.BindNode"))
    }

    def parseUtilFuncs(): CodeBlock = {
        val startAstType = analysis.typeDependenceGraph.inferType(SymbolNode(startSymbol().symbol))
        val startAstTypeSpec = startAstType.fixedType.getOrElse(startAstType.inferredTypes.head)

        CodeBlock(
            s"""lazy val naiveParser = new NaiveParser(ngrammar)
               |
               |def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
               |  naiveParser.parse(text)
               |
               |def parseAst(text: String): Either[${typeSpecToString(startAstTypeSpec)}, ParsingErrors.ParsingError] =
               |  parse(text) match {
               |    case Left(ctx) =>
               |      val tree = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
               |      tree match {
               |        case Some(forest) if forest.trees.size == 1 =>
               |          Left(matchStart(forest.trees.head))
               |        case Some(forest) =>
               |          Right(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size))
               |        case None => ???
               |      }
               |    case Right(error) => Right(error)
               |  }""".stripMargin,
            Set("jparser.nparser.NaiveParser", "jparser.nparser.Parser", "jparser.ParsingErrors",
                "jparser.ParseForestFunc", "jparser.nparser.ParseTreeConstructor"))
    }

    def toGrammarObject(objectName: String, parseUtils: Boolean = false): String = {
        var codeBlock = classDefs().append(sourceTextOf()).append(astifierDefs()).append(matchStart())
        if (parseUtils) codeBlock = codeBlock.addRequirement("parseUtilFuncs")

        var imports = Set[String]("jparser.nparser.NGrammar", "jparser.Symbols")
        while (codeBlock.requirements.nonEmpty) {
            // TODO 실행 순서가 deterministic하지 않을 수 있음
            val requirement = codeBlock.requirements.head
            requirement match {
                case "unrollRepeat0" => codeBlock = codeBlock.append(unrollRepeat0())
                case "parseUtilFuncs" => codeBlock = codeBlock.append(parseUtilFuncs())
                // TODO
                case imprt if requirement.startsWith("jparser.") => imports += imprt
            }
            codeBlock = codeBlock.removeRequirement(requirement)
        }

        val importsByParent = imports map { i => i.splitAt(i.lastIndexOf('.')) } groupBy (_._1)
        val importLines = (importsByParent.values map { i =>
            val lastTokens = i map (_._2.substring(1))
            if (lastTokens.size == 1) s"import com.giyeok.${i.head._1}.${lastTokens.head}"
            else s"import com.giyeok.${i.head._1}.{${lastTokens mkString ", "}}"
        }).toList

        s"""${importLines.sorted mkString "\n"}
           |
           |object $objectName {
           |  val ngrammar = ${ngrammarDef()}
           |
           |  ${codeBlock.code}
           |}""".stripMargin
    }
}
