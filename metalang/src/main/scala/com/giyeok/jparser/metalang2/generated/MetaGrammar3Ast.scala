package com.giyeok.jparser.metalang2.generated

import com.giyeok.jparser.Inputs.InputToShortString
import com.giyeok.jparser.ParseResultTree.{BindNode, JoinNode, Node, SequenceNode, TerminalNode}
import com.giyeok.jparser.examples.metalang3.MetaLang3Grammar
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor, Parser}
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
23 -> NGrammar.NNonterminal(23, Symbols.Nonterminal("Def"), Set(24,81)),
25 -> NGrammar.NNonterminal(25, Symbols.Nonterminal("Rule"), Set(26)),
27 -> NGrammar.NNonterminal(27, Symbols.Nonterminal("LHS"), Set(28)),
29 -> NGrammar.NNonterminal(29, Symbols.Nonterminal("Nonterminal"), Set(30)),
31 -> NGrammar.NNonterminal(31, Symbols.Nonterminal("Id"), Set(32)),
33 -> NGrammar.NLongest(33, Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))), 34),
34 -> NGrammar.NProxy(34, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))), 35),
36 -> NGrammar.NTerminal(36, Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
37 -> NGrammar.NRepeat(37, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), 7, 38),
39 -> NGrammar.NTerminal(39, Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
40 -> NGrammar.NOneOf(40, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))), Set(41,42)),
41 -> NGrammar.NProxy(41, Symbols.Proxy(Symbols.Sequence(Seq())), 7),
42 -> NGrammar.NProxy(42, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))), 43),
44 -> NGrammar.NTerminal(44, Symbols.ExactChar(':')),
45 -> NGrammar.NNonterminal(45, Symbols.Nonterminal("TypeDesc"), Set(46)),
47 -> NGrammar.NNonterminal(47, Symbols.Nonterminal("NonNullTypeDesc"), Set(48,50,53,77,81)),
49 -> NGrammar.NNonterminal(49, Symbols.Nonterminal("TypeName"), Set(30)),
51 -> NGrammar.NTerminal(51, Symbols.ExactChar('[')),
52 -> NGrammar.NTerminal(52, Symbols.ExactChar(']')),
54 -> NGrammar.NNonterminal(54, Symbols.Nonterminal("ValueType"), Set(55,64,70)),
56 -> NGrammar.NProxy(56, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'),Symbols.ExactChar('o'),Symbols.ExactChar('o'),Symbols.ExactChar('l'),Symbols.ExactChar('e'),Symbols.ExactChar('a'),Symbols.ExactChar('n')))), 57),
58 -> NGrammar.NTerminal(58, Symbols.ExactChar('b')),
59 -> NGrammar.NTerminal(59, Symbols.ExactChar('o')),
60 -> NGrammar.NTerminal(60, Symbols.ExactChar('l')),
61 -> NGrammar.NTerminal(61, Symbols.ExactChar('e')),
62 -> NGrammar.NTerminal(62, Symbols.ExactChar('a')),
63 -> NGrammar.NTerminal(63, Symbols.ExactChar('n')),
65 -> NGrammar.NProxy(65, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('c'),Symbols.ExactChar('h'),Symbols.ExactChar('a'),Symbols.ExactChar('r')))), 66),
67 -> NGrammar.NTerminal(67, Symbols.ExactChar('c')),
68 -> NGrammar.NTerminal(68, Symbols.ExactChar('h')),
69 -> NGrammar.NTerminal(69, Symbols.ExactChar('r')),
71 -> NGrammar.NProxy(71, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'),Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('i'),Symbols.ExactChar('n'),Symbols.ExactChar('g')))), 72),
73 -> NGrammar.NTerminal(73, Symbols.ExactChar('s')),
74 -> NGrammar.NTerminal(74, Symbols.ExactChar('t')),
75 -> NGrammar.NTerminal(75, Symbols.ExactChar('i')),
76 -> NGrammar.NTerminal(76, Symbols.ExactChar('g')),
78 -> NGrammar.NNonterminal(78, Symbols.Nonterminal("EnumTypeName"), Set(79)),
80 -> NGrammar.NTerminal(80, Symbols.ExactChar('%')),
82 -> NGrammar.NNonterminal(82, Symbols.Nonterminal("TypeDef"), Set(83,113,128)),
84 -> NGrammar.NNonterminal(84, Symbols.Nonterminal("ClassDef"), Set(85,98)),
86 -> NGrammar.NNonterminal(86, Symbols.Nonterminal("SuperTypes"), Set(87)),
88 -> NGrammar.NTerminal(88, Symbols.ExactChar('<')),
89 -> NGrammar.NOneOf(89, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Nonterminal("WS")))))), Set(41,90)),
90 -> NGrammar.NProxy(90, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Nonterminal("WS")))), 91),
92 -> NGrammar.NRepeat(92, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0), 7, 93),
94 -> NGrammar.NProxy(94, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 95),
96 -> NGrammar.NTerminal(96, Symbols.ExactChar(',')),
97 -> NGrammar.NTerminal(97, Symbols.ExactChar('>')),
99 -> NGrammar.NNonterminal(99, Symbols.Nonterminal("ClassParamsDef"), Set(100)),
101 -> NGrammar.NTerminal(101, Symbols.ExactChar('(')),
102 -> NGrammar.NOneOf(102, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0),Symbols.Nonterminal("WS")))))), Set(41,103)),
103 -> NGrammar.NProxy(103, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0),Symbols.Nonterminal("WS")))), 104),
105 -> NGrammar.NNonterminal(105, Symbols.Nonterminal("ClassParamDef"), Set(106)),
107 -> NGrammar.NNonterminal(107, Symbols.Nonterminal("ParamName"), Set(30)),
108 -> NGrammar.NRepeat(108, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0), 7, 109),
110 -> NGrammar.NProxy(110, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 111),
112 -> NGrammar.NTerminal(112, Symbols.ExactChar(')')),
114 -> NGrammar.NNonterminal(114, Symbols.Nonterminal("SuperDef"), Set(115)),
116 -> NGrammar.NTerminal(116, Symbols.ExactChar('{')),
117 -> NGrammar.NOneOf(117, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"),Symbols.Nonterminal("WS")))))), Set(41,118)),
118 -> NGrammar.NProxy(118, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"),Symbols.Nonterminal("WS")))), 119),
120 -> NGrammar.NNonterminal(120, Symbols.Nonterminal("SubTypes"), Set(121)),
122 -> NGrammar.NNonterminal(122, Symbols.Nonterminal("SubType"), Set(48,83,113)),
123 -> NGrammar.NRepeat(123, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0), 7, 124),
125 -> NGrammar.NProxy(125, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 126),
127 -> NGrammar.NTerminal(127, Symbols.ExactChar('}')),
129 -> NGrammar.NNonterminal(129, Symbols.Nonterminal("EnumTypeDef"), Set(130)),
131 -> NGrammar.NRepeat(131, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 0), 7, 132),
133 -> NGrammar.NProxy(133, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 134),
135 -> NGrammar.NOneOf(135, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))))), Set(41,136)),
136 -> NGrammar.NProxy(136, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))), 137),
138 -> NGrammar.NTerminal(138, Symbols.ExactChar('?')),
139 -> NGrammar.NTerminal(139, Symbols.ExactChar('=')),
140 -> NGrammar.NNonterminal(140, Symbols.Nonterminal("RHS"), Set(141)),
142 -> NGrammar.NNonterminal(142, Symbols.Nonterminal("Elem"), Set(143,229)),
144 -> NGrammar.NNonterminal(144, Symbols.Nonterminal("Symbol"), Set(145)),
146 -> NGrammar.NNonterminal(146, Symbols.Nonterminal("BinSymbol"), Set(147,227,228)),
148 -> NGrammar.NTerminal(148, Symbols.ExactChar('&')),
149 -> NGrammar.NNonterminal(149, Symbols.Nonterminal("PreUnSymbol"), Set(150,152,154)),
151 -> NGrammar.NTerminal(151, Symbols.ExactChar('^')),
153 -> NGrammar.NTerminal(153, Symbols.ExactChar('!')),
155 -> NGrammar.NNonterminal(155, Symbols.Nonterminal("PostUnSymbol"), Set(156,157,159,161)),
158 -> NGrammar.NTerminal(158, Symbols.ExactChar('*')),
160 -> NGrammar.NTerminal(160, Symbols.ExactChar('+')),
162 -> NGrammar.NNonterminal(162, Symbols.Nonterminal("AtomSymbol"), Set(163,180,198,210,211,220,223)),
164 -> NGrammar.NNonterminal(164, Symbols.Nonterminal("Terminal"), Set(165,178)),
166 -> NGrammar.NTerminal(166, Symbols.ExactChar('\'')),
167 -> NGrammar.NNonterminal(167, Symbols.Nonterminal("TerminalChar"), Set(168,171,173)),
169 -> NGrammar.NExcept(169, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')), 17, 170),
170 -> NGrammar.NTerminal(170, Symbols.ExactChar('\\')),
172 -> NGrammar.NTerminal(172, Symbols.Chars(Set('\'','\\','b','n','r','t'))),
174 -> NGrammar.NNonterminal(174, Symbols.Nonterminal("UnicodeChar"), Set(175)),
176 -> NGrammar.NTerminal(176, Symbols.ExactChar('u')),
177 -> NGrammar.NTerminal(177, Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet)),
179 -> NGrammar.NTerminal(179, Symbols.ExactChar('.')),
181 -> NGrammar.NNonterminal(181, Symbols.Nonterminal("TerminalChoice"), Set(182,197)),
183 -> NGrammar.NNonterminal(183, Symbols.Nonterminal("TerminalChoiceElem"), Set(184,191)),
185 -> NGrammar.NNonterminal(185, Symbols.Nonterminal("TerminalChoiceChar"), Set(186,189,173)),
187 -> NGrammar.NExcept(187, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))), 17, 188),
188 -> NGrammar.NTerminal(188, Symbols.Chars(Set('\'','-','\\'))),
190 -> NGrammar.NTerminal(190, Symbols.Chars(Set('\'','-','\\','b','n','r','t'))),
192 -> NGrammar.NNonterminal(192, Symbols.Nonterminal("TerminalChoiceRange"), Set(193)),
194 -> NGrammar.NTerminal(194, Symbols.ExactChar('-')),
195 -> NGrammar.NRepeat(195, Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), 183, 196),
199 -> NGrammar.NNonterminal(199, Symbols.Nonterminal("StringSymbol"), Set(200)),
201 -> NGrammar.NTerminal(201, Symbols.ExactChar('"')),
202 -> NGrammar.NRepeat(202, Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), 7, 203),
204 -> NGrammar.NNonterminal(204, Symbols.Nonterminal("StringChar"), Set(205,208,173)),
206 -> NGrammar.NExcept(206, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))), 17, 207),
207 -> NGrammar.NTerminal(207, Symbols.Chars(Set('"','\\'))),
209 -> NGrammar.NTerminal(209, Symbols.Chars(Set('"','\\','b','n','r','t'))),
212 -> NGrammar.NNonterminal(212, Symbols.Nonterminal("InPlaceChoices"), Set(213)),
214 -> NGrammar.NNonterminal(214, Symbols.Nonterminal("InPlaceSequence"), Set(141)),
215 -> NGrammar.NRepeat(215, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 0), 7, 216),
217 -> NGrammar.NProxy(217, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 218),
219 -> NGrammar.NTerminal(219, Symbols.ExactChar('|')),
221 -> NGrammar.NNonterminal(221, Symbols.Nonterminal("Longest"), Set(222)),
224 -> NGrammar.NNonterminal(224, Symbols.Nonterminal("EmptySequence"), Set(225)),
226 -> NGrammar.NTerminal(226, Symbols.ExactChar('#')),
230 -> NGrammar.NNonterminal(230, Symbols.Nonterminal("Processor"), Set(231,259)),
232 -> NGrammar.NNonterminal(232, Symbols.Nonterminal("Ref"), Set(233,254)),
234 -> NGrammar.NNonterminal(234, Symbols.Nonterminal("ValRef"), Set(235)),
236 -> NGrammar.NTerminal(236, Symbols.ExactChar('$')),
237 -> NGrammar.NOneOf(237, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))), Set(41,238)),
238 -> NGrammar.NNonterminal(238, Symbols.Nonterminal("CondSymPath"), Set(239)),
240 -> NGrammar.NRepeat(240, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1), 241, 242),
241 -> NGrammar.NOneOf(241, Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), Set(88,97)),
243 -> NGrammar.NNonterminal(243, Symbols.Nonterminal("RefIdx"), Set(244)),
245 -> NGrammar.NLongest(245, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))), 246),
246 -> NGrammar.NOneOf(246, Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))))), Set(247,248)),
247 -> NGrammar.NTerminal(247, Symbols.ExactChar('0')),
248 -> NGrammar.NProxy(248, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))), 249),
250 -> NGrammar.NTerminal(250, Symbols.Chars(('1' to '9').toSet)),
251 -> NGrammar.NRepeat(251, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 7, 252),
253 -> NGrammar.NTerminal(253, Symbols.Chars(('0' to '9').toSet)),
255 -> NGrammar.NNonterminal(255, Symbols.Nonterminal("RawRef"), Set(256)),
257 -> NGrammar.NProxy(257, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$')))), 258),
260 -> NGrammar.NNonterminal(260, Symbols.Nonterminal("PExpr"), Set(261)),
262 -> NGrammar.NNonterminal(262, Symbols.Nonterminal("TernateryExpr"), Set(263,354)),
264 -> NGrammar.NNonterminal(264, Symbols.Nonterminal("BoolOrExpr"), Set(265,352)),
266 -> NGrammar.NNonterminal(266, Symbols.Nonterminal("BoolAndExpr"), Set(267,349)),
268 -> NGrammar.NNonterminal(268, Symbols.Nonterminal("BoolEqExpr"), Set(269,346)),
270 -> NGrammar.NNonterminal(270, Symbols.Nonterminal("ElvisExpr"), Set(271,340)),
272 -> NGrammar.NNonterminal(272, Symbols.Nonterminal("AdditiveExpr"), Set(273,337)),
274 -> NGrammar.NNonterminal(274, Symbols.Nonterminal("PrefixNotExpr"), Set(275,276)),
277 -> NGrammar.NNonterminal(277, Symbols.Nonterminal("Atom"), Set(231,278,282,293,306,309,329,336)),
279 -> NGrammar.NNonterminal(279, Symbols.Nonterminal("BindExpr"), Set(280)),
281 -> NGrammar.NNonterminal(281, Symbols.Nonterminal("BinderExpr"), Set(231,278,259)),
283 -> NGrammar.NNonterminal(283, Symbols.Nonterminal("NamedConstructExpr"), Set(284)),
285 -> NGrammar.NNonterminal(285, Symbols.Nonterminal("NamedConstructParams"), Set(286)),
287 -> NGrammar.NNonterminal(287, Symbols.Nonterminal("NamedParam"), Set(288)),
289 -> NGrammar.NRepeat(289, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0), 7, 290),
291 -> NGrammar.NProxy(291, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 292),
294 -> NGrammar.NNonterminal(294, Symbols.Nonterminal("FuncCallOrConstructExpr"), Set(295)),
296 -> NGrammar.NNonterminal(296, Symbols.Nonterminal("TypeOrFuncName"), Set(30)),
297 -> NGrammar.NNonterminal(297, Symbols.Nonterminal("CallParams"), Set(298)),
299 -> NGrammar.NOneOf(299, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))), Set(41,300)),
300 -> NGrammar.NProxy(300, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))), 301),
302 -> NGrammar.NRepeat(302, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0), 7, 303),
304 -> NGrammar.NProxy(304, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 305),
307 -> NGrammar.NNonterminal(307, Symbols.Nonterminal("ArrayExpr"), Set(308)),
310 -> NGrammar.NNonterminal(310, Symbols.Nonterminal("Literal"), Set(311,314,321,324)),
312 -> NGrammar.NProxy(312, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'),Symbols.ExactChar('u'),Symbols.ExactChar('l'),Symbols.ExactChar('l')))), 313),
315 -> NGrammar.NOneOf(315, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))))), Set(316,318)),
316 -> NGrammar.NProxy(316, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))), 317),
318 -> NGrammar.NProxy(318, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))), 319),
320 -> NGrammar.NTerminal(320, Symbols.ExactChar('f')),
322 -> NGrammar.NNonterminal(322, Symbols.Nonterminal("CharChar"), Set(323)),
325 -> NGrammar.NRepeat(325, Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), 7, 326),
327 -> NGrammar.NNonterminal(327, Symbols.Nonterminal("StrChar"), Set(328)),
330 -> NGrammar.NNonterminal(330, Symbols.Nonterminal("EnumValue"), Set(331,334)),
332 -> NGrammar.NNonterminal(332, Symbols.Nonterminal("CanonicalEnumValue"), Set(333)),
335 -> NGrammar.NNonterminal(335, Symbols.Nonterminal("ShortenedEnumValue"), Set(79)),
338 -> NGrammar.NProxy(338, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':')))), 339),
341 -> NGrammar.NOneOf(341, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))))), Set(342,344)),
342 -> NGrammar.NProxy(342, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))), 343),
344 -> NGrammar.NProxy(344, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))), 345),
347 -> NGrammar.NProxy(347, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|')))), 348),
350 -> NGrammar.NProxy(350, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&')))), 351),
353 -> NGrammar.NLongest(353, Symbols.Longest(Symbols.Nonterminal("TernateryExpr")), 262),
355 -> NGrammar.NRepeat(355, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0), 7, 356),
357 -> NGrammar.NProxy(357, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 358),
359 -> NGrammar.NRepeat(359, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0), 7, 360),
361 -> NGrammar.NProxy(361, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 362),
363 -> NGrammar.NRepeat(363, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))), 0), 7, 364),
365 -> NGrammar.NProxy(365, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))), 366)),
  Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))), 0),Symbols.Nonterminal("WS"))), Seq(4,23,363,4)),
5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0))), Seq(6)),
7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq()), Seq()),
8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0),Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))))), Seq(6,9)),
12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.ExactChar('/'),Symbols.ExactChar('/'),Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0),Symbols.OneOf(ListSet(Symbols.Nonterminal("EOF"),Symbols.ExactChar('\n'))))), Seq(13,13,14,19)),
15 -> NGrammar.NSequence(15, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0),Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')))), Seq(14,16)),
21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.LookaheadExcept(Symbols.AnyChar))), Seq(22)),
24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Nonterminal("Rule"))), Seq(25)),
26 -> NGrammar.NSequence(26, Symbols.Sequence(Seq(Symbols.Nonterminal("LHS"),Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0))), Seq(27,4,139,4,140,359)),
28 -> NGrammar.NSequence(28, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))))), Seq(29,40)),
30 -> NGrammar.NSequence(30, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(31)),
32 -> NGrammar.NSequence(32, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))), Seq(33)),
35 -> NGrammar.NSequence(35, Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))), Seq(36,37)),
38 -> NGrammar.NSequence(38, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0),Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(37,39)),
43 -> NGrammar.NSequence(43, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc"))), Seq(4,44,4,45)),
46 -> NGrammar.NSequence(46, Symbols.Sequence(Seq(Symbols.Nonterminal("NonNullTypeDesc"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))))))), Seq(47,135)),
48 -> NGrammar.NSequence(48, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"))), Seq(49)),
50 -> NGrammar.NSequence(50, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc"),Symbols.Nonterminal("WS"),Symbols.ExactChar(']'))), Seq(51,4,45,4,52)),
53 -> NGrammar.NSequence(53, Symbols.Sequence(Seq(Symbols.Nonterminal("ValueType"))), Seq(54)),
55 -> NGrammar.NSequence(55, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'),Symbols.ExactChar('o'),Symbols.ExactChar('o'),Symbols.ExactChar('l'),Symbols.ExactChar('e'),Symbols.ExactChar('a'),Symbols.ExactChar('n')))))), Seq(56)),
57 -> NGrammar.NSequence(57, Symbols.Sequence(Seq(Symbols.ExactChar('b'),Symbols.ExactChar('o'),Symbols.ExactChar('o'),Symbols.ExactChar('l'),Symbols.ExactChar('e'),Symbols.ExactChar('a'),Symbols.ExactChar('n'))), Seq(58,59,59,60,61,62,63)),
64 -> NGrammar.NSequence(64, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('c'),Symbols.ExactChar('h'),Symbols.ExactChar('a'),Symbols.ExactChar('r')))))), Seq(65)),
66 -> NGrammar.NSequence(66, Symbols.Sequence(Seq(Symbols.ExactChar('c'),Symbols.ExactChar('h'),Symbols.ExactChar('a'),Symbols.ExactChar('r'))), Seq(67,68,62,69)),
70 -> NGrammar.NSequence(70, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'),Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('i'),Symbols.ExactChar('n'),Symbols.ExactChar('g')))))), Seq(71)),
72 -> NGrammar.NSequence(72, Symbols.Sequence(Seq(Symbols.ExactChar('s'),Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('i'),Symbols.ExactChar('n'),Symbols.ExactChar('g'))), Seq(73,74,69,75,63,76)),
77 -> NGrammar.NSequence(77, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"))), Seq(78)),
79 -> NGrammar.NSequence(79, Symbols.Sequence(Seq(Symbols.ExactChar('%'),Symbols.Nonterminal("Id"))), Seq(80,31)),
81 -> NGrammar.NSequence(81, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeDef"))), Seq(82)),
83 -> NGrammar.NSequence(83, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassDef"))), Seq(84)),
85 -> NGrammar.NSequence(85, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SuperTypes"))), Seq(49,4,86)),
87 -> NGrammar.NSequence(87, Symbols.Sequence(Seq(Symbols.ExactChar('<'),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar('>'))), Seq(88,4,89,97)),
91 -> NGrammar.NSequence(91, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Nonterminal("WS"))), Seq(49,92,4)),
93 -> NGrammar.NSequence(93, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))))), Seq(92,94)),
95 -> NGrammar.NSequence(95, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName"))), Seq(4,96,4,49)),
98 -> NGrammar.NSequence(98, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SuperTypes"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamsDef"))), Seq(49,4,86,4,99)),
100 -> NGrammar.NSequence(100, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0),Symbols.Nonterminal("WS")))))),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(101,4,102,4,112)),
104 -> NGrammar.NSequence(104, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0),Symbols.Nonterminal("WS"))), Seq(105,108,4)),
106 -> NGrammar.NSequence(106, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))))), Seq(107,40)),
109 -> NGrammar.NSequence(109, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef")))))), Seq(108,110)),
111 -> NGrammar.NSequence(111, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParamDef"))), Seq(4,96,4,105)),
113 -> NGrammar.NSequence(113, Symbols.Sequence(Seq(Symbols.Nonterminal("SuperDef"))), Seq(114)),
115 -> NGrammar.NSequence(115, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"),Symbols.Nonterminal("WS")))))),Symbols.ExactChar('}'))), Seq(49,4,116,4,117,127)),
119 -> NGrammar.NSequence(119, Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"),Symbols.Nonterminal("WS"))), Seq(120,4)),
121 -> NGrammar.NSequence(121, Symbols.Sequence(Seq(Symbols.Nonterminal("SubType"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0))), Seq(122,123)),
124 -> NGrammar.NSequence(124, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))))), Seq(123,125)),
126 -> NGrammar.NSequence(126, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType"))), Seq(4,96,4,122)),
128 -> NGrammar.NSequence(128, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeDef"))), Seq(129)),
130 -> NGrammar.NSequence(130, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"),Symbols.Nonterminal("WS"),Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 0),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(78,4,116,4,31,131,4,127)),
132 -> NGrammar.NSequence(132, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id")))))), Seq(131,133)),
134 -> NGrammar.NSequence(134, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id"))), Seq(4,96,4,31)),
137 -> NGrammar.NSequence(137, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?'))), Seq(4,138)),
141 -> NGrammar.NSequence(141, Symbols.Sequence(Seq(Symbols.Nonterminal("Elem"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0))), Seq(142,355)),
143 -> NGrammar.NSequence(143, Symbols.Sequence(Seq(Symbols.Nonterminal("Symbol"))), Seq(144)),
145 -> NGrammar.NSequence(145, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"))), Seq(146)),
147 -> NGrammar.NSequence(147, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('&'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(146,4,148,4,149)),
150 -> NGrammar.NSequence(150, Symbols.Sequence(Seq(Symbols.ExactChar('^'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(151,4,149)),
152 -> NGrammar.NSequence(152, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(153,4,149)),
154 -> NGrammar.NSequence(154, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"))), Seq(155)),
156 -> NGrammar.NSequence(156, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('?'))), Seq(155,4,138)),
157 -> NGrammar.NSequence(157, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('*'))), Seq(155,4,158)),
159 -> NGrammar.NSequence(159, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('+'))), Seq(155,4,160)),
161 -> NGrammar.NSequence(161, Symbols.Sequence(Seq(Symbols.Nonterminal("AtomSymbol"))), Seq(162)),
163 -> NGrammar.NSequence(163, Symbols.Sequence(Seq(Symbols.Nonterminal("Terminal"))), Seq(164)),
165 -> NGrammar.NSequence(165, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChar"),Symbols.ExactChar('\''))), Seq(166,167,166)),
168 -> NGrammar.NSequence(168, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')))), Seq(169)),
171 -> NGrammar.NSequence(171, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('\'','\\','b','n','r','t')))), Seq(170,172)),
173 -> NGrammar.NSequence(173, Symbols.Sequence(Seq(Symbols.Nonterminal("UnicodeChar"))), Seq(174)),
175 -> NGrammar.NSequence(175, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('u'),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet))), Seq(170,176,177,177,177,177)),
178 -> NGrammar.NSequence(178, Symbols.Sequence(Seq(Symbols.ExactChar('.'))), Seq(179)),
180 -> NGrammar.NSequence(180, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoice"))), Seq(181)),
182 -> NGrammar.NSequence(182, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1),Symbols.ExactChar('\''))), Seq(166,183,195,166)),
184 -> NGrammar.NSequence(184, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"))), Seq(185)),
186 -> NGrammar.NSequence(186, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))))), Seq(187)),
189 -> NGrammar.NSequence(189, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('\'','-','\\','b','n','r','t')))), Seq(170,190)),
191 -> NGrammar.NSequence(191, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceRange"))), Seq(192)),
193 -> NGrammar.NSequence(193, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"),Symbols.ExactChar('-'),Symbols.Nonterminal("TerminalChoiceChar"))), Seq(185,194,185)),
196 -> NGrammar.NSequence(196, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1),Symbols.Nonterminal("TerminalChoiceElem"))), Seq(195,183)),
197 -> NGrammar.NSequence(197, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceRange"),Symbols.ExactChar('\''))), Seq(166,192,166)),
198 -> NGrammar.NSequence(198, Symbols.Sequence(Seq(Symbols.Nonterminal("StringSymbol"))), Seq(199)),
200 -> NGrammar.NSequence(200, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.ExactChar('"'))), Seq(201,202,201)),
203 -> NGrammar.NSequence(203, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.Nonterminal("StringChar"))), Seq(202,204)),
205 -> NGrammar.NSequence(205, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))))), Seq(206)),
208 -> NGrammar.NSequence(208, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('"','\\','b','n','r','t')))), Seq(170,209)),
210 -> NGrammar.NSequence(210, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"))), Seq(29)),
211 -> NGrammar.NSequence(211, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceChoices"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(101,4,212,4,112)),
213 -> NGrammar.NSequence(213, Symbols.Sequence(Seq(Symbols.Nonterminal("InPlaceSequence"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 0))), Seq(214,215)),
216 -> NGrammar.NSequence(216, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))))), Seq(215,217)),
218 -> NGrammar.NSequence(218, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence"))), Seq(4,219,4,214)),
220 -> NGrammar.NSequence(220, Symbols.Sequence(Seq(Symbols.Nonterminal("Longest"))), Seq(221)),
222 -> NGrammar.NSequence(222, Symbols.Sequence(Seq(Symbols.ExactChar('<'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceChoices"),Symbols.Nonterminal("WS"),Symbols.ExactChar('>'))), Seq(88,4,212,4,97)),
223 -> NGrammar.NSequence(223, Symbols.Sequence(Seq(Symbols.Nonterminal("EmptySequence"))), Seq(224)),
225 -> NGrammar.NSequence(225, Symbols.Sequence(Seq(Symbols.ExactChar('#'))), Seq(226)),
227 -> NGrammar.NSequence(227, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('-'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(146,4,194,4,149)),
228 -> NGrammar.NSequence(228, Symbols.Sequence(Seq(Symbols.Nonterminal("PreUnSymbol"))), Seq(149)),
229 -> NGrammar.NSequence(229, Symbols.Sequence(Seq(Symbols.Nonterminal("Processor"))), Seq(230)),
231 -> NGrammar.NSequence(231, Symbols.Sequence(Seq(Symbols.Nonterminal("Ref"))), Seq(232)),
233 -> NGrammar.NSequence(233, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"))), Seq(234)),
235 -> NGrammar.NSequence(235, Symbols.Sequence(Seq(Symbols.ExactChar('$'),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))),Symbols.Nonterminal("RefIdx"))), Seq(236,237,243)),
239 -> NGrammar.NSequence(239, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1))), Seq(240)),
242 -> NGrammar.NSequence(242, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))), 1),Symbols.OneOf(ListSet(Symbols.ExactChar('<'),Symbols.ExactChar('>'))))), Seq(240,241)),
244 -> NGrammar.NSequence(244, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))))), Seq(245)),
249 -> NGrammar.NSequence(249, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(250,251)),
252 -> NGrammar.NSequence(252, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0),Symbols.Chars(('0' to '9').toSet))), Seq(251,253)),
254 -> NGrammar.NSequence(254, Symbols.Sequence(Seq(Symbols.Nonterminal("RawRef"))), Seq(255)),
256 -> NGrammar.NSequence(256, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$')))),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Nonterminal("CondSymPath"))),Symbols.Nonterminal("RefIdx"))), Seq(257,237,243)),
258 -> NGrammar.NSequence(258, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('\\'),Symbols.ExactChar('$'))), Seq(170,170,236)),
259 -> NGrammar.NSequence(259, Symbols.Sequence(Seq(Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(116,4,260,4,127)),
261 -> NGrammar.NSequence(261, Symbols.Sequence(Seq(Symbols.Nonterminal("TernateryExpr"))), Seq(262)),
263 -> NGrammar.NSequence(263, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar('?'),Symbols.Nonterminal("WS"),Symbols.Longest(Symbols.Nonterminal("TernateryExpr")),Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Longest(Symbols.Nonterminal("TernateryExpr")))), Seq(264,4,138,4,353,4,44,4,353)),
265 -> NGrammar.NSequence(265, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolOrExpr"))), Seq(266,4,350,4,264)),
267 -> NGrammar.NSequence(267, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolAndExpr"))), Seq(268,4,347,4,266)),
269 -> NGrammar.NSequence(269, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('=')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('=')))))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BoolEqExpr"))), Seq(270,4,341,4,268)),
271 -> NGrammar.NSequence(271, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"),Symbols.Nonterminal("WS"),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':')))),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ElvisExpr"))), Seq(272,4,338,4,270)),
273 -> NGrammar.NSequence(273, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar('+'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("AdditiveExpr"))), Seq(274,4,160,4,272)),
275 -> NGrammar.NSequence(275, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PrefixNotExpr"))), Seq(153,4,274)),
276 -> NGrammar.NSequence(276, Symbols.Sequence(Seq(Symbols.Nonterminal("Atom"))), Seq(277)),
278 -> NGrammar.NSequence(278, Symbols.Sequence(Seq(Symbols.Nonterminal("BindExpr"))), Seq(279)),
280 -> NGrammar.NSequence(280, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"),Symbols.Nonterminal("BinderExpr"))), Seq(234,281)),
282 -> NGrammar.NSequence(282, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedConstructExpr"))), Seq(283)),
284 -> NGrammar.NSequence(284, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedConstructParams"))), Seq(49,4,285)),
286 -> NGrammar.NSequence(286, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(101,4,287,289,4,112)),
288 -> NGrammar.NSequence(288, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))),Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"))), Seq(107,40,4,139,4,260)),
290 -> NGrammar.NSequence(290, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))))), Seq(289,291)),
292 -> NGrammar.NSequence(292, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam"))), Seq(4,96,4,287)),
293 -> NGrammar.NSequence(293, Symbols.Sequence(Seq(Symbols.Nonterminal("FuncCallOrConstructExpr"))), Seq(294)),
295 -> NGrammar.NSequence(295, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeOrFuncName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("CallParams"))), Seq(296,4,297)),
298 -> NGrammar.NSequence(298, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(')'))), Seq(101,4,299,112)),
301 -> NGrammar.NSequence(301, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS"))), Seq(260,302,4)),
303 -> NGrammar.NSequence(303, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))))), Seq(302,304)),
305 -> NGrammar.NSequence(305, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"))), Seq(4,96,4,260)),
306 -> NGrammar.NSequence(306, Symbols.Sequence(Seq(Symbols.Nonterminal("ArrayExpr"))), Seq(307)),
308 -> NGrammar.NSequence(308, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(']'))), Seq(51,4,299,52)),
309 -> NGrammar.NSequence(309, Symbols.Sequence(Seq(Symbols.Nonterminal("Literal"))), Seq(310)),
311 -> NGrammar.NSequence(311, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'),Symbols.ExactChar('u'),Symbols.ExactChar('l'),Symbols.ExactChar('l')))))), Seq(312)),
313 -> NGrammar.NSequence(313, Symbols.Sequence(Seq(Symbols.ExactChar('n'),Symbols.ExactChar('u'),Symbols.ExactChar('l'),Symbols.ExactChar('l'))), Seq(63,176,60,60)),
314 -> NGrammar.NSequence(314, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))))))), Seq(315)),
317 -> NGrammar.NSequence(317, Symbols.Sequence(Seq(Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('u'),Symbols.ExactChar('e'))), Seq(74,69,176,61)),
319 -> NGrammar.NSequence(319, Symbols.Sequence(Seq(Symbols.ExactChar('f'),Symbols.ExactChar('a'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e'))), Seq(320,62,60,73,61)),
321 -> NGrammar.NSequence(321, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("CharChar"),Symbols.ExactChar('\''))), Seq(166,322,166)),
323 -> NGrammar.NSequence(323, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChar"))), Seq(167)),
324 -> NGrammar.NSequence(324, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0),Symbols.ExactChar('"'))), Seq(201,325,201)),
326 -> NGrammar.NSequence(326, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0),Symbols.Nonterminal("StrChar"))), Seq(325,327)),
328 -> NGrammar.NSequence(328, Symbols.Sequence(Seq(Symbols.Nonterminal("StringChar"))), Seq(204)),
329 -> NGrammar.NSequence(329, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumValue"))), Seq(330)),
331 -> NGrammar.NSequence(331, Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue"))), Seq(332)),
333 -> NGrammar.NSequence(333, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"),Symbols.ExactChar('.'),Symbols.Nonterminal("Id"))), Seq(78,179,31)),
334 -> NGrammar.NSequence(334, Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))), Seq(335)),
336 -> NGrammar.NSequence(336, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(101,4,260,4,112)),
337 -> NGrammar.NSequence(337, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"))), Seq(274)),
339 -> NGrammar.NSequence(339, Symbols.Sequence(Seq(Symbols.ExactChar('?'),Symbols.ExactChar(':'))), Seq(138,44)),
340 -> NGrammar.NSequence(340, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"))), Seq(272)),
343 -> NGrammar.NSequence(343, Symbols.Sequence(Seq(Symbols.ExactChar('='),Symbols.ExactChar('='))), Seq(139,139)),
345 -> NGrammar.NSequence(345, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.ExactChar('='))), Seq(153,139)),
346 -> NGrammar.NSequence(346, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"))), Seq(270)),
348 -> NGrammar.NSequence(348, Symbols.Sequence(Seq(Symbols.ExactChar('|'),Symbols.ExactChar('|'))), Seq(219,219)),
349 -> NGrammar.NSequence(349, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"))), Seq(268)),
351 -> NGrammar.NSequence(351, Symbols.Sequence(Seq(Symbols.ExactChar('&'),Symbols.ExactChar('&'))), Seq(148,148)),
352 -> NGrammar.NSequence(352, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"))), Seq(266)),
354 -> NGrammar.NSequence(354, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"))), Seq(264)),
356 -> NGrammar.NSequence(356, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))))), Seq(355,357)),
358 -> NGrammar.NSequence(358, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem"))), Seq(4,142)),
360 -> NGrammar.NSequence(360, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))))), Seq(359,361)),
362 -> NGrammar.NSequence(362, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS"))), Seq(4,219,4,140)),
364 -> NGrammar.NSequence(364, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))))), Seq(363,365)),
366 -> NGrammar.NSequence(366, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def"))), Seq(4,23))),
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
case class Paren(astNode:Node, choices:InPlaceChoices) extends ASTNode with AtomSymbol{
  def prettyPrint(): String = "Paren(" + "choices=" + choices.prettyPrint() +  ")"
}
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
case class Nonterminal(astNode:Node, name:Node) extends ASTNode with AtomSymbol{
  def prettyPrint(): String = "Nonterminal(" + "name=" + name.sourceText +  ")"
}
case class InPlaceChoices(astNode:Node, choices:List[InPlaceSequence]) extends ASTNode{
  def prettyPrint(): String = "InPlaceChoices(" + "choices=" + "[" + choices.map(e => e.prettyPrint()).mkString(",") + "]" +  ")"
}
case class InPlaceSequence(astNode:Node, seq:List[Elem]) extends ASTNode{
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
sealed trait PExpr extends ASTNode with Processor
sealed trait TerExpr extends ASTNode with PExpr
case class TernateryOp(astNode:Node, cond:BoolOrExpr, then:Node, otherwise:Node) extends ASTNode with TerExpr{
  def prettyPrint(): String = "TernateryOp(" + "cond=" + cond.prettyPrint()+ ", " + "then=" + then.sourceText+ ", " + "otherwise=" + otherwise.sourceText +  ")"
}
sealed trait BoolOrExpr extends ASTNode with TerExpr
case class BinOp(astNode:Node, lhs:BoolAndExpr, rhs:BoolOrExpr, op:Node) extends ASTNode with AdditiveExpr{
  def prettyPrint(): String = "BinOp(" + "lhs=" + lhs.prettyPrint()+ ", " + "rhs=" + rhs.prettyPrint()+ ", " + "op=" + op.sourceText +  ")"
}
sealed trait BoolAndExpr extends ASTNode with BoolOrExpr
sealed trait BoolEqExpr extends ASTNode with BoolAndExpr
sealed trait ElvisExpr extends ASTNode with BoolEqExpr
case class ElvisOp(astNode:Node, value:AdditiveExpr, whenNull:ElvisExpr) extends ASTNode with ElvisExpr{
  def prettyPrint(): String = "ElvisOp(" + "value=" + value.prettyPrint()+ ", " + "whenNull=" + whenNull.prettyPrint() +  ")"
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
case class BindExpr(astNode:Node, ctx:ValRef, binder:PExpr) extends ASTNode with Atom{
  def prettyPrint(): String = "BindExpr(" + "ctx=" + ctx.prettyPrint()+ ", " + "binder=" + binder.prettyPrint() +  ")"
}
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
case class EnumTypeName(astNode:Node, name:Node) extends ASTNode with NonNullTypeDesc{
  def prettyPrint(): String = "EnumTypeName(" + "name=" + name.sourceText +  ")"
}
case class TypeName(astNode:Node, name:Node) extends ASTNode with NonNullTypeDesc with SubType{
  def prettyPrint(): String = "TypeName(" + "name=" + name.sourceText +  ")"
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
assert(v7.id == 365)
val BindNode(v9, v10) = v8
assert(v9.id == 366)
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
case 81 =>
val v23 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v24, v25) = v23
assert(v24.id == 82)
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
assert(v32.id == 140)
val v34 = matchRHS(v33)
val v35 = List(v34)
val v36 = body.asInstanceOf[SequenceNode].children(5)
val v41 = unrollRepeat0(v36) map { n =>
val BindNode(v37, v38) = n
assert(v37.id == 361)
val BindNode(v39, v40) = v38
assert(v39.id == 362)
v40
}
val v46 = v41 map { n =>
val v42 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v43, v44) = v42
assert(v43.id == 140)
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
val v58 = unrollOptional(v53, 41, 42) map { n =>
val BindNode(v54, v55) = n
assert(v54.id == 42)
val BindNode(v56, v57) = v55
assert(v56.id == 43)
v57
}
val v63 = v58 map { n =>
val v59 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v60, v61) = v59
assert(v60.id == 45)
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
    case 141 =>
val v65 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v66, v67) = v65
assert(v66.id == 142)
val v68 = matchElem(v67)
val v69 = List(v68)
val v70 = body.asInstanceOf[SequenceNode].children(1)
val v75 = unrollRepeat0(v70) map { n =>
val BindNode(v71, v72) = n
assert(v71.id == 357)
val BindNode(v73, v74) = v72
assert(v73.id == 358)
v74
}
val v80 = v75 map { n =>
val v76 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v77, v78) = v76
assert(v77.id == 142)
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
    case 143 =>
val v83 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v84, v85) = v83
assert(v84.id == 144)
val v86 = matchSymbol(v85)
v86
case 229 =>
val v87 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v88, v89) = v87
assert(v88.id == 230)
val v90 = matchProcessor(v89)
v90
  }
}
def matchSymbol(node: Node): Symbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 145 =>
val v91 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v92, v93) = v91
assert(v92.id == 146)
val v94 = matchBinSymbol(v93)
v94
  }
}
def matchBinSymbol(node: Node): BinSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 147 =>
val v95 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v96, v97) = v95
assert(v96.id == 146)
val v98 = matchBinSymbol(v97)
val v99 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v100, v101) = v99
assert(v100.id == 149)
val v102 = matchPreUnSymbol(v101)
val v103 = JoinSymbol(node,v98,v102)
v103
case 227 =>
val v104 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v105, v106) = v104
assert(v105.id == 146)
val v107 = matchBinSymbol(v106)
val v108 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v109, v110) = v108
assert(v109.id == 149)
val v111 = matchPreUnSymbol(v110)
val v112 = ExceptSymbol(node,v107,v111)
v112
case 228 =>
val v113 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v114, v115) = v113
assert(v114.id == 149)
val v116 = matchPreUnSymbol(v115)
v116
  }
}
def matchPreUnSymbol(node: Node): PreUnSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 150 =>
val v117 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v118, v119) = v117
assert(v118.id == 149)
val v120 = matchPreUnSymbol(v119)
val v121 = FollowedBy(node,v120)
v121
case 152 =>
val v122 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v123, v124) = v122
assert(v123.id == 149)
val v125 = matchPreUnSymbol(v124)
val v126 = NotFollowedBy(node,v125)
v126
case 154 =>
val v127 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v128, v129) = v127
assert(v128.id == 155)
val v130 = matchPostUnSymbol(v129)
v130
  }
}
def matchPostUnSymbol(node: Node): PostUnSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 156 =>
val v131 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v132, v133) = v131
assert(v132.id == 155)
val v134 = matchPostUnSymbol(v133)
val v135 = Optional(node,v134)
v135
case 157 =>
val v136 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v137, v138) = v136
assert(v137.id == 155)
val v139 = matchPostUnSymbol(v138)
val v140 = RepeatFromZero(node,v139)
v140
case 159 =>
val v141 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v142, v143) = v141
assert(v142.id == 155)
val v144 = matchPostUnSymbol(v143)
val v145 = RepeatFromOne(node,v144)
v145
case 161 =>
val v146 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v147, v148) = v146
assert(v147.id == 162)
val v149 = matchAtomSymbol(v148)
v149
  }
}
def matchAtomSymbol(node: Node): AtomSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 163 =>
val v150 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v151, v152) = v150
assert(v151.id == 164)
val v153 = matchTerminal(v152)
v153
case 180 =>
val v154 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v155, v156) = v154
assert(v155.id == 181)
val v157 = matchTerminalChoice(v156)
v157
case 198 =>
val v158 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v159, v160) = v158
assert(v159.id == 199)
val v161 = matchStringSymbol(v160)
v161
case 210 =>
val v162 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v163, v164) = v162
assert(v163.id == 29)
val v165 = matchNonterminal(v164)
v165
case 211 =>
val v166 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v167, v168) = v166
assert(v167.id == 212)
val v169 = matchInPlaceChoices(v168)
val v170 = Paren(node,v169)
v170
case 220 =>
val v171 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v172, v173) = v171
assert(v172.id == 221)
val v174 = matchLongest(v173)
v174
case 223 =>
val v175 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v176, v177) = v175
assert(v176.id == 224)
val v178 = matchEmptySequence(v177)
v178
  }
}
def matchTerminal(node: Node): Terminal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 165 =>
val v179 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v180, v181) = v179
assert(v180.id == 167)
val v182 = matchTerminalChar(v181)
v182
case 178 =>
val v183 = AnyTerminal(node)
v183
  }
}
def matchTerminalChoice(node: Node): TerminalChoice = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 182 =>
val v184 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v185, v186) = v184
assert(v185.id == 183)
val v187 = matchTerminalChoiceElem(v186)
val v188 = List(v187)
val v189 = body.asInstanceOf[SequenceNode].children(2)
val v193 = unrollRepeat1(v189) map { n =>
val BindNode(v190, v191) = n
assert(v190.id == 183)
val v192 = matchTerminalChoiceElem(v191)
v192
}
val v194 = v193 map { n =>
n
}
val v195 = v188 ++ v194
val v196 = TerminalChoice(node,v195)
v196
case 197 =>
val v197 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v198, v199) = v197
assert(v198.id == 192)
val v200 = matchTerminalChoiceRange(v199)
val v201 = List(v200)
val v202 = TerminalChoice(node,v201)
v202
  }
}
def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 184 =>
val v203 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v204, v205) = v203
assert(v204.id == 185)
val v206 = matchTerminalChoiceChar(v205)
v206
case 191 =>
val v207 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v208, v209) = v207
assert(v208.id == 192)
val v210 = matchTerminalChoiceRange(v209)
v210
  }
}
def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 193 =>
val v211 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v212, v213) = v211
assert(v212.id == 185)
val v214 = matchTerminalChoiceChar(v213)
val v215 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v216, v217) = v215
assert(v216.id == 185)
val v218 = matchTerminalChoiceChar(v217)
val v219 = TerminalChoiceRange(node,v214,v218)
v219
  }
}
def matchStringSymbol(node: Node): StringSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 200 =>
val v220 = body.asInstanceOf[SequenceNode].children(1)
val v224 = unrollRepeat0(v220) map { n =>
val BindNode(v221, v222) = n
assert(v221.id == 204)
val v223 = matchStringChar(v222)
v223
}
val v225 = v224 map { n =>
n
}
val v226 = StringSymbol(node,v225)
v226
  }
}
def matchNonterminal(node: Node): Nonterminal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 30 =>
val v227 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v228, v229) = v227
assert(v228.id == 31)
val v230 = matchId(v229)
val v231 = Nonterminal(node,v230)
v231
  }
}
def matchInPlaceChoices(node: Node): InPlaceChoices = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 213 =>
val v232 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v233, v234) = v232
assert(v233.id == 214)
val v235 = matchInPlaceSequence(v234)
val v236 = List(v235)
val v237 = body.asInstanceOf[SequenceNode].children(1)
val v242 = unrollRepeat0(v237) map { n =>
val BindNode(v238, v239) = n
assert(v238.id == 217)
val BindNode(v240, v241) = v239
assert(v240.id == 218)
v241
}
val v247 = v242 map { n =>
val v243 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v244, v245) = v243
assert(v244.id == 214)
val v246 = matchInPlaceSequence(v245)
v246
}
val v248 = v236 ++ v247
val v249 = InPlaceChoices(node,v248)
v249
  }
}
def matchInPlaceSequence(node: Node): InPlaceSequence = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 141 =>
val v250 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v251, v252) = v250
assert(v251.id == 142)
val v253 = matchElem(v252)
val v254 = List(v253)
val v255 = body.asInstanceOf[SequenceNode].children(1)
val v260 = unrollRepeat0(v255) map { n =>
val BindNode(v256, v257) = n
assert(v256.id == 357)
val BindNode(v258, v259) = v257
assert(v258.id == 358)
v259
}
val v265 = v260 map { n =>
val v261 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v262, v263) = v261
assert(v262.id == 142)
val v264 = matchElem(v263)
v264
}
val v266 = v254 ++ v265
val v267 = InPlaceSequence(node,v266)
v267
  }
}
def matchLongest(node: Node): Longest = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 222 =>
val v268 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v269, v270) = v268
assert(v269.id == 212)
val v271 = matchInPlaceChoices(v270)
val v272 = Longest(node,v271)
v272
  }
}
def matchEmptySequence(node: Node): EmptySeq = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 225 =>
val v273 = EmptySeq(node)
v273
  }
}
def matchTerminalChar(node: Node): TerminalChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 168 =>
val v274 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v275, v276) = v274
assert(v275.id == 169)
val v277 = CharAsIs(node,v276)
v277
case 171 =>
val v278 = body.asInstanceOf[SequenceNode].children(1)
val v279 = CharEscaped(node,v278)
v279
case 173 =>
val v280 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v281, v282) = v280
assert(v281.id == 174)
val v283 = matchUnicodeChar(v282)
v283
  }
}
def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 186 =>
val v284 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v285, v286) = v284
assert(v285.id == 187)
val v287 = CharAsIs(node,v286)
v287
case 189 =>
val v288 = body.asInstanceOf[SequenceNode].children(1)
val v289 = CharEscaped(node,v288)
v289
case 173 =>
val v290 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v291, v292) = v290
assert(v291.id == 174)
val v293 = matchUnicodeChar(v292)
v293
  }
}
def matchStringChar(node: Node): StringChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 205 =>
val v294 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v295, v296) = v294
assert(v295.id == 206)
val v297 = CharAsIs(node,v296)
v297
case 208 =>
val v298 = body.asInstanceOf[SequenceNode].children(1)
val v299 = CharEscaped(node,v298)
v299
case 173 =>
val v300 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v301, v302) = v300
assert(v301.id == 174)
val v303 = matchUnicodeChar(v302)
v303
  }
}
def matchUnicodeChar(node: Node): CharUnicode = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 175 =>
val v304 = body.asInstanceOf[SequenceNode].children(2)
val v305 = body.asInstanceOf[SequenceNode].children(3)
val v306 = body.asInstanceOf[SequenceNode].children(4)
val v307 = body.asInstanceOf[SequenceNode].children(5)
val v308 = List(v304,v305,v306,v307)
val v309 = CharUnicode(node,v308)
v309
  }
}
def matchProcessor(node: Node): Processor = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 231 =>
val v310 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v311, v312) = v310
assert(v311.id == 232)
val v313 = matchRef(v312)
v313
case 259 =>
val v314 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v315, v316) = v314
assert(v315.id == 260)
val v317 = matchPExpr(v316)
v317
  }
}
def matchRef(node: Node): Ref = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 233 =>
val v318 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v319, v320) = v318
assert(v319.id == 234)
val v321 = matchValRef(v320)
v321
case 254 =>
val v322 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v323, v324) = v322
assert(v323.id == 255)
val v325 = matchRawRef(v324)
v325
  }
}
def matchValRef(node: Node): ValRef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 235 =>
val v326 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v327, v328) = v326
assert(v327.id == 243)
val v329 = matchRefIdx(v328)
val v330 = body.asInstanceOf[SequenceNode].children(1)
val v334 = unrollOptional(v330, 41, 238) map { n =>
val BindNode(v331, v332) = n
assert(v331.id == 238)
val v333 = matchCondSymPath(v332)
v333
}
val v335 = ValRef(node,v329,v334)
v335
  }
}
def matchCondSymPath(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 239 =>
val v336 = body.asInstanceOf[SequenceNode].children(0)
val v337 = unrollRepeat1(v336) map { n =>
// UnrollChoices
n
}
v337
  }
}
def matchRawRef(node: Node): RawRef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 256 =>
val v338 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v339, v340) = v338
assert(v339.id == 243)
val v341 = matchRefIdx(v340)
val v342 = body.asInstanceOf[SequenceNode].children(1)
val v346 = unrollOptional(v342, 41, 238) map { n =>
val BindNode(v343, v344) = n
assert(v343.id == 238)
val v345 = matchCondSymPath(v344)
v345
}
val v347 = RawRef(node,v341,v346)
v347
  }
}
def matchPExpr(node: Node): PExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 261 =>
val v348 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v349, v350) = v348
assert(v349.id == 262)
val v351 = matchTernateryExpr(v350)
v351
  }
}
def matchTernateryExpr(node: Node): TerExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 263 =>
val v352 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v353, v354) = v352
assert(v353.id == 264)
val v355 = matchBoolOrExpr(v354)
val v356 = body.asInstanceOf[SequenceNode].children(4)
val v357 = body.asInstanceOf[SequenceNode].children(8)
val v358 = TernateryOp(node,v355,v356,v357)
v358
case 354 =>
val v359 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v360, v361) = v359
assert(v360.id == 264)
val v362 = matchBoolOrExpr(v361)
v362
  }
}
def matchBoolOrExpr(node: Node): BoolOrExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 265 =>
val v363 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v364, v365) = v363
assert(v364.id == 266)
val v366 = matchBoolAndExpr(v365)
val v367 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v368, v369) = v367
assert(v368.id == 264)
val v370 = matchBoolOrExpr(v369)
val v371 = body.asInstanceOf[SequenceNode].children(2)
val v372 = BinOp(node,v366,v370,v371)
v372
case 352 =>
val v373 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v374, v375) = v373
assert(v374.id == 266)
val v376 = matchBoolAndExpr(v375)
v376
  }
}
def matchBoolAndExpr(node: Node): BoolAndExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 267 =>
val v377 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v378, v379) = v377
assert(v378.id == 268)
val v380 = matchBoolEqExpr(v379)
val v381 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v382, v383) = v381
assert(v382.id == 266)
val v384 = matchBoolAndExpr(v383)
val v385 = body.asInstanceOf[SequenceNode].children(2)
val v386 = BinOp(node,v380,v384,v385)
v386
case 349 =>
val v387 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v388, v389) = v387
assert(v388.id == 268)
val v390 = matchBoolEqExpr(v389)
v390
  }
}
def matchBoolEqExpr(node: Node): BoolEqExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 269 =>
val v391 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v392, v393) = v391
assert(v392.id == 270)
val v394 = matchElvisExpr(v393)
val v395 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v396, v397) = v395
assert(v396.id == 268)
val v398 = matchBoolEqExpr(v397)
// UnrollChoices
val v399 = BinOp(node,v394,v398,body)
v399
case 346 =>
val v400 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v401, v402) = v400
assert(v401.id == 270)
val v403 = matchElvisExpr(v402)
v403
  }
}
def matchElvisExpr(node: Node): ElvisExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 271 =>
val v404 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v405, v406) = v404
assert(v405.id == 272)
val v407 = matchAdditiveExpr(v406)
val v408 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v409, v410) = v408
assert(v409.id == 270)
val v411 = matchElvisExpr(v410)
val v412 = ElvisOp(node,v407,v411)
v412
case 340 =>
val v413 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v414, v415) = v413
assert(v414.id == 272)
val v416 = matchAdditiveExpr(v415)
v416
  }
}
def matchAdditiveExpr(node: Node): AdditiveExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 273 =>
val v417 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v418, v419) = v417
assert(v418.id == 274)
val v420 = matchPrefixNotExpr(v419)
val v421 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v422, v423) = v421
assert(v422.id == 272)
val v424 = matchAdditiveExpr(v423)
val v425 = body.asInstanceOf[SequenceNode].children(2)
val v426 = BinOp(node,v420,v424,v425)
v426
case 337 =>
val v427 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v428, v429) = v427
assert(v428.id == 274)
val v430 = matchPrefixNotExpr(v429)
v430
  }
}
def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 275 =>
val v431 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v432, v433) = v431
assert(v432.id == 274)
val v434 = matchPrefixNotExpr(v433)
val v435 = body.asInstanceOf[SequenceNode].children(0)
val v436 = PrefixOp(node,v434,v435)
v436
case 276 =>
val v437 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v438, v439) = v437
assert(v438.id == 277)
val v440 = matchAtom(v439)
v440
  }
}
def matchAtom(node: Node): Atom = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 231 =>
val v441 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v442, v443) = v441
assert(v442.id == 232)
val v444 = matchRef(v443)
v444
case 278 =>
val v445 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v446, v447) = v445
assert(v446.id == 279)
val v448 = matchBindExpr(v447)
v448
case 282 =>
val v449 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v450, v451) = v449
assert(v450.id == 283)
val v452 = matchNamedConstructExpr(v451)
v452
case 293 =>
val v453 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v454, v455) = v453
assert(v454.id == 294)
val v456 = matchFuncCallOrConstructExpr(v455)
v456
case 306 =>
val v457 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v458, v459) = v457
assert(v458.id == 307)
val v460 = matchArrayExpr(v459)
v460
case 309 =>
val v461 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v462, v463) = v461
assert(v462.id == 310)
val v464 = matchLiteral(v463)
v464
case 329 =>
val v465 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v466, v467) = v465
assert(v466.id == 330)
val v468 = matchEnumValue(v467)
v468
case 336 =>
val v469 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v470, v471) = v469
assert(v470.id == 260)
val v472 = matchPExpr(v471)
val v473 = ExprParen(node,v472)
v473
  }
}
def matchBindExpr(node: Node): BindExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 280 =>
val v474 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v475, v476) = v474
assert(v475.id == 234)
val v477 = matchValRef(v476)
val v478 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v479, v480) = v478
assert(v479.id == 281)
val v481 = matchBinderExpr(v480)
val v482 = BindExpr(node,v477,v481)
v482
  }
}
def matchBinderExpr(node: Node): PExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 231 =>
val v483 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v484, v485) = v483
assert(v484.id == 232)
val v486 = matchRef(v485)
v486
case 278 =>
val v487 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v488, v489) = v487
assert(v488.id == 279)
val v490 = matchBindExpr(v489)
v490
case 259 =>
val v491 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v492, v493) = v491
assert(v492.id == 260)
val v494 = matchPExpr(v493)
v494
  }
}
def matchNamedConstructExpr(node: Node): NamedConstructExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 284 =>
val v495 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v496, v497) = v495
assert(v496.id == 49)
val v498 = matchTypeName(v497)
val v499 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v500, v501) = v499
assert(v500.id == 285)
val v502 = matchNamedConstructParams(v501)
val v503 = NamedConstructExpr(node,v498,v502)
v503
  }
}
def matchNamedConstructParams(node: Node): List[NamedParam] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 286 =>
val v504 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v505, v506) = v504
assert(v505.id == 287)
val v507 = matchNamedParam(v506)
val v508 = List(v507)
val v509 = body.asInstanceOf[SequenceNode].children(3)
val v514 = unrollRepeat0(v509) map { n =>
val BindNode(v510, v511) = n
assert(v510.id == 291)
val BindNode(v512, v513) = v511
assert(v512.id == 292)
v513
}
val v519 = v514 map { n =>
val v515 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v516, v517) = v515
assert(v516.id == 287)
val v518 = matchNamedParam(v517)
v518
}
val v520 = v508 ++ v519
v520
  }
}
def matchNamedParam(node: Node): NamedParam = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 288 =>
val v521 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v522, v523) = v521
assert(v522.id == 107)
val v524 = matchParamName(v523)
val v525 = body.asInstanceOf[SequenceNode].children(1)
val v530 = unrollOptional(v525, 41, 42) map { n =>
val BindNode(v526, v527) = n
assert(v526.id == 42)
val BindNode(v528, v529) = v527
assert(v528.id == 43)
v529
}
val v531 = body.asInstanceOf[SequenceNode].children(5)
val BindNode(v532, v533) = v531
assert(v532.id == 260)
val v534 = matchPExpr(v533)
val v535 = NamedParam(node,v524,v530,v534)
v535
  }
}
def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 295 =>
val v536 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v537, v538) = v536
assert(v537.id == 296)
val v539 = matchTypeOrFuncName(v538)
val v540 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v541, v542) = v540
assert(v541.id == 297)
val v543 = matchCallParams(v542)
val v544 = FuncCallOrConstructExpr(node,v539,v543)
v544
  }
}
def matchCallParams(node: Node): Option[List[PExpr]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 298 =>
val v545 = body.asInstanceOf[SequenceNode].children(2)
val v550 = unrollOptional(v545, 41, 300) map { n =>
val BindNode(v546, v547) = n
assert(v546.id == 300)
val BindNode(v548, v549) = v547
assert(v548.id == 301)
v549
}
val v568 = v550 map { n =>
val v551 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v552, v553) = v551
assert(v552.id == 260)
val v554 = matchPExpr(v553)
val v555 = List(v554)
val v556 = n.asInstanceOf[SequenceNode].children(1)
val v561 = unrollRepeat0(v556) map { n =>
val BindNode(v557, v558) = n
assert(v557.id == 304)
val BindNode(v559, v560) = v558
assert(v559.id == 305)
v560
}
val v566 = v561 map { n =>
val v562 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v563, v564) = v562
assert(v563.id == 260)
val v565 = matchPExpr(v564)
v565
}
val v567 = v555 ++ v566
v567
}
v568
  }
}
def matchArrayExpr(node: Node): ArrayExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 308 =>
val v569 = body.asInstanceOf[SequenceNode].children(2)
val v574 = unrollOptional(v569, 41, 300) map { n =>
val BindNode(v570, v571) = n
assert(v570.id == 300)
val BindNode(v572, v573) = v571
assert(v572.id == 301)
v573
}
val v592 = v574 map { n =>
val v575 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v576, v577) = v575
assert(v576.id == 260)
val v578 = matchPExpr(v577)
val v579 = List(v578)
val v580 = n.asInstanceOf[SequenceNode].children(1)
val v585 = unrollRepeat0(v580) map { n =>
val BindNode(v581, v582) = n
assert(v581.id == 304)
val BindNode(v583, v584) = v582
assert(v583.id == 305)
v584
}
val v590 = v585 map { n =>
val v586 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v587, v588) = v586
assert(v587.id == 260)
val v589 = matchPExpr(v588)
v589
}
val v591 = v579 ++ v590
v591
}
val v593 = ArrayExpr(node,v592)
v593
  }
}
def matchLiteral(node: Node): Literal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 311 =>
val v594 = NullLiteral(node)
v594
case 314 =>
// UnrollChoices
val v595 = BoolLiteral(node,body)
v595
case 321 =>
val v596 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v597, v598) = v596
assert(v597.id == 322)
val v599 = matchCharChar(v598)
val v600 = CharLiteral(node,v599)
v600
case 324 =>
val v601 = body.asInstanceOf[SequenceNode].children(1)
val v605 = unrollRepeat0(v601) map { n =>
val BindNode(v602, v603) = n
assert(v602.id == 327)
val v604 = matchStrChar(v603)
v604
}
val v606 = StringLiteral(node,v605)
v606
  }
}
def matchEnumValue(node: Node): AbstractEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 331 =>
val v607 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v608, v609) = v607
assert(v608.id == 332)
val v610 = matchCanonicalEnumValue(v609)
v610
case 334 =>
val v611 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v612, v613) = v611
assert(v612.id == 335)
val v614 = matchShortenedEnumValue(v613)
v614
  }
}
def matchCanonicalEnumValue(node: Node): CanonicalEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 333 =>
val v615 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v616, v617) = v615
assert(v616.id == 78)
val v618 = matchEnumTypeName(v617)
val v619 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v620, v621) = v619
assert(v620.id == 31)
val v622 = matchId(v621)
val v623 = CanonicalEnumValue(node,v618,v622)
v623
  }
}
def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 79 =>
val v624 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v625, v626) = v624
assert(v625.id == 31)
val v627 = matchId(v626)
val v628 = ShortenedEnumValue(node,v627)
v628
  }
}
def matchTypeDef(node: Node): TypeDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 83 =>
val v629 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v630, v631) = v629
assert(v630.id == 84)
val v632 = matchClassDef(v631)
v632
case 113 =>
val v633 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v634, v635) = v633
assert(v634.id == 114)
val v636 = matchSuperDef(v635)
v636
case 128 =>
val v637 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v638, v639) = v637
assert(v638.id == 129)
val v640 = matchEnumTypeDef(v639)
v640
  }
}
def matchClassDef(node: Node): ClassDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 85 =>
val v641 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v642, v643) = v641
assert(v642.id == 49)
val v644 = matchTypeName(v643)
val v645 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v646, v647) = v645
assert(v646.id == 86)
val v648 = matchSuperTypes(v647)
val v649 = AbstractClassDef(node,v644,v648)
v649
case 98 =>
val v650 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v651, v652) = v650
assert(v651.id == 49)
val v653 = matchTypeName(v652)
val v654 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v655, v656) = v654
assert(v655.id == 86)
val v657 = matchSuperTypes(v656)
val v658 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v659, v660) = v658
assert(v659.id == 99)
val v661 = matchClassParamsDef(v660)
val v662 = ConcreteClassDef(node,v653,v657,v661)
v662
  }
}
def matchSuperTypes(node: Node): Option[List[TypeName]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 87 =>
val v663 = body.asInstanceOf[SequenceNode].children(2)
val v668 = unrollOptional(v663, 41, 90) map { n =>
val BindNode(v664, v665) = n
assert(v664.id == 90)
val BindNode(v666, v667) = v665
assert(v666.id == 91)
v667
}
val v686 = v668 map { n =>
val v669 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v670, v671) = v669
assert(v670.id == 49)
val v672 = matchTypeName(v671)
val v673 = List(v672)
val v674 = n.asInstanceOf[SequenceNode].children(1)
val v679 = unrollRepeat0(v674) map { n =>
val BindNode(v675, v676) = n
assert(v675.id == 94)
val BindNode(v677, v678) = v676
assert(v677.id == 95)
v678
}
val v684 = v679 map { n =>
val v680 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v681, v682) = v680
assert(v681.id == 49)
val v683 = matchTypeName(v682)
v683
}
val v685 = v673 ++ v684
v685
}
v686
  }
}
def matchClassParamsDef(node: Node): Option[List[ClassParamDef]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 100 =>
val v687 = body.asInstanceOf[SequenceNode].children(2)
val v692 = unrollOptional(v687, 41, 103) map { n =>
val BindNode(v688, v689) = n
assert(v688.id == 103)
val BindNode(v690, v691) = v689
assert(v690.id == 104)
v691
}
val v710 = v692 map { n =>
val v693 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v694, v695) = v693
assert(v694.id == 105)
val v696 = matchClassParamDef(v695)
val v697 = List(v696)
val v698 = n.asInstanceOf[SequenceNode].children(1)
val v703 = unrollRepeat0(v698) map { n =>
val BindNode(v699, v700) = n
assert(v699.id == 110)
val BindNode(v701, v702) = v700
assert(v701.id == 111)
v702
}
val v708 = v703 map { n =>
val v704 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v705, v706) = v704
assert(v705.id == 105)
val v707 = matchClassParamDef(v706)
v707
}
val v709 = v697 ++ v708
v709
}
v710
  }
}
def matchClassParamDef(node: Node): ClassParamDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 106 =>
val v711 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v712, v713) = v711
assert(v712.id == 107)
val v714 = matchParamName(v713)
val v715 = body.asInstanceOf[SequenceNode].children(1)
val v720 = unrollOptional(v715, 41, 42) map { n =>
val BindNode(v716, v717) = n
assert(v716.id == 42)
val BindNode(v718, v719) = v717
assert(v718.id == 43)
v719
}
val v725 = v720 map { n =>
val v721 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v722, v723) = v721
assert(v722.id == 45)
val v724 = matchTypeDesc(v723)
v724
}
val v726 = ClassParamDef(node,v714,v725)
v726
  }
}
def matchSuperDef(node: Node): SuperDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 115 =>
val v727 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v728, v729) = v727
assert(v728.id == 49)
val v730 = matchTypeName(v729)
val v731 = body.asInstanceOf[SequenceNode].children(4)
val v736 = unrollOptional(v731, 41, 118) map { n =>
val BindNode(v732, v733) = n
assert(v732.id == 118)
val BindNode(v734, v735) = v733
assert(v734.id == 119)
v735
}
val v741 = v736 map { n =>
val v737 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v738, v739) = v737
assert(v738.id == 120)
val v740 = matchSubTypes(v739)
v740
}
val v742 = SuperDef(node,v730,v741)
v742
  }
}
def matchSubTypes(node: Node): List[SubType] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 121 =>
val v743 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v744, v745) = v743
assert(v744.id == 122)
val v746 = matchSubType(v745)
val v747 = List(v746)
val v748 = body.asInstanceOf[SequenceNode].children(1)
val v753 = unrollRepeat0(v748) map { n =>
val BindNode(v749, v750) = n
assert(v749.id == 125)
val BindNode(v751, v752) = v750
assert(v751.id == 126)
v752
}
val v758 = v753 map { n =>
val v754 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v755, v756) = v754
assert(v755.id == 122)
val v757 = matchSubType(v756)
v757
}
val v759 = v747 ++ v758
v759
  }
}
def matchSubType(node: Node): SubType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 48 =>
val v760 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v761, v762) = v760
assert(v761.id == 49)
val v763 = matchTypeName(v762)
v763
case 83 =>
val v764 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v765, v766) = v764
assert(v765.id == 84)
val v767 = matchClassDef(v766)
v767
case 113 =>
val v768 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v769, v770) = v768
assert(v769.id == 114)
val v771 = matchSuperDef(v770)
v771
  }
}
def matchEnumTypeDef(node: Node): EnumTypeDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 130 =>
val v772 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v773, v774) = v772
assert(v773.id == 78)
val v775 = matchEnumTypeName(v774)
val v776 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v777, v778) = v776
assert(v777.id == 31)
val v779 = matchId(v778)
val v780 = List(v779)
val v781 = body.asInstanceOf[SequenceNode].children(5)
val v786 = unrollRepeat0(v781) map { n =>
val BindNode(v782, v783) = n
assert(v782.id == 133)
val BindNode(v784, v785) = v783
assert(v784.id == 134)
v785
}
val v791 = v786 map { n =>
val v787 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v788, v789) = v787
assert(v788.id == 31)
val v790 = matchId(v789)
v790
}
val v792 = v780 ++ v791
val v793 = EnumTypeDef(node,v775,v792)
v793
  }
}
def matchTypeDesc(node: Node): TypeDesc = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 46 =>
val v794 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v795, v796) = v794
assert(v795.id == 47)
val v797 = matchNonNullTypeDesc(v796)
val v798 = body.asInstanceOf[SequenceNode].children(1)
val v803 = unrollOptional(v798, 41, 136) map { n =>
val BindNode(v799, v800) = n
assert(v799.id == 136)
val BindNode(v801, v802) = v800
assert(v801.id == 137)
v802
}
val v804 = TypeDesc(node,v797,v803)
v804
  }
}
def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 48 =>
val v805 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v806, v807) = v805
assert(v806.id == 49)
val v808 = matchTypeName(v807)
v808
case 50 =>
val v809 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v810, v811) = v809
assert(v810.id == 45)
val v812 = matchTypeDesc(v811)
val v813 = ArrayTypeDesc(node,v812)
v813
case 53 =>
val v814 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v815, v816) = v814
assert(v815.id == 54)
val v817 = matchValueType(v816)
v817
case 77 =>
val v818 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v819, v820) = v818
assert(v819.id == 78)
val v821 = matchEnumTypeName(v820)
v821
case 81 =>
val v822 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v823, v824) = v822
assert(v823.id == 82)
val v825 = matchTypeDef(v824)
v825
  }
}
def matchValueType(node: Node): ValueType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 55 =>
val v826 = BooleanType(node)
v826
case 64 =>
val v827 = CharType(node)
v827
case 70 =>
val v828 = StringType(node)
v828
  }
}
def matchEnumTypeName(node: Node): EnumTypeName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 79 =>
val v829 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v830, v831) = v829
assert(v830.id == 31)
val v832 = matchId(v831)
val v833 = EnumTypeName(node,v832)
v833
  }
}
def matchTypeName(node: Node): TypeName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 30 =>
val v834 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v835, v836) = v834
assert(v835.id == 31)
val v837 = matchId(v836)
val v838 = TypeName(node,v837)
v838
  }
}
def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 30 =>
val v839 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v840, v841) = v839
assert(v840.id == 31)
val v842 = matchId(v841)
val v843 = TypeOrFuncName(node,v842)
v843
  }
}
def matchParamName(node: Node): ParamName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 30 =>
val v844 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v845, v846) = v844
assert(v845.id == 31)
val v847 = matchId(v846)
val v848 = ParamName(node,v847)
v848
  }
}
def matchStrChar(node: Node): StringChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 328 =>
val v849 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v850, v851) = v849
assert(v850.id == 204)
val v852 = matchStringChar(v851)
v852
  }
}
def matchCharChar(node: Node): TerminalChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 323 =>
val v853 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v854, v855) = v853
assert(v854.id == 167)
val v856 = matchTerminalChar(v855)
v856
  }
}
def matchRefIdx(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 244 =>
val v857 = body.asInstanceOf[SequenceNode].children(0)
v857
  }
}
def matchId(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v858 = body.asInstanceOf[SequenceNode].children(0)
v858
  }
}
def matchWS(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 5 =>
val v859 = body.asInstanceOf[SequenceNode].children(0)
val v860 = unrollRepeat0(v859) map { n =>
// UnrollChoices
n
}
v860
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
val v861 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v862, v863) = v861
assert(v862.id == 22)
v863
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

def main(args: Array[String]): Unit =
  parseAst(MetaLang3Grammar.inMetaLang3.grammar) match {
    case Left(ast) =>
      println(ast.prettyPrint())
    case Right(err) =>
      println(err)
  }
}