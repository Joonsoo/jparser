package com.giyeok.moonparser.visualize

import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Graphics
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.zest.core.widgets.Graph
import org.eclipse.zest.core.widgets.GraphConnection
import org.eclipse.zest.core.widgets.GraphNode
import org.eclipse.zest.core.widgets.ZestStyles
import org.eclipse.zest.layouts.LayoutStyles
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm
import com.giyeok.moonparser.ParseTree
import com.giyeok.moonparser.ParseTree.TreePrint
import com.giyeok.moonparser.Parser
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Widget
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.KeyEvent
import scala.collection.JavaConversions._
import org.eclipse.draw2d.MarginBorder
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.Figure
import org.eclipse.zest.core.widgets.CGraphNode
import org.eclipse.swt.widgets.Display

class ParsingContextProceedVisualizeWidget(parent: Composite, val resources: ParseGraphVisualizer.Resources, val context: Option[Parser#ParsingContext], private val proceedLog: Parser#VerboseProceedLog) extends Composite(parent, SWT.NONE) with ParsingContextGraphVisualize {
    this.setLayout(new FillLayout)

    def initGraph() = new Graph(this, SWT.NONE)

    private val (nodes: Set[Parser#Node], edges: Set[Parser#DeriveEdge]) = context match {
        case Some(ctx) => (ctx.nodes.asInstanceOf[Set[Parser#Node]], ctx.edges.asInstanceOf[Set[Parser#DeriveEdge]])
        case _ => (Set(), Set())
    }

    nodes foreach { registerNode _ }

    val registerEdge1 = registerEdge(edges) _
    edges foreach { registerEdge1(_) }

    def registerLifting(lifting: Parser#Lifting): (GraphNode, GraphNode, Option[GraphNode], GraphConnection) = {
        val before = registerNode(lifting.before)
        val after = registerNode(lifting.after)

        val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, before, after)

        val by = lifting match {
            case l: Parser#NontermLifting => Some(registerNode(l.byNode))
            case l: Parser#JoinLifting => Some(registerNode(l.byNode))
            case l: Parser#TermLifting => None
        }

        (before, after, by, connection)
    }

    // blue or cyan edge means lifting
    // node with light gray background means that it is new nodes, that was not existed in the previous context
    // green edges are new edges, not existed in the previous context
    // nodes with violet border are root tips
    // violet edges are roots

    val newNodeBackgroundColor = ColorConstants.lightGray

    val liftings = (proceedLog.terminalLiftings ++ proceedLog.liftings).asInstanceOf[Set[Parser#Lifting]]
    val vliftings: Map[Parser#Lifting, (GraphNode, GraphNode, GraphConnection)] = (liftings map { lifting =>
        val (before, after, by, connection) = registerLifting(lifting)

        if (proceedLog.terminalLiftings.asInstanceOf[Set[Parser#Lifting]] contains lifting) {
            after.setFont(resources.bold14Font)
            after.setBackgroundColor(ColorConstants.orange)
            connection.setLineColor(ColorConstants.blue)
        } else {
            connection.setCurveDepth(-20)
            connection.setLineColor(ColorConstants.cyan)
            if (!(nodes contains lifting.after)) {
                after.setBackgroundColor(newNodeBackgroundColor)
            }
        }

        lifting -> (before, after, connection)
    }).toMap

    def unhighlightAllLiftings(): Unit = {
        vliftings foreach { v => v._2._3.setLineWidth(1) }
    }
    unhighlightAllLiftings()
    val liftingsByBy = liftings collect { case l: Parser#NontermLifting => l } groupBy { _.byNode }
    graph.addSelectionListener(new SelectionAdapter() {
        override def widgetSelected(e: SelectionEvent): Unit = {
            val selectedEdges = vedges filter { p => e.item == p._2 }
            println(selectedEdges)
            selectedEdges foreach { p =>
                p._1.nodes foreach { vnodes(_).highlight() }
                p._2.highlight()
            }

            val selectedLiftings = vliftings filter { p => e.item == p._2._3 }
            println(selectedLiftings)
            selectedLiftings foreach { p =>
                p._2._1.highlight()
                p._2._2.highlight()
                p._2._3.highlight()
            }

            val selectedNodes = vnodes filter { p => e.item == p._2 }
            println(selectedNodes)

            unhighlightAllLiftings()
            val interactiveLift = liftingsByBy filter { p => getNode(p._1) match { case Some(node) if node == e.item => true case _ => false } }
            if (!interactiveLift.isEmpty) {
                interactiveLift.values.flatten foreach { lifting =>
                    println(lifting)
                    vliftings(lifting)._3.setLineWidth(5)
                }
            }
        }
    })

    proceedLog.newEdges foreach { e =>
        val (from, to, connection) = registerEdge1(edges)(e)

        if (!(nodes contains e.start)) {
            from.setBackgroundColor(newNodeBackgroundColor)
        }
        if (!(nodes contains e.end)) {
            to.setBackgroundColor(newNodeBackgroundColor)
        }
        connection.setLineStyle(Graphics.LINE_DOT)
    }

    val rootColor = new Color(null, 238, 130, 238) // supposedly violet
    proceedLog.roots foreach { e =>
        assert(edges contains e)
        val edge = registerEdge(edges)(e)
        if (e.isInstanceOf[Parser#SimpleEdge]) {
            edge.setLineColor(rootColor)
        }
    }
    proceedLog.proceededEdges foreach { n => highlightProceededEdge(n._2) }
    proceedLog.internalProceededEdges foreach { n => highlightInternalProceededEdge(n._2) }

    // visualize reverters
    context foreach { _.reverters foreach { registerReverter(_) } }
    proceedLog.newReverters foreach { registerReverter(_) }
    proceedLog.finalReverters foreach { registerReverter(_) }

    proceedLog.activatedReverters foreach { activatedReverter =>
        vreverters(activatedReverter) foreach { e => e.setLineWidth(5) }
    }

    val revertedNodeBorderColor = new Color(Display.getDefault(), 200, 200, 200)
    val liftBlockedNodeBorderColor = new Color(Display.getDefault(), 255, 100, 100)
    proceedLog.revertedNodes foreach { revertedNode =>
        val cg = vnodes(revertedNode)
        val blurBorder = new LineBorder
        blurBorder.setColor(revertedNodeBorderColor)
        blurBorder.setStyle(Graphics.LINE_DASHDOT)
        cg.getFigure.setBorder(blurBorder)
    }
    proceedLog.revertedEdges foreach { revertedEdge =>
        val e = vedges(revertedEdge)
        e.setLineWidth(2)
        e.setLineStyle(Graphics.LINE_DASHDOT)
    }
    proceedLog.liftBlockedNodes foreach { liftBlockedNode =>
        val cg = vnodes(liftBlockedNode)
        val blurBorder = new LineBorder
        blurBorder.setColor(liftBlockedNodeBorderColor)
        blurBorder.setStyle(Graphics.LINE_DASHDOT)
        cg.getFigure.setBorder(blurBorder)
    }
    //    val propagatedAssassinEdgeColor = new Color(null, 233, 150, 122) // dark salmon
    //    log.propagatedAssassinEdges foreach { e =>
    //        val (from, to, connection) = registerEdge1(edges)(e)
    //        connection.setLineColor(propagatedAssassinEdgeColor)
    //    }

    graph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS), true)
}
