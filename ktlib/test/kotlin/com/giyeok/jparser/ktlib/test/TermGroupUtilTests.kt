package com.giyeok.jparser.ktlib.test

import com.giyeok.jparser.ktlib.TermGroupUtil
import com.giyeok.jparser.proto.TermGroupProto
import org.junit.jupiter.api.Test

class TermGroupUtilTests {
  @Test
  fun test() {
    val merged = TermGroupUtil.merge(
      listOf(
//        TermGroupProto.TermGroup.newBuilder()
//          .setCharsGroup(
//            TermGroupProto.CharsGroup.newBuilder()
//              .setChars("abc")
//          ).build(),
//        TermGroupProto.TermGroup.newBuilder()
//          .setCharsGroup(
//            TermGroupProto.CharsGroup.newBuilder()
//              .setChars("+")
//          ).build(),
//        TermGroupProto.TermGroup.newBuilder()
//          .setCharsGroup(
//            TermGroupProto.CharsGroup.newBuilder()
//              .setChars(" \n\r\t")
//          ).build(),
        TermGroupProto.TermGroup.newBuilder()
          .setCharsGroup(
            TermGroupProto.CharsGroup.newBuilder()
              .setChars("()[]+*")
          ).build(),
      )
    )

    println(merged)
  }
}
