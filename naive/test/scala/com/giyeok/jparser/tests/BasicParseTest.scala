package com.giyeok.jparser.tests

import com.giyeok.jparser.NGrammar._
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor, ParseTreeConstructor2}
import com.giyeok.jparser._
import com.giyeok.jparser.examples.{AmbiguousExamples, GrammarWithExamples}
import org.scalatest.flatspec.AnyFlatSpec

class BasicParseTest(val testsSuite: Iterable[GrammarWithExamples]) extends AnyFlatSpec {
    def log(s: String): Unit = {
        // println(s)
    }

    private def testNGrammar(ngrammar: NGrammar): Unit = {
        import NGrammar._
        assert((ngrammar.nsymbols.keySet intersect ngrammar.nsequences.keySet).isEmpty)

        val symbolIds = ngrammar.nsymbols.keySet
        val allSymbolIds = symbolIds ++ ngrammar.nsequences.keySet
        ngrammar.nsymbols.values foreach {
            case NTerminal(_, _) => // do nothing
            case d: NSimpleDerive =>
                assert(d.produces subsetOf allSymbolIds)
            case NExcept(_, _, body, except) =>
                assert((symbolIds contains body) && (symbolIds contains except))
            case NJoin(_, _, body, join) =>
                assert((symbolIds contains body) && (symbolIds contains join))
            case NLongest(_, _, body) =>
                assert(symbolIds contains body)
            case l: NLookaheadSymbol =>
                assert(symbolIds contains l.lookahead)
        }
        ngrammar.nsequences.values foreach {
            case NSequence(_, _, sequence) =>
                assert(sequence forall {
                    ngrammar.nsymbols.keySet contains _
                })
                assert(sequence forall { id => !(ngrammar.nsequences.keySet contains id) })
        }
    }

    type R = ParseForest
    val resultFunc = ParseForestFunc

    def parse(tests: GrammarWithExamples, source: Inputs.ConcreteSource): Either[R, ParsingError] = {
        // 여기서 nparser에 테스트하고싶은 파서 종류를 지정하면 됨
        val nparser = new NaiveParser(tests.ngrammar)
        nparser.parse(source) match {
            case Left(ctx) =>
                val resultOpt = new ParseTreeConstructor(resultFunc)(nparser.grammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
                val resultOpt2 = ParseTreeConstructor2.forestConstructor(nparser.grammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
                assert(resultOpt == resultOpt2)
                resultOpt match {
                    case Some(result) => Left(result)
                    case None => Right(ParsingErrors.UnexpectedError)
                }
            case Right(error) => Right(error)
        }
    }

    private def testCorrect(tests: GrammarWithExamples, source: Inputs.ConcreteSource): Unit = {
        log(s"testing ${tests.grammar.name} on ${source.toCleanString}")
        it should s"${tests.grammar.name} properly parsed on '${source.toCleanString}'" in {
            parse(tests, source) match {
                case Left(result) => checkParse(result, tests.grammar)
                case Right(error) => fail(error.msg)
            }
        }
    }

    private def testIncorrect(tests: GrammarWithExamples, source: Inputs.ConcreteSource): Unit = {
        log(s"testing ${tests.grammar.name} on ${source.toCleanString}")
        it should s"${tests.grammar.name} failed to parse on '${source.toCleanString}'" in {
            parse(tests, source) match {
                case Left(result) => fail("??")
                case Right(error) => assert(true)
            }
        }
    }

    private def testAmbiguous(tests: GrammarWithExamples, source: Inputs.ConcreteSource): Unit = {
        log(s"testing ${tests.grammar.name} on ${source.toCleanString}")
        it should s"${tests.grammar.name} is ambiguous on '${source.toCleanString}'" in {
            parse(tests, source) match {
                case Left(result) => checkParse(result, tests.grammar)
                case Right(error) => fail(error.msg)
            }
        }
    }

    private def checkParse(result: ParseForest, grammar: Grammar): Unit = {
        result.trees foreach {
            checkParse(_, grammar)
        }
    }

    private def getSymbolOf(node: ParseResultTree.Node): NSymbol = node match {
        // must not be TerminalNode or JoinNode
        case ParseResultTree.BindNode(symbol, _) => symbol
        case ParseResultTree.CyclicBindNode(_, _, symbol) => symbol
        case ParseResultTree.SequenceNode(_, _, symbol, _) => symbol
        case ParseResultTree.CyclicSequenceNode(_, _, symbol, _, _) => symbol
    }

    private def checkParse(parseTree: ParseResultTree.Node, grammar: Grammar): Unit = {
        import ParseResultTree._
        parseTree match {
            case BindNode(term: NTerminal, TerminalNode(_, input)) =>
                assert(term.symbol.accept(input))
            case BindNode(_: NStart, body) =>
                assert(grammar.startSymbol == getSymbolOf(body).symbol)
                checkParse(body, grammar)
            case BindNode(nonterm@NNonterminal(_, Nonterminal(name), _), body) =>
                val bodySym = getSymbolOf(body)
                assert(nonterm.produces contains bodySym.id)
                assert(grammar.rules(name) contains bodySym.symbol)
                checkParse(body, grammar)
            case BindNode(NJoin(_, sym: Join, _, _), body@JoinNode(_, BindNode(bodySym, _), BindNode(joinSym, _))) =>
                assert(sym.sym == bodySym.symbol)
                assert(sym.join == joinSym.symbol)
                checkParse(body, grammar)
            case BindNode(NSequence(_, Sequence(seq), _), seqBody: SequenceNode) =>
                val bodySyms = seqBody.children map (getSymbolOf(_).symbol)
                assert(bodySyms == seq)
                checkParse(seqBody, grammar)
            case BindNode(NOneOf(_, OneOf(syms), _), body) =>
                val bodySymbol = getSymbolOf(body)
                assert(syms contains bodySymbol.symbol.asInstanceOf[AtomicSymbol])
                checkParse(body, grammar)
            case BindNode(repeat@NRepeat(_, Repeat(sym, lower), _, _), body) =>
                val (bodyId, bodyContent) = body match {
                    case BindNode(symbol: NSequence, content: SequenceNode) => (symbol.id, content)
                    case BindNode(symbol: NSequence, content: CyclicSequenceNode) => (symbol.id, content)
                    case content: BindNode =>
                        assert(lower == 1)
                        (content.symbol.id, content)
                    case content: SequenceNode =>
                        assert(lower != 1)
                        (content.symbol.id, content)
                }
                assert(bodyId == repeat.baseSeq || bodyId == repeat.repeatSeq)
                if (bodyId == repeat.baseSeq) {
                    if (lower == 1) {
                        bodyContent match {
                            case BindNode(symbol, _) => assert(sym == symbol.symbol)
                        }
                    } else {
                        bodyContent match {
                            case seq: SequenceNode =>
                                assert(seq.children.length == lower)
                                seq.children foreach {
                                    case BindNode(elemSym, _) => assert(elemSym.symbol == sym)
                                }
                        }
                    }
                    checkParse(bodyContent, grammar)
                } else {
                    bodyContent match {
                        case seq: SequenceNode =>
                            assert(seq.children.length == 2)
                            seq.children(0) match {
                                case BindNode(nested: NRepeat, _) => assert(nested == repeat)
                                case _: CyclicBindNode => // ignore and do nothing
                            }
                            seq.children(1) match {
                                case BindNode(elemSym, _) => assert(elemSym.symbol == sym)
                            }
                        case _: CyclicSequenceNode => // ignore and do nothing
                    }
                    checkParse(bodyContent, grammar)
                }
            case BindNode(NExcept(_, Except(sym, _), _, _), body@BindNode(bodySym, _)) =>
                assert(sym == bodySym.symbol)
                checkParse(body, grammar)
            case BindNode(proxy@NProxy(_, Proxy(sym), _), body@BindNode(bodySym, _)) =>
                assert(proxy.produce == body.symbol.id)
                assert(sym == bodySym.symbol)
                checkParse(body, grammar)
            case BindNode(proxy@NProxy(_, Proxy(sym), _), body: SequenceNode) =>
                assert(proxy.produce == body.symbol.id)
                assert(sym == body.symbol.symbol)
                checkParse(body, grammar)
            case BindNode(NLongest(_, Longest(sym), _), body@BindNode(bodySym, _)) =>
                assert(sym == bodySym.symbol)
                checkParse(body, grammar)
            case BindNode(_: NLookaheadSymbol, body) =>
                // TODO: empty sequence nsymbol id?
                body match {
                    case BindNode(NSequence(_, Sequence(Seq()), Seq()), seq: SequenceNode) =>
                        assert(seq.children.isEmpty) // OK
                    case _ => assert(false)
                }
            case node: SequenceNode =>
                node.childrenAll foreach {
                    checkParse(_, grammar)
                }
            case JoinNode(_, body, join) =>
                checkParse(body, grammar)
                checkParse(join, grammar)
            case _: CyclicBindNode | _: CyclicSequenceNode =>
            // ignore cyclic binds and do nothing
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
        test.correctExampleInputs foreach {
            testCorrect(test, _)
        }
        test.incorrectExampleInputs foreach {
            testIncorrect(test, _)
        }
        test match {
            case samples: AmbiguousExamples =>
                samples.ambiguousExampleInputs foreach {
                    testAmbiguous(test, _)
                }
            case _ =>
        }
    }
}
