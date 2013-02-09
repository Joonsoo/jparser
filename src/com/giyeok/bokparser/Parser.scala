package com.giyeok.bokparser

import scala.collection.immutable.ListMap
import com.giyeok.bokparser.grammars.SampleGrammar1
import com.giyeok.bokparser.visualize.VisualizedStackSymbol

object Parser {
	def main(args: Array[String]) {
		val parser = new BlackboxParser(SampleGrammar1).parse("aaa")
	}
}

class ParseResult(val messages: List[ParsePossibility]) {
	def add(p: ParsePossibility) = {
		p match {
			case ParseSuccess(parsed) =>
				println("Successfully parsed")
				println(parsed)
				println(parsed.source)
				println(parsed.text)
				println(new VisualizedStackSymbol(parsed).repr)
			case ParseFailed(reason, location) =>
				println("Parsing failed since")
				println(s"  $reason at $location")
		}
		new ParseResult(p :: messages)
	}

	val ambiguous = messages.length > 1
	val succeed = messages.length == 1 && ((messages.head) match { case ParseSuccess(_) => true case _ => false })
}
sealed abstract class ParsePossibility
case class ParseFailed(reason: String, location: Int) extends ParsePossibility
case class ParseSuccess(parsed: StackSymbol) extends ParsePossibility

class BlackboxParser(val grammar: Grammar) {
	// === parser ===
	def parse(input: ParserInput): ParseResult = {
		val parser = new Parser(grammar, input)
		while (parser.parseStep()) ()
		parser.result
	}
	def parse(input: String): ParseResult = parse(ParserInput.fromString(input))
}
class Parser(val grammar: Grammar, val input: ParserInput, _stack: (Parser) => OctopusStack = (x: Parser) => new OctopusStack(x)) {
	val stack = _stack(this)

	// === parser ===
	private var _result: ParseResult = new ParseResult(Nil)
	def result = _result

	private var _finished = false
	def finished = _finished

	def parseStep() =
		if (stack hasNext) {
			val entry = stack.pop()
			val pointer = entry.pointer
			val fin = entry finished
			val term = TermSymbol(input at entry.pointer, pointer)
			val newentry = entry proceed (term, pointer + 1, entry, null)

			def pushFinished(f: List[Parser#StackEntry#StackEntryItem]): Unit =
				f match {
					case x :: xs =>
						if ((x.item proceed term) isEmpty) {
							if (x.generationPoint == null) {
								// Parsing finished
								if ((input at pointer) == EOFSymbol) {
									_result = _result add ParseSuccess(x.belonged.symbol) // Successfully parsed
								} else {
									_result = _result add ParseFailed("type 1", pointer) // Error while parsing
								}
							} else {
								stack add (x.generationPoint proceed (NontermSymbol(x.item), pointer, x.belonged, x))
							}
						}
						pushFinished(xs)
					case Nil =>
				}
			pushFinished(fin)

			if (!(newentry isEmpty)) {
				stack add newentry
			} else if (fin isEmpty) {
				// and if the entry has no child
				println(s"${entry.id} is vaporized")
			}
			true
		} else {
			_finished = true
			false
		}

	// === nullable checking ===
	object Nullable {
		// nonterminal to nullable boolean map
		private val map = new collection.mutable.HashMap[String, Boolean]()
	}
	implicit class Nullable(item: DefItem) {
		def isNullable(item: DefItem): Boolean = {
			def OR(rules: List[DefItem]): Boolean = rules match {
				case Nil => false
				case x :: xs => if (isNullable(x)) true else OR(xs)
			}
			item match {
				case Nonterminal(name, _, _) => (Nullable.map get name) match {
					case Some(v) => v
					case None => {
						Nullable.map += name -> false
						val rhs = (grammar.rules get name)
						if (rhs isEmpty) throw new Exception("Unknown nonterminal: " + name)
						val temp = OR(rhs.head)
						Nullable.map(name) = temp; temp
					}
				}
				case StringInput(string, _, _) => string.length == 0
				case _: CharacterInput | _: VirtualInput => false
				case Sequence(seq, whitespace, _, _) => {
					def AND(items: List[DefItem]): Boolean = items match {
						case Nil => true
						case x :: xs => if (isNullable(x)) AND(xs) else false
					}
					AND(seq.toList)
				}
				case OneOf(items, _, _) => OR(items toList)
				case Except(item, except, _, _) => isNullable(item)
				case LookaheadExcept(except, _, _) => false
				case Repeat(item, range, _, _) => range match {
					case RepeatRangeFrom(from) => if (from == 0) true else isNullable(item)
					case RepeatRangeTo(from, to) => if (from == 0) true else isNullable(item)
				}
			}
		}

		val nullable = isNullable(item)
	}

	// === Stack Entry ============================================================================
	object StackEntry {
		private var unique: Int = 0
		private def nextId = { unique += 1; unique }
	}
	class StackEntry(
			val parent: StackEntry,
			val symbol: StackSymbol,
			_items: (StackEntry) => List[Parser.this.StackEntry#StackEntryItem],
			val pointer: Int,
			val generatedFrom: Parser#StackEntry,
			val generatedFromItem: Parser#StackEntry#StackEntryItem) {
		def this() = this(null, StartSymbol, (x: StackEntry) => List(new x.StackEntryItem(x.defItemToState(Nonterminal(grammar.startSymbol)), null)), 0, null, null)
		def finished: List[StackEntry#StackEntryItem] = all filter (_ finishable)
		val items = _items(this)
		val id = StackEntry.nextId

		def proceed(n: StackSymbol, p: Int, from: Parser#StackEntry, fromItem: Parser#StackEntry#StackEntryItem) = {
			val f = (x: StackEntry) => {
				var k = List[StackEntry#StackEntryItem]()

				// map with filtering
				// all map (_ proceed n) filter (_ isDefined) map (_.get)
				for (i <- all) {
					(i proceed (n, x)) match {
						case Some(v) =>
							k = k ::: List(v)
						case None =>
					}
				}
				k
			}
			new StackEntry(this, n, f, p, from, fromItem)
		}
		val isEmpty = items isEmpty

		private def _adjacent(items: List[StackEntry#StackEntryItem]): List[StackEntry#StackEntryItem] = {
			def process(left: List[StackEntry#ParsingItem], set: List[StackEntry#ParsingItem], result: List[StackEntry#ParsingItem]): List[StackEntry#ParsingItem] = {
				def filterNew[T](r: List[T]) = (r.distinct filter (!set.contains(_)))
				left match {
					case x :: xs =>
						val q = filterNew(x.adjacent)
						process(xs ::: q, set ::: q, result ::: q)
					case Nil => result
				}
			}
			val _items = items map (_.item)
			(process(_items, _items, List())) map (new StackEntryItem(_, this))
		}
		val adjacent = _adjacent(items)
		val all = items ::: adjacent

		// ==== StackEntryItem ====================================================================
		object StackEntryItem {
			private var unique: Int = 0
			private def nextId = { unique += 1; unique }
		}
		class StackEntryItem(val item: StackEntry#ParsingItem, val generationPoint: StackEntry) {
			//		def this(item: StateDefItem)(implicit belonged: StackEntry) = this(item, null)
			//		def this(item: DefItem, generationPoint: StackEntry)(implicit belonged: StackEntry) = this(defItemToState(item), generationPoint)
			//		def this(item: DefItem)(implicit belonged: StackEntry) = this(item, null)
			//		def cloned = new StackEntryItem(item, generationPoint)

			val id = StackEntryItem.nextId
			val belonged = StackEntry.this

			override def equals(other: Any) = other match {
				case that: StackEntryItem => (that canEqual this) && that.item == item && that.generationPoint == generationPoint
				case _ => false
			}
			def canEqual(other: Any) = other.isInstanceOf[StackEntryItem]

			def finishable: Boolean = item finishable

			def proceed(next: StackSymbol, belonged: StackEntry): Option[StackEntry#StackEntryItem] = (item proceed next) match {
				case Some(v) => Some(new belonged.StackEntryItem(v, generationPoint))
				case None => None
			}
		}

		// ==== ParsingItems ======================================================================
		def defItemToState(i: DefItem) = i match {
			case j: CharacterInput => ParsingCharacterInput(j)
			case j: StringInput => ParsingStringInput(j)
			case j: VirtualInput => ParsingVirtualInput(j)
			case j: Nonterminal => ParsingNonterminal(j)
			case j: OneOf => ParsingOneOf(j)
			case j: Repeat => ParsingRepeat(j)
			case j: Sequence => new ParsingSequence(j)
			case j: Except => ParsingExcept(j)
			case j: LookaheadExcept => ParsingLookaheadExcept(j)
		}
		abstract class ParsingItem(val item: DefItem, val precedingWS: List[StackSymbol], val followingWS: List[StackSymbol]) {
			def finishable: Boolean
			def proceed(next: StackSymbol): Option[ParsingItem]
			def adjacent: List[ParsingItem]
			
			val enclosingEntry = StackEntry.this

			val children: List[StackSymbol]

			override def equals(other: Any) = other match {
				case that: ParsingItem => (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item)
				case _ => false
			}
			def canEqual(other: Any) = other.isInstanceOf[ParsingItem]
		}
		abstract class ParsingInput(override val item: Input, pWS: List[StackSymbol], fWS: List[StackSymbol]) extends ParsingItem(item, pWS, fWS) {
			// Input items have no need to define adjacent items
			def adjacent: List[ParsingItem] = List()
		}
		case class ParsingCharacterInput(override val item: CharacterInput, char: StackSymbol = null,
				pWS: List[StackSymbol] = Nil, fWS: List[StackSymbol] = Nil) extends ParsingInput(item, pWS, fWS) {
			val done = (char != null)
			def finishable = done
			def proceed(next: StackSymbol) = next match {
				case TermSymbol(CharInputSymbol(c), _) if (!done && (item acceptable c)) => Some(new ParsingCharacterInput(item, next))
				case _ => None
			}
			val children = if (!done) List() else List(char)

			override def equals(other: Any) = other match {
				case that: ParsingCharacterInput => 
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingCharacterInput]
		}
		case class ParsingStringInput(override val item: StringInput, str: List[StackSymbol] = Nil,
				pWS: List[StackSymbol] = Nil, fWS: List[StackSymbol] = Nil) extends ParsingInput(item, pWS, fWS) {
			val pointer = str.length
			def finishable = pointer >= item.string.length()
			def proceed(next: StackSymbol) = if (finishable) None else next match {
				case TermSymbol(CharInputSymbol(char), _) if (item.string.charAt(pointer) == char) =>
					Some(new ParsingStringInput(item, str ::: List(next)))
				case _ => None
			}
			val children = str

			override def equals(other: Any) = other match {
				case that: ParsingStringInput => 
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.pointer == pointer)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingStringInput]
		}
		case class ParsingVirtualInput(override val item: VirtualInput, virt: StackSymbol = null,
				pWS: List[StackSymbol] = Nil, fWS: List[StackSymbol] = Nil) extends ParsingInput(item, pWS, fWS) {
			val done = (virt != null)
			def finishable = done
			def proceed(next: StackSymbol) = next match {
				case TermSymbol(_@ VirtInputSymbol(v), _) if (!done && v == item.name) => Some(new ParsingVirtualInput(item, next))
				case _ => None
			}
			val children = if (!done) List() else List(virt)

			override def equals(other: Any) = other match {
				case that: ParsingVirtualInput => 
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingVirtualInput]
		}
		case class ParsingNonterminal(override val item: Nonterminal, nonterm: StackSymbol = null,
				pWS: List[StackSymbol] = Nil, fWS: List[StackSymbol] = Nil) extends ParsingItem(item, pWS, fWS) {
			val done = (nonterm != null)
			def finishable = done
			def proceed(next: StackSymbol) = next match {
				case NontermSymbol(rhs) if (!done && (grammar.rules(item.name) contains rhs.item)) => Some(new ParsingNonterminal(item, next))
				case _ => None
			}
			def adjacent = if (!done) (grammar.rules(item.name) map (defItemToState _)) else List()
			val children = if (!done) List() else List(nonterm)

			override def equals(other: Any) = other match {
				case that: ParsingNonterminal => 
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingNonterminal]
		}
		case class ParsingOneOf(override val item: OneOf, chosen: StackSymbol = null,
				pWS: List[StackSymbol] = Nil, fWS: List[StackSymbol] = Nil) extends ParsingItem(item, pWS, fWS) {
			val done = (chosen != null)
			def finishable = done
			def proceed(next: StackSymbol) = next match {
				case NontermSymbol(s) if (!done && (item.items contains s.item)) => Some(ParsingOneOf(item, next))
				case _ => None
			}
			def adjacent = if (!done) (item.items map (defItemToState _)) toList else List()
			val children = if (!done) List() else List(chosen)

			override def equals(other: Any) = other match {
				case that: ParsingOneOf => 
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingOneOf]
		}
		case class ParsingRepeat(override val item: Repeat, repeated: List[StackSymbol] = Nil,
				pWS: List[StackSymbol] = Nil, fWS: List[StackSymbol] = Nil) extends ParsingItem(item, pWS, fWS) {
			val count = repeated.length
			def finishable = item.range contains count
			def proceed(next: StackSymbol) = next match {
				case NontermSymbol(s) if (item.item == s.item && (item.range canProceed count)) => Some(ParsingRepeat(item, repeated ::: List(next)))
				case _ => None
			}
			val stateItem = defItemToState(item.item)
			def adjacent = if (item.range canProceed count) List(stateItem) else List()
			val children = repeated

			override def equals(other: Any) = other match {
				case that: ParsingRepeat => 
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.count == count)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingRepeat]
		}
		case class ParsingSequence(override val item: Sequence, _children: List[StackSymbol], nonWS: List[(Int, Int)], pointer: Int,
				pWS: List[StackSymbol], fWS: List[StackSymbol]) extends ParsingItem(item, pWS, fWS) {
			def this(item: Sequence) = this(item, Nil, Nil, 0, Nil, Nil)
			def finishable = pointer >= item.seq.length
			val fixed = (!(fWS isEmpty)) // fixed=true -> finishable must be true
			private def checkLookaheadNot(except: List[DefItem]): Boolean = {
				val g = new CompositeGrammar(except)
				val parser = new Parser(g, input.subinput(StackEntry.this.pointer))
				def rec: Boolean = if (parser parseStep) {
					if (!(parser.stack hasNext)) true else {
						val i = parser.stack.top.items
						// if "parser.stackTop" is like $* (finishing start symbol), it returns false
						if (i.length == 1 && ((i.head.item) match {
							case n @ ParsingNonterminal(Nonterminal(g.startSymbol, _, _), _, _, _) if (n.done) => true
							case _ => false
						})) false
						else rec
					}
				} else true
				rec
			}
			private val proceedables: List[DefItem] = {
				def propagate(l: List[DefItem]): List[DefItem] = l match {
					case (x @ LookaheadExcept(except, _, _)) :: xs => if (checkLookaheadNot(except)) x :: propagate(xs) else List()
					case x :: xs => if (x nullable) x :: propagate(xs) else List(x)
					case Nil => List()
				}
				propagate(item.seq drop pointer)
			}
			def proceed(next: StackSymbol) =
				if (fixed) (next match {
					case NontermSymbol(x) =>
						val input = x.item
						if (item.followingWS contains input) Some(ParsingSequence(item, _children, nonWS, pointer, pWS, fWS ::: List(next)))
						else None
					case _ => None
				})
				else (next match {
					case NontermSymbol(x) =>
						val input = x.item
						val Sequence(seq, ws, _, _) = item
						def checkProceedable(l: List[DefItem], i: Int = 0): Int =
							l match {
								case x :: xs =>
									if (x == input) i else { if (x nullable) checkProceedable(xs, i + 1) else -1 }
								case Nil => -1
							}
						// val k = checkProceedable(seq drop pointer)
						// require(k == (proceedables indexOf input))
						val k = (proceedables indexOf input)
						if (k < 0) {
							if ((pointer == 0) && (_children isEmpty) && (item.precedingWS contains input))
								Some(ParsingSequence(item, _children, nonWS, pointer, pWS ::: List(next), fWS))
							else if (!(_children isEmpty) && (ws contains input))
								Some(ParsingSequence(item, _children ::: List(next), nonWS, pointer, pWS, fWS))
							else if (finishable && (item.followingWS contains input))
								Some(ParsingSequence(item, _children, nonWS, pointer, pWS, fWS ::: List(next)))
							else None
						} else {
							Some(ParsingSequence(item, _children ::: List(next), nonWS ::: List((_children.length, pointer + k)), pointer + k + 1, pWS, fWS))
						}
					case _ => None
				})
			def adjacent = {
				if (fixed) (item.followingWS map (defItemToState _))
				else {
					var adj = (proceedables map (defItemToState _)) ::: (item.whitespace map (defItemToState _))
					if ((pointer == 0) && (_children isEmpty)) adj :::= (item.precedingWS map (defItemToState _))
					adj
				}
			}
			private def pick(indices: List[(Int, Int)], i: Int = 0): List[StackSymbol] = {
				def mult(c: Int): List[StackSymbol] = if (c > 0) (EmptySymbol :: mult(c - 1)) else Nil
				indices match {
					case x :: xs =>
						mult(x._2 - i) ::: List(_children(x._1)) ::: pick(xs, x._2 + 1)
					case Nil => List()
				}
			}
			val children = pick(nonWS) // pWS ::: _children ::: fWS

			override def equals(other: Any) = other match {
				case that: ParsingSequence => 
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.pointer == pointer)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingSequence]
		}
		case class ParsingExcept(override val item: Except, child: StackSymbol = null,
				pWS: List[StackSymbol] = Nil, fWS: List[StackSymbol] = Nil) extends ParsingItem(item, pWS, fWS) {
			private lazy val inputPointer = StackEntry.this.pointer

			val passed = child != null
			def finishable: Boolean = passed
			def proceed(next: StackSymbol): Option[ParsingItem] = next match {
				// check in proceed
				case NontermSymbol(x) if (x.item == item.item) =>
					// check input is not in item.except
					val test = new BlackboxParser(new CompositeGrammar(item.except)).parse(ParserInput.fromList(next.source))
					if (!(test succeed)) Some(ParsingExcept(item, next)) else None
				case _ => None
			}

			val stateItem = defItemToState(item.item)
			def adjacent: List[ParsingItem] = if (!passed) List(stateItem) else List()
			val children = List(child)

			override def equals(other: Any) = other match {
				case that: ParsingExcept => 
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.passed == passed)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingExcept]
		}
		case class ParsingLookaheadExcept(override val item: LookaheadExcept,
				pWS: List[StackSymbol] = Nil, fWS: List[StackSymbol] = Nil) extends ParsingItem(item, pWS, fWS) {
			// "lookahead except" items are processed in sequence, so this is just dummy
			def finishable: Boolean = false
			def proceed(next: StackSymbol): Option[ParsingItem] = None
			def adjacent: List[ParsingItem] = List()
			val children = List()

			override def equals(other: Any) = other match {
				case that: ParsingLookaheadExcept => 
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingLookaheadExcept]
		}
	}

	class CompositeGrammar(starting: List[DefItem]) extends Grammar {
		val name: String = "Except"
		val startSymbol: String = {
			def nextSymbol(x: String): String =
				if ((grammar.rules get x) isDefined) nextSymbol(x + "$")
				else x
			nextSymbol("$")
		}
		val rules: ListMap[String, List[DefItem]] = grammar.rules + ((startSymbol, starting))
	}
}

sealed abstract class StackSymbol {
	val text: String // with whitespaces
	val source: List[InputSymbol] // without whitespaces
}
case object StartSymbol extends StackSymbol {
	val text = ""
	val source = Nil
}
case class NontermSymbol(item: Parser#StackEntry#ParsingItem) extends StackSymbol {
	lazy val text = (item.children map (_ text)) mkString
	lazy val source = {
		def rec(l: List[StackSymbol]): List[InputSymbol] = l match {
			case x :: xs => x.source ::: rec(xs)
			case Nil => List()
		}
		rec(item.children)
	}
}
case class TermSymbol(input: InputSymbol, pointer: Int) extends StackSymbol {
	val text: String = input match { case CharInputSymbol(c) => String valueOf c case _ => "" }
	val source = List(input)
}
case object EmptySymbol extends StackSymbol {
	val text = ""
	val source = Nil
}
