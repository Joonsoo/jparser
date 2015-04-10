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

    case class VerboseProceedLog(
        terminalLiftings: Set[Lifting],
        liftings: Set[Lifting],
        newNodes: Set[Node],
        newEdges: Set[Edge],
        rootTips: Set[Node],
        roots: Set[SimpleEdge],
        finalNodes: Set[Node],
        finalEdges: Set[Edge])

    def logging(block: => Unit): Unit = {
        block
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
                def expand(queue: List[(Node, Node)], liftingsCC: Set[Lifting], newNodesCC: Set[Node], newEdgesCC: Set[Edge], rootTipsCC: Set[Node]): (Set[Lifting], Set[Node], Set[Edge], Set[Node]) =
                    queue match {
                        case (before, after) +: rest =>
                            var (nextQueue, nextLiftingsCC, nextNewNodesCC, nextNewEdgesCC, nextRootTipsCC) = (rest, liftingsCC, newNodesCC, newEdgesCC, rootTipsCC)

                            def allEdgesSoFar = graph.edges ++ nextNewEdgesCC

                            if (after.canFinish) {
                                val incomingEdges = allEdgesSoFar.incomingSimpleEdgesOf(before)
                                val thisLiftings = incomingEdges map { _.from lift after }
                                val newLiftings = thisLiftings -- liftingsCC
                                nextQueue ++= (newLiftings map { l => (l.before, l.after) })
                                nextLiftingsCC ++= newLiftings
                            }
                            if (after.isInstanceOf[SymbolProgressNonterminal]) {
                                val nextGen = gen + 1
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
                                                    if (!(nextLiftingsCC contains newLifting)) {
                                                        nextQueue +:= (newLifting.before, newLifting.after)
                                                        nextLiftingsCC += newLifting
                                                    }
                                                case _ => // nothing to do
                                            }
                                            edge match {
                                                case SimpleEdge(from, to: NonterminalNode) =>
                                                    val newDerives = (to derive nextGen) -- newEdgesCC
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
                            expand(nextQueue, nextLiftingsCC, nextNewNodesCC, nextNewEdgesCC, nextRootTipsCC)
                        case List() => (liftingsCC, newNodesCC, newEdgesCC, rootTipsCC)
                    }

                val (liftings, newNodes, newEdges, rootTips) = expand(terminalLiftings.toList map { lifting => (lifting.before, lifting.after) }, terminalLiftings, Set(), Set(), Set())

                assert(rootTips subsetOf graph.nodes)

                val roots = rootTips flatMap { rootTip => graph.edges.rootsOf(rootTip) }
                val finalEdges = roots ++ newEdges
                val finalNodes = finalEdges flatMap { _.nodes }

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

                // TODO assassin edges

                def collectResultCandidates(liftings: Set[Lifting]): Set[Node] =
                    liftings map { _.after } filter { _.symbol == grammar.startSymbol } collect {
                        case n: SymbolProgressNonterminal if n.derivedGen == 0 && n.canFinish => n
                    }
                val nextParsingContext = ParsingContext(gen + 1, Graph(finalNodes, finalEdges), collectResultCandidates(liftings))
                val verboseProceedLog = VerboseProceedLog(terminalLiftings, liftings, newNodes, newEdges, rootTips, roots, finalNodes, finalEdges)
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
            assert((graph.edges flatMap { _.nodes }) == graph.nodes)
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
            ParsingContext(0, graph, liftings map { _.after } filter { _.symbol == grammar.startSymbol } filter { _.canFinish })
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
