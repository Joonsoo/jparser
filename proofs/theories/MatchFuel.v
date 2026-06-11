(** * MatchFuel: an executable, fuel-bounded match checker.

    [match_fuel] is a two-valued boolean decision procedure: [false]
    means "no match found within the fuel bound".  It serves as an
    executable test harness for the definitions ([Examples.v]) and as
    the intended decision procedure behind [WellDefined.cdgmatch_dec].

    CAUTION (correcting the original formalization plan): the plan
    proposed proving

      fuel1 <= fuel2 -> match_fuel fuel1 ... = true ->
      match_fuel fuel2 ... = true

    as the route to well-definedness.  That statement is FALSE in the
    presence of negation: increasing fuel can flip a fuel-starved inner
    result from [false] to [true], turning an enclosing [negb] from
    [true] to [false].  (Example: [s_except a b] where [b] needs deeper
    recursion than the inner fuel allows.)  Monotonicity holds only for
    the negation-free fragment.  The correct exactness statement is
    stratum-wise: for a strict stratified grammar there is a sufficient
    fuel — bounded by grammar size, input length, and stratification
    height — at which [match_fuel] computes exactly [CDGMatch].  That
    proof is future work; the checker itself is useful today as a
    mechanized unit-test oracle. *)

From Stdlib Require Import Ascii String List Arith Bool Lia.
Import ListNotations.
From CDG Require Import Syntax Match.

Fixpoint match_fuel (fuel : nat) (G : grammar) (s : sym) (w : input)
                    (i j : nat) {struct fuel} : bool :=
  match fuel with
  | 0 => false
  | S f =>
      match s with
      | s_term a =>
          match nth_error w i with
          | Some c => Ascii.eqb a c && (j =? S i)
          | None => false
          end
      | s_eps => (i <=? length w) && (j =? i)
      | s_nt A =>
          match rules G A with
          | Some alpha => match_fuel f G alpha w i j
          | None => false
          end
      | s_seq a b =>
          existsb (fun k => match_fuel f G a w i k && match_fuel f G b w k j)
                  (seq i (S (j - i)))
      | s_alt a b => match_fuel f G a w i j || match_fuel f G b w i j
      | s_join a b => match_fuel f G a w i j && match_fuel f G b w i j
      | s_except a b => match_fuel f G a w i j && negb (match_fuel f G b w i j)
      | s_la a =>
          (j =? i) && existsb (fun k => match_fuel f G a w i k)
                              (seq i (S (length w - i)))
      | s_nla a =>
          (j =? i) && (i <=? length w) &&
          forallb (fun k => negb (match_fuel f G a w i k))
                  (seq i (S (length w - i)))
      | s_longest a =>
          match_fuel f G a w i j &&
          forallb (fun k => negb (match_fuel f G a w i k))
                  (seq (S j) (length w - j))
      end
  end.
