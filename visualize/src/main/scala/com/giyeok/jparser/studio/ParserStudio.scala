package com.giyeok.jparser.studio

import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Caret
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.custom.StyledText
import com.giyeok.jparser.visualize.BasicVisualizeResources
import org.eclipse.swt.custom.StyleRange
import org.eclipse.draw2d.ColorConstants
import org.eclipse.swt.custom.ExtendedModifyListener
import org.eclipse.swt.custom.ExtendedModifyEvent
import com.giyeok.jparser.nparser.Parser
import com.giyeok.jparser.nparser.Parser.WrappedContext
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import com.giyeok.jparser.visualize.utils.VerticalResizableSplittedComposite
import com.giyeok.jparser.visualize.utils.VerticalResizableSplittedComposite
import org.eclipse.swt.layout.FormLayout
import com.giyeok.jparser.visualize.utils.HorizontalResizableSplittedComposite
import com.giyeok.jparser.visualize.VisualizeResources
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.events.ModifyListener
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.swt.widgets.Canvas
import org.eclipse.swt.events.PaintListener
import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.nparser.PreprocessedParser
import com.giyeok.jparser.ParseResult
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.gramgram.GrammarGrammar
import com.giyeok.jparser.Grammar
import scala.collection.immutable.ListMap
import com.giyeok.jparser.Symbols.Nonterminal
import com.giyeok.jparser.Symbols.Sequence
import scala.collection.immutable.ListSet
import com.giyeok.jparser.nparser.SlicedPreprocessedParser
import com.giyeok.jparser.nparser.OnDemandCompactSlicedDerivationPreprocessor
import com.giyeok.jparser.nparser.CompactNGrammar
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.nparser.OnDemandSlicedDerivationPreprocessor
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.nparser.ParsingContext
import com.giyeok.jparser.nparser.Parser.DeriveTipsWrappedContext
import com.giyeok.jparser.nparser.ParsingContext.SymbolNode
import com.giyeok.jparser.nparser.ParsingContext.SequenceNode

object ParserStudio {

    def main(args: Array[String]): Unit = {
        val display = new Display()
        val shell = new Shell(display)

        val studio = new ParserStudio(shell, SWT.NONE)
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

class ParserStudio(parent: Composite, style: Int) extends Composite(parent, style) {
    setLayout(new FillLayout)

    val tempTestGrammar = """S = `Stmt+
                              #Stmt = LetStmt
                              #     | ExprStmt
                              #LetStmt = Let ' ' Id ' ' Expr ';'
                              #Let = Let0&Name
                              #Let0 = 'l' 'e' 't'
                              #Name = L(`[a-z]+)
                              #Id = Name-Let
                              #ExprStmt = Expr ';' la(LetStmt)
                              #Token = '+' | Id
                              #Expr = `Token+
                              #`Stmt+ = `Stmt+ Stmt | Stmt
                              #`Token+ = `Token+ Token | Token
                              #`[a-z]+ = `[a-z]+ `[a-z] | `[a-z]
                              #`[a-z] = [a-z]""".stripMargin('#') + "\n"

    val rootPanel = new VerticalResizableSplittedComposite(this, SWT.NONE, 40)

    val grammarDefParser = new ParseProcessor(NGrammar.fromGrammar(GrammarGrammar))

    val emptyGrammarParser = new ParseProcessor(NGrammar.fromGrammar(new Grammar() {
        val name: String = ""
        val rules: RuleMap = ListMap("S" -> ListSet(Sequence(Seq())))
        val startSymbol: Nonterminal = Nonterminal("S")
    }))

    // Grammar Panel
    val grammarPanel = rootPanel.leftPanel
    grammarPanel.setLayout(new FormLayout)
    val grammarText = new NotificationPanel(grammarPanel, SWT.NONE)(new SourceText(_, SWT.NONE, grammarDefParser))
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
    grammarText.control.setText(tempTestGrammar)

    grammarControlPanel.setLayout(new FillLayout)
    val grammarOpenButton = new Button(grammarControlPanel, SWT.NONE)
    grammarOpenButton.setText("Open")

    grammarInfoPanel.setLayout(new FillLayout)
    val definitionViewButton = new Button(grammarInfoPanel, SWT.NONE)
    definitionViewButton.setText("Derivation View")
    val parserGeneratorPanel = new Composite(grammarInfoPanel, SWT.NONE)
    parserGeneratorPanel.setLayout(new FillLayout(SWT.VERTICAL))
    val generateParserSelector = new ParserSelector(parserGeneratorPanel, SWT.NONE)
    val generateButton = new Button(parserGeneratorPanel, SWT.NONE)
    generateButton.setText("Generate Parser")

    // Test Panel
    val rightPanel = new HorizontalResizableSplittedComposite(rootPanel.rightPanel, SWT.NONE, 20)
    val highlightingSymbols = new NotificationPanel(rightPanel.upperPanel, SWT.NONE)(new HighlightingSymbolsViewer(_, SWT.NONE))
    val testPanel = new VerticalResizableSplittedComposite(rightPanel.lowerPanel, SWT.NONE)

    val testText = new NotificationPanel(testPanel.leftPanel, SWT.NONE)(new SourceText(_, SWT.NONE, emptyGrammarParser))
    val testResultPanel = testPanel.rightPanel
    testResultPanel.setLayout(new FormLayout)
    val parseTreeView = new NotificationPanel(testResultPanel, SWT.NONE)(new ParseTreeViewer(_, SWT.NONE))
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
    parseProceedPanel.setLayout(new FillLayout(SWT.VERTICAL))
    val proceedParserSelector = new ParserSelector(parseProceedPanel, SWT.NONE)
    val proceedButton = new Button(parseProceedPanel, SWT.NONE)
    proceedButton.setText("Proceed View")

    grammarText.control.addProcessListener(new ProcessListener[TextModel, ParseResult, ParseProcessor]() {
        def contentModified(value: TextModel): Unit = {}
        def processStarted(value: TextModel): Unit = {
            /*
            grammarText.showTextNotification(value match {
                case DiffText(_, change, _) => change.toString
                case _ => ""
            })
            */
        }
        def processCanceled(value: TextModel): Unit = {}
        def processDone(value: TextModel, result: ParseResult, processor: ParseProcessor, time: Int): Unit = {
            Display.getDefault().syncExec(new Runnable() {
                def run(): Unit = {
                    result match {
                        case ParseComplete(result, ctx) =>
                            grammarText.showTextNotification(s"Parsed in $time ms, ${result.trees.size} tree(s)")
                        case UnexpectedInput(error) =>
                            grammarText.showTextNotification(error.msg)
                        case IncompleteInput(ctx) =>
                            /*
                    val grammar = processor.parser.grammar
                    val s = ctx.deriveTips map {
                        case SymbolNode(symbolId, beginGen) =>
                            s"${grammar.nsymbols(symbolId).symbol.toShortString} from $beginGen"
                        case SequenceNode(sequenceId, pointer, beginGen, _) =>
                            s"${grammar.nsequences(sequenceId).symbol.toShortString} from $beginGen"
                    }
                    grammarText.showTextNotification(s mkString ",")
                    */
                            grammarText.showTextNotification("Incomplete input")
                    }
                }
            })
        }
    })
}

class NotificationPanel[T <: Control](parent: Composite, style: Int)(childFunc: Composite => T) extends Composite(parent, style) {
    setLayout(new FormLayout())

    val control = childFunc(this)
    val notificationPanel = new Label(this, SWT.NONE)
    control.setLayoutData({
        val d = new FormData()
        d.top = new FormAttachment(0)
        d.bottom = new FormAttachment(notificationPanel)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    })

    val notificationLayoutDataVisible = {
        val d = new FormData()
        d.bottom = new FormAttachment(100)
        d.left = new FormAttachment(0)
        d.right = new FormAttachment(100)
        d
    }
    val notificationLayoutDataInvisible = {
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

class SourceText(parent: Composite, style: Int, val initialProcessor: ParseProcessor, val initialValue: TextModel)
        extends Composite(parent, style)
        with ParseableTextStorage {
    def this(parent: Composite, style: Int, initialProcessor: ParseProcessor) = this(parent, style, initialProcessor, WholeText(""))
    setLayout(new FillLayout())

    val text = new StyledText(this, SWT.V_SCROLL | SWT.H_SCROLL)
    text.setFont(BasicVisualizeResources.fixedWidth12Font)

    def setText(text: String): Unit = {
        this.text.setText(text)
        modified(TextModel(text))
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

    val preprocessedButton = new Button(this, SWT.CHECK)
    val sliceButton = new Button(this, SWT.CHECK)
    val compactButton = new Button(this, SWT.CHECK)

    preprocessedButton.setText("Preprocessed")
    sliceButton.setText("Slice")
    compactButton.setText("Compact")

    preprocessedButton.setSelection(true)
    sliceButton.setSelection(true)
    compactButton.setSelection(true)

    def preprocessed: Boolean = preprocessedButton.getSelection()
    def slice: Boolean = sliceButton.getSelection()
    def compact: Boolean = compactButton.getSelection()

    private def enableButtons(): Unit = {
        sliceButton.setEnabled(preprocessed)
        compactButton.setEnabled(preprocessed)
    }

    preprocessedButton.addSelectionListener(new SelectionListener() {
        def widgetDefaultSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = ???
        def widgetSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {
            enableButtons()
        }
    })
    enableButtons()
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

    val figure = new org.eclipse.draw2d.Label("Parse Tree TODO")
    figureCanvas.setContents(figure)
}

trait ProcessListener[T, R, P] {
    def contentModified(value: T): Unit
    def processStarted(value: T): Unit
    def processCanceled(value: T): Unit
    def processDone(value: T, result: R, processor: P, time: Int): Unit
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

    private def requestProcess(): Unit = this.synchronized {
        _result = None
        // 별도 스레드에 처리를 요청하고 처리가 완료되었을 때 _dataVersion과 요청한 version을 비교해서 _result를 셋팅하고 parseListener들을 호출
        // TODO 별도 스레드에서 처리
        val startTime: Long = System.currentTimeMillis()
        _result = Some(_processor(_value))
        val endTime: Long = System.currentTimeMillis()
        processListeners foreach { _.processDone(_value, _result.get, processor, (endTime - startTime).toInt) }
    }

    def modified(newValue: T): Unit = this.synchronized {
        // TODO
        // - 필요하면 100~200 ms 기다려서 debounce 처리해주고,
        // - parser로 보내서 파싱을 시작하고
        this._value = newValue
        requestProcess()
        // - processListener들에게 파싱 시작 이벤트 전달
        processListeners foreach { _.processStarted(newValue) }
    }
}

sealed trait ParseResult
case class UnexpectedInput(error: ParsingError) extends ParseResult
case class IncompleteInput(context: DeriveTipsWrappedContext) extends ParseResult
case class ParseComplete(result: ParseForest, context: DeriveTipsWrappedContext) extends ParseResult

class ParseProcessor(val parser: PreprocessedParser) extends (TextModel => ParseResult) {
    // def this(grammar: CompactNGrammar) = this(new SlicedPreprocessedParser(grammar, new OnDemandCompactSlicedDerivationPreprocessor(grammar)))
    // def this(grammar: NGrammar) = this(CompactNGrammar.fromNGrammar(grammar))
    def this(grammar: NGrammar) = this(new SlicedPreprocessedParser(grammar, new OnDemandSlicedDerivationPreprocessor(grammar)))
    def this(grammar: Grammar) = this(NGrammar.fromGrammar(grammar))

    def apply(text: TextModel): ParseResult = {
        // TODO 가능하면 변경된 부분만 파싱하도록 수정
        parser.parse(text.text) match {
            case Left(ctx) =>
                val reconstructor = new ParseTreeConstructor(ParseForestFunc)(parser.grammar)(ctx.inputs, ctx.history, ctx.conditionFate)
                reconstructor.reconstruct() match {
                    case Some(parseTree) => ParseComplete(parseTree, ctx)
                    case None => IncompleteInput(ctx)
                }
            case Right(error) => UnexpectedInput(error)
        }
    }
}

trait ParseableTextStorage extends ProcessableStorage[TextModel, ParseResult, ParseProcessor]
