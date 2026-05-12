//! Port of `mgroup3/parser/kotlin/.../ParsingCtx.kt`.
//!
//! Data types for the parsing state. No parser logic here — see
//! `mgroup3-native/src/parser/`.

use std::cell::OnceCell;
use std::collections::{HashMap, HashSet};
use std::hash::{Hash, Hasher};
use std::rc::Rc;

use crate::accept_condition::AcceptCondition;
use crate::path_root::PathRoot;

/// A parse-time kernel (symbol + pointer at a given gen).
#[derive(Copy, Clone, PartialEq, Eq, Hash, Debug)]
pub struct Kernel {
    pub symbol_id: i32,
    pub pointer: i32,
    pub gen_idx: i32,
}

impl Kernel {
    pub fn new(symbol_id: i32, pointer: i32, gen_idx: i32) -> Self {
        Self { symbol_id, pointer, gen_idx }
    }

    pub fn kernel_template(&self) -> KernelTemplatePair {
        KernelTemplatePair { symbol_id: self.symbol_id, pointer: self.pointer }
    }
}

/// (symbolId, pointer) — used as a key in the tip/mid edge action maps.
#[derive(Copy, Clone, PartialEq, Eq, Hash, Debug)]
pub struct KernelTemplatePair {
    pub symbol_id: i32,
    pub pointer: i32,
}

/// Finished kernel + the accept condition under which it was produced.
#[derive(Clone, Debug)]
pub struct FinishedKernelRecord {
    pub kernel: Kernel,
    pub condition: AcceptCondition,
}

/// Progressed kernel record. `start_gen`/`mid_gen`/`end_gen` track the three
/// boundaries of the progression interval.
#[derive(Copy, Clone, PartialEq, Eq, Hash, Debug)]
pub struct ProgressedKernelRecord {
    pub symbol_id: i32,
    pub pointer: i32,
    pub start_gen: i32,
    pub mid_gen: i32,
    pub end_gen: i32,
}

/// Linked list of milestones forming a parser graph path. Each node is shared
/// via `Rc` so multiple paths can point at a common ancestor chain.
///
/// `Hash`/`Eq` are computed structurally over (gen, milestone,
/// observing_cond_symbol_ids, parent). The hash is cached lazily.
#[derive(Debug)]
pub struct MilestonePath {
    pub gen_idx: i32,
    pub milestone: Kernel,
    pub parent: Option<Rc<MilestonePath>>,
    /// Cond symbol IDs observed at this edge. Shared `Rc<[i32]>` so adding/copying
    /// paths doesn't reallocate.
    pub observing_cond_symbol_ids: Rc<[i32]>,
    cached_hash: OnceCell<u64>,
}

impl MilestonePath {
    pub fn new(
        gen_idx: i32,
        milestone: Kernel,
        parent: Option<Rc<MilestonePath>>,
        observing_cond_symbol_ids: Rc<[i32]>,
    ) -> Self {
        Self { gen_idx, milestone, parent, observing_cond_symbol_ids, cached_hash: OnceCell::new() }
    }

    /// Copy this node with one field replaced. Mirrors Kotlin's `copy(...)`.
    /// Used by `applyEdgeAction` to swap `observing_cond_symbol_ids` while
    /// keeping the parent chain.
    pub fn with_observing(self: &Rc<MilestonePath>, observing: Rc<[i32]>) -> Rc<MilestonePath> {
        Rc::new(MilestonePath::new(self.gen_idx, self.milestone, self.parent.clone(), observing))
    }

    fn compute_hash(&self) -> u64 {
        use std::collections::hash_map::DefaultHasher;
        let mut h = DefaultHasher::new();
        self.gen_idx.hash(&mut h);
        self.milestone.hash(&mut h);
        // observingCondSymbolIds: deep
        for sid in self.observing_cond_symbol_ids.iter() {
            sid.hash(&mut h);
        }
        // parent: include either the cached hash of the parent (recursive) or 0
        match &self.parent {
            Some(p) => p.cached_hash_value().hash(&mut h),
            None => 0u64.hash(&mut h),
        }
        h.finish()
    }

    fn cached_hash_value(&self) -> u64 {
        *self.cached_hash.get_or_init(|| self.compute_hash())
    }
}

impl Hash for MilestonePath {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.cached_hash_value().hash(state)
    }
}

impl PartialEq for MilestonePath {
    fn eq(&self, other: &Self) -> bool {
        if std::ptr::eq(self, other) {
            return true;
        }
        if self.gen_idx != other.gen_idx || self.milestone != other.milestone {
            return false;
        }
        if self.observing_cond_symbol_ids.len() != other.observing_cond_symbol_ids.len()
            || self
                .observing_cond_symbol_ids
                .iter()
                .zip(other.observing_cond_symbol_ids.iter())
                .any(|(a, b)| a != b)
        {
            return false;
        }
        match (&self.parent, &other.parent) {
            (None, None) => true,
            (Some(a), Some(b)) => Rc::ptr_eq(a, b) || a == b,
            _ => false,
        }
    }
}

impl Eq for MilestonePath {}

/// Path shape = (milestone chain, tip group id). The "shape" of a path,
/// independent of its accept condition. Used as the key of `PathMap`.
#[derive(Debug, Clone)]
pub struct PathShape {
    pub milestone_path: Option<Rc<MilestonePath>>,
    pub tip_group_id: i32,
    cached_hash: OnceCell<u64>,
}

impl PathShape {
    pub fn new(milestone_path: Option<Rc<MilestonePath>>, tip_group_id: i32) -> Self {
        Self { milestone_path, tip_group_id, cached_hash: OnceCell::new() }
    }

    fn compute_hash(&self) -> u64 {
        use std::collections::hash_map::DefaultHasher;
        let mut h = DefaultHasher::new();
        match &self.milestone_path {
            Some(p) => p.hash(&mut h),
            None => 0u64.hash(&mut h),
        }
        self.tip_group_id.hash(&mut h);
        h.finish()
    }

    fn cached_hash_value(&self) -> u64 {
        *self.cached_hash.get_or_init(|| self.compute_hash())
    }
}

impl Hash for PathShape {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.cached_hash_value().hash(state)
    }
}

impl PartialEq for PathShape {
    fn eq(&self, other: &Self) -> bool {
        if self.tip_group_id != other.tip_group_id {
            return false;
        }
        match (&self.milestone_path, &other.milestone_path) {
            (None, None) => true,
            (Some(a), Some(b)) => Rc::ptr_eq(a, b) || a == b,
            _ => false,
        }
    }
}

impl Eq for PathShape {}

/// PathShape → its accept condition.
pub type PathMap = HashMap<PathShape, AcceptCondition>;

/// One step's worth of recorded actions.
#[derive(Clone, Debug, Default)]
pub struct HistoryEntry {
    pub finished_kernels: Vec<FinishedKernelRecord>,
    pub progressed_kernels: Vec<ProgressedKernelRecord>,
    pub cond_path_finishes: HashMap<PathRoot, AcceptCondition>,
    pub active_cond_paths: HashSet<PathRoot>,
}

/// Top-level parser state.
#[derive(Clone, Debug)]
pub struct ParsingCtx {
    pub gen_idx: i32,
    pub line: i32,
    pub col: i32,
    pub main_root: PathRoot,
    /// All live paths — main path plus every active cond root.
    pub paths: HashMap<PathRoot, PathMap>,
    pub history: Vec<HistoryEntry>,
    /// Union of every `active_cond_paths` seen so far. Carried forward so
    /// `parseStep` can skip re-registering dead cond roots.
    pub ever_seen_cond_roots: HashSet<PathRoot>,
}

impl ParsingCtx {
    /// View of just the main path (or an empty borrow if main is gone).
    pub fn main_paths(&self) -> Option<&PathMap> {
        self.paths.get(&self.main_root)
    }

    /// Iterate cond-only roots (every key except `main_root`).
    pub fn cond_paths(&self) -> impl Iterator<Item = (&PathRoot, &PathMap)> {
        let main = self.main_root;
        self.paths.iter().filter(move |(r, _)| **r != main)
    }
}

/// Rust analog of `com.giyeok.jparser.ktlib.Kernel`. Used by `kernels_history`
/// output. Distinct from the in-graph `Kernel` (no `endGen` there).
#[derive(Copy, Clone, PartialEq, Eq, Hash, Debug, PartialOrd, Ord)]
pub struct KtlibKernel {
    pub symbol_id: i32,
    pub pointer: i32,
    pub begin_gen: i32,
    pub end_gen: i32,
}

/// Add a path to a `PathMap`, Or-merging the condition if a shape already
/// exists. `Never` conditions are dropped. Mirrors
/// `MutableMap<PathShape, AcceptCondition>.addPath` in Kotlin
/// (`ParsingCtx.kt:140-144`).
pub fn add_path(map: &mut PathMap, shape: PathShape, cond: AcceptCondition) {
    if matches!(cond, AcceptCondition::Never) {
        return;
    }
    use std::collections::hash_map::Entry;
    match map.entry(shape) {
        Entry::Vacant(v) => {
            v.insert(cond);
        }
        Entry::Occupied(mut o) => {
            let existing = o.get().clone();
            o.insert(AcceptCondition::or_from([existing, cond]));
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn ex(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::Exists { symbol_id: s, start_gen: g }
    }

    fn mp_root(gen_idx: i32, sid: i32) -> Rc<MilestonePath> {
        Rc::new(MilestonePath::new(
            gen_idx,
            Kernel::new(sid, 0, gen_idx),
            None,
            Rc::from(Vec::<i32>::new()),
        ))
    }

    #[test]
    fn kernel_template() {
        let k = Kernel::new(1, 2, 3);
        assert_eq!(k.kernel_template(), KernelTemplatePair { symbol_id: 1, pointer: 2 });
    }

    #[test]
    fn milestone_path_eq_with_shared_parent() {
        let parent = mp_root(0, 100);
        let a = Rc::new(MilestonePath::new(
            1,
            Kernel::new(7, 0, 1),
            Some(parent.clone()),
            Rc::from(vec![1, 2]),
        ));
        let b = Rc::new(MilestonePath::new(
            1,
            Kernel::new(7, 0, 1),
            Some(parent.clone()),
            Rc::from(vec![1, 2]),
        ));
        assert_eq!(a, b);
        // Different observing list — not equal.
        let c = Rc::new(MilestonePath::new(
            1,
            Kernel::new(7, 0, 1),
            Some(parent),
            Rc::from(vec![1, 3]),
        ));
        assert_ne!(a, c);
    }

    #[test]
    fn milestone_path_hash_is_idempotent() {
        let p = mp_root(5, 9);
        let h1 = {
            use std::hash::{Hash, Hasher};
            let mut s = std::collections::hash_map::DefaultHasher::new();
            p.hash(&mut s);
            s.finish()
        };
        let h2 = {
            use std::hash::{Hash, Hasher};
            let mut s = std::collections::hash_map::DefaultHasher::new();
            p.hash(&mut s);
            s.finish()
        };
        assert_eq!(h1, h2);
    }

    #[test]
    fn path_shape_eq_and_hash() {
        let parent = mp_root(0, 100);
        let a = PathShape::new(Some(parent.clone()), 5);
        let b = PathShape::new(Some(parent.clone()), 5);
        let c = PathShape::new(Some(parent), 6);
        let d = PathShape::new(None, 5);
        assert_eq!(a, b);
        assert_ne!(a, c);
        assert_ne!(a, d);

        // hash equality matches structural equality
        use std::hash::{Hash, Hasher};
        let mut ha = std::collections::hash_map::DefaultHasher::new();
        let mut hb = std::collections::hash_map::DefaultHasher::new();
        a.hash(&mut ha);
        b.hash(&mut hb);
        assert_eq!(ha.finish(), hb.finish());
    }

    #[test]
    fn add_path_drops_never() {
        let mut m: PathMap = HashMap::new();
        add_path(&mut m, PathShape::new(None, 1), AcceptCondition::Never);
        assert!(m.is_empty());
    }

    #[test]
    fn add_path_or_merges() {
        let mut m: PathMap = HashMap::new();
        let shape = PathShape::new(None, 1);
        add_path(&mut m, shape.clone(), ex(1, 0));
        add_path(&mut m, shape.clone(), ex(2, 0));
        let v = m.get(&shape).unwrap();
        assert_eq!(*v, AcceptCondition::or_from([ex(1, 0), ex(2, 0)]));
    }

    #[test]
    fn parsing_ctx_main_paths_view() {
        let main_root = PathRoot::new(1, 0);
        let mut paths: HashMap<PathRoot, PathMap> = HashMap::new();
        let mut mp = PathMap::new();
        mp.insert(PathShape::new(None, 5), AcceptCondition::Always);
        paths.insert(main_root, mp);
        let ctx = ParsingCtx {
            gen_idx: 0,
            line: 0,
            col: 0,
            main_root,
            paths,
            history: vec![],
            ever_seen_cond_roots: HashSet::new(),
        };
        assert_eq!(ctx.main_paths().unwrap().len(), 1);
        assert_eq!(ctx.cond_paths().count(), 0);
    }
}
