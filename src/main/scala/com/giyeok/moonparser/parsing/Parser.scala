package com.giyeok.moonparser.parsing

import com.giyeok.moonparser.ParserInputs._
import com.giyeok.moonparser.parsing._
import com.giyeok.moonparser.Grammar

abstract class Parser extends ParsingItems {
    val grammar: Grammar
    val input: ParserInput

    class ParsingContext(location: Int, kernels: Set[EntryData]) {
        object PropagationType extends Enumeration {
            val Subs, Lift = Value
        }
        case class EntryEdge(from: Entry, to: Entry, propType: PropagationType.Value)

        val (entries: Set[Entry], edges: Set[EntryEdge]) = {
            def propagate(newEntries: Set[Entry], entries: Set[Entry], edges: Set[EntryEdge]): (Set[Entry], Set[EntryEdge]) = {
                assert(newEntries subsetOf entries)
                newEntries map { entry =>
                    val subs = entry.subs map { _ inContext this }
                    val newSubsEdges = edges ++ (subs map { EntryEdge(entry, _, PropagationType.Subs) })

                    val fin = entry.finish
                    if (fin.isDefined) {
                        // lift
                    }
                }
                (Set(), Set())
            }
            val kernelEntries = kernels map { _.inContext(this) }
            propagate(kernelEntries, kernelEntries, Set())
        }
    }
}
