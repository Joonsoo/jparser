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

    val newNodeBackgroundColor = ColorConstants.lightGray

    (log.terminalLiftings ++ log.liftings).asInstanceOf[Set[Parser#Lifting]] foreach { lifting =>
        val (_, after, connection) = registerLifting(lifting)

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
    }

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
