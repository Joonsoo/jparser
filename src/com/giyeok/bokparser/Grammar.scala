package com.giyeok.bokparser

import scala.collection.immutable.ListMap

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
	def oneof(items: DefItem*) = OneOf(items toArray)
	def oneof(items: List[DefItem]) = OneOf(items toArray)
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

case class Sequence(seq: List[DefItem], whitespace: List[DefItem]) extends DefItem
case class OneOf(items: Array[DefItem]) extends DefItem
case class Except(item: DefItem, except: List[DefItem]) extends DefItem
case class LookaheadExcept(except: List[DefItem]) extends DefItem
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

	final lazy val rules: RuleMap = {
		def unmarshall(item: DefItem): DefItem =
			item match {
				case ActDefItem(item, _) => unmarshall(item)
				case Sequence(seq, whitespace) =>
					Sequence(seq map (unmarshall _), whitespace map (unmarshall _))
				case OneOf(items) =>
					OneOf(items map (unmarshall _))
				case Except(item, except) =>
					Except(unmarshall(item), except map (unmarshall _))
				case Repeat(item, range) =>
					Repeat(unmarshall(item), range)
				case LookaheadExcept(except) =>
					LookaheadExcept(except map (unmarshall _))
				case _: Nonterminal | _: Input => item
			}
		rulesWithAct map ((x) => (x._1, x._2 map (unmarshall _)))
	}
	type ActionMap = Map[(DefItem, DefItem), ((StackSymbol, List[Object]) => Object)]
	final lazy val actions: ActionMap = {
		def unmarshall(lhs: DefItem, rhs: DefItem): ActionMap =
			rhs match {
				case ActDefItem(item, action) => 
					unmarshall(lhs, item) + (((lhs, item), action))
				case Sequence(seq, whitespace) =>
					var x: ActionMap = Map()
					seq foreach (x ++= unmarshall(rhs, _))
					whitespace foreach (x ++= unmarshall(rhs, _))
					x
				case OneOf(items) =>
					var x: ActionMap = Map()
					items foreach (x ++= unmarshall(rhs, _))
					x
				case Except(item, except) =>
					var x: ActionMap = Map()
					x ++= unmarshall(rhs, item)
					except foreach (x ++= unmarshall(rhs, _))
					x
				case Repeat(item, _) =>
					unmarshall(rhs, item)
				case _: LookaheadExcept => Map()
				case _: Nonterminal | _: Input => Map()
			}
		var map: ActionMap = Map()
		rulesWithAct foreach ((x) =>
			x._2 foreach (map ++= unmarshall(Nonterminal(x._1), _))
		)
		map
	}
	def process(symbol: StackSymbol): Object = {
		symbol match {
			case StartSymbol => symbol
			case NontermSymbol(item) =>
				item.children foreach (process _)
			case TermSymbol(_, _) => symbol
			case EmptySymbol => symbol
		}
		new Integer(1)
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

	def act(action: (StackSymbol, List[Object]) => Object) = new ActDefItem(self, action)
}

case class ActDefItem(item: DefItem, val action: (StackSymbol, List[Object]) => Object) extends DefItem
