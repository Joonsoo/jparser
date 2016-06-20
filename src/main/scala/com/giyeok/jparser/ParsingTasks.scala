package com.giyeok.jparser

import ParsingGraph._
import Symbols._

trait ParsingTasks[R <: ParseResult, Graph <: ParsingGraph[R]] {
    val resultFunc: ParseResultFunc[R]

    sealed trait Task
    case class DeriveTask(nextGen: Int, baseNode: NontermNode) extends Task
    // node가 finishable해져서 lift하면 afterKernel의 커널과 parsed의 노드를 갖게 된다는 의미
    case class FinishingTask(nextGen: Int, node: Node, resultWithType: ParseResultWithType[R], condition: Condition) extends Task
    case class SequenceProgressTask(nextGen: Int, node: SequenceNode, childAndSymbol: BindedResult[R], condition: Condition) extends Task

    def nodeOf(symbol: Symbol, gen: Int): Node = symbol match {
        case s: Terminal => TermNode(s, gen)
        case s: AtomicNonterm => AtomicNode(s, gen)
        case s: Sequence => SequenceNode(s, 0, gen, gen)
    }
}

trait DeriveTasks[R <: ParseResult, Graph <: ParsingGraph[R]] extends ParsingTasks[R, Graph] {
    val grammar: Grammar

    def deriveTask(task: DeriveTask, cc: Graph): (Graph, Seq[Task]) = {
        val DeriveTask(gen, baseNode) = task

        case class TaskResult(graph: Graph, tasks: Seq[Task]) {
            def addNodes(nodes: Set[Node]): TaskResult = {
                val newNodes = nodes -- graph.nodes
                // newNodes 중 SequenceNode에 대해서 progress 추가
                val newSeqNodes = newNodes collect { case n: SequenceNode => n }
                val newProgresses = newSeqNodes.foldLeft(graph.progresses) { (results, newSeqNode) => results.update(newSeqNode, Condition.True, resultFunc.sequence()) }
                // 새로 추가된 노드들에 대한 derive task 및 그 중 바로 finishable한 sequence node에 대한 finish task
                val newTasks = newNodes collect {
                    case node @ SequenceNode(Sequence(seq, _), _, _, _) if seq.isEmpty =>
                        FinishingTask(gen, node, SequenceResult(resultFunc.sequence()), Condition.True)
                    case n: NontermNode => DeriveTask(gen, n)
                }
                TaskResult(graph.withNodes(newNodes).updateProgresses(newProgresses).asInstanceOf[Graph], tasks ++ newTasks)
            }
            def addEdges(edges: Set[Edge]): TaskResult = TaskResult(graph.withEdges(edges).asInstanceOf[Graph], tasks)
            def addTasks(newTasks: Seq[Task]): TaskResult = TaskResult(graph, tasks ++ newTasks)

            // derive된 노드가 이미 바로 완성 가능함이 알려져 있을 때 처리
            def finishable(node: Node): Seq[(Condition, R)] = {
                graph.results.of(node) match {
                    case Some(r) => r.toSeq
                    case None => Seq()
                }
            }

            def deriveAtomic(baseNode: AtomicNode, destNodeConds: Set[(Node, Condition)]): TaskResult = {
                val newEdges: Set[Edge] = destNodeConds map { nc => SimpleEdge(baseNode, nc._1, nc._2) }
                // destNodes중 finishable인 것들에 대한 FinishTask 추가
                val finishableTasks = destNodeConds.toSeq flatMap { nc =>
                    finishable(nc._1) map { cr =>
                        FinishingTask(gen, baseNode, BindedResult(resultFunc.bind(nc._1.symbol, cr._2), nc._1.symbol), Condition.conjunct(nc._2, cr._1))
                    }
                }
                this.addNodes(destNodeConds map { _._1 }).addEdges(newEdges).addTasks(finishableTasks)
            }
            def deriveJoin(baseNode: NontermNode, dest: Node, join: Node): TaskResult = {
                val newEdges: Set[Edge] = Set(JoinEdge(baseNode, dest, join))
                // TODO dest와 join이 모두 nullable한 경우 FinishTask 추가
                this.addNodes(Set(dest, join)).addEdges(newEdges)
            }
            def deriveSequence(baseNode: SequenceNode, destNodes: Set[Node]): TaskResult = {
                val newEdges: Set[Edge] = destNodes map { n => SimpleEdge(baseNode, n, Condition.True) }
                // destNodes중 finishable인 것들에 대한 FinishTask 추가
                val finishableTasks = destNodes.toSeq flatMap { destNode =>
                    finishable(destNode) map { cr =>
                        SequenceProgressTask(gen, baseNode, BindedResult(resultFunc.bind(destNode.symbol, cr._2), destNode.symbol), cr._1)
                    }
                }
                this.addNodes(destNodes).addEdges(newEdges).addTasks(finishableTasks)
            }

            def pair = (graph, tasks)
        }

        val init = TaskResult(cc, Seq())
        baseNode match {
            case baseNode @ AtomicNode(symbol, _) =>
                (symbol match {
                    case Start =>
                        init.deriveAtomic(baseNode, Set((nodeOf(grammar.startSymbol, gen), Condition.True)))
                    case Nonterminal(nonterminalName) =>
                        init.deriveAtomic(baseNode, grammar.rules(nonterminalName) map { s => (nodeOf(s, gen), Condition.True) })
                    case OneOf(syms) =>
                        init.deriveAtomic(baseNode, syms map { s => (nodeOf(s, gen), Condition.True) })
                    case Proxy(sym) =>
                        init.deriveAtomic(baseNode, Set((nodeOf(sym, gen), Condition.True)))
                    case Repeat(sym, lower) =>
                        val baseSeq = if (lower == 1) sym else Sequence(((0 until lower) map { _ => sym }).toSeq, Set())
                        val repeatSeq = Sequence(Seq(symbol, sym), Set())
                        init.deriveAtomic(baseNode, Set((nodeOf(baseSeq, gen), Condition.True), (nodeOf(repeatSeq, gen), Condition.True)))
                    case Join(sym, join) =>
                        init.deriveJoin(baseNode, nodeOf(sym, gen), nodeOf(join, gen))
                    case LookaheadIs(lookahead) =>
                        init.addNodes(Set(nodeOf(lookahead, gen)))
                            .addTasks(Seq(FinishingTask(gen, baseNode, SequenceResult(resultFunc.sequence()), Condition.Wait(nodeOf(lookahead, gen), gen))))
                    case LookaheadExcept(except) =>
                        init.addNodes(Set(nodeOf(except, gen)))
                            .addTasks(Seq(FinishingTask(gen, baseNode, SequenceResult(resultFunc.sequence()), Condition.Lift(nodeOf(except, gen), gen))))
                    case Longest(sym) =>
                        init.deriveAtomic(baseNode, Set((nodeOf(sym, gen), Condition.True)))
                    case EagerLongest(sym) =>
                        init.deriveAtomic(baseNode, Set((nodeOf(sym, gen), Condition.True)))
                    case Backup(sym, backup) =>
                        init.deriveAtomic(baseNode, Set(
                            (nodeOf(sym, gen), Condition.True),
                            (nodeOf(backup, gen), Condition.Lift(nodeOf(sym, gen), gen))))
                    case Except(sym, except) =>
                        init.deriveAtomic(baseNode, Set(
                            (nodeOf(sym, gen), Condition.True),
                            (nodeOf(except, gen), Condition.True)))
                }).pair
            case baseNode @ SequenceNode(Sequence(seq, whitespace), pointer, beginGen, endGen) =>
                assert(pointer < seq.size)
                val destNodes =
                    if (pointer > 0 && pointer < seq.size) {
                        // whitespace only between symbols
                        (whitespace + seq(pointer)) map { nodeOf(_, gen) }
                    } else {
                        Set(nodeOf(seq(pointer), gen))
                    }
                init.deriveSequence(baseNode, destNodes).pair
        }
    }
}

trait LiftTasks[R <: ParseResult, Graph <: ParsingGraph[R]] extends ParsingTasks[R, Graph] {
    def finishingTask(task: FinishingTask, cc: Graph): (Graph, Seq[Task]) = {
        val FinishingTask(nextGen, node, resultAndSymbol, condition) = task

        // NOTE finish되는 노드에 의해 발동되는 revertTrigger는 없다고 가정한다. 위의 deriveTask의 설명과 동일

        // assertion
        resultAndSymbol match {
            case TermResult(result) =>
                assert(node.symbol.isInstanceOf[Terminal])
            case JoinResult(result) =>
                assert(node.symbol.isInstanceOf[Join])
            case BindedResult(result, symbol) =>
                assert(node.symbol.isInstanceOf[AtomicNonterm])
            case SequenceResult(result) =>
                assert(node.symbol.isInstanceOf[Lookahead] || node.symbol.isInstanceOf[Sequence])
        }

        // 1. cc의 기존 result가 있는지 확인하고 기존 result가 있으면 merge하고 없으면 새로 생성
        val updatedResult: Option[R] = {
            cc.results.of(node, condition) match {
                case Some(baseResult) =>
                    val merged = resultFunc.merge(baseResult, resultAndSymbol.result)
                    if (merged != baseResult) Some(merged) else None
                case None => Some(resultAndSymbol.result)
            }
        }

        if (updatedResult.isEmpty) {
            // 기존 결과가 있는데 기존 결과에 merge를 해도 변하는 게 없으면 더이상 진행하지 않는다(그런 경우 이 task는 없는 것처럼 취급)
            (cc, Seq())
        } else {
            // 2. cc에 새로운 result를 업데이트하고
            val ncc = cc.updateResultOf(node, condition, updatedResult.get).asInstanceOf[Graph]

            // 3. chain lift task 만들어서 추가
            // - 노드에 붙어 있는 reservedReverter, 타고 가는 엣지에 붙어 있는 revertTriggers 주의해서 처리
            // - join 처리
            // - incomingEdge의 대상이 sequence인 경우 SequenceProgressTask가 생성됨
            // - result로 들어온 내용은 node의 symbol로 bind가 안 되어 있으므로 여기서 만드는 태스크에서는 resultFunc.bind(node.symbol, result) 로 줘야함 
            val newTasks: Seq[Task] = {
                // AtomicNode.reservedReverterType 처리
                val nodeCreatedCondition: Condition = node.symbol match {
                    case s: Longest => Condition.Lift(node, nextGen)
                    case s: EagerLongest => Condition.Alive(node, nextGen)
                    case s: Except =>
                        assert(resultAndSymbol.isInstanceOf[BindedResult[_]])
                        val BindedResult(result, resultSymbol) = resultAndSymbol.asInstanceOf[BindedResult[R]]
                        if (s.sym == resultSymbol) {
                            Condition.Exclusion(nodeOf(s.except, node.beginGen), nextGen)
                        } else {
                            assert(s.except == resultSymbol)
                            // except가 lift되어 올라왔으면 그냥 무시하면 되므로
                            Condition.False
                        }
                    case _ => Condition.True
                }

                if (nodeCreatedCondition.permanentFalse) {
                    Seq()
                } else {
                    val incomingSimpleEdges = cc.incomingSimpleEdgesTo(node)
                    val incomingJoinEdges = cc.incomingJoinEdgesTo(node)

                    val finishedResult = resultFunc.bind(node.symbol, resultAndSymbol.result)
                    val bindedResult = BindedResult(finishedResult, node.symbol)

                    assert(incomingSimpleEdges forall { _.end == node })
                    val simpleEdgeTasks: Set[Task] = incomingSimpleEdges collect {
                        case SimpleEdge(incoming: AtomicNode, _, edgeRevertTriggers) =>
                            FinishingTask(nextGen, incoming, bindedResult, Condition.conjunct(condition, edgeRevertTriggers, nodeCreatedCondition))
                        case SimpleEdge(incoming: SequenceNode, _, edgeRevertTriggers) =>
                            SequenceProgressTask(nextGen, incoming, bindedResult, Condition.conjunct(condition, edgeRevertTriggers, nodeCreatedCondition))
                    }

                    val joinEdgeTasks: Set[Task] = incomingJoinEdges flatMap {
                        case JoinEdge(start, end, join) =>
                            if (node == end) {
                                cc.results.of(join) match {
                                    case Some(results) =>
                                        results map { r =>
                                            FinishingTask(nextGen, start, JoinResult(resultFunc.join(finishedResult, r._2)), Condition.conjunct(condition, r._1, nodeCreatedCondition))
                                        }
                                    case None => Seq()
                                }
                            } else {
                                assert(node == join)
                                cc.results.of(end) match {
                                    case Some(results) =>
                                        results map { r =>
                                            FinishingTask(nextGen, start, JoinResult(resultFunc.join(r._2, finishedResult)), Condition.conjunct(condition, r._1, nodeCreatedCondition))
                                        }
                                    case None => Seq()
                                }
                            }
                    }

                    simpleEdgeTasks.toSeq ++ joinEdgeTasks.toSeq
                }
            }

            (ncc, newTasks)
        }
    }

    def sequenceProgressTask(task: SequenceProgressTask, cc: Graph): (Graph, Seq[Task]) = {
        val SequenceProgressTask(nextGen, node, BindedResult(child, childSymbol), condition) = task
        // 1. cc에
        // - 기존의 progress에 append
        // - pointer + 1된 SequenceNode 만들어서
        //   - 이 노드가 finishable이 되면 FinishingTask를 만들고
        //   - 아닌 경우
        //     - 이 노드가 
        //       - cc에 없으면 새로 추가하고 해당 노드의 progress로 appendedResult를 넣어주고(a)
        //       - 이미 같은 노드가 cc에 있으면 기존 progress에 merge해서 업데이트해주고
        //         - merge된 result가 기존의 progress와 다르면(b)
        //     - a나 b의 경우 추가된 노드에 대한 DeriveTask를 만들어 준다

        assert(node.pointer < node.symbol.seq.length)

        // progresses에 sequence node는 항상 포함되어야 한다
        assert(cc.progresses.of(node).isDefined)
        val baseProgresses = cc.progresses.of(node).get

        val isContent = node.symbol.seq(node.pointer) == childSymbol
        if (isContent && (node.pointer + 1 >= node.symbol.seq.length)) {
            // append되면 finish할 수 있는 상태
            // - FinishingTask만 만들어 주면 될듯
            val appendedProgresses = baseProgresses map { kv => Condition.conjunct(kv._1, condition) -> (resultFunc.append(kv._2, child)) }
            val finishingTasks = appendedProgresses map { kv => FinishingTask(nextGen, node, SequenceResult(kv._2), kv._1) }
            (cc, finishingTasks.toSeq)
        } else {
            // append되어도 finish할 수는 없는 상태
            val (appendedNode, appendedProgresses: Map[Condition, R]) = if (isContent) {
                // whitespace가 아닌 실제 내용인 경우
                assert(node.pointer + 1 < node.symbol.seq.length)
                val appendedNode = SequenceNode(node.symbol, node.pointer + 1, node.beginGen, nextGen)
                val appendedProgresses = baseProgresses map { kv => Condition.conjunct(kv._1, condition) -> resultFunc.append(kv._2, child) }
                (appendedNode, appendedProgresses)
            } else {
                // whitespace인 경우
                val appendedNode = SequenceNode(node.symbol, node.pointer, node.beginGen, nextGen)
                val appendedProgresses = baseProgresses map { kv => Condition.conjunct(kv._1, condition) -> resultFunc.appendWhitespace(kv._2, child) }
                (appendedNode, appendedProgresses)
            }

            if (cc.nodes contains appendedNode) {
                // appendedNode가 이미 그래프에 있는 경우
                // - baseProgresses에는 없고 appendedProgresses에 생긴 triggerSet이 있으면 그 부분 추가하고
                // - 겹치는 triggerSet에 대해서는 resultFunc.merge 해서
                // - 만들어진 mergedProgresses가 baseProgresses와 같으면 무시하고 진행하고 다르면 DeriveTask(appendedNode)를 추가한다

                val (mergedProgresses: Map[Condition, R], needDerive: Boolean) = appendedProgresses.foldLeft((cc.progresses.of(appendedNode).get, false)) { (cc, entry) =>
                    val (progresses, _) = cc
                    val (condition, result) = entry
                    progresses get condition match {
                        case Some(existingResult) =>
                            val mergedResult = resultFunc.merge(existingResult, result)
                            if (existingResult == mergedResult) {
                                cc
                            } else {
                                (progresses + (condition -> mergedResult), true)
                            }
                        case None =>
                            (progresses + entry, true)
                    }
                }

                (cc.updateProgressesOf(appendedNode, mergedProgresses).asInstanceOf[Graph], if (needDerive) Seq(DeriveTask(nextGen, appendedNode)) else Seq())
            } else {
                // append한 노드가 아직 cc에 없는 경우
                // node로 들어오는 모든 엣지(모두 SimpleEdge여야 함)의 start -> appendedNode로 가는 엣지를 추가한다
                val newEdges: Set[Edge] = cc.incomingSimpleEdgesTo(node) map {
                    case SimpleEdge(start, end, edgeCondition) =>
                        assert(end == node)
                        // 이미 progress에 반영되었기 때문에 여기서 task의 condition은 안 줘도 될듯
                        SimpleEdge(start, appendedNode, edgeCondition)
                }
                assert(cc.incomingJoinEdgesTo(node).isEmpty)
                (cc.withNodeEdgesProgresses(appendedNode, newEdges, Results(appendedNode -> appendedProgresses)).asInstanceOf[Graph], Seq(DeriveTask(nextGen, appendedNode)))
            }
        }
    }
}
