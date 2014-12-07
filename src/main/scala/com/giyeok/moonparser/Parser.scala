package com.giyeok.moonparser

case class ParseResult(parseNode: ParseTree.ParseNode[Symbols.Symbol])

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with SymbolsGraph
        with ParsingErrors {
    import Inputs._

    case class TerminalProceedLog(
        terminalProceeds: Set[(SymbolProgress, SymbolProgress)],
        simpleLifted: Set[(SymbolProgress, SymbolProgress)],
        eagerAssassinations: Set[(SymbolProgress, SymbolProgress)],
        newAssassinEdges: Map[EagerAssassinEdge, Set[Edge]])

    // 이 프로젝트 전체에서 asInstanceOf가 등장하는 경우는 대부분이 Set이 invariant해서 추가된 부분 - covariant한 Set으로 바꾸면 없앨 수 있음
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
            println(s"**** New Generation $gen")
            val nextNodes = proceedTerminal1(next)
            if (nextNodes isEmpty) {
                (Right(ParsingErrors.UnexpectedInput(next)), TerminalProceedLog(nextNodes, Set(), Set(), Map()))
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
                val liftedMap: Map[SymbolProgress, Set[SymbolProgress]] =
                    simpleLifted groupBy { _._1 } map { p => (p._1, p._2 map { _._2 }) }
                // liftedMap의 value중 값이 하나 이상인 것이 있으면 문법이 ambiguous하다는 뜻일듯
                println(simpleLifted)
                simpleLifted foreach { case (o, n) => println(s"lifted: ${o.toShortString} --> ${n.toShortString}") }
                // 1. 새로 만든(lift된) 노드로부터 derive할 게 있는 것들은 살린다.
                // 2. 옛날 노드를 향하고 있는 모든 옛날 노드는 살린다.
                val newEdges = organizeLifted(simpleLifted.toList) map { _.asInstanceOf[Edge] }
                println("New edges ***")
                newEdges foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }

                val newNodes = newEdges flatMap { _.nodes }

                // 현재 세대에 포함된 어쌔신 엣지
                val eagerAssassins = graph.edges filter { _.isInstanceOf[EagerAssassinEdge] }
                // 다음 세대에서 살아남을 어쌔신 엣지
                val aliveEagerAssassins: Set[Edge] = eagerAssassins filter { edge => newNodes contains edge.from }
                // lift된 것을 반영해서 다음 세대에서 확대될 어쌔신 엣지
                val newAssassinEdges: Map[EagerAssassinEdge, Set[Edge]] = (aliveEagerAssassins map { e =>
                    (liftedMap get e.to) match {
                        case Some(lifted) =>
                            val starting = lifted map { (e.to, _) }
                            (e.asInstanceOf[EagerAssassinEdge], (simpleLift(graph, starting.toList, starting) map { _._2 } map { EagerAssassinEdge(e.from, _) }).asInstanceOf[Set[Edge]])
                        case None =>
                            (e.asInstanceOf[EagerAssassinEdge], Set(EagerAssassinEdge(e.from, e.to)).asInstanceOf[Set[Edge]])
                    }
                }).toMap
                // 다음 세대로 넘어가기 전에 타겟이 제거되어야 할 어쌔신 엣지
                val eagerAssassinations: Set[(SymbolProgress, SymbolProgress)] = eagerAssassins flatMap { edge =>
                    // liftedMap의 value가 두 개 이상인 것도 이상한데, 그 중에 일부만 canFinish인건 더더군다나 말이 안되지..
                    if ((liftedMap contains edge.from) && (liftedMap(edge.from) exists { _.canFinish })) {
                        val from = liftedMap(edge.from).iterator.next
                        if (liftedMap contains edge.to) liftedMap(edge.to) map { (from, _) }
                        else Set[(SymbolProgress, SymbolProgress)]((from, edge.to))
                    } else Set[(SymbolProgress, SymbolProgress)]()
                }

                println("EagerAssassins")
                eagerAssassins foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                println("EagerAssassinations")
                eagerAssassinations foreach { e => println(s"${e._1.toShortString} -> ${e._2.toShortString}") }
                println("EagerAliveAssassins")
                aliveEagerAssassins foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                println("NewAssassinEdges")
                newAssassinEdges.toSeq flatMap { _._2 } foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                println("*** End")

                // 어쌔신 엣지들 중에 to가 이번 세대에서 없어지면 to 노드가 생기는게 아니라, 해당 어쌔신 엣지가 사라지도록 수정해야함
                val assassinEdges: Set[Edge] = aliveEagerAssassins ++ (newAssassinEdges.toSeq flatMap { _._2 }).toSet
                val assassinatedNodes: Set[Node] = eagerAssassinations map { _._2 }

                val aliveNewNodes = newNodes -- assassinatedNodes
                val aliveSimpleEdges = newEdges filter { edge => (aliveNewNodes contains edge.from) && (aliveNewNodes contains edge.to) }
                val finalAssassinEdges = assassinEdges filter { edge => (aliveNewNodes contains edge.from) && (aliveNewNodes contains edge.to) }

                val finalEdges: Set[Edge] = aliveSimpleEdges ++ finalAssassinEdges
                val finalNodes: Set[Node] = finalEdges flatMap { _.nodes }
                assert(finalNodes == aliveNewNodes)

                // TODO check newgraph still contains start symbol
                (Left(ParsingContext(gen + 1, Graph(finalNodes, finalEdges), collectResultCandidates(simpleLifted))),
                    TerminalProceedLog(nextNodes, simpleLifted, eagerAssassinations, newAssassinEdges))
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
                    incomingSimpleEdges foreach { e => println(s"(lifting) ${e.from.toShortString} -> ${e.to.toShortString} (by ${newNode.toShortString})") }
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
