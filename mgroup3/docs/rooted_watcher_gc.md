# root-traced watcher GC — 타당성 프로브 결과와 판정 (2026-07-10)

상태: **설계 검증 완료, 구현 보류 (재방문 트리거 명시).** PR #6 (eager EOF
resolution, 커밋 08eaf777 머지) 의 일반화 제안 — "워처 path 와 그것을 만들게 한
근원 accept condition 의 수요 관계를 추적하고, 근원이 사라지면 워처를 수거" —
의 probe-first 검증 기록. 프로브: `mgroup3-native/src/bin/rooted_gc_probe.rs`.

## 1. 설계 (제안의 정확한 형태)

**root-traced demand GC**: 현행 step 6 은 "살아있는 아무 path 에서 도달 가능하면
유지"인 reachability GC 라서, 유령 워처의 **자기참조 사이클** (자기 chain 의
observing anchor 가 자기 root 를 재참조 — PR #6 메커니즘 분석) 을 수거하지
못한다. 제안: 수요 추적을 **main path 들에서만 seed** 하고, 워처의 기여
(조건 referencedRoots + chain observing anchor) 는 그 워처 자신이 수요될 때만
전파하는 fixpoint — 자기참조 사이클은 main 수요와 끊어진 순간 사이클째 수거된다.

PR #6 이 기각한 "fork 에 부모 조건 상속"과 다름: 조건 복사가 아니라 수요 집합
추적이므로, 공유 워처의 revival 문제 (첫 질문자 조건 falsify 후 늦은 합법
질문자가 죽은 워처를 못 되살림) 가 발생하지 않는다 — 미래 질문자는 rooted path
의 observing 이 보수적으로 커버하므로 그런 워처는 애초에 수거되지 않는다.

## 2. 프로브 방법과 충실성

- read-only bin 이 무변경 파서를 구동하며 매 gen stratified fixpoint 를 계산,
  `unrooted = live watcher roots − demanded`. 기여 함수는 step 6 의 keep rule
  (core.rs:907-943) 을 의미론 수준에서 미러.
- **자기검증**: seed 를 전체 live paths 로 잡으면 현행 step 6 유지 집합과 정확히
  일치해야 함 — **610,481 gens (전 매트릭스), 0 실패.** 측정 신뢰 근거.
- 반사실 knob `--no-eof-fold`: `eof_cond_symbols` 를 비워 PR #6 의 fold 를 끄고
  병리를 재현 (병리 입력은 최소 재현 4.7KB + 동일 병리 98KB 합성판; PR 의 실측
  336.5 ≈ 338.4 재현으로 등가성 확인).

## 3. 결과

| 코퍼스 | 모드 | mean shapes/step | unrooted (%shapes) | churn (신규/gen) | 초과생존 gen (mean/max) |
|---|---|--:|--:|--:|---|
| 병리 최소 | fold-off | 336.5 | **94.6%** | 1.02 | 4.8 / 3,097 |
| 병리 98KB | fold-off | 347.3 | **94.4%** | 1.00 | 5.0 / **97,897** |
| 병리 (양쪽) | fold-on | 18~20 | 0% | 0 | 0 |
| mulang 정상 4종 | fold-on | 6~46 | **0.01~0.26%** | ≤0.008 | ≤0.77 / ≤2 |
| es5 3종 | fold-on | 9~35 | **0.87~2.84%** | 0.20~0.36 | 0.01~0.02 / 1 |

- **반사실 (병리, fold-off)**: GC 생존 대역 = **18.3~19.6 shapes/step — fold-on
  (18.1~19.6) 과 동일.** 즉 root-traced GC 단독으로도 병리를 완전히 잡았다.
  maxExcess 97,897 gen (한 유령이 98K-gen 파스 전체를 생존) 이 자기참조 맹점의
  결정적 증거.
- **정상 코퍼스의 잔여 수확 ≈ 0**: unrooted 는 shapes 의 ≤2.84% 이고 (대부분
  0.01~1%), 초과생존이 0~2 gen — **어차피 스스로 즉시 죽는 워처들**이라 GC 가
  회수할 실질 메모리/시간이 거의 없다. es5 는 `!.` 구문이 없어 fold 무관인데도
  마찬가지.
- **fixpoint 비용은 무시 가능**: 평균 1.00 반복/step (최대 2), 방문량은 기존
  step-6 워크와 동일.
- **churn 은 GC 로 제거 불가**: fold-off 병리에서 gen 당 ~1 유령 root 탄생 —
  GC 는 사후 수거일 뿐 탄생 (starter 시동 + 1~2 gen 파싱) 은 못 막는다. fold 는
  탄생 자체를 막는다.

## 4. 판정

- **설계는 옳고 완전하다**: 병리 클래스에 대해 eager fold 와 동일한 생존 대역을
  일반 기제로 달성하며, 비용도 사실상 0 (1-iteration fixpoint).
- **그러나 지금 구현할 실익이 없다**: 유일하게 관측된 병리 가족 (anychar-EOF) 은
  fold 가 이미 탄생 차단으로 해결했고 (수거보다 우월 — churn 없음), 정상
  코퍼스의 잔여 수확은 ~0 이다. step 6 은 이 리포에서 가장 예민한 컴포넌트라
  (observing 보수성 계약, replay 상호작용), 현재 이득 0 에 정확성 캠페인 비용을
  지불할 이유가 없다.
- **구현 보류, 재방문 트리거**:
  1. **새 유령 가족의 등장** — anychar 가 아닌 조건 심볼로 같은 패턴 ("1-step
     평가 유예 창에서 Always 시동 + 문법적 합법 + 자기유지") 이 재현되면, fold
     류 특수처리를 늘리는 대신 이 GC 를 구현한다. 그때의 진단 도구가 바로 이
     프로브다 (`rooted_gc_probe <pb> <input>` — unrooted 비율이 높으면 이 가족).
  2. 워처 비중이 높은 새 문법/워크로드에서 unrooted 수확이 유의해지는 경우.
- 구현 시 형태: step 6 의 seed 를 main 으로 좁히고 demanded 워처만 기여를
  전파하는 fixpoint (프로브의 기여 함수 재사용). 출력은 노이즈 제거로 변하므로
  fixture 재생성 + AST-수준 게이트 캠페인 (PR #6 의 플레이북).

## 5. 참고

- PR #6 (`0b186c2c` + 리뷰 보강 `6fea220d`) — 병리 메커니즘 확증과 eager fold.
- mulang `docs/parser_phantom_block_comment.md` — 병리 최초 리포트.
- 프로브 원시 로그: 세션 scratchpad (휘발) — 수치 정본은 본 문서.
