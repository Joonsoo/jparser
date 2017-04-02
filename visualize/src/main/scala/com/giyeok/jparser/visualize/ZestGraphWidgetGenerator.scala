package com.giyeok.jparser.visualize

import scala.collection.mutable
import scala.util.Try
import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.ParseResult
import com.giyeok.jparser.ParseResultDerivationsSetFunc
import com.giyeok.jparser.ParseResultFunc
import com.giyeok.jparser.ParseResultGraphFunc
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.nparser.Parser.Context
import com.giyeok.jparser.nparser.Parser.DeriveTipsContext
import com.giyeok.jparser.nparser.ParsingContext
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.visualize.FigureGenerator.Spacing
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.CompoundBorder
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.LineBorder
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.MouseListener
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.zest.core.viewers.GraphViewer
import org.eclipse.zest.core.widgets.CGraphNode
import org.eclipse.zest.core.widgets.Graph
import org.eclipse.zest.core.widgets.GraphConnection
import org.eclipse.zest.core.widgets.ZestStyles
import org.eclipse.zest.layouts.LayoutStyles

trait AbstractZestGraphWidget extends Control {
    val graphViewer: GraphViewer
    val graphCtrl: Graph
    val fig: NodeFigureGenerators[Figure]

    val nodesMap: mutable.Map[Node, CGraphNode] = scala.collection.mutable.Map[Node, CGraphNode]()
    val edgesMap: mutable.Map[Edge, Seq[GraphConnection]] = scala.collection.mutable.Map[Edge, Seq[GraphConnection]]()
    var visibleNodes: Set[Node] = Set[Node]()
    var visibleEdges: Set[Edge] = Set[Edge]()

    def addNode(grammar: NGrammar, node: Node): Unit = {
        if (!(nodesMap contains node)) {
            val nodeFig = fig.nodeFig(grammar, node)
            nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
            nodeFig.setOpaque(true)
            nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
            nodeFig.setSize(nodeFig.getPreferredSize())

            val gnode = new CGraphNode(graphCtrl, SWT.NONE, nodeFig)
            gnode.setData(node)

            nodesMap(node) = gnode
            visibleNodes += node
        }
    }

    val edgeColor: Color = ColorConstants.gray
    def addEdge(edge: Edge): Unit = {
        if (!(edgesMap contains edge)) {
            edge match {
                case SimpleEdge(start, end) =>
                    assert(nodesMap contains start)
                    assert(nodesMap contains end)
                    val conn = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(end))
                    conn.setLineColor(edgeColor)
                    edgesMap(edge) = Seq(conn)
                case JoinEdge(start, end, join) =>
                    assert(nodesMap contains start)
                    assert(nodesMap contains end)
                    assert(nodesMap contains join)
                    val conn = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(end))
                    val connJoin = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(start), nodesMap(join))
                    conn.setText("main")
                    connJoin.setText("join")
                    conn.setLineColor(edgeColor)
                    connJoin.setLineColor(edgeColor)
                    edgesMap(edge) = Seq(conn, connJoin)
            }
            visibleEdges += edge
        }
    }

    def addGraph(grammar: NGrammar, graph: ParsingContext.Graph): Unit = {
        graph.nodes foreach { node =>
            addNode(grammar, node)
        }
        graph.edges foreach { edge =>
            addEdge(edge)
        }
    }

    def applyLayout(animation: Boolean): Unit = {
        if (animation) {
            graphViewer.setNodeStyle(ZestStyles.NONE)
        } else {
            graphViewer.setNodeStyle(ZestStyles.NODES_NO_LAYOUT_ANIMATION)
        }
        import org.eclipse.zest.layouts.algorithms._
        val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
        graphCtrl.setLayoutAlgorithm(layoutAlgorithm, true)
    }

    def setVisibleSubgraph(nodes: Set[Node], edges: Set[Edge]): Unit = {
        visibleNodes = nodes
        visibleEdges = edges

        nodesMap foreach { kv =>
            kv._2.setVisible(nodes contains kv._1)
        }
        edgesMap foreach { kv =>
            val visible = edges contains kv._1
            kv._2 foreach { _.setVisible(visible) }
        }
    }
}

trait Highlightable extends AbstractZestGraphWidget {
    val blinkInterval = 100
    val blinkCycle = 10
    val highlightedNodes = scala.collection.mutable.Map[Node, (Long, Int)]()

    var _uniqueId = 0L
    def uniqueId: Long = { _uniqueId += 1; _uniqueId }

    def highlightNode(node: Node): Unit = {
        val display = getDisplay
        if (!(highlightedNodes contains node) && (visibleNodes contains node)) {
            nodesMap(node).highlight()
            val highlightId = uniqueId
            highlightedNodes(node) = (highlightId, 0)
            display.timerExec(100, new Runnable() {
                def run(): Unit = {
                    highlightedNodes get node match {
                        case Some((`highlightId`, count)) =>
                            val shownNode = nodesMap(node)
                            if (count < blinkCycle) {
                                shownNode.setVisible(count % 2 == 0)
                                highlightedNodes(node) = (highlightId, count + 1)
                                display.timerExec(blinkInterval, this)
                            } else {
                                shownNode.setVisible(true)
                            }
                        case _ => // nothing to do
                    }
                }
            })
        }
    }
    def unhighlightAllNodes(): Unit = {
        highlightedNodes.keys foreach { node =>
            val shownNode = nodesMap(node)
            shownNode.unhighlight()
            shownNode.setVisible(visibleNodes contains node)
        }
        highlightedNodes.clear()
    }
}

trait Interactable extends AbstractZestGraphWidget {
    def nodesAt(ex: Int, ey: Int): Seq[Any] = {
        import scala.collection.JavaConverters._

        val (x, y) = (ex + graphCtrl.getHorizontalBar.getSelection, ey + graphCtrl.getVerticalBar.getSelection)

        graphCtrl.getNodes.asScala collect {
            case n: CGraphNode if n != null && n.getNodeFigure != null && n.getNodeFigure.containsPoint(x, y) && n.getData() != null =>
                println(n)
                n.getData
        }
    }
}

class ZestGraphWidget(parent: Composite, style: Int, val fig: NodeFigureGenerators[Figure], val grammar: NGrammar, graph: ParsingContext.Graph)
        extends Composite(parent, style) with AbstractZestGraphWidget with Highlightable with Interactable {
    val graphViewer = new GraphViewer(this, style)
    val graphCtrl: Graph = graphViewer.getGraphControl

    setLayout(new FillLayout())

    def initialize(): Unit = {
        addGraph(grammar, graph)
        applyLayout(false)
        initializeTooltips(getTooltips)
    }
    initialize()

    def getTooltips: Map[Node, Seq[Figure]] = {
        var tooltips = Map[Node, Seq[Figure]]()
        //        // finish, progress 조건 툴팁으로 표시
        //        context.finishes.nodeConditions foreach { kv =>
        //            val (node, conditions) = kv
        //            if (!(nodesMap contains node)) {
        //                addNode(grammar, node)
        //            }
        //
        //            nodesMap(node).setBackgroundColor(ColorConstants.yellow)
        //
        //            val conditionsFig = conditions.toSeq map { fig.conditionFig(grammar, _) }
        //            tooltips += node -> (tooltips.getOrElse(node, Seq()) :+ fig.fig.verticalFig(Spacing.Medium, fig.fig.textFig("Finishes", fig.appear.default) +: conditionsFig))
        //        }
        //        context.progresses.nodeConditions foreach { kv =>
        //            val (node, conditions) = kv
        //
        //            val conditionsFig = conditions.toSeq map { fig.conditionFig(grammar, _) }
        //            tooltips += node -> (tooltips.getOrElse(node, Seq()) :+ fig.fig.verticalFig(Spacing.Medium, fig.fig.textFig("Progresses", fig.appear.default) +: conditionsFig))
        //        }
        tooltips
    }
    protected def initializeTooltips(tooltips: Map[Node, Seq[Figure]]): Unit = {
        tooltips foreach { kv =>
            val (node, figs) = kv
            val tooltipFig = fig.fig.verticalFig(Spacing.Big, figs)
            tooltipFig.setOpaque(true)
            tooltipFig.setBackgroundColor(ColorConstants.white)
            if (nodesMap contains node) nodesMap(node).getFigure.setToolTip(tooltipFig)
        }
    }

    // Interactions
    override def addKeyListener(keyListener: KeyListener): Unit = graphCtrl.addKeyListener(keyListener)
    override def addMouseListener(mouseListener: MouseListener): Unit = graphCtrl.addMouseListener(mouseListener)

    val inputMaxInterval = 2000
    case class InputAccumulator(textSoFar: String, lastTime: Long) {
        def accumulate(char: Char, thisTime: Long): InputAccumulator =
            if (thisTime - lastTime < inputMaxInterval) InputAccumulator(textSoFar + char, thisTime) else InputAccumulator("" + char, thisTime)
        def textAsInt: Option[Int] = try { Some(textSoFar.toInt) } catch { case _: Throwable => None }
        def textAsInts: Seq[Int] = {
            val tokens = textSoFar.split("\\D+") map { t => Try(t.toInt) }
            if (tokens forall { _.isSuccess }) tokens map { _.get } else Seq()
        }
    }
    var inputAccumulator = InputAccumulator("", 0)

    addKeyListener(new KeyListener() {
        def keyPressed(e: org.eclipse.swt.events.KeyEvent): Unit = {
            e.keyCode match {
                case 'R' | 'r' =>
                    applyLayout(true)
                case 'S' | 's' =>
                    // Stat
                    println(s"Nodes: ${graph.nodes.size} Edges: ${graph.edges.size}")
                case c if ('0' <= c && c <= '9') || (c == ' ' || c == ',' || c == '.') =>
                    unhighlightAllNodes()
                    inputAccumulator = inputAccumulator.accumulate(c.toChar, System.currentTimeMillis())
                    val textAsInts = inputAccumulator.textAsInts
                    println(textAsInts)
                    textAsInts match {
                        case Seq(symbolId) =>
                            nodesMap.keySet filter { node => node.kernel.symbolId == symbolId } foreach { highlightNode }
                        case Seq(symbolId, beginGen) =>
                            nodesMap.keySet filter { node => node.kernel.symbolId == symbolId && node.kernel.beginGen == beginGen } foreach { highlightNode }
                        case Seq(symbolId, pointer, beginGen, endGen) =>
                            nodesMap.keySet find { node =>
                                val k = node.kernel
                                k.symbolId == symbolId && k.pointer == pointer && k.beginGen == beginGen && k.endGen == endGen
                            } foreach { highlightNode }
                        case _ => // nothing to do
                    }
                case SWT.ESC =>
                    inputAccumulator = InputAccumulator("", 0)
                case _ =>
            }
        }
        def keyReleased(e: org.eclipse.swt.events.KeyEvent): Unit = {}
    })
}

trait ZestParseTreeConstructorView {
    val fig: NodeFigureGenerators[Figure]
    val grammar: NGrammar
    val context: Context

    def openParseTree[R <: ParseResult](resultFunc: ParseResultFunc[R])(opener: R => Unit)(gen: Int, kernel: Kernel): Unit = {
        val parseResultOpt = new ParseTreeConstructor(resultFunc)(grammar)(context.inputs, context.history, context.conditionAccumulate).reconstruct(kernel, context.gen)
        parseResultOpt match {
            case Some(parseResult) =>
                opener(parseResult)
            case None =>
        }
    }
    def parseTreeOpener(stateMask: Int): (Int, Kernel) => Unit = {
        val shiftPressed = (stateMask & SWT.SHIFT) != 0
        val ctrlPressed = (stateMask & SWT.CTRL) != 0
        (shiftPressed, ctrlPressed) match {
            case (true, _) =>
                // Shift + 노드 클릭 -> Parse Forest
                openParseTree[ParseForest](ParseForestFunc) { forest =>
                    forest.trees foreach { new ParseResultTreeViewer(_, fig.fig, fig.appear).start() }
                }
            case (false, true) =>
                // Ctrl + 노드 클릭 -> Derivation Set
                openParseTree(ParseResultDerivationsSetFunc) { derivationSet =>
                    new ParseResultDerivationsSetViewer(derivationSet, fig.fig, fig.appear).start()
                }
            case (false, false) =>
                openParseTree(ParseResultGraphFunc) { graph =>
                    new ParseResultGraphViewer(graph, fig.fig, fig.appear, fig.symbol).start()
                }
        }
    }
}

class ZestGraphTransitionWidget(parent: Composite, style: Int, fig: NodeFigureGenerators[Figure], grammar: NGrammar, baseGraph: ParsingContext.Graph, nextGraph: ParsingContext.Graph, val context: Context)
        extends ZestGraphWidget(parent, style, fig, grammar, context.nextGraph)
        with ZestParseTreeConstructorView {

    override def initialize(): Unit = {
        addGraph(grammar, baseGraph)
        addGraph(grammar, nextGraph)
        val thickLineWidth = 2
        // 변경이 있는 부분은 굵은 선
        // 없어지는 노드, 엣지는 주황색 점선
        (baseGraph.nodes -- nextGraph.nodes) foreach { removedNode =>
            val shownNode = nodesMap(removedNode)
            shownNode.getFigure.setBorder(new LineBorder(ColorConstants.orange, thickLineWidth, SWT.LINE_DASH))
            val preferredSize = shownNode.getFigure.getPreferredSize()
            shownNode.setSize(preferredSize.width, preferredSize.height)
        }
        (baseGraph.edges -- nextGraph.edges) foreach { removedEdge =>
            edgesMap(removedEdge) foreach { connection =>
                connection.setLineColor(ColorConstants.orange)
                connection.setLineWidth(thickLineWidth)
                connection.setLineStyle(SWT.LINE_DASH)
            }
        }
        // 새 노드, 엣지는 굵은 파란 실선
        (nextGraph.nodes -- baseGraph.nodes) foreach { newNode =>
            val shownNode = nodesMap(newNode)
            shownNode.getFigure.setBorder(new LineBorder(ColorConstants.blue, thickLineWidth))
            val preferredSize = shownNode.getFigure.getPreferredSize()
            shownNode.setSize(preferredSize.width, preferredSize.height)
        }
        (nextGraph.edges -- baseGraph.edges) foreach { newEdge =>
            edgesMap(newEdge) foreach { connection =>
                connection.setLineColor(ColorConstants.blue)
                connection.setLineWidth(thickLineWidth)
            }
        }
        // 공통 노드, 엣지는 기본 모양
        applyLayout(false)
        initializeTooltips(getTooltips)
    }

    private def setVisible(graph: ParsingContext.Graph, visible: Boolean): Unit = {
        graph.nodes foreach { node =>
            (nodesMap get node) foreach { shownNode =>
                shownNode.setVisible(visible)
            }
        }
        graph.edges foreach { edge =>
            (edgesMap get edge) foreach {
                _ foreach { connection =>
                    if (visible) {
                        connection.changeLineColor(edgeColor)
                    } else {
                        connection.changeLineColor(ColorConstants.white)
                    }
                }
            }
        }
    }

    addKeyListener(new KeyListener() {
        def keyPressed(e: org.eclipse.swt.events.KeyEvent): Unit = {
            e.keyCode match {
                case 'Q' | 'q' =>
                    // Show base graph only
                    setVisible(nextGraph, visible = false)
                    setVisible(baseGraph, visible = true)
                case 'W' | 'w' =>
                    // Show trans graph only
                    setVisible(baseGraph, visible = false)
                    setVisible(nextGraph, visible = true)
                case 'E' | 'e' =>
                    // Show both graphs
                    setVisible(baseGraph, visible = true)
                    setVisible(nextGraph, visible = true)
                case _ =>
            }
        }
        def keyReleased(e: org.eclipse.swt.events.KeyEvent): Unit = {}
    })

    addMouseListener(new MouseListener() {
        def mouseDown(e: org.eclipse.swt.events.MouseEvent): Unit = {}
        def mouseUp(e: org.eclipse.swt.events.MouseEvent): Unit = {}
        def mouseDoubleClick(e: org.eclipse.swt.events.MouseEvent): Unit = {
            nodesAt(e.x, e.y) foreach {
                case node: Node =>
                    parseTreeOpener(e.stateMask)(context.gen, node.kernel)
                case data =>
                    println(data)
            }
        }
    })
}

class ZestParsingContextWidget(parent: Composite, style: Int, fig: NodeFigureGenerators[Figure], grammar: NGrammar, val context: Context)
        extends ZestGraphWidget(parent, style, fig, grammar, context.nextGraph)
        with ZestParseTreeConstructorView {
    addKeyListener(new KeyListener() {
        def keyPressed(e: org.eclipse.swt.events.KeyEvent): Unit = {
            e.keyCode match {
                case 'T' | 't' =>
                    context.conditionAccumulate.unfixed foreach { kv =>
                        println(s"${kv._1} -> ${kv._2}")
                    }
                case _ =>
            }
        }
        def keyReleased(e: org.eclipse.swt.events.KeyEvent): Unit = {}
    })

    addMouseListener(new MouseListener() {
        def mouseDown(e: org.eclipse.swt.events.MouseEvent): Unit = {}
        def mouseUp(e: org.eclipse.swt.events.MouseEvent): Unit = {}
        def mouseDoubleClick(e: org.eclipse.swt.events.MouseEvent): Unit = {
            nodesAt(e.x, e.y) foreach {
                case node: Node =>
                    parseTreeOpener(e.stateMask)(context.gen, node.kernel)
                case data =>
                    println(data)
            }
        }
    })
}

trait TipNodes extends AbstractZestGraphWidget {
    def setTipNodeBorder(node: Node): Unit = {
        val shownNode = nodesMap(node)
        shownNode.getFigure.setBorder(new CompoundBorder(shownNode.getFigure.getBorder, new LineBorder(ColorConstants.orange, 3)))
        val size = shownNode.getFigure.getPreferredSize()
        shownNode.setSize(size.width, size.height)
    }
}

class ZestDeriveTipParsingContextWidget(parent: Composite, style: Int, fig: NodeFigureGenerators[Figure], grammar: NGrammar, context: DeriveTipsContext)
        extends ZestParsingContextWidget(parent, style, fig, grammar, context) with TipNodes {
    override def initialize(): Unit = {
        super.initialize()
        context.deriveTips foreach { deriveTip =>
            if (!(nodesMap contains deriveTip)) {
                println(s"Error! $deriveTip @ ${context.gen}")
            } else {
                setTipNodeBorder(deriveTip)
            }
        }
    }
}
