package com.giyeok.jparser.fast

// import com.giyeok.jparser.nparser.ParsingContext.Node

case class KernelTemplate(symbolId: Int, pointer: Int) extends Ordered[KernelTemplate] {
  override def compare(that: KernelTemplate): Int = if (symbolId == that.symbolId) pointer - that.pointer else symbolId - that.symbolId
}

//object KernelTemplate {
//  def fromNode(node: Node): KernelTemplate = KernelTemplate(node.kernel.symbolId, node.kernel.pointer)
//}
