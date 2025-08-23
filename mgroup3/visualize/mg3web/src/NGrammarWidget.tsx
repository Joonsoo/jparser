export function NGrammarWidget({grammar}: { grammar: NGrammar }) {
  let symbols = [];

  for (let symbolId of Object.keys(grammar.symbols)) {
    let symbol = grammar.symbols[+symbolId]
    symbols.push((<div key={symbolId}>
      <span>{symbolId}</span>: <NAtomicSymbolComponent grammar={grammar} symbol={symbol}></NAtomicSymbolComponent>
    </div>));
  }

  return (<div>
    {symbols}
    sequences = [{Object.keys(grammar.sequences).join(", ")}]
  </div>);
}

export function NAtomicSymbolComponent({grammar, symbol}: { grammar: NGrammar, symbol: NAtomicSymbol }) {
  if ("start" in symbol) {
    return (<div>Start</div>);
  }
  if ("nonterminal" in symbol) {
    let nonterm = symbol.nonterminal;
    return (<div>{nonterm.id} {nonterm.symbol.name}</div>);
  }
  if ("terminal" in symbol) {
    let term = symbol.terminal;
    return (<div>{term.id} {JSON.stringify(term.symbol)}</div>);
  }
  if ("oneOf" in symbol) {
    let oneOf = symbol.oneOf;
    return (<div>{oneOf.id} {oneOf.produces}</div>);
  }
  if ("proxy" in symbol) {
    let proxy = symbol.proxy;
    return (<div>{proxy.id} {proxy.produce}</div>);
  }
  return (<div>TODO {JSON.stringify(symbol)}</div>);
}
