package com.giyeok.jparser.study.parsing

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.study.CfgSymbols._
import com.giyeok.jparser.study.ContextFreeGrammar

case class AnalysisAndPrediction(analysisList: List[AnalysisItem], predictionStack: List[PredictionStackItem])

class PredictionStackContext(
        val grammar: ContextFreeGrammar,
        val matchedInput: List[Inputs.Input], val restOfInput: List[Inputs.Input],
        val stacks: Seq[AnalysisAndPrediction]
) {
    def prettyPrint(): Unit = {
        def prettyAnalysisItemString(item: AnalysisItem): String = item match {
            case AnalysisNonterminal(Nonterminal(name), ruleIdx, _) => s"${name}_${ruleIdx + 1}"
            case AnalysisTerminal(terminal) => terminal.terminal.toShortString
        }
        def prettyPredictionStackItemString(item: PredictionStackItem): String = item match {
            case PredictionStackNonterminal(Nonterminal(name)) => name
            case PredictionStackTerminal(terminal) => terminal.terminal.toShortString
        }

        val matchedInputString = matchedInput.reverse map { _.toShortString } mkString " "
        val restOfInputString = restOfInput map { _.toShortString } mkString " "
        val analysisListStrings = stacks map { _.analysisList.reverse map prettyAnalysisItemString mkString " " }
        val predictionStackStrings = stacks map { _.predictionStack map prettyPredictionStackItemString mkString " " }
        val leftLength = Math.max((analysisListStrings map { _.length }).max, matchedInputString.length)
        val rightLength = Math.max((predictionStackStrings map { _.length }).max, restOfInputString.length)

        def leftMargin(str: String, length: Int): String =
            s"${" " * (length - str.length)}$str"
        def rightMargin(str: String, length: Int): String =
            s"$str${" " * (length - str.length)}"

        println(leftMargin(matchedInputString, leftLength) + " | " + rightMargin(restOfInputString, rightLength))
        println(("-" * leftLength) + "-+-" + ("-" * rightLength))
        analysisListStrings zip predictionStackStrings foreach { x =>
            println(leftMargin(x._1, leftLength) + " | " + rightMargin(x._2, rightLength))
        }
    }
}

sealed trait PredictionStackItem {
    def isTerminal: Boolean
}
case class PredictionStackNonterminal(nonterminal: Nonterminal) extends PredictionStackItem {
    def isTerminal = false
}
case class PredictionStackTerminal(terminal: Terminal) extends PredictionStackItem {
    def isTerminal = true
}
object PredictionStackItem {
    def apply(symbol: CfgSymbol): PredictionStackItem =
        symbol match {
            case nonterminal: Nonterminal => PredictionStackNonterminal(nonterminal)
            case terminal: Terminal => PredictionStackTerminal(terminal)
        }
}

sealed trait AnalysisItem
case class AnalysisNonterminal(nonterminal: Nonterminal, ruleIdx: Int, rhs: List[CfgSymbol]) extends AnalysisItem
case class AnalysisTerminal(terminal: Terminal) extends AnalysisItem

sealed trait NextAction
case class NextPredict(options: Map[Int, Seq[AnalysisNonterminal]], selectable: Boolean) extends NextAction {
    // options: stacks의 index -> stack top의 nonterminal을 해석할 수 있는 방법들
}
case class NextMatch(survivingStacks: Seq[Int]) extends NextAction {
    // survivingStacks: stacks의 index. 다음 terminal과 일치하는 stack만 살아남는다
}
case object ParseDone extends NextAction
case object ParseFailed extends NextAction
