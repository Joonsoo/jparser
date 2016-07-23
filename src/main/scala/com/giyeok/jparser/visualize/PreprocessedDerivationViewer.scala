package com.giyeok.jparser.visualize

import org.eclipse.swt.widgets.Composite
import com.giyeok.jparser.nparser.NGrammar
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Shell
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.visualize.utils.HorizontalResizableSplittedComposite
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Label
import com.giyeok.jparser.visualize.utils.VerticalResizableSplittedComposite
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.draw2d.LineBorder
import com.giyeok.jparser.nparser.NGrammar.NAtomicSymbol
import com.giyeok.jparser.nparser.NGrammar.Sequence
import org.eclipse.draw2d.MouseListener
import org.eclipse.draw2d.ColorConstants
import org.eclipse.swt.custom.StackLayout
import com.giyeok.jparser.nparser.DerivationPreprocessor
import com.giyeok.jparser.nparser.DerivationPreprocessor.Preprocessed
import com.giyeok.jparser.nparser.ParsingContext.Node
import com.giyeok.jparser.nparser.ParsingContext.SymbolNode
import com.giyeok.jparser.nparser.ParsingContext.SequenceNode
import com.giyeok.jparser.visualize.FigureGenerator.Spacing
import org.eclipse.swt.widgets.List
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.nparser.SlicedDerivationPreprocessor
import org.eclipse.swt.events.SelectionListener

class PreprocessedDerivationViewer(grammar: Grammar, ngrammar: NGrammar, derivationPreprocessor: SlicedDerivationPreprocessor, nodeFig: NodeFigureGenerators[Figure], display: Display, shell: Shell) extends Composite(shell, SWT.NONE) {

    setLayout(new FillLayout())

    val splitPanel = new VerticalResizableSplittedComposite(this, SWT.NONE, 20)

    val leftPanel = new HorizontalResizableSplittedComposite(splitPanel.leftPanel, SWT.NONE, 70)

    val kernelsList = new FigureCanvas(leftPanel.upperPanel, SWT.NONE)
    val (kernelsListFig, kernelFigsMap) = {
        val fig = new Figure
        fig.setLayoutManager({
            val l = new ToolbarLayout(false)
            l.setSpacing(3)
            l
        })
        var kernelFigsMap = scala.collection.mutable.Map[(Int, Int), Figure]()

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
                    showKernel(symbolId, pointer)
                }
                def mouseReleased(e: org.eclipse.draw2d.MouseEvent): Unit = {}
                def mouseDoubleClicked(e: org.eclipse.draw2d.MouseEvent): Unit = {}
            })
            kernelFigsMap((symbolId, pointer)) = figure
            figure
        }

        (ngrammar.nsymbols ++ ngrammar.nsequences).toSeq sortBy { _._1 } foreach { kv =>
            val (symbolId, symbol) = kv
            symbol match {
                case symbol: NAtomicSymbol =>
                    fig.add(addListener(boxing(nodeFig.symbol.symbolFig(symbol.symbol)), symbolId, -1))
                case symbol: Sequence =>
                    if (symbol.sequence.isEmpty) {
                        fig.add(addListener(boxing(nodeFig.symbol.sequenceFig(symbol.symbol, 0)), symbolId, 0))
                    } else {
                        (0 until symbol.sequence.length) foreach { pointer =>
                            fig.add(addListener(boxing(nodeFig.symbol.sequenceFig(symbol.symbol, pointer)), symbolId, pointer))
                        }
                    }
            }
        }
        (fig, kernelFigsMap.toMap)
    }
    kernelsList.setContents(kernelsListFig)

    val termGroupsList = new List(leftPanel.lowerPanel, SWT.NONE)

    val graphView = new Composite(splitPanel.rightPanel, SWT.NONE)
    val graphStackLayout = new StackLayout()
    graphView.setLayout(graphStackLayout)

    val graphsMap = scala.collection.mutable.Map[(Int, Int), (Preprocessed, Map[TermGroupDesc, Preprocessed])]()
    val graphControlsMap = scala.collection.mutable.Map[(Int, Int, Option[TermGroupDesc]), PreprocessedDerivationGraphWidget]()
    def graphControlOf(symbolId: Int, pointer: Int, termGroup: Option[TermGroupDesc]): PreprocessedDerivationGraphWidget = {
        graphControlsMap get (symbolId, pointer, termGroup) match {
            case Some(control) => control
            case None =>
                val preprocessed = termGroup match {
                    case Some(termGroup) => graphsMap((symbolId, pointer))._2(termGroup)
                    case None => graphsMap((symbolId, pointer))._1
                }
                val control = new PreprocessedDerivationGraphWidget(graphView, SWT.NONE, nodeFig, ngrammar, preprocessed)
                graphControlsMap((symbolId, pointer, termGroup)) = control
                control
        }
    }

    var shownKernel = (-1, -1)
    var shownTermGroupList = Seq[Option[TermGroupDesc]]()
    var shownTermGroup = Option.empty[TermGroupDesc]

    def updateHighlight(newKernel: (Int, Int)): Unit = {
        kernelFigsMap get shownKernel foreach { _.setBackgroundColor(ColorConstants.white) }
        shownKernel = newKernel
        kernelFigsMap(shownKernel).setBackgroundColor(ColorConstants.lightGray)
    }
    def updateTermGroups(newTermGroups: Seq[TermGroupDesc]): Unit = {
        shownTermGroup = None
        termGroupsList.removeAll()
        shownTermGroupList = None +: (newTermGroups map { Some(_) })
        termGroupsList.add("All")
        newTermGroups foreach { termGroup => termGroupsList.add(termGroup.toShortString) }
        termGroupsList.select(0)
    }
    def showKernel(symbolId: Int, pointer: Int): Unit = {
        updateHighlight(symbolId, pointer)
        val (derivation, slice) = if (pointer < 0) {
            (derivationPreprocessor.symbolDerivationOf(symbolId), derivationPreprocessor.symbolSliceOf(symbolId))
        } else {
            (derivationPreprocessor.sequenceDerivationOf(symbolId, pointer), derivationPreprocessor.sequenceSliceOf(symbolId, pointer))
        }
        graphsMap((symbolId, pointer)) = (derivation, slice)
        updateTermGroups(slice.keys.toSeq)
        updateGraphView(graphControlOf(symbolId, pointer, None))
    }
    def updateGraphView(widget: PreprocessedDerivationGraphWidget): Unit = {
        graphStackLayout.topControl = widget
        graphView.layout()
    }
    termGroupsList.addSelectionListener(new SelectionListener() {
        def widgetDefaultSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {}
        def widgetSelected(e: org.eclipse.swt.events.SelectionEvent): Unit = {
            val idx = termGroupsList.getSelectionIndex()
            if (idx >= 0 && idx < shownTermGroupList.length) {
                updateGraphView(graphControlOf(shownKernel._1, shownKernel._2, shownTermGroupList(idx)))
            }
        }
    })
    showKernel(ngrammar.startSymbol, -1)

    def start(): Unit = {
        shell.setText("Derivation Graph")
        shell.setLayout(new FillLayout)
        shell.open()
    }
}

object PreprocessedDerivationGraphWidget {
    class BaseNodeFigureGenerators[Fig](fig: FigureGenerator.Generator[Fig], appear: FigureGenerator.Appearances[Fig], symbol: SymbolFigureGenerator[Fig])
            extends NodeFigureGenerators(fig, appear, symbol) {
        override def nodeFig(grammar: NGrammar, node: Node): Fig = node match {
            case SymbolNode(symbolId, beginGen) if beginGen < 0 =>
                fig.horizontalFig(Spacing.Big, Seq(
                    fig.textFig(s"$symbolId", appear.small),
                    symbolFigure(grammar, symbolId)))
            case SequenceNode(sequenceId, pointer, beginGen, endGen) if beginGen < 0 && endGen < 0 =>
                fig.horizontalFig(Spacing.Big, Seq(
                    fig.textFig(s"$sequenceId", appear.small),
                    sequenceFigure(grammar, sequenceId, pointer)))
            case node => super.nodeFig(grammar, node)
        }
    }

    def baseNodeFigGen(fig: NodeFigureGenerators[Figure]) = new BaseNodeFigureGenerators(fig.fig, fig.appear, fig.symbol)
}

class PreprocessedDerivationGraphWidget(parent: Composite, style: Int, fig: NodeFigureGenerators[Figure], grammar: NGrammar, preprocessed: Preprocessed)
        extends ZestGraphWidget(parent, style, PreprocessedDerivationGraphWidget.baseNodeFigGen(fig), grammar, preprocessed.context) {
    override def initialize(): Unit = {
        super.initialize()
        val shownBaseNode = nodesMap(preprocessed.baseNode)
        shownBaseNode.getFigure().setBorder(new LineBorder(ColorConstants.orange, 3))
        val size = shownBaseNode.getFigure().getPreferredSize
        shownBaseNode.setSize(size.width, size.height)
    }
}

