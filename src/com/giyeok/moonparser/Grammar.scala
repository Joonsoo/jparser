package com.giyeok.moonparser

abstract class Grammar {
	type RuleMap = Map[String, List[DefItem]]

	val name: String
	val rules: RuleMap
	val startSymbol: String

	def nonterm(name: String) = Nonterminal(name)
	def n(name: String) = nonterm(name)
	def input(string: String) = StringInput(string)
	def i(string: String) = input(string)
	def input_char() = AnyCharacterInput()
	def input_char(chars: Array[Char]) = PoolCharacterInput(chars)
	def input_char(from: Char, to: Char) = CharacterRangeInput(from, to)
	def c() = input_char()
	def c(chars: Array[Char]) = input_char(chars)
	def c(chars: String) = input_char(chars.toCharArray())
	def c(from: Char, to: Char) = input_char(from, to)
	def unicode_categories(categories: String*): UnicodeCategoryCharacterInput = unicode_categories(categories toArray)
	def unicode_categories(categories: Array[Byte]) = UnicodeCategoryCharacterInput(categories)
	def unicode_categories(categories: Array[String]) = UnicodeCategoryCharacterInput(UnicodeUtil.translateCategoryNamesToByte(categories))
	def virtual(name: String) = VirtualInput(name)
	def seq(seq: DefItem*) = sequence(seq: _*)
	def sequence(seq: DefItem*) = Sequence(seq toList, List())
	def sequence(whitespace: List[DefItem], seq: DefItem*) = Sequence(seq toList, whitespace)
	def oneof(items: DefItem*) = OneOf(items toList)
	def oneof(items: List[DefItem]) = OneOf(items toList)
	def lookahead_except(except: DefItem*) = LookaheadExcept(except toList)

	implicit def defItemRepeatable(item: DefItem): Repeatable =
		item match {
			case n: Nonterminal => new Repeatable { val self = n }
			case i: Input => new Repeatable { val self = i }
			case o: OneOf => new Repeatable { val self = o }
			case r: Repeat => new Repeatable { val self = r }
			case _ => throw new Exception("Applied repeat to the items that cannot be")
		}

	implicit def defItemExcludable(item: DefItem): Excludable =
		item match {
			case n: Nonterminal => new Excludable { val self = n }
			case i: Input => new Excludable { val self = i }
			case o: OneOf => new Excludable { val self = o }
			case e: Except => new Excludable { val self = e }
			case r: Repeat => new Excludable { val self = r }
			case _ => throw new Exception("Applied except/butnot to the items that cannot be")
		}
}

trait Excludable {
	val self: DefItem

	def except(except: DefItem*) = Except(self, except toList)
	def butnot(except: DefItem*) = Except(self, except toList)
}

trait Repeatable {
	val self: DefItem

	def repeat(from: Int, to: Int) = Repeat(self, RepeatRangeTo(from, to))
	def repeat(from: Int) = Repeat(self, RepeatRangeFrom(from))

	// optional
	def opt = repeat(0, 1)
	def question = opt

	// more than zero
	def asterisk = repeat(0)
	def star = asterisk

	// more than once
	def plus = repeat(1)
}

sealed abstract class DefItem {
	val id = DefItem.nextId

	override def equals(other: Any) = other match {
		case that: DefItem => (that canEqual this) && (that.id == id)
		case _ => false
	}
	def canEqual(other: Any) = other.isInstanceOf[DefItem]
}
object DefItem {
	private var unique: Int = 0
	private def nextId = { unique += 1; unique }
}

case class Nonterminal(name: String) extends DefItem {
	override def equals(other: Any) = other match {
		case that: Nonterminal => (that canEqual this) && (that.name == name)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[Nonterminal]
}

sealed abstract class Input extends DefItem
case class StringInput(string: String) extends Input {
	override def equals(other: Any) = other match {
		case that: StringInput => (that canEqual this) && (that.string == string)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[StringInput]
}
sealed abstract class CharacterInput extends Input {
	def acceptable(char: Char): Boolean
}
case class AnyCharacterInput extends CharacterInput {
	override def equals(other: Any) = other match {
		case that: AnyCharacterInput => (that canEqual this)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[AnyCharacterInput]
	def acceptable(char: Char) = true
}
case class PoolCharacterInput(chars: Array[Char]) extends CharacterInput {
	def acceptable(char: Char) = (chars contains char)

	override def equals(other: Any) = other match {
		case that: PoolCharacterInput => (that canEqual this) && ((that.chars toSet) == (chars toSet))
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[PoolCharacterInput]
}
case class UnicodeCategoryCharacterInput(categories: Array[Byte]) extends CharacterInput {
	override def equals(other: Any) = other match {
		case that: UnicodeCategoryCharacterInput => (that canEqual this) && ((that.categories toSet) == (categories toSet))
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[UnicodeCategoryCharacterInput]
	def acceptable(char: Char) = categories contains char.getType
}
case class CharacterRangeInput(from: Char, to: Char) extends CharacterInput {
	override def equals(other: Any) = other match {
		case that: CharacterRangeInput => (that canEqual this) && (that.from == from) && (that.to == to)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[CharacterRangeInput]
	def acceptable(char: Char) = (from <= char && char <= to)
}
case class VirtualInput(name: String) extends Input {
	override def equals(other: Any) = other match {
		case that: VirtualInput => (that canEqual this) && (that.name == name)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[VirtualInput]
}

case class Sequence(seq: List[DefItem], whitespace: List[DefItem]) extends DefItem {
	override def equals(other: Any) = other match {
		case that: Sequence => (that canEqual this) && (that.seq == seq) && (that.whitespace == whitespace)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[Sequence]
}
case class OneOf(items: List[DefItem]) extends DefItem {
	override def equals(other: Any) = other match {
		case that: OneOf => (that canEqual this) && (that.items == items)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[OneOf]
}
case class Except(item: DefItem, except: List[DefItem]) extends DefItem {
	override def equals(other: Any) = other match {
		case that: Except => (that canEqual this) && (that.item == item) && (that.except == except)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[Except]
}
case class LookaheadExcept(except: List[DefItem]) extends DefItem {
	override def equals(other: Any) = other match {
		case that: LookaheadExcept => (that canEqual this) && (that.except == except)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[LookaheadExcept]
}
case class Repeat(item: DefItem, range: RepeatRange) extends DefItem {
	override def equals(other: Any) = other match {
		case that: Repeat => (that canEqual this) && (that.item == item) && (that.range == range)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[Repeat]
}

sealed abstract class RepeatRange {
	def contains(v: Int): Boolean
	def canEqual(other: Any): Boolean
	def canProceed(x: Int): Boolean
	val from: Int
}
case class RepeatRangeFrom(val from: Int) extends RepeatRange {
	def contains(v: Int) = from <= v

	override def equals(other: Any) = other match {
		case that: RepeatRangeFrom => (that canEqual this) && (that.from == from)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[RepeatRangeFrom]

	override def canProceed(x: Int): Boolean = true
}
case class RepeatRangeTo(val from: Int, val to: Int) extends RepeatRange {
	override def contains(v: Int) = from <= v && v <= to

	override def equals(other: Any) = other match {
		case that: RepeatRangeTo => (that canEqual this) && (that.from == from) && (that.to == to)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[RepeatRangeTo]

	override def canProceed(x: Int): Boolean = x < to
}

abstract class ActGrammar extends Grammar {
	val rulesWithAct: Map[String, List[DefItem]]

	type ActionMap = Map[(List[DefItem], (Int, DefItem)), ((StackSymbol, List[Any]) => Any)]

	def unmarshallItem(item: DefItem): DefItem =
		item match {
			case ActDefItem(item, _) => unmarshallItem(item)
			case Sequence(seq, whitespace) =>
				Sequence(seq map (unmarshallItem _), whitespace map (unmarshallItem _))
			case OneOf(items) =>
				OneOf(items map (unmarshallItem _))
			case Except(item, except) =>
				Except(unmarshallItem(item), except map (unmarshallItem _))
			case Repeat(item, range) =>
				Repeat(unmarshallItem(item), range)
			case LookaheadExcept(except) =>
				LookaheadExcept(except map (unmarshallItem _))
			case _: Nonterminal | _: Input => item
		}
	final lazy val rules: RuleMap = {
		rulesWithAct map ((x) => (x._1, x._2 map (unmarshallItem _)))
	}
	final lazy val actions: ActionMap = {
		def unmarshall(path: List[DefItem], rhs: DefItem, pointer: Int): ActionMap = {
			val lhs = path.head
			rhs match {
				case ActDefItem(item, action) =>
					unmarshall(path, item, 0) + (((path, (pointer, unmarshallItem(item))), action))
				case Sequence(seq, whitespace) =>
					seq.foldLeft((Map(): ActionMap, 0))((cc, y) => (cc._1 ++ unmarshall(unmarshallItem(rhs) +: path, y, cc._2), cc._2 + 1))._1
				case OneOf(items) =>
					items.foldLeft(Map(): ActionMap)((cc, i) => cc ++ unmarshall(unmarshallItem(rhs) +: path, i, 0))
				case Except(item, _) => unmarshall(unmarshallItem(rhs) +: path, item, 0)
				case Repeat(item, _) => unmarshall(unmarshallItem(rhs) +: path, item, 0)
				case _: LookaheadExcept => Map()
				case _: Nonterminal | _: Input => Map()
			}
		}
		var map: ActionMap = Map()
		rulesWithAct foreach ((x) =>
			x._2 foreach (map ++= unmarshall(List(Nonterminal(x._1)), _, 0)))
		map
	}
	def process(symbol: StackSymbol): Any = {
		def proc(path: List[DefItem], symbol: StackSymbol, action: (StackSymbol, List[Any]) => Any): Any = {
			// println(path)
			symbol match {
				case StartSymbol => symbol
				case NontermSymbol(lhs) =>
					val children = lhs.children.foldLeft((0, List[Any]()))((cc, _rhs) => {
						val processed = _rhs match {
							case NontermSymbol(rhs) =>
								// println(s"$path := ${rhs.item}")
								(actions get ((path, (cc._1, rhs.item)))) match {
									case Some(action) =>
										if (rhs.item.isInstanceOf[Nonterminal]) proc(List(rhs.item), _rhs, action)
										else proc(rhs.item +: path, _rhs, action)
									case None =>
										rhs.item match {
											case _: Sequence | _: Repeat =>
												proc(rhs.item +: path, _rhs, (_, objs) => objs)
											case _: Nonterminal =>
												proc(List(rhs.item), _rhs, (_, objs) => objs(0))
											case _ =>
												proc(rhs.item +: path, _rhs, (_, objs) => objs(0))
										}
								}
							case EmptySymbol(item) =>
								(actions get ((path, (cc._1, item)))) match {
									case Some(action) => proc(item +: path, _rhs, action)
									case None => _rhs
								}
							case x => x
						}
						(cc._1 + 1, cc._2 :+ processed)
					})._2
					action(symbol, children)
				case TermSymbol(_, _) | EmptySymbol(_) => action(symbol, List(symbol))
			}
		}
		symbol match {
			case NontermSymbol(x) if x.item.isInstanceOf[Nonterminal] =>
				proc(List(x.item), symbol, (_, objs) => { assert(objs.length == 1); objs(0) })
			case _ =>
				assert(false)
		}
	}

	implicit def defItemActing(item: DefItem): Actingable =
		item match {
			case n: Nonterminal => new Actingable { val self = n }
			case i: Input => new Actingable { val self = i }
			case o: OneOf => new Actingable { val self = o }
			case r: Repeat => new Actingable { val self = r }
			case s: Sequence => new Actingable { val self = s }
			case e: Except => new Actingable { val self = e }
			case _ => throw new Exception("Applied act to the items that cannot be")
		}
}

trait Actingable {
	val self: DefItem

	def act(action: (StackSymbol, List[Any]) => Any) = new ActDefItem(self, action)
}

case class ActDefItem(item: DefItem, val action: (StackSymbol, List[Any]) => Any) extends DefItem
