package com.giyeok.jparser.parsergen.utils

import com.giyeok.jparser.Inputs.{CharacterTermGroupDesc, TermGroupDesc, VirtualTermGroupDesc}
import com.giyeok.jparser.NGrammar.NTerminal
import com.giyeok.jparser.Symbols.Terminal
import com.giyeok.jparser.Symbols.Terminals.{CharacterTerminal, VirtualTerminal}
import com.giyeok.jparser.nparser.ParsingContext

object TermGrouper {
  def termGroupsOf(terminals: Set[Terminal]): Set[TermGroupDesc] = {
    val charTerms: Set[CharacterTermGroupDesc] = terminals collect { case x: CharacterTerminal => TermGroupDesc.descOf(x) }
    val virtTerms: Set[VirtualTermGroupDesc] = terminals collect { case x: VirtualTerminal => TermGroupDesc.descOf(x) }

    def sliceTermGroups(termGroups: Set[CharacterTermGroupDesc]): Set[CharacterTermGroupDesc] = {
      val charIntersects: Set[CharacterTermGroupDesc] = termGroups flatMap { term1 =>
        termGroups collect {
          case term2 if term1 != term2 => term1 intersect term2
        } filterNot {
          _.isEmpty
        }
      }
      val essentials = (termGroups map { g =>
        charIntersects.foldLeft(g) {
          _ - _
        }
      }) filterNot {
        _.isEmpty
      }
      val intersections = if (charIntersects.isEmpty) Set() else sliceTermGroups(charIntersects)
      essentials ++ intersections
    }

    val charTermGroups = sliceTermGroups(charTerms)

    val virtIntersects: Set[VirtualTermGroupDesc] = virtTerms flatMap { term1 =>
      virtTerms collect {
        case term2 if term1 != term2 => term1 intersect term2
      } filterNot {
        _.isEmpty
      }
    }
    val virtTermGroups = (virtTerms map { term =>
      virtIntersects.foldLeft(term) {
        _ - _
      }
    }) ++ virtIntersects

    (charTermGroups ++ virtTermGroups) filterNot {
      _.isEmpty
    }
  }

  def termGroupsOf(graph: ParsingContext.Graph): Set[TermGroupDesc] = {
    val terms = graph.nodes.map { node => node.kernel.symbol }
      .collect { case terminal: NTerminal => terminal.symbol }
    termGroupsOf(terms)
  }
}
