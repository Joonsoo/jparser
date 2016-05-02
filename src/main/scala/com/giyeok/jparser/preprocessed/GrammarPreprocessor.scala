package com.giyeok.jparser.preprocessed

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Grammar.GrammarChecker
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.Kernels
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.ParseTree
import com.giyeok.jparser.Parser
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.Inputs._
import com.giyeok.jparser.Kernels._
import com.giyeok.jparser.ParseTree._
import PreprocessedGrammar._

class GrammarPreprocessor(grammar: Grammar) {

    def allKernels: Set[Kernel] = grammar.usedSymbols flatMap { Kernel.allKernelsOf _ }
    def allDerivableKernels: Set[Kernel] = allKernels filter { _.derivable }

    def derive(kernel: NontermKernel[Nonterm]): (Set[Seq[ParseNode[Symbol]]], Map[TermGroupDesc, KernelExpansion]) = {
        sealed trait ExpandTask
        case class Derive(node: NontermKernelNode, path: Seq[NontermKernelNode]) extends ExpandTask
        case class Lift(node: NontermKernelNode, by: ParseNode[Symbol]) extends ExpandTask

        sealed trait KernelNode
        case class TerminalNode(kernel: TerminalKernel) extends KernelNode
        sealed trait NontermKernelNode extends KernelNode { val kernel: NontermKernel[Nonterm] }
        case class AtomicKernelNode[T <: AtomicSymbol with Nonterm](kernel: AtomicNontermKernel[T]) extends NontermKernelNode
        case class NonAtomicKernelNode[T <: NonAtomicSymbol with Nonterm](kernel: NonAtomicNontermKernel[T], progress: ParsedSymbolsSeq[T]) extends NontermKernelNode
        object NontermKernelNode {
            def apply(kernel: NontermKernel[Nonterm]): NontermKernelNode =
                kernel match {
                    case kernel: AtomicNontermKernel[_] => AtomicKernelNode(kernel)
                    case kernel: NonAtomicNontermKernel[_] => NonAtomicKernelNode(kernel, ParsedSymbolsSeq(kernel.symbol, List(), List()))
                }
        }

        // empty kernel은 expand시 바로바로 처리해버려서 필요 없음

        case class ExpandResult(nodes: Set[KernelNode], edges: Set[(KernelNode, KernelNode)], derivations: Map[KernelNode, Derivation], cycles: Map[KernelNode, Set[Seq[KernelNode]]]) {
            def newNode(newNode: KernelNode): ExpandResult =
                ExpandResult(nodes + newNode, edges, derivations, cycles)
            def newNodeEdge(newNode: KernelNode, newEdge: (KernelNode, KernelNode)): ExpandResult =
                ExpandResult(nodes + newNode, edges + newEdge, derivations, cycles)
            def newDerivation(node: KernelNode, derivation: Derivation): ExpandResult =
                ExpandResult(nodes, edges, derivations + (node -> derivation), cycles)
            def newCycle(node: KernelNode, path: Seq[KernelNode]): ExpandResult =
                ExpandResult(nodes, edges, derivations, cycles + (node -> (cycles.getOrElse(node, Set()) + path)))
        }

        def expand(queue: List[ExpandTask], cc: ExpandResult): ExpandResult = {
            queue match {
                case Derive(node: NontermKernelNode, path) +: rest =>
                    val idx = path indexOf node
                    if (idx >= 0) {
                        expand(rest, cc.newCycle(node, path.slice(idx + 1, path.length)))
                    } else {
                        case class Qcc(queue: List[ExpandTask], cc: ExpandResult) {
                            def expand(derive: Kernel): Qcc = {
                                derive match {
                                    case EmptyKernel => Qcc(Lift(node, ParsedEmpty(Empty)) +: queue, cc)
                                    case term: TerminalKernel =>
                                        val newNode = TerminalNode(term)
                                        Qcc(queue, cc.newNodeEdge(newNode, (node, newNode)))
                                    case nonterm: NontermKernel[_] =>
                                        val newNode = NontermKernelNode(nonterm)
                                        Qcc(Derive(newNode, path :+ node) +: queue, cc.newNodeEdge(newNode, (node, newNode)))
                                }
                            }
                            def create(derive: Kernel): Qcc = {
                                derive match {
                                    case EmptyKernel => this // nothing to do
                                    case term: TerminalKernel =>
                                        val newNode = TerminalNode(term)
                                        Qcc(queue, cc.newNode(newNode))
                                    case nonterm: NontermKernel[_] =>
                                        val newNode = NontermKernelNode(nonterm)
                                        Qcc(Derive(newNode, Seq()) +: queue, cc.newNode(newNode))
                                }
                            }
                        }

                        val derivation = node.kernel.derive(grammar)
                        val initialQcc = Qcc(rest, cc.newDerivation(node, derivation))
                        derivation match {
                            case EmptyDerivation =>
                                // should never happen
                                throw new AssertionError("EmptyDerivation")
                            case SymbolDerivation(derives) =>
                                // derives중 empty kernel이 있으면 바로 empty lift가 되고
                                // terminal이 있으면 nodes와 edges에 추가해주고
                                // nonterminal이 있으면 nodes와 edges에 추가하고 Derive task를 큐에 추가한다
                                val qcc = derives.foldLeft(initialQcc) { (qcc, derive) =>
                                    qcc.expand(derive)
                                }
                                expand(qcc.queue, qcc.cc)
                            case JoinDerivation(derive, join) =>
                                val qcc = initialQcc.expand(derive).expand(join)
                                expand(qcc.queue, qcc.cc)
                            case TempLiftBlockableDerivation(derive, blockTrigger) =>
                                val qcc = initialQcc.expand(derive).create(blockTrigger)
                                expand(qcc.queue, qcc.cc)
                            case RevertableDerivation(derive, revertTrigger) =>
                                val qcc = initialQcc.expand(derive).create(revertTrigger)
                                expand(qcc.queue, qcc.cc)
                            case DeriveRevertableDerivation(derive, deriveRevertTrigger) =>
                                val qcc = initialQcc.expand(derive).expand(deriveRevertTrigger)
                                expand(qcc.queue, qcc.cc)
                            case reserved: ReservedLiftRevertableDerivation =>
                                val qcc = initialQcc.expand(reserved.derive)
                                expand(qcc.queue, qcc.cc)
                        }
                    }
                case Lift(node: AtomicKernelNode[_], by) +: rest =>
                    val lifted = ParsedSymbol[AtomicSymbol](node.kernel.symbol, by)
                    // kernel로 들어오는 incoming node들에 Lift(incoming, lifted) 적용
                    assert(node.kernel.lifted.finishable)
                    ???
                case Lift(node: NonAtomicKernelNode[_], by) +: rest =>
                    val (newKernel, newProgress) = node.kernel.lifted(node.progress, by)
                    // kernel로 들어오는 incoming node들에 Lift(incoming, lifted) 적용
                    ???
                case List() => cc
            }
        }
        val initialNode: NontermKernelNode = NontermKernelNode(kernel)
        expand(List(Derive(initialNode, Seq())), ExpandResult(Set(initialNode), Set(), Map(), Map()))
        ???
    }

    def startingExpansion: AtomicKernelExpansion = {
        ???
    }

    def kernelExpansion(kernel: Kernel): KernelExpansion = {
        ???
    }

    def preprocessed = PreprocessedGrammar(startingExpansion, allDerivableKernels map { kernel => (kernel -> kernelExpansion(kernel)) } toMap)
}
