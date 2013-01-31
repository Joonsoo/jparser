package com.giyeok.bokparser
import scala.collection.immutable.ListMap

abstract class Grammar {
	val name: String
	val rules: ListMap[String, List[DefItem]]
	val startSymbol: String

	def nonterm(name: String) = Nonterminal(name)
	def nonterm(name: String, action: (List[Object]) => Object) = new NonterminalAction(name, action)
	def n(name: String) = nonterm(name)
	def n(name: String, action: (List[Object]) => Object) = nonterm(name, action)
	def input(string: String) = StringInput(string)
	def input(string: String, action: (List[Object]) => Object) = new StringInputAction(string, action)
	def i(string: String) = input(string)
	def i(string: String, action: (List[Object]) => Object) = input(string, action)
	def input_char() = AnyCharacterInput()
	def input_char(action: (List[Object]) => Object) = new AnyCharacterInputAction(action)
	def input_char(chars: Array[Char]) = PoolCharacterInput(chars)
	def input_char(chars: Array[Char], action: (List[Object]) => Object) = new PoolCharacterInputAction(chars, action)
	def c() = input_char()
	def c(action: (List[Object]) => Object) = input_char(action: (List[Object]) => Object)
	def c(chars: Array[Char]) = input_char(chars)
	def c(chars: Array[Char], action: (List[Object]) => Object) = input_char(chars, action)
	def c(chars: String) = input_char(chars.toCharArray())
	def c(chars: String, action: (List[Object]) => Object) = input_char(chars.toCharArray(), action)
	def unicode_categories(categories: String*): UnicodeCategoryCharacterInput = unicode_categories(categories toArray)
	def unicode_categories(categories: Array[Byte]) = UnicodeCategoryCharacterInput(categories)
	def unicode_categories(categories: Array[String]) = UnicodeCategoryCharacterInput(UnicodeUtil.translateCategoryNamesToByte(categories))
	def unicode_categories(categories: Array[Byte], action: (List[Object]) => Object): UnicodeCategoryCharacterInputAction = new UnicodeCategoryCharacterInputAction(categories, action)
	def virtual(name: String) = VirtualInput(name)
	def virtual(name: String, action: (List[Object]) => Object) = new VirtualInputAction(name, action)
	def seq(seq: DefItem*) = sequence(seq: _*)
	def sequence(seq: DefItem*) = Sequence(seq toList, List())
	def sequence(whitespace: List[DefItem], seq: DefItem*) = Sequence(seq toList, whitespace)
	def sequence(whitespace: List[DefItem], seq: List[DefItem], action: (List[Object]) => Object) = new SequenceAction(seq, whitespace, action)
	def oneof(items: DefItem*) = OneOf(items toArray)
	def oneof(items: List[DefItem]) = OneOf(items toArray)
	def oneof(items: Array[DefItem], action: (List[Object]) => Object) = new OneOfAction(items, action)
	def lookahead_except(except: DefItem*) = LookaheadExcept(except toList)
	def lookahead_except(except: List[DefItem], action: (List[Object]) => Object) = new LookaheadExceptAction(except, action)

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
	def except(except: List[DefItem], action: (List[Object]) => Object) = new ExceptAction(self, except, action)
	def butnot(except: DefItem*) = Except(self, except toList)
	def butnot(except: List[DefItem], action: (List[Object]) => Object) = new ExceptAction(self, except, action)
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

trait DefAction {
	val action: (List[Object]) => Object
}

case class Nonterminal(name: String) extends DefItem {
	override def equals(other: Any) = other match {
		case that: Nonterminal => (that canEqual this) && (that.name == name)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[Nonterminal]
}
class NonterminalAction(name: String, val action: (List[Object]) => Object) extends Nonterminal(name) with DefAction

sealed abstract class Input extends DefItem
case class StringInput(string: String) extends Input {
	override def equals(other: Any) = other match {
		case that: StringInput => (that canEqual this) && (that.string == string)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[StringInput]
}
class StringInputAction(string: String, val action: (List[Object]) => Object) extends StringInput(string) with DefAction
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
class AnyCharacterInputAction(val action: (List[Object]) => Object) extends AnyCharacterInput with DefAction
case class PoolCharacterInput(chars: Array[Char]) extends CharacterInput {
	override def equals(other: Any) = other match {
		case that: PoolCharacterInput => (that canEqual this) && ((that.chars toSet) == (chars toSet))
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[PoolCharacterInput]
	def acceptable(char: Char) = (chars contains char)
}
class PoolCharacterInputAction(chars: Array[Char], val action: (List[Object]) => Object) extends PoolCharacterInput(chars) with DefAction
case class UnicodeCategoryCharacterInput(categories: Array[Byte]) extends CharacterInput {
	override def equals(other: Any) = other match {
		case that: UnicodeCategoryCharacterInput => (that canEqual this) && ((that.categories toSet) == (categories toSet))
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[UnicodeCategoryCharacterInput]
	def acceptable(char: Char) = categories contains char.getType
}
class UnicodeCategoryCharacterInputAction(categories: Array[Byte], val action: (List[Object]) => Object) extends UnicodeCategoryCharacterInput(categories) with DefAction
case class CharacterRangeInput(from: Char, to: Char) extends CharacterInput {
	override def equals(other: Any) = other match {
		case that: CharacterRangeInput => (that canEqual this) && (that.from == from) && (that.to == to)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[CharacterRangeInput]
	def acceptable(char: Char) = (from <= char && char <= to)
}
class CharacterRangeInputAction(from: Char, to: Char, val action: (List[Object]) => Object) extends CharacterRangeInput(from, to) with DefAction
case class VirtualInput(name: String) extends Input {
	override def equals(other: Any) = other match {
		case that: VirtualInput => (that canEqual this) && (that.name == name)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[VirtualInput]
}
class VirtualInputAction(name: String, val action: (List[Object]) => Object) extends VirtualInput(name) with DefAction

case class Sequence(seq: List[DefItem], whitespace: List[DefItem]) extends DefItem
class SequenceAction(seq: List[DefItem], whitespace: List[DefItem], val action: (List[Object]) => Object) extends Sequence(seq, whitespace) with DefAction
case class OneOf(items: Array[DefItem]) extends DefItem
class OneOfAction(items: Array[DefItem], val action: (List[Object]) => Object) extends OneOf(items) with DefAction
case class Except(item: DefItem, except: List[DefItem]) extends DefItem
class ExceptAction(item: DefItem, except: List[DefItem], val action: (List[Object]) => Object) extends Except(item, except) with DefAction
case class LookaheadExcept(except: List[DefItem]) extends DefItem
class LookaheadExceptAction(except: List[DefItem], val action: (List[Object]) => Object) extends LookaheadExcept(except) with DefAction
case class Repeat(item: DefItem, range: RepeatRange) extends DefItem {
	override def equals(other: Any) = other match {
		case that: Repeat => (that canEqual this) && (that.item == item) && (that.range == range)
		case _ => false
	}
	override def canEqual(other: Any) = other.isInstanceOf[Repeat]
}
class RepeatAction(item: DefItem, range: RepeatRange, val action: (List[Object]) => Object) extends Repeat(item, range) with DefAction

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
