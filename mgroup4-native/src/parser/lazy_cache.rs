//! mgroup4 Phase G-b5 — lazy 병합 전이 캐시의 Rust 이식 (설계 phase_g_design.md §2, §4).
//!
//! 이식 원본 (Kotlin, 검증 완료): `mgroup4/parser/kotlin/.../Mgroup4Parser.kt` 의
//! G-b1~b4.5 캐시 기계. 이 모듈은 그 기계를 **동형 이식**한다:
//!  - 인터닝 (`chain_sig_id_of`/`NodeTemplateKey`): immutable 체인 노드에 lazy 캐시,
//!    intern-시점 구조 비교 = 정확 키 (지문 금지). Kotlin `chainSigIdOf`.
//!  - condSig per-gen memo (`cond_sig_id_of`): anchor(=curGen) 회전 시 memo 클리어.
//!  - `state_sig_of`: (chainSigId, tipGroupId, condSigId) → u64 팩 (63비트, sign 0).
//!  - State 버킷 (`StateBucketKey` = (prefix 인스턴스, tipGroupId, condSigId)).
//!  - term 전이 캐시 (`merge_bucket_with_plan_capture`/`apply_transition_entry`): plan
//!    캡처/적용 + verdict 재확인 (realized R 보존의 핵심). Kotlin 동명 함수.
//!  - boundary 캐시 (`BoundaryKey`/`BoundaryEntry`, tip/mid edge 멤버 조회 캐시).
//!  - LRU 가드 (insertion-order FIFO 근사 — Kotlin `evict*IfOverBudget`).
//!  - 병렬 검증 모드 (`MG4_LAZY_VERIFY`) 는 core.rs 가 담당 (여기 캐시는 순수 캐시).
//!
//! ★ Rust 특유 아키텍처 결정 (Kotlin 과의 차이): Kotlin 캐시는 파서 인스턴스 필드
//! (파스 간 공유). Rust `Mgroup4Parser` 는 `Send + Sync` 이고 bibix4 병렬 파싱이 하나의
//! 핸들을 스레드 간 공유하므로 (core.rs 의 컴파일 타임 assert_send_sync), 파서에 가변
//! 캐시를 두면 락 경합/공유 위반이 생긴다. 그래서 이 캐시는 **파스-로컬**로 `ParsingCtx`
//! 에 산다 (term_action_cache/step_scratch 와 동일 패턴, 락 없음). 파스 간 공유(warm)는
//! ctx 에서 캐시를 꺼내 다음 파스로 넘기는 방식으로 실현한다 (R3 권고 "프로토타입 단일
//! 스레드 로컬 캐시부터; 공유는 파스별 로컬 + 이월"). 정확성/parity 는 파스-로컬이든
//! 이월이든 동일 — 캐시는 병합 결정만 상각하고 verdict 재확인이 매번 정확성을 보장한다.

use std::rc::Rc;
use std::sync::Arc;

use rustc_hash::FxHashMap as HashMap;

use crate::accept_condition::AcceptCondition;
use crate::parser_data::EdgeActionPlain;
use crate::parsing_ctx::{Kernel, KernelTemplatePair, MilestonePath, PathShape};

// 병합 전이 캐시 LRU 예산 상한 (설계 §2.5). 0 이하 = 무제한 (기본 — 실코퍼스는 수천
// 엔트리라 상한 불필요). env MG4_CACHE_BUDGET / MG4_BOUNDARY_CACHE_BUDGET 로 오버라이드.
// Kotlin mg4MergeCacheBudget / mg4BoundaryCacheBudget.
pub(crate) fn env_merge_cache_budget() -> i32 {
    std::env::var("MG4_CACHE_BUDGET").ok().and_then(|s| s.parse().ok()).unwrap_or(0)
}
pub(crate) fn env_boundary_cache_budget() -> i32 {
    std::env::var("MG4_BOUNDARY_CACHE_BUDGET").ok().and_then(|s| s.parse().ok()).unwrap_or(0)
}

/// 멤버 템플릿을 packed u64 로 (symbolId<<20 | pointer). pointer 는 문법상 유계라 20비트로
/// 충분. Kotlin `packTemplate`.
#[inline]
pub(crate) fn pack_template(symbol_id: i32, pointer: i32) -> u64 {
    ((symbol_id as u64) << 20) | ((pointer as u64) & 0xF_FFFF)
}

// === 인터닝 (§2.3 정확 키 계약) — Kotlin G-b4.5 chainSigInternTable/condSigInternTable ===

/// node-local 템플릿 정확 키 (gen 제외 — symbolId.pointer / group 멤버 템플릿 / observing).
/// stateSignature 의 노드 성분과 정확히 동일 정보. Eq/Hash 로 intern 테이블 정확 조회.
/// ★ 지문 금지: milestone/멤버 리스트/observing 을 **값으로** 들어 구조 비교. Kotlin `NodeTemplateKey`.
#[derive(PartialEq, Eq, Hash)]
struct NodeTemplateKey {
    parent_sig_id: u32,
    is_group: bool,
    /// singleton: milestone 템플릿 (symbolId,pointer) packed. group: 0.
    singleton: u64,
    /// group: 멤버 (symbolId,pointer) packed 리스트 (fold canonical 순 — 그대로 값 비교). singleton: 빈 리스트.
    members: Vec<u64>,
    observing: Vec<i32>,
}

/// condSig 정확 키 — 조건 구조 시그니처 (anchor 상대 오프셋 포함). 문자열 대신 구조 튜플로
/// 정확 표현 (지문 아님 — 구조 자체를 값으로 든다). Kotlin 은 문자열을 intern 했으나 Rust 는
/// 구조 열거를 직접 값 비교해 동등한 정확 키를 얻는다 (Ord/Hash 파생 — 정렬 정규형은 And/Or
/// items 가 이미 canonical 이라 재정렬 불필요).
#[derive(PartialEq, Eq, Hash)]
enum CondSigLeaf {
    Always,
    Never,
    NoLongerMatch { symbol_id: i32, rel_start: i32, rel_min_end: i32 },
    NeedLongerMatch { symbol_id: i32, rel_start: i32, rel_min_end: i32 },
    Exists { symbol_id: i32, rel_start: i32 },
    NotExists { symbol_id: i32, rel_start: i32 },
    Unless { symbol_id: i32, rel_start: i32, rel_end: i32 },
    OnlyIf { symbol_id: i32, rel_start: i32, rel_end: i32 },
    And(Vec<CondSigLeaf>),
    Or(Vec<CondSigLeaf>),
}

/// 조건을 anchor-상대 구조 키로 변환 (Kotlin condSignature 와 동일 규칙 — anchor 상대 오프셋).
/// And/Or 는 items 를 정렬해 순서 독립 (Kotlin 은 문자열 parts.sort(); 여기선 CondSigLeaf 를
/// 정렬 — Ord 파생 필요). 정확성: 두 다른 조건이 같은 키로 접히면 안 됨 (R4). 구조 전체를
/// 값으로 들어 안전.
fn cond_sig_leaf(cond: &AcceptCondition, anchor: i32) -> CondSigLeaf {
    match cond {
        AcceptCondition::Always => CondSigLeaf::Always,
        AcceptCondition::Never => CondSigLeaf::Never,
        AcceptCondition::NoLongerMatch { symbol_id, start_gen, min_end_gen } => {
            CondSigLeaf::NoLongerMatch {
                symbol_id: *symbol_id,
                rel_start: anchor - start_gen,
                rel_min_end: anchor - min_end_gen,
            }
        }
        AcceptCondition::NeedLongerMatch { symbol_id, start_gen, min_end_gen } => {
            CondSigLeaf::NeedLongerMatch {
                symbol_id: *symbol_id,
                rel_start: anchor - start_gen,
                rel_min_end: anchor - min_end_gen,
            }
        }
        AcceptCondition::Exists { symbol_id, start_gen } => {
            CondSigLeaf::Exists { symbol_id: *symbol_id, rel_start: anchor - start_gen }
        }
        AcceptCondition::NotExists { symbol_id, start_gen } => {
            CondSigLeaf::NotExists { symbol_id: *symbol_id, rel_start: anchor - start_gen }
        }
        AcceptCondition::Unless { symbol_id, start_gen, end_gen } => {
            CondSigLeaf::Unless {
                symbol_id: *symbol_id,
                rel_start: anchor - start_gen,
                rel_end: anchor - end_gen,
            }
        }
        AcceptCondition::OnlyIf { symbol_id, start_gen, end_gen } => {
            CondSigLeaf::OnlyIf {
                symbol_id: *symbol_id,
                rel_start: anchor - start_gen,
                rel_end: anchor - end_gen,
            }
        }
        AcceptCondition::And { items } => {
            let mut parts: Vec<CondSigLeaf> = items.iter().map(|c| cond_sig_leaf(c, anchor)).collect();
            parts.sort();
            CondSigLeaf::And(parts)
        }
        AcceptCondition::Or { items } => {
            let mut parts: Vec<CondSigLeaf> = items.iter().map(|c| cond_sig_leaf(c, anchor)).collect();
            parts.sort();
            CondSigLeaf::Or(parts)
        }
    }
}

// CondSigLeaf 정렬 — And/Or items 순서 독립화용. 필드 순서로 구조 비교 (파생 Ord).
impl PartialOrd for CondSigLeaf {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}
impl Ord for CondSigLeaf {
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        fn tag(c: &CondSigLeaf) -> u8 {
            match c {
                CondSigLeaf::Always => 0,
                CondSigLeaf::Never => 1,
                CondSigLeaf::NoLongerMatch { .. } => 2,
                CondSigLeaf::NeedLongerMatch { .. } => 3,
                CondSigLeaf::Exists { .. } => 4,
                CondSigLeaf::NotExists { .. } => 5,
                CondSigLeaf::Unless { .. } => 6,
                CondSigLeaf::OnlyIf { .. } => 7,
                CondSigLeaf::And(_) => 8,
                CondSigLeaf::Or(_) => 9,
            }
        }
        use std::cmp::Ordering;
        let t = tag(self).cmp(&tag(other));
        if t != Ordering::Equal {
            return t;
        }
        match (self, other) {
            (
                CondSigLeaf::NoLongerMatch { symbol_id: a, rel_start: b, rel_min_end: c },
                CondSigLeaf::NoLongerMatch { symbol_id: x, rel_start: y, rel_min_end: z },
            )
            | (
                CondSigLeaf::NeedLongerMatch { symbol_id: a, rel_start: b, rel_min_end: c },
                CondSigLeaf::NeedLongerMatch { symbol_id: x, rel_start: y, rel_min_end: z },
            ) => (a, b, c).cmp(&(x, y, z)),
            (
                CondSigLeaf::Unless { symbol_id: a, rel_start: b, rel_end: c },
                CondSigLeaf::Unless { symbol_id: x, rel_start: y, rel_end: z },
            )
            | (
                CondSigLeaf::OnlyIf { symbol_id: a, rel_start: b, rel_end: c },
                CondSigLeaf::OnlyIf { symbol_id: x, rel_start: y, rel_end: z },
            ) => (a, b, c).cmp(&(x, y, z)),
            (
                CondSigLeaf::Exists { symbol_id: a, rel_start: b },
                CondSigLeaf::Exists { symbol_id: x, rel_start: y },
            )
            | (
                CondSigLeaf::NotExists { symbol_id: a, rel_start: b },
                CondSigLeaf::NotExists { symbol_id: x, rel_start: y },
            ) => (a, b).cmp(&(x, y)),
            (CondSigLeaf::And(a), CondSigLeaf::And(x))
            | (CondSigLeaf::Or(a), CondSigLeaf::Or(x)) => a.cmp(x),
            _ => Ordering::Equal, // Always/Never 는 tag 로 이미 갈림
        }
    }
}

/// 전이 캐시 엔트리: 한 State(입력 시그니처 멀티셋)의 병합 파티션 결정.
/// groups: 각 원소 = (depth d, 그 group 을 이루는 멤버 stateSig(u64) 정렬 배열).
/// 히트 시 입력 shape 를 stateSig 로 버킷팅해 각 group 을 fold. 파티션에 안 든 sig 는
/// singleton 통과. Kotlin `TransitionEntry`.
pub(crate) struct TransitionEntry {
    pub groups: Vec<(i32, Vec<u64>)>,
}

/// 버킷 State 키 — 버킷 안 shape 들의 gen-무관 interned stateSig(u64) 정렬 배열. 값-동등
/// (정확 키). 같은 State 는 같은 배열 → 같은 키 (~96 gen 재사용). Kotlin `BucketStateKey`.
#[derive(PartialEq, Eq, Hash)]
pub(crate) struct BucketStateKey {
    pub sigs: Vec<u64>,
}

/// 경계 edge 캐시 키 — (정렬 멤버 템플릿 리스트, reduce 타깃, isTip). Kotlin `BoundaryKey`.
/// ★ 함정 (타깃 packing 충돌 금지): tip 타깃(replaceMgroupId as u64)과 mid 타깃(packed 템플릿)은
/// is_tip 이 갈라 절대 안 섞인다. mid packed 는 pack_template(20비트 pointer)로 안전.
#[derive(PartialEq, Eq, Hash)]
pub(crate) struct BoundaryKey {
    pub member_templates: Vec<u64>,
    pub target: u64,
    pub is_tip: bool,
}

/// 경계 edge 캐시 엔트리 — group 멤버 중 edge action 을 가진 (멤버 index, edge action) 만.
/// 히트 시 이 리스트만 순회해 그 멤버 singleton 을 만들고 apply_edge_action. edge action 은
/// gen-free 템플릿 키잉이라 `Arc<EdgeActionPlain>` 참조 비교(ptr_eq)가 정확한 대조.
/// Kotlin `BoundaryEntry`.
#[derive(Clone)]
pub(crate) struct BoundaryEntry {
    pub member_edges: Vec<(usize, Arc<EdgeActionPlain>)>,
}

/// 파스-로컬 lazy 병합 캐시 (Kotlin 의 파서-인스턴스 캐시 필드들을 한 구조로 묶음).
/// core.rs 의 `ParsingCtx.lazy_cache` 에 산다. warm 재파스는 이 구조를 파스 간 넘겨 공유.
///
/// ★ intern 테이블 (chain_sig / cond_sig) 도 여기 있다 — MilestonePath 노드의
/// chain_sig_id_cache 는 노드에 상주하지만 노드는 파스마다 새로 만들어지므로, 파스 간 재현성은
/// 이 intern 테이블이 담당한다 (같은 NodeTemplateKey → 같은 id).
pub struct LazyMergeCache {
    // --- 인터닝 ---
    chain_sig_intern: HashMap<NodeTemplateKey, u32>,
    chain_sig_counter: u32,
    /// condSig intern (구조 키 → id).
    cond_sig_intern: HashMap<CondSigLeaf, u32>,
    cond_sig_counter: u32,
    /// per-gen (cond identity, anchor) memo — anchor 는 한 gen 안에서 상수(=curGen)라 cond 만 키.
    /// anchor 회전 시 클리어. Kotlin condSigGenMemo/condSigMemoAnchor.
    cond_sig_gen_memo: HashMap<AcceptCondition, u32>,
    cond_sig_memo_anchor: i32,

    // --- 전이 캐시 (파스 간 공유) ---
    // TransitionEntry 는 Rc 로 — 히트 시 groups 를 통째 clone 하지 않고 Rc 참조만 bump.
    // (Kotlin 은 borrow 검사가 없어 엔트리를 직접 읽지만, Rust 는 apply 가 &mut cache 를
    //  요구해 엔트리 참조를 동시에 못 든다 → Rc 로 값 clone 회피. 파스-로컬 단일 스레드라 Rc OK.)
    pub(crate) merge_transition_cache: HashMap<BucketStateKey, std::rc::Rc<TransitionEntry>>,
    pub(crate) boundary_transition_cache: HashMap<BoundaryKey, BoundaryEntry>,

    // --- LRU 예산 (0 이하 = 무제한) ---
    merge_cache_budget: i32,
    boundary_cache_budget: i32,
    /// merge 캐시 삽입 순서 큐 (insertion-order FIFO 근사 — Kotlin LinkedHashMap iterator 첫 원소).
    merge_insert_order: std::collections::VecDeque<BucketStateKey>,
    boundary_insert_order: std::collections::VecDeque<BoundaryKey>,

    // --- 카운터 (파스 출력 무영향, 진단) ---
    pub cache_hits: i64,
    pub cache_misses: i64,
    pub boundary_cache_hits: i64,
    pub boundary_cache_misses: i64,
    pub merge_evictions: i64,
    pub boundary_evictions: i64,
    pub lazy_verify_checks: i64,
}

impl Default for LazyMergeCache {
    fn default() -> Self {
        Self::new(env_merge_cache_budget(), env_boundary_cache_budget())
    }
}

impl Clone for LazyMergeCache {
    // ctx 는 Clone 을 요구한다 (ParsingCtx derive(Clone)). 캐시 clone 은 실제 파스에서
    // 안 쓰이지만 (파스는 mem::take 로 이동), 컴파일 만족용으로 빈 캐시를 만든다 —
    // clone 이 캐시를 복제하면 파스-로컬 계약이 애매해지므로, 새 빈 캐시가 정확. 정확성
    // 불변 (빈 캐시는 첫 조우로 재구성).
    fn clone(&self) -> Self {
        Self::new(self.merge_cache_budget, self.boundary_cache_budget)
    }
}

impl std::fmt::Debug for LazyMergeCache {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("LazyMergeCache")
            .field("merge_cache_size", &self.merge_transition_cache.len())
            .field("boundary_cache_size", &self.boundary_transition_cache.len())
            .field("cache_hits", &self.cache_hits)
            .field("cache_misses", &self.cache_misses)
            .finish()
    }
}

impl LazyMergeCache {
    pub fn new(merge_budget: i32, boundary_budget: i32) -> Self {
        Self {
            chain_sig_intern: HashMap::default(),
            chain_sig_counter: 0,
            cond_sig_intern: HashMap::default(),
            cond_sig_counter: 0,
            cond_sig_gen_memo: HashMap::default(),
            cond_sig_memo_anchor: i32::MIN,
            merge_transition_cache: HashMap::default(),
            boundary_transition_cache: HashMap::default(),
            merge_cache_budget: merge_budget,
            boundary_cache_budget: boundary_budget,
            merge_insert_order: std::collections::VecDeque::new(),
            boundary_insert_order: std::collections::VecDeque::new(),
            cache_hits: 0,
            cache_misses: 0,
            boundary_cache_hits: 0,
            boundary_cache_misses: 0,
            merge_evictions: 0,
            boundary_evictions: 0,
            lazy_verify_checks: 0,
        }
    }

    /// 전이 캐시 수동 클리어 (축출 정확성 테스트용 — 클리어 후 재파스도 identical).
    /// intern 테이블도 클리어 (노드 캐시가 이미 리셋된 새 파스와 정합; 다음 조우로 재구성).
    /// Kotlin `clearLazyCaches` (Kotlin 은 intern 을 안 지웠으나, Rust 는 노드 sig 캐시가
    /// 파스마다 리셋되므로 intern 도 함께 지워야 id 재현이 일관 — clear 는 통째 리셋).
    pub fn clear(&mut self) {
        self.merge_transition_cache.clear();
        self.boundary_transition_cache.clear();
        self.merge_insert_order.clear();
        self.boundary_insert_order.clear();
        self.chain_sig_intern.clear();
        self.chain_sig_counter = 0;
        self.cond_sig_intern.clear();
        self.cond_sig_counter = 0;
        self.cond_sig_gen_memo.clear();
        self.cond_sig_memo_anchor = i32::MIN;
    }

    pub fn merge_cache_size(&self) -> usize {
        self.merge_transition_cache.len()
    }
    pub fn boundary_cache_size(&self) -> usize {
        self.boundary_transition_cache.len()
    }

    // === 인터닝 (Kotlin chainSigIdOf / condSigIdOf / stateSigOf) ===

    /// 노드의 gen-무관 chain 시그니처 id (lazy). parent 재귀 후 이 노드 성분과 합쳐 intern.
    /// ★ chain_sig_id_cache 는 노드에 상주 (immutable 체인 공유) — 한 번 부여되면 gen 간 재사용.
    /// tip 노드 조회 1회로 전체 체인 시그니처가 O(1) (parent id 들이 이미 캐시). Kotlin `chainSigIdOf`.
    pub(crate) fn chain_sig_id_of(&mut self, node: Option<&Rc<MilestonePath>>) -> u32 {
        let Some(node) = node else { return 0 }; // null prefix = id 0 (root 경계)
        if let Some(cached) = node.chain_sig_id_cache.get() {
            return *cached;
        }
        let parent_id = self.chain_sig_id_of(node.parent.as_ref());
        let key = if let Some(members) = node.group_members.as_ref() {
            let mut m = Vec::with_capacity(members.len());
            for k in members {
                m.push(pack_template(k.symbol_id, k.pointer));
            }
            NodeTemplateKey {
                parent_sig_id: parent_id,
                is_group: true,
                singleton: 0,
                members: m,
                observing: node.observing_cond_symbol_ids.to_vec(),
            }
        } else {
            NodeTemplateKey {
                parent_sig_id: parent_id,
                is_group: false,
                singleton: pack_template(node.milestone.symbol_id, node.milestone.pointer),
                members: Vec::new(),
                observing: node.observing_cond_symbol_ids.to_vec(),
            }
        };
        let counter = &mut self.chain_sig_counter;
        let id = *self.chain_sig_intern.entry(key).or_insert_with(|| {
            *counter += 1;
            *counter
        });
        // set 은 최초 1회만 성공 (immutable) — 그 뒤 재계산 없음. get_or_init 로 무경합.
        let _ = node.chain_sig_id_cache.set(id);
        id
    }

    /// 조건의 anchor-상대 시그니처 id. per-gen memo (재계산 제거) → 결과 구조 키를 intern.
    /// anchor 전환 시 memo 클리어. Kotlin `condSigIdOf`.
    pub(crate) fn cond_sig_id_of(&mut self, cond: &AcceptCondition, anchor: i32) -> u32 {
        if anchor != self.cond_sig_memo_anchor {
            self.cond_sig_gen_memo.clear();
            self.cond_sig_memo_anchor = anchor;
        }
        if let Some(memo) = self.cond_sig_gen_memo.get(cond) {
            return *memo;
        }
        let leaf = cond_sig_leaf(cond, anchor);
        let counter = &mut self.cond_sig_counter;
        let id = *self.cond_sig_intern.entry(leaf).or_insert_with(|| {
            *counter += 1;
            *counter
        });
        self.cond_sig_gen_memo.insert(cond.clone(), id);
        id
    }

    /// stateSig 팩 (63비트, sign 비트 0 유지): chainSigId (비트 42..62, 21비트) | tipGroupId
    /// (비트 21..41, 21비트) | condSigId (비트 0..20, 21비트). 세 필드 모두 구조와 1:1 인
    /// intern id → 팩 값 동등 = State 동등 (지문 아님). Kotlin `stateSigOf`.
    /// ★ 함정 (비트 겹침·부호): 각 필드 21비트 마스크. 오버플로 시 팩 상위 비트 유실로
    /// 충돌 → 정확성 깨짐. 실측 범위(필드 최대 수만)에선 발생 안 함. 방어 assert 로 잡는다.
    pub(crate) fn state_sig_of(
        &mut self,
        shape: &PathShape,
        cond: &AcceptCondition,
        cur_gen: i32,
    ) -> u64 {
        let chain_id = self.chain_sig_id_of(shape.milestone_path.as_ref());
        let cond_id = self.cond_sig_id_of(cond, cur_gen);
        let tg = shape.tip_group_id as u32;
        // 방어: 필드가 21비트 초과면 팩이 충돌 → 정확성 깨짐. 실측 범위에선 발생 안 함.
        debug_assert!(
            chain_id >> 21 == 0 && tg >> 21 == 0 && cond_id >> 21 == 0,
            "stateSig field overflow: chain={chain_id} tg={tg} cond={cond_id}"
        );
        ((chain_id as u64 & 0x1F_FFFF) << 42)
            | ((tg as u64 & 0x1F_FFFF) << 21)
            | (cond_id as u64 & 0x1F_FFFF)
    }

    // === LRU 예산 가드 (설계 §2.5) — Kotlin evict*IfOverBudget ===
    //
    // 캐시 엔트리 수가 예산 초과 시 가장 오래전 삽입된(=recency 최하) 엔트리부터 축출.
    // insertion-order FIFO 근사 (Kotlin LinkedHashMap 삽입 순서와 동형; access-order LRU 는
    // 히트마다 재삽입 비용이 커 프로토타입은 FIFO). ★ 정확성 계약: 축출은 정확성 무영향 —
    // 축출된 State/전이는 다음 조우 시 재구성될 뿐 (parity 게이트가 작은 예산으로 증명).
    // ★ 함정 (예산 0 = 무제한): 실코퍼스는 수천 엔트리라 상한 불필요; 상한 걸면 재구성 미스가
    //   히트율을 떨어뜨린다.
    fn evict_merge_if_over_budget(&mut self) {
        if self.merge_cache_budget <= 0 {
            return;
        }
        while self.merge_transition_cache.len() > self.merge_cache_budget as usize {
            // 이미 축출됐거나 재삽입돼 순서가 어긋난 키는 스킵 (여전히 존재하는 첫 키를 축출).
            let Some(oldest) = self.merge_insert_order.pop_front() else { break };
            if self.merge_transition_cache.remove(&oldest).is_some() {
                self.merge_evictions += 1;
            }
        }
    }

    fn evict_boundary_if_over_budget(&mut self) {
        if self.boundary_cache_budget <= 0 {
            return;
        }
        while self.boundary_transition_cache.len() > self.boundary_cache_budget as usize {
            let Some(oldest) = self.boundary_insert_order.pop_front() else { break };
            if self.boundary_transition_cache.remove(&oldest).is_some() {
                self.boundary_evictions += 1;
            }
        }
    }

    /// merge 전이 캐시에 엔트리 삽입 (+삽입 순서 기록 + LRU 가드). 키를 두 번 만들지 않도록
    /// 조회는 호출자가 하고 삽입만 여기서.
    pub(crate) fn insert_merge_entry(&mut self, key: BucketStateKey, entry: TransitionEntry) {
        // 삽입 순서 큐용으로 키를 복제 (Vec<u64> — 얕은 복제).
        self.merge_insert_order.push_back(BucketStateKey { sigs: key.sigs.clone() });
        self.merge_transition_cache.insert(key, std::rc::Rc::new(entry));
        self.evict_merge_if_over_budget();
    }

    pub(crate) fn insert_boundary_entry(&mut self, key: BoundaryKey, entry: BoundaryEntry) {
        self.boundary_insert_order.push_back(BoundaryKey {
            member_templates: key.member_templates.clone(),
            target: key.target,
            is_tip: key.is_tip,
        });
        self.boundary_transition_cache.insert(key, entry);
        self.evict_boundary_if_over_budget();
    }
}

/// group 노드의 멤버 템플릿 packed 리스트 (정렬). group 멤버는 fold 시 canonical 정렬됐지만
/// (symbolId, pointer, gen) 순이라 gen 제외 후 재정렬 (같은 (sym,ptr) 다른 gen 은 여기서 동일 키).
/// Kotlin `memberTemplateKey`.
pub(crate) fn member_template_key(members: &[Kernel]) -> Vec<u64> {
    let mut out: Vec<u64> = members.iter().map(|m| pack_template(m.symbol_id, m.pointer)).collect();
    out.sort_unstable();
    out
}

/// 실제 tipEdge 조회 (캐시 미스 / 캐시 비활성 / 병렬 검증 기준). 각 멤버 템플릿을
/// tip_edge_actions 에서 조회 — edge action 이 있는 멤버만 (index, action). Kotlin
/// `computeTipEdgeMemberEdges`.
pub(crate) fn compute_tip_edge_member_edges(
    tip_edge_actions: &HashMap<(KernelTemplatePair, i32), Arc<EdgeActionPlain>>,
    members: &[Kernel],
    replace_mgroup_id: i32,
) -> BoundaryEntry {
    let mut out = Vec::with_capacity(members.len());
    for (i, m) in members.iter().enumerate() {
        if let Some(ea) = tip_edge_actions.get(&(m.kernel_template(), replace_mgroup_id)) {
            out.push((i, Arc::clone(ea)));
        }
    }
    BoundaryEntry { member_edges: out }
}

/// 실제 midEdge 조회. Kotlin `computeMidEdgeMemberEdges`.
pub(crate) fn compute_mid_edge_member_edges(
    mid_edge_actions: &HashMap<(KernelTemplatePair, KernelTemplatePair), Arc<EdgeActionPlain>>,
    members: &[Kernel],
    tip_template: KernelTemplatePair,
) -> BoundaryEntry {
    let mut out = Vec::with_capacity(members.len());
    for (i, m) in members.iter().enumerate() {
        if let Some(ea) = mid_edge_actions.get(&(m.kernel_template(), tip_template)) {
            out.push((i, Arc::clone(ea)));
        }
    }
    BoundaryEntry { member_edges: out }
}

/// 두 BoundaryEntry 가 (멤버 index + edge action 참조) 동일한지 (병렬 검증용). edge action 은
/// gen-free 템플릿 키잉이라 Arc 참조 동등(ptr_eq)이 정확한 대조. Kotlin `verifyBoundaryEntry`.
pub(crate) fn boundary_entries_equal(a: &BoundaryEntry, b: &BoundaryEntry) -> bool {
    if a.member_edges.len() != b.member_edges.len() {
        return false;
    }
    for (x, y) in a.member_edges.iter().zip(b.member_edges.iter()) {
        if x.0 != y.0 || !Arc::ptr_eq(&x.1, &y.1) {
            return false;
        }
    }
    true
}
