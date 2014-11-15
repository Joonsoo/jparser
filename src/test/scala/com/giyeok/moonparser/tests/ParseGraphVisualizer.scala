package com.giyeok.moonparser.tests

import scala.annotation.migration
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.zest.core.widgets.Graph
import org.eclipse.zest.core.widgets.GraphConnection
import org.eclipse.zest.core.widgets.GraphNode
import org.eclipse.zest.core.widgets.ZestStyles
import org.eclipse.zest.layouts.LayoutStyles
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm
import com.giyeok.moonparser.Inputs
import com.giyeok.moonparser.Parser
import org.eclipse.draw2d.ColorConstants

class ParsingContextGraph(parent: Composite, private val context: Parser#ParsingContext) {
    private val (nodes, edges) = (context.graph.nodes, context.graph.edges)
    private val graph = new Graph(parent, SWT.NONE)
    private var vnodes: Map[Parser#Node, GraphNode] = (nodes map { n => (n, new GraphNode(graph, SWT.NONE, n.toShortString)) }).toMap
    private var vedges = edges flatMap {
        case e: Parser#SimpleEdge =>
            Set(new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.from), vnodes(e.to)))
        case e: Parser#DoubleEdge =>
            val e1 = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.from), vnodes(e.to))
            val e2 = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED | ZestStyles.CONNECTIONS_DASH_DOT, vnodes(e.from), vnodes(e.doub))
            Set(e1, e2)
    }
    graph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true)

    def proceedTerminal(next: Inputs.Input) = {
        val nextnodes = (nodes flatMap {
            case s: Parser#SymbolProgressTerminal => (s proceedTerminal next) map { (s, _) }
            case _ => None
        })
        nextnodes foreach { p =>
            val gn = new GraphNode(graph, SWT.NONE, p._2.toShortString)
            val gc = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED | ZestStyles.CONNECTIONS_DOT, vnodes(p._1), gn)
            gc.setLineColor(ColorConstants.blue)
        }
    }
}

object ParseGraphVisualizer {
    def main(args: Array[String]) = {
        val display = new Display
        val shell = new Shell(display)

        val parser = new Parser(SimpleGrammar2)

        shell.setText("Parsing Graph")
        shell.setLayout(new FillLayout)
        val graph = new ParsingContextGraph(shell, parser.startingContext)
        graph.proceedTerminal(Inputs.Character('v', 0))

        shell.open()
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }
}
