package com.giyeok.jparser.studio2

import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.metalang3.MetaLanguage3.{ProcessedGrammar, analyzeGrammar, transformGrammar}
import com.giyeok.jparser.metalang3.generated.MetaLang3Ast
import com.giyeok.jparser.metalang3.{CollectedErrors, ErrorCollector, ErrorMessage}
import com.giyeok.jparser.milestone.MilestoneParser
import com.giyeok.jparser.proto.MilestoneParserProtobufConverter.convertProtoToMilestoneParserData
import com.giyeok.jparser.proto.MilestoneParserDataProto
import com.giyeok.jparser.studio2.CodeEditor.CodeStyle
import com.giyeok.jparser.studio2.GrammarDefEditor._
import com.giyeok.jparser.{Inputs, NGrammar, ParsingErrors}
import io.reactivex.rxjava3.core.{Observable, Scheduler}
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.widgets.Composite

import java.io.{BufferedInputStream, FileInputStream}
import java.time.{Duration, LocalDateTime}
import java.util.concurrent.TimeUnit
import scala.util.Using

class GrammarDefEditor(val parent: Composite, val style: Int, val font: Font, val scheduler: Scheduler) {
  val editor = new CodeEditor(parent, style, font)
  val highlighter = new MetaLang3Highlighter(editor)

  private val milestoneParserData = convertProtoToMilestoneParserData(Using(
    new BufferedInputStream(new FileInputStream("MetaLang3AstMilestoneParser.pb"))) { inputStream =>
    MilestoneParserDataProto.MilestoneParserData.parseFrom(inputStream)
  }.get)
  private val milestoneParser = new MilestoneParser(milestoneParserData)

  private def grammarUpdateEventsFrom(observable: Observable[String]): Observable[UpdateEvent] =
    observable.switchMap({ text: String =>
      Observable.create[UpdateEvent] { sub =>
        sub.onNext(GrammarChanged)

        val startTime = LocalDateTime.now()

        def duration(): Duration = Duration.between(startTime, LocalDateTime.now())

        def onGrammarParsed(grammarDef: MetaLang3Ast.Grammar) = {
          println(s"${text.length} : Parsed")
          sub.onNext(GrammarDefParsed(grammarDef)(duration()))
          implicit val errorCollector: ErrorCollector = new ErrorCollector()
          try {
            val (transformer, grammar, ngrammar) = transformGrammar(grammarDef, "Generated")

            if (!errorCollector.isClear) {
              println(s"${text.length} : Error while transforming")
              sub.onNext(GrammarProcessError(errorCollector.collectedErrors)(duration()))
            } else {
              println(s"${text.length} : Grammar generated")
              sub.onNext(GrammarGenerated(ngrammar)(duration()))
              val processedGrammar = analyzeGrammar(transformer, grammar, ngrammar)
              if (!errorCollector.isClear) {
                println(s"${text.length} : Grammar analysis error")
                sub.onNext(GrammarProcessError(errorCollector.collectedErrors)(duration()))
              } else {
                println(s"${text.length} : Grammar processed")
                sub.onNext(GrammarProcessed(processedGrammar)(duration()))
              }
            }
          } catch {
            case e: Throwable =>
              e.printStackTrace()
              sub.onNext(GrammarProcessError(CollectedErrors(List(
                ErrorMessage(s"${e.getClass} ${e.getMessage}", None))))(duration()))
          }
          sub.onComplete()
        }

        println(s"${text.length} : Starting")
        val input = Inputs.fromString(text)
        milestoneParser.parse(input) match {
          case Left(finalCtx) =>
            MilestoneParser.reconstructParseTree(milestoneParserData, finalCtx, input) match {
              case Some(forest) =>
                println(forest.trees.size)
                assert(forest.trees.size == 1)
                val grammarDef = MetaLang3Ast.matchStart(forest.trees.head)
                onGrammarParsed(grammarDef)
              case None =>
            }
          case Right(parsingError) => sub.onNext(GrammarDefError(parsingError)(duration()))
        }
        //        MetaLang3Ast.parseAst(text) match {
        //          case Left(grammarDef) => onGrammarParsed(grammarDef)
        //          case Right(parsingError) => sub.onNext(GrammarDefError(parsingError)(duration()))
        //        }
      }.observeOn(scheduler).subscribeOn(scheduler)
    })

  val grammarUpdateObs: Observable[UpdateEvent] = grammarUpdateEventsFrom(
    editor.textObservable.debounce(250, TimeUnit.MILLISECONDS)
  ).publish().refCount().observeOn(scheduler).subscribeOn(scheduler)

  grammarUpdateObs.subscribe { update: UpdateEvent =>
    update match {
      case GrammarChanged =>
      case GrammarDefParsed(grammarDefinition) =>
        highlighter.highlightGrammar(grammarDefinition)

      case GrammarDefError(parsingError) => parsingError match {
        case ParsingErrors.UnexpectedEOF(expected, location) =>
          editor.clearStyles()
          editor.setStyle(CodeStyle.ERROR, location, location + 1)
          println("Unexpected EOF")
        case ParsingErrors.UnexpectedInput(next, expected, location) =>
          editor.clearStyles()
          editor.setStyle(CodeStyle.ERROR, location, location + 1)
          println(s"Unexpected input: ${expected.map(_.toShortString).mkString(", ")}")
        case ParsingErrors.AmbiguousParse(msg) => ???
        case ParsingErrors.UnexpectedError => ???
        case _ =>
      }

      case GrammarGenerated(ngrammar) =>
        println(s"Grammar generated: $ngrammar")

      case GrammarProcessed(processedGrammar) =>
        println(processedGrammar.nonterminalTypes)

      case GrammarProcessError(errors) =>
        println(s"Process error $errors")
    }
  }
}

object GrammarDefEditor {

  sealed trait UpdateEvent {
    val elapsedTime: Duration
  }

  case object GrammarChanged extends UpdateEvent {
    override val elapsedTime: Duration = Duration.ZERO
  }

  case class GrammarDefParsed(grammarDefinition: MetaLang3Ast.Grammar)(override val elapsedTime: Duration) extends UpdateEvent

  case class GrammarDefError(parsingError: ParsingError)(override val elapsedTime: Duration) extends UpdateEvent

  case class GrammarGenerated(ngrammar: NGrammar)(override val elapsedTime: Duration) extends UpdateEvent

  case class GrammarProcessed(processedGrammar: ProcessedGrammar)(override val elapsedTime: Duration) extends UpdateEvent

  case class GrammarProcessError(errors: CollectedErrors)(override val elapsedTime: Duration) extends UpdateEvent

}