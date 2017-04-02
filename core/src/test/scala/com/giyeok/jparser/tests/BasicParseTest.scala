package com.giyeok.jparser.tests

import com.giyeok.jparser.Inputs
import org.scalatest.FlatSpec
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.ParseResultTree
import com.giyeok.jparser.ParseResultGraph
import com.giyeok.jparser.ParseResultDerivationsSet
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.ParseResultGraphFunc
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.ParseForest

class BasicParseTest(val testsSuite: Traversable[GrammarTestCases]) extends FlatSpec {
    def log(s: String): Unit = {
        // println(s)
    }

    private def testNGrammar(ngrammar: NGrammar) = {
        import NGrammar._
        val allSymbolIds = ngrammar.nsymbols.keySet ++ ngrammar.nsequences.keySet
        assert((ngrammar.nsymbols.keySet intersect ngrammar.nsequences.keySet).isEmpty)
        ngrammar.nsymbols.values foreach {
            case Terminal(_) =>
            case d: NSimpleDerivable =>
                assert(d.produces subsetOf allSymbolIds)
            case l: NLookaheadSymbol =>
                assert(ngrammar.nsymbols.keySet contains l.lookahead)
            case Join(_, body, join) =>
                assert(ngrammar.nsymbols.keySet contains body)
                assert(ngrammar.nsymbols.keySet contains join)
        }
        ngrammar.nsequences.values foreach {
            case Sequence(_, sequence) =>
                assert(sequence forall { ngrammar.nsymbols.keySet contains _ })
                assert(sequence forall { id => !(ngrammar.nsequences.keySet contains id) })
        }
    }

    type R = ParseResultGraph
    val resultFunc = ParseResultGraphFunc

    def parse(tests: GrammarTestCases, source: Inputs.ConcreteSource): Either[R, ParsingError] = {
        // 여기서 nparser에 테스트하고싶은 파서 종류를 지정하면 됨
        val nparser = tests.nparserNaive
        nparser.parse(source) match {
            case Left(ctx) =>
                val resultOpt = new ParseTreeConstructor(resultFunc)(nparser.grammar)(ctx.inputs, ctx.history, ctx.conditionFate).reconstruct()
                resultOpt match {
                    case Some(result) => Left(result)
                    case None => Right(ParsingErrors.UnexpectedError)
                }
            case Right(error) => Right(error)
        }
    }

    private def testCorrect(tests: GrammarTestCases, source: Inputs.ConcreteSource, iterations: Int) = {
        log(s"testing ${tests.grammar.name} on ${source.toCleanString}")
        it should s"${tests.grammar.name} properly parsed on '${source.toCleanString}'" in {
            (0 until iterations) foreach { _ =>
                parse(tests, source) match {
                    case Left(result) => checkParse(result, tests.grammar)
                    case Right(error) => fail(error.msg)
                }
            }
        }
    }

    private def testIncorrect(tests: GrammarTestCases, source: Inputs.ConcreteSource, iterations: Int) = {
        log(s"testing ${tests.grammar.name} on ${source.toCleanString}")
        it should s"${tests.grammar.name} failed to parse on '${source.toCleanString}'" in {
            (0 until iterations) foreach { _ =>
                parse(tests, source) match {
                    case Left(result) => fail("??")
                    case Right(error) => assert(true)
                }
            }
        }
    }

    private def testAmbiguous(tests: GrammarTestCases, source: Inputs.ConcreteSource, iterations: Int) = {
        log(s"testing ${tests.grammar.name} on ${source.toCleanString}")
        it should s"${tests.grammar.name} is ambiguous on '${source.toCleanString}'" in {
            (0 until iterations) foreach { _ =>
                parse(tests, source) match {
                    case Left(result) => checkParse(result, tests.grammar)
                    case Right(error) => fail(error.msg)
                }
            }
        }
    }

    private def checkParse(result: ParseForest, grammar: Grammar): Unit = {
        result.trees foreach { checkParse(_, grammar) }
    }

    private def checkParse(parseTree: ParseResultTree.Node, grammar: Grammar): Unit = {
        import ParseResultTree._
        parseTree match {
            case BindNode(term: Terminal, TerminalNode(input)) =>
                assert(term.accept(input))
            case BindNode(Start, body @ BindNode(bodySym, _)) =>
                assert(grammar.startSymbol == bodySym)
                checkParse(body, grammar)
            case BindNode(Nonterminal(name), body @ BindNode(bodySymbol, _)) =>
                assert(grammar.rules(name) contains bodySymbol)
                checkParse(body, grammar)
            case BindNode(sym: Join, body @ JoinNode(BindNode(bodySym, _), BindNode(joinSym, _))) =>
                assert(sym.sym == bodySym)
                assert(sym.join == joinSym)
                checkParse(body, grammar)
            case BindNode(Sequence(seq, ws), seqBody: SequenceNode) =>
                assert((seqBody.children map { _.asInstanceOf[BindNode].symbol }) == seq)
                checkParse(seqBody, grammar)
            case BindNode(OneOf(syms), body @ BindNode(bodySymbol, _)) =>
                assert(syms contains bodySymbol)
                checkParse(body, grammar)
            case BindNode(Repeat(sym, _), body @ BindNode(bodySymbol, _)) =>
                def childrenOf(node: Node, sym: Symbol): Seq[BindNode] = node match {
                    case node @ BindNode(s, body) if s == sym => Seq(node)
                    case BindNode(s, body) => childrenOf(body, sym)
                    case s: SequenceNode => s.children flatMap { childrenOf(_, sym) }
                }
                val children = childrenOf(body, sym)
                assert(children forall { _.symbol == sym })
                checkParse(body, grammar)
            case BindNode(Except(sym, _), body @ BindNode(bodySym, _)) =>
                assert(sym == bodySym)
                checkParse(body, grammar)
            case BindNode(Proxy(sym), body @ BindNode(bodySym, _)) =>
                assert(sym == bodySym)
                checkParse(body, grammar)
            case BindNode(Longest(sym), body @ BindNode(bodySym, _)) =>
                assert(sym == bodySym)
                checkParse(body, grammar)
            case BindNode(EagerLongest(sym), body @ BindNode(bodySym, _)) =>
                assert(sym == bodySym)
                checkParse(body, grammar)
            case BindNode(_: LookaheadIs | _: LookaheadExcept, body) =>
                assert(body == SequenceNode(Sequence(Seq()), List()))
            case node: SequenceNode =>
                node.childrenAll foreach { checkParse(_, grammar) }
            case JoinNode(body, join) =>
                checkParse(body, grammar)
                checkParse(join, grammar)
            case BindNode(symbol, body) =>
                println(symbol)
                ???
            case _ =>
                println(parseTree)
                ???
        }
    }

    private def checkParse(result: ParseResultDerivationsSet, grammar: Grammar): Unit = {
        // TODO
    }

    private def checkParse(parseGraph: ParseResultGraph, grammar: Grammar): Unit = {
        // TODO
    }

    testsSuite foreach { test =>
        testNGrammar(test.ngrammar)
        val iterations = 10
        test.correctSampleInputs foreach { testCorrect(test, _, iterations) }
        test.incorrectSampleInputs foreach { testIncorrect(test, _, iterations) }
        if (test.isInstanceOf[AmbiguousSamples]) test.asInstanceOf[AmbiguousSamples].ambiguousSampleInputs foreach { testAmbiguous(test, _, iterations) }
    }
}
