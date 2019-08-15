package com.giyeok.jparser.utils

import scala.collection.mutable

case class Memoize[T, U]()(implicit ev$1: T => Equals) {
    private val memo = mutable.Map[T, U]()

    def apply(param: T)(func: => U): U = memo get param match {
        case Some(saved) => saved
        case None =>
            val newValue = func
            memo(param) = newValue
            newValue
    }
}
