//package com.giyeok.jparser.studio
//
//import org.eclipse.swt.widgets.Display
//import org.eclipse.swt.widgets.Shell
//import org.eclipse.swt.widgets.Caret
//import org.eclipse.swt.SWT
//import org.eclipse.swt.layout.FillLayout
//import org.eclipse.swt.custom.StyledText
//import com.giyeok.jparser.visualize.BasicVisualizeResources
//import org.eclipse.swt.custom.StyleRange
//import org.eclipse.draw2d.ColorConstants
//import org.eclipse.swt.custom.ExtendedModifyListener
//import org.eclipse.swt.custom.ExtendedModifyEvent
//import com.giyeok.jparser.nparser.Parser
//import com.giyeok.jparser.nparser.Parser.WrappedContext
//import org.eclipse.swt.widgets.Composite
//import org.eclipse.swt.widgets.Control
//import com.giyeok.jparser.visualize.utils.VerticalResizableSplittedComposite
//import com.giyeok.jparser.visualize.utils.VerticalResizableSplittedComposite
//import org.eclipse.swt.layout.FormLayout
//import com.giyeok.jparser.visualize.utils.HorizontalResizableSplittedComposite
//import com.giyeok.jparser.visualize.VisualizeResources
//import org.eclipse.swt.layout.FormData
//import org.eclipse.swt.layout.FormAttachment
//import org.eclipse.swt.widgets.Button
//import org.eclipse.swt.layout.RowLayout
//import org.eclipse.swt.events.SelectionListener
//import org.eclipse.swt.widgets.Label
//import org.eclipse.swt.events.ModifyListener
//import org.eclipse.draw2d.FigureCanvas
//import org.eclipse.swt.widgets.Canvas
//import org.eclipse.swt.events.PaintListener
//import com.giyeok.jparser.ParseForest
//import com.giyeok.jparser.nparser.PreprocessedParser
//import com.giyeok.jparser.nparser.ParseTreeConstructor
//import com.giyeok.jparser.nparser.NGrammar
//import com.giyeok.jparser.gramgram.GrammarGrammar
//import com.giyeok.jparser.Grammar
//import scala.collection.immutable.ListMap
//import com.giyeok.jparser.Symbols.Nonterminal
//import com.giyeok.jparser.Symbols.Sequence
//import scala.collection.immutable.ListSet
//import com.giyeok.jparser.nparser.SlicedPreprocessedParser
//import com.giyeok.jparser.nparser.OnDemandCompactSlicedDerivationPreprocessor
//import com.giyeok.jparser.nparser.CompactNGrammar
//import com.giyeok.jparser.ParsingErrors.ParsingError
//import com.giyeok.jparser.ParseForestFunc
//import com.giyeok.jparser.nparser.OnDemandSlicedDerivationPreprocessor
//import com.giyeok.jparser.ParsingErrors
//import com.giyeok.jparser.nparser.ParsingContext
//import com.giyeok.jparser.nparser.Parser.DeriveTipsWrappedContext
//import com.giyeok.jparser.nparser.ParsingContext.SymbolKernel
//import com.giyeok.jparser.nparser.ParsingContext.SequenceKernel
//import java.util.concurrent.LinkedBlockingDeque
//import com.giyeok.jparser.Inputs
//import com.giyeok.jparser.nparser.Parser.DeriveTipsWrappedContext
//import com.giyeok.jparser.Symbols
//import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
//import com.giyeok.jparser.nparser.AcceptCondition
//import com.giyeok.jparser.nparser.ParsingContext.Node
//import org.eclipse.swt.events.DisposeListener
//import com.giyeok.jparser.nparser.CompactParseTreeConstructor
//import com.giyeok.jparser.visualize.ParseResultFigureGenerator
//import org.eclipse.draw2d.Figure
//import com.giyeok.jparser.visualize.ParsingProcessVisualizer
//import com.giyeok.jparser.nparser.NaiveParser
//import com.giyeok.jparser.visualize.ZestParsingContextWidget
//import com.giyeok.jparser.nparser.Parser.NaiveWrappedContext
//import com.giyeok.jparser.nparser.OnDemandDerivationPreprocessor
//import com.giyeok.jparser.nparser.OnDemandSlicedDerivationPreprocessor
//import com.giyeok.jparser.nparser.OnDemandCompactDerivationPreprocessor
//import com.giyeok.jparser.nparser.OnDemandCompactSlicedDerivationPreprocessor
//import com.giyeok.jparser.visualize.ZestDeriveTipParsingContextWidget
//import com.giyeok.jparser.visualize.PreprocessedDerivationViewer
//
//object ParserStudio {
//
//    def main(args: Array[String]): Unit = {
//        val display = new Display()
//        val shell = new Shell(display)
//
//        val studio = new ParserStudio(shell, SWT.NONE)
//        shell.setLayout(new FillLayout)
//
//        shell.open()
//        while (!shell.isDisposed()) {
//            if (!display.readAndDispatch()) {
//                display.sleep()
//            }
//        }
//        display.dispose()
//    }
//}
//
//class ParserStudio(parent: Composite, style: Int) extends Composite(parent, style) {
//    setLayout(new FillLayout)
//
//    val tempTestGrammar = """S = `Stmt+
//                              #Stmt = LetStmt
//                              #     | ExprStmt
//                              #LetStmt = Let ' ' Id ' ' Expr ';'
//                              #Let = Let0&Name
//                              #Let0 = 'l' 'e' 't'
//                              #Name = L(`[a-z]+)
//                              #Id = Name-Let
//                              #ExprStmt = Expr ';' la(LetStmt)
//                              #Token = '+' | Id
//                              #Expr = `Token+
//                              #`Stmt+ = `Stmt+ Stmt | Stmt
//                              #`Token+ = `Token+ Token | Token
//                              #`[a-z]+ = `[a-z]+ `[a-z] | `[a-z]
//                              #`[a-z] = [a-z]""".stripMargin('#') + "\n"
//
//    val rootPanel = new VerticalResizableSplittedComposite(this, SWT.NONE, 40)
//
//    val grammarDefParser = new ParseProcessor[Option[Grammar]](NGrammar.fromGrammar(GrammarGrammar), (x: ParseForest) => GrammarGrammar.translate(x.trees.head))
//
//    val emptyGrammarParser = new ParseProcessor[ParseForest](NGrammar.fromGrammar(new Grammar() {
//        val name: String = ""
//        val rules: RuleMap = ListMap("S" -> ListSet(Sequence(Seq())))
//        val startSymbol: Nonterminal = Nonterminal("S")
//    }), (x: ParseForest) => x)
//
//    // Grammar Panel
//    val grammarPanel = rootPanel.leftPanel
//    grammarPanel.setLayout(new FormLayout)
//    val grammarText = new NotificationPanel(grammarPanel, SWT.NONE)(new SourceText(_, SWT.NONE, grammarDefParser))
//    val grammarControlPanel = new Composite(grammarPanel, SWT.NONE)
//    val grammarInfoPanel = new Composite(grammarPanel, SWT.NONE)
//    grammarText.setLayoutData({
//        val d = new FormData()
//        d.top = new FormAttachment(grammarControlPanel)
//        d.bottom = new FormAttachment(grammarInfoPanel)
//        d.left = new FormAttachment(0)
//        d.right = new FormAttachment(100)
//        d
//    })
//    grammarControlPanel.setLayoutData({
//        val d = new FormData()
//        d.top = new FormAttachment(0)
//        d.left = new FormAttachment(0)
//        d.right = new FormAttachment(100)
//        d
//    })
//    grammarInfoPanel.setLayoutData({
//        val d = new FormData()
//        d.bottom = new FormAttachment(100)
//        d.left = new FormAttachment(0)
//        d.right = new FormAttachment(100)
//        d
//    })
//    grammarText.control.setText(tempTestGrammar)
//
//    grammarControlPanel.setLayout(new FillLayout)
//    val grammarOpenButton = new Button(grammarControlPanel, SWT.NONE)
//    grammarOpenButton.setText("Open")
//
//    grammarInfoPanel.setLayout(new FillLayout)
//    val definitionViewButton = new Button(grammarInfoPanel, SWT.NONE)
//    definitionViewButton.setText("Derivation View")
//    val parserGeneratorPanel = new Composite(grammarInfoPanel, SWT.NONE)
//    parserGeneratorPanel.setLayout(new FillLayout(SWT.VERTICAL))
//    val generateParserSelector = new ParserSelector(parserGeneratorPanel, SWT.NONE)
//    val generateButton = new Button(parserGeneratorPanel, SWT.NONE)
//    generateButton.setText("Generate Parser")
//
//    // Test Panel
//    val rightPanel = new HorizontalResizableSplittedComposite(rootPanel.rightPanel, SWT.NONE, 20)
//    val highlightingSymbols = new NotificationPanel(rightPanel.upperPanel, SWT.NONE)(new HighlightingSymbolsViewer(_, SWT.NONE))
//    val testPanel = new VerticalResizableSplittedComposite(rightPanel.lowerPanel, SWT.NONE)
//
//    val testText = new NotificationPanel(testPanel.leftPanel, SWT.NONE)(new SourceText(_, SWT.NONE, emptyGrammarParser))
//    val testResultPanel = testPanel.rightPanel
//    testResultPanel.setLayout(new FormLayout)
//    val parseTreeView = new NotificationPanel(testResultPanel, SWT.NONE)(new ParseTreeViewer(_, SWT.NONE))
//    val parseProceedPanel = new Composite(testResultPanel, SWT.NONE)
//    parseTreeView.setLayoutData({
//        val d = new FormData()
//        d.top = new FormAttachment(0)
//        d.bottom = new FormAttachment(parseProceedPanel)
//        d.left = new FormAttachment(0)
//        d.right = new FormAttachment(100)
//        d
//    })
//    parseProceedPanel.setLayoutData({
//        val d = new FormData()
//        d.bottom = new FormAttachment(100)
//        d.left = new FormAttachment(0)
//        d.right = new FormAttachment(100)
//        d
//    })
//    parseProceedPanel.setLayout(new FillLayout(SWT.VERTICAL))
//    val proceedParserSelector = new ParserSelector(parseProceedPanel, SWT.NONE)
//    val proceedButton = new Button(parseProceedPanel, SWT.NONE)
//    proceedButton.setText("Proceed View")
//
//    grammarText.control.addProcessListener(new ProcessListener[TextModel, ParseResult[Option[Grammar]], ParseProcessor[Option[Grammar]]]() {
//        def contentModified(value: TextModel): Unit = {
//            Display.getDefault().asyncExec(new Runnable() {
//                def run(): Unit = {
//                    grammarText.showTextNotification("* Modified")
//                }
//            })
//        }
//        def processStarted(value: TextModel): Unit = {}
//        def processCanceled(value: TextModel): Unit = {}
//
//        val grammar = grammarDefParser.grammar
//        val nontermNameId = (grammar.nsymbols find { _._2.symbol == Symbols.Nonterminal("NontermName") }).get._1
//        val nontermNameIds = Set(nontermNameId) ++ grammar.reverseCorrespondingSymbols(nontermNameId)
//        val termExactId = (grammar.nsymbols find { _._2.symbol == Symbols.Nonterminal("TerminalExactChar") }).get._1
//        val termRangesId = (grammar.nsymbols find { _._2.symbol == Symbols.Nonterminal("TerminalRanges") }).get._1
//        val termSetId = (grammar.nsymbols find { _._2.symbol == Symbols.Nonterminal("TerminalSet") }).get._1
//        val termIds = Set(termExactId, termRangesId, termSetId) ++ grammar.reverseCorrespondingSymbols(termExactId) ++
//            grammar.reverseCorrespondingSymbols(termRangesId) ++ grammar.reverseCorrespondingSymbols(termSetId)
//
//        def expectedTerminalFrom(parser: PreprocessedParser, ctx: DeriveTipsWrappedContext): Set[Symbols.Terminal] = {
//            val terms = ctx.deriveTips flatMap { node =>
//                parser.derivation.termNodesOf(node) map { _.symbolId }
//            }
//            terms map { parser.grammar.nsymbols(_).symbol.asInstanceOf[Symbols.Terminal] }
//        }
//
//        def processDone(value: TextModel, result: ParseResult[Option[Grammar]], processor: ParseProcessor[Option[Grammar]], time: Int): Unit = {
//            Display.getDefault().asyncExec(new Runnable() {
//                def run(): Unit = {
//                    val parser = processor.parser
//                    result match {
//                        case ParseComplete(result, ctx) =>
//                            val basicNotiText = s"${value.length}, Parsed in $time ms"
//                            result match {
//                                case Some(grammar) =>
//                                    val missingSymbols = grammar.missingSymbols
//                                    if (missingSymbols.isEmpty) {
//                                        grammarText.showTextNotification(basicNotiText + ", no missing symbols")
//                                        testText.control.setProcessor(new ParseProcessor[ParseForest](NGrammar.fromGrammar(grammar), (x: ParseForest) => x))
//                                    } else {
//                                        grammarText.showTextNotification(basicNotiText + ", missing symbols: " + (missingSymbols map { _.toShortString }))
//                                    }
//                                case None =>
//                                    grammarText.showTextNotification(basicNotiText + ", invalid grammar")
//                            }
//                        case IncompleteInput(msg, ctx) =>
//                            grammarText.showTextNotification(s"$msg, expected: ${expectedTerminalFrom(parser, ctx) map { _.toShortString }}")
//                        case UnexpectedInput(error, ctx) =>
//                            grammarText.showTextNotification(s"${error.msg}, expected: ${expectedTerminalFrom(parser, ctx) map { _.toShortString }}")
//                            val styleRange = new StyleRange()
//                            styleRange.start = ctx.gen
//                            styleRange.length = 1
//                            styleRange.fontStyle = SWT.BOLD
//                            styleRange.background = ColorConstants.red
//                            grammarText.control.text.setStyleRange(styleRange)
//                    }
//                    // TODO 이렇게 하면 안될듯..
//                    // - NontermName, Terminal 로 하지 말고 NontermDef, Production로 해야 정확히 될듯
//                    val finishes = {
//                        def eligible(conditions: Set[AcceptCondition]): Boolean = {
//                            conditions exists { result.context.conditionFate.of(_).acceptable }
//                        }
//                        (result.context.history map {
//                            _.nodeConditions.toSet[(Node, Set[AcceptCondition])] collect {
//                                case (node, conditions) if eligible(conditions) => node
//                            }
//                        }).toVector
//                    }
//                    def isFinished(node: Node, symbolIds: Set[Int]): Boolean = {
//                        node match {
//                            case SymbolKernel(symbolId, _) => symbolIds contains symbolId
//                            case SequenceKernel(sequenceId, pointer, _, _) =>
//                                (symbolIds contains sequenceId) && (pointer + 1 == grammar.nsequences(sequenceId).sequence.length)
//                        }
//                    }
//                    finishes.zipWithIndex foreach { finIdx =>
//                        finIdx._1 collect {
//                            case node if isFinished(node, nontermNameIds) =>
//                                val endGen = finIdx._2
//                                val styleRange = new StyleRange()
//                                styleRange.start = node.beginGen
//                                styleRange.length = endGen - node.beginGen
//                                styleRange.fontStyle = SWT.BOLD
//                                styleRange.underline = true
//                                styleRange.underlineColor = ColorConstants.lightBlue
//                                styleRange.foreground = ColorConstants.blue
//                                grammarText.control.text.setStyleRange(styleRange)
//                            case node if isFinished(node, termIds) =>
//                                val endGen = finIdx._2
//                                val styleRange = new StyleRange()
//                                styleRange.start = node.beginGen
//                                styleRange.length = endGen - node.beginGen
//                                styleRange.fontStyle = SWT.BOLD
//                                styleRange.foreground = ColorConstants.red
//                                grammarText.control.text.setStyleRange(styleRange)
//                        }
//                    }
//                }
//            })
//        }
//    })
//    testText.control.addProcessListener(new ProcessListener[TextModel, ParseResult[ParseForest], ParseProcessor[ParseForest]]() {
//        def contentModified(value: TextModel): Unit = {
//            Display.getDefault().asyncExec(new Runnable() {
//                def run(): Unit = {
//                    testText.showTextNotification("* Modified")
//                }
//            })
//        }
//        def processStarted(value: TextModel): Unit = {}
//        def processCanceled(value: TextModel): Unit = {}
//
//        def expectedTerminalFrom(parser: PreprocessedParser, ctx: DeriveTipsWrappedContext): Set[Symbols.Terminal] = {
//            val terms = ctx.deriveTips flatMap { node =>
//                parser.derivation.termNodesOf(node) map { _.symbolId }
//            }
//            terms map { parser.grammar.nsymbols(_).symbol.asInstanceOf[Symbols.Terminal] }
//        }
//
//        def processDone(value: TextModel, result: ParseResult[ParseForest], processor: ParseProcessor[ParseForest], time: Int): Unit = {
//            Display.getDefault().asyncExec(new Runnable() {
//                def run(): Unit = {
//                    val parser = processor.parser
//                    result match {
//                        case ParseComplete(result, ctx) =>
//                            val styleRange = new StyleRange()
//                            styleRange.start = 0
//                            styleRange.length = value.length
//                            styleRange.fontStyle = SWT.NONE
//                            styleRange.background = ColorConstants.white
//                            testText.control.text.setStyleRange(styleRange)
//                            testText.showTextNotification(s"${value.length}, Parsed in $time ms (${result.trees.size} trees)")
//                            parseTreeView.control.setParseForest(result)
//                        case IncompleteInput(msg, ctx) =>
//                            testText.showTextNotification(s"$msg, expected: ${expectedTerminalFrom(parser, ctx) map { _.toShortString }}")
//                            parseTreeView.control.invalidateParseForest()
//                        case UnexpectedInput(error, ctx) =>
//                            testText.showTextNotification(s"${error.msg}, expected: ${expectedTerminalFrom(parser, ctx) map { _.toShortString }}")
//                            val styleRanges = if (ctx.gen + 1 < value.length) {
//                                val styleRanges = new Array[StyleRange](3)
//                                val styleRange0 = new StyleRange()
//                                styleRange0.start = 0
//                                styleRange0.length = ctx.gen
//                                styleRange0.fontStyle = SWT.NONE
//                                styleRange0.background = ColorConstants.white
//                                styleRanges(0) = styleRange0
//                                val styleRange1 = new StyleRange()
//                                styleRange1.start = ctx.gen
//                                styleRange1.length = 1
//                                styleRange1.fontStyle = SWT.BOLD
//                                styleRange1.background = ColorConstants.red
//                                styleRanges(1) = styleRange1
//                                val styleRange2 = new StyleRange()
//                                styleRange2.start = ctx.gen + 1
//                                styleRange2.length = value.length - ctx.gen - 1
//                                styleRange2.fontStyle = SWT.NONE
//                                styleRange2.background = ColorConstants.white
//                                styleRanges(2) = styleRange2
//                                styleRanges
//                            } else {
//                                val styleRanges = new Array[StyleRange](2)
//                                val styleRange0 = new StyleRange()
//                                styleRange0.start = 0
//                                styleRange0.length = ctx.gen
//                                styleRange0.fontStyle = SWT.NONE
//                                styleRange0.background = ColorConstants.white
//                                styleRanges(0) = styleRange0
//                                val styleRange1 = new StyleRange()
//                                styleRange1.start = ctx.gen
//                                styleRange1.length = 1
//                                styleRange1.fontStyle = SWT.BOLD
//                                styleRange1.background = ColorConstants.red
//                                styleRanges(1) = styleRange1
//                                styleRanges
//                            }
//                            testText.control.text.setStyleRanges(styleRanges)
//                            parseTreeView.control.invalidateParseForest()
//                    }
//                }
//            })
//        }
//    })
//    proceedButton.addSelectionListener(new SelectionListener() {
//        def widgetDefaultSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {}
//
//        def widgetSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {
//            grammarText.control.result match {
//                case Some(ParseComplete(Some(grammar), _)) =>
//                    val display = Display.getDefault()
//                    val shell = new Shell(display)
//                    val title = "Proceed View"
//                    val ngrammar = NGrammar.fromGrammar(grammar)
//                    val source = Inputs.fromString(testText.control.value.text)
//                    if (!proceedParserSelector.preprocessed) {
//                        ParsingProcessVisualizer.start[NaiveWrappedContext](title, new NaiveParser(ngrammar), source, display, shell, new ZestParsingContextWidget(_, _, _, _, _))
//                    } else {
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
//                    }
//                case _ => // TODO 어떻게 하지?
//            }
//        }
//    })
//    definitionViewButton.addSelectionListener(new SelectionListener() {
//        def widgetDefaultSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {}
//
//        def widgetSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {
//            grammarText.control.result match {
//                case Some(ParseComplete(Some(grammar), _)) =>
//                    val display = Display.getDefault()
//                    val shell = new Shell(display)
//                    val title = "Proceed View"
//                    val ngrammar = NGrammar.fromGrammar(grammar)
//                    new PreprocessedDerivationViewer(grammar, ngrammar, new OnDemandSlicedDerivationPreprocessor(ngrammar), new OnDemandCompactSlicedDerivationPreprocessor(CompactNGrammar.fromNGrammar(ngrammar)), BasicVisualizeResources.nodeFigureGenerators, display, new Shell(display)).start()
//                case _ => // TODO 어떻게 하지?
//            }
//        }
//    })
//
//    addDisposeListener(new DisposeListener() {
//        def widgetDisposed(e: org.eclipse.swt.events.DisposeEvent): Unit = {
//            grammarText.control.stopWorkers()
//            testText.control.stopWorkers()
//        }
//    })
//}
//
//class NotificationPanel[T <: Control](parent: Composite, style: Int)(childFunc: Composite => T) extends Composite(parent, style) {
//    setLayout(new FormLayout())
//
//    val control = childFunc(this)
//    val notificationPanel = new Label(this, SWT.NONE)
//    control.setLayoutData({
//        val d = new FormData()
//        d.top = new FormAttachment(0)
//        d.bottom = new FormAttachment(notificationPanel)
//        d.left = new FormAttachment(0)
//        d.right = new FormAttachment(100)
//        d
//    })
//
//    val notificationLayoutDataVisible = {
//        val d = new FormData()
//        d.bottom = new FormAttachment(100)
//        d.left = new FormAttachment(0)
//        d.right = new FormAttachment(100)
//        d
//    }
//    val notificationLayoutDataInvisible = {
//        val d = new FormData()
//        d.top = new FormAttachment(100)
//        d.bottom = new FormAttachment(100)
//        d.left = new FormAttachment(0)
//        d.right = new FormAttachment(100)
//        d
//    }
//    notificationPanel.setLayoutData(notificationLayoutDataInvisible)
//
//    def hideNotification(): Unit = {
//        notificationPanel.setLayoutData(notificationLayoutDataInvisible)
//        layout()
//    }
//    def showTextNotification(text: String): Unit = {
//        notificationPanel.setText(text)
//        notificationPanel.setLayoutData(notificationLayoutDataVisible)
//        layout()
//    }
//    def showTextNotification(text: String, time: Int): Unit = {
//        // time (ms) 만큼 보여주고 숨기기
//        notificationPanel.setText(text)
//        notificationPanel.setLayoutData(notificationLayoutDataVisible)
//        layout()
//    }
//}
//
//class SourceText[R](parent: Composite, style: Int, val initialProcessor: ParseProcessor[R], val initialValue: TextModel)
//        extends Composite(parent, style)
//        with ParseableTextStorage[R] {
//    def this(parent: Composite, style: Int, initialProcessor: ParseProcessor[R]) = this(parent, style, initialProcessor, WholeText(""))
//    setLayout(new FillLayout())
//
//    val text = new StyledText(this, SWT.V_SCROLL | SWT.H_SCROLL)
//    text.setFont(BasicVisualizeResources.fixedWidth12Font)
//
//    def setText(text: String): Unit = {
//        this.text.setText(text)
//        modified(TextModel(text))
//    }
//
//    text.addExtendedModifyListener(new ExtendedModifyListener() {
//        def modifyText(e: org.eclipse.swt.custom.ExtendedModifyEvent): Unit = {
//            val inserted = if (e.length == 0) {
//                // no new text insertion
//                ""
//            } else {
//                text.getText(e.start, e.start + e.length - 1)
//            }
//            val removedLength = e.replacedText.length()
//            modified(value.modify(e.start, removedLength, inserted))
//        }
//    })
//}
//
//class ParserSelector(parent: Composite, style: Int) extends Composite(parent, style) {
//    setLayout(new RowLayout())
//
//    val preprocessedButton = new Button(this, SWT.CHECK)
//    val sliceButton = new Button(this, SWT.CHECK)
//    val compactButton = new Button(this, SWT.CHECK)
//
//    preprocessedButton.setText("Preprocessed")
//    sliceButton.setText("Slice")
//    compactButton.setText("Compact")
//
//    preprocessedButton.setSelection(true)
//    sliceButton.setSelection(true)
//    compactButton.setSelection(true)
//
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
//}
//
//class HighlightingSymbolsViewer(parent: Composite, style: Int) extends Composite(parent, style) {
//    setLayout(new FillLayout)
//
//    val figureCanvas = new FigureCanvas(this, SWT.NONE)
//
//    val figure = new org.eclipse.draw2d.Label("Highlighting Symbols TODO")
//    figureCanvas.setContents(figure)
//}
//
//class ParseTreeViewer(parent: Composite, style: Int) extends Composite(parent, style) {
//    setLayout(new FillLayout)
//
//    val figureCanvas = new FigureCanvas(this, SWT.NONE)
//
//    val figure = new org.eclipse.draw2d.Label("Parse Tree")
//    figureCanvas.setContents(figure)
//
//    val parseResultFigureGenerator = new ParseResultFigureGenerator[Figure](BasicVisualizeResources.nodeFigureGenerators.fig, BasicVisualizeResources.nodeFigureGenerators.appear)
//
//    def setParseForest(parseForest: ParseForest): Unit = {
//        // TODO figure 모양 개선(세로형으로)
//        // TODO parse tree 안에 마우스 갖다대면 testText에 표시해주기
//        figureCanvas.setContents(parseResultFigureGenerator.parseResultFigure(parseForest))
//        figureCanvas.setBackground(ColorConstants.white)
//    }
//    def invalidateParseForest(): Unit = {
//        figureCanvas.setBackground(ColorConstants.lightGray)
//    }
//}
//
//trait ProcessListener[T, R, P] {
//    def contentModified(value: T): Unit
//    def processStarted(value: T): Unit
//    def processCanceled(value: T): Unit
//    def processDone(value: T, result: R, processor: P, time: Int): Unit
//}
//
//trait ProcessableStorage[T, R, P <: (T => R)] {
//    // (단일) Processor 관리
//    // (여러개) ProcessListener 관리
//
//    val debounceWait = 200
//
//    val initialValue: T
//    val initialProcessor: P
//
//    private var _value: T = initialValue
//    private var _dataVersion: Long = 0
//    private var _processor: P = initialProcessor
//    // _data를 _processor로 보내서 처리를 시작하면 None이 되고, 처리가 완료되면 Some(_)가 들어간다
//    private var _result: Option[R] = None
//
//    def value: T = this.synchronized { _value }
//    def processor: P = this.synchronized { _processor }
//    def result: Option[R] = this.synchronized { _result }
//
//    private var processListeners = List[ProcessListener[T, R, P]]()
//
//    def addProcessListener(listener: ProcessListener[T, R, P]): Unit = this.synchronized {
//        processListeners +:= listener
//    }
//
//    def setProcessor(newProcessor: P): Unit = this.synchronized {
//        _processor = newProcessor
//        requestProcess()
//    }
//
//    private val threadQueue = new LinkedBlockingDeque[(T, Long)]()
//
//    private var _running: Boolean = true
//    private def running = this.synchronized { _running }
//
//    class WorkerThread extends Runnable {
//        def run(): Unit = {
//            while (running) {
//                val (value, version) = threadQueue.take()
//                if (version >= 0) {
//                    val start = ProcessableStorage.this.synchronized { (version == _dataVersion) }
//                    if (start) {
//                        val startTime: Long = System.currentTimeMillis()
//                        val tempResult = _processor(_value)
//                        val endTime: Long = System.currentTimeMillis()
//                        ProcessableStorage.this.synchronized {
//                            if (version == _dataVersion) {
//                                _result = Some(tempResult)
//                                processListeners foreach { _.processDone(_value, _result.get, processor, (endTime - startTime).toInt) }
//                            } else {
//                                processListeners foreach { _.processCanceled(_value) }
//                            }
//                        }
//                    } else {
//                        processListeners foreach { _.processCanceled(_value) }
//                    }
//                }
//            }
//        }
//    }
//    val workerCount = 1
//    (0 until workerCount) foreach { _ => new Thread(new WorkerThread()).start() }
//
//    def stopWorkers(): Unit = this.synchronized {
//        println("Stopping Workers")
//        _running = false
//        // Waking threads up
//        (0 until workerCount) foreach { _ => threadQueue.add((_value, -1)) }
//    }
//
//    private def requestProcess(): Unit = this.synchronized {
//        _result = None
//        // - processListener들에게 파싱 시작 이벤트 전달
//        processListeners foreach { _.processStarted(_value) }
//        // 별도 스레드에 처리를 요청하고 처리가 완료되었을 때 _dataVersion과 요청한 version을 비교해서 _result를 셋팅하고 parseListener들을 호출
//        // TODO 별도 스레드에서 처리
//        threadQueue.add((_value, _dataVersion))
//    }
//
//    def modified(newValue: T): Unit = this.synchronized {
//        this._value = newValue
//        this._dataVersion += 1
//        processListeners foreach { _.contentModified(newValue) }
//        requestProcess()
//    }
//
//    requestProcess()
//}
//
//sealed trait ParseResult[R] { val context: DeriveTipsWrappedContext }
//case class UnexpectedInput[R](error: ParsingError, context: DeriveTipsWrappedContext) extends ParseResult[R]
//case class IncompleteInput[R](message: String, context: DeriveTipsWrappedContext) extends ParseResult[R]
//case class ParseComplete[R](result: R, context: DeriveTipsWrappedContext) extends ParseResult[R]
//
//class ParseProcessor[R](val grammar: CompactNGrammar, val parser: PreprocessedParser, postProcessor: ParseForest => R) extends (TextModel => ParseResult[R]) {
//    // TODO Compact 지원하는 ParseTreeConstructor 구현한 뒤 Compact 사용하도록 수정
//    // def this(grammar: CompactNGrammar) = this(grammar, new SlicedPreprocessedParser(grammar, new OnDemandCompactSlicedDerivationPreprocessor(grammar)))
//    // def this(grammar: NGrammar) = this(CompactNGrammar.fromNGrammar(grammar))
//    def this(grammar: NGrammar, postProcessor: ParseForest => R) = this(CompactNGrammar.fromNGrammar(grammar), new SlicedPreprocessedParser(grammar, new OnDemandSlicedDerivationPreprocessor(grammar)), postProcessor)
//    def this(grammar: Grammar, postProcessor: ParseForest => R) = this(NGrammar.fromGrammar(grammar), postProcessor)
//
//    def apply(text: TextModel): ParseResult[R] = {
//        // TODO 가능하면 변경된 부분만 파싱하도록 수정
//        val wholeText = text.text
//        val startTime = System.currentTimeMillis()
//        val (lastCtx, result) = Inputs.fromString(wholeText).foldLeft[(DeriveTipsWrappedContext, Either[DeriveTipsWrappedContext, ParsingError])]((parser.initialContext, Left(parser.initialContext))) {
//            (ctx, input) =>
//                ctx match {
//                    case (_, Left(ctx)) => (ctx, parser.proceed(ctx, input))
//                    case (lastCtx, error @ Right(_)) => (lastCtx, error)
//                }
//        }
//        val endTime = System.currentTimeMillis()
//        println(endTime - startTime)
//        result match {
//            case Left(ctx) =>
//                val reconstructor = new ParseTreeConstructor(ParseForestFunc)(grammar)(ctx.inputs, ctx.history, ctx.conditionFate)
//                reconstructor.reconstruct() match {
//                    case Some(parseTree) => ParseComplete(postProcessor(parseTree), ctx)
//                    case None => IncompleteInput("Incomplete input", ctx)
//                }
//            case Right(error) => UnexpectedInput(error, lastCtx)
//        }
//    }
//}
//
//trait ParseableTextStorage[R] extends ProcessableStorage[TextModel, ParseResult[R], ParseProcessor[R]]
