package com.giyeok.jparser.metalang2

import com.giyeok.jparser.Inputs.CharsGrouping
import com.giyeok.jparser.NGrammar.{NStart, NSymbol}
import com.giyeok.jparser.metalang2.TypeDependenceGraph.SymbolNode
import com.giyeok.jparser.{Grammar, NGrammar, Symbols}

object ScalaDefGenerator {
    private def escapeString(str: String): String = s""""$str""""

    def isPrintableChar(char: Char): Boolean = {
        val block = Character.UnicodeBlock.of(char)
        (!Character.isISOControl(char)) && block != null && block != Character.UnicodeBlock.SPECIALS
    }

    def javaChar(char: Char): String = char match {
        case '\n' => "\\n"
        case '\r' => "\\r"
        case '\t' => "\\t"
        case '\\' => "\\\\"
        case '\'' => "\\'"
        case c if !isPrintableChar(c) && c.toInt < 65536 =>
            val c1 = (c.toInt >> 8) % 256
            val c2 = c.toInt % 256
            val hexChars = "0123456789abcdef"
            s"\\u${hexChars(c1 >> 4)}${hexChars(c1 & 15)}${hexChars(c2 >> 4)}${hexChars(c2 & 15)}"
        case c => c.toString
    }

    private def escapeChar(c: Char): String = s"'${javaChar(c)}'"

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
        case Symbols.Sequence(seq) => s"Symbols.Sequence(Seq(${seq map symbolString mkString ","}))"
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

    def ngrammarDef(ngrammar: NGrammar): String = {
        val nsymbolsString = ngrammar.nsymbols.toList.sortBy(_._1) flatMap { s =>
            List( // s"// ${s._2.symbol.toShortString}",
                s"${s._1} -> ${nAtomicSymbolString(s._2)}")
        }
        val nseqsString = ngrammar.nsequences.toList.sortBy(_._1) flatMap { s =>
            List( // s"// ${s._2.symbol.toShortString}",
                s"${s._1} -> ${nSequenceString(s._2)}")
        }
        s"""new NGrammar(
           |  Map(${nsymbolsString mkString ",\n"}),
           |  Map(${nseqsString mkString ",\n"}),
           |  ${ngrammar.startSymbol})""".stripMargin
    }

    private def grammarDefBody(g: Grammar): String = {
        val ruleString = g.rules.toList.map { r =>
            s"""${escapeString(r._1)} -> ListSet(
               |  ${r._2 map symbolString mkString ",\n  "}
               |)""".stripMargin
        }
        s"""  val name: String = ${escapeString(g.name)}
           |  val startSymbol: Symbols.Nonterminal = Symbols.Nonterminal(${escapeString(g.startSymbol.name)})
           |  val rules: RuleMap = ListMap(
           |    ${ruleString mkString ",\n"}
           |  )""".stripMargin
    }

    def grammarDef(grammar: Grammar): String = {
        s"""new Grammar {
           |  ${ScalaDefGenerator.grammarDefBody(grammar)}
           |}""".stripMargin
    }
}

class ScalaDefGenerator(analysis: MetaLanguage2.Analysis, astPrettyPrinter: Boolean = true, parseUtils: Boolean = false) {
    private val astAnalysis = analysis.astAnalysis
    private val typeAnalysis = analysis.typeAnalysis

    def typeSpecToString(typeSpec: TypeSpec): String = typeSpec match {
        case ParseNodeType => "Node"
        case ClassType(className) => className
        case ArrayType(elemType) =>
            s"List[${typeSpecToString(elemType)}]"
        case OptionalType(valueType) =>
            s"Option[${typeSpecToString(valueType)}]"
        //        case UnionType(types) =>
        //            s"(${types.map(typeSpecToString).mkString("|")})"
        case _: UnionType =>
            throw new Exception(s"Union type is not supported: $typeSpec")
    }

    def classDefsList(rootClassName: String, astNodeParamName: String): List[String] = typeAnalysis.classDefs map { d =>
        val supers = s" extends ${(rootClassName +: d.supers) mkString " with "}"
        if (d.isAbstract) {
            s"sealed trait ${d.name}$supers"
        } else {
            val params = (ClassParam(astNodeParamName, ParseNodeType) +: d.params) map { p =>
                s"${p.name}:${typeSpecToString(p.typ)}"
            } mkString ", "

            val classBody: String = if (!astPrettyPrinter) "" else {
                def classParamPrettyPrint(name: String, typ: TypeSpec): String = typ match {
                    case ParseNodeType => name + ".sourceText + " + name + ".range"
                    case ClassType(_) => s"$name.prettyPrint()"
                    case ArrayType(elemType) =>
                        s""""[" + $name.map(e => ${classParamPrettyPrint("e", elemType)}).mkString(",") + "]""""
                    case OptionalType(valueType) =>
                        s"""($name match { case Some(v) =>
                           |  ${classParamPrettyPrint("v", valueType)}
                           |  case None => "null"
                           |})""".stripMargin
                    case _: UnionType => throw new Exception(s"Union type is not supported: $typ")
                }

                val paramsPrettyPrint = d.params.map { p =>
                    s""""${p.name}=" + ${classParamPrettyPrint(p.name, p.typ)}"""
                }
                s"""{
                   |  def prettyPrint(): String = s"${d.name}(" + ${paramsPrettyPrint.mkString("+ \", \" + ")} + ")"
                   |}""".stripMargin
            }
            s"case class ${d.name}($params)$supers$classBody"
        }
    }

    case class CodeBlock(code: String, requirements: Set[String]) {
        def append(codeBlock: CodeBlock): CodeBlock =
            CodeBlock(code + "\n" + codeBlock.code, requirements ++ codeBlock.requirements)

        def addRequirement(requirement: String): CodeBlock = CodeBlock(code, requirements + requirement)

        def removeRequirement(requirement: String): CodeBlock = CodeBlock(code, requirements - requirement)
    }

    def classDefs(): CodeBlock = CodeBlock(
        s"""sealed trait ASTNode {
           |  val astNode: ${typeSpecToString(ParseNodeType)}
           |  ${if (astPrettyPrinter) "def prettyPrint(): String" else ""}
           |}""".stripMargin, Set())
        .append(CodeBlock(classDefsList("ASTNode", "astNode") mkString "\n", Set()))

    private var _argNum = 0

    private def nextArgName(): String = {
        _argNum += 1
        "v" + _argNum
    }

    case class GenAstifierString(prepare: List[String], result: String, requirements: Set[String])

    private def astifierString(expr: AstifierExpr, argName: String): GenAstifierString = expr match {
        case ThisNode => GenAstifierString(List(), argName, Set())
        case Unbind(expr, symbol) =>
            // TODO symbol 타입에 맞춰서 match** 함수 호출하는 경우 처리
            val bindedSymbolId = astAnalysis.ngrammar.idOf(symbol)
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
        case UnrollRepeat(lower, source, eachAstifier) =>
            val r = astifierString(source, argName)
            val e = astifierString(eachAstifier, "n")
            val v = nextArgName()
            lower match {
                case 0 =>
                    GenAstifierString(r.prepare ++ List(
                        s"val $v = unrollRepeat0(${r.result}) map { n =>") ++
                        e.prepare ++
                        List(e.result, "}"),
                        v, r.requirements + "unrollRepeat0")
                case 1 =>
                    GenAstifierString(r.prepare ++ List(
                        s"val $v = unrollRepeat1(${r.result}) map { n =>") ++
                        e.prepare ++
                        List(e.result, "}"),
                        v, r.requirements + "unrollRepeat1")
            }
        case UnrollOptional(source, contentAstifier, emptySym, contentSym) =>
            val r = astifierString(source, argName)
            val c = astifierString(contentAstifier, "n")
            val v = nextArgName()
            GenAstifierString(r.prepare ++ List(
                s"val $v = unrollOptional(${r.result}, ${astAnalysis.ngrammar.idOf(emptySym)}, ${astAnalysis.ngrammar.idOf(contentSym)}) map { n =>") ++
                c.prepare ++
                List(c.result, "}"),
                v, r.requirements ++ c.requirements + "unrollOptional")
        case EachMap(target, mapFn) =>
            val targetAstifier = astifierString(target, argName)
            val mapFnAstifier = astifierString(mapFn, "n")
            val requiredFeatures = targetAstifier.requirements ++ mapFnAstifier.requirements
            val v = nextArgName()
            GenAstifierString(targetAstifier.prepare ++
                List(s"val $v = ${targetAstifier.result} map { n =>") ++
                mapFnAstifier.prepare ++
                List(mapFnAstifier.result, "}"),
                v, requiredFeatures)
        case UnrollChoices(choiceSymbols) =>
            // TODO
            GenAstifierString(List("// UnrollChoices"), argName, Set())
        case CreateObj(className, args) =>
            val argStrings = args map (astifierString(_, argName))
            val v = nextArgName()
            GenAstifierString(
                (argStrings flatMap (_.prepare)) :+
                    s"val $v = $className(${"node" +: (argStrings map (_.result)) mkString ","})",
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

    private def matchFuncSignature(symbol: Symbols.Symbol): (String, TypeSpec) = {
        val nodeType = typeAnalysis.typeDependenceGraph.inferType(TypeDependenceGraph.SymbolNode(symbol))
        val returnType = nodeType.fixedType.getOrElse {
            val types = typeAnalysis.typeHierarchyGraph.removeRedundantTypesFrom(nodeType.inferredTypes) map typeAnalysis.typeHierarchyGraph.cleanType
            if (types.size == 1) types.head else UnionType(types)
        }

        (matchFuncName(symbol), returnType)
    }

    def astifierDefs(): CodeBlock = {
        val astifierStrings = astAnalysis.astifiers map { a =>
            val (lhs, rhs) = a
            val lhsNonterm = Symbols.Nonterminal(lhs)
            val (funcName, returnType) = matchFuncSignature(lhsNonterm)

            val rr = rhs map { r =>
                // TODO unreachable symbol이 있으면 없는 심볼에 대해서 ngrammar.idOf 실행해서 오류 생길 수 있음
                val symbolId = astAnalysis.ngrammar.idOf(r._1)
                val gen = astifierString(r._2, "body")
                symbolId -> (((gen.prepare :+ gen.result) mkString "\n"), gen.requirements)
            }
            CodeBlock(
                s"""def $funcName(node: Node): ${typeSpecToString(returnType)} = {
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
        astAnalysis.ngrammar.symbolOf(astAnalysis.ngrammar.nsymbols(astAnalysis.ngrammar.startSymbol).asInstanceOf[NStart].produce)

    def sourceTextOf(): CodeBlock = CodeBlock(
        s"""implicit class SourceTextOfNode(node: Node) {
           |  def sourceText: String = node match {
           |    case TerminalNode(_, input) => input.toRawString
           |    case BindNode(_, body) => body.sourceText
           |    case JoinNode(_, body, _) => body.sourceText
           |    case seq: SequenceNode => seq.children map (_.sourceText) mkString ""
           |    case _ => throw new Exception("Cyclic bind")
           |  }
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
           |    case SequenceNode(_, _, symbol, emptySeq) =>
           |      assert(symbol.id == repeat.baseSeq)
           |      assert(emptySeq.isEmpty)
           |      List()
           |  }
           |}""".stripMargin,
        Set("jparser.ParseResultTree.Node", "jparser.ParseResultTree.BindNode", "jparser.ParseResultTree.SequenceNode",
            "jparser.NGrammar"))

    def unrollRepeat1(): CodeBlock = CodeBlock(
        s"""private def unrollRepeat1(node: Node): List[Node] = {
           |  val BindNode(repeat: NGrammar.NRepeat, body) = node
           |  body match {
           |    case BindNode(symbol, repeating: SequenceNode) if symbol.id == repeat.repeatSeq =>
           |      assert(symbol.id == repeat.repeatSeq)
           |      val s = repeating.children(1)
           |      val r = unrollRepeat1(repeating.children(0))
           |      r :+ s
           |    case base =>
           |      List(base)
           |  }
           |}""".stripMargin,
        Set("jparser.ParseResultTree.Node", "jparser.ParseResultTree.BindNode", "jparser.ParseResultTree.SequenceNode",
            "jparser.NGrammar"))

    def unrollOptional(): CodeBlock = CodeBlock(
        s"""private def unrollOptional(node: Node, emptyId: Int, contentId: Int): Option[Node] = {
           |  val BindNode(_: NGrammar.NOneOf, body@BindNode(bodySymbol, _)) = node
           |  if (bodySymbol.id == contentId) Some(body) else None
           |}""".stripMargin,
        Set("jparser.ParseResultTree.Node", "jparser.ParseResultTree.BindNode", "jparser.ParseResultTree.SequenceNode",
            "jparser.NGrammar"))

    def matchStart(): CodeBlock = {
        val (funcName, returnType) = matchFuncSignature(startSymbol().symbol)

        CodeBlock(
            s"""def matchStart(node: Node): ${typeSpecToString(returnType)} = {
               |  val BindNode(start, BindNode(startNonterm, body)) = node
               |  assert(start.id == ${astAnalysis.ngrammar.startSymbol})
               |  assert(startNonterm.id == ${startSymbol().id})
               |  $funcName(body)
               |}""".stripMargin,
            Set("jparser.ParseResultTree.Node", "jparser.ParseResultTree.BindNode"))
    }

    def parseUtilFuncs(): CodeBlock = {
        val startAstType = typeAnalysis.typeDependenceGraph.inferType(SymbolNode(startSymbol().symbol))
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
               |        case None =>
               |          val expectedTerms = ctx.nextGraph.nodes.flatMap { node =>
               |            node.kernel.symbol match {
               |              case NGrammar.NTerminal(_, term) => Some(term)
               |              case _ => None
               |            }
               |          }
               |          Right(ParsingErrors.UnexpectedEOF(expectedTerms, text.length))
               |      }
               |    case Right(error) => Right(error)
               |  }""".stripMargin,
            Set("jparser.nparser.NaiveParser", "jparser.nparser.Parser", "jparser.ParsingErrors",
                "jparser.ParseForestFunc", "jparser.nparser.ParseTreeConstructor", "jparser.NGrammar"))
    }

    def toGrammarObject(objectName: String): String = {
        _argNum = 0

        var codeBlock = classDefs().append(sourceTextOf()).append(astifierDefs()).append(matchStart())
        if (parseUtils) codeBlock = codeBlock.addRequirement("parseUtilFuncs")

        var imports = Set[String]("jparser.NGrammar", "jparser.Symbols", "scala.collection.immutable.ListSet")
        while (codeBlock.requirements.nonEmpty) {
            // TODO 실행 순서가 deterministic하지 않을 수 있음
            val requirement = codeBlock.requirements.head
            requirement match {
                case "unrollRepeat0" => codeBlock = codeBlock.append(unrollRepeat0())
                case "unrollRepeat1" => codeBlock = codeBlock.append(unrollRepeat1())
                case "unrollOptional" => codeBlock = codeBlock.append(unrollOptional())
                case "parseUtilFuncs" => codeBlock = codeBlock.append(parseUtilFuncs())
                // TODO
                case imprt if requirement.startsWith("jparser.") => imports += imprt
            }
            codeBlock = codeBlock.removeRequirement(requirement)
        }

        val importsByParent = imports map { i => i.splitAt(i.lastIndexOf('.')) } groupBy (_._1)
        val importLines = (importsByParent.values map { i =>
            if (i.head._1.startsWith("scala.")) {
                val lastTokens = i map (_._2.substring(1))
                if (lastTokens.size == 1) s"import ${i.head._1}.${lastTokens.head}"
                else s"import ${i.head._1}.{${lastTokens mkString ", "}}"
            } else {
                val lastTokens = i map (_._2.substring(1))
                if (lastTokens.size == 1) s"import com.giyeok.${i.head._1}.${lastTokens.head}"
                else s"import com.giyeok.${i.head._1}.{${lastTokens mkString ", "}}"
            }
        }).toList

        s"""${importLines.sorted mkString "\n"}
           |
           |object $objectName {
           |  val ngrammar = ${ScalaDefGenerator.ngrammarDef(astAnalysis.ngrammar)}
           |
           |  ${codeBlock.code}
           |}""".stripMargin
    }
}
