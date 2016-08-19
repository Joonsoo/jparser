package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.EligCondition.Condition
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.ParseResult
import com.giyeok.jparser.ParseResultFunc
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.Symbols.Symbol
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.nparser.NGrammar._

class ParseTreeConstructor[R <: ParseResult](resultFunc: ParseResultFunc[R])(grammar: NGrammar)(input: Seq[Input], val history: Seq[Results[Node]], conditionFate: Map[Condition, Condition]) {
    val finishes = {
        def eligible(conditions: Set[Condition]): Boolean = {
            conditions exists { conditionFate.getOrElse(_, EligCondition.False).eligible }
        }
        (history map {
            _.nodeConditions.toSet[(Node, Set[Condition])] collect {
                case (node, conditions) if eligible(conditions) => node
            }
        }).toVector
    }
    // TODO finishes의 node set을 symbolId 기준으로 정렬해 놓으면 더 빠르게 할 수 있을듯

    def reconstruct(): Option[R] = {
        reconstruct(SymbolNode(grammar.startSymbol, 0), input.length)
    }
    def reconstruct(node: Node, gen: Int): Option[R] = {
        if (finishes(gen) contains node) Some(reconstruct(node, gen, Set())) else None
    }

    private def reconstruct(node: Node, gen: Int, traces: Set[Int]): R = {
        def reconstruct0(child: Node, childGen: Int): R = {
            val newTraces = if ((node.beginGen, gen) == (child.beginGen, childGen)) (traces + node.symbolId) else Set[Int]()
            reconstruct(child, childGen, newTraces)
        }

        node match {
            case SymbolNode(symbolId, beginGen) if traces contains symbolId =>
                resultFunc.cyclicBind(beginGen, gen, grammar.nsymbols(symbolId).symbol)
            case SymbolNode(symbolId, beginGen) =>
                grammar.nsymbols(symbolId) match {
                    case Terminal(terminalSymbol) =>
                        resultFunc.bind(beginGen, gen, terminalSymbol, resultFunc.terminal(beginGen, input(beginGen)))
                    case symbol: NSimpleDerivable =>
                        val merging = finishes(gen) filter { child =>
                            (symbol.produces contains child.symbolId) && (beginGen == child.beginGen)
                        } flatMap {
                            case child: SymbolNode =>
                                Some(resultFunc.bind(beginGen, gen, symbol.symbol, reconstruct0(child, gen)))
                            case child: SequenceNode =>
                                val sequenceSymbol = grammar.nsequences(child.symbolId)
                                if (sequenceSymbol.sequence.isEmpty) {
                                    // child node가 empty sequence인 경우
                                    Some(resultFunc.bind(beginGen, gen, symbol.symbol, resultFunc.sequence(child.beginGen, gen, sequenceSymbol.symbol)))
                                } else if (child.pointer + 1 == sequenceSymbol.sequence.length) {
                                    // empty가 아닌 경우
                                    val prevSeq = reconstruct0(child, child.endGen)
                                    val append = reconstruct0(SymbolNode(sequenceSymbol.sequence.last, child.endGen), gen)
                                    Some(resultFunc.bind(beginGen, gen, symbol.symbol, resultFunc.append(prevSeq, append)))
                                } else {
                                    None
                                }
                        }
                        assert(!merging.isEmpty)
                        resultFunc.merge(merging).get
                    case Join(symbol, body, join) =>
                        resultFunc.bind(beginGen, gen, symbol,
                            resultFunc.join(beginGen, gen, symbol,
                                reconstruct0(SymbolNode(body, beginGen), gen),
                                reconstruct0(SymbolNode(join, beginGen), gen)))
                    case symbol: NLookaheadSymbol =>
                        resultFunc.bind(beginGen, gen, symbol.symbol, resultFunc.sequence(beginGen, gen, Symbols.Sequence(Seq())))
                }
            case SequenceNode(sequenceId, 0, beginGen, endGen) =>
                resultFunc.sequence(beginGen, gen, grammar.nsequences(sequenceId).symbol)
            case SequenceNode(sequenceId, pointer, beginGen, endGen) =>
                assert(gen == endGen)
                val childSymId = grammar.nsequences(sequenceId).sequence(pointer - 1)
                val merging = finishes(gen) flatMap {
                    case child: SymbolNode if child.symbolId == childSymId =>
                        val prevSeq = SequenceNode(sequenceId, pointer - 1, beginGen, child.beginGen)
                        if (finishes(gen) contains prevSeq) {
                            Some(resultFunc.append(reconstruct0(prevSeq, child.beginGen), reconstruct0(child, gen)))
                        } else {
                            None
                        }
                    case _ => None
                }
                if (merging.isEmpty) {
                    println(node)
                    println(grammar.nsequences(sequenceId).symbol)
                    println(grammar.nsymbols(childSymId).symbol)
                    println(merging)
                    println("??")
                }
                assert(!merging.isEmpty)
                resultFunc.merge(merging).get
        }
    }
}

class CompactParseTreeConstructor[R <: ParseResult](resultFunc: ParseResultFunc[R])(grammar: CompactNGrammar)(input: Seq[Input], val history: Seq[Results[Node]], conditionFate: Map[Condition, Condition]) {

}
