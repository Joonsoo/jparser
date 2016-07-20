package com.giyeok.jparser.visualize

import org.eclipse.swt.widgets.Composite
import com.giyeok.jparser.nparser.NGrammar
import org.eclipse.draw2d.Figure
import com.giyeok.jparser.ParseResultGraph
import org.eclipse.zest.core.widgets.CGraphNode
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.ColorConstants
import org.eclipse.swt.SWT
import org.eclipse.zest.core.viewers.GraphViewer
import org.eclipse.zest.core.widgets.Graph
import org.eclipse.zest.core.widgets.ZestStyles
import org.eclipse.zest.layouts.LayoutStyles
import org.eclipse.zest.core.widgets.GraphConnection
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.EligCondition._
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.MouseListener
import org.eclipse.swt.widgets.Control
import com.giyeok.jparser.visualize.FigureGenerator.Spacing

trait AbstractZestGraphWidget extends Control {
    val graphViewer: GraphViewer
    val graphCtrl: Graph
    val fig: NodeFigureGenerators[Figure]

    val nodesMap = scala.collection.mutable.Map[Node, CGraphNode]()
    val edgesMap = scala.collection.mutable.Map[Edge, Seq[GraphConnection]]()
    var visibleNodes = Set[Node]()
    var visibleEdges = Set[Edge]()

    def addNode(grammar: NGrammar, node: Node): Unit = {
        if (!(nodesMap contains node)) {
            val nodeFig = fig.nodeFig(grammar, node)
            nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
            nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
            nodeFig.setOpaque(true)
            nodeFig.setSize(nodeFig.getPreferredSize())

            val gnode = new CGraphNode(graphCtrl, SWT.NONE, nodeFig)
            gnode.setData(node)

            nodesMap(node) = gnode
            visibleNodes += node
        }
    }

    def addEdge(edge: Edge): Unit = {
        if (!(edgesMap contains edge)) {
            edge match {
                case SimpleEdge(start, end) =>
                    assert(nodesMap contains start)
                    assert(nodesMap contains end)
                    val conn = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(end))
                    edgesMap(edge) = Seq(conn)
                case JoinEdge(start, end, join) =>
                    assert(nodesMap contains start)
                    assert(nodesMap contains end)
                    assert(nodesMap contains join)
                    val conn = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(end))
                    val connJoin = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(join))
                    conn.setText("main")
                    connJoin.setText("join")
                    edgesMap(edge) = Seq(conn, connJoin)
            }
            visibleEdges += edge
        }
    }

    def addContext(grammar: NGrammar, context: Context): Unit = {
        context.graph.nodes foreach { node =>
            addNode(grammar, node)
        }
        context.graph.edges foreach { edge =>
            addEdge(edge)
        }
    }

    def applyLayout(animation: Boolean): Unit = {
        if (animation) {
            graphViewer.setNodeStyle(ZestStyles.NONE)
        } else {
            graphViewer.setNodeStyle(ZestStyles.NODES_NO_LAYOUT_ANIMATION)
        }
        import org.eclipse.zest.layouts.algorithms._
        val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
        graphCtrl.setLayoutAlgorithm(layoutAlgorithm, true)
    }

    def setVisibleSubgraph(nodes: Set[Node], edges: Set[Edge]): Unit = {
        visibleNodes = nodes
        visibleEdges = edges

        nodesMap foreach { kv =>
            kv._2.setVisible(nodes contains kv._1)
        }
        edgesMap foreach { kv =>
            val visible = edges contains kv._1
            kv._2 foreach { _.setVisible(visible) }
        }
    }
}

trait Highlightable extends AbstractZestGraphWidget {
    val blinkInterval = 100
    val blinkCycle = 10
    val highlightedNodes = scala.collection.mutable.Map[Node, Int]()

    def highlightNode(node: Node): Unit = {
        val display = getDisplay()
        if (!(highlightedNodes contains node) && (visibleNodes contains node)) {
            nodesMap(node).highlight()
            highlightedNodes(node) = 0
            display.timerExec(100, new Runnable() {
                def run(): Unit = {
                    highlightedNodes get node match {
                        case Some(count) =>
                            val shownNode = nodesMap(node)
                            if (count < blinkCycle) {
                                shownNode.setVisible(count % 2 == 0)
                                highlightedNodes(node) = count + 1
                                display.timerExec(blinkInterval, this)
                            } else {
                                shownNode.setVisible(true)
                            }
                        case None => // nothing to do
                    }
                }
            })
        }
    }
    def unhighlightAllNodes(): Unit = {
        highlightedNodes.keys foreach { node =>
            val shownNode = nodesMap(node)
            shownNode.unhighlight()
            shownNode.setVisible(visibleNodes contains node)
        }
        highlightedNodes.clear()
    }
}

class ZestGraphWidget(parent: Composite, style: Int, val fig: NodeFigureGenerators[Figure], grammar: NGrammar, context: Context) extends Composite(parent, style) with AbstractZestGraphWidget with Highlightable {
    val graphViewer = new GraphViewer(this, style)
    val graphCtrl = graphViewer.getGraphControl()

    setLayout(new FillLayout())

    addContext(grammar, context)
    applyLayout(false)

    // finishable node highlight
    val tooltips = scala.collection.mutable.Map[Node, Seq[Figure]]()

    context.finishes.nodeConditions foreach { kv =>
        val (node, conditions) = kv
        nodesMap(node).setBackgroundColor(ColorConstants.yellow)

        val conditionsFig = conditions.toSeq map { fig.conditionFig(grammar, _) }
        tooltips(node) = tooltips.getOrElse(node, Seq()) :+ fig.fig.textFig("Finishes", fig.appear.default) :+ fig.fig.verticalFig(Spacing.Medium, conditionsFig)
    }
    context.progresses.nodeConditions foreach { kv =>
        val (node, conditions) = kv

        val conditionsFig = conditions.toSeq map { fig.conditionFig(grammar, _) }
        tooltips(node) = tooltips.getOrElse(node, Seq()) :+ fig.fig.textFig("Progresses", fig.appear.default) :+ fig.fig.verticalFig(Spacing.Medium, conditionsFig)
    }
    tooltips foreach { kv =>
        val (node, figs) = kv
        val tooltipFig = fig.fig.verticalFig(Spacing.Big, figs)
        tooltipFig.setOpaque(true)
        tooltipFig.setBackgroundColor(ColorConstants.white)
        nodesMap(node).getFigure().setToolTip(tooltipFig)
    }
    // TODO finishable node 더블클릭시 result 보여주기

    override def addKeyListener(keyListener: KeyListener): Unit = graphCtrl.addKeyListener(keyListener)
    override def addMouseListener(mouseListener: MouseListener): Unit = graphCtrl.addMouseListener(mouseListener)
}
