package com.giyeok.jparser.metalang2.generated

import com.giyeok.jparser.Inputs.InputToShortString
import com.giyeok.jparser.ParseResultTree.{JoinNode, Node, BindNode, TerminalNode, SequenceNode}
import com.giyeok.jparser.nparser.{ParseTreeConstructor, NaiveParser, Parser}
import com.giyeok.jparser.{NGrammar, ParsingErrors, ParseForestFunc, Symbols}
import scala.collection.immutable.ListSet

object ProtobufTextFormatGrammar {
  val ngrammar = new NGrammar(
  Map(1 -> NGrammar.NStart(1, 2),
2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("Message"), Set(3)),
4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("WS"), Set(5)),
6 -> NGrammar.NLongest(6, Symbols.Longest(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Nonterminal("Whitespace"),Symbols.Nonterminal("COMMENT"))), 0)), 7),
7 -> NGrammar.NRepeat(7, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Nonterminal("Whitespace"),Symbols.Nonterminal("COMMENT"))), 0), 8, 9),
10 -> NGrammar.NOneOf(10, Symbols.OneOf(ListSet(Symbols.Nonterminal("Whitespace"),Symbols.Nonterminal("COMMENT"))), Set(11,18)),
11 -> NGrammar.NNonterminal(11, Symbols.Nonterminal("Whitespace"), Set(12,14)),
13 -> NGrammar.NTerminal(13, Symbols.Chars(Set('\t','\r',' '))),
15 -> NGrammar.NNonterminal(15, Symbols.Nonterminal("newline"), Set(16)),
17 -> NGrammar.NTerminal(17, Symbols.ExactChar('\n')),
18 -> NGrammar.NNonterminal(18, Symbols.Nonterminal("COMMENT"), Set(19)),
20 -> NGrammar.NTerminal(20, Symbols.ExactChar('#')),
21 -> NGrammar.NRepeat(21, Symbols.Repeat(Symbols.Except(Symbols.Nonterminal("char"), Symbols.Nonterminal("newline")), 0), 8, 22),
23 -> NGrammar.NExcept(23, Symbols.Except(Symbols.Nonterminal("char"), Symbols.Nonterminal("newline")), 24, 15),
24 -> NGrammar.NNonterminal(24, Symbols.Nonterminal("char"), Set(25)),
26 -> NGrammar.NTerminal(26, Symbols.AnyChar),
27 -> NGrammar.NOneOf(27, Symbols.OneOf(ListSet(Symbols.LookaheadExcept(Symbols.AnyChar),Symbols.Nonterminal("newline"))), Set(28,15)),
28 -> NGrammar.NLookaheadExcept(28, Symbols.LookaheadExcept(Symbols.AnyChar), 8, 26),
29 -> NGrammar.NOneOf(29, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Field"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Field")))), 0),Symbols.Nonterminal("WS")))))), Set(30,31)),
30 -> NGrammar.NProxy(30, Symbols.Proxy(Symbols.Sequence(Seq())), 8),
31 -> NGrammar.NProxy(31, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Field"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Field")))), 0),Symbols.Nonterminal("WS")))), 32),
33 -> NGrammar.NNonterminal(33, Symbols.Nonterminal("Field"), Set(34,213)),
35 -> NGrammar.NNonterminal(35, Symbols.Nonterminal("ScalarField"), Set(36)),
37 -> NGrammar.NNonterminal(37, Symbols.Nonterminal("FieldName"), Set(38,67,72)),
39 -> NGrammar.NNonterminal(39, Symbols.Nonterminal("ExtensionName"), Set(40)),
41 -> NGrammar.NTerminal(41, Symbols.ExactChar('[')),
42 -> NGrammar.NNonterminal(42, Symbols.Nonterminal("TypeName"), Set(43)),
44 -> NGrammar.NLongest(44, Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("IDENT"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('.'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("IDENT")))), 0))))), 45),
45 -> NGrammar.NProxy(45, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("IDENT"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('.'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("IDENT")))), 0)))), 46),
47 -> NGrammar.NNonterminal(47, Symbols.Nonterminal("IDENT"), Set(48)),
49 -> NGrammar.NLongest(49, Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Nonterminal("letter"),Symbols.Nonterminal("dec"))), 0))))), 50),
50 -> NGrammar.NProxy(50, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Nonterminal("letter"),Symbols.Nonterminal("dec"))), 0)))), 51),
52 -> NGrammar.NNonterminal(52, Symbols.Nonterminal("letter"), Set(53)),
54 -> NGrammar.NTerminal(54, Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
55 -> NGrammar.NRepeat(55, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Nonterminal("letter"),Symbols.Nonterminal("dec"))), 0), 8, 56),
57 -> NGrammar.NOneOf(57, Symbols.OneOf(ListSet(Symbols.Nonterminal("letter"),Symbols.Nonterminal("dec"))), Set(52,58)),
58 -> NGrammar.NNonterminal(58, Symbols.Nonterminal("dec"), Set(59)),
60 -> NGrammar.NTerminal(60, Symbols.Chars(('0' to '9').toSet)),
61 -> NGrammar.NRepeat(61, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('.'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("IDENT")))), 0), 8, 62),
63 -> NGrammar.NProxy(63, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('.'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("IDENT")))), 64),
65 -> NGrammar.NTerminal(65, Symbols.ExactChar('.')),
66 -> NGrammar.NTerminal(66, Symbols.ExactChar(']')),
68 -> NGrammar.NNonterminal(68, Symbols.Nonterminal("AnyName"), Set(69)),
70 -> NGrammar.NNonterminal(70, Symbols.Nonterminal("Domain"), Set(43)),
71 -> NGrammar.NTerminal(71, Symbols.ExactChar('/')),
73 -> NGrammar.NTerminal(73, Symbols.ExactChar(':')),
74 -> NGrammar.NNonterminal(74, Symbols.Nonterminal("ScalarFieldValue"), Set(75,198)),
76 -> NGrammar.NNonterminal(76, Symbols.Nonterminal("ScalarValue"), Set(77,112,151,153,156,162,175,189,192,195)),
78 -> NGrammar.NNonterminal(78, Symbols.Nonterminal("String"), Set(79)),
80 -> NGrammar.NLongest(80, Symbols.Longest(Symbols.Repeat(Symbols.Nonterminal("STRING"), 1)), 81),
81 -> NGrammar.NRepeat(81, Symbols.Repeat(Symbols.Nonterminal("STRING"), 1), 82, 111),
82 -> NGrammar.NNonterminal(82, Symbols.Nonterminal("STRING"), Set(83,101)),
84 -> NGrammar.NNonterminal(84, Symbols.Nonterminal("single_string"), Set(85)),
86 -> NGrammar.NTerminal(86, Symbols.ExactChar('\'')),
87 -> NGrammar.NRepeat(87, Symbols.Repeat(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('\'')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\'))))), 0), 8, 88),
89 -> NGrammar.NLongest(89, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('\'')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\'))))), 90),
90 -> NGrammar.NOneOf(90, Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('\'')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\')))), Set(91,98)),
91 -> NGrammar.NNonterminal(91, Symbols.Nonterminal("escape"), Set(92,95,96)),
93 -> NGrammar.NTerminal(93, Symbols.ExactChar('\\')),
94 -> NGrammar.NTerminal(94, Symbols.ExactChar('a')),
97 -> NGrammar.NTerminal(97, Symbols.ExactChar('"')),
98 -> NGrammar.NExcept(98, Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('\'')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\')), 99, 93),
99 -> NGrammar.NExcept(99, Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('\'')), Symbols.Nonterminal("newline")), 100, 15),
100 -> NGrammar.NExcept(100, Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('\'')), 24, 86),
102 -> NGrammar.NNonterminal(102, Symbols.Nonterminal("double_string"), Set(103)),
104 -> NGrammar.NRepeat(104, Symbols.Repeat(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('"')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\'))))), 0), 8, 105),
106 -> NGrammar.NLongest(106, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('"')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\'))))), 107),
107 -> NGrammar.NOneOf(107, Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('"')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\')))), Set(91,108)),
108 -> NGrammar.NExcept(108, Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('"')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\')), 109, 93),
109 -> NGrammar.NExcept(109, Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('"')), Symbols.Nonterminal("newline")), 110, 15),
110 -> NGrammar.NExcept(110, Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('"')), 24, 97),
113 -> NGrammar.NNonterminal(113, Symbols.Nonterminal("Float"), Set(114)),
115 -> NGrammar.NLongest(115, Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('-'),Symbols.Nonterminal("WS")))))),Symbols.Nonterminal("FLOAT"))))), 116),
116 -> NGrammar.NProxy(116, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('-'),Symbols.Nonterminal("WS")))))),Symbols.Nonterminal("FLOAT")))), 117),
118 -> NGrammar.NOneOf(118, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('-'),Symbols.Nonterminal("WS")))))), Set(30,119)),
119 -> NGrammar.NProxy(119, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('-'),Symbols.Nonterminal("WS")))), 120),
121 -> NGrammar.NTerminal(121, Symbols.ExactChar('-')),
122 -> NGrammar.NNonterminal(122, Symbols.Nonterminal("FLOAT"), Set(123,147)),
124 -> NGrammar.NLongest(124, Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("float_lit"),Symbols.Chars(Set('F','f')))))), 125),
125 -> NGrammar.NProxy(125, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("float_lit"),Symbols.Chars(Set('F','f'))))), 126),
127 -> NGrammar.NNonterminal(127, Symbols.Nonterminal("float_lit"), Set(128,137,145)),
129 -> NGrammar.NRepeat(129, Symbols.Repeat(Symbols.Nonterminal("dec"), 1), 58, 130),
131 -> NGrammar.NOneOf(131, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("exp"))), Set(30,132)),
132 -> NGrammar.NNonterminal(132, Symbols.Nonterminal("exp"), Set(133)),
134 -> NGrammar.NTerminal(134, Symbols.Chars(Set('E','e'))),
135 -> NGrammar.NOneOf(135, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Chars(Set('+','-')))), Set(30,136)),
136 -> NGrammar.NTerminal(136, Symbols.Chars(Set('+','-'))),
138 -> NGrammar.NNonterminal(138, Symbols.Nonterminal("dec_lit"), Set(139,141)),
140 -> NGrammar.NTerminal(140, Symbols.ExactChar('0')),
142 -> NGrammar.NExcept(142, Symbols.Except(Symbols.Nonterminal("dec"), Symbols.ExactChar('0')), 58, 140),
143 -> NGrammar.NRepeat(143, Symbols.Repeat(Symbols.Nonterminal("dec"), 0), 8, 144),
146 -> NGrammar.NTerminal(146, Symbols.Chars(Set('F','f'))),
148 -> NGrammar.NLongest(148, Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("dec_lit"),Symbols.Chars(Set('F','f')))))), 149),
149 -> NGrammar.NProxy(149, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("dec_lit"),Symbols.Chars(Set('F','f'))))), 150),
152 -> NGrammar.NNonterminal(152, Symbols.Nonterminal("Identifier"), Set(72)),
154 -> NGrammar.NNonterminal(154, Symbols.Nonterminal("SignedIdentifier"), Set(155)),
157 -> NGrammar.NNonterminal(157, Symbols.Nonterminal("DecSignedInteger"), Set(158)),
159 -> NGrammar.NNonterminal(159, Symbols.Nonterminal("DEC_INT"), Set(160)),
161 -> NGrammar.NLongest(161, Symbols.Longest(Symbols.Nonterminal("dec_lit")), 138),
163 -> NGrammar.NNonterminal(163, Symbols.Nonterminal("OctSignedInteger"), Set(164)),
165 -> NGrammar.NNonterminal(165, Symbols.Nonterminal("OCT_INT"), Set(166)),
167 -> NGrammar.NLongest(167, Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0'),Symbols.Repeat(Symbols.Nonterminal("oct"), 1))))), 168),
168 -> NGrammar.NProxy(168, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0'),Symbols.Repeat(Symbols.Nonterminal("oct"), 1)))), 169),
170 -> NGrammar.NRepeat(170, Symbols.Repeat(Symbols.Nonterminal("oct"), 1), 171, 174),
171 -> NGrammar.NNonterminal(171, Symbols.Nonterminal("oct"), Set(172)),
173 -> NGrammar.NTerminal(173, Symbols.Chars(('0' to '7').toSet)),
176 -> NGrammar.NNonterminal(176, Symbols.Nonterminal("HexSignedInteger"), Set(177)),
178 -> NGrammar.NNonterminal(178, Symbols.Nonterminal("HEX_INT"), Set(179)),
180 -> NGrammar.NLongest(180, Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0'),Symbols.Chars(Set('X','x')),Symbols.Repeat(Symbols.Nonterminal("hex"), 1))))), 181),
181 -> NGrammar.NProxy(181, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0'),Symbols.Chars(Set('X','x')),Symbols.Repeat(Symbols.Nonterminal("hex"), 1)))), 182),
183 -> NGrammar.NTerminal(183, Symbols.Chars(Set('X','x'))),
184 -> NGrammar.NRepeat(184, Symbols.Repeat(Symbols.Nonterminal("hex"), 1), 185, 188),
185 -> NGrammar.NNonterminal(185, Symbols.Nonterminal("hex"), Set(186)),
187 -> NGrammar.NTerminal(187, Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet)),
190 -> NGrammar.NNonterminal(190, Symbols.Nonterminal("DecUnsignedInteger"), Set(191)),
193 -> NGrammar.NNonterminal(193, Symbols.Nonterminal("OctUnsignedInteger"), Set(194)),
196 -> NGrammar.NNonterminal(196, Symbols.Nonterminal("HexUnsignedInteger"), Set(197)),
199 -> NGrammar.NNonterminal(199, Symbols.Nonterminal("ScalarList"), Set(200)),
201 -> NGrammar.NOneOf(201, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ScalarValue"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ScalarValue")))), 0),Symbols.Nonterminal("WS")))))), Set(30,202)),
202 -> NGrammar.NProxy(202, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ScalarValue"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ScalarValue")))), 0),Symbols.Nonterminal("WS")))), 203),
204 -> NGrammar.NRepeat(204, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ScalarValue")))), 0), 8, 205),
206 -> NGrammar.NProxy(206, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ScalarValue")))), 207),
208 -> NGrammar.NTerminal(208, Symbols.ExactChar(',')),
209 -> NGrammar.NOneOf(209, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Chars(Set(',',';'))))))), Set(30,210)),
210 -> NGrammar.NProxy(210, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Chars(Set(',',';'))))), 211),
212 -> NGrammar.NTerminal(212, Symbols.Chars(Set(',',';'))),
214 -> NGrammar.NNonterminal(214, Symbols.Nonterminal("MessageField"), Set(215)),
216 -> NGrammar.NOneOf(216, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.ExactChar(':'))), Set(30,73)),
217 -> NGrammar.NNonterminal(217, Symbols.Nonterminal("MessageFieldValue"), Set(218,226)),
219 -> NGrammar.NNonterminal(219, Symbols.Nonterminal("MessageValue"), Set(220,223)),
221 -> NGrammar.NTerminal(221, Symbols.ExactChar('{')),
222 -> NGrammar.NTerminal(222, Symbols.ExactChar('}')),
224 -> NGrammar.NTerminal(224, Symbols.ExactChar('<')),
225 -> NGrammar.NTerminal(225, Symbols.ExactChar('>')),
227 -> NGrammar.NNonterminal(227, Symbols.Nonterminal("MessageList"), Set(228)),
229 -> NGrammar.NOneOf(229, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("MessageValue"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("MessageValue")))), 0),Symbols.Nonterminal("WS")))))), Set(30,230)),
230 -> NGrammar.NProxy(230, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("MessageValue"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("MessageValue")))), 0),Symbols.Nonterminal("WS")))), 231),
232 -> NGrammar.NRepeat(232, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("MessageValue")))), 0), 8, 233),
234 -> NGrammar.NProxy(234, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("MessageValue")))), 235),
236 -> NGrammar.NRepeat(236, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Field")))), 0), 8, 237),
238 -> NGrammar.NProxy(238, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Field")))), 239)),
  Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Field"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Field")))), 0),Symbols.Nonterminal("WS")))))))), Seq(4,29)),
5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Nonterminal("Whitespace"),Symbols.Nonterminal("COMMENT"))), 0)))), Seq(6)),
8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq()), Seq()),
9 -> NGrammar.NSequence(9, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Nonterminal("Whitespace"),Symbols.Nonterminal("COMMENT"))), 0),Symbols.OneOf(ListSet(Symbols.Nonterminal("Whitespace"),Symbols.Nonterminal("COMMENT"))))), Seq(7,10)),
12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.Chars(Set('\t','\r',' ')))), Seq(13)),
14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq(Symbols.Nonterminal("newline"))), Seq(15)),
16 -> NGrammar.NSequence(16, Symbols.Sequence(Seq(Symbols.ExactChar('\n'))), Seq(17)),
19 -> NGrammar.NSequence(19, Symbols.Sequence(Seq(Symbols.ExactChar('#'),Symbols.Repeat(Symbols.Except(Symbols.Nonterminal("char"), Symbols.Nonterminal("newline")), 0),Symbols.OneOf(ListSet(Symbols.LookaheadExcept(Symbols.AnyChar),Symbols.Nonterminal("newline"))))), Seq(20,21,27)),
22 -> NGrammar.NSequence(22, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Except(Symbols.Nonterminal("char"), Symbols.Nonterminal("newline")), 0),Symbols.Except(Symbols.Nonterminal("char"), Symbols.Nonterminal("newline")))), Seq(21,23)),
25 -> NGrammar.NSequence(25, Symbols.Sequence(Seq(Symbols.AnyChar)), Seq(26)),
32 -> NGrammar.NSequence(32, Symbols.Sequence(Seq(Symbols.Nonterminal("Field"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Field")))), 0),Symbols.Nonterminal("WS"))), Seq(33,236,4)),
34 -> NGrammar.NSequence(34, Symbols.Sequence(Seq(Symbols.Nonterminal("ScalarField"))), Seq(35)),
36 -> NGrammar.NSequence(36, Symbols.Sequence(Seq(Symbols.Nonterminal("FieldName"),Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ScalarFieldValue"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Chars(Set(',',';'))))))))), Seq(37,4,73,4,74,209)),
38 -> NGrammar.NSequence(38, Symbols.Sequence(Seq(Symbols.Nonterminal("ExtensionName"))), Seq(39)),
40 -> NGrammar.NSequence(40, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.ExactChar(']'))), Seq(41,4,42,4,66)),
43 -> NGrammar.NSequence(43, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("IDENT"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('.'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("IDENT")))), 0))))))), Seq(44)),
46 -> NGrammar.NSequence(46, Symbols.Sequence(Seq(Symbols.Nonterminal("IDENT"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('.'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("IDENT")))), 0))), Seq(47,61)),
48 -> NGrammar.NSequence(48, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Nonterminal("letter"),Symbols.Nonterminal("dec"))), 0))))))), Seq(49)),
51 -> NGrammar.NSequence(51, Symbols.Sequence(Seq(Symbols.Nonterminal("letter"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Nonterminal("letter"),Symbols.Nonterminal("dec"))), 0))), Seq(52,55)),
53 -> NGrammar.NSequence(53, Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(54)),
56 -> NGrammar.NSequence(56, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Nonterminal("letter"),Symbols.Nonterminal("dec"))), 0),Symbols.OneOf(ListSet(Symbols.Nonterminal("letter"),Symbols.Nonterminal("dec"))))), Seq(55,57)),
59 -> NGrammar.NSequence(59, Symbols.Sequence(Seq(Symbols.Chars(('0' to '9').toSet))), Seq(60)),
62 -> NGrammar.NSequence(62, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('.'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("IDENT")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('.'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("IDENT")))))), Seq(61,63)),
64 -> NGrammar.NSequence(64, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('.'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("IDENT"))), Seq(4,65,4,47)),
67 -> NGrammar.NSequence(67, Symbols.Sequence(Seq(Symbols.Nonterminal("AnyName"))), Seq(68)),
69 -> NGrammar.NSequence(69, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Domain"),Symbols.Nonterminal("WS"),Symbols.ExactChar('/'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.ExactChar(']'))), Seq(41,4,70,4,71,4,42,4,66)),
72 -> NGrammar.NSequence(72, Symbols.Sequence(Seq(Symbols.Nonterminal("IDENT"))), Seq(47)),
75 -> NGrammar.NSequence(75, Symbols.Sequence(Seq(Symbols.Nonterminal("ScalarValue"))), Seq(76)),
77 -> NGrammar.NSequence(77, Symbols.Sequence(Seq(Symbols.Nonterminal("String"))), Seq(78)),
79 -> NGrammar.NSequence(79, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Repeat(Symbols.Nonterminal("STRING"), 1)))), Seq(80)),
83 -> NGrammar.NSequence(83, Symbols.Sequence(Seq(Symbols.Nonterminal("single_string"))), Seq(84)),
85 -> NGrammar.NSequence(85, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Repeat(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('\'')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\'))))), 0),Symbols.ExactChar('\''))), Seq(86,87,86)),
88 -> NGrammar.NSequence(88, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('\'')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\'))))), 0),Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('\'')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\'))))))), Seq(87,89)),
92 -> NGrammar.NSequence(92, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('a'))), Seq(93,94)),
95 -> NGrammar.NSequence(95, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\''))), Seq(93,86)),
96 -> NGrammar.NSequence(96, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('"'))), Seq(93,97)),
101 -> NGrammar.NSequence(101, Symbols.Sequence(Seq(Symbols.Nonterminal("double_string"))), Seq(102)),
103 -> NGrammar.NSequence(103, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('"')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\'))))), 0),Symbols.ExactChar('"'))), Seq(97,104,97)),
105 -> NGrammar.NSequence(105, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('"')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\'))))), 0),Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Nonterminal("escape"),Symbols.Except(Symbols.Except(Symbols.Except(Symbols.Nonterminal("char"), Symbols.ExactChar('"')), Symbols.Nonterminal("newline")), Symbols.ExactChar('\\'))))))), Seq(104,106)),
111 -> NGrammar.NSequence(111, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("STRING"), 1),Symbols.Nonterminal("STRING"))), Seq(81,82)),
112 -> NGrammar.NSequence(112, Symbols.Sequence(Seq(Symbols.Nonterminal("Float"))), Seq(113)),
114 -> NGrammar.NSequence(114, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('-'),Symbols.Nonterminal("WS")))))),Symbols.Nonterminal("FLOAT"))))))), Seq(115)),
117 -> NGrammar.NSequence(117, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('-'),Symbols.Nonterminal("WS")))))),Symbols.Nonterminal("FLOAT"))), Seq(118,122)),
120 -> NGrammar.NSequence(120, Symbols.Sequence(Seq(Symbols.ExactChar('-'),Symbols.Nonterminal("WS"))), Seq(121,4)),
123 -> NGrammar.NSequence(123, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("float_lit"),Symbols.Chars(Set('F','f')))))))), Seq(124)),
126 -> NGrammar.NSequence(126, Symbols.Sequence(Seq(Symbols.Nonterminal("float_lit"),Symbols.Chars(Set('F','f')))), Seq(127,146)),
128 -> NGrammar.NSequence(128, Symbols.Sequence(Seq(Symbols.ExactChar('.'),Symbols.Repeat(Symbols.Nonterminal("dec"), 1),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("exp"))))), Seq(65,129,131)),
130 -> NGrammar.NSequence(130, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("dec"), 1),Symbols.Nonterminal("dec"))), Seq(129,58)),
133 -> NGrammar.NSequence(133, Symbols.Sequence(Seq(Symbols.Chars(Set('E','e')),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Chars(Set('+','-')))),Symbols.Repeat(Symbols.Nonterminal("dec"), 1))), Seq(134,135,129)),
137 -> NGrammar.NSequence(137, Symbols.Sequence(Seq(Symbols.Nonterminal("dec_lit"),Symbols.ExactChar('.'),Symbols.Repeat(Symbols.Nonterminal("dec"), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("exp"))))), Seq(138,65,143,131)),
139 -> NGrammar.NSequence(139, Symbols.Sequence(Seq(Symbols.ExactChar('0'))), Seq(140)),
141 -> NGrammar.NSequence(141, Symbols.Sequence(Seq(Symbols.Except(Symbols.Nonterminal("dec"), Symbols.ExactChar('0')),Symbols.Repeat(Symbols.Nonterminal("dec"), 0))), Seq(142,143)),
144 -> NGrammar.NSequence(144, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("dec"), 0),Symbols.Nonterminal("dec"))), Seq(143,58)),
145 -> NGrammar.NSequence(145, Symbols.Sequence(Seq(Symbols.Nonterminal("dec_lit"),Symbols.Nonterminal("exp"))), Seq(138,132)),
147 -> NGrammar.NSequence(147, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("dec_lit"),Symbols.Chars(Set('F','f')))))))), Seq(148)),
150 -> NGrammar.NSequence(150, Symbols.Sequence(Seq(Symbols.Nonterminal("dec_lit"),Symbols.Chars(Set('F','f')))), Seq(138,146)),
151 -> NGrammar.NSequence(151, Symbols.Sequence(Seq(Symbols.Nonterminal("Identifier"))), Seq(152)),
153 -> NGrammar.NSequence(153, Symbols.Sequence(Seq(Symbols.Nonterminal("SignedIdentifier"))), Seq(154)),
155 -> NGrammar.NSequence(155, Symbols.Sequence(Seq(Symbols.ExactChar('-'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("IDENT"))), Seq(121,4,47)),
156 -> NGrammar.NSequence(156, Symbols.Sequence(Seq(Symbols.Nonterminal("DecSignedInteger"))), Seq(157)),
158 -> NGrammar.NSequence(158, Symbols.Sequence(Seq(Symbols.ExactChar('-'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("DEC_INT"))), Seq(121,4,159)),
160 -> NGrammar.NSequence(160, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Nonterminal("dec_lit")))), Seq(161)),
162 -> NGrammar.NSequence(162, Symbols.Sequence(Seq(Symbols.Nonterminal("OctSignedInteger"))), Seq(163)),
164 -> NGrammar.NSequence(164, Symbols.Sequence(Seq(Symbols.ExactChar('-'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("OCT_INT"))), Seq(121,4,165)),
166 -> NGrammar.NSequence(166, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0'),Symbols.Repeat(Symbols.Nonterminal("oct"), 1))))))), Seq(167)),
169 -> NGrammar.NSequence(169, Symbols.Sequence(Seq(Symbols.ExactChar('0'),Symbols.Repeat(Symbols.Nonterminal("oct"), 1))), Seq(140,170)),
172 -> NGrammar.NSequence(172, Symbols.Sequence(Seq(Symbols.Chars(('0' to '7').toSet))), Seq(173)),
174 -> NGrammar.NSequence(174, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("oct"), 1),Symbols.Nonterminal("oct"))), Seq(170,171)),
175 -> NGrammar.NSequence(175, Symbols.Sequence(Seq(Symbols.Nonterminal("HexSignedInteger"))), Seq(176)),
177 -> NGrammar.NSequence(177, Symbols.Sequence(Seq(Symbols.ExactChar('-'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("HEX_INT"))), Seq(121,4,178)),
179 -> NGrammar.NSequence(179, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0'),Symbols.Chars(Set('X','x')),Symbols.Repeat(Symbols.Nonterminal("hex"), 1))))))), Seq(180)),
182 -> NGrammar.NSequence(182, Symbols.Sequence(Seq(Symbols.ExactChar('0'),Symbols.Chars(Set('X','x')),Symbols.Repeat(Symbols.Nonterminal("hex"), 1))), Seq(140,183,184)),
186 -> NGrammar.NSequence(186, Symbols.Sequence(Seq(Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet))), Seq(187)),
188 -> NGrammar.NSequence(188, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("hex"), 1),Symbols.Nonterminal("hex"))), Seq(184,185)),
189 -> NGrammar.NSequence(189, Symbols.Sequence(Seq(Symbols.Nonterminal("DecUnsignedInteger"))), Seq(190)),
191 -> NGrammar.NSequence(191, Symbols.Sequence(Seq(Symbols.Nonterminal("DEC_INT"))), Seq(159)),
192 -> NGrammar.NSequence(192, Symbols.Sequence(Seq(Symbols.Nonterminal("OctUnsignedInteger"))), Seq(193)),
194 -> NGrammar.NSequence(194, Symbols.Sequence(Seq(Symbols.Nonterminal("OCT_INT"))), Seq(165)),
195 -> NGrammar.NSequence(195, Symbols.Sequence(Seq(Symbols.Nonterminal("HexUnsignedInteger"))), Seq(196)),
197 -> NGrammar.NSequence(197, Symbols.Sequence(Seq(Symbols.Nonterminal("HEX_INT"))), Seq(178)),
198 -> NGrammar.NSequence(198, Symbols.Sequence(Seq(Symbols.Nonterminal("ScalarList"))), Seq(199)),
200 -> NGrammar.NSequence(200, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ScalarValue"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ScalarValue")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(']'))), Seq(41,4,201,66)),
203 -> NGrammar.NSequence(203, Symbols.Sequence(Seq(Symbols.Nonterminal("ScalarValue"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ScalarValue")))), 0),Symbols.Nonterminal("WS"))), Seq(76,204,4)),
205 -> NGrammar.NSequence(205, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ScalarValue")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ScalarValue")))))), Seq(204,206)),
207 -> NGrammar.NSequence(207, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ScalarValue"))), Seq(4,208,4,76)),
211 -> NGrammar.NSequence(211, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Chars(Set(',',';')))), Seq(4,212)),
213 -> NGrammar.NSequence(213, Symbols.Sequence(Seq(Symbols.Nonterminal("MessageField"))), Seq(214)),
215 -> NGrammar.NSequence(215, Symbols.Sequence(Seq(Symbols.Nonterminal("FieldName"),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.ExactChar(':'))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("MessageFieldValue"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Chars(Set(',',';'))))))))), Seq(37,4,216,4,217,209)),
218 -> NGrammar.NSequence(218, Symbols.Sequence(Seq(Symbols.Nonterminal("MessageValue"))), Seq(219)),
220 -> NGrammar.NSequence(220, Symbols.Sequence(Seq(Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Message"),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(221,4,2,4,222)),
223 -> NGrammar.NSequence(223, Symbols.Sequence(Seq(Symbols.ExactChar('<'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Message"),Symbols.Nonterminal("WS"),Symbols.ExactChar('>'))), Seq(224,4,2,4,225)),
226 -> NGrammar.NSequence(226, Symbols.Sequence(Seq(Symbols.Nonterminal("MessageList"))), Seq(227)),
228 -> NGrammar.NSequence(228, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("MessageValue"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("MessageValue")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(']'))), Seq(41,4,229,66)),
231 -> NGrammar.NSequence(231, Symbols.Sequence(Seq(Symbols.Nonterminal("MessageValue"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("MessageValue")))), 0),Symbols.Nonterminal("WS"))), Seq(219,232,4)),
233 -> NGrammar.NSequence(233, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("MessageValue")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("MessageValue")))))), Seq(232,234)),
235 -> NGrammar.NSequence(235, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("MessageValue"))), Seq(4,208,4,219)),
237 -> NGrammar.NSequence(237, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Field")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Field")))))), Seq(236,238)),
239 -> NGrammar.NSequence(239, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Field"))), Seq(4,33))),
  1)

  sealed trait ASTNode {
  val astNode: Node
  def prettyPrint(): String
}
case class Message(astNode:Node, fields:Option[List[Field]]) extends ASTNode with MessageFieldValue{
  def prettyPrint(): String = s"Message(" + "fields=" + (fields match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
}) + ")"
}
sealed trait Field extends ASTNode
case class MessageField(astNode:Node, name:Node, value:MessageFieldValue) extends ASTNode with Field{
  def prettyPrint(): String = s"MessageField(" + "name=" + name.sourceText + name.range+ ", " + "value=" + value.prettyPrint() + ")"
}
sealed trait MessageFieldValue extends ASTNode
case class ScalarField(astNode:Node, name:Node, value:ScalarFieldValue) extends ASTNode with Field{
  def prettyPrint(): String = s"ScalarField(" + "name=" + name.sourceText + name.range+ ", " + "value=" + value.prettyPrint() + ")"
}
sealed trait ScalarFieldValue extends ASTNode
case class MessageList(astNode:Node, elems:Option[List[Message]]) extends ASTNode with MessageFieldValue{
  def prettyPrint(): String = s"MessageList(" + "elems=" + (elems match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
}) + ")"
}
case class ScalarList(astNode:Node, elems:Option[List[ScalarValue]]) extends ASTNode with ScalarFieldValue{
  def prettyPrint(): String = s"ScalarList(" + "elems=" + (elems match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
}) + ")"
}
sealed trait ScalarValue extends ASTNode with ScalarFieldValue
case class StringValue(astNode:Node, value:Node) extends ASTNode with ScalarValue{
  def prettyPrint(): String = s"StringValue(" + "value=" + value.sourceText + value.range + ")"
}
case class FloatValue(astNode:Node, value:Node) extends ASTNode with ScalarValue{
  def prettyPrint(): String = s"FloatValue(" + "value=" + value.sourceText + value.range + ")"
}
case class IdentifierValue(astNode:Node, value:Node) extends ASTNode with ScalarValue{
  def prettyPrint(): String = s"IdentifierValue(" + "value=" + value.sourceText + value.range + ")"
}
case class SignedIdentifierValue(astNode:Node, value:Node) extends ASTNode with ScalarValue{
  def prettyPrint(): String = s"SignedIdentifierValue(" + "value=" + value.sourceText + value.range + ")"
}
case class DecSignedIntegerValue(astNode:Node, value:Node) extends ASTNode with ScalarValue{
  def prettyPrint(): String = s"DecSignedIntegerValue(" + "value=" + value.sourceText + value.range + ")"
}
case class OctSignedIntegerValue(astNode:Node, value:Node) extends ASTNode with ScalarValue{
  def prettyPrint(): String = s"OctSignedIntegerValue(" + "value=" + value.sourceText + value.range + ")"
}
case class HexSignedIntegerValue(astNode:Node, value:Node) extends ASTNode with ScalarValue{
  def prettyPrint(): String = s"HexSignedIntegerValue(" + "value=" + value.sourceText + value.range + ")"
}
case class DecUnsignedIntegerValue(astNode:Node, value:Node) extends ASTNode with ScalarValue{
  def prettyPrint(): String = s"DecUnsignedIntegerValue(" + "value=" + value.sourceText + value.range + ")"
}
case class OctUnsignedIntegerValue(astNode:Node, value:Node) extends ASTNode with ScalarValue{
  def prettyPrint(): String = s"OctUnsignedIntegerValue(" + "value=" + value.sourceText + value.range + ")"
}
case class HexUnsignedIntegerValue(astNode:Node, value:Node) extends ASTNode with ScalarValue{
  def prettyPrint(): String = s"HexUnsignedIntegerValue(" + "value=" + value.sourceText + value.range + ")"
}
case class DecLit(astNode:Node, value:List[Node]) extends ASTNode{
  def prettyPrint(): String = s"DecLit(" + "value=" + "[" + value.map(e => e.sourceText + e.range).mkString(",") + "]" + ")"
}
case class FloatLit(astNode:Node, dec:DecLit, frac:DecLit, exp:Option[List[Node]]) extends ASTNode{
  def prettyPrint(): String = s"FloatLit(" + "dec=" + dec.prettyPrint()+ ", " + "frac=" + frac.prettyPrint()+ ", " + "exp=" + (exp match { case Some(v) =>
  "[" + v.map(e => e.sourceText + e.range).mkString(",") + "]"
  case None => "null"
}) + ")"
}
implicit class SourceTextOfNode(node: Node) {
  def sourceText: String = node match {
    case TerminalNode(_, input) => input.toRawString
    case BindNode(_, body) => body.sourceText
    case JoinNode(_, body, _) => body.sourceText
    case seq: SequenceNode => seq.children map (_.sourceText) mkString ""
    case _ => throw new Exception("Cyclic bind")
  }
}

def matchMessage(node: Node): Message = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 3 =>
val v1 = body.asInstanceOf[SequenceNode].children(1)
val v6 = unrollOptional(v1, 30, 31) map { n =>
val BindNode(v2, v3) = n
assert(v2.id == 31)
val BindNode(v4, v5) = v3
assert(v4.id == 32)
v5
}
val v24 = v6 map { n =>
val v7 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v8, v9) = v7
assert(v8.id == 33)
val v10 = matchField(v9)
val v11 = List(v10)
val v12 = n.asInstanceOf[SequenceNode].children(1)
val v17 = unrollRepeat0(v12) map { n =>
val BindNode(v13, v14) = n
assert(v13.id == 238)
val BindNode(v15, v16) = v14
assert(v15.id == 239)
v16
}
val v22 = v17 map { n =>
val v18 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v19, v20) = v18
assert(v19.id == 33)
val v21 = matchField(v20)
v21
}
val v23 = v11 ++ v22
v23
}
val v25 = Message(node,v24)
v25
  }
}
def matchField(node: Node): Field = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 34 =>
val v26 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v27, v28) = v26
assert(v27.id == 35)
val v29 = matchScalarField(v28)
v29
case 213 =>
val v30 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v31, v32) = v30
assert(v31.id == 214)
val v33 = matchMessageField(v32)
v33
  }
}
def matchMessageField(node: Node): MessageField = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 215 =>
val v34 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v35, v36) = v34
assert(v35.id == 37)
val v37 = matchFieldName(v36)
val v38 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v39, v40) = v38
assert(v39.id == 217)
val v41 = matchMessageFieldValue(v40)
val v42 = MessageField(node,v37,v41)
v42
  }
}
def matchMessageFieldValue(node: Node): MessageFieldValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 218 =>
val v43 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v44, v45) = v43
assert(v44.id == 219)
val v46 = matchMessageValue(v45)
v46
case 226 =>
val v47 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v48, v49) = v47
assert(v48.id == 227)
val v50 = matchMessageList(v49)
v50
  }
}
def matchScalarField(node: Node): ScalarField = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 36 =>
val v51 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v52, v53) = v51
assert(v52.id == 37)
val v54 = matchFieldName(v53)
val v55 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v56, v57) = v55
assert(v56.id == 74)
val v58 = matchScalarFieldValue(v57)
val v59 = ScalarField(node,v54,v58)
v59
  }
}
def matchScalarFieldValue(node: Node): ScalarFieldValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 75 =>
val v60 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v61, v62) = v60
assert(v61.id == 76)
val v63 = matchScalarValue(v62)
v63
case 198 =>
val v64 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v65, v66) = v64
assert(v65.id == 199)
val v67 = matchScalarList(v66)
v67
  }
}
def matchMessageList(node: Node): MessageList = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 228 =>
val v68 = body.asInstanceOf[SequenceNode].children(2)
val v73 = unrollOptional(v68, 30, 230) map { n =>
val BindNode(v69, v70) = n
assert(v69.id == 230)
val BindNode(v71, v72) = v70
assert(v71.id == 231)
v72
}
val v91 = v73 map { n =>
val v74 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v75, v76) = v74
assert(v75.id == 219)
val v77 = matchMessageValue(v76)
val v78 = List(v77)
val v79 = n.asInstanceOf[SequenceNode].children(1)
val v84 = unrollRepeat0(v79) map { n =>
val BindNode(v80, v81) = n
assert(v80.id == 234)
val BindNode(v82, v83) = v81
assert(v82.id == 235)
v83
}
val v89 = v84 map { n =>
val v85 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v86, v87) = v85
assert(v86.id == 219)
val v88 = matchMessageValue(v87)
v88
}
val v90 = v78 ++ v89
v90
}
val v92 = MessageList(node,v91)
v92
  }
}
def matchScalarList(node: Node): ScalarList = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 200 =>
val v93 = body.asInstanceOf[SequenceNode].children(2)
val v98 = unrollOptional(v93, 30, 202) map { n =>
val BindNode(v94, v95) = n
assert(v94.id == 202)
val BindNode(v96, v97) = v95
assert(v96.id == 203)
v97
}
val v116 = v98 map { n =>
val v99 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v100, v101) = v99
assert(v100.id == 76)
val v102 = matchScalarValue(v101)
val v103 = List(v102)
val v104 = n.asInstanceOf[SequenceNode].children(1)
val v109 = unrollRepeat0(v104) map { n =>
val BindNode(v105, v106) = n
assert(v105.id == 206)
val BindNode(v107, v108) = v106
assert(v107.id == 207)
v108
}
val v114 = v109 map { n =>
val v110 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v111, v112) = v110
assert(v111.id == 76)
val v113 = matchScalarValue(v112)
v113
}
val v115 = v103 ++ v114
v115
}
val v117 = ScalarList(node,v116)
v117
  }
}
def matchMessageValue(node: Node): Message = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 220 =>
val v118 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v119, v120) = v118
assert(v119.id == 2)
val v121 = matchMessage(v120)
v121
case 223 =>
val v122 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v123, v124) = v122
assert(v123.id == 2)
val v125 = matchMessage(v124)
v125
  }
}
def matchScalarValue(node: Node): ScalarValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 77 =>
val v126 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v127, v128) = v126
assert(v127.id == 78)
val v129 = matchString(v128)
val v130 = StringValue(node,v129)
v130
case 112 =>
val v131 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v132, v133) = v131
assert(v132.id == 113)
val v134 = matchFloat(v133)
val v135 = FloatValue(node,v134)
v135
case 151 =>
val v136 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v137, v138) = v136
assert(v137.id == 152)
val v139 = matchIdentifier(v138)
val v140 = IdentifierValue(node,v139)
v140
case 153 =>
val v141 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v142, v143) = v141
assert(v142.id == 154)
val v144 = matchSignedIdentifier(v143)
val v145 = SignedIdentifierValue(node,v144)
v145
case 156 =>
val v146 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v147, v148) = v146
assert(v147.id == 157)
val v149 = matchDecSignedInteger(v148)
val v150 = DecSignedIntegerValue(node,v149)
v150
case 162 =>
val v151 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v152, v153) = v151
assert(v152.id == 163)
val v154 = matchOctSignedInteger(v153)
val v155 = OctSignedIntegerValue(node,v154)
v155
case 175 =>
val v156 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v157, v158) = v156
assert(v157.id == 176)
val v159 = matchHexSignedInteger(v158)
val v160 = HexSignedIntegerValue(node,v159)
v160
case 189 =>
val v161 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v162, v163) = v161
assert(v162.id == 190)
val v164 = matchDecUnsignedInteger(v163)
val v165 = DecUnsignedIntegerValue(node,v164)
v165
case 192 =>
val v166 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v167, v168) = v166
assert(v167.id == 193)
val v169 = matchOctUnsignedInteger(v168)
val v170 = OctUnsignedIntegerValue(node,v169)
v170
case 195 =>
val v171 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v172, v173) = v171
assert(v172.id == 196)
val v174 = matchHexUnsignedInteger(v173)
val v175 = HexUnsignedIntegerValue(node,v174)
v175
  }
}
def matchString(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 79 =>
val v176 = body.asInstanceOf[SequenceNode].children(0)
v176
  }
}
def matchFloat(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 114 =>
val v177 = body.asInstanceOf[SequenceNode].children(0)
v177
  }
}
def matchIdentifier(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 72 =>
val v178 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v179, v180) = v178
assert(v179.id == 47)
val v181 = matchIDENT(v180)
v181
  }
}
def matchSignedIdentifier(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 155 =>
val v182 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v183, v184) = v182
assert(v183.id == 47)
val v185 = matchIDENT(v184)
v185
  }
}
def matchDecSignedInteger(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 158 =>
val v186 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v187, v188) = v186
assert(v187.id == 159)
val v189 = matchDEC_INT(v188)
v189
  }
}
def matchOctSignedInteger(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 164 =>
val v190 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v191, v192) = v190
assert(v191.id == 165)
val v193 = matchOCT_INT(v192)
v193
  }
}
def matchHexSignedInteger(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 177 =>
val v194 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v195, v196) = v194
assert(v195.id == 178)
val v197 = matchHEX_INT(v196)
v197
  }
}
def matchDecUnsignedInteger(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 191 =>
val v198 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v199, v200) = v198
assert(v199.id == 159)
val v201 = matchDEC_INT(v200)
v201
  }
}
def matchOctUnsignedInteger(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 194 =>
val v202 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v203, v204) = v202
assert(v203.id == 165)
val v205 = matchOCT_INT(v204)
v205
  }
}
def matchHexUnsignedInteger(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 197 =>
val v206 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v207, v208) = v206
assert(v207.id == 178)
val v209 = matchHEX_INT(v208)
v209
  }
}
def matchFieldName(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 38 =>
val v210 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v211, v212) = v210
assert(v211.id == 39)
val v213 = matchExtensionName(v212)
v213
case 67 =>
val v214 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v215, v216) = v214
assert(v215.id == 68)
val v217 = matchAnyName(v216)
v217
case 72 =>
val v218 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v219, v220) = v218
assert(v219.id == 47)
val v221 = matchIDENT(v220)
v221
  }
}
def matchExtensionName(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 40 =>
val v222 = body.asInstanceOf[SequenceNode].children(4)
v222
  }
}
def matchAnyName(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 69 =>
val v223 = body.asInstanceOf[SequenceNode].children(8)
v223
  }
}
def matchTypeName(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 43 =>
val v224 = body.asInstanceOf[SequenceNode].children(0)
v224
  }
}
def matchDomain(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 43 =>
val v225 = body.asInstanceOf[SequenceNode].children(0)
v225
  }
}
def matchLetter(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 53 =>
val v226 = body.asInstanceOf[SequenceNode].children(0)
v226
  }
}
def matchOct(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 172 =>
val v227 = body.asInstanceOf[SequenceNode].children(0)
v227
  }
}
def matchDec(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 59 =>
val v228 = body.asInstanceOf[SequenceNode].children(0)
v228
  }
}
def matchHex(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 186 =>
val v229 = body.asInstanceOf[SequenceNode].children(0)
v229
  }
}
def matchChar(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 25 =>
val v230 = body.asInstanceOf[SequenceNode].children(0)
v230
  }
}
def matchNewline(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 16 =>
val v231 = body.asInstanceOf[SequenceNode].children(0)
v231
  }
}
def matchCOMMENT(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 19 =>
// UnrollChoices
body
  }
}
def matchWhitespace(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 12 =>
val v232 = body.asInstanceOf[SequenceNode].children(0)
v232
case 14 =>
val v233 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v234, v235) = v233
assert(v234.id == 15)
val v236 = matchNewline(v235)
v236
  }
}
def matchWS(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 5 =>
val v237 = body.asInstanceOf[SequenceNode].children(0)
v237
  }
}
def matchIDENT(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 48 =>
val v238 = body.asInstanceOf[SequenceNode].children(0)
v238
  }
}
def matchDec_lit(node: Node): DecLit = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 139 =>
val v239 = body.asInstanceOf[SequenceNode].children(0)
val v240 = List(v239)
val v241 = DecLit(node,v240)
v241
case 141 =>
val v242 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v243, v244) = v242
assert(v243.id == 142)
val v245 = List(v244)
val v246 = body.asInstanceOf[SequenceNode].children(1)
val v250 = unrollRepeat0(v246) map { n =>
val BindNode(v247, v248) = n
assert(v247.id == 58)
val v249 = matchDec(v248)
v249
}
val v251 = v250 map { n =>
n
}
val v252 = v245 ++ v251
val v253 = DecLit(node,v252)
v253
  }
}
def matchFloat_lit(node: Node): FloatLit = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 128 =>
val v254 = List()
val v255 = DecLit(node,v254)
val v256 = body.asInstanceOf[SequenceNode].children(1)
val v260 = unrollRepeat1(v256) map { n =>
val BindNode(v257, v258) = n
assert(v257.id == 58)
val v259 = matchDec(v258)
v259
}
val v261 = v260 map { n =>
n
}
val v262 = DecLit(node,v261)
val v263 = body.asInstanceOf[SequenceNode].children(2)
val v267 = unrollOptional(v263, 30, 132) map { n =>
val BindNode(v264, v265) = n
assert(v264.id == 132)
val v266 = matchExp(v265)
v266
}
val v268 = v267 map { n =>
n
}
val v269 = FloatLit(node,v255,v262,v268)
v269
case 137 =>
val v270 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v271, v272) = v270
assert(v271.id == 138)
val v273 = matchDec_lit(v272)
val v274 = body.asInstanceOf[SequenceNode].children(2)
val v278 = unrollRepeat0(v274) map { n =>
val BindNode(v275, v276) = n
assert(v275.id == 58)
val v277 = matchDec(v276)
v277
}
val v279 = v278 map { n =>
n
}
val v280 = DecLit(node,v279)
val v281 = body.asInstanceOf[SequenceNode].children(3)
val v285 = unrollOptional(v281, 30, 132) map { n =>
val BindNode(v282, v283) = n
assert(v282.id == 132)
val v284 = matchExp(v283)
v284
}
val v286 = v285 map { n =>
n
}
val v287 = FloatLit(node,v273,v280,v286)
v287
case 145 =>
val v288 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v289, v290) = v288
assert(v289.id == 138)
val v291 = matchDec_lit(v290)
val v292 = List()
val v293 = DecLit(node,v292)
val v294 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v295, v296) = v294
assert(v295.id == 132)
val v297 = matchExp(v296)
val v298 = FloatLit(node,v291,v293,Some(v297))
v298
  }
}
def matchExp(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 133 =>
val v299 = body.asInstanceOf[SequenceNode].children(2)
val v303 = unrollRepeat1(v299) map { n =>
val BindNode(v300, v301) = n
assert(v300.id == 58)
val v302 = matchDec(v301)
v302
}
v303
  }
}
def matchDEC_INT(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 160 =>
val v304 = body.asInstanceOf[SequenceNode].children(0)
v304
  }
}
def matchOCT_INT(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 166 =>
val v305 = body.asInstanceOf[SequenceNode].children(0)
v305
  }
}
def matchHEX_INT(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 179 =>
val v306 = body.asInstanceOf[SequenceNode].children(0)
v306
  }
}
def matchFLOAT(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 123 =>
val v307 = body.asInstanceOf[SequenceNode].children(0)
v307
case 147 =>
val v308 = body.asInstanceOf[SequenceNode].children(0)
v308
  }
}
def matchSTRING(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 83 =>
val v309 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v310, v311) = v309
assert(v310.id == 84)
val v312 = matchSingle_string(v311)
v312
case 101 =>
val v313 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v314, v315) = v313
assert(v314.id == 102)
val v316 = matchDouble_string(v315)
v316
  }
}
def matchSingle_string(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 85 =>
val v317 = body.asInstanceOf[SequenceNode].children(2)
v317
  }
}
def matchDouble_string(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 103 =>
val v318 = body.asInstanceOf[SequenceNode].children(2)
v318
  }
}
def matchEscape(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 92 =>
val v319 = body.asInstanceOf[SequenceNode].children(1)
v319
case 95 =>
val v320 = body.asInstanceOf[SequenceNode].children(1)
v320
case 96 =>
val v321 = body.asInstanceOf[SequenceNode].children(1)
v321
  }
}
def matchStart(node: Node): Message = {
  val BindNode(start, BindNode(startNonterm, body)) = node
  assert(start.id == 1)
  assert(startNonterm.id == 2)
  matchMessage(body)
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

def parseAst(text: String): Either[Message, ParsingErrors.ParsingError] =
  parse(text) match {
    case Left(ctx) =>
      val tree = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
      tree match {
        case Some(forest) if forest.trees.size == 1 =>
          Left(matchStart(forest.trees.head))
        case Some(forest) =>
          Right(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size))
        case None =>
          val expectedTerms = ctx.nextGraph.nodes.flatMap { node =>
            node.kernel.symbol match {
              case NGrammar.NTerminal(_, term) => Some(term)
              case _ => None
            }
          }
          Right(ParsingErrors.UnexpectedEOF(expectedTerms, text.length))
      }
    case Right(error) => Right(error)
  }
  def main(args: Array[String]): Unit = {
    val ast = parseAst(
      """field1 {
        |  child1 {
        |    hello: "world"
        |    my: 123
        |    your: 456.789f
        |  }
        |  x: 'helllo~'
        |}
        |""".stripMargin).left.get
    println(ast.prettyPrint())
  }
}