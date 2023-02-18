package com.giyeok.jparser.studio2

import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.studio2.CodeEditor.CodeStyle
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.{StyleRange, StyledText}
import org.eclipse.swt.events.{KeyAdapter, KeyEvent, ModifyEvent}
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.widgets.Composite

class CodeEditor(val parent: Composite, val style: Int, val font: Font) {
  val styledText = new StyledText(parent, style)

  styledText.setFont(font)

  private val textSubject = PublishSubject.create[String]()

  def textObservable: Observable[String] = textSubject

  textSubject.onNext("")
  styledText.addModifyListener((e: ModifyEvent) => {
    textSubject.onNext(styledText.getText)
  })

  styledText.addKeyListener(new KeyAdapter {
    override def keyPressed(e: KeyEvent): Unit = {
      if ((e.stateMask & SWT.CTRL) != 0 && (e.keyCode == 'A' || e.keyCode == 'a')) {
        styledText.selectAll()
      }
    }
  })

  private def asyncRun(func: => Unit): Unit = parent.getDisplay.asyncExec(() => func)

  def clearStyles(): Unit = asyncRun {
    val styleRange = new StyleRange()
    val textLength = styledText.getText.length
    styleRange.start = 0
    styleRange.length = textLength
    styleRange.fontStyle = SWT.NONE
    styledText.setStyleRange(styleRange)
  }

  def setStyle(style: CodeStyle.Value, start: Int, end: Int): Unit = asyncRun {
    val textLength = styledText.getText.length
    if (textLength > 0) {
      val styleRange = new StyleRange()
      if (end <= textLength) {
        styleRange.start = start
        styleRange.length = end - start
      } else {
        styleRange.start = textLength - 1
        styleRange.length = 1
      }
      style match {
        case CodeStyle.ERROR =>
          styleRange.background = parent.getDisplay.getSystemColor(SWT.COLOR_RED)
        case CodeStyle.LHS =>
          styleRange.foreground = parent.getDisplay.getSystemColor(SWT.COLOR_BLUE)
        case CodeStyle.RHS =>
          styleRange.foreground = parent.getDisplay.getSystemColor(SWT.COLOR_DARK_BLUE)
        case CodeStyle.PROCESSOR_BLOCK =>
          styleRange.foreground = parent.getDisplay.getSystemColor(SWT.COLOR_GRAY)
        case CodeStyle.PEXPR =>
          styleRange.foreground = parent.getDisplay.getSystemColor(SWT.COLOR_GREEN)
        case CodeStyle.REF =>
          styleRange.foreground = parent.getDisplay.getSystemColor(SWT.COLOR_DARK_BLUE)
          styleRange.fontStyle = SWT.BOLD
        case CodeStyle.CLASS_NAME =>
          styleRange.foreground = parent.getDisplay.getSystemColor(SWT.COLOR_MAGENTA)
        case CodeStyle.PARAM_NAME =>
          styleRange.foreground = parent.getDisplay.getSystemColor(SWT.COLOR_DARK_GRAY)
        case CodeStyle.LITERAL =>
          styleRange.foreground = parent.getDisplay.getSystemColor(SWT.COLOR_BLACK)
          styleRange.fontStyle = SWT.BOLD
      }
      styledText.setStyleRange(styleRange)
    }
  }

  def setStyle(style: CodeStyle.Value, parseNode: Node): Unit = setStyle(style, parseNode.start, parseNode.end)
}

object CodeEditor {

  object CodeStyle extends Enumeration {
    val ERROR, LHS, RHS, PROCESSOR_BLOCK, PEXPR, REF, CLASS_NAME, PARAM_NAME, LITERAL = Value
  }

}
