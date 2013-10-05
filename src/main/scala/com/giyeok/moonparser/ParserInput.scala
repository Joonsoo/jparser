package com.giyeok.moonparser

import com.giyeok.moonparser.dynamic.Tokenizer
import com.giyeok.moonparser.ParsedSymbols._

object ParserInputs {
    import com.giyeok.moonparser.InputPieces._

    abstract class ParserInput {
        val length: Int
        def at(pointer: Int): Input

        def subinput(p: Int): ParserInput
    }
    class StringParserInput(val string: String) extends ParserInput {
        val length = string length
        def at(p: Int) = if (p < length) CharInput(string charAt p) else EndOfFile

        def subinput(p: Int) = new StringParserInput(string substring p)
    }
    class SeqParserInput(val list: Seq[Input]) extends ParserInput {
        val length = list length
        def at(p: Int) = if (p < length) list(p) else EndOfFile

        def subinput(p: Int) = new SeqParserInput(list drop p)
    }
    object ParserInput {
        def fromString(string: String) = new StringParserInput(string)
        def fromSeq(list: Seq[Input]) = new SeqParserInput(list)
    }
    object TokenParserInput {
        def fromGrammar(grammar: Grammar, token: String, raw: String, input: ParserInput) = {
            val tokenizer = new Tokenizer(grammar, token, raw, input)
            var tokens = List[Token]()

            while (tokenizer.hasNextToken) {
                tokens +:= tokenizer.nextToken
            }
            new SeqParserInput(tokens.reverse map (TokenInput(_)))
        }
    }
}

abstract sealed class Input
object InputPieces {
    case class CharInput(val char: Char) extends Input
    case class VirtInput(val name: String) extends Input
    case class TokenInput(val token: Token) extends Input
    case object EndOfFile extends Input
}
