# mgroup3 Parsing Algorithm

This document describes the parsing algorithm of *mgroup3* — the third
generation of the *milestone-group* parser family in jparser. It assumes
familiarity with general parsing/automata terminology (CFG, derive,
reduce, GLR-style chart parsing) and with CDG (Conditional Derivation
Grammar) concepts (the various accept conditions: longest match, lookahead,
except, join). It does **not** assume any prior knowledge of the
milestone-group parsers themselves.

## 1. Background — milestone parsers in jparser

A *naive* CDG parser (`NaiveParser` / `NaiveParser2` in jparser)
maintains a parsing context whose nodes are kernels — `(symbol, pointer,
startGen, endGen)` quadruples — and edges are the standard chart-parser
relationships (derive, sequence-progress, finish). The naive parser
walks this graph step by step as input characters are consumed.

The naive context graph is exact but expensive: each node carries
generation indices that distinguish positions in the input, so the graph
keeps growing as parsing proceeds.

The *milestone parsers* (milestone, mgroup1, mgroup2, and now mgroup3)
exploit a structural observation about CDG parsing graphs: only a small
subset of every node's information is needed to determine the parser's
future behaviour. Concretely, given a path leading to the current "tip"
of the parser, the only thing that matters is the chain of *milestones*
along the path — kernels that are about to consume more input. The rest
of the graph can be computed lazily from the kernel templates.

A *milestone* is therefore a `(symbol, pointer, gen)` kernel that lives
on the boundary between "already matched" and "still to match". A
*milestone group* is a set of milestones that share the same parsing
state — equivalent under the parser's transition function. Grouping
collapses the explosion of equivalent node configurations into a single
abstract state, much like an LR parser collapses derivations into
states.

A *milestone-group path* (or just *path*) is the data the parser
actually carries at runtime:

```
path := root | path → milestone(symbol, pointer, gen, observingCondSymbolIds) → tipGroup(milestoneGroupId)
```

The "head" of the path is a milestone group id (the *tip*); behind it
is a chain of concrete milestones from earlier in the parse. Parsing
proceeds by consuming an input character, which makes the tip transition
to a new milestone group, possibly extending or shortening the chain
behind it.

## 2. mgroup3 design goals

mgroup3 keeps the basic milestone-group structure of mgroup1/2 but
re-thinks how *accept conditions* are represented and evaluated. In
mgroup1/2, accept conditions (longest match, lookahead, except, join)
were implemented by reusing the naive parser's graph during
interpretation, leading to substantial complexity in the runtime.

mgroup3 instead splits the parser state into two kinds of paths:

- **Main paths** — the live parses for the start symbol.
- **Cond paths** — separate parser instances for each accept-condition
  *root symbol* that needs to be tracked. A "cond root" is a
  `(symbol, startGen)` pair: the symbol whose derivation determines
  whether the condition is satisfied, started at generation `startGen`.

A condition is then evaluated by inspecting the cond paths: did the
relevant cond root finish? did it finish *after* the matching gen? did
it remain active past a particular gen? The runtime question is reduced
to "look up cond root state in a separate parser context", instead of
reconstructing the naive graph on the fly.

The trade-off is potentially more state to maintain (cond paths grow
in addition to main paths), but each piece of state has a localised
meaning and the runtime logic is dramatically simpler.

## 3. Data structures

### 3.1 Generated parser data

The generator emits a flat data structure (`Mgroup3ParserData` proto)
containing:

- `pathRoots: map<symbolId, PathRootInfo>` — for each symbol that may
  serve as a path root (start symbol, every cond symbol referenced in
  any condition), an entry describing the initial milestone group, the
  initial parsing actions, the self-finish condition (if the symbol is
  nullable), and the set of *initial cond symbols* (cond symbols
  reachable purely from deriving this symbol).
- `milestoneGroups: int → set<KernelTemplate>` — the canonical mapping
  between a milestone-group id and the set of `(symbol, pointer)`
  kernel templates it represents.
- `termActions: int → list<(termGroup, TermAction)>` — for each
  milestone group, the per-character transitions. A `TermAction`
  describes the effect of consuming one input character that falls in a
  given term group (a class of equivalent characters).
- `tipEdgeActions: (parentTemplate, tipGroupId) → EdgeAction` and
  `midEdgeActions: (parentTemplate, tipTemplate) → EdgeAction` —
  reductions that propagate when the tip's progression is complete.
  These are looked up after a `replaceAndProgresses` term action fires.

Every `AppendMilestoneGroup` entry inside a term or edge action
additionally carries a list of `CondRootStarter { symbolId,
milestoneGroupId }` records — the cond-root symbols whose starter
must be created (in parallel with the main path) whenever this
milestone group is attached. This mirrors mgroup2's
`lookahead_requiring_symbols`. The generator populates it from two
sources:

1. `g2.observingCondSymbolIds` — cond symbols reached during the
   step's progress phase.
2. The new milestone group's own `derivedFrom` graph — cond symbols
   inside the still-to-be-matched body of the new group, like the
   NJoin/NLongest inside a `"||" & OpTk` that the main path is
   *about* to start consuming.

Without (2) the parallel starter would not be created at the moment
the main path begins matching a multi-character NJoin body and would
miss the body's prefix.

A `TermAction` has three fields:

- `replaceAndAppends: list<{replace, append}>` — the tip's milestone
  progresses to `replace`, and a fresh milestone group `append` is
  appended on top, becoming the new tip. The tip from before becomes a
  concrete milestone in the path.
- `replaceAndProgresses: list<{replaceMgroup, acceptCondition}>` —
  the tip is fully consumed (its sequence reaches the end). The parser
  must then fold up: look up the parent's edge action and apply it,
  potentially cascading through several mid edges until the tip is
  re-rooted.
- `parsingActions: {finished, progressed}` — the kernels finished or
  progressed during this transition. These records are not needed for
  the parser to *advance* — the abstract milestone-group transition is
  sufficient — but they are kept for cond-condition evaluation and for
  AST reconstruction.

An `EdgeAction` mirrors `TermAction` for the case where reductions
propagate up the path.

### 3.2 Accept condition templates

Each accept condition in the grammar is reified into an
`AcceptConditionTemplate` proto value:

- `Always`, `Never`, `And(...)`, `Or(...)` — boolean structure.
- `NoLongerMatch(symbolId, startGenTag)` — longest-match condition:
  cond root `(symbolId, ?)` should not finish at any generation
  *strictly later* than the gen at which this condition was emitted.
- `LookaheadFound(symbolId, startGenTag)` and
  `LookaheadNotFound(symbolId, startGenTag)` — positive/negative
  lookahead.
- `Except(symbolId, startGenTag)` — except condition: cond root must
  not finish from `startGen` onwards.
- `Join(symbolId, startGenTag)` — join condition: cond root must
  finish from `startGen` onwards (compatible with the main path's
  finish gen).

The `startGenTag` is a `KernelTemplateGen` enum (`CURR / MID / NEXT`)
which the parser resolves to a concrete `gen` at runtime using three
context values (`prevGen`, `midGen`, `gen`). The tag corresponds to the
position of the cond root in the parser graph at *generator* time —
specifically, to the `startGen` of the `GenNode` from which the
condition was emitted (= the *derive site* of the atomic symbol that
introduced the condition). This makes the condition's start gen fully
determined by the static graph, with no runtime heuristics required.

### 3.3 Runtime context

The runtime carries:

```
ParsingCtx:
  gen, line, col            -- current position
  mainRoot: PathRoot         -- (startSymbolId, 0)
  mainPaths: PathMap
  condPaths: Map<PathRoot, PathMap>
  history: List<HistoryEntry>

PathShape:
  milestonePath: MilestonePath?
  tipGroupId: Int

PathMap = Map<PathShape, AcceptCondition>
```

A path's *shape* (its position in the milestone graph) is separated
from the *condition* under which the parser reached that shape. Two
sources that produce the same shape with different conditions are
merged at insertion time into a single entry whose value is their
disjunction — dedup is structural rather than a separate pass.

- `milestonePath` is `null` for a *starter* (a freshly-spawned cond path
  whose tip is the root milestone group), otherwise it is a linked list
  of `MilestonePath` nodes, each carrying its own `gen`, the milestone
  kernel, and the set of cond symbols this milestone wants to observe.
  `MilestonePath` caches its `hashCode` lazily so that `PathShape` keys
  do not pay the linked-list traversal cost on every map operation.
- `tipGroupId` identifies the current milestone group at the tip.
- The map value, an `AcceptCondition` (the runtime form of
  `AcceptConditionTemplate`, with concrete generations substituted),
  records under what condition this shape is currently reachable.
  Conjunctions/disjunctions of leaf conditions accumulate as the path
  evolves.

Each `HistoryEntry` stores, for a given step:

- `finishedKernels` — kernels that finished during this step (with
  their finish conditions). Useful for AST reconstruction and for
  optimisations.
- `progressedKernels` — kernels that progressed during this step.
- `condPathFinishes: Map<PathRoot, AcceptCondition>` — for each cond
  root, the condition under which it finished *during this step*. Used
  to evolve later steps' accept conditions.
- `activeCondPaths: Set<PathRoot>` — cond roots that survived this
  step. Used to decide whether a condition's truth is still pending.

## 4. The main step

`parseStep(ctx, input, isLastInput)` consumes one character. It computes
the next set of main paths and cond paths in seven phases.

```
gen := ctx.gen + 1
```

### Phase 1 — apply term actions to main paths

For every path `p` in `ctx.mainPaths`, find the term action whose term
group matches `input`, and apply it (`applyTermAction`). The term action
emits new paths into `nextMainPaths`, records finishes/progresses in
`finishesByGroup`/`progressesByGroup`, and emits any path-root
progresses (the start symbol finishing) into `rootProgresses`. New cond
symbols introduced by the milestone group's `observingCondSymbolIds`
flow into `observingOut`. Every `CondRootStarter` listed on an
`AppendMilestoneGroup` is recorded in `condRootStartersFromTerm` with
`startGen = midGen` — the gen at which the main path *began*
consuming the new milestone group's body.

### Phase 1b — apply input to cond root starters

For each `(starterRoot, milestoneGroupId)` in
`condRootStartersFromTerm`, spawn a fresh starter `ParsingPath(null,
milestoneGroupId, Always)` and apply the current input via
`applyTermAction`, exactly as if it were one of `ctx.mainPaths`. Any
resulting path is queued under `starterRoot` in `nextCondPaths`. If
the starter has already been seen (history skip) or is already in
`ctx.condPaths`/`nextCondPaths`, it is left alone — its existing copy
will be advanced by phase 2.

This step is the analogue of mgroup2's
`lookahead_requiring_symbols`: the starter created at the moment the
main path begins matching an NJoin/NLongest body consumes the *same*
input character, so it never lags behind.

A `replaceAndAppends` entry creates a new `MilestonePath`:

```
replaceKernel := (rea.replace.symbolId, rea.replace.pointer, parentGen)
newMilestonePath := MilestonePath(gen, replaceKernel, oldPath.milestonePath, observingIds)
nextPathsOut.add( ParsingPath(newMilestonePath, rea.append.milestoneGroupId,
                              And(oldPath.acceptCondition, newCondition)) )
```

A `replaceAndProgresses` entry triggers a *fold-up* reduction. If the
old path has no `milestonePath` (the path is starter-only), we record
the progress on the path root. Otherwise we look up the parent's tip
edge action (`tipEdgeActionsMap[parent.kernel, replaceMgroup]`) and
recursively apply it via `applyEdgeAction`, which itself can chain
through `midEdgeActions` until the path is fully re-rooted.

### Phase 2 — apply term actions to cond paths

The same process is applied to each cond path in `ctx.condPaths`. Their
output goes into `nextCondPaths` keyed by the cond root, and any
progresses/finishes feed `condPathFinishes`. A cond path that does not
match the input dies silently.

### Phase 3 — register new cond roots

Every leaf accept condition in `nextMainPaths`/`nextCondPaths`
references a `PathRoot(symbolId, startGen)`. We compute the set of cond
roots referenced this step:

```
newCondRoots := { PathRoot(c.symbolId, c.startGen) | c ∈ leaf-conditions(path.acceptCondition) }
                ∪ { PathRoot(sid, gen) | sid ∈ observingCondSymbolIds of any milestone in any path }
```

A *transitive closure* extends `newCondRoots` over the chain of
`initialCondSymbolIds` carried by each `pathRootsMap[sym]`, so any cond
root that may be triggered by deriving a known cond root is also
registered.

For each newly-required `PathRoot(sym, startGen)`:

1. **Skip if already in `ctx.condPaths` or `nextCondPaths`** — the root
   is already being progressed (possibly via phase 1b's freshly
   spawned starter).
2. **Skip if the root has appeared in any prior `history.activeCondPaths`** —
   it was once active and has since died. Re-spawning a starter now
   would launch a "phantom" parse from a stale generation, producing
   spurious finishes. Treating dead roots as permanently dead lets the
   condition evaluator (Phase 5) handle them with the *both-null* path
   (`finCond=null && active=false`), which yields the semantically
   correct value (e.g. `Always` for `NoLongerMatch`, `Never` for
   `OnlyIf`).
3. **Self-finish** — if the root symbol is nullable (the generator
   filled its `selfFinishAcceptCondition`), record the immediate finish
   in `newCondRootProgresses`.
4. **Spawn a starter path**: apply the current input via
   `applyTermAction`. If the starter has no matching term action but
   the root's `startGen == gen` (i.e. it's a "next-step" root that
   hasn't yet had a chance to match anything), still queue an empty
   starter so it can match starting from the next step.

### Phase 4 — collect cond-path finishes

Combine `rootProgresses` (cond-path finishes from Phase 1/2 reductions
that bubbled up to the cond root level) and `newCondRootProgresses`
(self-finish or starter-derived finishes from Phase 3) into
`condPathFinishes: Map<PathRoot, AcceptCondition>`.

### Phase 5 — evolve accept conditions

For every path (main and cond), rewrite its `acceptCondition` against
`condPathFinishes` and `activeCondRoots := nextCondPaths.keys`. The
rules are:

| Condition          | This step's gen vs cond.endGen | finCond | active | Result                       |
| ------------------ | ------------------------------ | ------- | ------ | ---------------------------- |
| `NoLongerMatch`    | `gen == endGen`                | —       | —      | unchanged (same-step finish) |
| `NoLongerMatch`    | `gen != endGen`                | non-null| —      | `evolve(finCond.neg())`     |
| `NoLongerMatch`    | `gen != endGen`                | null    | true   | unchanged                    |
| `NoLongerMatch`    | `gen != endGen`                | null    | false  | `Always`                     |
| `Unless` / `NotExists` | —                          | non-null| —      | `evolve(finCond.neg())`     |
| `Unless` / `NotExists` | —                          | null    | true   | unchanged                    |
| `Unless` / `NotExists` | —                          | null    | false  | `Always`                     |
| `OnlyIf` / `Exists` / `NeedLongerMatch` | —             | non-null| —      | `evolve(finCond)`            |
| `OnlyIf` / `Exists` / `NeedLongerMatch` | —             | null    | true   | unchanged                    |
| `OnlyIf` / `Exists` / `NeedLongerMatch` | —             | null    | false  | `Never`                      |

Paths whose evolved condition is `Never` are pruned. Paths whose
condition stayed the same are returned as-is, otherwise a new
`ParsingPath` with the evolved condition is created.

**Cycle protection.** `condPathFinishes[root]` may itself reference
`root` (e.g. a left-recursive cond path finishes with a condition that
mentions its own `NoLongerMatch`). The recursive `evolve` carries a
`visiting: Set<PathRoot>` of roots that have already been expanded on
the current call chain; when a sub-condition references a root already
in `visiting`, it is left unchanged instead of expanding again. This
keeps the rewrite finite without losing information — the condition
will be re-evaluated against future steps' history in
`evaluateConditionWithHistory`.

### Phase 6 — prune unreferenced cond roots

`mainPaths` and `condPaths` are already keyed by `PathShape =
(milestonePath, tipGroupId)`, so duplicate shapes from different
sources OR-merge their `AcceptCondition`s at insertion time — there is
no separate dedup step. `MilestonePath` carries a lazy-cached
`hashCode` to keep map insertion O(1) amortized despite its linked-
list parent chain.

Garbage-collect cond paths whose root is not referenced by any
surviving accept condition or any milestone's
`observingCondSymbolIds`.

### Phase 7 — emit history and return

If `nextMainPaths` is empty and `isLastInput == false`, raise
`UnexpectedInput`. Otherwise build a `HistoryEntry` recording this
step's finishes/progresses/cond-finishes/active-cond-roots, append it
to `history`, and return the next `ParsingCtx`.

## 5. Acceptance check

After consuming the entire input, `isAccepted(ctx)` checks whether the
start symbol finished from generation 0 with a satisfied condition.
Specifically, scan `history.last().finishedKernels` for entries
matching `(startSymbolId, 1, 0)` and evaluate their condition with
`evaluateConditionWithHistory` — a *history-aware* evaluator that knows
about the full history of cond-path finishes (not just the last step).

History-aware evaluation is necessary because the live-time
`evolveAcceptCondition` only sees the most recent step's
`condPathFinishes`. By the time `isAccepted` runs, longer-match
violations may have been recorded several steps in the past; the
evaluator scans `condPathFinishes` over the entire history to pick them
up.

## 6. Generator overview

The generator (`Mgroup3ParserGenerator`) is a fairly direct port of the
mgroup2/milestone2 generator, with the simplification that no separate
"cond graph" is built per path. For each path root (start symbol or cond
symbol), the generator:

1. Builds an initial `GenParsingGraph` by `derivedFrom(startNode)`
   (essentially running the naive deriver over the canonical kernel
   set, with the parser's gen tags `Prev / Curr / Mid / Next` as
   abstract `GenNodeGeneration` values).
2. From every milestone in that graph, computes the term/edge actions
   by enumerating the possible input partitions (`TermGrouper`) and,
   for each, doing a fresh `progressedFrom`. Each
   `replaceAndAppend`/`replaceAndProgress`/`appendMilestone`/`startNodeProgress`
   in the resulting graph becomes a proto entry.
3. Records the cond symbols that were observed during the derivation
   in the graph's `observingCondSymbolIds`. These flow into the
   appropriate `observingCondSymbolIds` fields of the term/edge actions
   and, for the path root itself, into `initialCondSymbolIds`.

When the generator emits an accept condition, it uses the `GenNode`'s
`startGen` directly as the condition's `start_gen` tag. This is the
mechanism that conveys "where did this condition originate" to the
parser without any heuristics, and is the change that unlocked correct
handling of nested NLongest cases.

## 7. Comparison with mgroup1/2

| Feature                    | mgroup1/2                                  | mgroup3                                            |
| -------------------------- | ------------------------------------------ | -------------------------------------------------- |
| Implementation language    | Scala                                      | Kotlin                                             |
| Generator-side parsing     | Uses real `NaiveParser2` context to drive derive/progress | Uses an abstract `GenParsingGraph` with `GenNodeGeneration` slots (`Prev`/`Curr`/`Mid`/`Next`) |
| Cond-condition runtime     | Re-runs naive deriver on demand            | Separate cond paths progressed step-by-step        |
| Accept-condition rep       | Generator-side templates derived from NaiveParser's `AcceptCondition.NotExists(beginGen, endGen, sym)` etc. — the `(beginGen, endGen)` pattern (0/1/2/3 slot model) determines `fromNextGen` | Explicit `KernelTemplateGen` tag per condition, taken from the atomic symbol's `GenNode.startGen` |
| Path representation        | Linked list of milestones + tip            | Same                                               |
| Path dedup                 | Implicit                                    | Explicit `(milestonePath, tipGroupId) → OR` merge   |
| History-aware evaluation   | Yes (via `isEventuallyAccepted`)           | Yes (via `collectFinishesAfter` over history)      |

The critical difference for the unresolved NJoin+NLongest issue is the
**accept-condition representation**. mgroup2's generator carries the
NaiveParser's actual `(beginGen, endGen)` numbers all the way through:
when it summarises a step into a `TermAction`, the surviving
`AcceptCondition.NotExists(beginGen=2, endGen=3, sym)` is unambiguously
"a longest condition whose body started matching at the current
step's beginning (slot 2) and finished at next-step's beginning (slot
3)", and that maps cleanly to `LongestTemplate(beginFromNextGen=true)`
or `false` based on which slot pair appears
(`ParserGenBase2.scala:50-93`).

mgroup3 throws this `(beginGen, endGen)` information away. Its
`GenNode.startGen` only records *which graph slot* a node started
in, not which slot it actually matches from when the condition fires
later. For most symbols this is fine — NLongest/NExcept inside an
NRepeat genuinely starts at the iteration boundary — but for a
multi-character NJoin body like `"||" & OpTk`, the cond symbol's
match start is several slots earlier than the slot in which the
NJoin's `progress` task runs, and the abstraction has no way to
recover that fact.

## 8. Known limitations

The current implementation passes 97 of 98 mgroup3 tests (the failing
case is `testTryLetMu`, a grammar-interpretation issue rather than an
algorithmic one). History of resolved limitations:

- (Resolved) **`NJoin` whose body is a multi-character sequence and
  whose cond-symbol is a `NLongest`** (e.g. mulang's `"||" & OpTk`
  where `OpTk = <Op = ('+'|'-'|"||")+>`). Resolution mirrors
  mgroup2's `lookahead_requiring_symbols` / `addPendedForTermAction`
  machinery — the generator emits the cond root starter information
  alongside every new milestone group, and the parser registers and
  applies those starters to the current step's input in lock-step
  with the main path.

- (Resolved) **`evolveAcceptCondition` infinite recursion on self-
  referential `condPathFinishes`**. When a left-recursive cond root
  finishes with a condition that mentions its own `NoLongerMatch` at
  a different `endGen`, the unbounded `evolve(finCond.neg())`
  recursion could oscillate between `NoLongerMatch` and
  `NeedLongerMatch` forever. Fixed by carrying a `visiting:
  Set<PathRoot>` through `evolve` and leaving sub-conditions
  unchanged when they reference a root already on the call chain.
  The condition is then re-evaluated against history in
  `evaluateConditionWithHistory`.

- (Resolved) **Path explosion / OOM** on the larger mulang examples.
  Previously the runtime kept `mainPaths: List<ParsingPath>` and
  required a separate `dedupPaths` post-step to merge paths with the
  same `(milestonePath, tipGroupId)`. The List-based representation
  permitted redundant intermediate paths, and on `ccgen.mu` this
  blew up before dedup could catch up. Fixed by switching the
  runtime representation to `PathMap = Map<PathShape,
  AcceptCondition>` — dedup is now a property of the data
  structure: when two sources produce the same `PathShape`, their
  `acceptCondition`s OR-merge at insert time. `MilestonePath` uses a
  manual lazy-cached `hashCode` to keep map operations O(1)
  amortized despite the linked-list parent chain.

- **Lambda type signatures** (e.g. `(Literal, Literal) -> Expr` in
  `try_let.mu`) — independent grammar feature whose support is
  separate from the cond-condition machinery. This is the one
  remaining test failure (`testTryLetMu`).

These are tracked as follow-up work.
