(** * CounterExample: paper Definition 3.2 is insufficient as stated.

    Paper Definition 3.2 (strict stratified CDG) constrains only the
    nonterminals occurring in NEGATION positions ([rk B < rk A]) and
    places no constraint on positive references.  This file shows,
    mechanically, that the definition as stated does not support
    Theorem 3.3: there is a grammar that satisfies the paper's condition
    yet admits NO span predicate satisfying the defining clauses.

    The grammar is

      B -> C        (rule 0; positive reference, unconstrained)
      C -> !B       (rule 1; negation position, rk B < rk C holds)

    with rk B = 0, rk C = 1.  Unfolding the clauses gives
    Match(B, w, i, i)  iff  Match(C, w, i, i)  iff  not Match(B, w, i, i):
    a paradox.

    The corrected definition ([Stratification.strict_stratified]) adds
    the standard positive condition [rk B <= rk A] for every nonterminal
    occurring in a production body (as in stratified negation for logic
    programs); this grammar then fails stratification, as shown below.

    PAPER ACTION: fix Definition 3.2 accordingly, and adjust the proof
    sketch of Theorem 3.3 (rank-0 strata are self-contained only with
    the positive condition). *)

From Stdlib Require Import Ascii String List Arith Lia.
Import ListNotations.
From CDG Require Import Syntax Stratification Match.

(** The paradoxical grammar.  Nonterminal 0 is B, nonterminal 1 is C. *)
Definition BadG : grammar :=
  mkGrammar
    (fun n =>
       match n with
       | 0 => Some (s_nt 1)            (* B -> C *)
       | 1 => Some (s_nla (s_nt 0))    (* C -> !B *)
       | _ => None
       end)
    0.

(** Paper Definition 3.2, literally: only negation positions constrained. *)
Definition paper_stratified (G : grammar) (rk : nt_id -> nat) : Prop :=
  forall A alpha B,
    rules G A = Some alpha ->
    In B (neg_nts alpha) ->
    rk B < rk A.

(** [BadG] satisfies the paper's definition with rk = id. *)
Example badg_paper_stratified : paper_stratified BadG (fun n => n).
Proof.
  intros A alpha B Hr HB.
  destruct A as [|[|A]]; simpl in Hr; inversion Hr; subst; simpl in HB.
  - (* A = 0 : alpha = s_nt 1, no negation positions *) contradiction.
  - (* A = 1 : alpha = !B, neg_nts = [0] *)
    destruct HB as [HB|[]]; subst; simpl; lia.
Qed.

(** [BadG] fails the corrected definition under EVERY rank function. *)
Example badg_not_strict_stratified :
  forall rk, ~ strict_stratified BadG rk.
Proof.
  intros rk H.
  destruct (H 0 (s_nt 1) eq_refl) as [Hpos0 _].
  destruct (H 1 (s_nla (s_nt 0)) eq_refl) as [_ Hneg1].
  assert (H10 : rk 1 <= rk 0) by (apply Hpos0; left; reflexivity).
  assert (H01 : rk 0 < rk 1) by (apply Hneg1; left; reflexivity).
  lia.
Qed.

(** No span predicate satisfies the defining clauses on [BadG].  We only
    need the nonterminal clause and the negative-lookahead clause; [M] is
    otherwise arbitrary, so this refutes every candidate "Match" relation
    for [BadG], with any treatment of the remaining operators. *)
Theorem badg_no_model :
  forall M : sym -> input -> nat -> nat -> Prop,
    (forall A w i j,
        M (s_nt A) w i j <->
        (exists alpha, rules BadG A = Some alpha /\ M alpha w i j)) ->
    (forall a w i j,
        M (s_nla a) w i j <->
        (j = i /\ i <= length w /\ (forall k, ~ M a w i k))) ->
    False.
Proof.
  intros M Hnt Hnla.
  (* Any match of B on the empty input starting at 0 ends at 0. *)
  assert (Hspan : forall k, M (s_nt 0) [] 0 k -> k = 0).
  { intros k Hk.
    apply Hnt in Hk. destruct Hk as [alpha [Ha Hm]].
    simpl in Ha. inversion Ha; subst.
    apply Hnt in Hm. destruct Hm as [beta [Hb Hm']].
    simpl in Hb. inversion Hb; subst.
    apply Hnla in Hm'. destruct Hm' as [-> _]. reflexivity. }
  (* Match(B,[],0,0) iff not Match(B,[],0,0). *)
  assert (Hiff : M (s_nt 0) [] 0 0 <-> ~ M (s_nt 0) [] 0 0).
  { split.
    - intro H0.
      apply Hnt in H0. destruct H0 as [alpha [Ha Hm]].
      simpl in Ha. inversion Ha; subst.
      apply Hnt in Hm. destruct Hm as [beta [Hb Hm']].
      simpl in Hb. inversion Hb; subst.
      apply Hnla in Hm'. destruct Hm' as [_ [_ Hk]].
      intro HP. exact (Hk 0 HP).
    - intro Hn.
      apply Hnt. eexists. split; [reflexivity|].
      apply Hnt. eexists. split; [reflexivity|].
      apply Hnla.
      split; [reflexivity|]. split; [simpl; lia|].
      intros k Hk.
      apply Hn.
      pose proof (Hspan k Hk) as Hk0. subst k. exact Hk. }
  tauto.
Qed.
