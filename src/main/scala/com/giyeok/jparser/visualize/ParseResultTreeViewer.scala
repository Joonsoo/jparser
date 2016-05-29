package com.giyeok.jparser.visualize

import org.eclipse.swt.widgets._
import org.eclipse.swt.layout.FillLayout
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.swt.events.KeyAdapter
import com.giyeok.jparser.ParseResultTree.Node
import org.eclipse.swt.SWT
import org.eclipse.draw2d.Figure
import com.giyeok.jparser.Symbols.Symbol

class ParseResultTreeViewer(node: Node, figureGenerator: FigureGenerator.Generator[Figure], figureAppearances: FigureGenerator.Appearances[Figure], parseTreeFigureGenerator: ParseResultTreeFigureGenerator[Figure]) {
    val shell = new Shell(Display.getDefault())
    shell.setLayout(new FillLayout())
    val figCanvas = new FigureCanvas(shell)
    shell.addListener(SWT.Close, new Listener() {
        def handleEvent(e: Event): Unit = {
            shell.dispose()
        }
    })
    // shell.setText(node.toShortString)

    class MutableRenderingStatus(var horizontal: Boolean, var renderJoin: Boolean, var renderWS: Boolean, var renderLookaheadExcept: Boolean)
    val rs = new MutableRenderingStatus(true, true, true, true)
    def resetContents(): Unit = {
        val nodeFig =
            if (rs.horizontal) parseTreeFigureGenerator.parseNodeHFig(node, ParseResultTreeFigureGenerator.RenderingConfiguration(rs.renderJoin, rs.renderWS, rs.renderLookaheadExcept))
            else parseTreeFigureGenerator.parseNodeVFig(node, ParseResultTreeFigureGenerator.RenderingConfiguration(rs.renderJoin, rs.renderWS, rs.renderLookaheadExcept))
        figCanvas.setContents(
            figureGenerator.verticalFig(FigureGenerator.Spacing.Big, Seq(
                figureGenerator.textFig(s"${if (rs.horizontal) "Horizontal" else "Vertical"} renderJoin=${rs.renderJoin}, renderWS=${rs.renderWS}, renderLookaheadExcept=${rs.renderLookaheadExcept}", figureAppearances.default),
                nodeFig)))
    }
    resetContents()

    figCanvas.addKeyListener(new KeyAdapter() {
        override def keyPressed(e: org.eclipse.swt.events.KeyEvent): Unit = {
            if (e.keyCode == '`'.toInt) {
                rs.horizontal = !rs.horizontal
            }
            if (e.keyCode == '1'.toInt) {
                rs.renderJoin = !rs.renderJoin
            }
            if (e.keyCode == '2'.toInt) {
                rs.renderWS = !rs.renderWS
            }
            if (e.keyCode == '3'.toInt) {
                rs.renderLookaheadExcept = !rs.renderLookaheadExcept
            }
            resetContents()
        }
    })

    def start(): Unit = {
        shell.open()
    }
}
