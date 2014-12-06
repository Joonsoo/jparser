package com.giyeok.moonparser.visualize

import scala.Left
import scala.Right

import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Inputs.Input
import com.giyeok.moonparser.Inputs.InputToShortString
import com.giyeok.moonparser.Parser

object ParseGraphVisualizer {
    trait Resources {
        val default12Font: Font
        val fixedWidth12Font: Font
        val italic14Font: Font
        val bold14Font: Font
    }

    def start(grammar: Grammar, input: Seq[Input], display: Display, shell: Shell): Unit = {
        val resources = new Resources {
            val default12Font = new Font(null, "Monaco", 12, SWT.NONE)
            val fixedWidth12Font = new Font(null, "Monaco", 12, SWT.NONE)
            val italic14Font = new Font(null, "Monaco", 14, SWT.ITALIC)
            val bold14Font = new Font(null, "Monaco", 14, SWT.BOLD)
        }

        val layout = new StackLayout

        shell.setText("Parsing Graph")
        shell.setLayout(layout)

        val parser = new Parser(grammar)
        val source = input

        val fin = source.scanLeft[Either[Parser#ParsingContext, Parser#ParsingError], Seq[Either[Parser#ParsingContext, Parser#ParsingError]]](Left[Parser#ParsingContext, Parser#ParsingError](parser.startingContext)) {
            (ctx, terminal) =>
                ctx match {
                    case Left(ctx) =>
                        //Try(ctx proceedTerminal terminal).getOrElse(Right(parser.ParsingErrors.UnexpectedInput(terminal)))
                        (ctx proceedTerminal terminal) match {
                            case Left(next) => Left(next)
                            case Right(error) => Right(error.asInstanceOf[Parser#ParsingError])
                        }
                    case error @ Right(_) => error
                }
        }
        assert(fin.length == (source.length + 1))
        val views: Seq[Control] = (fin zip ((source map { Some(_) }) :+ None)) map {
            case (Left(ctx), src) =>
                new ParsingContextGraphVisualizeWidget(shell, resources, ctx, src)
            case (Right(error), _) =>
                val label = new Label(shell, SWT.NONE)
                label.setAlignment(SWT.CENTER)
                label.setText(error.msg)
                label
        }

        var currentLocation = 0

        def updateLocation(newLocation: Int): Unit = {
            if (newLocation >= 0 && newLocation <= source.size) {
                currentLocation = newLocation
                val sourceStr = source map { _.toCleanString }
                shell.setText((sourceStr take newLocation).mkString + "*" + (sourceStr drop newLocation).mkString)
                layout.topControl = views(newLocation)
                shell.layout()
            }
        }
        updateLocation(currentLocation)

        val keyListener = new KeyListener() {
            def keyPressed(x: org.eclipse.swt.events.KeyEvent): Unit = {
                x.keyCode match {
                    case SWT.ARROW_LEFT => updateLocation(currentLocation - 1)
                    case SWT.ARROW_RIGHT => updateLocation(currentLocation + 1)
                    case code =>
                }
            }

            def keyReleased(x: org.eclipse.swt.events.KeyEvent): Unit = {}
        }
        shell.addKeyListener(keyListener)
        views foreach { v => v.addKeyListener(keyListener) }
        views collect { case v: ParsingContextGraphVisualizeWidget => v.graph.addKeyListener(keyListener) }

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
