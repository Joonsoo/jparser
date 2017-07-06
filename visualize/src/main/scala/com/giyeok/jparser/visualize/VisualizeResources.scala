package com.giyeok.jparser.visualize

import org.eclipse.swt.graphics.Font
import org.eclipse.jface.resource.JFaceResources
import org.eclipse.swt.SWT
import org.eclipse.draw2d.MarginBorder
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.LineBorder

trait VisualizeResources[Fig] {
    val default12Font: Font
    val fixedWidth12Font: Font
    val italic14Font: Font
    val bold14Font: Font
    val smallFont: Font
    val smallerFont: Font

    val nodeFigureGenerators: StateFigureGenerators[Fig]
}

object BasicVisualizeResources extends VisualizeResources[Figure] {
    val defaultFontName = JFaceResources.getTextFont.getFontData.head.getName
    val default12Font = new Font(null, defaultFontName, 12, SWT.NONE)
    val fixedWidth12Font = new Font(null, defaultFontName, 12, SWT.NONE)
    val italic14Font = new Font(null, defaultFontName, 14, SWT.ITALIC)
    val bold14Font = new Font(null, defaultFontName, 14, SWT.BOLD)
    val smallFont = new Font(null, defaultFontName, 8, SWT.NONE)
    val smallerFont = new Font(null, defaultFontName, 6, SWT.NONE)

    val default10Font = new Font(null, defaultFontName, 10, SWT.NONE)
    val nonterminalFont = new Font(null, defaultFontName, 12, SWT.BOLD)
    val terminalFont = new Font(null, defaultFontName, 12, SWT.NONE)

    val nodeFigureGenerators = {
        val figureGenerator: FigureGenerator.Generator[Figure] = FigureGenerator.draw2d.Generator

        val figureAppearances = new FigureGenerator.Appearances[Figure] {
            val default = FigureGenerator.draw2d.FontAppearance(default10Font, ColorConstants.black)
            val nonterminal = FigureGenerator.draw2d.FontAppearance(nonterminalFont, ColorConstants.blue)
            val terminal = FigureGenerator.draw2d.FontAppearance(terminalFont, ColorConstants.red)

            override val small = FigureGenerator.draw2d.FontAppearance(new Font(null, defaultFontName, 8, SWT.NONE), ColorConstants.gray)
            override val kernelDot = FigureGenerator.draw2d.FontAppearance(new Font(null, defaultFontName, 12, SWT.NONE), ColorConstants.green)
            override val symbolBorder = FigureGenerator.draw2d.BorderAppearance(new LineBorder(ColorConstants.lightGray))
            override val hSymbolBorder =
                new FigureGenerator.draw2d.ComplexAppearance(
                    FigureGenerator.draw2d.BorderAppearance(new MarginBorder(0, 1, 1, 1)),
                    FigureGenerator.draw2d.NewFigureAppearance(),
                    FigureGenerator.draw2d.BorderAppearance(new FigureGenerator.draw2d.PartialLineBorder(ColorConstants.lightGray, 1, false, true, true, true)))
            override val vSymbolBorder =
                new FigureGenerator.draw2d.ComplexAppearance(
                    FigureGenerator.draw2d.BorderAppearance(new MarginBorder(1, 0, 1, 1)),
                    FigureGenerator.draw2d.NewFigureAppearance(),
                    FigureGenerator.draw2d.BorderAppearance(new FigureGenerator.draw2d.PartialLineBorder(ColorConstants.lightGray, 1, true, false, true, true)))
            override val wsBorder =
                new FigureGenerator.draw2d.ComplexAppearance(
                    FigureGenerator.draw2d.BorderAppearance(new MarginBorder(0, 1, 1, 1)),
                    FigureGenerator.draw2d.NewFigureAppearance(),
                    FigureGenerator.draw2d.BorderAppearance(new FigureGenerator.draw2d.PartialLineBorder(ColorConstants.lightBlue, 1, false, true, true, true)),
                    FigureGenerator.draw2d.BackgroundAppearance(ColorConstants.lightGray))
            override val joinHighlightBorder =
                new FigureGenerator.draw2d.ComplexAppearance(
                    FigureGenerator.draw2d.BorderAppearance(new MarginBorder(1, 1, 1, 1)),
                    FigureGenerator.draw2d.NewFigureAppearance(),
                    FigureGenerator.draw2d.BorderAppearance(new LineBorder(ColorConstants.red)))
        }

        val symbolFigureGenerator = new SymbolFigureGenerator(figureGenerator, figureAppearances)

        new StateFigureGenerators(figureGenerator, figureAppearances, symbolFigureGenerator)
    }
}
