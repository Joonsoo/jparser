package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3.codegen.RustOptCodeGen
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * Emits a per-grammar Rust crate that walks `kernels_history` into a typed AST
 * and encodes it to the ID-based proto `ParseResult`.
 *
 * DRAFT. The walk + encoder code is produced by [[RustOptCodeGen]] (Scala,
 * under metalang3) — the same way [[Stage3KotlinEmit]] delegates to
 * `KotlinOptCodeGen`. This stage only lays out the crate skeleton and the
 * static `ktlib.rs` support module.
 *
 * Crate layout:
 *   <rustDir>/
 *     Cargo.toml
 *     build.rs                 # prost-build over proto/ast.proto
 *     proto/ast.proto          # Stage2ProtoEmit (ID-based schema)
 *     src/lib.rs               # module tree + `pub mod proto { include!(...) }`
 *     src/ktlib.rs             # KernelSet + AstifierUtil port (static)
 *     src/ast.rs               # AST types + walk (RustOptCodeGen.generate)
 *     src/encode.rs            # typed AST -> proto (RustOptCodeGen.generateEncoder)
 *     src/bin/check_ast.rs     # end-to-end 검증 bin (feature `check-ast` 필요;
 *                              #   mgroup3-native 로 파싱→history→walk→short string)
 *
 * The `ktlib.rs` content is currently embedded here. TODO: move it into
 * `mgroup3-native` (or a dedicated `mgroup3-ktlib` crate) so generated crates
 * depend on it rather than each carrying a copy.
 */
object Stage4RustEmit {
  fun run(
    processed: ProcessedGrammar,
    schema: AstSchema,
    rustDir: Path,
    mgroup3NativePath: String,
    crateName: String = "mgroup3-generated-parser",
  ) {
    rustDir.createDirectories()
    val protoDir = rustDir.resolve("proto").also { it.createDirectories() }
    val srcDir = rustDir.resolve("src").also { it.createDirectories() }

    val codegen = RustOptCodeGen(processed)
    val astRs = codegen.generate()
    // generate() populates symbolsOfInterest etc. as a side effect; the encoder
    // only needs the class hierarchy, so order between the two does not matter.
    val encodeRs = codegen.generateEncoder()
    // AST-delta walk (Stage 2). MUST run after generate() (reuses _requiredNonterms).
    val deltaRs = DELTA_RT_RS + codegen.generateDelta()

    protoDir.resolve("ast.proto").writeText(Stage2ProtoEmit.emit(schema))
    rustDir.resolve("Cargo.toml").writeText(cargoToml(mgroup3NativePath, crateName))
    rustDir.resolve("build.rs").writeText(buildRs())
    srcDir.resolve("lib.rs").writeText(libRs(schema))
    srcDir.resolve("ktlib.rs").writeText(KTLIB_RS)
    srcDir.resolve("ast.rs").writeText(astRs)
    srcDir.resolve("encode.rs").writeText(encodeRs)
    srcDir.resolve("delta.rs").writeText(deltaRs)
    val binDir = srcDir.resolve("bin").also { it.createDirectories() }
    binDir.resolve("check_ast.rs").writeText(checkAstRs(crateName.replace('-', '_')))
    binDir.resolve("delta_oracle.rs").writeText(deltaOracleRs(crateName.replace('-', '_')))
    srcDir.resolve("ffi.rs").writeText(FFI_RS)
  }

  private fun cargoToml(mgroup3NativePath: String, crateName: String): String = """
    |[package]
    |name = "$crateName"
    |version = "0.1.0"
    |edition = "2021"
    |
    |[dependencies]
    |prost = "0.14"
    |bytes = "1"
    |# KernelSet = FxHashSet<Kernel>. 기본(self-contained) 빌드에도 필요.
    |# 순수 Rust crate 라 self-contained 를 깨지 않는다. feature 빌드에서는
    |# mgroup3-native 가 쓰는 것과 같은 crate 인스턴스여야 kernels_history 출력
    |# (Vec<FxHashSet<KtlibKernel>>) 타입과 일치하므로 semver 를 통일해 둔다.
    |rustc-hash = "2.1"
    |# check_ast 검증 bin 전용 (feature 로 격리 — 기본 빌드는 self-contained).
    |# 주의: path dependency 는 cargo 가 feature off 여도 manifest 해석 시 존재를
    |# 요구하므로, crate 를 리포 밖에 생성할 땐 GenCli 의 -mgroup3-native 로
    |# 올바른 경로를 넘겨야 한다.
    |mgroup3-native = { path = "$mgroup3NativePath", optional = true }
    |
    |[lib]
    |# cdylib: ffi feature 로 빌드 시 JVM(FFM) 등에서 로드할 동적 라이브러리.
    |crate-type = ["lib", "cdylib"]
    |
    |[features]
    |# end-to-end 검증: parserdata 로 실제 파싱 → kernels_history → 생성된
    |# AST walk(match_start) → to_short_string.
    |#   cargo run --features check-ast --bin check_ast -- <parserdata.pb> <input>...
    |check-ast = ["dep:mgroup3-native"]
    |# C-ABI export (mgroup3_gen_parse_ast 등) — cdylib 를 JVM FFM 으로 로드.
    |# mgroup3-native 의 mgroup3_parser_new* 심볼들도 함께 export 된다.
    |#   cargo build --features ffi
    |ffi = ["dep:mgroup3-native"]
    |
    |[[bin]]
    |name = "check_ast"
    |path = "src/bin/check_ast.rs"
    |required-features = ["check-ast"]
    |
    |# AST-delta differential oracle (Stage 2). Fuzzing edit sequences; every
    |# spliced edit is checked "delta-applied reconstruction == full walk+encode".
    |[[bin]]
    |name = "delta_oracle"
    |path = "src/bin/delta_oracle.rs"
    |required-features = ["check-ast"]
    |
    |[build-dependencies]
    |prost-build = "0.14"
    |
    |# 파서 dylib 는 한 번 빌드해 오래 쓰는 산물이라 빌드 시간보다 런타임 성능 우선.
    |# fat LTO + 단일 codegen unit 으로 crate 경계(생성 walk ↔ mgroup3-native) 넘는
    |# 인라이닝을 열어 파싱 hot path 를 최적화한다. (standalone crate 라 이 profile 이
    |# 의존 그래프 전체에 적용된다.)
    |[profile.release]
    |lto = "fat"
    |codegen-units = 1
    |""".trimMargin()

  private fun buildRs(): String = """
    |use std::io::Result;
    |
    |fn main() -> Result<()> {
    |    prost_build::compile_protos(&["proto/ast.proto"], &["proto"])?;
    |    Ok(())
    |}
    |""".trimMargin()

  /**
   * End-to-end 검증 bin: mgroup3-native 로 실제 파싱한 kernels_history 를
   * 생성된 AST walk 에 연결해 to_short_string 까지 완주하는지 확인한다.
   * (`cargo run --features check-ast --bin check_ast -- <parserdata.pb> <input>...`)
   * 파서·generator·walk 중 어느 한 곳의 좌표계가 어긋나면 여기서 panic 으로
   * 드러난다 — Phase B 디버깅의 1차 도구.
   */
  private fun checkAstRs(crateLibName: String): String = """
    |// Generated by GenCli (Stage4RustEmit) — do not edit by hand.
    |//
    |// Usage: check_ast <parserdata.pb> <input>...
    |//   (no <input> args: reads one input from stdin)
    |
    |use prost::Message;
    |
    |use $crateLibName::ast::Ctx;
    |use mgroup3_native::parser::Mgroup3Parser;
    |use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
    |
    |fn main() {
    |    let mut args = std::env::args().skip(1);
    |    let pd_path = args.next().expect("usage: check_ast <parserdata.pb> <input>...");
    |    let mut inputs: Vec<String> = args.collect();
    |    if inputs.is_empty() {
    |        use std::io::Read;
    |        let mut buf = String::new();
    |        std::io::stdin().read_to_string(&mut buf).expect("read stdin");
    |        inputs.push(buf);
    |    }
    |
    |    let bytes = std::fs::read(&pd_path).expect("read parserdata");
    |    let data = Mgroup3ParserData::decode(bytes.as_slice()).expect("decode parserdata");
    |    let parser = Mgroup3Parser::new(data);
    |
    |    let mut failed = 0;
    |    for input in &inputs {
    |        match run_one(&parser, input) {
    |            Ok(short) => println!("OK  {:?}\n => {}", input, short),
    |            Err(e) => {
    |                failed += 1;
    |                println!("ERR {:?}\n => {}", input, e);
    |            }
    |        }
    |    }
    |    if failed > 0 {
    |        std::process::exit(1);
    |    }
    |}
    |
    |fn run_one(parser: &Mgroup3Parser, input: &str) -> Result<String, String> {
    |    let ctx = parser.parse(input).map_err(|e| format!("parse error: {}", e))?;
    |    if !parser.is_accepted(&ctx) {
    |        return Err("not accepted".to_string());
    |    }
    |    // `kernels_history` returns `Vec<FxHashSet<KtlibKernel>>` = `Vec<KernelSet>`
    |    // in a feature build; hand it to the walk directly (no rebuild/downgrade).
    |    let history = parser.kernels_history(&ctx);
    |    let chars: Vec<char> = input.chars().collect();
    |    let result = std::panic::catch_unwind(move || {
    |        let mut walk_ctx = Ctx::new(&chars, &history);
    |        let ast = walk_ctx.match_start();
    |        ast.to_short_string()
    |    });
    |    result.map_err(|e| {
    |        let msg = e
    |            .downcast_ref::<String>()
    |            .cloned()
    |            .or_else(|| e.downcast_ref::<&str>().map(|s| s.to_string()))
    |            .unwrap_or_else(|| "panic".to_string());
    |        format!("walk panic: {}", msg)
    |    })
    |}
    |""".trimMargin()

  private fun libRs(schema: AstSchema): String = """
    |// Generated by GenCli — do not edit by hand.
    |
    |pub mod ktlib;
    |pub mod ast;
    |pub mod encode;
    |
    |#[cfg(feature = "ffi")]
    |pub mod ffi;
    |
    |// AST-delta walk (Stage 2). Borrows mgroup3-native's KernelsQuery, so it is
    |// only compiled when that dependency is present (ffi / check-ast features).
    |#[cfg(any(feature = "ffi", feature = "check-ast"))]
    |pub mod delta;
    |
    |pub mod proto {
    |    include!(concat!(env!("OUT_DIR"), "/${schema.packageName}.rs"));
    |}
    |""".trimMargin()

  /**
   * C-ABI: JVM(FFM) 등에서 cdylib 로 로드해 parse→walk→encode 를 한 번에.
   * parser handle 생성/해제는 mgroup3-native 의 export (`mgroup3_parser_new*`,
   * `mgroup3_parser_free`, `mgroup3_free_buffer`)를 그대로 사용한다 — rlib 의
   * #[no_mangle] 심볼들이 cdylib 에 함께 export 되므로 같은 dylib 에서 전부 찾을 수 있다.
   */
  private val FFI_RS: String = """
    |// Generated by GenCli (Stage4RustEmit) — do not edit by hand.
    |//
    |// C-ABI for the generated parser+walk. Load the cdylib (built with
    |// `cargo build --features ffi`) and use:
    |//   - mgroup3_parser_new / mgroup3_parser_new_from_file / mgroup3_parser_free
    |//     (re-exported from mgroup3-native)
    |//   - mgroup3_gen_parse_ast: parse + generated AST walk + encode to the
    |//     per-grammar ast.proto `ParseResult` bytes.
    |//   - mgroup3_gen_session_*: the INCREMENTAL variant of the above — a document
    |//     session (mgroup3-native `ParseSession`) that reuses a checkpoint ring /
    |//     splice across edits, emitting the SAME per-grammar ast.proto `ParseResult`
    |//     bytes after each `parse_full`/`edit`. This is what an editor/LSP consumes
    |//     per keystroke (mulang). Result bytes are byte-identical to
    |//     `mgroup3_gen_parse_ast` on the same final text (the session's
    |//     kernels_history is byte-identical to a full re-parse's — design §3.1).
    |//   - mgroup3_free_buffer (from mgroup3-native) to release the buffer.
    |
    |use std::panic::{catch_unwind, AssertUnwindSafe};
    |use std::ptr;
    |
    |use prost::Message;
    |
    |use mgroup3_native::parser::Mgroup3Parser;
    |use mgroup3_native::session::ParseSession;
    |
    |use crate::ast::Ctx;
    |use crate::encode;
    |
    |pub const MGROUP3_GEN_OK: i32 = 0;
    |pub const MGROUP3_GEN_ERR_NULL_ARG: i32 = 1;
    |pub const MGROUP3_GEN_ERR_UTF8: i32 = 4;
    |pub const MGROUP3_GEN_ERR_PANIC: i32 = 5;
    |pub const MGROUP3_GEN_ERR_PARSE: i32 = 6;
    |pub const MGROUP3_GEN_ERR_REJECTED: i32 = 7;
    |
    |/// Parse `input_bytes` (UTF-8), walk the generated AST, encode it as the
    |/// per-grammar `ParseResult` proto, and return the bytes via
    |/// `(*out_ptr, *out_len)`. Free with `mgroup3_free_buffer`.
    |///
    |/// Returns MGROUP3_GEN_OK on success; MGROUP3_GEN_ERR_PARSE when the input
    |/// is rejected mid-parse; MGROUP3_GEN_ERR_REJECTED when parsing completes
    |/// but the input is not accepted; MGROUP3_GEN_ERR_PANIC if the walk panics.
    |#[unsafe(no_mangle)]
    |pub extern "C" fn mgroup3_gen_parse_ast(
    |    parser: *mut Mgroup3Parser,
    |    input_bytes: *const u8,
    |    input_len: usize,
    |    out_ptr: *mut *mut u8,
    |    out_len: *mut usize,
    |) -> i32 {
    |    let result = catch_unwind(AssertUnwindSafe(|| {
    |        if parser.is_null() || out_ptr.is_null() || out_len.is_null() {
    |            return Err(MGROUP3_GEN_ERR_NULL_ARG);
    |        }
    |        let parser_ref = unsafe { &*parser };
    |        let slice = if input_len == 0 {
    |            &[][..]
    |        } else {
    |            if input_bytes.is_null() {
    |                return Err(MGROUP3_GEN_ERR_NULL_ARG);
    |            }
    |            unsafe { std::slice::from_raw_parts(input_bytes, input_len) }
    |        };
    |        let text = std::str::from_utf8(slice).map_err(|_| MGROUP3_GEN_ERR_UTF8)?;
    |
    |        let ctx = parser_ref.parse(text).map_err(|_| MGROUP3_GEN_ERR_PARSE)?;
    |        if !parser_ref.is_accepted(&ctx) {
    |            return Err(MGROUP3_GEN_ERR_REJECTED);
    |        }
    |        // `kernels_history` already returns `Vec<FxHashSet<KtlibKernel>>`,
    |        // which is exactly `Vec<KernelSet>` in a feature build (Kernel aliases
    |        // KtlibKernel, KernelSet = FxHashSet<Kernel>). Feed it to the walk
    |        // directly — no per-element rebuild, no hasher downgrade.
    |        let history = parser_ref.kernels_history(&ctx);
    |        let chars: Vec<char> = text.chars().collect();
    |        let mut walk_ctx = Ctx::new(&chars, &history);
    |        let ast = walk_ctx.match_start();
    |        let bytes = encode::encode(&ast).encode_to_vec();
    |
    |        let boxed = bytes.into_boxed_slice();
    |        let len = boxed.len();
    |        let raw = Box::into_raw(boxed) as *mut u8;
    |        unsafe {
    |            *out_ptr = raw;
    |            *out_len = len;
    |        }
    |        Ok(())
    |    }));
    |    let write_empty = |code: i32| {
    |        if !out_ptr.is_null() {
    |            unsafe {
    |                *out_ptr = ptr::null_mut();
    |            }
    |        }
    |        if !out_len.is_null() {
    |            unsafe {
    |                *out_len = 0;
    |            }
    |        }
    |        code
    |    };
    |    match result {
    |        Ok(Ok(())) => MGROUP3_GEN_OK,
    |        Ok(Err(code)) => write_empty(code),
    |        Err(_) => write_empty(MGROUP3_GEN_ERR_PANIC),
    |    }
    |}
    |
    |// ---------------------------------------------------------------------------
    |// Incremental session variant (Phase I3) — ADDITIVE. Same AST-proto output as
    |// mgroup3_gen_parse_ast, but over a mgroup3-native `ParseSession` that reuses a
    |// checkpoint ring / splice across edits. This is the per-keystroke consumer
    |// surface for an editor/LSP.
    |//
    |// Lifetime / threading (same as mgroup3-native's session FFI):
    |// - The session BORROWS the parser handle: one parser backs many document
    |//   sessions. The parser MUST outlive every session; destroy sessions before
    |//   `mgroup3_parser_free`.
    |// - A session is single-document: do not call session functions concurrently on
    |//   the SAME session handle. Distinct sessions are independent.
    |// - `pos_char`/`old_len_char` are CHAR (code point) offsets — the host converts
    |//   LSP UTF-16 positions to code points.
    |
    |/// Encode `session`'s current outcome as the per-grammar ast.proto `ParseResult`
    |/// bytes: kernels_history -> generated AST walk -> encode. Mirrors the body of
    |/// `mgroup3_gen_parse_ast`, driven from the session's stored outcome + document.
    |fn gen_session_emit(
    |    session: &ParseSession,
    |    out_ptr: *mut *mut u8,
    |    out_len: *mut usize,
    |) -> Result<(), i32> {
    |    // A parse error (session parsed to an error) maps to PARSE; a clean parse
    |    // that isn't accepted maps to REJECTED — same code contract as the one-shot
    |    // `mgroup3_gen_parse_ast`.
    |    if session.error().is_some() {
    |        return Err(MGROUP3_GEN_ERR_PARSE);
    |    }
    |    if !session.is_accepted() {
    |        return Err(MGROUP3_GEN_ERR_REJECTED);
    |    }
    |    // `kernels_history()` returns `Vec<FxHashSet<KtlibKernel>>` = `Vec<KernelSet>`
    |    // in a feature build — feed it to the walk directly (no rebuild/downgrade).
    |    let history = session.kernels_history().ok_or(MGROUP3_GEN_ERR_REJECTED)?;
    |    let chars = session.document();
    |    let mut walk_ctx = Ctx::new(chars, &history);
    |    let ast = walk_ctx.match_start();
    |    let bytes = encode::encode(&ast).encode_to_vec();
    |    let boxed = bytes.into_boxed_slice();
    |    let len = boxed.len();
    |    let raw = Box::into_raw(boxed) as *mut u8;
    |    unsafe {
    |        *out_ptr = raw;
    |        *out_len = len;
    |    }
    |    Ok(())
    |}
    |
    |fn gen_session_finish(
    |    result: Result<Result<(), i32>, Box<dyn std::any::Any + Send>>,
    |    out_ptr: *mut *mut u8,
    |    out_len: *mut usize,
    |) -> i32 {
    |    let clear = || {
    |        if !out_ptr.is_null() {
    |            unsafe { *out_ptr = ptr::null_mut() };
    |        }
    |        if !out_len.is_null() {
    |            unsafe { *out_len = 0 };
    |        }
    |    };
    |    match result {
    |        Ok(Ok(())) => MGROUP3_GEN_OK,
    |        Ok(Err(code)) => {
    |            clear();
    |            code
    |        }
    |        Err(_) => {
    |            clear();
    |            MGROUP3_GEN_ERR_PANIC
    |        }
    |    }
    |}
    |
    |/// Create a session over an existing parser handle (BORROWED — see lifetime
    |/// note above). Returns a non-null session handle + writes MGROUP3_GEN_OK to
    |/// `*err` on success; null + nonzero on null-arg/panic. Free with
    |/// `mgroup3_gen_session_destroy`.
    |#[unsafe(no_mangle)]
    |pub extern "C" fn mgroup3_gen_session_new(
    |    parser: *mut Mgroup3Parser,
    |    err: *mut i32,
    |) -> *mut ParseSession {
    |    let result = catch_unwind(AssertUnwindSafe(|| {
    |        if parser.is_null() {
    |            return Err(MGROUP3_GEN_ERR_NULL_ARG);
    |        }
    |        // SAFETY: non-null here; caller upholds parser-outlives-session.
    |        let session = unsafe {
    |            ParseSession::from_raw_parser(
    |                parser as *const Mgroup3Parser,
    |                mgroup3_native::session::DEFAULT_CHECKPOINT_INTERVAL,
    |            )
    |        };
    |        Ok(session)
    |    }));
    |    match result {
    |        Ok(Ok(session)) => {
    |            if !err.is_null() {
    |                unsafe { *err = MGROUP3_GEN_OK };
    |            }
    |            Box::into_raw(Box::new(session))
    |        }
    |        Ok(Err(code)) => {
    |            if !err.is_null() {
    |                unsafe { *err = code };
    |            }
    |            ptr::null_mut()
    |        }
    |        Err(_) => {
    |            if !err.is_null() {
    |                unsafe { *err = MGROUP3_GEN_ERR_PANIC };
    |            }
    |            ptr::null_mut()
    |        }
    |    }
    |}
    |
    |/// Full (re)parse of `input_bytes` (UTF-8) from gen 0, then emit the AST proto
    |/// bytes. Same result as `mgroup3_gen_parse_ast` on the same text. Error codes:
    |/// 6=PARSE (rejected mid-parse), 7=REJECTED (parsed but not accepted), 4=UTF8,
    |/// 5=PANIC, 1=NULL_ARG.
    |#[unsafe(no_mangle)]
    |pub extern "C" fn mgroup3_gen_session_parse_full(
    |    session: *mut ParseSession,
    |    input_bytes: *const u8,
    |    input_len: usize,
    |    out_ptr: *mut *mut u8,
    |    out_len: *mut usize,
    |) -> i32 {
    |    let result = catch_unwind(AssertUnwindSafe(|| {
    |        if session.is_null() || out_ptr.is_null() || out_len.is_null() {
    |            return Err(MGROUP3_GEN_ERR_NULL_ARG);
    |        }
    |        let slice = if input_len == 0 {
    |            &[][..]
    |        } else {
    |            if input_bytes.is_null() {
    |                return Err(MGROUP3_GEN_ERR_NULL_ARG);
    |            }
    |            unsafe { std::slice::from_raw_parts(input_bytes, input_len) }
    |        };
    |        let text = std::str::from_utf8(slice).map_err(|_| MGROUP3_GEN_ERR_UTF8)?;
    |        let session_ref = unsafe { &mut *session };
    |        session_ref.parse_full(text);
    |        gen_session_emit(session_ref, out_ptr, out_len)
    |    }));
    |    gen_session_finish(result, out_ptr, out_len)
    |}
    |
    |/// Apply an edit and re-parse incrementally, then emit the AST proto bytes.
    |/// `pos_char`/`old_len_char` are CHAR offsets; `new_bytes` (UTF-8) is the
    |/// replacement. Result is byte-identical to a full re-parse of the edited text.
    |/// On a parse error the session stays usable (the next edit re-parses safely).
    |#[unsafe(no_mangle)]
    |pub extern "C" fn mgroup3_gen_session_edit(
    |    session: *mut ParseSession,
    |    pos_char: usize,
    |    old_len_char: usize,
    |    new_bytes: *const u8,
    |    new_len: usize,
    |    out_ptr: *mut *mut u8,
    |    out_len: *mut usize,
    |) -> i32 {
    |    let result = catch_unwind(AssertUnwindSafe(|| {
    |        if session.is_null() || out_ptr.is_null() || out_len.is_null() {
    |            return Err(MGROUP3_GEN_ERR_NULL_ARG);
    |        }
    |        let slice = if new_len == 0 {
    |            &[][..]
    |        } else {
    |            if new_bytes.is_null() {
    |                return Err(MGROUP3_GEN_ERR_NULL_ARG);
    |            }
    |            unsafe { std::slice::from_raw_parts(new_bytes, new_len) }
    |        };
    |        let new_text = std::str::from_utf8(slice).map_err(|_| MGROUP3_GEN_ERR_UTF8)?;
    |        let session_ref = unsafe { &mut *session };
    |        session_ref.edit(pos_char, old_len_char, new_text);
    |        gen_session_emit(session_ref, out_ptr, out_len)
    |    }));
    |    gen_session_finish(result, out_ptr, out_len)
    |}
    |
    |/// Drop a session handle. Does NOT free the borrowed parser.
    |#[unsafe(no_mangle)]
    |pub extern "C" fn mgroup3_gen_session_destroy(session: *mut ParseSession) {
    |    if session.is_null() {
    |        return;
    |    }
    |    let _ = catch_unwind(AssertUnwindSafe(|| unsafe {
    |        drop(Box::from_raw(session));
    |    }));
    |}
    |""".trimMargin()

  /**
   * Grammar-INDEPENDENT half of `delta.rs` (AST-delta Stage 2, design §6). The
   * grammar-specific half (NodeEntry dispatch, per-nonterminal delta fns, canon,
   * walk_delta) is appended by [[RustOptCodeGen.generateDelta]]. Raw string (no
   * trimMargin) so the Rust closures `|x| ..` survive verbatim. References the
   * emitted `tag_of`/`span_of`/`shift_span`/`child_ids_of`/`walk_delta` (same
   * module). Only compiled with mgroup3-native present (ffi/check-ast).
   */
  private val DELTA_RT_RS: String = """// Generated by Stage4RustEmit (DELTA_RT_RS) + RustOptCodeGen.generateDelta.
// AST-delta walk (Stage 2). See mgroup3/docs/lsp_result_boundary.md §6.
#![cfg(any(feature = "ffi", feature = "check-ast"))]
#![allow(dead_code, unused_variables)]

use std::cell::OnceCell;

use rustc_hash::{FxHashMap, FxHashSet};

use mgroup3_native::parser::KernelsQuery;

use crate::ktlib::{Kernel, KernelSet, KernelSetExt, GenSpan, has_single_true};
use crate::proto;

// Lazy per-gen kernel source: `KernelsQuery::at(g)` recomputes one gen on demand;
// the delta walk only touches spine + dirty-window gens, so materializing the
// whole kernels_history (the O(n) cost we avoid) is replaced by a sparse cache.
pub struct LazyHistory<'a> {
    query: KernelsQuery<'a>,
    cells: Vec<OnceCell<KernelSet>>,
}

impl<'a> LazyHistory<'a> {
    pub fn new(query: KernelsQuery<'a>) -> Self {
        let n = query.num_gens();
        let mut cells = Vec::with_capacity(n);
        cells.resize_with(n, OnceCell::new);
        Self { query, cells }
    }

    #[inline]
    pub fn at(&self, gen: usize) -> &KernelSet {
        self.cells[gen].get_or_init(|| self.query.at(gen))
    }
}

fn get_sequence_elems_lazy(
    hist: &LazyHistory,
    sequence_id: i32,
    elems: &[i32],
    begin_gen: i32,
    end_gen: i32,
) -> Vec<GenSpan> {
    let n = elems.len();
    let last_elem = hist.at(end_gen as usize).find_by_begin_gen(sequence_id, n as i32, begin_gen);
    let mut list = vec![last_elem];
    let mut curr_gen = last_elem.end_gen;
    for pointer in (0..n).rev() {
        let candidates = hist
            .at(curr_gen as usize)
            .filter_by_begin_gen(sequence_id, pointer as i32, begin_gen);
        let curr_gen_snapshot = curr_gen;
        let filtered: Vec<Kernel> = candidates
            .into_iter()
            .filter(|prev| {
                hist.at(curr_gen_snapshot as usize).contains(&Kernel {
                    symbol_id: elems[pointer],
                    pointer: 1,
                    begin_gen: prev.end_gen,
                    end_gen: curr_gen_snapshot,
                })
            })
            .collect();
        assert_eq!(filtered.len(), 1, "sequence elem not single");
        let prev_elem = filtered[0];
        list.push(prev_elem);
        curr_gen = prev_elem.end_gen;
    }
    (0..n).map(|i| (list[n - i].end_gen, list[n - i - 1].end_gen)).collect()
}

fn unroll_repeat0_lazy(
    hist: &LazyHistory,
    symbol_id: i32,
    item_sym_id: i32,
    base_seq: i32,
    repeat_seq: i32,
    begin_gen: i32,
    end_gen: i32,
) -> Vec<GenSpan> {
    let mut acc: Vec<GenSpan> = Vec::new();
    let mut bg = begin_gen;
    let mut eg = end_gen;
    loop {
        let base = hist.at(eg as usize).find_by_begin_gen_opt(base_seq, 0, bg);
        let repeat = hist.at(eg as usize).find_by_begin_gen_opt(repeat_seq, 2, bg);
        assert!(has_single_true(&[base.is_some(), repeat.is_some()]));
        if base.is_some() {
            return acc;
        }
        let seq = get_sequence_elems_lazy(hist, repeat_seq, &[symbol_id, item_sym_id], bg, eg);
        let repeating = seq[0];
        let item = seq[1];
        acc.insert(0, item);
        bg = repeating.0;
        eg = repeating.1;
    }
}

fn unroll_repeat1_lazy(
    hist: &LazyHistory,
    symbol_id: i32,
    item_sym_id: i32,
    base_seq: i32,
    repeat_seq: i32,
    begin_gen: i32,
    end_gen: i32,
) -> Vec<GenSpan> {
    let mut acc: Vec<GenSpan> = Vec::new();
    let mut bg = begin_gen;
    let mut eg = end_gen;
    loop {
        let base = hist.at(eg as usize).find_by_begin_gen_opt(base_seq, 1, bg);
        let repeat = hist.at(eg as usize).find_by_begin_gen_opt(repeat_seq, 2, bg);
        assert!(has_single_true(&[base.is_some(), repeat.is_some()]));
        if base.is_some() {
            let base_item = hist.at(eg as usize).find_by_begin_gen(item_sym_id, 1, bg);
            acc.insert(0, (base_item.begin_gen, base_item.end_gen));
            return acc;
        }
        let seq = get_sequence_elems_lazy(hist, repeat_seq, &[symbol_id, item_sym_id], bg, eg);
        let repeating = seq[0];
        let item = seq[1];
        acc.insert(0, item);
        bg = repeating.0;
        eg = repeating.1;
    }
}

// Delta walk context. `try_reuse` references the emitted `tag_of`/`span_of`.
pub struct DeltaCtx<'a> {
    source_chars: &'a [char],
    hist: LazyHistory<'a>,
    dirty_lo: i32,
    dirty_hi: i32,
    delta: i32,
    old_nodes: &'a [proto::NodeEntry],
    by_id: &'a FxHashMap<i32, usize>,
    patched: Vec<proto::NodeEntry>,
    kept: FxHashSet<i32>,
    next_id: i32,
}

impl<'a> DeltaCtx<'a> {
    #[inline]
    fn alloc(&mut self) -> i32 {
        let id = self.next_id;
        self.next_id += 1;
        id
    }

    fn old_entry(&self, id: Option<i32>) -> Option<&proto::NodeEntry> {
        let id = id?;
        let idx = *self.by_id.get(&id)?;
        self.old_nodes.get(idx)
    }

    /// Reuse decision (span outside dirty window) + safety verification (the
    /// candidate old node's tag + old-coord span match). The verification makes
    /// reuse sound regardless of how the candidate was located; a mismatch falls
    /// back to a full rebuild.
    fn try_reuse(&mut self, tag: i32, b: i32, e: i32, old: Option<i32>) -> Option<i32> {
        let old_id = old?;
        let (oldb, olde) = if e < self.dirty_lo {
            (b, e) // VERBATIM — new coords == old coords
        } else if b > self.dirty_hi {
            (b - self.delta, e - self.delta) // SHIFT — old coords = new - delta
        } else {
            return None; // dirty window — must rebuild
        };
        let entry = self.old_entry(Some(old_id))?;
        if tag_of(entry) != tag {
            return None;
        }
        let (sb, se) = span_of(entry);
        if sb != oldb || se != olde {
            return None;
        }
        self.kept.insert(old_id);
        Some(old_id)
    }
}

/// Align a new repeated Msg child list against the old counterpart list and
/// recurse. Front (verbatim) aligns to old front by index; back (shift) to old
/// back by index-from-end; the middle aligns 1:1 only when counts match (else no
/// counterpart — a full rebuild). Every recursion re-verifies via try_reuse.
fn delta_list<F: FnMut(&mut DeltaCtx, i32, i32, Option<i32>) -> i32>(
    ctx: &mut DeltaCtx,
    coords: &[(i32, i32)],
    old_ids: &[i32],
    mut recurse: F,
) -> Vec<i32> {
    let k = coords.len();
    let m = old_ids.len();
    let mut fv = 0usize;
    while fv < k && coords[fv].1 < ctx.dirty_lo {
        fv += 1;
    }
    let mut bs = 0usize;
    while bs < k - fv && coords[k - 1 - bs].0 > ctx.dirty_hi {
        bs += 1;
    }
    let same_middle = k == m;
    let mut out = Vec::with_capacity(k);
    for (i, &(b, e)) in coords.iter().enumerate() {
        let old = if i < fv {
            old_ids.get(i).copied()
        } else if i >= k - bs {
            let from_end = k - 1 - i;
            m.checked_sub(1 + from_end).and_then(|idx| old_ids.get(idx).copied())
        } else if same_middle {
            old_ids.get(i).copied()
        } else {
            None
        };
        out.push(recurse(ctx, b, e, old));
    }
    out
}

/// The per-edit reuse boundary (the fields the delta walk needs from EditReuse).
#[derive(Clone, Copy, Debug)]
pub struct ReuseInfo {
    pub dirty_lo: i32,
    pub dirty_hi: i32,
    pub pivot: i32,
    pub delta: i32,
}

pub struct DeltaResult {
    pub patched: Vec<proto::NodeEntry>,
    pub freed: Vec<i32>,
    pub root_id: i32,
    pub shift_pivot: i32,
    pub shift_delta: i32,
}

/// DFS the OLD table from `old_root`, collecting ids NOT reused (the old dirty
/// subtree). Reused nodes prune the descent. O(dirty subtree). Uses the emitted
/// `child_ids_of`.
fn dfs_free(
    old_root: i32,
    kept: &FxHashSet<i32>,
    by_id: &FxHashMap<i32, usize>,
    old_nodes: &[proto::NodeEntry],
    out: &mut Vec<i32>,
) {
    let mut stack = vec![old_root];
    while let Some(id) = stack.pop() {
        if kept.contains(&id) {
            continue;
        }
        let Some(&idx) = by_id.get(&id) else { continue };
        let entry = &old_nodes[idx];
        out.push(id);
        child_ids_of(entry, &mut stack);
    }
}

// Session-lifetime delta state + reconstruction. The stored table is kept in
// current-document coordinates: after a delta it is reconstructed = (retained
// old nodes with `> pivot` coords shifted) ++ patched.
pub struct PrevResult {
    pub nodes: Vec<proto::NodeEntry>,
    pub by_id: FxHashMap<i32, usize>,
    pub root_id: i32,
    pub max_id: i32,
}

impl PrevResult {
    pub fn from_result(r: proto::ParseResult) -> Self {
        Self::new(r.nodes, r.root_id)
    }

    pub fn new(nodes: Vec<proto::NodeEntry>, root_id: i32) -> Self {
        let mut by_id = FxHashMap::default();
        let mut max_id = 0;
        for (i, n) in nodes.iter().enumerate() {
            by_id.insert(n.id, i);
            max_id = max_id.max(n.id);
        }
        PrevResult { nodes, by_id, root_id, max_id }
    }
}

/// Apply a `DeltaResult` to the previous table, yielding the new full table (the
/// next splice baseline). O(prev nodes) span shifts + clones. Uses the emitted
/// `shift_span`.
pub fn reconstruct(prev: &PrevResult, d: &DeltaResult) -> PrevResult {
    let freed: FxHashSet<i32> = d.freed.iter().copied().collect();
    let mut nodes: Vec<proto::NodeEntry> = Vec::with_capacity(prev.nodes.len() + d.patched.len());
    for n in &prev.nodes {
        if freed.contains(&n.id) {
            continue;
        }
        let mut n2 = n.clone();
        shift_span(&mut n2, d.shift_pivot, d.shift_delta);
        nodes.push(n2);
    }
    nodes.extend(d.patched.iter().cloned());
    PrevResult::new(nodes, d.root_id)
}

// FFI: an incremental session that emits a ParseDelta when it can, else a full
// ParseResult. ADDITIVE — the existing mgroup3_gen_session_* stay unchanged.
#[cfg(feature = "ffi")]
mod ffi_delta {
    use super::*;
    use std::panic::{catch_unwind, AssertUnwindSafe};
    use std::ptr;

    use mgroup3_native::parser::Mgroup3Parser;
    use mgroup3_native::session::{ParseOutcome, ParseSession};
    use prost::Message;

    pub const MGROUP3_GEN_OK: i32 = 0;
    pub const MGROUP3_GEN_ERR_NULL_ARG: i32 = 1;
    pub const MGROUP3_GEN_ERR_UTF8: i32 = 4;
    pub const MGROUP3_GEN_ERR_PANIC: i32 = 5;
    pub const MGROUP3_GEN_ERR_PARSE: i32 = 6;
    pub const MGROUP3_GEN_ERR_REJECTED: i32 = 7;

    /// A delta-aware session: a `ParseSession` plus the previous full result and a
    /// monotone version counter.
    pub struct GenDeltaSession {
        session: ParseSession,
        prev: Option<PrevResult>,
        version: i32,
    }

    /// Full walk+encode of the session's accepted outcome (fallback / baseline).
    fn full_result(session: &ParseSession) -> Option<proto::ParseResult> {
        let history = session.kernels_history()?;
        let chars = session.document();
        let mut ctx = crate::ast::Ctx::new(chars, &history);
        let ast = ctx.match_start();
        Some(crate::encode::encode(&ast))
    }

    fn write_out(bytes: Vec<u8>, out_ptr: *mut *mut u8, out_len: *mut usize) {
        let boxed = bytes.into_boxed_slice();
        let len = boxed.len();
        let raw = Box::into_raw(boxed) as *mut u8;
        unsafe {
            *out_ptr = raw;
            *out_len = len;
        }
    }

    /// Create a delta session over a borrowed parser handle (the parser must
    /// outlive the session). `*err` = MGROUP3_GEN_OK on success.
    #[unsafe(no_mangle)]
    pub extern "C" fn mgroup3_gen_delta_session_new(
        parser: *mut Mgroup3Parser,
        err: *mut i32,
    ) -> *mut GenDeltaSession {
        let result = catch_unwind(AssertUnwindSafe(|| {
            if parser.is_null() {
                return Err(MGROUP3_GEN_ERR_NULL_ARG);
            }
            let session = unsafe {
                ParseSession::from_raw_parser(
                    parser as *const Mgroup3Parser,
                    mgroup3_native::session::DEFAULT_CHECKPOINT_INTERVAL,
                )
            };
            Ok(GenDeltaSession { session, prev: None, version: 0 })
        }));
        match result {
            Ok(Ok(s)) => {
                if !err.is_null() {
                    unsafe { *err = MGROUP3_GEN_OK };
                }
                Box::into_raw(Box::new(s))
            }
            Ok(Err(code)) => {
                if !err.is_null() {
                    unsafe { *err = code };
                }
                ptr::null_mut()
            }
            Err(_) => {
                if !err.is_null() {
                    unsafe { *err = MGROUP3_GEN_ERR_PANIC };
                }
                ptr::null_mut()
            }
        }
    }

    /// Full (re)parse; emits a full `ParseResult` and (re)establishes the baseline.
    #[unsafe(no_mangle)]
    pub extern "C" fn mgroup3_gen_delta_session_parse_full(
        session: *mut GenDeltaSession,
        input_bytes: *const u8,
        input_len: usize,
        out_ptr: *mut *mut u8,
        out_len: *mut usize,
    ) -> i32 {
        let result = catch_unwind(AssertUnwindSafe(|| {
            if session.is_null() || out_ptr.is_null() || out_len.is_null() {
                return Err(MGROUP3_GEN_ERR_NULL_ARG);
            }
            let slice = if input_len == 0 {
                &[][..]
            } else {
                if input_bytes.is_null() {
                    return Err(MGROUP3_GEN_ERR_NULL_ARG);
                }
                unsafe { std::slice::from_raw_parts(input_bytes, input_len) }
            };
            let text = std::str::from_utf8(slice).map_err(|_| MGROUP3_GEN_ERR_UTF8)?;
            let s = unsafe { &mut *session };
            s.session.parse_full(text);
            if s.session.error().is_some() {
                return Err(MGROUP3_GEN_ERR_PARSE);
            }
            if !s.session.is_accepted() {
                return Err(MGROUP3_GEN_ERR_REJECTED);
            }
            let full = full_result(&s.session).ok_or(MGROUP3_GEN_ERR_REJECTED)?;
            s.prev = Some(PrevResult::from_result(full.clone()));
            s.version += 1;
            write_out(full.encode_to_vec(), out_ptr, out_len);
            Ok(())
        }));
        finish(result, out_ptr, out_len)
    }

    /// Apply an edit and emit a `ParseDelta` (when the edit spliced and a baseline
    /// is held) or a full `ParseResult` (fallback). `*kind` = 1 for delta, 0 for
    /// full (only meaningful when the return code is OK). Fallback rule (§6):
    /// non-splice / first edit / no baseline -> full.
    #[unsafe(no_mangle)]
    pub extern "C" fn mgroup3_gen_session_edit_delta(
        session: *mut GenDeltaSession,
        pos_char: usize,
        old_len_char: usize,
        new_bytes: *const u8,
        new_len: usize,
        out_ptr: *mut *mut u8,
        out_len: *mut usize,
        kind: *mut i32,
    ) -> i32 {
        let result = catch_unwind(AssertUnwindSafe(|| {
            if session.is_null() || out_ptr.is_null() || out_len.is_null() {
                return Err(MGROUP3_GEN_ERR_NULL_ARG);
            }
            let slice = if new_len == 0 {
                &[][..]
            } else {
                if new_bytes.is_null() {
                    return Err(MGROUP3_GEN_ERR_NULL_ARG);
                }
                unsafe { std::slice::from_raw_parts(new_bytes, new_len) }
            };
            let text = std::str::from_utf8(slice).map_err(|_| MGROUP3_GEN_ERR_UTF8)?;
            let s = unsafe { &mut *session };
            s.session.edit(pos_char, old_len_char, text);
            if s.session.error().is_some() {
                return Err(MGROUP3_GEN_ERR_PARSE);
            }
            if !s.session.is_accepted() {
                return Err(MGROUP3_GEN_ERR_REJECTED);
            }

            let reuse = s.session.edit_reuse();
            let can_delta = reuse.map(|r| r.spliced).unwrap_or(false) && s.prev.is_some();

            if can_delta {
                let r = reuse.unwrap();
                let prev = s.prev.as_ref().unwrap();
                let ctx = match s.session.outcome() {
                    Some(ParseOutcome::Ok(c)) => c,
                    _ => return Err(MGROUP3_GEN_ERR_REJECTED),
                };
                let query = s.session.parser().kernels_query(ctx);
                let d = walk_delta(
                    s.session.document(),
                    query,
                    ReuseInfo {
                        dirty_lo: r.dirty_lo as i32,
                        dirty_hi: r.dirty_hi as i32,
                        pivot: r.pivot,
                        delta: r.delta,
                    },
                    &prev.nodes,
                    &prev.by_id,
                    prev.root_id,
                    prev.max_id + 1,
                );
                let base_version = s.version;
                s.version += 1;
                let new_prev = reconstruct(prev, &d);
                let msg = proto::ParseDelta {
                    base_version,
                    new_version: s.version,
                    root_id: d.root_id,
                    shift_pivot: d.shift_pivot,
                    shift_delta: d.shift_delta,
                    patched: d.patched,
                    freed_ids: d.freed,
                };
                s.prev = Some(new_prev);
                if !kind.is_null() {
                    unsafe { *kind = 1 };
                }
                write_out(msg.encode_to_vec(), out_ptr, out_len);
            } else {
                let full = full_result(&s.session).ok_or(MGROUP3_GEN_ERR_REJECTED)?;
                s.prev = Some(PrevResult::from_result(full.clone()));
                s.version += 1;
                if !kind.is_null() {
                    unsafe { *kind = 0 };
                }
                write_out(full.encode_to_vec(), out_ptr, out_len);
            }
            Ok(())
        }));
        finish(result, out_ptr, out_len)
    }

    /// Drop a delta session (does NOT free the borrowed parser).
    #[unsafe(no_mangle)]
    pub extern "C" fn mgroup3_gen_delta_session_destroy(session: *mut GenDeltaSession) {
        if session.is_null() {
            return;
        }
        let _ = catch_unwind(AssertUnwindSafe(|| unsafe {
            drop(Box::from_raw(session));
        }));
    }

    fn finish(
        result: Result<Result<(), i32>, Box<dyn std::any::Any + Send>>,
        out_ptr: *mut *mut u8,
        out_len: *mut usize,
    ) -> i32 {
        let clear = || {
            if !out_ptr.is_null() {
                unsafe { *out_ptr = ptr::null_mut() };
            }
            if !out_len.is_null() {
                unsafe { *out_len = 0 };
            }
        };
        match result {
            Ok(Ok(())) => MGROUP3_GEN_OK,
            Ok(Err(code)) => {
                clear();
                code
            }
            Err(_) => {
                clear();
                MGROUP3_GEN_ERR_PANIC
            }
        }
    }
}
"""

  /**
   * Generic AST-delta differential oracle bin (grammar-agnostic; uses the emitted
   * `delta::canon` / `delta::walk_delta` / `delta::reconstruct`). Fuzzing edit
   * sequences over synthetic + supplied docs; each spliced edit is checked
   * "delta-applied reconstruction == full walk+encode". Fallback paths (reject /
   * resync / non-splice) are exercised too. Raw string (Rust closures survive);
   * `$crateLibName` is the only interpolation.
   */
  private fun deltaOracleRs(crateLibName: String): String = """// Generated by Stage4RustEmit (delta_oracle) — do not edit by hand.
// Usage: delta_oracle <parserdata.pb> [inputfile ...]   (no files: synthetic docs)

use prost::Message;

use ${crateLibName}::delta::{canon, reconstruct, walk_delta, PrevResult, ReuseInfo};
use ${crateLibName}::proto;
use mgroup3_native::parser::Mgroup3Parser;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use mgroup3_native::session::{ParseOutcome, ParseSession};
use std::sync::Arc;

fn full_encode(session: &ParseSession) -> Option<proto::ParseResult> {
    let history = session.kernels_history()?;
    let chars = session.document();
    let mut ctx = ${crateLibName}::ast::Ctx::new(chars, &history);
    let ast = ctx.match_start();
    Some(${crateLibName}::encode::encode(&ast))
}

struct Rng(u64);
impl Rng {
    fn new(seed: u64) -> Self { Rng(seed | 1) }
    fn next_u64(&mut self) -> u64 {
        let mut x = self.0;
        x ^= x >> 12; x ^= x << 25; x ^= x >> 27;
        self.0 = x;
        x.wrapping_mul(0x2545F4914F6CDD1D)
    }
    fn below(&mut self, n: usize) -> usize { if n == 0 { 0 } else { (self.next_u64() % n as u64) as usize } }
}

fn apply(doc: &mut Vec<char>, pos: usize, old_len: usize, new_text: &str) {
    let pos = pos.min(doc.len());
    let end = (pos + old_len).min(doc.len());
    let ins: Vec<char> = new_text.chars().collect();
    doc.splice(pos..end, ins);
}

fn is_ident_char(c: char) -> bool { c.is_alphanumeric() || c == '_' }

fn find_ident_span(chars: &[char], from: usize) -> Option<(usize, usize)> {
    let n = chars.len();
    let mut i = from.min(n.saturating_sub(1));
    while i < n && !is_ident_char(chars[i]) { i += 1; }
    if i >= n { return None; }
    let mut s = i;
    while s > 0 && is_ident_char(chars[s - 1]) { s -= 1; }
    let mut e = i;
    while e < n && is_ident_char(chars[e]) { e += 1; }
    if chars[s].is_alphabetic() || chars[s] == '_' { Some((s, e)) } else { None }
}

#[derive(Default)]
struct Stats {
    edits: usize,
    verified: usize,
    fallback: usize,
    rejected: usize,
    total_patched: usize,
    total_freed: usize,
    total_full: usize,
    wholesale: usize,
}

fn run_doc(parser: &Arc<Mgroup3Parser>, label: &str, base: &str, edits: usize, seed: u64, st: &mut Stats) {
    let mut session = ParseSession::new(Arc::clone(parser));
    session.parse_full(base);
    if !matches!(session.outcome(), Some(ParseOutcome::Ok(c)) if parser.is_accepted(c)) {
        eprintln!("[delta_oracle] {label}: baseline not accepted, skipping");
        return;
    }
    let mut current = PrevResult::from_result(full_encode(&session).expect("baseline"));
    let mut resync = false;
    let mut doc: Vec<char> = base.chars().collect();
    let mut rng = Rng::new(seed);
    let mut pending_undo: Option<(usize, usize)> = None;
    let fracs = [(1usize, 6usize), (1, 3), (1, 2), (2, 3), (5, 6), (1, 4), (3, 4), (2, 5)];

    for i in 0..edits {
        let n = doc.len();
        let (pos, old_len, new_text): (usize, usize, String) = if let Some((p, l)) = pending_undo.take() {
            (p, l, String::new())
        } else if i % 6 == 5 {
            let p = (n / 2).min(n);
            pending_undo = Some((p, 3));
            (p, 0, "@@@".to_string())
        } else if i % 3 == 1 {
            // insert a letter inside an identifier (validity-preserving).
            match find_ident_span(&doc, (n * (1 + rng.below(4))) / 5) {
                Some((s, e)) => ((s + 1).min(e), 0, "q".to_string()),
                None => (n / 2, 0, "q".to_string()),
            }
        } else {
            // substitute an identifier with a longer one.
            let (num, den) = fracs[i % fracs.len()];
            match find_ident_span(&doc, n * num / den) {
                Some((s, e)) => {
                    let old: String = doc[s..e].iter().collect();
                    (s, e - s, format!("q{}z", old))
                }
                None => (n / 2, 0, "q".to_string()),
            }
        };

        apply(&mut doc, pos, old_len, &new_text);
        session.edit(pos, old_len, &new_text);
        st.edits += 1;

        let accepted = matches!(session.outcome(), Some(ParseOutcome::Ok(c)) if parser.is_accepted(c));
        if !accepted {
            st.fallback += 1;
            st.rejected += 1;
            resync = true;
            continue;
        }
        let reuse = session.edit_reuse();
        let full_table = PrevResult::from_result(full_encode(&session).expect("full"));
        let spliced = reuse.map(|r| r.spliced).unwrap_or(false);
        if resync || !spliced {
            st.fallback += 1;
            current = full_table;
            resync = false;
            continue;
        }
        let r = reuse.unwrap();
        let ctx = match session.outcome() { Some(ParseOutcome::Ok(c)) => c, _ => unreachable!() };
        let query = parser.kernels_query(ctx);
        let d = walk_delta(
            session.document(),
            query,
            ReuseInfo { dirty_lo: r.dirty_lo as i32, dirty_hi: r.dirty_hi as i32, pivot: r.pivot, delta: r.delta },
            &current.nodes,
            &current.by_id,
            current.root_id,
            current.max_id + 1,
        );
        let reconstructed = reconstruct(&current, &d);
        let got = canon(&reconstructed.nodes, &reconstructed.by_id, reconstructed.root_id);
        let want = canon(&full_table.nodes, &full_table.by_id, full_table.root_id);
        if got != want {
            let fd = got.char_indices().zip(want.char_indices())
                .find(|((_, a), (_, b))| a != b).map(|((i, _), _)| i)
                .unwrap_or(got.len().min(want.len()));
            let lo = fd.saturating_sub(60);
            panic!(
                "=== DELTA ORACLE COUNTEREXAMPLE ===\n{label} edit#{i} pos={pos} old_len={old_len} new={new_text:?}\n\
                 reuse={r:?} patched={} freed={} root={}\n first diff @ {fd}\n  got : ...{}...\n  want: ...{}...\n doc_head={:?}",
                d.patched.len(), d.freed.len(), d.root_id,
                &got[lo..(fd + 60).min(got.len())], &want[lo..(fd + 60).min(want.len())],
                doc.iter().take(160).collect::<String>(),
            );
        }
        for p in &d.patched {
            assert!(p.id > current.max_id, "{label} edit#{i}: patched id {} not fresh (max_id {})", p.id, current.max_id);
        }
        for &f in &d.freed {
            assert!(current.by_id.contains_key(&f), "{label} edit#{i}: freed id {f} not in old table");
        }
        st.total_patched += d.patched.len();
        st.total_freed += d.freed.len();
        st.total_full += full_table.nodes.len();
        if d.patched.len() >= full_table.nodes.len() { st.wholesale += 1; }
        st.verified += 1;
        current = reconstructed;
    }
}

fn synthetic_docs() -> Vec<(String, String)> {
    vec![
        ("small".to_string(), "module M { foo = Bar(int x, str y) | Baz\n  attributes (int z) }".to_string()),
        ("commented".to_string(), "module M {\n  -- lead\n  a = (x y)\n  -- mid\n  b = P(int q) | R\n}".to_string()),
    ]
}

fn main() {
    let mut args = std::env::args().skip(1);
    let pd = args.next().expect("usage: delta_oracle <parserdata.pb> [inputfile ...]");
    let files: Vec<String> = args.collect();
    let bytes = std::fs::read(&pd).expect("read parserdata");
    let data = Mgroup3ParserData::decode(bytes.as_slice()).expect("decode parserdata");
    let parser = Arc::new(Mgroup3Parser::new(data));
    let edits: usize = std::env::var("DELTA_EDITS").ok().and_then(|s| s.parse().ok()).unwrap_or(60);
    let mut st = Stats::default();

    if files.is_empty() {
        for (label, doc) in synthetic_docs() {
            run_doc(&parser, &label, &doc, edits, 0xABCDEF ^ (label.len() as u64), &mut st);
        }
    } else {
        for f in &files {
            let content = std::fs::read_to_string(f).expect("read input file");
            let label = std::path::Path::new(f).file_name().unwrap().to_string_lossy().into_owned();
            run_doc(&parser, &label, &content, edits, 0xC0FFEE ^ (label.len() as u64), &mut st);
        }
    }

    eprintln!("[delta_oracle] edits={} verified={} fallback={} (rejected={}) wholesale={}",
        st.edits, st.verified, st.fallback, st.rejected, st.wholesale);
    eprintln!("[delta_oracle] avg patched={:.1} avg freed={:.1} avg full-nodes={:.1} (reuse ratio {:.1}%)",
        st.total_patched as f64 / st.verified.max(1) as f64,
        st.total_freed as f64 / st.verified.max(1) as f64,
        st.total_full as f64 / st.verified.max(1) as f64,
        100.0 * (1.0 - st.total_patched as f64 / st.total_full.max(1) as f64));
    assert!(st.verified > 0, "delta oracle verified no spliced edits");
    println!("delta_oracle OK: {} spliced edits verified", st.verified);
}
"""

  /**
   * Static support module: Rust port of `com.giyeok.jparser.ktlib.{Kernel,
   * KernelSet}` + `AstifierUtil`. Kept in sync with
   * examples/generated/rust/ktlib_ast/mod.rs.
   */
  private val KTLIB_RS: String = """
    |// Generated (static) by Stage4RustEmit. Port of ktlib + AstifierUtil.
    |// TODO: move into mgroup3-native and depend on it instead of copying.
    |
    |use rustc_hash::FxHashSet;
    |
    |// `Kernel` is the per-gen kernel record. In the default (self-contained)
    |// build it is defined locally. When the crate is built with the `check-ast`
    |// or `ffi` feature, it aliases `mgroup3_native::parsing_ctx::KtlibKernel`
    |// (field/derive identical) so the parser's `kernels_history` output is fed
    |// into the walk with no per-element rebuild or hasher downgrade.
    |#[cfg(not(any(feature = "check-ast", feature = "ffi")))]
    |#[derive(Copy, Clone, PartialEq, Eq, Hash, Debug)]
    |pub struct Kernel {
    |    pub symbol_id: i32,
    |    pub pointer: i32,
    |    pub begin_gen: i32,
    |    pub end_gen: i32,
    |}
    |
    |#[cfg(any(feature = "check-ast", feature = "ffi"))]
    |pub type Kernel = mgroup3_native::parsing_ctx::KtlibKernel;
    |
    |// A per-gen kernel set. Alias over `FxHashSet<Kernel>` so it is exactly the
    |// element type produced by `Mgroup3Parser::kernels_history`
    |// (`Vec<FxHashSet<KtlibKernel>>`) — no wrap, no conversion.
    |pub type KernelSet = FxHashSet<Kernel>;
    |
    |// Query helpers over a kernel set. Kept as an extension trait so the walk
    |// (`ast.rs`) and the free helpers below can call `set.find_by_begin_gen_opt(..)`
    |// with method syntax even though `KernelSet` is a plain type alias.
    |pub trait KernelSetExt {
    |    fn filter_by_begin_gen(&self, symbol_id: i32, pointer: i32, begin_gen: i32) -> Vec<Kernel>;
    |    fn find_by_begin_gen(&self, symbol_id: i32, pointer: i32, begin_gen: i32) -> Kernel;
    |    fn find_by_begin_gen_opt(&self, symbol_id: i32, pointer: i32, begin_gen: i32) -> Option<Kernel>;
    |    fn get_single(&self, symbol_id: i32, pointer: i32, begin_gen: i32, end_gen: i32) -> Kernel;
    |}
    |
    |impl KernelSetExt for KernelSet {
    |    fn filter_by_begin_gen(&self, symbol_id: i32, pointer: i32, begin_gen: i32) -> Vec<Kernel> {
    |        self.iter()
    |            .filter(|k| k.symbol_id == symbol_id && k.pointer == pointer && k.begin_gen == begin_gen)
    |            .copied()
    |            .collect()
    |    }
    |
    |    fn find_by_begin_gen(&self, symbol_id: i32, pointer: i32, begin_gen: i32) -> Kernel {
    |        let matches = self.filter_by_begin_gen(symbol_id, pointer, begin_gen);
    |        check_single(&matches);
    |        matches[0]
    |    }
    |
    |    fn find_by_begin_gen_opt(&self, symbol_id: i32, pointer: i32, begin_gen: i32) -> Option<Kernel> {
    |        let matches = self.filter_by_begin_gen(symbol_id, pointer, begin_gen);
    |        check_single_or_none(&matches);
    |        matches.into_iter().next()
    |    }
    |
    |    fn get_single(&self, symbol_id: i32, pointer: i32, begin_gen: i32, end_gen: i32) -> Kernel {
    |        self.iter()
    |            .copied()
    |            .find(|k| {
    |                k.symbol_id == symbol_id
    |                    && k.pointer == pointer
    |                    && k.begin_gen == begin_gen
    |                    && k.end_gen == end_gen
    |            })
    |            .unwrap_or_else(|| panic!("no kernel at ({}, {}, {}..{}) in set", symbol_id, pointer, begin_gen, end_gen))
    |    }
    |}
    |
    |fn check_single<T>(xs: &[T]) {
    |    assert_eq!(xs.len(), 1, "Kernel size was expected to be 1, but it was {}", xs.len());
    |}
    |
    |fn check_single_or_none<T>(xs: &[T]) {
    |    assert!(xs.len() <= 1, "Kernel size was expected to be <= 1, was {}", xs.len());
    |}
    |
    |pub fn has_single_true(bs: &[bool]) -> bool {
    |    bs.iter().filter(|b| **b).count() == 1
    |}
    |
    |pub type GenSpan = (i32, i32);
    |
    |pub fn get_sequence_elems(
    |    history: &[KernelSet],
    |    sequence_id: i32,
    |    elems: &[i32],
    |    begin_gen: i32,
    |    end_gen: i32,
    |) -> Vec<GenSpan> {
    |    let n = elems.len();
    |    let last_elem = history[end_gen as usize].find_by_begin_gen(sequence_id, n as i32, begin_gen);
    |    let mut list = vec![last_elem];
    |    let mut curr_gen = last_elem.end_gen;
    |    for pointer in (0..n).rev() {
    |        let candidates =
    |            history[curr_gen as usize].filter_by_begin_gen(sequence_id, pointer as i32, begin_gen);
    |        let curr_gen_snapshot = curr_gen;
    |        let filtered: Vec<Kernel> = candidates
    |            .into_iter()
    |            .filter(|prev| {
    |                history[curr_gen_snapshot as usize].contains(&Kernel {
    |                    symbol_id: elems[pointer],
    |                    pointer: 1,
    |                    begin_gen: prev.end_gen,
    |                    end_gen: curr_gen_snapshot,
    |                })
    |            })
    |            .collect();
    |        check_single(&filtered);
    |        let prev_elem = filtered[0];
    |        list.push(prev_elem);
    |        curr_gen = prev_elem.end_gen;
    |    }
    |    (0..n).map(|i| (list[n - i].end_gen, list[n - i - 1].end_gen)).collect()
    |}
    |
    |pub fn unroll_repeat0(
    |    history: &[KernelSet],
    |    symbol_id: i32,
    |    item_sym_id: i32,
    |    base_seq: i32,
    |    repeat_seq: i32,
    |    begin_gen: i32,
    |    end_gen: i32,
    |) -> Vec<GenSpan> {
    |    let mut acc: Vec<GenSpan> = Vec::new();
    |    let mut bg = begin_gen;
    |    let mut eg = end_gen;
    |    loop {
    |        let base = history[eg as usize].find_by_begin_gen_opt(base_seq, 0, bg);
    |        let repeat = history[eg as usize].find_by_begin_gen_opt(repeat_seq, 2, bg);
    |        assert!(has_single_true(&[base.is_some(), repeat.is_some()]));
    |        if base.is_some() {
    |            return acc;
    |        }
    |        let seq = get_sequence_elems(history, repeat_seq, &[symbol_id, item_sym_id], bg, eg);
    |        let repeating = seq[0];
    |        let item = seq[1];
    |        acc.insert(0, item);
    |        bg = repeating.0;
    |        eg = repeating.1;
    |    }
    |}
    |
    |pub fn unroll_repeat1(
    |    history: &[KernelSet],
    |    symbol_id: i32,
    |    item_sym_id: i32,
    |    base_seq: i32,
    |    repeat_seq: i32,
    |    begin_gen: i32,
    |    end_gen: i32,
    |) -> Vec<GenSpan> {
    |    let mut acc: Vec<GenSpan> = Vec::new();
    |    let mut bg = begin_gen;
    |    let mut eg = end_gen;
    |    loop {
    |        let base = history[eg as usize].find_by_begin_gen_opt(base_seq, 1, bg);
    |        let repeat = history[eg as usize].find_by_begin_gen_opt(repeat_seq, 2, bg);
    |        assert!(has_single_true(&[base.is_some(), repeat.is_some()]));
    |        if base.is_some() {
    |            let base_item = history[eg as usize].find_by_begin_gen(item_sym_id, 1, bg);
    |            acc.insert(0, (base_item.begin_gen, base_item.end_gen));
    |            return acc;
    |        }
    |        let seq = get_sequence_elems(history, repeat_seq, &[symbol_id, item_sym_id], bg, eg);
    |        let repeating = seq[0];
    |        let item = seq[1];
    |        acc.insert(0, item);
    |        bg = repeating.0;
    |        eg = repeating.1;
    |    }
    |}
    |
    |pub struct IdIssuer {
    |    next: i32,
    |}
    |
    |impl IdIssuer {
    |    pub fn new(start: i32) -> Self {
    |        Self { next: start }
    |    }
    |    pub fn next_id(&mut self) -> i32 {
    |        let id = self.next;
    |        self.next += 1;
    |        id
    |    }
    |}
    |""".trimMargin()
}
