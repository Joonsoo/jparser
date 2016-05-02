package com.giyeok.jparser

class Parser(val grammar: Grammar)
        extends SymbolProgresses
        with GraphDataStructure
        with Reverters {
    import Inputs._
    import Kernels._
    import ParseTree._
    import Symbols.{ Symbol, Start }
    import ParsingErrors.ParsingError

    sealed trait Lifting {
        val before: SymbolProgress
        val after: SymbolProgress
        def toShortString: String
    }
    case class TermLifting(before: TerminalSymbolProgress, after: TerminalSymbolProgress, by: Input) extends Lifting {
        def toShortString = s"${before.toShortString} => ${after.toShortString} (by ${by.toShortString})"
    }
    sealed trait NonterminalLifting[+E <: DeriveEdge] extends Lifting {
        val before: NonterminalSymbolProgress
        val after: NonterminalSymbolProgress
        val by: ParseNode[Symbol]
        val edge: E
        val internalLifting: Boolean
    }
    object NonterminalLifting {
        def unapply(lifting: NonterminalLifting[DeriveEdge]): Option[(NonterminalSymbolProgress, NonterminalSymbolProgress, ParseNode[Symbol], DeriveEdge, Boolean)] =
            Some((lifting.before, lifting.after, lifting.by, lifting.edge, lifting.internalLifting))
    }
    case class NontermLifting(before: NonterminalSymbolProgress, after: NonterminalSymbolProgress, byNode: Node, edge: SimpleEdge, internalLifting: Boolean) extends NonterminalLifting[SimpleEdge] {
        val by = byNode.parsed.get
        def toShortString = s"${before.toShortString} => ${after.toShortString} (by ${by.toShortString})"
    }
    case class JoinLifting(before: JoinSymbolProgress, after: JoinSymbolProgress, byNode: Node, joinNode: Node, edge: JoinEdge, internalLifting: Boolean) extends NonterminalLifting[JoinEdge] {
        val (by, join) = (byNode.parsed.get, joinNode.parsed.get)
        def toShortString = s""
    }
    object Lifting {
        def apply(before: NonterminalSymbolProgress, liftGen: Int, byNode: Node, edge: SimpleEdge, internalLifting: Boolean) = {
            assert(edge.start == before)
            val by = byNode.parsed.get
            val after = before.lift(liftGen, by)
            NontermLifting(before, after, byNode, edge, internalLifting)
        }
        def apply(before: JoinSymbolProgress, liftGen: Int, byNode: Node, joinNode: Node, edge: JoinEdge, internalLifting: Boolean) = {
            assert(edge.start == before)
            val by = byNode.parsed.get
            val join = joinNode.parsed.get
            val after = before.liftJoin(liftGen, by, join)
            new JoinLifting(before, after, byNode, joinNode, edge, internalLifting)
        }
    }

    case class VerboseProceedLog(
        activatedReverters: Set[Reverter],
        terminalLiftings: Set[TermLifting],
        liftings: Set[Lifting],
        newNodes: Set[Node],
        newEdges: Set[DeriveEdge],
        newReverters: Set[Reverter],
        proceededEdges: Map[SimpleEdge, SimpleEdge],
        internalProceededEdges: Map[SimpleEdge, SimpleEdge],
        roots: Set[DeriveEdge],
        revertersLog: Map[Reverter, String],
        finalNodes: Set[Node],
        finalEdges: Set[DeriveEdge],
        finalReverters: Set[Reverter],
        revertedNodes: Set[Node],
        revertedEdges: Set[DeriveEdge],
        liftBlockedNodes: Set[Node]) { val id = IdGen.nextId }

    val logConfs = Map[String, Boolean](
        "PCG" -> false,
        "expand" -> false,
        "proceedTerminal" -> false,
        "initialPC" -> false,
        "reverters" -> false,
        "reverterKill" -> false)
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

    case class ExpandResult(liftings: Set[Lifting], nodes: Set[Node], edges: Set[DeriveEdge], reverters: Set[Reverter], proceededEdges: Map[SimpleEdge, SimpleEdge], internalProceededEdges: Map[SimpleEdge, SimpleEdge]) {
        // proceededEdges: 이전 세대의 DeriveEdge중 내용이 바뀌어서 추가되는 DeriveEdge
        // internalProceededEdges: derive 과정 중 nullable한 애들에 의해서 자동으로 발생한 proceeded edges
        // 그냥 파싱할 때는 proceededEdges/internalProceededEdges를 구분할 필요는 없는데 PCG 분석 중에 필요할 수 있음
        def withNode(newNode: Node) = ExpandResult(liftings, nodes + newNode, edges, reverters, proceededEdges, internalProceededEdges)
        def withNodes(newNodes: Set[Node]) = ExpandResult(liftings, nodes ++ newNodes, edges, reverters, proceededEdges, internalProceededEdges)
        def withLifting(newLifting: Lifting) = ExpandResult(liftings + newLifting, nodes, edges, reverters, proceededEdges, internalProceededEdges)
        def withLiftings(newLiftings: Set[Lifting]) = ExpandResult(liftings ++ newLiftings, nodes, edges, reverters, proceededEdges, internalProceededEdges)
        def withProceededEdge(newProceededEdge: (SimpleEdge, SimpleEdge)) = ExpandResult(liftings, nodes, edges, reverters, proceededEdges + newProceededEdge, internalProceededEdges)
        def withProceededEdges(newProceededEdges: Map[SimpleEdge, SimpleEdge]) = ExpandResult(liftings, nodes, edges, reverters, proceededEdges ++ newProceededEdges, internalProceededEdges)
        def withInternalProceededEdges(newInternalProceededEdges: Map[SimpleEdge, SimpleEdge]) = ExpandResult(liftings, nodes, edges, reverters, proceededEdges, internalProceededEdges ++ newInternalProceededEdges)
        def withEdges(newEdges: Set[DeriveEdge]) = ExpandResult(liftings, nodes, edges ++ newEdges, reverters, proceededEdges, internalProceededEdges)
        def withReverters(newReverters: Set[Reverter]) = ExpandResult(liftings, nodes, edges, reverters ++ newReverters, proceededEdges, internalProceededEdges)
    }

    def expand(oldNodes: Set[Node], oldEdges: Set[DeriveEdge], liftBlockedNodes: Set[Node], newGenId: Int, queue: List[ExpandTask]): ExpandResult = {
        case class Qcc(queue: List[ExpandTask], cc: ExpandResult) {
            // Queue and CC
            def withReverters(newReverters: Set[Reverter]): Qcc = {
                val reverterTriggers = newReverters flatMap {
                    _ match {
                        case x: LiftTriggered => Set(x.trigger)
                        case x: MultiTriggered[_] => x.triggers map { _.trigger }
                    }
                }
                val newTriggerNodes = (reverterTriggers -- oldNodes -- cc.nodes)
                logging("reverters") {
                    println(reverterTriggers)
                    if (!newTriggerNodes.isEmpty) {
                        println(newTriggerNodes)
                    }
                }
                val newReverterTriggerDeriveTasks = newTriggerNodes collect { case x: NonterminalNode => x } map { DeriveTask(_) }
                Qcc(newReverterTriggerDeriveTasks.toList ++ queue, cc.withNodes(reverterTriggers).withReverters(newReverters))
            }
            def withLiftings(newLiftings: Set[Lifting]): Qcc = {
                val treatedLiftings = newLiftings filterNot { liftBlockedNodes contains _.before }
                val newLiftTasks = treatedLiftings map { LiftTask(_) }
                Qcc(newLiftTasks.toList ++: queue, cc.withLiftings(treatedLiftings))
            }
            def withNewNodesAndEdges(newNodes: Set[Node], newEdges: Set[DeriveEdge]): Qcc = {
                // 원래는 어떤 노드에서 derive해서 나온 엣지들이 가리키는 노드들은 DeriveTask를 돌아야 되는데, 같은 DeriveTask를 두 번 이상 하지 않도록 처리
                val alreadyExistingNodes = newNodes.intersect(cc.nodes)
                // 이미 derive해서 나온 엣지가 가리키는 노드들 중에 nullable이라 lift처리된 것들은 lift 태스크를 다시 돌도록 추가
                val newLiftTasks = alreadyExistingNodes flatMap { enode => cc.liftings filter { _.before == enode } } map { LiftTask(_) }
                val newDeriveTasks = (newNodes -- alreadyExistingNodes) collect { case n: NonterminalNode => DeriveTask(n) }
                val newCC = cc.withNodes(newNodes).withEdges(newEdges)
                Qcc(newDeriveTasks.toList ++: newLiftTasks.toList ++: queue, newCC)
            }
            def withNewLiftedNodeAndProceededEdges(liftedNode: NonterminalNode, proceededEdges: Map[SimpleEdge, SimpleEdge], internalProceededEdges: Boolean): Qcc = {
                val n = withNewNodesAndEdges(Set(liftedNode), proceededEdges.values.toSet)
                if (internalProceededEdges) {
                    Qcc(n.queue, n.cc.withInternalProceededEdges(proceededEdges))
                } else {
                    Qcc(n.queue, n.cc.withProceededEdges(proceededEdges))
                }
            }
        }

        def expand0(queue: List[ExpandTask], allTasksCC: Set[ExpandTask], cc: ExpandResult): (Set[ExpandTask], ExpandResult) = {
            val allEdges: Set[DeriveEdge] = oldEdges ++ cc.edges
            logging("expand") {
                println("left queues:")
                queue foreach { q => println(s"  $q") }
            }

            // queue에 중복된 DeriveTask 2개 들어가지 않는지 확인
            // TODO 중복된 LiftTask도 최소화
            assert(queue match {
                case List() => true
                case (task: DeriveTask) +: rest => !(allTasksCC contains task)
                case (task: LiftTask) +: rest => true
            })
            queue match {
                case task +: rest => task match {
                    case DeriveTask(node) =>
                        // `node`로부터 derive 처리
                        logging("expand", s"DeriveTask($node)")
                        assert(cc.nodes contains node)

                        var nextQcc = Qcc(rest, cc)

                        // `node`에서 derive할 수 있는 edge를 모두 찾고, 이미 처리된 edge는 제외
                        val (derivedEdges: Set[DeriveEdge], derivedReverters: Set[Reverter]) = node.derive(grammar, newGenId)
                        val newDerivedEdges: Set[DeriveEdge] = derivedEdges -- cc.edges
                        val newDerivedReverters: Set[Reverter] = derivedReverters -- cc.reverters
                        // `newNode`에서는 cc.nodes를 빼면 안됨. 이걸 빼면 아래 "이미 처리된 노드가 lift된 경우"가 확인이 안됨
                        val newNodes: Set[Node] = newDerivedEdges.flatMap(_.nodes)

                        nextQcc = nextQcc.withNewNodesAndEdges(newNodes, newDerivedEdges).withReverters(newDerivedReverters)

                        logging("expand") {
                            newDerivedEdges foreach { edge =>
                                println("  " + edge)
                            }
                        }

                        // nullable한 것들은 바로 lift처리한다
                        val newLiftings: Set[Lifting] = {
                            newDerivedEdges collect {
                                case e: SimpleEdge if e.end.kernel.finishable =>
                                    assert(e.end.parsed.isDefined)
                                    Lifting(node, newGenId, e.end, e, true)
                            }
                        }

                        logging("expand") {
                            newLiftings foreach { lifting =>
                                println("  " + lifting)
                            }
                        }

                        nextQcc = nextQcc.withLiftings(newLiftings)

                        // 새로 만들어진 노드가 이미 처리된 노드인 경우, 이미 처리된 노드가 lift되었을 경우를 확인해서 처리(SimpleGrammar6 참고)
                        val allNodes = allEdges flatMap { _.nodes }
                        val alreadyProcessedNodes: Set[NonterminalNode] = newNodes.intersect(allNodes) collect { case n: NonterminalNode => n }
                        val alreadyProcessedNodesLifting: Set[Lifting] = {
                            alreadyProcessedNodes flatMap { n =>
                                val lifters: Set[(Node, SimpleEdge, Boolean)] = cc.liftings collect { case NontermLifting(before, _, by, edge, internalLifting) if before == n => (by, edge, internalLifting) }
                                // TODO JoinLifting은 여기로 올 수 없는지 확인
                                lifters map { p => Lifting(n, newGenId, p._1, p._2, p._3) }
                            }
                        }
                        nextQcc = nextQcc.withLiftings(alreadyProcessedNodesLifting)

                        expand0(nextQcc.queue, allTasksCC + task, nextQcc.cc)

                    case LiftTask(TermLifting(before, after, by)) =>
                        // terminal element가 lift되는 경우 처리
                        logging("expand", s"TermLiftTask($before, $after, $by)")

                        // terminal element는 항상 before는 비어있고 after는 한 글자로 차 있어야 하며, 정의상 둘 다 derive가 불가능하다.
                        assert(!before.kernel.finishable)
                        assert(after.kernel.finishable && after.parsed.isDefined)

                        // 또 이번에 생성된 terminal element가 바로 lift되는 것은 불가능하므로 before는 반드시 oldGraph 소속이어야 한다.
                        assert(oldNodes contains before)

                        var nextQcc = Qcc(rest, cc)

                        allEdges.incomingEdgesOf(before) foreach { edge =>
                            edge match {
                                case e: SimpleEdge =>
                                    val lifting: Lifting = Lifting(e.start, newGenId, after, e, false)
                                    nextQcc = nextQcc.withLiftings(Set(lifting))
                                case e: JoinEdge =>
                                    val constraint: Option[Lifting] = cc.liftings.find { _.before == e.constraint }
                                    if (constraint.isDefined) {
                                        val lifting = Lifting(e.start, newGenId, after, constraint.get.after, e, false)
                                        nextQcc = nextQcc.withLiftings(Set(lifting))
                                    }
                            }
                        }

                        expand0(nextQcc.queue, allTasksCC + task, nextQcc.cc)

                    case LiftTask(NonterminalLifting(before, after, by, _, internalLifting)) =>
                        // nonterminal element가 lift되는 경우 처리
                        // 문제가 되는 lift는 전부 여기 문제
                        logging("expand", s"NontermLiftTask($before, $after, $by)")

                        var nextQcc = Qcc(rest, cc)

                        val incomingDeriveEdges = allEdges.incomingEdgesOf(before)

                        // lift된 node, 즉 `after`가 derive를 갖는 경우
                        // - 이런 경우는, `after`가 앞으로도 추가로 처리될 가능성이 있다는 의미
                        // - 따라서 새 그래프에 `after`를 추가해주고, `before`를 rootTip에 추가해서 추가적인 처리를 준비해야 함
                        if (after.kernel.derivable) {
                            logging("expand", "  derivable")
                            val (afterDerives, afterReverters): (Set[DeriveEdge], Set[Reverter]) = after.derive(grammar, newGenId)
                            // (afterDerives.isEmpty) 이면 (afterReverters.isEmpty) 이다
                            assert(!afterDerives.isEmpty)

                            val proceededEdges: Map[SimpleEdge, SimpleEdge] = (incomingDeriveEdges map { edge =>
                                edge match {
                                    case e: SimpleEdge =>
                                        (e -> SimpleEdge(e.start, after))
                                    case e: JoinEdge =>
                                        // should never be called (because of proxy)
                                        println(before, after)
                                        println(e)
                                        throw new java.lang.AssertionError("should not happen")
                                }
                            }).toMap
                            nextQcc = nextQcc.withNewLiftedNodeAndProceededEdges(after, proceededEdges, internalLifting).withReverters(afterReverters)
                        }

                        // lift된 node, 즉 `after`가 finishable인 경우
                        // - 이런 경우는, `after`가 (derive가 가능한가와는 무관하게) 완성된 상태이며, 이 노드에 영향을 받는 다른 노드들을 lift해야 한다는 의미
                        // - 따라서 `after`를 바라보고 있는 노드들을 lift해서 LiftTask를 추가해주어야 함
                        if (after.kernel.finishable) {
                            logging("expand", "  finishable")
                            incomingDeriveEdges foreach { edge =>
                                assert(before == edge.end)
                                edge match {
                                    case e: SimpleEdge =>
                                        val lifting = Lifting(e.start, newGenId, after, e, internalLifting)
                                        nextQcc = nextQcc.withLiftings(Set(lifting))
                                    case e: JoinEdge =>
                                        val constraintLifted = cc.liftings filter { _.before == e.constraint }
                                        if (!constraintLifted.isEmpty) {
                                            // println(before, after)
                                            // println(e)
                                            // println(constraintLifted)
                                            assert(constraintLifted forall { l => (l.after.derivedGen, l.after.lastLiftedGen) == (after.derivedGen, after.lastLiftedGen) })
                                            val liftings: Set[Lifting] = constraintLifted map { constraint =>
                                                if (!e.endConstraintReversed) Lifting(e.start, newGenId, after, constraint.after, e, internalLifting)
                                                else Lifting(e.start, newGenId, constraint.after, after, e, internalLifting)
                                            }
                                            nextQcc = nextQcc.withLiftings(liftings)
                                        }
                                    // just ignore if the constraint is not matched
                                }
                            }
                        }

                        expand0(nextQcc.queue, allTasksCC + task, nextQcc.cc)
                }
                case List() => (allTasksCC, cc)
            }
        }
        val initialLiftings: Set[Lifting] = (queue collect { case LiftTask(lifting) => lifting }).toSet
        val initialNodes: Set[Node] = (queue collect { case DeriveTask(node) => node }).toSet
        val (allTasks, result) = expand0(queue, Set(), ExpandResult(initialLiftings, initialNodes, Set(), Set(), Map(), Map()))
        logging("PCG") {
            println(newGenId)
            println(allTasks.filter(_.isInstanceOf[DeriveTask]).size, allTasks.filter(_.isInstanceOf[DeriveTask]))
            println(allTasks.size, allTasks)
            println(s"edges=${result.edges.size}, nodes=${result.nodes.size}")
        }
        // nullable한 node는 바로 lift가 되어서 바로 proceededEdges에 추가될 수 있어서 아래 assert는 맞지 않음
        // assert((result.proceededEdges map { _._1 }).toSet subsetOf oldEdges)
        // assert((result.proceededEdges map { _._2.start }).toSet subsetOf oldNodes)
        assert((result.proceededEdges map { _._2 }).toSet subsetOf result.edges)
        assert((result.proceededEdges map { _._2.end }).toSet subsetOf result.nodes)
        assert(result.proceededEdges forall { oldNew => (oldNew._1.start == oldNew._2.start) })
        // assert(result.proceededEdges forall { oldNodes contains _._1.start }) // 이건 만족해야되지 않나?

        // oldNodes의 노드들로부터 derive되는 경우가 있으면 안됨
        assert(((allTasks collect { case DeriveTask(node) => node }).asInstanceOf[Set[Node]] intersect oldNodes).isEmpty)
        result
    }

    def rootTipsOfProceededEdges(proceededEdges: Map[SimpleEdge, SimpleEdge]): Set[Node] =
        proceededEdges.keySet map { _.start }

    def proceedReverters(oldReverters: Set[Reverter], newReverters: Set[Reverter], liftings: Set[Lifting], allProceededEdges: Map[SimpleEdge, SimpleEdge], nextGenId: Int): Set[Reverter] = {
        // trigger는 관계가 없고, target이 변경된 내용을 추적하면 됨
        val nontermLiftings: Set[NonterminalLifting[DeriveEdge]] = liftings collect { case l: NonterminalLifting[_] => l }

        var resultReverters = Set[Reverter]()

        // DeriveReverter하고 NodeKillReverter는 묶어서 처리
        sealed trait ReverterPropagationTask
        case class PropagateDerive(deriveReverter: MultiTriggeredDeriveReverter) extends ReverterPropagationTask
        case class PropagateNodeKill(nodeKillReverter: MultiTriggeredNodeKillReverter) extends ReverterPropagationTask
        def propagateDeriveAndNodeKillReverters(queue: List[ReverterPropagationTask], deriveCC: Set[MultiTriggeredDeriveReverter], nodeKillCC: Set[MultiTriggeredNodeKillReverter]): (Set[MultiTriggeredDeriveReverter], Set[MultiTriggeredNodeKillReverter]) = {
            logging("reverters") {
                if (!queue.isEmpty) {
                    println(nextGenId + " " + queue.head)
                }
            }
            queue match {
                case PropagateDerive(reverter @ MultiTriggeredDeriveReverter(triggers, targetEdge)) +: rest =>
                    // DeriveReverter 처리
                    //   - Subclass: LiftTriggeredDeriveReverter
                    //   - lift중 target edge로 인해 발생한 것들(의 after 노드)가 kill 대상이 된다

                    assert(deriveCC contains reverter)

                    // 제거 대상인 DeriveEdge로 인해 발생한 Lifting도 삭제 대상으로 포함
                    val liftedNodesThroughThisEdge = nontermLiftings filter { _.edge == targetEdge } map { _.after }
                    val newNodeKillReverters = (liftedNodesThroughThisEdge map { MultiTriggeredNodeKillReverter(triggers, _) }) -- nodeKillCC

                    // proceededEdges의 정보를 바탕으로 기존의 DeriveEdge에서 변경된 DeriveEdge가 있는 경우 변경된 DeriveEdge도 revert 대상으로 추가
                    val newDeriveReverters = ((allProceededEdges get targetEdge) map { MultiTriggeredDeriveReverter(triggers, _) }).toSet -- deriveCC

                    propagateDeriveAndNodeKillReverters(
                        (newNodeKillReverters map { PropagateNodeKill(_) }) ++: (newDeriveReverters map { PropagateDerive(_) }) ++: rest,
                        deriveCC ++ newDeriveReverters,
                        nodeKillCC ++ newNodeKillReverters)

                case PropagateNodeKill(reverter @ MultiTriggeredNodeKillReverter(triggers, targetNode)) +: rest =>
                    // NodeKillReverter 처리
                    //   - Subclass: MultiTriggeredNodeKillReverter
                    //   - old/new에서 대상이 겹치는 것들 하나로 묶어주기
                    //   - target node가 lift되어서 나오는 모든 node로 확대되어야 함
                    //   - lift된 다음 derive되는 게 있는 경우에는 DeriveReverter 추가

                    assert(nodeKillCC contains reverter)

                    // (제거 대상인 노드가 lift되어 생긴 노드 + 제거 대상인 노드에 의해 lift되어 생긴 노드)를 계산해서 삭제 대상으로 추가
                    val liftedFrom: Set[Node] = liftings filter { lifting => lifting.before == targetNode } map { _.after }
                    val liftedBy: Set[Node] = if (targetNode.kernel.finishable) {
                        nontermLiftings collect {
                            case l: NontermLifting if l.by == targetNode.parsed.get => l.after
                            case l: JoinLifting if l.by == targetNode.parsed.get => l.after
                            case l: JoinLifting if l.join == targetNode.parsed.get => l.after
                        }
                    } else Set()
                    val lifted = liftedFrom ++ liftedBy
                    val newNodeKillReverters = (lifted map { MultiTriggeredNodeKillReverter(triggers, _) }) -- nodeKillCC

                    // lift된 이후에 derive가 되려면 non atomic 심볼인 repeat/sequence만 가능하므로 전부 SimpleEdge임
                    // TODO 이 부분을 좀 더 우아하게 하는 방법을 찾아보자(nextGenId 안받고)
                    val newDeriveEdges = (lifted collect { case n: NonterminalSymbolProgress if n.kernel.derivable => n.derive(grammar, nextGenId)._1 }).flatten map { _.asInstanceOf[SimpleEdge] }
                    val newDeriveReverters = (newDeriveEdges map { MultiTriggeredDeriveReverter(triggers, _) }) -- deriveCC

                    propagateDeriveAndNodeKillReverters(
                        (newNodeKillReverters map { PropagateNodeKill(_) }) ++: (newDeriveReverters map { PropagateDerive(_) }) ++: rest,
                        deriveCC ++ newDeriveReverters,
                        nodeKillCC ++ newNodeKillReverters)

                case List() => (deriveCC, nodeKillCC)
            }
        }
        val initDeriveReverters = (oldReverters ++ newReverters) collect { case r: DeriveReverter => r } map { _.asInstanceOf[MultiTriggeredDeriveReverter] }
        val initNodeKillReverters = (oldReverters ++ newReverters) collect { case r: NodeKillReverter => r } map { _.asInstanceOf[MultiTriggeredNodeKillReverter] }
        val propagationTasks = (initDeriveReverters.toList map { PropagateDerive(_) }) ++ (initNodeKillReverters.toList map { PropagateNodeKill(_) })
        val propagationCC = initDeriveReverters ++ initNodeKillReverters
        val (deriveReverters, nodeKillReverters) = propagateDeriveAndNodeKillReverters(propagationTasks, initDeriveReverters, initNodeKillReverters)

        // MultiTriggerReverter들이므로 같은 타겟에 대해 그룹핑해줌
        def groupMultiTriggerReverters[X, T <: MultiTriggered[X]](reverters: Set[T], creator: (Set[TriggerCondition], X) => T): Set[T] = {
            // NodeKillReverter는 MultiTriggeredNodeKillReverter인데, 같은 타겟 노드에 대해 그룹핑해주어야 함
            val revertersMap = reverters groupBy { _.target }
            val groupedReverters: Set[T] = (revertersMap map { kv =>
                val (target, reverters) = (kv._1, kv._2)
                creator(reverters.foldLeft(Set[TriggerCondition]()) { _ ++ _.triggers }, kv._1)
            }).toSet
            groupedReverters
        }

        resultReverters ++= groupMultiTriggerReverters[SimpleEdge, MultiTriggeredDeriveReverter](deriveReverters, { MultiTriggeredDeriveReverter(_, _) })
        resultReverters ++= groupMultiTriggerReverters[Node, MultiTriggeredNodeKillReverter](nodeKillReverters, { MultiTriggeredNodeKillReverter(_, _) })

        // TempLiftBlockReverter 처리
        //   - except에서만 사용되는 reverter
        //   - 추가로 확산시키거나 할 필요는 없고 그냥 있던 것 그대로 넘겨주면 됨
        resultReverters ++= oldReverters collect { case x: TempLiftBlockReverter => x }
        resultReverters ++= newReverters collect { case x: TempLiftBlockReverter => x }

        // ReservedReverter 처리
        //   - longest/eager_longest에서만 사용되는 reverter
        //   - 추가로 확산시키거나 할 필요는 없고 그냥 있던 것 그대로 넘겨주면 됨
        resultReverters ++= oldReverters collect { case x: ReservedReverter => x }
        resultReverters ++= newReverters collect { case x: ReservedReverter => x }

        logging("reverters") {
            resultReverters foreach { println _ }
        }
        resultReverters
    }

    // 이 프로젝트 전체에서 asInstanceOf가 등장하는 경우는 대부분이 Set이 invariant해서 추가된 부분 - covariant한 Set으로 바꾸면 없앨 수 있음
    case class ParsingContext(startNode: NonterminalNode, gen: Int, nodes: Set[Node], edges: Set[DeriveEdge], reverters: Set[Reverter], liftings: Set[Lifting], externalProceededEdges: Set[SimpleEdge], internalProceededEdges: Set[SimpleEdge]) {
        val id = IdGen.nextId
        logging("reverters") {
            println(s"- Reverters @ $gen")
            reverters foreach { r =>
                println(r)
            }
        }

        def resultCandidateNodes: Set[Node] = liftings collect {
            case NonterminalLifting(`startNode`, result, _, _, _) if result.derivedGen == 0 && result.kernel.finishable => result
        }
        def resultCandidates: Set[ParseNode[Symbol]] = resultCandidateNodes map { _.parsed.get }

        val allProceededEdges = (externalProceededEdges ++ internalProceededEdges)
        // assert(allProceededEdges forall { e => e.end.isInstanceOf[NonAtomicSymbolProgress[_]] })

        def terminalNodes: Set[TerminalNode] = nodes collect { case s: TerminalSymbolProgress => s }

        def proceedTerminal1(nextGen: Int, next: Input): Set[TermLifting] =
            terminalNodes flatMap { s => (s.proceedTerminal(nextGen, next)) map { TermLifting(s, _, next) } }
        def proceedTerminalVerbose(next: Input): (Either[(ParsingContext, VerboseProceedLog), ParsingError]) = {
            // `nextNodes` is actually type of `Set[(SymbolProgressTerminal, SymbolProgressTerminal)]`
            // but the invariance of `Set` of Scala, which I don't understand why, it is defined as Set[(SymbolProgress, SymbolProgress)]
            logging("proceedTerminal") {
                println(s"**** New Generation $gen -> ${gen + 1}")

                edges foreach { edge => println(edge.toShortString) }
                println()
            }

            val nextGenId = gen + 1

            val terminalLiftings0: Set[TermLifting] = proceedTerminal1(nextGenId, next)
            if (terminalLiftings0.isEmpty) {
                Right(ParsingErrors.UnexpectedInput(next))
            } else {
                val expand0 = expand(nodes, edges, Set(), nextGenId, terminalLiftings0.toList map { lifting => LiftTask(lifting) })

                var revertersLog = Map[Reverter, String]()

                val ExpandResult(liftings0, newNodes0, newEdges0, derivedReverters0, proceededEdges0, internalProceededEdges0) = expand0

                assert(terminalLiftings0.asInstanceOf[Set[Lifting]] subsetOf liftings0)
                val rootNodes0: Set[Node] = rootTipsOfProceededEdges(proceededEdges0) flatMap { edges.rootsOf(_) } flatMap { _.nodes }
                val activatedReverters: Set[Reverter] = reverters filter {
                    _ match {
                        case r: LiftTriggered => liftings0 exists { _.before == r.trigger }
                        case r: MultiTriggered[_] => r.triggers forall {
                            _ match {
                                case TriggerIfLift(trigger) => liftings0 exists { _.before == trigger }
                                case TriggerIfAlive(trigger) =>
                                    // 여기가 true이면 AlwaysTriggered랑 똑같음
                                    // ExpandResult가 나타내는 그래프에서(루트 포함) trigger가 살아있으면 activated되어야 하므로 true, 아니면 false
                                    rootNodes0 contains trigger
                            }
                        }
                    }
                }
                // Reserved Trigger 처리

                logging("reverters")(s"activated: $activatedReverters")

                val (terminalLiftings: Set[TermLifting], (treatedNodes: Set[Node], treatedEdges: Set[DeriveEdge], liftBlockedNodes: Set[Node], reservedReverters: Set[MultiTriggeredNodeKillReverter]), ExpandResult(liftings, newNodes, newEdges, derivedReverters, proceededEdges, internalProceededEdges)) = {
                    if (activatedReverters.isEmpty) {
                        (terminalLiftings0, (nodes, edges, Set(), Set()), expand0)
                    } else {
                        // activated된 reverter를 적용한다

                        val (treatedNodes: Set[Node], treatedEdges: Set[DeriveEdge], liftBlockedNodes: Set[Node], treatedTerminalLiftings: Set[TermLifting], reservedReverters: Set[MultiTriggeredNodeKillReverter]) = {
                            var killEdges = Set[SimpleEdge]()
                            var killNodes = Set[Node]()
                            var liftBlockedNodes = Set[Node]() // activatedReverters collect { case x: TempLiftBlockReverter => x.targetNode }
                            var reservedReverters = Set[MultiTriggeredNodeKillReverter]()
                            activatedReverters foreach {
                                _ match {
                                    case r: DeriveReverter => killEdges += r.targetEdge
                                    case r: NodeKillReverter => killNodes += r.targetNode
                                    case r: TempLiftBlockReverter => liftBlockedNodes += r.targetNode
                                    case r: ReservedReverter =>
                                        // r.node -> r.node에서 lift된 노드로 가는 NodeKillReverter를 다음 세대에 넣어준다
                                        val targetNodes: Set[Node] = liftings0 filter { _.before == r.node } map { _.after }
                                        r match {
                                            case ReservedLiftTriggeredLiftedNodeReverter(_) =>
                                                reservedReverters ++= (targetNodes map { MultiTriggeredNodeKillReverter(Set(TriggerIfLift(r.node)), _) })
                                            case ReservedAliveTriggeredLiftedNodeReverter(_) =>
                                                reservedReverters ++= (targetNodes map { MultiTriggeredNodeKillReverter(Set(TriggerIfAlive(r.node)), _) })
                                        }
                                }
                            }

                            // `killEdges`랑 `killNodes`를 빼고 startNode와 reverter의 trigger에서 reachable한 애들만 남기고 다 없애버리는 걸로 바꿔야겠다 - 싸이클때문에
                            def collectReachables(queue: List[Node], killNodes: Set[Node], killEdges: Set[SimpleEdge]): (Set[Node], Set[DeriveEdge]) = {
                                val survivedEdges = edges -- killEdges
                                def traverse(queue: List[Node], nodesCC: Set[Node], edgesCC: Set[DeriveEdge]): (Set[Node], Set[DeriveEdge]) = queue match {
                                    case node +: rest =>
                                        if (killNodes contains node) {
                                            traverse(rest, nodesCC, edgesCC)
                                        } else if (nodesCC contains node) {
                                            traverse(rest, nodesCC, edgesCC)
                                        } else {
                                            val traversingEdges: Set[DeriveEdge] = (survivedEdges.outgoingEdgesOf(node)) filter { _.nodes.intersect(killNodes).isEmpty }
                                            val traversingNodes: Set[Node] = traversingEdges flatMap {
                                                _ match {
                                                    case SimpleEdge(_, end) => Set(end)
                                                    case JoinEdge(_, end, join, _) => Set(end, join)
                                                }
                                            }
                                            traverse((traversingNodes -- nodesCC).toList ++: queue, nodesCC + node, edgesCC ++ traversingEdges)
                                        }
                                    case List() => (nodesCC, edgesCC)
                                }
                                traverse(queue, Set(), Set())
                            }
                            val activatedReverterTriggers: Set[Node] = activatedReverters flatMap {
                                _ match {
                                    case r: ReservedReverter => Set[Node]() // if (finalNodes contains r.node) Set(r.node) else Set() -> 이런 의미인데, startNode에서 reachable하단 얘기이므로 같은 얘기
                                    case r: LiftTriggered => Set(r.trigger)
                                    case r: MultiTriggered[_] => r.triggers map { _.trigger }
                                }
                            }
                            val (treatedNodes, treatedEdges) = collectReachables(startNode +: activatedReverterTriggers.toList, killNodes, killEdges)

                            val treatedTerminalLiftings = (terminalLiftings0 filter { lifting => (treatedNodes contains lifting.before) && !(liftBlockedNodes contains lifting.before) })

                            (treatedNodes, treatedEdges, liftBlockedNodes, treatedTerminalLiftings, reservedReverters)
                        }
                        val expand1 = expand(treatedNodes, treatedEdges, liftBlockedNodes, nextGenId, treatedTerminalLiftings.toList map { lifting => LiftTask(lifting) })
                        assert(treatedTerminalLiftings.asInstanceOf[Set[Lifting]] subsetOf expand1.liftings)
                        (treatedTerminalLiftings, (treatedNodes, treatedEdges, liftBlockedNodes, reservedReverters), expand1)
                    }
                }
                val roots: Set[DeriveEdge] = rootTipsOfProceededEdges(proceededEdges) flatMap { treatedEdges.rootsOf(_) }

                val finalEdges = newEdges ++ roots
                val finalNodes = finalEdges flatMap { _.nodes }
                val workingReverters0: Set[Reverter] = proceedReverters(reverters, derivedReverters ++ reservedReverters, liftings, proceededEdges ++ internalProceededEdges, nextGenId)
                val workingReverters: Set[Reverter] = workingReverters0 filter {
                    _ match {
                        case r: DeriveReverter => finalEdges contains r.targetEdge
                        case r: NodeKillReverter => finalNodes contains r.targetNode
                        case r: TempLiftBlockReverter => finalNodes contains r.targetNode
                        case r: ReservedReverter => finalNodes contains r.node
                    }
                } flatMap {
                    _ match {
                        case r: LiftTriggered => if (finalNodes contains r.trigger) Some(r) else None
                        case r: MultiTriggered[_] =>
                            val leftTriggers = (r.triggers filter { t => finalNodes contains t.trigger })
                            if (leftTriggers.isEmpty) None else Some(r.withNewTriggers(leftTriggers))
                    }
                }

                logging("proceedTerminal") {
                    println("- liftings")
                    liftings foreach { lifting => println(lifting.toShortString) }
                    println("- newNodes")
                    newNodes foreach { node => println(node.toShortString) }
                    println("- newEdges")
                    newEdges foreach { edge => println(edge.toShortString) }
                    println("- newReverters")
                    derivedReverters foreach { reverter => println(reverter.toShortString) }
                    println("- proceededEdges")
                    proceededEdges foreach { pe => println(s"${pe._1.toShortString} --> ${pe._2.toShortString}") }

                    println("- roots")
                    roots foreach { edge => println(edge.toShortString) }

                    println("=== Edges before assassin works ===")
                    expand0.edges foreach { edge => println(edge.toShortString) }
                    println("============ End of generation =======")
                }

                // liftings의 Lifting 중 동일한 after를 갖는 Lifting이 없는 것 assert
                val nextParsingContext = ParsingContext(startNode, gen + 1, finalNodes, finalEdges, workingReverters, liftings, proceededEdges.values.toSet, internalProceededEdges.values.toSet)
                val verboseProceedLog = VerboseProceedLog(
                    activatedReverters,
                    terminalLiftings,
                    liftings,
                    newNodes,
                    newEdges,
                    derivedReverters,
                    proceededEdges,
                    internalProceededEdges,
                    roots,
                    revertersLog,
                    finalNodes,
                    finalEdges,
                    workingReverters,
                    nodes -- treatedNodes,
                    edges -- treatedEdges,
                    liftBlockedNodes)
                Left((nextParsingContext, verboseProceedLog))
            }
        }
        def proceedTerminal(next: Input): Either[ParsingContext, ParsingError] =
            proceedTerminalVerbose(next) match {
                case Left((ctx, _)) => Left(ctx)
                case Right(error) => Right(error)
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
        def fromSymbolVerbose(startSymbol: Symbol): (ParsingContext, VerboseProceedLog) = {
            fromKernelVerbose(Kernel(startSymbol))
        }
        def fromKernelVerbose(startKernel: Kernel): (ParsingContext, VerboseProceedLog) = {
            val startNode = SymbolProgress(startKernel, 0)
            assert(startNode.isInstanceOf[NonterminalSymbolProgress])
            val ExpandResult(liftings, nodes, edges, reverters, proceededEdges, internalProceededEdges) = expand(Set(), Set(), Set(), 0, List(DeriveTask(startNode.asInstanceOf[NonterminalNode])))
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

            assert(nodes contains startNode)
            assertForAll[NonterminalSymbolProgress](nodes collect { case x: NonterminalSymbolProgress => x }, { node =>
                val derivation = node.derive(grammar, 0)
                (derivation._1 subsetOf edges) && (derivation._2 subsetOf reverters)
            })
            assert(edges forall { _.nodes subsetOf nodes })
            assert((edges flatMap { _.nodes }) == nodes)
            // assert(liftings filter { _.after.canFinish } forall { lifting => graph.edges.incomingSimpleEdgesOf(lifting.before) map { _.start } map { _.lift(lifting.after) } subsetOf liftings })
            assert(liftings filter { !_.after.kernel.finishable } forall { lifting =>
                edges.rootsOf(lifting.before).asInstanceOf[Set[DeriveEdge]] subsetOf edges
            })
            assert(proceededEdges.isEmpty)
            // assert(proceededEdges.isEmpty)
            // lifting의 after가 derive가 있는지 없는지에 따라서도 다를텐데..
            //assert(liftings collect { case l @ Lifting(_, after: SymbolProgressNonterminal, _) if !after.canFinish => l } filter { _.after.asInstanceOf[SymbolProgressNonterminal].derive(0).isEmpty } forall { lifting =>
            // graph.edges.rootsOf(lifting.before).asInstanceOf[Set[Edge]] subsetOf graph.edges
            //})

            // val finishable: Set[Lifting] = nodes collect { case n if n.canFinish => Lifting(n, n, None) }

            val workingReverters = proceedReverters(Set(), reverters, liftings, proceededEdges ++ internalProceededEdges, 0)
            val startingContext = ParsingContext(startNode.asInstanceOf[NonterminalNode], 0, nodes, edges, workingReverters, liftings, Set(), internalProceededEdges.values.toSet)
            val verboseProceedLog = VerboseProceedLog(
                Set(),
                Set(),
                liftings,
                nodes,
                edges,
                reverters,
                proceededEdges,
                internalProceededEdges,
                Set(),
                Map(),
                Set(),
                Set(),
                workingReverters,
                Set(),
                Set(),
                Set())
            (startingContext, verboseProceedLog)
        }
        def fromSymbol(startSymbol: Symbol): ParsingContext = fromSymbolVerbose(startSymbol)._1
        def fromKernel(startKernel: Kernel): ParsingContext = fromKernelVerbose(startKernel)._1
    }

    val initialContextVerbose = ParsingContext.fromSymbolVerbose(Start)
    val initialContext = initialContextVerbose._1

    def parse(source: Inputs.Source): Either[ParsingContext, ParsingError] =
        source.foldLeft[Either[ParsingContext, ParsingError]](Left(initialContext)) {
            (ctx, terminal) =>
                ctx match {
                    case Left(ctx) => ctx proceedTerminal terminal
                    case error @ Right(_) => error
                }
        }
    def parse(source: String): Either[ParsingContext, ParsingError] =
        parse(Inputs.fromString(source))

    object IdGen {
        private var counter = 0
        def nextId(): Int = {
            counter += 1
            counter
        }
    }
}
