package com.giyeok.moonparser.visualize

import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.LayoutManager
import org.eclipse.draw2d.Label
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Font
import scala.xml.MetaData
import scala.xml.UnprefixedAttribute

object GrammarFigureGenerator {
    trait Appearance[Figure] {
        def applyToFigure(fig: Figure): Figure
    }

    trait Appearances[Figure] {
        val default: Appearance[Figure]
        val nonterminal: Appearance[Figure]
        val terminal: Appearance[Figure]
    }

    trait Generator[Figure] {
        def textFig(text: String, appearance: Appearance[Figure]): Figure
        def horizontalFig(spacing: Spacing.Value, children: Seq[Figure]): Figure
        def verticalFig(spacing: Spacing.Value, children: Seq[Figure]): Figure
    }

    object Spacing extends Enumeration {
        val None, Small, Medium, Big = Value
    }

    object draw2d {
        case class Appearance(font: Font, color: Color) extends GrammarFigureGenerator.Appearance[Figure] {
            def applyToFigure(fig: Figure): Figure = {
                fig.setFont(font)
                fig.setForegroundColor(color)
                fig
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

            def textFig(text: String, appearance: GrammarFigureGenerator.Appearance[Figure]): Figure = {
                val label = new Label
                label.setText(text)
                appearance.applyToFigure(label)
            }
            def horizontalFig(spacing: Spacing.Value, children: Seq[Figure]): Figure =
                figWith(toolbarLayoutWith(true, spacing), children)
            def verticalFig(spacing: Spacing.Value, children: Seq[Figure]): Figure =
                figWith(toolbarLayoutWith(false, spacing), children)
        }
    }

    object html {
        type Fig = xml.Elem
        case class AppearanceClass(cls: String) extends GrammarFigureGenerator.Appearance[Fig] {
            def applyToFigure(fig: Fig): Fig =
                fig.copy(attributes = new UnprefixedAttribute("class", cls, xml.Null))
        }

        object Generator extends Generator[Fig] {
            def textFig(text: String, appearance: GrammarFigureGenerator.Appearance[Fig]): Fig =
                appearance.applyToFigure(<span>{ text }</span>)
            def horizontalFig(spacing: Spacing.Value, children: Seq[Fig]): Fig =
                <table><tr>{ children map { fig => <td>{ fig }</td> } }</tr></table>
            def verticalFig(spacing: Spacing.Value, children: Seq[Fig]): Fig =
                <table>{ children map { fig => <tr><td>{ fig }</td></tr> } }</table>
        }
    }
}
