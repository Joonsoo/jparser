package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.lang.management.ManagementFactory
import kotlin.concurrent.thread

// Simple sampling profiler for mgroup3. Spawns a sampler thread that
// captures the parsing thread's stack at fixed intervals while a large
// parse runs in a loop; reports per-method sample counts grouped by
// stack-frame containment (so "Mgroup3Parser.parseStep" rolls up time
// spent inside applyTermAction, evolveAcceptCondition, etc.).
class Mgroup3ProfilingTest {
  @Test
  @Disabled("manual profiling. Unannotate to run via runMgroup3Profile action.")
  fun profile() {
    val grammarText = """
      Grammar = Expr
      Expr = N | Expr WS Op&OpTk WS N
      N = '0-9'+
      OpTk = <Op>
      Op = ('+' | '-' | "||")+
      WS = ' '*
    """.trimIndent()
    val grammar = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(grammarText, "Grammar").ngrammar()
    val parser = Mgroup3Parser(Mgroup3ParserGenerator(grammar).generate())

    // Big input
    val ops = listOf("||", "+", "-", "||", "+", "||")
    val sb = StringBuilder()
    sb.append("1")
    for (i in 1 until 1000) {
      sb.append(' ')
      sb.append(ops[i % ops.size])
      sb.append(' ')
      sb.append(((i % 9) + 1).toString())
    }
    val input = sb.toString()
    println("Profile input: ${input.length} chars")

    // Warm up.
    repeat(5) { parser.parse(input) }

    // Phase timing.
    parser.enablePhaseTiming()
    repeat(10) { parser.parse(input) }
    println("Phase timing (10 parses): ${parser.reportPhaseTimers()}")
    parser.disablePhaseTiming()

    val parsingThread = Thread.currentThread()
    val threadId = parsingThread.threadId()
    val threadMx = ManagementFactory.getThreadMXBean()

    val inclusiveSamples = mutableMapOf<String, Int>()
    val topFrameSamples = mutableMapOf<String, Int>()  // exclusive (top of stack)
    val callerOfHash = mutableMapOf<String, Int>()  // HashMap.hash 의 caller
    val mgroup3CallSites = mutableMapOf<String, Int>()  // top-most mgroup3 frame in stack
    val stop = java.util.concurrent.atomic.AtomicBoolean(false)
    val sampler = thread(start = true, isDaemon = true, name = "sampler") {
      while (!stop.get()) {
        val info = threadMx.getThreadInfo(threadId, 200)
        if (info != null) {
          val stack = info.stackTrace
          if (stack.isNotEmpty()) {
            val top = stack[0]
            val topKey = "${top.className}.${top.methodName}"
            topFrameSamples[topKey] = (topFrameSamples[topKey] ?: 0) + 1

            // HashMap.hash 호출 시 caller 찾기.
            if (topKey == "java.util.HashMap.hash" && stack.size > 1) {
              for (i in 1 until stack.size) {
                val callerKey = "${stack[i].className}.${stack[i].methodName}"
                if (callerKey.contains("mgroup3")) {
                  callerOfHash[callerKey] = (callerOfHash[callerKey] ?: 0) + 1
                  break
                }
              }
            }

            // top-most mgroup3 frame — 어떤 mgroup3 method 가 hot 한지 (사이에 java/jdk 호출 끼면 그 위).
            for (frame in stack) {
              val cls = frame.className
              if (cls.startsWith("com.giyeok.jparser.mgroup3")) {
                val key = "$cls.${frame.methodName}"
                mgroup3CallSites[key] = (mgroup3CallSites[key] ?: 0) + 1
                break
              }
            }

            // inclusive (any frame).
            val seen = mutableSetOf<String>()
            for (frame in stack) {
              val key = "${frame.className}.${frame.methodName}"
              if (seen.add(key)) {
                inclusiveSamples[key] = (inclusiveSamples[key] ?: 0) + 1
              }
            }
          }
        }
        try { Thread.sleep(1) } catch (e: InterruptedException) { break }
      }
    }

    val totalParses = 30
    val start = System.currentTimeMillis()
    repeat(totalParses) {
      parser.parse(input)
    }
    val elapsed = System.currentTimeMillis() - start
    stop.set(true)
    sampler.join(1000)

    println("Total parses: $totalParses, elapsed: ${elapsed}ms (avg ${elapsed.toDouble() / totalParses}ms/parse)")
    println()

    fun printTop(name: String, samples: Map<String, Int>) {
      val total = samples.values.sum()
      println()
      println("=== $name (total samples: $total) ===")
      samples.entries.sortedByDescending { it.value }.take(30).forEach { (key, count) ->
        val pct = 100.0 * count / total
        println("  %5d (%5.1f%%) %s".format(count, pct, key))
      }
    }

    printTop("Top of stack (exclusive — actual hot method)", topFrameSamples)
    printTop("Hot mgroup3 method (top-most mgroup3 frame in stack)", mgroup3CallSites)
    printTop("Callers of java.util.HashMap.hash (mgroup3 only)", callerOfHash)
    printTop("Inclusive (any frame in stack)", inclusiveSamples)
  }
}
