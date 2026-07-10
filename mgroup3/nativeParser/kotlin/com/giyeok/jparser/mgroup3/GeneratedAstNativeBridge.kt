package com.giyeok.jparser.mgroup3

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * GenCli(Stage4RustEmit)가 생성한 per-grammar Rust crate 의 cdylib
 * (`cargo build --features ffi`) 를 FFM 으로 로드하는 브릿지.
 *
 * 생성 dylib 은 mgroup3-native 의 export (`mgroup3_parser_new_from_file`,
 * `mgroup3_parser_new_from_file_cached`, `mgroup3_parser_free`,
 * `mgroup3_free_buffer`)와 per-grammar 의
 * `mgroup3_gen_parse_ast` 를 함께 노출한다. `parseAst` 가 돌려주는 바이트는
 * 그 문법의 ast.proto `ParseResult` — Kotlin 쪽에서는 생성된
 * `AstProtoBinding.fromProtoBytes` 로 typed AST 를 복원한다.
 *
 * [Mgroup3NativeLibrary] 와 달리 dylib 경로를 받아 per-instance 로 로드한다
 * (문법마다 dylib 이 다르므로). 같은 JVM 에 서로 다른 문법의 dylib 을 여럿
 * 로드할 수 있지만, mgroup3-native 심볼이 중복 export 되므로 lookup 은
 * 인스턴스별 [SymbolLookup] 으로만 접근할 것.
 */
class GeneratedAstNativeBridge(libPath: Path) : AutoCloseable {
  private val arena: Arena = Arena.ofShared()
  private val linker: Linker = Linker.nativeLinker()
  private val lookup: SymbolLookup = SymbolLookup.libraryLookup(libPath, arena)

  private val parserNewFromFile: MethodHandle = downcall(
    "mgroup3_parser_new_from_file",
    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
  )
  private val parserNewFromFileCached: MethodHandle = downcall(
    "mgroup3_parser_new_from_file_cached",
    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
  )
  private val parserFree: MethodHandle = downcall(
    "mgroup3_parser_free",
    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
  )
  private val freeBuffer: MethodHandle = downcall(
    "mgroup3_free_buffer",
    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
  )
  private val genParseAst: MethodHandle = downcall(
    "mgroup3_gen_parse_ast",
    FunctionDescriptor.of(
      ValueLayout.JAVA_INT, // i32 status
      ValueLayout.ADDRESS, // parser
      ValueLayout.ADDRESS, // input_bytes
      ValueLayout.JAVA_LONG, // input_len
      ValueLayout.ADDRESS, // out_ptr **u8
      ValueLayout.ADDRESS, // out_len *usize
    ),
  )

  // ---------------------------------------------------------------------------
  // Incremental parse session (Phase I3) — ADDITIVE. 세션은 파서 핸들을 **빌린다**
  // (소유 X): 파서 핸들 하나가 여러 문서 세션을 뒷받침한다. 수명 계약 = 모든 세션을
  // sessionDestroy 한 뒤에만 freeParser (그 전 free 는 dangling deref). 같은 세션 핸들
  // 동시 호출 금지 (호출측이 문서별 직렬화). `pos_char`/`old_len_char` 는 코드포인트
  // 오프셋 (UTF-16/바이트 아님) — 변환은 호스트 책임. 결과 바이트는 `parseAst`
  // (= `mgroup3_gen_parse_ast`) 와 동일한 ast.proto ParseResult 인코딩이고, 전체
  // 재파스와 byte-identical 이 계약이다.

  private val genSessionNew: MethodHandle = downcall(
    "mgroup3_gen_session_new",
    FunctionDescriptor.of(
      ValueLayout.ADDRESS, // *mut ParseSession (핸들)
      ValueLayout.ADDRESS, // parser
      ValueLayout.ADDRESS, // err *i32 (out)
    ),
  )
  private val genSessionParseFull: MethodHandle = downcall(
    "mgroup3_gen_session_parse_full",
    FunctionDescriptor.of(
      ValueLayout.JAVA_INT, // i32 status
      ValueLayout.ADDRESS, // session
      ValueLayout.ADDRESS, // input_bytes
      ValueLayout.JAVA_LONG, // input_len
      ValueLayout.ADDRESS, // out_ptr **u8
      ValueLayout.ADDRESS, // out_len *usize
    ),
  )
  private val genSessionEdit: MethodHandle = downcall(
    "mgroup3_gen_session_edit",
    FunctionDescriptor.of(
      ValueLayout.JAVA_INT, // i32 status
      ValueLayout.ADDRESS, // session
      ValueLayout.JAVA_LONG, // pos_char (코드포인트 오프셋)
      ValueLayout.JAVA_LONG, // old_len_char (코드포인트)
      ValueLayout.ADDRESS, // new_bytes
      ValueLayout.JAVA_LONG, // new_len
      ValueLayout.ADDRESS, // out_ptr **u8
      ValueLayout.ADDRESS, // out_len *usize
    ),
  )
  private val genSessionDestroy: MethodHandle = downcall(
    "mgroup3_gen_session_destroy",
    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
  )

  /** parserdata 파일에서 파서 핸들 생성. 실패 시 throw. */
  fun newParserFromFile(parserDataPath: Path): MemorySegment =
    newParserFromFileVia(parserNewFromFile, "mgroup3_parser_new_from_file", parserDataPath)

  /**
   * parserdata 파일에서 파서 핸들 생성 — 첫 로드에서 sibling `.rkyv` 캐시를 굽고
   * 이후 로드는 mmap 으로 재사용해 prost decode 를 건너뛴다 (warm ~125ms).
   * 경로가 `.gz` 로 끝나면 Rust 쪽이 gunzip 한다 (JVM 에서 풀 필요 없음).
   * 산출 파서는 [newParserFromFile] 과 동일한 semantics. 실패 시 throw
   * (에러 규약 동일 + 코드 6 = cache orchestration 실패).
   */
  fun newParserFromFileCached(parserDataPath: Path): MemorySegment =
    newParserFromFileVia(
      parserNewFromFileCached,
      "mgroup3_parser_new_from_file_cached",
      parserDataPath,
    )

  private fun newParserFromFileVia(
    handleFn: MethodHandle,
    symbolName: String,
    parserDataPath: Path,
  ): MemorySegment = withConfinedArena { arena ->
    val pathBytes = parserDataPath.toAbsolutePath().toString().toByteArray(StandardCharsets.UTF_8)
    val pathSeg = arena.allocate((pathBytes.size + 1).toLong())
    MemorySegment.copy(pathBytes, 0, pathSeg, ValueLayout.JAVA_BYTE, 0, pathBytes.size)
    pathSeg.set(ValueLayout.JAVA_BYTE, pathBytes.size.toLong(), 0)
    val errSeg = arena.allocate(ValueLayout.JAVA_INT)
    val handle = handleFn.invokeExact(pathSeg, errSeg) as MemorySegment
    val err = errSeg.get(ValueLayout.JAVA_INT, 0)
    check(err == 0 && handle.address() != 0L) {
      "$symbolName failed: err=$err path=$parserDataPath"
    }
    handle
  }

  fun freeParser(parser: MemorySegment) {
    parserFree.invokeExact(parser)
  }

  /**
   * parse → 생성된 AST walk → ast.proto ParseResult 인코딩 바이트.
   * 입력 거부 시 [GeneratedAstParseException] (code 6=parse 중 거부, 7=미수락).
   */
  fun parseAst(parser: MemorySegment, input: String): ByteArray = withConfinedArena { arena ->
    val inputBytes = input.toByteArray(StandardCharsets.UTF_8)
    val inputSeg = allocInputBytes(arena, inputBytes)
    val outPtrSeg = arena.allocate(ValueLayout.ADDRESS)
    val outLenSeg = arena.allocate(ValueLayout.JAVA_LONG)
    val code = genParseAst.invokeExact(
      parser,
      inputSeg,
      inputBytes.size.toLong(),
      outPtrSeg,
      outLenSeg,
    ) as Int
    if (code != 0) {
      throw GeneratedAstParseException(code, input)
    }
    readAndFreeOutBuffer(outPtrSeg, outLenSeg)
  }

  /**
   * 파서 핸들 위에 증분 파스 세션을 연다. 파서를 **빌린다** — 세션보다 파서가 오래
   * 살아야 한다 (수명 계약: 모든 세션 [sessionDestroy] 후에만 [freeParser]).
   * 실패 시 [GeneratedAstParseException].
   */
  fun sessionNew(parser: MemorySegment): MemorySegment = withConfinedArena { arena ->
    val errSeg = arena.allocate(ValueLayout.JAVA_INT)
    val session = genSessionNew.invokeExact(parser, errSeg) as MemorySegment
    val err = errSeg.get(ValueLayout.JAVA_INT, 0)
    if (err != 0 || session.address() == 0L) {
      throw GeneratedAstParseException(if (err != 0) err else 5, "<session_new>")
    }
    session
  }

  /**
   * 세션에 전문(全文) 재파스를 먹인다 (gen 0 부터 — 체크포인트 링 리셋). 결과 바이트는
   * [parseAst] 와 동일 인코딩. 입력 거부 시 [GeneratedAstParseException] (code 6/7).
   */
  fun sessionParseFull(session: MemorySegment, input: String): ByteArray =
    withConfinedArena { arena ->
      val inputBytes = input.toByteArray(StandardCharsets.UTF_8)
      val inputSeg = allocInputBytes(arena, inputBytes)
      val outPtrSeg = arena.allocate(ValueLayout.ADDRESS)
      val outLenSeg = arena.allocate(ValueLayout.JAVA_LONG)
      val code = genSessionParseFull.invokeExact(
        session,
        inputSeg,
        inputBytes.size.toLong(),
        outPtrSeg,
        outLenSeg,
      ) as Int
      if (code != 0) {
        throw GeneratedAstParseException(code, input)
      }
      readAndFreeOutBuffer(outPtrSeg, outLenSeg)
    }

  /**
   * 세션에 단일 치환 편집을 먹인다. [posChar]/[oldLenChar] 는 **코드포인트 오프셋**
   * (UTF-16/바이트 아님 — 호출측이 변환). 결과 바이트는 [parseAst] 와 동일 인코딩이고
   * 전문 재파스와 byte-identical.
   *
   * 파스 실패 (code 6/7) 시에도 Rust 세션 내부 문서(`self.doc`)는 편집이 이미 반영된
   * 상태이며 세션은 다음 edit 에 안전하다 (jparser 계약) — 이 메소드는 그 경우
   * [GeneratedAstParseException] 을 던지되 세션을 파괴하지 않는다. 호출측은 tracked
   * 텍스트를 편집 반영으로 갱신해야 Rust 문서와 어긋나지 않는다.
   */
  fun sessionEdit(
    session: MemorySegment,
    posChar: Long,
    oldLenChar: Long,
    newText: String,
  ): ByteArray = withConfinedArena { arena ->
    val newBytes = newText.toByteArray(StandardCharsets.UTF_8)
    val newSeg = allocInputBytes(arena, newBytes)
    val outPtrSeg = arena.allocate(ValueLayout.ADDRESS)
    val outLenSeg = arena.allocate(ValueLayout.JAVA_LONG)
    val code = genSessionEdit.invokeExact(
      session,
      posChar,
      oldLenChar,
      newSeg,
      newBytes.size.toLong(),
      outPtrSeg,
      outLenSeg,
    ) as Int
    if (code != 0) {
      throw GeneratedAstParseException(code, newText)
    }
    readAndFreeOutBuffer(outPtrSeg, outLenSeg)
  }

  /** 세션 핸들 해제. 빌린 파서는 해제하지 않는다 (별도 [freeParser], 모든 세션 후에). */
  fun sessionDestroy(session: MemorySegment) {
    genSessionDestroy.invokeExact(session)
  }

  private fun allocInputBytes(arena: Arena, bytes: ByteArray): MemorySegment =
    if (bytes.isEmpty()) {
      MemorySegment.NULL
    } else {
      val seg = arena.allocate(bytes.size.toLong())
      MemorySegment.copy(bytes, 0, seg, ValueLayout.JAVA_BYTE, 0, bytes.size)
      seg
    }

  /** out-param (out_ptr/out_len) 이 가리키는 Rust 버퍼를 복사한 뒤 `mgroup3_free_buffer`. */
  private fun readAndFreeOutBuffer(
    outPtrSeg: MemorySegment,
    outLenSeg: MemorySegment,
  ): ByteArray {
    val outAddr = outPtrSeg.get(ValueLayout.ADDRESS, 0)
    val outLen = outLenSeg.get(ValueLayout.JAVA_LONG, 0)
    return try {
      if (outAddr.address() == 0L || outLen == 0L) {
        ByteArray(0)
      } else {
        outAddr.reinterpret(outLen).toArray(ValueLayout.JAVA_BYTE)
      }
    } finally {
      if (outAddr.address() != 0L) {
        freeBuffer.invokeExact(outAddr, outLen)
      }
    }
  }

  override fun close() {
    arena.close()
  }

  private fun downcall(name: String, descriptor: FunctionDescriptor): MethodHandle {
    val addr = lookup.find(name).orElseThrow {
      UnsatisfiedLinkError("symbol '$name' not found in generated parser dylib")
    }
    return linker.downcallHandle(addr, descriptor)
  }
}

class GeneratedAstParseException(val code: Int, val input: String) : Exception(
  "mgroup3_gen_parse_ast failed (code=$code${codeName(code)}) for input: $input"
) {
  companion object {
    private fun codeName(code: Int): String = when (code) {
      1 -> " NULL_ARG"
      4 -> " UTF8"
      5 -> " PANIC"
      6 -> " PARSE"
      7 -> " REJECTED"
      else -> ""
    }
  }
}
