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

class PreprocessedDerivationViewer(grammar: Grammar, ngrammar: NGrammar, derivationPreprocessor: DerivationPreprocessor, nodeFig: NodeFigureGenerators[Figure], display: Display, shell: Shell) extends Composite(shell, SWT.NONE) {

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
                    fig.add(addListener(boxing(nodeFig.symbol.symbolFig(symbol.symbol)), symbolId, 0))
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

    val graphView = new Composite(splitPanel.rightPanel, SWT.NONE)
    val graphStackLayout = new StackLayout()
    graphView.setLayout(graphStackLayout)

    val graphControlsMap = scala.collection.mutable.Map[(Int, Int), PreprocessedDerivationGraphWidget]()
    def graphControlOf(symbolId: Int, pointer: Int): PreprocessedDerivationGraphWidget = {
        graphControlsMap get (symbolId, pointer) match {
            case Some(control) => control
            case None =>
                val control =
                    if (ngrammar.nsymbols contains symbolId) new PreprocessedDerivationGraphWidget(graphView, SWT.NONE, nodeFig, ngrammar, derivationPreprocessor.symbolDerivationOf(symbolId))
                    else new PreprocessedDerivationGraphWidget(graphView, SWT.NONE, nodeFig, ngrammar, derivationPreprocessor.sequenceDerivationOf(symbolId, pointer))
                graphControlsMap((symbolId, pointer)) = control
                control
        }
    }

    var shownKernel = (-1, -1)
    def showKernel(symbolId: Int, pointer: Int): Unit = {
        kernelFigsMap get shownKernel foreach { _.setBackgroundColor(ColorConstants.white) }

        shownKernel = (symbolId, pointer)
        kernelFigsMap(shownKernel).setBackgroundColor(ColorConstants.lightGray)
        graphStackLayout.topControl = graphControlOf(symbolId, pointer)
        graphView.layout()
    }
    showKernel(ngrammar.startSymbol, 0)

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

