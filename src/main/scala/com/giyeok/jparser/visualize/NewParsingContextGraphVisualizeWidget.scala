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
import com.giyeok.jparser.DGraph
import org.eclipse.swt.widgets.Display
import com.giyeok.jparser.Symbols.Nonterm
import com.giyeok.jparser.Symbols.AtomicSymbol
import org.eclipse.zest.core.widgets.GraphNode
import org.eclipse.swt.events.MouseAdapter
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.Symbols.Symbol
import com.giyeok.jparser.NewParser
import com.giyeok.jparser.ParseResult
import com.giyeok.jparser.CtxGraph
import com.giyeok.jparser.ParsingGraph
import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.DerivationFunc
import com.giyeok.jparser.ParsingGraph.AtomicNode
import com.giyeok.jparser.ParseResultTree
import com.giyeok.jparser.Symbols.Sequence
import com.giyeok.jparser.Symbols.Empty
import com.giyeok.jparser.Symbols.AtomicNonterm
import org.eclipse.draw2d.ToolbarLayout
import com.giyeok.jparser.Results
import org.eclipse.swt.graphics.Color

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
    val symbolFigureGenerator = new SymbolFigureGenerator(figureGenerator, figureAppearances)
    val parseNodeFigureGenerator = new ParseResultTreeFigureGenerator(figureGenerator, tooltipAppearances)
}

trait KernelFigureGenerator[Fig] {
    val figureGenerator: FigureGenerator.Generator[Fig]
    val figureAppearances: FigureGenerator.Appearances[Fig]
    val symbolFigureGenerator: SymbolFigureGenerator[Fig]

    def dot = figureGenerator.textFig("\u2022", figureAppearances.kernelDot)
    def atomicFigure(symbol: AtomicSymbol) = {
        symbolFigureGenerator.symbolFig(symbol)
        // figureGenerator.horizontalFig(Spacing.Small, Seq(dot, symbolFigureGenerator.symbolFig(symbol)))
    }
    def sequenceFigure(symbol: Sequence, pointer: Int) = {
        val (p, f) = symbol.seq.splitAt(pointer)
        val f0 = figureGenerator.horizontalFig(Spacing.Medium, p map { symbolFigureGenerator.symbolFig _ })
        val f1 = figureGenerator.horizontalFig(Spacing.Medium, f map { symbolFigureGenerator.symbolFig _ })
        figureGenerator.horizontalFig(Spacing.Small, Seq(f0, dot, f1))
    }
}

class NodeIdCache {
    private var counter = 0
    private val nodeIdMap = scala.collection.mutable.Map[ParsingGraph.Node, Int]()

    def of(node: ParsingGraph.Node): Int = {
        nodeIdMap get node match {
            case Some(id) => id
            case None =>
                counter += 1
                nodeIdMap(node) = counter
                counter
        }
    }
}

trait NewParserGraphVisualizeWidget extends KernelFigureGenerator[Figure] {
    val graphView: Graph
    val grammar: Grammar
    val nodeIdCache: NodeIdCache

    val figureGenerator: FigureGenerator.Generator[Figure]
    val figureAppearances: FigureGenerator.Appearances[Figure]
    val symbolFigureGenerator: SymbolFigureGenerator[Figure]
    val parseNodeFigureGenerator: ParseResultTreeFigureGenerator[Figure]

    val nodesMap = scala.collection.mutable.Map[ParsingGraph.Node, CGraphNode]()
    val edgesMap = scala.collection.mutable.Map[ParsingGraph.Edge, Seq[GraphConnection]]()

    def nodeFromFigure(fig: Figure): CGraphNode = {
        val nodeFig = figureGenerator.horizontalFig(FigureGenerator.Spacing.Medium, Seq(fig))
        nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
        nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
        nodeFig.setOpaque(true)
        nodeFig.setSize(nodeFig.getPreferredSize())

        new CGraphNode(graphView, SWT.NONE, nodeFig)
    }

    def nodeOf(node: ParsingGraph.Node): CGraphNode = {
        val nodeId = nodeIdCache.of(node)

        val (g, ap) = (figureGenerator, figureAppearances)
        val fig = node match {
            case ParsingGraph.EmptyNode =>
                g.horizontalFig(Spacing.Big, Seq(
                    g.supFig(g.textFig(s"$nodeId", ap.default)),
                    g.horizontalFig(Spacing.Small, Seq(symbolFigureGenerator.symbolFig(Empty), dot))))
            case ParsingGraph.TermNode(symbol) =>
                g.horizontalFig(Spacing.Big, Seq(
                    g.supFig(g.textFig(s"$nodeId", ap.default)),
                    g.horizontalFig(Spacing.Small, Seq(dot, symbolFigureGenerator.symbolFig(symbol)))))
            case ParsingGraph.AtomicNode(symbol, beginGen) =>
                g.horizontalFig(Spacing.Big, Seq(
                    g.supFig(g.textFig(s"$nodeId", ap.default)),
                    atomicFigure(symbol),
                    g.textFig(s"$beginGen", ap.default)))
            case ParsingGraph.SequenceNode(symbol, pointer, beginGen, endGen) =>
                val f = g.horizontalFig(Spacing.Big, Seq(
                    g.supFig(g.textFig(s"$nodeId", ap.default)),
                    sequenceFigure(symbol, pointer),
                    g.textFig(s"$beginGen-$endGen", ap.default)))
                // TODO progresses 표시
                f
        }
        if (node.isInstanceOf[DGraph.BaseNode]) {
            fig.setOpaque(true)
            fig.setBackgroundColor(ColorConstants.yellow)
            fig
        }
        fig.setBorder(new MarginBorder(1, 2, 1, 2))

        val n = nodeFromFigure(fig)
        n.setData(node)
        n
    }

    def edgeOf(edge: ParsingGraph.Edge): Seq[GraphConnection] = {
        edge match {
            case ParsingGraph.SimpleEdge(start, end, revertTriggers) =>
                val conn = new GraphConnection(graphView, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(end))
                if (!(revertTriggers.isEmpty)) {
                    conn.setText(revertTriggersString(revertTriggers))
                }
                Seq(conn)
            case ParsingGraph.JoinEdge(start, end, join) =>
                val conn = new GraphConnection(graphView, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(end))
                val connJoin = new GraphConnection(graphView, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(join))
                conn.setText("main")
                connJoin.setText("join")
                Seq(conn, connJoin)
        }
    }

    def revertTriggersString(revertTriggers: Set[ParsingGraph.Trigger]): String =
        revertTriggers map {
            case ParsingGraph.Trigger(node, ttype) =>
                s"$ttype(${nodeIdCache.of(node)})"
        } mkString " or "

    def addGraph(graph: ParsingGraph[ParseForest]): Unit = {
        graph.nodes foreach { node =>
            if (!(nodesMap contains node)) {
                nodesMap(node) = nodeOf(node)
            }
        }

        graph.edges foreach { edge =>
            if (!(edgesMap contains edge)) {
                edgesMap(edge) = edgeOf(edge)
            }
        }
    }

    def nodesAt(ex: Int, ey: Int): Seq[Any] = {
        import scala.collection.JavaConversions._

        val (x, y) = (ex + graphView.getHorizontalBar().getSelection(), ey + graphView.getVerticalBar().getSelection())

        graphView.getNodes.toSeq collect {
            case n: CGraphNode if n != null && n.getNodeFigure() != null && n.getNodeFigure().containsPoint(x, y) =>
                n.getData
        }
    }

    def initializeListeners(): Unit = {
        graphView.addMouseListener(new MouseAdapter() {
            override def mouseDoubleClick(e: MouseEvent): Unit = {
                val nodes = nodesAt(e.x, e.y)
                nodes foreach { n => println(s"  -> $n") }
                nodes foreach {
                    case node: ParsingGraph.NontermNode =>
                        val derivationFunc = new DerivationFunc(grammar, ParseForestFunc)
                        val dgraph = node match {
                            case node: ParsingGraph.AtomicNode =>
                                derivationFunc.deriveAtomic(node.symbol)
                            case node: ParsingGraph.SequenceNode =>
                                derivationFunc.deriveSequence(node.symbol, node.pointer)
                        }

                        val shell = new Shell(Display.getCurrent())
                        shell.setLayout(new FillLayout())
                        new DerivationGraphVisualizeWidget(shell, SWT.NONE, grammar, new NodeIdCache(), dgraph)
                        shell.setText(s"Derivation Graph of $node")
                        shell.open()
                    case node: ParseResultTree.Node =>
                        new ParseResultTreeViewer(node.asInstanceOf[ParseResultTree.Node], figureGenerator, figureAppearances, parseNodeFigureGenerator).start()
                    case _ => // nothing to do
                }
            }
        })

        graphView.addKeyListener(new KeyAdapter() {
            override def keyPressed(e: KeyEvent): Unit = {
                e.keyCode match {
                    case 'r' | 'R' =>
                        graphView.applyLayout()
                    case code =>
                        println(code)
                }
            }
        })
    }
}

class DerivationGraphVisualizeWidget(parent: Composite, style: Int, val grammar: Grammar, val nodeIdCache: NodeIdCache, val dgraph: DGraph[ParseForest]) extends Composite(parent, style) with BasicGenerators with NewParserGraphVisualizeWidget {
    setLayout(new FillLayout)
    val graphView = new Graph(this, SWT.NONE)

    def initialize(): Unit = {
        addGraph(dgraph)

        def addResults[N <: ParsingGraph.Node](results: Results[N, ParseForest], bind: Boolean, lineColor: Color): Unit = {
            results.asMap foreach { result =>
                val (node, matches) = result
                matches foreach { m =>
                    val (triggers, result) = m
                    val forestFigure = new Figure()
                    forestFigure.setLayoutManager({
                        val l = new ToolbarLayout(false)
                        l.setSpacing(3)
                        l
                    })
                    val trees = if (bind) ParseForestFunc.bind(node.symbol, result).trees else result.trees
                    trees foreach { tree =>
                        val treeFigure = parseNodeFigureGenerator.parseNodeHFig(tree)
                        treeFigure.setBorder(new LineBorder(1))
                        forestFigure.add(treeFigure)
                    }
                    val forestNode = nodeFromFigure(forestFigure)
                    forestNode.setData(result)
                    val connection = new GraphConnection(graphView, ZestStyles.CONNECTIONS_SOLID, nodesMap(node), forestNode)
                    connection.setLineColor(lineColor)
                }
            }
        }
        addResults(dgraph.results, true, ColorConstants.blue)
        addResults(dgraph.progresses, false, ColorConstants.red)

        import org.eclipse.zest.layouts.algorithms._
        val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
        graphView.setLayoutAlgorithm(layoutAlgorithm, true)
    }

    initialize()
}

class NewParsingContextGraphVisualizeWidget(parent: Composite, style: Int, val grammar: Grammar, val nodeIdCache: NodeIdCache, context: NewParser[ParseForest]#ParsingCtx) extends Composite(parent, style) with BasicGenerators with NewParserGraphVisualizeWidget {
    setLayout(new FillLayout)
    val graphView = new Graph(this, SWT.NONE)

    def initialize(): Unit = {
        addGraph(context.graph)

        context.derivables foreach { node =>
            nodesMap(node).setBackgroundColor(ColorConstants.yellow)
        }
        context.result match {
            case Some(forest) =>
                forest.trees foreach { result =>
                    val resultNode = nodeFromFigure(parseNodeFigureGenerator.parseNodeHFig(result))
                    resultNode.setData(result)
                    if (context.graph.nodes contains context.startNode) {
                        val connection = new GraphConnection(graphView, ZestStyles.CONNECTIONS_SOLID, nodesMap(context.startNode), resultNode)
                        connection.setLineColor(ColorConstants.blue)
                    }
                }
            case None =>
        }

        import org.eclipse.zest.layouts.algorithms._
        val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
        graphView.setLayoutAlgorithm(layoutAlgorithm, true)
    }

    initialize()
    initializeListeners()
}

class NewParserExpandedGraphVisualizeWidget(parent: Composite, style: Int, val grammar: Grammar, val nodeIdCache: NodeIdCache, baseContext: NewParser[ParseForest]#ParsingCtx, proceed: NewParser[ParseForest]#ProceedDetail) extends Composite(parent, style) with BasicGenerators with NewParserGraphVisualizeWidget {
    setLayout(new FillLayout)
    val graphView = new Graph(this, SWT.NONE)

    def initialize(): Unit = {
        addGraph(proceed.expandedGraph)

        import org.eclipse.zest.layouts.algorithms._
        val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
        graphView.setLayoutAlgorithm(layoutAlgorithm, true)
    }

    initialize()
    initializeListeners()
}

class NewParserPreLiftGraphVisualizeWidget(parent: Composite, style: Int, val grammar: Grammar, val nodeIdCache: NodeIdCache, baseContext: NewParser[ParseForest]#ParsingCtx, proceed: NewParser[ParseForest]#ProceedDetail) extends Composite(parent, style) with BasicGenerators with NewParserGraphVisualizeWidget {
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
        /*
        proceed.lifts0 foreach { lift =>
            val parseNode = nodeFromFigure(parseNodeFigureGenerator.parseNodeHFig(lift.parsed))
            parseNode.setData("Hello")
            val connection = new GraphConnection(graphView, ZestStyles.CONNECTIONS_SOLID, nodesMap(lift.before), parseNode)
            if (!(lift.revertTriggers.isEmpty)) {
                connection.setText(revertTriggersString(lift.revertTriggers))
            }
            connection.setLineColor(ColorConstants.blue)
            if (lift.after.isDefined) {
                val liftConnection = new GraphConnection(graphView, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(lift.before), nodesMap(lift.after.get))
                liftConnection.setLineColor(ColorConstants.cyan)
                val liftToAfterConnection = new GraphConnection(graphView, ZestStyles.CONNECTIONS_DIRECTED, parseNode, nodesMap(lift.after.get))
                liftToAfterConnection.setLineColor(ColorConstants.cyan)
            }
            println(lift)
        }
        */

        import org.eclipse.zest.layouts.algorithms._
        val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
        graphView.setLayoutAlgorithm(layoutAlgorithm, true)
    }

    initialize()
    initializeListeners()
}
