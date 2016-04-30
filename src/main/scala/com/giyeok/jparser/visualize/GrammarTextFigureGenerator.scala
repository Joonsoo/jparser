package com.giyeok.moonparser.visualize

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Symbols
import com.giyeok.moonparser.Symbols.Backup
import com.giyeok.moonparser.Symbols.Join
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
import java.lang.Character.UnicodeBlock
import FigureGenerator.Spacing

class GrammarTextFigureGenerator[Fig](grammar: Grammar, ap: FigureGenerator.Appearances[Fig], fg: FigureGenerator.Generator[Fig]) {
    def grammarFigure: Fig =
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
