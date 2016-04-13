package com.giyeok.moonparser.visualize

import org.eclipse.draw2d.Figure
import com.giyeok.moonparser.Parser
import com.giyeok.moonparser.visualize.FigureGenerator.Spacing

class SymbolProgressFigureGenerator[Fig](g: FigureGenerator.Generator[Fig], ap: FigureGenerator.Appearances[Fig]) {
    val symbolFigureGenerator = new SymbolFigureGenerator(g, ap)

    def symbolProgFig(n: Parser#SymbolProgress) = {
        def dot = g.textFig("\u2022", ap.kernelDot)
        def withDot[T](parsed: Option[T], figs: Fig*): Seq[Fig] =
            if (parsed.isEmpty) (dot +: figs.toSeq) else (figs.toSeq :+ dot)
        n match {
            case n: Parser#EmptyProgress =>
                g.horizontalFig(Spacing.Small, Seq(g.textFig("\u03B5", ap.default), dot))
            case n: Parser#TerminalProgress =>
                g.horizontalFig(Spacing.Small, withDot(n.parsed, symbolFigureGenerator.symbolFig(n.symbol)))
            case n: Parser#NonterminalProgress =>
                g.horizontalFig(Spacing.Small, withDot(n.parsed, symbolFigureGenerator.symbolFig(n.symbol)))
            case n: Parser#SequenceProgress =>
                val ls: (Seq[Fig], Seq[Fig]) = n.symbol.seq map { s => ap.symbolBorder.applyToFigure(symbolFigureGenerator.symbolFig(s)) } splitAt n.locInSeq
                g.horizontalFig(Spacing.Medium, (ls._1 :+ dot) ++ ls._2)
            case n: Parser#OneOfProgress =>
                g.horizontalFig(Spacing.Small, withDot(n.parsed, symbolFigureGenerator.symbolFig(n.symbol)))
            case n: Parser#ExceptProgress =>
                g.textFig(n.toShortString, ap.default)
            case n: Parser#RepeatProgress =>
                val canProceed = n.symbol.range canProceed n._children.size
                g.horizontalFig(Spacing.Small,
                    ((if (canProceed) Seq(dot) else Seq()) :+ symbolFigureGenerator.symbolFig(n.symbol)) ++ (if (n.canFinish) Seq(dot) else Seq()))
            case n: Parser#LookaheadExceptProgress =>
                g.textFig(n.toShortString, ap.default)
            case n: Parser#BackupProgress =>
                g.horizontalFig(Spacing.Small, withDot(n.parsed, symbolFigureGenerator.symbolFig(n.symbol)))
            case n: Parser#JoinProgress =>
                g.horizontalFig(Spacing.Small, withDot(n.parsed, symbolFigureGenerator.symbolFig(n.symbol)))
            case n: Parser#ProxyProgress =>
                g.horizontalFig(Spacing.Small, withDot(n.parsed, symbolFigureGenerator.symbolFig(n.symbol)))
            case n: Parser#LongestProgress =>
                g.horizontalFig(Spacing.Small, withDot(n.parsed, symbolFigureGenerator.symbolFig(n.symbol)))
            case n: Parser#EagerLongestProgress =>
                g.horizontalFig(Spacing.Small, withDot(n.parsed, symbolFigureGenerator.symbolFig(n.symbol)))
        }
    }
}
