//! `Mgroup3Parser` struct and the parts of its public surface that don't need
//! `parseStep` yet: construction, `init_ctx`, `cond_paths_for`,
//! `find_applicable_action`, `expected_inputs_of`. `parseStep` and the recursive
//! helpers land in Step 3.6.

use std::cell::RefCell;
use std::collections::{HashMap, VecDeque};
use std::rc::Rc;

use crate::accept_condition::AcceptCondition;
use crate::parser::template::{build_condition, resolve_gen_i32};
use crate::parser_data::{EdgeActionPlain, ParserDataPlain, TermActionPlain};
use crate::parsing_ctx::{
    FinishedKernelRecord, HistoryEntry, Kernel, KernelTemplatePair, ParsingCtx, PathMap,
    PathShape, ProgressedKernelRecord,
};
use crate::path_root::PathRoot;
use crate::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use crate::term_group::{is_match, TermGroupBuilder, TermSet};

use super::ParsingError;

pub struct Mgroup3Parser {
    plain: ParserDataPlain,
    /// (parent kernel template, tip group id) → tip edge action.
    tip_edge_actions: HashMap<(KernelTemplatePair, i32), Rc<EdgeActionPlain>>,
    /// (parent kernel template, tip kernel template) → mid edge action.
    mid_edge_actions: HashMap<(KernelTemplatePair, KernelTemplatePair), Rc<EdgeActionPlain>>,
    /// (tipGroupId << 32) | charCode → term action lookup result.
    /// `RefCell` because parse-time we mutate from `&self`. Single-threaded.
    term_action_cache: RefCell<HashMap<i64, Option<Rc<TermActionPlain>>>>,
}

impl Mgroup3Parser {
    pub fn new(data: Mgroup3ParserData) -> Self {
        let plain = ParserDataPlain::from_proto(data);
        let tip_edge_actions = plain
            .tip_edge_actions
            .iter()
            .map(|p| {
                let key = (
                    KernelTemplatePair {
                        symbol_id: p.parent.symbol_id,
                        pointer: p.parent.pointer,
                    },
                    p.tip_group_id,
                );
                (key, Rc::clone(&p.edge_action))
            })
            .collect();
        let mid_edge_actions = plain
            .mid_edge_actions
            .iter()
            .map(|p| {
                let key = (
                    KernelTemplatePair {
                        symbol_id: p.parent.symbol_id,
                        pointer: p.parent.pointer,
                    },
                    KernelTemplatePair {
                        symbol_id: p.tip.symbol_id,
                        pointer: p.tip.pointer,
                    },
                );
                (key, Rc::clone(&p.edge_action))
            })
            .collect();
        Self {
            plain,
            tip_edge_actions,
            mid_edge_actions,
            term_action_cache: RefCell::new(HashMap::new()),
        }
    }

    pub fn plain(&self) -> &ParserDataPlain {
        &self.plain
    }

    pub fn start_symbol_id(&self) -> i32 {
        self.plain.start_symbol_id
    }

    #[allow(dead_code)] // used in Step 3.6
    pub(crate) fn tip_edge_action(
        &self,
        parent: KernelTemplatePair,
        tip_group_id: i32,
    ) -> Option<Rc<EdgeActionPlain>> {
        self.tip_edge_actions.get(&(parent, tip_group_id)).cloned()
    }

    #[allow(dead_code)] // used in Step 3.6
    pub(crate) fn mid_edge_action(
        &self,
        parent: KernelTemplatePair,
        tip: KernelTemplatePair,
    ) -> Option<Rc<EdgeActionPlain>> {
        self.mid_edge_actions.get(&(parent, tip)).cloned()
    }

    /// Initialize a parsing context at the configured start symbol.
    pub fn init_ctx(&self) -> ParsingCtx {
        self.init_ctx_with_start(self.plain.start_symbol_id)
    }

    /// Initialize a parsing context starting from `start_symbol_id`.
    /// Mirrors `Mgroup3Parser.kt:88-127`.
    pub fn init_ctx_with_start(&self, start_symbol_id: i32) -> ParsingCtx {
        let root_info = self
            .plain
            .path_roots
            .get(&start_symbol_id)
            .unwrap_or_else(|| panic!("No path root for symbol {}", start_symbol_id));

        let main_root = PathRoot::new(start_symbol_id, 0);
        let mut main_paths: PathMap = PathMap::new();
        main_paths.insert(PathShape::new(None, root_info.milestone_group_id), AcceptCondition::Always);

        let initial_cond_paths = self.cond_paths_for(&root_info.initial_cond_symbol_ids, 0);

        let mut all_paths: HashMap<PathRoot, PathMap> = HashMap::new();
        all_paths.insert(main_root, main_paths);
        for (k, v) in initial_cond_paths {
            all_paths.insert(k, v);
        }

        let mut initial_finished: Vec<FinishedKernelRecord> = Vec::new();
        let mut initial_progressed: Vec<ProgressedKernelRecord> = Vec::new();
        if let Some(pa) = &root_info.parsing_actions {
            for finished in &pa.finished {
                let start_gen = resolve_gen_i32(finished.start_gen, 0, 0, 0, 0);
                initial_finished.push(FinishedKernelRecord {
                    kernel: Kernel::new(finished.symbol_id, finished.pointer, start_gen),
                    condition: build_condition(
                        finished
                            .finish_condition
                            .as_ref()
                            .expect("FinishedKernelTemplate.finish_condition missing"),
                        0,
                        0,
                        0,
                        0,
                    ),
                });
            }
            for prog in &pa.progressed {
                let start_gen = resolve_gen_i32(prog.start_gen, 0, 0, 0, 0);
                let mid_gen = resolve_gen_i32(prog.mid_gen, 0, 0, 0, 0);
                initial_progressed.push(ProgressedKernelRecord {
                    symbol_id: prog.symbol_id,
                    pointer: prog.pointer,
                    start_gen,
                    mid_gen,
                    end_gen: 0,
                });
            }
        }
        if let Some(self_finish_tpl) = &root_info.self_finish_accept_condition {
            initial_finished.push(FinishedKernelRecord {
                kernel: Kernel::new(start_symbol_id, 1, 0),
                condition: build_condition(self_finish_tpl, 0, 0, 0, 0),
            });
        }

        let initial_entry = HistoryEntry {
            finished_kernels: initial_finished,
            progressed_kernels: initial_progressed,
            cond_path_finishes: HashMap::new(),
            active_cond_paths: Default::default(),
        };

        ParsingCtx {
            gen_idx: 0,
            line: 0,
            col: 0,
            main_root,
            paths: all_paths,
            history: vec![initial_entry],
            ever_seen_cond_roots: Default::default(),
        }
    }

    /// Build initial path maps for every symbol in the transitive closure of
    /// `cond_symbol_ids`. Mirrors `Mgroup3Parser.kt:72-86`. Each created path
    /// has a single shape `(None, milestoneGroupId)` with `Always` condition.
    pub fn cond_paths_for(
        &self,
        cond_symbol_ids: &[i32],
        gen_idx: i32,
    ) -> HashMap<PathRoot, PathMap> {
        let mut builder: HashMap<i32, PathShape> = HashMap::new();
        let mut queue: VecDeque<i32> = VecDeque::new();
        queue.extend(cond_symbol_ids.iter().copied());
        while let Some(sym_id) = queue.pop_front() {
            if builder.contains_key(&sym_id) {
                continue;
            }
            let Some(root_info) = self.plain.path_roots.get(&sym_id) else { continue };
            builder.insert(sym_id, PathShape::new(None, root_info.milestone_group_id));
            queue.extend(root_info.initial_cond_symbol_ids.iter().copied());
        }
        builder
            .into_iter()
            .map(|(sym_id, shape)| {
                let mut pm = PathMap::new();
                pm.insert(shape, AcceptCondition::Always);
                (PathRoot::new(sym_id, gen_idx), pm)
            })
            .collect()
    }

    /// Look up a term action for `(shape.tip_group_id, input)`. Cached.
    /// Mirrors `Mgroup3Parser.kt:164-173`.
    pub fn find_applicable_action(
        &self,
        shape: &PathShape,
        input: char,
    ) -> Option<Rc<TermActionPlain>> {
        let key = ((shape.tip_group_id as i64) << 32) | (input as u32 as i64);
        {
            let cache = self.term_action_cache.borrow();
            if let Some(v) = cache.get(&key) {
                return v.clone();
            }
        }
        let result = self.plain.term_actions.get(&shape.tip_group_id).and_then(|actions| {
            actions
                .iter()
                .find(|action| is_match(&action.term_group, input))
                .map(|action| Rc::clone(&action.term_action))
        });
        self.term_action_cache.borrow_mut().insert(key, result.clone());
        result
    }

    /// Collect the term groups reachable from the main path's tips into a
    /// `TermSet`. Used in error messages. Mirrors `Mgroup3Parser.kt:151-162`.
    pub fn expected_inputs_of(&self, ctx: &ParsingCtx) -> TermSet {
        let mut builder = TermGroupBuilder::new();
        if let Some(main_map) = ctx.paths.get(&ctx.main_root) {
            for shape in main_map.keys() {
                if let Some(actions) = self.plain.term_actions.get(&shape.tip_group_id) {
                    for action in actions {
                        builder.add(action.term_group.clone());
                    }
                }
            }
        }
        builder.build()
    }

    /// Drop the term-action cache. Useful in benchmarks or when the same
    /// `Mgroup3Parser` is reused across very different inputs. Not on the hot
    /// path.
    pub fn clear_term_action_cache(&self) {
        self.term_action_cache.borrow_mut().clear();
    }

    /// Catch the (currently unused) ParsingError variants so `cargo test`
    /// doesn't flag them dead-code while Step 3.5 is in flight.
    #[doc(hidden)]
    #[allow(dead_code)]
    fn _force_parsing_error_used(e: ParsingError) {
        let _ = e;
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::proto::com::giyeok::jparser::mgroup3::proto::*;

    fn empty_data_with_start(start: i32) -> Mgroup3ParserData {
        let mut d = Mgroup3ParserData::default();
        d.start_symbol_id = start;
        // Need a path root for `start` or initCtx panics.
        d.path_roots.insert(
            start,
            PathRootInfo {
                symbol_id: start,
                milestone_group_id: 0,
                initial_cond_symbol_ids: vec![],
                self_finish_accept_condition: None,
                parsing_actions: None,
            },
        );
        d
    }

    #[test]
    fn init_ctx_basic_shape() {
        let parser = Mgroup3Parser::new(empty_data_with_start(7));
        let ctx = parser.init_ctx();
        assert_eq!(ctx.gen_idx, 0);
        assert_eq!(ctx.line, 0);
        assert_eq!(ctx.col, 0);
        assert_eq!(ctx.main_root, PathRoot::new(7, 0));
        assert_eq!(ctx.paths.len(), 1);
        assert_eq!(ctx.paths.get(&ctx.main_root).unwrap().len(), 1);
        assert_eq!(ctx.history.len(), 1);
        assert!(ctx.ever_seen_cond_roots.is_empty());
    }

    #[test]
    fn cond_paths_for_three_node_dag() {
        // 1 → 2 → 3, all roots present.
        let mut d = Mgroup3ParserData::default();
        d.start_symbol_id = 1;
        for (sid, init) in [(1, vec![2]), (2, vec![3]), (3, vec![])] {
            d.path_roots.insert(
                sid,
                PathRootInfo {
                    symbol_id: sid,
                    milestone_group_id: sid * 100,
                    initial_cond_symbol_ids: init,
                    self_finish_accept_condition: None,
                    parsing_actions: None,
                },
            );
        }
        let parser = Mgroup3Parser::new(d);
        let paths = parser.cond_paths_for(&[1], 5);
        // Closure {1, 2, 3} — each becomes a PathRoot at gen=5.
        assert_eq!(paths.len(), 3);
        for sid in [1, 2, 3] {
            let pr = PathRoot::new(sid, 5);
            let pm = paths.get(&pr).expect("missing root");
            assert_eq!(pm.len(), 1);
            // The shape uses the symbol's milestone_group_id.
            let (shape, cond) = pm.iter().next().unwrap();
            assert_eq!(shape.tip_group_id, sid * 100);
            assert_eq!(*cond, AcceptCondition::Always);
        }
    }

    #[test]
    fn cond_paths_for_skips_unknown_symbol() {
        let mut d = Mgroup3ParserData::default();
        d.path_roots.insert(
            1,
            PathRootInfo {
                symbol_id: 1,
                milestone_group_id: 100,
                initial_cond_symbol_ids: vec![999],
                self_finish_accept_condition: None,
                parsing_actions: None,
            },
        );
        d.start_symbol_id = 1;
        let parser = Mgroup3Parser::new(d);
        let paths = parser.cond_paths_for(&[1], 0);
        assert_eq!(paths.len(), 1); // 999 is unknown — skipped
    }

    #[test]
    fn expected_inputs_empty_when_no_actions() {
        let parser = Mgroup3Parser::new(empty_data_with_start(1));
        let ctx = parser.init_ctx();
        let ts = parser.expected_inputs_of(&ctx);
        assert!(ts.is_empty());
    }
}
