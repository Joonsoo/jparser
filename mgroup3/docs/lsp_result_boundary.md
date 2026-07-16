# LSP 편집당 결과-반환 경계: 부분 결과 반환 / AST 재사용 설계 검토

2026-07-17. incremental session 의 O(n²) history 복사 수정(2f6d5b17) 이후 남은
편집당 O(n) 경계 — `mgroup3_gen_session_edit` 가 매 편집마다 문서 전체 AST 를
walk→ast.proto 인코딩해 반환하는 것 — 을 어떻게 없앨 수 있는지의 설계 검토.
**검토 문서일 뿐 구현 착수 전이며, mulang 쪽 코드는 변경하지 않는다** (사용자
지시: mulang 작업 진행 중).

## 0. 실측 — 경계가 지배적이다

프로브: scratchpad `gen_session_probe` (libloading 으로 mulang 의
`libmulang_parser.dylib` + `mulang-mg3-parserdata.pb.gz` 구동, 합성 mulang 문서,
주석 안 1글자 삽입, 7회 median). 세션 자체 비용은 같은 parserdata 로
`quadratic_probe` 재실행해 대조.

| n (chars) | FFI END edit ms | 세션 edit 자체 ms | 경계(walk+encode) ms | 경계 비중 | 반환 bytes |
|---:|---:|---:|---:|---:|---:|
| 1,053 | 6.7 | 0.60 | 6.1 | 91.0% | 5.3 KB |
| 4,001 | 24.4 | 0.75 | 23.6 | 96.9% | 20.5 KB |
| 16,053 | 96.1 | 0.93 | 95.2 | 99.0% | 80.4 KB |
| 32,028 | 191.5 | 0.88 | 190.6 | 99.5% | 166 KB |
| 64,049 | 397.8 | 1.41 | 396.4 | 99.6% | 338 KB |

- 경계 비용은 엄밀히 O(n): **~6.0 µs/문자** (60× 범위에서 평탄), 반환 **~5.1
  bytes/문자**. 64k 문자 문서에서 키 입력당 ~400ms + 330KB.
- 이 위에 JVM 쪽이 더 얹힌다 (proto 디코드 + `CompileUnit` 재구축 — 미실측,
  인코딩과 동차수로 추정) + IR1 재컴파일.
- LSP 는 recompile 디바운스로 키 입력을 합치므로 정확한 단위는 "recompile 당"
  이지만, 진단 지연(사용자 체감 레이턴시)에 이 경계가 그대로 들어간다.
- 결론: **"현상 유지" 옵션은 기각.** 세션 증분화의 이득(~1ms)이 경계에 완전히
  가려져 있다. 상수 최적화(인코더 버퍼 재사용 등)로는 O(n) 자체가 안 없어진다.

## 1. splice 가 공짜로 주는 재사용 구조

편집 (pivot p, delta) 이 splice 로 처리되면 (LSP 타이핑의 지배 케이스 — 수렴
거리 median 0 / p90 22자):

- 히스토리 = 새로 파싱된 프리픽스 `[0..=q*]` + 옛 서픽스 rebase(+delta).
  History 컨테이너(2f6d5b17) 덕에 두 영역이 Rc 청크로 물리 공유된다.
- AST 관점: 스팬이 편집 창 `[resume_gen, q*]` 와 교차하지 않는 서브트리는
  - (a) 창 앞 — 완전 동일 (스팬 포함),
  - (b) 창 뒤 — 구조 동일, 스팬만 일괄 +delta.
- 실제로 변하는 것 = 창을 덮는 스파인 (루트→창 경로, O(depth)) + 창 내부
  서브트리. 스팬은 중첩되므로 "창과 교차하는 노드" = 스파인 ∪ 창 내부로 닫힌다.

## 2. 재사용의 soundness 조건

walk 는 `history[end_gen].find_by_begin_gen(seq, ptr, begin)` 조회의 재귀이고,
kernels_history 는 record 조건을 record gen 에서 **전방으로만** 재생해 게이트한다
(record_cond.rs). 따라서:

- **서픽스 (gen > q\*)**: record 의 조건 평가가 서픽스만 본다 → 옛 결과와
  shifted-identical. 무조건 재사용 가능. (splice 자체가 differential oracle 로
  byte-identical 을 보증하는 것과 같은 이유.)
- **프리픽스 (gen < resume)**: record 조건이 편집 창을 넘어 전방을 볼 수 있어
  무조건 재사용은 불가. 단 `RecordConditionEvaluator` 의 per-root
  `last_active` (연속 활성 구간, record_cond.rs:60-62) 로 "그 record 의 평가
  창이 p 이전에 닫혔는지" 판별 가능 → 안전 프리픽스 경계 s = (모든 관련 root 의
  last_active < p 인 최대 gen) 을 계산하고, `[s, q*]` 만 재평가.
- **폴백**: splice 미발생 / 파스 에러 / 경계 계산 불가 시 현행 전체 결과 반환.
  정확성은 항상 전체-재파스 동치 (기존 오라클 패턴을 델타에도 확장).

## 3. 옵션 비교

### A. 델타 프로토콜 + 스팬 시프트 (권장)

FFI 가 edit 후 전체 결과 대신 델타를 반환:

```proto
message ParseDelta {
  int32 base_version = 1;   // 소비자가 든 직전 결과의 버전 (불일치 시 full 요구)
  int32 new_version = 2;
  int32 root_id = 3;
  int32 shift_pivot = 4;    // start/end > pivot 인 재사용 노드는 +shift_delta
  int32 shift_delta = 5;
  repeated NodeEntry patched = 6;  // 새/변경 노드 (스파인 + 편집 창)
}
```

- **Rust 증분 walk**: 세션이 직전 walk 결과(노드 테이블 + begin_gen→node 색인)
  를 보유. walk 재귀에서 자식 스팬이 안전 영역(§2)이면 이전 노드 ID 로
  short-circuit; 스파인+창만 새로 walk. walk 가 조회하는 gen 들만 on-demand
  kernels 재구성 (평가기 인덱스는 O(n) int 작업 — 수용 가능, 필요시 증분 유지).
- **ID 안정성**: 현행 `next_id()` 는 walk 순서 순차라 매 편집 전체 재번호화됨
  → 재사용 노드는 직전 결과의 ID 를 유지하고, 새 노드는 직전 max_id 이후 번호
  (세션 수명 동안 단조 증가; 세션 재시작 시 리셋).
- **스팬 처리**: 노드에 start/end 절대값이 내장 (proto 필드 100/101, Rust/Kotlin
  구조체 동일). 재사용 노드의 스팬 보정 방식:
  1. **in-place 시프트 (권장)** — Rust 캐시와 Kotlin 보유 트리 양쪽에서 pivot
     이후 노드의 start/end 를 제자리 갱신. O(#서픽스 노드) int 쓰기 — 64k 문서
     ~65k 노드 × 2 int ≈ 수십 µs. 노드 객체 자체는 공유 유지. Kotlin 쪽은
     스팬 필드를 var 로 (또는 별도 mutable span holder) 바꾸는 codegen 변경
     필요 — **공유된 트리를 소비자가 스냅샷으로 든다는 가정이 깨지므로**,
     소비 계약 (recompile 중에만 읽음 / 편집과 동시 접근 없음 — 현행
     LSP 세션 관리자의 락 규약과 일치) 을 문서화해야 함.
  2. 사이드 테이블 — nodeId→(start,end) 를 편집마다 재구축 (프리픽스 memcpy +
     서픽스 +delta). 노드는 불변 유지되나 스팬 접근 API 가 테이블 경유로 바뀜
     (모든 소비자 수정).
  3. red-green 분리 (옵션 C) 의 부분 도입.
- **Kotlin 패치**: `MulangAstProtoBinding` (jparser Stage3 생성물) 에 델타 적용
  경로 추가 — byId 맵에서 재사용 서브트리 객체 공유, patched 만 재구축, 스팬
  시프트 적용. 직전 결과 보유는 이미 LSP 세션 관리자가 함.
- **예상 비용**: 편집당 O(depth + 창 + 서픽스 int 쓰기) ≈ 1–3ms (64k 기준
  ~100×). 반환 바이트도 KB 미만.

### B. Rust 측 트리 보유 + 질의 FFI

Kotlin 이 전체 AST 를 재료화하지 않고 Rust 가 트리를 들고 LSP 질의(위치→노드,
진단 등)를 FFI 로 서빙. IR1 파이프라인이 Kotlin 이라 아키텍처 대개편 —
**현 시점 기각** (mulang 전면 개편 필요).

### C. red-green 트리 (위치-프리 노드)

노드가 상대 폭만 갖고 절대 위치는 lazy 계산 (Roslyn 식). 서브트리를 스팬 시프트
없이 그대로 공유 — 이론상 최선. 그러나 Stage2/3/4 codegen 전면 변경 + proto
스키마 변경 + 모든 소비자의 스팬 접근 변경. **A 의 in-place 시프트가 상수
부족으로 판명될 때만** 승격 (그럴 가능성 낮음 — 시프트는 수십 µs).

### D. 현상 유지

§0 실측으로 기각. 8k 문자에서 이미 recompile 당 ~48ms 가 경계에서 소모.

## 4. 다음 층: 소비자 쪽 의미 분석 (IR) 재컴파일

경계를 고쳐도 소비자(LSP)가 편집마다 수행하는 의미 분석(AST→IR 재컴파일)은
별개의 O(n) 층으로 남는다. 소비자 파이프라인을 검토한 결론만 요약하면
(상세 분석은 소비자 저장소 쪽 기록으로): AST→IR 하위 변환은 def 단위로
국소적이라 A 안이 주는 서브트리 객체 동일성을 캐시 키로 삼을 수 있는
구조이나, ID 발급의 위치 의존성과 상위 단계의 전역 의미 분석(타입 추론·
오버로드) 때문에 ID 안정화 + 의존성 추적이 전제된다. 현실적 첫 수는 def
단위가 아니라 **파일 단위 재사용**(편집 안 된 파일의 IR 통째 스킵)이다.

**이번 트랙의 범위는 AST(파스 결과) 층까지로 한정한다** — IR 층 증분화는
소비자 저장소의 후속 트랙 (위험도·규모가 별개 프로젝트급).

## 5. 권고 및 단계

1. **A1 — 델타 프로토콜 + in-place 스팬 시프트** (jparser 쪽: Stage4RustEmit 의
   walk/encode/ffi emitter + Stage2 proto emitter 에 ParseDelta + Stage3 바인딩
   델타 적용 경로; 세션 FFI 에 버전/캐시). 정확성 게이트 = "델타 적용 결과 ==
   전체 walk 결과" differential oracle (session_diff 패턴 확장).
2. **A2 — mulang 배선** (SessionParseManager 가 델타 경로 사용): mulang 작업
   재개 후. 이때 IR1 메모이제이션 (§4) 도 같이 검토.
3. 규모: I2 splice 트랙과 동급의 프로젝트 (증분 walk 의 soundness + 오라클이
   본체). 프로토콜/ID/스팬 규약은 이 문서 §3-A 를 기준으로.

## 부록: 프로브 재실행

```
cd <scratchpad>/gen_session_probe && cargo build --release
./target/release/gen_session_probe \
  <mulang>/parser/generated/resources/native/darwin-aarch64/libmulang_parser.dylib \
  <mulang>/parser/generated/resources/mulang-mg3-parserdata.pb.gz \
  --sizes 1,2,4,8,16,32,64 --reps 7
```

(세션 자체 대조: mgroup3-native `quadratic_probe` 를 같은 parserdata 로.)

## 6. Stage 2 설계 — 델타 walk (Rust 측, 확정 계약)

Stage 1 (`EditReuse`/`KernelsQuery`, 74ada6c7) 위에서 생성 crate 가 편집당
전체 walk+encode 대신 델타를 산출한다.

### 재사용 판정 (AST 노드 단위)
노드 스팬 [b,e] (new gen 좌표):
- `e < dirty_lo` → **VERBATIM** 재사용 — old node id 그대로, 스팬 불변.
- `b > dirty_hi` → **SHIFT** 재사용 — old node id 그대로 (old 좌표 =
  (b−delta, e−delta)), 스팬은 소비자가 pivot/delta 규칙으로 시프트.
- 그 외 (dirty 창과 교차 — 스파인 + 창 내부) → 재구축.
근거: walk 가 [b,e] 노드에서 수행하는 모든 kernels 조회는 [b,e] 안의 gen 에
국한되고, Stage 1 계약이 그 구간의 kernel 동일성(그대로/시프트)을 보증하므로
old 서브트리와 동형이다.

### lockstep 대응 (old node id 획득)
스파인 노드 재구축 시 자식 좌표는 **정상 walk 조회** (`KernelsQuery.at`) 로
도출하고, safe 자식의 old id 는 old 스파인 counterpart NodeEntry 의 해당
필드에서 취한다: 스칼라 자식 필드 = 그대로 대응, repeated = 앞쪽(끝<dirty_lo)
은 앞에서부터, 뒤쪽(시작>dirty_hi, old 좌표로 환산) 은 뒤에서부터 인덱스
대응, 중간은 재귀 재구축. old counterpart 는 루트에서 스파인을 따라 내려가며
유지한다. 대응이 성립하지 않는 예외 상황 발견 시 그 노드는 통째 재구축
(정확성 우선 — 오라클이 심판).

### ID / 세대 관리 (생성 crate 세션 상태)
직전 결과 보유: `Vec<NodeEntry>`(prost) + id→index 맵 + root id + max_id +
version 카운터. 재구축 노드 id = max_id+1.. (세션 수명 동안 단조).
freed = old 스파인 노드 id + 드롭된 중간 서브트리의 전이 id (old 테이블을
자식 id 로 DFS — O(창) 크기).

### ParseDelta (생성 ast.proto — Stage2ProtoEmit)
`base_version, new_version, root_id, shift_pivot, shift_delta,
repeated NodeEntry patched, repeated int32 freed_ids`.

### FFI (생성 crate — Stage4RustEmit)
`mgroup3_gen_session_edit_delta`: splice + 직전 결과 있으면 ParseDelta,
아니면 full `ParseResult` 폴백 (상태코드로 구분). 기존
`mgroup3_gen_session_edit` 는 무변경 (소비자 opt-in).

### 오라클 (필수 게이트)
"델타 적용 재구성 (old 테이블 + patched + 스팬 시프트 + freed 제거) == 같은
편집의 full walk+encode" — proto 수준 전수 비교, Stage 1 오라클과 같은 퍼징
코퍼스, 문법 asdl + mulang. 폴백 경로 (미splice/에러/버전 불일치) 포함.

### Stage 3 (별도)
Kotlin 바인딩 applyDelta + 생성 Kotlin AST 스팬 var 화 (Stage3KotlinEmit /
KotlinOptCodeGen) — 소비자 배선과 함께.
