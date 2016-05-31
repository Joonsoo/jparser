package com.giyeok.jparser.visualize
import com.giyeok.jparser.Grammar
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.layout.GridLayout
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.custom.StackLayout
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.ToolbarLayout
import com.giyeok.jparser.Symbols._
import FigureGenerator.Spacing
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.MouseListener
import org.eclipse.draw2d.MouseEvent
import com.giyeok.jparser.DerivationFunc
import com.giyeok.jparser.ParseForestFunc
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Control

class NewParserDerivationGraphVisualizer(grammar: Grammar, display: Display, shell: Shell, resources: VisualizeResources) extends BasicGenerators with KernelFigureGenerator[Figure] {
    val derivationFunc = new DerivationFunc(grammar, ParseForestFunc)

    val kernelList = new FigureCanvas(shell, SWT.NONE)
    kernelList.setLayoutData({
        val d = new GridData(GridData.FILL_VERTICAL)
        d.widthHint = 200
        d
    })
    kernelList.setBackground(ColorConstants.buttonLightest)

    val layout = new StackLayout

    val derivationGraphs = new Composite(shell, SWT.NONE)
    derivationGraphs.setLayout(layout)
    derivationGraphs.setLayoutData(new GridData(GridData.FILL_BOTH))
    derivationGraphs.setBackground(ColorConstants.white)

    type Kernel = Either[AtomicSymbol, (Sequence, Int)]

    val dgraphCache = scala.collection.mutable.Map[Kernel, Control]()

    val (kernelListFig, buttonsMap) = {
        val fig = new Figure
        fig.setLayoutManager({
            val l = new ToolbarLayout(false)
            l.setSpacing(3)
            l
        })

        def boxing(symbol: Figure): Figure = {
            val box = new Figure
            box.setLayoutManager(new ToolbarLayout)
            box.add(symbol)
            box.setBorder(new LineBorder)
            box.setOpaque(true)
            box
        }
        def setData(symbol: Figure, data: Kernel): Figure = {
            symbol.addMouseListener(new MouseListener() {
                def mouseDoubleClicked(e: MouseEvent): Unit = {}
                def mousePressed(e: MouseEvent): Unit = {
                    println(data)
                    updateKernel(data)
                }
                def mouseReleased(e: MouseEvent): Unit = {}
            })
            symbol
        }

        var map = Map[Kernel, Figure]()
        grammar.usedSymbols foreach {
            case s: AtomicNonterm =>
                val b = setData(boxing(atomicFigure(s)), Left(s))
                map += Left(s) -> b
                fig.add(b)
            case s: Sequence =>
                (0 until s.seq.length) foreach { pointer =>
                    val b = setData(boxing(sequenceFigure(s, pointer)), Right((s, pointer)))
                    map += Right((s, pointer)) -> b
                    fig.add(b)
                }
            case _ =>
                Seq()
        }

        (fig, map)
    }
    kernelList.setContents(kernelListFig)

    def updateKernel(selected: Kernel): Unit = {
        buttonsMap foreach { _._2.setBackgroundColor(ColorConstants.white) }
        buttonsMap(selected).setBackgroundColor(ColorConstants.buttonDarker)

        dgraphCache get selected match {
            case Some(graph) =>
                layout.topControl = graph
                derivationGraphs.layout()
            case None =>
                val dgraph = selected match {
                    case Left(symbol: AtomicNonterm) => new DerivationGraphVisualizeWidget(derivationGraphs, SWT.NONE, grammar, new NodeIdCache, derivationFunc.deriveAtomic(symbol))
                    case Left(symbol) =>
                        val l = new Label(derivationGraphs, SWT.NONE); l.setText("Terminal node has no derivation graph"); l
                    case Right((symbol, pointer)) => new DerivationGraphVisualizeWidget(derivationGraphs, SWT.NONE, grammar, new NodeIdCache, derivationFunc.deriveSequence(symbol, pointer))
                }
                dgraphCache(selected) = dgraph
                layout.topControl = dgraph
                derivationGraphs.layout()
        }
    }

    def initialize(): Unit = {
        updateKernel(Left(Start))
    }

    def start(): Unit = {
        shell.setText("Derivation Graph")
        shell.setLayout({
            val l = new GridLayout(2, false)
            l.marginWidth = 0
            l.marginHeight = 0
            l.verticalSpacing = 0
            l.horizontalSpacing = 0
            l
        })

        initialize()

        shell.open()
    }
}

object NewParserDerivationGraphVisualizer {
    def start(grammar: Grammar, display: Display, shell: Shell): Unit = {
        val resources = BasicVisualizeResources
        new NewParserDerivationGraphVisualizer(grammar, display, shell, resources).start()
    }

    def start(grammar: Grammar): Unit = {
        val display = Display.getDefault()
        val shell = new Shell(display)

        start(grammar, display, shell)

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }
}
