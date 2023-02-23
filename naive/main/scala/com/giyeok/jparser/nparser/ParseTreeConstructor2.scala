package com.giyeok.jparser.nparser

import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.NGrammar._
import com.giyeok.jparser._
import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.ParseTreeConstructor2.{KernelCore, Kernels}
import com.giyeok.jparser.nparser.ParsingContext.Graph

// Accept condition을 모두 통과한 node들의 커널들만. 커널사이의 관계는 grammar를 통해 유추하고 생략함.
class ParseTreeConstructor2[R <: ParseResult](resultFunc: ParseResultFunc[R])(grammar: NGrammar)(input: Seq[Input], history: Seq[Kernels]) {

  def reconstruct(): Option[R] =
    reconstruct(Kernel(grammar.startSymbol, 1, 0, input.length))

  def reconstruct(kernel: Kernel): Option[R] =
    if (kernel.pointer > 0 && history(kernel.endGen).kernels.contains(kernel)) Some(reconstruct(kernel, Set())) else None

  private def reconstruct(kernel: Kernel, traces: Set[KernelCore]): R = {
    val gen = kernel.endGen

    def reconstruct0(child: Kernel): R = {
      val newTraces: Set[KernelCore] = if ((kernel.beginGen, gen) != (child.beginGen, child.endGen)) Set()
      else traces + KernelCore(kernel.symbolId, kernel.pointer)
      reconstruct(child, newTraces)
    }

    grammar.symbolOf(kernel.symbolId) match {
      case symbol: NAtomicSymbol if traces contains KernelCore(kernel.symbolId, kernel.pointer) =>
        resultFunc.cyclicBind(kernel.beginGen, gen, symbol)

      case symbol: NSequence if traces contains KernelCore(kernel.symbolId, kernel.pointer) =>
        resultFunc.sequence(kernel.beginGen, gen, symbol, kernel.pointer)

      case symbol@NSequence(_, _, sequence) =>
        if (sequence.isEmpty) {
          assert(kernel.pointer == 0 && kernel.beginGen == kernel.endGen && kernel.beginGen == gen)
          resultFunc.bind(kernel.beginGen, gen, symbol, resultFunc.sequence(kernel.beginGen, kernel.endGen, symbol, 0))
        } else if (kernel.pointer == 0) {
          assert(kernel.beginGen == kernel.endGen)
          resultFunc.sequence(kernel.beginGen, kernel.endGen, symbol, 0)
        } else {
          val (symbolId, prevPointer) = (kernel.symbolId, kernel.pointer - 1)
          val prevKernels = history(gen).kernels filter { kern =>
            (kern.symbolId == symbolId) && (kern.pointer == prevPointer) && (kern.beginGen == kernel.beginGen)
          }
          val trees = prevKernels.toSeq.sortBy(_.tuple) flatMap { prevKernel =>
            val childKernel = Kernel(sequence(prevPointer), 1, prevKernel.endGen, gen)
            if (history(gen).kernels contains childKernel) {
              val precedingTree = reconstruct0(Kernel(kernel.symbolId, prevPointer, kernel.beginGen, prevKernel.endGen))
              val childTree = reconstruct0(childKernel)
              // println(s"preceding: $precedingTree")
              // println(s"child: $childTree")
              Some(resultFunc.append(precedingTree, childTree))
            } else None
          }
          val appendedSeq = resultFunc.merge(trees)
          if (kernel.pointer == sequence.length) resultFunc.bind(kernel.beginGen, gen, symbol, appendedSeq) else appendedSeq
        }

      case symbol@NJoin(_, _, body, join) =>
        assert(kernel.pointer == 1)
        val bodyKernel = Kernel(body, 1, kernel.beginGen, kernel.endGen)
        val joinKernel = Kernel(join, 1, kernel.beginGen, kernel.endGen)
        val bodyTree = reconstruct0(bodyKernel)
        val joinTree = reconstruct0(joinKernel)
        resultFunc.join(kernel.beginGen, kernel.endGen, symbol, bodyTree, joinTree)

      case symbol: NTerminal =>
        resultFunc.bind(kernel.beginGen, kernel.endGen, symbol,
          resultFunc.terminal(kernel.beginGen, input(kernel.beginGen)))

      case symbol: NAtomicSymbol =>
        assert(kernel.pointer == 1)

        def lastKernel(symbolId: Int) =
          Kernel(symbolId, Kernel.lastPointerOf(grammar.symbolOf(symbolId)), kernel.beginGen, gen)

        // assert(finishes(gen).edgesByStart(prevKernel) forall { _.isInstanceOf[SimpleKernelEdge] })
        // TODO history(gen).finished 중에서 kernel에서 derive된 것들이고 (beginGen==kernel.beginGen && endGen==gen)인 것들에 대해서 reconstruct
        val bodyKernels: Set[Kernel] = grammar.nsymbols(kernel.symbolId) match {
          case deriver: NSimpleDerive => deriver.produces.map(lastKernel)
          case NGrammar.NExcept(_, _, body, _) => Set(lastKernel(body))
          case NGrammar.NLongest(_, _, body) => Set(lastKernel(body))
          case symbol: NGrammar.NLookaheadSymbol => Set(lastKernel(symbol.emptySeqId))
          case _: NTerminal | _: NJoin => assert(false); ???
        }
        val validKernels = history(gen).kernels intersect bodyKernels
        assert(validKernels.nonEmpty)
        val bodyTrees = validKernels.toSeq.sortBy(_.tuple) map {
          bodyKernel => reconstruct0(bodyKernel)
        }
        assert(bodyTrees.nonEmpty)
        resultFunc.bind(kernel.beginGen, kernel.endGen, symbol, resultFunc.merge(bodyTrees))
    }
  }
}

object ParseTreeConstructor2 {

  case class Kernels(kernels: Set[Kernel])

  case class KernelCore(symbolId: Int, pointer: Int)

  def kernelsFrom(history: Seq[Graph], conditionFinal: Map[AcceptCondition, Boolean]): Seq[Kernels] =
    history.map(_.filterNode(node => conditionFinal(node.condition)))
      .map(graph => Kernels(graph.nodes.map(_.kernel)))

  def constructor[R <: ParseResult](resultFunc: ParseResultFunc[R])(grammar: NGrammar)
                                   (input: Seq[Input], history: Seq[Graph], conditionFinal: Map[AcceptCondition, Boolean]): ParseTreeConstructor2[R] =
    new ParseTreeConstructor2[R](resultFunc)(grammar)(input, kernelsFrom(history, conditionFinal))

  def forestConstructor(grammar: NGrammar)
                       (input: Seq[Input], history: Seq[Graph], conditionFinal: Map[AcceptCondition, Boolean]): ParseTreeConstructor2[ParseForest] =
    constructor(ParseForestFunc)(grammar)(input, history, conditionFinal)
}
