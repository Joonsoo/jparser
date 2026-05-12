//! AcceptCondition: port of `mgroup3/parser/kotlin/.../AcceptCondition.kt`.
//!
//! This module is built up incrementally. The enum itself + `Display` ship first.
//! Builders (`And::from`/`Or::from`), canonical sort, `neg`, `evolve`, and the
//! cached fields on composites are added in subsequent steps.

pub mod build;
pub mod canonical;
pub mod display;
pub mod eval;
pub mod neg;
pub mod parse;
pub mod root_set;

pub use root_set::RootSet;

/// One-of accept condition. Mirrors the Kotlin sealed hierarchy.
///
/// Construction notes:
/// - For And/Or, prefer the builder functions in `build.rs` once they exist —
///   they apply canonicalization (short-circuit, flatten, dedup, sort).
///   Direct construction is fine for tests and for parser-internal code that
///   has already canonicalized.
#[derive(Clone, PartialEq, Eq, Hash, Debug)]
pub enum AcceptCondition {
    Always,
    Never,
    NoLongerMatch { symbol_id: i32, start_gen: i32, from_next_gen: bool },
    NeedLongerMatch { symbol_id: i32, start_gen: i32, from_next_gen: bool },
    NotExists { symbol_id: i32, start_gen: i32 },
    Exists { symbol_id: i32, start_gen: i32 },
    Unless { symbol_id: i32, start_gen: i32 },
    OnlyIf { symbol_id: i32, start_gen: i32 },
    And { items: Vec<AcceptCondition> },
    Or { items: Vec<AcceptCondition> },
}

impl AcceptCondition {
    /// The set of `PathRoot`s referenced by leaves anywhere in this tree.
    /// Walked on demand (no cache). Composite trees should re-cache if this
    /// shows up in a profile.
    pub fn referenced_roots(&self) -> RootSet {
        root_set::referenced_roots(self)
    }
}
