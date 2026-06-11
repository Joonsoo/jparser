(** * Match: the CDG span semantics (paper Section 3).

    The semantics is built in three layers, mirroring the constructive
    reading of the paper's Theorem 3.3:

    - [EvalN N s w i j]: a structurally recursive span evaluator in which
      nonterminal references are resolved by an oracle [N].  It is used to
      evaluate operands in negation positions; under strict stratification
      those operands only mention lower-stratum nonterminals, for which
      the oracle is already fully determined.

    - [MatchO G N s w i j]: an inductively defined (least fixed point)
      match relation.  Positive structure — including nonterminal
      unfolding within the current stratum — is handled inductively;
      the three negative constructs ([s_except], [s_nla], [s_longest])
      consult [EvalN N] for their negated operand.  This is the paper's
      "monotone clauses given resolved lower strata".

    - [nt_level G r]: the oracle obtained by iterating [MatchO] [r] times
      starting from the empty relation, i.e. the relation after resolving
      [r] strata.  [CDGMatch G R] is [MatchO] at oracle [nt_level G R];
      for strict stratified grammars with ranks below [R] this is stable
      in [R] (see [Agreement.v]), which is the content of well-definedness.

    Note the deliberate design: [nt_level] does not mention the rank
    witness at all.  Ranks appear only in the stability theorems, which
    makes independence from the choice of stratification witness (paper
    Theorem 3.3, "independent of the choice of [≺]") a corollary instead
    of a separate construction. *)

From Stdlib Require Import Ascii String List Arith Lia.
Import ListNotations.
From CDG Require Import Syntax Stratification.

Definition input := list ascii.

(** Oracles: match relations for nonterminals (lower strata). *)
Definition nt_rel := nt_id -> input -> nat -> nat -> Prop.

Definition nt_bot : nt_rel := fun _ _ _ _ => False.

(** ** Structural evaluator with nonterminal oracle *)

Fixpoint EvalN (N : nt_rel) (s : sym) (w : input) (i j : nat) {struct s} : Prop :=
  match s with
  | s_term a    => nth_error w i = Some a /\ j = S i
  | s_eps       => i <= length w /\ j = i
  | s_nt A      => N A w i j
  | s_seq a b   => exists k, EvalN N a w i k /\ EvalN N b w k j
  | s_alt a b   => EvalN N a w i j \/ EvalN N b w i j
  | s_join a b  => EvalN N a w i j /\ EvalN N b w i j
  | s_except a b => EvalN N a w i j /\ ~ EvalN N b w i j
  | s_la a      => j = i /\ exists k, EvalN N a w i k
  | s_nla a     => j = i /\ i <= length w /\ (forall k, ~ EvalN N a w i k)
  | s_longest a => EvalN N a w i j /\ (forall k, j < k -> ~ EvalN N a w i k)
  end.

(** ** Inductive match relation, parameterized by the lower-strata oracle *)

Inductive MatchO (G : grammar) (N : nt_rel) : sym -> input -> nat -> nat -> Prop :=
  | MO_term : forall a w i,
      nth_error w i = Some a ->
      MatchO G N (s_term a) w i (S i)
  | MO_eps : forall w i,
      i <= length w ->
      MatchO G N s_eps w i i
  | MO_nt : forall A alpha w i j,
      rules G A = Some alpha ->
      MatchO G N alpha w i j ->
      MatchO G N (s_nt A) w i j
  | MO_seq : forall a b w i k j,
      MatchO G N a w i k ->
      MatchO G N b w k j ->
      MatchO G N (s_seq a b) w i j
  | MO_alt_l : forall a b w i j,
      MatchO G N a w i j ->
      MatchO G N (s_alt a b) w i j
  | MO_alt_r : forall a b w i j,
      MatchO G N b w i j ->
      MatchO G N (s_alt a b) w i j
  | MO_join : forall a b w i j,
      MatchO G N a w i j ->
      MatchO G N b w i j ->
      MatchO G N (s_join a b) w i j
  | MO_except : forall a b w i j,
      MatchO G N a w i j ->
      ~ EvalN N b w i j ->
      MatchO G N (s_except a b) w i j
  | MO_la : forall a w i k,
      MatchO G N a w i k ->
      MatchO G N (s_la a) w i i
  | MO_nla : forall a w i,
      i <= length w ->
      (forall k, ~ EvalN N a w i k) ->
      MatchO G N (s_nla a) w i i
  | MO_longest : forall a w i j,
      MatchO G N a w i j ->
      (forall k, j < k -> ~ EvalN N a w i k) ->
      MatchO G N (s_longest a) w i j.

(** ** Stratum iteration and the official match relation *)

Fixpoint nt_level (G : grammar) (r : nat) : nt_rel :=
  match r with
  | 0 => nt_bot
  | S r' => fun A w i j => MatchO G (nt_level G r') (s_nt A) w i j
  end.

(** The official match relation at stratification level [R].  For strict
    stratified grammars whose ranks are bounded by [R], this is the
    paper's [Match] (well-definedness = stability in [R], Agreement.v). *)
Definition CDGMatch (G : grammar) (R : nat) : sym -> input -> nat -> nat -> Prop :=
  MatchO G (nt_level G R).

(** ** Span validity

    Matches only relate valid spans: [i <= j <= length w].  The side
    conditions on [MO_eps] and [MO_nla] (and the corresponding [EvalN]
    clauses) restrict zero-width matches to positions inside the input;
    the paper quantifies spans as [0 <= i <= j <= |w|] globally, and this
    is where that constraint lands in the mechanization. *)

Lemma match_span_valid : forall G N s w i j,
  MatchO G N s w i j -> i <= j /\ j <= length w.
Proof.
  intros G N s w i j H; induction H; try lia.
  - (* term *)
    assert (i < length w).
    { apply nth_error_Some. rewrite H. discriminate. }
    lia.
Qed.

Definition nt_rel_valid (N : nt_rel) : Prop :=
  forall A w i j, N A w i j -> i <= j /\ j <= length w.

Lemma nt_bot_valid : nt_rel_valid nt_bot.
Proof. intros A w i j []. Qed.

Lemma eval_span_valid : forall N, nt_rel_valid N ->
  forall s w i j, EvalN N s w i j -> i <= j /\ j <= length w.
Proof.
  intros N HN s.
  induction s; simpl; intros w i j H.
  - (* term *) destruct H as [Hnth ->].
    assert (i < length w).
    { apply nth_error_Some. rewrite Hnth. discriminate. }
    lia.
  - (* eps *) destruct H as [Hle ->]. lia.
  - (* nt *) eapply HN; eauto.
  - (* seq *) destruct H as [k [Ha Hb]].
    apply IHs1 in Ha. apply IHs2 in Hb. lia.
  - (* alt *) destruct H as [Ha | Hb]; [apply IHs1 in Ha | apply IHs2 in Hb]; lia.
  - (* join *) destruct H as [Ha _]. apply IHs1 in Ha. lia.
  - (* except *) destruct H as [Ha _]. apply IHs1 in Ha. lia.
  - (* la *) destruct H as [-> [k Hk]]. apply IHs in Hk. lia.
  - (* nla *) destruct H as [-> [Hle _]]. lia.
  - (* longest *) destruct H as [Ha _]. apply IHs in Ha. lia.
Qed.

Lemma nt_level_valid : forall G r, nt_rel_valid (nt_level G r).
Proof.
  intros G r. induction r; simpl.
  - apply nt_bot_valid.
  - intros A w i j H. eapply match_span_valid; eauto.
Qed.

(** ** Per-operator clause lemmas

    Each CDG construct satisfies its defining clause at every fixed
    oracle.  These are the mechanized counterparts of the [Match] clauses
    of paper Section 3, and they decompose operator by operator: this is
    the shape that makes the accept-condition correspondence (paper
    Section 4) provable per operator.  In the three negative clauses the
    negated operand is evaluated by [EvalN N]; for strict stratified
    grammars at a sufficient level this coincides with the match relation
    itself ([Agreement.eval_match_agree]), recovering the paper's
    self-referential clause form ([WellDefined.v]). *)

Section Clauses.
  Variable G : grammar.
  Variable N : nt_rel.

  Lemma match_term_iff : forall a w i j,
    MatchO G N (s_term a) w i j <-> (nth_error w i = Some a /\ j = S i).
  Proof.
    split.
    - intro H; inversion H; subst; auto.
    - intros [Hnth ->]; constructor; auto.
  Qed.

  Lemma match_eps_iff : forall w i j,
    MatchO G N s_eps w i j <-> (i <= length w /\ j = i).
  Proof.
    split.
    - intro H; inversion H; subst; auto.
    - intros [Hle ->]; constructor; auto.
  Qed.

  Lemma match_nt_iff : forall A w i j,
    MatchO G N (s_nt A) w i j <->
    (exists alpha, rules G A = Some alpha /\ MatchO G N alpha w i j).
  Proof.
    split.
    - intro H; inversion H; subst; eauto.
    - intros [alpha [Hr Hm]]; econstructor; eauto.
  Qed.

  Lemma match_seq_iff : forall a b w i j,
    MatchO G N (s_seq a b) w i j <->
    (exists k, MatchO G N a w i k /\ MatchO G N b w k j).
  Proof.
    split.
    - intro H; inversion H; subst; eauto.
    - intros [k [Ha Hb]]; econstructor; eauto.
  Qed.

  Lemma match_alt_iff : forall a b w i j,
    MatchO G N (s_alt a b) w i j <->
    (MatchO G N a w i j \/ MatchO G N b w i j).
  Proof.
    split.
    - intro H; inversion H; subst; auto.
    - intros [Ha | Hb]; [apply MO_alt_l | apply MO_alt_r]; auto.
  Qed.

  Lemma match_join_iff : forall a b w i j,
    MatchO G N (s_join a b) w i j <->
    (MatchO G N a w i j /\ MatchO G N b w i j).
  Proof.
    split.
    - intro H; inversion H; subst; auto.
    - intros [Ha Hb]; constructor; auto.
  Qed.

  Lemma match_except_iff : forall a b w i j,
    MatchO G N (s_except a b) w i j <->
    (MatchO G N a w i j /\ ~ EvalN N b w i j).
  Proof.
    split.
    - intro H; inversion H; subst; auto.
    - intros [Ha Hb]; constructor; auto.
  Qed.

  Lemma match_la_iff : forall a w i j,
    MatchO G N (s_la a) w i j <->
    (j = i /\ exists k, MatchO G N a w i k).
  Proof.
    split.
    - intro H; inversion H; subst; eauto.
    - intros [-> [k Hk]]; econstructor; eauto.
  Qed.

  Lemma match_nla_iff : forall a w i j,
    MatchO G N (s_nla a) w i j <->
    (j = i /\ i <= length w /\ (forall k, ~ EvalN N a w i k)).
  Proof.
    split.
    - intro H; inversion H; subst; auto.
    - intros [-> [Hle Hk]]; constructor; auto.
  Qed.

  Lemma match_longest_iff : forall a w i j,
    MatchO G N (s_longest a) w i j <->
    (MatchO G N a w i j /\ (forall k, j < k -> ~ EvalN N a w i k)).
  Proof.
    split.
    - intro H; inversion H; subst; auto.
    - intros [Ha Hk]; constructor; auto.
  Qed.
End Clauses.
