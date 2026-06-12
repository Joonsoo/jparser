package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.mgroup3.generated.Ast
import com.giyeok.jparser.mgroup3.generated.AstProtoBinding
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText

/**
 * Phase B Kotlin 측 end-to-end:
 *   mgroup3 파서 → kernelsHistory → 생성된 walk(Ast.matchStart) → typed AST
 *   → AstProtoBinding.toProto → fromProto 라운드트립.
 *
 * `examples/generated/kotlin/.../Ast{,ProtoBinding}.kt` 는 GenCli 가
 * `examples/metalang3/resources/asdl/grammar.cdg` 로 생성한 스냅샷이다.
 * (재생성: genCliJar 빌드 후 GenCli 실행 — Stage3KotlinEmit/Stage2ProtoEmit 참고.
 *  스냅샷과 함께 examples/generated/proto/ast.proto 도 갱신해야 한다.)
 *
 * Rust 측 대응 검증은 생성 crate 의 check_ast bin (Stage4RustEmit).
 */
class Mgroup3AstRoundtripTest {
  private val inputs = listOf(
    "module M { foo = Bar(int x) | Baz }",
    "module M { a = (x y) }",
    "module Mod { foo = Bar(int x, str y) | Baz | Qux\n  attributes (int z) }",
    "module M {\n  -- comment here\n  foo = Bar(int x)\n}",
  )

  @Test
  fun walkAndProtoRoundtrip() {
    val cdg = Path("examples/metalang3/resources/asdl/grammar.cdg").readText()
    val analysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "AsdlGrammar")
    val parser = Mgroup3Parser(Mgroup3ParserGenerator(analysis.ngrammar()).generate())

    for (input in inputs) {
      val ctx = parser.parseOrThrow(input)
      val history = parser.kernelsHistory(ctx)

      // 생성된 walk 로 typed AST 구성.
      val ast = Ast(input, history).matchStart()
      val shortString = ast.toShortString()
      println("AST  ${input.replace("\n", "\\n")}\n  => $shortString")

      // proto 라운드트립: 의미 보존 (shortString/span) + 재인코딩 바이트 안정성.
      val proto = AstProtoBinding.toProto(ast)
      val decoded = AstProtoBinding.fromProto(proto)
      assertEquals(shortString, decoded.toShortString(), "roundtrip shortString mismatch: $input")
      assertEquals(ast.start, decoded.start)
      assertEquals(ast.end, decoded.end)
      assertArrayEquals(
        proto.toByteArray(),
        AstProtoBinding.toProto(decoded).toByteArray(),
        "re-encode bytes mismatch: $input",
      )

      // bytes 경유 경로도 동일해야 함.
      val viaBytes = AstProtoBinding.fromProtoBytes(AstProtoBinding.toProtoBytes(ast))
      assertEquals(shortString, viaBytes.toShortString())
    }
  }
}
