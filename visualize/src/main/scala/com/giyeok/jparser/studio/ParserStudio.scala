package com.giyeok.jparser.studio

import java.util.concurrent.LinkedBlockingDeque

import com.giyeok.jparser.NGrammar.NTerminal
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.Symbols.{Nonterminal, Sequence}
import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.gramgram.meta2.{ASTifier, AstAnalyzer, MetaGrammar2}
import com.giyeok.jparser.nparser.Parser.NaiveContext
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor, ParsingContext}
import com.giyeok.jparser.visualize._
import com.giyeok.jparser.visualize.utils.{HorizontalResizableSplittedComposite, VerticalResizableSplittedComposite}
import com.giyeok.jparser._
import org.eclipse.draw2d.{ColorConstants, Figure, FigureCanvas}
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.{ExtendedModifyListener, StyleRange, StyledText}
import org.eclipse.swt.events.{DisposeListener, SelectionEvent, SelectionListener}
import org.eclipse.swt.layout._
import org.eclipse.swt.widgets.{Button, Composite, Control, Display, Label, MessageBox, Shell}

import scala.collection.immutable.{ListMap, ListSet}
import scala.util.{Failure, Try}

object ParserStudio {
    def start(initialGrammar: String, initialTest: String, examples: Seq[GrammarExample]): Unit = {
        val display = new Display()
        val shell = new Shell(display)

        new ParserStudio(shell, SWT.NONE)(initialGrammar, initialTest, examples)

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

case class GrammarExample(name: String, grammar: Grammar, correctTests: Seq[String], incorrectTests: Seq[String], ambiguousTest: Seq[String])

class ParserStudio(parent: Composite, style: Int)(initialGrammar: String, initialTest: String, _exampleGrammars: Seq[GrammarExample]) extends Composite(parent, style) {
    setLayout(new FillLayout)

    private val exampleGrammars: Seq[(GrammarExample, String)] =
        _exampleGrammars flatMap { g =>
            val grammarInText = Try(MetaGrammar.stringify(g.grammar))
            if (grammarInText.isFailure) {
                println(s"${g.name}, ${grammarInText.asInstanceOf[Failure[_]].exception}")
            }
            grammarInText.toOption map { (g, _) }
        }

    val rootPanel = new VerticalResizableSplittedComposite(this, SWT.NONE, 40)

    val grammarDefParser = new ParseProcessor[Option[Grammar]](
        MetaGrammar2.GrammarDef.oldGrammar,
        //MetaGrammar2.GrammarDef.ngrammar,
        (x: ParseForest) => Some(new AstAnalyzer(ASTifier.matchGrammar(x.trees.head)).analyzeAstifiers().grammar("Grammar"))
    )

    val emptyGrammarParser = new ParseProcessor[ParseForest](
        NGrammar.fromGrammar(new Grammar() {
            val name: String = ""
            val rules: RuleMap = ListMap("S" -> ListSet(Sequence(Seq())))
            val startSymbol: Nonterminal = Nonterminal("S")
        }), (x: ParseForest) => x
    )

    // Grammar Panel
    val grammarPanel = rootPanel.leftPanel
    grammarPanel.setLayout(new FormLayout)
    val grammarText = new NotificationPanel(grammarPanel, SWT.NONE)(None, new SourceText(_, SWT.NONE, grammarDefParser))
    val grammarControlPanel = new Composite(grammarPanel, SWT.NONE)
    val grammarInfoPanel = new Composite(grammarPanel, SWT.NONE)
    grammarText.setLayoutData({
        val d = new FormData()
        d.top = new FormAttachment(grammarControlPanel)
        d.bottom = new FormAttachment(grammarInfoPanel)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    })
    grammarControlPanel.setLayoutData({
        val d = new FormData()
        d.top = new FormAttachment(0)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    })
    grammarInfoPanel.setLayoutData({
        val d = new FormData()
        d.bottom = new FormAttachment(100)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    })
    grammarText.control.setText(initialGrammar)

    grammarControlPanel.setLayout(new FillLayout)
    val grammarAddEpsilonButton = new Button(grammarControlPanel, SWT.NONE)
    grammarAddEpsilonButton.setText("ε")
    grammarAddEpsilonButton.addSelectionListener(new SelectionListener {
        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
        def widgetSelected(e: SelectionEvent): Unit = {
            grammarText.control.appendTextToCurrentCursor("ε")
            grammarText.control.setFocus()
        }
    })
    val grammarOpenButton = new Button(grammarControlPanel, SWT.NONE)
    grammarOpenButton.setText("Open Example Grammars")
    if (exampleGrammars.isEmpty) {
        grammarOpenButton.setEnabled(false)
    } else {
        grammarOpenButton.addSelectionListener(new SelectionListener {
            def widgetDefaultSelected(e: SelectionEvent): Unit = {}

            def widgetSelected(e: SelectionEvent): Unit = {
                val display = getDisplay
                val shell = new Shell(display, SWT.APPLICATION_MODAL | SWT.SHEET)

                shell.setLayout(new FillLayout())
                shell.setText("Choose a grammar")
                val panel = new VerticalResizableSplittedComposite(shell, SWT.NONE)
                val grammarsList = new org.eclipse.swt.widgets.List(panel.leftPanel, SWT.V_SCROLL)
                val grammarPreviewPanel = new Composite(panel.rightPanel, SWT.NONE)
                grammarPreviewPanel.setLayout(new FormLayout)

                val grammarPreview = new SourceText(grammarPreviewPanel, SWT.NONE, grammarDefParser)
                grammarPreview.text.setEditable(false)
                // val testsList = new org.eclipse.swt.widgets.List(panel.rightPanel, SWT.NONE)

                val grammarSetBtn = new Button(grammarPreviewPanel, SWT.NONE)
                grammarSetBtn.setText("Load")

                grammarPreview.setLayoutData({
                    val d = new FormData()
                    d.top = new FormAttachment(0)
                    d.bottom = new FormAttachment(grammarSetBtn)
                    d.left = new FormAttachment(0)
                    d.right = new FormAttachment(100)
                    d
                })
                grammarSetBtn.setLayoutData({
                    val d = new FormData()
                    d.bottom = new FormAttachment(100)
                    d.left = new FormAttachment(0)
                    d.right = new FormAttachment(100)
                    d
                })

                grammarsList.setFont(BasicVisualizeResources.fixedWidth12Font)

                exampleGrammars foreach { exampleGrammar =>
                    grammarsList.add(exampleGrammar._1.name)
                }

                grammarsList.addSelectionListener(new SelectionListener {
                    def widgetDefaultSelected(e: SelectionEvent): Unit = {}
                    def widgetSelected(e: SelectionEvent): Unit = {
                        val idx = grammarsList.getSelectionIndex
                        if (idx >= 0 && idx < exampleGrammars.length) {
                            val grammarDefText = exampleGrammars(idx)._2
                            grammarPreview.setText(grammarDefText)
                        }
                    }
                })

                grammarSetBtn.addSelectionListener(new SelectionListener {
                    def widgetDefaultSelected(e: SelectionEvent): Unit = {}
                    def widgetSelected(e: SelectionEvent): Unit = {
                        val idx = grammarsList.getSelectionIndex
                        if (idx >= 0 && idx < exampleGrammars.length) {
                            val grammarDefText = exampleGrammars(idx)._2
                            grammarText.control.setText(grammarDefText)
                            shell.close()
                        } else {
                            val msg = new MessageBox(shell)
                            msg.setMessage("Choose a grammar")
                            msg.open()
                        }
                    }
                })

                shell.open()

                while (!shell.isDisposed()) {
                    if (!display.readAndDispatch()) {
                        display.sleep()
                    }
                }
            }
        })
    }

    grammarInfoPanel.setLayout(new FillLayout)
    val parserGenerationViewButton = new Button(grammarInfoPanel, SWT.NONE)
    parserGenerationViewButton.setText("Parser Generator")
    //    val parserGeneratorPanel = new Composite(grammarInfoPanel, SWT.NONE)
    //    parserGeneratorPanel.setLayout(new FillLayout(SWT.VERTICAL))
    //    val generateParserSelector = new ParserSelector(parserGeneratorPanel, SWT.NONE)
    // val generateButton = new Button(parserGeneratorPanel, SWT.NONE)
    // generateButton.setText("Generate Parser")

    // Test Panel
    // val rightPanel = new HorizontalResizableSplittedComposite(rootPanel.rightPanel, SWT.NONE, 20)
    // val highlightingSymbols = new NotificationPanel(rightPanel.upperPanel, SWT.NONE)(new HighlightingSymbolsViewer(_, SWT.NONE))
    val testPanel = new HorizontalResizableSplittedComposite(rootPanel.rightPanel, SWT.NONE, 20)

    val testText = new NotificationPanel(testPanel.upperPanel, SWT.NONE)(Some("Test Text"), new SourceText(_, SWT.NONE, emptyGrammarParser))
    testText.control.setText(initialTest)
    val testResultPanel = testPanel.lowerPanel
    testResultPanel.setLayout(new FormLayout)
    val parseTreeView = new NotificationPanel(testResultPanel, SWT.NONE)(None, new ParseTreeViewer(_, SWT.NONE))
    val parseProceedPanel = new Composite(testResultPanel, SWT.NONE)
    parseTreeView.setLayoutData({
        val d = new FormData()
        d.top = new FormAttachment(0)
        d.bottom = new FormAttachment(parseProceedPanel)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    })
    parseProceedPanel.setLayoutData({
        val d = new FormData()
        d.bottom = new FormAttachment(100)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    })
    parseProceedPanel.setLayout(new FormLayout)
    val proceedParserSelector = new ParserSelector(parseProceedPanel, SWT.NONE)
    val proceedButton = new Button(parseProceedPanel, SWT.NONE)
    proceedButton.setText("Proceed View")
    proceedButton.setLayoutData({
        val d = new FormData()
        d.bottom = new FormAttachment(100)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    })
    proceedParserSelector.setLayoutData({
        val d = new FormData()
        d.bottom = new FormAttachment(proceedButton)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    })

    grammarText.control.addProcessListener(new ProcessListener[TextModel, ParseResult[Option[Grammar]], ParseProcessor[Option[Grammar]]]() {
        def contentModified(value: TextModel): Unit = {
            Display.getDefault().asyncExec(new Runnable() {
                def run(): Unit = {
                    grammarText.showTextNotification("* Modified")
                }
            })
        }
        def processStarted(value: TextModel): Unit = {}
        def processCanceled(value: TextModel): Unit = {}

        //        val grammar = grammarDefParser.grammar
        //        val nontermNameId = (grammar.nsymbols find { _._2.symbol == Symbols.Nonterminal("NontermName") }).get._1
        //        val nontermNameIds = Set(nontermNameId) ++ grammar.reverseCorrespondingSymbols(nontermNameId)
        //        val termExactId = (grammar.nsymbols find { _._2.symbol == Symbols.Nonterminal("TerminalExactChar") }).get._1
        //        val termRangesId = (grammar.nsymbols find { _._2.symbol == Symbols.Nonterminal("TerminalRanges") }).get._1
        //        val termSetId = (grammar.nsymbols find { _._2.symbol == Symbols.Nonterminal("TerminalSet") }).get._1
        //        val termIds = Set(termExactId, termRangesId, termSetId) ++ grammar.reverseCorrespondingSymbols(termExactId) ++
        //            grammar.reverseCorrespondingSymbols(termRangesId) ++ grammar.reverseCorrespondingSymbols(termSetId)

        def processDone(value: TextModel, result: ParseResult[Option[Grammar]], processor: ParseProcessor[Option[Grammar]], time: Int): Unit = {
            Display.getDefault().asyncExec(new Runnable() {
                def run(): Unit = {
                    val parser = processor.parser
                    result match {
                        case ParseComplete(result, ctx) =>
                            val styleRange = new StyleRange()
                            styleRange.start = 0
                            styleRange.length = ctx.gen
                            styleRange.fontStyle = SWT.NONE
                            styleRange.background = ColorConstants.white
                            grammarText.control.text.setStyleRange(styleRange)

                            val basicNotiText = s"${value.length}, Parsed in $time ms"
                            result match {
                                case Some(grammar) =>
                                    val missingSymbols = grammar.missingSymbols
                                    if (missingSymbols.isEmpty) {
                                        grammarText.showTextNotification(basicNotiText + ", no missing symbols")
                                        testText.control.setProcessor(new ParseProcessor[ParseForest](NGrammar.fromGrammar(grammar), (x: ParseForest) => x))
                                    } else {
                                        grammarText.showTextNotification(basicNotiText + ", missing symbols: " + (missingSymbols map { _.toShortString }))
                                    }
                                case None =>
                                    grammarText.showTextNotification(basicNotiText + ", invalid grammar")
                            }
                        case IncompleteInput(msg, ctx) =>
                            grammarText.showTextNotification(s"$msg, expected: ${expectedTerminalFrom(parser, ctx) map { _.toShortString }}")
                        case UnexpectedInput(error, ctx) =>
                            grammarText.showTextNotification(s"${error.msg}, expected: ${expectedTerminalFrom(parser, ctx) map { _.toShortString }}")
                            val styleRange = new StyleRange()
                            styleRange.start = ctx.gen
                            styleRange.length = 1
                            styleRange.fontStyle = SWT.BOLD
                            styleRange.background = ColorConstants.red
                            grammarText.control.text.setStyleRange(styleRange)
                    }
                    // TODO 이렇게 하면 안될듯..
                    // - NontermName, Terminal 로 하지 말고 NontermDef, Production로 해야 정확히 될듯
                    // TODO
                }
            })
        }
    })
    testText.control.addProcessListener(new ProcessListener[TextModel, ParseResult[ParseForest], ParseProcessor[ParseForest]]() {
        def contentModified(value: TextModel): Unit = {
            Display.getDefault().asyncExec(new Runnable() {
                def run(): Unit = {
                    testText.showTextNotification("* Modified")
                }
            })
        }
        def processStarted(value: TextModel): Unit = {}
        def processCanceled(value: TextModel): Unit = {}

        def processDone(value: TextModel, result: ParseResult[ParseForest], processor: ParseProcessor[ParseForest], time: Int): Unit = {
            Display.getDefault().asyncExec(new Runnable() {
                def run(): Unit = {
                    val parser = processor.parser
                    result match {
                        case ParseComplete(result, ctx) =>
                            val styleRange = new StyleRange()
                            styleRange.start = 0
                            styleRange.length = value.length
                            styleRange.fontStyle = SWT.NONE
                            styleRange.background = ColorConstants.white
                            testText.control.text.setStyleRange(styleRange)
                            testText.showTextNotification(s"${value.length}, Parsed in $time ms (${result.trees.size} trees)")
                            parseTreeView.control.setParseForest(result)
                        case IncompleteInput(msg, ctx) =>
                            testText.showTextNotification(s"$msg, expected: ${expectedTerminalFrom(parser, ctx) map { _.toShortString }}")
                            parseTreeView.control.invalidateParseForest()
                        case UnexpectedInput(error, ctx) =>
                            testText.showTextNotification(s"${error.msg}, expected: ${expectedTerminalFrom(parser, ctx) map { _.toShortString }}")
                            val styleRanges = if (ctx.gen + 1 < value.length) {
                                val styleRanges = new Array[StyleRange](3)
                                val styleRange0 = new StyleRange()
                                styleRange0.start = 0
                                styleRange0.length = ctx.gen
                                styleRange0.fontStyle = SWT.NONE
                                styleRange0.background = ColorConstants.white
                                styleRanges(0) = styleRange0
                                val styleRange1 = new StyleRange()
                                styleRange1.start = ctx.gen
                                styleRange1.length = 1
                                styleRange1.fontStyle = SWT.BOLD
                                styleRange1.background = ColorConstants.red
                                styleRanges(1) = styleRange1
                                val styleRange2 = new StyleRange()
                                styleRange2.start = ctx.gen + 1
                                styleRange2.length = value.length - ctx.gen - 1
                                styleRange2.fontStyle = SWT.NONE
                                styleRange2.background = ColorConstants.white
                                styleRanges(2) = styleRange2
                                styleRanges
                            } else {
                                val styleRanges = new Array[StyleRange](2)
                                val styleRange0 = new StyleRange()
                                styleRange0.start = 0
                                styleRange0.length = ctx.gen
                                styleRange0.fontStyle = SWT.NONE
                                styleRange0.background = ColorConstants.white
                                styleRanges(0) = styleRange0
                                val styleRange1 = new StyleRange()
                                styleRange1.start = ctx.gen
                                styleRange1.length = 1
                                styleRange1.fontStyle = SWT.BOLD
                                styleRange1.background = ColorConstants.red
                                styleRanges(1) = styleRange1
                                styleRanges
                            }
                            testText.control.text.setStyleRanges(styleRanges)
                            parseTreeView.control.invalidateParseForest()
                    }
                }
            })
        }
    })
    proceedButton.addSelectionListener(new SelectionListener() {
        def widgetDefaultSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {}

        def widgetSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {
            grammarText.control.result match {
                case Some(ParseComplete(Some(grammar), _)) =>
                    val display = Display.getDefault()
                    val shell = new Shell(display)
                    val title = "Proceed View"
                    val ngrammar = NGrammar.fromGrammar(grammar)
                    val source = Inputs.fromString(testText.control.value.text)
                    if (!proceedParserSelector.preprocessed) {
                        ParsingProcessVisualizer.start[NaiveContext](
                            title = title,
                            parser = new NaiveParser(ngrammar, trim = proceedParserSelector.trimGraph),
                            source, display, shell,
                            (parent: Composite, style: Int, fig: NodeFigureGenerators[Figure], grammar: NGrammar, graph: ParsingContext.Graph, context: NaiveContext) =>
                                new ZestParsingContextWidget(parent, style, fig, grammar, graph, context)
                        )
                    } else {
                        //                        (proceedParserSelector.slice, proceedParserSelector.compact) match {
                        //                            case (false, false) =>
                        //                                ParsingProcessVisualizer.start[DeriveTipsWrappedContext](title, new PreprocessedParser(ngrammar, new OnDemandDerivationPreprocessor(ngrammar)), source, display, shell, new ZestDeriveTipParsingContextWidget(_, _, _, _, _))
                        //                            case (true, false) =>
                        //                                ParsingProcessVisualizer.start[DeriveTipsWrappedContext](title, new SlicedPreprocessedParser(ngrammar, new OnDemandSlicedDerivationPreprocessor(ngrammar)), source, display, shell, new ZestDeriveTipParsingContextWidget(_, _, _, _, _))
                        //                            case (false, true) =>
                        //                                ParsingProcessVisualizer.start[DeriveTipsWrappedContext](title, new PreprocessedParser(ngrammar, new OnDemandCompactDerivationPreprocessor(CompactNGrammar.fromNGrammar(ngrammar))), source, display, shell, new ZestDeriveTipParsingContextWidget(_, _, _, _, _))
                        //                            case (true, true) =>
                        //                                ParsingProcessVisualizer.start[DeriveTipsWrappedContext](title, new SlicedPreprocessedParser(ngrammar, new OnDemandCompactSlicedDerivationPreprocessor(CompactNGrammar.fromNGrammar(ngrammar))), source, display, shell, new ZestDeriveTipParsingContextWidget(_, _, _, _, _))
                        //                        }
                    }
                case _ => // TODO 어떻게 하지?
            }
        }
    })
    parserGenerationViewButton.addSelectionListener(new SelectionListener() {
        def widgetDefaultSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {}

        def widgetSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {
            grammarText.control.result match {
                case Some(ParseComplete(Some(grammar), _)) =>
                    val display = Display.getDefault()
                    val shell = new Shell(display)
                    val title = "Parser Generator View"
                    val ngrammar = NGrammar.fromGrammar(grammar)
                    // TODO
                case _ => // TODO 어떻게 하지?
            }
        }
    })

    addDisposeListener(new DisposeListener() {
        def widgetDisposed(e: org.eclipse.swt.events.DisposeEvent): Unit = {
            grammarText.control.stopWorkers()
            testText.control.stopWorkers()
            System.exit(0)
        }
    })
}

class NotificationPanel[T <: Control](parent: Composite, style: Int)(titleOpt: Option[String], childFunc: Composite => T) extends Composite(parent, style) {
    setLayout(new FormLayout())

    val titleLabelOpt: Option[Label] = titleOpt map { title =>
        val label = new Label(this, SWT.CENTER)
        label.setText(title)
        label
    }
    val control: T = childFunc(this)
    val notificationPanel = new Label(this, SWT.NONE)
    control.setLayoutData({
        val d = new FormData()
        d.top = titleLabelOpt match {
            case Some(titleLabel) => new FormAttachment(titleLabel)
            case None => new FormAttachment(0)
        }
        d.bottom = new FormAttachment(notificationPanel)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    })

    titleLabelOpt foreach { title =>
        title.setLayoutData({
            val d = new FormData()
            d.top = new FormAttachment(0)
            d.left = new FormAttachment(0)
            d.right = new FormAttachment(100)
            d
        })
    }
    val notificationLayoutDataVisible: FormData = {
        val d = new FormData()
        d.bottom = new FormAttachment(100)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    }
    val notificationLayoutDataInvisible: FormData = {
        val d = new FormData()
        d.top = new FormAttachment(100)
        d.bottom = new FormAttachment(100)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    }
    notificationPanel.setLayoutData(notificationLayoutDataInvisible)

    def hideNotification(): Unit = {
        notificationPanel.setLayoutData(notificationLayoutDataInvisible)
        layout()
    }
    def showTextNotification(text: String): Unit = {
        notificationPanel.setText(text)
        notificationPanel.setLayoutData(notificationLayoutDataVisible)
        layout()
    }
    def showTextNotification(text: String, time: Int): Unit = {
        // time (ms) 만큼 보여주고 숨기기
        notificationPanel.setText(text)
        notificationPanel.setLayoutData(notificationLayoutDataVisible)
        layout()
    }
}

class SourceText[R](parent: Composite, style: Int, val initialProcessor: ParseProcessor[R], val initialValue: TextModel)
        extends Composite(parent, style)
        with ParseableTextStorage[R] {
    def this(parent: Composite, style: Int, initialProcessor: ParseProcessor[R]) = this(parent, style, initialProcessor, WholeText(""))
    setLayout(new FillLayout())

    val text = new StyledText(this, SWT.V_SCROLL | SWT.H_SCROLL)
    text.setFont(BasicVisualizeResources.fixedWidth12Font)

    def setText(text: String): Unit = {
        this.text.setText(text)
        modified(TextModel(text))
    }

    def appendTextToCurrentCursor(appendingText: String): Unit = {
        val allText = this.text.getText()
        val offset = this.text.getCaretOffset
        setText(allText.substring(0, offset) + appendingText + allText.substring(offset))
        this.text.setCaretOffset(offset + 1)
    }

    text.addExtendedModifyListener(new ExtendedModifyListener() {
        def modifyText(e: org.eclipse.swt.custom.ExtendedModifyEvent): Unit = {
            val inserted = if (e.length == 0) {
                // no new text insertion
                ""
            } else {
                text.getText(e.start, e.start + e.length - 1)
            }
            val removedLength = e.replacedText.length()
            modified(value.modify(e.start, removedLength, inserted))
        }
    })
}

class ParserSelector(parent: Composite, style: Int) extends Composite(parent, style) {
    setLayout(new RowLayout())

    val trimButton = new Button(this, SWT.CHECK)
    trimButton.setText("Trim Graph")
    trimButton.setSelection(true)

    def trimGraph: Boolean = trimButton.getSelection

    // TODO 현재 미지원
    //    val preprocessedButton = new Button(this, SWT.CHECK)
    //    val sliceButton = new Button(this, SWT.CHECK)
    //    val compactButton = new Button(this, SWT.CHECK)
    //
    //    preprocessedButton.setText("Preprocessed")
    //    sliceButton.setText("Slice")
    //    compactButton.setText("Compact")
    //
    //    preprocessedButton.setSelection(false)
    //    sliceButton.setSelection(false)
    //    compactButton.setSelection(false)
    //
    def preprocessed: Boolean = false
    //    def preprocessed: Boolean = preprocessedButton.getSelection()
    //    def slice: Boolean = sliceButton.getSelection()
    //    def compact: Boolean = compactButton.getSelection()
    //
    //    private def enableButtons(): Unit = {
    //        sliceButton.setEnabled(preprocessed)
    //        compactButton.setEnabled(preprocessed)
    //    }
    //
    //    preprocessedButton.addSelectionListener(new SelectionListener() {
    //        def widgetDefaultSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = ???
    //        def widgetSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {
    //            enableButtons()
    //        }
    //    })
    //    enableButtons()
}

class HighlightingSymbolsViewer(parent: Composite, style: Int) extends Composite(parent, style) {
    setLayout(new FillLayout)

    val figureCanvas = new FigureCanvas(this, SWT.NONE)

    val figure = new org.eclipse.draw2d.Label("Highlighting Symbols TODO")
    figureCanvas.setContents(figure)
}

class ParseTreeViewer(parent: Composite, style: Int) extends Composite(parent, style) {
    setLayout(new FillLayout)

    val figureCanvas = new FigureCanvas(this, SWT.NONE)

    val figure = new org.eclipse.draw2d.Label("Parse Tree")
    figureCanvas.setContents(figure)

    val parseResultFigureGenerator = new ParseResultFigureGenerator[Figure](BasicVisualizeResources.nodeFigureGenerators.fig, BasicVisualizeResources.nodeFigureGenerators.appear)

    def setParseForest(parseForest: ParseForest): Unit = {
        // TODO figure 모양 개선(세로형으로)
        // TODO parse tree 안에 마우스 갖다대면 testText에 표시해주기
        figureCanvas.setContents(parseResultFigureGenerator.parseResultFigure(parseForest))
        figureCanvas.setBackground(ColorConstants.white)
    }
    def invalidateParseForest(): Unit = {
        figureCanvas.setBackground(ColorConstants.lightGray)
    }
}

trait ProcessListener[T, R, P] {
    def contentModified(value: T): Unit
    def processStarted(value: T): Unit
    def processCanceled(value: T): Unit
    def processDone(value: T, result: R, processor: P, time: Int): Unit

    def expectedTerminalFrom(parser: NaiveParser, ctx: NaiveContext): Set[Symbols.Terminal] = {
        ctx.nextGraph.nodes flatMap { node =>
            node.kernel.symbol match {
                case NTerminal(_, terminal) => Some(terminal)
                case _ => None
            }
        }
    }
}

trait ProcessableStorage[T, R, P <: (T => R)] {
    // (단일) Processor 관리
    // (여러개) ProcessListener 관리

    val debounceWait = 200

    val initialValue: T
    val initialProcessor: P

    private var _value: T = initialValue
    private var _dataVersion: Long = 0
    private var _processor: P = initialProcessor
    // _data를 _processor로 보내서 처리를 시작하면 None이 되고, 처리가 완료되면 Some(_)가 들어간다
    private var _result: Option[R] = None

    def value: T = this.synchronized { _value }
    def processor: P = this.synchronized { _processor }
    def result: Option[R] = this.synchronized { _result }

    private var processListeners = List[ProcessListener[T, R, P]]()

    def addProcessListener(listener: ProcessListener[T, R, P]): Unit = this.synchronized {
        processListeners +:= listener
    }

    def setProcessor(newProcessor: P): Unit = this.synchronized {
        _processor = newProcessor
        requestProcess()
    }

    private val threadQueue = new LinkedBlockingDeque[(T, Long)]()

    private var _running: Boolean = true
    private def running = this.synchronized { _running }

    class WorkerThread extends Runnable {
        def run(): Unit = {
            while (running) {
                val (value, version) = threadQueue.take()
                if (version >= 0) {
                    val start = ProcessableStorage.this.synchronized { (version == _dataVersion) }
                    if (start) {
                        val startTime: Long = System.currentTimeMillis()
                        val tempResult = _processor(_value)
                        val endTime: Long = System.currentTimeMillis()
                        ProcessableStorage.this.synchronized {
                            if (version == _dataVersion) {
                                _result = Some(tempResult)
                                processListeners foreach { _.processDone(_value, _result.get, processor, (endTime - startTime).toInt) }
                            } else {
                                processListeners foreach { _.processCanceled(_value) }
                            }
                        }
                    } else {
                        processListeners foreach { _.processCanceled(_value) }
                    }
                }
            }
        }
    }
    val workerCount = 1
    (0 until workerCount) foreach { _ => new Thread(new WorkerThread()).start() }

    def stopWorkers(): Unit = this.synchronized {
        println("Stopping Workers")
        _running = false
        // Waking threads up
        (0 until workerCount) foreach { _ => threadQueue.add((_value, -1)) }
    }

    private def requestProcess(): Unit = this.synchronized {
        _result = None
        // - processListener들에게 파싱 시작 이벤트 전달
        processListeners foreach { _.processStarted(_value) }
        // 별도 스레드에 처리를 요청하고 처리가 완료되었을 때 _dataVersion과 요청한 version을 비교해서 _result를 셋팅하고 parseListener들을 호출
        // TODO 별도 스레드에서 처리
        threadQueue.add((_value, _dataVersion))
    }

    def modified(newValue: T): Unit = this.synchronized {
        this._value = newValue
        this._dataVersion += 1
        processListeners foreach { _.contentModified(newValue) }
        requestProcess()
    }

    requestProcess()
}

sealed trait ParseResult[R] { val context: NaiveContext }
case class UnexpectedInput[R](error: ParsingError, context: NaiveContext) extends ParseResult[R]
case class IncompleteInput[R](message: String, context: NaiveContext) extends ParseResult[R]
case class ParseComplete[R](result: R, context: NaiveContext) extends ParseResult[R]

class ParseProcessor[R](val grammar: NGrammar, val parser: NaiveParser, postProcessor: ParseForest => R) extends (TextModel => ParseResult[R]) {
    // TODO Compact 지원하는 ParseTreeConstructor 구현한 뒤 Compact 사용하도록 수정
    // def this(grammar: CompactNGrammar) = this(grammar, new SlicedPreprocessedParser(grammar, new OnDemandCompactSlicedDerivationPreprocessor(grammar)))
    // def this(grammar: NGrammar) = this(CompactNGrammar.fromNGrammar(grammar))
    def this(grammar: NGrammar, postProcessor: ParseForest => R) =
        this(grammar, new NaiveParser(grammar), postProcessor)

    def apply(text: TextModel): ParseResult[R] = {
        // TODO 가능하면 변경된 부분만 파싱하도록 수정
        val wholeText = text.text
        val startTime = System.currentTimeMillis()
        val (lastCtx, result) = Inputs.fromString(wholeText).foldLeft[(NaiveContext, Either[NaiveContext, ParsingError])]((parser.initialContext, Left(parser.initialContext))) {
            (ctx, input) =>
                ctx match {
                    case (_, Left(ctx)) => (ctx, parser.proceed(ctx, input))
                    case (lastCtx, error @ Right(_)) => (lastCtx, error)
                }
        }
        val endTime = System.currentTimeMillis()
        println(endTime - startTime)
        result match {
            case Left(ctx) =>
                val reconstructor = new ParseTreeConstructor(ParseForestFunc)(grammar)(ctx.inputs, ctx.history, ctx.conditionFinal)
                reconstructor.reconstruct() match {
                    case Some(parseTree) => ParseComplete(postProcessor(parseTree), ctx)
                    case None => IncompleteInput("Incomplete input", ctx)
                }
            case Right(error) => UnexpectedInput(error, lastCtx)
        }
    }
}

trait ParseableTextStorage[R] extends ProcessableStorage[TextModel, ParseResult[R], ParseProcessor[R]]
