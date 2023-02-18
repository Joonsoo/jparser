package com.giyeok.jparser.studio2

import com.giyeok.jparser.metalang3.Type
import com.giyeok.jparser.studio2.Utils.readableType
import org.eclipse.swt.widgets.{Composite, Text}

class NonterminalTypeViewWidget(parent: Composite, style: Int) {
  val panel = new Text(parent, style)

  panel.setText("Nonterminal types")

  def setNonterminalTypes(nonterminalTypes: Map[String, Type]): Unit = panel.getDisplay.asyncExec { () =>
    panel.setText(nonterminalTypes.toList.sortBy(_._1)
      .map(pair => s"${pair._1}: ${readableType(pair._2)}")
      .mkString("\n"))
  }

  def invalidate(): Unit = panel.getDisplay.asyncExec { () =>
    panel.setText("")
  }
}
