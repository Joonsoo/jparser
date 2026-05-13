package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import java.io.ByteArrayOutputStream
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream

/**
 * Kotlin façade over the `libmgroup3_native` C ABI. Mirrors the surface of
 * [Mgroup3Parser] (the Kotlin reference) but routes `parse()` through the
 * Rust implementation via FFM (JEP 454).
 *
 * Thread-safety: **not thread-safe**. Each instance wraps a Rust handle whose
 * internal term-action cache uses `RefCell`; concurrent calls on one handle
 * panic on the Rust side. Either instantiate per thread or guard externally.
 *
 * Lifecycle: implements [AutoCloseable]. Call [close] (or `use { ... }`) to
 * release the native handle. Calling [parse] after [close] throws
 * [IllegalStateException].
 *
 * Loading parser data: three flavours of factory.
 * - [fromParserDataFile] — pass a filesystem path; the multi-MB byte buffer
 *   stays in native memory. **Preferred for large grammars** (mulang.cdg
 *   etc.).
 * - [fromParserData] / [fromParserDataBytes] — bytes are already in JVM heap.
 *   Fine for tests and small grammars.
 * - [fromParserDataResource] — JAR-embedded resources go through JVM heap.
 */
class Mgroup3NativeParser private constructor(
  private val handle: MemorySegment,
) : AutoCloseable {

  private val closeFlag = OnceCloseFlag()

  /** Parse [input]; returns success or rejection wrapped as [Mgroup3NativeOutcome]. */
  fun parse(input: String): Mgroup3NativeOutcome {
    checkOpen()
    val bytes = input.toByteArray(StandardCharsets.UTF_8)
    return withConfinedArena { arena ->
      val resultBytes = invokeParse(arena, bytes)
      decodeMgroup3ParseResult(resultBytes)
    }
  }

  /** Like [parse], but throws [ParsingError] on rejection. */
  fun parseOrThrow(input: String): List<KernelSet> = parse(input).kernelsHistoryOrThrow()

  override fun close() {
    if (closeFlag.tryClose()) {
      Mgroup3NativeLibrary.mgroup3ParserFree.invokeExact(handle)
    }
  }

  private fun checkOpen() {
    check(!closeFlag.isClosed()) { "Mgroup3NativeParser is closed" }
  }

  private fun invokeParse(arena: Arena, input: ByteArray): ByteArray {
    val inputSegment: MemorySegment = if (input.isEmpty()) {
      MemorySegment.NULL
    } else {
      val seg = arena.allocate(input.size.toLong())
      MemorySegment.copy(input, 0, seg, ValueLayout.JAVA_BYTE, 0, input.size)
      seg
    }
    val outPtrSeg = arena.allocate(ValueLayout.ADDRESS)
    val outLenSeg = arena.allocate(ValueLayout.JAVA_LONG)

    val rc = Mgroup3NativeLibrary.mgroup3ParserParse.invokeExact(
      handle,
      inputSegment,
      input.size.toLong(),
      outPtrSeg,
      outLenSeg,
    ) as Int
    if (rc != 0) {
      throw IllegalStateException("mgroup3_parser_parse failed with rc=$rc")
    }

    val outAddr = outPtrSeg.get(ValueLayout.ADDRESS, 0)
    val outLen = outLenSeg.get(ValueLayout.JAVA_LONG, 0)
    return try {
      if (outAddr.address() == 0L || outLen == 0L) {
        ByteArray(0)
      } else {
        val resultSegment = outAddr.reinterpret(outLen)
        resultSegment.toArray(ValueLayout.JAVA_BYTE)
      }
    } finally {
      if (outAddr.address() != 0L) {
        Mgroup3NativeLibrary.mgroup3FreeBuffer.invokeExact(outAddr, outLen)
      }
    }
  }

  companion object {
    // --- Native-heap paths: prefer these for large grammars ---

    /**
     * Open the parser data from a filesystem path. The bytes never touch the
     * JVM heap. If [path] ends with `.gz` it is gunzipped on the native side.
     */
    fun fromParserDataFile(path: Path): Mgroup3NativeParser {
      val absolute = path.toAbsolutePath().toString()
      return withConfinedArena { arena ->
        val pathBytes = absolute.toByteArray(StandardCharsets.UTF_8)
        val pathSeg = arena.allocate((pathBytes.size + 1).toLong())
        MemorySegment.copy(pathBytes, 0, pathSeg, ValueLayout.JAVA_BYTE, 0, pathBytes.size)
        pathSeg.set(ValueLayout.JAVA_BYTE, pathBytes.size.toLong(), 0)
        val errSeg = arena.allocate(ValueLayout.JAVA_INT)
        val handle = Mgroup3NativeLibrary.mgroup3ParserNewFromFile
          .invokeExact(pathSeg, errSeg) as MemorySegment
        val err = errSeg.get(ValueLayout.JAVA_INT, 0)
        if (handle.address() == 0L) {
          throw IllegalStateException(
            "mgroup3_parser_new_from_file failed (path=$absolute, err=$err)"
          )
        }
        Mgroup3NativeParser(handle)
      }
    }

    // --- JVM-heap paths ---

    /** Convenience: serializes via [Mgroup3ParserData.toByteArray] first. */
    fun fromParserData(data: Mgroup3ParserData): Mgroup3NativeParser =
      fromParserDataBytes(data.toByteArray())

    /** Construct from already-decoded bytes; caller has gunzipped if needed. */
    fun fromParserDataBytes(bytes: ByteArray): Mgroup3NativeParser =
      withConfinedArena { arena ->
        val dataSeg = if (bytes.isEmpty()) {
          MemorySegment.NULL
        } else {
          val seg = arena.allocate(bytes.size.toLong())
          MemorySegment.copy(bytes, 0, seg, ValueLayout.JAVA_BYTE, 0, bytes.size)
          seg
        }
        val errSeg = arena.allocate(ValueLayout.JAVA_INT)
        val handle = Mgroup3NativeLibrary.mgroup3ParserNew
          .invokeExact(dataSeg, bytes.size.toLong(), errSeg) as MemorySegment
        val err = errSeg.get(ValueLayout.JAVA_INT, 0)
        if (handle.address() == 0L) {
          throw IllegalStateException(
            "mgroup3_parser_new failed (err=$err)"
          )
        }
        Mgroup3NativeParser(handle)
      }

    /**
     * Load parser data from a classpath resource. JAR-embedded resources have
     * no filesystem path the native side can read directly, so bytes go
     * through the JVM heap. Honors `.gz` suffix via [GZIPInputStream].
     */
    fun fromParserDataResource(
      name: String,
      loader: ClassLoader = Mgroup3NativeParser::class.java.classLoader,
    ): Mgroup3NativeParser {
      val stream = loader.getResourceAsStream(name)
        ?: throw IllegalArgumentException("resource not found: $name")
      val bytes = stream.use { input ->
        val effective = if (name.endsWith(".gz")) GZIPInputStream(input) else input
        val buf = ByteArrayOutputStream()
        effective.copyTo(buf)
        buf.toByteArray()
      }
      return fromParserDataBytes(bytes)
    }

    /** Optional helper: also auto-gunzips files that have the `.gz` suffix, mirroring native behavior. */
    fun fromParserDataFileGunzipIfNeeded(path: Path): Mgroup3NativeParser =
      // The native side already handles .gz so we just forward. This wrapper
      // exists for parity with [fromParserDataResource]'s behavior on the
      // resource path.
      fromParserDataFile(path)

    /** Probe the loaded dylib's version string. Mostly for debugging. */
    fun nativeVersion(): String = Mgroup3NativeLibrary.versionString()

    /** Smoke loader — touching this triggers Mgroup3NativeLibrary's static init. */
    fun ensureLoaded() {
      Mgroup3NativeLibrary.versionString()
    }

    // Keep Files import used.
    @Suppress("unused")
    private fun touchFiles(): Boolean = Files.exists(Path.of("/"))
  }
}
