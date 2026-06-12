# Rocq Formalization Plan — 목표 B (Match Predicate Mechanization)

> **STATUS UPDATE (2026-06-11)**: **목표 B 완료.** Step 1–6의 목표가 `proofs/`에
> 구현됨 (Rocq 9.1.1, Homebrew). Theorem 3.3 전체가 **Admitted 0, axiom 0으로
> 기계화 완료**: 층화 구성(EvalN/MatchO/nt_level), 층 안정성, level/witness
> 독립성, 연산자별 clause lemma 10개(자기참조 negation clause 포함),
> decidability(`Decidability.v`: 높이 지표 매치 + 유한 우주 포화 비둘기집),
> 검증된 예제(aⁿbⁿ, keyword exclusion, longest, aⁿbⁿcⁿ join) + 실행 가능한
> fuel checker. decidability는 문법의 유한 support만 필요하고 stratification은
> 불필요함이 증명에서 드러남(층 고정 관계는 항상 결정 가능; stratification은
> 정준화 담당). 상세는 `proofs/README.md`.
>
> **STATUS UPDATE 2 (2026-06-12)**: **목표 A 착수 + Theorem 4.1 증명 완료.**
> `proofs/theories/ACP.v`: Naive ACP를 표현식 문법 위에 직접 정의(kernel =
> dotted sym, 조건 대수 = 2×2 모양, 의미론적 discharge `csem` = 연산자별
> Match 절 해석, chart = Node/Edge 포화 규칙). **Thm 4.1(soundness)을
> axiom-free로 증명** — span-soundness 불변식이 연산자별로 분해되어 각
> 조건부 케이스가 해당 Match 절 생성자 + eval_match_agree 다리로 닫힘.
> 잔여: Thm 4.2(completeness; 유일한 Admitted), phase 2(운영적 조건 진화
> ≡ 의미론적 discharge).
>
> **본 계획 대비 두 가지 설계 변경** (둘 다 계획의 결함 수정):
> 1. Step 5/6의 `match_fuel_monotone`은 negation 하에서 **거짓** — fuel 증가가
>    부정 내부의 미달 결과를 뒤집을 수 있음. 층별 exactness가 올바른 진술
>    (MatchFuel.v에 문서화).
> 2. paper Definition 3.2(negation-only)는 불충분 — `B → C; C → !B` 역설 문법을
>    허용함을 기계화로 확인 (CounterExample.v). positive 참조에 `rk B ≤ rk A`
>    조건을 추가한 수정 정의로 형식화했고 paper §3도 수정됨. 오프라인 검사기
>    (docs/scripts/mulang_stratification_check.py)는 이미 수정 정의에 부합.

> **목적**: paper §2.2의 recognitional Match predicate과 §2.3의 strict stratification을 Rocq Prover에서 형식화. paper의 §5 (parser correctness)는 후속 단계로 미룸 (목표 A).
>
> **작성**: 2026-05-10. paper 작성과 병행 진행할 단독 mechanization 작업.
>
> **Path 결정**: Path X (목표 B 먼저, 끝나면 목표 A 평가). 사용자께서 Coq/Rocq 사용 경험 있으나 깊은 숙련도 없음.
>
> **최종 목표**: paper §2가 "informal definition + Rocq formalization" 두 layer로 받쳐지는 상태.

---

## 0. 진행 원칙

1. **점진적 commit**: 각 step의 결과물을 git에 commit. 막혀도 이전 단계 보존.
2. **막히면 mitigation**: Step 5 (stratification)가 가장 위험. fuel-based fallback으로 우회 가능.
3. **Test-driven**: 각 step에 example test를 두고 `Qed` 또는 `Defined`로 닫음. 정의가 의도대로 동작하는지 즉시 검증.
4. **Paper-aligned**: Coq 정의는 paper §2의 표기와 가능한 한 1:1 대응. 변형이 필요하면 paper에 명시.
5. **Stuck threshold**: 한 단계에서 1주 이상 막히면 mitigation 적용. 2주 이상 막히면 사용자에게 보고하고 재계획.

---

## 1. 작업 위치 및 환경

### 1.1 디렉토리 구조

```
/Users/joonsoo/Documents/workspace/jparser/
├── docs/
│   ├── paper-outline.md
│   ├── rocq_formalization_plan.md  ← 본 문서
│   └── ...
└── proofs/                          ← 새로 만듦
    ├── _CoqProject
    ├── README.md
    └── theories/
        ├── Syntax.v                 ← Step 2
        ├── Match.v                  ← Step 3, 5
        ├── Examples.v               ← Step 4
        ├── Stratification.v         ← Step 5
        └── WellDefined.v            ← Step 6
```

### 1.2 Rocq Prover 사용

- 2024-2025년 Coq → Rocq Prover로 rebranding.
- 본 작업은 **Rocq 9.0+**을 가정.
- 만약 Rocq 9.0 설치 어려우면 **Coq 8.20**으로 대체 가능 (대부분 syntax 호환).

---

## Step 1 — 환경 셋업 (예상 1일)

### 1.1 Rocq 설치 (macOS)

opam을 통한 설치가 가장 안정적.

```bash
# opam 설치
brew install opam

# opam 초기화
opam init -y --bare
opam switch create rocq-paper 4.14.2  # OCaml 4.14가 Rocq와 가장 호환
eval $(opam env --switch=rocq-paper)

# Rocq Prover 설치
opam repo add rocq-released https://rocq-prover.org/opam/released
opam install -y rocq-prover

# 확인
rocq --version
```

만약 opam의 rocq-prover repo가 안정 안 되어 있으면 fallback으로 Coq:

```bash
opam install -y coq
coqc --version  # 8.20+ 확인
```

### 1.2 디렉토리 생성

```bash
cd /Users/joonsoo/Documents/workspace/jparser
mkdir -p proofs/theories
cd proofs
```

### 1.3 `_CoqProject` 작성

```coqproject
-Q theories CDG
-arg -w -arg -notation-overridden

theories/Syntax.v
theories/Match.v
theories/Examples.v
theories/Stratification.v
theories/WellDefined.v
```

### 1.4 빌드 시스템

`Makefile`을 자동 생성:

```bash
coq_makefile -f _CoqProject -o Makefile
# (Rocq의 경우)  rocq makefile -f _CoqProject -o Makefile
```

이후 `make` 또는 `make -j 4`로 컴파일.

### 1.5 Sanity check — Hello World

`theories/Hello.v` (임시):

```coq
Theorem trivial : 1 + 1 = 2.
Proof. reflexivity. Qed.
```

`make`로 컴파일 통과하면 환경 OK. 통과 후 `Hello.v` 삭제.

### 1.6 Editor 추천

- **VS Code + VsCoq extension**: 가장 쉬운 옵션.
- **Emacs + Proof General**: 전통적, 강력함.
- **CoqIDE**: bundled GUI.

처음이면 VS Code가 가장 부담 적음.

### 1.7 Step 1 완료 기준

- [ ] `rocq --version` 또는 `coqc --version` 동작.
- [ ] `proofs/` 디렉토리 생성됨.
- [ ] `_CoqProject` 작성됨.
- [ ] 임시 Hello World가 컴파일 통과.
- [ ] Editor에서 Coq 파일 syntax highlighting 동작.

### 1.8 막힐 수 있는 지점

- **opam 초기화 실패**: macOS의 일부 환경에서 opam이 sandbox 권한 문제로 막힘. 해결: `opam init --disable-sandboxing -y`.
- **Rocq vs Coq 선택 혼란**: 설치가 어려우면 Coq 8.20으로 시작. 본 plan의 모든 코드는 두 버전 모두 호환 (특별히 명시 없는 한).
- **Editor 셋업**: 첫 Coq 사용자가 가장 흔히 1-2일 잃는 지점. VS Code + VsCoq extension이 가장 부담 적음.

---

## Step 2 — Symbol 정의 (예상 2-3일)

### 2.1 목표

paper §2.1의 symbol expression을 Coq inductive type으로. `theories/Syntax.v`.

### 2.2 핵심 정의

```coq
From Coq Require Import Strings.String Strings.Ascii Lists.List Arith.Arith.
From Coq Require Import Bool.Bool.
Import ListNotations.

(** ** Nonterminal identifiers
    Nonterminals are identified by natural numbers. *)
Definition nt_id := nat.

(** ** Symbol expressions
    Paper §2.1의 symbol expression BNF. *)
Inductive sym : Type :=
  | term : ascii -> sym               (** terminal *)
  | eps : sym                         (** empty string *)
  | nts : nt_id -> sym                (** nonterminal reference *)
  | seqs : sym -> sym -> sym          (** concatenation; 'seq'는 stdlib과 충돌하므로 'seqs' *)
  | alts : sym -> sym -> sym          (** alternative *)
  | joins : sym -> sym -> sym         (** join (intersection) *)
  | excepts : sym -> sym -> sym       (** except (difference) *)
  | lookahead : sym -> sym            (** positive lookahead *)
  | lookahead_except : sym -> sym     (** negative lookahead *)
  | longest : sym -> sym.             (** longest match *)

(** ** Grammar
    A grammar is a finite map from nonterminal IDs to symbol expressions
    plus a designated start nonterminal.
    For simplicity, we use a partial function [nt_id -> option sym]. *)
Record grammar : Type := mkGrammar {
  rules : nt_id -> option sym;
  start : nt_id;
}.

(** ** Decidable equality on sym *)
Lemma sym_eq_dec : forall s1 s2 : sym, {s1 = s2} + {s1 <> s2}.
Proof.
  decide equality.
  - apply ascii_dec.
  - apply Nat.eq_dec.
Defined.
```

### 2.3 보조 함수

```coq
(** Symbol size for induction. *)
Fixpoint sym_size (s : sym) : nat :=
  match s with
  | term _ | eps | nts _ => 1
  | seqs a b | alts a b | joins a b | excepts a b => S (sym_size a + sym_size b)
  | lookahead a | lookahead_except a | longest a => S (sym_size a)
  end.
```

### 2.4 Notation 권장 (선택)

```coq
Declare Scope sym_scope.
Bind Scope sym_scope with sym.
Notation "a ;; b" := (seqs a b) (at level 60, right associativity) : sym_scope.
Notation "a ||| b" := (alts a b) (at level 70, right associativity) : sym_scope.
Notation "a &&& b" := (joins a b) (at level 65, right associativity) : sym_scope.
(* paper의 `&` `-` `^` `!` `<·>` 와 정확히 같은 표기는 Coq에서 사용 어려움 — 우회 표기. *)
```

### 2.5 Step 2 완료 기준

- [ ] `theories/Syntax.v`가 컴파일 통과.
- [ ] `sym_eq_dec` 정의 동작 (decidable equality).
- [ ] 작은 example: `Definition example_grammar : grammar := mkGrammar (fun n => match n with | 0 => Some (term "a"%char) | _ => None end) 0.` 정의 가능.

### 2.6 막힐 수 있는 지점

- `decide equality` tactic이 `ascii`에 대해 실패하면 `apply ascii_dec`을 명시.
- nonterminal id를 `nat` 대신 `string`으로 하고 싶다면 변경 가능, 다만 decidable equality가 더 까다로움.

---

## Step 3 — Match Predicate (단순 버전, 예상 3-5일)

### 3.1 목표

paper §2.2의 11개 inference rule을 Coq Inductive predicate로. **단, `excepts`/`lookahead_except`/`longest`는 negation을 포함하므로 Step 5에서 처리**. Step 3에서는 monotone 부분만.

### 3.2 핵심 정의 — `theories/Match.v`

```coq
From CDG Require Import Syntax.
From Coq Require Import Strings.String Strings.Ascii Lists.List Arith.Arith.
From Coq Require Import Lia.

(** [str] is the input as a list of characters for indexed access. *)
Definition str := list ascii.

(** Helper: nth character of a string (option). *)
Definition char_at (w : str) (i : nat) : option ascii := nth_error w i.

(** ** Match Predicate (monotone subset, no negation)
    Paper §2.2 의 11개 rule 중 negation 포함 안 한 것만. *)
Inductive Match (G : grammar) : sym -> str -> nat -> nat -> Prop :=
  | M_term : forall a w i,
      char_at w i = Some a ->
      Match G (term a) w i (S i)
  | M_eps : forall w i,
      Match G eps w i i
  | M_nt : forall A α w i j,
      rules G A = Some α ->
      Match G α w i j ->
      Match G (nts A) w i j
  | M_seq : forall α β w i k j,
      Match G α w i k ->
      Match G β w k j ->
      Match G (seqs α β) w i j
  | M_alt_l : forall α β w i j,
      Match G α w i j ->
      Match G (alts α β) w i j
  | M_alt_r : forall α β w i j,
      Match G β w i j ->
      Match G (alts α β) w i j
  | M_join : forall α β w i j,
      Match G α w i j ->
      Match G β w i j ->
      Match G (joins α β) w i j
  | M_lookahead : forall α w i k,
      Match G α w i k ->
      Match G (lookahead α) w i i.

(** Note: [excepts], [lookahead_except], [longest] not handled here.
    These require negation and are addressed in Step 5 (Stratification.v). *)
```

### 3.3 보조 정리 (간단한 것부터)

```coq
(** [Match] preserves [i <= j] for non-lookahead rules.
    Lookahead의 경우 `i = j` (zero-width). *)
Lemma match_index_bounded : forall G s w i j,
  Match G s w i j -> i <= j.
Proof.
  intros G s w i j H.
  induction H; try lia.
Qed.

(** End index does not exceed string length. *)
Lemma match_end_within : forall G s w i j,
  Match G s w i j -> j <= length w.
Proof.
  intros G s w i j H.
  induction H; try (apply IHMatch); try lia.
  - apply nth_error_Some_lt in H. lia.  (* term case *)
  - assert (k <= length w) by assumption. lia.
  Admitted.  (* 처음에는 Admitted로 두고 나중에 채움 *)
```

> **Tip**: 처음에는 모든 보조 정리를 `Admitted.`로 두고 main 정의가 type-check 되는지 확인. 그 후 하나씩 `Qed.`로 채움. 이게 Rocq 첫 사용자에게 자신감 주는 방식.

### 3.4 Step 3 완료 기준

- [ ] `theories/Match.v`가 컴파일 통과.
- [ ] `Match` predicate 정의가 11개 rule 중 8개 (negation 제외) 포함.
- [ ] 보조 정리 `match_index_bounded` 증명 (또는 Admitted).
- [ ] `match_end_within` Admitted 또는 증명.

### 3.5 막힐 수 있는 지점

- `nth_error_Some_lt` 같은 stdlib lemma 이름이 버전마다 다름. Coq 8.20: `nth_error_Some`. Rocq 9.x: 동일. 모르면 `Search nth_error.`로 찾기.
- `induction H; try lia.` 가 자동으로 풀어주지 않는 case는 명시적으로 다뤄야 함.

---

## Step 4 — Unit Tests (예상 3-5일)

### 4.1 목표

paper §2.4의 motivating example들을 Coq에서 직접 evaluate. paper 정의가 의도대로 동작하는 것을 mechanically 확인.

### 4.2 Example 1 — `aⁿ bⁿ` (CFL)

`theories/Examples.v`:

```coq
From CDG Require Import Syntax Match.

(** Grammar:
    S → ε | a S b
    Nonterminal id 0 = S. *)
Definition AnBn : grammar :=
  mkGrammar
    (fun n =>
      match n with
      | 0 => Some (alts eps (seqs (term "a"%char) (seqs (nts 0) (term "b"%char))))
      | _ => None
      end)
    0.

(** Test: empty string matches. *)
Example match_empty : Match AnBn (nts 0) [] 0 0.
Proof.
  apply M_nt with (α := alts eps (seqs (term "a"%char) (seqs (nts 0) (term "b"%char)))).
  - reflexivity.
  - apply M_alt_l. apply M_eps.
Qed.

(** Test: "ab" matches. *)
Example match_ab : Match AnBn (nts 0) ["a"%char; "b"%char] 0 2.
Proof.
  apply M_nt with (α := alts eps (seqs (term "a"%char) (seqs (nts 0) (term "b"%char)))).
  - reflexivity.
  - apply M_alt_r.
    apply M_seq with (k := 1).
    + apply M_term. reflexivity.
    + apply M_seq with (k := 1).
      * apply M_nt with (α := alts eps (seqs (term "a"%char) (seqs (nts 0) (term "b"%char)))).
        -- reflexivity.
        -- apply M_alt_l. apply M_eps.
      * apply M_term. reflexivity.
Qed.
```

### 4.3 Example 2 — Lookahead

```coq
(** Grammar with lookahead:
    S → ^a · b · c
    "ab" 시작인 input은 매치되지 않음 (lookahead가 'a'를 요구하나 다음 토큰이 b·c여야 함을 모순).
    Wait, this example needs reframe. Let's pick:

    S → ^(a b) · a b
    즉 "ab"로 시작하는 substring을 lookahead로 확인 후 실제로 "ab" 매치. *)

Definition LookaheadAB : grammar :=
  mkGrammar
    (fun n =>
      match n with
      | 0 => Some (seqs
                    (lookahead (seqs (term "a"%char) (term "b"%char)))
                    (seqs (term "a"%char) (term "b"%char)))
      | _ => None
      end)
    0.

Example match_lookahead_ab : Match LookaheadAB (nts 0) ["a"%char; "b"%char] 0 2.
Proof.
  apply M_nt with (α := seqs (lookahead (seqs (term "a"%char) (term "b"%char)))
                              (seqs (term "a"%char) (term "b"%char))).
  - reflexivity.
  - apply M_seq with (k := 0).
    + apply M_lookahead with (k := 2).
      apply M_seq with (k := 1); apply M_term; reflexivity.
    + apply M_seq with (k := 1); apply M_term; reflexivity.
Qed.
```

### 4.4 Tactic 자동화

위 증명들이 길고 반복적이므로 helper tactic 만들기:

```coq
Ltac match_term := apply M_term; reflexivity.
Ltac match_eps := apply M_eps.
Ltac match_nt := match goal with
                 | [|- Match _ (nts ?n) _ _ _] =>
                   apply (M_nt _ n); [reflexivity | ]
                 end.

(* 위 example들을 더 짧게 다시 써보기 *)
```

### 4.5 Step 4 완료 기준

- [ ] `aⁿ bⁿ` grammar에 대해 4-5개 example 성공/실패 테스트.
- [ ] Lookahead 사용 example 1개.
- [ ] Join 사용 example 1개 (예: `(a* b* & a* b*) → a* b*` 자명한 경우).
- [ ] paper §2.2 정의가 의도대로 동작함을 본인 눈으로 확인.

### 4.6 막힐 수 있는 지점

- 증명이 너무 verbose. helper tactic으로 단축.
- `apply M_seq with (k := 1)` 같은 explicit witness가 필요한 경우 → tactic으로 자동화 어려움. 처음엔 manual.

---

## Step 5 — Stratification + Negation (예상 1-2주, 가장 어려운 단계)

### 5.1 목표

paper §2.3의 strict stratification을 Coq에 정의하고, `excepts` / `lookahead_except` / `longest`를 추가한 Match를 stratum-by-stratum으로 정의.

### 5.2 두 가지 접근법

#### Approach 5A — Strong induction on rank (paper §2.3과 정합)

```coq
(** Rank function: nonterminal -> nat. *)
Definition rank (G : grammar) (A : nt_id) : nat := (* user provided *).

(** Strict stratified condition. *)
Definition strict_stratified (G : grammar) : Prop :=
  forall A α B,
    rules G A = Some α ->
    in_negation_arg α B ->
    rank G B < rank G A.

(** Match defined recursively on rank. *)
Fixpoint match_at_rank (max_rank : nat) ... { struct max_rank } : ... := ...
```

장점: paper 정의와 1:1.
단점: Coq fixpoint termination 증명이 까다로움.

#### Approach 5B (Mitigation) — Fuel-based fixpoint (단순, 빠름)

```coq
Fixpoint match_with_fuel (fuel : nat) (G : grammar) (s : sym) (w : str) (i j : nat) : bool :=
  match fuel with
  | 0 => false  (* fuel 부족, 결과 미정 *)
  | S k =>
    match s with
    | term a => andb (Nat.eqb j (S i))
                     (match char_at w i with Some a' => ascii_eqb a a' | None => false end)
    | eps => Nat.eqb i j
    | nts A =>
      match rules G A with
      | None => false
      | Some α => match_with_fuel k G α w i j
      end
    | seqs α β =>
      existsb (fun mid => andb (match_with_fuel k G α w i mid)
                               (match_with_fuel k G β w mid j))
              (seq i (S (j - i)))
    | alts α β => orb (match_with_fuel k G α w i j)
                      (match_with_fuel k G β w i j)
    | joins α β => andb (match_with_fuel k G α w i j)
                        (match_with_fuel k G β w i j)
    | excepts α β => andb (match_with_fuel k G α w i j)
                          (negb (match_with_fuel k G β w i j))
    | lookahead α =>
      andb (Nat.eqb i j)
           (existsb (fun e => match_with_fuel k G α w i e)
                    (seq i (S (length w - i))))
    | lookahead_except α =>
      andb (Nat.eqb i j)
           (forallb (fun e => negb (match_with_fuel k G α w i e))
                    (seq i (S (length w - i))))
    | longest α =>
      andb (match_with_fuel k G α w i j)
           (forallb (fun e => negb (match_with_fuel k G α w i e))
                    (seq (S j) (length w - j)))
    end
  end.
```

장점:
- Coq에서 직접 `Fixpoint`로 정의 가능 (fuel decreases each call).
- Decidability가 자명 (boolean function).
- Negation 처리가 단순 (`negb`).

단점:
- paper §2.3의 strict stratification과 직접 대응 안 됨.
- "Match가 fuel-independent하다"는 보조 정리 필요 (충분히 큰 fuel이면 결과가 안정).

### 5.3 권장: Approach 5B 먼저, Approach 5A는 부록

처음 시작이시면 5B로 빠르게 가는 게 좋습니다. 이유:
- 1주 안에 동작하는 코드.
- paper §2.3 stratification과의 등가성은 별도 informal lemma로 적기.
- §5에서 parser correctness 증명할 때 이 boolean version과 inductive version 사이 가교를 informally 또는 별도 mechanize.

### 5.4 Approach 5B 완료 기준

- [ ] `match_with_fuel` 정의 컴파일 통과.
- [ ] AnBn grammar에 대해 `Compute (match_with_fuel 100 AnBn (nts 0) ["a";"b"] 0 2).` 가 `true` 반환.
- [ ] Except 사용 grammar example 동작.
- [ ] Longest 사용 grammar example 동작.

### 5.5 Approach 5B에서 Approach 5A로 (선택)

만약 5B가 잘 동작하고 paper §5에 더 정밀한 의미론이 필요하면, 5A로 마이그레이션. 그러나 본 plan의 목표 B에는 5B로 충분.

### 5.6 막힐 수 있는 지점

- **Fuel-based의 `seq` 함수가 stdlib에 있는지 확인**: `Coq.Lists.List.seq : nat -> nat -> list nat`.
- **Boolean function이 너무 느려서 evaluate 안 됨**: 작은 example만 테스트. 큰 grammar는 성능 안 봐도 됨 (verification 목적).

---

## Step 6 — Match Well-Definedness 정리 (예상 3-5일)

### 6.1 목표

paper §2.3.3의 Theorem 2.1을 Coq에 적기. **strict stratified grammar에 대해 Match가 well-defined**라는 진술.

### 6.2 진술 (Approach 5B 사용 시)

```coq
(** Match가 fuel-independent함을 보임:
    충분히 큰 fuel이면 결과가 안정. *)
Theorem match_fuel_monotone : forall fuel1 fuel2 G s w i j,
  fuel1 <= fuel2 ->
  match_with_fuel fuel1 G s w i j = true ->
  match_with_fuel fuel2 G s w i j = true.

(** Strict stratified grammar에 대해 충분 fuel이 존재. *)
Theorem match_stable : forall G s w i j,
  strict_stratified G ->
  exists fuel,
    forall fuel', fuel <= fuel' ->
      match_with_fuel fuel G s w i j = match_with_fuel fuel' G s w i j.
```

이 두 정리를 증명하면 "fuel을 충분히 크게 잡으면 Match는 well-defined"라는 것을 mechanize.

### 6.3 진술 (Approach 5A 사용 시 — 더 paper-aligned)

```coq
Theorem match_decidable : forall G s w i j,
  strict_stratified G ->
  {Match G s w i j} + {~ Match G s w i j}.
```

### 6.4 Step 6 완료 기준

- [ ] Approach 5B: `match_fuel_monotone` 증명 (또는 Admitted).
- [ ] `match_stable` 증명 sketch.
- [ ] paper §2.3.3 Theorem 2.1의 statement가 Coq에서 적힘.

### 6.5 막힐 수 있는 지점

- Fuel-based의 monotonicity 증명은 Match의 모든 case에 대해 induction 필요. 길지만 mechanical.
- `match_stable`은 grammar의 size에 의존하는 fuel bound를 명시해야 함. 이게 까다로움 — 일단 `Admitted`로 두고 paper에는 informal 정당화로.

---

## Step 7 — (선택) Boolean Grammar와의 등가성 statement

### 7.1 목표

paper §2.5의 정리 2.3을 Coq statement로만 적기. 증명은 `Admitted`.

```coq
Theorem CDG_BG_equivalent_on_join_except : forall G,
  uses_only_join_except G ->
  forall s w,
    Match G (nts (start G)) w 0 (length w) <->
    in_boolean_grammar_lang G w.
Admitted.  (* 증명은 future work *)
```

이걸 적기만 해도 paper §2.5에서 "정리는 Coq statement로 적었으나 증명은 informal" 이라 reporting 가능.

---

## 전체 일정 요약

| Step | 내용 | 예상 작업량 | Cumulative |
|---|---|---|---|
| 1 | 환경 셋업 | 1일 | 1일 |
| 2 | Symbol 정의 | 2-3일 | 3-4일 |
| 3 | Match 단순 버전 | 3-5일 | 6-9일 |
| 4 | Unit tests | 3-5일 | 9-14일 |
| 5 | Stratification + negation | 1-2주 | 16-28일 |
| 6 | Well-Definedness 정리 | 3-5일 | 19-33일 |
| 7 | (선택) BG 등가성 statement | 1-2일 | 20-35일 |

**총 예상**: 1.5-2개월. 처음 사용자 기준 (Coq 능숙자라면 절반).

---

## 진행 추적 체크리스트

### Step 1
- [ ] opam 설치
- [ ] Rocq Prover 설치
- [ ] `proofs/` 디렉토리 생성
- [ ] `_CoqProject` 작성
- [ ] Hello World 컴파일

### Step 2
- [ ] `Syntax.v`: sym inductive
- [ ] `Syntax.v`: grammar record
- [ ] `Syntax.v`: sym_eq_dec
- [ ] `Syntax.v` 컴파일

### Step 3
- [ ] `Match.v`: Match inductive (8 rules, no negation)
- [ ] `Match.v`: match_index_bounded
- [ ] `Match.v` 컴파일

### Step 4
- [ ] `Examples.v`: AnBn grammar
- [ ] `Examples.v`: 4-5 examples
- [ ] `Examples.v`: lookahead example
- [ ] `Examples.v`: join example
- [ ] `Examples.v` 컴파일

### Step 5
- [ ] `Stratification.v`: strict_stratified definition
- [ ] `Match.v` 또는 `Stratification.v`: match_with_fuel (Approach 5B)
- [ ] `Stratification.v`: examples with except, longest, lookahead_except
- [ ] 컴파일

### Step 6
- [ ] `WellDefined.v`: match_fuel_monotone (or Admitted)
- [ ] `WellDefined.v`: match_stable statement
- [ ] paper §2.3.3 Theorem 2.1 statement in Coq

### Step 7 (선택)
- [ ] `WellDefined.v`: CDG_BG_equivalent_on_join_except statement (Admitted)

---

## 막혔을 때 escalation procedure

1. **30분 막힘**: `Search` tactic으로 비슷한 lemma 찾기. stdlib 익숙해지기.
2. **반나절 막힘**: 해당 부분 단순화. complex induction 대신 case analysis로 회피 시도.
3. **1일 막힘**: `Admitted`로 두고 다음 step으로. 나중에 돌아오기.
4. **3일 막힘**: 본 plan의 mitigation 적용. Approach 5B로 fallback. 또는 negation 부분만 future work으로 미룸.
5. **1주 막힘**: 사용자에게 보고. 재계획.

---

## paper와의 연결

각 Step이 paper의 어느 부분을 받치는지:

- Step 2 → paper §2.1 (Syntax). "Coq mechanization of CDG syntax."
- Step 3, 5 → paper §2.2 (Match Predicate). "Coq Inductive definition of Match."
- Step 6 → paper §2.3 (Well-formedness). "Theorem 2.1 mechanized."
- Step 4 → paper §2.4 (Expressiveness examples). "Mechanically verified examples."
- Step 7 → paper §2.5 (Generative-Recognitional equivalence). "Statement mechanized, proof informal."

목표 B 완료 시 paper §2 전체가 "informal definition + Coq formalization" 두 layer 갖춤. **§5 (parser correctness)는 future work으로 명시**.

---

## 다음 작업 후보 — 목표 B 완료 후

목표 B 끝나면 다음 단계:

- **목표 A**: §5 verified parser. Naive ACP의 알고리즘을 Coq Fixpoint로 정의 + soundness/completeness 증명. 6-12개월.
- **Polarity-aware stratification**: 더 너그러운 well-formedness. mulang.cdg의 226/228 → 228/228 가능. paper future work에서 contribution으로 격상 가능.
- **Generator correctness**: milestone parser의 generator가 정확한 table을 만든다는 증명.

이건 목표 B 완료 후 진행 상황 보고 결정.

---

## 참고 자료 (Rocq 학습)

- **Software Foundations** (`https://softwarefoundations.cis.upenn.edu/`): Coq의 standard tutorial. Logical Foundations 권만 봐도 충분.
- **Certified Programming with Dependent Types** (Chlipala): Coq advanced tutorial.
- **Coq Reference Manual**: `https://coq.inria.fr/refman/`.
- **Rocq Prover**: `https://rocq-prover.org/`.
- **Coq stdlib search**: VS Code의 VsCoq에서 `Search` tactic.

---

## 작성 메모

- 본 plan은 사용자께서 Coq 처음은 아니나 깊은 숙련도 없는 상태를 가정.
- 각 step의 예상 작업량은 처음 사용자 기준. 익숙해지면 절반.
- Mitigation들은 paper 작성 데드라인이 6-9개월일 때 합리적.
- Step 5의 Approach 5B (fuel-based)가 paper의 정밀도를 약간 희생하는 것은 사실이나, **paper §2의 mechanization evidence로는 충분**. paper §5 (verified parser)로 가면 그때 더 정밀한 정의로 마이그레이션.
