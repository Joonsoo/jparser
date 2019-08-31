package com.giyeok.jparser.study.parsergen

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.examples.metalang.SimpleGrammars
import com.giyeok.jparser.metalang.MetaGrammar
import com.giyeok.jparser.parsergen.deprecated.{SimpleGen, SimpleGenGen}
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}
import com.giyeok.jparser.visualize.FigureGenerator.Spacing
import com.giyeok.jparser.visualize.{AbstractZestGraphWidget, BasicVisualizeResources, Interactable, NodeFigureGenerators}
import org.eclipse.draw2d.{ColorConstants, Figure, LineBorder}
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{KeyEvent, KeyListener}
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.{Composite, Display, Shell}
import org.eclipse.zest.core.viewers.GraphViewer
import org.eclipse.zest.core.widgets.{Graph, GraphConnection, ZestStyles}

class TopologyViewer(val simpleGen: SimpleGen) {
    private val grammar = simpleGen.grammar

    case class TopologyEdge(start: Int, end: Int, finishable: Boolean) extends AbstractEdge[Int]

    class TopologyGraph(val nodes: Set[Int],
                        val edges: Set[TopologyEdge],
                        val edgesByStart: Map[Int, Set[TopologyEdge]],
                        val edgesByEnd: Map[Int, Set[TopologyEdge]])
        extends AbstractGraph[Int, TopologyEdge, TopologyGraph] {
        override def createGraph(nodes: Set[Int], edges: Set[TopologyEdge], edgesByStart: Map[Int, Set[TopologyEdge]], edgesByEnd: Map[Int, Set[TopologyEdge]]): TopologyGraph =
            new TopologyGraph(nodes, edges, edgesByStart, edgesByEnd)
    }

    class SimpleGenGraphViewerWidget(parent: Composite, style: Int, val fig: NodeFigureGenerators[Figure])
        extends Composite(parent, style)
            with AbstractZestGraphWidget[Int, TopologyEdge, TopologyGraph]
            with Interactable[Int, TopologyEdge, TopologyGraph] {
        override val graphViewer: GraphViewer = new GraphViewer(this, style)
        override val graphCtrl: Graph = graphViewer.getGraphControl

        setLayout(new FillLayout())

        override def createFigure(node: Int): Figure = {
            val nodeFig = fig.fig.verticalFig(Spacing.Medium, Seq(
                fig.fig.textFig(s"$node", fig.appear.default)
            ) ++ (simpleGen.nodes(node) map { k => fig.symbol.symbolPointerFig(grammar, k.symbolId, k.pointer) }))
            nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
            nodeFig.setOpaque(true)
            nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
            nodeFig.setSize(nodeFig.getPreferredSize())
            nodeFig
        }

        override def createConnection(edge: TopologyEdge): GraphConnection = {
            val conn = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(edge.start), nodesMap(edge.end))
            if (edge.finishable) {
                conn.setText("finishable")
            }
            val count = edgesMap.keySet count { e => Set(e.start, e.end) == Set(edge.start, edge.end) }
            conn.setCurveDepth(count * 20)
            conn.setData(edge)
            conn
        }
    }


    def start(): Unit = {
        val display = new Display()
        val shell = new Shell(display)

        val graphViewer = new SimpleGenGraphViewerWidget(shell, SWT.NONE, BasicVisualizeResources.nodeFigureGenerators)

        var graph = new TopologyGraph(Set(), Set(), Map(), Map())
        simpleGen.nodes.keys foreach { n => graph = graph.addNode(n) }
        simpleGen.existables.edges foreach { e => graph = graph.addEdgeSafe(TopologyEdge(e.start, e.end, finishable = false)) }
        simpleGen.finishableEdges foreach { e => graph = graph.addEdgeSafe(TopologyEdge(e._1, e._2, finishable = true)) }

        graphViewer.addGraph(graph)
        graphViewer.applyLayout(true)

        graphViewer.addKeyListener(new KeyListener {
            override def keyPressed(e: KeyEvent): Unit = {
                e.keyCode match {
                    case 'R' | 'r' =>
                        graphViewer.applyLayout(true)
                    case _ => // do nothing
                }
            }

            override def keyReleased(e: KeyEvent): Unit = {}
        })

        shell.setLayout(new FillLayout)
        shell.open()
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }
}

object TopologyViewer {
    def main(args: Array[String]): Unit = {
        val grammar = NGrammar.fromGrammar(SimpleGrammars.arrayGrammar.toGrammar(MetaGrammar.translateForce))

        new TopologyViewer(new SimpleGenGen(grammar).generateGenerator()).start()
    }
}
