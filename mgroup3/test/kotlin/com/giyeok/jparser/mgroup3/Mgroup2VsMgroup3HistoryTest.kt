package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.Kernel
import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserDataKt
import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserKt
import com.giyeok.jparser.ktparser.mgroup2.ParsingContextKt
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup2.MilestoneGroupParserDataProtobufConverter
import com.giyeok.jparser.mgroup2.MilestoneGroupParserGen
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto
import org.junit.jupiter.api.Test
import scala.jdk.javaapi.CollectionConverters

/**
 * TEMPORARY diagnostic for the mgroup2-vs-mgroup3 kernels_history coordinate
 * mismatch.
 *
 * This run tags every kernel that mgroup2(ktparser)'s kernelsHistory emits with
 * its emission source (initial / term tasksSummary / tipEdge / midEdge /
 * progressedKernels / progressedRootMilestones / progressedKgroups /
 * progressedRootMgroups), replaying the same walk kernelsHistory performs but
 * WITHOUT condition filtering (over-approximation — sufficient for source
 * attribution since filtering only removes kernels).
 *
 * Goal: identify, for the kernels that differ from mgroup3's history (the +1
 * begins like Defs (2,1,0,35), and the (sym,0,g,g) empty kernels missing from
 * m3), exactly which m2 source produces them and with which genMap — so the
 * m3 fix can reproduce the same semantics at its corresponding recording site.
 */
class Mgroup2VsMgroup3HistoryTest {
  private val asdlGrammar = """
    |Defs = WS ModuleDef WS ${'$'}1
    |ModuleDef = "module"&Tk WS Name WS '{' WS SuperClassDef (WS SuperClassDef)* WS '}' {ModuleDef(name=${'$'}2, defs=[${'$'}6] + ${'$'}7)}
    |SuperClassDef = Name WS '=' WS SuperClassDefBody (WS AttributesDef)?
    |                {SuperClassDef(name=${'$'}0, body=${'$'}4, attrs=${'$'}5)}
    |SuperClassDefBody: SuperClassDefBody = SubClassDef (WS '|' WS SubClassDef)* {SealedClassDefs(subs=[${'$'}0] + ${'$'}1)}
    |  | Params {TupleDef(body=${'$'}0)}
    |AttributesDef = "attributes"&Tk WS Params {Attributes(attrs=${'$'}2)}
    |SubClassDef = Name (WS Params)? {SubClassDef(name=${'$'}0, params=${'$'}1)}
    |Params = '(' WS Param (WS ',' WS Param)* WS ')' {[${'$'}2] + ${'$'}3}
    |Param = Name (WS ('*' {%REPEATED} | '?' {%OPTIONAL}))? WS Name
    |        {Param(typeName=${'$'}0, typeAttr: %TypeAttr = ${'$'}1 ?: %PLAIN, name=${'$'}3)}
    |Name = <'a-zA-Z_'+ {str(${'$'}0)}>
    |Tk = <'a-zA-Z_'+>
    |WS = (' \t' | NEWLINE | LineComment)*
    |LineComment = "--" (.-'\n')* (EOF | '\n')
    |NEWLINE = '\n'
    |EOF = !.
  """.trimMargin()

  private val input = "module M { foo = Bar(int x) | Baz }"

  @Test
  fun dumpSideBySide() {
    val analysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(asdlGrammar, "Defs")
    val grammar = analysis.ngrammar()

    // mgroup2 (ktparser)
    val m2Data = MilestoneGroupParserGen(grammar).parserData()
    val m2Proto = MilestoneGroupParserDataProtobufConverter.toProto(m2Data)
    val m2DataKt = MilestoneGroupParserDataKt(m2Proto)
    val m2Parser = MilestoneGroupParserKt(m2DataKt)
    val m2Ctx = m2Parser.parse(input)
    val m2Hist = m2Parser.kernelsHistory(m2Ctx)

    // mgroup3
    val m3Data = Mgroup3ParserGenerator(grammar).generate()
    val m3Parser = Mgroup3Parser(m3Data)
    val m3Ctx = m3Parser.parse(input)
    val m3Hist = m3Parser.kernelsHistory(m3Ctx)

    println("=== input = '$input' (len ${input.length}) ===")
    println("m2 history size=${m2Hist.size}, m3 history size=${m3Hist.size}")

    val attribution = attributeM2Sources(m2Ctx, m2DataKt)

    // 1. sanity: every kernel kernelsHistory emitted must be reproduced by the
    //    attribution walk (it's the same walk minus condition filtering).
    var unattributed = 0
    m2Hist.forEachIndexed { gen, ks ->
      ks.kernels.forEach { k ->
        if (attribution[gen]?.get(k) == null) {
          unattributed++
          if (unattributed <= 10) println("UNATTRIBUTED gen=$gen ${fmt(k)}")
        }
      }
    }
    println("=== sanity: unattributed m2 kernels = $unattributed (must be 0) ===")

    // 2. last gen: sym 1,2,3 kernels with their m2 sources (the start-region +1 case)
    val lastGen = m2Hist.size - 1
    println("=== last gen ($lastGen): sym 1,2,3 kernels and their m2 sources ===")
    m2Hist[lastGen].kernels.filter { it.symbolId in setOf(1, 2, 3) }
      .sortedWith(kernelOrder).forEach { k ->
        println("  ${fmt(k)} <- ${attribution[lastGen]?.get(k)?.sorted()?.joinToString(" | ")}")
      }
    println("=== last gen ($lastGen): m3 sym 1,2,3 kernels ===")
    m3Hist[lastGen].kernels.filter { it.symbolId in setOf(1, 2, 3) }
      .sortedWith(kernelOrder).forEach { k -> println("  ${fmt(k)}") }

    // 2b. overall diff sizes
    var totalOnlyM2 = 0
    var totalOnlyM3 = 0
    for (gen in m2Hist.indices) {
      val m2k = m2Hist[gen].kernels
      val m3k = m3Hist.getOrNull(gen)?.kernels ?: emptySet()
      totalOnlyM2 += (m2k - m3k).size
      totalOnlyM3 += (m3k - m2k).size
    }
    println("=== TOTAL diff: only-m2=$totalOnlyM2, only-m3=$totalOnlyM3 ===")
    // m3-only kernels by gen (these must reach 0 — wrong coordinates m3 emits)
    for (gen in m2Hist.indices) {
      val m2k = m2Hist[gen].kernels
      val m3k = m3Hist.getOrNull(gen)?.kernels ?: emptySet()
      val onlyM3 = (m3k - m2k).sortedWith(kernelOrder)
      if (onlyM3.isNotEmpty()) {
        println("  only-m3 gen $gen: ${onlyM3.joinToString { fmt(it) }}")
      }
    }

    // 3. m2-only kernels (vs m3) aggregated by generic source
    val srcCounts = mutableMapOf<String, Int>()
    val ptr0SrcCounts = mutableMapOf<String, Int>()
    for (gen in m2Hist.indices) {
      val m2k = m2Hist[gen].kernels
      val m3k = m3Hist.getOrNull(gen)?.kernels ?: emptySet()
      (m2k - m3k).forEach { k ->
        val srcs = attribution[gen]?.get(k)?.sorted()?.joinToString("+") ?: "???"
        val generic = srcs.replace(Regex("\\[[^\\]]*\\]"), "")
        srcCounts.merge(generic, 1, Int::plus)
        if (k.pointer == 0 && k.beginGen == k.endGen) ptr0SrcCounts.merge(generic, 1, Int::plus)
      }
    }
    println("=== m2-only kernels by source combo (gen-stripped) ===")
    srcCounts.entries.sortedByDescending { it.value }.forEach { println("  ${it.value}\t${it.key}") }
    println("=== of which (sym,0,g,g) empty ptr0 kernels ===")
    ptr0SrcCounts.entries.sortedByDescending { it.value }.forEach { println("  ${it.value}\t${it.key}") }

    // 4. m2-only kernels: full individual list with gen and sources
    println("=== m2-only kernels (full list) ===")
    for (gen in m2Hist.indices) {
      val m2k = m2Hist[gen].kernels
      val m3k = m3Hist.getOrNull(gen)?.kernels ?: emptySet()
      (m2k - m3k).sortedWith(kernelOrder).forEach { k ->
        println("  gen $gen ${fmt(k)} <- ${attribution[gen]?.get(k)?.sorted()?.joinToString(" | ")}")
      }
    }

    // 4b. path structures at gens 7-8 (the (26,k) begin mismatch region)
    run {
      val m2Ctx7 = m2Parser.parse(input.substring(0, 7))
      println("=== m2 paths after 7 chars ===")
      m2Ctx7.paths.forEach { p -> println("  ${p.prettyString()}") }
      for (n in listOf(6, 7)) {
        val m3CtxN = m3Parser.parse(input.substring(0, n))
        println("=== m3 paths after $n chars ===")
        for ((root, pm) in m3CtxN.paths) {
          for ((shape, _) in pm) {
            val chain = generateSequence(shape.milestonePath) { it.parent }
              .map {
                "(${it.milestone.symbolId},${it.milestone.pointer}@${it.milestone.gen})" +
                  "edge@${it.gen}/rep=${it.reportGen}/mrep=${it.milestoneReportGen}"
              }
              .toList().reversed()
            println("  root=$root chain=${chain.joinToString("->")} tip=g${shape.tipGroupId}")
          }
        }
      }
    }

    // 4c. gen 18: apps with added templates for syms 77/26 (leaking ambiguous-variant kernels)
    run {
      val g = 18
      val entry = m3Ctx.history[g]
      println("=== gen $g: apps with sym 77/26 added templates ===")
      for (app in entry.actionApplications) {
        val rel = app.actions.added.filter { it.symbolId == 77 || it.symbolId == 26 }
        if (rel.isEmpty()) continue
        println("  app root=${app.root} rt=(${app.rtCurr},${app.rtMid},${app.next},${app.rtGrand}) rep=(${app.repCurr},${app.repMid},${app.repGrand})")
        for (a in rel) {
          println("    tpl (${a.symbolId},${a.pointer},${a.startGen}..${a.endGen}) cond=${a.acceptCondition.toString().replace("\n", " ").take(200)}")
        }
      }
      println("  condPathFinishes@18: ${entry.condPathFinishes.keys}")
      println("  condPathFinishes@19: ${m3Ctx.history[19].condPathFinishes.keys}")
    }

    // 5. cond symbol family: what they are + m2 vs m3 at early gens
    val condSyms = setOf(30, 37, 38, 39, 40, 41)
    println("=== cond symbol identities ===")
    condSyms.sorted().forEach { id ->
      println("  $id: ${grammar.symbolOf(id)}")
    }
    for (gen in 0..4) {
      println("=== gen $gen: cond syms m2 (with sources) vs m3 ===")
      m2Hist.getOrNull(gen)?.kernels?.filter { it.symbolId in condSyms }
        ?.sortedWith(kernelOrder)?.forEach { k ->
          println("  m2 ${fmt(k)} <- ${attribution[gen]?.get(k)?.sorted()?.joinToString(" | ")}")
        }
      m3Hist.getOrNull(gen)?.kernels?.filter { it.symbolId in condSyms }
        ?.sortedWith(kernelOrder)?.forEach { k -> println("  m3 ${fmt(k)}") }
    }
  }

  // Replays ktparser MilestoneGroupParserKt.kernelsHistory's emission walk,
  // tagging each produced kernel with its source. No condition filtering.
  private fun attributeM2Sources(
    m2Ctx: ParsingContextKt,
    m2DataKt: MilestoneGroupParserDataKt,
  ): Map<Int, Map<Kernel, Set<String>>> {
    val result = mutableMapOf<Int, MutableMap<Kernel, MutableSet<String>>>()
    fun add(gen: Int, k: Kernel, src: String) {
      result.getOrPut(gen) { mutableMapOf() }.getOrPut(k) { mutableSetOf() }.add(src)
    }

    fun protoK(k: MilestoneParserDataProto.Kernel, genMap: Map<Int, Int>): Kernel =
      Kernel(k.symbolId, k.pointer, genMap.getValue(k.beginGen), genMap.getValue(k.endGen))

    fun addSummary(
      gen: Int,
      ts: MilestoneParserDataProto.TasksSummary2,
      genMap: Map<Int, Int>,
      src: String,
    ) {
      ts.addedKernelsList.forEach { pair ->
        pair.kernelsList.forEach { add(gen, protoK(it, genMap), "$src/added") }
      }
      ts.progressedKernelsList.forEach { add(gen, protoK(it, genMap), "$src/prog") }
    }

    // gen 0 of the output comes from initialTasksSummary with all tags -> 0.
    addSummary(0, m2DataKt.initialTasksSummary, mapOf(-1 to 0, 0 to 0, 1 to 0, 2 to 0), "initial")

    val history = m2Ctx.history.toList()
    history.forEachIndexed { gen, entry ->
      if (gen == 0) return@forEachIndexed
      val ga = entry.genActions
      for ((mgroup, termAction) in ga.termActions) {
        addSummary(
          gen, termAction.tasksSummary,
          mapOf(0 to mgroup.gen, 1 to gen - 1, 2 to gen),
          "term[mg@${mgroup.gen}]",
        )
      }
      for ((edge, edgeAction) in ga.tipEdgeActions) {
        addSummary(
          gen, edgeAction.tasksSummary,
          mapOf(0 to edge.first.gen, 1 to edge.second.gen, 2 to gen),
          "tipEdge[${edge.first.gen}->${edge.second.gen}]",
        )
      }
      for ((edge, edgeAction) in ga.midEdgeActions) {
        addSummary(
          gen, edgeAction.tasksSummary,
          mapOf(0 to edge.first.gen, 1 to edge.second.gen, 2 to gen),
          "midEdge[${edge.first.gen}->${edge.second.gen}]",
        )
      }
      for ((kernelPair, _) in ga.progressedKernels) {
        val (milestone, parentGen) = kernelPair
        add(gen, Kernel(milestone.symbolId, milestone.pointer, parentGen, milestone.gen), "progKernel")
        add(gen, Kernel(milestone.symbolId, milestone.pointer + 1, parentGen, gen), "progKernel")
      }
      for ((milestone, _) in ga.progressedRootMilestones) {
        add(gen, Kernel(milestone.symbolId, milestone.pointer, milestone.gen, milestone.gen), "progRootMilestone")
        add(gen, Kernel(milestone.symbolId, milestone.pointer + 1, milestone.gen, gen), "progRootMilestone")
      }
      for ((kgroupPair, _) in ga.progressedKgroups) {
        val (mgroup, parentGen) = kgroupPair
        m2DataKt.milestonesOfGroup(mgroup.groupId).forEach { m ->
          add(gen, Kernel(m.symbolId, m.pointer, parentGen, mgroup.gen), "progKgroup")
          add(gen, Kernel(m.symbolId, m.pointer + 1, parentGen, gen), "progKgroup")
        }
      }
      for ((mgroup, _) in ga.progressedRootMgroups) {
        m2DataKt.milestonesOfGroup(mgroup.groupId).forEach { m ->
          add(gen, Kernel(m.symbolId, m.pointer, mgroup.gen, mgroup.gen), "progRootMgroup")
          add(gen, Kernel(m.symbolId, m.pointer + 1, mgroup.gen, gen), "progRootMgroup")
        }
      }
    }
    return result
  }

  private val kernelOrder =
    compareBy<Kernel>({ it.symbolId }, { it.pointer }, { it.beginGen }, { it.endGen })

  private fun fmt(k: Kernel) = "(${k.symbolId},${k.pointer},${k.beginGen}..${k.endGen})"

  /**
   * AST 동등성 검증: 생성된 AST 코드가 쓰는 것과 동일한 kernelsHistory 쿼리
   * (checkSingle/getSequenceElems/unrollRepeat0/1)로 NGrammar 구조를 따라
   * 파스 트리를 재구성해 m2/m3 가 동일한 트리를 주는지 비교한다.
   * AST 값은 트리(+입력)의 순수 함수이므로 트리 동일 ⇒ AST 동일.
   */
  @Test
  fun astWalkEquivalence() {
    val analysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(asdlGrammar, "Defs")
    val grammar = analysis.ngrammar()

    val m2Data = MilestoneGroupParserGen(grammar).parserData()
    val m2Proto = MilestoneGroupParserDataProtobufConverter.toProto(m2Data)
    val m2Parser = MilestoneGroupParserKt(MilestoneGroupParserDataKt(m2Proto))
    val m3Parser = Mgroup3Parser(Mgroup3ParserGenerator(grammar).generate())

    val inputs = listOf(
      "module M { foo = Bar(int x) | Baz }",
      "module M { a = (x y) }",
      "module Mod { foo = Bar(int x, str y) | Baz | Qux\n  attributes (int z) }",
      "module M {\n  -- comment here\n  foo = Bar(int x)\n}",
    )
    var failures = 0
    for (inp in inputs) {
      val m2Hist = m2Parser.kernelsHistory(m2Parser.parse(inp))
      val m3Ctx = m3Parser.parse(inp)
      check(m3Parser.isAccepted(m3Ctx)) { "m3 rejected: $inp" }
      val m3Hist = m3Parser.kernelsHistory(m3Ctx)
      val last = m2Hist.size - 1
      check(m3Hist.size == m2Hist.size)

      val t2 = runCatching {
        StringBuilder().also { walkTree(grammar, m2Hist, grammar.startSymbol(), 0, last, it, 0) }.toString()
      }
      val t3 = runCatching {
        StringBuilder().also { walkTree(grammar, m3Hist, grammar.startSymbol(), 0, last, it, 0) }.toString()
      }
      val ok = t2.isSuccess && t3.isSuccess && t2.getOrNull() == t3.getOrNull()
      println("=== AST walk [${if (ok) "OK" else "MISMATCH"}] '$inp'")
      if (!ok) {
        failures++
        println("  m2: ${t2.exceptionOrNull()?.message ?: "tree ok (${t2.getOrNull()?.lines()?.size} lines)"}")
        println("  m3: ${t3.exceptionOrNull()?.message ?: "tree ok (${t3.getOrNull()?.lines()?.size} lines)"}")
        if (t2.isSuccess && t3.isSuccess) {
          val l2 = t2.getOrThrow().lines()
          val l3 = t3.getOrThrow().lines()
          for (i in 0 until maxOf(l2.size, l3.size)) {
            val a = l2.getOrNull(i)
            val b = l3.getOrNull(i)
            if (a != b) {
              println("  first diff at line $i:\n    m2: $a\n    m3: $b")
              break
            }
          }
        }
      }
    }
    org.junit.jupiter.api.Assertions.assertEquals(0, failures, "AST walk mismatch")
  }

  // 완성 kernel 판정: sequence 는 (id, len, b, e), 그 외는 (id, 1, b, e).
  private fun isComplete(
    g: com.giyeok.jparser.NGrammar,
    hist: List<com.giyeok.jparser.ktlib.KernelSet>,
    symId: Int,
    b: Int,
    e: Int,
  ): Boolean = when (val sym = g.symbolOf(symId)) {
    is com.giyeok.jparser.NGrammar.NSequence ->
      hist[e].contains(Kernel(symId, sym.sequence().length(), b, e))
    else -> hist[e].contains(Kernel(symId, 1, b, e))
  }

  private fun walkTree(
    g: com.giyeok.jparser.NGrammar,
    hist: List<com.giyeok.jparser.ktlib.KernelSet>,
    symId: Int,
    begin: Int,
    end: Int,
    sb: StringBuilder,
    depth: Int,
  ) {
    check(depth < 200) { "depth overflow at sym=$symId [$begin..$end]" }
    val sym = g.symbolOf(symId)
    sb.append("  ".repeat(depth)).append("$symId[$begin..$end] ${sym.javaClass.simpleName}\n")

    fun walkSingleBody(bodyId: Int) = walkTree(g, hist, bodyId, begin, end, sb, depth + 1)

    fun pickProduce(produces: Collection<Int>) {
      val matched = produces.filter { isComplete(g, hist, it, begin, end) }
      check(matched.size == 1) {
        "sym=$symId [$begin..$end]: expected single matched produce, got $matched of $produces"
      }
      walkSingleBody(matched.single())
    }

    when (sym) {
      is com.giyeok.jparser.NGrammar.NTerminal -> {}
      is com.giyeok.jparser.NGrammar.NLookaheadIs -> {}
      is com.giyeok.jparser.NGrammar.NLookaheadExcept -> {}
      is com.giyeok.jparser.NGrammar.NSequence -> {
        val len = sym.sequence().length()
        if (len > 0) {
          val elems = (0 until len).map { sym.sequence().apply(it) as Int }
          val spans = com.giyeok.jparser.ktlib.getSequenceElems(hist, symId, elems, begin, end)
          for (i in elems.indices) {
            walkTree(g, hist, elems[i], spans[i].first, spans[i].second, sb, depth + 1)
          }
        }
      }
      is com.giyeok.jparser.NGrammar.NRepeat -> {
        val lower = sym.symbol().lower()
        val repeatSeq = g.symbolOf(sym.repeatSeq()) as com.giyeok.jparser.NGrammar.NSequence
        val itemSymId = repeatSeq.sequence().apply(1) as Int
        val items = if (lower == 0) {
          com.giyeok.jparser.ktlib.unrollRepeat0(hist, symId, itemSymId, sym.baseSeq(), sym.repeatSeq(), begin, end)
        } else {
          com.giyeok.jparser.ktlib.unrollRepeat1(hist, symId, itemSymId, sym.baseSeq(), sym.repeatSeq(), begin, end)
        }
        for (s in items) walkTree(g, hist, itemSymId, s.first, s.second, sb, depth + 1)
      }
      is com.giyeok.jparser.NGrammar.NStart -> walkSingleBody(sym.produce())
      is com.giyeok.jparser.NGrammar.NProxy -> walkSingleBody(sym.produce())
      is com.giyeok.jparser.NGrammar.NNonterminal ->
        pickProduce(CollectionConverters.asJava(sym.produces()).map { it as Int })
      is com.giyeok.jparser.NGrammar.NOneOf ->
        pickProduce(CollectionConverters.asJava(sym.produces()).map { it as Int })
      is com.giyeok.jparser.NGrammar.NExcept -> walkSingleBody(sym.body())
      is com.giyeok.jparser.NGrammar.NJoin -> walkSingleBody(sym.body())
      is com.giyeok.jparser.NGrammar.NLongest -> walkSingleBody(sym.body())
      else -> error("unsupported symbol kind: $sym")
    }
  }
}
