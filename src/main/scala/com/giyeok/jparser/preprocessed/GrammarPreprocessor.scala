package com.giyeok.jparser.preprocessed

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Grammar.GrammarChecker
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.Kernels
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.ParseTree
import com.giyeok.jparser.deprecated.Parser
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.Inputs._
import com.giyeok.jparser.Kernels._
import com.giyeok.jparser.ParseTree._
import PreprocessedGrammar._

class GrammarPreprocessor(grammar: Grammar) extends Parser(grammar) {

    def allKernels: Set[Kernel] = grammar.usedSymbols flatMap { Kernel.allKernelsOf _ }
    def allDerivableKernels: Set[Kernel] = allKernels filter { _.derivable }

    def termGroupsOf(terminals: Set[Terminal]): Set[TermGroupDesc] = {
        import Symbols.Terminals._

        val charTerms: Set[CharacterTermGroupDesc] = terminals collect { case x: CharacterTerminal => TermGroupDesc.descOf(x) }
        val virtTerms: Set[VirtualTermGroupDesc] = terminals collect { case x: VirtualTerminal => TermGroupDesc.descOf(x) }

        val charIntersects: Set[CharacterTermGroupDesc] = charTerms flatMap { term1 =>
            charTerms collect {
                case term2 if term1 != term2 => term1 intersect term2
            } filterNot { _.isEmpty }
        }
        val virtIntersects: Set[VirtualTermGroupDesc] = virtTerms flatMap { term1 =>
            virtTerms collect {
                case term2 if term1 != term2 => term1 intersect term2
            } filterNot { _.isEmpty }
        }

        // charIntersects foreach { d => println(d.toShortString) }
        // virtIntersects foreach { d => println(d.toShortString) }

        val charTermGroups = (charTerms map { term =>
            charIntersects.foldLeft(term) { _ - _ }
        }) ++ charIntersects
        val virtTermGroups = (virtTerms map { term =>
            virtIntersects.foldLeft(term) { _ - _ }
        }) ++ virtIntersects

        charTermGroups ++ virtTermGroups
    }

    def derive(startKernel: NontermKernel[Nonterm]): (Set[Seq[ParseNode[Symbol]]], Map[TermGroupDesc, KernelExpansion]) = {
        val startNode: NonterminalNode = SymbolProgress(startKernel, 0)
        val ctx = ParsingContext.fromKernel(startKernel)

        val termGroups: Set[TermGroupDesc] = termGroupsOf(ctx.terminalNodes map { _.kernel.symbol })

        // 1. ctx.nodes, ctx.edges 에서 싸이클 제거
        //   - 싸이클이 발견되면, 발견된 싸이클에서 startKernel에 가장 가까운 노드로 들어오는 엣지를 제거하고
        //   - 싸이클에서 startKernel에 가장 가까운 노드에 cycle 경로를 별도로 저장해둔다
        //   - 그러면 ctx의 그래프가 startNode에서 모든 지점에 reachable한 DAG가 됨
        // 2. ctx.nodes의 각 노드에서 도달 가능한 terminal을 기록한다
        //   - 결과는 Map[Node, Set[TerminalNode]]의 형태가 될 것
        // 3. termGroups의 각 Term Group에 대해 ctx.nodes 중 해당 Term Group이 속하는 terminal node로 도달 가능한 노드들만 추려서 subgraph를 만든다
        // 4. startNode에서 시작해서 추려진 subgraph를 SubDerive를 만들어 TermGroup->SubDerive 로 저장한다
        // 5. 3-4번 과정을 모든 termGroups의 term group에 대해 반복한다

        // startNode에 cycle 정보가 있을 수 있는데, 이 정보는 버려진다
        // 다른 kernel에서 이 노드를 생성할 때 거기서 필요하면 cycle 정보를 기록해서 사용하기 때문
        // 할 수 있으면 다른 kernel에서 이 노드를 생성할 때 cycle 정보가 잘 들어가는지 확인해봐도 좋겠다

        val immediateLiftings: Set[ParseNode[Symbol]] = ctx.liftings collect {
            case lifting: NonterminalLifting[_] if lifting.before == startNode && lifting.after.derivedGen == 0 && lifting.after.kernel.finishable => lifting.after.parsed.get
        }
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
