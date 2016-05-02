package com.giyeok.jparser.preprocessed

import PreprocessedGrammar._
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.Inputs
import scala.Right
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.ParseTree.ParseNode
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.Kernels._
import com.giyeok.jparser.Inputs.{ ConcreteInput => Input }
import PreprocessedParser._

// KernelSet이 state의 의미라고 보면 됨

class PreprocessedParser(spec: PreprocessedGrammar) {
    sealed trait RevertTrigger { val node: ProgNode }
    case class IfLift(node: ProgNode) extends RevertTrigger
    case class IfAlive(node: ProgNode) extends RevertTrigger

    // join edge는 node에서 처리하도록 바꿔서 edge가 단순해짐
    case class Graph(nodes: Set[ProgNode], edges: Set[(ProgNode, ProgNode)],
                     edgeReverters: Map[RevertTrigger, Set[(ProgNode, ProgNode)]], liftBlockers: Map[ProgNode, ProgNode], reservedLiftReverters: Set[RevertTrigger]) {
        assert(nodes contains StartNode)
        // assert cycle이 없음

        def incomingNodesOf(node: ProgNode): Set[ProgNode] = edges collect { case (start, `node`) => start }
        def outgoingNodesOf(node: ProgNode): Set[ProgNode] = edges collect { case (`node`, end) => end }
        // edgesOf: (incomingNodesOf(node), outgoingNodesOf(node))
        def edgesOf(node: ProgNode): (Set[ProgNode], Set[ProgNode]) = edges.foldLeft((Set[ProgNode](), Set[ProgNode]())) { (cc, edge) =>
            edge match {
                case (start, `node`) => (cc._1 + start, cc._2)
                case (`node`, end) => (cc._1, cc._2 + end)
                case _ => cc
            }
        }

        def dropNodes(droppingNodes: Set[ProgNode]): Graph = {
            assert(droppingNodes subsetOf this.nodes)
            // edgeReverters 중 조건이 droppingNodes인 것도 지워야하고, 타겟이 droppingNodes와 관련 엣지인 것도 지워야 함
            // liftBlockers 중 조건이나 타겟이 droppingNodes인 것도 지워야 함
            // reservedLiftReverters 중 droppingNodes와 연관된 것도 지워야 함
            ???
        }

        // expansions: Set[(Node, SubDerive)] -> (node를 기준으로 subderive를 expand해서 나오는 그래프, 그 때 말단 leaf 노드들)
        def expandMulti(gen: Int, expansions: Set[(ProgNode, BaseSubDerive)]): (Graph, Set[LeafNode]) = {
            val cache = scala.collection.mutable.Map[SubDerive, ProgNode]()
            val newNodes = scala.collection.mutable.Set[ProgNode](nodes.toSeq: _*)
            val newEdges = scala.collection.mutable.Set[(ProgNode, ProgNode)](edges.toSeq: _*)
            val newEdgeReverters = scala.collection.mutable.Map[RevertTrigger, Set[(ProgNode, ProgNode)]](edgeReverters.toSeq: _*)
            val newLiftBlockers = scala.collection.mutable.Map[ProgNode, ProgNode](liftBlockers.toSeq: _*)
            val newReservedLiftReverters = scala.collection.mutable.Set[RevertTrigger](reservedLiftReverters.toSeq: _*)
            val leafNodes = scala.collection.mutable.Set[LeafNode]()

            def create(subderive: SubDerive): ProgNode = {
                def addNode(newNode: KernelProgNode): KernelProgNode = {
                    newNodes += newNode
                    if (newNode.isInstanceOf[LeafNode]) {
                        leafNodes += newNode.asInstanceOf[LeafNode]
                    }
                    newNode
                }
                cache get subderive match {
                    case Some(derived) =>
                        // 이미 subderive가 derive된 경우엔 엣지만 추가하고 끝낸다
                        derived
                    case None =>
                        // subderive가 derive되지 않은 경우
                        val derived: KernelProgNode = subderive match {
                            case AtomicSymbolSubDerive(kernel, derives, cycles) =>
                                val baseNode = addNode(AtomicNode(kernel, gen, cycles))
                                derives foreach { expand(baseNode, _) }
                                baseNode
                            case NonAtomicSymbolSubDerive(kernel, progress, derives, cycles) =>
                                val baseNode = addNode(NonAtomicNode(kernel, gen, progress, cycles))
                                derives foreach { expand(baseNode, _) }
                                baseNode
                            case JoinSubDerive(kernel, derive, join, cycles) =>
                                val baseNode = addNode(JoinNode(kernel, gen, cycles))
                                expand(baseNode, derive)
                                expand(baseNode, join)
                                baseNode
                            case TempLiftBlockSubDerive(kernel, derive, blockTrigger, cycles) =>
                                val baseNode = addNode(AtomicNode(kernel, gen, cycles))
                                expand(baseNode, derive)
                                newLiftBlockers += (create(blockTrigger) -> baseNode)
                                baseNode
                            case RevertableSubDerive(kernel, derive, revertTrigger, cycles) =>
                                val baseNode = addNode(AtomicNode(kernel, gen, cycles))
                                val deriveEdge = expand(baseNode, derive)
                                val trigger = IfLift(create(revertTrigger))
                                newEdgeReverters += (trigger -> (newEdgeReverters.getOrElse(trigger, Set()) + deriveEdge))
                                baseNode
                            case DeriveRevertableSubDerive(kernel, derive, deriveRevertTrigger, cycles) =>
                                val baseNode = addNode(AtomicNode(kernel, gen, cycles))
                                val deriveEdge = expand(baseNode, derive)
                                val deriveRevertTriggerEdge = expand(baseNode, deriveRevertTrigger)
                                val trigger = IfLift(create(deriveRevertTrigger))
                                newEdgeReverters += (trigger -> (newEdgeReverters.getOrElse(trigger, Set()) + deriveEdge))
                                baseNode
                            case ReservedLiftTriggeredLiftReverterDeriveExpansion(kernel, derive, cycles) =>
                                val baseNode = addNode(AtomicNode(kernel, gen, cycles))
                                expand(baseNode, derive)
                                newReservedLiftReverters += IfLift(baseNode)
                                baseNode
                            case ReservedAliveTriggeredLiftReverterDeriveExpansion(kernel, derive, cycles) =>
                                val baseNode = addNode(AtomicNode(kernel, gen, cycles))
                                expand(baseNode, derive)
                                newReservedLiftReverters += IfAlive(baseNode)
                                baseNode
                            case TermLeafNode(kernel) =>
                                addNode(TerminalNode(kernel, gen))
                            case EmptyLeafNode =>
                                addNode(EmptyNode(gen))
                        }
                        cache += (subderive -> derived)
                        derived
                }
            }
            def expand(node: ProgNode, subderive: SubDerive): (ProgNode, ProgNode) = {
                val newNode = create(subderive)
                val newEdge = (node -> newNode)
                newEdges += newEdge
                newEdge
            }
            expansions foreach { expansion =>
                val (baseNode, BaseSubDerive(derives)) = expansion
                derives foreach { expand(baseNode, _) }
            }
            assert(leafNodes.toSet[ProgNode] subsetOf newNodes.toSet)
            (Graph(newNodes.toSet, newEdges.toSet, newEdgeReverters.toMap, newLiftBlockers.toMap, newReservedLiftReverters.toSet), leafNodes.toSet)
        }

        // terminal node `tip`에서부터 lift를 진행해서
        // 그 결과로 나온 그래프, 그 과정에서 생긴 ipn들(lift한 뒤에도 derivable한 커널을 가진 노드), node -> lift된 결과
        def shrink(tip: LeafNode): (Graph, Set[ProgNode], Set[(ProgNode, ParseNode[Symbol])]) = {
            ???
        }

        // StartNode로부터 reachable하지 않은 노드/엣지를 모두 지운 그래프(reverter도 고려해야함)
        def trim: Graph = ???

        // StartNode로부터 reachable한 노드
        def reachables: Set[ProgNode] = ???

        def leafNodes: Set[ProgNode] = ???
    }
    case class Context(graph: Graph, ipns: Set[ProgNode], results: Set[ParseNode[Symbol]]) {
        assert(graph.leafNodes subsetOf ipns) // (이 시점에서는) graph의 말단 노드는 모두 ipn

        def proceed(nextGen: Int, input: Input): Either[Context, ParsingError] = {
            // 1. ipn 각각을 들어온 input에 맞게 expand한다
            val (droppedNodes, applicableExpansions) = {
                val x = ipns map { ipn =>
                    val expansions = (ipn match {
                        case StartNode => spec.startingExpansion
                        case n: KernelProgNode => spec.kernelExpansions(n.kernel)
                    })
                    (ipn, expansions.termExpansions find { _._1.contains(input) } map { _._2 })
                }
                val (applicables, dropped) = x.partition(_._2.isDefined)
                (dropped map { _._1 }, applicables map { p => (p._1, p._2.get) })
            }

            // 만약 expand 과정에서 들어온 input이 적용되는 expansion이 하나도 없는 경우엔 ParsingError
            if (applicableExpansions.isEmpty) {
                Right(UnexpectedInput(input))
            } else {
                // 더이상 가능성이 없는 노드를 제거하고
                val trimmedGraph = graph.dropNodes(droppedNodes)

                // TODO 이 시점에서 aliveTriggered reverter는 이미 적용이 가능하겠다

                // applicableExpansions 를 적용해서 그래프를 확장한 다음
                val (expandedGraph, tips) = trimmedGraph.expandMulti(nextGen, applicableExpansions)
                assert(expandedGraph.leafNodes forall { _.isInstanceOf[LeafNode] }) // (이 시점에서는) graph의 말단 노드가 모두 laef node(terminal 혹은 empty)여야 함
                // assert (앞서 dropNodes를 했기 때문에) 이 시점에서 expandedGraph에는 모든 노드에서 leaf node로 도달 가능해야 함

                // 2. expandedGraph에서 leaf node에서부터 역으로 lift를 진행한다
                val (shrunkGraph, newIpns, lifts) = tips.foldLeft((expandedGraph, Set[ProgNode](), Set[(ProgNode, ParseNode[Symbol])]())) { (cc, tip) =>
                    val (shrinkingGraph, ipnsCC, liftsCC) = cc
                    val shrunk = shrinkingGraph.shrink(tip)
                    (shrunk._1, ipnsCC ++ shrunk._2, liftsCC ++ shrunk._3)
                }

                // TODO reverter
                // reverter 처리시에는 shrunkGraph.edgeReverters 에서 트리거가 만족된 것들을 추려서
                // expandedGraph에서 대상 엣지/노드들을 제거하고(동시에 reverter도 제거하고) 2번의 shrink 과정만 다시 진행한다

                Left(Context(shrunkGraph.trim, newIpns, lifts collect { case (StartNode, result) => result }))
            }
        }
    }

    val initialContext = Context(Graph(Set(StartNode), Set(), Map(), Map(), Set()), Set(StartNode), spec.startingExpansion.immediateLifts)
}

object PreprocessedParser {
    sealed trait ProgNode {
        val derivedGen: Int
        val cycles: Cycles
    }
    case object StartNode extends ProgNode {
        val derivedGen = 0
        val cycles = Cycles(Set())
    }
    sealed trait KernelProgNode extends ProgNode {
        val kernel: Kernel
    }

    sealed trait LeafNode extends KernelProgNode
    case class TerminalNode(kernel: TerminalKernel, derivedGen: Int) extends LeafNode {
        val cycles = Cycles(Set())
    }
    case class EmptyNode(derivedGen: Int) extends LeafNode {
        val kernel = EmptyKernel
        val cycles = Cycles(Set())
    }

    case class AtomicNode(kernel: AtomicNontermKernel[AtomicSymbol with Nonterm], derivedGen: Int, cycles: Cycles) extends KernelProgNode
    case class ReservedLiftRevertableNode(kernel: AtomicNontermKernel[AtomicSymbol with Nonterm], derivedGen: Int, cycles: Cycles) extends KernelProgNode
    case class JoinNode(kernel: JoinKernel, derivedGen: Int, cycles: Cycles) extends KernelProgNode
    case class NonAtomicNode(kernel: Kernel, derivedGen: Int, progress: Seq[ParseNode[Symbol]], cycles: Cycles) extends KernelProgNode
}
