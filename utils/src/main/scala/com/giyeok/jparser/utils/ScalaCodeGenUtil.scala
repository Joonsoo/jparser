package com.giyeok.jparser.utils

object ScalaCodeGenUtil {
  def isPrintableChar(char: Char): Boolean = {
    val block = Character.UnicodeBlock.of(char)
    (!Character.isISOControl(char)) && block != null && block != Character.UnicodeBlock.SPECIALS
  }

  def javaChar(char: Char): String = char match {
    case '\n' => "\\n"
    case '\r' => "\\r"
    case '\t' => "\\t"
    case '\\' => "\\\\"
    case '\'' => "\\'"
    case c if !isPrintableChar(c) && c.toInt < 65536 =>
      val c1 = (c.toInt >> 8) % 256
      val c2 = c.toInt % 256
      val hexChars = "0123456789abcdef"
      s"\\u${hexChars(c1 >> 4)}${hexChars(c1 & 15)}${hexChars(c2 >> 4)}${hexChars(c2 & 15)}"
    case c => c.toString
  }

  def addIndent(indentDepth: Int, line: String): String = {
    val indent = "  " * indentDepth
    s"$indent$line"
  }

  sealed trait ScalaCodeBlobNode {
    def generate(indent: Int): String
  }

  case class ScalaFile(packageName: String, imports: Set[String], body: List[ScalaCodeBlobNode]) {
    def generate(): String = {
      val builder = new StringBuilder
      if (packageName.nonEmpty) {
        builder.append(s"package $packageName\n\n")
      }
      if (imports.nonEmpty) {
        imports.foreach(imp => builder.append(s"import $imp\n"))
        builder.append("\n")
      }
      builder.append(body.map(_.generate(0)).mkString("\n"))
      builder.toString()
    }
  }

  case class ClassDef(className: String, body: List[ScalaCodeBlobNode]) extends ScalaCodeBlobNode {
    override def generate(indent: Int): String =
      addIndent(indent, s"class $className {") +
        body.map(_.generate(indent + 1)).mkString("\n\n") +
        addIndent(indent, "}")
  }

  case class ObjectDef(objectName: String, body: List[ScalaCodeBlobNode]) extends ScalaCodeBlobNode {
    override def generate(indent: Int): String =
      addIndent(indent, s"class $objectName {\n") +
        body.map(_.generate(indent + 1)).mkString("\n\n") +
        addIndent(indent, "}")
  }

  case class FuncDef(funcName: String, args: List[(String, String)], body: ScalaCodeBlobNode) extends ScalaCodeBlobNode {
    override def generate(indent: Int): String =
      addIndent(indent, s"def $funcName =\n") + body.generate(indent + 1)
  }

  case class BlockBlob(body: List[ScalaCodeBlobNode]) extends ScalaCodeBlobNode {
    override def generate(indent: Int): String =
      addIndent(indent, "{") + body.map(_.generate(indent + 1) + "\n").mkString("") + addIndent(indent, "}")
  }

  case class TextBlob(text: String) extends ScalaCodeBlobNode {
    override def generate(indent: Int): String = {
      val lines = text.split('\n')
      lines.map(addIndent(indent, _)).mkString("\n")
    }
  }

  def isOnelineCode(code: String) = !code.contains("\n")

  def isOnelineBlob(blob: ScalaCodeBlobNode) = isOnelineCode(blob.generate(0))

  case class ArgsCall(destName: String, args: List[ScalaCodeBlobNode]) extends ScalaCodeBlobNode {
    override def generate(indent: Int): String = {
      val isOneline = args.size <= 3 && args.forall(isOnelineBlob)
      if (isOneline) {
        addIndent(indent, s"$destName(${args.map(_.generate(0)).mkString(",")})")
      } else {
        addIndent(indent, destName + "(\n") +
          args.map(_.generate(indent + 1)).mkString(",\n") + ")"
      }
    }
  }

  case class PairBlob(first: ScalaCodeBlobNode, second: ScalaCodeBlobNode) extends ScalaCodeBlobNode {
    override def generate(indent: Int): String = {
      val firstCode = first.generate(0)
      val secondCode = second.generate(0)
      if (isOnelineCode(firstCode) && isOnelineCode(secondCode)) {
        addIndent(indent, s"($firstCode, $secondCode)")
      } else {
        addIndent(indent, "(") +
          first.generate(indent + 1) + ",\n" +
          second.generate(indent + 1) + "\n" +
          addIndent(indent, ")")
      }
    }
  }

  case class ArrowBlob(left: ScalaCodeBlobNode, right: ScalaCodeBlobNode) extends ScalaCodeBlobNode {
    override def generate(indent: Int): String = {
      val leftCode = left.generate(0)
      val rightCode = right.generate(0)
      if (!leftCode.contains("\n") && !rightCode.contains("\n")) {
        addIndent(indent, s"$leftCode -> $rightCode")
      } else {
        left.generate(indent) + " ->\n" +
          right.generate(indent + 1)
      }
    }
  }

  case class CharBlob(char: Char) extends ScalaCodeBlobNode {
    override def generate(indent: Int): String =
      addIndent(indent, s"'${javaChar(char)}'")
  }

  case class ValBlob(valName: String, body: ScalaCodeBlobNode) extends ScalaCodeBlobNode {
    override def generate(indent: Int): String = {
      if (isOnelineBlob(body)) addIndent(indent, s"val $valName = ${body.generate(0)}")
      else addIndent(indent, s"val $valName =\n") + body.generate(indent + 1)
    }
  }

}
