package com.giyeok.jparser.ktglue

import com.giyeok.jparser.mgroup2.MilestoneGroupParser
import com.giyeok.jparser.mgroup2.MilestoneGroupParserData
import com.giyeok.jparser.mgroup2.`MilestoneGroupParserDataProtobufConverter$`
import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream

object MilestoneGroup2ParserLoader {
  fun loadParserDataFromStream(stream: InputStream): MilestoneGroupParserData =
    `MilestoneGroupParserDataProtobufConverter$`.`MODULE$`.fromProto(
      MilestoneGroupParserDataProto.MilestoneGroupParserData.parseFrom(stream)
    )

  fun loadParserDataFromResource(name: String): MilestoneGroupParserData =
    this::class.java.getResourceAsStream(name)!!.buffered().use { stream ->
      loadParserDataFromStream(stream)
    }

  fun loadParserDataFromFile(file: Path): MilestoneGroupParserData =
    file.inputStream().buffered().use { stream -> loadParserDataFromStream(stream) }

  fun loadParserFromResource(name: String) =
    MilestoneGroupParser(loadParserDataFromResource(name))

  fun loadParserFromFile(file: Path) =
    MilestoneGroupParser(loadParserDataFromFile(file))
}
