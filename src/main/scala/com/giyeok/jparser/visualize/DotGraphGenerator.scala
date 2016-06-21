package com.giyeok.jparser.visualize

import com.giyeok.jparser.ParsingGraph
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.ParseResult

class DotGraphGenerator[R <: ParseResult](nodeIdCache: NodeIdCache) {
    implicit class DotGraphSymbols(sym: Symbol) {
        import com.giyeok.jparser.utils.UnicodeUtil.{ toReadable, categoryCodeToName }

        def toDotSymbolName: String = ???
        def toDotLabelName: String = sym match {
            case Any => "any"
            case AnyChar => "anychar"
            case ExactChar(c) =>
                c match {
                    case ' ' => "_"
                    case c => toReadable(c)
                }
            case chars: Terminals.Chars =>
                "[" + (chars.groups map { range =>
                    if (range._1 == range._2) s"${toReadable(range._1)}"
                    else if (range._1 + 1 == range._2) s"${toReadable(range._1)}-${toReadable(range._2)}"
                    else s"${toReadable(range._1)}-${toReadable(range._2)}"
                } mkString "|") + "]"
            case Unicode(c) => s"<unicode ${(c.toSeq.sorted map { categoryCodeToName(_) }) mkString ", "}>"
            case t: Terminal => t.toDotLabelName
            case Start => "<start>"
            case s: Nonterminal => s.name
            case s: Sequence => (s.seq map { _.toDotLabelName } mkString " ")
            case s: OneOf => s.syms map { _.toDotLabelName } mkString "|"
            case Repeat(sym, lower) =>
                lower match {
                    case 0 => s"${sym.toDotLabelName}*"
                    case 1 => s"${sym.toDotLabelName}+"
                    case lower => s"${sym.toDotLabelName}[$lower-]"
                }
            case s: Except => s"${s.sym.toDotLabelName}-${s.except.toDotLabelName}"
            case LookaheadIs(lookahead) => s"la(${lookahead.toDotLabelName})"
            case LookaheadExcept(except) => s"lx(${except.toDotLabelName})"
            case Proxy(sym) => s"P(${sym.toDotLabelName})"
            case Backup(sym, backup) => s"${sym.toDotLabelName}>${backup.toDotLabelName}"
            case Join(sym, join) => s"${sym.toDotLabelName}&${join.toDotLabelName}"
            case Longest(sym) => s"L(${sym.toDotLabelName})"
            case EagerLongest(sym) => s"EL(${sym.toDotLabelName})"
        }
    }

    trait Props[This <: Props[This]] {
        val properties: scala.collection.mutable.Map[String, String]
        def attr(name: String, value: String): This = {
            properties(name) = value
            this.asInstanceOf[This]
        }
        def attrString: String = {
            properties map { kv => kv._1 + "=\"" + kv._2 + "\"" } mkString ","
        }
    }
    class DotGraphNode(val name: String, val properties: scala.collection.mutable.Map[String, String]) extends Props[DotGraphNode] {
        def this(name: String) = this(name, scala.collection.mutable.Map[String, String]())
    }

    class DotGraphEdge(val properties: scala.collection.mutable.Map[String, String]) extends Props[DotGraphEdge] {
        def this() = this(scala.collection.mutable.Map[String, String]())
    }

    private val nodes = scala.collection.mutable.ListMap[ParsingGraph.Node, DotGraphNode]()
    private val edges = scala.collection.mutable.Map[ParsingGraph.Edge, DotGraphEdge]()

    def nodeNameOf(node: ParsingGraph.Node): String = s"node${nodeIdCache.of(node)}"

    def labelOf(node: ParsingGraph.Node): String = {
        node match {
            case node @ ParsingGraph.TermNode(symbol, beginGen) =>
                symbol.toDotLabelName + "," + beginGen
            case node @ ParsingGraph.AtomicNode(symbol, beginGen) =>
                symbol.toDotLabelName + "," + beginGen
            case node @ ParsingGraph.SequenceNode(Sequence(seq, whitespace), pointer, beginGen, endGen) =>
                val split = seq.splitAt(pointer)
                (split._1 map { _.toDotLabelName } mkString " ") + "&bull;" + (split._2 map { _.toDotLabelName } mkString " ") + "," + s"$beginGen&#x2025;$endGen"
        }
    }

    def addNode(node: ParsingGraph.Node): DotGraphNode = {
        val dotnode = new DotGraphNode(nodeNameOf(node)).attr("label", labelOf(node))
        node match {
            case node: ParsingGraph.SequenceNode =>
                dotnode.attr("shape", "rectangle")
            case _ =>
        }
        nodes(node) = dotnode
        dotnode
    }

    def addGraph(graph: ParsingGraph[R]): DotGraphGenerator[R] = {
        var visited = Set[ParsingGraph.Node]()
        var queue = List[ParsingGraph.Node]()
        def traverseNode(): Unit = {
            val node = queue.head
            queue = queue.tail
            addNode(node)
            graph.outgoingSimpleEdgesFrom(node).toSeq sortWith { (x, y) =>
                (x.end, y.end) match {
                    case (x: ParsingGraph.TermNode, y: ParsingGraph.TermNode) => labelOf(x) < labelOf(y)
                    case (x: ParsingGraph.TermNode, _) => true
                    case (x: ParsingGraph.AtomicNode, y: ParsingGraph.AtomicNode) => labelOf(x) < labelOf(y)
                    case (x: ParsingGraph.AtomicNode, y: ParsingGraph.TermNode) => false
                    case (x: ParsingGraph.AtomicNode, _) => true
                    case (x: ParsingGraph.SequenceNode, y: ParsingGraph.SequenceNode) => labelOf(x) < labelOf(y)
                    case _ => false
                }
            } foreach {
                case edge @ ParsingGraph.SimpleEdge(start, end, cond) =>
                    if (!(visited contains end)) queue :+= end
                    visited += end
            }
            graph.outgoingJoinEdgesFrom(node) foreach {
                case edge @ ParsingGraph.JoinEdge(start, end, join) =>
                    if (!(visited contains end)) queue :+= end
                    if (!(visited contains join)) queue :+= join
                    queue :+ join
                    visited += end
                    visited += join
            }
            if (!queue.isEmpty) {
                traverseNode()
            }
        }
        visited += ParsingGraph.AtomicNode(Start, 0)
        queue +:= ParsingGraph.AtomicNode(Start, 0)
        traverseNode()

        graph.edges foreach { edge =>
            edges(edge) = new DotGraphEdge()
        }
        this
    }

    def addTransition(baseGraph: ParsingGraph[R], afterGraph: ParsingGraph[R]): DotGraphGenerator[R] = {
        // baseGraph -> afterGraph 과정에서 없어진 노드/엣지 스타일에 dotted 추가
        (baseGraph.nodes -- afterGraph.nodes) foreach { removedNode =>
            nodes(removedNode).attr("style", "dotted")
        }
        (baseGraph.edges -- afterGraph.edges) foreach { removedEdge =>
            edges(removedEdge).attr("style", "dotted")
        }
        // 새로 추가된 노드/엣지 파란색으로 표시
        (afterGraph.nodes -- baseGraph.nodes) foreach { node =>
            addNode(node).attr("style", "filled")
        }
        (afterGraph.edges -- baseGraph.edges) foreach { edge =>
            if (!(edges contains edge)) {
                edges(edge) = new DotGraphEdge()
            }
        }
        this
    }

    def printDotGraph(): Unit = {
        println("digraph G {")
        println("    node[fontname=\"monospace\", height=.1];")
        nodes foreach { kv =>
            if (kv._1.symbol != Start) {
                println(s"    ${nodeNameOf(kv._1)}[${kv._2.attrString}];")
            }
        }
        println()
        edges foreach { kv =>
            kv._1 match {
                case edge @ ParsingGraph.SimpleEdge(start, end, cond) =>
                    if (start.symbol != Start) {
                        println(s"    ${nodeNameOf(start)} -> ${nodeNameOf(end)}[${kv._2.attrString}];")
                    }
                case edge @ ParsingGraph.JoinEdge(start, end, join) =>
                    println(s"    ${nodeNameOf(start)}proxy" + "[shape=\"point\", width=\"0.1\", height=\"0\"];")
                    println(s"    ${nodeNameOf(start)} -> ${nodeNameOf(start)}proxy[dir=none,${kv._2.attrString}];")
                    println(s"    ${nodeNameOf(start)}proxy -> ${nodeNameOf(end)}[${kv._2.attrString}];")
                    println(s"    ${nodeNameOf(start)}proxy -> ${nodeNameOf(join)}[${kv._2.attrString}];")
            }
        }
        println("}")
    }

}
