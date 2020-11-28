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
import com.giyeok.jparser.nparser.Parser
import com.giyeok.jparser.nparser.RepeatUtils.unrollRepeat0
import com.giyeok.jparser.nparser.RepeatUtils.unrollRepeat1
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
      286 -> NGrammar.NNonterminal(286, Symbols.Nonterminal("Processor"), Set(287, 326)),
      288 -> NGrammar.NNonterminal(288, Symbols.Nonterminal("Ref"), Set(289, 316, 321)),
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
      322 -> NGrammar.NNonterminal(322, Symbols.Nonterminal("AllRawRef"), Set(323)),
      324 -> NGrammar.NProxy(324, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$'), Symbols.ExactChar('$')))), 325),
      327 -> NGrammar.NNonterminal(327, Symbols.Nonterminal("PExpr"), Set(328)),
      329 -> NGrammar.NNonterminal(329, Symbols.Nonterminal("TernaryExpr"), Set(330, 438)),
      331 -> NGrammar.NNonterminal(331, Symbols.Nonterminal("BoolOrExpr"), Set(332, 434)),
      333 -> NGrammar.NNonterminal(333, Symbols.Nonterminal("BoolAndExpr"), Set(334, 431)),
      335 -> NGrammar.NNonterminal(335, Symbols.Nonterminal("BoolEqExpr"), Set(336, 428)),
      337 -> NGrammar.NNonterminal(337, Symbols.Nonterminal("ElvisExpr"), Set(338, 418)),
      339 -> NGrammar.NNonterminal(339, Symbols.Nonterminal("AdditiveExpr"), Set(340, 415)),
      341 -> NGrammar.NNonterminal(341, Symbols.Nonterminal("PrefixNotExpr"), Set(342, 343)),
      344 -> NGrammar.NNonterminal(344, Symbols.Nonterminal("Atom"), Set(287, 345, 349, 364, 379, 382, 396, 411)),
      346 -> NGrammar.NNonterminal(346, Symbols.Nonterminal("BindExpr"), Set(347)),
      348 -> NGrammar.NNonterminal(348, Symbols.Nonterminal("BinderExpr"), Set(287, 345, 326)),
      350 -> NGrammar.NNonterminal(350, Symbols.Nonterminal("NamedConstructExpr"), Set(351)),
      352 -> NGrammar.NNonterminal(352, Symbols.Nonterminal("NamedConstructParams"), Set(353)),
      354 -> NGrammar.NOneOf(354, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))))), Set(355)),
      355 -> NGrammar.NProxy(355, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))), 356),
      357 -> NGrammar.NNonterminal(357, Symbols.Nonterminal("NamedParam"), Set(358)),
      359 -> NGrammar.NRepeat(359, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), 7, 360),
      361 -> NGrammar.NOneOf(361, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), Set(362)),
      362 -> NGrammar.NProxy(362, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))), 363),
      365 -> NGrammar.NNonterminal(365, Symbols.Nonterminal("FuncCallOrConstructExpr"), Set(366)),
      367 -> NGrammar.NNonterminal(367, Symbols.Nonterminal("TypeOrFuncName"), Set(43, 89)),
      368 -> NGrammar.NNonterminal(368, Symbols.Nonterminal("CallParams"), Set(369)),
      370 -> NGrammar.NOneOf(370, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(371, 134)),
      371 -> NGrammar.NOneOf(371, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Set(372)),
      372 -> NGrammar.NProxy(372, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))), 373),
      374 -> NGrammar.NRepeat(374, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), 7, 375),
      376 -> NGrammar.NOneOf(376, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), Set(377)),
      377 -> NGrammar.NProxy(377, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 378),
      380 -> NGrammar.NNonterminal(380, Symbols.Nonterminal("ArrayExpr"), Set(381)),
      383 -> NGrammar.NNonterminal(383, Symbols.Nonterminal("Literal"), Set(86, 384, 388, 391)),
      385 -> NGrammar.NOneOf(385, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))))), Set(386, 387)),
      386 -> NGrammar.NProxy(386, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), 78),
      387 -> NGrammar.NProxy(387, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))), 82),
      389 -> NGrammar.NNonterminal(389, Symbols.Nonterminal("CharChar"), Set(390)),
      392 -> NGrammar.NRepeat(392, Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), 7, 393),
      394 -> NGrammar.NNonterminal(394, Symbols.Nonterminal("StrChar"), Set(395)),
      397 -> NGrammar.NNonterminal(397, Symbols.Nonterminal("EnumValue"), Set(398)),
      399 -> NGrammar.NLongest(399, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))))))), 400),
      400 -> NGrammar.NOneOf(400, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue")))))), Set(401, 407)),
      401 -> NGrammar.NProxy(401, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), 402),
      403 -> NGrammar.NNonterminal(403, Symbols.Nonterminal("CanonicalEnumValue"), Set(404)),
      405 -> NGrammar.NNonterminal(405, Symbols.Nonterminal("EnumValueName"), Set(406)),
      407 -> NGrammar.NProxy(407, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue")))), 408),
      409 -> NGrammar.NNonterminal(409, Symbols.Nonterminal("ShortenedEnumValue"), Set(410)),
      412 -> NGrammar.NOneOf(412, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))))), Set(413)),
      413 -> NGrammar.NProxy(413, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))), 414),
      416 -> NGrammar.NProxy(416, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':')))), 417),
      419 -> NGrammar.NOneOf(419, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))))), Set(420, 424)),
      420 -> NGrammar.NProxy(420, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), 421),
      422 -> NGrammar.NProxy(422, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('=')))), 423),
      424 -> NGrammar.NProxy(424, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))), 425),
      426 -> NGrammar.NProxy(426, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('=')))), 427),
      429 -> NGrammar.NProxy(429, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|')))), 430),
      432 -> NGrammar.NProxy(432, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&')))), 433),
      435 -> NGrammar.NLongest(435, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))), 436),
      436 -> NGrammar.NOneOf(436, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr")))))), Set(437)),
      437 -> NGrammar.NProxy(437, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr")))), 328),
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
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Def"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), 0), Symbols.Nonterminal("WS"))), Seq(4, 34, 449, 4)),
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
      194 -> NGrammar.NSequence(194, Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0))), Seq(195, 444)),
      196 -> NGrammar.NSequence(196, Symbols.Sequence(Seq(Symbols.Nonterminal("Sequence"))), Seq(197)),
      198 -> NGrammar.NSequence(198, Symbols.Sequence(Seq(Symbols.Nonterminal("Elem"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), 0))), Seq(199, 439)),
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
      321 -> NGrammar.NSequence(321, Symbols.Sequence(Seq(Symbols.Nonterminal("AllRawRef"))), Seq(322)),
      323 -> NGrammar.NSequence(323, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$'), Symbols.ExactChar('$')))))), Seq(324)),
      325 -> NGrammar.NSequence(325, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('$'), Symbols.ExactChar('$'))), Seq(227, 292, 292)),
      326 -> NGrammar.NSequence(326, Symbols.Sequence(Seq(Symbols.ExactChar('{'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(161, 4, 327, 4, 174)),
      328 -> NGrammar.NSequence(328, Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))), Seq(329)),
      330 -> NGrammar.NSequence(330, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar('?'), Symbols.Nonterminal("WS"), Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))), Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TernaryExpr"))))))))), Seq(331, 4, 190, 4, 435, 4, 95, 4, 435)),
      332 -> NGrammar.NSequence(332, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolOrExpr"))), Seq(333, 4, 432, 4, 331)),
      334 -> NGrammar.NSequence(334, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolAndExpr"))), Seq(335, 4, 429, 4, 333)),
      336 -> NGrammar.NSequence(336, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))))))))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("BoolEqExpr"))), Seq(337, 4, 419, 4, 335)),
      338 -> NGrammar.NSequence(338, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"), Symbols.Nonterminal("WS"), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':')))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ElvisExpr"))), Seq(339, 4, 416, 4, 337)),
      340 -> NGrammar.NSequence(340, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("AdditiveExpr"))), Seq(341, 4, 412, 4, 339)),
      342 -> NGrammar.NSequence(342, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PrefixNotExpr"))), Seq(210, 4, 341)),
      343 -> NGrammar.NSequence(343, Symbols.Sequence(Seq(Symbols.Nonterminal("Atom"))), Seq(344)),
      345 -> NGrammar.NSequence(345, Symbols.Sequence(Seq(Symbols.Nonterminal("BindExpr"))), Seq(346)),
      347 -> NGrammar.NSequence(347, Symbols.Sequence(Seq(Symbols.Nonterminal("ValRef"), Symbols.Nonterminal("BinderExpr"))), Seq(290, 348)),
      349 -> NGrammar.NSequence(349, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedConstructExpr"))), Seq(350)),
      351 -> NGrammar.NSequence(351, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("SuperTypes")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedConstructParams"))), Seq(100, 157, 4, 352)),
      353 -> NGrammar.NSequence(353, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.ExactChar(')'))), Seq(139, 4, 354, 152)),
      356 -> NGrammar.NSequence(356, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.Nonterminal("WS"))), Seq(357, 359, 4)),
      358 -> NGrammar.NSequence(358, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"))), Seq(146, 91, 4, 191, 4, 327)),
      360 -> NGrammar.NSequence(360, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))))), Seq(359, 361)),
      363 -> NGrammar.NSequence(363, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam"))), Seq(4, 133, 4, 357)),
      364 -> NGrammar.NSequence(364, Symbols.Sequence(Seq(Symbols.Nonterminal("FuncCallOrConstructExpr"))), Seq(365)),
      366 -> NGrammar.NSequence(366, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeOrFuncName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("CallParams"))), Seq(367, 4, 368)),
      369 -> NGrammar.NSequence(369, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar(')'))), Seq(139, 4, 370, 152)),
      373 -> NGrammar.NSequence(373, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS"))), Seq(327, 374, 4)),
      375 -> NGrammar.NSequence(375, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))))), Seq(374, 376)),
      378 -> NGrammar.NSequence(378, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"))), Seq(4, 133, 4, 327)),
      379 -> NGrammar.NSequence(379, Symbols.Sequence(Seq(Symbols.Nonterminal("ArrayExpr"))), Seq(380)),
      381 -> NGrammar.NSequence(381, Symbols.Sequence(Seq(Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), 0), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar(']'))), Seq(102, 4, 370, 103)),
      382 -> NGrammar.NSequence(382, Symbols.Sequence(Seq(Symbols.Nonterminal("Literal"))), Seq(383)),
      384 -> NGrammar.NSequence(384, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))))))))))), Seq(385)),
      388 -> NGrammar.NSequence(388, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("CharChar"), Symbols.ExactChar('\''))), Seq(223, 389, 223)),
      390 -> NGrammar.NSequence(390, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChar"))), Seq(224)),
      391 -> NGrammar.NSequence(391, Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), Symbols.ExactChar('"'))), Seq(257, 392, 257)),
      393 -> NGrammar.NSequence(393, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), Symbols.Nonterminal("StrChar"))), Seq(392, 394)),
      395 -> NGrammar.NSequence(395, Symbols.Sequence(Seq(Symbols.Nonterminal("StringChar"))), Seq(260)),
      396 -> NGrammar.NSequence(396, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumValue"))), Seq(397)),
      398 -> NGrammar.NSequence(398, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))))))))), Seq(399)),
      402 -> NGrammar.NSequence(402, Symbols.Sequence(Seq(Symbols.Nonterminal("CanonicalEnumValue"))), Seq(403)),
      404 -> NGrammar.NSequence(404, Symbols.Sequence(Seq(Symbols.Nonterminal("EnumTypeName"), Symbols.ExactChar('.'), Symbols.Nonterminal("EnumValueName"))), Seq(113, 235, 405)),
      406 -> NGrammar.NSequence(406, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(45)),
      408 -> NGrammar.NSequence(408, Symbols.Sequence(Seq(Symbols.Nonterminal("ShortenedEnumValue"))), Seq(409)),
      410 -> NGrammar.NSequence(410, Symbols.Sequence(Seq(Symbols.ExactChar('%'), Symbols.Nonterminal("EnumValueName"))), Seq(115, 405)),
      411 -> NGrammar.NSequence(411, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(139, 4, 327, 4, 152)),
      414 -> NGrammar.NSequence(414, Symbols.Sequence(Seq(Symbols.ExactChar('+'))), Seq(217)),
      415 -> NGrammar.NSequence(415, Symbols.Sequence(Seq(Symbols.Nonterminal("PrefixNotExpr"))), Seq(341)),
      417 -> NGrammar.NSequence(417, Symbols.Sequence(Seq(Symbols.ExactChar('?'), Symbols.ExactChar(':'))), Seq(190, 95)),
      418 -> NGrammar.NSequence(418, Symbols.Sequence(Seq(Symbols.Nonterminal("AdditiveExpr"))), Seq(339)),
      421 -> NGrammar.NSequence(421, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('=')))))), Seq(422)),
      423 -> NGrammar.NSequence(423, Symbols.Sequence(Seq(Symbols.ExactChar('='), Symbols.ExactChar('='))), Seq(191, 191)),
      425 -> NGrammar.NSequence(425, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('=')))))), Seq(426)),
      427 -> NGrammar.NSequence(427, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.ExactChar('='))), Seq(210, 191)),
      428 -> NGrammar.NSequence(428, Symbols.Sequence(Seq(Symbols.Nonterminal("ElvisExpr"))), Seq(337)),
      430 -> NGrammar.NSequence(430, Symbols.Sequence(Seq(Symbols.ExactChar('|'), Symbols.ExactChar('|'))), Seq(275, 275)),
      431 -> NGrammar.NSequence(431, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolEqExpr"))), Seq(335)),
      433 -> NGrammar.NSequence(433, Symbols.Sequence(Seq(Symbols.ExactChar('&'), Symbols.ExactChar('&'))), Seq(205, 205)),
      434 -> NGrammar.NSequence(434, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolAndExpr"))), Seq(333)),
      438 -> NGrammar.NSequence(438, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolOrExpr"))), Seq(331)),
      440 -> NGrammar.NSequence(440, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))))), Seq(439, 441)),
      443 -> NGrammar.NSequence(443, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem"))), Seq(4, 199)),
      445 -> NGrammar.NSequence(445, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))))), Seq(444, 446)),
      448 -> NGrammar.NSequence(448, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS"))), Seq(4, 275, 4, 195)),
      450 -> NGrammar.NSequence(450, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def")))))))), Seq(449, 451)),
      453 -> NGrammar.NSequence(453, Symbols.Sequence(Seq(Symbols.Nonterminal("WSNL"), Symbols.Nonterminal("Def"))), Seq(454, 34)),
      455 -> NGrammar.NSequence(455, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS"))))))))), Seq(456)),
      459 -> NGrammar.NSequence(459, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.ExactChar('\n'), Symbols.Nonterminal("WS"))), Seq(460, 25, 4)),
      461 -> NGrammar.NSequence(461, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' '))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))))), Seq(460, 462)),
      464 -> NGrammar.NSequence(464, Symbols.Sequence(Seq(Symbols.Chars(Set('\t', '\r', ' ')))), Seq(465))),
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

  sealed trait PExpr extends BinderExpr with Processor

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

  case class ConcreteClassDef(name: TypeName, supers: List[TypeName], params: List[ClassParamDef]) extends ClassDef

  case class FuncCallOrConstructExpr(funcName: TypeOrFuncName, params: List[PExpr]) extends Atom

  case class Sequence(seq: List[Elem]) extends Symbol

  case class InPlaceChoices(choices: List[Sequence]) extends AtomSymbol

  case class SuperDef(typeName: TypeName, subs: Option[List[SubType]], supers: Option[List[TypeName]]) extends TypeDef with SubType

  case class Rule(lhs: LHS, rhs: List[Sequence]) extends Def

  sealed trait TypeDef extends NonNullTypeDesc with Def

  case class StrLiteral(value: List[StringChar]) extends Literal

  case class CharAsIs(value: Char) extends StringChar with TerminalChoiceChar with TerminalChar

  case class AnyTerminal() extends Terminal

  case class BindExpr(ctx: ValRef, binder: BinderExpr) extends Atom

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

  sealed trait TernaryExpr extends PExpr

  case class NonterminalName(name: String)

  sealed trait NonNullTypeDesc

  case class AllRawRef() extends Ref

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

  sealed trait Ref extends Atom

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

  def matchClassParamsDef(node: Node): List[ClassParamDef] = {
    val BindNode(v545, v546) = node
    v545.id match {
      case 138 =>
        val v548 = v546.asInstanceOf[SequenceNode].children(2)
        val BindNode(v549, v550) = v548
        assert(v549.id == 140)
        val BindNode(v551, v552) = v550
        val v547 = v551.id match {
          case 134 =>
            None
          case 141 =>
            val BindNode(v553, v554) = v552
            Some(v553.id match {
              case 142 =>
                val BindNode(v555, v556) = v554
                assert(v555.id == 143)
                val v557 = v556.asInstanceOf[SequenceNode].children.head
                val BindNode(v558, v559) = v557
                assert(v558.id == 144)
                val v560 = v556.asInstanceOf[SequenceNode].children(1)
                val v561 = unrollRepeat0(v560).map { elem =>
                  val BindNode(v562, v563) = elem
                  assert(v562.id == 149)
                  val BindNode(v564, v565) = v563
                  v564.id match {
                    case 150 =>
                      val BindNode(v566, v567) = v565
                      assert(v566.id == 151)
                      val v568 = v567.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v569, v570) = v568
                      assert(v569.id == 144)
                      matchClassParamDef(v570)
                  }
                }
                List(matchClassParamDef(v559)) ++ v561
            })
        }
        if (v547.isDefined) v547.get else List()
    }
  }

  def matchBinderExpr(node: Node): BinderExpr = {
    val BindNode(v79, v80) = node
    v79.id match {
      case 287 =>
        val v81 = v80.asInstanceOf[SequenceNode].children.head
        val BindNode(v82, v83) = v81
        assert(v82.id == 288)
        matchRef(v83)
      case 345 =>
        val v84 = v80.asInstanceOf[SequenceNode].children.head
        val BindNode(v85, v86) = v84
        assert(v85.id == 346)
        matchBindExpr(v86)
      case 326 =>
        val v87 = v80.asInstanceOf[SequenceNode].children(2)
        val BindNode(v88, v89) = v87
        assert(v88.id == 327)
        matchPExpr(v89)
    }
  }

  def matchTypeDef(node: Node): TypeDef = {
    val BindNode(v498, v499) = node
    v498.id match {
      case 118 =>
        val v500 = v499.asInstanceOf[SequenceNode].children.head
        val BindNode(v501, v502) = v500
        assert(v501.id == 119)
        matchClassDef(v502)
      case 154 =>
        val v503 = v499.asInstanceOf[SequenceNode].children.head
        val BindNode(v504, v505) = v503
        assert(v504.id == 155)
        matchSuperDef(v505)
      case 175 =>
        val v506 = v499.asInstanceOf[SequenceNode].children.head
        val BindNode(v507, v508) = v506
        assert(v507.id == 176)
        matchEnumTypeDef(v508)
    }
  }

  def matchAdditiveExpr(node: Node): AdditiveExpr = {
    val BindNode(v756, v757) = node
    v756.id match {
      case 340 =>
        val v758 = v757.asInstanceOf[SequenceNode].children(2)
        val BindNode(v759, v760) = v758
        assert(v759.id == 412)
        val BindNode(v761, v762) = v760
        val v765 = v757.asInstanceOf[SequenceNode].children.head
        val BindNode(v766, v767) = v765
        assert(v766.id == 341)
        val v768 = v757.asInstanceOf[SequenceNode].children(4)
        val BindNode(v769, v770) = v768
        assert(v769.id == 339)
        BinOp(v761.id match {
          case 413 =>
            val BindNode(v763, v764) = v762
            assert(v763.id == 414)
            Op.ADD
        }, matchPrefixNotExpr(v767), matchAdditiveExpr(v770))
      case 415 =>
        val v771 = v757.asInstanceOf[SequenceNode].children.head
        val BindNode(v772, v773) = v771
        assert(v772.id == 341)
        matchPrefixNotExpr(v773)
    }
  }

  def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
    val BindNode(v138, v139) = node
    v138.id match {
      case 240 =>
        val v140 = v139.asInstanceOf[SequenceNode].children.head
        val BindNode(v141, v142) = v140
        assert(v141.id == 241)
        matchTerminalChoiceChar(v142)
      case 247 =>
        val v143 = v139.asInstanceOf[SequenceNode].children.head
        val BindNode(v144, v145) = v143
        assert(v144.id == 248)
        matchTerminalChoiceRange(v145)
    }
  }

  def matchKeyword(node: Node): KeyWord.Value = {
    val BindNode(v828, v829) = node
    v828.id match {
      case 86 =>
        KeyWord.NULL
      case 71 =>
        KeyWord.STRING
      case 78 =>
        KeyWord.TRUE
      case 56 =>
        KeyWord.BOOLEAN
      case 65 =>
        KeyWord.CHAR
      case 82 =>
        KeyWord.FALSE
    }
  }

  def matchTerminalChoice(node: Node): TerminalChoice = {
    val BindNode(v807, v808) = node
    v807.id match {
      case 238 =>
        val v809 = v808.asInstanceOf[SequenceNode].children(1)
        val BindNode(v810, v811) = v809
        assert(v810.id == 239)
        val v812 = v808.asInstanceOf[SequenceNode].children(2)
        val v813 = unrollRepeat1(v812).map { elem =>
          val BindNode(v814, v815) = elem
          assert(v814.id == 239)
          matchTerminalChoiceElem(v815)
        }
        TerminalChoice(List(matchTerminalChoiceElem(v811)) ++ v813)
      case 253 =>
        val v816 = v808.asInstanceOf[SequenceNode].children(1)
        val BindNode(v817, v818) = v816
        assert(v817.id == 248)
        TerminalChoice(List(matchTerminalChoiceRange(v818)))
    }
  }

  def matchPExpr(node: Node): PExpr = {
    val BindNode(v273, v274) = node
    v273.id match {
      case 328 =>
        val v275 = v274.asInstanceOf[SequenceNode].children.head
        val BindNode(v276, v277) = v275
        assert(v276.id == 329)
        matchTernaryExpr(v277)
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

  def matchSequence(node: Node): Sequence = {
    val BindNode(v411, v412) = node
    v411.id match {
      case 198 =>
        val v413 = v412.asInstanceOf[SequenceNode].children.head
        val BindNode(v414, v415) = v413
        assert(v414.id == 199)
        val v416 = v412.asInstanceOf[SequenceNode].children(1)
        val v417 = unrollRepeat0(v416).map { elem =>
          val BindNode(v418, v419) = elem
          assert(v418.id == 441)
          val BindNode(v420, v421) = v419
          v420.id match {
            case 442 =>
              val BindNode(v422, v423) = v421
              assert(v422.id == 443)
              val v424 = v423.asInstanceOf[SequenceNode].children(1)
              val BindNode(v425, v426) = v424
              assert(v425.id == 199)
              matchElem(v426)
          }
        }
        Sequence(List(matchElem(v415)) ++ v417)
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

  def matchAtom(node: Node): Atom = {
    val BindNode(v107, v108) = node
    v107.id match {
      case 345 =>
        val v109 = v108.asInstanceOf[SequenceNode].children.head
        val BindNode(v110, v111) = v109
        assert(v110.id == 346)
        matchBindExpr(v111)
      case 396 =>
        val v112 = v108.asInstanceOf[SequenceNode].children.head
        val BindNode(v113, v114) = v112
        assert(v113.id == 397)
        matchEnumValue(v114)
      case 379 =>
        val v115 = v108.asInstanceOf[SequenceNode].children.head
        val BindNode(v116, v117) = v115
        assert(v116.id == 380)
        matchArrayExpr(v117)
      case 411 =>
        val v118 = v108.asInstanceOf[SequenceNode].children(2)
        val BindNode(v119, v120) = v118
        assert(v119.id == 327)
        ExprParen(matchPExpr(v120))
      case 364 =>
        val v121 = v108.asInstanceOf[SequenceNode].children.head
        val BindNode(v122, v123) = v121
        assert(v122.id == 365)
        matchFuncCallOrConstructExpr(v123)
      case 382 =>
        val v124 = v108.asInstanceOf[SequenceNode].children.head
        val BindNode(v125, v126) = v124
        assert(v125.id == 383)
        matchLiteral(v126)
      case 287 =>
        val v127 = v108.asInstanceOf[SequenceNode].children.head
        val BindNode(v128, v129) = v127
        assert(v128.id == 288)
        matchRef(v129)
      case 349 =>
        val v130 = v108.asInstanceOf[SequenceNode].children.head
        val BindNode(v131, v132) = v130
        assert(v131.id == 350)
        matchNamedConstructExpr(v132)
    }
  }

  def matchDef(node: Node): Def = {
    val BindNode(v799, v800) = node
    v799.id match {
      case 35 =>
        val v801 = v800.asInstanceOf[SequenceNode].children.head
        val BindNode(v802, v803) = v801
        assert(v802.id == 36)
        matchRule(v803)
      case 116 =>
        val v804 = v800.asInstanceOf[SequenceNode].children.head
        val BindNode(v805, v806) = v804
        assert(v805.id == 117)
        matchTypeDef(v806)
    }
  }

  def matchEnumTypeDef(node: Node): EnumTypeDef = {
    val BindNode(v146, v147) = node
    v146.id match {
      case 177 =>
        val v148 = v147.asInstanceOf[SequenceNode].children.head
        val BindNode(v149, v150) = v148
        assert(v149.id == 113)
        val v151 = v147.asInstanceOf[SequenceNode].children(4)
        val BindNode(v152, v153) = v151
        assert(v152.id == 178)
        val BindNode(v154, v155) = v153
        EnumTypeDef(matchEnumTypeName(v150), v154.id match {
          case 179 =>
            val BindNode(v156, v157) = v155
            assert(v156.id == 180)
            val v158 = v157.asInstanceOf[SequenceNode].children.head
            val BindNode(v159, v160) = v158
            assert(v159.id == 45)
            val v161 = v157.asInstanceOf[SequenceNode].children(1)
            val v162 = unrollRepeat0(v161).map { elem =>
              val BindNode(v163, v164) = elem
              assert(v163.id == 183)
              val BindNode(v165, v166) = v164
              v165.id match {
                case 184 =>
                  val BindNode(v167, v168) = v166
                  assert(v167.id == 185)
                  val v169 = v168.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v170, v171) = v169
                  assert(v170.id == 45)
                  matchId(v171)
              }
            }
            List(matchId(v160)) ++ v162
        })
    }
  }

  def matchTypeOrFuncName(node: Node): TypeOrFuncName = {
    val BindNode(v824, v825) = node
    v824.id match {
      case 43 =>
        val v826 = v825.asInstanceOf[SequenceNode].children.head
        TypeOrFuncName(v826.toString)
      case 89 =>
        val v827 = v825.asInstanceOf[SequenceNode].children(1)
        TypeOrFuncName(v827.toString)
    }
  }

  def matchShortenedEnumValue(node: Node): ShortenedEnumValue = {
    val BindNode(v819, v820) = node
    v819.id match {
      case 410 =>
        val v821 = v820.asInstanceOf[SequenceNode].children(1)
        val BindNode(v822, v823) = v821
        assert(v822.id == 405)
        ShortenedEnumValue(matchEnumValueName(v823))
    }
  }

  def matchTypeName(node: Node): TypeName = {
    val BindNode(v189, v190) = node
    v189.id match {
      case 43 =>
        val v191 = v190.asInstanceOf[SequenceNode].children.head
        TypeName(v191.toString)
      case 89 =>
        val v192 = v190.asInstanceOf[SequenceNode].children(1)
        TypeName(v192.toString)
    }
  }

  def matchValRef(node: Node): ValRef = {
    val BindNode(v323, v324) = node
    v323.id match {
      case 291 =>
        val v325 = v324.asInstanceOf[SequenceNode].children(2)
        val BindNode(v326, v327) = v325
        assert(v326.id == 303)
        val v328 = v324.asInstanceOf[SequenceNode].children(1)
        val BindNode(v329, v330) = v328
        assert(v329.id == 293)
        val BindNode(v331, v332) = v330
        ValRef(matchRefIdx(v327), v331.id match {
          case 134 =>
            None
          case 294 =>
            Some(matchCondSymPath(v332))
        })
    }
  }

  def matchStringChar(node: Node): StringChar = {
    val BindNode(v312, v313) = node
    v312.id match {
      case 261 =>
        val v314 = v313.asInstanceOf[SequenceNode].children.head
        val BindNode(v315, v316) = v314
        assert(v315.id == 262)
        CharAsIs(v316.toString.charAt(0))
      case 264 =>
        val v317 = v313.asInstanceOf[SequenceNode].children(1)
        val BindNode(v318, v319) = v317
        assert(v318.id == 265)
        CharEscaped(v319.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0))
      case 230 =>
        val v320 = v313.asInstanceOf[SequenceNode].children.head
        val BindNode(v321, v322) = v320
        assert(v321.id == 231)
        matchUnicodeChar(v322)
    }
  }

  def matchProcessor(node: Node): Processor = {
    val BindNode(v181, v182) = node
    v181.id match {
      case 287 =>
        val v183 = v182.asInstanceOf[SequenceNode].children.head
        val BindNode(v184, v185) = v183
        assert(v184.id == 288)
        matchRef(v185)
      case 326 =>
        val v186 = v182.asInstanceOf[SequenceNode].children(2)
        val BindNode(v187, v188) = v186
        assert(v187.id == 327)
        matchPExpr(v188)
    }
  }

  def matchSuperDef(node: Node): SuperDef = {
    val BindNode(v443, v444) = node
    v443.id match {
      case 156 =>
        val v445 = v444.asInstanceOf[SequenceNode].children.head
        val BindNode(v446, v447) = v445
        assert(v446.id == 100)
        val v448 = v444.asInstanceOf[SequenceNode].children(4)
        val BindNode(v449, v450) = v448
        assert(v449.id == 162)
        val BindNode(v451, v452) = v450
        val v460 = v444.asInstanceOf[SequenceNode].children(1)
        val BindNode(v461, v462) = v460
        assert(v461.id == 157)
        val BindNode(v463, v464) = v462
        SuperDef(matchTypeName(v447), v451.id match {
          case 134 =>
            None
          case 163 =>
            val BindNode(v453, v454) = v452
            Some(v453.id match {
              case 164 =>
                val BindNode(v455, v456) = v454
                assert(v455.id == 165)
                val v457 = v456.asInstanceOf[SequenceNode].children(1)
                val BindNode(v458, v459) = v457
                assert(v458.id == 166)
                matchSubTypes(v459)
            })
        }, v463.id match {
          case 134 =>
            None
          case 158 =>
            val BindNode(v465, v466) = v464
            Some(v465.id match {
              case 159 =>
                val BindNode(v467, v468) = v466
                assert(v467.id == 160)
                val v469 = v468.asInstanceOf[SequenceNode].children(1)
                val BindNode(v470, v471) = v469
                assert(v470.id == 121)
                matchSuperTypes(v471)
            })
        })
    }
  }

  def matchPreUnSymbol(node: Node): PreUnSymbol = {
    val BindNode(v834, v835) = node
    v834.id match {
      case 207 =>
        val v836 = v835.asInstanceOf[SequenceNode].children(2)
        val BindNode(v837, v838) = v836
        assert(v837.id == 206)
        FollowedBy(matchPreUnSymbol(v838))
      case 209 =>
        val v839 = v835.asInstanceOf[SequenceNode].children(2)
        val BindNode(v840, v841) = v839
        assert(v840.id == 206)
        NotFollowedBy(matchPreUnSymbol(v841))
      case 211 =>
        val v842 = v835.asInstanceOf[SequenceNode].children.head
        val BindNode(v843, v844) = v842
        assert(v843.id == 212)
        matchPostUnSymbol(v844)
    }
  }

  def matchEnumValueName(node: Node): EnumValueName = {
    val BindNode(v930, v931) = node
    v930.id match {
      case 406 =>
        val v932 = v931.asInstanceOf[SequenceNode].children.head
        val BindNode(v933, v934) = v932
        assert(v933.id == 45)
        EnumValueName(matchId(v934))
    }
  }

  def matchGrammar(node: Node): Grammar = {
    val BindNode(v946, v947) = node
    v946.id match {
      case 3 =>
        val v948 = v947.asInstanceOf[SequenceNode].children(1)
        val BindNode(v949, v950) = v948
        assert(v949.id == 34)
        val v951 = v947.asInstanceOf[SequenceNode].children(2)
        val v952 = unrollRepeat0(v951).map { elem =>
          val BindNode(v953, v954) = elem
          assert(v953.id == 451)
          val BindNode(v955, v956) = v954
          v955.id match {
            case 452 =>
              val BindNode(v957, v958) = v956
              assert(v957.id == 453)
              val v959 = v958.asInstanceOf[SequenceNode].children(1)
              val BindNode(v960, v961) = v959
              assert(v960.id == 34)
              matchDef(v961)
          }
        }
        Grammar(List(matchDef(v950)) ++ v952)
    }
  }

  def matchNonNullTypeDesc(node: Node): NonNullTypeDesc = {
    val BindNode(v734, v735) = node
    v734.id match {
      case 101 =>
        val v736 = v735.asInstanceOf[SequenceNode].children(2)
        val BindNode(v737, v738) = v736
        assert(v737.id == 96)
        ArrayTypeDesc(matchTypeDesc(v738))
      case 116 =>
        val v739 = v735.asInstanceOf[SequenceNode].children.head
        val BindNode(v740, v741) = v739
        assert(v740.id == 117)
        matchTypeDef(v741)
      case 99 =>
        val v742 = v735.asInstanceOf[SequenceNode].children.head
        val BindNode(v743, v744) = v742
        assert(v743.id == 100)
        matchTypeName(v744)
      case 106 =>
        val v745 = v735.asInstanceOf[SequenceNode].children.head
        val BindNode(v746, v747) = v745
        assert(v746.id == 107)
        matchAnyType(v747)
      case 104 =>
        val v748 = v735.asInstanceOf[SequenceNode].children.head
        val BindNode(v749, v750) = v748
        assert(v749.id == 105)
        matchValueType(v750)
      case 112 =>
        val v751 = v735.asInstanceOf[SequenceNode].children.head
        val BindNode(v752, v753) = v751
        assert(v752.id == 113)
        matchEnumTypeName(v753)
    }
  }

  def matchBindExpr(node: Node): BindExpr = {
    val BindNode(v571, v572) = node
    v571.id match {
      case 347 =>
        val v573 = v572.asInstanceOf[SequenceNode].children.head
        val BindNode(v574, v575) = v573
        assert(v574.id == 290)
        val v576 = v572.asInstanceOf[SequenceNode].children(1)
        val BindNode(v577, v578) = v576
        assert(v577.id == 348)
        BindExpr(matchValRef(v575), matchBinderExpr(v578))
    }
  }

  def matchRef(node: Node): Ref = {
    val BindNode(v899, v900) = node
    v899.id match {
      case 289 =>
        val v901 = v900.asInstanceOf[SequenceNode].children.head
        val BindNode(v902, v903) = v901
        assert(v902.id == 290)
        matchValRef(v903)
      case 316 =>
        val v904 = v900.asInstanceOf[SequenceNode].children.head
        val BindNode(v905, v906) = v904
        assert(v905.id == 317)
        matchRawRef(v906)
      case 321 =>
        val v907 = v900.asInstanceOf[SequenceNode].children.head
        val BindNode(v908, v909) = v907
        assert(v908.id == 322)
        matchAllRawRef(v909)
    }
  }

  def matchEOF(node: Node): String = {
    val BindNode(v650, v651) = node
    v650.id match {
      case 30 =>
        ""
    }
  }

  def matchRule(node: Node): Rule = {
    val BindNode(v472, v473) = node
    v472.id match {
      case 37 =>
        val v474 = v473.asInstanceOf[SequenceNode].children.head
        val BindNode(v475, v476) = v474
        assert(v475.id == 38)
        val v477 = v473.asInstanceOf[SequenceNode].children(4)
        val BindNode(v478, v479) = v477
        assert(v478.id == 192)
        val BindNode(v480, v481) = v479
        Rule(matchLHS(v476), v480.id match {
          case 193 =>
            val BindNode(v482, v483) = v481
            assert(v482.id == 194)
            val v484 = v483.asInstanceOf[SequenceNode].children.head
            val BindNode(v485, v486) = v484
            assert(v485.id == 195)
            val v487 = v483.asInstanceOf[SequenceNode].children(1)
            val v488 = unrollRepeat0(v487).map { elem =>
              val BindNode(v489, v490) = elem
              assert(v489.id == 446)
              val BindNode(v491, v492) = v490
              v491.id match {
                case 447 =>
                  val BindNode(v493, v494) = v492
                  assert(v493.id == 448)
                  val v495 = v494.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v496, v497) = v495
                  assert(v496.id == 195)
                  matchRHS(v497)
              }
            }
            List(matchRHS(v486)) ++ v488
        })
    }
  }

  def matchNonterminal(node: Node): Nonterminal = {
    val BindNode(v172, v173) = node
    v172.id match {
      case 41 =>
        val v174 = v173.asInstanceOf[SequenceNode].children.head
        val BindNode(v175, v176) = v174
        assert(v175.id == 42)
        Nonterminal(matchNonterminalName(v176))
    }
  }

  def matchStrChar(node: Node): StringChar = {
    val BindNode(v725, v726) = node
    v725.id match {
      case 395 =>
        val v727 = v726.asInstanceOf[SequenceNode].children.head
        val BindNode(v728, v729) = v727
        assert(v728.id == 260)
        matchStringChar(v729)
    }
  }

  def matchBoolEqExpr(node: Node): BoolEqExpr = {
    val BindNode(v910, v911) = node
    v910.id match {
      case 336 =>
        val v912 = v911.asInstanceOf[SequenceNode].children(2)
        val BindNode(v913, v914) = v912
        assert(v913.id == 419)
        val BindNode(v915, v916) = v914
        val v921 = v911.asInstanceOf[SequenceNode].children.head
        val BindNode(v922, v923) = v921
        assert(v922.id == 337)
        val v924 = v911.asInstanceOf[SequenceNode].children(4)
        val BindNode(v925, v926) = v924
        assert(v925.id == 335)
        BinOp(v915.id match {
          case 420 =>
            val BindNode(v917, v918) = v916
            assert(v917.id == 421)
            Op.EQ
          case 424 =>
            val BindNode(v919, v920) = v916
            assert(v919.id == 425)
            Op.NE
        }, matchElvisExpr(v923), matchBoolEqExpr(v926))
      case 428 =>
        val v927 = v911.asInstanceOf[SequenceNode].children.head
        val BindNode(v928, v929) = v927
        assert(v928.id == 337)
        matchElvisExpr(v929)
    }
  }

  def matchLongest(node: Node): Longest = {
    val BindNode(v774, v775) = node
    v774.id match {
      case 278 =>
        val v776 = v775.asInstanceOf[SequenceNode].children(2)
        val BindNode(v777, v778) = v776
        assert(v777.id == 268)
        Longest(matchInPlaceChoices(v778))
    }
  }

  def matchTerminal(node: Node): Terminal = {
    val BindNode(v102, v103) = node
    v102.id match {
      case 222 =>
        val v104 = v103.asInstanceOf[SequenceNode].children(1)
        val BindNode(v105, v106) = v104
        assert(v105.id == 224)
        matchTerminalChar(v106)
      case 234 =>
        AnyTerminal()
    }
  }

  def matchTerminalChar(node: Node): TerminalChar = {
    val BindNode(v888, v889) = node
    v888.id match {
      case 225 =>
        val v890 = v889.asInstanceOf[SequenceNode].children.head
        val BindNode(v891, v892) = v890
        assert(v891.id == 226)
        CharAsIs(v892.toString.charAt(0))
      case 228 =>
        val v893 = v889.asInstanceOf[SequenceNode].children(1)
        val BindNode(v894, v895) = v893
        assert(v894.id == 229)
        CharEscaped(v895.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0))
      case 230 =>
        val v896 = v889.asInstanceOf[SequenceNode].children.head
        val BindNode(v897, v898) = v896
        assert(v897.id == 231)
        matchUnicodeChar(v898)
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

  def matchCallParams(node: Node): List[PExpr] = {
    val BindNode(v862, v863) = node
    v862.id match {
      case 369 =>
        val v865 = v863.asInstanceOf[SequenceNode].children(2)
        val BindNode(v866, v867) = v865
        assert(v866.id == 370)
        val BindNode(v868, v869) = v867
        val v864 = v868.id match {
          case 134 =>
            None
          case 371 =>
            val BindNode(v870, v871) = v869
            Some(v870.id match {
              case 372 =>
                val BindNode(v872, v873) = v871
                assert(v872.id == 373)
                val v874 = v873.asInstanceOf[SequenceNode].children.head
                val BindNode(v875, v876) = v874
                assert(v875.id == 327)
                val v877 = v873.asInstanceOf[SequenceNode].children(1)
                val v878 = unrollRepeat0(v877).map { elem =>
                  val BindNode(v879, v880) = elem
                  assert(v879.id == 376)
                  val BindNode(v881, v882) = v880
                  v881.id match {
                    case 377 =>
                      val BindNode(v883, v884) = v882
                      assert(v883.id == 378)
                      val v885 = v884.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v886, v887) = v885
                      assert(v886.id == 327)
                      matchPExpr(v887)
                  }
                }
                List(matchPExpr(v876)) ++ v878
            })
        }
        if (v864.isDefined) v864.get else List()
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

  def matchNamedConstructExpr(node: Node): NamedConstructExpr = {
    val BindNode(v195, v196) = node
    v195.id match {
      case 351 =>
        val v197 = v196.asInstanceOf[SequenceNode].children.head
        val BindNode(v198, v199) = v197
        assert(v198.id == 100)
        val v200 = v196.asInstanceOf[SequenceNode].children(3)
        val BindNode(v201, v202) = v200
        assert(v201.id == 352)
        val v203 = v196.asInstanceOf[SequenceNode].children(1)
        val BindNode(v204, v205) = v203
        assert(v204.id == 157)
        val BindNode(v206, v207) = v205
        NamedConstructExpr(matchTypeName(v199), matchNamedConstructParams(v202), v206.id match {
          case 134 =>
            None
          case 158 =>
            val BindNode(v208, v209) = v207
            Some(v208.id match {
              case 159 =>
                val BindNode(v210, v211) = v209
                assert(v210.id == 160)
                val v212 = v211.asInstanceOf[SequenceNode].children(1)
                val BindNode(v213, v214) = v212
                assert(v213.id == 121)
                matchSuperTypes(v214)
            })
        })
    }
  }

  def matchId(node: Node): String = {
    val BindNode(v100, v101) = node
    v100.id match {
      case 46 =>
        ""
    }
  }

  def matchBoolOrExpr(node: Node): BoolOrExpr = {
    val BindNode(v68, v69) = node
    v68.id match {
      case 332 =>
        val v70 = v69.asInstanceOf[SequenceNode].children.head
        val BindNode(v71, v72) = v70
        assert(v71.id == 333)
        val v73 = v69.asInstanceOf[SequenceNode].children(4)
        val BindNode(v74, v75) = v73
        assert(v74.id == 331)
        BinOp(Op.AND, matchBoolAndExpr(v72), matchBoolOrExpr(v75))
      case 434 =>
        val v76 = v69.asInstanceOf[SequenceNode].children.head
        val BindNode(v77, v78) = v76
        assert(v77.id == 333)
        matchBoolAndExpr(v78)
    }
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v779, v780) = node
    v779.id match {
      case 358 =>
        val v781 = v780.asInstanceOf[SequenceNode].children.head
        val BindNode(v782, v783) = v781
        assert(v782.id == 146)
        val v784 = v780.asInstanceOf[SequenceNode].children(1)
        val BindNode(v785, v786) = v784
        assert(v785.id == 91)
        val BindNode(v787, v788) = v786
        val v796 = v780.asInstanceOf[SequenceNode].children(5)
        val BindNode(v797, v798) = v796
        assert(v797.id == 327)
        NamedParam(matchParamName(v783), v787.id match {
          case 134 =>
            None
          case 92 =>
            val BindNode(v789, v790) = v788
            Some(v789.id match {
              case 93 =>
                val BindNode(v791, v792) = v790
                assert(v791.id == 94)
                val v793 = v792.asInstanceOf[SequenceNode].children(3)
                val BindNode(v794, v795) = v793
                assert(v794.id == 96)
                matchTypeDesc(v795)
            })
        }, matchPExpr(v798))
    }
  }

  def matchWS(node: Node): String = {
    val BindNode(v669, v670) = node
    v669.id match {
      case 5 =>
        ""
    }
  }

  def matchTypeDesc(node: Node): TypeDesc = {
    val BindNode(v676, v677) = node
    v676.id match {
      case 97 =>
        val v678 = v677.asInstanceOf[SequenceNode].children.head
        val BindNode(v679, v680) = v678
        assert(v679.id == 98)
        val v681 = v677.asInstanceOf[SequenceNode].children(1)
        val BindNode(v682, v683) = v681
        assert(v682.id == 186)
        val BindNode(v684, v685) = v683
        TypeDesc(matchNonNullTypeDesc(v680), (v684.id match {
          case 134 =>
            None
          case 187 =>
            val BindNode(v686, v687) = v685
            Some(v686.id match {
              case 188 =>
                val BindNode(v688, v689) = v687
                assert(v688.id == 189)
                val v690 = v689.asInstanceOf[SequenceNode].children(1)
                val BindNode(v691, v692) = v690
                assert(v691.id == 190)
                v692.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            })
        }).isDefined)
    }
  }

  def matchAnyType(node: Node): AnyType = {
    val BindNode(v179, v180) = node
    v179.id match {
      case 108 =>
        AnyType()
    }
  }

  def matchInPlaceChoices(node: Node): InPlaceChoices = {
    val BindNode(v427, v428) = node
    v427.id match {
      case 269 =>
        val v429 = v428.asInstanceOf[SequenceNode].children.head
        val BindNode(v430, v431) = v429
        assert(v430.id == 197)
        val v432 = v428.asInstanceOf[SequenceNode].children(1)
        val v433 = unrollRepeat0(v432).map { elem =>
          val BindNode(v434, v435) = elem
          assert(v434.id == 272)
          val BindNode(v436, v437) = v435
          v436.id match {
            case 273 =>
              val BindNode(v438, v439) = v437
              assert(v438.id == 274)
              val v440 = v439.asInstanceOf[SequenceNode].children(3)
              val BindNode(v441, v442) = v440
              assert(v441.id == 197)
              matchSequence(v442)
          }
        }
        InPlaceChoices(List(matchSequence(v431)) ++ v433)
    }
  }

  def matchSubType(node: Node): SubType = {
    val BindNode(v652, v653) = node
    v652.id match {
      case 99 =>
        val v654 = v653.asInstanceOf[SequenceNode].children.head
        val BindNode(v655, v656) = v654
        assert(v655.id == 100)
        matchTypeName(v656)
      case 118 =>
        val v657 = v653.asInstanceOf[SequenceNode].children.head
        val BindNode(v658, v659) = v657
        assert(v658.id == 119)
        matchClassDef(v659)
      case 154 =>
        val v660 = v653.asInstanceOf[SequenceNode].children.head
        val BindNode(v661, v662) = v660
        assert(v661.id == 155)
        matchSuperDef(v662)
    }
  }

  def matchParamName(node: Node): ParamName = {
    val BindNode(v830, v831) = node
    v830.id match {
      case 43 =>
        val v832 = v831.asInstanceOf[SequenceNode].children.head
        ParamName(v832.toString)
      case 89 =>
        val v833 = v831.asInstanceOf[SequenceNode].children(1)
        ParamName(v833.toString)
    }
  }

  def matchCharChar(node: Node): TerminalChar = {
    val BindNode(v133, v134) = node
    v133.id match {
      case 390 =>
        val v135 = v134.asInstanceOf[SequenceNode].children.head
        val BindNode(v136, v137) = v135
        assert(v136.id == 224)
        matchTerminalChar(v137)
    }
  }

  def matchBoolAndExpr(node: Node): BoolAndExpr = {
    val BindNode(v384, v385) = node
    v384.id match {
      case 334 =>
        val v386 = v385.asInstanceOf[SequenceNode].children.head
        val BindNode(v387, v388) = v386
        assert(v387.id == 335)
        val v389 = v385.asInstanceOf[SequenceNode].children(4)
        val BindNode(v390, v391) = v389
        assert(v390.id == 333)
        BinOp(Op.OR, matchBoolEqExpr(v388), matchBoolAndExpr(v391))
      case 431 =>
        val v392 = v385.asInstanceOf[SequenceNode].children.head
        val BindNode(v393, v394) = v392
        assert(v393.id == 335)
        matchBoolEqExpr(v394)
    }
  }

  def matchTernaryExpr(node: Node): TernaryExpr = {
    val BindNode(v693, v694) = node
    v693.id match {
      case 330 =>
        val v695 = v694.asInstanceOf[SequenceNode].children.head
        val BindNode(v696, v697) = v695
        assert(v696.id == 331)
        val v698 = v694.asInstanceOf[SequenceNode].children(4)
        val BindNode(v699, v700) = v698
        assert(v699.id == 435)
        val BindNode(v701, v702) = v700
        assert(v701.id == 436)
        val BindNode(v703, v704) = v702
        val v710 = v694.asInstanceOf[SequenceNode].children(8)
        val BindNode(v711, v712) = v710
        assert(v711.id == 435)
        val BindNode(v713, v714) = v712
        assert(v713.id == 436)
        val BindNode(v715, v716) = v714
        TernaryOp(matchBoolOrExpr(v697), v703.id match {
          case 437 =>
            val BindNode(v705, v706) = v704
            assert(v705.id == 328)
            val v707 = v706.asInstanceOf[SequenceNode].children.head
            val BindNode(v708, v709) = v707
            assert(v708.id == 329)
            matchTernaryExpr(v709)
        }, v715.id match {
          case 437 =>
            val BindNode(v717, v718) = v716
            assert(v717.id == 328)
            val v719 = v718.asInstanceOf[SequenceNode].children.head
            val BindNode(v720, v721) = v719
            assert(v720.id == 329)
            matchTernaryExpr(v721)
        })
      case 438 =>
        val v722 = v694.asInstanceOf[SequenceNode].children.head
        val BindNode(v723, v724) = v722
        assert(v723.id == 331)
        matchBoolOrExpr(v724)
    }
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v215, v216) = node
    v215.id match {
      case 120 =>
        val v217 = v216.asInstanceOf[SequenceNode].children.head
        val BindNode(v218, v219) = v217
        assert(v218.id == 100)
        val v220 = v216.asInstanceOf[SequenceNode].children(2)
        val BindNode(v221, v222) = v220
        assert(v221.id == 121)
        AbstractClassDef(matchTypeName(v219), matchSuperTypes(v222))
      case 136 =>
        val v223 = v216.asInstanceOf[SequenceNode].children.head
        val BindNode(v224, v225) = v223
        assert(v224.id == 100)
        val v226 = v216.asInstanceOf[SequenceNode].children(2)
        val BindNode(v227, v228) = v226
        assert(v227.id == 137)
        ConcreteClassDef(matchTypeName(v225), List(), matchClassParamsDef(v228))
      case 153 =>
        val v229 = v216.asInstanceOf[SequenceNode].children.head
        val BindNode(v230, v231) = v229
        assert(v230.id == 100)
        val v232 = v216.asInstanceOf[SequenceNode].children(2)
        val BindNode(v233, v234) = v232
        assert(v233.id == 121)
        val v235 = v216.asInstanceOf[SequenceNode].children(4)
        val BindNode(v236, v237) = v235
        assert(v236.id == 137)
        ConcreteClassDef(matchTypeName(v231), matchSuperTypes(v234), matchClassParamsDef(v237))
    }
  }

  def matchElem(node: Node): Elem = {
    val BindNode(v614, v615) = node
    v614.id match {
      case 200 =>
        val v616 = v615.asInstanceOf[SequenceNode].children.head
        val BindNode(v617, v618) = v616
        assert(v617.id == 201)
        matchSymbol(v618)
      case 285 =>
        val v619 = v615.asInstanceOf[SequenceNode].children.head
        val BindNode(v620, v621) = v619
        assert(v620.id == 286)
        matchProcessor(v621)
    }
  }

  def matchAllRawRef(node: Node): AllRawRef = {
    val BindNode(v754, v755) = node
    v754.id match {
      case 323 =>
        AllRawRef()
    }
  }

  def matchValueType(node: Node): ValueType = {
    val BindNode(v193, v194) = node
    v193.id match {
      case 56 =>
        BooleanType()
      case 65 =>
        CharType()
      case 71 =>
        StringType()
    }
  }

  def matchSubTypes(node: Node): List[SubType] = {
    val BindNode(v509, v510) = node
    v509.id match {
      case 167 =>
        val v511 = v510.asInstanceOf[SequenceNode].children.head
        val BindNode(v512, v513) = v511
        assert(v512.id == 168)
        val v514 = v510.asInstanceOf[SequenceNode].children(1)
        val v515 = unrollRepeat0(v514).map { elem =>
          val BindNode(v516, v517) = elem
          assert(v516.id == 171)
          val BindNode(v518, v519) = v517
          v518.id match {
            case 172 =>
              val BindNode(v520, v521) = v519
              assert(v520.id == 173)
              val v522 = v521.asInstanceOf[SequenceNode].children(3)
              val BindNode(v523, v524) = v522
              assert(v523.id == 168)
              matchSubType(v524)
          }
        }
        List(matchSubType(v513)) ++ v515
    }
  }

  def matchLHS(node: Node): LHS = {
    val BindNode(v367, v368) = node
    v367.id match {
      case 39 =>
        val v369 = v368.asInstanceOf[SequenceNode].children.head
        val BindNode(v370, v371) = v369
        assert(v370.id == 40)
        val v372 = v368.asInstanceOf[SequenceNode].children(1)
        val BindNode(v373, v374) = v372
        assert(v373.id == 91)
        val BindNode(v375, v376) = v374
        LHS(matchNonterminal(v371), v375.id match {
          case 134 =>
            None
          case 92 =>
            val BindNode(v377, v378) = v376
            Some(v377.id match {
              case 93 =>
                val BindNode(v379, v380) = v378
                assert(v379.id == 94)
                val v381 = v380.asInstanceOf[SequenceNode].children(3)
                val BindNode(v382, v383) = v381
                assert(v382.id == 96)
                matchTypeDesc(v383)
            })
        })
    }
  }

  def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
    val BindNode(v304, v305) = node
    v304.id match {
      case 249 =>
        val v306 = v305.asInstanceOf[SequenceNode].children.head
        val BindNode(v307, v308) = v306
        assert(v307.id == 241)
        val v309 = v305.asInstanceOf[SequenceNode].children(2)
        val BindNode(v310, v311) = v309
        assert(v310.id == 241)
        TerminalChoiceRange(matchTerminalChoiceChar(v308), matchTerminalChoiceChar(v311))
    }
  }

  def matchUnicodeChar(node: Node): CharUnicode = {
    val BindNode(v335, v336) = node
    v335.id match {
      case 232 =>
        val v337 = v336.asInstanceOf[SequenceNode].children(2)
        val BindNode(v338, v339) = v337
        assert(v338.id == 233)
        val v340 = v336.asInstanceOf[SequenceNode].children(3)
        val BindNode(v341, v342) = v340
        assert(v341.id == 233)
        val v343 = v336.asInstanceOf[SequenceNode].children(4)
        val BindNode(v344, v345) = v343
        assert(v344.id == 233)
        val v346 = v336.asInstanceOf[SequenceNode].children(5)
        val BindNode(v347, v348) = v346
        assert(v347.id == 233)
        CharUnicode(List(v339.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0), v342.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0), v345.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0), v348.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0)))
    }
  }

  def matchCondSymPath(node: Node): List[CondSymDir.Value] = {
    val BindNode(v261, v262) = node
    v261.id match {
      case 295 =>
        val v263 = v262.asInstanceOf[SequenceNode].children.head
        val v264 = unrollRepeat1(v263).map { elem =>
          val BindNode(v265, v266) = elem
          assert(v265.id == 297)
          val BindNode(v267, v268) = v266
          v267.id match {
            case 298 =>
              val BindNode(v269, v270) = v268
              assert(v269.id == 299)
              CondSymDir.BODY
            case 300 =>
              val BindNode(v271, v272) = v268
              assert(v271.id == 301)
              CondSymDir.COND
          }
        }
        v264
    }
  }

  def matchNonterminalName(node: Node): NonterminalName = {
    val BindNode(v730, v731) = node
    v730.id match {
      case 43 =>
        val v732 = v731.asInstanceOf[SequenceNode].children.head
        NonterminalName(v732.toString)
      case 89 =>
        val v733 = v731.asInstanceOf[SequenceNode].children(1)
        NonterminalName(v733.toString)
    }
  }

  def matchPrefixNotExpr(node: Node): PrefixNotExpr = {
    val BindNode(v395, v396) = node
    v395.id match {
      case 342 =>
        val v397 = v396.asInstanceOf[SequenceNode].children(2)
        val BindNode(v398, v399) = v397
        assert(v398.id == 341)
        PrefixOp(PreOp.NOT, matchPrefixNotExpr(v399))
      case 343 =>
        val v400 = v396.asInstanceOf[SequenceNode].children.head
        val BindNode(v401, v402) = v400
        assert(v401.id == 344)
        matchAtom(v402)
    }
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v349, v350) = node
    v349.id match {
      case 86 =>
        NullLiteral()
      case 384 =>
        val v351 = v350.asInstanceOf[SequenceNode].children.head
        val BindNode(v352, v353) = v351
        assert(v352.id == 385)
        val BindNode(v354, v355) = v353
        BoolLiteral(v354.id match {
          case 386 =>
            val BindNode(v356, v357) = v355
            assert(v356.id == 78)
            true
          case 387 =>
            val BindNode(v358, v359) = v355
            assert(v358.id == 82)
            false
        })
      case 388 =>
        val v360 = v350.asInstanceOf[SequenceNode].children(1)
        val BindNode(v361, v362) = v360
        assert(v361.id == 389)
        CharLiteral(matchCharChar(v362))
      case 391 =>
        val v363 = v350.asInstanceOf[SequenceNode].children(1)
        val v364 = unrollRepeat0(v363).map { elem =>
          val BindNode(v365, v366) = elem
          assert(v365.id == 394)
          matchStrChar(v366)
        }
        StrLiteral(v364)
    }
  }

  def matchEmptySequence(node: Node): EmptySeq = {
    val BindNode(v90, v91) = node
    v90.id match {
      case 281 =>
        EmptySeq()
    }
  }

  def matchElvisExpr(node: Node): ElvisExpr = {
    val BindNode(v935, v936) = node
    v935.id match {
      case 338 =>
        val v937 = v936.asInstanceOf[SequenceNode].children.head
        val BindNode(v938, v939) = v937
        assert(v938.id == 339)
        val v940 = v936.asInstanceOf[SequenceNode].children(4)
        val BindNode(v941, v942) = v940
        assert(v941.id == 337)
        Elvis(matchAdditiveExpr(v939), matchElvisExpr(v942))
      case 418 =>
        val v943 = v936.asInstanceOf[SequenceNode].children.head
        val BindNode(v944, v945) = v943
        assert(v944.id == 339)
        matchAdditiveExpr(v945)
    }
  }

  def matchWSNL(node: Node): String = {
    val BindNode(v333, v334) = node
    v333.id match {
      case 455 =>
        ""
    }
  }

  def matchRHS(node: Node): Sequence = {
    val BindNode(v645, v646) = node
    v645.id match {
      case 196 =>
        val v647 = v646.asInstanceOf[SequenceNode].children.head
        val BindNode(v648, v649) = v647
        assert(v648.id == 197)
        matchSequence(v649)
    }
  }

  def matchCanonicalEnumValue(node: Node): CanonicalEnumValue = {
    val BindNode(v92, v93) = node
    v92.id match {
      case 404 =>
        val v94 = v93.asInstanceOf[SequenceNode].children.head
        val BindNode(v95, v96) = v94
        assert(v95.id == 113)
        val v97 = v93.asInstanceOf[SequenceNode].children(2)
        val BindNode(v98, v99) = v97
        assert(v98.id == 405)
        CanonicalEnumValue(matchEnumTypeName(v96), matchEnumValueName(v99))
    }
  }

  def matchEnumTypeName(node: Node): EnumTypeName = {
    val BindNode(v590, v591) = node
    v590.id match {
      case 114 =>
        val v592 = v591.asInstanceOf[SequenceNode].children(1)
        val BindNode(v593, v594) = v592
        assert(v593.id == 45)
        EnumTypeName(matchId(v594).toString)
    }
  }

  def matchLineComment(node: Node): String = {
    val BindNode(v177, v178) = node
    v177.id match {
      case 16 =>
        ""
    }
  }

  def matchBinSymbol(node: Node): BinSymbol = {
    val BindNode(v845, v846) = node
    v845.id match {
      case 204 =>
        val v847 = v846.asInstanceOf[SequenceNode].children.head
        val BindNode(v848, v849) = v847
        assert(v848.id == 203)
        val v850 = v846.asInstanceOf[SequenceNode].children(4)
        val BindNode(v851, v852) = v850
        assert(v851.id == 206)
        JoinSymbol(matchBinSymbol(v849), matchPreUnSymbol(v852))
      case 283 =>
        val v853 = v846.asInstanceOf[SequenceNode].children.head
        val BindNode(v854, v855) = v853
        assert(v854.id == 203)
        val v856 = v846.asInstanceOf[SequenceNode].children(4)
        val BindNode(v857, v858) = v856
        assert(v857.id == 206)
        ExceptSymbol(matchBinSymbol(v855), matchPreUnSymbol(v858))
      case 284 =>
        val v859 = v846.asInstanceOf[SequenceNode].children.head
        val BindNode(v860, v861) = v859
        assert(v860.id == 206)
        matchPreUnSymbol(v861)
    }
  }

  def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
    val BindNode(v579, v580) = node
    v579.id match {
      case 242 =>
        val v581 = v580.asInstanceOf[SequenceNode].children.head
        val BindNode(v582, v583) = v581
        assert(v582.id == 243)
        CharAsIs(v583.toString.charAt(0))
      case 245 =>
        val v584 = v580.asInstanceOf[SequenceNode].children(1)
        val BindNode(v585, v586) = v584
        assert(v585.id == 246)
        CharEscaped(v586.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString.charAt(0))
      case 230 =>
        val v587 = v580.asInstanceOf[SequenceNode].children.head
        val BindNode(v588, v589) = v587
        assert(v588.id == 231)
        matchUnicodeChar(v589)
    }
  }

  def matchFuncCallOrConstructExpr(node: Node): FuncCallOrConstructExpr = {
    val BindNode(v403, v404) = node
    v403.id match {
      case 366 =>
        val v405 = v404.asInstanceOf[SequenceNode].children.head
        val BindNode(v406, v407) = v405
        assert(v406.id == 367)
        val v408 = v404.asInstanceOf[SequenceNode].children(2)
        val BindNode(v409, v410) = v408
        assert(v409.id == 368)
        FuncCallOrConstructExpr(matchTypeOrFuncName(v407), matchCallParams(v410))
    }
  }

  def matchRefIdx(node: Node): String = {
    val BindNode(v525, v526) = node
    v525.id match {
      case 304 =>
        val v527 = v526.asInstanceOf[SequenceNode].children.head
        val BindNode(v528, v529) = v527
        assert(v528.id == 305)
        val BindNode(v530, v531) = v529
        assert(v530.id == 306)
        val BindNode(v532, v533) = v531
        (v532.id match {
          case 307 =>
            val BindNode(v534, v535) = v533
            assert(v534.id == 308)
            val v536 = v535.asInstanceOf[SequenceNode].children.head
            val BindNode(v537, v538) = v536
            assert(v537.id == 309)
            v538.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
          case 310 =>
            val BindNode(v539, v540) = v533
            assert(v539.id == 311)
            val v541 = v540.asInstanceOf[SequenceNode].children(1)
            val v542 = unrollRepeat0(v541).map { elem =>
              val BindNode(v543, v544) = elem
              assert(v543.id == 315)
              v544.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            v542
        }).toString
    }
  }

  def matchAtomSymbol(node: Node): AtomSymbol = {
    val BindNode(v622, v623) = node
    v622.id match {
      case 266 =>
        val v624 = v623.asInstanceOf[SequenceNode].children.head
        val BindNode(v625, v626) = v624
        assert(v625.id == 40)
        matchNonterminal(v626)
      case 276 =>
        val v627 = v623.asInstanceOf[SequenceNode].children.head
        val BindNode(v628, v629) = v627
        assert(v628.id == 277)
        matchLongest(v629)
      case 254 =>
        val v630 = v623.asInstanceOf[SequenceNode].children.head
        val BindNode(v631, v632) = v630
        assert(v631.id == 255)
        matchStringSymbol(v632)
      case 220 =>
        val v633 = v623.asInstanceOf[SequenceNode].children.head
        val BindNode(v634, v635) = v633
        assert(v634.id == 221)
        matchTerminal(v635)
      case 279 =>
        val v636 = v623.asInstanceOf[SequenceNode].children.head
        val BindNode(v637, v638) = v636
        assert(v637.id == 280)
        matchEmptySequence(v638)
      case 267 =>
        val v639 = v623.asInstanceOf[SequenceNode].children(2)
        val BindNode(v640, v641) = v639
        assert(v640.id == 268)
        matchInPlaceChoices(v641)
      case 236 =>
        val v642 = v623.asInstanceOf[SequenceNode].children.head
        val BindNode(v643, v644) = v642
        assert(v643.id == 237)
        matchTerminalChoice(v644)
    }
  }

  def matchNamedConstructParams(node: Node): List[NamedParam] = {
    val BindNode(v238, v239) = node
    v238.id match {
      case 353 =>
        val v240 = v239.asInstanceOf[SequenceNode].children(2)
        val BindNode(v241, v242) = v240
        assert(v241.id == 354)
        val BindNode(v243, v244) = v242
        v243.id match {
          case 355 =>
            val BindNode(v245, v246) = v244
            assert(v245.id == 356)
            val v247 = v246.asInstanceOf[SequenceNode].children.head
            val BindNode(v248, v249) = v247
            assert(v248.id == 357)
            val v250 = v246.asInstanceOf[SequenceNode].children(1)
            val v251 = unrollRepeat0(v250).map { elem =>
              val BindNode(v252, v253) = elem
              assert(v252.id == 361)
              val BindNode(v254, v255) = v253
              v254.id match {
                case 362 =>
                  val BindNode(v256, v257) = v255
                  assert(v256.id == 363)
                  val v258 = v257.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v259, v260) = v258
                  assert(v259.id == 357)
                  matchNamedParam(v260)
              }
            }
            List(matchNamedParam(v249)) ++ v251
        }
    }
  }

  def matchStringSymbol(node: Node): StringSymbol = {
    val BindNode(v663, v664) = node
    v663.id match {
      case 256 =>
        val v665 = v664.asInstanceOf[SequenceNode].children(1)
        val v666 = unrollRepeat0(v665).map { elem =>
          val BindNode(v667, v668) = elem
          assert(v667.id == 260)
          matchStringChar(v668)
        }
        StringSymbol(v666)
    }
  }

  def matchArrayExpr(node: Node): ArrayExpr = {
    val BindNode(v278, v279) = node
    v278.id match {
      case 381 =>
        val v281 = v279.asInstanceOf[SequenceNode].children(2)
        val BindNode(v282, v283) = v281
        assert(v282.id == 370)
        val BindNode(v284, v285) = v283
        val v280 = v284.id match {
          case 134 =>
            None
          case 371 =>
            val BindNode(v286, v287) = v285
            assert(v286.id == 372)
            val BindNode(v288, v289) = v287
            assert(v288.id == 373)
            val v290 = v289.asInstanceOf[SequenceNode].children.head
            val BindNode(v291, v292) = v290
            assert(v291.id == 327)
            val v293 = v289.asInstanceOf[SequenceNode].children(1)
            val v294 = unrollRepeat0(v293).map { elem =>
              val BindNode(v295, v296) = elem
              assert(v295.id == 376)
              val BindNode(v297, v298) = v296
              v297.id match {
                case 377 =>
                  val BindNode(v299, v300) = v298
                  assert(v299.id == 378)
                  val v301 = v300.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v302, v303) = v301
                  assert(v302.id == 327)
                  matchPExpr(v303)
              }
            }
            Some(List(matchPExpr(v292)) ++ v294)
        }
        ArrayExpr(if (v280.isDefined) v280.get else List())
    }
  }

  def matchEnumValue(node: Node): AbstractEnumValue = {
    val BindNode(v595, v596) = node
    v595.id match {
      case 398 =>
        val v597 = v596.asInstanceOf[SequenceNode].children.head
        val BindNode(v598, v599) = v597
        assert(v598.id == 399)
        val BindNode(v600, v601) = v599
        assert(v600.id == 400)
        val BindNode(v602, v603) = v601
        v602.id match {
          case 401 =>
            val BindNode(v604, v605) = v603
            assert(v604.id == 402)
            val v606 = v605.asInstanceOf[SequenceNode].children.head
            val BindNode(v607, v608) = v606
            assert(v607.id == 403)
            matchCanonicalEnumValue(v608)
          case 407 =>
            val BindNode(v609, v610) = v603
            assert(v609.id == 408)
            val v611 = v610.asInstanceOf[SequenceNode].children.head
            val BindNode(v612, v613) = v611
            assert(v612.id == 409)
            matchShortenedEnumValue(v613)
        }
    }
  }

  def matchSymbol(node: Node): Symbol = {
    val BindNode(v671, v672) = node
    v671.id match {
      case 202 =>
        val v673 = v672.asInstanceOf[SequenceNode].children.head
        val BindNode(v674, v675) = v673
        assert(v674.id == 203)
        matchBinSymbol(v675)
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

  def parseAst(text: String): Either[Grammar, ParsingErrors.ParsingError] = parse(text) match {
    case Left(ctx) =>
      val tree = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
      tree match {
        case Some(forest) if forest.trees.size == 1 => Left(matchStart(forest.trees.head))
        case Some(forest) => Right(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size))
        case None => ???
      }
    case Right(error) => Right(error)
  }

  def main(args: Array[String]): Unit = {
    val grammar = parseAst("A = B C D")
    println(grammar)
  }
}
