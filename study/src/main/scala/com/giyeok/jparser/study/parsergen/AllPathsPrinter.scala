package com.giyeok.jparser.study.parsergen

import com.giyeok.jparser.{NGrammar, Symbols}
import com.giyeok.jparser.metalang.MetaGrammar
import com.giyeok.jparser.parsergen.deprecated.{AKernel, GrammarAnalyzer}
import com.giyeok.jparser.utils.TermGrouper
import com.giyeok.jparser.visualize.FigureGenerator.Spacing
import com.giyeok.jparser.visualize.{BasicVisualizeResources, FigureGenerator}
import org.eclipse.draw2d.{Figure, FigureCanvas, LineBorder}
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.{Display, Shell}


object AllPathsPrinter {
    def main(args: Array[String]): Unit = {
        val testGrammarText: String =
            """S = 'a'+
            """.stripMargin('|')

        val rawGrammar = MetaGrammar.translate("Test Grammar", testGrammarText).left.get
        val grammar: NGrammar = NGrammar.fromGrammar(rawGrammar)

        val analyzer = new GrammarAnalyzer(grammar)

        val display = new Display()
        val shell = new Shell(display)

        val g = FigureGenerator.draw2d.Generator
        val nodeFig = BasicVisualizeResources.nodeFigureGenerators
        val canvas = new FigureCanvas(shell, SWT.NONE)

        val paths = analyzer.zeroReachablePathsToTerminalsFrom(analyzer.startKernel) sortBy { path => path.last.end.symbolId }
        val reachableTerms = TermGrouper.termGroupsOf((paths map { path => path.last.end } map { reachableTerm => grammar.symbolOf(reachableTerm.symbolId) } map { term =>
            term.symbol.asInstanceOf[Symbols.Terminal]
        }).toSet)
        reachableTerms foreach { termGroup =>
            println(termGroup.toShortString)
        }
        val figure = g.verticalFig(Spacing.Big, paths map { path =>
            def genFig(kernel: AKernel): Figure = {
                nodeFig.symbol.symbolPointerFig(grammar, kernel.symbolId, kernel.pointer)
            }

            g.horizontalFig(Spacing.Small, List(genFig(path.head.start)) ++ (path map { edge =>
                val endFig = genFig(edge.end)
                if (grammar.nsequences.contains(edge.end.symbolId)) {
                    val border = new LineBorder()
                    endFig.setBorder(border)
                }
                g.horizontalFig(Spacing.None, Seq(g.textFig("->", nodeFig.appear.default), endFig))
            }))
        })
        canvas.setContents(figure)

        shell.setLayout(new FillLayout)
        shell.open()
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }
}
