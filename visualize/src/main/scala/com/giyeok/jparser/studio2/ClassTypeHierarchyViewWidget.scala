package com.giyeok.jparser.studio2

import com.giyeok.jparser.metalang3a.{ClassRelationCollector, Type}
import com.giyeok.jparser.studio2.Utils.readableType
import org.eclipse.swt.widgets.{Composite, Text}

class ClassTypeHierarchyViewWidget(parent: Composite, style: Int) {
  val panel = new Text(parent, style)

  panel.setText("ClassTypeHierarchyViewWidget")

  def setClassInfo(classRelations: ClassRelationCollector, classParamTypes: Map[String, List[(String, Type)]]): Unit = {
    val description = classRelations.toHierarchy.allTypes.toList.sortBy(_._1).map { typ =>
      val cls = typ._2
      classParamTypes get typ._1 match {
        case Some(params) =>
          s"${cls.className} params: (${params.map(param => s"${param._1}: ${readableType(param._2)}").mkString(",")}) supers: ${cls.superclasses.toList.sorted} subs: ${cls.subclasses.toList.sorted}"
        case None =>
          s"${cls.className} supers: ${cls.superclasses.toList.sorted} subs: ${cls.subclasses.toList.sorted}"
      }
    }.mkString("\n")
    panel.getDisplay.asyncExec { () =>
      panel.setText(description)
    }
  }

  def invalidate(): Unit = panel.getDisplay.asyncExec { () =>
    panel.setText("")
  }
}
