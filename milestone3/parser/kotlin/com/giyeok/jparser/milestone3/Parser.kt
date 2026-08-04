package com.giyeok.jparser.milestone3

import com.giyeok.jparser.Inputs

class Parser(val parseData: ParseData) {
  fun initialCtx(): ParsingContext {
    TODO()
  }

  fun parseStep(ctx: ParsingContext, input: Inputs.Input): ParsingContext {
    // 0. check
    if (ctx.paths.all { it.rootSymbolId != parseData.startSymbol }) {
      // main path가 없으면 - 파싱 실패
      throw Exception() // TODO 제대로된 exception
    }

    // 1. parsing tasks
    val builder = ParsingContextBuilder(ctx.gen + 1, mutableListOf())
    for (path in ctx.paths) {
      val tip = path.tipKernelTemplate()
      // tip에 따라 term action 찾아서 parsing action initiate
      // term action의 watchers를 nextGenPaths에 추가
      val actions = parseData.termActions[tip]!!
      val action = actions.find { it.first.contains(input) }
      if (action != null) {
        applyParsingAction(ctx.gen, path, action.second.parsingAction, builder)
        // TODO add paths for `action.second.watchers`
      }
    }

    // 2. evolve AC + trim
    // evolve - nextGenPaths의 모든 accept condition에 대해 evolveAcceptCondition
    // trim - 어떤 지점에서라도 Never가 포함된 모든 path는 제거
    return evolveAndTrim(builder)
  }

  fun applyParsingAction(
    gen: Int,
    path: MilestonePath,
    parseAction: ParseAction,
    builder: ParsingContextBuilder
  ) {
    for (append in parseAction.appends) {
      // 1. path에 append 를 붙인 path들을 builder에 추가
      val appendPath = appendMilestoneToPath(path, append, gen, builder.nextGen)
      builder.nextGenPaths.add(appendPath)
    }
    if (parseAction.tipProgress != null) {
      val edgeAction = parseData.edgeActions[path.tipEdge()]!!
      // TODO apply parse action
    }
    for (watcherRootSymbol in parseAction.watcherRootSymbols) {
      val watcherPath = MilestonePath(watcherRootSymbol, builder.nextGen, listOf())
      builder.nextGenPaths.add(watcherPath)
    }
  }

  fun appendMilestoneToPath(
    path: MilestonePath,
    append: AppendMilestone,
    gen: Int,
    nextGen: Int,
  ): MilestonePath {
    val newEdge = MilestoneEdge(
      reifyAcceptCondition(append.edgeAcceptCondition, gen, nextGen),
      Milestone(
        append.appendKernelTemplate.symbolId,
        append.appendKernelTemplate.pointer,
        nextGen,
        reifyAcceptCondition(append.appendAcceptCondition, gen, nextGen)
      )
    )
    return path.appendEdge(newEdge)
  }

  fun reifyAcceptCondition(
    template: M3AcceptConditionTemplate,
    gen: Int,
    nextGen: Int,
  ): M3AcceptCondition = when (template) {
    AlwaysTemplate -> Always
  }

  fun evolveAndTrim(builder: ParsingContextBuilder): ParsingContext {
    TODO()
  }
}

class ParsingContextBuilder(
  val nextGen: Int,
  val nextGenPaths: MutableList<MilestonePath>,
)
