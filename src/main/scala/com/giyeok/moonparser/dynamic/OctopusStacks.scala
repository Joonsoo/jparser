package com.giyeok.moonparser.dynamic

trait OctopusStacks {
    this: Parser =>

    /*
     * OctopusStack simulates the stacks in general parsers out there,
     * but it is actually a tree
     */
    class OctopusStack[T](val bottom: T) {
        private val tops = scala.collection.mutable.Queue[T](bottom)
        private val parents = scala.collection.mutable.HashMap[T, T]()

        def add(parent: T, entry: T) = {
            tops += entry
            parents += (entry -> parent)
        }
        def addAll(parent: T, entries: Seq[T]) = for (entry <- entries) add(parent, entry)
        def hasNext = !tops.isEmpty
        def top = tops.front
        def pop() = tops.dequeue()
        def iterator = tops.iterator

        def parentOf(child: T): T = parents(child)
        def parentOfOpt(child: T): Option[T] = parents get child
    }
}
