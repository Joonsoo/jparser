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
            else And(conds1)
        }
    def disjunct(conditions: AcceptCondition*): AcceptCondition = {
        if (conditions contains Always) Always
        else {
            val conds1 = conditions.toSet filter { _ != Never }
            if (conds1.isEmpty) Never
            else if (conds1.size == 1) conds1.head
            else Or(conds1)
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
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean = conditions forall { _.acceptable(gen: Int, graph, updatedNodes) }
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
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean = conditions exists { _.acceptable(gen: Int, graph, updatedNodes) }
        def neg: AcceptCondition = conjunct((conditions map { _.neg }).toSeq: _*)
    }

    case class Until(node: Node, activeGen: Int) extends AcceptCondition {
        def nodes: Set[Node] = Set(node)
        def shiftGen(gen: Int): AcceptCondition =
            Until(node.shiftGen(gen), activeGen + gen)
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition = ???
        //        if (gen <= activeGen) {
        //            this
        //        } else {
        //            finishes.of(node) match {
        //                case Some(conditions) =>
        //                    disjunct(conditions.toSeq: _*).evaluate(gen, finishes, survived).neg
        //                case None =>
        //                    if (survived contains node) this else Always
        //            }
        //        }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean = true
        def neg: AcceptCondition = After(node, activeGen)
    }
    case class After(node: Node, activeGen: Int) extends AcceptCondition {
        def nodes: Set[Node] = Set(node)
        def shiftGen(gen: Int): AcceptCondition =
            After(node.shiftGen(gen), activeGen + gen)
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition = ???
        //            if (gen <= activeGen) {
        //                this
        //            } else {
        //                finishes.of(node) match {
        //                    case Some(conditions) =>
        //                        disjunct(conditions.toSeq: _*).evaluate(gen, finishes, survived)
        //                    case None =>
        //                        if (survived contains node) this else Never
        //                }
        //            }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean = false
        def neg: AcceptCondition = Until(node, activeGen)
    }

    case class Alive(node: Node, activeGen: Int) extends AcceptCondition {
        def nodes: Set[Node] = Set(node)
        def shiftGen(gen: Int): AcceptCondition =
            Alive(node.shiftGen(gen), activeGen + gen)
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition = ???
        //            if (gen <= activeGen) this else { if (survived contains node) Never else Always }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean = true
        def neg: AcceptCondition = Dead(node, activeGen)
    }
    case class Dead(node: Node, activeGen: Int) extends AcceptCondition {
        def nodes: Set[Node] = Set(node)
        def shiftGen(gen: Int): AcceptCondition =
            Dead(node.shiftGen(gen), activeGen + gen)
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition = ???
        //            if (gen <= activeGen) this else { if (survived contains node) Always else Never }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean = false
        def neg: AcceptCondition = Alive(node, activeGen)
    }

    case class Unless(node: Node) extends AcceptCondition {
        def nodes: Set[Node] = Set(node)
        def shiftGen(gen: Int): AcceptCondition =
            Unless(node.shiftGen(gen))
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition = {
            if (!(graph.nodes contains node)) {
                // (이전 세대에서) trimming돼서 노드가 없어졌으면
                Always
            } else {
                // 그 외의 경우엔 그대로
                this
            }
        }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean =
            !(updatedNodes contains node)
        def neg: AcceptCondition =
            OnlyIf(node)
    }
    case class OnlyIf(node: Node) extends AcceptCondition {
        def nodes: Set[Node] = Set(node)
        def shiftGen(gen: Int): AcceptCondition =
            OnlyIf(node.shiftGen(gen))
        def evaluate(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): AcceptCondition = {
            if (!(graph.nodes contains node)) {
                // (이전 세대에서) trimming돼서 노드가 없어졌으면
                Never
            } else {
                // 그 외의 경우엔 그대로
                this
            }
        }
        def acceptable(gen: Int, graph: Graph, updatedNodes: Map[Node, Set[Node]]): Boolean =
            updatedNodes contains node
        def neg: AcceptCondition =
            Unless(node)
    }
}
