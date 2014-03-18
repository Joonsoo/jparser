package com.giyeok.moonparser

class ParsingProblem(val grammar: Grammar, input: Inputs.Source)
        extends SymbolProgresses {

    object EdgeKind extends Enumeration {
        val Derive, Lift = Value
    }

    class ParsingContext {

    }

    class Entry

    class Item
}
