package com.giyeok.jparser.metalang2.generated

import com.giyeok.jparser.Inputs.InputToShortString
import com.giyeok.jparser.ParseResultTree.{BindNode, JoinNode, Node, SequenceNode, TerminalNode}
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat0
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor, Parser, ParseTreeUtil}
import com.giyeok.jparser.{NGrammar, ParseForestFunc, ParsingErrors, Symbols}

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
105 -> NGrammar.NNonterminal(105, Symbols.Nonterminal("TypeDef"), Set(106,136,154)),
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
139 -> NGrammar.NOneOf(139, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SuperTypes")))))), Set(80,140)),
140 -> NGrammar.NProxy(140, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SuperTypes")))), 141),
142 -> NGrammar.NTerminal(142, Symbols.ExactChar('{')),
143 -> NGrammar.NOneOf(143, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubTypes")))))), Set(80,144)),
144 -> NGrammar.NProxy(144, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubTypes")))), 145),
146 -> NGrammar.NNonterminal(146, Symbols.Nonterminal("SubTypes"), Set(147)),
148 -> NGrammar.NNonterminal(148, Symbols.Nonterminal("SubType"), Set(87,106,136)),
149 -> NGrammar.NRepeat(149, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0), 7, 150),
151 -> NGrammar.NProxy(151, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 152),
153 -> NGrammar.NTerminal(153, Symbols.ExactChar('}')),
155 -> NGrammar.NNonterminal(155, Symbols.Nonterminal("EnumTypeDef"), Set(156)),
157 -> NGrammar.NRepeat(157, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 0), 7, 158),
159 -> NGrammar.NProxy(159, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 160),
161 -> NGrammar.NOneOf(161, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))))), Set(80,162)),
162 -> NGrammar.NProxy(162, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))), 163),
164 -> NGrammar.NTerminal(164, Symbols.ExactChar('?')),
165 -> NGrammar.NTerminal(165, Symbols.ExactChar('=')),
166 -> NGrammar.NNonterminal(166, Symbols.Nonterminal("RHS"), Set(167)),
168 -> NGrammar.NNonterminal(168, Symbols.Nonterminal("Sequence"), Set(169)),
170 -> NGrammar.NNonterminal(170, Symbols.Nonterminal("Elem"), Set(171,255)),
172 -> NGrammar.NNonterminal(172, Symbols.Nonterminal("Symbol"), Set(173)),
174 -> NGrammar.NNonterminal(174, Symbols.Nonterminal("BinSymbol"), Set(175,253,254)),
176 -> NGrammar.NTerminal(176, Symbols.ExactChar('&')),
177 -> NGrammar.NNonterminal(177, Symbols.Nonterminal("PreUnSymbol"), Set(178,180,182)),
179 -> NGrammar.NTerminal(179, Symbols.ExactChar('^')),
181 -> NGrammar.NTerminal(181, Symbols.ExactChar('!')),
183 -> NGrammar.NNonterminal(183, Symbols.Nonterminal("PostUnSymbol"), Set(184,185,187,189)),
186 -> NGrammar.NTerminal(186, Symbols.ExactChar('*')),
188 -> NGrammar.NTerminal(188, Symbols.ExactChar('+')),
190 -> NGrammar.NNonterminal(190, Symbols.Nonterminal("AtomSymbol"), Set(191,207,225,237,238,246,249)),
192 -> NGrammar.NNonterminal(192, Symbols.Nonterminal("Terminal"), Set(193,205)),
194 -> NGrammar.NTerminal(194, Symbols.ExactChar('\'')),
195 -> NGrammar.NNonterminal(195, Symbols.Nonterminal("TerminalChar"), Set(196,199,201)),
197 -> NGrammar.NExcept(197, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')), 17, 198),
198 -> NGrammar.NTerminal(198, Symbols.ExactChar('\\')),
200 -> NGrammar.NTerminal(200, Symbols.Chars(Set('\'','\\','b','n','r','t'))),
202 -> NGrammar.NNonterminal(202, Symbols.Nonterminal("UnicodeChar"), Set(203)),
204 -> NGrammar.NTerminal(204, Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet)),
206 -> NGrammar.NTerminal(206, Symbols.ExactChar('.')),
208 -> NGrammar.NNonterminal(208, Symbols.Nonterminal("TerminalChoice"), Set(209,224)),
210 -> NGrammar.NNonterminal(210, Symbols.Nonterminal("TerminalChoiceElem"), Set(211,218)),
212 -> NGrammar.NNonterminal(212, Symbols.Nonterminal("TerminalChoiceChar"), Set(213,216,201)),
214 -> NGrammar.NExcept(214, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))), 17, 215),
215 -> NGrammar.NTerminal(215, Symbols.Chars(Set('\'','-','\\'))),
217 -> NGrammar.NTerminal(217, Symbols.Chars(Set('\'','-','\\','b','n','r','t'))),
219 -> NGrammar.NNonterminal(219, Symbols.Nonterminal("TerminalChoiceRange"), Set(220)),
221 -> NGrammar.NTerminal(221, Symbols.ExactChar('-')),
222 -> NGrammar.NRepeat(222, Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), 210, 223),
226 -> NGrammar.NNonterminal(226, Symbols.Nonterminal("StringSymbol"), Set(227)),
228 -> NGrammar.NTerminal(228, Symbols.ExactChar('"')),
229 -> NGrammar.NRepeat(229, Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), 7, 230),
231 -> NGrammar.NNonterminal(231, Symbols.Nonterminal("StringChar"), Set(232,235,201)),
233 -> NGrammar.NExcept(233, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))), 17, 234),
234 -> NGrammar.NTerminal(234, Symbols.Chars(Set('"','\\'))),
236 -> NGrammar.NTerminal(236, Symbols.Chars(Set('"','\\','b','n','r','t'))),
239 -> NGrammar.NNonterminal(239, Symbols.Nonterminal("InPlaceChoices"), Set(240)),
241 -> NGrammar.NRepeat(241, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence")))), 0), 7, 242),
243 -> NGrammar.NProxy(243, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence")))), 244),
245 -> NGrammar.NTerminal(245, Symbols.ExactChar('|')),
247 -> NGrammar.NNonterminal(247, Symbols.Nonterminal("Longest"), Set(248)),
250 -> NGrammar.NNonterminal(250, Symbols.Nonterminal("EmptySequence"), Set(251)),
252 -> NGrammar.NTerminal(252, Symbols.ExactChar('#')),
256 -> NGrammar.NNonterminal(256, Symbols.Nonterminal("Processor"), Set(257,285)),
258 -> NGrammar.NNonterminal(258, Symbols.Nonterminal("Ref"), Set(259,280)),
260 -> NGrammar.NNonterminal(260, Symbols.Nonterminal("ValRef"), Set(261)),
262 -> NGrammar.NTerminal(262, Symbols.ExactChar('$')),
263 -> NGrammar.NOneOf(263, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))), Set(80,264)),
264 -> NGrammar.NNonterminal(264, Symbols.Nonterminal("CondSymPath"), Set(265)),
266 -> NGrammar.NRepeat(266, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1), 267, 268),
267 -> NGrammar.NOneOf(267, Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), Set(111,120)),
269 -> NGrammar.NNonterminal(269, Symbols.Nonterminal("RefIdx"), Set(270)),
271 -> NGrammar.NLongest(271, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))), 272),
272 -> NGrammar.NOneOf(272, Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))))), Set(273,274)),
273 -> NGrammar.NTerminal(273, Symbols.ExactChar('0')),
274 -> NGrammar.NProxy(274, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))), 275),
276 -> NGrammar.NTerminal(276, Symbols.Chars(('1' to '9').toSet)),
277 -> NGrammar.NRepeat(277, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 7, 278),
279 -> NGrammar.NTerminal(279, Symbols.Chars(('0' to '9').toSet)),
281 -> NGrammar.NNonterminal(281, Symbols.Nonterminal("RawRef"), Set(282)),
283 -> NGrammar.NProxy(283, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$')))), 284),
286 -> NGrammar.NNonterminal(286, Symbols.Nonterminal("PExpr"), Set(287)),
288 -> NGrammar.NNonterminal(288, Symbols.Nonterminal("TernaryExpr"), Set(289)),
290 -> NGrammar.NNonterminal(290, Symbols.Nonterminal("BoolOrExpr"), Set(291,373)),
292 -> NGrammar.NNonterminal(292, Symbols.Nonterminal("BoolAndExpr"), Set(293,370)),
294 -> NGrammar.NNonterminal(294, Symbols.Nonterminal("BoolEqExpr"), Set(295,367)),
296 -> NGrammar.NNonterminal(296, Symbols.Nonterminal("ElvisExpr"), Set(297,361)),
298 -> NGrammar.NNonterminal(298, Symbols.Nonterminal("AdditiveExpr"), Set(299,358)),
300 -> NGrammar.NNonterminal(300, Symbols.Nonterminal("PrefixNotExpr"), Set(301,302)),
303 -> NGrammar.NNonterminal(303, Symbols.Nonterminal("Atom"), Set(257,304,308,319,332,335,347,357)),
305 -> NGrammar.NNonterminal(305, Symbols.Nonterminal("BindExpr"), Set(306)),
307 -> NGrammar.NNonterminal(307, Symbols.Nonterminal("BinderExpr"), Set(257,304,285)),
309 -> NGrammar.NNonterminal(309, Symbols.Nonterminal("NamedConstructExpr"), Set(310)),
311 -> NGrammar.NNonterminal(311, Symbols.Nonterminal("NamedConstructParams"), Set(312)),
313 -> NGrammar.NNonterminal(313, Symbols.Nonterminal("NamedParam"), Set(314)),
315 -> NGrammar.NRepeat(315, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0), 7, 316),
317 -> NGrammar.NProxy(317, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 318),
320 -> NGrammar.NNonterminal(320, Symbols.Nonterminal("FuncCallOrConstructExpr"), Set(321)),
322 -> NGrammar.NNonterminal(322, Symbols.Nonterminal("TypeOrFuncName"), Set(32,77)),
323 -> NGrammar.NNonterminal(323, Symbols.Nonterminal("CallParams"), Set(324)),
325 -> NGrammar.NOneOf(325, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))), Set(80,326)),
326 -> NGrammar.NProxy(326, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))), 327),
328 -> NGrammar.NRepeat(328, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0), 7, 329),
330 -> NGrammar.NProxy(330, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 331),
333 -> NGrammar.NNonterminal(333, Symbols.Nonterminal("ArrayExpr"), Set(334)),
336 -> NGrammar.NNonterminal(336, Symbols.Nonterminal("Literal"), Set(74,337,339,342)),
338 -> NGrammar.NOneOf(338, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))))), Set(67,71)),
340 -> NGrammar.NNonterminal(340, Symbols.Nonterminal("CharChar"), Set(341)),
343 -> NGrammar.NRepeat(343, Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), 7, 344),
345 -> NGrammar.NNonterminal(345, Symbols.Nonterminal("StrChar"), Set(346)),
348 -> NGrammar.NNonterminal(348, Symbols.Nonterminal("EnumValue"), Set(349,354)),
350 -> NGrammar.NNonterminal(350, Symbols.Nonterminal("CanonicalEnumValue"), Set(351)),
352 -> NGrammar.NNonterminal(352, Symbols.Nonterminal("EnumValueName"), Set(353)),
355 -> NGrammar.NNonterminal(355, Symbols.Nonterminal("ShortenedEnumValue"), Set(356)),
359 -> NGrammar.NProxy(359, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':')))), 360),
362 -> NGrammar.NOneOf(362, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))))), Set(363,365)),
363 -> NGrammar.NProxy(363, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))), 364),
365 -> NGrammar.NProxy(365, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))), 366),
368 -> NGrammar.NProxy(368, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|')))), 369),
371 -> NGrammar.NProxy(371, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&')))), 372),
374 -> NGrammar.NRepeat(374, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0), 7, 375),
376 -> NGrammar.NProxy(376, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 377),
378 -> NGrammar.NRepeat(378, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0), 7, 379),
380 -> NGrammar.NProxy(380, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 381),
382 -> NGrammar.NRepeat(382, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 0), 7, 383),
384 -> NGrammar.NProxy(384, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 385),
386 -> NGrammar.NNonterminal(386, Symbols.Nonterminal("WSNL"), Set(387))),
  Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 0),Symbols.Nonterminal("WS"))), Seq(4,23,382,4)),
5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0))), Seq(6)),
7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq()), Seq()),
8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0),Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))))), Seq(6,9)),
12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.ExactChar('/'),Symbols.ExactChar('/'),Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0),Symbols.OneOf(ListSet(Symbols.Nonterminal("EOF"),Symbols.ExactChar('\n'))))), Seq(13,13,14,19)),
15 -> NGrammar.NSequence(15, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0),Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')))), Seq(14,16)),
21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.LookaheadExcept(Symbols.AnyChar))), Seq(22)),
24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Nonterminal("Rule"))), Seq(25)),
26 -> NGrammar.NSequence(26, Symbols.Sequence(Seq(Symbols.Nonterminal("LHS"),Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0))), Seq(27,4,165,4,166,378)),
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
85 -> NGrammar.NSequence(85, Symbols.Sequence(Seq(Symbols.Nonterminal("NonNullTypeDesc"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))))))), Seq(86,161)),
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
138 -> NGrammar.NSequence(138, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SuperTypes")))))),Symbols.Nonterminal("WS"),Symbols.ExactChar('{'),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubTypes")))))),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(88,139,4,142,143,4,153)),
141 -> NGrammar.NSequence(141, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SuperTypes"))), Seq(4,109)),
145 -> NGrammar.NSequence(145, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubTypes"))), Seq(4,146)),
147 -> NGrammar.NSequence(147, Symbols.Sequence(Seq(Symbols.Nonterminal("SubType"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0))), Seq(148,149)),
150 -> NGrammar.NSequence(150, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))))), Seq(149,151)),
152 -> NGrammar.NSequence(152, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType"))), Seq(4,119,4,148)),
154 -> NGrammar.NSequence(154, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeDef"))), Seq(155)),
156 -> NGrammar.NSequence(156, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"),Symbols.Nonterminal("WS"),Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 0),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(101,4,142,4,34,157,4,153)),
158 -> NGrammar.NSequence(158, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))))), Seq(157,159)),
160 -> NGrammar.NSequence(160, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id"))), Seq(4,119,4,34)),
163 -> NGrammar.NSequence(163, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?'))), Seq(4,164)),
167 -> NGrammar.NSequence(167, Symbols.Sequence(Seq(Symbols.Nonterminal("Sequence"))), Seq(168)),
169 -> NGrammar.NSequence(169, Symbols.Sequence(Seq(Symbols.Nonterminal("Elem"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0))), Seq(170,374)),
171 -> NGrammar.NSequence(171, Symbols.Sequence(Seq(Symbols.Nonterminal("Symbol"))), Seq(172)),
173 -> NGrammar.NSequence(173, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"))), Seq(174)),
175 -> NGrammar.NSequence(175, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('&'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(174,4,176,4,177)),
178 -> NGrammar.NSequence(178, Symbols.Sequence(Seq(Symbols.ExactChar('^'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(179,4,177)),
180 -> NGrammar.NSequence(180, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(181,4,177)),
182 -> NGrammar.NSequence(182, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"))), Seq(183)),
184 -> NGrammar.NSequence(184, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('?'))), Seq(183,4,164)),
185 -> NGrammar.NSequence(185, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('*'))), Seq(183,4,186)),
187 -> NGrammar.NSequence(187, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('+'))), Seq(183,4,188)),
189 -> NGrammar.NSequence(189, Symbols.Sequence(Seq(Symbols.Nonterminal("AtomSymbol"))), Seq(190)),
191 -> NGrammar.NSequence(191, Symbols.Sequence(Seq(Symbols.Nonterminal("Terminal"))), Seq(192)),
193 -> NGrammar.NSequence(193, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChar"),Symbols.ExactChar('\''))), Seq(194,195,194)),
196 -> NGrammar.NSequence(196, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')))), Seq(197)),
199 -> NGrammar.NSequence(199, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('\'','\\','b','n','r','t')))), Seq(198,200)),
201 -> NGrammar.NSequence(201, Symbols.Sequence(Seq(Symbols.Nonterminal("UnicodeChar"))), Seq(202)),
203 -> NGrammar.NSequence(203, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('u'),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet))), Seq(198,69,204,204,204,204)),
205 -> NGrammar.NSequence(205, Symbols.Sequence(Seq(Symbols.ExactChar('.'))), Seq(206)),
207 -> NGrammar.NSequence(207, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoice"))), Seq(208)),
209 -> NGrammar.NSequence(209, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1),Symbols.ExactChar('\''))), Seq(194,210,222,194)),
211 -> NGrammar.NSequence(211, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"))), Seq(212)),
213 -> NGrammar.NSequence(213, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))))), Seq(214)),
216 -> NGrammar.NSequence(216, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('\'','-','\\','b','n','r','t')))), Seq(198,217)),
218 -> NGrammar.NSequence(218, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceRange"))), Seq(219)),
220 -> NGrammar.NSequence(220, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"),Symbols.ExactChar('-'),Symbols.Nonterminal("TerminalChoiceChar"))), Seq(212,221,212)),
223 -> NGrammar.NSequence(223, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1),Symbols.Nonterminal("TerminalChoiceElem"))), Seq(222,210)),
224 -> NGrammar.NSequence(224, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceRange"),Symbols.ExactChar('\''))), Seq(194,219,194)),
225 -> NGrammar.NSequence(225, Symbols.Sequence(Seq(Symbols.Nonterminal("StringSymbol"))), Seq(226)),
227 -> NGrammar.NSequence(227, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.ExactChar('"'))), Seq(228,229,228)),
230 -> NGrammar.NSequence(230, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.Nonterminal("StringChar"))), Seq(229,231)),
232 -> NGrammar.NSequence(232, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))))), Seq(233)),
235 -> NGrammar.NSequence(235, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('"','\\','b','n','r','t')))), Seq(198,236)),
237 -> NGrammar.NSequence(237, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"))), Seq(29)),
238 -> NGrammar.NSequence(238, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceChoices"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(124,4,239,4,135)),
240 -> NGrammar.NSequence(240, Symbols.Sequence(Seq(Symbols.Nonterminal("Sequence"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence")))), 0))), Seq(168,241)),
242 -> NGrammar.NSequence(242, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence")))))), Seq(241,243)),
244 -> NGrammar.NSequence(244, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence"))), Seq(4,245,4,168)),
246 -> NGrammar.NSequence(246, Symbols.Sequence(Seq(Symbols.Nonterminal("Longest"))), Seq(247)),
248 -> NGrammar.NSequence(248, Symbols.Sequence(Seq(Symbols.ExactChar('<'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceChoices"),Symbols.Nonterminal("WS"),Symbols.ExactChar('>'))), Seq(111,4,239,4,120)),
249 -> NGrammar.NSequence(249, Symbols.Sequence(Seq(Symbols.Nonterminal("EmptySequence"))), Seq(250)),
251 -> NGrammar.NSequence(251, Symbols.Sequence(Seq(Symbols.ExactChar('#'))), Seq(252)),
253 -> NGrammar.NSequence(253, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('-'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(174,4,221,4,177)),
254 -> NGrammar.NSequence(254, Symbols.Sequence(Seq(Symbols.Nonterminal("PreUnSymbol"))), Seq(177)),
255 -> NGrammar.NSequence(255, Symbols.Sequence(Seq(Symbols.Nonterminal("Processor"))), Seq(256)),
257 -> NGrammar.NSequence(257, Symbols.Sequence(Seq(Symbols.Nonterminal("Ref"))), Seq(258)),
259 -> NGrammar.NSequence(259, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"))), Seq(260)),
261 -> NGrammar.NSequence(261, Symbols.Sequence(Seq(Symbols.ExactChar('$'),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))),Symbols.Nonterminal("RefIdx"))), Seq(262,263,269)),
265 -> NGrammar.NSequence(265, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1))), Seq(266)),
268 -> NGrammar.NSequence(268, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1),Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))))), Seq(266,267)),
270 -> NGrammar.NSequence(270, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))))), Seq(271)),
275 -> NGrammar.NSequence(275, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(276,277)),
278 -> NGrammar.NSequence(278, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0),Symbols.Chars(('0' to '9').toSet))), Seq(277,279)),
280 -> NGrammar.NSequence(280, Symbols.Sequence(Seq(Symbols.Nonterminal("RawRef"))), Seq(281)),
282 -> NGrammar.NSequence(282, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$')))),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))),Symbols.Nonterminal("RefIdx"))), Seq(283,263,269)),
284 -> NGrammar.NSequence(284, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$'))), Seq(198,198,262)),
285 -> NGrammar.NSequence(285, Symbols.Sequence(Seq(Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(142,4,286,4,153)),
287 -> NGrammar.NSequence(287, Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))), Seq(288)),
289 -> NGrammar.NSequence(289, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"))), Seq(290)),
291 -> NGrammar.NSequence(291, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolOrExpr"))), Seq(292,4,371,4,290)),
293 -> NGrammar.NSequence(293, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolAndExpr"))), Seq(294,4,368,4,292)),
295 -> NGrammar.NSequence(295, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolEqExpr"))), Seq(296,4,362,4,294)),
297 -> NGrammar.NSequence(297, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ElvisExpr"))), Seq(298,4,359,4,296)),
299 -> NGrammar.NSequence(299, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar('+'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("AdditiveExpr"))), Seq(300,4,188,4,298)),
301 -> NGrammar.NSequence(301, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PrefixNotExpr"))), Seq(181,4,300)),
302 -> NGrammar.NSequence(302, Symbols.Sequence(Seq(Symbols.Nonterminal("Atom"))), Seq(303)),
304 -> NGrammar.NSequence(304, Symbols.Sequence(Seq(Symbols.Nonterminal("BindExpr"))), Seq(305)),
306 -> NGrammar.NSequence(306, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"),Symbols.Nonterminal("BinderExpr"))), Seq(260,307)),
308 -> NGrammar.NSequence(308, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedConstructExpr"))), Seq(309)),
310 -> NGrammar.NSequence(310, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("SuperTypes")))))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedConstructParams"))), Seq(88,139,4,311)),
312 -> NGrammar.NSequence(312, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(124,4,313,315,4,135)),
314 -> NGrammar.NSequence(314, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))),Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"))), Seq(130,79,4,165,4,286)),
316 -> NGrammar.NSequence(316, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))))), Seq(315,317)),
318 -> NGrammar.NSequence(318, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam"))), Seq(4,119,4,313)),
319 -> NGrammar.NSequence(319, Symbols.Sequence(Seq(Symbols.Nonterminal("FuncCallOrConstructExpr"))), Seq(320)),
321 -> NGrammar.NSequence(321, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeOrFuncName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("CallParams"))), Seq(322,4,323)),
324 -> NGrammar.NSequence(324, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(')'))), Seq(124,4,325,135)),
327 -> NGrammar.NSequence(327, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS"))), Seq(286,328,4)),
329 -> NGrammar.NSequence(329, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))))), Seq(328,330)),
331 -> NGrammar.NSequence(331, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"))), Seq(4,119,4,286)),
332 -> NGrammar.NSequence(332, Symbols.Sequence(Seq(Symbols.Nonterminal("ArrayExpr"))), Seq(333)),
334 -> NGrammar.NSequence(334, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(']'))), Seq(90,4,325,91)),
335 -> NGrammar.NSequence(335, Symbols.Sequence(Seq(Symbols.Nonterminal("Literal"))), Seq(336)),
337 -> NGrammar.NSequence(337, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))))))), Seq(338)),
339 -> NGrammar.NSequence(339, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("CharChar"),Symbols.ExactChar('\''))), Seq(194,340,194)),
341 -> NGrammar.NSequence(341, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChar"))), Seq(195)),
342 -> NGrammar.NSequence(342, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0),Symbols.ExactChar('"'))), Seq(228,343,228)),
344 -> NGrammar.NSequence(344, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0),Symbols.Nonterminal("StrChar"))), Seq(343,345)),
346 -> NGrammar.NSequence(346, Symbols.Sequence(Seq(Symbols.Nonterminal("StringChar"))), Seq(231)),
347 -> NGrammar.NSequence(347, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumValue"))), Seq(348)),
349 -> NGrammar.NSequence(349, Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue"))), Seq(350)),
351 -> NGrammar.NSequence(351, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"),Symbols.ExactChar('.'),Symbols.Nonterminal("EnumValueName"))), Seq(101,206,352)),
353 -> NGrammar.NSequence(353, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(34)),
354 -> NGrammar.NSequence(354, Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))), Seq(355)),
356 -> NGrammar.NSequence(356, Symbols.Sequence(Seq(Symbols.ExactChar('%'),Symbols.Nonterminal("EnumValueName"))), Seq(103,352)),
357 -> NGrammar.NSequence(357, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(124,4,286,4,135)),
358 -> NGrammar.NSequence(358, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"))), Seq(300)),
360 -> NGrammar.NSequence(360, Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':'))), Seq(164,83)),
361 -> NGrammar.NSequence(361, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"))), Seq(298)),
364 -> NGrammar.NSequence(364, Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('='))), Seq(165,165)),
366 -> NGrammar.NSequence(366, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('='))), Seq(181,165)),
367 -> NGrammar.NSequence(367, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"))), Seq(296)),
369 -> NGrammar.NSequence(369, Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|'))), Seq(245,245)),
370 -> NGrammar.NSequence(370, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"))), Seq(294)),
372 -> NGrammar.NSequence(372, Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&'))), Seq(176,176)),
373 -> NGrammar.NSequence(373, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"))), Seq(292)),
375 -> NGrammar.NSequence(375, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))))), Seq(374,376)),
377 -> NGrammar.NSequence(377, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem"))), Seq(4,170)),
379 -> NGrammar.NSequence(379, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))))), Seq(378,380)),
381 -> NGrammar.NSequence(381, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS"))), Seq(4,245,4,166)),
383 -> NGrammar.NSequence(383, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))))), Seq(382,384)),
385 -> NGrammar.NSequence(385, Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def"))), Seq(386,23)),
387 -> NGrammar.NSequence(387, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"))), Seq(4))),
  1)

  sealed trait ASTNode {
  val astNode: Node
  def prettyPrint(): String
}
case class Grammar(astNode:Node, defs:List[Def]) extends ASTNode{
  def prettyPrint(): String = "Grammar(" + "defs=" + "[" + defs.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
sealed trait Def extends ASTNode
case class Rule(astNode:Node, lhs:LHS, rhs:List[Sequence]) extends ASTNode with Def{
  def prettyPrint(): String = "Rule(" + "lhs=" + lhs.prettyPrint()+ ", " + "rhs=" + "[" + rhs.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
case class LHS(astNode:Node, name:Nonterminal, typeDesc:Option[TypeDesc]) extends ASTNode{
  def prettyPrint(): String = "LHS(" + "name=" + name.prettyPrint()+ ", " + "typeDesc=" + (typeDesc match { case Some(v) =>
  v.prettyPrint()
  case None => "null"
}) +  ")"
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
case class InPlaceChoices(astNode:Node, choices:List[Sequence]) extends ASTNode with AtomSymbol{
  def prettyPrint(): String = "InPlaceChoices(" + "choices=" + "[" + choices.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
case class Sequence(astNode:Node, seq:List[Elem]) extends ASTNode with Symbol{
  def prettyPrint(): String = "Sequence(" + "seq=" + "[" + seq.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
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
case class NamedConstructExpr(astNode:Node, typeName:TypeName, params:List[NamedParam], supers:Option[Option[List[TypeName]]]) extends ASTNode with Atom{
  def prettyPrint(): String = "NamedConstructExpr(" + "typeName=" + typeName.prettyPrint()+ ", " + "params=" + "[" + params.map(e => e.prettyPrint()).mkString(",") + "]"+ ", " + "supers=" + (supers match { case Some(v) =>
  (v match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
})
  case None => "null"
}) +  ")"
}
case class NamedParam(astNode:Node, name:ParamName, typeDesc:Option[TypeDesc], expr:PExpr) extends ASTNode{
  def prettyPrint(): String = "NamedParam(" + "name=" + name.prettyPrint()+ ", " + "typeDesc=" + (typeDesc match { case Some(v) =>
  v.prettyPrint()
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
case class CanonicalEnumValue(astNode:Node, enumName:EnumTypeName, valueName:EnumValueName) extends ASTNode with AbstractEnumValue{
  def prettyPrint(): String = "CanonicalEnumValue(" + "enumName=" + enumName.prettyPrint()+ ", " + "valueName=" + valueName.prettyPrint() +  ")"
}
case class ShortenedEnumValue(astNode:Node, valueName:EnumValueName) extends ASTNode with AbstractEnumValue{
  def prettyPrint(): String = "ShortenedEnumValue(" + "valueName=" + valueName.prettyPrint() +  ")"
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
case class SuperDef(astNode:Node, typeName:TypeName, subs:Option[List[SubType]], supers:Option[Option[List[TypeName]]]) extends ASTNode with SubType with TypeDef{
  def prettyPrint(): String = "SuperDef(" + "typeName=" + typeName.prettyPrint()+ ", " + "subs=" + (subs match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
})+ ", " + "supers=" + (supers match { case Some(v) =>
  (v match { case Some(v) =>
  "[" + v.map(e => e.prettyPrint()).mkString(",") + "]"
  case None => "null"
})
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
case class EnumValueName(astNode:Node, name:Node) extends ASTNode{
  def prettyPrint(): String = "EnumValueName(" + "name=" + name.sourceText +  ")"
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
assert(v7.id == 384)
val BindNode(v9, v10) = v8
assert(v9.id == 385)
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
assert(v32.id == 166)
val v34 = matchRHS(v33)
val v35 = List(v34)
val v36 = body.asInstanceOf[SequenceNode].children(5)
val v41 = unrollRepeat0(v36) map { n =>
val BindNode(v37, v38) = n
assert(v37.id == 380)
val BindNode(v39, v40) = v38
assert(v39.id == 381)
v40
}
val v46 = v41 map { n =>
val v42 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v43, v44) = v42
assert(v43.id == 166)
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
def matchRHS(node: Node): Sequence = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 167 =>
val v65 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v66, v67) = v65
assert(v66.id == 168)
val v68 = matchSequence(v67)
v68
  }
}
def matchElem(node: Node): Elem = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 171 =>
val v69 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v70, v71) = v69
assert(v70.id == 172)
val v72 = matchSymbol(v71)
v72
case 255 =>
val v73 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v74, v75) = v73
assert(v74.id == 256)
val v76 = matchProcessor(v75)
v76
  }
}
def matchSymbol(node: Node): Symbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 173 =>
val v77 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v78, v79) = v77
assert(v78.id == 174)
val v80 = matchBinSymbol(v79)
v80
  }
}
def matchBinSymbol(node: Node): BinSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 175 =>
val v81 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v82, v83) = v81
assert(v82.id == 174)
val v84 = matchBinSymbol(v83)
val v85 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v86, v87) = v85
assert(v86.id == 177)
val v88 = matchPreUnSymbol(v87)
val v89 = JoinSymbol(node,v84,v88)
v89
case 253 =>
val v90 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v91, v92) = v90
assert(v91.id == 174)
val v93 = matchBinSymbol(v92)
val v94 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v95, v96) = v94
assert(v95.id == 177)
val v97 = matchPreUnSymbol(v96)
val v98 = ExceptSymbol(node,v93,v97)
v98
case 254 =>
val v99 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v100, v101) = v99
assert(v100.id == 177)
val v102 = matchPreUnSymbol(v101)
v102
  }
}
def matchPreUnSymbol(node: Node): PreUnSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 178 =>
val v103 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v104, v105) = v103
assert(v104.id == 177)
val v106 = matchPreUnSymbol(v105)
val v107 = FollowedBy(node,v106)
v107
case 180 =>
val v108 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v109, v110) = v108
assert(v109.id == 177)
val v111 = matchPreUnSymbol(v110)
val v112 = NotFollowedBy(node,v111)
v112
case 182 =>
val v113 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v114, v115) = v113
assert(v114.id == 183)
val v116 = matchPostUnSymbol(v115)
v116
  }
}
def matchPostUnSymbol(node: Node): PostUnSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 184 =>
val v117 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v118, v119) = v117
assert(v118.id == 183)
val v120 = matchPostUnSymbol(v119)
val v121 = Optional(node,v120)
v121
case 185 =>
val v122 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v123, v124) = v122
assert(v123.id == 183)
val v125 = matchPostUnSymbol(v124)
val v126 = RepeatFromZero(node,v125)
v126
case 187 =>
val v127 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v128, v129) = v127
assert(v128.id == 183)
val v130 = matchPostUnSymbol(v129)
val v131 = RepeatFromOne(node,v130)
v131
case 189 =>
val v132 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v133, v134) = v132
assert(v133.id == 190)
val v135 = matchAtomSymbol(v134)
v135
  }
}
def matchAtomSymbol(node: Node): AtomSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 191 =>
val v136 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v137, v138) = v136
assert(v137.id == 192)
val v139 = matchTerminal(v138)
v139
case 207 =>
val v140 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v141, v142) = v140
assert(v141.id == 208)
val v143 = matchTerminalChoice(v142)
v143
case 225 =>
val v144 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v145, v146) = v144
assert(v145.id == 226)
val v147 = matchStringSymbol(v146)
v147
case 237 =>
val v148 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v149, v150) = v148
assert(v149.id == 29)
val v151 = matchNonterminal(v150)
v151
case 238 =>
val v152 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v153, v154) = v152
assert(v153.id == 239)
val v155 = matchInPlaceChoices(v154)
v155
case 246 =>
val v156 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v157, v158) = v156
assert(v157.id == 247)
val v159 = matchLongest(v158)
v159
case 249 =>
val v160 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v161, v162) = v160
assert(v161.id == 250)
val v163 = matchEmptySequence(v162)
v163
  }
}
def matchTerminal(node: Node): Terminal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 193 =>
val v164 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v165, v166) = v164
assert(v165.id == 195)
val v167 = matchTerminalChar(v166)
v167
case 205 =>
val v168 = AnyTerminal(node)
v168
  }
}
def matchTerminalChoice(node: Node): TerminalChoice = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 209 =>
val v169 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v170, v171) = v169
assert(v170.id == 210)
val v172 = matchTerminalChoiceElem(v171)
val v173 = List(v172)
val v174 = body.asInstanceOf[SequenceNode].children(2)
val v178 = unrollRepeat1(v174) map { n =>
val BindNode(v175, v176) = n
assert(v175.id == 210)
val v177 = matchTerminalChoiceElem(v176)
v177
}
val v179 = v178 map { n =>
n
}
val v180 = v173 ++ v179
val v181 = TerminalChoice(node,v180)
v181
case 224 =>
val v182 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v183, v184) = v182
assert(v183.id == 219)
val v185 = matchTerminalChoiceRange(v184)
val v186 = List(v185)
val v187 = TerminalChoice(node,v186)
v187
  }
}
def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 211 =>
val v188 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v189, v190) = v188
assert(v189.id == 212)
val v191 = matchTerminalChoiceChar(v190)
v191
case 218 =>
val v192 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v193, v194) = v192
assert(v193.id == 219)
val v195 = matchTerminalChoiceRange(v194)
v195
  }
}
def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 220 =>
val v196 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v197, v198) = v196
assert(v197.id == 212)
val v199 = matchTerminalChoiceChar(v198)
val v200 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v201, v202) = v200
assert(v201.id == 212)
val v203 = matchTerminalChoiceChar(v202)
val v204 = TerminalChoiceRange(node,v199,v203)
v204
  }
}
def matchStringSymbol(node: Node): StringSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 227 =>
val v205 = body.asInstanceOf[SequenceNode].children(1)
val v209 = unrollRepeat0(v205) map { n =>
val BindNode(v206, v207) = n
assert(v206.id == 231)
val v208 = matchStringChar(v207)
v208
}
val v210 = v209 map { n =>
n
}
val v211 = StringSymbol(node,v210)
v211
  }
}
def matchNonterminal(node: Node): Nonterminal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 30 =>
val v212 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v213, v214) = v212
assert(v213.id == 31)
val v215 = matchNonterminalName(v214)
val v216 = Nonterminal(node,v215)
v216
  }
}
def matchInPlaceChoices(node: Node): InPlaceChoices = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 240 =>
val v217 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v218, v219) = v217
assert(v218.id == 168)
val v220 = matchSequence(v219)
val v221 = List(v220)
val v222 = body.asInstanceOf[SequenceNode].children(1)
val v227 = ParseTreeUtil.unrollRepeat0(v222) map { n =>
val BindNode(v223, v224) = n
assert(v223.id == 243)
val BindNode(v225, v226) = v224
assert(v225.id == 244)
v226
}
val v232 = v227 map { n =>
val v228 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v229, v230) = v228
assert(v229.id == 168)
val v231 = matchSequence(v230)
v231
}
val v233 = v221 ++ v232
val v234 = InPlaceChoices(node,v233)
v234
  }
}
def matchSequence(node: Node): Sequence = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 169 =>
val v235 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v236, v237) = v235
assert(v236.id == 170)
val v238 = matchElem(v237)
val v239 = List(v238)
val v240 = body.asInstanceOf[SequenceNode].children(1)
val v245 = ParseTreeUtil.unrollRepeat0(v240) map { n =>
val BindNode(v241, v242) = n
assert(v241.id == 376)
val BindNode(v243, v244) = v242
assert(v243.id == 377)
v244
}
val v250 = v245 map { n =>
val v246 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v247, v248) = v246
assert(v247.id == 170)
val v249 = matchElem(v248)
v249
}
val v251 = v239 ++ v250
val v252 = Sequence(node,v251)
v252
  }
}
def matchLongest(node: Node): Longest = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 248 =>
val v253 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v254, v255) = v253
assert(v254.id == 239)
val v256 = matchInPlaceChoices(v255)
val v257 = Longest(node,v256)
v257
  }
}
def matchEmptySequence(node: Node): EmptySeq = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 251 =>
val v258 = EmptySeq(node)
v258
  }
}
def matchTerminalChar(node: Node): TerminalChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 196 =>
val v259 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v260, v261) = v259
assert(v260.id == 197)
val v262 = CharAsIs(node,v261)
v262
case 199 =>
val v263 = body.asInstanceOf[SequenceNode].children(1)
val v264 = CharEscaped(node,v263)
v264
case 201 =>
val v265 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v266, v267) = v265
assert(v266.id == 202)
val v268 = matchUnicodeChar(v267)
v268
  }
}
def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 213 =>
val v269 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v270, v271) = v269
assert(v270.id == 214)
val v272 = CharAsIs(node,v271)
v272
case 216 =>
val v273 = body.asInstanceOf[SequenceNode].children(1)
val v274 = CharEscaped(node,v273)
v274
case 201 =>
val v275 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v276, v277) = v275
assert(v276.id == 202)
val v278 = matchUnicodeChar(v277)
v278
  }
}
def matchStringChar(node: Node): StringChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 232 =>
val v279 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v280, v281) = v279
assert(v280.id == 233)
val v282 = CharAsIs(node,v281)
v282
case 235 =>
val v283 = body.asInstanceOf[SequenceNode].children(1)
val v284 = CharEscaped(node,v283)
v284
case 201 =>
val v285 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v286, v287) = v285
assert(v286.id == 202)
val v288 = matchUnicodeChar(v287)
v288
  }
}
def matchUnicodeChar(node: Node): CharUnicode = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 203 =>
val v289 = body.asInstanceOf[SequenceNode].children(2)
val v290 = body.asInstanceOf[SequenceNode].children(3)
val v291 = body.asInstanceOf[SequenceNode].children(4)
val v292 = body.asInstanceOf[SequenceNode].children(5)
val v293 = List(v289,v290,v291,v292)
val v294 = CharUnicode(node,v293)
v294
  }
}
def matchProcessor(node: Node): Processor = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 257 =>
val v295 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v296, v297) = v295
assert(v296.id == 258)
val v298 = matchRef(v297)
v298
case 285 =>
val v299 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v300, v301) = v299
assert(v300.id == 286)
val v302 = matchPExpr(v301)
v302
  }
}
def matchRef(node: Node): Ref = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 259 =>
val v303 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v304, v305) = v303
assert(v304.id == 260)
val v306 = matchValRef(v305)
v306
case 280 =>
val v307 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v308, v309) = v307
assert(v308.id == 281)
val v310 = matchRawRef(v309)
v310
  }
}
def matchValRef(node: Node): ValRef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 261 =>
val v311 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v312, v313) = v311
assert(v312.id == 269)
val v314 = matchRefIdx(v313)
val v315 = body.asInstanceOf[SequenceNode].children(1)
val v319 = unrollOptional(v315, 80, 264) map { n =>
val BindNode(v316, v317) = n
assert(v316.id == 264)
val v318 = matchCondSymPath(v317)
v318
}
val v320 = ValRef(node,v314,v319)
v320
  }
}
def matchCondSymPath(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 265 =>
val v321 = body.asInstanceOf[SequenceNode].children(0)
val v322 = unrollRepeat1(v321) map { n =>
// UnrollChoices
n
}
v322
  }
}
def matchRawRef(node: Node): RawRef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 282 =>
val v323 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v324, v325) = v323
assert(v324.id == 269)
val v326 = matchRefIdx(v325)
val v327 = body.asInstanceOf[SequenceNode].children(1)
val v331 = unrollOptional(v327, 80, 264) map { n =>
val BindNode(v328, v329) = n
assert(v328.id == 264)
val v330 = matchCondSymPath(v329)
v330
}
val v332 = RawRef(node,v326,v331)
v332
  }
}
def matchPExpr(node: Node): PExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 287 =>
val v333 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v334, v335) = v333
assert(v334.id == 288)
val v336 = matchTernaryExpr(v335)
v336
  }
}
def matchTernaryExpr(node: Node): TerExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 289 =>
val v337 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v338, v339) = v337
assert(v338.id == 290)
val v340 = matchBoolOrExpr(v339)
v340
  }
}
def matchBoolOrExpr(node: Node): BoolOrExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 291 =>
val v341 = body.asInstanceOf[SequenceNode].children(2)
val v342 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v343, v344) = v342
assert(v343.id == 292)
val v345 = matchBoolAndExpr(v344)
val v346 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v347, v348) = v346
assert(v347.id == 290)
val v349 = matchBoolOrExpr(v348)
val v350 = BinOp(node,v341,v345,v349)
v350
case 373 =>
val v351 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v352, v353) = v351
assert(v352.id == 292)
val v354 = matchBoolAndExpr(v353)
v354
  }
}
def matchBoolAndExpr(node: Node): BoolAndExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 293 =>
val v355 = body.asInstanceOf[SequenceNode].children(2)
val v356 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v357, v358) = v356
assert(v357.id == 294)
val v359 = matchBoolEqExpr(v358)
val v360 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v361, v362) = v360
assert(v361.id == 292)
val v363 = matchBoolAndExpr(v362)
val v364 = BinOp(node,v355,v359,v363)
v364
case 370 =>
val v365 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v366, v367) = v365
assert(v366.id == 294)
val v368 = matchBoolEqExpr(v367)
v368
  }
}
def matchBoolEqExpr(node: Node): BoolEqExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 295 =>
// UnrollChoices
val v369 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v370, v371) = v369
assert(v370.id == 296)
val v372 = matchElvisExpr(v371)
val v373 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v374, v375) = v373
assert(v374.id == 294)
val v376 = matchBoolEqExpr(v375)
val v377 = BinOp(node,body.asInstanceOf[SequenceNode].children(2),v372,v376)
v377
case 367 =>
val v378 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v379, v380) = v378
assert(v379.id == 296)
val v381 = matchElvisExpr(v380)
v381
  }
}
def matchElvisExpr(node: Node): ElvisExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 297 =>
val v382 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v383, v384) = v382
assert(v383.id == 298)
val v385 = matchAdditiveExpr(v384)
val v386 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v387, v388) = v386
assert(v387.id == 296)
val v389 = matchElvisExpr(v388)
val v390 = ElvisOp(node,v385,v389)
v390
case 361 =>
val v391 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v392, v393) = v391
assert(v392.id == 298)
val v394 = matchAdditiveExpr(v393)
v394
  }
}
def matchAdditiveExpr(node: Node): AdditiveExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 299 =>
val v395 = body.asInstanceOf[SequenceNode].children(2)
val v396 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v397, v398) = v396
assert(v397.id == 300)
val v399 = matchPrefixNotExpr(v398)
val v400 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v401, v402) = v400
assert(v401.id == 298)
val v403 = matchAdditiveExpr(v402)
val v404 = BinOp(node,v395,v399,v403)
v404
case 358 =>
val v405 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v406, v407) = v405
assert(v406.id == 300)
val v408 = matchPrefixNotExpr(v407)
v408
  }
}
def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 301 =>
val v409 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v410, v411) = v409
assert(v410.id == 300)
val v412 = matchPrefixNotExpr(v411)
val v413 = body.asInstanceOf[SequenceNode].children(0)
val v414 = PrefixOp(node,v412,v413)
v414
case 302 =>
val v415 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v416, v417) = v415
assert(v416.id == 303)
val v418 = matchAtom(v417)
v418
  }
}
def matchAtom(node: Node): Atom = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 257 =>
val v419 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v420, v421) = v419
assert(v420.id == 258)
val v422 = matchRef(v421)
v422
case 304 =>
val v423 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v424, v425) = v423
assert(v424.id == 305)
val v426 = matchBindExpr(v425)
v426
case 308 =>
val v427 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v428, v429) = v427
assert(v428.id == 309)
val v430 = matchNamedConstructExpr(v429)
v430
case 319 =>
val v431 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v432, v433) = v431
assert(v432.id == 320)
val v434 = matchFuncCallOrConstructExpr(v433)
v434
case 332 =>
val v435 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v436, v437) = v435
assert(v436.id == 333)
val v438 = matchArrayExpr(v437)
v438
case 335 =>
val v439 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v440, v441) = v439
assert(v440.id == 336)
val v442 = matchLiteral(v441)
v442
case 347 =>
val v443 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v444, v445) = v443
assert(v444.id == 348)
val v446 = matchEnumValue(v445)
v446
case 357 =>
val v447 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v448, v449) = v447
assert(v448.id == 286)
val v450 = matchPExpr(v449)
val v451 = ExprParen(node,v450)
v451
  }
}
def matchBindExpr(node: Node): BindExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 306 =>
val v452 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v453, v454) = v452
assert(v453.id == 260)
val v455 = matchValRef(v454)
val v456 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v457, v458) = v456
assert(v457.id == 307)
val v459 = matchBinderExpr(v458)
val v460 = BindExpr(node,v455,v459)
v460
  }
}
def matchBinderExpr(node: Node): BinderExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 257 =>
val v461 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v462, v463) = v461
assert(v462.id == 258)
val v464 = matchRef(v463)
v464
case 304 =>
val v465 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v466, v467) = v465
assert(v466.id == 305)
val v468 = matchBindExpr(v467)
v468
case 285 =>
val v469 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v470, v471) = v469
assert(v470.id == 286)
val v472 = matchPExpr(v471)
v472
  }
}
def matchNamedConstructExpr(node: Node): NamedConstructExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 310 =>
val v473 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v474, v475) = v473
assert(v474.id == 88)
val v476 = matchTypeName(v475)
val v477 = body.asInstanceOf[SequenceNode].children(3)
val BindNode(v478, v479) = v477
assert(v478.id == 311)
val v480 = matchNamedConstructParams(v479)
val v481 = body.asInstanceOf[SequenceNode].children(1)
val v486 = unrollOptional(v481, 80, 140) map { n =>
val BindNode(v482, v483) = n
assert(v482.id == 140)
val BindNode(v484, v485) = v483
assert(v484.id == 141)
v485
}
val v491 = v486 map { n =>
val v487 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v488, v489) = v487
assert(v488.id == 109)
val v490 = matchSuperTypes(v489)
v490
}
val v492 = NamedConstructExpr(node,v476,v480,v491)
v492
  }
}
def matchNamedConstructParams(node: Node): List[NamedParam] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 312 =>
val v493 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v494, v495) = v493
assert(v494.id == 313)
val v496 = matchNamedParam(v495)
val v497 = List(v496)
val v498 = body.asInstanceOf[SequenceNode].children(3)
val v503 = ParseTreeUtil.unrollRepeat0(v498) map { n =>
val BindNode(v499, v500) = n
assert(v499.id == 317)
val BindNode(v501, v502) = v500
assert(v501.id == 318)
v502
}
val v508 = v503 map { n =>
val v504 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v505, v506) = v504
assert(v505.id == 313)
val v507 = matchNamedParam(v506)
v507
}
val v509 = v497 ++ v508
v509
  }
}
def matchNamedParam(node: Node): NamedParam = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 314 =>
val v510 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v511, v512) = v510
assert(v511.id == 130)
val v513 = matchParamName(v512)
val v514 = body.asInstanceOf[SequenceNode].children(1)
val v519 = unrollOptional(v514, 80, 81) map { n =>
val BindNode(v515, v516) = n
assert(v515.id == 81)
val BindNode(v517, v518) = v516
assert(v517.id == 82)
v518
}
val v524 = v519 map { n =>
val v520 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v521, v522) = v520
assert(v521.id == 84)
val v523 = matchTypeDesc(v522)
v523
}
val v525 = body.asInstanceOf[SequenceNode].children(5)
val BindNode(v526, v527) = v525
assert(v526.id == 286)
val v528 = matchPExpr(v527)
val v529 = NamedParam(node,v513,v524,v528)
v529
  }
}
def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 321 =>
val v530 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v531, v532) = v530
assert(v531.id == 322)
val v533 = matchTypeOrFuncName(v532)
val v534 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v535, v536) = v534
assert(v535.id == 323)
val v537 = matchCallParams(v536)
val v538 = FuncCallOrConstructExpr(node,v533,v537)
v538
  }
}
def matchCallParams(node: Node): Option[List[PExpr]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 324 =>
val v539 = body.asInstanceOf[SequenceNode].children(2)
val v544 = unrollOptional(v539, 80, 326) map { n =>
val BindNode(v540, v541) = n
assert(v540.id == 326)
val BindNode(v542, v543) = v541
assert(v542.id == 327)
v543
}
val v562 = v544 map { n =>
val v545 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v546, v547) = v545
assert(v546.id == 286)
val v548 = matchPExpr(v547)
val v549 = List(v548)
val v550 = n.asInstanceOf[SequenceNode].children(1)
val v555 = ParseTreeUtil.unrollRepeat0(v550) map { n =>
val BindNode(v551, v552) = n
assert(v551.id == 330)
val BindNode(v553, v554) = v552
assert(v553.id == 331)
v554
}
val v560 = v555 map { n =>
val v556 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v557, v558) = v556
assert(v557.id == 286)
val v559 = matchPExpr(v558)
v559
}
val v561 = v549 ++ v560
v561
}
v562
  }
}
def matchArrayExpr(node: Node): ArrayExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 334 =>
val v563 = body.asInstanceOf[SequenceNode].children(2)
val v568 = unrollOptional(v563, 80, 326) map { n =>
val BindNode(v564, v565) = n
assert(v564.id == 326)
val BindNode(v566, v567) = v565
assert(v566.id == 327)
v567
}
val v586 = v568 map { n =>
val v569 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v570, v571) = v569
assert(v570.id == 286)
val v572 = matchPExpr(v571)
val v573 = List(v572)
val v574 = n.asInstanceOf[SequenceNode].children(1)
val v579 = ParseTreeUtil.unrollRepeat0(v574) map { n =>
val BindNode(v575, v576) = n
assert(v575.id == 330)
val BindNode(v577, v578) = v576
assert(v577.id == 331)
v578
}
val v584 = v579 map { n =>
val v580 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v581, v582) = v580
assert(v581.id == 286)
val v583 = matchPExpr(v582)
v583
}
val v585 = v573 ++ v584
v585
}
val v587 = ArrayExpr(node,v586)
v587
  }
}
def matchLiteral(node: Node): Literal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 74 =>
val v588 = NullLiteral(node)
v588
case 337 =>
// UnrollChoices
val v589 = BoolLiteral(node,body)
v589
case 339 =>
val v590 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v591, v592) = v590
assert(v591.id == 340)
val v593 = matchCharChar(v592)
val v594 = CharLiteral(node,v593)
v594
case 342 =>
val v595 = body.asInstanceOf[SequenceNode].children(1)
val v599 = ParseTreeUtil.unrollRepeat0(v595) map { n =>
val BindNode(v596, v597) = n
assert(v596.id == 345)
val v598 = matchStrChar(v597)
v598
}
val v600 = StringLiteral(node,v599)
v600
  }
}
def matchEnumValue(node: Node): AbstractEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 349 =>
val v601 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v602, v603) = v601
assert(v602.id == 350)
val v604 = matchCanonicalEnumValue(v603)
v604
case 354 =>
val v605 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v606, v607) = v605
assert(v606.id == 355)
val v608 = matchShortenedEnumValue(v607)
v608
  }
}
def matchCanonicalEnumValue(node: Node): CanonicalEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 351 =>
val v609 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v610, v611) = v609
assert(v610.id == 101)
val v612 = matchEnumTypeName(v611)
val v613 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v614, v615) = v613
assert(v614.id == 352)
val v616 = matchEnumValueName(v615)
val v617 = CanonicalEnumValue(node,v612,v616)
v617
  }
}
def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 356 =>
val v618 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v619, v620) = v618
assert(v619.id == 352)
val v621 = matchEnumValueName(v620)
val v622 = ShortenedEnumValue(node,v621)
v622
  }
}
def matchTypeDef(node: Node): TypeDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 106 =>
val v623 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v624, v625) = v623
assert(v624.id == 107)
val v626 = matchClassDef(v625)
v626
case 136 =>
val v627 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v628, v629) = v627
assert(v628.id == 137)
val v630 = matchSuperDef(v629)
v630
case 154 =>
val v631 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v632, v633) = v631
assert(v632.id == 155)
val v634 = matchEnumTypeDef(v633)
v634
  }
}
def matchClassDef(node: Node): ClassDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 108 =>
val v635 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v636, v637) = v635
assert(v636.id == 88)
val v638 = matchTypeName(v637)
val v639 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v640, v641) = v639
assert(v640.id == 109)
val v642 = matchSuperTypes(v641)
val v643 = AbstractClassDef(node,v638,v642)
v643
case 121 =>
val v644 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v645, v646) = v644
assert(v645.id == 88)
val v647 = matchTypeName(v646)
val v648 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v649, v650) = v648
assert(v649.id == 109)
val v651 = matchSuperTypes(v650)
val v652 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v653, v654) = v652
assert(v653.id == 122)
val v655 = matchClassParamsDef(v654)
val v656 = ConcreteClassDef(node,v647,v651,v655)
v656
  }
}
def matchSuperTypes(node: Node): Option[List[TypeName]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 110 =>
val v657 = body.asInstanceOf[SequenceNode].children(2)
val v662 = unrollOptional(v657, 80, 113) map { n =>
val BindNode(v658, v659) = n
assert(v658.id == 113)
val BindNode(v660, v661) = v659
assert(v660.id == 114)
v661
}
val v680 = v662 map { n =>
val v663 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v664, v665) = v663
assert(v664.id == 88)
val v666 = matchTypeName(v665)
val v667 = List(v666)
val v668 = n.asInstanceOf[SequenceNode].children(1)
val v673 = ParseTreeUtil.unrollRepeat0(v668) map { n =>
val BindNode(v669, v670) = n
assert(v669.id == 117)
val BindNode(v671, v672) = v670
assert(v671.id == 118)
v672
}
val v678 = v673 map { n =>
val v674 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v675, v676) = v674
assert(v675.id == 88)
val v677 = matchTypeName(v676)
v677
}
val v679 = v667 ++ v678
v679
}
v680
  }
}
def matchClassParamsDef(node: Node): Option[List[ClassParamDef]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 123 =>
val v681 = body.asInstanceOf[SequenceNode].children(2)
val v686 = unrollOptional(v681, 80, 126) map { n =>
val BindNode(v682, v683) = n
assert(v682.id == 126)
val BindNode(v684, v685) = v683
assert(v684.id == 127)
v685
}
val v704 = v686 map { n =>
val v687 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v688, v689) = v687
assert(v688.id == 128)
val v690 = matchClassParamDef(v689)
val v691 = List(v690)
val v692 = n.asInstanceOf[SequenceNode].children(1)
val v697 = ParseTreeUtil.unrollRepeat0(v692) map { n =>
val BindNode(v693, v694) = n
assert(v693.id == 133)
val BindNode(v695, v696) = v694
assert(v695.id == 134)
v696
}
val v702 = v697 map { n =>
val v698 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v699, v700) = v698
assert(v699.id == 128)
val v701 = matchClassParamDef(v700)
v701
}
val v703 = v691 ++ v702
v703
}
v704
  }
}
def matchClassParamDef(node: Node): ClassParamDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 129 =>
val v705 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v706, v707) = v705
assert(v706.id == 130)
val v708 = matchParamName(v707)
val v709 = body.asInstanceOf[SequenceNode].children(1)
val v714 = unrollOptional(v709, 80, 81) map { n =>
val BindNode(v710, v711) = n
assert(v710.id == 81)
val BindNode(v712, v713) = v711
assert(v712.id == 82)
v713
}
val v719 = v714 map { n =>
val v715 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v716, v717) = v715
assert(v716.id == 84)
val v718 = matchTypeDesc(v717)
v718
}
val v720 = ClassParamDef(node,v708,v719)
v720
  }
}
def matchSuperDef(node: Node): SuperDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 138 =>
val v721 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v722, v723) = v721
assert(v722.id == 88)
val v724 = matchTypeName(v723)
val v725 = body.asInstanceOf[SequenceNode].children(4)
val v730 = unrollOptional(v725, 80, 144) map { n =>
val BindNode(v726, v727) = n
assert(v726.id == 144)
val BindNode(v728, v729) = v727
assert(v728.id == 145)
v729
}
val v735 = v730 map { n =>
val v731 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v732, v733) = v731
assert(v732.id == 146)
val v734 = matchSubTypes(v733)
v734
}
val v736 = body.asInstanceOf[SequenceNode].children(1)
val v741 = unrollOptional(v736, 80, 140) map { n =>
val BindNode(v737, v738) = n
assert(v737.id == 140)
val BindNode(v739, v740) = v738
assert(v739.id == 141)
v740
}
val v746 = v741 map { n =>
val v742 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v743, v744) = v742
assert(v743.id == 109)
val v745 = matchSuperTypes(v744)
v745
}
val v747 = SuperDef(node,v724,v735,v746)
v747
  }
}
def matchSubTypes(node: Node): List[SubType] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 147 =>
val v748 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v749, v750) = v748
assert(v749.id == 148)
val v751 = matchSubType(v750)
val v752 = List(v751)
val v753 = body.asInstanceOf[SequenceNode].children(1)
val v758 = ParseTreeUtil.unrollRepeat0(v753) map { n =>
val BindNode(v754, v755) = n
assert(v754.id == 151)
val BindNode(v756, v757) = v755
assert(v756.id == 152)
v757
}
val v763 = v758 map { n =>
val v759 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v760, v761) = v759
assert(v760.id == 148)
val v762 = matchSubType(v761)
v762
}
val v764 = v752 ++ v763
v764
  }
}
def matchSubType(node: Node): SubType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 87 =>
val v765 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v766, v767) = v765
assert(v766.id == 88)
val v768 = matchTypeName(v767)
v768
case 106 =>
val v769 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v770, v771) = v769
assert(v770.id == 107)
val v772 = matchClassDef(v771)
v772
case 136 =>
val v773 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v774, v775) = v773
assert(v774.id == 137)
val v776 = matchSuperDef(v775)
v776
  }
}
def matchEnumTypeDef(node: Node): EnumTypeDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 156 =>
val v777 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v778, v779) = v777
assert(v778.id == 101)
val v780 = matchEnumTypeName(v779)
val v781 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v782, v783) = v781
assert(v782.id == 34)
val v784 = matchId(v783)
val v785 = List(v784)
val v786 = body.asInstanceOf[SequenceNode].children(5)
val v791 = ParseTreeUtil.unrollRepeat0(v786) map { n =>
val BindNode(v787, v788) = n
assert(v787.id == 159)
val BindNode(v789, v790) = v788
assert(v789.id == 160)
v790
}
val v796 = v791 map { n =>
val v792 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v793, v794) = v792
assert(v793.id == 34)
val v795 = matchId(v794)
v795
}
val v797 = v785 ++ v796
val v798 = EnumTypeDef(node,v780,v797)
v798
  }
}
def matchTypeDesc(node: Node): TypeDesc = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 85 =>
val v799 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v800, v801) = v799
assert(v800.id == 86)
val v802 = matchNonNullTypeDesc(v801)
val v803 = body.asInstanceOf[SequenceNode].children(1)
val v808 = unrollOptional(v803, 80, 162) map { n =>
val BindNode(v804, v805) = n
assert(v804.id == 162)
val BindNode(v806, v807) = v805
assert(v806.id == 163)
v807
}
val v809 = TypeDesc(node,v802,v808)
v809
  }
}
def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 87 =>
val v810 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v811, v812) = v810
assert(v811.id == 88)
val v813 = matchTypeName(v812)
v813
case 89 =>
val v814 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v815, v816) = v814
assert(v815.id == 84)
val v817 = matchTypeDesc(v816)
val v818 = ArrayTypeDesc(node,v817)
v818
case 92 =>
val v819 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v820, v821) = v819
assert(v820.id == 93)
val v822 = matchValueType(v821)
v822
case 94 =>
val v823 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v824, v825) = v823
assert(v824.id == 95)
val v826 = matchAnyType(v825)
v826
case 100 =>
val v827 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v828, v829) = v827
assert(v828.id == 101)
val v830 = matchEnumTypeName(v829)
v830
case 104 =>
val v831 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v832, v833) = v831
assert(v832.id == 105)
val v834 = matchTypeDef(v833)
v834
  }
}
def matchValueType(node: Node): ValueType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 44 =>
val v835 = BooleanType(node)
v835
case 53 =>
val v836 = CharType(node)
v836
case 59 =>
val v837 = StringType(node)
v837
  }
}
def matchAnyType(node: Node): AnyType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 96 =>
val v838 = AnyType(node)
v838
  }
}
def matchEnumTypeName(node: Node): EnumTypeName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 102 =>
val v839 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v840, v841) = v839
assert(v840.id == 34)
val v842 = matchId(v841)
val v843 = EnumTypeName(node,v842)
v843
  }
}
def matchTypeName(node: Node): TypeName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v844 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v845, v846) = v844
assert(v845.id == 33)
val v847 = TypeName(node,v846)
v847
case 77 =>
val v848 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v849, v850) = v848
assert(v849.id == 34)
val v851 = matchId(v850)
val v852 = TypeName(node,v851)
v852
  }
}
def matchNonterminalName(node: Node): NonterminalName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v853 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v854, v855) = v853
assert(v854.id == 33)
val v856 = NonterminalName(node,v855)
v856
case 77 =>
val v857 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v858, v859) = v857
assert(v858.id == 34)
val v860 = matchId(v859)
val v861 = NonterminalName(node,v860)
v861
  }
}
def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v862 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v863, v864) = v862
assert(v863.id == 33)
val v865 = TypeOrFuncName(node,v864)
v865
case 77 =>
val v866 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v867, v868) = v866
assert(v867.id == 34)
val v869 = matchId(v868)
val v870 = TypeOrFuncName(node,v869)
v870
  }
}
def matchParamName(node: Node): ParamName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v871 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v872, v873) = v871
assert(v872.id == 33)
val v874 = ParamName(node,v873)
v874
case 77 =>
val v875 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v876, v877) = v875
assert(v876.id == 34)
val v878 = matchId(v877)
val v879 = ParamName(node,v878)
v879
  }
}
def matchEnumValueName(node: Node): EnumValueName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 353 =>
val v880 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v881, v882) = v880
assert(v881.id == 34)
val v883 = matchId(v882)
val v884 = EnumValueName(node,v883)
v884
  }
}
def matchKeyword(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 44 =>
val v885 = body.asInstanceOf[SequenceNode].children(0)
v885
case 53 =>
val v886 = body.asInstanceOf[SequenceNode].children(0)
v886
case 59 =>
val v887 = body.asInstanceOf[SequenceNode].children(0)
v887
case 66 =>
val v888 = body.asInstanceOf[SequenceNode].children(0)
v888
case 70 =>
val v889 = body.asInstanceOf[SequenceNode].children(0)
v889
case 74 =>
val v890 = body.asInstanceOf[SequenceNode].children(0)
v890
  }
}
def matchStrChar(node: Node): StringChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 346 =>
val v891 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v892, v893) = v891
assert(v892.id == 231)
val v894 = matchStringChar(v893)
v894
  }
}
def matchCharChar(node: Node): TerminalChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 341 =>
val v895 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v896, v897) = v895
assert(v896.id == 195)
val v898 = matchTerminalChar(v897)
v898
  }
}
def matchRefIdx(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 270 =>
val v899 = body.asInstanceOf[SequenceNode].children(0)
v899
  }
}
def matchId(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 35 =>
val v900 = body.asInstanceOf[SequenceNode].children(0)
v900
  }
}
def matchWS(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 5 =>
val v901 = body.asInstanceOf[SequenceNode].children(0)
val v902 = ParseTreeUtil.unrollRepeat0(v901) map { n =>
// UnrollChoices
n
}
v902
  }
}
def matchWSNL(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 387 =>
val v903 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v904, v905) = v903
assert(v904.id == 4)
val v906 = matchWS(v905)
v906
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
val v907 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v908, v909) = v907
assert(v908.id == 22)
v909
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