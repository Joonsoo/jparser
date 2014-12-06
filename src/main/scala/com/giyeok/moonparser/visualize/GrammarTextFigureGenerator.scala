package com.giyeok.moonparser.visualize

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Symbols
import com.giyeok.moonparser.Symbols.Backup
import com.giyeok.moonparser.Symbols.CharsGrouping
import com.giyeok.moonparser.Symbols.Empty
import com.giyeok.moonparser.Symbols.Except
import com.giyeok.moonparser.Symbols.LookaheadExcept
import com.giyeok.moonparser.Symbols.Nonterminal
import com.giyeok.moonparser.Symbols.OneOf
import com.giyeok.moonparser.Symbols.Repeat
import com.giyeok.moonparser.Symbols.Sequence
import com.giyeok.moonparser.Symbols.ShortStringSymbols
import com.giyeok.moonparser.Symbols.Symbol
import com.giyeok.moonparser.Symbols.Terminal
import com.giyeok.moonparser.Symbols.Terminals

object GrammarTextFigureGenerator {
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
        import org.eclipse.draw2d.{ ToolbarLayout, Figure, LayoutManager, Label }
        import org.eclipse.swt.graphics.{ Color, Font }

        case class Appearance(font: Font, color: Color) extends GrammarTextFigureGenerator.Appearance[Figure] {
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

            def textFig(text: String, appearance: GrammarTextFigureGenerator.Appearance[Figure]): Figure = {
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
        import scala.xml.{ MetaData, UnprefixedAttribute }

        case class AppearanceByClass(cls: String) extends GrammarTextFigureGenerator.Appearance[xml.Elem] {
            def applyToFigure(fig: xml.Elem): xml.Elem =
                fig.copy(attributes = new UnprefixedAttribute("class", cls, xml.Null))
        }

        object Generator extends Generator[xml.Elem] {
            def textFig(text: String, appearance: GrammarTextFigureGenerator.Appearance[xml.Elem]): xml.Elem =
                appearance.applyToFigure(<span>{ text }</span>)
            def horizontalFig(spacing: Spacing.Value, children: Seq[xml.Elem]): xml.Elem =
                <table><tr>{ children map { fig => <td>{ fig }</td> } }</tr></table>
            def verticalFig(spacing: Spacing.Value, children: Seq[xml.Elem]): xml.Elem =
                <table>{ children map { fig => <tr><td>{ fig }</td></tr> } }</table>
        }
    }
}

class GrammarTextFigureGenerator[Fig](grammar: Grammar, ap: GrammarTextFigureGenerator.Appearances[Fig], g: GrammarTextFigureGenerator.Generator[Fig]) {
    import GrammarTextFigureGenerator.Spacing

    def generate: Fig =
        g.verticalFig(Spacing.Big, grammar.rules.toSeq map { d => ruleFigure((d._1, d._2.toSeq)) })

    def ruleFigure(definition: (String, Seq[Symbols.Symbol])): Fig =
        g.horizontalFig(Spacing.Medium, Seq(
            g.textFig(definition._1, ap.nonterminal),
            g.textFig("::=", ap.default),
            g.verticalFig(Spacing.Medium, definition._2 map { textSymbolFig(_) })))

    def textSymbolFig(rule: Symbol): Fig = {
        def join(list: List[Fig], joining: => Fig): List[Fig] = list match {
            case head +: List() => List(head)
            case head +: next +: List() => List(head, joining, next)
            case head +: next +: rest => head +: joining +: join(next +: rest, joining)
        }

        def needParentheses(symbol: Symbol): Boolean =
            symbol match {
                case _@ (Nonterminal(_) | Terminals.ExactChar(_) | Sequence(Seq(Terminals.ExactChar(_)), _)) => false
                case _ => true
            }

        rule match {
            case Terminals.ExactChar(c) => g.textFig(c.toString, ap.terminal)
            case chars: Terminals.Chars =>
                g.horizontalFig(Spacing.None, join(chars.groups map {
                    case (f, t) if f == t => g.textFig(f.toString, ap.terminal)
                    case (f, t) =>
                        g.horizontalFig(Spacing.None, Seq(g.textFig(s"$f", ap.terminal), g.textFig("-", ap.default), g.textFig(s"$t", ap.terminal)))
                }, g.textFig("|", ap.default)))
            case t: Terminal => g.textFig(t.toShortString, ap.terminal)
            case Empty => g.textFig("ε", ap.nonterminal)
            case Nonterminal(name) => g.textFig(name, ap.nonterminal)
            case Sequence(seq, ws) =>
                if (seq.isEmpty) {
                    g.horizontalFig(Spacing.Medium, Seq())
                } else {
                    def adjExChars(list: List[Terminals.ExactChar]): Fig =
                        g.horizontalFig(Spacing.None, list map { textSymbolFig(_) })
                    val grouped = seq.foldRight((List[Fig](), List[Terminals.ExactChar]())) {
                        ((i, m) =>
                            i match {
                                case terminal: Terminals.ExactChar => (m._1, terminal +: m._2)
                                case symbol if m._2.isEmpty => (textSymbolFig(symbol) +: m._1, List())
                                case symbol => (textSymbolFig(symbol) +: adjExChars(m._2) +: m._1, List())
                            })
                    }
                    g.horizontalFig(Spacing.Medium, if (grouped._2.isEmpty) grouped._1 else adjExChars(grouped._2) +: grouped._1)
                }
            case OneOf(syms) =>
                g.horizontalFig(Spacing.None, join((syms map { sym =>
                    if (needParentheses(sym)) g.horizontalFig(Spacing.None, Seq(g.textFig("(", ap.default), textSymbolFig(sym), g.textFig(")", ap.default)))
                    else textSymbolFig(sym)
                }).toList, g.textFig("|", ap.default)))
            case Repeat(sym, range) =>
                val rep: String = range match {
                    case Repeat.RangeFrom(from) if from == 0 => "*"
                    case Repeat.RangeFrom(from) if from == 1 => "+"
                    case Repeat.RangeTo(from, to) if from == 0 && to == 1 => "?"
                    case r => s"[${r.toShortString}]"
                }
                if (needParentheses(sym))
                    g.horizontalFig(Spacing.None, Seq(g.textFig("(", ap.default), textSymbolFig(sym), g.textFig(")" + rep, ap.default)))
                else g.horizontalFig(Spacing.None, Seq(textSymbolFig(sym), g.textFig(rep, ap.default)))
            case _@ (Except(_, _) | LookaheadExcept(_) | Backup(_, _)) =>
                g.textFig("??", ap.default)
        }
    }
}
