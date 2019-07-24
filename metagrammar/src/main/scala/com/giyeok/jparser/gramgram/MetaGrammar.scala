package com.giyeok.jparser.gramgram

import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.Inputs.CharsGrouping
import com.giyeok.jparser.ParseResultTree._
import com.giyeok.jparser._
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.nparser.NGrammar.{NExcept, NNonterminal, NSequence, NStart}
import com.giyeok.jparser.nparser.{NGrammar, NaiveParser, ParseTreeConstructor}

import scala.collection.immutable.{ListMap, ListSet}

object MetaGrammar extends Grammar {
    val name = "Meta Grammar"
    val rules: RuleMap = ListMap(
        "Grammar" -> ListSet(
            Sequence(Seq(n("ws"), n("Rules"), n("ws")), Seq(1))
        ),
        "Rules" -> ListSet(
            Sequence(Seq(n("Rules"), longest(n("ws0").star), c('\n'), n("ws"), n("Rule")), Seq(0, 4)),
            n("Rule")
        ),
        "Rule" -> ListSet(
            seqWS(n("ws"), n("Nonterminal"), c('='), n("RHSs"))
        ),
        "RHSs" -> ListSet(
            seqWS(n("ws"), n("RHSs"), c('|'), n("Sequence")),
            n("Sequence")
        ),
        "EmptySequence" -> ListSet(
            c('#'),
            c('ε')
        ),
        "Sequence" -> ListSet(
            n("EmptySequence"),
            n("Symbol"),
            n("SymbolSeq")
        ),
        "SymbolSeq" -> ListSet(
            // Symbol 2개 이상
            seqWS(n("ws1").plus, n("SymbolSeq"), n("Symbol")),
            seqWS(n("ws1").plus, n("Symbol"), n("Symbol"))
        ),
        "Symbol" -> ListSet(
            n("Exclusion").except(n("Symbol4")),
            n("Symbol4")
        ),
        "Exclusion" -> ListSet(
            n("Symbol4"),
            seqWS(n("ws"), n("Exclusion"), c('-'), n("Symbol4"))
        ),
        "Symbol4" -> ListSet(
            n("Intersection").except(n("Symbol3")),
            n("Symbol3")
        ),
        "Intersection" -> ListSet(
            n("Symbol3"),
            seqWS(n("ws"), n("Intersection"), c('&'), n("Symbol3"))
        ),
        "Symbol3" -> ListSet(
            n("Repeat0"),
            n("Repeat1"),
            n("Optional"),
            n("Symbol2")
        ),
        "Repeat0" -> ListSet(
            seqWS(n("ws"), n("Symbol3"), c('*'))
        ),
        "Repeat1" -> ListSet(
            seqWS(n("ws"), n("Symbol3"), c('+'))
        ),
        "Optional" -> ListSet(
            seqWS(n("ws"), n("Symbol3"), c('?'))
        ),
        "Symbol2" -> ListSet(
            n("FollowedBy"),
            n("NotFollowedBy"),
            n("Symbol1")
        ),
        "FollowedBy" -> ListSet(
            seqWS(n("ws"), c('$'), n("Symbol2"))
        ),
        "NotFollowedBy" -> ListSet(
            seqWS(n("ws"), c('!'), n("Symbol2"))
        ),
        "Symbol1" -> ListSet(
            n("Terminal"),
            n("String"),
            n("Nonterminal"),
            n("Proxy"),
            n("Longest"),
            seqWS(n("ws"), c('('), n("Symbol"), c(')')),
            seqWS(n("ws"), c('('), n("Either"), c(')'))
        ),
        "Terminal" -> ListSet(
            n("anychar"),
            seq(c('\''), n("char"), c('\'')),
            n("TerminalCharSet")
        ),
        "TerminalCharSet" -> ListSet(
            seq(c('{'), n("TerminalCharRange").plus, c('}'))
        ),
        "TerminalCharRange" -> ListSet(
            n("charSetChar"),
            seq(n("charSetChar"), c('-'), n("charSetChar"))
        ),
        "String" -> ListSet(
            seq(c('\"'), n("stringChar").star, c('\"'))
        ),
        "Nonterminal" -> ListSet(
            n("PlainNonterminalName"),
            n("QuoteNonterminalName")
        ),
        "PlainNonterminalName" -> ListSet(
            c(('a' to 'z').toSet ++ ('A' to 'Z').toSet ++ ('0' to '9').toSet + '_').plus
        ),
        "QuoteNonterminalName" -> ListSet(
            seq(c('`'), n("nontermNameChar").star, c('`'))
        ),
        "Proxy" -> ListSet(
            seqWS(n("ws"), c('['), n("Sequence"), c(']'))
        ),
        "Either" -> ListSet(
            seqWS(n("ws"), n("Symbol"), c('|'), n("Symbol")),
            seqWS(n("ws"), n("Either"), c('|'), n("Symbol"))
        ),
        "Longest" -> ListSet(
            seqWS(n("ws"), c('<'), n("Symbol"), c('>'))
        ),
        "anychar" -> ListSet(
            c('.')
        ),
        "char" -> ListSet(
            anychar.except(c('\\')),
            seq(c('\\'), c("nrbt\"\'\\".toSet)),
            n("unicodeChar")
        ),
        "charSetChar" -> ListSet(
            anychar.except(c("\\}-".toSet)),
            seq(c('\\'), c("}-nrbt\"\'\\".toSet)),
            n("unicodeChar")
        ),
        "stringChar" -> ListSet(
            anychar.except(c("\\\"".toSet)),
            seq(c('\\'), c("nrbt\"\'\\".toSet)),
            n("unicodeChar")
        ),
        "nontermNameChar" -> ListSet(
            anychar.except(c("\\`".toSet)),
            seq(c('\\'), c("nrbt`\\".toSet)),
            n("unicodeChar")
        ),
        "unicodeChar" -> ListSet(
            seq(c('\\'), c('u'), c("0123456789abcdefABCDEF".toSet), c("0123456789abcdefABCDEF".toSet), c("0123456789abcdefABCDEF".toSet), c("0123456789abcdefABCDEF".toSet))
        ),
        "ws0" -> ListSet(
            c("\t\r ".toSet)
        ),
        "ws1" -> ListSet(
            c("\t\n\r ".toSet)
        ),
        "ws" -> ListSet(
            oneof(chars(" \t\n\r"), n("LineComment")).star,
        ),
        "LineComment" -> ListSet(
            // LineComment = "//" .* (!. | '\n')
            seq(c('/'), c('/'), anychar.except(c('\n')).star, oneof(lookahead_except(anychar), c('\n')))
        )
    )
    val startSymbol: Nonterminal = n("Grammar")

    class NewGrammar(val name: String, val rules: ListMap[String, ListSet[Symbols.Symbol]], val startSymbol: Symbols.Nonterminal) extends Grammar

    def childrenOf(node: Node, sym: Symbol): Seq[Node] = node match {
        case BindNode(s, _) if s.symbol == sym => Seq(node)
        case BindNode(_, body) => childrenOf(body, sym)
        case s: SequenceNode => s.children flatMap {
            childrenOf(_, sym)
        }
        case _ => Seq()
    }

    def textOf(node: Node): String = node match {
        case BindNode(s, body) => textOf(body)
        case s: SequenceNode => (s.children map {
            textOf
        }).mkString
        case JoinNode(body, join) => textOf(body)
        case TerminalNode(Inputs.Character(c)) => s"$c"
        case _ => ???
    }

    def charOf(node: Node): Char = {
        node match {
            case BindNode(_, unicodeChar@BindNode(NNonterminal(Nonterminal("unicodeChar"), _), _)) =>
                charOf(unicodeChar)
            case BindNode(NNonterminal(Nonterminal("unicodeChar"), _), BindNode(_: NSequence, unicodeCharBody: SequenceNode)) =>
                val code = Integer.parseInt(textOf(unicodeCharBody).substring(2).toLowerCase(), 16)
                code.toChar
            case BindNode(NNonterminal(Nonterminal(charNontermName), _), charBody) =>
                assert(Set("char", "charSetChar", "stringChar", "nontermNameChar") contains charNontermName)
                charBody match {
                    case BindNode(_: Except, body) => textOf(body).charAt(0)
                    case BindNode(_: Sequence, seq: SequenceNode) =>
                        textOf(seq.childrenAll(1)) match {
                            case "n" => '\n'
                            case "r" => '\r'
                            case "b" => '\b'
                            case "t" => '\t'
                            case "}" => '}'
                            case "-" => '-'
                            case "\"" => '"'
                            case "'" => '\''
                            case "`" => '`'
                            case "\\" => '\\'
                        }
                }
        }
    }

    def sequenceOf(node: Node): Symbols.Symbol = {
        val BindNode(NNonterminal(Nonterminal("Sequence"), _), body) = node
        body match {
            case BindNode(NNonterminal(Nonterminal("EmptySequence"), _), _) =>
                empty
            case BindNode(NNonterminal(Nonterminal("Symbol"), _), _) =>
                symbolOf(body)
            case BindNode(NNonterminal(Nonterminal("SymbolSeq"), _), symbolSeq) =>
                Sequence(childrenOf(symbolSeq, Nonterminal("Symbol")) map { node => proxyIfNeeded(symbolOf(node)) })
        }
    }

    def nonterminalNameOf(node: Node): String = {
        val BindNode(NNonterminal(Nonterminal("Nonterminal"), _), nonterminalNameBody) = node
        nonterminalNameBody match {
            case BindNode(NNonterminal(Nonterminal("PlainNonterminalName"), _), nameBody) =>
                textOf(nameBody)
            case BindNode(NNonterminal(Nonterminal("QuoteNonterminalName"), _), BindNode(_: NSequence, nameBody: SequenceNode)) =>
                (childrenOf(nameBody.childrenAll(1), Nonterminal("nontermNameChar")) map {
                    charOf
                }).mkString
        }
    }

    def symbolOf(node: Node): Symbols.Symbol = {
        node match {
            case BindNode(NNonterminal(Nonterminal("Symbol"), _), symbolBody) =>
                symbolBody match {
                    case BindNode(_: NExcept, exclusionBody@BindNode(NNonterminal(Nonterminal("Exclusion"), _), _)) =>
                        // Exclusion
                        symbolOf(exclusionBody)
                    case BindNode(NNonterminal(Nonterminal("Symbol4"), _), _) =>
                        symbolOf(symbolBody)
                }
            case BindNode(NNonterminal(Nonterminal("Exclusion"), _), exclusionBody) =>
                exclusionBody match {
                    case BindNode(NNonterminal(Nonterminal("Symbol4"), _), _) =>
                        symbolOf(exclusionBody)
                    case BindNode(_: NSequence, exclusionBody: SequenceNode) =>
                        symbolOf(exclusionBody.childrenAll(0)).except(symbolOf(exclusionBody.childrenAll(4)))
                }
            case BindNode(NNonterminal(Nonterminal("Symbol4"), _), symbol4Body) =>
                symbol4Body match {
                    case BindNode(_: NExcept, intersectionBody@BindNode(NNonterminal(Nonterminal("Intersection"), _), _)) =>
                        // Intersection
                        symbolOf(intersectionBody)
                    case BindNode(NNonterminal(Nonterminal("Symbol3"), _), _) =>
                        symbolOf(symbol4Body)
                }
            case BindNode(NNonterminal(Nonterminal("Intersection"), _), intersectionBody) =>
                intersectionBody match {
                    case BindNode(NNonterminal(Nonterminal("Symbol3"), _), symbol3) =>
                        symbolOf(symbol3)
                    case BindNode(_: NSequence, intersectionBody: SequenceNode) =>
                        symbolOf(intersectionBody.childrenAll(0)).join(symbolOf(intersectionBody.childrenAll(4)))
                }
            case BindNode(NNonterminal(Nonterminal("Symbol3"), _), symbol3Body) =>
                symbolOf(symbol3Body)
            case BindNode(NNonterminal(Nonterminal("Repeat0"), _), BindNode(_: NSequence, repeat0Body: SequenceNode)) =>
                symbolOf(repeat0Body.childrenAll.head).star
            case BindNode(NNonterminal(Nonterminal("Repeat1"), _), BindNode(_: NSequence, repeat1Body: SequenceNode)) =>
                symbolOf(repeat1Body.childrenAll.head).plus
            case BindNode(NNonterminal(Nonterminal("Optional"), _), BindNode(_: NSequence, optionalBody: SequenceNode)) =>
                symbolOf(optionalBody.childrenAll.head).opt
            case BindNode(NNonterminal(Nonterminal("Symbol2"), _), symbol2Body) =>
                symbolOf(symbol2Body)
            case BindNode(NNonterminal(Nonterminal("FollowedBy"), _), BindNode(_: NSequence, followedByBody: SequenceNode)) =>
                lookahead_is(symbolOf(followedByBody.childrenAll(2)))
            case BindNode(NNonterminal(Nonterminal("NotFollowedBy"), _), BindNode(_: NSequence, notFollowedByBody: SequenceNode)) =>
                lookahead_except(symbolOf(notFollowedByBody.childrenAll(2)))
            case BindNode(NNonterminal(Nonterminal("Symbol1"), _), symbol1Body) =>
                symbolOf(symbol1Body)
            case BindNode(NNonterminal(Nonterminal("Terminal"), _), terminalBody) =>
                terminalBody match {
                    case BindNode(NNonterminal(Nonterminal("anychar"), _), _) => anychar
                    case BindNode(NNonterminal(Nonterminal("TerminalCharSet"), _), charSetNodes) =>
                        val charRangeNodes = childrenOf(charSetNodes, Nonterminal("TerminalCharRange"))
                        val charRanges: Set[Char] = (charRangeNodes map {
                            case BindNode(NNonterminal(Nonterminal("TerminalCharRange"), _), singleChar@BindNode(NNonterminal(Nonterminal("charSetChar"), _), _)) =>
                                Set(charOf(singleChar))
                            case BindNode(NNonterminal(Nonterminal("TerminalCharRange"), _), BindNode(_: NSequence, seq: SequenceNode)) =>
                                (charOf(seq.childrenAll(0)) to charOf(seq.childrenAll(2))).toSet
                        }).toSet.flatten
                        c(charRanges)
                    case BindNode(_: NSequence, seq: SequenceNode) =>
                        Symbols.ExactChar(charOf(seq.childrenAll(1)))
                }
            case BindNode(NNonterminal(Nonterminal("String"), _), BindNode(_: NSequence, stringBody: SequenceNode)) =>
                val chars = childrenOf(stringBody.childrenAll(1), Nonterminal("stringChar")) map {
                    charOf
                }
                Sequence(chars map {
                    ExactChar
                })
            case nonterminal@BindNode(NNonterminal(Nonterminal("Nonterminal"), _), _) =>
                Nonterminal(nonterminalNameOf(nonterminal))
            case BindNode(NNonterminal(Nonterminal("Proxy"), _), BindNode(_: NSequence, proxyBody: SequenceNode)) =>
                Proxy(sequenceOf(proxyBody.childrenAll(2)))
            case BindNode(NNonterminal(Nonterminal("Longest"), _), BindNode(_: NSequence, longestBody: SequenceNode)) =>
                longest(symbolOf(longestBody.childrenAll(2)))
            case BindNode(_: NSequence, parenEitherBody: SequenceNode) =>
                oneof(childrenOf(parenEitherBody.childrenAll(2), Nonterminal("Symbol")) map {
                    symbolOf
                }: _*)
        }
    }

    def translate(name: String, tree: ParseResultTree.Node): Grammar = {
        val BindNode(_: NStart, BindNode(NNonterminal(Nonterminal("Grammar"), _), BindNode(_, rulesSeq: SequenceNode))) = tree
        val BindNode(NNonterminal(Nonterminal("Rules"), _), rules) = rulesSeq.childrenAll(1)
        val nontermDefs: Seq[(String, Seq[Symbols.Symbol])] = childrenOf(rules, Nonterminal("Rule")) map {
            case BindNode(NNonterminal(Nonterminal("Rule"), _), BindNode(_: NSequence, seq: SequenceNode)) =>
                val nonterminalName = nonterminalNameOf(seq.childrenAll(0))
                val rhsNodesSeq = childrenOf(seq.childrenAll(4), Nonterminal("Sequence"))
                // println(nonterminalName)
                // rhsNodesSeq foreach { rhs => println(s"  = ${textOf(rhs)}") }
                val rhsSeq: Seq[Symbols.Symbol] = rhsNodesSeq map {
                    sequenceOf
                }
                nonterminalName -> rhsSeq
        }
        assert(nontermDefs.nonEmpty)

        val startSymbol = Nonterminal(nontermDefs.head._1)
        val rulesMap = nontermDefs map { kv => kv._1 -> ListSet(kv._2: _*) }
        new NewGrammar(name, ListMap(rulesMap: _*), startSymbol)
    }

    def translate(name: String, source: String): Either[Grammar, String] = {
        val parser = new NaiveParser(NGrammar.fromGrammar(this))
        parser.parse(source) match {
            case Left(ctx) =>
                val tree = new ParseTreeConstructor(ParseForestFunc)(parser.grammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
                tree match {
                    case Some(forest) if forest.trees.size == 1 =>
                        Left(translate(name, forest.trees.head))
                    case _ =>
                        Right("??? ambiguous!")
                }
            case Right(error) =>
                println(error)
                Right(error.msg)
        }
    }

    def translateForce(name: String, source: String): Grammar =
        translate(name, source).left.get

    // TODO 우선순위 처리
    def reverse(grammar: Grammar): String = {
        def nonterminalNameOf(name: String): String = {
            val charArray = name.toCharArray
            if (charArray forall { c => ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || (c == '_') }) {
                name
            } else {
                // TODO \하고 ` escape
                s"`$name`"
            }
        }

        def symbolStringOf(symbol: Symbols.Symbol, outerPrecedence: Int): String = {
            val (string: String, precedence: Int) = symbol match {
                case terminal: Symbols.Terminal =>
                    val s = terminal match {
                        case Any | AnyChar => "."
                        case ExactChar(char) =>
                            val s = char match {
                                case '\n' => "\\n"
                                case '\\' => "\\\\"
                                case '\'' => "\\'"
                                // TODO complete escape
                                case _ => s"$char"
                            }
                            s"'$s'"
                        case chars: Symbols.Terminals.Chars =>
                            def escapeChar(char: Char): String =
                                char match {
                                    case '-' => "\\-"
                                    case '}' => "\\}"
                                    case '\n' => "\\n"
                                    case '\t' => "\\t"
                                    case '\r' => "\\r"
                                    case '\\' => "\\\\"
                                    // TODO complete escape
                                    case _ => "" + char
                                }

                            "{" + (chars.chars.groups.sorted map { pair =>
                                if (pair._1 == pair._2) s"${escapeChar(pair._1)}"
                                else if (pair._1 + 1 == pair._2) s"${escapeChar(pair._1)}${escapeChar(pair._2)}"
                                else s"${escapeChar(pair._1)}-${escapeChar(pair._2)}"
                            } mkString "") + "}"
                    }
                    (s, 0)
                case Nonterminal(nontermName) =>
                    (nonterminalNameOf(nontermName), 0)
                case Sequence(Seq(), _) =>
                    ("ε", 0)
                case Sequence(seq, _) =>
                    if (seq forall {
                        _.isInstanceOf[Symbols.Terminals.ExactChar]
                    }) {
                        // string인 경우
                        // TODO escape
                        ("\"" + (seq map {
                            _.asInstanceOf[Symbols.Terminals.ExactChar].char
                        }).mkString + "\"", 0)
                    } else {
                        (seq map {
                            symbolStringOf(_, 5)
                        } mkString " ", 5)
                    }
                case OneOf(syms) =>
                    if (syms.size == 2 && (syms contains Proxy(Sequence(Seq())))) {
                        (symbolStringOf((syms - Proxy(Sequence(Seq()))).head, 2) + "?", 2)
                    } else {
                        ("(" + (syms.toSeq map {
                            symbolStringOf(_, 5)
                        } mkString " | ") + ")", 0)
                    }
                case Repeat(sym, 0) =>
                    (symbolStringOf(sym, 2) + "*", 2)
                case Repeat(sym, 1) =>
                    (symbolStringOf(sym, 2) + "+", 2)
                case Repeat(sym, more) =>
                    ("[" + ((symbolStringOf(sym, 5) + " ") * more) + (symbolStringOf(sym, 5) + "*") + "]", 0)
                case Except(sym, except) =>
                    (symbolStringOf(sym, 4) + "-" + symbolStringOf(except, 4), 4)
                case LookaheadIs(lookahead) =>
                    ("$" + symbolStringOf(lookahead, 0), 1)
                case LookaheadExcept(lookahead) =>
                    ("!" + symbolStringOf(lookahead, 0), 1)
                case Proxy(proxy) =>
                    ("[" + symbolStringOf(proxy, 5) + "]", 0)
                case Join(sym, join) =>
                    (symbolStringOf(sym, 3) + "&" + symbolStringOf(join, 3), 3)
                case Longest(sym) =>
                    ("<" + symbolStringOf(sym, 5) + ">", 0)
            }
            if (outerPrecedence < precedence) "(" + string + ")" else string
        }

        def ruleStringOf(lhs: String, rhs: ListSet[Symbols.Symbol]): String = {
            nonterminalNameOf(lhs) + " = " + ((rhs map { symbol => symbolStringOf(symbol, 6) }) mkString "\n    | ")
        }

        val startRule = grammar.rules(grammar.startSymbol.name)
        val restRules = grammar.rules - grammar.startSymbol.name

        ruleStringOf(grammar.startSymbol.name, startRule) + "\n" +
            (restRules map { kv => ruleStringOf(kv._1, kv._2) } mkString "\n")
    }
}
