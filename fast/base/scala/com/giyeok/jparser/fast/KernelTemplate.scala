package com.giyeok.jparser.fast

case class KernelTemplate(symbolId: Int, pointer: Int) extends Ordered[KernelTemplate] {
  override def compare(that: KernelTemplate): Int =
    if (symbolId != that.symbolId) symbolId - that.symbolId else pointer - that.pointer
}
