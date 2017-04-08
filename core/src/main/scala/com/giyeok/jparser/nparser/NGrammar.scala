package com.giyeok.jparser.nparser

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.nparser.NGrammar._

// Numbered Grammar
class NGrammar(val nsymbols: Map[Int, NGrammar.NAtomicSymbol], val nsequences: Map[Int, NGrammar.NSequence], val startSymbol: Int) {
    def symbolOf(id: Int): NGrammar.NSymbol = {
        nsymbols get id match {
            case Some(nsymbol) => nsymbol
            case None => nsequences(id)
        }
    }
}

object NGrammar {
    sealed trait NSymbol { val symbol: Symbols.Symbol }
    sealed trait NAtomicSymbol extends NSymbol

    case class NTerminal(symbol: Symbols.Terminal) extends NAtomicSymbol

    sealed trait NSimpleDerivable extends NAtomicSymbol { val produces: Set[Int] }
    case class NStart(produces: Set[Int]) extends NSimpleDerivable { val symbol = Symbols.Start }
    case class NNonterminal(symbol: Symbols.Nonterminal, produces: Set[Int]) extends NSimpleDerivable
    case class NOneOf(symbol: Symbols.OneOf, produces: Set[Int]) extends NSimpleDerivable
    case class NProxy(symbol: Symbols.Proxy, produce: Int) extends NSimpleDerivable { val produces = Set(produce) }
    case class NRepeat(symbol: Symbols.Repeat, produces: Set[Int]) extends NSimpleDerivable
    case class NExcept(symbol: Symbols.Except, body: Int, except: Int) extends NSimpleDerivable { val produces = Set(body, except) }
    case class NLongest(symbol: Symbols.Longest, body: Int) extends NSimpleDerivable { val produces = Set(body) }

    case class NJoin(symbol: Symbols.Join, body: Int, join: Int) extends NAtomicSymbol

    sealed trait NLookaheadSymbol extends NAtomicSymbol {
        val symbol: Symbols.Lookahead
        val emptySeqId: Int
        val lookahead: Int
    }
    case class NLookaheadIs(symbol: Symbols.LookaheadIs, emptySeqId: Int, lookahead: Int) extends NLookaheadSymbol
    case class NLookaheadExcept(symbol: Symbols.LookaheadExcept, emptySeqId: Int, lookahead: Int) extends NLookaheadSymbol

    // case class Compaction(symbols: Seq[Int]) extends NSymbol

    case class NSequence(symbol: Symbols.Sequence, sequence: Seq[Int]) extends NSymbol

    def fromGrammar(grammar: Grammar): NGrammar = {
        var newId = 0
        val symbolsMap = scala.collection.mutable.Map[Symbols.Symbol, Int]()
        val nsymbols = scala.collection.mutable.Map[Int, NAtomicSymbol]()
        val nsequences = scala.collection.mutable.Map[Int, NSequence]()

        def numberOf(symbol: Symbols.Symbol): Int =
            symbolsMap get symbol match {
                case Some(nsymbolId) => nsymbolId
                case None =>
                    newId += 1
                    val myId = newId
                    symbolsMap(symbol) = myId
                    symbol match {
                        case symbol: Symbols.AtomicSymbol =>
                            nsymbols(myId) = symbol match {
                                case symbol: Symbols.Terminal => NTerminal(symbol)

                                case Symbols.Start => NStart(Set(numberOf(grammar.startSymbol)))
                                case symbol: Symbols.Nonterminal => NNonterminal(symbol, grammar.rules(symbol.name) map { numberOf })
                                case symbol: Symbols.OneOf => NOneOf(symbol, symbol.syms map { numberOf })
                                case symbol: Symbols.Proxy => NProxy(symbol, numberOf(symbol.sym))
                                case symbol: Symbols.Repeat => NRepeat(symbol, Set(numberOf(symbol.baseSeq), numberOf(symbol.repeatSeq)))
                                case symbol: Symbols.Except => NExcept(symbol, numberOf(symbol.sym), numberOf(symbol.except))
                                case symbol: Symbols.Longest => NLongest(symbol, numberOf(symbol.sym))

                                case symbol: Symbols.Join => NJoin(symbol, numberOf(symbol.sym), numberOf(symbol.join))

                                case symbol: Symbols.LookaheadIs => NLookaheadIs(symbol, numberOf(Symbols.Sequence(Seq())), numberOf(symbol.lookahead))
                                case symbol: Symbols.LookaheadExcept => NLookaheadExcept(symbol, numberOf(Symbols.Sequence(Seq())), numberOf(symbol.except))
                            }
                        case symbol: Symbols.Sequence =>
                            nsequences(myId) = NSequence(symbol, symbol.seq map { numberOf })
                            assert(symbol.seq forall { symbolsMap contains _ })
                    }
                    myId
            }
        val startSymbolId = numberOf(Symbols.Start)
        new NGrammar(nsymbols.toMap, nsequences.toMap, startSymbolId)
    }
}

class CompactNGrammar(nsymbols: Map[Int, NGrammar.NAtomicSymbol], nsequences: Map[Int, NGrammar.NSequence], startSymbol: Int) extends NGrammar(nsymbols, nsequences, startSymbol) {
    def isCompactable(symbolId: Int): Boolean = {
        nsymbols get symbolId match {
            case Some(nsymbol) =>
                nsymbol match {
                    case _: NStart | _: NNonterminal | _: NOneOf | _: NProxy | _: NRepeat => true
                    case _ => false
                }
            case None =>
                nsequences(symbolId).sequence.length == 1
        }
    }

    // correspondingSymbols의 (key, value)는 key가 finish되면 value의 심볼들도 모두 (같은 조건으로) finish됨을 의미한다
    val correspondingSymbols: Map[Int, Set[Int]] = {
        var cc = Map[Int, Set[Int]]()
        def traverse(symbolId: Int, path: Set[Int]): Unit = {
            cc += (symbolId -> (cc.getOrElse(symbolId, Set()) ++ path))
            if (isCompactable(symbolId)) {
                nsymbols get symbolId match {
                    case Some(nsymbol: NSimpleDerivable) =>
                        (nsymbol.produces -- path) foreach { traverse(_, path + symbolId) }
                    case Some(_) => // nothing to do
                    case None =>
                        val sequence = nsequences(symbolId)
                        assert(sequence.sequence.length == 1)
                        traverse(sequence.sequence.head, path + symbolId)
                }
            }
        }
        nsymbols.keys foreach { traverse(_, Set()) }
        cc
    }
    // reverseCorrespondingSymbols의 (key, value)는 value 중 하나가 finish되었으면 key도 (같은 조건으로) finish됨 의미한다
    val reverseCorrespondingSymbols: Map[Int, Set[Int]] = {
        correspondingSymbols.foldLeft(Map[Int, Set[Int]]()) { (cc, entry) =>
            entry._2.foldLeft(cc) { (cc, correspond) =>
                cc + (correspond -> (cc.getOrElse(correspond, Set[Int]()) + entry._1))
            }
        }
    }
}

object CompactNGrammar {
    def fromNGrammar(ngrammar: NGrammar): CompactNGrammar = new CompactNGrammar(ngrammar.nsymbols, ngrammar.nsequences, ngrammar.startSymbol)
    def fromGrammar(grammar: Grammar): CompactNGrammar = fromNGrammar(NGrammar.fromGrammar(grammar))
}
