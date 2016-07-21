package com.giyeok.jparser.visualize

import org.eclipse.swt.graphics.Font
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Inputs.ConcreteInput
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Color
import org.eclipse.draw2d.AbstractBorder
import org.eclipse.draw2d.IFigure
import org.eclipse.draw2d.geometry.Insets
import org.eclipse.draw2d.Graphics
import org.eclipse.draw2d.geometry.Insets
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.swt.custom.StackLayout
import org.eclipse.jface.resource.JFaceResources
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Control
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.draw2d.AbstractLayout
import org.eclipse.draw2d.geometry.Dimension
import org.eclipse.draw2d.geometry.Rectangle
import org.eclipse.draw2d.geometry.PrecisionRectangle
import org.eclipse.draw2d
import org.eclipse.swt.widgets.Label
import com.giyeok.jparser.ParsingErrors.ParsingError
import org.eclipse.swt.layout.FillLayout
import javax.swing.plaf.basic.CenterLayout
import org.eclipse.draw2d.LineBorder
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormLayout
import com.giyeok.jparser.ParseResultDerivationsSet
import com.giyeok.jparser.ParseResultDerivationsSetFunc
import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.ParseResultGraph
import com.giyeok.jparser.ParseResultGraphFunc
import com.giyeok.jparser.nparser
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.ParsingErrors.ParsingError

class ParsingProcessVisualizer(title: String, parser: NaiveParser, source: Seq[ConcreteInput], display: Display, shell: Shell, resources: VisualizeResources) {

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
        def previous = this
        def next = ParsingContextPointer(0)
        def previousBase = this
        def nextBase = ParsingContextPointer(0)

        def stringRepr: String = ">" + (source map { _.toCleanString }).mkString

        def <=(other: Pointer) = other != this
    }
    case class ParsingContextPointer(gen: Int) extends Pointer {
        def previous = if (gen == 0) ParsingContextInitializingPointer else ParsingContextTransitionPointer(gen - 1, 3)
        def next = ParsingContextTransitionPointer(gen, 1)
        def previousBase = ParsingContextPointer(gen - 1)
        def nextBase = ParsingContextPointer(gen + 1)

        def stringRepr = {
            val (before, after) = source map { _.toCleanString } splitAt gen
            (before.mkString + ("*") + after.mkString)
        }

        def <=(other: Pointer) = other match {
            case ParsingContextInitializingPointer => false
            case ParsingContextPointer(otherGen) => gen <= otherGen
            case ParsingContextTransitionPointer(otherGen, _) => gen <= otherGen
        }
    }
    case class ParsingContextTransitionPointer(gen: Int, stage: Int) extends Pointer {
        // stage 1: lift
        // stage 2: 트리밍
        // stage 3: revert
        assert(1 <= stage && stage <= 3)
        def previous = if (stage == 1) ParsingContextPointer(gen) else ParsingContextTransitionPointer(gen, stage - 1)
        def next = if (stage == 3) ParsingContextPointer(gen + 1) else ParsingContextTransitionPointer(gen, stage + 1)
        def previousBase = if (stage == 1) ParsingContextPointer(gen - 1) else ParsingContextPointer(gen)
        def nextBase = ParsingContextPointer(gen + 1)

        def stringRepr = {
            val (before, after) = source map { _.toCleanString } splitAt (gen + 1)
            (before.mkString + (">" * stage) + after.mkString)
        }

        def <=(other: Pointer) = other match {
            case ParsingContextInitializingPointer => false
            case ParsingContextPointer(otherGen) => gen <= otherGen
            case ParsingContextTransitionPointer(otherGen, otherStage) => gen < otherGen || (gen == otherGen && stage <= otherStage)
        }
    }

    def isValidLocation(pointer: Pointer): Boolean = pointer match {
        case ParsingContextInitializingPointer => true
        case ParsingContextPointer(gen) => 0 <= gen && gen <= source.length
        case ParsingContextTransitionPointer(gen, stage) => 0 <= gen && gen < source.length
    }

    val firstLocation = ParsingContextPointer(0)
    lazy val lastValidLocation = ParsingContextPointer(((0 to source.length).toSeq.reverse.find { contextAt(_).isLeft }).get)
    val lastLocation = ParsingContextPointer(source.length)
    var currentLocation: Pointer = firstLocation

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
                        val children = container.getChildren().asInstanceOf[java.util.List[IFigure]]
                        val i = children.iterator()
                        val containerBound: Rectangle = container.getBounds()
                        val containerSize = container.getPreferredSize()
                        while (i.hasNext()) {
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
                val transitionBoxWidth = (Seq("1", "2", "3", "4", "5", "6") map { textFig(_, resources.smallFont).getPreferredSize.width }).max
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

            shell.setText(s"$title: ${currentLocation.stringRepr}")
            val currentControl = controlAt(currentLocation)
            layout.topControl = currentControl
            contentView.layout()
            shell.layout()
            currentControl.setFocus()
        }
    }

    val contextCache = scala.collection.mutable.Map[Int, Either[NaiveParser#WrappedContext, ParsingError]]()
    val transitionCache = scala.collection.mutable.Map[Int, Either[NaiveParser#ProceedDetail, ParsingError]]()
    def contextAt(gen: Int): Either[NaiveParser#WrappedContext, ParsingError] =
        contextCache get gen match {
            case Some(cached) => cached
            case None =>
                val context: Either[NaiveParser#WrappedContext, ParsingError] = if (gen == 0) Left(parser.initialContext) else {
                    contextAt(gen - 1) match {
                        case Left(prevCtx) =>
                            prevCtx.proceedDetail(source(gen - 1)) match {
                                case Left((detail, nextCtx)) =>
                                    transitionCache(gen - 1) = Left(detail)
                                    Left(nextCtx)
                                case Right(error) => Right(error)
                            }
                        case Right(error) =>
                            transitionCache(gen - 1) = Right(error)
                            Right(error)
                    }
                }
                contextCache(gen) = context
                context
        }
    def transitionAt(gen: Int): Either[NaiveParser#ProceedDetail, ParsingError] =
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

    val nodeFigGenerator = {
        val figureGenerator: FigureGenerator.Generator[Figure] = FigureGenerator.draw2d.Generator

        val figureAppearances = new FigureGenerator.Appearances[Figure] {
            val default = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 10, SWT.NONE), ColorConstants.black)
            val nonterminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.BOLD), ColorConstants.blue)
            val terminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.NONE), ColorConstants.red)

            override val small = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 8, SWT.NONE), ColorConstants.gray)
            override val kernelDot = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.NONE), ColorConstants.green)
            override val symbolBorder = FigureGenerator.draw2d.BorderAppearance(new LineBorder(ColorConstants.lightGray))
        }

        val symbolFigureGenerator = new SymbolFigureGenerator(figureGenerator, figureAppearances)

        new NodeFigureGenerators(figureGenerator, figureAppearances, symbolFigureGenerator)
    }
    val controlCache = scala.collection.mutable.Map[Pointer, Control]()

    def controlAt(pointer: Pointer): Control = {
        controlCache get pointer match {
            case Some(cached) => cached
            case None =>
                val control = pointer match {
                    case ParsingContextInitializingPointer =>
                        errorControl("TODO")
                    case ParsingContextPointer(gen) =>
                        contextAt(gen) match {
                            case Left(wctx) =>
                                new ZestParsingContextWidget(contentView, SWT.NONE, nodeFigGenerator, parser.grammar, wctx)
                            case Right(error) => errorControl(error.msg)
                        }
                    case ParsingContextTransitionPointer(gen, stage) =>
                        def controlOpt[T](opt: Option[T], errorMsg: String)(func: T => Control): Control =
                            opt match {
                                case None => errorControl(errorMsg)
                                case Some(tuple) => func(tuple)
                            }
                        transitionAt(gen) match {
                            case Left(transition) =>
                                stage match {
                                    case 1 => new ZestGraphTransitionWidget(contentView, SWT.NONE, nodeFigGenerator, parser.grammar, transition.baseCtx, transition.liftedCtx)
                                    case 2 => new ZestGraphTransitionWidget(contentView, SWT.NONE, nodeFigGenerator, parser.grammar, transition.liftedCtx, transition.trimmedCtx)
                                    case 3 => new ZestGraphTransitionWidget(contentView, SWT.NONE, nodeFigGenerator, parser.grammar, transition.trimmedCtx, transition.revertedCtx)
                                }
                            case Right(error) => errorControl(error.msg)
                        }
                }
                control.addKeyListener(keyListener)
                controlCache(pointer) = control
                control
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
                                case Left(wctx) => dotGraphGen.get.addGraph(wctx.ctx.graph)
                                case Right(_) => // nothing to do
                            }
                        case ParsingContextTransitionPointer(gen, stage) =>
                            transitionAt(gen) match {
                                case Left(transition) =>
                                    stage match {
                                        case 1 => dotGraphGen.get.addTransition(transition.baseCtx.graph, transition.liftedCtx.graph)
                                        case 2 => dotGraphGen.get.addTransition(transition.liftedCtx.graph, transition.trimmedCtx.graph)
                                        case 3 => dotGraphGen.get.addTransition(transition.trimmedCtx.graph, transition.revertedCtx.graph)
                                    }
                                case Right(_) => // nothing to do
                            }
                        case _ => // nothing to do
                    }
                    dotGraphGen.get.printDotGraph()

                case 'F' | 'f' =>
                    dotGraphGen = None
                    println("DOT graph generator cleared")

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

object ParsingProcessVisualizer {
    def start(title: String, parser: NaiveParser, source: Seq[ConcreteInput], display: Display, shell: Shell): Unit = {
        new ParsingProcessVisualizer(title: String, parser, source, display, shell, BasicVisualizeResources).start()
    }
    def start(grammar: Grammar, source: Seq[ConcreteInput], display: Display, shell: Shell): Unit = {
        val ngrammar = NGrammar.fromGrammar(grammar)
        val parser = new NaiveParser(ngrammar)
        start(grammar.name, parser, source, display, shell)
    }
}
