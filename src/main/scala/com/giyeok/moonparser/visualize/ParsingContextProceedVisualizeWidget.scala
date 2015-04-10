package com.giyeok.moonparser.visualize

import org.eclipse.draw2d.ColorConstants
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
import com.giyeok.moonparser.ParseTree.TreePrintableParseNode
import com.giyeok.moonparser.Parser
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Event
import com.giyeok.moonparser.ParseTree.TreePrintableParseNode
import org.eclipse.swt.widgets.Widget

class ParsingContextProceedVisualizeWidget(parent: Composite, val resources: ParseGraphVisualizer.Resources, private val context: Parser#ParsingContext, private val log: Parser#VerboseProceedLog) extends Composite(parent, SWT.NONE) with ParsingContextGraphVisualize {
    this.setLayout(new FillLayout)

    val graph = new Graph(this, SWT.NONE)

    private val (nodes, edges) = (context.graph.nodes.asInstanceOf[Set[Parser#Node]], context.graph.edges.asInstanceOf[Set[Parser#Edge]])

    (nodes ++ context.resultCandidates) foreach { registerNode _ }
    context.resultCandidates foreach { highlightResultCandidate _ }

    val registerEdge1 = registerEdge(edges) _
    edges foreach { registerEdge1(_) }

    def registerLifting(lifting: Parser#Lifting): (GraphNode, GraphNode, GraphConnection) = {
        val before = registerNode(lifting.before)
        val after = registerNode(lifting.after)

        val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, before, after)

        lifting.by match {
            case Some(by) => connection.setText(by.id.toString)
            case None =>
        }

        (before, after, connection)
    }

    // blue or cyan edge means lifting
    // node with light gray background means that it is new nodes, that was not existed in the previous context
    // green edges are new edges, not existed in the previous context
    // nodes with red border are root tips
    // red edges are roots

    val newNodeBackgroundColor = ColorConstants.lightGray

    val liftings = (log.terminalLiftings ++ log.liftings).asInstanceOf[Set[Parser#Lifting]]
    val vliftings: Map[Parser#Lifting, (GraphNode, GraphNode, GraphConnection)] = (liftings map { lifting =>
        val (before, after, connection) = registerLifting(lifting)

        if (log.terminalLiftings.asInstanceOf[Set[Parser#Lifting]] contains lifting) {
            after.setFont(resources.bold14Font)
            after.setBackgroundColor(ColorConstants.orange)
            connection.setLineColor(ColorConstants.blue)
        } else {
            connection.setCurveDepth(-10)
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
    val liftingsByBy = liftings filter { _.by.isDefined } groupBy { _.by.get }
    graph.addSelectionListener(new SelectionAdapter() {
        override def widgetSelected(e: SelectionEvent): Unit = {
            unhighlightAllLiftings()
            val interactive = liftingsByBy filter { p => getNode(p._1) match { case Some(node) if node == e.item => true case _ => false } }
            if (!interactive.isEmpty) {
                interactive.values.flatten foreach { lifting =>
                    println(lifting)
                    vliftings(lifting)._3.setLineWidth(5)
                }
            }
        }
    })

    log.newEdges foreach { e =>
        val (from, to, connection) = registerEdge1(edges)(e)

        if (!(nodes contains e.from)) {
            from.setBackgroundColor(newNodeBackgroundColor)
        }
        if (!(nodes contains e.to)) {
            to.setBackgroundColor(newNodeBackgroundColor)
        }
        connection.setLineColor(ColorConstants.green)
    }

    log.rootTips foreach { n =>
        assert(nodes contains n)
        val node = registerNode(n)
        node.setBorderColor(ColorConstants.red)
    }
    log.roots foreach { e =>
        assert(edges contains e)
        val edge = registerEdge(edges)(e)
        edge.setLineColor(ColorConstants.red)
    }

    graph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true)
}
