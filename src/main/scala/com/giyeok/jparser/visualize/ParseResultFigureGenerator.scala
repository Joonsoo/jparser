package com.giyeok.jparser.visualize

import com.giyeok.jparser.ParseResultTree
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.visualize.FigureGenerator.Appearance
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.visualize.FigureGenerator.Spacing
import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.ParseResultDerivationsSet
import com.giyeok.jparser.ParseResultDerivations
import com.giyeok.jparser.ParseResultGraph
import com.giyeok.jparser.ParseResult
import com.giyeok.jparser.ParseResultDerivationsSet

object ParseResultFigureGenerator {
    case class RenderingConfiguration(renderJoin: Boolean, renderWS: Boolean, renderLookaheadExcept: Boolean)
    val cleanestConfiguration = RenderingConfiguration(true, true, true)
}

class ParseResultFigureGenerator[Fig](figureGenerator: FigureGenerator.Generator[Fig], appearances: FigureGenerator.Appearances[Fig]) {
    val (g, ap) = (figureGenerator, appearances)
    val symbolFigureGenerator = new SymbolFigureGenerator(figureGenerator, appearances)

    // Parse Forest
    def parseResultFigure(r: ParseForest): Fig =
        parseResultFigure(r: ParseForest, ParseResultFigureGenerator.cleanestConfiguration)

    def parseResultFigure(r: ParseForest, renderConf: ParseResultFigureGenerator.RenderingConfiguration): Fig = {
        parseForestResultFigure(ap.hSymbolBorder, g.verticalFig _, g.horizontalFig _, renderConf)(r)
    }

    def parseResultVerticalFigure(r: ParseForest): Fig =
        parseResultVerticalFigure(r: ParseForest, ParseResultFigureGenerator.cleanestConfiguration)

    def parseResultVerticalFigure(r: ParseForest, renderConf: ParseResultFigureGenerator.RenderingConfiguration): Fig = {
        parseForestResultFigure(ap.vSymbolBorder, g.horizontalFig _, g.verticalFig _, renderConf)(r)
    }

    def parseForestResultFigure(symbolBorder: FigureGenerator.Appearance[Fig], vfig: (Spacing.Value, Seq[Fig]) => Fig, hfig: (Spacing.Value, Seq[Fig]) => Fig, renderConf: ParseResultFigureGenerator.RenderingConfiguration)(r: ParseForest): Fig = {
        import ParseResultTree._
        def parseNodeFig(n: Node): Fig = n match {
            case TerminalNode(input) =>
                g.textFig(input.toShortString, ap.input)
            case TermFuncNode =>
                g.textFig("λt", ap.input)
            case BindNode(sym: Repeat, body) =>
                def childrenOf(node: Node, sym: Symbol): Seq[Node] = node match {
                    case BindNode(s, body) if s == sym => Seq(node)
                    case BindNode(s, body) => childrenOf(body, sym)
                    case s: SequenceNode => s.children flatMap { childrenOf(_, sym) }
                }
                val children = childrenOf(body, sym.sym)
                vfig(Spacing.Small, Seq(
                    symbolBorder.applyToFigure(hfig(Spacing.Small,
                        if (children.isEmpty) Seq(g.textFig("ε", ap.default))
                        else (children map { parseNodeFig _ }))),
                    symbolFigureGenerator.symbolFig(sym)))
            case BindNode(sym, body) =>
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
                        case BindNode(_: Symbols.LookaheadExcept, _) => true
                        case BindNode(_: Symbols.LookaheadIs, _) => true
                        case _ => false
                    }
                    val seq: Seq[Fig] =
                        s.children.zipWithIndex flatMap { c =>
                            val (childNode, idx) = c
                            if (s.symbol.contentIdx contains idx) {
                                // if it is content child
                                if (isLookaheadNode(childNode)) {
                                    if (renderConf.renderLookaheadExcept) {
                                        Some(ap.wsBorder.applyToFigure(parseNodeFig(childNode)))
                                    } else {
                                        None
                                    }
                                } else {
                                    Some(ap.symbolBorder.applyToFigure(parseNodeFig(childNode)))
                                }
                            } else {
                                // if it is whitespace child
                                if (renderConf.renderWS) {
                                    Some(ap.wsBorder.applyToFigure(parseNodeFig(childNode)))
                                } else {
                                    None
                                }
                            }
                        }
                    hfig(Spacing.Medium, seq)
                }
        }
        vfig(Spacing.Big, r.trees.toSeq map { parseNodeFig _ })
    }

    // Parse Derivations Set

    def parseResultFigure(r: ParseResultDerivationsSet): Fig =
        parseResultFigure(r: ParseResultDerivationsSet, ParseResultFigureGenerator.cleanestConfiguration)

    def parseResultFigure(r: ParseResultDerivationsSet, renderConf: ParseResultFigureGenerator.RenderingConfiguration): Fig = {
        parseResultDerivationsSetFigure(ap.hSymbolBorder, g.verticalFig _, g.horizontalFig _, renderConf)(r)
    }

    def parseResultVerticalFigure(r: ParseResultDerivationsSet): Fig =
        parseResultVerticalFigure(r: ParseResultDerivationsSet, ParseResultFigureGenerator.cleanestConfiguration)

    def parseResultVerticalFigure(r: ParseResultDerivationsSet, renderConf: ParseResultFigureGenerator.RenderingConfiguration): Fig = {
        parseResultDerivationsSetFigure(ap.vSymbolBorder, g.horizontalFig _, g.verticalFig _, renderConf)(r)
    }

    def parseResultDerivationsSetFigure(symbolBorder: FigureGenerator.Appearance[Fig], vfig: (Spacing.Value, Seq[Fig]) => Fig, hfig: (Spacing.Value, Seq[Fig]) => Fig, renderConf: ParseResultFigureGenerator.RenderingConfiguration)(r: ParseResultDerivationsSet): Fig = {
        import ParseResultDerivations._
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

    // Parse Graph
    def parseResultFigure(r: ParseResultGraph): Fig =
        parseResultFigure(r: ParseResultGraph, ParseResultFigureGenerator.cleanestConfiguration)

    def parseResultFigure(r: ParseResultGraph, renderConf: ParseResultFigureGenerator.RenderingConfiguration): Fig = {
        parseResultFigure(ap.hSymbolBorder, g.verticalFig _, g.horizontalFig _, renderConf)(r)
    }

    def parseResultVerticalFigure(r: ParseResultGraph): Fig =
        parseResultVerticalFigure(r: ParseResultGraph, ParseResultFigureGenerator.cleanestConfiguration)

    def parseResultVerticalFigure(r: ParseResultGraph, renderConf: ParseResultFigureGenerator.RenderingConfiguration): Fig = {
        parseResultFigure(ap.vSymbolBorder, g.horizontalFig _, g.verticalFig _, renderConf)(r)
    }

    def parseResultFigure(symbolBorder: FigureGenerator.Appearance[Fig], vfig: (Spacing.Value, Seq[Fig]) => Fig, hfig: (Spacing.Value, Seq[Fig]) => Fig, renderConf: ParseResultFigureGenerator.RenderingConfiguration)(r: ParseResultGraph): Fig = {
        import ParseResultGraph._
        def figureOf(node: Node, visited: Set[Node]): Fig =
            node match {
                case node: TermFunc =>
                    g.textFig(s"${node.range}λt", ap.input) ensuring (r.outgoingOf(node).isEmpty)
                case node @ Term(_, input) =>
                    g.textFig(s"T${node.range}${input.toShortString}", ap.input) ensuring (r.outgoingOf(node).isEmpty)
                case node @ Sequence(position, length, symbol, pointer) =>
                    assert(r.outgoingOf(node) forall { _.isInstanceOf[AppendEdge] })
                    val outgoingEdges = r.outgoingOf(node) map { _.asInstanceOf[AppendEdge] }
                    val prevSeqs = outgoingEdges filter { _.isBase }
                    vfig(Spacing.Small,
                        Seq(g.textFig(s"${node.range}", ap.default)))
                case node @ Bind(_, _, symbol) =>
                    val children = if (visited contains node) {
                        Seq(g.textFig("\u2672", ap.default))
                    } else {
                        (r.outgoingOf(node).toSeq map {
                            case BindEdge(_, end) => symbolBorder.applyToFigure(figureOf(end, visited + node))
                            case _ => ??? // BindEdge 외에 다른 엣지가 오면 안됨
                        })
                    }
                    vfig(Spacing.Small,
                        children :+ hfig(
                            Spacing.None,
                            Seq(g.textFig(s"${node.range}", ap.default), symbolFigureGenerator.symbolFig(symbol))))
                case Join(position, length, symbol) =>
                    vfig(Spacing.Small,
                        (r.outgoingOf(node).toSeq map {
                            case JoinEdge(_, end, join) =>
                                hfig(Spacing.None, Seq(figureOf(end, visited + node), g.textFig("&", ap.default), figureOf(join, visited + node)))
                            case _ => ??? // JoinEdge 외에 다른 엣지가 오면 안됨
                        }) :+ symbolFigureGenerator.symbolFig(symbol))
            }
        // figureOf(r.root, Set())
        parseForestResultFigure(symbolBorder, vfig, hfig, renderConf)(r.asParseForest._1)
    }
}
