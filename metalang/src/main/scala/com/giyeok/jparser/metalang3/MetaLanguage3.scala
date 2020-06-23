package com.giyeok.jparser.metalang3

import com.giyeok.jparser.NGrammar.{NNonterminal, NStart}
import com.giyeok.jparser.examples.metalang3.MetaLang3Grammar
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang3.analysis.{Analyzer, BottomUpTypeInferer, GrammarValidationException}
import com.giyeok.jparser.metalang3.codegen.ScalaGen
import com.giyeok.jparser.metalang3.valueify.{ValueifyGen, ValueifyInterpreter}
import com.giyeok.jparser.{Grammar, NGrammar, Symbols}

import scala.collection.immutable.{ListMap, ListSet}

object MetaLanguage3 {

    case class IllegalGrammar(msg: String) extends Exception(msg)

    def parseGrammar(grammar: String): MetaGrammar3Ast.Grammar = {
        MetaGrammar3Ast.parseAst(grammar) match {
            case Left(value) => value
            case Right(value) => throw IllegalGrammar(value.msg)
        }
    }

    /**
     * @throws GrammarValidationException
     * @param grammarDefinition
     * @param grammarName
     * @return
     */
    def analyzeGrammar(grammarDefinition: MetaGrammar3Ast.Grammar, grammarName: String): AnalysisResult = {
        val defs = grammarDefinition.defs
        val valueifyGen = new ValueifyGen(defs)
        val grammarRules = valueifyGen.grammarRules
        val names = grammarRules.map(_._1).groupBy(name => name)
        if (names.exists(_._2.size > 1)) {
            throw IllegalGrammar(s"Duplicate rule definitions: ${names.filter(_._2.size > 1).keySet.toList.sorted}")
        }

        println("====")
        valueifyGen.collector.classParamExprs.foreach(p => {
            p._2.foreach(x =>
                println(s"${p._1} -> $x")
            )
        })
        println("====")
        println(valueifyGen.collector.nonterminalTypes)
        println(valueifyGen.collector.classSuperTypes)

        valueifyGen.collector.validate()

        val startNonterminalName = grammarRules.head._1

        val bottomUpTypeInferer = new BottomUpTypeInferer(
            startNonterminalName, valueifyGen.symbolsMap, valueifyGen.collector.nonterminalTypes, Map())

        val ngrammar = NGrammar.fromGrammar(new Grammar {
            val name: String = grammarName
            val rules: RuleMap = ListMap.from(grammarRules.map(rule => rule._1 -> ListSet.from(rule._2._2)))
            val startSymbol: Symbols.Nonterminal = Symbols.Nonterminal(startNonterminalName)
        })
        println(ngrammar.startSymbol)
        println(ngrammar.nsymbols)
        println(ngrammar.nsequences)

        val analyzer = new Analyzer(ngrammar, grammarRules, valueifyGen.symbolsMap, valueifyGen.collector, bottomUpTypeInferer)
        analyzer.validate()

        val valueifyExprsMap = grammarRules.map(rule => rule._1 -> rule._2._1).toMap
        new AnalysisResult(ngrammar, startNonterminalName, valueifyExprsMap, valueifyGen.symbolsMap, analyzer)
    }

    def analyzeGrammar(grammarDefinition: String, grammarName: String = "GeneratedGrammar"): AnalysisResult =
        analyzeGrammar(parseGrammar(grammarDefinition), grammarName)

    def main(args: Array[String]): Unit = {
        MetaLang3Grammar.inMetaLang3
        val example =
            """A = 'a' B 'c' {Cl(name=$1)} $1
              |B = 'b'* {B(value=str($0))}
              |""".stripMargin

        val analysis = MetaLanguage3.analyzeGrammar(example)

        println(new ValueifyInterpreter(analysis).valueify("abc").left.get.prettyPrint())

        val scalaGen = new ScalaGen(analysis)
        scalaGen.matchFuncFor(analysis.startSymbolName, analysis.valueifyExprsMap(analysis.startSymbolName)).codes.foreach(println)
        println()

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
