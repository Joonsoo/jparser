package com.giyeok.jparser.studio2

import com.giyeok.jparser.metalang3.Type
import org.eclipse.swt.layout.{FormAttachment, FormData}
import org.eclipse.swt.widgets.Control

object Utils {
  def setMainAndBottomLayout(main: Control, bottom: Control): Unit = {
    main.setLayoutData({
      val formData = new FormData()
      formData.top = new FormAttachment(0)
      formData.bottom = new FormAttachment(bottom)
      formData.left = new FormAttachment(0)
      formData.right = new FormAttachment(100)
      formData
    })
    bottom.setLayoutData({
      val formData = new FormData()
      formData.bottom = new FormAttachment(100)
      formData.left = new FormAttachment(0)
      formData.right = new FormAttachment(100)
      formData
    })
  }

  def readableType(typ: Type): String = typ match {
    case Type.NodeType => "node"
    case Type.ClassType(name) => name
    case Type.OptionalOf(typ) => s"${readableType(typ)}?"
    case Type.ArrayOf(elemType) => s"[${readableType(elemType)}]"
    case Type.UnionOf(types) => types.map(readableType).toList.sorted.mkString("|")
    case Type.EnumType(enumName) => s"%$enumName"
    case Type.UnspecifiedEnumType(uniqueId) => s"%?$uniqueId"
    case Type.NullType => "null"
    case Type.AnyType => "any"
    case Type.NothingType => "nothing"
    case Type.BoolType => "bool"
    case Type.CharType => "char"
    case Type.StringType => "string"
  }
}
