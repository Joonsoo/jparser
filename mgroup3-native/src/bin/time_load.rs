//! Parserdata load-cost probe: break down the fixed cost paid before the
//! first parse.
//!
//! Two paths are measured per iteration:
//!   - proto path: file read / gunzip / prost decode / plain conversion+index.
//!   - rkyv cache path: cold (first run, writes cache) vs warm (reads cache).
//!
//! The rkyv warm path bypasses prost decode entirely, restoring
//! `ParserDataPlain` directly from a sibling `<path>.rkyv` archive.
//!
//! Usage: time_load <parserdata.pb[.gz]> [iters]
//!
//! Note: the cache file is written next to the source (`<path>.rkyv`). The
//! probe deletes it before the "cold" measurement so each run measures a true
//! cold→warm transition; it is left in place afterwards.

use std::io::Read;
use std::path::Path;
use std::time::Instant;

use mgroup3_native::parser::Mgroup3Parser;
use mgroup3_native::parser_cache;
use mgroup3_native::parser_data::ParserDataPlain;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn cache_path_of(source: &str) -> std::path::PathBuf {
    let src = Path::new(source);
    let mut name = src.file_name().map(|s| s.to_os_string()).unwrap_or_default();
    name.push(".rkyv");
    src.with_file_name(name)
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut args = std::env::args().skip(1);
    let data_path = args.next().expect("usage: time_load <parserdata.pb[.gz]> [iters]");
    let iters: usize = args.next().map(|s| s.parse().unwrap()).unwrap_or(3);

    let cache_path = cache_path_of(&data_path);

    for i in 0..iters {
        // ---------- proto path (baseline) ----------
        let t0 = Instant::now();
        let raw = std::fs::read(&data_path)?;
        let t_read = t0.elapsed();

        let t1 = Instant::now();
        let pb_bytes = if data_path.ends_with(".gz") {
            let mut buf = Vec::new();
            flate2::read::GzDecoder::new(raw.as_slice()).read_to_end(&mut buf)?;
            buf
        } else {
            raw.clone()
        };
        let t_gunzip = t1.elapsed();

        let t2 = Instant::now();
        let data = Mgroup3ParserData::decode(pb_bytes.as_slice())?;
        let t_decode = t2.elapsed();

        let t3 = Instant::now();
        let parser = Mgroup3Parser::new(data);
        let t_plain = t3.elapsed();
        std::hint::black_box(&parser);
        let t_proto_total = t0.elapsed();

        println!(
            "iter {i} PROTO : read={t_read:?} ({} bytes) gunzip={t_gunzip:?} ({} bytes) prost_decode={t_decode:?} plain+index={t_plain:?} | total={t_proto_total:?}",
            raw.len(),
            pb_bytes.len(),
        );

        // ---------- rkyv cache path: cold (no cache → proto + write cache) ----------
        let _ = std::fs::remove_file(&cache_path);
        let tc = Instant::now();
        let plain_cold: ParserDataPlain =
            parser_cache::load_plain_from_file(Path::new(&data_path))?;
        let t_cold = tc.elapsed();
        std::hint::black_box(&plain_cold);
        let cache_size = std::fs::metadata(&cache_path).map(|m| m.len()).unwrap_or(0);

        // ---------- rkyv cache path: warm (cache present → rkyv restore) ----------
        let tw = Instant::now();
        let plain_warm: ParserDataPlain =
            parser_cache::load_plain_from_file(Path::new(&data_path))?;
        let t_warm = tw.elapsed();
        // Build the parser from the warm plain to include the index cost too.
        let tw_idx = Instant::now();
        let parser_warm = Mgroup3Parser::from_plain(plain_warm);
        let t_warm_idx = tw_idx.elapsed();
        std::hint::black_box(&parser_warm);

        println!(
            "iter {i} RKYV  : cold(proto+write)={t_cold:?} | warm(load)={t_warm:?} warm(+index)={t_warm_idx:?} warm_total={:?} | cache={cache_size} bytes",
            t_warm + t_warm_idx,
        );

        // ---------- warm micro-breakdown (mmap stages, replicated inline) ----------
        // Mirrors parser_cache::load_cached stages so we can attribute cost:
        //   fs::open+mmap / source-hash gate / access_unchecked / deserialize /
        //   recompute_derived. Plus a zero-copy lower bound: mmap + access +
        //   touch-a-few-fields, WITHOUT the full deserialize.
        {
            use rkyv::rancor;
            use mgroup3_native::parser_data::ArchivedParserDataPlain;

            let src = std::fs::read(&data_path)?;
            let cp = cache_path.clone();

            let m0 = Instant::now();
            let file = std::fs::File::open(&cp)?;
            let mmap = unsafe { memmap2::Mmap::map(&file)? };
            let t_mmap = m0.elapsed();

            let raw: &[u8] = &mmap;
            const HEADER_LEN: usize = 64;
            let payload = &raw[HEADER_LEN..];

            let m1 = Instant::now();
            let sh = xxhash_rust::xxh3::xxh3_64(&src);
            std::hint::black_box(sh);
            let t_srchash = m1.elapsed();

            let m2 = Instant::now();
            let archived = unsafe { rkyv::access_unchecked::<ArchivedParserDataPlain>(payload) };
            let t_access = m2.elapsed();

            // Zero-copy lower bound: touch a handful of archived fields (forces the
            // pages holding those fields to fault in, but not the whole 300 MB).
            let m3 = Instant::now();
            let mut acc: u64 = archived.start_symbol_id.to_native() as u64;
            acc = acc.wrapping_add(archived.tip_edge_actions.len() as u64);
            acc = acc.wrapping_add(archived.mid_edge_actions.len() as u64);
            acc = acc.wrapping_add(archived.term_actions.len() as u64);
            acc = acc.wrapping_add(archived.milestone_groups.len() as u64);
            // touch the first tip edge action's nested id to hit a deeper page.
            if let Some(first) = archived.tip_edge_actions.iter().next() {
                acc = acc.wrapping_add(first.tip_group_id.to_native() as u64);
            }
            std::hint::black_box(acc);
            let t_touch = m3.elapsed();

            let m4 = Instant::now();
            let mut plain2 =
                rkyv::deserialize::<ParserDataPlain, rancor::Error>(archived).unwrap();
            let t_deser = m4.elapsed();

            let m5 = Instant::now();
            plain2.recompute_derived();
            let t_recompute = m5.elapsed();
            std::hint::black_box(&plain2);

            let zero_copy_lb = t_mmap + t_srchash + t_access + t_touch;
            println!(
                "iter {i} WARM* : mmap={t_mmap:?} src_hash={t_srchash:?} access={t_access:?} touch={t_touch:?} deserialize={t_deser:?} recompute_derived={t_recompute:?}",
            );
            println!(
                "iter {i} WARM* : zero-copy lower-bound (mmap+hash+access+touch, NO deserialize) = {zero_copy_lb:?}",
            );
        }
        println!();
    }
    Ok(())
}
