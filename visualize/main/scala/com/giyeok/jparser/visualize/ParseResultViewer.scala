package com.giyeok.jparser.visualize

import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.ParseResultDerivationsSet
import com.giyeok.jparser.ParseResultGraph
import com.giyeok.jparser.ParseResultTree
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.draw2d.LineBorder
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets._
import org.eclipse.zest.core.viewers.GraphViewer
import org.eclipse.zest.core.widgets.CGraphNode
import org.eclipse.zest.core.widgets.GraphConnection
import org.eclipse.zest.core.widgets.ZestStyles
import org.eclipse.zest.layouts.LayoutStyles

class ParseResultTreeViewer(forest: ParseForest, figureGenerator: FigureGenerator.Generator[Figure], figureAppearances: FigureGenerator.Appearances[Figure]) {
    val parseResultFigureGenerator = new ParseResultFigureGenerator[Figure](figureGenerator, figureAppearances)

    val shell = new Shell(Display.getDefault)
    shell.setLayout(new FillLayout())
    val figCanvas = new FigureCanvas(shell)
    shell.addListener(SWT.Close, new Listener() {
        def handleEvent(e: Event): Unit = {
            shell.dispose()
        }
    })
    // shell.setText(node.toShortString)

    class MutableRenderingStatus(var horizontal: Boolean, var renderJoin: Boolean, var renderWS: Boolean, var renderLookaheadExcept: Boolean, var unrollRepeat: Boolean)
    val rs = new MutableRenderingStatus(horizontal = true, renderJoin = true, renderWS = true, renderLookaheadExcept = true, unrollRepeat = true)
    def resetContents(): Unit = {
        val nodeFig =
            if (rs.horizontal) parseResultFigureGenerator.parseResultFigure(forest, ParseResultFigureGenerator.RenderingConfiguration(rs.renderJoin, rs.renderWS, rs.renderLookaheadExcept, rs.unrollRepeat))
            else parseResultFigureGenerator.parseResultVerticalFigure(forest, ParseResultFigureGenerator.RenderingConfiguration(rs.renderJoin, rs.renderWS, rs.renderLookaheadExcept, rs.unrollRepeat))
        figCanvas.setContents(
            figureGenerator.verticalFig(FigureGenerator.Spacing.Big, Seq(
                figureGenerator.textFig(s"${if (rs.horizontal) "Horizontal" else "Vertical"} renderJoin=${rs.renderJoin}, renderWS=${rs.renderWS}, renderLookaheadExcept=${rs.renderLookaheadExcept}, unrollRepeat=${rs.unrollRepeat}", figureAppearances.default),
                nodeFig
            ))
        )
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
            if (e.keyCode == '4'.toInt) {
                rs.unrollRepeat = !rs.unrollRepeat
            }
            resetContents()
        }
    })

    def start(): Unit = {
        shell.open()
    }
}

class ParseResultDerivationsSetViewer(r: ParseResultDerivationsSet, figureGenerator: FigureGenerator.Generator[Figure], figureAppearances: FigureGenerator.Appearances[Figure]) {
    val parseResultFigureGenerator = new ParseResultFigureGenerator[Figure](figureGenerator, figureAppearances)

    val shell = new Shell(Display.getDefault)
    shell.setLayout(new FillLayout())
    val figCanvas = new FigureCanvas(shell)
    shell.addListener(SWT.Close, new Listener() {
        def handleEvent(e: Event): Unit = {
            shell.dispose()
        }
    })
    // shell.setText(node.toShortString)

    class MutableRenderingStatus(var horizontal: Boolean, var renderJoin: Boolean, var renderWS: Boolean, var renderLookaheadExcept: Boolean)
    val rs = new MutableRenderingStatus(true, true, true, true)
    def resetContents(): Unit = {
        val renderConf = ParseResultFigureGenerator.RenderingConfiguration(rs.renderJoin, rs.renderWS, rs.renderLookaheadExcept, true)
        val resultFig = if (rs.horizontal) parseResultFigureGenerator.parseResultFigure(r, renderConf) else parseResultFigureGenerator.parseResultVerticalFigure(r, renderConf)
        figCanvas.setContents(
            figureGenerator.verticalFig(FigureGenerator.Spacing.Big, Seq(
                figureGenerator.textFig(s"${if (rs.horizontal) "Horizontal" else "Vertical"} renderJoin=${rs.renderJoin}, renderWS=${rs.renderWS}, renderLookaheadExcept=${rs.renderLookaheadExcept}", figureAppearances.default),
                resultFig
            ))
        )
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

    def start(): Unit = {
        shell.open()
    }
}

class ParseResultGraphViewer(r: ParseResultGraph, val figureGenerator: FigureGenerator.Generator[Figure], val figureAppearances: FigureGenerator.Appearances[Figure], val symbolFigureGenerator: SymbolFigureGenerator[Figure]) {
    import ParseResultGraph._
    import com.giyeok.jparser.visualize.FigureGenerator.Spacing

    val shell = new Shell(Display.getDefault)
    shell.setLayout(new FillLayout())
    shell.addListener(SWT.Close, new Listener() {
        def handleEvent(e: Event): Unit = {
            shell.dispose()
        }
    })

    val (g, ap, sfg) = (figureGenerator, figureAppearances, symbolFigureGenerator)
    val graphViewer = new GraphViewer(shell, SWT.NONE)
    val graph = graphViewer.getGraphControl
    val nodeMap = scala.collection.mutable.Map[Node, CGraphNode]()
    r.nodes foreach { node =>
        val figure = node match {
            case node @ Term(_, input) =>
                g.horizontalFig(Spacing.None, Seq(
                    g.textFig(node.range.toString, ap.small),
                    g.textFig(input.toShortString, ap.input)
                ))
            case node: Sequence =>
                g.horizontalFig(Spacing.None, Seq(
                    g.textFig(node.range.toString, ap.small),
                    sfg.symbolPointerFig(node.symbol.symbol, node.pointer)
                ))
            case node @ Bind(_, _, symbol) =>
                g.verticalFig(Spacing.None, Seq(
                    g.textFig(node.range.toString, ap.small),
                    sfg.symbolFig(symbol.symbol)
                ))
            case Join(_, _, symbol) =>
                g.verticalFig(Spacing.None, Seq(
                    g.textFig(node.range.toString, ap.small),
                    sfg.symbolFig(symbol.symbol)
                ))
        }
        figure.setBorder(new LineBorder(ColorConstants.darkGray))
        figure.setBackgroundColor(ColorConstants.buttonLightest)
        figure.setOpaque(true)
        figure.setSize(figure.getPreferredSize())

        val cgn = new CGraphNode(graph, SWT.NONE, figure)
        nodeMap(node) = cgn
        cgn
    }
    nodeMap(r.root).setBackgroundColor(ColorConstants.yellow)
    r.edges foreach {
        case BindEdge(start, end) =>
            new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, nodeMap(start), nodeMap(end))
        case AppendEdge(start, end, isBase) =>
            val style = ZestStyles.CONNECTIONS_DIRECTED
            val conn = new GraphConnection(graph, style, nodeMap(start), nodeMap(end))
            if (isBase) {
                conn.setLineColor(ColorConstants.green)
            }
        case JoinEdge(start, end, join) =>
            new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, nodeMap(start), nodeMap(end))
            new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, nodeMap(start), nodeMap(join))
    }

    def applyLayout(animation: Boolean): Unit = {
        if (animation) {
            graphViewer.setNodeStyle(ZestStyles.NONE)
        } else {
            graphViewer.setNodeStyle(ZestStyles.NODES_NO_LAYOUT_ANIMATION)
        }
        import org.eclipse.zest.layouts.algorithms._
        val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
        graph.setLayoutAlgorithm(layoutAlgorithm, true)
    }

    graph.addKeyListener(new KeyAdapter() {
        override def keyPressed(e: org.eclipse.swt.events.KeyEvent): Unit = {
            e.keyCode match {
                case 'r' | 'R' => applyLayout(true)
                case 't' | 'T' | 'f' | 'F' =>
                    // val parseResultFigureGenerator = new ParseResultFigureGenerator[Figure](figureGenerator, figureAppearances)
                    new ParseResultTreeViewer(r.asParseForest, figureGenerator, figureAppearances).start()
                case _ =>
            }
        }
    })

    def start(): Unit = {
        shell.open()
        applyLayout(false)
    }
}

