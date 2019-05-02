package com.giyeok.jparser.parsergen.nocond.visualize

import com.giyeok.jparser.examples.SimpleGrammars
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar.NSequence
import com.giyeok.jparser.parsergen.nocond._
import com.giyeok.jparser.utils.AbstractGraph
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
    def deriveGraphToAKernelGraph(deriveGraph: DeriveGraph): AKernelGraph =
        AbstractGraph[AKernel, AKernelEdge, AKernelGraph](deriveGraph.nodes,
            deriveGraph.edges map { e => AKernelEdge(e.start, e.end) }, AKernelGraph)

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
        val grammar = NGrammar.fromGrammar(SimpleGrammars.array0Grammar)

        val analyzer = new GrammarAnalyzer(grammar)
        println(analyzer.acceptableTerms(AKernelSet(Set(AKernel(grammar.startSymbol, 0)))))
        println(analyzer.acceptableTerms(AKernelSet(Set(AKernel(3, 1)))))
        println(analyzer.acceptableTerms(AKernelSet(Set(AKernel(3, 2)))))
        println(analyzer.acceptableTerms(AKernelSet(Set(AKernel(3, 3)))))
        val progressed = Set(AKernel(4, 0))
        val baseGraph = analyzer.deriveGraph.subgraphBetween(AKernel(1, 0), progressed)
        grammar.nsymbols.toList.sortBy(_._1) foreach (s => println(s"${s._1} -> ${s._2.symbol.toShortString}"))
        grammar.nsequences.toList.sortBy(_._1) foreach (s => println(s"${s._1} -> ${s._2.symbol.toShortString}"))
        val simulation = new ParsingTaskSimulator(grammar).simulate(baseGraph, progressed.toList map ProgressTask)
        simulation.tasks.foreach(println)
        println("====")
        println(simulation.nullableSymbolIds)
        (simulation.progressTasks -- (progressed map ProgressTask)).foreach(println)
        simulation.nullableProgressTasks filter (task => grammar.symbolOf(task.node.symbolId).isInstanceOf[NSequence]) foreach println
        // deriveGraphToAKernelGraph(analyzer.deriveGraph)

        println("====")
        val startSet = AKernelSet(Set(AKernel(12, 1), AKernel(3, 1), AKernel(9, 1), AKernel(3, 2), AKernel(3, 3)))
        val terms = analyzer.acceptableTerms(startSet)
        terms foreach { termGroup =>
            val graphChange = analyzer.termChanges(startSet, termGroup)
            println(termGroup)
            println(graphChange)
            graphChange.following foreach { f =>
                f.following.sortedItems foreach { i => println(i.toReadableString(grammar)) }
            }
        }

        start(grammar, Seq(deriveGraphToAKernelGraph(baseGraph), simulation.nextGraph))
    }
}
