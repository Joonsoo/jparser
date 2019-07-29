package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.NGrammar.NSequence
import com.giyeok.jparser.{Grammar, Symbols}

// Numbered Grammar
class NGrammar(val nsymbols: Map[Int, NGrammar.NAtomicSymbol], val nsequences: Map[Int, NGrammar.NSequence], val startSymbol: Int) {
    def symbolOf(id: Int): NGrammar.NSymbol = {
        nsymbols get id match {
            case Some(nsymbol) => nsymbol
            case None => nsequences(id)
        }
    }

    def lastPointerOf(symbolId: Int): Int = symbolOf(symbolId) match {
        case NSequence(_, _, seq) => seq.size
        case _ => 1
    }

    def findSymbol(symbol: Symbols.Symbol): Option[(Int, NGrammar.NSymbol)] =
        (nsymbols ++ nsequences) find { _._2.symbol == symbol }

    def describe(): Unit = {
        (nsymbols ++ nsequences).toList.sortBy(_._1) foreach { s =>
            println(s"${s._1} -> ${s._2.symbol.toShortString}")
        }
    }
}

object NGrammar {
    sealed trait NSymbol {
        val id: Int
        val symbol: Symbols.Symbol
    }
    sealed trait NAtomicSymbol extends NSymbol {
        override val symbol: Symbols.AtomicSymbol
    }

    case class NTerminal(id: Int, symbol: Symbols.Terminal) extends NAtomicSymbol

    sealed trait NSimpleDerive { val produces: Set[Int] }
    case class NStart(id: Int, produces: Set[Int]) extends NAtomicSymbol with NSimpleDerive { val symbol = Symbols.Start }
    case class NNonterminal(id: Int, symbol: Symbols.Nonterminal, produces: Set[Int]) extends NAtomicSymbol with NSimpleDerive
    case class NOneOf(id: Int, symbol: Symbols.OneOf, produces: Set[Int]) extends NAtomicSymbol with NSimpleDerive
    case class NProxy(id: Int, symbol: Symbols.Proxy, produce: Int) extends NAtomicSymbol with NSimpleDerive { val produces = Set(produce) }
    case class NRepeat(id: Int, symbol: Symbols.Repeat, baseSeq: Int, repeatSeq: Int) extends NAtomicSymbol with NSimpleDerive {
        val produces: Set[Int] = Set(baseSeq, repeatSeq)
    }
    case class NExcept(id: Int, symbol: Symbols.Except, body: Int, except: Int) extends NAtomicSymbol
    case class NJoin(id: Int, symbol: Symbols.Join, body: Int, join: Int) extends NAtomicSymbol
    case class NLongest(id: Int, symbol: Symbols.Longest, body: Int) extends NAtomicSymbol

    sealed trait NLookaheadSymbol extends NAtomicSymbol {
        val symbol: Symbols.Lookahead
        val emptySeqId: Int
        val lookahead: Int
    }
    case class NLookaheadIs(id: Int, symbol: Symbols.LookaheadIs, emptySeqId: Int, lookahead: Int) extends NLookaheadSymbol
    case class NLookaheadExcept(id: Int, symbol: Symbols.LookaheadExcept, emptySeqId: Int, lookahead: Int) extends NLookaheadSymbol

    case class NSequence(id: Int, symbol: Symbols.Sequence, sequence: Seq[Int]) extends NSymbol

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
                                case symbol: Symbols.Terminal => NTerminal(myId, symbol)

                                case Symbols.Start => NStart(myId, Set(numberOf(grammar.startSymbol)))
                                case symbol: Symbols.Nonterminal => NNonterminal(myId, symbol, grammar.rules(symbol.name) map { numberOf })
                                case symbol: Symbols.OneOf => NOneOf(myId, symbol, symbol.syms map { numberOf })
                                case symbol: Symbols.Proxy => NProxy(myId, symbol, numberOf(symbol.sym))
                                case symbol: Symbols.Repeat => NRepeat(myId, symbol, baseSeq = numberOf(symbol.baseSeq), repeatSeq = numberOf(symbol.repeatSeq))
                                case symbol: Symbols.Except => NExcept(myId, symbol, numberOf(symbol.sym), numberOf(symbol.except))
                                case symbol: Symbols.Longest => NLongest(myId, symbol, numberOf(symbol.sym))

                                case symbol: Symbols.Join => NJoin(myId, symbol, numberOf(symbol.sym), numberOf(symbol.join))

                                case symbol: Symbols.LookaheadIs => NLookaheadIs(myId, symbol, numberOf(Symbols.Sequence(Seq())), numberOf(symbol.lookahead))
                                case symbol: Symbols.LookaheadExcept => NLookaheadExcept(myId, symbol, numberOf(Symbols.Sequence(Seq())), numberOf(symbol.except))
                            }
                        case symbol: Symbols.Sequence =>
                            nsequences(myId) = NSequence(myId, symbol, symbol.seq map { numberOf })
                            assert(symbol.seq forall { symbolsMap contains _ })
                    }
                    myId
            }
        val startSymbolId = numberOf(Symbols.Start)
        new NGrammar(nsymbols.toMap, nsequences.toMap, startSymbolId)
    }
}
