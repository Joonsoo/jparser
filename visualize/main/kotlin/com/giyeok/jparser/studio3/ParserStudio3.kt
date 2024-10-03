package com.giyeok.jparser.studio3

import com.giyeok.gviz.figure.VertFlowFigure
import com.giyeok.gviz.render.swing.*
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.`ParseForestFunc$`
import com.giyeok.jparser.ParseResultTree
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.metalang3.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3.ValuefyExprSimulator
import com.giyeok.jparser.metalang3.ast.MetaLang3Ast
import com.giyeok.jparser.nparser2.NaiveParser2
import com.giyeok.jparser.swingvis.FigureGen
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GridLayout
import java.util.concurrent.Executors
import javax.swing.*

class ParserStudio3(val workerDispatcher: CoroutineDispatcher) {
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
    VertFlowStyle(0.0, Alignment.LEADING),
    HorizFlowStyle(0.0, Alignment.LEADING),
    GridStyle(0.0, 0.0)
  )

  val grammarPane = GrammarEditorPane(workerDispatcher)
  val examplePane = ExampleEditorPane()

  init {
    grammarPane.font = defaultFont
    examplePane.font = defaultFont
  }

  data class State(
    val ast: DataUpdateEvent<MetaLang3Ast.Grammar>,
    val grammar: DataUpdateEvent<ProcessedGrammar>,
    val example: DataUpdateEvent<String>
  )

  sealed class ExampleParseResult {
    object GrammarNotReady : ExampleParseResult()
    object ProcessingGrammar : ExampleParseResult()
    object ExampleWaiting : ExampleParseResult()
    data class ExampleParseError(val parsingError: ParsingError) : ExampleParseResult()
    data class ExampleParseException(val exception: Throwable) : ExampleParseResult()
    data class ExampleParseSucceeded(
      val grammar: ProcessedGrammar,
      val results: List<Pair<ParseResultTree.Node, ValuefyExprSimulator.Value>>,
    ) : ExampleParseResult()
  }

  val parseResultFlow: Flow<ExampleParseResult> = combine(
    grammarPane.parsedAstFlow,
    grammarPane.grammarFlow,
    examplePane.textFlow,
    ::State
  ).map { (ast, grammar, example) ->
    if (ast !is DataUpdateEvent.NewDataAvailable || grammar !is DataUpdateEvent.NewDataAvailable) {
      if (grammar is DataUpdateEvent.InvalidateLatestData) {
        ExampleParseResult.ProcessingGrammar
      } else {
        ExampleParseResult.GrammarNotReady
      }
    } else {
      when (example) {
        is DataUpdateEvent.ExceptionThrown -> TODO()
        is DataUpdateEvent.NoDataAvailable, is DataUpdateEvent.InvalidateLatestData ->
          ExampleParseResult.ExampleWaiting

        is DataUpdateEvent.NewDataAvailable -> {
          val ngrammar = grammar.data.ngrammar()
          val inputs = Inputs.fromString(example.data)
          val parser = NaiveParser2(ngrammar)
          val parsed = parser.parse(inputs)

          if (parsed.isRight) {
            val ctx = parsed.right().get()

            val reconstructor = parser.parseTreeReconstructor2(`ParseForestFunc$`.`MODULE$`, ctx)

            withContext(workerDispatcher + currentCoroutineContext()) {
              val reconstructionResult = reconstructor.reconstruct()
              if (reconstructionResult.isEmpty) {
                ExampleParseResult.ExampleParseException(IllegalStateException("??"))
              } else {
                val astSimulator = ValuefyExprSimulator(
                  grammar.data.ngrammar(),
                  grammar.data.startNonterminalName(),
                  grammar.data.nonterminalValuefyExprs(),
                  grammar.data.shortenedEnumTypesMap()
                )
                val parseForest = reconstructionResult.get()
                val parseTrees = parseForest.trees().toKtList()
                ExampleParseResult.ExampleParseSucceeded(
                  grammar.data,
                  parseTrees.map { Pair(it, astSimulator.valuefyStart(it)) }
                )
              }
            }
          } else {
            ExampleParseResult.ExampleParseError(parsed.left().get())
          }
        }
      }
    }
  }

  fun run() {
    println("Starting Parser Studio...")

    val frame = JFrame()
    frame.layout = GridLayout(1, 1)

    val leftPanel = JPanel()
    leftPanel.layout = GridLayout(2, 1)

    leftPanel.add(
      JScrollPane(
        grammarPane,
        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
      )
    )

    val rightPanel = JPanel()
    rightPanel.layout = GridLayout(3, 1)

    rightPanel.add(
      JScrollPane(
        examplePane,
        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS,
      )
    )

    val parseTreeView = JScrollPane()
    val astValueView = JScrollPane()

    CoroutineScope(workerDispatcher).launch {
      launch {
        grammarPane.parsedAstFlow.mapNotNull { (it as? DataUpdateEvent.NewDataAvailable)?.data }
          .collect { ast ->
            println("AST: $ast")
          }
      }
      launch {
        grammarPane.grammarFlow.collect {
          println("Grammar: $it")
        }
      }
      launch {
        parseResultFlow.collect { result ->
          when (result) {
            ExampleParseResult.GrammarNotReady -> {
              parseTreeView.setViewportView(JLabel("Finish the grammar first"))
            }

            ExampleParseResult.ProcessingGrammar -> {
              parseTreeView.setViewportView(JLabel("Processing the grammar..."))
            }

            ExampleParseResult.ExampleWaiting -> {
              parseTreeView.setViewportView(JLabel("Finish typing the example"))
            }

            is ExampleParseResult.ExampleParseError -> {
              parseTreeView.setViewportView(JLabel(result.parsingError.msg()))
            }

            is ExampleParseResult.ExampleParseException -> {
              parseTreeView.setViewportView(JLabel(result.exception.message))
            }

            is ExampleParseResult.ExampleParseSucceeded -> {
              println("# of trees: ${result.results.size}")
              CoroutineScope(workerDispatcher + currentCoroutineContext()).launch {
                val parseTreeFigureAsync = async {
                  val parseTree = result.results.first().first
                  figureGen.parseNodeFigure(parseTree)
                }
                val astValueFigureAsync = async {
                  VertFlowFigure(
                    result.results.map { (_, astValue) ->
                      figureGen.astValueFigure(astValue)
                    },
                    ""
                  )
                }
                val parseTreeFigure = parseTreeFigureAsync.await()
                val astValueFigure = astValueFigureAsync.await()
                for (ast in result.results) {
                  println(ast.second)
                }
                parseTreeView.setViewportView(FigureView(parseTreeFigure, styles))
                astValueView.setViewportView(FigureView(astValueFigure, styles))
              }
            }
          }
        }
      }
    }

    rightPanel.add(parseTreeView)
    rightPanel.add(astValueView)

    val rootPanel = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel)
    frame.add(rootPanel)

    frame.setSize(800, 600)
    frame.isVisible = true

    rootPanel.setDividerLocation(0.5)
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      ParserStudio3(Executors.newFixedThreadPool(4).asCoroutineDispatcher()).run()
    }
  }
}
