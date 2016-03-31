package com.giyeok.moonparser.visualize

import com.giyeok.moonparser.ParseTree.ParseNode
import com.giyeok.moonparser.Symbols._
import com.giyeok.moonparser.ParseTree.TreePrintableParseNode

class ParseNodeFigureGenerator[Fig](g: FigureGenerator.Generator[Fig], ap: FigureGenerator.Appearances[Fig]) {

    def parseNodeFig(n: ParseNode[Symbol]): Fig = {
        g.textFig(n.toHorizontalHierarchyString, ap.default)
    }
}
