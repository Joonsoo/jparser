package com.giyeok.jparser.utils

import scala.collection.mutable

case class Memoize[T, U]()(implicit ev$1: T => Equals) {
  private val _memoMap = mutable.Map[T, U]()

  def memoMap: Map[T, U] = _memoMap.toMap

  def apply(param: T)(func: => U): U = _memoMap get param match {
    case Some(saved) => saved
    case None =>
      val newValue = func
      _memoMap(param) = newValue
      newValue
  }
}
