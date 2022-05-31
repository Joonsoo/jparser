package com.giyeok.jparser.studio3

import com.giyeok.gviz.render.swing.*
import com.giyeok.jparser.metalang3.generated.MetaLang3Ast
import com.giyeok.jparser.swingvis.FigureGen
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GridLayout
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ParserStudio3 {
  val figureGen = FigureGen(false, true)

  val defaultFont = Font(Font.MONOSPACED, Font.PLAIN, 15)
  val boldFont = Font(Font.MONOSPACED, Font.BOLD, 15)
  val styles = SwingFigureStyles(
    mapOf(
      "nonterminal" to TextStyle(boldFont, Color.BLUE),
      "terminal" to TextStyle(defaultFont, Color.RED),
    ),
    mapOf(
      "bind" to ContainerStyle(
        0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0,
        BorderStyle(Color.BLACK, 1.0, BasicStroke())
      ),
      "input" to ContainerStyle(
        0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0,
        BorderStyle(Color.RED, 1.0, BasicStroke())
      ),
    ),
    mapOf(),
    mapOf(
      "sequence" to HorizFlowStyle(1.0, Alignment.LEADING),
    ),
    mapOf(),
    TextStyle(defaultFont, Color.BLACK),
    ContainerStyle(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null),
    VertFlowStyle(0.0, Alignment.CENTER),
    HorizFlowStyle(0.0, Alignment.LEADING),
    GridStyle(0.0, 0.0)
  )

  fun run() {
    val frame = JFrame()
    frame.layout = GridLayout(1, 2)

    val textPane = JTextPane()
    val sc = StyleContext()

    var default =
      sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.black)
    default = sc.addAttribute(default, StyleConstants.FontFamily, Font.MONOSPACED)
    default = sc.addAttribute(default, StyleConstants.FontSize, 15)
    default = sc.addAttribute(default, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED)
    val red = sc.addAttribute(default, StyleConstants.Foreground, Color.red)
    var blue = sc.addAttribute(default, StyleConstants.Foreground, Color.blue)
    blue = sc.addAttribute(blue, StyleConstants.Underline, true)
    blue = sc.addAttribute(blue, StyleConstants.Background, Color.yellow)

    val doc = DefaultStyledDocument(sc)
    doc.insertString(0, "Hello ", default)
    doc.insertString(doc.length, "World!", red)

    doc.setCharacterAttributes(3, 6, blue, true)

    textPane.document = doc

    frame.add(textPane)

    val figureView = JScrollPane()

    val figure =
      figureGen.parseNodeFigure(MetaLang3Ast.parseAst("A = '0-9a-f'*").left().get().parseNode())
    figureView.setViewportView(FigureView(figure, styles))

    frame.add(figureView)

    val docFlow = callbackFlow {
      doc.addDocumentListener(object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent) {
          runBlocking { send(e) }
        }

        override fun removeUpdate(e: DocumentEvent) {
          runBlocking { send(e) }
        }

        override fun changedUpdate(e: DocumentEvent) {
          runBlocking { send(e) }
        }
      })
      awaitClose { }
    }

    val parseFlow = docFlow.debounce(500).map {
      val text = doc.getText(0, doc.length)
      MetaLang3Ast.parseAst(text)
    }

    CoroutineScope(Executors.newFixedThreadPool(1).asCoroutineDispatcher()).async {
      parseFlow.collect { parseResult ->
        println(parseResult)
        if (parseResult.isLeft) {
          val parsed = parseResult.left().get()
          val figure = figureGen.parseNodeFigure(parsed.parseNode())
          CoroutineScope(Dispatchers.Main).launch {
            figureView.setViewportView(FigureView(figure, styles))
          }
        }
      }
    }

    frame.setSize(800, 600)
    frame.isVisible = true
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      ParserStudio3().run()
    }
  }
}
