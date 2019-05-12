package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.Inputs.{CharacterTermGroupDesc, CharsGroup}
import com.giyeok.jparser.examples.SimpleGrammars
import com.giyeok.jparser.nparser.NGrammar

class DisambigParserRunner(val disambigParser: DisambigParser) {

    case class Stack(nodeId: Int, prev: Option[Stack]) {
        def toIds: Seq[Int] = prev match {
            case Some(p) => p.toIds :+ nodeId
            case None => Seq(nodeId)
        }

        def toIdsString: String = toIds mkString " "

        def replaceTop(replace: Int) = Stack(replace, prev)

        def replaceTopSeq(replace: List[Int]): Stack = replace match {
            case List() => prev.get
            case init :+ last => Stack(last, Some(replaceTopSeq(init)))
        }

        def append(append: Int) = Stack(append, Some(this))

        def pop(): Stack = prev.get

        def pop(count: Int): Stack = (0 until count).foldLeft(this) { (s, _) => s.pop() }

        def finish(): Context = prev match {
            case Some(popped) => edgeAction(disambigParser.edgeActions(popped.nodeId -> nodeId))
            case None => SucceededContext(nodeId)
        }

        def edgeAction(edgeAction: DisambigParser.EdgeAction): Context = edgeAction match {
            case DisambigParser.DropLast(replace) => prev match {
                case Some(popped) => popped.replaceTop(replace).finish()
                case None => SucceededContext(replace)
            }
            case DisambigParser.ReplaceEdge(replacePrev, replaceLast, pF) =>
                ActiveContext(pop().replaceTop(replacePrev).append(replaceLast), pF map DropReplaceFinish)
            case DisambigParser.PopAndReplaceForEdge(popCount, replace, pF) =>
                ActiveContext(pop(popCount).replaceTop(replace), pF map ReplaceFinish)
        }
    }

    def createStack(nodes: List[Int]): Option[Stack] = nodes match {
        case List() => None
        case init :+ last => Some(Stack(last, createStack(init)))
    }

    sealed trait Context {
        def proceed(c: Char): Context

        def proceedEof(): Context
    }

    case class SucceededContext(lastNodeId: Int) extends Context {
        override def proceed(c: Char): Context = FailedContext(CharacterTermGroupDesc.empty, c)

        override def proceedEof(): Context = this
    }

    case class FailedContext(expected: CharacterTermGroupDesc, actual: Char) extends Context {
        override def proceed(c: Char): Context = this

        override def proceedEof(): Context = this
    }

    sealed trait PendingFinish

    case class DropReplaceFinish(replace: Int) extends PendingFinish

    case class ReplaceFinish(replace: Int) extends PendingFinish

    case class ActiveContext(stack: Stack, pendingFinish: Option[PendingFinish]) extends Context {
        def termAction(termAction: DisambigParser.TermAction): Context = termAction match {
            case DisambigParser.Finish(replace) => stack.replaceTop(replace).finish()
            case DisambigParser.Append(replace, append, pF) =>
                ActiveContext(stack.replaceTop(replace).append(append), pF map DropReplaceFinish)
            case DisambigParser.PopAndReplace(popCount, replace, pF) =>
                val newStack = stack.pop(popCount).replaceTop(replace)
                ActiveContext(newStack, pF map ReplaceFinish)
            case DisambigParser.ReplaceToSeq(replace, pF) =>
                ActiveContext(stack.replaceTopSeq(replace), pF map DropReplaceFinish)
        }

        def applyPendingFin: Context = pendingFinish.get match {
            case DropReplaceFinish(replace) => stack.pop().replaceTop(replace).finish()
            case ReplaceFinish(replace) => stack.replaceTop(replace).finish()
        }

        override def proceed(c: Char): Context = {
            val termActions = disambigParser.termActionsByNodeId(stack.nodeId)
            termActions find (_._1.contains(c)) match {
                case Some((_, action)) => termAction(action)
                case None =>
                    if (pendingFinish.isDefined) applyPendingFin.proceed(c)
                    else FailedContext(CharacterTermGroupDesc.merge(termActions.keys), c)
            }
        }

        def proceed(string: String): Context =
            string.foldLeft(this.asInstanceOf[Context])(_ proceed _)

        def proceedEof(): Context = {
            def repeatFinish(ctx: Context): Context = ctx match {
                case ctx: ActiveContext => repeatFinish(ctx.stack.finish().proceedEof())
                case _ => ctx
            }

            repeatFinish(this)
        }
    }

    def initialContext = ActiveContext(Stack(disambigParser.startNodeId, None), None)

    def describeContext(ctx: Context, indent: String): Unit = ctx match {
        case ctx@ActiveContext(stack, pendingFinish) =>
            println(s"$indent${stack.toIdsString}  pf=${pendingFinish.getOrElse(-1)}")
            val acceptableTerms = CharacterTermGroupDesc.merge(disambigParser.acceptableTermsOf(stack.nodeId))
            println(s"$indent: acceptable=${acceptableTerms.toShortString}")
            if (pendingFinish.isDefined) {
                describeContext(ctx.applyPendingFin, indent + "    ")
            }
        case SucceededContext(lastNodeId) =>
            println(s"${indent}succeeded - lastNode=$lastNodeId")
        case FailedContext(expected, actual) =>
            println(s"${indent}failed - expected=${expected.toShortString}, actual='$actual'")
    }
}

object DisambigParserRunner {
    def array0Example: DisambigParser = {
        val grammar = NGrammar.fromGrammar(SimpleGrammars.array0Grammar)
        val simpleParser = new SimpleParserGen(grammar).generateParser()
        val baseNodes = simpleParser.nodes

        def DisambigNode(paths: Seq[NodePath]) =
            AKernelSetPathSet((paths map { path =>
                AKernelSetPath(path.nodes map simpleParser.nodes)
            }))

        val nodes0 = (baseNodes.keySet map { nodeId =>
            nodeId -> DisambigNode(Seq(NodePath(List(nodeId))))
        }).toMap

        // 20: (2 -> 3), (12)
        // 21: (2 -> 3 -> 9), (7 -> 11)
        // 22: (2 -> 3 -> 16), (12)
        // 23: (2 -> 3 -> 16 -> 9), (7 -> 11)

        // Append를 치환할 때는 (path 길이 - 2), ReplaceEdge를 치환할 때는 (path 길이 - 1)
        // Append/ReplaceEdge(2, 3, Some(2))인 지점 term (1, 'a') -> PopAndReplace(0, 20, None)
        // Append/ReplaceEdge(3, 16, Some(3))인 지점 edge (3, 16) -> PopAndReplace(2, 22, None)

        // (20, ' ') -> PopAndReplace(0, 21, Some(7))
        // (20, ',') -> Restore(List(2, 3, 8), None)
        // (20, ']') -> Finish(4)
        // (21, ' ') -> PopAndReplace(0, 21, Some(7))
        // (21, ',') -> Restore(List(2, 3, 8), None)

        // (22, ' ') -> PopAndReplace(0, 23, Some(7))
        // (22, ']') -> Finish(4)
        // (22, ',') -> Restore(List(2, 3, 16, 8), None)
        // (23, ' ') -> PopAndReplace(0, 23, Some(7))
        // (23, ',') -> Restore(List(2, 3, 16, 8), None)

        val nodes: Map[Int, AKernelSetPathSet] = nodes0 +
            (20 -> DisambigNode(Seq(NodePath(List(2, 3)), NodePath(List(12))))) +
            (21 -> DisambigNode(Seq(NodePath(List(2, 3, 9)), NodePath(List(7, 11))))) +
            (22 -> DisambigNode(Seq(NodePath(List(2, 3, 16)), NodePath(List(12))))) +
            (23 -> DisambigNode(Seq(NodePath(List(2, 3, 16, 9)), NodePath(List(7, 11)))))

        val termActions0 = simpleParser.termActions map { kv =>
            val (trigger, action0) = kv
            val action = action0 match {
                case SimpleParser.Append(replace, append, pendingFinish) =>
                    DisambigParser.Append(replace, append, pendingFinish)
                case SimpleParser.Finish(replace) =>
                    DisambigParser.Finish(replace)
            }
            trigger -> action
        }
        val termA = CharsGroup(Set(), Set(), Set('a'))
        val termSpace = CharsGroup(Set(), Set(), Set(' '))
        val termComma = CharsGroup(Set(), Set(), Set(','))
        val termClose = CharsGroup(Set(), Set(), Set(']'))
        val termActions: Map[(Int, Inputs.CharacterTermGroupDesc), DisambigParser.TermAction] = termActions0 +
            ((1, termA) -> DisambigParser.PopAndReplace(0, 20, None)) +
            ((20, termSpace) -> DisambigParser.PopAndReplace(0, 21, Some(7))) +
            ((20, termComma) -> DisambigParser.ReplaceToSeq(List(2, 3, 8), None)) +
            ((20, termClose) -> DisambigParser.Finish(4)) +
            ((21, termSpace) -> DisambigParser.PopAndReplace(0, 21, Some(7))) +
            ((21, termComma) -> DisambigParser.ReplaceToSeq(List(2, 3, 8), None)) +
            ((22, termSpace) -> DisambigParser.PopAndReplace(0, 23, Some(7))) +
            ((22, termClose) -> DisambigParser.Finish(4)) +
            ((22, termComma) -> DisambigParser.ReplaceToSeq(List(2, 3, 16, 8), None)) +
            ((23, termSpace) -> DisambigParser.PopAndReplace(0, 23, Some(7))) +
            ((23, termComma) -> DisambigParser.ReplaceToSeq(List(2, 3, 16, 8), None))

        val edgeActions0 = simpleParser.edgeActions map { kv =>
            val (edge, action0) = kv
            val action = action0 match {
                case SimpleParser.ReplaceEdge(replacePrev, replaceLast, pendingFinish) =>
                    DisambigParser.ReplaceEdge(replacePrev, replaceLast, pendingFinish)
                case SimpleParser.DropLast(replace) =>
                    DisambigParser.DropLast(replace)
            }
            edge -> action
        }
        val edgeActions: Map[(Int, Int), DisambigParser.EdgeAction] = edgeActions0 +
            ((3, 16) -> DisambigParser.PopAndReplaceForEdge(2, 22, None))

        new DisambigParser(grammar, nodes,
            null, 0, termActions, edgeActions)
    }

    def main(args: Array[String]): Unit = {
        val runner = new DisambigParserRunner(array0Example)

        val input = "[a,a,   a,   a  ,a  ,  a  ,  a ]"
        input.foldLeft(runner.initialContext.asInstanceOf[runner.Context]) { (ctx, c) =>
            println(s"Proceed '$c'")
            val nextCtx = ctx.proceed(c)
            runner.describeContext(nextCtx, "")
            nextCtx
        }
    }
}
