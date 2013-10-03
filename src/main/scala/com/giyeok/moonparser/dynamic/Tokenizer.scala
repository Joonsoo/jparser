package com.giyeok.moonparser.dynamic

import com.giyeok.moonparser.ParserInput
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Token

class Tokenizer(grammar: Grammar, tokenSymbol: String, rawSymbol: String, input: ParserInput) {
    def hasNextToken = false
    def nextToken: Token = Token(Nil, Set())
    def nextTokenOpt: Option[Token] = None
}
