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
            case None => FinishedContext(true)
        }

        def edgeAction(edgeAction: SimpleParser.EdgeAction): Context = edgeAction match {
            case SimpleParser.DropLast(replace) => prev match {
                case Some(popped) => popped.replaceTop(replace).finish()
                case None => FinishedContext(true)
            }
            case SimpleParser.ReplaceEdge(replacePrev, replaceLast, pF) =>
                ActiveContext(pop().replaceTop(replacePrev).append(replaceLast), pF)
        }

        def pop(): Stack = prev.get
    }

    sealed trait Context {
        def proceed(c: Char): Context

        def proceedEof(): Context
    }

    case class FinishedContext(succeeded: Boolean) extends Context {
        override def proceed(c: Char): Context = FinishedContext(false)

        override def proceedEof(): Context = this
    }

    case class ActiveContext(stack: Stack, pendingFinish: Option[Int]) extends Context {
        def termAction(termAction: SimpleParser.TermAction): Context = termAction match {
            case SimpleParser.Finish(replace) => stack.replaceTop(replace).finish()
            case SimpleParser.Append(replace, append, pF) =>
                ActiveContext(stack.replaceTop(replace).append(append), pF)
        }

        def applyPendingFin: Context = stack.pop().finish()

        override def proceed(c: Char): Context = {
            simpleParser.termActionsByNodeId(stack.nodeId) find (_._1.contains(c)) match {
                case Some((_, action)) => termAction(action)
                case None =>
                    if (pendingFinish.isDefined) applyPendingFin.proceed(c)
                    else FinishedContext(false)
            }
        }

        def proceed(string: String): Context =
            string.foldLeft(this.asInstanceOf[Context])(_ proceed _)

        override def proceedEof(): Context = {
            def repeatFinish(ctx: Context): Context = ctx match {
                case ctx: ActiveContext => repeatFinish(ctx.stack.finish().proceedEof())
                case _: FinishedContext => ctx
            }

            repeatFinish(this)
        }
    }

    def initialContext = ActiveContext(Stack(simpleParser.startNodeId, None), None)
}

object SimpleParserRunner {
    def main(args: Array[String]): Unit = {
        val parser = SimpleParserJavaGen.generateParser(SimpleGrammars.array0Grammar)
        val runner = new SimpleParserRunner(parser)
        val input = "[a,a,a]"
        input.foldLeft(runner.initialContext.asInstanceOf[runner.Context]) { (ctx, c) =>
            println(s"Proceed $c")
            val nextCtx = ctx.proceed(c)

            def printCtx(ctx: SimpleParserRunner#Context, indent: String): Unit = ctx match {
                case ctx@runner.ActiveContext(stack, pendingFinish) =>
                    println(s"$indent${stack.toIdsString}  pf=${pendingFinish.getOrElse(-1)}  ${stack.toDescString}")
                    val acceptableTerms = CharacterTermGroupDesc.merge(parser.acceptableTermsOf(stack.nodeId))
                    println(s"${indent}acceptable=${acceptableTerms.toShortString}")
                    if (pendingFinish.isDefined) {
                        printCtx(ctx.applyPendingFin, indent + "  ")
                    }
                case runner.FinishedContext(succeeded) =>
                    println(s"${indent}finished - ${if (succeeded) "succeeded" else "failed"}")
            }

            printCtx(nextCtx, "")

            nextCtx
        }
    }
}
