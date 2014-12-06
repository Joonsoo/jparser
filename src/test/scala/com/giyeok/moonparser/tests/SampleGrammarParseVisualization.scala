package com.giyeok.moonparser.tests

import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Shell

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Inputs
import com.giyeok.moonparser.Inputs.SourceToCleanString
import com.giyeok.moonparser.visualize.GrammarTextFigureGenerator
import com.giyeok.moonparser.visualize.ParseGraphVisualizer
import com.giyeok.moonparser.visualize.GrammarTextFigureGenerator

trait Samples {
    val correctSampleInputs: Set[Inputs.Source]
    val incorrectSampleInputs: Set[Inputs.Source]
}

trait StringSamples extends Samples {
    val correctSamples: Set[String]
    val incorrectSamples: Set[String]

    lazy val correctSampleInputs: Set[Inputs.Source] = correctSamples map { Inputs.fromString _ }
    lazy val incorrectSampleInputs: Set[Inputs.Source] = incorrectSamples map { Inputs.fromString _ }
}

object SampleGrammarParseVisualization {
    val allTests: Set[Grammar with Samples] = Set(
        SimpleGrammarSet1.grammars,
        SimpleGrammarSet2.grammars,
        RecursiveGrammarSet1.grammars).flatten

    def main(args: Array[String]): Unit = {
        val display = new Display
        val shell = new Shell(display)

        shell.setLayout(new FillLayout)

        val grammarFigAppearances = new GrammarTextFigureGenerator.Appearances[Figure] {
            val default = GrammarTextFigureGenerator.draw2d.Appearance(new Font(null, "Monaco", 10, SWT.NONE), ColorConstants.black)
            val nonterminal = GrammarTextFigureGenerator.draw2d.Appearance(new Font(null, "Monaco", 12, SWT.BOLD), ColorConstants.blue)
            val terminal = GrammarTextFigureGenerator.draw2d.Appearance(new Font(null, "Monaco", 12, SWT.NONE), ColorConstants.red)
        }

        val sortedGrammars = allTests.toSeq.sortBy(_.name)

        val leftFrame = new org.eclipse.swt.widgets.Composite(shell, SWT.NONE)
        leftFrame.setLayout({ val layout = new FillLayout; layout.`type` = SWT.VERTICAL; layout })
        val grammarList = new org.eclipse.swt.widgets.List(leftFrame, SWT.BORDER | SWT.V_SCROLL)
        val grammarFig = new FigureCanvas(leftFrame)
        val textList = new org.eclipse.swt.widgets.List(leftFrame, SWT.BORDER | SWT.V_SCROLL)
        sortedGrammars foreach { t => grammarList.add(t.name) }
        var shownTexts: Seq[Inputs.Source] = Seq()
        grammarList.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                val grammar = sortedGrammars(grammarList.getSelectionIndex())
                println(new GrammarTextFigureGenerator[xml.Elem](grammar, new GrammarTextFigureGenerator.Appearances[xml.Elem] {
                    val default = GrammarTextFigureGenerator.html.AppearanceByClass("default")
                    val nonterminal = GrammarTextFigureGenerator.html.AppearanceByClass("nonterminal")
                    val terminal = GrammarTextFigureGenerator.html.AppearanceByClass("terminal")
                }, GrammarTextFigureGenerator.html.Generator).grammarFigure)
                grammarFig.setContents(new GrammarTextFigureGenerator[Figure](grammar, grammarFigAppearances, GrammarTextFigureGenerator.draw2d.Generator).grammarFigure)
                textList.removeAll()
                shownTexts = (grammar.correctSampleInputs.toSeq sortBy { _.toCleanString }) ++ (grammar.incorrectSampleInputs.toSeq sortBy { _.toCleanString })
                shownTexts foreach { i => textList.add(s"'${i.toCleanString}'") }
            }
        })
        textList.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                val grammar = sortedGrammars(grammarList.getSelectionIndex())
                val source = shownTexts(textList.getSelectionIndex())
                ParseGraphVisualizer.start(grammar, source.toSeq, display, new Shell(display))
            }
        })

        val rightFrame = new org.eclipse.swt.widgets.Composite(shell, SWT.NONE)
        rightFrame.setLayout({ val layout = new FillLayout; layout.`type` = SWT.VERTICAL; layout })
        val text = new org.eclipse.swt.widgets.Text(rightFrame, SWT.NONE)
        val button = new org.eclipse.swt.widgets.Button(rightFrame, SWT.NONE)
        button.setText("Show")
        button.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                if (grammarList.getSelectionIndex() >= 0) {
                    val grammar = sortedGrammars(grammarList.getSelectionIndex())
                    val source = Inputs.fromString(text.getText())
                    ParseGraphVisualizer.start(grammar, source.toSeq, display, new Shell(display))
                }
            }
        })

        val isMac = System.getProperty("os.name").toLowerCase contains "mac"
        display.addFilter(SWT.KeyDown, new Listener() {
            def handleEvent(e: Event): Unit = {
                (isMac, e.stateMask, e.keyCode) match {
                    case (true, SWT.COMMAND, 'w') | (false, SWT.CONTROL, 'w') =>
                        display.getActiveShell().dispose()
                    case _ =>
                }
            }
        })

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
