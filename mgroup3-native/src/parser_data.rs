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

        Self {
            start_symbol_id,
            path_roots,
            milestone_groups,
            term_actions,
            tip_edge_actions,
            mid_edge_actions,
            transitive_initial_cond_symbols,
            lookahead_cond_symbols,
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
    }
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
