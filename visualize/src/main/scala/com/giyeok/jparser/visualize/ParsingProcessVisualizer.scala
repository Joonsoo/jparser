package com.giyeok.jparser.visualize

import com.giyeok.jparser.Inputs.ConcreteInput
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.nparser.{Parser, ParsingContext}
import com.giyeok.jparser.nparser.Parser.Context
import com.giyeok.jparser.nparser.Parser.ProceedDetail
import com.giyeok.jparser.nparser.ParsingContext.Node
import org.eclipse.draw2d
import org.eclipse.draw2d.AbstractLayout
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.draw2d.IFigure
import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.draw2d.geometry.Dimension
import org.eclipse.draw2d.geometry.PrecisionRectangle
import org.eclipse.draw2d.geometry.Rectangle
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.MouseListener
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell

class ParsingProcessVisualizer[C <: Context](title: String, parser: Parser[C], source: Seq[ConcreteInput], display: Display, shell: Shell, resources: VisualizeResources[Figure], parsingContextWidgetFactory: ParsingContextWidgetFactory[C]) {

    // 상단 test string
    val sourceView = new FigureCanvas(shell, SWT.NONE)
    sourceView.setLayoutData({
        val formData = new FormData()
        formData.top = new FormAttachment(0, 0)
        formData.bottom = new FormAttachment(0, 30)
        formData.left = new FormAttachment(0, 0)
        formData.right = new FormAttachment(100, 0)
        formData
    })
    sourceView.setBackground(ColorConstants.white)

    val layout = new StackLayout

    // 하단 그래프 뷰
    val contentView = new Composite(shell, SWT.NONE)
    contentView.setLayout(layout)
    contentView.setLayoutData({
        val formData = new FormData()
        formData.top = new FormAttachment(sourceView)
        formData.bottom = new FormAttachment(100, 0)
        formData.right = new FormAttachment(100, 0)
        formData.left = new FormAttachment(0, 0)
        formData
    })

    sealed trait Pointer {
        def previous: Pointer
        def next: Pointer
        def previousBase: Pointer
        def nextBase: Pointer

        def stringRepr: String

        def <=(other: Pointer): Boolean
    }
    case object ParsingContextInitializingPointer extends Pointer {
        def previous = ParsingContextInitializingPointer
        def next = ParsingContextPointer(0)
        def previousBase = ParsingContextInitializingPointer
        def nextBase = ParsingContextPointer(0)

        def stringRepr: String = ">" + (source map { _.toCleanString }).mkString

        def <=(other: Pointer): Boolean = other != this
    }
    case class ParsingContextPointer(gen: Int) extends Pointer {
        def previous: Pointer =
            if (gen == 0) ParsingContextInitializingPointer
            else {
                transitionAt(gen - 1) match {
                    case Left((_, proceedDetail, _)) => ParsingContextTransitionPointer(gen - 1, proceedDetail.transitions.length)
                    case _ => previousBase
                }
            }
        def next: Pointer =
            if (gen < source.length) {
                transitionAt(gen) match {
                    case Left(_) => ParsingContextTransitionPointer(gen, 1)
                    case Right(_) => nextBase
                }
            } else this
        def previousBase = ParsingContextPointer(gen - 1)
        def nextBase = ParsingContextPointer(gen + 1)

        def stringRepr: String = {
            val (before, after) = source map { _.toCleanString } splitAt gen
            before.mkString + "*" + after.mkString
        }

        def <=(other: Pointer): Boolean = other match {
            case ParsingContextInitializingPointer => false
            case ParsingContextPointer(otherGen) => gen <= otherGen
            case ParsingContextTransitionPointer(otherGen, _) => gen <= otherGen
        }
    }
    case class ParsingContextTransitionPointer(gen: Int, stage: Int) extends Pointer {
        // assert(1 <= stage && stage <= stagesCount)
        def previous: Pointer =
            if (stage == 1) ParsingContextPointer(gen)
            else ParsingContextTransitionPointer(gen, stage - 1)
        def next: Pointer =
            transitionAt(gen) match {
                case Right(_) =>
                    nextBase
                case Left((_, proceedDetail, _)) if stage == proceedDetail.transitions.length =>
                    ParsingContextPointer(gen + 1)
                case _ =>
                    ParsingContextTransitionPointer(gen, stage + 1)
            }
        def previousBase: Pointer =
            if (stage == 1) ParsingContextPointer(gen - 1)
            else ParsingContextPointer(gen)
        def nextBase = ParsingContextPointer(gen + 1)

        def stringRepr: String = {
            val (before, after) = source map { _.toCleanString } splitAt (gen + 1)
            before.mkString + (">" * stage) + after.mkString
        }

        def <=(other: Pointer): Boolean = other match {
            case ParsingContextInitializingPointer => false
            case ParsingContextPointer(otherGen) => gen <= otherGen
            case ParsingContextTransitionPointer(otherGen, otherStage) => gen < otherGen || (gen == otherGen && stage <= otherStage)
        }
    }

    def isValidLocation(pointer: Pointer): Boolean = pointer match {
        case ParsingContextInitializingPointer => true
        case ParsingContextPointer(gen) => 0 <= gen && gen <= source.length
        case ParsingContextTransitionPointer(gen, _) => 0 <= gen && gen < source.length
    }

    val firstLocation = ParsingContextPointer(0)
    lazy val lastValidLocation = ParsingContextPointer(((source.length to 0 by -1) find { contextAt(_).isLeft }).get)
    val lastLocation = ParsingContextPointer(source.length)
    var currentLocation: Pointer = firstLocation

    var showReduced: Boolean = false

    def updateLocation(newLocation: Pointer): Unit = {
        val sourceViewHeight = 40
        if (isValidLocation(newLocation)) {
            currentLocation = newLocation

            sourceView.setContents({
                val f = new Figure
                f.setLayoutManager(new ToolbarLayout(true))

                abstract class RelocationLayout(offsetX: Int, offsetY: Int) extends AbstractLayout {
                    protected def calculatePreferredSize(container: IFigure, w: Int, h: Int): Dimension = {
                        new Dimension(w, h)
                    }

                    def layout(container: IFigure): Unit = {
                        val children = container.getChildren.asInstanceOf[java.util.List[IFigure]]
                        val i = children.iterator()
                        val containerBound: Rectangle = container.getBounds
                        val containerSize = container.getPreferredSize()
                        while (i.hasNext) {
                            val c = i.next()
                            val childSize = c.getPreferredSize()
                            val (x, y) = calculate(containerBound.width, containerBound.height, childSize.width, childSize.height)
                            val bound = new PrecisionRectangle(containerBound.x + x + offsetX, containerBound.y + y + offsetY, childSize.width, childSize.height)
                            c.setBounds(bound)
                        }
                    }

                    def calculate(boundWidth: Int, boundHeight: Int, childWidth: Int, childHeight: Int): (Int, Int)
                }
                class CenterLayout(offsetX: Int, offsetY: Int) extends RelocationLayout(offsetX, offsetY) {
                    def calculate(boundWidth: Int, boundHeight: Int, childWidth: Int, childHeight: Int): (Int, Int) =
                        ((boundWidth - childWidth) / 2 + offsetX, (boundHeight - childHeight) / 2 + offsetY)
                }
                class GroundLayout(offsetX: Int, offsetY: Int) extends RelocationLayout(offsetX, offsetY) {
                    def calculate(boundWidth: Int, boundHeight: Int, childWidth: Int, childHeight: Int): (Int, Int) =
                        ((boundWidth - childWidth) / 2, boundHeight - childHeight)
                }

                def listener(location: Pointer) = new draw2d.MouseListener() {
                    def mousePressed(e: draw2d.MouseEvent): Unit = { updateLocation(location) }
                    def mouseReleased(e: draw2d.MouseEvent): Unit = {}
                    def mouseDoubleClicked(e: draw2d.MouseEvent): Unit = {}
                }
                def terminalFig(s: ConcreteInput): Figure = {
                    val term = new draw2d.Label(s.toCleanString)
                    term.setForegroundColor(ColorConstants.red)
                    term.setFont(resources.fixedWidth12Font)
                    term
                }
                def textFig(text: String, font: Font): Figure = {
                    val label = new draw2d.Label()
                    label.setText(text)
                    label.setFont(font)
                    label
                }
                def ellipseFig(width: Int, height: Int): Figure = {
                    val ellipse = new draw2d.Ellipse
                    ellipse.setSize(width, height)
                    ellipse.setBackgroundColor(ColorConstants.lightGray)
                    ellipse.setForegroundColor(ColorConstants.white)
                    ellipse
                }
                def emptyBoxFig(width: Int, height: Int): Figure = {
                    val box = new draw2d.Figure
                    box.setLayoutManager(new ToolbarLayout)
                    box.setSize(width, height)
                    box.setPreferredSize(width, height)
                    box
                }

                val cursorColor = ColorConstants.lightGreen
                val height = if (source.isEmpty) 15 else terminalFig(source.head).getPreferredSize.height
                def pointerFig(pointer: Pointer): Figure = {
                    val fig = emptyBoxFig(12, height)
                    fig.setLayoutManager(new CenterLayout(0, 0))
                    val ellipse = ellipseFig(12, 12)
                    fig.add(ellipse)
                    pointer match {
                        case ParsingContextPointer(gen) =>
                            val genFig = textFig(s"$gen", resources.smallerFont)
                            fig.add(genFig)
                            fig.setPreferredSize(math.max(genFig.getPreferredSize.width, fig.getPreferredSize.width), height)
                        case _ =>
                    }
                    if (pointer == currentLocation) {
                        ellipse.setBackgroundColor(cursorColor)
                    }
                    fig.addMouseListener(listener(pointer))
                    fig
                }
                val transitionBoxWidth = ((1 to 7) map { _.toString } map { textFig(_, resources.smallFont).getPreferredSize.width }).max
                def transitionPointerFig(gen: Int): Figure = {
                    val fig = emptyBoxFig(transitionBoxWidth, height)
                    fig.setLayoutManager(new CenterLayout(0, 0))
                    currentLocation match {
                        case ParsingContextTransitionPointer(`gen`, stage) =>
                            val stageFig = textFig(s"$stage", resources.smallFont)
                            stageFig.setForegroundColor(cursorColor)
                            fig.add(stageFig)
                        case _ =>
                            fig.addMouseListener(listener(ParsingContextTransitionPointer(gen, 1)))
                    }
                    fig
                }

                f.add(pointerFig(ParsingContextInitializingPointer))
                source.zipWithIndex foreach { s =>
                    val (input, gen) = s
                    f.add(pointerFig(ParsingContextPointer(gen)))
                    f.add(terminalFig(input))
                    f.add(transitionPointerFig(gen))
                }
                f.add(pointerFig(ParsingContextPointer(source.length)))

                f.add({ val x = emptyBoxFig(100, height); x.addMouseListener(listener(ParsingContextPointer(source.length))); x })

                currentLocation match {
                    case ParsingContextPointer(gen) =>
                        contextAt(gen) match {
                            case Left(context) =>
                            // TODO
                            case Right(_) => // nothing to do
                        }
                    case _ =>
                }
                f
            })

            val (nameOpt, currentControl) = controlAt(currentLocation)
            val shellTitle = nameOpt match {
                case Some(name) =>
                    s"$title: ${currentLocation.stringRepr}, $name"
                case None =>
                    s"$title: ${currentLocation.stringRepr}"
            }
            shell.setText(shellTitle)
            layout.topControl = currentControl
            contentView.layout()
            shell.layout()
            currentControl.setFocus()
        }
    }

    private val contextCache = scala.collection.mutable.Map[Int, Either[C, ParsingError]]()
    private val transitionCache = scala.collection.mutable.Map[Int, Either[(Context, ProceedDetail, Context), ParsingError]]()
    def contextAt(gen: Int): Either[C, ParsingError] =
        contextCache get gen match {
            case Some(cached) => cached
            case None =>
                val context: Either[C, ParsingError] = if (gen == 0) Left(parser.initialContext) else {
                    contextAt(gen - 1) match {
                        case Left(prevCtx) =>
                            parser.proceedDetail(prevCtx, source(gen - 1)) match {
                                case Left((detail, nextCtx)) =>
                                    transitionCache(gen - 1) = Left((prevCtx, detail, nextCtx))
                                    Left(nextCtx)
                                case Right(error) =>
                                    transitionCache(gen - 1) = Right(error)
                                    Right(error)
                            }
                        case Right(error) =>
                            transitionCache(gen - 1) = Right(error)
                            Right(error)
                    }
                }
                assert((0 until gen) forall { g => transitionCache contains g })
                contextCache(gen) = context
                context
        }
    def transitionAt(gen: Int): Either[(Context, ProceedDetail, Context), ParsingError] =
        transitionCache get gen match {
            case Some(cached) => cached
            case None =>
                contextAt(gen + 1)
                transitionCache(gen)
        }

    def errorControl(message: String): Control = {
        val control = new Composite(contentView, SWT.NONE)
        control.setLayout(new FillLayout)
        new Label(control, SWT.NONE).setText(message)
        control
    }

    private val nodeFigGenerator = resources.nodeFigureGenerators
    private val controlCache = scala.collection.mutable.Map[(Pointer, Boolean), (Option[String], Control)]()

    def controlAt(pointer: Pointer): (Option[String], Control) = {
        controlCache get(pointer, showReduced) match {
            case Some(cached) => cached
            case None =>
                val (name, control) = pointer match {
                    case ParsingContextInitializingPointer =>
                        (None, errorControl("TODO"))
                    case ParsingContextPointer(gen) =>
                        contextAt(gen) match {
                            case Left(wctx) =>
                                val widget = if (showReduced) parsingContextWidgetFactory(contentView, SWT.NONE, nodeFigGenerator, parser.grammar, ParsingContext.reduced(wctx.nextGraph), wctx)
                                else parsingContextWidgetFactory(contentView, SWT.NONE, nodeFigGenerator, parser.grammar, wctx)
                                (None, widget)
                            case Right(error) =>
                                (None, errorControl(error.msg))
                        }
                    case ParsingContextTransitionPointer(gen, stage) =>
                        println(gen, stage)
                        def controlOpt[T](opt: Option[T], errorMsg: String)(func: T => Control): Control =
                            opt match {
                                case None => errorControl(errorMsg)
                                case Some(tuple) => func(tuple)
                            }
                        transitionAt(gen) match {
                            case Left((_, transition, nextCtx)) =>
                                val (prevGraph, nextGraph) = (transition.graphAt(stage - 1), transition.graphAt(stage))
                                val viewer = if (transition.isResultAt(stage)) {
                                    new ZestGraphTransitionWidget(contentView, SWT.NONE, nodeFigGenerator, parser.grammar, prevGraph, nextGraph) with ZestParseTreeConstructorView {
                                        val context: Context = nextCtx
                                        addMouseListener(new MouseListener() {
                                            def mouseDown(e: org.eclipse.swt.events.MouseEvent): Unit = {}

                                            def mouseUp(e: org.eclipse.swt.events.MouseEvent): Unit = {}

                                            def mouseDoubleClick(e: org.eclipse.swt.events.MouseEvent): Unit = {
                                                nodesAt(e.x, e.y) foreach {
                                                    case node: Node =>
                                                        parseTreeOpener(e.stateMask)(context.gen, node.kernel)
                                                    case data =>
                                                        println(data)
                                                }
                                            }
                                        })
                                    }
                                } else {
                                    new ZestGraphTransitionWidget(contentView, SWT.NONE, nodeFigGenerator, parser.grammar, prevGraph, nextGraph)
                                }
                                (Some(transition.nameOf(stage)), viewer)
                            case Right(error) => (None, errorControl(error.msg))
                        }
                }
                control.addKeyListener(keyListener)
                controlCache((pointer, showReduced)) = (name, control)
                (name, control)
        }
    }

    private var dotGraphGen = Option.empty[DotGraphGenerator]

    def keyListener = new KeyListener() {
        def keyPressed(x: KeyEvent): Unit = {
            x.keyCode match {
                case SWT.ARROW_LEFT => updateLocation(currentLocation.previous)
                case SWT.ARROW_RIGHT => updateLocation(currentLocation.next)
                case SWT.ARROW_UP => updateLocation(currentLocation.previousBase)
                case SWT.ARROW_DOWN => updateLocation(currentLocation.nextBase)
                case SWT.HOME => updateLocation(firstLocation)
                case SWT.END =>
                    if (lastValidLocation <= currentLocation && lastLocation != currentLocation) updateLocation(lastLocation) else updateLocation(lastValidLocation)

                case 'D' | 'd' =>
                    if (dotGraphGen.isEmpty) {
                        dotGraphGen = Some(new DotGraphGenerator(parser.grammar))
                    }
                    currentLocation match {
                        case ParsingContextPointer(gen) =>
                            contextAt(gen) match {
                                case Left(ctx) => dotGraphGen.get.addGraph(ctx.nextGraph)
                                case Right(_) => // nothing to do
                            }
                        case ParsingContextTransitionPointer(gen, stage) =>
                            transitionAt(gen) match {
                                case Left((_, transition, _)) =>
                                    dotGraphGen.get.addTransition(transition.graphAt(stage - 1), transition.graphAt(stage))
                                case Right(_) => // nothing to do
                            }
                        case _ => // nothing to do
                    }
                    dotGraphGen.get.printDotGraph()

                case 'F' | 'f' =>
                    dotGraphGen = None
                    println("DOT graph generator cleared")

                case 'Z' | 'z' =>
                    showReduced = !showReduced
                    updateLocation(currentLocation)

                case code =>
                    println(s"keyPressed: $code")
            }
        }

        def keyReleased(x: KeyEvent): Unit = {}
    }
    sourceView.addKeyListener(keyListener)

    def start(): Unit = {
        shell.setText("Parsing Graph")
        shell.setLayout(new FormLayout)

        updateLocation(currentLocation)

        shell.addKeyListener(keyListener)

        shell.open()
    }
}

trait ParsingContextWidgetFactory[C <: Context] {
    def apply(parent: Composite, style: Int, fig: NodeFigureGenerators[Figure], grammar: NGrammar, graph: ParsingContext.Graph, context: C): Control

    def apply(parent: Composite, style: Int, fig: NodeFigureGenerators[Figure], grammar: NGrammar, context: C): Control =
        apply(parent, style, fig, grammar, context.nextGraph, context)
}

object ParsingProcessVisualizer {
    def start[C <: Context](title: String, parser: Parser[C], source: Seq[ConcreteInput], display: Display, shell: Shell, parsingContextWidgetFactory: ParsingContextWidgetFactory[C]): Unit = {
        new ParsingProcessVisualizer(title: String, parser, source, display, shell, BasicVisualizeResources, parsingContextWidgetFactory).start()
    }
}
