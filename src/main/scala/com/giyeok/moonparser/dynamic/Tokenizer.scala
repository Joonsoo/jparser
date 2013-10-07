package com.giyeok.moonparser.dynamic

import com.giyeok.moonparser.ParserInputs._
import com.giyeok.moonparser.ParsedSymbols._
import com.giyeok.moonparser.Grammar

class Tokenizer(grammar: Grammar, tokenSymbol: String, rawSymbol: String, input: ParserInput) {
    def hasNextToken = ???
    def nextToken: Token = ???
    def nextTokenOpt: Option[Token] = ???
}
