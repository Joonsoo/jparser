package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserDataKt
import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserKt
import com.giyeok.jparser.ktparser.mgroup2.ParsingContextKt
import com.giyeok.jparser.ktparser.mgroup2.PathList
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup2.MilestoneGroupParserDataProtobufConverter
import com.giyeok.jparser.mgroup2.MilestoneGroupParserGen
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Test

/**
 * Phase A 계측 (kernels_history_optimization.md §0.1 후속 — 파스 페이즈 상태 폭발):
 * 같은 NGrammar 로 m2(mgroup2)/m3(mgroup3) 파서를 만들어 같은 입력을 step 구동하고,
 * live path 의 interior milestone 집합을 나란히 덤프한다.
 *
 * 가설: m3 는 nullable(WS) 경계의 인접 dot 쌍 (ptr N / N+1) 을 둘 다 interior
 * milestone 으로 물질화해 (m2 의 milestone 후보 필터
 * — MilestoneParserGen.scala:41-59, deriveTasks ∩ NSequence ∩ pointer>0 ∩
 * beginGen<endGen — 에 대응하는 정규화가 없어) 체인 수가 곱셈으로 늘어난다.
 * 이 테스트는 그 쌍의 존재/부재와, 쌍을 만들어내는 m3 term action 템플릿을 특정한다.
 */
class Mgroup2VsMgroup3PathsTest {
  // 병리 최소 재현: nullable WS 경계 + 그 아래 여러 gen 짜리 서브트리.
  private val microPairGrammar = """
    |S = A WS B
    |A = 'a'+
    |B = '(' WS A WS ')'
    |WS = ' '*
  """.trimMargin()

  // jar.bbx 축소판: 트레일링 블록 + longest 콜체인 + nullable WS + 블록 람다 중의성.
  private val microLambdaGrammar = """
    |S = WS Expr (WS Expr)* WS
    |Expr = Prim | Prim <Trailer+>
    |Trailer = WS '(' WS Args? WS ')' | WS Block
    |Args = Expr (WS ',' WS Expr)*
    |Block = '{' WS Elems? WS '}'
    |Elems = Expr (WS Expr)*
    |Prim = Name | Block
    |Name = <'a-z'+>
    |WS = ' '*
  """.trimMargin()

  // === Phase C 계측 (mgroup3/docs/watcher_anchor_dedup.md §3) ===
  // jar.bbx 의 인접-gen 워처 anchor 중복 재현: mulang 의 longest 층들 —
  // AddExpr = <MulExpr | AddExpr op MulExpr> (본문 전체 longest), LessExpr 의 <AddExpr>,
  // MulExpr 의 <Trailer+> 콜체인 — + 트레일링 블록. jar.bbx 의 `cached("...") { ... }`
  // 에 대응하는 입력으로 같은 워처 심볼의 인접 anchor 들이 블록 끝까지 사는지 관찰.
  private val microAddChainGrammar = """
    |S = WS Expr (WS Expr)* WS
    |Expr = <AddExpr> WS '<' WS AddExpr | AddExpr
    |AddExpr = <MulExpr | AddExpr WS '+' WS MulExpr>
    |MulExpr = Prim | Prim <Trailer+>
    |Trailer = WS '(' WS Args? WS ')' | WS Block
    |Args = Expr (WS ',' WS Expr)*
    |Block = '{' WS Elems? WS '}'
    |Elems = Expr (WS Expr)*
    |Prim = Name | Block
    |Name = <'a-z'+>
    |WS = ' '*
  """.trimMargin()

  @Test
  fun microAddChainCondRoots() =
    compareCondRoots("microAddChain", microAddChainGrammar, "S", "foo(a) { bar(b) qux }")

  @Test
  fun microPairZeroWidthWs() = compare("microPair(zero-width WS)", microPairGrammar, "S", "aa(aaa)")

  @Test
  fun microPairRealWs() = compare("microPair(real WS)", microPairGrammar, "S", "aa (aaa)")

  @Test
  fun microLambda() = compare("microLambda", microLambdaGrammar, "S", "f(a) { g(h) i }")

  private fun compare(name: String, grammarText: String, start: String, input: String) {
    val grammar = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(grammarText, start).ngrammar()

    val m2Data = MilestoneGroupParserGen(grammar).parserData()
    val m2Proto = MilestoneGroupParserDataProtobufConverter.toProto(m2Data)
    val m2Parser = MilestoneGroupParserKt(MilestoneGroupParserDataKt(m2Proto))

    val m3Data = Mgroup3ParserGenerator(grammar).generate()
    val m3Parser = Mgroup3Parser(m3Data)

    println("=== [$name] input='$input' ===")

    // 1. step 구동하며 gen 별 상태 크기 대조.
    var m2Ctx = m2Parser.initialCtx
    var m3Ctx = m3Parser.initCtx()
    var peakGen = 0
    var peakShapes = -1
    println("gen char | m2 paths | m3 shapes(roots)")
    for ((idx, c) in input.withIndex()) {
      m2Ctx = m2Parser.parseStep(m2Ctx, c)
      m3Ctx = m3Parser.parseStep(m3Ctx, c, idx + 1 == input.length)
      val m3Shapes = m3Ctx.paths.values.sumOf { it.size }
      println(
        "%3d  '%s' | %8d | %6d (%d)".format(idx + 1, c, m2Ctx.paths.size, m3Shapes, m3Ctx.paths.size)
      )
      if (m3Shapes > peakShapes) {
        peakShapes = m3Shapes
        peakGen = idx + 1
      }
    }

    // 2. m3 peak gen 에서 양쪽 체인 전체 덤프.
    println("--- state at m3 peak gen $peakGen ---")
    m2Ctx = m2Parser.initialCtx
    m3Ctx = m3Parser.initCtx()
    for ((idx, c) in input.withIndex()) {
      if (idx + 1 > peakGen) break
      m2Ctx = m2Parser.parseStep(m2Ctx, c)
      m3Ctx = m3Parser.parseStep(m3Ctx, c, idx + 1 == input.length)
    }
    dumpM3(grammar, m3Ctx)
    dumpM2(grammar, m2Ctx)

    // 3. interior (sym,ptr) 집합과 인접 ptr 쌍.
    val m3Interior = m3InteriorNodes(m3Ctx)
    val m2Interior = m2InteriorNodes(m2Ctx)
    val m3Pairs = adjacentPairs(m3Interior)
    val m2Pairs = adjacentPairs(m2Interior)
    println("m3 interior=${fmtNodes(grammar, m3Interior)}")
    println("m2 interior=${fmtNodes(grammar, m2Interior)}")
    println("m3-only interior=${fmtNodes(grammar, m3Interior - m2Interior)}")
    println("m3 adjacent ptr pairs=${fmtNodes(grammar, m3Pairs)}")
    println("m2 adjacent ptr pairs=${fmtNodes(grammar, m2Pairs)}")

    // 4. 생성기 수준 대조: 양쪽 템플릿 인벤토리 전체 (마이크로 문법 한정).
    if (m3Data.milestoneGroupsMap.size < 30) {
      println("--- m2 templates ---")
      for (mg in m2Proto.milestoneGroupsList.sortedBy { it.groupId }) {
        println("  m2 group ${mg.groupId}: ${mg.milestonesList.joinToString { "${symName(grammar, it.symbolId)}:${it.pointer}" }}")
      }
      for (ta in m2Proto.termActionsList.sortedBy { it.groupId }) {
        for (tga in ta.actionsList) {
          for (rag in tga.termAction.appendingMilestoneGroupsList) {
            println(
              "  m2 tipGroup ${ta.groupId} --term--> replace=(${symName(grammar, rag.replace.symbolId)}" +
                ":${rag.replace.pointer}) append=group${rag.append.groupId}"
            )
          }
        }
      }
      println("--- m3 templates ---")
      for ((groupId, group) in m3Data.milestoneGroupsMap.toSortedMap()) {
        println("  m3 group $groupId: ${group.kernelsList.joinToString { "${symName(grammar, it.symbolId)}:${it.pointer}" }}")
      }
      for ((tipGroupId, actions) in m3Data.termActionsMap.toSortedMap()) {
        for (tga in actions.actionsList) {
          for (rea in tga.termAction.replaceAndAppendsList) {
            println(
              "  m3 tipGroup $tipGroupId --term--> replace=(${symName(grammar, rea.replace.symbolId)}" +
                ":${rea.replace.pointer}) append=group${rea.append.milestoneGroupId}"
            )
          }
        }
      }
    }

    // 5. 쌍을 만드는 m3 term action 템플릿 (interior 는 term action 의
    //    replace_and_appends[].replace 에서만 물질화된다).
    if (m3Pairs.isNotEmpty()) {
      println("--- m3 term action templates materializing the pairs ---")
      for ((tipGroupId, actions) in m3Data.termActionsMap) {
        for (tga in actions.actionsList) {
          for (rea in tga.termAction.replaceAndAppendsList) {
            val key = rea.replace.symbolId to rea.replace.pointer
            if (key in m3Pairs || (key.first to key.second - 1) in m3Pairs) {
              println(
                "  tipGroup $tipGroupId --term--> replace=(${symName(grammar, rea.replace.symbolId)}" +
                  ":${rea.replace.pointer}) append=group${rea.append.milestoneGroupId}"
              )
            }
          }
        }
      }
      // 쌍 kernel 들이 들어있는 milestone group 들도 표시 (tip 수준 대안 파악용).
      for ((groupId, group) in m3Data.milestoneGroupsMap) {
        val hits = group.kernelsList.filter { k ->
          (k.symbolId to k.pointer) in m3Pairs || (k.symbolId to k.pointer - 1) in m3Pairs
        }
        if (hits.isNotEmpty()) {
          println("  group $groupId contains: ${hits.joinToString { "${symName(grammar, it.symbolId)}:${it.pointer}" }}")
        }
      }
    }
    println()
  }

  // Phase C 계측: m3 cond root 들의 수명 + 생존 이유 (조건 참조 vs step6 체인 anchor 유형)
  // 를 gen 별로 관찰하고, m2 의 워처 first (심볼@gen) 수명과 대조한다.
  private fun compareCondRoots(name: String, grammarText: String, start: String, input: String) {
    val grammar = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(grammarText, start).ngrammar()

    val m2Data = MilestoneGroupParserGen(grammar).parserData()
    val m2Proto = MilestoneGroupParserDataProtobufConverter.toProto(m2Data)
    val m2Parser = MilestoneGroupParserKt(MilestoneGroupParserDataKt(m2Proto))

    val m3Data = Mgroup3ParserGenerator(grammar).generate()
    val m3Parser = Mgroup3Parser(m3Data)

    println("=== [$name] cond root lifecycles, input='$input' ===")

    class RootStat(val firstGen: Int) {
      var lastGen = firstGen
      var everCondRef = false
      var sampleCondRef: String? = null
      // 조건 참조가 없던 gen 들에서 이 root 를 살린 step6 anchor 유형들 (tip/dot/parent).
      val anchorKinds = sortedSetOf<String>()
    }
    val m3Stats = LinkedHashMap<PathRoot, RootStat>()
    val m2Stats = LinkedHashMap<Pair<Int, Int>, IntArray>() // (sym, gen) -> [firstGen, lastGen, peakPaths]

    var m2Ctx = m2Parser.initialCtx
    var m3Ctx = m3Parser.initCtx()
    var prevRoots = setOf<PathRoot>()
    for ((idx, c) in input.withIndex()) {
      val gen = idx + 1
      m2Ctx = m2Parser.parseStep(m2Ctx, c)
      m3Ctx = m3Parser.parseStep(m3Ctx, c, idx + 1 == input.length)

      val roots = m3Ctx.paths.keys.filterTo(LinkedHashSet()) { it != m3Ctx.mainRoot }

      // 이번 gen 분류 — step6 collectFromShape 재현: 조건의 referencedRoots 와
      // milestone 체인의 observing anchor (tip=mp.gen, dot=mp.gen-1, parent=parentGen).
      val condRefs = mutableMapOf<PathRoot, MutableList<String>>()
      val anchorKinds = mutableMapOf<PathRoot, MutableSet<String>>()
      for ((root, pm) in m3Ctx.paths) {
        val rootLabel = if (root == m3Ctx.mainRoot) "main" else fmtRoot(grammar, root)
        for ((shape, cond) in pm) {
          cond.referencedRoots.forEach { r ->
            condRefs.getOrPut(r) { mutableListOf() }.add("$rootLabel: ${cond.toString().take(150)}")
          }
          var mp = shape.milestonePath
          while (mp != null) {
            val parentGen = mp.parent?.gen ?: m3Ctx.mainRoot.startGen
            for (sid in mp.observingCondSymbolIds) {
              anchorKinds.getOrPut(PathRoot(sid, mp.gen)) { mutableSetOf() }.add("tip@${mp.gen}(of ${symName(grammar, mp.milestone.symbolId)}:${mp.milestone.pointer})")
              anchorKinds.getOrPut(PathRoot(sid, mp.gen - 1)) { mutableSetOf() }.add("dot@${mp.gen - 1}(of ${symName(grammar, mp.milestone.symbolId)}:${mp.milestone.pointer})")
              anchorKinds.getOrPut(PathRoot(sid, parentGen)) { mutableSetOf() }.add("parent@$parentGen(of ${symName(grammar, mp.milestone.symbolId)}:${mp.milestone.pointer})")
            }
            mp = mp.parent
          }
        }
      }

      for (r in roots) {
        val stat = m3Stats.getOrPut(r) { RootStat(gen) }
        stat.lastGen = gen
        val refs = condRefs[r]
        if (refs != null) {
          stat.everCondRef = true
          if (stat.sampleCondRef == null) stat.sampleCondRef = refs.first()
        } else {
          stat.anchorKinds.addAll((anchorKinds[r] ?: setOf("NONE")).map { it.substringBefore("(") })
        }
      }
      for (f in m2Ctx.paths.map { it.first }.distinct()) {
        val key = f.symbolId to f.gen
        val arr = m2Stats.getOrPut(key) { intArrayOf(gen, gen, 0) }
        arr[1] = gen
        val cnt = m2Ctx.paths.count { it.first == f }
        if (cnt > arr[2]) arr[2] = cnt
      }

      val added = roots - prevRoots
      val removed = prevRoots - roots
      val m3Shapes = m3Ctx.paths.values.sumOf { it.size }
      println(
        "%3d '%s': m3 %3d shapes / %2d roots | m2 %3d paths / %2d firsts%s%s".format(
          gen, if (c == '\n') "\\n" else c.toString(), m3Shapes, roots.size,
          m2Ctx.paths.size, m2Ctx.paths.map { it.first }.distinct().size,
          if (added.isNotEmpty()) " | +" + added.joinToString(",") { fmtRoot(grammar, it) } else "",
          if (removed.isNotEmpty()) " | -" + removed.joinToString(",") { fmtRoot(grammar, it) } else "",
        )
      )
      prevRoots = roots
    }

    println("--- m3 cond root lifecycle summary (${m3Stats.size} roots) ---")
    for ((r, stat) in m3Stats) {
      val life = if (stat.firstGen == stat.lastGen) "gen ${stat.firstGen}" else "gens ${stat.firstGen}..${stat.lastGen}"
      println(
        "  ${fmtRoot(grammar, r)}: $life " +
          (if (stat.everCondRef) "COND-REF [${stat.sampleCondRef}]" else "never-cond-ref") +
          (if (stat.anchorKinds.isNotEmpty()) " anchors=${stat.anchorKinds}" else "")
      )
    }
    println("--- m2 watcher first lifecycle summary (${m2Stats.size} firsts) ---")
    for ((key, arr) in m2Stats) {
      val (sym, g) = key
      val life = if (arr[0] == arr[1]) "gen ${arr[0]}" else "gens ${arr[0]}..${arr[1]}"
      println("  ${symName(grammar, sym)}@$g: $life peakPaths=${arr[2]}")
    }
    println()
  }

  private fun fmtRoot(grammar: NGrammar, r: PathRoot): String =
    "${symName(grammar, r.symbolId)}@${r.startGen}"

  // Phase C 계측 2: 템플릿의 조건 startGen 태그 분포 — step6 체인 anchor 의 필요조건 확정용.
  // bounded (NLM/Unless/OnlyIf) 조건의 anchor 가 실제로 어떤 gen 태그로 등장하는지:
  //   edge 템플릿에서 CURR(=graph Prev, parent anchor) 가 없고 GRAND(dot) 뿐이면
  //   bounded 의 parent anchor 는 불필요. term 템플릿에서 GRAND 가 없으면
  //   parent anchor 는 term 쪽에서도 불필요. tip anchor 는 term CURR 용.
  @Test
  fun scanCondAnchorTags() {
    val micro = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(microAddChainGrammar, "S").ngrammar()
    scanCondAnchorTagsOf("microAddChain", Mgroup3ParserGenerator(micro).generate())
    val mulangPb = java.io.File("mgroup3-native/tests/fixtures/parser_generated/mulang/data.pb")
    if (mulangPb.exists()) {
      val data = com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData.parseFrom(mulangPb.readBytes())
      scanCondAnchorTagsOf("mulang(fixture)", data)
    } else {
      println("scanCondAnchorTags: mulang fixture not found at ${mulangPb.absolutePath} — skipped")
    }
  }

  private fun scanCondAnchorTagsOf(name: String, data: com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData) {
    // (frame, condKind, startGenTag) -> count
    val counts = sortedMapOf<String, Int>()
    fun walk(frame: String, cond: com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate) {
      when (cond.conditionCase) {
        com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate.ConditionCase.ALWAYS -> {}
        com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate.ConditionCase.AND ->
          cond.and.conditionsList.forEach { walk(frame, it) }
        com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate.ConditionCase.OR ->
          cond.or.conditionsList.forEach { walk(frame, it) }
        com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate.ConditionCase.NO_LONGER_MATCH ->
          counts.merge("$frame NLM start=${cond.noLongerMatch.startGen}", 1, Int::plus)
        com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate.ConditionCase.EXCEPT ->
          counts.merge("$frame Unless start=${cond.except.startGen}", 1, Int::plus)
        com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate.ConditionCase.JOIN ->
          counts.merge("$frame OnlyIf start=${cond.join.startGen}", 1, Int::plus)
        com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate.ConditionCase.LOOKAHEAD_FOUND ->
          counts.merge("$frame Exists start=${cond.lookaheadFound.startGen}", 1, Int::plus)
        com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate.ConditionCase.LOOKAHEAD_NOTFOUND ->
          counts.merge("$frame NotExists start=${cond.lookaheadNotfound.startGen}", 1, Int::plus)
        else -> {}
      }
    }
    fun walkActions(frame: String, pa: com.giyeok.jparser.mgroup3.proto.ParsingActions) {
      pa.finishedList.forEach { walk(frame, it.finishCondition) }
      pa.addedList.forEach { walk(frame, it.acceptCondition) }
    }
    for ((_, actions) in data.termActionsMap) {
      for (tga in actions.actionsList) {
        tga.termAction.replaceAndAppendsList.forEach { walk("term/rea", it.append.acceptCondition) }
        tga.termAction.replaceAndProgressesList.forEach { walk("term/rap", it.acceptCondition) }
        if (tga.termAction.hasParsingActions()) walkActions("term/pa", tga.termAction.parsingActions)
      }
    }
    fun walkEdge(frame: String, ea: com.giyeok.jparser.mgroup3.proto.EdgeAction) {
      ea.appendMilestoneGroupsList.forEach { walk("$frame/append", it.acceptCondition) }
      if (ea.hasStartNodeProgress()) walk("$frame/prog", ea.startNodeProgress)
      if (ea.hasParsingActions()) walkActions("$frame/pa", ea.parsingActions)
    }
    data.tipEdgeActionsList.forEach { walkEdge("tipEdge", it.edgeAction) }
    data.midEdgeActionsList.forEach { walkEdge("midEdge", it.edgeAction) }
    for ((_, rootInfo) in data.pathRootsMap) {
      if (rootInfo.hasSelfFinishAcceptCondition()) walk("root/selfFin", rootInfo.selfFinishAcceptCondition)
      if (rootInfo.hasParsingActions()) walkActions("root/pa", rootInfo.parsingActions)
    }
    for ((_, mg) in data.milestoneGroupsMap) {
      mg.possibleFinishesList.forEach { walk("mgroup/pf", it.acceptCondition) }
    }
    println("=== [$name] cond anchor tag distribution ===")
    counts.forEach { (k, v) -> println("  $k: $v") }
    println()
  }

  private fun dumpM3(grammar: NGrammar, ctx: ParsingCtx) {
    println("m3 paths (${ctx.paths.values.sumOf { it.size }} shapes / ${ctx.paths.size} roots):")
    for ((root, pathMap) in ctx.paths) {
      for ((shape, cond) in pathMap) {
        val nodes = generateSequence(shape.milestonePath) { it.parent }
          .map { "${symName(grammar, it.milestone.symbolId)}:${it.milestone.pointer}@${it.milestone.gen}" }
          .toList().reversed()
        println(
          "  [root ${symName(grammar, root.symbolId)}@${root.startGen}] " +
            nodes.joinToString(" -> ").ifEmpty { "(root)" } +
            " => tip[${shape.tipGroupId}] (${cond.toString().take(80)})"
        )
      }
    }
  }

  private fun dumpM2(grammar: NGrammar, ctx: ParsingContextKt) {
    println("m2 paths (${ctx.paths.size}):")
    for (path in ctx.paths) {
      val nodes = mutableListOf<String>()
      var pl = path.path
      while (pl is PathList.Cons) {
        nodes.add("${symName(grammar, pl.milestone.symbolId)}:${pl.milestone.pointer}@${pl.milestone.gen}")
        pl = pl.parent
      }
      println(
        "  [first ${symName(grammar, path.first.symbolId)}:${path.first.pointer}@${path.first.gen}] " +
          nodes.reversed().joinToString(" -> ").ifEmpty { "(root)" } +
          " => tip[${path.tip.groupId}]@${path.tip.gen} (${path.acceptCondition.prettyString().take(80)})"
      )
    }
  }

  private fun m3InteriorNodes(ctx: ParsingCtx): Set<Pair<Int, Int>> {
    val result = mutableSetOf<Pair<Int, Int>>()
    for (pathMap in ctx.paths.values) {
      for (shape in pathMap.keys) {
        var mp = shape.milestonePath
        while (mp != null) {
          result.add(mp.milestone.symbolId to mp.milestone.pointer)
          mp = mp.parent
        }
      }
    }
    return result
  }

  private fun m2InteriorNodes(ctx: ParsingContextKt): Set<Pair<Int, Int>> {
    val result = mutableSetOf<Pair<Int, Int>>()
    for (path in ctx.paths) {
      var pl = path.path
      while (pl is PathList.Cons) {
        result.add(pl.milestone.symbolId to pl.milestone.pointer)
        pl = pl.parent
      }
    }
    return result
  }

  private fun adjacentPairs(nodes: Set<Pair<Int, Int>>): Set<Pair<Int, Int>> =
    nodes.filter { (s, p) -> (s to p + 1) in nodes }.toSet()

  private fun fmtNodes(grammar: NGrammar, nodes: Set<Pair<Int, Int>>): String =
    nodes.sortedWith(compareBy({ it.first }, { it.second }))
      .joinToString(", ", "{", "}") { (s, p) -> "${symName(grammar, s)}:$p" }

  private fun symName(grammar: NGrammar, symbolId: Int): String =
    try {
      val sym = grammar.symbolOf(symbolId)
      val short = sym.symbol().toShortString()
      if (short.length > 24) "sym$symbolId" else "sym$symbolId($short)"
    } catch (e: Throwable) {
      "sym$symbolId"
    }
}
