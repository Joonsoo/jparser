package com.giyeok.jparser.metalang2.generated

import com.giyeok.jparser.Inputs.InputToShortString
import com.giyeok.jparser.ParseResultTree.{JoinNode, Node, BindNode, TerminalNode, SequenceNode}
import com.giyeok.jparser.nparser.{ParseTreeConstructor, NaiveParser, Parser}
import com.giyeok.jparser.{NGrammar, ParsingErrors, ParseForestFunc, Symbols}
import scala.collection.immutable.ListSet

object PyObjGrammar {
  val ngrammar = new NGrammar(
  Map(1 -> NGrammar.NStart(1, 2),
2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("PyObj"), Set(3)),
4 -> NGrammar.NTerminal(4, Symbols.ExactChar('{')),
5 -> NGrammar.NNonterminal(5, Symbols.Nonterminal("WS"), Set(6)),
7 -> NGrammar.NRepeat(7, Symbols.Repeat(Symbols.Chars(Set(' ') ++ ('\t' to '\n').toSet), 0), 8, 9),
10 -> NGrammar.NTerminal(10, Symbols.Chars(Set(' ') ++ ('\t' to '\n').toSet)),
11 -> NGrammar.NOneOf(11, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ObjField"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ObjField")))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))),Symbols.Nonterminal("WS")))))), Set(12,13)),
12 -> NGrammar.NProxy(12, Symbols.Proxy(Symbols.Sequence(Seq())), 8),
13 -> NGrammar.NProxy(13, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ObjField"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ObjField")))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))),Symbols.Nonterminal("WS")))), 14),
15 -> NGrammar.NNonterminal(15, Symbols.Nonterminal("ObjField"), Set(16)),
17 -> NGrammar.NNonterminal(17, Symbols.Nonterminal("StrLiteral"), Set(18,25)),
19 -> NGrammar.NTerminal(19, Symbols.ExactChar('"')),
20 -> NGrammar.NRepeat(20, Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), 8, 21),
22 -> NGrammar.NNonterminal(22, Symbols.Nonterminal("StrChar"), Set(23)),
24 -> NGrammar.NTerminal(24, Symbols.Chars(Set(' ','_','|') ++ ('0' to '9').toSet ++ ('<' to '>').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
26 -> NGrammar.NTerminal(26, Symbols.ExactChar('\'')),
27 -> NGrammar.NTerminal(27, Symbols.ExactChar(':')),
28 -> NGrammar.NNonterminal(28, Symbols.Nonterminal("Value"), Set(29,45,54,55,71)),
30 -> NGrammar.NNonterminal(30, Symbols.Nonterminal("BoolValue"), Set(31,38)),
32 -> NGrammar.NProxy(32, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('T'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))), 33),
34 -> NGrammar.NTerminal(34, Symbols.ExactChar('T')),
35 -> NGrammar.NTerminal(35, Symbols.ExactChar('r')),
36 -> NGrammar.NTerminal(36, Symbols.ExactChar('u')),
37 -> NGrammar.NTerminal(37, Symbols.ExactChar('e')),
39 -> NGrammar.NProxy(39, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('F'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))), 40),
41 -> NGrammar.NTerminal(41, Symbols.ExactChar('F')),
42 -> NGrammar.NTerminal(42, Symbols.ExactChar('a')),
43 -> NGrammar.NTerminal(43, Symbols.ExactChar('l')),
44 -> NGrammar.NTerminal(44, Symbols.ExactChar('s')),
46 -> NGrammar.NNonterminal(46, Symbols.Nonterminal("IntLiteral"), Set(47,49)),
48 -> NGrammar.NTerminal(48, Symbols.ExactChar('0')),
50 -> NGrammar.NTerminal(50, Symbols.Chars(('1' to '9').toSet)),
51 -> NGrammar.NRepeat(51, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 8, 52),
53 -> NGrammar.NTerminal(53, Symbols.Chars(('0' to '9').toSet)),
56 -> NGrammar.NNonterminal(56, Symbols.Nonterminal("ListValue"), Set(57)),
58 -> NGrammar.NTerminal(58, Symbols.ExactChar('[')),
59 -> NGrammar.NOneOf(59, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Value"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))),Symbols.Nonterminal("WS")))))), Set(12,60)),
60 -> NGrammar.NProxy(60, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Value"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))),Symbols.Nonterminal("WS")))), 61),
62 -> NGrammar.NRepeat(62, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))), 0), 8, 63),
64 -> NGrammar.NProxy(64, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))), 65),
66 -> NGrammar.NTerminal(66, Symbols.ExactChar(',')),
67 -> NGrammar.NOneOf(67, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))), Set(12,68)),
68 -> NGrammar.NProxy(68, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))), 69),
70 -> NGrammar.NTerminal(70, Symbols.ExactChar(']')),
72 -> NGrammar.NNonterminal(72, Symbols.Nonterminal("TupleValue"), Set(73,76)),
74 -> NGrammar.NTerminal(74, Symbols.ExactChar('(')),
75 -> NGrammar.NTerminal(75, Symbols.ExactChar(')')),
77 -> NGrammar.NRepeat(77, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))), 1), 64, 78),
79 -> NGrammar.NRepeat(79, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ObjField")))), 0), 8, 80),
81 -> NGrammar.NProxy(81, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ObjField")))), 82),
83 -> NGrammar.NTerminal(83, Symbols.ExactChar('}'))),
  Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ObjField"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ObjField")))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))),Symbols.Nonterminal("WS")))))),Symbols.ExactChar('}'))), Seq(4,5,11,83)),
6 -> NGrammar.NSequence(6, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set(' ') ++ ('\t' to '\n').toSet), 0))), Seq(7)),
8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq()), Seq()),
9 -> NGrammar.NSequence(9, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set(' ') ++ ('\t' to '\n').toSet), 0),Symbols.Chars(Set(' ') ++ ('\t' to '\n').toSet))), Seq(7,10)),
14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq(Symbols.Nonterminal("ObjField"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ObjField")))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))),Symbols.Nonterminal("WS"))), Seq(15,79,67,5)),
16 -> NGrammar.NSequence(16, Symbols.Sequence(Seq(Symbols.Nonterminal("StrLiteral"),Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value"))), Seq(17,5,27,5,28)),
18 -> NGrammar.NSequence(18, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0),Symbols.ExactChar('"'))), Seq(19,20,19)),
21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0),Symbols.Nonterminal("StrChar"))), Seq(20,22)),
23 -> NGrammar.NSequence(23, Symbols.Sequence(Seq(Symbols.Chars(Set(' ','_','|') ++ ('0' to '9').toSet ++ ('<' to '>').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(24)),
25 -> NGrammar.NSequence(25, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0),Symbols.ExactChar('\''))), Seq(26,20,26)),
29 -> NGrammar.NSequence(29, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolValue"))), Seq(30)),
31 -> NGrammar.NSequence(31, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('T'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))))), Seq(32)),
33 -> NGrammar.NSequence(33, Symbols.Sequence(Seq(Symbols.ExactChar('T'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e'))), Seq(34,35,36,37)),
38 -> NGrammar.NSequence(38, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('F'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))))), Seq(39)),
40 -> NGrammar.NSequence(40, Symbols.Sequence(Seq(Symbols.ExactChar('F'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e'))), Seq(41,42,43,44,37)),
45 -> NGrammar.NSequence(45, Symbols.Sequence(Seq(Symbols.Nonterminal("IntLiteral"))), Seq(46)),
47 -> NGrammar.NSequence(47, Symbols.Sequence(Seq(Symbols.ExactChar('0'))), Seq(48)),
49 -> NGrammar.NSequence(49, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(50,51)),
52 -> NGrammar.NSequence(52, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0),Symbols.Chars(('0' to '9').toSet))), Seq(51,53)),
54 -> NGrammar.NSequence(54, Symbols.Sequence(Seq(Symbols.Nonterminal("StrLiteral"))), Seq(17)),
55 -> NGrammar.NSequence(55, Symbols.Sequence(Seq(Symbols.Nonterminal("ListValue"))), Seq(56)),
57 -> NGrammar.NSequence(57, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Value"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(']'))), Seq(58,5,59,70)),
61 -> NGrammar.NSequence(61, Symbols.Sequence(Seq(Symbols.Nonterminal("Value"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))),Symbols.Nonterminal("WS"))), Seq(28,62,67,5)),
63 -> NGrammar.NSequence(63, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))))), Seq(62,64)),
65 -> NGrammar.NSequence(65, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value"))), Seq(5,66,5,28)),
69 -> NGrammar.NSequence(69, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','))), Seq(5,66)),
71 -> NGrammar.NSequence(71, Symbols.Sequence(Seq(Symbols.Nonterminal("TupleValue"))), Seq(72)),
73 -> NGrammar.NSequence(73, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value"),Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(74,5,28,5,66,5,75)),
76 -> NGrammar.NSequence(76, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))), 1),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(74,5,28,77,67,5,75)),
78 -> NGrammar.NSequence(78, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))), 1),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Value")))))), Seq(77,64)),
80 -> NGrammar.NSequence(80, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ObjField")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ObjField")))))), Seq(79,81)),
82 -> NGrammar.NSequence(82, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ObjField"))), Seq(5,66,5,15))),
  1)

  sealed trait ASTNode { val astNode: Node }
case class PyObj(astNode:Node, fields:Option[List[ObjField]]) extends ASTNode
case class ObjField(astNode:Node, name:StrLiteral, value:Value) extends ASTNode
case class StrLiteral(astNode:Node, value:List[Node]) extends ASTNode
sealed trait Value extends ASTNode
case class BoolValue(astNode:Node, value:Node) extends ASTNode with Value
case class IntValue(astNode:Node, value:IntLiteral) extends ASTNode with Value
case class StrValue(astNode:Node, value:StrLiteral) extends ASTNode with Value
case class ListValue(astNode:Node, elems:Option[List[Value]]) extends ASTNode with Value
case class TupleValue(astNode:Node, elems:List[Value]) extends ASTNode with Value
case class IntLiteral(astNode:Node, value:List[Node]) extends ASTNode
implicit class SourceTextOfNode(node: Node) {
  def sourceText: String = node match {
    case TerminalNode(_, input) => input.toRawString
    case BindNode(_, body) => body.sourceText
    case JoinNode(_, body, _) => body.sourceText
    case seq: SequenceNode => seq.children map (_.sourceText) mkString ""
    case _ => throw new Exception("Cyclic bind")
  }
}

def matchPyObj(node: Node): PyObj = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 3 =>
val v1 = body.asInstanceOf[SequenceNode].children(2)
val v6 = unrollOptional(v1, 12, 13) map { n =>
val BindNode(v2, v3) = n
assert(v2.id == 13)
val BindNode(v4, v5) = v3
assert(v4.id == 14)
v5
}
val v24 = v6 map { n =>
val v7 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v8, v9) = v7
assert(v8.id == 15)
val v10 = matchObjField(v9)
val v11 = List(v10)
val v12 = n.asInstanceOf[SequenceNode].children(1)
val v17 = unrollRepeat0(v12) map { n =>
val BindNode(v13, v14) = n
assert(v13.id == 81)
val BindNode(v15, v16) = v14
assert(v15.id == 82)
v16
}
val v22 = v17 map { n =>
val v18 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v19, v20) = v18
assert(v19.id == 15)
val v21 = matchObjField(v20)
v21
}
val v23 = v11 ++ v22
v23
}
val v25 = PyObj(node,v24)
v25
  }
}
def matchObjField(node: Node): ObjField = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 16 =>
val v26 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v27, v28) = v26
assert(v27.id == 17)
val v29 = matchStrLiteral(v28)
val v30 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v31, v32) = v30
assert(v31.id == 28)
val v33 = matchValue(v32)
val v34 = ObjField(node,v29,v33)
v34
  }
}
def matchStrLiteral(node: Node): StrLiteral = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 18 =>
val v35 = body.asInstanceOf[SequenceNode].children(1)
val v39 = unrollRepeat0(v35) map { n =>
val BindNode(v36, v37) = n
assert(v36.id == 22)
val v38 = matchStrChar(v37)
v38
}
val v40 = StrLiteral(node,v39)
v40
case 25 =>
val v41 = body.asInstanceOf[SequenceNode].children(1)
val v45 = unrollRepeat0(v41) map { n =>
val BindNode(v42, v43) = n
assert(v42.id == 22)
val v44 = matchStrChar(v43)
v44
}
val v46 = StrLiteral(node,v45)
v46
  }
}
def matchValue(node: Node): Value = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 29 =>
val v47 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v48, v49) = v47
assert(v48.id == 30)
val v50 = matchBoolValue(v49)
val v51 = BoolValue(node,v50)
v51
case 45 =>
val v52 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v53, v54) = v52
assert(v53.id == 46)
val v55 = matchIntLiteral(v54)
val v56 = IntValue(node,v55)
v56
case 54 =>
val v57 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v58, v59) = v57
assert(v58.id == 17)
val v60 = matchStrLiteral(v59)
val v61 = StrValue(node,v60)
v61
case 55 =>
val v62 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v63, v64) = v62
assert(v63.id == 56)
val v65 = matchListValue(v64)
v65
case 71 =>
val v66 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v67, v68) = v66
assert(v67.id == 72)
val v69 = matchTupleValue(v68)
v69
  }
}
def matchBoolValue(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 31 =>
val v70 = body.asInstanceOf[SequenceNode].children(0)
v70
case 38 =>
val v71 = body.asInstanceOf[SequenceNode].children(0)
v71
  }
}
def matchListValue(node: Node): ListValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 57 =>
val v72 = body.asInstanceOf[SequenceNode].children(2)
val v77 = unrollOptional(v72, 12, 60) map { n =>
val BindNode(v73, v74) = n
assert(v73.id == 60)
val BindNode(v75, v76) = v74
assert(v75.id == 61)
v76
}
val v95 = v77 map { n =>
val v78 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v79, v80) = v78
assert(v79.id == 28)
val v81 = matchValue(v80)
val v82 = List(v81)
val v83 = n.asInstanceOf[SequenceNode].children(1)
val v88 = unrollRepeat0(v83) map { n =>
val BindNode(v84, v85) = n
assert(v84.id == 64)
val BindNode(v86, v87) = v85
assert(v86.id == 65)
v87
}
val v93 = v88 map { n =>
val v89 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v90, v91) = v89
assert(v90.id == 28)
val v92 = matchValue(v91)
v92
}
val v94 = v82 ++ v93
v94
}
val v96 = ListValue(node,v95)
v96
  }
}
def matchTupleValue(node: Node): TupleValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 73 =>
val v97 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v98, v99) = v97
assert(v98.id == 28)
val v100 = matchValue(v99)
val v101 = List(v100)
val v102 = TupleValue(node,v101)
v102
case 76 =>
val v103 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v104, v105) = v103
assert(v104.id == 28)
val v106 = matchValue(v105)
val v107 = List(v106)
val v108 = body.asInstanceOf[SequenceNode].children(3)
val v113 = unrollRepeat1(v108) map { n =>
val BindNode(v109, v110) = n
assert(v109.id == 64)
val BindNode(v111, v112) = v110
assert(v111.id == 65)
v112
}
val v118 = v113 map { n =>
val v114 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v115, v116) = v114
assert(v115.id == 28)
val v117 = matchValue(v116)
v117
}
val v119 = v107 ++ v118
val v120 = TupleValue(node,v119)
v120
  }
}
def matchWS(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 6 =>
val v121 = body.asInstanceOf[SequenceNode].children(0)
val v122 = unrollRepeat0(v121) map { n =>
n
}
v122
  }
}
def matchIntLiteral(node: Node): IntLiteral = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 47 =>
val v123 = body.asInstanceOf[SequenceNode].children(0)
val v124 = List(v123)
val v125 = IntLiteral(node,v124)
v125
case 49 =>
val v126 = body.asInstanceOf[SequenceNode].children(0)
val v127 = List(v126)
val v128 = body.asInstanceOf[SequenceNode].children(1)
val v129 = unrollRepeat0(v128) map { n =>
n
}
val v130 = v129 map { n =>
n
}
val v131 = v127 ++ v130
val v132 = IntLiteral(node,v131)
v132
  }
}
def matchStrChar(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 23 =>
val v133 = body.asInstanceOf[SequenceNode].children(0)
v133
  }
}
def matchStart(node: Node): PyObj = {
  val BindNode(start, BindNode(startNonterm, body)) = node
  assert(start.id == 1)
  assert(startNonterm.id == 2)
  matchPyObj(body)
}
private def unrollRepeat1(node: Node): List[Node] = {
  val BindNode(repeat: NGrammar.NRepeat, body) = node
  body match {
    case BindNode(symbol, repeating: SequenceNode) if symbol.id == repeat.repeatSeq =>
      assert(symbol.id == repeat.repeatSeq)
      val s = repeating.children(1)
      val r = unrollRepeat1(repeating.children(0))
      r :+ s
    case base =>
      List(base)
  }
}
private def unrollRepeat0(node: Node): List[Node] = {
  val BindNode(repeat: NGrammar.NRepeat, body) = node
  body match {
    case BindNode(symbol, repeating: SequenceNode) =>
      assert(symbol.id == repeat.repeatSeq)
      val s = repeating.children(1)
      val r = unrollRepeat0(repeating.children(0))
      r :+ s
    case SequenceNode(_, _, symbol, emptySeq) =>
      assert(symbol.id == repeat.baseSeq)
      assert(emptySeq.isEmpty)
      List()
  }
}
private def unrollOptional(node: Node, emptyId: Int, contentId: Int): Option[Node] = {
  val BindNode(_: NGrammar.NOneOf, body@BindNode(bodySymbol, _)) = node
  if (bodySymbol.id == contentId) Some(body) else None
}
lazy val naiveParser = new NaiveParser(ngrammar)

def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
  naiveParser.parse(text)

def parseAst(text: String): Either[PyObj, ParsingErrors.ParsingError] =
  parse(text) match {
    case Left(ctx) =>
      val tree = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
      tree match {
        case Some(forest) if forest.trees.size == 1 =>
          Left(matchStart(forest.trees.head))
        case Some(forest) =>
          Right(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size))
        case None => ???
      }
    case Right(error) => Right(error)
  }

  def main(args:Array[String]): Unit = {
    val parsed = parseAst("{'descr': '<U4', 'fortran_order': True, 'shape': (1001,)}").left.get
    println(parsed.fields.get.length)
    val fields = parsed.fields.get
    println(fields(0).name.value.map(_.sourceText).mkString)
    println(fields(2).value match {
      case BoolValue(astNode, value) => s"bool ${value.sourceText}"
      case IntValue(astNode, value) => s"int ${astNode.sourceText}"
      case StrValue(astNode, value) => s"str ${astNode.sourceText}"
      case ListValue(astNode, elems) => s"list ${astNode.sourceText}"
      case TupleValue(astNode, elems) => s"tuple ${astNode.sourceText}"
    })
    println(fields(1).name.value.map(_.sourceText).mkString)
    println(fields(2).name.value.map(_.sourceText).mkString)
  }
}