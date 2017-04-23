package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.NGrammar.NAtomicSymbol

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

    case class Until(symbolId: Int, beginGen: Int, activeGen: Int)(val symbol: NAtomicSymbol) extends AcceptCondition with SymbolCondition {
        def shiftGen(gen: Int): AcceptCondition =
            Until(symbolId, beginGen + gen, activeGen + gen)(symbol)
        def evaluate(gen: Int, graph: Graph): AcceptCondition =
            if (gen <= activeGen) this else {
                //                updatedNodes get node match {
                //                    case Some(updated) =>
                //                        assert(updated forall { _.kernel.isFinished })
                //                        disjunct((updated map { _.condition }).toSeq: _*).neg.evaluate(gen, graph, updatedNodes)
                //                    case None =>
                //                        if (graph.nodes contains node) this else Always
                //                }
                val conditions = graph.conditionsOf(kernel1(gen))
                if (conditions.isEmpty) {
                    if (graph.nodes contains node0) this else Always
                } else {
                    disjunct(conditions.toSeq: _*).neg.evaluate(gen, graph)
                }
            }
        def acceptable(gen: Int, graph: Graph): Boolean =
            if (gen <= activeGen) true else {
                //                updatedNodes get node match {
                //                    case Some(updated) =>
                //                        assert(updated forall { _.kernel.isFinished })
                //                        disjunct((updated map { _.condition }).toSeq: _*).neg.acceptable(gen, graph, updatedNodes)
                //                    case None => true
                //                }
                val conditions = graph.conditionsOf(kernel1(gen))
                if (conditions.isEmpty) {
                    true
                } else {
                    disjunct(conditions.toSeq: _*).neg.acceptable(gen, graph)
                }
            }
        def neg: AcceptCondition = After(symbolId, beginGen, activeGen)(symbol)
    }
    case class After(symbolId: Int, beginGen: Int, activeGen: Int)(val symbol: NAtomicSymbol) extends AcceptCondition with SymbolCondition {
        def shiftGen(gen: Int): AcceptCondition =
            After(symbolId, beginGen + gen, activeGen + gen)(symbol)
        def evaluate(gen: Int, graph: Graph): AcceptCondition =
            if (gen <= activeGen) this else {
                //                updatedNodes get node match {
                //                    case Some(updated) =>
                //                        assert(updated forall { _.kernel.isFinished })
                //                        disjunct((updated map { _.condition }).toSeq: _*).evaluate(gen, graph, updatedNodes)
                //                    case None =>
                //                        if (graph.nodes contains node) this else Never
                //                }
                val conditions = graph.conditionsOf(kernel1(gen))
                if (conditions.isEmpty) {
                    if (graph.nodes contains node0) this else Never
                } else {
                    disjunct(conditions.toSeq: _*).evaluate(gen, graph)
                }
            }
        def acceptable(gen: Int, graph: Graph): Boolean =
            if (gen <= activeGen) false else {
                //                updatedNodes get node match {
                //                    case Some(updated) =>
                //                        assert(updated forall { _.kernel.isFinished })
                //                        disjunct((updated map { _.condition }).toSeq: _*).acceptable(gen, graph, updatedNodes)
                //                    case None =>
                //                        false
                //                }
                val conditions = graph.conditionsOf(kernel1(gen))
                if (conditions.isEmpty) {
                    false
                } else {
                    disjunct(conditions.toSeq: _*).acceptable(gen, graph)
                }
            }

        def neg: AcceptCondition = Until(symbolId, beginGen, activeGen)(symbol)
    }

    case class Unless(symbolId: Int, beginGen: Int, targetGen: Int)(val symbol: NAtomicSymbol) extends AcceptCondition with SymbolCondition {
        def shiftGen(gen: Int): AcceptCondition =
            Unless(symbolId, beginGen + gen, targetGen + gen)(symbol)
        def evaluate(gen: Int, graph: Graph): AcceptCondition = {
            if (gen > targetGen) {
                Always
            } else {
                //                if (updatedNodes contains node) {
                //                    // node가 finish되었으면
                //                    // TODO updatedNodes(node)의 컨디션들의 neg.evaluate한 값이어야 함 - acceptable에도 반영해야 함
                //                    val updated = updatedNodes(node)
                //                    assert(updated forall { _.kernel.isFinished })
                //                    disjunct((updated map { _.condition }).toSeq: _*).neg.evaluate(gen, graph, updatedNodes)
                //                } else if (!(graph.nodes contains node)) {
                //                    // (이전 세대에서) trimming돼서 노드가 없어졌으면
                //                    Always
                //                } else {
                //                    // 그 외의 경우엔 그대로
                //                    this
                //                }
                val conditions = graph.conditionsOf(kernel1(gen))
                if (conditions.isEmpty) {
                    if (graph.nodes contains node0) this else Always
                } else {
                    disjunct(conditions.toSeq: _*).neg.evaluate(gen, graph)
                }
            }
        }
        def acceptable(gen: Int, graph: Graph): Boolean =
            // (gen != targetGen) || (!(updatedNodes contains node))
            if (gen != targetGen) true else {
                // gen == targetGen
                //                updatedNodes get node match {
                //                    case Some(updated) =>
                //                        assert(updated forall { _.kernel.isFinished })
                //                        disjunct((updated map { _.condition }).toSeq: _*).neg.acceptable(gen, graph, updatedNodes)
                //                    case None => true
                //                }
                val conditions = graph.conditionsOf(kernel1(gen))
                if (conditions.isEmpty) {
                    true
                } else {
                    disjunct(conditions.toSeq: _*).neg.acceptable(gen, graph)
                }
            }

        def neg: AcceptCondition =
            OnlyIf(symbolId, beginGen, targetGen)(symbol)
    }
    case class OnlyIf(symbolId: Int, beginGen: Int, targetGen: Int)(val symbol: NAtomicSymbol) extends AcceptCondition with SymbolCondition {
        def shiftGen(gen: Int): AcceptCondition =
            OnlyIf(symbolId, beginGen + gen, targetGen + gen)(symbol)
        def evaluate(gen: Int, graph: Graph): AcceptCondition = {
            if (gen > targetGen) {
                Never
            } else {
                //                if (updatedNodes contains node) {
                //                    // TODO updatedNodes(node)의 컨디션들의 evaluate한 값이어야 함 - acceptable에도 반영해야 함
                //                    val updated = updatedNodes(node)
                //                    assert(updated forall { _.kernel.isFinished })
                //                    disjunct((updated map { _.condition }).toSeq: _*).evaluate(gen, graph, updatedNodes)
                //                } else if (!(graph.nodes contains node)) {
                //                    // (이전 세대에서) trimming돼서 노드가 없어졌으면
                //                    Never
                //                } else {
                //                    // 그 외의 경우엔 그대로
                //                    this
                //                }
                val conditions = graph.conditionsOf(kernel1(gen))
                if (conditions.isEmpty) {
                    if (graph.nodes contains node0) this else Never
                } else {
                    disjunct(conditions.toSeq: _*).evaluate(gen, graph)
                }
            }
        }
        def acceptable(gen: Int, graph: Graph): Boolean =
            // (gen == targetGen) && (updatedNodes contains node)
            if (gen != targetGen) false else {
                // gen == targetGen
                //                updatedNodes get node match {
                //                    case Some(updated) =>
                //                        assert(updated forall { _.kernel.isFinished })
                //                        disjunct((updated map { _.condition }).toSeq: _*).acceptable(gen, graph, updatedNodes)
                //                    case None => false
                //                }
                val conditions = graph.conditionsOf(kernel1(gen))
                if (conditions.isEmpty) {
                    false
                } else {
                    disjunct(conditions.toSeq: _*).acceptable(gen, graph)
                }
            }
        def neg: AcceptCondition =
            Unless(symbolId, beginGen, targetGen)(symbol)
    }
}
