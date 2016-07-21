package com.giyeok.jparser.visualize

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.Symbols.Join
import com.giyeok.jparser.Symbols.CharsGrouping
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
import FigureGenerator.Spacing

class GrammarDefinitionFigureGenerator[Fig](grammar: Grammar, ap: FigureGenerator.Appearances[Fig], fg: FigureGenerator.Generator[Fig]) {
    def grammarDefinitionFigure: Fig =
        fg.verticalFig(Spacing.Big, grammar.rules.toSeq map { d => ruleFigure((d._1, d._2.toSeq)) })

    val symgolFigureGenerator = new SymbolFigureGenerator[Fig](fg, ap)

    def ruleFigure(definition: (String, Seq[Symbols.Symbol])): Fig = {
        val rules = definition._2
        val ruleFigures: Seq[Fig] = rules map { symgolFigureGenerator.symbolFig(_) }
        val ruleFiguresWithSeparator: Seq[Fig] =
            if (ruleFigures.isEmpty) Seq(fg.horizontalFig(Spacing.Medium, Seq(fg.textFig("::= (Not defined)", ap.default))))
            else fg.horizontalFig(Spacing.Big, Seq(fg.textFig("::= ", ap.default), ruleFigures.head)) +: (ruleFigures.tail map { fig => fg.horizontalFig(Spacing.Medium, Seq(fg.textFig("  | ", ap.default), fig)) })

        fg.horizontalFig(Spacing.Medium, Seq(
            fg.textFig(definition._1, ap.nonterminal),
            fg.verticalFig(Spacing.Medium, ruleFiguresWithSeparator)))
    }
}
