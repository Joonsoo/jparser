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
}

trait DeriveTasks[R <: ParseResult, Graph <: ParsingGraph[R]] extends ParsingTasks[R, Graph] {
    val grammar: Grammar

    def newNode(symbol: Symbol, gen: Int): Node = symbol match {
        case s: Terminal => TermNode(s, gen)

        case s: Except => AtomicNode(s, gen)(Some(newNode(s.except, gen)))

        case s: AtomicNonterm => AtomicNode(s, gen)(None)
        case s: Sequence => SequenceNode(s, 0, gen, gen)
    }

    def deriveNode(baseNode: NontermNode, gen: Int): Set[Edge] = {
        def deriveAtomic(symbol: AtomicNonterm): Set[Edge] = symbol match {
            case Start =>
                Set(SimpleEdge(baseNode, newNode(grammar.startSymbol, gen), Condition.True))
            case Nonterminal(nonterminalName) =>
                grammar.rules(nonterminalName) map { s => SimpleEdge(baseNode, newNode(s, gen), Condition.True) }
            case OneOf(syms) =>
                syms map { s => SimpleEdge(baseNode, newNode(s, gen), Condition.True) }
            case Repeat(sym, lower) =>
                val baseSeq = Sequence(((0 until lower) map { _ => sym }).toSeq, Set())
                val repeatSeq = Sequence(Seq(symbol, sym), Set())
                Set(SimpleEdge(baseNode, newNode(baseSeq, gen), Condition.True),
                    SimpleEdge(baseNode, newNode(repeatSeq, gen), Condition.True))
            case Except(sym, except) =>
                // baseNode가 BaseNode인 경우는 실제 파싱에선 생길 수 없고 테스트 중에만 발생 가능
                // 일반적인 경우에는 baseNode가 AtomicNode이고 liftBlockTrigger가 k.symbol.except 가 들어있어야 함
                Set(SimpleEdge(baseNode, newNode(sym, gen), Condition.True))
            case Proxy(sym) =>
                Set(SimpleEdge(baseNode, newNode(sym, gen), Condition.True))
            case Backup(sym, backup) =>
                val preferNode = newNode(sym, gen)
                Set(SimpleEdge(baseNode, preferNode, Condition.True),
                    SimpleEdge(baseNode, newNode(backup, gen), Condition.Lift(preferNode, gen)))
            case Join(sym, join) =>
                Set(JoinEdge(baseNode, newNode(sym, gen), newNode(join, gen)))
            case Longest(sym) =>
                // baseNode가 NewAtomicNode이고 reservedRevertter가 Some(Trigger.Type.Lift)여야 함
                Set(SimpleEdge(baseNode, newNode(sym, gen), Condition.True))
            case EagerLongest(sym) =>
                // baseNode가 NewAtomicNode이고 reservedRevertter가 Some(Trigger.Type.Alive)여야 함
                Set(SimpleEdge(baseNode, newNode(sym, gen), Condition.True))
            // Except를 제외하고는 모두 liftBlockTrigger가 비어있어야 함
            case LookaheadIs(_) | LookaheadExcept(_) => ??? // must not be called
        }
        def deriveSequence(symbol: Sequence, pointer: Int): Set[Edge] = {
            assert(pointer < symbol.seq.size)
            val sym = symbol.seq(pointer)
            if (pointer > 0 && pointer < symbol.seq.size) {
                // whitespace only between symbols
                (symbol.whitespace + sym) map { newNode(_, gen) } map { SimpleEdge(baseNode, _, Condition.True) }
            } else {
                Set(SimpleEdge(baseNode, newNode(sym, gen), Condition.True))
            }
        }

        baseNode match {
            case AtomicNode(symbol, _) => deriveAtomic(symbol)
            case SequenceNode(symbol, pointer, _, _) => deriveSequence(symbol, pointer)
        }
    }

    def deriveTask(task: DeriveTask, cc: Graph): (Graph, Seq[Task]) = {
        val DeriveTask(nextGen, baseNode) = task

        baseNode.symbol match {
            case baseNodeSymbol: Lookahead =>
                val condition = baseNodeSymbol match {
                    case LookaheadIs(lookahead) => Condition.Wait(newNode(lookahead, nextGen), nextGen)
                    case LookaheadExcept(except) => Condition.Lift(newNode(except, nextGen), nextGen)
                }
                val newNodes = condition.nodes -- cc.nodes
                val newDeriveTasks = newNodes collect { case node: NontermNode => DeriveTask(nextGen, node) }
                (cc.withNodes(newNodes).asInstanceOf[Graph], Seq(FinishingTask(nextGen, baseNode, EmptyResult(resultFunc.empty()), condition)) ++ newDeriveTasks)
            case _ =>
                // 1. Derivation
                val newEdges = deriveNode(baseNode, nextGen)

                // NOTE derive된 엣지 중 revertTriggers의 조건이 cc.results에서 이미 만족된 게 있는 경우는 생기지 않는다고 가정한다
                // - 즉, lookahead/backup 조건이 nullable일 수 없다
                // - lookahead 조건이나 backup 조건이 nullable이면 아무 의미가 없고, 파싱을 시작하기 전에 문법만 보고 확인해서 수정이 가능하기 때문

                // 2. 새로 추가되는 노드에 대한 DeriveTask 만들고 derive된 edge/node를 cc에 넣기
                // 이 때, 새로 생성된 노드 중
                // - 바로 finishable한 노드(empty node, sequence.symbol.seq.isEmpty인 sequence node)들은 results에 추가해주고,
                // - sequence.symbol.seq.isEmpty가 아닌 sequence는 progresses에 추가해준다
                val newNodes = {
                    assert(newEdges forall { _.start == baseNode })
                    // derive된 엣지의 타겟들만 모으고
                    val newNodes0 = newEdges flatMap {
                        case SimpleEdge(start, end, aliveCondition) => Set(end) ++ aliveCondition.nodes
                        case JoinEdge(start, end, join) => Set(end, join)
                    }
                    // 그 중 liftBlockTrigger가 있으면 그것들도 모아서
                    val newNodes1 = newNodes0 flatMap {
                        case n: AtomicNode => Set(n) ++ n.liftBlockTrigger
                        case n => Set(n)
                    }
                    // 기존에 있던 것들 제외
                    newNodes1 -- cc.nodes
                }

                val newDeriveTasks: Set[DeriveTask] = newNodes collect {
                    case n: AtomicNode => DeriveTask(nextGen, n)
                    case n: SequenceNode if n.symbol.seq.length > 0 => DeriveTask(nextGen, n)
                }

                val ncc: Graph = {
                    val newProgresses: Results[SequenceNode, R] = Results((newNodes collect {
                        case node @ SequenceNode(Sequence(seq, _), pointer, _, _) if !seq.isEmpty =>
                            node -> Map(Condition.True -> resultFunc.sequence()) ensuring (pointer == 0)
                    }).toSeq: _*)
                    cc.withNodesEdgesProgresses(newNodes, newEdges, newProgresses).asInstanceOf[Graph]
                }

                // 3. 새로 derive된 노드 중 바로 끝낼 수 있는 노드들에 대해 FinishingTask를 만든다
                val newFinishingTasks: Set[FinishingTask] = newNodes collect {
                    case node @ SequenceNode(Sequence(seq, _), 0, _, _) if seq.isEmpty => // 이 시점에서 SequenceNode의 pointer는 반드시 0
                        FinishingTask(nextGen, node, SequenceResult(resultFunc.sequence()), Condition.True)
                }

                // 4. existingNodes 중 cc.results에 들어 있는 것들로 baseNode에 대한 FinishingTask나 ProgressTask를 만들어준다
                val duplFinishingTasks: Set[Task] = newEdges flatMap {
                    case SimpleEdge(start, end, edgeAliveCondition) if cc.results.of(end).isDefined =>
                        // cc.results에 결과가 있으면 노드도 당연히 이미 있었다는 의미
                        assert(!(newNodes contains end))
                        val results = cc.results.of(end).get
                        results map { tr =>
                            val (resultCondition, result) = tr
                            val finishedResult = BindedResult(resultFunc.bind(end.symbol, result), end.symbol)
                            baseNode match {
                                case baseNode: AtomicNode =>
                                    FinishingTask(nextGen, baseNode, finishedResult, Condition.conjunct(resultCondition, edgeAliveCondition))
                                case baseNode: SequenceNode =>
                                    SequenceProgressTask(nextGen, baseNode, finishedResult, Condition.conjunct(resultCondition, edgeAliveCondition))
                            }
                        }
                    // TODO JoinEdge는 안해줘도 되는지 고민해보기
                    case _ => Set()
                }

                (ncc, newDeriveTasks.toSeq ++ newFinishingTasks.toSeq ++ duplFinishingTasks.toSeq)
        }
    }
}

trait LiftTasks[R <: ParseResult, Graph <: ParsingGraph[R]] extends ParsingTasks[R, Graph] {
    def finishingTask(task: FinishingTask, cc: Graph): (Graph, Seq[Task]) = {
        val FinishingTask(nextGen, node, resultAndSymbol, condition) = task

        // NOTE finish되는 노드에 의해 발동되는 revertTrigger는 없다고 가정한다. 위의 deriveTask의 설명과 동일

        // assertion
        resultAndSymbol match {
            case EmptyResult(result) =>
                assert(node.symbol.isInstanceOf[Lookahead])
            case TermResult(result) =>
                assert(node.symbol.isInstanceOf[Terminal])
            case SequenceResult(result) =>
                assert(node.symbol.isInstanceOf[Sequence])
            case JoinResult(result) =>
                assert(node.symbol.isInstanceOf[Join])
            case BindedResult(result, symbol) =>
                assert(node.symbol.isInstanceOf[AtomicNonterm])
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
                val longestRevertCondition: Condition = node match {
                    case node: AtomicNode =>
                        node.symbol match {
                            case s: Longest => Condition.Lift(node, nextGen)
                            case s: EagerLongest => Condition.Alive(node, nextGen)
                            case _ => Condition.True
                        }
                    case _ =>
                        Condition.True
                }

                val incomingSimpleEdges = cc.incomingSimpleEdgesTo(node)
                val incomingJoinEdges = cc.incomingJoinEdgesTo(node)

                val finishedResult = resultFunc.bind(node.symbol, resultAndSymbol.result)
                val bindedResult = BindedResult(finishedResult, node.symbol)

                assert(incomingSimpleEdges forall { _.end == node })
                val simpleEdgeTasks: Set[Task] = incomingSimpleEdges collect {
                    case SimpleEdge(incoming: AtomicNode, _, edgeRevertTriggers) =>
                        FinishingTask(nextGen, incoming, bindedResult, Condition.conjunct(condition, edgeRevertTriggers, longestRevertCondition))
                    case SimpleEdge(incoming: SequenceNode, _, edgeRevertTriggers) =>
                        SequenceProgressTask(nextGen, incoming, bindedResult, Condition.conjunct(condition, edgeRevertTriggers, longestRevertCondition))
                }

                val joinEdgeTasks: Set[Task] = incomingJoinEdges flatMap {
                    case JoinEdge(start, end, join) =>
                        if (node == end) {
                            cc.results.of(join) match {
                                case Some(results) =>
                                    results map { r =>
                                        FinishingTask(nextGen, start, JoinResult(resultFunc.join(finishedResult, r._2)), Condition.conjunct(condition, r._1, longestRevertCondition))
                                    }
                                case None => Seq()
                            }
                        } else {
                            assert(node == join)
                            cc.results.of(end) match {
                                case Some(results) =>
                                    results map { r =>
                                        FinishingTask(nextGen, start, JoinResult(resultFunc.join(r._2, finishedResult)), Condition.conjunct(condition, r._1, longestRevertCondition))
                                    }
                                case None => Seq()
                            }
                        }
                }

                simpleEdgeTasks.toSeq ++ joinEdgeTasks.toSeq
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

        cc.progresses.of(node) match {
            case None =>
                // baseProgresses가 revert에 의해 없어진 경우에는 무시하고 지나간다
                (cc, Seq())
            case Some(baseProgresses) =>
                val isContent = node.symbol.seq(node.pointer) == childSymbol
                if (isContent && (node.pointer + 1 >= node.symbol.seq.length)) {
                    // append되면 finish할 수 있는 상태
                    // - FinishingTask만 만들어 주면 될듯
                    val appendedNode = SequenceNode(node.symbol, node.pointer + 1, node.beginGen, nextGen)
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
                                // TODO 여기서 condition을 이렇게 주는게 맞나? (task의) condition은 안 줘도 되나?
                                SimpleEdge(start, appendedNode, edgeCondition)
                        }
                        assert(cc.incomingJoinEdgesTo(node).isEmpty)
                        (cc.withNodeEdgesProgresses(appendedNode, newEdges, Results(appendedNode -> appendedProgresses)).asInstanceOf[Graph], Seq(DeriveTask(nextGen, appendedNode)))
                    }
                }
        }
    }
}
