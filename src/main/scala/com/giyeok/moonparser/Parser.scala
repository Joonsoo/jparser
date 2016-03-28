package com.giyeok.moonparser

case class ParseResult(parseNode: ParseTree.ParseNode[Symbols.Symbol])

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with GraphDataStructure
        with ParsingErrors {
    import Inputs._
    import Symbols.Symbol

    sealed trait Lifting {
        val before: SymbolProgress
        val after: SymbolProgress
        def toShortString: String
    }
    case class TermLifting(before: SymbolProgressTerminal, after: SymbolProgressTerminal, by: Input) extends Lifting {
        def toShortString = s"${before.toShortString} => ${after.toShortString} (by ${by.toShortString})"
    }
    case class NontermLifting(before: SymbolProgressNonterminal, after: SymbolProgressNonterminal, by: SymbolProgress) extends Lifting {
        def toShortString = s"${before.toShortString} => ${after.toShortString} (by ${by.toShortString})"
    }

    case class VerboseProceedLog(
        terminalLiftings: Set[Lifting],
        liftings: Set[Lifting],
        newNodes: Set[Node],
        newEdges: Set[Edge],
        rootTips: Set[Node],
        roots: Set[Edge],
        propagatedAssassinEdges: Set[AssassinEdge],
        finalNodes: Set[Node],
        finalEdges: Set[Edge])

    def logging(block: => Unit): Unit = {
        // block
    }
    def logging(str: String): Unit = {
        logging({ println(str) })
    }

    sealed trait ExpandTask
    case class DeriveTask(node: NonterminalNode) extends ExpandTask
    case class LiftTask(lifting: Lifting) extends ExpandTask

    case class ExpandResult(liftings: Set[Lifting], nodes: Set[Node], edges: Set[Edge], rootTips: Set[Node]) {
        def +(n: ExpandResult) = ExpandResult(n.liftings ++ liftings, n.nodes ++ nodes, n.edges ++ edges, n.rootTips ++ rootTips)
        def +(n: (Set[Lifting], Set[Node], Set[Edge], Set[Node])) = ExpandResult(n._1 ++ liftings, n._2 ++ nodes, n._3 ++ edges, n._4 ++ rootTips)
        def withLifting(newLifting: Lifting) = ExpandResult(liftings + newLifting, nodes, edges, rootTips)
        def withLiftings(newLiftings: Set[Lifting]) = ExpandResult(liftings ++ newLiftings, nodes, edges, rootTips)
        def withRootTip(newRootTip: Node) = ExpandResult(liftings, nodes, edges, rootTips + newRootTip)
    }

    def expand(oldGraph: Graph, newGenId: Int, queue: List[ExpandTask], excludingLiftings: Set[Lifting], excludingNodes: Set[Node]): ExpandResult = {
        def expand0(queue: List[ExpandTask], cc: ExpandResult): ExpandResult = {
            val allEdges: Set[Edge] = oldGraph.edges ++ cc.edges
            logging({
                println("left queues:")
                queue foreach { q => println(s"  $q") }
            })
            // TODO queue에 중복된 아이템 2개 들어가지 않도록 수정
            queue match {
                case task +: rest =>
                    task match {
                        case DeriveTask(node) =>
                            logging(s"DeriveTask($node)")

                            assert(cc.nodes contains node)

                            var (nextQueue, nextCC) = (rest, cc)

                            val newDerivedEdges: Set[Edge] = node.derive(newGenId) -- cc.edges
                            val newNodes: Set[Node] = newDerivedEdges.flatMap(_.nodes)

                            logging {
                                newDerivedEdges foreach { edge =>
                                    println("  " + edge)
                                }
                            }

                            nextQueue ++:= newNodes collect { case n: NonterminalNode => DeriveTask(n) }
                            nextCC += (Set(), newNodes, newDerivedEdges, Set())

                            val newLiftings = newDerivedEdges collect {
                                case e: SimpleEdge if e.end.canFinish => node.lift(e.end)
                            }

                            // TODO
                            val allNodes = allEdges flatMap { _.nodes }
                            val alreadyProcessedNodes: Set[NonterminalNode] = newNodes.intersect(allNodes) collect { case n: NonterminalNode => n }
                            val preprocessedNodesLifting: Set[Lifting] = (alreadyProcessedNodes flatMap { n =>
                                val lifters = cc.liftings collect { case NontermLifting(before, _, by) if before == n => by }
                                lifters map { n.lift(_) }
                            }) -- excludingLiftings
                            nextQueue ++:= preprocessedNodesLifting map { LiftTask(_) }
                            nextCC = nextCC.withLiftings(preprocessedNodesLifting)

                            logging {
                                newLiftings foreach { lifting =>
                                    println("  " + lifting)
                                }
                            }

                            nextQueue ++:= newLiftings map { LiftTask(_) }
                            nextCC = nextCC.withLiftings(newLiftings)

                            expand0(nextQueue, nextCC)

                        case LiftTask(TermLifting(before, after, by)) =>
                            logging(s"TermLiftTask($before, $after, $by)")

                            assert(oldGraph.nodes contains before)
                            assert(!before.canFinish)
                            assert(after.canFinish)

                            var (nextQueue, nextCC) = (rest, cc)

                            allEdges.incomingDeriveEdgesOf(before) foreach { edge =>
                                edge match {
                                    case e: SimpleEdge =>
                                        val lifting = e.start.lift(after)
                                        if (!(excludingLiftings contains lifting)) {
                                            nextQueue +:= LiftTask(lifting)
                                            nextCC = nextCC.withLifting(lifting)
                                        }
                                    case e: JoinEdge =>
                                        val constraint: Option[Lifting] = cc.liftings.find { _.before == e.constraint }
                                        if (constraint.isDefined) {
                                            val lifting = e.start.liftJoin(after, constraint.get.after)
                                            if (!(excludingLiftings contains lifting)) {
                                                nextQueue +:= LiftTask(lifting)
                                                nextCC = nextCC.withLifting(lifting)
                                            }
                                        }
                                }
                            }

                            expand0(nextQueue, nextCC)

                        case LiftTask(NontermLifting(before, after, by)) =>
                            logging(s"NontermLiftTask($before, $after, $by)")

                            var (nextQueue, nextCC) = (rest, cc)

                            val incomingDeriveEdges = allEdges.incomingDeriveEdgesOf(before)

                            val afterDerives: Set[Edge] = after.derive(newGenId)
                            if (!afterDerives.isEmpty) {
                                logging("  hasDerives")
                                val newEdges: Set[Edge] = (incomingDeriveEdges map { edge =>
                                    edge match {
                                        case e: SimpleEdge =>
                                            SimpleEdge(e.start, after)
                                        case e: JoinEdge =>
                                            ???
                                    }
                                })
                                nextQueue +:= DeriveTask(after)
                                nextCC += (Set(), Set(after), newEdges, Set(before))
                            }

                            if (after.canFinish) {
                                logging("  isCanFinish")
                                incomingDeriveEdges foreach { edge =>
                                    edge match {
                                        case e: SimpleEdge =>
                                            val lifting = e.start.lift(after)
                                            if (!(excludingLiftings contains lifting)) {
                                                nextQueue +:= LiftTask(lifting)
                                                nextCC = nextCC.withLifting(lifting)
                                            }
                                        case e: JoinEdge =>
                                            ???
                                    }
                                }
                            }

                            expand0(nextQueue, nextCC)
                    }
                case List() => cc
            }
        }
        val initialLiftings: Set[Lifting] = (queue collect { case LiftTask(lifting) => lifting }).toSet
        val initialNodes: Set[Node] = (queue collect { case DeriveTask(node) => node }).toSet
        expand0(queue, ExpandResult(initialLiftings, initialNodes, Set(), Set()))
    }

    def prepareNextAssassinEdges(edges: Set[Edge], liftings: Set[Lifting]): (Set[AssassinEdge], Set[Node], Set[Edge]) = {
        def propagateLiftAssassinEdges(queue: List[LiftAssassinEdge], newEdgesCC: Set[LiftAssassinEdge]): Set[LiftAssassinEdge] =
            queue match {
                case head +: rest =>
                    val liftedBy = liftings collect { case x @ NontermLifting(_, _, by) if by == head.end => x }
                    // val liftedBy = liftings filter { _.by == Some(head.to) }
                    val newAssassinEdges = (liftedBy map { lifting => LiftAssassinEdge(head.start, lifting.after) }) -- newEdgesCC
                    propagateLiftAssassinEdges(rest ++ newAssassinEdges.toList, newEdgesCC ++ newAssassinEdges)
                case List() => newEdgesCC
            }

        def propagateEagerAssassinEdges(assassinEdges: Set[EagerAssassinEdge]): Set[EagerAssassinEdge] = {
            val propagated: Map[Set[EagerAssassinEdge], Set[Node]] = assassinEdges groupBy { _.end } map { p =>
                assert(p._2 forall { _.end == p._1 })
                val sameDestinationEdges = p._2
                def propagate(queue: List[Node], nodesCC: Set[Node], liftingsCC: Set[Lifting], edgesCC: Set[SimpleEdge]): Set[Node] =
                    queue match {
                        case head +: rest =>
                            val affectedLiftings = liftings collect { case x @ NontermLifting(_, _, by) if by == head => x }
                            // val affectedLiftings = liftings filter { _.by == Some(head) }
                            val affectedEdges = edges.outgoingSimpleEdgesOf(head)
                            val newLiftingsCC = liftingsCC ++ affectedLiftings
                            val newEdgesCC = edgesCC ++ affectedEdges
                            val affectedNodesByLiftings: Set[NonterminalNode] = affectedLiftings map { _.after } filter { n => liftingsCC filter { _.after == n } subsetOf newLiftingsCC }
                            // affectedNodesByLiftings: affectedLiftings의 after 중에서 after가 자기에게로 향하는 lifting이 모두 newLiftingsCC에 포함된 노드
                            val affectedNodesByEdges: Set[Node] = affectedEdges map { _.end } filter { n => edges.incomingSimpleEdgesOf(n) subsetOf newEdgesCC }
                            // affectedNodesByEdges: affectedEdges의 to들 중에서 자기에게로 향하는 SimpleEdge가 모두 newEdgesCC에 포함된 노드
                            val newAffectedNodes = (affectedNodesByLiftings ++ affectedNodesByEdges) -- nodesCC
                            propagate(rest ++ newAffectedNodes.toList, nodesCC ++ newAffectedNodes, newLiftingsCC, newEdgesCC)
                        case List() => nodesCC
                    }
                (sameDestinationEdges, propagate(sameDestinationEdges.map(_.end).toList, Set(), Set(), Set()))
            }

            (propagated flatMap { p =>
                val (origins, affecteds) = p
                origins flatMap { origin => affecteds map { to => EagerAssassinEdge(origin.start, to) } }
            }).toSet
        }

        val propagatedAssassinEdges0 = propagateLiftAssassinEdges(edges.liftAssassinEdges.toList, Set()).asInstanceOf[Set[AssassinEdge]]
        val propagatedAssassinEdges1 = propagateEagerAssassinEdges(edges.eagerAssassinEdges).asInstanceOf[Set[AssassinEdge]]
        val propagatedAssassinEdges: Set[AssassinEdge] = propagatedAssassinEdges0 ++ propagatedAssassinEdges1

        // TODO outgoing edges of assassin targets
        // TODO lifting before?

        val nodes1 = edges flatMap { _.nodes }

        val edges2 = edges ++ propagatedAssassinEdges
        val edges3 = edges2 filter { e => (nodes1 contains e.start) && (nodes1 contains e.end) }

        val nodes2 = edges3 flatMap { _.nodes }

        (propagatedAssassinEdges, nodes2, edges3)
    }

    def collectResultCandidates(liftings: Set[Lifting]): Set[Node] =
        liftings map { _.after } filter { _.symbol == grammar.startSymbol } collect {
            case n: SymbolProgressNonterminal if n.derivedGen == 0 && n.canFinish => n
        }

    // 이 프로젝트 전체에서 asInstanceOf가 등장하는 경우는 대부분이 Set이 invariant해서 추가된 부분 - covariant한 Set으로 바꾸면 없앨 수 있음
    case class ParsingContext(gen: Int, graph: Graph, resultCandidates: Set[SymbolProgress]) {
        def proceedTerminal1(next: Input): Set[Lifting] =
            (graph.nodes flatMap {
                case s: SymbolProgressTerminal => (s proceedTerminal next) map { TermLifting(s, _, next) }
                case _ => None
            })
        def proceedTerminalVerbose(next: Input): (Either[(ParsingContext, VerboseProceedLog), ParsingError]) = {
            // `nextNodes` is actually type of `Set[(SymbolProgressTerminal, SymbolProgressTerminal)]`
            // but the invariance of `Set` of Scala, which I don't understand why, it is defined as Set[(SymbolProgress, SymbolProgress)]
            logging {
                println(s"**** New Generation $gen -> ${gen + 1}")

                graph.edges foreach { edge => println(edge.toShortString) }
                println()
            }

            val terminalLiftings: Set[Lifting] = proceedTerminal1(next)
            if (terminalLiftings isEmpty) {
                Right(ParsingErrors.UnexpectedInput(next))
            } else {
                val expandFn = expand(graph, gen + 1, terminalLiftings.toList map { lifting => LiftTask(lifting) }, _: Set[Lifting], _: Set[Node])

                val ExpandResult(liftings0, newNodes0, newEdges0, rootTips0) = expandFn(Set(), Set())

                val (liftings, newNodes, newEdges, rootTips, roots) = {
                    // assert(rootTips subsetOf graph.nodes)

                    assert(terminalLiftings subsetOf liftings0)
                    val activeAssassinEdges = graph.edges.assassinEdges filter { e => liftings0 map { _.before } contains e.start }

                    if (activeAssassinEdges.isEmpty) {
                        val roots = rootTips0 flatMap { rootTip => graph.edges.rootsOf(rootTip) }
                        (liftings0, newNodes0, newEdges0, rootTips0, roots)
                    } else {
                        val activeLiftAssassinEdges = activeAssassinEdges.liftAssassinEdges
                        val activeEagerAssassinEdges = activeAssassinEdges.eagerAssassinEdges

                        val blockingLiftings = liftings0 filter { l => activeAssassinEdges map { _.end } contains l.before }
                        logging {
                            if (!blockingLiftings.isEmpty) {
                                println("- blocked liftings by assassin edges")
                                blockingLiftings foreach { lifting => println(lifting.toShortString) }
                                println("- blocked terminal liftings")
                                (blockingLiftings & terminalLiftings) foreach { lifting => println(lifting.toShortString) }
                            }
                        }

                        val assassinatedNodes = activeEagerAssassinEdges map { _.end }
                        logging {
                            if (!assassinatedNodes.isEmpty) {
                                println("- blocked nodes by assassin edges")
                                assassinatedNodes foreach { node => println(node.toShortString) }
                            }
                        }

                        // assassinatedNodes는 expand할 때도 무시하고, roots에서도 제외해야 함

                        val ExpandResult(liftings1, newNodes1, newEdges1, rootTips1) = expandFn(blockingLiftings, assassinatedNodes)
                        val roots1 = rootTips1 flatMap { rootTip => graph.edges.rootsOf(rootTip) }

                        val roots = roots1 filterNot { assassinatedNodes contains _.start } filterNot { assassinatedNodes contains _.end }

                        (liftings1, newNodes1, newEdges1, rootTips1, roots)
                    }
                }

                val (propagatedAssassinEdges, finalNodes, finalEdges) = prepareNextAssassinEdges(newEdges ++ roots, liftings)

                logging {
                    println("- liftings")
                    liftings foreach { lifting => println(lifting.toShortString) }
                    println("- newNodes")
                    newNodes foreach { node => println(node.toShortString) }
                    println("- newEdges")
                    newEdges foreach { edge => println(edge.toShortString) }
                    println("- rootTips")
                    rootTips foreach { rootTip => println(rootTip.toShortString) }

                    println("- roots")
                    roots foreach { edge => println(edge.toShortString) }

                    println("=== Edges before assassin works ===")
                    newEdges0 foreach { edge => println(edge.toShortString) }
                    println("============ End of generation =======")
                }

                val nextParsingContext = ParsingContext(gen + 1, Graph(finalNodes, finalEdges), collectResultCandidates(liftings))
                val verboseProceedLog = VerboseProceedLog(terminalLiftings, liftings, newNodes, newEdges, rootTips, roots, propagatedAssassinEdges, finalNodes, finalEdges)
                Left((nextParsingContext, verboseProceedLog))
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

    def assertForAll[T](set: Iterable[T], p: T => Boolean): Unit = {
        val failedSet = set filterNot { p(_) }
        if (!failedSet.isEmpty) {
            println(failedSet)
            assert(failedSet.isEmpty)
        }
    }

    object ParsingContext {
        def fromSeedVerbose(seed: Symbol): (ParsingContext, VerboseProceedLog) = {
            val startProgress = SymbolProgress(seed, 0)
            assert(startProgress.isInstanceOf[SymbolProgressNonterminal])
            val ExpandResult(liftings, nodes, edges, _) = expand(Graph(Set(), Set()), 0, List(DeriveTask(startProgress.asInstanceOf[NonterminalNode])), Set(), Set())
            // expand2(seeds.toList, seeds, Set(), Set())
            val (propagatedAssassinEdges, finalNodes, finalEdges) = prepareNextAssassinEdges(edges, liftings)
            val graph = Graph(finalNodes, finalEdges)

            logging {
                println("- nodes")
                nodes.toSeq.sortBy { _.id } foreach { node => println(node.toShortString) }
                println("- edges")
                edges.toSeq.sortBy { e => (e.start.id, e.end.id) } foreach { edge => println(edge.toShortString) }
                println("- liftings")
                liftings.toSeq.sortBy { l => (l.before.id, l.after.id) } foreach { lifting =>
                    println(lifting.toShortString)
                }
            }

            assert(graph.nodes contains startProgress)
            assertForAll[SymbolProgressNonterminal](graph.nodes collect { case x: SymbolProgressNonterminal => x }, { _.derive(0) subsetOf graph.edges })
            assert(graph.edges forall { _.nodes subsetOf graph.nodes })
            assert((graph.edges flatMap { _.nodes }) == graph.nodes)
            // assert(liftings filter { _.after.canFinish } forall { lifting => graph.edges.incomingSimpleEdgesOf(lifting.before) map { _.start } map { _.lift(lifting.after) } subsetOf liftings })
            assert(liftings filter { !_.after.canFinish } forall { lifting =>
                graph.edges.rootsOf(lifting.before).asInstanceOf[Set[Edge]] subsetOf graph.edges
            })
            // lifting의 after가 derive가 있는지 없는지에 따라서도 다를텐데..
            //assert(liftings collect { case l @ Lifting(_, after: SymbolProgressNonterminal, _) if !after.canFinish => l } filter { _.after.asInstanceOf[SymbolProgressNonterminal].derive(0).isEmpty } forall { lifting =>
            // graph.edges.rootsOf(lifting.before).asInstanceOf[Set[Edge]] subsetOf graph.edges
            //})

            // val finishable: Set[Lifting] = nodes collect { case n if n.canFinish => Lifting(n, n, None) }

            val startingContext = ParsingContext(0, graph, collectResultCandidates(liftings))
            val verboseProceedLog = VerboseProceedLog(Set(), liftings, nodes, edges, Set(), Set(), propagatedAssassinEdges, Set(), Set())
            (startingContext, verboseProceedLog)
        }
        def fromSeed(seed: Symbol): ParsingContext = fromSeedVerbose(seed)._1
    }

    val startingContextVerbose = ParsingContext.fromSeedVerbose(grammar.startSymbol)
    val startingContext = startingContextVerbose._1

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
