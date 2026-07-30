//! `Mgroup3Parser` struct and the parts of its public surface that don't need
//! `parseStep` yet: construction, `init_ctx`, `cond_paths_for`,
//! `find_applicable_action`, `expected_inputs_of`. `parseStep` and the recursive
//! helpers land in Step 3.6.

use std::collections::VecDeque;
use rustc_hash::{FxHashMap as HashMap, FxHashSet as HashSet};
use std::rc::Rc;
use std::sync::Arc;

use crate::accept_condition::AcceptCondition;
use crate::history::History;
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

/// 시동 대기 중인 cond root starter — same_input 이면 이번 입력이 watcher 의 첫
/// 글자 (key == ctx.gen == span 시작). 2026-07-30 이후 lookahead 계열도 같은
/// span-정규화 규약을 쓰므로 key 와 span 시작이 항상 일치한다.
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

/// late 채널 finish 를 저장 전에 *직전 gen 기준으로* 한 step 먼저 evolve 해 둔다.
///
/// late fin 의 end 는 직전 gen (`ctx.gen_idx`) 인데 관찰은 이번 gen 에서 일어난다.
/// 그래서 그 조건 안의 bounded leaf (`end_gen == ctx.gen_idx`) 를 소비자가 관찰 gen 에서
/// 평가하면 이미 창 (`gen == end_gen`) 을 지나 있어 default (Unless→Always,
/// OnlyIf→Never) 로 오해소된다. 저장 전에 그 창을 정확히 소비시켜 두면 이후 소비자
/// (`evolve_accept_condition` 의 late 분기 / `RecordConditionEvaluator::bounded_fin`) 는
/// 잔여만 관찰 gen 에서 이어 평가하면 된다.
///
/// `active_cond_paths` 로는 `ctx.paths.keys()` (직전 step 의 살아남은 root + main) 를 쓴다:
///   - 이번 step 에 죽는 root 는 아직 `ctx.paths` 에 있으므로 "직전 gen 에 활성" 이 맞고,
///     그 leaf 는 pending 으로 남아 이번 step 의 late 채널로 해소된다.
///   - 직전 step 에 prune 된 root 는 그 gen 에 아무 조건도 참조하지 않았다는 뜻이고,
///     이번 step 에 late fin 을 낼 수도 없으므로 (`ctx.paths` 에 없어 term 적용 대상이
///     아니다) 비활성 처리해도 답이 같다.
///
/// (bug A) 이 정규화가 없으면 nullable 피제외항의 빈 매치를 감싼 finish 조건
/// (`X = WS - WSNoNL` 의 self-finish = `Unless(WSNoNL, g, g)`) 이 gen g+1 의 late
/// 재보고에서 Always 로 되살아나, `^X` 가 X 의 빈 매치를 오인해 `"ab"` 를 오수락했다.
fn settle_late_fin(
    cond: AcceptCondition,
    prev_entry: Option<&HistoryEntry>,
    active_cond_paths: &HashSet<PathRoot>,
    ctx: &ParsingCtx,
) -> AcceptCondition {
    if matches!(cond, AcceptCondition::Always | AcceptCondition::Never) {
        return cond;
    }
    let Some(prev) = prev_entry else { return cond };
    evolve_accept_condition(
        &cond,
        &prev.cond_path_finishes,
        &prev.late_cond_path_finishes,
        active_cond_paths,
        ctx.gen_idx,
        &ctx.seen_cond_path_fins,
    )
}

/// cond root 의 zero-width self-finish (빈 span `(start_gen, start_gen)` 매치) 를
/// 올바른 채널에 기록한다.
///
/// 두 채널의 end gen 은 규약으로 고정되어 있다 — eager (`cond_path_finishes`) 는
/// end == `next_gen`, late (`late_cond_path_finishes`) 는 end == `next_gen - 1`
/// (== `ctx_gen`). 빈 매치의 end 는 `root.start_gen` 이므로 채널은 `start_gen` 으로
/// 결정된다:
///   - `start_gen == next_gen` → eager (fresh 시동: 빈 매치의 end 가 이번 gen)
///   - `start_gen == ctx_gen` → late (same-input 시동 / 조건 참조로 재물질화:
///     end 는 직전 gen)
///   - `start_gen < ctx_gen` → 기록하지 않음. per-step 채널로 표현할 end 가 없고,
///     그 watcher 는 그 시점에 시동됐어야 하므로 지금 만들면 span 이 어긋난 zombie 다
///     (step 3 도 시동하지 않고 continue 한다).
///
/// (bug A) 이전에는 `start_gen` 과 무관하게 항상 eager 로 기록했다. 그래서 gen g 에
/// 빈 매치로 완성·소멸한 watcher 가 gen g+1 에 조건 참조로 재물질화되면 그 빈 매치가
/// "span (g, g+1) 의 매치" 로 오인되어, 같은 span 을 감시하는 bounded 조건을 부당하게
/// 해소했다: `X = '\n' - ' '*` (피제외항이 nullable) 가 `"a\nb"` 를 오거부하고,
/// `X = Y & Z` (Z nullable) 가 `"a\nb"` 를 오수락했다. `^`/`!` 아래에서는 양방향으로
/// 뒤집혔다.
fn record_zero_width_self_finish(
    root: PathRoot,
    cond: AcceptCondition,
    ctx_gen: i32,
    next_gen: i32,
    eager_out: &mut HashMap<PathRoot, AcceptCondition>,
    late_out: &mut HashMap<PathRoot, AcceptCondition>,
) {
    let out = if root.start_gen == next_gen {
        eager_out
    } else if root.start_gen == ctx_gen {
        late_out
    } else {
        return;
    };
    or_merge(out, root, cond);
}

pub struct Mgroup3Parser {
    plain: ParserDataPlain,
    /// (parent kernel template, tip group id) → tip edge action.
    tip_edge_actions: HashMap<(KernelTemplatePair, i32), Arc<EdgeActionPlain>>,
    /// (parent kernel template, tip kernel template) → mid edge action.
    mid_edge_actions: HashMap<(KernelTemplatePair, KernelTemplatePair), Arc<EdgeActionPlain>>,
}

// 파서 인스턴스는 스레드 간 공유되어 동시에 사용된다 — bibix4 의 병렬 파일 파싱이
// FFI 로 하나의 핸들을 공유한다. 공유 데이터는 전부 Arc/불변, term action 캐시는
// 파스-로컬 (ParsingCtx). (parse 중 만들어지는 ctx 내부의 Rc 들은 파스-로컬이라 무관.)
// 컴파일 타임 보증:
const _: () = {
    const fn assert_send_sync<T: Send + Sync>() {}
    let _ = assert_send_sync::<Mgroup3Parser>;
};

impl Mgroup3Parser {
    pub fn new(data: Mgroup3ParserData) -> Self {
        let plain = ParserDataPlain::from_proto(data);
        Self::from_plain(plain)
    }

    /// Build a parser from an already-materialized `ParserDataPlain`. This is the
    /// shared tail of `new` (which goes through proto) and the rkyv cache path —
    /// both produce identical `ParserDataPlain`, so the parser is identical.
    pub fn from_plain(plain: ParserDataPlain) -> Self {
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
        Self {
            plain,
            tip_edge_actions,
            mid_edge_actions,
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
    /// Mirrors `Mgroup3Parser.kt:88-127`.
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

        let initial_active: HashSet<PathRoot> = initial_cond_roots.iter().copied().collect();

        // gen 0 의 zero-width finish 도 누적 기록에 담는다 — 이후 gen 에서 물질화되는
        // lookahead leaf 가 이 관찰을 봐야 한다 (bug B).
        let mut initial_seen: HashMap<PathRoot, AcceptCondition> = HashMap::default();
        let mut initial_seen_pending: HashSet<PathRoot> = HashSet::default();
        self.update_seen_cond_path_fins(
            &mut initial_seen,
            &mut initial_seen_pending,
            &initial_cond_path_finishes,
            &HashMap::default(),
            &initial_active,
            0,
        );

        let initial_entry = HistoryEntry {
            action_applications: initial_apps,
            finished_kernels: initial_finished,
            added_kernels: Vec::new(),
            cond_path_finishes: initial_cond_path_finishes,
            late_cond_path_finishes: HashMap::default(),
            active_cond_paths: initial_active,
            main_root_finish: initial_main_root_finish,
            reported_cond_roots: initial_cond_roots.into_iter().collect(),
        };

        ParsingCtx {
            gen_idx: 0,
            line: 0,
            col: 0,
            main_root,
            paths: all_paths,
            history: History::from_entry(initial_entry),
            ever_seen_cond_roots: Default::default(),
            root_report_gens: Default::default(),
            seen_cond_path_fins: initial_seen,
            seen_cond_path_fins_pending: initial_seen_pending,
            term_action_cache: Default::default(),
            step_scratch: Default::default(),
        }
    }

    /// `seen_cond_path_fins` 에 담을 수 있는 관찰인가 (Kotlin
    /// `recordableLookaheadRoot`).
    ///  - `lookahead_cond_symbols` 밖의 root 를 참조하는 leaf 는 NotExists/Exists 가
    ///    아니다 (생성기가 lookahead 조건의 symbolId 로 정확히 이 집합을 emit 한다).
    ///  - `eof_cond_symbols` 의 leaf 는 생성 시점에 `resolve_eof_leaves` 가 접어
    ///    없앤다. eof watcher 는 매 gen 완성되므로 담으면 입력 길이만큼 entry 가 쌓인다.
    fn recordable_lookahead_root(&self, root: &PathRoot, fin: &AcceptCondition) -> bool {
        !matches!(fin, AcceptCondition::Never)
            && self.plain.lookahead_cond_symbols.contains(&root.symbol_id)
            && !self.plain.eof_cond_symbols.contains(&root.symbol_id)
    }

    /// 누적 lookahead finish 기록 갱신 (Kotlin `updateSeenCondPathFins`).
    /// 순서가 중요하다:
    ///  1) 이번 step 의 eager/late finish 를 Or 로 접어 넣는다 (raw).
    ///  2) 그 다음 *모든* entry 를 이번 gen 으로 evolve. 저장된 finish 조건은 (nested
    ///     join/except/longest 로) 다른 root 를 참조할 수 있어서 관찰 gen 부터 매 step
    ///     evolve 돼야 하며, 특히 *관찰 gen 자신의* evolve 를 건너뛰면 안 된다 —
    ///     예: watcher 16 의 fin 이 OnlyIf(23, 0, 2) 로 gen 2 에 관찰되면 그 discharge
    ///     (end_gen == 2 의 eager fin 흡수) 는 gen 2 의 evolve 에서만 일어난다. gen 3
    ///     에서 처음 evolve 하면 end_gen+1 분기가 late fin 부재로 Never 를 만들어
    ///     관찰이 사라진다. Always/Never 는 고정점이라 skip.
    ///  결과 Never 는 "그 관찰은 불가능했다" 이므로 entry 를 제거한다.
    ///  evolve 는 갱신 전 `seen` 만 읽고 (updates 에 모아 두었다가 일괄 적용) 순회
    ///  순서와 무관하게 결정적이다 (Kotlin↔Rust byte-identity 게이트).
    ///
    /// `pending` 은 `seen` 중 값이 상수(Always/Never)가 *아닌* key 집합 — evolve 패스가
    /// 실제로 다시 봐야 하는 entry 들. 상수는 evolve 의 고정점이라 영구히 건너뛰어도
    /// 의미가 같고, 이 집합만 순회하면 step 당 O(|seen|) (= 전체 O(n²)) 비용이 사라진다.
    /// 측정치는 `ParsingCtx::seen_cond_path_fins_pending` 문서 참고.
    fn update_seen_cond_path_fins(
        &self,
        seen: &mut HashMap<PathRoot, AcceptCondition>,
        pending: &mut HashSet<PathRoot>,
        cond_path_finishes: &HashMap<PathRoot, AcceptCondition>,
        late_cond_path_finishes: &HashMap<PathRoot, AcceptCondition>,
        active_cond_roots: &HashSet<PathRoot>,
        gen_idx: i32,
    ) {
        for source in [cond_path_finishes, late_cond_path_finishes] {
            for (root, fin) in source.iter() {
                if !self.recordable_lookahead_root(root, fin) {
                    continue;
                }
                // `or_merge` 와 같은 병합 (vacant → fin, occupied → Or(existing, fin)).
                // 병합 결과를 봐야 pending 을 유지할 수 있어 인라인한다.
                let merged = match seen.get(root) {
                    None => fin.clone(),
                    Some(existing) => AcceptCondition::or_from([existing.clone(), fin.clone()]),
                };
                let is_const = matches!(merged, AcceptCondition::Always | AcceptCondition::Never);
                seen.insert(*root, merged);
                if is_const {
                    pending.remove(root);
                } else {
                    pending.insert(*root);
                }
            }
        }
        if pending.is_empty() {
            return;
        }
        let updates: Vec<(PathRoot, AcceptCondition)> = {
            let seen_ro: &HashMap<PathRoot, AcceptCondition> = seen;
            let mut updates: Vec<(PathRoot, AcceptCondition)> = Vec::new();
            for root in pending.iter() {
                let c = &seen_ro[root];
                let evolved = evolve_accept_condition(
                    c,
                    cond_path_finishes,
                    late_cond_path_finishes,
                    active_cond_roots,
                    gen_idx,
                    seen_ro,
                );
                if evolved != *c {
                    updates.push((*root, evolved));
                }
            }
            updates
        };
        for (root, c) in updates {
            match c {
                // 그 관찰은 불가능했다 — 기록에서 제거.
                AcceptCondition::Never => {
                    seen.remove(&root);
                    pending.remove(&root);
                }
                // 확정된 관찰 — 기록은 유지하되 다시 evolve 할 필요는 없다.
                AcceptCondition::Always => {
                    seen.insert(root, c);
                    pending.remove(&root);
                }
                _ => {
                    seen.insert(root, c);
                }
            }
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
    /// Mirrors `Mgroup3Parser.kt:164-173`.
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

    /// Eager EOF resolution: `NotExists/Exists(S@g)` where S 는 "anychar 1글자"
    /// cond symbol (`ParserDataPlain::eof_cond_symbols` — EOF `!.` 의 부정 본문)
    /// 이면 그 진릿값은 입력 길이만의 함수다 — `next_gen - 1` 위치의 글자를
    /// 소비하는 step 에서 이미 확정 가능하다 (anchor 는 next_gen 을 넘지 않으므로
    /// 모든 leaf 가 해소된다). 기존에는 S@g watcher 의 완성이 한 step 뒤에
    /// 조건을 falsify 했는데, 그 1-step 창 동안 문법적으로 죽은 "줄주석이 여기서
    /// EOF 로 끝났다" 경계 shape 이 term action 을 발화해 유령 워처들을 Always
    /// 조건으로 시동시켰다 (mulang docs/parser_phantom_block_comment.md).
    /// 조건 생성 시점에 leaf 를 Never/Always 로 접어 그 창을 없앤다.
    fn resolve_eof_leaves(
        &self,
        cond: AcceptCondition,
        next_gen: i32,
        is_last_input: bool,
    ) -> AcceptCondition {
        if self.plain.eof_cond_symbols.is_empty() {
            return cond;
        }
        // 무변경이면 `None` 을 반환 — 자식이 하나도 접히지 않은 And/Or 는 원본
        // 인스턴스를 그대로 쓰게 해 불필요한 and_from/or_from 재구성(정렬/dedup) 을
        // 회피한다 (changed-flag; borrow 기반이라 clone 없음).
        fn walk(
            parser: &Mgroup3Parser,
            cond: &AcceptCondition,
            next_gen: i32,
            is_last_input: bool,
        ) -> Option<AcceptCondition> {
            // gen g 의 글자 존재: g < next_gen 이면 이미 소비됨(존재), g == next_gen
            // 이면 이번 글자가 마지막인지에 달렸다.
            let char_exists = |g: i32| g < next_gen || !is_last_input;
            match cond {
                // Soundness armor: fold 의 전제는 "조건 anchor(start_gen) 는 next_gen
                // 을 넘지 않는다"이다 — leaf 는 이번 step 에서 소비되는 글자(<=
                // next_gen-1) 를 감시하기 때문. 현 gen 태그 체계에선 start_gen >
                // next_gen 이 도달 불가하지만, 만약 그렇다면 char_exists 가
                // !is_last_input 을 반환해 미래 anchor 를 "존재"로 오판할 수 있으므로,
                // 방어적으로 fold 하지 않고 leaf 를 그대로 둔다 (무변경 → `None`;
                // 그러면 기존 watcher 경로가 처리 — 정확도 손실 없음).
                AcceptCondition::NotExists { symbol_id, start_gen }
                    if parser.plain.eof_cond_symbols.contains(symbol_id) && *start_gen <= next_gen =>
                {
                    Some(if char_exists(*start_gen) {
                        AcceptCondition::Never
                    } else {
                        AcceptCondition::Always
                    })
                }
                AcceptCondition::Exists { symbol_id, start_gen }
                    if parser.plain.eof_cond_symbols.contains(symbol_id) && *start_gen <= next_gen =>
                {
                    Some(if char_exists(*start_gen) {
                        AcceptCondition::Always
                    } else {
                        AcceptCondition::Never
                    })
                }
                AcceptCondition::And { items } => {
                    let mut changed = false;
                    let walked: Vec<AcceptCondition> = items
                        .iter()
                        .map(|c| match walk(parser, c, next_gen, is_last_input) {
                            Some(w) => {
                                changed = true;
                                w
                            }
                            None => c.clone(),
                        })
                        .collect();
                    if changed {
                        Some(AcceptCondition::and_from(walked))
                    } else {
                        None
                    }
                }
                AcceptCondition::Or { items } => {
                    let mut changed = false;
                    let walked: Vec<AcceptCondition> = items
                        .iter()
                        .map(|c| match walk(parser, c, next_gen, is_last_input) {
                            Some(w) => {
                                changed = true;
                                w
                            }
                            None => c.clone(),
                        })
                        .collect();
                    if changed {
                        Some(AcceptCondition::or_from(walked))
                    } else {
                        None
                    }
                }
                _ => None,
            }
        }
        walk(self, &cond, next_gen, is_last_input).unwrap_or(cond)
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

    /// Drive one input character. Mirrors `Mgroup3Parser.kt:388-698`.
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
        if super::birth_log::in_range(next_gen) {
            eprintln!(
                "=== STEP gen {}->{} input={:?} roots={}",
                ctx.gen_idx,
                next_gen,
                input,
                ctx.paths.len()
            );
            let mut roots: Vec<_> = ctx.paths.iter().collect();
            roots.sort_by_key(|(r, _)| (r.symbol_id, r.start_gen));
            for (root, pm) in roots {
                let mut tips: Vec<i32> = pm.keys().map(|s| s.tip_group_id).collect();
                tips.sort_unstable();
                tips.dedup();
                eprintln!(
                    "    root {}{} shapes={} tips={:?}",
                    super::birth_log::fmt_root(root),
                    if *root == ctx.main_root { "(MAIN)" } else { "" },
                    pm.len(),
                    tips
                );
            }
        }

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
                        is_last_input,
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
                                let pf_cond = self.resolve_eof_leaves(
                                    build_condition(
                                        &pf.accept_condition,
                                        prev_gen,
                                        mid_gen_local,
                                        next_gen,
                                        prev_gen,
                                    ),
                                    next_gen,
                                    is_last_input,
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

        // same-input 시동이 죽었을 때 (매치 실패 / 살아남은 path 없음): key 는 span
        // 시작 (== ctx.gen) 이므로 그 span 의 매치는 불가로 확정된다. key 를 소진시켜
        // 이후 재시동 (span 이 어긋난 zombie watcher) 을 막는다.
        // 2026-07-30: 구 규약의 "lookahead key(==next_gen) 는 fresh 로 재시동" fallback
        // 제거 — lookahead key 도 span-정규화되어 한 key 가 한 span 만 뜻하므로 재시동은
        // 곧 남의 span 매치를 그 key 에 기록하는 오염이다.
        macro_rules! starter_died {
            ($root:expr, $ctx:expr) => {
                $ctx.ever_seen_cond_roots.insert($root);
            };
        }

        // ----- step 1b: cond root starters 시동 -----
        //  - same_input: 이번 입력이 watcher 의 첫 글자, key == ctx.gen == span 시작.
        //  - !same_input: fresh — 시동만 하고 소비는 다음 step 부터
        //    (key == next_gen == span 시작, 새 경계 watcher).
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
            let blog = super::birth_log::in_range(next_gen);
            if !pending.same_input {
                // fresh 시동만.
                if blog {
                    eprintln!(
                        "  START1b {} g{} fresh-seeded",
                        super::birth_log::fmt_root(&starter_root),
                        pending.milestone_group_id
                    );
                }
                let mut seeded = PathMap::default();
                seeded.insert(starter_shape, AcceptCondition::Always);
                next_paths.insert(starter_root, seeded);
            } else {
                let ta = self.find_applicable_action(&mut term_cache, &starter_shape, input);
                if let Some(ta) = ta {
                    // key 는 span-정규화되어 있으므로 보고 anchor == key (드리프트 없음).
                    let report_gen = starter_root.start_gen;
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
                        is_last_input,
                        &mut per_starter_next,
                        &mut apps,
                        &mut finishes,
                        &mut added,
                        &mut root_progresses,
                        &mut observing,
                        &mut ignored_starters,
                    );
                    if !per_starter_next.is_empty() {
                        if blog {
                            eprintln!(
                                "  START1b {} g{} same-input LIVE shapes={}",
                                super::birth_log::fmt_root(&starter_root),
                                pending.milestone_group_id,
                                per_starter_next.len()
                            );
                        }
                        let acc = next_paths.entry(starter_root).or_insert_with(PathMap::default);
                        for (s, c) in per_starter_next {
                            add_path(acc, s, c);
                        }
                    } else {
                        if blog {
                            eprintln!(
                                "  START1b {} g{} same-input DIED(no-survivor)",
                                super::birth_log::fmt_root(&starter_root),
                                pending.milestone_group_id
                            );
                        }
                        starter_died!(starter_root, ctx);
                    }
                } else {
                    if blog {
                        eprintln!(
                            "  START1b {} g{} same-input DIED(no-term-action input mismatch)",
                            super::birth_log::fmt_root(&starter_root),
                            pending.milestone_group_id
                        );
                    }
                    starter_died!(starter_root, ctx);
                }
            }
            if let Some(self_finish_tpl) = root_info.self_finish_accept_condition.as_ref() {
                let cond = self.resolve_eof_leaves(
                    build_condition(
                        self_finish_tpl,
                        starter_root.start_gen,
                        starter_root.start_gen,
                        next_gen,
                        starter_root.start_gen,
                    ),
                    next_gen,
                    is_last_input,
                );
                // zero-width 매치이므로 채널은 start_gen 으로 결정된다 (bug A).
                record_zero_width_self_finish(
                    starter_root,
                    cond,
                    ctx.gen_idx,
                    next_gen,
                    &mut root_progresses,
                    &mut late_pf_progresses,
                );
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
        // cond root 의 *내부* cond symbol 들 (initial_cond_symbol_ids 의 transitive
        // closure) 은 그 root 와 같은 span 에서 시작한다 (예: `"fn"&Tk` 의 Tk,
        // `Tk = <Word>` 의 Word). init_ctx 의 cond_paths_for 는 gen 0 root 에 대해 이
        // closure 를 만들어 주지만, 입력 중간에 시동되는 starter 에는 그 경로가 없어서
        // (step 1b/step 3 는 starter 의 term action 의 cond_root_starters 를 무시한다)
        // 내부 watcher 가 `PathRoot(sym, next_gen)` — 즉 부모보다 뒤인 잘못된 span —
        // 으로만 생기고 있었다. 그 결과 부모 watcher 의 finish 조건
        // (OnlyIf(Tk@span, ...)) 이 빈 key 를 보고 Never 로 무너진다.
        // (`new_cond_roots_sorted` 를 스냅샷 버퍼로 재사용 — 아래에서 다시 clear 된다.)
        let mut new_cond_roots_sorted = std::mem::take(&mut scratch.new_cond_roots_sorted);
        new_cond_roots_sorted.clear();
        new_cond_roots_sorted.extend(new_cond_roots.iter().copied());
        for root in new_cond_roots_sorted.iter() {
            let Some(closure) = self.plain.transitive_initial_cond_symbols.get(&root.symbol_id)
            else {
                continue;
            };
            for &sym in closure.iter() {
                new_cond_roots.insert(PathRoot::new(sym, root.start_gen));
            }
        }

        let mut new_cond_root_progresses = std::mem::take(&mut scratch.new_cond_root_progresses);
        new_cond_root_progresses.clear();
        // Iterate over a sorted-by-(sym,next_gen) copy to keep step3 deterministic
        // across HashSet iteration orders. (drain keeps new_cond_roots' allocation
        // for reuse; the sorted vec is likewise pooled.)
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
                let self_cond = self.resolve_eof_leaves(
                    build_condition(
                        tpl,
                        path_root.start_gen,
                        path_root.start_gen,
                        next_gen,
                        path_root.start_gen,
                    ),
                    next_gen,
                    is_last_input,
                );
                // zero-width 매치이므로 채널은 start_gen 으로 결정된다 (bug A).
                // 예전의 덮어쓰기 (`insert`) 대신 Or-merge (helper 안에서 or_merge).
                record_zero_width_self_finish(
                    path_root,
                    self_cond,
                    ctx.gen_idx,
                    next_gen,
                    &mut new_cond_root_progresses,
                    &mut late_pf_progresses,
                );
            }
            let starter_shape = PathShape::new(None, root_info.milestone_group_id);
            // key(= span 시작) 기준 시동 — step 1b 와 동일한 규칙 (2026-07-30: lookahead
            // 도 span-정규화되어 계열 구분이 사라졌다):
            //  - start_gen == next_gen: fresh 시동만 (소비는 다음 step 부터).
            //  - start_gen == ctx.gen: same-input — 이번 입력이 첫 글자. 실패 시 key 소진.
            //  - start_gen < ctx.gen: 그 시점에 시동됐어야 하는 watcher — 지금 만들면
            //    span 이 어긋난 zombie 가 되므로 시동하지 않는다.
            let same_input = if path_root.start_gen == next_gen {
                false
            } else if path_root.start_gen == ctx.gen_idx {
                true
            } else {
                continue;
            };
            let blog = super::birth_log::in_range(next_gen);
            if !same_input {
                if blog {
                    eprintln!(
                        "  START3 {} g{} fresh-seeded",
                        super::birth_log::fmt_root(&path_root),
                        root_info.milestone_group_id
                    );
                }
                let mut seeded = PathMap::default();
                seeded.insert(starter_shape, AcceptCondition::Always);
                next_paths.insert(path_root, seeded);
            } else {
                let ta = self.find_applicable_action(&mut term_cache, &starter_shape, input);
                if let Some(ta) = ta {
                    // key 는 span-정규화되어 있으므로 보고 anchor == key.
                    let report_gen = path_root.start_gen;
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
                        is_last_input,
                        &mut starter_next_paths,
                        &mut apps,
                        &mut finishes,
                        &mut added,
                        &mut new_cond_root_progresses,
                        &mut observing,
                        &mut ignored_starters,
                    );
                    if !starter_next_paths.is_empty() {
                        if blog {
                            eprintln!(
                                "  START3 {} g{} same-input LIVE shapes={}",
                                super::birth_log::fmt_root(&path_root),
                                root_info.milestone_group_id,
                                starter_next_paths.len()
                            );
                        }
                        next_paths.insert(path_root, starter_next_paths);
                    } else {
                        if blog {
                            eprintln!(
                                "  START3 {} g{} same-input DIED(no-survivor)",
                                super::birth_log::fmt_root(&path_root),
                                root_info.milestone_group_id
                            );
                        }
                        starter_died!(path_root, ctx);
                    }
                } else {
                    if blog {
                        eprintln!(
                            "  START3 {} g{} same-input DIED(no-term-action input mismatch)",
                            super::birth_log::fmt_root(&path_root),
                            root_info.milestone_group_id
                        );
                    }
                    starter_died!(path_root, ctx);
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
        if !late_pf_progresses.is_empty() {
            // late fin 의 end 는 직전 gen 이므로 저장 전에 직전 step 채널로 한 step
            // 정규화한다 (settle_late_fin 주석 참조 — bug A). 임시 active 집합은 late
            // fin 이 있을 때만 만든다.
            let prev_entry = ctx.history.last();
            let active_prev: HashSet<PathRoot> = ctx.paths.keys().copied().collect();
            for (root, cond) in &late_pf_progresses {
                if *root != ctx.main_root {
                    let settled =
                        settle_late_fin(cond.clone(), prev_entry, &active_prev, &ctx);
                    if !matches!(settled, AcceptCondition::Never) {
                        late_cond_path_finishes.insert(*root, settled);
                    }
                }
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
                    &ctx.seen_cond_path_fins,
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
        let main_paths_evolved =
            paths_evolved.get(&ctx.main_root).cloned().unwrap_or_default();

        // ----- step 6: prune unreferenced cond paths -----
        // referenced_roots: 런타임 생존 규칙 — 조건 참조 root + observing 의 dot anchor.
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
                        // 모든 watcher 계열의 미래 조건 anchor 는 dot 뿐 — term 조건은
                        // MID(같은 step 에 starter 로 시동), edge 조건은 GRAND(=dot) 로만
                        // anchoring (remapEdgeCondGens; 실측 scanCondAnchorTags: mulang 전
                        // 템플릿에서 bounded CURR anchor 0건). tip(gen)/parent anchor 로만
                        // 살아남는 워처가 인접-gen 중복 root 의 원인
                        // (watcher_anchor_dedup.md §1).
                        // 2026-07-30: lookahead 도 remap 대상이 되어 dot-only 규칙에 합류
                        // (§9) — 구 규약의 tip/parent 예외 anchor 제거.
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

        // 이번 step 의 lookahead watcher finish 들을 누적 기록에 접어 넣는다 (step 5
        // 이후 — 이번 step 의 관찰은 per-step 채널이 이미 처리했고, 이 기록은 *다음*
        // step 부터 유효하다). bug B: 조건은 watcher 사망 이후에도 물질화될 수 있다.
        // (Kotlin 은 history push 직후에 호출하지만 이 함수는 history/
        // ever_seen_cond_roots 를 읽지 않으므로 순서는 출력에 무관 — 여기서는 두
        // finish 맵이 아직 owned local 인 지점에 둔다.)
        self.update_seen_cond_path_fins(
            &mut ctx.seen_cond_path_fins,
            &mut ctx.seen_cond_path_fins_pending,
            &history_entry.cond_path_finishes,
            &history_entry.late_cond_path_finishes,
            &active_cond_roots,
            next_gen,
        );

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

        let ParsingCtx {
            mut history,
            mut ever_seen_cond_roots,
            root_report_gens,
            seen_cond_path_fins,
            seen_cond_path_fins_pending,
            ..
        } = ctx;
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
            seen_cond_path_fins,
            seen_cond_path_fins_pending,
            term_action_cache: term_cache,
            step_scratch: scratch,
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
    /// accept 판정은 main root 의 progress 조건 전용 채널(main_root_finish)만 사용 —
    /// finished_kernels 는 보고 전용. Mirrors Kotlin `isAccepted`.
    pub fn is_accepted(&self, ctx: &ParsingCtx) -> bool {
        let Some(last_entry) = ctx.history.last() else { return false };
        let Some(cond) = &last_entry.main_root_finish else { return false };
        let end_late = self.end_of_input_late_fins(ctx);
        let evaluator = super::record_cond::RecordConditionEvaluator::new(&ctx.history, end_late);
        evaluator.evaluate(cond, ctx.history.len() as i32 - 1)
    }

    /// 입력 끝에서 아직 살아있는 cond path 들의 zero-width possible-finish —
    /// 죽음이 더는 step 으로 관찰되지 않으므로 마지막 gen 에 끝나는 finish 들을
    /// 한 번 쓸어 모아 최종 평가의 가상 late step 으로 쓴다. Kotlin
    /// `endOfInputLateFins` 대응.
    pub fn end_of_input_late_fins(&self, ctx: &ParsingCtx) -> HashMap<PathRoot, AcceptCondition> {
        let mut result: HashMap<PathRoot, AcceptCondition> = HashMap::default();
        // 가상 late step 도 end == ctx.gen_idx 이므로 실제 late 채널과 같은 정규화가
        // 필요하다 (settle_late_fin 주석 참조 — bug A).
        let prev_entry = ctx.history.last();
        let active_prev: HashSet<PathRoot> = ctx.paths.keys().copied().collect();
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
                        // 입력이 끝난 시점 — 글자는 0..ctx.gen_idx 에만 존재하므로
                        // (next_gen=ctx.gen_idx, is_last=true) 로 해소한다.
                        let pf_cond = self.resolve_eof_leaves(
                            build_condition(
                                &pf.accept_condition,
                                prev_gen,
                                ctx.gen_idx,
                                ctx.gen_idx + 1,
                                prev_gen,
                            ),
                            ctx.gen_idx,
                            true,
                        );
                        let combined = settle_late_fin(
                            AcceptCondition::and_from([cond.clone(), pf_cond]),
                            prev_entry,
                            &active_prev,
                            ctx,
                        );
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
        let evaluator = super::record_cond::RecordConditionEvaluator::new(&ctx.history, end_late);
        let mut out = Vec::with_capacity(ctx.history.len());
        // Per-gen ActionApplication dedup buffer, allocated once and cleared per
        // gen inside `kernels_at_gen_into` (see there for why dedup is
        // output-invariant). Reused across gens to avoid a per-gen allocation.
        let mut seen: HashMap<AppKey, SmallCondSet> = HashMap::default();
        for (gen_idx, entry) in ctx.history.iter().enumerate() {
            let mut kernels: HashSet<KtlibKernel> = HashSet::default();
            kernels_at_gen_into(&evaluator, entry, gen_idx as i32, &mut seen, &mut kernels);
            out.push(kernels);
        }
        out
    }

    /// A reusable single-gen kernel query: builds the record-condition evaluator
    /// (and its end-of-input late-fin closure) ONCE and answers `at(gen)` on
    /// demand. `query.at(g)` is byte-identical to `kernels_history(ctx)[g]` for
    /// every gen (both go through `kernels_at_gen_into`), but skips materializing
    /// the whole history — the O(gen) work the incremental delta protocol wants
    /// to avoid re-doing per edit. The evaluator borrows `ctx.history`; the query
    /// must not outlive `ctx`.
    pub fn kernels_query<'a>(&self, ctx: &'a ParsingCtx) -> KernelsQuery<'a> {
        let end_late = self.end_of_input_late_fins(ctx);
        let evaluator = super::record_cond::RecordConditionEvaluator::new(&ctx.history, end_late);
        KernelsQuery { history: &ctx.history, evaluator }
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
        // 보고 전용 root anchor (same-input starter 는 startGen-1).
        root_report_gen: i32,
        // eager EOF resolution 용 (resolve_eof_leaves) — 이번 step 이 마지막 입력인지.
        is_last_input: bool,
        next_paths_out: &mut PathMap,
        apps_out: &mut Vec<ActionApplication>,
        finishes_out: &mut Vec<FinishedKernelRecord>,
        added_out: &mut Vec<AddedKernelRecord>,
        root_progresses_out: &mut HashMap<PathRoot, AcceptCondition>,
        observing_out: &mut HashSet<i32>,
        cond_root_starters_out: &mut HashMap<PathRoot, PendingStarter>,
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
            let new_cond = self.resolve_eof_leaves(
                build_condition(
                    &rea.append.accept_condition,
                    parent_gen,
                    mid_gen,
                    gen_idx,
                    grand_gen,
                ),
                gen_idx,
                is_last_input,
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
            let new_shape = PathShape::new(Some(new_mp), rea.append.milestone_group_id);
            if super::birth_log::in_range(gen_idx) {
                let starters: Vec<String> = rea
                    .append
                    .cond_root_starters
                    .iter()
                    .map(|s| {
                        format!(
                            "sym{}->g{} key={:?} same={}",
                            s.symbol_id,
                            s.milestone_group_id,
                            starter_key_of(s.key_gen, mid_gen, gen_idx),
                            s.same_input
                        )
                    })
                    .collect();
                eprintln!(
                    "  TERM {} old={} repl={}.{} +new={} cond={} starters=[{}]",
                    super::birth_log::fmt_root(&path_root),
                    super::birth_log::fmt_shape(old_shape),
                    rea.replace.symbol_id,
                    rea.replace.pointer,
                    super::birth_log::fmt_shape(&new_shape),
                    combined,
                    starters.join("; ")
                );
            }
            add_path(next_paths_out, new_shape, combined);
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
            let new_cond = self.resolve_eof_leaves(
                build_condition(
                    &rap.accept_condition,
                    parent_gen,
                    mid_gen,
                    gen_idx,
                    grand_gen,
                ),
                gen_idx,
                is_last_input,
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
                            // m2 tip edge = (parent milestone @ m2 gen) -> (tip group @ 갱신된 부착 gen)
                            parent_path.milestone_report_gen,
                            parent_path.report_gen,
                            root_report_gen,
                            is_last_input,
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
        // 보고 좌표 바인딩 — m2 edge genMap {0→edge.first.gen, 1→edge.second.gen, 2→gen}.
        report_curr_gen: i32,
        report_mid_gen: i32,
        root_report_gen: i32,
        // eager EOF resolution 용 (resolve_eof_leaves).
        is_last_input: bool,
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
            let cond = self.resolve_eof_leaves(
                build_condition(
                    &append.accept_condition,
                    grand_parent_gen,
                    parent_gen,
                    gen_idx,
                    grand_grand_parent_gen,
                ),
                gen_idx,
                is_last_input,
            );
            let combined = AcceptCondition::and_from([prev_condition.clone(), cond]);
            if matches!(combined, AcceptCondition::Never) {
                continue;
            }
            // 새 tip group 은 이번 gen 에 재부착 — 보고용 report_gen 갱신 (런타임 gen 불변).
            let new_parent_path = parent_path
                .with_observing_and_report_gen(Arc::clone(&append.observing_cond_symbol_ids), gen_idx);
            let new_shape = PathShape::new(Some(new_parent_path), append.milestone_group_id);
            if super::birth_log::in_range(gen_idx) {
                let starters: Vec<String> = append
                    .cond_root_starters
                    .iter()
                    .map(|s| {
                        format!(
                            "sym{}->g{} key={:?} same={}",
                            s.symbol_id,
                            s.milestone_group_id,
                            starter_key_of(s.key_gen, parent_gen, gen_idx),
                            s.same_input
                        )
                    })
                    .collect();
                eprintln!(
                    "  EDGE {} parent={}.{}@{} +new={} cond={} starters=[{}]",
                    super::birth_log::fmt_root(&path_root),
                    parent_path.milestone.symbol_id,
                    parent_path.milestone.pointer,
                    parent_path.gen_idx,
                    super::birth_log::fmt_shape(&new_shape),
                    combined,
                    starters.join("; ")
                );
            }
            add_path(
                next_paths_out,
                new_shape,
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
            let start_node_cond = self.resolve_eof_leaves(
                build_condition(
                    start_node_tpl,
                    grand_parent_gen,
                    parent_gen,
                    gen_idx,
                    grand_grand_parent_gen,
                ),
                gen_idx,
                is_last_input,
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
                                // m2 mid edge = (grandParent milestone @ m2 gen) -> (parent milestone @ m2 gen)
                                grand_parent.milestone_report_gen,
                                parent_path.milestone_report_gen,
                                root_report_gen,
                                is_last_input,
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

/// Materialize the `KtlibKernel` set for ONE gen into `kernels` (cleared-and-
/// filled `seen` dedup buffer supplied by the caller so the whole-history loop
/// can reuse one allocation). Extracted verbatim from `kernels_history`'s per-gen
/// body so `kernels_history` and `KernelsQuery::at` share EXACTLY one
/// implementation — the byte-identity contract between them is structural, not a
/// hand-kept copy.
///
/// Per-gen ActionApplication dedup: the same (actions template, gen bindings,
/// condition) recurs many times within a gen (up to 11x on jar.bbx); each
/// redundant app rebuilds+re-evaluates+re-emits the same kernels into the same
/// set. Skipping duplicates is output-invariant — the kernel coordinates are a
/// pure function of the app's fields (equal for a duplicate) and every emit is an
/// idempotent set `insert`. Bucket on a cheap integer key (`AppKey`) and
/// disambiguate the rare differing-condition case inside the bucket by value
/// (`SmallCondSet`), so composite condition trees are never hashed.
fn kernels_at_gen_into(
    evaluator: &super::record_cond::RecordConditionEvaluator,
    entry: &HistoryEntry,
    gen_idx: i32,
    seen: &mut HashMap<AppKey, SmallCondSet>,
    kernels: &mut HashSet<KtlibKernel>,
) {
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
            let cond = build_condition(cond_tpl, app.rt_curr, app.rt_mid, app.next, app.rt_grand);
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
            let cond = build_condition(cond_tpl, app.rt_curr, app.rt_mid, app.next, app.rt_grand);
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
}

/// A single-gen kernel query holding a pre-built record-condition evaluator (see
/// `Mgroup3Parser::kernels_query`). `at(g)` equals `kernels_history(ctx)[g]`
/// byte-for-byte for every gen.
pub struct KernelsQuery<'a> {
    history: &'a History,
    evaluator: super::record_cond::RecordConditionEvaluator<'a>,
}

impl KernelsQuery<'_> {
    /// The `KtlibKernel` set at `gen` (== `kernels_history(ctx)[gen]`). An
    /// out-of-range gen yields the empty set (mirrors indexing past the vec end).
    pub fn at(&self, gen_idx: usize) -> HashSet<KtlibKernel> {
        let mut kernels: HashSet<KtlibKernel> = HashSet::default();
        let Some(entry) = self.history.get(gen_idx) else {
            return kernels;
        };
        let mut seen: HashMap<AppKey, SmallCondSet> = HashMap::default();
        kernels_at_gen_into(&self.evaluator, entry, gen_idx as i32, &mut seen, &mut kernels);
        kernels
    }

    /// Number of gens (== `kernels_history(ctx).len()` == history length).
    pub fn num_gens(&self) -> usize {
        self.history.len()
    }
}

/// record 생성 시점(record_gen)부터 매 step 의 evolve 를 재생한 뒤 입력-끝 평가.
/// 파스 중 live path 의 조건이 겪는 단계별 진화와 동일 — longest/join/except 의
/// 타이밍 의미가 보존된다. Mirrors Kotlin `evaluateRecordCondition` /
/// `RecordConditionEvaluator.evaluateReplay` / mgroup2 kernelsHistory 의
/// `isEventuallyAccepted`.
///
/// history 의 finish 는 누적 `seen` 채널로 넘긴다 — record 조건은 dot 이 조건부
/// kernel 을 통과하는 step 에 물질화되므로 watcher 사망 이후에 태어날 수 있고, 그
/// 경우 이전 관찰이 unbounded lookahead leaf 의 해소에 쓰여야 한다 (bug B).
pub fn evaluate_record_condition(
    cond: &AcceptCondition,
    history: &History,
    record_gen: i32,
    end_late_fins: &HashMap<PathRoot, AcceptCondition>,
) -> bool {
    let mut c = cond.clone();
    let len = history.len() as i32;
    // seen 은 *전 history* 의 fin 을 담는다 (parse_step 과 같은 merge→그 gen 에서
    // evolve 절차를 gen 0..len-1 에 대해 replay 루프 전에 미리 돌린다 — 그래야 gen g
    // 의 evolve 가 g 에 기록된 fin 을 이미 볼 수 있다; merge→same-gen evolve 규율은
    // `update_replay_seen` 안에 그대로 있다). 필터는 없다 — record 조건에는 eof leaf
    // 도 나타나고 direct 평가 (RecordConditionEvaluator::any_all_fins_true) 는
    // history 의 모든 fin 을 보므로 여기서도 전부 담아야 일치한다.
    //
    // 왜 record_gen 이전까지가 아니라 전체인가: unbounded lookahead 의 진릿값은
    // (symbol, anchor) 만의 함수이므로 replay 의 step-by-step 진행으로는 "이 root 가
    // *나중* gen 에 완성된다" 를 볼 수 없다. per-step 채널만 보면 root 가 그 step 에
    // 비활성이면 parts.is_empty() -> Always 로 조기 확정되는데, 그게 바로 bug B 다.
    //   실례 (Mgroup2VsMgroup3HistoryTest 의 asdl 문법, `EOF = !.`):
    //   AnyChar watcher (21@15) 는 gen 16 에 완성되지만 active_cond_paths 에는 한 번도
    //   오르지 않는다 (resolve_eof_leaves 가 path 조건의 eof leaf 를 생성 시점에 접기
    //   때문에 아무 조건도 그 root 를 참조하지 않아 step 6 이 즉시 버린다). record
    //   조건은 접히지 않은 채 남으므로 replay 는 gen 15 에서 Always 로 확정해
    //   "position 15 에 글자가 없다" 는 거짓 답을 냈다.
    // bounded(Unless/OnlyIf) / longest(NoLongerMatch) 는 여전히 per-gen 채널로만
    // discharge 되므로 replay 의 교차검증 가치는 유지된다.
    // (Kotlin `RecordConditionEvaluator.evaluateReplay` 와 정확히 같은 순서.)
    let mut seen: HashMap<PathRoot, AcceptCondition> = HashMap::default();
    for g in 0..len {
        let entry = history.get(g as usize).expect("history entry in range");
        update_replay_seen(&mut seen, entry, g);
    }
    for (root, fin) in end_late_fins.iter() {
        if matches!(fin, AcceptCondition::Never) {
            continue;
        }
        or_merge(&mut seen, *root, fin.clone());
    }
    let mut g = record_gen;
    while g < len {
        if matches!(c, AcceptCondition::Always) {
            return true;
        }
        if matches!(c, AcceptCondition::Never) {
            return false;
        }
        let entry = history.get(g as usize).expect("history entry in range");
        c = evolve_accept_condition(
            &c,
            &entry.cond_path_finishes,
            &entry.late_cond_path_finishes,
            &entry.active_cond_paths,
            g,
            &seen,
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
        c = evolve_accept_condition(&c, &no_fins, end_late_fins, &no_active, len, &seen);
    }
    evaluate_at_end_of_input(&c)
}

/// `Mgroup3Parser::update_seen_cond_path_fins` 의 거울 (Kotlin
/// `RecordConditionEvaluator.updateSeen`): merge → 그 gen 에서 전체 evolve.
/// 필터가 없다는 점만 다르다 (parser 는 `&self.plain` 의 lookahead/eof 집합으로
/// 걸러 기록 크기를 줄이지만, replay 검증은 direct 평가와 맞춰 전부 담는다).
fn update_replay_seen(
    seen: &mut HashMap<PathRoot, AcceptCondition>,
    entry: &HistoryEntry,
    gen_idx: i32,
) {
    for source in [&entry.cond_path_finishes, &entry.late_cond_path_finishes] {
        for (root, fin) in source.iter() {
            if matches!(fin, AcceptCondition::Never) {
                continue;
            }
            or_merge(seen, *root, fin.clone());
        }
    }
    if seen.is_empty() {
        return;
    }
    let updates: Vec<(PathRoot, AcceptCondition)> = {
        let seen_ro: &HashMap<PathRoot, AcceptCondition> = seen;
        let mut updates: Vec<(PathRoot, AcceptCondition)> = Vec::new();
        for (root, c) in seen_ro.iter() {
            if matches!(c, AcceptCondition::Always | AcceptCondition::Never) {
                continue;
            }
            let evolved = evolve_accept_condition(
                c,
                &entry.cond_path_finishes,
                &entry.late_cond_path_finishes,
                &entry.active_cond_paths,
                gen_idx,
                seen_ro,
            );
            if evolved != *c {
                updates.push((*root, evolved));
            }
        }
        updates
    };
    for (root, c) in updates {
        if matches!(c, AcceptCondition::Never) {
            seen.remove(&root);
        } else {
            seen.insert(root, c);
        }
    }
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
