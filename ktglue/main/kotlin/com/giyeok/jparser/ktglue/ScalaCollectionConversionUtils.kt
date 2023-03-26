package com.giyeok.jparser.ktglue

fun <T> scala.collection.immutable.List<T>.toKtList(): List<T> =
  List<T>(this.size()) { idx -> this.apply(idx) }

fun <T> scala.collection.immutable.Seq<T>.toKtList(): List<T> =
  List<T>(this.size()) { idx -> this.apply(idx) }

fun <T> scala.collection.immutable.Set<T>.toKtSet(): Set<T> =
  this.toList().toKtList().toSet()
