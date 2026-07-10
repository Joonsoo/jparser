# mgroup4 Phase B — Rust 미러와 프로덕션 채택 준비 (계획)

작성: 2026-07-10. 상태: **계획 확정, 착수** (Phase A 완주 직후). 전제 문서:
`mgroup3/docs/mgroup4_phase_a_design.md` (§7 = Phase A 실행 결과),
`mgroup3/docs/mgroup4_bounded_interior_groups.md` (§6 = Phase 0 계측 + §6.8 Phase A 요약).

## 0. 현재 구현 상태 (Phase A 완료 시점 스냅샷)

- **모듈**: `mgroup4/` (Kotlin) — mgroup3 parser 패키지 6파일 포크 (`Mgroup4Parser`,
  `ParsingCtx`, `AcceptCondition`, `RecordConditionEvaluator`, `Mgroup3ParserDataPlain`,
  `ParsingData`; AcceptCondition↔PathRoot 결합 클러스터라 통째 포크). proto/generator/
  parserdata 는 mgroup3 공유 (같은 .pb 로드). **mgroup3·mgroup3-native 는 무변경**
  (타 프로젝트 참조 보호 — 사용자 제약).
- **동작**: n 런타임 파라미터 (`MG4_INTERIOR_N`, n=1 ≡ mgroup3 비트동일 fast path).
  main root 한정. per-gen 런타임 packing (재파티션: prefix/suffix 메모이즈 해시로
  O(window)/shape), reduce 도달 시 멤버별 분열, window-exit 지연 분열, 보고(app)는
  기록-시-확장 (실측상 no-op — fold 시 보고 좌표가 멤버 불변).
- **게이트** (`runMgroup4DiffTest`, 전부 그린): G1 = mgroup4(n=1) ≡ mgroup3
  kernelsHistory·accept byte-identical (구조 문법 12종 + asdl 4입력 + chain_boundaries
  + json2), G2 = mgroup4(n∈{2,4,6}) ≡ mgroup4(n=1).
- **실측** (`Mgroup4Bench`, JVM 분리·8g·중앙값): json2 n=6 **2.10×** / jquery n=6
  **2.23×** / ccgen n=6 1.86× / chain n=4 1.10× (n=1 회귀 없음). realized shape ratio
  1.74~1.78 = 조정 ceiling 의 ~97%. 시간이 shape 를 초과하는 건 live-set 축소의
  GC/메모리 복리 (입력 클수록 확대).
- **미커밋**: `mgroup4/` 전체 + `build.bbx4`(additive) + docs + Phase 0 probe bin 2개
  (`mgroup3-native/src/bin/interior_merge.rs`/`interior_table.rs`, do-not-land 헤더).

## 1. 왜 런타임 packing 인가 — late-convergence 90.5% 해설

병합의 성립 조건: 두 live path 가 **정확히 한 interior 노드만 다르고** 나머지 전부
(길이·tip group·condition) 동일. 이 상태에 도달하는 경로가 두 가지다.

**(a) creation-mergeable (9.5%)** — fork 순간 이미 병합 가능. 한 shape 의 term action
에서 rea 두 개가 매치되는데 **append 대상 tip 이 같은** 경우:

```
gen g:  [prefix] ─m_a─ tip T     ┐  같은 T — 갈라진 노드(m_a/m_b)만 다름
        [prefix] ─m_b─ tip T     ┘  → 태어나자마자 병합 가능
```

이 부류만 문법(parserdata)에서 정적으로 예측·사전 그룹핑 가능하다.

**(b) late-convergence (90.5%)** — fork 순간엔 병합 불가 (새 노드도, tip 도 다름).
이후 두 path 가 **같은 입력 문자를 나란히 스캔**하다 tip 이 같은 group 으로 수렴하면,
그제서야 "묻힌 fork 노드 하나만 다른" 상태가 된다:

```
gen g:   [prefix] ─m_a─ tip T_a   ┐  T_a ≠ T_b — 두 군데 다름, 병합 불가
         [prefix] ─m_b─ tip T_b   ┘
  ... 둘 다 같은 문자들을 스캔 (예: es5 우선순위 타워 fork 들이 같은 식별자를 읽음) ...
gen g+k: [prefix] ─m_a─ u₁..uⱼ ─ tip U  ┐  tip 과 새 노드들이 수렴 —
         [prefix] ─m_b─ u₁..uⱼ ─ tip U  ┘  m_a/m_b (이제 depth j+2) 만 다름 → 병합
```

어떤 형제 쌍이 언제 수렴하는지는 **입력에 의존**하므로 문법만으로 열거 불가. 따라서
parserdata 사전 그룹핑(mgroup→mgroup edge 테이블)은 병합의 ~10% 만 잡고, 나머지는
**런타임에서 수렴을 감지해 접는 packing** 이어야 한다 — GLR 이 병합을 컴파일 타임이
아니라 런타임 GSS 의 local ambiguity packing 으로 하는 것과 같은 이유. 이 발견이
Phase B 의 중심을 "전이 테이블 사전 열거 (P4)" 에서 "런타임 packing 의 Rust 이식 +
상수 최적화" 로 옮겼다. (측정 정의: group 형성 시 상이 노드의 생성 gen 이 현재 gen±1
이면 (a), 그보다 오래면 (b). json2 n=6: 16,838 vs 160,903 collapse.)

## 2. Phase B 범위

**목표: "Rust 에서 같은 의미·같은 게이트로 재현하고, 프로덕션(FFI)에서 opt-in 으로
켤 수 있는 상태".**

### B1 — Rust 미러: `mgroup4-native` crate (신설, mgroup3-native 무변경)

- 포크 경계는 Kotlin 과 동형 예상: parsing_ctx / core(파서) / accept_condition /
  parser_data(plain 변환 + rkyv 캐시) — 상호 결합 클러스터. proto 는 mgroup3-native
  의 build.rs 패턴 재사용 (`../mgroup3/schema/proto` 에서 생성). 기존 엔진 최적화
  (rkyv parserdata 캐시, hist dedup, step6 visited-set, mimalloc, KernelSet 융합,
  fat-LTO release 프로필) 전부 유지한 위에 얹는다.
- 이식 대상: group 표현 (groupMembers + 멤버별 보고좌표, equals/hash 계약 — 보고
  좌표류 제외), per-gen packing (재파티션 + pre/suf 캐시 해시 — `interior_merge.rs`
  probe 의 패턴이 원형), reduce 멤버별 분열, window-exit, 기록-시-확장, n 파라미터
  (env `MG4_INTERIOR_N` 동일).
- **증분 버킷 유지는 하지 않는다** (Phase A 판정: 캐시 해시 후 병합 패스가 파스의
  13% 이하 — 복잡도 값어치 없음).

### B2 — 게이트 (전부 기존 오라클 재사용)

1. **mgroup4-native(n=1) ≡ mgroup3-native**: 기존 parser_diff fixture 전부 (Kotlin
   golden byte-identical — 출력 불변이므로 golden 재생성 없이 그대로).
2. **n∈{2,4,6} 도 같은 golden**: 출력 불변이 정체성이므로 동일 fixture 로 n 만
   바꿔 재실행.
3. **Kotlin mgroup4 와 교차**: realized ratio (json2 n6 1.743, n4 1.682, chain n4
   1.444) 와 카운터가 일치해야 함 — 두 구현의 독립 검증.
4. **11파일 corpus fingerprint**: n=1 vs mgroup3-native 일치.

### B3 — 실측 + n 기본값 + 채택 기준

- corpus 시간 (Rust, 기존 time_parse 패턴): es5 heavies + mulang/bibix4 회귀.
- **기대치 보정**: Rust 는 JVM GC 복리가 빠져 compute-순 이득 ≈ shape ratio
  (es5 heavies ~1.7×+) 로 수렴 전망. **채택 기준: es5 heavies n=6 ≥1.5×, mulang
  n=4 시간 비회귀.**
- n 기본값 확정 (Phase A 데이터: es5류 6~8, mulang류 4 — 문법별).

### B4 — P4 테이블 (조건부)

reduce 도달 시 멤버별 판정의 가속용. Phase A 실측 (fold/explode <1%) 상 불필요
전망 — **Rust 프로파일이 요구할 때만** 착수.

### B5 — FFI opt-in 배선

`mgroup4_parser_*` 심볼 + (선택) Kotlin bridge. bibix4/mulang 로더가 mgroup4-native
를 선택할 수 있게. parserdata 포맷 동일이라 배선은 로더 스왑 수준. **기본값 전환은
범위 밖** (Phase C).

### 범위 밖 (명시)

generator/parserdata 포맷 변경 (없음), mgroup3(-native) 수정 (없음), 워처 경로
일반화 (Phase C — mulang 프로덕션 이득의 관건), 2중 group 체인 (Phase C).

## 3. Phase C 백로그 (착수 별도 결정)

1. **워처 일반화** — bibix4/mulang 부하는 워처 지배적 (jar 워처 mean 47.9 vs main
   11.7) 이라 프로덕션 빌드 시간의 실이득 대부분이 여기. Kotlin 프로토타입 (게이트
   재사용) → Rust 순서.
2. **2중 group 체인** — json2 n6 에서 skipExistingGroup 62.8k건. 프로브로 여지 크기
   먼저 측정.
3. **기본값 on + mgroup3 대체 결정** — 병행 유지 기간 포함.
4. **(독립 트랙) cross-root 워처 중복 회수** — Phase 0 부수 발견 (jar 부하 ~24%).
   mgroup4 와 무관한 별도 아이디어.
5. **(선택) 논문 반영** — tip-precompute + GSS식 interior packing 하이브리드,
   late-convergence 90% 가 "순수 precompute 불가" 논거.

## 4. 진행 계획

B1a (crate 스캐폴드 + n=1 parity — fixture 전부) → B1b (group 기계 + n>1 게이트 +
Kotlin 교차) → B3 (실측·기본값) → 보고. B5 는 B3 통과 후. 각 단계 게이트 그린으로
종료, 메인 세션 리뷰.

## 5. Phase B 실행 결과 (2026-07-10) — **Rust 채택 기각**

### 5.1 정확성 (B1a/B1b): 완승

`mgroup4-native` crate (커밋 45ffe8bc, mgroup3-native 통째 포크 + group 기계 이식,
rkyv magic 분리 `MG4RKYV2`, FFI `mgroup4_parser_*`). parser_diff 14케이스/72입력이
n∈{1,2,4,6} 전부 Kotlin golden byte-identical, 11파일 corpus fingerprint 가
mgroup3-native 와 일치, **Kotlin 교차 검증 정확 일치** (json2 n6 ratio 1.743 / n4
1.682 / chain n4 1.444, creation/late-convergence 카운터 16,838/160,903 동일).
두 독립 구현의 완전 상호 검증.

### 5.2 시간 (B3): 채택 기준 대폭 미달 — 순 회귀 (유휴 재측정으로 확정)

1차 측정이 동시 작업 (mulang 테스트 워처) 과 겹쳐 오염 의심 → **유휴 상태 재측정**
(경쟁 프로세스 부재 확인, 셀당 9~12회, 2라운드 인터리브; sd 0.1~0.7%, 라운드 드리프트
±0.7%). 12셀 중 11셀이 1차와 ±1% 재현 — 유일한 보정은 jquery n=6 (5640→5182ms,
1차가 +8% 부풀려짐). **방향 불변.** 아래는 재측정 median:

| 파일 | m3 n=1 | m4 n=1 | m4 n=6 | n=6 vs m3 / vs m4 n=1 |
|---|--:|--:|--:|--:|
| json2.js | 267 | 284 (+6%) | 427 | **0.63× / 0.67×** (느려짐) |
| jquery.js | 4279 | 4412 (+3%) | 5182 | **0.83× / 0.85×** |
| ccgen.mu | 1012 | 1050 (+4%) | 1179 | 0.86× / 0.89× |

(json2 는 n=4/6/8 전부 0.62~0.64× 대역, jquery 는 n=4 0.72× / n=8 0.88×.) 채택 기준
(es5 heavies n=6 ≥1.5×, mulang 비회귀) 전부 미달. n=1 자체도 노드 struct 비대화
(group 2필드 + OnceCell 캐시 3개) 로 3~6% 회귀.

**원인 (Phase A §7.3 예측의 정정)**: Kotlin 의 2.1~2.2× 는 대부분 live-set 축소의
**GC/할당 복리**였다. 최적화된 Rust 엔진 (mimalloc·scratch 재사용·step6 visited-set)
에는 그 복리가 없고, mean live-set 이 23~43 shape 로 작아 per-gen 고정비가 지배 —
shape 1.74× 축소의 절감폭이 작은 반면 병합 패스 자기시간 (25~42%, Kotlin 은 3.9~14%)
은 그대로 남는다. "Rust 는 shape ratio 로 수렴" 예측은 오판.

### 5.3 peak-gated packing 실험 (판정 확정)

`MG4_MERGE_MIN_SHAPES` (live main shapes ≥ 임계값인 gen 만 병합 형성; 기본 0 = 무게이트,
출력 불변 — parser_diff 재확인): 임계값을 올릴수록 회귀가 단조 회복되나 **es5 n=6 에서
n=1 을 끝내 못 넘는다** (유휴 재측정: json2 n6@256 = 337ms → m4 n=1 대비 0.84×,
jquery n6@256 = 4609ms → 0.96×). 유일한 순이익은 jquery n=8@128 = 4325ms — m4 n=1
대비 1.02× (2% 빠름) 이나 **m3 대비는 0.99× 로 여전히 미달**이고, ratio 를 1.85→1.39
로 깎은 대가라 무의미. 임계값은 병합 비용과 병합 이득을 거의 1:1 로 맞바꾼다 →
**회귀는 저부하 gen 낭비가 아니라 병합 패스의 per-gen 비용 구조에 본질적.**

### 5.4 판정과 잔여 가치

- **mgroup4-native 프로덕션 채택 기각.** bibix4/mulang 은 mgroup3-native 유지.
  B5 (FFI 배선) 중단, Phase C 의 Rust 트랙 중단.
- 유지되는 가치: (i) **JVM 엔진에선 실증된 2.1~2.2×** (mgroup4 Kotlin 모듈 — JVM
  파서를 쓰는 소비자가 생기면 유효), (ii) **논문 재료** — 알고리즘 + late-convergence
  90.5% + "interior packing 은 할당-지배 런타임에서 이득, 값-타입 최적화 런타임에선
  역효과"라는 런타임 의존성 결과 자체, (iii) 차등 게이트·계측 인프라와 mgroup4-native
  (재현 가능한 부정적 결과의 측정 자산).
- **재방문 트리거**: ① Rust 엔진의 per-shape 비용이 커지는 변화 (예: 조건 평가
  고비용화), ② mean live-set 이 수백 shape 대역인 문법/워크로드 등장, ③ 재파티션을
  대체하는 저비용 병합 감지 아이디어 (예: fork-시점 형제 집합 추적을 gen 간 유지).
