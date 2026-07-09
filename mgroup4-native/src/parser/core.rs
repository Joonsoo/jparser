//! `Mgroup4Parser` struct and the parts of its public surface that don't need
//! `parseStep` yet: construction, `init_ctx`, `cond_paths_for`,
//! `find_applicable_action`, `expected_inputs_of`. `parseStep` and the recursive
//! helpers land in Step 3.6.

use std::cell::OnceCell;
use std::collections::VecDeque;
use rustc_hash::{FxHashMap as HashMap, FxHashSet as HashSet};
use std::rc::Rc;
use std::sync::Arc;

use crate::accept_condition::AcceptCondition;
use crate::parser::template::{build_condition, resolve_gen_i32};
use crate::parser_data::{EdgeActionPlain, ParserDataPlain, ParsingActionsPlain, TermActionPlain};
use crate::parsing_ctx::{
    add_path, ActionApplication, AddedKernelRecord, FinishedKernelRecord, HistoryEntry, Kernel,
    KernelTemplatePair, KtlibKernel, MilestonePath, ParsingCtx, PathMap, PathShape,
};
use crate::path_root::PathRoot;
use crate::proto::com::giyeok::jparser::mgroup3::proto::{KernelTemplateGen, Mgroup3ParserData};
use crate::accept_condition::eval::evolve_accept_condition;
use crate::term_group::{is_match, TermGroupBuilder, TermSet};

use super::ParsingError;

/// 시동 대기 중인 cond root starter — same_input 이면 이번 입력이 watcher 의 첫 글자
/// (key==gen 인 lookahead 구 규약이면 실제 span 은 gen-1 — 보고 anchor 별도 기록).
#[derive(Clone, Copy, Debug)]
pub struct PendingStarter {
    pub milestone_group_id: i32,
    pub same_input: bool,
}

/// cond root starter 의 key resolve. MID = ctx.gen (bounded span-정규화), NEXT = gen.
/// CURR 등 과거 경계는 그 시점에 이미 등록된 watcher — 등록하지 않는다 (None).
fn starter_key_of(key_gen: i32, mid_gen: i32, next_gen: i32) -> Option<i32> {
    match KernelTemplateGen::try_from(key_gen) {
        Ok(KernelTemplateGen::Mid) => Some(mid_gen),
        Ok(KernelTemplateGen::Next) => Some(next_gen),
        _ => None,
    }
}

// mgroup4 interior group window n. env var MG4_INTERIOR_N 로 전역 오버라이드 —
// 코드 수정 없이 전 스위트를 임의 n 으로 돌리기 위함 (Kotlin 과 동일 이름).
// env 미설정 시 생성자 기본값. n=1 ≡ mgroup3 (병합 패스 미실행 — 제로코스트).
pub const MG4_DEFAULT_INTERIOR_MAX_DEPTH: i32 = 1;

fn mg4_env_interior_max_depth() -> Option<i32> {
    std::env::var("MG4_INTERIOR_N").ok().and_then(|s| s.parse().ok())
}

// mgroup4 mean-shape 카운터 opt-in (§5.1) — 파스 출력 무영향, 진단 전용.
fn mg4_shape_stats_enabled() -> bool {
    std::env::var_os("MG4_SHAPE_STATS").is_some()
}

// A4 병합-패스 내부 프로파일 opt-in (`MG4_MERGE_PROFILE`). 매 gen merge_interior_groups
// 안의 세부 단계 시간을 ns 로 분해. 기본 false 라 hot path 에 Instant::now() 가 안
// 들어감. 정식 시간 측정에서는 꺼야 오버헤드 없는 실측이 된다. Kotlin mg4MergeProfileEnabled.
fn mg4_merge_profile_enabled() -> bool {
    std::env::var_os("MG4_MERGE_PROFILE").is_some()
}

/// mgroup4 interior group counters (opt-in via `MG4_SHAPE_STATS`). Atomic so the
/// parser handle stays `Send + Sync` (bibix4 parallel parsing) — the counters
/// are diagnostic-only and touched at most once per gen, so the atomics are off
/// the shapes-sensitive hot path. Kotlin `mg4*` fields (Mgroup4Parser.kt §5.1).
#[derive(Default)]
pub struct Mg4Stats {
    /// 병합 후 main shape 수 누적 / 병합 안 했을 때 (멤버 총수) 누적 / gen 수.
    pub merged_shape_sum: std::sync::atomic::AtomicI64,
    pub base_shape_sum: std::sync::atomic::AtomicI64,
    pub gens: std::sync::atomic::AtomicI64,
    /// 병합 거부 사유별 (H5 실현율 신호).
    pub reject_cond_diff: std::sync::atomic::AtomicI64,
    pub reject_gen_obs_diff: std::sync::atomic::AtomicI64,
    pub reject_report_coord_diff: std::sync::atomic::AtomicI64,
    /// 이미 group 노드가 있어 새 병합 후보에서 제외된 shape 수 (spec item 6).
    pub skip_existing_group: std::sync::atomic::AtomicI64,
    /// 병합 기원 분류 (spec item 7, Phase B 데이터). collapse 수 기준.
    pub creation_mergeable: std::sync::atomic::AtomicI64,
    pub late_convergence: std::sync::atomic::AtomicI64,
    /// reduce 분열 / window-exit 분열 카운터 (진단).
    pub reduce_splits: std::sync::atomic::AtomicI64,
    pub window_exit_splits: std::sync::atomic::AtomicI64,
    /// depth 별 실제 병합 collapse 수 (index = depth, 0/1 미사용).
    pub merges_at_depth: [std::sync::atomic::AtomicI64; 16],
    /// A4 병합-패스 세부 self-time (ns 누적) — `MG4_MERGE_PROFILE` opt-in 일 때만.
    /// slot: 0=partition(버킷 해시), 1=verdict, 2=fold, 3=mergeTotal(패스 전체).
    /// 기본 off 라 hot path 에 Instant::now() 가 안 들어감 (Kotlin mergeNanos 대응 —
    /// 정식 시간 측정에서는 꺼야 오버헤드 없는 실측이 된다).
    pub merge_nanos: [std::sync::atomic::AtomicI64; 4],
}

impl Mg4Stats {
    fn report(&self, n: i32) -> String {
        use std::sync::atomic::Ordering::Relaxed;
        let gens = self.gens.load(Relaxed);
        let mean_merged = if gens > 0 { self.merged_shape_sum.load(Relaxed) as f64 / gens as f64 } else { 0.0 };
        let mean_base = if gens > 0 { self.base_shape_sum.load(Relaxed) as f64 / gens as f64 } else { 0.0 };
        let ratio = if mean_merged > 0.0 { mean_base / mean_merged } else { 1.0 };
        let by_depth: String = self
            .merges_at_depth
            .iter()
            .enumerate()
            .filter_map(|(i, v)| {
                let x = v.load(Relaxed);
                if x > 0 { Some(format!("d{}={}", i, x)) } else { None }
            })
            .collect::<Vec<_>>()
            .join(",");
        let mt = self.merge_nanos[3].load(Relaxed);
        let merge_ms = mt as f64 / 1_000_000.0;
        format!(
            "mg4 n={} gens={} meanBase={:.2} meanMerged={:.2} ratio={:.3} \
             rejectCondDiff={} rejectGenObsDiff={} rejectReportCoordDiff={} \
             skipExistingGroup={} creationMergeable={} lateConvergence={} \
             reduceSplits={} windowExitSplits={} mergesByDepth=[{}] \
             mergeTotal={:.1}ms(partition={:.1}ms verdict={:.1}ms fold={:.1}ms)",
            n, gens, mean_base, mean_merged, ratio,
            self.reject_cond_diff.load(Relaxed),
            self.reject_gen_obs_diff.load(Relaxed),
            self.reject_report_coord_diff.load(Relaxed),
            self.skip_existing_group.load(Relaxed),
            self.creation_mergeable.load(Relaxed),
            self.late_convergence.load(Relaxed),
            self.reduce_splits.load(Relaxed),
            self.window_exit_splits.load(Relaxed),
            by_depth,
            merge_ms,
            self.merge_nanos[0].load(Relaxed) as f64 / 1_000_000.0,
            self.merge_nanos[1].load(Relaxed) as f64 / 1_000_000.0,
            self.merge_nanos[2].load(Relaxed) as f64 / 1_000_000.0,
        )
    }

    /// 병합 패스 전체 self-time (ns). `MG4_MERGE_PROFILE` opt-in 일 때만 채워짐.
    pub fn merge_total_nanos(&self) -> i64 {
        self.merge_nanos[3].load(std::sync::atomic::Ordering::Relaxed)
    }
}

pub struct Mgroup4Parser {
    plain: ParserDataPlain,
    /// (parent kernel template, tip group id) → tip edge action.
    tip_edge_actions: HashMap<(KernelTemplatePair, i32), Arc<EdgeActionPlain>>,
    /// (parent kernel template, tip kernel template) → mid edge action.
    mid_edge_actions: HashMap<(KernelTemplatePair, KernelTemplatePair), Arc<EdgeActionPlain>>,
    /// mgroup4: interior milestone group window n (tip 쪽 마지막 n 개 노드까지 group
    /// 허용). n=1 ≡ mgroup3 (병합 패스 미실행). env `MG4_INTERIOR_N` 가 생성자 인자보다
    /// 우선 (Kotlin interiorGroupMaxDepth 와 동일 규약).
    interior_group_max_depth: i32,
    /// opt-in shape stats (`MG4_SHAPE_STATS`). Atomic → 파서 Send+Sync 유지.
    stats_enabled: bool,
    /// opt-in merge-pass self-time profile (`MG4_MERGE_PROFILE`).
    merge_profile: bool,
    stats: Mg4Stats,
}

// 파서 인스턴스는 스레드 간 공유되어 동시에 사용된다 — bibix4 의 병렬 파일 파싱이
// FFI 로 하나의 핸들을 공유한다. 공유 데이터는 전부 Arc/불변, term action 캐시는
// 파스-로컬 (ParsingCtx). (parse 중 만들어지는 ctx 내부의 Rc 들은 파스-로컬이라 무관.)
// mg4 카운터는 AtomicI64 라 Send+Sync 위반 없음.
// 컴파일 타임 보증:
const _: () = {
    const fn assert_send_sync<T: Send + Sync>() {}
    let _ = assert_send_sync::<Mgroup4Parser>;
};

impl Mgroup4Parser {
    pub fn new(data: Mgroup3ParserData) -> Self {
        let plain = ParserDataPlain::from_proto(data);
        Self::from_plain(plain)
    }

    /// Build with an explicit interior-group window `n` (env `MG4_INTERIOR_N`
    /// still wins if set). n=1 ≡ mgroup3. Mirrors the Kotlin constructor arg.
    pub fn new_with_n(data: Mgroup3ParserData, n: i32) -> Self {
        let plain = ParserDataPlain::from_proto(data);
        Self::from_plain_with_n(plain, n)
    }

    /// Build a parser from an already-materialized `ParserDataPlain`. This is the
    /// shared tail of `new` (which goes through proto) and the rkyv cache path —
    /// both produce identical `ParserDataPlain`, so the parser is identical.
    pub fn from_plain(plain: ParserDataPlain) -> Self {
        Self::from_plain_with_n(plain, MG4_DEFAULT_INTERIOR_MAX_DEPTH)
    }

    /// `from_plain` with an explicit interior-group window `n` (env overrides).
    pub fn from_plain_with_n(plain: ParserDataPlain, n: i32) -> Self {
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
                (key, Arc::clone(&p.edge_action))
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
                (key, Arc::clone(&p.edge_action))
            })
            .collect();
        // 실효 n — env var 이 생성자 인자보다 우선 (Kotlin 규약).
        let interior_group_max_depth = mg4_env_interior_max_depth().unwrap_or(n);
        Self {
            plain,
            tip_edge_actions,
            mid_edge_actions,
            interior_group_max_depth,
            stats_enabled: mg4_shape_stats_enabled(),
            merge_profile: mg4_merge_profile_enabled(),
            stats: Mg4Stats::default(),
        }
    }

    pub fn plain(&self) -> &ParserDataPlain {
        &self.plain
    }

    pub fn start_symbol_id(&self) -> i32 {
        self.plain.start_symbol_id
    }

    /// Effective interior-group window `n`.
    pub fn interior_group_max_depth(&self) -> i32 {
        self.interior_group_max_depth
    }

    /// mgroup4 shape-stats report (opt-in via `MG4_SHAPE_STATS`). Empty-ish when
    /// disabled. Diagnostic only.
    pub fn report_mg4_stats(&self) -> String {
        self.stats.report(self.interior_group_max_depth)
    }

    /// 병합 패스 전체 self-time (ns). `MG4_MERGE_PROFILE` opt-in 일 때만.
    pub fn merge_total_nanos(&self) -> i64 {
        self.stats.merge_total_nanos()
    }

    #[allow(dead_code)] // used in Step 3.6
    pub(crate) fn tip_edge_action(
        &self,
        parent: KernelTemplatePair,
        tip_group_id: i32,
    ) -> Option<Arc<EdgeActionPlain>> {
        self.tip_edge_actions.get(&(parent, tip_group_id)).cloned()
    }

    #[allow(dead_code)] // used in Step 3.6
    pub(crate) fn mid_edge_action(
        &self,
        parent: KernelTemplatePair,
        tip: KernelTemplatePair,
    ) -> Option<Arc<EdgeActionPlain>> {
        self.mid_edge_actions.get(&(parent, tip)).cloned()
    }

    /// Initialize a parsing context at the configured start symbol.
    pub fn init_ctx(&self) -> ParsingCtx {
        self.init_ctx_with_start(self.plain.start_symbol_id)
    }

    /// Initialize a parsing context starting from `start_symbol_id`.
    /// Mirrors `Mgroup4Parser.kt:88-127`.
    pub fn init_ctx_with_start(&self, start_symbol_id: i32) -> ParsingCtx {
        let root_info = self
            .plain
            .path_roots
            .get(&start_symbol_id)
            .unwrap_or_else(|| panic!("No path root for symbol {}", start_symbol_id));

        let main_root = PathRoot::new(start_symbol_id, 0);
        let mut main_paths: PathMap = PathMap::default();
        main_paths.insert(PathShape::new(None, root_info.milestone_group_id), AcceptCondition::Always);

        let initial_cond_paths = self.cond_paths_for(&root_info.initial_cond_symbol_ids, 0);

        let mut all_paths: HashMap<PathRoot, PathMap> = HashMap::default();
        all_paths.insert(main_root, main_paths);
        for (k, v) in initial_cond_paths {
            all_paths.insert(k, v);
        }

        let mut initial_apps: Vec<ActionApplication> = Vec::new();
        let mut initial_finished: Vec<FinishedKernelRecord> = Vec::new();
        if let Some(pa) = &root_info.parsing_actions {
            initial_apps.push(initial_application(Arc::clone(pa), main_root));
        }
        // cond root 들의 초기 derive closure 도 보고 — m2 의 초기 tasksSummary 가
        // in-graph cond body 를 포함하는 것에 대응. (정렬: 결정적 출력)
        let mut initial_cond_roots: Vec<PathRoot> =
            all_paths.keys().copied().filter(|r| *r != main_root).collect();
        initial_cond_roots.sort_by_key(|r| (r.symbol_id, r.start_gen));
        for cond_root in &initial_cond_roots {
            if let Some(info) = self.plain.path_roots.get(&cond_root.symbol_id) {
                if let Some(pa) = &info.parsing_actions {
                    initial_apps.push(initial_application(Arc::clone(pa), *cond_root));
                }
            }
        }
        let mut initial_main_root_finish: Option<AcceptCondition> = None;
        if let Some(self_finish_tpl) = &root_info.self_finish_accept_condition {
            let cond = build_condition(self_finish_tpl, 0, 0, 0, 0);
            initial_finished.push(FinishedKernelRecord {
                kernel: Kernel::new(start_symbol_id, 1, 0),
                condition: cond.clone(),
                root: main_root,
            });
            initial_main_root_finish = Some(cond);
        }

        // gen 0 zero-width self-finish: 초기 cond root 가 빈 span (0,0) 으로 완성
        // 가능하면 entry 0 의 cond_path_finishes 에 등록 (빈 입력 수용; Kotlin
        // initCtx 대응).
        let mut initial_cond_path_finishes: HashMap<PathRoot, AcceptCondition> =
            HashMap::default();
        for cond_root in &initial_cond_roots {
            if let Some(info) = self.plain.path_roots.get(&cond_root.symbol_id) {
                if let Some(tpl) = &info.self_finish_accept_condition {
                    initial_cond_path_finishes
                        .insert(*cond_root, build_condition(tpl, 0, 0, 0, 0));
                }
            }
        }

        let initial_entry = HistoryEntry {
            action_applications: initial_apps,
            finished_kernels: initial_finished,
            added_kernels: Vec::new(),
            cond_path_finishes: initial_cond_path_finishes,
            late_cond_path_finishes: HashMap::default(),
            active_cond_paths: initial_cond_roots.iter().copied().collect(),
            main_root_finish: initial_main_root_finish,
            reported_cond_roots: initial_cond_roots.into_iter().collect(),
        };

        ParsingCtx {
            gen_idx: 0,
            line: 0,
            col: 0,
            main_root,
            paths: all_paths,
            history: vec![initial_entry],
            ever_seen_cond_roots: Default::default(),
            root_report_gens: Default::default(),
            term_action_cache: Default::default(),
            step_scratch: Default::default(),
        }
    }

    

    /// Build initial path maps for every symbol in the transitive closure of
    /// `cond_symbol_ids`. Mirrors `Mgroup4Parser.kt:72-86`. Each created path
    /// has a single shape `(None, milestoneGroupId)` with `Always` condition.
    pub fn cond_paths_for(
        &self,
        cond_symbol_ids: &[i32],
        gen_idx: i32,
    ) -> HashMap<PathRoot, PathMap> {
        let mut builder: HashMap<i32, PathShape> = HashMap::default();
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
                let mut pm = PathMap::default();
                pm.insert(shape, AcceptCondition::Always);
                (PathRoot::new(sym_id, gen_idx), pm)
            })
            .collect()
    }

    /// Look up a term action for `(shape.tip_group_id, input)`. Cached.
    /// Mirrors `Mgroup4Parser.kt:164-173`.
    /// cache 는 파스-로컬 (ParsingCtx.term_action_cache) — 파서 인스턴스는 여러
    /// 스레드가 동시에 사용하므로 (bibix4 병렬 파일 파싱) 공유 캐시는 핫패스에서
    /// cacheline 경합을 일으킨다. 파일 안에서는 같은 문자가 반복되므로 파스-로컬
    /// 캐시로도 hit rate 충분.
    pub fn find_applicable_action(
        &self,
        cache: &mut HashMap<i64, Option<Arc<TermActionPlain>>>,
        shape: &PathShape,
        input: char,
    ) -> Option<Arc<TermActionPlain>> {
        let key = ((shape.tip_group_id as i64) << 32) | (input as u32 as i64);
        if let Some(v) = cache.get(&key) {
            return v.clone();
        }
        let result = self.plain.term_actions.get(&shape.tip_group_id).and_then(|actions| {
            actions
                .iter()
                .find(|action| is_match(&action.term_group, input))
                .map(|action| Arc::clone(&action.term_action))
        });
        cache.insert(key, result.clone());
        result
    }

    /// Collect the term groups reachable from the main path's tips into a
    /// `TermSet`. Used in error messages. Mirrors `Mgroup4Parser.kt:151-162`.
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
    /// `Mgroup4Parser` is reused across very different inputs. Not on the hot
    /// path.

    /// Drive one input character. Mirrors `Mgroup4Parser.kt:388-698`.
    pub fn parse_step(
        &self,
        mut ctx: ParsingCtx,
        input: char,
        is_last_input: bool,
    ) -> Result<ParsingCtx, ParsingError> {

        // 파스-로컬 term action 캐시 — ctx 에서 꺼내 이번 step 동안 사용 후 되돌린다.
        let mut term_cache = std::mem::take(&mut ctx.term_action_cache);
        // 파스-로컬 step scratch — 매 step 새로 할당/폐기하던 컬렉션들을 재사용한다.
        // ctx 에서 꺼내 (mem::take) 이번 step 동안 쓰고 다음 ctx 로 되돌린다. 모든
        // 컬렉션은 사용 전 clear() 하므로 capacity 만 이월되고 stale 데이터는 남지 않는다.
        let mut scratch = std::mem::take(&mut ctx.step_scratch);
        let main_paths_before_empty =
            ctx.paths.get(&ctx.main_root).map(|m| m.is_empty()).unwrap_or(true);
        if main_paths_before_empty {
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

        // 재사용 대상: 순수 scratch (step 안에서 build→read→drop 되고 출력으로 새지
        // 않는) 컬렉션만. scratch 에서 mem::take 로 꺼내 owned 로 쓰고 (본문은 예전과
        // 동일하게 `&mut local` 로 넘긴다), step 끝에서 다시 scratch 로 되돌린다.
        // clear() 로 내용을 비워 capacity 를 유지한다. by-value 로 소비하던 지점
        // (paths_evolved step6, new_cond_roots→sorted) 은 컨테이너를 잃지 않도록
        // drain() 으로 바꿔 재사용을 유지한다.
        let mut next_paths = std::mem::take(&mut scratch.next_paths);
        let mut apps = std::mem::take(&mut scratch.apps);
        let mut finishes = std::mem::take(&mut scratch.finishes);
        let mut added = std::mem::take(&mut scratch.added);
        let mut observing = std::mem::take(&mut scratch.observing);
        let mut root_progresses = std::mem::take(&mut scratch.root_progresses);
        let mut late_pf_progresses = std::mem::take(&mut scratch.late_pf_progresses);
        let mut cond_root_starters_from_term =
            std::mem::take(&mut scratch.cond_root_starters_from_term);
        next_paths.clear();
        apps.clear();
        finishes.clear();
        added.clear();
        observing.clear();
        root_progresses.clear();
        late_pf_progresses.clear();
        cond_root_starters_from_term.clear();

        // ----- step 1+2: main and cond paths both run through applyTermAction -----
        for (root, path_map) in &ctx.paths {
            let is_main = *root == ctx.main_root;
            // per-root inner map from the pool (reused capacity) instead of a
            // fresh allocation each (root × step).
            let mut per_root_next = scratch.take_path_map();
            for (shape, cond) in path_map {
                let ta = self.find_applicable_action(&mut term_cache, shape, input);
                if let Some(ta) = ta {
                    let root_report_gen =
                        ctx.root_report_gens.get(root).copied().unwrap_or(root.start_gen);
                    self.apply_term_action(
                        shape,
                        cond,
                        *root,
                        &ta,
                        ctx.gen_idx,
                        next_gen,
                        root_report_gen,
                        &mut per_root_next,
                        &mut apps,
                        &mut finishes,
                        &mut added,
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
                                    or_merge(&mut late_pf_progresses, *root, combined);
                                }
                            }
                        }
                    }
                }
            }
            if !per_root_next.is_empty() {
                next_paths.insert(*root, per_root_next);
            } else {
                scratch.recycle_path_map(per_root_next);
            }
        }

        // same-input 시동이 죽었을 때 (매치 실패 / 살아남은 path 없음):
        //  - lookahead 계열 key (== next_gen): 구 규약의 fresh fallback — 같은 key 를
        //    다음 경계 watcher (span gen) 로 재시동. 드리프트하는 lookahead anchor 는
        //    같은 key 로 span gen-1 과 span gen 양쪽 해석을 요구할 수 있다.
        //  - bounded 계열 key (== ctx.gen): span-정규화 — 그 span 의 매치는 불가로
        //    확정, key 를 소진시켜 이후 재시동 (span 이 어긋난 zombie) 을 막는다.
        macro_rules! starter_died {
            ($root:expr, $shape:expr, $next_paths:expr, $ctx:expr) => {
                if $root.start_gen == next_gen {
                    let mut seeded = PathMap::default();
                    seeded.insert($shape, AcceptCondition::Always);
                    $next_paths.insert($root, seeded);
                } else {
                    $ctx.ever_seen_cond_roots.insert($root);
                }
            };
        }

        // ----- step 1b: cond root starters 시동 -----
        //  - same_input: 이번 입력이 watcher 의 첫 글자. bounded 계열은 key==ctx.gen
        //    (span-정규화), lookahead 계열은 key==gen (구 규약 — 실제 span 은 gen-1,
        //    보고 anchor 별도 기록).
        //  - !same_input: fresh — 시동만 하고 소비는 다음 step 부터 (새 경계 watcher).
        for (&starter_root, &pending) in &cond_root_starters_from_term {
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
            let starter_shape = PathShape::new(None, pending.milestone_group_id);
            if !pending.same_input {
                // fresh 시동만.
                let mut seeded = PathMap::default();
                seeded.insert(starter_shape, AcceptCondition::Always);
                next_paths.insert(starter_root, seeded);
            } else {
                let ta = self.find_applicable_action(&mut term_cache, &starter_shape, input);
                if let Some(ta) = ta {
                    // 실제 span 시작: key==gen (lookahead 구 규약) 이면 gen-1.
                    let report_gen = if starter_root.start_gen == next_gen {
                        next_gen - 1
                    } else {
                        starter_root.start_gen
                    };
                    if report_gen != starter_root.start_gen {
                        ctx.root_report_gens.insert(starter_root, report_gen);
                    }
                    let mut per_starter_next: PathMap = PathMap::default();
                    let mut ignored_starters: HashMap<PathRoot, PendingStarter> = HashMap::default();
                    self.apply_term_action(
                        &starter_shape,
                        &AcceptCondition::Always,
                        starter_root,
                        &ta,
                        ctx.gen_idx,
                        next_gen,
                        report_gen,
                        &mut per_starter_next,
                        &mut apps,
                        &mut finishes,
                        &mut added,
                        &mut root_progresses,
                        &mut observing,
                        &mut ignored_starters,
                    );
                    if !per_starter_next.is_empty() {
                        let acc = next_paths.entry(starter_root).or_insert_with(PathMap::default);
                        for (s, c) in per_starter_next {
                            add_path(acc, s, c);
                        }
                    } else {
                        starter_died!(starter_root, starter_shape, next_paths, ctx);
                    }
                } else {
                    starter_died!(starter_root, starter_shape, next_paths, ctx);
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
        let mut all_observing = std::mem::take(&mut scratch.all_observing);
        all_observing.clear();
        for sym in &observing {
            if let Some(closure) = self.plain.transitive_initial_cond_symbols.get(sym) {
                all_observing.extend(closure.iter().copied());
            } else {
                all_observing.insert(*sym);
            }
        }

        let mut new_cond_roots = std::mem::take(&mut scratch.new_cond_roots);
        new_cond_roots.clear();
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

        let mut new_cond_root_progresses = std::mem::take(&mut scratch.new_cond_root_progresses);
        new_cond_root_progresses.clear();
        // Iterate over a sorted-by-(sym,next_gen) copy to keep step3 deterministic
        // across HashSet iteration orders. (drain keeps new_cond_roots' allocation
        // for reuse; the sorted vec is likewise pooled.)
        let mut new_cond_roots_sorted = std::mem::take(&mut scratch.new_cond_roots_sorted);
        new_cond_roots_sorted.clear();
        new_cond_roots_sorted.extend(new_cond_roots.drain());
        new_cond_roots_sorted.sort_by_key(|r| (r.symbol_id, r.start_gen));
        for &path_root in new_cond_roots_sorted.iter() {
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
            // 시동 flavor (step 1b 와 동일한 규칙):
            //  - start_gen == next_gen: lookahead 심볼이면 구 규약 same-input (실제 span
            //    gen-1), 그 외 (새 경계 watcher) 는 fresh 시동만.
            //  - start_gen == ctx.gen: bounded span-정규화 same-input.
            //  - start_gen < ctx.gen: 그 시점에 시동됐어야 하는 watcher — 지금 만들면
            //    span 이 어긋난 zombie 가 되므로 시동하지 않는다.
            let same_input = if path_root.start_gen == next_gen {
                self.plain.lookahead_cond_symbols.contains(&path_root.symbol_id)
            } else if path_root.start_gen == ctx.gen_idx {
                true
            } else {
                continue;
            };
            if !same_input {
                let mut seeded = PathMap::default();
                seeded.insert(starter_shape, AcceptCondition::Always);
                next_paths.insert(path_root, seeded);
            } else {
                let ta = self.find_applicable_action(&mut term_cache, &starter_shape, input);
                if let Some(ta) = ta {
                    let report_gen = if path_root.start_gen == next_gen {
                        next_gen - 1
                    } else {
                        path_root.start_gen
                    };
                    if report_gen != path_root.start_gen {
                        ctx.root_report_gens.insert(path_root, report_gen);
                    }
                    let mut starter_next_paths: PathMap = PathMap::default();
                    let mut ignored_starters: HashMap<PathRoot, PendingStarter> = HashMap::default();
                    self.apply_term_action(
                        &starter_shape,
                        &AcceptCondition::Always,
                        path_root,
                        &ta,
                        ctx.gen_idx,
                        next_gen,
                        report_gen,
                        &mut starter_next_paths,
                        &mut apps,
                        &mut finishes,
                        &mut added,
                        &mut new_cond_root_progresses,
                        &mut observing,
                        &mut ignored_starters,
                    );
                    if !starter_next_paths.is_empty() {
                        next_paths.insert(path_root, starter_next_paths);
                    } else {
                        starter_died!(path_root, starter_shape, next_paths, ctx);
                    }
                } else {
                    starter_died!(path_root, starter_shape, next_paths, ctx);
                }
            }
        }

        // ----- step 4: cond path finish detection -----
        let mut cond_path_finishes: HashMap<PathRoot, AcceptCondition> = HashMap::default();
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
        let mut late_cond_path_finishes: HashMap<PathRoot, AcceptCondition> = HashMap::default();
        for (root, cond) in &late_pf_progresses {
            if *root != ctx.main_root {
                late_cond_path_finishes.insert(*root, cond.clone());
            }
        }

        // ----- step 5: evolve every condition in every path -----
        let mut active_cond_roots = std::mem::take(&mut scratch.active_cond_roots);
        active_cond_roots.clear();
        active_cond_roots.extend(next_paths.keys().copied());
        let mut paths_evolved = std::mem::take(&mut scratch.paths_evolved);
        paths_evolved.clear();
        for (root, pm) in &next_paths {
            // 재사용 pool 에서 빈 inner map 을 꺼낸다 (per-root × step 마다 새로
            // 할당하던 churn 제거). scratch 접근은 split borrow — next_paths/
            // paths_evolved 는 mem::take 로 owned local 이라 scratch 와 disjoint.
            let mut result = scratch.take_path_map();
            for (shape, cond) in pm {
                let evolved = evolve_accept_condition(
                    cond,
                    &cond_path_finishes,
                    &late_cond_path_finishes,
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
            } else {
                // 빈 결과는 pool 로 반환 (capacity 재사용).
                scratch.recycle_path_map(result);
            }
        }

        // ----- mgroup4 (§2.1): step 5 evolve 뒤, main root 만, per-gen 재파티션 병합 -----
        // n=1 이면 no-op (병합 패스 미실행 — 제로코스트). n≥2 면 window 안 한 위치만 상이한
        // 형제 shape 들을 interior group 으로 접는다. 병합된 group 은 여러 gen 을 살며,
        // reduce 가 group 노드에 도달(apply_edge_action 멤버 분열)하거나 window-exit(A3)
        // 시에만 분열 → 출력 불변 (parser_diff 오라클). Kotlin Mgroup4Parser.kt 참조.
        if self.interior_group_max_depth >= 2 {
            if let Some(main_evolved) = paths_evolved.get(&ctx.main_root) {
                if main_evolved.len() >= 2 {
                    let re =
                        self.merge_interior_groups(main_evolved, self.interior_group_max_depth, next_gen);
                    // 병합 전 map 은 pool 로 회수 (capacity 재사용).
                    if let Some(old) = paths_evolved.insert(ctx.main_root, re) {
                        scratch.recycle_path_map(old);
                    }
                }
            }
        }
        if self.stats_enabled {
            if let Some(main_map) = paths_evolved.get(&ctx.main_root) {
                self.record_shape_stats(main_map);
            }
        }

        let main_paths_evolved =
            paths_evolved.get(&ctx.main_root).cloned().unwrap_or_default();

        // ----- step 6: prune unreferenced cond paths -----
        // referenced_roots: 런타임 생존 규칙 — 조건 참조 root + observing 의 dot anchor
        //   (+ lookahead 는 tip/parent anchor 도 — 구 규약의 드리프트 쌍).
        // reported_cond_roots: 보고 대상 — m2 trackings 의 narrow 규칙
        //   (조건 참조 root + observing 의 parent-gen anchor 만).
        // next_paths 의 inner map 들은 step5 에서 읽기만 하고 이후 필요 없다 —
        // 다음 step 진입 시 clear() 로 drop 되기 전에 inner PathMap 들을 pool 로
        // 회수해 재사용한다 (outer HashMap 자체는 scratch 로 이월).
        for (_root, pm) in next_paths.drain() {
            scratch.recycle_path_map(pm);
        }
        let mut referenced_roots = std::mem::take(&mut scratch.referenced_roots);
        referenced_roots.clear();
        // 체인 노드별 기여는 그 노드만의 순수 함수 (gen 들은 생성 시점에 동결,
        // parent_gen fallback 은 상수 main-root start gen) 이고 walk 는 항상
        // tip→root 로 완주하므로, 이미 방문한 노드를 만나면 그 노드와 조상 전부의
        // 기여가 끝났다는 뜻 — 거기서 끊는다. 결과 집합은 동일 (set 은 어차피
        // 값 dedup). shapes × depth × |observing| 회 insert → distinct 노드 수로 축소.
        let mut walked_nodes = std::mem::take(&mut scratch.walked_nodes);
        walked_nodes.clear();
        let mut reported_cond_roots: HashSet<PathRoot> = HashSet::default();
        for pm in paths_evolved.values() {
            for (shape, cond) in pm {
                cond.referenced_roots().for_each(|r| {
                    referenced_roots.insert(*r);
                    reported_cond_roots.insert(*r);
                });
                let mut mp = shape.milestone_path.clone();
                while let Some(node) = mp {
                    if !walked_nodes.insert(Rc::as_ptr(&node) as usize) {
                        break;
                    }
                    for sid in node.observing_cond_symbol_ids.iter().copied() {
                        // span-정규화 key anchor — 이 milestone 의 dot(= gen - 1, 부착은
                        // 항상 dot+1)에서 시작한 watcher (조건이 emit 되기 전 중간 step
                        // 들의 생존 보장).
                        referenced_roots.insert(PathRoot::new(sid, node.gen_idx - 1));
                        reported_cond_roots.insert(PathRoot::new(sid, node.gen_idx - 1));
                        let parent_gen =
                            node.parent.as_ref().map(|p| p.gen_idx).unwrap_or(ctx.main_root.start_gen);
                        reported_cond_roots.insert(PathRoot::new(sid, parent_gen));
                        // bounded (except/join/longest) 의 미래 조건 anchor 는 dot 뿐 —
                        // term 조건은 MID(같은 step 에 starter 로 시동), edge 조건은
                        // GRAND(=dot) 로만 anchoring (remapEdgeCondGens; 실측
                        // scanCondAnchorTags: mulang 전 템플릿에서 bounded CURR anchor
                        // 0건). tip(gen)/parent anchor 로만 살아남는 bounded 워처가
                        // 인접-gen 중복 root 의 원인 (watcher_anchor_dedup.md §1).
                        // lookahead 는 edge 조건이 CURR/MID 태그를 유지하므로 구 규약의
                        // 3 anchor 그대로 — 드리프트 anchor 와 쌍인 자기일관 시스템.
                        if self.plain.lookahead_cond_symbols.contains(&sid) {
                            referenced_roots.insert(PathRoot::new(sid, node.gen_idx));
                            referenced_roots.insert(PathRoot::new(sid, parent_gen));
                        }
                    }
                    mp = node.parent.clone();
                }
            }
        }
        // paths_filtered 는 반환 ctx.paths (live state) 가 되므로 scratch 로 재사용할
        // 수 없다 — step 밖으로 새는 컨테이너다. 대신 paths_evolved 를 drain 해
        // outer 할당을 scratch 로 되돌리고, 걸러진 root 의 inner map 은 pool 로 회수.
        let mut paths_filtered: HashMap<PathRoot, PathMap> = HashMap::default();
        for (root, pm) in paths_evolved.drain() {
            if root == ctx.main_root || referenced_roots.contains(&root) {
                paths_filtered.insert(root, pm);
            } else {
                scratch.recycle_path_map(pm);
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

        // record 는 저장 시점에 필터+dedup — 보고 대상이 아닌 cond root 의 record 와
        // 완전 중복 record 를 버린다 (대형 입력의 메모리 누적 방지).
        let main_root = ctx.main_root;
        let prev_reported: HashSet<PathRoot> = ctx
            .history
            .last()
            .map(|e| e.reported_cond_roots.clone())
            .unwrap_or_default();
        let reportable = |r: PathRoot| {
            r == main_root || reported_cond_roots.contains(&r) || prev_reported.contains(&r)
        };
        // drain() 으로 dedup — apps/finishes/added 의 Vec 할당을 유지해 scratch 로
        // 되돌린다 (dedup 결과는 HistoryEntry 로 이동하므로 별도 fresh Vec).
        let mut apps_dedup: Vec<ActionApplication> = Vec::new();
        for app in apps.drain(..) {
            if reportable(app.root) && !apps_dedup.contains(&app) {
                apps_dedup.push(app);
            }
        }
        let mut finishes_dedup: Vec<FinishedKernelRecord> = Vec::new();
        for rec in finishes.drain(..) {
            if reportable(rec.root) && !finishes_dedup.contains(&rec) {
                finishes_dedup.push(rec);
            }
        }
        let mut added_dedup: Vec<AddedKernelRecord> = Vec::new();
        for rec in added.drain(..) {
            if reportable(rec.root) && !added_dedup.contains(&rec) {
                added_dedup.push(rec);
            }
        }

        let history_entry = HistoryEntry {
            action_applications: apps_dedup,
            finished_kernels: finishes_dedup,
            added_kernels: added_dedup,
            main_root_finish: root_progresses.get(&ctx.main_root).cloned(),
            cond_path_finishes,
            late_cond_path_finishes,
            active_cond_paths: active_cond_paths_for_history.clone(),
            reported_cond_roots,
        };

        // scratch 로 꺼냈던 owned 컬렉션들을 되돌린다 (다음 step 재사용). drain 된
        // 컨테이너는 비어있고 capacity 만 유지된 상태. 다음 step 진입 시 clear() 되므로
        // 여기서 별도 clear 불필요.
        scratch.next_paths = next_paths;
        scratch.apps = apps;
        scratch.finishes = finishes;
        scratch.added = added;
        scratch.observing = observing;
        scratch.root_progresses = root_progresses;
        scratch.late_pf_progresses = late_pf_progresses;
        scratch.cond_root_starters_from_term = cond_root_starters_from_term;
        scratch.all_observing = all_observing;
        scratch.new_cond_roots = new_cond_roots;
        scratch.new_cond_roots_sorted = new_cond_roots_sorted;
        scratch.new_cond_root_progresses = new_cond_root_progresses;
        scratch.paths_evolved = paths_evolved;
        scratch.active_cond_roots = active_cond_roots;
        scratch.referenced_roots = referenced_roots;
        scratch.walked_nodes = walked_nodes;

        let ParsingCtx { mut history, mut ever_seen_cond_roots, root_report_gens, .. } = ctx;
        history.push(history_entry);
        ever_seen_cond_roots.extend(active_cond_paths_for_history);

        Ok(ParsingCtx {
            gen_idx: next_gen,
            line: next_line,
            col: next_col,
            main_root,
            paths: paths_filtered,
            history,
            ever_seen_cond_roots,
            root_report_gens,
            term_action_cache: term_cache,
            step_scratch: scratch,
        })
    }

    /// Drive every character of `text` through `parse_step`. Mirrors
    /// `Mgroup4Parser.kt:701-707`.
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
    /// accept 판정은 main root 의 progress 조건 전용 채널(main_root_finish)만 사용 —
    /// finished_kernels 는 보고 전용. Mirrors Kotlin `isAccepted`.
    pub fn is_accepted(&self, ctx: &ParsingCtx) -> bool {
        let Some(last_entry) = ctx.history.last() else { return false };
        let Some(cond) = &last_entry.main_root_finish else { return false };
        let end_late = self.end_of_input_late_fins(ctx);
        let evaluator = super::record_cond::RecordConditionEvaluator::new(&ctx.history, &end_late);
        evaluator.evaluate(cond, ctx.history.len() as i32 - 1)
    }

    /// 입력 끝에서 아직 살아있는 cond path 들의 zero-width possible-finish —
    /// 죽음이 더는 step 으로 관찰되지 않으므로 마지막 gen 에 끝나는 finish 들을
    /// 한 번 쓸어 모아 최종 평가의 가상 late step 으로 쓴다. Kotlin
    /// `endOfInputLateFins` 대응.
    fn end_of_input_late_fins(&self, ctx: &ParsingCtx) -> HashMap<PathRoot, AcceptCondition> {
        let mut result: HashMap<PathRoot, AcceptCondition> = HashMap::default();
        for (root, path_map) in &ctx.paths {
            if *root == ctx.main_root {
                continue;
            }
            for (shape, cond) in path_map {
                let Some(mg) = self.plain.milestone_groups.get(&shape.tip_group_id) else {
                    continue;
                };
                for pf in &mg.possible_finishes {
                    if pf.symbol_id == root.symbol_id {
                        let prev_gen = shape
                            .milestone_path
                            .as_ref()
                            .map(|mp| mp.gen_idx)
                            .unwrap_or(root.start_gen);
                        let pf_cond = build_condition(
                            &pf.accept_condition,
                            prev_gen,
                            ctx.gen_idx,
                            ctx.gen_idx + 1,
                            prev_gen,
                        );
                        let combined = AcceptCondition::and_from([cond.clone(), pf_cond]);
                        if !matches!(combined, AcceptCondition::Never) {
                            or_merge(&mut result, *root, combined);
                        }
                    }
                }
            }
        }
        result
    }

    /// One `KtlibKernel` set per generation. Lazily resolves the recorded
    /// action applications: conditions via runtime bindings (replay-evaluated),
    /// kernel coordinates via report bindings. Mirrors Kotlin `kernelsHistory`.
    pub fn kernels_history(&self, ctx: &ParsingCtx) -> Vec<HashSet<KtlibKernel>> {
        let end_late = self.end_of_input_late_fins(ctx);
        // record 조건 평가는 replay 재생 대신 leaf-직접 조회 + 메모 (record_cond).
        // evaluator 의 인덱스/메모는 이 호출 로컬 — 파서 인스턴스는 Send+Sync 유지.
        let evaluator = super::record_cond::RecordConditionEvaluator::new(&ctx.history, &end_late);
        let mut out = Vec::with_capacity(ctx.history.len());
        // Per-gen ActionApplication dedup. The same (actions template, gen
        // bindings, condition) recurs many times within a gen (up to 11x on
        // jar.bbx); each redundant app rebuilds+re-evaluates+re-emits the same
        // kernels into the same set. Skipping duplicates is output-invariant:
        // the kernel coordinates are a pure function of the app's fields (which
        // are equal for a duplicate) and every emit is a set `insert`, which is
        // idempotent — so a dropped duplicate would only re-insert kernels
        // already present. Bucket on a cheap integer key (`AppKey`) and
        // disambiguate the rare differing-condition case inside the bucket by
        // value (`SmallCondSet`), so composite condition trees are never hashed.
        // The map is allocated once and cleared per gen.
        let mut seen: HashMap<AppKey, SmallCondSet> = HashMap::default();
        for (gen_idx, entry) in ctx.history.iter().enumerate() {
            let gen_idx = gen_idx as i32;
            let mut kernels: HashSet<KtlibKernel> = HashSet::default();
            seen.clear();
            for app in &entry.action_applications {
                let key = AppKey {
                    actions: Arc::as_ptr(&app.actions) as usize,
                    rt_curr: app.rt_curr,
                    rt_mid: app.rt_mid,
                    next: app.next,
                    rt_grand: app.rt_grand,
                    rep_curr: app.rep_curr,
                    rep_mid: app.rep_mid,
                    rep_grand: app.rep_grand,
                };
                let bucket = seen.entry(key).or_default();
                if bucket.contains(&app.condition) {
                    continue; // identical app already materialized this gen
                }
                bucket.push(app.condition.clone());
                // edge action 은 구동 조건으로 전체 게이팅.
                if !matches!(app.condition, AcceptCondition::Always)
                    && !evaluator.evaluate(&app.condition, gen_idx)
                {
                    continue;
                }
                let pa = &app.actions;
                for finished in &pa.finished {
                    let cond_tpl = finished
                        .finish_condition
                        .as_ref()
                        .expect("FinishedKernelTemplate.finish_condition missing");
                    let cond =
                        build_condition(cond_tpl, app.rt_curr, app.rt_mid, app.next, app.rt_grand);
                    if evaluator.evaluate(&cond, gen_idx) {
                        let begin = resolve_gen_i32(
                            finished.start_gen,
                            app.rep_curr,
                            app.rep_mid,
                            app.next,
                            app.rep_grand,
                        );
                        kernels.insert(KtlibKernel {
                            symbol_id: finished.symbol_id,
                            pointer: finished.pointer,
                            begin_gen: begin,
                            end_gen: gen_idx,
                        });
                    }
                }
                // pa.progressed 는 방출하지 않는다 — added 가 동일 kernel 을 조건과 함께 커버.
                for added in &pa.added {
                    let cond_tpl = added
                        .accept_condition
                        .as_ref()
                        .expect("AddedKernelTemplate.accept_condition missing");
                    let cond =
                        build_condition(cond_tpl, app.rt_curr, app.rt_mid, app.next, app.rt_grand);
                    if evaluator.evaluate(&cond, gen_idx) {
                        kernels.insert(KtlibKernel {
                            symbol_id: added.symbol_id,
                            pointer: added.pointer,
                            begin_gen: resolve_gen_i32(
                                added.start_gen,
                                app.rep_curr,
                                app.rep_mid,
                                app.next,
                                app.rep_grand,
                            ),
                            end_gen: resolve_gen_i32(
                                added.end_gen,
                                app.rep_curr,
                                app.rep_mid,
                                app.next,
                                app.rep_grand,
                            ),
                        });
                    }
                }
            }
            for rec in &entry.finished_kernels {
                if evaluator.evaluate(&rec.condition, gen_idx) {
                    kernels.insert(KtlibKernel {
                        symbol_id: rec.kernel.symbol_id,
                        pointer: rec.kernel.pointer,
                        begin_gen: rec.kernel.gen_idx,
                        end_gen: gen_idx,
                    });
                }
            }
            for rec in &entry.added_kernels {
                if evaluator.evaluate(&rec.condition, gen_idx) {
                    kernels.insert(KtlibKernel {
                        symbol_id: rec.symbol_id,
                        pointer: rec.pointer,
                        begin_gen: rec.begin_gen,
                        end_gen: rec.end_gen,
                    });
                }
            }
            out.push(kernels);
        }
        out
    }

    /// Apply one `TermAction` to a single (shape, cond) pair, accumulating
    /// outputs. Mirrors `Mgroup4Parser.kt:180-278`.
    fn apply_term_action(
        &self,
        old_shape: &PathShape,
        old_condition: &AcceptCondition,
        path_root: PathRoot,
        term_action: &TermActionPlain,
        mid_gen: i32,
        gen_idx: i32,
        // 보고 전용 root anchor (same-input starter 는 startGen-1).
        root_report_gen: i32,
        next_paths_out: &mut PathMap,
        apps_out: &mut Vec<ActionApplication>,
        finishes_out: &mut Vec<FinishedKernelRecord>,
        added_out: &mut Vec<AddedKernelRecord>,
        root_progresses_out: &mut HashMap<PathRoot, AcceptCondition>,
        observing_out: &mut HashSet<i32>,
        cond_root_starters_out: &mut HashMap<PathRoot, PendingStarter>,
    ) {
        // A3 window-exit (spec §2.3): 이 term action 이 descend (replace_and_appends) 를
        // 포함하고, shape 에 group 노드가 있으며, descend 로 그 노드의 depth 가 n 을 넘게
        // 되면 (post-descend depth > n) descend 전에 멤버별 분열해 각 멤버에 대해 term
        // action 을 재적용한다. n=1 이면 group 자체가 없으므로 이 분기는 안 탄다.
        // depth 계산은 tip 근처 n+1 칸만 순회 (§1.3 (b)) — group 이 window 안이면 유계.
        if self.interior_group_max_depth >= 2 && !term_action.replace_and_appends.is_empty() {
            let g_depth = Self::group_depth_near_tip(
                &old_shape.milestone_path,
                self.interior_group_max_depth,
            );
            if g_depth >= 0 && g_depth + 1 > self.interior_group_max_depth {
                if self.stats_enabled {
                    self.stats
                        .window_exit_splits
                        .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                }
                for member_shape in Self::explode_shape_fully(old_shape) {
                    self.apply_term_action(
                        &member_shape,
                        old_condition,
                        path_root,
                        term_action,
                        mid_gen,
                        gen_idx,
                        root_report_gen,
                        next_paths_out,
                        apps_out,
                        finishes_out,
                        added_out,
                        root_progresses_out,
                        observing_out,
                        cond_root_starters_out,
                    );
                }
                return;
            }
        }

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
        // 보고 좌표 바인딩 — m2 term genMap {0→mgroup.gen(갱신된 tip 부착 gen), 1→gen-1, 2→gen}.
        let report_parent_gen = old_shape
            .milestone_path
            .as_ref()
            .map(|mp| mp.report_gen)
            .unwrap_or(root_report_gen);
        let report_grand_gen = old_shape
            .milestone_path
            .as_ref()
            .map(|mp| mp.milestone_report_gen)
            .unwrap_or(root_report_gen);

        if let Some(pa) = &term_action.parsing_actions {
            // 보고는 lazy — 액션 참조와 바인딩만 기록 (kernels_history 가 해석).
            apps_out.push(ActionApplication {
                actions: Arc::clone(pa),
                root: path_root,
                rt_curr: parent_gen,
                rt_mid: mid_gen,
                next: gen_idx,
                rt_grand: grand_gen,
                rep_curr: report_parent_gen,
                rep_mid: mid_gen,
                rep_grand: report_grand_gen,
                condition: AcceptCondition::Always,
            });
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
            // 새 tip group 은 이번 gen 에 부착; replace milestone 의 m2 식 gen 은
            // 직전 tip 의 (갱신된) 부착 gen.
            let new_mp = Rc::new(MilestonePath::new(
                gen_idx,
                replace_kernel,
                old_shape.milestone_path.clone(),
                Arc::clone(&rea.append.observing_cond_symbol_ids),
                gen_idx,
                report_parent_gen,
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
                let Some(key) = starter_key_of(starter.key_gen, mid_gen, gen_idx) else {
                    continue;
                };
                cond_root_starters_out.insert(
                    PathRoot::new(starter.symbol_id, key),
                    PendingStarter {
                        milestone_group_id: starter.milestone_group_id,
                        same_input: starter.same_input,
                    },
                );
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
                        kernel: Kernel::new(path_root.symbol_id, 1, root_report_gen),
                        condition: combined.clone(),
                        root: path_root,
                    });
                    // 보고용: root 의 ptr0 init kernel (m2 progRootMilestone 의 ptr0 대응).
                    added_out.push(AddedKernelRecord {
                        symbol_id: path_root.symbol_id,
                        pointer: 0,
                        begin_gen: root_report_gen,
                        end_gen: root_report_gen,
                        condition: combined,
                        root: path_root,
                    });
                }
                Some(parent_path) => {
                    // A2: reduce 가 tip 인접 노드(parent_path = old_shape.milestone_path)로 pop.
                    // 이 노드가 group 이면 멤버별 template 로 tipEdge 조회가 갈리므로 여기서
                    // 분열 (spec item 2). singleton 이면 1개 멤버로 그대로 처리 (제로코스트).
                    // apply_edge_action 의 invariant: parent_path 는 항상 singleton (group 은
                    // 이 진입점과 midEdge 진입점에서 미리 멤버 singleton 으로 펼침).
                    let parent_members = Self::member_singletons_for_edge(parent_path);
                    if parent_members.len() > 1 && self.stats_enabled {
                        self.stats
                            .reduce_splits
                            .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                    }
                    for pm in &parent_members {
                        let key = (
                            pm.milestone.kernel_template(),
                            rap.replace_milestone_group_id,
                        );
                        let Some(tip_edge_action) = self.tip_edge_actions.get(&key).cloned() else {
                            continue;
                        };
                        let grand_parent_gen = pm
                            .parent
                            .as_ref()
                            .map(|p| p.gen_idx)
                            .unwrap_or(path_root.start_gen);
                        self.apply_edge_action(
                            pm,
                            &tip_edge_action,
                            path_root,
                            &combined,
                            grand_parent_gen,
                            pm.gen_idx,
                            gen_idx,
                            // m2 tip edge = (parent milestone @ m2 gen) -> (tip group @ 갱신된 부착 gen)
                            pm.milestone_report_gen,
                            pm.report_gen,
                            root_report_gen,
                            next_paths_out,
                            apps_out,
                            finishes_out,
                            added_out,
                            root_progresses_out,
                            observing_out,
                            cond_root_starters_out,
                        );
                    }
                }
            }
        }
    }

    /// Apply an `EdgeAction` reduction. Mirrors `Mgroup4Parser.kt:285-377`.
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
        // 보고 좌표 바인딩 — m2 edge genMap {0→edge.first.gen, 1→edge.second.gen, 2→gen}.
        report_curr_gen: i32,
        report_mid_gen: i32,
        root_report_gen: i32,
        next_paths_out: &mut PathMap,
        apps_out: &mut Vec<ActionApplication>,
        finishes_out: &mut Vec<FinishedKernelRecord>,
        added_out: &mut Vec<AddedKernelRecord>,
        root_progresses_out: &mut HashMap<PathRoot, AcceptCondition>,
        observing_out: &mut HashSet<i32>,
        cond_root_starters_out: &mut HashMap<PathRoot, PendingStarter>,
    ) {
        // edge action next_gen table:
        //   CURR  = grand_parent_gen   (Kotlin's "currGen" param)
        //   MID   = parent_gen
        //   NEXT  = gen_idx
        //   GRAND = grand_grand_parent_gen
        // GRAND = parent 의 dot gen. m3 의 rea 부착은 항상 dot+1 (same-input 부착 규약)
        // 이므로 균일하게 parent_gen - 1. bounded/longest 조건의 span-시작 anchor
        // (생성기의 remapEdgeCondGens Curr/Mid→Grand) 가 이 값을 참조한다.
        let grand_grand_parent_gen = parent_gen - 1;
        let report_grand_gen = parent_path.milestone_report_gen;

        if let Some(pa) = &edge_action.parsing_actions {
            // 보고는 lazy. edge 적용은 그것을 구동한 runtime 조건으로 전체 게이팅됨
            // (m2 kernelsHistory 의 progressedKgroups/progressedKernels 조건 게이트 대응).
            apps_out.push(ActionApplication {
                actions: Arc::clone(pa),
                root: path_root,
                rt_curr: grand_parent_gen,
                rt_mid: parent_gen,
                next: gen_idx,
                rt_grand: grand_grand_parent_gen,
                rep_curr: report_curr_gen,
                rep_mid: report_mid_gen,
                rep_grand: report_grand_gen,
                condition: prev_condition.clone(),
            });
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
            // 새 tip group 은 이번 gen 에 재부착 — 보고용 report_gen 갱신 (런타임 gen 불변).
            let new_parent_path = parent_path
                .with_observing_and_report_gen(Arc::clone(&append.observing_cond_symbol_ids), gen_idx);
            add_path(
                next_paths_out,
                PathShape::new(Some(new_parent_path), append.milestone_group_id),
                combined,
            );
            for sid in append.observing_cond_symbol_ids.iter().copied() {
                observing_out.insert(sid);
            }
            for starter in &append.cond_root_starters {
                // edge frame: 과거 경계(CURR) watcher 는 그 시점에 이미 등록됨 — skip.
                let Some(key) = starter_key_of(starter.key_gen, parent_gen, gen_idx) else {
                    continue;
                };
                cond_root_starters_out.insert(
                    PathRoot::new(starter.symbol_id, key),
                    PendingStarter {
                        milestone_group_id: starter.milestone_group_id,
                        same_input: starter.same_input,
                    },
                );
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
                            kernel: Kernel::new(path_root.symbol_id, 1, root_report_gen),
                            condition: combined.clone(),
                            root: path_root,
                        });
                        added_out.push(AddedKernelRecord {
                            symbol_id: path_root.symbol_id,
                            pointer: 0,
                            begin_gen: root_report_gen,
                            end_gen: root_report_gen,
                            condition: combined,
                            root: path_root,
                        });
                    }
                    Some(grand_parent) => {
                        // A2: reduce 가 grandParent 로 pop. grandParent 가 group 이면 멤버별
                        // template 로 midEdge 조회가 갈리므로 여기서 분열 (spec item 2). 각 멤버
                        // singleton 을 새 frame 의 parent_path 로 재귀 → apply_edge_action 의
                        // parent_path 는 항상 singleton (invariant 유지). parent_path (현 frame)
                        // 는 이미 singleton 이므로 .milestone.kernel_template() 안전.
                        let grand_members = Self::member_singletons_for_edge(grand_parent);
                        if grand_members.len() > 1 && self.stats_enabled {
                            self.stats
                                .reduce_splits
                                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                        }
                        for gm in &grand_members {
                            let key = (
                                gm.milestone.kernel_template(),
                                parent_path.milestone.kernel_template(),
                            );
                            let Some(mid_edge) = self.mid_edge_actions.get(&key).cloned() else {
                                continue;
                            };
                            let grand_grand_parent_gen_2 = gm
                                .parent
                                .as_ref()
                                .map(|p| p.gen_idx)
                                .unwrap_or(path_root.start_gen);
                            self.apply_edge_action(
                                gm,
                                &mid_edge,
                                path_root,
                                &combined,
                                grand_grand_parent_gen_2,
                                gm.gen_idx,
                                gen_idx,
                                // m2 mid edge = (grandParent milestone @ m2 gen) -> (parent milestone @ m2 gen)
                                gm.milestone_report_gen,
                                parent_path.milestone_report_gen,
                                root_report_gen,
                                next_paths_out,
                                apps_out,
                                finishes_out,
                                added_out,
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

    // === mgroup4 interior milestone group — merge / split (Phase A → Rust) ===
    // 이식 레퍼런스: mgroup4/parser/kotlin/.../Mgroup4Parser.kt (같은 이름 함수들).
    // Kotlin 의 지뢰 주석에 상응하는 주석을 여기 남긴다.

    /// 한 milestone chain 을 root..tip 순서 배열로 편다. index 0 = root-most.
    /// Kotlin `chainToList`.
    fn chain_to_list(tip: &Option<Rc<MilestonePath>>) -> Vec<Rc<MilestonePath>> {
        let mut rev = Vec::new();
        let mut cur = tip.clone();
        while let Some(node) = cur {
            cur = node.parent.clone();
            rev.push(node);
        }
        rev.reverse();
        rev
    }

    /// A3 (§1.3 (b)): tip 에서 위로 순회하며 group 노드의 tip-relative depth 를 찾는다.
    /// depth 1 = tipGroupId (여기 인자 tip 은 tip-most interior = depth 2 부터). 없으면 -1.
    /// max_scan+1 칸만 순회 — group 이 window(n) 안이면 유계. Kotlin `groupDepthNearTip`.
    fn group_depth_near_tip(tip: &Option<Rc<MilestonePath>>, max_scan: i32) -> i32 {
        let mut cur = tip.clone();
        let mut depth = 2; // tip-most interior 노드
        let mut steps = 0;
        while let Some(node) = cur {
            if steps > max_scan + 1 {
                break;
            }
            if node.group_members.is_some() {
                return depth;
            }
            cur = node.parent.clone();
            depth += 1;
            steps += 1;
        }
        -1
    }

    fn shape_has_group(shape: &PathShape) -> bool {
        let mut mp = shape.milestone_path.clone();
        while let Some(node) = mp {
            if node.group_members.is_some() {
                return true;
            }
            mp = node.parent.clone();
        }
        false
    }

    /// step 5 뒤 main root 병합 (§2.1). d=2..n greedy — Phase 0 merge_greedy 동형.
    /// 병합 조건: 같은 length·tip·condition, window 밖 노드 전부 동일, window 안 정확히
    /// depth-d 한 위치만 상이. 추가로 depth-d 노드의 gen·observing 이 멤버 간 동일해야 함
    /// (MilestonePath.gen 은 미래 condition anchor). 파티션은 캐시된 rolling hash 버킷 후
    /// 비마스크 부분의 실 equals 통과분만 접는다 (해시 충돌 방어).
    /// Kotlin `mergeInteriorGroups`.
    pub fn merge_interior_groups(
        &self,
        main_path_map: &PathMap,
        n: i32,
        cur_gen: i32,
    ) -> PathMap {
        use std::sync::atomic::Ordering::Relaxed;
        let profiling = self.merge_profile;
        let t_merge_start = if profiling { Some(std::time::Instant::now()) } else { None };
        // 아직 접히지 않은 후보들 (접힌 shape 는 제거). spec item 6: 체인에 이미 group
        // 노드가 있는 shape 는 새 병합 후보에서 제외 (2중 group 은 스코프 아님) — 그런
        // shape 는 병합 없이 그대로 결과로 통과. Kotlin 은 LinkedHashMap 으로 결정적
        // 순서를 유지하나, 최종 group 멤버는 fold 에서 정렬되고 어떤 shape 가 어떤
        // group 으로 접히는지는 멤버 내용 기반이라 iteration 순서와 무관 (출력 불변).
        let mut remaining: PathMap = HashMap::default();
        let mut merged: PathMap = HashMap::default();
        for (shape, cond) in main_path_map {
            if Self::shape_has_group(shape) {
                if self.stats_enabled {
                    self.stats.skip_existing_group.fetch_add(1, Relaxed);
                }
                merge_into(&mut merged, shape.clone(), cond.clone());
                continue;
            }
            remaining.insert(shape.clone(), cond.clone());
        }

        let mut d = 2;
        while d <= n {
            // depth d 노드가 존재하는 후보를 **구조 키**로 버킷팅. 구조 키는 condition 과
            // diff 노드의 gen/observing 을 **제외** — 같은 버킷 안에서 "구조는 병합 가능하나
            // condition/gen·observing 불일치로 거부된" 쌍을 세어 H5 신호를 낸다. 실제 병합은
            // strict equals (merge_verdict_at_depth). 버킷 키 = combine(1, L, tip, idx) 뒤
            // "idx 제외 전 노드"의 node-local rolling hash. prefix 성분 [0..idx-1] 은
            // prefix_hash_cached O(1), suffix 성분 [idx+1..L-1] 은 d-2 칸만 순회 (window 유계).
            // 키의 정확한 비트값은 무의미 — 실병합이 strict equals 로 재검증하므로 충돌해도
            // 정확성 불변, 분포만 좋으면 됨.
            let t_bucket0 = if profiling { Some(std::time::Instant::now()) } else { None };
            let mut buckets: HashMap<u64, Vec<MergeCandidate>> = HashMap::default();
            for (shape, cond) in remaining.iter() {
                let tip = shape.milestone_path.clone();
                let l = tip.as_ref().map(|t| t.chain_depth_cached()).unwrap_or(0);
                if d > l {
                    continue; // depth d 가 chain 밖
                }
                let idx = l - (d - 1); // root..tip 배열에서 depth-d 노드의 index
                if idx < 0 {
                    continue;
                }
                // diff 노드 = tip 에서 (d-2) 칸 위. 순회 중 tip-side(idx+1..L-1) 노드도 모은다.
                let mut diff_node = tip.clone();
                let mut steps_up = d - 2;
                // suffix rolling: tip-side 노드를 tip→root 순으로 담아 뒤에서 역순으로 접는다.
                let mut suffix_nodes: Vec<Rc<MilestonePath>> = Vec::new();
                while steps_up > 0 {
                    match diff_node {
                        Some(node) => {
                            let parent = node.parent.clone();
                            suffix_nodes.push(node);
                            diff_node = parent;
                            steps_up -= 1;
                        }
                        None => break,
                    }
                }
                let Some(diff_node) = diff_node else { continue };
                if diff_node.group_members.is_some() {
                    continue; // group-of-group 없음
                }
                let prefix_node = diff_node.parent.clone(); // 노드 [0..idx-1] 의 마지막
                let mut h: u64 = 1;
                h = h.wrapping_mul(31).wrapping_add(l as u64);
                h = h.wrapping_mul(31).wrapping_add(shape.tip_group_id as u64);
                h = h.wrapping_mul(31).wrapping_add(idx as u64);
                // prefix 성분 [0..idx-1]: prefix_hash_cached (seed 1 rolling) — O(1).
                let prefix_rolling = prefix_node.as_ref().map(|p| p.prefix_hash_cached()).unwrap_or(1);
                h = h.wrapping_mul(31).wrapping_add(prefix_rolling);
                // suffix 성분 [idx+1..L-1]: tip→root 로 모았으니 역순(root쪽→tip쪽)으로 접는다.
                for node in suffix_nodes.iter().rev() {
                    h = h.wrapping_mul(31).wrapping_add(node.node_local_hash_cached());
                }
                buckets
                    .entry(h)
                    .or_default()
                    .push(MergeCandidate::new(shape.clone(), cond.clone(), tip, l));
            }
            if let Some(t) = t_bucket0 {
                self.stats.merge_nanos[0].fetch_add(t.elapsed().as_nanos() as i64, Relaxed);
            }

            // 접힌 shape 목록 (remaining 에서 제거 대상) 과 새 group shape 를 모은다.
            let mut to_remove: Vec<PathShape> = Vec::new();
            let mut folded: Vec<(PathShape, AcceptCondition)> = Vec::new();
            for bucket in buckets.values() {
                if bucket.len() < 2 {
                    continue;
                }
                // 해시 버킷 후 실등가로 그룹핑 (해시 충돌 방어).
                let mut used = vec![false; bucket.len()];
                for i in 0..bucket.len() {
                    if used[i] {
                        continue;
                    }
                    used[i] = true;
                    let mut group: Vec<&MergeCandidate> = Vec::new();
                    let t_verdict0 = if profiling { Some(std::time::Instant::now()) } else { None };
                    for k in (i + 1)..bucket.len() {
                        if used[k] {
                            continue;
                        }
                        match self.merge_verdict_at_depth(&bucket[i], &bucket[k], d) {
                            MergeVerdict::Merge => {
                                if group.is_empty() {
                                    group.push(&bucket[i]);
                                }
                                group.push(&bucket[k]);
                                used[k] = true;
                            }
                            MergeVerdict::RejectCond => {
                                if self.stats_enabled {
                                    self.stats.reject_cond_diff.fetch_add(1, Relaxed);
                                }
                            }
                            MergeVerdict::RejectGenObs => {
                                if self.stats_enabled {
                                    self.stats.reject_gen_obs_diff.fetch_add(1, Relaxed);
                                }
                            }
                            MergeVerdict::RejectReportCoord => {
                                if self.stats_enabled {
                                    self.stats.reject_report_coord_diff.fetch_add(1, Relaxed);
                                }
                            }
                            MergeVerdict::NotCandidate => {}
                        }
                    }
                    if let Some(t) = t_verdict0 {
                        self.stats.merge_nanos[1].fetch_add(t.elapsed().as_nanos() as i64, Relaxed);
                    }
                    if group.len() >= 2 {
                        let t_fold0 = if profiling { Some(std::time::Instant::now()) } else { None };
                        let (folded_shape, folded_cond) = Self::fold_group(&group, d);
                        if let Some(t) = t_fold0 {
                            self.stats.merge_nanos[2].fetch_add(t.elapsed().as_nanos() as i64, Relaxed);
                        }
                        for m in &group {
                            to_remove.push(m.shape.clone());
                        }
                        folded.push((folded_shape, folded_cond));
                        if self.stats_enabled {
                            if (d as usize) < self.stats.merges_at_depth.len() {
                                self.stats.merges_at_depth[d as usize]
                                    .fetch_add((group.len() - 1) as i64, Relaxed);
                            }
                            self.classify_merge_origin(&group, d, cur_gen);
                        }
                    }
                }
            }
            // consume: 접힌 멤버는 이후 depth 재병합 대상에서 제외.
            for shape in &to_remove {
                remaining.remove(shape);
            }
            // 접힌 group shape 를 결과에 (dedup — 같은 group shape 가 이미 있으면 Or).
            for (shape, cond) in folded {
                merge_into(&mut merged, shape, cond);
            }
            d += 1;
        }
        // 안 접힌 나머지는 그대로.
        for (shape, cond) in remaining.drain() {
            merge_into(&mut merged, shape, cond);
        }
        if let Some(t) = t_merge_start {
            self.stats.merge_nanos[3].fetch_add(t.elapsed().as_nanos() as i64, Relaxed);
        }
        merged
    }

    /// spec item 7 — 병합 기원 분류 (Phase B 설계 데이터). diff 노드의 런타임 anchor gen
    /// 이 현재/직전 gen 이면 creation-mergeable (parserdata 사전 그룹핑 가능), 더 과거면
    /// late-convergence (런타임 packing 필요). Kotlin `classifyMergeOrigin`.
    fn classify_merge_origin(&self, group: &[&MergeCandidate], d: i32, cur_gen: i32) {
        use std::sync::atomic::Ordering::Relaxed;
        if cur_gen < 0 {
            return;
        }
        let chain0 = group[0].chain();
        let l = chain0.len() as i32;
        let idx = l - (d - 1);
        let diff_gen = chain0[idx as usize].gen_idx;
        let collapses = (group.len() - 1) as i64;
        if diff_gen == cur_gen || diff_gen == cur_gen - 1 {
            self.stats.creation_mergeable.fetch_add(collapses, Relaxed);
        } else {
            self.stats.late_convergence.fetch_add(collapses, Relaxed);
        }
    }

    /// 두 후보의 depth-d 병합 판정. 비마스크 부분(체인 다른 노드·tip·length)이 전부 같고
    /// depth-d 노드가 서로 다른 singleton 이면 병합 후보. condition 이 다르면 RejectCond,
    /// gen/observing 이 다르면 RejectGenObs, window 노드 보고 좌표가 다르면
    /// RejectReportCoord, 셋 다 같으면 Merge. Kotlin `mergeVerdictAtDepth`.
    fn merge_verdict_at_depth(
        &self,
        a: &MergeCandidate,
        b: &MergeCandidate,
        d: i32,
    ) -> MergeVerdict {
        if a.shape.tip_group_id != b.shape.tip_group_id {
            return MergeVerdict::NotCandidate;
        }
        if a.length != b.length {
            return MergeVerdict::NotCandidate;
        }
        let a_chain = a.chain();
        let b_chain = b.chain();
        let l = a_chain.len() as i32;
        let idx = l - (d - 1);
        if idx < 0 || idx >= l {
            return MergeVerdict::NotCandidate;
        }
        let an = &a_chain[idx as usize];
        let bn = &b_chain[idx as usize];
        // depth-d 노드: 둘 다 singleton 이어야 (group-of-group 없음).
        if an.group_members.is_some() || bn.group_members.is_some() {
            return MergeVerdict::NotCandidate;
        }
        // 정확히 이 위치만 상이 — milestone kernel 이 서로 달라야 실제 fork.
        if an.milestone == bn.milestone {
            return MergeVerdict::NotCandidate;
        }
        // 나머지 노드(root-side prefix + window 안 다른 위치 + tip-side)를 **node-local** 비교.
        // ★ 지뢰 (Kotlin 과 동일): MilestonePath.eq 는 parent 재귀라 diff 노드 위쪽(tip-side)
        // 노드는 재귀가 diff 노드에 걸려 항상 불일치 → 반드시 node-local(gen/milestone/
        // observing/group)만 비교. group 노드가 다른 위치에 섞여 있으면 오병합 금지
        // (node_local_eq 가 한쪽만 group 인 위치를 이미 불일치 처리).
        for j in 0..(l as usize) {
            if j == idx as usize {
                continue;
            }
            if !node_local_eq(&a_chain[j], &b_chain[j]) {
                return MergeVerdict::NotCandidate;
            }
        }
        // 여기까지 왔으면 구조 병합 후보 — 부착 상태 제약 순서대로 판정.
        // gen/observing 은 diff 위치 노드의 anchor.
        if an.gen_idx != bn.gen_idx
            || an.observing_cond_symbol_ids.len() != bn.observing_cond_symbol_ids.len()
            || an
                .observing_cond_symbol_ids
                .iter()
                .zip(bn.observing_cond_symbol_ids.iter())
                .any(|(x, y)| x != y)
        {
            return MergeVerdict::RejectGenObs;
        }
        if a.cond != b.cond {
            return MergeVerdict::RejectCond;
        }
        // spec item 4: window 노드 (diff 위치 제외 전 노드) 의 보고 좌표가 멤버 간 동일해야
        // 한다 — 분열 재구성이 대표의 window 노드를 복사해 쓰므로 (fold_group tip-side
        // 재구성), 좌표가 갈리면 byte-exact 복원이 깨진다. diff 노드(idx) 자체의
        // milestone_report_gen 은 group 이 멤버별 배열로 보존하므로 제외; 그 외 위치만 검사.
        for j in 0..(l as usize) {
            if j == idx as usize {
                continue;
            }
            let aj = &a_chain[j];
            let bj = &b_chain[j];
            if aj.report_gen != bj.report_gen || aj.milestone_report_gen != bj.milestone_report_gen {
                return MergeVerdict::RejectReportCoord;
            }
        }
        // ★ diff 노드(idx) 의 report_gen (tip 부착 gen) 은 멤버별 배열로 보존하지 않고
        // group 노드의 대표값을 member_singletons_for_edge 가 전 멤버에 쓴다. 따라서 diff
        // 노드의 report_gen 은 멤버 간 동일해야 정확 (milestone_report_gen 과 달리 배열 없음).
        // 실측상 항상 동일(같은 gen 에 같은 tip 부착)이라 이 reject 는 발화하지 않지만,
        // 재구성 정확성의 명시적 계약으로 검사한다.
        if an.report_gen != bn.report_gen {
            return MergeVerdict::RejectReportCoord;
        }
        MergeVerdict::Merge
    }

    /// 병합 그룹을 한 group shape 로 접는다. depth-d 노드를 group 노드로 (멤버 정렬 +
    /// 멤버별 milestone_report_gen 병렬 보관), window 구간(depth d..tip)은 새 인스턴스,
    /// 공유 prefix(depth > d)는 참조 재사용. condition 은 대표 것 그대로.
    /// Kotlin `foldGroup`.
    fn fold_group(group: &[&MergeCandidate], d: i32) -> (PathShape, AcceptCondition) {
        let rep = group[0];
        let rep_chain = rep.chain();
        let l = rep_chain.len() as i32;
        let idx = (l - (d - 1)) as usize;
        // 멤버 kernel 정렬 (canonical: symbolId, pointer, gen). 멤버별 milestone_report_gen 병렬.
        let mut sorted: Vec<&&MergeCandidate> = group.iter().collect();
        sorted.sort_by_key(|m| {
            let node = &m.chain()[idx];
            (node.milestone.symbol_id, node.milestone.pointer, node.milestone.gen_idx)
        });
        let mut members: Vec<Kernel> = Vec::with_capacity(sorted.len());
        let mut member_report_gens: Vec<i32> = Vec::with_capacity(sorted.len());
        for m in &sorted {
            let node = &m.chain()[idx];
            members.push(node.milestone);
            member_report_gens.push(node.milestone_report_gen);
        }
        let diff_node = &rep_chain[idx];
        // 공유 prefix: depth > d (idx 아래) — 대표의 parent 참조 그대로.
        let prefix: Option<Rc<MilestonePath>> = if idx == 0 {
            None
        } else {
            Some(rep_chain[idx - 1].clone())
        };
        // group 노드 (depth d): 대표 milestone = 정렬 첫 멤버. reportGen 은 group 필드에서
        // 복원되므로 파스 상태(gen/milestone/observing/parent)는 정확 — 보고 좌표는 분열 시.
        let mut node = Rc::new(MilestonePath::new_group(
            diff_node.gen_idx,
            members[0],
            prefix,
            Arc::clone(&diff_node.observing_cond_symbol_ids),
            diff_node.report_gen,
            member_report_gens[0],
            Some(members.clone()),
            Some(member_report_gens.clone()),
        ));
        // tip-side window 노드 (depth d-1..1) 는 대표에서 새 인스턴스로 재구성 (parent 만 교체).
        // ★ byte-exact 이려면 이 window 노드들의 보고 좌표가 멤버 간 동일해야 한다 —
        // merge_verdict_at_depth 가 RejectReportCoord 로 강제하므로 대표를 써도 안전.
        for j in (idx + 1)..(l as usize) {
            let orig = &rep_chain[j];
            node = Rc::new(MilestonePath::new_group(
                orig.gen_idx,
                orig.milestone,
                Some(node),
                Arc::clone(&orig.observing_cond_symbol_ids),
                orig.report_gen,
                orig.milestone_report_gen,
                orig.group_members.clone(),
                orig.group_member_report_gens.clone(),
            ));
        }
        (PathShape::new(Some(node), rep.shape.tip_group_id), rep.cond.clone())
    }

    /// A2 멤버 복원 기계 (spec item 3) — group 노드를 멤버별 singleton MilestonePath 로 편다.
    /// singleton 이면 자기 자신 1개 (제로코스트). reduce 진입점(tipEdge/midEdge)에서 이 노드
    /// 하나만 필요 (tip-side 노드는 reduce 가 이미 pop). Kotlin `memberSingletonsForEdge`.
    fn member_singletons_for_edge(node: &Rc<MilestonePath>) -> Vec<Rc<MilestonePath>> {
        let Some(members) = node.group_members.as_ref() else {
            return vec![node.clone()];
        };
        let report_gens = node.group_member_report_gens.as_ref();
        let mut out = Vec::with_capacity(members.len());
        for (i, m) in members.iter().enumerate() {
            out.push(Rc::new(MilestonePath::new_group(
                node.gen_idx,
                *m,
                node.parent.clone(),
                Arc::clone(&node.observing_cond_symbol_ids),
                node.report_gen,
                report_gens.map(|g| g[i]).unwrap_or(node.milestone_report_gen),
                None,
                None,
            )));
        }
        out
    }

    /// A3 window-exit 완전 분열 (spec item 3, 전체 shape 재구성) — 한 group shape 를 멤버별
    /// singleton shape 로 편다. group 노드 자리를 멤버 singleton 으로 바꾸고, tip-side 노드는
    /// parent 만 새 멤버 노드로 바꿔 복사. group 노드 아래(prefix)는 공유 참조.
    /// ★ tip-side 노드는 group 형성 이후 term descend 로 folded shape 위에 쌓인 공유 노드라
    /// 정의상 멤버 공통 (같은 인스턴스) → 복사가 byte-exact. Kotlin `explodeShapeFully`.
    fn explode_shape_fully(shape: &PathShape) -> Vec<PathShape> {
        let chain = Self::chain_to_list(&shape.milestone_path);
        let mut g_idx: i32 = -1;
        for (j, node) in chain.iter().enumerate() {
            if node.group_members.is_some() {
                g_idx = j as i32;
                break;
            }
        }
        if g_idx < 0 {
            return vec![shape.clone()];
        }
        let group_node = &chain[g_idx as usize];
        let member_nodes = Self::member_singletons_for_edge(group_node);
        let mut out = Vec::with_capacity(member_nodes.len());
        for mnode in member_nodes {
            // tip-side (g_idx+1..end) 를 대표 chain 에서 복사, parent 만 멤버 노드로 교체.
            let mut node = mnode;
            for j in ((g_idx + 1) as usize)..chain.len() {
                let orig = &chain[j];
                node = Rc::new(MilestonePath::new_group(
                    orig.gen_idx,
                    orig.milestone,
                    Some(node),
                    Arc::clone(&orig.observing_cond_symbol_ids),
                    orig.report_gen,
                    orig.milestone_report_gen,
                    orig.group_members.clone(),
                    orig.group_member_report_gens.clone(),
                ));
            }
            out.push(PathShape::new(Some(node), shape.tip_group_id));
        }
        out
    }

    /// 병합 후 main shape 수와 가상 base(멤버 총수)를 누적. group 노드의 members.size 곱이
    /// base 기여. Kotlin `recordShapeStats`.
    fn record_shape_stats(&self, main_map: &PathMap) {
        use std::sync::atomic::Ordering::Relaxed;
        let mut merged = 0i64;
        let mut base = 0i64;
        for shape in main_map.keys() {
            merged += 1;
            let mut contrib = 1i64;
            let mut mp = shape.milestone_path.clone();
            while let Some(node) = mp {
                if let Some(g) = node.group_members.as_ref() {
                    contrib *= g.len() as i64;
                }
                mp = node.parent.clone();
            }
            base += contrib;
        }
        self.stats.merged_shape_sum.fetch_add(merged, Relaxed);
        self.stats.base_shape_sum.fetch_add(base, Relaxed);
        self.stats.gens.fetch_add(1, Relaxed);
    }
}

/// 병합 파티션 후보 — 캐시된 hash 로 버킷팅 후 실등가 검증. chain(root..tip 배열)은
/// lazy — 다중-멤버 버킷에 들어간 후보만 물질화 (대부분 단독 버킷은 할당 안 함).
/// Kotlin `MergeCandidate`.
struct MergeCandidate {
    shape: PathShape,
    cond: AcceptCondition,
    tip: Option<Rc<MilestonePath>>,
    length: i32,
    chain_cache: OnceCell<Vec<Rc<MilestonePath>>>,
}

impl MergeCandidate {
    fn new(shape: PathShape, cond: AcceptCondition, tip: Option<Rc<MilestonePath>>, length: i32) -> Self {
        Self { shape, cond, tip, length, chain_cache: OnceCell::new() }
    }

    /// root..tip 배열 (index 0 = root-most). 필요 시 한 번만 물질화.
    fn chain(&self) -> &Vec<Rc<MilestonePath>> {
        self.chain_cache.get_or_init(|| {
            let mut c = Vec::with_capacity(self.length as usize);
            let mut cur = self.tip.clone();
            while let Some(node) = cur {
                cur = node.parent.clone();
                c.push(node);
            }
            c.reverse();
            c
        })
    }
}

#[derive(Clone, Copy, PartialEq, Eq)]
enum MergeVerdict {
    Merge,
    RejectCond,
    RejectGenObs,
    RejectReportCoord,
    NotCandidate,
}

/// MilestonePath 의 node-local(비재귀) 비교 — gen·milestone·observing·group 만
/// (parent 는 호출자가 위치별로 따로 비교). report_gen 류는 제외 (eq 계약과 동일).
/// Kotlin `nodeLocalEquals`.
fn node_local_eq(a: &MilestonePath, b: &MilestonePath) -> bool {
    if a.gen_idx != b.gen_idx {
        return false;
    }
    if a.observing_cond_symbol_ids.len() != b.observing_cond_symbol_ids.len()
        || a.observing_cond_symbol_ids
            .iter()
            .zip(b.observing_cond_symbol_ids.iter())
            .any(|(x, y)| x != y)
    {
        return false;
    }
    match (&a.group_members, &b.group_members) {
        (None, None) => a.milestone == b.milestone,
        (Some(x), Some(y)) => x == y,
        _ => false,
    }
}

/// Or-merge `cond` into `map[shape]`. `add_path` 는 Never 를 드롭하지만 여기서는
/// merge 결과를 그대로 담아야 하므로 (병합 group 의 cond 는 이미 non-Never) 직접 Or.
/// Kotlin 의 `merged[shape] = if (existing == null) cond else Or.from(existing, cond)`.
fn merge_into(map: &mut PathMap, shape: PathShape, cond: AcceptCondition) {
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

/// 초기 액션 적용: 모든 태그가 root 의 startGen 으로 resolve.
fn initial_application(pa: Arc<ParsingActionsPlain>, root: PathRoot) -> ActionApplication {
    let base = root.start_gen;
    ActionApplication {
        actions: pa,
        root,
        rt_curr: base,
        rt_mid: base,
        next: base,
        rt_grand: base,
        rep_curr: base,
        rep_mid: base,
        rep_grand: base,
        condition: AcceptCondition::Always,
    }
}

/// Per-gen ActionApplication dedup key (see `kernels_history`). Cheap integer
/// identity: the actions template (Arc ptr, since templates are shared) plus the
/// rt/rep gen bindings. Condition variants — the only field this omits — are
/// disambiguated inside the bucket (`SmallCondSet`) by value, so composite
/// condition trees are never hashed.
#[derive(Clone, Copy, PartialEq, Eq, Hash)]
struct AppKey {
    actions: usize,
    rt_curr: i32,
    rt_mid: i32,
    next: i32,
    rt_grand: i32,
    rep_curr: i32,
    rep_mid: i32,
    rep_grand: i32,
}

/// Conditions seen for one `AppKey` bucket this gen. Almost always length 0 or 1
/// (differing conditions under one key are rare), so a linear compare is cheaper
/// than hashing the condition tree. Owns its conditions so the bucket map can be
/// reused (cleared) across gens without lifetime ties.
#[derive(Default)]
struct SmallCondSet {
    items: Vec<AcceptCondition>,
}
impl SmallCondSet {
    #[inline]
    fn contains(&self, c: &AcceptCondition) -> bool {
        self.items.iter().any(|x| x == c)
    }
    #[inline]
    fn push(&mut self, c: AcceptCondition) {
        self.items.push(c);
    }
}

/// record 생성 시점(record_gen)부터 매 step 의 evolve 를 재생한 뒤 입력-끝 평가.
/// 파스 중 live path 의 조건이 겪는 단계별 진화와 동일 — longest/join/except 의
/// 타이밍 의미가 보존된다. Mirrors Kotlin `evaluateRecordCondition` /
/// mgroup2 kernelsHistory 의 `isEventuallyAccepted`.
pub fn evaluate_record_condition(
    cond: &AcceptCondition,
    history: &[HistoryEntry],
    record_gen: i32,
    end_late_fins: &HashMap<PathRoot, AcceptCondition>,
) -> bool {
    let mut c = cond.clone();
    let len = history.len() as i32;
    let mut g = record_gen;
    while g < len {
        if matches!(c, AcceptCondition::Always) {
            return true;
        }
        if matches!(c, AcceptCondition::Never) {
            return false;
        }
        let entry = &history[g as usize];
        c = evolve_accept_condition(
            &c,
            &entry.cond_path_finishes,
            &entry.late_cond_path_finishes,
            &entry.active_cond_paths,
            g,
        );
        g += 1;
    }
    if matches!(c, AcceptCondition::Always) {
        return true;
    }
    if matches!(c, AcceptCondition::Never) {
        return false;
    }
    // 가상 late step: 입력 끝에서 살아있던 root 들의 마지막-gen zero-width finish.
    if !end_late_fins.is_empty() {
        let no_fins: HashMap<PathRoot, AcceptCondition> = HashMap::default();
        let no_active: HashSet<PathRoot> = HashSet::default();
        c = evolve_accept_condition(&c, &no_fins, end_late_fins, &no_active, len);
    }
    evaluate_at_end_of_input(&c)
}

/// replay 후 residual 조건의 입력-끝 평가. residual leaf 는 "마지막 step 까지
/// 해당 finish 가 없었고 root 가 아직 미완"을 뜻하므로, 더 들어올 입력이 없어
/// NoLongerMatch/NotExists/Unless 는 true, 쌍대는 false 로 확정된다.
/// (마지막 step 의 finish 를 다시 보면 안 된다 — 같은 step 의 finish 는
///  "더 긴 매치"가 아니다.)
fn evaluate_at_end_of_input(c: &AcceptCondition) -> bool {
    match c {
        AcceptCondition::Always => true,
        AcceptCondition::Never => false,
        AcceptCondition::And { items } => items.iter().all(evaluate_at_end_of_input),
        AcceptCondition::Or { items } => items.iter().any(evaluate_at_end_of_input),
        AcceptCondition::NoLongerMatch { .. } => true,
        AcceptCondition::NeedLongerMatch { .. } => false,
        AcceptCondition::NotExists { .. } => true,
        AcceptCondition::Exists { .. } => false,
        AcceptCondition::Unless { .. } => true,
        AcceptCondition::OnlyIf { .. } => false,
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
        let parser = Mgroup4Parser::new(empty_data_with_start(7));
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
        let parser = Mgroup4Parser::new(d);
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
        let parser = Mgroup4Parser::new(d);
        let paths = parser.cond_paths_for(&[1], 0);
        assert_eq!(paths.len(), 1); // 999 is unknown — skipped
    }

    #[test]
    fn expected_inputs_empty_when_no_actions() {
        let parser = Mgroup4Parser::new(empty_data_with_start(1));
        let ctx = parser.init_ctx();
        let ts = parser.expected_inputs_of(&ctx);
        assert!(ts.is_empty());
    }

    /// Empty input — `is_last_input=true` for none, so we just take the
    /// initial ctx and confirm it doesn't break.
    #[test]
    fn parse_empty_input() {
        let parser = Mgroup4Parser::new(empty_data_with_start(1));
        let ctx = parser.parse("").expect("empty parse should succeed");
        assert_eq!(ctx.gen_idx, 0);
        assert_eq!(ctx.history.len(), 1);
    }

    /// Two characters against an empty grammar: the first char produces an
    /// empty main path, and the second char's step-start check
    /// (main_paths_before.is_empty) throws.
    #[test]
    fn parse_two_chars_against_empty_grammar_errors() {
        let parser = Mgroup4Parser::new(empty_data_with_start(1));
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
        let parser = Mgroup4Parser::new(empty_data_with_start(1));
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
        let parser = Mgroup4Parser::new(d);
        let ctx = parser.init_ctx();
        let kh = parser.kernels_history(&ctx);
        assert_eq!(kh.len(), 1);
        assert!(kh[0].iter().any(|k| k.symbol_id == 7 && k.pointer == 1));
        assert!(parser.is_accepted(&ctx));
    }
}
