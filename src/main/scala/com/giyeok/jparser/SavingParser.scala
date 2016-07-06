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

                val conditionNextFate0 = (conditionFate map { kv => (kv._1 -> kv._2.evaluate(nextGen, trimmedGraph.results, trimmedGraph.nodes)) }) ++ (revertedGraph.allConditions map { k => (k -> k) }).toMap
                val conditionNextFate = conditionNextFate0 filter { _._2 != Condition.False }
                val nextContext = new SavingParsingCtx(nextGen, revertedGraph, inputHistory :+ input, resultHistory :+ currResult, conditionNextFate)
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
                    conditionFate get (nc._2) match {
                        case Some(fate) => fate.eligible
                        case None => false
                    }
                }).entries
                (eligibleEntries map { _._1 }).toSet
            }

            import Symbols._
            def derive(symbol: AtomicNonterm): Set[Symbol] = symbol match {
                case Start => Set(grammar.startSymbol)
                case Nonterminal(nonterminalName) => grammar.rules(nonterminalName)
                case OneOf(syms) => syms
                case Proxy(sym) => Set(sym)
                case repeat: Repeat => Set(repeat.baseSeq, repeat.repeatSeq)
                case Join(sym, join) => ??? // should not happen
                case _: LookaheadIs | _: LookaheadExcept => ??? // should not happen
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
                def atomicNodeOf(symbol: AtomicSymbol, beginGen: Int): Node = symbol match {
                    case term: Terminal => TermNode(term, beginGen)
                    case nonterm: AtomicNonterm => AtomicNode(nonterm, beginGen)
                }
                node match {
                    case TermNode(symbol, beginGen) =>
                        assert(gen == beginGen + 1)
                        resultFunc.bind(symbol, resultFunc.terminal(beginGen, inputHistory(beginGen)))

                    case AtomicNode(symbol: Join, beginGen) =>
                        resultFunc.bind(symbol, resultFunc.join(symbol, reconstruct(atomicNodeOf(symbol.sym, beginGen), gen), reconstruct(atomicNodeOf(symbol.join, beginGen), gen)))
                    case AtomicNode(symbol: Lookahead, beginGen) =>
                        resultFunc.bind(symbol, resultFunc.sequence(beginGen, Sequence(Seq(), Set())))
                    case AtomicNode(symbol, beginGen) =>
                        // beginGen..gen을 커버하는 AtomicNode result 중에 symbol에서 derive될 수 있는 것들 추리기
                        // beginGen..gen을 커버하는 SequenceNode result 중에 symbol에서 derive될 수 있는 것들 추리기
                        val merging = actualHistory(gen) collect {
                            case child: TermNode if (beginGen == child.beginGen) && (derive(symbol) contains child.symbol) =>
                                resultFunc.bind(symbol, reconstruct(child, gen))
                            case child: AtomicNode if (beginGen == child.beginGen) && (derive(symbol) contains child.symbol) =>
                                resultFunc.bind(symbol, reconstruct(child, gen))
                            case child: SequenceNode if (derive(symbol) contains child.symbol) && (child.beginGen == beginGen) && (child.symbol.seq.length == 0) =>
                                resultFunc.bind(symbol, resultFunc.sequence(child.beginGen, child.symbol))
                            case child: SequenceNode if (derive(symbol) contains child.symbol) && (child.beginGen == beginGen) && (child.pointer + 1 == child.symbol.seq.length) =>
                                // sequence가 완전히 끝나는 경우
                                // 맨 마지막엔 whitespace가 올 수 없음
                                val lastChildSym = child.symbol.seq(child.pointer)
                                val childNode = atomicNodeOf(lastChildSym, child.endGen)
                                assert(actualHistory(gen) contains childNode)
                                resultFunc.bind(symbol, resultFunc.append(reconstruct(child, child.endGen), reconstruct(childNode, gen)))
                        }
                        assert(!merging.isEmpty)
                        // merge를 먼저 하고 bind를 하면 merge할 때 root가 바뀌어서 안되는 경우가 있을 수 있음
                        resultFunc.merge(merging).get

                    case SequenceNode(symbol, 0, beginGen, endGen) =>
                        resultFunc.sequence(beginGen, symbol) ensuring (gen == endGen && beginGen == endGen)

                    case SequenceNode(symbol, pointer, beginGen, endGen) =>
                        assert(gen == endGen)

                        val lastChildSym = symbol.seq(pointer - 1)
                        // actualHistory(gen) 에 lastChildSym이 끝난 것들이 있으면, 각각에 대해서 이 sequencenode의 child로 들어갈 수 있는지 확인(pointer가 하나 작은 sequence node가 child candidate node의 beginGen에서 끝났으면)
                        // whitespace도 비슷하게 처리
                        val mergingContent = actualHistory(gen) flatMap {
                            case child: AtomicNode if child.symbol == lastChildSym =>
                                // actualHistory(childGen)에 SequenceNode랑 symbol, pointer - 1, ?beginGen, 
                                actualHistory(gen) collect {
                                    case prevSeq: SequenceNode if prevSeq.symbol == symbol && prevSeq.pointer == pointer - 1 && prevSeq.beginGen == beginGen && prevSeq.endGen == child.beginGen =>
                                        resultFunc.append(reconstruct(prevSeq, child.beginGen), reconstruct(child, gen))
                                }
                            case child: TermNode if child.symbol == lastChildSym =>
                                // actualHistory(childGen)에 SequenceNode랑 symbol, pointer - 1, ?beginGen, 
                                actualHistory(gen) collect {
                                    case prevSeq: SequenceNode if prevSeq.symbol == symbol && prevSeq.pointer == pointer - 1 && prevSeq.beginGen == beginGen && prevSeq.endGen == child.beginGen =>
                                        resultFunc.append(reconstruct(prevSeq, child.beginGen), reconstruct(child, gen))
                                }
                            case _ => Seq()
                        }

                        val mergingWhitespace = {
                            val whitespaceSyms = symbol.whitespace
                            if (whitespaceSyms.isEmpty) Seq() else {
                                actualHistory(gen) flatMap {
                                    case child: AtomicNode if whitespaceSyms contains child.symbol =>
                                        // actualHistory(childGen)에 SequenceNode랑 symbol, pointer - 1, ?beginGen, 
                                        actualHistory(gen) collect {
                                            case prevSeq: SequenceNode if prevSeq.symbol == symbol && prevSeq.pointer == pointer && prevSeq.beginGen == beginGen && prevSeq.endGen == child.beginGen =>
                                                resultFunc.appendWhitespace(reconstruct(prevSeq, child.beginGen), reconstruct(child, gen))
                                        }
                                    case child: TermNode if whitespaceSyms contains child.symbol =>
                                        // actualHistory(childGen)에 SequenceNode랑 symbol, pointer - 1, ?beginGen, 
                                        actualHistory(gen) collect {
                                            case prevSeq: SequenceNode if prevSeq.symbol == symbol && prevSeq.pointer == pointer && prevSeq.beginGen == beginGen && prevSeq.endGen == child.beginGen =>
                                                resultFunc.appendWhitespace(reconstruct(prevSeq, child.beginGen), reconstruct(child, gen))
                                        }
                                    case _ => Seq()
                                }
                            }
                        }

                        val merging = mergingContent ++ mergingWhitespace
                        assert(!merging.isEmpty)
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
