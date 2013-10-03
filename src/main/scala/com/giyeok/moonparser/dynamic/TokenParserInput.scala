package com.giyeok.moonparser.dynamic

import com.giyeok.moonparser.EOF
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Input
import com.giyeok.moonparser.ParserInput

class TokenParserInput(val tokens: List[Input]) extends ParserInput {
    val length = tokens.length
    def at(p: Int) = if (p < length) tokens(p) else EOF

    def subinput(p: Int) = new TokenParserInput(tokens drop p)
}

object TokenParserInput {
    def fromGrammar(grammar: Grammar, token: String, raw: String, input: ParserInput) = {
        val tokenizer = new Tokenizer(grammar, token, raw, input)
        var tokens = List[Input]()

        while (tokenizer.hasNextToken) {
            tokens ++= tokenizer.nextToken()
        }
        new TokenParserInput(tokens)
    }
}
