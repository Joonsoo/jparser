//! Quick wall-clock probe: parse an input with a parserdata.pb and report the
//! cost of each phase (parse / is_accepted / kernels_history) separately.
//!
//! Usage: time_parse <parserdata.pb> <input.txt> [iters]

use std::time::Instant;

use mgroup4_native::parser::Mgroup4Parser;
use mgroup4_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut args = std::env::args().skip(1);
    let data_path = args.next().expect("usage: time_parse <parserdata.pb> <input.txt> [iters]");
    let input_path = args.next().expect("usage: time_parse <parserdata.pb> <input.txt> [iters]");
    let iters: usize = args.next().map(|s| s.parse().unwrap()).unwrap_or(3);

    let load_start = Instant::now();
    let data_bytes = std::fs::read(&data_path)?;
    let data = Mgroup3ParserData::decode(data_bytes.as_slice())?;
    let parser = Mgroup4Parser::new(data);
    println!("load+prepare: {:?}", load_start.elapsed());

    let input = std::fs::read_to_string(&input_path)?;
    println!("input: {} chars", input.chars().count());

    for i in 0..iters {
        let t0 = Instant::now();
        let ctx = match parser.parse(&input) {
            Ok(ctx) => ctx,
            Err(e) => {
                println!("iter {}: PARSE ERROR: {:?}", i, e);
                return Ok(());
            }
        };
        let t_parse = t0.elapsed();

        let t1 = Instant::now();
        let accepted = parser.is_accepted(&ctx);
        let t_accept = t1.elapsed();

        let t2 = Instant::now();
        let hist = parser.kernels_history(&ctx);
        let t_hist = t2.elapsed();

        let total_kernels: usize = hist.iter().map(|ks| ks.len()).sum();
        println!(
            "iter {}: parse={:?} is_accepted={:?} (accepted={}) kernels_history={:?} (gens={}, kernels={})",
            i, t_parse, t_accept, accepted, t_hist, hist.len(), total_kernels
        );
    }
    Ok(())
}
