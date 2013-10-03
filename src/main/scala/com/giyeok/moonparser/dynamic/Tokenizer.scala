package com.giyeok.moonparser.dynamic

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Nonterminal
import com.giyeok.moonparser.ParserInput
import com.giyeok.moonparser.ParsedSymbol
import com.giyeok.moonparser.StartSymbol
import com.giyeok.moonparser.StringParserInput
import com.giyeok.moonparser.grammars.JavaScriptGrammar
import com.giyeok.moonparser.NontermSymbol
import com.giyeok.moonparser.TermSymbol
import scala.collection.immutable.SortedSet
import com.giyeok.moonparser.GrElem
import com.giyeok.moonparser.Input
import com.giyeok.moonparser.CharInput
import com.giyeok.moonparser.TokenInput
import com.giyeok.moonparser.CharInput
import com.giyeok.moonparser.StringInput
import com.giyeok.moonparser.VirtualInput
import com.giyeok.moonparser.OneOf

class Tokenizer(grammar: Grammar, tokenSymbol: String, rawSymbol: String, input: ParserInput) {
    private val tokenCands = grammar.rules(tokenSymbol)

    private var nextPointer = 0
    private var _nextToken: List[ParsedSymbol] = Nil

    private class TokenizerParser(startingSymbolName: String) extends Parser(grammar, input) {
        override val starter = new StackEntry(null, StartSymbol, nextPointer, null, null) {
            def _items = List(new StackEntryItem(defItemToState(Nonterminal(startingSymbolName)), null, Nil))
        }
        override val stack = new OctopusStack(starter)

        var farthestPointer = -1

        override def reduced(newentry: StackEntry) = {
            if (newentry.parent == starter) {
                if (farthestPointer < newentry.pointer) {
                    farthestPointer = newentry.pointer
                    _nextToken = List(newentry.symbol)
                } else if (farthestPointer == newentry.pointer) {
                    _nextToken = _nextToken :+ newentry.symbol
                }
                super.reduced(newentry)
            } else {
                super.reduced(newentry)
            }
        }

        override def done() = {
            if (farthestPointer >= 0) {
                nextPointer = farthestPointer
            }
        }
    }

    def hasNextToken = nextPointer < input.length
    def nextToken(): List[Input] = {
        _nextToken = Nil
        (new TokenizerParser(tokenSymbol)).parseAll()
        if (_nextToken.isEmpty) {
            (new TokenizerParser(rawSymbol)).parseAll()
            _nextToken.head.source
        } else {
            val q = (_nextToken map (_ match {
                case x: NontermSymbol => Some(x.item.item)
                case _ => None
            })).flatten
            // assert _nextToken(i).source == _nextToken(j).source for all i, j in range
            val compats = Set(q: _*) intersect Set(tokenCands: _*)
            List(TokenInput(new Token(_nextToken.head.source, compats)))
        }
    }
}

class Token(val source: List[Input], val compatItems: Set[GrElem]) {
    lazy val text = source map (_ match {
        case x: CharInput => "" + x.char
        case _ => ""
    }) mkString ""

    def compat(item: GrElem) =
        item match {
            case StringInput(string) =>
                this.source.length == string.length && this.text == string
            case v: VirtualInput =>
                compatItems contains v
            case n: Nonterminal =>
                compatItems contains n
            case o: OneOf =>
                compatItems contains o
            case _ => false
        }
}

object Tokenizer {
    def main(args: Array[String]) {
        val tokenizer = new Tokenizer(JavaScriptGrammar, "_Token", "_Raw", new StringParserInput(
            """a;""".stripMargin('|')))
        while (tokenizer.hasNextToken) {
            val token = tokenizer.nextToken()
            token map (_ match {
                case TokenInput(token) =>
                    println("\"" + token.text + "\"")
                    println(token.compatItems)
                case CharInput(char) => println("'" + char + "'")
                case _ =>
            })
        }
        println("done")
    }
}
