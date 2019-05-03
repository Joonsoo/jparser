package com.giyeok.jparser.parsergen.nocond

class SimpleParserRunner(val simpleParser: SimpleParser, val verbose: Boolean) {

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
    }

    case class Context(stack: Stack, pendingFinish: Option[Int]) {
        def proceed(c: Char): Context = {
            ???
        }

        def proceed(string: String): Context =
            string.foldLeft(this)(_ proceed _)

        def proceedEof: Context = {
            ???
        }
    }

    def initialContext = Context(Stack(simpleParser.startNodeId, None), None)
}
