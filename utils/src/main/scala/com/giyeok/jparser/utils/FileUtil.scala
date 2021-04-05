package com.giyeok.jparser.utils

import java.io._
import scala.io.Source
import scala.util.Using

object FileUtil {
  def readFile(path: String): String = readFile(new File(path))

  def readFile(file: File): String = {
    val source = Source.fromFile(file)
    try source.mkString finally source.close()
  }

  def readFileBytes(path: String): Array[Byte] = readFileBytes(new File(path))

  def readFileBytes(file: File): Array[Byte] = Using(new BufferedInputStream(new FileInputStream(file))) {
    readFileBytes
  }.get

  def readFileBytes(input: InputStream): Array[Byte] = {
    val outputStream = new ByteArrayOutputStream()

    var reading = 1
    val result = new Array[Byte](1000)
    while (reading > 0) {
      reading = input.read(result, 0, 1000)
      if (reading > 0) {
        outputStream.write(result, 0, reading)
      }
    }
    outputStream.toByteArray
  }

  def writeFile(dest: File, text: String): Unit = {
    val writer = new BufferedWriter(new FileWriter(dest))
    writer.write(text)
    writer.close()
  }

  def writeFile(dest: File, bytes: Array[Byte]): Unit = {
    val writer = new BufferedOutputStream(new FileOutputStream(dest))
    writer.write(bytes)
    writer.close()
  }
}
