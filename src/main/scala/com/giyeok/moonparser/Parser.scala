package com.giyeok.moonparser

case class ParseResult(parseNode: ParseTree.ParseNode[Symbols.Symbol])

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with SymbolsGraph
        with ParsingErrors {
    import Inputs._

    case class TerminalProceedLog(
        terminalProceeds: Set[(SymbolProgress, SymbolProgress)])

    case class ParsingContext(gen: Int, graph: Graph, resultCandidates: Set[SymbolProgress]) {
        import ParsingContext.{ simpleLift, collectResultCandidates }

        def proceedTerminal1(next: Input): Set[(SymbolProgress, SymbolProgress)] =
            (graph.nodes flatMap {
                case s: SymbolProgressTerminal => (s proceedTerminal next) map { (s, _) }
                case _ => None
            })
        def proceedTerminalVerbose(next: Input): (Either[ParsingContext, ParsingError], TerminalProceedLog) = {
            // `nextNodes` is actually type of `Set[(SymbolProgressTerminal, SymbolProgressTerminal)]`
            // but the invariance in `Set` of Scala, which I don't understand why, it is defined as Set[(SymbolProgress, SymbolProgress)]
            println("**** New Generation")
            val nextNodes = proceedTerminal1(next)
            if (nextNodes isEmpty) {
                (Right(ParsingErrors.UnexpectedInput(next)), TerminalProceedLog(nextNodes))
            } else {
                def trackSurvivors(queue: List[SymbolProgress], cc: Set[SimpleEdge]): Set[SimpleEdge] =
                    queue match {
                        case survivor +: rest =>
                            println("Track survivor:" + survivor.toShortString)
                            val incomings = graph.incomingSimpleEdgesOf(survivor) -- cc
                            trackSurvivors(rest ++ (incomings.toList map { _.from }), cc ++ incomings)
                        case List() => cc
                    }
                def deriveNews(queue: List[SymbolProgress], cc: Set[SimpleEdge]): Set[SimpleEdge] =
                    queue match {
                        case (head: SymbolProgressNonterminal) +: rest =>
                            val derives: Set[SimpleEdge] = (head.derive(gen + 1) collect { case x: SimpleEdge => x }) -- cc
                            deriveNews(rest ++ (derives map { _.to }), derives ++ cc)
                        case _ +: rest => deriveNews(rest, cc)
                        case List() => cc
                    }
                def organizeLifted(queue: List[(SymbolProgress, SymbolProgress)]): Set[SimpleEdge] =
                    queue match {
                        case (o: SymbolProgressNonterminal, n: SymbolProgressNonterminal) +: rest =>
                            val prevIncomings: Set[SimpleEdge] =
                                if (n.derive(gen + 1).isEmpty) Set() else
                                    graph.incomingSimpleEdgesOf(o) flatMap { edge =>
                                        println(s"${edge.from.toShortString} -> ${n.toShortString}")
                                        trackSurvivors(List(edge.from), Set(SimpleEdge(edge.from, n)))
                                    }
                            val derives = n.derive(gen + 1).map(_.to)
                            derives foreach { d =>
                                println(s"${n.toShortString} (derive)-> ${d.toShortString}")
                            }
                            prevIncomings ++ deriveNews(List(n), Set()) ++ organizeLifted(rest)
                        case passed +: rest =>
                            println(s"${passed._1.toShortString} (passed)-> ${passed._2.toShortString}")
                            organizeLifted(rest)
                        case List() => Set()
                    }
                val simpleLifted: Set[(SymbolProgress, SymbolProgress)] = simpleLift(graph, nextNodes.toList, nextNodes)
                println(simpleLifted)
                simpleLifted foreach { case (o, n) => println(s"lifted: ${o.toShortString} --> ${n.toShortString}") }
                // 1. 새로 만든(lift된) 노드로부터 derive할 게 있는 것들은 살린다.
                // 2. 옛날 노드를 향하고 있는 모든 옛날 노드는 살린다.
                val newEdges = organizeLifted(simpleLifted.toList) map { _.asInstanceOf[Edge] }
                println("New edges ***")
                newEdges foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }

                val newNodes = newEdges flatMap { _.nodes }

                val assassins = graph.edges filter { _.isInstanceOf[EagerAssassinEdge] }
                println("Assassins")
                assassins foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                val assassinating = assassins filter { edge => simpleLifted map { _._1 } contains { edge.from } }
                val aliveAssassins = assassins filter { edge => newNodes contains edge.from }
                println("assassinating")
                assassinating foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                println("aliveAssassins")
                aliveAssassins foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }

                val finalEdges = newEdges ++ aliveAssassins
                val finalNodes = finalEdges flatMap { _.nodes }

                println("*** End")
                // TODO check newgraph still contains start symbol
                (Left(ParsingContext(gen + 1, Graph(finalNodes, finalEdges), collectResultCandidates(simpleLifted))),
                    TerminalProceedLog(nextNodes))
            }
        }
        def proceedTerminal(next: Input): Either[ParsingContext, ParsingError] =
            proceedTerminalVerbose(next)._1

        def toResult: Option[ParseResult] = {
            if (resultCandidates.size != 1) None
            else resultCandidates.iterator.next.parsed map { ParseResult(_) }
        }
    }

    object ParsingContext {
        private def simpleLift(graph: Graph, queue: List[(SymbolProgress, SymbolProgress)], cc: Set[(SymbolProgress, SymbolProgress)]): Set[(SymbolProgress, SymbolProgress)] =
            queue match {
                case (oldNode, newNode) +: rest if newNode canFinish =>
                    val incomingSimpleEdges = graph incomingSimpleEdgesOf oldNode
                    val simpleLifted: Set[(SymbolProgress, SymbolProgress)] =
                        incomingSimpleEdges flatMap { e => (e.from lift newNode) map { (e.from, _) } }
                    incomingSimpleEdges foreach { e => println(s"(lifting) ${e.from.toShortString} -> ${e.to.toShortString} by ${newNode.toShortString}") }
                    simpleLift(graph, rest ++ simpleLifted.toList, cc ++ simpleLifted)
                case _ +: rest =>
                    simpleLift(graph, rest, cc)
                case List() => cc
            }

        private def collectResultCandidates(lifted: Set[(SymbolProgress, SymbolProgress)]): Set[SymbolProgress] =
            lifted map { _._2 } collect { case s @ NonterminalProgress(sym, _, 0) if sym == grammar.startSymbol => s }

        def fromSeeds(seeds: Set[Node]): ParsingContext = {
            def expand(queue: List[Node], nodes: Set[Node], edges: Set[Edge]): (Set[Node], Set[Edge]) =
                queue match {
                    case (head: SymbolProgressNonterminal) +: tail =>
                        assert(nodes contains head)
                        val newedges = head.derive(0)
                        val news: Set[SymbolProgress] = newedges flatMap { _.nodes } filterNot { nodes contains _ }
                        expand(news.toList ++ tail, nodes ++ news, edges ++ newedges)
                    case head +: tail =>
                        expand(tail, nodes, edges)
                    case Nil => (nodes, edges)
                }
            val (nodes, edges) = expand(seeds.toList, seeds, Set())
            val graph = Graph(nodes, edges)
            val finishable: Set[(SymbolProgress, SymbolProgress)] = nodes collect { case n if n.canFinish => (n, n) }
            val simpleLifted: Set[(SymbolProgress, SymbolProgress)] = simpleLift(graph, finishable.toList, finishable)
            ParsingContext(0, graph, collectResultCandidates(simpleLifted))
        }
    }

    val startingContext = ParsingContext.fromSeeds(Set(SymbolProgress(grammar.startSymbol, 0)))

    def parse(source: Inputs.Source): Either[ParsingContext, ParsingError] =
        source.foldLeft[Either[ParsingContext, ParsingError]](Left(startingContext)) {
            (ctx, terminal) =>
                ctx match {
                    case Left(ctx) => ctx proceedTerminal terminal
                    case error @ Right(_) => error
                }
        }
    def parse(source: String): Either[ParsingContext, ParsingError] =
        parse(Inputs.fromString(source))
}
