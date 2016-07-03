package com.giyeok.jparser.visualize

import com.giyeok.jparser.ParseResultDerivationsSet
import com.giyeok.jparser.ParseResultDerivations._
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.visualize.FigureGenerator.Appearance
import com.giyeok.jparser.Symbols

class ParseResultDerivationsSetFigureGenerator[Fig](g: FigureGenerator.Generator[Fig], ap: FigureGenerator.Appearances[Fig]) {
    import com.giyeok.jparser.visualize.FigureGenerator.Spacing

    val symbolFigureGenerator = new SymbolFigureGenerator(g, ap)

    def parseNodeHFig(r: ParseResultDerivationsSet): Fig =
        parseNodeHFig(r: ParseResultDerivationsSet, ParseResultFigureGenerator.cleanestConfiguration)

    def parseNodeHFig(r: ParseResultDerivationsSet, renderConf: ParseResultFigureGenerator.RenderingConfiguration): Fig = {
        parseNodeFig(ap.hSymbolBorder, g.verticalFig _, g.horizontalFig _, renderConf)(r)
    }

    def parseNodeVFig(r: ParseResultDerivationsSet): Fig =
        parseNodeVFig(r: ParseResultDerivationsSet, ParseResultFigureGenerator.cleanestConfiguration)

    def parseNodeVFig(r: ParseResultDerivationsSet, renderConf: ParseResultFigureGenerator.RenderingConfiguration): Fig = {
        parseNodeFig(ap.vSymbolBorder, g.horizontalFig _, g.verticalFig _, renderConf)(r)
    }

    private def parseNodeFig(symbolBorder: FigureGenerator.Appearance[Fig], vfig: (Spacing.Value, Seq[Fig]) => Fig, hfig: (Spacing.Value, Seq[Fig]) => Fig, renderConf: ParseResultFigureGenerator.RenderingConfiguration)(r: ParseResultDerivationsSet): Fig = {
        def reduction(range: (Int, Int), reducedTo: Fig): Fig = {
            hfig(Spacing.Small, Seq(
                g.textFig(s"${range._1}-${range._2}", ap.small),
                reducedTo))
        }
        vfig(Spacing.Medium, r.derivations.toSeq.sortBy { _.range } map {
            case d @ TermFunc(position) =>
                reduction(d.range, g.textFig("λt", ap.input))
            case d @ Term(position, input) =>
                reduction(d.range, g.textFig(input.toShortString, ap.input))
            case d @ Bind(position, length, symbol) =>
                reduction(d.range, symbolFigureGenerator.symbolFig(symbol))
            case d @ LastChild(position, length, content) =>
                reduction(d.range, g.textFig(if (content) "child" else "whitespace", ap.input))
        })
    }
}
