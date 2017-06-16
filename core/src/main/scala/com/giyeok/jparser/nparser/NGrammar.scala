package com.giyeok.jparser.nparser

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Symbols

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

    sealed trait NSimpleDerive { val produces: Set[Int] }
    case class NStart(produces: Set[Int]) extends NAtomicSymbol with NSimpleDerive { val symbol = Symbols.Start }
    case class NNonterminal(symbol: Symbols.Nonterminal, produces: Set[Int]) extends NAtomicSymbol with NSimpleDerive
    case class NOneOf(symbol: Symbols.OneOf, produces: Set[Int]) extends NAtomicSymbol with NSimpleDerive
    case class NProxy(symbol: Symbols.Proxy, produce: Int) extends NAtomicSymbol with NSimpleDerive { val produces = Set(produce) }
    case class NRepeat(symbol: Symbols.Repeat, produces: Set[Int]) extends NAtomicSymbol with NSimpleDerive
    case class NExcept(symbol: Symbols.Except, body: Int, except: Int) extends NAtomicSymbol
    case class NJoin(symbol: Symbols.Join, body: Int, join: Int) extends NAtomicSymbol
    case class NLongest(symbol: Symbols.Longest, body: Int) extends NAtomicSymbol

    sealed trait NLookaheadSymbol extends NAtomicSymbol {
        val symbol: Symbols.Lookahead
        val emptySeqId: Int
        val lookahead: Int
    }
    case class NLookaheadIs(symbol: Symbols.LookaheadIs, emptySeqId: Int, lookahead: Int) extends NLookaheadSymbol
    case class NLookaheadExcept(symbol: Symbols.LookaheadExcept, emptySeqId: Int, lookahead: Int) extends NLookaheadSymbol

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
