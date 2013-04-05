package com.giyeok.bokparser

import com.giyeok.bokparser.grammars.SampleGrammar1

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
			case ParseFailed(reason, location) =>
				println("Parsing failed since")
				println(s"  $reason at $location")
		}
		new ParseResult(p :: messages)
	}

	val ambiguous = messages.length > 1
	val succeed = messages.length == 1 && ((messages.head) match { case ParseSuccess(_) => true case _ => false })
	val parsed: Option[StackSymbol] = if (succeed) Some(messages.head.asInstanceOf[ParseSuccess].parsed) else None
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
class Parser(val grammar: Grammar, val input: ParserInput) {
	val stack = new OctopusStack //_stack(this)

	// === stack ===
	class OctopusStack {
		val bottom = new StackEntry(null, StartSymbol, 0, null, null) {
			def _items = List(new StackEntryItem(defItemToState(Nonterminal(grammar.startSymbol)), null, Nil))
		}

		import scala.collection.mutable.Queue

		private val tops = Queue[StackEntry](bottom)

		def add(entry: StackEntry) = tops += entry
		def addAll(entries: Seq[StackEntry]) = for (entry <- entries) add(entry)
		def hasNext = !tops.isEmpty
		def top = tops.front
		def pop() = tops.dequeue()
		def iterator = tops.iterator
	}

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

			def pushFinished(f: List[entry.StackEntryItem]): Unit =
				f match {
					case x :: xs =>
						if ((x.item proceed term) isEmpty) {
							if (x.generationPoint == null) {
								// Parsing finished
								if ((input at pointer) == EOFSymbol) {
									_result = _result add ParseSuccess(NontermSymbol(x.item)) // Successfully parsed
								} else {
									_result = _result add ParseFailed("type 1", pointer) // Error while parsing
								}
							} else {
								(x.generationPoint proceed (NontermSymbol(x.item), pointer, x.belonged, x)) match {
									case Some(newentry) => stack add newentry
									case _ =>
								}
							}
						}
						pushFinished(xs)
					case Nil =>
				}
			pushFinished(fin)

			(entry proceed (term, pointer + 1, entry, null)) match {
				case Some(newentry) => stack add newentry
				case _ =>
					if (fin isEmpty) {
						// and if the entry has no child
						println(s"${entry.id} is vaporized")
					}
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
	object StackEntry {
		private var unique: Int = 0
		private def nextId = { unique += 1; unique }
	}
	abstract class StackEntry(
			val parent: StackEntry,
			val symbol: StackSymbol,
			val pointer: Int,
			val generatedFrom: Parser#StackEntry,
			val generatedFromItem: Parser#StackEntry#StackEntryItem) {
		def _items: List[this.StackEntryItem]

		def finished: List[this.StackEntryItem] = items filter (_ finishable)
		val kernels = _items
		val id = StackEntry.nextId

		def proceed(n: StackSymbol, p: Int, from: Parser#StackEntry, fromItem: Parser#StackEntry#StackEntryItem): Option[StackEntry] = {
			// TODO this is temporary, rewrite this
			val newentry = new StackEntry(this, n, p, from, fromItem) {
				def _items = parent.items flatMap ((x) => (x proceed (n, this)) match {
					case p @ Some(_) => p
					case _ => (x proceedWS (n, this))
				})
			}
			if (newentry isEmpty) None else Some(newentry)
		}
		val isEmpty = kernels isEmpty

		// NOTE: Not all entries of 'kernels' may not be included in 'items' (because of derivedFrom)
		val items = {
			import scala.collection.mutable.ListMap
			var _items = kernels
			val derivation = new ListMap[StackEntry#ParsingItem, StackEntry#StackEntryItem]

			kernels foreach ((i) => derivation(i.item) = i)

			def derive(left: List[StackEntry#StackEntryItem]): Unit = {
				left match {
					case x :: xs =>
						var added: List[StackEntry#StackEntryItem] = List()
						(x.item.derived.distinct) foreach ((d) => {
							val newitem = (derivation get d) match {
								case Some(q) =>
									// assert(!q.derivedFrom.contains(x))
									val nq = new StackEntryItem(q.item, this, x :: q.derivedFrom, q.id)
									_items = _items.updated(_items.indexOf(q), nq)
									nq
								case None =>
									val nq = new StackEntryItem(d, this, List(x))
									_items = _items ++ List(nq)
									added :+= nq
									nq
							}
							derivation(d) = newitem
						})
						derive(xs ++ added)
					case _ =>
				}
			}
			derive(kernels)
			_items
		}

		// ==== StackEntryItem ====================================================================
		object StackEntryItem {
			private var unique: Int = 0
			private def nextId = { unique += 1; unique }
		}
		class StackEntryItem(val item: StackEntry#ParsingItem, val generationPoint: StackEntry, val derivedFrom: List[StackEntry#StackEntryItem], _id: Int = StackEntryItem.nextId) {
			// derivedFrom: List[StackEntry.this.StackEntryItem]

			val id = _id
			val belonged = StackEntry.this

			override def equals(other: Any) = other match {
				case that: StackEntryItem => (that canEqual this) && that.item == item && that.generationPoint == generationPoint
				case _ => false
			}
			def canEqual(other: Any) = other.isInstanceOf[StackEntryItem]

			def finishable: Boolean = item finishable

			def proceed(next: StackSymbol, belonged: StackEntry): Option[belonged.StackEntryItem] = (item proceed next) match {
				case Some(v) => Some(new belonged.StackEntryItem(v, generationPoint, Nil))
				case None => None
			}
			def proceedWS(next: StackSymbol, belonged: StackEntry): Option[belonged.StackEntryItem] = (item proceedWS next) match {
				case Some(v) => Some(new belonged.StackEntryItem(v, generationPoint, Nil))
				case None => None
			}

			override def toString = "" + id
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
		abstract class ParsingItem(val item: DefItem) {
			def finishable: Boolean
			val children: List[StackSymbol]

			def derived: List[ParsingItem]

			def proceed(next: StackSymbol): Option[ParsingItem]
			def proceedWS(next: StackSymbol): Option[ParsingItem] = None

			val enclosingEntry = StackEntry.this

			override def equals(other: Any) = other match {
				case that: ParsingItem => (that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item)
				case _ => false
			}
			def canEqual(other: Any) = other.isInstanceOf[ParsingItem]
		}
		abstract class ParsingInput(override val item: Input) extends ParsingItem(item) {
			val done: Boolean
			def derived: List[ParsingItem] = Nil
		}
		case class ParsingCharacterInput(override val item: CharacterInput, char: StackSymbol = null) extends ParsingInput(item) {
			val done = (char != null)
			def finishable = done
			val children = if (!done) List() else List(char)

			def proceed(next: StackSymbol) = next match {
				case TermSymbol(CharInputSymbol(c), _) if (!done && (item acceptable c)) =>
					Some(ParsingCharacterInput(item, next))
				case _ => None
			}

			override def equals(other: Any) = other match {
				case that: ParsingCharacterInput =>
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingCharacterInput]
		}
		case class ParsingStringInput(override val item: StringInput, str: List[StackSymbol] = Nil) extends ParsingInput(item) {
			val pointer = str.length
			val done = pointer >= item.string.length()
			def finishable = done
			val children = str

			def proceed(next: StackSymbol) = if (finishable) None else next match {
				case TermSymbol(CharInputSymbol(char), _) if (item.string.charAt(pointer) == char) =>
					Some(ParsingStringInput(item, str ++ List(next)))
				case _ => None
			}

			override def equals(other: Any) = other match {
				case that: ParsingStringInput =>
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.pointer == pointer)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingStringInput]
		}
		case class ParsingVirtualInput(override val item: VirtualInput, virt: StackSymbol = null) extends ParsingInput(item) {
			val done = (virt != null)
			def finishable = done
			val children = if (!done) List() else List(virt)

			def proceed(next: StackSymbol) = next match {
				case TermSymbol(_@ VirtInputSymbol(v), _) if (!done && v == item.name) =>
					Some(ParsingVirtualInput(item, next))
				case _ => None
			}

			override def equals(other: Any) = other match {
				case that: ParsingVirtualInput =>
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingVirtualInput]
		}
		case class ParsingNonterminal(override val item: Nonterminal, nonterm: StackSymbol = null) extends ParsingItem(item) {
			val done = (nonterm != null)
			def finishable = done
			val children = if (!done) List() else List(nonterm)

			def derived = (if (!done) (grammar.rules(item.name)) else List()) map (defItemToState _)

			def proceed(next: StackSymbol) = next match {
				case NontermSymbol(rhs) if (!done && (grammar.rules(item.name) contains rhs.item)) =>
					Some(new ParsingNonterminal(item, next))
				case _ => None
			}

			override def equals(other: Any) = other match {
				case that: ParsingNonterminal =>
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingNonterminal]
		}
		case class ParsingOneOf(override val item: OneOf, chosen: StackSymbol = null) extends ParsingItem(item) {
			val done = (chosen != null)
			def finishable = done
			val children = if (!done) List() else List(chosen)

			def derived = (if (!done) item.items.toList else List()) map (defItemToState _)

			def proceed(next: StackSymbol) = next match {
				case NontermSymbol(s) if (!done && (item.items contains s.item)) =>
					Some(ParsingOneOf(item, next))
				case _ => None
			}

			override def equals(other: Any) = other match {
				case that: ParsingOneOf =>
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.done == done)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingOneOf]
		}
		case class ParsingRepeat(override val item: Repeat, repeated: List[StackSymbol] = Nil) extends ParsingItem(item) {
			val count = repeated.length
			def finishable = item.range contains count
			val children = repeated

			def derived = {
				var _derived: List[DefItem] = List()
				if (item.range canProceed count) _derived :+= item.item

				_derived map (defItemToState _)
			}

			def proceed(next: StackSymbol) = next match {
				case NontermSymbol(s) if (item.item == s.item && (item.range canProceed count)) =>
					Some(ParsingRepeat(item, repeated ++ List(next)))
				case _ => None
			}

			override def equals(other: Any) = other match {
				case that: ParsingRepeat =>
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.count == count)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingRepeat]
		}
		case class ParsingSequence(override val item: Sequence, _children: List[StackSymbol], nonWS: List[(Int, Int)], pointer: Int) extends ParsingItem(item) {
			// nonWS: index of _children(without whitespace) -> index of children(without whitespace)
			def this(item: Sequence) = this(item, Nil, Nil, 0)
			def finishable = pointer >= item.seq.length

			val children = {
				def pick(indices: List[(Int, Int)], i: Int = 0): List[StackSymbol] = {
					def mult(c: Int): List[StackSymbol] = if (c > 0) (EmptySymbol :: mult(c - 1)) else Nil
					indices match {
						case x :: xs =>
							mult(x._2 - i) ++ List(_children(x._1)) ++ pick(xs, x._2 + 1)
						case Nil => List()
					}
				}
				pick(nonWS)
			}
			val childrenWithWS = _children
			lazy val indexNonWS = nonWS map (_._1)

			def derived = (if (_children.isEmpty) proceedables else (proceedables ++ item.whitespace)) map (defItemToState _)

			private val proceedables: List[DefItem] = {
				def checkLookaheadNot(except: List[DefItem]): Boolean = {
					val g = new CompositeGrammar(except)
					val parser = new Parser(g, input.subinput(StackEntry.this.pointer))
					def rec: Boolean =
						if (parser parseStep) {
							if (!(parser.stack hasNext)) true else {
								val i = parser.stack.top.kernels
								// if "parser.stackTop" is like $* (finishing start symbol), it returns false
								if (i.length == 1 && ((i.head.item) match {
									// case n @ ParsingNonterminal(Nonterminal(g.startSymbol), _) if (n.done) => true
									// the following "weird" case is 
									case n if (n.isInstanceOf[ParsingNonterminal] && {
										val pn = n.asInstanceOf[ParsingNonterminal]; (pn.item match {
											case Nonterminal(g.startSymbol) => true
											case _ => false
										}) && pn.done
									}) => true
									case _ => false
								})) false
								else rec
							}
						} else true
					rec
				}
				def propagate(l: List[DefItem]): List[DefItem] = l match {
					case (x @ LookaheadExcept(except)) :: xs => if (checkLookaheadNot(except)) x :: propagate(xs) else List()
					case x :: xs => if (x nullable) x :: propagate(xs) else List(x)
					case Nil => List()
				}
				propagate(item.seq drop pointer)
			}
			def proceed(next: StackSymbol) =
				next match {
					case NontermSymbol(x) =>
						val k = proceedables indexOf x.item
						if (k < 0) None
						else Some(ParsingSequence(item, _children ++ List(next), nonWS ++ List((_children.length, pointer + k)), pointer + k + 1))
					case _ => None
				}
			override def proceedWS(next: StackSymbol) =
				next match {
					case NontermSymbol(x) =>
						val input = x.item
						val Sequence(_, midWS) = item

						if (!_children.isEmpty && (midWS contains input))
							Some(ParsingSequence(item, _children ++ List(next), nonWS, pointer))
						else None
					case _ => None
				}

			override def equals(other: Any) = other match {
				case that: ParsingSequence =>
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.pointer == pointer)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingSequence]
		}
		case class ParsingExcept(override val item: Except, child: StackSymbol = null) extends ParsingItem(item) {
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
			def derived: List[ParsingItem] = if (!passed) List(defItemToState(item.item)) else List()
			val children = List(child)

			override def equals(other: Any) = other match {
				case that: ParsingExcept =>
					(that canEqual this) && (that.enclosingEntry == enclosingEntry) && (that.item == item) && (that.passed == passed)
				case _ => false
			}
			override def canEqual(other: Any) = other.isInstanceOf[ParsingExcept]
		}
		case class ParsingLookaheadExcept(override val item: LookaheadExcept) extends ParsingItem(item) {
			// "lookahead except" items are processed in sequence, so this is just dummy
			def finishable: Boolean = false
			def proceed(next: StackSymbol): Option[ParsingItem] = None
			def derived: List[ParsingItem] = List()
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
		import scala.collection.immutable.ListMap

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
			case x :: xs => x.source ++ rec(xs)
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
