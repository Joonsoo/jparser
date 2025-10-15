package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.TermGroupUtil
import com.giyeok.jparser.ktlib.TermSet
import com.giyeok.jparser.mgroup3.proto.*
import java.util.*

class Mgroup3Parser(val data: Mgroup3ParserData) {
  val midEdgeActions = data.midEdgeActionsList.associate {
    (Pair(it.parent.symbolId, it.parent.pointer) to Pair(
      it.tip.symbolId,
      it.tip.pointer
    )) to it.edgeAction
  }

  fun condPathsFor(condSymbolIds: Collection<Int>, gen: Int): Map<PathRoot, List<ParsingPath>> {
    val builder = mutableMapOf<Int, List<ParsingPath>>()
    val queue = LinkedList<Int>()
    queue.addAll(condSymbolIds)
    while (queue.isNotEmpty()) {
      val symId = queue.pop()
      if (symId !in builder) {
        val d = data.pathRootsMap[symId]!!
        builder[symId] = listOf(ParsingPath(null, d.milestoneGroupId, Always))
        queue.addAll(d.initialCondSymbolIdsList)
      }
    }
    return builder.mapKeys { (symId, _) -> PathRoot(symId, gen) }
  }

  fun initCtx(startSymbolId: Int): ParsingCtx {
    val rootPath = data.pathRootsMap[startSymbolId]!!
    return ParsingCtx(
      0, 0, 0,
      PathRoot(startSymbolId, 0),
      listOf(
        ParsingPath(
          null,
          rootPath.milestoneGroupId,
          Always
        )
      ),
      condPathsFor(rootPath.initialCondSymbolIdsList, 0),
    )
  }

  fun initCtx(): ParsingCtx = initCtx(data.startSymbolId)

  fun expectedInputsOf(ctx: ParsingCtx): TermSet {
    val termGroups =
      ctx.mainPaths.flatMap { path -> data.termActionsMap[path.tipGroupId]!!.actionsList.map { it.termGroup } }
    val termSetBuilder = TermGroupUtil.TermGroupBuilder()
    for (termGroup in termGroups) {
      termSetBuilder.add(termGroup)
    }
    return termSetBuilder.build()
  }

  fun findApplicableAction(path: ParsingPath, input: Char): TermAction? =
    data.termActionsMap[path.tipGroupId]!!.actionsList.find { action ->
      TermGroupUtil.isMatch(action.termGroup, input)
    }?.termAction

  fun parseStep(ctx: ParsingCtx, input: Char, isLastInput: Boolean): ParsingCtx {
    if (ctx.mainPaths.isEmpty()) {
      throw ParsingError.UnexpectedInput(ctx.gen, ctx.line, ctx.col, expectedInputsOf(ctx), input)
    }
    val gen = ctx.gen + 1
    val nextLine: Int
    val nextCol: Int
    if (input == '\n') {
      nextLine = ctx.line + 1
      nextCol = 0
    } else {
      // TODO \r 문자 처리?
      nextLine = ctx.line
      nextCol = ctx.col + 1
    }

    val nextMainPathsOut = mutableListOf<ParsingPath>()
    val parsingActionsOut = mutableListOf<ParsingActions>()
    val observingSymbolIdsOut = mutableSetOf<Int>()
    for (path in ctx.mainPaths) {
      val termAction = findApplicableAction(path, input)
      if (termAction != null) {
        applyTermAction(
          path,
          gen,
          termAction,
          nextMainPathsOut,
          parsingActionsOut,
          observingSymbolIdsOut
        )
      }
    }

    val contCondPaths = mutableMapOf<PathRoot, MutableList<ParsingPath>>()
    val condPathFinishes = mutableMapOf<PathRoot, AcceptCondition>()
    for ((root, paths) in ctx.condPaths) {
      val nextPathsOut = mutableListOf<ParsingPath>()
      val parsingActionsOut = mutableListOf<ParsingActions>()
      for (path in paths) {
        val termAction = findApplicableAction(path, input)
        if (termAction != null) {
          applyTermAction(
            path,
            gen,
            termAction,
            nextPathsOut,
            parsingActionsOut,
            observingSymbolIdsOut
          )
        }
      }
      if (nextPathsOut.isNotEmpty()) {
        contCondPaths[root] = nextPathsOut
      }
      // TODO parsingActionsOut 중에 root에 대한 finish가 있는 경우, finish condition들을 or 로 묶어서 condPathFinishes에 넣어준다
    }

    val acceptConditions = (nextMainPathsOut.map { it.acceptCondition } +
      contCondPaths.values.flatMap { paths -> paths.map { it.acceptCondition } }).toSet()
    val newAcceptConditions = acceptConditions.associateWith {
      evolveAcceptCondition(it, condPathFinishes, contCondPaths.keys, gen)
    }
    // TODO 새로운 path들에 대해 newAcceptConditions 적용해서 업데이트 - 하고 Never인 애들 날리기

    if (!isLastInput && nextMainPathsOut.isEmpty()) {
      throw ParsingError.UnexpectedInput(ctx.gen, ctx.line, ctx.col, expectedInputsOf(ctx), input)
    }

    val newCondPaths = condPathsFor(observingSymbolIdsOut, gen)
    return ParsingCtx(
      gen,
      nextLine,
      nextCol,
      ctx.mainRoot,
      nextMainPathsOut,
      newCondPaths + contCondPaths
    )
  }

  fun applyTermAction(
    oldPath: ParsingPath,
    gen: Int,
    termAction: TermAction,
    out: MutableList<ParsingPath>,
    // TODO 그냥 ParsingActions만 있으면 안되고 gen에 대한 정보도 있어야 함
    parsingActionsOut: MutableList<ParsingActions>,
    observingSymbolIdsOut: MutableSet<Int>,
  ) {
    parsingActionsOut.add(termAction.parsingActions)
    for (action in termAction.replaceAndAppendsList) {
      val oldGen = oldPath.milestonePath?.gen ?: 0
      out.add(
        ParsingPath(
          MilestonePath(
            gen,
            action.replace.toKernel(oldGen),
            oldPath.milestonePath,
            action.append.observingCondSymbolIdsList.toSet(),
          ),
          action.append.milestoneGroupId,
          And.from(
            oldPath.acceptCondition,
            action.append.acceptCondition.toAcceptCondition(oldGen, gen)
          )
        )
      )
      observingSymbolIdsOut.addAll(action.append.observingCondSymbolIdsList)
    }
    for (progress in termAction.replaceAndProgressesList) {
      val parentOfTip = oldPath.milestonePath
      if (parentOfTip != null) {
        val tipEdgeAction = data.tipEdgeActionsList.find {
          it.tipGroupId == progress.replaceMilestoneGroupId &&
            it.parent.symbolId == parentOfTip.milestone.symbolId &&
            it.parent.pointer == parentOfTip.milestone.pointer
        }!!.edgeAction
        applyEdgeAction(
          parentOfTip,
          gen,
          tipEdgeAction,
          parsingActionsOut,
          oldPath.acceptCondition,
          out,
          observingSymbolIdsOut
        )
      }
    }
  }

  fun applyEdgeAction(
    mpath: MilestonePath,
    gen: Int,
    edgeAction: EdgeAction,
    // TODO 그냥 ParsingActions만 있으면 안되고 gen에 대한 정보도 있어야 함
    parsingActionsOut: MutableList<ParsingActions>,
    prevCondition: AcceptCondition,
    out: MutableList<ParsingPath>,
    observingSymbolIdsOut: MutableSet<Int>,
  ) {
    parsingActionsOut.add(edgeAction.parsingActions)
    // oldPath의 tip mgroup id는 edgeAction의 mgroup id와 다를 수 있음
    for (append in edgeAction.appendMilestoneGroupsList) {
      out.add(
        ParsingPath(
          mpath,
          append.milestoneGroupId,
          And.from(
            prevCondition,
            append.acceptCondition.toAcceptCondition(mpath.gen, gen)
          )
        )
      )
      observingSymbolIdsOut.addAll(append.observingCondSymbolIdsList)
    }
    if (edgeAction.hasStartNodeProgress()) {
      val parent = mpath.parent
      check(parent != null)
      val midEdgeAction = findMidEdge(
        parent.milestone.symbolId,
        parent.milestone.pointer,
        mpath.milestone.symbolId,
        mpath.milestone.pointer
      )
      applyEdgeAction(
        mpath.parent,
        gen,
        midEdgeAction,
        parsingActionsOut,
        prevCondition,
        out,
        observingSymbolIdsOut
      )
    }
  }

  fun findMidEdge(parentSymbolId: Int, parentPointer: Int, tipSymbolId: Int, tipPointer: Int) =
    midEdgeActions[Pair(parentSymbolId, parentPointer) to Pair(tipSymbolId, tipPointer)]!!

  fun KernelTemplate.toKernel(gen: Int) = Kernel(symbolId, pointer, gen)
  fun AcceptConditionTemplate.toAcceptCondition(prevGen: Int, gen: Int): AcceptCondition =
    when (conditionCase) {
      AcceptConditionTemplate.ConditionCase.ALWAYS -> Always
      AcceptConditionTemplate.ConditionCase.AND ->
        And.from(this.and.conditionsList.map { it.toAcceptCondition(prevGen, gen) }.toSet())

      AcceptConditionTemplate.ConditionCase.OR ->
        Or.from(this.or.conditionsList.map { it.toAcceptCondition(prevGen, gen) }.toSet())

      AcceptConditionTemplate.ConditionCase.NO_LONGER_MATCH ->
        NoLongerMatch(noLongerMatch, prevGen, gen)

      AcceptConditionTemplate.ConditionCase.LOOKAHEAD_FOUND ->
        Exists(lookaheadFound, gen)

      AcceptConditionTemplate.ConditionCase.LOOKAHEAD_NOTFOUND ->
        NotExists(lookaheadNotfound, gen)

      AcceptConditionTemplate.ConditionCase.EXCEPT ->
        Unless(except, prevGen)

      AcceptConditionTemplate.ConditionCase.JOIN ->
        OnlyIf(except, prevGen)

      AcceptConditionTemplate.ConditionCase.CONDITION_NOT_SET ->
        throw IllegalArgumentException()
    }

  fun parse(text: String): ParsingCtx {
    var ctx = initCtx()
    for ((idx, t) in text.withIndex()) {
      ctx = parseStep(ctx, t, text.length == idx + 1)
    }
    return ctx
  }
}

sealed class ParsingError: Exception() {
  data class UnexpectedInput(
    val loc: Int,
    val locLine: Int,
    val locCol: Int,
    val expected: TermSet,
    val actual: Char
  ): ParsingError()

  data class UnexpectedEof(
    val loc: Int,
    val locLine: Int,
    val locCol: Int,
    val expected: TermSet,
  ): ParsingError()
}
