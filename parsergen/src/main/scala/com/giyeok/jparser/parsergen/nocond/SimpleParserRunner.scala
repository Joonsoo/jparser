package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.examples.SimpleGrammars
import com.giyeok.jparser.parsergen.nocond.codegen.SimpleParserJavaGen

class SimpleParserRunner(val simpleParser: SimpleParser) {

    case class Stack(nodeId: Int, prev: Option[Stack]) {
        def toIds: Seq[Int] = prev match {
            case Some(p) => p.toIds :+ nodeId
            case None => Seq(nodeId)
        }

        def toIdsString: String = toIds mkString " "

        def toDescs: Seq[String] = {
            val desc = simpleParser.nodes(nodeId).toReadableString(simpleParser.grammar)
            prev match {
                case Some(p) => p.toDescs :+ desc
                case None => Seq(desc)
            }
        }

        def toDescString: String = toDescs mkString " "

        def replaceTop(replace: Int) = Stack(replace, prev)

        def append(append: Int) = Stack(append, Some(this))

        def finish(): Context = prev match {
            case Some(popped) => edgeAction(simpleParser.edgeActions(popped.nodeId -> nodeId))
            case None => SucceededContext(nodeId)
        }

        def edgeAction(edgeAction: SimpleParser.EdgeAction): Context = edgeAction match {
            case SimpleParser.DropLast(replace) => prev match {
                case Some(popped) => popped.replaceTop(replace).finish()
                case None => SucceededContext(replace)
            }
            case SimpleParser.ReplaceEdge(replacePrev, replaceLast, pF) =>
                ActiveContext(pop().replaceTop(replacePrev).append(replaceLast), pF)
        }

        def pop(): Stack = prev.get
    }

    def createStack(nodes: List[Int]): Option[Stack] = nodes match {
        case List() => None
        case init :+ last => Some(Stack(last, createStack(init)))
    }

    sealed trait Context {
        def proceed(c: Char): Context

        def proceedTermGroup(termGroup: CharacterTermGroupDesc): Context

        def proceedEof(): Context
    }

    case class SucceededContext(lastNodeId: Int) extends Context {
        def proceed(c: Char): Context = FailedContext(CharacterTermGroupDesc.empty, c)

        def proceedTermGroup(termGroup: CharacterTermGroupDesc): Context = ???

        def proceedEof(): Context = this
    }

    case class FailedContext(expected: CharacterTermGroupDesc, actual: Char) extends Context {
        def proceed(c: Char): Context = this

        def proceedTermGroup(termGroup: CharacterTermGroupDesc): Context = ???

        def proceedEof(): Context = this
    }

    case class ActiveContext(stack: Stack, pendingFinish: Option[Int]) extends Context {
        def termAction(termAction: SimpleParser.TermAction): Context = termAction match {
            case SimpleParser.Finish(replace) => stack.replaceTop(replace).finish()
            case SimpleParser.Append(replace, append, pF) =>
                ActiveContext(stack.replaceTop(replace).append(append), pF)
        }

        def applyPendingFin: Context = stack.pop().replaceTop(pendingFinish.get).finish()

        def proceed(c: Char): Context = {
            val termActions = simpleParser.termActionsByNodeId(stack.nodeId)
            termActions find (_._1.contains(c)) match {
                case Some((_, action)) => termAction(action)
                case None =>
                    if (pendingFinish.isDefined) applyPendingFin.proceed(c)
                    else FailedContext(CharacterTermGroupDesc.merge(termActions.keys), c)
            }
        }

        def proceedTermGroup(termGroup: CharacterTermGroupDesc): Context = ???

        def proceedString(string: String): Context =
            string.foldLeft(this.asInstanceOf[Context])(_ proceed _)

        def proceedEof(): Context = {
            def repeatFinish(ctx: Context): Context = ctx match {
                case ctx: ActiveContext => repeatFinish(ctx.stack.finish().proceedEof())
                case _ => ctx
            }

            repeatFinish(this)
        }
    }

    def initialContext = ActiveContext(Stack(simpleParser.startNodeId, None), None)

    def describeContext(ctx: Context, indent: String): Unit = ctx match {
        case ctx@ActiveContext(stack, pendingFinish) =>
            println(s"$indent${stack.toIdsString}  pf=${pendingFinish.getOrElse(-1)}  ${stack.toDescString}")
            val acceptableTerms = CharacterTermGroupDesc.merge(simpleParser.acceptableTermsOf(stack.nodeId))
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

object SimpleParserRunner {

    def main(args: Array[String]): Unit = {
        val parser = SimpleParserJavaGen.generateParser(SimpleGrammars.array0Grammar)
        val runner = new SimpleParserRunner(parser)
        val input = "[a  ]"
        input.foldLeft(runner.initialContext.asInstanceOf[runner.Context]) { (ctx, c) =>
            println(s"Proceed $c")
            val nextCtx = ctx.proceed(c)
            runner.describeContext(nextCtx, "")
            nextCtx
        }

        println()
        val nc1 = runner.ActiveContext(runner.createStack(List(2, 3, 16, 9)).get, None).proceed(',')
        runner.describeContext(nc1, "")
        val nc2 = runner.ActiveContext(runner.createStack(List(7, 11)).get, None).proceed(' ')
        runner.describeContext(nc2, "")
    }
}
