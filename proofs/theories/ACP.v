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

Theorem naive_acp_soundness :
  forall G rk R,
    strict_stratified G rk ->
    rank_bound G rk R ->
    forall w,
      AcceptedBy G R w ->
      CDGMatch G R (s_nt (start G)) w 0 (length w).
Admitted.

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
