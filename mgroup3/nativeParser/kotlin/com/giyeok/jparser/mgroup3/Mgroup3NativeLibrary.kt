package com.giyeok.jparser.mgroup3

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Loads `libmgroup3_native.{dylib,so,dll}` once per JVM and exposes cached
 * [MethodHandle]s for the C ABI declared in `mgroup3-native/src/ffi.rs`.
 *
 * Resolution order (first hit wins):
 *  1. `System.getenv("MGROUP3_NATIVE_LIB")` — absolute path to the dylib.
 *  2. Walk up from `System.getProperty("user.dir")` until a `mgroup3-native/`
 *     directory is found; try `target/release/libmgroup3_native.{ext}`, then
 *     `target/debug/...`. Same shape `Mgroup3NativeResultBridgeTest` already
 *     uses for the dump_result binary.
 *  3. Throw [UnsatisfiedLinkError] with a clear message.
 *
 * `--enable-native-access=ALL-UNNAMED` must be on the JVM command line. On
 * JDK 25 missing the flag yields a hard `IllegalCallerException` from FFM.
 * The class-load-time smoke probe ([versionString]) surfaces the failure
 * here rather than at the first parse call.
 */
internal object Mgroup3NativeLibrary {
  // Long lived — symbols stay valid for the JVM's lifetime.
  private val symbolArena: Arena = Arena.ofShared()
  private val linker: Linker = Linker.nativeLinker()
  private val lookup: SymbolLookup = SymbolLookup.libraryLookup(resolveLibraryPath(), symbolArena)

  val mgroup3ParserNew: MethodHandle = downcall(
    "mgroup3_parser_new",
    FunctionDescriptor.of(
      ValueLayout.ADDRESS, // returns *mut Mgroup3Parser
      ValueLayout.ADDRESS, // data_bytes
      ValueLayout.JAVA_LONG, // data_len (size_t — JNI long fits on LP64 macOS/Linux)
      ValueLayout.ADDRESS, // err *mut i32
    )
  )

  val mgroup3ParserNewFromFile: MethodHandle = downcall(
    "mgroup3_parser_new_from_file",
    FunctionDescriptor.of(
      ValueLayout.ADDRESS, // returns *mut Mgroup3Parser
      ValueLayout.ADDRESS, // path *const c_char
      ValueLayout.ADDRESS, // err *mut i32
    )
  )

  val mgroup3ParserParse: MethodHandle = downcall(
    "mgroup3_parser_parse",
    FunctionDescriptor.of(
      ValueLayout.JAVA_INT, // returns i32 status code
      ValueLayout.ADDRESS, // parser
      ValueLayout.ADDRESS, // input_bytes
      ValueLayout.JAVA_LONG, // input_len
      ValueLayout.ADDRESS, // out_ptr **u8
      ValueLayout.ADDRESS, // out_len *usize
    )
  )

  val mgroup3FreeBuffer: MethodHandle = downcall(
    "mgroup3_free_buffer",
    FunctionDescriptor.ofVoid(
      ValueLayout.ADDRESS, // ptr
      ValueLayout.JAVA_LONG, // len
    )
  )

  val mgroup3ParserFree: MethodHandle = downcall(
    "mgroup3_parser_free",
    FunctionDescriptor.ofVoid(
      ValueLayout.ADDRESS, // parser
    )
  )

  private val mgroup3NativeVersion: MethodHandle = downcall(
    "mgroup3_native_version",
    FunctionDescriptor.of(ValueLayout.ADDRESS),
  )

  /**
   * Static-init smoke check. Calling [versionString] eagerly verifies the
   * dylib loaded and at least one downcall works — catches missing
   * `--enable-native-access=ALL-UNNAMED` early with a readable error.
   */
  init {
    val v = versionString()
    require(v.isNotEmpty()) { "mgroup3_native_version returned empty string" }
  }

  fun versionString(): String {
    val addr = mgroup3NativeVersion.invokeExact() as MemorySegment
    if (addr.address() == 0L) return ""
    return addr.reinterpret(Long.MAX_VALUE).getString(0)
  }

  private fun downcall(name: String, descriptor: FunctionDescriptor): MethodHandle {
    val addr = lookup.find(name).orElseThrow {
      UnsatisfiedLinkError("symbol '$name' not found in libmgroup3_native")
    }
    return linker.downcallHandle(addr, descriptor)
  }

  private fun resolveLibraryPath(): Path {
    System.getenv("MGROUP3_NATIVE_LIB")?.let { env ->
      val p = Path.of(env)
      if (Files.exists(p)) return p
      throw UnsatisfiedLinkError("MGROUP3_NATIVE_LIB=$env but the file does not exist")
    }
    val ext = libExtension()
    val libFile = "libmgroup3_native.$ext"
    var dir: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
    while (dir != null) {
      val candidate = dir.resolve("mgroup3-native")
      if (Files.isDirectory(candidate)) {
        for (mode in listOf("release", "debug")) {
          val p = candidate.resolve("target").resolve(mode).resolve(libFile)
          if (Files.exists(p)) return p
        }
        throw UnsatisfiedLinkError(
          "found ${candidate.toAbsolutePath()} but neither target/release nor target/debug contains $libFile. " +
            "Build first: cd mgroup3-native && cargo build --release"
        )
      }
      dir = dir.parent
    }
    throw UnsatisfiedLinkError(
      "Could not locate mgroup3-native/. Set MGROUP3_NATIVE_LIB=/path/to/$libFile or " +
        "run this JVM from inside the jparser workspace after building the native library."
    )
  }

  private fun libExtension(): String {
    val osName = System.getProperty("os.name").lowercase()
    return when {
      osName.contains("mac") || osName.contains("darwin") -> "dylib"
      osName.contains("win") -> "dll"
      else -> "so"
    }
  }
}

// ----- internal helpers used by Mgroup3NativeParser -----

/** Allocate a confined arena scope; used per-call. */
internal inline fun <T> withConfinedArena(block: (Arena) -> T): T =
  Arena.ofConfined().use { arena -> block(arena) }

/** Track a one-shot close. */
internal class OnceCloseFlag {
  private val closed = AtomicBoolean(false)
  /** Returns true if this caller is the one that flipped the flag. */
  fun tryClose(): Boolean = closed.compareAndSet(false, true)
  fun isClosed(): Boolean = closed.get()
}
