(** * ACP: Naive Accept-Condition Parsing (paper Section "Accept-Condition
    Parsing", Theorems 4.1/4.2 — 목표 A, phase 1).

    This file mechanizes the reference parsing algorithm against the
    mechanized span semantics ([Match.v]/[WellDefined.v]).  Design choices,
    aligned with the paper's §ACP draft:

    - Kernels are dotted positions over the EXPRESSION syntax [sym] (the
      paper's normalized grammar is replaced by binary expression trees, so
      a sequence kernel has dot positions 0..2 and every other symbol
      0..1).  This removes the normalization layer entirely: the chart and
      the semantics speak the same language.

    - Accept conditions are syntax: the four primitive shapes of the
      paper's 2x2 table (bounded/unbounded x positive/negative) over
      watched operand expressions, closed under conjunction/disjunction.

    - PHASE 1 (this file): conditions are discharged SEMANTICALLY — the
      interpretation [csem] reads each shape with the corresponding Match
      clause.  The chart-construction rules ([Node]/[Edge]) are exactly
      the saturation rules of the paper (Derive / Monitor / Finish+Progress
      / terminal consumption), with generations implicit in kernel end
      positions.  Soundness/completeness (Theorems 4.1/4.2) are stated
      against [CDGMatch].

    - PHASE 2 (future): the operational condition mechanism — generation-
      by-generation evolution and final evaluation (the paper's evolution
      figure) — and its equivalence with [csem].  Phase 1 isolates chart
      correctness from condition-mechanism correctness; the per-operator
      "condition adequacy" decomposition lives in [csem]'s clauses.

    No pruning/trimming: the reference algorithm keeps all kernels.
    Pruning is an optimization of the implementations, out of scope here. *)

From Stdlib Require Import Ascii String List Arith Lia.
Import ListNotations.
From CDG Require Import Syntax Stratification Match Agreement WellDefined.

(** ** Kernels *)

(** Dot positions: a sequence has two components (positions 0, 1, 2);
    [s_eps] is complete at 0; everything else completes at 1. *)
Definition last_pos (s : sym) : nat :=
  match s with
  | s_seq _ _ => 2
  | s_eps => 0
  | _ => 1
  end.

Record kernel : Type := mkKernel {
  ksym : sym;
  kpos : nat;
  kbeg : nat;
  kend : nat;
}.

Definition kinit (s : sym) (g : nat) : kernel := mkKernel s 0 g g.

Definition kfinal (k : kernel) : Prop := kpos k = last_pos (ksym k).

(** ** Accept conditions (syntax)

    The four shapes of the paper's table; watched operands are
    expressions.  [c_exists]/[c_notexists] carry the start position [b]
    and the first generation [e] from which ends are constrained;
    [c_onlyif]/[c_unless] constrain the single span [(b, e)]. *)

Inductive cond : Type :=
  | c_true : cond
  | c_false : cond
  | c_and : cond -> cond -> cond
  | c_or : cond -> cond -> cond
  | c_onlyif : nat -> nat -> sym -> cond
  | c_unless : nat -> nat -> sym -> cond
  | c_exists : nat -> nat -> sym -> cond
  | c_notexists : nat -> nat -> sym -> cond.

(** Semantic discharge: each shape is read with the corresponding clause
    of the span semantics, at stratification level [R].  This is the
    per-operator "condition adequacy" of the paper made definitional:
    proving the chart sound/complete against [csem] and separately the
    operational evolution equal to [csem] decomposes the correctness
    argument operator by operator. *)
Fixpoint csem (G : grammar) (R : nat) (w : input) (c : cond) : Prop :=
  match c with
  | c_true => True
  | c_false => False
  | c_and l r => csem G R w l /\ csem G R w r
  | c_or l r => csem G R w l \/ csem G R w r
  | c_onlyif b e X => CDGMatch G R X w b e
  | c_unless b e X => ~ CDGMatch G R X w b e
  | c_exists b e X => exists g, e <= g /\ CDGMatch G R X w b g
  | c_notexists b e X => forall g, e <= g -> ~ CDGMatch G R X w b g
  end.

(** ** The grammar-driven structure of a kernel

    [expects k] — the symbols whose match the kernel is waiting for
    (children derived WITH an edge); [monitors s] — the operands parsed
    independently for condition discharge (derived WITHOUT an edge);
    [gencond s b e] — the condition attached when a kernel of [s]
    advances over its body span [(b, e)] (the paper's generation table). *)

Definition expects (k : kernel) (G : grammar) : list sym :=
  match ksym k, kpos k with
  | s_seq a _, 0 => [a]
  | s_seq _ b, 1 => [b]
  | s_nt A, 0 => match rules G A with Some alpha => [alpha] | None => [] end
  | s_alt a b, 0 => [a; b]
  | s_join a _, 0 => [a]
  | s_except a _, 0 => [a]
  | s_longest a, 0 => [a]
  | s_la _, 0 => [s_eps]
  | s_nla _, 0 => [s_eps]
  | _, _ => []
  end.

Definition monitors (s : sym) : list sym :=
  match s with
  | s_join _ z => [z]
  | s_except _ z => [z]
  | s_la y => [y]
  | s_nla y => [y]
  | _ => []
  end.

Definition gencond (s : sym) (b e : nat) : cond :=
  match s with
  | s_join _ z => c_onlyif b e z
  | s_except _ z => c_unless b e z
  | s_longest y => c_notexists b (S e) y
  | s_la y => c_exists e e y
  | s_nla y => c_notexists e e y
  | _ => c_true
  end.

(** ** The chart

    [Node G w k φ]: kernel [k] is justified by the input prefix it spans,
    conditionally on [φ].  [Edge G w j k]: the match in progress at [j]
    is waiting for the symbol being matched at [k].  Generations are
    implicit: the only rule that advances an end position past [g]
    consumes [w[g]].  This is the least relation closed under the
    saturation rules of the paper (with v1-style condition pairs: a
    kernel may be justified by several conditions; the per-kernel
    disjunction of the paper's presentation is the join over pairs). *)

Inductive Node (G : grammar) (w : input) : kernel -> cond -> Prop :=
  | N_start :
      Node G w (kinit (s_nt (start G)) 0) c_true
  | N_derive : forall k phi Y,
      Node G w k phi ->
      ~ kfinal k ->
      In Y (expects k G) ->
      Node G w (kinit Y (kend k)) c_true
  | N_monitor : forall k phi Z,
      Node G w k phi ->
      kpos k = 0 ->
      In Z (monitors (ksym k)) ->
      Node G w (kinit Z (kend k)) c_true
  | N_term : forall a g phi,
      Node G w (mkKernel (s_term a) 0 g g) phi ->
      nth_error w g = Some a ->
      Node G w (mkKernel (s_term a) 1 g (S g)) c_true
  | N_progress : forall j phi k' psi,
      Edge G w j k' ->
      Node G w j phi ->
      Node G w k' psi ->
      kfinal k' ->
      ~ kfinal j ->
      Node G w (mkKernel (ksym j) (S (kpos j)) (kbeg j) (kend k'))
           (c_and phi (c_and psi (gencond (ksym j) (kbeg j) (kend k'))))

with Edge (G : grammar) (w : input) : kernel -> kernel -> Prop :=
  | E_derive : forall k phi Y,
      Node G w k phi ->
      ~ kfinal k ->
      In Y (expects k G) ->
      Edge G w k (kinit Y (kend k))
  | E_inherit : forall i j k' psi,
      Edge G w i j ->
      Node G w k' psi ->
      Edge G w j k' ->
      kfinal k' ->
      ~ kfinal j ->
      Edge G w i (mkKernel (ksym j) (S (kpos j)) (kbeg j) (kend k')).

(** Acceptance: the start symbol's final kernel over the whole input,
    with a semantically true condition. *)
Definition AcceptedBy (G : grammar) (R : nat) (w : input) : Prop :=
  exists phi,
    Node G w (mkKernel (s_nt (start G)) 1 0 (length w)) phi /\
    csem G R w phi.

(** ** Prefix matches: the soundness invariant of kernels

    What a kernel [(X, p, b, e)] asserts about the input, conditionally
    on its condition: nothing for [p = 0] (except [b = e]); the left
    component for a sequence at [p = 1]; a full match at [p = last].
    For conditional symbols, the full match additionally requires the
    operator's clause — exactly [csem] of [gencond]. *)
Definition PrefixMatch (G : grammar) (R : nat) (w : input) (k : kernel) : Prop :=
  match ksym k, kpos k with
  | s_eps, _ => CDGMatch G R s_eps w (kbeg k) (kend k)
  | _, 0 => kbeg k = kend k
  | s_seq a _, 1 => CDGMatch G R a w (kbeg k) (kend k)
  | _, _ => CDGMatch G R (ksym k) w (kbeg k) (kend k)
  end.

(** ** Theorems 4.1 / 4.2 (statements; proofs are the next milestone)

    Hypotheses mirror [WellDefined.v]: strict stratification with rank
    witness [rk], and level [R] bounding the ranks of all nonterminals.
    The intended proof structure follows the paper's invariants:
    - span soundness: [Node k φ] with [csem φ] implies [PrefixMatch k]
      (induction over the chart; the conditional-operator cases reduce to
      the per-operator clause lemmas of [Match.v]/[WellDefined.v]);
    - completeness of saturation: every [Match]-justified prefix match is
      represented by a chart kernel whose condition is semantically true
      (induction over the semantics, Earley-completeness style). *)

(* [naive_acp_soundness] is stated and proved at the end of this file,
   after the chart invariants it depends on. *)

Theorem naive_acp_completeness :
  forall G rk R,
    strict_stratified G rk ->
    rank_bound G rk R ->
    forall w,
      CDGMatch G R (s_nt (start G)) w 0 (length w) ->
      AcceptedBy G R w.
Admitted.

(** ** First lemmas toward the invariant *)

(** Kernel positions never exceed the symbol's last position. *)
Lemma node_kpos_bound :
  forall G w k phi, Node G w k phi -> kpos k <= last_pos (ksym k).
Proof.
  intros G w k phi H.
  induction H; unfold kinit, kfinal in *; simpl in *; lia.
Qed.

(** Chart spans are within the input and properly ordered; edges connect
    adjacent spans. Proved by the mutual induction scheme. *)

Scheme Node_mut := Minimality for Node Sort Prop
  with Edge_mut := Minimality for Edge Sort Prop.

Combined Scheme NodeEdge_mut from Node_mut, Edge_mut.

Lemma node_edge_span_valid :
  forall G w,
    (forall k phi, Node G w k phi ->
       kbeg k <= kend k /\ kend k <= length w) /\
    (forall j k', Edge G w j k' -> kend j = kbeg k').
Proof.
  intros G w.
  apply (NodeEdge_mut G w
    (fun k phi => kbeg k <= kend k /\ kend k <= length w)
    (fun j k' => kend j = kbeg k')).
  - (* N_start *) simpl. lia.
  - (* N_derive *) intros k phi Y _ [Hbe Hew] _ _. simpl. lia.
  - (* N_monitor *) intros k phi Z _ [Hbe Hew] _ _. simpl. lia.
  - (* N_term *)
    intros a g phi _ _ Hnth. simpl.
    assert (g < length w).
    { apply nth_error_Some. rewrite Hnth. discriminate. }
    lia.
  - (* N_progress *)
    intros j phi k' psi _ Hjk' _ [Hbj Hjw] _ [Hbk Hkw] _ _.
    simpl. lia.
  - (* E_derive *) intros k phi Y _ _ _ _. simpl. reflexivity.
  - (* E_inherit *)
    intros i j k' psi _ Hij _ _ _ Hjk' _ _.
    simpl. exact Hij.
Qed.

Lemma node_span_valid :
  forall G w k phi, Node G w k phi -> kbeg k <= kend k /\ kend k <= length w.
Proof.
  intros G w. exact (proj1 (node_edge_span_valid G w)).
Qed.

(** Position-0 kernels are zero-width: every rule that creates one
    creates an initial kernel. *)
Lemma node_pos0_span :
  forall G w k phi, Node G w k phi -> kpos k = 0 -> kbeg k = kend k.
Proof.
  intros G w k phi H.
  induction H; unfold kinit; simpl; intros; solve [reflexivity | lia].
Qed.

(** A final kernel's prefix match is a full match of its symbol. *)
Lemma prefix_final :
  forall G R w k,
    kfinal k ->
    PrefixMatch G R w k ->
    CDGMatch G R (ksym k) w (kbeg k) (kend k).
Proof.
  intros G R w k Hf HP.
  unfold kfinal in Hf. unfold PrefixMatch in HP.
  destruct k as [s p b e]; simpl in *.
  subst p.
  destruct s; simpl in HP; exact HP.
Qed.

(** ** The span-soundness invariant (the heart of Theorem 4.1)

    Every chart kernel, conditionally on its accept condition, is a
    prefix match; every edge connects adjacent spans and respects the
    expected-symbol discipline.  The conditional-operator cases discharge
    via the per-operator clause constructors of [Match.v] and the
    [eval_match_agree] bridge of [Agreement.v] — the mechanized form of
    the paper's "condition adequacy" invariant. *)
Lemma node_sound_mut :
  forall G rk R,
    strict_stratified G rk ->
    rank_bound G rk R ->
    (exists alpha0, rules G (start G) = Some alpha0) ->
    forall w,
      (forall k phi, Node G w k phi ->
         (forall B, In B (nts_of (ksym k)) -> rk B < R) /\
         (csem G R w phi -> PrefixMatch G R w k)) /\
      (forall j k', Edge G w j k' ->
         kend j = kbeg k' /\ In (ksym k') (expects j G)).
Proof.
  intros G rk R Hstrat Hbound HstartD w.
  apply (NodeEdge_mut G w
    (fun k phi =>
       (forall B, In B (nts_of (ksym k)) -> rk B < R) /\
       (csem G R w phi -> PrefixMatch G R w k))
    (fun j k' =>
       kend j = kbeg k' /\ In (ksym k') (expects j G))).
  - (* N_start *)
    split.
    + intros B HB. simpl in HB. destruct HB as [HB|[]]. subst B.
      destruct HstartD as [alpha0 Halpha0]. exact (Hbound _ _ Halpha0).
    + intros _. unfold PrefixMatch, kinit. simpl. reflexivity.
  - (* N_derive *)
    intros k phi Y HNode IH Hnf HY.
    destruct IH as [Hbnd _].
    pose proof (node_span_valid _ _ _ _ HNode) as HspanK.
    split.
    + intros B HB. unfold kinit in *. simpl in *.
      destruct k as [s p b e]; simpl in *.
      unfold expects in HY.
      destruct s; simpl in HY.
      * (* s_term *) contradiction.
      * (* s_eps *) contradiction.
      * (* s_nt *)
        destruct p as [|p]; simpl in HY; [|contradiction].
        destruct (rules G n) as [alpha|] eqn:EA; simpl in HY; [|contradiction].
        destruct HY as [HY|[]]. subst Y.
        assert (HnA : rk n < R) by (apply Hbnd; left; reflexivity).
        pose proof (stratified_pos _ _ _ _ _ Hstrat EA HB). lia.
      * (* s_seq *)
        destruct p as [|[|p]]; simpl in HY; [| |contradiction].
        -- destruct HY as [HY|[]]. subst Y.
           apply Hbnd. apply in_or_app. left. exact HB.
        -- destruct HY as [HY|[]]. subst Y.
           apply Hbnd. apply in_or_app. right. exact HB.
      * (* s_alt *)
        destruct p as [|p]; simpl in HY; [|contradiction].
        destruct HY as [HY|[HY|[]]]; subst Y.
        -- apply Hbnd. apply in_or_app. left. exact HB.
        -- apply Hbnd. apply in_or_app. right. exact HB.
      * (* s_join *)
        destruct p as [|p]; simpl in HY; [|contradiction].
        destruct HY as [HY|[]]. subst Y.
        apply Hbnd. apply in_or_app. left. exact HB.
      * (* s_except *)
        destruct p as [|p]; simpl in HY; [|contradiction].
        destruct HY as [HY|[]]. subst Y.
        apply Hbnd. apply in_or_app. left. exact HB.
      * (* s_la *)
        destruct p as [|p]; simpl in HY; [|contradiction].
        destruct HY as [HY|[]]. subst Y. simpl in HB. contradiction.
      * (* s_nla *)
        destruct p as [|p]; simpl in HY; [|contradiction].
        destruct HY as [HY|[]]. subst Y. simpl in HB. contradiction.
      * (* s_longest *)
        destruct p as [|p]; simpl in HY; [|contradiction].
        destruct HY as [HY|[]]. subst Y. apply Hbnd. exact HB.
    + intros _. unfold PrefixMatch, kinit. simpl.
      destruct Y; simpl; try reflexivity.
      unfold CDGMatch. apply MO_eps. lia.
  - (* N_monitor *)
    intros k phi Z HNode IH Hp0 HZ.
    destruct IH as [Hbnd _].
    pose proof (node_span_valid _ _ _ _ HNode) as HspanK.
    split.
    + intros B HB. unfold kinit in *. simpl in *.
      unfold monitors in HZ.
      destruct (ksym k); simpl in HZ; try contradiction;
        destruct HZ as [HZ|[]]; subst Z.
      * (* s_join *) apply Hbnd. apply in_or_app. right. exact HB.
      * (* s_except *) apply Hbnd. apply in_or_app. right. exact HB.
      * (* s_la *) apply Hbnd. exact HB.
      * (* s_nla *) apply Hbnd. exact HB.
    + intros _. unfold PrefixMatch, kinit. simpl.
      destruct Z; simpl; try reflexivity.
      unfold CDGMatch. apply MO_eps. lia.
  - (* N_term *)
    intros a g phi HNode _ Hnth.
    split.
    + intros B HB. simpl in HB. contradiction.
    + intros _. unfold PrefixMatch. simpl.
      unfold CDGMatch. apply MO_term. exact Hnth.
  - (* N_progress *)
    intros j phi k' psi HEdge IHe HNj IHj HNk IHk' Hfin Hnf.
    destruct IHe as [Hadj Hexp].
    destruct IHj as [Hbndj HPj].
    destruct IHk' as [Hbndk HPk].
    split.
    + simpl. exact Hbndj.
    + intros Hc. simpl in Hc.
      destruct Hc as [Hcphi [Hcpsi Hcgen]].
      specialize (HPj Hcphi). specialize (HPk Hcpsi).
      pose proof (prefix_final _ _ _ _ Hfin HPk) as HMk.
      pose proof (node_span_valid _ _ _ _ HNj) as HspanJ.
      pose proof (node_span_valid _ _ _ _ HNk) as HspanK.
      pose proof (node_kpos_bound _ _ _ _ HNj) as Hkb.
      unfold kfinal in Hnf.
      destruct j as [sj pj bj ej]; simpl in *.
      unfold expects in Hexp. unfold gencond in Hcgen.
      destruct sj; simpl in *.
      * (* s_term: no expected children *)
        destruct pj as [|pj]; simpl in Hexp; [contradiction|lia].
      * (* s_eps: already final at 0 *)
        lia.
      * (* s_nt *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct (rules G n) as [alpha|] eqn:EA; simpl in Hexp; [|contradiction].
        destruct Hexp as [Heq|[]].
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        rewrite <- Hadj in HMk. rewrite <- Heq in HMk.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        eapply MO_nt; [exact EA|exact HMk].
      * (* s_seq *)
        destruct pj as [|[|pj]]; [| |lia].
        -- (* 0 -> 1 *)
           simpl in Hexp. destruct Hexp as [Heq|[]].
           assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
           subst bj.
           rewrite <- Hadj in HMk. rewrite <- Heq in HMk.
           unfold PrefixMatch. simpl. exact HMk.
        -- (* 1 -> 2 *)
           simpl in Hexp. destruct Hexp as [Heq|[]].
           rewrite <- Hadj in HMk. rewrite <- Heq in HMk.
           unfold PrefixMatch in HPj. simpl in HPj.
           unfold PrefixMatch. simpl. unfold CDGMatch in *.
           eapply MO_seq; [exact HPj|exact HMk].
      * (* s_alt *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        rewrite <- Hadj in HMk.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        destruct Hexp as [Heq|[Heq|[]]].
        -- rewrite <- Heq in HMk. apply MO_alt_l. exact HMk.
        -- rewrite <- Heq in HMk. apply MO_alt_r. exact HMk.
      * (* s_join *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct Hexp as [Heq|[]].
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        rewrite <- Hadj in HMk. rewrite <- Heq in HMk.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        simpl in Hcgen.
        apply MO_join; [exact HMk|exact Hcgen].
      * (* s_except *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct Hexp as [Heq|[]].
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        rewrite <- Hadj in HMk. rewrite <- Heq in HMk.
        assert (Hz : forall B, In B (nts_of sj2) -> rk B < R)
          by (intros B HB; apply Hbndj, in_or_app; right; exact HB).
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        simpl in Hcgen.
        apply MO_except; [exact HMk|].
        intro HEv. apply Hcgen.
        exact (proj1 (eval_match_agree G rk R Hstrat sj2 Hz w ej (kend k')) HEv).
      * (* s_la *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct Hexp as [Heq|[]].
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        assert (Hk0 : kpos k' = 0).
        { unfold kfinal in Hfin. rewrite <- Heq in Hfin. simpl in Hfin. exact Hfin. }
        assert (Hkz : kbeg k' = kend k')
          by (eapply (node_pos0_span _ _ _ _ HNk); exact Hk0).
        assert (Hee : kend k' = ej) by lia.
        rewrite Hee in Hcgen.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        rewrite Hee.
        destruct Hcgen as [g [Hg HMg]].
        eapply MO_la. exact HMg.
      * (* s_nla *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct Hexp as [Heq|[]].
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        assert (Hk0 : kpos k' = 0).
        { unfold kfinal in Hfin. rewrite <- Heq in Hfin. simpl in Hfin. exact Hfin. }
        assert (Hkz : kbeg k' = kend k')
          by (eapply (node_pos0_span _ _ _ _ HNk); exact Hk0).
        assert (Hee : kend k' = ej) by lia.
        rewrite Hee in Hcgen.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        rewrite Hee.
        apply MO_nla; [lia|].
        intros m HEv.
        pose proof (eval_span_valid _ (nt_level_valid G R) _ _ _ _ HEv) as [Hm1 _].
        apply (Hcgen m); [lia|].
        exact (proj1 (eval_match_agree G rk R Hstrat sj Hbndj w ej m) HEv).
      * (* s_longest *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct Hexp as [Heq|[]].
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        rewrite <- Hadj in HMk. rewrite <- Heq in HMk.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        simpl in Hcgen.
        apply MO_longest; [exact HMk|].
        intros m Hlt HEv.
        apply (Hcgen m); [lia|].
        exact (proj1 (eval_match_agree G rk R Hstrat sj Hbndj w ej m) HEv).
  - (* E_derive *)
    intros k phi Y HNode IH Hnf HY.
    split.
    + unfold kinit. simpl. reflexivity.
    + unfold kinit. simpl. exact HY.
  - (* E_inherit *)
    intros i j k' psi HE1 IHe1 HNk IHk' HE2 IHe2 Hfin Hnf.
    destruct IHe1 as [Hadj1 Hexp1].
    split.
    + simpl. exact Hadj1.
    + simpl. exact Hexp1.
Qed.

(** Theorem 4.1 (soundness of Naive ACP), proved. *)
Theorem naive_acp_soundness :
  forall G rk R,
    strict_stratified G rk ->
    rank_bound G rk R ->
    (exists alpha, rules G (start G) = Some alpha) ->
    forall w,
      AcceptedBy G R w ->
      CDGMatch G R (s_nt (start G)) w 0 (length w).
Proof.
  intros G rk R Hstrat Hbound HstartD w [phi [HN Hc]].
  pose proof (proj1 (node_sound_mut G rk R Hstrat Hbound HstartD w) _ _ HN)
    as [_ HP].
  specialize (HP Hc).
  unfold PrefixMatch in HP. simpl in HP.
  exact HP.
Qed.
