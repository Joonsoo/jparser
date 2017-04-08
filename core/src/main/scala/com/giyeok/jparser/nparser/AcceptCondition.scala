package com.giyeok.jparser.nparser

object AcceptCondition {
    import ParsingContext._

    sealed trait AcceptCondition {
        def nodes: Set[Node]
        def shiftGen(gen: Int): AcceptCondition
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean
        def neg: AcceptCondition
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
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition = this
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]) = true
        def neg = Never
    }
    case object Never extends AcceptCondition {
        val nodes: Set[Node] = Set[Node]()
        def shiftGen(gen: Int): AcceptCondition = this
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition = this
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]) = false
        def neg = Always
    }
    case class And(conditions: Set[AcceptCondition]) extends AcceptCondition {
        // assert(conditions forall { c => c != True && c != False })

        def nodes: Set[Node] = conditions flatMap { _.nodes }
        def shiftGen(gen: Int): AcceptCondition =
            And(conditions map { _.shiftGen(gen) })
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition =
            conditions.foldLeft[AcceptCondition](Always) { (cc, condition) =>
                conjunct(condition.evaluate(gen, graph, updatedNodes), cc)
            }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean =
            conditions forall { _.acceptable(gen, graph, updatedNodes) }
        def neg: AcceptCondition = disjunct((conditions map { _.neg }).toSeq: _*)
    }
    case class Or(conditions: Set[AcceptCondition]) extends AcceptCondition {
        // assert(conditions forall { c => c != True && c != False })

        def nodes: Set[Node] = conditions flatMap { _.nodes }
        def shiftGen(gen: Int): AcceptCondition = Or(conditions map { _.shiftGen(gen) })
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition =
            conditions.foldLeft[AcceptCondition](Never) { (cc, condition) =>
                disjunct(condition.evaluate(gen, graph, updatedNodes), cc)
            }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean =
            conditions exists { _.acceptable(gen, graph, updatedNodes) }
        def neg: AcceptCondition = conjunct((conditions map { _.neg }).toSeq: _*)
    }

    case class Until(node: Node, activeGen: Int) extends AcceptCondition {
        def nodes: Set[Node] = Set(node)
        def shiftGen(gen: Int): AcceptCondition =
            Until(node.shiftGen(gen), activeGen + gen)
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition =
            if (gen <= activeGen) this else {
                updatedNodes get node match {
                    case Some(updated) =>
                        assert(updated forall { _.kernel.isFinished })
                        disjunct((updated map { _.condition }).toSeq: _*).neg.evaluate(gen, graph, updatedNodes)
                    case None =>
                        if (graph.nodes contains node) this else Always
                }
            }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean =
            if (gen <= activeGen) true else {
                updatedNodes get node match {
                    case Some(updated) =>
                        assert(updated forall { _.kernel.isFinished })
                        disjunct((updated map { _.condition }).toSeq: _*).neg.acceptable(gen, graph, updatedNodes)
                    case None => true
                }
            }
        def neg: AcceptCondition = After(node, activeGen)
    }
    case class After(node: Node, activeGen: Int) extends AcceptCondition {
        def nodes: Set[Node] = Set(node)
        def shiftGen(gen: Int): AcceptCondition =
            After(node.shiftGen(gen), activeGen + gen)
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition =
            if (gen <= activeGen) this else {
                updatedNodes get node match {
                    case Some(updated) =>
                        assert(updated forall { _.kernel.isFinished })
                        disjunct((updated map { _.condition }).toSeq: _*).evaluate(gen, graph, updatedNodes)
                    case None =>
                        if (graph.nodes contains node) this else Never
                }
            }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean =
            if (gen <= activeGen) false else {
                updatedNodes get node match {
                    case Some(updated) =>
                        assert(updated forall { _.kernel.isFinished })
                        disjunct((updated map { _.condition }).toSeq: _*).acceptable(gen, graph, updatedNodes)
                    case None =>
                        false
                }
            }

        def neg: AcceptCondition = Until(node, activeGen)
    }

    case class Unless(node: Node, targetGen: Int) extends AcceptCondition {
        def nodes: Set[Node] = Set(node)
        def shiftGen(gen: Int): AcceptCondition =
            Unless(node.shiftGen(gen), targetGen + gen)
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition = {
            if (gen > targetGen) {
                Always
            } else if (updatedNodes contains node) {
                // node가 finish되었으면
                // TODO updatedNodes(node)의 컨디션들의 neg.evaluate한 값이어야 함 - acceptable에도 반영해야 함
                Never
            } else if (!(graph.nodes contains node)) {
                // (이전 세대에서) trimming돼서 노드가 없어졌으면
                Always
            } else {
                // 그 외의 경우엔 그대로
                this
            }
        }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean =
            (gen != targetGen) || (!(updatedNodes contains node))
        def neg: AcceptCondition =
            OnlyIf(node, targetGen)
    }
    case class OnlyIf(node: Node, targetGen: Int) extends AcceptCondition {
        def nodes: Set[Node] = Set(node)
        def shiftGen(gen: Int): AcceptCondition =
            OnlyIf(node.shiftGen(gen), targetGen + gen)
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition = {
            if (gen > targetGen) {
                Never
            } else if (updatedNodes contains node) {
                // TODO updatedNodes(node)의 컨디션들의 evaluate한 값이어야 함 - acceptable에도 반영해야 함
                Always
            } else if (!(graph.nodes contains node)) {
                // (이전 세대에서) trimming돼서 노드가 없어졌으면
                Never
            } else {
                // 그 외의 경우엔 그대로
                this
            }
        }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean =
            (gen == targetGen) && (updatedNodes contains node)
        def neg: AcceptCondition =
            Unless(node, targetGen)
    }
}
