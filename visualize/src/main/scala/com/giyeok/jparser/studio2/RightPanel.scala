package com.giyeok.jparser.studio2

import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.metalang3a.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3a.ValuefyExprSimulator
import com.giyeok.jparser.nparser.ParseTreeUtil.expectedTermsFrom
import com.giyeok.jparser.nparser.Parser.NaiveContext
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeUtil, ParsingContext}
import com.giyeok.jparser.studio2.CodeEditor.CodeStyle
import com.giyeok.jparser.studio2.Utils.setMainAndBottomLayout
import com.giyeok.jparser.visualize.utils.HorizontalResizableSplittedComposite
import com.giyeok.jparser.visualize.{NodeFigureGenerators, ParseTreeViewer, ParsingProcessVisualizer, ZestParsingContextWidget}
import com.giyeok.jparser.{Inputs, NGrammar, ParseForest, ParsingErrors}
import io.reactivex.rxjava3.core.{Observable, Scheduler}
import io.reactivex.rxjava3.subjects.PublishSubject
import org.eclipse.draw2d.Figure
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{SelectionAdapter, SelectionEvent}
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.{FillLayout, FormLayout}
import org.eclipse.swt.widgets.{Button, Composite, MessageBox, Shell}

import java.util.concurrent.TimeUnit

class RightPanel(parent: Composite, style: Int, font: Font, scheduler: Scheduler, grammarObs: Observable[GrammarDefEditor.UpdateEvent]) {
  private val rightPanel = new HorizontalResizableSplittedComposite(parent, style, 30)
  private var testCodeEditor: CodeEditor = null
  private var openProceedViewButton: Button = null
  private var parseTreeViewer: ParseTreeViewer = null
  private var astViewer: AstViewer = null

  def init(): Unit = {
    // 루트 -> 오른쪽 -> 상단 테스트 패널. 상단에 테스트 text editor, 하단에 "Proceed View" 버튼
    val testCodePanel = new Composite(rightPanel.upperPanel, SWT.NONE)
    testCodePanel.setLayout(new FormLayout)

    testCodeEditor = new CodeEditor(testCodePanel, SWT.V_SCROLL | SWT.H_SCROLL, font)
    openProceedViewButton = new Button(testCodePanel, SWT.NONE)
    openProceedViewButton.setText("Proceed View")
    val openProceedViewButtonObs = PublishSubject.create[SelectionEvent]()
    openProceedViewButton.addSelectionListener(new SelectionAdapter {
      override def widgetSelected(e: SelectionEvent): Unit = {
        openProceedViewButtonObs.onNext(e)
      }
    })
    setMainAndBottomLayout(testCodeEditor.styledText, openProceedViewButton)

    openProceedViewButtonObs.withLatestFrom(grammarObs, (_: SelectionEvent, _: GrammarDefEditor.UpdateEvent))
      .subscribe({ pair: (SelectionEvent, GrammarDefEditor.UpdateEvent) =>
        val gramOpt = pair._2 match {
          case GrammarDefEditor.GrammarGenerated(ngrammar) => Some(ngrammar)
          case GrammarDefEditor.GrammarProcessed(processedGrammar) => Some(processedGrammar.ngrammar)
          case _ => None
        }
        testCodeEditor.clearStyles()
        gramOpt match {
          case Some(ngrammar) =>
            val display = parent.getDisplay
            val newShell = new Shell(display)
            ParsingProcessVisualizer.start[NaiveContext](
              title = "Proceed View",
              parser = new NaiveParser(ngrammar, trim = true),
              Inputs.fromString(testCodeEditor.styledText.getText), display, newShell,
              (parent: Composite, style: Int, fig: NodeFigureGenerators[Figure], grammar: NGrammar, graph: ParsingContext.Graph, context: NaiveContext) =>
                new ZestParsingContextWidget(parent, style, fig, grammar, graph, context)
            )
          case None =>
            println(s"Cannot open proceed view, the latest parser was: ${pair._2}")
        }
      })

    // 루트 -> 오른쪽 -> 하단 테스트 텍스트 파싱 결과. 상단에는 parse tree, 하단에는 AST
    rightPanel.lowerPanel.setLayout(new FillLayout(SWT.VERTICAL))
    parseTreeViewer = new ParseTreeViewer(rightPanel.lowerPanel, SWT.NONE)
    astViewer = new AstViewer(rightPanel.lowerPanel, SWT.NONE)

    // 파싱 결과 표시
    val generatedGrammarObs: Observable[NGrammar] = grammarObs
      .filter(_.isInstanceOf[GrammarDefEditor.GrammarGenerated])
      .map(_.asInstanceOf[GrammarDefEditor.GrammarGenerated].ngrammar)
    val exampleParseResult = Observable.combineLatest(generatedGrammarObs,
      testCodeEditor.textObservable.debounce(250, TimeUnit.MILLISECONDS),
      (_: NGrammar, _: String)).switchMap { pair: (NGrammar, String) =>
      Observable.create[Either[ParseForest, ParsingError]] { sub =>
        new NaiveParser(pair._1).parse(pair._2) match {
          case Left(ctx) =>
            ParseTreeUtil.reconstructTree(pair._1, ctx) match {
              case Some(parseForest) => sub.onNext(Left(parseForest))
              case None => sub.onNext(Right(ParsingErrors.UnexpectedEOF(expectedTermsFrom(ctx), pair._2.length)))
            }
          case Right(parsingError) =>
            sub.onNext(Right(parsingError))
        }
        sub.onComplete()
      }.observeOn(scheduler).subscribeOn(scheduler)
    }.observeOn(scheduler).subscribeOn(scheduler).publish().refCount()

    exampleParseResult.subscribe { parseResult: Either[ParseForest, ParsingError] =>
      testCodeEditor.clearStyles()
      parseResult match {
        case Left(parseForest) =>
        // parseTreeViewer.setParseForest(parseForest)
        case Right(parsingError) =>
          parseTreeViewer.invalidateParseForest()
          parsingError match {
            case ParsingErrors.AmbiguousParse(msg) =>
            case ParsingErrors.UnexpectedEOF(expected, location) =>
              testCodeEditor.setStyle(CodeStyle.ERROR, location, location + 1)
            case ParsingErrors.UnexpectedError =>
            case ParsingErrors.UnexpectedInput(next, expected, location) =>
              testCodeEditor.setStyle(CodeStyle.ERROR, location, location + 1)
            case _ =>
          }
        // TODO 오류 표시
      }
    }

    val processedGrammarObs: Observable[ProcessedGrammar] = grammarObs
      .filter(_.isInstanceOf[GrammarDefEditor.GrammarProcessed])
      .map(_.asInstanceOf[GrammarDefEditor.GrammarProcessed].processedGrammar)
    val astResultObs: Observable[Either[List[ValuefyExprSimulator.Value], ParsingError]] =
      Observable.combineLatest(exampleParseResult, processedGrammarObs, (_: Either[ParseForest, ParsingError], _: ProcessedGrammar))
        .switchMap { pair: (Either[ParseForest, ParsingError], ProcessedGrammar) =>
          Observable.create[Either[List[ValuefyExprSimulator.Value], ParsingError]] { sub =>
            val (parseResult, processedGrammar) = pair
            parseResult match {
              case Left(parseForest) =>
                try {
                  val astValues = parseForest.trees.toList.map { parseTree =>
                    new ValuefyExprSimulator(processedGrammar.ngrammar, processedGrammar.startNonterminalName,
                      processedGrammar.nonterminalValuefyExprs, processedGrammar.shortenedEnumTypesMap).valuefy(parseTree)
                  }
                  sub.onNext(Left(astValues))
                } catch {
                  case _: Throwable =>
                    sub.onNext(Right(ParsingErrors.UnexpectedError))
                }
              case Right(value) =>
                sub.onNext(Right(value))
            }
            sub.onComplete()
          }.observeOn(scheduler).subscribeOn(scheduler)
        }

    astResultObs.observeOn(scheduler).subscribeOn(scheduler).subscribe {
      astResult: Either[List[ValuefyExprSimulator.Value], ParsingError] =>
        astResult match {
          case Left(astValues) =>
            astViewer.setAstValues(astValues)
            astValues.foreach(value => println(value.prettyPrint()))
          case Right(parsingError) =>
            astViewer.invalidateAstValues()
            println(parsingError) // TODO 오류 표시
        }
    }
  }

  init()
}
