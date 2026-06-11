(** * Stratification: strict stratified CDG (paper Definition 3.2, CORRECTED).

    The paper states strict stratification via an abstract well-founded
    strict partial order on nonterminals.  We mechanize it with a rank
    function [rk : nt_id -> nat]: for finite grammars the two forms are
    equivalent (any well-founded strict partial order on a finite set
    embeds into [nat]), and the rank form is what the rank-indexed
    construction of [Match] consumes directly.

    IMPORTANT — PAPER FIX REQUIRED.  Paper Definition 3.2 only constrains
    nonterminals in negation positions ([rk B < rk A]) and leaves positive
    references unconstrained.  That definition is insufficient: the grammar

      B -> C        (positive reference, unconstrained by Def 3.2)
      C -> !B       (negation position, rk B < rk C satisfiable)

    is strict stratified under the paper's definition with rk B = 0,
    rk C = 1, yet it is paradoxical: Match(B,w,i,i) iff Match(C,w,i,i) iff
    not Match(B,w,i,i).  No span predicate satisfies the defining clauses,
    so Theorem 3.3 is false as stated.  See [CounterExample.v] for the
    mechanized refutation.

    The corrected definition (standard stratified negation, as in logic
    programming) additionally requires ranks to be non-increasing along
    positive references: every nonterminal occurring in a production body
    has rank at most that of the head, and every nonterminal in a negation
    position has strictly smaller rank. *)

From Stdlib Require Import List Arith Lia.
Import ListNotations.
From CDG Require Import Syntax.

Definition strict_stratified (G : grammar) (rk : nt_id -> nat) : Prop :=
  forall A alpha,
    rules G A = Some alpha ->
    (forall B, In B (nts_of alpha) -> rk B <= rk A) /\
    (forall B, In B (neg_nts alpha) -> rk B < rk A).

Lemma stratified_pos :
  forall G rk A alpha B,
    strict_stratified G rk ->
    rules G A = Some alpha ->
    In B (nts_of alpha) ->
    rk B <= rk A.
Proof. intros G rk A alpha B Hs Hr HB. eapply Hs; eauto. Qed.

Lemma stratified_neg :
  forall G rk A alpha B,
    strict_stratified G rk ->
    rules G A = Some alpha ->
    In B (neg_nts alpha) ->
    rk B < rk A.
Proof. intros G rk A alpha B Hs Hr HB. eapply Hs; eauto. Qed.

(** Rank-0 nonterminals have no nonterminals in negation positions. *)
Lemma rank0_no_neg_nts :
  forall G rk A alpha,
    strict_stratified G rk ->
    rules G A = Some alpha ->
    rk A = 0 ->
    neg_nts alpha = [].
Proof.
  intros G rk A alpha Hstrat Hrule Hrk0.
  destruct (neg_nts alpha) as [| B rest] eqn:E; [reflexivity |].
  exfalso.
  assert (HB : In B (neg_nts alpha)) by (rewrite E; left; reflexivity).
  pose proof (stratified_neg _ _ _ _ _ Hstrat Hrule HB). lia.
Qed.

(** [R] bounds the ranks of all defined nonterminals of [G].  This is the
    finiteness assumption the paper gets from grammars being finite. *)
Definition rank_bound (G : grammar) (rk : nt_id -> nat) (R : nat) : Prop :=
  forall A alpha, rules G A = Some alpha -> rk A < R.
