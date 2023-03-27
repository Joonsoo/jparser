package com.giyeok.jparser.ktparser.mgroup2

import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto.MilestoneGroupParserData
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import kotlin.io.path.inputStream

object MilestoneGroupParserLoader {
  fun loadParserDataFromStream(inputStream: InputStream): MilestoneGroupParserData =
    MilestoneGroupParserData.parseFrom(inputStream)

  fun loadGzippedParserDataFromStream(inputStream: InputStream): MilestoneGroupParserData =
    GZIPInputStream(inputStream).use { stream ->
      loadParserDataFromStream(stream)
    }

  fun loadParserDataFromResource(resourceName: String): MilestoneGroupParserData =
    this::class.java.getResourceAsStream(resourceName)!!.buffered().use { inputStream ->
      loadParserDataFromStream(inputStream)
    }

  fun loadParserFromResource(resourceName: String): MilestoneGroupParserKt =
    MilestoneGroupParserKt(loadParserDataFromResource(resourceName))

  fun loadParserDataFromGzippedResource(resourceName: String): MilestoneGroupParserData =
    this::class.java.getResourceAsStream(resourceName)!!.buffered().use { inputStream ->
      loadGzippedParserDataFromStream(inputStream)
    }

  fun loadParserFromGzippedResource(resourceName: String): MilestoneGroupParserKt =
    MilestoneGroupParserKt(loadParserDataFromGzippedResource(resourceName))

  fun loadParserFromFile(file: Path): MilestoneGroupParserKt =
    file.inputStream().buffered().use { inputStream ->
      MilestoneGroupParserKt(loadParserDataFromStream(inputStream))
    }

  fun loadParserFromGzippedFile(file: Path): MilestoneGroupParserKt =
    file.inputStream().buffered().use { inputStream ->
      MilestoneGroupParserKt(loadGzippedParserDataFromStream(inputStream))
    }
}
