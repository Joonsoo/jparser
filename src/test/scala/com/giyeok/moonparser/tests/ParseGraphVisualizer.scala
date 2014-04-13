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

import com.giyeok.moonparser.Parser

object ParseGraphVisualizer {

    def openParsingContext(parent: Composite, context: Parser#ParsingContext) = {
        val (nodes, edges) = (context.graph.nodes, context.graph.edges)
        val graph = new Graph(parent, SWT.NONE)
        val vnodes: Map[Parser#Node, GraphNode] = (nodes map { n => (n, new GraphNode(graph, SWT.NONE, n.toShortString)) }).toMap
        edges foreach {
            case e: Parser#SimpleEdge =>
                new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.from), vnodes(e.end.to))
            case e: Parser#DoubleEdge =>
                new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.from), vnodes(e.end.to))
                new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, vnodes(e.from), vnodes(e.end.doub))
        }
        graph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true)
    }

    def main(args: Array[String]) = {
        val display = new Display
        val shell = new Shell(display)

        val parser = new Parser(SimpleGrammar1)

        shell.setText("Parsing Graph")
        shell.setLayout(new FillLayout)
        openParsingContext(shell, parser.startingContext)

        shell.open()
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }
}
