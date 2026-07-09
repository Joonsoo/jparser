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

    protoDir.resolve("ast.proto").writeText(Stage2ProtoEmit.emit(schema))
    rustDir.resolve("Cargo.toml").writeText(cargoToml(mgroup3NativePath, crateName))
    rustDir.resolve("build.rs").writeText(buildRs())
    srcDir.resolve("lib.rs").writeText(libRs(schema))
    srcDir.resolve("ktlib.rs").writeText(KTLIB_RS)
    srcDir.resolve("ast.rs").writeText(astRs)
    srcDir.resolve("encode.rs").writeText(encodeRs)
    val binDir = srcDir.resolve("bin").also { it.createDirectories() }
    binDir.resolve("check_ast.rs").writeText(checkAstRs(crateName.replace('-', '_')))
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
    |[build-dependencies]
    |prost-build = "0.14"
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
    |//   - mgroup3_free_buffer (from mgroup3-native) to release the buffer.
    |
    |use std::panic::{catch_unwind, AssertUnwindSafe};
    |use std::ptr;
    |
    |use prost::Message;
    |
    |use mgroup3_native::parser::Mgroup3Parser;
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
    |""".trimMargin()

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
