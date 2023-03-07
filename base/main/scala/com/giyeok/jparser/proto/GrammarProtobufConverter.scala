package com.giyeok.jparser.proto

import com.giyeok.jparser.NGrammar._
import com.giyeok.jparser.Symbols.Terminals
import com.giyeok.jparser.proto.GrammarProto.AtomicSymbol.AtomicSymbolCase
import com.giyeok.jparser.proto.GrammarProto.Empty
import com.giyeok.jparser.proto.GrammarProto.NAtomicSymbol.NAtomicSymbolCase
import com.giyeok.jparser.proto.GrammarProto.Symbol.SymbolCase
import com.giyeok.jparser.proto.GrammarProto.Terminal.TerminalCase
import com.giyeok.jparser.proto.ProtoConverterUtil._
import com.giyeok.jparser.{NGrammar, Symbols}

import scala.collection.immutable.ListSet
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava, SeqHasAsJava}

object GrammarProtobufConverter {
  private def convertAtomicSymbolToProtobuf(symbol: Symbols.AtomicSymbol): GrammarProto.AtomicSymbol = symbol match {
    case terminal: Symbols.Terminal =>
      GrammarProto.AtomicSymbol.newBuilder().setTerminal(convertTerminalSymbolToProto(terminal)).build()
    case Symbols.Start =>
      GrammarProto.AtomicSymbol.newBuilder().setStart(Empty.getDefaultInstance).build()
    case nonterminal: Symbols.Nonterminal =>
      GrammarProto.AtomicSymbol.newBuilder().setNonterminal(convertNonterminalSymbolToProto(nonterminal)).build()
    case oneof: Symbols.OneOf =>
      GrammarProto.AtomicSymbol.newBuilder().setOneOf(convertOneOfSymbolToProto(oneof)).build()
    case repeat: Symbols.Repeat =>
      GrammarProto.AtomicSymbol.newBuilder().setRepeat(convertRepeatSymbolToProto(repeat)).build()
    case except: Symbols.Except =>
      GrammarProto.AtomicSymbol.newBuilder().setExcept(convertExceptSymbolToProto(except)).build()
    case lookahead: Symbols.LookaheadIs =>
      GrammarProto.AtomicSymbol.newBuilder().setLookaheadIs(convertLookaheadIsSymbolToProto(lookahead)).build()
    case lookahead: Symbols.LookaheadExcept =>
      GrammarProto.AtomicSymbol.newBuilder().setLookaheadExcept(convertLookaheadExceptSymbolToProto(lookahead)).build()
    case proxy: Symbols.Proxy =>
      GrammarProto.AtomicSymbol.newBuilder().setProxy(convertProxySymbolToProto(proxy)).build()
    case join: Symbols.Join =>
      GrammarProto.AtomicSymbol.newBuilder().setJoin(convertJoinSymbolToProto(join)).build()
    case longest: Symbols.Longest =>
      GrammarProto.AtomicSymbol.newBuilder().setLongest(convertLongestSymbolToProto(longest)).build()
  }

  private def convertSymbolToProtobuf(symbol: Symbols.Symbol): GrammarProto.Symbol = symbol match {
    case symbol: Symbols.AtomicSymbol =>
      GrammarProto.Symbol.newBuilder()
        .setAtomicSymbol(convertAtomicSymbolToProtobuf(symbol)).build()
    case sequence: Symbols.Sequence =>
      GrammarProto.Symbol.newBuilder()
        .setSequence(convertSequenceSymbolToProtobuf(sequence)).build()
  }

  private def convertTerminalSymbolToProto(terminal: Symbols.Terminal): GrammarProto.Terminal = terminal match {
    case Terminals.Any => GrammarProto.Terminal.newBuilder().setAny(Empty.getDefaultInstance).build()
    case Terminals.AnyChar => GrammarProto.Terminal.newBuilder().setAnyChar(Empty.getDefaultInstance).build()
    case Terminals.ExactChar(char) =>
      GrammarProto.Terminal.newBuilder().setExactChar(char.toString).build()
    case Terminals.Chars(chars) =>
      GrammarProto.Terminal.newBuilder().setChars(
        GrammarProto.Terminal.Chars.newBuilder().addAllChars(chars.toList.sorted)).build()
    case Terminals.Unicode(categories) =>
      GrammarProto.Terminal.newBuilder().setUnicodes(
        GrammarProto.Terminal.Unicodes.newBuilder().addAllCategories(categories.toList.sorted)).build()
    case terminal: Terminals.VirtualTerminal => ???
  }

  def convertNonterminalSymbolToProto(symbol: Symbols.Nonterminal): GrammarProto.Nonterminal =
    GrammarProto.Nonterminal.newBuilder()
      .setName(symbol.name).build()

  def convertOneOfSymbolToProto(symbol: Symbols.OneOf): GrammarProto.OneOf =
    GrammarProto.OneOf.newBuilder()
      .addAllSymbols(symbol.syms.map(convertAtomicSymbolToProtobuf).toList.asJava).build()

  def convertProxySymbolToProto(symbol: Symbols.Proxy): GrammarProto.Proxy =
    GrammarProto.Proxy.newBuilder()
      .setSymbol(convertSymbolToProtobuf(symbol.sym)).build()

  def convertRepeatSymbolToProto(symbol: Symbols.Repeat): GrammarProto.Repeat =
    GrammarProto.Repeat.newBuilder()
      .setSymbol(convertAtomicSymbolToProtobuf(symbol.sym))
      .setLower(symbol.lower).build()

  def convertExceptSymbolToProto(symbol: Symbols.Except): GrammarProto.Except =
    GrammarProto.Except.newBuilder()
      .setBody(convertAtomicSymbolToProtobuf(symbol.sym))
      .setExcept(convertAtomicSymbolToProtobuf(symbol.except)).build()

  def convertJoinSymbolToProto(symbol: Symbols.Join): GrammarProto.Join =
    GrammarProto.Join.newBuilder()
      .setBody(convertAtomicSymbolToProtobuf(symbol.sym))
      .setJoin(convertAtomicSymbolToProtobuf(symbol.join)).build()

  def convertLongestSymbolToProto(symbol: Symbols.Longest): GrammarProto.Longest =
    GrammarProto.Longest.newBuilder()
      .setBody(convertAtomicSymbolToProtobuf(symbol.sym)).build()

  def convertLookaheadIsSymbolToProto(symbol: Symbols.LookaheadIs): GrammarProto.LookaheadIs =
    GrammarProto.LookaheadIs.newBuilder()
      .setLookahead(convertAtomicSymbolToProtobuf(symbol.lookahead)).build()

  def convertLookaheadExceptSymbolToProto(symbol: Symbols.LookaheadExcept): GrammarProto.LookaheadExcept =
    GrammarProto.LookaheadExcept.newBuilder()
      .setLookahead(convertAtomicSymbolToProtobuf(symbol.except)).build()

  def convertNAtomicSymbolToProtobuf(nsymbol: NAtomicSymbol): GrammarProto.NAtomicSymbol =
    nsymbol match {
      case NGrammar.NTerminal(id, symbol) =>
        GrammarProto.NAtomicSymbol.newBuilder().setTerminal(
          GrammarProto.NTerminal.newBuilder()
            .setId(id)
            .setSymbol(convertTerminalSymbolToProto(symbol))).build()
      case NGrammar.NStart(id, produce) =>
        GrammarProto.NAtomicSymbol.newBuilder().setStart(
          GrammarProto.NStart.newBuilder()
            .setId(id)
            .setProduce(produce)).build()
      case NGrammar.NNonterminal(id, symbol, produces) =>
        GrammarProto.NAtomicSymbol.newBuilder().setNonterminal(
          GrammarProto.NNonterminal.newBuilder()
            .setId(id)
            .setSymbol(convertNonterminalSymbolToProto(symbol))
            .addAllProduces(produces.toList.sorted)).build()
      case NGrammar.NOneOf(id, symbol, produces) =>
        GrammarProto.NAtomicSymbol.newBuilder().setOneOf(
          GrammarProto.NOneOf.newBuilder()
            .setId(id)
            .setSymbol(convertOneOfSymbolToProto(symbol))
            .addAllProduces(produces.toList.sorted)).build()
      case NGrammar.NProxy(id, symbol, produce) =>
        GrammarProto.NAtomicSymbol.newBuilder().setProxy(
          GrammarProto.NProxy.newBuilder()
            .setId(id)
            .setSymbol(convertProxySymbolToProto(symbol))
            .setProduce(produce)).build()
      case NGrammar.NRepeat(id, symbol, baseSeq, repeatSeq) =>
        GrammarProto.NAtomicSymbol.newBuilder().setRepeat(
          GrammarProto.NRepeat.newBuilder()
            .setId(id)
            .setSymbol(convertRepeatSymbolToProto(symbol))
            .setBaseSeq(baseSeq)
            .setRepeatSeq(repeatSeq)).build()
      case NGrammar.NExcept(id, symbol, body, except) =>
        GrammarProto.NAtomicSymbol.newBuilder().setExcept(
          GrammarProto.NExcept.newBuilder()
            .setId(id)
            .setSymbol(convertExceptSymbolToProto(symbol))
            .setBody(body)
            .setExcept(except)).build()
      case NGrammar.NJoin(id, symbol, body, join) =>
        GrammarProto.NAtomicSymbol.newBuilder().setJoin(
          GrammarProto.NJoin.newBuilder()
            .setId(id)
            .setSymbol(convertJoinSymbolToProto(symbol))
            .setBody(body)
            .setJoin(join)).build()
      case NGrammar.NLongest(id, symbol, body) =>
        GrammarProto.NAtomicSymbol.newBuilder().setLongest(
          GrammarProto.NLongest.newBuilder()
            .setId(id)
            .setSymbol(convertLongestSymbolToProto(symbol))
            .setBody(body)).build()
      case NGrammar.NLookaheadIs(id, symbol, emptySeqId, lookahead) =>
        GrammarProto.NAtomicSymbol.newBuilder().setLookaheadIs(
          GrammarProto.NLookaheadIs.newBuilder()
            .setId(id)
            .setSymbol(convertLookaheadIsSymbolToProto(symbol))
            .setEmptySeqId(emptySeqId)
            .setLookahead(lookahead)).build()
      case NGrammar.NLookaheadExcept(id, symbol, emptySeqId, lookahead) =>
        GrammarProto.NAtomicSymbol.newBuilder().setLookaheadExcept(
          GrammarProto.NLookaheadExcept.newBuilder()
            .setId(id)
            .setSymbol(convertLookaheadExceptSymbolToProto(symbol))
            .setEmptySeqId(emptySeqId)
            .setLookahead(lookahead)).build()
    }

  def convertSequenceSymbolToProtobuf(symbol: Symbols.Sequence): GrammarProto.Sequence =
    GrammarProto.Sequence.newBuilder()
      .addAllSeq(symbol.seq.map(convertAtomicSymbolToProtobuf).asJava).build()

  def convertNSequenceSymbolToProtobuf(sequence: NGrammar.NSequence): GrammarProto.NSequence =
    GrammarProto.NSequence.newBuilder()
      .setId(sequence.id)
      .setSymbol(convertSequenceSymbolToProtobuf(sequence.symbol))
      .addAllSequence(sequence.sequence.toList).build()

  def convertNGrammarToProto(ngrammar: NGrammar): GrammarProto.NGrammar = {
    val builder = GrammarProto.NGrammar.newBuilder()
      .setStartSymbol(ngrammar.startSymbol)
    ngrammar.nsymbols.toList.sortBy(_._1).foreach { symbol =>
      builder.putSymbols(symbol._1, convertNAtomicSymbolToProtobuf(symbol._2))
    }
    ngrammar.nsequences.toList.sortBy(_._1).foreach { sequence =>
      builder.putSequences(sequence._1, convertNSequenceSymbolToProtobuf(sequence._2))
    }
    builder.build()
  }

  def convertProtoToSymbol(symbol: GrammarProto.Symbol): Symbols.Symbol = symbol.getSymbolCase match {
    case SymbolCase.ATOMIC_SYMBOL => convertProtoToAtomicSymbol(symbol.getAtomicSymbol)
    case SymbolCase.SEQUENCE => convertProtoToSequence(symbol.getSequence)
  }

  def convertProtoToAtomicSymbol(proto: GrammarProto.AtomicSymbol): Symbols.AtomicSymbol = proto.getAtomicSymbolCase match {
    case AtomicSymbolCase.START => Symbols.Start
    case AtomicSymbolCase.TERMINAL => convertProtoToTerminalSymbol(proto.getTerminal)
    case AtomicSymbolCase.NONTERMINAL => convertProtoToNonterminalSymbol(proto.getNonterminal)
    case AtomicSymbolCase.ONE_OF => convertProtoToOneOfSymbol(proto.getOneOf)
    case AtomicSymbolCase.REPEAT => convertProtoToRepeatSymbol(proto.getRepeat)
    case AtomicSymbolCase.EXCEPT => convertProtoToExceptSymbol(proto.getExcept)
    case AtomicSymbolCase.LOOKAHEAD_IS => convertProtoToLookaheadIsSymbol(proto.getLookaheadIs)
    case AtomicSymbolCase.LOOKAHEAD_EXCEPT => convertProtoToLookaheadExceptSymbol(proto.getLookaheadExcept)
    case AtomicSymbolCase.PROXY => convertProtoToProxySymbol(proto.getProxy)
    case AtomicSymbolCase.JOIN => convertProtoToJoinSymbol(proto.getJoin)
    case AtomicSymbolCase.LONGEST => convertProtoToLongestSymbol(proto.getLongest)
  }

  def convertProtoToTerminalSymbol(proto: GrammarProto.Terminal): Symbols.Terminal = proto.getTerminalCase match {
    case TerminalCase.ANY => Terminals.Any
    case TerminalCase.ANY_CHAR => Terminals.AnyChar
    case TerminalCase.EXACT_CHAR => Terminals.ExactChar(proto.getExactChar.charAt(0))
    case TerminalCase.CHARS => Terminals.Chars(proto.getChars.getCharsList.asScala.toSet[String].map { x => x.charAt(0) })
    case TerminalCase.UNICODES => Terminals.Unicode(proto.getUnicodes.getCategoriesList.toSet)
  }

  def convertProtoToNonterminalSymbol(proto: GrammarProto.Nonterminal): Symbols.Nonterminal =
    Symbols.Nonterminal(proto.getName)

  def convertProtoToOneOfSymbol(proto: GrammarProto.OneOf): Symbols.OneOf = {
    val syms = proto.getSymbolsList.toScalaList(convertProtoToAtomicSymbol)
    Symbols.OneOf(ListSet(syms: _*))
  }

  def convertProtoToRepeatSymbol(proto: GrammarProto.Repeat): Symbols.Repeat =
    Symbols.Repeat(convertProtoToAtomicSymbol(proto.getSymbol), proto.getLower)

  def convertProtoToExceptSymbol(proto: GrammarProto.Except): Symbols.Except =
    Symbols.Except(convertProtoToAtomicSymbol(proto.getBody), convertProtoToAtomicSymbol(proto.getExcept))

  def convertProtoToLookaheadIsSymbol(proto: GrammarProto.LookaheadIs): Symbols.LookaheadIs =
    Symbols.LookaheadIs(convertProtoToAtomicSymbol(proto.getLookahead))

  def convertProtoToLookaheadExceptSymbol(proto: GrammarProto.LookaheadExcept): Symbols.LookaheadExcept =
    Symbols.LookaheadExcept(convertProtoToAtomicSymbol(proto.getLookahead))

  def convertProtoToProxySymbol(proto: GrammarProto.Proxy): Symbols.Proxy =
    Symbols.Proxy(convertProtoToSymbol(proto.getSymbol))

  def convertProtoToJoinSymbol(proto: GrammarProto.Join): Symbols.Join =
    Symbols.Join(convertProtoToAtomicSymbol(proto.getBody), convertProtoToAtomicSymbol(proto.getJoin))

  def convertProtoToLongestSymbol(proto: GrammarProto.Longest): Symbols.Longest =
    Symbols.Longest(convertProtoToAtomicSymbol(proto.getBody))

  def convertProtoToNAtomicSymbol(proto: GrammarProto.NAtomicSymbol): NAtomicSymbol = proto.getNAtomicSymbolCase match {
    case NAtomicSymbolCase.START =>
      val start = proto.getStart
      NStart(start.getId, start.getProduce)
    case NAtomicSymbolCase.TERMINAL =>
      val terminal = proto.getTerminal
      NTerminal(terminal.getId, convertProtoToTerminalSymbol(terminal.getSymbol))
    case NAtomicSymbolCase.NONTERMINAL =>
      val nonterm = proto.getNonterminal
      NNonterminal(nonterm.getId, convertProtoToNonterminalSymbol(nonterm.getSymbol), nonterm.getProducesList.toSet)
    case NAtomicSymbolCase.ONE_OF =>
      val oneof = proto.getOneOf
      NOneOf(oneof.getId, convertProtoToOneOfSymbol(oneof.getSymbol), oneof.getProducesList.toSet)
    case NAtomicSymbolCase.REPEAT =>
      val repeat = proto.getRepeat
      NRepeat(repeat.getId, convertProtoToRepeatSymbol(repeat.getSymbol), repeat.getBaseSeq, repeat.getRepeatSeq)
    case NAtomicSymbolCase.EXCEPT =>
      val except = proto.getExcept
      NExcept(except.getId, convertProtoToExceptSymbol(except.getSymbol), except.getBody, except.getExcept)
    case NAtomicSymbolCase.LOOKAHEAD_IS =>
      val lookahead = proto.getLookaheadIs
      NLookaheadIs(lookahead.getId, convertProtoToLookaheadIsSymbol(lookahead.getSymbol), lookahead.getEmptySeqId, lookahead.getLookahead)
    case NAtomicSymbolCase.LOOKAHEAD_EXCEPT =>
      val lookahead = proto.getLookaheadExcept
      NLookaheadExcept(lookahead.getId, convertProtoToLookaheadExceptSymbol(lookahead.getSymbol), lookahead.getEmptySeqId, lookahead.getLookahead)
    case NAtomicSymbolCase.PROXY =>
      val proxy = proto.getProxy
      NProxy(proxy.getId, convertProtoToProxySymbol(proxy.getSymbol), proxy.getProduce)
    case NAtomicSymbolCase.JOIN =>
      val join = proto.getJoin
      NJoin(join.getId, convertProtoToJoinSymbol(join.getSymbol), join.getBody, join.getJoin)
    case NAtomicSymbolCase.LONGEST =>
      val longest = proto.getLongest
      NLongest(longest.getId, convertProtoToLongestSymbol(longest.getSymbol), longest.getBody)
  }

  def convertProtoToNSequence(proto: GrammarProto.NSequence): NSequence =
    NSequence(proto.getId, convertProtoToSequence(proto.getSymbol), proto.getSequenceList)

  def convertProtoToSequence(proto: GrammarProto.Sequence): Symbols.Sequence =
    Symbols.Sequence(proto.getSeqList.toScalaList(convertProtoToAtomicSymbol))

  def convertProtoToNGrammar(protobuf: GrammarProto.NGrammar): NGrammar =
    new NGrammar(
      protobuf.getSymbolsMap.toScalaMap(e => e.getKey, e => convertProtoToNAtomicSymbol(e.getValue)),
      protobuf.getSequencesMap.toScalaMap(e => e.getKey, e => convertProtoToNSequence(e.getValue)),
      protobuf.getStartSymbol)
}
