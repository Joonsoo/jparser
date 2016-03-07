package com.giyeok.moonparser

case class ParseResult(parseNode: ParseTree.ParseNode[Symbols.Symbol])

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with GraphDataStructure
        with ParsingErrors {
    import Inputs._

    trait Lifting {
        val before: SymbolProgress
        val after: SymbolProgress
        def toShortString: String
    }
    case class TermLifting(before: SymbolProgress, after: SymbolProgress, by: Input) extends Lifting {
        def toShortString = s"${before.toShortString} => ${after.toShortString} (by ${by.toShortString})"
    }
    case class NontermLifting(before: SymbolProgress, after: SymbolProgress, by: SymbolProgress) extends Lifting {
        def toShortString = s"${before.toShortString} => ${after.toShortString} (by ${by.toShortString})"
    }

    case class VerboseProceedLog(
        terminalLiftings: Set[Lifting],
        liftings: Set[Lifting],
        newNodes: Set[Node],
        newEdges: Set[Edge],
        rootTips: Set[Node],
        roots: Set[Edge],
        propagatedAssassinEdges: Set[AssassinEdge0],
        finalNodes: Set[Node],
        finalEdges: Set[Edge])

    def logging(block: => Unit): Unit = {
        // block
    }

    sealed trait ExpandItem
    case class ExpandNode(node: Node) extends ExpandItem
    case class ExpandAdvance(before: Node, after: Node) extends ExpandItem

    case class ExpandResult(liftings: Set[Lifting], nodes: Set[Node], edges: Set[Edge], rootTips: Set[Node])

    def expand(oldEdges: Set[Edge], nextGen: Int, initials: List[ExpandItem], cc: ExpandResult): ExpandResult =
        expand(oldEdges, nextGen, initials, cc, Set(), Set())
    def expand(oldEdges: Set[Edge], nextGen: Int, initials: List[ExpandItem], cc: ExpandResult, excludingLiftings: Set[Lifting], excludingNodes: Set[Node]): ExpandResult = {
        def expand0(queue: List[ExpandItem], cc: ExpandResult): ExpandResult = {
            // TODO excludingNodes
            queue match {
                case ExpandNode(_: SymbolProgressTerminal) +: rest => expand0(rest, cc)
                case ExpandNode(node: SymbolProgressNonterminal) +: rest =>
                    var nextQueue = rest
                    var ExpandResult(nextLiftingsCC, nextNewNodesCC, nextNewEdgesCC, nextRootTipsCC) = cc
                    val allEdgesSoFar = oldEdges ++ nextNewEdgesCC

                    val afterDerives = node.asInstanceOf[SymbolProgressNonterminal].derive(nextGen)
                    if (!afterDerives.isEmpty) {
                        def recursiveDerive(queue: List[Edge], newNodesCC: Set[Node], newEdgesCC: Set[Edge]): (Set[Node], Set[Edge]) = queue match {
                            case edge +: rest =>
                                (edge.from, edge.to) match {
                                    case (from: NonterminalNode, to) if to.canFinish =>
                                        val newLifting = from lift to
                                        if (!(excludingLiftings contains newLifting)) {
                                            nextQueue +:= ExpandAdvance(newLifting.before, newLifting.after)
                                            nextLiftingsCC += newLifting
                                        }
                                    case _ => // nothing to do
                                }
                                edge match {
                                    case SimpleEdge(from, to: NonterminalNode) =>
                                        val newDerives = (to derive nextGen) -- newEdgesCC
                                        recursiveDerive(rest ++ newDerives, newNodesCC ++ edge.nodes, newEdgesCC + edge)
                                    case e: AssassinEdge0 if e.from.isInstanceOf[NonterminalNode] =>
                                        val from = e.from.asInstanceOf[NonterminalNode]
                                        val newDerives = (from derive nextGen) -- newEdgesCC
                                        recursiveDerive(rest ++ newDerives, newNodesCC ++ edge.nodes, newEdgesCC + edge)
                                    case _ =>
                                        recursiveDerive(rest, newNodesCC ++ edge.nodes, newEdgesCC + edge)
                                }
                            case List() => (newNodesCC, newEdgesCC)
                        }
                        val (derivedNodes, derivedEdges) = recursiveDerive(afterDerives.toList, Set(), Set())
                        nextNewNodesCC ++= derivedNodes
                        nextNewEdgesCC ++= derivedEdges
                    }
                    expand0(nextQueue, ExpandResult(nextLiftingsCC, nextNewNodesCC, nextNewEdgesCC, nextRootTipsCC))
                case ExpandAdvance(before, after) +: rest =>
                    var nextQueue = rest
                    var ExpandResult(nextLiftingsCC, nextNewNodesCC, nextNewEdgesCC, nextRootTipsCC) = cc
                    val allEdgesSoFar = oldEdges ++ nextNewEdgesCC

                    if (after.canFinish) {
                        val incomingEdges = allEdgesSoFar.incomingSimpleEdgesOf(before)
                        val newLiftings = (incomingEdges map { _.from lift after }) -- cc.liftings -- excludingLiftings
                        nextQueue ++= (newLiftings map { l => ExpandAdvance(l.before, l.after) })
                        nextLiftingsCC ++= newLiftings
                    }
                    if (after.isInstanceOf[SymbolProgressNonterminal]) {
                        val afterDerives = after.asInstanceOf[SymbolProgressNonterminal].derive(nextGen)
                        if (!afterDerives.isEmpty) {
                            val incomingEdgesToBefore = allEdgesSoFar.incomingSimpleEdgesOf(before)
                            nextRootTipsCC ++= incomingEdgesToBefore map { _.from }
                            nextNewEdgesCC ++= incomingEdgesToBefore map { edge => SimpleEdge(edge.from, after) }

                            def recursiveDerive(queue: List[Edge], newNodesCC: Set[Node], newEdgesCC: Set[Edge]): (Set[Node], Set[Edge]) = queue match {
                                case edge +: rest =>
                                    (edge.from, edge.to) match {
                                        case (from: NonterminalNode, to) if to.canFinish =>
                                            val newLifting = from lift to
                                            if (!(excludingLiftings contains newLifting)) {
                                                nextQueue +:= ExpandAdvance(newLifting.before, newLifting.after)
                                                nextLiftingsCC += newLifting
                                            }
                                        case _ => // nothing to do
                                    }
                                    edge match {
                                        case SimpleEdge(from, to: NonterminalNode) =>
                                            val newDerives = (to derive nextGen) -- newEdgesCC
                                            recursiveDerive(rest ++ newDerives, newNodesCC ++ edge.nodes, newEdgesCC + edge)
                                        case e: AssassinEdge0 if e.from.isInstanceOf[NonterminalNode] =>
                                            val from = e.from.asInstanceOf[NonterminalNode]
                                            val newDerives = (from derive nextGen) -- newEdgesCC
                                            recursiveDerive(rest ++ newDerives, newNodesCC ++ edge.nodes, newEdgesCC + edge)
                                        case _ =>
                                            recursiveDerive(rest, newNodesCC ++ edge.nodes, newEdgesCC + edge)
                                    }
                                case List() => (newNodesCC, newEdgesCC)
                            }
                            val (derivedNodes, derivedEdges) = recursiveDerive(afterDerives.toList, Set(), Set())
                            nextNewNodesCC ++= derivedNodes
                            nextNewEdgesCC ++= derivedEdges
                        }
                    }
                    expand0(nextQueue, ExpandResult(nextLiftingsCC, nextNewNodesCC, nextNewEdgesCC, nextRootTipsCC))
                case List() => cc
            }
        }
        expand0(initials, cc)
    }

    def prepareNextAssassinEdges(edges: Set[Edge], liftings: Set[Lifting]): (Set[AssassinEdge0], Set[Node], Set[Edge]) = {
        def propagateLiftAssassinEdges(queue: List[LiftAssassinEdge], newEdgesCC: Set[LiftAssassinEdge]): Set[LiftAssassinEdge] =
            queue match {
                case head +: rest =>
                    val liftedBy = liftings collect { case x @ NontermLifting(_, _, by) if by == head.to => x }
                    // val liftedBy = liftings filter { _.by == Some(head.to) }
                    val newAssassinEdges = (liftedBy map { lifting => LiftAssassinEdge(head.from, lifting.after) }) -- newEdgesCC
                    propagateLiftAssassinEdges(rest ++ newAssassinEdges.toList, newEdgesCC ++ newAssassinEdges)
                case List() => newEdgesCC
            }

        def propagateEagerAssassinEdges(assassinEdges: Set[EagerAssassinEdge]): Set[EagerAssassinEdge] = {
            val propagated: Map[Set[EagerAssassinEdge], Set[Node]] = assassinEdges groupBy { _.to } map { p =>
                assert(p._2 forall { _.to == p._1 })
                val sameDestinationEdges = p._2
                def propagate(queue: List[Node], nodesCC: Set[Node], liftingsCC: Set[Lifting], edgesCC: Set[SimpleEdge]): Set[Node] =
                    queue match {
                        case head +: rest =>
                            val affectedLiftings = liftings collect { case x @ NontermLifting(_, _, by) if by == head => x }
                            // val affectedLiftings = liftings filter { _.by == Some(head) }
                            val affectedEdges = edges.outgoingSimpleEdgesOf(head)
                            val newLiftingsCC = liftingsCC ++ affectedLiftings
                            val newEdgesCC = edgesCC ++ affectedEdges
                            val affectedNodesByLiftings: Set[Node] = affectedLiftings map { _.after } filter { n => liftingsCC filter { _.after == n } subsetOf newLiftingsCC }
                            // affectedNodesByLiftings: affectedLiftings의 after 중에서 after가 자기에게로 향하는 lifting이 모두 newLiftingsCC에 포함된 노드
                            val affectedNodesByEdges: Set[Node] = affectedEdges map { _.to } filter { n => edges.incomingSimpleEdgesOf(n) subsetOf newEdgesCC }
                            // affectedNodesByEdges: affectedEdges의 to들 중에서 자기에게로 향하는 SimpleEdge가 모두 newEdgesCC에 포함된 노드
                            val newAffectedNodes = (affectedNodesByLiftings ++ affectedNodesByEdges) -- nodesCC
                            propagate(rest ++ newAffectedNodes.toList, nodesCC ++ newAffectedNodes, newLiftingsCC, newEdgesCC)
                        case List() => nodesCC
                    }
                (sameDestinationEdges, propagate(sameDestinationEdges.map(_.to).toList, Set(), Set(), Set()))
            }

            (propagated flatMap { p =>
                val (origins, affecteds) = p
                origins flatMap { origin => affecteds map { to => EagerAssassinEdge(origin.from, to) } }
            }).toSet
        }

        val propagatedAssassinEdges0 = propagateLiftAssassinEdges(edges.liftAssassinEdges.toList, Set()).asInstanceOf[Set[AssassinEdge0]]
        val propagatedAssassinEdges1 = propagateEagerAssassinEdges(edges.eagerAssassinEdges).asInstanceOf[Set[AssassinEdge0]]
        val propagatedAssassinEdges: Set[AssassinEdge0] = propagatedAssassinEdges0 ++ propagatedAssassinEdges1

        // TODO outgoing edges of assassin targets
        // TODO lifting before?

        val nodes1 = edges flatMap { _.nodes }

        val edges2 = edges ++ propagatedAssassinEdges
        val edges3 = edges2 filter { e => (nodes1 contains e.from) && (nodes1 contains e.to) }

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
                println(s"**** New Generation $gen")

                graph.edges foreach { edge => println(edge.toShortString) }
                println()
            }

            val terminalLiftings: Set[Lifting] = proceedTerminal1(next)
            if (terminalLiftings isEmpty) {
                Right(ParsingErrors.UnexpectedInput(next))
            } else {
                val expandFn = expand(graph.edges, gen + 1, terminalLiftings.toList map { lifting => ExpandAdvance(lifting.before, lifting.after) }, ExpandResult(terminalLiftings, Set(), Set(), Set()), _: Set[Lifting], _: Set[Node])

                val ExpandResult(liftings0, newNodes0, newEdges0, rootTips0) = expandFn(Set(), Set())

                val (liftings, newNodes, newEdges, rootTips, roots) = {
                    // assert(rootTips subsetOf graph.nodes)

                    assert(terminalLiftings subsetOf liftings0)
                    val activeAssassinEdges = graph.edges.assassinEdges filter { e => liftings0 map { _.before } contains e.from }

                    if (activeAssassinEdges.isEmpty) {
                        val roots = rootTips0 flatMap { rootTip => graph.edges.rootsOf(rootTip) }
                        (liftings0, newNodes0, newEdges0, rootTips0, roots)
                    } else {
                        val activeLiftAssassinEdges = activeAssassinEdges.liftAssassinEdges
                        val activeEagerAssassinEdges = activeAssassinEdges.eagerAssassinEdges

                        val blockingLiftings = liftings0 filter { l => activeAssassinEdges map { _.to } contains l.before }
                        logging {
                            if (!blockingLiftings.isEmpty) {
                                println("- blocked liftings by assassin edges")
                                blockingLiftings foreach { lifting => println(lifting.toShortString) }
                                println("- blocked terminal liftings")
                                (blockingLiftings & terminalLiftings) foreach { lifting => println(lifting.toShortString) }
                            }
                        }

                        val assassinatedNodes = activeEagerAssassinEdges map { _.to }
                        logging {
                            if (!assassinatedNodes.isEmpty) {
                                println("- blocked nodes by assassin edges")
                                assassinatedNodes foreach { node => println(node.toShortString) }
                            }
                        }

                        // assassinatedNodes는 expand할 때도 무시하고, roots에서도 제외해야 함

                        val ExpandResult(liftings1, newNodes1, newEdges1, rootTips1) = expandFn(blockingLiftings, assassinatedNodes)
                        val roots1 = rootTips1 flatMap { rootTip => graph.edges.rootsOf(rootTip) }

                        val roots = roots1 filterNot { assassinatedNodes contains _.from } filterNot { assassinatedNodes contains _.to }

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

    object ParsingContext {
        def fromSeedsVerbose(seeds: Set[Node]): (ParsingContext, VerboseProceedLog) = {
            val ExpandResult(liftings, nodes, edges, _) = expand(Set(), 0, seeds.toList map { seed => ExpandNode(seed) }, ExpandResult(Set(), seeds, Set(), Set()))
            // expand2(seeds.toList, seeds, Set(), Set())
            val (propagatedAssassinEdges, finalNodes, finalEdges) = prepareNextAssassinEdges(edges, liftings)
            val graph = Graph(finalNodes, finalEdges)

            logging {
                println("- nodes")
                nodes.toSeq.sortBy { _.id } foreach { node => println(node.toShortString) }
                println("- edges")
                edges.toSeq.sortBy { e => (e.from.id, e.to.id) } foreach { edge => println(edge.toShortString) }
                println("- liftings")
                liftings.toSeq.sortBy { l => (l.before.id, l.after.id) } foreach { lifting =>
                    println(lifting.toShortString)
                }
            }

            assert(seeds subsetOf graph.nodes)
            assert(graph.nodes collect { case x: SymbolProgressNonterminal => x } forall { _.derive(0) subsetOf graph.edges })
            assert(graph.edges forall { _.nodes subsetOf graph.nodes })
            assert((graph.edges flatMap { _.nodes }) == graph.nodes)
            assert(liftings filter { _.after.canFinish } forall { lifting => graph.edges.incomingSimpleEdgesOf(lifting.before) map { _.from } map { _.lift(lifting.after) } subsetOf liftings })
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
        def fromSeeds(seeds: Set[Node]): ParsingContext = fromSeedsVerbose(seeds)._1
    }

    val startingContextVerbose = ParsingContext.fromSeedsVerbose(Set(SymbolProgress(grammar.startSymbol, 0)))
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
