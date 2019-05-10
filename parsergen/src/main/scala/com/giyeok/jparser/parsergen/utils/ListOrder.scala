package com.giyeok.jparser.parsergen.utils

object ListOrder {
    def compare[T <: Ordered[T]](a: List[T], b: List[T]): Int =
        a.zip(b).find(p => p._1 != p._2) match {
            case Some(firstDiff) => firstDiff._1.compare(firstDiff._2)
            case None => a.size - b.size
        }
}
