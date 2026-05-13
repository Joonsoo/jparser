package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3.codegen.KotlinOptCodeGen
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

object Stage3KotlinEmit {
  fun run(
    processed: ProcessedGrammar,
    outDir: Path,
    className: String = "Ast",
    pkg: String = "com.giyeok.jparser.mgroup3.generated",
  ) {
    val codegen = KotlinOptCodeGen(processed)
    val src = codegen.generate(className, pkg)
    val pkgDir = outDir.resolve(pkg.replace('.', '/'))
    pkgDir.createDirectories()
    pkgDir.resolve("$className.kt").writeText(src)
    pkgDir.resolve("${className}ProtoBinding.kt").writeText(stubBinding(className, pkg))
  }

  private fun stubBinding(className: String, pkg: String): String = """
    |package $pkg
    |
    |// TODO: Stage 3 — proto ↔ $className 양방향 변환 구현.
    |// 현재는 stub. 후속 PR 에서 채워야 함.
    |object ${className}ProtoBinding {
    |  fun toProto(ast: Any): ByteArray = TODO("Stage 3 stub — see GenCli.kt")
    |  fun fromProto(bytes: ByteArray): Any = TODO("Stage 3 stub — see GenCli.kt")
    |}
    |""".trimMargin()
}
