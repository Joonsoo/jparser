package com.giyeok.jparser.metalang3.symbols

import com.giyeok.jparser.{Grammar, Symbols}
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import Escapes.NonterminalName

object GrammarGen {
    def generateGrammarFrom(ast: MetaGrammar3Ast.Grammar, grammarName: String): Grammar = {
        ast.defs.collect { case MetaGrammar3Ast.Rule(astNode, lhs, rhsList) =>
            lhs.name.name.stringName -> (rhsList map { rhs =>
                rhs.elems collect { case symbol: MetaGrammar3Ast.Symbol =>
                    symbolFrom(symbol)
                }
            })
        }
        ???
    }

    def symbolFrom(symbol: MetaGrammar3Ast.Symbol): Symbols.Symbol = symbol match {
        case MetaGrammar3Ast.JoinSymbol(astNode, body, join) =>
            val bodySymbol = symbolFrom(body)
            val joinSymbol = symbolFrom(join)
            // Symbols.Join(bodySymbol, joinSymbol)
            ???
        case MetaGrammar3Ast.ExceptSymbol(astNode, body, except) => ???
        case MetaGrammar3Ast.FollowedBy(astNode, followedBy) =>
            val followedBySymbol = symbolFrom(followedBy)
            // Symbols.LookaheadIs(followedBySymbol)
            ???
        case MetaGrammar3Ast.NotFollowedBy(astNode, notFollowedBy) => ???
        case MetaGrammar3Ast.Optional(astNode, body) =>
            ???
        case MetaGrammar3Ast.RepeatFromZero(astNode, body) =>
            val bodySymbol = symbolFrom(body)
            // Symbols.Repeat(bodySymbol, 0)
            ???
        case MetaGrammar3Ast.RepeatFromOne(astNode, body) =>
            val bodySymbol = symbolFrom(body)
            // Symbols.Repeat(bodySymbol, 1)
            ???
        case terminal: MetaGrammar3Ast.Terminal => terminal match {
            case MetaGrammar3Ast.AnyTerminal(astNode) => Symbols.AnyChar
            case MetaGrammar3Ast.CharAsIs(astNode, value) => ???
            case MetaGrammar3Ast.CharEscaped(astNode, escapeCode) => ???
            case MetaGrammar3Ast.CharUnicode(astNode, code) => ???
        }
        case MetaGrammar3Ast.TerminalChoice(astNode, choices) => ???
        case MetaGrammar3Ast.StringSymbol(astNode, value) => ???
        case MetaGrammar3Ast.Nonterminal(_, name) => Symbols.Nonterminal(name.stringName)
        case MetaGrammar3Ast.InPlaceChoices(astNode, choices) => ???
        case MetaGrammar3Ast.Longest(astNode, choices) => ???
        case MetaGrammar3Ast.EmptySeq(astNode) => ???
        case MetaGrammar3Ast.InPlaceSequence(astNode, seq) => ???
    }
}
