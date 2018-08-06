package com.giyeok.jparser.study.parsergen

import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.nparser.NGrammar._
import com.giyeok.jparser.nparser.{NGrammar, ParsingTasks}

// Abstract Kernel? Aggregated Kernel?
case class AKernel(symbolId: Int, pointer: Int) {
    def toReadableString(grammar: NGrammar): String = {
        val symbols = grammar.symbolOf(symbolId) match {
            case atomicSymbol: NAtomicSymbol => Seq(atomicSymbol.symbol.toShortString)
            case NGrammar.NSequence(_, sequence) => sequence map { elemId =>
                grammar.symbolOf(elemId).symbol.toShortString
            }
        }
        (symbols.take(pointer) mkString " ") + "*" + (symbols.drop(pointer) mkString " ")
    }
}

case class AKernelEdge(start: AKernel, end: AKernel)

case class AKernelGraph(nodes: Set[AKernel], edges: Set[AKernelEdge], edgesByStart: Map[AKernel, Set[AKernelEdge]], edgesByEnd: Map[AKernel, Set[AKernelEdge]]) {
    def hasEdge(start: AKernel, end: AKernel): Boolean = ???

    def addNode(node: AKernel): AKernelGraph = ???

    def addEdge(start: AKernel, end: AKernel): AKernelGraph = ???
}

class FollowableAnalyzer(val grammar: NGrammar) extends ParsingTasks {
    lazy val deriveRelations: Map[AKernel, Set[AKernel]] = {
        def recursion(queue: List[AKernel], cc: Map[AKernel, Set[AKernel]]): Map[AKernel, Set[AKernel]] =
            queue match {
                case (kernel@AKernel(symbolId, pointer)) +: rest =>
                    // kernel은 derivable한 상태여야 함(atomic symbol이면 pointer==0, sequence이면 pointer<len(sequence))
                    def addings(from: AKernel, toSymbolId: Int): Set[(AKernel, AKernel)] =
                        grammar.symbolOf(toSymbolId) match {
                            case _: NAtomicSymbol =>
                                Set(from -> AKernel(toSymbolId, 0))
                            case NSequence(_, sequence) =>
                                ((0 until sequence.length) map { index => from -> AKernel(toSymbolId, index) }).toSet
                        }

                    val toAdds: Set[(AKernel, AKernel)] = grammar.symbolOf(symbolId) match {
                        case NTerminal(_) =>
                            Set()
                        case NSequence(_, sequence) =>
                            // add (kernel -> Kernel(sequence(pointer), 0)) to cc
                            addings(kernel, sequence(pointer))
                        case simpleDerive: NSimpleDerive =>
                            simpleDerive.produces flatMap {
                                addings(kernel, _)
                            }
                        case NExcept(_, body, except) =>
                            addings(kernel, except) ++ addings(kernel, body)
                        case NJoin(_, body, join) =>
                            addings(kernel, body) ++ addings(kernel, join)
                        case NLongest(_, body) =>
                            addings(kernel, body)
                        case lookahead: NLookaheadSymbol =>
                            addings(kernel, lookahead.lookahead) ++ addings(kernel, lookahead.emptySeqId)
                    }

                    var newKernels = Set[AKernel]()
                    val nextcc = toAdds.foldLeft(cc) { (cc1, adding) =>
                        val (deriver, derivable) = adding
                        val oldSet = cc1.getOrElse(deriver, Set())
                        if (!(oldSet contains derivable)) {
                            newKernels += derivable
                        }
                        cc1 + (deriver -> (oldSet + derivable))
                    }
                    recursion((newKernels ++ rest).toList, nextcc)
                case List() => cc
            }

        val startKernel = AKernel(grammar.startSymbol, 0)
        recursion(List(startKernel), Map(startKernel -> Set()))
    }

    // deriveRelations를 그래프라고 봤을 때,
    //  - deriversOf(S)는 AKernel(<start>, 0) --> AKernel(S, 0) 의 경로 사이에 있는 모든 노드들의 집합을 반환
    //  - derivablesOf(k)는 k에서 시작해서 도달 가능한 모든 노드들의 집합을 반환
    //  - surroundingsOf(S)는 deriveRelations에서 (k -> AKernel(S, 0))인 엣지를 뺸 그래프에서 AKernel(<start>, 0)에서 도달 가능한 모든 노드들의 집합을 반환

    def deriversOf(symbolId: Int): Set[AKernel] = {
        ???
    }

    def derivablesOf(kernel: AKernel): Set[AKernel] = ???

    def surroundingsOf(symbolId: Int): Set[AKernel] = ???
}

object FollowableAnalyzer {
    def main(args: Array[String]): Unit = {
        val expressionGrammar0Text: String =
            """expression = term | expression '+' term
              |term = factor | term '*' factor
              |factor = number | variable | '(' expression ')'
              |number = '0' | {1-9} {0-9}*
              |variable = {A-Za-z}+""".stripMargin('|')

        val rawGrammar = MetaGrammar.translate("Expression Grammar 0", expressionGrammar0Text).left.get
        val grammar: NGrammar = NGrammar.fromGrammar(rawGrammar)

        new FollowableAnalyzer(grammar).deriveRelations foreach { relation =>
            println(relation._1.toReadableString(grammar))
            relation._2 foreach { derivable =>
                println(s"    ${derivable.toReadableString(grammar)}")
            }
        }
    }
}
