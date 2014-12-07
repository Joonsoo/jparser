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

    private val (nodes, edges) = (context.graph.nodes, context.graph.edges)
    val graph = new Graph(this, SWT.NONE)
    private val proceed1: Set[(Parser#SymbolProgress, Parser#SymbolProgress)] = log match {
        case Some(log) => log.terminalProceeds.asInstanceOf[Set[(Parser#SymbolProgress, Parser#SymbolProgress)]]
        case None => Set()
    }
    private val proceeded = proceed1 map { _._2 }
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
    private val vedges: Set[GraphConnection] = edges flatMap {
        case e: Parser#SimpleEdge =>
            Set(new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.from), vnodes(e.to)))
        case e: Parser#EagerAssassinEdge =>
            val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.from), vnodes(e.to))
            connection.setLineColor(ColorConstants.red)
            Set(connection)
        case _ => None
    }
    private val nedges = proceed1 map { p =>
        val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(p._1), vnodes(p._2))
        connection.setLineColor(ColorConstants.blue)
        connection
    }
    graph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true)
}
