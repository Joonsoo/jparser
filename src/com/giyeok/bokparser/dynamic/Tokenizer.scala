package com.giyeok.bokparser.dynamic

import com.giyeok.bokparser.Grammar
import com.giyeok.bokparser.Nonterminal
import com.giyeok.bokparser.ParserInput
import com.giyeok.bokparser.StackSymbol
import com.giyeok.bokparser.StartSymbol
import com.giyeok.bokparser.StringParserInput
import com.giyeok.bokparser.grammars.JavaScriptGrammar
import com.giyeok.bokparser.NontermSymbol
import com.giyeok.bokparser.TermSymbol
import scala.collection.immutable.SortedSet
import com.giyeok.bokparser.DefItem
import com.giyeok.bokparser.InputSymbol
import com.giyeok.bokparser.CharInputSymbol
import com.giyeok.bokparser.TokenInputSymbol
import com.giyeok.bokparser.CharInputSymbol
import com.giyeok.bokparser.StringInput
import com.giyeok.bokparser.VirtualInput
import com.giyeok.bokparser.OneOf

class Tokenizer(grammar: Grammar, tokenSymbol: String, rawSymbol: String, input: ParserInput) {
	private val tokenCands = grammar.rules(tokenSymbol)
	
	private var nextPointer = 0
	private var _nextToken: List[StackSymbol] = Nil

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
	def nextToken(): List[InputSymbol] = {
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
			List(TokenInputSymbol(new Token(_nextToken.head.source, compats)))
		}
	}
}

class Token(val source: List[InputSymbol], val compatItems: Set[DefItem]) {
	lazy val text = source map (_ match {
		case x: CharInputSymbol => "" + x.char
		case _ => ""
	}) mkString ""

	def compat(item: DefItem) =
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
				case TokenInputSymbol(token) =>
					println("\"" + token.text + "\"")
					println(token.compatItems)
				case CharInputSymbol(char) => println("'" + char + "'")
				case _ =>
			})
		}
		println("done")
	}
}
