//! Port of `mgroup3/parser/kotlin/.../Mgroup3ParserDataPlain.kt`.
//!
//! Wraps the prost-generated `Mgroup3ParserData` into idiomatic Rust types so
//! the hot path doesn't pay protobuf accessor cost. Construction happens once
//! per parser; everything is then immutable.

use rustc_hash::{FxHashMap as HashMap, FxHashSet as HashSet};
use std::sync::Arc;

use crate::proto::com::giyeok::jparser::mgroup3::proto as pb;
use crate::proto::com::giyeok::jparser::proto::TermGroup;

// rkyv 캐시(parser_cache.rs): ParserDataPlain 및 도달 가능한 모든 타입에 rkyv
// derive 를 붙여 from_proto 결과물을 통째로 zero-copy archive 로 굽는다. Arc 필드는
// rkyv 0.8 의 shared-pointer dedup (ArcFlavor) 로 처리 — 같은 Arc 는 아카이브에서
// 한 번만 저장되고 deserialize 시 Pool 로 복원되어 공유가 유지된다.
// 임베드하는 prost 타입들(AcceptConditionTemplate 계열, KernelTemplate 계열,
// TermGroup 계열)의 rkyv derive 는 build.rs 의 type_attribute 로 주입한다.
//
// 스키마 버전 규약: 아래 Plain 구조체나 임베드 prost 타입 집합이 바뀌면
// parser_cache::PLAIN_SCHEMA_VERSION 을 수동 bump 할 것 (캐시 무효화).
use rkyv::{with::Skip, Archive, Deserialize, Serialize};

// Re-exported proto types kept raw in plain wrappers — materialization
// happens in `parser/template.rs` with gen parameters.
pub type AcceptConditionTemplate = pb::AcceptConditionTemplate;
pub type KernelTemplate = pb::KernelTemplate;
pub type ProgressedKernelTemplate = pb::ProgressedKernelTemplate;
pub type FinishedKernelTemplate = pb::FinishedKernelTemplate;
pub type AddedKernelTemplate = pb::AddedKernelTemplate;

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct ParserDataPlain {
    pub start_symbol_id: i32,
    pub path_roots: HashMap<i32, Arc<PathRootInfoPlain>>,
    pub milestone_groups: HashMap<i32, Arc<MilestoneGroupPlain>>,
    /// tipGroupId → ordered list of (termGroup, termAction).
    pub term_actions: HashMap<i32, Vec<Arc<TermGroupActionPlain>>>,
    pub tip_edge_actions: Vec<TipEdgeActionPair>,
    pub mid_edge_actions: Vec<MidEdgeActionPair>,
    /// For each symbol, the transitive set of initial cond symbol IDs
    /// (including itself). Cycles produce a set with just the cycle entry-point.
    ///
    /// **Derived** from `path_roots` (see `compute_transitive_initial_cond_symbols`),
    /// so it is *not* archived — `#[rkyv(with = Skip)]` leaves it empty on cache
    /// restore and the cache loader recomputes it via `recompute_derived`. This
    /// keeps the cache honest (no derived state on disk); the size/time delta is
    /// negligible for real grammars (mulang: ~1.8 KB, recompute ~sub-ms) but the
    /// invariant is worth keeping. When restoring, callers who bypass
    /// `parser_cache` (there are none today) must call `recompute_derived`.
    #[rkyv(with = Skip)]
    pub transitive_initial_cond_symbols: HashMap<i32, HashSet<i32>>,
    /// lookahead 가 감시하는 심볼들 — step 3 시동 flavor 판별 (구 규약: same-input).
    pub lookahead_cond_symbols: HashSet<i32>,
    /// "anychar 1글자" cond symbol (EOF = `!.` 의 부정 본문). 이 심볼 S 의 watcher
    /// S@g 는 "gen g 에 글자가 하나라도 있는가"와 동치이므로, NotExists(S@g) 는
    /// 입력 길이만의 함수다 — parse_step 이 조건 생성 시점에 즉시 Never/Always 로
    /// 확정한다 (eager EOF resolution; 줄주석 내부 유령 경계 shape 차단 —
    /// mulang docs/parser_phantom_block_comment.md). 탐지 기준은
    /// `compute_eof_cond_symbols` 참고 (보수적 — 미탐지는 최적화 미적용일 뿐).
    ///
    /// **Derived** from `path_roots`+`term_actions` — `transitive_initial_cond_symbols`
    /// 와 같은 이유로 not archived; 캐시 복원 시 `recompute_derived` 가 재계산.
    ///
    /// **캐시 호환 (magic bump 불필요).** `Skip` 의 archived 타입은 `()` (rkyv 0.8:
    /// `impl ArchiveWith<F> for Skip { type Archived = (); }`) — 크기 0·정렬 1 의
    /// ZST 이고 이 필드는 struct 끝에 붙으므로, 기존 archived 필드들의 오프셋도 전체
    /// 크기도 바뀌지 않는다. 즉 옛 `.rkyv` (eof 필드 없이 구운 것) 의 바이트열이 새
    /// `ArchivedParserDataPlain` 으로 그대로 `access_unchecked` 된다. 이 필드 추가
    /// 전(부모 커밋) 바이너리로 구운 캐시를 이 코드로 로드해 proto 경로와 파스 결과가
    /// 일치함을 실측 확인했다 (그래서 `parser_cache.rs::PLAIN_SCHEMA_VERSION` 은
    /// 그대로 2). 참고: parser_cache.rs 의 "Skip 필드 추가 시 bump" 는 보수적 기본값 —
    /// 임의 위치/타입 변경까지 포괄하려는 것이고, 끝에 붙는 Skip ZST 는 실측상 안전.
    #[rkyv(with = Skip)]
    pub eof_cond_symbols: HashSet<i32>,
}

impl ParserDataPlain {
    pub fn from_proto(proto: pb::Mgroup3ParserData) -> Self {
        let start_symbol_id = proto.start_symbol_id;

        let path_roots: HashMap<i32, Arc<PathRootInfoPlain>> = proto
            .path_roots
            .into_iter()
            .map(|(k, v)| (k, Arc::new(PathRootInfoPlain::from_proto(v))))
            .collect();

        let milestone_groups: HashMap<i32, Arc<MilestoneGroupPlain>> = proto
            .milestone_groups
            .into_iter()
            .map(|(k, v)| (k, Arc::new(MilestoneGroupPlain::from_proto(v))))
            .collect();

        let term_actions: HashMap<i32, Vec<Arc<TermGroupActionPlain>>> = proto
            .term_actions
            .into_iter()
            .map(|(k, v)| {
                let actions = v
                    .actions
                    .into_iter()
                    .map(|tga| Arc::new(TermGroupActionPlain::from_proto(tga)))
                    .collect();
                (k, actions)
            })
            .collect();

        let tip_edge_actions = proto
            .tip_edge_actions
            .into_iter()
            .map(TipEdgeActionPair::from_proto)
            .collect();

        let mid_edge_actions = proto
            .mid_edge_actions
            .into_iter()
            .map(MidEdgeActionPair::from_proto)
            .collect();

        let transitive_initial_cond_symbols = compute_transitive_initial_cond_symbols(&path_roots);
        let lookahead_cond_symbols: HashSet<i32> =
            proto.lookahead_cond_symbol_ids.iter().copied().collect();
        let eof_cond_symbols = compute_eof_cond_symbols(&path_roots, &term_actions);

        Self {
            start_symbol_id,
            path_roots,
            milestone_groups,
            term_actions,
            tip_edge_actions,
            mid_edge_actions,
            transitive_initial_cond_symbols,
            lookahead_cond_symbols,
            eof_cond_symbols,
        }
    }

    /// Recompute the derived `transitive_initial_cond_symbols` map from
    /// `path_roots`. Called after a cache restore, where the field is skipped in
    /// the archive (`#[rkyv(with = Skip)]`) and comes back empty. Idempotent —
    /// overwrites whatever is there. Cheap (mulang: sub-ms; the closure walk is
    /// bounded by `path_roots` which has tens of entries, not the full grammar).
    pub fn recompute_derived(&mut self) {
        self.transitive_initial_cond_symbols =
            compute_transitive_initial_cond_symbols(&self.path_roots);
        self.eof_cond_symbols = compute_eof_cond_symbols(&self.path_roots, &self.term_actions);
    }
}

/// "anychar 1글자" cond symbol 판별: root S 의 starter group 이
///   - term action 이 정확히 하나이고 그 term group 이 전체 문자를 커버
///     (AllCharsExcluding + 빈 제외 집합)
///   - replace_and_appends 없음 (경로가 깊어지지 않음 — 정확히 1글자)
///   - replace_and_progresses 만 있고 조건이 전부 Always (무조건 root 완성)
///   - self-finish 없음 (빈 매치 불가)
/// 이면 S@g 의 완성은 "gen g 에 글자 존재"와 동치. 기준은 의도적으로 보수적 —
/// 놓친 심볼은 기존 watcher 경로로 처리될 뿐 (정확도 손실 없음).
///
/// **기준이 오탐하지 않는 근거 (생성기 불변식).** replace_and_progresses 의 의미는
/// 생성기에서 고정된다: `mgroup3/gen/kotlin/.../Mgroup3ParserGenerator.kt:410-412` —
/// "replace_and_progresses = graph 의 milestone 중 **자기 자신의 끝까지 진행된**
/// 것들" (barrier 그래프의 완성분만; 계속 진행분은 append 로 나간다). 따라서
/// 위 4조건을 만족하려면 starter group 이 정확히 1글자를 소비하고 그 즉시 root 가
/// 무조건 완성돼야 한다 — 이는 EOF 부정 본문(`!.` 의 `.`, 즉 "글자 하나 존재")의
/// 구조와 정확히 일치한다. 다글자를 소비하는 심볼은 append(경로 연장)를 반드시
/// 남기므로 replace_and_appends 비어있음 조건에서, 조건부/빈 매치 심볼은
/// progresses 의 Always 조건 또는 self-finish 조건에서 걸러진다.
fn compute_eof_cond_symbols(
    path_roots: &HashMap<i32, Arc<PathRootInfoPlain>>,
    term_actions: &HashMap<i32, Vec<Arc<TermGroupActionPlain>>>,
) -> HashSet<i32> {
    use crate::proto::com::giyeok::jparser::mgroup3::proto::accept_condition_template::Condition;
    use crate::proto::com::giyeok::jparser::proto::term_group::TermGroup as TermGroupOneof;

    let mut out: HashSet<i32> = HashSet::default();
    for (sym, info) in path_roots {
        if info.self_finish_accept_condition.is_some() {
            continue;
        }
        let Some(actions) = term_actions.get(&info.milestone_group_id) else { continue };
        if actions.len() != 1 {
            continue;
        }
        let tga = &actions[0];
        let all_chars = match tga.term_group.term_group.as_ref() {
            Some(TermGroupOneof::AllCharsExcluding(ace)) => match ace.excluding.as_ref() {
                None => true,
                Some(cg) => cg.unicode_categories.is_empty() && cg.chars.is_empty(),
            },
            _ => false,
        };
        if !all_chars {
            continue;
        }
        let ta = &tga.term_action;
        if !ta.replace_and_appends.is_empty() || ta.replace_and_progresses.is_empty() {
            continue;
        }
        let all_always = ta
            .replace_and_progresses
            .iter()
            .all(|rap| matches!(rap.accept_condition.condition, Some(Condition::Always(_))));
        if !all_always {
            continue;
        }
        out.insert(*sym);
    }
    out
}

/// DFS with explicit stack-set for cycle detection. Mirrors
/// `Mgroup3ParserDataPlain.kt:48-72`.
///
/// Result for a symbol `s`: union of `{s}` with the closure of every entry in
/// `path_roots[s].initial_cond_symbol_ids`. A cycle hit returns just the
/// cycle-entry symbol; the caller's union catches the rest.
fn compute_transitive_initial_cond_symbols(
    path_roots: &HashMap<i32, Arc<PathRootInfoPlain>>,
) -> HashMap<i32, HashSet<i32>> {
    let mut out: HashMap<i32, HashSet<i32>> =
        HashMap::with_capacity_and_hasher(path_roots.len(), Default::default());
    let mut stack: HashSet<i32> = HashSet::default();

    fn closure_of(
        sym_id: i32,
        path_roots: &HashMap<i32, Arc<PathRootInfoPlain>>,
        out: &mut HashMap<i32, HashSet<i32>>,
        stack: &mut HashSet<i32>,
    ) -> HashSet<i32> {
        if let Some(cached) = out.get(&sym_id) {
            return cached.clone();
        }
        if !stack.insert(sym_id) {
            // Cycle — return self only.
            let mut s = HashSet::default();
            s.insert(sym_id);
            return s;
        }
        let Some(info) = path_roots.get(&sym_id) else {
            stack.remove(&sym_id);
            return HashSet::default();
        };
        let mut result = HashSet::default();
        result.insert(sym_id);
        for child in info.initial_cond_symbol_ids.iter().copied() {
            for sym in closure_of(child, path_roots, out, stack) {
                result.insert(sym);
            }
        }
        stack.remove(&sym_id);
        out.insert(sym_id, result.clone());
        result
    }

    for &sym_id in path_roots.keys() {
        closure_of(sym_id, path_roots, &mut out, &mut stack);
    }
    out
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct PathRootInfoPlain {
    pub symbol_id: i32,
    pub milestone_group_id: i32,
    pub initial_cond_symbol_ids: Arc<[i32]>,
    pub self_finish_accept_condition: Option<AcceptConditionTemplate>,
    pub parsing_actions: Option<Arc<ParsingActionsPlain>>,
}

impl PathRootInfoPlain {
    fn from_proto(proto: pb::PathRootInfo) -> Self {
        Self {
            symbol_id: proto.symbol_id,
            milestone_group_id: proto.milestone_group_id,
            initial_cond_symbol_ids: Arc::from(proto.initial_cond_symbol_ids),
            self_finish_accept_condition: proto.self_finish_accept_condition,
            parsing_actions: proto.parsing_actions.map(|pa| Arc::new(ParsingActionsPlain::from_proto(pa))),
        }
    }
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct MilestoneGroupPlain {
    pub possible_finishes: Vec<PossibleFinishPlain>,
}

impl MilestoneGroupPlain {
    fn from_proto(proto: pb::mgroup3_parser_data::MilestoneGroup) -> Self {
        Self {
            possible_finishes: proto
                .possible_finishes
                .into_iter()
                .map(PossibleFinishPlain::from_proto)
                .collect(),
        }
    }
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct PossibleFinishPlain {
    pub symbol_id: i32,
    pub accept_condition: AcceptConditionTemplate,
}

impl PossibleFinishPlain {
    fn from_proto(proto: pb::mgroup3_parser_data::PossibleFinish) -> Self {
        Self {
            symbol_id: proto.symbol_id,
            accept_condition: proto.accept_condition.expect("PossibleFinish.accept_condition missing"),
        }
    }
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct TermGroupActionPlain {
    pub term_group: TermGroup,
    pub term_action: Arc<TermActionPlain>,
}

impl TermGroupActionPlain {
    fn from_proto(proto: pb::mgroup3_parser_data::TermGroupAction) -> Self {
        Self {
            term_group: proto.term_group.expect("TermGroupAction.term_group missing"),
            term_action: Arc::new(TermActionPlain::from_proto(
                proto.term_action.expect("TermGroupAction.term_action missing"),
            )),
        }
    }
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct TermActionPlain {
    pub replace_and_appends: Vec<ReplaceAndAppendPlain>,
    pub replace_and_progresses: Vec<ReplaceAndProgressPlain>,
    pub parsing_actions: Option<Arc<ParsingActionsPlain>>,
}

impl TermActionPlain {
    fn from_proto(proto: pb::TermAction) -> Self {
        Self {
            replace_and_appends: proto
                .replace_and_appends
                .into_iter()
                .map(|raa| ReplaceAndAppendPlain {
                    replace: raa.replace.expect("ReplaceAndAppend.replace missing"),
                    append: AppendMilestoneGroupPlain::from_proto(
                        raa.append.expect("ReplaceAndAppend.append missing"),
                    ),
                })
                .collect(),
            replace_and_progresses: proto
                .replace_and_progresses
                .into_iter()
                .map(|rap| ReplaceAndProgressPlain {
                    replace_milestone_group_id: rap.replace_milestone_group_id,
                    accept_condition: rap
                        .accept_condition
                        .expect("ReplaceAndProgress.accept_condition missing"),
                })
                .collect(),
            parsing_actions: proto.parsing_actions.map(|pa| Arc::new(ParsingActionsPlain::from_proto(pa))),
        }
    }
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct ReplaceAndAppendPlain {
    pub replace: KernelTemplate,
    pub append: AppendMilestoneGroupPlain,
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct ReplaceAndProgressPlain {
    pub replace_milestone_group_id: i32,
    pub accept_condition: AcceptConditionTemplate,
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct AppendMilestoneGroupPlain {
    pub milestone_group_id: i32,
    pub accept_condition: AcceptConditionTemplate,
    pub observing_cond_symbol_ids: Arc<[i32]>,
    pub cond_root_starters: Vec<CondRootStarterPlain>,
}

impl AppendMilestoneGroupPlain {
    fn from_proto(proto: pb::AppendMilestoneGroup) -> Self {
        Self {
            milestone_group_id: proto.milestone_group_id,
            accept_condition: proto.accept_condition.expect("AppendMilestoneGroup.accept_condition missing"),
            observing_cond_symbol_ids: Arc::from(proto.observing_cond_symbol_ids),
            cond_root_starters: proto
                .cond_root_starters
                .into_iter()
                .map(CondRootStarterPlain::from_proto)
                .collect(),
        }
    }
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct CondRootStarterPlain {
    pub symbol_id: i32,
    pub milestone_group_id: i32,
    /// cond root key 를 resolve 할 태그 (KernelTemplateGen). bounded 계열: MID =
    /// span-정규화 same-input (key=ctx.gen). lookahead 계열: NEXT + same_input
    /// (구 규약 — key=gen, 이번 입력부터 소비). NEXT + !same_input = fresh.
    /// CURR = 과거 경계 (등록 skip).
    pub key_gen: i32,
    /// 이번 step 의 입력을 watcher 의 첫 글자로 소비할지 (same-input 시동).
    pub same_input: bool,
}

impl CondRootStarterPlain {
    fn from_proto(proto: pb::CondRootStarter) -> Self {
        Self {
            symbol_id: proto.symbol_id,
            milestone_group_id: proto.milestone_group_id,
            key_gen: proto.key_gen,
            same_input: proto.same_input,
        }
    }
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct EdgeActionPlain {
    pub append_milestone_groups: Vec<AppendMilestoneGroupPlain>,
    pub start_node_progress: Option<AcceptConditionTemplate>,
    pub parsing_actions: Option<Arc<ParsingActionsPlain>>,
}

impl EdgeActionPlain {
    fn from_proto(proto: pb::EdgeAction) -> Self {
        Self {
            append_milestone_groups: proto
                .append_milestone_groups
                .into_iter()
                .map(AppendMilestoneGroupPlain::from_proto)
                .collect(),
            start_node_progress: proto.start_node_progress,
            parsing_actions: proto.parsing_actions.map(|pa| Arc::new(ParsingActionsPlain::from_proto(pa))),
        }
    }
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct ParsingActionsPlain {
    pub progressed: Vec<ProgressedKernelTemplate>,
    pub finished: Vec<FinishedKernelTemplate>,
    /// kernels_history 보고 전용 — 파싱(조건 평가/accept 판정)에는 사용 안 함.
    pub added: Vec<AddedKernelTemplate>,
}

impl ParsingActionsPlain {
    fn from_proto(proto: pb::ParsingActions) -> Self {
        Self { progressed: proto.progressed, finished: proto.finished, added: proto.added }
    }
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct TipEdgeActionPair {
    pub parent: KernelTemplate,
    pub tip_group_id: i32,
    pub edge_action: Arc<EdgeActionPlain>,
}

impl TipEdgeActionPair {
    fn from_proto(proto: pb::mgroup3_parser_data::TipEdgeActionPair) -> Self {
        Self {
            parent: proto.parent.expect("TipEdgeActionPair.parent missing"),
            tip_group_id: proto.tip_group_id,
            edge_action: Arc::new(EdgeActionPlain::from_proto(
                proto.edge_action.expect("TipEdgeActionPair.edge_action missing"),
            )),
        }
    }
}

#[derive(Debug, Archive, Serialize, Deserialize)]
pub struct MidEdgeActionPair {
    pub parent: KernelTemplate,
    pub tip: KernelTemplate,
    pub edge_action: Arc<EdgeActionPlain>,
}

impl MidEdgeActionPair {
    fn from_proto(proto: pb::mgroup3_parser_data::MidEdgeActionPair) -> Self {
        Self {
            parent: proto.parent.expect("MidEdgeActionPair.parent missing"),
            tip: proto.tip.expect("MidEdgeActionPair.tip missing"),
            edge_action: Arc::new(EdgeActionPlain::from_proto(
                proto.edge_action.expect("MidEdgeActionPair.edge_action missing"),
            )),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::proto::com::giyeok::jparser::mgroup3::proto::*;

    /// Build a minimal Mgroup3ParserData with the given path-root info.
    fn pd_with_path_roots(roots: Vec<(i32, PathRootInfo)>) -> Mgroup3ParserData {
        let mut data = Mgroup3ParserData::default();
        for (k, v) in roots {
            data.path_roots.insert(k, v);
        }
        data
    }

    fn root(symbol_id: i32, initial: Vec<i32>) -> PathRootInfo {
        PathRootInfo {
            symbol_id,
            milestone_group_id: 0,
            initial_cond_symbol_ids: initial,
            self_finish_accept_condition: None,
            parsing_actions: None,
        }
    }

    #[test]
    fn closure_singleton() {
        // A → []  ⇒ closure(A) = {A}
        let pd = pd_with_path_roots(vec![(1, root(1, vec![]))]);
        let plain = ParserDataPlain::from_proto(pd);
        let c = plain.transitive_initial_cond_symbols.get(&1).unwrap();
        assert_eq!(*c, [1].into_iter().collect::<HashSet<_>>());
    }

    #[test]
    fn closure_chain() {
        // A → B → C
        let pd = pd_with_path_roots(vec![
            (1, root(1, vec![2])),
            (2, root(2, vec![3])),
            (3, root(3, vec![])),
        ]);
        let plain = ParserDataPlain::from_proto(pd);
        assert_eq!(
            *plain.transitive_initial_cond_symbols.get(&1).unwrap(),
            [1, 2, 3].into_iter().collect::<HashSet<_>>()
        );
        assert_eq!(
            *plain.transitive_initial_cond_symbols.get(&2).unwrap(),
            [2, 3].into_iter().collect::<HashSet<_>>()
        );
        assert_eq!(
            *plain.transitive_initial_cond_symbols.get(&3).unwrap(),
            [3].into_iter().collect::<HashSet<_>>()
        );
    }

    #[test]
    fn closure_cycle_self_only_for_inner_node() {
        // A → B, B → A (cycle). C → A.
        // closure(A) caches via the first walk — once A is on the stack and B
        // recurses into A, B sees A on the stack and returns {A} only. A's own
        // result becomes {A, B}. closure(C) walks A which is now cached:
        //   closure(C) = {C} ∪ closure(A) = {C, A, B}.
        let pd = pd_with_path_roots(vec![
            (1, root(1, vec![2])),
            (2, root(2, vec![1])),
            (3, root(3, vec![1])),
        ]);
        let plain = ParserDataPlain::from_proto(pd);
        // A's closure must contain A and B (B reached during A's walk).
        let a = plain.transitive_initial_cond_symbols.get(&1).unwrap();
        assert!(a.contains(&1) && a.contains(&2), "A closure = {:?}", a);
        // C's closure includes everything reached transitively.
        let c = plain.transitive_initial_cond_symbols.get(&3).unwrap();
        assert!(c.contains(&3) && c.contains(&1) && c.contains(&2), "C closure = {:?}", c);
    }

    #[test]
    fn closure_unknown_initial_symbol() {
        // A → B but no entry for B in path_roots.
        // closure(B) returns ∅ (no info), so closure(A) = {A}.
        let pd = pd_with_path_roots(vec![(1, root(1, vec![99]))]);
        let plain = ParserDataPlain::from_proto(pd);
        let a = plain.transitive_initial_cond_symbols.get(&1).unwrap();
        assert_eq!(*a, [1].into_iter().collect::<HashSet<_>>());
    }

    #[test]
    fn empty_parser_data_constructs() {
        let plain = ParserDataPlain::from_proto(Mgroup3ParserData::default());
        assert_eq!(plain.start_symbol_id, 0);
        assert!(plain.path_roots.is_empty());
        assert!(plain.milestone_groups.is_empty());
        assert!(plain.term_actions.is_empty());
        assert!(plain.tip_edge_actions.is_empty());
        assert!(plain.mid_edge_actions.is_empty());
        assert!(plain.transitive_initial_cond_symbols.is_empty());
    }
}
