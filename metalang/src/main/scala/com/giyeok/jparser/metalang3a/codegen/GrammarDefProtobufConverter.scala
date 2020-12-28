package com.giyeok.jparser.metalang3a.codegen

import com.giyeok.jparser.NGrammar._
import com.giyeok.jparser.Symbols.Terminals
import com.giyeok.jparser.metalang3a.proto.GrammarDefProto
import com.giyeok.jparser.metalang3a.proto.GrammarDefProto.AtomicSymbol.AtomicSymbolCase
import com.giyeok.jparser.metalang3a.proto.GrammarDefProto.Empty
import com.giyeok.jparser.metalang3a.proto.GrammarDefProto.NAtomicSymbol.NAtomicSymbolCase
import com.giyeok.jparser.metalang3a.proto.GrammarDefProto.Symbol.SymbolCase
import com.giyeok.jparser.metalang3a.proto.GrammarDefProto.Terminal.TerminalCase
import com.giyeok.jparser.{NGrammar, Symbols}

import scala.collection.immutable.ListSet
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava, MapHasAsScala, SeqHasAsJava}

object GrammarDefProtobufConverter {
  implicit def toJavaIntegerList(lst: List[Int]): java.util.List[Integer] =
    lst.map(i => i: java.lang.Integer).asJava

  implicit def toScalaIntList(lst: java.util.List[Integer]): List[Int] =
    lst.asScala.toList.map(i => i: Int)

  private def convertAtomicSymbolToProtobuf(symbol: Symbols.AtomicSymbol): GrammarDefProto.AtomicSymbol = symbol match {
    case terminal: Symbols.Terminal =>
      GrammarDefProto.AtomicSymbol.newBuilder().setTerminal(convertTerminalSymbolToProto(terminal)).build()
    case Symbols.Start =>
      GrammarDefProto.AtomicSymbol.newBuilder().setStart(Empty.getDefaultInstance).build()
    case nonterminal: Symbols.Nonterminal =>
      GrammarDefProto.AtomicSymbol.newBuilder().setNonterminal(convertNonterminalSymbolToProto(nonterminal)).build()
    case oneof: Symbols.OneOf =>
      GrammarDefProto.AtomicSymbol.newBuilder().setOneOf(convertOneOfSymbolToProto(oneof)).build()
    case repeat: Symbols.Repeat =>
      GrammarDefProto.AtomicSymbol.newBuilder().setRepeat(convertRepeatSymbolToProto(repeat)).build()
    case except: Symbols.Except =>
      GrammarDefProto.AtomicSymbol.newBuilder().setExcept(convertExceptSymbolToProto(except)).build()
    case lookahead: Symbols.LookaheadIs =>
      GrammarDefProto.AtomicSymbol.newBuilder().setLookaheadIs(convertLookaheadIsSymbolToProto(lookahead)).build()
    case lookahead: Symbols.LookaheadExcept =>
      GrammarDefProto.AtomicSymbol.newBuilder().setLookaheadExcept(convertLookaheadExceptSymbolToProto(lookahead)).build()
    case proxy: Symbols.Proxy =>
      GrammarDefProto.AtomicSymbol.newBuilder().setProxy(convertProxySymbolToProto(proxy)).build()
    case join: Symbols.Join =>
      GrammarDefProto.AtomicSymbol.newBuilder().setJoin(convertJoinSymbolToProto(join)).build()
    case longest: Symbols.Longest =>
      GrammarDefProto.AtomicSymbol.newBuilder().setLongest(convertLongestSymbolToProto(longest)).build()
  }

  private def convertSymbolToProtobuf(symbol: Symbols.Symbol): GrammarDefProto.Symbol = symbol match {
    case symbol: Symbols.AtomicSymbol =>
      GrammarDefProto.Symbol.newBuilder()
        .setAtomicSymbol(convertAtomicSymbolToProtobuf(symbol)).build()
    case sequence: Symbols.Sequence =>
      GrammarDefProto.Symbol.newBuilder()
        .setSequence(convertSequenceSymbolToProtobuf(sequence)).build()
  }

  private def convertTerminalSymbolToProto(terminal: Symbols.Terminal): GrammarDefProto.Terminal = terminal match {
    case Terminals.Any => GrammarDefProto.Terminal.newBuilder().setAny(Empty.getDefaultInstance).build()
    case Terminals.AnyChar => GrammarDefProto.Terminal.newBuilder().setAnyChar(Empty.getDefaultInstance).build()
    case Terminals.ExactChar(char) =>
      GrammarDefProto.Terminal.newBuilder().setExactChar(char.toString).build()
    case Terminals.Chars(chars) =>
      GrammarDefProto.Terminal.newBuilder().setChars(
        GrammarDefProto.Terminal.Chars.newBuilder().addAllChars(chars.map(_.toString).toList.sorted.asJava)).build()
    case Terminals.Unicode(categories) =>
      GrammarDefProto.Terminal.newBuilder().setUnicodes(
        GrammarDefProto.Terminal.Unicodes.newBuilder().addAllCategories(categories.toList.sorted)).build()
    case terminal: Terminals.VirtualTerminal => ???
  }

  def convertNonterminalSymbolToProto(symbol: Symbols.Nonterminal): GrammarDefProto.Nonterminal =
    GrammarDefProto.Nonterminal.newBuilder()
      .setName(symbol.name).build()

  def convertOneOfSymbolToProto(symbol: Symbols.OneOf): GrammarDefProto.OneOf =
    GrammarDefProto.OneOf.newBuilder()
      .addAllSymbols(symbol.syms.map(convertAtomicSymbolToProtobuf).toList.asJava).build()

  def convertProxySymbolToProto(symbol: Symbols.Proxy): GrammarDefProto.Proxy =
    GrammarDefProto.Proxy.newBuilder()
      .setSymbol(convertSymbolToProtobuf(symbol.sym)).build()

  def convertRepeatSymbolToProto(symbol: Symbols.Repeat): GrammarDefProto.Repeat =
    GrammarDefProto.Repeat.newBuilder()
      .setSymbol(convertAtomicSymbolToProtobuf(symbol.sym))
      .setLower(symbol.lower).build()

  def convertExceptSymbolToProto(symbol: Symbols.Except): GrammarDefProto.Except =
    GrammarDefProto.Except.newBuilder()
      .setBody(convertAtomicSymbolToProtobuf(symbol.sym))
      .setExcept(convertAtomicSymbolToProtobuf(symbol.except)).build()

  def convertJoinSymbolToProto(symbol: Symbols.Join): GrammarDefProto.Join =
    GrammarDefProto.Join.newBuilder()
      .setBody(convertAtomicSymbolToProtobuf(symbol.sym))
      .setJoin(convertAtomicSymbolToProtobuf(symbol.join)).build()

  def convertLongestSymbolToProto(symbol: Symbols.Longest): GrammarDefProto.Longest =
    GrammarDefProto.Longest.newBuilder()
      .setBody(convertAtomicSymbolToProtobuf(symbol.sym)).build()

  def convertLookaheadIsSymbolToProto(symbol: Symbols.LookaheadIs): GrammarDefProto.LookaheadIs =
    GrammarDefProto.LookaheadIs.newBuilder()
      .setLookahead(convertAtomicSymbolToProtobuf(symbol.lookahead)).build()

  def convertLookaheadExceptSymbolToProto(symbol: Symbols.LookaheadExcept): GrammarDefProto.LookaheadExcept =
    GrammarDefProto.LookaheadExcept.newBuilder()
      .setLookahead(convertAtomicSymbolToProtobuf(symbol.except)).build()

  def convertNAtomicSymbolToProtobuf(nsymbol: NAtomicSymbol): GrammarDefProto.NAtomicSymbol =
    nsymbol match {
      case NGrammar.NTerminal(id, symbol) =>
        GrammarDefProto.NAtomicSymbol.newBuilder().setTerminal(
          GrammarDefProto.NTerminal.newBuilder()
            .setId(id)
            .setSymbol(convertTerminalSymbolToProto(symbol))).build()
      case NGrammar.NStart(id, produce) =>
        GrammarDefProto.NAtomicSymbol.newBuilder().setStart(
          GrammarDefProto.NStart.newBuilder()
            .setId(id)
            .setProduce(produce)).build()
      case NGrammar.NNonterminal(id, symbol, produces) =>
        GrammarDefProto.NAtomicSymbol.newBuilder().setNonterminal(
          GrammarDefProto.NNonterminal.newBuilder()
            .setId(id)
            .setSymbol(convertNonterminalSymbolToProto(symbol))
            .addAllProduces(produces.toList.sorted)).build()
      case NGrammar.NOneOf(id, symbol, produces) =>
        GrammarDefProto.NAtomicSymbol.newBuilder().setOneOf(
          GrammarDefProto.NOneOf.newBuilder()
            .setId(id)
            .setSymbol(convertOneOfSymbolToProto(symbol))
            .addAllProduces(produces.toList.sorted)).build()
      case NGrammar.NProxy(id, symbol, produce) =>
        GrammarDefProto.NAtomicSymbol.newBuilder().setProxy(
          GrammarDefProto.NProxy.newBuilder()
            .setId(id)
            .setSymbol(convertProxySymbolToProto(symbol))
            .setProduce(produce)).build()
      case NGrammar.NRepeat(id, symbol, baseSeq, repeatSeq) =>
        GrammarDefProto.NAtomicSymbol.newBuilder().setRepeat(
          GrammarDefProto.NRepeat.newBuilder()
            .setId(id)
            .setSymbol(convertRepeatSymbolToProto(symbol))
            .setBaseSeq(baseSeq)
            .setRepeatSeq(repeatSeq)).build()
      case NGrammar.NExcept(id, symbol, body, except) =>
        GrammarDefProto.NAtomicSymbol.newBuilder().setExcept(
          GrammarDefProto.NExcept.newBuilder()
            .setId(id)
            .setSymbol(convertExceptSymbolToProto(symbol))
            .setBody(body)
            .setExcept(except)).build()
      case NGrammar.NJoin(id, symbol, body, join) =>
        GrammarDefProto.NAtomicSymbol.newBuilder().setJoin(
          GrammarDefProto.NJoin.newBuilder()
            .setId(id)
            .setSymbol(convertJoinSymbolToProto(symbol))
            .setBody(body)
            .setJoin(join)).build()
      case NGrammar.NLongest(id, symbol, body) =>
        GrammarDefProto.NAtomicSymbol.newBuilder().setLongest(
          GrammarDefProto.NLongest.newBuilder()
            .setId(id)
            .setSymbol(convertLongestSymbolToProto(symbol))
            .setBody(body)).build()
      case NGrammar.NLookaheadIs(id, symbol, emptySeqId, lookahead) =>
        GrammarDefProto.NAtomicSymbol.newBuilder().setLookaheadIs(
          GrammarDefProto.NLookaheadIs.newBuilder()
            .setId(id)
            .setSymbol(convertLookaheadIsSymbolToProto(symbol))
            .setEmptySeqId(emptySeqId)
            .setLookahead(lookahead)).build()
      case NGrammar.NLookaheadExcept(id, symbol, emptySeqId, lookahead) =>
        GrammarDefProto.NAtomicSymbol.newBuilder().setLookaheadExcept(
          GrammarDefProto.NLookaheadExcept.newBuilder()
            .setId(id)
            .setSymbol(convertLookaheadExceptSymbolToProto(symbol))
            .setEmptySeqId(emptySeqId)
            .setLookahead(lookahead)).build()
    }

  def convertSequenceSymbolToProtobuf(symbol: Symbols.Sequence): GrammarDefProto.Sequence =
    GrammarDefProto.Sequence.newBuilder()
      .addAllSeq(symbol.seq.map(convertAtomicSymbolToProtobuf).asJava).build()

  def convertNSequenceSymbolToProtobuf(sequence: NGrammar.NSequence): GrammarDefProto.NSequence =
    GrammarDefProto.NSequence.newBuilder()
      .setId(sequence.id)
      .setSymbol(convertSequenceSymbolToProtobuf(sequence.symbol))
      .addAllSequence(sequence.sequence.toList).build()

  def convertNGrammarToProtobuf(ngrammar: NGrammar): GrammarDefProto.NGrammar = {
    GrammarDefProto.NGrammar.newBuilder()
      .setStartSymbol(ngrammar.startSymbol)
      .putAllSymbols(ngrammar.nsymbols.map { x => (x._1: java.lang.Integer) -> convertNAtomicSymbolToProtobuf(x._2) }.asJava)
      .putAllSequences(ngrammar.nsequences.map { x => (x._1: java.lang.Integer) -> convertNSequenceSymbolToProtobuf(x._2) }.asJava)
      .build()
  }

  def convertProtoToSymbol(symbol: GrammarDefProto.Symbol): Symbols.Symbol = symbol.getSymbolCase match {
    case SymbolCase.ATOMIC_SYMBOL => convertProtoToAtomicSymbol(symbol.getAtomicSymbol)
    case SymbolCase.SEQUENCE => convertProtoToSequence(symbol.getSequence)
  }

  def convertProtoToAtomicSymbol(proto: GrammarDefProto.AtomicSymbol): Symbols.AtomicSymbol = proto.getAtomicSymbolCase match {
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

  def convertProtoToTerminalSymbol(proto: GrammarDefProto.Terminal): Symbols.Terminal = proto.getTerminalCase match {
    case TerminalCase.ANY => Terminals.Any
    case TerminalCase.ANY_CHAR => Terminals.AnyChar
    case TerminalCase.EXACT_CHAR => Terminals.ExactChar(proto.getExactChar.charAt(0))
    case TerminalCase.CHARS => Terminals.Chars(proto.getChars.getCharsList.asScala.toSet[String].map { x => x.charAt(0) })
    case TerminalCase.UNICODES => Terminals.Unicode(proto.getUnicodes.getCategoriesList.toSet)
  }

  def convertProtoToNonterminalSymbol(proto: GrammarDefProto.Nonterminal): Symbols.Nonterminal =
    Symbols.Nonterminal(proto.getName)

  def convertProtoToOneOfSymbol(proto: GrammarDefProto.OneOf): Symbols.OneOf = {
    val syms = proto.getSymbolsList.asScala.toList.map(convertProtoToAtomicSymbol)
    Symbols.OneOf(ListSet(syms: _*))
  }

  def convertProtoToRepeatSymbol(proto: GrammarDefProto.Repeat): Symbols.Repeat =
    Symbols.Repeat(convertProtoToAtomicSymbol(proto.getSymbol), proto.getLower)

  def convertProtoToExceptSymbol(proto: GrammarDefProto.Except): Symbols.Except =
    Symbols.Except(convertProtoToAtomicSymbol(proto.getBody), convertProtoToAtomicSymbol(proto.getExcept))

  def convertProtoToLookaheadIsSymbol(proto: GrammarDefProto.LookaheadIs): Symbols.LookaheadIs =
    Symbols.LookaheadIs(convertProtoToAtomicSymbol(proto.getLookahead))

  def convertProtoToLookaheadExceptSymbol(proto: GrammarDefProto.LookaheadExcept): Symbols.LookaheadExcept =
    Symbols.LookaheadExcept(convertProtoToAtomicSymbol(proto.getLookahead))

  def convertProtoToProxySymbol(proto: GrammarDefProto.Proxy): Symbols.Proxy =
    Symbols.Proxy(convertProtoToSymbol(proto.getSymbol))

  def convertProtoToJoinSymbol(proto: GrammarDefProto.Join): Symbols.Join =
    Symbols.Join(convertProtoToAtomicSymbol(proto.getBody), convertProtoToAtomicSymbol(proto.getJoin))

  def convertProtoToLongestSymbol(proto: GrammarDefProto.Longest): Symbols.Longest =
    Symbols.Longest(convertProtoToAtomicSymbol(proto.getBody))

  def convertProtoToNAtomicSymbol(proto: GrammarDefProto.NAtomicSymbol): NAtomicSymbol = proto.getNAtomicSymbolCase match {
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

  def convertProtoToNSequence(proto: GrammarDefProto.NSequence): NSequence =
    NSequence(proto.getId, convertProtoToSequence(proto.getSymbol), proto.getSequenceList)

  def convertProtoToSequence(proto: GrammarDefProto.Sequence): Symbols.Sequence =
    Symbols.Sequence(proto.getSeqList.asScala.toList.map(convertProtoToAtomicSymbol))

  def convertProtobufToNGrammar(protobuf: GrammarDefProto.NGrammar): NGrammar =
    new NGrammar(
      protobuf.getSymbolsMap.asScala.map { x => (x._1: Int) -> convertProtoToNAtomicSymbol(x._2) }.toMap,
      protobuf.getSequencesMap.asScala.map { x => (x._1: Int) -> convertProtoToNSequence(x._2) }.toMap,
      protobuf.getStartSymbol)
}
