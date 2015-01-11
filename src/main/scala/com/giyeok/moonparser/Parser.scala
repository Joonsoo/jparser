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
        import ParsingContext.{ collectResultCandidates }

        def proceedTerminal1(next: Input): Set[Lifting] =
            (graph.nodes flatMap {
                case s: SymbolProgressTerminal => (s proceedTerminal next) map { Lifting(s, _, None) }
                case _ => None
            })
        def proceedTerminalVerbose(next: Input): (Either[(ParsingContext, TerminalProceedLog), ParsingError]) = {
            // `nextNodes` is actually type of `Set[(SymbolProgressTerminal, SymbolProgressTerminal)]`
            // but the invariance in `Set` of Scala, which I don't understand why, it is defined as Set[(SymbolProgress, SymbolProgress)]
            println(s"**** New Generation $gen")
            val termLiftings: Set[Lifting] = proceedTerminal1(next)
            if (termLiftings isEmpty) {
                Right(ParsingErrors.UnexpectedInput(next))
            } else {
                val (simpleEdges, assassinEdges) = (graph.simpleEdges, graph.eagerAssassinEdges)

                def chainLiftAndTrackRoots(queue: List[Lifting], newEdgesCC: Set[SimpleEdge], liftingsCC: Set[Lifting]): (Set[SimpleEdge], Set[Lifting]) =
                    queue match {
                        case Lifting(before, after, _) +: rest if after canFinish =>
                            // chainLift and trackRoots
                            val liftings: Set[Lifting] = graph.incomingSimpleEdgesOf(before) flatMap { edge =>
                                edge.from.lift(after) map { Lifting(edge.from, _, Some(after)) }
                            }
                            chainLiftAndTrackRoots(rest ++ (liftings -- liftingsCC), newEdgesCC ++ graph.trackRootsOf(before), liftingsCC ++ liftings)
                        case Lifting(before, after, _) +: rest =>
                            // trackRoots
                            chainLiftAndTrackRoots(rest, newEdgesCC ++ graph.trackRootsOf(before), liftingsCC)
                        case List() => (newEdgesCC, liftingsCC)
                    }
                val (newEdges0, liftings0): (Set[SimpleEdge], Set[Lifting]) = chainLiftAndTrackRoots(termLiftings.toList, Set(), termLiftings)

                def deriveAndImmediateLift(queue: List[Lifting], newEdgesCC: Set[Edge], liftingsCC: Set[Lifting]): (Set[Edge], Set[Lifting]) =
                    queue match {
                        case Lifting(_, after, _) +: rest =>
                            def derive(nodeQueue: List[SymbolProgress], cc: Set[Edge]): Set[Edge] =
                                nodeQueue match {
                                    case (node: SymbolProgressNonterminal) +: rest =>
                                        val derives: Set[Edge] = node.derive(gen + 1) -- cc
                                        derive(rest ++ (derives map { _.to }), cc ++ derives)
                                    case _ +: rest => derive(rest, cc)
                                    case List() => cc
                                }
                            val derives: Set[Edge] = derive(List(after), Set())

                            def chainImmediateLiftings(queue: List[Lifting], cc: Set[Lifting]): Set[Lifting] = {
                                queue match {
                                    case Lifting(_, after, _) +: rest =>
                                        val incomingSimpleEdges = newEdgesCC collect { case e @ SimpleEdge(_, to) if to == after => e }
                                        val lifted: Set[Lifting] =
                                            incomingSimpleEdges flatMap { e => (e.from lift after) map { Lifting(e.from, _, Some(after)) } }
                                        chainImmediateLiftings(rest ++ (lifted -- cc).toList, cc ++ lifted)
                                    case List() => cc
                                }
                            }
                            val immediateLiftings0: Set[Lifting] = derives collect { case e: SimpleEdge if e.to.canFinish => e } flatMap { e =>
                                (e.from lift e.to) map { lifted => Lifting(e.from, lifted, Some(e.to)) }
                            }
                            val immediateLiftings: Set[Lifting] = chainImmediateLiftings(immediateLiftings0.toList, immediateLiftings0)

                            deriveAndImmediateLift(queue ++ (immediateLiftings -- liftingsCC).toList, newEdgesCC ++ derives, liftingsCC ++ immediateLiftings)
                        case List() => (newEdgesCC, liftingsCC)
                    }
                val (newEdges, liftings): (Set[Edge], Set[Lifting]) = deriveAndImmediateLift(liftings0.toList, newEdges0.asInstanceOf[Set[Edge]], liftings0)

                assert(newEdges0.asInstanceOf[Set[Edge]] subsetOf newEdges)
                assert(liftings0 subsetOf liftings)

                val liftingsMap: Map[SymbolProgress, Set[SymbolProgress]] =
                    liftings groupBy { _.before } map { p => (p._1, p._2 map { _.after }) }
                val liftedByMap: Map[SymbolProgress, Set[Lifting]] =
                    liftings groupBy { _.by } collect { case (Some(by), lifting) => (by, lifting) }
                // liftedMap의 value중 값이 하나 이상인 것이 있으면 문법이 ambiguous하다는 뜻일듯

                val newNodes = newEdges flatMap { _.nodes }

                // 현재 세대에 포함된 어쌔신 엣지
                val eagerAssassins = graph.edges filter { _.isInstanceOf[EagerAssassinEdge] }
                // 다음 세대로 넘어가기 전에 타겟이 제거되어야 할 어쌔신 엣지
                val eagerAssassinations: Set[(SymbolProgress, SymbolProgress)] = eagerAssassins flatMap { edge =>
                    // liftedMap의 value가 두 개 이상인 것도 이상한데, 그 중에 일부만 canFinish인건 더더군다나 말이 안되지..
                    if ((liftingsMap contains edge.from) && (liftingsMap(edge.from) exists { _.canFinish })) {
                        val from = liftingsMap(edge.from).iterator.next
                        def traverse(head: SymbolProgress, cc: Set[SymbolProgress]): Set[SymbolProgress] = {
                            (liftedByMap get head match {
                                case Some(set) =>
                                    set.foldLeft(cc) { (cc, lifted) => traverse(lifted.after, cc + lifted.after) }
                                case None => cc
                            }) ++ (liftingsMap get head match {
                                case Some(set) =>
                                    set.foldLeft(cc) { (cc, lifted) => traverse(lifted, cc + lifted) }
                                case None => cc
                            })
                        }
                        traverse(edge.to, Set(edge.to)) map { (from, _) }
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
                    (liftingsMap get e.to) match {
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
                val newctx = ParsingContext(gen + 1, Graph(finalNodes, finalEdges),
                    collectResultCandidates((lifted1 map { _.after }) -- assassinatedNodes))
                Left((newctx, TerminalProceedLog(nextNodes, newEdges, lifted1, eagerAssassinations, newAssassinEdges, newctx)))
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
        private def collectResultCandidates(nodes: Set[SymbolProgress]): Set[SymbolProgress] =
            nodes collect { case s @ NonterminalProgress(sym, Some(_), 0) if sym == grammar.startSymbol => s }

        def fromSeeds(seeds: Set[Node]): ParsingContext = {
            def expand(queue: List[Node], nodes: Set[Node], edges: Set[Edge]): (Set[Node], Set[Edge]) =
                queue match {
                    case (head: SymbolProgressNonterminal) +: tail =>
                        assert(nodes contains head)
                        val newedges = head.derive(0)
                        // TODO derive할 때 역방향으로 lift가 가능한 경우에 대한 처리 추가
                        val news: Set[SymbolProgress] = newedges flatMap { _.nodes } filterNot { nodes contains _ }
                        expand(news.toList ++ tail, nodes ++ news, edges ++ newedges)
                    case head +: tail =>
                        expand(tail, nodes, edges)
                    case Nil => (nodes, edges)
                }
            val (nodes, edges) = expand(seeds.toList, seeds, Set())
            val graph = Graph(nodes, edges)
            val finishable: Set[Lifting] = nodes collect { case n if n.canFinish => Lifting(n, n, None) }
            ParsingContext(0, graph, collectResultCandidates(finishable map { _.after }))
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
