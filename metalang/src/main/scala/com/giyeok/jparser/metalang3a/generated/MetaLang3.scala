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

  def matchEOF(node: Node): String = {
    val BindNode(v759, v760) = node
    val v761 = v759.id match {
      case 30 =>
        ""
    }
    v761
  }

  def matchLineComment(node: Node): String = {
    val BindNode(v221, v222) = node
    val v223 = v221.id match {
      case 16 =>
        ""
    }
    v223
  }

  def matchSymbol(node: Node): Symbol = {
    val BindNode(v784, v785) = node
    val v789 = v784.id match {
      case 204 =>
        val v786 = v785.asInstanceOf[SequenceNode].children.head
        val BindNode(v787, v788) = v786
        assert(v787.id == 205)
        matchBinSymbol(v788)
    }
    v789
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

  def matchSequence(node: Node): Sequence = {
    val BindNode(v509, v510) = node
    val v526 = v509.id match {
      case 200 =>
        val v511 = v510.asInstanceOf[SequenceNode].children.head
        val BindNode(v512, v513) = v511
        assert(v512.id == 201)
        val v514 = v510.asInstanceOf[SequenceNode].children(1)
        val v515 = unrollRepeat0(v514).map { elem =>
          val BindNode(v516, v517) = elem
          assert(v516.id == 439)
          val BindNode(v518, v519) = v517
          val v525 = v518.id match {
            case 440 =>
              val BindNode(v520, v521) = v519
              assert(v520.id == 441)
              val v522 = v521.asInstanceOf[SequenceNode].children(1)
              val BindNode(v523, v524) = v522
              assert(v523.id == 201)
              matchElem(v524)
          }
          v525
        }
        Sequence(List(matchElem(v513)) ++ v515)
    }
    v526
  }

  def matchSubType(node: Node): SubType = {
    val BindNode(v762, v763) = node
    val v773 = v762.id match {
      case 101 =>
        val v764 = v763.asInstanceOf[SequenceNode].children.head
        val BindNode(v765, v766) = v764
        assert(v765.id == 102)
        matchTypeName(v766)
      case 120 =>
        val v767 = v763.asInstanceOf[SequenceNode].children.head
        val BindNode(v768, v769) = v767
        assert(v768.id == 121)
        matchClassDef(v769)
      case 156 =>
        val v770 = v763.asInstanceOf[SequenceNode].children.head
        val BindNode(v771, v772) = v770
        assert(v771.id == 157)
        matchSuperDef(v772)
    }
    v773
  }

  def matchWS(node: Node): String = {
    val BindNode(v781, v782) = node
    val v783 = v781.id match {
      case 5 =>
        ""
    }
    v783
  }

  def matchAtomSymbol(node: Node): AtomSymbol = {
    val BindNode(v729, v730) = node
    val v752 = v729.id match {
      case 281 =>
        val v731 = v730.asInstanceOf[SequenceNode].children.head
        val BindNode(v732, v733) = v731
        assert(v732.id == 282)
        matchEmptySequence(v733)
      case 278 =>
        val v734 = v730.asInstanceOf[SequenceNode].children.head
        val BindNode(v735, v736) = v734
        assert(v735.id == 279)
        matchLongest(v736)
      case 268 =>
        val v737 = v730.asInstanceOf[SequenceNode].children.head
        val BindNode(v738, v739) = v737
        assert(v738.id == 40)
        matchNonterminal(v739)
      case 256 =>
        val v740 = v730.asInstanceOf[SequenceNode].children.head
        val BindNode(v741, v742) = v740
        assert(v741.id == 257)
        matchStringSymbol(v742)
      case 222 =>
        val v743 = v730.asInstanceOf[SequenceNode].children.head
        val BindNode(v744, v745) = v743
        assert(v744.id == 223)
        matchTerminal(v745)
      case 269 =>
        val v746 = v730.asInstanceOf[SequenceNode].children(2)
        val BindNode(v747, v748) = v746
        assert(v747.id == 270)
        matchInPlaceChoices(v748)
      case 238 =>
        val v749 = v730.asInstanceOf[SequenceNode].children.head
        val BindNode(v750, v751) = v749
        assert(v750.id == 239)
        matchTerminalChoice(v751)
    }
    v752
  }

  def matchTerminalChoice(node: Node): TerminalChoice = {
    val BindNode(v919, v920) = node
    val v931 = v919.id match {
      case 240 =>
        val v921 = v920.asInstanceOf[SequenceNode].children(1)
        val BindNode(v922, v923) = v921
        assert(v922.id == 241)
        val v924 = v920.asInstanceOf[SequenceNode].children(2)
        val v925 = unrollRepeat1(v924).map { elem =>
          val BindNode(v926, v927) = elem
          assert(v926.id == 241)
          matchTerminalChoiceElem(v927)
        }
        TerminalChoice(List(matchTerminalChoiceElem(v923)) ++ v925)
      case 255 =>
        val v928 = v920.asInstanceOf[SequenceNode].children(1)
        val BindNode(v929, v930) = v928
        assert(v929.id == 250)
        TerminalChoice(List(matchTerminalChoiceRange(v930)))
    }
    v931
  }

  def matchLongest(node: Node): Longest = {
    val BindNode(v881, v882) = node
    val v886 = v881.id match {
      case 280 =>
        val v883 = v882.asInstanceOf[SequenceNode].children(2)
        val BindNode(v884, v885) = v883
        assert(v884.id == 270)
        Longest(matchInPlaceChoices(v885))
    }
    v886
  }

  def matchStrChar(node: Node): StringChar = {
    val BindNode(v845, v846) = node
    val v850 = v845.id match {
      case 392 =>
        val v847 = v846.asInstanceOf[SequenceNode].children.head
        val BindNode(v848, v849) = v847
        assert(v848.id == 262)
        matchStringChar(v849)
    }
    v850
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

  def matchStringSymbol(node: Node): StringSymbol = {
    val BindNode(v774, v775) = node
    val v780 = v774.id match {
      case 258 =>
        val v776 = v775.asInstanceOf[SequenceNode].children(1)
        val v777 = unrollRepeat0(v776).map { elem =>
          val BindNode(v778, v779) = elem
          assert(v778.id == 262)
          matchStringChar(v779)
        }
        StringSymbol(v777)
    }
    v780
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

  def matchRefIdx(node: Node): String = {
    val BindNode(v638, v639) = node
    val v641 = v638.id match {
      case 306 =>
        val v640 = v639.asInstanceOf[SequenceNode].children.head
        v640.sourceText
    }
    v641
  }

  def matchDef(node: Node): Def = {
    val BindNode(v910, v911) = node
    val v918 = v910.id match {
      case 35 =>
        val v912 = v911.asInstanceOf[SequenceNode].children.head
        val BindNode(v913, v914) = v912
        assert(v913.id == 36)
        matchRule(v914)
      case 118 =>
        val v915 = v911.asInstanceOf[SequenceNode].children.head
        val BindNode(v916, v917) = v915
        assert(v916.id == 119)
        matchTypeDef(v917)
    }
    v918
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

  def matchTernaryExpr(node: Node): TernaryExpr = {
    val BindNode(v810, v811) = node
    val v844 = v810.id match {
      case 327 =>
        val v812 = v811.asInstanceOf[SequenceNode].children.head
        val BindNode(v813, v814) = v812
        assert(v813.id == 328)
        val v815 = v811.asInstanceOf[SequenceNode].children(4)
        val BindNode(v816, v817) = v815
        assert(v816.id == 432)
        val BindNode(v818, v819) = v817
        assert(v818.id == 433)
        val BindNode(v820, v821) = v819
        val v827 = v820.id match {
          case 434 =>
            val BindNode(v822, v823) = v821
            assert(v822.id == 435)
            val v824 = v823.asInstanceOf[SequenceNode].children.head
            val BindNode(v825, v826) = v824
            assert(v825.id == 326)
            matchTernaryExpr(v826)
        }
        val v828 = v811.asInstanceOf[SequenceNode].children(8)
        val BindNode(v829, v830) = v828
        assert(v829.id == 432)
        val BindNode(v831, v832) = v830
        assert(v831.id == 433)
        val BindNode(v833, v834) = v832
        val v840 = v833.id match {
          case 434 =>
            val BindNode(v835, v836) = v834
            assert(v835.id == 435)
            val v837 = v836.asInstanceOf[SequenceNode].children.head
            val BindNode(v838, v839) = v837
            assert(v838.id == 326)
            matchTernaryExpr(v839)
        }
        TernaryOp(matchBoolOrExpr(v814), v827, v840)
      case 436 =>
        val v841 = v811.asInstanceOf[SequenceNode].children.head
        val BindNode(v842, v843) = v841
        assert(v842.id == 328)
        matchBoolOrExpr(v843)
    }
    v844
  }

  def matchElvisExpr(node: Node): ElvisExpr = {
    val BindNode(v1068, v1069) = node
    val v1079 = v1068.id match {
      case 335 =>
        val v1070 = v1069.asInstanceOf[SequenceNode].children.head
        val BindNode(v1071, v1072) = v1070
        assert(v1071.id == 336)
        val v1073 = v1069.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1074, v1075) = v1073
        assert(v1074.id == 334)
        Elvis(matchAdditiveExpr(v1072), matchElvisExpr(v1075))
      case 415 =>
        val v1076 = v1069.asInstanceOf[SequenceNode].children.head
        val BindNode(v1077, v1078) = v1076
        assert(v1077.id == 336)
        matchAdditiveExpr(v1078)
    }
    v1079
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

  def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
    val BindNode(v938, v939) = node
    val v946 = v938.id match {
      case 43 =>
        val v940 = v939.asInstanceOf[SequenceNode].children.head
        val BindNode(v941, v942) = v940
        assert(v941.id == 44)
        TypeOrFuncName(matchIdNoKeyword(v942))
      case 91 =>
        val v943 = v939.asInstanceOf[SequenceNode].children(1)
        val BindNode(v944, v945) = v943
        assert(v944.id == 47)
        TypeOrFuncName(matchId(v945))
    }
    v946
  }

  def matchSubTypes(node: Node): List[SubType] = {
    val BindNode(v620, v621) = node
    val v637 = v620.id match {
      case 169 =>
        val v622 = v621.asInstanceOf[SequenceNode].children.head
        val BindNode(v623, v624) = v622
        assert(v623.id == 170)
        val v625 = v621.asInstanceOf[SequenceNode].children(1)
        val v626 = unrollRepeat0(v625).map { elem =>
          val BindNode(v627, v628) = elem
          assert(v627.id == 173)
          val BindNode(v629, v630) = v628
          val v636 = v629.id match {
            case 174 =>
              val BindNode(v631, v632) = v630
              assert(v631.id == 175)
              val v633 = v632.asInstanceOf[SequenceNode].children(3)
              val BindNode(v634, v635) = v633
              assert(v634.id == 170)
              matchSubType(v635)
          }
          v636
        }
        List(matchSubType(v624)) ++ v626
    }
    v637
  }

  def matchRule(node: Node): Rule = {
    val BindNode(v579, v580) = node
    val v607 = v579.id match {
      case 37 =>
        val v581 = v580.asInstanceOf[SequenceNode].children.head
        val BindNode(v582, v583) = v581
        assert(v582.id == 38)
        val v584 = v580.asInstanceOf[SequenceNode].children(4)
        val BindNode(v585, v586) = v584
        assert(v585.id == 194)
        val BindNode(v587, v588) = v586
        val v606 = v587.id match {
          case 195 =>
            val BindNode(v589, v590) = v588
            assert(v589.id == 196)
            val v591 = v590.asInstanceOf[SequenceNode].children.head
            val BindNode(v592, v593) = v591
            assert(v592.id == 197)
            val v594 = v590.asInstanceOf[SequenceNode].children(1)
            val v595 = unrollRepeat0(v594).map { elem =>
              val BindNode(v596, v597) = elem
              assert(v596.id == 444)
              val BindNode(v598, v599) = v597
              val v605 = v598.id match {
                case 445 =>
                  val BindNode(v600, v601) = v599
                  assert(v600.id == 446)
                  val v602 = v601.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v603, v604) = v602
                  assert(v603.id == 197)
                  matchRHS(v604)
              }
              v605
            }
            List(matchRHS(v593)) ++ v595
        }
        Rule(matchLHS(v583), v606)
    }
    v607
  }

  def matchUnicodeChar(node: Node): CharUnicode = {
    val BindNode(v424, v425) = node
    val v438 = v424.id match {
      case 234 =>
        val v426 = v425.asInstanceOf[SequenceNode].children(2)
        val BindNode(v427, v428) = v426
        assert(v427.id == 235)
        val v429 = v425.asInstanceOf[SequenceNode].children(3)
        val BindNode(v430, v431) = v429
        assert(v430.id == 235)
        val v432 = v425.asInstanceOf[SequenceNode].children(4)
        val BindNode(v433, v434) = v432
        assert(v433.id == 235)
        val v435 = v425.asInstanceOf[SequenceNode].children(5)
        val BindNode(v436, v437) = v435
        assert(v436.id == 235)
        CharUnicode(List(v428.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0), v431.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0), v434.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0), v437.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0)))
    }
    v438
  }

  def matchEnumValue(node: Node): AbstractEnumValue = {
    val BindNode(v699, v700) = node
    val v719 = v699.id match {
      case 395 =>
        val v701 = v700.asInstanceOf[SequenceNode].children.head
        val BindNode(v702, v703) = v701
        assert(v702.id == 396)
        val BindNode(v704, v705) = v703
        assert(v704.id == 397)
        val BindNode(v706, v707) = v705
        val v718 = v706.id match {
          case 398 =>
            val BindNode(v708, v709) = v707
            assert(v708.id == 399)
            val v710 = v709.asInstanceOf[SequenceNode].children.head
            val BindNode(v711, v712) = v710
            assert(v711.id == 400)
            matchCanonicalEnumValue(v712)
          case 404 =>
            val BindNode(v713, v714) = v707
            assert(v713.id == 405)
            val v715 = v714.asInstanceOf[SequenceNode].children.head
            val BindNode(v716, v717) = v715
            assert(v716.id == 406)
            matchShortenedEnumValue(v717)
        }
        v718
    }
    v719
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

  def matchCallParams(node: Node): List[PExpr] = {
    val BindNode(v989, v990) = node
    val v1018 = v989.id match {
      case 366 =>
        val v992 = v990.asInstanceOf[SequenceNode].children(2)
        val BindNode(v993, v994) = v992
        assert(v993.id == 367)
        val BindNode(v995, v996) = v994
        val v1017 = v995.id match {
          case 136 =>
            None
          case 368 =>
            val BindNode(v997, v998) = v996
            val v1016 = v997.id match {
              case 369 =>
                val BindNode(v999, v1000) = v998
                assert(v999.id == 370)
                val v1001 = v1000.asInstanceOf[SequenceNode].children.head
                val BindNode(v1002, v1003) = v1001
                assert(v1002.id == 324)
                val v1004 = v1000.asInstanceOf[SequenceNode].children(1)
                val v1005 = unrollRepeat0(v1004).map { elem =>
                  val BindNode(v1006, v1007) = elem
                  assert(v1006.id == 373)
                  val BindNode(v1008, v1009) = v1007
                  val v1015 = v1008.id match {
                    case 374 =>
                      val BindNode(v1010, v1011) = v1009
                      assert(v1010.id == 375)
                      val v1012 = v1011.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v1013, v1014) = v1012
                      assert(v1013.id == 324)
                      matchPExpr(v1014)
                  }
                  v1015
                }
                List(matchPExpr(v1003)) ++ v1005
            }
            Some(v1016)
        }
        val v991 = v1017
        if (v991.isDefined) v991.get else List()
    }
    v1018
  }

  def matchBinSymbol(node: Node): BinSymbol = {
    val BindNode(v971, v972) = node
    val v988 = v971.id match {
      case 206 =>
        val v973 = v972.asInstanceOf[SequenceNode].children.head
        val BindNode(v974, v975) = v973
        assert(v974.id == 205)
        val v976 = v972.asInstanceOf[SequenceNode].children(4)
        val BindNode(v977, v978) = v976
        assert(v977.id == 208)
        JoinSymbol(matchBinSymbol(v975), matchPreUnSymbol(v978))
      case 285 =>
        val v979 = v972.asInstanceOf[SequenceNode].children.head
        val BindNode(v980, v981) = v979
        assert(v980.id == 205)
        val v982 = v972.asInstanceOf[SequenceNode].children(4)
        val BindNode(v983, v984) = v982
        assert(v983.id == 208)
        ExceptSymbol(matchBinSymbol(v981), matchPreUnSymbol(v984))
      case 286 =>
        val v985 = v972.asInstanceOf[SequenceNode].children.head
        val BindNode(v986, v987) = v985
        assert(v986.id == 208)
        matchPreUnSymbol(v987)
    }
    v988
  }

  def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
    val BindNode(v681, v682) = node
    val v692 = v681.id match {
      case 244 =>
        val v683 = v682.asInstanceOf[SequenceNode].children.head
        val BindNode(v684, v685) = v683
        assert(v684.id == 245)
        CharAsIs(v685.toString.charAt(0))
      case 247 =>
        val v686 = v682.asInstanceOf[SequenceNode].children(1)
        val BindNode(v687, v688) = v686
        assert(v687.id == 248)
        CharEscaped(v688.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0))
      case 232 =>
        val v689 = v682.asInstanceOf[SequenceNode].children.head
        val BindNode(v690, v691) = v689
        assert(v690.id == 233)
        matchUnicodeChar(v691)
    }
    v692
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

  def matchTypeDesc(node: Node): TypeDesc = {
    val BindNode(v790, v791) = node
    val v809 = v790.id match {
      case 99 =>
        val v792 = v791.asInstanceOf[SequenceNode].children.head
        val BindNode(v793, v794) = v792
        assert(v793.id == 100)
        val v795 = v791.asInstanceOf[SequenceNode].children(1)
        val BindNode(v796, v797) = v795
        assert(v796.id == 188)
        val BindNode(v798, v799) = v797
        val v808 = v798.id match {
          case 136 =>
            None
          case 189 =>
            val BindNode(v800, v801) = v799
            val v807 = v800.id match {
              case 190 =>
                val BindNode(v802, v803) = v801
                assert(v802.id == 191)
                val v804 = v803.asInstanceOf[SequenceNode].children(1)
                val BindNode(v805, v806) = v804
                assert(v805.id == 192)
                v806.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v807)
        }
        TypeDesc(matchNonNullTypeDesc(v794), v808.isDefined)
    }
    v809
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

  def matchBoolAndExpr(node: Node): BoolAndExpr = {
    val BindNode(v479, v480) = node
    val v490 = v479.id match {
      case 331 =>
        val v481 = v480.asInstanceOf[SequenceNode].children.head
        val BindNode(v482, v483) = v481
        assert(v482.id == 332)
        val v484 = v480.asInstanceOf[SequenceNode].children(4)
        val BindNode(v485, v486) = v484
        assert(v485.id == 330)
        BinOp(Op.OR, matchBoolEqExpr(v483), matchBoolAndExpr(v486))
      case 428 =>
        val v487 = v480.asInstanceOf[SequenceNode].children.head
        val BindNode(v488, v489) = v487
        assert(v488.id == 332)
        matchBoolEqExpr(v489)
    }
    v490
  }

  def matchLHS(node: Node): LHS = {
    val BindNode(v459, v460) = node
    val v478 = v459.id match {
      case 39 =>
        val v461 = v460.asInstanceOf[SequenceNode].children.head
        val BindNode(v462, v463) = v461
        assert(v462.id == 40)
        val v464 = v460.asInstanceOf[SequenceNode].children(1)
        val BindNode(v465, v466) = v464
        assert(v465.id == 93)
        val BindNode(v467, v468) = v466
        val v477 = v467.id match {
          case 136 =>
            None
          case 94 =>
            val BindNode(v469, v470) = v468
            val v476 = v469.id match {
              case 95 =>
                val BindNode(v471, v472) = v470
                assert(v471.id == 96)
                val v473 = v472.asInstanceOf[SequenceNode].children(3)
                val BindNode(v474, v475) = v473
                assert(v474.id == 98)
                matchTypeDesc(v475)
            }
            Some(v476)
        }
        LHS(matchNonterminal(v463), v477)
    }
    v478
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

  def matchClassParamsDef(node: Node): List[ClassParamDef] = {
    val BindNode(v642, v643) = node
    val v671 = v642.id match {
      case 140 =>
        val v645 = v643.asInstanceOf[SequenceNode].children(2)
        val BindNode(v646, v647) = v645
        assert(v646.id == 142)
        val BindNode(v648, v649) = v647
        val v670 = v648.id match {
          case 136 =>
            None
          case 143 =>
            val BindNode(v650, v651) = v649
            val v669 = v650.id match {
              case 144 =>
                val BindNode(v652, v653) = v651
                assert(v652.id == 145)
                val v654 = v653.asInstanceOf[SequenceNode].children.head
                val BindNode(v655, v656) = v654
                assert(v655.id == 146)
                val v657 = v653.asInstanceOf[SequenceNode].children(1)
                val v658 = unrollRepeat0(v657).map { elem =>
                  val BindNode(v659, v660) = elem
                  assert(v659.id == 151)
                  val BindNode(v661, v662) = v660
                  val v668 = v661.id match {
                    case 152 =>
                      val BindNode(v663, v664) = v662
                      assert(v663.id == 153)
                      val v665 = v664.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v666, v667) = v665
                      assert(v666.id == 146)
                      matchClassParamDef(v667)
                  }
                  v668
                }
                List(matchClassParamDef(v656)) ++ v658
            }
            Some(v669)
        }
        val v644 = v670
        if (v644.isDefined) v644.get else List()
    }
    v671
  }

  def matchRHS(node: Node): Sequence = {
    val BindNode(v753, v754) = node
    val v758 = v753.id match {
      case 198 =>
        val v755 = v754.asInstanceOf[SequenceNode].children.head
        val BindNode(v756, v757) = v755
        assert(v756.id == 199)
        matchSequence(v757)
    }
    v758
  }

  def matchEnumTypeName(node: Node): EnumTypeName = {
    val BindNode(v693, v694) = node
    val v698 = v693.id match {
      case 116 =>
        val v695 = v694.asInstanceOf[SequenceNode].children(1)
        val BindNode(v696, v697) = v695
        assert(v696.id == 47)
        EnumTypeName(matchId(v697))
    }
    v698
  }

  def matchGrammar(node: Node): Grammar = {
    val BindNode(v1080, v1081) = node
    val v1097 = v1080.id match {
      case 3 =>
        val v1082 = v1081.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1083, v1084) = v1082
        assert(v1083.id == 34)
        val v1085 = v1081.asInstanceOf[SequenceNode].children(2)
        val v1086 = unrollRepeat0(v1085).map { elem =>
          val BindNode(v1087, v1088) = elem
          assert(v1087.id == 449)
          val BindNode(v1089, v1090) = v1088
          val v1096 = v1089.id match {
            case 450 =>
              val BindNode(v1091, v1092) = v1090
              assert(v1091.id == 451)
              val v1093 = v1092.asInstanceOf[SequenceNode].children(1)
              val BindNode(v1094, v1095) = v1093
              assert(v1094.id == 34)
              matchDef(v1095)
          }
          v1096
        }
        Grammar(List(matchDef(v1084)) ++ v1086)
    }
    v1097
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

  def matchTerminalChar(node: Node): TerminalChar = {
    val BindNode(v1019, v1020) = node
    val v1030 = v1019.id match {
      case 227 =>
        val v1021 = v1020.asInstanceOf[SequenceNode].children.head
        val BindNode(v1022, v1023) = v1021
        assert(v1022.id == 228)
        CharAsIs(v1023.toString.charAt(0))
      case 230 =>
        val v1024 = v1020.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1025, v1026) = v1024
        assert(v1025.id == 231)
        CharEscaped(v1026.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0))
      case 232 =>
        val v1027 = v1020.asInstanceOf[SequenceNode].children.head
        val BindNode(v1028, v1029) = v1027
        assert(v1028.id == 233)
        matchUnicodeChar(v1029)
    }
    v1030
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

  def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
    val BindNode(v932, v933) = node
    val v937 = v932.id match {
      case 407 =>
        val v934 = v933.asInstanceOf[SequenceNode].children(1)
        val BindNode(v935, v936) = v934
        assert(v935.id == 402)
        ShortenedEnumValue(matchEnumValueName(v936))
    }
    v937
  }

  def matchRef(node: Node): Ref = {
    val BindNode(v1031, v1032) = node
    val v1039 = v1031.id match {
      case 291 =>
        val v1033 = v1032.asInstanceOf[SequenceNode].children.head
        val BindNode(v1034, v1035) = v1033
        assert(v1034.id == 292)
        matchValRef(v1035)
      case 318 =>
        val v1036 = v1032.asInstanceOf[SequenceNode].children.head
        val BindNode(v1037, v1038) = v1036
        assert(v1037.id == 319)
        matchRawRef(v1038)
    }
    v1039
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

  def matchInPlaceChoices(node: Node): InPlaceChoices = {
    val BindNode(v527, v528) = node
    val v544 = v527.id match {
      case 271 =>
        val v529 = v528.asInstanceOf[SequenceNode].children.head
        val BindNode(v530, v531) = v529
        assert(v530.id == 199)
        val v532 = v528.asInstanceOf[SequenceNode].children(1)
        val v533 = unrollRepeat0(v532).map { elem =>
          val BindNode(v534, v535) = elem
          assert(v534.id == 274)
          val BindNode(v536, v537) = v535
          val v543 = v536.id match {
            case 275 =>
              val BindNode(v538, v539) = v537
              assert(v538.id == 276)
              val v540 = v539.asInstanceOf[SequenceNode].children(3)
              val BindNode(v541, v542) = v540
              assert(v541.id == 199)
              matchSequence(v542)
          }
          v543
        }
        InPlaceChoices(List(matchSequence(v531)) ++ v533)
    }
    v544
  }

  def matchWSNL(node: Node): String = {
    val BindNode(v421, v422) = node
    val v423 = v421.id match {
      case 453 =>
        ""
    }
    v423
  }

  def matchParamName(node: Node): ParamName = {
    val BindNode(v950, v951) = node
    val v958 = v950.id match {
      case 43 =>
        val v952 = v951.asInstanceOf[SequenceNode].children.head
        val BindNode(v953, v954) = v952
        assert(v953.id == 44)
        ParamName(matchIdNoKeyword(v954))
      case 91 =>
        val v955 = v951.asInstanceOf[SequenceNode].children(1)
        val BindNode(v956, v957) = v955
        assert(v956.id == 47)
        ParamName(matchId(v957))
    }
    v958
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
    val BindNode(v851, v852) = node
    val v859 = v851.id match {
      case 43 =>
        val v853 = v852.asInstanceOf[SequenceNode].children.head
        val BindNode(v854, v855) = v853
        assert(v854.id == 44)
        NonterminalName(matchIdNoKeyword(v855))
      case 91 =>
        val v856 = v852.asInstanceOf[SequenceNode].children(1)
        val BindNode(v857, v858) = v856
        assert(v857.id == 47)
        NonterminalName(matchId(v858))
    }
    v859
  }

  def matchBindExpr(node: Node): BindExpr = {
    val BindNode(v672, v673) = node
    val v680 = v672.id match {
      case 344 =>
        val v674 = v673.asInstanceOf[SequenceNode].children.head
        val BindNode(v675, v676) = v674
        assert(v675.id == 292)
        val v677 = v673.asInstanceOf[SequenceNode].children(1)
        val BindNode(v678, v679) = v677
        assert(v678.id == 345)
        BindExpr(matchValRef(v676), matchBinderExpr(v679))
    }
    v680
  }

  def matchSuperDef(node: Node): SuperDef = {
    val BindNode(v545, v546) = node
    val v578 = v545.id match {
      case 158 =>
        val v547 = v546.asInstanceOf[SequenceNode].children.head
        val BindNode(v548, v549) = v547
        assert(v548.id == 102)
        val v550 = v546.asInstanceOf[SequenceNode].children(4)
        val BindNode(v551, v552) = v550
        assert(v551.id == 164)
        val BindNode(v553, v554) = v552
        val v563 = v553.id match {
          case 136 =>
            None
          case 165 =>
            val BindNode(v555, v556) = v554
            val v562 = v555.id match {
              case 166 =>
                val BindNode(v557, v558) = v556
                assert(v557.id == 167)
                val v559 = v558.asInstanceOf[SequenceNode].children(1)
                val BindNode(v560, v561) = v559
                assert(v560.id == 168)
                matchSubTypes(v561)
            }
            Some(v562)
        }
        val v564 = v546.asInstanceOf[SequenceNode].children(1)
        val BindNode(v565, v566) = v564
        assert(v565.id == 159)
        val BindNode(v567, v568) = v566
        val v577 = v567.id match {
          case 136 =>
            None
          case 160 =>
            val BindNode(v569, v570) = v568
            val v576 = v569.id match {
              case 161 =>
                val BindNode(v571, v572) = v570
                assert(v571.id == 162)
                val v573 = v572.asInstanceOf[SequenceNode].children(1)
                val BindNode(v574, v575) = v573
                assert(v574.id == 123)
                matchSuperTypes(v575)
            }
            Some(v576)
        }
        SuperDef(matchTypeName(v549), v563, v577)
    }
    v578
  }

  def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
    val BindNode(v860, v861) = node
    val v880 = v860.id match {
      case 114 =>
        val v862 = v861.asInstanceOf[SequenceNode].children.head
        val BindNode(v863, v864) = v862
        assert(v863.id == 115)
        matchEnumTypeName(v864)
      case 106 =>
        val v865 = v861.asInstanceOf[SequenceNode].children.head
        val BindNode(v866, v867) = v865
        assert(v866.id == 107)
        matchValueType(v867)
      case 103 =>
        val v868 = v861.asInstanceOf[SequenceNode].children(2)
        val BindNode(v869, v870) = v868
        assert(v869.id == 98)
        ArrayTypeDesc(matchTypeDesc(v870))
      case 118 =>
        val v871 = v861.asInstanceOf[SequenceNode].children.head
        val BindNode(v872, v873) = v871
        assert(v872.id == 119)
        matchTypeDef(v873)
      case 108 =>
        val v874 = v861.asInstanceOf[SequenceNode].children.head
        val BindNode(v875, v876) = v874
        assert(v875.id == 109)
        matchAnyType(v876)
      case 101 =>
        val v877 = v861.asInstanceOf[SequenceNode].children.head
        val BindNode(v878, v879) = v877
        assert(v878.id == 102)
        matchTypeName(v879)
    }
    v880
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v887, v888) = node
    val v909 = v887.id match {
      case 355 =>
        val v889 = v888.asInstanceOf[SequenceNode].children.head
        val BindNode(v890, v891) = v889
        assert(v890.id == 148)
        val v892 = v888.asInstanceOf[SequenceNode].children(1)
        val BindNode(v893, v894) = v892
        assert(v893.id == 93)
        val BindNode(v895, v896) = v894
        val v905 = v895.id match {
          case 136 =>
            None
          case 94 =>
            val BindNode(v897, v898) = v896
            val v904 = v897.id match {
              case 95 =>
                val BindNode(v899, v900) = v898
                assert(v899.id == 96)
                val v901 = v900.asInstanceOf[SequenceNode].children(3)
                val BindNode(v902, v903) = v901
                assert(v902.id == 98)
                matchTypeDesc(v903)
            }
            Some(v904)
        }
        val v906 = v888.asInstanceOf[SequenceNode].children(5)
        val BindNode(v907, v908) = v906
        assert(v907.id == 324)
        NamedParam(matchParamName(v891), v905, matchPExpr(v908))
    }
    v909
  }

  def matchBoolEqExpr(node: Node): BoolEqExpr = {
    val BindNode(v1040, v1041) = node
    val v1061 = v1040.id match {
      case 333 =>
        val v1042 = v1041.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1043, v1044) = v1042
        assert(v1043.id == 416)
        val BindNode(v1045, v1046) = v1044
        val v1051 = v1045.id match {
          case 417 =>
            val BindNode(v1047, v1048) = v1046
            assert(v1047.id == 418)
            Op.EQ
          case 421 =>
            val BindNode(v1049, v1050) = v1046
            assert(v1049.id == 422)
            Op.NE
        }
        val v1052 = v1041.asInstanceOf[SequenceNode].children.head
        val BindNode(v1053, v1054) = v1052
        assert(v1053.id == 334)
        val v1055 = v1041.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1056, v1057) = v1055
        assert(v1056.id == 332)
        BinOp(v1051, matchElvisExpr(v1054), matchBoolEqExpr(v1057))
      case 425 =>
        val v1058 = v1041.asInstanceOf[SequenceNode].children.head
        val BindNode(v1059, v1060) = v1058
        assert(v1059.id == 334)
        matchElvisExpr(v1060)
    }
    v1061
  }

  def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
    val BindNode(v500, v501) = node
    val v508 = v500.id match {
      case 363 =>
        val v502 = v501.asInstanceOf[SequenceNode].children.head
        val BindNode(v503, v504) = v502
        assert(v503.id == 364)
        val v505 = v501.asInstanceOf[SequenceNode].children(2)
        val BindNode(v506, v507) = v505
        assert(v506.id == 365)
        FuncCallOrConstructExpr(matchTypeOrFuncName(v504), matchCallParams(v507))
    }
    v508
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

  def matchLiteral(node: Node): Literal = {
    val BindNode(v439, v440) = node
    val v458 = v439.id match {
      case 88 =>
        NullLiteral()
      case 381 =>
        val v441 = v440.asInstanceOf[SequenceNode].children.head
        val BindNode(v442, v443) = v441
        assert(v442.id == 382)
        val BindNode(v444, v445) = v443
        val v450 = v444.id match {
          case 383 =>
            val BindNode(v446, v447) = v445
            assert(v446.id == 80)
            true
          case 384 =>
            val BindNode(v448, v449) = v445
            assert(v448.id == 84)
            false
        }
        BoolLiteral(v450)
      case 385 =>
        val v451 = v440.asInstanceOf[SequenceNode].children(1)
        val BindNode(v452, v453) = v451
        assert(v452.id == 386)
        CharLiteral(matchCharChar(v453))
      case 388 =>
        val v454 = v440.asInstanceOf[SequenceNode].children(1)
        val v455 = unrollRepeat0(v454).map { elem =>
          val BindNode(v456, v457) = elem
          assert(v456.id == 391)
          matchStrChar(v457)
        }
        StrLiteral(v455)
    }
    v458
  }

  def matchKeyword(node: Node): KeyWord.Value = {
    val BindNode(v947, v948) = node
    val v949 = v947.id match {
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
    v949
  }

  def matchTypeDef(node: Node): TypeDef = {
    val BindNode(v608, v609) = node
    val v619 = v608.id match {
      case 120 =>
        val v610 = v609.asInstanceOf[SequenceNode].children.head
        val BindNode(v611, v612) = v610
        assert(v611.id == 121)
        matchClassDef(v612)
      case 156 =>
        val v613 = v609.asInstanceOf[SequenceNode].children.head
        val BindNode(v614, v615) = v613
        assert(v614.id == 157)
        matchSuperDef(v615)
      case 177 =>
        val v616 = v609.asInstanceOf[SequenceNode].children.head
        val BindNode(v617, v618) = v616
        assert(v617.id == 178)
        matchEnumTypeDef(v618)
    }
    v619
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

  def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
    val BindNode(v491, v492) = node
    val v499 = v491.id match {
      case 339 =>
        val v493 = v492.asInstanceOf[SequenceNode].children(2)
        val BindNode(v494, v495) = v493
        assert(v494.id == 338)
        PrefixOp(PreOp.NOT, matchPrefixNotExpr(v495))
      case 340 =>
        val v496 = v492.asInstanceOf[SequenceNode].children.head
        val BindNode(v497, v498) = v496
        assert(v497.id == 341)
        matchAtom(v498)
    }
    v499
  }

  def matchValRef(node: Node): ValRef = {
    val BindNode(v409, v410) = node
    val v420 = v409.id match {
      case 293 =>
        val v411 = v410.asInstanceOf[SequenceNode].children(2)
        val BindNode(v412, v413) = v411
        assert(v412.id == 305)
        val v414 = v410.asInstanceOf[SequenceNode].children(1)
        val BindNode(v415, v416) = v414
        assert(v415.id == 295)
        val BindNode(v417, v418) = v416
        val v419 = v417.id match {
          case 136 =>
            None
          case 296 =>
            Some(matchCondSymPath(v418))
        }
        ValRef(matchRefIdx(v413), v419)
    }
    v420
  }

  def matchAnyType(node: Node): AnyType = {
    val BindNode(v224, v225) = node
    val v226 = v224.id match {
      case 110 =>
        AnyType()
    }
    v226
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

  def matchElem(node: Node): Elem = {
    val BindNode(v720, v721) = node
    val v728 = v720.id match {
      case 202 =>
        val v722 = v721.asInstanceOf[SequenceNode].children.head
        val BindNode(v723, v724) = v722
        assert(v723.id == 203)
        matchSymbol(v724)
      case 287 =>
        val v725 = v721.asInstanceOf[SequenceNode].children.head
        val BindNode(v726, v727) = v725
        assert(v726.id == 288)
        matchProcessor(v727)
    }
    v728
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

  def matchPreUnSymbol(node: Node): PreUnSymbol = {
    val BindNode(v959, v960) = node
    val v970 = v959.id match {
      case 209 =>
        val v961 = v960.asInstanceOf[SequenceNode].children(2)
        val BindNode(v962, v963) = v961
        assert(v962.id == 208)
        FollowedBy(matchPreUnSymbol(v963))
      case 211 =>
        val v964 = v960.asInstanceOf[SequenceNode].children(2)
        val BindNode(v965, v966) = v964
        assert(v965.id == 208)
        NotFollowedBy(matchPreUnSymbol(v966))
      case 213 =>
        val v967 = v960.asInstanceOf[SequenceNode].children.head
        val BindNode(v968, v969) = v967
        assert(v968.id == 214)
        matchPostUnSymbol(v969)
    }
    v970
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

  def matchEnumValueName(node: Node): EnumValueName = {
    val BindNode(v1062, v1063) = node
    val v1067 = v1062.id match {
      case 403 =>
        val v1064 = v1063.asInstanceOf[SequenceNode].children.head
        val BindNode(v1065, v1066) = v1064
        assert(v1065.id == 47)
        EnumValueName(matchId(v1066))
    }
    v1067
  }

  def matchStringChar(node: Node): StringChar = {
    val BindNode(v397, v398) = node
    val v408 = v397.id match {
      case 263 =>
        val v399 = v398.asInstanceOf[SequenceNode].children.head
        val BindNode(v400, v401) = v399
        assert(v400.id == 264)
        CharAsIs(v401.toString.charAt(0))
      case 266 =>
        val v402 = v398.asInstanceOf[SequenceNode].children(1)
        val BindNode(v403, v404) = v402
        assert(v403.id == 267)
        CharEscaped(v404.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0))
      case 232 =>
        val v405 = v398.asInstanceOf[SequenceNode].children.head
        val BindNode(v406, v407) = v405
        assert(v406.id == 233)
        matchUnicodeChar(v407)
    }
    v408
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
    println(parseAst("""A = 'a' {null ?: "string"}""".stripMargin))
  }
}
