package com.giyeok.jparser.mgroup

class MilestoneGroupParserPrinter(val parserData: MilestoneGroupParserData) {
  def printMilestoneGroupPath(path: MilestoneGroupPath): Unit = {
    println(path.prettyString)

    case class MilestoneGroupText(lines: List[String], title: String) {
      val maxLength: Int = Math.max(lines.maxBy(_.length).length, title.length)

      def lineOf(idx: Int): String = {
        val line = if (idx < lines.size) lines(idx) else ""
        line + (" " * (maxLength - line.length))
      }

      def titleLine: String = title + (" " * (maxLength - title.length))
    }

    def pathToText(path: MilestoneGroupPath): List[MilestoneGroupText] = {
      val thisGroup = MilestoneGroupText(parserData.milestoneGroups(path.milestoneGroup).milestones.map { milestone =>
        s"${milestone.symbolId} ${milestone.pointer} [${milestone.acceptConditionSlot}]${path.acceptConditionSlots(milestone.acceptConditionSlot)}"
      }, s"[[ ${path.gen} ]]")
      path.parent match {
        case Some(parent) => pathToText(parent) :+ thisGroup
        case None => List(thisGroup)
      }
    }

    val pathTexts = pathToText(path)
    val totalLines = pathTexts.maxBy(_.lines.size).lines.size
    (0 until totalLines).foreach { lineIdx =>
      pathTexts.foreach { pt =>
        print(pt.lineOf(lineIdx) + " ")
      }
      println()
    }
    pathTexts.foreach { pt =>
      print(pt.titleLine + " ")
    }
    println()
  }

  def printMilestoneGroupPaths(paths: List[MilestoneGroupPath]): Unit = {
    if (paths.isEmpty) {
      println("** Empty")
    }
    paths.foreach(printMilestoneGroupPath)
  }
}
