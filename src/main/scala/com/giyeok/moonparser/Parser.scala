package com.giyeok.moonparser

case class ParseResult(parseNode: ParseTree.ParseNode[Symbols.Symbol])

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with GrammarChecker {
    import Inputs._

    object EdgeKind extends Enumeration {
        val Derive, Lift, OnGoing = Value
    }

    abstract class ParsingError {
        val next: Input
        val msg: String
    }
    object ParsingError {
        def apply(_next: Input, _msg: String) = new ParsingError {
            val next = _next
            val msg = _msg
        }
    }
    object ParsingErrors {
        case class UnexpectedInput(next: Input) extends ParsingError {
            val msg = s"Unexpected input at ${next.location}"
        }
    }

    type NodeType = SymbolProgress
    case class EdgeType(from: NodeType, to: NodeType, kind: EdgeKind.Value)
    case class ParsingContext(nodes: Set[NodeType], edges: Set[EdgeType]) {
        def proceedTerminal(next: Input): Either[ParsingContext, ParsingError] = {
            val nextnodes = (nodes collect {
                case s: SymbolProgressTerminal if s accept next =>
                    (s, (s proceedTerminal next).get)
            })
            def createGraph(queue: List[(NodeType, NodeType)], newnodes: Set[NodeType], newedges: Set[EdgeType]): (Set[NodeType], Set[EdgeType]) =
                queue match {
                    case (older, newer) +: tail =>
                        val oldRefs = edges collect {
                            case EdgeType(from, to, _) if to == older => from
                        }
                        ???
                    case Nil => (newnodes, newedges)
                }
            if (nextnodes isEmpty) Right(ParsingErrors.UnexpectedInput(next)) else {
                val newgraph = createGraph(nextnodes.toList, Set(), Set())
                // TODO check newgraph still contains start symbol
                Left(ParsingContext(newgraph._1, newgraph._2))
            }
        }
        def toResult: ParseResult = ParseResult(???)
    }
    object ParsingContext {
        def fromSeeds(seeds: Set[NodeType]): ParsingContext = {
            def expand(queue: List[NodeType], nodes: Set[NodeType], edges: Set[EdgeType]): (Set[NodeType], Set[EdgeType]) =
                queue match {
                    case (head: SymbolProgressNonterminal) +: tail =>
                        assert(nodes contains head)
                        val dests = head.derive
                        val news: Set[SymbolProgress] = dests map { _._1 } filterNot { nodes contains _ }
                        expand(news.toList ++ tail, nodes ++ news, edges ++ (dests map { d => EdgeType(head, d._1, d._2) }))
                    case head +: tail =>
                        expand(tail, nodes, edges)
                    case Nil => (nodes, edges)
                }
            val (nodes, edges) = expand(seeds.toList, seeds, Set())
            ParsingContext(nodes, edges)
        }
    }

    val startingContext = ParsingContext.fromSeeds(Set(SymbolProgress(grammar.startSymbol)))
}
