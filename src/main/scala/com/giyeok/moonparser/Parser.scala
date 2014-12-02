package com.giyeok.moonparser

case class ParseResult(parseNode: ParseTree.ParseNode[Symbols.Symbol])

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with SymbolsGraph
        with ParsingErrors
        with GrammarChecker {
    import Inputs._

    case class ParsingContext(graph: Graph) {
        def proceedTerminal(next: Input): Either[ParsingContext, ParsingError] = {
            // `nextNodes` is actually type of `Set[(SymbolProgressTerminal, SymbolProgressTerminal)]`
            // but the invariance in `Set` of Scala, which I don't understand why, it is defined as Set[(SymbolProgress, SymbolProgress)]
            val nextNodes: Set[(SymbolProgress, SymbolProgress)] =
                (graph.nodes flatMap {
                    case s: SymbolProgressTerminal => (s proceedTerminal next) map { (s, _) }
                    case _ => None
                })
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
                def retrackSurvivors(queue: List[SymbolProgress], cc: Set[SimpleEdge]): Set[SimpleEdge] =
                    queue match {
                        case survivor +: rest =>
                            println(survivor.toShortString)
                            val incomings = graph.incomingSimpleEdgesOf(survivor)
                            retrackSurvivors(rest ++ (incomings.toList map { _.from }), cc ++ incomings)
                        case List() => cc
                    }
                def deriveNews(newbie: SymbolProgress): Set[Edge] = // it returns Set[SimpleEdge]
                    newbie match {
                        case newbie: SymbolProgressNonterminal =>
                            val derives: Set[Edge] = newbie.derive
                            derives ++ (derives flatMap { e => deriveNews(e.to) })
                        case _ => Set()
                    }
                def organizeLifted(queue: List[(SymbolProgress, SymbolProgress)]): Set[Edge] = // it returns Set[SimpleEdge]
                    queue match {
                        case (o: SymbolProgressNonterminal, n: SymbolProgressNonterminal) +: rest =>
                            val prevIncomings: Set[Edge] = graph.incomingSimpleEdgesOf(o) flatMap { oi =>
                                (liftedMap get oi.from) match {
                                    case Some(lifted: SymbolProgressNonterminal) =>
                                        if (lifted.derive.map(_.to).map(_.symbol) contains n.symbol) {
                                            println(s"${liftedMap(oi.from).toShortString} (lifted)-> ${n.toShortString}")
                                            // TODO
                                            Set[SimpleEdge]()
                                        } else {
                                            println(s"${oi.from.toShortString} (non-lifted)-> ${n.toShortString}")
                                            // TODO
                                            Set[SimpleEdge](SimpleEdge(lifted, n))
                                        }
                                    case None =>
                                        println(s"${oi.from.toShortString} (survived)-> ${n.toShortString}")
                                        retrackSurvivors(List(oi.from), Set(SimpleEdge(oi.from, n)))
                                }
                            }
                            val derives = n.derive.map(_.to)
                            derives foreach { d =>
                                println(s"${n.toShortString} (derive)-> ${d.toShortString}")
                            }
                            prevIncomings ++ deriveNews(n) ++ organizeLifted(rest)
                        case passed +: rest =>
                            println(s"${passed._1.toShortString} (passed)-> ${passed._2.toShortString}")
                            organizeLifted(rest)
                        case List() => Set()
                    }
                println(simpleLifted)
                simpleLifted foreach { case (o, n) => println(s"${o.toShortString} --> ${n.toShortString}") }
                // 1. 새로 만든(lift된) 노드로부터 derive할 게 있는 것들은 살린다.
                // 2. 옛날 노드를 향하고 있는 모든 옛날 노드는 살린다.
                val edges = organizeLifted(simpleLifted.toList)
                println("New edges ***")
                edges foreach { e =>
                    println(s"${e.from.toShortString} -> ${e.to.toShortString}")
                }
                // TODO check newgraph still contains start symbol
                Left(ParsingContext(Graph(edges flatMap { _.nodes }, edges)))
            }
        }
        def toResult: Option[ParseResult] = {
            val startParsed: Option[SymbolProgress] = graph.nodes.find(node => node.symbol == grammar.startSymbol)
            startParsed.get.parsed map { ParseResult(_) }
        }
    }

    object ParsingContext {
        def fromSeeds(seeds: Set[Node]): ParsingContext = {
            def expand(queue: List[Node], nodes: Set[Node], edges: Set[Edge]): (Set[Node], Set[Edge]) =
                queue match {
                    case (head: SymbolProgressNonterminal) +: tail =>
                        assert(nodes contains head)
                        val newedges = head.derive
                        val news: Set[SymbolProgress] = newedges flatMap { _.nodes } filterNot { nodes contains _ }
                        expand(news.toList ++ tail, nodes ++ news, edges ++ newedges)
                    case head +: tail =>
                        expand(tail, nodes, edges)
                    case Nil => (nodes, edges)
                }
            val (nodes, edges) = expand(seeds.toList, seeds, Set())
            ParsingContext(Graph(nodes, edges))
        }
    }

    val startingContext = ParsingContext.fromSeeds(Set(SymbolProgress(grammar.startSymbol)))

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
