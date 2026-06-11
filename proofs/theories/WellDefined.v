(** * WellDefined: the mechanized content of paper Theorem 3.3.

    For a strict stratified grammar (in the CORRECTED sense of
    [Stratification.strict_stratified]; see [CounterExample.v] for why
    the paper's Definition 3.2 must be corrected), the match relation

      [CDGMatch G R]   (= [MatchO G (nt_level G R)])

    is canonical in every sense the paper claims:

    - existence/clause satisfaction: it satisfies the defining clauses of
      the span semantics, including the self-referential negation clauses
      ([cdgmatch_except_clause], [cdgmatch_nla_clause],
      [cdgmatch_longest_clause]); the positive clauses hold at any fixed
      oracle and are proved per operator in [Match.v] ([Section Clauses]).
    - level independence: any sufficiently large level gives the same
      relation ([cdgmatch_level_indep]).
    - witness independence: the relation does not depend on which rank
      function witnesses stratification ([cdgmatch_witness_indep]); this
      is the paper's "independent of the choice of [≺]".

    NOTE ON UNIQUENESS.  Paper Theorem 3.3 says "there is a unique span
    predicate satisfying the clauses".  Read literally (any relation
    closed under the clause biconditionals), uniqueness is FALSE already
    for the context-free fragment: for [S -> S | a], a relation that
    declares [S] to match everywhere also satisfies the clause
    biconditionals.  The intended reading — and what we mechanize — is
    that the clauses determine a unique relation under the stratified
    constructive interpretation: least fixed point within each stratum,
    lower strata resolved before negation is consulted.  The paper text
    of Theorem 3.3 should be adjusted to say this explicitly. *)

From Stdlib Require Import Ascii String List Arith Lia.
Import ListNotations.
From CDG Require Import Syntax Stratification Match Agreement.

(** ** Level independence *)

Theorem cdgmatch_level_indep :
  forall G rk R1 R2,
    strict_stratified G rk ->
    forall s,
      (forall B, In B (nts_of s) -> rk B < R1) ->
      (forall B, In B (nts_of s) -> rk B < R2) ->
      forall w i j, CDGMatch G R1 s w i j <-> CDGMatch G R2 s w i j.
Proof.
  intros G rk R1 R2 Hstrat s H1 H2 w i j.
  unfold CDGMatch.
  apply (match_oracle_agree G rk (Nat.min R1 R2) (nt_level G R1) (nt_level G R2) Hstrat).
  - intros C HC w' i' j'.
    apply (nt_level_stable G rk Hstrat (rk C) C (le_n _)); lia.
  - intros B HB.
    pose proof (H1 B HB). pose proof (H2 B HB). lia.
  - intros B HB. apply neg_nts_sub in HB.
    pose proof (H1 B HB). pose proof (H2 B HB). lia.
Qed.

(** ** Witness independence

    The relation does not depend on the stratification witness: two rank
    functions give the same relation at their respective sufficient
    levels.  The proof goes through a common higher level; note that
    [nt_level] itself never mentions a rank function, which is what makes
    this a corollary rather than a separate construction. *)

Theorem cdgmatch_witness_indep :
  forall G rk1 rk2 R1 R2,
    strict_stratified G rk1 ->
    strict_stratified G rk2 ->
    forall s,
      (forall B, In B (nts_of s) -> rk1 B < R1) ->
      (forall B, In B (nts_of s) -> rk2 B < R2) ->
      forall w i j, CDGMatch G R1 s w i j <-> CDGMatch G R2 s w i j.
Proof.
  intros G rk1 rk2 R1 R2 Hs1 Hs2 s Hb1 Hb2 w i j.
  apply iff_trans with (B := CDGMatch G (Nat.max R1 R2) s w i j).
  - apply (cdgmatch_level_indep G rk1); auto.
    intros B HB. pose proof (Hb1 B HB). lia.
  - apply (cdgmatch_level_indep G rk2); auto.
    intros B HB. pose proof (Hb2 B HB). lia.
Qed.

(** ** Self-referential negation clauses

    At a sufficient level the negated operands can be read with the match
    relation itself, recovering the paper's clause forms.  Together with
    the positive clause lemmas of [Match.v] this is the "existence" half
    of Theorem 3.3: [CDGMatch] satisfies all defining clauses of the CDG
    span semantics, operator by operator. *)

Theorem cdgmatch_except_clause :
  forall G rk R,
    strict_stratified G rk ->
    forall a b,
      (forall B, In B (nts_of b) -> rk B < R) ->
      forall w i j,
        CDGMatch G R (s_except a b) w i j <->
        (CDGMatch G R a w i j /\ ~ CDGMatch G R b w i j).
Proof.
  intros G rk R Hstrat a b Hnb w i j.
  unfold CDGMatch.
  split.
  - intro H. apply match_except_iff in H. destruct H as [Ha Hb]. split.
    + exact Ha.
    + intro Hc. apply Hb.
      exact (proj2 (eval_match_agree G rk R Hstrat b Hnb w i j) Hc).
  - intros [Ha Hb]. apply match_except_iff. split.
    + exact Ha.
    + intro Hc. apply Hb.
      exact (proj1 (eval_match_agree G rk R Hstrat b Hnb w i j) Hc).
Qed.

Theorem cdgmatch_nla_clause :
  forall G rk R,
    strict_stratified G rk ->
    forall a,
      (forall B, In B (nts_of a) -> rk B < R) ->
      forall w i j,
        CDGMatch G R (s_nla a) w i j <->
        (j = i /\ i <= length w /\ (forall k, ~ CDGMatch G R a w i k)).
Proof.
  intros G rk R Hstrat a Hna w i j.
  unfold CDGMatch.
  split.
  - intro H. apply match_nla_iff in H. destruct H as [-> [Hle Hk]].
    split; [reflexivity|]. split; [exact Hle|].
    intros k Hc. apply (Hk k).
    exact (proj2 (eval_match_agree G rk R Hstrat a Hna w i k) Hc).
  - intros [-> [Hle Hk]]. apply match_nla_iff.
    split; [reflexivity|]. split; [exact Hle|].
    intros k Hc. apply (Hk k).
    exact (proj1 (eval_match_agree G rk R Hstrat a Hna w i k) Hc).
Qed.

Theorem cdgmatch_longest_clause :
  forall G rk R,
    strict_stratified G rk ->
    forall a,
      (forall B, In B (nts_of a) -> rk B < R) ->
      forall w i j,
        CDGMatch G R (s_longest a) w i j <->
        (CDGMatch G R a w i j /\ (forall k, j < k -> ~ CDGMatch G R a w i k)).
Proof.
  intros G rk R Hstrat a Hna w i j.
  unfold CDGMatch.
  split.
  - intro H. apply match_longest_iff in H. destruct H as [Ha Hk].
    split; [exact Ha|].
    intros k Hlt Hc. apply (Hk k Hlt).
    exact (proj2 (eval_match_agree G rk R Hstrat a Hna w i k) Hc).
  - intros [Ha Hk]. apply match_longest_iff.
    split; [exact Ha|].
    intros k Hlt Hc. apply (Hk k Hlt).
    exact (proj1 (eval_match_agree G rk R Hstrat a Hna w i k) Hc).
Qed.

(** ** Decidability

    The last clause of paper Theorem 3.3 — decidability of [Match] on
    finite inputs — is proved in [Decidability.v] ([cdgmatch_dec]).  Its
    hypotheses are weaker than one might expect: finite support of the
    grammar suffices, and stratification is not needed at a fixed level.
    Stratification is what makes the levels stabilize into the canonical
    [Match] (the theorems above), at which point [Decidability.cdgmatch_dec]
    decides it. *)
