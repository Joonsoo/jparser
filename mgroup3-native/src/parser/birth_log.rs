//! Diagnostic instrumentation: cond-root birth logging. Built for the phantom
//! block-comment investigation (mulang docs/parser_phantom_block_comment.md §6)
//! and kept as a general path-genealogy probe — pathological shape growth is
//! most easily diagnosed by watching which action attaches which group where.
//!
//! Enable with `MG3_BIRTH_LOG=<fromGen>:<toGen>` — logs, for steps whose
//! next_gen falls in the range, every term/edge action append (with the parent
//! shape's milestone chain + tip group), every cond-root-starter pend, and
//! every actual cond root start in step 1b / step 3. Off by default; the
//! hot-path cost is one cached `OnceLock` read per append (same pattern as
//! `MG3_RECORD_COND_DIFF`).

use std::sync::OnceLock;

use crate::parsing_ctx::PathShape;
use crate::path_root::PathRoot;

pub fn birth_log_range() -> Option<(i32, i32)> {
    static RANGE: OnceLock<Option<(i32, i32)>> = OnceLock::new();
    *RANGE.get_or_init(|| {
        let v = std::env::var("MG3_BIRTH_LOG").ok()?;
        let (a, b) = v.split_once(':')?;
        Some((a.parse().ok()?, b.parse().ok()?))
    })
}

#[inline]
pub fn in_range(g: i32) -> bool {
    matches!(birth_log_range(), Some((a, b)) if g >= a && g <= b)
}

pub fn fmt_root(root: &PathRoot) -> String {
    format!("sym{}@{}", root.symbol_id, root.start_gen)
}

/// Milestone chain root→tip as `sym.ptr@gen`, then the tip group id.
pub fn fmt_shape(shape: &PathShape) -> String {
    let mut parts: Vec<String> = Vec::new();
    let mut mp = shape.milestone_path.clone();
    while let Some(node) = mp {
        parts.push(format!(
            "{}.{}@{}",
            node.milestone.symbol_id, node.milestone.pointer, node.gen_idx
        ));
        mp = node.parent.clone();
    }
    parts.reverse();
    format!("[{}]g{}", parts.join(" "), shape.tip_group_id)
}
