package com.giyeok.jparser.mgroup3.visualize

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.GenNode
import com.giyeok.jparser.mgroup3.gen.GenNodeGeneration.*
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.proto.GrammarProtobufConverter
import com.google.gson.Gson
import com.google.protobuf.util.JsonFormat
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.io.path.Path
import kotlin.io.path.readText

fun main() {
  val cdg = Path("examples/metalang3/resources/cmake/cmake_debug.cdg").readText()

  val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
  val grammar = grammarAnalysis.ngrammar()

  val grammarProto = GrammarProtobufConverter.convertNGrammarToProto(grammar)
  val grammarJson = JsonFormat.printer().print(grammarProto)

  val gson = Gson()

  val gen = Mgroup3ParserGenerator(grammar)
  val graph = gen.tasks.derivedFrom(
    setOf(GenNode(grammar.startSymbol(), 0, Curr, Curr))
  )

  embeddedServer(Netty, port = 8000) {
    install(CORS) {
      anyHost()
    }
    routing {
      get("/grammar") {
        call.respondText(grammarJson)
      }

      get("/graph0") {
        call.respondText(
          gson.toJson(
            mapOf(
              "nodes" to graph.nodes,
              "edges" to graph.edges,
            )
          )
        )
      }
    }
  }.start(wait = true)
}
