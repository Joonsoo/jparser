package com.giyeok.jparser.ktglue

import com.giyeok.jparser.ktlib.Kernel
import com.giyeok.jparser.ktlib.KernelSet

fun <T> scala.collection.immutable.List<T>.toKtList(): List<T> =
  List<T>(this.size()) { idx -> this.apply(idx) }

fun <T> scala.collection.immutable.Seq<T>.toKtList(): List<T> =
  List<T>(this.size()) { idx -> this.apply(idx) }

fun <T> scala.collection.immutable.Set<T>.toKtSet(): Set<T> =
  this.toList().toKtList().toSet()

fun scala.collection.immutable.Set<com.giyeok.jparser.nparser.Kernel>.toKtKernelSet(): KernelSet {
  val iter = this.iterator()
  val set = List<Kernel>(this.size()) {
    val k = iter.next()
    Kernel(k.symbolId(), k.pointer(), k.beginGen(), k.endGen())
  }.toSet()
  return KernelSet(set)
}
