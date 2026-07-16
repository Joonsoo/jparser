# mgroup3 incremental parsing — ParseSession 설계 (구현 전)

작성: 2026-07-10. 상태: **설계 (구현 전).** 상위 세션 리뷰 후 구현 승인 대상.
전제 문서 (필독 순):
- `incremental_parsing.md` — 트랙 계획 정본 (프로브 결과: 수렴 거리 median 0 /
  p90 22 / max 229자, strict−semantic 격차 ~1 gen, Phase I0~I3 스케치).
- `mgroup3-native/src/bin/incremental_probe.rs` — 지문 인코딩·수렴 감지의 실행
  가능한 정의. 이 설계가 재사용할 기반 (read-only, lib 무변경).
- 런타임: `mgroup3-native/src/parser/core.rs` (parse_step·kernels_history·
  is_accepted·eager EOF fold), `parsing_ctx.rs` (ParsingCtx·HistoryEntry·
  StepScratch), `parser/record_cond.rs` (RecordConditionEvaluator — 절대-gen
  소비의 핵심 지점), `ffi.rs`, `parser_cache.rs`.
- 스타일 전례: `mgroup4/docs/phase_g_design.md`.

이 문서는 **설계만** 담는다. 파스 의미론은 불변이 대전제 — **세션은 additive
레이어**다. 기존 게이트 (`parser_diff`, `cache_equivalence`, `accept_condition_diff`,
`eof_cond_symbols`, `ffi_smoke`) 는 전부 유지 가능해야 하고, 모든 설계는 세션 결과가
전체 재파스와 **byte-identical** (kernels history + accept) 임을 상시 게이트로
강제할 수 있어야 한다.

---

## 0. 실측 지반과 설계가 딛는 사실 (조사 요약)

프로브 (`incremental_parsing.md` §1) 가 확정한 사실:

1. **수렴 거리**: semantic median 0 / p90 22 / max 229자. 미수렴 0 / 재발산 0.
   strict−semantic 격차 ≈ 1 gen (보고 shadow 는 라이브 상태 재정렬 한 스텝 뒤).
2. **Phase I 단독 잔여 비용** (재파스 꼬리): 편집 위치 25/50/75% → 평균 14.1k /
   6.3k / 3.9k 자. 초중반 편집의 긴 꼬리가 Phase II(splice) 의 회수 대상.

런타임 구조 조사가 확정한 사실 (설계의 제약·기회):

3. **상태는 불변·구조공유.** `ParsingCtx` 는 매 step `parse_step(ctx) → 새 ctx`
   로 흐르고, `paths` 안의 `PathShape`/`MilestonePath` 는 `Rc` 링크드 리스트,
   조건은 값 타입 `AcceptCondition`. **체크포인트 = ctx 를 `Clone` 하되 Rc/Arc 는
   참조만 증가** (§1.3). 단 `history: Vec<HistoryEntry>` 와 `step_scratch` 는
   실제 깊은 복제 비용이 있다 (§1.3, §6-R1).
4. **gen 은 절대값으로 세 곳에서 소비된다** (splice 의 핵심 난소, §2.2):
   - (a) `AcceptCondition` leaf 의 `start_gen`/`min_end_gen`/`end_gen` — 절대 gen.
   - (b) `MilestonePath.gen_idx` (런타임 gen, Eq/Hash 에 포함) + `report_gen`/
     `milestone_report_gen` (보고 shadow, Eq/Hash 제외).
   - (c) `PathRoot.start_gen` (모든 `paths` 키·조건 참조·`ever_seen_cond_roots`).
   - `RecordConditionEvaluator` (record_cond.rs) 는 `history` 를 **절대 gen 으로
     인덱싱**한 역인덱스 (`eager_fin_gens`/`late_fin_gens`/`first_active`/
     `last_active`) 를 만들고, 조건 leaf 의 절대 gen 으로 조회한다. **조건 leaf 의
     gen 과 history 인덱스가 정합해야** kernels_history 가 정확하다.
5. **eager EOF fold 는 생성 시점 `is_last_input` 의존** (core.rs `resolve_eof_leaves`,
   PR #6). suffix 재사용 시 이 결정의 동치성을 논증해야 한다 (§2.4).
6. **핸들은 Send+Sync, 캐시는 파스-로컬.** `Mgroup3Parser` 는 스레드 공유
   (bibix4 병렬 파싱), term_action_cache·step_scratch 는 `ParsingCtx` 에 산다.
   세션은 **문서 단위** — 핸들 공유, 세션당 자기 ctx (§1.4).

**설계의 큰 그림**: 세션은 (i) prefix 체크포인트로 편집 앞부분 재파스를 건너뛰고
(Phase I), (ii) 편집 뒤 suffix 가 구 파스와 수렴하면 구 상태·history 를 gen-rebase
해 splice 한다 (Phase II). 두 레이어 모두 **출력 불변** — 차등 오라클이 정체성.

---

## 1. §1 ParseSession API

### 1.1 수명주기와 시그니처 (Rust 우선 — LSP 는 native 를 FFI 로 씀)

```
ParseSession::new(parser: Arc<Mgroup3Parser>) -> ParseSession
  .parse_full(text: &str) -> &ParseOutcome          // 최초 전체 파스
  .edit(pos_char: usize, old_len_char: usize, new_text: &str) -> &ParseOutcome
  .outcome() -> &ParseOutcome                        // 마지막 결과 재조회
```

- `ParseOutcome` 은 현행 `build_result` 와 **동일한 것**을 제공: accept 여부 +
  kernels history (또는 파스 에러). 소비자는 세션을 쓰든 안 쓰든 같은 것을 본다.
- `edit` 은 문서에 편집을 적용하고, 내부적으로 (a) 편집 위치 이전 체크포인트에서
  ctx 를 복원, (b) 편집 구간부터 재파스, (c) (Phase II) 수렴 지점에서 구 파스
  splice, (d) 새 결과를 재계산. **매 edit 후 결과가 전체 재파스와 동일**함이 계약.
- **편집 모델**: pos/old_len/new_text 는 프로브 `Edit` 와 동형 (char 단위 —
  `p_char`, `lo`, `ln`). 세션은 문서 텍스트 (`Vec<char>`) 를 자기가 보유한다 —
  LSP 는 full-text 든 range 든 세션에 char-오프셋 편집으로 정규화해 전달 (§4).

### 1.2 체크포인트 정책

- **매 K gen 마다 ctx 스냅샷 저장** + 그 gen 의 history 길이 마커.
- **K 선택**: 편집 지연 ≈ `K/2`(가장 가까운 앞 체크포인트까지 되감기 평균) +
  `편집구간 재파스` + `~수렴거리(p90 22)` + `splice`. K 가 작을수록 되감기 짧지만
  체크포인트 메모리·복제 비용 증가. **권고 K=64** (되감기 기대 32 gen ≈ 22 수렴
  거리와 같은 규모 — 비대칭 없음). 파일 크기 무관 상수로 시작, §5 측정 후 조정.
- **메모리 정책 — 링 예산**: 체크포인트는 파일 길이 N 에 대해 `N/K` 개.
  각 체크포인트의 실제 비용은 §1.3 — Rc 공유부는 참조만, **history·scratch 가 실
  비용**이라 링 예산은 gen 수가 아니라 **누적 history 크기**로 잡는다. 권고:
  체크포인트 총 예산 = `max(파일_history_바이트 × C, 하한)`, 초과 시 가장 오래된
  체크포인트부터 축출 (축출돼도 더 앞 체크포인트에서 재개 가능 — 정확성 불변,
  지연만 증가). C 는 §5 측정 후 확정.
- **history append-only 분리 여부** (조사 후 판단): 현행 `ctx.history` 는
  `Vec<HistoryEntry>` 이고 매 step `push` 만 한다 (core.rs:1038, truncate 없음).
  즉 **이미 append-only 로그**다. 체크포인트가 `history.len()` 마커만 들면,
  되감기는 "history 를 마커 길이로 truncate + 그 gen 의 ctx 스냅샷 복원"으로
  충분하다. **구조 변경 불요** — 단, 체크포인트마다 `history.clone()` 을 피하려면
  history 를 ctx 밖의 **공유 append-only 세그먼트 로그**로 빼는 게 이득 (§1.3,
  §6-R1). 이건 침습적 변경이라 Phase I0 는 clone 으로 시작하고 (게이트 우선),
  I2 에서 세그먼트 로그로 전환한다 (splice 가 세그먼트 rope 를 요구하므로 자연스러운
  합류점, §2.3).

### 1.3 체크포인트의 실제 비용 (Clone 해부)

`ParsingCtx` 는 `#[derive(Clone)]`. 필드별 복제 비용:

| 필드 | 타입 | Clone 비용 |
|---|---|---|
| `paths` | `HashMap<PathRoot, PathMap>` | 맵 골격 복제 (엔트리 수 × 소량). 내부 `PathShape`/`MilestonePath` 는 `Rc` — **참조만 증가**, 체인 미복제. 조건은 값 복제 (대개 얕음). |
| `history` | `History` (구조 공유) | **구현 반영**: 원래 `Vec<HistoryEntry>` 라 clone 이 gen 수에 비례한 깊은 복제 = 체크포인트/보관 비용의 지배항이었다. 지금은 `History` (구조 공유 chunked 컨테이너, `history.rs`) — seal 후 clone 은 청크 `Rc` bump 만 (O(#chunks)), 체크포인트는 아예 `clone_without_history` 로 안 든다. |
| `step_scratch` | `StepScratch` | 순수 scratch — 체크포인트는 **비운 것**을 들면 됨 (`Default`). 복제 불요. |
| `ever_seen_cond_roots`, `root_report_gens`, `term_action_cache` | 맵 | 골격 복제 (소~중). term_action_cache 는 재구성 가능 — 체크포인트는 비워도 됨. |

**결론**: Rc 공유부 (paths 의 체인) 는 프로브 전제대로 참조만 유지 — 싸다. **history
가 유일한 무거운 항** 이었다. 그래서 §1.2 의 "history 세그먼트 로그 분리" 가
체크포인트를 진짜 싸게 만드는 열쇠다. **체크포인트는 history 를 안 들고 marker
(usize=`at_gen`) 만 들고** (`snapshot` = `clone_without_history`) 재개 시 세션이
보관한 canonical history 를 marker 로 잘라 쓴다.

**구현 반영 (구조 공유 History)**: 초안은 재개 시 `history[..marker].to_vec()` 였는데,
이 잘라내기가 편집당 O(marker) 딥카피 (resume_gen ≈ n 인 END 편집에서 편집당 O(n))
였고, canonical history 보관도 `Vec` clone 이라 편집당·full parse 체크포인트당 O(n)
= full parse O(n²/K) 였다. 지금은 history 가 구조 공유 `History` (chunked, `Rc<Vec<..>>`
청크 + tail, `history.rs`):
- **canonical 보관**: 파스 끝에서 `seal()` 후 `clone()` = 청크 `Rc` bump 만 (O(#chunks)).
- **재개 (restore_ctx)**: `canonical.prefix(at+1)` — marker 이하 청크는 통째 `Rc`
  공유, 경계 청크만 앞부분 (≤ CHUNK) 딥카피. O(#chunks + CHUNK), marker 무관.
- **splice source (prev_final_ctx)**: `clone_without_history` — history 미보관 (소비처
  `GenRebase::ctx` 가 history 를 안 읽음, §2.2-a).

이로써 full parse 는 O(n²/K) → O(n) (실측 32k자 3985ms → 190ms), END 편집 per-edit 은
n 정비례 → 평탄 (32k 36.8ms → 0.87ms) 이 됐다.

### 1.4 스레딩

- 세션은 **단일 문서 단위**. 한 문서 = 한 세션. `ParseSession` 자체는 `!Sync`
  (내부 가변 상태 — 체크포인트 링, 문서 텍스트) 로 두고, **문서당 하나의 세션**을
  하나의 스레드가 소유한다 (LSP 는 문서별 직렬 처리).
- `Mgroup3Parser` 핸들은 `Arc` 로 **세션 간 공유** (Send+Sync 계약 유지 — 조사
  확인, core.rs:55). 세션은 파스-로컬 캐시 (term_action_cache, scratch) 를 ctx 에
  들고 다니므로 핸들 공유가 경합을 안 만든다.
- FFI 표면 (§4) 은 세션 핸들을 불투명 포인터로 넘긴다 — `ffi.rs` 주석의 "동일
  핸들 동시 호출 금지" 계약을 **세션 핸들**로 승격 (문서별 세션이라 자연 충족).

---

## 2. §2 Phase I2 splice 메커니즘 (핵심 난소)

### 2.1 수렴 감지 (프로브의 실행 정의를 프로덕션으로)

- **구 파스의 per-gen strict 지문을 `BaselineWalker` 로 lazy 산출.** 프로브
  `fp_state(..., Variant::Strict)` 가 gen 당 128비트 (16B). 개념적으로는 구 파스의
  gen별 strict 지문 시퀀스지만, **구현은 매 edit 마다 이 시퀀스를 upfront 로 전부
  계산하지 않는다** — 그건 편집당 구 문서 전체를 gen 0 부터 재파스하는 비용
  (≥ 전문 파스 1회) 이라 세션 이득을 지운다. 대신 소비 지점이 전부 **국소적·단조
  증가**임을 이용한다: 수렴 탐지는 `g >= edit_end` 부터 `og = g - delta` 를 오름차순으로만
  조회하고, 재발산 안정성 체크·splice guard 도 같은 방향이다. 실측 수렴 거리가
  median 0 / p90 22 gen 이므로 실제로 필요한 구-gen 지문은 편집 근방 소수뿐.
  → `BaselineWalker` (`session.rs`): 구 파스의 체크포인트 링에서 조회 시작 지점
  이하의 체크포인트 하나를 (링 truncate **전에** clone) 골라 그 라이브 상태를
  복원하고, `old_doc` 을 **요청받는 gen 까지만** 전진 파스하며 각 gen 의 strict
  지문을 내놓는다 (`fp_at(og)`, 단조 전진). walker 는 **history 가 필요 없다** —
  `fp_state`/`paths_match_after_rebase` 는 `ctx.paths` 만 읽고, `paths` 진화는
  `parse_step` 안에서 `ctx.history` 와 독립 (유일한 history 읽기 `prev_reported`
  는 폐기되는 report 채널 dedup 에만 쓰임) — 이므로 history-less 체크포인트에서
  복원해 전진해도 지문/guard 값이 upfront 트레이스와 byte-identical.
- **anchor 인코딩**: 프로브 `enc_gen(g, p, cur_gen)` 를 그대로 재사용 — 편집 위치
  p 이전은 절대, 이후는 `cur_gen - g` 오프셋. 이 인코딩이 **shift-동치를 지문 일치로**
  만든다 (프로브 헤더의 QED). 편집 재파스 중 매 gen 의 지문을 구 파스의
  `gen - delta` 지문과 대조 (구 지문은 위 walker 가 그 gen 까지 전진해 산출).
- **일치 시 구조적 완전 일치 1회 검증** (해시 충돌 차단): 지문이 처음 일치한
  gen q\* 에서, 신·구 두 라이브 상태 (`ctx.paths`) 를 **shift-정규화해 구조적으로
  완전 비교** (O(live state) 1회). `PathRoot`/`PathShape`/`MilestonePath`/조건을
  gen-정규화한 정준형으로 equals. 통과하면 splice, 실패하면 (충돌 — 극히 희박)
  splice 포기하고 계속 파스 (§2.5 폴백). **구 라이브 상태는 walker 가 방금 q\*-delta
  까지 전진했으므로 그 `current_ctx()` 를 그대로 쓴다** — 별도 재파스 불필요
  (예전엔 `old_live_state_at` 이 구 문서를 gen 0 부터 q\*-delta 까지 재파스했다).
- **왜 strict 로 감지하나**: history splice (§2.3) 는 보고 shadow 까지 일치해야
  재사용 가능. strict 지문은 보고 shadow (`report_gen`/`milestone_report_gen`) 를
  포함하므로, strict 수렴 = "라이브 상태 + 보고 좌표 모두 shift-동치" = history
  세그먼트 재사용 안전 조건. semantic 수렴은 라이브 상태만 — splice 는 strict 를
  쓴다 (프로브가 strict 를 별도로 잰 이유).

### 2.2 gen-rebase — 절대 gen 소비 지점의 처리

splice 는 구 suffix 를 `delta = new_len - old_len` 만큼 이동해 재사용한다. 절대 gen
소비 지점 (§0 사실4) 을 어떻게 rebase 하느냐가 핵심.

**(a) 최종 ctx (accept·kernels_history 평가용) 의 rebase — O(state) 1회 재구성.**
수렴 gen q\* 이후의 구 suffix 라이브 상태를 재사용하려면, 그 상태 안의 gen
(`PathRoot.start_gen`, `MilestonePath.gen_idx`, 조건 leaf 의 gen) 을 rebase 해야
한다. **⚠ 리뷰 정정 (필수)**: 매핑은 균일 `+delta` 가 아니라 **분할 매핑** —
`g <= p (편집 위치) → g (불변)`, `g > p → g + delta`. suffix 상태에는 prefix 에
앵커된 gen (편집 전 구간에서 태어난 장수 워처의 start_gen, 체인 상단 노드의
gen 등) 이 섞여 있고 이들은 새 문서에서도 같은 절대 위치다 — 수렴 지문이
정확히 이 분할 인코딩으로 일치를 판정했으므로, rebase 도 같은 매핑이어야
상태가 실제 새-파스 상태와 동일해진다. (균일 +delta 는 prefix 앵커를 잘못
밀어 조건 평가를 어긋나게 한다 — 차등 오라클이 잡겠지만 스펙부터 정확히.) 이건 **최종 ctx 하나에 대해서만** 필요하고 (accept 는 마지막 entry 의
`main_root_finish` 만 봄, kernels_history 는 history 전체를 봄 — 아래 (b)), 상태
크기가 작으므로 (프로브: live state = roots × pathmap, 대개 수백) **1회 완전 재구성**이
싸다. `Rc` 체인은 gen 이 노드에 박혀 있어 공유 재사용 불가 — rebase 는 새 체인을
만든다. 하지만 이건 splice 지점 1회뿐 (매 gen 아님).

**(b) history 는 재작성하지 않는다 — 세그먼트 + 오프셋 rope.**
history 전체를 `+delta` 재작성하면 O(전체 history) 라 splice 이득이 사라진다.
대신 history 를 **세그먼트들의 rope** 로 본다:
- 세그먼트 A: 편집 앞부분 (구 파스와 공유, 오프셋 0).
- 세그먼트 B: 편집 구간~수렴까지 새로 파스한 부분 (오프셋 0, 새 절대 gen).
- 세그먼트 C: 수렴 이후 구 suffix (**구 history 를 그대로 참조 + 오프셋 delta**).

`kernels_history` / `RecordConditionEvaluator` 가 history 를 소비할 때, **절대 gen
을 읽는 모든 지점에서 세그먼트 오프셋을 적용**한다. 소비 경로가 이를 수용할 수
있는지가 관건 — 아래 열거.

**절대 gen 을 읽는 지점 (kernelsHistory 소비 경로 전수 조사):**

1. `RecordConditionEvaluator::new` (record_cond.rs:78-90): `history.iter().
   enumerate()` 로 gen `g` 를 매기고 `eager_fin_gens`/`late_fin_gens`/`first_active`
   /`last_active` 역인덱스를 절대 gen 으로 채움. → **rope 위에서 인덱싱하면
   자동으로 rebase 된 gen 이 나온다** (세그먼트 C 를 순회할 때 g = 세그먼트오프셋 +
   로컬인덱스). 단 세그먼트 C 의 `HistoryEntry` 안의 `cond_path_finishes` 키
   (`PathRoot`) 는 **구 절대 gen 을 담은 start_gen** 을 갖는다 — 이 키를 조회하는
   조건 leaf 도 같은 구 gen 을 담아야 일치. 즉 **세그먼트 C 안에서는 gen 이 자기끼리
   정합** (구 gen 공간). 문제는 세그먼트 경계를 넘는 참조.
2. 조건 leaf 의 절대 gen (`start_gen` 등) 으로 `first_active.get(root)` 조회
   (record_cond.rs:158, 184, ...): root 의 `start_gen` 이 구 gen 공간이면 세그먼트 C
   의 구-gen 역인덱스와 일치. **세그먼트 C 의 조건은 구 gen 그대로 두고, 역인덱스도
   구 gen 으로 키잉하면 세그먼트 C 내부는 무변경 재사용.**
3. `kernels_history` 출력의 `begin_gen`/`end_gen` (core.rs:1179-1243): `gen_idx`
   (history enumerate 인덱스) 와 `resolve_gen_i32(...)` (app 의 rep 바인딩). 출력
   좌표는 **문서 절대 위치** — 최종 사용자가 보는 gen 은 새 문서 기준이어야 한다.
   → 세그먼트 C 의 begin/end 는 `+delta` 되어 나와야 함.

**핵심 판정 — 두 가지 rope 구현 후보:**
- **(rope-후보-1) 논리 오프셋 rope**: history 를 `Vec<Segment>` 로 두고
  (`Segment { entries: Arc<[HistoryEntry]>, gen_offset: i32, local_range }`), 소비
  경로가 gen 을 읽을 때마다 오프셋을 적용. RecordConditionEvaluator 와
  kernels_history 를 **오프셋-인지** 로 고쳐야 함 (침습적 — §6-R2). 세그먼트 C 를
  물리 복제·재작성하지 않음.
- **(rope-후보-2) lazy 물질화 rope**: 소비 시점 (kernels_history 호출) 에만
  세그먼트 C 를 `+delta` 로 **한 번 물질화**해 평평한 history 를 만든 뒤 현행
  소비 경로를 무변경 사용. 물질화는 O(전체 history) 지만 **파스 hot path 가 아니라
  결과 요청 시 1회** — LSP 는 편집당 결과를 1회 요청하므로 편집당 O(history) 1회.
  이건 "splice 로 파스 재계산을 아꼈지만 출력 조립은 여전히 O(N)" 을 뜻한다.

**설계 판정 (상위 세션 확인 결정 #2)**: LSP 소비자는 편집당 kernels_history 를
**1회** 요청하고, 그 자체가 이미 O(history) (evaluator 인덱스 구축 + gen별 조립)
이다. 따라서 **rope-후보-2 (lazy 물질화)** 가 옳은 시작점 — splice 의 이득은
**파스 재계산 절감** (편집 구간+수렴거리로 축소) 에서 나오고, 출력 조립의 O(N) 은
splice 유무와 무관하게 이미 존재한다. rope-후보-1 (오프셋-인지 소비) 은 출력
조립까지 증분화하려는 별도 최적화 — Phase I2 범위 밖, 백로그. **I2 는 lazy 물질화로
파스 절감만 실현**하고, 정확성은 물질화된 history 가 전체 재파스 history 와
byte-identical 임을 오라클로 강제한다.

### 2.3 history 세그먼트 로그 구조

- I0/I1 에서 history 는 세션이 소유한 단일 `Vec<HistoryEntry>` (append-only) +
  체크포인트의 `len` 마커. 되감기 = truncate.
- I2 에서 splice 를 지원하려면 되감기 후 **구 suffix 세그먼트를 붙일 수 있어야**
  한다. 최소 구조: 세션이 (i) 현재 활성 history (편집 앞 + 새 파스), (ii) 직전
  파스의 전체 history 를 `Arc<[HistoryEntry]>` 로 보관 (구 suffix 소스). splice 시
  "활성 history[..q_edit] + 구 history[q\*_old..] (분할 매핑 적용 — §2.2 리뷰 정정)" 를 lazy 물질화 (§2.2
  rope-후보-2).
- **구 history 보관 비용**: 직전 파스 history 1벌을 세션의 `canonical_history` 로
  유지 (편집당 갱신). 메모리 오버헤드 = history 1벌 (파일 크기 규모) — 수용 가능.
  체크포인트 예산과 별도 회계. **구현 반영**: 이 보관·갱신은 구조 공유 `History`
  (§1.3) 라 seal 후 `Rc` bump (O(#chunks)) — 편집당 O(n) 딥카피가 아니다. splice
  물질화 (세그먼트 C 의 `rebase.entry` 루프) 자체는 **BY DESIGN O(suffix)** 로 유지
  (rope-후보-2). splice 후 canonical 재보관도 `seal()` + `Rc` bump.

### 2.4 eager EOF fold 와의 상호작용

- **주장**: 편집이 EOF 를 옮기지 않는 위치 (문서 끝이 아닌 곳) 의 편집이고, 수렴
  지점 q\* 이후 **suffix 내용이 문자열로 동일**하면 (splice 의 전제 — 구 suffix 를
  그대로 재사용), 각 gen 의 `is_last_input` 값도 동일하다 (마지막 gen 만 true,
  나머지 false). `resolve_eof_leaves` (core.rs:293) 는 leaf 를 `is_last_input` +
  `next_gen` 대비 leaf 의 `start_gen` 위치로만 접는다 — suffix 가 shift-동치이고
  마지막 글자 위치의 상대 관계가 보존되므로 **fold 결정도 shift-동치로 동일**.
- **엄밀화**: `char_exists(g) = g < next_gen || !is_last_input` (core.rs:313).
  세그먼트 C 의 각 gen 은 구 파스에서 `is_last_input=false` 로 생성됐다 (마지막
  gen 제외). 편집 후 새 파스에서도 세그먼트 C 의 gen 들은 마지막이 아니므로
  `is_last_input=false` — **동일**. 마지막 gen (문서 끝) 은 새·구 모두 `true`, 그
  gen 의 leaf 도 위치 상대성 보존 → 동일 fold. 따라서 **문서 끝을 바꾸지 않는
  편집에 대해 eager EOF fold 는 splice 안전.**
- **반례 조건 (식별)**: 편집이 **문서 끝 근처** (마지막 글자를 삭제/삽입) 라 마지막
  gen 의 `is_last_input` 경계가 이동하면, 세그먼트 C 의 마지막 gen fold 가 달라질
  수 있다. 이 경우 수렴이 애초에 문서 끝까지 안 일어나거나 (편집이 꼬리에 있음 →
  재파스 꼬리가 짧아 splice 이득 자체가 미미, §0 사실2), splice 하더라도 마지막
  gen 은 세그먼트 B(새 파스) 에 포함되게 **수렴 판정에서 마지막 gen 을 제외**
  (q\* < 마지막_gen 인 경우만 splice) 하면 반례가 닫힌다. **설계: splice 는 q\* 가
  문서 끝보다 최소 1 gen 앞일 때만** (프로브가 이미 편집끝~파일끝 거리를 재고 있음).

### 2.5 재발산 불가 가정의 방어

- 프로브에선 재발산 0건이지만, splice 후 결과가 전체 재파스와 다르면 잡을 안전망:
  - **차등 오라클 (상시 게이트, §3)**: splice 결과 vs 전체 재파스 byte-identical.
    퍼징이 재발산을 잡는다.
  - **런타임 폴백 (프로덕션 안전망)**: §2.1 의 구조적 완전 일치 1회 검증이
    실패하면 (지문 충돌 또는 예기치 못한 불일치) **splice 포기하고 계속 파스**.
    최악의 경우 Phase I (prefix 재개) 성능으로 저하될 뿐 정확성 불변.
  - **선택적 셀프체크 모드** (디버그): `MG3_SESSION_VERIFY=1` 이면 매 edit 후
    전체 재파스를 병렬 실행해 byte-대조 (record_cond.rs 의 `MG3_RECORD_COND_DIFF`
    전례). 그린 확인 후 프로덕션은 끈다.

---

## 3. §3 게이트

### 3.1 차등 오라클 (완벽한 정확성 게이트가 공짜로 존재)

- **불변식**: 임의의 문서·임의의 편집 시퀀스에 대해, 세션의 `edit` 결과가 편집 후
  텍스트를 처음부터 전체 재파스한 결과와 **byte-identical**. 대조 표면은 소비자가
  보는 것 전부: **kernels_history + accept** (기본), 그리고 소비자가 AST proto 를
  쓰면 (mulang, §4.2) **ast.proto `ParseResult` 바이트**까지. 세 표면 중 하나라도
  어긋나면 fail — kernels_history 가 byte-동일이면 그로부터 파생되는 AST 도 동일
  하므로 kernels_history 대조가 근본 게이트, AST proto 대조는 소비자 경로 확인.
- **편집 퍼징 테스트 (상시 게이트)**: 코퍼스 × 무작위 편집 시퀀스 N회, **시드 고정**.
  각 편집 후 세션 결과와 전체 재파스를 대조. 프로브의 self-verification (identity,
  no-op) 을 세션 테스트로 승격 + 무작위 (삽입/삭제/치환 × 무작위 위치, 토큰 경계
  무시 — LSP 키스트로크 현실) 편집 시퀀스. `tests/` 에 `session_diff.rs` 로 추가.
- **기존 스위트 무영향**: 세션은 additive 레이어라 `parser_diff`·
  `cache_equivalence`·`accept_condition_diff`·`eof_cond_symbols`·`ffi_smoke` 는
  세션을 안 거치는 경로 (직접 `parse`) 를 그대로 탄다 — 세션 코드가 `parse_step`
  /`kernels_history` 를 **호출만** 하고 수정하지 않으면 무영향. 이 "수정 안 함" 이
  설계 제약 (아래 §6-R2 의 오프셋-인지 소비를 I2 범위 밖으로 미루는 이유).

### 3.2 Kotlin parity 는 범위 밖 (명시)

- 세션은 **native 전용으로 시작.** 소비자 (mulang LSP) 는 native 를 FFI 로 쓰고,
  Kotlin 파서에는 세션 상당물이 없다. Kotlin parity 는 명시적으로 범위 밖 —
  세션은 native 의 additive 레이어이고, native `parse`/`kernels_history` 는
  기존대로 Kotlin 과 `parser_diff` 로 parity 유지된다. **세션 결과 ≡ native 전체
  재파스 ≡ (기존 게이트로) Kotlin.** 삼단 논법으로 Kotlin parity 는 전이적으로 성립,
  세션이 새로 깨뜨릴 표면이 없다.

---

## 4. §4 단계별 구현 계획

각 단계 = 게이트 그린으로 종료. 게이트는 §3 차등 오라클 (+ 기존 스위트 무영향).
각 단계 후 메인 세션 리뷰 + 커밋.

### I0 — 세션 + 체크포인트 + prefix 재개 + 퍼징 게이트 (난이도: 중)

- `ParseSession` 구조 (§1.1) + 매 K gen 체크포인트 링 (§1.2, ctx 스냅샷 + history
  marker). `edit` 은 아직 **prefix 재개만** — 편집 위치 앞 체크포인트에서 재개해
  편집 구간부터 **파일 끝까지** 재파스 (splice 없음). 구 suffix 미사용.
- **게이트**: `session_diff.rs` 퍼징 — 세션 결과가 전체 재파스와 byte-identical.
  기존 스위트 전부 그린. 체크포인트 되감기 정확성 단위 검증 (임의 gen 에서 복원한
  ctx 로 이어 파스 == 처음부터 파스).
- **공수**: 1.5~2일. 리스크: 체크포인트 clone 비용 (§1.3) — history marker 방식으로
  회피. prefix 재개 자체는 자명하게 건전 (프로브 헤더).

### I1 — 지문 보존 + 수렴 감지 (splice 없이 카운터) (난이도: 중)

- 구 파스의 per-gen strict 지문을 매 edit 재파스 중 구 파스와 대조해 **수렴 gen q\*
  를 감지** — 하지만 splice 는 아직 안 함 (계속 파스). (현행 구현은 이 지문을
  upfront 벡터로 들지 않고 `BaselineWalker` 로 lazy 산출한다 — §2.1 갱신본 참고.)
  "수렴 이후 재사용 가능했던 gen 수" 를 카운터로 (실현 이득 사전 측정 — 프로브
  수치의 프로덕션 재확인).
- **게이트**: 결과 byte-identical 무변경 (지문·카운터는 계측만, 파스 무영향).
  수렴 카운터가 프로브 분포 (median 0 / p90 22) 와 정합 확인.
- **공수**: 1~1.5일. 리스크: 프로덕션 지문이 프로브 지문과 동일 값인지 (프로브
  코드를 lib 로 승격 — 현재 bin 전용). 지문 함수 재사용 검증.

### I2 — splice (핵심 난소) (난이도: 상)

- 수렴 지점 q\* 에서 구조적 완전 일치 1회 검증 (§2.1) → 통과 시 최종 ctx gen-rebase
  (§2.2-a, O(state) 1회) + history lazy 물질화 rope (§2.2-b rope-후보-2, §2.3).
  eager EOF fold 안전 조건 (§2.4 — q\* < 마지막 gen) 적용. 폴백 (§2.5) 배선.
- **게이트**: `session_diff.rs` 퍼징 byte-identical (splice 경로 활성). splice
  발동율·재발산 0 확인. `MG3_SESSION_VERIFY` 병렬 대조 모드로 개발 중 상시 검증.
- **공수**: 3~4일. **최대 리스크** — gen-rebase 정합 (§6-R1), 물질화 history 가
  전체 재파스 history 와 byte-동일 (§6-R2). 이 단계가 트랙의 무게중심.

### I3 — FFI 세션 API + mulang LSP 통합 노트 (난이도: 중)

- FFI 표면 (§4.1). mulang LSP 배선은 **노트만** (mulang 코드 수정 범위 밖, §4.2).
- **게이트**: `ffi_smoke.rs` 에 세션 라이프사이클 (new→parse_full→edit→free) 추가 —
  FFI 세션 결과가 직접 `parse` 결과와 동일. 메모리 누수 없음 (valgrind/asan smoke).
- **공수**: 1~1.5일. 리스크: FFI 세션 수명 관리 (§6-R3), UTF-16 오프셋 변환 배선
  지점 (§4.2, mulang 조사 기반).

### 4.1 FFI 세션 표면 (설계 스케치)

현행 `mgroup3_parser_*` (ffi.rs) 옆에 세션 심볼 추가 (기존 무변경):

```
mgroup3_session_new(parser: *mut Mgroup3Parser, err: *mut i32) -> *mut ParseSession
mgroup3_session_parse_full(sess, text_utf8, len, out_ptr, out_len) -> i32
mgroup3_session_edit(sess, pos_char, old_len_char, new_utf8, new_len,
                     out_ptr, out_len) -> i32
mgroup3_session_free(sess)
```

- **결과 포맷 = 소비자 계약 (미결정 #1).** 두 방출 경로:
  (a) `Mgroup3ParseResult` proto (kernels_history, `build_result` 재사용) —
  `Mgroup3NativeResult.kt` 디코더 대상. (b) **ast.proto `ParseResult`** — mulang
  이 실제로 소비하는 것 (현행 native 파스가 `mgroup3_gen_parse_ast` 로 냄, §4.2).
  세션도 소비자가 쓰는 (b) 를 내야 소비자 무변경 → 세션 심볼은
  `mgroup3_gen_session_edit` 등 **gen-종속 (grammar-specific) AST 방출**이 될
  가능성이 높다. 어느 쪽이든 세션 내부는 동일 (ctx→결과 조립); 방출 어댑터만 다름.
  splice 는 결과 포맷과 무관 (ctx 를 정확히 재구성하는 게 splice, 방출은 그 위).
- 세션은 파서 핸들을 **빌림** (`Arc` 승격 또는 세션이 핸들 수명 계약 문서화).
  현행 파서 핸들은 `Box::into_raw` 이므로, 세션이 핸들보다 오래 살면 안 된다는
  계약 — 또는 파서를 `Arc<Mgroup3Parser>` 로 승격해 세션이 클론 보유 (권고, §6-R3).
- `pos_char`/`old_len_char` 는 **char (코드포인트) 오프셋.** LSP 의 UTF-16 오프셋
  변환은 호스트(JVM) 측 책임 — FFI 경계는 char 단위로 고정 (§4.2, §6-R4).

### 4.2 mulang LSP 통합 노트

*(조사 기반. mulang 코드 수정은 범위 밖 — 배선 지점과 오프셋 변환 계약만.)*

**현행 배선 (조사 결과):**
- LSP 는 Kotlin (lsp4j). `/Users/joonsoo/Documents/workspace/mulang/lsp/main/kotlin/
  com/giyeok/mulang/lsp/` — `MulangLanguageServer.kt`, `MulangTextDocumentService.kt`.
- **native 를 FFI 로 씀 (선호), Kotlin mg2 폴백** (에러 위치 얻기용). native 경로:
  `NativeMulangParser` → `GeneratedAstNativeBridge` (jparser 의
  `mgroup3/nativeParser/kotlin/.../GeneratedAstNativeBridge.kt`). 바인딩 심볼:
  `mgroup3_parser_new_from_file_cached`, `mgroup3_parser_free`, `mgroup3_free_buffer`,
  그리고 파스는 **`mgroup3_gen_parse_ast`** (주의: `mgroup3_parser_parse` 가 아님 —
  mulang 브릿지는 ast.proto `ParseResult` 를 내는 gen 심볼을 쓴다). 세션 FFI 도
  **ast.proto 를 내는 세션 심볼** (`mgroup3_gen_session_*`) 이 필요할 수 있다 —
  §4.1 은 kernels-history proto 를 냈지만 mulang 은 AST proto 를 소비한다. 세션이
  낼 결과 포맷은 소비자 계약에 맞춰야 함 (상위 세션 확인 — §8 참조).
- **매 키스트로크 전체 재파스.** `didChange` → `onDocumentChanged(uri, text)` →
  200ms 디바운스 후 `Bibix4Workspace.load` 가 전체 문서를 재파스. **증분·range-diff·
  suffix 재사용 전무.** 유일한 절약은 `MulangAstCache` (SHA-256(sourceId+text) 키
  디스크 캐시) — full-text 키라 키스트로크마다 미스.
- **문서 동기화 = Full** (`TextDocumentSyncKind.Full`). `didChange` 는 항상 전체
  교체 텍스트를 나름 (range 아님).

**세션이 배선될 지점 (JVM seam):**
- `NativeMulangParser` (파서 핸들 `MemorySegment` 보유) 가 **문서당 세션을 들
  자연스러운 자리** — 현재는 stateless `parse(text)` 만 노출.
  `GeneratedAstNativeBridge` 가 세션 FFI 심볼을 기존 `mgroup3_*` 옆에 바인딩할 곳.
- **call-site seam**: `Bibix4Workspace.load` 의 `parseAst: (sourceId, text) ->
  CompileUnit` 클로저 (Bibix4Workspace.kt:92-98) — 모든 파일 파스가 지나는 단일
  간접점. 증분 경로는 full-text 캐시 조회 대신 sourceId(경로) 당 세션을 유지.
- **편집 공급**: 현행 `Full` 동기화라 세션 `edit(pos, oldLen, newText)` 를 먹이려면
  (a) `TextDocumentSyncKind.Incremental` 로 바꿔 range 를 직접 받거나, (b)
  `onDocumentChanged` 에서 구 full-text 대 신 full-text 를 diff. mulang 은 구
  텍스트를 이미 보유 (`openDocuments`/`DocumentState.source`) 하므로 (b) 가 mulang
  변경 최소. **어느 쪽이든 세션 FFI 계약은 char-오프셋 edit 하나 (§4.1) 로 고정.**

**오프셋 변환 계약 (R4 의 프로덕션 근거):**
- 세 오프셋 단위가 있다: (i) LSP Position = UTF-16 코드유닛, (ii) 현행 JVM
  "offset" = Kotlin String 인덱스 (= UTF-16 코드유닛, `SourceIndex`/`SourceMap`),
  (iii) **Rust 파서 gen/span = 코드포인트 인덱스** (core.rs:1057 `text.chars()`).
- **현행 스택은 이 셋을 암묵적으로 동일시** — BMP-only 빌드파일엔 정확하나
  astral-plane 문자엔 틀림 (코드포인트≠UTF-16). 아무도 정규화 안 함.
- **증분 설계는 이 변환을 FFI 경계에서 명시화해야.** 편집: LSP UTF-16 range →
  (SourceIndex) Kotlin String 인덱스 → **코드포인트 오프셋** (세션 edit 호출용).
  결과: 코드포인트 span → UTF-16 (진단/정의/호버 Position). **FFI 경계는 코드포인트
  오프셋 고정** (§4.1) — 변환은 mulang(호스트) 책임. mulang 이 이 identity 를
  깨야 하는 시점 (astral 지원) 은 별개 이슈지만, 세션 도입이 코드포인트↔UTF-16
  변환을 **드러나게** 만든다 (기존 암묵 identity 를 명시 변환으로 교체).

**소비 (참고):** native 경로는 kernels-history 가 아니라 **ast.proto `ParseResult`**
를 소비 → `MulangAst.CompileUnit` → `SourceMap`/진단. 진단은 컴파일 에러 (파서 직접
아님) 를 `SourceMap` 으로 Range 매핑. 세션이 AST proto 를 내야 소비자 무변경 (위
§4.1 결과 포맷 판정과 연결).

---

## 5. §5 성능 목표와 측정 계획

### 5.1 편집 지연 기대치 (모델)

편집 지연 = `체크포인트 되감기 (~K/2 gen)` + `편집 구간` + `수렴 거리 (~22 gen p90)`
+ `splice (rebase O(state) 1회 + history 물질화 O(N))` + `출력 조립 O(N)`.

| 파일 크기 N | 편집 위치 | Phase I 만 (재파스 꼬리) | Phase I2 (splice) |
|---|---|---|---|
| ~10k자 | 25% | ~7.5k자 재파스 | ~K/2+수렴(≤44) 재파스 + O(N) 조립 |
| ~10k자 | 75% | ~2.5k자 재파스 | 동상 (짧은 꼬리라 이득 작음) |
| ~40k자 | 25% | ~30k자 재파스 | ~K/2+수렴 재파스 + O(N) 조립 |

- **핵심**: splice 는 **파스 재계산**을 `K/2 + 수렴거리(≤44 p90)` gen 으로 상수화
  한다 (편집 위치·파일 크기 무관). 남는 O(N) 은 **출력 조립** (history 물질화 +
  kernels_history) — 이건 splice 유무와 무관하게 이미 존재하던 비용 (§2.2 판정).
  즉 splice 의 이득 = 초중반 편집의 긴 재파스 꼬리 (14.1k/6.3k자) 제거.
- **출력 조립 O(N) 이 병목이 되면** rope-후보-1 (오프셋-인지 소비, §2.2) 이
  다음 최적화 — I2 이후 백로그.

### 5.2 측정 하니스

- **편집 시퀀스 리플레이 바이너리** (`src/bin/session_bench.rs` 신규): 코퍼스 파일
  + 편집 시퀀스 (프로브 `build_edits` 재사용) 를 세션에 리플레이, 편집당 지연을
  **콜드 (parse_full) / 웜 (edit) 분리** 측정. 현행 `time_parse` 패턴 준수 —
  셀당 반복, sd 보고.
- **대조군**: 같은 시퀀스를 **전체 재파스** (`parser.parse` 매번) 로 돌린 지연.
  세션 대 전체재파스 speedup 을 편집 위치·파일 크기별 표로.
- **분리 원칙** (mgroup4 phase_g §5.2 교훈): 유휴 확인, JVM 미혼입 (native 단독
  측정), 콜드/웜 분리.

### 5.3 메모리 오버헤드 목표

- 체크포인트 링: `N/K` 개 × (paths 골격 + history marker). history 를 세그먼트
  로그로 공유하면 (§1.2/§1.3) 체크포인트당 O(paths 골격) — **파일 크기 대비
  선형이되 계수 작음.** 목표: 체크포인트 총 오버헤드 ≤ history 1벌 크기.
- splice 구 suffix 소스 (§2.3): 직전 파스 history 1벌 (`Arc`). 목표: 상수 1벌.
- **총 목표**: 세션 메모리 ≤ (파스 1벌 상태 + history) × 상수 (~2~3배). §5.2
  하니스로 실측.

---

## 6. §6 리스크와 미결정

### R1 — gen-rebase 정합 (최대 리스크, I2)

splice 시 최종 ctx 의 gen 을 `+delta` 재구성 (§2.2-a) 할 때, 절대 gen 소비 지점
(§0 사실4) 을 **하나라도 빠뜨리면** kernels_history/accept 가 어긋난다.
`AcceptCondition` leaf 3종 (start_gen/min_end_gen/end_gen), `MilestonePath` 의
gen_idx + 보고 shadow 2개, `PathRoot.start_gen`, `ever_seen_cond_roots`,
`root_report_gens` 전부.
- **완화**: I2 에서 `MG3_SESSION_VERIFY` 병렬 대조 (전체 재파스와 매 edit byte-
  대조) 를 개발 상시 켜고, 그린 확인 후 프로덕션 폴백 (§2.5) 만 남김. 퍼징이
  rebase 누락을 즉시 잡는다 (재발산 플래그).

### R2 — history 물질화 vs 오프셋-인지 소비 (침습성 미결정)

§2.2 판정은 rope-후보-2 (lazy 물질화) — 소비 경로 무변경, 출력 조립 O(N) 유지.
rope-후보-1 (오프셋-인지) 은 RecordConditionEvaluator/kernels_history 를 오프셋
인지로 고쳐야 해 **침습적** (기존 스위트가 이 코드를 공유 — §3.1 무영향 계약과
충돌 위험).
- **미결정 (상위 세션 확인 #3)**: I2 는 물질화로 시작 (출력 조립 O(N) 수용).
  오프셋-인지 소비는 출력 조립이 실측 병목일 때만 별도 트랙. 승인 요청.

### R3 — FFI 세션 수명 관리

세션이 파서 핸들보다 오래 살면 dangling. 현행 핸들은 `Box::into_raw`.
- **미결정 (상위 세션 확인 #4)**: 파서를 `Arc<Mgroup3Parser>` 로 승격해 세션이
  클론 보유 (권고 — 수명 얽힘 해소) vs "세션 < 핸들 수명" 계약 문서화 (변경 최소).
  전자 권고 — FFI 사용자 실수 방어.

### R4 — LSP 오프셋 변환 (UTF-16 ↔ char/byte)

LSP 프로토콜은 Position 을 UTF-16 코드유닛 오프셋으로 준다. 세션 FFI 는 char
(코드포인트) 오프셋 (§4.1). 변환 책임 경계가 미결정.
- **판정**: FFI 경계는 char (코드포인트) 오프셋 고정 (native 는 `Vec<char>` 로
  문서 보유 — 파서 gen 단위와 일치, core.rs:1057). UTF-16→코드포인트 변환은
  **호스트(mulang) 책임** — mulang 이 문서 텍스트를 이미 보유 (`SourceIndex`).
  주의: **현행 mulang 스택은 코드포인트=UTF-16 을 암묵 동일시** (BMP 만 정확, §4.2)
  — 세션 도입이 이 변환을 드러나게 만든다. mulang 변경 범위 밖이나, FFI 계약이
  코드포인트 오프셋임을 명시해 mulang 이 명시 변환을 넣을 지점을 §4.2 에 기록.

### R5 — 미결정 목록 (상위 세션 확인)

- **K (체크포인트 간격)**: 권고 64. §5 측정 후 파일 크기별 확정.
- **history 세그먼트 로그 분리 시점**: I0 marker 방식 → I2 세그먼트 rope. I0 에서
  분리를 미리 할지 (선제) vs I2 에서 (지연) — 지연 권고 (게이트 우선).
- **splice 마지막-gen 제외 규칙** (§2.4): q\* < 마지막 gen 일 때만 splice. 문서 끝
  편집은 어차피 재파스 꼬리가 짧아 이득 미미 — 규칙이 반례를 닫으면서 이득 손실 없음.

---

## 7. 예상 공수 요약

| 단계 | 내용 | 난이도 | 공수 | 주 게이트 |
|---|---|---|---|---|
| I0 | 세션+체크포인트+prefix 재개 | 중 | 1.5~2일 | session_diff 퍼징 + 기존 스위트 |
| I1 | 지문 보존+수렴 감지 (카운터) | 중 | 1~1.5일 | 결과 무변경 + 카운터 정합 |
| I2 | splice (rebase+물질화 rope) | 상 | 3~4일 | 퍼징 byte-identical + 병렬 대조 |
| I3 | FFI 세션 + mulang 통합 노트 | 중 | 1~1.5일 | ffi_smoke 세션 + 무누수 |

**합계: ~7~9일.** 무게중심은 I2 (splice) — gen-rebase 정합과 history 물질화 parity.
I0/I1 은 자명하게 건전한 prefix 재개 + 계측이라 게이트 통과 경로가 짧다.

---

## 8. 요약 — 상위 세션이 확인할 설계 결정

**대전제 (확인 불요, 명시)**: 세션은 additive 레이어, native 전용.
`parse_step`/`kernels_history` 무수정, 차등 오라클 (전체 재파스 byte-identical) 이
정체성. Kotlin parity 는 전이적 성립 (§3.2) — 범위 밖.

**확인 필요한 결정 (승인 대상):**

1. **splice history = lazy 물질화 rope (rope-후보-2)** — 파스 재계산만 증분화,
   출력 조립 O(N) 은 수용 (splice 유무 무관하게 이미 존재하던 비용). 오프셋-인지
   소비 (rope-후보-1) 는 출력 조립이 실측 병목일 때만 별도 트랙 (§2.2, R2). →
   **I2 를 물질화로 시작하는 것 승인?**
2. **결과 방출 포맷 = 소비자 계약** (§4.1). mulang 은 kernels-history 가 아니라
   **ast.proto `ParseResult`** 를 소비 (`mgroup3_gen_parse_ast`). 세션 FFI 도
   gen-종속 AST 방출 (`mgroup3_gen_session_*`) 이어야 소비자 무변경. → **세션 방출
   포맷을 AST proto 로 확정?** (kernels-history 방출은 native 자체 게이트용.)
3. **파서 핸들 `Arc<Mgroup3Parser>` 승격** 권고 (세션이 클론 보유, FFI 수명 얽힘
   해소, R3) vs "세션 < 핸들 수명" 계약 문서화 (변경 최소). → **Arc 승격 승인?**
4. **splice 는 q\* < 마지막 gen 일 때만** (eager EOF fold 반례 차단, §2.4). 문서 끝
   편집은 재파스 꼬리가 짧아 이득 손실 없음. → **이 안전 규칙 확정?**
5. **K=64 체크포인트 간격 + history marker 방식** (I0 는 history clone 회피, I2 에서
   세그먼트 rope 전환). → **K 초기값·분리 지연 승인?** (§5 측정 후 K 재조정.)

**부수 판정 (확인 불요, 조사 기반 고정)**: FFI 경계 = 코드포인트 오프셋; UTF-16
변환은 호스트 책임 (R4, §4.2 — mulang 의 암묵 identity 를 명시화).
