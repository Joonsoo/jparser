package com.giyeok.moonparser.visualize

import com.giyeok.moonparser.Inputs
import com.giyeok.moonparser.Parser
import org.eclipse.draw2d.graph.DirectedGraph
import org.eclipse.draw2d.graph.DirectedGraphLayout
import org.eclipse.draw2d.Label
import com.giyeok.moonparser.tests.SimpleGrammar1
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.layout.FillLayout
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.swt.SWT
import org.eclipse.draw2d.Panel
import org.eclipse.draw2d.XYLayout
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.geometry.Rectangle
import com.giyeok.moonparser.Parser
import org.eclipse.draw2d.PolylineConnection
import org.eclipse.draw2d.BendpointConnectionRouter
import org.eclipse.draw2d.AbsoluteBendpoint
import org.eclipse.draw2d.PolygonDecoration

class ParsingContextGraphVisualizeFigure(private val context: Parser#ParsingContext, private val src: Option[Inputs.Input]) extends Figure {
    val graph = new DirectedGraph
    private val nodes = graph.nodes.asInstanceOf[java.util.List[Any]]
    private val edges = graph.edges.asInstanceOf[java.util.List[Any]]

    val nodemap: Map[Parser#SymbolProgress, org.eclipse.draw2d.graph.Node] =
        (context.graph.nodes map { n =>
            val node = new org.eclipse.draw2d.graph.Node
            node.data = n
            nodes.add(node)
            (n, node)
        }).toMap
    context.graph.edges foreach { e =>
        val edge = new org.eclipse.draw2d.graph.Edge(nodemap(e.from), nodemap(e.to))
        edges.add(edge)
    }
    val graphLayout = new DirectedGraphLayout
    graphLayout.visit(graph)

    setLayoutManager(new XYLayout())
    layout()

    val ni = nodes.iterator()
    while (ni.hasNext()) {
        val node = ni.next().asInstanceOf[org.eclipse.draw2d.graph.Node]
        val n = node.data.asInstanceOf[Parser#SymbolProgress]
        val label = new Label
        label.setText(n.toShortString)
        label.setBorder(new LineBorder(1))
        node.width = 200
        add(label, new Rectangle(node.x, node.y, node.width, node.height))
    }

    val ei = edges.iterator()
    while (ei.hasNext()) {
        val edge = ei.next().asInstanceOf[org.eclipse.draw2d.graph.Edge]
        val conn = new PolylineConnection
        val vnodes = edge.vNodes
        val bends = new java.util.ArrayList[AbsoluteBendpoint]
        if (vnodes != null) {
            val vi = vnodes.asInstanceOf[List[org.eclipse.draw2d.graph.Node]].iterator
            if (vi != null) {
                while (vi.hasNext) {
                    val vnode = vi.next()
                    bends.add(new AbsoluteBendpoint(vnode.x, vnode.y))
                    bends.add(new AbsoluteBendpoint(vnode.x, vnode.y + vnode.height))
                }
            }
        }
        conn.setConnectionRouter(new BendpointConnectionRouter)
        conn.setRoutingConstraint(bends)

        val dec = new PolygonDecoration
        conn.setTargetDecoration(dec)
        conn.setPoints(edge.getPoints())
        add(conn)
    }
}

object ParsingContextGraphVisualizeFigure {
    def main(args: Array[String]): Unit = {
        val display = new Display
        val shell = new Shell(display)

        shell.setLayout(new FillLayout)

        val figure = new ParsingContextGraphVisualizeFigure(new Parser(SimpleGrammar1).startingContext, None)
        val canvas = new FigureCanvas(shell, SWT.NONE)

        canvas.setContents(figure)

        try {
            shell.open()
            try {
                while (!shell.isDisposed()) {
                    if (!display.readAndDispatch()) {
                        display.sleep()
                    }
                }
            } finally {
                if (!shell.isDisposed()) {
                    shell.dispose()
                }
            }
        } finally {
            display.dispose()
        }
    }
}
