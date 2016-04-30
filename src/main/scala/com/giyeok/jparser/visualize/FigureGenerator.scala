package com.giyeok.jparser.visualize

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.Symbols.Backup
import com.giyeok.jparser.Symbols.Join
import com.giyeok.jparser.Symbols.CharsGrouping
import com.giyeok.jparser.Symbols.Empty
import com.giyeok.jparser.Symbols.Except
import com.giyeok.jparser.Symbols.LookaheadExcept
import com.giyeok.jparser.Symbols.Nonterminal
import com.giyeok.jparser.Symbols.OneOf
import com.giyeok.jparser.Symbols.Repeat
import com.giyeok.jparser.Symbols.Sequence
import com.giyeok.jparser.Symbols.ShortStringSymbols
import com.giyeok.jparser.Symbols.Symbol
import com.giyeok.jparser.Symbols.Terminal
import com.giyeok.jparser.Symbols.Terminals
import java.lang.Character.UnicodeBlock
import org.eclipse.draw2d.Border
import org.eclipse.draw2d.AbstractBorder
import org.eclipse.mylar.zest.core.internal.viewers.figures.SubSupFigure

object FigureGenerator {
    trait Appearance[Figure] {
        def applyToFigure(fig: Figure): Figure
    }

    trait Appearances[Figure] {
        val default: Appearance[Figure]
        val nonterminal: Appearance[Figure]
        val terminal: Appearance[Figure]

        val input: Appearance[Figure] = EmptyAppearance
        val small: Appearance[Figure] = EmptyAppearance
        val kernelDot: Appearance[Figure] = EmptyAppearance
        val symbolBorder: Appearance[Figure] = EmptyAppearance
        val hSymbolBorder: Appearance[Figure] = EmptyAppearance
        val vSymbolBorder: Appearance[Figure] = EmptyAppearance
        val wsBorder: Appearance[Figure] = EmptyAppearance
        val joinHighlightBorder: Appearance[Figure] = EmptyAppearance

        object EmptyAppearance extends Appearance[Figure] {
            def applyToFigure(fig: Figure): Figure = {
                // do nothing
                fig
            }
        }
    }

    trait Generator[Figure] {
        def textFig(text: String, appearance: Appearance[Figure]): Figure
        // def textSupFig(text: String, appearance: Appearance[Figure]): Figure
        // def textSubFig(text: String, appearance: Appearance[Figure]): Figure
        def horizontalFig(spacing: Spacing.Value, children: Seq[Figure]): Figure
        def verticalFig(spacing: Spacing.Value, children: Seq[Figure]): Figure
        def subFig(child: Figure): Figure
        def supFig(child: Figure): Figure
    }

    object Spacing extends Enumeration {
        val None, Small, Medium, Big = Value
    }

    object draw2d {
        import org.eclipse.draw2d.{ ToolbarLayout, Figure, LayoutManager, Label }
        import org.eclipse.swt.graphics.{ Color, Font }

        case class FontAppearance(font: Font, color: Color) extends FigureGenerator.Appearance[Figure] {
            def applyToFigure(fig: Figure): Figure = {
                fig.setFont(font)
                fig.setForegroundColor(color)
                fig
            }
        }

        case class ComplexAppearance(appearances: FigureGenerator.Appearance[Figure]*) extends FigureGenerator.Appearance[Figure] {
            def applyToFigure(fig: Figure): Figure = {
                appearances.foldLeft(fig)((fig, ap) => ap.applyToFigure(fig))
            }
        }
        case class BorderAppearance(border: Border) extends FigureGenerator.Appearance[Figure] {
            def applyToFigure(fig: Figure): Figure = {
                fig.setBorder(border)
                fig
            }
        }
        case class NewFigureAppearance() extends FigureGenerator.Appearance[Figure] {
            def applyToFigure(fig: Figure): Figure = {
                val newFig = new Figure()
                newFig.add(fig)
                newFig.setLayoutManager(new ToolbarLayout())
                newFig
            }
        }
        case class BackgroundAppearance(color: Color) extends FigureGenerator.Appearance[Figure] {
            def applyToFigure(fig: Figure): Figure = {
                fig.setBackgroundColor(color)
                fig.setOpaque(true)
                fig
            }
        }
        class PartialLineBorder(color: Color, width: Int, top: Boolean, left: Boolean, bottom: Boolean, right: Boolean) extends AbstractBorder {
            import org.eclipse.draw2d.geometry.Insets
            import org.eclipse.draw2d.IFigure
            import org.eclipse.draw2d.Graphics
            import org.eclipse.draw2d.geometry.Rectangle

            private val tempRect = new Rectangle()
            def getInsets(figure: IFigure): Insets = new Insets(if (top) width else 0, if (left) width else 0, if (bottom) width else 0, if (right) width else 0)
            def paint(figure: IFigure, graphics: Graphics, insets: Insets): Unit = {
                tempRect.setBounds(figure.getBounds())
                val paintRect = tempRect.shrink(insets)
                graphics.setForegroundColor(color)
                graphics.setLineWidth(width)
                graphics.drawRectangle(tempRect)
                val halfWidth = (width + 1) / 2
                if (top) graphics.drawLine(paintRect.x, paintRect.y, paintRect.right, paintRect.y)
                if (left) graphics.drawLine(paintRect.x, paintRect.y, paintRect.x, paintRect.height)
                if (bottom) graphics.drawLine(paintRect.x, paintRect.bottom - halfWidth, paintRect.right, paintRect.bottom - halfWidth)
                if (right) graphics.drawLine(paintRect.right - halfWidth, paintRect.y, paintRect.right - halfWidth, paintRect.bottom)
            }
        }

        object Generator extends Generator[Figure] {
            private def toolbarLayoutWith(vertical: Boolean, spacing: Spacing.Value): ToolbarLayout = {
                val layout = new ToolbarLayout(vertical)
                layout.setSpacing(spacing match {
                    case Spacing.None => 0
                    case Spacing.Small => 1
                    case Spacing.Medium => 3
                    case Spacing.Big => 6
                })
                layout
            }

            private def figWith(layout: LayoutManager, children: Seq[Figure]): Label = {
                val fig = new Label
                fig.setLayoutManager(layout)
                children foreach { fig.add(_) }
                fig
            }

            def textFig(text: String, appearance: FigureGenerator.Appearance[Figure]): Figure = {
                val label = new Label
                label.setText(text)
                appearance.applyToFigure(label)
            }
            def horizontalFig(spacing: Spacing.Value, children: Seq[Figure]): Figure =
                figWith(toolbarLayoutWith(true, spacing), children)
            def verticalFig(spacing: Spacing.Value, children: Seq[Figure]): Figure =
                figWith(toolbarLayoutWith(false, spacing), children)

            def subFig(child: Figure): Figure = subFig(0.75, child)
            def supFig(child: Figure): Figure = supFig(0.75, child)
            def subFig(ratio: Double, child: Figure): Figure = {
                val fig = new SubSupFigure()
                fig.setLayoutManager(toolbarLayoutWith(true, Spacing.None))
                fig.add(child)
                fig.setScale(ratio)
                fig.setStickToTop(false)
                fig
            }
            def supFig(ratio: Double, child: Figure): Figure = {
                val fig = new SubSupFigure()
                fig.setLayoutManager(toolbarLayoutWith(true, Spacing.None))
                fig.add(child)
                fig.setScale(ratio)
                fig.setStickToTop(true)
                fig
            }
        }
    }

    object html {
        import scala.xml.{ MetaData, UnprefixedAttribute }

        case class AppearanceByClass(cls: String) extends FigureGenerator.Appearance[xml.Elem] {
            def applyToFigure(fig: xml.Elem): xml.Elem =
                fig.copy(attributes = new UnprefixedAttribute("class", cls, xml.Null))
        }

        object Generator extends Generator[xml.Elem] {
            def textFig(text: String, appearance: FigureGenerator.Appearance[xml.Elem]): xml.Elem =
                appearance.applyToFigure(<span>{ text }</span>)
            def horizontalFig(spacing: Spacing.Value, children: Seq[xml.Elem]): xml.Elem =
                <table><tr>{ children map { fig => <td>{ fig }</td> } }</tr></table>
            def verticalFig(spacing: Spacing.Value, children: Seq[xml.Elem]): xml.Elem =
                <table>{ children map { fig => <tr><td>{ fig }</td></tr> } }</table>
            def subFig(child: xml.Elem): xml.Elem =
                <sub>child</sub>
            def supFig(child: xml.Elem): xml.Elem =
                <sup>child</sup>
        }
    }
}
