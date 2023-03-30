package com.giyeok.jparser.metalang2.generated

import com.giyeok.jparser.Inputs.InputToShortString
import com.giyeok.jparser.ParseResultTree.{JoinNode, Node, BindNode, TerminalNode, SequenceNode}
import com.giyeok.jparser.nparser.{ParseTreeConstructor, NaiveParser, Parser}
import com.giyeok.jparser.{NGrammar, ParsingErrors, ParseForestFunc, Symbols}
import scala.collection.immutable.ListSet

object MetaGrammar2Ast {
  val ngrammar = ???
//  new NGrammar(
//  Map(1 -> NGrammar.NStart(1, 2),
//2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("Grammar"), Set(3)),
//4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("WS"), Set(5)),
//6 -> NGrammar.NRepeat(6, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0), 7, 8),
//9 -> NGrammar.NOneOf(9, Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), Set(10,11)),
//10 -> NGrammar.NTerminal(10, Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet)),
//11 -> NGrammar.NNonterminal(11, Symbols.Nonterminal("LineComment"), Set(12)),
//13 -> NGrammar.NTerminal(13, Symbols.ExactChar('/')),
//14 -> NGrammar.NRepeat(14, Symbols.Repeat(Symbols.Proxy(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0)), 7, 15),
//16 -> NGrammar.NExcept(16, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 17, 18),
//17 -> NGrammar.NTerminal(17, Symbols.AnyChar),
//18 -> NGrammar.NTerminal(18, Symbols.ExactChar('\n')),
//19 -> NGrammar.NOneOf(19, Symbols.OneOf(ListSet(Symbols.Nonterminal("EOF"),Symbols.ExactChar('\n'))), Set(20,18)),
//20 -> NGrammar.NNonterminal(20, Symbols.Nonterminal("EOF"), Set(21)),
//22 -> NGrammar.NLookaheadExcept(22, Symbols.LookaheadExcept(Symbols.AnyChar), 7, 17),
//23 -> NGrammar.NNonterminal(23, Symbols.Nonterminal("Def"), Set(24,243)),
//25 -> NGrammar.NNonterminal(25, Symbols.Nonterminal("Rule"), Set(26)),
//27 -> NGrammar.NNonterminal(27, Symbols.Nonterminal("LHS"), Set(28)),
//29 -> NGrammar.NNonterminal(29, Symbols.Nonterminal("Nonterminal"), Set(30)),
//31 -> NGrammar.NNonterminal(31, Symbols.Nonterminal("Id"), Set(32)),
//33 -> NGrammar.NLongest(33, Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))), 34),
//34 -> NGrammar.NProxy(34, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))), 35),
//36 -> NGrammar.NTerminal(36, Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
//37 -> NGrammar.NRepeat(37, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), 7, 38),
//39 -> NGrammar.NTerminal(39, Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
//40 -> NGrammar.NOneOf(40, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))), Set(41,42)),
//41 -> NGrammar.NProxy(41, Symbols.Proxy(Symbols.Sequence(Seq())), 7),
//42 -> NGrammar.NProxy(42, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))), 43),
//44 -> NGrammar.NTerminal(44, Symbols.ExactChar(':')),
//45 -> NGrammar.NNonterminal(45, Symbols.Nonterminal("TypeDesc"), Set(46)),
//47 -> NGrammar.NNonterminal(47, Symbols.Nonterminal("ValueTypeDesc"), Set(48,50,66)),
//49 -> NGrammar.NNonterminal(49, Symbols.Nonterminal("TypeName"), Set(30)),
//51 -> NGrammar.NNonterminal(51, Symbols.Nonterminal("OnTheFlyTypeDef"), Set(52)),
//53 -> NGrammar.NTerminal(53, Symbols.ExactChar('@')),
//54 -> NGrammar.NOneOf(54, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("OnTheFlySuperTypes")))))), Set(41,55)),
//55 -> NGrammar.NProxy(55, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("OnTheFlySuperTypes")))), 56),
//57 -> NGrammar.NNonterminal(57, Symbols.Nonterminal("OnTheFlySuperTypes"), Set(58)),
//59 -> NGrammar.NTerminal(59, Symbols.ExactChar('<')),
//60 -> NGrammar.NRepeat(60, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0), 7, 61),
//62 -> NGrammar.NProxy(62, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 63),
//64 -> NGrammar.NTerminal(64, Symbols.ExactChar(',')),
//65 -> NGrammar.NTerminal(65, Symbols.ExactChar('>')),
//67 -> NGrammar.NTerminal(67, Symbols.ExactChar('[')),
//68 -> NGrammar.NTerminal(68, Symbols.ExactChar(']')),
//69 -> NGrammar.NOneOf(69, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))))), Set(41,70)),
//70 -> NGrammar.NProxy(70, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))), 71),
//72 -> NGrammar.NTerminal(72, Symbols.ExactChar('?')),
//73 -> NGrammar.NTerminal(73, Symbols.ExactChar('=')),
//74 -> NGrammar.NNonterminal(74, Symbols.Nonterminal("RHSs"), Set(75)),
//76 -> NGrammar.NNonterminal(76, Symbols.Nonterminal("RHS"), Set(77)),
//78 -> NGrammar.NNonterminal(78, Symbols.Nonterminal("Elem"), Set(79,146)),
//80 -> NGrammar.NNonterminal(80, Symbols.Nonterminal("Processor"), Set(81,96)),
//82 -> NGrammar.NNonterminal(82, Symbols.Nonterminal("Ref"), Set(83)),
//84 -> NGrammar.NTerminal(84, Symbols.ExactChar('$')),
//85 -> NGrammar.NNonterminal(85, Symbols.Nonterminal("RefIdx"), Set(86)),
//87 -> NGrammar.NLongest(87, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))), 88),
//88 -> NGrammar.NOneOf(88, Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))))), Set(89,90)),
//89 -> NGrammar.NTerminal(89, Symbols.ExactChar('0')),
//90 -> NGrammar.NProxy(90, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))), 91),
//92 -> NGrammar.NTerminal(92, Symbols.Chars(('1' to '9').toSet)),
//93 -> NGrammar.NRepeat(93, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 7, 94),
//95 -> NGrammar.NTerminal(95, Symbols.Chars(('0' to '9').toSet)),
//97 -> NGrammar.NTerminal(97, Symbols.ExactChar('{')),
//98 -> NGrammar.NNonterminal(98, Symbols.Nonterminal("PExpr"), Set(99,144)),
//100 -> NGrammar.NLongest(100, Symbols.Longest(Symbols.Nonterminal("BinOp")), 101),
//101 -> NGrammar.NNonterminal(101, Symbols.Nonterminal("BinOp"), Set(102)),
//103 -> NGrammar.NProxy(103, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))), 104),
//105 -> NGrammar.NTerminal(105, Symbols.ExactChar('+')),
//106 -> NGrammar.NNonterminal(106, Symbols.Nonterminal("PTerm"), Set(81,107,111,140,141)),
//108 -> NGrammar.NNonterminal(108, Symbols.Nonterminal("BoundPExpr"), Set(109)),
//110 -> NGrammar.NNonterminal(110, Symbols.Nonterminal("BoundedPExpr"), Set(81,107,96)),
//112 -> NGrammar.NNonterminal(112, Symbols.Nonterminal("ConstructExpr"), Set(113,125)),
//114 -> NGrammar.NNonterminal(114, Symbols.Nonterminal("ConstructParams"), Set(115)),
//116 -> NGrammar.NTerminal(116, Symbols.ExactChar('(')),
//117 -> NGrammar.NOneOf(117, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))), Set(41,118)),
//118 -> NGrammar.NProxy(118, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))), 119),
//120 -> NGrammar.NRepeat(120, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0), 7, 121),
//122 -> NGrammar.NProxy(122, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 123),
//124 -> NGrammar.NTerminal(124, Symbols.ExactChar(')')),
//126 -> NGrammar.NNonterminal(126, Symbols.Nonterminal("OnTheFlyTypeDefConstructExpr"), Set(127)),
//128 -> NGrammar.NNonterminal(128, Symbols.Nonterminal("NamedParams"), Set(129)),
//130 -> NGrammar.NOneOf(130, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Nonterminal("WS")))))), Set(41,131)),
//131 -> NGrammar.NProxy(131, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Nonterminal("WS")))), 132),
//133 -> NGrammar.NNonterminal(133, Symbols.Nonterminal("NamedParam"), Set(134)),
//135 -> NGrammar.NNonterminal(135, Symbols.Nonterminal("ParamName"), Set(30)),
//136 -> NGrammar.NRepeat(136, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0), 7, 137),
//138 -> NGrammar.NProxy(138, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 139),
//142 -> NGrammar.NNonterminal(142, Symbols.Nonterminal("ArrayTerm"), Set(143)),
//145 -> NGrammar.NTerminal(145, Symbols.ExactChar('}')),
//147 -> NGrammar.NNonterminal(147, Symbols.Nonterminal("Symbol"), Set(148)),
//149 -> NGrammar.NNonterminal(149, Symbols.Nonterminal("BinSymbol"), Set(150,233,234)),
//151 -> NGrammar.NTerminal(151, Symbols.ExactChar('&')),
//152 -> NGrammar.NNonterminal(152, Symbols.Nonterminal("PreUnSymbol"), Set(153,155,157)),
//154 -> NGrammar.NTerminal(154, Symbols.ExactChar('^')),
//156 -> NGrammar.NTerminal(156, Symbols.ExactChar('!')),
//158 -> NGrammar.NNonterminal(158, Symbols.Nonterminal("PostUnSymbol"), Set(159,162)),
//160 -> NGrammar.NOneOf(160, Symbols.OneOf(ListSet(Symbols.ExactChar('?'),Symbols.ExactChar('*'),Symbols.ExactChar('+'))), Set(72,161,105)),
//161 -> NGrammar.NTerminal(161, Symbols.ExactChar('*')),
//163 -> NGrammar.NNonterminal(163, Symbols.Nonterminal("AtomSymbol"), Set(164,181,199,211,212,226,229)),
//165 -> NGrammar.NNonterminal(165, Symbols.Nonterminal("Terminal"), Set(166,179)),
//167 -> NGrammar.NTerminal(167, Symbols.ExactChar('\'')),
//168 -> NGrammar.NNonterminal(168, Symbols.Nonterminal("TerminalChar"), Set(169,172,174)),
//170 -> NGrammar.NExcept(170, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')), 17, 171),
//171 -> NGrammar.NTerminal(171, Symbols.ExactChar('\\')),
//173 -> NGrammar.NTerminal(173, Symbols.Chars(Set('\'','\\','b','n','r','t'))),
//175 -> NGrammar.NNonterminal(175, Symbols.Nonterminal("UnicodeChar"), Set(176)),
//177 -> NGrammar.NTerminal(177, Symbols.ExactChar('u')),
//178 -> NGrammar.NTerminal(178, Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet)),
//180 -> NGrammar.NTerminal(180, Symbols.ExactChar('.')),
//182 -> NGrammar.NNonterminal(182, Symbols.Nonterminal("TerminalChoice"), Set(183,198)),
//184 -> NGrammar.NNonterminal(184, Symbols.Nonterminal("TerminalChoiceElem"), Set(185,192)),
//186 -> NGrammar.NNonterminal(186, Symbols.Nonterminal("TerminalChoiceChar"), Set(187,190,174)),
//188 -> NGrammar.NExcept(188, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))), 17, 189),
//189 -> NGrammar.NTerminal(189, Symbols.Chars(Set('\'','-','\\'))),
//191 -> NGrammar.NTerminal(191, Symbols.Chars(Set('\'','-','\\','b','n','r','t'))),
//193 -> NGrammar.NNonterminal(193, Symbols.Nonterminal("TerminalChoiceRange"), Set(194)),
//195 -> NGrammar.NTerminal(195, Symbols.ExactChar('-')),
//196 -> NGrammar.NRepeat(196, Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), 184, 197),
//200 -> NGrammar.NNonterminal(200, Symbols.Nonterminal("StringLiteral"), Set(201)),
//202 -> NGrammar.NTerminal(202, Symbols.ExactChar('"')),
//203 -> NGrammar.NRepeat(203, Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), 7, 204),
//205 -> NGrammar.NNonterminal(205, Symbols.Nonterminal("StringChar"), Set(206,209,174)),
//207 -> NGrammar.NExcept(207, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))), 17, 208),
//208 -> NGrammar.NTerminal(208, Symbols.Chars(Set('"','\\'))),
//210 -> NGrammar.NTerminal(210, Symbols.Chars(Set('"','\\','b','n','r','t'))),
//213 -> NGrammar.NNonterminal(213, Symbols.Nonterminal("InPlaceChoices"), Set(214)),
//215 -> NGrammar.NNonterminal(215, Symbols.Nonterminal("InPlaceSequence"), Set(216)),
//217 -> NGrammar.NRepeat(217, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Symbol")))), 0), 7, 218),
//219 -> NGrammar.NProxy(219, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Symbol")))), 220),
//221 -> NGrammar.NRepeat(221, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 0), 7, 222),
//223 -> NGrammar.NProxy(223, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 224),
//225 -> NGrammar.NTerminal(225, Symbols.ExactChar('|')),
//227 -> NGrammar.NNonterminal(227, Symbols.Nonterminal("Longest"), Set(228)),
//230 -> NGrammar.NNonterminal(230, Symbols.Nonterminal("EmptySequence"), Set(231)),
//232 -> NGrammar.NTerminal(232, Symbols.ExactChar('#')),
//235 -> NGrammar.NRepeat(235, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0), 7, 236),
//237 -> NGrammar.NProxy(237, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 238),
//239 -> NGrammar.NRepeat(239, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0), 7, 240),
//241 -> NGrammar.NProxy(241, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 242),
//244 -> NGrammar.NNonterminal(244, Symbols.Nonterminal("TypeDef"), Set(245,259)),
//246 -> NGrammar.NNonterminal(246, Symbols.Nonterminal("ClassDef"), Set(247)),
//248 -> NGrammar.NOneOf(248, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParams"),Symbols.Nonterminal("WS")))))), Set(41,249)),
//249 -> NGrammar.NProxy(249, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParams"),Symbols.Nonterminal("WS")))), 250),
//251 -> NGrammar.NNonterminal(251, Symbols.Nonterminal("ClassParams"), Set(252)),
//253 -> NGrammar.NNonterminal(253, Symbols.Nonterminal("ClassParam"), Set(254)),
//255 -> NGrammar.NRepeat(255, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParam")))), 0), 7, 256),
//257 -> NGrammar.NProxy(257, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParam")))), 258),
//260 -> NGrammar.NNonterminal(260, Symbols.Nonterminal("SuperDef"), Set(261)),
//262 -> NGrammar.NOneOf(262, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"),Symbols.Nonterminal("WS")))))), Set(41,263)),
//263 -> NGrammar.NProxy(263, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"),Symbols.Nonterminal("WS")))), 264),
//265 -> NGrammar.NNonterminal(265, Symbols.Nonterminal("SubTypes"), Set(266)),
//267 -> NGrammar.NNonterminal(267, Symbols.Nonterminal("SubType"), Set(48,268,269)),
//270 -> NGrammar.NRepeat(270, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0), 7, 271),
//272 -> NGrammar.NProxy(272, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 273),
//274 -> NGrammar.NRepeat(274, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))), 0), 7, 275),
//276 -> NGrammar.NProxy(276, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))), 277)),
//  Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))), 0),Symbols.Nonterminal("WS"))), Seq(4,23,274,4)),
//5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0))), Seq(6)),
//7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq()), Seq()),
//8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))), 0),Symbols.OneOf(ListSet(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet),Symbols.Nonterminal("LineComment"))))), Seq(6,9)),
//12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.ExactChar('/'),Symbols.ExactChar('/'),Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0),Symbols.OneOf(ListSet(Symbols.Nonterminal("EOF"),Symbols.ExactChar('\n'))))), Seq(13,13,14,19)),
//15 -> NGrammar.NSequence(15, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0),Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')))), Seq(14,16)),
//21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.LookaheadExcept(Symbols.AnyChar))), Seq(22)),
//24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Nonterminal("Rule"))), Seq(25)),
//26 -> NGrammar.NSequence(26, Symbols.Sequence(Seq(Symbols.Nonterminal("LHS"),Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHSs"))), Seq(27,4,73,4,74)),
//28 -> NGrammar.NSequence(28, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))))), Seq(29,40)),
//30 -> NGrammar.NSequence(30, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(31)),
//32 -> NGrammar.NSequence(32, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))), Seq(33)),
//35 -> NGrammar.NSequence(35, Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))), Seq(36,37)),
//38 -> NGrammar.NSequence(38, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0),Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(37,39)),
//43 -> NGrammar.NSequence(43, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc"))), Seq(4,44,4,45)),
//46 -> NGrammar.NSequence(46, Symbols.Sequence(Seq(Symbols.Nonterminal("ValueTypeDesc"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))))))), Seq(47,69)),
//48 -> NGrammar.NSequence(48, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"))), Seq(49)),
//50 -> NGrammar.NSequence(50, Symbols.Sequence(Seq(Symbols.Nonterminal("OnTheFlyTypeDef"))), Seq(51)),
//52 -> NGrammar.NSequence(52, Symbols.Sequence(Seq(Symbols.ExactChar('@'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("OnTheFlySuperTypes")))))))), Seq(53,4,49,54)),
//56 -> NGrammar.NSequence(56, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("OnTheFlySuperTypes"))), Seq(4,57)),
//58 -> NGrammar.NSequence(58, Symbols.Sequence(Seq(Symbols.ExactChar('<'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Nonterminal("WS"),Symbols.ExactChar('>'))), Seq(59,4,49,60,4,65)),
//61 -> NGrammar.NSequence(61, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName")))))), Seq(60,62)),
//63 -> NGrammar.NSequence(63, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeName"))), Seq(4,64,4,49)),
//66 -> NGrammar.NSequence(66, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc"),Symbols.Nonterminal("WS"),Symbols.ExactChar(']'))), Seq(67,4,45,4,68)),
//71 -> NGrammar.NSequence(71, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?'))), Seq(4,72)),
//75 -> NGrammar.NSequence(75, Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0))), Seq(76,239)),
//77 -> NGrammar.NSequence(77, Symbols.Sequence(Seq(Symbols.Nonterminal("Elem"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0))), Seq(78,235)),
//79 -> NGrammar.NSequence(79, Symbols.Sequence(Seq(Symbols.Nonterminal("Processor"))), Seq(80)),
//81 -> NGrammar.NSequence(81, Symbols.Sequence(Seq(Symbols.Nonterminal("Ref"))), Seq(82)),
//83 -> NGrammar.NSequence(83, Symbols.Sequence(Seq(Symbols.ExactChar('$'),Symbols.Nonterminal("RefIdx"))), Seq(84,85)),
//86 -> NGrammar.NSequence(86, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))))), Seq(87)),
//91 -> NGrammar.NSequence(91, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(92,93)),
//94 -> NGrammar.NSequence(94, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0),Symbols.Chars(('0' to '9').toSet))), Seq(93,95)),
//96 -> NGrammar.NSequence(96, Symbols.Sequence(Seq(Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(97,4,98,4,145)),
//99 -> NGrammar.NSequence(99, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Nonterminal("WS"),Symbols.Longest(Symbols.Nonterminal("BinOp")),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PTerm"))), Seq(98,4,100,4,106)),
//102 -> NGrammar.NSequence(102, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))))), Seq(103)),
//104 -> NGrammar.NSequence(104, Symbols.Sequence(Seq(Symbols.ExactChar('+'))), Seq(105)),
//107 -> NGrammar.NSequence(107, Symbols.Sequence(Seq(Symbols.Nonterminal("BoundPExpr"))), Seq(108)),
//109 -> NGrammar.NSequence(109, Symbols.Sequence(Seq(Symbols.Nonterminal("Ref"),Symbols.Nonterminal("BoundedPExpr"))), Seq(82,110)),
//111 -> NGrammar.NSequence(111, Symbols.Sequence(Seq(Symbols.Nonterminal("ConstructExpr"))), Seq(112)),
//113 -> NGrammar.NSequence(113, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ConstructParams"))), Seq(49,4,114)),
//115 -> NGrammar.NSequence(115, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(')'))), Seq(116,4,117,124)),
//119 -> NGrammar.NSequence(119, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS"))), Seq(98,120,4)),
//121 -> NGrammar.NSequence(121, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))))), Seq(120,122)),
//123 -> NGrammar.NSequence(123, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"))), Seq(4,64,4,98)),
//125 -> NGrammar.NSequence(125, Symbols.Sequence(Seq(Symbols.Nonterminal("OnTheFlyTypeDefConstructExpr"))), Seq(126)),
//127 -> NGrammar.NSequence(127, Symbols.Sequence(Seq(Symbols.Nonterminal("OnTheFlyTypeDef"),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParams"))), Seq(51,4,128)),
//129 -> NGrammar.NSequence(129, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(')'))), Seq(116,4,130,124)),
//132 -> NGrammar.NSequence(132, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Nonterminal("WS"))), Seq(133,136,4)),
//134 -> NGrammar.NSequence(134, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))),Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"))), Seq(135,40,4,73,4,98)),
//137 -> NGrammar.NSequence(137, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam")))))), Seq(136,138)),
//139 -> NGrammar.NSequence(139, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("NamedParam"))), Seq(4,64,4,133)),
//140 -> NGrammar.NSequence(140, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(116,4,98,4,124)),
//141 -> NGrammar.NSequence(141, Symbols.Sequence(Seq(Symbols.Nonterminal("ArrayTerm"))), Seq(142)),
//143 -> NGrammar.NSequence(143, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PExpr")))), 0),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(']'))), Seq(67,4,117,68)),
//144 -> NGrammar.NSequence(144, Symbols.Sequence(Seq(Symbols.Nonterminal("PTerm"))), Seq(106)),
//146 -> NGrammar.NSequence(146, Symbols.Sequence(Seq(Symbols.Nonterminal("Symbol"))), Seq(147)),
//148 -> NGrammar.NSequence(148, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"))), Seq(149)),
//150 -> NGrammar.NSequence(150, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('&'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(149,4,151,4,152)),
//153 -> NGrammar.NSequence(153, Symbols.Sequence(Seq(Symbols.ExactChar('^'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(154,4,152)),
//155 -> NGrammar.NSequence(155, Symbols.Sequence(Seq(Symbols.ExactChar('!'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(156,4,152)),
//157 -> NGrammar.NSequence(157, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"))), Seq(158)),
//159 -> NGrammar.NSequence(159, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.ExactChar('?'),Symbols.ExactChar('*'),Symbols.ExactChar('+'))))), Seq(158,4,160)),
//162 -> NGrammar.NSequence(162, Symbols.Sequence(Seq(Symbols.Nonterminal("AtomSymbol"))), Seq(163)),
//164 -> NGrammar.NSequence(164, Symbols.Sequence(Seq(Symbols.Nonterminal("Terminal"))), Seq(165)),
//166 -> NGrammar.NSequence(166, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChar"),Symbols.ExactChar('\''))), Seq(167,168,167)),
//169 -> NGrammar.NSequence(169, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')))), Seq(170)),
//172 -> NGrammar.NSequence(172, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('\'','\\','b','n','r','t')))), Seq(171,173)),
//174 -> NGrammar.NSequence(174, Symbols.Sequence(Seq(Symbols.Nonterminal("UnicodeChar"))), Seq(175)),
//176 -> NGrammar.NSequence(176, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.ExactChar('u'),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet),Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet))), Seq(171,177,178,178,178,178)),
//179 -> NGrammar.NSequence(179, Symbols.Sequence(Seq(Symbols.ExactChar('.'))), Seq(180)),
//181 -> NGrammar.NSequence(181, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoice"))), Seq(182)),
//183 -> NGrammar.NSequence(183, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1),Symbols.ExactChar('\''))), Seq(167,184,196,167)),
//185 -> NGrammar.NSequence(185, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"))), Seq(186)),
//187 -> NGrammar.NSequence(187, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))))), Seq(188)),
//190 -> NGrammar.NSequence(190, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('\'','-','\\','b','n','r','t')))), Seq(171,191)),
//192 -> NGrammar.NSequence(192, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceRange"))), Seq(193)),
//194 -> NGrammar.NSequence(194, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"),Symbols.ExactChar('-'),Symbols.Nonterminal("TerminalChoiceChar"))), Seq(186,195,186)),
//197 -> NGrammar.NSequence(197, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1),Symbols.Nonterminal("TerminalChoiceElem"))), Seq(196,184)),
//198 -> NGrammar.NSequence(198, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceRange"),Symbols.ExactChar('\''))), Seq(167,193,167)),
//199 -> NGrammar.NSequence(199, Symbols.Sequence(Seq(Symbols.Nonterminal("StringLiteral"))), Seq(200)),
//201 -> NGrammar.NSequence(201, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.ExactChar('"'))), Seq(202,203,202)),
//204 -> NGrammar.NSequence(204, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.Nonterminal("StringChar"))), Seq(203,205)),
//206 -> NGrammar.NSequence(206, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))))), Seq(207)),
//209 -> NGrammar.NSequence(209, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('"','\\','b','n','r','t')))), Seq(171,210)),
//211 -> NGrammar.NSequence(211, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"))), Seq(29)),
//212 -> NGrammar.NSequence(212, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("InPlaceChoices"),Symbols.ExactChar(')'))), Seq(116,213,124)),
//214 -> NGrammar.NSequence(214, Symbols.Sequence(Seq(Symbols.Nonterminal("InPlaceSequence"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 0))), Seq(215,221)),
//216 -> NGrammar.NSequence(216, Symbols.Sequence(Seq(Symbols.Nonterminal("Symbol"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Symbol")))), 0))), Seq(147,217)),
//218 -> NGrammar.NSequence(218, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Symbol")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Symbol")))))), Seq(217,219)),
//220 -> NGrammar.NSequence(220, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Symbol"))), Seq(4,147)),
//222 -> NGrammar.NSequence(222, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence")))))), Seq(221,223)),
//224 -> NGrammar.NSequence(224, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("InPlaceSequence"))), Seq(4,225,4,215)),
//226 -> NGrammar.NSequence(226, Symbols.Sequence(Seq(Symbols.Nonterminal("Longest"))), Seq(227)),
//228 -> NGrammar.NSequence(228, Symbols.Sequence(Seq(Symbols.ExactChar('<'),Symbols.Nonterminal("InPlaceChoices"),Symbols.ExactChar('>'))), Seq(59,213,65)),
//229 -> NGrammar.NSequence(229, Symbols.Sequence(Seq(Symbols.Nonterminal("EmptySequence"))), Seq(230)),
//231 -> NGrammar.NSequence(231, Symbols.Sequence(Seq(Symbols.ExactChar('#'))), Seq(232)),
//233 -> NGrammar.NSequence(233, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"),Symbols.Nonterminal("WS"),Symbols.ExactChar('-'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("PreUnSymbol"))), Seq(149,4,195,4,152)),
//234 -> NGrammar.NSequence(234, Symbols.Sequence(Seq(Symbols.Nonterminal("PreUnSymbol"))), Seq(152)),
//236 -> NGrammar.NSequence(236, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem")))))), Seq(235,237)),
//238 -> NGrammar.NSequence(238, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Elem"))), Seq(4,78)),
//240 -> NGrammar.NSequence(240, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS")))))), Seq(239,241)),
//242 -> NGrammar.NSequence(242, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('|'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("RHS"))), Seq(4,225,4,76)),
//243 -> NGrammar.NSequence(243, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeDef"))), Seq(244)),
//245 -> NGrammar.NSequence(245, Symbols.Sequence(Seq(Symbols.ExactChar('@'),Symbols.Nonterminal("ClassDef"))), Seq(53,246)),
//247 -> NGrammar.NSequence(247, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParams"),Symbols.Nonterminal("WS")))))),Symbols.ExactChar(')'))), Seq(49,4,116,4,248,124)),
//250 -> NGrammar.NSequence(250, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParams"),Symbols.Nonterminal("WS"))), Seq(251,4)),
//252 -> NGrammar.NSequence(252, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParam"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParam")))), 0))), Seq(253,255)),
//254 -> NGrammar.NSequence(254, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeDesc")))))))), Seq(135,40)),
//256 -> NGrammar.NSequence(256, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParam")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParam")))))), Seq(255,257)),
//258 -> NGrammar.NSequence(258, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ClassParam"))), Seq(4,64,4,253)),
//259 -> NGrammar.NSequence(259, Symbols.Sequence(Seq(Symbols.ExactChar('@'),Symbols.Nonterminal("SuperDef"))), Seq(53,260)),
//261 -> NGrammar.NSequence(261, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"),Symbols.Nonterminal("WS"),Symbols.ExactChar('{'),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"),Symbols.Nonterminal("WS")))))),Symbols.ExactChar('}'))), Seq(49,4,97,4,262,145)),
//264 -> NGrammar.NSequence(264, Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"),Symbols.Nonterminal("WS"))), Seq(265,4)),
//266 -> NGrammar.NSequence(266, Symbols.Sequence(Seq(Symbols.Nonterminal("SubType"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0))), Seq(267,270)),
//268 -> NGrammar.NSequence(268, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassDef"))), Seq(246)),
//269 -> NGrammar.NSequence(269, Symbols.Sequence(Seq(Symbols.Nonterminal("SuperDef"))), Seq(260)),
//271 -> NGrammar.NSequence(271, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType")))))), Seq(270,272)),
//273 -> NGrammar.NSequence(273, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SubType"))), Seq(4,64,4,267)),
//275 -> NGrammar.NSequence(275, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))), 0),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))))), Seq(274,276)),
//277 -> NGrammar.NSequence(277, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def"))), Seq(4,23))),
//  1)

  sealed trait ASTNode { val astNode: Node }
case class Grammar(astNode:Node, defs:List[Def]) extends ASTNode
sealed trait Def extends ASTNode
sealed trait TypeDef extends ASTNode with Def
case class ClassDef(astNode:Node, typeName:TypeName, params:Option[List[ClassParam]]) extends ASTNode with SubType with TypeDef
case class SuperDef(astNode:Node, typeName:TypeName, subs:Option[List[SubType]]) extends ASTNode with SubType with TypeDef
case class TypeName(astNode:Node, name:Node) extends ASTNode with SubType with ValueTypeDesc
case class ClassParam(astNode:Node, name:ParamName, typeDesc:Option[TypeDesc]) extends ASTNode
case class ParamName(astNode:Node, name:Node) extends ASTNode
case class TypeDesc(astNode:Node, typ:ValueTypeDesc, optional:Option[Node]) extends ASTNode
sealed trait ValueTypeDesc extends ASTNode
case class ArrayTypeDesc(astNode:Node, elemType:TypeDesc) extends ASTNode with ValueTypeDesc
sealed trait SubType extends ASTNode
case class OnTheFlyTypeDef(astNode:Node, name:TypeName, supers:Option[List[TypeName]]) extends ASTNode with ValueTypeDesc
case class Rule(astNode:Node, lhs:LHS, rhs:List[RHS]) extends ASTNode with Def
case class LHS(astNode:Node, name:Nonterminal, typeDesc:Option[TypeDesc]) extends ASTNode
case class RHS(astNode:Node, elems:List[Elem]) extends ASTNode
sealed trait Elem extends ASTNode
sealed trait Processor extends ASTNode with Elem
sealed trait PExpr extends ASTNode with BoundedPExpr with Processor
case class BinOpExpr(astNode:Node, op:Node, lhs:PExpr, rhs:PTerm) extends ASTNode with PExpr
sealed trait PTerm extends ASTNode with PExpr
case class PTermParen(astNode:Node, expr:PExpr) extends ASTNode with PTerm
case class Ref(astNode:Node, idx:Node) extends ASTNode with PTerm
case class PTermSeq(astNode:Node, elems:Option[List[PExpr]]) extends ASTNode with PTerm
case class BoundPExpr(astNode:Node, ctx:Ref, expr:BoundedPExpr) extends ASTNode with PTerm
sealed trait BoundedPExpr extends ASTNode
sealed trait AbstractConstructExpr extends ASTNode with PTerm
case class ConstructExpr(astNode:Node, typeName:TypeName, params:Option[List[PExpr]]) extends ASTNode with AbstractConstructExpr
case class OnTheFlyTypeDefConstructExpr(astNode:Node, typeDef:OnTheFlyTypeDef, params:Option[List[NamedParam]]) extends ASTNode with AbstractConstructExpr
case class NamedParam(astNode:Node, name:ParamName, typeDesc:Option[TypeDesc], expr:PExpr) extends ASTNode
sealed trait Symbol extends ASTNode with Elem
sealed trait BinSymbol extends ASTNode with Symbol
case class JoinSymbol(astNode:Node, symbol1:BinSymbol, symbol2:PreUnSymbol) extends ASTNode with BinSymbol
case class ExceptSymbol(astNode:Node, symbol1:BinSymbol, symbol2:PreUnSymbol) extends ASTNode with BinSymbol
sealed trait PreUnSymbol extends ASTNode with BinSymbol
case class FollowedBy(astNode:Node, expr:PreUnSymbol) extends ASTNode with PreUnSymbol
case class NotFollowedBy(astNode:Node, expr:PreUnSymbol) extends ASTNode with PreUnSymbol
sealed trait PostUnSymbol extends ASTNode with PreUnSymbol
case class Repeat(astNode:Node, expr:PostUnSymbol, repeat:Node) extends ASTNode with PostUnSymbol
sealed trait AtomSymbol extends ASTNode with PostUnSymbol
case class Paren(astNode:Node, choices:InPlaceChoices) extends ASTNode with AtomSymbol
case class EmptySeq(astNode:Node) extends ASTNode with AtomSymbol
case class InPlaceChoices(astNode:Node, choices:List[InPlaceSequence]) extends ASTNode
case class InPlaceSequence(astNode:Node, seq:List[Symbol]) extends ASTNode
case class Longest(astNode:Node, choices:InPlaceChoices) extends ASTNode with AtomSymbol
case class Nonterminal(astNode:Node, name:Node) extends ASTNode with AtomSymbol
sealed trait Terminal extends ASTNode with AtomSymbol
case class AnyTerminal(astNode:Node, c:Node) extends ASTNode with Terminal
case class TerminalChoice(astNode:Node, choices:List[TerminalChoiceElem]) extends ASTNode with AtomSymbol
sealed trait TerminalChoiceElem extends ASTNode
case class TerminalChoiceRange(astNode:Node, start:TerminalChoiceChar, end:TerminalChoiceChar) extends ASTNode with TerminalChoiceElem
case class StringLiteral(astNode:Node, value:List[StringChar]) extends ASTNode with AtomSymbol
case class CharUnicode(astNode:Node, code:List[Node]) extends ASTNode with StringChar with TerminalChar with TerminalChoiceChar
sealed trait TerminalChar extends ASTNode with Terminal
case class CharAsIs(astNode:Node, c:Node) extends ASTNode with StringChar with TerminalChar with TerminalChoiceChar
case class CharEscaped(astNode:Node, escapeCode:Node) extends ASTNode with StringChar with TerminalChar with TerminalChoiceChar
sealed trait TerminalChoiceChar extends ASTNode with TerminalChoiceElem
sealed trait StringChar extends ASTNode
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
assert(v7.id == 276)
val BindNode(v9, v10) = v8
assert(v9.id == 277)
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
case 243 =>
val v23 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v24, v25) = v23
assert(v24.id == 244)
val v26 = matchTypeDef(v25)
v26
  }
}
def matchTypeDef(node: Node): TypeDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 245 =>
val v27 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v28, v29) = v27
assert(v28.id == 246)
val v30 = matchClassDef(v29)
v30
case 259 =>
val v31 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v32, v33) = v31
assert(v32.id == 260)
val v34 = matchSuperDef(v33)
v34
  }
}
def matchClassDef(node: Node): ClassDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 247 =>
val v35 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v36, v37) = v35
assert(v36.id == 49)
val v38 = matchTypeName(v37)
val v39 = body.asInstanceOf[SequenceNode].children(4)
val v44 = unrollOptional(v39, 41, 249) map { n =>
val BindNode(v40, v41) = n
assert(v40.id == 249)
val BindNode(v42, v43) = v41
assert(v42.id == 250)
v43
}
val v49 = v44 map { n =>
val v45 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v46, v47) = v45
assert(v46.id == 251)
val v48 = matchClassParams(v47)
v48
}
val v50 = ClassDef(node,v38,v49)
v50
  }
}
def matchSuperDef(node: Node): SuperDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 261 =>
val v51 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v52, v53) = v51
assert(v52.id == 49)
val v54 = matchTypeName(v53)
val v55 = body.asInstanceOf[SequenceNode].children(4)
val v60 = unrollOptional(v55, 41, 263) map { n =>
val BindNode(v56, v57) = n
assert(v56.id == 263)
val BindNode(v58, v59) = v57
assert(v58.id == 264)
v59
}
val v65 = v60 map { n =>
val v61 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v62, v63) = v61
assert(v62.id == 265)
val v64 = matchSubTypes(v63)
v64
}
val v66 = SuperDef(node,v54,v65)
v66
  }
}
def matchTypeName(node: Node): TypeName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 30 =>
val v67 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v68, v69) = v67
assert(v68.id == 31)
val v70 = matchId(v69)
val v71 = TypeName(node,v70)
v71
  }
}
def matchClassParams(node: Node): List[ClassParam] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 252 =>
val v72 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v73, v74) = v72
assert(v73.id == 253)
val v75 = matchClassParam(v74)
val v76 = List(v75)
val v77 = body.asInstanceOf[SequenceNode].children(1)
val v82 = unrollRepeat0(v77) map { n =>
val BindNode(v78, v79) = n
assert(v78.id == 257)
val BindNode(v80, v81) = v79
assert(v80.id == 258)
v81
}
val v87 = v82 map { n =>
val v83 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v84, v85) = v83
assert(v84.id == 253)
val v86 = matchClassParam(v85)
v86
}
val v88 = v76 ++ v87
v88
  }
}
def matchClassParam(node: Node): ClassParam = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 254 =>
val v89 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v90, v91) = v89
assert(v90.id == 135)
val v92 = matchParamName(v91)
val v93 = body.asInstanceOf[SequenceNode].children(1)
val v98 = unrollOptional(v93, 41, 42) map { n =>
val BindNode(v94, v95) = n
assert(v94.id == 42)
val BindNode(v96, v97) = v95
assert(v96.id == 43)
v97
}
val v103 = v98 map { n =>
val v99 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v100, v101) = v99
assert(v100.id == 45)
val v102 = matchTypeDesc(v101)
v102
}
val v104 = ClassParam(node,v92,v103)
v104
  }
}
def matchParamName(node: Node): ParamName = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 30 =>
val v105 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v106, v107) = v105
assert(v106.id == 31)
val v108 = matchId(v107)
val v109 = ParamName(node,v108)
v109
  }
}
def matchTypeDesc(node: Node): TypeDesc = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 46 =>
val v110 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v111, v112) = v110
assert(v111.id == 47)
val v113 = matchValueTypeDesc(v112)
val v114 = body.asInstanceOf[SequenceNode].children(1)
val v119 = unrollOptional(v114, 41, 70) map { n =>
val BindNode(v115, v116) = n
assert(v115.id == 70)
val BindNode(v117, v118) = v116
assert(v117.id == 71)
v118
}
val v120 = TypeDesc(node,v113,v119)
v120
  }
}
def matchValueTypeDesc(node: Node): ValueTypeDesc = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 48 =>
val v121 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v122, v123) = v121
assert(v122.id == 49)
val v124 = matchTypeName(v123)
v124
case 50 =>
val v125 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v126, v127) = v125
assert(v126.id == 51)
val v128 = matchOnTheFlyTypeDef(v127)
v128
case 66 =>
val v129 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v130, v131) = v129
assert(v130.id == 45)
val v132 = matchTypeDesc(v131)
val v133 = ArrayTypeDesc(node,v132)
v133
  }
}
def matchSubTypes(node: Node): List[SubType] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 266 =>
val v134 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v135, v136) = v134
assert(v135.id == 267)
val v137 = matchSubType(v136)
val v138 = List(v137)
val v139 = body.asInstanceOf[SequenceNode].children(1)
val v144 = unrollRepeat0(v139) map { n =>
val BindNode(v140, v141) = n
assert(v140.id == 272)
val BindNode(v142, v143) = v141
assert(v142.id == 273)
v143
}
val v149 = v144 map { n =>
val v145 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v146, v147) = v145
assert(v146.id == 267)
val v148 = matchSubType(v147)
v148
}
val v150 = v138 ++ v149
v150
  }
}
def matchSubType(node: Node): SubType = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 48 =>
val v151 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v152, v153) = v151
assert(v152.id == 49)
val v154 = matchTypeName(v153)
v154
case 268 =>
val v155 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v156, v157) = v155
assert(v156.id == 246)
val v158 = matchClassDef(v157)
v158
case 269 =>
val v159 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v160, v161) = v159
assert(v160.id == 260)
val v162 = matchSuperDef(v161)
v162
  }
}
def matchOnTheFlyTypeDef(node: Node): OnTheFlyTypeDef = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 52 =>
val v163 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v164, v165) = v163
assert(v164.id == 49)
val v166 = matchTypeName(v165)
val v167 = body.asInstanceOf[SequenceNode].children(3)
val v172 = unrollOptional(v167, 41, 55) map { n =>
val BindNode(v168, v169) = n
assert(v168.id == 55)
val BindNode(v170, v171) = v169
assert(v170.id == 56)
v171
}
val v177 = v172 map { n =>
val v173 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v174, v175) = v173
assert(v174.id == 57)
val v176 = matchOnTheFlySuperTypes(v175)
v176
}
val v178 = OnTheFlyTypeDef(node,v166,v177)
v178
  }
}
def matchOnTheFlySuperTypes(node: Node): List[TypeName] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 58 =>
val v179 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v180, v181) = v179
assert(v180.id == 49)
val v182 = matchTypeName(v181)
val v183 = List(v182)
val v184 = body.asInstanceOf[SequenceNode].children(3)
val v189 = unrollRepeat0(v184) map { n =>
val BindNode(v185, v186) = n
assert(v185.id == 62)
val BindNode(v187, v188) = v186
assert(v187.id == 63)
v188
}
val v194 = v189 map { n =>
val v190 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v191, v192) = v190
assert(v191.id == 49)
val v193 = matchTypeName(v192)
v193
}
val v195 = v183 ++ v194
v195
  }
}
def matchRule(node: Node): Rule = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 26 =>
val v196 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v197, v198) = v196
assert(v197.id == 27)
val v199 = matchLHS(v198)
val v200 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v201, v202) = v200
assert(v201.id == 74)
val v203 = matchRHSs(v202)
val v204 = Rule(node,v199,v203)
v204
  }
}
def matchLHS(node: Node): LHS = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 28 =>
val v205 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v206, v207) = v205
assert(v206.id == 29)
val v208 = matchNonterminal(v207)
val v209 = body.asInstanceOf[SequenceNode].children(1)
val v214 = unrollOptional(v209, 41, 42) map { n =>
val BindNode(v210, v211) = n
assert(v210.id == 42)
val BindNode(v212, v213) = v211
assert(v212.id == 43)
v213
}
val v219 = v214 map { n =>
val v215 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v216, v217) = v215
assert(v216.id == 45)
val v218 = matchTypeDesc(v217)
v218
}
val v220 = LHS(node,v208,v219)
v220
  }
}
def matchRHSs(node: Node): List[RHS] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 75 =>
val v221 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v222, v223) = v221
assert(v222.id == 76)
val v224 = matchRHS(v223)
val v225 = List(v224)
val v226 = body.asInstanceOf[SequenceNode].children(1)
val v231 = unrollRepeat0(v226) map { n =>
val BindNode(v227, v228) = n
assert(v227.id == 241)
val BindNode(v229, v230) = v228
assert(v229.id == 242)
v230
}
val v236 = v231 map { n =>
val v232 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v233, v234) = v232
assert(v233.id == 76)
val v235 = matchRHS(v234)
v235
}
val v237 = v225 ++ v236
v237
  }
}
def matchRHS(node: Node): RHS = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 77 =>
val v238 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v239, v240) = v238
assert(v239.id == 78)
val v241 = matchElem(v240)
val v242 = List(v241)
val v243 = body.asInstanceOf[SequenceNode].children(1)
val v248 = unrollRepeat0(v243) map { n =>
val BindNode(v244, v245) = n
assert(v244.id == 237)
val BindNode(v246, v247) = v245
assert(v246.id == 238)
v247
}
val v253 = v248 map { n =>
val v249 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v250, v251) = v249
assert(v250.id == 78)
val v252 = matchElem(v251)
v252
}
val v254 = v242 ++ v253
val v255 = RHS(node,v254)
v255
  }
}
def matchElem(node: Node): Elem = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 79 =>
val v256 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v257, v258) = v256
assert(v257.id == 80)
val v259 = matchProcessor(v258)
v259
case 146 =>
val v260 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v261, v262) = v260
assert(v261.id == 147)
val v263 = matchSymbol(v262)
v263
  }
}
def matchProcessor(node: Node): Processor = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 81 =>
val v264 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v265, v266) = v264
assert(v265.id == 82)
val v267 = matchRef(v266)
v267
case 96 =>
val v268 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v269, v270) = v268
assert(v269.id == 98)
val v271 = matchPExpr(v270)
v271
  }
}
def matchPExpr(node: Node): PExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 99 =>
val v272 = body.asInstanceOf[SequenceNode].children(2)
val v273 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v274, v275) = v273
assert(v274.id == 98)
val v276 = matchPExpr(v275)
val v277 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v278, v279) = v277
assert(v278.id == 106)
val v280 = matchPTerm(v279)
val v281 = BinOpExpr(node,v272,v276,v280)
v281
case 144 =>
val v282 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v283, v284) = v282
assert(v283.id == 106)
val v285 = matchPTerm(v284)
v285
  }
}
def matchBinOp(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 102 =>
val v286 = body.asInstanceOf[SequenceNode].children(0)
v286
  }
}
def matchPTerm(node: Node): PTerm = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 81 =>
val v287 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v288, v289) = v287
assert(v288.id == 82)
val v290 = matchRef(v289)
v290
case 107 =>
val v291 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v292, v293) = v291
assert(v292.id == 108)
val v294 = matchBoundPExpr(v293)
v294
case 111 =>
val v295 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v296, v297) = v295
assert(v296.id == 112)
val v298 = matchConstructExpr(v297)
v298
case 140 =>
val v299 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v300, v301) = v299
assert(v300.id == 98)
val v302 = matchPExpr(v301)
val v303 = PTermParen(node,v302)
v303
case 141 =>
val v304 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v305, v306) = v304
assert(v305.id == 142)
val v307 = matchArrayTerm(v306)
v307
  }
}
def matchRef(node: Node): Ref = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 83 =>
val v308 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v309, v310) = v308
assert(v309.id == 85)
val v311 = matchRefIdx(v310)
val v312 = Ref(node,v311)
v312
  }
}
def matchArrayTerm(node: Node): PTermSeq = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 143 =>
val v313 = body.asInstanceOf[SequenceNode].children(2)
val v318 = unrollOptional(v313, 41, 118) map { n =>
val BindNode(v314, v315) = n
assert(v314.id == 118)
val BindNode(v316, v317) = v315
assert(v316.id == 119)
v317
}
val v336 = v318 map { n =>
val v319 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v320, v321) = v319
assert(v320.id == 98)
val v322 = matchPExpr(v321)
val v323 = List(v322)
val v324 = n.asInstanceOf[SequenceNode].children(1)
val v329 = unrollRepeat0(v324) map { n =>
val BindNode(v325, v326) = n
assert(v325.id == 122)
val BindNode(v327, v328) = v326
assert(v327.id == 123)
v328
}
val v334 = v329 map { n =>
val v330 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v331, v332) = v330
assert(v331.id == 98)
val v333 = matchPExpr(v332)
v333
}
val v335 = v323 ++ v334
v335
}
val v337 = PTermSeq(node,v336)
v337
  }
}
def matchBoundPExpr(node: Node): BoundPExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 109 =>
val v338 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v339, v340) = v338
assert(v339.id == 82)
val v341 = matchRef(v340)
val v342 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v343, v344) = v342
assert(v343.id == 110)
val v345 = matchBoundedPExpr(v344)
val v346 = BoundPExpr(node,v341,v345)
v346
  }
}
def matchBoundedPExpr(node: Node): PExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 81 =>
val v347 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v348, v349) = v347
assert(v348.id == 82)
val v350 = matchRef(v349)
v350
case 107 =>
val v351 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v352, v353) = v351
assert(v352.id == 108)
val v354 = matchBoundPExpr(v353)
v354
case 96 =>
val v355 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v356, v357) = v355
assert(v356.id == 98)
val v358 = matchPExpr(v357)
v358
  }
}
def matchConstructExpr(node: Node): AbstractConstructExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 113 =>
val v359 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v360, v361) = v359
assert(v360.id == 49)
val v362 = matchTypeName(v361)
val v363 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v364, v365) = v363
assert(v364.id == 114)
val v366 = matchConstructParams(v365)
val v367 = ConstructExpr(node,v362,v366)
v367
case 125 =>
val v368 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v369, v370) = v368
assert(v369.id == 126)
val v371 = matchOnTheFlyTypeDefConstructExpr(v370)
v371
  }
}
def matchConstructParams(node: Node): Option[List[PExpr]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 115 =>
val v372 = body.asInstanceOf[SequenceNode].children(2)
val v377 = unrollOptional(v372, 41, 118) map { n =>
val BindNode(v373, v374) = n
assert(v373.id == 118)
val BindNode(v375, v376) = v374
assert(v375.id == 119)
v376
}
val v395 = v377 map { n =>
val v378 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v379, v380) = v378
assert(v379.id == 98)
val v381 = matchPExpr(v380)
val v382 = List(v381)
val v383 = n.asInstanceOf[SequenceNode].children(1)
val v388 = unrollRepeat0(v383) map { n =>
val BindNode(v384, v385) = n
assert(v384.id == 122)
val BindNode(v386, v387) = v385
assert(v386.id == 123)
v387
}
val v393 = v388 map { n =>
val v389 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v390, v391) = v389
assert(v390.id == 98)
val v392 = matchPExpr(v391)
v392
}
val v394 = v382 ++ v393
v394
}
v395
  }
}
def matchOnTheFlyTypeDefConstructExpr(node: Node): OnTheFlyTypeDefConstructExpr = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 127 =>
val v396 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v397, v398) = v396
assert(v397.id == 51)
val v399 = matchOnTheFlyTypeDef(v398)
val v400 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v401, v402) = v400
assert(v401.id == 128)
val v403 = matchNamedParams(v402)
val v404 = OnTheFlyTypeDefConstructExpr(node,v399,v403)
v404
  }
}
def matchNamedParams(node: Node): Option[List[NamedParam]] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 129 =>
val v405 = body.asInstanceOf[SequenceNode].children(2)
val v410 = unrollOptional(v405, 41, 131) map { n =>
val BindNode(v406, v407) = n
assert(v406.id == 131)
val BindNode(v408, v409) = v407
assert(v408.id == 132)
v409
}
val v428 = v410 map { n =>
val v411 = n.asInstanceOf[SequenceNode].children(0)
val BindNode(v412, v413) = v411
assert(v412.id == 133)
val v414 = matchNamedParam(v413)
val v415 = List(v414)
val v416 = n.asInstanceOf[SequenceNode].children(1)
val v421 = unrollRepeat0(v416) map { n =>
val BindNode(v417, v418) = n
assert(v417.id == 138)
val BindNode(v419, v420) = v418
assert(v419.id == 139)
v420
}
val v426 = v421 map { n =>
val v422 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v423, v424) = v422
assert(v423.id == 133)
val v425 = matchNamedParam(v424)
v425
}
val v427 = v415 ++ v426
v427
}
v428
  }
}
def matchNamedParam(node: Node): NamedParam = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 134 =>
val v429 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v430, v431) = v429
assert(v430.id == 135)
val v432 = matchParamName(v431)
val v433 = body.asInstanceOf[SequenceNode].children(1)
val v438 = unrollOptional(v433, 41, 42) map { n =>
val BindNode(v434, v435) = n
assert(v434.id == 42)
val BindNode(v436, v437) = v435
assert(v436.id == 43)
v437
}
val v443 = v438 map { n =>
val v439 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v440, v441) = v439
assert(v440.id == 45)
val v442 = matchTypeDesc(v441)
v442
}
val v444 = body.asInstanceOf[SequenceNode].children(5)
val BindNode(v445, v446) = v444
assert(v445.id == 98)
val v447 = matchPExpr(v446)
val v448 = NamedParam(node,v432,v443,v447)
v448
  }
}
def matchSymbol(node: Node): Symbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 148 =>
val v449 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v450, v451) = v449
assert(v450.id == 149)
val v452 = matchBinSymbol(v451)
v452
  }
}
def matchBinSymbol(node: Node): BinSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 150 =>
val v453 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v454, v455) = v453
assert(v454.id == 149)
val v456 = matchBinSymbol(v455)
val v457 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v458, v459) = v457
assert(v458.id == 152)
val v460 = matchPreUnSymbol(v459)
val v461 = JoinSymbol(node,v456,v460)
v461
case 233 =>
val v462 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v463, v464) = v462
assert(v463.id == 149)
val v465 = matchBinSymbol(v464)
val v466 = body.asInstanceOf[SequenceNode].children(4)
val BindNode(v467, v468) = v466
assert(v467.id == 152)
val v469 = matchPreUnSymbol(v468)
val v470 = ExceptSymbol(node,v465,v469)
v470
case 234 =>
val v471 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v472, v473) = v471
assert(v472.id == 152)
val v474 = matchPreUnSymbol(v473)
v474
  }
}
def matchPreUnSymbol(node: Node): PreUnSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 153 =>
val v475 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v476, v477) = v475
assert(v476.id == 152)
val v478 = matchPreUnSymbol(v477)
val v479 = FollowedBy(node,v478)
v479
case 155 =>
val v480 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v481, v482) = v480
assert(v481.id == 152)
val v483 = matchPreUnSymbol(v482)
val v484 = NotFollowedBy(node,v483)
v484
case 157 =>
val v485 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v486, v487) = v485
assert(v486.id == 158)
val v488 = matchPostUnSymbol(v487)
v488
  }
}
def matchPostUnSymbol(node: Node): PostUnSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 159 =>
val v489 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v490, v491) = v489
assert(v490.id == 158)
val v492 = matchPostUnSymbol(v491)
// UnrollChoices
val v493 = Repeat(node,v492,body)
v493
case 162 =>
val v494 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v495, v496) = v494
assert(v495.id == 163)
val v497 = matchAtomSymbol(v496)
v497
  }
}
def matchAtomSymbol(node: Node): AtomSymbol = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 164 =>
val v498 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v499, v500) = v498
assert(v499.id == 165)
val v501 = matchTerminal(v500)
v501
case 181 =>
val v502 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v503, v504) = v502
assert(v503.id == 182)
val v505 = matchTerminalChoice(v504)
v505
case 199 =>
val v506 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v507, v508) = v506
assert(v507.id == 200)
val v509 = matchStringLiteral(v508)
v509
case 211 =>
val v510 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v511, v512) = v510
assert(v511.id == 29)
val v513 = matchNonterminal(v512)
v513
case 212 =>
val v514 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v515, v516) = v514
assert(v515.id == 213)
val v517 = matchInPlaceChoices(v516)
val v518 = Paren(node,v517)
v518
case 226 =>
val v519 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v520, v521) = v519
assert(v520.id == 227)
val v522 = matchLongest(v521)
v522
case 229 =>
val v523 = EmptySeq(node)
v523
  }
}
def matchInPlaceChoices(node: Node): InPlaceChoices = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 214 =>
val v524 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v525, v526) = v524
assert(v525.id == 215)
val v527 = matchInPlaceSequence(v526)
val v528 = List(v527)
val v529 = body.asInstanceOf[SequenceNode].children(1)
val v534 = unrollRepeat0(v529) map { n =>
val BindNode(v530, v531) = n
assert(v530.id == 223)
val BindNode(v532, v533) = v531
assert(v532.id == 224)
v533
}
val v539 = v534 map { n =>
val v535 = n.asInstanceOf[SequenceNode].children(3)
val BindNode(v536, v537) = v535
assert(v536.id == 215)
val v538 = matchInPlaceSequence(v537)
v538
}
val v540 = v528 ++ v539
val v541 = InPlaceChoices(node,v540)
v541
  }
}
def matchInPlaceSequence(node: Node): InPlaceSequence = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 216 =>
val v542 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v543, v544) = v542
assert(v543.id == 147)
val v545 = matchSymbol(v544)
val v546 = List(v545)
val v547 = body.asInstanceOf[SequenceNode].children(1)
val v552 = unrollRepeat0(v547) map { n =>
val BindNode(v548, v549) = n
assert(v548.id == 219)
val BindNode(v550, v551) = v549
assert(v550.id == 220)
v551
}
val v557 = v552 map { n =>
val v553 = n.asInstanceOf[SequenceNode].children(1)
val BindNode(v554, v555) = v553
assert(v554.id == 147)
val v556 = matchSymbol(v555)
v556
}
val v558 = v546 ++ v557
val v559 = InPlaceSequence(node,v558)
v559
  }
}
def matchLongest(node: Node): Longest = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 228 =>
val v560 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v561, v562) = v560
assert(v561.id == 213)
val v563 = matchInPlaceChoices(v562)
val v564 = Longest(node,v563)
v564
  }
}
def matchEmptySequence(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 231 =>
val v565 = body.asInstanceOf[SequenceNode].children(0)
v565
  }
}
def matchNonterminal(node: Node): Nonterminal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 30 =>
val v566 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v567, v568) = v566
assert(v567.id == 31)
val v569 = matchId(v568)
val v570 = Nonterminal(node,v569)
v570
  }
}
def matchTerminal(node: Node): Terminal = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 166 =>
val v571 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v572, v573) = v571
assert(v572.id == 168)
val v574 = matchTerminalChar(v573)
v574
case 179 =>
val v575 = body.asInstanceOf[SequenceNode].children(0)
val v576 = AnyTerminal(node,v575)
v576
  }
}
def matchTerminalChoice(node: Node): TerminalChoice = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 183 =>
val v577 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v578, v579) = v577
assert(v578.id == 184)
val v580 = matchTerminalChoiceElem(v579)
val v581 = List(v580)
val v582 = body.asInstanceOf[SequenceNode].children(2)
val v586 = unrollRepeat1(v582) map { n =>
val BindNode(v583, v584) = n
assert(v583.id == 184)
val v585 = matchTerminalChoiceElem(v584)
v585
}
val v587 = v586 map { n =>
n
}
val v588 = v581 ++ v587
val v589 = TerminalChoice(node,v588)
v589
case 198 =>
val v590 = body.asInstanceOf[SequenceNode].children(1)
val BindNode(v591, v592) = v590
assert(v591.id == 193)
val v593 = matchTerminalChoiceRange(v592)
val v594 = List(v593)
val v595 = TerminalChoice(node,v594)
v595
  }
}
def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 185 =>
val v596 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v597, v598) = v596
assert(v597.id == 186)
val v599 = matchTerminalChoiceChar(v598)
v599
case 192 =>
val v600 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v601, v602) = v600
assert(v601.id == 193)
val v603 = matchTerminalChoiceRange(v602)
v603
  }
}
def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 194 =>
val v604 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v605, v606) = v604
assert(v605.id == 186)
val v607 = matchTerminalChoiceChar(v606)
val v608 = body.asInstanceOf[SequenceNode].children(2)
val BindNode(v609, v610) = v608
assert(v609.id == 186)
val v611 = matchTerminalChoiceChar(v610)
val v612 = TerminalChoiceRange(node,v607,v611)
v612
  }
}
def matchStringLiteral(node: Node): StringLiteral = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 201 =>
val v613 = body.asInstanceOf[SequenceNode].children(1)
val v617 = unrollRepeat0(v613) map { n =>
val BindNode(v614, v615) = n
assert(v614.id == 205)
val v616 = matchStringChar(v615)
v616
}
val v618 = v617 map { n =>
n
}
val v619 = StringLiteral(node,v618)
v619
  }
}
def matchUnicodeChar(node: Node): CharUnicode = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 176 =>
val v620 = body.asInstanceOf[SequenceNode].children(2)
val v621 = body.asInstanceOf[SequenceNode].children(3)
val v622 = body.asInstanceOf[SequenceNode].children(4)
val v623 = body.asInstanceOf[SequenceNode].children(5)
val v624 = List(v620,v621,v622,v623)
val v625 = CharUnicode(node,v624)
v625
  }
}
def matchTerminalChar(node: Node): TerminalChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 169 =>
val v626 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v627, v628) = v626
assert(v627.id == 170)
val v629 = CharAsIs(node,v628)
v629
case 172 =>
val v630 = body.asInstanceOf[SequenceNode].children(1)
val v631 = CharEscaped(node,v630)
v631
case 174 =>
val v632 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v633, v634) = v632
assert(v633.id == 175)
val v635 = matchUnicodeChar(v634)
v635
  }
}
def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 187 =>
val v636 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v637, v638) = v636
assert(v637.id == 188)
val v639 = CharAsIs(node,v638)
v639
case 190 =>
val v640 = body.asInstanceOf[SequenceNode].children(1)
val v641 = CharEscaped(node,v640)
v641
case 174 =>
val v642 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v643, v644) = v642
assert(v643.id == 175)
val v645 = matchUnicodeChar(v644)
v645
  }
}
def matchStringChar(node: Node): StringChar = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 206 =>
val v646 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v647, v648) = v646
assert(v647.id == 207)
val v649 = CharAsIs(node,v648)
v649
case 209 =>
val v650 = body.asInstanceOf[SequenceNode].children(1)
val v651 = CharEscaped(node,v650)
v651
case 174 =>
val v652 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v653, v654) = v652
assert(v653.id == 175)
val v655 = matchUnicodeChar(v654)
v655
  }
}
def matchRefIdx(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 86 =>
val v656 = body.asInstanceOf[SequenceNode].children(0)
v656
  }
}
def matchId(node: Node): Node = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 32 =>
val v657 = body.asInstanceOf[SequenceNode].children(0)
v657
  }
}
def matchWS(node: Node): List[Node] = {
  val BindNode(symbol, body) = node
  symbol.id match {
    case 5 =>
val v658 = body.asInstanceOf[SequenceNode].children(0)
val v659 = unrollRepeat0(v658) map { n =>
// UnrollChoices
n
}
v659
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
val v660 = body.asInstanceOf[SequenceNode].children(0)
val BindNode(v661, v662) = v660
assert(v661.id == 22)
v662
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
        case None => ???
      }
    case Right(error) => Right(error)
  }
}