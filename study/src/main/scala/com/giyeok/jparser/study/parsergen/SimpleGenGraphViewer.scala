package com.giyeok.jparser.study.parsergen

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.examples.SimpleGrammars
import com.giyeok.jparser.parsergen.deprecated.{AKernel, SimpleGen, SimpleGenGen}
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

class SimpleGenGraphViewer(val simpleGen: SimpleGen) {
    private val grammar = simpleGen.grammar

    case class SimpleGenNode(nodeTypeId: Int)(val kernels: Set[AKernel])

    case class SimpleGenEdge(start: SimpleGenNode, end: SimpleGenNode)(val description: String)
        extends AbstractEdge[SimpleGenNode]

    class SimpleGenGraph(val nodes: Set[SimpleGenNode], val edges: Set[SimpleGenEdge], val edgesByStart: Map[SimpleGenNode, Set[SimpleGenEdge]], val edgesByEnd: Map[SimpleGenNode, Set[SimpleGenEdge]]) extends AbstractGraph[SimpleGenNode, SimpleGenEdge, SimpleGenGraph] {
        def createGraph(nodes: Set[SimpleGenNode], edges: Set[SimpleGenEdge], edgesByStart: Map[SimpleGenNode, Set[SimpleGenEdge]], edgesByEnd: Map[SimpleGenNode, Set[SimpleGenEdge]]) =
            new SimpleGenGraph(nodes, edges, edgesByStart, edgesByEnd)
    }

    class SimpleGenGraphViewerWidget(parent: Composite, style: Int, val fig: NodeFigureGenerators[Figure])
        extends Composite(parent, style)
            with AbstractZestGraphWidget[SimpleGenNode, SimpleGenEdge, SimpleGenGraph]
            with Interactable[SimpleGenNode, SimpleGenEdge, SimpleGenGraph] {
        override val graphViewer: GraphViewer = new GraphViewer(this, style)
        override val graphCtrl: Graph = graphViewer.getGraphControl

        setLayout(new FillLayout())

        override def createFigure(node: SimpleGenNode): Figure = {
            val nodeFig = fig.fig.verticalFig(Spacing.Medium, Seq(
                fig.fig.textFig(s"${node.nodeTypeId}", fig.appear.default)
            ) ++ (node.kernels map { k => fig.symbol.symbolPointerFig(grammar, k.symbolId, k.pointer) }))
            nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
            nodeFig.setOpaque(true)
            nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
            nodeFig.setSize(nodeFig.getPreferredSize())
            nodeFig
        }

        override def createConnection(edge: SimpleGenEdge): GraphConnection = {
            val conn = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(edge.start), nodesMap(edge.end))
            conn.setText(edge.description)
            conn.setData(edge)
            conn
        }
    }

    def simpleGenGraph(): SimpleGenGraph = {
        var graph = new SimpleGenGraph(Set(), Set(), Map(), Map())
        val nodesMap = simpleGen.nodes map { node => node._1 -> SimpleGenNode(node._1)(node._2) }
        nodesMap.values foreach { node =>
            graph = graph.addNode(node)
        }
        simpleGen.termActions foreach { kv =>
            val ((lastNode, term), action) = kv
            action match {
                case SimpleGen.Append(appendNodeType, pendingFinish) =>
                    graph = graph.addEdge(SimpleGenEdge(nodesMap(lastNode), nodesMap(appendNodeType))(s"$term -> ${action.toString}"))
                case SimpleGen.ReplaceAndAppend(replaceNodeType, appendNodeType, pendingFinish) =>
                    graph = graph.addEdge(SimpleGenEdge(nodesMap(lastNode), nodesMap(appendNodeType))(s"$term -> ${action.toString}"))
                case _ => // TODO
            }
        }
        simpleGen.impliedNodes foreach { kv =>
            val (originalEdge, impliedEdge) = kv
            graph = graph.addEdge(SimpleGenEdge(nodesMap(originalEdge._1), nodesMap(originalEdge._2))(s"Original"))
            impliedEdge foreach { impl =>
                graph = graph.addEdge(SimpleGenEdge(nodesMap(impl._1), nodesMap(impl._2))(s"Implied from $originalEdge: ${impl._3}"))
            }
        }
        graph
    }

    def start(): Unit = {
        val display = new Display()
        val shell = new Shell(display)

        val graphViewer = new SimpleGenGraphViewerWidget(shell, SWT.NONE, BasicVisualizeResources.nodeFigureGenerators)

        graphViewer.addGraph(simpleGenGraph())
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

object SimpleGenGraphViewer {
    def main(args: Array[String]): Unit = {
        val grammar = NGrammar.fromGrammar(SimpleGrammars.arrayGrammar)

        new SimpleGenGraphViewer(new SimpleGenGen(grammar).generateGenerator()).start()
    }
}
