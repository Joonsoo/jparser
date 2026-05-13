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
use crate::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;

// Error codes written to `*err` parameters or returned by `parser_parse`.
pub const MGROUP3_OK: i32 = 0;
pub const MGROUP3_ERR_NULL_ARG: i32 = 1;
pub const MGROUP3_ERR_PROTO_DECODE: i32 = 2;
pub const MGROUP3_ERR_IO: i32 = 3;
pub const MGROUP3_ERR_UTF8: i32 = 4;
pub const MGROUP3_ERR_PANIC: i32 = 5;

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
