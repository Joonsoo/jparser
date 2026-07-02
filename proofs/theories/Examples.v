(** * Examples: mechanically verified unit tests for the CDG semantics.

    These examples check that the definitions of [Match.v] behave as the
    paper intends, covering the context-free skeleton and all five
    conditional operators:

    - [AnBn]: the CFL {a^n b^n} (skeleton sanity checks).
    - [kwG]: keyword exclusion [Word = Id - "if"], the paper's motivating
      use of [s_except], with a closed (nonterminal-free) negated operand.
    - [LongG]: maximal munch via [s_longest], with a recursive negated
      operand, exercising the stratified construction at rank 1.
    - [AnBnCn]: the non-CFL {a^n b^n c^n} via [s_join] (paper Section 3,
      Expressiveness).
    - [laG]: statement termination by an unconsumed '}' via [s_la]
      (paper Section 2's End rule).
    - [nlaG]: keyword boundary [If = "if" !Letter] via [s_nla], with a
      closed negated operand.

    Positive examples are proved for an arbitrary oracle [N] when the
    derivation is oracle-independent.  Boolean unit tests via
    [match_fuel] cover the negative cases cheaply. *)

From Stdlib Require Import Ascii String List Arith Lia.
Import ListNotations.
From CDG Require Import Syntax Stratification Match Agreement WellDefined MatchFuel.

(** ** {a^n b^n} *)

Definition AnBn : grammar :=
  mkGrammar
    (fun n =>
       match n with
       | 0 => Some (s_alt s_eps
                          (s_seq (s_term "a")
                                 (s_seq (s_nt 0) (s_term "b"))))
       | _ => None
       end)
    0.

Section AnBnExamples.
  Variable N : nt_rel.

  Example anbn_empty : MatchO AnBn N (s_nt 0) [] 0 0.
  Proof.
    eapply MO_nt; [reflexivity|].
    apply MO_alt_l. apply MO_eps. simpl. lia.
  Qed.

  Example anbn_ab : MatchO AnBn N (s_nt 0) ["a"%char; "b"%char] 0 2.
  Proof.
    eapply MO_nt; [reflexivity|].
    apply MO_alt_r.
    eapply MO_seq.
    - apply MO_term. reflexivity.
    - eapply MO_seq.
      + eapply MO_nt; [reflexivity|].
        apply MO_alt_l. apply MO_eps. simpl. lia.
      + apply MO_term. reflexivity.
  Qed.
End AnBnExamples.

(** ** Keyword exclusion: Word = Id - "if" *)

Definition kwG : grammar :=
  mkGrammar
    (fun n =>
       match n with
       | 0 (* Id     *) => Some (s_alt (s_nt 1) (s_seq (s_nt 1) (s_nt 0)))
       | 1 (* Letter *) => Some (s_alt (s_term "i")
                                       (s_alt (s_term "f") (s_term "a")))
       | 2 (* Word   *) => Some (s_except (s_nt 0)
                                          (s_seq (s_term "i") (s_term "f")))
       | _ => None
       end)
    2.

Section KeywordExamples.
  Variable N : nt_rel.

  (** "ia" is a Word: it is an Id and it is not the keyword "if".  The
      negated operand is closed, so the proof is oracle-independent. *)
  Example word_ia : MatchO kwG N (s_nt 2) ["i"%char; "a"%char] 0 2.
  Proof.
    eapply MO_nt; [reflexivity|].
    apply MO_except.
    - (* "ia" is an Id *)
      eapply MO_nt; [reflexivity|].
      apply MO_alt_r.
      eapply MO_seq.
      + eapply MO_nt; [reflexivity|].
        apply MO_alt_l. apply MO_term. reflexivity.
      + eapply MO_nt; [reflexivity|].   (* Id, single letter *)
        apply MO_alt_l.
        eapply MO_nt; [reflexivity|].   (* Letter = 'a' *)
        apply MO_alt_r. apply MO_alt_r. apply MO_term. reflexivity.
    - (* "ia" is not "if" *)
      simpl. intros [k [[H1 Hk] [H2 Hj]]].
      subst k. simpl in H2. discriminate H2.
  Qed.

  (** "if" is not a Word: the exclusion fires. *)
  Example word_if_no : ~ MatchO kwG N (s_nt 2) ["i"%char; "f"%char] 0 2.
  Proof.
    intro H.
    apply match_nt_iff in H. destruct H as [alpha [Hr Hm]].
    cbn in Hr. injection Hr as Heq. subst alpha.
    apply match_except_iff in Hm. destruct Hm as [_ Hneg].
    apply Hneg. cbn. exists 1.
    repeat split; reflexivity.
  Qed.
End KeywordExamples.

(** ** Maximal munch: L = <A>, A = a | a A *)

Definition LongG : grammar :=
  mkGrammar
    (fun n =>
       match n with
       | 0 (* A *) => Some (s_alt (s_term "a")
                                  (s_seq (s_term "a") (s_nt 0)))
       | 1 (* L *) => Some (s_longest (s_nt 0))
       | _ => None
       end)
    1.

(** [LongG] is strict stratified with rank = identity. *)
Example longG_stratified : strict_stratified LongG (fun n => n).
Proof.
  intros A alpha Hr.
  destruct A as [|[|A]]; simpl in Hr; inversion Hr; subst; split; simpl.
  - intros B [HB|[]]; subst; lia.
  - intros B [].
  - intros B [HB|[]]; subst; lia.
  - intros B [HB|[]]; subst; lia.
Qed.

Lemma a_matches_aa : forall N, MatchO LongG N (s_nt 0) ["a"%char; "a"%char] 0 2.
Proof.
  intro N.
  eapply MO_nt; [reflexivity|].
  apply MO_alt_r.
  eapply MO_seq.
  - apply MO_term. reflexivity.
  - eapply MO_nt; [reflexivity|].
    apply MO_alt_l. apply MO_term. reflexivity.
Qed.

(** The longest match (0,2) is accepted... *)
Example longest_aa : CDGMatch LongG 2 (s_nt 1) ["a"%char; "a"%char] 0 2.
Proof.
  unfold CDGMatch.
  eapply MO_nt; [reflexivity|].
  apply MO_longest.
  - apply a_matches_aa.
  - intros k Hk Hc.
    simpl in Hc.
    apply match_span_valid in Hc. simpl in Hc. lia.
Qed.

(** ...and the shorter match (0,1) is rejected: a longer A-match exists. *)
Example longest_aa_no : ~ CDGMatch LongG 2 (s_nt 1) ["a"%char; "a"%char] 0 1.
Proof.
  intro H. unfold CDGMatch in H.
  apply match_nt_iff in H. destruct H as [alpha [Hr Hm]].
  cbn in Hr. injection Hr as Heq. subst alpha.
  apply match_longest_iff in Hm. destruct Hm as [_ Hall].
  apply (Hall 2); [lia|].
  cbn. apply a_matches_aa.
Qed.

(** ** {a^n b^n c^n} via join (paper Section 3, Expressiveness) *)

Definition AnBnCn : grammar :=
  mkGrammar
    (fun n =>
       match n with
       | 0 (* S    *) => Some (s_join (s_nt 1) (s_nt 2))
       | 1 (* ABc  *) => Some (s_seq (s_nt 3) (s_nt 5))
       | 2 (* aBC  *) => Some (s_seq (s_nt 6) (s_nt 4))
       | 3 (* AnBn *) => Some (s_alt s_eps
                                     (s_seq (s_term "a")
                                            (s_seq (s_nt 3) (s_term "b"))))
       | 4 (* BnCn *) => Some (s_alt s_eps
                                     (s_seq (s_term "b")
                                            (s_seq (s_nt 4) (s_term "c"))))
       | 5 (* C*   *) => Some (s_alt s_eps (s_seq (s_term "c") (s_nt 5)))
       | 6 (* A*   *) => Some (s_alt s_eps (s_seq (s_term "a") (s_nt 6)))
       | _ => None
       end)
    0.

Definition wabc : input := ["a"%char; "b"%char; "c"%char].

Example abc_in : forall N, MatchO AnBnCn N (s_nt 0) wabc 0 3.
Proof.
  intro N.
  eapply MO_nt; [reflexivity|].
  apply MO_join.
  - (* ABc: a^n b^n followed by c* *)
    eapply MO_nt; [reflexivity|].
    eapply MO_seq with (k := 2).
    + (* AnBn matches "ab" *)
      eapply MO_nt; [reflexivity|].
      apply MO_alt_r.
      eapply MO_seq.
      * apply MO_term. reflexivity.
      * eapply MO_seq.
        -- eapply MO_nt; [reflexivity|].
           apply MO_alt_l. apply MO_eps. simpl. lia.
        -- apply MO_term. reflexivity.
    + (* C* matches "c" *)
      eapply MO_nt; [reflexivity|].
      apply MO_alt_r.
      eapply MO_seq.
      * apply MO_term. reflexivity.
      * eapply MO_nt; [reflexivity|].
        apply MO_alt_l. apply MO_eps. simpl. lia.
  - (* aBC: a* followed by b^n c^n *)
    eapply MO_nt; [reflexivity|].
    eapply MO_seq with (k := 1).
    + (* A* matches "a" *)
      eapply MO_nt; [reflexivity|].
      apply MO_alt_r.
      eapply MO_seq.
      * apply MO_term. reflexivity.
      * eapply MO_nt; [reflexivity|].
        apply MO_alt_l. apply MO_eps. simpl. lia.
    + (* BnCn matches "bc" *)
      eapply MO_nt; [reflexivity|].
      apply MO_alt_r.
      eapply MO_seq.
      * apply MO_term. reflexivity.
      * eapply MO_seq.
        -- eapply MO_nt; [reflexivity|].
           apply MO_alt_l. apply MO_eps. simpl. lia.
        -- apply MO_term. reflexivity.
Qed.

(** ** Lookahead: End = ';' | ^'}' (paper Section 2's statement terminator) *)

Definition laG : grammar :=
  mkGrammar
    (fun n =>
       match n with
       | 0 (* End *) => Some (s_alt (s_term ";") (s_la (s_term "}")))
       | _ => None
       end)
    0.

Section LookaheadExamples.
  Variable N : nt_rel.

  (** A following '}' terminates the statement without being consumed:
      [End] matches the empty span in front of it. *)
  Example la_end_brace : MatchO laG N (s_nt 0) ["}"%char] 0 0.
  Proof.
    eapply MO_nt; [reflexivity|].
    apply MO_alt_r.
    eapply MO_la.
    apply MO_term. reflexivity.
  Qed.

  (** With no '}' ahead, the zero-width alternative is refuted: the
      lookahead demands a witness that is not there. *)
  Example la_end_none : ~ MatchO laG N (s_nt 0) ["x"%char] 0 0.
  Proof.
    intro H.
    apply match_nt_iff in H. destruct H as [alpha [Hr Hm]].
    cbn in Hr. injection Hr as Heq. subst alpha.
    apply match_alt_iff in Hm. destruct Hm as [Hm | Hm].
    - inversion Hm.   (* ';' cannot match a zero-width span *)
    - apply match_la_iff in Hm. destruct Hm as [_ [k Hk]].
      inversion Hk; subst. cbn in *. discriminate.
  Qed.
End LookaheadExamples.

(** ** Negative lookahead: If = "if" !Letter (paper Section 2's keyword
    boundary) *)

Definition letterE : sym :=
  s_alt (s_term "i") (s_alt (s_term "f") (s_term "a")).

Definition nlaG : grammar :=
  mkGrammar
    (fun n =>
       match n with
       | 0 (* If *) => Some (s_seq (s_term "i")
                                   (s_seq (s_term "f") (s_nla letterE)))
       | _ => None
       end)
    0.

Section NegLookaheadExamples.
  Variable N : nt_rel.

  (** "if" at the end of the input is the keyword: no letter follows.
      The negated operand is closed, so the proof is oracle-independent. *)
  Example nla_if_keyword : MatchO nlaG N (s_nt 0) ["i"%char; "f"%char] 0 2.
  Proof.
    eapply MO_nt; [reflexivity|].
    eapply MO_seq with (k := 1).
    - apply MO_term. reflexivity.
    - eapply MO_seq with (k := 2).
      + apply MO_term. reflexivity.
      + apply MO_nla.
        * cbn. lia.
        * intros k H. cbn in H.
          destruct H as [[Hn _] | [[Hn _] | [Hn _]]]; discriminate Hn.
  Qed.

  (** "if" followed by a letter is not the keyword: the boundary
      lookahead is violated by the very next character. *)
  Example nla_if_no :
    ~ MatchO nlaG N (s_nt 0) ["i"%char; "f"%char; "a"%char] 0 2.
  Proof.
    intro H.
    apply match_nt_iff in H. destruct H as [alpha [Hr Hm]].
    cbn in Hr. injection Hr as Heq. subst alpha.
    apply match_seq_iff in Hm. destruct Hm as [k1 [Hi Hm]].
    inversion Hi; subst.
    apply match_seq_iff in Hm. destruct Hm as [k2 [Hf Hm]].
    inversion Hf; subst.
    apply match_nla_iff in Hm. destruct Hm as [_ [_ Hno]].
    apply (Hno 3). cbn.
    right. right. split; reflexivity.
  Qed.
End NegLookaheadExamples.

(** ** Boolean unit tests via the fuel checker *)

Definition waabbcc : input :=
  ["a"%char; "a"%char; "b"%char; "b"%char; "c"%char; "c"%char].
Definition waabbc : input :=
  ["a"%char; "a"%char; "b"%char; "b"%char; "c"%char].

Example fuel_anbn_ab :
  match_fuel 50 AnBn (s_nt 0) ["a"%char; "b"%char] 0 2 = true.
Proof. vm_compute. reflexivity. Qed.

Example fuel_anbn_a :
  match_fuel 50 AnBn (s_nt 0) ["a"%char] 0 1 = false.
Proof. vm_compute. reflexivity. Qed.

Example fuel_word_ia :
  match_fuel 50 kwG (s_nt 2) ["i"%char; "a"%char] 0 2 = true.
Proof. vm_compute. reflexivity. Qed.

Example fuel_word_if :
  match_fuel 50 kwG (s_nt 2) ["i"%char; "f"%char] 0 2 = false.
Proof. vm_compute. reflexivity. Qed.

Example fuel_longest_aa :
  match_fuel 50 LongG (s_nt 1) ["a"%char; "a"%char] 0 2 = true.
Proof. vm_compute. reflexivity. Qed.

Example fuel_longest_aa_no :
  match_fuel 50 LongG (s_nt 1) ["a"%char; "a"%char] 0 1 = false.
Proof. vm_compute. reflexivity. Qed.

Example fuel_nla_pos :
  match_fuel 50 AnBn (s_nla (s_term "b")) ["a"%char] 0 0 = true.
Proof. vm_compute. reflexivity. Qed.

Example fuel_nla_neg :
  match_fuel 50 AnBn (s_nla (s_term "a")) ["a"%char] 0 0 = false.
Proof. vm_compute. reflexivity. Qed.

Example fuel_la_end :
  match_fuel 50 laG (s_nt 0) ["}"%char] 0 0 = true.
Proof. vm_compute. reflexivity. Qed.

Example fuel_la_end_no :
  match_fuel 50 laG (s_nt 0) ["x"%char] 0 0 = false.
Proof. vm_compute. reflexivity. Qed.

Example fuel_if_keyword :
  match_fuel 50 nlaG (s_nt 0) ["i"%char; "f"%char] 0 2 = true.
Proof. vm_compute. reflexivity. Qed.

Example fuel_if_keyword_no :
  match_fuel 50 nlaG (s_nt 0) ["i"%char; "f"%char; "a"%char] 0 2 = false.
Proof. vm_compute. reflexivity. Qed.

Example fuel_abc :
  match_fuel 50 AnBnCn (s_nt 0) wabc 0 3 = true.
Proof. vm_compute. reflexivity. Qed.

Example fuel_aabbcc :
  match_fuel 60 AnBnCn (s_nt 0) waabbcc 0 6 = true.
Proof. vm_compute. reflexivity. Qed.

Example fuel_aabbc :
  match_fuel 60 AnBnCn (s_nt 0) waabbc 0 5 = false.
Proof. vm_compute. reflexivity. Qed.
