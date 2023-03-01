package com.giyeok.jparser.ktlib

import com.giyeok.jparser.nparser.Kernel

class KernelSet(val kernels: Set<Kernel>) {
  fun filter(pred: (Kernel) -> Boolean) = kernels.filter(pred)

  fun filterByBeginGen(symbolId: Int, pointer: Int, beginGen: Int): Set<Kernel> = kernels
    .filter { it.symbolId() == symbolId && it.pointer() == pointer && it.beginGen() == beginGen }
    .toSet()

  fun findByBeginGen(symbolId: Int, pointer: Int, beginGen: Int): Kernel =
    filterByBeginGen(symbolId, pointer, beginGen).checkSingle()

  fun findByBeginGenOpt(symbolId: Int, pointer: Int, beginGen: Int): Kernel? = kernels
    .filter { it.symbolId() == symbolId && it.pointer() == pointer && it.beginGen() == beginGen }
    .checkSingleOrNone()

  fun contains(kernel: Kernel): Boolean = kernels.contains(kernel)
}

fun Collection<Kernel>.checkSingle(): Kernel {
  check(this.size == 1) {
    "Kernel size was expected to be 1, but it was ${this.size}"
  }
  return this.first()
}

fun Collection<Kernel>.checkSingleOrNone(): Kernel? {
  check(this.size <= 1)
  return this.firstOrNull()
}

fun getSequenceElems(
  history: List<KernelSet>,
  sequenceId: Int,
  elems: List<Int>,
  beginGen: Int,
  endGen: Int
): List<Pair<Int, Int>> {
  val lastElem = history[endGen].findByBeginGen(sequenceId, elems.size, beginGen)
  val list = mutableListOf(lastElem)
  var currGen = lastElem.endGen()
  for (pointer in elems.size - 1 downTo 0) {
    val prevElem = history[currGen].filterByBeginGen(sequenceId, pointer, beginGen)
      .filter { history[currGen].contains(Kernel(elems[pointer], 1, it.endGen(), currGen)) }
      .checkSingle()
    list.add(prevElem)
    currGen = prevElem.endGen()
  }
  return (elems.size - 1 downTo 0).map { i -> list[i + 1].endGen() to list[i].endGen() }
}

fun hasSingleTrue(vararg booleans: Boolean): Boolean =
  booleans.count { it } == 1

fun unrollRepeat0(
  history: List<KernelSet>,
  symbolId: Int,
  itemSymId: Int,
  baseSeq: Int,
  repeatSeq: Int,
  beginGen: Int,
  endGen: Int
): List<Pair<Int, Int>> {
  val base = history[endGen].findByBeginGenOpt(baseSeq, 0, beginGen)
  val repeat = history[endGen].findByBeginGenOpt(repeatSeq, 2, beginGen)
  check(hasSingleTrue(base != null, repeat != null))
  return if (base != null) {
    listOf()
  } else {
    val seq = getSequenceElems(history, repeatSeq, listOf(symbolId, itemSymId), beginGen, endGen)
    val repeating = seq.first()
    val item = seq[1]
    unrollRepeat0(
      history,
      symbolId,
      itemSymId,
      baseSeq,
      repeatSeq,
      repeating.first,
      repeating.second
    ) + item
  }
}

fun unrollRepeat1(
  history: List<KernelSet>,
  symbolId: Int,
  itemSymId: Int,
  baseSeq: Int,
  repeatSeq: Int,
  beginGen: Int,
  endGen: Int
): List<Pair<Int, Int>> {
  val base = history[endGen].findByBeginGenOpt(baseSeq, 1, beginGen)
  val repeat = history[endGen].findByBeginGenOpt(repeatSeq, 2, beginGen)
  check(hasSingleTrue(base != null, repeat != null))
  return if (base != null) {
    val baseItem = history[endGen].findByBeginGen(itemSymId, 1, beginGen)
    listOf(baseItem.beginGen() to baseItem.endGen())
  } else {
    val seq = getSequenceElems(history, repeatSeq, listOf(symbolId, itemSymId), beginGen, endGen)
    println(seq)
    val repeating = seq.first()
    val item = seq[1]
    unrollRepeat1(
      history,
      symbolId,
      itemSymId,
      baseSeq,
      repeatSeq,
      repeating.first,
      repeating.second
    ) + item
  }
}
