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
        newEdges: Set[DeriveEdge],
        newReverters: Set[PreReverter],
        proceededEdges: Map[SimpleEdge, SimpleEdge],
        roots: Set[DeriveEdge],
        revertersLog: Map[Reverter, String],
        finalNodes: Set[Node],
        finalEdges: Set[DeriveEdge],
        finalReverters: Set[WorkingReverter])

    val logConfs = Map[String, Boolean](
        "PCG" -> true,
        "expand" -> false,
        "proceedTerminal" -> false,
        "initialPC" -> false)
    def logging(logType: String)(block: => Unit): Unit = {
        logConfs get logType match {
            case Some(true) => block
            case Some(false) => // do nothing
            case None =>
                throw new Exception("Unknown log type: " + logType)
        }
    }
    def logging(logType: String, str: String): Unit = {
        logging(logType) {
            println(str)
        }
    }

    sealed trait ExpandTask
    case class DeriveTask(node: NonterminalNode) extends ExpandTask
    case class LiftTask(lifting: Lifting) extends ExpandTask

    case class ExpandResult(liftings: Set[Lifting], nodes: Set[Node], edges: Set[DeriveEdge], reverters: Set[PreReverter], proceededEdges: Map[SimpleEdge, SimpleEdge]) {
        // proceededEdges: 이전 세대의 DeriveEdge중 내용이 바뀌어서 추가되는 DeriveEdge
        def +(n: ExpandResult) = ExpandResult(n.liftings ++ liftings, n.nodes ++ nodes, n.edges ++ edges, n.reverters ++ reverters, n.proceededEdges ++ proceededEdges)
        def +(n: (Set[Lifting], Set[Node], Set[DeriveEdge], Set[PreReverter], Map[SimpleEdge, SimpleEdge])) = ExpandResult(n._1 ++ liftings, n._2 ++ nodes, n._3 ++ edges, n._4 ++ reverters, n._5 ++ proceededEdges)
        def withNode(newNode: Node) = ExpandResult(liftings, nodes + newNode, edges, reverters, proceededEdges)
        def withLifting(newLifting: Lifting) = ExpandResult(liftings + newLifting, nodes, edges, reverters, proceededEdges)
        def withLiftings(newLiftings: Set[Lifting]) = ExpandResult(liftings ++ newLiftings, nodes, edges, reverters, proceededEdges)
        def withProceededEdge(newProceededEdge: (SimpleEdge, SimpleEdge)) = ExpandResult(liftings, nodes, edges, reverters, proceededEdges + newProceededEdge)
        def withProceededEdges(newProceededEdges: Map[SimpleEdge, SimpleEdge]) = ExpandResult(liftings, nodes, edges, reverters, proceededEdges ++ newProceededEdges)
        def withEdges(newEdges: Set[DeriveEdge]) = ExpandResult(liftings, nodes, edges ++ newEdges, reverters, proceededEdges)
        def withReverters(newReverters: Set[PreReverter]) = ExpandResult(liftings, nodes, edges, reverters ++ newReverters, proceededEdges)
    }

    def expand(oldNodes: Set[Node], oldEdges: Set[DeriveEdge], newGenId: Int, queue: List[ExpandTask]): ExpandResult = {
        def expand0(queue: List[ExpandTask], allTasksCC: Set[ExpandTask], cc: ExpandResult): (Set[ExpandTask], ExpandResult) = {
            val allEdges: Set[DeriveEdge] = oldEdges ++ cc.edges
            logging("expand") {
                println("left queues:")
                queue foreach { q => println(s"  $q") }
            }
            // TODO queue에 중복된 아이템 2개 들어가지 않도록 수정
            queue match {
                case task +: rest => task match {
                    case DeriveTask(node) =>
                        // `node`로부터 derive 처리
                        logging("expand", s"DeriveTask($node)")
                        assert(cc.nodes contains node)

                        var (nextQueue, nextCC) = (rest, cc)

                        // `node`에서 derive할 수 있는 edge를 모두 찾고, 이미 처리된 edge는 제외
                        val (derivedEdges: Set[DeriveEdge], derivedReverters: Set[PreReverter]) = node.derive(newGenId)
                        val newDerivedEdges: Set[DeriveEdge] = derivedEdges -- cc.edges
                        val newDerivedReverters: Set[PreReverter] = derivedReverters -- cc.reverters
                        // `newNode`에서는 cc.nodes를 빼면 안됨. 이걸 빼면 아래 "이미 처리된 노드가 lift된 경우"가 확인이 안됨
                        val newNodes: Set[Node] = newDerivedEdges.flatMap(_.nodes)

                        nextQueue ++:= newNodes collect { case n: NonterminalNode => DeriveTask(n) }
                        nextCC += ExpandResult(Set(), newNodes, newDerivedEdges, newDerivedReverters, Map())

                        logging("expand") {
                            newDerivedEdges foreach { edge =>
                                println("  " + edge)
                            }
                        }

                        // nullable한 것들은 바로 lift처리한다
                        val (newLiftings, newReverters): (Set[Lifting], Set[PreReverter]) = {
                            val lifts = newDerivedEdges collect {
                                case e: SimpleEdge if e.end.canFinish => node.lift(e.end)
                            }
                            (lifts map { _._1 }, lifts flatMap { _._2 })
                        }

                        logging("expand") {
                            newLiftings foreach { lifting =>
                                println("  " + lifting)
                            }
                        }

                        nextQueue ++:= newLiftings map { LiftTask(_) }
                        nextCC = nextCC.withLiftings(newLiftings).withReverters(newReverters)

                        // 새로 만들어진 노드가 이미 처리된 노드인 경우, 이미 처리된 노드가 lift되었을 경우를 확인해서 처리
                        val allNodes = allEdges flatMap { _.nodes }
                        val alreadyProcessedNodes: Set[NonterminalNode] = newNodes.intersect(allNodes) collect { case n: NonterminalNode => n }
                        val (alreadyProcessedNodesLifting, alreadyProcessedReverters): (Set[Lifting], Set[PreReverter]) = {
                            val x = alreadyProcessedNodes map { n =>
                                val lifters: Set[Node] = cc.liftings collect { case NontermLifting(before, _, by) if before == n => by }
                                val lifts: Set[(Lifting, Set[PreReverter])] = lifters map { n.lift(_) }
                                (lifts map { _._1 }, lifts flatMap { _._2 })
                            }
                            (x flatMap { _._1 }, x flatMap { _._2 })
                        }
                        nextQueue ++:= alreadyProcessedNodesLifting map { LiftTask(_) }
                        nextCC = nextCC.withLiftings(alreadyProcessedNodesLifting).withReverters(alreadyProcessedReverters)

                        expand0(nextQueue, allTasksCC + task, nextCC)

                    case LiftTask(TermLifting(before, after, by)) =>
                        // terminal element가 lift되는 경우 처리
                        logging("expand", s"TermLiftTask($before, $after, $by)")

                        // terminal element는 항상 before는 비어있고 after는 한 글자로 차 있어야 하며, 정의상 둘 다 derive가 불가능하다.
                        assert(!before.canFinish)
                        assert(after.canFinish)

                        // 또 이번에 생성된 terminal element가 바로 lift되는 것은 불가능하므로 before는 반드시 oldGraph 소속이어야 한다.
                        assert(oldNodes contains before)

                        var (nextQueue, nextCC) = (rest, cc)

                        allEdges.incomingEdgesOf(before) foreach { edge =>
                            edge match {
                                case e: SimpleEdge =>
                                    val (lifting, newReverters): (Lifting, Set[PreReverter]) = e.start.lift(after)
                                    nextQueue +:= LiftTask(lifting)
                                    nextCC = nextCC.withLifting(lifting).withReverters(newReverters)
                                case e: JoinEdge =>
                                    val constraint: Option[Lifting] = cc.liftings.find { _.before == e.constraint }
                                    if (constraint.isDefined) {
                                        val lifting = e.start.liftJoin(after, constraint.get.after)
                                        nextQueue +:= LiftTask(lifting)
                                        nextCC = nextCC.withLifting(lifting)
                                    }
                            }
                        }

                        expand0(nextQueue, allTasksCC + task, nextCC)

                    case LiftTask(NontermLifting(before, after, by)) =>
                        // nonterminal element가 lift되는 경우 처리
                        // 문제가 되는 lift는 전부 여기 문제
                        logging("expand", s"NontermLiftTask($before, $after, $by)")

                        var (nextQueue, nextCC) = (rest, cc)

                        val incomingDeriveEdges = allEdges.incomingEdgesOf(before)

                        // lift된 node, 즉 `after`가 derive를 갖는 경우
                        // - 이런 경우는, `after`가 앞으로도 추가로 처리될 가능성이 있다는 의미
                        // - 따라서 새 그래프에 `after`를 추가해주고, `before`를 rootTip에 추가해서 추가적인 처리를 준비해야 함
                        val (afterDerives: Set[DeriveEdge], afterReverters: Set[PreReverter]) = after.derive(newGenId)
                        // (afterDerives.isEmpty) 이면 (afterReverters.isEmpty) 이다
                        assert(!afterDerives.isEmpty || afterReverters.isEmpty)
                        if (!afterDerives.isEmpty) {
                            logging("expand", "  hasDerives")

                            var proceededEdges: Map[SimpleEdge, SimpleEdge] = (incomingDeriveEdges map { edge =>
                                edge match {
                                    case e: SimpleEdge =>
                                        (e -> SimpleEdge(e.start, after))
                                    case e: JoinEdge =>
                                        // should never be called (because of proxy)
                                        println(before, after)
                                        println(e)
                                        assert(false)
                                        ???
                                }
                            }).toMap
                            nextQueue +:= DeriveTask(after)
                            nextCC = nextCC.withNode(after).withReverters(afterReverters).withEdges(proceededEdges.values.toSet).withProceededEdges(proceededEdges)
                        }

                        // lift된 node, 즉 `after`가 canFinish인 경우
                        // - 이런 경우는, `after`가 (derive가 가능한가와는 무관하게) 완성된 상태이며, 이 노드에 영향을 받는 다른 노드들을 lift해야 한다는 의미
                        // - 따라서 `after`를 바라보고 있는 노드들을 lift해서 LiftTask를 추가해주어야 함
                        if (after.canFinish) {
                            logging("expand", "  isCanFinish")
                            incomingDeriveEdges foreach { edge =>
                                assert(before == edge.end)
                                edge match {
                                    case e: SimpleEdge =>
                                        val (lifting, newReverters) = e.start.lift(after)
                                        nextQueue +:= LiftTask(lifting)
                                        nextCC = nextCC.withLifting(lifting).withReverters(newReverters)
                                    case e: JoinEdge =>
                                        val constraintLifted = cc.liftings filter { _.before == e.constraint }
                                        if (!constraintLifted.isEmpty) {
                                            // println(before, after)
                                            // println(e)
                                            // println(constraintLifted)
                                            val liftings = constraintLifted map { constraint =>
                                                if (!e.endConstraintReversed) e.start.liftJoin(after, constraint.after)
                                                else e.start.liftJoin(constraint.after, after)
                                            }
                                            nextQueue ++:= liftings map { lifting => LiftTask(lifting) }
                                            nextCC = nextCC.withLiftings(liftings)
                                        }
                                    // just ignore if the constraint is not matched
                                }
                            }
                        }

                        expand0(nextQueue, allTasksCC + task, nextCC)
                }
                case List() => (allTasksCC, cc)
            }
        }
        val initialLiftings: Set[Lifting] = (queue collect { case LiftTask(lifting) => lifting }).toSet
        val initialNodes: Set[Node] = (queue collect { case DeriveTask(node) => node }).toSet
        val (allTasks, result) = expand0(queue, Set(), ExpandResult(initialLiftings, initialNodes, Set(), Set(), Map()))
        logging("PCG") {
            println(newGenId)
            println(allTasks.filter(_.isInstanceOf[DeriveTask]).size, allTasks.filter(_.isInstanceOf[DeriveTask]))
            println(allTasks.size, allTasks)
        }
        // nullable한 node는 바로 lift가 되어서 바로 proceededEdges에 추가될 수 있어서 아래 assert는 맞지 않음
        // assert((result.proceededEdges map { _._1 }).toSet subsetOf oldEdges)
        // assert((result.proceededEdges map { _._2.start }).toSet subsetOf oldNodes)
        assert((result.proceededEdges map { _._2 }).toSet subsetOf result.edges)
        assert((result.proceededEdges map { _._2.end }).toSet subsetOf result.nodes)
        result
    }

    def proceedReverters(reverters: Set[PreReverter], liftings: Set[Lifting], proceededEdges: Map[SimpleEdge, SimpleEdge]): Set[WorkingReverter] = {
        val (deriveReverters0: Set[DeriveReverter], liftReverters: Set[LiftReverter]) = {
            val x = reverters map {
                case r: DeriveReverter => (Some(r), None)
                case r: LiftReverter => (None, Some(r))
            }
            (x flatMap { _._1 }, x flatMap { _._2 })
        }

        val deriveReverters: Set[DeriveReverter] = deriveReverters0 map { r =>
            proceededEdges get r.targetEdge match {
                case Some(proceededEdge) =>
                    // TODO `proceededEdge`가 `proceededEdges`에 또 들어있을 수도 있는 경우 어떻게 할 지 고민해봐야 함
                    r.withNewTargetEdge(proceededEdge)
                case None => r
            }
        }

        val nodeKillReverters: Set[NodeKillReverter] = {
            val liftedToMap: Map[Node, Set[LiftReverter]] = liftReverters groupBy { _.targetLifting.after }
            if (liftedToMap.isEmpty) Set() else {
                (liftedToMap map { kv =>
                    val (node: Node, revertingLift: Lifting) = kv
                    (???, ???)
                }).toSet
                ???
            }
        }

        deriveReverters ++ nodeKillReverters
    }

    def collectResultCandidates(liftings: Set[Lifting]): Set[Node] =
        liftings map { _.after } filter { _.symbol == grammar.startSymbol } collect {
            case n: SymbolProgressNonterminal if n.derivedGen == 0 && n.canFinish => n
        }

    // 이 프로젝트 전체에서 asInstanceOf가 등장하는 경우는 대부분이 Set이 invariant해서 추가된 부분 - covariant한 Set으로 바꾸면 없앨 수 있음
    case class ParsingContext(gen: Int, nodes: Set[Node], edges: Set[DeriveEdge], reverters: Set[WorkingReverter], resultCandidates: Set[SymbolProgress]) {
        def proceedTerminal1(next: Input): Set[Lifting] =
            (nodes flatMap {
                case s: SymbolProgressTerminal => (s proceedTerminal next) map { TermLifting(s, _, next) }
                case _ => None
            })
        def proceedTerminalVerbose(next: Input): (Either[(ParsingContext, VerboseProceedLog), ParsingError]) = {
            // `nextNodes` is actually type of `Set[(SymbolProgressTerminal, SymbolProgressTerminal)]`
            // but the invariance of `Set` of Scala, which I don't understand why, it is defined as Set[(SymbolProgress, SymbolProgress)]
            logging("proceedTerminal") {
                println(s"**** New Generation $gen -> ${gen + 1}")

                edges foreach { edge => println(edge.toShortString) }
                println()
            }

            val terminalLiftings: Set[Lifting] = proceedTerminal1(next)
            if (terminalLiftings isEmpty) {
                Right(ParsingErrors.UnexpectedInput(next))
            } else {
                val alwaysTriggeredNodeKillReverter: Set[AlwaysTriggeredNodeKillReverter] = reverters collect { case r: AlwaysTriggeredNodeKillReverter => r }

                val nextGenId = gen + 1
                val expandTasks = terminalLiftings.toList map { lifting => LiftTask(lifting) }

                val ExpandResult(liftings0, newNodes0, newEdges0, newReverters0, proceededEdges0) = expand(nodes, edges, nextGenId, expandTasks)

                var revertersLog = Map[Reverter, String]()

                val (liftings, newNodes, newEdges, newReverters, proceededEdges, roots): (Set[Lifting], Set[Node], Set[DeriveEdge], Set[PreReverter], Map[SimpleEdge, SimpleEdge], Set[DeriveEdge]) = {
                    // assert(rootTips subsetOf graph.nodes)
                    assert(terminalLiftings subsetOf liftings0)
                    val activatedReverters = reverters filter {
                        _ match {
                            case r: LiftTriggered => liftings0 exists { _.before == r.trigger }
                            case r: MultiLiftTriggered => r.triggers forall { trigger => liftings0 exists { _.before == trigger } }
                            case r: AlwaysTriggered => false
                        }
                    }

                    if (activatedReverters.isEmpty) {
                        val roots = (proceededEdges0.values flatMap { pe => edges.rootsOf(pe.start) }).toSet
                        (liftings0, newNodes0, newEdges0, newReverters0, proceededEdges0, roots)
                    } else {
                        (???, ???, ???, ???, ???, ???)
                    }
                }

                val finalEdges = newEdges ++ roots
                val finalNodes = finalEdges flatMap { _.nodes }
                val workingReverters = proceedReverters(newReverters, liftings, proceededEdges)

                logging("proceedTerminal") {
                    println("- liftings")
                    liftings foreach { lifting => println(lifting.toShortString) }
                    println("- newNodes")
                    newNodes foreach { node => println(node.toShortString) }
                    println("- newEdges")
                    newEdges foreach { edge => println(edge.toShortString) }
                    println("- newReverters")
                    newReverters foreach { reverter => println(reverter.toShortString) }
                    println("- proceededEdges")
                    proceededEdges foreach { pe => println(s"${pe._1.toShortString} --> ${pe._2.toShortString}") }

                    println("- roots")
                    roots foreach { edge => println(edge.toShortString) }

                    println("=== Edges before assassin works ===")
                    newEdges0 foreach { edge => println(edge.toShortString) }
                    println("============ End of generation =======")
                }

                val nextParsingContext = ParsingContext(gen + 1, finalNodes, finalEdges, workingReverters, collectResultCandidates(liftings))
                val verboseProceedLog = VerboseProceedLog(
                    terminalLiftings,
                    liftings,
                    newNodes,
                    newEdges,
                    newReverters,
                    proceededEdges,
                    roots,
                    revertersLog,
                    finalNodes,
                    finalEdges,
                    workingReverters)
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
            val ExpandResult(liftings, nodes, edges, reverters, proceededEdges) = expand(Set(), Set(), 0, List(DeriveTask(startProgress.asInstanceOf[NonterminalNode])))
            // expand2(seeds.toList, seeds, Set(), Set())

            logging("initialPC") {
                println("- nodes")
                nodes.toSeq.sortBy { _.id } foreach { node => println(node.toShortString) }
                println("- edges")
                edges.toSeq.sortBy { e => (e.start.id, e.end.id) } foreach { edge => println(edge.toShortString) }
                println("- liftings")
                liftings.toSeq.sortBy { l => (l.before.id, l.after.id) } foreach { lifting =>
                    println(lifting.toShortString)
                }
            }

            assert(nodes contains startProgress)
            assertForAll[SymbolProgressNonterminal](nodes collect { case x: SymbolProgressNonterminal => x }, { node =>
                val derivation = node.derive(0)
                (derivation._1 subsetOf edges) && (derivation._2 subsetOf reverters)
            })
            assert(edges forall { _.nodes subsetOf nodes })
            assert((edges flatMap { _.nodes }) == nodes)
            // assert(liftings filter { _.after.canFinish } forall { lifting => graph.edges.incomingSimpleEdgesOf(lifting.before) map { _.start } map { _.lift(lifting.after) } subsetOf liftings })
            assert(liftings filter { !_.after.canFinish } forall { lifting =>
                edges.rootsOf(lifting.before).asInstanceOf[Set[DeriveEdge]] subsetOf edges
            })
            // lifting의 after가 derive가 있는지 없는지에 따라서도 다를텐데..
            //assert(liftings collect { case l @ Lifting(_, after: SymbolProgressNonterminal, _) if !after.canFinish => l } filter { _.after.asInstanceOf[SymbolProgressNonterminal].derive(0).isEmpty } forall { lifting =>
            // graph.edges.rootsOf(lifting.before).asInstanceOf[Set[Edge]] subsetOf graph.edges
            //})

            // val finishable: Set[Lifting] = nodes collect { case n if n.canFinish => Lifting(n, n, None) }

            val workingReverters = proceedReverters(reverters, liftings, proceededEdges)
            val startingContext = ParsingContext(0, nodes, edges, workingReverters, collectResultCandidates(liftings))
            val verboseProceedLog = VerboseProceedLog(
                Set(),
                liftings,
                nodes,
                edges,
                reverters,
                proceededEdges,
                Set(),
                Map(),
                Set(),
                Set(),
                workingReverters)
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
