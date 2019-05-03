package com.giyeok.jparser.parsergen.nocond.visualize

import com.giyeok.jparser.examples.ExpressionGrammars
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar.NSequence
import com.giyeok.jparser.parsergen.nocond._
import com.giyeok.jparser.visualize.{AbstractZestGraphWidget, BasicVisualizeResources}
import org.eclipse.draw2d.{ColorConstants, Figure, LineBorder}
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.{Composite, Display, Shell}
import org.eclipse.zest.core.viewers.GraphViewer
import org.eclipse.zest.core.widgets.{Graph, GraphConnection, ZestStyles}

class AKernelGraphVisualizeWidget(parent: Composite, style: Int, grammar: NGrammar, graph: AKernelGraph) extends Composite(parent, style)
    with AbstractZestGraphWidget[AKernel, AKernelEdge, AKernelGraph] {
    override val graphViewer: GraphViewer = new GraphViewer(this, style)
    override val graphCtrl: Graph = graphViewer.getGraphControl

    private val fig = BasicVisualizeResources.nodeFigureGenerators

    setLayout(new FillLayout())

    def initialize(): Unit = {
        addGraph(graph)
        applyLayout(false)
    }

    initialize()

    override def createFigure(node: AKernel): Figure = {
        val nodeFig = fig.symbol.symbolPointerFig(grammar, node.symbolId, node.pointer)
        nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
        nodeFig.setOpaque(true)
        nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
        nodeFig.setSize(nodeFig.getPreferredSize())
        nodeFig
    }

    override def createConnection(edge: AKernelEdge): GraphConnection =
        new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(edge.start), nodesMap(edge.end))
}

object AKernelGraphVisualizeWidget {
    def start(grammar: NGrammar, graphs: Seq[AKernelGraph]): Unit = {
        val display = new Display()
        val shell = new Shell(display)

        graphs.foreach { graph =>
            new AKernelGraphVisualizeWidget(shell, SWT.NONE, grammar, graph)
        }

        shell.setLayout(new FillLayout)
        shell.open()
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }

    def main(args: Array[String]): Unit = {
        val grammar = NGrammar.fromGrammar(ExpressionGrammars.simple)

        (grammar.nsymbols ++ grammar.nsequences).toList.sortBy(_._1) foreach { s =>
            println(s"${s._1} -> ${s._2.symbol.toShortString}")
        }

        val startKernel = AKernel(1, 0)
        val endSet = Set(AKernel(13, 2))

        val analyzer = new GrammarAnalyzer(grammar)
        val baseGraph = analyzer.deriveGraphFrom(startKernel)
        val simulation = new ParsingTaskSimulator(grammar).simulateProgress(baseGraph, endSet.toList map ProgressTask)

        simulation.tasks.foreach(println)
        simulation.progressTasks.filter(task => grammar.symbolOf(task.node.symbolId).isInstanceOf[NSequence]).foreach { t =>
            println(t, t.node.toReadableString(grammar, "."))
        }

        start(grammar, Seq(baseGraph, simulation.nextGraph))
    }
}
