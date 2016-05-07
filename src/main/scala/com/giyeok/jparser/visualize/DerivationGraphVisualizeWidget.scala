package com.giyeok.jparser.visualize

import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import com.giyeok.jparser.DerivationGraph
import com.giyeok.jparser.DerivationGraph._
import org.eclipse.zest.core.widgets.Graph
import org.eclipse.swt.SWT
import org.eclipse.draw2d.Figure
import org.eclipse.swt.graphics.Font
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.LineBorder
import org.eclipse.zest.core.widgets.CGraphNode
import org.eclipse.zest.core.widgets.GraphConnection
import org.eclipse.zest.core.widgets.GraphConnection
import org.eclipse.zest.layouts.LayoutStyles
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Label
import org.eclipse.zest.core.widgets.ZestStyles
import org.eclipse.draw2d.MarginBorder
import org.eclipse.swt.events.KeyListener
import org.eclipse.draw2d.geometry.Point
import org.eclipse.draw2d.MouseListener
import com.giyeok.jparser.Inputs

class DerivationGraphVisualizeWidget(parent: Composite, val resources: ParseGraphVisualizer.Resources, derivationGraph: DerivationGraph) extends Composite(parent, SWT.NONE) {
    this.setLayout(new FillLayout())
    this.setBackground(ColorConstants.red)

    val graph = new Graph(this, SWT.NONE)

    val figureGenerator: FigureGenerator.Generator[Figure] = FigureGenerator.draw2d.Generator

    val figureAppearances = new FigureGenerator.Appearances[Figure] {
        val default = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 10, SWT.NONE), ColorConstants.black)
        val nonterminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.BOLD), ColorConstants.blue)
        val terminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.NONE), ColorConstants.red)

        override val small = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 8, SWT.NONE), ColorConstants.gray)
        override val kernelDot = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.NONE), ColorConstants.green)
        override val symbolBorder = FigureGenerator.draw2d.BorderAppearance(new LineBorder(ColorConstants.lightGray))
    }
    val parseNodeAppearances = new FigureGenerator.Appearances[Figure] {
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
    val parseNodeFigureGenerator = new ParseNodeFigureGenerator(figureGenerator, parseNodeAppearances)

    case class VisibleNode(id: Int, node: CGraphNode)

    val vnodes = scala.collection.mutable.Map[Node, VisibleNode]()
    val vedges = scala.collection.mutable.Map[Edge, Set[GraphConnection]]()

    var nodeId = 0

    def addNode(node: Node): VisibleNode = {
        vnodes get node match {
            case Some(vn) => vn
            case None =>
                nodeId += 1
                import FigureGenerator.Spacing
                val (g, ap) = (figureGenerator, figureAppearances)
                val nodeFig0 = g.horizontalFig(Spacing.Small, Seq(
                    g.supFig(g.textFig("" + nodeId, ap.default)),
                    symbolProgressFigureGenerator.kernelFig(node.kernel)))
                nodeFig0.setBorder(new LineBorder(ColorConstants.darkGray))

                val nodeFig = node match {
                    case base: BaseNode =>
                        nodeFig0.setBackgroundColor(ColorConstants.yellow)
                        nodeFig0
                    case NewAtomicNode(kernel, Some(liftBlockTrigger), None) =>
                        val f = g.verticalFig(Spacing.Small, Seq(
                            nodeFig0,
                            g.textFig(s"LiftBlockedIf(${addNode(liftBlockTrigger).id} lifted)", ap.default)))
                        nodeFig0.setBackgroundColor(ColorConstants.buttonLightest)
                        f.setBackgroundColor(ColorConstants.buttonLightest)
                        f.setBorder(new LineBorder(ColorConstants.darkGray))
                        f
                    case NewAtomicNode(kernel, None, Some(reservedReverter)) =>
                        val f = g.verticalFig(Spacing.Small, Seq(
                            nodeFig0,
                            g.textFig(reservedReverter match {
                                case Trigger.Type.Lift => "ReservedIfLift"
                                case Trigger.Type.Alive => "ReservedIfAlive"
                            }, ap.default)))
                        nodeFig0.setBackgroundColor(ColorConstants.buttonLightest)
                        f.setBackgroundColor(ColorConstants.buttonLightest)
                        f.setBorder(new LineBorder(ColorConstants.darkGray))
                        f
                    case node: NewNode =>
                        nodeFig0.setBackgroundColor(ColorConstants.buttonLightest)
                        nodeFig0
                }
                nodeFig.setOpaque(true)
                nodeFig.setSize(nodeFig.getPreferredSize())
                val graphNode = new CGraphNode(graph, SWT.NONE, nodeFig)
                val vn = VisibleNode(nodeId, graphNode)
                vnodes(node) = vn
                vn
        }
    }

    derivationGraph.nodes foreach { node =>
        addNode(node)
    }

    def triggersToString(revertTriggers: Set[Trigger]): String = (revertTriggers map {
        _ match {
            case IfLift(node) => s"Lift(${vnodes(node).id})"
            case IfAlive(node) => s"Alive(${vnodes(node).id})"
        }
    }).mkString(", ")

    derivationGraph.edges foreach { edge =>
        edge match {
            case edge @ SimpleEdge(start, end, revertTriggers) =>
                val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(start).node, vnodes(end).node)
                if (!revertTriggers.isEmpty) {
                    connection.setText(triggersToString(revertTriggers))
                }
                connection.setLineColor(ColorConstants.lightGray)
                vedges(edge) = Set(connection)
            case JoinEdge(start, end, join) =>
                val connectionEnd = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(start).node, vnodes(end).node)
                val connectionJoin = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(start).node, vnodes(join).node)
                connectionEnd.setText("main")
                connectionJoin.setText("constraint")
                connectionEnd.setLineColor(ColorConstants.lightGray)
                connectionJoin.setLineColor(ColorConstants.lightGray)
                vedges(edge) = Set(connectionEnd, connectionJoin)
        }
    }

    derivationGraph.baseNodeLifts foreach { lift =>
        val fig = parseNodeFigureGenerator.parseNodeHFig(lift.parsed)
        fig.setOpaque(true)
        fig.setBorder(new LineBorder(ColorConstants.blue))
        fig.setSize(fig.getPreferredSize())
        val node = new CGraphNode(graph, SWT.NONE, fig)
        val connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_SOLID, vnodes(derivationGraph.baseNode).node, node)
        if (!lift.revertTriggers.isEmpty) {
            connection.setText(triggersToString(lift.revertTriggers))
        }
        connection.setLineColor(ColorConstants.blue)
    }

    val termGroups = derivationGraph.termGroups
    var highlightedTermGroup = Option.empty[Inputs.TermGroupDesc]
    val termGroupButtons: Map[Inputs.TermGroupDesc, (Figure, CGraphNode)] = {
        (termGroups map { termGroup =>
            val fig = figureGenerator.textFig(termGroup.toShortString, figureAppearances.default)
            fig.setOpaque(true)
            fig.setBorder(new LineBorder())
            fig.setSize(fig.getPreferredSize())
            val node = new CGraphNode(graph, SWT.NONE, fig)

            fig.setVisible(false)

            fig.addMouseListener(new org.eclipse.draw2d.MouseListener() {
                def mousePressed(event: org.eclipse.draw2d.MouseEvent): Unit = {
                    if (highlightedTermGroup.isDefined) {
                        dehighlightAllTermGroupButtons()
                    }
                    if (highlightedTermGroup != Some(termGroup)) {
                        highlightedTermGroup = Some(termGroup)
                        termGroupButtons(termGroup)._1.setBackgroundColor(ColorConstants.lightBlue)
                        val subgraph = derivationGraph.subgraphTo(termGroup)
                        subgraph.nodes foreach { node => vnodes(node).node.setBorderColor(ColorConstants.red) }
                        subgraph.edges foreach { edge => vedges(edge) foreach { _.setLineColor(ColorConstants.red) } }
                    } else {
                        highlightedTermGroup = None
                    }
                }
                def mouseReleased(event: org.eclipse.draw2d.MouseEvent): Unit = {}
                def mouseDoubleClicked(event: org.eclipse.draw2d.MouseEvent): Unit = {}
            })
            termGroup -> (fig, node)
        }).toMap
    }
    def dehighlightAllTermGroupButtons(): Unit = {
        vnodes map { _._2.node.setBorderColor(ColorConstants.lightGray) }
        vedges.values map { _ foreach { _.setLineColor(ColorConstants.lightGray) } }
        termGroupButtons foreach { _._2._1.setBackgroundColor(ColorConstants.white) }
    }

    var termGroupButtonsVisible = false
    graph.addListener(SWT.KeyDown, new Listener() {
        def handleEvent(e: Event): Unit = {
            e.keyCode match {
                case 't' | 'T' =>
                    termGroupButtonsVisible = !termGroupButtonsVisible
                    if (termGroupButtonsVisible) {
                        highlightedTermGroup = None
                        var y = 0
                        termGroupButtons foreach { kv =>
                            kv._2._2.setLocation(0, y)
                            y += kv._2._1.getPreferredSize().height + 5
                            kv._2._1.setVisible(true)
                        }
                    } else {
                        highlightedTermGroup = None
                        dehighlightAllTermGroupButtons()
                        termGroupButtons foreach { kv =>
                            kv._2._1.setVisible(false)
                        }
                    }
                case 'r' | 'R' =>
                    graph.applyLayout()
                case _ =>
            }
        }
    })

    import org.eclipse.zest.layouts.algorithms._
    val layoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
    graph.setLayoutAlgorithm(layoutAlgorithm, true)
}
