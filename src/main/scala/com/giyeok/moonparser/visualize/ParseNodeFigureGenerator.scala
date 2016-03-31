package com.giyeok.moonparser.visualize

import com.giyeok.moonparser.ParseTree.ParseNode
import com.giyeok.moonparser.Symbols._
import com.giyeok.moonparser.ParseTree.TreePrintableParseNode

class ParseNodeFigureGenerator[Fig](g: FigureGenerator.Generator[Fig], ap: FigureGenerator.Appearances[Fig]) {
    import com.giyeok.moonparser.ParseTree._
    import com.giyeok.moonparser.visualize.FigureGenerator.Spacing

    val symbolFigureGenerator = new SymbolFigureGenerator(g, ap)

    def parseNodeFig(n: ParseNode[Symbol]): Fig =
        parseNodeFig(n: ParseNode[Symbol], false, true)

    def parseNodeFig(n: ParseNode[Symbol], renderJoin: Boolean, renderWS: Boolean): Fig = {
        def parseNodeFig(n: ParseNode[Symbol]): Fig =
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
                case ParsedSymbolsSeq(sym, body, bodyWS) =>
                    val seq: Seq[Fig] = if (renderWS && bodyWS.isDefined) {
                        val (bws, idx0) = bodyWS.get
                        val idx = if (idx0.last == bws.size - 1) idx0 else (idx0 :+ bws.size)
                        (idx.foldLeft(0, Seq[Fig]()) { (m, idx) =>
                            val (lastIdx, seq) = m
                            val wsFigs = (lastIdx until idx) map { wsIdx => ap.wsBorder.applyToFigure(parseNodeFig(bws(wsIdx))) }
                            val symFig = ap.symbolBorder.applyToFigure(parseNodeFig(bws(idx)))
                            val newSeq = (seq ++ wsFigs) :+ symFig
                            (idx + 1, newSeq)
                        })._2
                    } else {
                        body map { b => ap.symbolBorder.applyToFigure(parseNodeFig(b)) }
                    }
                    g.verticalFig(Spacing.Small, Seq(
                        g.horizontalFig(Spacing.Medium, seq),
                        symbolFigureGenerator.symbolFig(sym)))
                case ParsedSymbolJoin(sym, body, join) =>
                    var content = Seq(ap.symbolBorder.applyToFigure(parseNodeFig(body)))
                    if (renderJoin) {
                        content ++= Seq(
                            g.textFig("&", ap.default),
                            ap.symbolBorder.applyToFigure(parseNodeFig(join)))
                    }
                    content :+= symbolFigureGenerator.symbolFig(sym)
                    g.verticalFig(Spacing.Small, content)
            }
        parseNodeFig(n)
    }
}
