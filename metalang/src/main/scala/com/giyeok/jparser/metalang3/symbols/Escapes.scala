package com.giyeok.jparser.metalang3.symbols

import com.giyeok.jparser.Symbols
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.{CharUnicode, StringChar, Terminal, TerminalChoiceElem}

object Escapes {

    implicit class NonterminalName(val nonterminal: MetaGrammar3Ast.NonterminalName) {
        def stringName: String = nonterminal.name.sourceText // backtick escape

        def toSymbol: Symbols.Nonterminal = Symbols.Nonterminal(nonterminal.stringName)
    }

    def terminalCharToChar(c: MetaGrammar3Ast.TerminalChar): Char = c match {
        case MetaGrammar3Ast.CharAsIs(astNode, value) => value.sourceText.charAt(0)
        case MetaGrammar3Ast.CharEscaped(astNode, escapeCode) => escapeCode.sourceText match {
            case "\'" => '\''
            case "\\" => '\\'
            case "b" => '\b'
            case "n" => '\n'
            case "r" => '\r'
            case "t" => '\t'
        }
        case MetaGrammar3Ast.CharUnicode(astNode, code) =>
            assert(code.size == 4)
            Integer.parseInt(s"${code(0).sourceText}${code(1).sourceText}${code(2).sourceText}${code(3).sourceText}", 16).toChar
    }

    def terminalChoiceCharToChar(c: MetaGrammar3Ast.TerminalChoiceChar): Char = c match {
        case MetaGrammar3Ast.CharAsIs(astNode, value) => value.sourceText.charAt(0)
        case MetaGrammar3Ast.CharEscaped(astNode, escapeCode) => escapeCode.sourceText match {
            case "\'" => '\''
            case "\\" => '\\'
            case "b" => '\b'
            case "n" => '\n'
            case "r" => '\r'
            case "t" => '\t'
        }
        case MetaGrammar3Ast.CharUnicode(astNode, code) =>
            assert(code.size == 4)
            Integer.parseInt(s"${code(0).sourceText}${code(1).sourceText}${code(2).sourceText}${code(3).sourceText}", 16).toChar
    }

    def terminalToSymbol(terminal: Terminal): Symbols.Terminal = terminal match {
        case MetaGrammar3Ast.AnyTerminal(astNode) => Symbols.AnyChar
        case char: MetaGrammar3Ast.TerminalChar => Symbols.ExactChar(terminalCharToChar(char))
    }

    def terminalChoiceToSymbol(choices: List[TerminalChoiceElem]): Symbols.Terminal = {
        val chars = choices.flatMap {
            case MetaGrammar3Ast.TerminalChoiceRange(astNode, start, end) =>
                terminalChoiceCharToChar(start) to terminalChoiceCharToChar(end)
            case char: MetaGrammar3Ast.TerminalChoiceChar =>
                Set(terminalChoiceCharToChar(char))
        }.toSet
        Symbols.Chars(chars)
    }

    def stringCharToChar(stringChar: StringChar): Symbols.Terminal = stringChar match {
        case MetaGrammar3Ast.CharAsIs(astNode, value) => ???
        case MetaGrammar3Ast.CharEscaped(astNode, escapeCode) => ???
        case CharUnicode(astNode, code) => ???
    }
}
