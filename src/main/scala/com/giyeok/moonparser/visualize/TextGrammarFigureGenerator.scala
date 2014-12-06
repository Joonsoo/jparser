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

class TextGrammarFigureGenerator[Fig](grammar: Grammar, ap: GrammarFigureGenerator.Appearances[Fig], g: GrammarFigureGenerator.Generator[Fig]) {
    def generate: Fig =
        g.verticalFig(2, grammar.rules.toSeq map { d => ruleFigure((d._1, d._2.toSeq)) })

    def ruleFigure(definition: (String, Seq[Symbols.Symbol])): Fig =
        g.horizontalFig(4, Seq(
            g.textFig(definition._1, ap.nonterminal),
            g.textFig("::=", ap.default),
            g.verticalFig(4, definition._2 map { textSymbolFig(_) })))

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
                g.horizontalFig(0, join(chars.groups map {
                    case (f, t) if f == t => g.textFig(f.toString, ap.terminal)
                    case (f, t) =>
                        g.horizontalFig(0, Seq(g.textFig(s"$f", ap.terminal), g.textFig("-", ap.default), g.textFig(s"$t", ap.terminal)))
                }, g.textFig("|", ap.default)))
            case t: Terminal => g.textFig(t.toShortString, ap.terminal)
            case Empty => g.textFig("ε", ap.nonterminal)
            case Nonterminal(name) => g.textFig(name, ap.nonterminal)
            case Sequence(seq, ws) =>
                if (seq.isEmpty) {
                    g.horizontalFig(3, Seq())
                } else {
                    def adjExChars(list: List[Terminals.ExactChar]): Fig =
                        g.horizontalFig(1, list map { textSymbolFig(_) })
                    val grouped = seq.foldRight((List[Fig](), List[Terminals.ExactChar]())) {
                        ((i, m) =>
                            i match {
                                case terminal: Terminals.ExactChar => (m._1, terminal +: m._2)
                                case symbol if m._2.isEmpty => (textSymbolFig(symbol) +: m._1, List())
                                case symbol => (textSymbolFig(symbol) +: adjExChars(m._2) +: m._1, List())
                            })
                    }
                    g.horizontalFig(3, if (grouped._2.isEmpty) grouped._1 else adjExChars(grouped._2) +: grouped._1)
                }
            case OneOf(syms) =>
                g.horizontalFig(0, join((syms map { sym =>
                    if (needParentheses(sym)) g.horizontalFig(0, Seq(g.textFig("(", ap.default), textSymbolFig(sym), g.textFig(")", ap.default)))
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
                    g.horizontalFig(1, Seq(g.textFig("(", ap.default), textSymbolFig(sym), g.textFig(")" + rep, ap.default)))
                else g.horizontalFig(1, Seq(textSymbolFig(sym), g.textFig(rep, ap.default)))
            case _@ (Except(_, _) | LookaheadExcept(_) | Backup(_, _)) =>
                g.textFig("??", ap.default)
        }
    }
}
