package com.giyeok.jparser.visualize

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.ParsingContext.Kernel
import com.giyeok.jparser.nparser.ParsingContext.Node
import com.giyeok.jparser.npreparser.DerivationPreprocessor
import com.giyeok.jparser.visualize.utils.HorizontalResizableSplittedComposite
import com.giyeok.jparser.visualize.utils.VerticalResizableSplittedComposite
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.MouseListener
import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.List
import org.eclipse.swt.widgets.Shell
import org.eclipse.zest.core.widgets.CGraphNode
import org.eclipse.zest.core.widgets.GraphConnection
import org.eclipse.zest.core.widgets.ZestStyles

class PreprocessedDerivationViewer(grammar: Grammar, ngrammar: NGrammar,
        derivationPreprocessor: DerivationPreprocessor,
        nodeFig: NodeFigureGenerators[Figure], display: Display, shell: Shell) extends Composite(shell, SWT.NONE) {
    setLayout(new FillLayout())

    val splitPanel = new VerticalResizableSplittedComposite(this, SWT.NONE, 20)

    val leftPanel = new HorizontalResizableSplittedComposite(splitPanel.leftPanel, SWT.NONE, 70)

    val kernelsList = new FigureCanvas(leftPanel.upperPanel, SWT.NONE)
    val (kernelsListFig, kernelFigsList) = {
        val fig = new Figure
        fig.setLayoutManager({
            val l = new ToolbarLayout(false)
            l.setSpacing(3)
            l
        })
        var _kernelFigsList = scala.collection.immutable.List[((Int, Int), Figure)]()

        def boxing(symbol: Figure): Figure = {
            val box = new Figure
            box.setLayoutManager(new ToolbarLayout)
            box.add(symbol)
            box.setBorder(new LineBorder)
            box.setOpaque(true)
            box.setBackgroundColor(ColorConstants.white)
            box
        }

        def addListener(figure: Figure, symbolId: Int, pointer: Int): Figure = {
            figure.addMouseListener(new MouseListener() {
                def mousePressed(e: org.eclipse.draw2d.MouseEvent): Unit = {
                    setShownKernel(symbolId, pointer)
                }
                def mouseReleased(e: org.eclipse.draw2d.MouseEvent): Unit = {}
                def mouseDoubleClicked(e: org.eclipse.draw2d.MouseEvent): Unit = {}
            })
            _kernelFigsList +:= ((symbolId, pointer) -> figure)
            figure
        }

        (ngrammar.nsymbols ++ ngrammar.nsequences).toSeq sortBy { _._1 } foreach { kv =>
            val (symbolId, symbol) = kv
            0 until Kernel.lastPointerOf(symbol) foreach { pointer =>
                val figure = nodeFig.symbol.symbolPointerFig(ngrammar, symbolId, pointer)
                fig.add(addListener(boxing(figure), symbolId, pointer))
            }
        }
        (fig, _kernelFigsList.reverse)
    }
    kernelsList.setContents(kernelsListFig)

    val termGroupsList = new List(leftPanel.lowerPanel, SWT.NONE)

    leftPanel.lowerPanel.setLayout(new FormLayout())
    termGroupsList.setLayoutData({
        val formData = new FormData()
        formData.top = new FormAttachment(0, 0) //(compactionToggle)
        formData.bottom = new FormAttachment(100, 0)
        formData.left = new FormAttachment(0, 0)
        formData.right = new FormAttachment(100, 0)
        formData
    })

    val graphView = new Composite(splitPanel.rightPanel, SWT.NONE)
    val graphStackLayout = new StackLayout()
    graphView.setLayout(graphStackLayout)

    private val graphControlsMap = scala.collection.mutable.Map[(Int, Int, Option[TermGroupDesc]), Control]()
    def graphControlOf(symbolId: Int, pointer: Int, termGroupOpt: Option[TermGroupDesc]): Control = {
        graphControlsMap get (symbolId, pointer, termGroupOpt) match {
            case Some(control) => control
            case None =>
                val derivePreprocessed = derivationPreprocessor.preprocessedOf(symbolId, pointer)
                val control = termGroupOpt match {
                    case Some(termGroup) =>
                        new PreprocessedSlicedDerivationGraphWidget(graphView, SWT.NONE, nodeFig, ngrammar, derivePreprocessed, derivePreprocessed.slices(termGroup))
                    case None =>
                        new PreprocessedDerivationGraphWidget(graphView, SWT.NONE, nodeFig, ngrammar, derivePreprocessed)
                }
                graphControlsMap((symbolId, pointer, termGroupOpt)) = control
                control
        }
    }

    private var shownKernelOpt: Option[(Int, Int)] = None
    private var shownTermGroupList = Seq[Option[TermGroupDesc]]()
    private var shownTermGroup = Option.empty[TermGroupDesc]

    def setShownKernel(symbolId: Int, pointer: Int): Unit = {
        shownKernelOpt foreach { shownKernel =>
            kernelFigsList find { _._1 == shownKernel } foreach {
                _._2.setBackgroundColor(ColorConstants.white)
            }
        }
        shownKernelOpt = None
        kernelFigsList find { _._1 == (symbolId, pointer) } foreach { kernelFig =>
            shownKernelOpt = Some(kernelFig._1)
            kernelFig._2.setBackgroundColor(ColorConstants.lightGray)

            val newTermGroups = derivationPreprocessor.preprocessedOf(symbolId, pointer).slices.keys.toSeq
            shownTermGroup = None
            termGroupsList.removeAll()
            shownTermGroupList = None +: (newTermGroups map { Some(_) })
            termGroupsList.add("All")
            newTermGroups foreach { termGroup => termGroupsList.add(termGroup.toShortString) }
            termGroupsList.select(0)
        }

        updateGraphView()
    }

    def updateGraphView(): Unit = {
        val idx = termGroupsList.getSelectionIndex
        if (idx >= 0 && idx < shownTermGroupList.length) {
            shownKernelOpt foreach { shownKernel =>
                graphStackLayout.topControl = graphControlOf(shownKernel._1, shownKernel._2, shownTermGroupList(idx))
            }
            graphView.layout()
        }
    }

    termGroupsList.addSelectionListener(new SelectionListener() {
        def widgetDefaultSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {}
        def widgetSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {
            updateGraphView()
        }
    })
    setShownKernel(ngrammar.startSymbol, 0)
    updateGraphView()

    private def keyListener = new KeyListener {
        override def keyReleased(e: KeyEvent): Unit = {}
        override def keyPressed(e: KeyEvent): Unit = {
            e.keyCode match {
                case SWT.ARROW_UP =>
                    shownKernelOpt foreach { shownKernel =>
                        kernelFigsList.zipWithIndex find { _._1._1 == shownKernel } foreach { p =>
                            val newIdx = p._2 - 1
                            if (newIdx >= 0) {
                                val k = kernelFigsList(newIdx)._1
                                setShownKernel(k._1, k._2)
                            }
                        }
                    }
                case SWT.ARROW_DOWN =>
                    shownKernelOpt foreach { shownKernel =>
                        kernelFigsList.zipWithIndex find { _._1._1 == shownKernel } foreach { p =>
                            val newIdx = p._2 + 1
                            if (newIdx < kernelFigsList.length) {
                                val k = kernelFigsList(newIdx)._1
                                setShownKernel(k._1, k._2)
                            }
                        }
                    }
                case 'z' | 'Z' =>
                    val selection = termGroupsList.getSelectionIndex
                    if (selection >= 0) {
                        val newSelection = selection - 1
                        if (newSelection >= 0) {
                            termGroupsList.setSelection(newSelection)
                            updateGraphView()
                        }
                    }
                case 'x' | 'X' =>
                    val selection = termGroupsList.getSelectionIndex
                    if (selection >= 0) {
                        val newSelection = selection + 1
                        if (newSelection < termGroupsList.getItemCount) {
                            termGroupsList.setSelection(newSelection)
                            updateGraphView()
                        }
                    }
                case _ => // nothing to do
            }
        }
    }

    shell.addKeyListener(keyListener)
    kernelsList.addKeyListener(keyListener)

    def start(): Unit = {
        shell.setText("Derivation Graph")
        shell.setLayout(new FillLayout)
        shell.open()
    }
}

class PreprocessedDerivationGraphWidget(
        parent: Composite, style: Int,
        fig: NodeFigureGenerators[Figure], grammar: NGrammar,
        preprocessed: DerivationPreprocessor#DerivePreprocessed
) extends ZestGraphWidget(parent, style, fig, grammar, preprocessed.graph) with TipNodes {
    val progressConditionEdgeColor: Color = ColorConstants.yellow

    override def initialize(): Unit = {
        super.initialize()
        setTipNodeBorder(preprocessed.baseNode)

        if (preprocessed.baseNodeTasks.nonEmpty) {
            val baseNode = nodesMap(preprocessed.baseNode)
            preprocessed.baseNodeTasks.foreach { progress =>
                val conditionFig = fig.conditionFig(grammar, progress.condition)
                conditionFig.setBackgroundColor(ColorConstants.buttonLightest)
                conditionFig.setOpaque(true)
                conditionFig.setBorder(new LineBorder(ColorConstants.darkGray))
                conditionFig.setSize(conditionFig.getPreferredSize())
                val conditionNode = new CGraphNode(graphCtrl, SWT.NONE, conditionFig)

                val conn = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_SOLID, baseNode, conditionNode)
                conn.setLineColor(progressConditionEdgeColor)
            }
        }
    }
}

class PreprocessedSlicedDerivationGraphWidget(
        parent: Composite, style: Int,
        fig: NodeFigureGenerators[Figure], grammar: NGrammar,
        base: DerivationPreprocessor#DerivePreprocessed, sliced: DerivationPreprocessor#ProgressPreprocessed
) extends ZestGraphTransitionWidget(parent, style, fig, grammar, base.graph, sliced.graph) with TipNodes {
    val progressConditionEdgeColor: Color = ColorConstants.yellow

    // assert(preprocessed.baseNode == sliced._1.baseNode)
    override def initialize(): Unit = {
        super.initialize()
        setTipNodeBorder(base.baseNode)
        sliced.nextDeriveTips foreach { setTipNodeBorder }

        if (sliced.baseNodeTasks.nonEmpty) {
            val baseNode = nodesMap(sliced.baseNode)
            sliced.baseNodeTasks.foreach { progress =>
                val conditionFig = fig.conditionFig(grammar, progress.condition)
                conditionFig.setBackgroundColor(ColorConstants.buttonLightest)
                conditionFig.setOpaque(true)
                conditionFig.setBorder(new LineBorder(ColorConstants.darkGray))
                conditionFig.setSize(conditionFig.getPreferredSize())
                val conditionNode = new CGraphNode(graphCtrl, SWT.NONE, conditionFig)

                val conn = new GraphConnection(graphCtrl, ZestStyles.CONNECTIONS_SOLID, baseNode, conditionNode)
                conn.setLineColor(progressConditionEdgeColor)
            }
        }
    }
}
