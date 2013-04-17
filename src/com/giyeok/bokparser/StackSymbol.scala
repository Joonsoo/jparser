package com.giyeok.bokparser

import com.giyeok.bokparser.dynamic.Parser

sealed abstract class StackSymbol {
	val text: String
	val source: List[InputSymbol]
}
case object StartSymbol extends StackSymbol {
	val text = ""
	val source = Nil
}
case class NontermSymbol(item: Parser#StackEntry#ParsingItem) extends StackSymbol {
	lazy val text = (item.children map (_ text)) mkString
	lazy val source = {
		def rec(l: List[StackSymbol]): List[InputSymbol] = l match {
			case x :: xs => x.source ++ rec(xs)
			case Nil => List()
		}
		rec(item.children)
	}
}
case class TermSymbol(input: InputSymbol, pointer: Int) extends StackSymbol {
	val text: String = input match { case CharInputSymbol(c) => String valueOf c case _ => "" }
	val source = List(input)
}
case object EmptySymbol extends StackSymbol {
	val text = ""
	val source = Nil
}
