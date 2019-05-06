package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.examples.SimpleGrammars
import com.giyeok.jparser.parsergen.nocond.codegen.SimpleParserJavaGen

// path는 맨 뒤가 stack top
case class NodePath(nodes: List[Int]) {
    def top = nodes.last

    def prepend(node: Int) = NodePath(node +: nodes)

    def append(node: Int) = NodePath(nodes :+ node)

    def dropLast() = NodePath(nodes.init)

    def replaceLast(replace: Int) = NodePath(nodes.init :+ replace)
}

case class FinishablePaths(paths: Seq[NodePath]) {
    def prependNode(node: Int) = FinishablePaths(paths map (_.prepend(node)))

    def addPath(newPath: NodePath) = FinishablePaths(paths :+ newPath)

    // paths 중에 맨 앞에 공통적으로 있는 node들 추려서 _1로, 남는 부분은 _2로 반환
    def maxCommonPaths: (NodePath, FinishablePaths) = {
        ???
    }
}

class SimpleParserFinishSimulator(val parser: SimpleParser) {
    private def traverse(head: Int, replace: Int, paths: FinishablePaths, cc: Seq[FinishablePaths]): Seq[FinishablePaths] = {
        val prevs = parser.nodeRelInferer.prevOf(head).toSeq.sorted
        prevs flatMap { prev =>
            parser.edgeActions(prev -> replace) match {
                case SimpleParser.DropLast(replacePrev) =>
                    traverse(prev, replacePrev, paths.prependNode(prev), cc)
                case SimpleParser.ReplaceEdge(replacePrev, replaceLast, pendingFinish) =>
                    val newPaths = paths.prependNode(prev).addPath(NodePath(List(replacePrev, replaceLast)))
                    val newCC = cc :+ newPaths
                    pendingFinish match {
                        case Some(pF) =>
                            traverse(replacePrev, pF, newPaths, newCC)
                        case None => newCC
                    }
            }
        }
    }

    def simulatePendingFinish(prev: Int, last: Int, pendingFinish: Int): Seq[FinishablePaths] = {
        val settledPath = NodePath(List(prev, last))
        val initialFinishablePaths = FinishablePaths(Seq(settledPath))
        traverse(prev, pendingFinish, initialFinishablePaths, Seq())
    }
}

object SimpleParserFinishSimulator {
    private def simulatePendingFin(sim: SimpleParserFinishSimulator, prev: Int, last: Int, pendingFinish: Int): Unit = {
        val simulation = sim.simulatePendingFinish(prev, last, pendingFinish)
        println(s"$prev $last $pendingFinish: ")
        simulation foreach { paths =>
            val pathStrings = paths.paths map { nodePath =>
                s"(${nodePath.nodes mkString " -> "})"
            }
            val acceptableTerms = paths.paths map { nodePath =>
                CharacterTermGroupDesc.merge(sim.parser.acceptableTermsOf(nodePath.top))
            }
            println(pathStrings mkString " ")
            println(acceptableTerms map (_.toShortString) mkString " ")
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
