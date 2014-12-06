package com.giyeok.moonparser.visualize

import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.Label
import org.eclipse.draw2d.ToolbarLayout
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Symbols
import com.giyeok.moonparser.Symbols.ShortStringSymbols
import com.giyeok.moonparser.Symbols._
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.Color
import org.eclipse.draw2d.LayoutManager

object GrammarFigure {
    case class Appearance(font: Font, color: Color)
    trait Appearances {
        val default: Appearance
        val nonterminal: Appearance
        val terminal: Appearance
    }
}

class TextGrammarFigure(grammar: Grammar, fonts: GrammarFigure.Appearances) extends Figure {
    def toolbarLayoutWith(vertical: Boolean, spacing: Int): ToolbarLayout = {
        val layout = new ToolbarLayout(vertical)
        layout.setSpacing(spacing)
        layout
    }

    def labelWith(text: String, appearance: GrammarFigure.Appearance): Label = {
        val label = new Label
        label.setText(text)
        label.setFont(appearance.font)
        label.setForegroundColor(appearance.color)
        label
    }

    setLayoutManager(toolbarLayoutWith(false, 2))

    grammar.rules.toSeq foreach { r => add(new RuleFigure(r)) }

    class RuleFigure(definition: (String, Set[Symbols.Symbol])) extends Figure {
        setLayoutManager(toolbarLayoutWith(true, 4))

        add(labelWith(definition._1, fonts.nonterminal))
        add(labelWith("::=", fonts.default))
        add({
            val rules = new Figure
            rules.setLayoutManager(toolbarLayoutWith(false, 4))
            definition._2 foreach { rule => rules.add(textSymbolFig(rule)) }
            rules
        })
    }

    def textSymbolFig(rule: Symbol): Figure = {
        def join(list: List[Figure], joining: => Figure): List[Figure] = list match {
            case head +: List() => List(head)
            case head +: next +: List() => List(head, joining, next)
            case head +: next +: rest => head +: joining +: join(next +: rest, joining)
        }

        def figWith(layout: LayoutManager, children: Seq[Figure]): Label = {
            val fig = new Label
            fig.setLayoutManager(layout)
            children foreach { fig.add(_) }
            fig
        }

        def needParentheses(symbol: Symbol): Boolean =
            symbol match {
                case _@ (Nonterminal(_) | Terminals.ExactChar(_) | Sequence(Seq(Terminals.ExactChar(_)), _)) => false
                case _ => true
            }

        rule match {
            case Terminals.ExactChar(c) => labelWith(c.toString, fonts.terminal)
            case chars: Terminals.Chars =>
                figWith(toolbarLayoutWith(true, 0), join(chars.groups map {
                    case (f, t) if f == t => labelWith(f.toString, fonts.terminal)
                    case (f, t) =>
                        figWith(toolbarLayoutWith(true, 0), Seq(labelWith(s"$f", fonts.terminal), labelWith("-", fonts.default), labelWith(s"$t", fonts.terminal)))
                }, labelWith("|", fonts.default)))
            case t: Terminal => labelWith(t.toShortString, fonts.terminal)
            case Empty => labelWith("ε", fonts.nonterminal)
            case Nonterminal(name) => labelWith(name, fonts.nonterminal)
            case Sequence(seq, ws) =>
                val (gap, excharGap) = (6, 0)
                if (seq.isEmpty) {
                    figWith(toolbarLayoutWith(true, gap), Seq())
                } else {
                    def adjExChars(list: List[Terminals.ExactChar]): Figure =
                        figWith(toolbarLayoutWith(true, 1), list map { textSymbolFig(_) })
                    val grouped = seq.foldRight((List[Figure](), List[Terminals.ExactChar]())) { (i, m) =>
                        i match {
                            case terminal: Terminals.ExactChar => (m._1, terminal +: m._2)
                            case symbol if m._2.isEmpty => (textSymbolFig(symbol) +: m._1, List())
                            case symbol => (textSymbolFig(symbol) +: adjExChars(m._2) +: m._1, List())
                        }
                    }
                    figWith(toolbarLayoutWith(true, gap), if (grouped._2.isEmpty) grouped._1 else adjExChars(grouped._2) +: grouped._1)
                }
            case OneOf(syms) =>
                figWith(toolbarLayoutWith(true, 0), join((syms map { sym =>
                    if (needParentheses(sym)) figWith(toolbarLayoutWith(true, 0), Seq(labelWith("(", fonts.default), textSymbolFig(sym), labelWith(")", fonts.default)))
                    else textSymbolFig(sym)
                }).toList, labelWith("|", fonts.default)))
            case Repeat(sym, range) =>
                val rep: String = range match {
                    case Repeat.RangeFrom(from) if from == 0 => "*"
                    case Repeat.RangeFrom(from) if from == 1 => "+"
                    case Repeat.RangeTo(from, to) if from == 0 && to == 1 => "?"
                    case r => s"[${r.toShortString}]"
                }
                if (needParentheses(sym))
                    figWith(toolbarLayoutWith(true, 1), Seq(labelWith("(", fonts.default), textSymbolFig(sym), labelWith(")" + rep, fonts.default)))
                else figWith(toolbarLayoutWith(true, 1), Seq(textSymbolFig(sym), labelWith(rep, fonts.default)))
            case Except(sym, except) =>
                labelWith("??", fonts.default)
            case LookaheadExcept(except) =>
                labelWith("??", fonts.default)
            case Backup(sym, backup) =>
                labelWith("??", fonts.default)
        }
    }
}
