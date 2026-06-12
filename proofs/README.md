# CDG Mechanization (Rocq)

Rocq formalization of the CDG span semantics and strict stratification,
backing Section 3 of the paper ("목표 B" of
`docs/rocq_formalization_plan.md`). Built with Rocq 9.1.1 (Homebrew).

```sh
coq_makefile -f _CoqProject -o Makefile   # once
make -j4
```

## Files

| File | Contents | Paper |
|---|---|---|
| `theories/Syntax.v` | Symbol expressions, grammars, nonterminal/negation-position occurrence (`nts_of`, `neg_nts`) | §3 syntax, Def 3.1 |
| `theories/Stratification.v` | `strict_stratified` (rank-function form, **corrected** — see below), `rank_bound` | Def 3.2 |
| `theories/Match.v` | `EvalN` (structural evaluator with NT oracle), `MatchO` (inductive match, negation via `EvalN`), `nt_level` (stratum iteration), `CDGMatch`, span validity, per-operator clause lemmas | §3 Match clauses |
| `theories/Agreement.v` | Oracle-agreement lemmas, `nt_level_stable` (stability of the stratified construction), `eval_match_agree` | Thm 3.3 (core) |
| `theories/WellDefined.v` | `cdgmatch_level_indep`, `cdgmatch_witness_indep`, self-referential negation clauses | Thm 3.3 |
| `theories/Decidability.v` | `cdgmatch_dec`: decidability via height-indexed match + finite-universe saturation (counting pigeonhole); `matchb`, `matchb_dec`, `stab_complete`, `eval_dec` | Thm 3.3 (decidability) |
| `theories/CounterExample.v` | `BadG` (`B → C; C → !B`): satisfies the paper's Def 3.2 literally, admits **no** model (`badg_no_model`) | Def 3.2 fix |
| `theories/ACP.v` | **목표 A phase 1**: Naive ACP over expression syntax — kernels (dotted `sym`), accept-condition syntax (2×2 shapes), semantic discharge `csem` (per-operator Match clauses), chart (`Node`/`Edge` saturation rules); **Theorem 4.1 (soundness) PROVED axiom-free** via the span-soundness invariant `node_sound_mut` (PrefixMatch + per-operator condition adequacy); Thm 4.2 (completeness) stated (the only Admitted) | Thm 4.1/4.2 |
| `theories/MatchFuel.v` | Executable fuel-bounded checker (test oracle; exactness future work) | — |
| `theories/Examples.v` | Verified unit tests: aⁿbⁿ, keyword exclusion (`Id - "if"`), maximal munch (`<A>`), aⁿbⁿcⁿ via join; boolean fuel tests | §3 examples |

## Status

**Theorem 3.3 (목표 B) fully mechanized: zero `Admitted`, zero axioms**
(checked with `Print Assumptions`). This covers: span validity, all ten
per-operator clause lemmas, oracle agreement, `nt_level_stable`,
`eval_match_agree`, level independence, rank-witness independence, the
self-referential negation clauses, decidability (`cdgmatch_dec`), the
`BadG` no-model theorem, and all examples.

Notable refinement surfaced by the decidability proof: **decidability
needs only finite support of the grammar** (a list of its defined
nonterminals) and holds for every level of the iteration; stratification
plays no role in it. Stratification is exactly what makes the levels
stabilize into the canonical `Match`.

**목표 A (in progress)**: `ACP.v` mechanizes the Naive ACP reference
algorithm against this semantics — phase 1 discharges conditions
semantically (each shape interpreted by its Match clause), isolating
chart correctness from the operational condition-evolution mechanism
(phase 2). **Theorem 4.1 (soundness) is proved, axiom-free**: the
span-soundness invariant (`node_sound_mut`) decomposes per operator,
each conditional case closing with the corresponding Match clause
constructor plus the `eval_match_agree` bridge. Remaining: Theorem 4.2
(completeness, Earley-style induction over the semantics — the only
`Admitted` in the development) and phase 2 (operational condition
evolution ≡ semantic discharge). No pruning in the reference algorithm
(it is an optimization of the implementations).

## Findings that require paper changes

1. **Definition 3.2 is insufficient as stated.** Constraining only
   negation positions admits `B → C; C → !B` (rk B=0, rk C=1), which has
   no model — mechanized in `CounterExample.v`. The corrected definition
   adds the standard positive condition: every nonterminal occurring in a
   production body has rank `<=` the head's rank; negation positions rank
   strictly below. (Standard stratified negation, as in logic programming.)
2. **Theorem 3.3's uniqueness phrasing.** "The unique predicate satisfying
   the clauses" is false read literally (for `S → S | a`, declaring `S` to
   match everywhere also satisfies the clause biconditionals). The
   mechanized canonicity statement is: the stratified constructive
   interpretation (least fixed point per stratum) is independent of both
   the iteration level and the choice of stratification witness.
3. **Representation note.** The paper's abstract well-founded order is
   mechanized as a `nat`-valued rank function; equivalent for finite
   grammars, worth a remark in the paper's appendix.
4. **Follow-up for the implementation**: check whether the metalang
   analyzer's stratification check enforces the positive condition, and
   whether the documented mulang violations are classified under the
   corrected definition.
