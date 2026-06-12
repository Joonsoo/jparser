(** * Evolution: operational accept-condition evolution (목표 A, phase 2).

    Phase 1 ([ACP.v]) discharges accept conditions semantically: [csem]
    reads each shape with its Match clause, quantifying over the entire
    input at once.  The implementations instead EVOLVE conditions
    generation by generation (the paper's evolution figure) and evaluate
    the residue at the end of the input.  This file mechanizes that
    staging.

    PHASE 2a (no pruning):
    - [evolve_step g] is one generation of evolution as a syntactic
      transformation: an unbounded watcher whose index has arrived
      ([c_exists]/[c_notexists] with [e = g]) absorbs the bounded fact
      about matches ending at [g] — as the corresponding bounded
      primitive — and advances its index.  Bounded primitives stay in
      place: their discharge is the chart lookup, covered by phase 1 and
      executable via [Decidability.cdgmatch_dec].
    - Every step preserves [csem] ([evolve_step_csem]).  After evolving
      through the whole input, every watcher's index lies beyond the
      input ([evolve_all_ge]), where [c_exists] fails and [c_notexists]
      holds vacuously — span validity is what makes the vacuous reading
      sound ([csem_exists_final]/[csem_notexists_final]).
    - End to end: evolving through generations 0..|w| and evaluating the
      residue with a boolean decision for bounded facts computes exactly
      [csem] ([op_eval_csem]); composed with Theorems 4.1/4.2 this gives
      an operational acceptance equivalent to the semantics
      ([op_accepted_iff], [op_accept_correct]).

    PHASE 2b (abstract early resolution):
    - With pruning, implementations resolve a watcher EARLY, as soon as
      its monitored root dies.  [evolve_step_pruned] models this against
      an oracle [dead] with the soundness hypothesis [dead_sound]: a dead
      root admits no future matches.  This hypothesis is EXACTLY the
      invariant a sound pruning must maintain — the invariant violated by
      the trimming bugs fixed in the implementations on 2026-06-12
      (monitored roots trimmed while their conditions still watched
      them).  Connecting a concrete reachability-based trimming to
      [dead_sound], and the implementations' recursion into looked-up
      condition trees (per-kernel joins), remain future work. *)

From Stdlib Require Import Ascii String List Arith Lia.
Import ListNotations.
From CDG Require Import Syntax Stratification Match Agreement WellDefined Decidability ACP.

Section Evolution.
  Variable G : grammar.
  Variable R : nat.
  Variable w : input.

  (** ** One generation of evolution *)

  Fixpoint evolve_step (g : nat) (c : cond) : cond :=
    match c with
    | c_and l r => c_and (evolve_step g l) (evolve_step g r)
    | c_or l r => c_or (evolve_step g l) (evolve_step g r)
    | c_exists b e X =>
        if e =? g then c_or (c_onlyif b g X) (c_exists b (S g) X) else c
    | c_notexists b e X =>
        if e =? g then c_and (c_unless b g X) (c_notexists b (S g) X) else c
    | _ => c
    end.

  (** ** The staging recurrences, semantically *)

  Lemma csem_exists_step :
    forall b e X,
      csem G R w (c_exists b e X) <->
      csem G R w (c_or (c_onlyif b e X) (c_exists b (S e) X)).
  Proof.
    intros b e X. simpl. split.
    - intros [g [Hg HM]].
      destruct (Nat.eq_dec g e) as [->|Hne].
      + left. exact HM.
      + right. exists g. split; [lia|exact HM].
    - intros [HM | [g [Hg HM]]].
      + exists e. split; [lia|exact HM].
      + exists g. split; [lia|exact HM].
  Qed.

  Lemma csem_notexists_step :
    forall b e X,
      csem G R w (c_notexists b e X) <->
      csem G R w (c_and (c_unless b e X) (c_notexists b (S e) X)).
  Proof.
    intros b e X. simpl. split.
    - intros Hall. split.
      + apply Hall. lia.
      + intros g Hg. apply Hall. lia.
    - intros [He Hnext] g Hg.
      destruct (Nat.eq_dec g e) as [->|Hne].
      + exact He.
      + apply Hnext. lia.
  Qed.

  (** Beyond the end of the input no match can end (span validity), so an
      expired [c_exists] is false and an expired [c_notexists] vacuous. *)

  Lemma csem_exists_final :
    forall b e X, length w < e -> ~ csem G R w (c_exists b e X).
  Proof.
    intros b e X He. simpl. intros [g [Hg HM]].
    unfold CDGMatch in HM.
    destruct (match_span_valid _ _ _ _ _ _ HM) as [_ Hj]. lia.
  Qed.

  Lemma csem_notexists_final :
    forall b e X, length w < e -> csem G R w (c_notexists b e X).
  Proof.
    intros b e X He. simpl. intros g Hg HM.
    unfold CDGMatch in HM.
    destruct (match_span_valid _ _ _ _ _ _ HM) as [_ Hj]. lia.
  Qed.

  (** ** Each step preserves the semantics *)

  Lemma evolve_step_csem :
    forall g c, csem G R w (evolve_step g c) <-> csem G R w c.
  Proof.
    intros g c. induction c; simpl; try tauto.
    - (* c_exists *)
      destruct (n0 =? g) eqn:E.
      + apply Nat.eqb_eq in E. subst n0.
        symmetry. apply csem_exists_step.
      + tauto.
    - (* c_notexists *)
      destruct (n0 =? g) eqn:E.
      + apply Nat.eqb_eq in E. subst n0.
        symmetry. apply csem_notexists_step.
      + tauto.
  Qed.

  (** ** Watcher indices advance past every processed generation *)

  Fixpoint watchers_ge (g : nat) (c : cond) : Prop :=
    match c with
    | c_and l r => watchers_ge g l /\ watchers_ge g r
    | c_or l r => watchers_ge g l /\ watchers_ge g r
    | c_exists _ e _ => g <= e
    | c_notexists _ e _ => g <= e
    | _ => True
    end.

  Lemma watchers_ge_0 : forall c, watchers_ge 0 c.
  Proof. induction c; simpl; auto; lia. Qed.

  Lemma evolve_step_ge :
    forall g c, watchers_ge g c -> watchers_ge (S g) (evolve_step g c).
  Proof.
    intros g c. induction c; simpl; intros H; auto; try tauto.
    - destruct (n0 =? g) eqn:E; simpl.
      + lia.
      + apply Nat.eqb_neq in E. lia.
    - destruct (n0 =? g) eqn:E; simpl.
      + lia.
      + apply Nat.eqb_neq in E. lia.
  Qed.

  (** ** Evolving through the whole input *)

  Definition evolve_all (n : nat) (c : cond) : cond :=
    fold_left (fun c' g => evolve_step g c') (seq 0 (S n)) c.

  Lemma evolve_fold_csem :
    forall gs c,
      csem G R w (fold_left (fun c' g => evolve_step g c') gs c) <->
      csem G R w c.
  Proof.
    induction gs as [|g gs IH]; intros c; simpl.
    - tauto.
    - rewrite IH. apply evolve_step_csem.
  Qed.

  Lemma evolve_all_csem :
    forall n c, csem G R w (evolve_all n c) <-> csem G R w c.
  Proof. intros. apply evolve_fold_csem. Qed.

  Lemma evolve_all_ge :
    forall n c, watchers_ge (S n) (evolve_all n c).
  Proof.
    intros n c. unfold evolve_all.
    assert (Hgen : forall len start c',
               watchers_ge start c' ->
               watchers_ge (start + len)
                 (fold_left (fun c'' g => evolve_step g c'') (seq start len) c')).
    { induction len as [|len IH]; intros start c' Hc'; simpl.
      - replace (start + 0) with start by lia. exact Hc'.
      - replace (start + S len) with (S start + len) by lia.
        apply IH. apply evolve_step_ge. exact Hc'. }
    replace (S n) with (0 + S n) by lia.
    apply Hgen. apply watchers_ge_0.
  Qed.

  (** ** Final evaluation of the residue *)

  Variable lkp : sym -> nat -> nat -> bool.
  Hypothesis lkp_correct :
    forall X b e, lkp X b e = true <-> CDGMatch G R X w b e.

  Fixpoint final_eval (c : cond) : bool :=
    match c with
    | c_true => true
    | c_false => false
    | c_and l r => final_eval l && final_eval r
    | c_or l r => final_eval l || final_eval r
    | c_onlyif b e X => lkp X b e
    | c_unless b e X => negb (lkp X b e)
    | c_exists _ _ _ => false
    | c_notexists _ _ _ => true
    end.

  Lemma final_eval_csem :
    forall c, watchers_ge (S (length w)) c ->
      (final_eval c = true <-> csem G R w c).
  Proof.
    induction c; simpl; intros Hge.
    - tauto.
    - split; [discriminate|tauto].
    - rewrite Bool.andb_true_iff. rewrite IHc1, IHc2 by tauto. tauto.
    - rewrite Bool.orb_true_iff. rewrite IHc1, IHc2 by tauto. tauto.
    - apply lkp_correct.
    - rewrite Bool.negb_true_iff.
      split.
      + intros Hf HM. apply lkp_correct in HM. congruence.
      + intros Hn. destruct (lkp s n n0) eqn:E; [|reflexivity].
        exfalso. apply Hn. apply lkp_correct. exact E.
    - (* c_exists: expired *)
      split; [discriminate|].
      intro Hc. exfalso.
      apply (csem_exists_final n n0 s); [lia|]. exact Hc.
    - (* c_notexists: vacuous *)
      split; [|reflexivity].
      intros _. apply csem_notexists_final. lia.
  Qed.

  (** ** End to end: the operational pipeline computes the semantics *)

  Definition op_eval (c : cond) : bool := final_eval (evolve_all (length w) c).

  Theorem op_eval_csem :
    forall c, op_eval c = true <-> csem G R w c.
  Proof.
    intro c. unfold op_eval.
    rewrite (final_eval_csem (evolve_all (length w) c) (evolve_all_ge (length w) c)).
    apply evolve_all_csem.
  Qed.

  (** The implementations absorb not the bounded primitive but the
      looked-up condition TREE of the final kernel.  At the level of the
      pipeline's meaning this is harmless: the outcome is invariant under
      replacing the condition by any [csem]-equivalent one ([csem] is
      compositional, so this extends to subtree substitution). *)
  Corollary op_eval_ext :
    forall c c',
      (csem G R w c <-> csem G R w c') ->
      (op_eval c = true <-> op_eval c' = true).
  Proof.
    intros c c' He.
    rewrite (op_eval_csem c), (op_eval_csem c'). exact He.
  Qed.

  (** ** Phase 2b (abstract): early resolution against a dead-root oracle

      With pruning, a watcher resolves as soon as its monitored root can
      no longer produce matches.  [dead X b g = true] must guarantee that
      no [X]-match from [b] ends at any generation [>= g]; this is the
      precise obligation of a sound pruning. *)

  Variable dead : sym -> nat -> nat -> bool.
  Hypothesis dead_sound :
    forall X b g, dead X b g = true ->
      forall g', g <= g' -> ~ CDGMatch G R X w b g'.

  Fixpoint evolve_step_pruned (g : nat) (c : cond) : cond :=
    match c with
    | c_and l r => c_and (evolve_step_pruned g l) (evolve_step_pruned g r)
    | c_or l r => c_or (evolve_step_pruned g l) (evolve_step_pruned g r)
    | c_exists b e X =>
        if e =? g then
          if dead X b g then c_false
          else if dead X b (S g) then c_onlyif b g X
          else c_or (c_onlyif b g X) (c_exists b (S g) X)
        else c
    | c_notexists b e X =>
        if e =? g then
          if dead X b g then c_true
          else if dead X b (S g) then c_unless b g X
          else c_and (c_unless b g X) (c_notexists b (S g) X)
        else c
    | _ => c
    end.

  Lemma csem_exists_dead :
    forall b e X,
      (forall g', e <= g' -> ~ CDGMatch G R X w b g') ->
      ~ csem G R w (c_exists b e X).
  Proof.
    intros b e X Hdead. simpl. intros [g [Hg HM]].
    exact (Hdead g Hg HM).
  Qed.

  Lemma csem_notexists_dead :
    forall b e X,
      (forall g', e <= g' -> ~ CDGMatch G R X w b g') ->
      csem G R w (c_notexists b e X).
  Proof.
    intros b e X Hdead. simpl. exact Hdead.
  Qed.

  Lemma evolve_step_pruned_csem :
    forall g c, csem G R w (evolve_step_pruned g c) <-> csem G R w c.
  Proof.
    intros g c. induction c; simpl; try tauto.
    - (* c_exists *)
      destruct (n0 =? g) eqn:E; [|tauto].
      apply Nat.eqb_eq in E. subst n0.
      destruct (dead s n g) eqn:ED.
      + (* dead now: the whole watcher is false *)
        simpl. split; [tauto|].
        intro Hc. exact (csem_exists_dead n g s (dead_sound _ _ _ ED) Hc).
      + destruct (dead s n (S g)) eqn:ED'.
        * (* dead from the next generation: only the absorbed fact remains *)
          simpl. split.
          -- intro HM. exists g. split; [lia|exact HM].
          -- intros [g' [Hg' HM]].
             destruct (Nat.eq_dec g' g) as [->|Hne]; [exact HM|].
             exfalso. exact (dead_sound _ _ _ ED' g' ltac:(lia) HM).
        * (* alive: same as the unpruned step *)
          symmetry. apply csem_exists_step.
    - (* c_notexists *)
      destruct (n0 =? g) eqn:E; [|tauto].
      apply Nat.eqb_eq in E. subst n0.
      destruct (dead s n g) eqn:ED.
      + simpl. split; [|tauto].
        intros _. exact (csem_notexists_dead n g s (dead_sound _ _ _ ED)).
      + destruct (dead s n (S g)) eqn:ED'.
        * simpl. split.
          -- intros Hu g' Hg' HM.
             destruct (Nat.eq_dec g' g) as [->|Hne]; [exact (Hu HM)|].
             exact (dead_sound _ _ _ ED' g' ltac:(lia) HM).
          -- intros Hall HM. exact (Hall g ltac:(lia) HM).
        * symmetry. apply csem_notexists_step.
  Qed.

End Evolution.

(** ** Acceptance through the operational pipeline

    Composing the pipeline with phase 1: operationally accepting runs of
    the chart coincide with [AcceptedBy], hence with the span semantics
    by Theorems 4.1/4.2. *)

Definition OpAccepted (G : grammar) (R : nat) (w : input)
                      (lkp : sym -> nat -> nat -> bool) : Prop :=
  exists phi,
    Node G w (mkKernel (s_nt (start G)) 1 0 (length w)) phi /\
    op_eval w lkp phi = true.

Theorem op_accepted_iff :
  forall G R w lkp,
    (forall X b e, lkp X b e = true <-> CDGMatch G R X w b e) ->
    (OpAccepted G R w lkp <-> AcceptedBy G R w).
Proof.
  intros G R w lkp Hl.
  unfold OpAccepted, AcceptedBy.
  split; intros [phi [HN Hc]]; exists phi; split; auto.
  - exact (proj1 (op_eval_csem G R w lkp Hl phi) Hc).
  - exact (proj2 (op_eval_csem G R w lkp Hl phi) Hc).
Qed.

(** With the executable bounded-fact decision from [Decidability.v], the
    pipeline decides exactly the span semantics. *)
Theorem op_accept_correct :
  forall G rk R (dom : list nt_id)
    (Hstrat : strict_stratified G rk)
    (Hbound : rank_bound G rk R)
    (Hdom : forall A alpha, rules G A = Some alpha -> In A dom)
    (HstartD : exists alpha, rules G (start G) = Some alpha),
    forall w,
      OpAccepted G R w
        (fun X b e => if cdgmatch_dec G dom Hdom R X w b e
                      then true else false) <->
      CDGMatch G R (s_nt (start G)) w 0 (length w).
Proof.
  intros G rk R dom Hstrat Hbound Hdom HstartD w.
  rewrite op_accepted_iff.
  - split.
    + intro HA. exact (naive_acp_soundness G rk R Hstrat Hbound HstartD w HA).
    + intro HM. exact (naive_acp_completeness G rk R Hstrat Hbound HstartD w HM).
  - intros X b e.
    destruct (cdgmatch_dec G dom Hdom R X w b e) as [H|H].
    + split; auto.
    + split; [discriminate|intro HM; contradiction].
Qed.
