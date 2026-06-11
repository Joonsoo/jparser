(** * Decidability: the last clause of paper Theorem 3.3.

    [CDGMatch G R] is decidable on finite inputs for every grammar with
    FINITE SUPPORT (a list [dom] containing every defined nonterminal).
    Notably, stratification is NOT needed for decidability of the
    level-indexed relation: every level is decidable for an arbitrary
    finite grammar; stratification is what makes the levels stabilize
    into a canonical [Match] ([Agreement.v], [WellDefined.v]).

    Proof architecture (avoiding the false fuel-monotonicity route, cf.
    [MatchFuel.v]):

    - [matchb k]: height-indexed match.  [matchb] is monotone in [k]
      ([matchb_le]), sound for [MatchO] ([matchb_sound]), and each level
      is decidable ([matchb_dec]) because all quantifiers are bounded by
      the input length.
    - The items relevant to a query form a finite universe:
      (subexpression of the query or of some rule body) x (span).  The
      sets [S_k = { items | matchb k }] grow monotonically inside this
      finite universe, so some level [K <= M] (M = number of items)
      satisfies [S_K = S_{S K}] (counting pigeonhole, [mono_bounded_stab]).
    - A stabilized level is closed under the derivation rules, so by rule
      induction every [MatchO]-derivable item already satisfies
      [matchb K] ([stab_complete]) — and hence [matchb M] by monotonicity.
    - Therefore deciding [matchb M] decides [MatchO]
      ([matcho_dec_main]), and by recursion over levels the oracle
      [nt_level G R] is decidable ([nt_level_dec]), giving
      [cdgmatch_dec]. *)

From Stdlib Require Import Ascii String List Arith Lia.
Import ListNotations.
From CDG Require Import Syntax Stratification Match.

(** ** Generic helpers *)

Lemma dec_ex_le :
  forall (P : nat -> Prop),
    (forall x, {P x} + {~ P x}) ->
    forall n, {exists x, x <= n /\ P x} + {forall x, x <= n -> ~ P x}.
Proof.
  intros P Pdec n. induction n.
  - destruct (Pdec 0) as [H|H].
    + left. exists 0. split; [lia|exact H].
    + right. intros x Hx. assert (x = 0) by lia. subst. exact H.
  - destruct IHn as [IH|IH].
    + left. destruct IH as [x [Hx HP]]. exists x. split; [lia|exact HP].
    + destruct (Pdec (S n)) as [H|H].
      * left. exists (S n). split; [lia|exact H].
      * right. intros x Hx HP.
        destruct (Nat.eq_dec x (S n)) as [->|Hne].
        -- exact (H HP).
        -- apply (IH x); [lia|exact HP].
Qed.

Lemma filter_length_le :
  forall (A : Type) (f : A -> bool) (l : list A),
    length (filter f l) <= length l.
Proof.
  intros A f l. induction l as [|a l IH]; simpl; [lia|].
  destruct (f a); simpl; lia.
Qed.

Lemma filter_mono_length :
  forall (A : Type) (f g : A -> bool) (l : list A),
    (forall x, In x l -> f x = true -> g x = true) ->
    length (filter f l) <= length (filter g l).
Proof.
  intros A f g l H. induction l as [|a l IH]; simpl; [lia|].
  assert (Hl : forall x, In x l -> f x = true -> g x = true)
    by (intros x Hx; apply H; right; exact Hx).
  specialize (IH Hl).
  destruct (f a) eqn:Ef.
  - rewrite (H a (or_introl eq_refl) Ef). simpl. lia.
  - destruct (g a); simpl; lia.
Qed.

Lemma filter_mono_eq :
  forall (A : Type) (f g : A -> bool) (l : list A),
    (forall x, In x l -> f x = true -> g x = true) ->
    length (filter f l) = length (filter g l) ->
    forall x, In x l -> g x = true -> f x = true.
Proof.
  intros A f g l. induction l as [|a l IH]; simpl; intros Hfg Hlen x Hx Hgx.
  - destruct Hx.
  - assert (Hl : forall y, In y l -> f y = true -> g y = true)
      by (intros y Hy; apply Hfg; right; exact Hy).
    destruct (f a) eqn:Ef.
    + rewrite (Hfg a (or_introl eq_refl) Ef) in Hlen. simpl in Hlen.
      destruct Hx as [->|Hx]; [exact Ef|].
      apply (IH Hl); [lia|exact Hx|exact Hgx].
    + destruct (g a) eqn:Eg.
      * (* lengths force a contradiction: filter f l = S (filter g l) *)
        simpl in Hlen.
        pose proof (filter_mono_length A f g l Hl). lia.
      * destruct Hx as [->|Hx]; [congruence|].
        apply (IH Hl); [exact Hlen|exact Hx|exact Hgx].
Qed.

Lemma mono_bounded_stab :
  forall (f : nat -> nat) (M : nat),
    (forall k, f k <= f (S k)) ->
    (forall k, f k <= M) ->
    exists K, K <= M /\ f K = f (S K).
Proof.
  intros f M Hmono Hbound.
  destruct (dec_ex_le (fun k => f k = f (S k))
                      (fun k => Nat.eq_dec (f k) (f (S k))) M) as [H|H].
  - destruct H as [K [HK Heq]]. exists K. split; assumption.
  - exfalso.
    assert (Hgrow : forall k, k <= S M -> k + f 0 <= f k).
    { induction k; intro Hk; [lia|].
      assert (Hlt : f k < f (S k)).
      { pose proof (Hmono k). pose proof (H k ltac:(lia)). lia. }
      assert (Hk' : k <= S M) by lia.
      specialize (IHk Hk'). lia. }
    pose proof (Hgrow (S M) (le_n _)).
    pose proof (Hbound (S M)). lia.
Qed.

(** ** Decidability of the structural evaluator

    All quantifiers in [EvalN] range over match endpoints; for a valid
    oracle these are bounded by the input length, so bounded search
    decides them. *)

Lemma eval_dec :
  forall (N : nt_rel),
    nt_rel_valid N ->
    (forall A w i j, {N A w i j} + {~ N A w i j}) ->
    forall s w i j, {EvalN N s w i j} + {~ EvalN N s w i j}.
Proof.
  intros N Nvalid Ndec s.
  induction s; intros w i j; simpl.
  - (* term *)
    destruct (nth_error w i) as [c|] eqn:E.
    + destruct (ascii_dec a c) as [->|Hne].
      * destruct (Nat.eq_dec j (S i)) as [->|Hne2].
        -- left. split; reflexivity.
        -- right. intros [_ Hj]. exact (Hne2 Hj).
      * right. intros [H _]. congruence.
    + right. intros [H _]. congruence.
  - (* eps *)
    destruct (le_dec i (length w)) as [Hle|Hle].
    + destruct (Nat.eq_dec j i) as [->|Hne].
      * left. split; [exact Hle|reflexivity].
      * right. intros [_ Hj]. exact (Hne Hj).
    + right. intros [H _]. exact (Hle H).
  - (* nt *) apply Ndec.
  - (* seq *)
    assert (Pdec : forall m, {EvalN N s1 w i m /\ EvalN N s2 w m j} +
                             {~ (EvalN N s1 w i m /\ EvalN N s2 w m j)}).
    { intro m.
      destruct (IHs1 w i m) as [H1|H1]; [|right; intros [HA _]; exact (H1 HA)].
      destruct (IHs2 w m j) as [H2|H2]; [|right; intros [_ HB]; exact (H2 HB)].
      left; split; assumption. }
    destruct (dec_ex_le _ Pdec (length w)) as [Hyes|Hno].
    + left. destruct Hyes as [m [_ HP]]. exists m. exact HP.
    + right. intros [m [HA HB]].
      destruct (eval_span_valid N Nvalid s1 w i m HA) as [_ Hm].
      apply (Hno m Hm). split; assumption.
  - (* alt *)
    destruct (IHs1 w i j) as [H1|H1]; [left; left; exact H1|].
    destruct (IHs2 w i j) as [H2|H2]; [left; right; exact H2|].
    right. intros [H|H]; [exact (H1 H)|exact (H2 H)].
  - (* join *)
    destruct (IHs1 w i j) as [H1|H1]; [|right; intros [HA _]; exact (H1 HA)].
    destruct (IHs2 w i j) as [H2|H2]; [|right; intros [_ HB]; exact (H2 HB)].
    left; split; assumption.
  - (* except *)
    destruct (IHs1 w i j) as [H1|H1]; [|right; intros [HA _]; exact (H1 HA)].
    destruct (IHs2 w i j) as [H2|H2].
    + right. intros [_ HB]. exact (HB H2).
    + left. split; assumption.
  - (* la *)
    destruct (Nat.eq_dec j i) as [->|Hne];
      [|right; intros [Hj _]; exact (Hne Hj)].
    destruct (dec_ex_le (fun m => EvalN N s w i m)
                        (fun m => IHs w i m) (length w)) as [Hyes|Hno].
    + left. split; [reflexivity|].
      destruct Hyes as [m [_ HP]]. exists m. exact HP.
    + right. intros [_ [m Hm]].
      destruct (eval_span_valid N Nvalid s w i m Hm) as [_ Hmw].
      exact (Hno m Hmw Hm).
  - (* nla *)
    destruct (Nat.eq_dec j i) as [->|Hne];
      [|right; intros [Hj _]; exact (Hne Hj)].
    destruct (le_dec i (length w)) as [Hle|Hle];
      [|right; intros [_ [Hi _]]; exact (Hle Hi)].
    destruct (dec_ex_le (fun m => EvalN N s w i m)
                        (fun m => IHs w i m) (length w)) as [Hyes|Hno].
    + right. intros [_ [_ Hall]].
      destruct Hyes as [m [_ HP]]. exact (Hall m HP).
    + left. split; [reflexivity|]. split; [exact Hle|].
      intros m Hm.
      destruct (eval_span_valid N Nvalid s w i m Hm) as [_ Hmw].
      exact (Hno m Hmw Hm).
  - (* longest *)
    destruct (IHs w i j) as [H1|H1]; [|right; intros [HA _]; exact (H1 HA)].
    destruct (dec_ex_le (fun m => j < m /\ EvalN N s w i m)
                        (fun m =>
                           match lt_dec j m with
                           | left Hlt =>
                               match IHs w i m with
                               | left HE => left (conj Hlt HE)
                               | right HE => right (fun HP => HE (proj2 HP))
                               end
                           | right Hlt => right (fun HP => Hlt (proj1 HP))
                           end) (length w)) as [Hyes|Hno].
    + right. intros [_ Hall].
      destruct Hyes as [m [_ [Hlt HE]]]. exact (Hall m Hlt HE).
    + left. split; [exact H1|].
      intros m Hlt HE.
      destruct (eval_span_valid N Nvalid s w i m HE) as [_ Hmw].
      exact (Hno m Hmw (conj Hlt HE)).
Qed.

(** ** Subexpression enumeration *)

Fixpoint subexprs (s : sym) : list sym :=
  s :: match s with
       | s_seq a b | s_alt a b | s_join a b | s_except a b =>
           subexprs a ++ subexprs b
       | s_la a | s_nla a | s_longest a => subexprs a
       | _ => []
       end.

Lemma subexprs_refl : forall s, In s (subexprs s).
Proof. destruct s; simpl; left; reflexivity. Qed.

Lemma subexprs_trans :
  forall s t, In t (subexprs s) -> incl (subexprs t) (subexprs s).
Proof.
  induction s; simpl; intros t Ht.
  - destruct Ht as [Heq|[]]; subst t; apply incl_refl.
  - destruct Ht as [Heq|[]]; subst t; apply incl_refl.
  - destruct Ht as [Heq|[]]; subst t; apply incl_refl.
  - (* seq *)
    destruct Ht as [Heq|Ht]; [subst t; apply incl_refl|].
    apply in_app_or in Ht. destruct Ht as [Ht|Ht].
    + apply incl_tl, incl_appl. apply IHs1, Ht.
    + apply incl_tl, incl_appr. apply IHs2, Ht.
  - (* alt *)
    destruct Ht as [Heq|Ht]; [subst t; apply incl_refl|].
    apply in_app_or in Ht. destruct Ht as [Ht|Ht].
    + apply incl_tl, incl_appl. apply IHs1, Ht.
    + apply incl_tl, incl_appr. apply IHs2, Ht.
  - (* join *)
    destruct Ht as [Heq|Ht]; [subst t; apply incl_refl|].
    apply in_app_or in Ht. destruct Ht as [Ht|Ht].
    + apply incl_tl, incl_appl. apply IHs1, Ht.
    + apply incl_tl, incl_appr. apply IHs2, Ht.
  - (* except *)
    destruct Ht as [Heq|Ht]; [subst t; apply incl_refl|].
    apply in_app_or in Ht. destruct Ht as [Ht|Ht].
    + apply incl_tl, incl_appl. apply IHs1, Ht.
    + apply incl_tl, incl_appr. apply IHs2, Ht.
  - (* la *)
    destruct Ht as [Heq|Ht]; [subst t; apply incl_refl|].
    apply incl_tl. apply IHs, Ht.
  - (* nla *)
    destruct Ht as [Heq|Ht]; [subst t; apply incl_refl|].
    apply incl_tl. apply IHs, Ht.
  - (* longest *)
    destruct Ht as [Heq|Ht]; [subst t; apply incl_refl|].
    apply incl_tl. apply IHs, Ht.
Qed.

(** ** Height-indexed match, its decidability, and saturation *)

Section Decide.
  Variable G : grammar.
  Variable N : nt_rel.
  Hypothesis Nvalid : nt_rel_valid N.
  Hypothesis Ndec : forall A w i j, {N A w i j} + {~ N A w i j}.
  Variable dom : list nt_id.
  Hypothesis Hdom : forall A alpha, rules G A = Some alpha -> In A dom.

  (** Height-indexed match.  Negated operands use [EvalN N] exactly as in
      [MatchO]; the height only bounds the positive derivation.  The
      existentials carry explicit [<= length w] bounds (justified against
      [MatchO] by span validity) so that each level is decidable by
      bounded search. *)
  Fixpoint matchb (k : nat) (s : sym) (w : input) (i j : nat) {struct k} : Prop :=
    match k with
    | 0 => False
    | S k' =>
        match s with
        | s_term a => nth_error w i = Some a /\ j = S i
        | s_eps => i <= length w /\ j = i
        | s_nt A => exists alpha, rules G A = Some alpha /\ matchb k' alpha w i j
        | s_seq a b => exists m, m <= length w /\
                                 matchb k' a w i m /\ matchb k' b w m j
        | s_alt a b => matchb k' a w i j \/ matchb k' b w i j
        | s_join a b => matchb k' a w i j /\ matchb k' b w i j
        | s_except a b => matchb k' a w i j /\ ~ EvalN N b w i j
        | s_la a => j = i /\ exists m, m <= length w /\ matchb k' a w i m
        | s_nla a => j = i /\ i <= length w /\ (forall m, ~ EvalN N a w i m)
        | s_longest a => matchb k' a w i j /\
                         (forall m, j < m -> ~ EvalN N a w i m)
        end
    end.

  Lemma matchb_sound :
    forall k s w i j, matchb k s w i j -> MatchO G N s w i j.
  Proof.
    induction k; intros s w i j H; [destruct H|].
    destruct s; simpl in H.
    - destruct H as [Hn ->]. constructor. exact Hn.
    - destruct H as [Hle ->]. constructor. exact Hle.
    - destruct H as [alpha [Hr Hm]]. eapply MO_nt; [exact Hr|]. apply IHk, Hm.
    - destruct H as [m [_ [Ha Hb]]]. eapply MO_seq; apply IHk; eassumption.
    - destruct H as [Ha|Hb]; [apply MO_alt_l|apply MO_alt_r]; apply IHk; assumption.
    - destruct H as [Ha Hb]. apply MO_join; apply IHk; assumption.
    - destruct H as [Ha Hb]. apply MO_except; [apply IHk, Ha|exact Hb].
    - destruct H as [-> [m [_ Hm]]]. eapply MO_la. apply IHk, Hm.
    - destruct H as [-> [Hle Hall]]. apply MO_nla; assumption.
    - destruct H as [Ha Hall]. apply MO_longest; [apply IHk, Ha|exact Hall].
  Qed.

  Lemma matchb_le :
    forall k k', k <= k' ->
    forall s w i j, matchb k s w i j -> matchb k' s w i j.
  Proof.
    induction k; intros k' Hle s w i j H; [destruct H|].
    destruct k' as [|k'']; [lia|].
    assert (Hle' : k <= k'') by lia.
    destruct s; simpl in H |- *.
    - exact H.
    - exact H.
    - destruct H as [alpha [Hr Hm]]. exists alpha. split; [exact Hr|].
      apply (IHk k'' Hle'), Hm.
    - destruct H as [m [Hm [Ha Hb]]]. exists m. split; [exact Hm|].
      split; apply (IHk k'' Hle'); assumption.
    - destruct H as [Ha|Hb]; [left|right]; apply (IHk k'' Hle'); assumption.
    - destruct H as [Ha Hb]. split; apply (IHk k'' Hle'); assumption.
    - destruct H as [Ha Hb]. split; [apply (IHk k'' Hle'), Ha|exact Hb].
    - destruct H as [-> [m [Hm Ha]]]. split; [reflexivity|].
      exists m. split; [exact Hm|]. apply (IHk k'' Hle'), Ha.
    - exact H.
    - destruct H as [Ha Hall]. split; [apply (IHk k'' Hle'), Ha|exact Hall].
  Qed.

  Lemma matchb_dec :
    forall k s w i j, {matchb k s w i j} + {~ matchb k s w i j}.
  Proof.
    induction k; intros s w i j; [right; intro H; exact H|].
    destruct s; simpl.
    - (* term *)
      destruct (nth_error w i) as [c|] eqn:E.
      + destruct (ascii_dec a c) as [->|Hne].
        * destruct (Nat.eq_dec j (S i)) as [->|Hne2];
            [left; split; reflexivity
            |right; intros [_ Hj]; exact (Hne2 Hj)].
        * right. intros [H _]. congruence.
      + right. intros [H _]. congruence.
    - (* eps *)
      destruct (le_dec i (length w)) as [Hle|Hle];
        [|right; intros [Hi _]; exact (Hle Hi)].
      destruct (Nat.eq_dec j i) as [->|Hne];
        [left; split; [exact Hle|reflexivity]
        |right; intros [_ Hj]; exact (Hne Hj)].
    - (* nt *)
      destruct (rules G n) as [alpha|] eqn:E.
      + destruct (IHk alpha w i j) as [Hm|Hm].
        * left. exists alpha. split; [reflexivity|exact Hm].
        * right. intros [alpha' [E' Hm']].
          injection E' as Heq. subst alpha'. exact (Hm Hm').
      + right. intros [alpha [E' _]]. congruence.
    - (* seq *)
      assert (Pdec : forall m, {matchb k s1 w i m /\ matchb k s2 w m j} +
                               {~ (matchb k s1 w i m /\ matchb k s2 w m j)}).
      { intro m.
        destruct (IHk s1 w i m) as [H1|H1];
          [|right; intros [HA _]; exact (H1 HA)].
        destruct (IHk s2 w m j) as [H2|H2];
          [|right; intros [_ HB]; exact (H2 HB)].
        left; split; assumption. }
      destruct (dec_ex_le _ Pdec (length w)) as [Hyes|Hno].
      + left. destruct Hyes as [m [Hm HP]]. exists m. split; [exact Hm|exact HP].
      + right. intros [m [Hm HP]]. exact (Hno m Hm HP).
    - (* alt *)
      destruct (IHk s1 w i j) as [H1|H1]; [left; left; exact H1|].
      destruct (IHk s2 w i j) as [H2|H2]; [left; right; exact H2|].
      right. intros [H|H]; [exact (H1 H)|exact (H2 H)].
    - (* join *)
      destruct (IHk s1 w i j) as [H1|H1];
        [|right; intros [HA _]; exact (H1 HA)].
      destruct (IHk s2 w i j) as [H2|H2];
        [|right; intros [_ HB]; exact (H2 HB)].
      left; split; assumption.
    - (* except *)
      destruct (IHk s1 w i j) as [H1|H1];
        [|right; intros [HA _]; exact (H1 HA)].
      destruct (eval_dec N Nvalid Ndec s2 w i j) as [H2|H2].
      + right. intros [_ HB]. exact (HB H2).
      + left. split; assumption.
    - (* la *)
      destruct (Nat.eq_dec j i) as [->|Hne];
        [|right; intros [Hj _]; exact (Hne Hj)].
      assert (Pdec : forall m, {matchb k s w i m} + {~ matchb k s w i m})
        by (intro m; apply IHk).
      destruct (dec_ex_le _ Pdec (length w)) as [Hyes|Hno].
      + left. split; [reflexivity|].
        destruct Hyes as [m [Hm HP]]. exists m. split; [exact Hm|exact HP].
      + right. intros [_ [m [Hm HP]]]. exact (Hno m Hm HP).
    - (* nla *)
      destruct (Nat.eq_dec j i) as [->|Hne];
        [|right; intros [Hj _]; exact (Hne Hj)].
      destruct (le_dec i (length w)) as [Hle|Hle];
        [|right; intros [_ [Hi _]]; exact (Hle Hi)].
      destruct (dec_ex_le (fun m => EvalN N s w i m)
                          (fun m => eval_dec N Nvalid Ndec s w i m)
                          (length w)) as [Hyes|Hno].
      + right. intros [_ [_ Hall]].
        destruct Hyes as [m [_ HP]]. exact (Hall m HP).
      + left. split; [reflexivity|]. split; [exact Hle|].
        intros m Hm.
        destruct (eval_span_valid N Nvalid s w i m Hm) as [_ Hmw].
        exact (Hno m Hmw Hm).
    - (* longest *)
      destruct (IHk s w i j) as [H1|H1];
        [|right; intros [HA _]; exact (H1 HA)].
      destruct (dec_ex_le (fun m => j < m /\ EvalN N s w i m)
                          (fun m =>
                             match lt_dec j m with
                             | left Hlt =>
                                 match eval_dec N Nvalid Ndec s w i m with
                                 | left HE => left (conj Hlt HE)
                                 | right HE => right (fun HP => HE (proj2 HP))
                                 end
                             | right Hlt => right (fun HP => Hlt (proj1 HP))
                             end) (length w)) as [Hyes|Hno].
      + right. intros [_ Hall].
        destruct Hyes as [m [_ [Hlt HE]]]. exact (Hall m Hlt HE).
      + left. split; [exact H1|].
        intros m Hlt HE.
        destruct (eval_span_valid N Nvalid s w i m HE) as [_ Hmw].
        exact (Hno m Hmw (conj Hlt HE)).
  Qed.

  (** *** The finite expression universe of a query *)

  Definition body_exprs : list sym :=
    flat_map (fun A => match rules G A with
                       | Some alpha => subexprs alpha
                       | None => []
                       end) dom.

  Definition universe (s0 : sym) : list sym := subexprs s0 ++ body_exprs.

  Lemma U_query : forall s0, In s0 (universe s0).
  Proof.
    intro s0. unfold universe. apply in_or_app. left. apply subexprs_refl.
  Qed.

  Lemma U_subexprs :
    forall s0 t, In t (universe s0) -> incl (subexprs t) (universe s0).
  Proof.
    intros s0 t Ht. unfold universe in *.
    apply in_app_or in Ht. destruct Ht as [Ht|Ht].
    - apply incl_appl. apply subexprs_trans, Ht.
    - apply incl_appr.
      apply in_flat_map in Ht. destruct Ht as [A [HA Hin]].
      destruct (rules G A) as [alpha|] eqn:E; [|destruct Hin].
      intros x Hx.
      apply in_flat_map. exists A. split; [exact HA|].
      rewrite E. apply (subexprs_trans alpha t Hin), Hx.
  Qed.

  Lemma U_body :
    forall s0 A alpha, rules G A = Some alpha -> In alpha (universe s0).
  Proof.
    intros s0 A alpha Hr. unfold universe. apply in_or_app. right.
    apply in_flat_map. exists A. split; [exact (Hdom A alpha Hr)|].
    rewrite Hr. apply subexprs_refl.
  Qed.

  (** *** A stabilized level is complete *)

  Lemma stab_complete :
    forall (s0 : sym) (w : input) (K : nat),
      (forall t i j, In t (universe s0) -> i <= length w -> j <= length w ->
         matchb (S K) t w i j -> matchb K t w i j) ->
      forall t i j, In t (universe s0) ->
        MatchO G N t w i j -> matchb K t w i j.
  Proof.
    intros s0 w K Hstab t i j HU H. revert HU.
    induction H; intro HU.
    - (* term *)
      assert (Hi : i < length w)
        by (apply nth_error_Some; rewrite H; discriminate).
      apply Hstab; [exact HU|lia|lia|].
      simpl. split; [exact H|reflexivity].
    - (* eps *)
      apply Hstab; [exact HU|lia|lia|].
      simpl. split; [exact H|reflexivity].
    - (* nt *)
      destruct (match_span_valid _ _ _ _ _ _ H0) as [Hij Hj].
      assert (HUa : In alpha (universe s0)) by (eapply U_body; exact H).
      apply Hstab; [exact HU|lia|lia|].
      simpl. exists alpha. split; [exact H|]. exact (IHMatchO Hstab HUa).
    - (* seq *)
      destruct (match_span_valid _ _ _ _ _ _ H) as [Hik Hk].
      destruct (match_span_valid _ _ _ _ _ _ H0) as [Hkj Hj].
      assert (HUa : In a (universe s0)).
      { apply (U_subexprs s0 _ HU). simpl. right.
        apply in_or_app. left. apply subexprs_refl. }
      assert (HUb : In b (universe s0)).
      { apply (U_subexprs s0 _ HU). simpl. right.
        apply in_or_app. right. apply subexprs_refl. }
      apply Hstab; [exact HU|lia|lia|].
      simpl. exists k. split; [lia|].
      split; [exact (IHMatchO1 Hstab HUa)|exact (IHMatchO2 Hstab HUb)].
    - (* alt_l *)
      destruct (match_span_valid _ _ _ _ _ _ H) as [Hij Hj].
      assert (HUa : In a (universe s0)).
      { apply (U_subexprs s0 _ HU). simpl. right.
        apply in_or_app. left. apply subexprs_refl. }
      apply Hstab; [exact HU|lia|lia|].
      simpl. left. exact (IHMatchO Hstab HUa).
    - (* alt_r *)
      destruct (match_span_valid _ _ _ _ _ _ H) as [Hij Hj].
      assert (HUb : In b (universe s0)).
      { apply (U_subexprs s0 _ HU). simpl. right.
        apply in_or_app. right. apply subexprs_refl. }
      apply Hstab; [exact HU|lia|lia|].
      simpl. right. exact (IHMatchO Hstab HUb).
    - (* join *)
      destruct (match_span_valid _ _ _ _ _ _ H) as [Hij Hj].
      assert (HUa : In a (universe s0)).
      { apply (U_subexprs s0 _ HU). simpl. right.
        apply in_or_app. left. apply subexprs_refl. }
      assert (HUb : In b (universe s0)).
      { apply (U_subexprs s0 _ HU). simpl. right.
        apply in_or_app. right. apply subexprs_refl. }
      apply Hstab; [exact HU|lia|lia|].
      simpl. split; [exact (IHMatchO1 Hstab HUa)|exact (IHMatchO2 Hstab HUb)].
    - (* except *)
      destruct (match_span_valid _ _ _ _ _ _ H) as [Hij Hj].
      assert (HUa : In a (universe s0)).
      { apply (U_subexprs s0 _ HU). simpl. right.
        apply in_or_app. left. apply subexprs_refl. }
      apply Hstab; [exact HU|lia|lia|].
      simpl. split; [exact (IHMatchO Hstab HUa)|exact H0].
    - (* la *)
      destruct (match_span_valid _ _ _ _ _ _ H) as [Hik Hk].
      assert (HUa : In a (universe s0)).
      { apply (U_subexprs s0 _ HU). simpl. right. apply subexprs_refl. }
      apply Hstab; [exact HU|lia|lia|].
      simpl. split; [reflexivity|].
      exists k. split; [lia|]. exact (IHMatchO Hstab HUa).
    - (* nla *)
      apply Hstab; [exact HU|lia|lia|].
      simpl. split; [reflexivity|]. split; [exact H|exact H0].
    - (* longest *)
      destruct (match_span_valid _ _ _ _ _ _ H) as [Hij Hj].
      assert (HUa : In a (universe s0)).
      { apply (U_subexprs s0 _ HU). simpl. right. apply subexprs_refl. }
      apply Hstab; [exact HU|lia|lia|].
      simpl. split; [exact (IHMatchO Hstab HUa)|exact H0].
  Qed.

  (** *** The decision procedure *)

  Theorem matcho_dec_main :
    forall s0 w i j, {MatchO G N s0 w i j} + {~ MatchO G N s0 w i j}.
  Proof.
    intros s0 w i j.
    pose (U := universe s0).
    pose (n := length w).
    pose (items := list_prod U (list_prod (seq 0 (S n)) (seq 0 (S n)))).
    pose (M := length items).
    destruct (matchb_dec M s0 w i j) as [Hyes|Hno].
    - left. eapply matchb_sound. exact Hyes.
    - right. intro H. apply Hno. clear Hno.
      pose (fb := fun (k : nat) (it : sym * (nat * nat)) =>
                    if matchb_dec k (fst it) w (fst (snd it)) (snd (snd it))
                    then true else false).
      assert (Hfb : forall k it, fb k it = true <->
                      matchb k (fst it) w (fst (snd it)) (snd (snd it))).
      { intros k it. unfold fb.
        destruct (matchb_dec k (fst it) w (fst (snd it)) (snd (snd it))).
        - split; auto.
        - split; [discriminate|intro Hm; contradiction]. }
      assert (Hfg : forall k x, In x items -> fb k x = true -> fb (S k) x = true).
      { intros k x _ Hx. apply Hfb. apply Hfb in Hx.
        eapply matchb_le; [|exact Hx]. lia. }
      pose (ct := fun k => length (filter (fb k) items)).
      assert (Hmono : forall k, ct k <= ct (S k)).
      { intro k. apply filter_mono_length. apply Hfg. }
      assert (Hbound : forall k, ct k <= M)
        by (intro k; apply filter_length_le).
      destruct (mono_bounded_stab ct M Hmono Hbound) as [K [HKM Hstab]].
      assert (Hpt : forall t i' j', In t (universe s0) ->
                      i' <= length w -> j' <= length w ->
                      matchb (S K) t w i' j' -> matchb K t w i' j').
      { intros t i' j' HU Hi Hj Hm.
        assert (Hin : In (t, (i', j')) items).
        { unfold items. apply in_prod; [exact HU|].
          apply in_prod; apply in_seq; unfold n; lia. }
        pose proof (filter_mono_eq _ (fb K) (fb (S K)) items
                      (Hfg K) Hstab (t, (i', j')) Hin) as FE.
        apply (Hfb K (t, (i', j'))). apply FE.
        apply (Hfb (S K) (t, (i', j'))). exact Hm. }
      eapply matchb_le; [exact HKM|].
      eapply stab_complete; [exact Hpt|apply U_query|exact H].
  Qed.

End Decide.

(** ** Decidability of the stratum oracles and of [CDGMatch] *)

Lemma nt_level_dec :
  forall G (dom : list nt_id),
    (forall A alpha, rules G A = Some alpha -> In A dom) ->
    forall R A w i j, {nt_level G R A w i j} + {~ nt_level G R A w i j}.
Proof.
  intros G dom Hdom. induction R; intros A w i j.
  - right. intro H. exact H.
  - simpl.
    apply (matcho_dec_main G (nt_level G R) (nt_level_valid G R) (IHR)
                           dom Hdom (s_nt A) w i j).
Qed.

(** The decidability clause of paper Theorem 3.3.  Note the hypotheses:
    finite support suffices; stratification is not needed at a fixed
    level.  Under stratification, [CDGMatch G R] at a sufficient level is
    the canonical [Match] ([WellDefined.v]), so this also decides the
    paper's [Match]. *)
Theorem cdgmatch_dec :
  forall G (dom : list nt_id),
    (forall A alpha, rules G A = Some alpha -> In A dom) ->
    forall R s w i j, {CDGMatch G R s w i j} + {~ CDGMatch G R s w i j}.
Proof.
  intros G dom Hdom R s w i j.
  unfold CDGMatch.
  apply (matcho_dec_main G (nt_level G R) (nt_level_valid G R)
                         (nt_level_dec G dom Hdom R) dom Hdom s w i j).
Qed.
