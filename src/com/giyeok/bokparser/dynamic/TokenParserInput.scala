package com.giyeok.bokparser.dynamic

import com.giyeok.bokparser.Grammar
import com.giyeok.bokparser.ParserInput
import com.giyeok.bokparser.InputSymbol
import com.giyeok.bokparser.EOFSymbol

class TokenParserInput(val tokens: List[InputSymbol]) extends ParserInput {
	val length = tokens.length
	def at(p: Int) = if (p < length) tokens(p) else EOFSymbol
	
	def subinput(p: Int) = new TokenParserInput(tokens drop p)
}

object TokenParserInput {
	def fromGrammar(grammar: Grammar, token: String, raw: String, input: ParserInput) = {
		val tokenizer = new Tokenizer(grammar, token, raw, input)
		var tokens = List[InputSymbol]()
		
		while (tokenizer.hasNextToken) {
			tokens ++= tokenizer.nextToken()
		}
		new TokenParserInput(tokens)
	}
}
