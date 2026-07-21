package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

// 논문용: 문법별 milestone group(=MGACP 의 "그룹") 인벤토리 수 = LR state count 대응.
// group 인벤토리는 Mgroup3ParserGenerator 의 milestoneGroups (HashBiMap<Int, Set<KernelTemplate>>)
// 로 생성되고 proto 의 map<int32, MilestoneGroup> milestone_groups (field 4) 로 직렬화된다.
// 그룹 수 = milestoneGroupsCount, 커널 템플릿 총수(중복 포함) = sum(kernelsCount),
// distinct 커널 템플릿 = 서로 다른 (symbolId, pointer) 수.
// 실행: bibix4 runGroupInventoryStats
@EnabledIfEnvironmentVariable(named = "GROUP_STATS", matches = "1")
class GroupInventoryStatsTest {
  data class Row(
    val name: String,
    val groups: Int,
    val kernelsTotal: Int,
    val distinctKernels: Int,
    val note: String,
  )

  private fun statsFromData(name: String, data: Mgroup3ParserData, note: String): Row {
    val groups = data.milestoneGroupsCount
    var kernelsTotal = 0
    val distinct = HashSet<Long>()
    for ((_, mg) in data.milestoneGroupsMap) {
      kernelsTotal += mg.kernelsCount
      for (kt in mg.kernelsList) {
        distinct.add((kt.symbolId.toLong() shl 32) or (kt.pointer.toLong() and 0xFFFFFFFFL))
      }
    }
    return Row(name, groups, kernelsTotal, distinct.size, note)
  }

  private fun genStats(name: String, cdgPath: Path, start: String): Row? {
    if (!cdgPath.exists()) {
      println("$name: $cdgPath not found, skipping"); return null
    }
    val grammar = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdgPath.readText(), start).ngrammar()
    val t0 = System.nanoTime()
    val data = Mgroup3ParserGenerator(grammar).generate()
    val genMs = (System.nanoTime() - t0) / 1e6
    return statsFromData(name, data, "generated %.0fms".format(genMs))
  }

  // Mulang on-disk fixture 대조 행.
  private fun mulangFixtureRow(): Row? {
    val fixturePath = Path.of("mgroup3-native/tests/fixtures/parser_generated/mulang/data.pb")
    if (!fixturePath.exists()) {
      println("Mulang fixture not found at ${fixturePath.toAbsolutePath()}"); return null
    }
    val sizeBytes = Files.size(fixturePath)
    val data = BufferedInputStream(FileInputStream(fixturePath.toFile())).use {
      Mgroup3ParserData.parseFrom(it)
    }
    return statsFromData("Mulang-fixture", data, "on-disk fixture $sizeBytes bytes")
  }

  @Test
  fun groupCounts() {
    // pinned Mulang 재계산 모드: MULANG_PINNED_CDG=<cdg 파일 경로> 이면 그것만 (+ fixture 대조).
    val pinnedCdg = System.getenv("MULANG_PINNED_CDG")
    if (pinnedCdg != null) {
      val start = System.getenv("MULANG_START") ?: "CompileUnit"
      val rows = mutableListOf<Row>()
      genStats("Mulang-pinned", Path.of(pinnedCdg), start)?.let { rows += it }
      mulangFixtureRow()?.let { rows += it }
      println()
      println("=== Mulang pinned vs on-disk fixture (start=$start, cdg=$pinnedCdg) ===")
      println("%-16s %8s %12s %14s  %s".format("grammar", "groups", "kernelsTot", "distinctKt", "note"))
      for (r in rows) {
        println("%-16s %8d %12d %14d  %s".format(r.name, r.groups, r.kernelsTotal, r.distinctKernels, r.note))
      }
      println()
      println("CSV grammar,groups,kernelsTotal,distinctKernels")
      for (r in rows) println("CSV ${r.name},${r.groups},${r.kernelsTotal},${r.distinctKernels}")
      return
    }

    val rows = mutableListOf<Row>()
    genStats("JSON", Path.of("examples/metalang3/resources/json/grammar.cdg"), "json")?.let { rows += it }
    genStats("Proto3", Path.of("examples/metalang3/resources/proto3/grammar.cdg"), "proto3")?.let { rows += it }
    genStats("ES5.1", Path.of("examples/metalang3/resources/es5/grammar.cdg"), "Program")?.let { rows += it }

    // Mulang: pinned fixture 만 로드 (HEAD 문법으로 재생성 금지). 테스트가 로드하는 data.pb.
    val fixturePath = Path.of("mgroup3-native/tests/fixtures/parser_generated/mulang/data.pb")
    if (fixturePath.exists()) {
      val sizeBytes = Files.size(fixturePath)
      val data = BufferedInputStream(FileInputStream(fixturePath.toFile())).use {
        Mgroup3ParserData.parseFrom(it)
      }
      rows += statsFromData("Mulang", data, "fixture $sizeBytes bytes")
    } else {
      println("Mulang: fixture not found at ${fixturePath.toAbsolutePath()}")
    }

    println()
    println("%-10s %8s %12s %14s  %s".format("grammar", "groups", "kernelsTot", "distinctKt", "note"))
    for (r in rows) {
      println("%-10s %8d %12d %14d  %s".format(r.name, r.groups, r.kernelsTotal, r.distinctKernels, r.note))
    }
    println()
    println("CSV grammar,groups,kernelsTotal,distinctKernels,groupsPerDistinctKt")
    for (r in rows) {
      val ratio = if (r.distinctKernels > 0) r.groups.toDouble() / r.distinctKernels else 0.0
      println("CSV ${r.name},${r.groups},${r.kernelsTotal},${r.distinctKernels},%.3f".format(ratio))
    }
  }
}
