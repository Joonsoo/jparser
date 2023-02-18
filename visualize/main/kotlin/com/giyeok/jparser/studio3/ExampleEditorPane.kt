package com.giyeok.jparser.studio3

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import javax.swing.JTextPane
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.StyleContext
import kotlin.time.Duration.Companion.milliseconds

class ExampleEditorPane : JTextPane() {
  val styleContext = StyleContext()
  val document = DefaultStyledDocument(styleContext)

//  doc.insertString(0, "Hello ", default)
//  doc.insertString(doc.length, "World!", red)
//
//  doc.setCharacterAttributes(3, 6, blue, true)

  val documentEventFlow = MutableSharedFlow<DocumentEvent>()

  init {
    this.setDocument(document)
    document.addDocumentListener(object : DocumentListener {
      override fun insertUpdate(e: DocumentEvent) {
        runBlocking { documentEventFlow.emit(e) }
      }

      override fun removeUpdate(e: DocumentEvent) {
        runBlocking { documentEventFlow.emit(e) }
      }

      override fun changedUpdate(e: DocumentEvent) {
        runBlocking { documentEventFlow.emit(e) }
      }
    })
  }

  val textFlow = DataUpdateEvent.processorFlow(documentEventFlow, 500.milliseconds) { text }

  override fun getScrollableTracksViewportWidth(): Boolean =
    getUI().getPreferredSize(this).width <= parent.size.width
}
