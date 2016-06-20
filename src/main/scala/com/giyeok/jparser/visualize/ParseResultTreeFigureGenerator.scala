package com.giyeok.jparser.visualize

import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.ParseResultTree.TreePrint
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.visualize.FigureGenerator.Appearance
import com.giyeok.jparser.Symbols

object ParseResultTreeFigureGenerator {
    case class RenderingConfiguration(renderJoin: Boolean, renderWS: Boolean, renderLookaheadExcept: Boolean)
    val cleanestConfiguration = RenderingConfiguration(true, true, true)
}

class ParseResultTreeFigureGenerator[Fig](g: FigureGenerator.Generator[Fig], ap: FigureGenerator.Appearances[Fig]) {
    import com.giyeok.jparser.ParseResultTree._
    import com.giyeok.jparser.visualize.FigureGenerator.Spacing

    val symbolFigureGenerator = new SymbolFigureGenerator(g, ap)

    def parseNodeHFig(n: Node): Fig =
        parseNodeHFig(n: Node, ParseResultTreeFigureGenerator.cleanestConfiguration)

    def parseNodeHFig(n: Node, renderConf: ParseResultTreeFigureGenerator.RenderingConfiguration): Fig = {
        parseNodeFig(ap.hSymbolBorder, g.verticalFig _, g.horizontalFig _, renderConf)(n)
    }

    def parseNodeVFig(n: Node): Fig =
        parseNodeVFig(n: Node, ParseResultTreeFigureGenerator.cleanestConfiguration)

    def parseNodeVFig(n: Node, renderConf: ParseResultTreeFigureGenerator.RenderingConfiguration): Fig = {
        parseNodeFig(ap.vSymbolBorder, g.horizontalFig _, g.verticalFig _, renderConf)(n)
    }

    private def parseNodeFig(symbolBorder: FigureGenerator.Appearance[Fig], vfig: (Spacing.Value, Seq[Fig]) => Fig, hfig: (Spacing.Value, Seq[Fig]) => Fig, renderConf: ParseResultTreeFigureGenerator.RenderingConfiguration)(n: Node): Fig = {
        def parseNodeFig(n: Node): Fig = n match {
            case TerminalNode(input) =>
                g.textFig(input.toShortString, ap.input)
            case TermFuncNode =>
                g.textFig("λt", ap.input)
            case BindedNode(sym: Repeat, body) =>
                def childrenOf(node: Node, sym: Symbol): Seq[Node] = node match {
                    case BindedNode(s, body) if s == sym => Seq(node)
                    case BindedNode(s, body) => childrenOf(body, sym)
                    case s: SequenceNode => s.children flatMap { childrenOf(_, sym) }
                }
                val children = childrenOf(body, sym.sym)
                vfig(Spacing.Small, Seq(
                    symbolBorder.applyToFigure(hfig(Spacing.Small,
                        if (children.isEmpty) Seq(g.textFig("ε", ap.default))
                        else (children map { parseNodeFig _ }))),
                    symbolFigureGenerator.symbolFig(sym)))
            case BindedNode(sym, body) =>
                vfig(Spacing.Small, Seq(
                    symbolBorder.applyToFigure(parseNodeFig(body)),
                    symbolFigureGenerator.symbolFig(sym)))
            case JoinNode(body, join) =>
                var content = Seq(symbolBorder.applyToFigure(parseNodeFig(body)))
                if (renderConf.renderJoin) {
                    content :+= ap.joinHighlightBorder.applyToFigure(hfig(Spacing.Small, Seq(g.textFig("&", ap.default), parseNodeFig(join))))
                }
                vfig(Spacing.Small, content)
            case s: SequenceNode =>
                if (s.children.isEmpty) {
                    g.textFig("ε", ap.default)
                } else {
                    def isLookaheadNode(node: Node): Boolean = node match {
                        case BindedNode(_: Symbols.LookaheadExcept, _) => true
                        case BindedNode(_: Symbols.LookaheadIs, _) => true
                        case _ => false
                    }
                    val seq: Seq[Fig] =
                        s.childrenWS.zipWithIndex flatMap { c =>
                            val (childNode, idx) = c
                            if (s.wsIdx contains idx) {
                                // if it is whitespace child
                                if (renderConf.renderWS) {
                                    Some(ap.wsBorder.applyToFigure(parseNodeFig(childNode)))
                                } else {
                                    None
                                }
                            } else {
                                if (isLookaheadNode(childNode)) {
                                    if (renderConf.renderLookaheadExcept) {
                                        Some(ap.wsBorder.applyToFigure(parseNodeFig(childNode)))
                                    } else {
                                        None
                                    }
                                } else {
                                    Some(ap.symbolBorder.applyToFigure(parseNodeFig(childNode)))
                                }
                            }
                        }
                    hfig(Spacing.Medium, seq)
                }
        }
        parseNodeFig(n)
    }
}
