# mgroup4 — bounded interior milestone groups (프로토타입)

mgroup3 파서의 **bounded interior milestone group** 확장 프로토타입 (**Phase A 완주:
A0~A4, 2026-07-10 — 킬 기준 통과**. Phase B 계획: `docs/phase_b_plan.md`).
현행 mgroup3 은 tip 한 자리만 group 이고 interior 체인 노드는 전부 singleton `Kernel`
이다. mgroup4 는 tip 쪽 마지막 `n` 개 interior 노드까지 group 을 허용해 (window n),
"window 안 한 위치만 다른 형제 path" 를 하나의 group shape 로 접어 파스 shape 수를 줄인다.
`n=1` 이면 병합 패스가 no-op 이라 현행 mgroup3 과 동일하게 동작한다.

**Phase A 상태 (완주)**: A0 (표현) + A1 (per-gen 병합) + A2 (group 다중-gen 수명 +
reduce 분열) + A3 (window-exit 지연 분열) + **A4 (병합 패스 최적화 + 정식 시간 측정)**
완료. group 은 여러 gen 을 살며 term descend 로 심화되고, (i) reduce 가 group 노드에
도달할 때 또는 (ii) descend 로 depth 가 n 을 넘을 때만 분열한다. 모든 n∈{1,2,4,6} 에서
G1/G2 byte-identical. **핵심 결과**:
- **정식 시간 (Mgroup4Bench, JVM 분리, 중앙값)**: json2 n=6 **2.10×** (2401→1143ms),
  jquery n=6 **2.23×** (40.3s→18.1s), ccgen n=6 1.86×, chain n=4 1.10×. n=1 은 mgroup3
  대비 회귀 없음. 시간 이득이 shape ratio(1.74~1.78)를 초과하는 건 live-set 축소의
  GC/메모리 복리 효과. (주의: A2/A3 시점의 "n=6 이 1.32× 느림" 신호는 단일-JVM
  interleave 측정 편향으로 판명 — 벤치는 반드시 JVM 분리.)
- 병합의 ~90.5% 가 late-convergence (fork 후 여러 gen 뒤 tip 이 수렴) — parserdata 사전
  그룹핑만으로는 ~10% 만 잡힘. **런타임 packing 이 주 기제** (Phase B 아키텍처 결정).
- fold 시 멤버는 `milestone.gen`/`milestoneReportGen`/`reportGen` 이 항상 멤버-불변 —
  app 멤버별 복제는 실측상 no-op.
- 병합 패스 자기시간은 prefix/suffix 해시 메모이즈 + 지연 물질화 후 파스의 3.9~14%
  (병목 아님). 지배 비용은 step6_prune (파서 본체) 이고 n 증가 이득의 원천.

## mgroup3 무변경 원칙

**mgroup3 은 다른 프로젝트들이 참조하는 안정 모듈이라 무변경으로 유지한다.** mgroup4 는
별도 폴더(`mgroup4/`) + 별도 build target(`mgroup4.parser`, `mgroup4.test`)이다. mgroup3
소스는 건드리지 않는다.

## 모듈 구조

- `parser/kotlin/com/giyeok/jparser/mgroup4/` — 파서 런타임 포크. mgroup3 parser 패키지
  6개 파일을 그대로 포크했다 (`ParsingCtx.kt`, `AcceptCondition.kt`,
  `RecordConditionEvaluator.kt`, `Mgroup3ParserDataPlain.kt`, `ParsingData.kt`,
  `Mgroup4Parser.kt`). 클래스명은 `Mgroup3Parser` → `Mgroup4Parser` 만 개명, 나머지는
  패키지(`mgroup4`)로 구분되므로 파일/클래스명 유지.
  - **왜 전 파일 포크?** `AcceptCondition` 이 `PathRoot`(ParsingCtx.kt 정의)를
    참조하고 `PathShape`/`PathMap` 이 `AcceptCondition` 을 참조하는 상호 결합이라,
    `ParsingCtx` 만 포크하면 `mgroup3.PathRoot` vs `mgroup4.PathRoot` 타입 불일치가
    생긴다 (`AcceptCondition.referencedRoots` 가 map key 로 쓰임). 결합 클러스터를
    통째로 옮겨야 타입이 일관된다.
- **proto 는 재사용**: `mgroup3.proto.Mgroup3ParserData` 를 그대로 로드한다 (포맷 동일 —
  generator 도 mgroup3 `Mgroup3ParserGenerator` 재사용). 포크 파일들은
  `com.giyeok.jparser.mgroup3.proto.*` 만 import 한다.
- `test/kotlin/com/giyeok/jparser/mgroup4/Mgroup4DifferentialTest.kt` — 차등 오라클.

## 설계 · 계측 문서

- `mgroup3/docs/mgroup4_bounded_interior_groups.md` — 아이디어·Phase 0 계측·판정 갱신·
  Phase A 요약 (§6.8). (mgroup3 트리에 있는 이유: 커밋 이력이 거기서 시작)
- `mgroup3/docs/mgroup4_phase_a_design.md` — Phase A 설계 + 실행 결과 (§7).
- `mgroup4/docs/phase_b_plan.md` — **Phase B 계획 + 현재 구현 상태 + late-convergence
  해설** (이후 문서는 mgroup4/docs 에).

## 게이트 실행법

빌드: `/Users/joonsoo/Documents/apps/bibix4/bibix4 <action>`,
로그: `bbxbuild/logs/actions/jparser.<action>/context.log`.

- **차등 오라클** (핵심): `bibix4 runMgroup4DiffTest`
  - **G1** — mgroup4(n=1) ≡ mgroup3: 같은 parserdata·입력에서 두 독립 파서 클래스의
    kernelsHistory·accept 를 직접 비교 (fork 가 로직을 안 바꿨음을 증명).
  - **G2** — mgroup4(n∈{2,4,6}) ≡ mgroup4(n=1): interior group 병합이 내부 표현일 뿐
    출력 불변임을 검증.
  - 인프로세스 문법 셋(structural corpus + asdl)은 항상 실행. mulang chain_boundaries.mu
    / es5 json2.js 는 `MG4_DIFF=1` (action 이 기본 설정) + 대형 parserdata 존재 시.
  - `MG4_SHAPE_STATS=1` (action 이 기본 설정) → 병합률/거부사유(condition/gen·obs)
    카운터 출력 (A1 §H5 실현율 측정).
- **A1 측정** (mean-shape 카운터): env `MG4_SHAPE_STATS=1` 로 실행. 파스 출력 무영향.
- **n 전역 오버라이드**: env `MG4_INTERIOR_N=<n>` (생성자 기본값보다 우선 — 코드 수정
  없이 임의 n 으로 스위트 구동).
- **정식 시간 측정**: `Mgroup4Bench` main 러너 — **설정(n)별 별도 JVM 필수** (단일-JVM
  interleave 는 편향 실측됨), 8g heap, 웜업 후 median/min. classpath 는
  `bibix4 dumpMgroup4Classpath` 로 덤프해 java 직접 spawn.
