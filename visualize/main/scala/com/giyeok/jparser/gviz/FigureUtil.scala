package com.giyeok.jparser.gviz

import com.giyeok.gviz.figure._

import scala.jdk.CollectionConverters.SeqHasAsJava

object FigureUtil {
  def text(text: String, styleClass: String): TextFigure = new TextFigure(text, styleClass)

  def container(child: Figure, styleClass: String): ContainerFigure = new ContainerFigure(child, styleClass)

  def vert(children: Seq[Figure], styleClass: String): VertFlowFigure = new VertFlowFigure(children.asJava, styleClass)

  def horiz(children: Seq[Figure], styleClass: String): HorizFlowFigure = new HorizFlowFigure(children.asJava, styleClass)
}
