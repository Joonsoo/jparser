package com.giyeok.jparser.ktparser.mgroup2

import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto
import org.junit.jupiter.api.Test
import java.util.zip.GZIPInputStream

class MilestoneGroupParserKtTests {
  @Test
  fun testMetalang3() {
    val parserData: MilestoneGroupParserDataProto.MilestoneGroupParserData =
      this::class.java.getResourceAsStream("/cdglang3-mg2-parserdata.pb")!!.buffered()
        .use { inputStream ->
          MilestoneGroupParserDataProto.MilestoneGroupParserData.parseFrom(inputStream)
        }

    val parser = MilestoneGroupParserKt(parserData)

    val parseResult = parser.parse("A = 'a'+")
    println(parseResult)

    val kernels = parser.kernelsHistory(parseResult)
    println(kernels)
  }

  @Test
  fun testJ1() {
    val parserData: MilestoneGroupParserDataProto.MilestoneGroupParserData =
      this::class.java.getResourceAsStream("/j1-mg2-parserdata.pb.gz")!!.buffered()
        .use { inputStream ->
          GZIPInputStream(inputStream).use { gzipStream ->
            MilestoneGroupParserDataProto.MilestoneGroupParserData.parseFrom(gzipStream)
          }
        }

    val parser = MilestoneGroupParserKt(parserData)

    val parseResult = parser.parse("class Abc {}")
    println(parseResult)

    val kernels = parser.kernelsHistory(parseResult)
    println(kernels)
  }
}
