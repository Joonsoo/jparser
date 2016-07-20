package com.giyeok.jparser.nparser

import NGrammar._

object ParsingContext {
    sealed trait Node {
        val symbolId: Int
        val beginGen: Int

        def shiftGen(gen: Int): Node
    }
    case class SymbolNode(symbolId: Int, beginGen: Int) extends Node {
        def shiftGen(gen: Int) = SymbolNode(symbolId, beginGen + gen)
    }
    case class SequenceNode(symbolId: Int, pointer: Int, beginGen: Int, endGen: Int) extends Node {
        def shiftGen(gen: Int) = SequenceNode(symbolId, pointer, beginGen + gen, endGen + gen)
    }

    sealed trait Edge
    case class SimpleEdge(start: Node, end: Node) extends Edge
    case class JoinEdge(start: Node, end: Node, join: Node) extends Edge

    case class Graph(nodes: Set[Node], edges: Set[Edge], edgesByStart: Map[Node, Set[Edge]], edgesByDest: Map[Node, Set[Edge]]) {
        def addNode(node: Node) =
            if (nodes contains node) this else Graph(nodes + node, edges, edgesByStart + (node -> Set()), edgesByDest + (node -> Set()))
        def addEdge(edge: Edge) = edge match {
            case SimpleEdge(start, end) =>
                val edgesByStart1 = edgesByStart.updated(start, edgesByStart(start) + edge)
                val edgesByDest1 = edgesByDest.updated(end, (edgesByDest(end) + edge))
                Graph(nodes, edges + edge, edgesByStart1, edgesByDest1)
            case JoinEdge(start, end, join) =>
                Graph(nodes, edges + edge, edgesByStart.updated(start, edgesByStart(start) + edge), edgesByDest.updated(end, edgesByDest(end) + edge).updated(join, edgesByDest(join) + edge))
        }
        def removeNode(node: Node) = {
            val involvedEdges = edgesByStart(node) ++ edgesByDest(node)
            val edgesByStart1 = edgesByStart mapValues { _ -- involvedEdges }
            val edgesByDest1 = edgesByDest mapValues { _ -- involvedEdges }
            Graph(nodes - node, edges -- involvedEdges, edgesByStart1 - node, edgesByDest1 - node)
        }
    }
    object Graph {
        def apply(nodes: Set[Node], edges: Set[Edge]): Graph = {
            var edgesByStart: Map[Node, Set[Edge]] = (nodes map { n => (n -> Set[Edge]()) }).toMap
            var edgesByDest: Map[Node, Set[Edge]] = (nodes map { n => (n -> Set[Edge]()) }).toMap
            edges foreach {
                case edge @ SimpleEdge(start, end) =>
                    edgesByStart += start -> (edgesByStart(start) + edge)
                    edgesByDest += end -> (edgesByDest(end) + edge)
                case edge @ JoinEdge(start, end, join) =>
                    edgesByStart += start -> (edgesByStart(start) + edge)
                    edgesByDest += end -> (edgesByDest(end) + edge)
                    edgesByDest += join -> (edgesByDest(join) + edge)
            }
            Graph(nodes, edges, edgesByStart.toMap, edgesByDest.toMap)
        }
    }

    case class Results[N <: Node](nodeConditions: Map[N, Set[EligCondition.Condition]]) {
        def of(node: N) = nodeConditions get node
        def contains(node: N, condition: EligCondition.Condition): Boolean =
            nodeConditions get node match {
                case Some(conditions) => conditions contains condition
                case None => false
            }
        def contains(node: N, conditions: Set[EligCondition.Condition]): Boolean =
            nodeConditions get node match {
                case Some(existingConditions) => conditions subsetOf existingConditions
                case None => false
            }
        def conditions = nodeConditions flatMap { _._2 }
        def involvedNodes = conditions flatMap { _.nodes }
        def update(node: N, condition: EligCondition.Condition): Results[N] =
            nodeConditions get node match {
                case Some(conditions) => Results(nodeConditions + (node -> (conditions + condition)))
                case None => Results(nodeConditions + (node -> Set(condition)))
            }
        def update(node: N, conditions: Set[EligCondition.Condition]): Results[N] =
            nodeConditions get node match {
                case Some(existingConditions) => Results(nodeConditions + (node -> (existingConditions ++ conditions)))
                case None => Results(nodeConditions + (node -> conditions))
            }
        def mapCondition(func: EligCondition.Condition => EligCondition.Condition): Results[N] =
            Results(nodeConditions mapValues { _ map { c => func(c) } })
        def trimFalse: (Results[N], Set[N]) = {
            val falseNodes = (nodeConditions filter { _._2 == Set(EligCondition.False) }).keySet
            (Results(nodeConditions -- falseNodes), falseNodes)
        }
    }
    object Results {
        def apply[N <: Node](): Results[N] = Results(Map())
    }

    case class Context(graph: Graph, progresses: Results[SequenceNode], finishes: Results[Node]) {
        def updateGraph(newGraph: Graph): Context =
            Context(newGraph, progresses, finishes)
        def updateGraph(graphUpdateFunc: Graph => Graph): Context =
            Context(graphUpdateFunc(graph), progresses, finishes)

        def updateProgresses(newProgresses: Results[SequenceNode]): Context =
            Context(graph, newProgresses, finishes)
        def updateProgresses(progressesUpdateFunc: Results[SequenceNode] => Results[SequenceNode]): Context =
            updateProgresses(progressesUpdateFunc(progresses))

        def updateFinishes(newFinishes: Results[Node]): Context =
            Context(graph, progresses, newFinishes)
        def updateFinishes(finishesUpdateFunc: Results[Node] => Results[Node]): Context =
            updateFinishes(finishesUpdateFunc(finishes))

        def emptyFinishes = Context(graph, progresses, Results())
    }
}

object EligCondition {
    import ParsingContext._

    sealed trait Condition {
        def nodes: Set[Node]
        def shiftGen(gen: Int): Condition
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]): Condition
        def eligible: Boolean
        def neg: Condition
    }
    def conjunct(conditions: Condition*) =
        if (conditions exists { _ == False }) False
        else {
            val conds1 = conditions.toSet filterNot { _ == True }
            if (conds1.isEmpty) True
            else if (conds1.size == 1) conds1.head
            else And(conds1)
        }
    def disjunct(conditions: Condition*): Condition = {
        if (conditions exists { _ == True }) True
        else {
            val conds1 = conditions.toSet filterNot { _ == False }
            if (conds1.isEmpty) False
            else if (conds1.size == 1) conds1.head
            else Or(conds1)
        }
    }

    object True extends Condition {
        val nodes = Set[Node]()
        def shiftGen(gen: Int) = this
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) = this
        def eligible = true
        def neg = False
    }
    object False extends Condition {
        val nodes = Set[Node]()
        def shiftGen(gen: Int) = this
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) = this
        def eligible = false
        def neg = True
    }
    case class And(conditions: Set[Condition]) extends Condition {
        assert(conditions forall { c => c != True && c != False })

        def nodes = conditions flatMap { _.nodes }
        def shiftGen(gen: Int) = And(conditions map { _.shiftGen(gen) })
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) =
            conditions.foldLeft[Condition](True) { (cc, condition) =>
                conjunct(condition.evaluate(gen, finishes, survived), cc)
            }
        def eligible = conditions forall { _.eligible }
        def neg = disjunct((conditions map { _.neg }).toSeq: _*)
    }
    case class Or(conditions: Set[Condition]) extends Condition {
        assert(conditions forall { c => c != True && c != False })

        def nodes = conditions flatMap { _.nodes }
        def shiftGen(gen: Int) = Or(conditions map { _.shiftGen(gen) })
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) =
            conditions.foldLeft[Condition](False) { (cc, condition) =>
                disjunct(condition.evaluate(gen, finishes, survived), cc)
            }
        def eligible = conditions exists { _.eligible }
        def neg = conjunct((conditions map { _.neg }).toSeq: _*)
    }
    case class Until(node: Node, activeGen: Int) extends Condition {
        def nodes = Set(node)
        def shiftGen(gen: Int) = Until(node.shiftGen(gen), activeGen + gen)
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) =
            if (gen <= activeGen) {
                this
            } else {
                finishes.of(node) match {
                    case Some(conditions) =>
                        disjunct(conditions.toSeq: _*).evaluate(gen, finishes, survived).neg
                    case None =>
                        if (survived contains node) this else True
                }
            }
        def eligible = true
        def neg = After(node, activeGen)
    }
    case class After(node: Node, activeGen: Int) extends Condition {
        def nodes = Set(node)
        def shiftGen(gen: Int) = After(node.shiftGen(gen), activeGen + gen)
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) =
            if (gen <= activeGen) {
                this
            } else {
                finishes.of(node) match {
                    case Some(conditions) =>
                        disjunct(conditions.toSeq: _*).evaluate(gen, finishes, survived)
                    case None =>
                        if (survived contains node) this else False
                }
            }
        val eligible = false
        def neg = Until(node, activeGen)
    }
    case class Alive(node: Node, activeGen: Int) extends Condition {
        def nodes = Set(node)
        def shiftGen(gen: Int) = Alive(node.shiftGen(gen), activeGen + gen)
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) =
            if (gen <= activeGen) this else { if (survived contains node) False else True }
        def eligible = true
        def neg = ???
    }
    case class Exclude(node: Node) extends Condition {
        def nodes = Set(node)
        def shiftGen(gen: Int) = Exclude(node.shiftGen(gen))
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) =
            finishes.of(node) match {
                case Some(conditions) =>
                    val evaluated = disjunct(conditions.toSeq: _*).evaluate(gen, finishes, survived)
                    // evaluated 안에는 Exclude가 없어야 한다
                    assert({
                        def check(condition: Condition): Boolean =
                            condition match {
                                case _: Exclude => false
                                case And(conds) => conds forall check
                                case Or(conds) => conds forall check
                                case _ => true
                            }
                        check(evaluated)
                    })
                    evaluated.neg
                case None => True
            }
        def eligible = ???
        def neg = ???
    }
}
