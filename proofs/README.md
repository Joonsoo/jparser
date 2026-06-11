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
| `theories/WellDefined.v` | `cdgmatch_level_indep`, `cdgmatch_witness_indep`, self-referential negation clauses; decidability stated (Admitted) | Thm 3.3 |
| `theories/CounterExample.v` | `BadG` (`B → C; C → !B`): satisfies the paper's Def 3.2 literally, admits **no** model (`badg_no_model`) | Def 3.2 fix |
| `theories/MatchFuel.v` | Executable fuel-bounded checker (test oracle; exactness future work) | — |
| `theories/Examples.v` | Verified unit tests: aⁿbⁿ, keyword exclusion (`Id - "if"`), maximal munch (`<A>`), aⁿbⁿcⁿ via join; boolean fuel tests | §3 examples |

## Status

- **Proved, axiom-free** (checked with `Print Assumptions`): span validity,
  all ten per-operator clause lemmas, oracle agreement, `nt_level_stable`,
  `eval_match_agree`, level independence, rank-witness independence, the
  self-referential negation clauses, the `BadG` no-model theorem, and all
  examples.
- **Admitted (1)**: `cdgmatch_dec` (decidability clause of Thm 3.3).
  Intended route: exactness of `match_fuel` at sufficient fuel, stratum by
  stratum. Note the original plan's `match_fuel_monotone` is false under
  negation (documented in `MatchFuel.v`).

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
