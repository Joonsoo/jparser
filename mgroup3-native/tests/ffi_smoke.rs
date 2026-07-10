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

// Session C signatures (Phase I3). The session handle is an opaque `*mut u8`.
type SessionNew = unsafe extern "C" fn(parser: *mut u8, err: *mut i32) -> *mut u8;
type SessionParseFull = unsafe extern "C" fn(
    session: *mut u8,
    input_bytes: *const u8,
    input_len: usize,
    out_ptr: *mut *mut u8,
    out_len: *mut usize,
) -> i32;
type SessionEdit = unsafe extern "C" fn(
    session: *mut u8,
    pos_char: usize,
    old_len_char: usize,
    new_bytes: *const u8,
    new_len: usize,
    out_ptr: *mut *mut u8,
    out_len: *mut usize,
) -> i32;
type SessionDestroy = unsafe extern "C" fn(session: *mut u8);

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
    let session_destroy: Symbol<SessionDestroy> =
        unsafe { lib.get(b"mgroup3_session_destroy").expect("symbol session_destroy") };

    // All should accept NULL without crashing.
    unsafe { free_buffer(ptr::null_mut(), 0) };
    unsafe { parser_free(ptr::null_mut()) };
    unsafe { session_destroy(ptr::null_mut()) };
}

/// A fixture that admits interesting mid-document edits: `('a' 'b')+`.
fn nested_repeat_data_pb() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("tests/fixtures/parser/nested_repeat/data.pb")
}

/// Run `mgroup3_parser_parse` on `text` and return the result-proto bytes. The
/// session must produce byte-identical bytes for the same final text.
fn direct_parse_bytes(
    parse: &Symbol<ParserParse>,
    free_buffer: &Symbol<FreeBuffer>,
    parser: *mut u8,
    text: &str,
) -> Vec<u8> {
    let mut out_ptr: *mut u8 = ptr::null_mut();
    let mut out_len: usize = 0;
    let bytes = text.as_bytes();
    let (in_ptr, in_len) = if bytes.is_empty() {
        (ptr::null(), 0usize)
    } else {
        (bytes.as_ptr(), bytes.len())
    };
    let rc = unsafe { parse(parser, in_ptr, in_len, &mut out_ptr, &mut out_len) };
    assert_eq!(rc, MGROUP3_OK, "direct parse({text:?}) rc={rc}");
    assert!(!out_ptr.is_null(), "direct parse({text:?}) null buffer");
    let copy = unsafe { std::slice::from_raw_parts(out_ptr, out_len).to_vec() };
    unsafe { free_buffer(out_ptr, out_len) };
    copy
}

/// End-to-end session lifecycle over FFI: new -> parse_full -> edit(s) ->
/// destroy. After each parse_full/edit the session's result bytes must be
/// byte-identical to `mgroup3_parser_parse` on the same final text (the design's
/// differential-oracle contract, exercised across the FFI boundary). Buffers are
/// always freed (no leak); the session is destroyed while the parser stays alive.
#[test]
fn ffi_smoke_session_lifecycle() {
    let lib = unsafe { Library::new(dylib_path()).expect("load dylib") };

    let new_from_file: Symbol<ParserNewFromFile> = unsafe {
        lib.get(b"mgroup3_parser_new_from_file").expect("symbol new_from_file")
    };
    let parse: Symbol<ParserParse> =
        unsafe { lib.get(b"mgroup3_parser_parse").expect("symbol parse") };
    let free_buffer: Symbol<FreeBuffer> =
        unsafe { lib.get(b"mgroup3_free_buffer").expect("symbol free_buffer") };
    let parser_free: Symbol<ParserFree> =
        unsafe { lib.get(b"mgroup3_parser_free").expect("symbol parser_free") };
    let session_new: Symbol<SessionNew> =
        unsafe { lib.get(b"mgroup3_session_new").expect("symbol session_new") };
    let session_parse_full: Symbol<SessionParseFull> =
        unsafe { lib.get(b"mgroup3_session_parse_full").expect("symbol session_parse_full") };
    let session_edit: Symbol<SessionEdit> =
        unsafe { lib.get(b"mgroup3_session_edit").expect("symbol session_edit") };
    let session_destroy: Symbol<SessionDestroy> =
        unsafe { lib.get(b"mgroup3_session_destroy").expect("symbol session_destroy") };

    let data_path = nested_repeat_data_pb();
    assert!(data_path.exists(), "fixture missing at {data_path:?}");
    let c_path = CString::new(data_path.to_str().unwrap()).unwrap();
    let mut err = -1i32;
    let parser = unsafe { new_from_file(c_path.as_ptr(), &mut err) };
    assert_eq!(err, MGROUP3_OK, "new_from_file err={err}");
    assert!(!parser.is_null());

    // Two sessions SHARE the one parser handle (the LSP's multi-document model).
    let mut serr = -1i32;
    let session = unsafe { session_new(parser, &mut serr) };
    assert_eq!(serr, MGROUP3_OK, "session_new err={serr}");
    assert!(!session.is_null());
    let mut serr2 = -1i32;
    let session2 = unsafe { session_new(parser, &mut serr2) };
    assert_eq!(serr2, MGROUP3_OK, "session_new(2) err={serr2}");
    assert!(!session2.is_null());

    let session_result = |input_len_edits: &dyn Fn() -> (i32, *mut u8, usize)| {
        let (rc, out_ptr, out_len) = input_len_edits();
        assert_eq!(rc, MGROUP3_OK, "session call rc={rc}");
        assert!(!out_ptr.is_null(), "session call null buffer");
        let copy = unsafe { std::slice::from_raw_parts(out_ptr, out_len).to_vec() };
        unsafe { free_buffer(out_ptr, out_len) };
        copy
    };

    // parse_full "abab" -> compare to direct parse.
    let full_bytes = session_result(&|| {
        let mut op: *mut u8 = ptr::null_mut();
        let mut ol: usize = 0;
        let t = b"abab";
        let rc = unsafe {
            session_parse_full(session, t.as_ptr(), t.len(), &mut op, &mut ol)
        };
        (rc, op, ol)
    });
    assert_eq!(
        full_bytes,
        direct_parse_bytes(&parse, &free_buffer, parser, "abab"),
        "session parse_full('abab') != direct parse"
    );

    // Edit sequence — each verified byte-identical to a from-scratch parse of the
    // edited text (insert / delete, interior + parse-breaking + recovery).
    // (pos_char, old_len_char, new_text, resulting full document)
    let edits: &[(usize, usize, &str, &str)] = &[
        (2, 0, "ab", "ababab"),  // insert "ab" mid -> ababab (splice-eligible)
        (0, 2, "", "abab"),      // delete leading "ab" -> abab
        (4, 0, "ab", "ababab"),  // append -> ababab
        (1, 1, "a", "aaabab"),   // replace 'b'->'a' — breaks the parse
        (1, 1, "b", "ababab"),   // fix back — recovers, session still usable
    ];
    for (i, &(pos, old, new, doc)) in edits.iter().enumerate() {
        let edited = session_result(&|| {
            let mut op: *mut u8 = ptr::null_mut();
            let mut ol: usize = 0;
            let nb = new.as_bytes();
            let (np, nl) = if nb.is_empty() {
                (ptr::null(), 0usize)
            } else {
                (nb.as_ptr(), nb.len())
            };
            let rc = unsafe {
                session_edit(session, pos, old, np, nl, &mut op, &mut ol)
            };
            (rc, op, ol)
        });
        assert_eq!(
            edited,
            direct_parse_bytes(&parse, &free_buffer, parser, doc),
            "edit#{i} (pos={pos},old={old},new={new:?}) session != direct parse of {doc:?}"
        );
    }

    // The second (independent) session over the SAME parser is unaffected by the
    // first session's edits — confirms sessions don't share mutable state.
    let s2_bytes = session_result(&|| {
        let mut op: *mut u8 = ptr::null_mut();
        let mut ol: usize = 0;
        let t = b"ababab";
        let rc = unsafe {
            session_parse_full(session2, t.as_ptr(), t.len(), &mut op, &mut ol)
        };
        (rc, op, ol)
    });
    assert_eq!(
        s2_bytes,
        direct_parse_bytes(&parse, &free_buffer, parser, "ababab"),
        "second session parse_full != direct parse"
    );

    // Destroy sessions BEFORE freeing the parser (lifetime contract). No leaks:
    // every result buffer above was freed.
    unsafe { session_destroy(session) };
    unsafe { session_destroy(session2) };
    unsafe { parser_free(parser) };
}

/// Bad UTF-8 into a session edit returns a nonzero code and a null buffer (no
/// leak, no crash); the session remains usable afterward.
#[test]
fn ffi_smoke_session_bad_utf8_is_safe() {
    let lib = unsafe { Library::new(dylib_path()).expect("load dylib") };
    let new_from_file: Symbol<ParserNewFromFile> = unsafe {
        lib.get(b"mgroup3_parser_new_from_file").expect("symbol new_from_file")
    };
    let free_buffer: Symbol<FreeBuffer> =
        unsafe { lib.get(b"mgroup3_free_buffer").expect("symbol free_buffer") };
    let parser_free: Symbol<ParserFree> =
        unsafe { lib.get(b"mgroup3_parser_free").expect("symbol parser_free") };
    let session_new: Symbol<SessionNew> =
        unsafe { lib.get(b"mgroup3_session_new").expect("symbol session_new") };
    let session_parse_full: Symbol<SessionParseFull> =
        unsafe { lib.get(b"mgroup3_session_parse_full").expect("symbol session_parse_full") };
    let session_destroy: Symbol<SessionDestroy> =
        unsafe { lib.get(b"mgroup3_session_destroy").expect("symbol session_destroy") };

    let data_path = nested_repeat_data_pb();
    let c_path = CString::new(data_path.to_str().unwrap()).unwrap();
    let mut err = -1i32;
    let parser = unsafe { new_from_file(c_path.as_ptr(), &mut err) };
    assert_eq!(err, MGROUP3_OK);
    let mut serr = -1i32;
    let session = unsafe { session_new(parser, &mut serr) };
    assert_eq!(serr, MGROUP3_OK);

    // Invalid UTF-8 (lone 0xFF) into parse_full: nonzero rc, null buffer.
    let bad: [u8; 1] = [0xFF];
    let mut op: *mut u8 = ptr::null_mut();
    let mut ol: usize = 0;
    let rc = unsafe {
        session_parse_full(session, bad.as_ptr(), bad.len(), &mut op, &mut ol)
    };
    assert_ne!(rc, MGROUP3_OK, "bad utf8 should fail");
    assert!(op.is_null(), "bad utf8 must leave a null buffer");
    assert_eq!(ol, 0);
    // Nothing to free (null buffer). Confirm free_buffer(null) is still a no-op.
    unsafe { free_buffer(ptr::null_mut(), 0) };

    unsafe { session_destroy(session) };
    unsafe { parser_free(parser) };
}
