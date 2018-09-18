package com.giyeok.jparser.parsergen

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.Symbols.Terminal
import com.giyeok.jparser.examples.{ExpressionGrammars, SimpleGrammars}
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar.{NAtomicSymbol, NSequence, NTerminal}
import com.giyeok.jparser.parsergen.SimpleGen.Action
import com.giyeok.jparser.parsergen.SimpleGenGen.KAction

object SimpleGenGen {

    sealed trait KAction

    case class Append(appendKernels: Set[AKernel], pendingFinish: Boolean) extends KAction

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

    // 1. startKernels 중에서 endKernels에 속한 모든 kernel로 도달 가능한 것들 - reachableKernels
    // 2. appendKernels
    // 3. endKernels가 모두 progress되었을 때 feasibleKernels 이 progress되는지 여부 boolean
    def influences(startKernels: Set[AKernel], endKernels: Set[AKernel]): (Set[AKernel], Set[AKernel], Boolean) = {
        val endKernels0 = endKernels map { k => AKernel(k.symbolId, 0) }
        val reachables = startKernels filter { startKernel =>
            endKernels0 subsetOf analyzer.zeroReachablesFrom(startKernel).nodes
        }
        //        val reachables = startKernels filter { startKernel =>
        //            // TODO forall? exists? forall인듯 하긴 한데
        //            endKernels exists { endKernel =>
        //                analyzer.deriveRelations.reachableBetween(startKernel, endKernel)
        //            }
        //        }

        val zeroReachables = reachables.foldLeft(AKernelGraph.empty) { (m, i) => m merge analyzer.zeroReachablesFrom(i) }

        // TODO (*E ) -> (*E ) 가 오는 경우, end를 우선 progress한 다음에 찾기 시작해야하는데..
        def recursion(queue: List[(AKernel, Boolean)], visited: Set[AKernel], ccAffected: Set[AKernel], ccAppended: Set[AKernel]): (Set[AKernel], Set[AKernel]) =
            queue match {
                case (head, isInitial) +: rest =>
                    assert(visited contains head)
                    if (!isInitial && (startKernels contains head)) {
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
                                            // assert(ccAppended contains progressed)
                                            recursion(rest, visited, ccAffected, ccAppended)
                                        } else {
                                            // TODO progressed가 reachables에서 zero reachable할 때만. (모든 reachables로부터? 그중 하나로부터?)
                                            recursion((progressed, false) +: rest, visited + progressed, ccAffected, ccAppended + progressed)
                                        }
                                    } else {
                                        // nullable이 아니면 ccAffected에만 추가하고 종료
                                        // TODO progressed가 reachables에서 zero reachable할 때만. (모든 reachables로부터? 그중 하나로부터?)
                                        recursion(rest, visited + progressed, ccAffected, ccAppended + progressed)
                                    }
                                } else {
                                    // progress하면 끝나는 경우
                                    val next = (zeroReachables.edgesByEnd(AKernel(head.symbolId, 0)) map { e => e.start }) -- visited
                                    recursion(rest ++ (next map { n => (n, false) }), visited ++ next, ccAffected, ccAppended)
                                }
                            case _: NAtomicSymbol =>
                                val next = (zeroReachables.edgesByEnd(head) map { e => e.start }) -- visited
                                recursion(rest ++ (next map { n => (n, false) }), visited ++ next, ccAffected, ccAppended)
                        }
                    }
                case _ =>
                    (ccAffected, ccAppended)
            }

        val (affected, appended0) = recursion(endKernels.toList map { n => (n, true) }, endKernels, Set(), Set())
        val appended = appended0 // TODO startKernel들에서 zero reachable 해야함. 모든 start kernel에서? 그중 하나에서??
        // appended와 reachables의 관계?
        (reachables, appended, affected.nonEmpty)
    }

    // kernels로 이루어진 노드와 하위 노드들에서 받을 수 있는 term group -> 이 때 취할 액션
    def termActions(startKernels: Set[AKernel]): Map[CharacterTermGroupDesc, KAction] = {
        val (termNodes, termGroups) = consumableTerms(startKernels)
        (termGroups map { termGroup =>
            val acceptNodes = termNodes filter { termNode =>
                grammar.symbolOf(termNode.symbolId).symbol.asInstanceOf[Terminal].accept(termGroup)
            }

            val (reachable, appended, finishing) = influences(startKernels, acceptNodes)

            // kernels에서 acceptNodes로 zero reachable한 것들만 추림
            //   -> 만약 추려서 모든 kernel이 acceptNodes로 zero reachable하지 않으면 in-replace 필요
            // 추려진 애들에 대해서 implied edge할 때랑 비슷하게 progress, pendingFinish 계산해서
            //   -> implied node가 있고 start kernel들이 모두 progress되면 -> Append/ReplaceAppend(implied node, true)
            //   -> implied node가 있고 start kernel까지 오지 않으면 -> Append/ReplaceAppend(implied node, false)
            //   -> implied node 없이 start kernel까지 오면 -> Finish/ReplaceFinish + 가능한 edge 추가
            // => 기존의 replace는 finish/ReplaceFinish + 가능한 edge 조합을 추가하고 implied edge 추가해서 해결
            // TODO 추가로 zero reachable인 것들을 계산해서 뭔가 해야할 듯 한데.. affected
            val kaction: KAction = if (reachable == startKernels) {
                if (appended.isEmpty) {
                    // TODO affected의 pointer +1로 replace해야하나?
                    // finishing = true여야 하나?
                    SimpleGenGen.Finish
                } else {
                    SimpleGenGen.Append(appended, pendingFinish = finishing)
                }
            } else {
                if (appended.isEmpty) {
                    // TODO 그냥 replace로 치환
                    SimpleGenGen.InReplaceAndAppend(reachable, appended, pendingFinish = finishing)
                } else {
                    SimpleGenGen.InReplaceAndAppend(reachable, appended, pendingFinish = finishing)
                }
            }

            //            println(s"$startKernels  $termGroup  $acceptNodes")
            //            println(s"  -> $reachable")
            //            println(s"  -> $appended")
            //            println(s"  -> $finishing")
            //            println(s"  -> $kaction")
            //            println()

            termGroup -> kaction
        }).toMap
    }

    // end의 kernel들이 모두 progress되었을 때,
    //   - start -> end 사이에서 progress되어 start 밑에 더 붙게 될 kernel들이 있으면 _1로 반환
    //   - start도 progress되게 되면 _2로 true 반환
    //   - start -> end 사이에 더 붙게 될 kernel이 없으면 반드시 start가 progress되고, 그런 경우 None 반환
    def impliedNodes(start: Set[AKernel], end: Set[AKernel]): Option[(Set[AKernel], Set[AKernel], Boolean)] = {
        val (reachable, appended, finishing) = influences(start, end)
        if (reachable == start) {
            if (appended.isEmpty) {
                // assert finishing?
                None
            } else {
                Some(reachable, appended, finishing)
            }
        } else {
            Some(reachable, appended, finishing)
        }
    }

    private class Generating {
        private def termActions1(nodeId: Int): Map[CharacterTermGroupDesc, Action] = {
            val ta = termActions(kernelsOf(nodeId))
            ta map { kv =>
                val action = kv._2 match {
                    case SimpleGenGen.Append(appendKernels, pendingFinish) =>
                        SimpleGen.Append(nodeIdOf(appendKernels), pendingFinish)
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
                // println(s"New node $newId -> $kernels")
                newNodes +:= newId
                nodesToKernels += newId -> kernels
                kernelsToNodes += kernels -> newId
                newId
        }

        private var allPossibleEdges = Map[(Int, Int), Option[Boolean]]()

        def addEdge(start: Int, end: Int): Boolean = {
            if (!(allPossibleEdges contains ((start, end)))) {
                allPossibleEdges += ((start, end) -> None)
                true
            } else false
        }

        def generate(): SimpleGen = {
            val startNodeId = nodeIdOf(Set(AKernel(grammar.startSymbol, 0)))

            var termActions = Map[(Int, CharacterTermGroupDesc), Action]()
            var alwaysReplaced = Map[Int, Boolean]()
            var canBeReplaced = Map[Int, Set[Int]]()
            var impliedNodes = Map[(Int, Int), Option[(Int, Int, Boolean)]]()

            while (newNodes.nonEmpty) {
                val nextNode = newNodes.head
                newNodes = newNodes.tail

                val thisTermActions = termActions1(nextNode)
                termActions ++= (thisTermActions map { kv => (nextNode, kv._1) -> kv._2 })
                // TODO possibleEdges 계산해서 impliedNodes 계산

                val thisNodeAlwaysReplaced = thisTermActions.values forall {
                    case _: SimpleGen.Append | SimpleGen.Finish => false
                    case _ => true
                }
                alwaysReplaced += nextNode -> thisNodeAlwaysReplaced

                // return true if modified

                canBeReplaced += nextNode -> Set()
                thisTermActions.values foreach {
                    case SimpleGen.Append(appendNodeType, _) =>
                        addEdge(nextNode, appendNodeType)
                    case SimpleGen.ReplaceAndAppend(replaceNodeType, appendNodeType, _) =>
                        canBeReplaced += nextNode -> (canBeReplaced(nextNode) + replaceNodeType)
                        addEdge(replaceNodeType, appendNodeType)
                    case SimpleGen.Finish => // do nothing
                    case SimpleGen.ReplaceAndFinish(replaceNodeType) =>
                        canBeReplaced += nextNode -> (canBeReplaced(nextNode) + replaceNodeType)
                }

                // 방문했던 노드는 alwaysReplaced와 canBeReplaced에 값이 항상 들어감

                // TODO 불필요하게 allPossibleEdges 전부 돌면서 possible.isEmpty 하지 말고 처리해야할 노드들 따로 관리하기
                var modified = true
                while (modified) {
                    modified = false
                    allPossibleEdges foreach { kv =>
                        val ((start, end), possible) = kv
                        // 아직 처리되지 않은 엣지인데, 각 노드는 방문한 뒤이면
                        if ((alwaysReplaced contains start) && (alwaysReplaced contains end)) {
                            if (possible.isEmpty) {
                                modified = true
                                val isPossibleEdge = !(alwaysReplaced(start) || alwaysReplaced(end))
                                allPossibleEdges += (start, end) -> Some(isPossibleEdge)
                                if (isPossibleEdge) {
                                    val replEdge = impliedNodes1(start, end)
                                    impliedNodes += (start, end) -> replEdge
                                    replEdge foreach { repl =>
                                        assert(modified)
                                        addEdge(repl._1, repl._2)
                                    }
                                }
                            }
                            assert((canBeReplaced contains start) && (canBeReplaced contains end))
                            canBeReplaced(start) ++ (if (alwaysReplaced(start)) Set() else Set(start)) foreach { startRepl =>
                                canBeReplaced(end) ++ (if (alwaysReplaced(end)) Set() else Set(end)) foreach { endRepl =>
                                    modified ||= addEdge(startRepl, endRepl)
                                }
                            }
                        }
                    }
                }

                /*
                // TODO code refactoring
                impliedNodes.keySet foreach { edge =>
                    canBeReplaced.getOrElse(edge._2, Set()) foreach { end =>
                        val newPossibleEdge = edge._1 -> end
                        if (!(impliedNodes contains newPossibleEdge)) {
                            possibleEdgesCandidate += newPossibleEdge
                        }
                    }
                }

                // TODO SimpleArrayGrammar 기준으로:
                // newPossibleEdges에 (1, 2) 엣지가 있긴 하지만 2는 사실 항상 다른 노드로 replace될 노드기 때문에
                // 2가 alwaysReplaced인지 아닌지 알기 전까지는 (1, 2)에 대해서는 추가적인 처리를 하지 않아야 할 듯
                possibleEdgesCandidate ++= (impliedNodes.values.flatten map { t => t._1 -> t._2 }).toSet -- impliedNodes.keySet
                val (newEdges, stillCandidates) = possibleEdgesCandidate partition { edge =>
                    edge._1
                }
                while (newPossibleEdges.nonEmpty) {
                    newPossibleEdges foreach { newPossibleEdge =>
                        impliedNodes += newPossibleEdge -> impliedNodes1(newPossibleEdge._1, newPossibleEdge._2)
                    }
                    newPossibleEdges = (impliedNodes.values.flatten map { t => t._1 -> t._2 }).toSet -- impliedNodes.keySet

                    impliedNodes.keySet foreach { edge =>
                        canBeReplaced.getOrElse(edge._2, Set()) foreach { end =>
                            val newPossibleEdge = edge._1 -> end
                            if (!(impliedNodes contains newPossibleEdge)) {
                                newPossibleEdges += newPossibleEdge
                            }
                        }
                    }
                }
                */
            }
            assert(allPossibleEdges.values.forall(_.isDefined))
            assert(allPossibleEdges.filter(p => p._2.contains(true)).forall(p => impliedNodes contains p._1))

            //            nodesToKernels.toList.sortBy(_._1) foreach { nk =>
            //                println(s"${nk._1} ${
            //                    nk._2 map {
            //                        _.toReadableString(grammar)
            //                    } mkString "|"
            //                }")
            //            }
            //            println(impliedNodes.keySet.toList.sorted)
            //            impliedNodes.toList.sortBy(_._1) foreach { p =>
            //                println(s"${p._1} -> ${p._2}")
            //            }
            //            canBeReplaced.toList.sortBy(_._1) foreach { p =>
            //                println(s"${p._1} -> ${p._2}")
            //            }

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
        val grammar = NGrammar.fromGrammar(SimpleGrammars.arrayGrammar)
        val gengen = new SimpleGenGen(grammar)
        val gen = gengen.generateGenerator()
        gen.writeFormattedJavaTo("parsergen/src/main/java/com/giyeok/jparser/parsergen/generated/GeneratedArrayGrammarParser0.java",
            "com.giyeok.jparser.parsergen.generated", "GeneratedArrayGrammarParser0", Some("[ a ]"))
    }
}
