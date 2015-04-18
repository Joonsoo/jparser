package com.giyeok.moonparser

case class ParseResult(parseNode: ParseTree.ParseNode[Symbols.Symbol])

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with SymbolsGraph
        with ParsingErrors {
    import Inputs._

    case class Lifting(before: SymbolProgress, after: SymbolProgress, by: Option[SymbolProgress])

    implicit class AugEdges(edges: Set[Edge]) {
        def simpleEdges: Set[SimpleEdge] = edges collect { case e: SimpleEdge => e }
        def eagerAssassinEdges: Set[AssassinEdge] = edges collect { case e: AssassinEdge => e }

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

    case class VerboseProceedLog(
        terminalLiftings: Set[Lifting],
        liftings: Set[Lifting],
        newNodes: Set[Node],
        newEdges: Set[Edge],
        rootTips: Set[Node],
        roots: Set[SimpleEdge],
        propagatedAssassinEdges: Set[AssassinEdge],
        finalNodes: Set[Node],
        finalEdges: Set[Edge])

    def logging(block: => Unit): Unit = {
        // block
    }

    def expand(oldEdges: Set[Edge], nextGen: Int, queue: List[(Option[Node], Node)], liftingsCC: Set[Lifting], newNodesCC: Set[Node], newEdgesCC: Set[Edge], rootTipsCC: Set[Node]): (Set[Lifting], Set[Node], Set[Edge], Set[Node]) =
        queue match {
            case (before, after) +: rest =>
                var (nextQueue, nextLiftingsCC, nextNewNodesCC, nextNewEdgesCC, nextRootTipsCC) = (rest, liftingsCC, newNodesCC, newEdgesCC, rootTipsCC)
                val allEdgesSoFar = oldEdges ++ nextNewEdgesCC

                if (after.canFinish && before.isDefined) {
                    val incomingEdges = allEdgesSoFar.incomingSimpleEdgesOf(before.get)
                    val newLiftings = (incomingEdges map { _.from lift after }) -- liftingsCC
                    nextQueue ++= (newLiftings map { l => (Some(l.before), l.after) })
                    nextLiftingsCC ++= newLiftings
                }
                if (after.isInstanceOf[SymbolProgressNonterminal]) {
                    val afterDerives = after.asInstanceOf[SymbolProgressNonterminal].derive(nextGen)
                    if (!afterDerives.isEmpty) {
                        if (before.isDefined) {
                            val incomingEdgesToBefore = allEdgesSoFar.incomingSimpleEdgesOf(before.get)
                            nextRootTipsCC ++= incomingEdgesToBefore map { _.from }
                            nextNewEdgesCC ++= incomingEdgesToBefore map { edge => SimpleEdge(edge.from, after) }
                        }

                        def recursiveDerive(queue: List[Edge], newNodesCC: Set[Node], newEdgesCC: Set[Edge]): (Set[Node], Set[Edge]) = queue match {
                            case edge +: rest =>
                                (edge.from, edge.to) match {
                                    case (from: NonterminalNode, to) if to.canFinish =>
                                        val newLifting = from lift to
                                        nextQueue +:= (Some(newLifting.before), newLifting.after)
                                        nextLiftingsCC += newLifting
                                    case _ => // nothing to do
                                }
                                edge match {
                                    case SimpleEdge(from, to: NonterminalNode) =>
                                        val newDerives = (to derive nextGen) -- newEdgesCC
                                        recursiveDerive(rest ++ newDerives, newNodesCC ++ edge.nodes, newEdgesCC + edge)
                                    case AssassinEdge(from: NonterminalNode, to) =>
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
                expand(oldEdges, nextGen, nextQueue, nextLiftingsCC, nextNewNodesCC, nextNewEdgesCC, nextRootTipsCC)
            case List() => (liftingsCC, newNodesCC, newEdgesCC, rootTipsCC)
        }

    def invokeAssassinEdges(edges: Set[Edge], liftings: Set[Lifting]): Nothing = {
        ???
    }

    def prepareNextAssassinEdges(edges: Set[Edge], liftings: Set[Lifting]): (Set[AssassinEdge], Set[Node], Set[Edge]) = {
        val assassinEdges = edges collect { case e: AssassinEdge => e }
        def propagateAssassinEdges(queue: List[AssassinEdge], newEdgesCC: Set[AssassinEdge]): Set[AssassinEdge] =
            queue match {
                case head +: rest =>
                    val liftedBy = liftings filter { _.by == Some(head.to) }
                    val newAssassinEdges = (liftedBy map { lifting => AssassinEdge(head.from, lifting.after) }) -- newEdgesCC
                    propagateAssassinEdges(rest ++ newAssassinEdges.toList, newEdgesCC ++ newAssassinEdges)
                case List() => newEdgesCC
            }
        val propagatedAssassinEdges = propagateAssassinEdges(assassinEdges.toList, Set())

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
                case s: SymbolProgressTerminal => (s proceedTerminal next) map { Lifting(s, _, None) }
                case _ => None
            })
        def proceedTerminalVerbose(next: Input): (Either[(ParsingContext, VerboseProceedLog), ParsingError]) = {
            // `nextNodes` is actually type of `Set[(SymbolProgressTerminal, SymbolProgressTerminal)]`
            // but the invariance of `Set` of Scala, which I don't understand why, it is defined as Set[(SymbolProgress, SymbolProgress)]
            logging {
                println(s"**** New Generation $gen")

                graph.edges foreach { edge =>
                    println(s"${edge.from.toShortString} -> ${edge.to.toShortString}")
                }
                println()
            }

            val terminalLiftings: Set[Lifting] = proceedTerminal1(next)
            if (terminalLiftings isEmpty) {
                Right(ParsingErrors.UnexpectedInput(next))
            } else {
                val (liftings, newNodes, newEdges, rootTips) = expand(graph.edges, gen + 1, terminalLiftings.toList map { lifting => (Some(lifting.before), lifting.after) }, terminalLiftings, Set(), Set(), Set())

                // assert(rootTips subsetOf graph.nodes)

                val roots = rootTips flatMap { rootTip => graph.edges.rootsOf(rootTip) }

                // TODO invokeAssassinEdges
                val (propagatedAssassinEdges, finalNodes, finalEdges) = prepareNextAssassinEdges(newEdges ++ roots, liftings)

                logging {
                    println("- liftings")
                    liftings foreach { lifting => println(s"${lifting.before.toShortString} => ${lifting.after.toShortString} (by ${lifting.by map { _.toShortString }})") }
                    println("- newNodes")
                    newNodes foreach { node => println(node.toShortString) }
                    println("- newEdges")
                    newEdges foreach { edge => println(s"${edge.from.toShortString} -> ${edge.to.toShortString}") }
                    println("- rootTips")
                    rootTips foreach { rootTip => println(rootTip.toShortString) }

                    println("- roots")
                    roots foreach { edge => println(s"${edge.from.toShortString} -> ${edge.to.toShortString}") }

                    println("=== Edges before assassin works ===")
                    finalEdges foreach { edge => println(s"${edge.from.toShortString} -> ${edge.to.toShortString}") }
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
            val (liftings: Set[Lifting], nodes: Set[Node], edges: Set[Edge], _) = expand(Set(), 0, seeds map { seed => (None, seed) } toList, Set(), seeds, Set(), Set())
            // expand2(seeds.toList, seeds, Set(), Set())
            val (propagatedAssassinEdges, finalNodes, finalEdges) = prepareNextAssassinEdges(edges, liftings)
            val graph = Graph(finalNodes, finalEdges)

            logging {
                liftings foreach { lifting =>
                    println(s"${lifting.before.toShortString} -(liftTo)-> ${lifting.after.toShortString}  (by ${lifting.by map { _.toShortString }})")
                    println()
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
            assert(liftings collect { case l @ Lifting(_, after: SymbolProgressNonterminal, _) if !after.canFinish => l } filter { _.after.asInstanceOf[SymbolProgressNonterminal].derive(0).isEmpty } forall { lifting =>
                graph.edges.rootsOf(lifting.before).asInstanceOf[Set[Edge]] subsetOf graph.edges
            })

            val finishable: Set[Lifting] = nodes collect { case n if n.canFinish => Lifting(n, n, None) }

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
