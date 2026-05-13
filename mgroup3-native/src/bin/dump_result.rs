//! Standalone CLI binary used by the JVM bridge differential test
//! (`Mgroup3NativeResultBridgeTest.kt`). Reads a `Mgroup3ParserData` proto and
//! an input text file, runs `Mgroup3Parser`, encodes the outcome via
//! `encode_parse_result`, and writes the bytes to stdout.
//!
//! Usage:
//!   dump_result <parserdata.pb> <input.txt>
//!
//! The binary is also handy on its own for inspecting what the native parser
//! emits for a given grammar + input. `cargo run --bin dump_result -- ...`.

use std::io::{Read, Write};
use std::process::ExitCode;

use mgroup3_native::parser::{encode_parse_result, Mgroup3Parser};
use mgroup3_native::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use prost::Message;

fn main() -> ExitCode {
    let mut args = std::env::args().skip(1);
    let Some(data_path) = args.next() else {
        eprintln!("usage: dump_result <parserdata.pb> <input.txt>");
        return ExitCode::from(2);
    };
    let Some(input_path) = args.next() else {
        eprintln!("usage: dump_result <parserdata.pb> <input.txt>");
        return ExitCode::from(2);
    };

    let data_bytes = match std::fs::read(&data_path) {
        Ok(b) => b,
        Err(e) => {
            eprintln!("read {}: {}", data_path, e);
            return ExitCode::from(1);
        }
    };
    let data = match Mgroup3ParserData::decode(data_bytes.as_slice()) {
        Ok(d) => d,
        Err(e) => {
            eprintln!("decode {}: {}", data_path, e);
            return ExitCode::from(1);
        }
    };

    let mut input = String::new();
    if input_path == "-" {
        if let Err(e) = std::io::stdin().read_to_string(&mut input) {
            eprintln!("read stdin: {}", e);
            return ExitCode::from(1);
        }
    } else {
        match std::fs::read_to_string(&input_path) {
            Ok(s) => input = s,
            Err(e) => {
                eprintln!("read {}: {}", input_path, e);
                return ExitCode::from(1);
            }
        }
    }

    let parser = Mgroup3Parser::new(data);
    let outcome = parser.parse(&input);
    let bytes = encode_parse_result(&parser, outcome.as_ref().map_err(|e| e));
    if let Err(e) = std::io::stdout().write_all(&bytes) {
        eprintln!("write stdout: {}", e);
        return ExitCode::from(1);
    }
    ExitCode::SUCCESS
}
