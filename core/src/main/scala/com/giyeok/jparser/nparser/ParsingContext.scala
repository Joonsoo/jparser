package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.AcceptCondition.Always
import com.giyeok.jparser.nparser.NGrammar.NAtomicSymbol
import com.giyeok.jparser.nparser.NGrammar.NSymbol
import com.giyeok.jparser.nparser.NGrammar.NSequence

case class SymbolIdAndBeginGen(symbolId: Int, beginGen: Int)(val symbol: NSymbol) {
    def toKernel: Kernel = Kernel(symbolId, 0, beginGen, beginGen)(symbol)
}

object Kernel {
    def lastPointerOf(symbol: NSymbol): Int = symbol match {
        case _: NAtomicSymbol => 1
        case NSequence(_, seq) => seq.length
    }
}
case class Kernel(symbolId: Int, pointer: Int, beginGen: Int, endGen: Int)(val symbol: NSymbol) {
    def shiftGen(gen: Int): Kernel = Kernel(symbolId, pointer, beginGen + gen, endGen + gen)(symbol)
    def initial: Kernel = if (pointer == 0) this else Kernel(symbolId, 0, beginGen, beginGen)(symbol)

    def isInitial: Boolean = pointer == 0
    def isFinal: Boolean =
        pointer == Kernel.lastPointerOf(symbol) ensuring (0 <= pointer && pointer <= Kernel.lastPointerOf(symbol))

    override def toString: String = s"Kernel($symbolId ${symbol.symbol.toShortString}, $pointer, $beginGen..$endGen)"
}
case class State(kernel: Kernel, condition: AcceptCondition) {
    def shiftGen(gen: Int) = State(kernel.shiftGen(gen), condition.shiftGen(gen))
    lazy val initial: State = if (kernel.pointer == 0) this else State(kernel.initial, Always)
    lazy val kernelBase: SymbolIdAndBeginGen = SymbolIdAndBeginGen(kernel.symbolId, kernel.beginGen)(kernel.symbol)

    def isInitial: Boolean = kernel.isInitial
    def isFinal: Boolean = kernel.isFinal

    override val hashCode: Int = (kernel, condition).hashCode()
}

case class Expectation(state: State, expect: SymbolIdAndBeginGen) {
    override val hashCode: Int = (state, expect).hashCode()
}

class ParsingContext(val states: Set[State], val expects: Set[Expectation],
        val expectsByStart: Map[State, Set[Expectation]], val expectsByEnd: Map[SymbolIdAndBeginGen, Set[Expectation]], val statesByBase: Map[SymbolIdAndBeginGen, Set[State]]) {
    // assert(edgesByStart.keySet == nodes && edgesByDest.keySet == nodes)
    // assert((edgesByStart flatMap { _._2 }).toSet subsetOf edges)
    // assert((edgesByDest flatMap { _._2 }).toSet subsetOf edges)
    def addState(newState: State): ParsingContext =
        if (states contains newState) this else {
            if (newState.isInitial) {
                new ParsingContext(states + newState, expects,
                    expectsByStart + (newState -> Set()),
                    expectsByEnd + (newState.kernelBase -> Set()),
                    statesByBase + (newState.kernelBase -> (statesByBase.getOrElse(newState.kernelBase, Set()) + newState)))
            } else {
                new ParsingContext(states + newState, expects,
                    expectsByStart + (newState -> Set()),
                    expectsByEnd,
                    statesByBase + (newState.kernelBase -> (statesByBase.getOrElse(newState.kernelBase, Set()) + newState)))
            }
        }
    def addExpect(expect: Expectation): ParsingContext = {
        val expectsByStart1 = expectsByStart.updated(expect.state, expectsByStart(expect.state) + expect)
        val expectsByEnd1 = expectsByEnd.updated(expect.expect, expectsByEnd(expect.expect) + expect)
        new ParsingContext(states, expects + expect, expectsByStart1, expectsByEnd1, statesByBase)
    }
    def removeStates(removingStates: Set[State]): ParsingContext = {
        // TODO
        // removingStates의 base도 지우기
        val remainingExpects = expects filterNot { expect => removingStates contains expect.state }
        val newEdgesByStart = expectsByStart -- removingStates
        val newEdgesByEnd = expectsByEnd mapValues { _ filterNot { exp => removingStates contains exp.state } }
        val newStatesByBase = statesByBase map { kv => kv._1 -> (kv._2 -- removingStates) } filter { _._2.nonEmpty }
        new ParsingContext(states -- removingStates, remainingExpects, newEdgesByStart, newEdgesByEnd, newStatesByBase)
    }

    // TODO state에서 kernel은 바뀌지 않아야 함. (conditionFunc: AcceptCondition => AcceptCondition) 형태로 바꾸기
    def mapStateCondition(stateFunc: State => State): ParsingContext = {
        val newStatesMap: Map[State, State] = (states map { node => node -> stateFunc(node) }).toMap
        val newStates: Set[State] = newStatesMap.values.toSet
        val newExpectsMap: Map[Expectation, Expectation] = (expects map { edge =>
            edge -> Expectation(newStatesMap(edge.state), edge.expect)
        }).toMap
        val newExpectsByStart = expectsByStart map { kv =>
            newStatesMap(kv._1) -> kv._2
        }
        val newExpectsByEnd = expectsByEnd map { kv =>
            kv._1 -> (kv._2 map { exp => Expectation(newStatesMap(exp.state), exp.expect) })
        }
        val newStatesByBase = statesByBase mapValues { _ map newStatesMap }
        new ParsingContext(newStates, newExpectsMap.values.toSet, newExpectsByStart, newExpectsByEnd, newStatesByBase)
    }
    def filterStates(statePred: State => Boolean): ParsingContext = {
        val statesPredMap: Map[State, Boolean] = (states map { node => node -> statePred(node) }).toMap
        val newStates: Set[State] = (statesPredMap filter { _._2 }).keySet
        removeStates(states -- newStates)
    }
    def merge(other: ParsingContext): ParsingContext = {
        //            def mergeEdgesMap(map: Map[State, Set[Expectation]], merging: Map[State, Set[Expectation]]): Map[State, Set[Expectation]] =
        //                merging.foldLeft(map) { (cc, i) =>
        //                    val (node, edges) = i
        //                    if (!(cc contains node)) cc + (node -> edges) else cc + (node -> (cc(node) ++ edges))
        //                }
        //            Graph(states ++ other.states, expects ++ other.expects, mergeEdgesMap(expectsByStart, other.expectsByStart), mergeEdgesMap(expectsByEnd, other.expectsByEnd))
        ???
    }
    def conditionsOf(kernel: Kernel): Set[AcceptCondition] =
        states collect { case State(`kernel`, condition) => condition }
}
object ParsingContext {
    def apply(states: Set[State], expects: Set[Expectation]): ParsingContext = {
        val _expectsByStart: Map[State, Set[Expectation]] = expects groupBy { _.state }
        val _expectsByEnd: Map[SymbolIdAndBeginGen, Set[Expectation]] = expects groupBy { _.expect }
        val expectsByStart = _expectsByStart ++ ((states -- _expectsByStart.keySet) map { _ -> Set[Expectation]() })
        // states에서 initial인 것들 추려서
        val expectsByEnd = _expectsByEnd ++ (((states filter { _.isInitial } map { _.kernelBase }) -- _expectsByEnd.keySet) map { _ -> Set[Expectation]() })
        val statesByBase = states groupBy { _.kernelBase }
        new ParsingContext(states, expects, expectsByStart, expectsByEnd, statesByBase)
    }
}
