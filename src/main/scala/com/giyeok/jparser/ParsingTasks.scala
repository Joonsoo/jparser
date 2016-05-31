package com.giyeok.jparser

import ParsingGraph._
import Symbols._

trait ParsingTasks[R <: ParseResult, Graph <: ParsingGraph[R]] {
    val resultFunc: ParseResultFunc[R]

    sealed trait Task
    case class DeriveTask(nextGen: Int, baseNode: NontermNode) extends Task
    // node가 finishable해져서 lift하면 afterKernel의 커널과 parsed의 노드를 갖게 된다는 의미
    case class FinishingTask(nextGen: Int, node: Node, result: R, revertTriggers: Set[Trigger]) extends Task
    case class SequenceProgressTask(nextGen: Int, node: SequenceNode, child: R, revertTriggers: Set[Trigger]) extends Task
}

trait DeriveTasks[R <: ParseResult, Graph <: ParsingGraph[R]] extends ParsingTasks[R, Graph] {
    val grammar: Grammar

    def newNode(symbol: Symbol): Node = symbol match {
        case Empty => EmptyNode

        case s: Terminal => TermNode(s)

        case s: Except => AtomicNode(s, 0)(Some(newNode(s.except)), None)
        case s: Longest => AtomicNode(s, 0)(None, Some(Trigger.Type.Lift))
        case s: EagerLongest => AtomicNode(s, 0)(None, Some(Trigger.Type.Alive))

        case s: AtomicNonterm => AtomicNode(s, 0)(None, None)
        case s: Sequence => SequenceNode(s, 0, 0, 0)
    }

    def deriveNode(baseNode: NontermNode): Set[Edge] = {
        def deriveAtomic(symbol: AtomicNonterm): Set[Edge] = symbol match {
            case Start =>
                Set(SimpleEdge(baseNode, newNode(grammar.startSymbol), Set()))
            case Nonterminal(nonterminalName) =>
                grammar.rules(nonterminalName) map { s => SimpleEdge(baseNode, newNode(s), Set()) }
            case OneOf(syms) =>
                syms map { s => SimpleEdge(baseNode, newNode(s), Set()) }
            case Repeat(sym, lower) =>
                val baseSeq = if (lower > 0) Sequence(((0 until lower) map { _ => sym }).toSeq, Set()) else Empty
                val repeatSeq = Sequence(Seq(symbol, sym), Set())
                Set(SimpleEdge(baseNode, newNode(baseSeq), Set()),
                    SimpleEdge(baseNode, newNode(repeatSeq), Set()))
            case Except(sym, except) =>
                // baseNode가 BaseNode인 경우는 실제 파싱에선 생길 수 없고 테스트 중에만 발생 가능
                // 일반적인 경우에는 baseNode가 AtomicNode이고 liftBlockTrigger가 k.symbol.except 가 들어있어야 함
                Set(SimpleEdge(baseNode, newNode(sym), Set()))
            case LookaheadIs(lookahead) =>
                Set(SimpleEdge(baseNode, newNode(Empty), Set(Trigger(newNode(lookahead), Trigger.Type.Wait))))
            case LookaheadExcept(except) =>
                Set(SimpleEdge(baseNode, newNode(Empty), Set(Trigger(newNode(except), Trigger.Type.Lift))))
            case Proxy(sym) =>
                Set(SimpleEdge(baseNode, newNode(sym), Set()))
            case Backup(sym, backup) =>
                val preferNode = newNode(sym)
                Set(SimpleEdge(baseNode, preferNode, Set()),
                    SimpleEdge(baseNode, newNode(backup), Set(Trigger(preferNode, Trigger.Type.Lift))))
            case Join(sym, join) =>
                Set(JoinEdge(baseNode, newNode(sym), newNode(join)))
            case Longest(sym) =>
                // baseNode가 NewAtomicNode이고 reservedRevertter가 Some(Trigger.Type.Lift)여야 함
                Set(SimpleEdge(baseNode, newNode(sym), Set()))
            case EagerLongest(sym) =>
                // baseNode가 NewAtomicNode이고 reservedRevertter가 Some(Trigger.Type.Alive)여야 함
                Set(SimpleEdge(baseNode, newNode(sym), Set()))
            // Except, Longest, EagerLongest의 경우를 제외하고는 모두 liftBlockTrigger와 reservedReverter가 비어있어야 함
        }
        def deriveSequence(symbol: Sequence, pointer: Int): Set[Edge] = {
            assert(pointer < symbol.seq.size)
            val sym = symbol.seq(pointer)
            if (pointer > 0 && pointer < symbol.seq.size) {
                // whitespace only between symbols
                (symbol.whitespace + sym) map { newNode _ } map { SimpleEdge(baseNode, _, Set()) }
            } else {
                Set(SimpleEdge(baseNode, newNode(sym), Set()))
            }
        }

        baseNode match {
            case AtomicNode(symbol, _) => deriveAtomic(symbol)
            case SequenceNode(symbol, pointer, _, _) => deriveSequence(symbol, pointer)
        }
    }

    def deriveTask(task: DeriveTask, cc: Graph): (Graph, Seq[Task]) = {
        val DeriveTask(nextGen, baseNode) = task
        // 1. Derivation
        val newEdges = deriveNode(baseNode)

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
                case SimpleEdge(start, end, revertTriggers) => Set(end) ++ (revertTriggers map { _.node })
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
                    node -> Map(Set[Trigger]() -> resultFunc.sequence()) ensuring (pointer == 0)
            }).toSeq: _*)
            cc.withNodesEdgesProgresses(newNodes, newEdges, newProgresses).asInstanceOf[Graph]
        }

        // 3. 새로 derive된 노드 중 바로 끝낼 수 있는 노드들에 대해 FinishingTask를 만든다
        val newFinishingTasks: Set[FinishingTask] = newNodes collect {
            case node @ EmptyNode =>
                FinishingTask(nextGen, node, resultFunc.empty(), Set[Trigger]())
            case node @ SequenceNode(Sequence(seq, _), 0, _, _) if seq.isEmpty => // 이 시점에서 SequenceNode의 pointer는 반드시 0
                FinishingTask(nextGen, node, resultFunc.sequence(), Set[Trigger]())
        }

        // 4. existingNodes 중 cc.results에 들어 있는 것들로 baseNode에 대한 FinishingTask나 ProgressTask를 만들어준다
        val duplFinishingTasks: Set[Task] = newEdges flatMap {
            case SimpleEdge(start, end, edgeRevertTriggers) if cc.results.of(end).isDefined =>
                // cc.results에 결과가 있으면 노드도 당연히 이미 있었다는 의미
                assert(!(newNodes contains end))
                val results = cc.results.of(end).get
                results map { tr =>
                    val (resultRevertTriggers, result) = tr
                    val finishedResult = resultFunc.bind(end.symbol, result)
                    baseNode match {
                        case baseNode: AtomicNode =>
                            FinishingTask(nextGen, baseNode, finishedResult, resultRevertTriggers ++ edgeRevertTriggers)
                        case baseNode: SequenceNode =>
                            SequenceProgressTask(nextGen, baseNode, finishedResult, resultRevertTriggers ++ edgeRevertTriggers)
                    }
                }
            case _ => Set()
        }

        (ncc, newDeriveTasks.toSeq ++ newFinishingTasks.toSeq ++ duplFinishingTasks.toSeq)
    }
}

trait LiftTasks[R <: ParseResult, Graph <: ParsingGraph[R]] extends ParsingTasks[R, Graph] {
    def finishingTask(task: FinishingTask, cc: Graph): (Graph, Seq[Task]) = {
        val FinishingTask(nextGen, node, result, revertTriggers) = task

        // NOTE finish되는 노드에 의해 발동되는 revertTrigger는 없다고 가정한다. 위의 deriveTask의 설명과 동일

        // 1. cc의 기존 result가 있는지 확인하고 기존 result가 있으면 merge하고 없으면 새로 생성
        val updatedResult: Option[R] = {
            cc.results.of(node, revertTriggers) match {
                case Some(baseResult) =>
                    val merged = resultFunc.merge(baseResult, result)
                    if (merged != baseResult) Some(merged) else None
                case None => Some(result)
            }
        }

        if (updatedResult.isEmpty) {
            // 기존 결과가 있는데 기존 결과에 merge를 해도 변하는 게 없으면 더이상 진행하지 않는다(그런 경우 이 task는 없는 것처럼 취급)
            (cc, Seq())
        } else {
            // 2. cc에 새로운 result를 업데이트하고
            val ncc = cc.updateResultOf(node, revertTriggers, updatedResult.get).asInstanceOf[Graph]

            // 3. chain lift task 만들어서 추가
            // - 노드에 붙어 있는 reservedReverter, 타고 가는 엣지에 붙어 있는 revertTriggers 주의해서 처리
            // - join 처리
            // - incomingEdge의 대상이 sequence인 경우 SequenceProgressTask가 생성됨
            // - result로 들어온 내용은 node의 symbol로 bind가 안 되어 있으므로 여기서 만드는 태스크에서는 resultFunc.bind(node.symbol, result) 로 줘야함 
            val newTasks: Seq[Task] = {
                // AtomicNode.reservedReverterType 처리
                val reservedReverter: Option[Trigger] = node match {
                    case node: AtomicNode => node.reservedReverterType map { Trigger(node, _) }
                    case _ => None
                }

                val incomingSimpleEdges = cc.incomingSimpleEdgesTo(node)
                val incomingJoinEdges = cc.incomingJoinEdgesTo(node)

                val finishedResult = resultFunc.bind(node.symbol, result)

                assert(incomingSimpleEdges forall { _.end == node })
                val simpleEdgeTasks: Set[Task] = incomingSimpleEdges collect {
                    case SimpleEdge(incoming: AtomicNode, _, edgeRevertTriggers) =>
                        FinishingTask(nextGen, incoming, finishedResult, revertTriggers ++ edgeRevertTriggers ++ reservedReverter)
                    case SimpleEdge(incoming: SequenceNode, _, edgeRevertTriggers) =>
                        SequenceProgressTask(nextGen, incoming, finishedResult, revertTriggers ++ edgeRevertTriggers ++ reservedReverter)
                }

                val joinEdgeTasks: Set[Task] = incomingJoinEdges flatMap {
                    case JoinEdge(start, end, join) =>
                        if (node == end) {
                            cc.results.of(join) match {
                                case Some(results) =>
                                    results map { r =>
                                        FinishingTask(nextGen, start, resultFunc.join(finishedResult, r._2), revertTriggers ++ r._1 ++ reservedReverter)
                                    }
                                case None => Seq()
                            }
                        } else {
                            assert(node == join)
                            cc.results.of(end) match {
                                case Some(results) =>
                                    results map { r =>
                                        FinishingTask(nextGen, start, resultFunc.join(r._2, finishedResult), revertTriggers ++ r._1 ++ reservedReverter)
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

    // 나중에 parser 코드랑 합칠 때 정리해야될듯
    def sequenceProgressTask(task: SequenceProgressTask, cc: Graph): (Graph, Seq[Task]) = {
        val SequenceProgressTask(nextGen, node, child, revertTriggers) = task
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

        val baseResult = { val opt = cc.progresses.of(node); opt.get ensuring opt.isDefined }
        val appendedResult = baseResult map { kv => (kv._1 ++ revertTriggers) -> (resultFunc.append(kv._2, child)) }

        if (node.pointer + 1 < node.symbol.seq.length) {
            // append된 뒤에도 아직 finishable이 되지 않는 상태
            val appendedNode = SequenceNode(node.symbol, node.pointer + 1, node.beginGen, nextGen)
            if (cc.nodes contains appendedNode) {
                // appendedNode에서 나가는 엣지들 중 result가 있는 것들로 appendedNode를 다시 progress 혹은 finish 시켜줘야 한다

                val outgoingEdges = cc.outgoingSimpleEdgesFrom(appendedNode)

                // SequenceNode에서 JoinEdge가 나올 수 없음
                assert(cc.outgoingJoinEdgesFrom(node).isEmpty)

                val moreLiftTasks = outgoingEdges flatMap {
                    case SimpleEdge(start, end, edgeRevertTriggers) if cc.results.of(end).isDefined =>
                        // 이런 경우는 DerivationGraph에서만 생길 것으로 생각됨
                        val results = cc.results.of(end).get
                        results map { tr =>
                            val (resultRevertTriggers, result) = tr
                            val finishedResult = resultFunc.bind(end.symbol, result)
                            val finalRevertTriggers = resultRevertTriggers ++ edgeRevertTriggers ++ revertTriggers
                            SequenceProgressTask(nextGen, appendedNode, finishedResult, finalRevertTriggers)
                        }
                    case _ => Set()
                }

                (cc.updateProgressesOf(appendedNode, appendedResult).asInstanceOf[Graph], moreLiftTasks.toSeq)
            } else {
                // append한 노드가 아직 cc에 없는 경우
                // node로 들어오는 모든 엣지(모두 SimpleEdge여야 함)의 start -> appendedNode로 가는 엣지를 추가한다
                val newEdges: Set[Edge] = cc.incomingSimpleEdgesTo(node) map {
                    case SimpleEdge(start, end, edgeRevertTriggers) =>
                        assert(end == node)
                        // TODO 여기서 revertTrigger를 이렇게 주는게 맞나? edgeRevertTriggers만 주면 될 것 같기도 하고? (revertTriggers는 result에 반영되니까)
                        SimpleEdge(start, appendedNode, revertTriggers ++ edgeRevertTriggers)
                }
                assert(cc.incomingJoinEdgesTo(node).isEmpty)
                (cc.withNodeEdgesProgresses(appendedNode, newEdges, Results(appendedNode -> appendedResult)).asInstanceOf[Graph], Seq(DeriveTask(nextGen, appendedNode)))
            }
        } else {
            // append되면 finish할 수 있는 상태
            // - FinishingTask만 만들어 주면 될듯
            val finishingTasks = appendedResult map { kv => FinishingTask(nextGen, node, kv._2, kv._1) }
            (cc, finishingTasks.toSeq)
        }
    }
}
