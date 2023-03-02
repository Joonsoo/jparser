package com.giyeok.jparser.utils

import scala.collection.mutable

case class Memoize[T, U]() {
  private val _memoMap = mutable.Map[T, U]()

  def memoMap: Map[T, U] = _memoMap.toMap

  def apply(param: T)(func: => U): U = _memoMap get param match {
    case Some(saved) => saved
    case None =>
      val newValue = func
      _memoMap(param) = newValue
      newValue
  }

  def removeIf(pred: T => Boolean): Unit = {
    val removingKeys = _memoMap.keySet.filter(pred)
    _memoMap --= removingKeys
  }
}
