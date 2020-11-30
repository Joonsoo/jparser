package com.giyeok.jparser.metalang3a.generated

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseResultTree.BindNode
import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.ParseResultTree.SequenceNode
import com.giyeok.jparser.ParseResultTree.TerminalNode
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParseTreeUtil
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat0
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat1
import com.giyeok.jparser.nparser.Parser
import scala.collection.immutable.ListSet

object MetaLang3 {
  val ngrammar = new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
      2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("Grammar"), Set(3)),
      4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("WS"), Set(5)),
      6 -> NGrammar.NRepeat(6, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), 7, 8),
      9 -> NGrammar.NOneOf(9, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), Set(10, 13)),
      10 -> NGrammar.NProxy(10, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)))), 11),
      12 -> NGrammar.NTerminal(12, Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)),
      13 -> NGrammar.NProxy(13, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))), 14),
      15 -> NGrammar.NNonterminal(15, Symbols.Nonterminal("LineComment"), Set(16)),
      17 -> NGrammar.NTerminal(17, Symbols.ExactChar('/')),
      18 -> NGrammar.NRepeat(18, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), 0), 7, 19),
      20 -> NGrammar.NOneOf(20, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), Set(21)),
      21 -> NGrammar.NProxy(21, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))), 22),
      23 -> NGrammar.NExcept(23, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 24, 25),
      24 -> NGrammar.NTerminal(24, Symbols.AnyChar),
      25 -> NGrammar.NTerminal(25, Symbols.ExactChar('\n')),
      26 -> NGrammar.NOneOf(26, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("EOF")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\n')))))), Set(27, 32)),
      27 -> NGrammar.NProxy(27, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("EOF")))), 28),
      29 -> NGrammar.NNonterminal(29, Symbols.Nonterminal("EOF"), Set(30)),
      31 -> NGrammar.NLookaheadExcept(31, Symbols.LookaheadExcept(Symbols.AnyChar), 7, 24),
      32 -> NGrammar.NProxy(32, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\n')))), 33),
      34 -> NGrammar.NNonterminal(34, Symbols.Nonterminal("Def"), Set(35, 118)),
      36 -> NGrammar.NNonterminal(36, Symbols.Nonterminal("Rule"), Set(37)),
      38 -> NGrammar.NNonterminal(38, Symbols.Nonterminal("LHS"), Set(39)),
      40 -> NGrammar.NNonterminal(40, Symbols.Nonterminal("Nonterminal"), Set(41)),
      42 -> NGrammar.NNonterminal(42, Symbols.Nonterminal("NonterminalName"), Set(43, 91)),
      44 -> NGrammar.NNonterminal(44, Symbols.Nonterminal("IdNoKeyword"), Set(45)),
      46 -> NGrammar.NExcept(46, Symbols.Except(Symbols.Nonterminal("Id"), Symbols.Nonterminal("Keyword")), 47, 57),
      47 -> NGrammar.NNonterminal(47, Symbols.Nonterminal("Id"), Set(48)),
      49 -> NGrammar.NLongest(49, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))), 50),
      50 -> NGrammar.NOneOf(50, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))))), Set(51)),
      51 -> NGrammar.NProxy(51, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))), 52),
      53 -> NGrammar.NTerminal(53, Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
      54 -> NGrammar.NRepeat(54, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), 7, 55),
      56 -> NGrammar.NTerminal(56, Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
      57 -> NGrammar.NNonterminal(57, Symbols.Nonterminal("Keyword"), Set(58, 67, 73, 80, 84, 88)),
      59 -> NGrammar.NProxy(59, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('n')))), 60),
      61 -> NGrammar.NTerminal(61, Symbols.ExactChar('b')),
      62 -> NGrammar.NTerminal(62, Symbols.ExactChar('o')),
      63 -> NGrammar.NTerminal(63, Symbols.ExactChar('l')),
      64 -> NGrammar.NTerminal(64, Symbols.ExactChar('e')),
      65 -> NGrammar.NTerminal(65, Symbols.ExactChar('a')),
      66 -> NGrammar.NTerminal(66, Symbols.ExactChar('n')),
      68 -> NGrammar.NProxy(68, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('c'), Symbols.ExactChar('h'), Symbols.ExactChar('a'), Symbols.ExactChar('r')))), 69),
      70 -> NGrammar.NTerminal(70, Symbols.ExactChar('c')),
      71 -> NGrammar.NTerminal(71, Symbols.ExactChar('h')),
      72 -> NGrammar.NTerminal(72, Symbols.ExactChar('r')),
      74 -> NGrammar.NProxy(74, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g')))), 75),
      76 -> NGrammar.NTerminal(76, Symbols.ExactChar('s')),
      77 -> NGrammar.NTerminal(77, Symbols.ExactChar('t')),
      78 -> NGrammar.NTerminal(78, Symbols.ExactChar('i')),
      79 -> NGrammar.NTerminal(79, Symbols.ExactChar('g')),
      81 -> NGrammar.NProxy(81, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e')))), 82),
      83 -> NGrammar.NTerminal(83, Symbols.ExactChar('u')),
      85 -> NGrammar.NProxy(85, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e')))), 86),
      87 -> NGrammar.NTerminal(87, Symbols.ExactChar('f')),
      89 -> NGrammar.NProxy(89, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('l'), Symbols.ExactChar('l')))), 90),
      92 -> NGrammar.NTerminal(92, Symbols.ExactChar('`')),
      93 -> NGrammar.NOneOf(93, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(94, 136)),
      94 -> NGrammar.NOneOf(94, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Set(95)),
      95 -> NGrammar.NProxy(95, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))), 96),
      97 -> NGrammar.NTerminal(97, Symbols.ExactChar(':')),
      98 -> NGrammar.NNonterminal(98, Symbols.Nonterminal("TypeDesc"), Set(99)),
      100 -> NGrammar.NNonterminal(100, Symbols.Nonterminal("NonNullTypeDesc"), Set(101, 103, 106, 108, 114, 118)),
      102 -> NGrammar.NNonterminal(102, Symbols.Nonterminal("TypeName"), Set(43, 91)),
      104 -> NGrammar.NTerminal(104, Symbols.ExactChar('[')),
      105 -> NGrammar.NTerminal(105, Symbols.ExactChar(']')),
      107 -> NGrammar.NNonterminal(107, Symbols.Nonterminal("ValueType"), Set(58, 67, 73)),
      109 -> NGrammar.NNonterminal(109, Symbols.Nonterminal("AnyType"), Set(110)),
      111 -> NGrammar.NProxy(111, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'), Symbols.ExactChar('n'), Symbols.ExactChar('y')))), 112),
      113 -> NGrammar.NTerminal(113, Symbols.ExactChar('y')),
      115 -> NGrammar.NNonterminal(115, Symbols.Nonterminal("EnumTypeName"), Set(116)),
      117 -> NGrammar.NTerminal(117, Symbols.ExactChar('%')),
      119 -> NGrammar.NNonterminal(119, Symbols.Nonterminal("TypeDef"), Set(120, 156, 177)),
      121 -> NGrammar.NNonterminal(121, Symbols.Nonterminal("ClassDef"), Set(122, 138, 155)),
      123 -> NGrammar.NNonterminal(123, Symbols.Nonterminal("SuperTypes"), Set(124)),
      125 -> NGrammar.NTerminal(125, Symbols.ExactChar('<')),
      126 -> NGrammar.NOneOf(126, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(127, 136)),
      127 -> NGrammar.NOneOf(127, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))))), Set(128)),
      128 -> NGrammar.NProxy(128, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))), 129),
      130 -> NGrammar.NRepeat(130, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), 7, 131),
      132 -> NGrammar.NOneOf(132, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), Set(133)),
      133 -> NGrammar.NProxy(133, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))), 134),
      135 -> NGrammar.NTerminal(135, Symbols.ExactChar(',')),
      136 -> NGrammar.NProxy(136, Symbols.Proxy(Symbols.Sequence(Seq())), 7),
      137 -> NGrammar.NTerminal(137, Symbols.ExactChar('>')),
      139 -> NGrammar.NNonterminal(139, Symbols.Nonterminal("ClassParamsDef"), Set(140)),
      141 -> NGrammar.NTerminal(141, Symbols.ExactChar('(')),
      142 -> NGrammar.NOneOf(142, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(143, 136)),
      143 -> NGrammar.NOneOf(143, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))))), Set(144)),
      144 -> NGrammar.NProxy(144, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))), 145),
      146 -> NGrammar.NNonterminal(146, Symbols.Nonterminal("ClassParamDef"), Set(147)),
      148 -> NGrammar.NNonterminal(148, Symbols.Nonterminal("ParamName"), Set(43, 91)),
      149 -> NGrammar.NRepeat(149, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), 7, 150),
      151 -> NGrammar.NOneOf(151, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), Set(152)),
      152 -> NGrammar.NProxy(152, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))), 153),
      154 -> NGrammar.NTerminal(154, Symbols.ExactChar(')')),
      157 -> NGrammar.NNonterminal(157, Symbols.Nonterminal("SuperDef"), Set(158)),
      159 -> NGrammar.NOneOf(159, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(160, 136)),
      160 -> NGrammar.NOneOf(160, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Set(161)),
      161 -> NGrammar.NProxy(161, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))), 162),
      163 -> NGrammar.NTerminal(163, Symbols.ExactChar('{')),
      164 -> NGrammar.NOneOf(164, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(165, 136)),
      165 -> NGrammar.NOneOf(165, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))))), Set(166)),
      166 -> NGrammar.NProxy(166, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))), 167),
      168 -> NGrammar.NNonterminal(168, Symbols.Nonterminal("SubTypes"), Set(169)),
      170 -> NGrammar.NNonterminal(170, Symbols.Nonterminal("SubType"), Set(101, 120, 156)),
      171 -> NGrammar.NRepeat(171, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), 0), 7, 172),
      173 -> NGrammar.NOneOf(173, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), Set(174)),
      174 -> NGrammar.NProxy(174, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))), 175),
      176 -> NGrammar.NTerminal(176, Symbols.ExactChar('}')),
      178 -> NGrammar.NNonterminal(178, Symbols.Nonterminal("EnumTypeDef"), Set(179)),
      180 -> NGrammar.NOneOf(180, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0)))))), Set(181)),
      181 -> NGrammar.NProxy(181, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0)))), 182),
      183 -> NGrammar.NRepeat(183, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0), 7, 184),
      185 -> NGrammar.NOneOf(185, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), Set(186)),
      186 -> NGrammar.NProxy(186, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))), 187),
      188 -> NGrammar.NOneOf(188, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(189, 136)),
      189 -> NGrammar.NOneOf(189, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))))), Set(190)),
      190 -> NGrammar.NProxy(190, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))), 191),
      192 -> NGrammar.NTerminal(192, Symbols.ExactChar('?')),
      193 -> NGrammar.NTerminal(193, Symbols.ExactChar('=')),
      194 -> NGrammar.NOneOf(194, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0)))))), Set(195)),
      195 -> NGrammar.NProxy(195, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0)))), 196),
      197 -> NGrammar.NNonterminal(197, Symbols.Nonterminal("RHS"), Set(198)),
      199 -> NGrammar.NNonterminal(199, Symbols.Nonterminal("Sequence"), Set(200)),
      201 -> NGrammar.NNonterminal(201, Symbols.Nonterminal("Elem"), Set(202, 287)),
      203 -> NGrammar.NNonterminal(203, Symbols.Nonterminal("Symbol"), Set(204)),
      205 -> NGrammar.NNonterminal(205, Symbols.Nonterminal("BinSymbol"), Set(206, 285, 286)),
      207 -> NGrammar.NTerminal(207, Symbols.ExactChar('&')),
      208 -> NGrammar.NNonterminal(208, Symbols.Nonterminal("PreUnSymbol"), Set(209, 211, 213)),
      210 -> NGrammar.NTerminal(210, Symbols.ExactChar('^')),
      212 -> NGrammar.NTerminal(212, Symbols.ExactChar('!')),
      214 -> NGrammar.NNonterminal(214, Symbols.Nonterminal("PostUnSymbol"), Set(215, 216, 218, 220)),
      217 -> NGrammar.NTerminal(217, Symbols.ExactChar('*')),
      219 -> NGrammar.NTerminal(219, Symbols.ExactChar('+')),
      221 -> NGrammar.NNonterminal(221, Symbols.Nonterminal("AtomSymbol"), Set(222, 238, 256, 268, 269, 278, 281)),
      223 -> NGrammar.NNonterminal(223, Symbols.Nonterminal("Terminal"), Set(224, 236)),
      225 -> NGrammar.NTerminal(225, Symbols.ExactChar('\'')),
      226 -> NGrammar.NNonterminal(226, Symbols.Nonterminal("TerminalChar"), Set(227, 230, 232)),
      228 -> NGrammar.NExcept(228, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')), 24, 229),
      229 -> NGrammar.NTerminal(229, Symbols.ExactChar('\\')),
      231 -> NGrammar.NTerminal(231, Symbols.Chars(Set('\'', '\\', 'b', 'n', 'r', 't'))),
      233 -> NGrammar.NNonterminal(233, Symbols.Nonterminal("UnicodeChar"), Set(234)),
      235 -> NGrammar.NTerminal(235, Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet)),
      237 -> NGrammar.NTerminal(237, Symbols.ExactChar('.')),
      239 -> NGrammar.NNonterminal(239, Symbols.Nonterminal("TerminalChoice"), Set(240, 255)),
      241 -> NGrammar.NNonterminal(241, Symbols.Nonterminal("TerminalChoiceElem"), Set(242, 249)),
      243 -> NGrammar.NNonterminal(243, Symbols.Nonterminal("TerminalChoiceChar"), Set(244, 247, 232)),
      245 -> NGrammar.NExcept(245, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'', '-', '\\'))), 24, 246),
      246 -> NGrammar.NTerminal(246, Symbols.Chars(Set('\'', '-', '\\'))),
      248 -> NGrammar.NTerminal(248, Symbols.Chars(Set('\'', '-', '\\', 'b', 'n', 'r', 't'))),
      250 -> NGrammar.NNonterminal(250, Symbols.Nonterminal("TerminalChoiceRange"), Set(251)),
      252 -> NGrammar.NTerminal(252, Symbols.ExactChar('-')),
      253 -> NGrammar.NRepeat(253, Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), 241, 254),
      257 -> NGrammar.NNonterminal(257, Symbols.Nonterminal("StringSymbol"), Set(258)),
      259 -> NGrammar.NTerminal(259, Symbols.ExactChar('"')),
      260 -> NGrammar.NRepeat(260, Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), 7, 261),
      262 -> NGrammar.NNonterminal(262, Symbols.Nonterminal("StringChar"), Set(263, 266, 232)),
      264 -> NGrammar.NExcept(264, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"', '\\'))), 24, 265),
      265 -> NGrammar.NTerminal(265, Symbols.Chars(Set('"', '\\'))),
      267 -> NGrammar.NTerminal(267, Symbols.Chars(Set('"', '\\', 'b', 'n', 'r', 't'))),
      270 -> NGrammar.NNonterminal(270, Symbols.Nonterminal("InPlaceChoices"), Set(271)),
      272 -> NGrammar.NRepeat(272, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), 0), 7, 273),
      274 -> NGrammar.NOneOf(274, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), Set(275)),
      275 -> NGrammar.NProxy(275, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))), 276),
      277 -> NGrammar.NTerminal(277, Symbols.ExactChar('|')),
      279 -> NGrammar.NNonterminal(279, Symbols.Nonterminal("Longest"), Set(280)),
      282 -> NGrammar.NNonterminal(282, Symbols.Nonterminal("EmptySequence"), Set(283)),
      284 -> NGrammar.NTerminal(284, Symbols.ExactChar('#')),
      288 -> NGrammar.NNonterminal(288, Symbols.Nonterminal("Processor"), Set(289, 323)),
      290 -> NGrammar.NNonterminal(290, Symbols.Nonterminal("Ref"), Set(291, 318)),
      292 -> NGrammar.NNonterminal(292, Symbols.Nonterminal("ValRef"), Set(293)),
      294 -> NGrammar.NTerminal(294, Symbols.ExactChar('$')),
      295 -> NGrammar.NOneOf(295, Symbols.OneOf(ListSet(Symbols.Nonterminal("CondSymPath"), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(296, 136)),
      296 -> NGrammar.NNonterminal(296, Symbols.Nonterminal("CondSymPath"), Set(297)),
      298 -> NGrammar.NRepeat(298, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), 1), 299, 304),
      299 -> NGrammar.NOneOf(299, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), Set(300, 302)),
      300 -> NGrammar.NProxy(300, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), 301),
      302 -> NGrammar.NProxy(302, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))), 303),
      305 -> NGrammar.NNonterminal(305, Symbols.Nonterminal("RefIdx"), Set(306)),
      307 -> NGrammar.NLongest(307, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))), 308),
      308 -> NGrammar.NOneOf(308, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))))), Set(309, 312)),
      309 -> NGrammar.NProxy(309, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), 310),
      311 -> NGrammar.NTerminal(311, Symbols.ExactChar('0')),
      312 -> NGrammar.NProxy(312, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))), 313),
      314 -> NGrammar.NTerminal(314, Symbols.Chars(('1' to '9').toSet)),
      315 -> NGrammar.NRepeat(315, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 7, 316),
      317 -> NGrammar.NTerminal(317, Symbols.Chars(('0' to '9').toSet)),
      319 -> NGrammar.NNonterminal(319, Symbols.Nonterminal("RawRef"), Set(320)),
      321 -> NGrammar.NProxy(321, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$')))), 322),
      324 -> NGrammar.NNonterminal(324, Symbols.Nonterminal("PExpr"), Set(325)),
      326 -> NGrammar.NNonterminal(326, Symbols.Nonterminal("TernaryExpr"), Set(327, 436)),
      328 -> NGrammar.NNonterminal(328, Symbols.Nonterminal("BoolOrExpr"), Set(329, 431)),
      330 -> NGrammar.NNonterminal(330, Symbols.Nonterminal("BoolAndExpr"), Set(331, 428)),
      332 -> NGrammar.NNonterminal(332, Symbols.Nonterminal("BoolEqExpr"), Set(333, 425)),
      334 -> NGrammar.NNonterminal(334, Symbols.Nonterminal("ElvisExpr"), Set(335, 415)),
      336 -> NGrammar.NNonterminal(336, Symbols.Nonterminal("AdditiveExpr"), Set(337, 412)),
      338 -> NGrammar.NNonterminal(338, Symbols.Nonterminal("PrefixNotExpr"), Set(339, 340)),
      341 -> NGrammar.NNonterminal(341, Symbols.Nonterminal("Atom"), Set(289, 342, 346, 361, 376, 379, 393, 408)),
      343 -> NGrammar.NNonterminal(343, Symbols.Nonterminal("BindExpr"), Set(344)),
      345 -> NGrammar.NNonterminal(345, Symbols.Nonterminal("BinderExpr"), Set(289, 342, 323)),
      347 -> NGrammar.NNonterminal(347, Symbols.Nonterminal("NamedConstructExpr"), Set(348)),
      349 -> NGrammar.NNonterminal(349, Symbols.Nonterminal("NamedConstructParams"), Set(350)),
      351 -> NGrammar.NOneOf(351, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))))), Set(352)),
      352 -> NGrammar.NProxy(352, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))), 353),
      354 -> NGrammar.NNonterminal(354, Symbols.Nonterminal("NamedParam"), Set(355)),
      356 -> NGrammar.NRepeat(356, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), 7, 357),
      358 -> NGrammar.NOneOf(358, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), Set(359)),
      359 -> NGrammar.NProxy(359, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))), 360),
      362 -> NGrammar.NNonterminal(362, Symbols.Nonterminal("FuncCallOrConstructExpr"), Set(363)),
      364 -> NGrammar.NNonterminal(364, Symbols.Nonterminal("TypeOrFuncName"), Set(43, 91)),
      365 -> NGrammar.NNonterminal(365, Symbols.Nonterminal("CallParams"), Set(366)),
      367 -> NGrammar.NOneOf(367, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(368, 136)),
      368 -> NGrammar.NOneOf(368, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Set(369)),
      369 -> NGrammar.NProxy(369, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))), 370),
      371 -> NGrammar.NRepeat(371, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), 7, 372),
      373 -> NGrammar.NOneOf(373, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), Set(374)),
      374 -> NGrammar.NProxy(374, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 375),
      377 -> NGrammar.NNonterminal(377, Symbols.Nonterminal("ArrayExpr"), Set(378)),
      380 -> NGrammar.NNonterminal(380, Symbols.Nonterminal("Literal"), Set(88, 381, 385, 388)),
      382 -> NGrammar.NOneOf(382, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))))), Set(383, 384)),
      383 -> NGrammar.NProxy(383, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), 80),
      384 -> NGrammar.NProxy(384, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))), 84),
      386 -> NGrammar.NNonterminal(386, Symbols.Nonterminal("CharChar"), Set(387)),
      389 -> NGrammar.NRepeat(389, Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), 7, 390),
      391 -> NGrammar.NNonterminal(391, Symbols.Nonterminal("StrChar"), Set(392)),
      394 -> NGrammar.NNonterminal(394, Symbols.Nonterminal("EnumValue"), Set(395)),
      396 -> NGrammar.NLongest(396, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))))))), 397),
      397 -> NGrammar.NOneOf(397, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue")))))), Set(398, 404)),
      398 -> NGrammar.NProxy(398, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), 399),
      400 -> NGrammar.NNonterminal(400, Symbols.Nonterminal("CanonicalEnumValue"), Set(401)),
      402 -> NGrammar.NNonterminal(402, Symbols.Nonterminal("EnumValueName"), Set(403)),
      404 -> NGrammar.NProxy(404, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue")))), 405),
      406 -> NGrammar.NNonterminal(406, Symbols.Nonterminal("ShortenedEnumValue"), Set(407)),
      409 -> NGrammar.NOneOf(409, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))))), Set(410)),
      410 -> NGrammar.NProxy(410, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))), 411),
      413 -> NGrammar.NProxy(413, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':')))), 414),
      416 -> NGrammar.NOneOf(416, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))))), Set(417, 421)),
      417 -> NGrammar.NProxy(417, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), 418),
      419 -> NGrammar.NProxy(419, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('=')))), 420),
      421 -> NGrammar.NProxy(421, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))), 422),
      423 -> NGrammar.NProxy(423, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('=')))), 424),
      426 -> NGrammar.NProxy(426, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|')))), 427),
      429 -> NGrammar.NProxy(429, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&')))), 430),
      432 -> NGrammar.NLongest(432, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))), 433),
      433 -> NGrammar.NOneOf(433, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr")))))), Set(434)),
      434 -> NGrammar.NProxy(434, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr")))), 435),
      437 -> NGrammar.NRepeat(437, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), 0), 7, 438),
      439 -> NGrammar.NOneOf(439, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), Set(440)),
      440 -> NGrammar.NProxy(440, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))), 441),
      442 -> NGrammar.NRepeat(442, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0), 7, 443),
      444 -> NGrammar.NOneOf(444, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), Set(445)),
      445 -> NGrammar.NProxy(445, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))), 446),
      447 -> NGrammar.NRepeat(447, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), 0), 7, 448),
      449 -> NGrammar.NOneOf(449, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), Set(450)),
      450 -> NGrammar.NProxy(450, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))), 451),
      452 -> NGrammar.NNonterminal(452, Symbols.Nonterminal("WSNL"), Set(453)),
      454 -> NGrammar.NLongest(454, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS"))))))), 455),
      455 -> NGrammar.NOneOf(455, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS")))))), Set(456)),
      456 -> NGrammar.NProxy(456, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS")))), 457),
      458 -> NGrammar.NRepeat(458, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), 7, 459),
      460 -> NGrammar.NOneOf(460, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), Set(461, 13)),
      461 -> NGrammar.NProxy(461, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), 462),
      463 -> NGrammar.NTerminal(463, Symbols.Chars(Set('\t', '\r', ' ')))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Def"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), 0), Symbols.Nonterminal("WS"))), Seq(4, 34, 447, 4)),
      5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0))), Seq(6)),
      7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq()), Seq()),
      8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))))), Seq(6, 9)),
      11 -> NGrammar.NSequence(11, Symbols.Sequence(Seq(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet))), Seq(12)),
      14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment"))), Seq(15)),
      16 -> NGrammar.NSequence(16, Symbols.Sequence(Seq(Symbols.ExactChar('/'), Symbols.ExactChar('/'), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("EOF")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\n')))))))), Seq(17, 17, 18, 26)),
      19 -> NGrammar.NSequence(19, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))))), Seq(18, 20)),
      22 -> NGrammar.NSequence(22, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')))), Seq(23)),
      28 -> NGrammar.NSequence(28, Symbols.Sequence(Seq(Symbols.Nonterminal("EOF"))), Seq(29)),
      30 -> NGrammar.NSequence(30, Symbols.Sequence(Seq(Symbols.LookaheadExcept(Symbols.AnyChar))), Seq(31)),
      33 -> NGrammar.NSequence(33, Symbols.Sequence(Seq(Symbols.ExactChar('\n'))), Seq(25)),
      35 -> NGrammar.NSequence(35, Symbols.Sequence(Seq(Symbols.Nonterminal("Rule"))), Seq(36)),
      37 -> NGrammar.NSequence(37, Symbols.Sequence(Seq(Symbols.Nonterminal("LHS"), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0)))))))), Seq(38, 4, 193, 4, 194)),
      39 -> NGrammar.NSequence(39, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(40, 93)),
      41 -> NGrammar.NSequence(41, Symbols.Sequence(Seq(Symbols.Nonterminal("NonterminalName"))), Seq(42)),
      43 -> NGrammar.NSequence(43, Symbols.Sequence(Seq(Symbols.Nonterminal("IdNoKeyword"))), Seq(44)),
      45 -> NGrammar.NSequence(45, Symbols.Sequence(Seq(Symbols.Except(Symbols.Nonterminal("Id"), Symbols.Nonterminal("Keyword")))), Seq(46)),
      48 -> NGrammar.NSequence(48, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))))), Seq(49)),
      52 -> NGrammar.NSequence(52, Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))), Seq(53, 54)),
      55 -> NGrammar.NSequence(55, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(54, 56)),
      58 -> NGrammar.NSequence(58, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('n')))))), Seq(59)),
      60 -> NGrammar.NSequence(60, Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('n'))), Seq(61, 62, 62, 63, 64, 65, 66)),
      67 -> NGrammar.NSequence(67, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('c'), Symbols.ExactChar('h'), Symbols.ExactChar('a'), Symbols.ExactChar('r')))))), Seq(68)),
      69 -> NGrammar.NSequence(69, Symbols.Sequence(Seq(Symbols.ExactChar('c'), Symbols.ExactChar('h'), Symbols.ExactChar('a'), Symbols.ExactChar('r'))), Seq(70, 71, 65, 72)),
      73 -> NGrammar.NSequence(73, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g')))))), Seq(74)),
      75 -> NGrammar.NSequence(75, Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g'))), Seq(76, 77, 72, 78, 66, 79)),
      80 -> NGrammar.NSequence(80, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e')))))), Seq(81)),
      82 -> NGrammar.NSequence(82, Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))), Seq(77, 72, 83, 64)),
      84 -> NGrammar.NSequence(84, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e')))))), Seq(85)),
      86 -> NGrammar.NSequence(86, Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))), Seq(87, 65, 63, 76, 64)),
      88 -> NGrammar.NSequence(88, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('l'), Symbols.ExactChar('l')))))), Seq(89)),
      90 -> NGrammar.NSequence(90, Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('l'), Symbols.ExactChar('l'))), Seq(66, 83, 63, 63)),
      91 -> NGrammar.NSequence(91, Symbols.Sequence(Seq(Symbols.ExactChar('`'), Symbols.Nonterminal("Id"), Symbols.ExactChar('`'))), Seq(92, 47, 92)),
      96 -> NGrammar.NSequence(96, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc"))), Seq(4, 97, 4, 98)),
      99 -> NGrammar.NSequence(99, Symbols.Sequence(Seq(Symbols.Nonterminal("NonNullTypeDesc"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(100, 188)),
      101 -> NGrammar.NSequence(101, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"))), Seq(102)),
      103 -> NGrammar.NSequence(103, Symbols.Sequence(Seq(Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc"), Symbols.Nonterminal("WS"), Symbols.ExactChar(']'))), Seq(104, 4, 98, 4, 105)),
      106 -> NGrammar.NSequence(106, Symbols.Sequence(Seq(Symbols.Nonterminal("ValueType"))), Seq(107)),
      108 -> NGrammar.NSequence(108, Symbols.Sequence(Seq(Symbols.Nonterminal("AnyType"))), Seq(109)),
      110 -> NGrammar.NSequence(110, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'), Symbols.ExactChar('n'), Symbols.ExactChar('y')))))), Seq(111)),
      112 -> NGrammar.NSequence(112, Symbols.Sequence(Seq(Symbols.ExactChar('a'), Symbols.ExactChar('n'), Symbols.ExactChar('y'))), Seq(65, 66, 113)),
      114 -> NGrammar.NSequence(114, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"))), Seq(115)),
      116 -> NGrammar.NSequence(116, Symbols.Sequence(Seq(Symbols.ExactChar('%'), Symbols.Nonterminal("Id"))), Seq(117, 47)),
      118 -> NGrammar.NSequence(118, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeDef"))), Seq(119)),
      120 -> NGrammar.NSequence(120, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassDef"))), Seq(121)),
      122 -> NGrammar.NSequence(122, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes"))), Seq(102, 4, 123)),
      124 -> NGrammar.NSequence(124, Symbols.Sequence(Seq(Symbols.ExactChar('<'), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar('>'))), Seq(125, 4, 126, 137)),
      129 -> NGrammar.NSequence(129, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS"))), Seq(102, 130, 4)),
      131 -> NGrammar.NSequence(131, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))))), Seq(130, 132)),
      134 -> NGrammar.NSequence(134, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName"))), Seq(4, 135, 4, 102)),
      138 -> NGrammar.NSequence(138, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamsDef"))), Seq(102, 4, 139)),
      140 -> NGrammar.NSequence(140, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(141, 4, 142, 4, 154)),
      145 -> NGrammar.NSequence(145, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS"))), Seq(146, 149, 4)),
      147 -> NGrammar.NSequence(147, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(148, 93)),
      150 -> NGrammar.NSequence(150, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))))), Seq(149, 151)),
      153 -> NGrammar.NSequence(153, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef"))), Seq(4, 135, 4, 146)),
      155 -> NGrammar.NSequence(155, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamsDef"))), Seq(102, 4, 123, 4, 139)),
      156 -> NGrammar.NSequence(156, Symbols.Sequence(Seq(Symbols.Nonterminal("SuperDef"))), Seq(157)),
      158 -> NGrammar.NSequence(158, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar('{'), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(102, 159, 4, 163, 164, 4, 176)),
      162 -> NGrammar.NSequence(162, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes"))), Seq(4, 123)),
      167 -> NGrammar.NSequence(167, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes"))), Seq(4, 168)),
      169 -> NGrammar.NSequence(169, Symbols.Sequence(Seq(Symbols.Nonterminal("SubType"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), 0))), Seq(170, 171)),
      172 -> NGrammar.NSequence(172, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))))), Seq(171, 173)),
      175 -> NGrammar.NSequence(175, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType"))), Seq(4, 135, 4, 170)),
      177 -> NGrammar.NSequence(177, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeDef"))), Seq(178)),
      179 -> NGrammar.NSequence(179, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('{'), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0)))))), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(115, 4, 163, 4, 180, 4, 176)),
      182 -> NGrammar.NSequence(182, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0))), Seq(47, 183)),
      184 -> NGrammar.NSequence(184, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))))), Seq(183, 185)),
      187 -> NGrammar.NSequence(187, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id"))), Seq(4, 135, 4, 47)),
      191 -> NGrammar.NSequence(191, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?'))), Seq(4, 192)),
      196 -> NGrammar.NSequence(196, Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0))), Seq(197, 442)),
      198 -> NGrammar.NSequence(198, Symbols.Sequence(Seq(Symbols.Nonterminal("Sequence"))), Seq(199)),
      200 -> NGrammar.NSequence(200, Symbols.Sequence(Seq(Symbols.Nonterminal("Elem"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), 0))), Seq(201, 437)),
      202 -> NGrammar.NSequence(202, Symbols.Sequence(Seq(Symbols.Nonterminal("Symbol"))), Seq(203)),
      204 -> NGrammar.NSequence(204, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"))), Seq(205)),
      206 -> NGrammar.NSequence(206, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('&'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(205, 4, 207, 4, 208)),
      209 -> NGrammar.NSequence(209, Symbols.Sequence(Seq(Symbols.ExactChar('^'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(210, 4, 208)),
      211 -> NGrammar.NSequence(211, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(212, 4, 208)),
      213 -> NGrammar.NSequence(213, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"))), Seq(214)),
      215 -> NGrammar.NSequence(215, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('?'))), Seq(214, 4, 192)),
      216 -> NGrammar.NSequence(216, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('*'))), Seq(214, 4, 217)),
      218 -> NGrammar.NSequence(218, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('+'))), Seq(214, 4, 219)),
      220 -> NGrammar.NSequence(220, Symbols.Sequence(Seq(Symbols.Nonterminal("AtomSymbol"))), Seq(221)),
      222 -> NGrammar.NSequence(222, Symbols.Sequence(Seq(Symbols.Nonterminal("Terminal"))), Seq(223)),
      224 -> NGrammar.NSequence(224, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChar"), Symbols.ExactChar('\''))), Seq(225, 226, 225)),
      227 -> NGrammar.NSequence(227, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')))), Seq(228)),
      230 -> NGrammar.NSequence(230, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('\'', '\\', 'b', 'n', 'r', 't')))), Seq(229, 231)),
      232 -> NGrammar.NSequence(232, Symbols.Sequence(Seq(Symbols.Nonterminal("UnicodeChar"))), Seq(233)),
      234 -> NGrammar.NSequence(234, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('u'), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet))), Seq(229, 83, 235, 235, 235, 235)),
      236 -> NGrammar.NSequence(236, Symbols.Sequence(Seq(Symbols.ExactChar('.'))), Seq(237)),
      238 -> NGrammar.NSequence(238, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoice"))), Seq(239)),
      240 -> NGrammar.NSequence(240, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChoiceElem"), Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), Symbols.ExactChar('\''))), Seq(225, 241, 253, 225)),
      242 -> NGrammar.NSequence(242, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"))), Seq(243)),
      244 -> NGrammar.NSequence(244, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'', '-', '\\'))))), Seq(245)),
      247 -> NGrammar.NSequence(247, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('\'', '-', '\\', 'b', 'n', 'r', 't')))), Seq(229, 248)),
      249 -> NGrammar.NSequence(249, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceRange"))), Seq(250)),
      251 -> NGrammar.NSequence(251, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"), Symbols.ExactChar('-'), Symbols.Nonterminal("TerminalChoiceChar"))), Seq(243, 252, 243)),
      254 -> NGrammar.NSequence(254, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), Symbols.Nonterminal("TerminalChoiceElem"))), Seq(253, 241)),
      255 -> NGrammar.NSequence(255, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChoiceRange"), Symbols.ExactChar('\''))), Seq(225, 250, 225)),
      256 -> NGrammar.NSequence(256, Symbols.Sequence(Seq(Symbols.Nonterminal("StringSymbol"))), Seq(257)),
      258 -> NGrammar.NSequence(258, Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), Symbols.ExactChar('"'))), Seq(259, 260, 259)),
      261 -> NGrammar.NSequence(261, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), Symbols.Nonterminal("StringChar"))), Seq(260, 262)),
      263 -> NGrammar.NSequence(263, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"', '\\'))))), Seq(264)),
      266 -> NGrammar.NSequence(266, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('"', '\\', 'b', 'n', 'r', 't')))), Seq(229, 267)),
      268 -> NGrammar.NSequence(268, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"))), Seq(40)),
      269 -> NGrammar.NSequence(269, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceChoices"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(141, 4, 270, 4, 154)),
      271 -> NGrammar.NSequence(271, Symbols.Sequence(Seq(Symbols.Nonterminal("Sequence"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), 0))), Seq(199, 272)),
      273 -> NGrammar.NSequence(273, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))))), Seq(272, 274)),
      276 -> NGrammar.NSequence(276, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence"))), Seq(4, 277, 4, 199)),
      278 -> NGrammar.NSequence(278, Symbols.Sequence(Seq(Symbols.Nonterminal("Longest"))), Seq(279)),
      280 -> NGrammar.NSequence(280, Symbols.Sequence(Seq(Symbols.ExactChar('<'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceChoices"), Symbols.Nonterminal("WS"), Symbols.ExactChar('>'))), Seq(125, 4, 270, 4, 137)),
      281 -> NGrammar.NSequence(281, Symbols.Sequence(Seq(Symbols.Nonterminal("EmptySequence"))), Seq(282)),
      283 -> NGrammar.NSequence(283, Symbols.Sequence(Seq(Symbols.ExactChar('#'))), Seq(284)),
      285 -> NGrammar.NSequence(285, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('-'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(205, 4, 252, 4, 208)),
      286 -> NGrammar.NSequence(286, Symbols.Sequence(Seq(Symbols.Nonterminal("PreUnSymbol"))), Seq(208)),
      287 -> NGrammar.NSequence(287, Symbols.Sequence(Seq(Symbols.Nonterminal("Processor"))), Seq(288)),
      289 -> NGrammar.NSequence(289, Symbols.Sequence(Seq(Symbols.Nonterminal("Ref"))), Seq(290)),
      291 -> NGrammar.NSequence(291, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"))), Seq(292)),
      293 -> NGrammar.NSequence(293, Symbols.Sequence(Seq(Symbols.ExactChar('$'), Symbols.OneOf(ListSet(Symbols.Nonterminal("CondSymPath"), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("RefIdx"))), Seq(294, 295, 305)),
      297 -> NGrammar.NSequence(297, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), 1))), Seq(298)),
      301 -> NGrammar.NSequence(301, Symbols.Sequence(Seq(Symbols.ExactChar('<'))), Seq(125)),
      303 -> NGrammar.NSequence(303, Symbols.Sequence(Seq(Symbols.ExactChar('>'))), Seq(137)),
      304 -> NGrammar.NSequence(304, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), 1), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))))), Seq(298, 299)),
      306 -> NGrammar.NSequence(306, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))))), Seq(307)),
      310 -> NGrammar.NSequence(310, Symbols.Sequence(Seq(Symbols.ExactChar('0'))), Seq(311)),
      313 -> NGrammar.NSequence(313, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(314, 315)),
      316 -> NGrammar.NSequence(316, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), Symbols.Chars(('0' to '9').toSet))), Seq(315, 317)),
      318 -> NGrammar.NSequence(318, Symbols.Sequence(Seq(Symbols.Nonterminal("RawRef"))), Seq(319)),
      320 -> NGrammar.NSequence(320, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$')))), Symbols.OneOf(ListSet(Symbols.Nonterminal("CondSymPath"), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("RefIdx"))), Seq(321, 295, 305)),
      322 -> NGrammar.NSequence(322, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$'))), Seq(229, 294)),
      323 -> NGrammar.NSequence(323, Symbols.Sequence(Seq(Symbols.ExactChar('{'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(163, 4, 324, 4, 176)),
      325 -> NGrammar.NSequence(325, Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(326, 93)),
      327 -> NGrammar.NSequence(327, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar('?'), Symbols.Nonterminal("WS"), Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))), Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))))), Seq(328, 4, 192, 4, 432, 4, 97, 4, 432)),
      329 -> NGrammar.NSequence(329, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolOrExpr"))), Seq(330, 4, 429, 4, 328)),
      331 -> NGrammar.NSequence(331, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolAndExpr"))), Seq(332, 4, 426, 4, 330)),
      333 -> NGrammar.NSequence(333, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolEqExpr"))), Seq(334, 4, 416, 4, 332)),
      335 -> NGrammar.NSequence(335, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ElvisExpr"))), Seq(336, 4, 413, 4, 334)),
      337 -> NGrammar.NSequence(337, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("AdditiveExpr"))), Seq(338, 4, 409, 4, 336)),
      339 -> NGrammar.NSequence(339, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PrefixNotExpr"))), Seq(212, 4, 338)),
      340 -> NGrammar.NSequence(340, Symbols.Sequence(Seq(Symbols.Nonterminal("Atom"))), Seq(341)),
      342 -> NGrammar.NSequence(342, Symbols.Sequence(Seq(Symbols.Nonterminal("BindExpr"))), Seq(343)),
      344 -> NGrammar.NSequence(344, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"), Symbols.Nonterminal("BinderExpr"))), Seq(292, 345)),
      346 -> NGrammar.NSequence(346, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedConstructExpr"))), Seq(347)),
      348 -> NGrammar.NSequence(348, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedConstructParams"))), Seq(102, 159, 4, 349)),
      350 -> NGrammar.NSequence(350, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.ExactChar(')'))), Seq(141, 4, 351, 154)),
      353 -> NGrammar.NSequence(353, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS"))), Seq(354, 356, 4)),
      355 -> NGrammar.NSequence(355, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"))), Seq(148, 93, 4, 193, 4, 324)),
      357 -> NGrammar.NSequence(357, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))))), Seq(356, 358)),
      360 -> NGrammar.NSequence(360, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam"))), Seq(4, 135, 4, 354)),
      361 -> NGrammar.NSequence(361, Symbols.Sequence(Seq(Symbols.Nonterminal("FuncCallOrConstructExpr"))), Seq(362)),
      363 -> NGrammar.NSequence(363, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeOrFuncName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("CallParams"))), Seq(364, 4, 365)),
      366 -> NGrammar.NSequence(366, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar(')'))), Seq(141, 4, 367, 154)),
      370 -> NGrammar.NSequence(370, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS"))), Seq(324, 371, 4)),
      372 -> NGrammar.NSequence(372, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))))), Seq(371, 373)),
      375 -> NGrammar.NSequence(375, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"))), Seq(4, 135, 4, 324)),
      376 -> NGrammar.NSequence(376, Symbols.Sequence(Seq(Symbols.Nonterminal("ArrayExpr"))), Seq(377)),
      378 -> NGrammar.NSequence(378, Symbols.Sequence(Seq(Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar(']'))), Seq(104, 4, 367, 105)),
      379 -> NGrammar.NSequence(379, Symbols.Sequence(Seq(Symbols.Nonterminal("Literal"))), Seq(380)),
      381 -> NGrammar.NSequence(381, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))))))), Seq(382)),
      385 -> NGrammar.NSequence(385, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("CharChar"), Symbols.ExactChar('\''))), Seq(225, 386, 225)),
      387 -> NGrammar.NSequence(387, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChar"))), Seq(226)),
      388 -> NGrammar.NSequence(388, Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), Symbols.ExactChar('"'))), Seq(259, 389, 259)),
      390 -> NGrammar.NSequence(390, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), Symbols.Nonterminal("StrChar"))), Seq(389, 391)),
      392 -> NGrammar.NSequence(392, Symbols.Sequence(Seq(Symbols.Nonterminal("StringChar"))), Seq(262)),
      393 -> NGrammar.NSequence(393, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumValue"))), Seq(394)),
      395 -> NGrammar.NSequence(395, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))))))))), Seq(396)),
      399 -> NGrammar.NSequence(399, Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue"))), Seq(400)),
      401 -> NGrammar.NSequence(401, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"), Symbols.ExactChar('.'), Symbols.Nonterminal("EnumValueName"))), Seq(115, 237, 402)),
      403 -> NGrammar.NSequence(403, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(47)),
      405 -> NGrammar.NSequence(405, Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))), Seq(406)),
      407 -> NGrammar.NSequence(407, Symbols.Sequence(Seq(Symbols.ExactChar('%'), Symbols.Nonterminal("EnumValueName"))), Seq(117, 402)),
      408 -> NGrammar.NSequence(408, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(141, 4, 324, 4, 154)),
      411 -> NGrammar.NSequence(411, Symbols.Sequence(Seq(Symbols.ExactChar('+'))), Seq(219)),
      412 -> NGrammar.NSequence(412, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"))), Seq(338)),
      414 -> NGrammar.NSequence(414, Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':'))), Seq(192, 97)),
      415 -> NGrammar.NSequence(415, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"))), Seq(336)),
      418 -> NGrammar.NSequence(418, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('=')))))), Seq(419)),
      420 -> NGrammar.NSequence(420, Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))), Seq(193, 193)),
      422 -> NGrammar.NSequence(422, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('=')))))), Seq(423)),
      424 -> NGrammar.NSequence(424, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))), Seq(212, 193)),
      425 -> NGrammar.NSequence(425, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"))), Seq(334)),
      427 -> NGrammar.NSequence(427, Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|'))), Seq(277, 277)),
      428 -> NGrammar.NSequence(428, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"))), Seq(332)),
      430 -> NGrammar.NSequence(430, Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&'))), Seq(207, 207)),
      431 -> NGrammar.NSequence(431, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"))), Seq(330)),
      435 -> NGrammar.NSequence(435, Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))), Seq(326)),
      436 -> NGrammar.NSequence(436, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"))), Seq(328)),
      438 -> NGrammar.NSequence(438, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))))), Seq(437, 439)),
      441 -> NGrammar.NSequence(441, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem"))), Seq(4, 201)),
      443 -> NGrammar.NSequence(443, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))))), Seq(442, 444)),
      446 -> NGrammar.NSequence(446, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS"))), Seq(4, 277, 4, 197)),
      448 -> NGrammar.NSequence(448, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))))), Seq(447, 449)),
      451 -> NGrammar.NSequence(451, Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def"))), Seq(452, 34)),
      453 -> NGrammar.NSequence(453, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS"))))))))), Seq(454)),
      457 -> NGrammar.NSequence(457, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS"))), Seq(458, 25, 4)),
      459 -> NGrammar.NSequence(459, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))))), Seq(458, 460)),
      462 -> NGrammar.NSequence(462, Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' ')))), Seq(463))),
    1)

  sealed trait TerminalChoiceElem

  sealed trait PostUnSymbol extends PreUnSymbol

  sealed trait BoolOrExpr extends TernaryExpr

  sealed trait BinderExpr

  sealed trait TerminalChar extends Terminal

  case class EmptySeq() extends AtomSymbol

  case class StringType() extends ValueType

  sealed trait Terminal extends AtomSymbol

  sealed trait Atom extends PrefixNotExpr

  case class AbstractClassDef(name: TypeName, supers: List[TypeName]) extends ClassDef

  case class NotFollowedBy(notFollowedBy: PreUnSymbol) extends PreUnSymbol

  sealed trait AbstractEnumValue extends Atom

  case class ClassParamDef(name: ParamName, typeDesc: Option[TypeDesc])

  case class EnumTypeDef(name: EnumTypeName, values: List[String]) extends TypeDef

  case class Nonterminal(name: NonterminalName) extends AtomSymbol

  case class FollowedBy(followedBy: PreUnSymbol) extends PreUnSymbol

  case class AnyType() extends NonNullTypeDesc

  sealed trait Processor extends Elem

  case class TypeName(name: String) extends SubType with NonNullTypeDesc

  case class Optional(body: PostUnSymbol) extends PostUnSymbol

  sealed trait ValueType extends NonNullTypeDesc

  case class NamedConstructExpr(typeName: TypeName, params: List[NamedParam], supers: Option[List[TypeName]]) extends Atom

  sealed trait ClassDef extends TypeDef with SubType

  case class PExpr(body: TernaryExpr, typ: Option[TypeDesc]) extends BinderExpr with Processor

  case class ValRef(idx: String, condSymPath: Option[List[CondSymDir.Value]]) extends Ref

  case class ArrayExpr(elems: List[PExpr]) extends Atom

  case class TerminalChoiceRange(start: TerminalChoiceChar, end: TerminalChoiceChar) extends TerminalChoiceElem

  sealed trait StringChar

  case class ExceptSymbol(body: BinSymbol, except: PreUnSymbol) extends BinSymbol

  case class RawRef(idx: String, condSymPath: Option[List[CondSymDir.Value]]) extends Ref

  case class BoolLiteral(value: Boolean) extends Literal

  case class Elvis(value: AdditiveExpr, ifNull: ElvisExpr) extends ElvisExpr

  sealed trait Literal extends Atom

  case class LHS(name: Nonterminal, typeDesc: Option[TypeDesc])

  case class TernaryOp(cond: BoolOrExpr, ifTrue: TernaryExpr, ifFalse: TernaryExpr) extends TernaryExpr

  sealed trait BoolAndExpr extends BoolOrExpr

  sealed trait PrefixNotExpr extends AdditiveExpr

  case class ConcreteClassDef(name: TypeName, supers: Option[List[TypeName]], params: List[ClassParamDef]) extends ClassDef

  case class FuncCallOrConstructExpr(funcName: TypeOrFuncName, params: List[PExpr]) extends Atom

  case class Sequence(seq: List[Elem]) extends Symbol

  case class InPlaceChoices(choices: List[Sequence]) extends AtomSymbol

  case class SuperDef(typeName: TypeName, subs: Option[List[SubType]], supers: Option[List[TypeName]]) extends TypeDef with SubType

  case class Rule(lhs: LHS, rhs: List[Sequence]) extends Def

  sealed trait TypeDef extends NonNullTypeDesc with Def

  case class StrLiteral(value: List[StringChar]) extends Literal

  case class CharAsIs(value: Char) extends StringChar with TerminalChoiceChar with TerminalChar

  case class AnyTerminal() extends Terminal

  case class BindExpr(ctx: ValRef, binder: BinderExpr) extends BinderExpr with Atom

  sealed trait TerminalChoiceChar extends TerminalChoiceElem

  case class RepeatFromZero(body: PostUnSymbol) extends PostUnSymbol

  case class ExprParen(body: PExpr) extends Atom

  case class EnumTypeName(name: String) extends NonNullTypeDesc

  case class JoinSymbol(body: BinSymbol, join: PreUnSymbol) extends BinSymbol

  sealed trait Elem

  sealed trait AtomSymbol extends PostUnSymbol

  case class ArrayTypeDesc(elemType: TypeDesc) extends NonNullTypeDesc

  case class NullLiteral() extends Literal

  sealed trait SubType

  case class CharType() extends ValueType

  case class StringSymbol(value: List[StringChar]) extends AtomSymbol

  case class BinOp(op: Op.Value, lhs: BoolAndExpr, rhs: BoolOrExpr) extends AdditiveExpr

  sealed trait Symbol extends Elem

  case class TypeDesc(typ: NonNullTypeDesc, optional: Boolean)

  sealed trait TernaryExpr

  case class NonterminalName(name: String)

  sealed trait NonNullTypeDesc

  sealed trait AdditiveExpr extends ElvisExpr

  case class RepeatFromOne(body: PostUnSymbol) extends PostUnSymbol

  case class Longest(choices: InPlaceChoices) extends AtomSymbol

  case class NamedParam(name: ParamName, typeDesc: Option[TypeDesc], expr: PExpr)

  sealed trait Def

  case class PrefixOp(op: PreOp.Value, expr: PrefixNotExpr) extends PrefixNotExpr

  case class CharLiteral(value: TerminalChar) extends Literal

  case class TerminalChoice(choices: List[TerminalChoiceElem]) extends AtomSymbol

  case class ShortenedEnumValue(valueName: EnumValueName) extends AbstractEnumValue

  case class TypeOrFuncName(name: String)

  case class ParamName(name: String)

  case class CharUnicode(code: List[Char]) extends StringChar with TerminalChoiceChar with TerminalChar

  sealed trait PreUnSymbol extends BinSymbol

  case class BooleanType() extends ValueType

  sealed trait BinSymbol extends Symbol

  case class CanonicalEnumValue(enumName: EnumTypeName, valueName: EnumValueName) extends AbstractEnumValue

  case class CharEscaped(escapeCode: Char) extends StringChar with TerminalChoiceChar with TerminalChar

  sealed trait Ref extends BinderExpr with Atom with Processor

  sealed trait BoolEqExpr extends BoolAndExpr

  case class EnumValueName(name: String)

  sealed trait ElvisExpr extends BoolEqExpr

  case class Grammar(defs: List[Def])

  object CondSymDir extends Enumeration {
    val BODY, COND = Value
  }

  object KeyWord extends Enumeration {
    val BOOLEAN, CHAR, FALSE, NULL, STRING, TRUE = Value
  }

  object Op extends Enumeration {
    val ADD, AND, EQ, NE, OR = Value
  }

  object PreOp extends Enumeration {
    val NOT = Value
  }

  def matchStringSymbol(node: Node): StringSymbol = {
    val BindNode(v778, v779) = node
    val v784 = v778.id match {
      case 258 =>
        val v780 = v779.asInstanceOf[SequenceNode].children(1)
        val v781 = unrollRepeat0(v780).map { elem =>
          val BindNode(v782, v783) = elem
          assert(v782.id == 262)
          matchStringChar(v783)
        }
        StringSymbol(v781)
    }
    v784
  }

  def matchAtom(node: Node): Atom = {
    val BindNode(v144, v145) = node
    val v170 = v144.id match {
      case 393 =>
        val v146 = v145.asInstanceOf[SequenceNode].children.head
        val BindNode(v147, v148) = v146
        assert(v147.id == 394)
        matchEnumValue(v148)
      case 289 =>
        val v149 = v145.asInstanceOf[SequenceNode].children.head
        val BindNode(v150, v151) = v149
        assert(v150.id == 290)
        matchRef(v151)
      case 408 =>
        val v152 = v145.asInstanceOf[SequenceNode].children(2)
        val BindNode(v153, v154) = v152
        assert(v153.id == 324)
        ExprParen(matchPExpr(v154))
      case 346 =>
        val v155 = v145.asInstanceOf[SequenceNode].children.head
        val BindNode(v156, v157) = v155
        assert(v156.id == 347)
        matchNamedConstructExpr(v157)
      case 361 =>
        val v158 = v145.asInstanceOf[SequenceNode].children.head
        val BindNode(v159, v160) = v158
        assert(v159.id == 362)
        matchFuncCallOrConstructExpr(v160)
      case 376 =>
        val v161 = v145.asInstanceOf[SequenceNode].children.head
        val BindNode(v162, v163) = v161
        assert(v162.id == 377)
        matchArrayExpr(v163)
      case 379 =>
        val v164 = v145.asInstanceOf[SequenceNode].children.head
        val BindNode(v165, v166) = v164
        assert(v165.id == 380)
        matchLiteral(v166)
      case 342 =>
        val v167 = v145.asInstanceOf[SequenceNode].children.head
        val BindNode(v168, v169) = v167
        assert(v168.id == 343)
        matchBindExpr(v169)
    }
    v170
  }

  def matchPostUnSymbol(node: Node): PostUnSymbol = {
    val BindNode(v33, v34) = node
    val v47 = v33.id match {
      case 215 =>
        val v35 = v34.asInstanceOf[SequenceNode].children.head
        val BindNode(v36, v37) = v35
        assert(v36.id == 214)
        Optional(matchPostUnSymbol(v37))
      case 216 =>
        val v38 = v34.asInstanceOf[SequenceNode].children.head
        val BindNode(v39, v40) = v38
        assert(v39.id == 214)
        RepeatFromZero(matchPostUnSymbol(v40))
      case 218 =>
        val v41 = v34.asInstanceOf[SequenceNode].children.head
        val BindNode(v42, v43) = v41
        assert(v42.id == 214)
        RepeatFromOne(matchPostUnSymbol(v43))
      case 220 =>
        val v44 = v34.asInstanceOf[SequenceNode].children.head
        val BindNode(v45, v46) = v44
        assert(v45.id == 221)
        matchAtomSymbol(v46)
    }
    v47
  }

  def matchBinderExpr(node: Node): BinderExpr = {
    val BindNode(v110, v111) = node
    val v121 = v110.id match {
      case 289 =>
        val v112 = v111.asInstanceOf[SequenceNode].children.head
        val BindNode(v113, v114) = v112
        assert(v113.id == 290)
        matchRef(v114)
      case 342 =>
        val v115 = v111.asInstanceOf[SequenceNode].children.head
        val BindNode(v116, v117) = v115
        assert(v116.id == 343)
        matchBindExpr(v117)
      case 323 =>
        val v118 = v111.asInstanceOf[SequenceNode].children(2)
        val BindNode(v119, v120) = v118
        assert(v119.id == 324)
        matchPExpr(v120)
    }
    v121
  }

  def matchBinSymbol(node: Node): BinSymbol = {
    val BindNode(v975, v976) = node
    val v992 = v975.id match {
      case 206 =>
        val v977 = v976.asInstanceOf[SequenceNode].children.head
        val BindNode(v978, v979) = v977
        assert(v978.id == 205)
        val v980 = v976.asInstanceOf[SequenceNode].children(4)
        val BindNode(v981, v982) = v980
        assert(v981.id == 208)
        JoinSymbol(matchBinSymbol(v979), matchPreUnSymbol(v982))
      case 285 =>
        val v983 = v976.asInstanceOf[SequenceNode].children.head
        val BindNode(v984, v985) = v983
        assert(v984.id == 205)
        val v986 = v976.asInstanceOf[SequenceNode].children(4)
        val BindNode(v987, v988) = v986
        assert(v987.id == 208)
        ExceptSymbol(matchBinSymbol(v985), matchPreUnSymbol(v988))
      case 286 =>
        val v989 = v976.asInstanceOf[SequenceNode].children.head
        val BindNode(v990, v991) = v989
        assert(v990.id == 208)
        matchPreUnSymbol(v991)
    }
    v992
  }

  def matchLongest(node: Node): Longest = {
    val BindNode(v885, v886) = node
    val v890 = v885.id match {
      case 280 =>
        val v887 = v886.asInstanceOf[SequenceNode].children(2)
        val BindNode(v888, v889) = v887
        assert(v888.id == 270)
        Longest(matchInPlaceChoices(v889))
    }
    v890
  }

  def matchUnicodeChar(node: Node): CharUnicode = {
    val BindNode(v426, v427) = node
    val v440 = v426.id match {
      case 234 =>
        val v428 = v427.asInstanceOf[SequenceNode].children(2)
        val BindNode(v429, v430) = v428
        assert(v429.id == 235)
        val v431 = v427.asInstanceOf[SequenceNode].children(3)
        val BindNode(v432, v433) = v431
        assert(v432.id == 235)
        val v434 = v427.asInstanceOf[SequenceNode].children(4)
        val BindNode(v435, v436) = v434
        assert(v435.id == 235)
        val v437 = v427.asInstanceOf[SequenceNode].children(5)
        val BindNode(v438, v439) = v437
        assert(v438.id == 235)
        CharUnicode(List(v430.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v433.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v436.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v439.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char))
    }
    v440
  }

  def matchIdNoKeyword(node: Node): String = {
    val BindNode(v245, v246) = node
    val v248 = v245.id match {
      case 45 =>
        val v247 = v246.asInstanceOf[SequenceNode].children.head
        v247.sourceText
    }
    v248
  }

  def matchDef(node: Node): Def = {
    val BindNode(v914, v915) = node
    val v922 = v914.id match {
      case 35 =>
        val v916 = v915.asInstanceOf[SequenceNode].children.head
        val BindNode(v917, v918) = v916
        assert(v917.id == 36)
        matchRule(v918)
      case 118 =>
        val v919 = v915.asInstanceOf[SequenceNode].children.head
        val BindNode(v920, v921) = v919
        assert(v920.id == 119)
        matchTypeDef(v921)
    }
    v922
  }

  def matchRule(node: Node): Rule = {
    val BindNode(v581, v582) = node
    val v609 = v581.id match {
      case 37 =>
        val v583 = v582.asInstanceOf[SequenceNode].children.head
        val BindNode(v584, v585) = v583
        assert(v584.id == 38)
        val v586 = v582.asInstanceOf[SequenceNode].children(4)
        val BindNode(v587, v588) = v586
        assert(v587.id == 194)
        val BindNode(v589, v590) = v588
        val v608 = v589.id match {
          case 195 =>
            val BindNode(v591, v592) = v590
            assert(v591.id == 196)
            val v593 = v592.asInstanceOf[SequenceNode].children.head
            val BindNode(v594, v595) = v593
            assert(v594.id == 197)
            val v596 = v592.asInstanceOf[SequenceNode].children(1)
            val v597 = unrollRepeat0(v596).map { elem =>
              val BindNode(v598, v599) = elem
              assert(v598.id == 444)
              val BindNode(v600, v601) = v599
              val v607 = v600.id match {
                case 445 =>
                  val BindNode(v602, v603) = v601
                  assert(v602.id == 446)
                  val v604 = v603.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v605, v606) = v604
                  assert(v605.id == 197)
                  matchRHS(v606)
              }
              v607
            }
            List(matchRHS(v595)) ++ v597
        }
        Rule(matchLHS(v585), v608)
    }
    v609
  }

  def matchCallParams(node: Node): List[PExpr] = {
    val BindNode(v993, v994) = node
    val v1022 = v993.id match {
      case 366 =>
        val v996 = v994.asInstanceOf[SequenceNode].children(2)
        val BindNode(v997, v998) = v996
        assert(v997.id == 367)
        val BindNode(v999, v1000) = v998
        val v1021 = v999.id match {
          case 136 =>
            None
          case 368 =>
            val BindNode(v1001, v1002) = v1000
            val v1020 = v1001.id match {
              case 369 =>
                val BindNode(v1003, v1004) = v1002
                assert(v1003.id == 370)
                val v1005 = v1004.asInstanceOf[SequenceNode].children.head
                val BindNode(v1006, v1007) = v1005
                assert(v1006.id == 324)
                val v1008 = v1004.asInstanceOf[SequenceNode].children(1)
                val v1009 = unrollRepeat0(v1008).map { elem =>
                  val BindNode(v1010, v1011) = elem
                  assert(v1010.id == 373)
                  val BindNode(v1012, v1013) = v1011
                  val v1019 = v1012.id match {
                    case 374 =>
                      val BindNode(v1014, v1015) = v1013
                      assert(v1014.id == 375)
                      val v1016 = v1015.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v1017, v1018) = v1016
                      assert(v1017.id == 324)
                      matchPExpr(v1018)
                  }
                  v1019
                }
                List(matchPExpr(v1007)) ++ v1009
            }
            Some(v1020)
        }
        val v995 = v1021
        if (v995.isDefined) v995.get else List()
    }
    v1022
  }

  def matchTypeDef(node: Node): TypeDef = {
    val BindNode(v610, v611) = node
    val v621 = v610.id match {
      case 120 =>
        val v612 = v611.asInstanceOf[SequenceNode].children.head
        val BindNode(v613, v614) = v612
        assert(v613.id == 121)
        matchClassDef(v614)
      case 156 =>
        val v615 = v611.asInstanceOf[SequenceNode].children.head
        val BindNode(v616, v617) = v615
        assert(v616.id == 157)
        matchSuperDef(v617)
      case 177 =>
        val v618 = v611.asInstanceOf[SequenceNode].children.head
        val BindNode(v619, v620) = v618
        assert(v619.id == 178)
        matchEnumTypeDef(v620)
    }
    v621
  }

  def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
    val BindNode(v177, v178) = node
    val v185 = v177.id match {
      case 242 =>
        val v179 = v178.asInstanceOf[SequenceNode].children.head
        val BindNode(v180, v181) = v179
        assert(v180.id == 243)
        matchTerminalChoiceChar(v181)
      case 249 =>
        val v182 = v178.asInstanceOf[SequenceNode].children.head
        val BindNode(v183, v184) = v182
        assert(v183.id == 250)
        matchTerminalChoiceRange(v184)
    }
    v185
  }

  def matchRawRef(node: Node): RawRef = {
    val BindNode(v21, v22) = node
    val v32 = v21.id match {
      case 320 =>
        val v23 = v22.asInstanceOf[SequenceNode].children(2)
        val BindNode(v24, v25) = v23
        assert(v24.id == 305)
        val v26 = v22.asInstanceOf[SequenceNode].children(1)
        val BindNode(v27, v28) = v26
        assert(v27.id == 295)
        val BindNode(v29, v30) = v28
        val v31 = v29.id match {
          case 136 =>
            None
          case 296 =>
            Some(matchCondSymPath(v30))
        }
        RawRef(matchRefIdx(v25), v31)
    }
    v32
  }

  def matchCanonicalEnumValue(node: Node): CanonicalEnumValue = {
    val BindNode(v125, v126) = node
    val v133 = v125.id match {
      case 401 =>
        val v127 = v126.asInstanceOf[SequenceNode].children.head
        val BindNode(v128, v129) = v127
        assert(v128.id == 115)
        val v130 = v126.asInstanceOf[SequenceNode].children(2)
        val BindNode(v131, v132) = v130
        assert(v131.id == 402)
        CanonicalEnumValue(matchEnumTypeName(v129), matchEnumValueName(v132))
    }
    v133
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v891, v892) = node
    val v913 = v891.id match {
      case 355 =>
        val v893 = v892.asInstanceOf[SequenceNode].children.head
        val BindNode(v894, v895) = v893
        assert(v894.id == 148)
        val v896 = v892.asInstanceOf[SequenceNode].children(1)
        val BindNode(v897, v898) = v896
        assert(v897.id == 93)
        val BindNode(v899, v900) = v898
        val v909 = v899.id match {
          case 136 =>
            None
          case 94 =>
            val BindNode(v901, v902) = v900
            val v908 = v901.id match {
              case 95 =>
                val BindNode(v903, v904) = v902
                assert(v903.id == 96)
                val v905 = v904.asInstanceOf[SequenceNode].children(3)
                val BindNode(v906, v907) = v905
                assert(v906.id == 98)
                matchTypeDesc(v907)
            }
            Some(v908)
        }
        val v910 = v892.asInstanceOf[SequenceNode].children(5)
        val BindNode(v911, v912) = v910
        assert(v911.id == 324)
        NamedParam(matchParamName(v895), v909, matchPExpr(v912))
    }
    v913
  }

  def matchSequence(node: Node): Sequence = {
    val BindNode(v511, v512) = node
    val v528 = v511.id match {
      case 200 =>
        val v513 = v512.asInstanceOf[SequenceNode].children.head
        val BindNode(v514, v515) = v513
        assert(v514.id == 201)
        val v516 = v512.asInstanceOf[SequenceNode].children(1)
        val v517 = unrollRepeat0(v516).map { elem =>
          val BindNode(v518, v519) = elem
          assert(v518.id == 439)
          val BindNode(v520, v521) = v519
          val v527 = v520.id match {
            case 440 =>
              val BindNode(v522, v523) = v521
              assert(v522.id == 441)
              val v524 = v523.asInstanceOf[SequenceNode].children(1)
              val BindNode(v525, v526) = v524
              assert(v525.id == 201)
              matchElem(v526)
          }
          v527
        }
        Sequence(List(matchElem(v515)) ++ v517)
    }
    v528
  }

  def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
    val BindNode(v683, v684) = node
    val v696 = v683.id match {
      case 244 =>
        val v685 = v684.asInstanceOf[SequenceNode].children.head
        val BindNode(v686, v687) = v685
        assert(v686.id == 245)
        val BindNode(v688, v689) = v687
        assert(v688.id == 24)
        CharAsIs(v689.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)
      case 247 =>
        val v690 = v684.asInstanceOf[SequenceNode].children(1)
        val BindNode(v691, v692) = v690
        assert(v691.id == 248)
        CharEscaped(v692.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)
      case 232 =>
        val v693 = v684.asInstanceOf[SequenceNode].children.head
        val BindNode(v694, v695) = v693
        assert(v694.id == 233)
        matchUnicodeChar(v695)
    }
    v696
  }

  def matchRef(node: Node): Ref = {
    val BindNode(v1037, v1038) = node
    val v1045 = v1037.id match {
      case 291 =>
        val v1039 = v1038.asInstanceOf[SequenceNode].children.head
        val BindNode(v1040, v1041) = v1039
        assert(v1040.id == 292)
        matchValRef(v1041)
      case 318 =>
        val v1042 = v1038.asInstanceOf[SequenceNode].children.head
        val BindNode(v1043, v1044) = v1042
        assert(v1043.id == 319)
        matchRawRef(v1044)
    }
    v1045
  }

  def matchNonterminal(node: Node): Nonterminal = {
    val BindNode(v215, v216) = node
    val v220 = v215.id match {
      case 41 =>
        val v217 = v216.asInstanceOf[SequenceNode].children.head
        val BindNode(v218, v219) = v217
        assert(v218.id == 42)
        Nonterminal(matchNonterminalName(v219))
    }
    v220
  }

  def matchSuperDef(node: Node): SuperDef = {
    val BindNode(v547, v548) = node
    val v580 = v547.id match {
      case 158 =>
        val v549 = v548.asInstanceOf[SequenceNode].children.head
        val BindNode(v550, v551) = v549
        assert(v550.id == 102)
        val v552 = v548.asInstanceOf[SequenceNode].children(4)
        val BindNode(v553, v554) = v552
        assert(v553.id == 164)
        val BindNode(v555, v556) = v554
        val v565 = v555.id match {
          case 136 =>
            None
          case 165 =>
            val BindNode(v557, v558) = v556
            val v564 = v557.id match {
              case 166 =>
                val BindNode(v559, v560) = v558
                assert(v559.id == 167)
                val v561 = v560.asInstanceOf[SequenceNode].children(1)
                val BindNode(v562, v563) = v561
                assert(v562.id == 168)
                matchSubTypes(v563)
            }
            Some(v564)
        }
        val v566 = v548.asInstanceOf[SequenceNode].children(1)
        val BindNode(v567, v568) = v566
        assert(v567.id == 159)
        val BindNode(v569, v570) = v568
        val v579 = v569.id match {
          case 136 =>
            None
          case 160 =>
            val BindNode(v571, v572) = v570
            val v578 = v571.id match {
              case 161 =>
                val BindNode(v573, v574) = v572
                assert(v573.id == 162)
                val v575 = v574.asInstanceOf[SequenceNode].children(1)
                val BindNode(v576, v577) = v575
                assert(v576.id == 123)
                matchSuperTypes(v577)
            }
            Some(v578)
        }
        SuperDef(matchTypeName(v551), v565, v579)
    }
    v580
  }

  def matchId(node: Node): String = {
    val BindNode(v134, v135) = node
    val v137 = v134.id match {
      case 48 =>
        val v136 = v135.asInstanceOf[SequenceNode].children.head
        v136.sourceText
    }
    v137
  }

  def matchCondSymPath(node: Node): List[CondSymDir.Value] = {
    val BindNode(v325, v326) = node
    val v338 = v325.id match {
      case 297 =>
        val v327 = v326.asInstanceOf[SequenceNode].children.head
        val v328 = unrollRepeat1(v327).map { elem =>
          val BindNode(v329, v330) = elem
          assert(v329.id == 299)
          val BindNode(v331, v332) = v330
          val v337 = v331.id match {
            case 300 =>
              val BindNode(v333, v334) = v332
              assert(v333.id == 301)
              CondSymDir.BODY
            case 302 =>
              val BindNode(v335, v336) = v332
              assert(v335.id == 303)
              CondSymDir.COND
          }
          v337
        }
        v328
    }
    v338
  }

  def matchEnumTypeName(node: Node): EnumTypeName = {
    val BindNode(v697, v698) = node
    val v702 = v697.id match {
      case 116 =>
        val v699 = v698.asInstanceOf[SequenceNode].children(1)
        val BindNode(v700, v701) = v699
        assert(v700.id == 47)
        EnumTypeName(matchId(v701))
    }
    v702
  }

  def matchNamedConstructExpr(node: Node): NamedConstructExpr = {
    val BindNode(v252, v253) = node
    val v274 = v252.id match {
      case 348 =>
        val v254 = v253.asInstanceOf[SequenceNode].children.head
        val BindNode(v255, v256) = v254
        assert(v255.id == 102)
        val v257 = v253.asInstanceOf[SequenceNode].children(3)
        val BindNode(v258, v259) = v257
        assert(v258.id == 349)
        val v260 = v253.asInstanceOf[SequenceNode].children(1)
        val BindNode(v261, v262) = v260
        assert(v261.id == 159)
        val BindNode(v263, v264) = v262
        val v273 = v263.id match {
          case 136 =>
            None
          case 160 =>
            val BindNode(v265, v266) = v264
            val v272 = v265.id match {
              case 161 =>
                val BindNode(v267, v268) = v266
                assert(v267.id == 162)
                val v269 = v268.asInstanceOf[SequenceNode].children(1)
                val BindNode(v270, v271) = v269
                assert(v270.id == 123)
                matchSuperTypes(v271)
            }
            Some(v272)
        }
        NamedConstructExpr(matchTypeName(v256), matchNamedConstructParams(v259), v273)
    }
    v274
  }

  def matchTypeDesc(node: Node): TypeDesc = {
    val BindNode(v794, v795) = node
    val v813 = v794.id match {
      case 99 =>
        val v796 = v795.asInstanceOf[SequenceNode].children.head
        val BindNode(v797, v798) = v796
        assert(v797.id == 100)
        val v799 = v795.asInstanceOf[SequenceNode].children(1)
        val BindNode(v800, v801) = v799
        assert(v800.id == 188)
        val BindNode(v802, v803) = v801
        val v812 = v802.id match {
          case 136 =>
            None
          case 189 =>
            val BindNode(v804, v805) = v803
            val v811 = v804.id match {
              case 190 =>
                val BindNode(v806, v807) = v805
                assert(v806.id == 191)
                val v808 = v807.asInstanceOf[SequenceNode].children(1)
                val BindNode(v809, v810) = v808
                assert(v809.id == 192)
                v810.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v811)
        }
        TypeDesc(matchNonNullTypeDesc(v798), v812.isDefined)
    }
    v813
  }

  def matchSuperTypes(node: Node): List[TypeName] = {
    val BindNode(v48, v49) = node
    val v77 = v48.id match {
      case 124 =>
        val v51 = v49.asInstanceOf[SequenceNode].children(2)
        val BindNode(v52, v53) = v51
        assert(v52.id == 126)
        val BindNode(v54, v55) = v53
        val v76 = v54.id match {
          case 136 =>
            None
          case 127 =>
            val BindNode(v56, v57) = v55
            val v75 = v56.id match {
              case 128 =>
                val BindNode(v58, v59) = v57
                assert(v58.id == 129)
                val v60 = v59.asInstanceOf[SequenceNode].children.head
                val BindNode(v61, v62) = v60
                assert(v61.id == 102)
                val v63 = v59.asInstanceOf[SequenceNode].children(1)
                val v64 = unrollRepeat0(v63).map { elem =>
                  val BindNode(v65, v66) = elem
                  assert(v65.id == 132)
                  val BindNode(v67, v68) = v66
                  val v74 = v67.id match {
                    case 133 =>
                      val BindNode(v69, v70) = v68
                      assert(v69.id == 134)
                      val v71 = v70.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v72, v73) = v71
                      assert(v72.id == 102)
                      matchTypeName(v73)
                  }
                  v74
                }
                List(matchTypeName(v62)) ++ v64
            }
            Some(v75)
        }
        val v50 = v76
        if (v50.isDefined) v50.get else List()
    }
    v77
  }

  def matchParamName(node: Node): ParamName = {
    val BindNode(v954, v955) = node
    val v962 = v954.id match {
      case 43 =>
        val v956 = v955.asInstanceOf[SequenceNode].children.head
        val BindNode(v957, v958) = v956
        assert(v957.id == 44)
        ParamName(matchIdNoKeyword(v958))
      case 91 =>
        val v959 = v955.asInstanceOf[SequenceNode].children(1)
        val BindNode(v960, v961) = v959
        assert(v960.id == 47)
        ParamName(matchId(v961))
    }
    v962
  }

  def matchEnumValue(node: Node): AbstractEnumValue = {
    val BindNode(v703, v704) = node
    val v723 = v703.id match {
      case 395 =>
        val v705 = v704.asInstanceOf[SequenceNode].children.head
        val BindNode(v706, v707) = v705
        assert(v706.id == 396)
        val BindNode(v708, v709) = v707
        assert(v708.id == 397)
        val BindNode(v710, v711) = v709
        val v722 = v710.id match {
          case 398 =>
            val BindNode(v712, v713) = v711
            assert(v712.id == 399)
            val v714 = v713.asInstanceOf[SequenceNode].children.head
            val BindNode(v715, v716) = v714
            assert(v715.id == 400)
            matchCanonicalEnumValue(v716)
          case 404 =>
            val BindNode(v717, v718) = v711
            assert(v717.id == 405)
            val v719 = v718.asInstanceOf[SequenceNode].children.head
            val BindNode(v720, v721) = v719
            assert(v720.id == 406)
            matchShortenedEnumValue(v721)
        }
        v722
    }
    v723
  }

  def matchClassParamsDef(node: Node): List[ClassParamDef] = {
    val BindNode(v644, v645) = node
    val v673 = v644.id match {
      case 140 =>
        val v647 = v645.asInstanceOf[SequenceNode].children(2)
        val BindNode(v648, v649) = v647
        assert(v648.id == 142)
        val BindNode(v650, v651) = v649
        val v672 = v650.id match {
          case 136 =>
            None
          case 143 =>
            val BindNode(v652, v653) = v651
            val v671 = v652.id match {
              case 144 =>
                val BindNode(v654, v655) = v653
                assert(v654.id == 145)
                val v656 = v655.asInstanceOf[SequenceNode].children.head
                val BindNode(v657, v658) = v656
                assert(v657.id == 146)
                val v659 = v655.asInstanceOf[SequenceNode].children(1)
                val v660 = unrollRepeat0(v659).map { elem =>
                  val BindNode(v661, v662) = elem
                  assert(v661.id == 151)
                  val BindNode(v663, v664) = v662
                  val v670 = v663.id match {
                    case 152 =>
                      val BindNode(v665, v666) = v664
                      assert(v665.id == 153)
                      val v667 = v666.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v668, v669) = v667
                      assert(v668.id == 146)
                      matchClassParamDef(v669)
                  }
                  v670
                }
                List(matchClassParamDef(v658)) ++ v660
            }
            Some(v671)
        }
        val v646 = v672
        if (v646.isDefined) v646.get else List()
    }
    v673
  }

  def matchTerminalChar(node: Node): TerminalChar = {
    val BindNode(v1023, v1024) = node
    val v1036 = v1023.id match {
      case 227 =>
        val v1025 = v1024.asInstanceOf[SequenceNode].children.head
        val BindNode(v1026, v1027) = v1025
        assert(v1026.id == 228)
        val BindNode(v1028, v1029) = v1027
        assert(v1028.id == 24)
        CharAsIs(v1029.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)
      case 230 =>
        val v1030 = v1024.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1031, v1032) = v1030
        assert(v1031.id == 231)
        CharEscaped(v1032.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)
      case 232 =>
        val v1033 = v1024.asInstanceOf[SequenceNode].children.head
        val BindNode(v1034, v1035) = v1033
        assert(v1034.id == 233)
        matchUnicodeChar(v1035)
    }
    v1036
  }

  def matchAtomSymbol(node: Node): AtomSymbol = {
    val BindNode(v733, v734) = node
    val v756 = v733.id match {
      case 281 =>
        val v735 = v734.asInstanceOf[SequenceNode].children.head
        val BindNode(v736, v737) = v735
        assert(v736.id == 282)
        matchEmptySequence(v737)
      case 278 =>
        val v738 = v734.asInstanceOf[SequenceNode].children.head
        val BindNode(v739, v740) = v738
        assert(v739.id == 279)
        matchLongest(v740)
      case 268 =>
        val v741 = v734.asInstanceOf[SequenceNode].children.head
        val BindNode(v742, v743) = v741
        assert(v742.id == 40)
        matchNonterminal(v743)
      case 256 =>
        val v744 = v734.asInstanceOf[SequenceNode].children.head
        val BindNode(v745, v746) = v744
        assert(v745.id == 257)
        matchStringSymbol(v746)
      case 222 =>
        val v747 = v734.asInstanceOf[SequenceNode].children.head
        val BindNode(v748, v749) = v747
        assert(v748.id == 223)
        matchTerminal(v749)
      case 269 =>
        val v750 = v734.asInstanceOf[SequenceNode].children(2)
        val BindNode(v751, v752) = v750
        assert(v751.id == 270)
        matchInPlaceChoices(v752)
      case 238 =>
        val v753 = v734.asInstanceOf[SequenceNode].children.head
        val BindNode(v754, v755) = v753
        assert(v754.id == 239)
        matchTerminalChoice(v755)
    }
    v756
  }

  def matchTernaryExpr(node: Node): TernaryExpr = {
    val BindNode(v814, v815) = node
    val v848 = v814.id match {
      case 327 =>
        val v816 = v815.asInstanceOf[SequenceNode].children.head
        val BindNode(v817, v818) = v816
        assert(v817.id == 328)
        val v819 = v815.asInstanceOf[SequenceNode].children(4)
        val BindNode(v820, v821) = v819
        assert(v820.id == 432)
        val BindNode(v822, v823) = v821
        assert(v822.id == 433)
        val BindNode(v824, v825) = v823
        val v831 = v824.id match {
          case 434 =>
            val BindNode(v826, v827) = v825
            assert(v826.id == 435)
            val v828 = v827.asInstanceOf[SequenceNode].children.head
            val BindNode(v829, v830) = v828
            assert(v829.id == 326)
            matchTernaryExpr(v830)
        }
        val v832 = v815.asInstanceOf[SequenceNode].children(8)
        val BindNode(v833, v834) = v832
        assert(v833.id == 432)
        val BindNode(v835, v836) = v834
        assert(v835.id == 433)
        val BindNode(v837, v838) = v836
        val v844 = v837.id match {
          case 434 =>
            val BindNode(v839, v840) = v838
            assert(v839.id == 435)
            val v841 = v840.asInstanceOf[SequenceNode].children.head
            val BindNode(v842, v843) = v841
            assert(v842.id == 326)
            matchTernaryExpr(v843)
        }
        TernaryOp(matchBoolOrExpr(v818), v831, v844)
      case 436 =>
        val v845 = v815.asInstanceOf[SequenceNode].children.head
        val BindNode(v846, v847) = v845
        assert(v846.id == 328)
        matchBoolOrExpr(v847)
    }
    v848
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v275, v276) = node
    val v298 = v275.id match {
      case 122 =>
        val v277 = v276.asInstanceOf[SequenceNode].children.head
        val BindNode(v278, v279) = v277
        assert(v278.id == 102)
        val v280 = v276.asInstanceOf[SequenceNode].children(2)
        val BindNode(v281, v282) = v280
        assert(v281.id == 123)
        AbstractClassDef(matchTypeName(v279), matchSuperTypes(v282))
      case 138 =>
        val v283 = v276.asInstanceOf[SequenceNode].children.head
        val BindNode(v284, v285) = v283
        assert(v284.id == 102)
        val v286 = v276.asInstanceOf[SequenceNode].children(2)
        val BindNode(v287, v288) = v286
        assert(v287.id == 139)
        ConcreteClassDef(matchTypeName(v285), None, matchClassParamsDef(v288))
      case 155 =>
        val v289 = v276.asInstanceOf[SequenceNode].children.head
        val BindNode(v290, v291) = v289
        assert(v290.id == 102)
        val v292 = v276.asInstanceOf[SequenceNode].children(2)
        val BindNode(v293, v294) = v292
        assert(v293.id == 123)
        val v295 = v276.asInstanceOf[SequenceNode].children(4)
        val BindNode(v296, v297) = v295
        assert(v296.id == 139)
        ConcreteClassDef(matchTypeName(v291), Some(matchSuperTypes(v294)), matchClassParamsDef(v297))
    }
    v298
  }

  def matchElem(node: Node): Elem = {
    val BindNode(v724, v725) = node
    val v732 = v724.id match {
      case 202 =>
        val v726 = v725.asInstanceOf[SequenceNode].children.head
        val BindNode(v727, v728) = v726
        assert(v727.id == 203)
        matchSymbol(v728)
      case 287 =>
        val v729 = v725.asInstanceOf[SequenceNode].children.head
        val BindNode(v730, v731) = v729
        assert(v730.id == 288)
        matchProcessor(v731)
    }
    v732
  }

  def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
    val BindNode(v388, v389) = node
    val v396 = v388.id match {
      case 251 =>
        val v390 = v389.asInstanceOf[SequenceNode].children.head
        val BindNode(v391, v392) = v390
        assert(v391.id == 243)
        val v393 = v389.asInstanceOf[SequenceNode].children(2)
        val BindNode(v394, v395) = v393
        assert(v394.id == 243)
        TerminalChoiceRange(matchTerminalChoiceChar(v392), matchTerminalChoiceChar(v395))
    }
    v396
  }

  def matchValRef(node: Node): ValRef = {
    val BindNode(v411, v412) = node
    val v422 = v411.id match {
      case 293 =>
        val v413 = v412.asInstanceOf[SequenceNode].children(2)
        val BindNode(v414, v415) = v413
        assert(v414.id == 305)
        val v416 = v412.asInstanceOf[SequenceNode].children(1)
        val BindNode(v417, v418) = v416
        assert(v417.id == 295)
        val BindNode(v419, v420) = v418
        val v421 = v419.id match {
          case 136 =>
            None
          case 296 =>
            Some(matchCondSymPath(v420))
        }
        ValRef(matchRefIdx(v415), v421)
    }
    v422
  }

  def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
    val BindNode(v493, v494) = node
    val v501 = v493.id match {
      case 339 =>
        val v495 = v494.asInstanceOf[SequenceNode].children(2)
        val BindNode(v496, v497) = v495
        assert(v496.id == 338)
        PrefixOp(PreOp.NOT, matchPrefixNotExpr(v497))
      case 340 =>
        val v498 = v494.asInstanceOf[SequenceNode].children.head
        val BindNode(v499, v500) = v498
        assert(v499.id == 341)
        matchAtom(v500)
    }
    v501
  }

  def matchGrammar(node: Node): Grammar = {
    val BindNode(v1086, v1087) = node
    val v1103 = v1086.id match {
      case 3 =>
        val v1088 = v1087.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1089, v1090) = v1088
        assert(v1089.id == 34)
        val v1091 = v1087.asInstanceOf[SequenceNode].children(2)
        val v1092 = unrollRepeat0(v1091).map { elem =>
          val BindNode(v1093, v1094) = elem
          assert(v1093.id == 449)
          val BindNode(v1095, v1096) = v1094
          val v1102 = v1095.id match {
            case 450 =>
              val BindNode(v1097, v1098) = v1096
              assert(v1097.id == 451)
              val v1099 = v1098.asInstanceOf[SequenceNode].children(1)
              val BindNode(v1100, v1101) = v1099
              assert(v1100.id == 34)
              matchDef(v1101)
          }
          v1102
        }
        Grammar(List(matchDef(v1090)) ++ v1092)
    }
    v1103
  }

  def matchSubType(node: Node): SubType = {
    val BindNode(v766, v767) = node
    val v777 = v766.id match {
      case 101 =>
        val v768 = v767.asInstanceOf[SequenceNode].children.head
        val BindNode(v769, v770) = v768
        assert(v769.id == 102)
        matchTypeName(v770)
      case 120 =>
        val v771 = v767.asInstanceOf[SequenceNode].children.head
        val BindNode(v772, v773) = v771
        assert(v772.id == 121)
        matchClassDef(v773)
      case 156 =>
        val v774 = v767.asInstanceOf[SequenceNode].children.head
        val BindNode(v775, v776) = v774
        assert(v775.id == 157)
        matchSuperDef(v776)
    }
    v777
  }

  def matchValueType(node: Node): ValueType = {
    val BindNode(v249, v250) = node
    val v251 = v249.id match {
      case 58 =>
        BooleanType()
      case 67 =>
        CharType()
      case 73 =>
        StringType()
    }
    v251
  }

  def matchEmptySequence(node: Node): EmptySeq = {
    val BindNode(v122, v123) = node
    val v124 = v122.id match {
      case 283 =>
        EmptySeq()
    }
    v124
  }

  def matchClassParamDef(node: Node): ClassParamDef = {
    val BindNode(v1, v2) = node
    val v20 = v1.id match {
      case 147 =>
        val v3 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v4, v5) = v3
        assert(v4.id == 148)
        val v6 = v2.asInstanceOf[SequenceNode].children(1)
        val BindNode(v7, v8) = v6
        assert(v7.id == 93)
        val BindNode(v9, v10) = v8
        val v19 = v9.id match {
          case 136 =>
            None
          case 94 =>
            val BindNode(v11, v12) = v10
            val v18 = v11.id match {
              case 95 =>
                val BindNode(v13, v14) = v12
                assert(v13.id == 96)
                val v15 = v14.asInstanceOf[SequenceNode].children(3)
                val BindNode(v16, v17) = v15
                assert(v16.id == 98)
                matchTypeDesc(v17)
            }
            Some(v18)
        }
        ClassParamDef(matchParamName(v5), v19)
    }
    v20
  }

  def matchTerminalChoice(node: Node): TerminalChoice = {
    val BindNode(v923, v924) = node
    val v935 = v923.id match {
      case 240 =>
        val v925 = v924.asInstanceOf[SequenceNode].children(1)
        val BindNode(v926, v927) = v925
        assert(v926.id == 241)
        val v928 = v924.asInstanceOf[SequenceNode].children(2)
        val v929 = unrollRepeat1(v928).map { elem =>
          val BindNode(v930, v931) = elem
          assert(v930.id == 241)
          matchTerminalChoiceElem(v931)
        }
        TerminalChoice(List(matchTerminalChoiceElem(v927)) ++ v929)
      case 255 =>
        val v932 = v924.asInstanceOf[SequenceNode].children(1)
        val BindNode(v933, v934) = v932
        assert(v933.id == 250)
        TerminalChoice(List(matchTerminalChoiceRange(v934)))
    }
    v935
  }

  def matchBindExpr(node: Node): BindExpr = {
    val BindNode(v674, v675) = node
    val v682 = v674.id match {
      case 344 =>
        val v676 = v675.asInstanceOf[SequenceNode].children.head
        val BindNode(v677, v678) = v676
        assert(v677.id == 292)
        val v679 = v675.asInstanceOf[SequenceNode].children(1)
        val BindNode(v680, v681) = v679
        assert(v680.id == 345)
        BindExpr(matchValRef(v678), matchBinderExpr(v681))
    }
    v682
  }

  def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
    val BindNode(v864, v865) = node
    val v884 = v864.id match {
      case 114 =>
        val v866 = v865.asInstanceOf[SequenceNode].children.head
        val BindNode(v867, v868) = v866
        assert(v867.id == 115)
        matchEnumTypeName(v868)
      case 106 =>
        val v869 = v865.asInstanceOf[SequenceNode].children.head
        val BindNode(v870, v871) = v869
        assert(v870.id == 107)
        matchValueType(v871)
      case 103 =>
        val v872 = v865.asInstanceOf[SequenceNode].children(2)
        val BindNode(v873, v874) = v872
        assert(v873.id == 98)
        ArrayTypeDesc(matchTypeDesc(v874))
      case 118 =>
        val v875 = v865.asInstanceOf[SequenceNode].children.head
        val BindNode(v876, v877) = v875
        assert(v876.id == 119)
        matchTypeDef(v877)
      case 108 =>
        val v878 = v865.asInstanceOf[SequenceNode].children.head
        val BindNode(v879, v880) = v878
        assert(v879.id == 109)
        matchAnyType(v880)
      case 101 =>
        val v881 = v865.asInstanceOf[SequenceNode].children.head
        val BindNode(v882, v883) = v881
        assert(v882.id == 102)
        matchTypeName(v883)
    }
    v884
  }

  def matchLineComment(node: Node): String = {
    val BindNode(v221, v222) = node
    val v223 = v221.id match {
      case 16 =>
        ""
    }
    v223
  }

  def matchArrayExpr(node: Node): ArrayExpr = {
    val BindNode(v359, v360) = node
    val v387 = v359.id match {
      case 378 =>
        val v362 = v360.asInstanceOf[SequenceNode].children(2)
        val BindNode(v363, v364) = v362
        assert(v363.id == 367)
        val BindNode(v365, v366) = v364
        val v386 = v365.id match {
          case 136 =>
            None
          case 368 =>
            val BindNode(v367, v368) = v366
            assert(v367.id == 369)
            val BindNode(v369, v370) = v368
            assert(v369.id == 370)
            val v371 = v370.asInstanceOf[SequenceNode].children.head
            val BindNode(v372, v373) = v371
            assert(v372.id == 324)
            val v374 = v370.asInstanceOf[SequenceNode].children(1)
            val v375 = unrollRepeat0(v374).map { elem =>
              val BindNode(v376, v377) = elem
              assert(v376.id == 373)
              val BindNode(v378, v379) = v377
              val v385 = v378.id match {
                case 374 =>
                  val BindNode(v380, v381) = v379
                  assert(v380.id == 375)
                  val v382 = v381.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v383, v384) = v382
                  assert(v383.id == 324)
                  matchPExpr(v384)
              }
              v385
            }
            Some(List(matchPExpr(v373)) ++ v375)
        }
        val v361 = v386
        ArrayExpr(if (v361.isDefined) v361.get else List())
    }
    v387
  }

  def matchWS(node: Node): String = {
    val BindNode(v785, v786) = node
    val v787 = v785.id match {
      case 5 =>
        ""
    }
    v787
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v441, v442) = node
    val v460 = v441.id match {
      case 88 =>
        NullLiteral()
      case 381 =>
        val v443 = v442.asInstanceOf[SequenceNode].children.head
        val BindNode(v444, v445) = v443
        assert(v444.id == 382)
        val BindNode(v446, v447) = v445
        val v452 = v446.id match {
          case 383 =>
            val BindNode(v448, v449) = v447
            assert(v448.id == 80)
            true
          case 384 =>
            val BindNode(v450, v451) = v447
            assert(v450.id == 84)
            false
        }
        BoolLiteral(v452)
      case 385 =>
        val v453 = v442.asInstanceOf[SequenceNode].children(1)
        val BindNode(v454, v455) = v453
        assert(v454.id == 386)
        CharLiteral(matchCharChar(v455))
      case 388 =>
        val v456 = v442.asInstanceOf[SequenceNode].children(1)
        val v457 = unrollRepeat0(v456).map { elem =>
          val BindNode(v458, v459) = elem
          assert(v458.id == 391)
          matchStrChar(v459)
        }
        StrLiteral(v457)
    }
    v460
  }

  def matchBoolAndExpr(node: Node): BoolAndExpr = {
    val BindNode(v481, v482) = node
    val v492 = v481.id match {
      case 331 =>
        val v483 = v482.asInstanceOf[SequenceNode].children.head
        val BindNode(v484, v485) = v483
        assert(v484.id == 332)
        val v486 = v482.asInstanceOf[SequenceNode].children(4)
        val BindNode(v487, v488) = v486
        assert(v487.id == 330)
        BinOp(Op.OR, matchBoolEqExpr(v485), matchBoolAndExpr(v488))
      case 428 =>
        val v489 = v482.asInstanceOf[SequenceNode].children.head
        val BindNode(v490, v491) = v489
        assert(v490.id == 332)
        matchBoolEqExpr(v491)
    }
    v492
  }

  def matchStringChar(node: Node): StringChar = {
    val BindNode(v397, v398) = node
    val v410 = v397.id match {
      case 263 =>
        val v399 = v398.asInstanceOf[SequenceNode].children.head
        val BindNode(v400, v401) = v399
        assert(v400.id == 264)
        val BindNode(v402, v403) = v401
        assert(v402.id == 24)
        CharAsIs(v403.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)
      case 266 =>
        val v404 = v398.asInstanceOf[SequenceNode].children(1)
        val BindNode(v405, v406) = v404
        assert(v405.id == 267)
        CharEscaped(v406.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)
      case 232 =>
        val v407 = v398.asInstanceOf[SequenceNode].children.head
        val BindNode(v408, v409) = v407
        assert(v408.id == 233)
        matchUnicodeChar(v409)
    }
    v410
  }

  def matchEOF(node: Node): String = {
    val BindNode(v763, v764) = node
    val v765 = v763.id match {
      case 30 =>
        ""
    }
    v765
  }

  def matchElvisExpr(node: Node): ElvisExpr = {
    val BindNode(v1074, v1075) = node
    val v1085 = v1074.id match {
      case 335 =>
        val v1076 = v1075.asInstanceOf[SequenceNode].children.head
        val BindNode(v1077, v1078) = v1076
        assert(v1077.id == 336)
        val v1079 = v1075.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1080, v1081) = v1079
        assert(v1080.id == 334)
        Elvis(matchAdditiveExpr(v1078), matchElvisExpr(v1081))
      case 415 =>
        val v1082 = v1075.asInstanceOf[SequenceNode].children.head
        val BindNode(v1083, v1084) = v1082
        assert(v1083.id == 336)
        matchAdditiveExpr(v1084)
    }
    v1085
  }

  def matchPExpr(node: Node): PExpr = {
    val BindNode(v339, v340) = node
    val v358 = v339.id match {
      case 325 =>
        val v341 = v340.asInstanceOf[SequenceNode].children.head
        val BindNode(v342, v343) = v341
        assert(v342.id == 326)
        val v344 = v340.asInstanceOf[SequenceNode].children(1)
        val BindNode(v345, v346) = v344
        assert(v345.id == 93)
        val BindNode(v347, v348) = v346
        val v357 = v347.id match {
          case 136 =>
            None
          case 94 =>
            val BindNode(v349, v350) = v348
            val v356 = v349.id match {
              case 95 =>
                val BindNode(v351, v352) = v350
                assert(v351.id == 96)
                val v353 = v352.asInstanceOf[SequenceNode].children(3)
                val BindNode(v354, v355) = v353
                assert(v354.id == 98)
                matchTypeDesc(v355)
            }
            Some(v356)
        }
        PExpr(matchTernaryExpr(v343), v357)
    }
    v358
  }

  def matchProcessor(node: Node): Processor = {
    val BindNode(v227, v228) = node
    val v235 = v227.id match {
      case 289 =>
        val v229 = v228.asInstanceOf[SequenceNode].children.head
        val BindNode(v230, v231) = v229
        assert(v230.id == 290)
        matchRef(v231)
      case 323 =>
        val v232 = v228.asInstanceOf[SequenceNode].children(2)
        val BindNode(v233, v234) = v232
        assert(v233.id == 324)
        matchPExpr(v234)
    }
    v235
  }

  def matchNamedConstructParams(node: Node): List[NamedParam] = {
    val BindNode(v299, v300) = node
    val v324 = v299.id match {
      case 350 =>
        val v301 = v300.asInstanceOf[SequenceNode].children(2)
        val BindNode(v302, v303) = v301
        assert(v302.id == 351)
        val BindNode(v304, v305) = v303
        val v323 = v304.id match {
          case 352 =>
            val BindNode(v306, v307) = v305
            assert(v306.id == 353)
            val v308 = v307.asInstanceOf[SequenceNode].children.head
            val BindNode(v309, v310) = v308
            assert(v309.id == 354)
            val v311 = v307.asInstanceOf[SequenceNode].children(1)
            val v312 = unrollRepeat0(v311).map { elem =>
              val BindNode(v313, v314) = elem
              assert(v313.id == 358)
              val BindNode(v315, v316) = v314
              val v322 = v315.id match {
                case 359 =>
                  val BindNode(v317, v318) = v316
                  assert(v317.id == 360)
                  val v319 = v318.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v320, v321) = v319
                  assert(v320.id == 354)
                  matchNamedParam(v321)
              }
              v322
            }
            List(matchNamedParam(v310)) ++ v312
        }
        v323
    }
    v324
  }

  def matchNonterminalName(node: Node): NonterminalName = {
    val BindNode(v855, v856) = node
    val v863 = v855.id match {
      case 43 =>
        val v857 = v856.asInstanceOf[SequenceNode].children.head
        val BindNode(v858, v859) = v857
        assert(v858.id == 44)
        NonterminalName(matchIdNoKeyword(v859))
      case 91 =>
        val v860 = v856.asInstanceOf[SequenceNode].children(1)
        val BindNode(v861, v862) = v860
        assert(v861.id == 47)
        NonterminalName(matchId(v862))
    }
    v863
  }

  def matchStrChar(node: Node): StringChar = {
    val BindNode(v849, v850) = node
    val v854 = v849.id match {
      case 392 =>
        val v851 = v850.asInstanceOf[SequenceNode].children.head
        val BindNode(v852, v853) = v851
        assert(v852.id == 262)
        matchStringChar(v853)
    }
    v854
  }

  def matchPreUnSymbol(node: Node): PreUnSymbol = {
    val BindNode(v963, v964) = node
    val v974 = v963.id match {
      case 209 =>
        val v965 = v964.asInstanceOf[SequenceNode].children(2)
        val BindNode(v966, v967) = v965
        assert(v966.id == 208)
        FollowedBy(matchPreUnSymbol(v967))
      case 211 =>
        val v968 = v964.asInstanceOf[SequenceNode].children(2)
        val BindNode(v969, v970) = v968
        assert(v969.id == 208)
        NotFollowedBy(matchPreUnSymbol(v970))
      case 213 =>
        val v971 = v964.asInstanceOf[SequenceNode].children.head
        val BindNode(v972, v973) = v971
        assert(v972.id == 214)
        matchPostUnSymbol(v973)
    }
    v974
  }

  def matchTerminal(node: Node): Terminal = {
    val BindNode(v138, v139) = node
    val v143 = v138.id match {
      case 224 =>
        val v140 = v139.asInstanceOf[SequenceNode].children(1)
        val BindNode(v141, v142) = v140
        assert(v141.id == 226)
        matchTerminalChar(v142)
      case 236 =>
        AnyTerminal()
    }
    v143
  }

  def matchBoolEqExpr(node: Node): BoolEqExpr = {
    val BindNode(v1046, v1047) = node
    val v1067 = v1046.id match {
      case 333 =>
        val v1048 = v1047.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1049, v1050) = v1048
        assert(v1049.id == 416)
        val BindNode(v1051, v1052) = v1050
        val v1057 = v1051.id match {
          case 417 =>
            val BindNode(v1053, v1054) = v1052
            assert(v1053.id == 418)
            Op.EQ
          case 421 =>
            val BindNode(v1055, v1056) = v1052
            assert(v1055.id == 422)
            Op.NE
        }
        val v1058 = v1047.asInstanceOf[SequenceNode].children.head
        val BindNode(v1059, v1060) = v1058
        assert(v1059.id == 334)
        val v1061 = v1047.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1062, v1063) = v1061
        assert(v1062.id == 332)
        BinOp(v1057, matchElvisExpr(v1060), matchBoolEqExpr(v1063))
      case 425 =>
        val v1064 = v1047.asInstanceOf[SequenceNode].children.head
        val BindNode(v1065, v1066) = v1064
        assert(v1065.id == 334)
        matchElvisExpr(v1066)
    }
    v1067
  }

  def matchSymbol(node: Node): Symbol = {
    val BindNode(v788, v789) = node
    val v793 = v788.id match {
      case 204 =>
        val v790 = v789.asInstanceOf[SequenceNode].children.head
        val BindNode(v791, v792) = v790
        assert(v791.id == 205)
        matchBinSymbol(v792)
    }
    v793
  }

  def matchLHS(node: Node): LHS = {
    val BindNode(v461, v462) = node
    val v480 = v461.id match {
      case 39 =>
        val v463 = v462.asInstanceOf[SequenceNode].children.head
        val BindNode(v464, v465) = v463
        assert(v464.id == 40)
        val v466 = v462.asInstanceOf[SequenceNode].children(1)
        val BindNode(v467, v468) = v466
        assert(v467.id == 93)
        val BindNode(v469, v470) = v468
        val v479 = v469.id match {
          case 136 =>
            None
          case 94 =>
            val BindNode(v471, v472) = v470
            val v478 = v471.id match {
              case 95 =>
                val BindNode(v473, v474) = v472
                assert(v473.id == 96)
                val v475 = v474.asInstanceOf[SequenceNode].children(3)
                val BindNode(v476, v477) = v475
                assert(v476.id == 98)
                matchTypeDesc(v477)
            }
            Some(v478)
        }
        LHS(matchNonterminal(v465), v479)
    }
    v480
  }

  def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
    val BindNode(v936, v937) = node
    val v941 = v936.id match {
      case 407 =>
        val v938 = v937.asInstanceOf[SequenceNode].children(1)
        val BindNode(v939, v940) = v938
        assert(v939.id == 402)
        ShortenedEnumValue(matchEnumValueName(v940))
    }
    v941
  }

  def matchEnumTypeDef(node: Node): EnumTypeDef = {
    val BindNode(v186, v187) = node
    val v214 = v186.id match {
      case 179 =>
        val v188 = v187.asInstanceOf[SequenceNode].children.head
        val BindNode(v189, v190) = v188
        assert(v189.id == 115)
        val v191 = v187.asInstanceOf[SequenceNode].children(4)
        val BindNode(v192, v193) = v191
        assert(v192.id == 180)
        val BindNode(v194, v195) = v193
        val v213 = v194.id match {
          case 181 =>
            val BindNode(v196, v197) = v195
            assert(v196.id == 182)
            val v198 = v197.asInstanceOf[SequenceNode].children.head
            val BindNode(v199, v200) = v198
            assert(v199.id == 47)
            val v201 = v197.asInstanceOf[SequenceNode].children(1)
            val v202 = unrollRepeat0(v201).map { elem =>
              val BindNode(v203, v204) = elem
              assert(v203.id == 185)
              val BindNode(v205, v206) = v204
              val v212 = v205.id match {
                case 186 =>
                  val BindNode(v207, v208) = v206
                  assert(v207.id == 187)
                  val v209 = v208.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v210, v211) = v209
                  assert(v210.id == 47)
                  matchId(v211)
              }
              v212
            }
            List(matchId(v200)) ++ v202
        }
        EnumTypeDef(matchEnumTypeName(v190), v213)
    }
    v214
  }

  def matchRefIdx(node: Node): String = {
    val BindNode(v640, v641) = node
    val v643 = v640.id match {
      case 306 =>
        val v642 = v641.asInstanceOf[SequenceNode].children.head
        v642.sourceText
    }
    v643
  }

  def matchAnyType(node: Node): AnyType = {
    val BindNode(v224, v225) = node
    val v226 = v224.id match {
      case 110 =>
        AnyType()
    }
    v226
  }

  def matchWSNL(node: Node): String = {
    val BindNode(v423, v424) = node
    val v425 = v423.id match {
      case 453 =>
        ""
    }
    v425
  }

  def matchEnumValueName(node: Node): EnumValueName = {
    val BindNode(v1068, v1069) = node
    val v1073 = v1068.id match {
      case 403 =>
        val v1070 = v1069.asInstanceOf[SequenceNode].children.head
        val BindNode(v1071, v1072) = v1070
        assert(v1071.id == 47)
        EnumValueName(matchId(v1072))
    }
    v1073
  }

  def matchInPlaceChoices(node: Node): InPlaceChoices = {
    val BindNode(v529, v530) = node
    val v546 = v529.id match {
      case 271 =>
        val v531 = v530.asInstanceOf[SequenceNode].children.head
        val BindNode(v532, v533) = v531
        assert(v532.id == 199)
        val v534 = v530.asInstanceOf[SequenceNode].children(1)
        val v535 = unrollRepeat0(v534).map { elem =>
          val BindNode(v536, v537) = elem
          assert(v536.id == 274)
          val BindNode(v538, v539) = v537
          val v545 = v538.id match {
            case 275 =>
              val BindNode(v540, v541) = v539
              assert(v540.id == 276)
              val v542 = v541.asInstanceOf[SequenceNode].children(3)
              val BindNode(v543, v544) = v542
              assert(v543.id == 199)
              matchSequence(v544)
          }
          v545
        }
        InPlaceChoices(List(matchSequence(v533)) ++ v535)
    }
    v546
  }

  def matchKeyword(node: Node): KeyWord.Value = {
    val BindNode(v951, v952) = node
    val v953 = v951.id match {
      case 84 =>
        KeyWord.FALSE
      case 80 =>
        KeyWord.TRUE
      case 88 =>
        KeyWord.NULL
      case 67 =>
        KeyWord.CHAR
      case 73 =>
        KeyWord.STRING
      case 58 =>
        KeyWord.BOOLEAN
    }
    v953
  }

  def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
    val BindNode(v502, v503) = node
    val v510 = v502.id match {
      case 363 =>
        val v504 = v503.asInstanceOf[SequenceNode].children.head
        val BindNode(v505, v506) = v504
        assert(v505.id == 364)
        val v507 = v503.asInstanceOf[SequenceNode].children(2)
        val BindNode(v508, v509) = v507
        assert(v508.id == 365)
        FuncCallOrConstructExpr(matchTypeOrFuncName(v506), matchCallParams(v509))
    }
    v510
  }

  def matchAdditiveExpr(node: Node): AdditiveExpr = {
    val BindNode(v90, v91) = node
    val v109 = v90.id match {
      case 337 =>
        val v92 = v91.asInstanceOf[SequenceNode].children(2)
        val BindNode(v93, v94) = v92
        assert(v93.id == 409)
        val BindNode(v95, v96) = v94
        val v99 = v95.id match {
          case 410 =>
            val BindNode(v97, v98) = v96
            assert(v97.id == 411)
            Op.ADD
        }
        val v100 = v91.asInstanceOf[SequenceNode].children.head
        val BindNode(v101, v102) = v100
        assert(v101.id == 338)
        val v103 = v91.asInstanceOf[SequenceNode].children(4)
        val BindNode(v104, v105) = v103
        assert(v104.id == 336)
        BinOp(v99, matchPrefixNotExpr(v102), matchAdditiveExpr(v105))
      case 412 =>
        val v106 = v91.asInstanceOf[SequenceNode].children.head
        val BindNode(v107, v108) = v106
        assert(v107.id == 338)
        matchPrefixNotExpr(v108)
    }
    v109
  }

  def matchCharChar(node: Node): TerminalChar = {
    val BindNode(v171, v172) = node
    val v176 = v171.id match {
      case 387 =>
        val v173 = v172.asInstanceOf[SequenceNode].children.head
        val BindNode(v174, v175) = v173
        assert(v174.id == 226)
        matchTerminalChar(v175)
    }
    v176
  }

  def matchTypeName(node: Node): TypeName = {
    val BindNode(v236, v237) = node
    val v244 = v236.id match {
      case 43 =>
        val v238 = v237.asInstanceOf[SequenceNode].children.head
        val BindNode(v239, v240) = v238
        assert(v239.id == 44)
        TypeName(matchIdNoKeyword(v240))
      case 91 =>
        val v241 = v237.asInstanceOf[SequenceNode].children(1)
        val BindNode(v242, v243) = v241
        assert(v242.id == 47)
        TypeName(matchId(v243))
    }
    v244
  }

  def matchRHS(node: Node): Sequence = {
    val BindNode(v757, v758) = node
    val v762 = v757.id match {
      case 198 =>
        val v759 = v758.asInstanceOf[SequenceNode].children.head
        val BindNode(v760, v761) = v759
        assert(v760.id == 199)
        matchSequence(v761)
    }
    v762
  }

  def matchSubTypes(node: Node): List[SubType] = {
    val BindNode(v622, v623) = node
    val v639 = v622.id match {
      case 169 =>
        val v624 = v623.asInstanceOf[SequenceNode].children.head
        val BindNode(v625, v626) = v624
        assert(v625.id == 170)
        val v627 = v623.asInstanceOf[SequenceNode].children(1)
        val v628 = unrollRepeat0(v627).map { elem =>
          val BindNode(v629, v630) = elem
          assert(v629.id == 173)
          val BindNode(v631, v632) = v630
          val v638 = v631.id match {
            case 174 =>
              val BindNode(v633, v634) = v632
              assert(v633.id == 175)
              val v635 = v634.asInstanceOf[SequenceNode].children(3)
              val BindNode(v636, v637) = v635
              assert(v636.id == 170)
              matchSubType(v637)
          }
          v638
        }
        List(matchSubType(v626)) ++ v628
    }
    v639
  }

  def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
    val BindNode(v942, v943) = node
    val v950 = v942.id match {
      case 43 =>
        val v944 = v943.asInstanceOf[SequenceNode].children.head
        val BindNode(v945, v946) = v944
        assert(v945.id == 44)
        TypeOrFuncName(matchIdNoKeyword(v946))
      case 91 =>
        val v947 = v943.asInstanceOf[SequenceNode].children(1)
        val BindNode(v948, v949) = v947
        assert(v948.id == 47)
        TypeOrFuncName(matchId(v949))
    }
    v950
  }

  def matchBoolOrExpr(node: Node): BoolOrExpr = {
    val BindNode(v78, v79) = node
    val v89 = v78.id match {
      case 329 =>
        val v80 = v79.asInstanceOf[SequenceNode].children.head
        val BindNode(v81, v82) = v80
        assert(v81.id == 330)
        val v83 = v79.asInstanceOf[SequenceNode].children(4)
        val BindNode(v84, v85) = v83
        assert(v84.id == 328)
        BinOp(Op.AND, matchBoolAndExpr(v82), matchBoolOrExpr(v85))
      case 431 =>
        val v86 = v79.asInstanceOf[SequenceNode].children.head
        val BindNode(v87, v88) = v86
        assert(v87.id == 330)
        matchBoolAndExpr(v88)
    }
    v89
  }

  def matchStart(node: Node): Grammar = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchGrammar(body)
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[Grammar, ParsingErrors.ParsingError] =
    ParseTreeUtil.parseAst(naiveParser, text, matchStart)

  def main(args: Array[String]): Unit = {
    //    println(parseAst("A = 'abc' 'd-f'"))
    //    println(parseAst("A = '\\-'"))
    //    println(parseAst("A = '--'"))
    //    println(parseAst("MyClass()"))
    //    println(parseAst(
    //      """Class2<Super1,Super2>
    //        |Class2<>(param1:string, param2:string)
    //        |Class2(param1:string)
    //        |""".stripMargin))
    //    println(parseAst(
    //      """SuperClass<GrandSuper> {
    //        |  SomeClass,
    //        |  SubSuperClass<AnotherClass> {
    //        |    IndirectSubConcreteClass(param1:string)
    //        |  },
    //        |  DirectSubConcreteClass()
    //        |}""".stripMargin))
    println(parseAst("""A = 'a' {$0?"abc":"def":string}""".stripMargin))
    println(parseAst("""A = 'a' {$0?:"abc":string}""".stripMargin))
    println(parseAst("""A = 'a' {($0?:"abc"):string}""".stripMargin))
    println(parseAst("""A = 'a' {$0?:("abc":string):string}""".stripMargin))
    println(parseAst("""A = 'a' {true && ispresent($<0)} {null}""".stripMargin))
    println(parseAst("""StartSymbol = AnotherNonterminal {null ?: "string"}""".stripMargin))
  }
}
