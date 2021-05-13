package com.giyeok.jparser.proto

import com.google.protobuf.ProtocolStringList

import java.util.stream.Collectors
import scala.collection.mutable
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala, SeqHasAsJava}

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

  implicit class JavaMapToScalaCollection[K, V](javaMap: java.util.Map[K, V]) {
    def toScalaMap[K2, V2](keyMapper: java.util.Map.Entry[K, V] => K2, valueMapper: java.util.Map.Entry[K, V] => V2): Map[K2, V2] = {
      val keyMapperJavaFunc = toJavaFunc(keyMapper)
      val valueMapperJavaFunc = toJavaFunc(valueMapper)
      javaMap.entrySet().stream().collect(Collectors.toMap[java.util.Map.Entry[K, V], K2, V2](keyMapperJavaFunc, valueMapperJavaFunc)).asScala.toMap
    }
  }

  def toJavaFunc[A, B](func: A => B): java.util.function.Function[A, B] = new java.util.function.Function[A, B] {
    override def apply(v: A): B = func(v)
  }

  def toJavaConsumer[A](func: A => Unit): java.util.function.Consumer[A] = new java.util.function.Consumer[A] {
    override def accept(v: A): Unit = func(v)
  }

  implicit class JavaListToScalaCollection[T](javaList: java.util.List[T]) {
    def toScalaBuffer[T2](mapper: T => T2): mutable.Buffer[T2] = {
      javaList.parallelStream().map(toJavaFunc(mapper)).collect(Collectors.toList[T2]).asScala
    }

    def toScalaList[T2](mapper: T => T2): List[T2] =
      toScalaBuffer(mapper).toList

    def toScalaMap[K, V](keyMapper: T => K, valueMapper: T => V): Map[K, V] = {
      val keyMapperJavaFunc = toJavaFunc(keyMapper)
      val valueMapperJavaFunc = toJavaFunc(valueMapper)
      javaList.parallelStream().collect(Collectors.toMap[T, K, V](keyMapperJavaFunc, valueMapperJavaFunc)).asScala.toMap
    }

    def toScalaSet[V](valueMapper: T => V): Set[V] =
      toScalaBuffer(valueMapper).toSet
  }
}
