//! Mgroup3 parser. Port of `mgroup3/parser/kotlin/.../Mgroup4Parser.kt`.

pub mod core;
pub mod lazy_cache;
pub mod record_cond;
pub mod result;
pub mod template;

pub use core::Mgroup4Parser;
pub use lazy_cache::LazyMergeCache;
pub use result::{build_result, encode_parse_result};
pub use template::{build_condition, resolve_gen};

use crate::term_group::TermSet;

/// Parse-time error. Returned by `parse_step` and `parse`. Mirrors Kotlin's
/// `ParsingError` sealed class.
#[derive(Debug, Clone)]
pub enum ParsingError {
    UnexpectedInput { loc: i32, line: i32, col: i32, expected: TermSet, actual: char },
    UnexpectedEof { loc: i32, line: i32, col: i32, expected: TermSet },
}

impl std::fmt::Display for ParsingError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ParsingError::UnexpectedInput { loc, line, col, actual, .. } => {
                write!(
                    f,
                    "unexpected input {:?} at {}:{} (gen {})",
                    actual, line, col, loc
                )
            }
            ParsingError::UnexpectedEof { loc, line, col, .. } => {
                write!(f, "unexpected EOF at {}:{} (gen {})", line, col, loc)
            }
        }
    }
}

impl std::error::Error for ParsingError {}
