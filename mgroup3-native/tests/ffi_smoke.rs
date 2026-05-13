//! Smoke test for the C ABI in `src/ffi.rs`. Loads `libmgroup3_native.dylib`
//! via `libloading` and exercises the full lifecycle on a tiny fixture so we
//! catch ABI mistakes (signature drift, missing free, panic propagation)
//! without bringing a JVM into the picture.
//!
//! This is an *integration test* (not unit) so it runs in a process distinct
//! from the lib's tests — the dylib path is the same one a JVM would consume.

use std::ffi::{c_char, CString};
use std::path::PathBuf;
use std::ptr;

use libloading::{Library, Symbol};

const MGROUP3_OK: i32 = 0;

// Mirror the C signatures from src/ffi.rs.
type ParserNewFromFile =
    unsafe extern "C" fn(path: *const c_char, err: *mut i32) -> *mut u8;
type ParserParse = unsafe extern "C" fn(
    parser: *mut u8,
    input_bytes: *const u8,
    input_len: usize,
    out_ptr: *mut *mut u8,
    out_len: *mut usize,
) -> i32;
type FreeBuffer = unsafe extern "C" fn(ptr: *mut u8, len: usize);
type ParserFree = unsafe extern "C" fn(parser: *mut u8);
type NativeVersion = unsafe extern "C" fn() -> *const c_char;

fn dylib_path() -> PathBuf {
    let crate_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    let release = crate_dir.join("target/release/libmgroup3_native.dylib");
    let debug = crate_dir.join("target/debug/libmgroup3_native.dylib");
    if release.exists() {
        release
    } else if debug.exists() {
        debug
    } else {
        panic!(
            "no built dylib found — run `cargo build` or `cargo build --release` first \
             (looked in {:?} and {:?})",
            release, debug
        )
    }
}

/// Pick a fixture that's already committed so we don't depend on Kotlin
/// regenerating anything.
fn fixture_data_pb() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("tests/fixtures/parser/single_char/data.pb")
}

#[test]
fn ffi_smoke_roundtrip() {
    let lib = unsafe { Library::new(dylib_path()).expect("load dylib") };

    let version: Symbol<NativeVersion> =
        unsafe { lib.get(b"mgroup3_native_version").expect("symbol native_version") };
    let new_from_file: Symbol<ParserNewFromFile> = unsafe {
        lib.get(b"mgroup3_parser_new_from_file").expect("symbol new_from_file")
    };
    let parse: Symbol<ParserParse> =
        unsafe { lib.get(b"mgroup3_parser_parse").expect("symbol parse") };
    let free_buffer: Symbol<FreeBuffer> =
        unsafe { lib.get(b"mgroup3_free_buffer").expect("symbol free_buffer") };
    let parser_free: Symbol<ParserFree> =
        unsafe { lib.get(b"mgroup3_parser_free").expect("symbol parser_free") };

    // version probe — just confirm we get a non-null pointer to a valid C str.
    unsafe {
        let v_ptr = version();
        assert!(!v_ptr.is_null(), "version returned NULL");
        let v = std::ffi::CStr::from_ptr(v_ptr).to_str().expect("version utf8");
        assert!(!v.is_empty(), "version is empty");
    }

    // construct from file
    let data_path = fixture_data_pb();
    assert!(data_path.exists(), "fixture data.pb missing at {:?}", data_path);
    let c_path = CString::new(data_path.to_str().unwrap()).unwrap();
    let mut err = -1i32;
    let parser = unsafe { new_from_file(c_path.as_ptr(), &mut err) };
    assert_eq!(err, MGROUP3_OK, "new_from_file err={}", err);
    assert!(!parser.is_null(), "new_from_file returned NULL");

    // parse 'a' (accepted) and 'b' (rejected at first char)
    for (input, label) in [("a", "accepted"), ("b", "rejected")] {
        let mut out_ptr: *mut u8 = ptr::null_mut();
        let mut out_len: usize = 0;
        let bytes = input.as_bytes();
        let rc = unsafe {
            parse(parser, bytes.as_ptr(), bytes.len(), &mut out_ptr, &mut out_len)
        };
        assert_eq!(rc, MGROUP3_OK, "parse({}) rc={}", label, rc);
        assert!(!out_ptr.is_null(), "parse({}) returned null buffer", label);
        assert!(out_len > 0, "parse({}) returned empty buffer", label);
        // We don't decode here — that's exercised in lib unit tests. We just
        // confirm the buffer round-trip works.
        unsafe { free_buffer(out_ptr, out_len) };
    }

    // free
    unsafe { parser_free(parser) };
}

#[test]
fn ffi_smoke_null_handles_are_safe() {
    let lib = unsafe { Library::new(dylib_path()).expect("load dylib") };
    let free_buffer: Symbol<FreeBuffer> =
        unsafe { lib.get(b"mgroup3_free_buffer").expect("symbol free_buffer") };
    let parser_free: Symbol<ParserFree> =
        unsafe { lib.get(b"mgroup3_parser_free").expect("symbol parser_free") };

    // Both should accept NULL without crashing.
    unsafe { free_buffer(ptr::null_mut(), 0) };
    unsafe { parser_free(ptr::null_mut()) };
}
