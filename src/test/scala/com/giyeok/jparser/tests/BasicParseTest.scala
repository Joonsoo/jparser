package com.giyeok.jparser.tests

import com.giyeok.jparser.Inputs
import org.scalatest.FlatSpec
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.deprecated.NewParser
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.deprecated.DerivationSliceFunc
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.ParseResultTree
import com.giyeok.jparser.ParseResultGraph
import com.giyeok.jparser.ParseResultDerivationsSet
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.ParseResultGraphFunc
import com.giyeok.jparser.nparser.NGrammar

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

    //    private def testReconstructParser(tests: GrammarTestCases, source: Inputs.ConcreteSource, result: ParseResultGraph) = {
    //        val parser = tests.reconstructParser
    //        parser.parse(source) match {
    //            case Left(ctx) =>
    //                assert(ctx.asInstanceOf[parser.SavingParsingCtx].reconstructResult == result)
    //            case Right(error) => fail(error.msg)
    //        }
    //    }
    private def testNparserNaive(tests: GrammarTestCases, source: Inputs.ConcreteSource, result: ParseResultGraph) = {
        val nparser = tests.nparserNaive
        nparser.parse(source) match {
            case Left(ctx) =>
                val parseTree: Option[ParseResultGraph] = new ParseTreeConstructor(ParseResultGraphFunc)(nparser.grammar)(ctx.inputs, ctx.history, ctx.conditionFate).reconstruct(nparser.startNode, ctx.gen)
            //assert(parseTree == result)
            case Right(error) => fail(error.msg)
        }
    }

    def parserOf(tests: GrammarTestCases) = tests.naiveParser

    private def testCorrect(tests: GrammarTestCases, source: Inputs.ConcreteSource) = {
        log(s"testing ${tests.grammar.name} on ${source.toCleanString}")
        val result = parserOf(tests).parse(source)
        it should s"${tests.grammar.name} properly parsed on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) =>
                    assert(ctx.result.isDefined)
                    //                    val trees = ctx.result.get.trees
                    //                    if (trees.size != 1) {
                    //                        trees.zipWithIndex foreach { result =>
                    //                            log(s"=== ${result._2} ===")
                    //                            log(result._1.toHorizontalHierarchyString)
                    //                        }
                    //                    }
                    //                    assert(trees.size == 1)
                    //                    trees foreach { checkParse(_, tests.grammar) }
                    checkParse(ctx.result.get, tests.grammar)

                    //                    log(s"testing ReconstructParser ${tests.grammar.name} on ${source.toCleanString}")
                    //                    testReconstructParser(tests, source, ctx.result.get)
                    log(s"testing NumberedNaiveParser ${tests.grammar.name} on ${source.toCleanString}")
                    testNparserNaive(tests, source, ctx.result.get)
                case Right(error) => fail(error.msg)
            }
        }
    }

    private def testIncorrect(tests: GrammarTestCases, source: Inputs.ConcreteSource) = {
        log(s"testing ${tests.grammar.name} on ${source.toCleanString}")
        val result = parserOf(tests).parse(source)
        it should s"${tests.grammar.name} failed to parse on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) => assert(ctx.result.isEmpty)
                case Right(_) => assert(true)
            }
        }
    }

    private def testAmbiguous(tests: GrammarTestCases, source: Inputs.ConcreteSource) = {
        log(s"testing ${tests.grammar.name} on ${source.toCleanString}")
        val result = parserOf(tests).parse(source)
        it should s"${tests.grammar.name} is ambiguous on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) =>
                    assert(ctx.result.isDefined)
                    //                    val trees = ctx.result.get.trees
                    //                    assert(trees.size > 1)
                    //                    trees foreach { checkParse(_, tests.grammar) }
                    checkParse(ctx.result.get, tests.grammar)

                    //                    log(s"testing ReconstructParser ${tests.grammar.name} on ${source.toCleanString}")
                    //                    testReconstructParser(tests, source, ctx.result.get)
                    log(s"testing NumberedNaiveParser ${tests.grammar.name} on ${source.toCleanString}")
                    testNparserNaive(tests, source, ctx.result.get)
                case Right(_) => assert(false)
            }
        }
    }

    private def checkParse(parseTree: ParseResultTree.Node, grammar: Grammar): Unit = {
        import ParseResultTree._
        parseTree match {
            case TermFuncNode => ??? // should not happen
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
        test.correctSampleInputs foreach { testCorrect(test, _) }
        test.incorrectSampleInputs foreach { testIncorrect(test, _) }
        if (test.isInstanceOf[AmbiguousSamples]) test.asInstanceOf[AmbiguousSamples].ambiguousSampleInputs foreach { testAmbiguous(test, _) }
    }
}
