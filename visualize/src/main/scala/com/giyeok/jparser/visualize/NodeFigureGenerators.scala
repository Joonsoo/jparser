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
            fig.textFig(s"$symbolId", appear.small),
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
            case Until(node, activeGen) =>
                fig.horizontalFig(Spacing.Small, Seq(
                    fig.textFig("Until(", d),
                    nodeFig(grammar, node),
                    fig.textFig(s", $activeGen)", d)
                ))
            case After(node, activeGen) =>
                fig.horizontalFig(Spacing.Small, Seq(
                    fig.textFig("After(", d),
                    nodeFig(grammar, node),
                    fig.textFig(s", $activeGen)", d)
                ))
            case Unless(node, targetGen) =>
                fig.horizontalFig(Spacing.Small, Seq(
                    fig.textFig("Unless(", d),
                    nodeFig(grammar, node),
                    fig.textFig(s", $targetGen)", d)
                ))
            case OnlyIf(node, targetGen) =>
                fig.horizontalFig(Spacing.Small, Seq(
                    fig.textFig("OnlyIf(", d),
                    nodeFig(grammar, node),
                    fig.textFig(s", $targetGen)", d)
                ))
        }
    }
}
