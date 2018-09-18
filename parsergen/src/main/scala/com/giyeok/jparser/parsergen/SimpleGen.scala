package com.giyeok.jparser.parsergen

import java.io.{File, PrintWriter}

import com.giyeok.jparser.Inputs.{CharacterTermGroupDesc, CharsGroup, CharsGrouping, TermGroupDesc}
import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.parsergen.SimpleGen._
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}
import com.google.googlejavaformat.java.Formatter

object SimpleGen {

    sealed trait Action

    case class Append(appendNodeType: Int, pendingFinish: Boolean = false) extends Action

    case class ReplaceAndAppend(replaceNodeType: Int, appendNodeType: Int, pendingFinish: Boolean) extends Action

    case object Finish extends Action

    case class ReplaceAndFinish(replaceNodeType: Int) extends Action

}

object Topology {

    sealed trait Edge extends AbstractEdge[Int]

    case class AppendEdge(start: Int, end: Int)(val pendingFinish: Boolean) extends Edge

    case class ReplaceEdge(start: Int, end: Int) extends Edge

    class Graph(val nodes: Set[Int], val edges: Set[Edge], val edgesByStart: Map[Int, Set[Edge]], val edgesByEnd: Map[Int, Set[Edge]])
        extends AbstractGraph[Int, Edge, Graph] {
        override def createGraph(nodes: Set[Int], edges: Set[Edge], edgesByStart: Map[Int, Set[Edge]], edgesByEnd: Map[Int, Set[Edge]]): Graph =
            new Graph(nodes, edges, edgesByStart, edgesByEnd)

        def findAppendEdge(start: Int, end: Int): Option[AppendEdge] =
            (edges find {
                case Topology.AppendEdge(`start`, `end`) => true
                case _ => false
            }) map { e => e.asInstanceOf[AppendEdge] }
    }

    class Builder {
        private var _graph: Graph = new Graph(Set(), Set(), Map(), Map())

        def graph(): Graph = _graph

        // TODO AppendEdge(x -> y)와 ReplaceEdge(y -> z)가 있으면 AppendEdge(x -> z)도 추가하고 함께 반환
        private def addEdges(edges: Edge*): List[Edge] = {
            val newEdges = edges filterNot _graph.edges.contains

            _graph = newEdges.foldLeft(_graph) { (g, e) => g.addEdgeSafe(e) }

            newEdges.toList
        }

        def addTermAction(baseNodeType: Int, action: SimpleGen.Action): List[Edge] =
            action match {
                case SimpleGen.Append(appendNodeType, pendingFinish) =>
                    addEdges(AppendEdge(baseNodeType, appendNodeType)(pendingFinish))
                case SimpleGen.ReplaceAndAppend(replaceNodeType, appendNodeType, pendingFinish) =>
                    addEdges(ReplaceEdge(baseNodeType, replaceNodeType),
                        AppendEdge(replaceNodeType, appendNodeType)(pendingFinish))
                case SimpleGen.Finish => List()
                case SimpleGen.ReplaceAndFinish(replaceNodeType) =>
                    addEdges(ReplaceEdge(baseNodeType, replaceNodeType))
            }

        def addImpliedEdge(original: (Int, Int), implied: (Int, Int, Boolean)): List[Edge] = {
            // (originalStart -> originalEnd) append edge는 원래 있어야 함
            assert(_graph.findAppendEdge(original._1, original._2).isDefined)
            // (originalStart -> impliedStart) replace edge가 필요하면 넣고 둘이 같으면 무시
            // (impliedStart -> impliedEnd) append edge 추가, (impliedStart -> impliedEnd) append edge는 원래 없었어야 함
            assert(_graph.findAppendEdge(implied._1, implied._2).isEmpty)
            if (original._1 != implied._1) {
                addEdges(ReplaceEdge(original._1, implied._1), AppendEdge(implied._1, implied._2)(implied._3))
            } else {
                addEdges(AppendEdge(implied._1, implied._2)(implied._3))
            }
        }
    }

}

// grammar and nodes are debugging purpose
class SimpleGen(val grammar: NGrammar,
                val nodes: Map[Int, Set[AKernel]],
                val startNodeId: Int,
                val termActions: Map[(Int, CharacterTermGroupDesc), Action],
                // 엣지가 finish되면 새로 붙어야 하는 node
                val topologyGraph: Topology.Graph,
                val impliedNodes: Map[(Int, Int), Option[(Int, Int, Boolean)]]) {
    def genJava(pkgName: String, className: String, testStr: Option[String] = None): String = {
        val impliedNodesIfStack = ((impliedNodes.toList.sortBy { p => p._1 } map { impliedNode =>
            val impliedEdge = impliedNode._1
            val firstLine = s"if (prevNodeType == ${impliedEdge._1} && lastNodeType == ${impliedEdge._2}) {\n"
            val lastLine = "}"
            val body = impliedNode._2 match {
                case Some((newStart, implied, pendingFinish)) if newStart == impliedEdge._1 =>
                    s"""last = new Node($implied, last.parent);
                       |pendingFinish = $pendingFinish;
                     """.stripMargin
                case Some((newStart, implied, pendingFinish)) =>
                    s"""last = new Node($implied, new Node($newStart, last.parent.parent));
                       |pendingFinish = $pendingFinish;
                     """.stripMargin
                case None =>
                    """last = last.parent;
                      |finish();
                    """.stripMargin
            }
            firstLine + body + lastLine
        }) :+
            s"""throw new RuntimeException("Unknown edge, " + prevNodeType + " -> " + lastNodeType + ", " + nodeDesc(prevNodeType) + " -> " + nodeDesc(lastNodeType));""") mkString " else "

        def javaChar(c: Char) = c match {
            case '\n' => "\\n"
            case '\r' => "\\r"
            case '\t' => "\\t"
            case '\\' => "\\\\"
            // TODO finish
            case c => c.toString
        }

        def javaString(str: String) =
            str.replaceAllLiterally("\\", "\\\\")
                .replaceAllLiterally("\"", "\\\"")

        def charsToCondition(chars: Set[Char], varName: String): String =
            chars.groups map { group =>
                if (group._1 == group._2) s"($varName == '${javaChar(group._1)}')"
                else s"('${javaChar(group._1)}' <= $varName && $varName <= '${javaChar(group._2)}')"
            } mkString " || "

        def charGroupToCondition(charsGroup: CharsGroup, varName: String): String =
            charsToCondition(charsGroup.chars, varName)

        implicit def termGroupOrdering[A <: TermGroupDesc]: Ordering[A] = (x: A, y: A) => {
            (x, y) match {
                case (xx: CharsGroup, yy: CharsGroup) =>
                    xx.chars.min - yy.chars.min
            }
        }

        val nodeTermActions = (termActions groupBy { p => p._1._1 }).toList.sortBy { p => p._1 } map { p =>
            p._1 -> (p._2 map { pp => pp._1._2 -> pp._2 }).toList.sortBy { p3 => p3._1 }
        }

        val proceed1Stack = nodeTermActions map { nodeActions =>
            val (nodeTypeId, actions) = nodeActions
            val firstLine = s"case $nodeTypeId: // ${nodes(nodeTypeId) map { k => k.toReadableString(grammar) } mkString "|"}\n"
            val body = actions map { termAction =>
                val (charsGroup, action) = termAction
                val firstLine = s"if (${charGroupToCondition(charsGroup.asInstanceOf[CharsGroup], "next")}) {"
                val body = action match {
                    case Append(appendNodeType, pendingFinish) => s"append($appendNodeType, $pendingFinish);"
                    case ReplaceAndAppend(replaceNodeType, appendNodeType, pendingFinish) => s"replace($replaceNodeType); append($appendNodeType, $pendingFinish);"
                    case Finish => "finish();"
                    case ReplaceAndFinish(replaceNodeType) => s"replace($replaceNodeType); finish();"
                }
                val lastLine = "return true;\n}"
                firstLine + "\n" + body + "\n" + lastLine
            } mkString " else "
            val lastLine = "break;"
            firstLine + body + lastLine
        } mkString "\n"

        val canHandleStack = nodeTermActions map { nodeActions =>
            val (nodeTypeId, termActions) = nodeActions
            val condition = charsToCondition((termActions flatMap { p => p._1.asInstanceOf[CharsGroup].chars }).toSet, "c")
            s"case $nodeTypeId: return $condition;"
        } mkString "\n"

        val nodeDescriptionStack = nodes.toList.sortBy(_._1) map { node =>
            val description = node._2 map { k => javaString(k.toReadableString(grammar, "\u2022")) } mkString "|"
            s"""case ${node._1}: return "{${javaString(description)}}";"""
        } mkString "\n"

        val mainStack = testStr match {
            case Some(value) =>
                s"""public static void main(String[] args) {
                   |  $className parser = new $className();
                   |  if (parser.proceed("${javaString(value)}")) {
                   |    boolean result = parser.eof();
                   |    System.out.println(result);
                   |  } else {
                   |    System.out.println("Failed");
                   |  }
                   |}""".stripMargin
            case None => ""
        }

        s"""package $pkgName;
           |
           |public class $className {
           |  static class Node {
           |    public final int nodeTypeId;
           |    public final Node parent;
           |
           |    public Node(int nodeTypeId, Node parent) {
           |      this.nodeTypeId = nodeTypeId;
           |      this.parent = parent;
           |    }
           |  }
           |
           |  private int location;
           |  private Node last;
           |  private boolean pendingFinish;
           |
           |  public $className() {
           |    last = new Node($startNodeId, null);
           |  }
           |
           |  private boolean canHandle(int nodeTypeId, char c) {
           |    switch (nodeTypeId) {
           |    $canHandleStack
           |    }
           |    throw new RuntimeException("Unknown nodeTypeId=" + nodeTypeId);
           |  }
           |
           |  private void append(int newNodeType, boolean pendingFinish) {
           |    last = new Node(newNodeType, last);
           |    this.pendingFinish = pendingFinish;
           |  }
           |
           |  private void replace(int newNodeType) {
           |    last = new Node(newNodeType, last.parent);
           |    this.pendingFinish = false;
           |  }
           |
           |  private void finish() {
           |    System.out.println(nodeString() + " " + nodeDescString());
           |    int prevNodeType = last.parent.nodeTypeId, lastNodeType = last.nodeTypeId;
           |
           |    $impliedNodesIfStack
           |  }
           |
           |  private boolean tryFinishable(char next) {
           |    if (pendingFinish) {
           |      while (pendingFinish) {
           |        last = last.parent;
           |        finish();
           |        if (canHandle(last.nodeTypeId, next)) {
           |          return proceed1(next);
           |        }
           |      }
           |      return proceed1(next);
           |    } else {
           |      return false;
           |    }
           |  }
           |
           |  private boolean proceed1(char next) {
           |    switch (last.nodeTypeId) {
           |      $proceed1Stack
           |    }
           |    return tryFinishable(next);
           |  }
           |
           |  private String nodeString(Node node) {
           |    if (node.parent == null) return "" + node.nodeTypeId;
           |    else return nodeString(node.parent) + " " + node.nodeTypeId;
           |  }
           |
           |  public String nodeString() {
           |    return nodeString(last);
           |  }
           |
           |  public String nodeDesc(int nodeTypeId) {
           |    switch (nodeTypeId) {
           |      $nodeDescriptionStack
           |    }
           |    throw new RuntimeException("Unknown nodeTypeId=" + nodeTypeId);
           |  }
           |
           |  private String nodeDescString(Node node) {
           |    if (node.parent == null) return "" + nodeDesc(node.nodeTypeId);
           |    else return nodeDescString(node.parent) + " " + nodeDesc(node.nodeTypeId);
           |  }
           |
           |  public String nodeDescString() {
           |    return nodeDescString(last);
           |  }
           |
           |  public boolean proceed(char next) {
           |    location += 1;
           |    return proceed1(next);
           |  }
           |
           |  public boolean proceed(String next) {
           |    for (int i = 0; i < next.length(); i++) {
           |      System.out.println(nodeString() + " " + nodeDescString());
           |      System.out.println(i + " " + next.charAt(i));
           |      if (!proceed(next.charAt(i))) {
           |        return false;
           |      }
           |    }
           |    return true;
           |  }
           |
           |  public boolean eof() {
           |    while (pendingFinish) {
           |      last = last.parent;
           |      if (last.nodeTypeId == 1) {
           |        return pendingFinish;
           |      }
           |      finish();
           |    }
           |    return false;
           |  }
           |  $mainStack
           |}
        """.stripMargin
    }

    def genFormattedJava(pkgName: String, className: String, testStr: Option[String] = None): String =
        new Formatter().formatSource(genJava(pkgName, className, testStr))

    def writeFormattedJavaTo(path: String, pkgName: String, className: String, testStr: Option[String] = None): Unit = {
        val writer = new PrintWriter(new File(path))
        writer.write(genFormattedJava(pkgName, className, testStr))
        writer.close()
    }
}

object SimpleGenMain {
    def charsGroup(c: Char): CharacterTermGroupDesc =
        CharsGroup(Set(), Set(), Set(c))

    def charsGroup(start: Char, end: Char): CharacterTermGroupDesc =
        CharsGroup(Set(), Set(), (start to end).toSet)

    //    def expr(args: Array[String]): Unit = {
    //        val grammar = NGrammar.fromGrammar(ExpressionGrammars.simple)
    //        val nodes: Map[Int, Set[AKernel]] = Map(
    //            1 -> Set(AKernel(1, 0)),
    //            2 -> Set(AKernel(16, 1), AKernel(18, 1)),
    //            3 -> Set(AKernel(7, 1), AKernel(16, 1), AKernel(18, 1)),
    //            4 -> Set(AKernel(13, 1)),
    //            5 -> Set(AKernel(11, 1)),
    //            6 -> Set(AKernel(16, 2)),
    //            7 -> Set(AKernel(18, 2)),
    //            8 -> Set(AKernel(7, 1)),
    //            9 -> Set(AKernel(16, 1)),
    //            10 -> Set(AKernel(7, 1), AKernel(16, 1)),
    //            11 -> Set(AKernel(18, 1)),
    //            12 -> Set(AKernel(13, 2)),
    //            13 -> Set(AKernel(11, 1), AKernel(16, 1)))
    //        val termActions: Map[(Int, CharacterTermGroupDesc), Action] = Map(
    //            (1, charsGroup('0')) -> Append(2, pendingFinish = true),
    //            (1, charsGroup('1', '9')) -> Append(3, pendingFinish = true),
    //            (1, charsGroup('(')) -> Append(4),
    //            (2, charsGroup('*')) -> Replace(6),
    //            (2, charsGroup('+')) -> Replace(7),
    //            (3, charsGroup('0', '9')) -> ReplaceAndAppend(8, 5, pendingFinish = true),
    //            (3, charsGroup('*')) -> Replace(6),
    //            (3, charsGroup('+')) -> Replace(7),
    //            (4, charsGroup('0')) -> Append(2),
    //            (4, charsGroup('1', '9')) -> Append(3),
    //            (4, charsGroup('(')) -> Append(4),
    //            (5, charsGroup('0', '9')) -> Finish,
    //            (6, charsGroup('0')) -> Finish,
    //            (6, charsGroup('1', '9')) -> Append(8, pendingFinish = true),
    //            (6, charsGroup('(')) -> Append(4),
    //            (7, charsGroup('0')) -> Append(9, pendingFinish = true),
    //            (7, charsGroup('1', '9')) -> Append(10, pendingFinish = true),
    //            (7, charsGroup('(')) -> Append(4),
    //            (8, charsGroup('0', '9')) -> Append(5, pendingFinish = true),
    //            (9, charsGroup('*')) -> Replace(6),
    //            (10, charsGroup('0', '9')) -> ReplaceAndAppend(8, 5, pendingFinish = true),
    //            (10, charsGroup('*')) -> Replace(6),
    //            (11, charsGroup('+')) -> Replace(7),
    //            (12, charsGroup(')')) -> Finish,
    //            (13, charsGroup('*')) -> Replace(6),
    //            (13, charsGroup('0', '9')) -> Finish)
    //        val impliedNodes: Map[(Int, Int), Option[(Int, Int, Boolean)]] = Map(
    //            (1, 6) -> Some(1, 2, true),
    //            (1, 7) -> Some(1, 2, true),
    //            (1, 8) -> Some(1, 3, true),
    //            (1, 12) -> Some(1, 2, true),
    //            (4, 6) -> Some(4, 2, true),
    //            (4, 7) -> Some(4, 11, true),
    //            (4, 11) -> None,
    //            (4, 8) -> Some(4, 3, true),
    //            (6, 8) -> None,
    //            (7, 10) -> None,
    //            (7, 12) -> Some(7, 9, true),
    //            (7, 6) -> Some(7, 9, true),
    //            (1, 4) -> Some(1, 12, false),
    //            (4, 4) -> Some(4, 12, false),
    //            (4, 12) -> Some(4, 2, true),
    //            (6, 4) -> Some(6, 12, false),
    //            (6, 12) -> None,
    //            (7, 4) -> Some(7, 12, false),
    //            (7, 5) -> Some(7, 13, true),
    //            (7, 8) -> Some(7, 9, true),
    //            (8, 5) -> Some(8, 5, true),
    //            (7, 9) -> Some(7, 9, false))
    //        val rule = new SimpleGen(grammar, nodes, 1, termActions, Map(), impliedNodes)
    //        println(rule.genJava("com.giyeok.jparser.parsergen.generated", "ExprGrammarSimpleParser"))
    //    }

    def main(args: Array[String]): Unit = {
        val gram = MetaGrammar.translateForce("abcde",
            """S = A 'a' B | C D E F
              |A = 'x'
              |C = 'x'
              |B = 'b'
              |E = 'e'
              |F = 'f'
              |D = 'a'+
            """.stripMargin)
        val grammar = NGrammar.fromGrammar(gram)
        val nodes: Map[Int, Set[AKernel]] = Map(
            1 -> Set(AKernel(1, 0)))
        val termActions: Map[(Int, CharacterTermGroupDesc), Action] = Map(
            (1, charsGroup('0')) -> Append(2, pendingFinish = true))
        val impliedNodes: Map[(Int, Int), Option[(Int, Int, Boolean)]] = Map(
            (1, 6) -> Some(1, 2, true))
        val rule = new SimpleGen(grammar, nodes, 1, termActions, new Topology.Graph(Set(), Set(), Map(), Map()), impliedNodes)
        println(rule.genJava("com.giyeok.jparser.parsergen.generated", "ExprGrammarSimpleParser"))
    }
}
