package com.giyeok.jparser.studio3

import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.metalang3.generated.MetaLang3Ast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.awt.Color
import java.awt.Font
import javax.swing.JTextPane
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import kotlin.time.Duration.Companion.milliseconds

class GrammarEditorPane(val workerDispatcher: CoroutineDispatcher) : JTextPane() {
  val styleContext = StyleContext()

  init {
    var default =
      styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.black)
    default = styleContext.addAttribute(default, StyleConstants.FontFamily, Font.MONOSPACED)
    default = styleContext.addAttribute(default, StyleConstants.FontSize, 15)
    default =
      styleContext.addAttribute(default, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED)
    val red = styleContext.addAttribute(default, StyleConstants.Foreground, Color.red)
    var blue = styleContext.addAttribute(default, StyleConstants.Foreground, Color.blue)
    blue = styleContext.addAttribute(blue, StyleConstants.Underline, true)
    blue = styleContext.addAttribute(blue, StyleConstants.Background, Color.yellow)
  }

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

  suspend fun grammarText(): String = text

  val parsedAstFlow: Flow<DataUpdateEvent<MetaLang3Ast.Grammar>> = runBlocking {
    DataUpdateEvent.processorFlow(documentEventFlow, 500.milliseconds) {
      val grammarText = grammarText()
      withContext(currentCoroutineContext() + workerDispatcher) {
        MetaLanguage3.parseGrammar(grammarText)
      }
    }.stateIn(CoroutineScope(workerDispatcher))
  }

  val grammarFlow: StateFlow<DataUpdateEvent<MetaLanguage3.ProcessedGrammar>> = runBlocking {
    DataUpdateEvent.processorFlow(parsedAstFlow, 500.milliseconds) { ast ->
      (ast as? DataUpdateEvent.NewDataAvailable)?.let {
        withContext(currentCoroutineContext() + workerDispatcher) {
          MetaLanguage3.analyzeGrammar(ast.data, "Generated")
        }
      } ?: throw IllegalStateException("Grammar not ready: $ast")
    }.stateIn(CoroutineScope(workerDispatcher))
  }

  override fun getScrollableTracksViewportWidth(): Boolean =
    getUI().getPreferredSize(this).width <= parent.size.width
}
