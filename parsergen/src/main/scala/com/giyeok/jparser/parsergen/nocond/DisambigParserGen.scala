package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.nparser.NGrammar

class DisambigParserGen(val simpleParser: SimpleParser) {
    def this(grammar: NGrammar) = this(new SimpleParserGen(grammar).generateParser())

    def generateParser(): DisambigParser = ???
}
