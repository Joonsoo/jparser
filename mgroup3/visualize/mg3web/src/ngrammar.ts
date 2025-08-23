type NGrammar = {
  startSymbol: number
  symbols: { [symbol_id: number]: NAtomicSymbol }
  sequences: { [sequence_id: number]: NSequence }
};

type NAtomicSymbol =
  { start: NStart } |
  { nonterminal: NNonterminal } |
  { terminal: NTerminal } |
  { oneOf: NOneOf } |
  { proxy: NProxy } |
  { repeat: NRepeat } |
  { except: NExcept } |
  { join: NJoin } |
  { longest: NLongest } |
  { lookahead_is: NLookaheadIs } |
  { lookahead_except: NLookaheadExcept };

type NStart = { id: number, produce: number };
type NNonterminal = { id: number, symbol: Nonterminal, produces: number[] };
type NTerminal = { id: number, symbol: Terminal };
type NOneOf = { id: number, symbol: OneOf, produces: number[] };
type NProxy = { id: number, symbol: Proxy, produce: number };
type NRepeat = { id: number, symbol: Repeat, baseSeq: number, repeatSeq: number };
type NExcept = { id: number, symbol: Except, body: number, except: number };
type NJoin = { id: number, symbol: Join, body: number, join: number };
type NLongest = { id: number, symbol: Longest, body: number };
type NLookaheadIs = { id: number, symbol: LookaheadIs, emptySeqId: number, lookahead: number };
type NLookaheadExcept = { id: number, symbol: LookaheadExcept, emptySeqId: number, lookahead: number };
type NSequence = { id: number, symbol: Sequence, sequence: number[] };

type AtomicSymbol =
  { start: {} } |
  { terminal: Terminal } |
  { nonterminal: Nonterminal } |
  { one_of: OneOf } |
  { repeat: Repeat } |
  { except: Except } |
  { lookahead_is: LookaheadIs } |
  { lookahead_except: LookaheadExcept } |
  { proxy: Proxy } |
  { join: Join } |
  { longest: Longest };

type Terminal = { any: {} } |
  { anyChar: {} } |
  { exactChar: string } |
  { chars: string[] } |
  { unicodes: number[] };
type Nonterminal = { name: string };
type OneOf = { symbols: AtomicSymbol[] };
type Repeat = { symbol: AtomicSymbol, lower: number };
type Except = { body: AtomicSymbol, except: AtomicSymbol };
type LookaheadIs = { lookahead: AtomicSymbol };
type LookaheadExcept = { lookahead: AtomicSymbol };
type Proxy = { symbol: AtomicSymbol | Sequence };
type Join = { body: AtomicSymbol, join: AtomicSymbol };
type Longest = { body: AtomicSymbol };

type Sequence = { seq: AtomicSymbol[] };
