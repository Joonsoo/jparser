package com.giyeok.jparser.parsergen.deprecated.utils

class BiDirectionalMap[K, V](val byKey: Map[K, V], val byVal: Map[V, K]) {
    def size = byKey.size ensuring byKey.size == byVal.size

    def add(key: K, value: V) = new BiDirectionalMap(byKey + (key -> value), byVal + (value -> key))

    def +(pair: (K, V)) = new BiDirectionalMap(byKey + pair, byVal + (pair._2 -> pair._1))
}

object BiDirectionalMap {
    def apply[K, V]() = new BiDirectionalMap[K, V](Map(), Map())
}
