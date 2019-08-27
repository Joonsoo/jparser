package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.Symbols
import com.giyeok.jparser.gramgram.meta2.Analyzer.Analysis
import com.giyeok.jparser.Inputs.CharsGrouping
import com.giyeok.jparser.gramgram.meta2.TypeDependenceGraph.SymbolNode
import com.giyeok.jparser.nparser.NGrammar

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
        case NGrammar.NStart(id, produces) =>
            s"NGrammar.NStart($id, ${intSetString(produces)})"
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
        val nsymbolsString = analysis.ngrammar.nsymbols.toList map { s =>
            s"${s._1} -> ${nAtomicSymbolString(s._2)}"
        }
        val nseqsString = analysis.ngrammar.nsequences.toList map { s =>
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

    def classDefs(): String = classDefsList() mkString "\n"

    private var _argNum = 0

    private def nextArgName(): String = {
        _argNum += 1
        "v" + _argNum
    }

    case class GenAstifierString(prepare: List[String], result: String)

    private def astifierString(expr: AstifierExpr, argName: String): GenAstifierString = expr match {
        case ThisNode => GenAstifierString(List(), argName)
        case Unbinder(expr, symbol) =>
            // TODO symbol 타입에 맞춰서 match** 함수 호출하는 경우 처리
            val bindedSymbolId = analysis.ngrammar.idOf(symbol)
            val e = astifierString(expr, argName)
            val xn = nextArgName()
            val nv = nextArgName()
            GenAstifierString(e.prepare ++ List(
                s"val BindNode($xn, $nv) = ${e.result}",
                s"assert($xn.id == $bindedSymbolId)"),
                nv)
        case SeqRef(expr, idx) =>
            val e = astifierString(expr, argName)
            val nv = nextArgName()
            GenAstifierString(e.prepare :+ s"val $nv = ${e.result}.asInstanceOf[SequenceNode].children($idx)", nv)
        case UnrollMapper(boundType, target, mapFn) =>
            // TODO
            astifierString(target, argName)
        case UnrollChoices(choiceSymbols) =>
            // TODO
            ???
        case CreateObj(className, args) =>
            val argStrings = args map (astifierString(_, argName))
            val nv = nextArgName()
            GenAstifierString(
                (argStrings flatMap (_.prepare)) :+
                    s"val $nv = $className(${argStrings map (_.result) mkString ","})",
                nv)
        case CreateList(elems) =>
            val elemStrings = elems map (astifierString(_, argName))
            val nv = nextArgName()
            GenAstifierString(
                (elemStrings flatMap (_.prepare)) :+
                    s"val $nv = List(${elemStrings map (_.result) mkString ","})",
                nv)
        case ConcatList(lhs, rhs) =>
            val ln = astifierString(lhs, argName)
            val rn = astifierString(rhs, argName)
            val nv = nextArgName()
            GenAstifierString((ln.prepare ++ rn.prepare) :+
                s"val $nv = ${ln.result} ++ ${rn.result}",
                nv)
    }

    private def matchFuncName(lhsName: String): String = s"match$lhsName"

    def astifierDefs(): String = {
        val astifierStrings = analysis.astifiers map { a =>
            val (lhs, rhs) = a
            val funcName = matchFuncName(lhs)

            val returnType = analysis.typeDependenceGraph.inferType(SymbolNode(Symbols.Nonterminal(lhs)))
            val returnTypeSpec = returnType.fixedType.getOrElse(returnType.inferredTypes.head)

            val rr = rhs map { r =>
                // TODO unreachable symbol이 있으면 없는 심볼에 대해서 ngrammar.idOf 실행해서 오류 생길 수 있음
                val symbolId = analysis.ngrammar.idOf(r._1)
                val gen = astifierString(r._2, "body")
                symbolId -> ((gen.prepare :+ gen.result) mkString "\n")
            }
            s"""def $funcName(node: Node): ${typeSpecToString(returnTypeSpec)} = {
               |  val BindNode(symbol, body) = node
               |  symbol.id match {
               |    ${rr map { c => s"case ${c._1} =>\n${c._2}" } mkString "\n"}
               |  }
               |}""".stripMargin
        }
        astifierStrings mkString "\n\n"
    }

    def toGrammarObject(objectName: String): String = {
        s"""import com.giyeok.jparser.{Grammar, Symbols}
           |import com.giyeok.jparser.nparser.NGrammar
           |import com.giyeok.jparser.ParseResultTree.Node
           |import scala.collection.immutable.{ListMap, ListSet}
           |
           |object $objectName {
           |  val ngrammar = ${ngrammarDef()}
           |
           |  ${classDefs()}
           |
           |  ${astifierDefs()}
           |}
           |""".stripMargin
    }
}
