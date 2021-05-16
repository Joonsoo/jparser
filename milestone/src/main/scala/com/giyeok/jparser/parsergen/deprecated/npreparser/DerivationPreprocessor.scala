package com.giyeok.jparser.parsergen.deprecated.npreparser

import com.giyeok.jparser.Inputs.{Input, TermGroupDesc}
import com.giyeok.jparser.NGrammar.NTerminal
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always}
import com.giyeok.jparser.nparser.ParsingContext.{Graph, Kernel, Node}
import com.giyeok.jparser.nparser.ParsingTasks
import com.giyeok.jparser.parsergen.utils.TermGrouper

import scala.annotation.tailrec


trait DerivationPreprocessor extends ParsingTasks {

    case class Preprocessed(base: Node, lifted: Cont, baseTasks: List[ProgressTask], nextGraph: Graph, nextDeriveTips: Set[Node]) {
        def instantiate(beginGen: Int, endGen: Int, condition: AcceptCondition): Preprocessed = {
            // base 노드가 실제로는 beginGen..endGen을 커버하고 Always가 아니라 condition 조건을 달고 있음 - symbolId, pointer는 동일
            // preprocessed의 모든 그래프는 base로부터 derive된 것이므로 다른 노드들의 accept condition에는 영향이 없고 모두 endGen으로 shift하면 됨
            // tasks
            val base1 = Node(Kernel(base.kernel.symbolId, base.kernel.pointer, beginGen, endGen), condition)
            val nodeMapper = { (node: Node) =>
                node match {
                    case `base` => base1
                    case other => other.shiftGen(endGen)
                }
            }
            val lifted1 = lifted.graph mapNode nodeMapper
            //            val updated1 = lifted.updatedNodesMap map { kv =>
            //                nodeMapper(kv._1) -> (kv._2 map nodeMapper)
            //            }
            //            val tasks1 = baseTasks map { progressTask =>
            //                // TODO 다시 봐야함
            //                ProgressTask(nodeMapper(progressTask.node), progressTask.condition.shiftGen(endGen))
            //            }
            //            val nextGraph1 = nextGraph mapNode nodeMapper
            //            val nextDeriveTips1 = nextDeriveTips map { _.shiftGen(endGen) }
            //            Preprocessed(base1, Cont(lifted1, updated1), tasks1, nextGraph1, nextDeriveTips1)
            ???
        }
    }

    private var cache = Map[(Int, Int), (Preprocessed, Map[TermGroupDesc, Preprocessed])]()

    @tailrec private def rootBlockingRec(nextGen: Int, root: Node, tasks: List[Task], cc: Cont, rootTasks: List[ProgressTask]): (Cont, List[ProgressTask]) =
        tasks match {
            case (task@ProgressTask(`root`, _)) +: rest =>
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
            case (task@ProgressTask(`root`, _)) +: rest =>
                deriveBlockingRec(nextGen, root, rest, cc, task +: rootTasks, tipsTasks)
            case (FinishTask(`root`)) +: _ =>
                ??? // should not happen
            case (task@DeriveTask(Node(kernel, _))) +: rest if kernel.pointer > 0 =>
                deriveBlockingRec(nextGen, root, rest, cc, rootTasks, task +: tipsTasks)
            case task +: rest =>
                val (ncc, newTasks) = process(nextGen, task, cc)
                deriveBlockingRec(nextGen, root, newTasks ++: rest, ncc, rootTasks, tipsTasks)
            case List() => (cc, rootTasks, tipsTasks)
        }

    def sliceOf(symbolId: Int, pointer: Int): (Preprocessed, Map[TermGroupDesc, Preprocessed]) = {
        cache get ((symbolId, pointer)) match {
            case Some(cached) => cached
            case None =>
                // TODO base preprocessed를 구할 때는 root에 대한 Progress/Finish task만 막으면 되고
                // TODO slice할 때는 kernel.pointer > 0인 모든 DeriveTask도 막아야 되고
                val baseNode = Node(Kernel(symbolId, pointer, 0, 0), Always)
                val (lifted, rootTasks) = rootBlockingRec(0, baseNode, List(DeriveTask(baseNode)), Cont(Graph(Set(baseNode), Set()), Map()), List())
                val nextGraph = trimGraph(lifted.graph, baseNode, 0)
                val base = Preprocessed(baseNode, lifted, rootTasks, nextGraph, Set(baseNode))
                val termGroups = TermGrouper.termGroupsOf(termNodes(lifted.graph, 0) map { node =>
                    grammar.symbolOf(node.kernel.symbolId).asInstanceOf[NTerminal].symbol
                })
                val slicedMap = (termGroups map { termGroup =>
                    val termFinishes = finishableTermNodes(nextGraph, 0, termGroup).toList map {
                        ProgressTask(_, Always)
                    }
                    val (partialLifted, liftedRootTasks, tipsTasks) = deriveBlockingRec(1, baseNode, termFinishes, Cont(lifted.graph, Map()), List(), List())
                    val nextDeriveTips = (tipsTasks map {
                        _.node
                    }).toSet
                    val nextNextGraph = trimUnreachables(partialLifted.graph, baseNode, nextDeriveTips.toSet)
                    val sliced = Preprocessed(baseNode, partialLifted, liftedRootTasks, nextNextGraph, nextDeriveTips)
                    termGroup -> sliced
                }).toMap
                cache += (symbolId, pointer) -> (base, slicedMap)
                (base, slicedMap)
        }
    }

    def sliceOf(node: Node, input: Input): Option[Preprocessed] = {
        val Node(kernel, condition) = node
        val sliceMap = sliceOf(kernel.symbolId, kernel.pointer)._2

        assert((sliceMap count {
            _._1 contains input
        }) <= 1)

        sliceMap find {
            _._1 contains input
        } map {
            _._2
        } map { preprocessed =>
            assert(preprocessed.base.kernel.symbolId == node.kernel.symbolId)
            assert(preprocessed.base.kernel.pointer == node.kernel.pointer)
            preprocessed.instantiate(kernel.beginGen, kernel.endGen, condition)
        }
    }
}
