//! Equivalence test: the rkyv cache path must produce a parser identical to the
//! proto path. For a set of small committed fixtures we build a parser both ways
//! and assert every input's `is_accepted` + serialized `kernels_history` match.
//!
//! The cache file is written into a per-test tempdir (never the committed
//! fixture dir) and cleaned up at the end, so fixtures stay pristine.

use std::collections::BTreeSet;
use std::fs;
use std::path::{Path, PathBuf};

use mgroup3_native::parser::{Mgroup3Parser, ParsingError};
use mgroup3_native::parser_cache;
use mgroup3_native::parsing_ctx::KtlibKernel;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn committed_fixtures_root() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("tests/fixtures/parser")
}

/// Minimal self-cleaning tempdir (no tempfile dep). Unique per (pid, nanos).
struct TempDir(PathBuf);
impl TempDir {
    fn new(tag: &str) -> Self {
        let nanos = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_nanos();
        let p = std::env::temp_dir().join(format!(
            "mg3_cache_eq_{}_{}_{}",
            tag,
            std::process::id(),
            nanos
        ));
        fs::create_dir_all(&p).expect("create tempdir");
        TempDir(p)
    }
    fn path(&self) -> &Path {
        &self.0
    }
}
impl Drop for TempDir {
    fn drop(&mut self) {
        let _ = fs::remove_dir_all(&self.0);
    }
}

fn serialize_result(parser: &Mgroup3Parser, input: &str) -> String {
    match parser.parse(input) {
        Ok(ctx) => {
            if parser.is_accepted(&ctx) {
                let mut out = String::from("ACCEPTED\n");
                for (gen_idx, kernels) in parser.kernels_history(&ctx).iter().enumerate() {
                    out.push_str(&format!("# gen {}\n", gen_idx));
                    let sorted: BTreeSet<KtlibKernel> = kernels.iter().copied().collect();
                    for k in sorted {
                        out.push_str(&format!(
                            "{} {} {} {}\n",
                            k.symbol_id, k.pointer, k.begin_gen, k.end_gen
                        ));
                    }
                }
                out
            } else {
                "REJECTED\n".to_string()
            }
        }
        Err(ParsingError::UnexpectedInput { .. } | ParsingError::UnexpectedEof { .. }) => {
            "REJECTED\n".to_string()
        }
    }
}

fn inputs_of(case_dir: &Path) -> Vec<String> {
    let inputs_dir = case_dir.join("inputs");
    let mut entries: Vec<PathBuf> = fs::read_dir(&inputs_dir)
        .unwrap_or_else(|e| panic!("read_dir {}: {}", inputs_dir.display(), e))
        .filter_map(|e| e.ok().map(|e| e.path()))
        .filter(|p| p.is_dir())
        .collect();
    entries.sort();
    entries
        .into_iter()
        .map(|d| {
            fs::read_to_string(d.join("input.txt"))
                .unwrap_or_else(|e| panic!("read {}/input.txt: {}", d.display(), e))
        })
        .collect()
}

#[test]
fn proto_and_cache_paths_agree() {
    let root = committed_fixtures_root();
    assert!(
        root.exists(),
        "committed fixture dir {} missing",
        root.display()
    );

    let mut case_dirs: Vec<PathBuf> = fs::read_dir(&root)
        .unwrap_or_else(|e| panic!("read_dir {}: {}", root.display(), e))
        .filter_map(|e| e.ok().map(|e| e.path()))
        .filter(|p| p.is_dir() && p.join("data.pb").exists())
        .collect();
    case_dirs.sort();
    assert!(!case_dirs.is_empty(), "no fixture cases with data.pb under {}", root.display());

    let tmp = TempDir::new("main");
    let mut total_inputs = 0usize;
    let mut checked_cases = 0usize;

    for case_dir in &case_dirs {
        let case_name = case_dir.file_name().unwrap().to_string_lossy().into_owned();
        let src_pb = case_dir.join("data.pb");
        let bytes = fs::read(&src_pb).expect("read data.pb");

        // Proto-path parser.
        let proto_parser = {
            let data = Mgroup3ParserData::decode(bytes.as_slice()).expect("decode data.pb");
            Mgroup3Parser::new(data)
        };

        // Cache-path parser: copy data.pb into the tempdir, then load through the
        // cache orchestration twice — cold (writes <path>.rkyv) then warm (reads
        // it). Both must equal the proto parser.
        let tmp_pb = tmp.path().join(format!("{}.pb", case_name));
        fs::write(&tmp_pb, &bytes).expect("write temp data.pb");

        let plain_cold =
            parser_cache::load_plain_from_file(&tmp_pb).expect("cold cache load");
        let cache_file = {
            let mut n = tmp_pb.file_name().unwrap().to_os_string();
            n.push(".rkyv");
            tmp_pb.with_file_name(n)
        };
        assert!(
            cache_file.exists(),
            "cold load did not write cache file {}",
            cache_file.display()
        );
        let cold_parser = Mgroup3Parser::from_plain(plain_cold);

        let plain_warm =
            parser_cache::load_plain_from_file(&tmp_pb).expect("warm cache load");
        let warm_parser = Mgroup3Parser::from_plain(plain_warm);

        // start symbol must match across all three.
        assert_eq!(
            proto_parser.start_symbol_id(),
            cold_parser.start_symbol_id(),
            "{}: start symbol mismatch proto vs cold",
            case_name
        );
        assert_eq!(
            proto_parser.start_symbol_id(),
            warm_parser.start_symbol_id(),
            "{}: start symbol mismatch proto vs warm",
            case_name
        );

        for input in inputs_of(case_dir) {
            total_inputs += 1;
            let expected = serialize_result(&proto_parser, &input);
            let got_cold = serialize_result(&cold_parser, &input);
            let got_warm = serialize_result(&warm_parser, &input);
            assert_eq!(
                expected, got_cold,
                "{}: cold-cache result diverged for input {:?}",
                case_name, input
            );
            assert_eq!(
                expected, got_warm,
                "{}: warm-cache result diverged for input {:?}",
                case_name, input
            );
        }
        checked_cases += 1;
    }

    println!(
        "cache_equivalence: {} cases / {} inputs — proto == cold == warm",
        checked_cases, total_inputs
    );
    // tmp dropped here (removes the tempdir + any .rkyv files).
}

/// A stale/corrupt cache must be ignored (fall back to proto) rather than
/// producing a wrong parser or erroring.
#[test]
fn corrupt_cache_falls_back() {
    let root = committed_fixtures_root();
    let case_dir = root.join("simple_sequence");
    if !case_dir.join("data.pb").exists() {
        // Fixture set changed — nothing to assert here.
        return;
    }
    let bytes = fs::read(case_dir.join("data.pb")).expect("read data.pb");

    let tmp = TempDir::new("corrupt");
    let tmp_pb = tmp.path().join("simple_sequence.pb");
    fs::write(&tmp_pb, &bytes).expect("write temp data.pb");
    let cache_file = {
        let mut n = tmp_pb.file_name().unwrap().to_os_string();
        n.push(".rkyv");
        tmp_pb.with_file_name(n)
    };

    // Write garbage where the cache should be.
    fs::write(&cache_file, b"not a valid rkyv cache header at all").unwrap();

    // Load must still succeed via proto fallback and rewrite the cache.
    let plain = parser_cache::load_plain_from_file(&tmp_pb).expect("fallback load");
    let parser = Mgroup3Parser::from_plain(plain);
    let proto_parser =
        Mgroup3Parser::new(Mgroup3ParserData::decode(bytes.as_slice()).unwrap());
    assert_eq!(parser.start_symbol_id(), proto_parser.start_symbol_id());

    // The corrupt cache should have been overwritten with a valid one; the next
    // load succeeds too.
    let plain2 = parser_cache::load_plain_from_file(&tmp_pb).expect("second load");
    let _ = Mgroup3Parser::from_plain(plain2);
}

/// The mmap-based loader must not panic on degenerate cache files: an empty file
/// (mmap of a 0-length file fails on some platforms), a file shorter than the
/// 64-byte header, and a header-sized-but-bogus file. All must fall back cleanly.
#[test]
fn degenerate_cache_files_fall_back() {
    let root = committed_fixtures_root();
    let case_dir = root.join("simple_sequence");
    if !case_dir.join("data.pb").exists() {
        return;
    }
    let bytes = fs::read(case_dir.join("data.pb")).expect("read data.pb");
    let proto_start = Mgroup3Parser::new(Mgroup3ParserData::decode(bytes.as_slice()).unwrap())
        .start_symbol_id();

    for (tag, cache_contents) in [
        ("empty", vec![]),
        ("tiny", vec![0u8; 8]),
        ("under_header", vec![0xABu8; 40]),
        ("header_only_garbage", vec![0xCDu8; 64]),
    ] {
        let tmp = TempDir::new(tag);
        let tmp_pb = tmp.path().join("simple_sequence.pb");
        fs::write(&tmp_pb, &bytes).expect("write temp data.pb");
        let cache_file = {
            let mut n = tmp_pb.file_name().unwrap().to_os_string();
            n.push(".rkyv");
            tmp_pb.with_file_name(n)
        };
        fs::write(&cache_file, &cache_contents).unwrap();

        // Must not panic; must produce a correct parser via proto fallback.
        let plain = parser_cache::load_plain_from_file(&tmp_pb)
            .unwrap_or_else(|e| panic!("{tag}: fallback load failed: {e}"));
        let parser = Mgroup3Parser::from_plain(plain);
        assert_eq!(
            parser.start_symbol_id(),
            proto_start,
            "{tag}: start symbol mismatch after fallback"
        );
    }
}
