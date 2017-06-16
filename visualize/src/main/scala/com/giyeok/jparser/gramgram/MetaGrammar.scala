package com.giyeok.jparser.gramgram

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.ParseResultTree
import com.giyeok.jparser.ParseResultTree._
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParseTreeConstructor

object MetaGrammar extends Grammar {
    val name = "Meta Grammar"
    val rules: RuleMap = ListMap(
        "Grammar" -> ListSet(
            Sequence(Seq(n("ws").star, n("Rules"), n("ws").star), Seq(1))
        ),
        "Rules" -> ListSet(
            Sequence(Seq(n("Rules"), longest((n("ws").except(c('\n'))).star), c('\n'), n("ws").star, n("Rule")), Seq(0, 4)),
            n("Rule")
        ),
        "Rule" -> ListSet(
            seqWS(n("ws").star, n("Nonterminal"), c('='), n("RHSs"))
        ),
        "RHSs" -> ListSet(
            seqWS(n("ws").star, n("RHSs"), c('|'), n("Sequence")),
            n("Sequence")
        ),
        "EmptySequence" -> ListSet(
            empty,
            c("#ε".toSet)
        ),
        "Sequence" -> ListSet(
            n("EmptySequence"),
            n("Symbol"),
            n("SymbolSeq")
        ),
        "SymbolSeq" -> ListSet(
            // Symbol 2개 이상
            seqWS(n("ws").plus, n("SymbolSeq"), n("Symbol")),
            seqWS(n("ws").plus, n("Symbol"), n("Symbol"))
        ),
        "Symbol" -> ListSet(
            n("Exclusion").except(n("Symbol4")),
            n("Symbol4")
        ),
        "Exclusion" -> ListSet(
            n("Symbol4"),
            seqWS(n("ws").star, n("Exclusion"), c('-'), n("Symbol4"))
        ),
        "Symbol4" -> ListSet(
            n("Intersection").except(n("Symbol3")),
            n("Symbol3")
        ),
        "Intersection" -> ListSet(
            n("Symbol3"),
            seqWS(n("ws").star, n("Intersection"), c('&'), n("Symbol3"))
        ),
        "Symbol3" -> ListSet(
            n("Repeat0"),
            n("Repeat1"),
            n("Optional"),
            n("Symbol2")
        ),
        "Repeat0" -> ListSet(
            seqWS(n("ws").star, n("Symbol3"), c('*'))
        ),
        "Repeat1" -> ListSet(
            seqWS(n("ws").star, n("Symbol3"), c('+'))
        ),
        "Optional" -> ListSet(
            seqWS(n("ws").star, n("Symbol3"), c('?'))
        ),
        "Symbol2" -> ListSet(
            n("FollowedBy"),
            n("NotFollowedBy"),
            n("Symbol1")
        ),
        "FollowedBy" -> ListSet(
            seqWS(n("ws").star, c('$'), n("Symbol2"))
        ),
        "NotFollowedBy" -> ListSet(
            seqWS(n("ws").star, c('!'), n("Symbol2"))
        ),
        "Symbol1" -> ListSet(
            n("Terminal"),
            n("String"),
            n("Nonterminal"),
            n("Proxy"),
            n("Longest"),
            seqWS(n("ws").star, c('('), n("Symbol"), c(')')),
            seqWS(n("ws").star, c('('), n("Either"), c(')'))
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
            seqWS(n("ws").star, c('['), n("Sequence"), c(']'))
        ),
        "Either" -> ListSet(
            seqWS(n("ws").star, n("Symbol"), c('|'), n("Symbol")),
            seqWS(n("ws").star, n("Either"), c('|'), n("Symbol"))
        ),
        "Longest" -> ListSet(
            seqWS(n("ws").star, c('<'), n("Symbol"), c('>'))
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
        "ws" -> ListSet(
            chars(" \t\n\r")
        )
    )
    val startSymbol: Nonterminal = n("Grammar")

    class NewGrammar(val name: String, val rules: ListMap[String, ListSet[Symbols.Symbol]], val startSymbol: Symbols.Nonterminal) extends Grammar

    def childrenOf(node: Node, sym: Symbol): Seq[Node] = node match {
        case BindNode(s, body) if s == sym => Seq(node)
        case BindNode(s, body) => childrenOf(body, sym)
        case s: SequenceNode => s.children flatMap { childrenOf(_, sym) }
        case _ => Seq()
    }
    def textOf(node: Node): String = node match {
        case BindNode(s, body) => textOf(body)
        case s: SequenceNode => (s.children map { textOf }).mkString
        case JoinNode(body, join) => textOf(body)
        case TerminalNode(Inputs.Character(c)) => s"$c"
        case _ => ???
    }

    def charOf(node: Node): Char = {
        node match {
            case BindNode(_, unicodeChar @ BindNode(Nonterminal("unicodeChar"), _)) =>
                charOf(unicodeChar)
            case BindNode(Nonterminal("unicodeChar"), BindNode(_: Sequence, unicodeCharBody: SequenceNode)) =>
                val code = Integer.parseInt(textOf(unicodeCharBody).substring(2).toLowerCase(), 16)
                code.toChar
            case BindNode(Nonterminal(charNontermName), charBody) =>
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
        val BindNode(Nonterminal("Sequence"), body) = node
        body match {
            case BindNode(Nonterminal("EmptySequence"), _) =>
                empty
            case BindNode(Nonterminal("Symbol"), _) =>
                symbolOf(body)
            case BindNode(Nonterminal("SymbolSeq"), symbolSeq) =>
                Sequence(childrenOf(symbolSeq, Nonterminal("Symbol")) map { node => proxyIfNeeded(symbolOf(node)) })
        }
    }

    def nonterminalNameOf(node: Node): String = {
        val BindNode(Nonterminal("Nonterminal"), nonterminalNameBody) = node
        nonterminalNameBody match {
            case BindNode(Nonterminal("PlainNonterminalName"), nameBody) =>
                textOf(nameBody)
            case BindNode(Nonterminal("QuoteNonterminalName"), BindNode(_: Sequence, nameBody: SequenceNode)) =>
                (childrenOf(nameBody.childrenAll(1), Nonterminal("nontermNameChar")) map { charOf }).mkString
        }
    }

    def symbolOf(node: Node): Symbols.Symbol = {
        node match {
            case BindNode(Nonterminal("Symbol"), symbolBody) =>
                symbolBody match {
                    case BindNode(_: Symbols.Except, exclusionBody @ BindNode(Nonterminal("Exclusion"), _)) =>
                        // Exclusion
                        symbolOf(exclusionBody)
                    case BindNode(Nonterminal("Symbol4"), _) =>
                        symbolOf(symbolBody)
                }
            case BindNode(Nonterminal("Exclusion"), exclusionBody) =>
                exclusionBody match {
                    case BindNode(Nonterminal("Symbol4"), _) =>
                        symbolOf(exclusionBody)
                    case BindNode(_: Sequence, exclusionBody: SequenceNode) =>
                        symbolOf(exclusionBody.childrenAll(0)).except(symbolOf(exclusionBody.childrenAll(4)))
                }
            case BindNode(Nonterminal("Symbol4"), symbol4Body) =>
                symbol4Body match {
                    case BindNode(_: Symbols.Except, intersectionBody @ BindNode(Nonterminal("Intersection"), _)) =>
                        // Intersection
                        symbolOf(intersectionBody)
                    case BindNode(Nonterminal("Symbol3"), _) =>
                        symbolOf(symbol4Body)
                }
            case BindNode(Nonterminal("Intersection"), intersectionBody) =>
                intersectionBody match {
                    case BindNode(Nonterminal("Symbol3"), symbol3) =>
                        symbolOf(symbol3)
                    case BindNode(_: Sequence, intersectionBody: SequenceNode) =>
                        symbolOf(intersectionBody.childrenAll(0)).join(symbolOf(intersectionBody.childrenAll(4)))
                }
            case BindNode(Nonterminal("Symbol3"), symbol3Body) =>
                symbolOf(symbol3Body)
            case BindNode(Nonterminal("Repeat0"), BindNode(_: Sequence, repeat0Body: SequenceNode)) =>
                symbolOf(repeat0Body.childrenAll.head).star
            case BindNode(Nonterminal("Repeat1"), BindNode(_: Sequence, repeat1Body: SequenceNode)) =>
                symbolOf(repeat1Body.childrenAll.head).plus
            case BindNode(Nonterminal("Optional"), BindNode(_: Sequence, optionalBody: SequenceNode)) =>
                symbolOf(optionalBody.childrenAll.head).opt
            case BindNode(Nonterminal("Symbol2"), symbol2Body) =>
                symbolOf(symbol2Body)
            case BindNode(Nonterminal("FollowedBy"), BindNode(_: Sequence, followedByBody: SequenceNode)) =>
                lookahead_is(symbolOf(followedByBody.childrenAll(2)))
            case BindNode(Nonterminal("NotFollowedBy"), BindNode(_: Sequence, notFollowedByBody: SequenceNode)) =>
                lookahead_except(symbolOf(notFollowedByBody.childrenAll(2)))
            case BindNode(Nonterminal("Symbol1"), symbol1Body) =>
                symbolOf(symbol1Body)
            case BindNode(Nonterminal("Terminal"), terminalBody) =>
                terminalBody match {
                    case BindNode(Nonterminal("anychar"), _) => anychar
                    case BindNode(Nonterminal("TerminalCharSet"), charSetNodes) =>
                        val charRangeNodes = childrenOf(charSetNodes, Nonterminal("TerminalCharRange"))
                        val charRanges: Set[Char] = (charRangeNodes map {
                            case BindNode(Nonterminal("TerminalCharRange"), singleChar @ BindNode(Nonterminal("charSetChar"), _)) =>
                                Set(charOf(singleChar))
                            case BindNode(Nonterminal("TerminalCharRange"), BindNode(_: Sequence, seq: SequenceNode)) =>
                                (charOf(seq.childrenAll(0)) to charOf(seq.childrenAll(2))).toSet
                        }).toSet.flatten
                        c(charRanges)
                    case BindNode(_: Sequence, seq: SequenceNode) =>
                        Symbols.ExactChar(charOf(seq.childrenAll(1)))
                }
            case BindNode(Nonterminal("String"), BindNode(_: Sequence, stringBody: SequenceNode)) =>
                val chars = childrenOf(stringBody.childrenAll(1), Nonterminal("stringChar")) map { charOf }
                Sequence(chars map { ExactChar })
            case nonterminal @ BindNode(Nonterminal("Nonterminal"), _) =>
                Nonterminal(nonterminalNameOf(nonterminal))
            case BindNode(Nonterminal("Proxy"), BindNode(_: Sequence, proxyBody: SequenceNode)) =>
                Proxy(sequenceOf(proxyBody.childrenAll(2)))
            case BindNode(Nonterminal("Longest"), BindNode(_: Sequence, longestBody: SequenceNode)) =>
                longest(symbolOf(longestBody.childrenAll(2)))
            case BindNode(_: Sequence, parenEitherBody: SequenceNode) =>
                oneof(childrenOf(parenEitherBody.childrenAll(2), Nonterminal("Symbol")) map { symbolOf }: _*)
        }
    }

    def translate(name: String, tree: ParseResultTree.Node): Grammar = {
        val BindNode(Start, BindNode(Nonterminal("Grammar"), BindNode(_, rulesSeq: SequenceNode))) = tree
        val BindNode(Nonterminal("Rules"), rules) = rulesSeq.childrenAll(1)
        val nontermDefs: Seq[(String, Seq[Symbols.Symbol])] = childrenOf(rules, Nonterminal("Rule")) map {
            case BindNode(Nonterminal("Rule"), BindNode(_: Sequence, seq: SequenceNode)) =>
                val nonterminalName = nonterminalNameOf(seq.childrenAll(0))
                val rhsNodesSeq = childrenOf(seq.childrenAll(4), Nonterminal("Sequence"))
                // println(nonterminalName)
                // rhsNodesSeq foreach { rhs => println(s"  = ${textOf(rhs)}") }
                val rhsSeq: Seq[Symbols.Symbol] = rhsNodesSeq map { sequenceOf }
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
            val string = symbol match {
                case terminal: Symbols.Terminal =>
                    terminal match {
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
                            "{" + (chars.groups.sorted map { pair =>
                                if (pair._1 == pair._2) s"${escapeChar(pair._1)}"
                                else if (pair._1 + 1 == pair._2) s"${escapeChar(pair._1)}${escapeChar(pair._2)}"
                                else s"${escapeChar(pair._1)}-${escapeChar(pair._2)}"
                            } mkString "") + "}"
                    }
                case Nonterminal(nontermName) =>
                    nonterminalNameOf(nontermName)
                case Sequence(Seq(), _) =>
                    "ε"
                case Sequence(seq, _) =>
                    // TODO string if possible
                    seq map { symbolStringOf(_, 0) } mkString " "
                case OneOf(syms) =>
                    if (syms.size == 2 && (syms contains Sequence(Seq()))) {
                        symbolStringOf((syms - Sequence(Seq())).head, 0) + "?"
                    } else {
                        "(" + (syms.toSeq map { symbolStringOf(_, 0) } mkString " | ") + ")"
                    }
                case Repeat(sym, 0) =>
                    symbolStringOf(sym, 0) + "*"
                case Repeat(sym, 1) =>
                    symbolStringOf(sym, 0) + "+"
                case Repeat(sym, more) =>
                    "[" + ((symbolStringOf(sym, 0) + " ") * more) + (symbolStringOf(sym, 0) + "*") + "]"
                case Except(sym, except) =>
                    symbolStringOf(sym, 0) + "-" + symbolStringOf(except, 0)
                case LookaheadIs(lookahead) =>
                    "~" + symbolStringOf(lookahead, 0)
                case LookaheadExcept(lookahead) =>
                    "!" + symbolStringOf(lookahead, 0)
                case Proxy(proxy) =>
                    "[" + symbolStringOf(proxy, 0) + "]"
                case Join(sym, join) =>
                    symbolStringOf(sym, 0) + "&" + symbolStringOf(join, 0)
                case Longest(sym) =>
                    "<" + symbolStringOf(sym, 0) + ">"
            }
            string
        }
        def ruleStringOf(lhs: String, rhs: ListSet[Symbols.Symbol]): String = {
            nonterminalNameOf(lhs) + " = " + ((rhs map { symbol => symbolStringOf(symbol, 0) }) mkString "\n    | ")
        }

        val startRule = grammar.rules(grammar.startSymbol.name)
        val restRules = grammar.rules - grammar.startSymbol.name

        ruleStringOf(grammar.startSymbol.name, startRule) + "\n" +
            (restRules map { kv => ruleStringOf(kv._1, kv._2) } mkString "\n")
    }
}
