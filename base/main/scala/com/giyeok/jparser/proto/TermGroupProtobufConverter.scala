package com.giyeok.jparser.proto

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.Inputs.{CharsGroup, TermGroupDesc}
import com.giyeok.jparser.proto.ProtoConverterUtil._
import com.giyeok.jparser.proto.TermGroupProto.TermGroup.TermGroupCase

import scala.jdk.CollectionConverters.SeqHasAsJava

object TermGroupProtobufConverter {
  def convertTermGroupToProto(termGroup: TermGroupDesc): TermGroupProto.TermGroup = termGroup match {
    case Inputs.AllCharsExcluding(excluding) =>
      TermGroupProto.TermGroup.newBuilder().setAllCharsExcluding(
        TermGroupProto.AllCharsExcluding.newBuilder()
          .setExcluding(convertCharsGroupToProto(excluding))).build()
    case charsGroup: Inputs.CharsGroup =>
      TermGroupProto.TermGroup.newBuilder()
        .setCharsGroup(convertCharsGroupToProto(charsGroup)).build()
    case Inputs.VirtualsGroup(virtualNames) =>
      TermGroupProto.TermGroup.newBuilder()
        .setVirtualsGroup(TermGroupProto.VirtualsGroup.newBuilder()
          .addAllVirtualNames(virtualNames.toList.asJava)).build()
  }

  def convertCharsGroupToProto(charsGroup: CharsGroup): TermGroupProto.CharsGroup =
    TermGroupProto.CharsGroup.newBuilder()
      .addAllUnicodeCategories(charsGroup.unicodeCategories.toList.sorted)
      .addAllExcludingChars(charsGroup.excludingChars.toList.sorted)
      .addAllChars(charsGroup.chars.toList.sorted).build()

  def convertProtoToTermGroup(proto: TermGroupProto.TermGroup): TermGroupDesc = proto.getTermGroupCase match {
    case TermGroupCase.ALL_CHARS_EXCLUDING =>
      Inputs.AllCharsExcluding(convertProtoToCharsGroup(proto.getAllCharsExcluding.getExcluding))
    case TermGroupCase.CHARS_GROUP =>
      convertProtoToCharsGroup(proto.getCharsGroup)
    case TermGroupCase.VIRTUALS_GROUP =>
      Inputs.VirtualsGroup(proto.getVirtualsGroup.getVirtualNamesList.toSet)
  }

  def convertProtoToCharsGroup(proto: TermGroupProto.CharsGroup): CharsGroup = CharsGroup(
    proto.getUnicodeCategoriesList.toSet,
    proto.getExcludingCharsList.map(s => s.charAt(0) ensuring s.length == 1).toSet,
    proto.getCharsList.map(s => s.charAt(0) ensuring s.length == 1).toSet
  )
}
