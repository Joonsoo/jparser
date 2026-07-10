//! C-ABI exports for use from the JVM (via FFM / JEP 454) or any other host
//! language that speaks C calling convention.
//!
//! All exports are wrapped in `catch_unwind` so a Rust panic never crosses the
//! boundary as an unwind. On panic, fallible entry points return a nonzero
//! error code.
//!
//! Memory ownership:
//! - `mgroup3_parser_new*` returns a `*mut Mgroup3Parser` allocated with
//!   `Box::into_raw`. Free with `mgroup3_parser_free`.
//! - `mgroup3_parser_parse` writes a `(ptr, len)` pair pointing at a heap
//!   buffer the callee allocated via `Box::into_raw(boxed_slice)`. The
//!   caller must release it with `mgroup3_free_buffer`.
//!
//! Threading:
//! - `Mgroup3Parser` is not thread-safe (a `RefCell` cache lives inside).
//!   The caller must not invoke `mgroup3_parser_parse` concurrently on the
//!   same handle. Different handles are independent.

use std::ffi::CStr;
use std::fs::File;
use std::io::Read;
use std::os::raw::c_char;
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::ptr;

use prost::Message;

use crate::parser::{encode_parse_result, Mgroup3Parser};
use crate::parser_cache;
use crate::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use crate::session::ParseSession;

// Error codes written to `*err` parameters or returned by `parser_parse`.
pub const MGROUP3_OK: i32 = 0;
pub const MGROUP3_ERR_NULL_ARG: i32 = 1;
pub const MGROUP3_ERR_PROTO_DECODE: i32 = 2;
pub const MGROUP3_ERR_IO: i32 = 3;
pub const MGROUP3_ERR_UTF8: i32 = 4;
pub const MGROUP3_ERR_PANIC: i32 = 5;
/// rkyv 캐시 로드/작성 orchestration 실패 (파일 IO 또는 proto decode). 세부 원인은
/// 구분하지 않고 하나의 코드로 보고한다 — 캐시 경로는 실패 시 proto 경로와 동일한
/// 이유(IO/decode)로만 깨진다.
pub const MGROUP3_ERR_CACHE: i32 = 6;

fn write_err(err_out: *mut i32, code: i32) {
    if !err_out.is_null() {
        unsafe {
            *err_out = code;
        }
    }
}

/// Construct a parser from `Mgroup3ParserData` proto bytes already in memory.
///
/// On success returns a non-null handle and writes `MGROUP3_OK` to `*err`.
/// On failure returns null and writes a nonzero code.
#[unsafe(no_mangle)]
pub extern "C" fn mgroup3_parser_new(
    data_bytes: *const u8,
    data_len: usize,
    err: *mut i32,
) -> *mut Mgroup3Parser {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if data_bytes.is_null() && data_len != 0 {
            return Err(MGROUP3_ERR_NULL_ARG);
        }
        let slice = if data_len == 0 {
            &[][..]
        } else {
            unsafe { std::slice::from_raw_parts(data_bytes, data_len) }
        };
        let data = Mgroup3ParserData::decode(slice).map_err(|_| MGROUP3_ERR_PROTO_DECODE)?;
        Ok(Mgroup3Parser::new(data))
    }));
    match result {
        Ok(Ok(parser)) => {
            write_err(err, MGROUP3_OK);
            Box::into_raw(Box::new(parser))
        }
        Ok(Err(code)) => {
            write_err(err, code);
            ptr::null_mut()
        }
        Err(_) => {
            write_err(err, MGROUP3_ERR_PANIC);
            ptr::null_mut()
        }
    }
}

/// Construct a parser by reading the parser-data file directly on the native
/// side. The path is a UTF-8, null-terminated string. If it ends with `.gz`
/// the contents are gunzipped during decode. The JVM heap is not used —
/// callers can avoid lifting multi-MB buffers through Java just to pass them
/// straight back to native.
#[unsafe(no_mangle)]
pub extern "C" fn mgroup3_parser_new_from_file(
    path: *const c_char,
    err: *mut i32,
) -> *mut Mgroup3Parser {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if path.is_null() {
            return Err(MGROUP3_ERR_NULL_ARG);
        }
        let path_cstr = unsafe { CStr::from_ptr(path) };
        let path_str = path_cstr.to_str().map_err(|_| MGROUP3_ERR_UTF8)?;
        let mut buf = Vec::new();
        let mut file = File::open(path_str).map_err(|_| MGROUP3_ERR_IO)?;
        if path_str.ends_with(".gz") {
            let mut decoder = flate2::read::GzDecoder::new(file);
            decoder.read_to_end(&mut buf).map_err(|_| MGROUP3_ERR_IO)?;
        } else {
            file.read_to_end(&mut buf).map_err(|_| MGROUP3_ERR_IO)?;
        }
        let data =
            Mgroup3ParserData::decode(buf.as_slice()).map_err(|_| MGROUP3_ERR_PROTO_DECODE)?;
        Ok(Mgroup3Parser::new(data))
    }));
    match result {
        Ok(Ok(parser)) => {
            write_err(err, MGROUP3_OK);
            Box::into_raw(Box::new(parser))
        }
        Ok(Err(code)) => {
            write_err(err, code);
            ptr::null_mut()
        }
        Err(_) => {
            write_err(err, MGROUP3_ERR_PANIC);
            ptr::null_mut()
        }
    }
}

/// Like `mgroup3_parser_new_from_file`, but uses a sibling rkyv cache
/// (`<path>.rkyv`) to skip the prost decode on warm loads. On a cache miss/
/// invalid/corrupt cache it falls back to the proto path (read → gunzip →
/// prost decode → from_proto) and best-effort rewrites the cache. The parser
/// produced is identical to the proto path (same `ParserDataPlain`), so parse
/// semantics are unchanged.
///
/// Same error-code convention as the other constructors. `MGROUP3_ERR_CACHE`
/// signals an IO or decode failure in the orchestration (a missing/stale cache
/// is not an error — it silently falls back).
#[unsafe(no_mangle)]
pub extern "C" fn mgroup3_parser_new_from_file_cached(
    path: *const c_char,
    err: *mut i32,
) -> *mut Mgroup3Parser {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if path.is_null() {
            return Err(MGROUP3_ERR_NULL_ARG);
        }
        let path_cstr = unsafe { CStr::from_ptr(path) };
        let path_str = path_cstr.to_str().map_err(|_| MGROUP3_ERR_UTF8)?;
        let plain = parser_cache::load_plain_from_file(std::path::Path::new(path_str))
            .map_err(|_| MGROUP3_ERR_CACHE)?;
        Ok(Mgroup3Parser::from_plain(plain))
    }));
    match result {
        Ok(Ok(parser)) => {
            write_err(err, MGROUP3_OK);
            Box::into_raw(Box::new(parser))
        }
        Ok(Err(code)) => {
            write_err(err, code);
            ptr::null_mut()
        }
        Err(_) => {
            write_err(err, MGROUP3_ERR_PANIC);
            ptr::null_mut()
        }
    }
}

/// Parse `input_bytes` (UTF-8) against the given parser. The result —
/// success OR error — is encoded as a `Mgroup3ParseResult` proto and the
/// resulting bytes are returned via `(*out_ptr, *out_len)`. Caller must
/// release that buffer via `mgroup3_free_buffer`.
///
/// Returns `MGROUP3_OK` on success (including parse rejection encoded inside
/// the result proto) or a nonzero code on internal failure (panic, bad UTF-8,
/// null arg).
#[unsafe(no_mangle)]
pub extern "C" fn mgroup3_parser_parse(
    parser: *mut Mgroup3Parser,
    input_bytes: *const u8,
    input_len: usize,
    out_ptr: *mut *mut u8,
    out_len: *mut usize,
) -> i32 {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if parser.is_null() || out_ptr.is_null() || out_len.is_null() {
            return Err(MGROUP3_ERR_NULL_ARG);
        }
        let parser_ref = unsafe { &*parser };
        let slice = if input_len == 0 {
            &[][..]
        } else {
            if input_bytes.is_null() {
                return Err(MGROUP3_ERR_NULL_ARG);
            }
            unsafe { std::slice::from_raw_parts(input_bytes, input_len) }
        };
        let text = std::str::from_utf8(slice).map_err(|_| MGROUP3_ERR_UTF8)?;
        let outcome = parser_ref.parse(text);
        let bytes = encode_parse_result(parser_ref, outcome.as_ref().map_err(|e| e));
        let boxed = bytes.into_boxed_slice();
        let len = boxed.len();
        let raw = Box::into_raw(boxed) as *mut u8;
        unsafe {
            *out_ptr = raw;
            *out_len = len;
        }
        Ok(())
    }));
    match result {
        Ok(Ok(())) => MGROUP3_OK,
        Ok(Err(code)) => {
            if !out_ptr.is_null() {
                unsafe {
                    *out_ptr = ptr::null_mut();
                }
            }
            if !out_len.is_null() {
                unsafe {
                    *out_len = 0;
                }
            }
            code
        }
        Err(_) => {
            if !out_ptr.is_null() {
                unsafe {
                    *out_ptr = ptr::null_mut();
                }
            }
            if !out_len.is_null() {
                unsafe {
                    *out_len = 0;
                }
            }
            MGROUP3_ERR_PANIC
        }
    }
}

/// Free a buffer previously returned via `mgroup3_parser_parse`.
#[unsafe(no_mangle)]
pub extern "C" fn mgroup3_free_buffer(ptr: *mut u8, len: usize) {
    if ptr.is_null() || len == 0 {
        return;
    }
    let _ = catch_unwind(AssertUnwindSafe(|| unsafe {
        let slice = std::slice::from_raw_parts_mut(ptr, len);
        drop(Box::from_raw(slice as *mut [u8]));
    }));
}

/// Drop a parser handle previously returned by one of the `mgroup3_parser_new*`
/// functions.
#[unsafe(no_mangle)]
pub extern "C" fn mgroup3_parser_free(parser: *mut Mgroup3Parser) {
    if parser.is_null() {
        return;
    }
    let _ = catch_unwind(AssertUnwindSafe(|| unsafe {
        drop(Box::from_raw(parser));
    }));
}

/// Static version probe — returns `CARGO_PKG_VERSION` as a null-terminated
/// string. Useful for asserting that the loaded `.dylib` matches the expected
/// build.
#[unsafe(no_mangle)]
pub extern "C" fn mgroup3_native_version() -> *const c_char {
    static VERSION: &str = concat!(env!("CARGO_PKG_VERSION"), "\0");
    VERSION.as_ptr() as *const c_char
}

// ---------------------------------------------------------------------------
// Incremental parse session (Phase I3) — ADDITIVE. The symbols above are
// unchanged. A session keeps one document + a checkpoint ring so an edit can
// resume/splice instead of re-parsing from scratch (see `session.rs`). Its
// result is byte-identical to `mgroup3_parser_parse` on the same final text.
//
// Threading / lifetime:
// - A session is a SINGLE-DOCUMENT object. Do not call session functions
//   concurrently on the SAME session handle. Different sessions are independent
//   (each carries its own document + checkpoints), so per-document sessions on
//   separate threads are fine — they only ever READ the shared parser.
// - The session BORROWS the parser handle (it does not take ownership): ONE
//   parser handle may back MANY sessions (the LSP shares a single handle across
//   all open documents). LIFETIME CONTRACT: the parser handle passed to
//   `mgroup3_session_new` MUST outlive every session created from it. Destroy all
//   sessions (`mgroup3_session_destroy`) BEFORE freeing the parser
//   (`mgroup3_parser_free`). Violating this dereferences a freed parser.

/// Create a session over an existing parser handle. The parser is BORROWED — it
/// must outlive the session (see the lifetime contract above). Returns a non-null
/// session handle and writes `MGROUP3_OK` to `*err` on success; returns null and a
/// nonzero code on null-arg / panic. Free the session with
/// `mgroup3_session_destroy`.
#[unsafe(no_mangle)]
pub extern "C" fn mgroup3_session_new(
    parser: *mut Mgroup3Parser,
    err: *mut i32,
) -> *mut ParseSession {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if parser.is_null() {
            return Err(MGROUP3_ERR_NULL_ARG);
        }
        // SAFETY: `parser` is non-null here; the caller upholds the lifetime
        // contract that it stays valid for the session's whole life. The default
        // checkpoint interval matches the Rust-native `ParseSession::new`.
        let session = unsafe {
            ParseSession::from_raw_parser(
                parser as *const Mgroup3Parser,
                crate::session::DEFAULT_CHECKPOINT_INTERVAL,
            )
        };
        Ok(session)
    }));
    match result {
        Ok(Ok(session)) => {
            write_err(err, MGROUP3_OK);
            Box::into_raw(Box::new(session))
        }
        Ok(Err(code)) => {
            write_err(err, code);
            ptr::null_mut()
        }
        Err(_) => {
            write_err(err, MGROUP3_ERR_PANIC);
            ptr::null_mut()
        }
    }
}

/// Emit the session's current outcome as `Mgroup3ParseResult` proto bytes via
/// `(*out_ptr, *out_len)` (release with `mgroup3_free_buffer`). Shared tail of
/// `parse_full`/`edit`. Assumes a parse has run (the session sets an outcome on
/// every `parse_full`/`edit`).
fn write_session_result(
    session: &ParseSession,
    out_ptr: *mut *mut u8,
    out_len: *mut usize,
) -> Result<(), i32> {
    // A parse always leaves an outcome; `None` only before the first parse, which
    // the entry points below never expose (they parse first).
    let bytes = session.encode_result().ok_or(MGROUP3_ERR_PANIC)?;
    let boxed = bytes.into_boxed_slice();
    let len = boxed.len();
    let raw = Box::into_raw(boxed) as *mut u8;
    unsafe {
        *out_ptr = raw;
        *out_len = len;
    }
    Ok(())
}

fn finish_session_call(
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
        Ok(Ok(())) => MGROUP3_OK,
        Ok(Err(code)) => {
            clear();
            code
        }
        Err(_) => {
            clear();
            MGROUP3_ERR_PANIC
        }
    }
}

/// Full (re)parse of the whole document `input_bytes` (UTF-8) from gen 0. Resets
/// the session's checkpoint ring + baseline. Encodes the result exactly like
/// `mgroup3_parser_parse`. `MGROUP3_OK` on success (parse rejection is encoded
/// inside the result proto); nonzero on null-arg / bad UTF-8 / panic.
#[unsafe(no_mangle)]
pub extern "C" fn mgroup3_session_parse_full(
    session: *mut ParseSession,
    input_bytes: *const u8,
    input_len: usize,
    out_ptr: *mut *mut u8,
    out_len: *mut usize,
) -> i32 {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if session.is_null() || out_ptr.is_null() || out_len.is_null() {
            return Err(MGROUP3_ERR_NULL_ARG);
        }
        let slice = if input_len == 0 {
            &[][..]
        } else {
            if input_bytes.is_null() {
                return Err(MGROUP3_ERR_NULL_ARG);
            }
            unsafe { std::slice::from_raw_parts(input_bytes, input_len) }
        };
        let text = std::str::from_utf8(slice).map_err(|_| MGROUP3_ERR_UTF8)?;
        let session_ref = unsafe { &mut *session };
        session_ref.parse_full(text);
        write_session_result(session_ref, out_ptr, out_len)
    }));
    finish_session_call(result, out_ptr, out_len)
}

/// Apply an edit and re-parse incrementally. `pos_char`/`old_len_char` are CHAR
/// (Unicode code point) offsets into the current document — NOT UTF-16 code units
/// and NOT bytes. The host is responsible for converting LSP UTF-16 positions to
/// code-point offsets (see `incremental_parsing.md` §LSP). `new_bytes` (UTF-8) is
/// the replacement text. Encodes the result exactly like `mgroup3_parser_parse`;
/// the result is byte-identical to a full re-parse of the edited text.
///
/// On a parse error the session's outcome becomes that error (encoded in the
/// result) and the NEXT edit safely re-parses from scratch (no stale baseline) —
/// the session stays usable.
#[unsafe(no_mangle)]
pub extern "C" fn mgroup3_session_edit(
    session: *mut ParseSession,
    pos_char: usize,
    old_len_char: usize,
    new_bytes: *const u8,
    new_len: usize,
    out_ptr: *mut *mut u8,
    out_len: *mut usize,
) -> i32 {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if session.is_null() || out_ptr.is_null() || out_len.is_null() {
            return Err(MGROUP3_ERR_NULL_ARG);
        }
        let slice = if new_len == 0 {
            &[][..]
        } else {
            if new_bytes.is_null() {
                return Err(MGROUP3_ERR_NULL_ARG);
            }
            unsafe { std::slice::from_raw_parts(new_bytes, new_len) }
        };
        let new_text = std::str::from_utf8(slice).map_err(|_| MGROUP3_ERR_UTF8)?;
        let session_ref = unsafe { &mut *session };
        session_ref.edit(pos_char, old_len_char, new_text);
        write_session_result(session_ref, out_ptr, out_len)
    }));
    finish_session_call(result, out_ptr, out_len)
}

/// Drop a session handle previously returned by `mgroup3_session_new`. Does NOT
/// free the borrowed parser — free that separately with `mgroup3_parser_free`
/// AFTER all its sessions are destroyed.
#[unsafe(no_mangle)]
pub extern "C" fn mgroup3_session_destroy(session: *mut ParseSession) {
    if session.is_null() {
        return;
    }
    let _ = catch_unwind(AssertUnwindSafe(|| unsafe {
        drop(Box::from_raw(session));
    }));
}
