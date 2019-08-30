package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar._
import com.giyeok.jparser.parsergen.utils.TermGrouper
import com.giyeok.jparser.utils.Memoize

// 현재 stack top이 replace로 바뀐 다음 following이 그 뒤에 붙음
// pendingFinishReplace.nonEmpty인 경우 following을 붙이는 대신 stack top을 finish할 수도 있었다는 것을 의미하는데,
// 만약 following이 다음 term을 받지 못해서 following을 스택에서 제거하고 pendingFinish를 진행하는 경우 stack top을
// pendingFinishReplace로 바꿔서 진행해야 함.
case class Following(following: AKernelSet, pendingFinishReplace: AKernelSet)

case class GraphChange(replacePrev: AKernelSet, following: Option[Following], simulationResult: ParsingTaskSimulationResult)

class GrammarAnalyzer(val grammar: NGrammar) {
    lazy val nullableSymbols: Set[Int] = {
        val initialNullables: Set[Int] =
            (grammar.nsequences filter (_._2.sequence.isEmpty)).keySet ++
                (grammar.nsymbols filter (_._2.isInstanceOf[NLookaheadSymbol])).keySet

        def traverse(cc: Set[Int]): Set[Int] = {
            val newNullableSeqs = (grammar.nsequences filter {
                _._2.sequence forall cc.contains
            }).keySet -- cc
            val newNullableSyms = (grammar.nsymbols filter {
                _._2 match {
                    case simpleDerive: NSimpleDerive => (simpleDerive.produces intersect cc).nonEmpty
                    case except: NExcept => cc contains except.body
                    case join: NJoin => (cc contains join.body) && (cc contains join.join)
                    case longest: NLongest => cc contains longest.body
                    case _: NLookaheadSymbol => true
                    case _: NTerminal => false
                }
            }).keySet -- cc
            if (newNullableSeqs.isEmpty && newNullableSyms.isEmpty) cc else traverse(cc ++ newNullableSeqs ++ newNullableSyms)
        }

        traverse(initialNullables)
    }

    private val deriveGraphMemo = Memoize[AKernel, AKernelGenGraph]()
    private val memoReachableTermSymbolIds = Memoize[AKernel, Set[Int]]()

    def deriveGraphFrom(kernel: AKernel): AKernelGenGraph = deriveGraphMemo(kernel) {
        val start = AKernelGen.from(kernel, 0, 0)
        val initGraph = AKernelGenGraph(Set(), Set(), Map(), Map()).addNode(start)
        val simulation = new ParsingTaskSimulator(grammar).simulate(initGraph, List(DeriveTask(start)), Set())

        val nodesMap = (simulation.nextGraph.nodes map { k => k -> AKernelGen(k.symbolId, k.pointer, 0, 0) }).toMap
        val edges = simulation.nextGraph.edges map { e => AKernelGenEdge(nodesMap(e.start), nodesMap(e.end)) }

        val deriveGraph0 = nodesMap.values.foldLeft(AKernelGenGraph.emptyGraph) { (m, i) => m.addNode(i) }
        edges.foldLeft(deriveGraph0) { (m, i) => m.addEdge(i) }
    }

    // NTerminal의 ID들을 return
    def reachableTermSymbolIdsFrom(kernel: AKernel): Set[Int] = memoReachableTermSymbolIds(kernel) {
        val deriveGraph = deriveGraphFrom(kernel)
        deriveGraph.nodes filter (k => grammar.symbolOf(k.symbolId).isInstanceOf[NTerminal]) map (_.symbolId)
    }

    def termSymbolsFrom(kernelSet: AKernelSet): Set[NTerminal] = {
        val termSymbolIds = kernelSet.items flatMap reachableTermSymbolIdsFrom
        termSymbolIds map (grammar.symbolOf(_).asInstanceOf[NTerminal])
    }

    def acceptableTerms(kernelSet: AKernelSet): Set[CharacterTermGroupDesc] = {
        val termSymbols = termSymbolsFrom(kernelSet) map (_.symbol)
        TermGrouper.termGroupsOf(termSymbols) map (_.asInstanceOf[CharacterTermGroupDesc])
    }

    private val termChangesMemo = Memoize[(AKernelSet, CharacterTermGroupDesc), GraphChange]()
    private val edgeChangesMemo = Memoize[(AKernelSet, AKernelSet), GraphChange]()

    // kernelSet에 term에 속한 글자가 들어왔을때 그래프의 변화.
    // - replace는 kernelSet의 커널들 중 term을 받을 수 있는 것들이고,
    // - append는 replace 커널셋 뒤에 붙을 커널셋.
    //   - append가 None이면 붙을 게 없고 kernelSet이 바로 finish된단 의미.
    //   - append가 None이 아니고 append._2는 finishable이란 의미로, true이면 append되는 kernelset이 finish 가능함을 의미
    // - 사실 termChanges는 edgeChanges에서 nextKernelSet이 reachableTermSymbolsFrom(kernelSet) 중 term을 받을 수 있는
    // 커널셋을 nextKernelSet으로 주었을 때 나오는 결과의 다른 형태
    def termChanges(kernelSet: AKernelSet, term: CharacterTermGroupDesc): GraphChange = termChangesMemo(kernelSet, term) {
        val termSymbols = kernelSet.items flatMap reachableTermSymbolIdsFrom filter { symbolId =>
            grammar.symbolOf(symbolId).asInstanceOf[NTerminal].symbol.acceptTermGroup(term)
        }
        val termKernelSets = termSymbols map { termSymbolId =>
            AKernel(termSymbolId, 0)
        }
        edgeChanges(kernelSet, AKernelSet(termKernelSets))
    }

    // prevKernelSet -> nextKernelSet 이 이어져 있을 때 nextKernelSet이 finish되는 경우의 graph change를 반환
    // - nextKernelSet은 finish(core jparser식 표현으로는 progress)되므로 즉시 탈락되고 prevKernelSet이 stack top이 된다고 봐야함
    // - replacePrev는 prevKernelSet 중 nextKernelSet 의 커널 중 하나라도 도달가능한 커널들만 추린 것.
    // - nextKernelSet의 모든 커널이 progress된 경우, progress/finish가 replacedPrevKernelSet까지 도달하지 못한 것들이
    //   있으면 그런 커널들이 following.following으로 반환
    // - 만약 following.follwing이 non empty인데 progress/finish가 replacedPrevKernelSet의 일부로 도달하는 경우, 그렇게
    //   도달해서 progress되어야 하는 커널들은 following.pendingFinishReplace으로 반환
    def edgeChanges(prevKernelSet: AKernelSet, nextKernelSet: AKernelSet): GraphChange = edgeChangesMemo(prevKernelSet, nextKernelSet) {
        // DeriveGraph에는 AKernel pointer=0으로 되어 있으므로
        val nextKernelSet0 = nextKernelSet.items map (k => AKernelGen(k.symbolId, 0, 0, 0))

        val deriveGraphs = (prevKernelSet.items map { prevKernel =>
            // 필요한건 deriveGraphFrom(prevKernel)에서 nextKernelSet로 도달 가능한 노드들만 추린 subgraph지만 굳이..?
            prevKernel -> deriveGraphFrom(prevKernel)
        }).toMap

        // val replacePrev = (deriveGraphs filterNot { p => (p._2.nodes intersect nextKernelSet0).isEmpty }).keySet
        val replacePrev = (deriveGraphs filter { p =>
            val (startKernel, deriveGraph) = p

            var visited = Set[AKernelGen]()

            // progress된 노드들도 모두 이어져있다고 가정했을 때 parent->child가 reachable한지 여부 반환
            // deriveGraph에선 어차피 created, updated가 둘다 항상 0이므로 간단히 reachability 조사
            def canBeInfluencedBy(queue: List[AKernelGen]): Boolean = queue match {
                case head +: rest =>
                    if (nextKernelSet0 contains head) true else {
                        val derived0 = (deriveGraph.edgesByStart(head) map (_.end)) flatMap { d =>
                            (0 until grammar.lastPointerOf(d.symbolId) map { p => AKernelGen(d.symbolId, p, 0, 0) }).toSet
                        }
                        val derived = (derived0 intersect deriveGraph.nodes) -- visited
                        if ((derived intersect nextKernelSet0).nonEmpty) true else {
                            visited ++= derived
                            canBeInfluencedBy(derived.toList ++ rest)
                        }
                    }
                case List() => false
            }

            canBeInfluencedBy(List(AKernelGen.from(startKernel, 0, 0)))
        }).keySet

        assert(replacePrev subsetOf prevKernelSet.items)

        val baseGraph = deriveGraphs.values.foldLeft(AKernelGenGraph.emptyGraph) { (m, i) =>
            m.merge(i)
        }

        // mergedSubgraph에서 nextKernelSet에 속한 커널들이 progress된다고 가정했을 때 발생할 progress operation들을 수집
        // 그중 replacePrev에 대한 progress인 것(실제로 progress된 이후, 즉 pointer+=1된 것들) -> following.following
        // 아닌 것들에 대한 progress(progress되기 전 상태로) -> following.pendingFinishReplace
        // 만약 following.following.isEmpty이면 replacePrev == following.pendingFinishReplace일 것이고, following을 None으로 리턴
        //   --> 이 때 replacePrev == following.pendingFinishReplace가 맞나?

        val validNextKernelGens = nextKernelSet.items filter { k =>
            baseGraph.nodes contains AKernelGen(k.symbolId, 0, 0, 0)
        } map { k =>
            AKernelGen(k.symbolId, k.pointer, 0, 0)
        }
        val initiatingProgressTasks = validNextKernelGens.toList map ProgressTask
        val boundaryTasks: Set[Task] = replacePrev map { k => ProgressTask(AKernelGen.from(k, 0, 0)) }
        val simulationResult = new ParsingTaskSimulator(grammar).simulate(baseGraph, initiatingProgressTasks, boundaryTasks)

        // simulation.updateMap.get(replacePrev)
        // simulationResult.progressTasks에서 initiatingProgressTasks는 제외
        val progressTasks = simulationResult.progressTasks filter (_.node.created == 0)
        val (pendingFinishReplace, appending) = progressTasks map (_.node.kernel) partition replacePrev.contains
        // appending에는 simulationResult.nullableProgressTasks 중 sequence progress 추가
        val seqAppending = appending filter { k =>
            grammar.symbolOf(k.symbolId).isInstanceOf[NSequence]
        }

        val notFinishedSeqAppendings = seqAppending map { k =>
            AKernel(k.symbolId, k.pointer + 1)
        } filter { k =>
            k.pointer < grammar.lastPointerOf(k.symbolId)
        }

        val following = if (notFinishedSeqAppendings.isEmpty) {
            // boundaryTasks때문에 prevKernelSet 사이에 derive 관계가 있는 경우 replacePrev != pendingFinishReplace일 수 있음
            // assert(replacePrev == pendingFinishReplace)
            assert(pendingFinishReplace subsetOf replacePrev)
            None
        } else {
            Some(Following(AKernelSet(notFinishedSeqAppendings), AKernelSet(pendingFinishReplace)))
        }
        GraphChange(AKernelSet(replacePrev), following, simulationResult)
    }
}
