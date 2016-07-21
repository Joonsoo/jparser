package com.giyeok.jparser.deprecated

import ParsingGraph._
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.ParseResult
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.ParseResultFunc

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

sealed trait ParseResultWithType[R <: ParseResult] {
    val result: R
    def mapResult(func: R => R): ParseResultWithType[R]
}
case class TermResult[R <: ParseResult](result: R) extends ParseResultWithType[R] {
    def mapResult(func: R => R): TermResult[R] = TermResult(func(result))
}
case class SequenceResult[R <: ParseResult](result: R) extends ParseResultWithType[R] {
    def mapResult(func: R => R): SequenceResult[R] = SequenceResult(func(result))
}
case class JoinResult[R <: ParseResult](result: R) extends ParseResultWithType[R] {
    def mapResult(func: R => R): JoinResult[R] = JoinResult(func(result))
}
case class BindedResult[R <: ParseResult](result: R, symbol: Symbols.Symbol) extends ParseResultWithType[R] {
    def mapResult(func: R => R): BindedResult[R] = BindedResult(func(result), symbol)
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
                val newProgresses = newSeqNodes.foldLeft(graph.progresses) { (results, newSeqNode) => results.update(newSeqNode, Condition.True, resultFunc.sequence(gen, newSeqNode.symbol)) }
                // 새로 추가된 노드들에 대한 derive task 및 그 중 바로 finishable한 sequence node에 대한 finish task
                val newTasks = newNodes collect {
                    case node @ SequenceNode(seqSymbol @ Sequence(seq, _), _, _, _) if seq.isEmpty =>
                        FinishingTask(gen, node, SequenceResult(resultFunc.sequence(gen, seqSymbol)), Condition.True)
                    case n: NontermNode => DeriveTask(gen, n)
                }
                TaskResult(graph.withNodes(newNodes).replaceProgresses(newProgresses).asInstanceOf[Graph], tasks ++ newTasks)
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

            def deriveAtomic(baseNode: AtomicNode, destNodes: Set[Node]): TaskResult = {
                val newEdges: Set[Edge] = destNodes map { SimpleEdge(baseNode, _) }
                // destNodes중 finishable인 것들에 대한 FinishTask 추가
                val finishableTasks = destNodes.toSeq flatMap { node =>
                    finishable(node) map { cr =>
                        FinishingTask(gen, baseNode, BindedResult(cr._2, node.symbol), cr._1)
                    }
                }
                this.addNodes(destNodes).addEdges(newEdges).addTasks(finishableTasks)
            }
            def deriveJoin(baseNode: NontermNode, dest: Node, join: Node): TaskResult = {
                val newEdges: Set[Edge] = Set(JoinEdge(baseNode, dest, join))
                // dest와 join이 모두 nullable한 경우 FinishTask 추가
                val startSymbol = baseNode.symbol.asInstanceOf[Symbols.Join]
                val finishableTasks: Seq[FinishingTask] =
                    finishable(dest) flatMap { destResult =>
                        finishable(join) map { joinResult =>
                            FinishingTask(gen, baseNode, JoinResult(resultFunc.join(startSymbol, destResult._2, joinResult._2)), Condition.conjunct(destResult._1, joinResult._1))
                        }
                    }
                this.addNodes(Set(dest, join)).addEdges(newEdges).addTasks(finishableTasks)
            }
            def deriveSequence(baseNode: SequenceNode, destNodes: Set[Node]): TaskResult = {
                val newEdges: Set[Edge] = destNodes map { n => SimpleEdge(baseNode, n) }
                // destNodes중 finishable인 것들에 대한 FinishTask 추가
                val finishableTasks = destNodes.toSeq flatMap { destNode =>
                    finishable(destNode) map { cr =>
                        SequenceProgressTask(gen, baseNode, BindedResult(cr._2, destNode.symbol), cr._1)
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
                        init.deriveAtomic(baseNode, Set(nodeOf(grammar.startSymbol, gen)))
                    case Nonterminal(nonterminalName) =>
                        init.deriveAtomic(baseNode, grammar.rules(nonterminalName) map { nodeOf(_, gen) })
                    case OneOf(syms) =>
                        init.deriveAtomic(baseNode, syms map { nodeOf(_, gen) })
                    case Proxy(sym) =>
                        init.deriveAtomic(baseNode, Set(nodeOf(sym, gen)))
                    case repeat: Repeat =>
                        init.deriveAtomic(baseNode, Set(nodeOf(repeat.baseSeq, gen), nodeOf(repeat.repeatSeq, gen)))
                    case Join(sym, join) =>
                        init.deriveJoin(baseNode, nodeOf(sym, gen), nodeOf(join, gen))
                    case LookaheadIs(lookahead) =>
                        init.addNodes(Set(nodeOf(lookahead, gen)))
                            .addTasks(Seq(FinishingTask(gen, baseNode, SequenceResult(resultFunc.sequence(gen, Sequence(Seq()))), Condition.After(nodeOf(lookahead, gen), gen))))
                    case LookaheadExcept(except) =>
                        init.addNodes(Set(nodeOf(except, gen)))
                            .addTasks(Seq(FinishingTask(gen, baseNode, SequenceResult(resultFunc.sequence(gen, Sequence(Seq()))), Condition.Until(nodeOf(except, gen), gen))))
                    case Longest(sym) =>
                        init.deriveAtomic(baseNode, Set(nodeOf(sym, gen)))
                    case EagerLongest(sym) =>
                        init.deriveAtomic(baseNode, Set(nodeOf(sym, gen)))
                    case Except(sym, except) =>
                        init.deriveAtomic(baseNode, Set(nodeOf(sym, gen), nodeOf(except, gen)))
                }).pair
            case baseNode @ SequenceNode(Sequence(seq, _), pointer, beginGen, endGen) =>
                assert(pointer < seq.size)
                val destNodes = Set(nodeOf(seq(pointer), gen))
                init.deriveSequence(baseNode, destNodes).pair
        }
    }
}

trait LiftTasks[R <: ParseResult, Graph <: ParsingGraph[R]] extends ParsingTasks[R, Graph] {
    def finishingTask(task: FinishingTask, cc: Graph): (Graph, Seq[Task]) = {
        val FinishingTask(nextGen, node, resultAndSymbol, condition0) = task

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

        val condition: Condition = node.symbol match {
            case s: Except =>
                assert(resultAndSymbol.isInstanceOf[BindedResult[_]])
                val BindedResult(result, resultSymbol) = resultAndSymbol.asInstanceOf[BindedResult[R]]
                if (s.sym == resultSymbol) {
                    // except일 때는 updateResultOf할 때 조건에 nodeCreatedCondition도 들어가야 함
                    // Exclude는 cc.updateResultOf할 때 Condition.conjunct(condition, )하고 같이 들어가야 함
                    Condition.conjunct(condition0, Condition.Exclude(nodeOf(s.except, node.beginGen)))
                } else {
                    assert(s.except == resultSymbol)
                    Condition.False
                }
            case _ => condition0
        }

        // 1. cc의 기존 result가 있는지 확인하고 기존 result가 있으면 merge하고 없으면 새로 생성
        val updatedResult: Option[R] = {
            val bindedResult = node match {
                case _: SequenceNode => resultAndSymbol.result
                case node: AtomicNode => resultFunc.bind(node.symbol, resultAndSymbol.result)
                case node: TermNode => resultFunc.bind(node.symbol, resultAndSymbol.result)
            }
            cc.results.of(node, condition) match {
                case Some(existingResult) =>
                    val merged = resultFunc.merge(existingResult, bindedResult)
                    if (merged != existingResult) Some(merged) else None
                case None => Some(bindedResult)
            }
        }

        if (condition == Condition.False || updatedResult.isEmpty) {
            // 기존 결과가 있는데 기존 결과에 merge를 해도 변하는 게 없으면 더이상 진행하지 않는다(그런 경우 이 task는 없는 것처럼 취급)
            (cc, Seq())
        } else {
            // 2. cc에 새로운 result를 업데이트하고

            // 3. chain lift task 만들어서 추가
            // - 노드에 붙어 있는 reservedReverter, 타고 가는 엣지에 붙어 있는 revertTriggers 주의해서 처리
            // - join 처리
            // - incomingEdge의 대상이 sequence인 경우 SequenceProgressTask가 생성됨
            // - result로 들어온 내용은 node의 symbol로 bind가 안 되어 있으므로 여기서 만드는 태스크에서는 resultFunc.bind(node.symbol, result) 로 줘야함
            val ncc = cc.updateResultOf(node, condition, updatedResult.get).asInstanceOf[Graph]
            val newTasks: Seq[Task] = {
                // AtomicNode.reservedReverterType 처리
                val afterCondition = node.symbol match {
                    case s: Longest => Condition.conjunct(condition, Condition.Until(node, nextGen))
                    case s: EagerLongest => Condition.conjunct(condition, Condition.Alive(node, nextGen))
                    case _ => condition
                }

                val incomingSimpleEdges = cc.incomingSimpleEdgesTo(node)
                val incomingJoinEdges = cc.incomingJoinEdgesTo(node)

                val bindedResult = BindedResult(updatedResult.get, node.symbol)

                assert(incomingSimpleEdges forall { _.end == node })
                val simpleEdgeTasks: Set[Task] = incomingSimpleEdges collect {
                    case SimpleEdge(incoming: AtomicNode, _) =>
                        FinishingTask(nextGen, incoming, bindedResult, afterCondition)
                    case SimpleEdge(incoming: SequenceNode, _) =>
                        SequenceProgressTask(nextGen, incoming, bindedResult, afterCondition)
                }

                val joinEdgeTasks: Set[Task] = incomingJoinEdges flatMap {
                    case JoinEdge(start, end, join) =>
                        val startSymbol = start.symbol.asInstanceOf[Symbols.Join]
                        if (node == end) {
                            cc.results.of(join) match {
                                case Some(results) =>
                                    results map { r =>
                                        FinishingTask(nextGen, start, JoinResult(resultFunc.join(startSymbol, updatedResult.get, r._2)), Condition.conjunct(r._1, afterCondition))
                                    }
                                case None => Seq()
                            }
                        } else {
                            assert(node == join)
                            cc.results.of(end) match {
                                case Some(results) =>
                                    results map { r =>
                                        FinishingTask(nextGen, start, JoinResult(resultFunc.join(startSymbol, r._2, updatedResult.get)), Condition.conjunct(r._1, afterCondition))
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

        // progresses에 sequence node는 항상 포함되어야 한다
        assert(cc.progresses.of(node).isDefined)
        val baseProgresses = cc.progresses.of(node).get

        assert(node.symbol.seq(node.pointer) == childSymbol)
        if (node.pointer + 1 >= node.symbol.seq.length) {
            // append되면 finish할 수 있는 상태
            // - FinishingTask만 만들어 주면 될듯
            val appendedProgresses = baseProgresses map { kv => Condition.conjunct(kv._1, condition) -> (resultFunc.append(kv._2, child)) }
            val finishingTasks = appendedProgresses map { kv => FinishingTask(nextGen, node, SequenceResult(kv._2), kv._1) }
            (cc, finishingTasks.toSeq)
        } else {
            // append되어도 finish할 수는 없는 상태
            assert(node.pointer + 1 < node.symbol.seq.length)
            val appendedNode = SequenceNode(node.symbol, node.pointer + 1, node.beginGen, nextGen)
            val appendedProgresses = baseProgresses map { kv => Condition.conjunct(kv._1, condition) -> resultFunc.append(kv._2, child) }

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

                (cc.updateResultsOf(node, appendedProgresses).updateProgressesOf(appendedNode, mergedProgresses).asInstanceOf[Graph], if (needDerive) Seq(DeriveTask(nextGen, appendedNode)) else Seq())
            } else {
                // append한 노드가 아직 cc에 없는 경우
                // node로 들어오는 모든 엣지(모두 SimpleEdge여야 함)의 start -> appendedNode로 가는 엣지를 추가한다
                val newEdges: Set[Edge] = cc.incomingSimpleEdgesTo(node) map {
                    case SimpleEdge(start, end) =>
                        assert(end == node)
                        // 이미 progress에 반영되었기 때문에 여기서 task의 condition은 안 줘도 될듯
                        SimpleEdge(start, appendedNode)
                }
                assert(cc.incomingJoinEdgesTo(node).isEmpty)
                (cc.withNodes(Set(appendedNode)).withEdges(newEdges).updateResultsOf(node, appendedProgresses).updateProgressesOf(appendedNode, appendedProgresses).asInstanceOf[Graph], Seq(DeriveTask(nextGen, appendedNode)))
            }
        }
    }
}