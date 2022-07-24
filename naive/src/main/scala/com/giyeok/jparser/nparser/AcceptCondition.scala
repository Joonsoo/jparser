package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser2.{ParsingContext => ParsingContext2}
import com.giyeok.jparser.nparser2.{Kernel => Kernel2}

object AcceptCondition {

  import ParsingContext._

  sealed trait AcceptCondition extends Equals {
    def nodes: Set[Node]

    def evaluate(gen: Int, graph: Graph): AcceptCondition

    def evolve(gen: Int, ctx: ParsingContext2): AcceptCondition

    def acceptable(gen: Int, graph: Graph): Boolean

    def accepted(gen: Int, ctx: ParsingContext2): Boolean

    def neg: AcceptCondition
  }

  // accept condition의 조건으로 들어오는 심볼은 모두 atomic symbol
  sealed trait SymbolCondition extends AcceptCondition {
    val symbolId: Int
    val beginGen: Int

    lazy val node0: Node = Node(Kernel(symbolId, 0, beginGen, beginGen), Always)

    def kernel1(endGen: Int): Kernel = Kernel(symbolId, 1, beginGen, endGen)

    def initKernel: Kernel2 = Kernel2(symbolId, 0, beginGen, beginGen)

    def finalKernel(endGen: Int): Kernel2 = Kernel2(symbolId, 1, beginGen, endGen)

    lazy val nodes: Set[Node] = Set(node0)
  }

  private def containsConflictingConditions(conditions: Set[AcceptCondition]): Boolean = conditions exists {
    case NotExists(beginGen, endGen, symbolId) => conditions contains Exists(beginGen, endGen, symbolId)
    case Exists(beginGen, endGen, symbolId) => conditions contains NotExists(beginGen, endGen, symbolId)
    case Unless(beginGen, endGen, symbolId) => conditions contains OnlyIf(beginGen, endGen, symbolId)
    case OnlyIf(beginGen, endGen, symbolId) => conditions contains Unless(beginGen, endGen, symbolId)
    case _ => false
  }

  // conjunct는 condition들을 and로 연결
  def conjunct(conditions: AcceptCondition*): AcceptCondition =
    if (conditions contains Never) Never
    else {
      val conds1 = conditions flatMap {
        case And(set) => set
        case item => Set(item)
      }
      val conds2 = conds1.toSet filter {
        _ != Always
      }
      if (conds2.isEmpty) Always
      else if (conds2.size == 1) conds2.head
      else {
        // 상충되는 두 조건(e.g. Exist(a, b, c)와 NotExists(a, b, c))이 함께 들어 있으면 Never 반환
        if (containsConflictingConditions(conds2)) Never else And(conds2)
      }
    }

  // disjunct는 condition들을 or로 연결
  def disjunct(conditions: AcceptCondition*): AcceptCondition = {
    if (conditions contains Always) Always
    else {
      val conds1 = conditions flatMap {
        case Or(set) => set
        case item => Set(item)
      }
      val conds2 = conds1.toSet filter (_ != Never)
      if (conds2.isEmpty) Never
      else if (conds2.size == 1) conds2.head
      else {
        // 상충되는 두 조건(e.g. Exist(a, b, c)와 NotExists(a, b, c))이 함께 들어 있으면 Always 반환
        if (containsConflictingConditions(conds2)) Always else Or(conds2)
      }
    }
  }

  case object Always extends AcceptCondition {
    val nodes: Set[Node] = Set()

    override def evaluate(gen: Int, graph: Graph): AcceptCondition = this

    override def evolve(gen: Int, ctx: ParsingContext2): AcceptCondition = this

    override def acceptable(gen: Int, graph: Graph) = true

    override def accepted(gen: Int, ctx: ParsingContext2): Boolean = true

    def neg: AcceptCondition = Never
  }

  case object Never extends AcceptCondition {
    val nodes: Set[Node] = Set()

    override def evaluate(gen: Int, graph: Graph): AcceptCondition = this

    override def evolve(gen: Int, ctx: ParsingContext2): AcceptCondition = this

    override def acceptable(gen: Int, graph: Graph) = false

    override def accepted(gen: Int, ctx: ParsingContext2): Boolean = true

    def neg: AcceptCondition = Always
  }

  case class And(conditions: Set[AcceptCondition]) extends AcceptCondition {
    // assert(conditions forall { c => c != True && c != False })

    def nodes: Set[Node] = conditions flatMap (_.nodes)

    override def evaluate(gen: Int, graph: Graph): AcceptCondition =
      conditions.foldLeft[AcceptCondition](Always) { (cc, condition) =>
        conjunct(condition.evaluate(gen, graph), cc)
      }

    override def evolve(gen: Int, ctx: ParsingContext2): AcceptCondition =
      conditions.foldLeft[AcceptCondition](Always) { (cc, condition) =>
        conjunct(condition.evolve(gen, ctx), cc)
      }

    override def acceptable(gen: Int, graph: Graph): Boolean = conditions forall (_.acceptable(gen, graph))

    override def accepted(gen: Int, ctx: ParsingContext2): Boolean = conditions forall (_.accepted(gen, ctx))

    def neg: AcceptCondition = disjunct((conditions map (_.neg)).toSeq: _*)
  }

  case class Or(conditions: Set[AcceptCondition]) extends AcceptCondition {
    // assert(conditions forall { c => c != True && c != False })

    def nodes: Set[Node] = conditions flatMap (_.nodes)

    override def evaluate(gen: Int, graph: Graph): AcceptCondition =
      conditions.foldLeft[AcceptCondition](Never) { (cc, condition) =>
        disjunct(condition.evaluate(gen, graph), cc)
      }

    override def evolve(gen: Int, ctx: ParsingContext2): AcceptCondition =
      conditions.foldLeft[AcceptCondition](Never) { (cc, condition) =>
        disjunct(condition.evolve(gen, ctx), cc)
      }

    override def acceptable(gen: Int, graph: Graph): Boolean = conditions exists (_.acceptable(gen, graph))

    override def accepted(gen: Int, ctx: ParsingContext2): Boolean = conditions exists (_.accepted(gen, ctx))

    def neg: AcceptCondition = conjunct((conditions map {
      _.neg
    }).toSeq: _*)
  }

  case class NotExists(beginGen: Int, endGen: Int, symbolId: Int) extends AcceptCondition with SymbolCondition {
    def evaluate(gen: Int, graph: Graph): AcceptCondition = {
      if (gen < endGen) this else {
        val conditions0 = graph.conditionsOf(kernel1(gen)) map {
          _.neg.evaluate(gen, graph)
        }
        val conditions = conditions0 ++ (if (graph.nodes contains node0) Set(this) else Set())
        conjunct(conditions.toSeq: _*)
      }
    }

    override def evolve(gen: Int, ctx: ParsingContext2): AcceptCondition = {
      if (gen < endGen) this else {
        val condition = ctx.acceptConditions(finalKernel(gen)).neg.evolve(gen, ctx)
        if (ctx.graph.nodes contains initKernel) conjunct(condition, this) else condition
      }
    }

    override def acceptable(gen: Int, graph: Graph): Boolean = {
      if (gen < endGen) true else {
        graph.conditionsOf(kernel1(gen)) forall {
          _.acceptable(gen, graph) == false
        }
      }
    }

    override def accepted(gen: Int, ctx: ParsingContext2): Boolean = {
      if (gen < endGen) true else {
        !ctx.acceptConditions(finalKernel(gen)).accepted(gen, ctx)
      }
    }


    def neg: AcceptCondition = Exists(beginGen, endGen, symbolId)
  }

  case class Exists(beginGen: Int, endGen: Int, symbolId: Int) extends AcceptCondition with SymbolCondition {
    override def evaluate(gen: Int, graph: Graph): AcceptCondition = {
      if (gen < endGen) this else {
        val conditions0 = graph.conditionsOf(kernel1(gen)) map {
          _.evaluate(gen, graph)
        }
        val conditions = conditions0 ++ (if (graph.nodes contains node0) Set(this) else Set())
        disjunct(conditions.toSeq: _*)
      }
    }

    override def evolve(gen: Int, ctx: ParsingContext2): AcceptCondition = {
      if (gen < endGen) this else {
        val condition = ctx.acceptConditions(finalKernel(gen)).evolve(gen, ctx)
        if (ctx.graph.nodes contains initKernel) disjunct(condition, this) else condition
      }
    }

    override def acceptable(gen: Int, graph: Graph): Boolean = {
      if (gen < endGen) false else {
        graph.conditionsOf(kernel1(gen)) exists {
          _.acceptable(gen, graph)
        }
      }
    }

    override def accepted(gen: Int, ctx: ParsingContext2): Boolean = {
      if (gen < endGen) false else {
        ctx.acceptConditions(finalKernel(gen)).accepted(gen, ctx)
      }
    }


    def neg: AcceptCondition = NotExists(beginGen, endGen, symbolId)
  }

  case class Unless(beginGen: Int, endGen: Int, symbolId: Int) extends AcceptCondition with SymbolCondition {
    def evaluate(gen: Int, graph: Graph): AcceptCondition = {
      assert(gen >= endGen)
      if (gen > endGen) {
        Always
      } else {
        val conditions = graph.conditionsOf(kernel1(gen))
        disjunct(conditions.toSeq: _*).neg.evaluate(gen, graph)
      }
    }

    override def evolve(gen: Int, ctx: ParsingContext2): AcceptCondition = {
      assert(gen >= endGen)
      if (gen > endGen) {
        Always
      } else {
        ctx.acceptConditions(finalKernel(gen)).neg.evolve(gen, ctx)
      }
    }

    override def acceptable(gen: Int, graph: Graph): Boolean = {
      assert(gen >= endGen)
      if (gen != endGen) true else {
        graph.conditionsOf(kernel1(gen)) forall {
          _.acceptable(gen, graph) == false
        }
      }
    }

    override def accepted(gen: Int, ctx: ParsingContext2): Boolean = {
      assert(gen >= endGen)
      if (gen != endGen) true else {
        !ctx.acceptConditions(finalKernel(gen)).accepted(gen, ctx)
      }
    }

    def neg: AcceptCondition =
      OnlyIf(beginGen, endGen, symbolId)
  }

  case class OnlyIf(beginGen: Int, endGen: Int, symbolId: Int) extends AcceptCondition with SymbolCondition {
    def evaluate(gen: Int, graph: Graph): AcceptCondition = {
      assert(gen >= endGen)
      if (gen > endGen) {
        Never
      } else {
        val conditions = graph.conditionsOf(kernel1(gen))
        disjunct(conditions.toSeq: _*).evaluate(gen, graph)
      }
    }

    override def evolve(gen: Int, ctx: ParsingContext2): AcceptCondition = {
      assert(gen >= endGen)
      if (gen > endGen) {
        Never
      } else {
        ctx.acceptConditions(finalKernel(gen)).evolve(gen, ctx)
      }
    }

    def acceptable(gen: Int, graph: Graph): Boolean = {
      assert(gen >= endGen)
      if (gen != endGen) false else {
        graph.conditionsOf(kernel1(gen)) exists {
          _.acceptable(gen, graph)
        }
      }
    }

    override def accepted(gen: Int, ctx: ParsingContext2): Boolean = {
      assert(gen >= endGen)
      if (gen != endGen) false else {
        ctx.acceptConditions(finalKernel(gen)).accepted(gen, ctx)
      }
    }

    def neg: AcceptCondition =
      Unless(beginGen, endGen, symbolId)
  }

  case class AcceptConditionSlot(slotIdx: Int) extends AcceptCondition {
    override def nodes: Set[Node] = Set()

    override def evaluate(gen: Int, graph: Graph): AcceptCondition = ???

    override def acceptable(gen: Int, graph: Graph): Boolean = ???

    override def accepted(gen: Int, ctx: ParsingContext2): Boolean = ???

    override def neg: AcceptCondition = AcceptConditionSlotNeg(slotIdx)

    override def evolve(gen: Int, ctx: ParsingContext2): AcceptCondition = ???
  }

  case class AcceptConditionSlotNeg(slotIdx: Int) extends AcceptCondition {
    override def nodes: Set[Node] = Set()

    override def evaluate(gen: Int, graph: Graph): AcceptCondition = ???

    override def acceptable(gen: Int, graph: Graph): Boolean = ???

    override def accepted(gen: Int, ctx: ParsingContext2): Boolean = ???

    override def neg: AcceptCondition = AcceptConditionSlot(slotIdx)

    override def evolve(gen: Int, ctx: ParsingContext2): AcceptCondition = ???
  }
}
