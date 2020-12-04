package com.giyeok.jparser.studio2

import com.giyeok.jparser.examples.metalang3.SimpleExamples
import com.giyeok.jparser.studio2.GrammarDefEditor.UpdateEvent
import com.giyeok.jparser.studio2.Utils.setMainAndBottomLayout
import com.giyeok.jparser.visualize.utils.{HorizontalResizableSplittedComposite, VerticalResizableSplittedComposite}
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.{FillLayout, FormLayout}
import org.eclipse.swt.widgets.{Button, Composite, Display, Shell}

import java.util.concurrent.{ExecutorService, Executors}

class ParserStudio2(private val shell: Shell, parseExecutor: ExecutorService) {
  private val font = new Font(shell.getDisplay, "JetBrains Mono", 12, SWT.NONE)
  private val scheduler: Scheduler = Schedulers.from(parseExecutor)

  // 왼쪽 패널
  private var grammarDefEditor: GrammarDefEditor = null
  private var openGrammarExamplesButton: Button = null
  private var classTypeHierarchyViewWidget: ClassTypeHierarchyViewWidget = null
  private var nonterminalTypeViewWidget: NonterminalTypeViewWidget = null

  // 오른쪽 패널
  private var rightPanel: RightPanel = null

  def start(): Unit = {
    // 루트 패널
    val rootPanel = new VerticalResizableSplittedComposite(shell, SWT.NONE)

    // 루트 -> 왼쪽 패널
    val leftPanel = new HorizontalResizableSplittedComposite(rootPanel.leftPanel, SWT.NONE, 60)
    initLeftPanel(leftPanel)

    rightPanel = new RightPanel(rootPanel.rightPanel, SWT.NONE, font, scheduler, grammarDefEditor.grammarUpdateObs)

    grammarDefEditor.editor.styledText.setText(SimpleExamples.ex4.grammar)
    grammarDefEditor.grammarUpdateObs.subscribe { update: UpdateEvent =>
      update match {
        case GrammarDefEditor.GrammarChanged =>
          classTypeHierarchyViewWidget.invalidate()
          nonterminalTypeViewWidget.invalidate()
        case GrammarDefEditor.GrammarDefError(parsingError) => // TODO 오류 메시지 표시
          println(parsingError.msg)
        case GrammarDefEditor.GrammarProcessError(errors) => // TODO 오류 메시지 표시
        case GrammarDefEditor.GrammarProcessed(processedGrammar) =>
          classTypeHierarchyViewWidget.setClassInfo(processedGrammar.classRelations, processedGrammar.classParamTypes)
          nonterminalTypeViewWidget.setNonterminalTypes(processedGrammar.nonterminalTypes)
        case GrammarDefEditor.GrammarGenerated(ngrammar) => // do nothing, RightPanel에서 처리할 것
        case GrammarDefEditor.GrammarDefParsed(_) => // do nothing, GrammarDefEditor에서 처리할 것
      }
    }
  }

  private def initLeftPanel(leftPanel: HorizontalResizableSplittedComposite): Unit = {
    // 루트 -> 왼쪽 -> 상단 문법 정의 패널. 상단에 definition text editor, 하단에 "Open Example" 버튼
    val grammarDefPanel = new Composite(leftPanel.upperPanel, SWT.NONE)
    grammarDefPanel.setLayout(new FormLayout)

    grammarDefEditor = new GrammarDefEditor(grammarDefPanel, SWT.V_SCROLL | SWT.H_SCROLL, font, scheduler)
    openGrammarExamplesButton = new Button(grammarDefPanel, SWT.NONE)
    openGrammarExamplesButton.setText("Open Example")
    setMainAndBottomLayout(grammarDefEditor.editor.styledText, openGrammarExamplesButton)

    // 루트 -> 왼쪽 -> 하단 문법 정보 표시
    val processedGrammarInfoPanel = new Composite(leftPanel.lowerPanel, SWT.NONE)
    processedGrammarInfoPanel.setLayout(new FillLayout)
    // TODO 루트 -> 왼쪽 -> 하단 문법 정보 -> 왼쪽 클래스 타입 트리
    classTypeHierarchyViewWidget = new ClassTypeHierarchyViewWidget(processedGrammarInfoPanel, SWT.V_SCROLL | SWT.H_SCROLL)
    classTypeHierarchyViewWidget.panel.setFont(font)
    // TODO 루트 -> 왼쪽 -> 하단 문법 정보 -> 오른쪽 넌터미널 타입
    nonterminalTypeViewWidget = new NonterminalTypeViewWidget(processedGrammarInfoPanel, SWT.V_SCROLL | SWT.H_SCROLL)
    nonterminalTypeViewWidget.panel.setFont(font)
  }

  private def initRightPanel(rightPanel: HorizontalResizableSplittedComposite): Unit = {
  }
}

object ParserStudio2 {
  def main(args: Array[String]): Unit = {
    val display = new Display()
    val shell = new Shell(display)
    shell.setText("Parser Studio 2")
    shell.setLayout(new FillLayout)

    val parseExecutor = Executors.newFixedThreadPool(4)

    new ParserStudio2(shell, parseExecutor).start()

    shell.open()
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep()
      }
    }
    parseExecutor.shutdownNow()
    display.dispose()
  }
}
