package com.giyeok.jparser.mgroup3.gen

import com.giyeok.jparser.NGrammar

sealed class GenParsingTask {
  data class Derive(val node: GenNode): GenParsingTask()
  data class Progress(val node: GenNode, val acc: Set<GenAcceptCondition>): GenParsingTask()
  data class Finish(val node: GenNode): GenParsingTask()
}

class GenParsingTaskRunner(val grammar: NGrammar) {
  fun derivedFrom(node: GenNode): GenParsingGraph {
    val graph = GenParsingGraph(
      node,
      mutableSetOf(node),
      mutableSetOf(),
      mutableMapOf(),
      mutableMapOf(),
      mutableSetOf(),
      mutableMapOf(),
      mutableMapOf(),
    )

    run(graph, GenParsingTask.Derive(node))
    return graph
  }

  fun progressedFrom(graph: GenParsingGraph, taskToProgress: GenNode): GenParsingGraph {
    val newGraph = graph.clone()

    run(newGraph, GenParsingTask.Progress(taskToProgress, setOf()))
    return newGraph
  }

  fun finishedFrom(graph: GenParsingGraph, taskToFinish: GenNode): GenParsingGraph {
    val newGraph = graph.clone()

    run(newGraph, GenParsingTask.Finish(taskToFinish))
    return newGraph
  }

  fun run(graph: GenParsingGraph, initTask: GenParsingTask) {
    val tasks = mutableListOf(initTask)
    while (tasks.isNotEmpty()) {
      val next = tasks.removeFirst()
      val newTasks = when (next) {
        is GenParsingTask.Derive -> derive(graph, next.node)
        is GenParsingTask.Progress -> progress(graph, next.node, next.acc)
        is GenParsingTask.Finish -> finish(graph, next.node)
      }
      tasks.addAll(newTasks)
    }
  }

  fun derive(graph: GenParsingGraph, node: GenNode): Set<GenParsingTask> {
    val newTasks = mutableSetOf<GenParsingTask>()

    fun addDerive(deriveSymbolId: Int) {
      val d = GenNode(deriveSymbolId, 0)
      if (graph.addNode(d)) {
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

  fun progress(
    graph: GenParsingGraph,
    node: GenNode,
    acceptConditions: Set<GenAcceptCondition>
  ): MutableSet<GenParsingTask> {
    val newTasks = mutableSetOf<GenParsingTask>()

    fun processAtomicSymbol(newAcceptConditions: Set<GenAcceptCondition>) {
      check(node.pointer == 0)
      val newNode = GenNode(node.symbolId, 1)
      graph.addProgressedTo(node, newNode, acceptConditions + newAcceptConditions)
      newTasks.add(GenParsingTask.Finish(newNode))
    }

    when (val symbol = grammar.symbolOf(node.symbolId)) {
      is NGrammar.NStart -> processAtomicSymbol(setOf())
      is NGrammar.NNonterminal -> processAtomicSymbol(setOf())
      is NGrammar.NOneOf -> processAtomicSymbol(setOf())
      is NGrammar.NProxy -> processAtomicSymbol(setOf())
      is NGrammar.NRepeat -> processAtomicSymbol(setOf())
      is NGrammar.NSequence -> {
        check(node.pointer in 0..<symbol.sequence().length())
        val newNode = GenNode(node.symbolId, node.pointer + 1)
        graph.addProgressedTo(node, newNode, acceptConditions)
      }

      is NGrammar.NExcept -> {
        processAtomicSymbol(setOf(GenAcceptCondition.Unless(symbol.except())))
      }

      is NGrammar.NJoin -> {
        processAtomicSymbol(setOf(GenAcceptCondition.Unless(symbol.join())))
      }

      is NGrammar.NLongest -> {
        processAtomicSymbol(setOf(GenAcceptCondition.NoLongerMatch(symbol.body())))
      }

      is NGrammar.NLookaheadExcept -> {
        processAtomicSymbol(setOf(GenAcceptCondition.NotExists(symbol.lookahead())))
      }

      is NGrammar.NLookaheadIs -> {
        processAtomicSymbol(setOf(GenAcceptCondition.Exists(symbol.lookahead())))
      }

      is NGrammar.NTerminal -> {
        processAtomicSymbol(setOf())
      }
    }
    return newTasks
  }

  fun finish(
    graph: GenParsingGraph,
    node: GenNode,
  ): Set<GenParsingTask> {
    val newTasks = mutableSetOf<GenParsingTask>()

    fun process(finishPointer: Int, vararg newAcceptConditions: GenAcceptCondition) {
      check(node.pointer == finishPointer)
      graph.edgesByEnd[node]?.let { edges ->
        val acc = newAcceptConditions.toSet()
        for (toProg in edges) {
          newTasks.add(GenParsingTask.Progress(toProg, acc))
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
      is NGrammar.NExcept -> process(1, GenAcceptCondition.Unless(symbol.except()))
      is NGrammar.NJoin -> process(1, GenAcceptCondition.OnlyIf(symbol.join()))
      is NGrammar.NLongest -> process(1, GenAcceptCondition.NoLongerMatch(symbol.body()))
      is NGrammar.NLookaheadExcept -> process(1, GenAcceptCondition.NotExists(symbol.lookahead()))
      is NGrammar.NLookaheadIs -> process(1, GenAcceptCondition.Exists(symbol.lookahead()))
      is NGrammar.NTerminal -> process(1)
    }
    return newTasks
  }
}
