package com.giyeok.jparser.visualize

import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.EligCondition._
import com.giyeok.jparser.visualize.FigureGenerator.Spacing

class NodeFigureGenerators[Fig](
        val fig: FigureGenerator.Generator[Fig],
        val appear: FigureGenerator.Appearances[Fig],
        val symbol: SymbolFigureGenerator[Fig]) {

    def symbolFigure(grammar: NGrammar, symbolId: Int): Fig = {
        symbol.symbolFig(grammar.nsymbols(symbolId).symbol)
    }

    def dot = fig.textFig("\u2022", appear.kernelDot)
    def sequenceFigure(grammar: NGrammar, sequenceId: Int, pointer: Int): Fig = {
        val (past, future) = grammar.nsequences(sequenceId).sequence.splitAt(pointer)
        val pastFig = fig.horizontalFig(Spacing.Medium, (past map { symbolFigure(grammar, _) }))
        val futureFig = fig.horizontalFig(Spacing.Medium, (future map { symbolFigure(grammar, _) }))
        fig.horizontalFig(Spacing.Small, Seq(pastFig, dot, futureFig))
    }

    def nodeFig(grammar: NGrammar, node: Node): Fig = node match {
        case SymbolNode(symbolId, beginGen) =>
            fig.horizontalFig(Spacing.Big, Seq(
                symbolFigure(grammar, symbolId),
                fig.textFig(s"$beginGen", appear.default)))
        case SequenceNode(symbolId, pointer, beginGen, endGen) =>
            fig.horizontalFig(Spacing.Big, Seq(
                sequenceFigure(grammar, symbolId, pointer),
                fig.textFig(s"$beginGen-$endGen", appear.default)))
    }

    def conditionFig(grammar: NGrammar, condition: Condition): Fig = {
        val d = appear.default
        condition match {
            case True => fig.textFig("true", d)
            case False => fig.textFig("false", d)
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
                    fig.textFig(s", $activeGen)", d)))
            case After(node, activeGen) =>
                fig.horizontalFig(Spacing.Small, Seq(
                    fig.textFig("After(", d),
                    nodeFig(grammar, node),
                    fig.textFig(s", $activeGen)", d)))
            case Alive(node, activeGen) =>
                fig.horizontalFig(Spacing.Small, Seq(
                    fig.textFig("Alive(", d),
                    nodeFig(grammar, node),
                    fig.textFig(s", $activeGen)", d)))
            case Exclude(node) =>
                fig.horizontalFig(Spacing.Small, Seq(
                    fig.textFig("Exclude(", d),
                    nodeFig(grammar, node),
                    fig.textFig(")", d)))
        }
    }
}
