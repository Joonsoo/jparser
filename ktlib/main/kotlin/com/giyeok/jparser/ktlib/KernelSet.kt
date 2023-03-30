package com.giyeok.jparser.ktlib

import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto

// TODO 성능 개선 포인트들이 있을 듯
// TODO AstifierUtil과 Kernel을 주고받을 필요는 없음. 성능 개선 포인트
class KernelSet(val kernelsBySymbol: Map<Int, Set<KernelGen>>) {
  class KernelGen(val pointer: Int, val beginGen: Int, val endGen: Int)

  class Builder {
    private val mapBuilder = mutableMapOf<Int, MutableSet<KernelGen>>()

    fun addKernel(symbolId: Int, pointer: Int, beginGen: Int, endGen: Int) {
      mapBuilder.getOrPut(symbolId) { mutableSetOf() }
        .add(KernelGen(pointer, beginGen, endGen))
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
      KernelSet(mapBuilder)
  }

//  fun filter(pred: (Kernel) -> Boolean) = kernels.filter(pred)

  fun filterByBeginGen(symbolId: Int, pointer: Int, beginGen: Int): Set<Kernel> =
    kernelsBySymbol[symbolId]
      ?.filter { it.pointer == pointer && it.beginGen == beginGen }
      ?.map { Kernel(symbolId, pointer, beginGen, it.endGen) }
      ?.toSet() ?: setOf()

  fun findByBeginGen(symbolId: Int, pointer: Int, beginGen: Int): Kernel =
    filterByBeginGen(symbolId, pointer, beginGen).checkSingle()

  fun findByBeginGenOpt(symbolId: Int, pointer: Int, beginGen: Int): Kernel? =
    filterByBeginGen(symbolId, pointer, beginGen)
      .checkSingleOrNone()

  fun contains(kernel: Kernel): Boolean =
    filterByBeginGen(kernel.symbolId, kernel.pointer, kernel.beginGen)
      .any { it.endGen == kernel.endGen }

  fun getSingle(symbolId: Int, pointer: Int, beginGen: Int, endGen: Int): Kernel =
    filterByBeginGen(symbolId, pointer, beginGen).find { it.endGen == endGen }!!

  fun toKernelsSet(): Set<Kernel> =
    kernelsBySymbol.flatMap { (symbolId, gens) ->
      gens.map { gen ->
        Kernel(symbolId, gen.pointer, gen.beginGen, gen.endGen)
      }
    }.toSet()
}
