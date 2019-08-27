package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.Symbols
import com.giyeok.jparser.gramgram.meta2.Analyzer.Analysis
import com.giyeok.jparser.Inputs.CharsGrouping

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

    def grammarDef(grammarName: String, includeAstifiers: Boolean): String = {
        val g = analysis.grammar(grammarName)

        val ruleString = g.rules.toList.map { r =>
            s"""${escapeString(r._1)} -> ListSet(
               |  ${r._2 map symbolString mkString ",\n  "}
               |)""".stripMargin
        }
        s"""new Grammar {
           |  val name: String = ${escapeString(grammarName)}
           |  val startSymbol: Symbols.Nonterminal = Symbols.Nonterminal(${escapeString(g.startSymbol.name)})
           |  val rules: RuleMap = ListMap(
           |    ${ruleString mkString ",\n"}
           |  )
           |}
           |""".stripMargin
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

    private def astifierString(expr: AstifierExpr): String = expr match {
        case ThisNode => "node"
        case Unbinder(expr, symbol) =>
            val bindedSymbolId = analysis.ngrammar.idOf(symbol)
            astifierString(expr)
            ???
        case SeqRef(expr, idx) => ???
        case UnrollMapper(boundType, target, mapFn) => ???
        case UnrollChoices(choiceSymbols) => ???
        case CreateObj(className, args) => ???
        case CreateList(elems) => ???
        case ConcatList(lhs, rhs) => ???
    }

    def astifierDefs(): String = {
        "???"
    }
}
