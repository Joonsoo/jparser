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
}
