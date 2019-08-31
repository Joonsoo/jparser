package com.giyeok.jparser.parsergen.nocond.visualize

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.NSequence
import com.giyeok.jparser.examples.metagram.SimpleGrammars
import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.nparser.ParsingContext
import com.giyeok.jparser.parsergen.nocond._
import com.giyeok.jparser.visualize.{AbstractZestGraphWidget, BasicVisualizeResources}
import org.eclipse.draw2d.{ColorConstants, Figure, LineBorder}
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.{Composite, Display, Shell}
import org.eclipse.zest.core.viewers.GraphViewer
import org.eclipse.zest.core.widgets.{Graph, GraphConnection, ZestStyles}

class AKernelGenGraphVisualizeWidget(parent: Composite, style: Int, grammar: NGrammar, graph: AKernelGenGraph) extends Composite(parent, style)
    with AbstractZestGraphWidget[AKernelGen, AKernelGenEdge, AKernelGenGraph] {
    override val graphViewer: GraphViewer = new GraphViewer(this, style)
    override val graphCtrl: Graph = graphViewer.getGraphControl

    private val fig = BasicVisualizeResources.nodeFigureGenerators

    setLayout(new FillLayout())

    def initialize(): Unit = {
        addGraph(graph)
        applyLayout(false)
    }

    initialize()

    override def createFigure(node: AKernelGen): Figure = {
        val kernel = ParsingContext.Kernel(node.symbolId, node.pointer, node.created, node.updated)(grammar.symbolOf(node.symbolId))
        val nodeFig = fig.kernelFig(grammar, kernel)
        nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
        nodeFig.setOpaque(true)
        nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
        nodeFig.setSize(nodeFig.getPreferredSize())
        nodeFig
    }

    override def createConnection(edge: AKernelGenEdge): GraphConnection =
        new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(edge.start), nodesMap(edge.end))
}

object AKernelGraphVisualizeWidget {
    def start(grammar: NGrammar, graphs: Seq[AKernelGenGraph]): Unit = {
        val display = new Display()
        val shell = new Shell(display)

        graphs.foreach { graph =>
            new AKernelGenGraphVisualizeWidget(shell, SWT.NONE, grammar, graph)
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
        val grammar = NGrammar.fromGrammar(SimpleGrammars.array0Grammar.toGrammar(MetaGrammar.translateForce))

        (grammar.nsymbols ++ grammar.nsequences).toList.sortBy(_._1) foreach { s =>
            println(s"${s._1} -> ${s._2.symbol.toShortString}")
        }

        val startKernels = Set(AKernel(12, 1), AKernel(19, 1))
        val endSet = Set(AKernelGen(13, 0, 0, 0))

        val analyzer = new GrammarAnalyzer(grammar)
        val baseGraph = startKernels.foldLeft(AKernelGenGraph.emptyGraph) { (m, i) => m.merge(analyzer.deriveGraphFrom(i)) }
        val boundaryTasks: Set[Task] = startKernels map { k => ProgressTask(AKernelGen.from(k, 0, 0)) }
        val simulation = new ParsingTaskSimulator(grammar).simulate(baseGraph, endSet.toList map ProgressTask, boundaryTasks)

        boundaryTasks.foreach(println)
        println("===")
        simulation.tasks.foreach(println)
        simulation.progressTasks.filter(task => grammar.symbolOf(task.node.symbolId).isInstanceOf[NSequence]).foreach { t =>
            println(t, t.node.kernel.toReadableString(grammar), t.node.created, t.node.updated)
        }

        start(grammar, Seq(baseGraph, simulation.nextGraph))
    }
}
