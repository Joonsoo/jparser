use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;
use std::fs::File;
use std::io::Read;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let path = std::env::args().nth(1).expect("usage: sanity <parserdata.pb>");
    let mut buf = Vec::new();
    File::open(&path)?.read_to_end(&mut buf)?;
    let data = Mgroup3ParserData::decode(buf.as_slice())?;
    println!("start_symbol_id:  {}", data.start_symbol_id);
    println!("path_roots:       {}", data.path_roots.len());
    println!("milestone_groups: {}", data.milestone_groups.len());
    println!("term_actions:     {}", data.term_actions.len());
    println!("tip_edge_actions: {}", data.tip_edge_actions.len());
    println!("mid_edge_actions: {}", data.mid_edge_actions.len());
    if let Some(grammar) = data.grammar.as_ref() {
        println!("grammar.symbols:  {}", grammar.symbols.len());
    } else {
        println!("grammar:          <none>");
    }
    Ok(())
}
