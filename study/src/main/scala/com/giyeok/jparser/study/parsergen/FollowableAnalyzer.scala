package com.giyeok.jparser.study.parsergen

import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.nparser.NGrammar._
import com.giyeok.jparser.nparser.{NGrammar, ParsingTasks}
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}
import com.giyeok.jparser.visualize.{AbstractZestGraphWidget, BasicVisualizeResources, NodeFigureGenerators}
import org.eclipse.draw2d.{ColorConstants, Figure, LineBorder}
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.{Composite, Display, Shell}
import org.eclipse.zest.core.viewers.GraphViewer
import org.eclipse.zest.core.widgets.{Graph, GraphConnection, ZestStyles}

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

case class AKernelEdge(start: AKernel, end: AKernel) extends AbstractEdge[AKernel]

case class AKernelGraph(nodes: Set[AKernel], edges: Set[AKernelEdge], edgesByStart: Map[AKernel, Set[AKernelEdge]], edgesByEnd: Map[AKernel, Set[AKernelEdge]])
    extends AbstractGraph[AKernel, AKernelEdge, AKernelGraph] {

    def createGraph(nodes: Set[AKernel], edges: Set[AKernelEdge], edgesByStart: Map[AKernel, Set[AKernelEdge]], edgesByEnd: Map[AKernel, Set[AKernelEdge]]): AKernelGraph =
        AKernelGraph(nodes, edges, edgesByStart, edgesByEnd)

    def hasEdge(start: AKernel, end: AKernel): Boolean = edges contains AKernelEdge(start, end)

    // node로부터 시작해서 도달 가능한 노드/엣지로 이루어진 sub graph
    def reachablesFrom(node: AKernel): AKernelGraph = ???

    // start에서 시작해서 end로 도착 가능한 모든 path에 속한 노드/엣지로 이루어진 sub graph
    def pathsBetween(start: AKernel, end: AKernel): AKernelGraph = ???
}

object AKernelGraph {
    val empty = AKernelGraph(Set(), Set(), Map(), Map())
}

class FollowableAnalyzer(val grammar: NGrammar) extends ParsingTasks {
    def allAKernels(symbolId: Int): Set[AKernel] =
        grammar.symbolOf(symbolId) match {
            case _: NAtomicSymbol =>
                Set(AKernel(symbolId, 0))
            case NSequence(_, sequence) =>
                ((0 until sequence.length) map { index => AKernel(symbolId, index) }).toSet
        }

    lazy val deriveRelations: AKernelGraph = {
        def recursion(queue: List[AKernel], cc: AKernelGraph): AKernelGraph =
            queue match {
                case (kernel@AKernel(symbolId, pointer)) +: rest =>
                    // kernel은 derivable한 상태여야 함(atomic symbol이면 pointer==0, sequence이면 pointer<len(sequence))
                    def addings(from: AKernel, toSymbolId: Int): Set[(AKernel, AKernel)] =
                        allAKernels(toSymbolId) map { end => from -> end }

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
                        if (!cc1.hasEdge(deriver, derivable)) {
                            newKernels += derivable
                            cc1.addNode(deriver).addNode(derivable).addEdge(AKernelEdge(deriver, derivable))
                        } else cc1
                    }
                    recursion((newKernels ++ rest).toList, nextcc)
                case List() => cc
            }

        val startKernel = AKernel(grammar.startSymbol, 0)
        recursion(List(startKernel), AKernelGraph.empty.addNode(startKernel))
    }

    // deriveRelations를 그래프라고 봤을 때,
    //  - deriversOf(S)는 AKernel(<start>, 0) --> AKernel(S, 0) 의 경로 사이에 있는 모든 노드들의 집합을 반환
    //  - derivablesOf(k)는 k에서 시작해서 도달 가능한 모든 노드들의 집합을 반환
    //  - surroundingsOf(S)는 deriveRelations에서 (k -> AKernel(S, 0))인 엣지를 뺸 그래프에서 AKernel(<start>, 0)에서 도달 가능한 모든 노드들의 집합을 반환

    val startKernel = AKernel(grammar.startSymbol, 0)

    def deriversOf(symbolId: Int): AKernelGraph = {
        deriveRelations.pathsBetween(startKernel, AKernel(symbolId, 0))
    }

    def derivablesOf(kernel: AKernel): AKernelGraph =
        deriveRelations.reachablesFrom(kernel)

    def surroundingsOf(symbolId: Int): AKernelGraph = {
        // symbolId와 관련된 모든 kernel을 지우고 start에서 도달 가능한 subgraph
        deriveRelations.removeNodes(allAKernels(symbolId)).reachablesFrom(startKernel)
    }
}

object FollowableAnalyzer {

    class AKernelGraphViewer(parent: Composite, style: Int, val fig: NodeFigureGenerators[Figure], val grammar: NGrammar)
        extends Composite(parent, style)
            with AbstractZestGraphWidget[AKernel, AKernelEdge, AKernelGraph] {
        val graphViewer = new GraphViewer(this, style)
        val graphCtrl: Graph = graphViewer.getGraphControl

        setLayout(new FillLayout())

        override def createFigure(node: AKernel): Figure = {
            val nodeFig = fig.symbol.symbolPointerFig(grammar, node.symbolId, node.pointer)
            nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
            nodeFig.setOpaque(true)
            nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
            nodeFig.setSize(nodeFig.getPreferredSize())
            nodeFig
        }

        override def createConnection(edge: AKernelEdge): GraphConnection = {
            val conn = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(edge.start), nodesMap(edge.end))
            conn.setData((Seq(edge.start, edge.end), Seq(conn)))
            conn
        }
    }

    def main(args: Array[String]): Unit = {
        val expressionGrammar0Text: String =
            """expression = term | expression '+' term
              |term = factor | term '*' factor
              |factor = number | variable | '(' expression ')'
              |number = '0' | {1-9} {0-9}*
              |variable = {A-Za-z}+""".stripMargin('|')

        val rawGrammar = MetaGrammar.translate("Expression Grammar 0", expressionGrammar0Text).left.get
        val grammar: NGrammar = NGrammar.fromGrammar(rawGrammar)

        val analyzer = new FollowableAnalyzer(grammar)
        analyzer.deriveRelations.edgesByStart foreach { relation =>
            println(relation._1.toReadableString(grammar))
            relation._2 foreach { derivable =>
                println(s"    ${derivable.end.toReadableString(grammar)}")
            }
        }

        val display = new Display()
        val shell = new Shell(display)

        val graphViewer = new AKernelGraphViewer(shell, SWT.NONE, BasicVisualizeResources.nodeFigureGenerators, grammar)

        shell.setLayout(new FillLayout)

        graphViewer.addGraph(analyzer.deriveRelations)
        graphViewer.applyLayout(true)

        shell.open()
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }
}
