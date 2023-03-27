package com.giyeok.jparser.ktlib

import com.giyeok.jparser.proto.TermGroupProto

object TermGroupUtil {
  fun isMatch(termGroup: TermGroupProto.TermGroup, input: Char): Boolean =
    when (termGroup.termGroupCase) {
      TermGroupProto.TermGroup.TermGroupCase.ALL_CHARS_EXCLUDING ->
        !isMatch(termGroup.allCharsExcluding.excluding, input)

      TermGroupProto.TermGroup.TermGroupCase.CHARS_GROUP ->
        isMatch(termGroup.charsGroup, input)

      TermGroupProto.TermGroup.TermGroupCase.VIRTUALS_GROUP -> TODO()

      TermGroupProto.TermGroup.TermGroupCase.TERMGROUP_NOT_SET -> TODO()
    }

  fun isMatch(charsGroup: TermGroupProto.CharsGroup, input: Char): Boolean {
    val inputCategory = input.category.value
    // TODO excludingChars와 chars가 정렬되어 있는 점을 활용해서 최적화
    return (!charsGroup.excludingChars.contains(input) &&
      (charsGroup.unicodeCategoriesList.contains(inputCategory) ||
        charsGroup.chars.contains(input)))
  }

  class TermGroupBuilder {
    // negative가 true이면 모든 글자 중 unicodeCategories, excludingChars, includingChars 로 정의되는 글자들은 빼고 전부
    private var negative = false

    // unicodeCatgories로 지정된 유니코드 카테고리에 속하는 모든 문자-excludingChars+includingChars, virtuals는 별개
    private val unicodeCategories = mutableSetOf<Int>()
    private val excludingChars = mutableSetOf<Char>()
    private val includingChars = mutableSetOf<Char>()

    private val virtuals = mutableSetOf<String>()

    fun add(termGroup: TermGroupProto.TermGroup) {
      when (termGroup.termGroupCase) {
        TermGroupProto.TermGroup.TermGroupCase.CHARS_GROUP -> {
          val charsGroup = termGroup.charsGroup
          if (!negative) {
            if (charsGroup.unicodeCategoriesList.isNotEmpty()) {
              addUnicodeCategory(
                charsGroup.unicodeCategoriesList.toSet(),
                charsGroup.excludingChars.toSet()
              )
            }
            charsGroup.chars.forEach { addChar(it) }
          }
        }

        TermGroupProto.TermGroup.TermGroupCase.ALL_CHARS_EXCLUDING -> {
          TODO()
        }

        TermGroupProto.TermGroup.TermGroupCase.VIRTUALS_GROUP ->
          virtuals.addAll(termGroup.virtualsGroup.virtualNamesList)

        else -> TODO()
      }
    }

    private fun addUnicodeCategory(unicodeCategories: Set<Int>, excludingChars: Set<Char>) {
      TODO()
    }

    private fun addChar(char: Char) {
      excludingChars.remove(char)
      val charCategory = char.category.value
      if (!unicodeCategories.contains(charCategory)) {
        includingChars.add(char)
      }
    }

    fun build(): TermSet =
      TermSet(negative, unicodeCategories, excludingChars, includingChars, virtuals)
  }

  fun merge(termGroups: List<TermGroupProto.TermGroup>): TermSet {
    val builder = TermGroupBuilder()
    termGroups.forEach { termGroup ->
      builder.add(termGroup)
    }
    return builder.build()
  }
}

class TermSet(
  val negative: Boolean,
  val unicodeCategories: Set<Int>,
  val excludingChars: Set<Char>,
  val includingChars: Set<Char>,
  val virtuals: Set<String>,
) {
  companion object {
    fun charsPrettyString(chars: Collection<Char>): String {
      if (chars.isEmpty()) {
        return ""
      }
      val builder = mutableListOf<String>()

      fun addRange(startRange: Char, endRange: Char) {
        fun prettyChar(c: Char): String = when (c) {
          ' ' -> "' '"
          '\\' -> "\\"
          '\n' -> "\\n"
          '\r' -> "\\r"
          '\t' -> "\\t"
          else -> c.toString()
        }
        when {
          startRange == endRange -> builder.add(prettyChar(startRange))

          startRange + 1 == endRange -> {
            builder.add(prettyChar(startRange))
            builder.add(prettyChar(endRange))
          }

          else -> builder.add("${prettyChar(startRange)}-${prettyChar(endRange)}")
        }
      }

      val groupableRanges = setOf('a'..'z', 'A'..'Z', '0'..'9')
      val sorted = chars.sorted()
      var startRange = sorted.first()
      var endRange = sorted.first()
      sorted.drop(1).forEach { char ->
        if (groupableRanges.any { it.contains(char) } && char == endRange + 1) {
          endRange = char
        } else {
          addRange(startRange, endRange)
          startRange = char
          endRange = char
        }
      }
      addRange(startRange, endRange)
      return builder.joinToString(", ")
    }
  }

  override fun toString(): String {
    val builder = StringBuilder()

    if (negative) {
      builder.append("-")
    }
    if (unicodeCategories.isEmpty()) {
      builder.append("[${charsPrettyString(includingChars)}]")
    } else {
      val unicodeCates = unicodeCategories.sorted().joinToString(", ") { it.toString() }
      val excludings = charsPrettyString(excludingChars)
      val includings = charsPrettyString(includingChars)
      builder.append("$unicodeCates - [$excludings] + [$includings]")
    }
    if (virtuals.isNotEmpty()) {
      builder.append(", $virtuals")
    }
    return builder.toString()
  }
}
