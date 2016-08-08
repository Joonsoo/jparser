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
import com.giyeok.jparser.visualize.GrammarDefinitionFigureGenerator
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.visualize.FigureGenerator
import com.giyeok.jparser.visualize.ParsingProcessVisualizer
import org.eclipse.swt.widgets.MessageBox
import com.giyeok.jparser.Inputs.ConcreteInput
import com.giyeok.jparser.ParseResultGraphFunc
import com.giyeok.jparser.visualize.PreprocessedDerivationViewer
import com.giyeok.jparser.visualize.BasicVisualizeResources
import com.giyeok.jparser.nparser.DerivationPreprocessor
import com.giyeok.jparser.visualize.ZestParsingContextWidget
import com.giyeok.jparser.visualize.ZestDeriveTipParsingContextWidget
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.Parser.NaiveWrappedContext
import com.giyeok.jparser.nparser.Parser.DeriveTipsWrappedContext
import com.giyeok.jparser.nparser.SlicedDerivationPreprocessor
import com.giyeok.jparser.nparser.OnDemandSlicedDerivationPreprocessor
import com.giyeok.jparser.nparser.OnDemandDerivationPreprocessor
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.PreprocessedParser
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.nparser.SlicedPreprocessedParser

object AllViewer extends Viewer {
    val allTests = Set(
        com.giyeok.jparser.tests.basics.Visualization.allTests,
        com.giyeok.jparser.tests.gramgram.Visualization.allTests,
        com.giyeok.jparser.tests.javascript.Visualization.allTests).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}

trait Viewer {
    val allTests: Set[GrammarTestCases]

    def start(): Unit = {
        val display = Display.getDefault()
        val shell = new Shell(display)

        shell.setLayout(new FillLayout)

        val grammarFigAppearances = new FigureGenerator.Appearances[Figure] {
            val default = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 10, SWT.NONE), ColorConstants.black)
            val nonterminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.BOLD), ColorConstants.blue)
            val terminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.NONE), ColorConstants.red)
        }

        val sortedTestCases = allTests.toSeq.sortBy(_.grammar.name)

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
        val parserTypeButton = new org.eclipse.swt.widgets.Button(testButtons, SWT.PUSH)
        val proceedView = new org.eclipse.swt.widgets.Button(testButtons, SWT.NONE)
        val derivationView = new org.eclipse.swt.widgets.Button(testButtons, SWT.NONE)
        parserTypeButton.setText("Parser Type")
        proceedView.setText("Proceed View")
        derivationView.setText("Derivation View")

        textList.setFont(JFaceResources.getTextFont)

        sortedTestCases foreach { t => grammarList.add(t.grammar.name) }
        var shownTexts: Seq[Inputs.ConcreteSource] = Seq()
        grammarList.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                val selectedIndex = grammarList.getSelectionIndex()
                if (0 <= selectedIndex && selectedIndex < sortedTestCases.length) {
                    val testCases = sortedTestCases(grammarList.getSelectionIndex())
                    val grammar = testCases.grammar
                    def generateHtml(): xml.Elem =
                        new GrammarDefinitionFigureGenerator[xml.Elem](grammar, new FigureGenerator.Appearances[xml.Elem] {
                            val default = FigureGenerator.html.AppearanceByClass("default")
                            val nonterminal = FigureGenerator.html.AppearanceByClass("nonterminal")
                            val terminal = FigureGenerator.html.AppearanceByClass("terminal")
                        }, FigureGenerator.html.Generator).grammarDefinitionFigure
                    // grammar.usedSymbols foreach { s => println(s"used: $s") }
                    val (missingSymbols, wrongLookaheads, unusedSymbols) = (grammar.missingSymbols, grammar.wrongLookaheads, grammar.unusedSymbols)
                    val textFig = new GrammarDefinitionFigureGenerator[Figure](grammar, grammarFigAppearances, FigureGenerator.draw2d.Generator).grammarDefinitionFigure
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
                    testCases.correctSampleInputs.toSeq sortBy { _.toCleanString } foreach { i => addText(i, s"O: '${i.toCleanString}'") }
                    testCases.incorrectSampleInputs.toSeq sortBy { _.toCleanString } foreach { i => addText(i, s"X: '${i.toCleanString}'") }
                    if (testCases.isInstanceOf[AmbiguousSamples]) {
                        testCases.asInstanceOf[AmbiguousSamples].ambiguousSampleInputs.toSeq sortBy { _.toCleanString } foreach { i => addText(i, s"A: '${i.toCleanString}'") }
                    }
                }
            }
        })

        object ParserTypes extends Enumeration {
            val Naive, Preprocessed, PreprocessedSliced, PreprocessedCompacted, PreprocessedSlicedCompacted, GrowingCompacted, MostOptimized = Value
            val order = Seq(Naive, Preprocessed, PreprocessedSliced, PreprocessedCompacted, PreprocessedSlicedCompacted, GrowingCompacted, MostOptimized)
        }

        var selectedParserType: ParserTypes.Value = ParserTypes.Naive
        def setParserType(newParserType: ParserTypes.Value): Unit = {
            selectedParserType = newParserType
            selectedParserType match {
                case ParserTypes.Naive => parserTypeButton.setText("Naive")
                case ParserTypes.Preprocessed => parserTypeButton.setText("Preprocessed")
                case ParserTypes.PreprocessedSliced => parserTypeButton.setText("Preprocessed Sliced")
                case ParserTypes.PreprocessedCompacted => parserTypeButton.setText("(TODO) Preprocessed Compacted")
                case ParserTypes.PreprocessedSlicedCompacted => parserTypeButton.setText("(TODO) Preprocessed Sliced Compacted")
                case ParserTypes.GrowingCompacted => parserTypeButton.setText("(TODO) Growing")
                case ParserTypes.MostOptimized => parserTypeButton.setText("(TODO) Most Optimized\nPreprocessed Sliced Compacted + Growing")
            }
        }
        setParserType(ParserTypes.Naive)
        def nextParserType(): ParserTypes.Value = {
            val idx = ParserTypes.order.indexOf(selectedParserType)
            if (idx + 1 < ParserTypes.order.length) {
                ParserTypes.order(idx + 1)
            } else {
                ParserTypes.order(0)
            }
        }
        def startParserVisualizer(gt: GrammarTestCases, source: Seq[ConcreteInput], display: Display, shell: Shell): Unit = {
            val grammar = gt.grammar
            selectedParserType match {
                case ParserTypes.Naive =>
                    ParsingProcessVisualizer.start[NaiveWrappedContext](grammar.name, gt.nparserNaive, source, display, new Shell(display), new ZestParsingContextWidget(_, _, _, _, _))
                case ParserTypes.Preprocessed =>
                    ParsingProcessVisualizer.start[DeriveTipsWrappedContext](grammar.name, gt.nparserPreprocessed, source, display, new Shell(display), new ZestDeriveTipParsingContextWidget(_, _, _, _, _))
                case ParserTypes.PreprocessedSliced =>
                    ParsingProcessVisualizer.start[DeriveTipsWrappedContext](grammar.name, gt.nparserSlicedPreprocessed, source, display, new Shell(display), new ZestDeriveTipParsingContextWidget(_, _, _, _, _))
                case ParserTypes.PreprocessedCompacted => // TODO
                case ParserTypes.PreprocessedSlicedCompacted => // TODO
                case ParserTypes.GrowingCompacted => // TODO
                case ParserTypes.MostOptimized => // TODO
            }
        }

        parserTypeButton.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                setParserType(nextParserType())
            }
        })

        textList.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                val gt = sortedTestCases(grammarList.getSelectionIndex())
                val source = shownTexts(textList.getSelectionIndex())

                startParserVisualizer(gt, source.toSeq, display, new Shell(display))
            }
        })

        proceedView.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                if (grammarList.getSelectionIndex >= 0) {
                    val gt = sortedTestCases(grammarList.getSelectionIndex())
                    val source = Inputs.fromString(testText.getText())
                    startParserVisualizer(gt, source.toSeq, display, new Shell(display))
                }
            }
        })

        derivationView.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event): Unit = {
                if (grammarList.getSelectionIndex >= 0) {
                    val gt = sortedTestCases(grammarList.getSelectionIndex())
                    new PreprocessedDerivationViewer(gt.grammar, gt.ngrammar, gt.slicedDerivationPreprocessor, gt.compactSlicedDerivationPreprocessor, BasicVisualizeResources.nodeFigureGenerators, display, new Shell(display)).start
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