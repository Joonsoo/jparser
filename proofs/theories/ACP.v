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
      clause.  The chart-construction rules ([Node]/[Edge]) implement the
      saturation rules of the paper, with generations implicit in kernel
      end positions.  Edges always point at INITIAL kernels; the Progress
      rule pairs an edge with the corresponding final kernel (same symbol,
      same begin).  This is equivalent to the paper's presentation in
      which progress copies predecessor edges to the advanced kernel, but
      avoids edge-inheritance bookkeeping in the proofs.

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

(** ** Accept conditions (syntax) *)

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
    of the span semantics, at stratification level [R]. *)
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

(** ** The grammar-driven structure of a kernel *)

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

    [Node G w k phi]: kernel [k] is justified by the input prefix it
    spans, conditionally on [phi].  [Edge G w j k0]: the match in
    progress at [j] is waiting for the symbol whose initial kernel is
    [k0].  [N_progress] advances [j] over a completed match of an
    expected symbol: an edge to the initial kernel of [Y] at [g],
    together with the final kernel of [Y] beginning at [g].  This is the
    least relation closed under the rules (v1-style condition pairs: a
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
  | N_progress : forall j phi Y g e psi,
      Edge G w j (kinit Y g) ->
      Node G w j phi ->
      Node G w (mkKernel Y (last_pos Y) g e) psi ->
      ~ kfinal j ->
      Node G w (mkKernel (ksym j) (S (kpos j)) (kbeg j) e)
           (c_and phi (c_and psi (gencond (ksym j) (kbeg j) e)))

with Edge (G : grammar) (w : input) : kernel -> kernel -> Prop :=
  | E_derive : forall k phi Y,
      Node G w k phi ->
      ~ kfinal k ->
      In Y (expects k G) ->
      Edge G w k (kinit Y (kend k)).

Definition AcceptedBy (G : grammar) (R : nat) (w : input) : Prop :=
  exists phi,
    Node G w (mkKernel (s_nt (start G)) 1 0 (length w)) phi /\
    csem G R w phi.

(** ** Prefix matches: the soundness invariant of kernels *)

Definition PrefixMatch (G : grammar) (R : nat) (w : input) (k : kernel) : Prop :=
  match ksym k, kpos k with
  | s_eps, _ => CDGMatch G R s_eps w (kbeg k) (kend k)
  | _, 0 => kbeg k = kend k
  | s_seq a _, 1 => CDGMatch G R a w (kbeg k) (kend k)
  | _, _ => CDGMatch G R (ksym k) w (kbeg k) (kend k)
  end.

(* Theorems 4.1/4.2 are stated and proved at the end of this file. *)

(** ** Basic invariants *)

Lemma node_kpos_bound :
  forall G w k phi, Node G w k phi -> kpos k <= last_pos (ksym k).
Proof.
  intros G w k phi H.
  induction H; unfold kinit, kfinal in *; simpl in *; lia.
Qed.

Scheme Node_mut := Minimality for Node Sort Prop
  with Edge_mut := Minimality for Edge Sort Prop.

Combined Scheme NodeEdge_mut from Node_mut, Edge_mut.

Lemma node_edge_span_valid :
  forall G w,
    (forall k phi, Node G w k phi ->
       kbeg k <= kend k /\ kend k <= length w) /\
    (forall j k0, Edge G w j k0 -> kend j = kbeg k0).
Proof.
  intros G w.
  apply (NodeEdge_mut G w
    (fun k phi => kbeg k <= kend k /\ kend k <= length w)
    (fun j k0 => kend j = kbeg k0)).
  - simpl. lia.
  - intros k phi Y _ [Hbe Hew] _ _. simpl. lia.
  - intros k phi Z _ [Hbe Hew] _ _. simpl. lia.
  - intros a g phi _ _ Hnth. simpl.
    assert (g < length w).
    { apply nth_error_Some. rewrite Hnth. discriminate. }
    lia.
  - intros j phi Y g e psi _ Hjk _ [Hbj Hjw] _ [Hge Hew] _.
    simpl in *. lia.
  - intros k phi Y _ _ _ _. simpl. reflexivity.
Qed.

Lemma node_span_valid :
  forall G w k phi, Node G w k phi -> kbeg k <= kend k /\ kend k <= length w.
Proof.
  intros G w. exact (proj1 (node_edge_span_valid G w)).
Qed.

Lemma node_pos0_span :
  forall G w k phi, Node G w k phi -> kpos k = 0 -> kbeg k = kend k.
Proof.
  intros G w k phi H.
  induction H; unfold kinit; simpl; intros; solve [reflexivity | lia].
Qed.

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

(** ** The span-soundness invariant (the heart of Theorem 4.1) *)
Lemma node_sound_mut :
  forall G rk R,
    strict_stratified G rk ->
    rank_bound G rk R ->
    (exists alpha0, rules G (start G) = Some alpha0) ->
    forall w,
      (forall k phi, Node G w k phi ->
         (forall B, In B (nts_of (ksym k)) -> rk B < R) /\
         (csem G R w phi -> PrefixMatch G R w k)) /\
      (forall j k0, Edge G w j k0 ->
         kend j = kbeg k0 /\ In (ksym k0) (expects j G)).
Proof.
  intros G rk R Hstrat Hbound HstartD w.
  apply (NodeEdge_mut G w
    (fun k phi =>
       (forall B, In B (nts_of (ksym k)) -> rk B < R) /\
       (csem G R w phi -> PrefixMatch G R w k))
    (fun j k0 =>
       kend j = kbeg k0 /\ In (ksym k0) (expects j G))).
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
      * contradiction.
      * contradiction.
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
      * apply Hbnd. apply in_or_app. right. exact HB.
      * apply Hbnd. apply in_or_app. right. exact HB.
      * apply Hbnd. exact HB.
      * apply Hbnd. exact HB.
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
    intros j phi Y g e psi HEdge IHe HNj IHj HNY IHY Hnf.
    destruct IHe as [Hadj Hexp].
    destruct IHj as [Hbndj HPj].
    destruct IHY as [HbndY HPY].
    split.
    + simpl. exact Hbndj.
    + intros Hc. simpl in Hc.
      destruct Hc as [Hcphi [Hcpsi Hcgen]].
      specialize (HPj Hcphi). specialize (HPY Hcpsi).
      assert (HfinY : kfinal (mkKernel Y (last_pos Y) g e))
        by (unfold kfinal; simpl; reflexivity).
      pose proof (prefix_final _ _ _ _ HfinY HPY) as HMY.
      simpl in HMY.
      pose proof (node_span_valid _ _ _ _ HNj) as HspanJ.
      pose proof (node_span_valid _ _ _ _ HNY) as HspanY.
      pose proof (node_kpos_bound _ _ _ _ HNj) as Hkb.
      unfold kfinal in Hnf.
      unfold kinit in Hadj. simpl in Hadj.
      destruct j as [sj pj bj ej]; simpl in *.
      subst g.
      unfold expects in Hexp. unfold gencond in Hcgen.
      destruct sj; simpl in *.
      * (* s_term *)
        destruct pj as [|pj]; simpl in Hexp; [contradiction|lia].
      * (* s_eps *)
        lia.
      * (* s_nt *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct (rules G n) as [alpha|] eqn:EA; simpl in Hexp; [|contradiction].
        destruct Hexp as [Heq|[]]. subst Y.
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        eapply MO_nt; [exact EA|exact HMY].
      * (* s_seq *)
        destruct pj as [|[|pj]]; [| |lia].
        -- simpl in Hexp. destruct Hexp as [Heq|[]]. subst Y.
           assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
           subst bj.
           unfold PrefixMatch. simpl. exact HMY.
        -- simpl in Hexp. destruct Hexp as [Heq|[]]. subst Y.
           unfold PrefixMatch in HPj. simpl in HPj.
           unfold PrefixMatch. simpl. unfold CDGMatch in *.
           eapply MO_seq; [exact HPj|exact HMY].
      * (* s_alt *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        destruct Hexp as [Heq|[Heq|[]]]; subst Y.
        -- apply MO_alt_l. exact HMY.
        -- apply MO_alt_r. exact HMY.
      * (* s_join *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct Hexp as [Heq|[]]. subst Y.
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        simpl in Hcgen.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        apply MO_join; [exact HMY|exact Hcgen].
      * (* s_except *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct Hexp as [Heq|[]]. subst Y.
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        assert (Hz : forall B, In B (nts_of sj2) -> rk B < R)
          by (intros B HB; apply Hbndj, in_or_app; right; exact HB).
        simpl in Hcgen.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        apply MO_except; [exact HMY|].
        intro HEv. apply Hcgen.
        exact (proj1 (eval_match_agree G rk R Hstrat sj2 Hz w ej e) HEv).
      * (* s_la *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct Hexp as [Heq|[]]. subst Y.
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        assert (Hee : e = ej).
        { symmetry. eapply (node_pos0_span _ _ _ _ HNY). simpl. reflexivity. }
        subst e.
        simpl in Hcgen.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        destruct Hcgen as [g0 [Hg0 HMg]].
        eapply MO_la. exact HMg.
      * (* s_nla *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct Hexp as [Heq|[]]. subst Y.
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        assert (Hee : e = ej).
        { symmetry. eapply (node_pos0_span _ _ _ _ HNY). simpl. reflexivity. }
        subst e.
        simpl in Hcgen.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        apply MO_nla; [lia|].
        intros m HEv.
        pose proof (eval_span_valid _ (nt_level_valid G R) _ _ _ _ HEv) as [Hm1 _].
        apply (Hcgen m); [lia|].
        exact (proj1 (eval_match_agree G rk R Hstrat sj Hbndj w ej m) HEv).
      * (* s_longest *)
        destruct pj as [|pj]; [|lia]. simpl in Hexp.
        destruct Hexp as [Heq|[]]. subst Y.
        assert (Hbj : bj = ej) by (eapply (node_pos0_span _ _ _ _ HNj); reflexivity).
        subst bj.
        simpl in Hcgen.
        unfold PrefixMatch. simpl. unfold CDGMatch in *.
        apply MO_longest; [exact HMY|].
        intros m Hlt HEv.
        apply (Hcgen m); [lia|].
        exact (proj1 (eval_match_agree G rk R Hstrat sj Hbndj w ej m) HEv).
  - (* E_derive *)
    intros k phi Y HNode IH Hnf HY.
    split.
    + unfold kinit. simpl. reflexivity.
    + unfold kinit. simpl. exact HY.
Qed.

(** Theorem 4.1 (soundness of Naive ACP). *)
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

(** ** Completeness: every semantic match is charted *)

(** Application helpers with explicit equations (the chart constructors
    mention [kend]/[kpos] projections that unification does not unfold). *)
Lemma derive_init_at :
  forall G w k phi Y g,
    Node G w k phi -> ~ kfinal k -> In Y (expects k G) -> g = kend k ->
    Node G w (kinit Y g) c_true.
Proof. intros. subst g. eapply N_derive; eauto. Qed.

Lemma derive_edge_at :
  forall G w k phi Y g,
    Node G w k phi -> ~ kfinal k -> In Y (expects k G) -> g = kend k ->
    Edge G w k (kinit Y g).
Proof. intros. subst g. eapply E_derive; eauto. Qed.

Lemma progress_at :
  forall G w j phi Y g e psi X p b c,
    Edge G w j (kinit Y g) ->
    Node G w j phi ->
    Node G w (mkKernel Y (last_pos Y) g e) psi ->
    ~ kfinal j ->
    X = ksym j -> p = S (kpos j) -> b = kbeg j ->
    c = c_and phi (c_and psi (gencond (ksym j) (kbeg j) e)) ->
    Node G w (mkKernel X p b e) c.
Proof. intros. subst. eapply N_progress; eauto. Qed.

Lemma node_complete :
  forall G rk R,
    strict_stratified G rk ->
    forall w X i j,
      MatchO G (nt_level G R) X w i j ->
      (forall B, In B (nts_of X) -> rk B < R) ->
      Node G w (kinit X i) c_true ->
      exists phi,
        Node G w (mkKernel X (last_pos X) i j) phi /\ csem G R w phi.
Proof.
  intros G rk R Hstrat w X i j HM.
  induction HM; intros Hbnd HInit.
  - (* MO_term *)
    exists c_true. split; [|simpl; exact I].
    eapply N_term; [exact HInit|exact H].
  - (* MO_eps *)
    exists c_true. split; [|simpl; exact I].
    exact HInit.
  - (* MO_nt *)
    assert (HnA : rk A < R) by (apply Hbnd; left; reflexivity).
    assert (Hba : forall B, In B (nts_of alpha) -> rk B < R).
    { intros B HB. pose proof (stratified_pos _ _ _ _ _ Hstrat H HB). lia. }
    assert (Hnf : ~ kfinal (kinit (s_nt A) i))
      by (unfold kfinal, kinit; simpl; lia).
    assert (Hexp : In alpha (expects (kinit (s_nt A) i) G))
      by (unfold expects, kinit; simpl; rewrite H; left; reflexivity).
    assert (HInitA : Node G w (kinit alpha i) c_true)
      by (eapply derive_init_at; eauto).
    destruct (IHHM Hba HInitA) as [phi [HNa Hca]].
    assert (HEdge : Edge G w (kinit (s_nt A) i) (kinit alpha i))
      by (eapply derive_edge_at; eauto).
    eexists. split.
    + eapply (progress_at G w (kinit (s_nt A) i) c_true alpha i j phi);
        eauto; unfold kinit; simpl; reflexivity.
    + simpl. try unfold kinit. unfold gencond. simpl. repeat split.
      exact Hca.
  - (* MO_seq *)
    assert (Hba : forall B, In B (nts_of a) -> rk B < R)
      by (intros B HB; apply Hbnd, in_or_app; left; exact HB).
    assert (Hbb : forall B, In B (nts_of b) -> rk B < R)
      by (intros B HB; apply Hbnd, in_or_app; right; exact HB).
    assert (Hnf0 : ~ kfinal (kinit (s_seq a b) i))
      by (unfold kfinal, kinit; simpl; lia).
    assert (Hexp0 : In a (expects (kinit (s_seq a b) i) G))
      by (unfold expects, kinit; simpl; left; reflexivity).
    assert (HInitA : Node G w (kinit a i) c_true)
      by (eapply derive_init_at; eauto).
    destruct (IHHM1 Hba HInitA) as [phi1 [HNa Hca]].
    assert (HEdgeA : Edge G w (kinit (s_seq a b) i) (kinit a i))
      by (eapply derive_edge_at; eauto).
    assert (HN1 : Node G w (mkKernel (s_seq a b) 1 i k)
                    (c_and c_true (c_and phi1 c_true))).
    { eapply (progress_at G w (kinit (s_seq a b) i) c_true a i k phi1);
        eauto; unfold kinit; simpl; reflexivity. }
    assert (Hnf1 : ~ kfinal (mkKernel (s_seq a b) 1 i k))
      by (unfold kfinal; simpl; lia).
    assert (Hexp1 : In b (expects (mkKernel (s_seq a b) 1 i k) G))
      by (unfold expects; simpl; left; reflexivity).
    assert (HInitB : Node G w (kinit b k) c_true)
      by (eapply derive_init_at; eauto).
    destruct (IHHM2 Hbb HInitB) as [phi2 [HNb Hcb]].
    assert (HEdgeB : Edge G w (mkKernel (s_seq a b) 1 i k) (kinit b k))
      by (eapply derive_edge_at; eauto).
    eexists. split.
    + eapply (progress_at G w (mkKernel (s_seq a b) 1 i k)
               (c_and c_true (c_and phi1 c_true)) b k j phi2);
        eauto; simpl; reflexivity.
    + simpl. try unfold kinit. unfold gencond. simpl. repeat split.
      * exact Hca.
      * exact Hcb.
  - (* MO_alt_l *)
    assert (Hba : forall B, In B (nts_of a) -> rk B < R)
      by (intros B HB; apply Hbnd, in_or_app; left; exact HB).
    assert (Hnf : ~ kfinal (kinit (s_alt a b) i))
      by (unfold kfinal, kinit; simpl; lia).
    assert (Hexp : In a (expects (kinit (s_alt a b) i) G))
      by (unfold expects, kinit; simpl; left; reflexivity).
    assert (HInitA : Node G w (kinit a i) c_true)
      by (eapply derive_init_at; eauto).
    destruct (IHHM Hba HInitA) as [phi [HNa Hca]].
    assert (HEdge : Edge G w (kinit (s_alt a b) i) (kinit a i))
      by (eapply derive_edge_at; eauto).
    eexists. split.
    + eapply (progress_at G w (kinit (s_alt a b) i) c_true a i j phi);
        eauto; unfold kinit; simpl; reflexivity.
    + simpl. try unfold kinit. unfold gencond. simpl. repeat split.
      exact Hca.
  - (* MO_alt_r *)
    assert (Hbb : forall B, In B (nts_of b) -> rk B < R)
      by (intros B HB; apply Hbnd, in_or_app; right; exact HB).
    assert (Hnf : ~ kfinal (kinit (s_alt a b) i))
      by (unfold kfinal, kinit; simpl; lia).
    assert (Hexp : In b (expects (kinit (s_alt a b) i) G))
      by (unfold expects, kinit; simpl; right; left; reflexivity).
    assert (HInitB : Node G w (kinit b i) c_true)
      by (eapply derive_init_at; eauto).
    destruct (IHHM Hbb HInitB) as [phi [HNb Hcb]].
    assert (HEdge : Edge G w (kinit (s_alt a b) i) (kinit b i))
      by (eapply derive_edge_at; eauto).
    eexists. split.
    + eapply (progress_at G w (kinit (s_alt a b) i) c_true b i j phi);
        eauto; unfold kinit; simpl; reflexivity.
    + simpl. try unfold kinit. unfold gencond. simpl. repeat split.
      exact Hcb.
  - (* MO_join *)
    assert (Hba : forall B, In B (nts_of a) -> rk B < R)
      by (intros B HB; apply Hbnd, in_or_app; left; exact HB).
    assert (Hnf : ~ kfinal (kinit (s_join a b) i))
      by (unfold kfinal, kinit; simpl; lia).
    assert (Hexp : In a (expects (kinit (s_join a b) i) G))
      by (unfold expects, kinit; simpl; left; reflexivity).
    assert (HInitA : Node G w (kinit a i) c_true)
      by (eapply derive_init_at; eauto).
    destruct (IHHM1 Hba HInitA) as [phi [HNa Hca]].
    assert (HEdge : Edge G w (kinit (s_join a b) i) (kinit a i))
      by (eapply derive_edge_at; eauto).
    eexists. split.
    + eapply (progress_at G w (kinit (s_join a b) i) c_true a i j phi);
        eauto; unfold kinit; simpl; reflexivity.
    + simpl. try unfold kinit. unfold gencond. simpl. repeat split.
      * exact Hca.
      * exact HM2.
  - (* MO_except *)
    assert (Hba : forall B, In B (nts_of a) -> rk B < R)
      by (intros B HB; apply Hbnd, in_or_app; left; exact HB).
    assert (Hbb : forall B, In B (nts_of b) -> rk B < R)
      by (intros B HB; apply Hbnd, in_or_app; right; exact HB).
    assert (Hnf : ~ kfinal (kinit (s_except a b) i))
      by (unfold kfinal, kinit; simpl; lia).
    assert (Hexp : In a (expects (kinit (s_except a b) i) G))
      by (unfold expects, kinit; simpl; left; reflexivity).
    assert (HInitA : Node G w (kinit a i) c_true)
      by (eapply derive_init_at; eauto).
    destruct (IHHM Hba HInitA) as [phi [HNa Hca]].
    assert (HEdge : Edge G w (kinit (s_except a b) i) (kinit a i))
      by (eapply derive_edge_at; eauto).
    eexists. split.
    + eapply (progress_at G w (kinit (s_except a b) i) c_true a i j phi);
        eauto; unfold kinit; simpl; reflexivity.
    + simpl. try unfold kinit. unfold gencond. simpl. repeat split.
      * exact Hca.
      * intro HC. apply H. unfold CDGMatch in HC.
        exact (proj2 (eval_match_agree G rk R Hstrat b Hbb w i j) HC).
  - (* MO_la *)
    assert (Hnf : ~ kfinal (kinit (s_la a) i))
      by (unfold kfinal, kinit; simpl; lia).
    assert (Hexp : In s_eps (expects (kinit (s_la a) i) G))
      by (unfold expects, kinit; simpl; left; reflexivity).
    assert (HInitE : Node G w (kinit s_eps i) c_true)
      by (eapply derive_init_at; eauto).
    assert (HEdge : Edge G w (kinit (s_la a) i) (kinit s_eps i))
      by (eapply derive_edge_at; eauto).
    pose proof (match_span_valid _ _ _ _ _ _ HM) as [Hik _].
    eexists. split.
    + eapply (progress_at G w (kinit (s_la a) i) c_true s_eps i i c_true);
        eauto; unfold kinit; simpl; reflexivity.
    + simpl. try unfold kinit. unfold gencond. simpl. repeat split.
      exists k. split; [lia|exact HM].
  - (* MO_nla *)
    assert (Hnf : ~ kfinal (kinit (s_nla a) i))
      by (unfold kfinal, kinit; simpl; lia).
    assert (Hexp : In s_eps (expects (kinit (s_nla a) i) G))
      by (unfold expects, kinit; simpl; left; reflexivity).
    assert (HInitE : Node G w (kinit s_eps i) c_true)
      by (eapply derive_init_at; eauto).
    assert (HEdge : Edge G w (kinit (s_nla a) i) (kinit s_eps i))
      by (eapply derive_edge_at; eauto).
    eexists. split.
    + eapply (progress_at G w (kinit (s_nla a) i) c_true s_eps i i c_true);
        eauto; unfold kinit; simpl; reflexivity.
    + simpl. try unfold kinit. unfold gencond. simpl. repeat split.
      intros g Hg HC. apply (H0 g). unfold CDGMatch in HC.
      exact (proj2 (eval_match_agree G rk R Hstrat a Hbnd w i g) HC).
  - (* MO_longest *)
    assert (Hnf : ~ kfinal (kinit (s_longest a) i))
      by (unfold kfinal, kinit; simpl; lia).
    assert (Hexp : In a (expects (kinit (s_longest a) i) G))
      by (unfold expects, kinit; simpl; left; reflexivity).
    assert (HInitA : Node G w (kinit a i) c_true)
      by (eapply derive_init_at; eauto).
    destruct (IHHM Hbnd HInitA) as [phi [HNa Hca]].
    assert (HEdge : Edge G w (kinit (s_longest a) i) (kinit a i))
      by (eapply derive_edge_at; eauto).
    eexists. split.
    + eapply (progress_at G w (kinit (s_longest a) i) c_true a i j phi);
        eauto; unfold kinit; simpl; reflexivity.
    + simpl. try unfold kinit. unfold gencond. simpl. repeat split.
      * exact Hca.
      * intros g Hg HC. apply (H g); [lia|]. unfold CDGMatch in HC.
        exact (proj2 (eval_match_agree G rk R Hstrat a Hbnd w i g) HC).
Qed.

(** Theorem 4.2 (completeness of Naive ACP). *)
Theorem naive_acp_completeness :
  forall G rk R,
    strict_stratified G rk ->
    rank_bound G rk R ->
    (exists alpha, rules G (start G) = Some alpha) ->
    forall w,
      CDGMatch G R (s_nt (start G)) w 0 (length w) ->
      AcceptedBy G R w.
Proof.
  intros G rk R Hstrat Hbound HstartD w HM.
  assert (Hb : forall B, In B (nts_of (s_nt (start G))) -> rk B < R).
  { intros B HB. simpl in HB. destruct HB as [HB|[]]. subst B.
    destruct HstartD as [alpha0 Halpha0]. exact (Hbound _ _ Halpha0). }
  destruct (node_complete G rk R Hstrat w (s_nt (start G)) 0 (length w)
              HM Hb (N_start G w)) as [phi [HN Hc]].
  exists phi. split; [exact HN|exact Hc].
Qed.
