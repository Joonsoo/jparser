package com.giyeok.jparser

import com.giyeok.jparser.Inputs.ConcreteInput
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingGraph.TermNode
import com.giyeok.jparser.ParsingGraph.AtomicNode
import com.giyeok.jparser.ParsingGraph.SequenceNode
import com.giyeok.jparser.ParsingGraph.Condition
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.ParsingGraph.Node

class SavingParser(grammar: Grammar, resultFunc: ParseResultFunc[AlwaysTrue.type], derivationFunc: DerivationSliceFunc[AlwaysTrue.type]) extends NewParser(grammar, resultFunc, derivationFunc) with DeriveTasks[AlwaysTrue.type, CtxGraph[AlwaysTrue.type]] {
    class SavingParsingCtx(gen: Int, graph: Graph, inputHistory: Seq[ConcreteInput], resultHistory: Seq[Results[Node, AlwaysTrue.type]], conditionFate: Map[Condition, Condition]) extends ParsingCtx(gen, graph, Set()) {
        override def proceedDetail(input: ConcreteInput): (ParsingCtxTransition, Either[SavingParsingCtx, ParsingError]) = {
            val nextGen = gen + 1

            // 1. No expansion
            val termFinishingTasks = graph.nodes collect {
                case node @ TermNode(term, _) if term accept input => FinishingTask(nextGen, node, TermResult(resultFunc.terminal(gen, input)), Condition.True)
            }

            if (termFinishingTasks.isEmpty) {
                (ParsingCtxTransition(None, None), Right(UnexpectedInput(input)))
            } else {
                val expandTransition = ExpandTransition(s"Gen $gen > (1) - No Expansion", graph, graph, Set())

                val currResult = graph.results
                // 2. Lift
                val liftedGraph = rec(termFinishingTasks.toList, graph.withNoResults.asInstanceOf[Graph])
                val liftTransition = LiftTransition(s"Gen $gen > (2) Lift", graph, liftedGraph, termFinishingTasks.asInstanceOf[Set[NewParser[AlwaysTrue.type]#Task]], Set())

                // 3. Trimming
                val newTermNodes: Set[Node] = liftedGraph.nodes collect { case node @ TermNode(_, `nextGen`) => node }
                val trimmingStartNodes = Set(startNode) ++ liftedGraph.nodesInResultsAndProgresses
                val trimmedGraph = liftedGraph.subgraphIn(trimmingStartNodes, newTermNodes, resultFunc).asInstanceOf[Graph]
                val firstTrimmingTransition = TrimmingTransition(s"Gen $gen > (3) First Trimming", liftedGraph, trimmedGraph, trimmingStartNodes, newTermNodes)

                // 4. Revert
                val revertedGraph = ParsingCtx.revert(nextGen, trimmedGraph, trimmedGraph.results, trimmedGraph.nodes)
                val revertTransition = RevertTransition(s"Gen $gen > (4) Revert", trimmedGraph, revertedGraph, trimmedGraph, trimmedGraph.results)

                val nextGraph = revertedGraph
                val conditionNextFate = (conditionFate ++ (nextGraph.allConditions map { k => (k -> k) }).toMap) map { kv => (kv._1 -> kv._2.evaluate(nextGen, trimmedGraph.results, trimmedGraph.nodes)) }
                val nextContext = new SavingParsingCtx(nextGen, nextGraph, inputHistory :+ input, resultHistory :+ currResult, conditionNextFate)
                val transition = ParsingCtxTransition(
                    Some(expandTransition, liftTransition),
                    Some(firstTrimmingTransition, revertTransition))
                (transition, Left(nextContext))
            }
        }

        override def proceed(input: ConcreteInput): Either[ParsingCtx, ParsingError] = proceedDetail(input)._2

        def reconstructResult: ParseResultGraph = {
            val resultFunc = ParseResultGraphFunc

            val actualHistory = (resultHistory :+ graph.results) map { resultMap =>
                val eligibleEntries = (resultMap filter { nc =>
                    conditionFate(nc._2).eligible
                }).entries
                (eligibleEntries map { _._1 }).toSet
            }

            import Symbols._
            def derive(symbol: Symbol): Set[Symbol] = symbol match {
                case Start => Set(grammar.startSymbol)
                case Nonterminal(nonterminalName) => grammar.rules(nonterminalName)
                case OneOf(syms) => syms
                case Proxy(sym) => Set(sym)
                case repeat: Repeat => Set(repeat.baseSeq, repeat.repeatSeq)
                case Join(sym, join) => ??? // sym, join
                case _: LookaheadIs | _: LookaheadExcept =>
                    Set(Sequence(Seq(), Set()))
                case Longest(sym) =>
                    Set(sym)
                case EagerLongest(sym) =>
                    Set(sym)
                case Backup(sym, backup) =>
                    Set(sym, backup)
                case Except(sym, except) =>
                    Set(sym, except)
            }

            def reconstruct(node: Node, gen: Int): ParseResultGraph = {
                node match {
                    case TermNode(symbol, beginGen) =>
                        assert(gen == beginGen + 1)
                        resultFunc.bind(symbol, resultFunc.terminal(beginGen, inputHistory(beginGen)))
                    case AtomicNode(symbol: Join, beginGen) =>
                        ???
                    case AtomicNode(symbol, beginGen) =>
                        // beginGen..gen을 커버하는 AtomicNode result 중에 symbol에서 derive될 수 있는 것들 추리기
                        // beginGen..gen을 커버하는 SequenceNode result 중에 symbol에서 derive될 수 있는 것들 추리기
                        val merging = actualHistory(gen) collect {
                            case child: TermNode if (beginGen == child.beginGen) && (derive(symbol) contains child.symbol) =>
                                val body = reconstruct(child, gen)
                                resultFunc.bind(symbol, body)
                            case child: AtomicNode if (beginGen == child.beginGen) && (derive(symbol) contains child.symbol) =>
                                val body = reconstruct(child, gen)
                                resultFunc.bind(symbol, body) // bind를 했는데 그대로이면 뭔가 해야 될듯?
                            case child: SequenceNode if (derive(symbol) contains child.symbol) && (child.beginGen == beginGen) && (child.symbol.seq.length == 0) =>
                                resultFunc.sequence(child.beginGen, child.symbol)
                            case child: SequenceNode if (derive(symbol) contains child.symbol) && (child.beginGen == beginGen) && (child.pointer + 1 == child.symbol.seq.length) =>
                                // sequence가 완전히 끝나는 경우
                                // 맨 마지막엔 whitespace가 올 수 없음
                                val lastChildSym = child.symbol.seq(child.pointer)
                                val childNode = lastChildSym match {
                                    case term: Terminal => TermNode(term, child.endGen)
                                    case nonterm: AtomicNonterm => AtomicNode(nonterm, child.endGen)
                                }
                                assert(actualHistory(gen) contains childNode)
                                resultFunc.append(reconstruct(child, child.endGen), reconstruct(childNode, gen))
                        }
                        if (merging.isEmpty) {
                            println(symbol)
                            actualHistory(gen) foreach { child =>
                                println(child)
                            }
                        }
                        resultFunc.merge(merging).get
                    case SequenceNode(symbol, 0, beginGen, endGen) =>
                        resultFunc.sequence(beginGen, symbol) ensuring (gen == endGen && beginGen == endGen)
                    case SequenceNode(symbol, pointer, beginGen, endGen) =>
                        assert(gen == endGen)

                        val lastChildSym = symbol.seq(pointer - 1)
                        // actualHistory(gen) 에 lastChildSym이 끝난 것들이 있으면, 각각에 대해서 이 sequencenode의 child로 들어갈 수 있는지 확인(pointer가 하나 작은 sequence node가 child candidate node의 beginGen에서 끝났으면)
                        // whitespace도 비슷하게 처리
                        val merging = actualHistory(gen) flatMap {
                            case child: AtomicNode if child.symbol == lastChildSym =>
                                // actualHistory(childGen)에 SequenceNode랑 symbol, pointer - 1, ?beginGen, 
                                actualHistory(gen) collect {
                                    case prevSeq: SequenceNode if prevSeq.symbol == symbol && prevSeq.pointer == pointer - 1 && prevSeq.beginGen == beginGen && prevSeq.endGen == child.beginGen =>
                                        resultFunc.append(reconstruct(prevSeq, child.beginGen), reconstruct(child, gen))
                                }
                            case child: TermNode if child.symbol == lastChildSym =>
                                // actualHistory(childGen)에 SequenceNode랑 symbol, pointer - 1, ?beginGen, 
                                actualHistory(gen) foreach { h =>
                                    println(h)
                                    h match {
                                        case prevSeq: SequenceNode =>
                                            println(prevSeq.symbol == symbol)
                                            println(prevSeq.pointer, pointer - 1, prevSeq.pointer == pointer - 1)
                                            println(prevSeq.beginGen == beginGen)
                                            println(prevSeq.endGen, child.beginGen, prevSeq.endGen == child.beginGen)
                                        case _ =>
                                    }
                                }
                                actualHistory(gen) collect {
                                    case prevSeq: SequenceNode if prevSeq.symbol == symbol && prevSeq.pointer == pointer - 1 && prevSeq.beginGen == beginGen && prevSeq.endGen == child.beginGen =>
                                        resultFunc.append(reconstruct(prevSeq, child.beginGen), reconstruct(child, gen))
                                }
                            case _ => Seq()
                        }
                        if (merging.isEmpty) {
                            println("???")
                        }
                        resultFunc.merge(merging).get
                }
            }
            reconstruct(startNode, gen)
        }
    }

    def process(task: Task, cc: CtxGraph[AlwaysTrue.type]): (CtxGraph[AlwaysTrue.type], Seq[Task]) = {
        task match {
            case task: DeriveTask => deriveTask(task, cc)
            case task: FinishingTask => finishingTask(task, cc)
            case task: SequenceProgressTask => sequenceProgressTask(task, cc)
        }
    }

    def rec(tasks: List[Task], cc: CtxGraph[AlwaysTrue.type]): CtxGraph[AlwaysTrue.type] =
        tasks match {
            case task +: rest =>
                val (newCC, newTasks) = process(task, cc)
                rec((newTasks.toList ++ rest).distinct, newCC)
            case List() => cc
        }

    override val initialContext = {
        val initialGraph = rec(List(DeriveTask(0, startNode)), CtxGraph(Set(startNode), Set(), Results(), Results()))
        new SavingParsingCtx(0, initialGraph, Seq(), Seq(), (initialGraph.allConditions map { k => (k -> k) }).toMap)
    }
}
