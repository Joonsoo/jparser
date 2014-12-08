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
import com.giyeok.moonparser.ParseTree.TreePrintableParseNode
import com.giyeok.moonparser.Parser

class ParsingContextGraphVisualizeWidget(parent: Composite, resources: ParseGraphVisualizer.Resources, private val context: Parser#ParsingContext, private val log: Option[Parser#TerminalProceedLog]) extends Composite(parent, SWT.NONE) {
    this.setLayout(new FillLayout)

    private val (nodes, edges) = (context.graph.nodes, context.graph.edges.asInstanceOf[Set[Parser#Edge]])
    val graph = new Graph(this, SWT.NONE)
    private val proceed1: Set[Parser#Lifting] = log match {
        case Some(log) => log.terminalProceeds.asInstanceOf[Set[Parser#Lifting]]
        case None => Set()
    }
    private val proceeded = proceed1 map { _.after }
    private var vnodes: Map[Parser#Node, GraphNode] = ((nodes ++ context.resultCandidates ++ proceeded) map { n =>
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
        if (proceeded contains n) {
            graphNode.setFont(resources.italic14Font)
            graphNode.setBackgroundColor(ColorConstants.yellow)
        }
        (n, graphNode)
    }).toMap
    context.resultCandidates foreach { p =>
        val node = vnodes(p)
        node.setFont(resources.bold14Font)
        node.setBackgroundColor(ColorConstants.orange)
    }
    private def calculateCurve(edges: Set[Parser#Edge], e: Parser#Edge): Int = {
        val overlapping = edges filter { r => (r.from == e.from) && (r.to == e.to) && (e != r) }
        if (!overlapping.isEmpty) {
            overlapping count { _.hashCode < e.hashCode }
        } else if (edges exists { r => (r.from == e.to) && (r.to == e.from) }) 1
        else 0
    }
    private val vedges: Set[GraphConnection] = edges collect {
        case e: Parser#SimpleEdge =>
            val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.from), vnodes(e.to))
            val curves = calculateCurve(edges, e)

            if (curves > 0) {
                connection.setCurveDepth(curves * 12)
            }
            connection
        case e: Parser#EagerAssassinEdge =>
            val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.from), vnodes(e.to))
            connection.setLineColor(ColorConstants.red)
            connection
    }
    private val nedges = proceed1 map { p =>
        val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(p.before), vnodes(p.after))
        connection.setLineColor(ColorConstants.blue)
        connection
    }
    graph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true)
}
