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

    newGraph.progressedNodes.clear()
    newGraph.observingCondSymbolIds.clear()

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
      if (graph.addNode(d, GenAcceptCondition.Always)) {
        newTasks.add(GenParsingTask.Derive(d))
      }
      graph.addEdge(node, d)
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
        val newNode = GenNode(node.symbolId, node.pointer + 1, node.endGen, nextGen)
        if (graph.addProgressedTo(node, newNode, acceptCondition)) {
          if (newNode.pointer < symbol.sequence().length()) {
            newTasks.add(GenParsingTask.Derive(newNode))
          } else {
            newTasks.add(GenParsingTask.Finish(newNode))
          }
        }
      }

      is NGrammar.NExcept -> {
        processAtomicSymbol(GenAcceptCondition.Unless(symbol.except()))
      }

      is NGrammar.NJoin -> {
        processAtomicSymbol(GenAcceptCondition.Unless(symbol.join()))
      }

      is NGrammar.NLongest -> {
        processAtomicSymbol(GenAcceptCondition.NoLongerMatch(symbol.body()))
      }

      is NGrammar.NLookaheadExcept -> {
        processAtomicSymbol(GenAcceptCondition.NotExists(symbol.lookahead()))
      }

      is NGrammar.NLookaheadIs -> {
        processAtomicSymbol(GenAcceptCondition.Exists(symbol.lookahead()))
      }

      is NGrammar.NTerminal -> {
        processAtomicSymbol()
      }
    }
    return newTasks
  }
}
