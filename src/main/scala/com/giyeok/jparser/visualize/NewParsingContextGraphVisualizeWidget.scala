package com.giyeok.jparser.visualize

import com.giyeok.jparser.NewParser
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Composite
import org.eclipse.zest.core.widgets.Graph
import org.eclipse.swt.SWT
import org.eclipse.zest.core.widgets.GraphConnection
import org.eclipse.zest.core.widgets.CGraphNode
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.MarginBorder
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d
import org.eclipse.swt.graphics.Font
import com.giyeok.jparser.visualize.FigureGenerator.Spacing
import org.eclipse.zest.layouts.LayoutStyles
import org.eclipse.swt.layout.FillLayout
import org.eclipse.zest.core.widgets.ZestStyles
import com.giyeok.jparser.Grammar
import org.eclipse.swt.widgets.Shell
import com.giyeok.jparser.DerivationGraph
import org.eclipse.swt.widgets.Display
import com.giyeok.jparser.Symbols.Nonterm
import org.eclipse.zest.core.widgets.GraphNode
import org.eclipse.swt.events.MouseAdapter
import org.eclipse.swt.events.MouseEvent

trait BasicGenerators {
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
    val parseNodeFigureGenerator = new ParseNodeFigureGenerator(figureGenerator, tooltipAppearances)
}

trait NewParserGraphVisualizeWidget {
    val graphView: Graph
    val grammar: Grammar

    val figureGenerator: FigureGenerator.Generator[Figure]
    val figureAppearances: FigureGenerator.Appearances[Figure]
    val symbolProgressFigureGenerator: SymbolProgressFigureGenerator[Figure]

    val nodesMap = scala.collection.mutable.Map[NewParser.Node, CGraphNode]()
    val edgesMap = scala.collection.mutable.Map[NewParser.Edge, Seq[GraphConnection]]()

    def nodeFromFigure(fig: Figure): CGraphNode = {
        val nodeFig = figureGenerator.horizontalFig(FigureGenerator.Spacing.Medium, Seq(fig))
        nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
        nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
        nodeFig.setOpaque(true)
        nodeFig.setSize(nodeFig.getPreferredSize())
        // nodeFig.setToolTip(newSymbolProgressContentFig(n, true, ParseNodeFigureGenerator.cleanestConfiguration))

        new CGraphNode(graphView, SWT.NONE, nodeFig) //new GraphNode(graph, SWT.NONE, n.toShortString)
    }

    def addGraph(graph: NewParser#Graph): Unit = {
        val (g, ap) = (figureGenerator, figureAppearances)

        graph.nodes foreach { node =>
            if (!(nodesMap contains node)) {
                val fig = node match {
                    case NewParser.TermNode(kernel) =>
                        symbolProgressFigureGenerator.kernelFig(kernel)
                    case NewParser.AtomicNode(kernel, gen, _, _) =>
                        g.horizontalFig(Spacing.Big, Seq(
                            symbolProgressFigureGenerator.kernelFig(kernel),
                            g.textFig(s"$gen", ap.default)))
                    case NewParser.NonAtomicNode(kernel, gen, _) =>
                        g.horizontalFig(Spacing.Big, Seq(
                            symbolProgressFigureGenerator.kernelFig(kernel),
                            g.textFig(s"$gen", ap.default)))
                }
                fig.setBorder(new MarginBorder(1, 2, 1, 2))

                val n = nodeFromFigure(fig)
                n.setData(node)
                nodesMap(node) = n
            }
        }

        graph.edges foreach { edge =>
            if (!(edgesMap contains edge)) {
                edge match {
                    case NewParser.SimpleEdge(start, end, revertTriggers) =>
                        edgesMap(edge) = Seq(
                            new GraphConnection(graphView, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(end)))
                    case NewParser.JoinEdge(start, end, join) =>
                        edgesMap(edge) = Seq(
                            new GraphConnection(graphView, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(end)),
                            new GraphConnection(graphView, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(join)))
                }
            }
        }
    }

    def nodesAt(ex: Int, ey: Int): Seq[NewParser.Node] = {
        import scala.collection.JavaConversions._

        val (x, y) = (ex + graphView.getHorizontalBar().getSelection(), ey + graphView.getVerticalBar().getSelection())
        println(graphView.getFigureAt(x, y))

        val selectedNodeData = graphView.getNodes.toList collect {
            case n: GraphNode if n != null && n.getNodeFigure() != null && n.getNodeFigure().containsPoint(x, y) => n.getData
        }
        selectedNodeData collect { case node: NewParser.Node => node }
    }

    def initializeDerivationGraphViewer(): Unit = {
        graphView.addMouseListener(new MouseAdapter() {
            override def mouseDoubleClick(e: MouseEvent): Unit = {
                nodesAt(e.x, e.y) foreach {
                    case node: NewParser.NontermNode[Nonterm] =>
                        val dgraph = DerivationGraph.deriveFromKernel(grammar, node.kernel)
                        val shell = new Shell(Display.getCurrent())
                        shell.setLayout(new FillLayout())
                        new DerivationGraphVisualizeWidget(shell, dgraph)
                        shell.setText(s"Derivation Graph of ${node.kernel.toShortString}")
                        shell.open()
                    case _ => // nothing to do
                }
            }
        })
    }
}

class NewParsingContextGraphVisualizeWidget(parent: Composite, style: Int, val grammar: Grammar, context: NewParser#ParsingContext) extends Composite(parent, style) with BasicGenerators with NewParserGraphVisualizeWidget {
    setLayout(new FillLayout)
    val graphView = new Graph(this, SWT.NONE)

    def initialize(): Unit = {
        addGraph(context.graph)

        context.derivables foreach { node =>
            nodesMap(node).setBackgroundColor(ColorConstants.yellow)
        }
        context.results foreach { result =>
            val resultNode = nodeFromFigure(parseNodeFigureGenerator.parseNodeHFig(result))
            if (context.graph.nodes contains context.startNode) {
                val connection = new GraphConnection(graphView, ZestStyles.CONNECTIONS_SOLID, nodesMap(context.startNode), resultNode)
                connection.setLineColor(ColorConstants.blue)
            }
        }

        import org.eclipse.zest.layouts.algorithms._
        val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
        graphView.setLayoutAlgorithm(layoutAlgorithm, true)
    }

    initialize()
    initializeDerivationGraphViewer()
}

class NewParserExpandedGraphVisualizeWidget(parent: Composite, style: Int, val grammar: Grammar, baseContext: NewParser#ParsingContext, proceed: NewParser#ProceedDetail) extends Composite(parent, style) with BasicGenerators with NewParserGraphVisualizeWidget {
    setLayout(new FillLayout)
    val graphView = new Graph(this, SWT.NONE)

    def initialize(): Unit = {
        addGraph(proceed.expandedGraph)

        import org.eclipse.zest.layouts.algorithms._
        val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
        graphView.setLayoutAlgorithm(layoutAlgorithm, true)
    }

    initialize()
    initializeDerivationGraphViewer()
}

class NewParserPreLiftGraphVisualizeWidget(parent: Composite, style: Int, val grammar: Grammar, baseContext: NewParser#ParsingContext, proceed: NewParser#ProceedDetail) extends Composite(parent, style) with BasicGenerators with NewParserGraphVisualizeWidget {
    setLayout(new FillLayout)
    val graphView = new Graph(this, SWT.NONE)

    def initialize(): Unit = {
        addGraph(proceed.expandedGraph)
        addGraph(proceed.liftedGraph0)
        // expandedGraph에서 liftedGraph0로 오면서 사라진 노드들 표시
        (proceed.expandedGraph.nodes -- proceed.liftedGraph0.nodes) foreach { removedNode =>
            nodesMap(removedNode).getFigure.setBorder(new LineBorder(ColorConstants.red))
        }
        // expandedGraph에서 liftedGraph0로 오면서 사라진 엣지들 표시
        (proceed.expandedGraph.edges -- proceed.liftedGraph0.edges) foreach { removedEdge =>
            edgesMap(removedEdge) foreach { _.setLineColor(ColorConstants.red) }
        }
        // 사용 가능한 term node 배경 노랗게 표시
        proceed.eligibleTermNodes0 foreach { node =>
            nodesMap(node).setBackgroundColor(ColorConstants.orange)
        }
        proceed.nextDerivables0 foreach { node =>
            nodesMap(node).setBackgroundColor(ColorConstants.yellow)
        }
        proceed.lifts0 foreach { lift =>
            val parseNode = nodeFromFigure(parseNodeFigureGenerator.parseNodeHFig(lift.parsed))
            val connection = new GraphConnection(graphView, ZestStyles.CONNECTIONS_SOLID, nodesMap(lift.before), parseNode)
            connection.setLineColor(ColorConstants.blue)
            if (lift.after.isDefined) {
                val liftConnection = new GraphConnection(graphView, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(lift.before), nodesMap(lift.after.get))
                liftConnection.setLineColor(ColorConstants.cyan)
            }
            println(lift)
        }

        import org.eclipse.zest.layouts.algorithms._
        val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
        graphView.setLayoutAlgorithm(layoutAlgorithm, true)
    }

    initialize()
    initializeDerivationGraphViewer()
}
