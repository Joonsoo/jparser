package com.giyeok.jparser.milestone2.test

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto
import com.giyeok.jparser.milestone2.{MilestoneParser2ProtobufConverter, MilestoneParserData, MilestoneParserGen}
import com.giyeok.jparser.proto.GrammarProtobufConverter

import java.nio.file.{Files, Path}
import java.security.MessageDigest

object MilestoneParserDataCache {
  val cacheRoot = Path.of("milestone2-test-cache")

  def parserDataOf(grammarName: String, grammar: NGrammar): MilestoneParserData = {
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
      val proto = MilestoneParserDataProto.Milestone2ParserData.parseFrom(content)
      MilestoneParser2ProtobufConverter.fromProto(proto)
    } else {
      val data = new MilestoneParserGen(grammar).parserData()
      println(s"Writing parser data cache to ${path.toAbsolutePath}")
      Files.write(path, MilestoneParser2ProtobufConverter.toProto(data).toByteArray)
      data
    }
  }
}
