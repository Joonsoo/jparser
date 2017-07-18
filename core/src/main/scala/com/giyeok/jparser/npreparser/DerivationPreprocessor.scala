package com.giyeok.jparser.npreparser

import scala.annotation.tailrec
import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.Inputs.VirtualTermGroupDesc
import com.giyeok.jparser.Symbols.Terminal
import com.giyeok.jparser.Symbols.Terminals.CharacterTerminal
import com.giyeok.jparser.Symbols.Terminals.VirtualTerminal
import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.AcceptCondition.Always
import com.giyeok.jparser.nparser.NGrammar.NTerminal
import com.giyeok.jparser.nparser.ParsingContext.Graph
import com.giyeok.jparser.nparser.ParsingContext.Kernel
import com.giyeok.jparser.nparser.ParsingContext.Node
import com.giyeok.jparser.nparser.ParsingTasks

trait DerivationPreprocessor extends ParsingTasks {
    def termGroupsOf(terminals: Set[Terminal]): Set[TermGroupDesc] = {
        val charTerms: Set[CharacterTermGroupDesc] = terminals collect { case x: CharacterTerminal => TermGroupDesc.descOf(x) }
        val virtTerms: Set[VirtualTermGroupDesc] = terminals collect { case x: VirtualTerminal => TermGroupDesc.descOf(x) }

        def sliceTermGroups(termGroups: Set[CharacterTermGroupDesc]): Set[CharacterTermGroupDesc] = {
            val charIntersects: Set[CharacterTermGroupDesc] = termGroups flatMap { term1 =>
                termGroups collect {
                    case term2 if term1 != term2 => term1 intersect term2
                } filterNot { _.isEmpty }
            }
            val essentials = (termGroups map { g => charIntersects.foldLeft(g) { _ - _ } }) filterNot { _.isEmpty }
            val intersections = if (charIntersects.isEmpty) Set() else sliceTermGroups(charIntersects)
            essentials ++ intersections
        }
        val charTermGroups = sliceTermGroups(charTerms)

        val virtIntersects: Set[VirtualTermGroupDesc] = virtTerms flatMap { term1 =>
            virtTerms collect {
                case term2 if term1 != term2 => term1 intersect term2
            } filterNot { _.isEmpty }
        }
        val virtTermGroups = (virtTerms map { term =>
            virtIntersects.foldLeft(term) { _ - _ }
        }) ++ virtIntersects

        (charTermGroups ++ virtTermGroups) filterNot { _.isEmpty }
    }

    case class DerivePreprocessed(baseNode: Node, graph: Graph, updatedNodesMap: Map[Node, Set[Node]], baseNodeTasks: List[ProgressTask]) {
        assert(graph.nodes contains baseNode)
        assert(updatedNodesMap.keySet subsetOf graph.nodes)
        assert(updatedNodesMap.values.flatten.toSet subsetOf graph.nodes)
        assert(baseNodeTasks forall { _.node == baseNode })

        def conform(beginGen: Int, endGen: Int, condition: AcceptCondition): DerivePreprocessed = {
            // base노드만 beginGen..endGen 및 condition을 갖고 그 외의 노드는 모두 endGen으로 shift한 것
            val base1 = Node(Kernel(baseNode.kernel.symbolId, baseNode.kernel.pointer, beginGen, endGen)(baseNode.kernel.symbol), condition)
            val nodesMap = (graph.nodes map { node =>
                val newNode = node match {
                    case `baseNode` => base1
                    case other => other.shiftGen(endGen)
                }
                node -> newNode
            }).toMap
            val graph1 = graph mapNode nodesMap
            val updated1 = updatedNodesMap map { kv =>
                nodesMap(kv._1) -> (kv._2 map nodesMap)
            }
            val tasks1 = baseNodeTasks map { progressTask =>
                ProgressTask(nodesMap(progressTask.node), progressTask.condition.shiftGen(endGen))
            }
            DerivePreprocessed(base1, graph1, updated1, tasks1)
        }

        lazy val slices: Map[TermGroupDesc, ProgressPreprocessed] = {
            val terminalNodes = termNodes(graph, 0)
            val termGroups = termGroupsOf(terminalNodes map { _.kernel.symbol.asInstanceOf[NTerminal].symbol })

            (termGroups map { termGroup =>
                val initialTasks = terminalNodes filter { _.kernel.symbol.asInstanceOf[NTerminal].symbol.accept(termGroup) } map { ProgressTask(_, Always) }
                val (Cont(liftedGraph, updatedNodesMap), baseNodeProgresses, tipDerives) = deriveBlockingRec(1, baseNode, initialTasks.toList, Cont(graph, Map()), List(), List())
                // TODO trim graph
                termGroup -> ProgressPreprocessed(baseNode, liftedGraph, updatedNodesMap, (tipDerives map { _.node }).toSet, baseNodeProgresses)
            }).toMap
        }
    }
    case class ProgressPreprocessed(baseNode: Node, graph: Graph, updatedNodesMap: Map[Node, Set[Node]], nextDeriveTips: Set[Node], baseNodeTasks: List[ProgressTask]) {
        assert(graph.nodes contains baseNode)
        assert(baseNodeTasks forall { _.node == baseNode })
        assert(nextDeriveTips subsetOf graph.nodes)
        assert(!(nextDeriveTips contains baseNode))

        val trimmedGraph: Graph = trimUnreachables(graph, baseNode, nextDeriveTips)

        def conform(beginGen: Int, endGen: Int, condition: AcceptCondition): ProgressPreprocessed = {
            // base노드만 beginGen..endGen 및 condition을 갖고 그 외의 노드는 모두 endGen으로 shift한 것
            val base1 = Node(Kernel(baseNode.kernel.symbolId, baseNode.kernel.pointer, beginGen, endGen)(baseNode.kernel.symbol), condition)
            val nodesMap = (graph.nodes map { node =>
                val newNode = node match {
                    case `baseNode` => base1
                    case other => other.shiftGen(endGen)
                }
                node -> newNode
            }).toMap
            val graph1 = graph mapNode nodesMap
            val updated1 = updatedNodesMap map { kv =>
                nodesMap(kv._1) -> (kv._2 map nodesMap)
            }
            val nextDeriveTips1 = nextDeriveTips map { _.shiftGen(endGen) }
            val tasks1 = baseNodeTasks map { progressTask =>
                ProgressTask(nodesMap(progressTask.node), progressTask.condition.shiftGen(endGen))
            }
            ProgressPreprocessed(base1, graph1, updated1, nextDeriveTips1, tasks1)
        }
    }

    private var cache = Map[(Int, Int), DerivePreprocessed]()

    @tailrec private def rootBlockingRec(nextGen: Int, root: Node, tasks: List[Task], cc: Cont, rootTasks: List[ProgressTask]): (Cont, List[ProgressTask]) =
        tasks match {
            case (task @ ProgressTask(`root`, _)) +: rest =>
                rootBlockingRec(nextGen, root, rest, cc, task +: rootTasks)
            case (FinishTask(`root`)) +: _ =>
                ??? // should not happen
            case task +: rest =>
                val (ncc, newTasks) = process(nextGen, task, cc)
                rootBlockingRec(nextGen, root, newTasks ++: rest, ncc, rootTasks)
            case List() => (cc, rootTasks)
        }

    @tailrec private def deriveBlockingRec(nextGen: Int, root: Node, tasks: List[Task], cc: Cont, rootTasks: List[ProgressTask], tipsTasks: List[DeriveTask]): (Cont, List[ProgressTask], List[DeriveTask]) =
        tasks match {
            case (task @ ProgressTask(`root`, _)) +: rest =>
                deriveBlockingRec(nextGen, root, rest, cc, task +: rootTasks, tipsTasks)
            case (FinishTask(`root`)) +: _ =>
                ??? // should not happen
            case (task @ DeriveTask(Node(kernel, _))) +: rest if kernel.pointer > 0 =>
                deriveBlockingRec(nextGen, root, rest, cc, rootTasks, task +: tipsTasks)
            case task +: rest =>
                val (ncc, newTasks) = process(nextGen, task, cc)
                deriveBlockingRec(nextGen, root, newTasks ++: rest, ncc, rootTasks, tipsTasks)
            case List() => (cc, rootTasks, tipsTasks)
        }

    def sliceOf(symbolId: Int, pointer: Int): DerivePreprocessed = {
        cache get ((symbolId, pointer)) match {
            case Some(cached) => cached
            case None =>
                // TODO base preprocessed를 구할 때는 root에 대한 Progress/Finish task만 막으면 되고
                // TODO slice할 때는 kernel.pointer > 0인 모든 DeriveTask도 막아야 되고
                val baseNode = Node(Kernel(symbolId, pointer, 0, 0)(grammar.symbolOf(symbolId)), Always)
                val (Cont(liftedGraph, updatedNodesMap), baseNodeTasks) = rootBlockingRec(0, baseNode, List(DeriveTask(baseNode)), Cont(Graph(Set(baseNode), Set()), Map()), List())

                val preprocessed = DerivePreprocessed(baseNode, liftedGraph, updatedNodesMap, baseNodeTasks)
                cache += (symbolId, pointer) -> preprocessed
                preprocessed
        }
    }

    def sliceOf(node: Node, input: Input): Option[ProgressPreprocessed] = {
        val Node(kernel, condition) = node
        val deriveProgress = sliceOf(kernel.symbolId, kernel.pointer)

        val slices = deriveProgress.slices filter { _._1 contains input }
        assert(slices.size <= 1)

        slices.headOption map { p =>
            val preprocessed = p._2
            assert(preprocessed.baseNode.kernel.symbolId == node.kernel.symbolId)
            assert(preprocessed.baseNode.kernel.pointer == node.kernel.pointer)
            preprocessed.conform(kernel.beginGen, kernel.endGen, condition)
        }
    }
}
