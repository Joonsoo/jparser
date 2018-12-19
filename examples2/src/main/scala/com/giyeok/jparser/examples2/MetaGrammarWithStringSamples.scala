package com.giyeok.jparser.examples2

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.examples1.StringSamples
import com.giyeok.jparser.metagrammar.MetaGrammar

abstract class MetaGrammarWithStringSamples(val name: String) extends StringSamples {
    val grammarText: String

    val grammar = MetaGrammar.translate(name, grammarText).left.get

    def toPair: (Grammar, StringSamples) = (grammar, this)
}
