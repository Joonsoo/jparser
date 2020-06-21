package com.giyeok.jparser.metalang3.symbols

import com.giyeok.jparser.Symbols
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.{CharEscaped, CharUnicode, StringChar, Terminal, TerminalChoiceElem, TypeOrFuncName}

object Escapes {

    implicit class NonterminalName(val nonterminal: MetaGrammar3Ast.NonterminalName) {
        def stringName: String = nonterminal.name.sourceText // backtick escape

        def toSymbol: Symbols.Nonterminal = Symbols.Nonterminal(nonterminal.stringName)
    }

    implicit class TypeNameName(val typeName: MetaGrammar3Ast.TypeName) {
        def stringName: String = typeName.name.sourceText // backtick escape
    }

    implicit class EnumTypeNameName(val enumTypeName: MetaGrammar3Ast.EnumTypeName) {
        def stringName: String = enumTypeName.name.sourceText // backtick escape
    }

    implicit class TypeOrFuncNameName(val typeOrFuncName: MetaGrammar3Ast.TypeOrFuncName) {
        def stringName: String = typeOrFuncName.name.sourceText // TODO
    }

    implicit class ParamNameName(val namedParam: MetaGrammar3Ast.ParamName) {
        def stringName: String = namedParam.name.sourceText
    }

    def charEscapedToChar(charEscaped: MetaGrammar3Ast.CharEscaped): Char = charEscaped.escapeCode.sourceText match {
        case "\'" => '\''
        case "\\" => '\\'
        case "b" => '\b'
        case "n" => '\n'
        case "r" => '\r'
        case "t" => '\t'
    }

    def charUnicodeToChar(charUnicode: CharUnicode): Char = {
        val code = charUnicode.code
        assert(code.size == 4)
        Integer.parseInt(s"${code(0).sourceText}${code(1).sourceText}${code(2).sourceText}${code(3).sourceText}", 16).toChar
    }

    def terminalCharToChar(c: MetaGrammar3Ast.TerminalChar): Char = c match {
        case MetaGrammar3Ast.CharAsIs(astNode, value) => value.sourceText.head
        case escaped: MetaGrammar3Ast.CharEscaped => charEscapedToChar(escaped)
        case unicode: MetaGrammar3Ast.CharUnicode => charUnicodeToChar(unicode)
    }

    def terminalChoiceCharToChar(c: MetaGrammar3Ast.TerminalChoiceChar): Char = c match {
        case MetaGrammar3Ast.CharAsIs(astNode, value) => value.sourceText.head
        case escaped: MetaGrammar3Ast.CharEscaped => charEscapedToChar(escaped)
        case unicode: MetaGrammar3Ast.CharUnicode => charUnicodeToChar(unicode)
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

    def stringCharToChar(stringChar: StringChar): Char = stringChar match {
        case MetaGrammar3Ast.CharAsIs(astNode, value) => value.sourceText.head
        case escaped: MetaGrammar3Ast.CharEscaped => charEscapedToChar(escaped)
        case unicode: MetaGrammar3Ast.CharUnicode => charUnicodeToChar(unicode)
    }

    def stringCharsToString(chars: List[StringChar]): String = chars.map(Escapes.stringCharToChar).mkString
}
