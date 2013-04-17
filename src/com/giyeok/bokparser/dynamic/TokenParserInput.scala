package com.giyeok.bokparser.dynamic

import com.giyeok.bokparser.Grammar
import com.giyeok.bokparser.ParserInput
import com.giyeok.bokparser.InputSymbol

class TokenParserInput(val tokens: List[InputSymbol]) extends ParserInput {
	def this(grammar: Grammar, token: String, raw: String, input: ParserInput) = this({
		val tokenizer = new Tokenizer(grammar, token, raw, input)
		var tokens = List[InputSymbol]()
		
		while (tokenizer.hasNextToken) {
			tokens ++= tokenizer.nextToken()
		}
		tokens
	})
	
	val length = tokens.length
	def at(pointer: Int) = tokens(pointer)
	
	def subinput(p: Int) = new TokenParserInput(tokens drop p)
}
