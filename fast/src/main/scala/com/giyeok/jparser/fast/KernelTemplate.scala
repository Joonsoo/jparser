package com.giyeok.jparser.fast

case class KernelTemplate(symbolId: Int, pointer: Int) extends Ordered[KernelTemplate] {
  override def compare(that: KernelTemplate): Int = if (symbolId == that.symbolId) pointer - that.pointer else symbolId - that.symbolId
}
