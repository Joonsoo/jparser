# 워처 anchor 중복 제거 (Phase C) — 인접 gen 워처 dedup

작성: 2026-07-03. 다음 세션이 이 작업을 이어받기 위한 설계/실행 문서.
배경: `mgroup3/docs/kernels_history_optimization.md` §0.1–0.3 (파스 상태 폭발 분석,
replace 귀속 수정 = Phase B, 잔여 격차 분석). 이 문서는 그 잔여 중 "m3 고유"
부분인 워처 anchor 중복을 다룬다.

## 0. 결과 (2026-07-03 구현 완료 — 커밋 9debbd2f, 897e8541, 00dd82d5)

**원인은 §3 의 예상과 달리 "등록"이 아니라 step6 생존 규칙**이었다. 계측
(microAddChain 재현 + scanCondAnchorTags, `Mgroup2VsMgroup3PathsTest`):

- 등록은 m2 도 동일하게 한다 (인접 gen 워처 시동 자체는 양쪽 같음). m2 는
  실체화된 조건이 참조하지 않으면 다음 step 에 버리는데, m3 step6 는 미래
  조건의 잠재 anchor 3종 (tip=mp.gen / dot=mp.gen-1 / parent=parentGen) 을
  전부 유지해서, 어떤 조건도 참조하지 않는 bounded 워처가 체인이 사는 동안
  (블록 끝까지) 함께 살았다. 마이크로: sym13(AddExpr)@1 이 21 gen 생존
  (m2 는 1 gen) — jar.bbx 의 sym1227@2243/2244 가 정확히 이 패턴.
- **bounded (except/join/longest) 조건의 미래 anchor 는 dot 뿐임을 실측 확정**:
  scanCondAnchorTags 로 mulang 전 템플릿 (수십만 조건) 스캔 — bounded 의
  startGen 태그는 term frame 에서 MID (같은 step 에 starter 로 시동됨),
  edge frame 에서 GRAND(=dot, remapEdgeCondGens 의 결과) 뿐. CURR anchor 0건.
  lookahead 는 edge 조건이 CURR/MID 태그를 유지하므로 (remap 대상 아님) 구
  규약 유지 필요.
- **수정 (§4 의 A 변형)**: step6 collectFromShape 에서 bounded 심볼의 체인
  anchor 를 dot 만 유지 (lookahead 는 기존 3 anchor). Kotlin
  `Mgroup3Parser.kt` + Rust `core.rs` 미러, 각 10줄. 생성기/proto 변경 없음
  (fixture byte-identical). reportedCondRoots (보고 필터) 는 불변.
- 안전성 논거: fresh 로 만들어진 root 가 생성 step 에 pruned 되어도, 다음
  step 의 MID 조건/starter 는 같은 key 의 same-input 시동으로 동일 span 을
  복원한다 (span-정규화 규약과 정합). 생성-step에 pruned 된 root 는
  activeCondPaths 에 오르지 않아 everSeen 에 들어가지 않으므로 no-respawn
  규칙과 충돌 없음. 수명 중간에 pruned 되는 root 는 dot anchor 가 사라진
  시점 = 그 span 을 GRAND 로 참조할 수 있는 마지막 체인 milestone 이 죽은
  시점이므로 미래 참조가 불가능.

**검증**: microAddChain 65→32 roots, pre-close gen 에서 m3 cond roots+main =
m2 firsts 정확히 일치 (13=13, shapes 85=85). `MG3_RECORD_COND_DIFF=1`
runMgroup3Test 132/0 (신규 계측 2 포함) + HistoryDiff 2/2 + PathsDiff 5/5,
cargo test 전부 그린 + 커밋된 fixture byte-identical, mulang HEAD worktree
`parser.generate` + `parser.test:test` 55/55.

**성능 (release, jar.bbx)**: peak 2,640 → **1,519 shapes** (목표 ~1,900 초과),
outer 워처 6→3 roots (sym1225@2242 / sym1227@2242 / sym792@2248 — 심볼당
1 anchor, m2 와 동일; 중첩 블록도 3 roots), parse 5.9s → **1.89s** (예상
~4.5s 를 크게 초과 — 중복 워처가 peak 뿐 아니라 블록 전 구간에서 시뮬레이션을
복제하고 있었음). cc.bbx 3.2→1.23s, maven 1.27s, ktjvm 1.21s, junit 0.89s.
잔여 후속은 walk/encode (§0.1 (c)) 와 문법 트랙 (§8).

## 1. 목표와 현황

Phase B (jparser 커밋 47a7b5e7) 이후 상태:
- native 가 mg2 를 역전 (bibix4 --profile-startup Phase 1+2: 16.0s vs mg2 19.1s).
- main path 의 구조적 모호성은 m2 와 path 단위로 정확히 일치
  (jar.bbx gen 3731: 68=68, gen 4369: 236=236).
- **남은 m3 고유 잔여 = 같은 워처 심볼이 인접 gen 2–3개에 별도 cond root 로
  시동되는 것.** jar.bbx peak (gen 4369) 실측 (profile_steps):

  ```
  sym1225@2242: 236    sym1225@2243: 236
  sym1227@2242: 236    sym1227@2243: 236    sym1227@2244: 236
  sym792@2248:  236    (main sym1@0: 236)
  sym1225@2918: 118    sym1227@2920: 118    sym792@2926: 118   (중첩 블록)
  ```

  sym1225 = AddExpr (LessExpr 의 `<AddExpr>` longest body), sym1227 = AddExpr
  자체 `<...>` 의 body (OneOf{MulExpr|seq}), sym792 = `CallChain_+` (longest
  body). 같은 지점에서 m2 는 심볼당 anchor 1개 (sym1214/1216@2242, sym780@2248).
  outer 블록 워처가 m3 6 roots vs m2 3 roots → +~700 shapes (peak 2,640 의 ~27%).
- 각 워처 root 는 anchor 부터 블록 끝까지 독립 시뮬레이션이므로 anchor 하나를
  없애면 그 시뮬레이션 전체 (~236 shapes × 블록 길이 gen) 가 사라진다.
- **기대 효과**: jar.bbx peak ~2,640 → ~1,900, parse 5.9s → ~4.5s 추정.
  (1차 목표는 이미 달성 — 성능 임계 아님. 우선순위 중간.)

## 2. 현재 규약 (span-정규화 arc 커밋 950f1b9d 의 결과)

- **bounded 계열 (Unless/OnlyIf/NoLongerMatch = except/join/longest)**:
  span-정규화 key. CondRootStarter { key_gen, same_input } — derive-phase 관찰
  → MID + same-input (key = ctx.gen, 이번 입력이 watcher 첫 글자), progress-phase
  frontier → NEXT + fresh. same-input 시동 즉사 시 key 소진 (everSeenCondRoots)
  = zombie 방지.
- **lookahead 계열 (Exists/NotExists)**: 구 규약 유지 (의도적) — NEXT +
  same_input (실 span 은 gen-1, rootReportGens 에 -1 기록), **죽으면 같은 key 로
  fresh fallback 재시동**. 이유: m3 는 고정 위치 atomic 을 매 step 현재 경계에서
  재파생하므로 lookahead anchor 가 드리프트하고 (Exists(P,k) at step k), 등록-gen
  키 same-input watcher 와 쌍을 이루는 자기일관 시스템 (a^n b^n c^n 다세대
  lookahead 가 의존; per-char !b repeat 는 같은 key 의 양쪽 해석 필요).
  런타임은 proto `lookahead_cond_symbol_ids` 로 시동 flavor 판별.
- 관련 코드: 시동 = Mgroup3Parser.kt step1b/step3 (starterDied,
  everSeenCondRoots), 생성기 emitCondRootStarters / observedCondSymbolsFromAcc
  (Mgroup3ParserGenerator.kt), 생존/prune = step6 (referencedRoots 기반).
  Rust 미러: mgroup3-native/src/parser/core.rs (생성기 수정만이면 Rust 코드
  변경 불필요 — parserdata 재생성으로 전파).

## 3. 미해결 질문 — Phase C 의 첫 조사 과제

실측된 anchor 2242/2243/2244 는 입력상 `cached(` 의 'c','a','c' — **뒤 두 개는
식별자 중간 위치다** (2242 가 진짜 표현식 시작). 그리고 sym1225/1227 은 longest
= bounded 계열이라 이미 span-정규화 대상인데도 인접 anchor 가 2–3개다.

확정해야 할 것:
1. **누가 2243/2244 anchor 를 등록하는가**: main path 가 식별자('cached')를
   스캔하는 동안 매 step 의 derive closure 가 enclosing longest cond symbol 을
   현재 경계 gen 으로 재관찰(observedCondSymbols → CondRootStarter)해서 새 root
   를 만드는 것으로 추정 — 등록 경로 (step1b/step3 의 condRootStarters vs
   observing closure) 를 계측으로 특정할 것.
2. **그 anchor 를 참조하는 조건이 실제로 존재하는가**: NoLongerMatch(1227, 2243)
   을 참조하는 live 조건이 있는지. (a) 참조가 없다면 — step6 prune 이 왜 못
   버리는지 (everSeen/보고 필터와의 상호작용?), (b) 참조가 있다면 — 그 조건은
   어떤 파스 해석에 속하며 m2 는 같은 해석을 어떻게 1-anchor 로 처리하는지
   (m2 collectTrackings / pendedAcceptConditionKernels 대조).
3. 왜 이 root 들이 블록 끝까지 사는가: 'ached', 'ched' 로 시작하는 AddExpr 도
   실제로 유효하게 파싱 가능 (긴 매치 존재) 하기 때문일 것 — 즉 등록만 막으면
   생존은 자동 해결.

계측 방법: profile_steps 는 root 별 shapes 만 보여줌 — 조건 참조까지 보려면
(i) Kotlin 쪽에서 jar.bbx 급 입력을 돌리기는 무거우니 마이크로 재현 우선:
`Mgroup2VsMgroup3PathsTest` (runMgroup3PathsDiffTest) 의 microLambda 문법에
`<AddExpr>` 식 longest 를 추가해 인접-anchor 중복을 소규모 재현, 조건/root 를
직접 덤프. (ii) 필요시 Rust profile_steps 에 특정 root 를 참조하는 조건 덤프
추가 (PathMap 의 cond 들에서 referencedRoots 검사).

## 4. 접근 방향 (조사 결과에 따라 택1)

- **A. 참조 없는 anchor 인 경우 (가장 유력/안전)**: 등록 필터 강화 — 어떤
  조건도 참조하지 않(을 것이 확정인)는 cond root 는 시동하지 않거나 (생성기의
  emitCondRootStarters 에서 관찰-조건 대응을 좁힘), step6 prune 이 버리게 함.
  생성기만 수정이면 Rust 변경 불필요.
- **B. 참조는 있으나 중복 표현인 경우**: bounded span-정규화의 잔여 구멍 —
  재파생 프레임에서 span start 가 +1/+2 드리프트하는 케이스를 anchor 정규화로
  고정 (950f1b9d 의 bounded 규약을 해당 등록 지점까지 확장).
- **C. lookahead 구 규약의 드리프트가 원인인 경우**: §5 리스크를 먼저 읽을 것.
  자기일관 시스템이라 국소 수정이 어렵다는 실측이 있음 — 이 경우 비용 대비
  보류도 유효한 결론.

## 5. 리스크 (950f1b9d arc 의 실측 교훈)

- 전면 정규화 + 전면 Grand 시도 → mut def 깨짐. parentGen-1 재정의 →
  a^n b^n c^n 다세대 lookahead 깨짐 (드리프트 쌍 의존 발견). per-char lookahead
  의 same-input/fresh 동일 key 충돌 → lookahead 만 구 규약 + fallback 복원으로
  수렴한 역사.
- zombie watcher 가 남의 span finish 를 Unless 로 흡수 (ExceptGrammar4_1 오거부)
  — anchor 를 잘못 지우면 반대 방향 오답. 등록을 막을 땐 "그 key 를 참조하는
  조건이 없다"가 전제.
- fresh fallback 제거 → per-char except/lookahead repeat 깨짐 실측.

## 6. 검증 사다리

1. 계측 (§3) 으로 원인 확정 — 수정 전에 마이크로 재현 확보.
2. `runMgroup3Test` (130/0; `MG3_RECORD_COND_DIFF=1` 권장), CornerCase,
   Advanced, `runMgroup3HistoryDiffTest` (m2 parity), `runMgroup3PathsDiffTest`.
3. `cd mgroup3-native && cargo test` — 커밋된 fixture 는 runMgroup3Test 재생성
   후에도 byte-identical 이어야 정상; mulang fixture 는 parser_diff golden 유지.
4. jar.bbx 측정: profile_steps 로 outer 워처 6→3 roots / peak ~1,900 확인,
   time_parse 로 parse 시간.
5. mulang: `bibix4 parser.generate` → `parser.test:test` (NativeParserDiffTest;
   testMlc 실패는 기존 이슈). mulang 작업트리에 WIP 가 있으면 HEAD worktree 로
   빌드할 것 (앞선 세션들 방식).

## 7. 도구

- `mgroup3-native/src/bin/profile_steps.rs`: step 별 wall + 상태 크기, peak 또는
  `MG3_DUMP_AT=<gen>` 지점의 root 별 shapes/체인 구조, `MG3_DUMP_CHAINS=1` 로
  체인 전량 덤프. 데이터: `tests/fixtures/parser_generated/mulang/data.pb`
  (runMgroup3Test 가 재생성), 입력 jar.bbx 는
  `unzip bibix4-bundle.jar "*.bbx"` 로 추출.
- `mgroup3-native/src/bin/symdump.rs`: symbolId → 문법 정의.
- `Mgroup2VsMgroup3PathsTest` (`bibix4 runMgroup3PathsDiffTest`): 같은 NGrammar
  로 m2/m3 를 나란히 구동 — 마이크로 재현/A-B 용.

## 8. 관련 별도 트랙 — 문법 수준 대안

**`mgroup3/docs/mulang_grammar_ambiguity.md` 로 분리** (분석 상세 + 개선 옵션
+ 검토/실험 계획). 요지: jar.bbx 비용의 더 큰 구조적 원인은 mulang 문법의
블록-스팬 longest 두 가족 (`<CallChain_+>`, `<AddExpr>` 계열) — 워처 자체가
사라지므로 기대 효과가 Phase C 보다 크다 (peak ~300–500, jar.bbx ~1s 급 추정).
문법 트랙이 성사되면 Phase C 의 대상 워처가 대부분 사라지므로 이 문서의
우선순위는 재평가할 것. 단 언어 설계 결정 필요 — 사용자와 검토 세션 예정.
