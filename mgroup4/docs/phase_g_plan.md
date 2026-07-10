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

## 5. G0 결과 (2026-07-10) — **킬 게이트 1 발화, Phase G 종료 (G1+ 미착수)**

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

### 5.3 종합 통찰 (mgroup4 트랙 전체의 결론)

**n=1 (현행 tip group) 은 정적 열거 가능한 grouping 깊이의 실측 경계다** —
jquery 에서 n=1 은 790 상태로 포화, n=2 는 169k+ 로 발산. 즉 mgroup3 의 설계점
(tip 만 group + interior singleton) 은 우연이 아니라 "precompute 가 성립하는
최대 깊이"이고, 그보다 깊은 interior 공유는 본질적으로 런타임 packing 이며
(Phase A/B), 그 packing 은 할당-지배 런타임 (JVM, 2.1~2.2×) 에서만 이득이고
값-타입 최적화 런타임 (Rust) 에선 회귀다. 세 아키텍처 (런타임 packing / 정적
결정화 / lazy 하이브리드) 가 모두 측정으로 닫혔다.
