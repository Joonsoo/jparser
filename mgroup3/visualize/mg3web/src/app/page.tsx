"use client";

import {useEffect, useState} from "react";
import {NGrammarWidget} from "@/NGrammarWidget";
import {GenParsingContextGraphWidget} from "@/GenParsingContextWidget";

export default function Home() {
  return (<GraphView></GraphView>);
}

export function GrammarView() {
  const [grammar, setGrammar] = useState<NGrammar | null>(null);

  useEffect(() => {
    fetch("http://localhost:8000/grammar").then(async res => {
      setGrammar(await res.json());
    });
  }, []);

  return (<div>{grammar === null ? <div></div> : <NGrammarWidget grammar={grammar}></NGrammarWidget>}</div>)
}

export function GraphView() {
  const [graph, setGraph] = useState<GenParsingContextGraph | null>(null);

  useEffect(() => {
    fetch("http://localhost:8000/graph0").then(async res => {
      setGraph(await res.json());
    });
  }, []);

  return (<div>{graph === null ? <div></div> :
    <GenParsingContextGraphWidget graph={graph}></GenParsingContextGraphWidget>}</div>)
}
