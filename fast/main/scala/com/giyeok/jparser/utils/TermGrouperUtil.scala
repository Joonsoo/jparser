package com.giyeok.jparser.utils

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.NTerminal
import com.giyeok.jparser.nparser.ParsingContext

object TermGrouperUtil {
  def termGroupsOf(grammar: NGrammar, graph: ParsingContext.Graph): List[TermGroupDesc] = {
    val terms = graph.nodes.map { node => grammar.symbolOf(node.kernel.symbolId) }
      .collect { case terminal: NTerminal => terminal.symbol }
    TermGrouper.termGroupsOf(terms)
  }
}
