package com.giyeok.jparser.parsergen.nocond.codegen

import com.giyeok.jparser.Inputs.{CharacterTermGroupDesc, CharsGroup, CharsGrouping, TermGroupDesc}

object JavaGenUtils {
    implicit def termGroupOrdering[A <: TermGroupDesc]: Ordering[A] = (x: A, y: A) => {
        (x, y) match {
            case (xx: CharsGroup, yy: CharsGroup) =>
                xx.chars.min - yy.chars.min
        }
    }

    def javaChar(c: Char): String = c match {
        case '\n' => "\\n"
        case '\r' => "\\r"
        case '\t' => "\\t"
        case '\\' => "\\\\"
        // TODO finish
        case c => c.toString
    }

    def escapeToJavaString(str: String): String =
        str.replaceAllLiterally("\\", "\\\\").replaceAllLiterally("\"", "\\\"")

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
