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

object MetaLang3Ast {
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
      17 -> NGrammar.NProxy(17, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('/'), Symbols.ExactChar('/')))), 18),
      19 -> NGrammar.NTerminal(19, Symbols.ExactChar('/')),
      20 -> NGrammar.NRepeat(20, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), 0), 7, 21),
      22 -> NGrammar.NOneOf(22, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), Set(23)),
      23 -> NGrammar.NProxy(23, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))), 24),
      25 -> NGrammar.NExcept(25, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 26, 27),
      26 -> NGrammar.NTerminal(26, Symbols.AnyChar),
      27 -> NGrammar.NTerminal(27, Symbols.ExactChar('\n')),
      28 -> NGrammar.NOneOf(28, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("EOF")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\n')))))), Set(29, 34)),
      29 -> NGrammar.NProxy(29, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("EOF")))), 30),
      31 -> NGrammar.NNonterminal(31, Symbols.Nonterminal("EOF"), Set(32)),
      33 -> NGrammar.NLookaheadExcept(33, Symbols.LookaheadExcept(Symbols.AnyChar), 7, 26),
      34 -> NGrammar.NProxy(34, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\n')))), 35),
      36 -> NGrammar.NNonterminal(36, Symbols.Nonterminal("Def"), Set(37, 120)),
      38 -> NGrammar.NNonterminal(38, Symbols.Nonterminal("Rule"), Set(39)),
      40 -> NGrammar.NNonterminal(40, Symbols.Nonterminal("LHS"), Set(41)),
      42 -> NGrammar.NNonterminal(42, Symbols.Nonterminal("Nonterminal"), Set(43)),
      44 -> NGrammar.NNonterminal(44, Symbols.Nonterminal("NonterminalName"), Set(45, 93)),
      46 -> NGrammar.NNonterminal(46, Symbols.Nonterminal("IdNoKeyword"), Set(47)),
      48 -> NGrammar.NExcept(48, Symbols.Except(Symbols.Nonterminal("Id"), Symbols.Nonterminal("Keyword")), 49, 59),
      49 -> NGrammar.NNonterminal(49, Symbols.Nonterminal("Id"), Set(50)),
      51 -> NGrammar.NLongest(51, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))), 52),
      52 -> NGrammar.NOneOf(52, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))))), Set(53)),
      53 -> NGrammar.NProxy(53, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))), 54),
      55 -> NGrammar.NTerminal(55, Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
      56 -> NGrammar.NRepeat(56, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), 7, 57),
      58 -> NGrammar.NTerminal(58, Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
      59 -> NGrammar.NNonterminal(59, Symbols.Nonterminal("Keyword"), Set(60, 69, 75, 82, 86, 90)),
      61 -> NGrammar.NProxy(61, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('n')))), 62),
      63 -> NGrammar.NTerminal(63, Symbols.ExactChar('b')),
      64 -> NGrammar.NTerminal(64, Symbols.ExactChar('o')),
      65 -> NGrammar.NTerminal(65, Symbols.ExactChar('l')),
      66 -> NGrammar.NTerminal(66, Symbols.ExactChar('e')),
      67 -> NGrammar.NTerminal(67, Symbols.ExactChar('a')),
      68 -> NGrammar.NTerminal(68, Symbols.ExactChar('n')),
      70 -> NGrammar.NProxy(70, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('c'), Symbols.ExactChar('h'), Symbols.ExactChar('a'), Symbols.ExactChar('r')))), 71),
      72 -> NGrammar.NTerminal(72, Symbols.ExactChar('c')),
      73 -> NGrammar.NTerminal(73, Symbols.ExactChar('h')),
      74 -> NGrammar.NTerminal(74, Symbols.ExactChar('r')),
      76 -> NGrammar.NProxy(76, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g')))), 77),
      78 -> NGrammar.NTerminal(78, Symbols.ExactChar('s')),
      79 -> NGrammar.NTerminal(79, Symbols.ExactChar('t')),
      80 -> NGrammar.NTerminal(80, Symbols.ExactChar('i')),
      81 -> NGrammar.NTerminal(81, Symbols.ExactChar('g')),
      83 -> NGrammar.NProxy(83, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e')))), 84),
      85 -> NGrammar.NTerminal(85, Symbols.ExactChar('u')),
      87 -> NGrammar.NProxy(87, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e')))), 88),
      89 -> NGrammar.NTerminal(89, Symbols.ExactChar('f')),
      91 -> NGrammar.NProxy(91, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('l'), Symbols.ExactChar('l')))), 92),
      94 -> NGrammar.NTerminal(94, Symbols.ExactChar('`')),
      95 -> NGrammar.NOneOf(95, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(96, 138)),
      96 -> NGrammar.NOneOf(96, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Set(97)),
      97 -> NGrammar.NProxy(97, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))), 98),
      99 -> NGrammar.NTerminal(99, Symbols.ExactChar(':')),
      100 -> NGrammar.NNonterminal(100, Symbols.Nonterminal("TypeDesc"), Set(101)),
      102 -> NGrammar.NNonterminal(102, Symbols.Nonterminal("NonNullTypeDesc"), Set(103, 105, 108, 110, 116, 120)),
      104 -> NGrammar.NNonterminal(104, Symbols.Nonterminal("TypeName"), Set(45, 93)),
      106 -> NGrammar.NTerminal(106, Symbols.ExactChar('[')),
      107 -> NGrammar.NTerminal(107, Symbols.ExactChar(']')),
      109 -> NGrammar.NNonterminal(109, Symbols.Nonterminal("ValueType"), Set(60, 69, 75)),
      111 -> NGrammar.NNonterminal(111, Symbols.Nonterminal("AnyType"), Set(112)),
      113 -> NGrammar.NProxy(113, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'), Symbols.ExactChar('n'), Symbols.ExactChar('y')))), 114),
      115 -> NGrammar.NTerminal(115, Symbols.ExactChar('y')),
      117 -> NGrammar.NNonterminal(117, Symbols.Nonterminal("EnumTypeName"), Set(118)),
      119 -> NGrammar.NTerminal(119, Symbols.ExactChar('%')),
      121 -> NGrammar.NNonterminal(121, Symbols.Nonterminal("TypeDef"), Set(122, 158, 179)),
      123 -> NGrammar.NNonterminal(123, Symbols.Nonterminal("ClassDef"), Set(124, 140, 157)),
      125 -> NGrammar.NNonterminal(125, Symbols.Nonterminal("SuperTypes"), Set(126)),
      127 -> NGrammar.NTerminal(127, Symbols.ExactChar('<')),
      128 -> NGrammar.NOneOf(128, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(129, 138)),
      129 -> NGrammar.NOneOf(129, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))))), Set(130)),
      130 -> NGrammar.NProxy(130, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))), 131),
      132 -> NGrammar.NRepeat(132, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), 7, 133),
      134 -> NGrammar.NOneOf(134, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), Set(135)),
      135 -> NGrammar.NProxy(135, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))), 136),
      137 -> NGrammar.NTerminal(137, Symbols.ExactChar(',')),
      138 -> NGrammar.NProxy(138, Symbols.Proxy(Symbols.Sequence(Seq())), 7),
      139 -> NGrammar.NTerminal(139, Symbols.ExactChar('>')),
      141 -> NGrammar.NNonterminal(141, Symbols.Nonterminal("ClassParamsDef"), Set(142)),
      143 -> NGrammar.NTerminal(143, Symbols.ExactChar('(')),
      144 -> NGrammar.NOneOf(144, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(145, 138)),
      145 -> NGrammar.NOneOf(145, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))))), Set(146)),
      146 -> NGrammar.NProxy(146, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))), 147),
      148 -> NGrammar.NNonterminal(148, Symbols.Nonterminal("ClassParamDef"), Set(149)),
      150 -> NGrammar.NNonterminal(150, Symbols.Nonterminal("ParamName"), Set(45, 93)),
      151 -> NGrammar.NRepeat(151, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), 7, 152),
      153 -> NGrammar.NOneOf(153, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), Set(154)),
      154 -> NGrammar.NProxy(154, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))), 155),
      156 -> NGrammar.NTerminal(156, Symbols.ExactChar(')')),
      159 -> NGrammar.NNonterminal(159, Symbols.Nonterminal("SuperDef"), Set(160)),
      161 -> NGrammar.NOneOf(161, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(162, 138)),
      162 -> NGrammar.NOneOf(162, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Set(163)),
      163 -> NGrammar.NProxy(163, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))), 164),
      165 -> NGrammar.NTerminal(165, Symbols.ExactChar('{')),
      166 -> NGrammar.NOneOf(166, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(167, 138)),
      167 -> NGrammar.NOneOf(167, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))))), Set(168)),
      168 -> NGrammar.NProxy(168, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))), 169),
      170 -> NGrammar.NNonterminal(170, Symbols.Nonterminal("SubTypes"), Set(171)),
      172 -> NGrammar.NNonterminal(172, Symbols.Nonterminal("SubType"), Set(103, 122, 158)),
      173 -> NGrammar.NRepeat(173, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), 0), 7, 174),
      175 -> NGrammar.NOneOf(175, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), Set(176)),
      176 -> NGrammar.NProxy(176, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))), 177),
      178 -> NGrammar.NTerminal(178, Symbols.ExactChar('}')),
      180 -> NGrammar.NNonterminal(180, Symbols.Nonterminal("EnumTypeDef"), Set(181)),
      182 -> NGrammar.NOneOf(182, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0)))))), Set(183)),
      183 -> NGrammar.NProxy(183, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0)))), 184),
      185 -> NGrammar.NRepeat(185, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0), 7, 186),
      187 -> NGrammar.NOneOf(187, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), Set(188)),
      188 -> NGrammar.NProxy(188, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))), 189),
      190 -> NGrammar.NOneOf(190, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(191, 138)),
      191 -> NGrammar.NOneOf(191, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))))), Set(192)),
      192 -> NGrammar.NProxy(192, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))), 193),
      194 -> NGrammar.NTerminal(194, Symbols.ExactChar('?')),
      195 -> NGrammar.NTerminal(195, Symbols.ExactChar('=')),
      196 -> NGrammar.NOneOf(196, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0)))))), Set(197)),
      197 -> NGrammar.NProxy(197, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0)))), 198),
      199 -> NGrammar.NNonterminal(199, Symbols.Nonterminal("RHS"), Set(200)),
      201 -> NGrammar.NNonterminal(201, Symbols.Nonterminal("Sequence"), Set(202)),
      203 -> NGrammar.NNonterminal(203, Symbols.Nonterminal("Elem"), Set(204, 289)),
      205 -> NGrammar.NNonterminal(205, Symbols.Nonterminal("Symbol"), Set(206)),
      207 -> NGrammar.NNonterminal(207, Symbols.Nonterminal("BinSymbol"), Set(208, 287, 288)),
      209 -> NGrammar.NTerminal(209, Symbols.ExactChar('&')),
      210 -> NGrammar.NNonterminal(210, Symbols.Nonterminal("PreUnSymbol"), Set(211, 213, 215)),
      212 -> NGrammar.NTerminal(212, Symbols.ExactChar('^')),
      214 -> NGrammar.NTerminal(214, Symbols.ExactChar('!')),
      216 -> NGrammar.NNonterminal(216, Symbols.Nonterminal("PostUnSymbol"), Set(217, 218, 220, 222)),
      219 -> NGrammar.NTerminal(219, Symbols.ExactChar('*')),
      221 -> NGrammar.NTerminal(221, Symbols.ExactChar('+')),
      223 -> NGrammar.NNonterminal(223, Symbols.Nonterminal("AtomSymbol"), Set(224, 240, 258, 270, 271, 280, 283)),
      225 -> NGrammar.NNonterminal(225, Symbols.Nonterminal("Terminal"), Set(226, 238)),
      227 -> NGrammar.NTerminal(227, Symbols.ExactChar('\'')),
      228 -> NGrammar.NNonterminal(228, Symbols.Nonterminal("TerminalChar"), Set(229, 232, 234)),
      230 -> NGrammar.NExcept(230, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')), 26, 231),
      231 -> NGrammar.NTerminal(231, Symbols.ExactChar('\\')),
      233 -> NGrammar.NTerminal(233, Symbols.Chars(Set('\'', '\\', 'b', 'n', 'r', 't'))),
      235 -> NGrammar.NNonterminal(235, Symbols.Nonterminal("UnicodeChar"), Set(236)),
      237 -> NGrammar.NTerminal(237, Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet)),
      239 -> NGrammar.NTerminal(239, Symbols.ExactChar('.')),
      241 -> NGrammar.NNonterminal(241, Symbols.Nonterminal("TerminalChoice"), Set(242, 257)),
      243 -> NGrammar.NNonterminal(243, Symbols.Nonterminal("TerminalChoiceElem"), Set(244, 251)),
      245 -> NGrammar.NNonterminal(245, Symbols.Nonterminal("TerminalChoiceChar"), Set(246, 249, 234)),
      247 -> NGrammar.NExcept(247, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'', '-', '\\'))), 26, 248),
      248 -> NGrammar.NTerminal(248, Symbols.Chars(Set('\'', '-', '\\'))),
      250 -> NGrammar.NTerminal(250, Symbols.Chars(Set('\'', '-', '\\', 'b', 'n', 'r', 't'))),
      252 -> NGrammar.NNonterminal(252, Symbols.Nonterminal("TerminalChoiceRange"), Set(253)),
      254 -> NGrammar.NTerminal(254, Symbols.ExactChar('-')),
      255 -> NGrammar.NRepeat(255, Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), 243, 256),
      259 -> NGrammar.NNonterminal(259, Symbols.Nonterminal("StringSymbol"), Set(260)),
      261 -> NGrammar.NTerminal(261, Symbols.ExactChar('"')),
      262 -> NGrammar.NRepeat(262, Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), 7, 263),
      264 -> NGrammar.NNonterminal(264, Symbols.Nonterminal("StringChar"), Set(265, 268, 234)),
      266 -> NGrammar.NExcept(266, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"', '\\'))), 26, 267),
      267 -> NGrammar.NTerminal(267, Symbols.Chars(Set('"', '\\'))),
      269 -> NGrammar.NTerminal(269, Symbols.Chars(Set('"', '\\', 'b', 'n', 'r', 't'))),
      272 -> NGrammar.NNonterminal(272, Symbols.Nonterminal("InPlaceChoices"), Set(273)),
      274 -> NGrammar.NRepeat(274, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), 0), 7, 275),
      276 -> NGrammar.NOneOf(276, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), Set(277)),
      277 -> NGrammar.NProxy(277, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))), 278),
      279 -> NGrammar.NTerminal(279, Symbols.ExactChar('|')),
      281 -> NGrammar.NNonterminal(281, Symbols.Nonterminal("Longest"), Set(282)),
      284 -> NGrammar.NNonterminal(284, Symbols.Nonterminal("EmptySequence"), Set(285)),
      286 -> NGrammar.NTerminal(286, Symbols.ExactChar('#')),
      290 -> NGrammar.NNonterminal(290, Symbols.Nonterminal("Processor"), Set(291, 325)),
      292 -> NGrammar.NNonterminal(292, Symbols.Nonterminal("Ref"), Set(293, 320)),
      294 -> NGrammar.NNonterminal(294, Symbols.Nonterminal("ValRef"), Set(295)),
      296 -> NGrammar.NTerminal(296, Symbols.ExactChar('$')),
      297 -> NGrammar.NOneOf(297, Symbols.OneOf(ListSet(Symbols.Nonterminal("CondSymPath"), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(298, 138)),
      298 -> NGrammar.NNonterminal(298, Symbols.Nonterminal("CondSymPath"), Set(299)),
      300 -> NGrammar.NRepeat(300, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), 1), 301, 306),
      301 -> NGrammar.NOneOf(301, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), Set(302, 304)),
      302 -> NGrammar.NProxy(302, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), 303),
      304 -> NGrammar.NProxy(304, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))), 305),
      307 -> NGrammar.NNonterminal(307, Symbols.Nonterminal("RefIdx"), Set(308)),
      309 -> NGrammar.NLongest(309, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))), 310),
      310 -> NGrammar.NOneOf(310, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))))), Set(311, 314)),
      311 -> NGrammar.NProxy(311, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), 312),
      313 -> NGrammar.NTerminal(313, Symbols.ExactChar('0')),
      314 -> NGrammar.NProxy(314, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))), 315),
      316 -> NGrammar.NTerminal(316, Symbols.Chars(('1' to '9').toSet)),
      317 -> NGrammar.NRepeat(317, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 7, 318),
      319 -> NGrammar.NTerminal(319, Symbols.Chars(('0' to '9').toSet)),
      321 -> NGrammar.NNonterminal(321, Symbols.Nonterminal("RawRef"), Set(322)),
      323 -> NGrammar.NProxy(323, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$')))), 324),
      326 -> NGrammar.NNonterminal(326, Symbols.Nonterminal("PExpr"), Set(327, 437)),
      328 -> NGrammar.NNonterminal(328, Symbols.Nonterminal("TernaryExpr"), Set(329, 438)),
      330 -> NGrammar.NNonterminal(330, Symbols.Nonterminal("BoolOrExpr"), Set(331, 433)),
      332 -> NGrammar.NNonterminal(332, Symbols.Nonterminal("BoolAndExpr"), Set(333, 430)),
      334 -> NGrammar.NNonterminal(334, Symbols.Nonterminal("BoolEqExpr"), Set(335, 427)),
      336 -> NGrammar.NNonterminal(336, Symbols.Nonterminal("ElvisExpr"), Set(337, 417)),
      338 -> NGrammar.NNonterminal(338, Symbols.Nonterminal("AdditiveExpr"), Set(339, 414)),
      340 -> NGrammar.NNonterminal(340, Symbols.Nonterminal("PrefixNotExpr"), Set(341, 342)),
      343 -> NGrammar.NNonterminal(343, Symbols.Nonterminal("Atom"), Set(291, 344, 348, 363, 378, 381, 395, 410)),
      345 -> NGrammar.NNonterminal(345, Symbols.Nonterminal("BindExpr"), Set(346)),
      347 -> NGrammar.NNonterminal(347, Symbols.Nonterminal("BinderExpr"), Set(291, 344, 325)),
      349 -> NGrammar.NNonterminal(349, Symbols.Nonterminal("NamedConstructExpr"), Set(350)),
      351 -> NGrammar.NNonterminal(351, Symbols.Nonterminal("NamedConstructParams"), Set(352)),
      353 -> NGrammar.NOneOf(353, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))))), Set(354)),
      354 -> NGrammar.NProxy(354, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))), 355),
      356 -> NGrammar.NNonterminal(356, Symbols.Nonterminal("NamedParam"), Set(357)),
      358 -> NGrammar.NRepeat(358, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), 7, 359),
      360 -> NGrammar.NOneOf(360, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), Set(361)),
      361 -> NGrammar.NProxy(361, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))), 362),
      364 -> NGrammar.NNonterminal(364, Symbols.Nonterminal("FuncCallOrConstructExpr"), Set(365)),
      366 -> NGrammar.NNonterminal(366, Symbols.Nonterminal("TypeOrFuncName"), Set(45, 93)),
      367 -> NGrammar.NNonterminal(367, Symbols.Nonterminal("CallParams"), Set(368)),
      369 -> NGrammar.NOneOf(369, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(370, 138)),
      370 -> NGrammar.NOneOf(370, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Set(371)),
      371 -> NGrammar.NProxy(371, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))), 372),
      373 -> NGrammar.NRepeat(373, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), 7, 374),
      375 -> NGrammar.NOneOf(375, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), Set(376)),
      376 -> NGrammar.NProxy(376, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 377),
      379 -> NGrammar.NNonterminal(379, Symbols.Nonterminal("ArrayExpr"), Set(380)),
      382 -> NGrammar.NNonterminal(382, Symbols.Nonterminal("Literal"), Set(90, 383, 387, 390)),
      384 -> NGrammar.NOneOf(384, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))))), Set(385, 386)),
      385 -> NGrammar.NProxy(385, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), 82),
      386 -> NGrammar.NProxy(386, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))), 86),
      388 -> NGrammar.NNonterminal(388, Symbols.Nonterminal("CharChar"), Set(389)),
      391 -> NGrammar.NRepeat(391, Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), 7, 392),
      393 -> NGrammar.NNonterminal(393, Symbols.Nonterminal("StrChar"), Set(394)),
      396 -> NGrammar.NNonterminal(396, Symbols.Nonterminal("EnumValue"), Set(397)),
      398 -> NGrammar.NLongest(398, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))))))), 399),
      399 -> NGrammar.NOneOf(399, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue")))))), Set(400, 406)),
      400 -> NGrammar.NProxy(400, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), 401),
      402 -> NGrammar.NNonterminal(402, Symbols.Nonterminal("CanonicalEnumValue"), Set(403)),
      404 -> NGrammar.NNonterminal(404, Symbols.Nonterminal("EnumValueName"), Set(405)),
      406 -> NGrammar.NProxy(406, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue")))), 407),
      408 -> NGrammar.NNonterminal(408, Symbols.Nonterminal("ShortenedEnumValue"), Set(409)),
      411 -> NGrammar.NOneOf(411, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))))), Set(412)),
      412 -> NGrammar.NProxy(412, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))), 413),
      415 -> NGrammar.NProxy(415, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':')))), 416),
      418 -> NGrammar.NOneOf(418, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))))), Set(419, 423)),
      419 -> NGrammar.NProxy(419, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), 420),
      421 -> NGrammar.NProxy(421, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('=')))), 422),
      423 -> NGrammar.NProxy(423, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))), 424),
      425 -> NGrammar.NProxy(425, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('=')))), 426),
      428 -> NGrammar.NProxy(428, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|')))), 429),
      431 -> NGrammar.NProxy(431, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&')))), 432),
      434 -> NGrammar.NLongest(434, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))), 435),
      435 -> NGrammar.NOneOf(435, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr")))))), Set(436)),
      436 -> NGrammar.NProxy(436, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr")))), 437),
      439 -> NGrammar.NRepeat(439, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), 0), 7, 440),
      441 -> NGrammar.NOneOf(441, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), Set(442)),
      442 -> NGrammar.NProxy(442, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))), 443),
      444 -> NGrammar.NRepeat(444, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0), 7, 445),
      446 -> NGrammar.NOneOf(446, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), Set(447)),
      447 -> NGrammar.NProxy(447, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))), 448),
      449 -> NGrammar.NRepeat(449, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), 0), 7, 450),
      451 -> NGrammar.NOneOf(451, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), Set(452)),
      452 -> NGrammar.NProxy(452, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))), 453),
      454 -> NGrammar.NNonterminal(454, Symbols.Nonterminal("WSNL"), Set(455)),
      456 -> NGrammar.NLongest(456, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS"))))))), 457),
      457 -> NGrammar.NOneOf(457, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS")))))), Set(458)),
      458 -> NGrammar.NProxy(458, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS")))), 459),
      460 -> NGrammar.NRepeat(460, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), 7, 461),
      462 -> NGrammar.NOneOf(462, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), Set(463, 13)),
      463 -> NGrammar.NProxy(463, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), 464),
      465 -> NGrammar.NTerminal(465, Symbols.Chars(Set('\t', '\r', ' ')))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Def"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), 0), Symbols.Nonterminal("WS"))), Seq(4, 36, 449, 4)),
      5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0))), Seq(6)),
      7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq()), Seq()),
      8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))))), Seq(6, 9)),
      11 -> NGrammar.NSequence(11, Symbols.Sequence(Seq(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet))), Seq(12)),
      14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment"))), Seq(15)),
      16 -> NGrammar.NSequence(16, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('/'), Symbols.ExactChar('/')))), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("EOF")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\n')))))))), Seq(17, 20, 28)),
      18 -> NGrammar.NSequence(18, Symbols.Sequence(Seq(Symbols.ExactChar('/'), Symbols.ExactChar('/'))), Seq(19, 19)),
      21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))))), Seq(20, 22)),
      24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')))), Seq(25)),
      30 -> NGrammar.NSequence(30, Symbols.Sequence(Seq(Symbols.Nonterminal("EOF"))), Seq(31)),
      32 -> NGrammar.NSequence(32, Symbols.Sequence(Seq(Symbols.LookaheadExcept(Symbols.AnyChar))), Seq(33)),
      35 -> NGrammar.NSequence(35, Symbols.Sequence(Seq(Symbols.ExactChar('\n'))), Seq(27)),
      37 -> NGrammar.NSequence(37, Symbols.Sequence(Seq(Symbols.Nonterminal("Rule"))), Seq(38)),
      39 -> NGrammar.NSequence(39, Symbols.Sequence(Seq(Symbols.Nonterminal("LHS"), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0)))))))), Seq(40, 4, 195, 4, 196)),
      41 -> NGrammar.NSequence(41, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(42, 95)),
      43 -> NGrammar.NSequence(43, Symbols.Sequence(Seq(Symbols.Nonterminal("NonterminalName"))), Seq(44)),
      45 -> NGrammar.NSequence(45, Symbols.Sequence(Seq(Symbols.Nonterminal("IdNoKeyword"))), Seq(46)),
      47 -> NGrammar.NSequence(47, Symbols.Sequence(Seq(Symbols.Except(Symbols.Nonterminal("Id"), Symbols.Nonterminal("Keyword")))), Seq(48)),
      50 -> NGrammar.NSequence(50, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))))), Seq(51)),
      54 -> NGrammar.NSequence(54, Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))), Seq(55, 56)),
      57 -> NGrammar.NSequence(57, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(56, 58)),
      60 -> NGrammar.NSequence(60, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('n')))))), Seq(61)),
      62 -> NGrammar.NSequence(62, Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('n'))), Seq(63, 64, 64, 65, 66, 67, 68)),
      69 -> NGrammar.NSequence(69, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('c'), Symbols.ExactChar('h'), Symbols.ExactChar('a'), Symbols.ExactChar('r')))))), Seq(70)),
      71 -> NGrammar.NSequence(71, Symbols.Sequence(Seq(Symbols.ExactChar('c'), Symbols.ExactChar('h'), Symbols.ExactChar('a'), Symbols.ExactChar('r'))), Seq(72, 73, 67, 74)),
      75 -> NGrammar.NSequence(75, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g')))))), Seq(76)),
      77 -> NGrammar.NSequence(77, Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g'))), Seq(78, 79, 74, 80, 68, 81)),
      82 -> NGrammar.NSequence(82, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e')))))), Seq(83)),
      84 -> NGrammar.NSequence(84, Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))), Seq(79, 74, 85, 66)),
      86 -> NGrammar.NSequence(86, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e')))))), Seq(87)),
      88 -> NGrammar.NSequence(88, Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))), Seq(89, 67, 65, 78, 66)),
      90 -> NGrammar.NSequence(90, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('l'), Symbols.ExactChar('l')))))), Seq(91)),
      92 -> NGrammar.NSequence(92, Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('l'), Symbols.ExactChar('l'))), Seq(68, 85, 65, 65)),
      93 -> NGrammar.NSequence(93, Symbols.Sequence(Seq(Symbols.ExactChar('`'), Symbols.Nonterminal("Id"), Symbols.ExactChar('`'))), Seq(94, 49, 94)),
      98 -> NGrammar.NSequence(98, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc"))), Seq(4, 99, 4, 100)),
      101 -> NGrammar.NSequence(101, Symbols.Sequence(Seq(Symbols.Nonterminal("NonNullTypeDesc"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(102, 190)),
      103 -> NGrammar.NSequence(103, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"))), Seq(104)),
      105 -> NGrammar.NSequence(105, Symbols.Sequence(Seq(Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc"), Symbols.Nonterminal("WS"), Symbols.ExactChar(']'))), Seq(106, 4, 100, 4, 107)),
      108 -> NGrammar.NSequence(108, Symbols.Sequence(Seq(Symbols.Nonterminal("ValueType"))), Seq(109)),
      110 -> NGrammar.NSequence(110, Symbols.Sequence(Seq(Symbols.Nonterminal("AnyType"))), Seq(111)),
      112 -> NGrammar.NSequence(112, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'), Symbols.ExactChar('n'), Symbols.ExactChar('y')))))), Seq(113)),
      114 -> NGrammar.NSequence(114, Symbols.Sequence(Seq(Symbols.ExactChar('a'), Symbols.ExactChar('n'), Symbols.ExactChar('y'))), Seq(67, 68, 115)),
      116 -> NGrammar.NSequence(116, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"))), Seq(117)),
      118 -> NGrammar.NSequence(118, Symbols.Sequence(Seq(Symbols.ExactChar('%'), Symbols.Nonterminal("Id"))), Seq(119, 49)),
      120 -> NGrammar.NSequence(120, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeDef"))), Seq(121)),
      122 -> NGrammar.NSequence(122, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassDef"))), Seq(123)),
      124 -> NGrammar.NSequence(124, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes"))), Seq(104, 4, 125)),
      126 -> NGrammar.NSequence(126, Symbols.Sequence(Seq(Symbols.ExactChar('<'), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar('>'))), Seq(127, 4, 128, 139)),
      131 -> NGrammar.NSequence(131, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS"))), Seq(104, 132, 4)),
      133 -> NGrammar.NSequence(133, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))))), Seq(132, 134)),
      136 -> NGrammar.NSequence(136, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName"))), Seq(4, 137, 4, 104)),
      140 -> NGrammar.NSequence(140, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamsDef"))), Seq(104, 4, 141)),
      142 -> NGrammar.NSequence(142, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(143, 4, 144, 4, 156)),
      147 -> NGrammar.NSequence(147, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS"))), Seq(148, 151, 4)),
      149 -> NGrammar.NSequence(149, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(150, 95)),
      152 -> NGrammar.NSequence(152, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))))), Seq(151, 153)),
      155 -> NGrammar.NSequence(155, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef"))), Seq(4, 137, 4, 148)),
      157 -> NGrammar.NSequence(157, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamsDef"))), Seq(104, 4, 125, 4, 141)),
      158 -> NGrammar.NSequence(158, Symbols.Sequence(Seq(Symbols.Nonterminal("SuperDef"))), Seq(159)),
      160 -> NGrammar.NSequence(160, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar('{'), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(104, 161, 4, 165, 166, 4, 178)),
      164 -> NGrammar.NSequence(164, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes"))), Seq(4, 125)),
      169 -> NGrammar.NSequence(169, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes"))), Seq(4, 170)),
      171 -> NGrammar.NSequence(171, Symbols.Sequence(Seq(Symbols.Nonterminal("SubType"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), 0))), Seq(172, 173)),
      174 -> NGrammar.NSequence(174, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))))), Seq(173, 175)),
      177 -> NGrammar.NSequence(177, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType"))), Seq(4, 137, 4, 172)),
      179 -> NGrammar.NSequence(179, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeDef"))), Seq(180)),
      181 -> NGrammar.NSequence(181, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('{'), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0)))))), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(117, 4, 165, 4, 182, 4, 178)),
      184 -> NGrammar.NSequence(184, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0))), Seq(49, 185)),
      186 -> NGrammar.NSequence(186, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))))), Seq(185, 187)),
      189 -> NGrammar.NSequence(189, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id"))), Seq(4, 137, 4, 49)),
      193 -> NGrammar.NSequence(193, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?'))), Seq(4, 194)),
      198 -> NGrammar.NSequence(198, Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0))), Seq(199, 444)),
      200 -> NGrammar.NSequence(200, Symbols.Sequence(Seq(Symbols.Nonterminal("Sequence"))), Seq(201)),
      202 -> NGrammar.NSequence(202, Symbols.Sequence(Seq(Symbols.Nonterminal("Elem"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), 0))), Seq(203, 439)),
      204 -> NGrammar.NSequence(204, Symbols.Sequence(Seq(Symbols.Nonterminal("Symbol"))), Seq(205)),
      206 -> NGrammar.NSequence(206, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"))), Seq(207)),
      208 -> NGrammar.NSequence(208, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('&'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(207, 4, 209, 4, 210)),
      211 -> NGrammar.NSequence(211, Symbols.Sequence(Seq(Symbols.ExactChar('^'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(212, 4, 210)),
      213 -> NGrammar.NSequence(213, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(214, 4, 210)),
      215 -> NGrammar.NSequence(215, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"))), Seq(216)),
      217 -> NGrammar.NSequence(217, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('?'))), Seq(216, 4, 194)),
      218 -> NGrammar.NSequence(218, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('*'))), Seq(216, 4, 219)),
      220 -> NGrammar.NSequence(220, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('+'))), Seq(216, 4, 221)),
      222 -> NGrammar.NSequence(222, Symbols.Sequence(Seq(Symbols.Nonterminal("AtomSymbol"))), Seq(223)),
      224 -> NGrammar.NSequence(224, Symbols.Sequence(Seq(Symbols.Nonterminal("Terminal"))), Seq(225)),
      226 -> NGrammar.NSequence(226, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChar"), Symbols.ExactChar('\''))), Seq(227, 228, 227)),
      229 -> NGrammar.NSequence(229, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')))), Seq(230)),
      232 -> NGrammar.NSequence(232, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('\'', '\\', 'b', 'n', 'r', 't')))), Seq(231, 233)),
      234 -> NGrammar.NSequence(234, Symbols.Sequence(Seq(Symbols.Nonterminal("UnicodeChar"))), Seq(235)),
      236 -> NGrammar.NSequence(236, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('u'), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet))), Seq(231, 85, 237, 237, 237, 237)),
      238 -> NGrammar.NSequence(238, Symbols.Sequence(Seq(Symbols.ExactChar('.'))), Seq(239)),
      240 -> NGrammar.NSequence(240, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoice"))), Seq(241)),
      242 -> NGrammar.NSequence(242, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChoiceElem"), Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), Symbols.ExactChar('\''))), Seq(227, 243, 255, 227)),
      244 -> NGrammar.NSequence(244, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"))), Seq(245)),
      246 -> NGrammar.NSequence(246, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'', '-', '\\'))))), Seq(247)),
      249 -> NGrammar.NSequence(249, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('\'', '-', '\\', 'b', 'n', 'r', 't')))), Seq(231, 250)),
      251 -> NGrammar.NSequence(251, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceRange"))), Seq(252)),
      253 -> NGrammar.NSequence(253, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"), Symbols.ExactChar('-'), Symbols.Nonterminal("TerminalChoiceChar"))), Seq(245, 254, 245)),
      256 -> NGrammar.NSequence(256, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), Symbols.Nonterminal("TerminalChoiceElem"))), Seq(255, 243)),
      257 -> NGrammar.NSequence(257, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChoiceRange"), Symbols.ExactChar('\''))), Seq(227, 252, 227)),
      258 -> NGrammar.NSequence(258, Symbols.Sequence(Seq(Symbols.Nonterminal("StringSymbol"))), Seq(259)),
      260 -> NGrammar.NSequence(260, Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), Symbols.ExactChar('"'))), Seq(261, 262, 261)),
      263 -> NGrammar.NSequence(263, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), Symbols.Nonterminal("StringChar"))), Seq(262, 264)),
      265 -> NGrammar.NSequence(265, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"', '\\'))))), Seq(266)),
      268 -> NGrammar.NSequence(268, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('"', '\\', 'b', 'n', 'r', 't')))), Seq(231, 269)),
      270 -> NGrammar.NSequence(270, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"))), Seq(42)),
      271 -> NGrammar.NSequence(271, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceChoices"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(143, 4, 272, 4, 156)),
      273 -> NGrammar.NSequence(273, Symbols.Sequence(Seq(Symbols.Nonterminal("Sequence"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), 0))), Seq(201, 274)),
      275 -> NGrammar.NSequence(275, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))))), Seq(274, 276)),
      278 -> NGrammar.NSequence(278, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence"))), Seq(4, 279, 4, 201)),
      280 -> NGrammar.NSequence(280, Symbols.Sequence(Seq(Symbols.Nonterminal("Longest"))), Seq(281)),
      282 -> NGrammar.NSequence(282, Symbols.Sequence(Seq(Symbols.ExactChar('<'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceChoices"), Symbols.Nonterminal("WS"), Symbols.ExactChar('>'))), Seq(127, 4, 272, 4, 139)),
      283 -> NGrammar.NSequence(283, Symbols.Sequence(Seq(Symbols.Nonterminal("EmptySequence"))), Seq(284)),
      285 -> NGrammar.NSequence(285, Symbols.Sequence(Seq(Symbols.ExactChar('#'))), Seq(286)),
      287 -> NGrammar.NSequence(287, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('-'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(207, 4, 254, 4, 210)),
      288 -> NGrammar.NSequence(288, Symbols.Sequence(Seq(Symbols.Nonterminal("PreUnSymbol"))), Seq(210)),
      289 -> NGrammar.NSequence(289, Symbols.Sequence(Seq(Symbols.Nonterminal("Processor"))), Seq(290)),
      291 -> NGrammar.NSequence(291, Symbols.Sequence(Seq(Symbols.Nonterminal("Ref"))), Seq(292)),
      293 -> NGrammar.NSequence(293, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"))), Seq(294)),
      295 -> NGrammar.NSequence(295, Symbols.Sequence(Seq(Symbols.ExactChar('$'), Symbols.OneOf(ListSet(Symbols.Nonterminal("CondSymPath"), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("RefIdx"))), Seq(296, 297, 307)),
      299 -> NGrammar.NSequence(299, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), 1))), Seq(300)),
      303 -> NGrammar.NSequence(303, Symbols.Sequence(Seq(Symbols.ExactChar('<'))), Seq(127)),
      305 -> NGrammar.NSequence(305, Symbols.Sequence(Seq(Symbols.ExactChar('>'))), Seq(139)),
      306 -> NGrammar.NSequence(306, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), 1), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))))), Seq(300, 301)),
      308 -> NGrammar.NSequence(308, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))))), Seq(309)),
      312 -> NGrammar.NSequence(312, Symbols.Sequence(Seq(Symbols.ExactChar('0'))), Seq(313)),
      315 -> NGrammar.NSequence(315, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(316, 317)),
      318 -> NGrammar.NSequence(318, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), Symbols.Chars(('0' to '9').toSet))), Seq(317, 319)),
      320 -> NGrammar.NSequence(320, Symbols.Sequence(Seq(Symbols.Nonterminal("RawRef"))), Seq(321)),
      322 -> NGrammar.NSequence(322, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$')))), Symbols.OneOf(ListSet(Symbols.Nonterminal("CondSymPath"), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("RefIdx"))), Seq(323, 297, 307)),
      324 -> NGrammar.NSequence(324, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$'))), Seq(231, 296)),
      325 -> NGrammar.NSequence(325, Symbols.Sequence(Seq(Symbols.ExactChar('{'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(165, 4, 326, 4, 178)),
      327 -> NGrammar.NSequence(327, Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc"))), Seq(328, 4, 99, 4, 100)),
      329 -> NGrammar.NSequence(329, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar('?'), Symbols.Nonterminal("WS"), Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))), Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))))), Seq(330, 4, 194, 4, 434, 4, 99, 4, 434)),
      331 -> NGrammar.NSequence(331, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolOrExpr"))), Seq(332, 4, 431, 4, 330)),
      333 -> NGrammar.NSequence(333, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolAndExpr"))), Seq(334, 4, 428, 4, 332)),
      335 -> NGrammar.NSequence(335, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolEqExpr"))), Seq(336, 4, 418, 4, 334)),
      337 -> NGrammar.NSequence(337, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ElvisExpr"))), Seq(338, 4, 415, 4, 336)),
      339 -> NGrammar.NSequence(339, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("AdditiveExpr"))), Seq(340, 4, 411, 4, 338)),
      341 -> NGrammar.NSequence(341, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PrefixNotExpr"))), Seq(214, 4, 340)),
      342 -> NGrammar.NSequence(342, Symbols.Sequence(Seq(Symbols.Nonterminal("Atom"))), Seq(343)),
      344 -> NGrammar.NSequence(344, Symbols.Sequence(Seq(Symbols.Nonterminal("BindExpr"))), Seq(345)),
      346 -> NGrammar.NSequence(346, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"), Symbols.Nonterminal("BinderExpr"))), Seq(294, 347)),
      348 -> NGrammar.NSequence(348, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedConstructExpr"))), Seq(349)),
      350 -> NGrammar.NSequence(350, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedConstructParams"))), Seq(104, 161, 4, 351)),
      352 -> NGrammar.NSequence(352, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.ExactChar(')'))), Seq(143, 4, 353, 156)),
      355 -> NGrammar.NSequence(355, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS"))), Seq(356, 358, 4)),
      357 -> NGrammar.NSequence(357, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"))), Seq(150, 95, 4, 195, 4, 326)),
      359 -> NGrammar.NSequence(359, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))))), Seq(358, 360)),
      362 -> NGrammar.NSequence(362, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam"))), Seq(4, 137, 4, 356)),
      363 -> NGrammar.NSequence(363, Symbols.Sequence(Seq(Symbols.Nonterminal("FuncCallOrConstructExpr"))), Seq(364)),
      365 -> NGrammar.NSequence(365, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeOrFuncName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("CallParams"))), Seq(366, 4, 367)),
      368 -> NGrammar.NSequence(368, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar(')'))), Seq(143, 4, 369, 156)),
      372 -> NGrammar.NSequence(372, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS"))), Seq(326, 373, 4)),
      374 -> NGrammar.NSequence(374, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))))), Seq(373, 375)),
      377 -> NGrammar.NSequence(377, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"))), Seq(4, 137, 4, 326)),
      378 -> NGrammar.NSequence(378, Symbols.Sequence(Seq(Symbols.Nonterminal("ArrayExpr"))), Seq(379)),
      380 -> NGrammar.NSequence(380, Symbols.Sequence(Seq(Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar(']'))), Seq(106, 4, 369, 107)),
      381 -> NGrammar.NSequence(381, Symbols.Sequence(Seq(Symbols.Nonterminal("Literal"))), Seq(382)),
      383 -> NGrammar.NSequence(383, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))))))), Seq(384)),
      387 -> NGrammar.NSequence(387, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("CharChar"), Symbols.ExactChar('\''))), Seq(227, 388, 227)),
      389 -> NGrammar.NSequence(389, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChar"))), Seq(228)),
      390 -> NGrammar.NSequence(390, Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), Symbols.ExactChar('"'))), Seq(261, 391, 261)),
      392 -> NGrammar.NSequence(392, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), Symbols.Nonterminal("StrChar"))), Seq(391, 393)),
      394 -> NGrammar.NSequence(394, Symbols.Sequence(Seq(Symbols.Nonterminal("StringChar"))), Seq(264)),
      395 -> NGrammar.NSequence(395, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumValue"))), Seq(396)),
      397 -> NGrammar.NSequence(397, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))))))))), Seq(398)),
      401 -> NGrammar.NSequence(401, Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue"))), Seq(402)),
      403 -> NGrammar.NSequence(403, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"), Symbols.ExactChar('.'), Symbols.Nonterminal("EnumValueName"))), Seq(117, 239, 404)),
      405 -> NGrammar.NSequence(405, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(49)),
      407 -> NGrammar.NSequence(407, Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))), Seq(408)),
      409 -> NGrammar.NSequence(409, Symbols.Sequence(Seq(Symbols.ExactChar('%'), Symbols.Nonterminal("EnumValueName"))), Seq(119, 404)),
      410 -> NGrammar.NSequence(410, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(143, 4, 326, 4, 156)),
      413 -> NGrammar.NSequence(413, Symbols.Sequence(Seq(Symbols.ExactChar('+'))), Seq(221)),
      414 -> NGrammar.NSequence(414, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"))), Seq(340)),
      416 -> NGrammar.NSequence(416, Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':'))), Seq(194, 99)),
      417 -> NGrammar.NSequence(417, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"))), Seq(338)),
      420 -> NGrammar.NSequence(420, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('=')))))), Seq(421)),
      422 -> NGrammar.NSequence(422, Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))), Seq(195, 195)),
      424 -> NGrammar.NSequence(424, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('=')))))), Seq(425)),
      426 -> NGrammar.NSequence(426, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))), Seq(214, 195)),
      427 -> NGrammar.NSequence(427, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"))), Seq(336)),
      429 -> NGrammar.NSequence(429, Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|'))), Seq(279, 279)),
      430 -> NGrammar.NSequence(430, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"))), Seq(334)),
      432 -> NGrammar.NSequence(432, Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&'))), Seq(209, 209)),
      433 -> NGrammar.NSequence(433, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"))), Seq(332)),
      437 -> NGrammar.NSequence(437, Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))), Seq(328)),
      438 -> NGrammar.NSequence(438, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"))), Seq(330)),
      440 -> NGrammar.NSequence(440, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))))), Seq(439, 441)),
      443 -> NGrammar.NSequence(443, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem"))), Seq(4, 203)),
      445 -> NGrammar.NSequence(445, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))))), Seq(444, 446)),
      448 -> NGrammar.NSequence(448, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS"))), Seq(4, 279, 4, 199)),
      450 -> NGrammar.NSequence(450, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))))), Seq(449, 451)),
      453 -> NGrammar.NSequence(453, Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def"))), Seq(454, 36)),
      455 -> NGrammar.NSequence(455, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS"))))))))), Seq(456)),
      459 -> NGrammar.NSequence(459, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS"))), Seq(460, 27, 4)),
      461 -> NGrammar.NSequence(461, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))))), Seq(460, 462)),
      464 -> NGrammar.NSequence(464, Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' ')))), Seq(465))),
    1)

  sealed trait TerminalChoiceElem

  sealed trait PostUnSymbol extends PreUnSymbol

  sealed trait BoolOrExpr extends TernaryExpr

  sealed trait BinderExpr

  case class EmptySeq()(val astNode: Node) extends AtomSymbol

  case class StringType()(val astNode: Node) extends ValueType

  sealed trait Terminal extends AtomSymbol

  sealed trait Atom extends PrefixNotExpr

  case class AbstractClassDef(name: TypeName, supers: List[TypeName])(val astNode: Node) extends ClassDef

  case class NotFollowedBy(notFollowedBy: PreUnSymbol)(val astNode: Node) extends PreUnSymbol

  sealed trait AbstractEnumValue extends Atom

  case class ClassParamDef(name: ParamName, typeDesc: Option[TypeDesc])(val astNode: Node)

  case class EnumTypeDef(name: EnumTypeName, values: List[String])(val astNode: Node) extends TypeDef

  case class Nonterminal(name: NonterminalName)(val astNode: Node) extends AtomSymbol

  case class FollowedBy(followedBy: PreUnSymbol)(val astNode: Node) extends PreUnSymbol

  case class AnyType()(val astNode: Node) extends NonNullTypeDesc

  sealed trait Processor extends Elem

  case class TypeName(name: String)(val astNode: Node) extends SubType with NonNullTypeDesc

  case class Optional(body: PostUnSymbol)(val astNode: Node) extends PostUnSymbol

  sealed trait ValueType extends NonNullTypeDesc

  case class NamedConstructExpr(typeName: TypeName, params: List[NamedParam], supers: Option[List[TypeName]])(val astNode: Node) extends Atom

  sealed trait ClassDef extends TypeDef with SubType

  sealed trait PExpr extends BinderExpr with Processor

  case class ValRef(idx: String, condSymPath: Option[List[CondSymDir.Value]])(val astNode: Node) extends Ref

  case class ArrayExpr(elems: List[PExpr])(val astNode: Node) extends Atom

  case class TerminalChoiceRange(start: TerminalChoiceChar, end: TerminalChoiceChar)(val astNode: Node) extends TerminalChoiceElem

  sealed trait StringChar

  case class ExceptSymbol(body: BinSymbol, except: PreUnSymbol)(val astNode: Node) extends BinSymbol

  case class RawRef(idx: String, condSymPath: Option[List[CondSymDir.Value]])(val astNode: Node) extends Ref

  case class BoolLiteral(value: Boolean)(val astNode: Node) extends Literal

  sealed trait Literal extends Atom

  case class LHS(name: Nonterminal, typeDesc: Option[TypeDesc])(val astNode: Node)

  case class TernaryOp(cond: BoolOrExpr, ifTrue: TernaryExpr, ifFalse: TernaryExpr)(val astNode: Node) extends TernaryExpr

  sealed trait BoolAndExpr extends BoolOrExpr

  sealed trait PrefixNotExpr extends AdditiveExpr

  case class ConcreteClassDef(name: TypeName, supers: Option[List[TypeName]], params: List[ClassParamDef])(val astNode: Node) extends ClassDef

  case class FuncCallOrConstructExpr(funcName: TypeOrFuncName, params: List[PExpr])(val astNode: Node) extends Atom

  case class Sequence(seq: List[Elem])(val astNode: Node) extends Symbol

  case class InPlaceChoices(choices: List[Sequence])(val astNode: Node) extends AtomSymbol

  case class SuperDef(typeName: TypeName, subs: Option[List[SubType]], supers: Option[List[TypeName]])(val astNode: Node) extends TypeDef with SubType

  case class Rule(lhs: LHS, rhs: List[Sequence])(val astNode: Node) extends Def

  sealed trait TypeDef extends NonNullTypeDesc with Def

  case class StrLiteral(value: List[StringChar])(val astNode: Node) extends Literal

  case class CharAsIs(value: Char)(val astNode: Node) extends StringChar with TerminalChoiceChar with TerminalChar

  case class ElvisOp(value: AdditiveExpr, ifNull: ElvisExpr)(val astNode: Node) extends ElvisExpr

  case class AnyTerminal()(val astNode: Node) extends Terminal

  case class BindExpr(ctx: ValRef, binder: BinderExpr)(val astNode: Node) extends Atom

  sealed trait TerminalChoiceChar extends TerminalChoiceElem

  case class RepeatFromZero(body: PostUnSymbol)(val astNode: Node) extends PostUnSymbol

  case class ExprParen(body: PExpr)(val astNode: Node) extends Atom

  case class EnumTypeName(name: String)(val astNode: Node) extends NonNullTypeDesc

  case class JoinSymbol(body: BinSymbol, join: PreUnSymbol)(val astNode: Node) extends BinSymbol

  sealed trait Elem

  sealed trait AtomSymbol extends PostUnSymbol

  case class ArrayTypeDesc(elemType: TypeDesc)(val astNode: Node) extends NonNullTypeDesc

  case class NullLiteral()(val astNode: Node) extends Literal

  sealed trait SubType

  case class CharType()(val astNode: Node) extends ValueType

  case class StringSymbol(value: List[StringChar])(val astNode: Node) extends AtomSymbol

  case class BinOp(op: Op.Value, lhs: BoolAndExpr, rhs: BoolOrExpr)(val astNode: Node) extends AdditiveExpr

  sealed trait Symbol extends Elem

  case class TypeDesc(typ: NonNullTypeDesc, optional: Boolean)(val astNode: Node)

  sealed trait TernaryExpr extends PExpr

  case class NonterminalName(name: String)(val astNode: Node)

  sealed trait NonNullTypeDesc

  sealed trait AdditiveExpr extends ElvisExpr

  case class RepeatFromOne(body: PostUnSymbol)(val astNode: Node) extends PostUnSymbol

  case class Longest(choices: InPlaceChoices)(val astNode: Node) extends AtomSymbol

  case class NamedParam(name: ParamName, typeDesc: Option[TypeDesc], expr: PExpr)(val astNode: Node)

  sealed trait Def

  case class PrefixOp(op: PreOp.Value, expr: PrefixNotExpr)(val astNode: Node) extends PrefixNotExpr

  case class CharLiteral(value: TerminalChar)(val astNode: Node) extends Literal

  case class TerminalChoice(choices: List[TerminalChoiceElem])(val astNode: Node) extends AtomSymbol

  case class ShortenedEnumValue(valueName: EnumValueName)(val astNode: Node) extends AbstractEnumValue

  case class TypeOrFuncName(name: String)(val astNode: Node)

  case class ParamName(name: String)(val astNode: Node)

  case class CharUnicode(code: List[Char])(val astNode: Node) extends StringChar with TerminalChoiceChar with TerminalChar

  sealed trait PreUnSymbol extends BinSymbol

  case class BooleanType()(val astNode: Node) extends ValueType

  sealed trait BinSymbol extends Symbol

  case class TypedPExpr(body: TernaryExpr, typ: TypeDesc)(val astNode: Node) extends PExpr

  sealed trait TerminalChar extends Terminal

  case class CanonicalEnumValue(enumName: EnumTypeName, valueName: EnumValueName)(val astNode: Node) extends AbstractEnumValue

  case class CharEscaped(escapeCode: Char)(val astNode: Node) extends StringChar with TerminalChoiceChar with TerminalChar

  sealed trait Ref extends Atom

  sealed trait BoolEqExpr extends BoolAndExpr

  case class EnumValueName(name: String)(val astNode: Node)

  sealed trait ElvisExpr extends BoolEqExpr

  case class Grammar(defs: List[Def])(val astNode: Node)

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

  def matchAdditiveExpr(node: Node): AdditiveExpr = {
    val BindNode(v1, v2) = node
    val v20 = v1.id match {
      case 339 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 411)
        val BindNode(v6, v7) = v5
        val v10 = v6.id match {
          case 412 =>
            val BindNode(v8, v9) = v7
            assert(v8.id == 413)
            Op.ADD
        }
        val v11 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v12, v13) = v11
        assert(v12.id == 340)
        val v14 = v2.asInstanceOf[SequenceNode].children(4)
        val BindNode(v15, v16) = v14
        assert(v15.id == 338)
        BinOp(v10, matchPrefixNotExpr(v13), matchAdditiveExpr(v16))(v2)
      case 414 =>
        val v17 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v18, v19) = v17
        assert(v18.id == 340)
        matchPrefixNotExpr(v19)
    }
    v20
  }

  def matchAnyType(node: Node): AnyType = {
    val BindNode(v21, v22) = node
    val v23 = v21.id match {
      case 112 =>
        AnyType()(v22)
    }
    v23
  }

  def matchArrayExpr(node: Node): ArrayExpr = {
    val BindNode(v24, v25) = node
    val v52 = v24.id match {
      case 380 =>
        val v27 = v25.asInstanceOf[SequenceNode].children(2)
        val BindNode(v28, v29) = v27
        assert(v28.id == 369)
        val BindNode(v30, v31) = v29
        val v51 = v30.id match {
          case 138 =>
            None
          case 370 =>
            val BindNode(v32, v33) = v31
            assert(v32.id == 371)
            val BindNode(v34, v35) = v33
            assert(v34.id == 372)
            val v36 = v35.asInstanceOf[SequenceNode].children.head
            val BindNode(v37, v38) = v36
            assert(v37.id == 326)
            val v39 = v35.asInstanceOf[SequenceNode].children(1)
            val v40 = unrollRepeat0(v39).map { elem =>
              val BindNode(v41, v42) = elem
              assert(v41.id == 375)
              val BindNode(v43, v44) = v42
              val v50 = v43.id match {
                case 376 =>
                  val BindNode(v45, v46) = v44
                  assert(v45.id == 377)
                  val v47 = v46.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v48, v49) = v47
                  assert(v48.id == 326)
                  matchPExpr(v49)
              }
              v50
            }
            Some(List(matchPExpr(v38)) ++ v40)
        }
        val v26 = v51
        ArrayExpr(if (v26.isDefined) v26.get else List())(v25)
    }
    v52
  }

  def matchAtom(node: Node): Atom = {
    val BindNode(v53, v54) = node
    val v79 = v53.id match {
      case 291 =>
        val v55 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v56, v57) = v55
        assert(v56.id == 292)
        matchRef(v57)
      case 378 =>
        val v58 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v59, v60) = v58
        assert(v59.id == 379)
        matchArrayExpr(v60)
      case 363 =>
        val v61 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v62, v63) = v61
        assert(v62.id == 364)
        matchFuncCallOrConstructExpr(v63)
      case 381 =>
        val v64 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v65, v66) = v64
        assert(v65.id == 382)
        matchLiteral(v66)
      case 344 =>
        val v67 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v68, v69) = v67
        assert(v68.id == 345)
        matchBindExpr(v69)
      case 348 =>
        val v70 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v71, v72) = v70
        assert(v71.id == 349)
        matchNamedConstructExpr(v72)
      case 395 =>
        val v73 = v54.asInstanceOf[SequenceNode].children.head
        val BindNode(v74, v75) = v73
        assert(v74.id == 396)
        matchEnumValue(v75)
      case 410 =>
        val v76 = v54.asInstanceOf[SequenceNode].children(2)
        val BindNode(v77, v78) = v76
        assert(v77.id == 326)
        ExprParen(matchPExpr(v78))(v54)
    }
    v79
  }

  def matchAtomSymbol(node: Node): AtomSymbol = {
    val BindNode(v80, v81) = node
    val v103 = v80.id match {
      case 258 =>
        val v82 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v83, v84) = v82
        assert(v83.id == 259)
        matchStringSymbol(v84)
      case 270 =>
        val v85 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v86, v87) = v85
        assert(v86.id == 42)
        matchNonterminal(v87)
      case 283 =>
        val v88 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v89, v90) = v88
        assert(v89.id == 284)
        matchEmptySequence(v90)
      case 240 =>
        val v91 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v92, v93) = v91
        assert(v92.id == 241)
        matchTerminalChoice(v93)
      case 271 =>
        val v94 = v81.asInstanceOf[SequenceNode].children(2)
        val BindNode(v95, v96) = v94
        assert(v95.id == 272)
        matchInPlaceChoices(v96)
      case 224 =>
        val v97 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v98, v99) = v97
        assert(v98.id == 225)
        matchTerminal(v99)
      case 280 =>
        val v100 = v81.asInstanceOf[SequenceNode].children.head
        val BindNode(v101, v102) = v100
        assert(v101.id == 281)
        matchLongest(v102)
    }
    v103
  }

  def matchBinSymbol(node: Node): BinSymbol = {
    val BindNode(v104, v105) = node
    val v121 = v104.id match {
      case 208 =>
        val v106 = v105.asInstanceOf[SequenceNode].children.head
        val BindNode(v107, v108) = v106
        assert(v107.id == 207)
        val v109 = v105.asInstanceOf[SequenceNode].children(4)
        val BindNode(v110, v111) = v109
        assert(v110.id == 210)
        JoinSymbol(matchBinSymbol(v108), matchPreUnSymbol(v111))(v105)
      case 287 =>
        val v112 = v105.asInstanceOf[SequenceNode].children.head
        val BindNode(v113, v114) = v112
        assert(v113.id == 207)
        val v115 = v105.asInstanceOf[SequenceNode].children(4)
        val BindNode(v116, v117) = v115
        assert(v116.id == 210)
        ExceptSymbol(matchBinSymbol(v114), matchPreUnSymbol(v117))(v105)
      case 288 =>
        val v118 = v105.asInstanceOf[SequenceNode].children.head
        val BindNode(v119, v120) = v118
        assert(v119.id == 210)
        matchPreUnSymbol(v120)
    }
    v121
  }

  def matchBindExpr(node: Node): BindExpr = {
    val BindNode(v122, v123) = node
    val v130 = v122.id match {
      case 346 =>
        val v124 = v123.asInstanceOf[SequenceNode].children.head
        val BindNode(v125, v126) = v124
        assert(v125.id == 294)
        val v127 = v123.asInstanceOf[SequenceNode].children(1)
        val BindNode(v128, v129) = v127
        assert(v128.id == 347)
        BindExpr(matchValRef(v126), matchBinderExpr(v129))(v123)
    }
    v130
  }

  def matchBinderExpr(node: Node): BinderExpr = {
    val BindNode(v131, v132) = node
    val v142 = v131.id match {
      case 291 =>
        val v133 = v132.asInstanceOf[SequenceNode].children.head
        val BindNode(v134, v135) = v133
        assert(v134.id == 292)
        matchRef(v135)
      case 344 =>
        val v136 = v132.asInstanceOf[SequenceNode].children.head
        val BindNode(v137, v138) = v136
        assert(v137.id == 345)
        matchBindExpr(v138)
      case 325 =>
        val v139 = v132.asInstanceOf[SequenceNode].children(2)
        val BindNode(v140, v141) = v139
        assert(v140.id == 326)
        matchPExpr(v141)
    }
    v142
  }

  def matchBoolAndExpr(node: Node): BoolAndExpr = {
    val BindNode(v143, v144) = node
    val v154 = v143.id match {
      case 333 =>
        val v145 = v144.asInstanceOf[SequenceNode].children.head
        val BindNode(v146, v147) = v145
        assert(v146.id == 334)
        val v148 = v144.asInstanceOf[SequenceNode].children(4)
        val BindNode(v149, v150) = v148
        assert(v149.id == 332)
        BinOp(Op.OR, matchBoolEqExpr(v147), matchBoolAndExpr(v150))(v144)
      case 430 =>
        val v151 = v144.asInstanceOf[SequenceNode].children.head
        val BindNode(v152, v153) = v151
        assert(v152.id == 334)
        matchBoolEqExpr(v153)
    }
    v154
  }

  def matchBoolEqExpr(node: Node): BoolEqExpr = {
    val BindNode(v155, v156) = node
    val v176 = v155.id match {
      case 335 =>
        val v157 = v156.asInstanceOf[SequenceNode].children(2)
        val BindNode(v158, v159) = v157
        assert(v158.id == 418)
        val BindNode(v160, v161) = v159
        val v166 = v160.id match {
          case 419 =>
            val BindNode(v162, v163) = v161
            assert(v162.id == 420)
            Op.EQ
          case 423 =>
            val BindNode(v164, v165) = v161
            assert(v164.id == 424)
            Op.NE
        }
        val v167 = v156.asInstanceOf[SequenceNode].children.head
        val BindNode(v168, v169) = v167
        assert(v168.id == 336)
        val v170 = v156.asInstanceOf[SequenceNode].children(4)
        val BindNode(v171, v172) = v170
        assert(v171.id == 334)
        BinOp(v166, matchElvisExpr(v169), matchBoolEqExpr(v172))(v156)
      case 427 =>
        val v173 = v156.asInstanceOf[SequenceNode].children.head
        val BindNode(v174, v175) = v173
        assert(v174.id == 336)
        matchElvisExpr(v175)
    }
    v176
  }

  def matchBoolOrExpr(node: Node): BoolOrExpr = {
    val BindNode(v177, v178) = node
    val v188 = v177.id match {
      case 331 =>
        val v179 = v178.asInstanceOf[SequenceNode].children.head
        val BindNode(v180, v181) = v179
        assert(v180.id == 332)
        val v182 = v178.asInstanceOf[SequenceNode].children(4)
        val BindNode(v183, v184) = v182
        assert(v183.id == 330)
        BinOp(Op.AND, matchBoolAndExpr(v181), matchBoolOrExpr(v184))(v178)
      case 433 =>
        val v185 = v178.asInstanceOf[SequenceNode].children.head
        val BindNode(v186, v187) = v185
        assert(v186.id == 332)
        matchBoolAndExpr(v187)
    }
    v188
  }

  def matchCallParams(node: Node): List[PExpr] = {
    val BindNode(v189, v190) = node
    val v218 = v189.id match {
      case 368 =>
        val v192 = v190.asInstanceOf[SequenceNode].children(2)
        val BindNode(v193, v194) = v192
        assert(v193.id == 369)
        val BindNode(v195, v196) = v194
        val v217 = v195.id match {
          case 138 =>
            None
          case 370 =>
            val BindNode(v197, v198) = v196
            val v216 = v197.id match {
              case 371 =>
                val BindNode(v199, v200) = v198
                assert(v199.id == 372)
                val v201 = v200.asInstanceOf[SequenceNode].children.head
                val BindNode(v202, v203) = v201
                assert(v202.id == 326)
                val v204 = v200.asInstanceOf[SequenceNode].children(1)
                val v205 = unrollRepeat0(v204).map { elem =>
                  val BindNode(v206, v207) = elem
                  assert(v206.id == 375)
                  val BindNode(v208, v209) = v207
                  val v215 = v208.id match {
                    case 376 =>
                      val BindNode(v210, v211) = v209
                      assert(v210.id == 377)
                      val v212 = v211.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v213, v214) = v212
                      assert(v213.id == 326)
                      matchPExpr(v214)
                  }
                  v215
                }
                List(matchPExpr(v203)) ++ v205
            }
            Some(v216)
        }
        val v191 = v217
        if (v191.isDefined) v191.get else List()
    }
    v218
  }

  def matchCanonicalEnumValue(node: Node): CanonicalEnumValue = {
    val BindNode(v219, v220) = node
    val v227 = v219.id match {
      case 403 =>
        val v221 = v220.asInstanceOf[SequenceNode].children.head
        val BindNode(v222, v223) = v221
        assert(v222.id == 117)
        val v224 = v220.asInstanceOf[SequenceNode].children(2)
        val BindNode(v225, v226) = v224
        assert(v225.id == 404)
        CanonicalEnumValue(matchEnumTypeName(v223), matchEnumValueName(v226))(v220)
    }
    v227
  }

  def matchCharChar(node: Node): TerminalChar = {
    val BindNode(v228, v229) = node
    val v233 = v228.id match {
      case 389 =>
        val v230 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v231, v232) = v230
        assert(v231.id == 228)
        matchTerminalChar(v232)
    }
    v233
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v234, v235) = node
    val v257 = v234.id match {
      case 124 =>
        val v236 = v235.asInstanceOf[SequenceNode].children.head
        val BindNode(v237, v238) = v236
        assert(v237.id == 104)
        val v239 = v235.asInstanceOf[SequenceNode].children(2)
        val BindNode(v240, v241) = v239
        assert(v240.id == 125)
        AbstractClassDef(matchTypeName(v238), matchSuperTypes(v241))(v235)
      case 140 =>
        val v242 = v235.asInstanceOf[SequenceNode].children.head
        val BindNode(v243, v244) = v242
        assert(v243.id == 104)
        val v245 = v235.asInstanceOf[SequenceNode].children(2)
        val BindNode(v246, v247) = v245
        assert(v246.id == 141)
        ConcreteClassDef(matchTypeName(v244), None, matchClassParamsDef(v247))(v235)
      case 157 =>
        val v248 = v235.asInstanceOf[SequenceNode].children.head
        val BindNode(v249, v250) = v248
        assert(v249.id == 104)
        val v251 = v235.asInstanceOf[SequenceNode].children(2)
        val BindNode(v252, v253) = v251
        assert(v252.id == 125)
        val v254 = v235.asInstanceOf[SequenceNode].children(4)
        val BindNode(v255, v256) = v254
        assert(v255.id == 141)
        ConcreteClassDef(matchTypeName(v250), Some(matchSuperTypes(v253)), matchClassParamsDef(v256))(v235)
    }
    v257
  }

  def matchClassParamDef(node: Node): ClassParamDef = {
    val BindNode(v258, v259) = node
    val v277 = v258.id match {
      case 149 =>
        val v260 = v259.asInstanceOf[SequenceNode].children.head
        val BindNode(v261, v262) = v260
        assert(v261.id == 150)
        val v263 = v259.asInstanceOf[SequenceNode].children(1)
        val BindNode(v264, v265) = v263
        assert(v264.id == 95)
        val BindNode(v266, v267) = v265
        val v276 = v266.id match {
          case 138 =>
            None
          case 96 =>
            val BindNode(v268, v269) = v267
            val v275 = v268.id match {
              case 97 =>
                val BindNode(v270, v271) = v269
                assert(v270.id == 98)
                val v272 = v271.asInstanceOf[SequenceNode].children(3)
                val BindNode(v273, v274) = v272
                assert(v273.id == 100)
                matchTypeDesc(v274)
            }
            Some(v275)
        }
        ClassParamDef(matchParamName(v262), v276)(v259)
    }
    v277
  }

  def matchClassParamsDef(node: Node): List[ClassParamDef] = {
    val BindNode(v278, v279) = node
    val v307 = v278.id match {
      case 142 =>
        val v281 = v279.asInstanceOf[SequenceNode].children(2)
        val BindNode(v282, v283) = v281
        assert(v282.id == 144)
        val BindNode(v284, v285) = v283
        val v306 = v284.id match {
          case 138 =>
            None
          case 145 =>
            val BindNode(v286, v287) = v285
            val v305 = v286.id match {
              case 146 =>
                val BindNode(v288, v289) = v287
                assert(v288.id == 147)
                val v290 = v289.asInstanceOf[SequenceNode].children.head
                val BindNode(v291, v292) = v290
                assert(v291.id == 148)
                val v293 = v289.asInstanceOf[SequenceNode].children(1)
                val v294 = unrollRepeat0(v293).map { elem =>
                  val BindNode(v295, v296) = elem
                  assert(v295.id == 153)
                  val BindNode(v297, v298) = v296
                  val v304 = v297.id match {
                    case 154 =>
                      val BindNode(v299, v300) = v298
                      assert(v299.id == 155)
                      val v301 = v300.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v302, v303) = v301
                      assert(v302.id == 148)
                      matchClassParamDef(v303)
                  }
                  v304
                }
                List(matchClassParamDef(v292)) ++ v294
            }
            Some(v305)
        }
        val v280 = v306
        if (v280.isDefined) v280.get else List()
    }
    v307
  }

  def matchCondSymPath(node: Node): List[CondSymDir.Value] = {
    val BindNode(v308, v309) = node
    val v321 = v308.id match {
      case 299 =>
        val v310 = v309.asInstanceOf[SequenceNode].children.head
        val v311 = unrollRepeat1(v310).map { elem =>
          val BindNode(v312, v313) = elem
          assert(v312.id == 301)
          val BindNode(v314, v315) = v313
          val v320 = v314.id match {
            case 302 =>
              val BindNode(v316, v317) = v315
              assert(v316.id == 303)
              CondSymDir.BODY
            case 304 =>
              val BindNode(v318, v319) = v315
              assert(v318.id == 305)
              CondSymDir.COND
          }
          v320
        }
        v311
    }
    v321
  }

  def matchDef(node: Node): Def = {
    val BindNode(v322, v323) = node
    val v330 = v322.id match {
      case 37 =>
        val v324 = v323.asInstanceOf[SequenceNode].children.head
        val BindNode(v325, v326) = v324
        assert(v325.id == 38)
        matchRule(v326)
      case 120 =>
        val v327 = v323.asInstanceOf[SequenceNode].children.head
        val BindNode(v328, v329) = v327
        assert(v328.id == 121)
        matchTypeDef(v329)
    }
    v330
  }

  def matchElem(node: Node): Elem = {
    val BindNode(v331, v332) = node
    val v339 = v331.id match {
      case 204 =>
        val v333 = v332.asInstanceOf[SequenceNode].children.head
        val BindNode(v334, v335) = v333
        assert(v334.id == 205)
        matchSymbol(v335)
      case 289 =>
        val v336 = v332.asInstanceOf[SequenceNode].children.head
        val BindNode(v337, v338) = v336
        assert(v337.id == 290)
        matchProcessor(v338)
    }
    v339
  }

  def matchElvisExpr(node: Node): ElvisExpr = {
    val BindNode(v340, v341) = node
    val v351 = v340.id match {
      case 337 =>
        val v342 = v341.asInstanceOf[SequenceNode].children.head
        val BindNode(v343, v344) = v342
        assert(v343.id == 338)
        val v345 = v341.asInstanceOf[SequenceNode].children(4)
        val BindNode(v346, v347) = v345
        assert(v346.id == 336)
        ElvisOp(matchAdditiveExpr(v344), matchElvisExpr(v347))(v341)
      case 417 =>
        val v348 = v341.asInstanceOf[SequenceNode].children.head
        val BindNode(v349, v350) = v348
        assert(v349.id == 338)
        matchAdditiveExpr(v350)
    }
    v351
  }

  def matchEmptySequence(node: Node): EmptySeq = {
    val BindNode(v352, v353) = node
    val v354 = v352.id match {
      case 285 =>
        EmptySeq()(v353)
    }
    v354
  }

  def matchEnumTypeDef(node: Node): EnumTypeDef = {
    val BindNode(v355, v356) = node
    val v383 = v355.id match {
      case 181 =>
        val v357 = v356.asInstanceOf[SequenceNode].children.head
        val BindNode(v358, v359) = v357
        assert(v358.id == 117)
        val v360 = v356.asInstanceOf[SequenceNode].children(4)
        val BindNode(v361, v362) = v360
        assert(v361.id == 182)
        val BindNode(v363, v364) = v362
        val v382 = v363.id match {
          case 183 =>
            val BindNode(v365, v366) = v364
            assert(v365.id == 184)
            val v367 = v366.asInstanceOf[SequenceNode].children.head
            val BindNode(v368, v369) = v367
            assert(v368.id == 49)
            val v370 = v366.asInstanceOf[SequenceNode].children(1)
            val v371 = unrollRepeat0(v370).map { elem =>
              val BindNode(v372, v373) = elem
              assert(v372.id == 187)
              val BindNode(v374, v375) = v373
              val v381 = v374.id match {
                case 188 =>
                  val BindNode(v376, v377) = v375
                  assert(v376.id == 189)
                  val v378 = v377.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v379, v380) = v378
                  assert(v379.id == 49)
                  matchId(v380)
              }
              v381
            }
            List(matchId(v369)) ++ v371
        }
        EnumTypeDef(matchEnumTypeName(v359), v382)(v356)
    }
    v383
  }

  def matchEnumTypeName(node: Node): EnumTypeName = {
    val BindNode(v384, v385) = node
    val v389 = v384.id match {
      case 118 =>
        val v386 = v385.asInstanceOf[SequenceNode].children(1)
        val BindNode(v387, v388) = v386
        assert(v387.id == 49)
        EnumTypeName(matchId(v388))(v385)
    }
    v389
  }

  def matchEnumValue(node: Node): AbstractEnumValue = {
    val BindNode(v390, v391) = node
    val v410 = v390.id match {
      case 397 =>
        val v392 = v391.asInstanceOf[SequenceNode].children.head
        val BindNode(v393, v394) = v392
        assert(v393.id == 398)
        val BindNode(v395, v396) = v394
        assert(v395.id == 399)
        val BindNode(v397, v398) = v396
        val v409 = v397.id match {
          case 400 =>
            val BindNode(v399, v400) = v398
            assert(v399.id == 401)
            val v401 = v400.asInstanceOf[SequenceNode].children.head
            val BindNode(v402, v403) = v401
            assert(v402.id == 402)
            matchCanonicalEnumValue(v403)
          case 406 =>
            val BindNode(v404, v405) = v398
            assert(v404.id == 407)
            val v406 = v405.asInstanceOf[SequenceNode].children.head
            val BindNode(v407, v408) = v406
            assert(v407.id == 408)
            matchShortenedEnumValue(v408)
        }
        v409
    }
    v410
  }

  def matchEnumValueName(node: Node): EnumValueName = {
    val BindNode(v411, v412) = node
    val v416 = v411.id match {
      case 405 =>
        val v413 = v412.asInstanceOf[SequenceNode].children.head
        val BindNode(v414, v415) = v413
        assert(v414.id == 49)
        EnumValueName(matchId(v415))(v412)
    }
    v416
  }

  def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
    val BindNode(v417, v418) = node
    val v425 = v417.id match {
      case 365 =>
        val v419 = v418.asInstanceOf[SequenceNode].children.head
        val BindNode(v420, v421) = v419
        assert(v420.id == 366)
        val v422 = v418.asInstanceOf[SequenceNode].children(2)
        val BindNode(v423, v424) = v422
        assert(v423.id == 367)
        FuncCallOrConstructExpr(matchTypeOrFuncName(v421), matchCallParams(v424))(v418)
    }
    v425
  }

  def matchGrammar(node: Node): Grammar = {
    val BindNode(v426, v427) = node
    val v443 = v426.id match {
      case 3 =>
        val v428 = v427.asInstanceOf[SequenceNode].children(1)
        val BindNode(v429, v430) = v428
        assert(v429.id == 36)
        val v431 = v427.asInstanceOf[SequenceNode].children(2)
        val v432 = unrollRepeat0(v431).map { elem =>
          val BindNode(v433, v434) = elem
          assert(v433.id == 451)
          val BindNode(v435, v436) = v434
          val v442 = v435.id match {
            case 452 =>
              val BindNode(v437, v438) = v436
              assert(v437.id == 453)
              val v439 = v438.asInstanceOf[SequenceNode].children(1)
              val BindNode(v440, v441) = v439
              assert(v440.id == 36)
              matchDef(v441)
          }
          v442
        }
        Grammar(List(matchDef(v430)) ++ v432)(v427)
    }
    v443
  }

  def matchId(node: Node): String = {
    val BindNode(v444, v445) = node
    val v447 = v444.id match {
      case 50 =>
        val v446 = v445.asInstanceOf[SequenceNode].children.head
        v446.sourceText
    }
    v447
  }

  def matchIdNoKeyword(node: Node): String = {
    val BindNode(v448, v449) = node
    val v451 = v448.id match {
      case 47 =>
        val v450 = v449.asInstanceOf[SequenceNode].children.head
        v450.sourceText
    }
    v451
  }

  def matchInPlaceChoices(node: Node): InPlaceChoices = {
    val BindNode(v452, v453) = node
    val v469 = v452.id match {
      case 273 =>
        val v454 = v453.asInstanceOf[SequenceNode].children.head
        val BindNode(v455, v456) = v454
        assert(v455.id == 201)
        val v457 = v453.asInstanceOf[SequenceNode].children(1)
        val v458 = unrollRepeat0(v457).map { elem =>
          val BindNode(v459, v460) = elem
          assert(v459.id == 276)
          val BindNode(v461, v462) = v460
          val v468 = v461.id match {
            case 277 =>
              val BindNode(v463, v464) = v462
              assert(v463.id == 278)
              val v465 = v464.asInstanceOf[SequenceNode].children(3)
              val BindNode(v466, v467) = v465
              assert(v466.id == 201)
              matchSequence(v467)
          }
          v468
        }
        InPlaceChoices(List(matchSequence(v456)) ++ v458)(v453)
    }
    v469
  }

  def matchLHS(node: Node): LHS = {
    val BindNode(v470, v471) = node
    val v489 = v470.id match {
      case 41 =>
        val v472 = v471.asInstanceOf[SequenceNode].children.head
        val BindNode(v473, v474) = v472
        assert(v473.id == 42)
        val v475 = v471.asInstanceOf[SequenceNode].children(1)
        val BindNode(v476, v477) = v475
        assert(v476.id == 95)
        val BindNode(v478, v479) = v477
        val v488 = v478.id match {
          case 138 =>
            None
          case 96 =>
            val BindNode(v480, v481) = v479
            val v487 = v480.id match {
              case 97 =>
                val BindNode(v482, v483) = v481
                assert(v482.id == 98)
                val v484 = v483.asInstanceOf[SequenceNode].children(3)
                val BindNode(v485, v486) = v484
                assert(v485.id == 100)
                matchTypeDesc(v486)
            }
            Some(v487)
        }
        LHS(matchNonterminal(v474), v488)(v471)
    }
    v489
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v490, v491) = node
    val v509 = v490.id match {
      case 90 =>
        NullLiteral()(v491)
      case 383 =>
        val v492 = v491.asInstanceOf[SequenceNode].children.head
        val BindNode(v493, v494) = v492
        assert(v493.id == 384)
        val BindNode(v495, v496) = v494
        val v501 = v495.id match {
          case 385 =>
            val BindNode(v497, v498) = v496
            assert(v497.id == 82)
            true
          case 386 =>
            val BindNode(v499, v500) = v496
            assert(v499.id == 86)
            false
        }
        BoolLiteral(v501)(v491)
      case 387 =>
        val v502 = v491.asInstanceOf[SequenceNode].children(1)
        val BindNode(v503, v504) = v502
        assert(v503.id == 388)
        CharLiteral(matchCharChar(v504))(v491)
      case 390 =>
        val v505 = v491.asInstanceOf[SequenceNode].children(1)
        val v506 = unrollRepeat0(v505).map { elem =>
          val BindNode(v507, v508) = elem
          assert(v507.id == 393)
          matchStrChar(v508)
        }
        StrLiteral(v506)(v491)
    }
    v509
  }

  def matchLongest(node: Node): Longest = {
    val BindNode(v510, v511) = node
    val v515 = v510.id match {
      case 282 =>
        val v512 = v511.asInstanceOf[SequenceNode].children(2)
        val BindNode(v513, v514) = v512
        assert(v513.id == 272)
        Longest(matchInPlaceChoices(v514))(v511)
    }
    v515
  }

  def matchNamedConstructExpr(node: Node): NamedConstructExpr = {
    val BindNode(v516, v517) = node
    val v538 = v516.id match {
      case 350 =>
        val v518 = v517.asInstanceOf[SequenceNode].children.head
        val BindNode(v519, v520) = v518
        assert(v519.id == 104)
        val v521 = v517.asInstanceOf[SequenceNode].children(3)
        val BindNode(v522, v523) = v521
        assert(v522.id == 351)
        val v524 = v517.asInstanceOf[SequenceNode].children(1)
        val BindNode(v525, v526) = v524
        assert(v525.id == 161)
        val BindNode(v527, v528) = v526
        val v537 = v527.id match {
          case 138 =>
            None
          case 162 =>
            val BindNode(v529, v530) = v528
            val v536 = v529.id match {
              case 163 =>
                val BindNode(v531, v532) = v530
                assert(v531.id == 164)
                val v533 = v532.asInstanceOf[SequenceNode].children(1)
                val BindNode(v534, v535) = v533
                assert(v534.id == 125)
                matchSuperTypes(v535)
            }
            Some(v536)
        }
        NamedConstructExpr(matchTypeName(v520), matchNamedConstructParams(v523), v537)(v517)
    }
    v538
  }

  def matchNamedConstructParams(node: Node): List[NamedParam] = {
    val BindNode(v539, v540) = node
    val v564 = v539.id match {
      case 352 =>
        val v541 = v540.asInstanceOf[SequenceNode].children(2)
        val BindNode(v542, v543) = v541
        assert(v542.id == 353)
        val BindNode(v544, v545) = v543
        val v563 = v544.id match {
          case 354 =>
            val BindNode(v546, v547) = v545
            assert(v546.id == 355)
            val v548 = v547.asInstanceOf[SequenceNode].children.head
            val BindNode(v549, v550) = v548
            assert(v549.id == 356)
            val v551 = v547.asInstanceOf[SequenceNode].children(1)
            val v552 = unrollRepeat0(v551).map { elem =>
              val BindNode(v553, v554) = elem
              assert(v553.id == 360)
              val BindNode(v555, v556) = v554
              val v562 = v555.id match {
                case 361 =>
                  val BindNode(v557, v558) = v556
                  assert(v557.id == 362)
                  val v559 = v558.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v560, v561) = v559
                  assert(v560.id == 356)
                  matchNamedParam(v561)
              }
              v562
            }
            List(matchNamedParam(v550)) ++ v552
        }
        v563
    }
    v564
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v565, v566) = node
    val v587 = v565.id match {
      case 357 =>
        val v567 = v566.asInstanceOf[SequenceNode].children.head
        val BindNode(v568, v569) = v567
        assert(v568.id == 150)
        val v570 = v566.asInstanceOf[SequenceNode].children(1)
        val BindNode(v571, v572) = v570
        assert(v571.id == 95)
        val BindNode(v573, v574) = v572
        val v583 = v573.id match {
          case 138 =>
            None
          case 96 =>
            val BindNode(v575, v576) = v574
            val v582 = v575.id match {
              case 97 =>
                val BindNode(v577, v578) = v576
                assert(v577.id == 98)
                val v579 = v578.asInstanceOf[SequenceNode].children(3)
                val BindNode(v580, v581) = v579
                assert(v580.id == 100)
                matchTypeDesc(v581)
            }
            Some(v582)
        }
        val v584 = v566.asInstanceOf[SequenceNode].children(5)
        val BindNode(v585, v586) = v584
        assert(v585.id == 326)
        NamedParam(matchParamName(v569), v583, matchPExpr(v586))(v566)
    }
    v587
  }

  def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
    val BindNode(v588, v589) = node
    val v608 = v588.id match {
      case 116 =>
        val v590 = v589.asInstanceOf[SequenceNode].children.head
        val BindNode(v591, v592) = v590
        assert(v591.id == 117)
        matchEnumTypeName(v592)
      case 120 =>
        val v593 = v589.asInstanceOf[SequenceNode].children.head
        val BindNode(v594, v595) = v593
        assert(v594.id == 121)
        matchTypeDef(v595)
      case 110 =>
        val v596 = v589.asInstanceOf[SequenceNode].children.head
        val BindNode(v597, v598) = v596
        assert(v597.id == 111)
        matchAnyType(v598)
      case 103 =>
        val v599 = v589.asInstanceOf[SequenceNode].children.head
        val BindNode(v600, v601) = v599
        assert(v600.id == 104)
        matchTypeName(v601)
      case 108 =>
        val v602 = v589.asInstanceOf[SequenceNode].children.head
        val BindNode(v603, v604) = v602
        assert(v603.id == 109)
        matchValueType(v604)
      case 105 =>
        val v605 = v589.asInstanceOf[SequenceNode].children(2)
        val BindNode(v606, v607) = v605
        assert(v606.id == 100)
        ArrayTypeDesc(matchTypeDesc(v607))(v589)
    }
    v608
  }

  def matchNonterminal(node: Node): Nonterminal = {
    val BindNode(v609, v610) = node
    val v614 = v609.id match {
      case 43 =>
        val v611 = v610.asInstanceOf[SequenceNode].children.head
        val BindNode(v612, v613) = v611
        assert(v612.id == 44)
        Nonterminal(matchNonterminalName(v613))(v610)
    }
    v614
  }

  def matchNonterminalName(node: Node): NonterminalName = {
    val BindNode(v615, v616) = node
    val v623 = v615.id match {
      case 45 =>
        val v617 = v616.asInstanceOf[SequenceNode].children.head
        val BindNode(v618, v619) = v617
        assert(v618.id == 46)
        NonterminalName(matchIdNoKeyword(v619))(v616)
      case 93 =>
        val v620 = v616.asInstanceOf[SequenceNode].children(1)
        val BindNode(v621, v622) = v620
        assert(v621.id == 49)
        NonterminalName(matchId(v622))(v616)
    }
    v623
  }

  def matchPExpr(node: Node): PExpr = {
    val BindNode(v624, v625) = node
    val v635 = v624.id match {
      case 327 =>
        val v626 = v625.asInstanceOf[SequenceNode].children.head
        val BindNode(v627, v628) = v626
        assert(v627.id == 328)
        val v629 = v625.asInstanceOf[SequenceNode].children(4)
        val BindNode(v630, v631) = v629
        assert(v630.id == 100)
        TypedPExpr(matchTernaryExpr(v628), matchTypeDesc(v631))(v625)
      case 437 =>
        val v632 = v625.asInstanceOf[SequenceNode].children.head
        val BindNode(v633, v634) = v632
        assert(v633.id == 328)
        matchTernaryExpr(v634)
    }
    v635
  }

  def matchParamName(node: Node): ParamName = {
    val BindNode(v636, v637) = node
    val v644 = v636.id match {
      case 45 =>
        val v638 = v637.asInstanceOf[SequenceNode].children.head
        val BindNode(v639, v640) = v638
        assert(v639.id == 46)
        ParamName(matchIdNoKeyword(v640))(v637)
      case 93 =>
        val v641 = v637.asInstanceOf[SequenceNode].children(1)
        val BindNode(v642, v643) = v641
        assert(v642.id == 49)
        ParamName(matchId(v643))(v637)
    }
    v644
  }

  def matchPostUnSymbol(node: Node): PostUnSymbol = {
    val BindNode(v645, v646) = node
    val v659 = v645.id match {
      case 217 =>
        val v647 = v646.asInstanceOf[SequenceNode].children.head
        val BindNode(v648, v649) = v647
        assert(v648.id == 216)
        Optional(matchPostUnSymbol(v649))(v646)
      case 218 =>
        val v650 = v646.asInstanceOf[SequenceNode].children.head
        val BindNode(v651, v652) = v650
        assert(v651.id == 216)
        RepeatFromZero(matchPostUnSymbol(v652))(v646)
      case 220 =>
        val v653 = v646.asInstanceOf[SequenceNode].children.head
        val BindNode(v654, v655) = v653
        assert(v654.id == 216)
        RepeatFromOne(matchPostUnSymbol(v655))(v646)
      case 222 =>
        val v656 = v646.asInstanceOf[SequenceNode].children.head
        val BindNode(v657, v658) = v656
        assert(v657.id == 223)
        matchAtomSymbol(v658)
    }
    v659
  }

  def matchPreUnSymbol(node: Node): PreUnSymbol = {
    val BindNode(v660, v661) = node
    val v671 = v660.id match {
      case 211 =>
        val v662 = v661.asInstanceOf[SequenceNode].children(2)
        val BindNode(v663, v664) = v662
        assert(v663.id == 210)
        FollowedBy(matchPreUnSymbol(v664))(v661)
      case 213 =>
        val v665 = v661.asInstanceOf[SequenceNode].children(2)
        val BindNode(v666, v667) = v665
        assert(v666.id == 210)
        NotFollowedBy(matchPreUnSymbol(v667))(v661)
      case 215 =>
        val v668 = v661.asInstanceOf[SequenceNode].children.head
        val BindNode(v669, v670) = v668
        assert(v669.id == 216)
        matchPostUnSymbol(v670)
    }
    v671
  }

  def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
    val BindNode(v672, v673) = node
    val v680 = v672.id match {
      case 341 =>
        val v674 = v673.asInstanceOf[SequenceNode].children(2)
        val BindNode(v675, v676) = v674
        assert(v675.id == 340)
        PrefixOp(PreOp.NOT, matchPrefixNotExpr(v676))(v673)
      case 342 =>
        val v677 = v673.asInstanceOf[SequenceNode].children.head
        val BindNode(v678, v679) = v677
        assert(v678.id == 343)
        matchAtom(v679)
    }
    v680
  }

  def matchProcessor(node: Node): Processor = {
    val BindNode(v681, v682) = node
    val v689 = v681.id match {
      case 291 =>
        val v683 = v682.asInstanceOf[SequenceNode].children.head
        val BindNode(v684, v685) = v683
        assert(v684.id == 292)
        matchRef(v685)
      case 325 =>
        val v686 = v682.asInstanceOf[SequenceNode].children(2)
        val BindNode(v687, v688) = v686
        assert(v687.id == 326)
        matchPExpr(v688)
    }
    v689
  }

  def matchRHS(node: Node): Sequence = {
    val BindNode(v690, v691) = node
    val v695 = v690.id match {
      case 200 =>
        val v692 = v691.asInstanceOf[SequenceNode].children.head
        val BindNode(v693, v694) = v692
        assert(v693.id == 201)
        matchSequence(v694)
    }
    v695
  }

  def matchRawRef(node: Node): RawRef = {
    val BindNode(v696, v697) = node
    val v707 = v696.id match {
      case 322 =>
        val v698 = v697.asInstanceOf[SequenceNode].children(2)
        val BindNode(v699, v700) = v698
        assert(v699.id == 307)
        val v701 = v697.asInstanceOf[SequenceNode].children(1)
        val BindNode(v702, v703) = v701
        assert(v702.id == 297)
        val BindNode(v704, v705) = v703
        val v706 = v704.id match {
          case 138 =>
            None
          case 298 =>
            Some(matchCondSymPath(v705))
        }
        RawRef(matchRefIdx(v700), v706)(v697)
    }
    v707
  }

  def matchRef(node: Node): Ref = {
    val BindNode(v708, v709) = node
    val v716 = v708.id match {
      case 293 =>
        val v710 = v709.asInstanceOf[SequenceNode].children.head
        val BindNode(v711, v712) = v710
        assert(v711.id == 294)
        matchValRef(v712)
      case 320 =>
        val v713 = v709.asInstanceOf[SequenceNode].children.head
        val BindNode(v714, v715) = v713
        assert(v714.id == 321)
        matchRawRef(v715)
    }
    v716
  }

  def matchRefIdx(node: Node): String = {
    val BindNode(v717, v718) = node
    val v720 = v717.id match {
      case 308 =>
        val v719 = v718.asInstanceOf[SequenceNode].children.head
        v719.sourceText
    }
    v720
  }

  def matchRule(node: Node): Rule = {
    val BindNode(v721, v722) = node
    val v749 = v721.id match {
      case 39 =>
        val v723 = v722.asInstanceOf[SequenceNode].children.head
        val BindNode(v724, v725) = v723
        assert(v724.id == 40)
        val v726 = v722.asInstanceOf[SequenceNode].children(4)
        val BindNode(v727, v728) = v726
        assert(v727.id == 196)
        val BindNode(v729, v730) = v728
        val v748 = v729.id match {
          case 197 =>
            val BindNode(v731, v732) = v730
            assert(v731.id == 198)
            val v733 = v732.asInstanceOf[SequenceNode].children.head
            val BindNode(v734, v735) = v733
            assert(v734.id == 199)
            val v736 = v732.asInstanceOf[SequenceNode].children(1)
            val v737 = unrollRepeat0(v736).map { elem =>
              val BindNode(v738, v739) = elem
              assert(v738.id == 446)
              val BindNode(v740, v741) = v739
              val v747 = v740.id match {
                case 447 =>
                  val BindNode(v742, v743) = v741
                  assert(v742.id == 448)
                  val v744 = v743.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v745, v746) = v744
                  assert(v745.id == 199)
                  matchRHS(v746)
              }
              v747
            }
            List(matchRHS(v735)) ++ v737
        }
        Rule(matchLHS(v725), v748)(v722)
    }
    v749
  }

  def matchSequence(node: Node): Sequence = {
    val BindNode(v750, v751) = node
    val v767 = v750.id match {
      case 202 =>
        val v752 = v751.asInstanceOf[SequenceNode].children.head
        val BindNode(v753, v754) = v752
        assert(v753.id == 203)
        val v755 = v751.asInstanceOf[SequenceNode].children(1)
        val v756 = unrollRepeat0(v755).map { elem =>
          val BindNode(v757, v758) = elem
          assert(v757.id == 441)
          val BindNode(v759, v760) = v758
          val v766 = v759.id match {
            case 442 =>
              val BindNode(v761, v762) = v760
              assert(v761.id == 443)
              val v763 = v762.asInstanceOf[SequenceNode].children(1)
              val BindNode(v764, v765) = v763
              assert(v764.id == 203)
              matchElem(v765)
          }
          v766
        }
        Sequence(List(matchElem(v754)) ++ v756)(v751)
    }
    v767
  }

  def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
    val BindNode(v768, v769) = node
    val v773 = v768.id match {
      case 409 =>
        val v770 = v769.asInstanceOf[SequenceNode].children(1)
        val BindNode(v771, v772) = v770
        assert(v771.id == 404)
        ShortenedEnumValue(matchEnumValueName(v772))(v769)
    }
    v773
  }

  def matchStrChar(node: Node): StringChar = {
    val BindNode(v774, v775) = node
    val v779 = v774.id match {
      case 394 =>
        val v776 = v775.asInstanceOf[SequenceNode].children.head
        val BindNode(v777, v778) = v776
        assert(v777.id == 264)
        matchStringChar(v778)
    }
    v779
  }

  def matchStringChar(node: Node): StringChar = {
    val BindNode(v780, v781) = node
    val v793 = v780.id match {
      case 265 =>
        val v782 = v781.asInstanceOf[SequenceNode].children.head
        val BindNode(v783, v784) = v782
        assert(v783.id == 266)
        val BindNode(v785, v786) = v784
        assert(v785.id == 26)
        CharAsIs(v786.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v781)
      case 268 =>
        val v787 = v781.asInstanceOf[SequenceNode].children(1)
        val BindNode(v788, v789) = v787
        assert(v788.id == 269)
        CharEscaped(v789.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v781)
      case 234 =>
        val v790 = v781.asInstanceOf[SequenceNode].children.head
        val BindNode(v791, v792) = v790
        assert(v791.id == 235)
        matchUnicodeChar(v792)
    }
    v793
  }

  def matchStringSymbol(node: Node): StringSymbol = {
    val BindNode(v794, v795) = node
    val v800 = v794.id match {
      case 260 =>
        val v796 = v795.asInstanceOf[SequenceNode].children(1)
        val v797 = unrollRepeat0(v796).map { elem =>
          val BindNode(v798, v799) = elem
          assert(v798.id == 264)
          matchStringChar(v799)
        }
        StringSymbol(v797)(v795)
    }
    v800
  }

  def matchSubType(node: Node): SubType = {
    val BindNode(v801, v802) = node
    val v812 = v801.id match {
      case 103 =>
        val v803 = v802.asInstanceOf[SequenceNode].children.head
        val BindNode(v804, v805) = v803
        assert(v804.id == 104)
        matchTypeName(v805)
      case 122 =>
        val v806 = v802.asInstanceOf[SequenceNode].children.head
        val BindNode(v807, v808) = v806
        assert(v807.id == 123)
        matchClassDef(v808)
      case 158 =>
        val v809 = v802.asInstanceOf[SequenceNode].children.head
        val BindNode(v810, v811) = v809
        assert(v810.id == 159)
        matchSuperDef(v811)
    }
    v812
  }

  def matchSubTypes(node: Node): List[SubType] = {
    val BindNode(v813, v814) = node
    val v830 = v813.id match {
      case 171 =>
        val v815 = v814.asInstanceOf[SequenceNode].children.head
        val BindNode(v816, v817) = v815
        assert(v816.id == 172)
        val v818 = v814.asInstanceOf[SequenceNode].children(1)
        val v819 = unrollRepeat0(v818).map { elem =>
          val BindNode(v820, v821) = elem
          assert(v820.id == 175)
          val BindNode(v822, v823) = v821
          val v829 = v822.id match {
            case 176 =>
              val BindNode(v824, v825) = v823
              assert(v824.id == 177)
              val v826 = v825.asInstanceOf[SequenceNode].children(3)
              val BindNode(v827, v828) = v826
              assert(v827.id == 172)
              matchSubType(v828)
          }
          v829
        }
        List(matchSubType(v817)) ++ v819
    }
    v830
  }

  def matchSuperDef(node: Node): SuperDef = {
    val BindNode(v831, v832) = node
    val v864 = v831.id match {
      case 160 =>
        val v833 = v832.asInstanceOf[SequenceNode].children.head
        val BindNode(v834, v835) = v833
        assert(v834.id == 104)
        val v836 = v832.asInstanceOf[SequenceNode].children(4)
        val BindNode(v837, v838) = v836
        assert(v837.id == 166)
        val BindNode(v839, v840) = v838
        val v849 = v839.id match {
          case 138 =>
            None
          case 167 =>
            val BindNode(v841, v842) = v840
            val v848 = v841.id match {
              case 168 =>
                val BindNode(v843, v844) = v842
                assert(v843.id == 169)
                val v845 = v844.asInstanceOf[SequenceNode].children(1)
                val BindNode(v846, v847) = v845
                assert(v846.id == 170)
                matchSubTypes(v847)
            }
            Some(v848)
        }
        val v850 = v832.asInstanceOf[SequenceNode].children(1)
        val BindNode(v851, v852) = v850
        assert(v851.id == 161)
        val BindNode(v853, v854) = v852
        val v863 = v853.id match {
          case 138 =>
            None
          case 162 =>
            val BindNode(v855, v856) = v854
            val v862 = v855.id match {
              case 163 =>
                val BindNode(v857, v858) = v856
                assert(v857.id == 164)
                val v859 = v858.asInstanceOf[SequenceNode].children(1)
                val BindNode(v860, v861) = v859
                assert(v860.id == 125)
                matchSuperTypes(v861)
            }
            Some(v862)
        }
        SuperDef(matchTypeName(v835), v849, v863)(v832)
    }
    v864
  }

  def matchSuperTypes(node: Node): List[TypeName] = {
    val BindNode(v865, v866) = node
    val v894 = v865.id match {
      case 126 =>
        val v868 = v866.asInstanceOf[SequenceNode].children(2)
        val BindNode(v869, v870) = v868
        assert(v869.id == 128)
        val BindNode(v871, v872) = v870
        val v893 = v871.id match {
          case 138 =>
            None
          case 129 =>
            val BindNode(v873, v874) = v872
            val v892 = v873.id match {
              case 130 =>
                val BindNode(v875, v876) = v874
                assert(v875.id == 131)
                val v877 = v876.asInstanceOf[SequenceNode].children.head
                val BindNode(v878, v879) = v877
                assert(v878.id == 104)
                val v880 = v876.asInstanceOf[SequenceNode].children(1)
                val v881 = unrollRepeat0(v880).map { elem =>
                  val BindNode(v882, v883) = elem
                  assert(v882.id == 134)
                  val BindNode(v884, v885) = v883
                  val v891 = v884.id match {
                    case 135 =>
                      val BindNode(v886, v887) = v885
                      assert(v886.id == 136)
                      val v888 = v887.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v889, v890) = v888
                      assert(v889.id == 104)
                      matchTypeName(v890)
                  }
                  v891
                }
                List(matchTypeName(v879)) ++ v881
            }
            Some(v892)
        }
        val v867 = v893
        if (v867.isDefined) v867.get else List()
    }
    v894
  }

  def matchSymbol(node: Node): Symbol = {
    val BindNode(v895, v896) = node
    val v900 = v895.id match {
      case 206 =>
        val v897 = v896.asInstanceOf[SequenceNode].children.head
        val BindNode(v898, v899) = v897
        assert(v898.id == 207)
        matchBinSymbol(v899)
    }
    v900
  }

  def matchTerminal(node: Node): Terminal = {
    val BindNode(v901, v902) = node
    val v906 = v901.id match {
      case 226 =>
        val v903 = v902.asInstanceOf[SequenceNode].children(1)
        val BindNode(v904, v905) = v903
        assert(v904.id == 228)
        matchTerminalChar(v905)
      case 238 =>
        AnyTerminal()(v902)
    }
    v906
  }

  def matchTerminalChar(node: Node): TerminalChar = {
    val BindNode(v907, v908) = node
    val v920 = v907.id match {
      case 229 =>
        val v909 = v908.asInstanceOf[SequenceNode].children.head
        val BindNode(v910, v911) = v909
        assert(v910.id == 230)
        val BindNode(v912, v913) = v911
        assert(v912.id == 26)
        CharAsIs(v913.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v908)
      case 232 =>
        val v914 = v908.asInstanceOf[SequenceNode].children(1)
        val BindNode(v915, v916) = v914
        assert(v915.id == 233)
        CharEscaped(v916.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v908)
      case 234 =>
        val v917 = v908.asInstanceOf[SequenceNode].children.head
        val BindNode(v918, v919) = v917
        assert(v918.id == 235)
        matchUnicodeChar(v919)
    }
    v920
  }

  def matchTerminalChoice(node: Node): TerminalChoice = {
    val BindNode(v921, v922) = node
    val v933 = v921.id match {
      case 242 =>
        val v923 = v922.asInstanceOf[SequenceNode].children(1)
        val BindNode(v924, v925) = v923
        assert(v924.id == 243)
        val v926 = v922.asInstanceOf[SequenceNode].children(2)
        val v927 = unrollRepeat1(v926).map { elem =>
          val BindNode(v928, v929) = elem
          assert(v928.id == 243)
          matchTerminalChoiceElem(v929)
        }
        TerminalChoice(List(matchTerminalChoiceElem(v925)) ++ v927)(v922)
      case 257 =>
        val v930 = v922.asInstanceOf[SequenceNode].children(1)
        val BindNode(v931, v932) = v930
        assert(v931.id == 252)
        TerminalChoice(List(matchTerminalChoiceRange(v932)))(v922)
    }
    v933
  }

  def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
    val BindNode(v934, v935) = node
    val v947 = v934.id match {
      case 246 =>
        val v936 = v935.asInstanceOf[SequenceNode].children.head
        val BindNode(v937, v938) = v936
        assert(v937.id == 247)
        val BindNode(v939, v940) = v938
        assert(v939.id == 26)
        CharAsIs(v940.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v935)
      case 249 =>
        val v941 = v935.asInstanceOf[SequenceNode].children(1)
        val BindNode(v942, v943) = v941
        assert(v942.id == 250)
        CharEscaped(v943.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v935)
      case 234 =>
        val v944 = v935.asInstanceOf[SequenceNode].children.head
        val BindNode(v945, v946) = v944
        assert(v945.id == 235)
        matchUnicodeChar(v946)
    }
    v947
  }

  def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
    val BindNode(v948, v949) = node
    val v956 = v948.id match {
      case 244 =>
        val v950 = v949.asInstanceOf[SequenceNode].children.head
        val BindNode(v951, v952) = v950
        assert(v951.id == 245)
        matchTerminalChoiceChar(v952)
      case 251 =>
        val v953 = v949.asInstanceOf[SequenceNode].children.head
        val BindNode(v954, v955) = v953
        assert(v954.id == 252)
        matchTerminalChoiceRange(v955)
    }
    v956
  }

  def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
    val BindNode(v957, v958) = node
    val v965 = v957.id match {
      case 253 =>
        val v959 = v958.asInstanceOf[SequenceNode].children.head
        val BindNode(v960, v961) = v959
        assert(v960.id == 245)
        val v962 = v958.asInstanceOf[SequenceNode].children(2)
        val BindNode(v963, v964) = v962
        assert(v963.id == 245)
        TerminalChoiceRange(matchTerminalChoiceChar(v961), matchTerminalChoiceChar(v964))(v958)
    }
    v965
  }

  def matchTernaryExpr(node: Node): TernaryExpr = {
    val BindNode(v966, v967) = node
    val v1000 = v966.id match {
      case 329 =>
        val v968 = v967.asInstanceOf[SequenceNode].children.head
        val BindNode(v969, v970) = v968
        assert(v969.id == 330)
        val v971 = v967.asInstanceOf[SequenceNode].children(4)
        val BindNode(v972, v973) = v971
        assert(v972.id == 434)
        val BindNode(v974, v975) = v973
        assert(v974.id == 435)
        val BindNode(v976, v977) = v975
        val v983 = v976.id match {
          case 436 =>
            val BindNode(v978, v979) = v977
            assert(v978.id == 437)
            val v980 = v979.asInstanceOf[SequenceNode].children.head
            val BindNode(v981, v982) = v980
            assert(v981.id == 328)
            matchTernaryExpr(v982)
        }
        val v984 = v967.asInstanceOf[SequenceNode].children(8)
        val BindNode(v985, v986) = v984
        assert(v985.id == 434)
        val BindNode(v987, v988) = v986
        assert(v987.id == 435)
        val BindNode(v989, v990) = v988
        val v996 = v989.id match {
          case 436 =>
            val BindNode(v991, v992) = v990
            assert(v991.id == 437)
            val v993 = v992.asInstanceOf[SequenceNode].children.head
            val BindNode(v994, v995) = v993
            assert(v994.id == 328)
            matchTernaryExpr(v995)
        }
        TernaryOp(matchBoolOrExpr(v970), v983, v996)(v967)
      case 438 =>
        val v997 = v967.asInstanceOf[SequenceNode].children.head
        val BindNode(v998, v999) = v997
        assert(v998.id == 330)
        matchBoolOrExpr(v999)
    }
    v1000
  }

  def matchTypeDef(node: Node): TypeDef = {
    val BindNode(v1001, v1002) = node
    val v1012 = v1001.id match {
      case 122 =>
        val v1003 = v1002.asInstanceOf[SequenceNode].children.head
        val BindNode(v1004, v1005) = v1003
        assert(v1004.id == 123)
        matchClassDef(v1005)
      case 158 =>
        val v1006 = v1002.asInstanceOf[SequenceNode].children.head
        val BindNode(v1007, v1008) = v1006
        assert(v1007.id == 159)
        matchSuperDef(v1008)
      case 179 =>
        val v1009 = v1002.asInstanceOf[SequenceNode].children.head
        val BindNode(v1010, v1011) = v1009
        assert(v1010.id == 180)
        matchEnumTypeDef(v1011)
    }
    v1012
  }

  def matchTypeDesc(node: Node): TypeDesc = {
    val BindNode(v1013, v1014) = node
    val v1032 = v1013.id match {
      case 101 =>
        val v1015 = v1014.asInstanceOf[SequenceNode].children.head
        val BindNode(v1016, v1017) = v1015
        assert(v1016.id == 102)
        val v1018 = v1014.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1019, v1020) = v1018
        assert(v1019.id == 190)
        val BindNode(v1021, v1022) = v1020
        val v1031 = v1021.id match {
          case 138 =>
            None
          case 191 =>
            val BindNode(v1023, v1024) = v1022
            val v1030 = v1023.id match {
              case 192 =>
                val BindNode(v1025, v1026) = v1024
                assert(v1025.id == 193)
                val v1027 = v1026.asInstanceOf[SequenceNode].children(1)
                val BindNode(v1028, v1029) = v1027
                assert(v1028.id == 194)
                v1029.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v1030)
        }
        TypeDesc(matchNonNullTypeDesc(v1017), v1031.isDefined)(v1014)
    }
    v1032
  }

  def matchTypeName(node: Node): TypeName = {
    val BindNode(v1033, v1034) = node
    val v1041 = v1033.id match {
      case 45 =>
        val v1035 = v1034.asInstanceOf[SequenceNode].children.head
        val BindNode(v1036, v1037) = v1035
        assert(v1036.id == 46)
        TypeName(matchIdNoKeyword(v1037))(v1034)
      case 93 =>
        val v1038 = v1034.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1039, v1040) = v1038
        assert(v1039.id == 49)
        TypeName(matchId(v1040))(v1034)
    }
    v1041
  }

  def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
    val BindNode(v1042, v1043) = node
    val v1050 = v1042.id match {
      case 45 =>
        val v1044 = v1043.asInstanceOf[SequenceNode].children.head
        val BindNode(v1045, v1046) = v1044
        assert(v1045.id == 46)
        TypeOrFuncName(matchIdNoKeyword(v1046))(v1043)
      case 93 =>
        val v1047 = v1043.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1048, v1049) = v1047
        assert(v1048.id == 49)
        TypeOrFuncName(matchId(v1049))(v1043)
    }
    v1050
  }

  def matchUnicodeChar(node: Node): CharUnicode = {
    val BindNode(v1051, v1052) = node
    val v1065 = v1051.id match {
      case 236 =>
        val v1053 = v1052.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1054, v1055) = v1053
        assert(v1054.id == 237)
        val v1056 = v1052.asInstanceOf[SequenceNode].children(3)
        val BindNode(v1057, v1058) = v1056
        assert(v1057.id == 237)
        val v1059 = v1052.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1060, v1061) = v1059
        assert(v1060.id == 237)
        val v1062 = v1052.asInstanceOf[SequenceNode].children(5)
        val BindNode(v1063, v1064) = v1062
        assert(v1063.id == 237)
        CharUnicode(List(v1055.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v1058.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v1061.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, v1064.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char))(v1052)
    }
    v1065
  }

  def matchValRef(node: Node): ValRef = {
    val BindNode(v1066, v1067) = node
    val v1077 = v1066.id match {
      case 295 =>
        val v1068 = v1067.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1069, v1070) = v1068
        assert(v1069.id == 307)
        val v1071 = v1067.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1072, v1073) = v1071
        assert(v1072.id == 297)
        val BindNode(v1074, v1075) = v1073
        val v1076 = v1074.id match {
          case 138 =>
            None
          case 298 =>
            Some(matchCondSymPath(v1075))
        }
        ValRef(matchRefIdx(v1070), v1076)(v1067)
    }
    v1077
  }

  def matchValueType(node: Node): ValueType = {
    val BindNode(v1078, v1079) = node
    val v1080 = v1078.id match {
      case 60 =>
        BooleanType()(v1079)
      case 69 =>
        CharType()(v1079)
      case 75 =>
        StringType()(v1079)
    }
    v1080
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
    println(parseAst("A = B C 'd' 'e'*"))
  }
}
