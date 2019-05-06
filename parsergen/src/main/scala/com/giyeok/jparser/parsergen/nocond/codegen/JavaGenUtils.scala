package com.giyeok.jparser.parsergen.nocond.codegen

import com.giyeok.jparser.Inputs.{CharacterTermGroupDesc, CharsGroup, CharsGrouping, TermGroupDesc}

object JavaGenUtils {
    implicit def termGroupOrdering[A <: TermGroupDesc]: Ordering[A] = (x: A, y: A) => {
        (x, y) match {
            case (xx: CharsGroup, yy: CharsGroup) =>
                xx.chars.min - yy.chars.min
        }
    }

    def javaChar(char: Char): String = char match {
        case '\n' => "\\n"
        case '\r' => "\\r"
        case '\t' => "\\t"
        case '\\' => "\\\\"
        // TODO finish
        case c if (c.toInt < 32 || c.toInt > 126) && c.toInt < 65536 =>
            val c1 = (c.toInt >> 8) % 256
            val c2 = c.toInt % 256
            val hexChars = "0123456789abcdef"
            s"\\u${hexChars(c1 >> 4)}${hexChars(c1 & 15)}${hexChars(c2 >> 4)}${hexChars(c2 & 15)}"
        case c => c.toString
    }

    def escapeToJavaString(str: String): String =
        str.flatMap(javaChar).replaceAllLiterally("\"", "\\\"")

    def javaString(str: String): String =
        "\"" + escapeToJavaString(str) + "\""

    def charsToCondition(chars: Set[Char], varName: String): String =
        chars.groups map { group =>
            if (group._1 == group._2) s"($varName == '${javaChar(group._1)}')"
            else s"('${javaChar(group._1)}' <= $varName && $varName <= '${javaChar(group._2)}')"
        } mkString " || "

    def charGroupToCondition(charsGroup: CharsGroup, varName: String): String =
        charsToCondition(charsGroup.chars, varName)

    def charGroupToCondition(termGroupDesc: CharacterTermGroupDesc, varName: String): String =
        charGroupToCondition(termGroupDesc.asInstanceOf[CharsGroup], varName)
}
