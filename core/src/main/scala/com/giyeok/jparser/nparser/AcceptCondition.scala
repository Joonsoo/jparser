package com.giyeok.jparser.nparser

import com.giyeok.jparser.NGrammar.NAtomicSymbol

object AcceptCondition {
    import ParsingContext._

    sealed trait AcceptCondition {
        def nodes: Set[Node]
        def shiftGen(gen: Int): AcceptCondition
        def evaluate(gen: Int, graph: Graph): AcceptCondition
        def acceptable(gen: Int, graph: Graph): Boolean
        def neg: AcceptCondition
    }
    sealed trait SymbolCondition extends AcceptCondition {
        val symbolId: Int
        val symbol: NAtomicSymbol
        val beginGen: Int

        lazy val node0: Node = Node(Kernel(symbolId, 0, beginGen, beginGen)(symbol), Always)
        def kernel1(endGen: Int): Kernel = Kernel(symbolId, 1, beginGen, endGen)(symbol)
        lazy val nodes: Set[Node] = Set(node0)
    }
    def conjunct(conditions: AcceptCondition*): AcceptCondition =
        if (conditions contains Never) Never
        else {
            val conds1 = conditions.toSet filter { _ != Always }
            if (conds1.isEmpty) Always
            else if (conds1.size == 1) conds1.head
            else {
                val conds2 = conds1 flatMap {
                    case And(set) => set
                    case item => Set(item)
                }
                And(conds2)
            }
        }
    def disjunct(conditions: AcceptCondition*): AcceptCondition = {
        if (conditions contains Always) Always
        else {
            val conds1 = conditions.toSet filter { _ != Never }
            if (conds1.isEmpty) Never
            else if (conds1.size == 1) conds1.head
            else {
                val conds2 = conds1 flatMap {
                    case Or(set) => set
                    case item => Set(item)
                }
                Or(conds2)
            }
        }
    }

    case object Always extends AcceptCondition {
        val nodes: Set[Node] = Set[Node]()
        def shiftGen(gen: Int): AcceptCondition = this
        def evaluate(gen: Int, graph: Graph): AcceptCondition = this
        def acceptable(gen: Int, graph: Graph) = true
        def neg = Never
    }
    case object Never extends AcceptCondition {
        val nodes: Set[Node] = Set[Node]()
        def shiftGen(gen: Int): AcceptCondition = this
        def evaluate(gen: Int, graph: Graph): AcceptCondition = this
        def acceptable(gen: Int, graph: Graph) = false
        def neg = Always
    }
    case class And(conditions: Set[AcceptCondition]) extends AcceptCondition {
        // assert(conditions forall { c => c != True && c != False })

        def nodes: Set[Node] = conditions flatMap { _.nodes }
        def shiftGen(gen: Int): AcceptCondition =
            And(conditions map { _.shiftGen(gen) })
        def evaluate(gen: Int, graph: Graph): AcceptCondition =
            conditions.foldLeft[AcceptCondition](Always) { (cc, condition) =>
                conjunct(condition.evaluate(gen, graph), cc)
            }
        def acceptable(gen: Int, graph: Graph): Boolean =
            conditions forall { _.acceptable(gen, graph) }
        def neg: AcceptCondition = disjunct((conditions map { _.neg }).toSeq: _*)
    }
    case class Or(conditions: Set[AcceptCondition]) extends AcceptCondition {
        // assert(conditions forall { c => c != True && c != False })

        def nodes: Set[Node] = conditions flatMap { _.nodes }
        def shiftGen(gen: Int): AcceptCondition = Or(conditions map { _.shiftGen(gen) })
        def evaluate(gen: Int, graph: Graph): AcceptCondition =
            conditions.foldLeft[AcceptCondition](Never) { (cc, condition) =>
                disjunct(condition.evaluate(gen, graph), cc)
            }
        def acceptable(gen: Int, graph: Graph): Boolean =
            conditions exists { _.acceptable(gen, graph) }
        def neg: AcceptCondition = conjunct((conditions map { _.neg }).toSeq: _*)
    }

    case class NotExists(beginGen: Int, endGen: Int, symbolId: Int)(val symbol: NAtomicSymbol) extends AcceptCondition with SymbolCondition {
        def shiftGen(gen: Int): AcceptCondition =
            NotExists(beginGen + gen, endGen + gen, symbolId)(symbol)
        def evaluate(gen: Int, graph: Graph): AcceptCondition = {
            if (gen < endGen) this else {
                val conditions0 = graph.conditionsOf(kernel1(gen)) map { _.neg.evaluate(gen, graph) }
                val conditions = conditions0 ++ (if (graph.nodes contains node0) Set(this) else Set())
                conjunct(conditions.toSeq: _*)
            }
        }
        def acceptable(gen: Int, graph: Graph): Boolean = {
            if (gen < endGen) true else {
                graph.conditionsOf(kernel1(gen)) forall { _.acceptable(gen, graph) == false }
            }
        }
        def neg: AcceptCondition = Exists(beginGen, endGen, symbolId)(symbol)
    }
    case class Exists(beginGen: Int, endGen: Int, symbolId: Int)(val symbol: NAtomicSymbol) extends AcceptCondition with SymbolCondition {
        def shiftGen(gen: Int): AcceptCondition =
            Exists(beginGen + gen, endGen + gen, symbolId)(symbol)
        def evaluate(gen: Int, graph: Graph): AcceptCondition = {
            if (gen < endGen) this else {
                val conditions0 = graph.conditionsOf(kernel1(gen)) map { _.evaluate(gen, graph) }
                val conditions = conditions0 ++ (if (graph.nodes contains node0) Set(this) else Set())
                disjunct(conditions.toSeq: _*)
            }
        }
        def acceptable(gen: Int, graph: Graph): Boolean = {
            if (gen < endGen) false else {
                graph.conditionsOf(kernel1(gen)) exists { _.acceptable(gen, graph) }
            }
        }
        def neg: AcceptCondition = NotExists(beginGen, endGen, symbolId)(symbol)
    }

    case class Unless(beginGen: Int, endGen: Int, symbolId: Int)(val symbol: NAtomicSymbol) extends AcceptCondition with SymbolCondition {
        def shiftGen(gen: Int): AcceptCondition =
            Unless(beginGen + gen, endGen + gen, symbolId)(symbol)
        def evaluate(gen: Int, graph: Graph): AcceptCondition = {
            assert(gen >= endGen)
            if (gen > endGen) {
                Always
            } else {
                val conditions = graph.conditionsOf(kernel1(gen))
//                if (conditions.isEmpty) {
//                    if (graph.nodes contains node0) this else Always
//                } else {
                disjunct(conditions.toSeq: _*).neg.evaluate(gen, graph)
//                }
            }
        }
        def acceptable(gen: Int, graph: Graph): Boolean = {
            assert(gen >= endGen)
            if (gen != endGen) true else {
                graph.conditionsOf(kernel1(gen)) forall { _.acceptable(gen, graph) == false }
            }
        }
        def neg: AcceptCondition =
            OnlyIf(beginGen, endGen, symbolId)(symbol)
    }
    case class OnlyIf(beginGen: Int, endGen: Int, symbolId: Int)(val symbol: NAtomicSymbol) extends AcceptCondition with SymbolCondition {
        def shiftGen(gen: Int): AcceptCondition =
            OnlyIf(beginGen + gen, endGen + gen, symbolId)(symbol)
        def evaluate(gen: Int, graph: Graph): AcceptCondition = {
            assert(gen >= endGen)
            if (gen > endGen) {
                Never
            } else {
                val conditions = graph.conditionsOf(kernel1(gen))
//                if (conditions.isEmpty) {
//                    if (graph.nodes contains node0) this else Never
//                } else {
                disjunct(conditions.toSeq: _*).evaluate(gen, graph)
//                }
            }
        }
        def acceptable(gen: Int, graph: Graph): Boolean = {
            assert(gen >= endGen)
            if (gen != endGen) false else {
                graph.conditionsOf(kernel1(gen)) exists { _.acceptable(gen, graph) }
            }
        }
        def neg: AcceptCondition =
            Unless(beginGen, endGen, symbolId)(symbol)
    }
}
