package com.giyeok.moonparser.dynamic

import com.giyeok.moonparser.GrammarDefinitionException

trait IsNullable {
    this: Parser =>

    import com.giyeok.moonparser.GrElems._

    object Nullable {
        // nonterminal to nullable boolean map
        private var map = Map[String, Boolean]()
    }
    implicit class Nullable(item: GrElem) {
        def isNullable: Boolean = {
            item match {
                case Nonterminal(name) => (Nullable.map get name) match {
                    case Some(v) => v
                    case None =>
                        Nullable.map += name -> false
                        val rhs = (grammar.rules get name)
                        if (rhs isEmpty) throw GrammarDefinitionException(s"Unknown nonterminal: $name")
                        val temp = rhs.get exists { _.isNullable }
                        Nullable.map += name -> temp; temp
                }
                case StringInput(string) => string.length == 0
                case _: CharacterInput | _: VirtualInput => false
                case Sequence(seq, _) => seq forall { _.isNullable }
                case OneOf(items) => items exists { _.isNullable }
                case Except(item, _) => item.isNullable
                case LookaheadExcept(_) => false
                case Repeat(item, range) => range match {
                    case Repeat.RangeFrom(from) => if (from == 0) true else item.isNullable
                    case Repeat.RangeTo(from, to) => if (from == 0) true else item.isNullable
                }
            }
        }
    }

}
