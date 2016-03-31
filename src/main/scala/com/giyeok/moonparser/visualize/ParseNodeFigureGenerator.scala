package com.giyeok.moonparser.visualize

import com.giyeok.moonparser.ParseTree.ParseNode
import com.giyeok.moonparser.Symbols._
import com.giyeok.moonparser.ParseTree.TreePrintableParseNode

class ParseNodeFigureGenerator[Fig](g: FigureGenerator.Generator[Fig], ap: FigureGenerator.Appearances[Fig]) {
    import com.giyeok.moonparser.ParseTree._
    import com.giyeok.moonparser.visualize.FigureGenerator.Spacing

    val symbolFigureGenerator = new SymbolFigureGenerator(g, ap)

    def parseNodeFig(n: ParseNode[Symbol]): Fig = {
        n match {
            case ParsedEmpty(sym) =>
                g.verticalFig(Spacing.Small, Seq(
                    g.textFig("", ap.default),
                    symbolFigureGenerator.symbolFig(sym)))
            case ParsedTerminal(sym, child) =>
                g.verticalFig(Spacing.Small, Seq(
                    g.textFig(child.toCleanString, ap.input),
                    symbolFigureGenerator.symbolFig(sym)))
            case ParsedSymbol(sym, body) =>
                g.verticalFig(Spacing.Small, Seq(
                    ap.symbolBorder.applyToFigure(parseNodeFig(body)),
                    symbolFigureGenerator.symbolFig(sym)))
            case ParsedSymbolsSeq(sym, body) =>
                g.verticalFig(Spacing.Small, Seq(
                    g.horizontalFig(Spacing.Medium, body map { b => ap.symbolBorder.applyToFigure(parseNodeFig(b)) }),
                    symbolFigureGenerator.symbolFig(sym)))
            case ParsedSymbolJoin(sym, body, join) =>
                g.verticalFig(Spacing.Small, Seq(
                    ap.symbolBorder.applyToFigure(parseNodeFig(body)),
                    g.textFig("&", ap.default),
                    ap.symbolBorder.applyToFigure(parseNodeFig(join)),
                    symbolFigureGenerator.symbolFig(sym)))
        }
    }
}
