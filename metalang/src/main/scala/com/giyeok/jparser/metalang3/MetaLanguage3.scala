package com.giyeok.jparser.metalang3

import com.giyeok.jparser.NGrammar.{NNonterminal, NStart}
import com.giyeok.jparser.examples.metalang3.MetaLang3Grammar
import com.giyeok.jparser.{Grammar, NGrammar, Symbols}
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.Rule
import com.giyeok.jparser.metalang3.codegen.ScalaGen
import com.giyeok.jparser.metalang3.valueify.ValueifyGen
import com.giyeok.jparser.metalang3.symbols.Escapes.NonterminalName

import scala.collection.immutable.{ListMap, ListSet}

object MetaLanguage3 {

    case class IllegalGrammar(msg: String) extends Exception(msg)

    def parseGrammar(grammar: String): MetaGrammar3Ast.Grammar = {
        MetaGrammar3Ast.parseAst(grammar) match {
            case Left(value) => value
            case Right(value) => throw IllegalGrammar(value.msg)
        }
    }

    def analyzeGrammar(grammarDefinition: String, grammarName: String = "GeneratedGrammar"): AnalysisResult = {
        val defs = parseGrammar(grammarDefinition).defs
        val valueifyGen = new ValueifyGen()
        val grammarRules = defs collect {
            case MetaGrammar3Ast.Rule(_, lhs, rhsList) =>
                lhs.name.name.stringName -> valueifyGen.valueifyRule(rhsList)
        }
        val names = grammarRules.map(_._1).groupBy(name => name)
        if (names.exists(_._2.size > 1)) {
            throw IllegalGrammar(s"Duplicate rule definitions: ${names.filter(_._2.size > 1).keySet.toList.sorted}")
        }

        //        val typeDefs = defs collect {
        //            case typeDef: MetaGrammar3Ast.TypeDef =>
        //                typeDef match {
        //                    case classDef: MetaGrammar3Ast.ClassDef =>
        //                        classDef match {
        //                            case MetaGrammar3Ast.AbstractClassDef(astNode, name, supers) => ???
        //                            case MetaGrammar3Ast.ConcreteClassDef(astNode, name, supers, params) => ???
        //                        }
        //                    case MetaGrammar3Ast.SuperDef(astNode, typeName, subs) => ???
        //                    case MetaGrammar3Ast.EnumTypeDef(astNode, name, values) => ???
        //                }
        //        }

        val ngrammar = NGrammar.fromGrammar(new Grammar {
            val name: String = grammarName
            val rules: RuleMap = ListMap.from(grammarRules.map(rule => rule._1 -> ListSet.from(rule._2._2)))
            val startSymbol: Symbols.Nonterminal = Symbols.Nonterminal(grammarRules.head._1)
        })
        println(ngrammar)

        val rule = defs.head.asInstanceOf[Rule]

        val startNonterminalName = rule.lhs.name.name.stringName
        val analysis = new AnalysisResult(startNonterminalName, ngrammar, valueifyGen.symbolsMap, Map())
        val scalaGen = new ScalaGen(analysis)
        scalaGen.matchFuncFor(rule.lhs.name, grammarRules.head._2._1).codes.foreach(println)
        println()

        analysis
    }

    def main(args: Array[String]): Unit = {
        MetaLang3Grammar.inMetaLang3
        val example =
            """A = (B? {$0 ?: "abc"}) 'd' {MyClass(value=$0$0)}
              |B = 'b'
              |C = 'c'
              |""".stripMargin

        val analysis = MetaLanguage3.analyzeGrammar(example)
        analysis.ngrammar.nsymbols.foreach(pair =>
            println(s"${pair._2.symbol.toShortString} -> ${pair._1}"))
        analysis.ngrammar.nsequences.foreach(pair =>
            println(s"${pair._2.symbol.toShortString} -> ${pair._1}"))
        println("====")
        analysis.ngrammar.nsymbols.collect {
            case (i, symbol: NStart) =>
                println(s"<start> $i -> ${symbol.produces.toList.sorted}")
            case (i, symbol: NNonterminal) =>
                println(s"${symbol.symbol.name} $i -> ${symbol.produces.toList.sorted}")
        }
    }
}