package com.giyeok.moonparser

case class ParseResult(parseNode: ParseTree.ParseNode[Symbols.Symbol])

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with SymbolsGraph
        with ParsingErrors {
    import Inputs._

    case class TerminalProceedLog(
        terminalProceeds: Set[Lifting],
        newEdges: Set[Edge],
        simpleLifted: Set[Lifting],
        eagerAssassinations: Set[(SymbolProgress, SymbolProgress)],
        newAssassinEdges: Set[Edge], //Set[EagerAssassinEdge],
        nextContext: ParsingContext)

    // 이 프로젝트 전체에서 asInstanceOf가 등장하는 경우는 대부분이 Set이 invariant해서 추가된 부분 - covariant한 Set으로 바꾸면 없앨 수 있음
    case class ParsingContext(gen: Int, graph: Graph, resultCandidates: Set[SymbolProgress]) {
        import ParsingContext.{ simpleLift, collectResultCandidates }

        def proceedTerminal1(next: Input): Set[Lifting] =
            (graph.nodes flatMap {
                case s: SymbolProgressTerminal => (s proceedTerminal next) map { Lifting(s, _, None) }
                case _ => None
            })
        def proceedTerminalVerbose(next: Input): (Either[(ParsingContext, TerminalProceedLog), ParsingError]) = {
            // `nextNodes` is actually type of `Set[(SymbolProgressTerminal, SymbolProgressTerminal)]`
            // but the invariance in `Set` of Scala, which I don't understand why, it is defined as Set[(SymbolProgress, SymbolProgress)]
            println(s"**** New Generation $gen")
            val nextNodes = proceedTerminal1(next)
            if (nextNodes isEmpty) {
                Right(ParsingErrors.UnexpectedInput(next))
            } else {
                def trackSurvivors(queue: List[SymbolProgress], cc: Set[SimpleEdge]): Set[SimpleEdge] =
                    queue match {
                        case survivor +: rest =>
                            println("Track survivor:" + survivor.toShortString)
                            val incomings = graph.incomingSimpleEdgesOf(survivor) -- cc
                            trackSurvivors(rest ++ (incomings.toList map { _.from }), cc ++ incomings)
                        case List() => cc
                    }
                def deriveNews(queue: List[SymbolProgress], cc: Set[Edge]): Set[Edge] =
                    queue match {
                        case (head: SymbolProgressNonterminal) +: rest =>
                            val derives: Set[Edge] = head.derive(gen + 1) -- cc
                            deriveNews(rest ++ (derives map { _.to }), derives ++ cc)
                        case _ +: rest => deriveNews(rest, cc)
                        case List() => cc
                    }
                def organizeLifted(queue: List[Lifting]): Set[Edge] =
                    queue match {
                        case Lifting(o: SymbolProgressNonterminal, n: SymbolProgressNonterminal, _) +: rest =>
                            val prevIncomings: Set[SimpleEdge] =
                                if (n.derive(gen + 1).isEmpty) Set() else
                                    graph.incomingSimpleEdgesOf(o) flatMap { edge =>
                                        println(s"${edge.from.toShortString} -> ${n.toShortString}")
                                        trackSurvivors(List(edge.from), Set(SimpleEdge(edge.from, n)))
                                    }
                            prevIncomings ++ deriveNews(List(n), Set()) ++ organizeLifted(rest)
                        case passed +: rest =>
                            println(s"${passed.before.toShortString} (passed)-> ${passed.after.toShortString}")
                            organizeLifted(rest)
                        case List() => Set()
                    }
                val simpleLifted: Set[Lifting] = simpleLift(graph, nextNodes.toList, nextNodes)
                val liftedMap: Map[SymbolProgress, Set[SymbolProgress]] =
                    simpleLifted groupBy { _.before } map { p => (p._1, p._2 map { _.after }) }
                val liftedByMap: Map[SymbolProgress, Set[Lifting]] =
                    simpleLifted groupBy { _.by } collect { case (Some(by), lifting) => (by, lifting) }
                // liftedMap의 value중 값이 하나 이상인 것이 있으면 문법이 ambiguous하다는 뜻일듯
                println(simpleLifted)
                simpleLifted foreach { case Lifting(o, n, by) => println(s"lifted: ${o.toShortString} --> ${n.toShortString} (by ${by map { _.toShortString }}") }
                // 1. 새로 만든(lift된) 노드로부터 derive할 게 있는 것들은 살린다.
                // 2. 옛날 노드를 향하고 있는 모든 옛날 노드는 살린다.
                val newEdges = organizeLifted(simpleLifted.toList) map { _.asInstanceOf[Edge] }
                println("New edges ***")
                newEdges foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }

                val newNodes = newEdges flatMap { _.nodes }

                // 현재 세대에 포함된 어쌔신 엣지
                val eagerAssassins = graph.edges filter { _.isInstanceOf[EagerAssassinEdge] }
                // 다음 세대로 넘어가기 전에 타겟이 제거되어야 할 어쌔신 엣지
                val eagerAssassinations: Set[(SymbolProgress, SymbolProgress)] = eagerAssassins flatMap { edge =>
                    // liftedMap의 value가 두 개 이상인 것도 이상한데, 그 중에 일부만 canFinish인건 더더군다나 말이 안되지..
                    if ((liftedMap contains edge.from) && (liftedMap(edge.from) exists { _.canFinish })) {
                        val from = liftedMap(edge.from).iterator.next
                        def traverse(head: SymbolProgress, cc: Set[SymbolProgress]): Set[SymbolProgress] =
                            liftedByMap get head match {
                                case Some(set) =>
                                    set.foldLeft(cc) { (cc, lifted) => traverse(lifted.after, cc + lifted.after) }
                                case None => cc
                            }
                        traverse(from, Set(edge.to)) map { (from, _) }
                    } else None
                }
                // 다음 세대에서 살아남을 어쌔신 엣지
                val aliveEagerAssassins: Set[Edge] = eagerAssassins filter { edge => newNodes contains edge.from }
                // lift된 것을 반영해서 다음 세대에 사용될 확대된 어쌔신 엣지
                println("liftedByMap")
                liftedByMap foreach { p =>
                    println(p._1.toShortString)
                    p._2 foreach { p =>
                        println("    " + p.before.toShortString + " -> " + p.after.toShortString)
                    }
                }
                val newAssassinEdges: Set[Edge] = newEdges filter { _.isInstanceOf[EagerAssassinEdge] } flatMap { e =>
                    def traverse(head: SymbolProgress, cc: Set[EagerAssassinEdge]): Set[EagerAssassinEdge] =
                        liftedByMap get head match {
                            case Some(set) =>
                                set.foldLeft(cc) { (cc, lifted) => traverse(lifted.after, cc + EagerAssassinEdge(e.from, lifted.after)) }
                            case None => cc
                        }
                    traverse(e.to, Set(e.asInstanceOf[EagerAssassinEdge]))
                }
                println("NewEagerAssassinEdges")
                newEdges filter { _.isInstanceOf[EagerAssassinEdge] } foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                println("ExtendedNewEagerAssassinEdges")
                newAssassinEdges filter { _.isInstanceOf[EagerAssassinEdge] } foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                val newSurvivedAssassinEdges: Set[Edge] = aliveEagerAssassins flatMap { e =>
                    (liftedMap get e.to) match {
                        case Some(lifted) =>
                            val starting = lifted map { Lifting(e.to, _, None) }
                            simpleLift(graph, starting.toList, starting) map { _.after } map { EagerAssassinEdge(e.from, _).asInstanceOf[Edge] }
                        case None =>
                            Set(EagerAssassinEdge(e.from, e.to).asInstanceOf[Edge])
                    }
                }

                println("EagerAssassins")
                eagerAssassins foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                println("EagerAssassinations")
                eagerAssassinations foreach { e => println(s"${e._1.toShortString} -> ${e._2.toShortString}") }
                println(s"AliveEagerAssassins ${aliveEagerAssassins.size}")
                aliveEagerAssassins foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                println(s"NewAssassinEdges ${newAssassinEdges.size}")
                newAssassinEdges foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                println(s"NewSurvivedAssassinEdges ${newSurvivedAssassinEdges.size}")
                newAssassinEdges foreach { e => println(s"${e.from.toShortString} -> ${e.to.toShortString}") }
                println("*** End")

                // 어쌔신 엣지들 중에 to가 이번 세대에서 없어지면 to 노드가 생기는게 아니라, 해당 어쌔신 엣지가 사라지도록 수정해야함
                val assassinEdges: Set[Edge] = aliveEagerAssassins
                val assassinatedNodes: Set[Node] = eagerAssassinations map { _._2 }

                val aliveNewNodes = newNodes -- assassinatedNodes
                val aliveSimpleEdges = newEdges filter { edge => (aliveNewNodes contains edge.from) && (aliveNewNodes contains edge.to) }
                val finalAssassinEdges = newAssassinEdges filter { edge => (aliveNewNodes contains edge.to) }

                val finalEdges: Set[Edge] = aliveSimpleEdges ++ finalAssassinEdges
                val finalNodes: Set[Node] = finalEdges flatMap { _.nodes }

                // TODO check newgraph still contains start symbol
                val newctx = ParsingContext(gen + 1, Graph(finalNodes, finalEdges), collectResultCandidates(finalNodes))
                Left((newctx, TerminalProceedLog(nextNodes, newEdges, simpleLifted, eagerAssassinations, newAssassinEdges, newctx)))
            }
        }
        def proceedTerminal(next: Input): Either[ParsingContext, ParsingError] =
            proceedTerminalVerbose(next) match {
                case Left((ctx, _)) => Left(ctx)
                case Right(error) => Right(error)
            }

        def toResult: Option[ParseResult] = {
            if (resultCandidates.size != 1) None
            else resultCandidates.iterator.next.parsed map { ParseResult(_) }
        }
    }

    case class Lifting(before: SymbolProgress, after: SymbolProgress, by: Option[SymbolProgress])

    object ParsingContext {
        private def simpleLift(graph: Graph, queue: List[Lifting], cc: Set[Lifting]): Set[Lifting] =
            queue match {
                case Lifting(oldNode, newNode, _) +: rest if newNode canFinish =>
                    val incomingSimpleEdges = graph incomingSimpleEdgesOf oldNode
                    val simpleLifted: Set[Lifting] =
                        incomingSimpleEdges flatMap { e => (e.from lift newNode) map { Lifting(e.from, _, Some(newNode)) } }
                    incomingSimpleEdges foreach { e => println(s"(lifting) ${e.from.toShortString} -> ${e.to.toShortString} (by ${newNode.toShortString})") }
                    simpleLift(graph, rest ++ simpleLifted.toList, cc ++ simpleLifted)
                case _ +: rest =>
                    simpleLift(graph, rest, cc)
                case List() => cc
            }

        private def collectResultCandidates(nodes: Set[SymbolProgress]): Set[SymbolProgress] =
            nodes collect { case s @ NonterminalProgress(sym, _, 0) if sym == grammar.startSymbol => s }

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
            val finishable: Set[Lifting] = nodes collect { case n if n.canFinish => Lifting(n, n, None) }
            val simpleLifted: Set[Lifting] = simpleLift(graph, finishable.toList, finishable)
            ParsingContext(0, graph, collectResultCandidates(simpleLifted map { _.after }))
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
