package com.giyeok.jparser.metalang3

import com.giyeok.jparser.mgroup2.{MilestoneGroupParser, MilestoneGroupParserDataProtobufConverter}
import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto.MilestoneGroupParserData

object MetaLang3Parser {
  val parserData = MilestoneGroupParserDataProtobufConverter.fromProto(
    MilestoneGroupParserData.parseFrom(getClass.getResourceAsStream("/cdglang3-mg2-parserdata.pb"))
  )

  val parser = new MilestoneGroupParser(parserData)
}
