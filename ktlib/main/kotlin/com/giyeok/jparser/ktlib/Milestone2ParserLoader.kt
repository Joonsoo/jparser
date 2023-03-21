package com.giyeok.jparser.ktlib

import com.giyeok.jparser.milestone2.MilestoneParser
import com.giyeok.jparser.milestone2.`MilestoneParser2ProtobufConverter$`
import com.giyeok.jparser.milestone2.MilestoneParserData
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream

object Milestone2ParserLoader {
  fun loadParserDataFromStream(stream: InputStream): MilestoneParserData =
    `MilestoneParser2ProtobufConverter$`.`MODULE$`.fromProto(
      MilestoneParserDataProto.Milestone2ParserData.parseFrom(stream)
    )

  fun loadParserDataFromResource(name: String): MilestoneParserData =
    this::class.java.getResourceAsStream(name)!!.buffered().use { stream ->
      loadParserDataFromStream(stream)
    }

  fun loadParserDataFromFile(file: Path): MilestoneParserData =
    file.inputStream().buffered().use { stream -> loadParserDataFromStream(stream) }

  fun loadParserFromResource(name: String) =
    MilestoneParser(loadParserDataFromResource(name))

  fun loadParserFromFile(file: Path) =
    MilestoneParser(loadParserDataFromFile(file))
}
