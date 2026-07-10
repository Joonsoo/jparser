//! Diagnostic probe: inspect cond-symbol path roots and validate the structural
//! detection of "anychar single-char" cond symbols (EOF negation bodies) —
//! the inputs to `ParserDataPlain::eof_cond_symbols` (eager EOF resolution).
//! Usage: eofprobe <parserdata.pb> [symIds...]   (no symIds = all path roots)

use mgroup3_native::parser_data::ParserDataPlain;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let data = std::fs::read(&args[1]).expect("read pb");
    let proto = Mgroup3ParserData::decode(&data[..]).expect("decode");
    let plain = ParserDataPlain::from_proto(proto);

    let syms: Vec<i32> = args[2..].iter().map(|s| s.parse().unwrap()).collect();
    for (sym, info) in {
        let mut v: Vec<_> = plain.path_roots.iter().collect();
        v.sort_by_key(|(s, _)| **s);
        v
    } {
        if !syms.is_empty() && !syms.contains(sym) {
            continue;
        }
        let g = info.milestone_group_id;
        let actions = plain.term_actions.get(&g);
        println!(
            "sym{} group={} self_finish={:?} n_actions={}",
            sym,
            g,
            info.self_finish_accept_condition
                .as_ref()
                .map(|c| format!("{:?}", c.condition)),
            actions.map(|a| a.len()).unwrap_or(0)
        );
        if let Some(actions) = actions {
            for (i, tga) in actions.iter().enumerate() {
                let ta = &tga.term_action;
                println!(
                    "  action[{}] term_group={:?} n_rea={} n_rap={} rap_conds={:?}",
                    i,
                    tga.term_group,
                    ta.replace_and_appends.len(),
                    ta.replace_and_progresses.len(),
                    ta.replace_and_progresses
                        .iter()
                        .map(|r| format!("{:?}", r.accept_condition.condition))
                        .collect::<Vec<_>>()
                );
            }
        }
    }
}
