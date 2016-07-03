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
import com.giyeok.jparser.ParseResultDerivationsSetFunc
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.List
import com.giyeok.jparser.DGraph
import com.giyeok.jparser.Inputs.TermGroupDesc
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Event
import com.giyeok.jparser.ParsingGraph
import DerivationGraphVisualizer.Kernel
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.events.ShellListener
import com.giyeok.jparser.DerivationSliceFunc
import com.giyeok.jparser.ParseResultDerivationsSet

class DerivationGraphVisualizer(grammar: Grammar, display: Display, shell: Shell, resources: VisualizeResources, defaultKernel: Kernel) extends BasicGenerators with KernelFigureGenerator[Figure] {
    val derivationFunc = new DerivationSliceFunc(grammar, ParseResultDerivationsSetFunc)

    shell.setLayout(new FillLayout)

    val sash = new SashForm(shell, SWT.HORIZONTAL)

    val kernelList = new FigureCanvas(sash, SWT.NONE)
    kernelList.setBackground(ColorConstants.white)

    val termGroupsList = new List(sash, SWT.NONE)
    var termGroupsItems = Seq[Option[TermGroupDesc]]() // None은 전체를 의미

    val layout = new StackLayout

    val derivationGraphs = new Composite(sash, SWT.NONE)
    derivationGraphs.setLayout(layout)
    derivationGraphs.setBackground(ColorConstants.buttonDarkest)

    sash.setBackground(ColorConstants.lightGray)
    sash.setWeights(Seq[Int](1, 1, 3).toArray)

    case class DGraphWidget(
            dgraph: DGraph[ParseResultDerivationsSet]) {
        val nodeIdCache = new NodeIdCache

        val sliceMap: Map[TermGroupDesc, (DGraph[ParseResultDerivationsSet], Set[ParsingGraph.NontermNode])] = derivationFunc.sliceByTermGroups(dgraph)

        val graphWidget: DerivationGraphVisualizeWidget =
            new DerivationGraphVisualizeWidget(derivationGraphs, SWT.NONE, grammar, nodeIdCache, dgraph)

        def showAllGraph(): Unit = {
            layout.topControl = graphWidget
            derivationGraphs.layout()
        }

        val sliceWidgetCache = scala.collection.mutable.Map[TermGroupDesc, Control]()

        def sliceWidgetOf(termGroupDesc: TermGroupDesc): Control = {
            sliceWidgetCache get termGroupDesc match {
                case Some(widget) => widget
                case None =>
                    val slice = sliceMap(termGroupDesc)
                    val widget = new DerivationSliceGraphVisualizeWidget(derivationGraphs, SWT.NONE, grammar, nodeIdCache, dgraph, slice._1, slice._2)
                    sliceWidgetCache(termGroupDesc) = widget
                    widget
            }
        }

        def showSliceGraphOf(termGroupDesc: TermGroupDesc): Unit = {
            layout.topControl = sliceWidgetOf(termGroupDesc)
            derivationGraphs.layout()
        }
    }

    val dgraphCache = scala.collection.mutable.Map[Kernel, DGraphWidget]()
    var currentDGraph: DGraphWidget = null

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
            data match {
                case Left(_: AtomicNonterm) | Right(_) =>
                    symbol.addMouseListener(new MouseListener() {
                        def mouseDoubleClicked(e: MouseEvent): Unit = {}
                        def mousePressed(e: MouseEvent): Unit = {
                            println(data)
                            updateKernel(data)
                        }
                        def mouseReleased(e: MouseEvent): Unit = {}
                    })
                case _ =>
                    symbol.setBorder(new LineBorder(ColorConstants.buttonLightest)) // do nothing
            }
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

    termGroupsList.addListener(SWT.Selection, new Listener() {
        def handleEvent(e: Event): Unit = {
            val idx = termGroupsList.getSelectionIndex
            if (0 <= idx && idx < termGroupsItems.length) {
                termGroupsItems(idx) match {
                    case None =>
                        currentDGraph.showAllGraph()
                    case Some(termGroupDesc) =>
                        currentDGraph.showSliceGraphOf(termGroupDesc)
                }
            } else {
                // Something's wrong
            }
        }
    })
    def updateTermGroupDescList(): Unit = {
        termGroupsItems = Seq(None) ++ (currentDGraph.sliceMap.keys map { Some(_) })
        termGroupsList.removeAll()
        termGroupsItems foreach {
            case None =>
                termGroupsList.add("All")
            case Some(termGroup) =>
                termGroupsList.add(termGroup.toShortString)
        }
        termGroupsList.select(0)
    }

    def updateKernel(selected: Kernel): Unit = {
        buttonsMap foreach { _._2.setBackgroundColor(ColorConstants.white) }
        buttonsMap(selected).setBackgroundColor(ColorConstants.buttonDarker)

        val widget = dgraphCache get selected match {
            case Some(cached) => cached
            case None =>
                val dgraph: DGraph[ParseResultDerivationsSet] = selected match {
                    case Left(symbol: AtomicNonterm) => derivationFunc.deriveAtomic(symbol)
                    case Right((symbol, pointer)) => derivationFunc.deriveSequence(symbol, pointer)
                    case Left(symbol) => ??? // not going to happen
                }
                val cache = DGraphWidget(dgraph)
                dgraphCache(selected) = cache
                cache
        }

        currentDGraph = widget
        updateTermGroupDescList()
        layout.topControl = widget.graphWidget
        derivationGraphs.layout()
    }

    def initialize(): Unit = {
        updateKernel(defaultKernel)
    }

    def start(): Unit = {
        shell.setText("Derivation Graph")

        initialize()

        shell.open()
    }
}

object DerivationGraphVisualizer {
    type Kernel = Either[AtomicSymbol, (Sequence, Int)]

    def kernelOf(node: ParsingGraph.NontermNode): Kernel =
        node match {
            case n: ParsingGraph.AtomicNode => Left(n.symbol)
            case n: ParsingGraph.SequenceNode => Right(n.symbol, n.pointer)
        }

    def start(grammar: Grammar, display: Display, shell: Shell): Unit = {
        start(grammar, display, shell, Left(Start))
    }

    def start(grammar: Grammar, display: Display, shell: Shell, defaultKernel: Kernel): Unit = {
        val resources = BasicVisualizeResources
        new DerivationGraphVisualizer(grammar, display, shell, resources, defaultKernel).start()
    }

    def start(grammar: Grammar): Unit = {
        start(grammar, Left(Start))
    }

    def start(grammar: Grammar, defaultKernel: Kernel): Unit = {
        val display = Display.getDefault()
        val shell = new Shell(display)

        start(grammar, display, shell, defaultKernel)

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }
}
