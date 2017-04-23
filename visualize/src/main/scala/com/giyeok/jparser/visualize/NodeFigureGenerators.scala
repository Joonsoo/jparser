package com.giyeok.jparser.visualize

import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.visualize.FigureGenerator.Spacing

class NodeFigureGenerators[Fig](
        val fig: FigureGenerator.Generator[Fig],
        val appear: FigureGenerator.Appearances[Fig],
        val symbol: SymbolFigureGenerator[Fig]
) {

    def kernelFig(grammar: NGrammar, kernel: Kernel): Fig = {
        val Kernel(symbolId, pointer, beginGen, endGen) = kernel

        fig.horizontalFig(Spacing.Big, Seq(
            symbol.symbolPointerFig(grammar, symbolId, pointer),
            fig.textFig(s"$beginGen-$endGen", appear.default)
        ))
    }

    def nodeFig(grammar: NGrammar, node: Node): Fig = {
        val Node(kernel, condition) = node
        fig.verticalFig(Spacing.Medium, Seq(
            kernelFig(grammar, kernel),
            conditionFig(grammar, condition)
        ))
    }

    def conditionFig(grammar: NGrammar, condition: AcceptCondition): Fig = {
        val d = appear.default
        def joinFigs(figs: Seq[Fig], joiner: => Fig): Seq[Fig] =
            figs.tail.foldLeft(Seq(figs.head)) { (cc, i) => cc :+ joiner :+ i }

        condition match {
            case Always => fig.textFig("always", d)
            case Never => fig.textFig("never", d)
            case And(conds) =>
                val condsFig = conds.toSeq map { conditionFig(grammar, _) }
                fig.horizontalFig(Spacing.Small, fig.textFig("(", d) +: joinFigs(condsFig, fig.textFig("&", d)) :+ fig.textFig(")", d))
            case Or(conds) =>
                val condsFig = conds.toSeq map { conditionFig(grammar, _) }
                fig.horizontalFig(Spacing.Small, fig.textFig("(", d) +: joinFigs(condsFig, fig.textFig("|", d)) :+ fig.textFig(")", d))
            case Until(sym, beginGen, activeGen) =>
                fig.horizontalFig(Spacing.Small, Seq(
                    fig.textFig("Until(", d),
                    symbol.symbolFig(sym.symbol),
                    fig.textFig(s", $beginGen..$activeGen)", d)
                ))
            case After(sym, beginGen, activeGen) =>
                fig.horizontalFig(Spacing.Small, Seq(
                    fig.textFig("After(", d),
                    symbol.symbolFig(sym.symbol),
                    fig.textFig(s", $beginGen..$activeGen)", d)
                ))
            case Unless(sym, beginGen, targetGen) =>
                fig.horizontalFig(Spacing.Small, Seq(
                    fig.textFig("Unless(", d),
                    symbol.symbolFig(sym.symbol),
                    fig.textFig(s", $beginGen..$targetGen)", d)
                ))
            case OnlyIf(sym, beginGen, targetGen) =>
                fig.horizontalFig(Spacing.Small, Seq(
                    fig.textFig("OnlyIf(", d),
                    symbol.symbolFig(sym.symbol),
                    fig.textFig(s", $beginGen..$targetGen)", d)
                ))
        }
    }
}
