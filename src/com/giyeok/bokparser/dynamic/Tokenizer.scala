package com.giyeok.bokparser.dynamic

import com.giyeok.bokparser.Grammar
import com.giyeok.bokparser.Nonterminal
import com.giyeok.bokparser.ParserInput
import com.giyeok.bokparser.StackSymbol
import com.giyeok.bokparser.StartSymbol
import com.giyeok.bokparser.StringParserInput
import com.giyeok.bokparser.grammars.JavaScriptGrammar

class Tokenizer(grammar: Grammar, tokenSymbol: String, input: ParserInput) {
	private var nextPointer = 0
	private var _nextToken: StackSymbol = null

	private class TokenizerParser extends Parser(grammar, input) {
		override val starter = new StackEntry(null, StartSymbol, nextPointer, null, null) {
			def _items = List(new StackEntryItem(defItemToState(Nonterminal(tokenSymbol)), null, Nil))
		}
		override val stack = new OctopusStack(starter)

		var farthestPointer = -1

		override def reduced(newentry: StackEntry) = {
			if (newentry.parent == starter) {
				if (farthestPointer < newentry.pointer) {
					farthestPointer = newentry.pointer
					_nextToken = newentry.symbol
				}
				stack add newentry
			} else {
				stack add newentry
			}
		}

		override def done() = {
			nextPointer = farthestPointer
		}
	}

	def hasNextToken = nextPointer < input.length
	def nextToken() = {
		_nextToken = null
		(new TokenizerParser).parseAll()
		_nextToken
	}
}

object Tokenizer {
	def main(args: Array[String]) {
		val tokenizer = new Tokenizer(JavaScriptGrammar, "Start", new StringParserInput("vara = 'hello world'; console.log(a);"))
		while (tokenizer.hasNextToken) {
			println(s"'${tokenizer.nextToken().text}'")
		}
	}
}
