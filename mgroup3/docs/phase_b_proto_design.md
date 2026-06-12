# Phase B — Grammar-specific AST proto schema (ID-based)

Design for `Stage2ProtoEmit` (per-grammar `.proto` generation) and the
matching `Stage4RustEmit` (Rust crate generation) under the mgroup3 generator.

## Goal

Each grammar gets its own `.proto` so Rust can serialize a typed AST and
Kotlin can decode it without grammar-specific FFI plumbing. The schema must
handle:

- Self-referential nodes (`AddExpr.lhs: Expr` where `Expr` includes `AddExpr`).
- Mutually recursive node groups.
- Sealed hierarchies, including children that implement multiple parents.
- A grammar-agnostic decoder template (decode logic does not change per
  grammar — only the per-message structs do).

## Decision: ID-based flat node table

Cross-node references are `int32` ids resolved against a flat `NodeEntry`
table. There are no nested message references between AST nodes.

```proto
message ParseResult {
  int32 root_id = 1;
  repeated NodeEntry nodes = 2;
}

message NodeEntry {
  int32 id = 1;
  oneof node {
    AddExpr add_expr = 10;
    Number  number   = 11;
    // ... one variant per concrete node type
  }
}

message AddExpr {
  int32 lhs = 1;   // -> NodeEntry.id
  int32 rhs = 2;
  int32 start = 100;
  int32 end = 101;
}
```

### Alternatives considered

Four shapes were on the table. Brief recap of each, why it was rejected.

#### A. Sealed-parent-as-oneof, nested message references (the original Stage2ProtoEmit shape)

```proto
message Expr {
  oneof value {
    AddExpr add_expr = 1;
    MulExpr mul_expr = 2;
    Number  number   = 3;
    ...
  }
}
message AddExpr {
  Expr lhs = 1;   // nested message — wire-level type guarantees lhs ∈ Expr
  Expr rhs = 2;
  ...
}
```

- **Pros**: strongest wire-level type safety (oneof variant set restricts what
  can appear at each reference point). proto schema mirrors Kotlin/Rust sealed
  hierarchy 1:1. Decoder is a straight recursive descent over prost-generated
  enums — no id table, no validation pass.
- **Cons**:
  1. **Rust struct cycle break**: prost cannot emit a self-recursive enum
     variant (`Expr::AddExpr(AddExpr)` where `AddExpr` contains `Expr`) — the
     struct has infinite size. The emitter must inject `Box<T>` at a cycle-
     breaking point, which requires SCC analysis of the message graph
     (currently a TODO in `Stage4RustEmit.kt:100`) or per-field
     `prost-build::field_attribute` directives. Either way it's grammar-
     specific complexity in the emitter.
  2. **Multi-superclass requires duplication**: a child like
     `ObjField: Value, AstNode` must appear in both `Value.oneof` and
     `AstNode.oneof`. Same `message ObjField` referenced from both — legal in
     proto3 but the emitter has to know which dispatch parents are "active"
     (used at a choice point) vs purely declarative, otherwise the
     universal-marker `AstNode` becomes a giant oneof with every node in it.
  3. **Sealed parent message per dispatch parent**: one wrapper message per
     `Value`, `Stmt`, `Expr`, …. Mostly fine, but the wrapper carries no
     payload of its own — it's pure wire overhead (oneof tag + length-
     delimited frame per cross-node reference).

  Rejected because (1) and (2) push grammar-specific reasoning into the
  emitter, which is the cost we most want to avoid.

#### B. Universal envelope (`AnyAstNode` oneof of every concrete node, used at every reference site)

```proto
message AnyAstNode {
  oneof node {
    AddExpr add_expr = 1;
    Number  number   = 2;
    ...   // every concrete node in the grammar
  }
}
message AddExpr {
  AnyAstNode lhs = 1;   // any node can appear, validate at decode time
  AnyAstNode rhs = 2;
  ...
}
```

- **Pros**: emitter is trivially uniform — there is one envelope message and
  every cross-node reference uses it. Multi-superclass dissolves (`ObjField`
  is just one of the variants like everything else). Adding a new node type
  is one `oneof` entry, no other emit changes.
- **Cons**:
  1. **All wire-level type safety lost**: `AddExpr.lhs` can carry a `Number`
     or a `Stmt` or anything — proto3 won't reject it. Validation moves
     entirely to the decoder, which now has to check "is this node a member
     of `Expr`?" at every reference.
  2. **Wire overhead per reference**: every cross-node edge carries an
     envelope (oneof tag + length-delimited frame), even though both encoder
     and decoder know the expected type. Cumulative on large ASTs.
  3. **Universal-enum churn**: every grammar change touches the same big
     oneof. Removing a node breaks wire compatibility; adding is safe but
     noisy.

  Rejected because the type-safety loss is identical to the ID approach but
  without any of its decoder-side benefits, and the envelope-per-reference
  overhead is worse.

#### C. ID-based flat table (chosen — see below)

#### D. Hybrid: ID for cycle-crossing edges only, nested elsewhere

```proto
// Cycle-crossing reference (AddExpr.lhs across the Expr sealed parent):
message AddExpr {
  int32 lhs = 1;   // id reference because Expr ↔ AddExpr cycles
  ...
}
// Non-cyclic reference (e.g. SuperClassDef.body where the sealed parent
// doesn't loop back to SuperClassDef): nested as in A.
message SuperClassDef {
  SuperClassDefBody body = 1;
  ...
}
```

- **Pros**: keeps wire-level type safety wherever cycles don't force a
  break. Smaller wire size on the non-cyclic parts of the AST.
- **Cons**: emitter has to run SCC analysis just to decide *which* references
  become ids — exactly the complexity we were trying to delete in option A,
  just gated to a smaller surface. Decoder logic also bifurcates (some
  references resolve directly, others via id lookup), so the Kotlin builder
  template has two code paths per field type.

  Rejected because it preserves the worst part of A (SCC analysis) for a
  partial benefit, and complicates the decoder for no net win.

### Why ID (option C) over the alternatives

| | A. Nested + sealed oneof | B. Universal envelope | C. ID-based (chosen) | D. Hybrid (id at cycles, nested elsewhere) |
|---|---|---|---|---|
| Rust struct cycle break | per-grammar `Box<T>` placement (SCC analysis) | per-grammar `Box<T>` still needed inside envelope | none — `int32` | SCC analysis (limited to cycle-crossing edges) |
| prost-build config | `field_attribute` directives per cycle | minimal | none | partial |
| sealed parent emitted | yes (one per dispatch parent) | one universal envelope | not emitted | mixed |
| multi-superclass handling | child in N oneofs; needs "active parent" analysis | not applicable | not applicable | child in N oneofs (cycle-crossing only) |
| wire validation | strong | none (all references are `AnyAstNode`) | weak (id can point at wrong type) | partial |
| wire size overhead | low (envelope per dispatch level) | high (universal envelope per reference) | medium (flat table + per-entry envelope) | low–medium |
| decoder pattern | recursive descent via prost enums | recursive descent + per-edge type check | id lookup + dispatch | hybrid |
| emitter complexity | high (SCC + active-parent) | low | low | high |

The trade C makes: lose some wire-level type safety, in exchange for
**removing every grammar-specific quirk from the emitter**. Multi-superclass
dissolves; cycle break dissolves; sealed parent messages don't get emitted at
all. The emitter becomes a near-mechanical translation from `AstSchema` to
`.proto`, with no per-grammar analysis.

The "weak wire validation" cost is a single decoder-side cast per reference
(`builder.build(id, byId) as Expr`), which is the same `as` the Kotlin AST
walk does today against `KernelSet`. Acceptable.

Multi-superclass dissolves entirely: a node like `ObjField: Value, AstNode`
appears once in `NodeEntry`, and consumers infer its membership from the
referring context (whichever Rust/Kotlin field type expected `Value` casts
the resolved node to `Value`).

## Emit rules

### Per-grammar `.proto`

For every grammar, emit a single `.proto` containing:

1. **Package**: `com.giyeok.jparser.mgroup3.generated.<grammar>` (the same
   package as the schema's Kotlin counterpart).

2. **Enums**: each `EnumDef`. Proto3 requires a zero variant, so prepend
   `<ENUM>_UNSPECIFIED = 0;`. Original values get tags 1..N.

3. **Concrete messages** (one per `MessageDef` whose `sealedChildren` is
   empty):
   - Field number layout:
     - `1..89` — grammar payload fields, numbered in `FieldDef.number` order
       from `SchemaBuilder`.
     - `100`, `101` — `start`, `end` (always present).
   - `node_id` is **not** emitted as a per-message field; it lives on
     `NodeEntry.id`.
   - **Field names**: the `AstSchema` carries Kotlin/Scala camelCase
     identifiers (`typeName`, `typeAttr`). The emitter converts each to
     snake_case (`type_name`, `type_attr`) per the proto3 style guide. The
     `<name>_present` flag for optionals is derived from the already-converted
     snake_case name.
   - Field type mapping:
     - `Bool` → `bool`
     - `Int32` → `int32`
     - `Str` → `string`
     - `NodeBytes` → `bytes`
     - `Enm(name)` → enum reference
     - `Msg(name)` → `int32` (id reference)
     - `Opt(X)` → uniform two-field split, regardless of `X`:
       `bool <name>_present = N; <X> <name> = N+1;`. For `Opt(Msg)` the
       payload field is `int32` (id reference); for `Opt(Arr(T))` it is
       `repeated <T>`. The split is uniform across all `Opt` so the decoder
       has one rule. (Proto3 has `optional` since 3.15, but sticking to a
       uniform `_present` flag keeps decode logic identical for all
       optional-of-X cases and avoids the synthetic-oneof prost generates
       for `optional`.)
     - `Arr(Msg)` → `repeated int32 <name>` (id references)
     - `Arr(scalar)` → `repeated <scalar> <name>`

4. **Sealed parents are NOT emitted as messages.** Their dispatch is
   recovered by the consumer when resolving an id: the resolved `NodeEntry`'s
   `oneof node` case tells the consumer which concrete type the id points to,
   and the consumer's Rust/Kotlin enum (a separate Stage 3/4 artifact) maps
   that case to its sealed-parent variant.

5. **Envelope** — always the same two messages at the bottom:
   - `NodeEntry { int32 id; oneof node { <all concrete messages> } }`
     - oneof tag numbers start at 10. Variants listed in alphabetical order
       of message name (stable identity across runs).
   - `ParseResult { int32 root_id; repeated NodeEntry nodes; }`

### Encoder side (Rust)

`Stage4RustEmit` will be revised separately. The contract Rust must honor:

- `id` allocation: monotonic, starts at 1. `0` reserved as sentinel for
  optional-Msg fields (matches the absent case).
- `nodes` ordering: post-order — every entry's referenced ids must already
  have appeared earlier in the list. This lets the decoder build the AST in
  one forward pass without pre-indexing.
- One `NodeEntry` per AST node. No de-duplication of equal subtrees (current
  AST is a tree, not a DAG; preserves the original `node_id` semantics).

### Decoder side (Kotlin)

Decoder is grammar-agnostic in shape:

```kotlin
fun <Root> decode(bytes: ByteArray, builder: AstBuilder<Root>): Root {
  val result = ParseResult.parseFrom(bytes)
  val byId = HashMap<Int, NodeEntry>(result.nodesCount)
  for (e in result.nodesList) byId[e.id] = e
  return builder.build(result.rootId, byId)
}
```

`AstBuilder<Root>` is per-grammar (Stage 3 emits one). It walks ids, switches
on `NodeEntry.nodeCase`, materializes the typed Kotlin data class, and casts
to the expected sealed parent at the call site (`builder.build(id, byId) as
Expr`). The cast is the runtime validation point — wire-level errors surface
as `ClassCastException` wrapped with field-name context.

## Field tag stability

oneof tags in `NodeEntry` and field numbers inside concrete messages must
be stable across regenerations to preserve wire compatibility of stored
results. Rules:

- Alphabetical ordering of names (messages, enum variants, oneof arms) is the
  source of truth. `SchemaBuilder` already sorts; emitter must preserve that.
- Adding a new node type → new variant gets the next free tag in NodeEntry.
  Existing tags do not shift. (Achieved by sorting on name and remembering
  the highest assigned tag — not yet implemented; deferred until the first
  wire-stable consumer exists.)
- For now (no external consumers), regenerate freely.

## Open items

- **Span representation**: currently `int32 start`, `int32 end` on every
  concrete message. If grammars ever exceed 2^31 characters in a single parse,
  switch to `int64`. Not urgent.
- **bytes fields for NodeType / AnyType / NullType**: the current Kotlin
  schema falls back to `bytes` for these. The decoder needs grammar-specific
  knowledge to interpret. Not in the Phase B PoC scope.
- **Versioning**: no version field on `ParseResult` yet. Add when the first
  long-lived consumer ships.

## Stage 4 — Rust code emitter (DRAFT)

`RustOptCodeGen` (Scala, under `metalang/.../codegen/`) is the Rust counterpart
of `KotlinOptCodeGen`: it recurses over the *same* `ValuefyExpr` tree and emits
Rust instead of Kotlin. `Stage4RustEmit` calls it (mirroring how
`Stage3KotlinEmit` calls `KotlinOptCodeGen`) and lays out a self-contained crate:

```
<rustDir>/
  Cargo.toml, build.rs
  proto/ast.proto         # Stage2ProtoEmit (ID-based)
  src/lib.rs              # module tree + prost include!
  src/ktlib.rs            # static: KernelSet + AstifierUtil port
  src/ast.rs              # AST types + AstNode trait + walk (match_* fns)
  src/encode.rs           # typed AST -> proto ParseResult (ID table)
```

### Three explicit coercions Rust needs that Kotlin gets for free

The walk structure is identical to Kotlin's; the hard part is that Rust makes
explicit what Kotlin's type system hides. `RustOptCodeGen.coerce` handles:

1. **Boxing** — match fns return UNBOXED values at every level (`ModuleDef`,
   `Option<Attributes>`, `Vec<Param>`). `Box` is added at exactly ONE site:
   storing into a struct field (`ConstructCall`). This is deliberate — the
   metalang type system has no `Box`, so "is it already boxed?" is undecidable
   from the inferred type; keeping a single boxing site avoids double-boxing.
2. **Nullable promotion** — a bare value flowing into an `Option<T>` field /
   return / ternary branch is wrapped in `Some(...)`.
3. **Sealed up-cast** — a concrete child produced where the parent enum is
   expected becomes `Parent::Child(Box::new(...))`. Applied per choice arm AND
   for single-choice bodies (e.g. JSON `Element = Value`).

### Source access

Generated walkers hold `source_chars: &[char]` (not `&str`) — gen indices are
char indices. `match_start` reads `self.source_chars.len()`.

### Verification (2026-06-11)

End-to-end: `bibix4 mgroup3.genCliJar`, then run GenCli on a grammar, then
`cargo build` the emitted crate. **asdl / pyobj / json all compile clean**
(0 warnings, 0 errors) — exercising sealed dispatch (5-way), enums, optional
lists, char extraction, single-choice up-cast, and empty (no-field) structs.
The prost-generated proto types check too. `runMgroup3GenTest` stays green.

### Known limitations / open items

- **Reserved span field names.** A grammar whose own AST has a field literally
  named `start` or `end` (e.g. metalang3's own grammar:
  `TerminalChoiceRange(start=$0, end=$2)`) collides with the synthetic span
  fields. protoc rejects the duplicate; Kotlin codegen has the same latent
  clash. Fix: rename the synthetic span fields (e.g. `node_start`/`node_end`)
  across Stage2ProtoEmit + RustOptCodeGen + the decoder together. Deferred.
- **`InputNode`** ValuefyExpr is `???` (partial parse-tree reconstruct); none
  of the PoC grammars hit it.
- **Proto enum encoding** (`encode.rs`): enum values are written as `i32`
  codepoints/ordinals. The exact prost enum field semantics still want a
  closer look before wire round-tripping is claimed correct.
- **`ktlib.rs` is embedded** in Stage4RustEmit as a string. Should graduate
  into `mgroup3-native` (or a `mgroup3-ktlib` crate) so generated crates depend
  on it rather than each carrying a copy.
- **No decoder yet.** The Kotlin `AstProtoBinding` (Stage 3) is still a stub —
  proto → typed Kotlin AST reconstruction is the next piece.

## See also

- `metalang/main/scala/com/giyeok/jparser/metalang3/codegen/RustOptCodeGen.scala`
  — the Stage 4 emitter (DRAFT).
- `examples/generated/rust/asdl_ast.proto` — hand-written reference for ASDL.
- `examples/generated/rust/pyobj_opt_ast.proto` — hand-written reference for
  PyObjOpt (exercises sealed dispatch, multi-superclass, optional-list).
- `examples/generated/rust/{asdl_ast,pyobj_opt_ast}.rs` — Rust AST target
  models (still nested-struct shape, decoupled from this proto design;
  Stage4RustEmit will reconcile).
- `mgroup3/genCli/kotlin/com/giyeok/jparser/mgroup3/SchemaBuilder.kt` — IR
  source for the emitter.
- `mgroup3/genCli/kotlin/com/giyeok/jparser/mgroup3/Stage2ProtoEmit.kt` —
  emitter to be rewritten per these rules.
