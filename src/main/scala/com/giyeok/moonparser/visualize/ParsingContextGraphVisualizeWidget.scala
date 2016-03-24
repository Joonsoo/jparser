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
import com.giyeok.moonparser.Inputs
import com.giyeok.moonparser.ParseTree
import com.giyeok.moonparser.Parser
import com.giyeok.moonparser.ParseTree.TreePrintableParseNode
import org.eclipse.swt.graphics.Color

trait ParsingContextGraphVisualize {
    val graph: Graph
    val resources: ParseGraphVisualizer.Resources

    private val vnodes = scala.collection.mutable.Map[Parser#Node, GraphNode]()
    private val vedges = scala.collection.mutable.Map[Parser#Edge, GraphConnection]()

    def registerNode(n: Parser#SymbolProgress): GraphNode = vnodes get n match {
        case Some(node) => node
        case None =>
            val graphNode = new GraphNode(graph, SWT.NONE, n.toShortString)
            graphNode.setFont(resources.default12Font)
            n match {
                case term: Parser#SymbolProgressTerminal if term.parsed.isEmpty =>
                    graphNode.setBackgroundColor(ColorConstants.lightGreen)
                case _ =>
            }
            val tooltipText0 = n match {
                case rep: Parser#RepeatProgress if !rep.children.isEmpty =>
                    val list = ParseTree.HorizontalTreeStringSeqUtil.merge(rep.children map { _.toHorizontalHierarchyStringSeq })
                    list._2 mkString "\n"
                case seq: Parser#SequenceProgress if !seq.childrenWS.isEmpty =>
                    val list = ParseTree.HorizontalTreeStringSeqUtil.merge(seq.childrenWS map { _.toHorizontalHierarchyStringSeq })
                    list._2 mkString "\n"
                case n if n.canFinish =>
                    n.parsed.get.toHorizontalHierarchyString
                case _ =>
                    n.toShortString
            }
            val tooltipText = n match {
                case n: Parser#SymbolProgressNonterminal => s"${n.derivedGen}\n$tooltipText0"
                case _ => tooltipText0
            }
            val f = new org.eclipse.draw2d.Label()
            f.setFont(resources.fixedWidth12Font)
            f.setText(tooltipText)
            graphNode.setTooltip(f)
            graph.addSelectionListener(new SelectionAdapter() {
                override def widgetSelected(e: SelectionEvent): Unit = {
                    if (e.item == graphNode) {
                        println(e.item)
                        println(tooltipText)
                    }
                }
            })
            vnodes(n) = graphNode
            graphNode
    }
    def getNode(n: Parser#SymbolProgress): Option[GraphNode] = vnodes get n

    def highlightResultCandidate(n: Parser#Node): Unit = {
        val node = vnodes(n)
        node.setFont(resources.bold14Font)
        node.setBackgroundColor(ColorConstants.orange)
    }

    private def calculateCurve(edges: Set[Parser#Edge], e: Parser#Edge): Int = {
        val overlapping = edges filter { r => (((r.start == e.start) && (r.end == e.end)) || ((r.start == e.end) && (r.end == e.start))) && (e != r) }
        if (!overlapping.isEmpty) {
            overlapping count { _.hashCode < e.hashCode }
        } else if (edges exists { r => (r.start == e.end) && (r.end == e.start) }) 1
        else 0
    }

    val darkerRed = new Color(null, 139, 0, 0)

    def registerEdge(edges: Set[Parser#Edge])(e: Parser#Edge): GraphConnection = e match {
        case e: Parser#SimpleEdge =>
            val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.start), vnodes(e.end))
            val curves = calculateCurve(edges, e)

            if (curves > 0) {
                connection.setCurveDepth(curves * 12)
            }
            vedges(e) = connection
            connection
        case e: Parser#JoinEdge =>
            val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.start), vnodes(e.end))
            // TODO e.contraint 도 표시
            connection
        case e: Parser#LiftAssassinEdge =>
            val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.start), vnodes(e.end))
            connection.setLineColor(ColorConstants.red)
            vedges(e) = connection
            connection
        case e: Parser#EagerAssassinEdge =>
            val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.start), vnodes(e.end))
            connection.setLineColor(darkerRed)
            vedges(e) = connection
            connection
    }
    def registerEdge1(edges: Set[Parser#Edge])(e: Parser#Edge): (GraphNode, GraphNode, GraphConnection) = {
        val start = registerNode(e.start)
        val end = registerNode(e.end)
        (start, end, registerEdge(edges)(e))
    }
}

class ParsingContextGraphVisualizeWidget(parent: Composite, val resources: ParseGraphVisualizer.Resources, private val context: Parser#ParsingContext) extends Composite(parent, SWT.NONE) with ParsingContextGraphVisualize {
    this.setLayout(new FillLayout)

    val graph = new Graph(this, SWT.NONE)

    (context.graph.nodes ++ context.resultCandidates) foreach { registerNode _ }
    context.resultCandidates foreach { highlightResultCandidate _ }

    val registerEdge1 = registerEdge(context.graph.edges.asInstanceOf[Set[Parser#Edge]]) _
    context.graph.edges foreach { registerEdge1(_) }

    graph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true)
}
