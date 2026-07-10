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

(실행 완료 2026-07-09/10 — §6.1~6.6. 계측 구현·실행은 Opus 에이전트 위임, 리뷰·판정은
메인 세션. §6.0 은 본실행 전 선행 관찰이며 구 문법 기준이라 아래 수치와 직접 비교 불가.)

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

### 6.1 실행 개요

- **프로브**: `mgroup3-native/src/bin/interior_merge.rs` (P1+P2+P3 — 무변경 파서를
  구동하며 live shape 를 재집계하는 read-only bin), `interior_table.rs` (P4 —
  parserdata 정적 분석, 파스 없음). 지시서 §3 은 Kotlin 경로를 지목했으나
  sharing_ceiling.rs 전례대로 Rust bin 으로 구현 — `PathShape.milestone_path` 가
  interior 체인을 온전히 보존해 파서 lib 훅이 불필요했고 (tracked diff 0), 스위트
  리스크 0. 스위트: runMgroup3Test 139/0 (12 skip = env-gated corpus),
  runMgroup3ParserTest 17/17, cargo test (parser_diff golden byte-identical 포함) 그린.
- **parserdata 신선도 (중요)**: 착수 시 fixture pb 가 stale (구 문법) →
  runMgroup3Test 가 현 문법 (2026-07-09 문법 트랙 1a/1b) 으로 재생성. 이것만으로
  jar.bbx base peak 이 1519→444 로 급감 — 문법 트랙 효과의 실측이자, mgroup4 가
  회수할 fork 풀 자체가 이미 줄었다는 뜻. 아래 수치는 전부 현 문법 기준.
- **1차 실행의 결함 3건을 리뷰에서 발견, 수정 후 전체 재실행** (아래는 수정본):
  (i) P3 매칭 키에 tip group 포함 → expand 시 매칭이 깨져 exit 를 pop 으로 오분류,
  (ii) 같은 depth 체류 (persist) 를 pop 으로 계상 → exit 율이 체류 gen 수만큼 희석,
  (iii) shape 키에 root 정체성 미포함 → cross-root 가짜 병합 (→ §6.2 부수 발견).
  (i)(ii) 는 exit 율을 낮추는 (게이트에 유리한) 편향이었다 — 수정 전 P3 는 0~13%,
  수정 후 §6.4 구간으로 반전.
- **재실행**: `cargo build --release --bin interior_merge --bin interior_table` 후
  `interior_merge <parserdata.pb> <input> [--adjusted]` / `interior_table <parserdata.pb> <label>`.
  코퍼스: §3 전체 (mlc = alphafold2/train_llama/flux, es5 = json2/underscore/jquery).
  bin 은 mgroup3-native/src/bin 에 커밋 (shape_integral/sharing_ceiling 전례).

### 6.2 P1 — interior-merge ceiling

병합 정의: shape = shape_integral 단위 ((root, PathShape) 엔트리). depth 1 = tip
group (≡ n=1 = 현행), depth d≥2 = tip 에서 d 번째 체인 노드. 병합 규칙은 k=1 —
같은 root·같은 길이·window 밖 노드 전부 동일·window 안 정확히 한 위치만 상이 —
를 d=2..n 오름차순 greedy 로 적용 (고정 d 의 wildcard-key 파티션은 동치관계,
병합된 shape 는 소비). 키는 무손실급 128-bit 2-lane 해시, O(shapes×window)/gen.

파일 × n (ALL 버킷). r = n=1 대비 감소배율. **mean 이 시간 프록시** (§6.0: 스텝시간
∝ shapes, corr 0.997); peak 은 메모리/최악 스텝 지표.

| 파일 | base peak | peak r @n2/n3/n4/∞ | base mean | mean r @n2/n3/n4/∞ |
|---|--:|---|--:|---|
| jar.bbx | 444 | 1.13 / **1.55** / 1.55 / 1.55 | 59.5 | 1.14 / 1.30 / **1.38** / 1.38 |
| cc.bbx | 639 | 1.10 / 1.48 / 1.53 / 1.99 | 30.5 | 1.11 / 1.26 / 1.34 / 1.36 |
| maven.bbx | 449 | 1.83 / 2.20 / 2.39 / 2.51 | 30.9 | 1.13 / 1.25 / 1.33 / 1.35 |
| ccgen.mu | 552 | 1.13 / **1.45** / 1.91 / 1.91 | 42.8 | 1.12 / 1.22 / **1.29** / 1.29 |
| chain_boundaries.mu | 252 | 1.15 / 1.46 / 1.85 / 1.85 | 54.7 | 1.16 / 1.36 / 1.58 / 1.58 |
| alphafold2.mu | 646 | 1.67 / 1.75 / 1.91 / 2.98 | 26.5 | 1.16 / 1.33 / 1.39 / 1.48 |
| train_llama.mu | 700 | 2.12 / 3.26 / 3.29 / 3.29 | 24.6 | 1.11 / 1.25 / 1.30 / 1.37 |
| flux.mu | 674 | 1.66 / 1.86 / 2.05 / 2.56 | 26.1 | 1.14 / 1.32 / 1.38 / 1.45 |
| json2.js | 1120 | 2.12 / 2.12 / 2.12 / 4.12 | 34.5 | 1.47 / 1.59 / 1.76 / 2.69 |
| underscore.js | 304 | 2.45 / 3.80 / 4.11 / 4.22 | 9.0 | 1.17 / 1.24 / 1.33 / 1.40 |
| jquery.js | 8960 | 2.83 / 2.83 / 2.83 / 6.36 | 22.8 | 1.31 / 1.48 / 1.72 / 2.23 |

관찰:

- **bounded n=4 가 full interior 의 mean 을 사실상 전부 회수한다 (bibix4/mulang)**:
  mean r@n4 vs r@∞ 격차가 전 파일 ≤5% (jar·ccgen·chain·train_llama 는 peak 도
  n4=∞ 일치). §5 의 재방문 트리거 ("full 이 크고 bounded 가 작으면") 는 목표
  워크로드에선 발화하지 않는다. es5 는 예외 — 아래.
- **mean(시간) ceiling 은 peak 보다 훨씬 얇다**: 병리 입력 n=4 에서 jar 1.38× /
  ccgen 1.29×. peak 게이트는 시간 예측 지표로 부적합 (§6.6).
- **main/워처 분리**: bibix4/mulang 은 워처 부하가 지배적 (jar 워처 base mean 47.9
  vs main 11.7) 이나 mean 감소율은 두 버킷 유사 (r@n4 1.25~1.41 대역). es5 는
  워처가 거의 없고 (mean ~1.2) 이득 전체가 main (json2 main mean r@n4 1.81, jquery
  1.80, ∞ 에선 2.4~2.9). mulang 특화 아티팩트 아님 확인.
- **부수 발견 — cross-root 워처 중복** (결함 iii 수정의 효과): root 스코핑이 n=∞
  병합량을 39~54% 깎았다 (jar collapse 147,720→89,967, 상이노드 최대 depth 33→5).
  즉 구계측이 "깊은 interior fork" 로 보이던 것의 대부분은 **서로 다른 (워처) root
  아래 동일 체인 shape 들**이다 — 같은 root 내 병합인 mgroup4 가 회수할 수 없는
  별도 축이고, jar 기준 전체 부하의 ~24% (mean 43.3 vs 언스코프 28.9). 워처 root 간
  dedup/그룹화라는 별도 아이디어의 근거 데이터 (단 root 별 trigger gen / accept
  condition 이 달라 자명하지 않음; watcher_anchor_dedup 계열 후속 검토감).

### 6.3 P2 — fork-depth 분포 (gen 가중, n=∞ 병합량 대비 커버리지)

| 파일 | n≤2 | n≤3 | n≤4 | 최대 depth | 최빈 depth |
|---|--:|--:|--:|--:|---|
| jar.bbx | 45.0% | 85.1% | 99.9% | 5 | d2 (45%) |
| cc.bbx | 37.6% | 77.4% | 96.0% | 15 | d3 (40%) |
| maven.bbx | 44.5% | 77.5% | 95.3% | 21 | d2 (44%) |
| ccgen.mu | 47.1% | 81.3% | 99.1% | 11 | d2 (47%) |
| chain_boundaries.mu | 38.2% | 72.7% | 100.0% | 6 | d2 (38%) |
| alphafold2.mu | 42.2% | 76.3% | 86.1% | 9 | d2 (42%) |
| train_llama.mu | 37.4% | 75.6% | 85.7% | 8 | d3 (38%) |
| flux.mu | 39.0% | 77.3% | 87.9% | 9 | d2 (39%) |
| json2.js | 50.9% | 59.2% | 68.6% | 24 | d2 (51%) |
| underscore.js | 49.8% | 69.0% | 86.6% | 18 | d2 (50%) |
| jquery.js | 42.5% | 58.8% | 76.0% | 30 | d2 (42%) |

§2.3 의 핵심 불확실성이 유리하게 해소됐다: 같은 root 내 fork 는 얕다 (depth 2 최빈,
§6.0 표본 관찰과 일치). bibix4/mulang 은 n≤3 이 73~85%, n≤4 가 86~100% 를 커버.
es5 는 꼬리가 깊어 (최대 depth 18~30, n≤4 커버 69~87%) 타워가 깊은 문법에선 더 큰
n 또는 full interior 가 필요하다.

### 6.4 P3 — window-exit vs pop (per-gen 전이 근사)

휴리스틱 (수정본): 매칭 키 R = root 정체성 + 상이 노드보다 root 쪽 체인 노드들의
prefix 해시 (tip 불포함 — depth d, 길이 L 그룹의 prefix 경계 idx = L−(d−1) 은
성장(L+1,d+1)·유지(L,d)·tip쪽 수축(L−1,d−1) 세 전이 모두에서 동일하므로 불변).
prev gen 그룹 (d,R) 에 대해 cur gen 에 (d+1)∈R → EXIT, (d) 또는 (d−1)∈R →
PERSIST (이벤트 제외), 그 외 → POP. **잔여 한계**: 같은 R 의 다른 fork 가 d+1 에
있으면 persist 가 exit 로 찍히는 false-positive — ambig 카운터 (exit 매칭과 persist
depth 동시 성립) 로 정량화해 [low=(e−a)/(e−a+p) .. high=e/(e+p)] 구간 보고. fork
밀집 구간에선 같은 R 의 다중 depth 배치가 상시라 ambig 가 exit 의 41~99% — 구간이
넓다.

boundary n 별 window-exit 율 [low..high]%:

| 파일 | n=2 | n=3 | n=4 |
|---|---|---|---|
| jar.bbx | [45.3 .. 72.4] | [0.4 .. 73.7] | [0.0 .. 18.2] |
| cc.bbx | [39.8 .. 65.7] | [1.1 .. 76.4] | [3.2 .. 58.9] |
| maven.bbx | [40.1 .. 66.6] | [1.2 .. 73.3] | [4.7 .. 46.1] |
| ccgen.mu | [47.5 .. 71.4] | [0.3 .. 66.6] | [1.7 .. 77.1] |
| chain_boundaries.mu | [16.7 .. 82.1] | [0.0 .. 91.4] | [0.7 .. 0.7] |
| alphafold2.mu | [38.4 .. 57.9] | [11.2 .. 51.4] | [16.4 .. 83.7] |
| train_llama.mu | [44.0 .. 57.0] | [6.9 .. 43.0] | [5.9 .. 81.2] |
| flux.mu | [40.8 .. 58.7] | [9.9 .. 51.7] | [9.6 .. 84.8] |
| json2.js | [12.9 .. 86.5] | [37.1 .. 97.5] | [27.7 .. 89.6] |
| underscore.js | [11.1 .. 69.8] | [34.6 .. 92.2] | [31.1 .. 85.1] |
| jquery.js | [9.7 .. 78.7] | [38.9 .. 95.9] | [35.3 .. 90.9] |

판독:

- **n=2 는 low 기준으로도 40~48% (bibix4/ccgen)** — 게이트 (<30%) 명백 초과. P2
  커버리지 (<50%) 와 정합: fork 는 depth 2 를 자주 벗어난다. n=2 는 탈락.
- n=3/4 는 구간이 임계 30% 를 걸쳐 이 프로브로는 **판별 불가**. 정밀화하려면 shape
  단위 생애 추적 (prefix-hash 로 gen 간 shape 대응) 이 필요 — 필요 시 후속 옵션.
- 단 **게이트로서의 P3 는 P1 로 대체 가능해졌다**: P1(n) 의 per-gen 재집계는 window
  밖 fork 를 이미 비병합으로 계상하므로, P1(n)≈P1(∞) (§6.2) 이면 window-exit 손실은
  이미 ceiling 에 가격 반영된 것. P3 가 남기는 잔여 질문은 hysteresis (실 알고리즘이
  분열 후 재병합하지 않는 손실) 뿐인데, exit 이벤트가 persist 대비 희소해 (jar n=4:
  exit 28 vs persist 3,270) 2차 효과로 판단.

### 6.5 P4 — precompute 크기 (정적, 파스 불요)

| 문법 | 현 tip REDUCE (tip_edge+mid_edge) | interior REDUCE (reachable) | 배수 | (upper bound 배수) |
|---|--:|--:|--:|--:|
| mulang (bibix4/.mu 공용) | 49,943 | 116,017 | **2.32×** | 19.97× |
| es5 | 47,004 | 99,517 | **2.12×** | 6.48× |

reachable = 컴파일된 edge-action 테이블의 (parent, completing-child) 리듀스 키를
interior 후보 group 과 교차한 distinct 쌍 수 (문법 추측 없음, 정확). 한계: 정적
테이블은 n 에 독립 (depth 별 분리 저장 시 ×(n−1); 진짜 depth≤n 도달성은 파스
그래프 필요) — 함의: **n 의 실질 근거는 precompute 폭발 (§2.2 (a)) 이 아니라
런타임 group 표현 비용 (§2.2 (b)) 뿐이다.**

### 6.6 게이트 판정과 권고 (2026-07-10)

§5 원문 기준:

- **P1** (병리 입력 n≤3 peak ≥1.5×): jar 1.55× 충족 / ccgen 1.45× 미달. n≤4 로는
  1.55× / 1.91× 둘 다 충족.
- **P3** (조기 분열 <30%): n=2 초과 확정, n=3/4 판별 불가 — 단 §6.4 판독대로
  P1(n)≈P1(∞) 수렴이 같은 질문에 더 직접적으로 답한다 (게이트 실질 통과로 갈음 가능).
- **P4** (n=2 테이블 ≤10×): 2.32× / 2.12× 충족.

게이트 재조준 (§5 "수치 보고 조정" 조항 행사): peak 은 시간 예측 지표로 부적합하다
(§6.0 의 shapes-시간 선형성이 이번에 재확인됨). P1 게이트는 **mean (gen-가중, =예상
파스시간 비율)** 기준이어야 하고, 그 수치는 병리 입력 n=4 에서 jar 1.38× / ccgen
1.29× 다.

**권고: Phase A 착수 보류 (기각 쪽).** 구조 가설 자체는 전부 검증됐다 — fork 는
얕고 (P2), n=4 bounded 가 full interior ceiling 을 사실상 전부 회수하며 (P1),
precompute 는 무해하다 (P4). 그러나 **절대 이득이 얇다**: 시간 ceiling 1.29~1.38×
(병리 입력) 는 k=1 greedy 상한이며 reduce-분열·group 유지 오버헤드·hysteresis 를
빼면 실측 기대는 ~1.15~1.25× (추정). 2026-07-09 엔진 트랙이 1.87× (parse
1.72→0.92s) 를 회수한 직후라, 남은 파이 대비 알고리즘 복잡도 (interior group
의미론, 지연 분열, parserdata 확장, Rust 미러, m2 좌표 호환) 의 보상이 부족하다.
현 문법 (문법 트랙 1a/1b 이후) 이 fork 풀을 이미 줄인 것도 한 원인 (§6.1).
**착수한다면 n=4** (n=2 무의미 — P2 커버 <50% + P3 low 40%+; n=3 은 병합량
15~27% 를 남김; n≥5 는 체감 없음).

재방문 트리거:

1. **es5 류 (깊은 우선순위 타워) 문법이 1차 워크로드가 되는 경우** — es5 는
   ceiling 이 유의미하게 크다 (jquery mean n4 1.72× / ∞ 2.23×, json2 ∞ 2.69×;
   peak 2.8~6.4×). 이 경우 full interior (동적 집합) 까지 포함해 재평가 — §5 의
   원 재방문 트리거는 es5 축에서만 발화한다.
2. **cross-root 워처 중복 회수** (§6.2 부수 발견, jar 기준 전체 부하의 ~24%) —
   mgroup4 와 독립인 별도 트랙으로 검토 가치.
3. mulang 문법이 fork 를 늘리는 방향으로 재진화하면 재측정 (본 수치는 2026-07-09
   문법 트랙 이후 기준).

### 6.7 판정 갱신 (2026-07-10) — 재방문 트리거 1 발화, Phase A 개설

사용자 확인: **main-path fork 가 많은 문법 (es5 류, mulang 의 chain 패턴) 이 1차
중요 워크로드.** 이 가중에서는 §6.6 의 보류 근거 (mulang 병리 케이스의 얇은
ceiling) 가 결정 변수가 아니게 된다.

depth 히스토그램에서 유도한 정확 mean 곡선 (항등식 merged(n) = base − Σ_{d≤n}
collapse; 측정치 n=2..4 와 교차검증 일치). 주요 파일:

| 파일 | n4 | n5 | n6 | n8 | n10 | ∞ | 병합량 90%/95% 도달 n |
|---|--:|--:|--:|--:|--:|--:|---|
| json2.js | 1.76 | 1.80 | 1.85 | 2.13 | 2.48 | 2.69 | 9 / 11 |
| jquery.js | 1.72 | 1.88 | 1.96 | 2.07 | 2.14 | 2.23 | 7 / 9 |
| chain_boundaries.mu | 1.58 | — | — | — | — | 1.58 (n4 포화) | 4 / 4 |
| (bibix4/mulang 기타) | 1.29~1.39 | — | — | — | — | n4~6 포화 | 4~6 |

main-fork 문법의 ceiling 은 n=6~8 에서 1.9~2.1× 대역 — 실현율 70% 가정에도
~1.7×. chain_boundaries.mu (mulang 의 main-fork 패턴 파수꾼) 도 같은 서명
(ALL 1.58× / main 버킷 1.80× @n4).

**Phase A 형태 (§4 를 다음과 같이 수정 적용)**:

- Kotlin 프로토타입, **n 을 런타임 파라미터로** (n=1 ≡ 현행 비트동일 fast path —
  같은 코드베이스 A/B 는 원설계 §2.2 그대로).
- **main path 우선** — es5 는 워처가 무시 가능 수준 (mean ~1.2 shapes) 이라 이득
  전체가 main. 워처 일반화는 A 검증 후 (mulang 이득의 관건이므로 B 후보).
- 전이 테이블은 depth-독립 union (P4 2.1~2.3×). depth 별 복제 (×(n−1)) 는 n=8
  에서 게이트 (10×) 초과라 배제.
- 성능 기준: jquery/json2 (n=6) + chain_boundaries (n=4) 실측 mean-shape 감소의
  ceiling 실현율. **킬 기준: es5 heavies n=6 실측 <1.4× (실현율 <50%) 면 Phase B
  진입 전 중단·분석.**
- mulang/bibix4 워크로드 단독 관점의 보류 판정 (§6.6) 은 그 자체로 유효 — 이번
  개설은 워크로드 가중 변경에 의한 것임을 명기.

### 6.8 Phase A 완주 결과 (2026-07-10) — 킬 기준 통과

Phase A (A0~A4) 완료. 구현은 별도 모듈 `mgroup4/` (mgroup3 무변경), 상세 기록은
`mgroup4_phase_a_design.md` §7. 요지:

- **정확성**: mgroup4(n=1) ≡ mgroup3 / mgroup4(임의 n) ≡ n=1 — kernelsHistory
  byte-identical (차등 게이트 전부 그린). 의미 보존 실증.
- **시간 (JVM 분리 실측)**: json2 n=6 **2.10×**, jquery n=6 **2.23×** (킬 기준
  1.25× 큰 폭 통과), ccgen 1.86× (mulang 도 시간상 순이익), n=1 회귀 없음.
  shape ratio (1.74~1.78) 를 초과하는 이득은 live-set 축소의 GC/메모리 복리 효과.
- **조정 ceiling 실현율 ~97%** (json2 n6 realized 1.743 vs 조정 ceiling 1.80).
- **Phase B 결정 데이터**: 병합의 90.5% 가 late-convergence → parserdata 사전
  그룹핑이 아니라 **런타임 packing (재파티션 + 캐시 해시)** 이 주 기제. P4 테이블은
  reduce 판정 가속 보조. 잔여 스코프: 워처 일반화, 2중 group, Rust 미러.

### 6.9 Phase B 결과 (2026-07-10) — Rust 채택 기각 (확정)

mgroup4-native (Rust 미러, 커밋 45ffe8bc) 는 정확성 게이트 전승 (fixture 14케이스/
72입력 n∈{1,2,4,6} byte-identical, Kotlin 교차 ratio·카운터 정확 일치) 했으나 시간은
순 회귀: es5 heavies n=6 이 mgroup3-native 대비 **0.63~0.83×** (느려짐), mulang
0.86×, n=1 자체 3~6% 회귀 (struct 비대화). peak-gated packing (`MG4_MERGE_MIN_SHAPES`)
으로도 반전 불가. 동시 작업 오염 의심으로 **유휴 재측정까지 수행해 확정** (11/12셀
±1% 재현). 원인: Kotlin 의 2.1~2.2× 는 live-set 축소의 GC/할당 복리가 원천 —
값-타입 최적화 런타임 (mean live-set 23~43) 에선 per-gen 고정비가 지배해 병합 패스
비용이 이득을 초과한다. **프로덕션 (bibix4/mulang) 은 mgroup3-native 유지.** 상세
분석·재방문 트리거: `mgroup4/docs/phase_b_plan.md` §5. 유지 가치: JVM 엔진 2.1~2.2×
(Kotlin 모듈), 논문 재료 (late-convergence 90.5% + packing 이득의 런타임 의존성).

## 7. 참고

- `kernels_history_optimization.md` — 엔진 트랙 측정 방법론, Phase B 잔여 분석 (fork 목록)
- `watcher_anchor_dedup.md` / `watcher_main_sharing.md` — anchor 정리, ceiling probe 기각 전례
- `mulang_grammar_ambiguity.md` — fork 의 문법적 원천 분석, 문법 트랙 종결 (§11 + mulang 2026-07-09 결정)
- mulang 리포 `parser/test/GrammarDiffTest.kt` — 문법/파서 변경의 언어 동일성 차등 게이트 (dump/compare)
- 선행연구 대응: GLR 의 GSS 노드 공유 / local ambiguity packing — interior group 은
  "packed stack segment" 에 해당. mgroup 의 tip-group precompute 와의 결합이 novelty.
