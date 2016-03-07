package com.giyeok.moonparser

import com.giyeok.moonparser._
import com.giyeok.moonparser.Symbols._

trait IsNullable {
    this: Parser =>

    object Nullable {
        // nonterminal to nullable boolean map
        private var map = Map[String, Boolean]()
    }
    implicit class Nullable(item: Symbol) {
        def isNullable: Boolean = {
            item match {
                case _: Terminal => false
                case Empty => true
                case Nonterminal(name) => (Nullable.map get name) match {
                    case Some(v) => v
                    case None =>
                        Nullable.map += name -> false
                        val rhs = (grammar.rules get name)
                        if (rhs isEmpty) throw GrammarDefinitionException(s"Unknown nonterminal: $name")
                        val temp = rhs.get exists { _.isNullable }
                        Nullable.map += name -> temp; temp
                }
                case Sequence(seq, _) => seq forall { _.isNullable }
                case OneOf(items) => items exists { _.isNullable }
                case Except(item, _) => item.isNullable
                case Repeat(item, range) => range.isNullable || item.isNullable
                case LookaheadExcept(_) => true
                case Backup(elem, backup) =>
                    elem.isNullable // Maybe need: || (backup exists { _.isNullable })
            }
        }
    }
}