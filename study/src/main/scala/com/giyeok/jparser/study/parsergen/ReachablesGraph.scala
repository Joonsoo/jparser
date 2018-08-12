package com.giyeok.jparser.study.parsergen

import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar.NTerminal
import com.giyeok.jparser.visualize.{AbstractZestGraphWidget, BasicVisualizeResources, Interactable, NodeFigureGenerators}
import org.eclipse.draw2d.{ColorConstants, Figure, LineBorder}
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{KeyEvent, KeyListener, MouseListener}
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.{Composite, Display, Shell}
import org.eclipse.zest.core.viewers.GraphViewer
import org.eclipse.zest.core.widgets.{Graph, GraphConnection, ZestStyles}

object ReachablesGraph {

    class AKernelGraphViewer(parent: Composite, style: Int, val fig: NodeFigureGenerators[Figure], val analyzer: GrammarAnalyzer)
        extends Composite(parent, style)
            with AbstractZestGraphWidget[AKernel, AKernelEdge, AKernelGraph]
            with Interactable[AKernel, AKernelEdge, AKernelGraph] {
        val graphViewer = new GraphViewer(this, style)
        val graphCtrl: Graph = graphViewer.getGraphControl

        val grammar = analyzer.grammar

        setLayout(new FillLayout())

        override def createFigure(node: AKernel): Figure = {
            val nodeFig = fig.symbol.symbolPointerFig(grammar, node.symbolId, node.pointer)
            nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
            nodeFig.setOpaque(true)
            nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
            nodeFig.setSize(nodeFig.getPreferredSize())
            nodeFig
        }

        override def createConnection(edge: AKernelEdge): GraphConnection = {
            val conn = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_DIRECTED, nodesMap(edge.start), nodesMap(edge.end))
            if (analyzer.isZeroReachableAKernel(edge.end)) {
                conn.setLineColor(ColorConstants.green)
            }
            conn.setData((Seq(edge.start, edge.end), Seq(conn)))
            conn
        }
    }

    def main(args: Array[String]): Unit = {
        val testGrammarText: String =
            """S = T*
              |T = Kw | Id
              |Kw = N & "if"
              |Id = N - Kw
              |N = <{a-z}+>
            """.stripMargin('|')

        val rawGrammar = MetaGrammar.translate("Test Grammar", testGrammarText).left.get
        val grammar: NGrammar = NGrammar.fromGrammar(rawGrammar)

        val analyzer = new GrammarAnalyzer(grammar)
        analyzer.deriveRelations.edgesByStart foreach { relation =>
            println(relation._1.toReadableString(grammar))
            relation._2 foreach { derivable =>
                println(s"    ${derivable.end.toReadableString(grammar)}")
            }
        }
        analyzer.nullableSymbols foreach { symbolId =>
            println(s"nullable: $symbolId ${grammar.symbolOf(symbolId)}")
        }

        val display = new Display()
        val shell = new Shell(display)

        val graphViewer = new AKernelGraphViewer(shell, SWT.NONE, BasicVisualizeResources.nodeFigureGenerators, analyzer)

        object SubgraphStatus extends Enumeration {
            val None, Derivers, Derivables, Surroundings, ZeroDerivers = Value
        }
        var showingSubgraph = Option.empty[(AKernel, SubgraphStatus.Value)]

        val zrColor = new Color(null, 229, 140, 96)
        val zrTermColor = new Color(null, 255, 253, 121)
        val termColor = new Color(null, 179, 205, 224)

        def updateSubgraph(): Unit = {
            println(showingSubgraph)
            showingSubgraph match {
                case Some((node, status)) =>
                    val subgraph: AKernelGraph = status match {
                        case SubgraphStatus.None => analyzer.deriveRelations
                        case SubgraphStatus.Derivers => analyzer.deriversOf(node.symbolId)
                        case SubgraphStatus.Derivables => analyzer.derivablesOf(node)
                        case SubgraphStatus.Surroundings => analyzer.surroundingsOf(node.symbolId)
                        case SubgraphStatus.ZeroDerivers => analyzer.zeroDeriversOf(node.symbolId)
                    }
                    graphViewer.setVisibleSubgraph(subgraph.nodes, subgraph.edges)
                    graphViewer.nodesMap(node).setBackgroundColor(ColorConstants.orange)
                case None =>
                    graphViewer.setVisibleSubgraph(analyzer.deriveRelations.nodes, analyzer.deriveRelations.edges)
                    graphViewer.nodesMap.foreach { case (node, visual) =>
                        if (grammar.symbolOf(node.symbolId).isInstanceOf[NTerminal]) {
                            visual.setBackgroundColor(termColor)
                        } else {
                            visual.setBackgroundColor(ColorConstants.white)
                        }
                        val border = new LineBorder()
                        visual.getFigure.setBorder(border)
                    }
            }
        }

        graphViewer.addMouseListener(new MouseListener() {
            def mouseDown(e: org.eclipse.swt.events.MouseEvent): Unit = {}

            def mouseUp(e: org.eclipse.swt.events.MouseEvent): Unit = {}

            def mouseDoubleClick(e: org.eclipse.swt.events.MouseEvent): Unit = {
                graphViewer.nodesAt(e.x, e.y) foreach {
                    case node: AKernel =>
                        showingSubgraph = None
                        updateSubgraph()
                        showingSubgraph = Some((node, SubgraphStatus.None))
                        analyzer.zeroReachablesFrom(node).nodes foreach { zr =>
                            if (grammar.symbolOf(zr.symbolId).isInstanceOf[NTerminal]) {
                                graphViewer.nodesMap(zr).setBackgroundColor(zrTermColor)
                            } else {
                                graphViewer.nodesMap(zr).setBackgroundColor(zrColor)
                            }
                        }
                        updateSubgraph()
                    case data =>
                        println(data)
                }
            }
        })

        graphViewer.addKeyListener(new KeyListener {
            override def keyPressed(e: KeyEvent): Unit = {
                println(e.keyCode)
                e.keyCode match {
                    case '`' | '~' if showingSubgraph.isDefined =>
                        showingSubgraph = Some((showingSubgraph.get._1, SubgraphStatus.None))
                        updateSubgraph()
                    case '1' if showingSubgraph.isDefined =>
                        showingSubgraph = Some((showingSubgraph.get._1, SubgraphStatus.Derivers))
                        updateSubgraph()
                    case '2' if showingSubgraph.isDefined =>
                        showingSubgraph = Some((showingSubgraph.get._1, SubgraphStatus.Derivables))
                        updateSubgraph()
                    case '3' if showingSubgraph.isDefined =>
                        showingSubgraph = Some((showingSubgraph.get._1, SubgraphStatus.Surroundings))
                        updateSubgraph()
                    case '4' if showingSubgraph.isDefined =>
                        showingSubgraph = Some((showingSubgraph.get._1, SubgraphStatus.ZeroDerivers))
                        updateSubgraph()
                    case 'X' | 'x' if showingSubgraph.isDefined =>
                        showingSubgraph = None
                        updateSubgraph()
                    case 'R' | 'r' =>
                        graphViewer.applyLayout(true)
                    case _ => // do nothing
                }
            }

            override def keyReleased(e: KeyEvent): Unit = {}
        })
        // pointer == 0 인 노드들로 propagate

        graphViewer.addGraph(analyzer.deriveRelations)
        graphViewer.nodesMap foreach { case (node, visual) =>
            if (grammar.symbolOf(node.symbolId).isInstanceOf[NTerminal]) {
                visual.setBackgroundColor(termColor)
            }
        }
        graphViewer.applyLayout(true)

        shell.setLayout(new FillLayout)
        shell.open()
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }
}
