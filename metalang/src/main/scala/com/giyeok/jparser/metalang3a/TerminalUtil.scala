package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.Symbols
import com.giyeok.jparser.metalang3a.generated.MetaLang3Ast

object TerminalUtil {

  def charEscapedToChar(charEscaped: MetaLang3Ast.CharEscaped): Char = charEscaped.escapeCode match {
    case '\'' => '\''
    case '\"' => '"'
    case '\\' => '\\'
    case 'b' => '\b'
    case 'n' => '\n'
    case 'r' => '\r'
    case 't' => '\t'
    case '-' => '-'
  }

  def charUnicodeToChar(charUnicode: MetaLang3Ast.CharUnicode): Char = {
    val code = charUnicode.code
    assert(code.size == 4)
    Integer.parseInt(s"${code.head}${code(1)}${code(2)}${code(3)}", 16).toChar
  }

  def terminalCharToChar(c: MetaLang3Ast.TerminalChar): Char = c match {
    case MetaLang3Ast.CharAsIs(value) => value
    case escaped: MetaLang3Ast.CharEscaped => charEscapedToChar(escaped)
    case unicode: MetaLang3Ast.CharUnicode => charUnicodeToChar(unicode)
  }

  def terminalChoiceCharToChar(c: MetaLang3Ast.TerminalChoiceChar): Char = c match {
    case MetaLang3Ast.CharAsIs(value) => value
    case escaped: MetaLang3Ast.CharEscaped => charEscapedToChar(escaped)
    case unicode: MetaLang3Ast.CharUnicode => charUnicodeToChar(unicode)
  }

  def terminalToSymbol(terminal: MetaLang3Ast.Terminal): Symbols.Terminal = terminal match {
    case MetaLang3Ast.AnyTerminal() => Symbols.AnyChar
    case char: MetaLang3Ast.TerminalChar => Symbols.ExactChar(terminalCharToChar(char))
  }

  def terminalChoicesToSymbol(choices: List[MetaLang3Ast.TerminalChoiceElem]): Symbols.Terminal = {
    val chars = choices.flatMap {
      case MetaLang3Ast.TerminalChoiceRange(start, end) =>
        terminalChoiceCharToChar(start) to terminalChoiceCharToChar(end)
      case char: MetaLang3Ast.TerminalChoiceChar =>
        Set(terminalChoiceCharToChar(char))
    }.toSet
    Symbols.Chars(chars)
  }

  def stringCharToChar(stringChar: MetaLang3Ast.StringChar): Char = stringChar match {
    case MetaLang3Ast.CharAsIs(value) => value
    case escaped: MetaLang3Ast.CharEscaped => charEscapedToChar(escaped)
    case unicode: MetaLang3Ast.CharUnicode => charUnicodeToChar(unicode)
  }

  def stringCharsToString(chars: List[MetaLang3Ast.StringChar]): String =
    chars.map(stringCharToChar).mkString
}
