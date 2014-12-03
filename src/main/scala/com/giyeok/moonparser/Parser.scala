package com.giyeok.moonparser

case class ParseResult(parseNode: ParseTree.ParseNode[Symbols.Symbol])

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with SymbolsGraph
        with ParsingErrors
        with GrammarChecker {
    import Inputs._

    case class ParsingContext(gen: Int, graph: Graph, resultCandidates: Set[SymbolProgress]) {
        def proceedTerminal1(next: Input): Set[(SymbolProgress, SymbolProgress)] =
            (graph.nodes flatMap {
                case s: SymbolProgressTerminal => (s proceedTerminal next) map { (s, _) }
                case _ => None
            })
        def proceedTerminal(next: Input): Either[ParsingContext, ParsingError] = {
            // `nextNodes` is actually type of `Set[(SymbolProgressTerminal, SymbolProgressTerminal)]`
            // but the invariance in `Set` of Scala, which I don't understand why, it is defined as Set[(SymbolProgress, SymbolProgress)]
            val nextNodes = proceedTerminal1(next)
            if (nextNodes isEmpty) Right(ParsingErrors.UnexpectedInput(next)) else {
                def simpleLift(queue: List[(SymbolProgress, SymbolProgress)], cc: Set[(SymbolProgress, SymbolProgress)]): Set[(SymbolProgress, SymbolProgress)] =
                    queue match {
                        case (oldNode, newNode) +: rest if newNode canFinish =>
                            val incomingSimpleEdges = graph incomingSimpleEdgesOf oldNode
                            val simpleLifted: Set[(SymbolProgress, SymbolProgress)] =
                                incomingSimpleEdges flatMap { e => (e.from lift newNode) map { (e.from, _) } }
                            simpleLift(rest ++ simpleLifted.toList, cc ++ simpleLifted)
                        case _ +: rest =>
                            simpleLift(rest, cc)
                        case List() => cc
                    }
                val simpleLifted: Set[(SymbolProgress, SymbolProgress)] = simpleLift(nextNodes.toList, nextNodes)
                val liftedMap: Map[SymbolProgress, SymbolProgress] = simpleLifted.toMap
                def trackSurvivors(queue: List[SymbolProgress], cc: Set[SimpleEdge]): Set[SimpleEdge] =
                    queue match {
                        case survivor +: rest =>
                            println("Track survivor:" + survivor.toShortString)
                            val incomings = graph.incomingSimpleEdgesOf(survivor)
                            trackSurvivors(rest ++ (incomings.toList map { _.from }), cc ++ incomings)
                        case List() => cc
                    }
                def deriveNews(newbie: SymbolProgress): Set[SimpleEdge] =
                    newbie match {
                        case newbie: SymbolProgressNonterminal =>
                            val derives: Set[SimpleEdge] = newbie.derive(gen + 1) collect { case x: SimpleEdge => x }
                            derives ++ (derives flatMap { e => deriveNews(e.to) })
                        case _ => Set()
                    }
                def organizeLifted(queue: List[(SymbolProgress, SymbolProgress)]): Set[SimpleEdge] =
                    queue match {
                        case (o: SymbolProgressNonterminal, n: SymbolProgressNonterminal) +: rest =>
                            val prevIncomings: Set[SimpleEdge] = if (n.derive(gen + 1).isEmpty) Set() else
                                graph.incomingSimpleEdgesOf(o) flatMap { oi =>
                                    (liftedMap get oi.from) match {
                                        case Some(lifted: SymbolProgressNonterminal) =>
                                            if (lifted.derive(gen + 1).map(_.to).map(_.symbol) contains n.symbol) {
                                                println(s"${liftedMap(oi.from).toShortString} (lifted)-> ${n.toShortString}")
                                                // TODO
                                                ???
                                            } else {
                                                println(s"${oi.from.toShortString} | ${lifted.toShortString} (non-lifted)-> ${n.toShortString}")
                                                // TODO verify this
                                                trackSurvivors(List(oi.from), Set(SimpleEdge(oi.from, n)))
                                            }
                                        case None =>
                                            println(s"${oi.from.toShortString} (survived)-> ${n.toShortString}")
                                            trackSurvivors(List(oi.from), Set(SimpleEdge(oi.from, n)))
                                    }
                                }
                            val derives = n.derive(gen + 1).map(_.to)
                            derives foreach { d =>
                                println(s"${n.toShortString} (derive)-> ${d.toShortString}")
                            }
                            prevIncomings ++ deriveNews(n) ++ organizeLifted(rest)
                        case passed +: rest =>
                            println(s"${passed._1.toShortString} (passed)-> ${passed._2.toShortString}")
                            organizeLifted(rest)
                        case List() => Set()
                    }
                println("**** New Generation")
                println(simpleLifted)
                simpleLifted foreach { case (o, n) => println(s"lifted: ${o.toShortString} --> ${n.toShortString}") }
                // 1. 새로 만든(lift된) 노드로부터 derive할 게 있는 것들은 살린다.
                // 2. 옛날 노드를 향하고 있는 모든 옛날 노드는 살린다.
                val edges = organizeLifted(simpleLifted.toList) map { _.asInstanceOf[Edge] }
                println("New edges ***")
                edges foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                println("*** End")
                // TODO check newgraph still contains start symbol
                Left(ParsingContext(gen + 1, Graph(edges flatMap { _.nodes }, edges),
                    (simpleLifted map { _._2 } filter { _.symbol == grammar.startSymbol })))
            }
        }
        def toResult: Option[ParseResult] = {
            if (resultCandidates.size != 1) None
            else resultCandidates.iterator.next.parsed map { ParseResult(_) }
        }
    }

    object ParsingContext {
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
            ParsingContext(0, Graph(nodes, edges), Set())
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
