package com.giyeok.jparser.metalang3a.generated

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseResultTree.BindNode
import com.giyeok.jparser.ParseResultTree.JoinNode
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

object Proto3DefinitionAst {
  val ngrammar = new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
      2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("proto3"), Set(3)),
      4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("WS"), Set(5)),
      6 -> NGrammar.NRepeat(6, Symbols.Repeat(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet), 0), 7, 8),
      9 -> NGrammar.NTerminal(9, Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)),
      10 -> NGrammar.NNonterminal(10, Symbols.Nonterminal("syntax"), Set(11)),
      12 -> NGrammar.NJoin(12, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('y'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('a'), Symbols.ExactChar('x')))), Symbols.Nonterminal("Tk")), 13, 21),
      13 -> NGrammar.NProxy(13, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('y'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('a'), Symbols.ExactChar('x')))), 14),
      15 -> NGrammar.NTerminal(15, Symbols.ExactChar('s')),
      16 -> NGrammar.NTerminal(16, Symbols.ExactChar('y')),
      17 -> NGrammar.NTerminal(17, Symbols.ExactChar('n')),
      18 -> NGrammar.NTerminal(18, Symbols.ExactChar('t')),
      19 -> NGrammar.NTerminal(19, Symbols.ExactChar('a')),
      20 -> NGrammar.NTerminal(20, Symbols.ExactChar('x')),
      21 -> NGrammar.NNonterminal(21, Symbols.Nonterminal("Tk"), Set(22)),
      23 -> NGrammar.NLongest(23, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))), 24),
      24 -> NGrammar.NOneOf(24, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))))), Set(25)),
      25 -> NGrammar.NProxy(25, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))), 26),
      27 -> NGrammar.NRepeat(27, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), 7, 28),
      29 -> NGrammar.NTerminal(29, Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
      30 -> NGrammar.NTerminal(30, Symbols.ExactChar('=')),
      31 -> NGrammar.NOneOf(31, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('"'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('\''))))))))), Set(32, 41)),
      32 -> NGrammar.NProxy(32, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('"'))))))), 33),
      34 -> NGrammar.NProxy(34, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('"')))), 35),
      36 -> NGrammar.NTerminal(36, Symbols.ExactChar('"')),
      37 -> NGrammar.NTerminal(37, Symbols.ExactChar('p')),
      38 -> NGrammar.NTerminal(38, Symbols.ExactChar('r')),
      39 -> NGrammar.NTerminal(39, Symbols.ExactChar('o')),
      40 -> NGrammar.NTerminal(40, Symbols.ExactChar('3')),
      41 -> NGrammar.NProxy(41, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('\''))))))), 42),
      43 -> NGrammar.NProxy(43, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('\'')))), 44),
      45 -> NGrammar.NTerminal(45, Symbols.ExactChar('\'')),
      46 -> NGrammar.NTerminal(46, Symbols.ExactChar(';')),
      47 -> NGrammar.NRepeat(47, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("import")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("package")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("topLevelDef")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), 7, 48),
      49 -> NGrammar.NOneOf(49, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("import")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("package")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("topLevelDef")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), Set(50)),
      50 -> NGrammar.NProxy(50, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("import")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("package")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("topLevelDef")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))), 51),
      52 -> NGrammar.NOneOf(52, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("import")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("package")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("topLevelDef")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement")))))), Set(53, 110, 148, 237, 398)),
      53 -> NGrammar.NProxy(53, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("import")))), 54),
      55 -> NGrammar.NNonterminal(55, Symbols.Nonterminal("import"), Set(56)),
      57 -> NGrammar.NJoin(57, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('m'), Symbols.ExactChar('p'), Symbols.ExactChar('o'), Symbols.ExactChar('r'), Symbols.ExactChar('t')))), Symbols.Nonterminal("Tk")), 58, 21),
      58 -> NGrammar.NProxy(58, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('m'), Symbols.ExactChar('p'), Symbols.ExactChar('o'), Symbols.ExactChar('r'), Symbols.ExactChar('t')))), 59),
      60 -> NGrammar.NTerminal(60, Symbols.ExactChar('i')),
      61 -> NGrammar.NTerminal(61, Symbols.ExactChar('m')),
      62 -> NGrammar.NOneOf(62, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('w'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('k')))), Symbols.Nonterminal("Tk"))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('i'), Symbols.ExactChar('c')))), Symbols.Nonterminal("Tk"))))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(63, 81)),
      63 -> NGrammar.NOneOf(63, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('w'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('k')))), Symbols.Nonterminal("Tk"))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('i'), Symbols.ExactChar('c')))), Symbols.Nonterminal("Tk"))))))), Set(64, 72)),
      64 -> NGrammar.NProxy(64, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('w'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('k')))), Symbols.Nonterminal("Tk"))))), 65),
      66 -> NGrammar.NJoin(66, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('w'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('k')))), Symbols.Nonterminal("Tk")), 67, 21),
      67 -> NGrammar.NProxy(67, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('w'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('k')))), 68),
      69 -> NGrammar.NTerminal(69, Symbols.ExactChar('w')),
      70 -> NGrammar.NTerminal(70, Symbols.ExactChar('e')),
      71 -> NGrammar.NTerminal(71, Symbols.ExactChar('k')),
      72 -> NGrammar.NProxy(72, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('i'), Symbols.ExactChar('c')))), Symbols.Nonterminal("Tk"))))), 73),
      74 -> NGrammar.NJoin(74, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('i'), Symbols.ExactChar('c')))), Symbols.Nonterminal("Tk")), 75, 21),
      75 -> NGrammar.NProxy(75, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('i'), Symbols.ExactChar('c')))), 76),
      77 -> NGrammar.NTerminal(77, Symbols.ExactChar('u')),
      78 -> NGrammar.NTerminal(78, Symbols.ExactChar('b')),
      79 -> NGrammar.NTerminal(79, Symbols.ExactChar('l')),
      80 -> NGrammar.NTerminal(80, Symbols.ExactChar('c')),
      81 -> NGrammar.NProxy(81, Symbols.Proxy(Symbols.Sequence(Seq())), 7),
      82 -> NGrammar.NNonterminal(82, Symbols.Nonterminal("strLit"), Set(83, 109)),
      84 -> NGrammar.NRepeat(84, Symbols.Repeat(Symbols.Nonterminal("charValue"), 0), 7, 85),
      86 -> NGrammar.NNonterminal(86, Symbols.Nonterminal("charValue"), Set(87, 95, 101, 105)),
      88 -> NGrammar.NNonterminal(88, Symbols.Nonterminal("hexEscape"), Set(89)),
      90 -> NGrammar.NTerminal(90, Symbols.ExactChar('\\')),
      91 -> NGrammar.NTerminal(91, Symbols.Chars(Set('X', 'x'))),
      92 -> NGrammar.NNonterminal(92, Symbols.Nonterminal("hexDigit"), Set(93)),
      94 -> NGrammar.NTerminal(94, Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet)),
      96 -> NGrammar.NNonterminal(96, Symbols.Nonterminal("octEscape"), Set(97)),
      98 -> NGrammar.NNonterminal(98, Symbols.Nonterminal("octalDigit"), Set(99)),
      100 -> NGrammar.NTerminal(100, Symbols.Chars(('0' to '7').toSet)),
      102 -> NGrammar.NNonterminal(102, Symbols.Nonterminal("charEscape"), Set(103)),
      104 -> NGrammar.NTerminal(104, Symbols.Chars(Set('"', '\'', '\\', 'f', 'n', 'r', 't', 'v') ++ ('a' to 'b').toSet)),
      106 -> NGrammar.NExcept(106, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\n', '\\'))), 107, 108),
      107 -> NGrammar.NTerminal(107, Symbols.AnyChar),
      108 -> NGrammar.NTerminal(108, Symbols.Chars(Set('\n', '\\'))),
      110 -> NGrammar.NProxy(110, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("package")))), 111),
      112 -> NGrammar.NNonterminal(112, Symbols.Nonterminal("package"), Set(113)),
      114 -> NGrammar.NJoin(114, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('a'), Symbols.ExactChar('c'), Symbols.ExactChar('k'), Symbols.ExactChar('a'), Symbols.ExactChar('g'), Symbols.ExactChar('e')))), Symbols.Nonterminal("Tk")), 115, 21),
      115 -> NGrammar.NProxy(115, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('a'), Symbols.ExactChar('c'), Symbols.ExactChar('k'), Symbols.ExactChar('a'), Symbols.ExactChar('g'), Symbols.ExactChar('e')))), 116),
      117 -> NGrammar.NTerminal(117, Symbols.ExactChar('g')),
      118 -> NGrammar.NNonterminal(118, Symbols.Nonterminal("fullIdent"), Set(119)),
      120 -> NGrammar.NNonterminal(120, Symbols.Nonterminal("ident"), Set(121)),
      122 -> NGrammar.NLongest(122, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalDigit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('_')))))), 0))))))), 123),
      123 -> NGrammar.NOneOf(123, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalDigit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('_')))))), 0)))))), Set(124)),
      124 -> NGrammar.NProxy(124, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalDigit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('_')))))), 0)))), 125),
      126 -> NGrammar.NNonterminal(126, Symbols.Nonterminal("letter"), Set(127)),
      128 -> NGrammar.NTerminal(128, Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
      129 -> NGrammar.NRepeat(129, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalDigit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('_')))))), 0), 7, 130),
      131 -> NGrammar.NOneOf(131, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalDigit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('_')))))), Set(132, 134, 139)),
      132 -> NGrammar.NProxy(132, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter")))), 133),
      134 -> NGrammar.NProxy(134, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalDigit")))), 135),
      136 -> NGrammar.NNonterminal(136, Symbols.Nonterminal("decimalDigit"), Set(137)),
      138 -> NGrammar.NTerminal(138, Symbols.Chars(('0' to '9').toSet)),
      139 -> NGrammar.NProxy(139, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('_')))), 140),
      141 -> NGrammar.NTerminal(141, Symbols.ExactChar('_')),
      142 -> NGrammar.NRepeat(142, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))))), 0), 7, 143),
      144 -> NGrammar.NOneOf(144, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))))), Set(145)),
      145 -> NGrammar.NProxy(145, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))), 146),
      147 -> NGrammar.NTerminal(147, Symbols.ExactChar('.')),
      148 -> NGrammar.NProxy(148, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), 149),
      150 -> NGrammar.NNonterminal(150, Symbols.Nonterminal("option"), Set(151)),
      152 -> NGrammar.NJoin(152, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('o'), Symbols.ExactChar('p'), Symbols.ExactChar('t'), Symbols.ExactChar('i'), Symbols.ExactChar('o'), Symbols.ExactChar('n')))), Symbols.Nonterminal("Tk")), 153, 21),
      153 -> NGrammar.NProxy(153, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('o'), Symbols.ExactChar('p'), Symbols.ExactChar('t'), Symbols.ExactChar('i'), Symbols.ExactChar('o'), Symbols.ExactChar('n')))), 154),
      155 -> NGrammar.NNonterminal(155, Symbols.Nonterminal("optionName"), Set(156)),
      157 -> NGrammar.NOneOf(157, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ident")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fullIdent"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')')))))), Set(158, 160)),
      158 -> NGrammar.NProxy(158, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ident")))), 159),
      160 -> NGrammar.NProxy(160, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fullIdent"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')')))), 161),
      162 -> NGrammar.NTerminal(162, Symbols.ExactChar('(')),
      163 -> NGrammar.NTerminal(163, Symbols.ExactChar(')')),
      164 -> NGrammar.NRepeat(164, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))))), 0), 7, 165),
      166 -> NGrammar.NOneOf(166, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))))), Set(167)),
      167 -> NGrammar.NProxy(167, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))), 168),
      169 -> NGrammar.NNonterminal(169, Symbols.Nonterminal("constant"), Set(170, 180, 215, 235, 236)),
      171 -> NGrammar.NExcept(171, Symbols.Except(Symbols.Nonterminal("fullIdent"), Symbols.Nonterminal("boolLit")), 118, 172),
      172 -> NGrammar.NNonterminal(172, Symbols.Nonterminal("boolLit"), Set(173, 176)),
      174 -> NGrammar.NProxy(174, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e')))), 175),
      177 -> NGrammar.NProxy(177, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e')))), 178),
      179 -> NGrammar.NTerminal(179, Symbols.ExactChar('f')),
      181 -> NGrammar.NOneOf(181, Symbols.OneOf(ListSet(Symbols.Nonterminal("sign"), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(182, 81)),
      182 -> NGrammar.NNonterminal(182, Symbols.Nonterminal("sign"), Set(183, 185)),
      184 -> NGrammar.NTerminal(184, Symbols.ExactChar('+')),
      186 -> NGrammar.NTerminal(186, Symbols.ExactChar('-')),
      187 -> NGrammar.NNonterminal(187, Symbols.Nonterminal("intLit"), Set(188)),
      189 -> NGrammar.NLongest(189, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("zeroLit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalLit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("octalLit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("hexLit"))))))), 190),
      190 -> NGrammar.NOneOf(190, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("zeroLit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalLit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("octalLit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("hexLit")))))), Set(191, 196, 203, 209)),
      191 -> NGrammar.NProxy(191, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("zeroLit")))), 192),
      193 -> NGrammar.NNonterminal(193, Symbols.Nonterminal("zeroLit"), Set(194)),
      195 -> NGrammar.NTerminal(195, Symbols.ExactChar('0')),
      196 -> NGrammar.NProxy(196, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalLit")))), 197),
      198 -> NGrammar.NNonterminal(198, Symbols.Nonterminal("decimalLit"), Set(199)),
      200 -> NGrammar.NTerminal(200, Symbols.Chars(('1' to '9').toSet)),
      201 -> NGrammar.NRepeat(201, Symbols.Repeat(Symbols.Nonterminal("decimalDigit"), 0), 7, 202),
      203 -> NGrammar.NProxy(203, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("octalLit")))), 204),
      205 -> NGrammar.NNonterminal(205, Symbols.Nonterminal("octalLit"), Set(206)),
      207 -> NGrammar.NRepeat(207, Symbols.Repeat(Symbols.Nonterminal("octalDigit"), 1), 98, 208),
      209 -> NGrammar.NProxy(209, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("hexLit")))), 210),
      211 -> NGrammar.NNonterminal(211, Symbols.Nonterminal("hexLit"), Set(212)),
      213 -> NGrammar.NRepeat(213, Symbols.Repeat(Symbols.Nonterminal("hexDigit"), 1), 92, 214),
      216 -> NGrammar.NNonterminal(216, Symbols.Nonterminal("floatLit"), Set(217, 227, 228, 229, 232)),
      218 -> NGrammar.NNonterminal(218, Symbols.Nonterminal("decimals"), Set(219)),
      220 -> NGrammar.NRepeat(220, Symbols.Repeat(Symbols.Nonterminal("decimalDigit"), 1), 136, 221),
      222 -> NGrammar.NOneOf(222, Symbols.OneOf(ListSet(Symbols.Nonterminal("decimals"), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(218, 81)),
      223 -> NGrammar.NOneOf(223, Symbols.OneOf(ListSet(Symbols.Nonterminal("exponent"), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(224, 81)),
      224 -> NGrammar.NNonterminal(224, Symbols.Nonterminal("exponent"), Set(225)),
      226 -> NGrammar.NTerminal(226, Symbols.Chars(Set('E', 'e'))),
      230 -> NGrammar.NProxy(230, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('f')))), 231),
      233 -> NGrammar.NProxy(233, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('a'), Symbols.ExactChar('n')))), 234),
      237 -> NGrammar.NProxy(237, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("topLevelDef")))), 238),
      239 -> NGrammar.NNonterminal(239, Symbols.Nonterminal("topLevelDef"), Set(240, 365, 473)),
      241 -> NGrammar.NNonterminal(241, Symbols.Nonterminal("message"), Set(242)),
      243 -> NGrammar.NJoin(243, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('e'), Symbols.ExactChar('s'), Symbols.ExactChar('s'), Symbols.ExactChar('a'), Symbols.ExactChar('g'), Symbols.ExactChar('e')))), Symbols.Nonterminal("Tk")), 244, 21),
      244 -> NGrammar.NProxy(244, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('e'), Symbols.ExactChar('s'), Symbols.ExactChar('s'), Symbols.ExactChar('a'), Symbols.ExactChar('g'), Symbols.ExactChar('e')))), 245),
      246 -> NGrammar.NNonterminal(246, Symbols.Nonterminal("messageName"), Set(159)),
      247 -> NGrammar.NNonterminal(247, Symbols.Nonterminal("messageBody"), Set(248)),
      249 -> NGrammar.NTerminal(249, Symbols.ExactChar('{')),
      250 -> NGrammar.NRepeat(250, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("messageBodyElem")))))), 0), 7, 251),
      252 -> NGrammar.NOneOf(252, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("messageBodyElem")))))), Set(253)),
      253 -> NGrammar.NProxy(253, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("messageBodyElem")))), 254),
      255 -> NGrammar.NNonterminal(255, Symbols.Nonterminal("messageBodyElem"), Set(256, 365, 240, 149, 403, 420, 433, 399)),
      257 -> NGrammar.NNonterminal(257, Symbols.Nonterminal("field"), Set(258)),
      259 -> NGrammar.NOneOf(259, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('p'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('d')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(260, 81)),
      260 -> NGrammar.NOneOf(260, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('p'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('d')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS")))))), Set(261)),
      261 -> NGrammar.NProxy(261, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('p'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('d')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS")))), 262),
      263 -> NGrammar.NJoin(263, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('p'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('d')))), Symbols.Nonterminal("Tk")), 264, 21),
      264 -> NGrammar.NProxy(264, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('p'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('d')))), 265),
      266 -> NGrammar.NTerminal(266, Symbols.ExactChar('d')),
      267 -> NGrammar.NNonterminal(267, Symbols.Nonterminal("type"), Set(268, 336)),
      269 -> NGrammar.NNonterminal(269, Symbols.Nonterminal("builtinType"), Set(270)),
      271 -> NGrammar.NJoin(271, Symbols.Join(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'), Symbols.ExactChar('o'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('e'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('l'), Symbols.ExactChar('o'), Symbols.ExactChar('a'), Symbols.ExactChar('t'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('y'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('s'))))))))), Symbols.Nonterminal("Tk")), 272, 21),
      272 -> NGrammar.NOneOf(272, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'), Symbols.ExactChar('o'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('e'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('l'), Symbols.ExactChar('o'), Symbols.ExactChar('a'), Symbols.ExactChar('t'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('y'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('s'))))))))), Set(273, 277, 281, 286, 292, 296, 300, 304, 308, 312, 316, 320, 324, 328, 332)),
      273 -> NGrammar.NProxy(273, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'), Symbols.ExactChar('o'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('e'))))))), 274),
      275 -> NGrammar.NProxy(275, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'), Symbols.ExactChar('o'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('e')))), 276),
      277 -> NGrammar.NProxy(277, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('l'), Symbols.ExactChar('o'), Symbols.ExactChar('a'), Symbols.ExactChar('t'))))))), 278),
      279 -> NGrammar.NProxy(279, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('l'), Symbols.ExactChar('o'), Symbols.ExactChar('a'), Symbols.ExactChar('t')))), 280),
      281 -> NGrammar.NProxy(281, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), 282),
      283 -> NGrammar.NProxy(283, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2')))), 284),
      285 -> NGrammar.NTerminal(285, Symbols.ExactChar('2')),
      286 -> NGrammar.NProxy(286, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), 287),
      288 -> NGrammar.NProxy(288, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4')))), 289),
      290 -> NGrammar.NTerminal(290, Symbols.ExactChar('6')),
      291 -> NGrammar.NTerminal(291, Symbols.ExactChar('4')),
      292 -> NGrammar.NProxy(292, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), 293),
      294 -> NGrammar.NProxy(294, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2')))), 295),
      296 -> NGrammar.NProxy(296, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), 297),
      298 -> NGrammar.NProxy(298, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4')))), 299),
      300 -> NGrammar.NProxy(300, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), 301),
      302 -> NGrammar.NProxy(302, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2')))), 303),
      304 -> NGrammar.NProxy(304, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), 305),
      306 -> NGrammar.NProxy(306, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4')))), 307),
      308 -> NGrammar.NProxy(308, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), 309),
      310 -> NGrammar.NProxy(310, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2')))), 311),
      312 -> NGrammar.NProxy(312, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), 313),
      314 -> NGrammar.NProxy(314, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4')))), 315),
      316 -> NGrammar.NProxy(316, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), 317),
      318 -> NGrammar.NProxy(318, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2')))), 319),
      320 -> NGrammar.NProxy(320, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), 321),
      322 -> NGrammar.NProxy(322, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4')))), 323),
      324 -> NGrammar.NProxy(324, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'))))))), 325),
      326 -> NGrammar.NProxy(326, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l')))), 327),
      328 -> NGrammar.NProxy(328, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g'))))))), 329),
      330 -> NGrammar.NProxy(330, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g')))), 331),
      332 -> NGrammar.NProxy(332, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('y'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('s'))))))), 333),
      334 -> NGrammar.NProxy(334, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('y'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('s')))), 335),
      337 -> NGrammar.NExcept(337, Symbols.Except(Symbols.Nonterminal("messageType"), Symbols.Nonterminal("builtinType")), 338, 269),
      338 -> NGrammar.NNonterminal(338, Symbols.Nonterminal("messageType"), Set(339)),
      340 -> NGrammar.NOneOf(340, Symbols.OneOf(ListSet(Symbols.ExactChar('.'), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(147, 81)),
      341 -> NGrammar.NRepeat(341, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ident"), Symbols.ExactChar('.')))))), 0), 7, 342),
      343 -> NGrammar.NOneOf(343, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ident"), Symbols.ExactChar('.')))))), Set(344)),
      344 -> NGrammar.NProxy(344, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ident"), Symbols.ExactChar('.')))), 345),
      346 -> NGrammar.NNonterminal(346, Symbols.Nonterminal("fieldName"), Set(159)),
      347 -> NGrammar.NNonterminal(347, Symbols.Nonterminal("fieldNumber"), Set(348)),
      349 -> NGrammar.NOneOf(349, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOptions"), Symbols.Nonterminal("WS"), Symbols.ExactChar(']')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(350, 81)),
      350 -> NGrammar.NOneOf(350, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOptions"), Symbols.Nonterminal("WS"), Symbols.ExactChar(']')))))), Set(351)),
      351 -> NGrammar.NProxy(351, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOptions"), Symbols.Nonterminal("WS"), Symbols.ExactChar(']')))), 352),
      353 -> NGrammar.NTerminal(353, Symbols.ExactChar('[')),
      354 -> NGrammar.NNonterminal(354, Symbols.Nonterminal("fieldOptions"), Set(355)),
      356 -> NGrammar.NNonterminal(356, Symbols.Nonterminal("fieldOption"), Set(357)),
      358 -> NGrammar.NRepeat(358, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOption")))))), 0), 7, 359),
      360 -> NGrammar.NOneOf(360, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOption")))))), Set(361)),
      361 -> NGrammar.NProxy(361, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOption")))), 362),
      363 -> NGrammar.NTerminal(363, Symbols.ExactChar(',')),
      364 -> NGrammar.NTerminal(364, Symbols.ExactChar(']')),
      366 -> NGrammar.NNonterminal(366, Symbols.Nonterminal("enum"), Set(367)),
      368 -> NGrammar.NJoin(368, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('e'), Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('m')))), Symbols.Nonterminal("Tk")), 369, 21),
      369 -> NGrammar.NProxy(369, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('e'), Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('m')))), 370),
      371 -> NGrammar.NNonterminal(371, Symbols.Nonterminal("enumName"), Set(159)),
      372 -> NGrammar.NNonterminal(372, Symbols.Nonterminal("enumBody"), Set(373)),
      374 -> NGrammar.NRepeat(374, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("enumField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), 7, 375),
      376 -> NGrammar.NOneOf(376, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("enumField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), Set(377)),
      377 -> NGrammar.NProxy(377, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("enumField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))), 378),
      379 -> NGrammar.NOneOf(379, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("enumField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement")))))), Set(148, 380, 398)),
      380 -> NGrammar.NProxy(380, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("enumField")))), 381),
      382 -> NGrammar.NNonterminal(382, Symbols.Nonterminal("enumField"), Set(383)),
      384 -> NGrammar.NOneOf(384, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('-')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(385, 81)),
      385 -> NGrammar.NOneOf(385, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('-')))))), Set(386)),
      386 -> NGrammar.NProxy(386, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('-')))), 387),
      388 -> NGrammar.NOneOf(388, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption")))))), 0), Symbols.Nonterminal("WS"), Symbols.ExactChar(']')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(389, 81)),
      389 -> NGrammar.NOneOf(389, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption")))))), 0), Symbols.Nonterminal("WS"), Symbols.ExactChar(']')))))), Set(390)),
      390 -> NGrammar.NProxy(390, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption")))))), 0), Symbols.Nonterminal("WS"), Symbols.ExactChar(']')))), 391),
      392 -> NGrammar.NNonterminal(392, Symbols.Nonterminal("enumValueOption"), Set(357)),
      393 -> NGrammar.NRepeat(393, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption")))))), 0), 7, 394),
      395 -> NGrammar.NOneOf(395, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption")))))), Set(396)),
      396 -> NGrammar.NProxy(396, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption")))), 397),
      398 -> NGrammar.NProxy(398, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement")))), 399),
      400 -> NGrammar.NNonterminal(400, Symbols.Nonterminal("emptyStatement"), Set(401)),
      402 -> NGrammar.NTerminal(402, Symbols.ExactChar('}')),
      404 -> NGrammar.NNonterminal(404, Symbols.Nonterminal("oneof"), Set(405)),
      406 -> NGrammar.NJoin(406, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('o'), Symbols.ExactChar('n'), Symbols.ExactChar('e'), Symbols.ExactChar('o'), Symbols.ExactChar('f')))), Symbols.Nonterminal("Tk")), 407, 21),
      407 -> NGrammar.NProxy(407, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('o'), Symbols.ExactChar('n'), Symbols.ExactChar('e'), Symbols.ExactChar('o'), Symbols.ExactChar('f')))), 408),
      409 -> NGrammar.NNonterminal(409, Symbols.Nonterminal("oneofName"), Set(159)),
      410 -> NGrammar.NRepeat(410, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("oneofField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), 7, 411),
      412 -> NGrammar.NOneOf(412, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("oneofField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), Set(413)),
      413 -> NGrammar.NProxy(413, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("oneofField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))), 414),
      415 -> NGrammar.NOneOf(415, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("oneofField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement")))))), Set(148, 416, 398)),
      416 -> NGrammar.NProxy(416, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("oneofField")))), 417),
      418 -> NGrammar.NNonterminal(418, Symbols.Nonterminal("oneofField"), Set(419)),
      421 -> NGrammar.NNonterminal(421, Symbols.Nonterminal("mapField"), Set(422)),
      423 -> NGrammar.NJoin(423, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('a'), Symbols.ExactChar('p')))), Symbols.Nonterminal("Tk")), 424, 21),
      424 -> NGrammar.NProxy(424, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('a'), Symbols.ExactChar('p')))), 425),
      426 -> NGrammar.NTerminal(426, Symbols.ExactChar('<')),
      427 -> NGrammar.NNonterminal(427, Symbols.Nonterminal("keyType"), Set(428)),
      429 -> NGrammar.NJoin(429, Symbols.Join(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g'))))))))), Symbols.Nonterminal("Tk")), 430, 21),
      430 -> NGrammar.NOneOf(430, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g'))))))))), Set(281, 286, 292, 296, 300, 304, 308, 312, 316, 320, 324, 328)),
      431 -> NGrammar.NTerminal(431, Symbols.ExactChar('>')),
      432 -> NGrammar.NNonterminal(432, Symbols.Nonterminal("mapName"), Set(159)),
      434 -> NGrammar.NNonterminal(434, Symbols.Nonterminal("reserved"), Set(435)),
      436 -> NGrammar.NJoin(436, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('s'), Symbols.ExactChar('e'), Symbols.ExactChar('r'), Symbols.ExactChar('v'), Symbols.ExactChar('e'), Symbols.ExactChar('d')))), Symbols.Nonterminal("Tk")), 437, 21),
      437 -> NGrammar.NProxy(437, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('s'), Symbols.ExactChar('e'), Symbols.ExactChar('r'), Symbols.ExactChar('v'), Symbols.ExactChar('e'), Symbols.ExactChar('d')))), 438),
      439 -> NGrammar.NTerminal(439, Symbols.ExactChar('v')),
      440 -> NGrammar.NOneOf(440, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ranges")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("fieldNames")))))), Set(441, 464)),
      441 -> NGrammar.NProxy(441, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ranges")))), 442),
      443 -> NGrammar.NNonterminal(443, Symbols.Nonterminal("ranges"), Set(444)),
      445 -> NGrammar.NNonterminal(445, Symbols.Nonterminal("range"), Set(446)),
      447 -> NGrammar.NOneOf(447, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('o')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("rangeEnd")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(448, 81)),
      448 -> NGrammar.NOneOf(448, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('o')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("rangeEnd")))))), Set(449)),
      449 -> NGrammar.NProxy(449, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('o')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("rangeEnd")))), 450),
      451 -> NGrammar.NJoin(451, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('o')))), Symbols.Nonterminal("Tk")), 452, 21),
      452 -> NGrammar.NProxy(452, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('o')))), 453),
      454 -> NGrammar.NNonterminal(454, Symbols.Nonterminal("rangeEnd"), Set(348, 455)),
      456 -> NGrammar.NJoin(456, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('a'), Symbols.ExactChar('x')))), Symbols.Nonterminal("Tk")), 457, 21),
      457 -> NGrammar.NProxy(457, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('a'), Symbols.ExactChar('x')))), 458),
      459 -> NGrammar.NRepeat(459, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("range")))))), 0), 7, 460),
      461 -> NGrammar.NOneOf(461, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("range")))))), Set(462)),
      462 -> NGrammar.NProxy(462, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("range")))), 463),
      464 -> NGrammar.NProxy(464, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("fieldNames")))), 465),
      466 -> NGrammar.NNonterminal(466, Symbols.Nonterminal("fieldNames"), Set(467)),
      468 -> NGrammar.NRepeat(468, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldName")))))), 0), 7, 469),
      470 -> NGrammar.NOneOf(470, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldName")))))), Set(471)),
      471 -> NGrammar.NProxy(471, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldName")))), 472),
      474 -> NGrammar.NNonterminal(474, Symbols.Nonterminal("service"), Set(475)),
      476 -> NGrammar.NJoin(476, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('e'), Symbols.ExactChar('r'), Symbols.ExactChar('v'), Symbols.ExactChar('i'), Symbols.ExactChar('c'), Symbols.ExactChar('e')))), Symbols.Nonterminal("Tk")), 477, 21),
      477 -> NGrammar.NProxy(477, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('e'), Symbols.ExactChar('r'), Symbols.ExactChar('v'), Symbols.ExactChar('i'), Symbols.ExactChar('c'), Symbols.ExactChar('e')))), 478),
      479 -> NGrammar.NNonterminal(479, Symbols.Nonterminal("serviceName"), Set(159)),
      480 -> NGrammar.NRepeat(480, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("rpc")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), 7, 481),
      482 -> NGrammar.NOneOf(482, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("rpc")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), Set(483)),
      483 -> NGrammar.NProxy(483, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("rpc")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))), 484),
      485 -> NGrammar.NOneOf(485, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("rpc")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement")))))), Set(148, 486, 398)),
      486 -> NGrammar.NProxy(486, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("rpc")))), 487),
      488 -> NGrammar.NNonterminal(488, Symbols.Nonterminal("rpc"), Set(489)),
      490 -> NGrammar.NJoin(490, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('p'), Symbols.ExactChar('c')))), Symbols.Nonterminal("Tk")), 491, 21),
      491 -> NGrammar.NProxy(491, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('p'), Symbols.ExactChar('c')))), 492),
      493 -> NGrammar.NNonterminal(493, Symbols.Nonterminal("rpcName"), Set(159)),
      494 -> NGrammar.NOneOf(494, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('m')))), Symbols.Nonterminal("Tk"))))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(495, 81)),
      495 -> NGrammar.NOneOf(495, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('m')))), Symbols.Nonterminal("Tk"))))))), Set(496)),
      496 -> NGrammar.NProxy(496, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('m')))), Symbols.Nonterminal("Tk"))))), 497),
      498 -> NGrammar.NJoin(498, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('m')))), Symbols.Nonterminal("Tk")), 499, 21),
      499 -> NGrammar.NProxy(499, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('m')))), 500),
      501 -> NGrammar.NJoin(501, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('t'), Symbols.ExactChar('u'), Symbols.ExactChar('r'), Symbols.ExactChar('n'), Symbols.ExactChar('s')))), Symbols.Nonterminal("Tk")), 502, 21),
      502 -> NGrammar.NProxy(502, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('t'), Symbols.ExactChar('u'), Symbols.ExactChar('r'), Symbols.ExactChar('n'), Symbols.ExactChar('s')))), 503),
      504 -> NGrammar.NNonterminal(504, Symbols.Nonterminal("rpcEnding"), Set(505, 401)),
      506 -> NGrammar.NRepeat(506, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), 7, 507),
      508 -> NGrammar.NOneOf(508, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), Set(509)),
      509 -> NGrammar.NProxy(509, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))), 510),
      511 -> NGrammar.NOneOf(511, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement")))))), Set(148, 398))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("syntax"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("import")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("package")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("topLevelDef")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), Symbols.Nonterminal("WS"))), Seq(4, 10, 47, 4)),
      5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet), 0))), Seq(6)),
      7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq()), Seq()),
      8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet), 0), Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet))), Seq(6, 9)),
      11 -> NGrammar.NSequence(11, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('y'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('a'), Symbols.ExactChar('x')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('"'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('\''))))))))), Symbols.Nonterminal("WS"), Symbols.ExactChar(';'))), Seq(12, 4, 30, 4, 31, 4, 46)),
      14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('y'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('a'), Symbols.ExactChar('x'))), Seq(15, 16, 17, 18, 19, 20)),
      22 -> NGrammar.NSequence(22, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))))), Seq(23)),
      26 -> NGrammar.NSequence(26, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))), Seq(27)),
      28 -> NGrammar.NSequence(28, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(27, 29)),
      33 -> NGrammar.NSequence(33, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('"')))))), Seq(34)),
      35 -> NGrammar.NSequence(35, Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('"'))), Seq(36, 37, 38, 39, 18, 39, 40, 36)),
      42 -> NGrammar.NSequence(42, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('\'')))))), Seq(43)),
      44 -> NGrammar.NSequence(44, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.ExactChar('p'), Symbols.ExactChar('r'), Symbols.ExactChar('o'), Symbols.ExactChar('t'), Symbols.ExactChar('o'), Symbols.ExactChar('3'), Symbols.ExactChar('\''))), Seq(45, 37, 38, 39, 18, 39, 40, 45)),
      48 -> NGrammar.NSequence(48, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("import")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("package")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("topLevelDef")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("import")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("package")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("topLevelDef")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))))), Seq(47, 49)),
      51 -> NGrammar.NSequence(51, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("import")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("package")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("topLevelDef")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement")))))))), Seq(4, 52)),
      54 -> NGrammar.NSequence(54, Symbols.Sequence(Seq(Symbols.Nonterminal("import"))), Seq(55)),
      56 -> NGrammar.NSequence(56, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('m'), Symbols.ExactChar('p'), Symbols.ExactChar('o'), Symbols.ExactChar('r'), Symbols.ExactChar('t')))), Symbols.Nonterminal("Tk")), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('w'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('k')))), Symbols.Nonterminal("Tk"))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('i'), Symbols.ExactChar('c')))), Symbols.Nonterminal("Tk"))))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("strLit"), Symbols.Nonterminal("WS"), Symbols.ExactChar(';'))), Seq(57, 62, 4, 82, 4, 46)),
      59 -> NGrammar.NSequence(59, Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('m'), Symbols.ExactChar('p'), Symbols.ExactChar('o'), Symbols.ExactChar('r'), Symbols.ExactChar('t'))), Seq(60, 61, 37, 39, 38, 18)),
      65 -> NGrammar.NSequence(65, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('w'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('k')))), Symbols.Nonterminal("Tk")))), Seq(4, 66)),
      68 -> NGrammar.NSequence(68, Symbols.Sequence(Seq(Symbols.ExactChar('w'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('k'))), Seq(69, 70, 19, 71)),
      73 -> NGrammar.NSequence(73, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('i'), Symbols.ExactChar('c')))), Symbols.Nonterminal("Tk")))), Seq(4, 74)),
      76 -> NGrammar.NSequence(76, Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('i'), Symbols.ExactChar('c'))), Seq(37, 77, 78, 79, 60, 80)),
      83 -> NGrammar.NSequence(83, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Repeat(Symbols.Nonterminal("charValue"), 0), Symbols.ExactChar('\''))), Seq(45, 84, 45)),
      85 -> NGrammar.NSequence(85, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("charValue"), 0), Symbols.Nonterminal("charValue"))), Seq(84, 86)),
      87 -> NGrammar.NSequence(87, Symbols.Sequence(Seq(Symbols.Nonterminal("hexEscape"))), Seq(88)),
      89 -> NGrammar.NSequence(89, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('X', 'x')), Symbols.Nonterminal("hexDigit"), Symbols.Nonterminal("hexDigit"))), Seq(90, 91, 92, 92)),
      93 -> NGrammar.NSequence(93, Symbols.Sequence(Seq(Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet))), Seq(94)),
      95 -> NGrammar.NSequence(95, Symbols.Sequence(Seq(Symbols.Nonterminal("octEscape"))), Seq(96)),
      97 -> NGrammar.NSequence(97, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Nonterminal("octalDigit"), Symbols.Nonterminal("octalDigit"), Symbols.Nonterminal("octalDigit"))), Seq(90, 98, 98, 98)),
      99 -> NGrammar.NSequence(99, Symbols.Sequence(Seq(Symbols.Chars(('0' to '7').toSet))), Seq(100)),
      101 -> NGrammar.NSequence(101, Symbols.Sequence(Seq(Symbols.Nonterminal("charEscape"))), Seq(102)),
      103 -> NGrammar.NSequence(103, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('"', '\'', '\\', 'f', 'n', 'r', 't', 'v') ++ ('a' to 'b').toSet))), Seq(90, 104)),
      105 -> NGrammar.NSequence(105, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\n', '\\'))))), Seq(106)),
      109 -> NGrammar.NSequence(109, Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.Repeat(Symbols.Nonterminal("charValue"), 0), Symbols.ExactChar('"'))), Seq(36, 84, 36)),
      111 -> NGrammar.NSequence(111, Symbols.Sequence(Seq(Symbols.Nonterminal("package"))), Seq(112)),
      113 -> NGrammar.NSequence(113, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('a'), Symbols.ExactChar('c'), Symbols.ExactChar('k'), Symbols.ExactChar('a'), Symbols.ExactChar('g'), Symbols.ExactChar('e')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fullIdent"), Symbols.Nonterminal("WS"), Symbols.ExactChar(';'))), Seq(114, 4, 118, 4, 46)),
      116 -> NGrammar.NSequence(116, Symbols.Sequence(Seq(Symbols.ExactChar('p'), Symbols.ExactChar('a'), Symbols.ExactChar('c'), Symbols.ExactChar('k'), Symbols.ExactChar('a'), Symbols.ExactChar('g'), Symbols.ExactChar('e'))), Seq(37, 19, 80, 71, 19, 117, 70)),
      119 -> NGrammar.NSequence(119, Symbols.Sequence(Seq(Symbols.Nonterminal("ident"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))))), 0))), Seq(120, 142)),
      121 -> NGrammar.NSequence(121, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalDigit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('_')))))), 0))))))))), Seq(122)),
      125 -> NGrammar.NSequence(125, Symbols.Sequence(Seq(Symbols.Nonterminal("letter"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalDigit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('_')))))), 0))), Seq(126, 129)),
      127 -> NGrammar.NSequence(127, Symbols.Sequence(Seq(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(128)),
      130 -> NGrammar.NSequence(130, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalDigit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('_')))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("letter")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalDigit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('_')))))))), Seq(129, 131)),
      133 -> NGrammar.NSequence(133, Symbols.Sequence(Seq(Symbols.Nonterminal("letter"))), Seq(126)),
      135 -> NGrammar.NSequence(135, Symbols.Sequence(Seq(Symbols.Nonterminal("decimalDigit"))), Seq(136)),
      137 -> NGrammar.NSequence(137, Symbols.Sequence(Seq(Symbols.Chars(('0' to '9').toSet))), Seq(138)),
      140 -> NGrammar.NSequence(140, Symbols.Sequence(Seq(Symbols.ExactChar('_'))), Seq(141)),
      143 -> NGrammar.NSequence(143, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))))))), Seq(142, 144)),
      146 -> NGrammar.NSequence(146, Symbols.Sequence(Seq(Symbols.ExactChar('.'), Symbols.Nonterminal("ident"))), Seq(147, 120)),
      149 -> NGrammar.NSequence(149, Symbols.Sequence(Seq(Symbols.Nonterminal("option"))), Seq(150)),
      151 -> NGrammar.NSequence(151, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('o'), Symbols.ExactChar('p'), Symbols.ExactChar('t'), Symbols.ExactChar('i'), Symbols.ExactChar('o'), Symbols.ExactChar('n')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("optionName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.Nonterminal("constant"), Symbols.Nonterminal("WS"), Symbols.ExactChar(';'))), Seq(152, 4, 155, 4, 30, 4, 169, 4, 46)),
      154 -> NGrammar.NSequence(154, Symbols.Sequence(Seq(Symbols.ExactChar('o'), Symbols.ExactChar('p'), Symbols.ExactChar('t'), Symbols.ExactChar('i'), Symbols.ExactChar('o'), Symbols.ExactChar('n'))), Seq(39, 37, 18, 60, 39, 17)),
      156 -> NGrammar.NSequence(156, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ident")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fullIdent"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')')))))), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))))), 0))), Seq(157, 164)),
      159 -> NGrammar.NSequence(159, Symbols.Sequence(Seq(Symbols.Nonterminal("ident"))), Seq(120)),
      161 -> NGrammar.NSequence(161, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fullIdent"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(162, 4, 118, 4, 163)),
      165 -> NGrammar.NSequence(165, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('.'), Symbols.Nonterminal("ident")))))))), Seq(164, 166)),
      168 -> NGrammar.NSequence(168, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('.'), Symbols.Nonterminal("ident"))), Seq(4, 147, 120)),
      170 -> NGrammar.NSequence(170, Symbols.Sequence(Seq(Symbols.Except(Symbols.Nonterminal("fullIdent"), Symbols.Nonterminal("boolLit")))), Seq(171)),
      173 -> NGrammar.NSequence(173, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e')))))), Seq(174)),
      175 -> NGrammar.NSequence(175, Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))), Seq(18, 38, 77, 70)),
      176 -> NGrammar.NSequence(176, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e')))))), Seq(177)),
      178 -> NGrammar.NSequence(178, Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))), Seq(179, 19, 79, 15, 70)),
      180 -> NGrammar.NSequence(180, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Nonterminal("sign"), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("intLit"))), Seq(181, 187)),
      183 -> NGrammar.NSequence(183, Symbols.Sequence(Seq(Symbols.ExactChar('+'))), Seq(184)),
      185 -> NGrammar.NSequence(185, Symbols.Sequence(Seq(Symbols.ExactChar('-'))), Seq(186)),
      188 -> NGrammar.NSequence(188, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("zeroLit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("decimalLit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("octalLit")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("hexLit"))))))))), Seq(189)),
      192 -> NGrammar.NSequence(192, Symbols.Sequence(Seq(Symbols.Nonterminal("zeroLit"))), Seq(193)),
      194 -> NGrammar.NSequence(194, Symbols.Sequence(Seq(Symbols.ExactChar('0'))), Seq(195)),
      197 -> NGrammar.NSequence(197, Symbols.Sequence(Seq(Symbols.Nonterminal("decimalLit"))), Seq(198)),
      199 -> NGrammar.NSequence(199, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Nonterminal("decimalDigit"), 0))), Seq(200, 201)),
      202 -> NGrammar.NSequence(202, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("decimalDigit"), 0), Symbols.Nonterminal("decimalDigit"))), Seq(201, 136)),
      204 -> NGrammar.NSequence(204, Symbols.Sequence(Seq(Symbols.Nonterminal("octalLit"))), Seq(205)),
      206 -> NGrammar.NSequence(206, Symbols.Sequence(Seq(Symbols.ExactChar('0'), Symbols.Repeat(Symbols.Nonterminal("octalDigit"), 1))), Seq(195, 207)),
      208 -> NGrammar.NSequence(208, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("octalDigit"), 1), Symbols.Nonterminal("octalDigit"))), Seq(207, 98)),
      210 -> NGrammar.NSequence(210, Symbols.Sequence(Seq(Symbols.Nonterminal("hexLit"))), Seq(211)),
      212 -> NGrammar.NSequence(212, Symbols.Sequence(Seq(Symbols.ExactChar('0'), Symbols.Chars(Set('X', 'x')), Symbols.Repeat(Symbols.Nonterminal("hexDigit"), 1))), Seq(195, 91, 213)),
      214 -> NGrammar.NSequence(214, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("hexDigit"), 1), Symbols.Nonterminal("hexDigit"))), Seq(213, 92)),
      215 -> NGrammar.NSequence(215, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Nonterminal("sign"), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("floatLit"))), Seq(181, 216)),
      217 -> NGrammar.NSequence(217, Symbols.Sequence(Seq(Symbols.Nonterminal("decimals"), Symbols.ExactChar('.'), Symbols.OneOf(ListSet(Symbols.Nonterminal("decimals"), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.OneOf(ListSet(Symbols.Nonterminal("exponent"), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(218, 147, 222, 223)),
      219 -> NGrammar.NSequence(219, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("decimalDigit"), 1))), Seq(220)),
      221 -> NGrammar.NSequence(221, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("decimalDigit"), 1), Symbols.Nonterminal("decimalDigit"))), Seq(220, 136)),
      225 -> NGrammar.NSequence(225, Symbols.Sequence(Seq(Symbols.Chars(Set('E', 'e')), Symbols.OneOf(ListSet(Symbols.Nonterminal("sign"), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("decimals"))), Seq(226, 181, 218)),
      227 -> NGrammar.NSequence(227, Symbols.Sequence(Seq(Symbols.Nonterminal("decimals"), Symbols.Nonterminal("exponent"))), Seq(218, 224)),
      228 -> NGrammar.NSequence(228, Symbols.Sequence(Seq(Symbols.ExactChar('.'), Symbols.Nonterminal("decimals"), Symbols.OneOf(ListSet(Symbols.Nonterminal("exponent"), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(147, 218, 223)),
      229 -> NGrammar.NSequence(229, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('f')))))), Seq(230)),
      231 -> NGrammar.NSequence(231, Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('f'))), Seq(60, 17, 179)),
      232 -> NGrammar.NSequence(232, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('a'), Symbols.ExactChar('n')))))), Seq(233)),
      234 -> NGrammar.NSequence(234, Symbols.Sequence(Seq(Symbols.ExactChar('n'), Symbols.ExactChar('a'), Symbols.ExactChar('n'))), Seq(17, 19, 17)),
      235 -> NGrammar.NSequence(235, Symbols.Sequence(Seq(Symbols.Nonterminal("strLit"))), Seq(82)),
      236 -> NGrammar.NSequence(236, Symbols.Sequence(Seq(Symbols.Nonterminal("boolLit"))), Seq(172)),
      238 -> NGrammar.NSequence(238, Symbols.Sequence(Seq(Symbols.Nonterminal("topLevelDef"))), Seq(239)),
      240 -> NGrammar.NSequence(240, Symbols.Sequence(Seq(Symbols.Nonterminal("message"))), Seq(241)),
      242 -> NGrammar.NSequence(242, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('e'), Symbols.ExactChar('s'), Symbols.ExactChar('s'), Symbols.ExactChar('a'), Symbols.ExactChar('g'), Symbols.ExactChar('e')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("messageName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("messageBody"))), Seq(243, 4, 246, 4, 247)),
      245 -> NGrammar.NSequence(245, Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('e'), Symbols.ExactChar('s'), Symbols.ExactChar('s'), Symbols.ExactChar('a'), Symbols.ExactChar('g'), Symbols.ExactChar('e'))), Seq(61, 70, 15, 15, 19, 117, 70)),
      248 -> NGrammar.NSequence(248, Symbols.Sequence(Seq(Symbols.ExactChar('{'), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("messageBodyElem")))))), 0), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(249, 250, 4, 402)),
      251 -> NGrammar.NSequence(251, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("messageBodyElem")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("messageBodyElem")))))))), Seq(250, 252)),
      254 -> NGrammar.NSequence(254, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("messageBodyElem"))), Seq(4, 255)),
      256 -> NGrammar.NSequence(256, Symbols.Sequence(Seq(Symbols.Nonterminal("field"))), Seq(257)),
      258 -> NGrammar.NSequence(258, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('p'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('d')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("type"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldNumber"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOptions"), Symbols.Nonterminal("WS"), Symbols.ExactChar(']')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar(';'))), Seq(259, 267, 4, 346, 4, 30, 4, 347, 349, 4, 46)),
      262 -> NGrammar.NSequence(262, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('p'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('d')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"))), Seq(263, 4)),
      265 -> NGrammar.NSequence(265, Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('p'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('d'))), Seq(38, 70, 37, 70, 19, 18, 70, 266)),
      268 -> NGrammar.NSequence(268, Symbols.Sequence(Seq(Symbols.Nonterminal("builtinType"))), Seq(269)),
      270 -> NGrammar.NSequence(270, Symbols.Sequence(Seq(Symbols.Join(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'), Symbols.ExactChar('o'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('e'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('l'), Symbols.ExactChar('o'), Symbols.ExactChar('a'), Symbols.ExactChar('t'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('y'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('s'))))))))), Symbols.Nonterminal("Tk")))), Seq(271)),
      274 -> NGrammar.NSequence(274, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'), Symbols.ExactChar('o'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('e')))))), Seq(275)),
      276 -> NGrammar.NSequence(276, Symbols.Sequence(Seq(Symbols.ExactChar('d'), Symbols.ExactChar('o'), Symbols.ExactChar('u'), Symbols.ExactChar('b'), Symbols.ExactChar('l'), Symbols.ExactChar('e'))), Seq(266, 39, 77, 78, 79, 70)),
      278 -> NGrammar.NSequence(278, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('l'), Symbols.ExactChar('o'), Symbols.ExactChar('a'), Symbols.ExactChar('t')))))), Seq(279)),
      280 -> NGrammar.NSequence(280, Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('l'), Symbols.ExactChar('o'), Symbols.ExactChar('a'), Symbols.ExactChar('t'))), Seq(179, 79, 39, 19, 18)),
      282 -> NGrammar.NSequence(282, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2')))))), Seq(283)),
      284 -> NGrammar.NSequence(284, Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))), Seq(60, 17, 18, 40, 285)),
      287 -> NGrammar.NSequence(287, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4')))))), Seq(288)),
      289 -> NGrammar.NSequence(289, Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))), Seq(60, 17, 18, 290, 291)),
      293 -> NGrammar.NSequence(293, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2')))))), Seq(294)),
      295 -> NGrammar.NSequence(295, Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))), Seq(77, 60, 17, 18, 40, 285)),
      297 -> NGrammar.NSequence(297, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4')))))), Seq(298)),
      299 -> NGrammar.NSequence(299, Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))), Seq(77, 60, 17, 18, 290, 291)),
      301 -> NGrammar.NSequence(301, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2')))))), Seq(302)),
      303 -> NGrammar.NSequence(303, Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))), Seq(15, 60, 17, 18, 40, 285)),
      305 -> NGrammar.NSequence(305, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4')))))), Seq(306)),
      307 -> NGrammar.NSequence(307, Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))), Seq(15, 60, 17, 18, 290, 291)),
      309 -> NGrammar.NSequence(309, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2')))))), Seq(310)),
      311 -> NGrammar.NSequence(311, Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))), Seq(179, 60, 20, 70, 266, 40, 285)),
      313 -> NGrammar.NSequence(313, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4')))))), Seq(314)),
      315 -> NGrammar.NSequence(315, Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))), Seq(179, 60, 20, 70, 266, 290, 291)),
      317 -> NGrammar.NSequence(317, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2')))))), Seq(318)),
      319 -> NGrammar.NSequence(319, Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))), Seq(15, 179, 60, 20, 70, 266, 40, 285)),
      321 -> NGrammar.NSequence(321, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4')))))), Seq(322)),
      323 -> NGrammar.NSequence(323, Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))), Seq(15, 179, 60, 20, 70, 266, 290, 291)),
      325 -> NGrammar.NSequence(325, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l')))))), Seq(326)),
      327 -> NGrammar.NSequence(327, Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'))), Seq(78, 39, 39, 79)),
      329 -> NGrammar.NSequence(329, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g')))))), Seq(330)),
      331 -> NGrammar.NSequence(331, Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g'))), Seq(15, 18, 38, 60, 17, 117)),
      333 -> NGrammar.NSequence(333, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('y'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('s')))))), Seq(334)),
      335 -> NGrammar.NSequence(335, Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('y'), Symbols.ExactChar('t'), Symbols.ExactChar('e'), Symbols.ExactChar('s'))), Seq(78, 16, 18, 70, 15)),
      336 -> NGrammar.NSequence(336, Symbols.Sequence(Seq(Symbols.Except(Symbols.Nonterminal("messageType"), Symbols.Nonterminal("builtinType")))), Seq(337)),
      339 -> NGrammar.NSequence(339, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.ExactChar('.'), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ident"), Symbols.ExactChar('.')))))), 0), Symbols.Nonterminal("messageName"))), Seq(340, 341, 246)),
      342 -> NGrammar.NSequence(342, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ident"), Symbols.ExactChar('.')))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ident"), Symbols.ExactChar('.')))))))), Seq(341, 343)),
      345 -> NGrammar.NSequence(345, Symbols.Sequence(Seq(Symbols.Nonterminal("ident"), Symbols.ExactChar('.'))), Seq(120, 147)),
      348 -> NGrammar.NSequence(348, Symbols.Sequence(Seq(Symbols.Nonterminal("intLit"))), Seq(187)),
      352 -> NGrammar.NSequence(352, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOptions"), Symbols.Nonterminal("WS"), Symbols.ExactChar(']'))), Seq(4, 353, 4, 354, 4, 364)),
      355 -> NGrammar.NSequence(355, Symbols.Sequence(Seq(Symbols.Nonterminal("fieldOption"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOption")))))), 0))), Seq(356, 358)),
      357 -> NGrammar.NSequence(357, Symbols.Sequence(Seq(Symbols.Nonterminal("optionName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.Nonterminal("constant"))), Seq(155, 4, 30, 4, 169)),
      359 -> NGrammar.NSequence(359, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOption")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOption")))))))), Seq(358, 360)),
      362 -> NGrammar.NSequence(362, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOption"))), Seq(4, 363, 4, 356)),
      365 -> NGrammar.NSequence(365, Symbols.Sequence(Seq(Symbols.Nonterminal("enum"))), Seq(366)),
      367 -> NGrammar.NSequence(367, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('e'), Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('m')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumBody"))), Seq(368, 4, 371, 4, 372)),
      370 -> NGrammar.NSequence(370, Symbols.Sequence(Seq(Symbols.ExactChar('e'), Symbols.ExactChar('n'), Symbols.ExactChar('u'), Symbols.ExactChar('m'))), Seq(70, 17, 77, 61)),
      373 -> NGrammar.NSequence(373, Symbols.Sequence(Seq(Symbols.ExactChar('{'), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("enumField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(249, 374, 4, 402)),
      375 -> NGrammar.NSequence(375, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("enumField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("enumField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))))), Seq(374, 376)),
      378 -> NGrammar.NSequence(378, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("enumField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement")))))))), Seq(4, 379)),
      381 -> NGrammar.NSequence(381, Symbols.Sequence(Seq(Symbols.Nonterminal("enumField"))), Seq(382)),
      383 -> NGrammar.NSequence(383, Symbols.Sequence(Seq(Symbols.Nonterminal("ident"), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('-')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("intLit"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption")))))), 0), Symbols.Nonterminal("WS"), Symbols.ExactChar(']')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar(';'))), Seq(120, 4, 30, 384, 4, 187, 388, 4, 46)),
      387 -> NGrammar.NSequence(387, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('-'))), Seq(4, 186)),
      391 -> NGrammar.NSequence(391, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption")))))), 0), Symbols.Nonterminal("WS"), Symbols.ExactChar(']'))), Seq(4, 353, 4, 392, 393, 4, 364)),
      394 -> NGrammar.NSequence(394, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption")))))))), Seq(393, 395)),
      397 -> NGrammar.NSequence(397, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("enumValueOption"))), Seq(4, 363, 4, 392)),
      399 -> NGrammar.NSequence(399, Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))), Seq(400)),
      401 -> NGrammar.NSequence(401, Symbols.Sequence(Seq(Symbols.ExactChar(';'))), Seq(46)),
      403 -> NGrammar.NSequence(403, Symbols.Sequence(Seq(Symbols.Nonterminal("oneof"))), Seq(404)),
      405 -> NGrammar.NSequence(405, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('o'), Symbols.ExactChar('n'), Symbols.ExactChar('e'), Symbols.ExactChar('o'), Symbols.ExactChar('f')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("oneofName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('{'), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("oneofField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(406, 4, 409, 4, 249, 410, 4, 402)),
      408 -> NGrammar.NSequence(408, Symbols.Sequence(Seq(Symbols.ExactChar('o'), Symbols.ExactChar('n'), Symbols.ExactChar('e'), Symbols.ExactChar('o'), Symbols.ExactChar('f'))), Seq(39, 17, 70, 39, 179)),
      411 -> NGrammar.NSequence(411, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("oneofField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("oneofField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))))), Seq(410, 412)),
      414 -> NGrammar.NSequence(414, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("oneofField")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement")))))))), Seq(4, 415)),
      417 -> NGrammar.NSequence(417, Symbols.Sequence(Seq(Symbols.Nonterminal("oneofField"))), Seq(418)),
      419 -> NGrammar.NSequence(419, Symbols.Sequence(Seq(Symbols.Nonterminal("type"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldNumber"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOptions"), Symbols.Nonterminal("WS"), Symbols.ExactChar(']')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar(';'))), Seq(267, 4, 346, 4, 30, 4, 347, 349, 4, 46)),
      420 -> NGrammar.NSequence(420, Symbols.Sequence(Seq(Symbols.Nonterminal("mapField"))), Seq(421)),
      422 -> NGrammar.NSequence(422, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('a'), Symbols.ExactChar('p')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.ExactChar('<'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("keyType"), Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("type"), Symbols.Nonterminal("WS"), Symbols.ExactChar('>'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("mapName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldNumber"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldOptions"), Symbols.Nonterminal("WS"), Symbols.ExactChar(']')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar(';'))), Seq(423, 4, 426, 4, 427, 4, 363, 4, 267, 4, 431, 4, 432, 4, 30, 4, 347, 349, 4, 46)),
      425 -> NGrammar.NSequence(425, Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('a'), Symbols.ExactChar('p'))), Seq(61, 19, 37)),
      428 -> NGrammar.NSequence(428, Symbols.Sequence(Seq(Symbols.Join(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('t'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('3'), Symbols.ExactChar('2'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('f'), Symbols.ExactChar('i'), Symbols.ExactChar('x'), Symbols.ExactChar('e'), Symbols.ExactChar('d'), Symbols.ExactChar('6'), Symbols.ExactChar('4'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('b'), Symbols.ExactChar('o'), Symbols.ExactChar('o'), Symbols.ExactChar('l'))))))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('i'), Symbols.ExactChar('n'), Symbols.ExactChar('g'))))))))), Symbols.Nonterminal("Tk")))), Seq(429)),
      433 -> NGrammar.NSequence(433, Symbols.Sequence(Seq(Symbols.Nonterminal("reserved"))), Seq(434)),
      435 -> NGrammar.NSequence(435, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('s'), Symbols.ExactChar('e'), Symbols.ExactChar('r'), Symbols.ExactChar('v'), Symbols.ExactChar('e'), Symbols.ExactChar('d')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ranges")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("fieldNames")))))), Symbols.Nonterminal("WS"), Symbols.ExactChar(';'))), Seq(436, 4, 440, 4, 46)),
      438 -> NGrammar.NSequence(438, Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('s'), Symbols.ExactChar('e'), Symbols.ExactChar('r'), Symbols.ExactChar('v'), Symbols.ExactChar('e'), Symbols.ExactChar('d'))), Seq(38, 70, 15, 70, 38, 439, 70, 266)),
      442 -> NGrammar.NSequence(442, Symbols.Sequence(Seq(Symbols.Nonterminal("ranges"))), Seq(443)),
      444 -> NGrammar.NSequence(444, Symbols.Sequence(Seq(Symbols.Nonterminal("range"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("range")))))), 0))), Seq(445, 459)),
      446 -> NGrammar.NSequence(446, Symbols.Sequence(Seq(Symbols.Nonterminal("intLit"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('o')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("rangeEnd")))))), Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(187, 447)),
      450 -> NGrammar.NSequence(450, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('o')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("rangeEnd"))), Seq(4, 451, 4, 454)),
      453 -> NGrammar.NSequence(453, Symbols.Sequence(Seq(Symbols.ExactChar('t'), Symbols.ExactChar('o'))), Seq(18, 39)),
      455 -> NGrammar.NSequence(455, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('a'), Symbols.ExactChar('x')))), Symbols.Nonterminal("Tk")))), Seq(456)),
      458 -> NGrammar.NSequence(458, Symbols.Sequence(Seq(Symbols.ExactChar('m'), Symbols.ExactChar('a'), Symbols.ExactChar('x'))), Seq(61, 19, 20)),
      460 -> NGrammar.NSequence(460, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("range")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("range")))))))), Seq(459, 461)),
      463 -> NGrammar.NSequence(463, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("range"))), Seq(4, 363, 4, 445)),
      465 -> NGrammar.NSequence(465, Symbols.Sequence(Seq(Symbols.Nonterminal("fieldNames"))), Seq(466)),
      467 -> NGrammar.NSequence(467, Symbols.Sequence(Seq(Symbols.Nonterminal("fieldName"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldName")))))), 0))), Seq(346, 468)),
      469 -> NGrammar.NSequence(469, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldName")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldName")))))))), Seq(468, 470)),
      472 -> NGrammar.NSequence(472, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("fieldName"))), Seq(4, 363, 4, 346)),
      473 -> NGrammar.NSequence(473, Symbols.Sequence(Seq(Symbols.Nonterminal("service"))), Seq(474)),
      475 -> NGrammar.NSequence(475, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('e'), Symbols.ExactChar('r'), Symbols.ExactChar('v'), Symbols.ExactChar('i'), Symbols.ExactChar('c'), Symbols.ExactChar('e')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("serviceName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('{'), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("rpc")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(476, 4, 479, 4, 249, 480, 4, 402)),
      478 -> NGrammar.NSequence(478, Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('e'), Symbols.ExactChar('r'), Symbols.ExactChar('v'), Symbols.ExactChar('i'), Symbols.ExactChar('c'), Symbols.ExactChar('e'))), Seq(15, 70, 38, 439, 60, 80, 70)),
      481 -> NGrammar.NSequence(481, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("rpc")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("rpc")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))))), Seq(480, 482)),
      484 -> NGrammar.NSequence(484, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("rpc")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement")))))))), Seq(4, 485)),
      487 -> NGrammar.NSequence(487, Symbols.Sequence(Seq(Symbols.Nonterminal("rpc"))), Seq(488)),
      489 -> NGrammar.NSequence(489, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('p'), Symbols.ExactChar('c')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("rpcName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('('), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('m')))), Symbols.Nonterminal("Tk"))))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("messageType"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'), Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('t'), Symbols.ExactChar('u'), Symbols.ExactChar('r'), Symbols.ExactChar('n'), Symbols.ExactChar('s')))), Symbols.Nonterminal("Tk")), Symbols.Nonterminal("WS"), Symbols.ExactChar('('), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('m')))), Symbols.Nonterminal("Tk"))))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.Nonterminal("messageType"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("rpcEnding"))), Seq(490, 4, 493, 4, 162, 494, 4, 338, 4, 163, 4, 501, 4, 162, 494, 4, 338, 4, 163, 4, 504)),
      492 -> NGrammar.NSequence(492, Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('p'), Symbols.ExactChar('c'))), Seq(38, 37, 80)),
      497 -> NGrammar.NSequence(497, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('m')))), Symbols.Nonterminal("Tk")))), Seq(4, 498)),
      500 -> NGrammar.NSequence(500, Symbols.Sequence(Seq(Symbols.ExactChar('s'), Symbols.ExactChar('t'), Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('a'), Symbols.ExactChar('m'))), Seq(15, 18, 38, 70, 19, 61)),
      503 -> NGrammar.NSequence(503, Symbols.Sequence(Seq(Symbols.ExactChar('r'), Symbols.ExactChar('e'), Symbols.ExactChar('t'), Symbols.ExactChar('u'), Symbols.ExactChar('r'), Symbols.ExactChar('n'), Symbols.ExactChar('s'))), Seq(38, 70, 18, 77, 38, 17, 15)),
      505 -> NGrammar.NSequence(505, Symbols.Sequence(Seq(Symbols.ExactChar('{'), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(249, 506, 4, 402)),
      507 -> NGrammar.NSequence(507, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement"))))))))))))), Seq(506, 508)),
      510 -> NGrammar.NSequence(510, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("option")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("emptyStatement")))))))), Seq(4, 511))),
    1)

  sealed trait WithParseNode {
    val parseNode: Node
  }

  case class BoolConstant(value: BoolLit.Value)(override val parseNode: Node) extends Constant with WithParseNode

  case class BuiltinType(typ: BuiltinTypeEnum.Value)(override val parseNode: Node) extends Type with WithParseNode

  case class CharEscape(value: Char)(override val parseNode: Node) extends CharValue with WithParseNode

  sealed trait CharValue extends WithParseNode

  case class Character(value: Char)(override val parseNode: Node) extends CharValue with WithParseNode

  sealed trait Constant extends WithParseNode

  case class DecimalLit(value: String)(override val parseNode: Node) extends IntLit with WithParseNode

  case class DoubleQuoteStrLit(value: List[CharValue])(override val parseNode: Node) extends StrLit with WithParseNode

  case class EmptyStatement()(override val parseNode: Node) extends EnumBodyElem with MessageBodyElem with OneOfElem with ProtoDefElem with ServiceBodyElem with WithParseNode

  sealed trait EnumBodyElem extends WithParseNode

  case class EnumDef(name: Ident, body: List[EnumBodyElem])(override val parseNode: Node) extends MessageBodyElem with TopLevelDef with WithParseNode

  case class EnumFieldDef(name: Ident, minus: Boolean, value: IntLit, options: Option[List[EnumValueOption]])(override val parseNode: Node) extends EnumBodyElem with WithParseNode

  case class EnumType(firstDot: Boolean, parent: List[Char], name: Ident)(override val parseNode: Node) extends WithParseNode

  case class EnumValueOption(name: OptionName, value: Constant)(override val parseNode: Node) extends WithParseNode

  case class Exponent(sign: Option[Sign.Value], value: String)(override val parseNode: Node) extends WithParseNode

  case class Field(isRepeated: Boolean, typ: Type, name: Ident, fieldNumber: IntLit, options: Option[List[FieldOption]])(override val parseNode: Node) extends MessageBodyElem with WithParseNode

  case class FieldNames(names: List[Ident])(override val parseNode: Node) extends ReservedBody with WithParseNode

  case class FieldOption(name: OptionName, value: Constant)(override val parseNode: Node) extends WithParseNode

  case class FloatConstant(sign: Option[Sign.Value], value: FloatLit)(override val parseNode: Node) extends Constant with WithParseNode

  sealed trait FloatLit extends WithParseNode

  case class FloatLiteral(integral: String, fractional: String, exponent: Option[Exponent])(override val parseNode: Node) extends FloatLit with WithParseNode

  case class FullIdent(names: List[Ident])(override val parseNode: Node) extends Constant with OptionScope with WithParseNode

  case class HexEscape(value: String)(override val parseNode: Node) extends CharValue with WithParseNode

  case class HexLit(value: String)(override val parseNode: Node) extends IntLit with WithParseNode

  case class Ident(name: String)(override val parseNode: Node) extends OptionScope with WithParseNode

  case class Import(importType: Option[ImportType.Value], target: StrLit)(override val parseNode: Node) extends ProtoDefElem with WithParseNode

  case class Inf()(override val parseNode: Node) extends FloatLit with WithParseNode

  case class IntConstant(sign: Option[Sign.Value], value: IntLit)(override val parseNode: Node) extends Constant with WithParseNode

  sealed trait IntLit extends WithParseNode

  case class MapField(keyType: MapKeyType.Value, valueType: Type, mapName: Ident, number: IntLit, options: Option[List[FieldOption]])(override val parseNode: Node) extends MessageBodyElem with WithParseNode

  case class Message(name: Ident, body: List[MessageBodyElem])(override val parseNode: Node) extends MessageBodyElem with TopLevelDef with WithParseNode

  sealed trait MessageBodyElem extends WithParseNode

  case class MessageOrEnumType(name: MessageType)(override val parseNode: Node) extends Type with WithParseNode

  case class MessageType(firstDot: Boolean, parent: List[Char], name: Ident)(override val parseNode: Node) extends WithParseNode

  case class NaN()(override val parseNode: Node) extends FloatLit with WithParseNode

  case class OctalEscape(value: String)(override val parseNode: Node) extends CharValue with WithParseNode

  case class OctalLit(value: String)(override val parseNode: Node) extends IntLit with WithParseNode

  sealed trait OneOfElem extends WithParseNode

  case class OneofDef(name: Ident, elems: List[OneOfElem])(override val parseNode: Node) extends MessageBodyElem with WithParseNode

  case class OneofField(typ: Type, name: Ident, number: IntLit, options: Option[List[FieldOption]])(override val parseNode: Node) extends OneOfElem with WithParseNode

  case class OptionDef(name: OptionName, value: Constant)(override val parseNode: Node) extends EnumBodyElem with MessageBodyElem with OneOfElem with ProtoDefElem with ServiceBodyElem with WithParseNode

  case class OptionName(scope: OptionScope, name: List[Ident])(override val parseNode: Node) extends WithParseNode

  sealed trait OptionScope extends WithParseNode

  case class Package(name: FullIdent)(override val parseNode: Node) extends ProtoDefElem with WithParseNode

  case class Proto3(defs: List[ProtoDefElem])(override val parseNode: Node) extends WithParseNode

  sealed trait ProtoDefElem extends WithParseNode

  case class Range(start: IntLit, end: Option[RangeEnd])(override val parseNode: Node) extends WithParseNode

  sealed trait RangeEnd extends WithParseNode

  case class RangeEndMax()(override val parseNode: Node) extends RangeEnd with WithParseNode

  case class RangeEndValue(value: IntLit)(override val parseNode: Node) extends RangeEnd with WithParseNode

  case class Ranges(values: List[Range])(override val parseNode: Node) extends ReservedBody with WithParseNode

  case class Reserved(value: ReservedBody)(override val parseNode: Node) extends MessageBodyElem with WithParseNode

  sealed trait ReservedBody extends WithParseNode

  case class Rpc(name: Ident, isInputStream: Boolean, inputType: MessageType, isOutputStream: Boolean, outputType: MessageType, options: List[Option[OptionDef]])(override val parseNode: Node) extends ServiceBodyElem with WithParseNode

  case class Service(name: Ident, body: List[ServiceBodyElem])(override val parseNode: Node) extends TopLevelDef with WithParseNode

  sealed trait ServiceBodyElem extends WithParseNode

  case class SingleQuoteStrLit(value: List[CharValue])(override val parseNode: Node) extends StrLit with WithParseNode

  sealed trait StrLit extends WithParseNode

  case class StringConstant(value: StrLit)(override val parseNode: Node) extends Constant with WithParseNode

  sealed trait TopLevelDef extends ProtoDefElem with WithParseNode

  sealed trait Type extends WithParseNode

  object BoolLit extends Enumeration {
    val FALSE, TRUE = Value
  }

  object BuiltinTypeEnum extends Enumeration {
    val BOOL, BYTES, DOUBLE, FIXED32, FIXED64, FLOAT, INT32, INT64, SFIXED32, SFIXED64, SINT32, SINT64, STRING, UINT32, UINT64 = Value
  }

  object ImportType extends Enumeration {
    val PUBLIC, WEAK = Value
  }

  object MapKeyType extends Enumeration {
    val BOOL, FIXED32, FIXED64, INT32, INT64, SFIXED32, SFIXED64, SINT32, SINT64, STRING, UINT32, UINT64 = Value
  }

  object Sign extends Enumeration {
    val MINUS, PLUS = Value
  }

  def matchWS(node: Node): List[Char] = {
    val BindNode(v1, v2) = node
    val v7 = v1.id match {
      case 5 =>
        val v3 = v2.asInstanceOf[SequenceNode].children.head
        val v4 = unrollRepeat0(v3).map { elem =>
          val BindNode(v5, v6) = elem
          assert(v5.id == 9)
          v6.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        v4
    }
    v7
  }

  def matchBoolLit(node: Node): BoolLit.Value = {
    val BindNode(v8, v9) = node
    val v10 = v8.id match {
      case 173 =>
        BoolLit.TRUE
      case 176 =>
        BoolLit.FALSE
    }
    v10
  }

  def matchBuiltinType(node: Node): BuiltinTypeEnum.Value = {
    val BindNode(v11, v12) = node
    val v52 = v11.id match {
      case 270 =>
        val v13 = v12.asInstanceOf[SequenceNode].children.head
        val BindNode(v14, v15) = v13
        assert(v14.id == 271)
        val JoinNode(_, v16, _) = v15
        val BindNode(v17, v18) = v16
        assert(v17.id == 272)
        val BindNode(v19, v20) = v18
        val v51 = v19.id match {
          case 273 =>
            val BindNode(v21, v22) = v20
            assert(v21.id == 274)
            BuiltinTypeEnum.DOUBLE
          case 277 =>
            val BindNode(v23, v24) = v20
            assert(v23.id == 278)
            BuiltinTypeEnum.FLOAT
          case 312 =>
            val BindNode(v25, v26) = v20
            assert(v25.id == 313)
            BuiltinTypeEnum.FIXED64
          case 286 =>
            val BindNode(v27, v28) = v20
            assert(v27.id == 287)
            BuiltinTypeEnum.INT64
          case 308 =>
            val BindNode(v29, v30) = v20
            assert(v29.id == 309)
            BuiltinTypeEnum.FIXED32
          case 320 =>
            val BindNode(v31, v32) = v20
            assert(v31.id == 321)
            BuiltinTypeEnum.SFIXED64
          case 328 =>
            val BindNode(v33, v34) = v20
            assert(v33.id == 329)
            BuiltinTypeEnum.STRING
          case 300 =>
            val BindNode(v35, v36) = v20
            assert(v35.id == 301)
            BuiltinTypeEnum.SINT32
          case 316 =>
            val BindNode(v37, v38) = v20
            assert(v37.id == 317)
            BuiltinTypeEnum.SFIXED32
          case 332 =>
            val BindNode(v39, v40) = v20
            assert(v39.id == 333)
            BuiltinTypeEnum.BYTES
          case 281 =>
            val BindNode(v41, v42) = v20
            assert(v41.id == 282)
            BuiltinTypeEnum.INT32
          case 304 =>
            val BindNode(v43, v44) = v20
            assert(v43.id == 305)
            BuiltinTypeEnum.SINT64
          case 296 =>
            val BindNode(v45, v46) = v20
            assert(v45.id == 297)
            BuiltinTypeEnum.UINT64
          case 292 =>
            val BindNode(v47, v48) = v20
            assert(v47.id == 293)
            BuiltinTypeEnum.UINT32
          case 324 =>
            val BindNode(v49, v50) = v20
            assert(v49.id == 325)
            BuiltinTypeEnum.BOOL
        }
        v51
    }
    v52
  }

  def matchCharEscape(node: Node): CharEscape = {
    val BindNode(v53, v54) = node
    val v58 = v53.id match {
      case 103 =>
        val v55 = v54.asInstanceOf[SequenceNode].children(1)
        val BindNode(v56, v57) = v55
        assert(v56.id == 104)
        CharEscape(v57.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v54)
    }
    v58
  }

  def matchCharValue(node: Node): CharValue = {
    val BindNode(v59, v60) = node
    val v75 = v59.id match {
      case 87 =>
        val v61 = v60.asInstanceOf[SequenceNode].children.head
        val BindNode(v62, v63) = v61
        assert(v62.id == 88)
        matchHexEscape(v63)
      case 95 =>
        val v64 = v60.asInstanceOf[SequenceNode].children.head
        val BindNode(v65, v66) = v64
        assert(v65.id == 96)
        matchOctEscape(v66)
      case 101 =>
        val v67 = v60.asInstanceOf[SequenceNode].children.head
        val BindNode(v68, v69) = v67
        assert(v68.id == 102)
        matchCharEscape(v69)
      case 105 =>
        val v70 = v60.asInstanceOf[SequenceNode].children.head
        val BindNode(v71, v72) = v70
        assert(v71.id == 106)
        val BindNode(v73, v74) = v72
        assert(v73.id == 107)
        Character(v74.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v60)
    }
    v75
  }

  def matchConstant(node: Node): Constant = {
    val BindNode(v76, v77) = node
    val v107 = v76.id match {
      case 180 =>
        val v78 = v77.asInstanceOf[SequenceNode].children.head
        val BindNode(v79, v80) = v78
        assert(v79.id == 181)
        val BindNode(v81, v82) = v80
        val v83 = v81.id match {
          case 81 =>
            None
          case 182 =>
            Some(matchSign(v82))
        }
        val v84 = v77.asInstanceOf[SequenceNode].children(1)
        val BindNode(v85, v86) = v84
        assert(v85.id == 187)
        IntConstant(v83, matchIntLit(v86))(v77)
      case 215 =>
        val v87 = v77.asInstanceOf[SequenceNode].children.head
        val BindNode(v88, v89) = v87
        assert(v88.id == 181)
        val BindNode(v90, v91) = v89
        val v92 = v90.id match {
          case 81 =>
            None
          case 182 =>
            Some(matchSign(v91))
        }
        val v93 = v77.asInstanceOf[SequenceNode].children(1)
        val BindNode(v94, v95) = v93
        assert(v94.id == 216)
        FloatConstant(v92, matchFloatLit(v95))(v77)
      case 235 =>
        val v96 = v77.asInstanceOf[SequenceNode].children.head
        val BindNode(v97, v98) = v96
        assert(v97.id == 82)
        StringConstant(matchStrLit(v98))(v77)
      case 236 =>
        val v99 = v77.asInstanceOf[SequenceNode].children.head
        val BindNode(v100, v101) = v99
        assert(v100.id == 172)
        BoolConstant(matchBoolLit(v101))(v77)
      case 170 =>
        val v102 = v77.asInstanceOf[SequenceNode].children.head
        val BindNode(v103, v104) = v102
        assert(v103.id == 171)
        val BindNode(v105, v106) = v104
        assert(v105.id == 118)
        matchFullIdent(v106)
    }
    v107
  }

  def matchDecimalDigit(node: Node): Char = {
    val BindNode(v108, v109) = node
    val v113 = v108.id match {
      case 137 =>
        val v110 = v109.asInstanceOf[SequenceNode].children.head
        val BindNode(v111, v112) = v110
        assert(v111.id == 138)
        v112.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v113
  }

  def matchDecimalLit(node: Node): DecimalLit = {
    val BindNode(v114, v115) = node
    val v123 = v114.id match {
      case 199 =>
        val v116 = v115.asInstanceOf[SequenceNode].children.head
        val BindNode(v117, v118) = v116
        assert(v117.id == 200)
        val v119 = v115.asInstanceOf[SequenceNode].children(1)
        val v120 = unrollRepeat0(v119).map { elem =>
          val BindNode(v121, v122) = elem
          assert(v121.id == 136)
          matchDecimalDigit(v122)
        }
        DecimalLit(v118.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v120.map(x => x.toString).mkString(""))(v115)
    }
    v123
  }

  def matchDecimals(node: Node): String = {
    val BindNode(v124, v125) = node
    val v130 = v124.id match {
      case 219 =>
        val v126 = v125.asInstanceOf[SequenceNode].children.head
        val v127 = unrollRepeat1(v126).map { elem =>
          val BindNode(v128, v129) = elem
          assert(v128.id == 136)
          matchDecimalDigit(v129)
        }
        v127.map(x => x.toString).mkString("")
    }
    v130
  }

  def matchEmptyStatement(node: Node): EmptyStatement = {
    val BindNode(v131, v132) = node
    val v133 = v131.id match {
      case 401 =>
        EmptyStatement()(v132)
    }
    v133
  }

  def matchEnum(node: Node): EnumDef = {
    val BindNode(v134, v135) = node
    val v142 = v134.id match {
      case 367 =>
        val v136 = v135.asInstanceOf[SequenceNode].children(2)
        val BindNode(v137, v138) = v136
        assert(v137.id == 371)
        val v139 = v135.asInstanceOf[SequenceNode].children(4)
        val BindNode(v140, v141) = v139
        assert(v140.id == 372)
        EnumDef(matchEnumName(v138), matchEnumBody(v141))(v135)
    }
    v142
  }

  def matchEnumBody(node: Node): List[EnumBodyElem] = {
    val BindNode(v143, v144) = node
    val v175 = v143.id match {
      case 373 =>
        val v145 = v144.asInstanceOf[SequenceNode].children(1)
        val v146 = unrollRepeat0(v145).map { elem =>
          val BindNode(v147, v148) = elem
          assert(v147.id == 376)
          val BindNode(v149, v150) = v148
          val v174 = v149.id match {
            case 377 =>
              val BindNode(v151, v152) = v150
              assert(v151.id == 378)
              val v153 = v152.asInstanceOf[SequenceNode].children(1)
              val BindNode(v154, v155) = v153
              assert(v154.id == 379)
              val BindNode(v156, v157) = v155
              val v173 = v156.id match {
                case 148 =>
                  val BindNode(v158, v159) = v157
                  assert(v158.id == 149)
                  val v160 = v159.asInstanceOf[SequenceNode].children.head
                  val BindNode(v161, v162) = v160
                  assert(v161.id == 150)
                  matchOption(v162)
                case 380 =>
                  val BindNode(v163, v164) = v157
                  assert(v163.id == 381)
                  val v165 = v164.asInstanceOf[SequenceNode].children.head
                  val BindNode(v166, v167) = v165
                  assert(v166.id == 382)
                  matchEnumField(v167)
                case 398 =>
                  val BindNode(v168, v169) = v157
                  assert(v168.id == 399)
                  val v170 = v169.asInstanceOf[SequenceNode].children.head
                  val BindNode(v171, v172) = v170
                  assert(v171.id == 400)
                  matchEmptyStatement(v172)
              }
              v173
          }
          v174
        }
        v146
    }
    v175
  }

  def matchEnumField(node: Node): EnumFieldDef = {
    val BindNode(v176, v177) = node
    val v224 = v176.id match {
      case 383 =>
        val v178 = v177.asInstanceOf[SequenceNode].children.head
        val BindNode(v179, v180) = v178
        assert(v179.id == 120)
        val v181 = v177.asInstanceOf[SequenceNode].children(3)
        val BindNode(v182, v183) = v181
        assert(v182.id == 384)
        val BindNode(v184, v185) = v183
        val v194 = v184.id match {
          case 81 =>
            None
          case 385 =>
            val BindNode(v186, v187) = v185
            val v193 = v186.id match {
              case 386 =>
                val BindNode(v188, v189) = v187
                assert(v188.id == 387)
                val v190 = v189.asInstanceOf[SequenceNode].children(1)
                val BindNode(v191, v192) = v190
                assert(v191.id == 186)
                v192.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v193)
        }
        val v195 = v177.asInstanceOf[SequenceNode].children(5)
        val BindNode(v196, v197) = v195
        assert(v196.id == 187)
        val v198 = v177.asInstanceOf[SequenceNode].children(6)
        val BindNode(v199, v200) = v198
        assert(v199.id == 388)
        val BindNode(v201, v202) = v200
        val v223 = v201.id match {
          case 81 =>
            None
          case 389 =>
            val BindNode(v203, v204) = v202
            val v222 = v203.id match {
              case 390 =>
                val BindNode(v205, v206) = v204
                assert(v205.id == 391)
                val v207 = v206.asInstanceOf[SequenceNode].children(3)
                val BindNode(v208, v209) = v207
                assert(v208.id == 392)
                val v210 = v206.asInstanceOf[SequenceNode].children(4)
                val v211 = unrollRepeat0(v210).map { elem =>
                  val BindNode(v212, v213) = elem
                  assert(v212.id == 395)
                  val BindNode(v214, v215) = v213
                  val v221 = v214.id match {
                    case 396 =>
                      val BindNode(v216, v217) = v215
                      assert(v216.id == 397)
                      val v218 = v217.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v219, v220) = v218
                      assert(v219.id == 392)
                      matchEnumValueOption(v220)
                  }
                  v221
                }
                List(matchEnumValueOption(v209)) ++ v211
            }
            Some(v222)
        }
        EnumFieldDef(matchIdent(v180), v194.isDefined, matchIntLit(v197), v223)(v177)
    }
    v224
  }

  def matchEnumName(node: Node): Ident = {
    val BindNode(v225, v226) = node
    val v230 = v225.id match {
      case 159 =>
        val v227 = v226.asInstanceOf[SequenceNode].children.head
        val BindNode(v228, v229) = v227
        assert(v228.id == 120)
        matchIdent(v229)
    }
    v230
  }

  def matchEnumValueOption(node: Node): EnumValueOption = {
    val BindNode(v231, v232) = node
    val v239 = v231.id match {
      case 357 =>
        val v233 = v232.asInstanceOf[SequenceNode].children.head
        val BindNode(v234, v235) = v233
        assert(v234.id == 155)
        val v236 = v232.asInstanceOf[SequenceNode].children(4)
        val BindNode(v237, v238) = v236
        assert(v237.id == 169)
        EnumValueOption(matchOptionName(v235), matchConstant(v238))(v232)
    }
    v239
  }

  def matchExponent(node: Node): Exponent = {
    val BindNode(v240, v241) = node
    val v251 = v240.id match {
      case 225 =>
        val v242 = v241.asInstanceOf[SequenceNode].children(1)
        val BindNode(v243, v244) = v242
        assert(v243.id == 181)
        val BindNode(v245, v246) = v244
        val v247 = v245.id match {
          case 81 =>
            None
          case 182 =>
            Some(matchSign(v246))
        }
        val v248 = v241.asInstanceOf[SequenceNode].children(2)
        val BindNode(v249, v250) = v248
        assert(v249.id == 218)
        Exponent(v247, matchDecimals(v250))(v241)
    }
    v251
  }

  def matchField(node: Node): Field = {
    val BindNode(v252, v253) = node
    val v291 = v252.id match {
      case 258 =>
        val v254 = v253.asInstanceOf[SequenceNode].children.head
        val BindNode(v255, v256) = v254
        assert(v255.id == 259)
        val BindNode(v257, v258) = v256
        val v267 = v257.id match {
          case 81 =>
            None
          case 260 =>
            val BindNode(v259, v260) = v258
            val v266 = v259.id match {
              case 261 =>
                val BindNode(v261, v262) = v260
                assert(v261.id == 262)
                val v263 = v262.asInstanceOf[SequenceNode].children(1)
                val BindNode(v264, v265) = v263
                assert(v264.id == 4)
                matchWS(v265)
            }
            Some(v266)
        }
        val v268 = v253.asInstanceOf[SequenceNode].children(1)
        val BindNode(v269, v270) = v268
        assert(v269.id == 267)
        val v271 = v253.asInstanceOf[SequenceNode].children(3)
        val BindNode(v272, v273) = v271
        assert(v272.id == 346)
        val v274 = v253.asInstanceOf[SequenceNode].children(7)
        val BindNode(v275, v276) = v274
        assert(v275.id == 347)
        val v277 = v253.asInstanceOf[SequenceNode].children(8)
        val BindNode(v278, v279) = v277
        assert(v278.id == 349)
        val BindNode(v280, v281) = v279
        val v290 = v280.id match {
          case 81 =>
            None
          case 350 =>
            val BindNode(v282, v283) = v281
            val v289 = v282.id match {
              case 351 =>
                val BindNode(v284, v285) = v283
                assert(v284.id == 352)
                val v286 = v285.asInstanceOf[SequenceNode].children(3)
                val BindNode(v287, v288) = v286
                assert(v287.id == 354)
                matchFieldOptions(v288)
            }
            Some(v289)
        }
        Field(v267.isDefined, matchType(v270), matchFieldName(v273), matchFieldNumber(v276), v290)(v253)
    }
    v291
  }

  def matchFieldName(node: Node): Ident = {
    val BindNode(v292, v293) = node
    val v297 = v292.id match {
      case 159 =>
        val v294 = v293.asInstanceOf[SequenceNode].children.head
        val BindNode(v295, v296) = v294
        assert(v295.id == 120)
        matchIdent(v296)
    }
    v297
  }

  def matchFieldNames(node: Node): FieldNames = {
    val BindNode(v298, v299) = node
    val v315 = v298.id match {
      case 467 =>
        val v300 = v299.asInstanceOf[SequenceNode].children.head
        val BindNode(v301, v302) = v300
        assert(v301.id == 346)
        val v303 = v299.asInstanceOf[SequenceNode].children(1)
        val v304 = unrollRepeat0(v303).map { elem =>
          val BindNode(v305, v306) = elem
          assert(v305.id == 470)
          val BindNode(v307, v308) = v306
          val v314 = v307.id match {
            case 471 =>
              val BindNode(v309, v310) = v308
              assert(v309.id == 472)
              val v311 = v310.asInstanceOf[SequenceNode].children(3)
              val BindNode(v312, v313) = v311
              assert(v312.id == 346)
              matchFieldName(v313)
          }
          v314
        }
        FieldNames(List(matchFieldName(v302)) ++ v304)(v299)
    }
    v315
  }

  def matchFieldNumber(node: Node): IntLit = {
    val BindNode(v316, v317) = node
    val v321 = v316.id match {
      case 348 =>
        val v318 = v317.asInstanceOf[SequenceNode].children.head
        val BindNode(v319, v320) = v318
        assert(v319.id == 187)
        matchIntLit(v320)
    }
    v321
  }

  def matchFieldOption(node: Node): FieldOption = {
    val BindNode(v322, v323) = node
    val v330 = v322.id match {
      case 357 =>
        val v324 = v323.asInstanceOf[SequenceNode].children.head
        val BindNode(v325, v326) = v324
        assert(v325.id == 155)
        val v327 = v323.asInstanceOf[SequenceNode].children(4)
        val BindNode(v328, v329) = v327
        assert(v328.id == 169)
        FieldOption(matchOptionName(v326), matchConstant(v329))(v323)
    }
    v330
  }

  def matchFieldOptions(node: Node): List[FieldOption] = {
    val BindNode(v331, v332) = node
    val v348 = v331.id match {
      case 355 =>
        val v333 = v332.asInstanceOf[SequenceNode].children.head
        val BindNode(v334, v335) = v333
        assert(v334.id == 356)
        val v336 = v332.asInstanceOf[SequenceNode].children(1)
        val v337 = unrollRepeat0(v336).map { elem =>
          val BindNode(v338, v339) = elem
          assert(v338.id == 360)
          val BindNode(v340, v341) = v339
          val v347 = v340.id match {
            case 361 =>
              val BindNode(v342, v343) = v341
              assert(v342.id == 362)
              val v344 = v343.asInstanceOf[SequenceNode].children(3)
              val BindNode(v345, v346) = v344
              assert(v345.id == 356)
              matchFieldOption(v346)
          }
          v347
        }
        List(matchFieldOption(v335)) ++ v337
    }
    v348
  }

  def matchFloatLit(node: Node): FloatLit = {
    val BindNode(v349, v350) = node
    val v382 = v349.id match {
      case 217 =>
        val v351 = v350.asInstanceOf[SequenceNode].children.head
        val BindNode(v352, v353) = v351
        assert(v352.id == 218)
        val v355 = v350.asInstanceOf[SequenceNode].children(2)
        val BindNode(v356, v357) = v355
        assert(v356.id == 222)
        val BindNode(v358, v359) = v357
        val v360 = v358.id match {
          case 81 =>
            None
          case 218 =>
            Some(matchDecimals(v359))
        }
        val v354 = v360
        val v361 = v350.asInstanceOf[SequenceNode].children(3)
        val BindNode(v362, v363) = v361
        assert(v362.id == 223)
        val BindNode(v364, v365) = v363
        val v366 = v364.id match {
          case 81 =>
            None
          case 224 =>
            Some(matchExponent(v365))
        }
        FloatLiteral(matchDecimals(v353), if (v354.isDefined) v354.get else "", v366)(v350)
      case 228 =>
        val v367 = v350.asInstanceOf[SequenceNode].children(1)
        val BindNode(v368, v369) = v367
        assert(v368.id == 218)
        val v370 = v350.asInstanceOf[SequenceNode].children(2)
        val BindNode(v371, v372) = v370
        assert(v371.id == 223)
        val BindNode(v373, v374) = v372
        val v375 = v373.id match {
          case 81 =>
            None
          case 224 =>
            Some(matchExponent(v374))
        }
        FloatLiteral("", matchDecimals(v369), v375)(v350)
      case 232 =>
        NaN()(v350)
      case 229 =>
        Inf()(v350)
      case 227 =>
        val v376 = v350.asInstanceOf[SequenceNode].children.head
        val BindNode(v377, v378) = v376
        assert(v377.id == 218)
        val v379 = v350.asInstanceOf[SequenceNode].children(1)
        val BindNode(v380, v381) = v379
        assert(v380.id == 224)
        FloatLiteral(matchDecimals(v378), "", Some(matchExponent(v381)))(v350)
    }
    v382
  }

  def matchFullIdent(node: Node): FullIdent = {
    val BindNode(v383, v384) = node
    val v400 = v383.id match {
      case 119 =>
        val v385 = v384.asInstanceOf[SequenceNode].children.head
        val BindNode(v386, v387) = v385
        assert(v386.id == 120)
        val v388 = v384.asInstanceOf[SequenceNode].children(1)
        val v389 = unrollRepeat0(v388).map { elem =>
          val BindNode(v390, v391) = elem
          assert(v390.id == 144)
          val BindNode(v392, v393) = v391
          val v399 = v392.id match {
            case 145 =>
              val BindNode(v394, v395) = v393
              assert(v394.id == 146)
              val v396 = v395.asInstanceOf[SequenceNode].children(1)
              val BindNode(v397, v398) = v396
              assert(v397.id == 120)
              matchIdent(v398)
          }
          v399
        }
        FullIdent(List(matchIdent(v387)) ++ v389)(v384)
    }
    v400
  }

  def matchHexDigit(node: Node): Char = {
    val BindNode(v401, v402) = node
    val v406 = v401.id match {
      case 93 =>
        val v403 = v402.asInstanceOf[SequenceNode].children.head
        val BindNode(v404, v405) = v403
        assert(v404.id == 94)
        v405.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v406
  }

  def matchHexEscape(node: Node): HexEscape = {
    val BindNode(v407, v408) = node
    val v415 = v407.id match {
      case 89 =>
        val v409 = v408.asInstanceOf[SequenceNode].children(2)
        val BindNode(v410, v411) = v409
        assert(v410.id == 92)
        val v412 = v408.asInstanceOf[SequenceNode].children(3)
        val BindNode(v413, v414) = v412
        assert(v413.id == 92)
        HexEscape(matchHexDigit(v411).toString + matchHexDigit(v414).toString)(v408)
    }
    v415
  }

  def matchHexLit(node: Node): HexLit = {
    val BindNode(v416, v417) = node
    val v422 = v416.id match {
      case 212 =>
        val v418 = v417.asInstanceOf[SequenceNode].children(2)
        val v419 = unrollRepeat1(v418).map { elem =>
          val BindNode(v420, v421) = elem
          assert(v420.id == 92)
          matchHexDigit(v421)
        }
        HexLit(v419.map(x => x.toString).mkString(""))(v417)
    }
    v422
  }

  def matchIdent(node: Node): Ident = {
    val BindNode(v423, v424) = node
    val v460 = v423.id match {
      case 121 =>
        val v425 = v424.asInstanceOf[SequenceNode].children.head
        val BindNode(v426, v427) = v425
        assert(v426.id == 122)
        val BindNode(v428, v429) = v427
        assert(v428.id == 123)
        val BindNode(v430, v431) = v429
        val v459 = v430.id match {
          case 124 =>
            val BindNode(v432, v433) = v431
            assert(v432.id == 125)
            val v434 = v433.asInstanceOf[SequenceNode].children.head
            val BindNode(v435, v436) = v434
            assert(v435.id == 126)
            val v437 = v433.asInstanceOf[SequenceNode].children(1)
            val v438 = unrollRepeat0(v437).map { elem =>
              val BindNode(v439, v440) = elem
              assert(v439.id == 131)
              val BindNode(v441, v442) = v440
              val v458 = v441.id match {
                case 132 =>
                  val BindNode(v443, v444) = v442
                  assert(v443.id == 133)
                  val v445 = v444.asInstanceOf[SequenceNode].children.head
                  val BindNode(v446, v447) = v445
                  assert(v446.id == 126)
                  matchLetter(v447)
                case 134 =>
                  val BindNode(v448, v449) = v442
                  assert(v448.id == 135)
                  val v450 = v449.asInstanceOf[SequenceNode].children.head
                  val BindNode(v451, v452) = v450
                  assert(v451.id == 136)
                  matchDecimalDigit(v452)
                case 139 =>
                  val BindNode(v453, v454) = v442
                  assert(v453.id == 140)
                  val v455 = v454.asInstanceOf[SequenceNode].children.head
                  val BindNode(v456, v457) = v455
                  assert(v456.id == 141)
                  v457.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
              }
              v458
            }
            matchLetter(v436).toString + v438.map(x => x.toString).mkString("")
        }
        Ident(v459)(v424)
    }
    v460
  }

  def matchImport(node: Node): Import = {
    val BindNode(v461, v462) = node
    val v479 = v461.id match {
      case 56 =>
        val v463 = v462.asInstanceOf[SequenceNode].children(1)
        val BindNode(v464, v465) = v463
        assert(v464.id == 62)
        val BindNode(v466, v467) = v465
        val v475 = v466.id match {
          case 81 =>
            None
          case 63 =>
            val BindNode(v468, v469) = v467
            val v474 = v468.id match {
              case 64 =>
                val BindNode(v470, v471) = v469
                assert(v470.id == 65)
                ImportType.WEAK
              case 72 =>
                val BindNode(v472, v473) = v469
                assert(v472.id == 73)
                ImportType.PUBLIC
            }
            Some(v474)
        }
        val v476 = v462.asInstanceOf[SequenceNode].children(3)
        val BindNode(v477, v478) = v476
        assert(v477.id == 82)
        Import(v475, matchStrLit(v478))(v462)
    }
    v479
  }

  def matchIntLit(node: Node): IntLit = {
    val BindNode(v480, v481) = node
    val v510 = v480.id match {
      case 188 =>
        val v482 = v481.asInstanceOf[SequenceNode].children.head
        val BindNode(v483, v484) = v482
        assert(v483.id == 189)
        val BindNode(v485, v486) = v484
        assert(v485.id == 190)
        val BindNode(v487, v488) = v486
        val v509 = v487.id match {
          case 191 =>
            val BindNode(v489, v490) = v488
            assert(v489.id == 192)
            val v491 = v490.asInstanceOf[SequenceNode].children.head
            val BindNode(v492, v493) = v491
            assert(v492.id == 193)
            matchZeroLit(v493)
          case 196 =>
            val BindNode(v494, v495) = v488
            assert(v494.id == 197)
            val v496 = v495.asInstanceOf[SequenceNode].children.head
            val BindNode(v497, v498) = v496
            assert(v497.id == 198)
            matchDecimalLit(v498)
          case 203 =>
            val BindNode(v499, v500) = v488
            assert(v499.id == 204)
            val v501 = v500.asInstanceOf[SequenceNode].children.head
            val BindNode(v502, v503) = v501
            assert(v502.id == 205)
            matchOctalLit(v503)
          case 209 =>
            val BindNode(v504, v505) = v488
            assert(v504.id == 210)
            val v506 = v505.asInstanceOf[SequenceNode].children.head
            val BindNode(v507, v508) = v506
            assert(v507.id == 211)
            matchHexLit(v508)
        }
        v509
    }
    v510
  }

  def matchKeyType(node: Node): MapKeyType.Value = {
    val BindNode(v511, v512) = node
    val v546 = v511.id match {
      case 428 =>
        val v513 = v512.asInstanceOf[SequenceNode].children.head
        val BindNode(v514, v515) = v513
        assert(v514.id == 429)
        val JoinNode(_, v516, _) = v515
        val BindNode(v517, v518) = v516
        assert(v517.id == 430)
        val BindNode(v519, v520) = v518
        val v545 = v519.id match {
          case 312 =>
            val BindNode(v521, v522) = v520
            assert(v521.id == 313)
            MapKeyType.FIXED64
          case 286 =>
            val BindNode(v523, v524) = v520
            assert(v523.id == 287)
            MapKeyType.INT64
          case 308 =>
            val BindNode(v525, v526) = v520
            assert(v525.id == 309)
            MapKeyType.FIXED32
          case 320 =>
            val BindNode(v527, v528) = v520
            assert(v527.id == 321)
            MapKeyType.SFIXED64
          case 328 =>
            val BindNode(v529, v530) = v520
            assert(v529.id == 329)
            MapKeyType.STRING
          case 300 =>
            val BindNode(v531, v532) = v520
            assert(v531.id == 301)
            MapKeyType.SINT32
          case 316 =>
            val BindNode(v533, v534) = v520
            assert(v533.id == 317)
            MapKeyType.SFIXED32
          case 281 =>
            val BindNode(v535, v536) = v520
            assert(v535.id == 282)
            MapKeyType.INT32
          case 304 =>
            val BindNode(v537, v538) = v520
            assert(v537.id == 305)
            MapKeyType.SINT64
          case 296 =>
            val BindNode(v539, v540) = v520
            assert(v539.id == 297)
            MapKeyType.UINT64
          case 292 =>
            val BindNode(v541, v542) = v520
            assert(v541.id == 293)
            MapKeyType.UINT32
          case 324 =>
            val BindNode(v543, v544) = v520
            assert(v543.id == 325)
            MapKeyType.BOOL
        }
        v545
    }
    v546
  }

  def matchLetter(node: Node): Char = {
    val BindNode(v547, v548) = node
    val v552 = v547.id match {
      case 127 =>
        val v549 = v548.asInstanceOf[SequenceNode].children.head
        val BindNode(v550, v551) = v549
        assert(v550.id == 128)
        v551.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v552
  }

  def matchMapField(node: Node): MapField = {
    val BindNode(v553, v554) = node
    val v581 = v553.id match {
      case 422 =>
        val v555 = v554.asInstanceOf[SequenceNode].children(4)
        val BindNode(v556, v557) = v555
        assert(v556.id == 427)
        val v558 = v554.asInstanceOf[SequenceNode].children(8)
        val BindNode(v559, v560) = v558
        assert(v559.id == 267)
        val v561 = v554.asInstanceOf[SequenceNode].children(12)
        val BindNode(v562, v563) = v561
        assert(v562.id == 432)
        val v564 = v554.asInstanceOf[SequenceNode].children(16)
        val BindNode(v565, v566) = v564
        assert(v565.id == 347)
        val v567 = v554.asInstanceOf[SequenceNode].children(17)
        val BindNode(v568, v569) = v567
        assert(v568.id == 349)
        val BindNode(v570, v571) = v569
        val v580 = v570.id match {
          case 81 =>
            None
          case 350 =>
            val BindNode(v572, v573) = v571
            val v579 = v572.id match {
              case 351 =>
                val BindNode(v574, v575) = v573
                assert(v574.id == 352)
                val v576 = v575.asInstanceOf[SequenceNode].children(3)
                val BindNode(v577, v578) = v576
                assert(v577.id == 354)
                matchFieldOptions(v578)
            }
            Some(v579)
        }
        MapField(matchKeyType(v557), matchType(v560), matchMapName(v563), matchFieldNumber(v566), v580)(v554)
    }
    v581
  }

  def matchMapName(node: Node): Ident = {
    val BindNode(v582, v583) = node
    val v587 = v582.id match {
      case 159 =>
        val v584 = v583.asInstanceOf[SequenceNode].children.head
        val BindNode(v585, v586) = v584
        assert(v585.id == 120)
        matchIdent(v586)
    }
    v587
  }

  def matchMessage(node: Node): Message = {
    val BindNode(v588, v589) = node
    val v596 = v588.id match {
      case 242 =>
        val v590 = v589.asInstanceOf[SequenceNode].children(2)
        val BindNode(v591, v592) = v590
        assert(v591.id == 246)
        val v593 = v589.asInstanceOf[SequenceNode].children(4)
        val BindNode(v594, v595) = v593
        assert(v594.id == 247)
        Message(matchMessageName(v592), matchMessageBody(v595))(v589)
    }
    v596
  }

  def matchMessageBody(node: Node): List[MessageBodyElem] = {
    val BindNode(v597, v598) = node
    val v611 = v597.id match {
      case 248 =>
        val v599 = v598.asInstanceOf[SequenceNode].children(1)
        val v600 = unrollRepeat0(v599).map { elem =>
          val BindNode(v601, v602) = elem
          assert(v601.id == 252)
          val BindNode(v603, v604) = v602
          val v610 = v603.id match {
            case 253 =>
              val BindNode(v605, v606) = v604
              assert(v605.id == 254)
              val v607 = v606.asInstanceOf[SequenceNode].children(1)
              val BindNode(v608, v609) = v607
              assert(v608.id == 255)
              matchMessageBodyElem(v609)
          }
          v610
        }
        v600
    }
    v611
  }

  def matchMessageBodyElem(node: Node): MessageBodyElem = {
    val BindNode(v612, v613) = node
    val v638 = v612.id match {
      case 256 =>
        val v614 = v613.asInstanceOf[SequenceNode].children.head
        val BindNode(v615, v616) = v614
        assert(v615.id == 257)
        matchField(v616)
      case 403 =>
        val v617 = v613.asInstanceOf[SequenceNode].children.head
        val BindNode(v618, v619) = v617
        assert(v618.id == 404)
        matchOneof(v619)
      case 420 =>
        val v620 = v613.asInstanceOf[SequenceNode].children.head
        val BindNode(v621, v622) = v620
        assert(v621.id == 421)
        matchMapField(v622)
      case 365 =>
        val v623 = v613.asInstanceOf[SequenceNode].children.head
        val BindNode(v624, v625) = v623
        assert(v624.id == 366)
        matchEnum(v625)
      case 399 =>
        val v626 = v613.asInstanceOf[SequenceNode].children.head
        val BindNode(v627, v628) = v626
        assert(v627.id == 400)
        matchEmptyStatement(v628)
      case 240 =>
        val v629 = v613.asInstanceOf[SequenceNode].children.head
        val BindNode(v630, v631) = v629
        assert(v630.id == 241)
        matchMessage(v631)
      case 433 =>
        val v632 = v613.asInstanceOf[SequenceNode].children.head
        val BindNode(v633, v634) = v632
        assert(v633.id == 434)
        matchReserved(v634)
      case 149 =>
        val v635 = v613.asInstanceOf[SequenceNode].children.head
        val BindNode(v636, v637) = v635
        assert(v636.id == 150)
        matchOption(v637)
    }
    v638
  }

  def matchMessageName(node: Node): Ident = {
    val BindNode(v639, v640) = node
    val v644 = v639.id match {
      case 159 =>
        val v641 = v640.asInstanceOf[SequenceNode].children.head
        val BindNode(v642, v643) = v641
        assert(v642.id == 120)
        matchIdent(v643)
    }
    v644
  }

  def matchMessageType(node: Node): MessageType = {
    val BindNode(v645, v646) = node
    val v668 = v645.id match {
      case 339 =>
        val v647 = v646.asInstanceOf[SequenceNode].children.head
        val BindNode(v648, v649) = v647
        assert(v648.id == 340)
        val BindNode(v650, v651) = v649
        val v652 = v650.id match {
          case 81 =>
            None
          case 147 =>
            Some(v651.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)
        }
        val v653 = v646.asInstanceOf[SequenceNode].children(1)
        val v654 = unrollRepeat0(v653).map { elem =>
          val BindNode(v655, v656) = elem
          assert(v655.id == 343)
          val BindNode(v657, v658) = v656
          val v664 = v657.id match {
            case 344 =>
              val BindNode(v659, v660) = v658
              assert(v659.id == 345)
              val v661 = v660.asInstanceOf[SequenceNode].children(1)
              val BindNode(v662, v663) = v661
              assert(v662.id == 147)
              v663.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
          }
          v664
        }
        val v665 = v646.asInstanceOf[SequenceNode].children(2)
        val BindNode(v666, v667) = v665
        assert(v666.id == 246)
        MessageType(v652.isDefined, v654, matchMessageName(v667))(v646)
    }
    v668
  }

  def matchOctEscape(node: Node): OctalEscape = {
    val BindNode(v669, v670) = node
    val v680 = v669.id match {
      case 97 =>
        val v671 = v670.asInstanceOf[SequenceNode].children(1)
        val BindNode(v672, v673) = v671
        assert(v672.id == 98)
        val v674 = v670.asInstanceOf[SequenceNode].children(2)
        val BindNode(v675, v676) = v674
        assert(v675.id == 98)
        val v677 = v670.asInstanceOf[SequenceNode].children(3)
        val BindNode(v678, v679) = v677
        assert(v678.id == 98)
        OctalEscape(matchOctalDigit(v673).toString + matchOctalDigit(v676).toString + matchOctalDigit(v679).toString)(v670)
    }
    v680
  }

  def matchOctalDigit(node: Node): Char = {
    val BindNode(v681, v682) = node
    val v686 = v681.id match {
      case 99 =>
        val v683 = v682.asInstanceOf[SequenceNode].children.head
        val BindNode(v684, v685) = v683
        assert(v684.id == 100)
        v685.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v686
  }

  def matchOctalLit(node: Node): OctalLit = {
    val BindNode(v687, v688) = node
    val v693 = v687.id match {
      case 206 =>
        val v689 = v688.asInstanceOf[SequenceNode].children(1)
        val v690 = unrollRepeat1(v689).map { elem =>
          val BindNode(v691, v692) = elem
          assert(v691.id == 98)
          matchOctalDigit(v692)
        }
        OctalLit(v690.map(x => x.toString).mkString(""))(v688)
    }
    v693
  }

  def matchOneof(node: Node): OneofDef = {
    val BindNode(v694, v695) = node
    val v729 = v694.id match {
      case 405 =>
        val v696 = v695.asInstanceOf[SequenceNode].children(2)
        val BindNode(v697, v698) = v696
        assert(v697.id == 409)
        val v699 = v695.asInstanceOf[SequenceNode].children(5)
        val v700 = unrollRepeat0(v699).map { elem =>
          val BindNode(v701, v702) = elem
          assert(v701.id == 412)
          val BindNode(v703, v704) = v702
          val v728 = v703.id match {
            case 413 =>
              val BindNode(v705, v706) = v704
              assert(v705.id == 414)
              val v707 = v706.asInstanceOf[SequenceNode].children(1)
              val BindNode(v708, v709) = v707
              assert(v708.id == 415)
              val BindNode(v710, v711) = v709
              val v727 = v710.id match {
                case 148 =>
                  val BindNode(v712, v713) = v711
                  assert(v712.id == 149)
                  val v714 = v713.asInstanceOf[SequenceNode].children.head
                  val BindNode(v715, v716) = v714
                  assert(v715.id == 150)
                  matchOption(v716)
                case 416 =>
                  val BindNode(v717, v718) = v711
                  assert(v717.id == 417)
                  val v719 = v718.asInstanceOf[SequenceNode].children.head
                  val BindNode(v720, v721) = v719
                  assert(v720.id == 418)
                  matchOneofField(v721)
                case 398 =>
                  val BindNode(v722, v723) = v711
                  assert(v722.id == 399)
                  val v724 = v723.asInstanceOf[SequenceNode].children.head
                  val BindNode(v725, v726) = v724
                  assert(v725.id == 400)
                  matchEmptyStatement(v726)
              }
              v727
          }
          v728
        }
        OneofDef(matchOneofName(v698), v700)(v695)
    }
    v729
  }

  def matchOneofField(node: Node): OneofField = {
    val BindNode(v730, v731) = node
    val v755 = v730.id match {
      case 419 =>
        val v732 = v731.asInstanceOf[SequenceNode].children.head
        val BindNode(v733, v734) = v732
        assert(v733.id == 267)
        val v735 = v731.asInstanceOf[SequenceNode].children(2)
        val BindNode(v736, v737) = v735
        assert(v736.id == 346)
        val v738 = v731.asInstanceOf[SequenceNode].children(6)
        val BindNode(v739, v740) = v738
        assert(v739.id == 347)
        val v741 = v731.asInstanceOf[SequenceNode].children(7)
        val BindNode(v742, v743) = v741
        assert(v742.id == 349)
        val BindNode(v744, v745) = v743
        val v754 = v744.id match {
          case 81 =>
            None
          case 350 =>
            val BindNode(v746, v747) = v745
            val v753 = v746.id match {
              case 351 =>
                val BindNode(v748, v749) = v747
                assert(v748.id == 352)
                val v750 = v749.asInstanceOf[SequenceNode].children(3)
                val BindNode(v751, v752) = v750
                assert(v751.id == 354)
                matchFieldOptions(v752)
            }
            Some(v753)
        }
        OneofField(matchType(v734), matchFieldName(v737), matchFieldNumber(v740), v754)(v731)
    }
    v755
  }

  def matchOneofName(node: Node): Ident = {
    val BindNode(v756, v757) = node
    val v761 = v756.id match {
      case 159 =>
        val v758 = v757.asInstanceOf[SequenceNode].children.head
        val BindNode(v759, v760) = v758
        assert(v759.id == 120)
        matchIdent(v760)
    }
    v761
  }

  def matchOption(node: Node): OptionDef = {
    val BindNode(v762, v763) = node
    val v770 = v762.id match {
      case 151 =>
        val v764 = v763.asInstanceOf[SequenceNode].children(2)
        val BindNode(v765, v766) = v764
        assert(v765.id == 155)
        val v767 = v763.asInstanceOf[SequenceNode].children(6)
        val BindNode(v768, v769) = v767
        assert(v768.id == 169)
        OptionDef(matchOptionName(v766), matchConstant(v769))(v763)
    }
    v770
  }

  def matchOptionName(node: Node): OptionName = {
    val BindNode(v771, v772) = node
    val v801 = v771.id match {
      case 156 =>
        val v773 = v772.asInstanceOf[SequenceNode].children.head
        val BindNode(v774, v775) = v773
        assert(v774.id == 157)
        val BindNode(v776, v777) = v775
        val v788 = v776.id match {
          case 158 =>
            val BindNode(v778, v779) = v777
            assert(v778.id == 159)
            val v780 = v779.asInstanceOf[SequenceNode].children.head
            val BindNode(v781, v782) = v780
            assert(v781.id == 120)
            matchIdent(v782)
          case 160 =>
            val BindNode(v783, v784) = v777
            assert(v783.id == 161)
            val v785 = v784.asInstanceOf[SequenceNode].children(2)
            val BindNode(v786, v787) = v785
            assert(v786.id == 118)
            matchFullIdent(v787)
        }
        val v789 = v772.asInstanceOf[SequenceNode].children(1)
        val v790 = unrollRepeat0(v789).map { elem =>
          val BindNode(v791, v792) = elem
          assert(v791.id == 166)
          val BindNode(v793, v794) = v792
          val v800 = v793.id match {
            case 167 =>
              val BindNode(v795, v796) = v794
              assert(v795.id == 168)
              val v797 = v796.asInstanceOf[SequenceNode].children(2)
              val BindNode(v798, v799) = v797
              assert(v798.id == 120)
              matchIdent(v799)
          }
          v800
        }
        OptionName(v788, v790)(v772)
    }
    v801
  }

  def matchPackage(node: Node): Package = {
    val BindNode(v802, v803) = node
    val v807 = v802.id match {
      case 113 =>
        val v804 = v803.asInstanceOf[SequenceNode].children(2)
        val BindNode(v805, v806) = v804
        assert(v805.id == 118)
        Package(matchFullIdent(v806))(v803)
    }
    v807
  }

  def matchProto3(node: Node): Proto3 = {
    val BindNode(v808, v809) = node
    val v850 = v808.id match {
      case 3 =>
        val v810 = v809.asInstanceOf[SequenceNode].children(2)
        val v811 = unrollRepeat0(v810).map { elem =>
          val BindNode(v812, v813) = elem
          assert(v812.id == 49)
          val BindNode(v814, v815) = v813
          val v849 = v814.id match {
            case 50 =>
              val BindNode(v816, v817) = v815
              assert(v816.id == 51)
              val v818 = v817.asInstanceOf[SequenceNode].children(1)
              val BindNode(v819, v820) = v818
              assert(v819.id == 52)
              val BindNode(v821, v822) = v820
              val v848 = v821.id match {
                case 148 =>
                  val BindNode(v823, v824) = v822
                  assert(v823.id == 149)
                  val v825 = v824.asInstanceOf[SequenceNode].children.head
                  val BindNode(v826, v827) = v825
                  assert(v826.id == 150)
                  matchOption(v827)
                case 237 =>
                  val BindNode(v828, v829) = v822
                  assert(v828.id == 238)
                  val v830 = v829.asInstanceOf[SequenceNode].children.head
                  val BindNode(v831, v832) = v830
                  assert(v831.id == 239)
                  matchTopLevelDef(v832)
                case 53 =>
                  val BindNode(v833, v834) = v822
                  assert(v833.id == 54)
                  val v835 = v834.asInstanceOf[SequenceNode].children.head
                  val BindNode(v836, v837) = v835
                  assert(v836.id == 55)
                  matchImport(v837)
                case 110 =>
                  val BindNode(v838, v839) = v822
                  assert(v838.id == 111)
                  val v840 = v839.asInstanceOf[SequenceNode].children.head
                  val BindNode(v841, v842) = v840
                  assert(v841.id == 112)
                  matchPackage(v842)
                case 398 =>
                  val BindNode(v843, v844) = v822
                  assert(v843.id == 399)
                  val v845 = v844.asInstanceOf[SequenceNode].children.head
                  val BindNode(v846, v847) = v845
                  assert(v846.id == 400)
                  matchEmptyStatement(v847)
              }
              v848
          }
          v849
        }
        Proto3(v811)(v809)
    }
    v850
  }

  def matchRange(node: Node): Range = {
    val BindNode(v851, v852) = node
    val v870 = v851.id match {
      case 446 =>
        val v853 = v852.asInstanceOf[SequenceNode].children.head
        val BindNode(v854, v855) = v853
        assert(v854.id == 187)
        val v856 = v852.asInstanceOf[SequenceNode].children(1)
        val BindNode(v857, v858) = v856
        assert(v857.id == 447)
        val BindNode(v859, v860) = v858
        val v869 = v859.id match {
          case 81 =>
            None
          case 448 =>
            val BindNode(v861, v862) = v860
            val v868 = v861.id match {
              case 449 =>
                val BindNode(v863, v864) = v862
                assert(v863.id == 450)
                val v865 = v864.asInstanceOf[SequenceNode].children(3)
                val BindNode(v866, v867) = v865
                assert(v866.id == 454)
                matchRangeEnd(v867)
            }
            Some(v868)
        }
        Range(matchIntLit(v855), v869)(v852)
    }
    v870
  }

  def matchRangeEnd(node: Node): RangeEnd = {
    val BindNode(v871, v872) = node
    val v876 = v871.id match {
      case 348 =>
        val v873 = v872.asInstanceOf[SequenceNode].children.head
        val BindNode(v874, v875) = v873
        assert(v874.id == 187)
        RangeEndValue(matchIntLit(v875))(v872)
      case 455 =>
        RangeEndMax()(v872)
    }
    v876
  }

  def matchRanges(node: Node): Ranges = {
    val BindNode(v877, v878) = node
    val v894 = v877.id match {
      case 444 =>
        val v879 = v878.asInstanceOf[SequenceNode].children.head
        val BindNode(v880, v881) = v879
        assert(v880.id == 445)
        val v882 = v878.asInstanceOf[SequenceNode].children(1)
        val v883 = unrollRepeat0(v882).map { elem =>
          val BindNode(v884, v885) = elem
          assert(v884.id == 461)
          val BindNode(v886, v887) = v885
          val v893 = v886.id match {
            case 462 =>
              val BindNode(v888, v889) = v887
              assert(v888.id == 463)
              val v890 = v889.asInstanceOf[SequenceNode].children(3)
              val BindNode(v891, v892) = v890
              assert(v891.id == 445)
              matchRange(v892)
          }
          v893
        }
        Ranges(List(matchRange(v881)) ++ v883)(v878)
    }
    v894
  }

  def matchReserved(node: Node): Reserved = {
    val BindNode(v895, v896) = node
    val v913 = v895.id match {
      case 435 =>
        val v897 = v896.asInstanceOf[SequenceNode].children(2)
        val BindNode(v898, v899) = v897
        assert(v898.id == 440)
        val BindNode(v900, v901) = v899
        val v912 = v900.id match {
          case 441 =>
            val BindNode(v902, v903) = v901
            assert(v902.id == 442)
            val v904 = v903.asInstanceOf[SequenceNode].children.head
            val BindNode(v905, v906) = v904
            assert(v905.id == 443)
            matchRanges(v906)
          case 464 =>
            val BindNode(v907, v908) = v901
            assert(v907.id == 465)
            val v909 = v908.asInstanceOf[SequenceNode].children.head
            val BindNode(v910, v911) = v909
            assert(v910.id == 466)
            matchFieldNames(v911)
        }
        Reserved(v912)(v896)
    }
    v913
  }

  def matchRpc(node: Node): Rpc = {
    val BindNode(v914, v915) = node
    val v958 = v914.id match {
      case 489 =>
        val v916 = v915.asInstanceOf[SequenceNode].children(2)
        val BindNode(v917, v918) = v916
        assert(v917.id == 493)
        val v919 = v915.asInstanceOf[SequenceNode].children(5)
        val BindNode(v920, v921) = v919
        assert(v920.id == 494)
        val BindNode(v922, v923) = v921
        val v933 = v922.id match {
          case 81 =>
            None
          case 495 =>
            val BindNode(v924, v925) = v923
            val v932 = v924.id match {
              case 496 =>
                val BindNode(v926, v927) = v925
                assert(v926.id == 497)
                val v928 = v927.asInstanceOf[SequenceNode].children(1)
                val BindNode(v929, v930) = v928
                assert(v929.id == 498)
                val JoinNode(_, v931, _) = v930
                "stream"
            }
            Some(v932)
        }
        val v934 = v915.asInstanceOf[SequenceNode].children(7)
        val BindNode(v935, v936) = v934
        assert(v935.id == 338)
        val v937 = v915.asInstanceOf[SequenceNode].children(14)
        val BindNode(v938, v939) = v937
        assert(v938.id == 494)
        val BindNode(v940, v941) = v939
        val v951 = v940.id match {
          case 81 =>
            None
          case 495 =>
            val BindNode(v942, v943) = v941
            val v950 = v942.id match {
              case 496 =>
                val BindNode(v944, v945) = v943
                assert(v944.id == 497)
                val v946 = v945.asInstanceOf[SequenceNode].children(1)
                val BindNode(v947, v948) = v946
                assert(v947.id == 498)
                val JoinNode(_, v949, _) = v948
                "stream"
            }
            Some(v950)
        }
        val v952 = v915.asInstanceOf[SequenceNode].children(16)
        val BindNode(v953, v954) = v952
        assert(v953.id == 338)
        val v955 = v915.asInstanceOf[SequenceNode].children(20)
        val BindNode(v956, v957) = v955
        assert(v956.id == 504)
        Rpc(matchRpcName(v918), v933.isDefined, matchMessageType(v936), v951.isDefined, matchMessageType(v954), matchRpcEnding(v957))(v915)
    }
    v958
  }

  def matchRpcEnding(node: Node): List[Option[OptionDef]] = {
    val BindNode(v959, v960) = node
    val v983 = v959.id match {
      case 505 =>
        val v961 = v960.asInstanceOf[SequenceNode].children(1)
        val v962 = unrollRepeat0(v961).map { elem =>
          val BindNode(v963, v964) = elem
          assert(v963.id == 508)
          val BindNode(v965, v966) = v964
          val v982 = v965.id match {
            case 509 =>
              val BindNode(v967, v968) = v966
              assert(v967.id == 510)
              val v969 = v968.asInstanceOf[SequenceNode].children(1)
              val BindNode(v970, v971) = v969
              assert(v970.id == 511)
              val BindNode(v972, v973) = v971
              val v981 = v972.id match {
                case 148 =>
                  val BindNode(v974, v975) = v973
                  assert(v974.id == 149)
                  val v976 = v975.asInstanceOf[SequenceNode].children.head
                  val BindNode(v977, v978) = v976
                  assert(v977.id == 150)
                  Some(matchOption(v978))
                case 398 =>
                  val BindNode(v979, v980) = v973
                  assert(v979.id == 399)
                  None
              }
              v981
          }
          v982
        }
        v962
      case 401 =>
        List()
    }
    v983
  }

  def matchRpcName(node: Node): Ident = {
    val BindNode(v984, v985) = node
    val v989 = v984.id match {
      case 159 =>
        val v986 = v985.asInstanceOf[SequenceNode].children.head
        val BindNode(v987, v988) = v986
        assert(v987.id == 120)
        matchIdent(v988)
    }
    v989
  }

  def matchService(node: Node): Service = {
    val BindNode(v990, v991) = node
    val v1025 = v990.id match {
      case 475 =>
        val v992 = v991.asInstanceOf[SequenceNode].children(2)
        val BindNode(v993, v994) = v992
        assert(v993.id == 479)
        val v995 = v991.asInstanceOf[SequenceNode].children(5)
        val v996 = unrollRepeat0(v995).map { elem =>
          val BindNode(v997, v998) = elem
          assert(v997.id == 482)
          val BindNode(v999, v1000) = v998
          val v1024 = v999.id match {
            case 483 =>
              val BindNode(v1001, v1002) = v1000
              assert(v1001.id == 484)
              val v1003 = v1002.asInstanceOf[SequenceNode].children(1)
              val BindNode(v1004, v1005) = v1003
              assert(v1004.id == 485)
              val BindNode(v1006, v1007) = v1005
              val v1023 = v1006.id match {
                case 148 =>
                  val BindNode(v1008, v1009) = v1007
                  assert(v1008.id == 149)
                  val v1010 = v1009.asInstanceOf[SequenceNode].children.head
                  val BindNode(v1011, v1012) = v1010
                  assert(v1011.id == 150)
                  matchOption(v1012)
                case 486 =>
                  val BindNode(v1013, v1014) = v1007
                  assert(v1013.id == 487)
                  val v1015 = v1014.asInstanceOf[SequenceNode].children.head
                  val BindNode(v1016, v1017) = v1015
                  assert(v1016.id == 488)
                  matchRpc(v1017)
                case 398 =>
                  val BindNode(v1018, v1019) = v1007
                  assert(v1018.id == 399)
                  val v1020 = v1019.asInstanceOf[SequenceNode].children.head
                  val BindNode(v1021, v1022) = v1020
                  assert(v1021.id == 400)
                  matchEmptyStatement(v1022)
              }
              v1023
          }
          v1024
        }
        Service(matchServiceName(v994), v996)(v991)
    }
    v1025
  }

  def matchServiceName(node: Node): Ident = {
    val BindNode(v1026, v1027) = node
    val v1031 = v1026.id match {
      case 159 =>
        val v1028 = v1027.asInstanceOf[SequenceNode].children.head
        val BindNode(v1029, v1030) = v1028
        assert(v1029.id == 120)
        matchIdent(v1030)
    }
    v1031
  }

  def matchSign(node: Node): Sign.Value = {
    val BindNode(v1032, v1033) = node
    val v1034 = v1032.id match {
      case 183 =>
        Sign.PLUS
      case 185 =>
        Sign.MINUS
    }
    v1034
  }

  def matchStrLit(node: Node): StrLit = {
    val BindNode(v1035, v1036) = node
    val v1045 = v1035.id match {
      case 83 =>
        val v1037 = v1036.asInstanceOf[SequenceNode].children(1)
        val v1038 = unrollRepeat0(v1037).map { elem =>
          val BindNode(v1039, v1040) = elem
          assert(v1039.id == 86)
          matchCharValue(v1040)
        }
        SingleQuoteStrLit(v1038)(v1036)
      case 109 =>
        val v1041 = v1036.asInstanceOf[SequenceNode].children(1)
        val v1042 = unrollRepeat0(v1041).map { elem =>
          val BindNode(v1043, v1044) = elem
          assert(v1043.id == 86)
          matchCharValue(v1044)
        }
        DoubleQuoteStrLit(v1042)(v1036)
    }
    v1045
  }

  def matchTopLevelDef(node: Node): TopLevelDef = {
    val BindNode(v1046, v1047) = node
    val v1057 = v1046.id match {
      case 240 =>
        val v1048 = v1047.asInstanceOf[SequenceNode].children.head
        val BindNode(v1049, v1050) = v1048
        assert(v1049.id == 241)
        matchMessage(v1050)
      case 365 =>
        val v1051 = v1047.asInstanceOf[SequenceNode].children.head
        val BindNode(v1052, v1053) = v1051
        assert(v1052.id == 366)
        matchEnum(v1053)
      case 473 =>
        val v1054 = v1047.asInstanceOf[SequenceNode].children.head
        val BindNode(v1055, v1056) = v1054
        assert(v1055.id == 474)
        matchService(v1056)
    }
    v1057
  }

  def matchType(node: Node): Type = {
    val BindNode(v1058, v1059) = node
    val v1068 = v1058.id match {
      case 268 =>
        val v1060 = v1059.asInstanceOf[SequenceNode].children.head
        val BindNode(v1061, v1062) = v1060
        assert(v1061.id == 269)
        BuiltinType(matchBuiltinType(v1062))(v1059)
      case 336 =>
        val v1063 = v1059.asInstanceOf[SequenceNode].children.head
        val BindNode(v1064, v1065) = v1063
        assert(v1064.id == 337)
        val BindNode(v1066, v1067) = v1065
        assert(v1066.id == 338)
        MessageOrEnumType(matchMessageType(v1067))(v1059)
    }
    v1068
  }

  def matchZeroLit(node: Node): DecimalLit = {
    val BindNode(v1069, v1070) = node
    val v1071 = v1069.id match {
      case 194 =>
        DecimalLit("0")(v1070)
    }
    v1071
  }

  def matchStart(node: Node): Proto3 = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchProto3(body)
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[Proto3, ParsingErrors.ParsingError] =
    ParseTreeUtil.parseAst(naiveParser, text, matchStart)
}
