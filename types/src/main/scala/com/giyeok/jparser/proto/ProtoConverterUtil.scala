package com.giyeok.jparser.proto

import com.google.protobuf.ProtocolStringList

import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

object ProtoConverterUtil {
  implicit def toJavaIntegerList(lst: List[Int]): java.util.List[Integer] =
    lst.map(i => i: java.lang.Integer).asJava

  implicit def toScalaIntList(lst: java.util.List[Integer]): List[Int] =
    lst.asScala.toList.map(i => i: Int)

  implicit def toJavaStringList(lst: List[Char]): java.util.List[String] =
    lst.map(_.toString).asJava

  implicit def toScalaCharList(lst: java.util.List[String]): List[Char] =
    lst.asScala.toList.map(i => i.charAt(0) ensuring i.length == 1)

  implicit def toScalaStringList(lst: ProtocolStringList): List[String] =
    lst.asScala.toList
}
