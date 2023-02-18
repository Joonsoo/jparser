package com.giyeok.jparser.visualize

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.nparser.AcceptCondition._
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
            case NotExists(beginGen, endGen, symbolId) =>
                fig.horizontalFig(Spacing.Big, Seq(
                    fig.textFig("NotExists", d),
                    fig.textFig(s"$beginGen $endGen", d),
                    symbol.symbolFig(grammar, symbolId)
                ))
            case Exists(beginGen, endGen, symbolId) =>
                fig.horizontalFig(Spacing.Big, Seq(
                    fig.textFig("Exists", d),
                    fig.textFig(s"$beginGen $endGen", d),
                    symbol.symbolFig(grammar, symbolId)
                ))
            case Unless(beginGen, endGen, symbolId) =>
                fig.horizontalFig(Spacing.Big, Seq(
                    fig.textFig("Unless", d),
                    fig.textFig(s"$beginGen $endGen", d),
                    symbol.symbolFig(grammar, symbolId)
                ))
            case OnlyIf(beginGen, endGen, symbolId) =>
                fig.horizontalFig(Spacing.Big, Seq(
                    fig.textFig("OnlyIf", d),
                    fig.textFig(s"$beginGen $endGen", d),
                    symbol.symbolFig(grammar, symbolId)
                ))
        }
    }
}
