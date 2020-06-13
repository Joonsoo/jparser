package com.giyeok.jparser.metalang2.generated

import com.giyeok.jparser.Inputs.InputToShortString
import com.giyeok.jparser.ParseResultTree.{JoinNode, Node, BindNode, TerminalNode, SequenceNode}
import com.giyeok.jparser.nparser.{ParseTreeConstructor, NaiveParser, Parser}
import com.giyeok.jparser.{NGrammar, ParsingErrors, ParseForestFunc, Symbols}
import scala.collection.immutable.ListSet

object MetaGrammar3Ast {
  val ngrammar = new NGrammar(
  Map(1 -> NGrammar.NStart(1, 2),
2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("Grammar"), Set(3)),
4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("WS"), Set(5)),
6 -> NGrammar.NRepeat(6, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0), 7, 8),
9 -> NGrammar.NOneOf(9, Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), Set(10,11)),
10 -> NGrammar.NTerminal(10, Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet)),
11 -> NGrammar.NNonterminal(11, Symbols.Nonterminal("LineComment"), Set(12)),
13 -> NGrammar.NTerminal(13, Symbols.ExactChar('/')),
14 -> NGrammar.NRepeat(14, Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0), 7, 15),
16 -> NGrammar.NExcept(16, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 17, 18),
17 -> NGrammar.NTerminal(17, Symbols.AnyChar),
18 -> NGrammar.NTerminal(18, Symbols.ExactChar('\n')),
19 -> NGrammar.NOneOf(19, Symbols.OneOf(ListSet(Symbols.Nonterminal("EOF"),Symbols.ExactChar('\n'))), Set(20,18)),
20 -> NGrammar.NNonterminal(20, Symbols.Nonterminal("EOF"), Set(21)),
22 -> NGrammar.NLookaheadExcept(22, Symbols.LookaheadExcept(Symbols.AnyChar), 7, 17),
23 -> NGrammar.NNonterminal(23, Symbols.Nonterminal("Def"), Set(24,104)),
25 -> NGrammar.NNonterminal(25, Symbols.Nonterminal("Rule"), Set(26)),
27 -> NGrammar.NNonterminal(27, Symbols.Nonterminal("LHS"), Set(28)),
29 -> NGrammar.NNonterminal(29, Symbols.Nonterminal("Nonterminal"), Set(30)),
31 -> NGrammar.NNonterminal(31, Symbols.Nonterminal("NonterminalName"), Set(32,77)),
33 -> NGrammar.NExcept(33, Symbols.Except(Symbols.Nonterminal("Id"), Symbols.Nonterminal("Keyword")), 34, 43),
34 -> NGrammar.NNonterminal(34, Symbols.Nonterminal("Id"), Set(35)),
36 -> NGrammar.NLongest(36, Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))), 37),
37 -> NGrammar.NProxy(37, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))), 38),
39 -> NGrammar.NTerminal(39, Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
40 -> NGrammar.NRepeat(40, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), 7, 41),
42 -> NGrammar.NTerminal(42, Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
43 -> NGrammar.NNonterminal(43, Symbols.Nonterminal("Keyword"), Set(44,53,59,66,70,74)),
45 -> NGrammar.NProxy(45, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'),Symbols.ExactChar('o'),Symbols.ExactChar('o'),Symbols.ExactChar('l'),Symbols.ExactChar('e'),Symbols.ExactChar('a'),Symbols.ExactChar('n')))), 46),
47 -> NGrammar.NTerminal(47, Symbols.ExactChar('b')),
48 -> NGrammar.NTerminal(48, Symbols.ExactChar('o')),
49 -> NGrammar.NTerminal(49, Symbols.ExactChar('l')),
50 -> NGrammar.NTerminal(50, Symbols.ExactChar('e')),
51 -> NGrammar.NTerminal(51, Symbols.ExactChar('a')),
52 -> NGrammar.NTerminal(52, Symbols.ExactChar('n')),
54 -> NGrammar.NProxy(54, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('c'),Symbols.ExactChar('h'),Symbols.ExactChar('a'),Symbols.ExactChar('r')))), 55),
56 -> NGrammar.NTerminal(56, Symbols.ExactChar('c')),
57 -> NGrammar.NTerminal(57, Symbols.ExactChar('h')),
58 -> NGrammar.NTerminal(58, Symbols.ExactChar('r')),
60 -> NGrammar.NProxy(60, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'),Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('i'),Symbols.ExactChar('n'),Symbols.ExactChar('g')))), 61),
62 -> NGrammar.NTerminal(62, Symbols.ExactChar('s')),
63 -> NGrammar.NTerminal(63, Symbols.ExactChar('t')),
64 -> NGrammar.NTerminal(64, Symbols.ExactChar('i')),
65 -> NGrammar.NTerminal(65, Symbols.ExactChar('g')),
67 -> NGrammar.NProxy(67, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))), 68),
69 -> NGrammar.NTerminal(69, Symbols.ExactChar('u')),
71 -> NGrammar.NProxy(71, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))), 72),
73 -> NGrammar.NTerminal(73, Symbols.ExactChar('f')),
75 -> NGrammar.NProxy(75, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'),Symbols.ExactChar('u'),Symbols.ExactChar('l'),Symbols.ExactChar('l')))), 76),
78 -> NGrammar.NTerminal(78, Symbols.ExactChar('`')),
79 -> NGrammar.NOneOf(79, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))), Set(80,81)),
80 -> NGrammar.NProxy(80, Symbols.Proxy(Symbols.Sequence(Seq())), 7),
81 -> NGrammar.NProxy(81, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))), 82),
83 -> NGrammar.NTerminal(83, Symbols.ExactChar(':')),
84 -> NGrammar.NNonterminal(84, Symbols.Nonterminal("TypeDesc"), Set(85)),
86 -> NGrammar.NNonterminal(86, Symbols.Nonterminal("NonNullTypeDesc"), Set(87,89,92,94,100,104)),
88 -> NGrammar.NNonterminal(88, Symbols.Nonterminal("TypeName"), Set(32,77)),
90 -> NGrammar.NTerminal(90, Symbols.ExactChar('[')),
91 -> NGrammar.NTerminal(91, Symbols.ExactChar(']')),
93 -> NGrammar.NNonterminal(93, Symbols.Nonterminal("ValueType"), Set(44,53,59)),
95 -> NGrammar.NNonterminal(95, Symbols.Nonterminal("AnyType"), Set(96)),
97 -> NGrammar.NProxy(97, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('n'),Symbols.ExactChar('y')))), 98),
99 -> NGrammar.NTerminal(99, Symbols.ExactChar('y')),
101 -> NGrammar.NNonterminal(101, Symbols.Nonterminal("EnumTypeName"), Set(102)),
103 -> NGrammar.NTerminal(103, Symbols.ExactChar('%')),
105 -> NGrammar.NNonterminal(105, Symbols.Nonterminal("TypeDef"), Set(106,136,151)),
107 -> NGrammar.NNonterminal(107, Symbols.Nonterminal("ClassDef"), Set(108,121)),
109 -> NGrammar.NNonterminal(109, Symbols.Nonterminal("SuperTypes"), Set(110)),
111 -> NGrammar.NTerminal(111, Symbols.ExactChar('<')),
112 -> NGrammar.NOneOf(112, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Nonterminal("WS")))))), Set(80,113)),
113 -> NGrammar.NProxy(113, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Nonterminal("WS")))), 114),
115 -> NGrammar.NRepeat(115, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0), 7, 116),
117 -> NGrammar.NProxy(117, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 118),
119 -> NGrammar.NTerminal(119, Symbols.ExactChar(',')),
120 -> NGrammar.NTerminal(120, Symbols.ExactChar('>')),
122 -> NGrammar.NNonterminal(122, Symbols.Nonterminal("ClassParamsDef"), Set(123)),
124 -> NGrammar.NTerminal(124, Symbols.ExactChar('(')),
125 -> NGrammar.NOneOf(125, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0),Symbols.Nonterminal("WS")))))), Set(80,126)),
126 -> NGrammar.NProxy(126, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0),Symbols.Nonterminal("WS")))), 127),
128 -> NGrammar.NNonterminal(128, Symbols.Nonterminal("ClassParamDef"), Set(129)),
130 -> NGrammar.NNonterminal(130, Symbols.Nonterminal("ParamName"), Set(32,77)),
131 -> NGrammar.NRepeat(131, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0), 7, 132),
133 -> NGrammar.NProxy(133, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 134),
135 -> NGrammar.NTerminal(135, Symbols.ExactChar(')')),
137 -> NGrammar.NNonterminal(137, Symbols.Nonterminal("SuperDef"), Set(138)),
139 -> NGrammar.NTerminal(139, Symbols.ExactChar('{')),
140 -> NGrammar.NOneOf(140, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubTypes")))))), Set(80,141)),
141 -> NGrammar.NProxy(141, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubTypes")))), 142),
143 -> NGrammar.NNonterminal(143, Symbols.Nonterminal("SubTypes"), Set(144)),
145 -> NGrammar.NNonterminal(145, Symbols.Nonterminal("SubType"), Set(87,106,136)),
146 -> NGrammar.NRepeat(146, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0), 7, 147),
148 -> NGrammar.NProxy(148, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 149),
150 -> NGrammar.NTerminal(150, Symbols.ExactChar('}')),
152 -> NGrammar.NNonterminal(152, Symbols.Nonterminal("EnumTypeDef"), Set(153)),
154 -> NGrammar.NRepeat(154, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 0), 7, 155),
156 -> NGrammar.NProxy(156, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 157),
158 -> NGrammar.NOneOf(158, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))))), Set(80,159)),
159 -> NGrammar.NProxy(159, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))), 160),
161 -> NGrammar.NTerminal(161, Symbols.ExactChar('?')),
162 -> NGrammar.NTerminal(162, Symbols.ExactChar('=')),
163 -> NGrammar.NNonterminal(163, Symbols.Nonterminal("RHS"), Set(164)),
165 -> NGrammar.NNonterminal(165, Symbols.Nonterminal("Elem"), Set(166,251)),
167 -> NGrammar.NNonterminal(167, Symbols.Nonterminal("Symbol"), Set(168)),
169 -> NGrammar.NNonterminal(169, Symbols.Nonterminal("BinSymbol"), Set(170,249,250)),
171 -> NGrammar.NTerminal(171, Symbols.ExactChar('&')),
172 -> NGrammar.NNonterminal(172, Symbols.Nonterminal("PreUnSymbol"), Set(173,175,177)),
174 -> NGrammar.NTerminal(174, Symbols.ExactChar('^')),
176 -> NGrammar.NTerminal(176, Symbols.ExactChar('!')),
178 -> NGrammar.NNonterminal(178, Symbols.Nonterminal("PostUnSymbol"), Set(179,180,182,184)),
181 -> NGrammar.NTerminal(181, Symbols.ExactChar('*')),
183 -> NGrammar.NTerminal(183, Symbols.ExactChar('+')),
185 -> NGrammar.NNonterminal(185, Symbols.Nonterminal("AtomSymbol"), Set(186,202,220,232,233,242,245)),
187 -> NGrammar.NNonterminal(187, Symbols.Nonterminal("Terminal"), Set(188,200)),
189 -> NGrammar.NTerminal(189, Symbols.ExactChar('\'')),
190 -> NGrammar.NNonterminal(190, Symbols.Nonterminal("TerminalChar"), Set(191,194,196)),
192 -> NGrammar.NExcept(192, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')), 17, 193),
193 -> NGrammar.NTerminal(193, Symbols.ExactChar('\\')),
195 -> NGrammar.NTerminal(195, Symbols.Chars(Set('\'','\\','b','n','r','t'))),
197 -> NGrammar.NNonterminal(197, Symbols.Nonterminal("UnicodeChar"), Set(198)),
199 -> NGrammar.NTerminal(199, Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet)),
201 -> NGrammar.NTerminal(201, Symbols.ExactChar('.')),
203 -> NGrammar.NNonterminal(203, Symbols.Nonterminal("TerminalChoice"), Set(204,219)),
205 -> NGrammar.NNonterminal(205, Symbols.Nonterminal("TerminalChoiceElem"), Set(206,213)),
207 -> NGrammar.NNonterminal(207, Symbols.Nonterminal("TerminalChoiceChar"), Set(208,211,196)),
209 -> NGrammar.NExcept(209, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))), 17, 210),
210 -> NGrammar.NTerminal(210, Symbols.Chars(Set('\'','-','\\'))),
212 -> NGrammar.NTerminal(212, Symbols.Chars(Set('\'','-','\\','b','n','r','t'))),
214 -> NGrammar.NNonterminal(214, Symbols.Nonterminal("TerminalChoiceRange"), Set(215)),
216 -> NGrammar.NTerminal(216, Symbols.ExactChar('-')),
217 -> NGrammar.NRepeat(217, Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), 205, 218),
221 -> NGrammar.NNonterminal(221, Symbols.Nonterminal("StringSymbol"), Set(222)),
223 -> NGrammar.NTerminal(223, Symbols.ExactChar('"')),
224 -> NGrammar.NRepeat(224, Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), 7, 225),
226 -> NGrammar.NNonterminal(226, Symbols.Nonterminal("StringChar"), Set(227,230,196)),
228 -> NGrammar.NExcept(228, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))), 17, 229),
229 -> NGrammar.NTerminal(229, Symbols.Chars(Set('"','\\'))),
231 -> NGrammar.NTerminal(231, Symbols.Chars(Set('"','\\','b','n','r','t'))),
234 -> NGrammar.NNonterminal(234, Symbols.Nonterminal("InPlaceChoices"), Set(235)),
236 -> NGrammar.NNonterminal(236, Symbols.Nonterminal("InPlaceSequence"), Set(164)),
237 -> NGrammar.NRepeat(237, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 0), 7, 238),
239 -> NGrammar.NProxy(239, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 240),
241 -> NGrammar.NTerminal(241, Symbols.ExactChar('|')),
243 -> NGrammar.NNonterminal(243, Symbols.Nonterminal("Longest"), Set(244)),
246 -> NGrammar.NNonterminal(246, Symbols.Nonterminal("EmptySequence"), Set(247)),
248 -> NGrammar.NTerminal(248, Symbols.ExactChar('#')),
252 -> NGrammar.NNonterminal(252, Symbols.Nonterminal("Processor"), Set(253,281)),
254 -> NGrammar.NNonterminal(254, Symbols.Nonterminal("Ref"), Set(255,276)),
256 -> NGrammar.NNonterminal(256, Symbols.Nonterminal("ValRef"), Set(257)),
258 -> NGrammar.NTerminal(258, Symbols.ExactChar('$')),
259 -> NGrammar.NOneOf(259, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))), Set(80,260)),
260 -> NGrammar.NNonterminal(260, Symbols.Nonterminal("CondSymPath"), Set(261)),
262 -> NGrammar.NRepeat(262, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1), 263, 264),
263 -> NGrammar.NOneOf(263, Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), Set(111,120)),
265 -> NGrammar.NNonterminal(265, Symbols.Nonterminal("RefIdx"), Set(266)),
267 -> NGrammar.NLongest(267, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))), 268),
268 -> NGrammar.NOneOf(268, Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))))), Set(269,270)),
269 -> NGrammar.NTerminal(269, Symbols.ExactChar('0')),
270 -> NGrammar.NProxy(270, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))), 271),
272 -> NGrammar.NTerminal(272, Symbols.Chars(('1' to '9').toSet)),
273 -> NGrammar.NRepeat(273, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 7, 274),
275 -> NGrammar.NTerminal(275, Symbols.Chars(('0' to '9').toSet)),
277 -> NGrammar.NNonterminal(277, Symbols.Nonterminal("RawRef"), Set(278)),
279 -> NGrammar.NProxy(279, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$')))), 280),
282 -> NGrammar.NNonterminal(282, Symbols.Nonterminal("PExpr"), Set(283)),
284 -> NGrammar.NNonterminal(284, Symbols.Nonterminal("TernaryExpr"), Set(285)),
286 -> NGrammar.NNonterminal(286, Symbols.Nonterminal("BoolOrExpr"), Set(287,369)),
288 -> NGrammar.NNonterminal(288, Symbols.Nonterminal("BoolAndExpr"), Set(289,366)),
290 -> NGrammar.NNonterminal(290, Symbols.Nonterminal("BoolEqExpr"), Set(291,363)),
292 -> NGrammar.NNonterminal(292, Symbols.Nonterminal("ElvisExpr"), Set(293,357)),
294 -> NGrammar.NNonterminal(294, Symbols.Nonterminal("AdditiveExpr"), Set(295,354)),
296 -> NGrammar.NNonterminal(296, Symbols.Nonterminal("PrefixNotExpr"), Set(297,298)),
299 -> NGrammar.NNonterminal(299, Symbols.Nonterminal("Atom"), Set(253,300,304,315,328,331,343,353)),
301 -> NGrammar.NNonterminal(301, Symbols.Nonterminal("BindExpr"), Set(302)),
303 -> NGrammar.NNonterminal(303, Symbols.Nonterminal("BinderExpr"), Set(253,300,281)),
305 -> NGrammar.NNonterminal(305, Symbols.Nonterminal("NamedConstructExpr"), Set(306)),
307 -> NGrammar.NNonterminal(307, Symbols.Nonterminal("NamedConstructParams"), Set(308)),
309 -> NGrammar.NNonterminal(309, Symbols.Nonterminal("NamedParam"), Set(310)),
311 -> NGrammar.NRepeat(311, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0), 7, 312),
313 -> NGrammar.NProxy(313, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 314),
316 -> NGrammar.NNonterminal(316, Symbols.Nonterminal("FuncCallOrConstructExpr"), Set(317)),
318 -> NGrammar.NNonterminal(318, Symbols.Nonterminal("TypeOrFuncName"), Set(32,77)),
319 -> NGrammar.NNonterminal(319, Symbols.Nonterminal("CallParams"), Set(320)),
321 -> NGrammar.NOneOf(321, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))), Set(80,322)),
322 -> NGrammar.NProxy(322, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))), 323),
324 -> NGrammar.NRepeat(324, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0), 7, 325),
326 -> NGrammar.NProxy(326, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 327),
329 -> NGrammar.NNonterminal(329, Symbols.Nonterminal("ArrayExpr"), Set(330)),
332 -> NGrammar.NNonterminal(332, Symbols.Nonterminal("Literal"), Set(74,333,335,338)),
334 -> NGrammar.NOneOf(334, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))))), Set(67,71)),
336 -> NGrammar.NNonterminal(336, Symbols.Nonterminal("CharChar"), Set(337)),
339 -> NGrammar.NRepeat(339, Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), 7, 340),
341 -> NGrammar.NNonterminal(341, Symbols.Nonterminal("StrChar"), Set(342)),
344 -> NGrammar.NNonterminal(344, Symbols.Nonterminal("EnumValue"), Set(345,350)),
346 -> NGrammar.NNonterminal(346, Symbols.Nonterminal("CanonicalEnumValue"), Set(347)),
348 -> NGrammar.NNonterminal(348, Symbols.Nonterminal("EnumValueName"), Set(349)),
351 -> NGrammar.NNonterminal(351, Symbols.Nonterminal("ShortenedEnumValue"), Set(352)),
355 -> NGrammar.NProxy(355, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':')))), 356),
358 -> NGrammar.NOneOf(358, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))))), Set(359,361)),
359 -> NGrammar.NProxy(359, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))), 360),
361 -> NGrammar.NProxy(361, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))), 362),
364 -> NGrammar.NProxy(364, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|')))), 365),
367 -> NGrammar.NProxy(367, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&')))), 368),
370 -> NGrammar.NRepeat(370, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0), 7, 371),
372 -> NGrammar.NProxy(372, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 373),
374 -> NGrammar.NRepeat(374, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0), 7, 375),
376 -> NGrammar.NProxy(376, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 377),
378 -> NGrammar.NRepeat(378, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 0), 7, 379),
380 -> NGrammar.NProxy(380, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 381),
382 -> NGrammar.NNonterminal(382, Symbols.Nonterminal("WSNL"), Set(383))),
  Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 0),Symbols.Nonterminal("WS"))), Seq(4,23,378,4)),
5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0))), Seq(6)),
7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq()), Seq()),
8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0),Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))))), Seq(6,9)),
12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.ExactChar('/'),Symbols.ExactChar('/'),Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0),Symbols.OneOf(ListSet(Symbols.Nonterminal("EOF"),Symbols.ExactChar('\n'))))), Seq(13,13,14,19)),
15 -> NGrammar.NSequence(15, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0),Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')))), Seq(14,16)),
21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.LookaheadExcept(Symbols.AnyChar))), Seq(22)),
24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Nonterminal("Rule"))), Seq(25)),
26 -> NGrammar.NSequence(26, Symbols.Sequence(Seq(Symbols.Nonterminal("LHS"),Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0))), Seq(27,4,162,4,163,374)),
28 -> NGrammar.NSequence(28, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))))), Seq(29,79)),
30 -> NGrammar.NSequence(30, Symbols.Sequence(Seq(Symbols.Nonterminal("NonterminalName"))), Seq(31)),
32 -> NGrammar.NSequence(32, Symbols.Sequence(Seq(Symbols.Except(Symbols.Nonterminal("Id"), Symbols.Nonterminal("Keyword")))), Seq(33)),
35 -> NGrammar.NSequence(35, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))), Seq(36)),
38 -> NGrammar.NSequence(38, Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))), Seq(39,40)),
41 -> NGrammar.NSequence(41, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0),Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(40,42)),
44 -> NGrammar.NSequence(44, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'),Symbols.ExactChar('o'),Symbols.ExactChar('o'),Symbols.ExactChar('l'),Symbols.ExactChar('e'),Symbols.ExactChar('a'),Symbols.ExactChar('n')))))), Seq(45)),
46 -> NGrammar.NSequence(46, Symbols.Sequence(Seq(Symbols.ExactChar('b'),Symbols.ExactChar('o'),Symbols.ExactChar('o'),Symbols.ExactChar('l'),Symbols.ExactChar('e'),Symbols.ExactChar('a'),Symbols.ExactChar('n'))), Seq(47,48,48,49,50,51,52)),
53 -> NGrammar.NSequence(53, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('c'),Symbols.ExactChar('h'),Symbols.ExactChar('a'),Symbols.ExactChar('r')))))), Seq(54)),
55 -> NGrammar.NSequence(55, Symbols.Sequence(Seq(Symbols.ExactChar('c'),Symbols.ExactChar('h'),Symbols.ExactChar('a'),Symbols.ExactChar('r'))), Seq(56,57,51,58)),
59 -> NGrammar.NSequence(59, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'),Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('i'),Symbols.ExactChar('n'),Symbols.ExactChar('g')))))), Seq(60)),
61 -> NGrammar.NSequence(61, Symbols.Sequence(Seq(Symbols.ExactChar('s'),Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('i'),Symbols.ExactChar('n'),Symbols.ExactChar('g'))), Seq(62,63,58,64,52,65)),
66 -> NGrammar.NSequence(66, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))))), Seq(67)),
68 -> NGrammar.NSequence(68, Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e'))), Seq(63,58,69,50)),
70 -> NGrammar.NSequence(70, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))))), Seq(71)),
72 -> NGrammar.NSequence(72, Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e'))), Seq(73,51,49,62,50)),
74 -> NGrammar.NSequence(74, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'),Symbols.ExactChar('u'),Symbols.ExactChar('l'),Symbols.ExactChar('l')))))), Seq(75)),
76 -> NGrammar.NSequence(76, Symbols.Sequence(Seq(Symbols.ExactChar('n'),Symbols.ExactChar('u'),Symbols.ExactChar('l'),Symbols.ExactChar('l'))), Seq(52,69,49,49)),
77 -> NGrammar.NSequence(77, Symbols.Sequence(Seq(Symbols.ExactChar('`'),Symbols.Nonterminal("Id"),Symbols.ExactChar('`'))), Seq(78,34,78)),
82 -> NGrammar.NSequence(82, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc"))), Seq(4,83,4,84)),
85 -> NGrammar.NSequence(85, Symbols.Sequence(Seq(Symbols.Nonterminal("NonNullTypeDesc"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))))))), Seq(86,158)),
87 -> NGrammar.NSequence(87, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"))), Seq(88)),
89 -> NGrammar.NSequence(89, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc"),Symbols.Nonterminal("WS"),Symbols.ExactChar(']'))), Seq(90,4,84,4,91)),
92 -> NGrammar.NSequence(92, Symbols.Sequence(Seq(Symbols.Nonterminal("ValueType"))), Seq(93)),
94 -> NGrammar.NSequence(94, Symbols.Sequence(Seq(Symbols.Nonterminal("AnyType"))), Seq(95)),
96 -> NGrammar.NSequence(96, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('n'),Symbols.ExactChar('y')))))), Seq(97)),
98 -> NGrammar.NSequence(98, Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('n'),Symbols.ExactChar('y'))), Seq(51,52,99)),
100 -> NGrammar.NSequence(100, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"))), Seq(101)),
102 -> NGrammar.NSequence(102, Symbols.Sequence(Seq(Symbols.ExactChar('%'),Symbols.Nonterminal("Id"))), Seq(103,34)),
104 -> NGrammar.NSequence(104, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeDef"))), Seq(105)),
106 -> NGrammar.NSequence(106, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassDef"))), Seq(107)),
108 -> NGrammar.NSequence(108, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SuperTypes"))), Seq(88,4,109)),
110 -> NGrammar.NSequence(110, Symbols.Sequence(Seq(Symbols.ExactChar('<'),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar('>'))), Seq(111,4,112,120)),
114 -> NGrammar.NSequence(114, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Nonterminal("WS"))), Seq(88,115,4)),
116 -> NGrammar.NSequence(116, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))))), Seq(115,117)),
118 -> NGrammar.NSequence(118, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName"))), Seq(4,119,4,88)),
121 -> NGrammar.NSequence(121, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SuperTypes"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamsDef"))), Seq(88,4,109,4,122)),
123 -> NGrammar.NSequence(123, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0),Symbols.Nonterminal("WS")))))),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(124,4,125,4,135)),
127 -> NGrammar.NSequence(127, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0),Symbols.Nonterminal("WS"))), Seq(128,131,4)),
129 -> NGrammar.NSequence(129, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))))), Seq(130,79)),
132 -> NGrammar.NSequence(132, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))))), Seq(131,133)),
134 -> NGrammar.NSequence(134, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef"))), Seq(4,119,4,128)),
136 -> NGrammar.NSequence(136, Symbols.Sequence(Seq(Symbols.Nonterminal("SuperDef"))), Seq(137)),
138 -> NGrammar.NSequence(138, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.ExactChar('{'),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubTypes")))))),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(88,4,139,140,4,150)),
142 -> NGrammar.NSequence(142, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubTypes"))), Seq(4,143)),
144 -> NGrammar.NSequence(144, Symbols.Sequence(Seq(Symbols.Nonterminal("SubType"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0))), Seq(145,146)),
147 -> NGrammar.NSequence(147, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))))), Seq(146,148)),
149 -> NGrammar.NSequence(149, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType"))), Seq(4,119,4,145)),
151 -> NGrammar.NSequence(151, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeDef"))), Seq(152)),
153 -> NGrammar.NSequence(153, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"),Symbols.Nonterminal("WS"),Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 0),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(101,4,139,4,34,154,4,150)),
155 -> NGrammar.NSequence(155, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))))), Seq(154,156)),
157 -> NGrammar.NSequence(157, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id"))), Seq(4,119,4,34)),
160 -> NGrammar.NSequence(160, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?'))), Seq(4,161)),
164 -> NGrammar.NSequence(164, Symbols.Sequence(Seq(Symbols.Nonterminal("Elem"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0))), Seq(165,370)),
166 -> NGrammar.NSequence(166, Symbols.Sequence(Seq(Symbols.Nonterminal("Symbol"))), Seq(167)),
168 -> NGrammar.NSequence(168, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"))), Seq(169)),
170 -> NGrammar.NSequence(170, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('&'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(169,4,171,4,172)),
173 -> NGrammar.NSequence(173, Symbols.Sequence(Seq(Symbols.ExactChar('^'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(174,4,172)),
175 -> NGrammar.NSequence(175, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(176,4,172)),
177 -> NGrammar.NSequence(177, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"))), Seq(178)),
179 -> NGrammar.NSequence(179, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('?'))), Seq(178,4,161)),
180 -> NGrammar.NSequence(180, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('*'))), Seq(178,4,181)),
182 -> NGrammar.NSequence(182, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('+'))), Seq(178,4,183)),
184 -> NGrammar.NSequence(184, Symbols.Sequence(Seq(Symbols.Nonterminal("AtomSymbol"))), Seq(185)),
186 -> NGrammar.NSequence(186, Symbols.Sequence(Seq(Symbols.Nonterminal("Terminal"))), Seq(187)),
188 -> NGrammar.NSequence(188, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChar"),Symbols.ExactChar('\''))), Seq(189,190,189)),
191 -> NGrammar.NSequence(191, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')))), Seq(192)),
194 -> NGrammar.NSequence(194, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('\'','\\','b','n','r','t')))), Seq(193,195)),
196 -> NGrammar.NSequence(196, Symbols.Sequence(Seq(Symbols.Nonterminal("UnicodeChar"))), Seq(197)),
198 -> NGrammar.NSequence(198, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('u'),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet))), Seq(193,69,199,199,199,199)),
200 -> NGrammar.NSequence(200, Symbols.Sequence(Seq(Symbols.ExactChar('.'))), Seq(201)),
202 -> NGrammar.NSequence(202, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoice"))), Seq(203)),
204 -> NGrammar.NSequence(204, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1),Symbols.ExactChar('\''))), Seq(189,205,217,189)),
206 -> NGrammar.NSequence(206, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"))), Seq(207)),
208 -> NGrammar.NSequence(208, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))))), Seq(209)),
211 -> NGrammar.NSequence(211, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('\'','-','\\','b','n','r','t')))), Seq(193,212)),
213 -> NGrammar.NSequence(213, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceRange"))), Seq(214)),
215 -> NGrammar.NSequence(215, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"),Symbols.ExactChar('-'),Symbols.Nonterminal("TerminalChoiceChar"))), Seq(207,216,207)),
218 -> NGrammar.NSequence(218, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1),Symbols.Nonterminal("TerminalChoiceElem"))), Seq(217,205)),
219 -> NGrammar.NSequence(219, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceRange"),Symbols.ExactChar('\''))), Seq(189,214,189)),
220 -> NGrammar.NSequence(220, Symbols.Sequence(Seq(Symbols.Nonterminal("StringSymbol"))), Seq(221)),
222 -> NGrammar.NSequence(222, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.ExactChar('"'))), Seq(223,224,223)),
225 -> NGrammar.NSequence(225, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.Nonterminal("StringChar"))), Seq(224,226)),
227 -> NGrammar.NSequence(227, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))))), Seq(228)),
230 -> NGrammar.NSequence(230, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('"','\\','b','n','r','t')))), Seq(193,231)),
232 -> NGrammar.NSequence(232, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"))), Seq(29)),
233 -> NGrammar.NSequence(233, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceChoices"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(124,4,234,4,135)),
235 -> NGrammar.NSequence(235, Symbols.Sequence(Seq(Symbols.Nonterminal("InPlaceSequence"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 0))), Seq(236,237)),
238 -> NGrammar.NSequence(238, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))))), Seq(237,239)),
240 -> NGrammar.NSequence(240, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence"))), Seq(4,241,4,236)),
242 -> NGrammar.NSequence(242, Symbols.Sequence(Seq(Symbols.Nonterminal("Longest"))), Seq(243)),
244 -> NGrammar.NSequence(244, Symbols.Sequence(Seq(Symbols.ExactChar('<'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceChoices"),Symbols.Nonterminal("WS"),Symbols.ExactChar('>'))), Seq(111,4,234,4,120)),
245 -> NGrammar.NSequence(245, Symbols.Sequence(Seq(Symbols.Nonterminal("EmptySequence"))), Seq(246)),
247 -> NGrammar.NSequence(247, Symbols.Sequence(Seq(Symbols.ExactChar('#'))), Seq(248)),
249 -> NGrammar.NSequence(249, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('-'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(169,4,216,4,172)),
250 -> NGrammar.NSequence(250, Symbols.Sequence(Seq(Symbols.Nonterminal("PreUnSymbol"))), Seq(172)),
251 -> NGrammar.NSequence(251, Symbols.Sequence(Seq(Symbols.Nonterminal("Processor"))), Seq(252)),
253 -> NGrammar.NSequence(253, Symbols.Sequence(Seq(Symbols.Nonterminal("Ref"))), Seq(254)),
255 -> NGrammar.NSequence(255, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"))), Seq(256)),
257 -> NGrammar.NSequence(257, Symbols.Sequence(Seq(Symbols.ExactChar('$'),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))),Symbols.Nonterminal("RefIdx"))), Seq(258,259,265)),
261 -> NGrammar.NSequence(261, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1))), Seq(262)),
264 -> NGrammar.NSequence(264, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1),Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))))), Seq(262,263)),
266 -> NGrammar.NSequence(266, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))))), Seq(267)),
271 -> NGrammar.NSequence(271, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(272,273)),
274 -> NGrammar.NSequence(274, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0),Symbols.Chars(('0' to '9').toSet))), Seq(273,275)),
276 -> NGrammar.NSequence(276, Symbols.Sequence(Seq(Symbols.Nonterminal("RawRef"))), Seq(277)),
278 -> NGrammar.NSequence(278, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$')))),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))),Symbols.Nonterminal("RefIdx"))), Seq(279,259,265)),
280 -> NGrammar.NSequence(280, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$'))), Seq(193,193,258)),
281 -> NGrammar.NSequence(281, Symbols.Sequence(Seq(Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(139,4,282,4,150)),
283 -> NGrammar.NSequence(283, Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))), Seq(284)),
285 -> NGrammar.NSequence(285, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"))), Seq(286)),
287 -> NGrammar.NSequence(287, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolOrExpr"))), Seq(288,4,367,4,286)),
289 -> NGrammar.NSequence(289, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolAndExpr"))), Seq(290,4,364,4,288)),
291 -> NGrammar.NSequence(291, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolEqExpr"))), Seq(292,4,358,4,290)),
293 -> NGrammar.NSequence(293, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ElvisExpr"))), Seq(294,4,355,4,292)),
295 -> NGrammar.NSequence(295, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar('+'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("AdditiveExpr"))), Seq(296,4,183,4,294)),
297 -> NGrammar.NSequence(297, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PrefixNotExpr"))), Seq(176,4,296)),
298 -> NGrammar.NSequence(298, Symbols.Sequence(Seq(Symbols.Nonterminal("Atom"))), Seq(299)),
300 -> NGrammar.NSequence(300, Symbols.Sequence(Seq(Symbols.Nonterminal("BindExpr"))), Seq(301)),
302 -> NGrammar.NSequence(302, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"),Symbols.Nonterminal("BinderExpr"))), Seq(256,303)),
304 -> NGrammar.NSequence(304, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedConstructExpr"))), Seq(305)),
306 -> NGrammar.NSequence(306, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedConstructParams"))), Seq(88,4,307)),
308 -> NGrammar.NSequence(308, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(124,4,309,311,4,135)),
310 -> NGrammar.NSequence(310, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))),Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"))), Seq(130,79,4,162,4,282)),
312 -> NGrammar.NSequence(312, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))))), Seq(311,313)),
314 -> NGrammar.NSequence(314, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam"))), Seq(4,119,4,309)),
315 -> NGrammar.NSequence(315, Symbols.Sequence(Seq(Symbols.Nonterminal("FuncCallOrConstructExpr"))), Seq(316)),
317 -> NGrammar.NSequence(317, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeOrFuncName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("CallParams"))), Seq(318,4,319)),
320 -> NGrammar.NSequence(320, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(')'))), Seq(124,4,321,135)),
323 -> NGrammar.NSequence(323, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS"))), Seq(282,324,4)),
325 -> NGrammar.NSequence(325, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))))), Seq(324,326)),
327 -> NGrammar.NSequence(327, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"))), Seq(4,119,4,282)),
328 -> NGrammar.NSequence(328, Symbols.Sequence(Seq(Symbols.Nonterminal("ArrayExpr"))), Seq(329)),
330 -> NGrammar.NSequence(330, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(']'))), Seq(90,4,321,91)),
331 -> NGrammar.NSequence(331, Symbols.Sequence(Seq(Symbols.Nonterminal("Literal"))), Seq(332)),
333 -> NGrammar.NSequence(333, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))))))), Seq(334)),
335 -> NGrammar.NSequence(335, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("CharChar"),Symbols.ExactChar('\''))), Seq(189,336,189)),
337 -> NGrammar.NSequence(337, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChar"))), Seq(190)),
338 -> NGrammar.NSequence(338, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0),Symbols.ExactChar('"'))), Seq(223,339,223)),
340 -> NGrammar.NSequence(340, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0),Symbols.Nonterminal("StrChar"))), Seq(339,341)),
342 -> NGrammar.NSequence(342, Symbols.Sequence(Seq(Symbols.Nonterminal("StringChar"))), Seq(226)),
343 -> NGrammar.NSequence(343, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumValue"))), Seq(344)),
345 -> NGrammar.NSequence(345, Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue"))), Seq(346)),
347 -> NGrammar.NSequence(347, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"),Symbols.ExactChar('.'),Symbols.Nonterminal("EnumValueName"))), Seq(101,201,348)),
349 -> NGrammar.NSequence(349, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(34)),
350 -> NGrammar.NSequence(350, Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))), Seq(351)),
352 -> NGrammar.NSequence(352, Symbols.Sequence(Seq(Symbols.ExactChar('%'),Symbols.Nonterminal("EnumValueName"))), Seq(103,348)),
353 -> NGrammar.NSequence(353, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(124,4,282,4,135)),
354 -> NGrammar.NSequence(354, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"))), Seq(296)),
356 -> NGrammar.NSequence(356, Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':'))), Seq(161,83)),
357 -> NGrammar.NSequence(357, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"))), Seq(294)),
360 -> NGrammar.NSequence(360, Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('='))), Seq(162,162)),
362 -> NGrammar.NSequence(362, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('='))), Seq(176,162)),
363 -> NGrammar.NSequence(363, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"))), Seq(292)),
365 -> NGrammar.NSequence(365, Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|'))), Seq(241,241)),
366 -> NGrammar.NSequence(366, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"))), Seq(290)),
368 -> NGrammar.NSequence(368, Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&'))), Seq(171,171)),
369 -> NGrammar.NSequence(369, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"))), Seq(288)),
371 -> NGrammar.NSequence(371, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))))), Seq(370,372)),
373 -> NGrammar.NSequence(373, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem"))), Seq(4,165)),
375 -> NGrammar.NSequence(375, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))))), Seq(374,376)),
377 -> NGrammar.NSequence(377, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS"))), Seq(4,241,4,163)),
379 -> NGrammar.NSequence(379, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))))), Seq(378,380)),
381 -> NGrammar.NSequence(381, Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def"))), Seq(382,23)),
383 -> NGrammar.NSequence(383, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"))), Seq(4))),
  1)

  sealed trait ASTNode {
  val astNode: Node
  def prettyPrint(): String
}
case class Grammar(astNode:Node, defs:List[Def]) extends ASTNode{
  def prettyPrint(): String = "Grammar(" + "defs=" + "[" + defs.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
sealed trait Def extends ASTNode
case class Rule(astNode:Node, lhs:LHS, rhs:List[RHS]) extends ASTNode with Def{
  def prettyPrint(): String = "Rule(" + "lhs=" + lhs.prettyPrint()+ ", " + "rhs=" + "[" + rhs.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
case class LHS(astNode:Node, name:Nonterminal, typeDesc:Option[TypeDesc]) extends ASTNode{
  def prettyPrint(): String = "LHS(" + "name=" + name.prettyPrint()+ ", " + "typeDesc=" + (typeDesc match { case Some(v) =>
  v.prettyPrint()
  case None => "null"
}) +  ")"
}
case class RHS(astNode:Node, elems:List[Elem]) extends ASTNode{
  def prettyPrint(): String = "RHS(" + "elems=" + "[" + elems.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
sealed trait Elem extends ASTNode
sealed trait Symbol extends ASTNode with Elem
sealed trait BinSymbol extends ASTNode with Symbol
case class JoinSymbol(astNode:Node, body:BinSymbol, join:PreUnSymbol) extends ASTNode with BinSymbol{
  def prettyPrint(): String = "JoinSymbol(" + "body=" + body.prettyPrint()+ ", " + "join=" + join.prettyPrint() +  ")"
}
case class ExceptSymbol(astNode:Node, body:BinSymbol, except:PreUnSymbol) extends ASTNode with BinSymbol{
  def prettyPrint(): String = "ExceptSymbol(" + "body=" + body.prettyPrint()+ ", " + "except=" + except.prettyPrint() +  ")"
}
sealed trait PreUnSymbol extends ASTNode with BinSymbol
case class FollowedBy(astNode:Node, followedBy:PreUnSymbol) extends ASTNode with PreUnSymbol{
  def prettyPrint(): String = "FollowedBy(" + "followedBy=" + followedBy.prettyPrint() +  ")"
}
case class NotFollowedBy(astNode:Node, notFollowedBy:PreUnSymbol) extends ASTNode with PreUnSymbol{
  def prettyPrint(): String = "NotFollowedBy(" + "notFollowedBy=" + notFollowedBy.prettyPrint() +  ")"
}
sealed trait PostUnSymbol extends ASTNode with PreUnSymbol
case class Optional(astNode:Node, body:PostUnSymbol) extends ASTNode with PostUnSymbol{
  def prettyPrint(): String = "Optional(" + "body=" + body.prettyPrint() +  ")"
}
case class RepeatFromZero(astNode:Node, body:PostUnSymbol) extends ASTNode with PostUnSymbol{
  def prettyPrint(): String = "RepeatFromZero(" + "body=" + body.prettyPrint() +  ")"
}
case class RepeatFromOne(astNode:Node, body:PostUnSymbol) extends ASTNode with PostUnSymbol{
  def prettyPrint(): String = "RepeatFromOne(" + "body=" + body.prettyPrint() +  ")"
}
sealed trait AtomSymbol extends ASTNode with PostUnSymbol
sealed trait Terminal extends ASTNode with AtomSymbol
case class AnyTerminal(astNode:Node) extends ASTNode with Terminal{
  def prettyPrint(): String = "AnyTerminal(" +  ")"
}
case class TerminalChoice(astNode:Node, choices:List[TerminalChoiceElem]) extends ASTNode with AtomSymbol{
  def prettyPrint(): String = "TerminalChoice(" + "choices=" + "[" + choices.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
sealed trait TerminalChoiceElem extends ASTNode
case class TerminalChoiceRange(astNode:Node, start:TerminalChoiceChar, end:TerminalChoiceChar) extends ASTNode with TerminalChoiceElem{
  def prettyPrint(): String = "TerminalChoiceRange(" + "start=" + start.prettyPrint()+ ", " + "end=" + end.prettyPrint() +  ")"
}
case class StringSymbol(astNode:Node, value:List[StringChar]) extends ASTNode with AtomSymbol{
  def prettyPrint(): String = "StringSymbol(" + "value=" + "[" + value.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
case class Nonterminal(astNode:Node, name:NonterminalName) extends ASTNode with AtomSymbol{
  def prettyPrint(): String = "Nonterminal(" + "name=" + name.prettyPrint() +  ")"
}
case class InPlaceChoices(astNode:Node, choices:List[InPlaceSequence]) extends ASTNode with AtomSymbol{
  def prettyPrint(): String = "InPlaceChoices(" + "choices=" + "[" + choices.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
case class InPlaceSequence(astNode:Node, seq:List[Elem]) extends ASTNode with Symbol{
  def prettyPrint(): String = "InPlaceSequence(" + "seq=" + "[" + seq.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
case class Longest(astNode:Node, choices:InPlaceChoices) extends ASTNode with AtomSymbol{
  def prettyPrint(): String = "Longest(" + "choices=" + choices.prettyPrint() +  ")"
}
case class EmptySeq(astNode:Node) extends ASTNode with AtomSymbol{
  def prettyPrint(): String = "EmptySeq(" +  ")"
}
sealed trait TerminalChar extends ASTNode with Terminal
case class CharAsIs(astNode:Node, value:Node) extends ASTNode with StringChar with TerminalChar with TerminalChoiceChar{
  def prettyPrint(): String = "CharAsIs(" + "value=" + value.sourceText +  ")"
}
case class CharEscaped(astNode:Node, escapeCode:Node) extends ASTNode with StringChar with TerminalChar with TerminalChoiceChar{
  def prettyPrint(): String = "CharEscaped(" + "escapeCode=" + escapeCode.sourceText +  ")"
}
sealed trait TerminalChoiceChar extends ASTNode with TerminalChoiceElem
sealed trait StringChar extends ASTNode
case class CharUnicode(astNode:Node, code:List[Node]) extends ASTNode with StringChar with TerminalChar with TerminalChoiceChar{
  def prettyPrint(): String = "CharUnicode(" + "code=" + "[" + code.map(e => e.sourceText).mkString(",") + "]" +  ")"
}
sealed trait Processor extends ASTNode with Elem
sealed trait Ref extends ASTNode with Atom
case class ValRef(astNode:Node, idx:Node, condSymPath:Option[List[Node]]) extends ASTNode with Ref{
  def prettyPrint(): String = "ValRef(" + "idx=" + idx.sourceText+ ", " + "condSymPath=" + (condSymPath match { case Some(v) =>
  "[" + v.map(e => e.sourceText).mkString(",") + "]"
  case None => "null"
}) +  ")"
}
case class RawRef(astNode:Node, idx:Node, condSymPath:Option[List[Node]]) extends ASTNode with Ref{
  def prettyPrint(): String = "RawRef(" + "idx=" + idx.sourceText+ ", " + "condSymPath=" + (condSymPath match { case Some(v) =>
  "[" + v.map(e => e.sourceText).mkString(",") + "]"
  case None => "null"
}) +  ")"
}
sealed trait PExpr extends ASTNode with BinderExpr with Processor
sealed trait TerExpr extends ASTNode with PExpr
sealed trait BoolOrExpr extends ASTNode with TerExpr
case class BinOp(astNode:Node, op:Node, lhs:BoolAndExpr, rhs:BoolOrExpr) extends ASTNode with AdditiveExpr{
  def prettyPrint(): String = "BinOp(" + "op=" + op.sourceText+ ", " + "lhs=" + lhs.prettyPrint()+ ", " + "rhs=" + rhs.prettyPrint() +  ")"
}
sealed trait BoolAndExpr extends ASTNode with BoolOrExpr
sealed trait BoolEqExpr extends ASTNode with BoolAndExpr
sealed trait ElvisExpr extends ASTNode with BoolEqExpr
case class ElvisOp(astNode:Node, value:AdditiveExpr, ifNull:ElvisExpr) extends ASTNode with ElvisExpr{
  def prettyPrint(): String = "ElvisOp(" + "value=" + value.prettyPrint()+ ", " + "ifNull=" + ifNull.prettyPrint() +  ")"
}
sealed trait AdditiveExpr extends ASTNode with ElvisExpr
sealed trait PrefixNotExpr extends ASTNode with AdditiveExpr
case class PrefixOp(astNode:Node, expr:PrefixNotExpr, op:Node) extends ASTNode with PrefixNotExpr{
  def prettyPrint(): String = "PrefixOp(" + "expr=" + expr.prettyPrint()+ ", " + "op=" + op.sourceText +  ")"
}
sealed trait Atom extends ASTNode with PrefixNotExpr
case class ExprParen(astNode:Node, body:PExpr) extends ASTNode with Atom{
  def prettyPrint(): String = "ExprParen(" + "body=" + body.prettyPrint() +  ")"
}
case class BindExpr(astNode:Node, ctx:ValRef, binder:BinderExpr) extends ASTNode with Atom{
  def prettyPrint(): String = "BindExpr(" + "ctx=" + ctx.prettyPrint()+ ", " + "binder=" + binder.prettyPrint() +  ")"
}
sealed trait BinderExpr extends ASTNode
case class NamedConstructExpr(astNode:Node, typeName:TypeName, params:List[NamedParam]) extends ASTNode with Atom{
  def prettyPrint(): String = "NamedConstructExpr(" + "typeName=" + typeName.prettyPrint()+ ", " + "params=" + "[" + params.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
case class NamedParam(astNode:Node, name:ParamName, typeDesc:Option[Node], expr:PExpr) extends ASTNode{
  def prettyPrint(): String = "NamedParam(" + "name=" + name.prettyPrint()+ ", " + "typeDesc=" + (typeDesc match { case Some(v) =>
  v.sourceText
  case None => "null"
})+ ", " + "expr=" + expr.prettyPrint() +  ")"
}
case class FuncCallOrConstructExpr(astNode:Node, funcName:TypeOrFuncName, params:Option[List[PExpr]]) extends ASTNode with Atom{
  def prettyPrint(): String = "FuncCallOrConstructExpr(" + "funcName=" + funcName.prettyPrint()+ ", " + "params=" + (params match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
}) +  ")"
}
case class ArrayExpr(astNode:Node, elems:Option[List[PExpr]]) extends ASTNode with Atom{
  def prettyPrint(): String = "ArrayExpr(" + "elems=" + (elems match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
}) +  ")"
}
sealed trait Literal extends ASTNode with Atom
case class NullLiteral(astNode:Node) extends ASTNode with Literal{
  def prettyPrint(): String = "NullLiteral(" +  ")"
}
case class BoolLiteral(astNode:Node, value:Node) extends ASTNode with Literal{
  def prettyPrint(): String = "BoolLiteral(" + "value=" + value.sourceText +  ")"
}
case class CharLiteral(astNode:Node, value:TerminalChar) extends ASTNode with Literal{
  def prettyPrint(): String = "CharLiteral(" + "value=" + value.prettyPrint() +  ")"
}
case class StringLiteral(astNode:Node, value:List[StringChar]) extends ASTNode with Literal{
  def prettyPrint(): String = "StringLiteral(" + "value=" + "[" + value.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
sealed trait AbstractEnumValue extends ASTNode with Atom
case class CanonicalEnumValue(astNode:Node, enumName:EnumTypeName, valueName:Node) extends ASTNode with AbstractEnumValue{
  def prettyPrint(): String = "CanonicalEnumValue(" + "enumName=" + enumName.prettyPrint()+ ", " + "valueName=" + valueName.sourceText +  ")"
}
case class ShortenedEnumValue(astNode:Node, valueName:Node) extends ASTNode with AbstractEnumValue{
  def prettyPrint(): String = "ShortenedEnumValue(" + "valueName=" + valueName.sourceText +  ")"
}
sealed trait TypeDef extends ASTNode with Def with NonNullTypeDesc
sealed trait ClassDef extends ASTNode with SubType with TypeDef
case class AbstractClassDef(astNode:Node, name:TypeName, supers:Option[List[TypeName]]) extends ASTNode with ClassDef{
  def prettyPrint(): String = "AbstractClassDef(" + "name=" + name.prettyPrint()+ ", " + "supers=" + (supers match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
}) +  ")"
}
case class ConcreteClassDef(astNode:Node, name:TypeName, supers:Option[List[TypeName]], params:Option[List[ClassParamDef]]) extends ASTNode with ClassDef{
  def prettyPrint(): String = "ConcreteClassDef(" + "name=" + name.prettyPrint()+ ", " + "supers=" + (supers match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
})+ ", " + "params=" + (params match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
}) +  ")"
}
case class ClassParamDef(astNode:Node, name:ParamName, typeDesc:Option[TypeDesc]) extends ASTNode{
  def prettyPrint(): String = "ClassParamDef(" + "name=" + name.prettyPrint()+ ", " + "typeDesc=" + (typeDesc match { case Some(v) =>
  v.prettyPrint()
  case None => "null"
}) +  ")"
}
case class SuperDef(astNode:Node, typeName:TypeName, subs:Option[List[SubType]]) extends ASTNode with SubType with TypeDef{
  def prettyPrint(): String = "SuperDef(" + "typeName=" + typeName.prettyPrint()+ ", " + "subs=" + (subs match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
}) +  ")"
}
sealed trait SubType extends ASTNode
case class EnumTypeDef(astNode:Node, name:EnumTypeName, values:List[Node]) extends ASTNode with TypeDef{
  def prettyPrint(): String = "EnumTypeDef(" + "name=" + name.prettyPrint()+ ", " + "values=" + "[" + values.map(e => e.sourceText).mkString(",") + "]" +  ")"
}
case class TypeDesc(astNode:Node, typ:NonNullTypeDesc, optional:Option[Node]) extends ASTNode{
  def prettyPrint(): String = "TypeDesc(" + "typ=" + typ.prettyPrint()+ ", " + "optional=" + (optional match { case Some(v) =>
  v.sourceText
  case None => "null"
}) +  ")"
}
sealed trait NonNullTypeDesc extends ASTNode
case class ArrayTypeDesc(astNode:Node, elemType:TypeDesc) extends ASTNode with NonNullTypeDesc{
  def prettyPrint(): String = "ArrayTypeDesc(" + "elemType=" + elemType.prettyPrint() +  ")"
}
sealed trait ValueType extends ASTNode with NonNullTypeDesc
case class BooleanType(astNode:Node) extends ASTNode with ValueType{
  def prettyPrint(): String = "BooleanType(" +  ")"
}
case class CharType(astNode:Node) extends ASTNode with ValueType{
  def prettyPrint(): String = "CharType(" +  ")"
}
case class StringType(astNode:Node) extends ASTNode with ValueType{
  def prettyPrint(): String = "StringType(" +  ")"
}
case class AnyType(astNode:Node) extends ASTNode with NonNullTypeDesc{
  def prettyPrint(): String = "AnyType(" +  ")"
}
case class EnumTypeName(astNode:Node, name:Node) extends ASTNode with NonNullTypeDesc{
  def prettyPrint(): String = "EnumTypeName(" + "name=" + name.sourceText +  ")"
}
case class TypeName(astNode:Node, name:Node) extends ASTNode with NonNullTypeDesc with SubType{
  def prettyPrint(): String = "TypeName(" + "name=" + name.sourceText +  ")"
}
case class NonterminalName(astNode:Node, name:Node) extends ASTNode{
  def prettyPrint(): String = "NonterminalName(" + "name=" + name.sourceText +  ")"
}
case class TypeOrFuncName(astNode:Node, name:Node) extends ASTNode{
  def prettyPrint(): String = "TypeOrFuncName(" + "name=" + name.sourceText +  ")"
}
case class ParamName(astNode:Node, name:Node) extends ASTNode{
  def prettyPrint(): String = "ParamName(" + "name=" + name.sourceText +  ")"
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

def matchGrammar(node: Node): Grammar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 3 =>
val v1 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v2, v3) = v1
assert(v2.id == 23)
val v4 = matchDef(v3)
val v5 = List(v4)
val v6 = body.asInstanceOf[SequenceNode].children(2)
val v11 = unrollRepeat0(v6) map { n =>
val BindNode(v7, v8) = n
assert(v7.id == 380)
val BindNode(v9, v10) = v8
assert(v9.id == 381)
v10
}
val v16 = v11 map { n =>
val v12 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v13, v14) = v12
assert(v13.id == 23)
val v15 = matchDef(v14)
v15
}
val v17 = v5 ++ v16
val v18 = Grammar(node,v17)
v18
  }
}
def matchDef(node: Node): Def = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 24 =>
val v19 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v20, v21) = v19
assert(v20.id == 25)
val v22 = matchRule(v21)
v22
case 104 =>
val v23 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v24, v25) = v23
assert(v24.id == 105)
val v26 = matchTypeDef(v25)
v26
  }
}
def matchRule(node: Node): Rule = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 26 =>
val v27 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v28, v29) = v27
assert(v28.id == 27)
val v30 = matchLHS(v29)
val v31 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v32, v33) = v31
assert(v32.id == 163)
val v34 = matchRHS(v33)
val v35 = List(v34)
val v36 = body.asInstanceOf[SequenceNode].children(5)
val v41 = unrollRepeat0(v36) map { n =>
val BindNode(v37, v38) = n
assert(v37.id == 376)
val BindNode(v39, v40) = v38
assert(v39.id == 377)
v40
}
val v46 = v41 map { n =>
val v42 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v43, v44) = v42
assert(v43.id == 163)
val v45 = matchRHS(v44)
v45
}
val v47 = v35 ++ v46
val v48 = Rule(node,v30,v47)
v48
  }
}
def matchLHS(node: Node): LHS = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 28 =>
val v49 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v50, v51) = v49
assert(v50.id == 29)
val v52 = matchNonterminal(v51)
val v53 = body.asInstanceOf[SequenceNode].children(1)
val v58 = unrollOptional(v53, 80, 81) map { n =>
val BindNode(v54, v55) = n
assert(v54.id == 81)
val BindNode(v56, v57) = v55
assert(v56.id == 82)
v57
}
val v63 = v58 map { n =>
val v59 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v60, v61) = v59
assert(v60.id == 84)
val v62 = matchTypeDesc(v61)
v62
}
val v64 = LHS(node,v52,v63)
v64
  }
}
def matchRHS(node: Node): RHS = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 164 =>
val v65 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v66, v67) = v65
assert(v66.id == 165)
val v68 = matchElem(v67)
val v69 = List(v68)
val v70 = body.asInstanceOf[SequenceNode].children(1)
val v75 = unrollRepeat0(v70) map { n =>
val BindNode(v71, v72) = n
assert(v71.id == 372)
val BindNode(v73, v74) = v72
assert(v73.id == 373)
v74
}
val v80 = v75 map { n =>
val v76 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v77, v78) = v76
assert(v77.id == 165)
val v79 = matchElem(v78)
v79
}
val v81 = v69 ++ v80
val v82 = RHS(node,v81)
v82
  }
}
def matchElem(node: Node): Elem = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 166 =>
val v83 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v84, v85) = v83
assert(v84.id == 167)
val v86 = matchSymbol(v85)
v86
case 251 =>
val v87 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v88, v89) = v87
assert(v88.id == 252)
val v90 = matchProcessor(v89)
v90
  }
}
def matchSymbol(node: Node): Symbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 168 =>
val v91 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v92, v93) = v91
assert(v92.id == 169)
val v94 = matchBinSymbol(v93)
v94
  }
}
def matchBinSymbol(node: Node): BinSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 170 =>
val v95 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v96, v97) = v95
assert(v96.id == 169)
val v98 = matchBinSymbol(v97)
val v99 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v100, v101) = v99
assert(v100.id == 172)
val v102 = matchPreUnSymbol(v101)
val v103 = JoinSymbol(node,v98,v102)
v103
case 249 =>
val v104 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v105, v106) = v104
assert(v105.id == 169)
val v107 = matchBinSymbol(v106)
val v108 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v109, v110) = v108
assert(v109.id == 172)
val v111 = matchPreUnSymbol(v110)
val v112 = ExceptSymbol(node,v107,v111)
v112
case 250 =>
val v113 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v114, v115) = v113
assert(v114.id == 172)
val v116 = matchPreUnSymbol(v115)
v116
  }
}
def matchPreUnSymbol(node: Node): PreUnSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 173 =>
val v117 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v118, v119) = v117
assert(v118.id == 172)
val v120 = matchPreUnSymbol(v119)
val v121 = FollowedBy(node,v120)
v121
case 175 =>
val v122 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v123, v124) = v122
assert(v123.id == 172)
val v125 = matchPreUnSymbol(v124)
val v126 = NotFollowedBy(node,v125)
v126
case 177 =>
val v127 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v128, v129) = v127
assert(v128.id == 178)
val v130 = matchPostUnSymbol(v129)
v130
  }
}
def matchPostUnSymbol(node: Node): PostUnSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 179 =>
val v131 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v132, v133) = v131
assert(v132.id == 178)
val v134 = matchPostUnSymbol(v133)
val v135 = Optional(node,v134)
v135
case 180 =>
val v136 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v137, v138) = v136
assert(v137.id == 178)
val v139 = matchPostUnSymbol(v138)
val v140 = RepeatFromZero(node,v139)
v140
case 182 =>
val v141 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v142, v143) = v141
assert(v142.id == 178)
val v144 = matchPostUnSymbol(v143)
val v145 = RepeatFromOne(node,v144)
v145
case 184 =>
val v146 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v147, v148) = v146
assert(v147.id == 185)
val v149 = matchAtomSymbol(v148)
v149
  }
}
def matchAtomSymbol(node: Node): AtomSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 186 =>
val v150 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v151, v152) = v150
assert(v151.id == 187)
val v153 = matchTerminal(v152)
v153
case 202 =>
val v154 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v155, v156) = v154
assert(v155.id == 203)
val v157 = matchTerminalChoice(v156)
v157
case 220 =>
val v158 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v159, v160) = v158
assert(v159.id == 221)
val v161 = matchStringSymbol(v160)
v161
case 232 =>
val v162 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v163, v164) = v162
assert(v163.id == 29)
val v165 = matchNonterminal(v164)
v165
case 233 =>
val v166 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v167, v168) = v166
assert(v167.id == 234)
val v169 = matchInPlaceChoices(v168)
v169
case 242 =>
val v170 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v171, v172) = v170
assert(v171.id == 243)
val v173 = matchLongest(v172)
v173
case 245 =>
val v174 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v175, v176) = v174
assert(v175.id == 246)
val v177 = matchEmptySequence(v176)
v177
  }
}
def matchTerminal(node: Node): Terminal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 188 =>
val v178 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v179, v180) = v178
assert(v179.id == 190)
val v181 = matchTerminalChar(v180)
v181
case 200 =>
val v182 = AnyTerminal(node)
v182
  }
}
def matchTerminalChoice(node: Node): TerminalChoice = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 204 =>
val v183 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v184, v185) = v183
assert(v184.id == 205)
val v186 = matchTerminalChoiceElem(v185)
val v187 = List(v186)
val v188 = body.asInstanceOf[SequenceNode].children(2)
val v192 = unrollRepeat1(v188) map { n =>
val BindNode(v189, v190) = n
assert(v189.id == 205)
val v191 = matchTerminalChoiceElem(v190)
v191
}
val v193 = v192 map { n =>
n
}
val v194 = v187 ++ v193
val v195 = TerminalChoice(node,v194)
v195
case 219 =>
val v196 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v197, v198) = v196
assert(v197.id == 214)
val v199 = matchTerminalChoiceRange(v198)
val v200 = List(v199)
val v201 = TerminalChoice(node,v200)
v201
  }
}
def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 206 =>
val v202 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v203, v204) = v202
assert(v203.id == 207)
val v205 = matchTerminalChoiceChar(v204)
v205
case 213 =>
val v206 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v207, v208) = v206
assert(v207.id == 214)
val v209 = matchTerminalChoiceRange(v208)
v209
  }
}
def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 215 =>
val v210 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v211, v212) = v210
assert(v211.id == 207)
val v213 = matchTerminalChoiceChar(v212)
val v214 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v215, v216) = v214
assert(v215.id == 207)
val v217 = matchTerminalChoiceChar(v216)
val v218 = TerminalChoiceRange(node,v213,v217)
v218
  }
}
def matchStringSymbol(node: Node): StringSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 222 =>
val v219 = body.asInstanceOf[SequenceNode].children(1)
val v223 = unrollRepeat0(v219) map { n =>
val BindNode(v220, v221) = n
assert(v220.id == 226)
val v222 = matchStringChar(v221)
v222
}
val v224 = v223 map { n =>
n
}
val v225 = StringSymbol(node,v224)
v225
  }
}
def matchNonterminal(node: Node): Nonterminal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 30 =>
val v226 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v227, v228) = v226
assert(v227.id == 31)
val v229 = matchNonterminalName(v228)
val v230 = Nonterminal(node,v229)
v230
  }
}
def matchInPlaceChoices(node: Node): InPlaceChoices = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 235 =>
val v231 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v232, v233) = v231
assert(v232.id == 236)
val v234 = matchInPlaceSequence(v233)
val v235 = List(v234)
val v236 = body.asInstanceOf[SequenceNode].children(1)
val v241 = unrollRepeat0(v236) map { n =>
val BindNode(v237, v238) = n
assert(v237.id == 239)
val BindNode(v239, v240) = v238
assert(v239.id == 240)
v240
}
val v246 = v241 map { n =>
val v242 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v243, v244) = v242
assert(v243.id == 236)
val v245 = matchInPlaceSequence(v244)
v245
}
val v247 = v235 ++ v246
val v248 = InPlaceChoices(node,v247)
v248
  }
}
def matchInPlaceSequence(node: Node): InPlaceSequence = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 164 =>
val v249 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v250, v251) = v249
assert(v250.id == 165)
val v252 = matchElem(v251)
val v253 = List(v252)
val v254 = body.asInstanceOf[SequenceNode].children(1)
val v259 = unrollRepeat0(v254) map { n =>
val BindNode(v255, v256) = n
assert(v255.id == 372)
val BindNode(v257, v258) = v256
assert(v257.id == 373)
v258
}
val v264 = v259 map { n =>
val v260 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v261, v262) = v260
assert(v261.id == 165)
val v263 = matchElem(v262)
v263
}
val v265 = v253 ++ v264
val v266 = InPlaceSequence(node,v265)
v266
  }
}
def matchLongest(node: Node): Longest = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 244 =>
val v267 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v268, v269) = v267
assert(v268.id == 234)
val v270 = matchInPlaceChoices(v269)
val v271 = Longest(node,v270)
v271
  }
}
def matchEmptySequence(node: Node): EmptySeq = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 247 =>
val v272 = EmptySeq(node)
v272
  }
}
def matchTerminalChar(node: Node): TerminalChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 191 =>
val v273 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v274, v275) = v273
assert(v274.id == 192)
val v276 = CharAsIs(node,v275)
v276
case 194 =>
val v277 = body.asInstanceOf[SequenceNode].children(1)
val v278 = CharEscaped(node,v277)
v278
case 196 =>
val v279 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v280, v281) = v279
assert(v280.id == 197)
val v282 = matchUnicodeChar(v281)
v282
  }
}
def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 208 =>
val v283 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v284, v285) = v283
assert(v284.id == 209)
val v286 = CharAsIs(node,v285)
v286
case 211 =>
val v287 = body.asInstanceOf[SequenceNode].children(1)
val v288 = CharEscaped(node,v287)
v288
case 196 =>
val v289 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v290, v291) = v289
assert(v290.id == 197)
val v292 = matchUnicodeChar(v291)
v292
  }
}
def matchStringChar(node: Node): StringChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 227 =>
val v293 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v294, v295) = v293
assert(v294.id == 228)
val v296 = CharAsIs(node,v295)
v296
case 230 =>
val v297 = body.asInstanceOf[SequenceNode].children(1)
val v298 = CharEscaped(node,v297)
v298
case 196 =>
val v299 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v300, v301) = v299
assert(v300.id == 197)
val v302 = matchUnicodeChar(v301)
v302
  }
}
def matchUnicodeChar(node: Node): CharUnicode = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 198 =>
val v303 = body.asInstanceOf[SequenceNode].children(2)
val v304 = body.asInstanceOf[SequenceNode].children(3)
val v305 = body.asInstanceOf[SequenceNode].children(4)
val v306 = body.asInstanceOf[SequenceNode].children(5)
val v307 = List(v303,v304,v305,v306)
val v308 = CharUnicode(node,v307)
v308
  }
}
def matchProcessor(node: Node): Processor = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 253 =>
val v309 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v310, v311) = v309
assert(v310.id == 254)
val v312 = matchRef(v311)
v312
case 281 =>
val v313 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v314, v315) = v313
assert(v314.id == 282)
val v316 = matchPExpr(v315)
v316
  }
}
def matchRef(node: Node): Ref = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 255 =>
val v317 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v318, v319) = v317
assert(v318.id == 256)
val v320 = matchValRef(v319)
v320
case 276 =>
val v321 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v322, v323) = v321
assert(v322.id == 277)
val v324 = matchRawRef(v323)
v324
  }
}
def matchValRef(node: Node): ValRef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 257 =>
val v325 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v326, v327) = v325
assert(v326.id == 265)
val v328 = matchRefIdx(v327)
val v329 = body.asInstanceOf[SequenceNode].children(1)
val v333 = unrollOptional(v329, 80, 260) map { n =>
val BindNode(v330, v331) = n
assert(v330.id == 260)
val v332 = matchCondSymPath(v331)
v332
}
val v334 = ValRef(node,v328,v333)
v334
  }
}
def matchCondSymPath(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 261 =>
val v335 = body.asInstanceOf[SequenceNode].children(0)
val v336 = unrollRepeat1(v335) map { n =>
// UnrollChoices
n
}
v336
  }
}
def matchRawRef(node: Node): RawRef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 278 =>
val v337 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v338, v339) = v337
assert(v338.id == 265)
val v340 = matchRefIdx(v339)
val v341 = body.asInstanceOf[SequenceNode].children(1)
val v345 = unrollOptional(v341, 80, 260) map { n =>
val BindNode(v342, v343) = n
assert(v342.id == 260)
val v344 = matchCondSymPath(v343)
v344
}
val v346 = RawRef(node,v340,v345)
v346
  }
}
def matchPExpr(node: Node): PExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 283 =>
val v347 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v348, v349) = v347
assert(v348.id == 284)
val v350 = matchTernaryExpr(v349)
v350
  }
}
def matchTernaryExpr(node: Node): TerExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 285 =>
val v351 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v352, v353) = v351
assert(v352.id == 286)
val v354 = matchBoolOrExpr(v353)
v354
  }
}
def matchBoolOrExpr(node: Node): BoolOrExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 287 =>
val v355 = body.asInstanceOf[SequenceNode].children(2)
val v356 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v357, v358) = v356
assert(v357.id == 288)
val v359 = matchBoolAndExpr(v358)
val v360 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v361, v362) = v360
assert(v361.id == 286)
val v363 = matchBoolOrExpr(v362)
val v364 = BinOp(node,v355,v359,v363)
v364
case 369 =>
val v365 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v366, v367) = v365
assert(v366.id == 288)
val v368 = matchBoolAndExpr(v367)
v368
  }
}
def matchBoolAndExpr(node: Node): BoolAndExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 289 =>
val v369 = body.asInstanceOf[SequenceNode].children(2)
val v370 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v371, v372) = v370
assert(v371.id == 290)
val v373 = matchBoolEqExpr(v372)
val v374 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v375, v376) = v374
assert(v375.id == 288)
val v377 = matchBoolAndExpr(v376)
val v378 = BinOp(node,v369,v373,v377)
v378
case 366 =>
val v379 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v380, v381) = v379
assert(v380.id == 290)
val v382 = matchBoolEqExpr(v381)
v382
  }
}
def matchBoolEqExpr(node: Node): BoolEqExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 291 =>
// UnrollChoices
val v383 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v384, v385) = v383
assert(v384.id == 292)
val v386 = matchElvisExpr(v385)
val v387 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v388, v389) = v387
assert(v388.id == 290)
val v390 = matchBoolEqExpr(v389)
val v391 = BinOp(node,body,v386,v390)
v391
case 363 =>
val v392 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v393, v394) = v392
assert(v393.id == 292)
val v395 = matchElvisExpr(v394)
v395
  }
}
def matchElvisExpr(node: Node): ElvisExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 293 =>
val v396 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v397, v398) = v396
assert(v397.id == 294)
val v399 = matchAdditiveExpr(v398)
val v400 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v401, v402) = v400
assert(v401.id == 292)
val v403 = matchElvisExpr(v402)
val v404 = ElvisOp(node,v399,v403)
v404
case 357 =>
val v405 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v406, v407) = v405
assert(v406.id == 294)
val v408 = matchAdditiveExpr(v407)
v408
  }
}
def matchAdditiveExpr(node: Node): AdditiveExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 295 =>
val v409 = body.asInstanceOf[SequenceNode].children(2)
val v410 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v411, v412) = v410
assert(v411.id == 296)
val v413 = matchPrefixNotExpr(v412)
val v414 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v415, v416) = v414
assert(v415.id == 294)
val v417 = matchAdditiveExpr(v416)
val v418 = BinOp(node,v409,v413,v417)
v418
case 354 =>
val v419 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v420, v421) = v419
assert(v420.id == 296)
val v422 = matchPrefixNotExpr(v421)
v422
  }
}
def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 297 =>
val v423 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v424, v425) = v423
assert(v424.id == 296)
val v426 = matchPrefixNotExpr(v425)
val v427 = body.asInstanceOf[SequenceNode].children(0)
val v428 = PrefixOp(node,v426,v427)
v428
case 298 =>
val v429 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v430, v431) = v429
assert(v430.id == 299)
val v432 = matchAtom(v431)
v432
  }
}
def matchAtom(node: Node): Atom = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 253 =>
val v433 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v434, v435) = v433
assert(v434.id == 254)
val v436 = matchRef(v435)
v436
case 300 =>
val v437 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v438, v439) = v437
assert(v438.id == 301)
val v440 = matchBindExpr(v439)
v440
case 304 =>
val v441 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v442, v443) = v441
assert(v442.id == 305)
val v444 = matchNamedConstructExpr(v443)
v444
case 315 =>
val v445 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v446, v447) = v445
assert(v446.id == 316)
val v448 = matchFuncCallOrConstructExpr(v447)
v448
case 328 =>
val v449 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v450, v451) = v449
assert(v450.id == 329)
val v452 = matchArrayExpr(v451)
v452
case 331 =>
val v453 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v454, v455) = v453
assert(v454.id == 332)
val v456 = matchLiteral(v455)
v456
case 343 =>
val v457 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v458, v459) = v457
assert(v458.id == 344)
val v460 = matchEnumValue(v459)
v460
case 353 =>
val v461 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v462, v463) = v461
assert(v462.id == 282)
val v464 = matchPExpr(v463)
val v465 = ExprParen(node,v464)
v465
  }
}
def matchBindExpr(node: Node): BindExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 302 =>
val v466 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v467, v468) = v466
assert(v467.id == 256)
val v469 = matchValRef(v468)
val v470 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v471, v472) = v470
assert(v471.id == 303)
val v473 = matchBinderExpr(v472)
val v474 = BindExpr(node,v469,v473)
v474
  }
}
def matchBinderExpr(node: Node): BinderExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 253 =>
val v475 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v476, v477) = v475
assert(v476.id == 254)
val v478 = matchRef(v477)
v478
case 300 =>
val v479 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v480, v481) = v479
assert(v480.id == 301)
val v482 = matchBindExpr(v481)
v482
case 281 =>
val v483 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v484, v485) = v483
assert(v484.id == 282)
val v486 = matchPExpr(v485)
v486
  }
}
def matchNamedConstructExpr(node: Node): NamedConstructExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 306 =>
val v487 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v488, v489) = v487
assert(v488.id == 88)
val v490 = matchTypeName(v489)
val v491 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v492, v493) = v491
assert(v492.id == 307)
val v494 = matchNamedConstructParams(v493)
val v495 = NamedConstructExpr(node,v490,v494)
v495
  }
}
def matchNamedConstructParams(node: Node): List[NamedParam] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 308 =>
val v496 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v497, v498) = v496
assert(v497.id == 309)
val v499 = matchNamedParam(v498)
val v500 = List(v499)
val v501 = body.asInstanceOf[SequenceNode].children(3)
val v506 = unrollRepeat0(v501) map { n =>
val BindNode(v502, v503) = n
assert(v502.id == 313)
val BindNode(v504, v505) = v503
assert(v504.id == 314)
v505
}
val v511 = v506 map { n =>
val v507 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v508, v509) = v507
assert(v508.id == 309)
val v510 = matchNamedParam(v509)
v510
}
val v512 = v500 ++ v511
v512
  }
}
def matchNamedParam(node: Node): NamedParam = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 310 =>
val v513 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v514, v515) = v513
assert(v514.id == 130)
val v516 = matchParamName(v515)
val v517 = body.asInstanceOf[SequenceNode].children(1)
val v522 = unrollOptional(v517, 80, 81) map { n =>
val BindNode(v518, v519) = n
assert(v518.id == 81)
val BindNode(v520, v521) = v519
assert(v520.id == 82)
v521
}
val v523 = body.asInstanceOf[SequenceNode].children(5)
val BindNode(v524, v525) = v523
assert(v524.id == 282)
val v526 = matchPExpr(v525)
val v527 = NamedParam(node,v516,v522,v526)
v527
  }
}
def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 317 =>
val v528 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v529, v530) = v528
assert(v529.id == 318)
val v531 = matchTypeOrFuncName(v530)
val v532 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v533, v534) = v532
assert(v533.id == 319)
val v535 = matchCallParams(v534)
val v536 = FuncCallOrConstructExpr(node,v531,v535)
v536
  }
}
def matchCallParams(node: Node): Option[List[PExpr]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 320 =>
val v537 = body.asInstanceOf[SequenceNode].children(2)
val v542 = unrollOptional(v537, 80, 322) map { n =>
val BindNode(v538, v539) = n
assert(v538.id == 322)
val BindNode(v540, v541) = v539
assert(v540.id == 323)
v541
}
val v560 = v542 map { n =>
val v543 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v544, v545) = v543
assert(v544.id == 282)
val v546 = matchPExpr(v545)
val v547 = List(v546)
val v548 = n.asInstanceOf[SequenceNode].children(1)
val v553 = unrollRepeat0(v548) map { n =>
val BindNode(v549, v550) = n
assert(v549.id == 326)
val BindNode(v551, v552) = v550
assert(v551.id == 327)
v552
}
val v558 = v553 map { n =>
val v554 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v555, v556) = v554
assert(v555.id == 282)
val v557 = matchPExpr(v556)
v557
}
val v559 = v547 ++ v558
v559
}
v560
  }
}
def matchArrayExpr(node: Node): ArrayExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 330 =>
val v561 = body.asInstanceOf[SequenceNode].children(2)
val v566 = unrollOptional(v561, 80, 322) map { n =>
val BindNode(v562, v563) = n
assert(v562.id == 322)
val BindNode(v564, v565) = v563
assert(v564.id == 323)
v565
}
val v584 = v566 map { n =>
val v567 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v568, v569) = v567
assert(v568.id == 282)
val v570 = matchPExpr(v569)
val v571 = List(v570)
val v572 = n.asInstanceOf[SequenceNode].children(1)
val v577 = unrollRepeat0(v572) map { n =>
val BindNode(v573, v574) = n
assert(v573.id == 326)
val BindNode(v575, v576) = v574
assert(v575.id == 327)
v576
}
val v582 = v577 map { n =>
val v578 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v579, v580) = v578
assert(v579.id == 282)
val v581 = matchPExpr(v580)
v581
}
val v583 = v571 ++ v582
v583
}
val v585 = ArrayExpr(node,v584)
v585
  }
}
def matchLiteral(node: Node): Literal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 74 =>
val v586 = NullLiteral(node)
v586
case 333 =>
// UnrollChoices
val v587 = BoolLiteral(node,body)
v587
case 335 =>
val v588 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v589, v590) = v588
assert(v589.id == 336)
val v591 = matchCharChar(v590)
val v592 = CharLiteral(node,v591)
v592
case 338 =>
val v593 = body.asInstanceOf[SequenceNode].children(1)
val v597 = unrollRepeat0(v593) map { n =>
val BindNode(v594, v595) = n
assert(v594.id == 341)
val v596 = matchStrChar(v595)
v596
}
val v598 = StringLiteral(node,v597)
v598
  }
}
def matchEnumValue(node: Node): AbstractEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 345 =>
val v599 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v600, v601) = v599
assert(v600.id == 346)
val v602 = matchCanonicalEnumValue(v601)
v602
case 350 =>
val v603 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v604, v605) = v603
assert(v604.id == 351)
val v606 = matchShortenedEnumValue(v605)
v606
  }
}
def matchCanonicalEnumValue(node: Node): CanonicalEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 347 =>
val v607 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v608, v609) = v607
assert(v608.id == 101)
val v610 = matchEnumTypeName(v609)
val v611 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v612, v613) = v611
assert(v612.id == 348)
val v614 = matchEnumValueName(v613)
val v615 = CanonicalEnumValue(node,v610,v614)
v615
  }
}
def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 352 =>
val v616 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v617, v618) = v616
assert(v617.id == 348)
val v619 = matchEnumValueName(v618)
val v620 = ShortenedEnumValue(node,v619)
v620
  }
}
def matchTypeDef(node: Node): TypeDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 106 =>
val v621 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v622, v623) = v621
assert(v622.id == 107)
val v624 = matchClassDef(v623)
v624
case 136 =>
val v625 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v626, v627) = v625
assert(v626.id == 137)
val v628 = matchSuperDef(v627)
v628
case 151 =>
val v629 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v630, v631) = v629
assert(v630.id == 152)
val v632 = matchEnumTypeDef(v631)
v632
  }
}
def matchClassDef(node: Node): ClassDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 108 =>
val v633 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v634, v635) = v633
assert(v634.id == 88)
val v636 = matchTypeName(v635)
val v637 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v638, v639) = v637
assert(v638.id == 109)
val v640 = matchSuperTypes(v639)
val v641 = AbstractClassDef(node,v636,v640)
v641
case 121 =>
val v642 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v643, v644) = v642
assert(v643.id == 88)
val v645 = matchTypeName(v644)
val v646 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v647, v648) = v646
assert(v647.id == 109)
val v649 = matchSuperTypes(v648)
val v650 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v651, v652) = v650
assert(v651.id == 122)
val v653 = matchClassParamsDef(v652)
val v654 = ConcreteClassDef(node,v645,v649,v653)
v654
  }
}
def matchSuperTypes(node: Node): Option[List[TypeName]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 110 =>
val v655 = body.asInstanceOf[SequenceNode].children(2)
val v660 = unrollOptional(v655, 80, 113) map { n =>
val BindNode(v656, v657) = n
assert(v656.id == 113)
val BindNode(v658, v659) = v657
assert(v658.id == 114)
v659
}
val v678 = v660 map { n =>
val v661 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v662, v663) = v661
assert(v662.id == 88)
val v664 = matchTypeName(v663)
val v665 = List(v664)
val v666 = n.asInstanceOf[SequenceNode].children(1)
val v671 = unrollRepeat0(v666) map { n =>
val BindNode(v667, v668) = n
assert(v667.id == 117)
val BindNode(v669, v670) = v668
assert(v669.id == 118)
v670
}
val v676 = v671 map { n =>
val v672 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v673, v674) = v672
assert(v673.id == 88)
val v675 = matchTypeName(v674)
v675
}
val v677 = v665 ++ v676
v677
}
v678
  }
}
def matchClassParamsDef(node: Node): Option[List[ClassParamDef]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 123 =>
val v679 = body.asInstanceOf[SequenceNode].children(2)
val v684 = unrollOptional(v679, 80, 126) map { n =>
val BindNode(v680, v681) = n
assert(v680.id == 126)
val BindNode(v682, v683) = v681
assert(v682.id == 127)
v683
}
val v702 = v684 map { n =>
val v685 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v686, v687) = v685
assert(v686.id == 128)
val v688 = matchClassParamDef(v687)
val v689 = List(v688)
val v690 = n.asInstanceOf[SequenceNode].children(1)
val v695 = unrollRepeat0(v690) map { n =>
val BindNode(v691, v692) = n
assert(v691.id == 133)
val BindNode(v693, v694) = v692
assert(v693.id == 134)
v694
}
val v700 = v695 map { n =>
val v696 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v697, v698) = v696
assert(v697.id == 128)
val v699 = matchClassParamDef(v698)
v699
}
val v701 = v689 ++ v700
v701
}
v702
  }
}
def matchClassParamDef(node: Node): ClassParamDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 129 =>
val v703 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v704, v705) = v703
assert(v704.id == 130)
val v706 = matchParamName(v705)
val v707 = body.asInstanceOf[SequenceNode].children(1)
val v712 = unrollOptional(v707, 80, 81) map { n =>
val BindNode(v708, v709) = n
assert(v708.id == 81)
val BindNode(v710, v711) = v709
assert(v710.id == 82)
v711
}
val v717 = v712 map { n =>
val v713 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v714, v715) = v713
assert(v714.id == 84)
val v716 = matchTypeDesc(v715)
v716
}
val v718 = ClassParamDef(node,v706,v717)
v718
  }
}
def matchSuperDef(node: Node): SuperDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 138 =>
val v719 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v720, v721) = v719
assert(v720.id == 88)
val v722 = matchTypeName(v721)
val v723 = body.asInstanceOf[SequenceNode].children(3)
val v728 = unrollOptional(v723, 80, 141) map { n =>
val BindNode(v724, v725) = n
assert(v724.id == 141)
val BindNode(v726, v727) = v725
assert(v726.id == 142)
v727
}
val v733 = v728 map { n =>
val v729 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v730, v731) = v729
assert(v730.id == 143)
val v732 = matchSubTypes(v731)
v732
}
val v734 = SuperDef(node,v722,v733)
v734
  }
}
def matchSubTypes(node: Node): List[SubType] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 144 =>
val v735 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v736, v737) = v735
assert(v736.id == 145)
val v738 = matchSubType(v737)
val v739 = List(v738)
val v740 = body.asInstanceOf[SequenceNode].children(1)
val v745 = unrollRepeat0(v740) map { n =>
val BindNode(v741, v742) = n
assert(v741.id == 148)
val BindNode(v743, v744) = v742
assert(v743.id == 149)
v744
}
val v750 = v745 map { n =>
val v746 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v747, v748) = v746
assert(v747.id == 145)
val v749 = matchSubType(v748)
v749
}
val v751 = v739 ++ v750
v751
  }
}
def matchSubType(node: Node): SubType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 87 =>
val v752 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v753, v754) = v752
assert(v753.id == 88)
val v755 = matchTypeName(v754)
v755
case 106 =>
val v756 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v757, v758) = v756
assert(v757.id == 107)
val v759 = matchClassDef(v758)
v759
case 136 =>
val v760 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v761, v762) = v760
assert(v761.id == 137)
val v763 = matchSuperDef(v762)
v763
  }
}
def matchEnumTypeDef(node: Node): EnumTypeDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 153 =>
val v764 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v765, v766) = v764
assert(v765.id == 101)
val v767 = matchEnumTypeName(v766)
val v768 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v769, v770) = v768
assert(v769.id == 34)
val v771 = matchId(v770)
val v772 = List(v771)
val v773 = body.asInstanceOf[SequenceNode].children(5)
val v778 = unrollRepeat0(v773) map { n =>
val BindNode(v774, v775) = n
assert(v774.id == 156)
val BindNode(v776, v777) = v775
assert(v776.id == 157)
v777
}
val v783 = v778 map { n =>
val v779 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v780, v781) = v779
assert(v780.id == 34)
val v782 = matchId(v781)
v782
}
val v784 = v772 ++ v783
val v785 = EnumTypeDef(node,v767,v784)
v785
  }
}
def matchTypeDesc(node: Node): TypeDesc = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 85 =>
val v786 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v787, v788) = v786
assert(v787.id == 86)
val v789 = matchNonNullTypeDesc(v788)
val v790 = body.asInstanceOf[SequenceNode].children(1)
val v795 = unrollOptional(v790, 80, 159) map { n =>
val BindNode(v791, v792) = n
assert(v791.id == 159)
val BindNode(v793, v794) = v792
assert(v793.id == 160)
v794
}
val v796 = TypeDesc(node,v789,v795)
v796
  }
}
def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 87 =>
val v797 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v798, v799) = v797
assert(v798.id == 88)
val v800 = matchTypeName(v799)
v800
case 89 =>
val v801 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v802, v803) = v801
assert(v802.id == 84)
val v804 = matchTypeDesc(v803)
val v805 = ArrayTypeDesc(node,v804)
v805
case 92 =>
val v806 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v807, v808) = v806
assert(v807.id == 93)
val v809 = matchValueType(v808)
v809
case 94 =>
val v810 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v811, v812) = v810
assert(v811.id == 95)
val v813 = matchAnyType(v812)
v813
case 100 =>
val v814 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v815, v816) = v814
assert(v815.id == 101)
val v817 = matchEnumTypeName(v816)
v817
case 104 =>
val v818 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v819, v820) = v818
assert(v819.id == 105)
val v821 = matchTypeDef(v820)
v821
  }
}
def matchValueType(node: Node): ValueType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 44 =>
val v822 = BooleanType(node)
v822
case 53 =>
val v823 = CharType(node)
v823
case 59 =>
val v824 = StringType(node)
v824
  }
}
def matchAnyType(node: Node): AnyType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 96 =>
val v825 = AnyType(node)
v825
  }
}
def matchEnumTypeName(node: Node): EnumTypeName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 102 =>
val v826 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v827, v828) = v826
assert(v827.id == 34)
val v829 = matchId(v828)
val v830 = EnumTypeName(node,v829)
v830
  }
}
def matchTypeName(node: Node): TypeName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v831 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v832, v833) = v831
assert(v832.id == 33)
val v834 = TypeName(node,v833)
v834
case 77 =>
val v835 = body.asInstanceOf[SequenceNode].children(0)
val v836 = TypeName(node,v835)
v836
  }
}
def matchNonterminalName(node: Node): NonterminalName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v837 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v838, v839) = v837
assert(v838.id == 33)
val v840 = NonterminalName(node,v839)
v840
case 77 =>
val v841 = body.asInstanceOf[SequenceNode].children(0)
val v842 = NonterminalName(node,v841)
v842
  }
}
def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v843 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v844, v845) = v843
assert(v844.id == 33)
val v846 = TypeOrFuncName(node,v845)
v846
case 77 =>
val v847 = body.asInstanceOf[SequenceNode].children(0)
val v848 = TypeOrFuncName(node,v847)
v848
  }
}
def matchParamName(node: Node): ParamName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v849 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v850, v851) = v849
assert(v850.id == 33)
val v852 = ParamName(node,v851)
v852
case 77 =>
val v853 = body.asInstanceOf[SequenceNode].children(0)
val v854 = ParamName(node,v853)
v854
  }
}
def matchEnumValueName(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 349 =>
val v855 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v856, v857) = v855
assert(v856.id == 34)
val v858 = matchId(v857)
v858
  }
}
def matchKeyword(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 44 =>
val v859 = body.asInstanceOf[SequenceNode].children(0)
v859
case 53 =>
val v860 = body.asInstanceOf[SequenceNode].children(0)
v860
case 59 =>
val v861 = body.asInstanceOf[SequenceNode].children(0)
v861
case 66 =>
val v862 = body.asInstanceOf[SequenceNode].children(0)
v862
case 70 =>
val v863 = body.asInstanceOf[SequenceNode].children(0)
v863
case 74 =>
val v864 = body.asInstanceOf[SequenceNode].children(0)
v864
  }
}
def matchStrChar(node: Node): StringChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 342 =>
val v865 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v866, v867) = v865
assert(v866.id == 226)
val v868 = matchStringChar(v867)
v868
  }
}
def matchCharChar(node: Node): TerminalChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 337 =>
val v869 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v870, v871) = v869
assert(v870.id == 190)
val v872 = matchTerminalChar(v871)
v872
  }
}
def matchRefIdx(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 266 =>
val v873 = body.asInstanceOf[SequenceNode].children(0)
v873
  }
}
def matchId(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 35 =>
val v874 = body.asInstanceOf[SequenceNode].children(0)
v874
  }
}
def matchWS(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 5 =>
val v875 = body.asInstanceOf[SequenceNode].children(0)
val v876 = unrollRepeat0(v875) map { n =>
// UnrollChoices
n
}
v876
  }
}
def matchWSNL(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 383 =>
val v877 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v878, v879) = v877
assert(v878.id == 4)
val v880 = matchWS(v879)
v880
  }
}
def matchLineComment(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 12 =>
// UnrollChoices
body
  }
}
def matchEOF(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 21 =>
val v881 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v882, v883) = v881
assert(v882.id == 22)
v883
  }
}
def matchStart(node: Node): Grammar = {
  val BindNode(start, BindNode(startNonterm, body)) = node
  assert(start.id == 1)
  assert(startNonterm.id == 2)
  matchGrammar(body)
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

def parseAst(text: String): Either[Grammar, ParsingErrors.ParsingError] =
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
}