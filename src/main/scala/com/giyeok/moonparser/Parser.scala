package com.giyeok.moonparser

case class ParseResult(parseNode: ParseTree.ParseNode[Symbols.Symbol])

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with SymbolsGraph
        with GrammarChecker {
    import Inputs._

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

    case class ParsingContext(nodes: Set[Node], edges: Set[Edge]) {
        def proceedTerminal(next: Input): Either[ParsingContext, ParsingError] = {
            val nextnodes = (nodes collect {
                case s: SymbolProgressTerminal if s accept next =>
                    (s, (s proceedTerminal next).get)
            })
            println(nextnodes)
            if (nextnodes isEmpty) Right(ParsingErrors.UnexpectedInput(next)) else {
                val newgraph: (Set[Node], Set[Edge]) = ???
                // TODO check newgraph still contains start symbol
                Left(ParsingContext(newgraph._1, newgraph._2))
            }
        }
        def toResult: ParseResult = ParseResult(???)
    }
    object ParsingContext {
        def fromSeeds(seeds: Set[Node]): ParsingContext = {
            def expand(queue: List[Node], nodes: Set[Node], edges: Set[Edge]): (Set[Node], Set[Edge]) =
                queue match {
                    case (head: SymbolProgressNonterminal) +: tail =>
                        assert(nodes contains head)
                        val newedges = head.derive map { _.toEdge(head) }
                        val news: Set[SymbolProgress] = newedges flatMap { _.nodes } filterNot { nodes contains _ }
                        expand(news.toList ++ tail, nodes ++ news, edges ++ newedges)
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
