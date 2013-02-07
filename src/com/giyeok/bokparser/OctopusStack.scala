package com.giyeok.bokparser

import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue

object OctopusStack {
	def starter(parser: Parser): Parser#StackEntry = {
		new parser.StackEntry()
	}
}

class OctopusStack(val bottom: Parser#StackEntry) {
	def this(parser: Parser) = this(OctopusStack.starter(parser))

	import scala.collection.mutable.Queue

	private val tops = Queue[Parser#StackEntry](bottom)

	def add(entry: Parser#StackEntry) = tops += entry
	def addAll(entries: Seq[Parser#StackEntry]) = for (entry <- entries) add(entry)
	def hasNext = !tops.isEmpty
	def top = tops.front
	def pop() = tops.dequeue()
	def iterator = tops.iterator
}

class PreservingOctopusStack(bottom: Parser#StackEntry) extends OctopusStack(bottom) {
	def this(parser: Parser) = this(OctopusStack.starter(parser))

	protected var all = List[Parser#StackEntry](bottom)
	protected var done = List[Parser#StackEntry]()

	override def add(entry: Parser#StackEntry) = {
		all ::= entry
		super.add(entry)
	}
	override def pop() = {
		done ::= top
		super.pop()
	}
	def getAll = all
	def getDone = done
}

trait ChildrenMap extends PreservingOctopusStack {
	def getChildrenOf(parent: Parser#StackEntry) =
		for (entry <- all if entry.parent == parent) yield entry
}

trait HashedChildrenMap extends PreservingOctopusStack with ChildrenMap {
	import scala.collection.mutable.HashMap

	private val childrenMap = new HashMap[Parser#StackEntry, List[Parser#StackEntry]]()

	override def add(entry: Parser#StackEntry) = {
		if (entry.parent != null) {
			(childrenMap get entry.parent) match {
				case Some(l) => childrenMap(entry.parent) = l ::: List(entry)
				case None => childrenMap += entry.parent -> List(entry)
			}
		}
		super.add(entry)
	}
	override def getChildrenOf(parent: Parser#StackEntry) =
		(childrenMap get parent) match { case Some(v) => v case _ => List() }
}
