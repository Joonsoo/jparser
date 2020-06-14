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
165 -> NGrammar.NNonterminal(165, Symbols.Nonterminal("Sequence"), Set(166)),
167 -> NGrammar.NNonterminal(167, Symbols.Nonterminal("Elem"), Set(168,252)),
169 -> NGrammar.NNonterminal(169, Symbols.Nonterminal("Symbol"), Set(170)),
171 -> NGrammar.NNonterminal(171, Symbols.Nonterminal("BinSymbol"), Set(172,250,251)),
173 -> NGrammar.NTerminal(173, Symbols.ExactChar('&')),
174 -> NGrammar.NNonterminal(174, Symbols.Nonterminal("PreUnSymbol"), Set(175,177,179)),
176 -> NGrammar.NTerminal(176, Symbols.ExactChar('^')),
178 -> NGrammar.NTerminal(178, Symbols.ExactChar('!')),
180 -> NGrammar.NNonterminal(180, Symbols.Nonterminal("PostUnSymbol"), Set(181,182,184,186)),
183 -> NGrammar.NTerminal(183, Symbols.ExactChar('*')),
185 -> NGrammar.NTerminal(185, Symbols.ExactChar('+')),
187 -> NGrammar.NNonterminal(187, Symbols.Nonterminal("AtomSymbol"), Set(188,204,222,234,235,243,246)),
189 -> NGrammar.NNonterminal(189, Symbols.Nonterminal("Terminal"), Set(190,202)),
191 -> NGrammar.NTerminal(191, Symbols.ExactChar('\'')),
192 -> NGrammar.NNonterminal(192, Symbols.Nonterminal("TerminalChar"), Set(193,196,198)),
194 -> NGrammar.NExcept(194, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')), 17, 195),
195 -> NGrammar.NTerminal(195, Symbols.ExactChar('\\')),
197 -> NGrammar.NTerminal(197, Symbols.Chars(Set('\'','\\','b','n','r','t'))),
199 -> NGrammar.NNonterminal(199, Symbols.Nonterminal("UnicodeChar"), Set(200)),
201 -> NGrammar.NTerminal(201, Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet)),
203 -> NGrammar.NTerminal(203, Symbols.ExactChar('.')),
205 -> NGrammar.NNonterminal(205, Symbols.Nonterminal("TerminalChoice"), Set(206,221)),
207 -> NGrammar.NNonterminal(207, Symbols.Nonterminal("TerminalChoiceElem"), Set(208,215)),
209 -> NGrammar.NNonterminal(209, Symbols.Nonterminal("TerminalChoiceChar"), Set(210,213,198)),
211 -> NGrammar.NExcept(211, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))), 17, 212),
212 -> NGrammar.NTerminal(212, Symbols.Chars(Set('\'','-','\\'))),
214 -> NGrammar.NTerminal(214, Symbols.Chars(Set('\'','-','\\','b','n','r','t'))),
216 -> NGrammar.NNonterminal(216, Symbols.Nonterminal("TerminalChoiceRange"), Set(217)),
218 -> NGrammar.NTerminal(218, Symbols.ExactChar('-')),
219 -> NGrammar.NRepeat(219, Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), 207, 220),
223 -> NGrammar.NNonterminal(223, Symbols.Nonterminal("StringSymbol"), Set(224)),
225 -> NGrammar.NTerminal(225, Symbols.ExactChar('"')),
226 -> NGrammar.NRepeat(226, Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), 7, 227),
228 -> NGrammar.NNonterminal(228, Symbols.Nonterminal("StringChar"), Set(229,232,198)),
230 -> NGrammar.NExcept(230, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))), 17, 231),
231 -> NGrammar.NTerminal(231, Symbols.Chars(Set('"','\\'))),
233 -> NGrammar.NTerminal(233, Symbols.Chars(Set('"','\\','b','n','r','t'))),
236 -> NGrammar.NNonterminal(236, Symbols.Nonterminal("InPlaceChoices"), Set(237)),
238 -> NGrammar.NRepeat(238, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence")))), 0), 7, 239),
240 -> NGrammar.NProxy(240, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence")))), 241),
242 -> NGrammar.NTerminal(242, Symbols.ExactChar('|')),
244 -> NGrammar.NNonterminal(244, Symbols.Nonterminal("Longest"), Set(245)),
247 -> NGrammar.NNonterminal(247, Symbols.Nonterminal("EmptySequence"), Set(248)),
249 -> NGrammar.NTerminal(249, Symbols.ExactChar('#')),
253 -> NGrammar.NNonterminal(253, Symbols.Nonterminal("Processor"), Set(254,282)),
255 -> NGrammar.NNonterminal(255, Symbols.Nonterminal("Ref"), Set(256,277)),
257 -> NGrammar.NNonterminal(257, Symbols.Nonterminal("ValRef"), Set(258)),
259 -> NGrammar.NTerminal(259, Symbols.ExactChar('$')),
260 -> NGrammar.NOneOf(260, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))), Set(80,261)),
261 -> NGrammar.NNonterminal(261, Symbols.Nonterminal("CondSymPath"), Set(262)),
263 -> NGrammar.NRepeat(263, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1), 264, 265),
264 -> NGrammar.NOneOf(264, Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), Set(111,120)),
266 -> NGrammar.NNonterminal(266, Symbols.Nonterminal("RefIdx"), Set(267)),
268 -> NGrammar.NLongest(268, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))), 269),
269 -> NGrammar.NOneOf(269, Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))))), Set(270,271)),
270 -> NGrammar.NTerminal(270, Symbols.ExactChar('0')),
271 -> NGrammar.NProxy(271, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))), 272),
273 -> NGrammar.NTerminal(273, Symbols.Chars(('1' to '9').toSet)),
274 -> NGrammar.NRepeat(274, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 7, 275),
276 -> NGrammar.NTerminal(276, Symbols.Chars(('0' to '9').toSet)),
278 -> NGrammar.NNonterminal(278, Symbols.Nonterminal("RawRef"), Set(279)),
280 -> NGrammar.NProxy(280, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$')))), 281),
283 -> NGrammar.NNonterminal(283, Symbols.Nonterminal("PExpr"), Set(284)),
285 -> NGrammar.NNonterminal(285, Symbols.Nonterminal("TernaryExpr"), Set(286)),
287 -> NGrammar.NNonterminal(287, Symbols.Nonterminal("BoolOrExpr"), Set(288,370)),
289 -> NGrammar.NNonterminal(289, Symbols.Nonterminal("BoolAndExpr"), Set(290,367)),
291 -> NGrammar.NNonterminal(291, Symbols.Nonterminal("BoolEqExpr"), Set(292,364)),
293 -> NGrammar.NNonterminal(293, Symbols.Nonterminal("ElvisExpr"), Set(294,358)),
295 -> NGrammar.NNonterminal(295, Symbols.Nonterminal("AdditiveExpr"), Set(296,355)),
297 -> NGrammar.NNonterminal(297, Symbols.Nonterminal("PrefixNotExpr"), Set(298,299)),
300 -> NGrammar.NNonterminal(300, Symbols.Nonterminal("Atom"), Set(254,301,305,316,329,332,344,354)),
302 -> NGrammar.NNonterminal(302, Symbols.Nonterminal("BindExpr"), Set(303)),
304 -> NGrammar.NNonterminal(304, Symbols.Nonterminal("BinderExpr"), Set(254,301,282)),
306 -> NGrammar.NNonterminal(306, Symbols.Nonterminal("NamedConstructExpr"), Set(307)),
308 -> NGrammar.NNonterminal(308, Symbols.Nonterminal("NamedConstructParams"), Set(309)),
310 -> NGrammar.NNonterminal(310, Symbols.Nonterminal("NamedParam"), Set(311)),
312 -> NGrammar.NRepeat(312, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0), 7, 313),
314 -> NGrammar.NProxy(314, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 315),
317 -> NGrammar.NNonterminal(317, Symbols.Nonterminal("FuncCallOrConstructExpr"), Set(318)),
319 -> NGrammar.NNonterminal(319, Symbols.Nonterminal("TypeOrFuncName"), Set(32,77)),
320 -> NGrammar.NNonterminal(320, Symbols.Nonterminal("CallParams"), Set(321)),
322 -> NGrammar.NOneOf(322, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))), Set(80,323)),
323 -> NGrammar.NProxy(323, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))), 324),
325 -> NGrammar.NRepeat(325, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0), 7, 326),
327 -> NGrammar.NProxy(327, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 328),
330 -> NGrammar.NNonterminal(330, Symbols.Nonterminal("ArrayExpr"), Set(331)),
333 -> NGrammar.NNonterminal(333, Symbols.Nonterminal("Literal"), Set(74,334,336,339)),
335 -> NGrammar.NOneOf(335, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))))), Set(67,71)),
337 -> NGrammar.NNonterminal(337, Symbols.Nonterminal("CharChar"), Set(338)),
340 -> NGrammar.NRepeat(340, Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), 7, 341),
342 -> NGrammar.NNonterminal(342, Symbols.Nonterminal("StrChar"), Set(343)),
345 -> NGrammar.NNonterminal(345, Symbols.Nonterminal("EnumValue"), Set(346,351)),
347 -> NGrammar.NNonterminal(347, Symbols.Nonterminal("CanonicalEnumValue"), Set(348)),
349 -> NGrammar.NNonterminal(349, Symbols.Nonterminal("EnumValueName"), Set(350)),
352 -> NGrammar.NNonterminal(352, Symbols.Nonterminal("ShortenedEnumValue"), Set(353)),
356 -> NGrammar.NProxy(356, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':')))), 357),
359 -> NGrammar.NOneOf(359, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))))), Set(360,362)),
360 -> NGrammar.NProxy(360, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))), 361),
362 -> NGrammar.NProxy(362, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))), 363),
365 -> NGrammar.NProxy(365, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|')))), 366),
368 -> NGrammar.NProxy(368, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&')))), 369),
371 -> NGrammar.NRepeat(371, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0), 7, 372),
373 -> NGrammar.NProxy(373, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 374),
375 -> NGrammar.NRepeat(375, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0), 7, 376),
377 -> NGrammar.NProxy(377, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 378),
379 -> NGrammar.NRepeat(379, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 0), 7, 380),
381 -> NGrammar.NProxy(381, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 382),
383 -> NGrammar.NNonterminal(383, Symbols.Nonterminal("WSNL"), Set(384))),
  Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 0),Symbols.Nonterminal("WS"))), Seq(4,23,379,4)),
5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0))), Seq(6)),
7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq()), Seq()),
8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0),Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))))), Seq(6,9)),
12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.ExactChar('/'),Symbols.ExactChar('/'),Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0),Symbols.OneOf(ListSet(Symbols.Nonterminal("EOF"),Symbols.ExactChar('\n'))))), Seq(13,13,14,19)),
15 -> NGrammar.NSequence(15, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0),Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')))), Seq(14,16)),
21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.LookaheadExcept(Symbols.AnyChar))), Seq(22)),
24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Nonterminal("Rule"))), Seq(25)),
26 -> NGrammar.NSequence(26, Symbols.Sequence(Seq(Symbols.Nonterminal("LHS"),Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0))), Seq(27,4,162,4,163,375)),
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
164 -> NGrammar.NSequence(164, Symbols.Sequence(Seq(Symbols.Nonterminal("Sequence"))), Seq(165)),
166 -> NGrammar.NSequence(166, Symbols.Sequence(Seq(Symbols.Nonterminal("Elem"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0))), Seq(167,371)),
168 -> NGrammar.NSequence(168, Symbols.Sequence(Seq(Symbols.Nonterminal("Symbol"))), Seq(169)),
170 -> NGrammar.NSequence(170, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"))), Seq(171)),
172 -> NGrammar.NSequence(172, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('&'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(171,4,173,4,174)),
175 -> NGrammar.NSequence(175, Symbols.Sequence(Seq(Symbols.ExactChar('^'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(176,4,174)),
177 -> NGrammar.NSequence(177, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(178,4,174)),
179 -> NGrammar.NSequence(179, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"))), Seq(180)),
181 -> NGrammar.NSequence(181, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('?'))), Seq(180,4,161)),
182 -> NGrammar.NSequence(182, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('*'))), Seq(180,4,183)),
184 -> NGrammar.NSequence(184, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('+'))), Seq(180,4,185)),
186 -> NGrammar.NSequence(186, Symbols.Sequence(Seq(Symbols.Nonterminal("AtomSymbol"))), Seq(187)),
188 -> NGrammar.NSequence(188, Symbols.Sequence(Seq(Symbols.Nonterminal("Terminal"))), Seq(189)),
190 -> NGrammar.NSequence(190, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChar"),Symbols.ExactChar('\''))), Seq(191,192,191)),
193 -> NGrammar.NSequence(193, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')))), Seq(194)),
196 -> NGrammar.NSequence(196, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('\'','\\','b','n','r','t')))), Seq(195,197)),
198 -> NGrammar.NSequence(198, Symbols.Sequence(Seq(Symbols.Nonterminal("UnicodeChar"))), Seq(199)),
200 -> NGrammar.NSequence(200, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('u'),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet))), Seq(195,69,201,201,201,201)),
202 -> NGrammar.NSequence(202, Symbols.Sequence(Seq(Symbols.ExactChar('.'))), Seq(203)),
204 -> NGrammar.NSequence(204, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoice"))), Seq(205)),
206 -> NGrammar.NSequence(206, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1),Symbols.ExactChar('\''))), Seq(191,207,219,191)),
208 -> NGrammar.NSequence(208, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"))), Seq(209)),
210 -> NGrammar.NSequence(210, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))))), Seq(211)),
213 -> NGrammar.NSequence(213, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('\'','-','\\','b','n','r','t')))), Seq(195,214)),
215 -> NGrammar.NSequence(215, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceRange"))), Seq(216)),
217 -> NGrammar.NSequence(217, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"),Symbols.ExactChar('-'),Symbols.Nonterminal("TerminalChoiceChar"))), Seq(209,218,209)),
220 -> NGrammar.NSequence(220, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1),Symbols.Nonterminal("TerminalChoiceElem"))), Seq(219,207)),
221 -> NGrammar.NSequence(221, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceRange"),Symbols.ExactChar('\''))), Seq(191,216,191)),
222 -> NGrammar.NSequence(222, Symbols.Sequence(Seq(Symbols.Nonterminal("StringSymbol"))), Seq(223)),
224 -> NGrammar.NSequence(224, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.ExactChar('"'))), Seq(225,226,225)),
227 -> NGrammar.NSequence(227, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.Nonterminal("StringChar"))), Seq(226,228)),
229 -> NGrammar.NSequence(229, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))))), Seq(230)),
232 -> NGrammar.NSequence(232, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('"','\\','b','n','r','t')))), Seq(195,233)),
234 -> NGrammar.NSequence(234, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"))), Seq(29)),
235 -> NGrammar.NSequence(235, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceChoices"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(124,4,236,4,135)),
237 -> NGrammar.NSequence(237, Symbols.Sequence(Seq(Symbols.Nonterminal("Sequence"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence")))), 0))), Seq(165,238)),
239 -> NGrammar.NSequence(239, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence")))))), Seq(238,240)),
241 -> NGrammar.NSequence(241, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Sequence"))), Seq(4,242,4,165)),
243 -> NGrammar.NSequence(243, Symbols.Sequence(Seq(Symbols.Nonterminal("Longest"))), Seq(244)),
245 -> NGrammar.NSequence(245, Symbols.Sequence(Seq(Symbols.ExactChar('<'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceChoices"),Symbols.Nonterminal("WS"),Symbols.ExactChar('>'))), Seq(111,4,236,4,120)),
246 -> NGrammar.NSequence(246, Symbols.Sequence(Seq(Symbols.Nonterminal("EmptySequence"))), Seq(247)),
248 -> NGrammar.NSequence(248, Symbols.Sequence(Seq(Symbols.ExactChar('#'))), Seq(249)),
250 -> NGrammar.NSequence(250, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('-'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(171,4,218,4,174)),
251 -> NGrammar.NSequence(251, Symbols.Sequence(Seq(Symbols.Nonterminal("PreUnSymbol"))), Seq(174)),
252 -> NGrammar.NSequence(252, Symbols.Sequence(Seq(Symbols.Nonterminal("Processor"))), Seq(253)),
254 -> NGrammar.NSequence(254, Symbols.Sequence(Seq(Symbols.Nonterminal("Ref"))), Seq(255)),
256 -> NGrammar.NSequence(256, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"))), Seq(257)),
258 -> NGrammar.NSequence(258, Symbols.Sequence(Seq(Symbols.ExactChar('$'),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))),Symbols.Nonterminal("RefIdx"))), Seq(259,260,266)),
262 -> NGrammar.NSequence(262, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1))), Seq(263)),
265 -> NGrammar.NSequence(265, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1),Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))))), Seq(263,264)),
267 -> NGrammar.NSequence(267, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))))), Seq(268)),
272 -> NGrammar.NSequence(272, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(273,274)),
275 -> NGrammar.NSequence(275, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0),Symbols.Chars(('0' to '9').toSet))), Seq(274,276)),
277 -> NGrammar.NSequence(277, Symbols.Sequence(Seq(Symbols.Nonterminal("RawRef"))), Seq(278)),
279 -> NGrammar.NSequence(279, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$')))),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))),Symbols.Nonterminal("RefIdx"))), Seq(280,260,266)),
281 -> NGrammar.NSequence(281, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$'))), Seq(195,195,259)),
282 -> NGrammar.NSequence(282, Symbols.Sequence(Seq(Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(139,4,283,4,150)),
284 -> NGrammar.NSequence(284, Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))), Seq(285)),
286 -> NGrammar.NSequence(286, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"))), Seq(287)),
288 -> NGrammar.NSequence(288, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolOrExpr"))), Seq(289,4,368,4,287)),
290 -> NGrammar.NSequence(290, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolAndExpr"))), Seq(291,4,365,4,289)),
292 -> NGrammar.NSequence(292, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolEqExpr"))), Seq(293,4,359,4,291)),
294 -> NGrammar.NSequence(294, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ElvisExpr"))), Seq(295,4,356,4,293)),
296 -> NGrammar.NSequence(296, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar('+'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("AdditiveExpr"))), Seq(297,4,185,4,295)),
298 -> NGrammar.NSequence(298, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PrefixNotExpr"))), Seq(178,4,297)),
299 -> NGrammar.NSequence(299, Symbols.Sequence(Seq(Symbols.Nonterminal("Atom"))), Seq(300)),
301 -> NGrammar.NSequence(301, Symbols.Sequence(Seq(Symbols.Nonterminal("BindExpr"))), Seq(302)),
303 -> NGrammar.NSequence(303, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"),Symbols.Nonterminal("BinderExpr"))), Seq(257,304)),
305 -> NGrammar.NSequence(305, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedConstructExpr"))), Seq(306)),
307 -> NGrammar.NSequence(307, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedConstructParams"))), Seq(88,4,308)),
309 -> NGrammar.NSequence(309, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(124,4,310,312,4,135)),
311 -> NGrammar.NSequence(311, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))),Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"))), Seq(130,79,4,162,4,283)),
313 -> NGrammar.NSequence(313, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))))), Seq(312,314)),
315 -> NGrammar.NSequence(315, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam"))), Seq(4,119,4,310)),
316 -> NGrammar.NSequence(316, Symbols.Sequence(Seq(Symbols.Nonterminal("FuncCallOrConstructExpr"))), Seq(317)),
318 -> NGrammar.NSequence(318, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeOrFuncName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("CallParams"))), Seq(319,4,320)),
321 -> NGrammar.NSequence(321, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(')'))), Seq(124,4,322,135)),
324 -> NGrammar.NSequence(324, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS"))), Seq(283,325,4)),
326 -> NGrammar.NSequence(326, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))))), Seq(325,327)),
328 -> NGrammar.NSequence(328, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"))), Seq(4,119,4,283)),
329 -> NGrammar.NSequence(329, Symbols.Sequence(Seq(Symbols.Nonterminal("ArrayExpr"))), Seq(330)),
331 -> NGrammar.NSequence(331, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(']'))), Seq(90,4,322,91)),
332 -> NGrammar.NSequence(332, Symbols.Sequence(Seq(Symbols.Nonterminal("Literal"))), Seq(333)),
334 -> NGrammar.NSequence(334, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))))))), Seq(335)),
336 -> NGrammar.NSequence(336, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("CharChar"),Symbols.ExactChar('\''))), Seq(191,337,191)),
338 -> NGrammar.NSequence(338, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChar"))), Seq(192)),
339 -> NGrammar.NSequence(339, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0),Symbols.ExactChar('"'))), Seq(225,340,225)),
341 -> NGrammar.NSequence(341, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0),Symbols.Nonterminal("StrChar"))), Seq(340,342)),
343 -> NGrammar.NSequence(343, Symbols.Sequence(Seq(Symbols.Nonterminal("StringChar"))), Seq(228)),
344 -> NGrammar.NSequence(344, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumValue"))), Seq(345)),
346 -> NGrammar.NSequence(346, Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue"))), Seq(347)),
348 -> NGrammar.NSequence(348, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"),Symbols.ExactChar('.'),Symbols.Nonterminal("EnumValueName"))), Seq(101,203,349)),
350 -> NGrammar.NSequence(350, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(34)),
351 -> NGrammar.NSequence(351, Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))), Seq(352)),
353 -> NGrammar.NSequence(353, Symbols.Sequence(Seq(Symbols.ExactChar('%'),Symbols.Nonterminal("EnumValueName"))), Seq(103,349)),
354 -> NGrammar.NSequence(354, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(124,4,283,4,135)),
355 -> NGrammar.NSequence(355, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"))), Seq(297)),
357 -> NGrammar.NSequence(357, Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':'))), Seq(161,83)),
358 -> NGrammar.NSequence(358, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"))), Seq(295)),
361 -> NGrammar.NSequence(361, Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('='))), Seq(162,162)),
363 -> NGrammar.NSequence(363, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('='))), Seq(178,162)),
364 -> NGrammar.NSequence(364, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"))), Seq(293)),
366 -> NGrammar.NSequence(366, Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|'))), Seq(242,242)),
367 -> NGrammar.NSequence(367, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"))), Seq(291)),
369 -> NGrammar.NSequence(369, Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&'))), Seq(173,173)),
370 -> NGrammar.NSequence(370, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"))), Seq(289)),
372 -> NGrammar.NSequence(372, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))))), Seq(371,373)),
374 -> NGrammar.NSequence(374, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem"))), Seq(4,167)),
376 -> NGrammar.NSequence(376, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))))), Seq(375,377)),
378 -> NGrammar.NSequence(378, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS"))), Seq(4,242,4,163)),
380 -> NGrammar.NSequence(380, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def")))))), Seq(379,381)),
382 -> NGrammar.NSequence(382, Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"),Symbols.Nonterminal("Def"))), Seq(383,23)),
384 -> NGrammar.NSequence(384, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"))), Seq(4))),
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
assert(v7.id == 381)
val BindNode(v9, v10) = v8
assert(v9.id == 382)
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
assert(v37.id == 377)
val BindNode(v39, v40) = v38
assert(v39.id == 378)
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
def matchRHS(node: Node): Sequence = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 164 =>
val v65 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v66, v67) = v65
assert(v66.id == 165)
val v68 = matchSequence(v67)
v68
  }
}
def matchElem(node: Node): Elem = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 168 =>
val v69 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v70, v71) = v69
assert(v70.id == 169)
val v72 = matchSymbol(v71)
v72
case 252 =>
val v73 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v74, v75) = v73
assert(v74.id == 253)
val v76 = matchProcessor(v75)
v76
  }
}
def matchSymbol(node: Node): Symbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 170 =>
val v77 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v78, v79) = v77
assert(v78.id == 171)
val v80 = matchBinSymbol(v79)
v80
  }
}
def matchBinSymbol(node: Node): BinSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 172 =>
val v81 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v82, v83) = v81
assert(v82.id == 171)
val v84 = matchBinSymbol(v83)
val v85 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v86, v87) = v85
assert(v86.id == 174)
val v88 = matchPreUnSymbol(v87)
val v89 = JoinSymbol(node,v84,v88)
v89
case 250 =>
val v90 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v91, v92) = v90
assert(v91.id == 171)
val v93 = matchBinSymbol(v92)
val v94 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v95, v96) = v94
assert(v95.id == 174)
val v97 = matchPreUnSymbol(v96)
val v98 = ExceptSymbol(node,v93,v97)
v98
case 251 =>
val v99 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v100, v101) = v99
assert(v100.id == 174)
val v102 = matchPreUnSymbol(v101)
v102
  }
}
def matchPreUnSymbol(node: Node): PreUnSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 175 =>
val v103 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v104, v105) = v103
assert(v104.id == 174)
val v106 = matchPreUnSymbol(v105)
val v107 = FollowedBy(node,v106)
v107
case 177 =>
val v108 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v109, v110) = v108
assert(v109.id == 174)
val v111 = matchPreUnSymbol(v110)
val v112 = NotFollowedBy(node,v111)
v112
case 179 =>
val v113 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v114, v115) = v113
assert(v114.id == 180)
val v116 = matchPostUnSymbol(v115)
v116
  }
}
def matchPostUnSymbol(node: Node): PostUnSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 181 =>
val v117 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v118, v119) = v117
assert(v118.id == 180)
val v120 = matchPostUnSymbol(v119)
val v121 = Optional(node,v120)
v121
case 182 =>
val v122 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v123, v124) = v122
assert(v123.id == 180)
val v125 = matchPostUnSymbol(v124)
val v126 = RepeatFromZero(node,v125)
v126
case 184 =>
val v127 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v128, v129) = v127
assert(v128.id == 180)
val v130 = matchPostUnSymbol(v129)
val v131 = RepeatFromOne(node,v130)
v131
case 186 =>
val v132 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v133, v134) = v132
assert(v133.id == 187)
val v135 = matchAtomSymbol(v134)
v135
  }
}
def matchAtomSymbol(node: Node): AtomSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 188 =>
val v136 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v137, v138) = v136
assert(v137.id == 189)
val v139 = matchTerminal(v138)
v139
case 204 =>
val v140 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v141, v142) = v140
assert(v141.id == 205)
val v143 = matchTerminalChoice(v142)
v143
case 222 =>
val v144 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v145, v146) = v144
assert(v145.id == 223)
val v147 = matchStringSymbol(v146)
v147
case 234 =>
val v148 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v149, v150) = v148
assert(v149.id == 29)
val v151 = matchNonterminal(v150)
v151
case 235 =>
val v152 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v153, v154) = v152
assert(v153.id == 236)
val v155 = matchInPlaceChoices(v154)
v155
case 243 =>
val v156 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v157, v158) = v156
assert(v157.id == 244)
val v159 = matchLongest(v158)
v159
case 246 =>
val v160 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v161, v162) = v160
assert(v161.id == 247)
val v163 = matchEmptySequence(v162)
v163
  }
}
def matchTerminal(node: Node): Terminal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 190 =>
val v164 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v165, v166) = v164
assert(v165.id == 192)
val v167 = matchTerminalChar(v166)
v167
case 202 =>
val v168 = AnyTerminal(node)
v168
  }
}
def matchTerminalChoice(node: Node): TerminalChoice = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 206 =>
val v169 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v170, v171) = v169
assert(v170.id == 207)
val v172 = matchTerminalChoiceElem(v171)
val v173 = List(v172)
val v174 = body.asInstanceOf[SequenceNode].children(2)
val v178 = unrollRepeat1(v174) map { n =>
val BindNode(v175, v176) = n
assert(v175.id == 207)
val v177 = matchTerminalChoiceElem(v176)
v177
}
val v179 = v178 map { n =>
n
}
val v180 = v173 ++ v179
val v181 = TerminalChoice(node,v180)
v181
case 221 =>
val v182 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v183, v184) = v182
assert(v183.id == 216)
val v185 = matchTerminalChoiceRange(v184)
val v186 = List(v185)
val v187 = TerminalChoice(node,v186)
v187
  }
}
def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 208 =>
val v188 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v189, v190) = v188
assert(v189.id == 209)
val v191 = matchTerminalChoiceChar(v190)
v191
case 215 =>
val v192 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v193, v194) = v192
assert(v193.id == 216)
val v195 = matchTerminalChoiceRange(v194)
v195
  }
}
def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 217 =>
val v196 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v197, v198) = v196
assert(v197.id == 209)
val v199 = matchTerminalChoiceChar(v198)
val v200 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v201, v202) = v200
assert(v201.id == 209)
val v203 = matchTerminalChoiceChar(v202)
val v204 = TerminalChoiceRange(node,v199,v203)
v204
  }
}
def matchStringSymbol(node: Node): StringSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 224 =>
val v205 = body.asInstanceOf[SequenceNode].children(1)
val v209 = unrollRepeat0(v205) map { n =>
val BindNode(v206, v207) = n
assert(v206.id == 228)
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
    case 237 =>
val v217 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v218, v219) = v217
assert(v218.id == 165)
val v220 = matchSequence(v219)
val v221 = List(v220)
val v222 = body.asInstanceOf[SequenceNode].children(1)
val v227 = unrollRepeat0(v222) map { n =>
val BindNode(v223, v224) = n
assert(v223.id == 240)
val BindNode(v225, v226) = v224
assert(v225.id == 241)
v226
}
val v232 = v227 map { n =>
val v228 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v229, v230) = v228
assert(v229.id == 165)
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
    case 166 =>
val v235 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v236, v237) = v235
assert(v236.id == 167)
val v238 = matchElem(v237)
val v239 = List(v238)
val v240 = body.asInstanceOf[SequenceNode].children(1)
val v245 = unrollRepeat0(v240) map { n =>
val BindNode(v241, v242) = n
assert(v241.id == 373)
val BindNode(v243, v244) = v242
assert(v243.id == 374)
v244
}
val v250 = v245 map { n =>
val v246 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v247, v248) = v246
assert(v247.id == 167)
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
    case 245 =>
val v253 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v254, v255) = v253
assert(v254.id == 236)
val v256 = matchInPlaceChoices(v255)
val v257 = Longest(node,v256)
v257
  }
}
def matchEmptySequence(node: Node): EmptySeq = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 248 =>
val v258 = EmptySeq(node)
v258
  }
}
def matchTerminalChar(node: Node): TerminalChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 193 =>
val v259 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v260, v261) = v259
assert(v260.id == 194)
val v262 = CharAsIs(node,v261)
v262
case 196 =>
val v263 = body.asInstanceOf[SequenceNode].children(1)
val v264 = CharEscaped(node,v263)
v264
case 198 =>
val v265 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v266, v267) = v265
assert(v266.id == 199)
val v268 = matchUnicodeChar(v267)
v268
  }
}
def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 210 =>
val v269 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v270, v271) = v269
assert(v270.id == 211)
val v272 = CharAsIs(node,v271)
v272
case 213 =>
val v273 = body.asInstanceOf[SequenceNode].children(1)
val v274 = CharEscaped(node,v273)
v274
case 198 =>
val v275 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v276, v277) = v275
assert(v276.id == 199)
val v278 = matchUnicodeChar(v277)
v278
  }
}
def matchStringChar(node: Node): StringChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 229 =>
val v279 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v280, v281) = v279
assert(v280.id == 230)
val v282 = CharAsIs(node,v281)
v282
case 232 =>
val v283 = body.asInstanceOf[SequenceNode].children(1)
val v284 = CharEscaped(node,v283)
v284
case 198 =>
val v285 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v286, v287) = v285
assert(v286.id == 199)
val v288 = matchUnicodeChar(v287)
v288
  }
}
def matchUnicodeChar(node: Node): CharUnicode = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 200 =>
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
    case 254 =>
val v295 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v296, v297) = v295
assert(v296.id == 255)
val v298 = matchRef(v297)
v298
case 282 =>
val v299 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v300, v301) = v299
assert(v300.id == 283)
val v302 = matchPExpr(v301)
v302
  }
}
def matchRef(node: Node): Ref = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 256 =>
val v303 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v304, v305) = v303
assert(v304.id == 257)
val v306 = matchValRef(v305)
v306
case 277 =>
val v307 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v308, v309) = v307
assert(v308.id == 278)
val v310 = matchRawRef(v309)
v310
  }
}
def matchValRef(node: Node): ValRef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 258 =>
val v311 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v312, v313) = v311
assert(v312.id == 266)
val v314 = matchRefIdx(v313)
val v315 = body.asInstanceOf[SequenceNode].children(1)
val v319 = unrollOptional(v315, 80, 261) map { n =>
val BindNode(v316, v317) = n
assert(v316.id == 261)
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
    case 262 =>
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
    case 279 =>
val v323 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v324, v325) = v323
assert(v324.id == 266)
val v326 = matchRefIdx(v325)
val v327 = body.asInstanceOf[SequenceNode].children(1)
val v331 = unrollOptional(v327, 80, 261) map { n =>
val BindNode(v328, v329) = n
assert(v328.id == 261)
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
    case 284 =>
val v333 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v334, v335) = v333
assert(v334.id == 285)
val v336 = matchTernaryExpr(v335)
v336
  }
}
def matchTernaryExpr(node: Node): TerExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 286 =>
val v337 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v338, v339) = v337
assert(v338.id == 287)
val v340 = matchBoolOrExpr(v339)
v340
  }
}
def matchBoolOrExpr(node: Node): BoolOrExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 288 =>
val v341 = body.asInstanceOf[SequenceNode].children(2)
val v342 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v343, v344) = v342
assert(v343.id == 289)
val v345 = matchBoolAndExpr(v344)
val v346 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v347, v348) = v346
assert(v347.id == 287)
val v349 = matchBoolOrExpr(v348)
val v350 = BinOp(node,v341,v345,v349)
v350
case 370 =>
val v351 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v352, v353) = v351
assert(v352.id == 289)
val v354 = matchBoolAndExpr(v353)
v354
  }
}
def matchBoolAndExpr(node: Node): BoolAndExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 290 =>
val v355 = body.asInstanceOf[SequenceNode].children(2)
val v356 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v357, v358) = v356
assert(v357.id == 291)
val v359 = matchBoolEqExpr(v358)
val v360 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v361, v362) = v360
assert(v361.id == 289)
val v363 = matchBoolAndExpr(v362)
val v364 = BinOp(node,v355,v359,v363)
v364
case 367 =>
val v365 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v366, v367) = v365
assert(v366.id == 291)
val v368 = matchBoolEqExpr(v367)
v368
  }
}
def matchBoolEqExpr(node: Node): BoolEqExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 292 =>
// UnrollChoices
val v369 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v370, v371) = v369
assert(v370.id == 293)
val v372 = matchElvisExpr(v371)
val v373 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v374, v375) = v373
assert(v374.id == 291)
val v376 = matchBoolEqExpr(v375)
val v377 = BinOp(node,body,v372,v376)
v377
case 364 =>
val v378 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v379, v380) = v378
assert(v379.id == 293)
val v381 = matchElvisExpr(v380)
v381
  }
}
def matchElvisExpr(node: Node): ElvisExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 294 =>
val v382 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v383, v384) = v382
assert(v383.id == 295)
val v385 = matchAdditiveExpr(v384)
val v386 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v387, v388) = v386
assert(v387.id == 293)
val v389 = matchElvisExpr(v388)
val v390 = ElvisOp(node,v385,v389)
v390
case 358 =>
val v391 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v392, v393) = v391
assert(v392.id == 295)
val v394 = matchAdditiveExpr(v393)
v394
  }
}
def matchAdditiveExpr(node: Node): AdditiveExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 296 =>
val v395 = body.asInstanceOf[SequenceNode].children(2)
val v396 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v397, v398) = v396
assert(v397.id == 297)
val v399 = matchPrefixNotExpr(v398)
val v400 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v401, v402) = v400
assert(v401.id == 295)
val v403 = matchAdditiveExpr(v402)
val v404 = BinOp(node,v395,v399,v403)
v404
case 355 =>
val v405 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v406, v407) = v405
assert(v406.id == 297)
val v408 = matchPrefixNotExpr(v407)
v408
  }
}
def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 298 =>
val v409 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v410, v411) = v409
assert(v410.id == 297)
val v412 = matchPrefixNotExpr(v411)
val v413 = body.asInstanceOf[SequenceNode].children(0)
val v414 = PrefixOp(node,v412,v413)
v414
case 299 =>
val v415 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v416, v417) = v415
assert(v416.id == 300)
val v418 = matchAtom(v417)
v418
  }
}
def matchAtom(node: Node): Atom = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 254 =>
val v419 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v420, v421) = v419
assert(v420.id == 255)
val v422 = matchRef(v421)
v422
case 301 =>
val v423 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v424, v425) = v423
assert(v424.id == 302)
val v426 = matchBindExpr(v425)
v426
case 305 =>
val v427 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v428, v429) = v427
assert(v428.id == 306)
val v430 = matchNamedConstructExpr(v429)
v430
case 316 =>
val v431 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v432, v433) = v431
assert(v432.id == 317)
val v434 = matchFuncCallOrConstructExpr(v433)
v434
case 329 =>
val v435 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v436, v437) = v435
assert(v436.id == 330)
val v438 = matchArrayExpr(v437)
v438
case 332 =>
val v439 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v440, v441) = v439
assert(v440.id == 333)
val v442 = matchLiteral(v441)
v442
case 344 =>
val v443 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v444, v445) = v443
assert(v444.id == 345)
val v446 = matchEnumValue(v445)
v446
case 354 =>
val v447 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v448, v449) = v447
assert(v448.id == 283)
val v450 = matchPExpr(v449)
val v451 = ExprParen(node,v450)
v451
  }
}
def matchBindExpr(node: Node): BindExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 303 =>
val v452 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v453, v454) = v452
assert(v453.id == 257)
val v455 = matchValRef(v454)
val v456 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v457, v458) = v456
assert(v457.id == 304)
val v459 = matchBinderExpr(v458)
val v460 = BindExpr(node,v455,v459)
v460
  }
}
def matchBinderExpr(node: Node): BinderExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 254 =>
val v461 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v462, v463) = v461
assert(v462.id == 255)
val v464 = matchRef(v463)
v464
case 301 =>
val v465 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v466, v467) = v465
assert(v466.id == 302)
val v468 = matchBindExpr(v467)
v468
case 282 =>
val v469 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v470, v471) = v469
assert(v470.id == 283)
val v472 = matchPExpr(v471)
v472
  }
}
def matchNamedConstructExpr(node: Node): NamedConstructExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 307 =>
val v473 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v474, v475) = v473
assert(v474.id == 88)
val v476 = matchTypeName(v475)
val v477 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v478, v479) = v477
assert(v478.id == 308)
val v480 = matchNamedConstructParams(v479)
val v481 = NamedConstructExpr(node,v476,v480)
v481
  }
}
def matchNamedConstructParams(node: Node): List[NamedParam] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 309 =>
val v482 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v483, v484) = v482
assert(v483.id == 310)
val v485 = matchNamedParam(v484)
val v486 = List(v485)
val v487 = body.asInstanceOf[SequenceNode].children(3)
val v492 = unrollRepeat0(v487) map { n =>
val BindNode(v488, v489) = n
assert(v488.id == 314)
val BindNode(v490, v491) = v489
assert(v490.id == 315)
v491
}
val v497 = v492 map { n =>
val v493 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v494, v495) = v493
assert(v494.id == 310)
val v496 = matchNamedParam(v495)
v496
}
val v498 = v486 ++ v497
v498
  }
}
def matchNamedParam(node: Node): NamedParam = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 311 =>
val v499 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v500, v501) = v499
assert(v500.id == 130)
val v502 = matchParamName(v501)
val v503 = body.asInstanceOf[SequenceNode].children(1)
val v508 = unrollOptional(v503, 80, 81) map { n =>
val BindNode(v504, v505) = n
assert(v504.id == 81)
val BindNode(v506, v507) = v505
assert(v506.id == 82)
v507
}
val v509 = body.asInstanceOf[SequenceNode].children(5)
val BindNode(v510, v511) = v509
assert(v510.id == 283)
val v512 = matchPExpr(v511)
val v513 = NamedParam(node,v502,v508,v512)
v513
  }
}
def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 318 =>
val v514 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v515, v516) = v514
assert(v515.id == 319)
val v517 = matchTypeOrFuncName(v516)
val v518 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v519, v520) = v518
assert(v519.id == 320)
val v521 = matchCallParams(v520)
val v522 = FuncCallOrConstructExpr(node,v517,v521)
v522
  }
}
def matchCallParams(node: Node): Option[List[PExpr]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 321 =>
val v523 = body.asInstanceOf[SequenceNode].children(2)
val v528 = unrollOptional(v523, 80, 323) map { n =>
val BindNode(v524, v525) = n
assert(v524.id == 323)
val BindNode(v526, v527) = v525
assert(v526.id == 324)
v527
}
val v546 = v528 map { n =>
val v529 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v530, v531) = v529
assert(v530.id == 283)
val v532 = matchPExpr(v531)
val v533 = List(v532)
val v534 = n.asInstanceOf[SequenceNode].children(1)
val v539 = unrollRepeat0(v534) map { n =>
val BindNode(v535, v536) = n
assert(v535.id == 327)
val BindNode(v537, v538) = v536
assert(v537.id == 328)
v538
}
val v544 = v539 map { n =>
val v540 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v541, v542) = v540
assert(v541.id == 283)
val v543 = matchPExpr(v542)
v543
}
val v545 = v533 ++ v544
v545
}
v546
  }
}
def matchArrayExpr(node: Node): ArrayExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 331 =>
val v547 = body.asInstanceOf[SequenceNode].children(2)
val v552 = unrollOptional(v547, 80, 323) map { n =>
val BindNode(v548, v549) = n
assert(v548.id == 323)
val BindNode(v550, v551) = v549
assert(v550.id == 324)
v551
}
val v570 = v552 map { n =>
val v553 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v554, v555) = v553
assert(v554.id == 283)
val v556 = matchPExpr(v555)
val v557 = List(v556)
val v558 = n.asInstanceOf[SequenceNode].children(1)
val v563 = unrollRepeat0(v558) map { n =>
val BindNode(v559, v560) = n
assert(v559.id == 327)
val BindNode(v561, v562) = v560
assert(v561.id == 328)
v562
}
val v568 = v563 map { n =>
val v564 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v565, v566) = v564
assert(v565.id == 283)
val v567 = matchPExpr(v566)
v567
}
val v569 = v557 ++ v568
v569
}
val v571 = ArrayExpr(node,v570)
v571
  }
}
def matchLiteral(node: Node): Literal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 74 =>
val v572 = NullLiteral(node)
v572
case 334 =>
// UnrollChoices
val v573 = BoolLiteral(node,body)
v573
case 336 =>
val v574 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v575, v576) = v574
assert(v575.id == 337)
val v577 = matchCharChar(v576)
val v578 = CharLiteral(node,v577)
v578
case 339 =>
val v579 = body.asInstanceOf[SequenceNode].children(1)
val v583 = unrollRepeat0(v579) map { n =>
val BindNode(v580, v581) = n
assert(v580.id == 342)
val v582 = matchStrChar(v581)
v582
}
val v584 = StringLiteral(node,v583)
v584
  }
}
def matchEnumValue(node: Node): AbstractEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 346 =>
val v585 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v586, v587) = v585
assert(v586.id == 347)
val v588 = matchCanonicalEnumValue(v587)
v588
case 351 =>
val v589 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v590, v591) = v589
assert(v590.id == 352)
val v592 = matchShortenedEnumValue(v591)
v592
  }
}
def matchCanonicalEnumValue(node: Node): CanonicalEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 348 =>
val v593 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v594, v595) = v593
assert(v594.id == 101)
val v596 = matchEnumTypeName(v595)
val v597 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v598, v599) = v597
assert(v598.id == 349)
val v600 = matchEnumValueName(v599)
val v601 = CanonicalEnumValue(node,v596,v600)
v601
  }
}
def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 353 =>
val v602 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v603, v604) = v602
assert(v603.id == 349)
val v605 = matchEnumValueName(v604)
val v606 = ShortenedEnumValue(node,v605)
v606
  }
}
def matchTypeDef(node: Node): TypeDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 106 =>
val v607 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v608, v609) = v607
assert(v608.id == 107)
val v610 = matchClassDef(v609)
v610
case 136 =>
val v611 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v612, v613) = v611
assert(v612.id == 137)
val v614 = matchSuperDef(v613)
v614
case 151 =>
val v615 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v616, v617) = v615
assert(v616.id == 152)
val v618 = matchEnumTypeDef(v617)
v618
  }
}
def matchClassDef(node: Node): ClassDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 108 =>
val v619 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v620, v621) = v619
assert(v620.id == 88)
val v622 = matchTypeName(v621)
val v623 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v624, v625) = v623
assert(v624.id == 109)
val v626 = matchSuperTypes(v625)
val v627 = AbstractClassDef(node,v622,v626)
v627
case 121 =>
val v628 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v629, v630) = v628
assert(v629.id == 88)
val v631 = matchTypeName(v630)
val v632 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v633, v634) = v632
assert(v633.id == 109)
val v635 = matchSuperTypes(v634)
val v636 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v637, v638) = v636
assert(v637.id == 122)
val v639 = matchClassParamsDef(v638)
val v640 = ConcreteClassDef(node,v631,v635,v639)
v640
  }
}
def matchSuperTypes(node: Node): Option[List[TypeName]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 110 =>
val v641 = body.asInstanceOf[SequenceNode].children(2)
val v646 = unrollOptional(v641, 80, 113) map { n =>
val BindNode(v642, v643) = n
assert(v642.id == 113)
val BindNode(v644, v645) = v643
assert(v644.id == 114)
v645
}
val v664 = v646 map { n =>
val v647 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v648, v649) = v647
assert(v648.id == 88)
val v650 = matchTypeName(v649)
val v651 = List(v650)
val v652 = n.asInstanceOf[SequenceNode].children(1)
val v657 = unrollRepeat0(v652) map { n =>
val BindNode(v653, v654) = n
assert(v653.id == 117)
val BindNode(v655, v656) = v654
assert(v655.id == 118)
v656
}
val v662 = v657 map { n =>
val v658 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v659, v660) = v658
assert(v659.id == 88)
val v661 = matchTypeName(v660)
v661
}
val v663 = v651 ++ v662
v663
}
v664
  }
}
def matchClassParamsDef(node: Node): Option[List[ClassParamDef]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 123 =>
val v665 = body.asInstanceOf[SequenceNode].children(2)
val v670 = unrollOptional(v665, 80, 126) map { n =>
val BindNode(v666, v667) = n
assert(v666.id == 126)
val BindNode(v668, v669) = v667
assert(v668.id == 127)
v669
}
val v688 = v670 map { n =>
val v671 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v672, v673) = v671
assert(v672.id == 128)
val v674 = matchClassParamDef(v673)
val v675 = List(v674)
val v676 = n.asInstanceOf[SequenceNode].children(1)
val v681 = unrollRepeat0(v676) map { n =>
val BindNode(v677, v678) = n
assert(v677.id == 133)
val BindNode(v679, v680) = v678
assert(v679.id == 134)
v680
}
val v686 = v681 map { n =>
val v682 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v683, v684) = v682
assert(v683.id == 128)
val v685 = matchClassParamDef(v684)
v685
}
val v687 = v675 ++ v686
v687
}
v688
  }
}
def matchClassParamDef(node: Node): ClassParamDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 129 =>
val v689 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v690, v691) = v689
assert(v690.id == 130)
val v692 = matchParamName(v691)
val v693 = body.asInstanceOf[SequenceNode].children(1)
val v698 = unrollOptional(v693, 80, 81) map { n =>
val BindNode(v694, v695) = n
assert(v694.id == 81)
val BindNode(v696, v697) = v695
assert(v696.id == 82)
v697
}
val v703 = v698 map { n =>
val v699 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v700, v701) = v699
assert(v700.id == 84)
val v702 = matchTypeDesc(v701)
v702
}
val v704 = ClassParamDef(node,v692,v703)
v704
  }
}
def matchSuperDef(node: Node): SuperDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 138 =>
val v705 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v706, v707) = v705
assert(v706.id == 88)
val v708 = matchTypeName(v707)
val v709 = body.asInstanceOf[SequenceNode].children(3)
val v714 = unrollOptional(v709, 80, 141) map { n =>
val BindNode(v710, v711) = n
assert(v710.id == 141)
val BindNode(v712, v713) = v711
assert(v712.id == 142)
v713
}
val v719 = v714 map { n =>
val v715 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v716, v717) = v715
assert(v716.id == 143)
val v718 = matchSubTypes(v717)
v718
}
val v720 = SuperDef(node,v708,v719)
v720
  }
}
def matchSubTypes(node: Node): List[SubType] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 144 =>
val v721 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v722, v723) = v721
assert(v722.id == 145)
val v724 = matchSubType(v723)
val v725 = List(v724)
val v726 = body.asInstanceOf[SequenceNode].children(1)
val v731 = unrollRepeat0(v726) map { n =>
val BindNode(v727, v728) = n
assert(v727.id == 148)
val BindNode(v729, v730) = v728
assert(v729.id == 149)
v730
}
val v736 = v731 map { n =>
val v732 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v733, v734) = v732
assert(v733.id == 145)
val v735 = matchSubType(v734)
v735
}
val v737 = v725 ++ v736
v737
  }
}
def matchSubType(node: Node): SubType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 87 =>
val v738 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v739, v740) = v738
assert(v739.id == 88)
val v741 = matchTypeName(v740)
v741
case 106 =>
val v742 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v743, v744) = v742
assert(v743.id == 107)
val v745 = matchClassDef(v744)
v745
case 136 =>
val v746 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v747, v748) = v746
assert(v747.id == 137)
val v749 = matchSuperDef(v748)
v749
  }
}
def matchEnumTypeDef(node: Node): EnumTypeDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 153 =>
val v750 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v751, v752) = v750
assert(v751.id == 101)
val v753 = matchEnumTypeName(v752)
val v754 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v755, v756) = v754
assert(v755.id == 34)
val v757 = matchId(v756)
val v758 = List(v757)
val v759 = body.asInstanceOf[SequenceNode].children(5)
val v764 = unrollRepeat0(v759) map { n =>
val BindNode(v760, v761) = n
assert(v760.id == 156)
val BindNode(v762, v763) = v761
assert(v762.id == 157)
v763
}
val v769 = v764 map { n =>
val v765 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v766, v767) = v765
assert(v766.id == 34)
val v768 = matchId(v767)
v768
}
val v770 = v758 ++ v769
val v771 = EnumTypeDef(node,v753,v770)
v771
  }
}
def matchTypeDesc(node: Node): TypeDesc = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 85 =>
val v772 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v773, v774) = v772
assert(v773.id == 86)
val v775 = matchNonNullTypeDesc(v774)
val v776 = body.asInstanceOf[SequenceNode].children(1)
val v781 = unrollOptional(v776, 80, 159) map { n =>
val BindNode(v777, v778) = n
assert(v777.id == 159)
val BindNode(v779, v780) = v778
assert(v779.id == 160)
v780
}
val v782 = TypeDesc(node,v775,v781)
v782
  }
}
def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 87 =>
val v783 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v784, v785) = v783
assert(v784.id == 88)
val v786 = matchTypeName(v785)
v786
case 89 =>
val v787 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v788, v789) = v787
assert(v788.id == 84)
val v790 = matchTypeDesc(v789)
val v791 = ArrayTypeDesc(node,v790)
v791
case 92 =>
val v792 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v793, v794) = v792
assert(v793.id == 93)
val v795 = matchValueType(v794)
v795
case 94 =>
val v796 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v797, v798) = v796
assert(v797.id == 95)
val v799 = matchAnyType(v798)
v799
case 100 =>
val v800 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v801, v802) = v800
assert(v801.id == 101)
val v803 = matchEnumTypeName(v802)
v803
case 104 =>
val v804 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v805, v806) = v804
assert(v805.id == 105)
val v807 = matchTypeDef(v806)
v807
  }
}
def matchValueType(node: Node): ValueType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 44 =>
val v808 = BooleanType(node)
v808
case 53 =>
val v809 = CharType(node)
v809
case 59 =>
val v810 = StringType(node)
v810
  }
}
def matchAnyType(node: Node): AnyType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 96 =>
val v811 = AnyType(node)
v811
  }
}
def matchEnumTypeName(node: Node): EnumTypeName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 102 =>
val v812 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v813, v814) = v812
assert(v813.id == 34)
val v815 = matchId(v814)
val v816 = EnumTypeName(node,v815)
v816
  }
}
def matchTypeName(node: Node): TypeName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v817 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v818, v819) = v817
assert(v818.id == 33)
val v820 = TypeName(node,v819)
v820
case 77 =>
val v821 = body.asInstanceOf[SequenceNode].children(0)
val v822 = TypeName(node,v821)
v822
  }
}
def matchNonterminalName(node: Node): NonterminalName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v823 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v824, v825) = v823
assert(v824.id == 33)
val v826 = NonterminalName(node,v825)
v826
case 77 =>
val v827 = body.asInstanceOf[SequenceNode].children(0)
val v828 = NonterminalName(node,v827)
v828
  }
}
def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v829 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v830, v831) = v829
assert(v830.id == 33)
val v832 = TypeOrFuncName(node,v831)
v832
case 77 =>
val v833 = body.asInstanceOf[SequenceNode].children(0)
val v834 = TypeOrFuncName(node,v833)
v834
  }
}
def matchParamName(node: Node): ParamName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v835 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v836, v837) = v835
assert(v836.id == 33)
val v838 = ParamName(node,v837)
v838
case 77 =>
val v839 = body.asInstanceOf[SequenceNode].children(0)
val v840 = ParamName(node,v839)
v840
  }
}
def matchEnumValueName(node: Node): EnumValueName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 350 =>
val v841 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v842, v843) = v841
assert(v842.id == 34)
val v844 = matchId(v843)
val v845 = EnumValueName(node,v844)
v845
  }
}
def matchKeyword(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 44 =>
val v846 = body.asInstanceOf[SequenceNode].children(0)
v846
case 53 =>
val v847 = body.asInstanceOf[SequenceNode].children(0)
v847
case 59 =>
val v848 = body.asInstanceOf[SequenceNode].children(0)
v848
case 66 =>
val v849 = body.asInstanceOf[SequenceNode].children(0)
v849
case 70 =>
val v850 = body.asInstanceOf[SequenceNode].children(0)
v850
case 74 =>
val v851 = body.asInstanceOf[SequenceNode].children(0)
v851
  }
}
def matchStrChar(node: Node): StringChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 343 =>
val v852 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v853, v854) = v852
assert(v853.id == 228)
val v855 = matchStringChar(v854)
v855
  }
}
def matchCharChar(node: Node): TerminalChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 338 =>
val v856 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v857, v858) = v856
assert(v857.id == 192)
val v859 = matchTerminalChar(v858)
v859
  }
}
def matchRefIdx(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 267 =>
val v860 = body.asInstanceOf[SequenceNode].children(0)
v860
  }
}
def matchId(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 35 =>
val v861 = body.asInstanceOf[SequenceNode].children(0)
v861
  }
}
def matchWS(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 5 =>
val v862 = body.asInstanceOf[SequenceNode].children(0)
val v863 = unrollRepeat0(v862) map { n =>
// UnrollChoices
n
}
v863
  }
}
def matchWSNL(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 384 =>
val v864 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v865, v866) = v864
assert(v865.id == 4)
val v867 = matchWS(v866)
v867
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
val v868 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v869, v870) = v868
assert(v869.id == 22)
v870
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