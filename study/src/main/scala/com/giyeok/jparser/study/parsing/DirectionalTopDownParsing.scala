package com.giyeok.jparser.study.parsing

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.study.CfgSymbols.Nonterminal
import com.giyeok.jparser.study.ContextFreeGrammar

trait DirectionalTopDownParser {
    def nextAction(context: PredictionStackContext): NextAction
    def next(context: PredictionStackContext, nextAction: NextAction): PredictionStackContext

    def iterate(context: PredictionStackContext, count: Int): PredictionStackContext = {
        context.prettyPrint()
        (0 until count).foldLeft(context) { (current, i) =>
            println()
            println(s"Iteration ${i + 1}")
            val action = nextAction(current)
            println(s"Action: $action")
            val nextContext = next(current, action)
            nextContext.prettyPrint()
            nextContext
        }
    }
}

object DirectionalTopDownParsing {
    // Interactive(rule selected by user)
    def interactive(grammar: ContextFreeGrammar, input: List[Inputs.Input]): (PredictionStackContext, DirectionalTopDownParser) = {
        val stackConfiguration = Seq(
            AnalysisAndPrediction(List(), List(PredictionStackNonterminal(Nonterminal(grammar.startNonterminal))))
        )
        val initialContext = new PredictionStackContext(grammar, List(), input, stackConfiguration)
        val parser = new DirectionalTopDownParser {
            def nextAction(context: PredictionStackContext): NextAction = {
                if (context.stacks.isEmpty) {
                    // 더이상 파싱할 수 있는 가능성이 없으면 fail
                    ParseFailed
                } else if (context.restOfInput.isEmpty) {
                    // 모든 입력이 끝났으면
                    ParseDone
                } else {
                    // 현재 context에서 할 수 있는 다음 동작
                    val nextIsMatch = context.stacks forall { _.predictionStack.head.isTerminal }
                    if (nextIsMatch) {
                        // 모든 prediction stack들의 top이 전부 terminal이면 NextMatch
                        val nextInput = context.restOfInput.head
                        val survivingStacks = context.stacks.zipWithIndex filter { x =>
                            x._1.predictionStack match {
                                case (head: PredictionStackTerminal) +: _ => head.terminal.terminal accept nextInput
                                case _ => false
                            }
                        } map { _._2 }
                        if (survivingStacks.isEmpty) ParseFailed else NextMatch(survivingStacks)
                    } else {
                        // Nonterminal이 있으면 NextPredict
                        val predictingStacks = context.stacks.zipWithIndex flatMap { x =>
                            val (stack, idx) = x
                            stack.predictionStack.head match {
                                case PredictionStackNonterminal(nonterminal @ Nonterminal(name)) =>
                                    val analyses = grammar.rules(name).toSeq.zipWithIndex map { rhsIdx =>
                                        val (rhs, idx) = rhsIdx
                                        AnalysisNonterminal(nonterminal, idx, rhs.toList)
                                    }
                                    Some(idx -> analyses)
                                case _ => None
                            }
                        }
                        NextPredict(predictingStacks.toMap, selectable = true)
                    }
                }
            }

            def next(context: PredictionStackContext, nextAction: NextAction): PredictionStackContext = nextAction match {
                case NextPredict(options, _) =>
                    // context.stacks 에서 options.keys가 지정한 스택들에 대해서는
                    // context.stacks의 predictionStack의 top을 pop하고,
                    // rhs에 해당하는 내용들을 predictionStack에 push하고,
                    // analysisStack에 AnalysisNonterminal을 push한다.
                    // options.keys가 지정하지 않은 스택들은 건드리지 않는다.
                    // matchedInput과 restOfInput은 건드리지 않는다
                    val newStacks = context.stacks.zipWithIndex flatMap { x =>
                        val (stack, idx) = x
                        options get idx match {
                            case Some(rhs) =>
                                rhs map { r =>
                                    AnalysisAndPrediction(
                                        r +: stack.analysisList,
                                        (r.rhs map { PredictionStackItem(_) }) ++: stack.predictionStack.tail
                                    )
                                }
                            case None => Seq(stack)
                        }
                    }
                    new PredictionStackContext(
                        context.grammar,
                        context.matchedInput, context.restOfInput,
                        newStacks
                    )
                case NextMatch(survivingStacks) =>
                    // context.stacks 에서 survivingStacks 가 지정한 살아남을 stack만 남기고
                    // restOfInput에서 한글자를 matchedInput으로 넘기고
                    // stacks에서 predictionStack의 top을 pop하고 analysisList에 AnalysisTerminal을 push한다
                    val survivedStacks = survivingStacks map { context.stacks(_) }
                    new PredictionStackContext(
                        grammar,
                        context.restOfInput.head +: context.matchedInput, context.restOfInput.tail,
                        survivedStacks map { x =>
                            AnalysisAndPrediction(
                                AnalysisTerminal(x.predictionStack.head.asInstanceOf[PredictionStackTerminal].terminal) +: x.analysisList,
                                x.predictionStack.tail
                            )
                        }
                    )
                case _ =>
                    // 더이상 할 수 있는 게 없음. 잘못된 호출이므로 그냥 그대로 반환
                    context
            }
        }
        (initialContext, parser)
    }

    // Breadth-first
    def breadthFirst(grammar: ContextFreeGrammar): Unit = {

    }

    // Depth-first
    def depthFirst(grammar: ContextFreeGrammar): Unit = {

    }

    // Recursive descent
    // Breadth-first recursive descent
    // and so on
}
