package com.giyeok.jparser.proto

import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.nparser.proto.NaiveParserProto

object ProtobufConverter {
  def convertKernelToProto(kernel: Kernel): NaiveParserProto.Kernel =
    NaiveParserProto.Kernel.newBuilder()
      .setSymbolId(kernel.symbolId).setPointer(kernel.pointer).setBeginGen(kernel.beginGen).setEndGen(kernel.endGen).build()

  def convertProtoToKernel(proto: NaiveParserProto.Kernel): Kernel =
    Kernel(proto.getSymbolId, proto.getPointer, proto.getBeginGen, proto.getEndGen)
}
