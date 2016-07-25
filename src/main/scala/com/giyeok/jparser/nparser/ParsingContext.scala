package com.giyeok.jparser.nparser

import NGrammar._
import com.giyeok.jparser.Inputs.Input

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

    sealed trait Edge { val start: Node; val end: Node }
    case class SimpleEdge(start: Node, end: Node) extends Edge
    case class JoinEdge(start: Node, end: Node, join: Node) extends Edge

    case class Graph(nodes: Set[Node], edges: Set[Edge], edgesByStart: Map[Node, Set[Edge]], edgesByDest: Map[Node, Set[Edge]]) {
        // assert(edgesByStart.keySet == nodes && edgesByDest.keySet == nodes)
        // assert((edgesByStart flatMap { _._2 }).toSet subsetOf edges)
        // assert((edgesByDest flatMap { _._2 }).toSet subsetOf edges)
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
        def removeNodes(removing: Set[Node]) = {
            removing.foldLeft(this) { _ removeNode _ }
        }
        def removeNode(removing: Node) = {
            val removedEdges = edges -- edgesByStart(removing) -- edgesByDest(removing)
            val (removedEdgesByStart, removedEdgesByDest) = {
                val edgesByDest0 = edgesByStart(removing).foldLeft(edgesByDest) { (ccDest, edge) =>
                    edge match {
                        case SimpleEdge(_, end) =>
                            ccDest + (end -> (ccDest(end) - edge))
                        case JoinEdge(_, end, join) =>
                            ccDest + (end -> (ccDest(end) - edge)) + (join -> (ccDest(join) - edge))
                    }
                }
                edgesByDest(removing).foldLeft((edgesByStart, edgesByDest0)) { (cc, edge) =>
                    val (ccStart, ccDest) = cc
                    edge match {
                        case edge @ SimpleEdge(start, _) =>
                            (ccStart + (start -> (ccStart(start) - edge)), ccDest)
                        case edge @ JoinEdge(start, `removing`, other) =>
                            (ccStart + (start -> (ccStart(start) - edge)), ccDest + (other -> (ccDest(other) - edge)))
                        case edge @ JoinEdge(start, other, _) =>
                            (ccStart + (start -> (ccStart(start) - edge)), ccDest + (other -> (ccDest(other) - edge)))
                    }
                }
            }
            Graph(nodes - removing, removedEdges, removedEdgesByStart - removing, removedEdgesByDest - removing)
        }
        def shiftGen(gen: Int): Graph = {
            def shiftGen(edge: Edge, gen: Int): Edge = edge match {
                case SimpleEdge(start, end) => SimpleEdge(start.shiftGen(gen), end.shiftGen(gen))
                case JoinEdge(start, end, join) => JoinEdge(start.shiftGen(gen), end.shiftGen(gen), join.shiftGen(gen))
            }
            Graph(nodes map { _.shiftGen(gen) }, edges map { shiftGen(_, gen) },
                edgesByStart map { kv => (kv._1.shiftGen(gen)) -> (kv._2 map { shiftGen(_, gen) }) },
                edgesByDest map { kv => (kv._1.shiftGen(gen)) -> (kv._2 map { shiftGen(_, gen) }) })
        }
        def replaceNode(original: Node, replaced: Node): Graph = {
            if (!(nodes contains original)) this else {
                def replace(node: Node) = if (node == original) replaced else node
                val replacedNodes = nodes - original + replaced
                var replacedEdges = edges
                var replacedEdgesByStart = edgesByStart - original + (replaced -> Set[Edge]())
                var replacedEdgesByDest = edgesByDest - original + (replaced -> Set[Edge]())
                (edgesByStart(original) ++ edgesByDest(original)) foreach {
                    case edge @ SimpleEdge(start, end) =>
                        val (rstart, rend) = (replace(start), replace(end))
                        val replacedEdge = SimpleEdge(rstart, rend)
                        replacedEdges = replacedEdges - edge + replacedEdge
                        replacedEdgesByStart += (rstart -> (replacedEdgesByStart(rstart) - edge + replacedEdge))
                        replacedEdgesByDest += (rend -> (replacedEdgesByDest(rend) - edge + replacedEdge))
                    case edge @ JoinEdge(start, end, join) =>
                        val (rstart, rend, rjoin) = (replace(start), replace(end), replace(join))
                        val replacedEdge = JoinEdge(rstart, rend, rjoin)
                        replacedEdges = replacedEdges - edge + replacedEdge
                        replacedEdgesByStart += (rstart -> (replacedEdgesByStart(rstart) - edge + replacedEdge))
                        replacedEdgesByDest += (rend -> (replacedEdgesByDest(rend) - edge + replacedEdge))
                        replacedEdgesByDest += (rjoin -> (replacedEdgesByDest(rjoin) - edge + replacedEdge))
                }
                Graph(replacedNodes, replacedEdges, replacedEdgesByStart, replacedEdgesByDest)
            }
        }
        def merge(other: Graph): Graph = {
            def mergeEdgesMap(map: Map[Node, Set[Edge]], merging: Map[Node, Set[Edge]]): Map[Node, Set[Edge]] =
                merging.foldLeft(map) { (cc, i) =>
                    val (node, edges) = i
                    if (!(cc contains node)) cc + (node -> edges) else cc + (node -> (cc(node) ++ edges))
                }
            Graph(nodes ++ other.nodes, edges ++ other.edges, mergeEdgesMap(edgesByStart, other.edgesByStart), mergeEdgesMap(edgesByDest, other.edgesByDest))
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
        def isEmpty = nodeConditions.isEmpty
        def nodes = nodeConditions.keySet
        def conditions = nodeConditions flatMap { _._2 }
        def conditionNodes = conditions flatMap { _.nodes }
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
            val filteredNodeConditions = nodeConditions mapValues { _ filter { _ != EligCondition.False } }
            val falseNodes = (filteredNodeConditions filter { _._2.isEmpty }).keySet
            (Results(filteredNodeConditions -- falseNodes), falseNodes)
        }
        def removeNode(node: N): Results[N] = Results(nodeConditions - node)
        def shiftGen(gen: Int): Results[N] =
            Results(nodeConditions map { kv => (kv._1.shiftGen(gen).asInstanceOf[N]) -> (kv._2 map { _.shiftGen(gen) }) })
        def replaceNode(original: N, replaced: N): Results[N] = {
            // replaceNode는 preprocessed parser에서만 사용하는데, 그렇게 사용하는 경우 Condition의 node중에는 original이 없어야 함
            // assert(!(conditionNodes.toSet contains original))
            val replacedNodeConditions = nodeConditions get original match {
                case Some(conditions) => (nodeConditions - original) + (replaced -> conditions)
                case None => nodeConditions
            }
            Results(replacedNodeConditions)
        }
        def merge(other: Results[N]): Results[N] = {
            val commonNodes = nodes intersect other.nodes
            val newMap = commonNodes.foldLeft(nodeConditions ++ other.nodeConditions) { (cc, node) =>
                cc + (node -> (nodeConditions(node) ++ other.nodeConditions(node)))
            }
            Results(newMap)
        }
    }
    object Results {
        def apply[N <: Node](): Results[N] = Results(Map[N, Set[EligCondition.Condition]]())
        def apply[N <: Node](items: (N, Set[EligCondition.Condition])*): Results[N] = Results(items.toMap)
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

        def shiftGen(gen: Int) = Context(graph.shiftGen(gen), progresses.shiftGen(gen), finishes.shiftGen(gen))

        def merge(otherCtx: Context): Context = {
            val Context(otherGraph, otherProgresses, otherFinishes) = otherCtx
            val (mergedGraph, mergedProgresses, mergedFinishes) = (graph.merge(otherGraph), progresses.merge(otherProgresses), finishes.merge(otherFinishes))
            // assert(mergedProgresses.nodes == (mergedGraph.nodes collect { case n: SequenceNode => n }))
            Context(mergedGraph, mergedProgresses, mergedFinishes)
        }
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

    case object True extends Condition {
        val nodes = Set[Node]()
        def shiftGen(gen: Int) = this
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) = this
        def eligible = true
        def neg = False
    }
    case object False extends Condition {
        val nodes = Set[Node]()
        def shiftGen(gen: Int) = this
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) = this
        def eligible = false
        def neg = True
    }
    case class And(conditions: Set[Condition]) extends Condition {
        // assert(conditions forall { c => c != True && c != False })

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
        // assert(conditions forall { c => c != True && c != False })

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
        def neg = Dead(node, activeGen)
    }
    case class Dead(node: Node, activeGen: Int) extends Condition {
        def nodes = Set(node)
        def shiftGen(gen: Int) = Dead(node.shiftGen(gen), activeGen + gen)
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) =
            if (gen <= activeGen) this else { if (survived contains node) True else False }
        def eligible = false
        def neg = Alive(node, activeGen)
    }
    case class Exclude(node: Node) extends Condition {
        def nodes = Set(node)
        def shiftGen(gen: Int) = Exclude(node.shiftGen(gen))
        def evaluate(gen: Int, finishes: Results[Node], survived: Set[Node]) =
            finishes.of(node) match {
                case Some(conditions) =>
                    val evaluated = disjunct(conditions.toSeq: _*).evaluate(gen, finishes, survived)
                    // evaluated 안에는 Exclude가 없어야 한다
                    // assert({
                    //     def check(condition: Condition): Boolean =
                    //         condition match {
                    //             case _: Exclude => false
                    //             case And(conds) => conds forall check
                    //             case Or(conds) => conds forall check
                    //             case _ => true
                    //         }
                    //     check(evaluated)
                    // })
                    evaluated.neg
                case None => True
            }
        def eligible = ???
        def neg = ???
    }
}
