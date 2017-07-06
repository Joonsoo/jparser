package com.giyeok.jparser.visualize

import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.ParsingContext
import com.giyeok.jparser.nparser.State
import com.giyeok.jparser.nparser.SymbolIdAndBeginGen
import com.giyeok.jparser.visualize.FigureGenerator.Spacing
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite

trait StateFigureGen {
    val fig: StateFigureGenerators[Figure]
    val grammar: NGrammar

    def figureOf(state: State, expects: Set[SymbolIdAndBeginGen]): (Figure, Figure) = {
        val stateFigure = fig.stateFig(grammar, state)
        stateFigure.setBorder(new LineBorder(ColorConstants.black, 1))

        if (expects.isEmpty) {
            (stateFigure, stateFigure)
        } else {
            val baseFigs = expects.toSeq sortBy { b => (b.symbolId, b.beginGen) } map { base => fig.baseFig(grammar, base) }
            baseFigs foreach { _.setBorder(new LineBorder(ColorConstants.black, 1)) }
            val itemFigure = fig.fig.horizontalFig(Spacing.Small, Seq(
                stateFigure,
                fig.fig.textFig("->", fig.appear.small),
                fig.fig.horizontalFig(Spacing.Big, baseFigs)
            ))
            (stateFigure, itemFigure)
        }
    }
}

class ParsingContextWidget(parent: Composite, style: Int, val fig: StateFigureGenerators[Figure], val grammar: NGrammar, ctx: ParsingContext) extends Composite(parent, SWT.NONE) with StateFigureGen {
    setLayout(new FillLayout())

    val figureCanvas = new FigureCanvas(this, SWT.NONE)

    {
        val figure = new Figure
        figure.setLayoutManager({
            val l = new ToolbarLayout(false)
            l.setSpacing(3)
            l
        })
        val states = ctx.states.toSeq sortBy { s => (s.kernel.symbolId, s.kernel.pointer, s.kernel.beginGen, s.kernel.endGen) }
        states foreach { state =>
            figure.add(figureOf(state, ctx.expectsByStart(state) map { _.expect })._2)
        }
        figureCanvas.setContents(figure)
    }

    override def addKeyListener(listener: KeyListener): Unit = {
        super.addKeyListener(listener)
        figureCanvas.addKeyListener(listener)
    }

    addKeyListener(new KeyListener {
        override def keyPressed(e: KeyEvent): Unit = {
            e.keyCode match {
                case 'L' | 'l' =>
                    // TODO
                    println("latex::")
                case _ => // do nothing
            }
        }

        override def keyReleased(e: KeyEvent): Unit = {}
    })
}

class ParsingContextTransitionWidget(parent: Composite, style: Int, val fig: StateFigureGenerators[Figure], val grammar: NGrammar, baseCtx: ParsingContext, nextCtx: ParsingContext) extends Composite(parent, SWT.NONE) with StateFigureGen {
    setLayout(new FillLayout())

    val figureCanvas = new FigureCanvas(this, SWT.NONE)

    {
        val figure = new Figure
        figure.setLayoutManager({
            val l = new ToolbarLayout(false)
            l.setSpacing(3)
            l
        })
        val states = (baseCtx.states ++ nextCtx.states).toSeq sortBy { s => (s.kernel.symbolId, s.kernel.pointer, s.kernel.beginGen, s.kernel.endGen) }
        states foreach { state =>
            val expects = baseCtx.expectsByStart.getOrElse(state, Set()) ++ nextCtx.expectsByStart.getOrElse(state, Set())
            val (stateFigure, itemFigure) = figureOf(state, expects map { _.expect })
            val isBase = baseCtx.states contains state
            val isNew = nextCtx.states contains state
            (isBase, isNew) match {
                case (true, false) =>
                    // 없어진 state
                    stateFigure.setBorder(new LineBorder(ColorConstants.lightGray, 1))
                case (false, true) =>
                    // 새 state
                    stateFigure.setBorder(new LineBorder(ColorConstants.blue, 2))
                case _ =>
                    // 살아남은 state
                    stateFigure.setBorder(new LineBorder(ColorConstants.black, 1))
            }
            figure.add(itemFigure)
        }
        figureCanvas.setContents(figure)
    }

    override def addKeyListener(listener: KeyListener): Unit = {
        super.addKeyListener(listener)
        figureCanvas.addKeyListener(listener)
    }
}
