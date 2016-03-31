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
import org.eclipse.zest.core.widgets.CGraphNode
import org.eclipse.draw2d.Figure
import org.eclipse.swt.graphics.Font
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.MarginBorder
import org.eclipse.draw2d.ToolbarLayout

trait ParsingContextGraphVisualize {
    val graph: Graph
    val resources: ParseGraphVisualizer.Resources

    val figureGenerator: FigureGenerator.Generator[Figure] = FigureGenerator.draw2d.Generator

    val figureAppearances = new FigureGenerator.Appearances[Figure] {
        val default = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 10, SWT.NONE), ColorConstants.black)
        val nonterminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.BOLD), ColorConstants.blue)
        val terminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.NONE), ColorConstants.red)

        override val small = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 8, SWT.NONE), ColorConstants.gray)
        override val kernelDot = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.NONE), ColorConstants.green)
        override val symbolBorder = FigureGenerator.draw2d.BorderAppearance(new LineBorder(ColorConstants.lightGray))
    }
    val tooltipAppearances = new FigureGenerator.Appearances[Figure] {
        val default = figureAppearances.default
        val nonterminal = figureAppearances.nonterminal
        val terminal = figureAppearances.terminal

        override val input = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 10, SWT.NONE), ColorConstants.black)
        override val small = figureAppearances.small
        override val kernelDot = figureAppearances.kernelDot
        override val symbolBorder =
            new FigureGenerator.draw2d.ComplexAppearance(
                FigureGenerator.draw2d.BorderAppearance(new MarginBorder(0, 1, 1, 1)),
                FigureGenerator.draw2d.NewFigureAppearance(),
                FigureGenerator.draw2d.BorderAppearance(new FigureGenerator.draw2d.PartialLineBorder(ColorConstants.lightGray, 1, false, true, true, true)))
    }
    val symbolProgressFigureGenerator = new SymbolProgressFigureGenerator(figureGenerator, figureAppearances)
    val tooltipParseNodeFigureGenerator = new ParseNodeFigureGenerator(figureGenerator, tooltipAppearances)

    private val vnodes = scala.collection.mutable.Map[Parser#Node, GraphNode]()
    private val vedges = scala.collection.mutable.Map[Parser#Edge, GraphConnection]()

    def registerNode(n: Parser#SymbolProgress): GraphNode = vnodes get n match {
        case Some(node) => node
        case None =>
            val fig = symbolProgressFigureGenerator.symbolProgFig(n)
            fig.setBorder(new MarginBorder(1, 2, 1, 2))

            val nodeFig = figureGenerator.horizontalFig(FigureGenerator.Spacing.Medium, Seq(figureGenerator.textFig("" + n.id, figureAppearances.small), fig))
            nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
            nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
            nodeFig.setOpaque(true)
            nodeFig.setSize(nodeFig.getPreferredSize())
            val tooltipFig0: Figure = n match {
                case rep: Parser#RepeatProgress if !rep.children.isEmpty =>
                    figureGenerator.horizontalFig(FigureGenerator.Spacing.Medium, rep.children map { tooltipParseNodeFigureGenerator.parseNodeFig _ })
                case seq: Parser#SequenceProgress if !seq.childrenWS.isEmpty =>
                    figureGenerator.horizontalFig(FigureGenerator.Spacing.Medium, seq.childrenWS map { tooltipParseNodeFigureGenerator.parseNodeFig _ })
                case n if n.canFinish =>
                    tooltipParseNodeFigureGenerator.parseNodeFig(n.parsed.get)
                case _ =>
                    symbolProgressFigureGenerator.symbolProgFig(n)
            }
            val tooltipFig = n match {
                case n: Parser#SymbolProgressNonterminal => figureGenerator.horizontalFig(FigureGenerator.Spacing.Big, Seq(figureGenerator.textFig(s"${n.derivedGen}", figureAppearances.small), tooltipFig0))
                case _ => tooltipFig0
            }
            tooltipFig.setBackgroundColor(ColorConstants.white)
            tooltipFig.setOpaque(true)
            nodeFig.setToolTip(tooltipFig)

            val graphNode = new CGraphNode(graph, SWT.NONE, nodeFig) //new GraphNode(graph, SWT.NONE, n.toShortString)

            graph.addSelectionListener(new SelectionAdapter() {
                override def widgetSelected(e: SelectionEvent): Unit = {
                    if (e.item == graphNode) {
                        println(e.item)
                        println(graphNode.asInstanceOf[CGraphNode].getFigure.getPreferredSize)
                        println(graphNode.asInstanceOf[CGraphNode].getFigure.getInsets)
                        val printString =
                            n match {
                                case rep: Parser#RepeatProgress if !rep.children.isEmpty =>
                                    val list = ParseTree.HorizontalTreeStringSeqUtil.merge(rep.children map { _.toHorizontalHierarchyStringSeq })
                                    list._2 mkString "\n"
                                case seq: Parser#SequenceProgress if !seq.childrenWS.isEmpty =>
                                    val list = ParseTree.HorizontalTreeStringSeqUtil.merge(seq.childrenWS map { _.toHorizontalHierarchyStringSeq })
                                    list._2 mkString "\n"
                                case n if n.canFinish =>
                                    println(n.parsed.get.toHorizontalHierarchyString)
                                case n =>
                                    n.toShortString
                            }
                        println(printString)
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
