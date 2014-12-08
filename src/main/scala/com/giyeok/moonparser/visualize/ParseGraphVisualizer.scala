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

object ParseGraphVisualizer {
    trait Resources {
        val default12Font: Font
        val fixedWidth12Font: Font
        val italic14Font: Font
        val bold14Font: Font
    }

    def start(grammar: Grammar, source: Seq[Input], display: Display, shell: Shell): Unit = {
        val resources = new Resources {
            val default12Font = new Font(null, "Monaco", 12, SWT.NONE)
            val fixedWidth12Font = new Font(null, "Monaco", 12, SWT.NONE)
            val italic14Font = new Font(null, "Monaco", 14, SWT.ITALIC)
            val bold14Font = new Font(null, "Monaco", 14, SWT.BOLD)
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
        val sourceFont = new Font(null, "Monaco", 14, SWT.BOLD)

        val layout = new StackLayout

        val graphView = new Composite(shell, SWT.NONE)
        graphView.setLayout(layout)
        graphView.setLayoutData(new GridData(GridData.FILL_BOTH))

        val parser = new Parser(grammar)

        val finReversed: (List[Either[Parser#ParsingContext, Parser#ParsingError]], List[Option[Parser#TerminalProceedLog]]) =
            source.foldLeft[(List[Either[Parser#ParsingContext, Parser#ParsingError]], List[Option[Parser#TerminalProceedLog]])](List(Left(parser.startingContext)), List()) { (cl, terminal) =>
                val (contexts, logs) = cl
                contexts match {
                    case Left(ctx) +: rest =>
                        //Try(ctx proceedTerminal terminal).getOrElse(Right(parser.ParsingErrors.UnexpectedInput(terminal)))
                        (ctx proceedTerminalVerbose terminal) match {
                            case Left((next, log)) => (Left(next) +: contexts, Some(log) +: logs)
                            case Right(error) => (Right(error.asInstanceOf[Parser#ParsingError]) +: contexts, None +: logs)
                        }
                    case (error @ Right(_)) +: rest => (error +: contexts, None +: logs)
                }
            }
        assert(finReversed._1.length == (source.length + 1))
        assert(finReversed._2.length == source.length)
        val fin = (finReversed._1.reverse, (None +: finReversed._2).reverse)
        val views: Seq[(Control, Option[Control])] = (fin._1 zip ((source map { Some(_) }) :+ None)).zipWithIndex map {
            case ((Left(ctx), src), idx) =>
                val logOpt = fin._2(idx)
                (new ParsingContextGraphVisualizeWidget(graphView, resources, ctx, logOpt),
                    logOpt map { new ParsingContextProceedVisualizeWidget(graphView, resources, ctx, _) })
            case ((Right(error), _), idx) =>
                val label = new Label(graphView, SWT.NONE)
                label.setAlignment(SWT.CENTER)
                label.setText(error.msg)
                (label, None)
        }

        var currentLocation = (0, false)

        def updateLocation(newLocation: Int, showProceed0: Boolean): Unit = {
            if (newLocation >= 0 && newLocation <= source.size) {
                val showProceed = showProceed0 && (views(newLocation)._2.isDefined)

                currentLocation = (newLocation, showProceed)

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
                    def listener(location: Int, showProceed: Boolean) = new draw2d.MouseListener() {
                        def mousePressed(e: draw2d.MouseEvent): Unit = {
                            updateLocation(location, showProceed)
                        }
                        def mouseReleased(e: draw2d.MouseEvent): Unit = {}
                        def mouseDoubleClicked(e: draw2d.MouseEvent): Unit = {}
                    }
                    def pointerFig(location: Int, addingWidth: Int): Figure = {
                        val pointer = new draw2d.Figure
                        if (location == newLocation) {
                            val ellipseFrame = new draw2d.Figure
                            ellipseFrame.setSize(12, 20)
                            ellipseFrame.setLayoutManager(new CenterLayout)
                            val ellipse = new draw2d.Ellipse
                            ellipse.setSize(6, 6)
                            ellipse.setBackgroundColor(if (showProceed) ColorConstants.orange else ColorConstants.black)
                            ellipseFrame.add(ellipse)
                            pointer.add(ellipseFrame)
                        }
                        pointer.setSize(12 + addingWidth, 20)
                        pointer.addMouseListener(listener(location, false))
                        pointer
                    }
                    source.zipWithIndex foreach { s =>
                        val pointer = pointerFig(s._2, 0)
                        val term = new draw2d.Label(s._1.toCleanString)
                        term.setForegroundColor(ColorConstants.red)
                        term.setFont(sourceFont)

                        term.addMouseListener(listener(s._2, true))
                        f.add(pointer)
                        f.add(term)
                    }
                    f.add(pointerFig(source.size, 50))
                    fin._1(newLocation) match {
                        case Left(ctx) =>
                            f.add(new draw2d.Label(s"N:${ctx.graph.nodes.size} E:${ctx.graph.edges.size} R:${ctx.resultCandidates.size}"))
                        case _ =>
                    }
                    f
                })

                val sourceStr = source map { _.toCleanString }
                shell.setText((sourceStr take newLocation).mkString + "*" + (sourceStr drop newLocation).mkString)
                layout.topControl = if (showProceed) views(newLocation)._2.get else views(newLocation)._1
                graphView.layout()
                shell.layout()
                sourceView.forceFocus()
            }
        }
        updateLocation(0, false)

        def keyListener = new KeyListener() {
            def keyPressed(x: KeyEvent): Unit = {
                x.keyCode match {
                    case SWT.ARROW_LEFT =>
                        if (currentLocation._2) updateLocation(currentLocation._1, false)
                        else updateLocation(currentLocation._1 - 1, true)
                    case SWT.ARROW_RIGHT =>
                        if (currentLocation._2 || views(currentLocation._1)._2.isEmpty) updateLocation(currentLocation._1 + 1, false)
                        else updateLocation(currentLocation._1, true)
                    case code =>
                }
            }

            def keyReleased(x: KeyEvent): Unit = {}
        }
        shell.addKeyListener(keyListener)
        views foreach { v =>
            v._1.addKeyListener(keyListener)
            v._2.foreach { _.addKeyListener(keyListener) }
            v._1 match {
                case v: ParsingContextGraphVisualizeWidget => v.graph.addKeyListener(keyListener)
                case v: ParsingContextProceedVisualizeWidget => v.graph.addKeyListener(keyListener)
                case _ =>
            }
            v._2 match {
                case Some(v: ParsingContextGraphVisualizeWidget) => v.graph.addKeyListener(keyListener)
                case Some(v: ParsingContextProceedVisualizeWidget) => v.graph.addKeyListener(keyListener)
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
