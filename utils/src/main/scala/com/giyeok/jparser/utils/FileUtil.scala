package com.giyeok.jparser.utils

import java.io.{BufferedWriter, File, FileWriter}
import scala.io.Source

object FileUtil {
  def readFile(path: String): String = readFile(new File(path))

  def readFile(file: File): String = {
    val source = Source.fromFile(file)
    try source.mkString finally source.close()
  }

  def writeFile(dest: File, text: String): Unit = {
    val writer = new BufferedWriter(new FileWriter(dest))
    writer.write(text)
    writer.close()
  }
}
