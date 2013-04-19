package com.giyeok.bokparser

import com.giyeok.bokparser.dynamic.Parser

sealed abstract class StackSymbol {
	val text: String
	val source: List[InputSymbol]
	
	val processed: Object
}
case object StartSymbol extends StackSymbol {
	val text = ""
	val source = Nil
	
	val processed = this
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
	
	lazy val processed = {
		val objs = item.children map (_.processed)
		item.item.action(this, objs)
	}
}
case class TermSymbol(input: InputSymbol, pointer: Int) extends StackSymbol {
	val text: String = input match { 
		case CharInputSymbol(c) => String valueOf c
		case TokenInputSymbol(token) => token text
		case _ => "" 
	}
	val source = List(input)
	
	val processed = this
}
case object EmptySymbol extends StackSymbol {
	val text = ""
	val source = Nil

	val processed = this
}
