package com.giyeok.jparser.studio3

import com.giyeok.gviz.render.swing._
import com.giyeok.jparser.metalang3.generated.MetaLang3Ast
import com.giyeok.jparser.swingvis.FigureGen

import java.awt.{BasicStroke, Color, Font, GridLayout}
import javax.swing.text.{DefaultStyledDocument, SimpleAttributeSet, StyleConstants, StyleContext}
import javax.swing.{JFrame, JTextPane}
import scala.jdk.CollectionConverters.MapHasAsJava

object ParserStudio3 {
  def main(args: Array[String]): Unit = {
    val frame = new JFrame()
    frame.setLayout(new GridLayout(1, 2))

    val textPane = new JTextPane()
    val sc = new StyleContext()

    var default = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.black)
    default = sc.addAttribute(default, StyleConstants.FontFamily, Font.MONOSPACED)
    default = sc.addAttribute(default, StyleConstants.FontSize, 15)
    default = sc.addAttribute(default, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED)
    val red = sc.addAttribute(default, StyleConstants.Foreground, Color.red)
    var blue = sc.addAttribute(default, StyleConstants.Foreground, Color.blue)
    blue = sc.addAttribute(blue, StyleConstants.Underline, true)
    blue = sc.addAttribute(blue, StyleConstants.Background, Color.yellow)

    val doc = new DefaultStyledDocument(sc)
    doc.insertString(0, "Hello ", default)
    doc.insertString(doc.getLength, "World!", red)

    doc.setCharacterAttributes(3, 6, blue, true)

    textPane.setDocument(doc)

    frame.add(textPane)

    val figureGen = new FigureGen()

    val defaultFont = new Font(Font.MONOSPACED, Font.PLAIN, 15)
    val styles = new SwingFigureStyles(
      Map(
        "nonterminal" -> new TextStyle(defaultFont, Color.BLUE),
        "terminal" -> new TextStyle(defaultFont, Color.RED)
      ).asJava,
      Map(
        "bind" -> new ContainerStyle(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0,
          new BorderStyle(Color.black, 1.0, new BasicStroke()))
      ).asJava,
      Map().asJava,
      Map().asJava,
      Map().asJava,
      new TextStyle(defaultFont, Color.BLACK),
      new ContainerStyle(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null),
      new VertFlowStyle(0.0, Alignment.CENTER),
      new HorizFlowStyle(0.0, Alignment.LEADING),
      new GridStyle(0.0, 0.0),
    )
    val figure = figureGen.parseNodeFigure(MetaLang3Ast.parseAst("A = '0-9a-f'*").left.get.parseNode)
    frame.add(new FigureView(figure, styles))

    frame.setSize(800, 600)
    frame.setVisible(true)
  }
}
