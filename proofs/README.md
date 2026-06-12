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
| `theories/ACP.v` | **목표 A phase 1 COMPLETE**: Naive ACP over expression syntax — kernels (dotted `sym`), accept-condition syntax (2×2 shapes), semantic discharge `csem` (per-operator Match clauses), chart (`Node`/`Edge`, edges point at initials, Progress pairs an edge with the completed match); **Theorems 4.1 AND 4.2 PROVED axiom-free** (`node_sound_mut` span-soundness invariant; `node_complete` Earley-style completeness) | Thm 4.1/4.2 |
| `theories/Evolution.v` | **목표 A phase 2**: operational condition evolution — `evolve_step` (watcher absorb-and-carry), csem preservation, expiry beyond input end (`watchers_ge`), boolean `final_eval`, end-to-end `op_eval_csem`/`op_accept_correct` (executable via `cdgmatch_dec`); phase 2b abstract: `evolve_step_pruned` with the `dead_sound` oracle (the exact invariant a sound pruning must maintain) | fig:acp-evolution |
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
(phase 2). **Both Theorem 4.1 (soundness) and Theorem 4.2
(completeness) are proved, axiom-free** — the development has ZERO
`Admitted` overall. Soundness is the span-soundness invariant
(`node_sound_mut`), decomposing per operator, each conditional case
closing with the corresponding Match clause constructor plus the
`eval_match_agree` bridge; completeness (`node_complete`) is an
Earley-style induction over the match derivation. Notable: attempting
completeness exposed a definition bug in the first chart (no edges to
finished terminals), fixed by reformulating Progress to pair an edge
to an initial kernel with the corresponding final kernel — also a
simpler presentation than edge inheritance. **Phase 2 (Evolution.v)**: the operational
condition evolution of the paper's evolution figure is mechanized —
each generation step preserves the semantics, watchers expire beyond
the input end where the vacuous/failing reading is sound (span
validity), and the boolean final evaluation composed with the
decidability procedure decides acceptance exactly (`op_accept_correct`).
Early resolution under pruning is proved against an abstract dead-root
oracle (`dead_sound` — precisely the invariant violated by the trimming
bugs fixed in the implementations). Remaining toward the
implementations: per-kernel condition joins with recursion into
looked-up condition trees, and a concrete reachability-based trimming
satisfying `dead_sound`.

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
