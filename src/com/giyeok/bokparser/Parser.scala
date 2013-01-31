package com.giyeok.bokparser

import scala.collection.immutable.ListMap

import com.giyeok.bokparser.grammars.SampleGrammar1

object Parser {
	def main(args: Array[String]) {
		val parser = new Parser(SampleGrammar1, InputStream.fromString("aacdeb")).parse()
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

class Parser(val grammar: Grammar, val input: InputStream, _stack: (Parser) => OctopusStack = (x: Parser) => new OctopusStack(x)) {
	val stack = _stack(this)

	// === parser ===
	def parse(): ParseResult = {
		while (parseStep()) ()
		result
	}

	// === parser ===
	private var _result: ParseResult = new ParseResult(Nil)
	def result = _result

	private var _finished = false
	def finished = _finished

	def parseStep() =
		if (stack hasNext) {
			val entry = stack.pop()
			val newentry = entry proceed (TermSymbol(input.symbol, input.pointer), input.pointer + 1)
			if (newentry isEmpty) {
				val fin = entry.finished

				def printFinished(f: List[Parser#StackEntry#StackEntryItem]): Unit =
					f match {
						case x :: xs =>
							println(x); println(x.generationPoint); printFinished(xs)
						case Nil =>
					}
				printFinished(fin)

				def pushFinished(f: List[Parser#StackEntry#StackEntryItem]): Unit =
					f match {
						case x :: xs =>
							def _children(y: Parser#StackEntry): List[StackSymbol] =
								if (y != x.generationPoint)
									_children(y.parent) ::: List(y.symbol)
								else Nil
							val children = _children(x.belonged)
							if (x.generationPoint == null) {
								// Parsing finished
								if (input.symbol == EOFSymbol) {
									_result = _result add ParseSuccess(x.belonged.symbol) // Successfully parsed
								} else {
									_result = _result add ParseFailed("type 1", input.pointer) // Error while parsing
								}
							} else {
								stack add (x.generationPoint.proceed(new NontermSymbol(x.item.item, children), input.pointer))
							}
							pushFinished(xs)
						case Nil =>
					}
				pushFinished(fin)
			} else {
				stack add newentry
				input.next()
			}
			true
		} else {
			_finished = true
			if (input hasNext) {
				_result = _result add ParseFailed("type 2", input.pointer)
			}
			if (result.messages isEmpty) {
				_result = _result add ParseFailed("Unexpected end of file", input.pointer)
			}
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
				case Nonterminal(name) => (Nullable.map get name) match {
					case Some(v) => v
					case None => {
						Nullable.map += name -> false
						val rhs = (grammar.rules get name)
						if (rhs isEmpty) throw new Exception("Unknown nonterminal: " + name)
						val temp = OR(rhs.head)
						Nullable.map(name) = temp; temp
					}
				}
				case StringInput(string) => string.length == 0
				case _: CharacterInput | _: VirtualInput => false
				case Sequence(seq, whitespace) => {
					def AND(items: List[DefItem]): Boolean = items match {
						case Nil => true
						case x :: xs => if (isNullable(x)) AND(xs) else false
					}
					AND(seq.toList)
				}
				case OneOf(items) => OR(items toList)
				case Except(item, except) => isNullable(item)
				case LookaheadExcept(except) => false
				case Repeat(item, range) => range match {
					case RepeatRangeFrom(from) => if (from == 0) true else isNullable(item)
					case RepeatRangeTo(from, to) => if (from == 0) true else isNullable(item)
				}
			}
		}

		val nullable = isNullable(item)
	}

	// === Stack Entry ============================================================================
	class StackEntry(val parent: StackEntry, val symbol: StackSymbol, _items: (StackEntry) => List[Parser.this.StackEntry#StackEntryItem], val pointer: Int) {
		def this() = this(null, StartSymbol, (x: StackEntry) => List(new x.StackEntryItem(x.defItemToState(Nonterminal(grammar.startSymbol)), null)), 0)
		def finished: List[StackEntry#StackEntryItem] = all filter (_ finishable)
		val items = _items(this)

		def proceed(n: StackSymbol, p: Int) = {
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
			new StackEntry(this, n, f, p)
		}
		val isEmpty = items isEmpty

		private def _adjacent(items: List[StackEntry#StackEntryItem]): List[StackEntry#StackEntryItem] = {
			def process(left: List[StackEntry#StateDefItem], set: List[StackEntry#StateDefItem], result: List[StackEntry#StateDefItem]): List[StackEntry#StateDefItem] = {
				def filterNew[T](r: List[T]) = (r filter (!set.contains(_)))
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
		class StackEntryItem(val item: StackEntry#StateDefItem, val generationPoint: StackEntry) {
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

		// ==== StateDefItems =====================================================================
		def defItemToState(i: DefItem) = i match {
			case j: CharacterInput => StateCharacterInput(j)
			case j: StringInput => StateStringInput(j)
			case j: VirtualInput => StateVirtualInput(j)
			case j: Nonterminal => StateNonterminal(j)
			case j: OneOf => StateOneOf(j)
			case j: Repeat => StateRepeat(j)
			case j: Sequence => StateSequence(j)
			case j: Except => StateExcept(j)
			case j: LookaheadExcept => StateLookaheadExcept(j)
		}
		abstract class StateDefItem(val item: DefItem) {
			def finishable: Boolean
			def proceed(next: StackSymbol): Option[StateDefItem]
			def adjacent: List[StateDefItem]
		}
		abstract class StateInput(override val item: Input) extends StateDefItem(item) {
			// Input items have no need to define adjacent items
			def adjacent: List[StateDefItem] = List()
		}
		case class StateCharacterInput(override val item: CharacterInput, done: Boolean = false) extends StateInput(item) {
			def finishable = done
			def proceed(next: StackSymbol) = next match {
				case TermSymbol(_@ CharInputSymbol(char), _) if (!done && (item acceptable char)) => Some(new StateCharacterInput(item, true))
				case _ => None
			}
		}
		case class StateStringInput(override val item: StringInput, pointer: Int = 0) extends StateInput(item) {
			def finishable = pointer >= item.string.length()
			def proceed(next: StackSymbol) = if (finishable) None else next match {
				case TermSymbol(_@ CharInputSymbol(char), _) if (item.string.charAt(pointer) == char) => Some(new StateStringInput(item, pointer + 1))
				case _ => None
			}
		}
		case class StateVirtualInput(override val item: VirtualInput, done: Boolean = false) extends StateInput(item) {
			def finishable = done
			def proceed(next: StackSymbol) = next match {
				case TermSymbol(_@ VirtInputSymbol(virt), _) if (!done && virt == item.name) => Some(new StateVirtualInput(item, true))
				case _ => None
			}
		}
		case class StateNonterminal(override val item: Nonterminal, done: Boolean = false) extends StateDefItem(item) {
			def finishable = done
			def proceed(next: StackSymbol) = next match {
				case NontermSymbol(rhs, _) if (!done && (grammar.rules(item.name) contains rhs)) => Some(new StateNonterminal(item, true))
				case _ => None
			}
			def adjacent = if (!done) (grammar.rules(item.name) map (defItemToState _)) else List()
		}
		case class StateOneOf(override val item: OneOf, done: Boolean = false) extends StateDefItem(item) {
			def finishable = done
			def proceed(next: StackSymbol) = next match {
				case NontermSymbol(s, _) if (!done && (item.items contains s)) => Some(StateOneOf(item, true))
				case _ => None
			}
			def adjacent = if (!done) (item.items map (defItemToState _)) toList else List()
		}
		case class StateRepeat(override val item: Repeat, count: Int = 0) extends StateDefItem(item) {
			def finishable = item.range contains count
			def proceed(next: StackSymbol) = next match {
				case NontermSymbol(s, _) if (item.item == s && (item.range canProceed count)) => Some(StateRepeat(item, count + 1))
				case _ => None
			}
			val stateItem = defItemToState(item.item)
			def adjacent = if (item.range canProceed count) List(stateItem) else List()
		}
		case class StateSequence(override val item: Sequence, pointer: Int = 0) extends StateDefItem(item) {
			def finishable = pointer >= item.seq.length
			private def checkLookaheadNot(except: List[DefItem]): Boolean = {
				val g = new CompositeGrammar(except)
				val parser = new Parser(g, input.subStreamFrom(input.pointer))
				def rec: Boolean = if (parser parseStep) {
					if (!(parser.stack hasNext)) true else {
						val i = parser.stack.top.items
						// if "parser.stackTop" is like $* (finishing start symbol), it returns false
						if (i.length == 1 && ((i.head.item) match {
							case StateNonterminal(item @ Nonterminal(g.startSymbol), true) => true
							case _ => false
						})) false
						else rec
					}
				} else true
				rec
			}
			private val proceedables: List[DefItem] = {
				def propagate(l: List[DefItem]): List[DefItem] = l match {
					case (x @ LookaheadExcept(except)) :: xs => if (checkLookaheadNot(except)) x :: propagate(xs) else List()
					case x :: xs => if (x nullable) x :: propagate(xs) else List(x)
					case Nil => List()
				}
				propagate(item.seq drop pointer)
			}
			def proceed(next: StackSymbol) = next match {
				case NontermSymbol(input, _) =>
					val Sequence(seq, ws) = item
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
						if (ws contains input) Some(new StateSequence(item, pointer)) // COMMENT Some(this) is OK?
						else None
					} else {
						Some(new StateSequence(item, pointer + k + 1))
					}
				case _ => None
			}
			def adjacent = {
				(proceedables map (defItemToState _)) ::: (item.whitespace map (defItemToState _))
			}
		}
		case class StateExcept(override val item: Except, passed: Boolean = false) extends StateDefItem(item) {
			private lazy val inputPointer = StackEntry.this.pointer

			def finishable: Boolean = passed
			def proceed(next: StackSymbol): Option[StateDefItem] = next match {
				// check in proceed
				case NontermSymbol(input, _) if (input == item.item) =>
					// check input is not in item.except
					val tester = new Parser(new CompositeGrammar(item.except), InputStream.fromList(next.source))
					val test = tester.parse()
					// println(test)
					// println("Except: ")
					// println(next.source)
					// println(item.except)
					if (!(test succeed)) Some(StateExcept(item, true)) else None
				case _ => None
			}

			val stateItem = defItemToState(item.item)
			def adjacent: List[StateDefItem] = if (!passed) List(stateItem) else List()
		}
		case class StateLookaheadExcept(override val item: LookaheadExcept) extends StateDefItem(item) {
			// "lookahead except" items are processed in sequence, so this is just dummy
			def finishable: Boolean = false
			def proceed(next: StackSymbol): Option[StateDefItem] = None
			def adjacent: List[StateDefItem] = List()
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
	val text: String
	val source: List[InputSymbol]
}
case object StartSymbol extends StackSymbol {
	val text = ""
	val source = Nil
}
case class NontermSymbol(item: DefItem, containing: List[StackSymbol]) extends StackSymbol {
	lazy val text = (containing map (_ text)) mkString
	lazy val source = {
		def rec(l: List[StackSymbol]): List[InputSymbol] = l match {
			case x :: xs => x.source ::: rec(xs)
			case Nil => List()
		}
		rec(containing)
	}
}
case class TermSymbol(input: InputSymbol, pointer: Int) extends StackSymbol {
	val text: String = input match { case CharInputSymbol(c) => String valueOf c case _ => "" }
	val source = List(input)
}
