package com.giyeok.jparser.parsergen

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.examples.{JsonGrammar, SimpleGrammars}
import com.giyeok.jparser.nparser.NGrammar

class UnambiguousGen(val grammar: NGrammar) {
    val simpleGenGen: SimpleGenGen = new SimpleGenGen(grammar)
    val generating: simpleGenGen.Generating = new simpleGenGen.Generating()
    val simpleGen: SimpleGen = generating.generate()

    val nodeActions: Map[Int, Map[CharacterTermGroupDesc, SimpleGen.Action]] =
        simpleGen.termActions.groupBy(_._1._1) map { kv =>
            kv._1 -> (kv._2.toSeq map { k => k._1._2 -> k._2 }).toMap
        }

    def handledByNode(nodeId: Int): CharacterTermGroupDesc =
        nodeActions.getOrElse(nodeId, Map()).keySet.foldLeft(CharacterTermGroupDesc.empty) { (m, i) => i + m }

    case class AmbiguousTermAction(finReachables: Set[Int], handledByAppended: CharacterTermGroupDesc, ambiguousActions: Seq[(Int, CharacterTermGroupDesc)])

    def analyzeAmbiguousTermActions(): Map[(Int, CharacterTermGroupDesc), AmbiguousTermAction] = {
        val finishableEdgesByEnd = simpleGen.finishableEdges groupBy (_._2)

        def termOverlappedNodes(origin: Int, appended: Int): AmbiguousTermAction = {
            // origin 노드와, origin 노드로 들어오는 incoming finishable node들부터 시작해서 pendingFinish=false일 때까지 finish backtrack 해서 나올 수 있는 노드들 중에
            // 처리할 수 있는 term group이 겹치는 경우가 있으면 출력

            var visited = Set[(Int, Int)]()

            def simulateFinish(node: Int, cc: Set[Int]): Set[Int] = {
                finishableEdgesByEnd.getOrElse(node, Set()) flatMap { e =>
                    if (!(visited contains (e._1 -> e._2))) {
                        visited += (e._1 -> e._2)
                        simpleGen.impliedNodes(e) match {
                            case Some((implStart, implEnd, pendingFinish)) =>
                                if (pendingFinish) simulateFinish(implStart, cc + implEnd)
                                else cc + implEnd
                            case None =>
                                simulateFinish(e._1, cc)
                        }
                    } else cc
                }
            }

            val finReachables = simulateFinish(origin, Set())

            val handledByAppended = handledByNode(appended)
            val ambiguousActions = finReachables flatMap { fin =>
                val intersect = handledByAppended intersect handledByNode(fin)
                if (intersect.isEmpty) None else Some(fin -> intersect)
            }
            AmbiguousTermAction(finReachables, handledByAppended, ambiguousActions.toSeq)
        }

        (simpleGen.termActions flatMap { p =>
            val ((node, termGroup), action) = p
            action match {
                case SimpleGen.ReplaceAndAppend(replaceNodeType, appendNodeType, true) =>
                    Some((node, termGroup) -> termOverlappedNodes(replaceNodeType, appendNodeType))
                case SimpleGen.Append(appendNodeType, true) =>
                    Some((node, termGroup) -> termOverlappedNodes(node, appendNodeType))
                case _ => None // nothing
            }
        }).toMap
    }
}

object UnambiguousGen {
    def printUnambiguousGen(ugen: UnambiguousGen): Unit = {
        ugen.simpleGen.nodes.toList.sortBy(_._1) foreach { nk =>
            println(s"${nk._1} ${
                nk._2 map {
                    _.toReadableString(ugen.grammar)
                } mkString "|"
            }")
        }
        ugen.nodeActions.toSeq.sortBy(_._1) foreach { kv =>
            println(s"Node ${kv._1}:")
            kv._2.toSeq.sortBy(_._1.toShortString) foreach { acts =>
                println(s"  ${acts._1.toShortString} -> ${acts._2}")
            }
        }
        println("Implied:")
        ugen.simpleGen.impliedNodes.toSeq.sortBy(_._1) foreach { kv =>
            println(s"  ${kv._1} -> ${kv._2}")
        }
    }

    implicit class ToReadableString(kernels: Set[AKernel]) {
        def string(grammar: NGrammar): String =
            kernels.toSeq.sortBy(k => (k.symbolId, k.pointer)).map(_.toReadableString(grammar)).mkString("|")
    }

    def printAmbiguousTermActions(ugen: UnambiguousGen, ambiguousActions: Map[(Int, CharacterTermGroupDesc), UnambiguousGen#AmbiguousTermAction]): Unit = {
        def nodeString(node: Int): String = ugen.simpleGen.nodes(node).string(ugen.grammar)

        ambiguousActions filter { p => p._2.ambiguousActions.nonEmpty } foreach { p =>
            val ((node, termGroup), ambiguousAction) = p
            val originalAction = ugen.simpleGen.termActions((node, termGroup))
            originalAction match {
                case SimpleGen.ReplaceAndAppend(replaceNodeType, appendNodeType, true) =>
                    println(s"Consider $node ${nodeString(node)} -> ${termGroup.toShortString}, repl&app($replaceNodeType ${nodeString(replaceNodeType)}, $appendNodeType ${nodeString(appendNodeType)})")
                case SimpleGen.Append(appendNodeType, true) =>
                    println(s"Consider $node ${nodeString(node)} -> ${termGroup.toShortString}, app($appendNodeType ${nodeString(appendNodeType)})")
            }
            println(ambiguousAction.finReachables)
            ambiguousAction.ambiguousActions foreach { p =>
                println(s"    ${p._1} ${nodeString(p._1)} -> ${p._2.toShortString}")
            }
        }
    }

    def main(args: Array[String]): Unit = {
        val grammar = NGrammar.fromGrammar(JsonGrammar.fromJsonOrg)
        val ugen = new UnambiguousGen(grammar)
        printUnambiguousGen(ugen)
        printAmbiguousTermActions(ugen, ugen.analyzeAmbiguousTermActions())
    }
}
