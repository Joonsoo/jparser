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

class NewParserResultVisualizer(grammar: Grammar, source: Seq[ConcreteInput], display: Display, shell: Shell, resources: NewParserResultVisualizer.Resources) {
    val parser = new NewParser(grammar, ParseForestFunc)

    // 좌측 test string
    val sourceView = new FigureCanvas(shell, SWT.NONE)
    sourceView.setLayoutData(new GridData(GridData.FILL_HORIZONTAL))
    sourceView.setBackground(ColorConstants.white)

    def keyListener = new KeyListener() {
        def keyPressed(x: KeyEvent): Unit = {
            x.keyCode match {
                case code =>
                    println(s"keyPressed: $code")
            }
        }

        def keyReleased(x: KeyEvent): Unit = {}
    }

    def start(): Unit = {
        shell.setText("Parsing Result View")
        shell.setLayout({
            val l = new GridLayout
            l.marginWidth = 0
            l.marginHeight = 0
            l.verticalSpacing = 0
            l.horizontalSpacing = 0
            l
        })

        shell.addKeyListener(keyListener)

        shell.open()
    }
}

object NewParserResultVisualizer {
    trait Resources {
        val default12Font: Font
        val fixedWidth12Font: Font
        val italic14Font: Font
        val bold14Font: Font
        val smallFont: Font
    }

    def start(grammar: Grammar, source: Seq[ConcreteInput], display: Display, shell: Shell): Unit = {
        val resources = new Resources {
            val defaultFontName = JFaceResources.getTextFont.getFontData.head.getName
            val default12Font = new Font(null, defaultFontName, 12, SWT.NONE)
            val fixedWidth12Font = new Font(null, defaultFontName, 12, SWT.NONE)
            val italic14Font = new Font(null, defaultFontName, 14, SWT.ITALIC)
            val bold14Font = new Font(null, defaultFontName, 14, SWT.BOLD)
            val smallFont = new Font(null, defaultFontName, 6, SWT.NONE)
        }
        new NewParserResultVisualizer(grammar, source, display, shell, resources).start()
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
