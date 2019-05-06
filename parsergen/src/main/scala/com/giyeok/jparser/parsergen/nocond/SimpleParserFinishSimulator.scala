package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.examples.SimpleGrammars
import com.giyeok.jparser.parsergen.nocond.codegen.SimpleParserJavaGen

// path는 맨 뒤가 stack top
case class NodePath(nodes: List[Int]) {
    def top: Int = nodes.last

    def prepend(node: Int) = NodePath(node +: nodes)

    def append(node: Int) = NodePath(nodes :+ node)

    def dropLast() = NodePath(nodes.init)

    def replaceLast(replace: Int) = NodePath(nodes.init :+ replace)
}

case class NodePathSet(paths: Seq[NodePath]) {
    def prependNode(node: Int) = NodePathSet(paths map (_.prepend(node)))

    def addPath(newPath: NodePath) = NodePathSet(paths :+ newPath)

    // paths 중에 맨 앞에 공통적으로 있는 node들 추려서 _1로, 남는 부분은 _2로 반환
    def maxCommonPaths: (NodePath, NodePathSet) = {
        ???
    }
}

class SimpleParserFinishSimulator(val parser: SimpleParser) {
    private def traverse(head: Int, replace: Int, pathSet: NodePathSet, cc: Seq[NodePathSet]): Seq[NodePathSet] = {
        val prevs = parser.nodeRelInferer.prevOf(head).toSeq.sorted
        prevs flatMap { prev =>
            parser.edgeActions(prev -> replace) match {
                case SimpleParser.DropLast(replacePrev) =>
                    traverse(prev, replacePrev, pathSet.prependNode(prev), cc)
                case SimpleParser.ReplaceEdge(replacePrev, replaceLast, pendingFinish) =>
                    val newPaths = pathSet.prependNode(prev).addPath(NodePath(List(replacePrev, replaceLast)))
                    val newCC = cc :+ newPaths
                    pendingFinish match {
                        case Some(pF) =>
                            traverse(replacePrev, pF, newPaths, newCC)
                        case None => newCC
                    }
            }
        }
    }

    def simulatePendingFinish(prev: Int, last: Int, pendingFinish: Int): Seq[NodePathSet] = {
        val settledPath = NodePath(List(prev, last))
        val initialFinishablePathSet = NodePathSet(Seq(settledPath))
        traverse(prev, pendingFinish, initialFinishablePathSet, Seq())
    }
}

object SimpleParserFinishSimulator {
    private def simulatePendingFin(sim: SimpleParserFinishSimulator, prev: Int, last: Int, pendingFinish: Int): Unit = {
        val pathSets = sim.simulatePendingFinish(prev, last, pendingFinish)
        println(s"$prev $last $pendingFinish:")
        pathSets foreach { pathSet =>
            val conflictingTerms = CharacterTermGroupDesc.merge(
                pathSet.paths.zipWithIndex flatMap { p1index =>
                    val (p1, index) = p1index
                    val p1Acc = sim.parser.acceptableTermsOf(p1.top)
                    val rest = pathSet.paths.drop(index + 1)
                    rest flatMap { p2 => sim.parser.acceptableTermsOf(p2.top) intersect p1Acc }
                })
            val pathStrings = pathSet.paths map { nodePath =>
                s"(${nodePath.nodes mkString " -> "})"
            }
            val acceptableTerms = pathSet.paths map { nodePath =>
                CharacterTermGroupDesc.merge(sim.parser.acceptableTermsOf(nodePath.top))
            }
            println(pathStrings mkString " ")
            println(acceptableTerms map (_.toShortString) mkString " ")
            println(s"Conflict: ${conflictingTerms.toShortString}")
            println()
        }
    }

    def main(args: Array[String]): Unit = {
        val parser = SimpleParserJavaGen.generateParser(SimpleGrammars.array0Grammar)
        val sim = new SimpleParserFinishSimulator(parser)

        simulatePendingFin(sim, 2, 3, 2)
        simulatePendingFin(sim, 3, 16, 3)
        simulatePendingFin(sim, 5, 6, 7)
        simulatePendingFin(sim, 7, 11, 7)
        simulatePendingFin(sim, 14, 11, 14)
    }
}
