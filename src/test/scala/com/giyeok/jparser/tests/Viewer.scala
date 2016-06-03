package com.giyeok.jparser.tests

import com.giyeok.jparser.Grammar
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
import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.draw2d.Label
import org.eclipse.jface.resource.JFaceResources
import com.giyeok.jparser.visualize.GrammarTextFigureGenerator
import com.giyeok.jparser.visualize.GrammarTextFigureGenerator
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.visualize.FigureGenerator
import com.giyeok.jparser.visualize.ParseProcessVisualizer
import org.eclipse.swt.widgets.MessageBox
import com.giyeok.jparser.visualize.DerivationGraphVisualizer

trait Viewer {
    val allTests: Set[Grammar with Samples]

    def start(): Unit = {
        val display = Display.getDefault()
        val shell = new Shell(display)

        shell.setLayout(new FillLayout)

        val grammarFigAppearances = new FigureGenerator.Appearances[Figure] {
            val default = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 10, SWT.NONE), ColorConstants.black)
            val nonterminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.BOLD), ColorConstants.blue)
            val terminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.NONE), ColorConstants.red)
        }

        val sortedGrammars = allTests.toSeq.sortBy(_.name)

        val leftFrame = new org.eclipse.swt.widgets.Composite(shell, SWT.NONE)
        leftFrame.setLayout({ val layout = new FillLayout; layout.`type` = SWT.VERTICAL; layout })
        val rightFrame = new org.eclipse.swt.widgets.Composite(shell, SWT.NONE)
        rightFrame.setLayout({ val layout = new FillLayout; layout.`type` = SWT.VERTICAL; layout })

        val grammarList = new org.eclipse.swt.widgets.List(leftFrame, SWT.BORDER | SWT.V_SCROLL)
        val grammarFig = new FigureCanvas(leftFrame)

        val textList = new org.eclipse.swt.widgets.List(rightFrame, SWT.BORDER | SWT.V_SCROLL)
        val testText = new org.eclipse.swt.widgets.Text(rightFrame, SWT.MULTI)

        val testButtons = new org.eclipse.swt.widgets.Composite(rightFrame, SWT.BORDER)
        testButtons.setLayout(new FillLayout(SWT.HORIZONTAL))
        val proceedView = new org.eclipse.swt.widgets.Button(testButtons, SWT.NONE)
        val derivationView = new org.eclipse.swt.widgets.Button(testButtons, SWT.NONE)
        proceedView.setText("Proceed View")
        derivationView.setText("Derivation View")

        textList.setFont(JFaceResources.getTextFont)

        sortedGrammars foreach { t => grammarList.add(t.name) }
        var shownTexts: Seq[Inputs.ConcreteSource] = Seq()
        grammarList.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                val grammar = sortedGrammars(grammarList.getSelectionIndex())
                def generateHtml(): xml.Elem =
                    new GrammarTextFigureGenerator[xml.Elem](grammar, new FigureGenerator.Appearances[xml.Elem] {
                        val default = FigureGenerator.html.AppearanceByClass("default")
                        val nonterminal = FigureGenerator.html.AppearanceByClass("nonterminal")
                        val terminal = FigureGenerator.html.AppearanceByClass("terminal")
                    }, FigureGenerator.html.Generator).grammarFigure
                grammar.usedSymbols foreach { s => println(s"used: $s") }
                val (missingSymbols, wrongLookaheads, unusedSymbols) = (grammar.missingSymbols, grammar.wrongLookaheads, grammar.unusedSymbols)
                val textFig = new GrammarTextFigureGenerator[Figure](grammar, grammarFigAppearances, FigureGenerator.draw2d.Generator).grammarFigure
                if (missingSymbols.isEmpty && wrongLookaheads.isEmpty && unusedSymbols.isEmpty) {
                    grammarFig.setContents(textFig)
                } else {
                    val messages = Seq(
                        (if (!missingSymbols.isEmpty) Some(s"Missing: ${missingSymbols map { _.toShortString } mkString ", "}") else None),
                        (if (!wrongLookaheads.isEmpty) Some(s"Wrong: ${wrongLookaheads map { _.toShortString } mkString ", "}") else None),
                        (if (!unusedSymbols.isEmpty) Some(s"Unused: ${unusedSymbols map { _.toShortString } mkString ", "}") else None))
                    val fig = new Figure
                    fig.setLayoutManager(new ToolbarLayout(false))
                    val label = new Label
                    label.setText(messages.flatten mkString "\n")
                    label.setForegroundColor(ColorConstants.red)
                    fig.add(label)
                    fig.add(textFig)
                    grammarFig.setContents(fig)
                }
                textList.removeAll()

                shownTexts = Seq()
                def addText(input: Inputs.ConcreteSource, text: String): Unit = {
                    shownTexts = shownTexts :+ input
                    textList.add(text)
                }
                grammar.correctSampleInputs.toSeq sortBy { _.toCleanString } foreach { i => addText(i, s"O: '${i.toCleanString}'") }
                grammar.incorrectSampleInputs.toSeq sortBy { _.toCleanString } foreach { i => addText(i, s"X: '${i.toCleanString}'") }
            }
        })
        textList.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                val grammar = sortedGrammars(grammarList.getSelectionIndex())
                val source = shownTexts(textList.getSelectionIndex())
                ParseProcessVisualizer.start(grammar, source.toSeq, display, new Shell(display))
            }
        })

        proceedView.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                if (grammarList.getSelectionIndex >= 0) {
                    val grammar = sortedGrammars(grammarList.getSelectionIndex())
                    val source = Inputs.fromString(testText.getText())
                    ParseProcessVisualizer.start(grammar, source.toSeq, display, new Shell(display))
                }
            }
        })

        derivationView.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                if (grammarList.getSelectionIndex >= 0) {
                    val grammar = sortedGrammars(grammarList.getSelectionIndex())
                    val source = Inputs.fromString(testText.getText())
                    DerivationGraphVisualizer.start(grammar, display, new Shell(display))
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