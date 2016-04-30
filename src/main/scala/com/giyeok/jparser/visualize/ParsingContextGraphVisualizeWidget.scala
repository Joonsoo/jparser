package com.giyeok.jparser.visualize

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
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ParseTree
import com.giyeok.jparser.Parser
import com.giyeok.jparser.ParseTree.TreePrint
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
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.Symbols.Terminal
import com.giyeok.jparser.Symbols.Terminals

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

    val vnodes = scala.collection.mutable.Map[Parser#Node, CGraphNode]()
    val vedges = scala.collection.mutable.Map[Parser#DeriveEdge, GraphConnection]()
    val vreverters = scala.collection.mutable.Map[Parser#Reverter, List[GraphConnection]]()

    def newSymbolProgressContentFig(node: Parser#SymbolProgress, horizontal: Boolean, renderConf: ParseNodeFigureGenerator.RenderingConfiguration) = {
        val figFunc: (ParseTree.ParseNode[Symbols.Symbol], ParseNodeFigureGenerator.RenderingConfiguration) => Figure =
            if (horizontal) tooltipParseNodeFigureGenerator.parseNodeHFig else tooltipParseNodeFigureGenerator.parseNodeVFig
        val tooltipFig0: Figure = node match {
            case rep: Parser#NonAtomicSymbolProgress[_] if !rep._parsed.children.isEmpty =>
                figureGenerator.horizontalFig(FigureGenerator.Spacing.Medium, rep._parsed.children map { figFunc(_, renderConf) })
            case n if n.kernel.finishable =>
                figFunc(n.parsed.get, renderConf)
            case _ =>
                symbolProgressFigureGenerator.symbolProgFig(node)
        }
        val tooltipFig = figureGenerator.horizontalFig(FigureGenerator.Spacing.Big, Seq(figureGenerator.textFig(s"${node.derivedGen}${node.lastLiftedGen match { case Some(x) => s"-$x" case _ => "" }}", figureAppearances.small), tooltipFig0))
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
                                case rep: Parser#NonAtomicSymbolProgress[_] if !rep._parsed.children.isEmpty =>
                                    val list = ParseTree.HorizontalTreeStringSeqUtil.merge(rep._parsed.children map { _.toHorizontalHierarchyStringSeq })
                                    list._2 mkString "\n"
                                case n if n.kernel.finishable =>
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
        node.highlight()
    }

    val proceededEdgeColor = new Color(null, 255, 128, 128)
    def highlightProceededEdge(e: Parser#SimpleEdge): Unit = {
        val proceededEdge = vedges(e)
        proceededEdge.setLineColor(proceededEdgeColor)
        proceededEdge.setLineWidth(3)
    }
    def highlightInternalProceededEdge(e: Parser#SimpleEdge): Unit = {
        val proceededEdge = vedges(e)
        proceededEdge.setLineColor(proceededEdgeColor)
    }

    private def calculateCurve(edges: Set[Parser#DeriveEdge], e: Parser#DeriveEdge): Int = {
        val overlapping = edges filter { r => (((r.start == e.start) && (r.end == e.end)) || ((r.start == e.end) && (r.end == e.start))) && (e != r) }
        if (!overlapping.isEmpty) {
            overlapping count { _.hashCode < e.hashCode }
        } else if (edges exists { r => (r.start == e.end) && (r.end == e.start) }) 1
        else 0
    }

    val darkerRed = new Color(null, 139, 0, 0)
    val deriveReverterEdgeColor = darkerRed
    val liftReverterEdgeColor = darkerRed
    val tempLiftBlockReverterEdgeColor = darkerRed

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
    def registerReverter(reverter: Parser#Reverter): Unit = {
        reverter match {
            case x: Parser#MultiTriggeredDeriveReverter =>
                // Working Reverter
                val end = registerNode(x.targetEdge.end)
                val edges = x.triggers.toList map {
                    _ match {
                        case t: Parser#TriggerIfLift =>
                            val edge = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, registerNode(t.trigger), end)
                            edge.setCurveDepth(-40)
                            edge.setLineColor(deriveReverterEdgeColor)
                            edge.setText(s"${x.targetEdge.start.id}->${x.targetEdge.end.id} if Lifted")
                            edge
                        case t: Parser#TriggerIfAlive =>
                            val edge = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, registerNode(t.trigger), end)
                            edge.setCurveDepth(-40)
                            edge.setLineColor(deriveReverterEdgeColor)
                            edge.setText(s"${x.targetEdge.start.id}->${x.targetEdge.end.id} if Alive")
                            edge
                    }
                }
                vreverters(x) = edges

            case x: Parser#MultiTriggeredNodeKillReverter =>
                // Working Reverter
                val end = registerNode(x.targetNode)
                val edges = x.triggers.toList map {
                    _ match {
                        case t: Parser#TriggerIfLift =>
                            val start = registerNode(t.trigger)
                            val edge = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, start, end)
                            edge.setText("if Lifted")
                            edge
                        case t: Parser#TriggerIfAlive =>
                            val start = registerNode(t.trigger)
                            val edge = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, start, end)
                            edge.setText("if Alive")
                            edge
                    }
                }
                edges.zipWithIndex foreach { ei =>
                    val (edge, idx) = ei
                    edge.setLineColor(liftReverterEdgeColor)
                    edge.setCurveDepth(-20 * (idx + 1))
                }
                vreverters(x) = edges

            case x: Parser#LiftTriggeredTempLiftBlockReverter =>
                // Working Reverter
                val start = registerNode(x.trigger)
                val end = registerNode(x.targetNode)
                val edge = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, start, end)
                edge.setLineColor(tempLiftBlockReverterEdgeColor)
                edge.setCurveDepth(-40)
                edge.setText("TempLiftBlock")
                vreverters(x) = List(edge)

            case x: Parser#ReservedAliveTriggeredLiftedNodeReverter =>
                vreverters(x) = List()

            case x: Parser#ReservedLiftTriggeredLiftedNodeReverter =>
                vreverters(x) = List()
        }
    }

    def nodesAt(ex: Int, ey: Int): Seq[Parser#SymbolProgress] = {
        import scala.collection.JavaConversions._

        val (x, y) = (ex + graph.getHorizontalBar().getSelection(), ey + graph.getVerticalBar().getSelection())
        println(graph.getFigureAt(x, y))

        val selectedNodeData = graph.getNodes.toList collect {
            case n: GraphNode if n != null && n.getNodeFigure() != null && n.getNodeFigure().containsPoint(x, y) => n.getData
        }
        selectedNodeData collect { case node: Parser#SymbolProgress => node }
    }

    graph.addMouseListener(new MouseAdapter() {
        override def mouseDoubleClick(e: MouseEvent): Unit = {
            println("ClickedFigure:")
            nodesAt(e.x, e.y) foreach { node =>
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

    var terminalHighlighted: Boolean = false
    def toggleHighlightTerminals(forced: Boolean): Unit = {
        terminalHighlighted = forced || !terminalHighlighted
        vnodes filter { _._1.kernel.symbol.isInstanceOf[Terminal] } foreach { kv =>
            val node = kv._2
            node.setBackgroundColor(if (terminalHighlighted) ColorConstants.cyan else ColorConstants.white)
            node.highlight()
        }
    }
    def reorderTerminals(): Unit = {
        val totalDim = graph.getSize()
        val sortedNodesKv = (vnodes map { kv => (kv, kv._1.kernel.symbol) } collect {
            case (kv, t: Terminal) => (kv, t)
        }).toSeq.sortWith((kv1, kv2) => Terminals.compare(kv1._2, kv2._2) < 0)
        (sortedNodesKv map { _._1 }).foldLeft(0) { (y, kv) =>
            val node = kv._2
            val dim = node.getSize()
            node.setLocation(totalDim.x - dim.width, y)
            node.setBackgroundColor(if (terminalHighlighted) ColorConstants.cyan else ColorConstants.white)
            node.highlight()
            y + dim.height + 5
        }
    }
}

class ParsingContextGraphVisualizeWidget(parent: Composite, val resources: ParseGraphVisualizer.Resources, val context: Parser#ParsingContext) extends Composite(parent, SWT.NONE) with ParsingContextGraphVisualize {
    this.setLayout(new FillLayout)

    def initGraph() = new Graph(this, SWT.NONE)

    (context.nodes ++ context.resultCandidates) foreach { registerNode _ }

    val registerEdge1 = registerEdge(context.edges.asInstanceOf[Set[Parser#DeriveEdge]]) _
    context.edges foreach { registerEdge1(_) }

    context.reverters foreach { registerReverter(_) }

    context.resultCandidates foreach { highlightResultCandidate _ }

    context.externalProceededEdges foreach { highlightProceededEdge _ }
    context.internalProceededEdges foreach { highlightInternalProceededEdge _ }

    import org.eclipse.zest.layouts.algorithms._
    val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
    graph.setLayoutAlgorithm(layoutAlgorithm, true)
}
