(** * Agreement: stability of the stratified construction.

    This file contains the technical heart of paper Theorem 3.3
    (well-definedness of [Match] for strict stratified CDG):

    - [eval_oracle_agree]: [EvalN] only depends on the oracle's values at
      nonterminals occurring in the expression.
    - [match_oracle_agree]: [MatchO] only depends on the oracle's values
      at nonterminals below the current stratum, provided the expression
      respects the stratification.
    - [nt_level_stable]: the stratum iteration [nt_level] is stable: a
      nonterminal of rank [r] receives its final value at every level
      above [r].  This is the constructive "induction on stratification
      rank" of the paper's proof sketch.
    - [eval_match_agree]: at a sufficient level, the structural evaluator
      used for negated operands coincides with the inductive match
      relation, which lets the negation clauses be read self-referentially
      as in the paper. *)

From Stdlib Require Import Ascii String List Arith Lia Wf_nat.
Import ListNotations.
From CDG Require Import Syntax Stratification Match.

(** ** Oracle agreement for the structural evaluator *)

Lemma eval_oracle_agree :
  forall (rk : nt_id -> nat) (rho : nat) (N1 N2 : nt_rel),
    (forall C, rk C < rho -> forall w i j, N1 C w i j <-> N2 C w i j) ->
    forall s,
      (forall B, In B (nts_of s) -> rk B < rho) ->
      forall w i j, EvalN N1 s w i j <-> EvalN N2 s w i j.
Proof.
  intros rk rho N1 N2 Hag s.
  induction s; simpl; intros Hnts w i j.
  - (* term *) tauto.
  - (* eps *) tauto.
  - (* nt *) apply Hag, Hnts; left; reflexivity.
  - (* seq *)
    assert (Ha : forall B, In B (nts_of s1) -> rk B < rho)
      by (intros B HB; apply Hnts, in_or_app; left; exact HB).
    assert (Hb : forall B, In B (nts_of s2) -> rk B < rho)
      by (intros B HB; apply Hnts, in_or_app; right; exact HB).
    split; intros [k [H1 H2]]; exists k.
    + split; [exact (proj1 (IHs1 Ha w i k) H1)
             | exact (proj1 (IHs2 Hb w k j) H2)].
    + split; [exact (proj2 (IHs1 Ha w i k) H1)
             | exact (proj2 (IHs2 Hb w k j) H2)].
  - (* alt *)
    assert (Ha : forall B, In B (nts_of s1) -> rk B < rho)
      by (intros B HB; apply Hnts, in_or_app; left; exact HB).
    assert (Hb : forall B, In B (nts_of s2) -> rk B < rho)
      by (intros B HB; apply Hnts, in_or_app; right; exact HB).
    split; (intros [H|H]; [left|right]).
    + exact (proj1 (IHs1 Ha w i j) H).
    + exact (proj1 (IHs2 Hb w i j) H).
    + exact (proj2 (IHs1 Ha w i j) H).
    + exact (proj2 (IHs2 Hb w i j) H).
  - (* join *)
    assert (Ha : forall B, In B (nts_of s1) -> rk B < rho)
      by (intros B HB; apply Hnts, in_or_app; left; exact HB).
    assert (Hb : forall B, In B (nts_of s2) -> rk B < rho)
      by (intros B HB; apply Hnts, in_or_app; right; exact HB).
    split; intros [H1 H2]; split.
    + exact (proj1 (IHs1 Ha w i j) H1).
    + exact (proj1 (IHs2 Hb w i j) H2).
    + exact (proj2 (IHs1 Ha w i j) H1).
    + exact (proj2 (IHs2 Hb w i j) H2).
  - (* except *)
    assert (Ha : forall B, In B (nts_of s1) -> rk B < rho)
      by (intros B HB; apply Hnts, in_or_app; left; exact HB).
    assert (Hb : forall B, In B (nts_of s2) -> rk B < rho)
      by (intros B HB; apply Hnts, in_or_app; right; exact HB).
    split; intros [H1 H2]; split.
    + exact (proj1 (IHs1 Ha w i j) H1).
    + intro Hc; apply H2; exact (proj2 (IHs2 Hb w i j) Hc).
    + exact (proj2 (IHs1 Ha w i j) H1).
    + intro Hc; apply H2; exact (proj1 (IHs2 Hb w i j) Hc).
  - (* la *)
    split; intros [-> [k Hk]]; (split; [reflexivity|]); exists k.
    + exact (proj1 (IHs Hnts w i k) Hk).
    + exact (proj2 (IHs Hnts w i k) Hk).
  - (* nla *)
    split; intros [-> [Hle Hk]]; (split; [reflexivity|]); (split; [exact Hle|]);
      intros k Hc; apply (Hk k).
    + exact (proj2 (IHs Hnts w i k) Hc).
    + exact (proj1 (IHs Hnts w i k) Hc).
  - (* longest *)
    split; intros [H1 Hk]; split.
    + exact (proj1 (IHs Hnts w i j) H1).
    + intros k Hlt Hc; apply (Hk k Hlt); exact (proj2 (IHs Hnts w i k) Hc).
    + exact (proj2 (IHs Hnts w i j) H1).
    + intros k Hlt Hc; apply (Hk k Hlt); exact (proj1 (IHs Hnts w i k) Hc).
Qed.

(** ** Oracle agreement for the inductive match relation *)

Lemma match_oracle_agree_dir :
  forall G rk (rho : nat) N1 N2,
    strict_stratified G rk ->
    (forall C, rk C < rho -> forall w i j, N1 C w i j <-> N2 C w i j) ->
    forall s w i j,
      MatchO G N1 s w i j ->
      (forall B, In B (nts_of s) -> rk B <= rho) ->
      (forall B, In B (neg_nts s) -> rk B < rho) ->
      MatchO G N2 s w i j.
Proof.
  intros G rk rho N1 N2 Hstrat Hag s w i j H.
  induction H; intros Hpos Hneg; simpl in Hpos, Hneg.
  - (* term *) constructor; assumption.
  - (* eps *) constructor; assumption.
  - (* nt *)
    assert (HA : rk A <= rho) by (apply Hpos; left; reflexivity).
    destruct (Hstrat A alpha H) as [Hp Hn].
    eapply MO_nt; eauto.
    apply IHMatchO.
    + intros B HB. specialize (Hp B HB). lia.
    + intros B HB. specialize (Hn B HB). lia.
  - (* seq *)
    eapply MO_seq.
    + apply IHMatchO1.
      * intros B HB; apply Hpos, in_or_app; left; exact HB.
      * intros B HB; apply Hneg, in_or_app; left; exact HB.
    + apply IHMatchO2.
      * intros B HB; apply Hpos, in_or_app; right; exact HB.
      * intros B HB; apply Hneg, in_or_app; right; exact HB.
  - (* alt_l *)
    apply MO_alt_l.
    apply IHMatchO.
    + intros B HB; apply Hpos, in_or_app; left; exact HB.
    + intros B HB; apply Hneg, in_or_app; left; exact HB.
  - (* alt_r *)
    apply MO_alt_r.
    apply IHMatchO.
    + intros B HB; apply Hpos, in_or_app; right; exact HB.
    + intros B HB; apply Hneg, in_or_app; right; exact HB.
  - (* join *)
    apply MO_join.
    + apply IHMatchO1.
      * intros B HB; apply Hpos, in_or_app; left; exact HB.
      * intros B HB; apply Hneg, in_or_app; left; exact HB.
    + apply IHMatchO2.
      * intros B HB; apply Hpos, in_or_app; right; exact HB.
      * intros B HB; apply Hneg, in_or_app; right; exact HB.
  - (* except *)
    assert (Hb : forall B, In B (nts_of b) -> rk B < rho)
      by (intros B HB; apply Hneg, in_or_app; right; exact HB).
    apply MO_except.
    + apply IHMatchO.
      * intros B HB; apply Hpos, in_or_app; left; exact HB.
      * intros B HB; apply Hneg, in_or_app; left; exact HB.
    + intro Hc; apply H0.
      exact (proj2 (eval_oracle_agree rk rho N1 N2 Hag b Hb w i j) Hc).
  - (* la *)
    eapply MO_la.
    apply IHMatchO; [exact Hpos | exact Hneg].
  - (* nla *)
    apply MO_nla; [assumption|].
    intros k Hc; apply (H0 k).
    exact (proj2 (eval_oracle_agree rk rho N1 N2 Hag a Hneg w i k) Hc).
  - (* longest *)
    apply MO_longest.
    + apply IHMatchO.
      * exact Hpos.
      * intros B HB; apply Hneg, neg_nts_sub; exact HB.
    + intros k Hlt Hc; apply (H0 k Hlt).
      exact (proj2 (eval_oracle_agree rk rho N1 N2 Hag a Hneg w i k) Hc).
Qed.

Lemma match_oracle_agree :
  forall G rk (rho : nat) N1 N2,
    strict_stratified G rk ->
    (forall C, rk C < rho -> forall w i j, N1 C w i j <-> N2 C w i j) ->
    forall s,
      (forall B, In B (nts_of s) -> rk B <= rho) ->
      (forall B, In B (neg_nts s) -> rk B < rho) ->
      forall w i j, MatchO G N1 s w i j <-> MatchO G N2 s w i j.
Proof.
  intros G rk rho N1 N2 Hstrat Hag s Hpos Hneg w i j.
  split; intro H.
  - eapply match_oracle_agree_dir; eauto.
  - eapply match_oracle_agree_dir with (N1 := N2); eauto.
    intros C HC w' i' j'. symmetry. apply Hag; assumption.
Qed.

(** ** Stability of the stratum iteration

    A nonterminal of rank at most [rho] has the same match relation at
    every level strictly above [rho].  This is the mechanized form of the
    paper's "induction on stratification rank": beyond its own stratum, a
    nonterminal's relation never changes again. *)

Lemma nt_level_stable :
  forall G rk,
    strict_stratified G rk ->
    forall (rho : nat) B,
      rk B <= rho ->
      forall L1 L2,
        rho < L1 -> rho < L2 ->
        forall w i j, nt_level G L1 B w i j <-> nt_level G L2 B w i j.
Proof.
  intros G rk Hstrat rho.
  induction rho as [rho IH] using lt_wf_ind.
  intros B HB L1 L2 HL1 HL2 w i j.
  destruct L1 as [|L1']; [lia|].
  destruct L2 as [|L2']; [lia|].
  simpl.
  apply (match_oracle_agree G rk (rk B) (nt_level G L1') (nt_level G L2') Hstrat).
  - (* oracles agree strictly below rk B *)
    intros C HC w' i' j'.
    assert (HmC : rk C < rho) by lia.
    assert (HC1 : rk C < L1') by lia.
    assert (HC2 : rk C < L2') by lia.
    exact (IH (rk C) HmC C (le_n _) L1' L2' HC1 HC2 w' i' j').
  - (* positive occurrences of [s_nt B] rank at most rk B *)
    intros B' HB'. destruct HB' as [HB'|[]]. subst B'. apply le_n.
  - (* no negation positions in [s_nt B] *)
    intros B' HB'. destruct HB'.
Qed.

(** ** The evaluator agrees with the match relation at sufficient level

    For an expression whose nonterminals all rank below [R], evaluating
    with the level-[R] oracle is the same as matching inductively at
    level [R].  This closes the gap between the [EvalN]-phrased negation
    clauses of [MatchO] and the self-referential clause form used in the
    paper (see [WellDefined.v]). *)

Lemma eval_match_agree :
  forall G rk R,
    strict_stratified G rk ->
    forall s,
      (forall B, In B (nts_of s) -> rk B < R) ->
      forall w i j,
        EvalN (nt_level G R) s w i j <-> MatchO G (nt_level G R) s w i j.
Proof.
  intros G rk R Hstrat s.
  induction s; simpl; intros Hnts w i j.
  - (* term *)
    rewrite match_term_iff. tauto.
  - (* eps *)
    rewrite match_eps_iff. tauto.
  - (* nt *)
    assert (HB : rk n < R) by (apply Hnts; left; reflexivity).
    destruct R as [|R']; [lia|].
    change (nt_level G (S R') n w i j)
      with (MatchO G (nt_level G R') (s_nt n) w i j).
    apply (match_oracle_agree G rk (rk n) (nt_level G R') (nt_level G (S R')) Hstrat).
    + intros C HC w' i' j'.
      assert (HC1 : rk C < R') by lia.
      assert (HC2 : rk C < S R') by lia.
      exact (nt_level_stable G rk Hstrat (rk C) C (le_n _) R' (S R') HC1 HC2 w' i' j').
    + intros B' HB'. destruct HB' as [HB'|[]]. subst B'. apply le_n.
    + intros B' HB'. destruct HB'.
  - (* seq *)
    assert (Ha : forall B, In B (nts_of s1) -> rk B < R)
      by (intros B HB; apply Hnts, in_or_app; left; exact HB).
    assert (Hb : forall B, In B (nts_of s2) -> rk B < R)
      by (intros B HB; apply Hnts, in_or_app; right; exact HB).
    rewrite match_seq_iff.
    split; intros [k [H1 H2]]; exists k.
    + split; [exact (proj1 (IHs1 Ha w i k) H1)
             | exact (proj1 (IHs2 Hb w k j) H2)].
    + split; [exact (proj2 (IHs1 Ha w i k) H1)
             | exact (proj2 (IHs2 Hb w k j) H2)].
  - (* alt *)
    assert (Ha : forall B, In B (nts_of s1) -> rk B < R)
      by (intros B HB; apply Hnts, in_or_app; left; exact HB).
    assert (Hb : forall B, In B (nts_of s2) -> rk B < R)
      by (intros B HB; apply Hnts, in_or_app; right; exact HB).
    rewrite match_alt_iff.
    split; (intros [H|H]; [left|right]).
    + exact (proj1 (IHs1 Ha w i j) H).
    + exact (proj1 (IHs2 Hb w i j) H).
    + exact (proj2 (IHs1 Ha w i j) H).
    + exact (proj2 (IHs2 Hb w i j) H).
  - (* join *)
    assert (Ha : forall B, In B (nts_of s1) -> rk B < R)
      by (intros B HB; apply Hnts, in_or_app; left; exact HB).
    assert (Hb : forall B, In B (nts_of s2) -> rk B < R)
      by (intros B HB; apply Hnts, in_or_app; right; exact HB).
    rewrite match_join_iff.
    split; intros [H1 H2]; split.
    + exact (proj1 (IHs1 Ha w i j) H1).
    + exact (proj1 (IHs2 Hb w i j) H2).
    + exact (proj2 (IHs1 Ha w i j) H1).
    + exact (proj2 (IHs2 Hb w i j) H2).
  - (* except: the negated operand is [EvalN] on both sides *)
    assert (Ha : forall B, In B (nts_of s1) -> rk B < R)
      by (intros B HB; apply Hnts, in_or_app; left; exact HB).
    rewrite match_except_iff.
    split; intros [H1 H2]; split; try exact H2.
    + exact (proj1 (IHs1 Ha w i j) H1).
    + exact (proj2 (IHs1 Ha w i j) H1).
  - (* la *)
    rewrite match_la_iff.
    split; intros [-> [k Hk]]; (split; [reflexivity|]); exists k.
    + exact (proj1 (IHs Hnts w i k) Hk).
    + exact (proj2 (IHs Hnts w i k) Hk).
  - (* nla: the operand is [EvalN] on both sides *)
    rewrite match_nla_iff. tauto.
  - (* longest: positive operand bridged, negative operand identical *)
    rewrite match_longest_iff.
    split; intros [H1 H2]; split; try exact H2.
    + exact (proj1 (IHs Hnts w i j) H1).
    + exact (proj2 (IHs Hnts w i j) H1).
Qed.
