(** * Syntax: CDG symbol expressions and grammars.

    Mechanization of paper Section 3 (Conditional Derivation Grammar).
    Corresponds to the expression syntax of CDG: the context-free skeleton
    (terminal, epsilon, nonterminal reference, concatenation, alternative)
    plus the five conditional operators (join, except, positive lookahead,
    negative lookahead, longest match). *)

From Stdlib Require Import Ascii String List Arith Lia.
Import ListNotations.

(** ** Nonterminal identifiers *)

Definition nt_id := nat.

(** ** Symbol expressions

    Paper notation:
    - [s_join a b]    : a & b   (same-span intersection)
    - [s_except a b]  : a - b   (same-span exclusion)
    - [s_la a]        : ^a      (positive lookahead, zero-width)
    - [s_nla a]       : !a      (negative lookahead, zero-width)
    - [s_longest a]   : <a>     (same-start longest match) *)

Inductive sym : Type :=
  | s_term    : ascii -> sym
  | s_eps     : sym
  | s_nt      : nt_id -> sym
  | s_seq     : sym -> sym -> sym
  | s_alt     : sym -> sym -> sym
  | s_join    : sym -> sym -> sym
  | s_except  : sym -> sym -> sym
  | s_la      : sym -> sym
  | s_nla     : sym -> sym
  | s_longest : sym -> sym.

(** ** Grammars

    A grammar maps nonterminal identifiers to their defining expression.
    Multiple productions [A -> a1 | a2] are represented as a single rule
    [A -> s_alt a1 a2].  The map is partial; the paper's finiteness of
    grammars is captured where needed by explicit rank bounds
    (see [WellDefined.rank_bound]). *)

Record grammar : Type := mkGrammar {
  rules : nt_id -> option sym;
  start : nt_id;
}.

(** ** Decidable equality *)

Lemma sym_eq_dec : forall s1 s2 : sym, {s1 = s2} + {s1 <> s2}.
Proof.
  decide equality.
  - apply ascii_dec.
  - apply Nat.eq_dec.
Defined.

(** ** Size measure (for induction on expressions) *)

Fixpoint sym_size (s : sym) : nat :=
  match s with
  | s_term _ | s_eps | s_nt _ => 1
  | s_seq a b | s_alt a b | s_join a b | s_except a b =>
      S (sym_size a + sym_size b)
  | s_la a | s_nla a | s_longest a => S (sym_size a)
  end.

Lemma sym_size_pos : forall s, 0 < sym_size s.
Proof. destruct s; simpl; lia. Qed.

(** ** Nonterminal occurrences *)

(** All nonterminals occurring anywhere in an expression. *)
Fixpoint nts_of (s : sym) : list nt_id :=
  match s with
  | s_term _ | s_eps => []
  | s_nt A => [A]
  | s_seq a b | s_alt a b | s_join a b | s_except a b => nts_of a ++ nts_of b
  | s_la a | s_nla a | s_longest a => nts_of a
  end.

(** Nonterminals occurring inside negation positions (paper Definition 3.1,
    [NegNT]).  A subexpression occurs in a negation position if it is the
    right operand of [s_except], or the operand of [s_nla] or [s_longest].
    All nonterminals anywhere inside such subexpressions are collected;
    positive lookahead [s_la] is NOT a negation position. *)
Fixpoint neg_nts (s : sym) : list nt_id :=
  match s with
  | s_term _ | s_eps | s_nt _ => []
  | s_seq a b | s_alt a b | s_join a b => neg_nts a ++ neg_nts b
  | s_except a b => neg_nts a ++ nts_of b
  | s_la a => neg_nts a
  | s_nla a => nts_of a
  | s_longest a => nts_of a
  end.

(** Negation positions only contain nonterminals that occur in the
    expression at all. *)
Lemma neg_nts_sub : forall s B, In B (neg_nts s) -> In B (nts_of s).
Proof.
  induction s; simpl; intros B H; try contradiction;
    try (apply in_app_or in H; apply in_or_app;
         destruct H; [left; auto | right; auto]);
    auto.
Qed.
