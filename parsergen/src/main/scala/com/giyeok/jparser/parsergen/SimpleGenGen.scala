package com.giyeok.jparser.parsergen

import com.giyeok.jparser.Inputs.{CharacterTermGroupDesc, TermGroupDesc}
import com.giyeok.jparser.Symbols.Terminal
import com.giyeok.jparser.examples.ExpressionGrammars
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar.{NAtomicSymbol, NSequence, NTerminal}
import com.giyeok.jparser.parsergen.SimpleGen.Action
import com.giyeok.jparser.parsergen.SimpleGenGen.KAction
import com.giyeok.jparser.study.parsergen.{AKernel, GrammarAnalyzer}

object SimpleGenGen {

    sealed trait KAction

    case class Append(appendKernels: Set[AKernel], pendingFinish: Boolean) extends KAction

    case class Replace(replaceKernels: Set[AKernel]) extends KAction

    case class InReplaceAndAppend(inreplaceKernels: Set[AKernel], appendKernels: Set[AKernel], pendingFinish: Boolean) extends KAction

    case object Finish extends KAction

}

class SimpleGenGen(val grammar: NGrammar) {
    private val analyzer = new GrammarAnalyzer(grammar)

    def consumableTerms(kernels: Set[AKernel]): (Set[AKernel], Set[CharacterTermGroupDesc]) = {
        val zeroReachableNodes = kernels flatMap { kernel => analyzer.zeroReachablesFrom(kernel).nodes }
        val zeroReachableTermNodes = zeroReachableNodes filter { node =>
            grammar.symbolOf(node.symbolId).isInstanceOf[NTerminal]
        }
        val zeroReachableTerms = zeroReachableTermNodes map { node =>
            grammar.symbolOf(node.symbolId).asInstanceOf[NTerminal].symbol
        }
        val termGroups = TermGrouper.termGroupsOf(zeroReachableTerms) map {
            _.asInstanceOf[CharacterTermGroupDesc]
        }
        (zeroReachableTermNodes, termGroups)
    }

    // TODO 어떤 노드가 progress되었을 때 그로 인해 progress되는(여파가 미치는) akernel들
    // startKernels --> endKernels 사이의 subgraph에서, endKernels가 모두 하나씩 progress되었을 떄,
    // - endKernels가 모두 progress되면 따라서 progress될 startKernels의 subset
    //   - 그리고 걔들이 progress된 것들(+거기에 nullable 떄문에 progress된 것들 포함)
    // - subgraph에서 progress될 kernel들의 집합(startKernels exclusive, endKernels inclusive)
    def influences(startKernels: Set[AKernel], endKernels: Set[AKernel]): (Set[AKernel], Set[AKernel]) = {
        def recursion(queue: List[AKernel], visited: Set[AKernel], ccAffected: Set[AKernel], ccAppended: Set[AKernel]): (Set[AKernel], Set[AKernel]) =
            queue match {
                case head +: rest =>
                    assert(visited contains head)
                    if (startKernels contains head) {
                        recursion(rest, visited, ccAffected + head, ccAppended)
                    } else {
                        val symbol = grammar.symbolOf(head.symbolId)
                        symbol match {
                            case seq: NSequence =>
                                if (head.pointer + 1 < seq.sequence.length) {
                                    // progress 해도 안 끝나는 경우
                                    val progressed = AKernel(head.symbolId, head.pointer + 1)
                                    if (analyzer.isNullable(seq.sequence(progressed.pointer))) {
                                        // 점 뒤가 nullable이면 더 진행
                                        if (visited contains progressed) {
                                            assert(ccAppended contains progressed)
                                            recursion(rest, visited, ccAffected, ccAppended)
                                        } else {
                                            recursion(progressed +: rest, visited + progressed, ccAffected, ccAppended + progressed)
                                        }
                                    } else {
                                        // nullable이 아니면 ccAffected에만 추가하고 종료
                                        recursion(rest, visited + progressed, ccAffected, ccAppended + progressed)
                                    }
                                } else {
                                    // progress하면 끝나는 경우
                                    val next = (analyzer.deriveRelations.edgesByEnd(head) map { e => e.start }) -- visited
                                    recursion(rest ++ next, visited ++ next, ccAffected, ccAppended)
                                }
                            case _: NAtomicSymbol =>
                                val next = (analyzer.deriveRelations.edgesByEnd(head) map { e => e.start }) -- visited
                                recursion(rest ++ next, visited ++ next, ccAffected, ccAppended)
                        }
                    }
                case _ =>
                    (ccAffected, ccAppended)
            }

        recursion(endKernels.toList, endKernels, Set(), Set())
    }

    // kernels로 이루어진 노드와 하위 노드들에서 받을 수 있는 term group -> 이 때 취할 액션
    def termActions(startKernels: Set[AKernel]): Map[CharacterTermGroupDesc, KAction] = {
        val (termNodes, termGroups) = consumableTerms(startKernels)
        (termGroups map { termGroup =>
            val acceptNodes = termNodes filter { termNode =>
                grammar.symbolOf(termNode.symbolId).symbol.asInstanceOf[Terminal].accept(termGroup)
            }

            val (affected, appended) = influences(startKernels, acceptNodes)

            // kernels에서 acceptNodes로 zero reachable한 것들만 추림
            //   -> 만약 추려서 모든 kernel이 acceptNodes로 zero reachable하지 않으면 in-replace 필요
            // 추려진 애들에 대해서 implied edge할 때랑 비슷하게 progress, pendingFinish 계산해서
            //   -> implied node가 있고 start kernel들이 모두 progress되면 -> Append/ReplaceAppend(implied node, true)
            //   -> implied node가 있고 start kernel까지 오지 않으면 -> Append/ReplaceAppend(implied node, false)
            //   -> implied node 없이 start kernel까지 오면 -> Finish/ReplaceFinish + 가능한 edge 추가
            // => 기존의 replace는 finish/ReplaceFinish + 가능한 edge 조합을 추가하고 implied edge 추가해서 해결
            // TODO 추가로 zero reachable인 것들을 계산해서 뭔가 해야할 듯 한데.. affected
            val kaction: KAction = if (affected == startKernels) {
                if (appended.isEmpty) {
                    // TODO affected의 pointer +1로 replace해야하나?
                    SimpleGenGen.Finish
                } else {
                    SimpleGenGen.Append(appended, pendingFinish = true)
                }
            } else {
                SimpleGenGen.InReplaceAndAppend(affected, appended, pendingFinish = affected.nonEmpty)
            }

            println(s"$startKernels  $termGroup  $acceptNodes")
            println(s"  -> $affected")
            println(s"  -> $appended")
            println(s"  -> $kaction")
            println()

            termGroup -> kaction
        }).toMap
    }

    // end의 kernel들이 모두 progress되었을 때,
    //   - start -> end 사이에서 progress되어 start 밑에 더 붙게 될 kernel들이 있으면 _1로 반환
    //   - start도 progress되게 되면 _2로 true 반환
    //   - start -> end 사이에 더 붙게 될 kernel이 없으면 반드시 start가 progress되고, 그런 경우 None 반환
    def impliedNodes(start: Set[AKernel], end: Set[AKernel]): Option[(Set[AKernel], Set[AKernel], Boolean)] = ???

    private class Generating {
        private def termActions1(nodeId: Int): Map[CharacterTermGroupDesc, Action] = {
            val ta = termActions(kernelsOf(nodeId))
            ta map { kv =>
                val action = kv._2 match {
                    case SimpleGenGen.Append(appendKernels, pendingFinish) =>
                        SimpleGen.Append(nodeIdOf(appendKernels), pendingFinish)
                    case SimpleGenGen.Replace(replaceKernels) =>
                        SimpleGen.Replace(nodeIdOf(replaceKernels))
                    case SimpleGenGen.InReplaceAndAppend(inreplaceKernels, appendKernels, pendingFinish) =>
                        SimpleGen.ReplaceAndAppend(nodeIdOf(inreplaceKernels), nodeIdOf(appendKernels), pendingFinish)
                    case SimpleGenGen.Finish =>
                        SimpleGen.Finish
                }
                kv._1 -> action
            }
        }

        private def impliedNodes1(startNodeId: Int, endNodeId: Int): Option[(Int, Int, Boolean)] = {
            impliedNodes(kernelsOf(startNodeId), kernelsOf(endNodeId)) map { implied =>
                (nodeIdOf(implied._1), nodeIdOf(implied._2), implied._3)
            }
        }

        private var kernelsToNodes = Map[Set[AKernel], Int]()
        private var nodesToKernels = Map[Int, Set[AKernel]]()
        private var newNodes = Seq[Int]()

        private def kernelsOf(nodeId: Int): Set[AKernel] = nodesToKernels(nodeId)

        private def nodeIdOf(kernels: Set[AKernel]): Int = kernelsToNodes get kernels match {
            case Some(exist) => exist
            case None =>
                val newId = nodesToKernels.size + 1
                nodesToKernels += newId -> kernels
                kernelsToNodes += kernels -> newId
                newId
        }

        def generate(): SimpleGen = {
            val startNodeId = nodeIdOf(Set(AKernel(grammar.startSymbol, 0)))

            var termActions = Map[(Int, CharacterTermGroupDesc), Action]()
            var impliedNodes = Map[(Int, Int), Option[(Int, Int, Boolean)]]()

            while (newNodes.isEmpty) {
                val nextNode = newNodes.head
                newNodes = newNodes.tail

                termActions ++= (termActions1(nextNode) map { kv => (nextNode, kv._1) -> kv._2 })
            }

            new SimpleGen(grammar, nodesToKernels, startNodeId, termActions, impliedNodes)
        }
    }

    def generateGenerator(): SimpleGen = {
        val gen = new Generating()
        gen.generate()
    }
}

object SimpleGenGenMain {
    def main(args: Array[String]): Unit = {
        val grammar = NGrammar.fromGrammar(ExpressionGrammars.simple)
        val gengen = new SimpleGenGen(grammar)
        gengen.termActions(Set(AKernel(grammar.startSymbol, 0)))
    }
}
