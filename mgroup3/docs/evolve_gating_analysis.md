# Evolve identity gate — leaf-by-leaf derivation

> 2026-07-09. step-5 evolve 게이팅 ("이벤트 root 와 disjoint 면 evolve 항등이므로
> skip") 조사·구현 기록. **결론: 게이트는 정확했으나 (11코퍼스 byte-identical +
> 전 스위트 그린) 이득 0.32% (jar 1.5%) 로 원복.** skip 40.6% 중 27%p 가
> Always (evolve 가 이미 O(1)) 라 non-trivial skip 은 13%p 뿐 — evolve 비용은
> skip 불가능한 이벤트-보유 조건에 집중. 재검토 트리거: 큰 composite pending
> 조건이 다수 지속되는 문법 변경 시. 아래는 구현 당시의 명제 도출 원문 —
> 이벤트 소스 전수와 leaf 별 항등 조건은 향후 evolve 관련 작업에 재사용 가치가
> 있어 보존한다. (성능 수치·검증 상세: mgroup3-native/PERFORMANCE.md "Dropped")

## Inputs to `evolve_accept_condition(cond, cpf, lcpf, active, gen)` at step5 (gen = next_gen)
- cpf  = cond_path_finishes      (eager, ends at gen)
- lcpf = late_cond_path_finishes (late, ends at gen-1)
- active = active_cond_roots = next_paths.keys()  (roots with a live path THIS step)
- gen  = next_gen (= ctx.gen_idx + 1)

## Goal: identify all cases where evolve(c) != c STRUCTURALLY.
For each leaf L referencing root R = (sym, sg):

### And / Or
evolve recurses on children then rebuilds via and_from/or_from.
- If EVERY child evolves to itself structurally → children Vec unchanged →
  and_from(children) may STILL differ from original `And{items}`?
  - and_from re-flattens/dedups/sorts. But the original condition was ALSO produced
    by and_from (all conditions in paths come from build_condition / and_from / or_from /
    apply_term_action combine). So it's already canonical: flatten idempotent, dedup
    idempotent, sort idempotent. Re-running and_from on an already-canonical items Vec
    yields the same Vec. HOWEVER children are re-cloned then rebuilt — result Eq to input.
  - Structural equality: canonical form is a normal form; and_from(canonical items) ==
    original. Confirmed by builder being deterministic + idempotent.
  => An And/Or evolves to itself IFF every child evolves to itself. Recurse.

### NoLongerMatch{sym,sg,minEnd}  (longest, root R)
```
if visiting(R): Always          <- can't happen at top-level (visiting starts empty)
parts=[]
if gen   >= minEnd && cpf[R]:  parts.push(neg(cpf[R]) evolved)
if gen-1 >= minEnd && lcpf[R]: parts.push(neg(lcpf[R]) evolved)
if active(R): parts.push(self)
if parts empty: Always else and_from(parts)
```
For evolve(NLM)==NLM structurally we need: parts == [self] exactly, i.e.
  - cpf[R] absent OR gen < minEnd
  - lcpf[R] absent OR gen-1 < minEnd
  - active(R) present
If active(R) absent → result is Always (or from other parts) != NLM. **DIFFERS.**
If cpf/lcpf present within window → extra part added != NLM. **DIFFERS.**
So NLM is identity ONLY IF: R has NO finish event this step (cpf/lcpf absent for R
  in the applicable window) AND R is active.
=> Event triggers for NLM: (a) cpf[R] present (with gen>=minEnd), (b) lcpf[R] present
   (with gen-1>=minEnd), (c) active(R) state — i.e. R NOT in active.

### NeedLongerMatch — dual of NLM (or_from, empty→Never). Same trigger set.
If active(R) absent and no fins → Never != NeedLM. Same conditions.

### NotExists{sym,sg} (lookahead, root R)
```
parts=[]
if cpf[R]:  parts.push(neg(cpf[R]) evolved)
if lcpf[R]: parts.push(neg(lcpf[R]) evolved)
if active(R): parts.push(self)
empty→Always else and_from(parts)
```
Identity IFF parts==[self]: cpf[R] absent, lcpf[R] absent, active(R) present.
Same as NLM but NO minEnd window (any finish triggers). Trigger: cpf[R]/lcpf[R]/¬active.

### Exists — dual. Same.

### Unless{sym,sg,end} (bounded, root R)  — TIME-ELAPSE SENSITIVE
```
if visiting(R): self
if gen < end:      self                          <- IDENTITY (self returned)
else if gen == end:
   if cpf[R]:   neg(cpf[R]) evolved
   elif active(R): self
   else:        Always
else if gen == end+1:
   if lcpf[R]:  neg(lcpf[R]) evolved
   else:        Always
else (gen > end+1): Always
```
CRITICAL: at gen==end with active(R) & no cpf → self (IDENTITY). Fine.
BUT at gen==end with NO cpf and NOT active → Always. And at gen>end → Always
regardless of events! **This is TIME-ELAPSE: even with zero events for R,
if gen > end (i.e. gen >= end+1 and no lcpf), Unless collapses to Always.**
Also at gen==end+1 no lcpf → Always (time-elapse).
So Unless is identity ONLY when gen < end (strictly before its span endpoint), OR
gen==end with active(R) and no cpf/... — but the latter has event dependence.
CONSERVATIVE RULE: Unless is time-elapse-safe-identity IFF gen < end_gen.
  When gen < end_gen the result is `self` UNCONDITIONALLY (independent of cpf/lcpf/active).
  When gen >= end_gen it can collapse to Always with no root event → NOT gated by root_set.

### OnlyIf — dual. gen < end → self unconditionally. gen>=end → collapse (Never) time-elapse.
CONSERVATIVE RULE: OnlyIf identity IFF gen < end_gen.

## SUMMARY of identity condition per leaf
| Leaf         | identity condition (structural evolve(c)==c)                          |
|--------------|-----------------------------------------------------------------------|
| Always/Never | always identity                                                       |
| NLM,NeedLM   | R active AND cpf[R] absent(or gen<minEnd) AND lcpf[R] absent(or gen-1<minEnd) |
| NotExists,Exists | R active AND cpf[R] absent AND lcpf[R] absent                     |
| Unless,OnlyIf| gen < end_gen  (then self returned unconditionally)                   |
| And/Or       | every child identity                                                  |

## The GATE (conservative, root-set based)
Define EVENT_ROOTS = { R : cpf.contains(R) } ∪ { R : lcpf.contains(R) } ∪ (roots that
  changed active-membership this step).

Problem: "active membership change" — active = next_paths.keys(). A root can LEAVE
active between prev step and this step with NO finish. NLM/NeedLM/NotExists/Exists all
depend on active(R): if their root drops out of active, they change (self part dropped).
So EVENT_ROOTS must ALSO include roots that were referenced-and-active last step but are
no longer active. BUT — the condition `c` we are evolving was already the *result* of
last step's evolve/filter. Its leaf's root R: was R active when c was produced?

KEY REALISATION — the pending (self) part is ONLY present in c if R was active when c was
last evolved. If R was NOT active last step, evolve would have DROPPED the self part, so
c would no longer contain a bare NLM/NotExists leaf for a dead R (it'd have collapsed to
Always/the finish-neg). Concretely: a live NLM{R} leaf in `c` implies R ∈ active at the
step c was produced (otherwise that leaf couldn't survive as a bare pending obligation).

So the persistence invariant: **if c (a path condition entering step5) contains a bare
NLM/NeedLM/NotExists/Exists leaf for root R, then R was active at the previous step.**
For evolve(c) to be identity this step we need R STILL active this step AND no finish.
"R still active" = R ∈ active_cond_roots (next_paths.keys()) this step.

Therefore the gate needs, for the leaf-roots of c:
  1. disjoint from EVENT_ROOTS (no cpf/lcpf finish) — the memory's `is_disjoint_from`.
  2. For NLM/NeedLM/NotExists/Exists leaves: R ∈ active_cond_roots (still active).
  3. For Unless/OnlyIf leaves: gen < end_gen.

Simplest conservative gate that is PROVABLY identity:
  root_set(c).is_disjoint_from(EVENT_ROOTS)   [handles cpf/lcpf events]
  AND root_set(c) ⊆ active_cond_roots         [handles active-drop for pending leaves]
  AND c contains no Unless/OnlyIf with gen >= end_gen   [handles time-elapse bounded]

But there's a subtlety with the bounded leaves and root_set/active:
- Unless/OnlyIf at gen < end return self UNCONDITIONALLY (they don't even look at active).
  So for a c that is PURELY bounded leaves with gen<end, disjointness+active not needed.
- But mixed conditions: gate must hold for ALL leaves.

## Cleanest formulation
Gate passes (skip evolve, reuse c) iff for EVERY leaf L in c:
  - L is Always/Never: ok
  - L is NLM/NeedLM/NotExists/Exists with root R:
       R ∉ EVENT_ROOTS  AND  R ∈ active_cond_roots
  - L is Unless/OnlyIf with end_gen e:  gen < e   (root-event-independent)

Since EVENT_ROOTS ⊆ (cpf keys ∪ lcpf keys), and R∈active is a per-root check,
we can compute this with a single walk of c OR with the precomputed root_set + a
scan for bounded end_gens. The memory suggests root_set + is_disjoint_from. But
root_set alone can't express "R active" or "Unless gen<end". So I will do a
purpose-built predicate `evolve_is_identity(c, cpf, lcpf, active, gen)` that walks
the tree ONCE and short-circuits — this is strictly cheaper than evolve (no clone,
no neg, no rebuild, early-out).

## Why a walk-predicate beats root_set+disjoint:
- root_set(c) already walks c to build a BTreeSet (alloc!). The predicate walks c
  without allocating and early-exits on first disqualifying leaf. Strictly cheaper.
- Need per-leaf info (kind, end_gen, active membership) anyway.

## Normalisation side-effects (Step 1.3)
evolve rebuilds And/Or through and_from/or_from even in the no-event case (children
map identity then rebuild). Could and_from(canonical items) != original And{items}?
- All conditions in paths are already canonical (produced by builders). Builders are
  idempotent + deterministic (flatten/dedup/sort are normal-form ops). So re-running
  yields Eq value. BUT the gate SKIPS evolve entirely and reuses the ORIGINAL c by
  clone — so we never even call and_from. The reused c is byte-identical to what was
  stored. No normalisation drift possible when we skip. (When we DON'T skip, behaviour
  is exactly as before.) => gate is safe by construction: skip ⟹ output = input c.

## Ungated (always evolve) cases — conservative exclusions
- Any Unless/OnlyIf leaf with gen >= end_gen  → evolve (time-elapse collapse).
- Any leaf whose root has a cpf/lcpf finish this step → evolve.
- Any pending leaf whose root left active → evolve.
- visiting-set recursion never triggers at top (starts empty); the predicate treats
  cyclic self-reference conservatively: a leaf whose neg(fin) would recurse only
  happens when cpf/lcpf present → already excluded. So visiting is moot for the gate.
