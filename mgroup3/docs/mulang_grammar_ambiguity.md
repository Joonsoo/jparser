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

## 10. 권장안의 구체 CDG 수정안 (2026-07-04 — 검토 세션용 초안, 미적용)

원칙 두 개로 요약된다:
- **밀착 = generic, 공백 = 비교** (`f<T>(x)` vs `a < b`).
- **이항 연산자는 lhs 와 같은 줄** (`a +\n b` 허용, `a\n+ b` 는 두 문장 —
  Kotlin 과 동일).

### 10.0 새 보조 정의

```
// 같은 줄 공백 1개 이상 — 비교 연산자를 밀착 generic 과 토큰 수준에서 분해.
WS_NO_NL1 = (' \t' | BlockComment) WS_NO_NL {""}
```

(대칭 강제 — `a <b` 도 불허 — 를 원하면 op 뒤도 1+ 로:
`WS1 = (' \n\r\t' | Comment) WS {""}`. 분해에는 op 앞 1+ 만으로 충분해서
아래 초안은 앞쪽만 강제. 뒤쪽까지 강제할지는 스타일 결정.)

### 10.1 권장안 1 (2a+) — `<AddExpr>` 가족 3종 제거

**(a) LessExpr/GreaterExpr — 가드 제거 + 비교 op 앞 공백 1+ (같은 줄):**

```
// 현행 (:539-546)
LessExpr
  = (<AddExpr> WS ("<" {%LT} | "<=" {%LE})&OpTk WS)+ AddExpr
    {LessExpr(lhsChain=$0{LessChainLhs(lhs=$0, op: %LessOps=$2)}, rhs=$1)}
GreaterExpr
  = (AddExpr WS (">" {%GT} | ">=" {%GE})&OpTk WS)+ AddExpr
    {GreaterExpr(lhsChain=$0{GreaterChainLhs(lhs=$0, op: %GreaterOps=$2)}, rhs=$1)}

// 수정안 (액션 인덱스 불변 — 원소 수 동일)
LessExpr
  = (AddExpr WS_NO_NL1 ("<" {%LT} | "<=" {%LE})&OpTk WS)+ AddExpr
    {LessExpr(lhsChain=$0{LessChainLhs(lhs=$0, op: %LessOps=$2)}, rhs=$1)}
GreaterExpr
  = (AddExpr WS_NO_NL1 (">" {%GT} | ">=" {%GE})&OpTk WS)+ AddExpr
    {GreaterExpr(lhsChain=$0{GreaterChainLhs(lhs=$0, op: %GreaterOps=$2)}, rhs=$1)}
```

`>` 계열은 generic 분해에 필수는 아니지만 (`<` 만으로 갈라짐) `a<b` 불허 /
`a>b` 허용의 비대칭을 피하기 위해 같은 규칙 적용. LessExprNoLambda (:992) /
GreaterExprNoLambda (:996) 도 동형으로 (`<AddExprNoLambda>` →
`AddExprNoLambda`, `WS` → `WS_NO_NL1`).

**(b) AddExpr/MulExpr — 자체 `<>` 제거 + op 앞 같은-줄 (0+):**

```
// 현행 (:552-560)
AddExpr: AddExprOr = <MulExpr
  | MulExpr
    (WS ("+" {%ADD} | "-" {%SUB})&OpTk WS MulExpr {AddChain(op: %AddOps=$1, rhs=$3)})+
    {AddExpr(lhs=$0, chain=$1)}>
MulExpr: MulExprOr = PrefixExpr
  | MulExpr WS ("*" {%MUL} | "/" {%DIV} | "%" {%REM})&OpTk WS PrefixExpr
    {MulExpr(op: %MulOps=$2, lhs=$0, rhs=$4)}

// 수정안 (액션 인덱스 불변)
AddExpr: AddExprOr = MulExpr
  | MulExpr
    (WS_NO_NL ("+" {%ADD} | "-" {%SUB})&OpTk WS MulExpr {AddChain(op: %AddOps=$1, rhs=$3)})+
    {AddExpr(lhs=$0, chain=$1)}
MulExpr: MulExprOr = PrefixExpr
  | MulExpr WS_NO_NL ("*" {%MUL} | "/" {%DIV} | "%" {%REM})&OpTk WS PrefixExpr
    {MulExpr(op: %MulOps=$2, lhs=$0, rhs=$4)}
```

- 근거: AddExpr `<>` 의 실제 역할 = 개행 continuation (`let x = a\n+ b`) vs
  다음 문장 prefix-op (`+b`/`-b`/`*p = 42`) 모호성 해소 (§9). op 앞
  WS_NO_NL 로 국소 분해되면 longest 불필요. **:557 주석의 `*p` deref 미해결
  이슈가 함께 풀린다** (`123\n*p = 42` 에서 `*` 가 이제 continuation 불가 →
  DerefAssign 문장).
- 코퍼스의 줄바꿈 체인 4건은 전부 op-at-EOL (`a +\n b`) 이라 그대로 수락.
- AddExprNoLambda (:1009) / MulExprNoLambda (:1015) 동형 적용.
- 선택: 나머지 이항 레벨 (Or `||`/And `&&`/Eq/NotEq/Range/In/Is) 도 op 앞
  WS_NO_NL 로 통일할지. 이들의 op 는 문장을 시작할 수 없어 모호성은 없음 —
  순수 스타일 일관성 문제 (통일 권장하나 필수 아님).

**(c) GenericArgs 밀착 (WS_NO_NL 제거) — 표현식 위치 5곳 + 타입 위치 3곳:**

```
// 표현식 위치 (필수 — 이게 있어야 (a) 의 가드 제거가 안전)
// :587  (BaseCallee)          액션 불변 ($1 = 그룹의 마지막 원소 = GenericArgs)
| NameTok GenericArgs? {SimpleNameCallee(name=$0, genericArgs=$1)}
// :591  (BaseCallee)          액션 불변
| '.' NameTok GenericArgs? {OneofShorthandCreate(name=$1, genericArgs=$2)}
// :686  (SubscribeAccess)     액션 불변
= CallExpr-Digits GenericArgs? <Subscribe_+>
  {SubscribeAccess(base=$0, baseGenericArgs=$1, subs=$2)}
// :690  (LongNameAccess)      ★ 인덱스 시프트: $2→$1, $3→$2
= NameLong GenericArgs <MemberSubscribe_+>
  {LongNameAccess(target=$0, genericArgs=$1, nameChain=$2)}
// :701  (GenericMemberSubscribe) ★ 인덱스 시프트: $5→$4
| WS '.' WS NameTok GenericArgs {GenericMemberSubscribe(name=$3, genericArgs=$4)}

// 타입 위치 (일관성 — "generic 은 항상 밀착" 한 문장 규칙을 위해 권장)
// :163  ClassType             액션 불변
ClassType = NameLong GenericArgs? {ClassType(clsName=$0, genericArgs=$1)}
// :270  ExtendFor             액션 불변
ExtendFor = NameLong GenericArgs? {ExtendFor(name=$0, genericArgs=$1)}
// :422  TypeWithGenericArgs   ★ 인덱스 시프트: $2→$1
TypeWithGenericArgs = TypePrimary GenericArgs {TypeWithGenericArgs(base=$0, args=$1)}
```

주의: 현행은 `WS_NO_NL` (같은 줄 공백 허용) 부착이라 `hello <T>(x)` 도
generic 인데, 밀착으로 좁혀야 `hello < T` (비교) 와 갈라진다. 코퍼스의
generic 은 전부 밀착이라 (§9) 실입력 영향 없음.

### 10.2 권장안 2 (1b) — TrailingLambda lookahead 를 1자로

```
// 현행 (:574-576)
CallChainArgs
  = (CallArgs WS_NO_NL)? TrailingLambda {CallChainArgs(args=$0$0, trailingLambda=$1)}
  | CallArgs !(WS_NO_NL TrailingLambda) {CallChainArgs(args=$0, trailingLambda=null)}

// 수정안 (둘째 arm 의 lookahead 만 교체 — 액션 불변)
CallChainArgs
  = (CallArgs WS_NO_NL)? TrailingLambda {CallChainArgs(args=$0$0, trailingLambda=$1)}
  | CallArgs !(WS_NO_NL '{') {CallChainArgs(args=$0, trailingLambda=null)}
```

근거: `TrailingLambda = Lambda | Block` 이 둘 다 `'{'` 시작 (§9) — 갈라지는
입력은 현행에서도 에러. NotExists 워처가 블록-스팬 → 1–2자로.

### 10.3 권장안 3 (1a) — `<CallChain_+>` 제거 (실험 후 확정)

```
// 현행 (:569-571)
CallChain
  = (Annots WS)? BaseCallee <CallChain_+>
    {CallChainExpr(annots=$0$0, callee=$1, chain=$2)}

// 수정안 (액션 불변)
CallChain
  = (Annots WS)? BaseCallee CallChain_+
    {CallChainExpr(annots=$0$0, callee=$1, chain=$2)}
```

성립 조건: 체인 연장 지점이 전부 국소 결정 — 트레일러 부착은 WS_NO_NL
(같은 줄), 람다 vs 비-람다는 10.2 의 1자 lookahead, 문장 경계는 StmtDelim.
반례 (짧은 체인 해석과 긴 체인 해석이 모두 전체 입력을 완주하는 케이스) 는
실험으로 수집 — 나오면 그 지점만 국소 가드.

### 10.4 이번 범위 밖 (관련 메모)

- `<Subscribe_+>` (:686, :1044) / `<MemberSubscribe_+>` (:690) /
  CallChainNoLambda 의 `<(...)+>` (:1047) / `<BorrowSubscribe_*>` — 같은
  chain-greed 가족이지만 peak 상위 워처가 아니고 스팬이 대체로 짧음
  (`[Index]` 내부에 큰 식이 오는 경우만 예외). 1a 가 성공하면 같은 논리로
  후속 정리 가능.
- 우선순위 타워 (§5) 는 별도 트랙.

### 10.5 표면 규칙 변화 요약 (사용자 결정 사항)

| # | 새 규칙 | 불허되는 것 (현행 허용) | 코퍼스 영향 |
|---|---|---|---|
| 1 | 비교 `<`/`<=`/`>`/`>=` 앞 공백 1+ (같은 줄) | `a<b` (비교 의도) | 0건 |
| 2 | GenericArgs 는 이름에 밀착 | `hello <T>(x)` | 0건 (전부 밀착) |
| 3 | `+ - * / %` 는 lhs 와 같은 줄 | `a\n+ b` continuation | 0건 (EOL 체인 4건은 유지) |
| 4 | 같은 줄 `{` 는 무조건 트레일링 람다 | (현행에서도 사실상 에러) | 0건 |

검증 게이트 (§7): 마이크로 A/B → mulang worktree 에서 `parser.generate` →
MulangCdgTest 15 예제 + NativeParserDiffTest + 실코퍼스 13 파일 전부 수락 →
jar.bbx profile_steps/time_parse.

## 11. 언어 보존성 분류와 결정 (2026-07-04)

**결정 (사용자): 언어(정의된 문법이 수락하는 language)는 바꾸지 않는다.**
"코퍼스 영향 0건"은 현존 코드가 안 깨진다는 뜻이지 언어가 안 바뀐다는 뜻이
아니므로, §10 의 안들을 언어 보존성으로 재분류하면:

| 제안 | 문법 텍스트 | accept 집합 | 기존 문자열의 AST |
|---|---|---|---|
| 1b (`{` 1자 lookahead) | 변경 | **보존** (분석상 — 갈라지는 입력은 현행에서도 에러) | 보존 |
| 1a (`<CallChain_+>` 제거) | 변경 | 보존 아님 — 늘어나는 방향 (longest 가 죽이던 짧은 해석의 완주가 새로 수락될 수 있음) | 바뀔 수 있음 (모호화 → walk 다중매치 위험) |
| 2a+ (spacing/밀착/같은-줄) | 변경 | **축소** (`a<b`, `hello <T>`, `a\n+ b` 불허) | 일부 변화 (`let x = a\n+ b`: 한 식 → 두 문장) |
| 타워 압축 | 변경 | 동일 유지 가능 | AST 스키마 변경 |

- 1a 의 "늘어나는 방향"도 엄밀한 보장은 아님 — CDG 는 Boolean 문법이라
  except 경유의 역방향 효과가 이론상 가능 (mulang 의 except 대상은
  토큰류라 실질 무관). 차등 게이트로 실측할 것.
- **왜 `<AddExpr>` (47%) 는 언어 불변으로 못 없애나**: `hello<generic>(123)`
  류 모호성은 실재하고 어느 해석이 이기는지 자체가 언어 정의의 일부다.
  현행 문법은 그 정의를 longest 로 표현하며 longest 의 런타임 의미가 곧
  "블록 끝까지 감시"라 비용이 정의에 내장돼 있다. 같은 언어를 유지한 채
  비용만 빼려면 문법이 아니라 파서 구현 (워처-main 시뮬레이션 공유 트랙,
  kernels_history_optimization.md §0.1 후보 (b)) 에서 감당해야 한다.
  C++/Rust/Kotlin 이 이 부류를 전부 언어 규칙 (turbofish, spacing 민감성)
  으로 푼 것도 같은 이유.

**언어 불변 제약 하에서 남는 실행 계획**:
1. **1b 선행 적용 후보** (워처 12%): 언어 보존 신뢰도 최고. 게이트 (§7)
   통과가 조건 — "분석상 동일"이지 증명은 아님.
2. **1a 는 차등 게이트 후 결정**: 구/신 문법 parserdata 를 나란히 만들어
   코퍼스 + 예제 + 생성 입력의 accept/AST diff (NativeParserDiffTest /
   PathsDiffTest 인프라 재활용). 델타 0 이면 채택, 아니면 그 케이스가 곧
   언어 결정 안건.
3. **2a+ 는 보류** (언어를 바꾸겠다는 결정이 있을 때만 재개). §10.1 초안은
   그 시점을 위해 유지.
4. 47% 몫의 언어-불변 대안은 파서 트랙 (워처 공유) 뿐 — 아키텍처 변경이
   커서 후순위, 필요 시 별도 설계 문서로.
