//! Mgroup3 parser. Port of `mgroup3/parser/kotlin/.../Mgroup3Parser.kt`.

pub mod core;
pub mod eval_with_history;
pub mod template;

pub use core::Mgroup3Parser;
pub use eval_with_history::{collect_finishes_at_or_after, evaluate_with_history};
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
