# mgroup4 (가칭): bounded interior milestone groups — 아이디어와 Phase 0 실험 지시서

작성: 2026-07-09. 상태: **아이디어 검토 완료, Phase 0 (계측) 착수 대기** — 알고리즘
구현 전. 방법론 전례: `watcher_main_sharing.md` (ceiling probe 로 기각),
`kernels_history_optimization.md` (엔진 트랙 측정 방식).

## 1. 배경 — 남은 비용이 어디 있고, 왜 파서 구현의 몫인가

2026-07-09 엔진 트랙 (rkyv/hist dedup/step-6/mimalloc/KernelSet 융합) 과 mulang
문법 트랙 (1b lookahead 1자화 + 1a `<CallChain_+>` 제거, 차등 게이트 델타 0) 이후에도
남는 비용은 Phase B 잔여 분석 (kernels_history_optimization.md §0.3) 이 밝힌
**곱셈적 fork** 들이다 — 같은 문자들을 스캔하면서 경로의 일부 노드만 다른 경로들:

- 문자열 인자 뒤 우선순위 타워 ×5 (sym1233~1259 8종이 같은 span 에 — "이 인자가
  이항연산으로 이어질 가능성"이 우선순위 레벨마다 하나씩) — main + 워처 공통
- 중첩 블록 ×2-3, attach-gen 변형 ×1.5
- `<AddExpr>` 가족 워처 (mulang jar.bbx peak 의 47%)

이들을 문법에서 제거하는 트랙 (spacing/밀착 규칙, mulang_grammar_ambiguity.md §10.1)
은 **mulang 언어 설계 결정으로 최종 기각** (2026-07-09: `a<b` 는 비교로 해석돼야
하고 개행 continuation 도 언어의 일부). 즉 이 fork 들은 언어 정의에 내장된 비용이고,
회수하려면 파서 구현이 감당해야 한다.

주의 — **watcher-main 공유 기각 (ceiling 1.00×) 과는 다른 축이다.** 그 실험은 워처
경로와 main 경로 *사이* 의 구조 공유였고 (live 구조가 disjoint 라 기각), 이 문서는
같은 부류 (main 이면 main, 워처면 워처) *안에서* interior 만 다른 경로들의 병합이다.
그 ceiling 측정은 이 아이디어에 대해 아무것도 말해주지 않는다.

## 2. 아이디어

### 2.1 core: interior milestone group

현 mgroup3 의 path 는 [singleton milestone, ..., singleton milestone] + **tip 만
milestone group** 이다. §1 의 fork 들은 공통적으로 "tip (스캔 위치) 은 같고 interior
한두 노드만 다른" 경로 집합 — 각각 별개 path/shape 로 전 스캔 구간의 비용을 낸다.

interior 위치에도 group 을 허용하면 이들이 `[공유 prefix][GROUP{...}][공유 suffix/tip]`
하나로 접힌다. 분열(unpack)은 그 노드가 실제로 행동을 가르는 시점 — 자식 subtree 가
완료되어 progress 가 전파될 때 (reduce), 또는 그 노드 자체가 tip 이 되어 전이할 때 —
에만 일어난다. 긴 인자를 스캔하는 동안 (비용의 대부분) 은 병합 상태로 간다.

이는 GLR 의 GSS (graph-structured stack) / local ambiguity packing 과 동형이라
이론적 건전성 선례가 확실하다. mgroup 계열의 차별점 (tip 전이의 parserdata
precompute) 을 유지한 채 스택 내부 공유를 얹는 하이브리드로 볼 수 있다.

### 2.2 bounded 변형 (이 문서의 제안): 마지막 n 개 노드만 group 허용

full interior group 의 두 장애물을 **사용자 선택 파라미터 n (>=1)** 으로 통제한다:

- (a) precompute 폭발: 임의 위치 group 을 허용하면 "가능한 interior group 집합 ×
  자식 완료" 전이의 사전 열거가 조합 폭발할 수 있다. 마지막 n 개로 제한하면
  테이블 공간이 depth-k (k<=n) 별로 유계.
- (b) 런타임 동적 집합 비용: window 가 유계면 group 표현/비교도 유계.

의미론 스케치:

- path 의 tip 쪽 마지막 n 개 노드가 group 일 수 있다. **n=1 ≡ 현 mgroup3** (하위 호환
  — 같은 코드베이스에서 n 을 올려가며 A/B 가능해야 한다).
- expand 로 path 가 자라면 window 가 앞으로 밀린다. group 노드가 window 밖 (tip 에서
  n+1 번째) 으로 밀리는 순간 멤버별로 분열한다 — **지연 분열**. 그 전에 자식 subtree
  가 완료되어 pop 되면 분열은 아예 일어나지 않는다 (이게 이득의 원천).
- reduce (자식 완료) 시 group 멤버별 반응이 갈리면 그 시점에 분열. 갈리지 않으면
  (같은 전이) group 유지.

### 2.3 핵심 불확실성 — 그래서 Phase 0

**fork 노드가 스캔 중 tip 에서 얼마나 깊이 있나**가 bounded 변형의 성패를 가른다:

- 타워 fork 의 상이 노드는 "인자 subtree 가 타워에 매달린 지점" — 인자가 평평하면
  (문자열 리터럴 등) tip 근처 (depth 1~3) 에 머물고, 인자 중첩이 깊어지면 tip 에서
  멀어진다. 얕은 경우가 지배적이면 작은 n 으로 대부분 병합되고, 깊은 경우가
  지배적이면 window 탈출 분열로 이득이 소멸한다.
- 이 분포는 **지금 mgroup3 계측으로 알고리즘 변경 없이 잴 수 있다.** 구현 착수 전에
  재서 결정한다 (sharing_ceiling 전례).

## 3. Phase 0 — 계측 probe (알고리즘 무변경)

구현 위치: Kotlin `Mgroup3Parser` 계측 경로 (기존 profile/shape 카운트 인프라 재사용).
파스 결과에 영향이 없어야 한다 (기존 스위트 그린 유지가 전제).

### P1. interior-merge ceiling

각 gen 의 live paths/shapes 를 "tip 쪽 마지막 n 노드 window 안에서 상이 노드를
마스킹한 구조 키"로 재집계 — n ∈ {1 (기준 = 현행), 2, 3, 4, ∞ (full interior)}.
- 마스킹 규칙: 같은 길이·같은 나머지 노드, window 안 정확히 한 노드만 다른 path
  들을 한 그룹으로 (일반화: k 개 상이까지 — 1차는 k=1 로 충분).
- 산출: gen 별 shape 수 (기준 대비 감소율), peak / 평균, main·워처 분리 집계.

### P2. fork-depth 분포

P1 에서 병합된 각 집합에 대해 상이 노드의 tip-거리 (depth) 를 기록 — depth 히스토그램
(gen 가중). "n=2 로 커버되는 병합의 비율 / n=3 / n=4" 를 직접 산출.

### P3. window-exit vs pop — 지연 분열의 실익

P1 의 병합 상태를 시간축으로 추적: 병합 집합이 (i) window 탈출로 강제 분열하는
사건 vs (ii) 분열 전에 subtree pop 으로 소멸하는 사건의 비율. (ii) 가 높을수록
bounded 변형의 실익이 크다. depth 별로 분리 집계.

### P4. precompute 크기 추정

parserdata (문법) 로부터: n=2, 3 에서 필요한 전이 테이블 — (depth-k group ×
자식 완료 심볼) → (분열 클래스 / 유지) — 의 도달가능 조합 수를 정적으로 열거.
현 tip-group 테이블 크기 대비 배수로 보고. 폭발하면 동적 집합 + 캐시 방향으로
전환 판단.

### 입력 코퍼스

- bibix4 stdlib: jar.bbx (최악), cc.bbx, maven.bbx
- mulang (`../mulang`): examples/ccgen.mu (mulang 쪽 최악 — 별도 조사 진행 중),
  examples/chain_boundaries.mu, mlc/examples 대형 2-3개 (alphafold2.mu 등)
- es5-corpus 샘플 2-3개 (문법 외적 타당성 — mulang 특화 아님을 확인)

### 결과 기록

이 문서 §6 에 표로 기록 (파일 × n × peak/평균 감소율, depth 히스토그램 요약,
window-exit 비율, P4 테이블 크기).

## 4. Phase A/B 스케치 (Phase 0 통과 시 — 참고용, 착수 전 재설계)

- **Phase A (Kotlin 프로토타입)**: n=2 고정. group 표현은 동적 집합 허용 (성능보다
  동작 검증 우선). 게이트 = 기존 golden 불변: runMgroup3Test 130/0 + corner +
  advanced + m2 parity + cargo parser_diff + mulang NativeParserDiffTest ·
  GrammarDiffTest (mulang 쪽 차등 인프라 재사용). 성능은 P1 ceiling 대비 실현율로
  평가.
- **Phase B**: 전이 테이블 precompute (P4 설계), Rust 미러, n 파라미터화 (기본값
  측정으로 결정), kernels history 는 기록 시점에 구체 milestone 로 해소해 m2 좌표
  호환 유지 (현 diff-gate 오라클의 기반이므로 필수).
- 워처 경로에도 동일 적용 검토 — 워처 잔여 fork (타워 ×5 등) 도 같은 구조다.

## 5. 결정 게이트 (Phase 0 → A)

아래는 예시 기준 — Phase 0 수치를 보고 조정:

- P1: 병리 입력 (jar.bbx / ccgen.mu) 에서 **n<=3 으로 peak shapes >=1.5× 감소**
- P3: 병합 집합의 **조기 (window-exit) 분열 비율 < 30%**
- P4: n=2 테이블이 현 tip 테이블의 **~10× 이내**

셋 다 만족 → Phase A 개설. 미달 → 기각 문서화 (watcher_main_sharing.md 전례 —
측정 결과와 재방문 트리거를 남긴다). full interior (n=∞) ceiling 이 크고 bounded 가
작으면 "동적 집합 기반 full interior" 재검토가 재방문 트리거.

## 6. Phase 0 결과 (실행 후 기록)

(미실행)

### 6.0 선행 관찰 — mulang ccgen.mu 비용 조사에서 (2026-07-09, Phase 0 본실행 전)

ccgen.mu (mulang 쪽 최고비용 파일) 가 왜 비싼지 조사하는 과정에서 이 문서의
가설과 직접 관련된 데이터가 먼저 나왔다:

- **스텝 시간은 live shapes 에 선형** — shape-bucket 별 평균 스텝시간이 전 파일
  동일 (0-10: 0.01ms … 200-500: 0.18ms), corr(parse-ms/KB, mean shapes/step) =
  0.997. → shape 감소가 곧 시간 감소 (P1 의 전제 성립).
- **ccgen 의 원인은 peak 가 아니라 sustained baseline**: peak 552 는 오히려
  대조군 (train_llama 700) 보다 낮고, 파스 시간의 65.6% 가 >=50-shape 스텝
  (대조군 39.9%). named-arg 생성자 중첩·트레일링 람다·interpolation 이 파일
  전체에 밀집해 타워 ×5 / 블록 ×2-3 곱셈이 상시 켜져 있는 구조. → interior
  병합은 peak 뿐 아니라 이 sustained 대역 전체를 접는다.
- **fork-depth 표본 (P2 의 예고편)**: ccgen peak gen 의 최대 root (sym1223)
  에서 sample chain 2개를 diff — prefix/suffix 완전 동일, **tip-거리 3 의
  interior 노드 하나만 상이** (sym1231 ↔ sym1233, 우선순위 타워 fork). 즉 이
  사례는 n<=3 bounded window 안. (단 peak 한 지점의 표본 2개 — P2 는 gen-가중
  전수 분포로 재야 확정.)
- 계측 도구: `mgroup3-native/src/bin/shape_integral.rs` (적분/히스토그램 —
  P1 기저 계측으로 재사용), 기존 `profile_steps` (peak/top-N/chain dump).

## 7. 참고

- `kernels_history_optimization.md` — 엔진 트랙 측정 방법론, Phase B 잔여 분석 (fork 목록)
- `watcher_anchor_dedup.md` / `watcher_main_sharing.md` — anchor 정리, ceiling probe 기각 전례
- `mulang_grammar_ambiguity.md` — fork 의 문법적 원천 분석, 문법 트랙 종결 (§11 + mulang 2026-07-09 결정)
- mulang 리포 `parser/test/GrammarDiffTest.kt` — 문법/파서 변경의 언어 동일성 차등 게이트 (dump/compare)
- 선행연구 대응: GLR 의 GSS 노드 공유 / local ambiguity packing — interior group 은
  "packed stack segment" 에 해당. mgroup 의 tip-group precompute 와의 결합이 novelty.
