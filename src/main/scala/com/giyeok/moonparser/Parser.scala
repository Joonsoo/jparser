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
            // but the invariance of `Set` of Scala, which I don't understand why, it is defined as Set[(SymbolProgress, SymbolProgress)]
            println(s"**** New Generation $gen")
            val terminalLiftings: Set[Lifting] = proceedTerminal1(next)
            if (terminalLiftings isEmpty) {
                Right(ParsingErrors.UnexpectedInput(next))
            } else {
                val (simpleEdges, eagerAssassinEdges) = (graph.edges.simpleEdges, graph.edges.eagerAssassinEdges)

                def chainLiftAndTrackRoots(queue: List[Lifting], newEdgesCC: Set[SimpleEdge], liftingsCC: Set[Lifting]): (Set[SimpleEdge], Set[Lifting]) =
                    queue match {
                        case Lifting(before, after, _) +: rest if after canFinish =>
                            // chainLift and trackRoots
                            val liftings: Set[Lifting] = graph.edges.incomingSimpleEdgesOf(before) map { edge =>
                                edge.from.lift(after)
                            }
                            chainLiftAndTrackRoots(rest ++ (liftings -- liftingsCC), newEdgesCC ++ graph.edges.rootsOf(before), liftingsCC ++ liftings)
                        case Lifting(before, after, _) +: rest =>
                            // trackRoots
                            chainLiftAndTrackRoots(rest, newEdgesCC ++ graph.edges.rootsOf(before), liftingsCC)
                        case List() => (newEdgesCC, liftingsCC)
                    }
                val (newEdges0, liftings0): (Set[SimpleEdge], Set[Lifting]) = chainLiftAndTrackRoots(terminalLiftings.toList, Set(), terminalLiftings)

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
                                            incomingSimpleEdges map { e => e.from lift after }
                                        chainImmediateLiftings(rest ++ (lifted -- cc).toList, cc ++ lifted)
                                    case List() => cc
                                }
                            }
                            val immediateLiftings0: Set[Lifting] = derives collect { case e: SimpleEdge if e.to.canFinish => e } map { e =>
                                (e.from lift e.to)
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
                            // simpleLift(graph, starting.toList, starting) map { _.after } map { EagerAssassinEdge(e.from, _).asInstanceOf[Edge] }
                            ???
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
                //val newctx = ParsingContext(gen + 1, Graph(finalNodes, finalEdges),
                //    collectResultCandidates((lifted1 map { _.after }) -- assassinatedNodes))
                //Left((newctx, TerminalProceedLog(nextNodes, newEdges, lifted1, eagerAssassinations, newAssassinEdges, newctx)))
                ???
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

    implicit class AugEdges(edges: Set[Edge]) {
        def simpleEdges: Set[SimpleEdge] = edges collect { case e: SimpleEdge => e }
        def eagerAssassinEdges: Set[EagerAssassinEdge] = edges collect { case e: EagerAssassinEdge => e }

        def incomingSimpleEdgesOf(node: Node): Set[SimpleEdge] = simpleEdges filter { _.to == node }
        def outgoingEdges(node: Node): Set[Edge] = ???

        def rootsOf(node: Node): Set[SimpleEdge] = {
            def trackRoots(queue: List[SymbolProgress], cc: Set[SimpleEdge]): Set[SimpleEdge] =
                queue match {
                    case node +: rest =>
                        val incomings = incomingSimpleEdgesOf(node) -- cc
                        trackRoots(rest ++ (incomings.toList map { _.from }), cc ++ incomings)
                    case List() => cc
                }
            trackRoots(List(node), Set())
        }
    }

    object ParsingContext {
        private def collectResultCandidates(nodes: Set[SymbolProgress]): Set[SymbolProgress] =
            nodes collect { case s @ NonterminalProgress(sym, Some(_), 0) if sym == grammar.startSymbol => s }

        def expand(queue: List[Node], nodesCC: Set[Node], edgesCC: Set[Edge], liftingsCC: Set[Lifting]): (Set[Node], Set[Edge], Set[Lifting]) = {
            def immediateLifting(node: Node, edges: Set[Edge]): (Set[Lifting], Set[Edge]) = {
                def immediateLifting0(queue: List[(SymbolProgressNonterminal, SymbolProgress)], liftingsCC: Set[Lifting], edgesCC: Set[Edge]): (Set[Lifting], Set[Edge]) = queue match {
                    case (from, to) +: tail if to.canFinish =>
                        println(s"Trying zeroLifting for ${to.toShortString}  (<- ${from.toShortString})")
                        val newLifting = from.lift(to)
                        println(s"${newLifting.before.toShortString} -> ${newLifting.after.toShortString}  (by ${newLifting.by map { _.toShortString }})")
                        val newIncomings = edgesCC.incomingSimpleEdgesOf(from) map { edge => (edge.from, newLifting.after) }
                        newIncomings foreach { e =>
                            println(s"${e._1.toShortString} -> ${e._2.toShortString}")
                        }
                        val newEdgesCC = to match {
                            case nonterm: SymbolProgressNonterminal if !nonterm.derive(0).isEmpty => edgesCC + SimpleEdge(from, to)
                            case _ => edgesCC
                        }
                        immediateLifting0(tail ++ (newIncomings.toList), liftingsCC + newLifting, newEdgesCC)
                    case (from, to) +: tail =>
                        // assert (!to.derive(0).isEmpty)
                        immediateLifting0(tail, liftingsCC, edgesCC + SimpleEdge(from, to))
                    case List() => (liftingsCC, edgesCC)
                }
                val result = edges.incomingSimpleEdgesOf(node) map { edge => immediateLifting0(List((edge.from, edge.to)), Set(), edges) }
                result.foldLeft((Set[Lifting](), edges))((m, i) => (m._1 ++ i._1, m._2 ++ i._2))
            }
            queue match {
                case node +: tail =>
                    println(s"expand ${node.toShortString}")
                    assert(nodesCC contains node)
                    val (newEdges: Set[Edge], newNodes: Set[Node]) = node match {
                        case node: SymbolProgressNonterminal =>
                            val derived: Set[Edge] = node.derive(0)
                            (derived, (derived flatMap { _.nodes }))
                        case node => (Set(), Set())
                    }
                    newEdges foreach { e => println(s"${e.from.toShortString} -(derive)-> ${e.to.toShortString}") }
                    val immediateLiftings = immediateLifting(node, newEdges ++ edgesCC)

                    // zeroLiftings 중 after.derive가 있는 경우엔 before의 root들을 포함시켜주어야 한다
                    val liftingRoots = (immediateLiftings._1 collect {
                        case Lifting(before, after: SymbolProgressNonterminal, _) if !after.derive(0).isEmpty =>
                            (newEdges ++ edgesCC).rootsOf(before)
                    }).flatten

                    val newDerivables: Set[SymbolProgress] = immediateLiftings._1 collect { case l @ Lifting(_, after: SymbolProgressNonterminal, _) if !after.derive(0).isEmpty => l.after }
                    val liftingRootNodes = liftingRoots flatMap { _.nodes }

                    // (newEdges ++ liftingRoots -- edgesCC) 중에서 to가 nodesCC에 포함된 SimpleEdge를 다시 처리해야 함
                    expand(tail ++ ((newNodes ++ newDerivables -- nodesCC).toList), newNodes ++ newDerivables ++ liftingRootNodes ++ nodesCC, newEdges ++ liftingRoots ++ immediateLiftings._2 ++ edgesCC, immediateLiftings._1 ++ liftingsCC)
                case Nil => (nodesCC, edgesCC, liftingsCC)
            }
        }
        def fromSeeds(seeds: Set[Node]): ParsingContext = {
            val (nodes, edges, liftings) = expand(seeds.toList, seeds, Set(), Set())
            val graph = Graph(nodes, edges)

            liftings foreach { lifting =>
                println(s"${lifting.before.toShortString} -(liftTo)-> ${lifting.after.toShortString}  (by ${lifting.by map { _.toShortString }})")
            }
            println()

            assert(seeds subsetOf graph.nodes)
            assert(graph.nodes collect { case x: SymbolProgressNonterminal => x } forall { _.derive(0) subsetOf graph.edges })
            assert(graph.edges forall { _.nodes subsetOf graph.nodes })
            assert((graph.edges flatMap { _.nodes }) == graph.edges)
            assert(liftings filter { _.after.canFinish } forall { lifting =>
                graph.edges.incomingSimpleEdgesOf(lifting.before) map { _.from } map { _.lift(lifting.after) } subsetOf liftings
            })
            assert(liftings filter { !_.after.canFinish } forall { lifting =>
                graph.edges.rootsOf(lifting.before).asInstanceOf[Set[Edge]] subsetOf graph.edges
            })
            // lifting의 after가 derive가 있는지 없는지에 따라서도 다를텐데..
            assert(liftings collect { case l @ Lifting(_, after: SymbolProgressNonterminal, _) if !after.canFinish => l } filter { _.after.asInstanceOf[SymbolProgressNonterminal].derive(0).isEmpty } forall { lifting =>
                graph.edges.rootsOf(lifting.before).asInstanceOf[Set[Edge]] subsetOf graph.edges
            })

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
