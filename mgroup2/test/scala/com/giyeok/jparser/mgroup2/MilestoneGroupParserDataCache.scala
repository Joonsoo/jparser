package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto
import com.giyeok.jparser.proto.GrammarProtobufConverter

import java.nio.file.{Files, Path}
import java.security.MessageDigest

object MilestoneGroupParserDataCache {
  val cacheRoot = Path.of("mgroup2-test-cache")

  def parserDataOf(grammarName: String, grammar: NGrammar): MilestoneGroupParserData = {
    if (!Files.exists(cacheRoot)) {
      Files.createDirectory(cacheRoot)
      println(cacheRoot.toAbsolutePath)
    }

    val sha1 = MessageDigest.getInstance("SHA-1")
    val hash = sha1.digest(GrammarProtobufConverter.convertNGrammarToProto(grammar).toByteArray)
    val hashHex = hash.map("%02X" format _).mkString

    val path = cacheRoot.resolve(grammarName + "-" + hashHex + ".pb")

    if (Files.exists(path)) {
      println(s"Using cached parser data from ${path.toAbsolutePath}")
      val content = Files.readAllBytes(path)
      val proto = MilestoneGroupParserDataProto.MilestoneGroupParserData.parseFrom(content)
      MilestoneGroupParserDataProtobufConverter.fromProto(proto)
    } else {
      val data = new MilestoneGroupParserGen(grammar).parserData()
      println(s"Writing parser data cache to ${path.toAbsolutePath}")
      Files.write(path, MilestoneGroupParserDataProtobufConverter.toProto(data).toByteArray)
      data
    }
  }
}
