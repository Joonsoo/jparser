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
import com.giyeok.jparser.nparser.Parser
import scala.collection.immutable.ListSet

object AutoDbAst {
  val ngrammar =   new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("AutoDbSchema"), Set(3)),
  4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("WS"), Set(5)),
  6 -> NGrammar.NRepeat(6, Symbols.Repeat(Symbols.Chars(Set('\r',' ') ++ ('\u0008' to '\n').toSet), 0), 7, 8),
  9 -> NGrammar.NTerminal(9, Symbols.Chars(Set('\r',' ') ++ ('\u0008' to '\n').toSet)),
  10 -> NGrammar.NJoin(10, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'),Symbols.ExactChar('a'),Symbols.ExactChar('t'),Symbols.ExactChar('a'),Symbols.ExactChar('b'),Symbols.ExactChar('a'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))), Symbols.Nonterminal("Tk")), 11, 19),
  11 -> NGrammar.NProxy(11, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'),Symbols.ExactChar('a'),Symbols.ExactChar('t'),Symbols.ExactChar('a'),Symbols.ExactChar('b'),Symbols.ExactChar('a'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))), 12),
  13 -> NGrammar.NTerminal(13, Symbols.ExactChar('d')),
  14 -> NGrammar.NTerminal(14, Symbols.ExactChar('a')),
  15 -> NGrammar.NTerminal(15, Symbols.ExactChar('t')),
  16 -> NGrammar.NTerminal(16, Symbols.ExactChar('b')),
  17 -> NGrammar.NTerminal(17, Symbols.ExactChar('s')),
  18 -> NGrammar.NTerminal(18, Symbols.ExactChar('e')),
  19 -> NGrammar.NNonterminal(19, Symbols.Nonterminal("Tk"), Set(20)),
  21 -> NGrammar.NLongest(21, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1))))))), 22),
  22 -> NGrammar.NOneOf(22, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1)))))), Set(23)),
  23 -> NGrammar.NProxy(23, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1)))), 24),
  25 -> NGrammar.NRepeat(25, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1), 26, 27),
  26 -> NGrammar.NTerminal(26, Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
  28 -> NGrammar.NNonterminal(28, Symbols.Nonterminal("Name"), Set(29)),
  30 -> NGrammar.NNonterminal(30, Symbols.Nonterminal("Id"), Set(31)),
  32 -> NGrammar.NLongest(32, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))), 33),
  33 -> NGrammar.NOneOf(33, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))))), Set(34)),
  34 -> NGrammar.NProxy(34, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))), 35),
  36 -> NGrammar.NRepeat(36, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), 7, 37),
  38 -> NGrammar.NTerminal(38, Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
  39 -> NGrammar.NTerminal(39, Symbols.ExactChar('{')),
  40 -> NGrammar.NOneOf(40, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Defs")))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(41,110)),
  41 -> NGrammar.NOneOf(41, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Defs")))))), Set(42)),
  42 -> NGrammar.NProxy(42, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Defs")))), 43),
  44 -> NGrammar.NNonterminal(44, Symbols.Nonterminal("Defs"), Set(45)),
  46 -> NGrammar.NNonterminal(46, Symbols.Nonterminal("Def"), Set(47,159)),
  48 -> NGrammar.NNonterminal(48, Symbols.Nonterminal("EntityDef"), Set(49)),
  50 -> NGrammar.NJoin(50, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('e'),Symbols.ExactChar('n'),Symbols.ExactChar('t'),Symbols.ExactChar('i'),Symbols.ExactChar('t'),Symbols.ExactChar('y')))), Symbols.Nonterminal("Tk")), 51, 19),
  51 -> NGrammar.NProxy(51, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('e'),Symbols.ExactChar('n'),Symbols.ExactChar('t'),Symbols.ExactChar('i'),Symbols.ExactChar('t'),Symbols.ExactChar('y')))), 52),
  53 -> NGrammar.NTerminal(53, Symbols.ExactChar('n')),
  54 -> NGrammar.NTerminal(54, Symbols.ExactChar('i')),
  55 -> NGrammar.NTerminal(55, Symbols.ExactChar('y')),
  56 -> NGrammar.NTerminal(56, Symbols.ExactChar('(')),
  57 -> NGrammar.NOneOf(57, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParams")))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(58,110)),
  58 -> NGrammar.NOneOf(58, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParams")))))), Set(59)),
  59 -> NGrammar.NProxy(59, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParams")))), 60),
  61 -> NGrammar.NNonterminal(61, Symbols.Nonterminal("EntityParams"), Set(62)),
  63 -> NGrammar.NNonterminal(63, Symbols.Nonterminal("EntityParam"), Set(64)),
  65 -> NGrammar.NTerminal(65, Symbols.ExactChar(':')),
  66 -> NGrammar.NNonterminal(66, Symbols.Nonterminal("Type"), Set(67)),
  68 -> NGrammar.NNonterminal(68, Symbols.Nonterminal("NonNullType"), Set(69,76,81,87,99)),
  70 -> NGrammar.NJoin(70, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('S'),Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('i'),Symbols.ExactChar('n'),Symbols.ExactChar('g')))), Symbols.Nonterminal("Tk")), 71, 19),
  71 -> NGrammar.NProxy(71, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('S'),Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('i'),Symbols.ExactChar('n'),Symbols.ExactChar('g')))), 72),
  73 -> NGrammar.NTerminal(73, Symbols.ExactChar('S')),
  74 -> NGrammar.NTerminal(74, Symbols.ExactChar('r')),
  75 -> NGrammar.NTerminal(75, Symbols.ExactChar('g')),
  77 -> NGrammar.NJoin(77, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('I'),Symbols.ExactChar('n'),Symbols.ExactChar('t')))), Symbols.Nonterminal("Tk")), 78, 19),
  78 -> NGrammar.NProxy(78, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('I'),Symbols.ExactChar('n'),Symbols.ExactChar('t')))), 79),
  80 -> NGrammar.NTerminal(80, Symbols.ExactChar('I')),
  82 -> NGrammar.NJoin(82, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('L'),Symbols.ExactChar('o'),Symbols.ExactChar('n'),Symbols.ExactChar('g')))), Symbols.Nonterminal("Tk")), 83, 19),
  83 -> NGrammar.NProxy(83, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('L'),Symbols.ExactChar('o'),Symbols.ExactChar('n'),Symbols.ExactChar('g')))), 84),
  85 -> NGrammar.NTerminal(85, Symbols.ExactChar('L')),
  86 -> NGrammar.NTerminal(86, Symbols.ExactChar('o')),
  88 -> NGrammar.NJoin(88, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('B'),Symbols.ExactChar('y'),Symbols.ExactChar('t'),Symbols.ExactChar('e'),Symbols.ExactChar('s')))), Symbols.Nonterminal("Tk")), 89, 19),
  89 -> NGrammar.NProxy(89, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('B'),Symbols.ExactChar('y'),Symbols.ExactChar('t'),Symbols.ExactChar('e'),Symbols.ExactChar('s')))), 90),
  91 -> NGrammar.NTerminal(91, Symbols.ExactChar('B')),
  92 -> NGrammar.NNonterminal(92, Symbols.Nonterminal("BytesLength"), Set(93)),
  94 -> NGrammar.NTerminal(94, Symbols.Chars(('1' to '9').toSet)),
  95 -> NGrammar.NRepeat(95, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 7, 96),
  97 -> NGrammar.NTerminal(97, Symbols.Chars(('0' to '9').toSet)),
  98 -> NGrammar.NTerminal(98, Symbols.ExactChar(')')),
  100 -> NGrammar.NJoin(100, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('R'),Symbols.ExactChar('e'),Symbols.ExactChar('f')))), Symbols.Nonterminal("Tk")), 101, 19),
  101 -> NGrammar.NProxy(101, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('R'),Symbols.ExactChar('e'),Symbols.ExactChar('f')))), 102),
  103 -> NGrammar.NTerminal(103, Symbols.ExactChar('R')),
  104 -> NGrammar.NTerminal(104, Symbols.ExactChar('f')),
  105 -> NGrammar.NOneOf(105, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(106,110)),
  106 -> NGrammar.NOneOf(106, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))))), Set(107)),
  107 -> NGrammar.NProxy(107, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))), 108),
  109 -> NGrammar.NTerminal(109, Symbols.ExactChar('?')),
  110 -> NGrammar.NProxy(110, Symbols.Proxy(Symbols.Sequence(Seq())), 7),
  111 -> NGrammar.NOneOf(111, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('s')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name")))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(112,110)),
  112 -> NGrammar.NOneOf(112, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('s')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name")))))), Set(113)),
  113 -> NGrammar.NProxy(113, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('s')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name")))), 114),
  115 -> NGrammar.NJoin(115, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('s')))), Symbols.Nonterminal("Tk")), 116, 19),
  116 -> NGrammar.NProxy(116, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('s')))), 117),
  118 -> NGrammar.NRepeat(118, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParam")))))), 0), 7, 119),
  120 -> NGrammar.NOneOf(120, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParam")))))), Set(121)),
  121 -> NGrammar.NProxy(121, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParam")))), 122),
  123 -> NGrammar.NTerminal(123, Symbols.ExactChar(',')),
  124 -> NGrammar.NOneOf(124, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(125,110)),
  125 -> NGrammar.NOneOf(125, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))), Set(126)),
  126 -> NGrammar.NProxy(126, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))), 127),
  128 -> NGrammar.NOneOf(128, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityKey")))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(129,110)),
  129 -> NGrammar.NOneOf(129, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityKey")))))), Set(130)),
  130 -> NGrammar.NProxy(130, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityKey")))), 131),
  132 -> NGrammar.NNonterminal(132, Symbols.Nonterminal("EntityKey"), Set(133)),
  134 -> NGrammar.NNonterminal(134, Symbols.Nonterminal("PrimaryKey"), Set(135)),
  136 -> NGrammar.NJoin(136, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('k'),Symbols.ExactChar('e'),Symbols.ExactChar('y')))), Symbols.Nonterminal("Tk")), 137, 19),
  137 -> NGrammar.NProxy(137, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('k'),Symbols.ExactChar('e'),Symbols.ExactChar('y')))), 138),
  139 -> NGrammar.NTerminal(139, Symbols.ExactChar('k')),
  140 -> NGrammar.NRepeat(140, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name")))))), 0), 7, 141),
  142 -> NGrammar.NOneOf(142, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name")))))), Set(143)),
  143 -> NGrammar.NProxy(143, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name")))), 144),
  145 -> NGrammar.NRepeat(145, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SecondaryKey")))))), 0), 7, 146),
  147 -> NGrammar.NOneOf(147, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SecondaryKey")))))), Set(148)),
  148 -> NGrammar.NProxy(148, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SecondaryKey")))), 149),
  150 -> NGrammar.NNonterminal(150, Symbols.Nonterminal("SecondaryKey"), Set(151)),
  152 -> NGrammar.NJoin(152, Symbols.Join(Symbols.Nonterminal("SecondaryKeyType"), Symbols.Nonterminal("Tk")), 153, 19),
  153 -> NGrammar.NNonterminal(153, Symbols.Nonterminal("SecondaryKeyType"), Set(154)),
  155 -> NGrammar.NProxy(155, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'),Symbols.ExactChar('n'),Symbols.ExactChar('i'),Symbols.ExactChar('q'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))), 156),
  157 -> NGrammar.NTerminal(157, Symbols.ExactChar('u')),
  158 -> NGrammar.NTerminal(158, Symbols.ExactChar('q')),
  160 -> NGrammar.NNonterminal(160, Symbols.Nonterminal("FuncDef"), Set(161)),
  162 -> NGrammar.NJoin(162, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'),Symbols.ExactChar('e'),Symbols.ExactChar('f')))), Symbols.Nonterminal("Tk")), 163, 19),
  163 -> NGrammar.NProxy(163, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'),Symbols.ExactChar('e'),Symbols.ExactChar('f')))), 164),
  165 -> NGrammar.NRepeat(165, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))))), 0), 7, 166),
  167 -> NGrammar.NOneOf(167, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))))), Set(168)),
  168 -> NGrammar.NProxy(168, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))), 169),
  170 -> NGrammar.NTerminal(170, Symbols.ExactChar('}'))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'),Symbols.ExactChar('a'),Symbols.ExactChar('t'),Symbols.ExactChar('a'),Symbols.ExactChar('b'),Symbols.ExactChar('a'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name"),Symbols.Nonterminal("WS"),Symbols.ExactChar('{'),Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Defs")))))),Symbols.Proxy(Symbols.Sequence(Seq())))),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'),Symbols.Nonterminal("WS"))), Seq(4,10,4,28,4,39,40,4,170,4)),
  5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('\r',' ') ++ ('\u0008' to '\n').toSet), 0))), Seq(6)),
  7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq()), Seq()),
  8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('\r',' ') ++ ('\u0008' to '\n').toSet), 0),Symbols.Chars(Set('\r',' ') ++ ('\u0008' to '\n').toSet))), Seq(6,9)),
  12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.ExactChar('d'),Symbols.ExactChar('a'),Symbols.ExactChar('t'),Symbols.ExactChar('a'),Symbols.ExactChar('b'),Symbols.ExactChar('a'),Symbols.ExactChar('s'),Symbols.ExactChar('e'))), Seq(13,14,15,14,16,14,17,18)),
  20 -> NGrammar.NSequence(20, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1))))))))), Seq(21)),
  24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1))), Seq(25)),
  27 -> NGrammar.NSequence(27, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1),Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(25,26)),
  29 -> NGrammar.NSequence(29, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(30)),
  31 -> NGrammar.NSequence(31, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))))), Seq(32)),
  35 -> NGrammar.NSequence(35, Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))), Seq(26,36)),
  37 -> NGrammar.NSequence(37, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0),Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(36,38)),
  43 -> NGrammar.NSequence(43, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Defs"))), Seq(4,44)),
  45 -> NGrammar.NSequence(45, Symbols.Sequence(Seq(Symbols.Nonterminal("Def"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))))), 0))), Seq(46,165)),
  47 -> NGrammar.NSequence(47, Symbols.Sequence(Seq(Symbols.Nonterminal("EntityDef"))), Seq(48)),
  49 -> NGrammar.NSequence(49, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('e'),Symbols.ExactChar('n'),Symbols.ExactChar('t'),Symbols.ExactChar('i'),Symbols.ExactChar('t'),Symbols.ExactChar('y')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name"),Symbols.Nonterminal("WS"),Symbols.ExactChar('('),Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParams")))))),Symbols.Proxy(Symbols.Sequence(Seq())))),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'),Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityKey")))))),Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(50,4,28,4,56,57,4,98,128)),
  52 -> NGrammar.NSequence(52, Symbols.Sequence(Seq(Symbols.ExactChar('e'),Symbols.ExactChar('n'),Symbols.ExactChar('t'),Symbols.ExactChar('i'),Symbols.ExactChar('t'),Symbols.ExactChar('y'))), Seq(18,53,15,54,15,55)),
  60 -> NGrammar.NSequence(60, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParams"))), Seq(4,61)),
  62 -> NGrammar.NSequence(62, Symbols.Sequence(Seq(Symbols.Nonterminal("EntityParam"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParam")))))), 0),Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(',')))))),Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(63,118,124)),
  64 -> NGrammar.NSequence(64, Symbols.Sequence(Seq(Symbols.Nonterminal("Name"),Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Type"),Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('s')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name")))))),Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(28,4,65,4,66,111)),
  67 -> NGrammar.NSequence(67, Symbols.Sequence(Seq(Symbols.Nonterminal("NonNullType"),Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?')))))),Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(68,105)),
  69 -> NGrammar.NSequence(69, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('S'),Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('i'),Symbols.ExactChar('n'),Symbols.ExactChar('g')))), Symbols.Nonterminal("Tk")))), Seq(70)),
  72 -> NGrammar.NSequence(72, Symbols.Sequence(Seq(Symbols.ExactChar('S'),Symbols.ExactChar('t'),Symbols.ExactChar('r'),Symbols.ExactChar('i'),Symbols.ExactChar('n'),Symbols.ExactChar('g'))), Seq(73,15,74,54,53,75)),
  76 -> NGrammar.NSequence(76, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('I'),Symbols.ExactChar('n'),Symbols.ExactChar('t')))), Symbols.Nonterminal("Tk")))), Seq(77)),
  79 -> NGrammar.NSequence(79, Symbols.Sequence(Seq(Symbols.ExactChar('I'),Symbols.ExactChar('n'),Symbols.ExactChar('t'))), Seq(80,53,15)),
  81 -> NGrammar.NSequence(81, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('L'),Symbols.ExactChar('o'),Symbols.ExactChar('n'),Symbols.ExactChar('g')))), Symbols.Nonterminal("Tk")))), Seq(82)),
  84 -> NGrammar.NSequence(84, Symbols.Sequence(Seq(Symbols.ExactChar('L'),Symbols.ExactChar('o'),Symbols.ExactChar('n'),Symbols.ExactChar('g'))), Seq(85,86,53,75)),
  87 -> NGrammar.NSequence(87, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('B'),Symbols.ExactChar('y'),Symbols.ExactChar('t'),Symbols.ExactChar('e'),Symbols.ExactChar('s')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("BytesLength"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(88,4,56,4,92,4,98)),
  90 -> NGrammar.NSequence(90, Symbols.Sequence(Seq(Symbols.ExactChar('B'),Symbols.ExactChar('y'),Symbols.ExactChar('t'),Symbols.ExactChar('e'),Symbols.ExactChar('s'))), Seq(91,55,15,18,17)),
  93 -> NGrammar.NSequence(93, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(94,95)),
  96 -> NGrammar.NSequence(96, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0),Symbols.Chars(('0' to '9').toSet))), Seq(95,97)),
  99 -> NGrammar.NSequence(99, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('R'),Symbols.ExactChar('e'),Symbols.ExactChar('f')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(100,4,56,4,28,4,98)),
  102 -> NGrammar.NSequence(102, Symbols.Sequence(Seq(Symbols.ExactChar('R'),Symbols.ExactChar('e'),Symbols.ExactChar('f'))), Seq(103,18,104)),
  108 -> NGrammar.NSequence(108, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('?'))), Seq(4,109)),
  114 -> NGrammar.NSequence(114, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('s')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name"))), Seq(4,115,4,28)),
  117 -> NGrammar.NSequence(117, Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('s'))), Seq(14,17)),
  119 -> NGrammar.NSequence(119, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParam")))))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParam")))))))), Seq(118,120)),
  122 -> NGrammar.NSequence(122, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityParam"))), Seq(4,123,4,63)),
  127 -> NGrammar.NSequence(127, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','))), Seq(4,123)),
  131 -> NGrammar.NSequence(131, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("EntityKey"))), Seq(4,132)),
  133 -> NGrammar.NSequence(133, Symbols.Sequence(Seq(Symbols.Nonterminal("PrimaryKey"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SecondaryKey")))))), 0))), Seq(134,145)),
  135 -> NGrammar.NSequence(135, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('k'),Symbols.ExactChar('e'),Symbols.ExactChar('y')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name")))))), 0),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(136,4,56,4,28,140,4,98)),
  138 -> NGrammar.NSequence(138, Symbols.Sequence(Seq(Symbols.ExactChar('k'),Symbols.ExactChar('e'),Symbols.ExactChar('y'))), Seq(139,18,55)),
  141 -> NGrammar.NSequence(141, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name")))))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name")))))))), Seq(140,142)),
  144 -> NGrammar.NSequence(144, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name"))), Seq(4,123,4,28)),
  146 -> NGrammar.NSequence(146, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SecondaryKey")))))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SecondaryKey")))))))), Seq(145,147)),
  149 -> NGrammar.NSequence(149, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("SecondaryKey"))), Seq(4,123,4,150)),
  151 -> NGrammar.NSequence(151, Symbols.Sequence(Seq(Symbols.Join(Symbols.Nonterminal("SecondaryKeyType"), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name")))))), 0),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(152,4,56,4,28,140,4,98)),
  154 -> NGrammar.NSequence(154, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('u'),Symbols.ExactChar('n'),Symbols.ExactChar('i'),Symbols.ExactChar('q'),Symbols.ExactChar('u'),Symbols.ExactChar('e')))))), Seq(155)),
  156 -> NGrammar.NSequence(156, Symbols.Sequence(Seq(Symbols.ExactChar('u'),Symbols.ExactChar('n'),Symbols.ExactChar('i'),Symbols.ExactChar('q'),Symbols.ExactChar('u'),Symbols.ExactChar('e'))), Seq(157,53,54,158,157,18)),
  159 -> NGrammar.NSequence(159, Symbols.Sequence(Seq(Symbols.Nonterminal("FuncDef"))), Seq(160)),
  161 -> NGrammar.NSequence(161, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('d'),Symbols.ExactChar('e'),Symbols.ExactChar('f')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Name"))), Seq(162,4,28)),
  164 -> NGrammar.NSequence(164, Symbols.Sequence(Seq(Symbols.ExactChar('d'),Symbols.ExactChar('e'),Symbols.ExactChar('f'))), Seq(13,18,104)),
  166 -> NGrammar.NSequence(166, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def")))))))), Seq(165,167)),
  169 -> NGrammar.NSequence(169, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Def"))), Seq(4,46))),
    1)

  sealed trait WithParseNode { val parseNode: Node }
  case class AutoDbSchema(name: Name, defs: List[Def])(override val parseNode: Node) extends WithParseNode
  case class BytesType(length: String)(override val parseNode: Node) extends NonNullType with WithParseNode
  sealed trait Def extends WithParseNode
  case class EntityDef(name: Name, params: List[EntityParam], key: Option[EntityKey])(override val parseNode: Node) extends Def with WithParseNode
  case class EntityKey(primary: PrimaryKey, secondary: List[SecondaryKey])(override val parseNode: Node) extends WithParseNode
  case class EntityParam(name: Name, typ: Type, alias: Option[Name])(override val parseNode: Node) extends WithParseNode
  case class FuncDef(name: Name)(override val parseNode: Node) extends Def with WithParseNode
  case class IntType()(override val parseNode: Node) extends NonNullType with WithParseNode
  case class LongType()(override val parseNode: Node) extends NonNullType with WithParseNode
  case class Name(name: String)(override val parseNode: Node) extends WithParseNode
  sealed trait NonNullType extends Type with WithParseNode
  case class OptionalType(valueType: NonNullType)(override val parseNode: Node) extends Type with WithParseNode
  case class PrimaryKey(name: List[Name])(override val parseNode: Node) extends WithParseNode
  case class RefType(entity: Name)(override val parseNode: Node) extends NonNullType with WithParseNode
  case class SecondaryKey(name: List[Name])(override val parseNode: Node) extends WithParseNode
  case class StringType()(override val parseNode: Node) extends NonNullType with WithParseNode
  sealed trait Type extends WithParseNode
  object SecondaryKeyType extends Enumeration { val UNIQUE = Value }

  def matchAutoDbSchema(node: Node): AutoDbSchema = {
    val BindNode(v1, v2) = node
    val v21 = v1.id match {
      case 3 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(3)
        val BindNode(v4, v5) = v3
        assert(v4.id == 28)
        val v7 = v2.asInstanceOf[SequenceNode].children(6)
        val BindNode(v8, v9) = v7
        assert(v8.id == 40)
        val BindNode(v10, v11) = v9
        val v20 = v10.id match {
        case 110 =>
        None
        case 41 =>
          val BindNode(v12, v13) = v11
          val v19 = v12.id match {
          case 42 =>
            val BindNode(v14, v15) = v13
            assert(v14.id == 43)
            val v16 = v15.asInstanceOf[SequenceNode].children(1)
            val BindNode(v17, v18) = v16
            assert(v17.id == 44)
            matchDefs(v18)
        }
          Some(v19)
      }
        val v6 = v20
        AutoDbSchema(matchName(v5), if (v6.isDefined) v6.get else List())(v2)
    }
    v21
  }

  def matchBytesLength(node: Node): String = {
    val BindNode(v22, v23) = node
    val v31 = v22.id match {
      case 93 =>
        val v24 = v23.asInstanceOf[SequenceNode].children.head
        val BindNode(v25, v26) = v24
        assert(v25.id == 94)
        val v27 = v23.asInstanceOf[SequenceNode].children(1)
        val v28 = unrollRepeat0(v27).map { elem =>
        val BindNode(v29, v30) = elem
        assert(v29.id == 97)
        v30.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        v26.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v28.map(x => x.toString).mkString("")
    }
    v31
  }

  def matchDef(node: Node): Def = {
    val BindNode(v32, v33) = node
    val v40 = v32.id match {
      case 47 =>
        val v34 = v33.asInstanceOf[SequenceNode].children.head
        val BindNode(v35, v36) = v34
        assert(v35.id == 48)
        matchEntityDef(v36)
      case 159 =>
        val v37 = v33.asInstanceOf[SequenceNode].children.head
        val BindNode(v38, v39) = v37
        assert(v38.id == 160)
        matchFuncDef(v39)
    }
    v40
  }

  def matchDefs(node: Node): List[Def] = {
    val BindNode(v41, v42) = node
    val v58 = v41.id match {
      case 45 =>
        val v43 = v42.asInstanceOf[SequenceNode].children.head
        val BindNode(v44, v45) = v43
        assert(v44.id == 46)
        val v46 = v42.asInstanceOf[SequenceNode].children(1)
        val v47 = unrollRepeat0(v46).map { elem =>
        val BindNode(v48, v49) = elem
        assert(v48.id == 167)
        val BindNode(v50, v51) = v49
        val v57 = v50.id match {
        case 168 =>
          val BindNode(v52, v53) = v51
          assert(v52.id == 169)
          val v54 = v53.asInstanceOf[SequenceNode].children(1)
          val BindNode(v55, v56) = v54
          assert(v55.id == 46)
          matchDef(v56)
      }
        v57
        }
        List(matchDef(v45)) ++ v47
    }
    v58
  }

  def matchEntityDef(node: Node): EntityDef = {
    val BindNode(v59, v60) = node
    val v93 = v59.id match {
      case 49 =>
        val v61 = v60.asInstanceOf[SequenceNode].children(2)
        val BindNode(v62, v63) = v61
        assert(v62.id == 28)
        val v65 = v60.asInstanceOf[SequenceNode].children(5)
        val BindNode(v66, v67) = v65
        assert(v66.id == 57)
        val BindNode(v68, v69) = v67
        val v78 = v68.id match {
        case 110 =>
        None
        case 58 =>
          val BindNode(v70, v71) = v69
          val v77 = v70.id match {
          case 59 =>
            val BindNode(v72, v73) = v71
            assert(v72.id == 60)
            val v74 = v73.asInstanceOf[SequenceNode].children(1)
            val BindNode(v75, v76) = v74
            assert(v75.id == 61)
            matchEntityParams(v76)
        }
          Some(v77)
      }
        val v64 = v78
        val v79 = v60.asInstanceOf[SequenceNode].children(8)
        val BindNode(v80, v81) = v79
        assert(v80.id == 128)
        val BindNode(v82, v83) = v81
        val v92 = v82.id match {
        case 110 =>
        None
        case 129 =>
          val BindNode(v84, v85) = v83
          val v91 = v84.id match {
          case 130 =>
            val BindNode(v86, v87) = v85
            assert(v86.id == 131)
            val v88 = v87.asInstanceOf[SequenceNode].children(1)
            val BindNode(v89, v90) = v88
            assert(v89.id == 132)
            matchEntityKey(v90)
        }
          Some(v91)
      }
        EntityDef(matchName(v63), if (v64.isDefined) v64.get else List(), v92)(v60)
    }
    v93
  }

  def matchEntityKey(node: Node): EntityKey = {
    val BindNode(v94, v95) = node
    val v111 = v94.id match {
      case 133 =>
        val v96 = v95.asInstanceOf[SequenceNode].children.head
        val BindNode(v97, v98) = v96
        assert(v97.id == 134)
        val v99 = v95.asInstanceOf[SequenceNode].children(1)
        val v100 = unrollRepeat0(v99).map { elem =>
        val BindNode(v101, v102) = elem
        assert(v101.id == 147)
        val BindNode(v103, v104) = v102
        val v110 = v103.id match {
        case 148 =>
          val BindNode(v105, v106) = v104
          assert(v105.id == 149)
          val v107 = v106.asInstanceOf[SequenceNode].children(3)
          val BindNode(v108, v109) = v107
          assert(v108.id == 150)
          matchSecondaryKey(v109)
      }
        v110
        }
        EntityKey(matchPrimaryKey(v98), v100)(v95)
    }
    v111
  }

  def matchEntityParam(node: Node): EntityParam = {
    val BindNode(v112, v113) = node
    val v134 = v112.id match {
      case 64 =>
        val v114 = v113.asInstanceOf[SequenceNode].children.head
        val BindNode(v115, v116) = v114
        assert(v115.id == 28)
        val v117 = v113.asInstanceOf[SequenceNode].children(4)
        val BindNode(v118, v119) = v117
        assert(v118.id == 66)
        val v120 = v113.asInstanceOf[SequenceNode].children(5)
        val BindNode(v121, v122) = v120
        assert(v121.id == 111)
        val BindNode(v123, v124) = v122
        val v133 = v123.id match {
        case 110 =>
        None
        case 112 =>
          val BindNode(v125, v126) = v124
          val v132 = v125.id match {
          case 113 =>
            val BindNode(v127, v128) = v126
            assert(v127.id == 114)
            val v129 = v128.asInstanceOf[SequenceNode].children(3)
            val BindNode(v130, v131) = v129
            assert(v130.id == 28)
            matchName(v131)
        }
          Some(v132)
      }
        EntityParam(matchName(v116), matchType(v119), v133)(v113)
    }
    v134
  }

  def matchEntityParams(node: Node): List[EntityParam] = {
    val BindNode(v135, v136) = node
    val v152 = v135.id match {
      case 62 =>
        val v137 = v136.asInstanceOf[SequenceNode].children.head
        val BindNode(v138, v139) = v137
        assert(v138.id == 63)
        val v140 = v136.asInstanceOf[SequenceNode].children(1)
        val v141 = unrollRepeat0(v140).map { elem =>
        val BindNode(v142, v143) = elem
        assert(v142.id == 120)
        val BindNode(v144, v145) = v143
        val v151 = v144.id match {
        case 121 =>
          val BindNode(v146, v147) = v145
          assert(v146.id == 122)
          val v148 = v147.asInstanceOf[SequenceNode].children(3)
          val BindNode(v149, v150) = v148
          assert(v149.id == 63)
          matchEntityParam(v150)
      }
        v151
        }
        List(matchEntityParam(v139)) ++ v141
    }
    v152
  }

  def matchFuncDef(node: Node): FuncDef = {
    val BindNode(v153, v154) = node
    val v158 = v153.id match {
      case 161 =>
        val v155 = v154.asInstanceOf[SequenceNode].children(2)
        val BindNode(v156, v157) = v155
        assert(v156.id == 28)
        FuncDef(matchName(v157))(v154)
    }
    v158
  }

  def matchId(node: Node): String = {
    val BindNode(v159, v160) = node
    val v162 = v159.id match {
      case 31 =>
        val v161 = v160.asInstanceOf[SequenceNode].children.head
        v161.sourceText
    }
    v162
  }

  def matchName(node: Node): Name = {
    val BindNode(v163, v164) = node
    val v168 = v163.id match {
      case 29 =>
        val v165 = v164.asInstanceOf[SequenceNode].children.head
        val BindNode(v166, v167) = v165
        assert(v166.id == 30)
        Name(matchId(v167))(v164)
    }
    v168
  }

  def matchNonNullType(node: Node): NonNullType = {
    val BindNode(v169, v170) = node
    val v177 = v169.id match {
      case 99 =>
        val v171 = v170.asInstanceOf[SequenceNode].children(4)
        val BindNode(v172, v173) = v171
        assert(v172.id == 28)
        RefType(matchName(v173))(v170)
      case 69 =>
      StringType()(v170)
      case 76 =>
      IntType()(v170)
      case 81 =>
      LongType()(v170)
      case 87 =>
        val v174 = v170.asInstanceOf[SequenceNode].children(4)
        val BindNode(v175, v176) = v174
        assert(v175.id == 92)
        BytesType(matchBytesLength(v176))(v170)
    }
    v177
  }

  def matchPrimaryKey(node: Node): PrimaryKey = {
    val BindNode(v178, v179) = node
    val v195 = v178.id match {
      case 135 =>
        val v180 = v179.asInstanceOf[SequenceNode].children(4)
        val BindNode(v181, v182) = v180
        assert(v181.id == 28)
        val v183 = v179.asInstanceOf[SequenceNode].children(5)
        val v184 = unrollRepeat0(v183).map { elem =>
        val BindNode(v185, v186) = elem
        assert(v185.id == 142)
        val BindNode(v187, v188) = v186
        val v194 = v187.id match {
        case 143 =>
          val BindNode(v189, v190) = v188
          assert(v189.id == 144)
          val v191 = v190.asInstanceOf[SequenceNode].children(3)
          val BindNode(v192, v193) = v191
          assert(v192.id == 28)
          matchName(v193)
      }
        v194
        }
        PrimaryKey(List(matchName(v182)) ++ v184)(v179)
    }
    v195
  }

  def matchSecondaryKey(node: Node): SecondaryKey = {
    val BindNode(v196, v197) = node
    val v213 = v196.id match {
      case 151 =>
        val v198 = v197.asInstanceOf[SequenceNode].children(4)
        val BindNode(v199, v200) = v198
        assert(v199.id == 28)
        val v201 = v197.asInstanceOf[SequenceNode].children(5)
        val v202 = unrollRepeat0(v201).map { elem =>
        val BindNode(v203, v204) = elem
        assert(v203.id == 142)
        val BindNode(v205, v206) = v204
        val v212 = v205.id match {
        case 143 =>
          val BindNode(v207, v208) = v206
          assert(v207.id == 144)
          val v209 = v208.asInstanceOf[SequenceNode].children(3)
          val BindNode(v210, v211) = v209
          assert(v210.id == 28)
          matchName(v211)
      }
        v212
        }
        SecondaryKey(List(matchName(v200)) ++ v202)(v197)
    }
    v213
  }

  def matchType(node: Node): Type = {
    val BindNode(v214, v215) = node
    val v237 = v214.id match {
      case 67 =>
        val v216 = v215.asInstanceOf[SequenceNode].children(1)
        val BindNode(v217, v218) = v216
        assert(v217.id == 105)
        val BindNode(v219, v220) = v218
        val v229 = v219.id match {
        case 110 =>
        None
        case 106 =>
          val BindNode(v221, v222) = v220
          val v228 = v221.id match {
          case 107 =>
            val BindNode(v223, v224) = v222
            assert(v223.id == 108)
            val v225 = v224.asInstanceOf[SequenceNode].children(1)
            val BindNode(v226, v227) = v225
            assert(v226.id == 109)
            v227.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
          Some(v228)
      }
        val v236 = if (v229.isDefined) {
        val v230 = v215.asInstanceOf[SequenceNode].children.head
        val BindNode(v231, v232) = v230
        assert(v231.id == 68)
        OptionalType(matchNonNullType(v232))(v215)
        } else {
        val v233 = v215.asInstanceOf[SequenceNode].children.head
        val BindNode(v234, v235) = v233
        assert(v234.id == 68)
        matchNonNullType(v235)
        }
        v236
    }
    v237
  }

  def matchStart(node: Node): AutoDbSchema = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchAutoDbSchema(body)
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[AutoDbSchema, ParsingErrors.ParsingError] =
    ParseTreeUtil.parseAst(naiveParser, text, matchStart)

  def main(args: Array[String]): Unit = {
    println(parseAst("database DartFriends {\r\n  entity User(\r\n    userId: Long as UserId,\r\n    firebaseUid: String?,\r\n    userDetail: Ref(UserDetail),\r\n  ) key(userId), unique(loginName)\r\n\r\n  entity UserDetail(\r\n    user: Ref(User),\r\n    version: Long as UserDetailVersion,\r\n    profileImage: String?,\r\n    homeShop: Ref(Shop)?\r\n  ) key(user)\r\n\r\n  entity Following(\r\n    follower: Ref(User),\r\n    following: Ref(User),\r\n  ) key(follower, following)\r\n}"))
  }
}
