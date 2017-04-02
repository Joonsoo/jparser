package com.giyeok.jparser.visualize

import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.NGrammar.NAtomicSymbol
import com.giyeok.jparser.visualize.FigureGenerator.Spacing

class NodeFigureGenerators[Fig](
        val fig: FigureGenerator.Generator[Fig],
        val appear: FigureGenerator.Appearances[Fig],
        val symbol: SymbolFigureGenerator[Fig]
) {

    def symbolFigure(grammar: NGrammar, symbolId: Int): Fig = {
        symbol.symbolFig(grammar.nsymbols(symbolId).symbol)
    }

    def dot = fig.textFig("\u2022", appear.kernelDot)
    def sequenceFigure(grammar: NGrammar, sequenceId: Int, pointer: Int): Fig = {
        val (past, future) = grammar.nsequences(sequenceId).sequence.splitAt(pointer)
        val pastFig = fig.horizontalFig(Spacing.Medium, past map { symbolFigure(grammar, _) })
        val futureFig = fig.horizontalFig(Spacing.Medium, future map { symbolFigure(grammar, _) })
        fig.horizontalFig(Spacing.Small, Seq(pastFig, dot, futureFig))
    }

    def nodeFig(grammar: NGrammar, node: Node): Fig = {
        val Node(Kernel(symbolId, pointer, beginGen, endGen), condition) = node
        node.kernel.symbol match {
            case _: NAtomicSymbol =>
                val symbolFig = symbolFigure(grammar, symbolId)
                fig.verticalFig(Spacing.Medium, Seq(
                    fig.horizontalFig(Spacing.Big, Seq(
                        fig.textFig(s"$symbolId", appear.small),
                        fig.horizontalFig(Spacing.Small, if (pointer == 0) Seq(dot, symbolFig) else Seq(symbolFig, dot)),
                        fig.textFig(s"$beginGen-$endGen", appear.default)
                    )),
                    conditionFig(grammar, condition)
                ))
            case _ =>
                fig.verticalFig(Spacing.Medium, Seq(
                    fig.horizontalFig(Spacing.Big, Seq(
                        fig.textFig(s"$symbolId", appear.small),
                        sequenceFigure(grammar, symbolId, pointer),
                        fig.textFig(s"$beginGen-$endGen", appear.default)
                    )),
                    conditionFig(grammar, condition)
                ))
        }
    }

    def conditionFig(grammar: NGrammar, condition: AcceptCondition): Fig = {
        val d = appear.default
        condition match {
            case Always => fig.textFig("always", d)
            case Never => fig.textFig("never", d)
            case And(conds) =>
                val condsFig = conds.toSeq map { conditionFig(grammar, _) }
                val joinedCondsFig = condsFig.tail.foldLeft(Seq(condsFig.head)) { _ :+ fig.textFig("&", d) :+ _ }
                fig.horizontalFig(Spacing.Small, condsFig)
            case Or(conds) =>
                val condsFig = conds.toSeq map { conditionFig(grammar, _) }
                val joinedCondsFig = condsFig.tail.foldLeft(Seq(condsFig.head)) { _ :+ fig.textFig("|", d) :+ _ }
                fig.horizontalFig(Spacing.Small, condsFig)
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
