package com.giyeok.moonparser

import Inputs.Input

case class ParseResult(parseNode: ParseTree.ParseNode[Symbols.Symbol])

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with GraphDataStructure
        with ParsingErrors {

    trait Lifting {
        val before: SymbolProgress
        val after: SymbolProgress
    }
    case class NontermLifting(before: SymbolProgress, after: SymbolProgress, edge: Edge, symbols: Set[SymbolProgress]) extends Lifting
    case class TermLifting(before: SymbolProgress, after: SymbolProgress, by: Input) extends Lifting

    case class ParsingContext(gen: Int, graph: Graph, resultCandidates: Set[SymbolProgress]) {
        def proceedTerminal(next: Input): Either[ParsingContext, ParsingError] = ???
    }

    case class ProceedLog()

    trait ExpandQueueItem
    /**
     * ExpandQueueItem은 기본적으로 이전 세대->다음 세대로 갈 때 노드의 변화를 나타낸다.
     * NodeInitial은 파싱을 처음 시작해서 이전 세대의 상태가 없을 때에만(즉 fromSeeds 메소드에서만) 사용된다.
     * 그 외의 경우에는 모두 NodeAdvance가 사용된다.
     */
    case class NodeInitial(node: Node) extends ExpandQueueItem
    case class NodeAdvance(before: Node, after: Node) extends ExpandQueueItem

    case class ExpandResult(liftings: Set[Lifting], nodes: Set[Node], edges: Set[Edge], rootTips: Set[Node])
    def expand(oldEdges: Set[Edge], initials: List[ExpandQueueItem], nextGen: Int, excludingLiftings: Set[Lifting], excludingNodes: Set[Node]): ExpandResult = {
        def expand0(queue: List[ExpandQueueItem], cc: ExpandResult): ExpandResult = {
            queue match {
                case NodeInitial(node: NonterminalNode) +: rest =>
                    // fromSeeds에서 호출되어 온 경우
                    var ExpandResult(liftingsCC: Set[Lifting], nodesCC: Set[Node], edgesCC: Set[Edge], rootTipsCC: Set[Node]) = cc
                    val deriveEdges = node.derive(nextGen)
                    val deriveNodes = deriveEdges flatMap { _.ends }
                    expand0(rest, ExpandResult(liftingsCC, nodesCC, edgesCC, rootTipsCC))
                case NodeInitial(node: TerminalNode) +: rest => expand0(rest, cc)
                case List() => cc
            }
        }
        expand0(initials, ExpandResult(Set(), Set(), oldEdges, Set()))
    }

    object ParsingContext {
        def fromSeedsLog(seeds: Set[Symbols.Nonterminal]): (ParsingContext, ProceedLog) = {
            expand(Set(), seeds.toList map { symbol => NodeInitial(SymbolProgress(symbol, 0)) }, 0, Set(), Set())
            ???
        }
        def fromSeeds(seeds: Set[Symbols.Nonterminal]): ParsingContext = fromSeedsLog(seeds)._1
    }

    val startingContextLog = ParsingContext.fromSeedsLog(Set(grammar.startSymbol))
    val startingContext = startingContextLog._1

    def parse(source: Inputs.Source): Either[ParsingContext, ParsingError] = {
        source.foldLeft[Either[ParsingContext, ParsingError]](Left(startingContext)) {
            (ctx, terminal) =>
                ctx match {
                    case Left(ctx) => ctx proceedTerminal terminal
                    case error @ Right(_) => error
                }
        }
    }
    def parse(source: String): Either[ParsingContext, ParsingError] = {
        parse(Inputs.fromString(source))
    }
}
