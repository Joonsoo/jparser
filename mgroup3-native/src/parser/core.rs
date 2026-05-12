//! `Mgroup3Parser` struct and the parts of its public surface that don't need
//! `parseStep` yet: construction, `init_ctx`, `cond_paths_for`,
//! `find_applicable_action`, `expected_inputs_of`. `parseStep` and the recursive
//! helpers land in Step 3.6.

use std::cell::RefCell;
use std::collections::{HashMap, HashSet, VecDeque};
use std::rc::Rc;

use crate::accept_condition::AcceptCondition;
use crate::parser::eval_with_history::evaluate_with_history;
use crate::parser::template::{build_condition, resolve_gen_i32};
use crate::parser_data::{EdgeActionPlain, ParserDataPlain, TermActionPlain};
use crate::parsing_ctx::{
    add_path, FinishedKernelRecord, HistoryEntry, Kernel, KernelTemplatePair, KtlibKernel,
    MilestonePath, ParsingCtx, PathMap, PathShape, ProgressedKernelRecord,
};
use crate::path_root::PathRoot;
use crate::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
use crate::accept_condition::eval::evolve_accept_condition;
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

    /// Drive one input character. Mirrors `Mgroup3Parser.kt:388-698`.
    pub fn parse_step(
        &self,
        ctx: ParsingCtx,
        input: char,
        is_last_input: bool,
    ) -> Result<ParsingCtx, ParsingError> {
        let main_paths_before = ctx.paths.get(&ctx.main_root).cloned().unwrap_or_default();
        if main_paths_before.is_empty() {
            let expected = self.expected_inputs_of(&ctx);
            return Err(ParsingError::UnexpectedInput {
                loc: ctx.gen_idx,
                line: ctx.line,
                col: ctx.col,
                expected,
                actual: input,
            });
        }
        let next_gen = ctx.gen_idx + 1;
        let (next_line, next_col) = if input == '\n' {
            (ctx.line + 1, 0)
        } else {
            (ctx.line, ctx.col + 1)
        };

        let mut next_paths: HashMap<PathRoot, PathMap> = HashMap::new();
        let mut finishes: Vec<FinishedKernelRecord> = Vec::new();
        let mut progresses: Vec<ProgressedKernelRecord> = Vec::new();
        let mut observing: HashSet<i32> = HashSet::new();
        let mut root_progresses: HashMap<PathRoot, AcceptCondition> = HashMap::new();
        let mut cond_root_starters_from_term: HashMap<PathRoot, i32> = HashMap::new();

        // ----- step 1+2: main and cond paths both run through applyTermAction -----
        for (root, path_map) in &ctx.paths {
            let is_main = *root == ctx.main_root;
            let mut per_root_next: PathMap = PathMap::new();
            for (shape, cond) in path_map {
                let ta = self.find_applicable_action(shape, input);
                if let Some(ta) = ta {
                    self.apply_term_action(
                        shape,
                        cond,
                        *root,
                        &ta,
                        ctx.gen_idx,
                        next_gen,
                        &mut per_root_next,
                        &mut finishes,
                        &mut progresses,
                        &mut root_progresses,
                        &mut observing,
                        &mut cond_root_starters_from_term,
                    );
                } else if !is_main {
                    // Dead cond path — check possible_finishes for self-finish.
                    if let Some(mg) = self.plain.milestone_groups.get(&shape.tip_group_id) {
                        for pf in &mg.possible_finishes {
                            if pf.symbol_id == root.symbol_id {
                                let prev_gen = shape
                                    .milestone_path
                                    .as_ref()
                                    .map(|mp| mp.gen_idx)
                                    .unwrap_or(root.start_gen);
                                let mid_gen_local = ctx.gen_idx;
                                let pf_cond = build_condition(
                                    &pf.accept_condition,
                                    prev_gen,
                                    mid_gen_local,
                                    next_gen,
                                    prev_gen,
                                );
                                let combined =
                                    AcceptCondition::and_from([cond.clone(), pf_cond]);
                                if !matches!(combined, AcceptCondition::Never) {
                                    or_merge(&mut root_progresses, *root, combined);
                                }
                            }
                        }
                    }
                }
            }
            if !per_root_next.is_empty() {
                next_paths.insert(*root, per_root_next);
            }
        }

        // ----- step 1b: cond root starters from main path's append actions -----
        for (&starter_root, &mgroup_id) in &cond_root_starters_from_term {
            if ctx.paths.contains_key(&starter_root) {
                continue;
            }
            if next_paths.contains_key(&starter_root) {
                continue;
            }
            if ctx.ever_seen_cond_roots.contains(&starter_root) {
                continue;
            }
            let Some(root_info) = self.plain.path_roots.get(&starter_root.symbol_id).cloned()
            else {
                continue;
            };
            let starter_shape = PathShape::new(None, mgroup_id);
            let ta = self.find_applicable_action(&starter_shape, input);
            if let Some(ta) = ta {
                let mut per_starter_next: PathMap = PathMap::new();
                let mut ignored_starters: HashMap<PathRoot, i32> = HashMap::new();
                self.apply_term_action(
                    &starter_shape,
                    &AcceptCondition::Always,
                    starter_root,
                    &ta,
                    ctx.gen_idx,
                    next_gen,
                    &mut per_starter_next,
                    &mut finishes,
                    &mut progresses,
                    &mut root_progresses,
                    &mut observing,
                    &mut ignored_starters,
                );
                if !per_starter_next.is_empty() {
                    let acc = next_paths.entry(starter_root).or_insert_with(PathMap::new);
                    for (s, c) in per_starter_next {
                        add_path(acc, s, c);
                    }
                }
            }
            if let Some(self_finish_tpl) = root_info.self_finish_accept_condition.as_ref() {
                let cond = build_condition(
                    self_finish_tpl,
                    starter_root.start_gen,
                    starter_root.start_gen,
                    next_gen,
                    starter_root.start_gen,
                );
                or_merge(&mut root_progresses, starter_root, cond);
            }
        }

        // ----- step 3: new cond paths from observing closure + condition.referenced_roots -----
        let mut all_observing: HashSet<i32> = HashSet::with_capacity(observing.len() * 2);
        for sym in &observing {
            if let Some(closure) = self.plain.transitive_initial_cond_symbols.get(sym) {
                all_observing.extend(closure.iter().copied());
            } else {
                all_observing.insert(*sym);
            }
        }

        let mut new_cond_roots: HashSet<PathRoot> = HashSet::new();
        for pm in next_paths.values() {
            for cond in pm.values() {
                cond.referenced_roots().for_each(|r| {
                    new_cond_roots.insert(*r);
                });
            }
        }
        for sym in &all_observing {
            new_cond_roots.insert(PathRoot::new(*sym, next_gen));
        }
        for &root in cond_root_starters_from_term.keys() {
            new_cond_roots.insert(root);
        }

        let mut new_cond_root_progresses: HashMap<PathRoot, AcceptCondition> = HashMap::new();
        // Iterate over a sorted-by-(sym,next_gen) copy to keep step3 deterministic
        // across HashSet iteration orders.
        let mut new_cond_roots_sorted: Vec<PathRoot> = new_cond_roots.into_iter().collect();
        new_cond_roots_sorted.sort_by_key(|r| (r.symbol_id, r.start_gen));
        for path_root in new_cond_roots_sorted {
            if path_root == ctx.main_root {
                continue;
            }
            if ctx.paths.contains_key(&path_root) || next_paths.contains_key(&path_root) {
                continue;
            }
            if ctx.ever_seen_cond_roots.contains(&path_root) {
                continue;
            }
            let Some(root_info) = self.plain.path_roots.get(&path_root.symbol_id).cloned() else {
                continue;
            };
            if let Some(tpl) = root_info.self_finish_accept_condition.as_ref() {
                let self_cond = build_condition(
                    tpl,
                    path_root.start_gen,
                    path_root.start_gen,
                    next_gen,
                    path_root.start_gen,
                );
                new_cond_root_progresses.insert(path_root, self_cond);
            }
            let starter_shape = PathShape::new(None, root_info.milestone_group_id);
            let ta = self.find_applicable_action(&starter_shape, input);
            let mut starter_next_paths: PathMap = PathMap::new();
            if let Some(ta) = ta {
                let mut ignored_starters: HashMap<PathRoot, i32> = HashMap::new();
                self.apply_term_action(
                    &starter_shape,
                    &AcceptCondition::Always,
                    path_root,
                    &ta,
                    ctx.gen_idx,
                    next_gen,
                    &mut starter_next_paths,
                    &mut finishes,
                    &mut progresses,
                    &mut new_cond_root_progresses,
                    &mut observing,
                    &mut ignored_starters,
                );
            }
            if !starter_next_paths.is_empty() {
                next_paths.insert(path_root, starter_next_paths);
            } else if path_root.start_gen == next_gen {
                let mut fallback = PathMap::new();
                fallback.insert(starter_shape, AcceptCondition::Always);
                next_paths.insert(path_root, fallback);
            }
        }

        // ----- step 4: cond path finish detection -----
        let mut cond_path_finishes: HashMap<PathRoot, AcceptCondition> = HashMap::new();
        for (root, cond) in &root_progresses {
            if *root != ctx.main_root {
                cond_path_finishes.insert(*root, cond.clone());
            }
        }
        for (root, cond) in &new_cond_root_progresses {
            if *root != ctx.main_root {
                or_merge(&mut cond_path_finishes, *root, cond.clone());
            }
        }

        // ----- step 5: evolve every condition in every path -----
        let active_cond_roots: HashSet<PathRoot> = next_paths.keys().copied().collect();
        let mut paths_evolved: HashMap<PathRoot, PathMap> = HashMap::new();
        for (root, pm) in &next_paths {
            let mut result: PathMap = PathMap::new();
            for (shape, cond) in pm {
                let evolved = evolve_accept_condition(
                    cond,
                    &cond_path_finishes,
                    &active_cond_roots,
                    next_gen,
                );
                if matches!(evolved, AcceptCondition::Never) {
                    continue;
                }
                add_path(&mut result, shape.clone(), evolved);
            }
            if !result.is_empty() {
                paths_evolved.insert(*root, result);
            }
        }
        let main_paths_evolved =
            paths_evolved.get(&ctx.main_root).cloned().unwrap_or_default();

        // ----- step 6: prune unreferenced cond paths -----
        let mut referenced_roots: HashSet<PathRoot> = HashSet::new();
        for pm in paths_evolved.values() {
            for (shape, cond) in pm {
                cond.referenced_roots().for_each(|r| {
                    referenced_roots.insert(*r);
                });
                let mut mp = shape.milestone_path.clone();
                while let Some(node) = mp {
                    for sid in node.observing_cond_symbol_ids.iter().copied() {
                        referenced_roots.insert(PathRoot::new(sid, node.gen_idx));
                        let parent_gen =
                            node.parent.as_ref().map(|p| p.gen_idx).unwrap_or(ctx.main_root.start_gen);
                        referenced_roots.insert(PathRoot::new(sid, parent_gen));
                    }
                    mp = node.parent.clone();
                }
            }
        }
        let mut paths_filtered: HashMap<PathRoot, PathMap> = HashMap::new();
        for (root, pm) in paths_evolved {
            if root == ctx.main_root || referenced_roots.contains(&root) {
                paths_filtered.insert(root, pm);
            }
        }

        // ----- step 7: error check + history -----
        if !is_last_input && main_paths_evolved.is_empty() {
            let expected = self.expected_inputs_of(&ctx);
            return Err(ParsingError::UnexpectedInput {
                loc: ctx.gen_idx,
                line: ctx.line,
                col: ctx.col,
                expected,
                actual: input,
            });
        }

        let active_cond_paths_for_history: HashSet<PathRoot> = paths_filtered
            .keys()
            .copied()
            .filter(|r| *r != ctx.main_root)
            .collect();

        let history_entry = HistoryEntry {
            finished_kernels: finishes,
            progressed_kernels: progresses,
            cond_path_finishes,
            active_cond_paths: active_cond_paths_for_history.clone(),
        };

        let ParsingCtx { mut history, mut ever_seen_cond_roots, .. } = ctx;
        history.push(history_entry);
        ever_seen_cond_roots.extend(active_cond_paths_for_history);

        Ok(ParsingCtx {
            gen_idx: next_gen,
            line: next_line,
            col: next_col,
            main_root: ctx.main_root,
            paths: paths_filtered,
            history,
            ever_seen_cond_roots,
        })
    }

    /// Drive every character of `text` through `parse_step`. Mirrors
    /// `Mgroup3Parser.kt:701-707`.
    pub fn parse(&self, text: &str) -> Result<ParsingCtx, ParsingError> {
        let chars: Vec<char> = text.chars().collect();
        let total = chars.len();
        let mut ctx = self.init_ctx();
        for (idx, c) in chars.into_iter().enumerate() {
            ctx = self.parse_step(ctx, c, idx + 1 == total)?;
        }
        Ok(ctx)
    }

    /// True iff parsing reached an accept state for the start symbol.
    /// Mirrors `Mgroup3Parser.kt:709-722`.
    pub fn is_accepted(&self, ctx: &ParsingCtx) -> bool {
        let start = self.plain.start_symbol_id;
        let Some(last_entry) = ctx.history.last() else { return false };
        let active_cond_paths: HashSet<PathRoot> = ctx
            .paths
            .keys()
            .copied()
            .filter(|r| *r != ctx.main_root)
            .collect();
        for rec in &last_entry.finished_kernels {
            if rec.kernel.symbol_id == start
                && rec.kernel.gen_idx == 0
                && rec.kernel.pointer >= 1
                && evaluate_with_history(&rec.condition, &ctx.history, &active_cond_paths)
            {
                return true;
            }
        }
        false
    }

    /// One `KtlibKernel` set per generation, including conditional finishes
    /// that survive history evaluation. Mirrors `Mgroup3Parser.kt:814-851`.
    pub fn kernels_history(&self, ctx: &ParsingCtx) -> Vec<HashSet<KtlibKernel>> {
        let final_active_cond_paths: HashSet<PathRoot> = ctx
            .paths
            .keys()
            .copied()
            .filter(|r| *r != ctx.main_root)
            .collect();
        let mut out = Vec::with_capacity(ctx.history.len());
        for (gen_idx, entry) in ctx.history.iter().enumerate() {
            let gen_idx = gen_idx as i32;
            let mut kernels: HashSet<KtlibKernel> = HashSet::new();
            for rec in &entry.finished_kernels {
                if evaluate_with_history(
                    &rec.condition,
                    &ctx.history,
                    &final_active_cond_paths,
                ) {
                    kernels.insert(KtlibKernel {
                        symbol_id: rec.kernel.symbol_id,
                        pointer: rec.kernel.pointer,
                        begin_gen: rec.kernel.gen_idx,
                        end_gen: gen_idx,
                    });
                }
            }
            for rec in &entry.progressed_kernels {
                kernels.insert(KtlibKernel {
                    symbol_id: rec.symbol_id,
                    pointer: rec.pointer,
                    begin_gen: rec.start_gen,
                    end_gen: rec.mid_gen,
                });
                kernels.insert(KtlibKernel {
                    symbol_id: rec.symbol_id,
                    pointer: rec.pointer + 1,
                    begin_gen: rec.start_gen,
                    end_gen: rec.end_gen,
                });
            }
            out.push(kernels);
        }
        out
    }

    /// Apply one `TermAction` to a single (shape, cond) pair, accumulating
    /// outputs. Mirrors `Mgroup3Parser.kt:180-278`.
    fn apply_term_action(
        &self,
        old_shape: &PathShape,
        old_condition: &AcceptCondition,
        path_root: PathRoot,
        term_action: &TermActionPlain,
        mid_gen: i32,
        gen_idx: i32,
        next_paths_out: &mut PathMap,
        finishes_out: &mut Vec<FinishedKernelRecord>,
        progresses_out: &mut Vec<ProgressedKernelRecord>,
        root_progresses_out: &mut HashMap<PathRoot, AcceptCondition>,
        observing_out: &mut HashSet<i32>,
        cond_root_starters_out: &mut HashMap<PathRoot, i32>,
    ) {
        let parent_gen = old_shape
            .milestone_path
            .as_ref()
            .map(|mp| mp.gen_idx)
            .unwrap_or(path_root.start_gen);
        let grand_gen = old_shape
            .milestone_path
            .as_ref()
            .map(|mp| mp.milestone.gen_idx)
            .unwrap_or(path_root.start_gen);

        if let Some(pa) = &term_action.parsing_actions {
            for finished in &pa.finished {
                let start_gen =
                    resolve_gen_i32(finished.start_gen, parent_gen, mid_gen, gen_idx, grand_gen);
                let cond_tpl = finished
                    .finish_condition
                    .as_ref()
                    .expect("FinishedKernelTemplate.finish_condition missing");
                finishes_out.push(FinishedKernelRecord {
                    kernel: Kernel::new(finished.symbol_id, finished.pointer, start_gen),
                    condition: build_condition(
                        cond_tpl, parent_gen, mid_gen, gen_idx, grand_gen,
                    ),
                });
            }
            for prog in &pa.progressed {
                let start_gen =
                    resolve_gen_i32(prog.start_gen, parent_gen, mid_gen, gen_idx, grand_gen);
                let m_gen =
                    resolve_gen_i32(prog.mid_gen, parent_gen, mid_gen, gen_idx, grand_gen);
                progresses_out.push(ProgressedKernelRecord {
                    symbol_id: prog.symbol_id,
                    pointer: prog.pointer,
                    start_gen,
                    mid_gen: m_gen,
                    end_gen: gen_idx,
                });
            }
        }

        for rea in &term_action.replace_and_appends {
            let new_cond = build_condition(
                &rea.append.accept_condition,
                parent_gen,
                mid_gen,
                gen_idx,
                grand_gen,
            );
            let combined = AcceptCondition::and_from([old_condition.clone(), new_cond]);
            if matches!(combined, AcceptCondition::Never) {
                continue;
            }
            let replace_kernel =
                Kernel::new(rea.replace.symbol_id, rea.replace.pointer, parent_gen);
            let new_mp = Rc::new(MilestonePath::new(
                gen_idx,
                replace_kernel,
                old_shape.milestone_path.clone(),
                Rc::clone(&rea.append.observing_cond_symbol_ids),
            ));
            add_path(
                next_paths_out,
                PathShape::new(Some(new_mp), rea.append.milestone_group_id),
                combined,
            );
            for sid in rea.append.observing_cond_symbol_ids.iter().copied() {
                observing_out.insert(sid);
            }
            for starter in &rea.append.cond_root_starters {
                cond_root_starters_out
                    .insert(PathRoot::new(starter.symbol_id, gen_idx), starter.milestone_group_id);
            }
        }

        for rap in &term_action.replace_and_progresses {
            let new_cond = build_condition(
                &rap.accept_condition,
                parent_gen,
                mid_gen,
                gen_idx,
                grand_gen,
            );
            let combined = AcceptCondition::and_from([old_condition.clone(), new_cond]);
            if matches!(combined, AcceptCondition::Never) {
                continue;
            }
            let parent_path = old_shape.milestone_path.as_ref();
            match parent_path {
                None => {
                    // Direct self-progress from root (start symbol finish).
                    or_merge(root_progresses_out, path_root, combined.clone());
                    finishes_out.push(FinishedKernelRecord {
                        kernel: Kernel::new(path_root.symbol_id, 1, path_root.start_gen),
                        condition: combined,
                    });
                }
                Some(parent_path) => {
                    let key = (
                        parent_path.milestone.kernel_template(),
                        rap.replace_milestone_group_id,
                    );
                    if let Some(tip_edge_action) = self.tip_edge_actions.get(&key).cloned() {
                        let grand_parent_gen = parent_path
                            .parent
                            .as_ref()
                            .map(|p| p.gen_idx)
                            .unwrap_or(path_root.start_gen);
                        self.apply_edge_action(
                            parent_path,
                            &tip_edge_action,
                            path_root,
                            &combined,
                            grand_parent_gen,
                            parent_path.gen_idx,
                            gen_idx,
                            next_paths_out,
                            finishes_out,
                            progresses_out,
                            root_progresses_out,
                            observing_out,
                            cond_root_starters_out,
                        );
                    }
                }
            }
        }
    }

    /// Apply an `EdgeAction` reduction. Mirrors `Mgroup3Parser.kt:285-377`.
    /// `parent_path` is the milestone immediately upstream from the just-finished tip.
    fn apply_edge_action(
        &self,
        parent_path: &Rc<MilestonePath>,
        edge_action: &EdgeActionPlain,
        path_root: PathRoot,
        prev_condition: &AcceptCondition,
        grand_parent_gen: i32,
        parent_gen: i32,
        gen_idx: i32,
        next_paths_out: &mut PathMap,
        finishes_out: &mut Vec<FinishedKernelRecord>,
        progresses_out: &mut Vec<ProgressedKernelRecord>,
        root_progresses_out: &mut HashMap<PathRoot, AcceptCondition>,
        observing_out: &mut HashSet<i32>,
        cond_root_starters_out: &mut HashMap<PathRoot, i32>,
    ) {
        // edge action next_gen table:
        //   CURR  = grand_parent_gen   (Kotlin's "currGen" param)
        //   MID   = parent_gen
        //   NEXT  = gen_idx
        //   GRAND = grand_grand_parent_gen
        let grand_grand_parent_gen = parent_path.milestone.gen_idx;

        if let Some(pa) = &edge_action.parsing_actions {
            for finished in &pa.finished {
                let start_gen = resolve_gen_i32(
                    finished.start_gen,
                    grand_parent_gen,
                    parent_gen,
                    gen_idx,
                    grand_grand_parent_gen,
                );
                let cond_tpl = finished
                    .finish_condition
                    .as_ref()
                    .expect("FinishedKernelTemplate.finish_condition missing");
                finishes_out.push(FinishedKernelRecord {
                    kernel: Kernel::new(finished.symbol_id, finished.pointer, start_gen),
                    condition: build_condition(
                        cond_tpl,
                        grand_parent_gen,
                        parent_gen,
                        gen_idx,
                        grand_grand_parent_gen,
                    ),
                });
            }
            for prog in &pa.progressed {
                let start_gen = resolve_gen_i32(
                    prog.start_gen,
                    grand_parent_gen,
                    parent_gen,
                    gen_idx,
                    grand_grand_parent_gen,
                );
                let m_gen = resolve_gen_i32(
                    prog.mid_gen,
                    grand_parent_gen,
                    parent_gen,
                    gen_idx,
                    grand_grand_parent_gen,
                );
                progresses_out.push(ProgressedKernelRecord {
                    symbol_id: prog.symbol_id,
                    pointer: prog.pointer,
                    start_gen,
                    mid_gen: m_gen,
                    end_gen: gen_idx,
                });
            }
        }

        for append in &edge_action.append_milestone_groups {
            let cond = build_condition(
                &append.accept_condition,
                grand_parent_gen,
                parent_gen,
                gen_idx,
                grand_grand_parent_gen,
            );
            let combined = AcceptCondition::and_from([prev_condition.clone(), cond]);
            if matches!(combined, AcceptCondition::Never) {
                continue;
            }
            let new_parent_path = parent_path.with_observing(Rc::clone(&append.observing_cond_symbol_ids));
            add_path(
                next_paths_out,
                PathShape::new(Some(new_parent_path), append.milestone_group_id),
                combined,
            );
            for sid in append.observing_cond_symbol_ids.iter().copied() {
                observing_out.insert(sid);
            }
            for starter in &append.cond_root_starters {
                cond_root_starters_out
                    .insert(PathRoot::new(starter.symbol_id, gen_idx), starter.milestone_group_id);
            }
        }

        if let Some(start_node_tpl) = edge_action.start_node_progress.as_ref() {
            let start_node_cond = build_condition(
                start_node_tpl,
                grand_parent_gen,
                parent_gen,
                gen_idx,
                grand_grand_parent_gen,
            );
            let combined = AcceptCondition::and_from([prev_condition.clone(), start_node_cond]);
            if !matches!(combined, AcceptCondition::Never) {
                match parent_path.parent.as_ref() {
                    None => {
                        or_merge(root_progresses_out, path_root, combined.clone());
                        finishes_out.push(FinishedKernelRecord {
                            kernel: Kernel::new(path_root.symbol_id, 1, path_root.start_gen),
                            condition: combined,
                        });
                    }
                    Some(grand_parent) => {
                        let key = (
                            grand_parent.milestone.kernel_template(),
                            parent_path.milestone.kernel_template(),
                        );
                        if let Some(mid_edge) = self.mid_edge_actions.get(&key).cloned() {
                            let grand_grand_parent_gen_2 = grand_parent
                                .parent
                                .as_ref()
                                .map(|p| p.gen_idx)
                                .unwrap_or(path_root.start_gen);
                            self.apply_edge_action(
                                grand_parent,
                                &mid_edge,
                                path_root,
                                &combined,
                                grand_grand_parent_gen_2,
                                grand_parent.gen_idx,
                                gen_idx,
                                next_paths_out,
                                finishes_out,
                                progresses_out,
                                root_progresses_out,
                                observing_out,
                                cond_root_starters_out,
                            );
                        }
                    }
                }
            }
        }
    }
}

/// Helper: Or-merge `cond` into `map[key]`. If `cond == Never` we still insert
/// it (callers usually filter `Never` before calling, but this keeps the
/// semantics in one place).
fn or_merge(
    map: &mut HashMap<PathRoot, AcceptCondition>,
    key: PathRoot,
    cond: AcceptCondition,
) {
    use std::collections::hash_map::Entry;
    match map.entry(key) {
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
        // Closure {1, 2, 3} — each becomes a PathRoot at next_gen=5.
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

    /// Empty input — `is_last_input=true` for none, so we just take the
    /// initial ctx and confirm it doesn't break.
    #[test]
    fn parse_empty_input() {
        let parser = Mgroup3Parser::new(empty_data_with_start(1));
        let ctx = parser.parse("").expect("empty parse should succeed");
        assert_eq!(ctx.gen_idx, 0);
        assert_eq!(ctx.history.len(), 1);
    }

    /// Two characters against an empty grammar: the first char produces an
    /// empty main path, and the second char's step-start check
    /// (main_paths_before.is_empty) throws.
    #[test]
    fn parse_two_chars_against_empty_grammar_errors() {
        let parser = Mgroup3Parser::new(empty_data_with_start(1));
        let err = parser.parse("ab").expect_err("should reject");
        match err {
            ParsingError::UnexpectedInput { .. } => {}
            other => panic!("expected UnexpectedInput, got {:?}", other),
        }
    }

    /// Single char on an empty grammar succeeds (step 7 only throws on
    /// non-last input). is_accepted is false because nothing finished.
    #[test]
    fn parse_one_char_empty_grammar_succeeds_but_not_accepted() {
        let parser = Mgroup3Parser::new(empty_data_with_start(1));
        let ctx = parser.parse("a").expect("single char should succeed");
        assert!(!parser.is_accepted(&ctx));
    }

    #[test]
    fn kernels_history_includes_initial_finish() {
        // Path root with self_finish_accept_condition = Always — at gen 0 we
        // have a finished kernel (start, 1, 0).
        use crate::proto::com::giyeok::jparser::mgroup3::proto::accept_condition_template::Condition;
        use crate::proto::com::giyeok::jparser::mgroup3::proto::AcceptConditionTemplate;
        let mut d = empty_data_with_start(7);
        d.path_roots.get_mut(&7).unwrap().self_finish_accept_condition =
            Some(AcceptConditionTemplate { condition: Some(Condition::Always(())) });
        let parser = Mgroup3Parser::new(d);
        let ctx = parser.init_ctx();
        let kh = parser.kernels_history(&ctx);
        assert_eq!(kh.len(), 1);
        assert!(kh[0].iter().any(|k| k.symbol_id == 7 && k.pointer == 1));
        assert!(parser.is_accepted(&ctx));
    }
}
