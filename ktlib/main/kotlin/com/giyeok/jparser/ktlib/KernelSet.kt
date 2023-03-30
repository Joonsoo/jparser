package com.giyeok.jparser.ktlib

import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto

// TODO 성능 개선 포인트들이 있을 듯
class KernelSet(val kernels: Set<Kernel>) {
  class Builder {
    private val setBuilder = mutableSetOf<Kernel>()

    fun addKernel(symbolId: Int, pointer: Int, beginGen: Int, endGen: Int) {
      setBuilder.add(Kernel(symbolId, pointer, beginGen, endGen))
    }

    fun addKernel(kernel: MilestoneParserDataProto.Kernel, genMap: Map<Int, Int>) {
      addKernel(
        kernel.symbolId,
        kernel.pointer,
        genMap.getValue(kernel.beginGen),
        genMap.getValue(kernel.endGen)
      )
    }

    fun build(): KernelSet =
      KernelSet(setBuilder.toSet())
  }

  fun filter(pred: (Kernel) -> Boolean) = kernels.filter(pred)

  fun filterByBeginGen(symbolId: Int, pointer: Int, beginGen: Int): Set<Kernel> = kernels
    .filter { it.symbolId == symbolId && it.pointer == pointer && it.beginGen == beginGen }
    .toSet()

  fun findByBeginGen(symbolId: Int, pointer: Int, beginGen: Int): Kernel =
    filterByBeginGen(symbolId, pointer, beginGen).checkSingle()

  fun findByBeginGenOpt(symbolId: Int, pointer: Int, beginGen: Int): Kernel? = kernels
    .filter { it.symbolId == symbolId && it.pointer == pointer && it.beginGen == beginGen }
    .checkSingleOrNone()

  fun contains(kernel: Kernel): Boolean = kernels.contains(kernel)

  fun getSingle(symbolId: Int, pointer: Int, beginGen: Int, endGen: Int): Kernel =
    filterByBeginGen(symbolId, pointer, beginGen).find { it.endGen == endGen }!!

  fun toKernels(): Set<Kernel> = kernels
}
