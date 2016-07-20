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
}

trait GraphGenerator[Fig] {
    def addNode(node: Node, nodeFigFunc: () => Fig): Unit
    def addEdge(edge: Edge): Unit
}

class ParsingContextGraphGenerator[Fig](fig: NodeFigureGenerators[Fig]) {
    def nodeFig(grammar: NGrammar, node: Node): Fig = node match {
        case SymbolNode(symbolId, beginGen) =>
            fig.fig.horizontalFig(Spacing.Big, Seq(
                fig.symbolFigure(grammar, symbolId),
                fig.fig.textFig(s"$beginGen", fig.appear.default)))
        case SequenceNode(symbolId, pointer, beginGen, endGen) =>
            fig.fig.horizontalFig(Spacing.Big, Seq(
                fig.sequenceFigure(grammar, symbolId, pointer),
                fig.fig.textFig(s"$beginGen-$endGen", fig.appear.default)))
    }

    def addContext(graph: GraphGenerator[Fig], grammar: NGrammar, context: Context): Unit = {
        context.graph.nodes foreach { node =>
            graph.addNode(node, () => nodeFig(grammar, node))
        }
        context.graph.edges foreach { edge =>
            graph.addEdge(edge)
        }
    }

    def addContextTransition(graph: GraphGenerator[Fig], grammar: NGrammar, baseContext: Context, changedContext: Context): Unit = {
        ???
    }
}
