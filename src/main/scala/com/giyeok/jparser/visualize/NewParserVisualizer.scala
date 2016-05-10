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

class NewParserVisualizer(grammar: Grammar, source: Seq[ConcreteInput], display: Display, shell: Shell, resources: NewParserVisualizer.Resources) {
    val parser = new NewParser(grammar)

    // 상단 test string
    val sourceView = new FigureCanvas(shell, SWT.NONE)
    sourceView.setLayoutData(new GridData(GridData.FILL_HORIZONTAL))
    sourceView.setBackground(ColorConstants.white)

    val layout = new StackLayout

    // 하단 그래프 뷰
    val graphView = new Composite(shell, SWT.NONE)
    graphView.setLayout(layout)
    graphView.setLayoutData(new GridData(GridData.FILL_BOTH))

    case class VisualizationLocation(baseGen: Int, stage: Int) {
        // stage 0: base generation의 parsing context
        // stage 1: expand
        // stage 2: pre-lift
        // stage 3: revert and trimmed
        // stage 4: final lift
        def previousLocation = if (stage == 0) VisualizationLocation(baseGen - 1, 4) else VisualizationLocation(baseGen, stage - 1)
        def nextLocation = if (stage == 4) VisualizationLocation(baseGen + 1, 0) else VisualizationLocation(baseGen, stage + 1)
        def previousBase = if (stage == 0) VisualizationLocation(baseGen - 1, 0) else VisualizationLocation(baseGen, 0)
        def nextBase = VisualizationLocation(baseGen + 1, 0)

        def stringRepresentation = {
            val sourceStr = source map { _.toCleanString }

            val divider = baseGen + (if (stage > 0) 1 else 0)
            ((sourceStr take divider).mkString + (if (stage == 0) "*" else ">") + (sourceStr drop divider).mkString)
        }
    }
    def isValidLocation(location: VisualizationLocation): Boolean = {
        val VisualizationLocation(baseGen, stage) = location
        (((0 <= baseGen && baseGen < source.length) && (0 <= stage && stage <= 4)) ||
            ((baseGen == source.length) && (stage == 0)))
    }

    var currentLocation = VisualizationLocation(0, 0)

    class UnderbarBorder(color: Color, width: Int) extends AbstractBorder {
        def getInsets(figure: IFigure): Insets = new Insets(0)
        def paint(figure: IFigure, graphics: Graphics, insets: Insets): Unit = {
            graphics.setLineWidth(width)
            val bounds = figure.getBounds
            graphics.setLineWidth(width)
            graphics.setForegroundColor(color)
            graphics.drawLine(bounds.x, bounds.bottom, bounds.right, bounds.bottom)
        }
    }
    val cursorBorder = new UnderbarBorder(ColorConstants.black, 10)

    def updateLocation(newLocation: VisualizationLocation): Unit = {
        if (isValidLocation(newLocation)) {
            currentLocation = newLocation

            println(newLocation)
            sourceView.setContents({
                val f = new Figure
                f.setLayoutManager(new ToolbarLayout(true))

                class CenterLayout extends AbstractLayout {
                    protected def calculatePreferredSize(container: IFigure, w: Int, h: Int): Dimension = {
                        new Dimension(w, h)
                    }

                    def layout(container: IFigure): Unit = {
                        val children = container.getChildren().asInstanceOf[java.util.List[IFigure]]
                        val i = children.iterator()
                        while (i.hasNext()) {
                            val c = i.next()
                            val allbound: Rectangle = container.getBounds()
                            val containerSize = container.getPreferredSize()
                            val childSize = c.getPreferredSize()
                            val bound = new PrecisionRectangle(
                                allbound.x + allbound.width / 2 + (containerSize.width - childSize.width) / 2,
                                allbound.y + allbound.height / 2 + (containerSize.height - childSize.height) / 2,
                                childSize.width, childSize.height)
                            c.setBounds(bound)
                        }
                    }
                }
                def listener(location: VisualizationLocation) = new draw2d.MouseListener() {
                    def mousePressed(e: draw2d.MouseEvent): Unit = { updateLocation(location) }
                    def mouseReleased(e: draw2d.MouseEvent): Unit = {}
                    def mouseDoubleClicked(e: draw2d.MouseEvent): Unit = {}
                }
                def pointerFig(location: VisualizationLocation, addingWidth: Int): Figure = {
                    val pointer = new draw2d.Figure
                    val stageLabelFrame = new draw2d.Figure
                    stageLabelFrame.setSize(6, 20)
                    stageLabelFrame.setLayoutManager(new CenterLayout)
                    if (location == newLocation) {
                        stageLabelFrame.setBorder(cursorBorder)
                    }
                    if (location.stage == 0) {
                        val ellipse = new draw2d.Ellipse
                        ellipse.setSize(3, 3)
                        ellipse.setBackgroundColor(ColorConstants.lightGray)
                        stageLabelFrame.add(ellipse)
                    } else {
                        val stageLabel = new draw2d.Label()
                        stageLabel.setText(s"${location.stage}")
                        stageLabel.setFont(resources.smallFont)
                        stageLabelFrame.add(stageLabel)
                    }
                    pointer.add(stageLabelFrame)
                    pointer.setSize(5 + addingWidth, 20)
                    pointer.addMouseListener(listener(location))
                    pointer
                }
                def terminalFig(s: ConcreteInput): Figure = {
                    val term = new draw2d.Label(s.toCleanString)
                    term.setForegroundColor(ColorConstants.red)
                    term.setFont(resources.fixedWidth12Font)
                    term
                }
                def textFig(text: String): Figure = {
                    val label = new draw2d.Label()
                    label.setText(text)
                    label.setFont(resources.smallFont)
                    label
                }
                source.zipWithIndex foreach { s =>
                    val (input, gen) = s
                    f.add(pointerFig(VisualizationLocation(gen, 0), 0))
                    f.add(terminalFig(input))
                    (1 to 4) foreach { stage =>
                        f.add(pointerFig(VisualizationLocation(gen, stage), 0))
                    }
                }
                f.add(pointerFig(VisualizationLocation(source.length, 0), 200))
                contextAt(newLocation.baseGen) match {
                    case Left(context) =>
                        f.add(textFig(s"r=${context.results.size}"))
                    case Right(_) => // nothing to do
                }
                f
            })

            shell.setText(s"${grammar.name}: ${currentLocation.stringRepresentation}")
            layout.topControl = graphAt(currentLocation)
            graphView.layout()
            shell.layout()
            sourceView.setFocus()
        }
    }

    val contextCache = scala.collection.mutable.Map[Int, Either[NewParser#ParsingContext, ParsingError]]()
    val proceedCache = scala.collection.mutable.Map[Int, Either[NewParser#ProceedDetail, ParsingError]]()
    def contextAt(gen: Int): Either[NewParser#ParsingContext, ParsingError] =
        contextCache get gen match {
            case Some(context) => context
            case None =>
                val context: Either[NewParser#ParsingContext, ParsingError] = if (gen == 0) Left(parser.initialContext) else {
                    contextAt(gen - 1) match {
                        case Left(prevContext) =>
                            val detail = prevContext.proceedDetail(source(gen - 1))
                            proceedCache(gen - 1) = detail
                            detail match {
                                case Left(detail) => Left(detail.nextContext)
                                case Right(error) => Right(error)
                            }
                        case Right(error) => Right(error)
                    }
                }
                contextCache(gen) = context
                context
        }
    def proceedAt(gen: Int): Either[NewParser#ProceedDetail, ParsingError] =
        proceedCache get gen match {
            case Some(context) => context
            case None =>
                contextAt(gen + 1)
                proceedCache(gen)
        }

    def errorControl(message: String): Control = {
        val control = new Label(graphView, SWT.NONE)
        control.setText(message)
        control
    }

    val graphCache = scala.collection.mutable.Map[VisualizationLocation, Control]()
    def graphAt(location: VisualizationLocation): Control = {
        graphCache get location match {
            case Some(control) => control
            case None =>
                val control = location.stage match {
                    case 0 =>
                        contextAt(location.baseGen) match {
                            case Left(context) =>
                                new NewParsingContextGraphVisualizeWidget(graphView, SWT.NONE, grammar, context)
                            case Right(error) =>
                                errorControl(error.msg)
                        }
                    case 1 =>
                        // Expand
                        (contextAt(location.baseGen), proceedAt(location.baseGen)) match {
                            case (Left(context), Left(proceed)) =>
                                new NewParserExpandedGraphVisualizeWidget(graphView, SWT.NONE, grammar, context, proceed)
                            case (Right(error), _) => errorControl(error.msg)
                            case (_, Right(error)) => errorControl(error.msg)
                        }
                    case 2 =>
                        // PreLift
                        (contextAt(location.baseGen), proceedAt(location.baseGen)) match {
                            case (Left(context), Left(proceed)) =>
                                new NewParserPreLiftGraphVisualizeWidget(graphView, SWT.NONE, grammar, context, proceed)
                            case (Right(error), _) => errorControl(error.msg)
                            case (_, Right(error)) => errorControl(error.msg)
                        }
                    case 3 =>
                        // Revert and Trim
                        errorControl("TODO")
                    case 4 =>
                        // Final Lift
                        errorControl("TODO")
                }
                graphCache(location) = finalizeView(control)
                control
        }
    }

    def keyListener = new KeyListener() {
        def keyPressed(x: KeyEvent): Unit = {
            x.keyCode match {
                case SWT.ARROW_LEFT => updateLocation(currentLocation.previousLocation)
                case SWT.ARROW_RIGHT => updateLocation(currentLocation.nextLocation)
                case SWT.ARROW_UP => updateLocation(currentLocation.previousBase)
                case SWT.ARROW_DOWN => updateLocation(currentLocation.nextBase)
                case code =>
                    println(s"keyPressed: $code")
            }
        }

        def keyReleased(x: KeyEvent): Unit = {}
    }
    def finalizeView(v: Control): Control = {
        v.addKeyListener(keyListener)
        v match {
            case v: ParsingContextGraphVisualizeWidget => v.graph.addKeyListener(keyListener)
            case v: ParsingContextProceedVisualizeWidget => v.graph.addKeyListener(keyListener)
            case _ =>
        }
        v
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
    trait Resources {
        val default12Font: Font
        val fixedWidth12Font: Font
        val italic14Font: Font
        val bold14Font: Font
        val smallFont: Font
    }

    def start(grammar: Grammar, source: Seq[ConcreteInput], display: Display, shell: Shell): Unit = {
        val resources = new NewParserVisualizer.Resources {
            val defaultFontName = JFaceResources.getTextFont.getFontData.head.getName
            val default12Font = new Font(null, defaultFontName, 12, SWT.NONE)
            val fixedWidth12Font = new Font(null, defaultFontName, 12, SWT.NONE)
            val italic14Font = new Font(null, defaultFontName, 14, SWT.ITALIC)
            val bold14Font = new Font(null, defaultFontName, 14, SWT.BOLD)
            val smallFont = new Font(null, defaultFontName, 6, SWT.NONE)
        }
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