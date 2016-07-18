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
        def addNode(node: Node) = Graph(nodes + node, edges, edgesByStart + (node -> Set()), edgesByDest + (node -> Set()))
        def addEdge(edge: Edge) = edge match {
            case SimpleEdge(start, end) =>
                Graph(nodes, edges + edge, edgesByStart + (start -> (edgesByStart(start) + edge)), edgesByDest + (end -> (edgesByDest(end) + edge)))
            case JoinEdge(start, end, join) =>
                Graph(nodes, edges + edge, edgesByStart + (start -> (edgesByStart(start) + edge)), edgesByDest + (end -> (edgesByDest(end) + edge)) + (join -> (edgesByDest(join) + edge)))
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
    }

    case class Context(graph: Graph, progresses: Results[SequenceNode], finishes: Results[Node]) {
        def updateGraph(newGraph: Graph): Context = {
            Context(newGraph, progresses, finishes)
        }
        def updateGraph(graphUpdateFunc: Graph => Graph): Context = {
            val newGraph = graphUpdateFunc(graph)
            Context(newGraph, progresses, finishes)
        }
        def updateProgresses(progressesUpdateFunc: Results[SequenceNode] => Results[SequenceNode]): Context = {
            val newProgresses = progressesUpdateFunc(progresses)
            Context(graph, newProgresses, finishes)
        }
        def updateFinishes(finishesUpdateFunc: Results[Node] => Results[Node]): Context = {
            val newFinishes = finishesUpdateFunc(finishes)
            Context(graph, progresses, newFinishes)
        }
    }
}

object EligCondition {
    import ParsingContext._

    sealed trait Condition {
        def nodes: Set[Node]
        def shiftGen(gen: Int): Condition
        def evaluate(gen: Int, results: Results[Node], survived: Set[Node]): Condition
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
        def evaluate(gen: Int, results: Results[Node], survived: Set[Node]) = this
        def eligible = true
        def neg = False
    }
    object False extends Condition {
        val nodes = Set[Node]()
        def shiftGen(gen: Int) = this
        def evaluate(gen: Int, results: Results[Node], survived: Set[Node]) = this
        def eligible = false
        def neg = True
    }
    case class And(conditions: Set[Condition]) extends Condition {
        assert(conditions forall { c => c != True && c != False })

        def nodes = conditions flatMap { _.nodes }
        def shiftGen(gen: Int) = And(conditions map { _.shiftGen(gen) })
        def evaluate(gen: Int, results: Results[Node], survived: Set[Node]) =
            conditions.foldLeft[Condition](True) { (cc, condition) =>
                conjunct(condition.evaluate(gen, results, survived), cc)
            }
        def eligible = conditions forall { _.eligible }
        def neg = disjunct((conditions map { _.neg }).toSeq: _*)
    }
    case class Or(conditions: Set[Condition]) extends Condition {
        assert(conditions forall { c => c != True && c != False })

        def nodes = conditions flatMap { _.nodes }
        def shiftGen(gen: Int) = Or(conditions map { _.shiftGen(gen) })
        def evaluate(gen: Int, results: Results[Node], survived: Set[Node]) =
            conditions.foldLeft[Condition](False) { (cc, condition) =>
                disjunct(condition.evaluate(gen, results, survived), cc)
            }
        def eligible = conditions exists { _.eligible }
        def neg = conjunct((conditions map { _.neg }).toSeq: _*)
    }
    case class Until(node: Node, activeGen: Int) extends Condition {
        def nodes = Set(node)
        def shiftGen(gen: Int) = Until(node.shiftGen(gen), activeGen + gen)
        def evaluate(gen: Int, results: Results[Node], survived: Set[Node]) =
            if (gen <= activeGen) {
                this
            } else {
                results.of(node) match {
                    case Some(conditions) =>
                        disjunct(conditions.toSeq: _*).evaluate(gen, results, survived)
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
        def evaluate(gen: Int, results: Results[Node], survived: Set[Node]) =
            if (gen <= activeGen) {
                this
            } else {
                results.of(node) match {
                    case Some(conditions) =>
                        disjunct(conditions.toSeq: _*).evaluate(gen, results, survived)
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
        def evaluate(gen: Int, results: Results[Node], survived: Set[Node]) =
            if (gen < activeGen) this else { if (survived contains node) False else True }
        def eligible = true
        def neg = ???
    }
    case class Exclude(node: Node) extends Condition {
        def nodes = ???
        def shiftGen(gen: Int) = Exclude(node.shiftGen(gen))
        def evaluate(gen: Int, results: Results[Node], survived: Set[Node]) =
            results.of(node) match {
                case Some(conditions) =>
                    val evaluated = disjunct(conditions.toSeq: _*).evaluate(gen, results, survived)
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
