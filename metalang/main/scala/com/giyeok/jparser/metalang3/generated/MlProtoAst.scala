package com.giyeok.jparser.metalang3.generated

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

object MlProtoAst {
  val ngrammar =   new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("Program"), Set(3)),
  4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("WS"), Set(5)),
  6 -> NGrammar.NRepeat(6, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet)))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0), 7, 8),
  9 -> NGrammar.NOneOf(9, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet)))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), Set(10,13)),
  10 -> NGrammar.NProxy(10, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet)))), 11),
  12 -> NGrammar.NTerminal(12, Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet)),
  13 -> NGrammar.NProxy(13, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))), 14),
  15 -> NGrammar.NNonterminal(15, Symbols.Nonterminal("LineComment"), Set(16)),
  17 -> NGrammar.NProxy(17, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('/'),Symbols.ExactChar('/')))), 18),
  19 -> NGrammar.NTerminal(19, Symbols.ExactChar('/')),
  20 -> NGrammar.NRepeat(20, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), 0), 7, 21),
  22 -> NGrammar.NOneOf(22, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), Set(23)),
  23 -> NGrammar.NProxy(23, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))), 24),
  25 -> NGrammar.NExcept(25, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 26, 27),
  26 -> NGrammar.NTerminal(26, Symbols.AnyChar),
  27 -> NGrammar.NTerminal(27, Symbols.ExactChar('\n')),
  28 -> NGrammar.NOneOf(28, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("EOF")))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\n')))))), Set(29,34)),
  29 -> NGrammar.NProxy(29, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("EOF")))), 30),
  31 -> NGrammar.NNonterminal(31, Symbols.Nonterminal("EOF"), Set(32)),
  33 -> NGrammar.NLookaheadExcept(33, Symbols.LookaheadExcept(Symbols.AnyChar), 7, 26),
  34 -> NGrammar.NProxy(34, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\n')))), 35),
  36 -> NGrammar.NNonterminal(36, Symbols.Nonterminal("Module"), Set(37)),
  38 -> NGrammar.NJoin(38, Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'),Symbols.ExactChar('o'),Symbols.ExactChar('d'),Symbols.ExactChar('u'),Symbols.ExactChar('l'),Symbols.ExactChar('e')))), Symbols.Nonterminal("Tk")), 39, 47),
  39 -> NGrammar.NProxy(39, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'),Symbols.ExactChar('o'),Symbols.ExactChar('d'),Symbols.ExactChar('u'),Symbols.ExactChar('l'),Symbols.ExactChar('e')))), 40),
  41 -> NGrammar.NTerminal(41, Symbols.ExactChar('m')),
  42 -> NGrammar.NTerminal(42, Symbols.ExactChar('o')),
  43 -> NGrammar.NTerminal(43, Symbols.ExactChar('d')),
  44 -> NGrammar.NTerminal(44, Symbols.ExactChar('u')),
  45 -> NGrammar.NTerminal(45, Symbols.ExactChar('l')),
  46 -> NGrammar.NTerminal(46, Symbols.ExactChar('e')),
  47 -> NGrammar.NNonterminal(47, Symbols.Nonterminal("Tk"), Set(48)),
  49 -> NGrammar.NLongest(49, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('/' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar(':'),Symbols.ExactChar('='))))))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'))))))), 50),
  50 -> NGrammar.NOneOf(50, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('/' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar(':'),Symbols.ExactChar('='))))))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?')))))), Set(51,57,63)),
  51 -> NGrammar.NProxy(51, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('/' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))), 52),
  53 -> NGrammar.NTerminal(53, Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
  54 -> NGrammar.NRepeat(54, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('/' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), 7, 55),
  56 -> NGrammar.NTerminal(56, Symbols.Chars(Set('_') ++ ('/' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
  57 -> NGrammar.NProxy(57, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar(':'),Symbols.ExactChar('='))))))), 58),
  59 -> NGrammar.NProxy(59, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar(':'),Symbols.ExactChar('=')))), 60),
  61 -> NGrammar.NTerminal(61, Symbols.ExactChar(':')),
  62 -> NGrammar.NTerminal(62, Symbols.ExactChar('=')),
  63 -> NGrammar.NProxy(63, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?')))), 64),
  65 -> NGrammar.NTerminal(65, Symbols.ExactChar('?')),
  66 -> NGrammar.NNonterminal(66, Symbols.Nonterminal("Id"), Set(67)),
  68 -> NGrammar.NLongest(68, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))), 69),
  69 -> NGrammar.NOneOf(69, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))))), Set(70)),
  70 -> NGrammar.NProxy(70, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))), 71),
  72 -> NGrammar.NRepeat(72, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), 7, 73),
  74 -> NGrammar.NTerminal(74, Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
  75 -> NGrammar.NOneOf(75, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeGenerics")))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(76,83)),
  76 -> NGrammar.NOneOf(76, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeGenerics")))))), Set(77)),
  77 -> NGrammar.NProxy(77, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeGenerics")))), 78),
  79 -> NGrammar.NNonterminal(79, Symbols.Nonterminal("TypeGenerics"), Set(80)),
  81 -> NGrammar.NTerminal(81, Symbols.ExactChar('<')),
  82 -> NGrammar.NTerminal(82, Symbols.ExactChar('>')),
  83 -> NGrammar.NProxy(83, Symbols.Proxy(Symbols.Sequence(Seq())), 7),
  84 -> NGrammar.NTerminal(84, Symbols.ExactChar('(')),
  85 -> NGrammar.NOneOf(85, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParams")))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(86,83)),
  86 -> NGrammar.NOneOf(86, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParams")))))), Set(87)),
  87 -> NGrammar.NProxy(87, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParams")))), 88),
  89 -> NGrammar.NNonterminal(89, Symbols.Nonterminal("ModuleParams"), Set(90)),
  91 -> NGrammar.NNonterminal(91, Symbols.Nonterminal("ModuleParam"), Set(92)),
  93 -> NGrammar.NNonterminal(93, Symbols.Nonterminal("ParamName"), Set(94)),
  95 -> NGrammar.NNonterminal(95, Symbols.Nonterminal("ParamType"), Set(94)),
  96 -> NGrammar.NOneOf(96, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(97,83)),
  97 -> NGrammar.NOneOf(97, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))), Set(98)),
  98 -> NGrammar.NProxy(98, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))), 99),
  100 -> NGrammar.NNonterminal(100, Symbols.Nonterminal("Expr"), Set(101)),
  102 -> NGrammar.NTerminal(102, Symbols.ExactChar('b')),
  103 -> NGrammar.NRepeat(103, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParam")))))), 0), 7, 104),
  105 -> NGrammar.NOneOf(105, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParam")))))), Set(106)),
  106 -> NGrammar.NProxy(106, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParam")))), 107),
  108 -> NGrammar.NTerminal(108, Symbols.ExactChar(',')),
  109 -> NGrammar.NTerminal(109, Symbols.ExactChar(')')),
  110 -> NGrammar.NTerminal(110, Symbols.ExactChar('{')),
  111 -> NGrammar.NOneOf(111, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleBody")))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(112,83)),
  112 -> NGrammar.NOneOf(112, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleBody")))))), Set(113)),
  113 -> NGrammar.NProxy(113, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleBody")))), 114),
  115 -> NGrammar.NNonterminal(115, Symbols.Nonterminal("ModuleBody"), Set(116)),
  117 -> NGrammar.NTerminal(117, Symbols.ExactChar('a')),
  118 -> NGrammar.NTerminal(118, Symbols.ExactChar('}')),
  119 -> NGrammar.NRepeat(119, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Module")))))), 0), 7, 120),
  121 -> NGrammar.NOneOf(121, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Module")))))), Set(122)),
  122 -> NGrammar.NProxy(122, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Module")))), 123)),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Module"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Module")))))), 0),Symbols.Nonterminal("WS"))), Seq(4,36,119,4)),
  5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet)))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0))), Seq(6)),
  7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq()), Seq()),
  8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet)))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet)))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment")))))))), Seq(6,9)),
  11 -> NGrammar.NSequence(11, Symbols.Sequence(Seq(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet))), Seq(12)),
  14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq(Symbols.Nonterminal("LineComment"))), Seq(15)),
  16 -> NGrammar.NSequence(16, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('/'),Symbols.ExactChar('/')))),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("EOF")))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('\n')))))))), Seq(17,20,28)),
  18 -> NGrammar.NSequence(18, Symbols.Sequence(Seq(Symbols.ExactChar('/'),Symbols.ExactChar('/'))), Seq(19,19)),
  21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n'))))))))), Seq(20,22)),
  24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')))), Seq(25)),
  30 -> NGrammar.NSequence(30, Symbols.Sequence(Seq(Symbols.Nonterminal("EOF"))), Seq(31)),
  32 -> NGrammar.NSequence(32, Symbols.Sequence(Seq(Symbols.LookaheadExcept(Symbols.AnyChar))), Seq(33)),
  35 -> NGrammar.NSequence(35, Symbols.Sequence(Seq(Symbols.ExactChar('\n'))), Seq(27)),
  37 -> NGrammar.NSequence(37, Symbols.Sequence(Seq(Symbols.Join(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('m'),Symbols.ExactChar('o'),Symbols.ExactChar('d'),Symbols.ExactChar('u'),Symbols.ExactChar('l'),Symbols.ExactChar('e')))), Symbols.Nonterminal("Tk")),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Id"),Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeGenerics")))))),Symbols.Proxy(Symbols.Sequence(Seq())))),Symbols.Nonterminal("WS"),Symbols.ExactChar('('),Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParams")))))),Symbols.Proxy(Symbols.Sequence(Seq())))),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'),Symbols.Nonterminal("WS"),Symbols.ExactChar('{'),Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleBody")))))),Symbols.Proxy(Symbols.Sequence(Seq())))),Symbols.Nonterminal("WS"),Symbols.ExactChar('}'))), Seq(38,4,66,75,4,84,85,4,109,4,110,111,4,118)),
  40 -> NGrammar.NSequence(40, Symbols.Sequence(Seq(Symbols.ExactChar('m'),Symbols.ExactChar('o'),Symbols.ExactChar('d'),Symbols.ExactChar('u'),Symbols.ExactChar('l'),Symbols.ExactChar('e'))), Seq(41,42,43,44,45,46)),
  48 -> NGrammar.NSequence(48, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('/' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar(':'),Symbols.ExactChar('='))))))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('?'))))))))), Seq(49)),
  52 -> NGrammar.NSequence(52, Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('/' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))), Seq(53,54)),
  55 -> NGrammar.NSequence(55, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('/' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0),Symbols.Chars(Set('_') ++ ('/' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(54,56)),
  58 -> NGrammar.NSequence(58, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar(':'),Symbols.ExactChar('=')))))), Seq(59)),
  60 -> NGrammar.NSequence(60, Symbols.Sequence(Seq(Symbols.ExactChar(':'),Symbols.ExactChar('='))), Seq(61,62)),
  64 -> NGrammar.NSequence(64, Symbols.Sequence(Seq(Symbols.ExactChar('?'))), Seq(65)),
  67 -> NGrammar.NSequence(67, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))))), Seq(68)),
  71 -> NGrammar.NSequence(71, Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet),Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))), Seq(53,72)),
  73 -> NGrammar.NSequence(73, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0),Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(72,74)),
  78 -> NGrammar.NSequence(78, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("TypeGenerics"))), Seq(4,79)),
  80 -> NGrammar.NSequence(80, Symbols.Sequence(Seq(Symbols.ExactChar('<'),Symbols.Nonterminal("WS"),Symbols.ExactChar('>'))), Seq(81,4,82)),
  88 -> NGrammar.NSequence(88, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParams"))), Seq(4,89)),
  90 -> NGrammar.NSequence(90, Symbols.Sequence(Seq(Symbols.Nonterminal("ModuleParam"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParam")))))), 0))), Seq(91,103)),
  92 -> NGrammar.NSequence(92, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"),Symbols.Nonterminal("WS"),Symbols.ExactChar(':'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ParamType"),Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))),Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(93,4,61,4,95,96)),
  94 -> NGrammar.NSequence(94, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(66)),
  99 -> NGrammar.NSequence(99, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar('='),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr"))), Seq(4,62,4,100)),
  101 -> NGrammar.NSequence(101, Symbols.Sequence(Seq(Symbols.ExactChar('b'))), Seq(102)),
  104 -> NGrammar.NSequence(104, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParam")))))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParam")))))))), Seq(103,105)),
  107 -> NGrammar.NSequence(107, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleParam"))), Seq(4,108,4,91)),
  114 -> NGrammar.NSequence(114, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("ModuleBody"))), Seq(4,115)),
  116 -> NGrammar.NSequence(116, Symbols.Sequence(Seq(Symbols.ExactChar('a'))), Seq(117)),
  120 -> NGrammar.NSequence(120, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Module")))))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Module")))))))), Seq(119,121)),
  123 -> NGrammar.NSequence(123, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.Nonterminal("Module"))), Seq(4,36))),
    1)

  sealed trait WithParseNode { val parseNode: Node }
  case class MLProto(modules: List[Module])(override val parseNode: Node) extends WithParseNode
  case class Module(name: String, generics: Option[Char], params: List[ModuleParam], body: Option[Char])(override val parseNode: Node) extends WithParseNode
  case class ModuleParam(name: ParamName, typ: String, defaultValue: Option[Char])(override val parseNode: Node) extends WithParseNode
  case class ParamName(value: String)(override val parseNode: Node) extends WithParseNode


  def matchExpr(node: Node): Char = {
    val BindNode(v1, v2) = node
    val v6 = v1.id match {
      case 101 =>
        val v3 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v4, v5) = v3
        assert(v4.id == 102)
        v5.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v6
  }

  def matchId(node: Node): String = {
    val BindNode(v7, v8) = node
    val v10 = v7.id match {
      case 67 =>
        val v9 = v8.asInstanceOf[SequenceNode].children.head
        v9.sourceText
    }
    v10
  }

  def matchModule(node: Node): Module = {
    val BindNode(v11, v12) = node
    val v59 = v11.id match {
      case 37 =>
        val v13 = v12.asInstanceOf[SequenceNode].children(2)
        val BindNode(v14, v15) = v13
        assert(v14.id == 66)
        val v16 = v12.asInstanceOf[SequenceNode].children(3)
        val BindNode(v17, v18) = v16
        assert(v17.id == 75)
        val BindNode(v19, v20) = v18
        val v29 = v19.id match {
        case 83 =>
        None
        case 76 =>
          val BindNode(v21, v22) = v20
          val v28 = v21.id match {
          case 77 =>
            val BindNode(v23, v24) = v22
            assert(v23.id == 78)
            val v25 = v24.asInstanceOf[SequenceNode].children(1)
            val BindNode(v26, v27) = v25
            assert(v26.id == 79)
            matchTypeGenerics(v27)
        }
          Some(v28)
      }
        val v31 = v12.asInstanceOf[SequenceNode].children(6)
        val BindNode(v32, v33) = v31
        assert(v32.id == 85)
        val BindNode(v34, v35) = v33
        val v44 = v34.id match {
        case 83 =>
        None
        case 86 =>
          val BindNode(v36, v37) = v35
          val v43 = v36.id match {
          case 87 =>
            val BindNode(v38, v39) = v37
            assert(v38.id == 88)
            val v40 = v39.asInstanceOf[SequenceNode].children(1)
            val BindNode(v41, v42) = v40
            assert(v41.id == 89)
            matchModuleParams(v42)
        }
          Some(v43)
      }
        val v30 = v44
        val v45 = v12.asInstanceOf[SequenceNode].children(11)
        val BindNode(v46, v47) = v45
        assert(v46.id == 111)
        val BindNode(v48, v49) = v47
        val v58 = v48.id match {
        case 83 =>
        None
        case 112 =>
          val BindNode(v50, v51) = v49
          val v57 = v50.id match {
          case 113 =>
            val BindNode(v52, v53) = v51
            assert(v52.id == 114)
            val v54 = v53.asInstanceOf[SequenceNode].children(1)
            val BindNode(v55, v56) = v54
            assert(v55.id == 115)
            matchModuleBody(v56)
        }
          Some(v57)
      }
        Module(matchId(v15), v29, if (v30.isDefined) v30.get else List(), v58)(v12)
    }
    v59
  }

  def matchModuleBody(node: Node): Char = {
    val BindNode(v60, v61) = node
    val v65 = v60.id match {
      case 116 =>
        val v62 = v61.asInstanceOf[SequenceNode].children.head
        val BindNode(v63, v64) = v62
        assert(v63.id == 117)
        v64.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v65
  }

  def matchModuleParam(node: Node): ModuleParam = {
    val BindNode(v66, v67) = node
    val v88 = v66.id match {
      case 92 =>
        val v68 = v67.asInstanceOf[SequenceNode].children.head
        val BindNode(v69, v70) = v68
        assert(v69.id == 93)
        val v71 = v67.asInstanceOf[SequenceNode].children(4)
        val BindNode(v72, v73) = v71
        assert(v72.id == 95)
        val v74 = v67.asInstanceOf[SequenceNode].children(5)
        val BindNode(v75, v76) = v74
        assert(v75.id == 96)
        val BindNode(v77, v78) = v76
        val v87 = v77.id match {
        case 83 =>
        None
        case 97 =>
          val BindNode(v79, v80) = v78
          val v86 = v79.id match {
          case 98 =>
            val BindNode(v81, v82) = v80
            assert(v81.id == 99)
            val v83 = v82.asInstanceOf[SequenceNode].children(3)
            val BindNode(v84, v85) = v83
            assert(v84.id == 100)
            matchExpr(v85)
        }
          Some(v86)
      }
        ModuleParam(matchParamName(v70), matchParamType(v73), v87)(v67)
    }
    v88
  }

  def matchModuleParams(node: Node): List[ModuleParam] = {
    val BindNode(v89, v90) = node
    val v106 = v89.id match {
      case 90 =>
        val v91 = v90.asInstanceOf[SequenceNode].children.head
        val BindNode(v92, v93) = v91
        assert(v92.id == 91)
        val v94 = v90.asInstanceOf[SequenceNode].children(1)
        val v95 = unrollRepeat0(v94).map { elem =>
        val BindNode(v96, v97) = elem
        assert(v96.id == 105)
        val BindNode(v98, v99) = v97
        val v105 = v98.id match {
        case 106 =>
          val BindNode(v100, v101) = v99
          assert(v100.id == 107)
          val v102 = v101.asInstanceOf[SequenceNode].children(3)
          val BindNode(v103, v104) = v102
          assert(v103.id == 91)
          matchModuleParam(v104)
      }
        v105
        }
        List(matchModuleParam(v93)) ++ v95
    }
    v106
  }

  def matchParamName(node: Node): ParamName = {
    val BindNode(v107, v108) = node
    val v112 = v107.id match {
      case 94 =>
        val v109 = v108.asInstanceOf[SequenceNode].children.head
        val BindNode(v110, v111) = v109
        assert(v110.id == 66)
        ParamName(matchId(v111))(v108)
    }
    v112
  }

  def matchParamType(node: Node): String = {
    val BindNode(v113, v114) = node
    val v118 = v113.id match {
      case 94 =>
        val v115 = v114.asInstanceOf[SequenceNode].children.head
        val BindNode(v116, v117) = v115
        assert(v116.id == 66)
        matchId(v117)
    }
    v118
  }

  def matchProgram(node: Node): MLProto = {
    val BindNode(v119, v120) = node
    val v136 = v119.id match {
      case 3 =>
        val v121 = v120.asInstanceOf[SequenceNode].children(1)
        val BindNode(v122, v123) = v121
        assert(v122.id == 36)
        val v124 = v120.asInstanceOf[SequenceNode].children(2)
        val v125 = unrollRepeat0(v124).map { elem =>
        val BindNode(v126, v127) = elem
        assert(v126.id == 121)
        val BindNode(v128, v129) = v127
        val v135 = v128.id match {
        case 122 =>
          val BindNode(v130, v131) = v129
          assert(v130.id == 123)
          val v132 = v131.asInstanceOf[SequenceNode].children(1)
          val BindNode(v133, v134) = v132
          assert(v133.id == 36)
          matchModule(v134)
      }
        v135
        }
        MLProto(List(matchModule(v123)) ++ v125)(v120)
    }
    v136
  }

  def matchTypeGenerics(node: Node): Char = {
    val BindNode(v137, v138) = node
    val v142 = v137.id match {
      case 80 =>
        val v139 = v138.asInstanceOf[SequenceNode].children(2)
        val BindNode(v140, v141) = v139
        assert(v140.id == 82)
        v141.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v142
  }

  def matchStart(node: Node): MLProto = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchProgram(body)
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[MLProto, ParsingErrors.ParsingError] =
    ParseTreeUtil.parseAst(naiveParser, text, matchStart)

  def main(args: Array[String]): Unit = {
    println(parseAst("module Conv2d<>(inChannels: Int) {}"))
  }
}
