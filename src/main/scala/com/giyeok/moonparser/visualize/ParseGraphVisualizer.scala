package com.giyeok.moonparser.visualize

import scala.Left
import scala.Right
import org.eclipse.draw2d
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Inputs.Input
import com.giyeok.moonparser.Inputs.InputToShortString
import com.giyeok.moonparser.Parser
import org.eclipse.draw2d.geometry.Insets
import org.eclipse.draw2d.IFigure
import org.eclipse.draw2d.BorderLayout
import org.eclipse.draw2d.MarginBorder
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.AbstractLayout
import org.eclipse.draw2d.geometry.Dimension
import org.eclipse.draw2d.geometry.PrecisionRectangle
import org.eclipse.draw2d.geometry.Rectangle
import org.eclipse.jface.resource.JFaceResources
import com.giyeok.moonparser.Inputs
import org.eclipse.draw2d.AbstractBorder
import org.eclipse.draw2d.Graphics
import org.eclipse.swt.graphics.Color

object ParseGraphVisualizer {
    trait Resources {
        val default12Font: Font
        val fixedWidth12Font: Font
        val italic14Font: Font
        val bold14Font: Font
    }

    def start(grammar: Grammar, source: Seq[Input], display: Display, shell: Shell): Unit = {
        val resources = new Resources {
            val defaultFontName = "Consolas"
            val default12Font = new Font(null, defaultFontName, 12, SWT.NONE)
            val fixedWidth12Font = new Font(null, defaultFontName, 12, SWT.NONE)
            val italic14Font = new Font(null, defaultFontName, 14, SWT.ITALIC)
            val bold14Font = new Font(null, defaultFontName, 14, SWT.BOLD)
        }

        shell.setText("Parsing Graph")
        shell.setLayout({
            val l = new GridLayout
            l.marginWidth = 0
            l.marginHeight = 0
            l.verticalSpacing = 0
            l.horizontalSpacing = 0
            l
        })

        val sourceView = new FigureCanvas(shell, SWT.NONE)
        sourceView.setLayoutData(new GridData(GridData.FILL_HORIZONTAL))
        sourceView.setBackground(ColorConstants.white)
        val sourceFont = new Font(null, JFaceResources.getTextFont.getFontData.head.getName, 14, SWT.BOLD)

        val layout = new StackLayout

        val graphView = new Composite(shell, SWT.NONE)
        graphView.setLayout(layout)
        graphView.setLayoutData(new GridData(GridData.FILL_BOTH))

        val parser = new Parser(grammar)

        val finReversed: List[(Either[(Parser#ParsingContext, Parser#VerboseProceedLog), Parser#ParsingError])] =
            source.foldLeft[List[(Either[(Parser#ParsingContext, Parser#VerboseProceedLog), Parser#ParsingError])]](List((Left(parser.startingContext, parser.startingContextVerbose._2)))) { (cl, terminal) =>
                cl.head match {
                    case Left((ctx, _)) => (ctx proceedTerminalVerbose terminal) +: cl
                    case error @ Right(_) => error +: cl
                }
            }
        val fin = finReversed.reverse
        assert(fin.length == source.length + 1)

        case class VisualizationLocation(location: Int, showResult: Boolean) {
            def previousLocation = if (showResult) VisualizationLocation(location, false) else VisualizationLocation(location - 1, true)
            def nextLocation = if (showResult) VisualizationLocation(location + 1, false) else VisualizationLocation(location, true)

            def stringRepresentation = {
                val sourceStr = source map { _.toCleanString }

                val divider = location + (if (showResult) 1 else 0)
                if (location < 0 && !showResult) ("> " + (sourceStr.mkString))
                else ((sourceStr take divider).mkString + (if (showResult) "*" else ">") + (sourceStr drop divider).mkString)
            }
        }

        val views: Map[VisualizationLocation, Control] =
            (fin.zipWithIndex.foldLeft((Option.empty[Parser#ParsingContext], Map[VisualizationLocation, Control]())) { (m, i) =>
                val (lastCtx, map) = m
                i match {
                    case (Left((ctx, log)), idx) =>
                        val proceedWidget = new ParsingContextProceedVisualizeWidget(graphView, resources, lastCtx, log)
                        val resultWidget = new ParsingContextGraphVisualizeWidget(graphView, resources, ctx)
                        (Some(ctx), map + (VisualizationLocation(idx - 1, false) -> proceedWidget) + (VisualizationLocation(idx - 1, true) -> resultWidget))
                    case (Right(error), idx) =>
                        val errorView = new Label(graphView, SWT.NONE)
                        errorView.setText(error.msg)
                        errorView.setAlignment(SWT.CENTER)
                        (None, map + (VisualizationLocation(idx - 1, false) -> errorView) + (VisualizationLocation(idx - 1, true) -> errorView))
                }
            })._2

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

        var currentLocation = VisualizationLocation(-1, true)

        def updateLocation(newLocation: VisualizationLocation): Unit = {
            if (views contains newLocation) {
                currentLocation = newLocation

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
                        if (location == newLocation) {
                            val ellipseFrame = new draw2d.Figure
                            ellipseFrame.setSize(12, 20)
                            ellipseFrame.setLayoutManager(new CenterLayout)
                            val ellipse = new draw2d.Ellipse
                            ellipse.setSize(6, 6)
                            ellipse.setBackgroundColor(ColorConstants.black)
                            ellipseFrame.add(ellipse)
                            pointer.add(ellipseFrame)
                        }
                        pointer.setSize(12 + addingWidth, 20)
                        pointer.addMouseListener(listener(location))
                        pointer
                    }
                    def terminalFig(location: VisualizationLocation, s: Inputs.Input): Figure = {
                        val term = new draw2d.Label(s.toCleanString)
                        term.setForegroundColor(ColorConstants.red)
                        term.setFont(sourceFont)
                        if (location == currentLocation) {
                            term.setBorder(cursorBorder)
                        }
                        term.addMouseListener(listener(location))
                        term
                    }
                    f.add(pointerFig(VisualizationLocation(-1, false), 5))
                    source.zipWithIndex foreach { s =>
                        f.add(pointerFig(VisualizationLocation(s._2 - 1, true), 0))
                        f.add(terminalFig(VisualizationLocation(s._2, false), s._1))
                    }
                    f.add(pointerFig(VisualizationLocation(source.length - 1, true), 200))
                    f
                })

                shell.setText(s"${grammar.name}: ${currentLocation.stringRepresentation}")
                layout.topControl = views(currentLocation)
                graphView.layout()
                shell.layout()
                sourceView.setFocus()
            }
        }
        updateLocation(currentLocation)

        def keyListener = new KeyListener() {
            def keyPressed(x: KeyEvent): Unit = {
                x.keyCode match {
                    case SWT.ARROW_LEFT => updateLocation(currentLocation.previousLocation)
                    case SWT.ARROW_RIGHT => updateLocation(currentLocation.nextLocation)
                    case SWT.ARROW_UP => updateLocation(VisualizationLocation(currentLocation.location - 1, currentLocation.showResult))
                    case SWT.ARROW_DOWN => updateLocation(VisualizationLocation(currentLocation.location + 1, currentLocation.showResult))
                    case code =>
                }
            }

            def keyReleased(x: KeyEvent): Unit = {}
        }
        shell.addKeyListener(keyListener)
        views foreach { v =>
            v._2.addKeyListener(keyListener)
            v._2 match {
                case v: ParsingContextGraphVisualizeWidget => v.graph.addKeyListener(keyListener)
                case v: ParsingContextProceedVisualizeWidget => v.graph.addKeyListener(keyListener)
                case _ =>
            }
        }
        sourceView.addKeyListener(keyListener)

        shell.open()
    }

    def start(grammar: Grammar, input: Seq[Input]): Unit = {
        val display = new Display
        val shell = new Shell(display)

        start(grammar, input, display, shell)

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }
}
