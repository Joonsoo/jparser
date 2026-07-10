# mgroup3 incremental parsing — 수렴 프로브 결과와 트랙 계획

작성: 2026-07-10. 상태: **프로브 완료, Phase I/II 설계 진행.** 소비자: mulang 리포의
LSP (현재 bibix4 빌드파일 전용) — 키스트로크 편집마다 재파스.
프로브: `mgroup3-native/src/bin/incremental_probe.rs` (read-only, lib 무변경).

## 0. 아이디어

- mgroup3 는 엄격한 좌→우 단일 패스 + gen-인덱스 상태라 **prefix 체크포인트 재개
  (Phase I) 가 자명하게 건전**하고, 상태가 불변·구조공유라 체크포인트가 싸다.
- **Phase II (suffix 재사용)**: 편집 구간 재파스가 구 파스 상태와 "gen-shift
  modulo 재일치"하는 지점부터 구 상태·기록을 splice. CDG/ACP 는 비국소 의무가
  **명시적 조건 집합**으로 상태에 드러나므로, "조건 집합 포함 상태 완전 일치"가
  건전한 재사용 기준이 된다 (숨은 비국소성이 없음 — 이 형식론의 구조적 이점).

## 1. 수렴 거리 프로브 결과 (2026-07-10)

방법: 원본/편집 파스의 per-gen 지문 비교. gen 앵커는 편집위치-인지 인코딩
(g≤p 절대 / g>p 는 cur−g 오프셋 — shift-동치가 지문 일치가 되도록). semantic
(보고 shadow 제외) / strict (포함) 이중 기준. **자기검증 통과** (identity 5,523
gen 전일치, no-op 편집 즉시 수렴). 코퍼스: jar.bbx, cc.bbx, ccgen.mu,
chain_boundaries.mu × 편집 44종 (삽입/삭제/치환 × 위치 + 주석/문자열 내부).

| 지표 | semantic | strict |
|---|---|---|
| 수렴 거리 median | **0** | 1 |
| mean / p90 / max | 18.5 / 22 / 229 | 19.0 / 22 / 229 |
| 즉시 수렴 (=0) | 24/38 | — |
| 미수렴 / 재발산 | **0 / 0** | 0 / 0 |

- **핵심 답**: block-spanning 워처가 수렴을 수백~수천 gen 으로 늘릴 것이라는
  우려는 실측으로 반박됨 — 편집 후 **수십 자 안에 상태가 재정렬**되고, 늦는
  케이스의 블로커도 워처 폭발이 아니라 main 체인/조건 앵커의 재정렬 지연이다.
- **strict−semantic 격차 ≈ 1 gen**: 보고 shadow 는 라이브 상태 재정렬 한 스텝
  뒤에 따라온다 → **history splice 의 추가 비용이 거의 없다.**
- Phase I 단독의 잔여 비용 (재파스 꼬리): 편집 위치 25%/50%/75% → 평균 14.1k /
  6.3k / 3.9k 자 — 초중반 편집의 긴 꼬리가 Phase II 의 회수 대상.

**판정: Phase II 까지 착수 가치 확정.** 편집당 재파스가 "체크포인트→편집
구간→수십 자"로 떨어질 수 있는 헤드룸이 실측으로 존재한다.

## 2. 계획 (설계 문서: `incremental_parsing_design.md`)

- **Phase I0** — ParseSession + 주기 체크포인트 (K gen 링) + prefix 재개.
  게이트: 전체 재파스와 kernels history·accept **byte-identical** (편집 퍼징
  차등 오라클 — 완벽한 정확성 게이트가 공짜로 존재).
- **Phase I1** — per-gen strict 지문 보존 + 수렴 감지 (splice 없이 "재사용
  가능했던 양" 카운터 — 실현 이득 측정).
- **Phase I2** — splice: 동기화 지점에서 구조적 완전 일치 1회 검증 (해시 충돌
  차단) 후 suffix 상태 gen-rebase + history 를 세그먼트+오프셋 rope 로 splice.
- **Phase I3** — FFI 세션 API + mulang LSP 배선.

## 3. 참고

- 프로브 재현: `incremental_probe <mulang pb> [--json] [--edits-per-file N]`.
- 관련: rooted_watcher_gc.md (상태 구조·step6 의미론), PR #6 (eager EOF fold —
  suffix 재사용과의 상호작용은 설계 문서에서).
