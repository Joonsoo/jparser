package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.TermGroupUtil
import com.giyeok.jparser.ktlib.TermSet
import com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate
import com.giyeok.jparser.mgroup3.proto.EdgeAction
import com.giyeok.jparser.mgroup3.proto.KernelTemplate
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import com.giyeok.jparser.mgroup3.proto.TermAction

class Mgroup3Parser(val data: Mgroup3ParserData) {
  val midEdgeActions = data.midEdgeActionsList.associate {
    (Pair(it.parent.symbolId, it.parent.pointer) to Pair(
      it.tip.symbolId,
      it.tip.pointer
    )) to it.edgeAction
  }

  fun initCtx(): ParsingCtx {
    val rootPath = data.pathRootsMap[data.startSymbolId]!!
    return ParsingCtx(
      0, 0, 0,
      PathRoot(data.startSymbolId, 0),
      listOf(
        ParsingPath(
          null,
          rootPath.milestoneGroupId,
          Always
        )
      ),
      rootPath.initialCondSymbolIdsList.associate { condSymbolId ->
        PathRoot(condSymbolId, 0) to listOf(
          ParsingPath(
            null,
            data.pathRootsMap[condSymbolId]!!.milestoneGroupId,
            Always
          )
        )
      }
    )
  }

  fun expectedInputsOf(ctx: ParsingCtx): Set<TermSet> {
    TODO()
  }

  fun parseStep(ctx: ParsingCtx, input: Char): ParsingCtx {
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

    val nextMainPaths = mutableListOf<ParsingPath>()
    val observingSymbolIdsOut = mutableSetOf<Int>()
    for (path in ctx.mainPaths) {
      val termAction = data.termActionsMap[path.tipGroupId]!!.actionsList.find { action ->
        TermGroupUtil.isMatch(action.termGroup, input)
      }?.termAction
      if (termAction != null) {
        applyTermAction(path, gen, termAction, nextMainPaths, observingSymbolIdsOut)
      }
    }
    TODO()
  }

  fun applyTermAction(
    oldPath: ParsingPath,
    gen: Int,
    termAction: TermAction,
    out: MutableList<ParsingPath>,
    observingSymbolIdsOut: MutableSet<Int>,
  ) {
    for (action in termAction.replaceAndAppendsList) {
      out.add(
        ParsingPath(
          MilestonePath(
            gen,
            action.replace.toKernel(gen),
            oldPath.milestonePath,
            action.append.observingCondSymbolIdsList.toSet(),
          ),
          action.append.milestoneGroupId,
          And.from(
            oldPath.acceptCondition,
            action.append.acceptCondition.toAcceptCondition(oldPath.milestonePath?.gen ?: 0, gen)
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
    prevCondition: AcceptCondition,
    out: MutableList<ParsingPath>,
    observingSymbolIdsOut: MutableSet<Int>,
  ) {
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
    for (t in text) {
      ctx = parseStep(ctx, t)
    }
    return ctx
  }
}

sealed class ParsingError: Exception() {
  data class UnexpectedInput(
    val loc: Int,
    val locLine: Int,
    val locCol: Int,
    val expected: Set<TermSet>,
    val actual: Char
  ): ParsingError()

  data class UnexpectedEof(
    val loc: Int,
    val locLine: Int,
    val locCol: Int,
    val expected: Set<TermSet>
  ): ParsingError()
}
