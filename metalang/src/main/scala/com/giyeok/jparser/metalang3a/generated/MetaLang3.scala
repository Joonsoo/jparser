package com.giyeok.jparser.metalang3a.generated

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.ParseResultTree.BindNode
import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.ParseResultTree.SequenceNode
import com.giyeok.jparser.ParseResultTree.TerminalNode
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParseTreeConstructor
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
      34 -> NGrammar.NNonterminal(34, Symbols.Nonterminal("Def"), Set(35, 116)),
      36 -> NGrammar.NNonterminal(36, Symbols.Nonterminal("Rule"), Set(37)),
      38 -> NGrammar.NNonterminal(38, Symbols.Nonterminal("LHS"), Set(39)),
      40 -> NGrammar.NNonterminal(40, Symbols.Nonterminal("Nonterminal"), Set(41)),
      42 -> NGrammar.NNonterminal(42, Symbols.Nonterminal("NonterminalName"), Set(43, 89)),
      44 -> NGrammar.NExcept(44, Symbols.Except(Symbols.Nonterminal("Id"), Symbols.Nonterminal("Keyword")), 45, 55),
      45 -> NGrammar.NNonterminal(45, Symbols.Nonterminal("Id"), Set(46)),
      47 -> NGrammar.NLongest(47, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))), 48),
      48 -> NGrammar.NOneOf(48, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))))), Set(49)),
      49 -> NGrammar.NProxy(49, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))), 50),
      51 -> NGrammar.NTerminal(51, Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
      52 -> NGrammar.NRepeat(52, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), 7, 53),
      54 -> NGrammar.NTerminal(54, Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
      55 -> NGrammar.NNonterminal(55, Symbols.Nonterminal("Keyword"), Set(56, 65, 71, 78, 82, 86)),
      57 -> NGrammar.NProxy(57, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('n')))), 58),
      59 -> NGrammar.NTerminal(59, Symbols.ExactChar('b')),
      60 -> NGrammar.NTerminal(60, Symbols.ExactChar('o')),
      61 -> NGrammar.NTerminal(61, Symbols.ExactChar('l')),
      62 -> NGrammar.NTerminal(62, Symbols.ExactChar('e')),
      63 -> NGrammar.NTerminal(63, Symbols.ExactChar('a')),
      64 -> NGrammar.NTerminal(64, Symbols.ExactChar('n')),
      66 -> NGrammar.NProxy(66, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('c'), Symbols.ExactChar('h'), Symbols.ExactChar('a'), Symbols.ExactChar('r')))), 67),
      68 -> NGrammar.NTerminal(68, Symbols.ExactChar('c')),
      69 -> NGrammar.NTerminal(69, Symbols.ExactChar('h')),
      70 -> NGrammar.NTerminal(70, Symbols.ExactChar('r')),
      72 -> NGrammar.NProxy(72, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g')))), 73),
      74 -> NGrammar.NTerminal(74, Symbols.ExactChar('s')),
      75 -> NGrammar.NTerminal(75, Symbols.ExactChar('t')),
      76 -> NGrammar.NTerminal(76, Symbols.ExactChar('i')),
      77 -> NGrammar.NTerminal(77, Symbols.ExactChar('g')),
      79 -> NGrammar.NProxy(79, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e')))), 80),
      81 -> NGrammar.NTerminal(81, Symbols.ExactChar('u')),
      83 -> NGrammar.NProxy(83, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e')))), 84),
      85 -> NGrammar.NTerminal(85, Symbols.ExactChar('f')),
      87 -> NGrammar.NProxy(87, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('l'), Symbols.ExactChar('l')))), 88),
      90 -> NGrammar.NTerminal(90, Symbols.ExactChar('`')),
      91 -> NGrammar.NOneOf(91, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(92, 134)),
      92 -> NGrammar.NOneOf(92, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Set(93)),
      93 -> NGrammar.NProxy(93, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))), 94),
      95 -> NGrammar.NTerminal(95, Symbols.ExactChar(':')),
      96 -> NGrammar.NNonterminal(96, Symbols.Nonterminal("TypeDesc"), Set(97)),
      98 -> NGrammar.NNonterminal(98, Symbols.Nonterminal("NonNullTypeDesc"), Set(99, 101, 104, 106, 112, 116)),
      100 -> NGrammar.NNonterminal(100, Symbols.Nonterminal("TypeName"), Set(43, 89)),
      102 -> NGrammar.NTerminal(102, Symbols.ExactChar('[')),
      103 -> NGrammar.NTerminal(103, Symbols.ExactChar(']')),
      105 -> NGrammar.NNonterminal(105, Symbols.Nonterminal("ValueType"), Set(56, 65, 71)),
      107 -> NGrammar.NNonterminal(107, Symbols.Nonterminal("AnyType"), Set(108)),
      109 -> NGrammar.NProxy(109, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'), Symbols.ExactChar('n'), Symbols.ExactChar('y')))), 110),
      111 -> NGrammar.NTerminal(111, Symbols.ExactChar('y')),
      113 -> NGrammar.NNonterminal(113, Symbols.Nonterminal("EnumTypeName"), Set(114)),
      115 -> NGrammar.NTerminal(115, Symbols.ExactChar('%')),
      117 -> NGrammar.NNonterminal(117, Symbols.Nonterminal("TypeDef"), Set(118, 154, 175)),
      119 -> NGrammar.NNonterminal(119, Symbols.Nonterminal("ClassDef"), Set(120, 136, 153)),
      121 -> NGrammar.NNonterminal(121, Symbols.Nonterminal("SuperTypes"), Set(122)),
      123 -> NGrammar.NTerminal(123, Symbols.ExactChar('<')),
      124 -> NGrammar.NOneOf(124, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(125, 134)),
      125 -> NGrammar.NOneOf(125, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))))), Set(126)),
      126 -> NGrammar.NProxy(126, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))), 127),
      128 -> NGrammar.NRepeat(128, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), 7, 129),
      130 -> NGrammar.NOneOf(130, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), Set(131)),
      131 -> NGrammar.NProxy(131, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))), 132),
      133 -> NGrammar.NTerminal(133, Symbols.ExactChar(',')),
      134 -> NGrammar.NProxy(134, Symbols.Proxy(Symbols.Sequence(Seq())), 7),
      135 -> NGrammar.NTerminal(135, Symbols.ExactChar('>')),
      137 -> NGrammar.NNonterminal(137, Symbols.Nonterminal("ClassParamsDef"), Set(138)),
      139 -> NGrammar.NTerminal(139, Symbols.ExactChar('(')),
      140 -> NGrammar.NOneOf(140, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(141, 134)),
      141 -> NGrammar.NOneOf(141, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))))), Set(142)),
      142 -> NGrammar.NProxy(142, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))), 143),
      144 -> NGrammar.NNonterminal(144, Symbols.Nonterminal("ClassParamDef"), Set(145)),
      146 -> NGrammar.NNonterminal(146, Symbols.Nonterminal("ParamName"), Set(43, 89)),
      147 -> NGrammar.NRepeat(147, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), 7, 148),
      149 -> NGrammar.NOneOf(149, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), Set(150)),
      150 -> NGrammar.NProxy(150, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))), 151),
      152 -> NGrammar.NTerminal(152, Symbols.ExactChar(')')),
      155 -> NGrammar.NNonterminal(155, Symbols.Nonterminal("SuperDef"), Set(156)),
      157 -> NGrammar.NOneOf(157, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(158, 134)),
      158 -> NGrammar.NOneOf(158, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Set(159)),
      159 -> NGrammar.NProxy(159, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))), 160),
      161 -> NGrammar.NTerminal(161, Symbols.ExactChar('{')),
      162 -> NGrammar.NOneOf(162, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(163, 134)),
      163 -> NGrammar.NOneOf(163, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))))), Set(164)),
      164 -> NGrammar.NProxy(164, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))), 165),
      166 -> NGrammar.NNonterminal(166, Symbols.Nonterminal("SubTypes"), Set(167)),
      168 -> NGrammar.NNonterminal(168, Symbols.Nonterminal("SubType"), Set(99, 118, 154)),
      169 -> NGrammar.NRepeat(169, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), 0), 7, 170),
      171 -> NGrammar.NOneOf(171, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), Set(172)),
      172 -> NGrammar.NProxy(172, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))), 173),
      174 -> NGrammar.NTerminal(174, Symbols.ExactChar('}')),
      176 -> NGrammar.NNonterminal(176, Symbols.Nonterminal("EnumTypeDef"), Set(177)),
      178 -> NGrammar.NOneOf(178, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0)))))), Set(179)),
      179 -> NGrammar.NProxy(179, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0)))), 180),
      181 -> NGrammar.NRepeat(181, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0), 7, 182),
      183 -> NGrammar.NOneOf(183, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), Set(184)),
      184 -> NGrammar.NProxy(184, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))), 185),
      186 -> NGrammar.NOneOf(186, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(187, 134)),
      187 -> NGrammar.NOneOf(187, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))))), Set(188)),
      188 -> NGrammar.NProxy(188, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))), 189),
      190 -> NGrammar.NTerminal(190, Symbols.ExactChar('?')),
      191 -> NGrammar.NTerminal(191, Symbols.ExactChar('=')),
      192 -> NGrammar.NOneOf(192, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0)))))), Set(193)),
      193 -> NGrammar.NProxy(193, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0)))), 194),
      195 -> NGrammar.NNonterminal(195, Symbols.Nonterminal("RHS"), Set(196)),
      197 -> NGrammar.NNonterminal(197, Symbols.Nonterminal("Sequence"), Set(198)),
      199 -> NGrammar.NNonterminal(199, Symbols.Nonterminal("Elem"), Set(200, 285)),
      201 -> NGrammar.NNonterminal(201, Symbols.Nonterminal("Symbol"), Set(202)),
      203 -> NGrammar.NNonterminal(203, Symbols.Nonterminal("BinSymbol"), Set(204, 283, 284)),
      205 -> NGrammar.NTerminal(205, Symbols.ExactChar('&')),
      206 -> NGrammar.NNonterminal(206, Symbols.Nonterminal("PreUnSymbol"), Set(207, 209, 211)),
      208 -> NGrammar.NTerminal(208, Symbols.ExactChar('^')),
      210 -> NGrammar.NTerminal(210, Symbols.ExactChar('!')),
      212 -> NGrammar.NNonterminal(212, Symbols.Nonterminal("PostUnSymbol"), Set(213, 214, 216, 218)),
      215 -> NGrammar.NTerminal(215, Symbols.ExactChar('*')),
      217 -> NGrammar.NTerminal(217, Symbols.ExactChar('+')),
      219 -> NGrammar.NNonterminal(219, Symbols.Nonterminal("AtomSymbol"), Set(220, 236, 254, 266, 267, 276, 279)),
      221 -> NGrammar.NNonterminal(221, Symbols.Nonterminal("Terminal"), Set(222, 234)),
      223 -> NGrammar.NTerminal(223, Symbols.ExactChar('\'')),
      224 -> NGrammar.NNonterminal(224, Symbols.Nonterminal("TerminalChar"), Set(225, 228, 230)),
      226 -> NGrammar.NExcept(226, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')), 24, 227),
      227 -> NGrammar.NTerminal(227, Symbols.ExactChar('\\')),
      229 -> NGrammar.NTerminal(229, Symbols.Chars(Set('\'', '\\', 'b', 'n', 'r', 't'))),
      231 -> NGrammar.NNonterminal(231, Symbols.Nonterminal("UnicodeChar"), Set(232)),
      233 -> NGrammar.NTerminal(233, Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet)),
      235 -> NGrammar.NTerminal(235, Symbols.ExactChar('.')),
      237 -> NGrammar.NNonterminal(237, Symbols.Nonterminal("TerminalChoice"), Set(238, 253)),
      239 -> NGrammar.NNonterminal(239, Symbols.Nonterminal("TerminalChoiceElem"), Set(240, 247)),
      241 -> NGrammar.NNonterminal(241, Symbols.Nonterminal("TerminalChoiceChar"), Set(242, 245, 230)),
      243 -> NGrammar.NExcept(243, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'', '-', '\\'))), 24, 244),
      244 -> NGrammar.NTerminal(244, Symbols.Chars(Set('\'', '-', '\\'))),
      246 -> NGrammar.NTerminal(246, Symbols.Chars(Set('\'', '-', '\\', 'b', 'n', 'r', 't'))),
      248 -> NGrammar.NNonterminal(248, Symbols.Nonterminal("TerminalChoiceRange"), Set(249)),
      250 -> NGrammar.NTerminal(250, Symbols.ExactChar('-')),
      251 -> NGrammar.NRepeat(251, Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), 239, 252),
      255 -> NGrammar.NNonterminal(255, Symbols.Nonterminal("StringSymbol"), Set(256)),
      257 -> NGrammar.NTerminal(257, Symbols.ExactChar('"')),
      258 -> NGrammar.NRepeat(258, Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), 7, 259),
      260 -> NGrammar.NNonterminal(260, Symbols.Nonterminal("StringChar"), Set(261, 264, 230)),
      262 -> NGrammar.NExcept(262, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"', '\\'))), 24, 263),
      263 -> NGrammar.NTerminal(263, Symbols.Chars(Set('"', '\\'))),
      265 -> NGrammar.NTerminal(265, Symbols.Chars(Set('"', '\\', 'b', 'n', 'r', 't'))),
      268 -> NGrammar.NNonterminal(268, Symbols.Nonterminal("InPlaceChoices"), Set(269)),
      270 -> NGrammar.NRepeat(270, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), 0), 7, 271),
      272 -> NGrammar.NOneOf(272, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), Set(273)),
      273 -> NGrammar.NProxy(273, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))), 274),
      275 -> NGrammar.NTerminal(275, Symbols.ExactChar('|')),
      277 -> NGrammar.NNonterminal(277, Symbols.Nonterminal("Longest"), Set(278)),
      280 -> NGrammar.NNonterminal(280, Symbols.Nonterminal("EmptySequence"), Set(281)),
      282 -> NGrammar.NTerminal(282, Symbols.ExactChar('#')),
      286 -> NGrammar.NNonterminal(286, Symbols.Nonterminal("Processor"), Set(287, 321)),
      288 -> NGrammar.NNonterminal(288, Symbols.Nonterminal("Ref"), Set(289, 316)),
      290 -> NGrammar.NNonterminal(290, Symbols.Nonterminal("ValRef"), Set(291)),
      292 -> NGrammar.NTerminal(292, Symbols.ExactChar('$')),
      293 -> NGrammar.NOneOf(293, Symbols.OneOf(ListSet(Symbols.Nonterminal("CondSymPath"), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(294, 134)),
      294 -> NGrammar.NNonterminal(294, Symbols.Nonterminal("CondSymPath"), Set(295)),
      296 -> NGrammar.NRepeat(296, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), 1), 297, 302),
      297 -> NGrammar.NOneOf(297, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), Set(298, 300)),
      298 -> NGrammar.NProxy(298, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), 299),
      300 -> NGrammar.NProxy(300, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))), 301),
      303 -> NGrammar.NNonterminal(303, Symbols.Nonterminal("RefIdx"), Set(304)),
      305 -> NGrammar.NLongest(305, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))), 306),
      306 -> NGrammar.NOneOf(306, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))))), Set(307, 310)),
      307 -> NGrammar.NProxy(307, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), 308),
      309 -> NGrammar.NTerminal(309, Symbols.ExactChar('0')),
      310 -> NGrammar.NProxy(310, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))), 311),
      312 -> NGrammar.NTerminal(312, Symbols.Chars(('1' to '9').toSet)),
      313 -> NGrammar.NRepeat(313, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 7, 314),
      315 -> NGrammar.NTerminal(315, Symbols.Chars(('0' to '9').toSet)),
      317 -> NGrammar.NNonterminal(317, Symbols.Nonterminal("RawRef"), Set(318)),
      319 -> NGrammar.NProxy(319, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$')))), 320),
      322 -> NGrammar.NNonterminal(322, Symbols.Nonterminal("PExpr"), Set(323)),
      324 -> NGrammar.NNonterminal(324, Symbols.Nonterminal("TernaryExpr"), Set(325, 434)),
      326 -> NGrammar.NNonterminal(326, Symbols.Nonterminal("BoolOrExpr"), Set(327, 429)),
      328 -> NGrammar.NNonterminal(328, Symbols.Nonterminal("BoolAndExpr"), Set(329, 426)),
      330 -> NGrammar.NNonterminal(330, Symbols.Nonterminal("BoolEqExpr"), Set(331, 423)),
      332 -> NGrammar.NNonterminal(332, Symbols.Nonterminal("ElvisExpr"), Set(333, 413)),
      334 -> NGrammar.NNonterminal(334, Symbols.Nonterminal("AdditiveExpr"), Set(335, 410)),
      336 -> NGrammar.NNonterminal(336, Symbols.Nonterminal("PrefixNotExpr"), Set(337, 338)),
      339 -> NGrammar.NNonterminal(339, Symbols.Nonterminal("Atom"), Set(287, 340, 344, 359, 374, 377, 391, 406)),
      341 -> NGrammar.NNonterminal(341, Symbols.Nonterminal("BindExpr"), Set(342)),
      343 -> NGrammar.NNonterminal(343, Symbols.Nonterminal("BinderExpr"), Set(287, 340, 321)),
      345 -> NGrammar.NNonterminal(345, Symbols.Nonterminal("NamedConstructExpr"), Set(346)),
      347 -> NGrammar.NNonterminal(347, Symbols.Nonterminal("NamedConstructParams"), Set(348)),
      349 -> NGrammar.NOneOf(349, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))))), Set(350)),
      350 -> NGrammar.NProxy(350, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))), 351),
      352 -> NGrammar.NNonterminal(352, Symbols.Nonterminal("NamedParam"), Set(353)),
      354 -> NGrammar.NRepeat(354, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), 7, 355),
      356 -> NGrammar.NOneOf(356, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), Set(357)),
      357 -> NGrammar.NProxy(357, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))), 358),
      360 -> NGrammar.NNonterminal(360, Symbols.Nonterminal("FuncCallOrConstructExpr"), Set(361)),
      362 -> NGrammar.NNonterminal(362, Symbols.Nonterminal("TypeOrFuncName"), Set(43, 89)),
      363 -> NGrammar.NNonterminal(363, Symbols.Nonterminal("CallParams"), Set(364)),
      365 -> NGrammar.NOneOf(365, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(366, 134)),
      366 -> NGrammar.NOneOf(366, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Set(367)),
      367 -> NGrammar.NProxy(367, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))), 368),
      369 -> NGrammar.NRepeat(369, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), 7, 370),
      371 -> NGrammar.NOneOf(371, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), Set(372)),
      372 -> NGrammar.NProxy(372, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 373),
      375 -> NGrammar.NNonterminal(375, Symbols.Nonterminal("ArrayExpr"), Set(376)),
      378 -> NGrammar.NNonterminal(378, Symbols.Nonterminal("Literal"), Set(86, 379, 383, 386)),
      380 -> NGrammar.NOneOf(380, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))))), Set(381, 382)),
      381 -> NGrammar.NProxy(381, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), 78),
      382 -> NGrammar.NProxy(382, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))), 82),
      384 -> NGrammar.NNonterminal(384, Symbols.Nonterminal("CharChar"), Set(385)),
      387 -> NGrammar.NRepeat(387, Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), 7, 388),
      389 -> NGrammar.NNonterminal(389, Symbols.Nonterminal("StrChar"), Set(390)),
      392 -> NGrammar.NNonterminal(392, Symbols.Nonterminal("EnumValue"), Set(393)),
      394 -> NGrammar.NLongest(394, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))))))), 395),
      395 -> NGrammar.NOneOf(395, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue")))))), Set(396, 402)),
      396 -> NGrammar.NProxy(396, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), 397),
      398 -> NGrammar.NNonterminal(398, Symbols.Nonterminal("CanonicalEnumValue"), Set(399)),
      400 -> NGrammar.NNonterminal(400, Symbols.Nonterminal("EnumValueName"), Set(401)),
      402 -> NGrammar.NProxy(402, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue")))), 403),
      404 -> NGrammar.NNonterminal(404, Symbols.Nonterminal("ShortenedEnumValue"), Set(405)),
      407 -> NGrammar.NOneOf(407, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))))), Set(408)),
      408 -> NGrammar.NProxy(408, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))), 409),
      411 -> NGrammar.NProxy(411, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':')))), 412),
      414 -> NGrammar.NOneOf(414, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))))), Set(415, 419)),
      415 -> NGrammar.NProxy(415, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), 416),
      417 -> NGrammar.NProxy(417, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('=')))), 418),
      419 -> NGrammar.NProxy(419, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))), 420),
      421 -> NGrammar.NProxy(421, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('=')))), 422),
      424 -> NGrammar.NProxy(424, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|')))), 425),
      427 -> NGrammar.NProxy(427, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&')))), 428),
      430 -> NGrammar.NLongest(430, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))), 431),
      431 -> NGrammar.NOneOf(431, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr")))))), Set(432)),
      432 -> NGrammar.NProxy(432, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr")))), 433),
      435 -> NGrammar.NRepeat(435, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), 0), 7, 436),
      437 -> NGrammar.NOneOf(437, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), Set(438)),
      438 -> NGrammar.NProxy(438, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))), 439),
      440 -> NGrammar.NRepeat(440, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0), 7, 441),
      442 -> NGrammar.NOneOf(442, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), Set(443)),
      443 -> NGrammar.NProxy(443, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))), 444),
      445 -> NGrammar.NRepeat(445, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), 0), 7, 446),
      447 -> NGrammar.NOneOf(447, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), Set(448)),
      448 -> NGrammar.NProxy(448, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))), 449),
      450 -> NGrammar.NNonterminal(450, Symbols.Nonterminal("WSNL"), Set(451)),
      452 -> NGrammar.NLongest(452, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS"))))))), 453),
      453 -> NGrammar.NOneOf(453, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS")))))), Set(454)),
      454 -> NGrammar.NProxy(454, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS")))), 455),
      456 -> NGrammar.NRepeat(456, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), 7, 457),
      458 -> NGrammar.NOneOf(458, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), Set(459, 13)),
      459 -> NGrammar.NProxy(459, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), 460),
      461 -> NGrammar.NTerminal(461, Symbols.Chars(Set('\t', '\r', ' ')))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Def"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), 0), Symbols.Nonterminal("WS"))), Seq(4, 34, 445, 4)),
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
      37 -> NGrammar.NSequence(37, Symbols.Sequence(Seq(Symbols.Nonterminal("LHS"), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0)))))))), Seq(38, 4, 191, 4, 192)),
      39 -> NGrammar.NSequence(39, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(40, 91)),
      41 -> NGrammar.NSequence(41, Symbols.Sequence(Seq(Symbols.Nonterminal("NonterminalName"))), Seq(42)),
      43 -> NGrammar.NSequence(43, Symbols.Sequence(Seq(Symbols.Except(Symbols.Nonterminal("Id"), Symbols.Nonterminal("Keyword")))), Seq(44)),
      46 -> NGrammar.NSequence(46, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))))), Seq(47)),
      50 -> NGrammar.NSequence(50, Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))), Seq(51, 52)),
      53 -> NGrammar.NSequence(53, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(52, 54)),
      56 -> NGrammar.NSequence(56, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('n')))))), Seq(57)),
      58 -> NGrammar.NSequence(58, Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('n'))), Seq(59, 60, 60, 61, 62, 63, 64)),
      65 -> NGrammar.NSequence(65, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('c'), Symbols.ExactChar('h'), Symbols.ExactChar('a'), Symbols.ExactChar('r')))))), Seq(66)),
      67 -> NGrammar.NSequence(67, Symbols.Sequence(Seq(Symbols.ExactChar('c'), Symbols.ExactChar('h'), Symbols.ExactChar('a'), Symbols.ExactChar('r'))), Seq(68, 69, 63, 70)),
      71 -> NGrammar.NSequence(71, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g')))))), Seq(72)),
      73 -> NGrammar.NSequence(73, Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g'))), Seq(74, 75, 70, 76, 64, 77)),
      78 -> NGrammar.NSequence(78, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e')))))), Seq(79)),
      80 -> NGrammar.NSequence(80, Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))), Seq(75, 70, 81, 62)),
      82 -> NGrammar.NSequence(82, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e')))))), Seq(83)),
      84 -> NGrammar.NSequence(84, Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))), Seq(85, 63, 61, 74, 62)),
      86 -> NGrammar.NSequence(86, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('l'), Symbols.ExactChar('l')))))), Seq(87)),
      88 -> NGrammar.NSequence(88, Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('l'), Symbols.ExactChar('l'))), Seq(64, 81, 61, 61)),
      89 -> NGrammar.NSequence(89, Symbols.Sequence(Seq(Symbols.ExactChar('`'), Symbols.Nonterminal("Id"), Symbols.ExactChar('`'))), Seq(90, 45, 90)),
      94 -> NGrammar.NSequence(94, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc"))), Seq(4, 95, 4, 96)),
      97 -> NGrammar.NSequence(97, Symbols.Sequence(Seq(Symbols.Nonterminal("NonNullTypeDesc"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(98, 186)),
      99 -> NGrammar.NSequence(99, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"))), Seq(100)),
      101 -> NGrammar.NSequence(101, Symbols.Sequence(Seq(Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc"), Symbols.Nonterminal("WS"), Symbols.ExactChar(']'))), Seq(102, 4, 96, 4, 103)),
      104 -> NGrammar.NSequence(104, Symbols.Sequence(Seq(Symbols.Nonterminal("ValueType"))), Seq(105)),
      106 -> NGrammar.NSequence(106, Symbols.Sequence(Seq(Symbols.Nonterminal("AnyType"))), Seq(107)),
      108 -> NGrammar.NSequence(108, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'), Symbols.ExactChar('n'), Symbols.ExactChar('y')))))), Seq(109)),
      110 -> NGrammar.NSequence(110, Symbols.Sequence(Seq(Symbols.ExactChar('a'), Symbols.ExactChar('n'), Symbols.ExactChar('y'))), Seq(63, 64, 111)),
      112 -> NGrammar.NSequence(112, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"))), Seq(113)),
      114 -> NGrammar.NSequence(114, Symbols.Sequence(Seq(Symbols.ExactChar('%'), Symbols.Nonterminal("Id"))), Seq(115, 45)),
      116 -> NGrammar.NSequence(116, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeDef"))), Seq(117)),
      118 -> NGrammar.NSequence(118, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassDef"))), Seq(119)),
      120 -> NGrammar.NSequence(120, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes"))), Seq(100, 4, 121)),
      122 -> NGrammar.NSequence(122, Symbols.Sequence(Seq(Symbols.ExactChar('<'), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar('>'))), Seq(123, 4, 124, 135)),
      127 -> NGrammar.NSequence(127, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.Nonterminal("WS"))), Seq(100, 128, 4)),
      129 -> NGrammar.NSequence(129, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))))), Seq(128, 130)),
      132 -> NGrammar.NSequence(132, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName"))), Seq(4, 133, 4, 100)),
      136 -> NGrammar.NSequence(136, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamsDef"))), Seq(100, 4, 137)),
      138 -> NGrammar.NSequence(138, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(139, 4, 140, 4, 152)),
      143 -> NGrammar.NSequence(143, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParamDef"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.Nonterminal("WS"))), Seq(144, 147, 4)),
      145 -> NGrammar.NSequence(145, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(146, 91)),
      148 -> NGrammar.NSequence(148, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef")))))))), Seq(147, 149)),
      151 -> NGrammar.NSequence(151, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamDef"))), Seq(4, 133, 4, 144)),
      153 -> NGrammar.NSequence(153, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParamsDef"))), Seq(100, 4, 121, 4, 137)),
      154 -> NGrammar.NSequence(154, Symbols.Sequence(Seq(Symbols.Nonterminal("SuperDef"))), Seq(155)),
      156 -> NGrammar.NSequence(156, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar('{'), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(100, 157, 4, 161, 162, 4, 174)),
      160 -> NGrammar.NSequence(160, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes"))), Seq(4, 121)),
      165 -> NGrammar.NSequence(165, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubTypes"))), Seq(4, 166)),
      167 -> NGrammar.NSequence(167, Symbols.Sequence(Seq(Symbols.Nonterminal("SubType"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), 0))), Seq(168, 169)),
      170 -> NGrammar.NSequence(170, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))))), Seq(169, 171)),
      173 -> NGrammar.NSequence(173, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType"))), Seq(4, 133, 4, 168)),
      175 -> NGrammar.NSequence(175, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeDef"))), Seq(176)),
      177 -> NGrammar.NSequence(177, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('{'), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0)))))), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(113, 4, 161, 4, 178, 4, 174)),
      180 -> NGrammar.NSequence(180, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0))), Seq(45, 181)),
      182 -> NGrammar.NSequence(182, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id")))))))), Seq(181, 183)),
      185 -> NGrammar.NSequence(185, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Id"))), Seq(4, 133, 4, 45)),
      189 -> NGrammar.NSequence(189, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?'))), Seq(4, 190)),
      194 -> NGrammar.NSequence(194, Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0))), Seq(195, 440)),
      196 -> NGrammar.NSequence(196, Symbols.Sequence(Seq(Symbols.Nonterminal("Sequence"))), Seq(197)),
      198 -> NGrammar.NSequence(198, Symbols.Sequence(Seq(Symbols.Nonterminal("Elem"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), 0))), Seq(199, 435)),
      200 -> NGrammar.NSequence(200, Symbols.Sequence(Seq(Symbols.Nonterminal("Symbol"))), Seq(201)),
      202 -> NGrammar.NSequence(202, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"))), Seq(203)),
      204 -> NGrammar.NSequence(204, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('&'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(203, 4, 205, 4, 206)),
      207 -> NGrammar.NSequence(207, Symbols.Sequence(Seq(Symbols.ExactChar('^'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(208, 4, 206)),
      209 -> NGrammar.NSequence(209, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(210, 4, 206)),
      211 -> NGrammar.NSequence(211, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"))), Seq(212)),
      213 -> NGrammar.NSequence(213, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('?'))), Seq(212, 4, 190)),
      214 -> NGrammar.NSequence(214, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('*'))), Seq(212, 4, 215)),
      216 -> NGrammar.NSequence(216, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('+'))), Seq(212, 4, 217)),
      218 -> NGrammar.NSequence(218, Symbols.Sequence(Seq(Symbols.Nonterminal("AtomSymbol"))), Seq(219)),
      220 -> NGrammar.NSequence(220, Symbols.Sequence(Seq(Symbols.Nonterminal("Terminal"))), Seq(221)),
      222 -> NGrammar.NSequence(222, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChar"), Symbols.ExactChar('\''))), Seq(223, 224, 223)),
      225 -> NGrammar.NSequence(225, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')))), Seq(226)),
      228 -> NGrammar.NSequence(228, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('\'', '\\', 'b', 'n', 'r', 't')))), Seq(227, 229)),
      230 -> NGrammar.NSequence(230, Symbols.Sequence(Seq(Symbols.Nonterminal("UnicodeChar"))), Seq(231)),
      232 -> NGrammar.NSequence(232, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('u'), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet))), Seq(227, 81, 233, 233, 233, 233)),
      234 -> NGrammar.NSequence(234, Symbols.Sequence(Seq(Symbols.ExactChar('.'))), Seq(235)),
      236 -> NGrammar.NSequence(236, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoice"))), Seq(237)),
      238 -> NGrammar.NSequence(238, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChoiceElem"), Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), Symbols.ExactChar('\''))), Seq(223, 239, 251, 223)),
      240 -> NGrammar.NSequence(240, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"))), Seq(241)),
      242 -> NGrammar.NSequence(242, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'', '-', '\\'))))), Seq(243)),
      245 -> NGrammar.NSequence(245, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('\'', '-', '\\', 'b', 'n', 'r', 't')))), Seq(227, 246)),
      247 -> NGrammar.NSequence(247, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceRange"))), Seq(248)),
      249 -> NGrammar.NSequence(249, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"), Symbols.ExactChar('-'), Symbols.Nonterminal("TerminalChoiceChar"))), Seq(241, 250, 241)),
      252 -> NGrammar.NSequence(252, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), Symbols.Nonterminal("TerminalChoiceElem"))), Seq(251, 239)),
      253 -> NGrammar.NSequence(253, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChoiceRange"), Symbols.ExactChar('\''))), Seq(223, 248, 223)),
      254 -> NGrammar.NSequence(254, Symbols.Sequence(Seq(Symbols.Nonterminal("StringSymbol"))), Seq(255)),
      256 -> NGrammar.NSequence(256, Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), Symbols.ExactChar('"'))), Seq(257, 258, 257)),
      259 -> NGrammar.NSequence(259, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), Symbols.Nonterminal("StringChar"))), Seq(258, 260)),
      261 -> NGrammar.NSequence(261, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"', '\\'))))), Seq(262)),
      264 -> NGrammar.NSequence(264, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('"', '\\', 'b', 'n', 'r', 't')))), Seq(227, 265)),
      266 -> NGrammar.NSequence(266, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"))), Seq(40)),
      267 -> NGrammar.NSequence(267, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceChoices"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(139, 4, 268, 4, 152)),
      269 -> NGrammar.NSequence(269, Symbols.Sequence(Seq(Symbols.Nonterminal("Sequence"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), 0))), Seq(197, 270)),
      271 -> NGrammar.NSequence(271, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence")))))))), Seq(270, 272)),
      274 -> NGrammar.NSequence(274, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Sequence"))), Seq(4, 275, 4, 197)),
      276 -> NGrammar.NSequence(276, Symbols.Sequence(Seq(Symbols.Nonterminal("Longest"))), Seq(277)),
      278 -> NGrammar.NSequence(278, Symbols.Sequence(Seq(Symbols.ExactChar('<'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceChoices"), Symbols.Nonterminal("WS"), Symbols.ExactChar('>'))), Seq(123, 4, 268, 4, 135)),
      279 -> NGrammar.NSequence(279, Symbols.Sequence(Seq(Symbols.Nonterminal("EmptySequence"))), Seq(280)),
      281 -> NGrammar.NSequence(281, Symbols.Sequence(Seq(Symbols.ExactChar('#'))), Seq(282)),
      283 -> NGrammar.NSequence(283, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('-'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(203, 4, 250, 4, 206)),
      284 -> NGrammar.NSequence(284, Symbols.Sequence(Seq(Symbols.Nonterminal("PreUnSymbol"))), Seq(206)),
      285 -> NGrammar.NSequence(285, Symbols.Sequence(Seq(Symbols.Nonterminal("Processor"))), Seq(286)),
      287 -> NGrammar.NSequence(287, Symbols.Sequence(Seq(Symbols.Nonterminal("Ref"))), Seq(288)),
      289 -> NGrammar.NSequence(289, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"))), Seq(290)),
      291 -> NGrammar.NSequence(291, Symbols.Sequence(Seq(Symbols.ExactChar('$'), Symbols.OneOf(ListSet(Symbols.Nonterminal("CondSymPath"), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("RefIdx"))), Seq(292, 293, 303)),
      295 -> NGrammar.NSequence(295, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), 1))), Seq(296)),
      299 -> NGrammar.NSequence(299, Symbols.Sequence(Seq(Symbols.ExactChar('<'))), Seq(123)),
      301 -> NGrammar.NSequence(301, Symbols.Sequence(Seq(Symbols.ExactChar('>'))), Seq(135)),
      302 -> NGrammar.NSequence(302, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))), 1), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('<')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('>')))))))), Seq(296, 297)),
      304 -> NGrammar.NSequence(304, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('0')))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))))), Seq(305)),
      308 -> NGrammar.NSequence(308, Symbols.Sequence(Seq(Symbols.ExactChar('0'))), Seq(309)),
      311 -> NGrammar.NSequence(311, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(312, 313)),
      314 -> NGrammar.NSequence(314, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), Symbols.Chars(('0' to '9').toSet))), Seq(313, 315)),
      316 -> NGrammar.NSequence(316, Symbols.Sequence(Seq(Symbols.Nonterminal("RawRef"))), Seq(317)),
      318 -> NGrammar.NSequence(318, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$')))), Symbols.OneOf(ListSet(Symbols.Nonterminal("CondSymPath"), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("RefIdx"))), Seq(319, 293, 303)),
      320 -> NGrammar.NSequence(320, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$'))), Seq(227, 292)),
      321 -> NGrammar.NSequence(321, Symbols.Sequence(Seq(Symbols.ExactChar('{'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(161, 4, 322, 4, 174)),
      323 -> NGrammar.NSequence(323, Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(324, 91)),
      325 -> NGrammar.NSequence(325, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar('?'), Symbols.Nonterminal("WS"), Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))), Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))))), Seq(326, 4, 190, 4, 430, 4, 95, 4, 430)),
      327 -> NGrammar.NSequence(327, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolOrExpr"))), Seq(328, 4, 427, 4, 326)),
      329 -> NGrammar.NSequence(329, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolAndExpr"))), Seq(330, 4, 424, 4, 328)),
      331 -> NGrammar.NSequence(331, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolEqExpr"))), Seq(332, 4, 414, 4, 330)),
      333 -> NGrammar.NSequence(333, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ElvisExpr"))), Seq(334, 4, 411, 4, 332)),
      335 -> NGrammar.NSequence(335, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("AdditiveExpr"))), Seq(336, 4, 407, 4, 334)),
      337 -> NGrammar.NSequence(337, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PrefixNotExpr"))), Seq(210, 4, 336)),
      338 -> NGrammar.NSequence(338, Symbols.Sequence(Seq(Symbols.Nonterminal("Atom"))), Seq(339)),
      340 -> NGrammar.NSequence(340, Symbols.Sequence(Seq(Symbols.Nonterminal("BindExpr"))), Seq(341)),
      342 -> NGrammar.NSequence(342, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"), Symbols.Nonterminal("BinderExpr"))), Seq(290, 343)),
      344 -> NGrammar.NSequence(344, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedConstructExpr"))), Seq(345)),
      346 -> NGrammar.NSequence(346, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedConstructParams"))), Seq(100, 157, 4, 347)),
      348 -> NGrammar.NSequence(348, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.ExactChar(')'))), Seq(139, 4, 349, 152)),
      351 -> NGrammar.NSequence(351, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS"))), Seq(352, 354, 4)),
      353 -> NGrammar.NSequence(353, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"))), Seq(146, 91, 4, 191, 4, 322)),
      355 -> NGrammar.NSequence(355, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))))), Seq(354, 356)),
      358 -> NGrammar.NSequence(358, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam"))), Seq(4, 133, 4, 352)),
      359 -> NGrammar.NSequence(359, Symbols.Sequence(Seq(Symbols.Nonterminal("FuncCallOrConstructExpr"))), Seq(360)),
      361 -> NGrammar.NSequence(361, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeOrFuncName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("CallParams"))), Seq(362, 4, 363)),
      364 -> NGrammar.NSequence(364, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar(')'))), Seq(139, 4, 365, 152)),
      368 -> NGrammar.NSequence(368, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS"))), Seq(322, 369, 4)),
      370 -> NGrammar.NSequence(370, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))))), Seq(369, 371)),
      373 -> NGrammar.NSequence(373, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"))), Seq(4, 133, 4, 322)),
      374 -> NGrammar.NSequence(374, Symbols.Sequence(Seq(Symbols.Nonterminal("ArrayExpr"))), Seq(375)),
      376 -> NGrammar.NSequence(376, Symbols.Sequence(Seq(Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar(']'))), Seq(102, 4, 365, 103)),
      377 -> NGrammar.NSequence(377, Symbols.Sequence(Seq(Symbols.Nonterminal("Literal"))), Seq(378)),
      379 -> NGrammar.NSequence(379, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))))))), Seq(380)),
      383 -> NGrammar.NSequence(383, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("CharChar"), Symbols.ExactChar('\''))), Seq(223, 384, 223)),
      385 -> NGrammar.NSequence(385, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChar"))), Seq(224)),
      386 -> NGrammar.NSequence(386, Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), Symbols.ExactChar('"'))), Seq(257, 387, 257)),
      388 -> NGrammar.NSequence(388, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), Symbols.Nonterminal("StrChar"))), Seq(387, 389)),
      390 -> NGrammar.NSequence(390, Symbols.Sequence(Seq(Symbols.Nonterminal("StringChar"))), Seq(260)),
      391 -> NGrammar.NSequence(391, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumValue"))), Seq(392)),
      393 -> NGrammar.NSequence(393, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))))))))), Seq(394)),
      397 -> NGrammar.NSequence(397, Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue"))), Seq(398)),
      399 -> NGrammar.NSequence(399, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"), Symbols.ExactChar('.'), Symbols.Nonterminal("EnumValueName"))), Seq(113, 235, 400)),
      401 -> NGrammar.NSequence(401, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(45)),
      403 -> NGrammar.NSequence(403, Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))), Seq(404)),
      405 -> NGrammar.NSequence(405, Symbols.Sequence(Seq(Symbols.ExactChar('%'), Symbols.Nonterminal("EnumValueName"))), Seq(115, 400)),
      406 -> NGrammar.NSequence(406, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(139, 4, 322, 4, 152)),
      409 -> NGrammar.NSequence(409, Symbols.Sequence(Seq(Symbols.ExactChar('+'))), Seq(217)),
      410 -> NGrammar.NSequence(410, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"))), Seq(336)),
      412 -> NGrammar.NSequence(412, Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':'))), Seq(190, 95)),
      413 -> NGrammar.NSequence(413, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"))), Seq(334)),
      416 -> NGrammar.NSequence(416, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('=')))))), Seq(417)),
      418 -> NGrammar.NSequence(418, Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))), Seq(191, 191)),
      420 -> NGrammar.NSequence(420, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('=')))))), Seq(421)),
      422 -> NGrammar.NSequence(422, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))), Seq(210, 191)),
      423 -> NGrammar.NSequence(423, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"))), Seq(332)),
      425 -> NGrammar.NSequence(425, Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|'))), Seq(275, 275)),
      426 -> NGrammar.NSequence(426, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"))), Seq(330)),
      428 -> NGrammar.NSequence(428, Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&'))), Seq(205, 205)),
      429 -> NGrammar.NSequence(429, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"))), Seq(328)),
      433 -> NGrammar.NSequence(433, Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))), Seq(324)),
      434 -> NGrammar.NSequence(434, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"))), Seq(326)),
      436 -> NGrammar.NSequence(436, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))))), Seq(435, 437)),
      439 -> NGrammar.NSequence(439, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem"))), Seq(4, 199)),
      441 -> NGrammar.NSequence(441, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))))), Seq(440, 442)),
      444 -> NGrammar.NSequence(444, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS"))), Seq(4, 275, 4, 195)),
      446 -> NGrammar.NSequence(446, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))))), Seq(445, 447)),
      449 -> NGrammar.NSequence(449, Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def"))), Seq(450, 34)),
      451 -> NGrammar.NSequence(451, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS"))))))))), Seq(452)),
      455 -> NGrammar.NSequence(455, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS"))), Seq(456, 25, 4)),
      457 -> NGrammar.NSequence(457, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))))), Seq(456, 458)),
      460 -> NGrammar.NSequence(460, Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' ')))), Seq(461))),
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

  case class JoinSymbol(body: BinSymbol, join: PreUnSymbol)(val astNode:Node) extends BinSymbol

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

  def matchRule(node: Node): Rule = {
    val BindNode(v502, v503) = node
    v502.id match {
      case 37 =>
        val v504 = v503.asInstanceOf[SequenceNode].children.head
        val BindNode(v505, v506) = v504
        assert(v505.id == 38)
        val v507 = v503.asInstanceOf[SequenceNode].children(4)
        val BindNode(v508, v509) = v507
        assert(v508.id == 192)
        val BindNode(v510, v511) = v509
        Rule(matchLHS(v506), v510.id match {
          case 193 =>
            val BindNode(v512, v513) = v511
            assert(v512.id == 194)
            val v514 = v513.asInstanceOf[SequenceNode].children.head
            val BindNode(v515, v516) = v514
            assert(v515.id == 195)
            val v517 = v513.asInstanceOf[SequenceNode].children(1)
            val v518 = unrollRepeat0(v517).map { elem =>
              val BindNode(v519, v520) = elem
              assert(v519.id == 442)
              val BindNode(v521, v522) = v520
              v521.id match {
                case 443 =>
                  val BindNode(v523, v524) = v522
                  assert(v523.id == 444)
                  val v525 = v524.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v526, v527) = v525
                  assert(v526.id == 195)
                  matchRHS(v527)
              }
            }
            List(matchRHS(v516)) ++ v518
        })
    }
  }

  def matchSequence(node: Node): Sequence = {
    val BindNode(v441, v442) = node
    v441.id match {
      case 198 =>
        val v443 = v442.asInstanceOf[SequenceNode].children.head
        val BindNode(v444, v445) = v443
        assert(v444.id == 199)
        val v446 = v442.asInstanceOf[SequenceNode].children(1)
        val v447 = unrollRepeat0(v446).map { elem =>
          val BindNode(v448, v449) = elem
          assert(v448.id == 437)
          val BindNode(v450, v451) = v449
          v450.id match {
            case 438 =>
              val BindNode(v452, v453) = v451
              assert(v452.id == 439)
              val v454 = v453.asInstanceOf[SequenceNode].children(1)
              val BindNode(v455, v456) = v454
              assert(v455.id == 199)
              matchElem(v456)
          }
        }
        Sequence(List(matchElem(v445)) ++ v447)
    }
  }

  def matchRHS(node: Node): Sequence = {
    val BindNode(v675, v676) = node
    v675.id match {
      case 196 =>
        val v677 = v676.asInstanceOf[SequenceNode].children.head
        val BindNode(v678, v679) = v677
        assert(v678.id == 197)
        matchSequence(v679)
    }
  }

  def matchAnyType(node: Node): AnyType = {
    val BindNode(v197, v198) = node
    v197.id match {
      case 108 =>
        AnyType()
    }
  }

  def matchBoolOrExpr(node: Node): BoolOrExpr = {
    val BindNode(v68, v69) = node
    v68.id match {
      case 327 =>
        val v70 = v69.asInstanceOf[SequenceNode].children.head
        val BindNode(v71, v72) = v70
        assert(v71.id == 328)
        val v73 = v69.asInstanceOf[SequenceNode].children(4)
        val BindNode(v74, v75) = v73
        assert(v74.id == 326)
        BinOp(Op.AND, matchBoolAndExpr(v72), matchBoolOrExpr(v75))
      case 429 =>
        val v76 = v69.asInstanceOf[SequenceNode].children.head
        val BindNode(v77, v78) = v76
        assert(v77.id == 328)
        matchBoolAndExpr(v78)
    }
  }

  def matchEOF(node: Node): String = {
    val BindNode(v680, v681) = node
    v680.id match {
      case 30 =>
        ""
    }
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v233, v234) = node
    v233.id match {
      case 120 =>
        val v235 = v234.asInstanceOf[SequenceNode].children.head
        val BindNode(v236, v237) = v235
        assert(v236.id == 100)
        val v238 = v234.asInstanceOf[SequenceNode].children(2)
        val BindNode(v239, v240) = v238
        assert(v239.id == 121)
        AbstractClassDef(matchTypeName(v237), matchSuperTypes(v240))
      case 136 =>
        val v241 = v234.asInstanceOf[SequenceNode].children.head
        val BindNode(v242, v243) = v241
        assert(v242.id == 100)
        val v244 = v234.asInstanceOf[SequenceNode].children(2)
        val BindNode(v245, v246) = v244
        assert(v245.id == 137)
        ConcreteClassDef(matchTypeName(v243), None, matchClassParamsDef(v246))
      case 153 =>
        val v247 = v234.asInstanceOf[SequenceNode].children.head
        val BindNode(v248, v249) = v247
        assert(v248.id == 100)
        val v250 = v234.asInstanceOf[SequenceNode].children(2)
        val BindNode(v251, v252) = v250
        assert(v251.id == 121)
        val v253 = v234.asInstanceOf[SequenceNode].children(4)
        val BindNode(v254, v255) = v253
        assert(v254.id == 137)
        ConcreteClassDef(matchTypeName(v249), Some(matchSuperTypes(v252)), matchClassParamsDef(v255))
    }
  }

  def matchKeyword(node: Node): KeyWord.Value = {
    val BindNode(v838, v839) = node
    v838.id match {
      case 65 =>
        KeyWord.CHAR
      case 71 =>
        KeyWord.STRING
      case 78 =>
        KeyWord.TRUE
      case 56 =>
        KeyWord.BOOLEAN
      case 86 =>
        KeyWord.NULL
      case 82 =>
        KeyWord.FALSE
    }
  }

  def matchStrChar(node: Node): StringChar = {
    val BindNode(v755, v756) = node
    v755.id match {
      case 390 =>
        val v757 = v756.asInstanceOf[SequenceNode].children.head
        val BindNode(v758, v759) = v757
        assert(v758.id == 260)
        matchStringChar(v759)
    }
  }

  def matchNamedConstructParams(node: Node): List[NamedParam] = {
    val BindNode(v256, v257) = node
    v256.id match {
      case 348 =>
        val v258 = v257.asInstanceOf[SequenceNode].children(2)
        val BindNode(v259, v260) = v258
        assert(v259.id == 349)
        val BindNode(v261, v262) = v260
        v261.id match {
          case 350 =>
            val BindNode(v263, v264) = v262
            assert(v263.id == 351)
            val v265 = v264.asInstanceOf[SequenceNode].children.head
            val BindNode(v266, v267) = v265
            assert(v266.id == 352)
            val v268 = v264.asInstanceOf[SequenceNode].children(1)
            val v269 = unrollRepeat0(v268).map { elem =>
              val BindNode(v270, v271) = elem
              assert(v270.id == 356)
              val BindNode(v272, v273) = v271
              v272.id match {
                case 357 =>
                  val BindNode(v274, v275) = v273
                  assert(v274.id == 358)
                  val v276 = v275.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v277, v278) = v276
                  assert(v277.id == 352)
                  matchNamedParam(v278)
              }
            }
            List(matchNamedParam(v267)) ++ v269
        }
    }
  }

  def matchSymbol(node: Node): Symbol = {
    val BindNode(v701, v702) = node
    v701.id match {
      case 202 =>
        val v703 = v702.asInstanceOf[SequenceNode].children.head
        val BindNode(v704, v705) = v703
        assert(v704.id == 203)
        matchBinSymbol(v705)
    }
  }

  def matchNonterminalName(node: Node): NonterminalName = {
    val BindNode(v760, v761) = node
    v760.id match {
      case 43 =>
        val v762 = v761.asInstanceOf[SequenceNode].children.head
        NonterminalName(v762.sourceText)
      case 89 =>
        val v763 = v761.asInstanceOf[SequenceNode].children(1)
        NonterminalName(v763.sourceText)
    }
  }

  def matchRef(node: Node): Ref = {
    val BindNode(v909, v910) = node
    v909.id match {
      case 289 =>
        val v911 = v910.asInstanceOf[SequenceNode].children.head
        val BindNode(v912, v913) = v911
        assert(v912.id == 290)
        matchValRef(v913)
      case 316 =>
        val v914 = v910.asInstanceOf[SequenceNode].children.head
        val BindNode(v915, v916) = v914
        assert(v915.id == 317)
        matchRawRef(v916)
    }
  }

  def matchTerminal(node: Node): Terminal = {
    val BindNode(v120, v121) = node
    v120.id match {
      case 222 =>
        val v122 = v121.asInstanceOf[SequenceNode].children(1)
        val BindNode(v123, v124) = v122
        assert(v123.id == 224)
        matchTerminalChar(v124)
      case 234 =>
        AnyTerminal()
    }
  }

  def matchRawRef(node: Node): RawRef = {
    val BindNode(v18, v19) = node
    v18.id match {
      case 318 =>
        val v20 = v19.asInstanceOf[SequenceNode].children(2)
        val BindNode(v21, v22) = v20
        assert(v21.id == 303)
        val v23 = v19.asInstanceOf[SequenceNode].children(1)
        val BindNode(v24, v25) = v23
        assert(v24.id == 293)
        val BindNode(v26, v27) = v25
        RawRef(matchRefIdx(v22), v26.id match {
          case 134 =>
            None
          case 294 =>
            Some(matchCondSymPath(v27))
        })
    }
  }

  def matchStringSymbol(node: Node): StringSymbol = {
    val BindNode(v693, v694) = node
    v693.id match {
      case 256 =>
        val v695 = v694.asInstanceOf[SequenceNode].children(1)
        val v696 = unrollRepeat0(v695).map { elem =>
          val BindNode(v697, v698) = elem
          assert(v697.id == 260)
          matchStringChar(v698)
        }
        StringSymbol(v696)
    }
  }

  def matchClassParamDef(node: Node): ClassParamDef = {
    val BindNode(v1, v2) = node
    v1.id match {
      case 145 =>
        val v3 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v4, v5) = v3
        assert(v4.id == 146)
        val v6 = v2.asInstanceOf[SequenceNode].children(1)
        val BindNode(v7, v8) = v6
        assert(v7.id == 91)
        val BindNode(v9, v10) = v8
        ClassParamDef(matchParamName(v5), v9.id match {
          case 134 =>
            None
          case 92 =>
            val BindNode(v11, v12) = v10
            Some(v11.id match {
              case 93 =>
                val BindNode(v13, v14) = v12
                assert(v13.id == 94)
                val v15 = v14.asInstanceOf[SequenceNode].children(3)
                val BindNode(v16, v17) = v15
                assert(v16.id == 96)
                matchTypeDesc(v17)
            })
        })
    }
  }

  def matchEnumValue(node: Node): AbstractEnumValue = {
    val BindNode(v625, v626) = node
    v625.id match {
      case 393 =>
        val v627 = v626.asInstanceOf[SequenceNode].children.head
        val BindNode(v628, v629) = v627
        assert(v628.id == 394)
        val BindNode(v630, v631) = v629
        assert(v630.id == 395)
        val BindNode(v632, v633) = v631
        v632.id match {
          case 396 =>
            val BindNode(v634, v635) = v633
            assert(v634.id == 397)
            val v636 = v635.asInstanceOf[SequenceNode].children.head
            val BindNode(v637, v638) = v636
            assert(v637.id == 398)
            matchCanonicalEnumValue(v638)
          case 402 =>
            val BindNode(v639, v640) = v633
            assert(v639.id == 403)
            val v641 = v640.asInstanceOf[SequenceNode].children.head
            val BindNode(v642, v643) = v641
            assert(v642.id == 404)
            matchShortenedEnumValue(v643)
        }
    }
  }

  def matchEnumTypeName(node: Node): EnumTypeName = {
    val BindNode(v620, v621) = node
    v620.id match {
      case 114 =>
        val v622 = v621.asInstanceOf[SequenceNode].children(1)
        val BindNode(v623, v624) = v622
        assert(v623.id == 45)
        EnumTypeName(matchId(v624))
    }
  }

  def matchSuperDef(node: Node): SuperDef = {
    val BindNode(v473, v474) = node
    v473.id match {
      case 156 =>
        val v475 = v474.asInstanceOf[SequenceNode].children.head
        val BindNode(v476, v477) = v475
        assert(v476.id == 100)
        val v478 = v474.asInstanceOf[SequenceNode].children(4)
        val BindNode(v479, v480) = v478
        assert(v479.id == 162)
        val BindNode(v481, v482) = v480
        val v490 = v474.asInstanceOf[SequenceNode].children(1)
        val BindNode(v491, v492) = v490
        assert(v491.id == 157)
        val BindNode(v493, v494) = v492
        SuperDef(matchTypeName(v477), v481.id match {
          case 134 =>
            None
          case 163 =>
            val BindNode(v483, v484) = v482
            Some(v483.id match {
              case 164 =>
                val BindNode(v485, v486) = v484
                assert(v485.id == 165)
                val v487 = v486.asInstanceOf[SequenceNode].children(1)
                val BindNode(v488, v489) = v487
                assert(v488.id == 166)
                matchSubTypes(v489)
            })
        }, v493.id match {
          case 134 =>
            None
          case 158 =>
            val BindNode(v495, v496) = v494
            Some(v495.id match {
              case 159 =>
                val BindNode(v497, v498) = v496
                assert(v497.id == 160)
                val v499 = v498.asInstanceOf[SequenceNode].children(1)
                val BindNode(v500, v501) = v499
                assert(v500.id == 121)
                matchSuperTypes(v501)
            })
        })
    }
  }

  def matchParamName(node: Node): ParamName = {
    val BindNode(v840, v841) = node
    v840.id match {
      case 43 =>
        val v842 = v841.asInstanceOf[SequenceNode].children.head
        ParamName(v842.sourceText)
      case 89 =>
        val v843 = v841.asInstanceOf[SequenceNode].children(1)
        ParamName(v843.sourceText)
    }
  }

  def matchId(node: Node): String = {
    val BindNode(v118, v119) = node
    v118.id match {
      case 46 =>
        ""
    }
  }

  def matchElvisExpr(node: Node): ElvisExpr = {
    val BindNode(v942, v943) = node
    v942.id match {
      case 333 =>
        val v944 = v943.asInstanceOf[SequenceNode].children.head
        val BindNode(v945, v946) = v944
        assert(v945.id == 334)
        val v947 = v943.asInstanceOf[SequenceNode].children(4)
        val BindNode(v948, v949) = v947
        assert(v948.id == 332)
        Elvis(matchAdditiveExpr(v946), matchElvisExpr(v949))
      case 413 =>
        val v950 = v943.asInstanceOf[SequenceNode].children.head
        val BindNode(v951, v952) = v950
        assert(v951.id == 334)
        matchAdditiveExpr(v952)
    }
  }

  def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
    val BindNode(v834, v835) = node
    v834.id match {
      case 43 =>
        val v836 = v835.asInstanceOf[SequenceNode].children.head
        TypeOrFuncName(v836.sourceText)
      case 89 =>
        val v837 = v835.asInstanceOf[SequenceNode].children(1)
        TypeOrFuncName(v837.sourceText)
    }
  }

  def matchEmptySequence(node: Node): EmptySeq = {
    val BindNode(v108, v109) = node
    v108.id match {
      case 281 =>
        EmptySeq()
    }
  }

  def matchBoolEqExpr(node: Node): BoolEqExpr = {
    val BindNode(v917, v918) = node
    v917.id match {
      case 331 =>
        val v919 = v918.asInstanceOf[SequenceNode].children(2)
        val BindNode(v920, v921) = v919
        assert(v920.id == 414)
        val BindNode(v922, v923) = v921
        val v928 = v918.asInstanceOf[SequenceNode].children.head
        val BindNode(v929, v930) = v928
        assert(v929.id == 332)
        val v931 = v918.asInstanceOf[SequenceNode].children(4)
        val BindNode(v932, v933) = v931
        assert(v932.id == 330)
        BinOp(v922.id match {
          case 415 =>
            val BindNode(v924, v925) = v923
            assert(v924.id == 416)
            Op.EQ
          case 419 =>
            val BindNode(v926, v927) = v923
            assert(v926.id == 420)
            Op.NE
        }, matchElvisExpr(v930), matchBoolEqExpr(v933))
      case 423 =>
        val v934 = v918.asInstanceOf[SequenceNode].children.head
        val BindNode(v935, v936) = v934
        assert(v935.id == 332)
        matchElvisExpr(v936)
    }
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v379, v380) = node
    v379.id match {
      case 86 =>
        NullLiteral()
      case 379 =>
        val v381 = v380.asInstanceOf[SequenceNode].children.head
        val BindNode(v382, v383) = v381
        assert(v382.id == 380)
        val BindNode(v384, v385) = v383
        BoolLiteral(v384.id match {
          case 381 =>
            val BindNode(v386, v387) = v385
            assert(v386.id == 78)
            true
          case 382 =>
            val BindNode(v388, v389) = v385
            assert(v388.id == 82)
            false
        })
      case 383 =>
        val v390 = v380.asInstanceOf[SequenceNode].children(1)
        val BindNode(v391, v392) = v390
        assert(v391.id == 384)
        CharLiteral(matchCharChar(v392))
      case 386 =>
        val v393 = v380.asInstanceOf[SequenceNode].children(1)
        val v394 = unrollRepeat0(v393).map { elem =>
          val BindNode(v395, v396) = elem
          assert(v395.id == 389)
          matchStrChar(v396)
        }
        StrLiteral(v394)
    }
  }

  def matchPExpr(node: Node): PExpr = {
    val BindNode(v291, v292) = node
    v291.id match {
      case 323 =>
        val v293 = v292.asInstanceOf[SequenceNode].children.head
        val BindNode(v294, v295) = v293
        assert(v294.id == 324)
        val v296 = v292.asInstanceOf[SequenceNode].children(1)
        val BindNode(v297, v298) = v296
        assert(v297.id == 91)
        val BindNode(v299, v300) = v298
        PExpr(matchTernaryExpr(v295), v299.id match {
          case 134 =>
            None
          case 92 =>
            val BindNode(v301, v302) = v300
            Some(v301.id match {
              case 93 =>
                val BindNode(v303, v304) = v302
                assert(v303.id == 94)
                val v305 = v304.asInstanceOf[SequenceNode].children(3)
                val BindNode(v306, v307) = v305
                assert(v306.id == 96)
                matchTypeDesc(v307)
            })
        })
    }
  }

  def matchLHS(node: Node): LHS = {
    val BindNode(v397, v398) = node
    v397.id match {
      case 39 =>
        val v399 = v398.asInstanceOf[SequenceNode].children.head
        val BindNode(v400, v401) = v399
        assert(v400.id == 40)
        val v402 = v398.asInstanceOf[SequenceNode].children(1)
        val BindNode(v403, v404) = v402
        assert(v403.id == 91)
        val BindNode(v405, v406) = v404
        LHS(matchNonterminal(v401), v405.id match {
          case 134 =>
            None
          case 92 =>
            val BindNode(v407, v408) = v406
            Some(v407.id match {
              case 93 =>
                val BindNode(v409, v410) = v408
                assert(v409.id == 94)
                val v411 = v410.asInstanceOf[SequenceNode].children(3)
                val BindNode(v412, v413) = v411
                assert(v412.id == 96)
                matchTypeDesc(v413)
            })
        })
    }
  }

  def matchValRef(node: Node): ValRef = {
    val BindNode(v353, v354) = node
    v353.id match {
      case 291 =>
        val v355 = v354.asInstanceOf[SequenceNode].children(2)
        val BindNode(v356, v357) = v355
        assert(v356.id == 303)
        val v358 = v354.asInstanceOf[SequenceNode].children(1)
        val BindNode(v359, v360) = v358
        assert(v359.id == 293)
        val BindNode(v361, v362) = v360
        ValRef(matchRefIdx(v357), v361.id match {
          case 134 =>
            None
          case 294 =>
            Some(matchCondSymPath(v362))
        })
    }
  }

  def matchAtom(node: Node): Atom = {
    val BindNode(v125, v126) = node
    v125.id match {
      case 340 =>
        val v127 = v126.asInstanceOf[SequenceNode].children.head
        val BindNode(v128, v129) = v127
        assert(v128.id == 341)
        matchBindExpr(v129)
      case 344 =>
        val v130 = v126.asInstanceOf[SequenceNode].children.head
        val BindNode(v131, v132) = v130
        assert(v131.id == 345)
        matchNamedConstructExpr(v132)
      case 287 =>
        val v133 = v126.asInstanceOf[SequenceNode].children.head
        val BindNode(v134, v135) = v133
        assert(v134.id == 288)
        matchRef(v135)
      case 374 =>
        val v136 = v126.asInstanceOf[SequenceNode].children.head
        val BindNode(v137, v138) = v136
        assert(v137.id == 375)
        matchArrayExpr(v138)
      case 377 =>
        val v139 = v126.asInstanceOf[SequenceNode].children.head
        val BindNode(v140, v141) = v139
        assert(v140.id == 378)
        matchLiteral(v141)
      case 391 =>
        val v142 = v126.asInstanceOf[SequenceNode].children.head
        val BindNode(v143, v144) = v142
        assert(v143.id == 392)
        matchEnumValue(v144)
      case 406 =>
        val v145 = v126.asInstanceOf[SequenceNode].children(2)
        val BindNode(v146, v147) = v145
        assert(v146.id == 322)
        ExprParen(matchPExpr(v147))
      case 359 =>
        val v148 = v126.asInstanceOf[SequenceNode].children.head
        val BindNode(v149, v150) = v148
        assert(v149.id == 360)
        matchFuncCallOrConstructExpr(v150)
    }
  }

  def matchTypeName(node: Node): TypeName = {
    val BindNode(v207, v208) = node
    v207.id match {
      case 43 =>
        val v209 = v208.asInstanceOf[SequenceNode].children.head
        TypeName(v209.sourceText)
      case 89 =>
        val v210 = v208.asInstanceOf[SequenceNode].children(1)
        TypeName(v210.sourceText)
    }
  }

  def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
    val BindNode(v433, v434) = node
    v433.id match {
      case 361 =>
        val v435 = v434.asInstanceOf[SequenceNode].children.head
        val BindNode(v436, v437) = v435
        assert(v436.id == 362)
        val v438 = v434.asInstanceOf[SequenceNode].children(2)
        val BindNode(v439, v440) = v438
        assert(v439.id == 363)
        FuncCallOrConstructExpr(matchTypeOrFuncName(v437), matchCallParams(v440))
    }
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v789, v790) = node
    v789.id match {
      case 353 =>
        val v791 = v790.asInstanceOf[SequenceNode].children.head
        val BindNode(v792, v793) = v791
        assert(v792.id == 146)
        val v794 = v790.asInstanceOf[SequenceNode].children(1)
        val BindNode(v795, v796) = v794
        assert(v795.id == 91)
        val BindNode(v797, v798) = v796
        val v806 = v790.asInstanceOf[SequenceNode].children(5)
        val BindNode(v807, v808) = v806
        assert(v807.id == 322)
        NamedParam(matchParamName(v793), v797.id match {
          case 134 =>
            None
          case 92 =>
            val BindNode(v799, v800) = v798
            Some(v799.id match {
              case 93 =>
                val BindNode(v801, v802) = v800
                assert(v801.id == 94)
                val v803 = v802.asInstanceOf[SequenceNode].children(3)
                val BindNode(v804, v805) = v803
                assert(v804.id == 96)
                matchTypeDesc(v805)
            })
        }, matchPExpr(v808))
    }
  }

  def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
    val BindNode(v609, v610) = node
    v609.id match {
      case 242 =>
        val v611 = v610.asInstanceOf[SequenceNode].children.head
        val BindNode(v612, v613) = v611
        assert(v612.id == 243)
        CharAsIs(v613.toString.charAt(0))
      case 245 =>
        val v614 = v610.asInstanceOf[SequenceNode].children(1)
        val BindNode(v615, v616) = v614
        assert(v615.id == 246)
        CharEscaped(v616.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0))
      case 230 =>
        val v617 = v610.asInstanceOf[SequenceNode].children.head
        val BindNode(v618, v619) = v617
        assert(v618.id == 231)
        matchUnicodeChar(v619)
    }
  }

  def matchRefIdx(node: Node): String = {
    val BindNode(v555, v556) = node
    v555.id match {
      case 304 =>
        val v557 = v556.asInstanceOf[SequenceNode].children.head
        val BindNode(v558, v559) = v557
        assert(v558.id == 305)
        val BindNode(v560, v561) = v559
        assert(v560.id == 306)
        val BindNode(v562, v563) = v561
        v562.id match {
          case 307 =>
            val BindNode(v564, v565) = v563
            assert(v564.id == 308)
            val v566 = v565.asInstanceOf[SequenceNode].children.head
            val BindNode(v567, v568) = v566
            assert(v567.id == 309)
            v568.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString
          case 310 =>
            val BindNode(v569, v570) = v563
            assert(v569.id == 311)
            val v571 = v570.asInstanceOf[SequenceNode].children(1)
            val v572 = unrollRepeat0(v571).map { elem =>
              val BindNode(v573, v574) = elem
              assert(v573.id == 315)
              v574.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            new String(v572.toArray)
        }
    }
  }

  def matchSuperTypes(node: Node): List[TypeName] = {
    val BindNode(v42, v43) = node
    v42.id match {
      case 122 =>
        val v45 = v43.asInstanceOf[SequenceNode].children(2)
        val BindNode(v46, v47) = v45
        assert(v46.id == 124)
        val BindNode(v48, v49) = v47
        val v44 = v48.id match {
          case 134 =>
            None
          case 125 =>
            val BindNode(v50, v51) = v49
            Some(v50.id match {
              case 126 =>
                val BindNode(v52, v53) = v51
                assert(v52.id == 127)
                val v54 = v53.asInstanceOf[SequenceNode].children.head
                val BindNode(v55, v56) = v54
                assert(v55.id == 100)
                val v57 = v53.asInstanceOf[SequenceNode].children(1)
                val v58 = unrollRepeat0(v57).map { elem =>
                  val BindNode(v59, v60) = elem
                  assert(v59.id == 130)
                  val BindNode(v61, v62) = v60
                  v61.id match {
                    case 131 =>
                      val BindNode(v63, v64) = v62
                      assert(v63.id == 132)
                      val v65 = v64.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v66, v67) = v65
                      assert(v66.id == 100)
                      matchTypeName(v67)
                  }
                }
                List(matchTypeName(v56)) ++ v58
            })
        }
        if (v44.isDefined) v44.get else List()
    }
  }

  def matchNamedConstructExpr(node: Node): NamedConstructExpr = {
    val BindNode(v213, v214) = node
    v213.id match {
      case 346 =>
        val v215 = v214.asInstanceOf[SequenceNode].children.head
        val BindNode(v216, v217) = v215
        assert(v216.id == 100)
        val v218 = v214.asInstanceOf[SequenceNode].children(3)
        val BindNode(v219, v220) = v218
        assert(v219.id == 347)
        val v221 = v214.asInstanceOf[SequenceNode].children(1)
        val BindNode(v222, v223) = v221
        assert(v222.id == 157)
        val BindNode(v224, v225) = v223
        NamedConstructExpr(matchTypeName(v217), matchNamedConstructParams(v220), v224.id match {
          case 134 =>
            None
          case 158 =>
            val BindNode(v226, v227) = v225
            Some(v226.id match {
              case 159 =>
                val BindNode(v228, v229) = v227
                assert(v228.id == 160)
                val v230 = v229.asInstanceOf[SequenceNode].children(1)
                val BindNode(v231, v232) = v230
                assert(v231.id == 121)
                matchSuperTypes(v232)
            })
        })
    }
  }

  def matchCanonicalEnumValue(node: Node): CanonicalEnumValue = {
    val BindNode(v110, v111) = node
    v110.id match {
      case 399 =>
        val v112 = v111.asInstanceOf[SequenceNode].children.head
        val BindNode(v113, v114) = v112
        assert(v113.id == 113)
        val v115 = v111.asInstanceOf[SequenceNode].children(2)
        val BindNode(v116, v117) = v115
        assert(v116.id == 400)
        CanonicalEnumValue(matchEnumTypeName(v114), matchEnumValueName(v117))
    }
  }

  def matchSubTypes(node: Node): List[SubType] = {
    val BindNode(v539, v540) = node
    v539.id match {
      case 167 =>
        val v541 = v540.asInstanceOf[SequenceNode].children.head
        val BindNode(v542, v543) = v541
        assert(v542.id == 168)
        val v544 = v540.asInstanceOf[SequenceNode].children(1)
        val v545 = unrollRepeat0(v544).map { elem =>
          val BindNode(v546, v547) = elem
          assert(v546.id == 171)
          val BindNode(v548, v549) = v547
          v548.id match {
            case 172 =>
              val BindNode(v550, v551) = v549
              assert(v550.id == 173)
              val v552 = v551.asInstanceOf[SequenceNode].children(3)
              val BindNode(v553, v554) = v552
              assert(v553.id == 168)
              matchSubType(v554)
          }
        }
        List(matchSubType(v543)) ++ v545
    }
  }

  def matchArrayExpr(node: Node): ArrayExpr = {
    val BindNode(v308, v309) = node
    v308.id match {
      case 376 =>
        val v311 = v309.asInstanceOf[SequenceNode].children(2)
        val BindNode(v312, v313) = v311
        assert(v312.id == 365)
        val BindNode(v314, v315) = v313
        val v310 = v314.id match {
          case 134 =>
            None
          case 366 =>
            val BindNode(v316, v317) = v315
            assert(v316.id == 367)
            val BindNode(v318, v319) = v317
            assert(v318.id == 368)
            val v320 = v319.asInstanceOf[SequenceNode].children.head
            val BindNode(v321, v322) = v320
            assert(v321.id == 322)
            val v323 = v319.asInstanceOf[SequenceNode].children(1)
            val v324 = unrollRepeat0(v323).map { elem =>
              val BindNode(v325, v326) = elem
              assert(v325.id == 371)
              val BindNode(v327, v328) = v326
              v327.id match {
                case 372 =>
                  val BindNode(v329, v330) = v328
                  assert(v329.id == 373)
                  val v331 = v330.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v332, v333) = v331
                  assert(v332.id == 322)
                  matchPExpr(v333)
              }
            }
            Some(List(matchPExpr(v322)) ++ v324)
        }
        ArrayExpr(if (v310.isDefined) v310.get else List())
    }
  }

  def matchClassParamsDef(node: Node): List[ClassParamDef] = {
    val BindNode(v575, v576) = node
    v575.id match {
      case 138 =>
        val v578 = v576.asInstanceOf[SequenceNode].children(2)
        val BindNode(v579, v580) = v578
        assert(v579.id == 140)
        val BindNode(v581, v582) = v580
        val v577 = v581.id match {
          case 134 =>
            None
          case 141 =>
            val BindNode(v583, v584) = v582
            Some(v583.id match {
              case 142 =>
                val BindNode(v585, v586) = v584
                assert(v585.id == 143)
                val v587 = v586.asInstanceOf[SequenceNode].children.head
                val BindNode(v588, v589) = v587
                assert(v588.id == 144)
                val v590 = v586.asInstanceOf[SequenceNode].children(1)
                val v591 = unrollRepeat0(v590).map { elem =>
                  val BindNode(v592, v593) = elem
                  assert(v592.id == 149)
                  val BindNode(v594, v595) = v593
                  v594.id match {
                    case 150 =>
                      val BindNode(v596, v597) = v595
                      assert(v596.id == 151)
                      val v598 = v597.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v599, v600) = v598
                      assert(v599.id == 144)
                      matchClassParamDef(v600)
                  }
                }
                List(matchClassParamDef(v589)) ++ v591
            })
        }
        if (v577.isDefined) v577.get else List()
    }
  }

  def matchPostUnSymbol(node: Node): PostUnSymbol = {
    val BindNode(v28, v29) = node
    v28.id match {
      case 213 =>
        val v30 = v29.asInstanceOf[SequenceNode].children.head
        val BindNode(v31, v32) = v30
        assert(v31.id == 212)
        Optional(matchPostUnSymbol(v32))
      case 214 =>
        val v33 = v29.asInstanceOf[SequenceNode].children.head
        val BindNode(v34, v35) = v33
        assert(v34.id == 212)
        RepeatFromZero(matchPostUnSymbol(v35))
      case 216 =>
        val v36 = v29.asInstanceOf[SequenceNode].children.head
        val BindNode(v37, v38) = v36
        assert(v37.id == 212)
        RepeatFromOne(matchPostUnSymbol(v38))
      case 218 =>
        val v39 = v29.asInstanceOf[SequenceNode].children.head
        val BindNode(v40, v41) = v39
        assert(v40.id == 219)
        matchAtomSymbol(v41)
    }
  }

  def matchCondSymPath(node: Node): List[CondSymDir.Value] = {
    val BindNode(v279, v280) = node
    v279.id match {
      case 295 =>
        val v281 = v280.asInstanceOf[SequenceNode].children.head
        val v282 = unrollRepeat1(v281).map { elem =>
          val BindNode(v283, v284) = elem
          assert(v283.id == 297)
          val BindNode(v285, v286) = v284
          v285.id match {
            case 298 =>
              val BindNode(v287, v288) = v286
              assert(v287.id == 299)
              CondSymDir.BODY
            case 300 =>
              val BindNode(v289, v290) = v286
              assert(v289.id == 301)
              CondSymDir.COND
          }
        }
        v282
    }
  }

  def matchBinSymbol(node: Node): BinSymbol = {
    val BindNode(v855, v856) = node
    v855.id match {
      case 204 =>
        val v857 = v856.asInstanceOf[SequenceNode].children.head
        val BindNode(v858, v859) = v857
        assert(v858.id == 203)
        val v860 = v856.asInstanceOf[SequenceNode].children(4)
        val BindNode(v861, v862) = v860
        assert(v861.id == 206)
        JoinSymbol(matchBinSymbol(v859), matchPreUnSymbol(v862))(node)
      case 283 =>
        val v863 = v856.asInstanceOf[SequenceNode].children.head
        val BindNode(v864, v865) = v863
        assert(v864.id == 203)
        val v866 = v856.asInstanceOf[SequenceNode].children(4)
        val BindNode(v867, v868) = v866
        assert(v867.id == 206)
        ExceptSymbol(matchBinSymbol(v865), matchPreUnSymbol(v868))
      case 284 =>
        val v869 = v856.asInstanceOf[SequenceNode].children.head
        val BindNode(v870, v871) = v869
        assert(v870.id == 206)
        matchPreUnSymbol(v871)
    }
  }

  def matchTypeDesc(node: Node): TypeDesc = {
    val BindNode(v706, v707) = node
    v706.id match {
      case 97 =>
        val v708 = v707.asInstanceOf[SequenceNode].children.head
        val BindNode(v709, v710) = v708
        assert(v709.id == 98)
        val v711 = v707.asInstanceOf[SequenceNode].children(1)
        val BindNode(v712, v713) = v711
        assert(v712.id == 186)
        val BindNode(v714, v715) = v713
        TypeDesc(matchNonNullTypeDesc(v710), (v714.id match {
          case 134 =>
            None
          case 187 =>
            val BindNode(v716, v717) = v715
            Some(v716.id match {
              case 188 =>
                val BindNode(v718, v719) = v717
                assert(v718.id == 189)
                val v720 = v719.asInstanceOf[SequenceNode].children(1)
                val BindNode(v721, v722) = v720
                assert(v721.id == 190)
                v722.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            })
        }).isDefined)
    }
  }

  def matchWSNL(node: Node): String = {
    val BindNode(v363, v364) = node
    v363.id match {
      case 451 =>
        ""
    }
  }

  def matchTypeDef(node: Node): TypeDef = {
    val BindNode(v528, v529) = node
    v528.id match {
      case 118 =>
        val v530 = v529.asInstanceOf[SequenceNode].children.head
        val BindNode(v531, v532) = v530
        assert(v531.id == 119)
        matchClassDef(v532)
      case 154 =>
        val v533 = v529.asInstanceOf[SequenceNode].children.head
        val BindNode(v534, v535) = v533
        assert(v534.id == 155)
        matchSuperDef(v535)
      case 175 =>
        val v536 = v529.asInstanceOf[SequenceNode].children.head
        val BindNode(v537, v538) = v536
        assert(v537.id == 176)
        matchEnumTypeDef(v538)
    }
  }

  def matchProcessor(node: Node): Processor = {
    val BindNode(v199, v200) = node
    v199.id match {
      case 287 =>
        val v201 = v200.asInstanceOf[SequenceNode].children.head
        val BindNode(v202, v203) = v201
        assert(v202.id == 288)
        matchRef(v203)
      case 321 =>
        val v204 = v200.asInstanceOf[SequenceNode].children(2)
        val BindNode(v205, v206) = v204
        assert(v205.id == 322)
        matchPExpr(v206)
    }
  }

  def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
    val BindNode(v764, v765) = node
    v764.id match {
      case 104 =>
        val v766 = v765.asInstanceOf[SequenceNode].children.head
        val BindNode(v767, v768) = v766
        assert(v767.id == 105)
        matchValueType(v768)
      case 116 =>
        val v769 = v765.asInstanceOf[SequenceNode].children.head
        val BindNode(v770, v771) = v769
        assert(v770.id == 117)
        matchTypeDef(v771)
      case 106 =>
        val v772 = v765.asInstanceOf[SequenceNode].children.head
        val BindNode(v773, v774) = v772
        assert(v773.id == 107)
        matchAnyType(v774)
      case 99 =>
        val v775 = v765.asInstanceOf[SequenceNode].children.head
        val BindNode(v776, v777) = v775
        assert(v776.id == 100)
        matchTypeName(v777)
      case 101 =>
        val v778 = v765.asInstanceOf[SequenceNode].children(2)
        val BindNode(v779, v780) = v778
        assert(v779.id == 96)
        ArrayTypeDesc(matchTypeDesc(v780))
      case 112 =>
        val v781 = v765.asInstanceOf[SequenceNode].children.head
        val BindNode(v782, v783) = v781
        assert(v782.id == 113)
        matchEnumTypeName(v783)
    }
  }

  def matchValueType(node: Node): ValueType = {
    val BindNode(v211, v212) = node
    v211.id match {
      case 56 =>
        BooleanType()
      case 65 =>
        CharType()
      case 71 =>
        StringType()
    }
  }

  def matchElem(node: Node): Elem = {
    val BindNode(v644, v645) = node
    v644.id match {
      case 200 =>
        val v646 = v645.asInstanceOf[SequenceNode].children.head
        val BindNode(v647, v648) = v646
        assert(v647.id == 201)
        matchSymbol(v648)
      case 285 =>
        val v649 = v645.asInstanceOf[SequenceNode].children.head
        val BindNode(v650, v651) = v649
        assert(v650.id == 286)
        matchProcessor(v651)
    }
  }

  def matchBindExpr(node: Node): BindExpr = {
    val BindNode(v601, v602) = node
    v601.id match {
      case 342 =>
        val v603 = v602.asInstanceOf[SequenceNode].children.head
        val BindNode(v604, v605) = v603
        assert(v604.id == 290)
        val v606 = v602.asInstanceOf[SequenceNode].children(1)
        val BindNode(v607, v608) = v606
        assert(v607.id == 343)
        BindExpr(matchValRef(v605), matchBinderExpr(v608))
    }
  }

  def matchCharChar(node: Node): TerminalChar = {
    val BindNode(v151, v152) = node
    v151.id match {
      case 385 =>
        val v153 = v152.asInstanceOf[SequenceNode].children.head
        val BindNode(v154, v155) = v153
        assert(v154.id == 224)
        matchTerminalChar(v155)
    }
  }

  def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
    val BindNode(v425, v426) = node
    v425.id match {
      case 337 =>
        val v427 = v426.asInstanceOf[SequenceNode].children(2)
        val BindNode(v428, v429) = v427
        assert(v428.id == 336)
        PrefixOp(PreOp.NOT, matchPrefixNotExpr(v429))
      case 338 =>
        val v430 = v426.asInstanceOf[SequenceNode].children.head
        val BindNode(v431, v432) = v430
        assert(v431.id == 339)
        matchAtom(v432)
    }
  }

  def matchAtomSymbol(node: Node): AtomSymbol = {
    val BindNode(v652, v653) = node
    v652.id match {
      case 267 =>
        val v654 = v653.asInstanceOf[SequenceNode].children(2)
        val BindNode(v655, v656) = v654
        assert(v655.id == 268)
        matchInPlaceChoices(v656)
      case 236 =>
        val v657 = v653.asInstanceOf[SequenceNode].children.head
        val BindNode(v658, v659) = v657
        assert(v658.id == 237)
        matchTerminalChoice(v659)
      case 279 =>
        val v660 = v653.asInstanceOf[SequenceNode].children.head
        val BindNode(v661, v662) = v660
        assert(v661.id == 280)
        matchEmptySequence(v662)
      case 220 =>
        val v663 = v653.asInstanceOf[SequenceNode].children.head
        val BindNode(v664, v665) = v663
        assert(v664.id == 221)
        matchTerminal(v665)
      case 254 =>
        val v666 = v653.asInstanceOf[SequenceNode].children.head
        val BindNode(v667, v668) = v666
        assert(v667.id == 255)
        matchStringSymbol(v668)
      case 266 =>
        val v669 = v653.asInstanceOf[SequenceNode].children.head
        val BindNode(v670, v671) = v669
        assert(v670.id == 40)
        matchNonterminal(v671)
      case 276 =>
        val v672 = v653.asInstanceOf[SequenceNode].children.head
        val BindNode(v673, v674) = v672
        assert(v673.id == 277)
        matchLongest(v674)
    }
  }

  def matchCallParams(node: Node): List[PExpr] = {
    val BindNode(v872, v873) = node
    v872.id match {
      case 364 =>
        val v875 = v873.asInstanceOf[SequenceNode].children(2)
        val BindNode(v876, v877) = v875
        assert(v876.id == 365)
        val BindNode(v878, v879) = v877
        val v874 = v878.id match {
          case 134 =>
            None
          case 366 =>
            val BindNode(v880, v881) = v879
            Some(v880.id match {
              case 367 =>
                val BindNode(v882, v883) = v881
                assert(v882.id == 368)
                val v884 = v883.asInstanceOf[SequenceNode].children.head
                val BindNode(v885, v886) = v884
                assert(v885.id == 322)
                val v887 = v883.asInstanceOf[SequenceNode].children(1)
                val v888 = unrollRepeat0(v887).map { elem =>
                  val BindNode(v889, v890) = elem
                  assert(v889.id == 371)
                  val BindNode(v891, v892) = v890
                  v891.id match {
                    case 372 =>
                      val BindNode(v893, v894) = v892
                      assert(v893.id == 373)
                      val v895 = v894.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v896, v897) = v895
                      assert(v896.id == 322)
                      matchPExpr(v897)
                  }
                }
                List(matchPExpr(v886)) ++ v888
            })
        }
        if (v874.isDefined) v874.get else List()
    }
  }

  def matchTernaryExpr(node: Node): TernaryExpr = {
    val BindNode(v723, v724) = node
    v723.id match {
      case 325 =>
        val v725 = v724.asInstanceOf[SequenceNode].children.head
        val BindNode(v726, v727) = v725
        assert(v726.id == 326)
        val v728 = v724.asInstanceOf[SequenceNode].children(4)
        val BindNode(v729, v730) = v728
        assert(v729.id == 430)
        val BindNode(v731, v732) = v730
        assert(v731.id == 431)
        val BindNode(v733, v734) = v732
        val v740 = v724.asInstanceOf[SequenceNode].children(8)
        val BindNode(v741, v742) = v740
        assert(v741.id == 430)
        val BindNode(v743, v744) = v742
        assert(v743.id == 431)
        val BindNode(v745, v746) = v744
        TernaryOp(matchBoolOrExpr(v727), v733.id match {
          case 432 =>
            val BindNode(v735, v736) = v734
            assert(v735.id == 433)
            val v737 = v736.asInstanceOf[SequenceNode].children.head
            val BindNode(v738, v739) = v737
            assert(v738.id == 324)
            matchTernaryExpr(v739)
        }, v745.id match {
          case 432 =>
            val BindNode(v747, v748) = v746
            assert(v747.id == 433)
            val v749 = v748.asInstanceOf[SequenceNode].children.head
            val BindNode(v750, v751) = v749
            assert(v750.id == 324)
            matchTernaryExpr(v751)
        })
      case 434 =>
        val v752 = v724.asInstanceOf[SequenceNode].children.head
        val BindNode(v753, v754) = v752
        assert(v753.id == 326)
        matchBoolOrExpr(v754)
    }
  }

  def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
    val BindNode(v829, v830) = node
    v829.id match {
      case 405 =>
        val v831 = v830.asInstanceOf[SequenceNode].children(1)
        val BindNode(v832, v833) = v831
        assert(v832.id == 400)
        ShortenedEnumValue(matchEnumValueName(v833))
    }
  }

  def matchUnicodeChar(node: Node): CharUnicode = {
    val BindNode(v365, v366) = node
    v365.id match {
      case 232 =>
        val v367 = v366.asInstanceOf[SequenceNode].children(2)
        val BindNode(v368, v369) = v367
        assert(v368.id == 233)
        val v370 = v366.asInstanceOf[SequenceNode].children(3)
        val BindNode(v371, v372) = v370
        assert(v371.id == 233)
        val v373 = v366.asInstanceOf[SequenceNode].children(4)
        val BindNode(v374, v375) = v373
        assert(v374.id == 233)
        val v376 = v366.asInstanceOf[SequenceNode].children(5)
        val BindNode(v377, v378) = v376
        assert(v377.id == 233)
        CharUnicode(List(v369.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0), v372.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0), v375.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0), v378.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0)))
    }
  }

  def matchPreUnSymbol(node: Node): PreUnSymbol = {
    val BindNode(v844, v845) = node
    v844.id match {
      case 207 =>
        val v846 = v845.asInstanceOf[SequenceNode].children(2)
        val BindNode(v847, v848) = v846
        assert(v847.id == 206)
        FollowedBy(matchPreUnSymbol(v848))
      case 209 =>
        val v849 = v845.asInstanceOf[SequenceNode].children(2)
        val BindNode(v850, v851) = v849
        assert(v850.id == 206)
        NotFollowedBy(matchPreUnSymbol(v851))
      case 211 =>
        val v852 = v845.asInstanceOf[SequenceNode].children.head
        val BindNode(v853, v854) = v852
        assert(v853.id == 212)
        matchPostUnSymbol(v854)
    }
  }

  def matchEnumValueName(node: Node): EnumValueName = {
    val BindNode(v937, v938) = node
    v937.id match {
      case 401 =>
        val v939 = v938.asInstanceOf[SequenceNode].children.head
        val BindNode(v940, v941) = v939
        assert(v940.id == 45)
        EnumValueName(matchId(v941))
    }
  }

  def matchTerminalChoice(node: Node): TerminalChoice = {
    val BindNode(v817, v818) = node
    v817.id match {
      case 238 =>
        val v819 = v818.asInstanceOf[SequenceNode].children(1)
        val BindNode(v820, v821) = v819
        assert(v820.id == 239)
        val v822 = v818.asInstanceOf[SequenceNode].children(2)
        val v823 = unrollRepeat1(v822).map { elem =>
          val BindNode(v824, v825) = elem
          assert(v824.id == 239)
          matchTerminalChoiceElem(v825)
        }
        TerminalChoice(List(matchTerminalChoiceElem(v821)) ++ v823)
      case 253 =>
        val v826 = v818.asInstanceOf[SequenceNode].children(1)
        val BindNode(v827, v828) = v826
        assert(v827.id == 248)
        TerminalChoice(List(matchTerminalChoiceRange(v828)))
    }
  }

  def matchWS(node: Node): String = {
    val BindNode(v699, v700) = node
    v699.id match {
      case 5 =>
        ""
    }
  }

  def matchTerminalChar(node: Node): TerminalChar = {
    val BindNode(v898, v899) = node
    v898.id match {
      case 225 =>
        val v900 = v899.asInstanceOf[SequenceNode].children.head
        val BindNode(v901, v902) = v900
        assert(v901.id == 226)
        CharAsIs(v902.toString.charAt(0))
      case 228 =>
        val v903 = v899.asInstanceOf[SequenceNode].children(1)
        val BindNode(v904, v905) = v903
        assert(v904.id == 229)
        CharEscaped(v905.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0))
      case 230 =>
        val v906 = v899.asInstanceOf[SequenceNode].children.head
        val BindNode(v907, v908) = v906
        assert(v907.id == 231)
        matchUnicodeChar(v908)
    }
  }

  def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
    val BindNode(v156, v157) = node
    v156.id match {
      case 240 =>
        val v158 = v157.asInstanceOf[SequenceNode].children.head
        val BindNode(v159, v160) = v158
        assert(v159.id == 241)
        matchTerminalChoiceChar(v160)
      case 247 =>
        val v161 = v157.asInstanceOf[SequenceNode].children.head
        val BindNode(v162, v163) = v161
        assert(v162.id == 248)
        matchTerminalChoiceRange(v163)
    }
  }

  def matchLineComment(node: Node): String = {
    val BindNode(v195, v196) = node
    v195.id match {
      case 16 =>
        ""
    }
  }

  def matchDef(node: Node): Def = {
    val BindNode(v809, v810) = node
    v809.id match {
      case 35 =>
        val v811 = v810.asInstanceOf[SequenceNode].children.head
        val BindNode(v812, v813) = v811
        assert(v812.id == 36)
        matchRule(v813)
      case 116 =>
        val v814 = v810.asInstanceOf[SequenceNode].children.head
        val BindNode(v815, v816) = v814
        assert(v815.id == 117)
        matchTypeDef(v816)
    }
  }

  def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
    val BindNode(v334, v335) = node
    v334.id match {
      case 249 =>
        val v336 = v335.asInstanceOf[SequenceNode].children.head
        val BindNode(v337, v338) = v336
        assert(v337.id == 241)
        val v339 = v335.asInstanceOf[SequenceNode].children(2)
        val BindNode(v340, v341) = v339
        assert(v340.id == 241)
        TerminalChoiceRange(matchTerminalChoiceChar(v338), matchTerminalChoiceChar(v341))
    }
  }

  def matchBoolAndExpr(node: Node): BoolAndExpr = {
    val BindNode(v414, v415) = node
    v414.id match {
      case 329 =>
        val v416 = v415.asInstanceOf[SequenceNode].children.head
        val BindNode(v417, v418) = v416
        assert(v417.id == 330)
        val v419 = v415.asInstanceOf[SequenceNode].children(4)
        val BindNode(v420, v421) = v419
        assert(v420.id == 328)
        BinOp(Op.OR, matchBoolEqExpr(v418), matchBoolAndExpr(v421))
      case 426 =>
        val v422 = v415.asInstanceOf[SequenceNode].children.head
        val BindNode(v423, v424) = v422
        assert(v423.id == 330)
        matchBoolEqExpr(v424)
    }
  }

  def matchInPlaceChoices(node: Node): InPlaceChoices = {
    val BindNode(v457, v458) = node
    v457.id match {
      case 269 =>
        val v459 = v458.asInstanceOf[SequenceNode].children.head
        val BindNode(v460, v461) = v459
        assert(v460.id == 197)
        val v462 = v458.asInstanceOf[SequenceNode].children(1)
        val v463 = unrollRepeat0(v462).map { elem =>
          val BindNode(v464, v465) = elem
          assert(v464.id == 272)
          val BindNode(v466, v467) = v465
          v466.id match {
            case 273 =>
              val BindNode(v468, v469) = v467
              assert(v468.id == 274)
              val v470 = v469.asInstanceOf[SequenceNode].children(3)
              val BindNode(v471, v472) = v470
              assert(v471.id == 197)
              matchSequence(v472)
          }
        }
        InPlaceChoices(List(matchSequence(v461)) ++ v463)
    }
  }

  def matchNonterminal(node: Node): Nonterminal = {
    val BindNode(v190, v191) = node
    v190.id match {
      case 41 =>
        val v192 = v191.asInstanceOf[SequenceNode].children.head
        val BindNode(v193, v194) = v192
        assert(v193.id == 42)
        Nonterminal(matchNonterminalName(v194))
    }
  }

  def matchGrammar(node: Node): Grammar = {
    val BindNode(v953, v954) = node
    v953.id match {
      case 3 =>
        val v955 = v954.asInstanceOf[SequenceNode].children(1)
        val BindNode(v956, v957) = v955
        assert(v956.id == 34)
        val v958 = v954.asInstanceOf[SequenceNode].children(2)
        val v959 = unrollRepeat0(v958).map { elem =>
          val BindNode(v960, v961) = elem
          assert(v960.id == 447)
          val BindNode(v962, v963) = v961
          v962.id match {
            case 448 =>
              val BindNode(v964, v965) = v963
              assert(v964.id == 449)
              val v966 = v965.asInstanceOf[SequenceNode].children(1)
              val BindNode(v967, v968) = v966
              assert(v967.id == 34)
              matchDef(v968)
          }
        }
        Grammar(List(matchDef(v957)) ++ v959)
    }
  }

  def matchSubType(node: Node): SubType = {
    val BindNode(v682, v683) = node
    v682.id match {
      case 99 =>
        val v684 = v683.asInstanceOf[SequenceNode].children.head
        val BindNode(v685, v686) = v684
        assert(v685.id == 100)
        matchTypeName(v686)
      case 118 =>
        val v687 = v683.asInstanceOf[SequenceNode].children.head
        val BindNode(v688, v689) = v687
        assert(v688.id == 119)
        matchClassDef(v689)
      case 154 =>
        val v690 = v683.asInstanceOf[SequenceNode].children.head
        val BindNode(v691, v692) = v690
        assert(v691.id == 155)
        matchSuperDef(v692)
    }
  }

  def matchAdditiveExpr(node: Node): AdditiveExpr = {
    val BindNode(v79, v80) = node
    v79.id match {
      case 335 =>
        val v81 = v80.asInstanceOf[SequenceNode].children(2)
        val BindNode(v82, v83) = v81
        assert(v82.id == 407)
        val BindNode(v84, v85) = v83
        val v88 = v80.asInstanceOf[SequenceNode].children.head
        val BindNode(v89, v90) = v88
        assert(v89.id == 336)
        val v91 = v80.asInstanceOf[SequenceNode].children(4)
        val BindNode(v92, v93) = v91
        assert(v92.id == 334)
        BinOp(v84.id match {
          case 408 =>
            val BindNode(v86, v87) = v85
            assert(v86.id == 409)
            Op.ADD
        }, matchPrefixNotExpr(v90), matchAdditiveExpr(v93))
      case 410 =>
        val v94 = v80.asInstanceOf[SequenceNode].children.head
        val BindNode(v95, v96) = v94
        assert(v95.id == 336)
        matchPrefixNotExpr(v96)
    }
  }

  def matchStringChar(node: Node): StringChar = {
    val BindNode(v342, v343) = node
    v342.id match {
      case 261 =>
        val v344 = v343.asInstanceOf[SequenceNode].children.head
        val BindNode(v345, v346) = v344
        assert(v345.id == 262)
        CharAsIs(v346.toString.charAt(0))
      case 264 =>
        val v347 = v343.asInstanceOf[SequenceNode].children(1)
        val BindNode(v348, v349) = v347
        assert(v348.id == 265)
        CharEscaped(v349.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0))
      case 230 =>
        val v350 = v343.asInstanceOf[SequenceNode].children.head
        val BindNode(v351, v352) = v350
        assert(v351.id == 231)
        matchUnicodeChar(v352)
    }
  }

  def matchLongest(node: Node): Longest = {
    val BindNode(v784, v785) = node
    v784.id match {
      case 278 =>
        val v786 = v785.asInstanceOf[SequenceNode].children(2)
        val BindNode(v787, v788) = v786
        assert(v787.id == 268)
        Longest(matchInPlaceChoices(v788))
    }
  }

  def matchEnumTypeDef(node: Node): EnumTypeDef = {
    val BindNode(v164, v165) = node
    v164.id match {
      case 177 =>
        val v166 = v165.asInstanceOf[SequenceNode].children.head
        val BindNode(v167, v168) = v166
        assert(v167.id == 113)
        val v169 = v165.asInstanceOf[SequenceNode].children(4)
        val BindNode(v170, v171) = v169
        assert(v170.id == 178)
        val BindNode(v172, v173) = v171
        EnumTypeDef(matchEnumTypeName(v168), v172.id match {
          case 179 =>
            val BindNode(v174, v175) = v173
            assert(v174.id == 180)
            val v176 = v175.asInstanceOf[SequenceNode].children.head
            val BindNode(v177, v178) = v176
            assert(v177.id == 45)
            val v179 = v175.asInstanceOf[SequenceNode].children(1)
            val v180 = unrollRepeat0(v179).map { elem =>
              val BindNode(v181, v182) = elem
              assert(v181.id == 183)
              val BindNode(v183, v184) = v182
              v183.id match {
                case 184 =>
                  val BindNode(v185, v186) = v184
                  assert(v185.id == 185)
                  val v187 = v186.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v188, v189) = v187
                  assert(v188.id == 45)
                  matchId(v189)
              }
            }
            List(matchId(v178)) ++ v180
        })
    }
  }

  def matchBinderExpr(node: Node): BinderExpr = {
    val BindNode(v97, v98) = node
    v97.id match {
      case 287 =>
        val v99 = v98.asInstanceOf[SequenceNode].children.head
        val BindNode(v100, v101) = v99
        assert(v100.id == 288)
        matchRef(v101)
      case 340 =>
        val v102 = v98.asInstanceOf[SequenceNode].children.head
        val BindNode(v103, v104) = v102
        assert(v103.id == 341)
        matchBindExpr(v104)
      case 321 =>
        val v105 = v98.asInstanceOf[SequenceNode].children(2)
        val BindNode(v106, v107) = v105
        assert(v106.id == 322)
        matchPExpr(v107)
    }
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
