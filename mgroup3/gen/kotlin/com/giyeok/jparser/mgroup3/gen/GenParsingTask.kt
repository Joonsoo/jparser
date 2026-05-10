package com.giyeok.jparser.mgroup3.gen

import com.giyeok.jparser.NGrammar

sealed class GenParsingTask {
  abstract val node: GenNode

  data class Derive(override val node: GenNode): GenParsingTask()
  data class Finish(override val node: GenNode): GenParsingTask()
  data class Progress(
    override val node: GenNode,
    val acc: GenAcceptCondition
  ): GenParsingTask()
}

class GenParsingTaskRunner(val grammar: NGrammar) {
  // 어떤 symbol이 NRepeat 안에 (직접 또는 간접) 있는지.
  // NRepeat 안에 있는 cond symbol (NExcept, NLookahead, NLongest 등)은 매 iteration마다 새로
  // 시작되어야 하므로 fromNextGen=true로 표시.
  // 단, NRepeat 자체로부터 도달 가능한 모든 symbol을 포함하면 NRepeat 안에서만 사용되는 게 아닌
  // symbol도 포함될 수 있음. 그래서 더 정확하게는 NRepeat의 repeatSeq에서만 도달 가능한 symbol들을 본다.
  val symbolsInCycle: Set<Int> by lazy { computeSymbolsInCycle() }

  private val nsymbolsCache: Map<Int, NGrammar.NAtomicSymbol> by lazy {
    val raw = scala.jdk.javaapi.CollectionConverters.asJava(grammar.nsymbols())
    raw.entries.associate { (it.key as Int) to it.value }
  }
  private val nseqsCache: Map<Int, NGrammar.NSequence> by lazy {
    val raw = scala.jdk.javaapi.CollectionConverters.asJava(grammar.nsequences())
    raw.entries.associate { (it.key as Int) to it.value }
  }

  private fun computeSymbolsInCycle(): Set<Int> {
    // NRepeat의 repeatSeq에서 derive 가능한 symbol들 모음
    val fromRepeat = mutableSetOf<Int>()
    for ((_, sym) in nsymbolsCache) {
      if (sym is NGrammar.NRepeat) {
        collectDerivableSymbols(
          sym.repeatSeq() as Int,
          fromRepeat,
          mutableSetOf(),
          nsymbolsCache,
          nseqsCache,
        )
      }
    }
    return fromRepeat
  }

  private fun collectDerivableSymbols(
    symbolId: Int,
    out: MutableSet<Int>,
    visited: MutableSet<Int>,
    nsymbols: Map<Int, NGrammar.NAtomicSymbol>,
    nseqs: Map<Int, NGrammar.NSequence>,
  ) {
    if (symbolId in visited) return
    visited.add(symbolId)
    out.add(symbolId)
    val sym: NGrammar.NSymbol = nsymbols[symbolId] ?: nseqs[symbolId] ?: return
    when (sym) {
      is NGrammar.NTerminal -> {}
      is NGrammar.NStart ->
        collectDerivableSymbols(sym.produce() as Int, out, visited, nsymbols, nseqs)
      is NGrammar.NNonterminal ->
        sym.produces().foreach { collectDerivableSymbols(it as Int, out, visited, nsymbols, nseqs) }
      is NGrammar.NOneOf ->
        sym.produces().foreach { collectDerivableSymbols(it as Int, out, visited, nsymbols, nseqs) }
      is NGrammar.NProxy ->
        collectDerivableSymbols(sym.produce() as Int, out, visited, nsymbols, nseqs)
      is NGrammar.NRepeat -> {
        collectDerivableSymbols(sym.baseSeq() as Int, out, visited, nsymbols, nseqs)
        collectDerivableSymbols(sym.repeatSeq() as Int, out, visited, nsymbols, nseqs)
      }
      is NGrammar.NExcept -> {
        collectDerivableSymbols(sym.body() as Int, out, visited, nsymbols, nseqs)
        // except symbol 자체는 cond path로만 추적되고 derive 그래프에는 안 들어가지만
        // 일관성을 위해 포함
        collectDerivableSymbols(sym.except() as Int, out, visited, nsymbols, nseqs)
      }
      is NGrammar.NJoin -> {
        collectDerivableSymbols(sym.body() as Int, out, visited, nsymbols, nseqs)
        collectDerivableSymbols(sym.join() as Int, out, visited, nsymbols, nseqs)
      }
      is NGrammar.NLongest ->
        collectDerivableSymbols(sym.body() as Int, out, visited, nsymbols, nseqs)
      is NGrammar.NLookaheadExcept -> {
        collectDerivableSymbols(sym.emptySeqId() as Int, out, visited, nsymbols, nseqs)
        collectDerivableSymbols(sym.lookahead() as Int, out, visited, nsymbols, nseqs)
      }
      is NGrammar.NLookaheadIs -> {
        collectDerivableSymbols(sym.emptySeqId() as Int, out, visited, nsymbols, nseqs)
        collectDerivableSymbols(sym.lookahead() as Int, out, visited, nsymbols, nseqs)
      }
      is NGrammar.NSequence -> {
        val seq = sym.sequence()
        for (i in 0 until seq.size()) {
          collectDerivableSymbols(seq.apply(i) as Int, out, visited, nsymbols, nseqs)
        }
      }
    }
  }

  fun derivedFrom(nodes: Set<GenNode>): GenParsingGraph {
    val graph = GenParsingGraph(
      startNodes = nodes,
      nodes = nodes.toMutableSet(),
      edges = mutableSetOf(),
      edgesByStart = mutableMapOf(),
      edgesByEnd = mutableMapOf(),
      observingCondSymbolIds = mutableSetOf(),
      acceptConditions = nodes.associateWith { GenAcceptCondition.Always }.toMutableMap(),
      progressedNodes = mutableMapOf(),
      finishedNodes = mutableSetOf(),
    )

    val endGen = nodes.map { it.endGen }.toSet()
    check(endGen.size == 1)

    run(graph, endGen.single(), nodes.map { GenParsingTask.Derive(it) }.toSet())
    return graph
  }

  fun progressedFrom(
    graph: GenParsingGraph,
    tasksToProgress: Set<GenNode>,
    nextGen: GenNodeGeneration
  ): GenParsingGraph {
    check(graph.nodes.containsAll(tasksToProgress))
    check(graph.acceptConditions.keys.containsAll(tasksToProgress))
    val newGraph = graph.clone()

    // term progress 단계에서는 progressedNodes를 새로 시작 (term progress 결과만 추적)
    // 단, derive 단계에서 만들어진 progressedNodes 정보는 별도로 보존 (reachables 계산용)
    newGraph.derivePhaseProgressedNodes.putAll(newGraph.progressedNodes)
    newGraph.progressedNodes.clear()
    // observingCondSymbolIds는 derive phase에서 누적된 것을 그대로 유지.
    // (cond path가 path 살아있는 동안 추적되어야 하므로 obs에 포함되어야 한다.)
    // newGraph.observingCondSymbolIds.clear()

    val initTasks = tasksToProgress.map {
      GenParsingTask.Progress(it, GenAcceptCondition.Always)
    }.toSet()
    run(newGraph, nextGen, initTasks)
    return newGraph
  }

//  fun finishedFrom(graph: GenParsingGraph, taskToFinish: GenNode): GenParsingGraph {
//    check(taskToFinish in graph.nodes)
//
//    val newGraph = graph.clone()
//
//    run(newGraph, taskToFinish.endGen, setOf(GenParsingTask.Finish(taskToFinish)))
//    return newGraph
//  }

  fun run(graph: GenParsingGraph, nextGen: GenNodeGeneration, initTasks: Set<GenParsingTask>) {
    val tasks = initTasks.toMutableList()
    while (tasks.isNotEmpty()) {
      val newTasks = when (val next = tasks.removeFirst()) {
        is GenParsingTask.Derive -> derive(graph, next.node)
        is GenParsingTask.Finish -> finish(graph, next.node)
        is GenParsingTask.Progress -> progress(graph, next.node, nextGen, next.acc)
      }
      tasks.addAll(newTasks)
    }
  }

  fun derive(
    graph: GenParsingGraph,
    node: GenNode,
  ): Set<GenParsingTask> {
    val newTasks = mutableSetOf<GenParsingTask>()

    fun addDerive(deriveSymbolId: Int) {
      val d = GenNode(deriveSymbolId, 0, node.endGen, node.endGen)
      val isNew = graph.addNode(d, GenAcceptCondition.Always)
      if (isNew) {
        newTasks.add(GenParsingTask.Derive(d))
      }
      val isNewEdge = graph.addEdge(node, d)
      // d로 들어가는 새 incoming edge가 추가되었을 때, 이미 d 또는 d의 progress 결과가 finish된 적이 있으면
      // 그 finish chain의 progress가 node로도 전파되어야 한다.
      // - d 자체가 finished (예: NSeq empty가 즉시 finish): d의 condition 사용
      // - d의 progress 결과 finished (pointer >= 1): 그 condition 사용
      if (isNewEdge) {
        for (finished in graph.finishedNodes) {
          if (finished.symbolId == d.symbolId && finished.startGen == d.startGen) {
            val cond = graph.acceptConditions[finished] ?: GenAcceptCondition.Always
            newTasks.add(GenParsingTask.Progress(node, cond))
          }
        }
      }
    }

    when (val symbol = grammar.symbolOf(node.symbolId)) {
      is NGrammar.NStart -> {
        addDerive(symbol.produce())
      }

      is NGrammar.NNonterminal -> {
        symbol.produces().foreach {
          addDerive(it as Int)
        }
      }

      is NGrammar.NOneOf -> {
        symbol.produces().foreach {
          addDerive(it as Int)
        }
      }

      is NGrammar.NProxy -> {
        addDerive(symbol.produce())
      }

      is NGrammar.NRepeat -> {
        addDerive(symbol.baseSeq())
        addDerive(symbol.repeatSeq())
      }

      is NGrammar.NSequence -> {
        if (symbol.sequence().isEmpty) {
          newTasks.add(GenParsingTask.Finish(node))
        } else {
          check(node.pointer in 0..<symbol.sequence().length())
          val nextSymbol = symbol.sequence().apply(node.pointer) as Int
          addDerive(nextSymbol)
        }
      }

      is NGrammar.NExcept -> {
        addDerive(symbol.body())
        graph.observingCondSymbolIds.add(symbol.except())
      }

      is NGrammar.NJoin -> {
        addDerive(symbol.body())
        graph.observingCondSymbolIds.add(symbol.join())
      }

      is NGrammar.NLongest -> {
        addDerive(symbol.body())
        graph.observingCondSymbolIds.add(symbol.body())
      }

      is NGrammar.NLookaheadExcept -> {
        addDerive(symbol.emptySeqId())
        graph.observingCondSymbolIds.add(symbol.lookahead())
      }

      is NGrammar.NLookaheadIs -> {
        addDerive(symbol.emptySeqId())
        graph.observingCondSymbolIds.add(symbol.lookahead())
      }

      is NGrammar.NTerminal -> {}
    }
    return newTasks
  }

  fun finish(
    graph: GenParsingGraph,
    node: GenNode,
  ): Set<GenParsingTask> {
    graph.finishedNodes.add(node)

    val newTasks = mutableSetOf<GenParsingTask>()

    fun process(finishPointer: Int) {
      check(node.pointer == finishPointer)
      val condition = graph.acceptConditions[node]!!
      val initNode = GenNode(node.symbolId, 0, node.startGen, node.startGen)
      graph.edgesByEnd[initNode]?.let { edges ->
        for (toProg in edges) {
          newTasks.add(GenParsingTask.Progress(toProg, condition))
        }
      }
    }

    when (val symbol = grammar.symbolOf(node.symbolId)) {
      is NGrammar.NStart -> {}
      is NGrammar.NNonterminal -> process(1)
      is NGrammar.NOneOf -> process(1)
      is NGrammar.NProxy -> process(1)
      is NGrammar.NRepeat -> process(1)
      is NGrammar.NSequence -> process(symbol.sequence().length())
      is NGrammar.NExcept -> process(1)
      is NGrammar.NJoin -> process(1)
      is NGrammar.NLongest -> process(1)
      is NGrammar.NLookaheadExcept -> process(1)
      is NGrammar.NLookaheadIs -> process(1)
      is NGrammar.NTerminal -> process(1)
    }
    return newTasks
  }

  fun progress(
    graph: GenParsingGraph,
    node: GenNode,
    nextGen: GenNodeGeneration,
    acceptCondition: GenAcceptCondition,
  ): MutableSet<GenParsingTask> {
    val newTasks = mutableSetOf<GenParsingTask>()

    fun processAtomicSymbol(newAcceptConditions: GenAcceptCondition = GenAcceptCondition.Always) {
      check(node.pointer == 0)
      val after = GenNode(node.symbolId, 1, node.endGen, nextGen)
      val newAcc = GenAcceptCondition.And.from(acceptCondition, newAcceptConditions)
      if (graph.addProgressedTo(node, after, newAcc)) {
        newTasks.add(GenParsingTask.Finish(after))
      }
    }

    when (val symbol = grammar.symbolOf(node.symbolId)) {
      is NGrammar.NStart -> processAtomicSymbol()
      is NGrammar.NNonterminal -> processAtomicSymbol()
      is NGrammar.NOneOf -> processAtomicSymbol()
      is NGrammar.NProxy -> processAtomicSymbol()
      is NGrammar.NRepeat -> processAtomicSymbol()
      is NGrammar.NSequence -> {
        check(node.pointer in 0..<symbol.sequence().length())
        // NSequence는 시퀀스 시작점(startGen)을 유지하면서 진행함.
        // pointer 0인 경우 startGen=endGen이라 어느 쪽을 써도 동일.
        // pointer > 0인 경우 startGen이 시퀀스의 시작 시점이며 그대로 유지.
        val newNode = GenNode(node.symbolId, node.pointer + 1, node.startGen, nextGen)
        // node 자체의 acceptCondition (예: 이전 sym에서 propagate된 NotExists 등)도 합쳐야 함.
        val nodeCondition = graph.acceptConditions[node] ?: GenAcceptCondition.Always
        val newAcc = GenAcceptCondition.And.from(nodeCondition, acceptCondition)
        if (graph.addProgressedTo(node, newNode, newAcc)) {
          if (newNode.pointer < symbol.sequence().length()) {
            newTasks.add(GenParsingTask.Derive(newNode))
          } else {
            newTasks.add(GenParsingTask.Finish(newNode))
          }
        }
      }

      is NGrammar.NExcept -> {
        // condition의 startGen 은 이 atomic symbol 의 derive 시점 = node.startGen
        processAtomicSymbol(GenAcceptCondition.Unless(symbol.except(), node.startGen))
      }

      is NGrammar.NJoin -> {
        processAtomicSymbol(GenAcceptCondition.OnlyIf(symbol.join(), node.startGen))
      }

      is NGrammar.NLongest -> {
        processAtomicSymbol(GenAcceptCondition.NoLongerMatch(symbol.body(), node.startGen))
      }

      is NGrammar.NLookaheadExcept -> {
        processAtomicSymbol(GenAcceptCondition.NotExists(symbol.lookahead(), node.startGen))
      }

      is NGrammar.NLookaheadIs -> {
        processAtomicSymbol(GenAcceptCondition.Exists(symbol.lookahead(), node.startGen))
      }

      is NGrammar.NTerminal -> {
        processAtomicSymbol()
      }
    }
    return newTasks
  }
}
