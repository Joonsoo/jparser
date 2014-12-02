package com.giyeok.moonparser.tests

import scala.Left
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import org.eclipse.zest.core.widgets.Graph
import org.eclipse.zest.core.widgets.GraphConnection
import org.eclipse.zest.core.widgets.GraphNode
import org.eclipse.zest.core.widgets.ZestStyles
import org.eclipse.zest.layouts.LayoutStyles
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm
import com.giyeok.moonparser.Inputs
import com.giyeok.moonparser.ParseTree.TreePrintableParseNode
import com.giyeok.moonparser.Parser
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Control

class ParsingContextGraph(parent: Composite, resources: ParseGraphVisualizer.Resources, private val context: Parser#ParsingContext) extends Composite(parent, SWT.NONE) {
    this.setLayout(new FillLayout)

    private val (nodes, edges) = (context.graph.nodes, context.graph.edges)
    private val graph = new Graph(this, SWT.NONE)
    private var vnodes: Map[Parser#Node, GraphNode] = (nodes map { n =>
        val graphNode = new GraphNode(graph, SWT.NONE, n.toShortString)
        if (n.canFinish) {
            val f = new org.eclipse.draw2d.Label()
            f.setFont(resources.fixedWidth10Font)
            f.setText(n.parsed.get.toHorizontalHierarchyString)
            graphNode.setTooltip(f)
        }
        (n, graphNode)
    }).toMap
    private var vedges = edges flatMap {
        case e: Parser#SimpleEdge =>
            Set(new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.from), vnodes(e.to)))
        case e: Parser#AssassinEdge =>
            Set(new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.from), vnodes(e.to)))
    }
    graph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true)
}

object ParseGraphVisualizer {
    class Resources(val fixedWidth10Font: Font)

    def main(args: Array[String]) = {
        val display = new Display
        val shell = new Shell(display)

        val resources = new Resources(new Font(null, "Monaco", 11, SWT.NONE))

        val parser = new Parser(SimpleGrammar1_1)

        val layout = new StackLayout

        shell.setText("Parsing Graph")
        shell.setLayout(layout)

        val source = Inputs.fromString("abcabcabc")
        val fin = source.scanLeft[Either[Parser#ParsingContext, Parser#ParsingError], Seq[Either[Parser#ParsingContext, Parser#ParsingError]]](Left[Parser#ParsingContext, Parser#ParsingError](parser.startingContext)) {
            (ctx, terminal) =>
                ctx match {
                    case Left(ctx) => ctx proceedTerminal terminal
                    case error @ Right(_) => error
                }
        }
        val views: Seq[Control] = fin map {
            case Left(ctx) =>
                new ParsingContextGraph(shell, resources, ctx)
            case Right(error) =>
                val label = new Label(shell, SWT.NONE)
                label.setAlignment(SWT.CENTER)
                label.setText(error.msg)
                label
        }

        var currentLocation = 0

        def updateLocation(newLocation: Int): Unit = {
            println("newloc: " + newLocation)
            println("newloc: " + (newLocation >= 0))
            println("newloc: " + (newLocation <= source.size))
            if (newLocation >= 0 && newLocation <= source.size) {
                println("newloc: " + newLocation)
                currentLocation = newLocation
                shell.setText((source take newLocation map { _.toShortString } mkString " ") + "*" + (source drop newLocation map { _.toShortString } mkString " "))
                layout.topControl = views(newLocation)
                shell.layout()
            }
        }
        updateLocation(currentLocation)

        val keyListener = new KeyListener() {
            def keyPressed(x: org.eclipse.swt.events.KeyEvent): Unit = {
                x.keyCode match {
                    case SWT.ARROW_LEFT => updateLocation(currentLocation - 1)
                    case SWT.ARROW_RIGHT => updateLocation(currentLocation + 1)
                    case code => println(code)
                }
            }

            def keyReleased(x: org.eclipse.swt.events.KeyEvent): Unit = {}
        }
        shell.addKeyListener(keyListener)
        views foreach { _.addKeyListener(keyListener) }

        shell.open()
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }
}
