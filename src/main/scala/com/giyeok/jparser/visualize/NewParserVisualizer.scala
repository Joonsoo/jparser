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
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridData
import org.eclipse.jface.resource.JFaceResources
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.layout.GridLayout
import com.giyeok.jparser.NewParser
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
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.ParseForest
import javax.swing.plaf.basic.CenterLayout
import org.eclipse.draw2d.LineBorder

class NewParserVisualizer(grammar: Grammar, source: Seq[ConcreteInput], display: Display, shell: Shell, resources: VisualizeResources) {
    type Parser = NewParser[ParseForest]

    val parser = new NewParser(grammar, ParseForestFunc)

    // 상단 test string
    val sourceView = new FigureCanvas(shell, SWT.NONE)
    sourceView.setLayoutData({
        val d = new GridData(GridData.FILL_HORIZONTAL)
        d.heightHint = 40
        d
    })
    sourceView.setBackground(ColorConstants.white)

    val layout = new StackLayout

    // 하단 그래프 뷰
    val graphView = new Composite(shell, SWT.NONE)
    graphView.setLayout(layout)
    graphView.setLayoutData(new GridData(GridData.FILL_BOTH))

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
        def previous = if (gen == 0) ParsingContextInitializingPointer else ParsingContextTransitionPointer(gen - 1, 6)
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
        // stage 1: expand
        // stage 2: 1차 lift
        // stage 3: 1차 lift 트리밍
        // stage 4: revert
        // stage 5: 2차 lift
        // stage 6: 2차 lift 트리밍
        assert(1 <= stage && stage <= 6)
        def previous = if (stage == 1) ParsingContextPointer(gen) else ParsingContextTransitionPointer(gen, stage - 1)
        def next = if (stage == 6) ParsingContextPointer(gen + 1) else ParsingContextTransitionPointer(gen, stage + 1)
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
        val sourceViewHeight = 20
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
                    ellipse
                }
                def emptyBoxFig(width: Int, height: Int): Figure = {
                    val box = new draw2d.Figure
                    box.setLayoutManager(new ToolbarLayout)
                    box.setSize(width, height)
                    box.setPreferredSize(width, height)
                    box
                }

                val height = if (source.isEmpty) 15 else terminalFig(source.head).getPreferredSize.height
                def pointerFig(pointer: Pointer): Figure = {
                    val fig = emptyBoxFig(11, height)
                    fig.setLayoutManager(new CenterLayout(0, 0))
                    val ellipse = ellipseFig(7, 7)
                    fig.add(ellipse)
                    if (pointer == currentLocation) {
                        ellipse.setBackgroundColor(ColorConstants.red)
                    } else {
                        ellipse.setForegroundColor(ColorConstants.white)
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
                            fig.add(textFig(s"$stage", resources.smallFont))
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

                currentLocation match {
                    case ParsingContextPointer(gen) =>
                        contextAt(gen) match {
                            case Left(context) =>
                                context.result match {
                                    case Some(results) =>
                                        f.add(emptyBoxFig(100, 5))
                                        f.add(textFig(s"r=${results.trees.size}", resources.smallFont))
                                    case _ =>
                                }
                            case Right(_) => // nothing to do
                        }
                    case _ =>
                }
                f
            })

            shell.setText(s"${grammar.name}: ${currentLocation.stringRepr}")
            layout.topControl = controlAt(currentLocation)
            graphView.layout()
            shell.layout()
            sourceView.setFocus()
        }
    }

    val contextCache = scala.collection.mutable.Map[Int, Either[Parser#ParsingCtx, ParsingError]]()
    val transitionCache = scala.collection.mutable.Map[Int, Either[Parser#ParsingCtxTransition, ParsingError]]()
    def contextAt(gen: Int): Either[Parser#ParsingCtx, ParsingError] =
        contextCache get gen match {
            case Some(cached) => cached
            case None =>
                val context: Either[Parser#ParsingCtx, ParsingError] = if (gen == 0) Left(parser.initialContext) else {
                    contextAt(gen - 1) match {
                        case Left(prevContext) =>
                            val detail = prevContext.proceedDetail(source(gen - 1))
                            transitionCache(gen - 1) = Left(detail._1.asInstanceOf[Parser#ParsingCtxTransition])
                            detail._2 match {
                                case Left(nextCtx) => Left(nextCtx)
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
    def transitionAt(gen: Int): Either[Parser#ParsingCtxTransition, ParsingError] =
        transitionCache get gen match {
            case Some(cached) => cached
            case None =>
                contextAt(gen + 1)
                transitionCache(gen)
        }

    def errorControl(message: String): Control = {
        val control = new Label(graphView, SWT.NONE)
        control.setText(message)
        control
    }

    val nodeIdCache = new NodeIdCache()

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
                            case Left(ctx) => new NewParsingContextGraphVisualizeWidget(graphView, SWT.NONE, grammar, nodeIdCache, ctx)
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
                                    case 1 =>
                                        controlOpt(transition.firstStage, "No Expansion") { t => new ExpandTransitionVisualize(graphView, SWT.NONE, grammar, nodeIdCache, t._1) }
                                    case 2 =>
                                        controlOpt(transition.firstStage, "No Expansion") { t => new LiftTransitionVisualize(graphView, SWT.NONE, grammar, nodeIdCache, t._2) }
                                    case 3 =>
                                        controlOpt(transition.secondStage, "No Lift") { t => new TrimmingTransitionVisualize(graphView, SWT.NONE, grammar, nodeIdCache, t._1) }
                                    case 4 =>
                                        controlOpt(transition.secondStage, "No Lift") { t => new RevertTransitionVisualize(graphView, SWT.NONE, grammar, nodeIdCache, t._2) }
                                    case 5 =>
                                        controlOpt(transition.secondStage, "No Lift") { t => new LiftTransitionVisualize(graphView, SWT.NONE, grammar, nodeIdCache, t._3) }
                                    case 6 =>
                                        controlOpt(transition.finalTrimming, "No Lift") { t => new TrimmingTransitionVisualize(graphView, SWT.NONE, grammar, nodeIdCache, t) }
                                }
                            case Right(error) => errorControl(error.msg)
                        }
                }
                def finalizeView(v: Control): Control = {
                    // 나중에 이벤트 리스너들 넣기
                    v.addKeyListener(keyListener)
                    v
                }
                controlCache(pointer) = finalizeView(control)
                control
        }
    }

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
                case code =>
                    println(s"keyPressed: $code")
            }
        }

        def keyReleased(x: KeyEvent): Unit = {}
    }
    sourceView.addKeyListener(keyListener)

    def start(): Unit = {
        shell.setText("Parsing Graph")
        shell.setLayout({
            val l = new GridLayout
            l.marginWidth = 0
            l.marginHeight = 0
            l.verticalSpacing = 0
            l.horizontalSpacing = 0
            l
        })

        updateLocation(currentLocation)

        shell.addKeyListener(keyListener)

        shell.open()
    }
}

object NewParserVisualizer {
    def start(grammar: Grammar, source: Seq[ConcreteInput], display: Display, shell: Shell): Unit = {
        val resources = BasicVisualizeResources
        new NewParserVisualizer(grammar, source, display, shell, resources).start()
    }

    def start(grammar: Grammar, source: Seq[ConcreteInput]): Unit = {
        val display = Display.getDefault()
        val shell = new Shell(display)

        start(grammar, source, display, shell)

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }

}