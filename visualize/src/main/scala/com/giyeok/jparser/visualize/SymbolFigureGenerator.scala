package com.giyeok.jparser.visualize

import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar.NSymbol
import com.giyeok.jparser.visualize.FigureGenerator.Spacing

class SymbolFigureGenerator[Fig](fig: FigureGenerator.Generator[Fig], ap: FigureGenerator.Appearances[Fig]) {
    def symbolFig(symbol: Symbol): Fig = {
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

        def exactCharacterRepr(char: Char): String = char match {
            case c if 33 <= c && c <= 126 => c.toString
            case '\n' => "\\n"
            case '\r' => "\\r"
            case '\t' => "\\t"
            case c => f"\\u$c%04x"
        }
        def rangeCharactersRepr(start: Char, end: Char): (String, String) =
            (exactCharacterRepr(start), exactCharacterRepr(end))

        symbol match {
            case Terminals.ExactChar(c) => fig.textFig(exactCharacterRepr(c), ap.terminal)
            case chars: Terminals.Chars =>
                fig.horizontalFig(Spacing.None, join(chars.groups map {
                    case (f, t) if f == t => fig.textFig(exactCharacterRepr(f), ap.terminal)
                    case (f, t) =>
                        val (rangeStart: String, rangeEnd: String) = rangeCharactersRepr(f, t)
                        fig.horizontalFig(Spacing.None, Seq(fig.textFig(rangeStart, ap.terminal), fig.textFig("-", ap.default), fig.textFig(rangeEnd, ap.terminal)))
                }, fig.textFig("|", ap.default)))
            case t: Terminal => fig.textFig(t.toShortString, ap.terminal)
            case Start => fig.textFig("Start", ap.default)
            case Nonterminal(name) => fig.textFig(name, ap.nonterminal)
            case Sequence(seq, ws) =>
                if (seq.isEmpty) {
                    fig.textFig("ε", ap.nonterminal)
                } else {
                    def adjExChars(list: List[Terminals.ExactChar]): Fig =
                        fig.horizontalFig(Spacing.None, list map { symbolFig(_) })
                    val grouped = seq.foldRight((List[Fig](), List[Terminals.ExactChar]())) {
                        (i, m) =>
                            i match {
                                case terminal: Terminals.ExactChar => (m._1, terminal +: m._2)
                                case symbol if m._2.isEmpty => (symbolFig(symbol) +: m._1, List())
                                case symbol => (symbolFig(symbol) +: adjExChars(m._2) +: m._1, List())
                            }
                    }
                    fig.horizontalFig(Spacing.Medium, if (grouped._2.isEmpty) grouped._1 else adjExChars(grouped._2) +: grouped._1)
                }
            case OneOf(syms) =>
                if (syms.size == 2 && (syms contains Sequence(Seq(), Seq()))) {
                    // A? 의 경우
                    val opt = (syms - Sequence(Seq(), Seq())).head
                    fig.horizontalFig(Spacing.None, Seq(symbolFig(opt), fig.textFig("?", ap.small)))
                } else {
                    fig.horizontalFig(Spacing.None, join((syms map { sym =>
                        if (needParentheses(sym)) fig.horizontalFig(Spacing.None, Seq(fig.textFig("(", ap.default), symbolFig(sym), fig.textFig(")", ap.default)))
                        else symbolFig(sym)
                    }).toList, fig.textFig("|", ap.default)))
                }
            case Repeat(sym, lower) =>
                val rep: String = lower match {
                    case 0 => "*"
                    case 1 => "+"
                    case n => s"$n+"
                }
                if (needParentheses(sym))
                    fig.horizontalFig(Spacing.None, Seq(fig.textFig("(", ap.default), symbolFig(sym), fig.textFig(")" + rep, ap.default)))
                else fig.horizontalFig(Spacing.None, Seq(symbolFig(sym), fig.textFig(rep, ap.default)))
            case Except(sym, except) =>
                val symFig =
                    if (!needParentheses(sym)) symbolFig(sym)
                    else fig.horizontalFig(Spacing.None, Seq(fig.textFig("(", ap.default), symbolFig(sym), fig.textFig(")", ap.default)))
                val exceptFig =
                    if (!needParentheses(except)) symbolFig(except)
                    else fig.horizontalFig(Spacing.None, Seq(fig.textFig("(", ap.default), symbolFig(except), fig.textFig(")", ap.default)))

                fig.horizontalFig(Spacing.Medium, Seq(symFig, fig.textFig("except", ap.default), exceptFig))
            case LookaheadIs(lookahead) =>
                fig.horizontalFig(Spacing.Small, Seq(fig.textFig("(", ap.default), fig.textFig("lookahead_is", ap.default), symbolFig(lookahead), fig.textFig(")", ap.default)))
            case LookaheadExcept(except) =>
                fig.horizontalFig(Spacing.Small, Seq(fig.textFig("(", ap.default), fig.textFig("lookahead_except", ap.default), symbolFig(except), fig.textFig(")", ap.default)))
            case Proxy(sym) =>
                fig.horizontalFig(Spacing.Small, Seq(fig.textFig("P(", ap.default), symbolFig(sym), fig.textFig(")", ap.default)))
            case Join(sym, join) =>
                fig.horizontalFig(Spacing.Small, Seq(symbolFig(sym), fig.textFig("&", ap.default), symbolFig(join)))
            case Longest(sym) =>
                fig.horizontalFig(Spacing.Small, Seq(fig.textFig("L(", ap.default), symbolFig(sym), fig.textFig(")", ap.default)))
        }
    }

    def symbolFig(grammar: NGrammar, symbolId: Int): Fig = {
        fig.horizontalFig(Spacing.Small, Seq(
            fig.textFig(symbolId.toString, ap.small),
            symbolFig(grammar.symbolOf(symbolId).symbol)
        ))
    }

    private def dot = fig.textFig("\u2022", ap.kernelDot)

    def symbolPointerFig(symbol: Symbol, pointer: Int): Fig = {
        symbol match {
            case _: AtomicSymbol =>
                fig.horizontalFig(Spacing.Small, if (pointer == 0) Seq(dot, symbolFig(symbol)) else Seq(symbolFig(symbol), dot))

            case Sequence(sequence, _) =>
                val (p, f) = sequence.splitAt(pointer)
                val f0 = fig.horizontalFig(Spacing.Medium, p map { symbolFig })
                val f1 = fig.horizontalFig(Spacing.Medium, f map { symbolFig })
                fig.horizontalFig(Spacing.Small, Seq(f0, dot, f1))
        }
    }

    def symbolPointerFig(grammar: NGrammar, symbolId: Int, pointer: Int): Fig = {
        fig.horizontalFig(Spacing.Small, Seq(
            fig.textFig(symbolId.toString, ap.small),
            symbolPointerFig(grammar.symbolOf(symbolId).symbol, pointer)
        ))
    }
}
