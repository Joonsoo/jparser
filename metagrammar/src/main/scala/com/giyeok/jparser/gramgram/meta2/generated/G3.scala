import com.giyeok.jparser.Inputs.InputToShortString
import com.giyeok.jparser.ParseResultTree.{JoinNode, Node, BindNode, TerminalNode, SequenceNode}
import com.giyeok.jparser.nparser.{NGrammar, ParseTreeConstructor, NaiveParser, Parser}
import com.giyeok.jparser.{ParsingErrors, ParseForestFunc, Symbols}
import scala.collection.immutable.ListSet

object G3 {
    val ngrammar = new NGrammar(
        Map( // <start>,
            1 -> NGrammar.NStart(1, 2),
            // Grammar,
            2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("Grammar"), Set(3)),
            // WS,
            4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("WS"), Set(5)),
            // {\t-\n\r\u0020}|LineComment*,
            6 -> NGrammar.NRepeat(6, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet), Symbols.Nonterminal("LineComment"))), 0), 7, 8),
            // {\t-\n\r\u0020}|LineComment,
            9 -> NGrammar.NOneOf(9, Symbols.OneOf(ListSet(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet), Symbols.Nonterminal("LineComment"))), Set(10, 11)),
            // {\t-\n\r\u0020},
            10 -> NGrammar.NTerminal(10, Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet)),
            // LineComment,
            11 -> NGrammar.NNonterminal(11, Symbols.Nonterminal("LineComment"), Set(12)),
            // '/',
            13 -> NGrammar.NTerminal(13, Symbols.ExactChar('/')),
            // <any>-'\n'*,
            14 -> NGrammar.NRepeat(14, Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0), 7, 15),
            // <any>-'\n',
            16 -> NGrammar.NExcept(16, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 17, 18),
            // <any>,
            17 -> NGrammar.NTerminal(17, Symbols.AnyChar),
            // '\n',
            18 -> NGrammar.NTerminal(18, Symbols.ExactChar('\n')),
            // EOF|'\n',
            19 -> NGrammar.NOneOf(19, Symbols.OneOf(ListSet(Symbols.Nonterminal("EOF"), Symbols.ExactChar('\n'))), Set(20, 18)),
            // EOF,
            20 -> NGrammar.NNonterminal(20, Symbols.Nonterminal("EOF"), Set(21)),
            // la_except <any>,
            22 -> NGrammar.NLookaheadExcept(22, Symbols.LookaheadExcept(Symbols.AnyChar), 7, 17),
            // Def,
            23 -> NGrammar.NNonterminal(23, Symbols.Nonterminal("Def"), Set(24, 243)),
            // Rule,
            25 -> NGrammar.NNonterminal(25, Symbols.Nonterminal("Rule"), Set(26)),
            // LHS,
            27 -> NGrammar.NNonterminal(27, Symbols.Nonterminal("LHS"), Set(28)),
            // Nonterminal,
            29 -> NGrammar.NNonterminal(29, Symbols.Nonterminal("Nonterminal"), Set(30)),
            // Id,
            31 -> NGrammar.NNonterminal(31, Symbols.Nonterminal("Id"), Set(32)),
            // <([{A-Z_a-z} {0-9A-Z_a-z}*])>,
            33 -> NGrammar.NLongest(33, Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))), 34),
            // ([{A-Z_a-z} {0-9A-Z_a-z}*]),
            34 -> NGrammar.NProxy(34, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0)))), 35),
            // {A-Z_a-z},
            36 -> NGrammar.NTerminal(36, Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
            // {0-9A-Z_a-z}*,
            37 -> NGrammar.NRepeat(37, Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), 7, 38),
            // {0-9A-Z_a-z},
            39 -> NGrammar.NTerminal(39, Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
            // ([WS ':' WS TypeDesc])?,
            40 -> NGrammar.NOneOf(40, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Set(41, 42)),
            // ([]),
            41 -> NGrammar.NProxy(41, Symbols.Proxy(Symbols.Sequence(Seq())), 7),
            // ([WS ':' WS TypeDesc]),
            42 -> NGrammar.NProxy(42, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))), 43),
            // ':',
            44 -> NGrammar.NTerminal(44, Symbols.ExactChar(':')),
            // TypeDesc,
            45 -> NGrammar.NNonterminal(45, Symbols.Nonterminal("TypeDesc"), Set(46)),
            // ValueTypeDesc,
            47 -> NGrammar.NNonterminal(47, Symbols.Nonterminal("ValueTypeDesc"), Set(48, 50, 66)),
            // TypeName,
            49 -> NGrammar.NNonterminal(49, Symbols.Nonterminal("TypeName"), Set(30)),
            // OnTheFlyTypeDef,
            51 -> NGrammar.NNonterminal(51, Symbols.Nonterminal("OnTheFlyTypeDef"), Set(52)),
            // '@',
            53 -> NGrammar.NTerminal(53, Symbols.ExactChar('@')),
            // ([WS OnTheFlySuperTypes])?,
            54 -> NGrammar.NOneOf(54, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("OnTheFlySuperTypes")))))), Set(41, 55)),
            // ([WS OnTheFlySuperTypes]),
            55 -> NGrammar.NProxy(55, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("OnTheFlySuperTypes")))), 56),
            // OnTheFlySuperTypes,
            57 -> NGrammar.NNonterminal(57, Symbols.Nonterminal("OnTheFlySuperTypes"), Set(58)),
            // '<',
            59 -> NGrammar.NTerminal(59, Symbols.ExactChar('<')),
            // ([WS ',' WS TypeName])*,
            60 -> NGrammar.NRepeat(60, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))), 0), 7, 61),
            // ([WS ',' WS TypeName]),
            62 -> NGrammar.NProxy(62, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))), 63),
            // ',',
            64 -> NGrammar.NTerminal(64, Symbols.ExactChar(',')),
            // '>',
            65 -> NGrammar.NTerminal(65, Symbols.ExactChar('>')),
            // '[',
            67 -> NGrammar.NTerminal(67, Symbols.ExactChar('[')),
            // ']',
            68 -> NGrammar.NTerminal(68, Symbols.ExactChar(']')),
            // ([WS '?'])?,
            69 -> NGrammar.NOneOf(69, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))))), Set(41, 70)),
            // ([WS '?']),
            70 -> NGrammar.NProxy(70, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))), 71),
            // '?',
            72 -> NGrammar.NTerminal(72, Symbols.ExactChar('?')),
            // '=',
            73 -> NGrammar.NTerminal(73, Symbols.ExactChar('=')),
            // RHSs,
            74 -> NGrammar.NNonterminal(74, Symbols.Nonterminal("RHSs"), Set(75)),
            // RHS,
            76 -> NGrammar.NNonterminal(76, Symbols.Nonterminal("RHS"), Set(77)),
            // Elem,
            78 -> NGrammar.NNonterminal(78, Symbols.Nonterminal("Elem"), Set(79, 146)),
            // Processor,
            80 -> NGrammar.NNonterminal(80, Symbols.Nonterminal("Processor"), Set(81, 96)),
            // Ref,
            82 -> NGrammar.NNonterminal(82, Symbols.Nonterminal("Ref"), Set(83)),
            // '$',
            84 -> NGrammar.NTerminal(84, Symbols.ExactChar('$')),
            // RefIdx,
            85 -> NGrammar.NNonterminal(85, Symbols.Nonterminal("RefIdx"), Set(86)),
            // <'0'|([{1-9} {0-9}*])>,
            87 -> NGrammar.NLongest(87, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))), 88),
            // '0'|([{1-9} {0-9}*]),
            88 -> NGrammar.NOneOf(88, Symbols.OneOf(ListSet(Symbols.ExactChar('0'), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))))), Set(89, 90)),
            // '0',
            89 -> NGrammar.NTerminal(89, Symbols.ExactChar('0')),
            // ([{1-9} {0-9}*]),
            90 -> NGrammar.NProxy(90, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0)))), 91),
            // {1-9},
            92 -> NGrammar.NTerminal(92, Symbols.Chars(('1' to '9').toSet)),
            // {0-9}*,
            93 -> NGrammar.NRepeat(93, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 7, 94),
            // {0-9},
            95 -> NGrammar.NTerminal(95, Symbols.Chars(('0' to '9').toSet)),
            // '{',
            97 -> NGrammar.NTerminal(97, Symbols.ExactChar('{')),
            // PExpr,
            98 -> NGrammar.NNonterminal(98, Symbols.Nonterminal("PExpr"), Set(99, 144)),
            // <BinOp>,
            100 -> NGrammar.NLongest(100, Symbols.Longest(Symbols.Nonterminal("BinOp")), 101),
            // BinOp,
            101 -> NGrammar.NNonterminal(101, Symbols.Nonterminal("BinOp"), Set(102)),
            // (['+']),
            103 -> NGrammar.NProxy(103, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))), 104),
            // '+',
            105 -> NGrammar.NTerminal(105, Symbols.ExactChar('+')),
            // PTerm,
            106 -> NGrammar.NNonterminal(106, Symbols.Nonterminal("PTerm"), Set(81, 107, 111, 140, 141)),
            // BoundPExpr,
            108 -> NGrammar.NNonterminal(108, Symbols.Nonterminal("BoundPExpr"), Set(109)),
            // BoundedPExpr,
            110 -> NGrammar.NNonterminal(110, Symbols.Nonterminal("BoundedPExpr"), Set(81, 107, 96)),
            // ConstructExpr,
            112 -> NGrammar.NNonterminal(112, Symbols.Nonterminal("ConstructExpr"), Set(113, 125)),
            // ConstructParams,
            114 -> NGrammar.NNonterminal(114, Symbols.Nonterminal("ConstructParams"), Set(115)),
            // '(',
            116 -> NGrammar.NTerminal(116, Symbols.ExactChar('(')),
            // ([PExpr ([WS ',' WS PExpr])* WS])?,
            117 -> NGrammar.NOneOf(117, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 0), Symbols.Nonterminal("WS")))))), Set(41, 118)),
            // ([PExpr ([WS ',' WS PExpr])* WS]),
            118 -> NGrammar.NProxy(118, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 0), Symbols.Nonterminal("WS")))), 119),
            // ([WS ',' WS PExpr])*,
            120 -> NGrammar.NRepeat(120, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 0), 7, 121),
            // ([WS ',' WS PExpr]),
            122 -> NGrammar.NProxy(122, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 123),
            // ')',
            124 -> NGrammar.NTerminal(124, Symbols.ExactChar(')')),
            // OnTheFlyTypeDefConstructExpr,
            126 -> NGrammar.NNonterminal(126, Symbols.Nonterminal("OnTheFlyTypeDefConstructExpr"), Set(127)),
            // NamedParams,
            128 -> NGrammar.NNonterminal(128, Symbols.Nonterminal("NamedParams"), Set(129)),
            // ([NamedParam ([WS ',' WS NamedParam])* WS])?,
            130 -> NGrammar.NOneOf(130, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))), 0), Symbols.Nonterminal("WS")))))), Set(41, 131)),
            // ([NamedParam ([WS ',' WS NamedParam])* WS]),
            131 -> NGrammar.NProxy(131, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))), 0), Symbols.Nonterminal("WS")))), 132),
            // NamedParam,
            133 -> NGrammar.NNonterminal(133, Symbols.Nonterminal("NamedParam"), Set(134)),
            // ParamName,
            135 -> NGrammar.NNonterminal(135, Symbols.Nonterminal("ParamName"), Set(30)),
            // ([WS ',' WS NamedParam])*,
            136 -> NGrammar.NRepeat(136, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))), 0), 7, 137),
            // ([WS ',' WS NamedParam]),
            138 -> NGrammar.NProxy(138, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))), 139),
            // ArrayTerm,
            142 -> NGrammar.NNonterminal(142, Symbols.Nonterminal("ArrayTerm"), Set(143)),
            // '}',
            145 -> NGrammar.NTerminal(145, Symbols.ExactChar('}')),
            // Symbol,
            147 -> NGrammar.NNonterminal(147, Symbols.Nonterminal("Symbol"), Set(148)),
            // BinSymbol,
            149 -> NGrammar.NNonterminal(149, Symbols.Nonterminal("BinSymbol"), Set(150, 233, 234)),
            // '&',
            151 -> NGrammar.NTerminal(151, Symbols.ExactChar('&')),
            // PreUnSymbol,
            152 -> NGrammar.NNonterminal(152, Symbols.Nonterminal("PreUnSymbol"), Set(153, 155, 157)),
            // '^',
            154 -> NGrammar.NTerminal(154, Symbols.ExactChar('^')),
            // '!',
            156 -> NGrammar.NTerminal(156, Symbols.ExactChar('!')),
            // PostUnSymbol,
            158 -> NGrammar.NNonterminal(158, Symbols.Nonterminal("PostUnSymbol"), Set(159, 162)),
            // '?'|'*'|'+',
            160 -> NGrammar.NOneOf(160, Symbols.OneOf(ListSet(Symbols.ExactChar('?'), Symbols.ExactChar('*'), Symbols.ExactChar('+'))), Set(72, 161, 105)),
            // '*',
            161 -> NGrammar.NTerminal(161, Symbols.ExactChar('*')),
            // AtomSymbol,
            163 -> NGrammar.NNonterminal(163, Symbols.Nonterminal("AtomSymbol"), Set(164, 181, 199, 211, 212, 226, 229)),
            // Terminal,
            165 -> NGrammar.NNonterminal(165, Symbols.Nonterminal("Terminal"), Set(166, 179)),
            // ''',
            167 -> NGrammar.NTerminal(167, Symbols.ExactChar('\'')),
            // TerminalChar,
            168 -> NGrammar.NNonterminal(168, Symbols.Nonterminal("TerminalChar"), Set(169, 172, 174)),
            // <any>-'\',
            170 -> NGrammar.NExcept(170, Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')), 17, 171),
            // '\',
            171 -> NGrammar.NTerminal(171, Symbols.ExactChar('\\')),
            // {'\bnrt},
            173 -> NGrammar.NTerminal(173, Symbols.Chars(Set('\'', '\\', 'b', 'n', 'r', 't'))),
            // UnicodeChar,
            175 -> NGrammar.NNonterminal(175, Symbols.Nonterminal("UnicodeChar"), Set(176)),
            // 'u',
            177 -> NGrammar.NTerminal(177, Symbols.ExactChar('u')),
            // {0-9A-Fa-f},
            178 -> NGrammar.NTerminal(178, Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet)),
            // '.',
            180 -> NGrammar.NTerminal(180, Symbols.ExactChar('.')),
            // TerminalChoice,
            182 -> NGrammar.NNonterminal(182, Symbols.Nonterminal("TerminalChoice"), Set(183, 198)),
            // TerminalChoiceElem,
            184 -> NGrammar.NNonterminal(184, Symbols.Nonterminal("TerminalChoiceElem"), Set(185, 192)),
            // TerminalChoiceChar,
            186 -> NGrammar.NNonterminal(186, Symbols.Nonterminal("TerminalChoiceChar"), Set(187, 190, 174)),
            // <any>-{'-\},
            188 -> NGrammar.NExcept(188, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'', '-', '\\'))), 17, 189),
            // {'-\},
            189 -> NGrammar.NTerminal(189, Symbols.Chars(Set('\'', '-', '\\'))),
            // {'-\bnrt},
            191 -> NGrammar.NTerminal(191, Symbols.Chars(Set('\'', '-', '\\', 'b', 'n', 'r', 't'))),
            // TerminalChoiceRange,
            193 -> NGrammar.NNonterminal(193, Symbols.Nonterminal("TerminalChoiceRange"), Set(194)),
            // '-',
            195 -> NGrammar.NTerminal(195, Symbols.ExactChar('-')),
            // TerminalChoiceElem+,
            196 -> NGrammar.NRepeat(196, Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), 184, 197),
            // StringLiteral,
            200 -> NGrammar.NNonterminal(200, Symbols.Nonterminal("StringLiteral"), Set(201)),
            // '"',
            202 -> NGrammar.NTerminal(202, Symbols.ExactChar('"')),
            // StringChar*,
            203 -> NGrammar.NRepeat(203, Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), 7, 204),
            // StringChar,
            205 -> NGrammar.NNonterminal(205, Symbols.Nonterminal("StringChar"), Set(206, 209, 174)),
            // <any>-{"\},
            207 -> NGrammar.NExcept(207, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"', '\\'))), 17, 208),
            // {"\},
            208 -> NGrammar.NTerminal(208, Symbols.Chars(Set('"', '\\'))),
            // {"\bnrt},
            210 -> NGrammar.NTerminal(210, Symbols.Chars(Set('"', '\\', 'b', 'n', 'r', 't'))),
            // InPlaceChoices,
            213 -> NGrammar.NNonterminal(213, Symbols.Nonterminal("InPlaceChoices"), Set(214)),
            // InPlaceSequence,
            215 -> NGrammar.NNonterminal(215, Symbols.Nonterminal("InPlaceSequence"), Set(216)),
            // ([WS Symbol])*,
            217 -> NGrammar.NRepeat(217, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Symbol")))), 0), 7, 218),
            // ([WS Symbol]),
            219 -> NGrammar.NProxy(219, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Symbol")))), 220),
            // ([WS '|' WS InPlaceSequence])*,
            221 -> NGrammar.NRepeat(221, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceSequence")))), 0), 7, 222),
            // ([WS '|' WS InPlaceSequence]),
            223 -> NGrammar.NProxy(223, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceSequence")))), 224),
            // '|',
            225 -> NGrammar.NTerminal(225, Symbols.ExactChar('|')),
            // Longest,
            227 -> NGrammar.NNonterminal(227, Symbols.Nonterminal("Longest"), Set(228)),
            // EmptySequence,
            230 -> NGrammar.NNonterminal(230, Symbols.Nonterminal("EmptySequence"), Set(231)),
            // '#',
            232 -> NGrammar.NTerminal(232, Symbols.ExactChar('#')),
            // ([WS Elem])*,
            235 -> NGrammar.NRepeat(235, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))), 0), 7, 236),
            // ([WS Elem]),
            237 -> NGrammar.NProxy(237, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))), 238),
            // ([WS '|' WS RHS])*,
            239 -> NGrammar.NRepeat(239, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))), 0), 7, 240),
            // ([WS '|' WS RHS]),
            241 -> NGrammar.NProxy(241, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))), 242),
            // TypeDef,
            244 -> NGrammar.NNonterminal(244, Symbols.Nonterminal("TypeDef"), Set(245, 259)),
            // ClassDef,
            246 -> NGrammar.NNonterminal(246, Symbols.Nonterminal("ClassDef"), Set(247)),
            // ([ClassParams WS])?,
            248 -> NGrammar.NOneOf(248, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParams"), Symbols.Nonterminal("WS")))))), Set(41, 249)),
            // ([ClassParams WS]),
            249 -> NGrammar.NProxy(249, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParams"), Symbols.Nonterminal("WS")))), 250),
            // ClassParams,
            251 -> NGrammar.NNonterminal(251, Symbols.Nonterminal("ClassParams"), Set(252)),
            // ClassParam,
            253 -> NGrammar.NNonterminal(253, Symbols.Nonterminal("ClassParam"), Set(254)),
            // ([WS ',' WS ClassParam])*,
            255 -> NGrammar.NRepeat(255, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParam")))), 0), 7, 256),
            // ([WS ',' WS ClassParam]),
            257 -> NGrammar.NProxy(257, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParam")))), 258),
            // SuperDef,
            260 -> NGrammar.NNonterminal(260, Symbols.Nonterminal("SuperDef"), Set(261)),
            // ([SubTypes WS])?,
            262 -> NGrammar.NOneOf(262, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"), Symbols.Nonterminal("WS")))))), Set(41, 263)),
            // ([SubTypes WS]),
            263 -> NGrammar.NProxy(263, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"), Symbols.Nonterminal("WS")))), 264),
            // SubTypes,
            265 -> NGrammar.NNonterminal(265, Symbols.Nonterminal("SubTypes"), Set(266)),
            // SubType,
            267 -> NGrammar.NNonterminal(267, Symbols.Nonterminal("SubType"), Set(48, 268, 269)),
            // ([WS ',' WS SubType])*,
            270 -> NGrammar.NRepeat(270, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))), 0), 7, 271),
            // ([WS ',' WS SubType]),
            272 -> NGrammar.NProxy(272, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))), 273),
            // ([WS Def])*,
            274 -> NGrammar.NRepeat(274, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Def")))), 0), 7, 275),
            // ([WS Def]),
            276 -> NGrammar.NProxy(276, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Def")))), 277)),
        Map( // [WS Def ([WS Def])* WS],
            3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Def"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Def")))), 0), Symbols.Nonterminal("WS"))), Seq(4, 23, 274, 4)),
            // [{\t-\n\r\u0020}|LineComment*],
            5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet), Symbols.Nonterminal("LineComment"))), 0))), Seq(6)),
            // [],
            7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq()), Seq()),
            // [{\t-\n\r\u0020}|LineComment* {\t-\n\r\u0020}|LineComment],
            8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet), Symbols.Nonterminal("LineComment"))), 0), Symbols.OneOf(ListSet(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet), Symbols.Nonterminal("LineComment"))))), Seq(6, 9)),
            // ['/' '/' <any>-'\n'* EOF|'\n'],
            12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.ExactChar('/'), Symbols.ExactChar('/'), Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0), Symbols.OneOf(ListSet(Symbols.Nonterminal("EOF"), Symbols.ExactChar('\n'))))), Seq(13, 13, 14, 19)),
            // [<any>-'\n'* <any>-'\n'],
            15 -> NGrammar.NSequence(15, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')), 0), Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\n')))), Seq(14, 16)),
            // [la_except <any>],
            21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.LookaheadExcept(Symbols.AnyChar))), Seq(22)),
            // [Rule],
            24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Nonterminal("Rule"))), Seq(25)),
            // [LHS WS '=' WS RHSs],
            26 -> NGrammar.NSequence(26, Symbols.Sequence(Seq(Symbols.Nonterminal("LHS"), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHSs"))), Seq(27, 4, 73, 4, 74)),
            // [Nonterminal ([WS ':' WS TypeDesc])?],
            28 -> NGrammar.NSequence(28, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))))), Seq(29, 40)),
            // [Id],
            30 -> NGrammar.NSequence(30, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(31)),
            // [<([{A-Z_a-z} {0-9A-Z_a-z}*])>],
            32 -> NGrammar.NSequence(32, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))))))), Seq(33)),
            // [{A-Z_a-z} {0-9A-Z_a-z}*],
            35 -> NGrammar.NSequence(35, Symbols.Sequence(Seq(Symbols.Chars(Set('_') ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0))), Seq(36, 37)),
            // [{0-9A-Z_a-z}* {0-9A-Z_a-z}],
            38 -> NGrammar.NSequence(38, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet), 0), Symbols.Chars(Set('_') ++ ('0' to '9').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(37, 39)),
            // [WS ':' WS TypeDesc],
            43 -> NGrammar.NSequence(43, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc"))), Seq(4, 44, 4, 45)),
            // [ValueTypeDesc ([WS '?'])?],
            46 -> NGrammar.NSequence(46, Symbols.Sequence(Seq(Symbols.Nonterminal("ValueTypeDesc"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?')))))))), Seq(47, 69)),
            // [TypeName],
            48 -> NGrammar.NSequence(48, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"))), Seq(49)),
            // [OnTheFlyTypeDef],
            50 -> NGrammar.NSequence(50, Symbols.Sequence(Seq(Symbols.Nonterminal("OnTheFlyTypeDef"))), Seq(51)),
            // ['@' WS TypeName ([WS OnTheFlySuperTypes])?],
            52 -> NGrammar.NSequence(52, Symbols.Sequence(Seq(Symbols.ExactChar('@'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("OnTheFlySuperTypes")))))))), Seq(53, 4, 49, 54)),
            // [WS OnTheFlySuperTypes],
            56 -> NGrammar.NSequence(56, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("OnTheFlySuperTypes"))), Seq(4, 57)),
            // ['<' WS TypeName ([WS ',' WS TypeName])* WS '>'],
            58 -> NGrammar.NSequence(58, Symbols.Sequence(Seq(Symbols.ExactChar('<'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))), 0), Symbols.Nonterminal("WS"), Symbols.ExactChar('>'))), Seq(59, 4, 49, 60, 4, 65)),
            // [([WS ',' WS TypeName])* ([WS ',' WS TypeName])],
            61 -> NGrammar.NSequence(61, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))), 0), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName")))))), Seq(60, 62)),
            // [WS ',' WS TypeName],
            63 -> NGrammar.NSequence(63, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeName"))), Seq(4, 64, 4, 49)),
            // ['[' WS TypeDesc WS ']'],
            66 -> NGrammar.NSequence(66, Symbols.Sequence(Seq(Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc"), Symbols.Nonterminal("WS"), Symbols.ExactChar(']'))), Seq(67, 4, 45, 4, 68)),
            // [WS '?'],
            71 -> NGrammar.NSequence(71, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('?'))), Seq(4, 72)),
            // [RHS ([WS '|' WS RHS])*],
            75 -> NGrammar.NSequence(75, Symbols.Sequence(Seq(Symbols.Nonterminal("RHS"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))), 0))), Seq(76, 239)),
            // [Elem ([WS Elem])*],
            77 -> NGrammar.NSequence(77, Symbols.Sequence(Seq(Symbols.Nonterminal("Elem"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))), 0))), Seq(78, 235)),
            // [Processor],
            79 -> NGrammar.NSequence(79, Symbols.Sequence(Seq(Symbols.Nonterminal("Processor"))), Seq(80)),
            // [Ref],
            81 -> NGrammar.NSequence(81, Symbols.Sequence(Seq(Symbols.Nonterminal("Ref"))), Seq(82)),
            // ['$' RefIdx],
            83 -> NGrammar.NSequence(83, Symbols.Sequence(Seq(Symbols.ExactChar('$'), Symbols.Nonterminal("RefIdx"))), Seq(84, 85)),
            // [<'0'|([{1-9} {0-9}*])>],
            86 -> NGrammar.NSequence(86, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.ExactChar('0'), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))))))))), Seq(87)),
            // [{1-9} {0-9}*],
            91 -> NGrammar.NSequence(91, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(92, 93)),
            // [{0-9}* {0-9}],
            94 -> NGrammar.NSequence(94, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), Symbols.Chars(('0' to '9').toSet))), Seq(93, 95)),
            // ['{' WS PExpr WS '}'],
            96 -> NGrammar.NSequence(96, Symbols.Sequence(Seq(Symbols.ExactChar('{'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar('}'))), Seq(97, 4, 98, 4, 145)),
            // [PExpr WS <BinOp> WS PTerm],
            99 -> NGrammar.NSequence(99, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Nonterminal("WS"), Symbols.Longest(Symbols.Nonterminal("BinOp")), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PTerm"))), Seq(98, 4, 100, 4, 106)),
            // [(['+'])],
            102 -> NGrammar.NSequence(102, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('+')))))), Seq(103)),
            // ['+'],
            104 -> NGrammar.NSequence(104, Symbols.Sequence(Seq(Symbols.ExactChar('+'))), Seq(105)),
            // [BoundPExpr],
            107 -> NGrammar.NSequence(107, Symbols.Sequence(Seq(Symbols.Nonterminal("BoundPExpr"))), Seq(108)),
            // [Ref BoundedPExpr],
            109 -> NGrammar.NSequence(109, Symbols.Sequence(Seq(Symbols.Nonterminal("Ref"), Symbols.Nonterminal("BoundedPExpr"))), Seq(82, 110)),
            // [ConstructExpr],
            111 -> NGrammar.NSequence(111, Symbols.Sequence(Seq(Symbols.Nonterminal("ConstructExpr"))), Seq(112)),
            // [TypeName WS ConstructParams],
            113 -> NGrammar.NSequence(113, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ConstructParams"))), Seq(49, 4, 114)),
            // ['(' WS ([PExpr ([WS ',' WS PExpr])* WS])? ')'],
            115 -> NGrammar.NSequence(115, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 0), Symbols.Nonterminal("WS")))))), Symbols.ExactChar(')'))), Seq(116, 4, 117, 124)),
            // [PExpr ([WS ',' WS PExpr])* WS],
            119 -> NGrammar.NSequence(119, Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 0), Symbols.Nonterminal("WS"))), Seq(98, 120, 4)),
            // [([WS ',' WS PExpr])* ([WS ',' WS PExpr])],
            121 -> NGrammar.NSequence(121, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 0), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))))), Seq(120, 122)),
            // [WS ',' WS PExpr],
            123 -> NGrammar.NSequence(123, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"))), Seq(4, 64, 4, 98)),
            // [OnTheFlyTypeDefConstructExpr],
            125 -> NGrammar.NSequence(125, Symbols.Sequence(Seq(Symbols.Nonterminal("OnTheFlyTypeDefConstructExpr"))), Seq(126)),
            // [OnTheFlyTypeDef WS NamedParams],
            127 -> NGrammar.NSequence(127, Symbols.Sequence(Seq(Symbols.Nonterminal("OnTheFlyTypeDef"), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParams"))), Seq(51, 4, 128)),
            // ['(' WS ([NamedParam ([WS ',' WS NamedParam])* WS])? ')'],
            129 -> NGrammar.NSequence(129, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))), 0), Symbols.Nonterminal("WS")))))), Symbols.ExactChar(')'))), Seq(116, 4, 130, 124)),
            // [NamedParam ([WS ',' WS NamedParam])* WS],
            132 -> NGrammar.NSequence(132, Symbols.Sequence(Seq(Symbols.Nonterminal("NamedParam"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))), 0), Symbols.Nonterminal("WS"))), Seq(133, 136, 4)),
            // [ParamName ([WS ':' WS TypeDesc])? WS '=' WS PExpr],
            134 -> NGrammar.NSequence(134, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))), Symbols.Nonterminal("WS"), Symbols.ExactChar('='), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"))), Seq(135, 40, 4, 73, 4, 98)),
            // [([WS ',' WS NamedParam])* ([WS ',' WS NamedParam])],
            137 -> NGrammar.NSequence(137, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))), 0), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam")))))), Seq(136, 138)),
            // [WS ',' WS NamedParam],
            139 -> NGrammar.NSequence(139, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("NamedParam"))), Seq(4, 64, 4, 133)),
            // ['(' WS PExpr WS ')'],
            140 -> NGrammar.NSequence(140, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr"), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(116, 4, 98, 4, 124)),
            // [ArrayTerm],
            141 -> NGrammar.NSequence(141, Symbols.Sequence(Seq(Symbols.Nonterminal("ArrayTerm"))), Seq(142)),
            // ['[' WS ([PExpr ([WS ',' WS PExpr])* WS])? ']'],
            143 -> NGrammar.NSequence(143, Symbols.Sequence(Seq(Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("PExpr"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PExpr")))), 0), Symbols.Nonterminal("WS")))))), Symbols.ExactChar(']'))), Seq(67, 4, 117, 68)),
            // [PTerm],
            144 -> NGrammar.NSequence(144, Symbols.Sequence(Seq(Symbols.Nonterminal("PTerm"))), Seq(106)),
            // [Symbol],
            146 -> NGrammar.NSequence(146, Symbols.Sequence(Seq(Symbols.Nonterminal("Symbol"))), Seq(147)),
            // [BinSymbol],
            148 -> NGrammar.NSequence(148, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"))), Seq(149)),
            // [BinSymbol WS '&' WS PreUnSymbol],
            150 -> NGrammar.NSequence(150, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('&'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(149, 4, 151, 4, 152)),
            // ['^' WS PreUnSymbol],
            153 -> NGrammar.NSequence(153, Symbols.Sequence(Seq(Symbols.ExactChar('^'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(154, 4, 152)),
            // ['!' WS PreUnSymbol],
            155 -> NGrammar.NSequence(155, Symbols.Sequence(Seq(Symbols.ExactChar('!'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(156, 4, 152)),
            // [PostUnSymbol],
            157 -> NGrammar.NSequence(157, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"))), Seq(158)),
            // [PostUnSymbol WS '?'|'*'|'+'],
            159 -> NGrammar.NSequence(159, Symbols.Sequence(Seq(Symbols.Nonterminal("PostUnSymbol"), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.ExactChar('?'), Symbols.ExactChar('*'), Symbols.ExactChar('+'))))), Seq(158, 4, 160)),
            // [AtomSymbol],
            162 -> NGrammar.NSequence(162, Symbols.Sequence(Seq(Symbols.Nonterminal("AtomSymbol"))), Seq(163)),
            // [Terminal],
            164 -> NGrammar.NSequence(164, Symbols.Sequence(Seq(Symbols.Nonterminal("Terminal"))), Seq(165)),
            // [''' TerminalChar '''],
            166 -> NGrammar.NSequence(166, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChar"), Symbols.ExactChar('\''))), Seq(167, 168, 167)),
            // [<any>-'\'],
            169 -> NGrammar.NSequence(169, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.ExactChar('\\')))), Seq(170)),
            // ['\' {'\bnrt}],
            172 -> NGrammar.NSequence(172, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('\'', '\\', 'b', 'n', 'r', 't')))), Seq(171, 173)),
            // [UnicodeChar],
            174 -> NGrammar.NSequence(174, Symbols.Sequence(Seq(Symbols.Nonterminal("UnicodeChar"))), Seq(175)),
            // ['\' 'u' {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f}],
            176 -> NGrammar.NSequence(176, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.ExactChar('u'), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet), Symbols.Chars(('0' to '9').toSet ++ ('A' to 'F').toSet ++ ('a' to 'f').toSet))), Seq(171, 177, 178, 178, 178, 178)),
            // ['.'],
            179 -> NGrammar.NSequence(179, Symbols.Sequence(Seq(Symbols.ExactChar('.'))), Seq(180)),
            // [TerminalChoice],
            181 -> NGrammar.NSequence(181, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoice"))), Seq(182)),
            // [''' TerminalChoiceElem TerminalChoiceElem+ '''],
            183 -> NGrammar.NSequence(183, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChoiceElem"), Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), Symbols.ExactChar('\''))), Seq(167, 184, 196, 167)),
            // [TerminalChoiceChar],
            185 -> NGrammar.NSequence(185, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"))), Seq(186)),
            // [<any>-{'-\}],
            187 -> NGrammar.NSequence(187, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'', '-', '\\'))))), Seq(188)),
            // ['\' {'-\bnrt}],
            190 -> NGrammar.NSequence(190, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('\'', '-', '\\', 'b', 'n', 'r', 't')))), Seq(171, 191)),
            // [TerminalChoiceRange],
            192 -> NGrammar.NSequence(192, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceRange"))), Seq(193)),
            // [TerminalChoiceChar '-' TerminalChoiceChar],
            194 -> NGrammar.NSequence(194, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"), Symbols.ExactChar('-'), Symbols.Nonterminal("TerminalChoiceChar"))), Seq(186, 195, 186)),
            // [TerminalChoiceElem+ TerminalChoiceElem],
            197 -> NGrammar.NSequence(197, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), Symbols.Nonterminal("TerminalChoiceElem"))), Seq(196, 184)),
            // [''' TerminalChoiceRange '''],
            198 -> NGrammar.NSequence(198, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChoiceRange"), Symbols.ExactChar('\''))), Seq(167, 193, 167)),
            // [StringLiteral],
            199 -> NGrammar.NSequence(199, Symbols.Sequence(Seq(Symbols.Nonterminal("StringLiteral"))), Seq(200)),
            // ['"' StringChar* '"'],
            201 -> NGrammar.NSequence(201, Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), Symbols.ExactChar('"'))), Seq(202, 203, 202)),
            // [StringChar* StringChar],
            204 -> NGrammar.NSequence(204, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), Symbols.Nonterminal("StringChar"))), Seq(203, 205)),
            // [<any>-{"\}],
            206 -> NGrammar.NSequence(206, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"', '\\'))))), Seq(207)),
            // ['\' {"\bnrt}],
            209 -> NGrammar.NSequence(209, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('"', '\\', 'b', 'n', 'r', 't')))), Seq(171, 210)),
            // [Nonterminal],
            211 -> NGrammar.NSequence(211, Symbols.Sequence(Seq(Symbols.Nonterminal("Nonterminal"))), Seq(29)),
            // ['(' InPlaceChoices ')'],
            212 -> NGrammar.NSequence(212, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("InPlaceChoices"), Symbols.ExactChar(')'))), Seq(116, 213, 124)),
            // [InPlaceSequence ([WS '|' WS InPlaceSequence])*],
            214 -> NGrammar.NSequence(214, Symbols.Sequence(Seq(Symbols.Nonterminal("InPlaceSequence"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceSequence")))), 0))), Seq(215, 221)),
            // [Symbol ([WS Symbol])*],
            216 -> NGrammar.NSequence(216, Symbols.Sequence(Seq(Symbols.Nonterminal("Symbol"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Symbol")))), 0))), Seq(147, 217)),
            // [([WS Symbol])* ([WS Symbol])],
            218 -> NGrammar.NSequence(218, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Symbol")))), 0), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Symbol")))))), Seq(217, 219)),
            // [WS Symbol],
            220 -> NGrammar.NSequence(220, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Symbol"))), Seq(4, 147)),
            // [([WS '|' WS InPlaceSequence])* ([WS '|' WS InPlaceSequence])],
            222 -> NGrammar.NSequence(222, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceSequence")))), 0), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceSequence")))))), Seq(221, 223)),
            // [WS '|' WS InPlaceSequence],
            224 -> NGrammar.NSequence(224, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("InPlaceSequence"))), Seq(4, 225, 4, 215)),
            // [Longest],
            226 -> NGrammar.NSequence(226, Symbols.Sequence(Seq(Symbols.Nonterminal("Longest"))), Seq(227)),
            // ['<' InPlaceChoices '>'],
            228 -> NGrammar.NSequence(228, Symbols.Sequence(Seq(Symbols.ExactChar('<'), Symbols.Nonterminal("InPlaceChoices"), Symbols.ExactChar('>'))), Seq(59, 213, 65)),
            // [EmptySequence],
            229 -> NGrammar.NSequence(229, Symbols.Sequence(Seq(Symbols.Nonterminal("EmptySequence"))), Seq(230)),
            // ['#'],
            231 -> NGrammar.NSequence(231, Symbols.Sequence(Seq(Symbols.ExactChar('#'))), Seq(232)),
            // [BinSymbol WS '-' WS PreUnSymbol],
            233 -> NGrammar.NSequence(233, Symbols.Sequence(Seq(Symbols.Nonterminal("BinSymbol"), Symbols.Nonterminal("WS"), Symbols.ExactChar('-'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("PreUnSymbol"))), Seq(149, 4, 195, 4, 152)),
            // [PreUnSymbol],
            234 -> NGrammar.NSequence(234, Symbols.Sequence(Seq(Symbols.Nonterminal("PreUnSymbol"))), Seq(152)),
            // [([WS Elem])* ([WS Elem])],
            236 -> NGrammar.NSequence(236, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))), 0), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem")))))), Seq(235, 237)),
            // [WS Elem],
            238 -> NGrammar.NSequence(238, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Elem"))), Seq(4, 78)),
            // [([WS '|' WS RHS])* ([WS '|' WS RHS])],
            240 -> NGrammar.NSequence(240, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))), 0), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS")))))), Seq(239, 241)),
            // [WS '|' WS RHS],
            242 -> NGrammar.NSequence(242, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar('|'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("RHS"))), Seq(4, 225, 4, 76)),
            // [TypeDef],
            243 -> NGrammar.NSequence(243, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeDef"))), Seq(244)),
            // ['@' ClassDef],
            245 -> NGrammar.NSequence(245, Symbols.Sequence(Seq(Symbols.ExactChar('@'), Symbols.Nonterminal("ClassDef"))), Seq(53, 246)),
            // [TypeName WS '(' WS ([ClassParams WS])? ')'],
            247 -> NGrammar.NSequence(247, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParams"), Symbols.Nonterminal("WS")))))), Symbols.ExactChar(')'))), Seq(49, 4, 116, 4, 248, 124)),
            // [ClassParams WS],
            250 -> NGrammar.NSequence(250, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParams"), Symbols.Nonterminal("WS"))), Seq(251, 4)),
            // [ClassParam ([WS ',' WS ClassParam])*],
            252 -> NGrammar.NSequence(252, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassParam"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParam")))), 0))), Seq(253, 255)),
            // [ParamName ([WS ':' WS TypeDesc])?],
            254 -> NGrammar.NSequence(254, Symbols.Sequence(Seq(Symbols.Nonterminal("ParamName"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("TypeDesc")))))))), Seq(135, 40)),
            // [([WS ',' WS ClassParam])* ([WS ',' WS ClassParam])],
            256 -> NGrammar.NSequence(256, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParam")))), 0), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParam")))))), Seq(255, 257)),
            // [WS ',' WS ClassParam],
            258 -> NGrammar.NSequence(258, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ClassParam"))), Seq(4, 64, 4, 253)),
            // ['@' SuperDef],
            259 -> NGrammar.NSequence(259, Symbols.Sequence(Seq(Symbols.ExactChar('@'), Symbols.Nonterminal("SuperDef"))), Seq(53, 260)),
            // [TypeName WS '{' WS ([SubTypes WS])? '}'],
            261 -> NGrammar.NSequence(261, Symbols.Sequence(Seq(Symbols.Nonterminal("TypeName"), Symbols.Nonterminal("WS"), Symbols.ExactChar('{'), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq())), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"), Symbols.Nonterminal("WS")))))), Symbols.ExactChar('}'))), Seq(49, 4, 97, 4, 262, 145)),
            // [SubTypes WS],
            264 -> NGrammar.NSequence(264, Symbols.Sequence(Seq(Symbols.Nonterminal("SubTypes"), Symbols.Nonterminal("WS"))), Seq(265, 4)),
            // [SubType ([WS ',' WS SubType])*],
            266 -> NGrammar.NSequence(266, Symbols.Sequence(Seq(Symbols.Nonterminal("SubType"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))), 0))), Seq(267, 270)),
            // [ClassDef],
            268 -> NGrammar.NSequence(268, Symbols.Sequence(Seq(Symbols.Nonterminal("ClassDef"))), Seq(246)),
            // [SuperDef],
            269 -> NGrammar.NSequence(269, Symbols.Sequence(Seq(Symbols.Nonterminal("SuperDef"))), Seq(260)),
            // [([WS ',' WS SubType])* ([WS ',' WS SubType])],
            271 -> NGrammar.NSequence(271, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))), 0), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType")))))), Seq(270, 272)),
            // [WS ',' WS SubType],
            273 -> NGrammar.NSequence(273, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("SubType"))), Seq(4, 64, 4, 267)),
            // [([WS Def])* ([WS Def])],
            275 -> NGrammar.NSequence(275, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Def")))), 0), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Def")))))), Seq(274, 276)),
            // [WS Def],
            277 -> NGrammar.NSequence(277, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.Nonterminal("Def"))), Seq(4, 23))),
        1)

    sealed trait ASTNode {
        val astNode: Node
    }

    case class Grammar(astNode: Node, defs: List[Def]) extends ASTNode

    sealed trait Def extends ASTNode

    sealed trait TypeDef extends ASTNode with Def

    case class ClassDef(astNode: Node, typeName: TypeName, params: Option[List[ClassParam]]) extends ASTNode with SubType with TypeDef

    case class SuperDef(astNode: Node, typeName: TypeName, subs: Option[List[SubType]]) extends ASTNode with SubType with TypeDef

    case class TypeName(astNode: Node, name: Node) extends ASTNode with SubType with ValueTypeDesc

    case class ClassParam(astNode: Node, name: ParamName, typeDesc: Option[TypeDesc]) extends ASTNode

    case class ParamName(astNode: Node, name: Node) extends ASTNode

    case class TypeDesc(astNode: Node, typ: ValueTypeDesc, optional: Option[Node]) extends ASTNode

    sealed trait ValueTypeDesc extends ASTNode

    case class ArrayTypeDesc(astNode: Node, elemType: TypeDesc) extends ASTNode with ValueTypeDesc

    sealed trait SubType extends ASTNode

    case class OnTheFlyTypeDef(astNode: Node, name: TypeName, supers: Option[List[TypeName]]) extends ASTNode with ValueTypeDesc

    case class Rule(astNode: Node, lhs: LHS, rhs: List[RHS]) extends ASTNode with Def

    case class LHS(astNode: Node, name: Nonterminal, typeDesc: Option[TypeDesc]) extends ASTNode

    case class RHS(astNode: Node, elems: List[Elem]) extends ASTNode

    sealed trait Elem extends ASTNode

    sealed trait Processor extends ASTNode with Elem

    sealed trait PExpr extends ASTNode with BoundedPExpr with Processor

    case class BinOpExpr(astNode: Node, op: Node, lhs: PExpr, rhs: PTerm) extends ASTNode with PExpr

    sealed trait PTerm extends ASTNode with PExpr

    case class PTermParen(astNode: Node, expr: PExpr) extends ASTNode with PTerm

    case class Ref(astNode: Node, idx: Node) extends ASTNode with PTerm

    case class PTermSeq(astNode: Node, elems: Option[List[PExpr]]) extends ASTNode with PTerm

    case class BoundPExpr(astNode: Node, ctx: Ref, expr: BoundedPExpr) extends ASTNode with PTerm

    sealed trait BoundedPExpr extends ASTNode

    sealed trait AbstractConstructExpr extends ASTNode with PTerm

    case class ConstructExpr(astNode: Node, typeName: TypeName, params: Option[List[PExpr]]) extends ASTNode with AbstractConstructExpr

    case class OnTheFlyTypeDefConstructExpr(astNode: Node, typeDef: OnTheFlyTypeDef, params: Option[List[NamedParam]]) extends ASTNode with AbstractConstructExpr

    case class NamedParam(astNode: Node, name: ParamName, typeDesc: Option[TypeDesc], expr: PExpr) extends ASTNode

    sealed trait Symbol extends ASTNode with Elem

    sealed trait BinSymbol extends ASTNode with Symbol

    case class JoinSymbol(astNode: Node, symbol1: BinSymbol, symbol2: PreUnSymbol) extends ASTNode with BinSymbol

    case class ExceptSymbol(astNode: Node, symbol1: BinSymbol, symbol2: PreUnSymbol) extends ASTNode with BinSymbol

    sealed trait PreUnSymbol extends ASTNode with BinSymbol

    case class FollowedBy(astNode: Node, expr: PreUnSymbol) extends ASTNode with PreUnSymbol

    case class NotFollowedBy(astNode: Node, expr: PreUnSymbol) extends ASTNode with PreUnSymbol

    sealed trait PostUnSymbol extends ASTNode with PreUnSymbol

    case class Repeat(astNode: Node, expr: PostUnSymbol, repeat: Node) extends ASTNode with PostUnSymbol

    sealed trait AtomSymbol extends ASTNode with PostUnSymbol

    case class Paren(astNode: Node, choices: InPlaceChoices) extends ASTNode with AtomSymbol

    case class EmptySeq(astNode: Node) extends ASTNode with AtomSymbol

    case class InPlaceChoices(astNode: Node, choices: List[InPlaceSequence]) extends ASTNode

    case class InPlaceSequence(astNode: Node, seq: List[Symbol]) extends ASTNode

    case class Longest(astNode: Node, choices: InPlaceChoices) extends ASTNode with AtomSymbol

    case class Nonterminal(astNode: Node, name: Node) extends ASTNode with AtomSymbol

    sealed trait Terminal extends ASTNode with AtomSymbol

    case class AnyTerminal(astNode: Node, c: Node) extends ASTNode with Terminal

    case class TerminalChoice(astNode: Node, choices: List[TerminalChoiceElem]) extends ASTNode with AtomSymbol

    sealed trait TerminalChoiceElem extends ASTNode

    case class TerminalChoiceRange(astNode: Node, start: TerminalChoiceChar, end: TerminalChoiceChar) extends ASTNode with TerminalChoiceElem

    case class StringLiteral(astNode: Node, value: List[StringChar]) extends ASTNode with AtomSymbol

    case class CharUnicode(astNode: Node, code: List[Node]) extends ASTNode with StringChar with TerminalChar with TerminalChoiceChar

    sealed trait TerminalChar extends ASTNode with Terminal

    case class CharAsIs(astNode: Node, c: Node) extends ASTNode with StringChar with TerminalChar with TerminalChoiceChar

    case class CharEscaped(astNode: Node, escapeCode: Node) extends ASTNode with StringChar with TerminalChar with TerminalChoiceChar

    sealed trait TerminalChoiceChar extends ASTNode with TerminalChoiceElem

    sealed trait StringChar extends ASTNode

    def sourceTextOf(node: Node): String = node match {
        case TerminalNode(input) => input.toRawString
        case BindNode(_, body) => sourceTextOf(body)
        case JoinNode(body, _) => sourceTextOf(body)
        case seq: SequenceNode => seq.children map sourceTextOf mkString ""
        case _ => throw new Exception("Cyclic bind")
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
                val v18 = Grammar(node, v17)
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
                val v50 = ClassDef(node, v38, v49)
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
                val v66 = SuperDef(node, v54, v65)
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
                val v71 = TypeName(node, v70)
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
                val v104 = ClassParam(node, v92, v103)
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
                val v109 = ParamName(node, v108)
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
                val v120 = TypeDesc(node, v113, v119)
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
                val v133 = ArrayTypeDesc(node, v132)
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
                val v178 = OnTheFlyTypeDef(node, v166, v177)
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
                val v204 = Rule(node, v199, v203)
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
                val v220 = LHS(node, v208, v219)
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
                val v255 = RHS(node, v254)
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
                val BindNode(v273, v274) = v272
                assert(v273.id == 100)
                val BindNode(v275, v276) = v274
                assert(v275.id == 101)
                val v277 = matchBinOp(v276)
                val v278 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v279, v280) = v278
                assert(v279.id == 98)
                val v281 = matchPExpr(v280)
                val v282 = body.asInstanceOf[SequenceNode].children(4)
                val BindNode(v283, v284) = v282
                assert(v283.id == 106)
                val v285 = matchPTerm(v284)
                val v286 = BinOpExpr(node, v277, v281, v285)
                v286
            case 144 =>
                val v287 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v288, v289) = v287
                assert(v288.id == 106)
                val v290 = matchPTerm(v289)
                v290
        }
    }

    def matchBinOp(node: Node): Node = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 102 =>
                val v291 = body.asInstanceOf[SequenceNode].children(0)
                v291
        }
    }

    def matchPTerm(node: Node): PTerm = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 81 =>
                val v292 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v293, v294) = v292
                assert(v293.id == 82)
                val v295 = matchRef(v294)
                v295
            case 107 =>
                val v296 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v297, v298) = v296
                assert(v297.id == 108)
                val v299 = matchBoundPExpr(v298)
                v299
            case 111 =>
                val v300 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v301, v302) = v300
                assert(v301.id == 112)
                val v303 = matchConstructExpr(v302)
                v303
            case 140 =>
                val v304 = body.asInstanceOf[SequenceNode].children(2)
                val BindNode(v305, v306) = v304
                assert(v305.id == 98)
                val v307 = matchPExpr(v306)
                val v308 = PTermParen(node, v307)
                v308
            case 141 =>
                val v309 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v310, v311) = v309
                assert(v310.id == 142)
                val v312 = matchArrayTerm(v311)
                v312
        }
    }

    def matchRef(node: Node): Ref = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 83 =>
                val v313 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v314, v315) = v313
                assert(v314.id == 85)
                val v316 = matchRefIdx(v315)
                val v317 = Ref(node, v316)
                v317
        }
    }

    def matchArrayTerm(node: Node): PTermSeq = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 143 =>
                val v318 = body.asInstanceOf[SequenceNode].children(2)
                val v323 = unrollOptional(v318, 41, 118) map { n =>
                    val BindNode(v319, v320) = n
                    assert(v319.id == 118)
                    val BindNode(v321, v322) = v320
                    assert(v321.id == 119)
                    v322
                }
                val v341 = v323 map { n =>
                    val v324 = n.asInstanceOf[SequenceNode].children(0)
                    val BindNode(v325, v326) = v324
                    assert(v325.id == 98)
                    val v327 = matchPExpr(v326)
                    val v328 = List(v327)
                    val v329 = n.asInstanceOf[SequenceNode].children(1)
                    val v334 = unrollRepeat0(v329) map { n =>
                        val BindNode(v330, v331) = n
                        assert(v330.id == 122)
                        val BindNode(v332, v333) = v331
                        assert(v332.id == 123)
                        v333
                    }
                    val v339 = v334 map { n =>
                        val v335 = n.asInstanceOf[SequenceNode].children(3)
                        val BindNode(v336, v337) = v335
                        assert(v336.id == 98)
                        val v338 = matchPExpr(v337)
                        v338
                    }
                    val v340 = v328 ++ v339
                    v340
                }
                val v342 = PTermSeq(node, v341)
                v342
        }
    }

    def matchBoundPExpr(node: Node): BoundPExpr = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 109 =>
                val v343 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v344, v345) = v343
                assert(v344.id == 82)
                val v346 = matchRef(v345)
                val v347 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v348, v349) = v347
                assert(v348.id == 110)
                val v350 = matchBoundedPExpr(v349)
                val v351 = BoundPExpr(node, v346, v350)
                v351
        }
    }

    def matchBoundedPExpr(node: Node): PExpr = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 81 =>
                val v352 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v353, v354) = v352
                assert(v353.id == 82)
                val v355 = matchRef(v354)
                v355
            case 107 =>
                val v356 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v357, v358) = v356
                assert(v357.id == 108)
                val v359 = matchBoundPExpr(v358)
                v359
            case 96 =>
                val v360 = body.asInstanceOf[SequenceNode].children(2)
                val BindNode(v361, v362) = v360
                assert(v361.id == 98)
                val v363 = matchPExpr(v362)
                v363
        }
    }

    def matchConstructExpr(node: Node): AbstractConstructExpr = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 113 =>
                val v364 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v365, v366) = v364
                assert(v365.id == 49)
                val v367 = matchTypeName(v366)
                val v368 = body.asInstanceOf[SequenceNode].children(2)
                val BindNode(v369, v370) = v368
                assert(v369.id == 114)
                val v371 = matchConstructParams(v370)
                val v372 = ConstructExpr(node, v367, v371)
                v372
            case 125 =>
                val v373 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v374, v375) = v373
                assert(v374.id == 126)
                val v376 = matchOnTheFlyTypeDefConstructExpr(v375)
                v376
        }
    }

    def matchConstructParams(node: Node): Option[List[PExpr]] = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 115 =>
                val v377 = body.asInstanceOf[SequenceNode].children(2)
                val v382 = unrollOptional(v377, 41, 118) map { n =>
                    val BindNode(v378, v379) = n
                    assert(v378.id == 118)
                    val BindNode(v380, v381) = v379
                    assert(v380.id == 119)
                    v381
                }
                val v400 = v382 map { n =>
                    val v383 = n.asInstanceOf[SequenceNode].children(0)
                    val BindNode(v384, v385) = v383
                    assert(v384.id == 98)
                    val v386 = matchPExpr(v385)
                    val v387 = List(v386)
                    val v388 = n.asInstanceOf[SequenceNode].children(1)
                    val v393 = unrollRepeat0(v388) map { n =>
                        val BindNode(v389, v390) = n
                        assert(v389.id == 122)
                        val BindNode(v391, v392) = v390
                        assert(v391.id == 123)
                        v392
                    }
                    val v398 = v393 map { n =>
                        val v394 = n.asInstanceOf[SequenceNode].children(3)
                        val BindNode(v395, v396) = v394
                        assert(v395.id == 98)
                        val v397 = matchPExpr(v396)
                        v397
                    }
                    val v399 = v387 ++ v398
                    v399
                }
                v400
        }
    }

    def matchOnTheFlyTypeDefConstructExpr(node: Node): OnTheFlyTypeDefConstructExpr = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 127 =>
                val v401 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v402, v403) = v401
                assert(v402.id == 51)
                val v404 = matchOnTheFlyTypeDef(v403)
                val v405 = body.asInstanceOf[SequenceNode].children(2)
                val BindNode(v406, v407) = v405
                assert(v406.id == 128)
                val v408 = matchNamedParams(v407)
                val v409 = OnTheFlyTypeDefConstructExpr(node, v404, v408)
                v409
        }
    }

    def matchNamedParams(node: Node): Option[List[NamedParam]] = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 129 =>
                val v410 = body.asInstanceOf[SequenceNode].children(2)
                val v415 = unrollOptional(v410, 41, 131) map { n =>
                    val BindNode(v411, v412) = n
                    assert(v411.id == 131)
                    val BindNode(v413, v414) = v412
                    assert(v413.id == 132)
                    v414
                }
                val v433 = v415 map { n =>
                    val v416 = n.asInstanceOf[SequenceNode].children(0)
                    val BindNode(v417, v418) = v416
                    assert(v417.id == 133)
                    val v419 = matchNamedParam(v418)
                    val v420 = List(v419)
                    val v421 = n.asInstanceOf[SequenceNode].children(1)
                    val v426 = unrollRepeat0(v421) map { n =>
                        val BindNode(v422, v423) = n
                        assert(v422.id == 138)
                        val BindNode(v424, v425) = v423
                        assert(v424.id == 139)
                        v425
                    }
                    val v431 = v426 map { n =>
                        val v427 = n.asInstanceOf[SequenceNode].children(3)
                        val BindNode(v428, v429) = v427
                        assert(v428.id == 133)
                        val v430 = matchNamedParam(v429)
                        v430
                    }
                    val v432 = v420 ++ v431
                    v432
                }
                v433
        }
    }

    def matchNamedParam(node: Node): NamedParam = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 134 =>
                val v434 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v435, v436) = v434
                assert(v435.id == 135)
                val v437 = matchParamName(v436)
                val v438 = body.asInstanceOf[SequenceNode].children(1)
                val v443 = unrollOptional(v438, 41, 42) map { n =>
                    val BindNode(v439, v440) = n
                    assert(v439.id == 42)
                    val BindNode(v441, v442) = v440
                    assert(v441.id == 43)
                    v442
                }
                val v448 = v443 map { n =>
                    val v444 = n.asInstanceOf[SequenceNode].children(3)
                    val BindNode(v445, v446) = v444
                    assert(v445.id == 45)
                    val v447 = matchTypeDesc(v446)
                    v447
                }
                val v449 = body.asInstanceOf[SequenceNode].children(5)
                val BindNode(v450, v451) = v449
                assert(v450.id == 98)
                val v452 = matchPExpr(v451)
                val v453 = NamedParam(node, v437, v448, v452)
                v453
        }
    }

    def matchSymbol(node: Node): Symbol = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 148 =>
                val v454 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v455, v456) = v454
                assert(v455.id == 149)
                val v457 = matchBinSymbol(v456)
                v457
        }
    }

    def matchBinSymbol(node: Node): BinSymbol = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 150 =>
                val v458 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v459, v460) = v458
                assert(v459.id == 149)
                val v461 = matchBinSymbol(v460)
                val v462 = body.asInstanceOf[SequenceNode].children(4)
                val BindNode(v463, v464) = v462
                assert(v463.id == 152)
                val v465 = matchPreUnSymbol(v464)
                val v466 = JoinSymbol(node, v461, v465)
                v466
            case 233 =>
                val v467 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v468, v469) = v467
                assert(v468.id == 149)
                val v470 = matchBinSymbol(v469)
                val v471 = body.asInstanceOf[SequenceNode].children(4)
                val BindNode(v472, v473) = v471
                assert(v472.id == 152)
                val v474 = matchPreUnSymbol(v473)
                val v475 = ExceptSymbol(node, v470, v474)
                v475
            case 234 =>
                val v476 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v477, v478) = v476
                assert(v477.id == 152)
                val v479 = matchPreUnSymbol(v478)
                v479
        }
    }

    def matchPreUnSymbol(node: Node): PreUnSymbol = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 153 =>
                val v480 = body.asInstanceOf[SequenceNode].children(2)
                val BindNode(v481, v482) = v480
                assert(v481.id == 152)
                val v483 = matchPreUnSymbol(v482)
                val v484 = FollowedBy(node, v483)
                v484
            case 155 =>
                val v485 = body.asInstanceOf[SequenceNode].children(2)
                val BindNode(v486, v487) = v485
                assert(v486.id == 152)
                val v488 = matchPreUnSymbol(v487)
                val v489 = NotFollowedBy(node, v488)
                v489
            case 157 =>
                val v490 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v491, v492) = v490
                assert(v491.id == 158)
                val v493 = matchPostUnSymbol(v492)
                v493
        }
    }

    def matchPostUnSymbol(node: Node): PostUnSymbol = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 159 =>
                val v494 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v495, v496) = v494
                assert(v495.id == 158)
                val v497 = matchPostUnSymbol(v496)
                // UnrollChoices
                val v498 = Repeat(node, v497, body)
                v498
            case 162 =>
                val v499 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v500, v501) = v499
                assert(v500.id == 163)
                val v502 = matchAtomSymbol(v501)
                v502
        }
    }

    def matchAtomSymbol(node: Node): AtomSymbol = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 164 =>
                val v503 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v504, v505) = v503
                assert(v504.id == 165)
                val v506 = matchTerminal(v505)
                v506
            case 181 =>
                val v507 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v508, v509) = v507
                assert(v508.id == 182)
                val v510 = matchTerminalChoice(v509)
                v510
            case 199 =>
                val v511 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v512, v513) = v511
                assert(v512.id == 200)
                val v514 = matchStringLiteral(v513)
                v514
            case 211 =>
                val v515 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v516, v517) = v515
                assert(v516.id == 29)
                val v518 = matchNonterminal(v517)
                v518
            case 212 =>
                val v519 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v520, v521) = v519
                assert(v520.id == 213)
                val v522 = matchInPlaceChoices(v521)
                val v523 = Paren(node, v522)
                v523
            case 226 =>
                val v524 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v525, v526) = v524
                assert(v525.id == 227)
                val v527 = matchLongest(v526)
                v527
            case 229 =>
                val v528 = EmptySeq(node)
                v528
        }
    }

    def matchInPlaceChoices(node: Node): InPlaceChoices = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 214 =>
                val v529 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v530, v531) = v529
                assert(v530.id == 215)
                val v532 = matchInPlaceSequence(v531)
                val v533 = List(v532)
                val v534 = body.asInstanceOf[SequenceNode].children(1)
                val v539 = unrollRepeat0(v534) map { n =>
                    val BindNode(v535, v536) = n
                    assert(v535.id == 223)
                    val BindNode(v537, v538) = v536
                    assert(v537.id == 224)
                    v538
                }
                val v544 = v539 map { n =>
                    val v540 = n.asInstanceOf[SequenceNode].children(3)
                    val BindNode(v541, v542) = v540
                    assert(v541.id == 215)
                    val v543 = matchInPlaceSequence(v542)
                    v543
                }
                val v545 = v533 ++ v544
                val v546 = InPlaceChoices(node, v545)
                v546
        }
    }

    def matchInPlaceSequence(node: Node): InPlaceSequence = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 216 =>
                val v547 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v548, v549) = v547
                assert(v548.id == 147)
                val v550 = matchSymbol(v549)
                val v551 = List(v550)
                val v552 = body.asInstanceOf[SequenceNode].children(1)
                val v557 = unrollRepeat0(v552) map { n =>
                    val BindNode(v553, v554) = n
                    assert(v553.id == 219)
                    val BindNode(v555, v556) = v554
                    assert(v555.id == 220)
                    v556
                }
                val v562 = v557 map { n =>
                    val v558 = n.asInstanceOf[SequenceNode].children(1)
                    val BindNode(v559, v560) = v558
                    assert(v559.id == 147)
                    val v561 = matchSymbol(v560)
                    v561
                }
                val v563 = v551 ++ v562
                val v564 = InPlaceSequence(node, v563)
                v564
        }
    }

    def matchLongest(node: Node): Longest = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 228 =>
                val v565 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v566, v567) = v565
                assert(v566.id == 213)
                val v568 = matchInPlaceChoices(v567)
                val v569 = Longest(node, v568)
                v569
        }
    }

    def matchEmptySequence(node: Node): Node = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 231 =>
                val v570 = body.asInstanceOf[SequenceNode].children(0)
                v570
        }
    }

    def matchNonterminal(node: Node): Nonterminal = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 30 =>
                val v571 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v572, v573) = v571
                assert(v572.id == 31)
                val v574 = matchId(v573)
                val v575 = Nonterminal(node, v574)
                v575
        }
    }

    def matchTerminal(node: Node): Terminal = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 166 =>
                val v576 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v577, v578) = v576
                assert(v577.id == 168)
                val v579 = matchTerminalChar(v578)
                v579
            case 179 =>
                val v580 = body.asInstanceOf[SequenceNode].children(0)
                val v581 = AnyTerminal(node, v580)
                v581
        }
    }

    def matchTerminalChoice(node: Node): TerminalChoice = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 183 =>
                val v582 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v583, v584) = v582
                assert(v583.id == 184)
                val v585 = matchTerminalChoiceElem(v584)
                val v586 = List(v585)
                val v587 = body.asInstanceOf[SequenceNode].children(2)
                val v591 = unrollRepeat1(v587) map { n =>
                    val BindNode(v588, v589) = n
                    assert(v588.id == 184)
                    val v590 = matchTerminalChoiceElem(v589)
                    v590
                }
                val v592 = v591 map { n =>
                    n
                }
                val v593 = v586 ++ v592
                val v594 = TerminalChoice(node, v593)
                v594
            case 198 =>
                val v595 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v596, v597) = v595
                assert(v596.id == 193)
                val v598 = matchTerminalChoiceRange(v597)
                val v599 = List(v598)
                val v600 = TerminalChoice(node, v599)
                v600
        }
    }

    def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 185 =>
                val v601 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v602, v603) = v601
                assert(v602.id == 186)
                val v604 = matchTerminalChoiceChar(v603)
                v604
            case 192 =>
                val v605 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v606, v607) = v605
                assert(v606.id == 193)
                val v608 = matchTerminalChoiceRange(v607)
                v608
        }
    }

    def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 194 =>
                val v609 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v610, v611) = v609
                assert(v610.id == 186)
                val v612 = matchTerminalChoiceChar(v611)
                val v613 = body.asInstanceOf[SequenceNode].children(2)
                val BindNode(v614, v615) = v613
                assert(v614.id == 186)
                val v616 = matchTerminalChoiceChar(v615)
                val v617 = TerminalChoiceRange(node, v612, v616)
                v617
        }
    }

    def matchStringLiteral(node: Node): StringLiteral = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 201 =>
                val v618 = body.asInstanceOf[SequenceNode].children(1)
                val v622 = unrollRepeat0(v618) map { n =>
                    val BindNode(v619, v620) = n
                    assert(v619.id == 205)
                    val v621 = matchStringChar(v620)
                    v621
                }
                val v623 = v622 map { n =>
                    n
                }
                val v624 = StringLiteral(node, v623)
                v624
        }
    }

    def matchUnicodeChar(node: Node): CharUnicode = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 176 =>
                val v625 = body.asInstanceOf[SequenceNode].children(2)
                val v626 = body.asInstanceOf[SequenceNode].children(3)
                val v627 = body.asInstanceOf[SequenceNode].children(4)
                val v628 = body.asInstanceOf[SequenceNode].children(5)
                val v629 = List(v625, v626, v627, v628)
                val v630 = CharUnicode(node, v629)
                v630
        }
    }

    def matchTerminalChar(node: Node): TerminalChar = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 169 =>
                val v631 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v632, v633) = v631
                assert(v632.id == 170)
                val v634 = CharAsIs(node, v633)
                v634
            case 172 =>
                val v635 = body.asInstanceOf[SequenceNode].children(1)
                val v636 = CharEscaped(node, v635)
                v636
            case 174 =>
                val v637 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v638, v639) = v637
                assert(v638.id == 175)
                val v640 = matchUnicodeChar(v639)
                v640
        }
    }

    def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 187 =>
                val v641 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v642, v643) = v641
                assert(v642.id == 188)
                val v644 = CharAsIs(node, v643)
                v644
            case 190 =>
                val v645 = body.asInstanceOf[SequenceNode].children(1)
                val v646 = CharEscaped(node, v645)
                v646
            case 174 =>
                val v647 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v648, v649) = v647
                assert(v648.id == 175)
                val v650 = matchUnicodeChar(v649)
                v650
        }
    }

    def matchStringChar(node: Node): StringChar = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 206 =>
                val v651 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v652, v653) = v651
                assert(v652.id == 207)
                val v654 = CharAsIs(node, v653)
                v654
            case 209 =>
                val v655 = body.asInstanceOf[SequenceNode].children(1)
                val v656 = CharEscaped(node, v655)
                v656
            case 174 =>
                val v657 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v658, v659) = v657
                assert(v658.id == 175)
                val v660 = matchUnicodeChar(v659)
                v660
        }
    }

    def matchRefIdx(node: Node): Node = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 86 =>
                // UnrollChoices
                body
        }
    }

    def matchId(node: Node): Node = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 32 =>
                val v661 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v662, v663) = v661
                assert(v662.id == 33)
                val BindNode(v664, v665) = v663
                assert(v664.id == 34)
                val BindNode(v666, v667) = v665
                assert(v666.id == 35)
                v667
        }
    }

    def matchWS(node: Node): List[Node] = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 5 =>
                val v668 = body.asInstanceOf[SequenceNode].children(0)
                val v669 = unrollRepeat0(v668) map { n =>
                    // UnrollChoices
                    n
                }
                v669
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
                val v670 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v671, v672) = v670
                assert(v671.id == 22)
                v672
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
            case SequenceNode(symbol, emptySeq) =>
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

    def main(args: Array[String]): Unit = {
        val example =
            """ABC:XYZ = A B C
              |
              |@XYZ(a: Node, xyz: QQQ)""".stripMargin
        val ast = parseAst(example).swap.getOrElse(throw new Exception())
        println(ast)
        println(sourceTextOf(ast.astNode))
        ast.defs foreach {
            case superDef: SuperDef =>
                println(superDef)
            case classDef: ClassDef =>
                val params = classDef.params.getOrElse(List())
                println(s"${sourceTextOf(classDef.typeName.astNode)} has ${params.size} arguments: ${params map { p => sourceTextOf(p.name.astNode) }}")
            case Rule(astNode, lhs, rhs) =>
                println(s"${sourceTextOf(lhs.name.astNode)} ::= ${rhs map (r => sourceTextOf(r.astNode)) mkString "\n  | "}")
        }
    }
}
