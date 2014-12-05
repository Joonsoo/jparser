package com.giyeok.moonparser.tests

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Shell

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Inputs
import com.giyeok.moonparser.Inputs.SourceToCleanString
import com.giyeok.moonparser.visualize.ParseGraphVisualizer

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

        val sortedGrammars = allTests.toSeq.sortBy(_.name)

        val leftFrame = new org.eclipse.swt.widgets.Composite(shell, SWT.NONE)
        leftFrame.setLayout({ val layout = new FillLayout; layout.`type` = SWT.VERTICAL; layout })
        val grammarList = new org.eclipse.swt.widgets.List(leftFrame, SWT.BORDER | SWT.V_SCROLL)
        val textList = new org.eclipse.swt.widgets.List(leftFrame, SWT.BORDER | SWT.V_SCROLL)
        sortedGrammars foreach { t => grammarList.add(t.name) }
        var shownTexts: Seq[Inputs.Source] = Seq()
        grammarList.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                val grammar = sortedGrammars(grammarList.getSelectionIndex())
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

        shell.open()
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }
}
