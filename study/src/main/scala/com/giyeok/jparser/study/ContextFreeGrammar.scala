package com.giyeok.jparser.study

import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Symbols

object CfgSymbols {
    sealed trait Symbol

    case class Terminal(terminal: Symbols.Symbol) extends Symbol
    case class Nonterminal(name: String) extends Symbol
}

/*
import com.giyeok.jparser.tests.basics._
import com.giyeok.jparser.study._
def p(c:com.giyeok.jparser.Grammar):Unit = { val g = ContextFreeGrammar.convertFrom(c); g.printPretty(); g.printMapping() }
*/

trait ContextFreeGrammar {
    type RuleMap = ListMap[String, ListSet[Seq[CfgSymbols.Symbol]]]

    val name: String
    val rules: RuleMap
    val startNonterminal: String

    def printPretty(): Unit = {
        def ruleString(rule: Seq[CfgSymbols.Symbol]): String =
            if (rule.isEmpty) "<empty>" else
                (rule map {
                    case CfgSymbols.Terminal(term) => s"'${term.toShortString}'"
                    case CfgSymbols.Nonterminal(nonterminalName) => nonterminalName
                }) mkString " "

        println(s"** $name (Start $startNonterminal)")
        rules foreach { kv =>
            val (lhs, rhs) = kv
            val rules = rhs.toList
            println(s"$lhs = ${ruleString(rules.head)}")
            rules.tail foreach { rule =>
                println(s"${(0 until lhs.length map { _ => " " }).mkString} | ${ruleString(rule)}")
            }
        }
    }
}

case class ConvertedContextFreeGrammar(name: String, rules: ContextFreeGrammar#RuleMap, startNonterminal: String)(val mappings: Map[Symbols.Symbol, CfgSymbols.Symbol]) extends ContextFreeGrammar {
    def printMapping(): Unit = {
        println("** Mappings")
        mappings foreach { kv =>
            println(s"${kv._1.toShortString} -> ${kv._2}")
        }
    }
}

object ContextFreeGrammar {
    case class NotAContextFreeGrammar(symbol: Symbols.Symbol) extends Exception

    def convertFrom(grammar: Grammar): ConvertedContextFreeGrammar = {
        import Symbols.{Symbol => Symbol}
        import CfgSymbols.{Symbol => CfgSymbol}

        case class ConvertCC(mappings: Map[Symbol, CfgSymbol], rules: ListSet[(String, Seq[CfgSymbol])], usedNames: Set[String]) {
            assert(rules map { _._1 } subsetOf usedNames)

            def addMapping(symbol: Symbol, newCfgSymbol: => CfgSymbol): (ConvertCC, CfgSymbol) =
                mappings get symbol match {
                    case Some(cfgSymbol) => (ConvertCC(mappings, rules, usedNames), cfgSymbol)
                    case None =>
                        val cfgSymbol = newCfgSymbol
                        (ConvertCC(mappings + (symbol -> cfgSymbol), rules, usedNames), cfgSymbol)
                }

            def addNewNameMapping(baseName: String, symbol: Symbol, newCfgSymbol: String => CfgSymbol): (ConvertCC, CfgSymbol) = {
                val (name, ncc) = newName(baseName)
                ncc.addMapping(symbol, newCfgSymbol(name))
            }

            def addNewNontermMapping(baseName: String, symbol: Symbol): (ConvertCC, String, CfgSymbol) = {
                mappings get symbol match {
                    case Some(cfgSymbol @ CfgSymbols.Nonterminal(nonterminalName)) =>
                        (this, nonterminalName, cfgSymbol)
                    case Some(_) =>
                        throw new AssertionError("addNewNontermMapping gets a terminal symbol")
                    case None =>
                        val (newName, ncc0) = newNameNumbered(baseName)
                        val (ncc, cfgSymbol) = ncc0.addMapping(symbol, CfgSymbols.Nonterminal(newName))
                        (ncc, newName, cfgSymbol)
                }
            }

            def addRule(lhs: String, rhs: Seq[CfgSymbol]): ConvertCC =
                ConvertCC(mappings, rules + (lhs -> rhs), usedNames + lhs)

            def newName(baseName: String): (String, ConvertCC) = {
                def append(nextName: String): String =
                    if (usedNames contains nextName) append(nextName + "'")
                    else nextName
                val name = append(baseName)
                (name, ConvertCC(mappings, rules, usedNames + name))
            }

            def newNameNumbered(baseName: String): (String, ConvertCC) = {
                def append(number: Int): String = {
                    val nextName = s"${baseName}_$number"
                    if (usedNames contains nextName) append(number + 1)
                    else nextName
                }
                val name = append(0)
                (name, ConvertCC(mappings, rules, usedNames + name))
            }
        }

        def convert(list: List[(String, Symbol)], cc: ConvertCC, startNonterminal: String): ConvertedContextFreeGrammar = {
            list match {
                case (lhs, rhs) +: tail =>
                    // mappingOf는 기본적으로 Symbol을 넣으면 CfgSymbol이 나오는 함수
                    def sequenceOf(seq: Seq[Symbol], contentIdx: Set[Int], cc: ConvertCC): (ConvertCC, Seq[CfgSymbol]) = {
                        val wsIdx = seq.indices.toSet -- contentIdx
                        val (ncc, elemsRev) = seq.zipWithIndex.foldLeft((cc, Seq[CfgSymbol]())) { (nccSeq, symbolIdx) =>
                            val (ncc, elemsRev) = nccSeq
                            val (symbol, idx) = symbolIdx
                            val (nextCC, cfgSymbol) = mappingOf(symbol, ncc)
                            if (wsIdx contains idx) {
                                // TODO wsIdx 처리
                                // (nextCC, cfgSymbol의 optional 버젼 +: elemsRev)
                                ???
                            } else {
                                (nextCC, cfgSymbol +: elemsRev)
                            }
                        }
                        (ncc, elemsRev.reverse)
                    }

                    def mappingOf(symbol: Symbol, cc: ConvertCC): (ConvertCC, CfgSymbol) = {
                        symbol match {
                            case Symbols.Sequence(seq, contentIdx) =>
                                val (ncc0, newName, cfgSymbol) = cc.addNewNontermMapping("Seq", symbol)
                                val (ncc1, elems) = sequenceOf(seq, contentIdx.toSet, ncc0)
                                (ncc1.addRule(newName, elems), cfgSymbol)
                            case terminal: Symbols.Terminal =>
                                // mapping에 (terminal -> CfgSymbols.Terminal(terminal)) 를 넣는다
                                cc.addMapping(terminal, CfgSymbols.Terminal(terminal))
                            case nonterminal @ Symbols.Nonterminal(name) =>
                                // mapping에 (nonterminal -> CfgSymbols.Nonterminal(name)) 를 넣는다
                                cc.addMapping(nonterminal, CfgSymbols.Nonterminal(name))
                            case Symbols.OneOf(syms) =>
                                // syms에 해당하는 심볼들을 모두 mapping에 추가한다
                                // OneOf -> syms_0 | syms_1 | ... | syms_n 인 새 심볼 OneOf 를 mapping에 추가한 다음
                                // rhs로 OneOf 넌터미널을 리턴한다
                                val (ncc0, newName, cfgSymbol) = cc.addNewNontermMapping("OneOf", symbol)
                                val ncc = syms.foldLeft(ncc0) { (ncc, symbol) =>
                                    val (nextCC, cfgSymbol) = mappingOf(symbol, ncc)
                                    nextCC.addRule(newName, Seq(cfgSymbol))
                                }
                                (ncc, cfgSymbol)
                            case proxy @ Symbols.Proxy(sym) =>
                                val (ncc0, proxyCfgSymbol) = mappingOf(sym, cc)
                                val (ncc, newName, cfgSymbol) = ncc0.addNewNontermMapping("Proxy", symbol)
                                (ncc.addRule(newName, Seq(proxyCfgSymbol)), cfgSymbol)
                            case Symbols.Repeat(sym, lower) =>
                                val (ncc0, symCfgSymbol) = mappingOf(sym, cc)
                                // TODO
                                ???
                            case _: Symbols.EagerLongest | _: Symbols.Except | _: Symbols.Join | _: Symbols.Longest | _: Symbols.Lookahead | Symbols.Start =>
                                // exception 발생
                                throw NotAContextFreeGrammar(symbol)
                        }
                    }

                    val newCC = {
                        rhs match {
                            case Symbols.Sequence(seq, contentIdx) =>
                                // seq에 속한 Symbol을 CfgSymbol로 바꿔서 모두 맵핑에 넣는다
                                // contentIdx에 속하지 않은 인덱스의 seq의 element에 대해서는 원래의 Symbol.Symbol의 optional 심볼들을 모두 맵핑에 추가한다
                                val (ncc, elems) = sequenceOf(seq, contentIdx.toSet, cc)
                                ncc.addRule(lhs, elems)
                            case symbol =>
                                val (ncc, cfgSymbol) = mappingOf(symbol, cc)
                                ncc.addRule(lhs, Seq(cfgSymbol))
                        }
                    }
                    convert(tail, newCC, startNonterminal)
                case List() =>
                    val rulesMap = cc.rules.foldLeft(ListMap[String, ListSet[Seq[CfgSymbol]]]()) { (map, rule) =>
                        val (lhs, rhs) = rule
                        if (map contains lhs) {
                            map + (lhs -> (map(lhs) + rhs))
                        } else {
                            map + (lhs -> ListSet(rhs))
                        }
                    }
                    ConvertedContextFreeGrammar(grammar.name, rulesMap, startNonterminal)(cc.mappings)
            }
        }
        val flattenRules = grammar.rules.toList flatMap { rule => rule._2 map { rule._1 -> _ } }
        // rhs에 등장하는 모든 nonterminal은 lhs에도 등장해야 한다
        val lhsNames = grammar.rules.keySet
        // Symbols.Start는 무시해도 될 듯
        convert(flattenRules, ConvertCC(Map(), ListSet(), lhsNames), grammar.startSymbol.name)
    }

    implicit class ContextFreeGrammarChecker(grammar: ContextFreeGrammar) {
        def isGreibachNormalForm(emptyable: Boolean): Boolean = {
            ???
        }

        def hasEmptyRightHandSide: Boolean = {
            grammar.rules.values.flatten exists { _.isEmpty }
        }
    }
}
