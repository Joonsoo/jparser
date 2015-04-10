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

class ParsingContextProceedVisualizeWidget(parent: Composite, resources: ParseGraphVisualizer.Resources, private val context: Parser#ParsingContext, private val log: Parser#VerboseProceedLog) extends Composite(parent, SWT.NONE) {
    this.setLayout(new FillLayout)

    private val (nodes, edges) = (context.graph.nodes, context.graph.edges.asInstanceOf[Set[Parser#Edge]])
    val graph = new Graph(this, SWT.NONE)

    private var vnodes: Map[Parser#Node, GraphNode] = ((nodes ++ context.resultCandidates) map { n: Parser#SymbolProgress =>
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
        (n, graphNode)
    }).toMap

    graph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true)
}
