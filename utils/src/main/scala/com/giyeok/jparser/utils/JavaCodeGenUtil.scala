package com.giyeok.jparser.utils

object JavaCodeGenUtil {
  def isPrintableChar(char: Char): Boolean = {
    val block = Character.UnicodeBlock.of(char)
    (!Character.isISOControl(char)) && block != null && block != Character.UnicodeBlock.SPECIALS
  }

  def javaChar(char: Char): String = char match {
    case '\b' => "\\b"
    case '\n' => "\\n"
    case '\r' => "\\r"
    case '\t' => "\\t"
    case '\\' => "\\\\"
    case '\'' => "\\'"
    case c if !isPrintableChar(c) && c.toInt < 65536 =>
      val c1 = (c.toInt >> 8) % 256
      val c2 = c.toInt % 256
      val hexChars = "0123456789abcdef"
      s"\\u${hexChars(c1 >> 4)}${hexChars(c1 & 15)}${hexChars(c2 >> 4)}${hexChars(c2 & 15)}"
    case c => c.toString
  }
}
