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
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.swt.events.MouseAdapter
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.MouseEvent
import com.giyeok.moonparser.Symbols

trait ParsingContextGraphVisualize {
    def initGraph(): Graph
    val graph: Graph = initGraph()
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
        override val hSymbolBorder =
            new FigureGenerator.draw2d.ComplexAppearance(
                FigureGenerator.draw2d.BorderAppearance(new MarginBorder(0, 1, 1, 1)),
                FigureGenerator.draw2d.NewFigureAppearance(),
                FigureGenerator.draw2d.BorderAppearance(new FigureGenerator.draw2d.PartialLineBorder(ColorConstants.lightGray, 1, false, true, true, true)))
        override val vSymbolBorder =
            new FigureGenerator.draw2d.ComplexAppearance(
                FigureGenerator.draw2d.BorderAppearance(new MarginBorder(1, 0, 1, 1)),
                FigureGenerator.draw2d.NewFigureAppearance(),
                FigureGenerator.draw2d.BorderAppearance(new FigureGenerator.draw2d.PartialLineBorder(ColorConstants.lightGray, 1, true, false, true, true)))
        override val wsBorder =
            new FigureGenerator.draw2d.ComplexAppearance(
                FigureGenerator.draw2d.BorderAppearance(new MarginBorder(0, 1, 1, 1)),
                FigureGenerator.draw2d.NewFigureAppearance(),
                FigureGenerator.draw2d.BorderAppearance(new FigureGenerator.draw2d.PartialLineBorder(ColorConstants.lightBlue, 1, false, true, true, true)),
                FigureGenerator.draw2d.BackgroundAppearance(ColorConstants.lightGray))
        override val joinHighlightBorder =
            new FigureGenerator.draw2d.ComplexAppearance(
                FigureGenerator.draw2d.BorderAppearance(new MarginBorder(1, 1, 1, 1)),
                FigureGenerator.draw2d.NewFigureAppearance(),
                FigureGenerator.draw2d.BorderAppearance(new LineBorder(ColorConstants.red)))

    }
    val symbolProgressFigureGenerator = new SymbolProgressFigureGenerator(figureGenerator, figureAppearances)
    val tooltipParseNodeFigureGenerator = new ParseNodeFigureGenerator(figureGenerator, tooltipAppearances)

    private val vnodes = scala.collection.mutable.Map[Parser#Node, CGraphNode]()
    private val vedges = scala.collection.mutable.Map[Parser#DeriveEdge, GraphConnection]()

    def newSymbolProgressContentFig(node: Parser#SymbolProgress, horizontal: Boolean, renderConf: ParseNodeFigureGenerator.RenderingConfiguration) = {
        val figFunc: (ParseTree.ParseNode[Symbols.Symbol], ParseNodeFigureGenerator.RenderingConfiguration) => Figure =
            if (horizontal) tooltipParseNodeFigureGenerator.parseNodeHFig else tooltipParseNodeFigureGenerator.parseNodeVFig
        val tooltipFig0: Figure = node match {
            case rep: Parser#RepeatProgress if !rep.children.isEmpty =>
                figureGenerator.horizontalFig(FigureGenerator.Spacing.Medium, rep.children map { figFunc(_, renderConf) })
            case seq: Parser#SequenceProgress if !seq.childrenWS.isEmpty =>
                figureGenerator.horizontalFig(FigureGenerator.Spacing.Medium, seq.childrenWS map { figFunc(_, renderConf) })
            case n if n.canFinish =>
                figFunc(n.parsed.get, renderConf)
            case _ =>
                symbolProgressFigureGenerator.symbolProgFig(node)
        }
        val tooltipFig = node match {
            case n: Parser#SymbolProgressNonterminal =>
                figureGenerator.horizontalFig(FigureGenerator.Spacing.Big, Seq(figureGenerator.textFig(s"Gen ${n.derivedGen}", figureAppearances.small), tooltipFig0))
            case _ => tooltipFig0
        }
        tooltipFig.setBackgroundColor(ColorConstants.white)
        tooltipFig.setOpaque(true)
        tooltipFig
    }

    def registerNode(n: Parser#SymbolProgress): CGraphNode = vnodes get n match {
        case Some(node) => node
        case None =>
            val fig = symbolProgressFigureGenerator.symbolProgFig(n)
            fig.setBorder(new MarginBorder(1, 2, 1, 2))

            val nodeFig = figureGenerator.horizontalFig(FigureGenerator.Spacing.Medium, Seq(figureGenerator.textFig("" + n.id, figureAppearances.small), fig))
            nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
            nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
            nodeFig.setOpaque(true)
            nodeFig.setSize(nodeFig.getPreferredSize())
            nodeFig.setToolTip(newSymbolProgressContentFig(n, true, ParseNodeFigureGenerator.cleanestConfiguration))

            val graphNode = new CGraphNode(graph, SWT.NONE, nodeFig) //new GraphNode(graph, SWT.NONE, n.toShortString)
            graphNode.setData(n)

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

    private def calculateCurve(edges: Set[Parser#DeriveEdge], e: Parser#DeriveEdge): Int = {
        val overlapping = edges filter { r => (((r.start == e.start) && (r.end == e.end)) || ((r.start == e.end) && (r.end == e.start))) && (e != r) }
        if (!overlapping.isEmpty) {
            overlapping count { _.hashCode < e.hashCode }
        } else if (edges exists { r => (r.start == e.end) && (r.end == e.start) }) 1
        else 0
    }

    val darkerRed = new Color(null, 139, 0, 0)

    def registerEdge(edges: Set[Parser#DeriveEdge])(e: Parser#DeriveEdge): GraphConnection = {
        val edge = e match {
            case e: Parser#SimpleEdge =>
                val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.start), vnodes(e.end))
                vedges(e) = connection
                connection
            case e: Parser#JoinEdge =>
                val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.start), vnodes(e.end))
                // TODO e.contraint 도 표시
                connection
        }
        val curves = calculateCurve(edges, e)

        if (curves > 0) {
            edge.setCurveDepth(curves * 20)
        }
        edge
    }
    def registerEdge1(edges: Set[Parser#DeriveEdge])(e: Parser#DeriveEdge): (GraphNode, GraphNode, GraphConnection) = {
        val start = registerNode(e.start)
        val end = registerNode(e.end)
        (start, end, registerEdge(edges)(e))
    }

    graph.addMouseListener(new MouseAdapter() {
        import scala.collection.JavaConversions._

        override def mouseDoubleClick(e: MouseEvent): Unit = {
            println("ClickedFigure:")
            val (x, y) = (e.x + graph.getHorizontalBar().getSelection(), e.y + graph.getVerticalBar().getSelection())
            println(graph.getFigureAt(x, y))

            val selectedNodeData = graph.getNodes.toList collect {
                case n: GraphNode if n != null && n.getNodeFigure() != null && n.getNodeFigure().containsPoint(x, y) => n.getData
            }
            val selectedNodes = selectedNodeData collect { case node: Parser#SymbolProgress => node }
            selectedNodes foreach { node =>
                import org.eclipse.swt.widgets._
                val shell = new Shell(Display.getDefault())
                shell.setLayout(new FillLayout())
                val figCanvas = new FigureCanvas(shell)

                class MutableRenderingStatus(var horizontal: Boolean, var renderJoin: Boolean, var renderWS: Boolean, var renderLookaheadExcept: Boolean)
                val rs = new MutableRenderingStatus(true, true, true, true)
                def resetContents(): Unit = {
                    figCanvas.setContents(
                        figureGenerator.verticalFig(FigureGenerator.Spacing.Big, Seq(
                            figureGenerator.textFig(s"${if (rs.horizontal) "Horizontal" else "Vertical"} renderJoin=${rs.renderJoin}, renderWS=${rs.renderWS}, renderLookaheadExcept=${rs.renderLookaheadExcept}", figureAppearances.default),
                            newSymbolProgressContentFig(node, rs.horizontal, ParseNodeFigureGenerator.RenderingConfiguration(rs.renderJoin, rs.renderWS, rs.renderLookaheadExcept)))))
                }
                resetContents()

                figCanvas.addKeyListener(new KeyAdapter() {
                    override def keyPressed(e: org.eclipse.swt.events.KeyEvent): Unit = {
                        if (e.keyCode == '`'.toInt) {
                            rs.horizontal = !rs.horizontal
                        }
                        if (e.keyCode == '1'.toInt) {
                            rs.renderJoin = !rs.renderJoin
                        }
                        if (e.keyCode == '2'.toInt) {
                            rs.renderWS = !rs.renderWS
                        }
                        if (e.keyCode == '3'.toInt) {
                            rs.renderLookaheadExcept = !rs.renderLookaheadExcept
                        }
                        resetContents()
                    }
                })
                shell.addListener(SWT.Close, new Listener() {
                    def handleEvent(e: Event): Unit = {
                        shell.dispose()
                    }
                })
                shell.setText(node.toShortString)
                shell.open()
            }
        }
    })
}

class ParsingContextGraphVisualizeWidget(parent: Composite, val resources: ParseGraphVisualizer.Resources, private val context: Parser#ParsingContext) extends Composite(parent, SWT.NONE) with ParsingContextGraphVisualize {
    this.setLayout(new FillLayout)

    def initGraph() = new Graph(this, SWT.NONE)

    (context.nodes ++ context.resultCandidates) foreach { registerNode _ }
    context.resultCandidates foreach { highlightResultCandidate _ }

    val registerEdge1 = registerEdge(context.edges.asInstanceOf[Set[Parser#DeriveEdge]]) _
    context.edges foreach { registerEdge1(_) }

    import org.eclipse.zest.layouts.algorithms._
    val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
    graph.setLayoutAlgorithm(layoutAlgorithm, true)
}
