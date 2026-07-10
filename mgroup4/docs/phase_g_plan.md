# mgroup4 Phase G — generator-native (mgroup→mgroup parserdata + 신규 생성기) 계획

작성: 2026-07-10. 상태: **착수** (사용자 결정 — "이론 상한 1.4×라도 실제 구현으로 확인").
전제: `phase_b_plan.md` §5 (런타임 packing 의 Rust 기각과 그 원인 분석),
본체 doc §6.9. **mgroup3(-native) 무변경 — 작업은 mgroup4/ (+ mgroup4-native/) 에서만.**

## 0. 목표 아키텍처 (사용자 원안의 온전한 형태)

런타임 path 비교·folding·dedup 기계 **전무**. 대신 parserdata 가 결정화를 담는다:

- **상태 = 공유 prefix + "suffix-set" 정적 id.** suffix-set = 길이 ≤ n 의 상관된
  milestone-path-suffix 튜플들의 집합 (window 안 각 위치의 멤버가 열(column)로
  상관됨). **n=1 이면 suffix-set ≡ 현행 milestone group** — 기존 mgroup3 는 이
  일반화의 depth-1 특수형이다.
- fork 지점에서 path 가 갈라지는 대신 suffix-set 전이 하나가 나온다 (튜플이
  늘어남). late-convergence 도 공통 fork 조상에서 시작되므로 태생부터 커버 —
  90.5% 문제는 이 아키텍처엔 해당 없음 (그 통계의 함의는 "상관 상태가 여러 gen
  을 산다" = 상태 공간 크기의 동인).
- window 안(內) reduce 는 suffix-set 전이에 흡수 (사전열거), window 경계를 넘는
  reduce 만 (prefix 노드 템플릿 × suffix-set) 키의 edge 테이블. window-exit
  분열도 사전열거된 분열 전이.
- 조건: 멤버별 accept-condition 템플릿이 상태 정체성에 들어감 (다르면 상태가
  갈라짐 — 런타임 packing 의 "condition 동일 병합" 제약과 동형).
- 보고: kernels history byte-parity 를 위해 상태에 멤버별 보고 메타데이터 보존,
  기록 시점 멤버 확장 (Phase A 검증 전략 재사용).

## 1. 예측과 판정 기준 (착수 시점)

- 시간 예측 (es5 heavies, Rust): speedup = 1/(1−L·(1−1/R)).
  **Probe B 결과 (2026-07-10, per-gen 회귀)**: L_회귀 = json2 0.56 / jquery 0.48 /
  ccgen 0.56 / jar 0.74 — 종전 추정 (0.29~0.35, Phase B A/B 차감) 보다 크게 높음.
  **두 측정의 모순 해소**: L_회귀는 "shape 에 상관된 시간의 총량" = 접기의 이론
  상한이고, 그중 일부 (특히 kernels-history byte-parity 를 위한 멤버별 확장 기록)
  는 병합해도 사라지지 않는다. Phase B runtime packing 의 실효 L≈0.29 는 그 비용
  + packing 특유 오염 (struct 비대·분열 재구성) 을 뺀 값. **generator-native 의
  실효 L 은 0.29 (하한, runtime packing 실측) ~ 0.48-0.56 (상한, 회귀) 사이** —
  중간값 가정 시 es5 heavies 예측: n=4 **1.2~1.3×**, n=8 **1.3~1.4×**, ∞ 1.3~1.5×.
  mulang 은 L 이 높아도 (jar 0.74) R 이 condition 제약으로 1.2~1.4 에 묶여 1.1~1.2×.
  이 예측을 실측으로 검증/반증하는 것이 Phase G 의 목적이며, **G4 (Kotlin 실측)
  에서 실효 L 을 직접 확정**한다 (보고-확장 비용의 실측 분리 포함).
- **킬 게이트 1 (G0)**: n=2 에서 suffix-set 상태 수 또는 테이블 크기가 현행 대비
  폭발 (기준: 상태 ~10×, 테이블 엔트리 ~10× 초과) 하면 중단·보고.
- **킬 게이트 2 (G4)**: Kotlin end-to-end 에서 realized R 이 ceiling 을 크게
  밑돌거나 (실현율 <70%), L-예측 기준 Rust 기대가 <1.1× 로 떨어지면 G5 (Rust)
  진입 전 중단·보고.

## 2. 단계 계획

| 단계 | 내용 | 게이트 | 모델 |
|---|---|---|---|
| **G0** | 정적 타당성 프로브: suffix-set closure 시뮬레이션 (생성기 미개발) — 문법 {asdl, mulang, es5} × n∈{2,3,4} 의 도달가능 상태 수·전이 수·조건-템플릿 분화 영향. + **Probe B** (병행): Rust 엔진 per-gen 시간 vs live shapes 회귀로 L 정밀화 | 킬 게이트 1 | G0: Opus / Probe B: Sonnet |
| **G1** | 설계 문서: proto 스키마 (mgroup4 전용 신규 — mgroup3 schema 무변경), 생성기 아키텍처 (mgroup3.gen 을 읽기 전용 lib 의존으로 재사용 가능 범위 조사), 런타임 설계, 보고 parity 전략, 단계 분할 | 메인 세션 설계 리뷰 | Opus (리뷰: Fable) |
| **G2** | 생성기 (Kotlin, mgroup4/gen): n 파라미터 suffix-set closure + 신규 parserdata 방출. 소형 문법(구조 12종+asdl)부터 | 생성 성공 + G3 준비 | Opus |
| **G3** | 런타임 (Kotlin, mgroup4/parser 에 신규 파서 클래스): 신규 parserdata 소비. **게이트 = kernels history byte-diff vs mgroup3** (기존 차등 하니스 재사용), 전 코퍼스 | byte-identical | Opus |
| **G4** | 측정 (Kotlin): realized R, JVM 시간, es5/mulang 대형 코퍼스 | 킬 게이트 2 | Sonnet~Opus |
| **G5** | Rust 런타임 (mgroup4-native 확장) + 시간 실측 — **G4 통과 시에만** | 최종 시간 판정 (es5 heavies, 예측 대비) | Opus |

각 단계 종료마다 메인 세션 리뷰 + 커밋. 난이도가 리뷰에서 감당 불가로 판정되는
지점 (예: G2 생성기의 closure 의미론 버그) 은 메인 세션 직접 개입 또는 Fable
에이전트 (사용자 승인된 예외).

## 3. 리스크

1. **상태 폭발 (G0 가 정면 측정)** — 상관 상태가 여러 gen 을 살아야 한다는 실측
   (late-convergence 90.5%) 이 위험 신호. n 과 조건-템플릿 분화가 증폭 요인.
2. **생성기·보고 의미론의 난도** — 현행 생성기의 m2-parity 보고 레이어 (added
   채널, barrier 시뮬레이션, 리맵) 는 이 리포에서 가장 까다로웠던 컴포넌트.
   suffix-set 으로 일반화하면서 이 parity 를 유지하는 것이 G2/G3 의 본질적 위험.
3. **이득 상한이 얇음** — 전부 성공해도 Rust 예측 1.1~1.3×. 이 트랙은 성능
   도박이 아니라 "실측으로 예측을 검증"하는 성격임을 명시 (논문 재료 가치 포함).

## 4. 산출물 위치

- 생성기: `mgroup4/gen/`, 스키마: `mgroup4/schema/`, 런타임: `mgroup4/parser/`
  (기존 런타임-packing 파서와 공존 — 별도 클래스), 프로브: `mgroup4-native/src/bin/`.
- 문서: 본 파일 + G1 설계 문서 (`phase_g_design.md`).

## 5. G0 결과 — ⚠️ v1 판정 철회됨 (아래 §5.v1 은 기록 보존, 정정 결과는 §6)

**v1 측정 (§5.1~5.3) 은 상태 정체성 결함으로 무효**: 캐노니컬라이즈가 gen 상대
오프셋 **원값**을 포함해, 긴 스캔 중 같은 구조적 설정이 매 gen 새 상태로 집계됐다
(전체 집계의 94~98% 가 이 아티팩트 — §6.3). 사용자 리뷰 ("유한 문법이 발산할 수
있나?") 가 결함을 지적, 정체성을 정정해 재측정한 결과 **킬 게이트 1 은 발화하지
않는다** (§6). 아래 v1 기록은 방법론 교훈으로 보존.

### 5.v1 (무효 — 기록 보존) 원 판정: 킬 게이트 1 발화, Phase G 종료

### 5.1 측정 (동적 하한 — 실코퍼스에서 관찰된 distinct suffix-set 상태 수)

프로브: `G0SuffixSetStats.kt` + `Mgroup4Parser` opt-in 훅 (`MG4_G0_STATS`) +
`runMgroup4G0Probe` 액션. gen 값은 상대 오프셋으로 정규화, group 은 멤버로 펼쳐
관찰, 조건은 구조 클래스로 근사. **방법론 검증 = n=1 대조군**: suffix = tipGroupId
만이면 (≡ 현행 milestone group) jquery 293k자에서 **790개 상태로 포화** (현행
group 1,824 의 0.43×) — gen 정규화가 올바르고, 폭발은 전적으로 window 확장에서
온다는 것을 격리.

| 대상 | n=2 (÷현행 group) | n=3 | n=4 | 포화? |
|---|--:|--:|--:|---|
| asdl | 135 (1.6×) | 1.5× | 1.3× | 포화 |
| chain_boundaries.mu | 6,775 (2.7×) | 5.5× | 6.2× | — |
| ccgen.mu | 40,371 (**15.9×**) | 34.8× | 42.1× | 발산 |
| json2.js | 39,156 (**21.5×**) | 30.0× | 29.1× | 발산 |
| jquery.js | 169,463 (**92.9×**) | **318×** | **459×** | **발산** (262k gen 에도 gen 당 ~0.5 신규) |

조건 클래스 포함 시 더 큼 (jquery n=2 129×). n=1→2 한 칸에 jquery 790→169,463
(**214배**) — "직전 milestone 이 무엇이었나"가 상태를 가르는 순간 정적 열거가
무너진다. late-convergence 90.5% (상관 상태가 여러 gen 생존) 와 동형의 현상.

### 5.2 판정

- **킬 게이트 1 (n=2 에서 ~10×) 을 실문법 전부가 9~46× 초과** — 그것도 *하한*이
  발산 중. 정적 열거는 "비용이 큰" 문제가 아니라 **유한하게 닫히지 않는** 문제다
  (도달가능 상태가 입력 의존적으로 계속 늘어남).
- **lazy 물질화 하이브리드 (상태를 만나면 생성·캐시) 도 구제 불가**: jquery n=2
  에서 gen 2개당 신규 상태 ~1 — 재사용율이 낮아 상태 구성 비용이 상각되지 않고,
  이는 곧 런타임 packing 비용의 재현이다 (Phase B 에서 이미 기각된 경제성).
- **Phase G 종료.** G1 (설계) 이후 미착수. 프로브 코드·수치는 재현 가능하게 커밋.

### 5.3 (무효) v1 의 "종합 통찰" — §6 에서 정정됨

(v1 은 "n=1 이 정적 열거의 경계, n≥2 발산"으로 결론냈으나 이는 정체성 아티팩트.
정정된 결론은 §6.4.)

## 6. G0 재측정 (v2, gen-free 정체성) — **킬 게이트 통과, Phase G 재개**

### 6.1 정정된 상태 정체성

gen 은 정적 상태가 아니라 **런타임 바인딩**이다 (현행 parserdata 가 이미 그런
팩터링 — 테이블 키는 템플릿/group id 뿐, gen 은 path 가 든다). 변형 A (주) =
튜플 (window 노드들의 (symbolId, pointer) + tipGroupId) 의 정렬 집합, gen·중복도
완전 제외. 변형 B (참고) = A + gen 동등/순서 패턴 (원값 아님).

### 6.2 결과 (동적 하한, 현행 group 대비 배수)

| 대상 | n=2 (noCond/withCond) | n=3 | n=4 | n=1 대조 |
|---|---|---|---|---|
| asdl | 0.81× / 0.84× | 0.84× | 0.84× | — |
| chain_boundaries | 0.16× / 0.24× | 0.26× | 0.34× | 0.09× |
| ccgen | 0.53× / 0.83× | 0.95× / 1.37× | 1.48× / 2.06× | 0.27× |
| json2 | 0.48× / 0.57× | 0.81× | 1.10× / 1.24× | 0.22× |
| jquery | **1.67× / 2.10×** | 4.84× / 5.92× | **9.59× / 11.36×** | 0.43× |

- **킬 게이트 1 (n=2 에서 ~10×): 통과** — 최대 2.10× (jquery withCond). n=3 도
  ≤5.9×. n=4 는 es5 에서 경계 (9.6~11.4×).
- **포화**: v1 의 발산 꼬리 (~0.5 신규/gen) 는 사라짐 — jquery n=2 꼬리
  0.0016/gen, **자명하게 유한한 n=1 대조군과 같은 로그형 곡선**. 입력 15.5× 에
  상태 3.5× (강한 sublinear).
- **변형 B ≈ A (차이 ≤2 상태)**: gen 패턴은 상태를 사실상 가르지 않는다 —
  "gen 은 순수 런타임 바인딩" 팩터링의 실측 검증. 조건 클래스는 +20~40%.
- 아티팩트 기여: v1 집계의 94~98% (n≥2 에서 15~66× 팽창).

### 6.3 경제성의 재계산 — lazy 물질화 부활

v1 의 "상태 재사용 ~2 gen → 상각 불가"도 아티팩트였다. 정정: jquery n=2 는
293k gen 에 3,047 상태 = **상태당 평균 ~96 gen 재사용**. 즉 상태·전이를 만나는
시점에 구성해 캐시하는 **lazy 물질화 (regex lazy-DFA 방식)** 가 잘 상각된다 —
전체 정적 생성기 없이도, 워밍업 후에는 generator-native 와 같은 순수 테이블
조회 런타임이 된다. Phase B 를 죽인 per-gen 병합 패스 비용이 "상태당 1회" 로
바뀌는 구조라 **Rust 경제성 재도전의 유력 경로**.

### 6.4 재판정과 G1 입력 (이후 §7 에서 최종 종결)

- **Phase G 재개 — G1 (설계) 진행.** G1 은 두 아키텍처를 비교 설계한다:
  (a) 전체 정적 생성기 (원안), (b) **lazy 캐시 구성** (신규 유력 — 생성기/proto
  작업 없이 런타임에서 상태 JIT, Rust 직행 가능).
- G1 설계 입력 (G0 실측): 상태 정체성 = kernel 튜플 집합 (+조건 템플릿 클래스),
  gen 은 전부 런타임 행; n=2~3 이 상태 비용 스위트 스폿, n=4 는 es5 에서 경계;
  동적 하한이므로 정적 closure 는 이보다 큼 (lazy 는 동적 도달분만 만들므로
  이 하한이 곧 실비용).
- 시간 이득 기대는 §1 그대로 (es5 n=2~4 에서 1.1~1.3×, 실효 L 에 따라) — lazy
  경로는 packing 오버헤드를 제거하므로 Phase B 회귀의 주범이 사라진 상태에서
  이 기대치를 검증하게 된다.

## 7. Phase G 최종 결과 (2026-07-10, G-b0~G-b5 완주) — **Rust 채택 기각, 트랙 완결**

### 7.1 구현·게이트 (전승)

lazy 물질화가 Kotlin (G-b0~b4.5) 과 Rust (G-b5, mgroup4-native 재배선) 양쪽에
완성됐다. 게이트: parser_diff 8/8 셀 (n∈{1,2,4,6}×캐시 on/off) byte-identical,
Kotlin 교차 (realized R 소수 3자리 일치), MG4_LAZY_VERIFY 병렬 대조 (600k+ gen),
corpus fingerprint 16/16. **병렬 검증이 실버그를 잡음** (버킷 prefix 를 Rc 포인터
동일성으로 잡아 under-merge — 구조 동등으로 수정): 오라클 설계가 작동한 증거.

### 7.2 시간 (Kotlin vs Rust — 대조적 결말)

- **Kotlin (G-b4→b4.5)**: 캐시 as-built 는 packing 대비 1.81× 느림 (히트 비용의
  88% = 시그니처 재계산) → **인터닝으로 114× 절감, 역전** (웜 ~1040ms vs packing
  ~1090ms, ~1.05× 우위). JVM 에선 lazy ≈ packing ≈ n=1 대비 2.2×.
- **Rust (G-b5)**: 인터닝 포함 완전 이식에도 **전 셀이 mgroup3-native 보다 느림** —
  es5 웜 n=4~6 = 0.64~0.84×, mulang 0.82~0.86×, n=1 도 2~4% 회귀 (Phase B struct
  비대 지속). warm vs packing 은 0.97~1.08× (코퍼스 의존, jquery 는 손해) — Kotlin
  의 인터닝 승리가 Rust 에선 재현 안 됨 (Phase B 최적화로 재파티션이 이미 싸서).
  병합 자기시간 43.9→34.6% 로 줄여도 **n≥2 그룹 기계 자체가 순손실**: realized R
  1.45~1.74 의 절감이 그룹 기계 비용 (구조 비대·분열 재구성·per-shape 상수 증가)
  을 넘지 못한다. §1 의 L-예측 (1.13~1.27×) 은 "그룹 기계 비용 0" 가정이 깨져
  미실현 — 실효 L 은 기계 비용 차감 후 ~0.2 미만에 해당.

### 7.3 최종 판정과 트랙 완결

- **mgroup4-native (lazy 포함) 프로덕션 채택 기각. 프로덕션 = mgroup3-native 유지.**
- mgroup4 트랙의 설계 공간이 **전부 측정으로 닫혔다**: ① 런타임 packing — JVM
  2.19× / Rust 회귀, ② 정적 결정화 — 상태 공간은 유한·소규모 (v2) 이나 정적 생성
  불필요 판정 후 ③ lazy 물질화로 구현 — JVM 에서 packing 동급, Rust 에서 여전히
  순손실. **결론: interior grouping 의 이득은 런타임 특성 (할당-지배 여부) 의
  함수이고, 값-타입 최적화 엔진에서는 이 R 대역 (1.4~1.8) 으로는 어떤 아키텍처로도
  못 갚는다.**
- 유지 가치: JVM 파서 소비자가 생기면 Kotlin 모듈 (packing/lazy) 즉시 유효 (2.2×);
  논문 재료 (3-아키텍처 측정 완결 서사 + enumerability 정정 + 런타임 의존성);
  차등 오라클·벤치 인프라.
- **재방문 트리거**: ① 값-타입 엔진의 per-shape 비용이 커지는 변화 (조건 평가
  고비용화 등), ② R 이 훨씬 큰 문법/워크로드 (live-set 수백 대역), ③ JVM 배포
  경로 등장. + 방법론 교훈 3건: 정적 상태 정체성에 gen 금지 (G0 v1), 단일-JVM
  interleave 벤치 금지 (Phase A/G-b4), 유휴 확인 (Phase B).
