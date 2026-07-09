//! Dump NGrammar symbol definitions from a parserdata.pb by id.
//! Usage: symdump <parserdata.pb> <symId> [<symId> ...]

use mgroup4_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut args = std::env::args().skip(1);
    let data_path = args
        .next()
        .expect("usage: symdump <parserdata.pb> <symId>...");
    let ids: Vec<i32> = args.map(|s| s.parse().unwrap()).collect();
    let data = Mgroup3ParserData::decode(std::fs::read(&data_path)?.as_slice())?;
    let grammar = data.grammar.expect("no grammar in parserdata");
    for id in ids {
        if let Some(sym) = grammar.symbols.get(&id) {
            let dbg = format!("{:?}", sym);
            println!("sym{}: {}", id, &dbg[..dbg.len().min(400)]);
        } else if let Some(seq) = grammar.sequences.get(&id) {
            println!("sym{} (seq): {:?}", id, seq);
        } else {
            println!("sym{}: NOT FOUND", id);
        }
    }
    Ok(())
}
