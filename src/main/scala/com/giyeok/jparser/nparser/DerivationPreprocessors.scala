package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.DerivationPreprocessor.Preprocessed
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.EligCondition.Condition
import com.giyeok.jparser.nparser.EligCondition.True
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.Inputs.Input
import scala.annotation.tailrec
import com.giyeok.jparser.nparser.NGrammar._

class OnDemandDerivationPreprocessor(val grammar: NGrammar, val compaction: Boolean) extends DerivationPreprocessor with ParsingTasks {
    private val symbolDerivations = scala.collection.mutable.Map[Int, Preprocessed]()
    private val sequenceDerivations = scala.collection.mutable.Map[(Int, Int), Preprocessed]()

    private val symbolTermNodes = scala.collection.mutable.Map[Int, Set[SymbolNode]]()
    private val sequenceTermNodes = scala.collection.mutable.Map[(Int, Int), Set[SymbolNode]]()

    private val followingSymbolsMap = scala.collection.mutable.Map[Int, Set[Int]]()
    def followingSymbols: Map[Int, Set[Int]] = followingSymbolsMap.toMap

    def compaction(preprocessed: Preprocessed): Preprocessed = {
        if (preprocessed.context.graph.nodes contains preprocessed.baseNode) {
            val initialBarriers: Set[Node] = Set(preprocessed.baseNode) ++ (preprocessed.context.graph.nodes flatMap {
                case node @ SymbolNode(symbolId, _) =>
                    grammar.nsymbols(symbolId) match {
                        case _: Start | _: Nonterminal | _: OneOf | _: Proxy | _: Repeat => None
                        case _: Except => Set(node) ++ (preprocessed.context.graph.edgesByStart(node) map { _.asInstanceOf[SimpleEdge].end })
                        case _: Join => Set(node) ++ (preprocessed.context.graph.edgesByStart(node) map { _.asInstanceOf[JoinEdge] } flatMap { n => Set(n.end, n.join) })
                        case _ => Some(node)
                    }
                case node @ SequenceNode(sequenceId, pointer, _, _) =>
                    if (grammar.nsequences(sequenceId).sequence.length != 1) Some(node)
                    else None ensuring (pointer == 0)
            })
            val initialStartNodes = initialBarriers filter {
                case SymbolNode(symbolId, _) =>
                    grammar.nsymbols(symbolId) match {
                        case _: Except | _: Join => false
                        case _ => true
                    }
                case _ => true
            }

            def reachables(graph: Graph, barriers: Set[Node], queue: List[Node], cc: (Set[Node], Set[Node])): (Set[Node], Set[Node]) =
                queue match {
                    case node +: rest =>
                        val outgoingNodes = graph.edgesByStart(node) map { _.asInstanceOf[SimpleEdge].end }
                        val outgoingBarriers = outgoingNodes intersect barriers
                        val outgoingReachables = outgoingNodes -- barriers
                        val newReachables = outgoingReachables -- cc._2
                        reachables(graph, barriers, rest ++ newReachables, (cc._1 ++ outgoingBarriers, cc._2 ++ newReachables))
                    case List() => cc
                }

            def reachablePaths(graph: Graph, barriers: Set[Node], node: Node, path: Set[Node]): Map[Node, Set[Node]] = {
                val outgoingNodes = (graph.edgesByStart(node) map { _.asInstanceOf[SimpleEdge].end })
                val (outgoingBarriers, outgoingReachables) = (outgoingNodes intersect barriers, outgoingNodes -- barriers)
                val outgoingPaths = (outgoingReachables -- path) map { outgoing => reachablePaths(graph, barriers, outgoing, path + outgoing) }
                outgoingPaths.foldLeft((outgoingBarriers map { _ -> path }).toMap) { (paths, merging) =>
                    merging.foldLeft(paths) { (paths, kv) =>
                        val (dest, path) = kv
                        paths + (dest -> (paths.getOrElse(dest, Set()) ++ path))
                    }
                }
            }

            def compaction(startNodes: List[Node], barriers: Set[Node], cc: Preprocessed): Preprocessed = {
                startNodes match {
                    case startNode +: rest =>
                        val graph = cc.context.graph
                        assert(barriers contains startNode)
                        val (reachableBarriers, reachableNodes) = reachables(graph, barriers, List(startNode), (Set(), Set()))
                        // reachableNodes에 reachableNodes 이외의 노드에서 오는 incoming edges가 있는 노드들을 incomingNodes라고 하고
                        // incomingNodes가 비어있지 않으면 barriers와 startNodes로 추가해서 다시 진행
                        val allReachables = reachableNodes + startNode
                        // TODO reachableBarriers에서 들어오는 엣지는 어떻게 할지 고민
                        val incomingEdges = reachableNodes flatMap { graph.edgesByDest(_) } filterNot { allReachables contains _.start }
                        if (incomingEdges.isEmpty) {
                            // reachableNodes 없애고
                            val newGraph0 = graph.removeNodes(reachableNodes)
                            // startNode -> reachableBarrier로 가는 엣지 추가
                            val newGraph = reachableBarriers.foldLeft(newGraph0) { (g, b) => g.addEdge(SimpleEdge(startNode, b)) }
                            compaction(rest, barriers, cc.updateContext(cc.context.updateGraph(newGraph)))
                        } else {
                            val incomingNodes = incomingEdges map { _.end }
                            compaction(startNodes ++ incomingNodes, barriers ++ incomingNodes, cc)
                        }
                    case List() => cc
                }
            }
            compaction(initialStartNodes.toList, initialBarriers, preprocessed)
        } else {
            preprocessed
        }
    }
    def compactionIfNeeded(preprocessed: Preprocessed) = if (compaction) compaction(preprocessed) else preprocessed

    @tailrec private def recNoBase(baseNode: Node, nextGen: Int, tasks: List[Task], cc: Preprocessed): Preprocessed =
        tasks match {
            case FinishTask(`baseNode`, condition, lastSymbol) +: rest =>
                recNoBase(baseNode, nextGen, rest, cc.addBaseFinish(condition, lastSymbol))
            case ProgressTask(`baseNode`, condition) +: rest =>
                recNoBase(baseNode, nextGen, rest, cc.addBaseProgress(condition))
            case task +: rest =>
                val (newContext, newTasks) = process(nextGen, task, cc.context)
                recNoBase(baseNode, nextGen, newTasks ++: rest, cc.updateContext(newContext))
            case List() => cc
        }

    def symbolDerivationOf(symbolId: Int): Preprocessed = {
        symbolDerivations get symbolId match {
            case Some(preprocessed) => preprocessed
            case None =>
                val baseNode = SymbolNode(symbolId, -1)
                val initialPreprocessed = Preprocessed(baseNode, Context(Graph(Set(baseNode), Set()), Results(), Results()), Seq(), Seq())
                val preprocessed = compactionIfNeeded(recNoBase(baseNode, 0, List(DeriveTask(baseNode)), initialPreprocessed))
                symbolDerivations(symbolId) = preprocessed
                preprocessed
        }
    }
    def sequenceDerivationOf(sequenceId: Int, pointer: Int): Preprocessed = {
        sequenceDerivations get (sequenceId, pointer) match {
            case Some(baseNodeAndDerivation) => baseNodeAndDerivation
            case None =>
                val baseNode = SequenceNode(sequenceId, pointer, -1, -1)
                val initialPreprocessed = Preprocessed(baseNode, Context(Graph(Set(baseNode), Set()), Results(baseNode -> Set[Condition]()), Results()), Seq(), Seq())
                val preprocessed = compactionIfNeeded(recNoBase(baseNode, 0, List(DeriveTask(baseNode)), initialPreprocessed))
                sequenceDerivations((sequenceId, pointer)) = preprocessed
                preprocessed
        }
    }

    def symbolTermNodesOf(symbolId: Int): Set[SymbolNode] = {
        symbolTermNodes get symbolId match {
            case Some(termNodes) => termNodes
            case None =>
                val termNodes: Set[SymbolNode] = symbolDerivationOf(symbolId).context.graph.nodes collect {
                    case node @ SymbolNode(symbolId, _) if grammar.nsymbols(symbolId).isInstanceOf[NGrammar.Terminal] => node
                }
                symbolTermNodes(symbolId) = termNodes
                termNodes
        }
    }
    def sequenceTermNodesOf(sequenceId: Int, pointer: Int): Set[SymbolNode] = {
        sequenceTermNodes get (sequenceId, pointer) match {
            case Some(termNodes) => termNodes
            case None =>
                val termNodes: Set[SymbolNode] = sequenceDerivationOf(sequenceId, pointer).context.graph.nodes collect {
                    case node @ SymbolNode(symbolId, beginGen) if grammar.nsymbols(symbolId).isInstanceOf[NGrammar.Terminal] => node
                }
                sequenceTermNodes((sequenceId, pointer)) = termNodes
                termNodes
        }
    }
}

class OnDemandSlicedDerivationPreprocessor(grammar: NGrammar, override val compaction: Boolean) extends OnDemandDerivationPreprocessor(grammar, false) with SlicedDerivationPreprocessor {
    // slice는 TermGroupDesc -> Preprocessed + new derive tips, finish나 progress는 baseNode에 대해서만 수행한다
    private val symbolSliced = scala.collection.mutable.Map[Int, Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])]]()
    private val sequenceSliced = scala.collection.mutable.Map[(Int, Int), Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])]]()

    @tailrec private def recNoBaseNoDerive(baseNode: Node, nextGen: Int, tasks: List[Task], cc: Preprocessed, deriveTips: Set[SequenceNode]): (Preprocessed, Set[SequenceNode]) =
        tasks match {
            case DeriveTask(deriveTip: SequenceNode) +: rest =>
                // context에 deriveTip의 finish task 추가
                val preprocessed = derivationOf(deriveTip)
                assert(preprocessed.baseFinishes.isEmpty)
                val immediateProgresses = preprocessed.baseProgresses map { condition => ProgressTask(deriveTip, condition.shiftGen(nextGen)) }
                val ncc = cc.updateContext(cc.context.updateFinishes(_.merge(preprocessed.context.finishes.shiftGen(nextGen))))
                recNoBaseNoDerive(baseNode, nextGen, immediateProgresses ++: rest, ncc, deriveTips + deriveTip)
            case FinishTask(`baseNode`, condition, lastSymbol) +: rest =>
                recNoBaseNoDerive(baseNode, nextGen, rest, cc.addBaseFinish(condition, lastSymbol), deriveTips)
            case ProgressTask(`baseNode`, condition) +: rest =>
                recNoBaseNoDerive(baseNode, nextGen, rest, cc.addBaseProgress(condition), deriveTips)
            case task +: rest =>
                assert(!task.isInstanceOf[DeriveTask])
                val (newContext, newTasks) = process(nextGen, task, cc.context)
                recNoBaseNoDerive(baseNode, nextGen, newTasks ++: rest, cc.updateContext(newContext), deriveTips)
            case List() =>
                (cc, deriveTips)
        }

    private def slice(derivation: Preprocessed, termNodes: Set[SymbolNode]): Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])] = {
        val terminals = termNodes map { node => grammar.nsymbols(node.symbolId).asInstanceOf[NGrammar.Terminal].symbol }
        val termGroups = termGroupsOf(terminals)
        (termGroups map { termGroup =>
            val finishables = finishableTermNodes(derivation.context, 0, termGroup)
            val finishTasks = finishables.toList map { FinishTask(_, True, None) }
            val cc = Preprocessed(derivation.baseNode, derivation.context.emptyFinishes, Seq(), Seq())
            val (newPreprocessed, newDeriveTips) = recNoBaseNoDerive(derivation.baseNode, 1, finishTasks, cc, Set())
            val trimStarts = (Set(derivation.baseNode)) ++ (derivation.context.finishes.conditionNodes) ++ (derivation.context.progresses.conditionNodes)
            val trimmedContext = trim(newPreprocessed.context, trimStarts, newDeriveTips.asInstanceOf[Set[Node]])
            val sequenceNodes = trimmedContext.graph.nodes collect { case n: SequenceNode => n }
            val sliced = (compactionIfNeeded(newPreprocessed.updateContext(trimmedContext)), newDeriveTips intersect sequenceNodes)
            (termGroup -> sliced)
        }).toMap
    }
    def symbolSliceOf(symbolId: Int): Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])] = {
        symbolSliced get symbolId match {
            case Some(slicedMap) => slicedMap
            case None =>
                val slicedMap = slice(symbolDerivationOf(symbolId), symbolTermNodesOf(symbolId))
                symbolSliced(symbolId) = slicedMap
                slicedMap
        }
    }
    def sequenceSliceOf(sequenceId: Int, pointer: Int): Map[TermGroupDesc, (Preprocessed, Set[SequenceNode])] = {
        sequenceSliced get (sequenceId, pointer) match {
            case Some(slicedMap) => slicedMap
            case None =>
                val slicedMap = slice(sequenceDerivationOf(sequenceId, pointer), sequenceTermNodesOf(sequenceId, pointer))
                sequenceSliced((sequenceId, pointer)) = slicedMap
                slicedMap
        }
    }
}
