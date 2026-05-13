//! Differential test: run the same AcceptCondition operations on the Kotlin
//! reference (capturing into the checked-in fixture) and on the Rust port
//! (executed here). Asserts semantic enum equality on the results.
//!
//! Fixture format and corpus are produced by
//! `mgroup3/test/kotlin/.../AcceptConditionFixtureGenTest.kt`. To regenerate:
//! `/Users/joonsoo/Documents/apps/bibix4/bibix4 runMgroup3Test`.

use rustc_hash::{FxHashMap as HashMap, FxHashSet as HashSet};
use std::fs;

use mgroup3_native::accept_condition::eval::{evaluate_accept_condition, evolve_accept_condition};
use mgroup3_native::accept_condition::parse::{parse_condition, parse_path_root};
use mgroup3_native::accept_condition::AcceptCondition;
use mgroup3_native::path_root::PathRoot;

#[derive(Debug)]
struct Block {
    kind: String,
    fields: HashMap<String, String>,
    line_no: usize,
}

fn load_blocks() -> Vec<Block> {
    let path = "tests/fixtures/accept_condition.txt";
    let text = fs::read_to_string(path)
        .unwrap_or_else(|e| panic!("failed to read {}: {} (run runMgroup3Test to regenerate)", path, e));
    let mut blocks = Vec::new();
    let mut current_fields = HashMap::default();
    let mut current_kind: Option<String> = None;
    let mut block_start = 0usize;
    for (i, raw_line) in text.lines().enumerate() {
        let line = raw_line.trim_end();
        if line.starts_with('#') {
            continue;
        }
        if line.is_empty() {
            if let Some(kind) = current_kind.take() {
                blocks.push(Block { kind, fields: std::mem::take(&mut current_fields), line_no: block_start });
            }
            continue;
        }
        let (k, v) = match line.split_once('=') {
            Some((k, v)) => (k.to_string(), v.to_string()),
            None => panic!("line {} not in key=value form: {:?}", i + 1, line),
        };
        if k == "kind" {
            if current_kind.is_some() {
                panic!("line {}: new 'kind=' before previous block ended (missing blank line?)", i + 1);
            }
            current_kind = Some(v);
            block_start = i + 1;
        } else {
            current_fields.insert(k, v);
        }
    }
    if let Some(kind) = current_kind {
        blocks.push(Block { kind, fields: current_fields, line_no: block_start });
    }
    blocks
}

fn split_pipe(s: &str) -> Vec<&str> {
    if s.is_empty() {
        Vec::new()
    } else {
        s.split('|').collect()
    }
}

fn parse_fins(block: &Block) -> HashMap<PathRoot, AcceptCondition> {
    let keys = block.fields.get("finsKeys").map(|s| s.as_str()).unwrap_or("");
    let values = block.fields.get("finsValues").map(|s| s.as_str()).unwrap_or("");
    let ks = split_pipe(keys);
    let vs = split_pipe(values);
    assert_eq!(
        ks.len(),
        vs.len(),
        "block @ line {}: finsKeys ({}) and finsValues ({}) length mismatch",
        block.line_no,
        ks.len(),
        vs.len()
    );
    let mut out = HashMap::default();
    for (k, v) in ks.into_iter().zip(vs.into_iter()) {
        let root = parse_path_root(k).unwrap_or_else(|e| panic!("block @ line {}: bad PathRoot {:?}: {}", block.line_no, k, e));
        let cond = parse_condition(v).unwrap_or_else(|e| panic!("block @ line {}: bad AcceptCondition {:?}: {}", block.line_no, v, e));
        out.insert(root, cond);
    }
    out
}

fn parse_active(block: &Block) -> HashSet<PathRoot> {
    let s = block.fields.get("active").map(|s| s.as_str()).unwrap_or("");
    split_pipe(s)
        .into_iter()
        .map(|k| parse_path_root(k).unwrap_or_else(|e| panic!("block @ line {}: bad PathRoot in active: {}", block.line_no, e)))
        .collect()
}

fn get_field<'a>(block: &'a Block, name: &str) -> &'a str {
    block
        .fields
        .get(name)
        .unwrap_or_else(|| panic!("block @ line {}: missing field {}", block.line_no, name))
        .as_str()
}

fn parse_cond_field(block: &Block, name: &str) -> AcceptCondition {
    let s = get_field(block, name);
    parse_condition(s).unwrap_or_else(|e| panic!("block @ line {}: bad {} = {:?}: {}", block.line_no, name, s, e))
}

#[test]
fn diff_against_kotlin_fixture() {
    let blocks = load_blocks();
    assert!(blocks.len() > 50, "fixture too small ({} blocks)", blocks.len());
    let mut counts: HashMap<String, usize> = HashMap::default();

    for (idx, block) in blocks.iter().enumerate() {
        *counts.entry(block.kind.clone()).or_default() += 1;
        let header = format!("block #{} (kind={}, line={})", idx + 1, block.kind, block.line_no);
        match block.kind.as_str() {
            "display" => {
                let cond = parse_cond_field(block, "cond");
                let expected = parse_cond_field(block, "expected");
                assert_eq!(cond, expected, "{}: display roundtrip mismatch", header);
            }
            "neg" => {
                let cond = parse_cond_field(block, "cond");
                let expected = parse_cond_field(block, "expected");
                assert_eq!(cond.neg(), expected, "{}: neg mismatch", header);
            }
            "and_from" => {
                let inputs_raw = get_field(block, "inputs");
                let inputs: Vec<AcceptCondition> = split_pipe(inputs_raw)
                    .into_iter()
                    .map(|s| parse_condition(s).unwrap())
                    .collect();
                let expected = parse_cond_field(block, "expected");
                assert_eq!(AcceptCondition::and_from(inputs), expected, "{}: and_from mismatch", header);
            }
            "or_from" => {
                let inputs_raw = get_field(block, "inputs");
                let inputs: Vec<AcceptCondition> = split_pipe(inputs_raw)
                    .into_iter()
                    .map(|s| parse_condition(s).unwrap())
                    .collect();
                let expected = parse_cond_field(block, "expected");
                assert_eq!(AcceptCondition::or_from(inputs), expected, "{}: or_from mismatch", header);
            }
            "evaluate" => {
                let cond = parse_cond_field(block, "cond");
                let fins = parse_fins(block);
                let active = parse_active(block);
                let expected: bool = get_field(block, "expected")
                    .parse()
                    .unwrap_or_else(|e| panic!("{}: expected bool: {}", header, e));
                assert_eq!(
                    evaluate_accept_condition(&cond, &fins, &active),
                    expected,
                    "{}: evaluate mismatch (cond={})",
                    header,
                    cond
                );
            }
            "evolve" => {
                let cond = parse_cond_field(block, "cond");
                let fins = parse_fins(block);
                let active = parse_active(block);
                let gen_step: i32 = get_field(block, "gen").parse().unwrap();
                let expected = parse_cond_field(block, "expected");
                let actual = evolve_accept_condition(&cond, &fins, &active, gen_step);
                assert_eq!(
                    actual, expected,
                    "{}: evolve mismatch\n  cond={}\n  expected={}\n  actual={}",
                    header, cond, expected, actual
                );
            }
            other => panic!("{}: unknown block kind {:?}", header, other),
        }
    }

    println!("diff_against_kotlin_fixture: {} blocks", blocks.len());
    for (k, v) in &counts {
        println!("  {}: {}", k, v);
    }
}
