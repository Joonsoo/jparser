# mulang 문법의 파스 비용 — 블록-스팬 longest 분석과 개선 옵션

작성: 2026-07-03. 새 세션에서 사용자와 함께 검토하기 위한 분석/옵션 문서.
**언어 설계 결정이 필요한 트랙** — 파서 구현 쪽 후속 (Phase C) 은
`watcher_anchor_dedup.md`, 배경 실측은 `kernels_history_optimization.md`
§0.1–0.3. 대상 파일: `../mulang/grammar/mulang.cdg` (jparser 기준 상대경로).

## 1. 왜 이 문서인가

Phase B (커밋 47a7b5e7) 이후 native 는 mg2 를 역전했지만 (16.0s vs 19.1s),
jar.bbx 급 입력은 여전히 코퍼스 최대 스트래글러다 (parse 5.9s, mg2 는 같은
파일에 11.2s). **남은 비용의 구조적 원인은 파서가 아니라 문법이다** — m2/m3
가 같은 지점에서 path 단위로 동일한 상태를 유지함을 실측했다 (68=68, 236=236).
이 문서는 그 문법 원인 세 곳과 개선 옵션을 정리한다.

## 2. 비용 메커니즘 — CDG `<X>` (longest) 의 런타임 의미

- `<X>` 는 NoLongerMatch 조건 + **워처 cond path** 로 구현된다: anchor 부터
  X 의 가능한 모든 해석을 main path 와 나란히 별도 시뮬레이션하고, "더 긴
  매치"가 완성되는지 감시한다. **워처 수명 = X 가 도달할 수 있는 끝까지.**
- 따라서 X 가 블록(`{...}`, 수천 자)을 포함할 수 있으면 워처가 블록 전체를
  중복 재파싱한다. 워처 내부 상태 크기는 main path 와 같은 규모다.
- negative lookahead `!(X)` (NotExists 워처) 도 X 가 크면 같은 문제 — 단
  매치가 일찍 죽는 쪽 (X 가 실제로 없는 경우) 은 싸다. X 가 실제로 있는
  경로에서는 X 전체를 파싱한다.
- **실측 (jar.bbx, gen 4369 peak)**: 전체 2,640 shapes 중 워처가 ~2,400.
  `cached("...") { ...2,500자... }` 의 시작 (gen 2242–2248) 에 anchor 된
  워처들이 gen 4369 까지 (2,100+ gen) 생존하며 각각 main (236 shapes) 을
  복제. join/except 는 무관 — 전부 longest (와 큰 negative lookahead) 몫.

## 3. 문제 지점 1 — `<CallChain_+>`: 트레일링 람다 체인 greed

```
CallChain                                              // mulang.cdg:569
  = (Annots WS)? BaseCallee <CallChain_+>
CallChain_
  = Subscribe_* WS_NO_NL CallChainArgs
CallChainArgs
  = (CallArgs WS_NO_NL)? TrailingLambda
  | CallArgs !(WS_NO_NL TrailingLambda)                // :576
```

목적: `f(a) { ... }` 의 `{...}` 가 체인의 트레일링 람다이지 다음 문장이
아니도록 greedy 매치. 실측 워처: sym792(CallChain_+)@2248 — 블록 전체 생존.

**이미 있는 국소화 장치들** (이 장치들 덕에 chain-stop 은 대부분 O(1)자에서
자연 결정된다):
- 트레일러 부착은 `WS_NO_NL` (같은 줄만).
- 문장 구분은 `StmtDelim = WS_NO_NL ('\n'|LineComment) WS | WS ';' WS`
  (:937) — 같은 줄 연속 두 문장은 애초에 불법.
- CallChainArgs 의 `!(WS_NO_NL TrailingLambda)` — args-only 와 args+lambda
  arm 의 국소 분해. (단 이 lookahead 자체도 TrailingLambda **전체**를 파싱하는
  NotExists 워처라, 람다가 실제로 뒤따르는 경로에서는 블록-스팬이다.)

**개선 옵션**:
- **1a. `<...>` 제거 실험**: 위 장치들로 충분한지 실측 — 제거 후 15 예제 +
  실코퍼스에서 모호/오파싱이 나는 입력을 수집하고, 나오는 케이스만 국소
  가드. (주석 :733 "원래 그냥 Block도 있었는데, Stmts에 lambda가 오는 것과
  ambiguous해서 제외" — 저자가 이 모호성 가족과 싸운 흔적이 있으니 지키는
  케이스가 실재할 수 있음. 케이스를 문서화하는 것 자체가 성과.)
- **1b. 여는-토큰 1자 lookahead 로 대체**: `!(WS_NO_NL TrailingLambda)` →
  `!(WS_NO_NL '{')` 류. 의미: "같은 줄의 `{` 는 무조건 트레일링 람다" (Kotlin
  과 같은 규칙). 워처가 다음 1–2자만 보고 죽는다. 표면 의미 변화: 같은 줄
  `{` 가 람다로 완성되지 못하면 (지금은 다른 해석으로 살아남을 입력이) 파스
  에러가 됨 — 실코퍼스 영향 사전 조사 필요.
- 기대 효과: sym792 워처 가족 소멸.

## 4. 문제 지점 2 — `<AddExpr>` 가족: generic vs 비교 연산 모호성

```
// <AddExpr> 인 이유는 hello<generic>(123) 에서 <generic>이 비교 연산으로
// 해석되지 않게 하기 위함                              // mulang.cdg:538
LessExpr = (<AddExpr> WS ("<"|"<=")&OpTk WS)+ AddExpr   // :540
AddExpr: AddExprOr = <MulExpr
  | MulExpr (WS ("+"|"-")&OpTk WS MulExpr ...)+ ...>    // :552-555
```

본질: C++ template angle-bracket 과 같은 부류의 **진짜 CFG 모호성** —
`hello<generic>(123)` 은 비교 체인 (`hello < generic > (123)`, Less/Greater
가 n-ary 체인이라 성립) 으로도 파싱된다. 가드 (longest) 는 정당하다.

문제는 **가드의 범위**: AddExpr ⊃ MulExpr ⊃ CallExpr ⊃ 트레일링 람다 블록.
`return cached("...") { ... }` 한 문장에서 AddExpr-longest 워처
(실측 sym1225/1227@2242–2244, 인접 anchor 2–3개씩 — anchor 중복은 Phase C
로 해결됨, §9) 가 **블록 전체를 스팬**한다. jar.bbx 워처 비용의 다수가 이
가족 (Phase C 후 47%).

**개선 옵션**:
- **2a. spacing 규칙으로 토큰 수준 분해 (권장 검토안)**: generic 은 이름에
  붙는다 (`f<T>` — 이미 `NameTok (WS_NO_NL GenericArgs)?` 로 같은-줄 제약
  있음), 비교 `<` 는 **양쪽 공백 필수** (`a < b`; `a<b` 불허). LessExpr 의
  op 주변 WS 를 "1자 이상" 으로 바꾸면 (`WS_min1` 류 신설) `f<T>(...)` 와
  `f < T` 가 지역적으로 갈라져 `<AddExpr>` 가드가 필요 없어진다. 비용:
  표면 문법 제약 (붙인 비교 불허) — 기존 코퍼스 grep 으로 영향 사전 확인
  가능. Swift/Kotlin 계열이 같은 종류의 spacing-민감 규칙을 쓴다.
- **2b. Rust 식 turbofish** (`f::<T>(...)`): 모호성 자체 제거, 표면 변화 큼.
- **2c. 비교 체인의 이항 제한**: Less/Greater 의 `(...)+` 를 이항으로 —
  `a < b > c` 불허. `hello<generic>(123)` 의 비교-해석 성립 범위가 줄지만
  `hello < generic` 접두 모호는 남으므로 2a 보다 불완전.
- **AddExpr 자체의 `<...>` 존재 이유 확정 필요**: bare MulExpr arm 의 이른
  종료는 대부분 enclosing sequence 에서 1–2 토큰 내 죽는다 — 이 `<>` 가
  generic 케이스 외에 무엇을 지키는지 제거 실험으로 확인. (참고 주석 :557
  "mgroup2 에서 왜인지 잘 안되는듯함" — 이 영역에 이미 알려진 거친 모서리
  있음.)
- 기대 효과: sym1225/1227 워처 가족 (jar.bbx 워처의 대부분) 소멸.

## 5. 문제 지점 3 — 우선순위 타워 (~10레벨)

`Expr→BorrowOrMove→Or→And→NotEq→Eq→Compare→Range→Add→Mul→Prefix→Call→Primary`
(:509–565). atom 하나를 파싱하는 동안 "내 연산자가 뒤따를 가능성" kernel 이
레벨마다 유지된다 — 실측 main path fork ×5 (sym1233~1259, 8종이 같은 span
4208→4320 에; `file.writeText("...${mainClass}...")` 인자). longest 와 무관한
순수 표현 비용이고 mg2 도 동일하게 지불.

**옵션**: 이항 레벨들을 `BinExpr = PrefixExpr (WS BinOp&OpTk WS PrefixExpr)*`
하나로 합치고 우선순위 재결합은 AST 소비층 (MulangAst 이후, CDG astifier 밖
Kotlin/Rust 코드) 에서 수행. **AST 스키마 변경** → parser.generate 산출물 +
AstToIR0 수정 필요 — 가장 침습적. 지점 2 를 먼저 풀어야 의미 있음 (타워를
합쳐도 longest 가드가 남으면 워처는 여전히 블록-스팬).

## 6. 기대 효과와 우선순위

| 변경 | 기대 효과 (jar.bbx) | 침습도 |
|---|---|---|
| 1 (+2) 블록-스팬 longest 제거/국소화 | peak 2,640 → ~300–500, parse 5.9s → ~1s 급 | 문법만; 표면 규칙 일부 변화 |
| 3 우선순위 타워 압축 | main ×5 → ×1–2 (추가 ~2–3×) | AST 스키마 + IR0 변환 |

mg2 fallback 도 같은 문법을 쓰므로 동반 개선된다 (mg2 의 jar.bbx 11.2s 도
같은 원인). 문법 변경 시 mg2/mg3 parserdata 재생성 + mulang 커밋이 필요하고,
**기존 .bbx/.bbx4/.mu 코퍼스가 새 문법에서 전부 수락되는지가 1차 게이트다.**

## 7. 검토/실험 계획 (새 세션)

0. **코퍼스 사전 조사** (문법 변경 없이): 기존 코퍼스에서 영향 패턴 grep —
   붙인 비교 `a<b` (2a 가 금지하게 됨), 같은 줄 `}` 뒤 `(`/`{`/`[` (1b 영향),
   generic 사용 위치. bibix4 stdlib .bbx 는 `unzip bibix4-bundle.jar "*.bbx"`,
   mulang 예제는 ../mulang/examples/.
1. **마이크로 A/B** (mulang.cdg 무변경): `Mgroup2VsMgroup3PathsTest`
   (runMgroup3PathsDiffTest) 하니스에 microLambda 변형 문법들을 추가 —
   (i) 현행 (longest), (ii) longest 제거, (iii) 1자 lookahead, (iv) spacing
   규칙 — shapes/step 시간을 대조해 옵션별 효과를 소규모 정량화.
2. **mulang.cdg 실험**: 브랜치/worktree 에서 `<CallChain_+>` 부터 하나씩 —
   `bibix4 parser.generate` → MulangCdgTest (예제 15) + NativeParserDiffTest →
   jar.bbx profile_steps/time_parse. 모호성이 드러나는 입력은 케이스 목록으로
   문서화 (실패해도 성과).
3. **언어 설계 결정** (사용자): spacing 규칙/트레일링 람다 규칙의 표면 변화
   수용 여부. 결정 후 mulang 문법+코퍼스 수정, parserdata 재생성, 커밋.

도구: `mgroup3-native` 의 profile_steps (MG3_DUMP_AT / MG3_DUMP_CHAINS),
time_parse, symdump; jar.bbx 등 코퍼스 추출 방법은 `watcher_anchor_dedup.md`
§7 과 동일.

## 8. 관련 문서

- `kernels_history_optimization.md` §0.1–0.3 — 상태 폭발 실측, Phase A/B.
- `watcher_anchor_dedup.md` — Phase C (파서 쪽 워처 anchor 중복; 이 문서와
  독립·병행 가능. 문법 트랙이 성사되면 Phase C 의 대상 워처 자체가 대부분
  사라지므로 우선순위 재평가).

## 9. 현황 갱신 + 사전 조사 결과 (2026-07-03, Phase C 완료 후)

**Phase C (워처 anchor dedup) 완료 후에도 문제는 그대로 유효하다** — Phase C
는 심볼당 anchor 를 1개로 줄였을 뿐, 워처 1개가 블록 전체를 스팬하는 것은
문법 고유라 남는다. jar.bbx 재실측 (peak gen 4369, parse 1.89s):

- peak 1,519 shapes = main 236 (16%) + **워처 1,283 (84%)**:
  - `<AddExpr>` 가족 (sym1225/1227@2242,2918,4207): **721 (47%)**
  - `<CallChain_+>` (sym792@2248,2926): **354 (23%)**
  - `!(WS_NO_NL TrailingLambda)` (sym820@2287,2940): **177 (12%)** —
    §3 의 "lookahead 자체도 블록-스팬" 노트가 실측으로 확인됨 (sym820 =
    `WS_NO_NL TrailingLambda` 시퀀스의 NotExists 워처).
- anchor 2242–2287 워처들이 gen 4369 에도 생존 (2,100+ gen 스팬) — 지속 확인.
- 기대 효과 재계산: 500+ shapes 스텝들이 parse 의 ~1.0s/1.89s — 워처 가족
  제거 시 peak ~250–300, parse **~0.6–0.8s** 추정 (§6 표의 숫자를 대체).
  타워 압축 (지점 3) 은 그 위에 추가 ~2×.

**§7.0 코퍼스 사전 조사 완료** (bibix4 stdlib 13 .bbx + mulang examples +
양쪽 build.bbx4):

- **무공백 비교 (`a<b`) 0건** — 표현식 비교는 전부 공백 사용 (`i < n`,
  `queue.size > 0u64`). → 2a 의 spacing 규칙은 코퍼스-클린.
- **표현식 위치 generic 호출이 실존**: `collectTargets<JunitTestLib>()`
  (stdlib junit.bbx!), `move<u64>(x=123)`, `list<CppMember>()`,
  `map<u32, map<u32, arc&Buffer>>()` — 전부 이름에 밀착. **가드는 실제로
  지키는 게 있다 — `<AddExpr>` 단순 제거는 불가, 2a 류 대체 장치 필수.**
  주의: 현행 GenericArgs 부착은 `WS_NO_NL` (같은 줄 공백 허용 —
  `hello <T>` 도 generic) 이므로 2a 설계 시 밀착(juxtaposition)으로 좁혀야
  `hello < T` 비교와 갈라진다.
- **줄 시작 이항 연산자 continuation 0건**, 줄 끝 연산자 continuation 4건
  (cc.bbx 3, maven.bbx 1) — 아래 "AddExpr 자체 `<>`" 대체 규칙과 호환.

**문법 정독으로 확정한 추가 사실**:

- `TrailingLambda = Lambda | Block` (:608) 이고 둘 다 `'{'` 로 시작 —
  **1b (`!(WS_NO_NL '{')`) 는 사실상 정확한 대체**다. 두 표현이 갈라지는
  경우는 같은 줄 `{...}` 가 Lambda 로도 Block 으로도 파싱 불가할 때뿐인데,
  그 입력은 현행 문법에서도 (같은 줄 두 문장 불법 + `{` 를 흡수할 다른
  규칙 부재로) 파스 에러다.
- **NoLambda 가족이 이미 "식 뒤 같은 줄 `{`" 컨텍스트를 구조적으로 분리**:
  while/for/if 조건, match 대상, MapEntry key 는 ExprNoLambda (:499–:871) —
  1b 가 이들을 오작동시키지 않는다. 단 `<AddExprNoLambda>` (:993) 와
  AddExprNoLambda 자체 `<>` (:1009) 가 있으므로 **지점 2 수정은 두 가족
  모두** 적용해야 함. CallChainNoLambda (:1047) 의 자체 `<>` 는 람다가 없어
  args 범위로 국소적 — 우선순위 낮음.
- **AddExpr 자체 `<...>` 의 존재 이유 확정**: 체인 연산자가 plain `WS`
  (개행 허용) 라 `let x = a\n+ b` (continuation) vs `a` 문장 + `+b` 문장
  (prefix op) 의 진짜 모호성 — :557 주석의 `*p` deref 케이스와 같은 부류
  (JS ASI 류). jparser 루트의 구 mulang.cdg 사본은 여기 `WS_NO_NL` 을 썼고
  현 문법은 longest 로 바꾼 흔적. **대체 후보: Kotlin 식 "연산자는 lhs 와
  같은 줄" 규칙** (op 앞 WS_NO_NL, `a +\n b` 허용 / `a\n+ b` 는 두 문장) —
  코퍼스의 줄바꿈 체인 4건 전부 op-at-EOL 이라 클린.

**권장 우선순위 (효과 순, 전부 사용자 언어-설계 결정 필요)**:
1. **2a+ (47%)**: 비교 `<`/`>` 양쪽 공백 필수 + GenericArgs 밀착 + 이항
   연산자 앞 WS_NO_NL → `<AddExpr>`/`<AddExprNoLambda>`/AddExpr 자체 `<>`
   3종 모두 제거 가능.
2. **1b (12%)**: `!(WS_NO_NL TrailingLambda)` → `!(WS_NO_NL '{')`.
3. **1a (23%)**: `<CallChain_+>` 제거 실험 — 1b 와 함께면 체인 정지가
   국소 결정되므로 성립 가능성 높음. 반례는 실험으로 수집.
4. 지점 3 (타워, main ×5): 1–3 이후 별도 트랙 (AST 스키마 변경).
