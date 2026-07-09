//! mgroup4 shape-stats probe — parse an input with a parserdata.pb at a given
//! interior window `n` and print the realized-shape ratio (Kotlin cross-check).
//!
//! Usage: mg4_shape_stats <parserdata.pb> <input.txt> [n]
//!   n defaults to env MG4_INTERIOR_N or 1. This bin forces MG4_SHAPE_STATS on
//!   so the ratio counters are populated regardless of the env.
//!
//! The reported `ratio = meanBase / meanMerged` must match the Kotlin
//! `reportMg4Stats` ratio for the same (grammar, input, n) — the two independent
//! implementations cross-validate (B1b gate (ii)).

use mgroup4_native::parser::Mgroup4Parser;
use mgroup4_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Force shape stats on so the counters populate.
    unsafe {
        std::env::set_var("MG4_SHAPE_STATS", "1");
    }
    let mut args = std::env::args().skip(1);
    let data_path = args
        .next()
        .expect("usage: mg4_shape_stats <parserdata.pb> <input.txt> [n]");
    let input_path = args
        .next()
        .expect("usage: mg4_shape_stats <parserdata.pb> <input.txt> [n]");
    let n: i32 = args
        .next()
        .and_then(|s| s.parse().ok())
        .or_else(|| std::env::var("MG4_INTERIOR_N").ok().and_then(|s| s.parse().ok()))
        .unwrap_or(1);

    let data = Mgroup3ParserData::decode(std::fs::read(&data_path)?.as_slice())?;
    // new_with_n honors env MG4_INTERIOR_N if set; pass n explicitly otherwise.
    let parser = Mgroup4Parser::new_with_n(data, n);
    let input = std::fs::read_to_string(&input_path)?;

    let ctx = parser.parse(&input).map_err(|e| format!("{:?}", e))?;
    let accepted = parser.is_accepted(&ctx);
    println!(
        "file={} n={} accepted={}",
        std::path::Path::new(&input_path)
            .file_name()
            .unwrap()
            .to_string_lossy(),
        parser.interior_group_max_depth(),
        accepted,
    );
    println!("{}", parser.report_mg4_stats());
    Ok(())
}
