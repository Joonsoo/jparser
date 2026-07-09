//! One-off: break down which fields of `ParserDataPlain` dominate the rkyv
//! archive size. Serializes each top-level field in isolation (rkyv `to_bytes`
//! over the borrowed field) and prints the byte cost of each, plus the whole.
//!
//! Usage: archive_breakdown <parserdata.pb[.gz]>

use std::io::Read;

use mgroup3_native::parser_data::ParserDataPlain;
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;
use rkyv::rancor;

fn load_plain_via_proto(path: &str) -> ParserDataPlain {
    let raw = std::fs::read(path).unwrap();
    let pb_bytes = if path.ends_with(".gz") {
        let mut buf = Vec::new();
        flate2::read::GzDecoder::new(raw.as_slice())
            .read_to_end(&mut buf)
            .unwrap();
        buf
    } else {
        raw
    };
    let data = Mgroup3ParserData::decode(pb_bytes.as_slice()).unwrap();
    ParserDataPlain::from_proto(data)
}

fn sz<T>(label: &str, v: &T)
where
    T: for<'a> rkyv::Serialize<
        rkyv::api::high::HighSerializer<
            rkyv::util::AlignedVec,
            rkyv::ser::allocator::ArenaHandle<'a>,
            rancor::Error,
        >,
    >,
{
    let bytes = rkyv::to_bytes::<rancor::Error>(v).unwrap();
    println!("{:<40} {:>13} bytes  ({:>7.2} MB)", label, bytes.len(), bytes.len() as f64 / 1e6);
}

fn main() {
    let path = std::env::args().nth(1).expect("usage: archive_breakdown <parserdata.pb[.gz]>");
    let p = load_plain_via_proto(&path);

    println!("=== whole ParserDataPlain ===");
    let whole = rkyv::to_bytes::<rancor::Error>(&p).unwrap();
    println!("{:<40} {:>13} bytes  ({:>7.2} MB)", "ParserDataPlain (whole)", whole.len(), whole.len() as f64 / 1e6);
    println!();

    println!("=== per top-level field (serialized in isolation) ===");
    sz("path_roots", &p.path_roots);
    sz("milestone_groups", &p.milestone_groups);
    sz("term_actions", &p.term_actions);
    sz("tip_edge_actions", &p.tip_edge_actions);
    sz("mid_edge_actions", &p.mid_edge_actions);
    sz("transitive_initial_cond_symbols", &p.transitive_initial_cond_symbols);
    sz("lookahead_cond_symbols", &p.lookahead_cond_symbols);
    println!();

    println!("=== collection element counts ===");
    println!("path_roots entries                       {}", p.path_roots.len());
    println!("milestone_groups entries                 {}", p.milestone_groups.len());
    println!("term_actions entries (map)               {}", p.term_actions.len());
    let term_actions_total: usize = p.term_actions.values().map(|v| v.len()).sum();
    println!("term_actions total actions               {}", term_actions_total);
    println!("tip_edge_actions                         {}", p.tip_edge_actions.len());
    println!("mid_edge_actions                         {}", p.mid_edge_actions.len());
    println!("transitive_initial_cond_symbols entries  {}", p.transitive_initial_cond_symbols.len());
    let tics_total: usize = p.transitive_initial_cond_symbols.values().map(|s| s.len()).sum();
    println!("transitive_initial_cond_symbols set sum  {}", tics_total);
    println!("lookahead_cond_symbols                   {}", p.lookahead_cond_symbols.len());
    println!();

    // ---- deeper: split edge actions into parsing_actions (progressed/finished/
    // added kernel templates) vs the append-milestone-group payload (AcceptCondition
    // trees + cond_root_starters). `added` is reporting-only per parser_data.rs. ----
    println!("=== deep dive: edge actions component sizes ===");
    let mut all_edge_actions: Vec<&mgroup3_native::parser_data::EdgeActionPlain> = Vec::new();
    for p2 in &p.tip_edge_actions {
        all_edge_actions.push(&p2.edge_action);
    }
    for p2 in &p.mid_edge_actions {
        all_edge_actions.push(&p2.edge_action);
    }

    // Collect the parsing_actions components across all edge actions.
    let mut progressed: Vec<_> = Vec::new();
    let mut finished: Vec<_> = Vec::new();
    let mut added: Vec<_> = Vec::new();
    let mut appends: Vec<_> = Vec::new();
    let mut start_progress: Vec<_> = Vec::new();
    for ea in &all_edge_actions {
        if let Some(pa) = &ea.parsing_actions {
            for x in &pa.progressed { progressed.push(x.clone()); }
            for x in &pa.finished { finished.push(x.clone()); }
            for x in &pa.added { added.push(x.clone()); }
        }
        for amg in &ea.append_milestone_groups { appends.push(amg.milestone_group_id); let _ = amg; }
        if let Some(sp) = &ea.start_node_progress { start_progress.push(sp.clone()); }
    }
    sz("edge.parsing_actions.progressed (all)", &progressed);
    sz("edge.parsing_actions.finished (all)", &finished);
    sz("edge.parsing_actions.added (all, report-only)", &added);
    sz("edge.start_node_progress (all)", &start_progress);
    println!("edge parsing_actions counts: progressed={} finished={} added={}",
        progressed.len(), finished.len(), added.len());
    println!("edge append_milestone_groups total: {}", appends.len());
    println!();

    // term_actions deep dive
    println!("=== deep dive: term actions component sizes ===");
    let mut t_progressed: Vec<_> = Vec::new();
    let mut t_finished: Vec<_> = Vec::new();
    let mut t_added: Vec<_> = Vec::new();
    let mut t_appends_count = 0usize;
    let mut t_raa_count = 0usize;
    let mut t_rap_count = 0usize;
    for actions in p.term_actions.values() {
        for tga in actions {
            let ta = &tga.term_action;
            if let Some(pa) = &ta.parsing_actions {
                for x in &pa.progressed { t_progressed.push(x.clone()); }
                for x in &pa.finished { t_finished.push(x.clone()); }
                for x in &pa.added { t_added.push(x.clone()); }
            }
            t_raa_count += ta.replace_and_appends.len();
            t_rap_count += ta.replace_and_progresses.len();
            for raa in &ta.replace_and_appends {
                let _ = &raa.append.milestone_group_id;
                t_appends_count += 1;
            }
        }
    }
    sz("term.parsing_actions.progressed (all)", &t_progressed);
    sz("term.parsing_actions.finished (all)", &t_finished);
    sz("term.parsing_actions.added (all, report-only)", &t_added);
    println!("term parsing_actions counts: progressed={} finished={} added={}",
        t_progressed.len(), t_finished.len(), t_added.len());
    println!("term replace_and_appends={} replace_and_progresses={} appends={}",
        t_raa_count, t_rap_count, t_appends_count);
}
