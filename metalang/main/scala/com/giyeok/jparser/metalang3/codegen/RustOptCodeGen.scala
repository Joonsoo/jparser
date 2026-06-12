package com.giyeok.jparser.metalang3.codegen

import com.giyeok.jparser.NGrammar.{NNonterminal, NRepeat, NStart}
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.metalang3.MetaLanguage3.{ProcessedGrammar, check}
import com.giyeok.jparser.metalang3.{ClassHierarchyItem, ClassRelationCollector, Type, ValuefyExpr}

import java.io.StringWriter
import scala.annotation.tailrec

/**
 * DRAFT — Rust counterpart of [[KotlinOptCodeGen]].
 *
 * Generates a Rust module that walks a `Vec<KernelSet>` (kernels_history) into
 * a grammar-specific typed AST, mirroring what `KotlinOptCodeGen` produces on
 * the JVM side. The *structure* of the walk is identical (it recurses over the
 * same `ValuefyExpr` tree); only the emitted syntax differs.
 *
 * Differences from the Kotlin generator that drive the syntax mapping:
 *   - `val x = e`                  → `let x = e;`
 *   - `when { c -> a; else -> b }` → `if c { a } else { b }`
 *   - `listOf(a, b)`               → `vec![a, b]`
 *   - `xs.map { k -> e }`          → `xs.into_iter().map(|k| e).collect()`
 *   - `x ?: y`                     → `x.unwrap_or_else(|| y)` (or `.unwrap_or(y)`)
 *   - `e?.let { x -> f(x) }`       → `e.map(|x| f(x))`
 *   - `source[g]`                  → `self.source_chars[g as usize]`
 *   - `source.length`             → `self.source_chars.len() as i32`
 *   - `nextId()`                  → `self.next_id()`  (requires `&mut self`)
 *   - nullable `T?`                → `Option<T>`
 *   - construct `Foo(a, .., id, b, e)` → `Foo { f1: a, .., node_id: id, start: b, end: e }`
 *       (field names come from `analysis.classParamTypes`)
 *
 * Self-reference is broken with `Box<T>` on every `Msg`-typed field, matching
 * the PoC under `examples/generated/rust/asdl_ast.rs`. A precise SCC analysis
 * can unbox acyclic references later; always-Box is correct, just not minimal.
 *
 * Source access: the parser drives `parse_step` from `text.chars()`, so gen
 * indices index a `&[char]`, NOT a UTF-8 `&str`. The generated walker holds
 * `source_chars: &[char]`. See mgroup3/docs/phase_b_proto_design.md.
 *
 * NOT YET IMPLEMENTED here (separate stages): the typed-AST → proto ID-table
 * encoder (see [[RustProtoEncoderGen]]) and the AST type definitions (structs/
 * enums + AstNode trait, see [[RustAstTypeGen]]).
 */
class RustOptCodeGen(val analysis: ProcessedGrammar) {
  private var varId: Int = 0

  def newVar(): String = {
    varId += 1
    s"var$varId"
  }

  var _requiredNonterms: Set[String] = Set()
  var _symbolsOfInterest: Set[Int] = Set()

  def symbolsOfInterest: Set[Int] = _symbolsOfInterest

  // ---- expression / statement blob (same shape as KotlinOptCodeGen.ExprBlob) -

  /**
   * A chunk of generated code: a list of preparatory statements plus a final
   * result expression. `required` carries the set of nonterminal names that
   * the result references and therefore must also be generated.
   */
  case class ExprBlob(prepares: List[String], result: String, required: Set[String]) {
    // Result wrapped in a block when used where a single expression is needed
    // but `prepares` is non-empty. In Rust a `{ stmts; expr }` block is itself
    // an expression, so we can always inline.
    def asBlockExpr: String =
      if (prepares.isEmpty) result
      else s"{ ${prepares.mkString(" ")} $result }"
  }

  object ExprBlob {
    def code(result: String): ExprBlob = ExprBlob(List(), result, Set())
  }

  // ---- char escaping (Rust syntax) -------------------------------------------

  def escapeChar(c: Char): String = c match {
    case '\b' => "\\u{8}"
    case '\n' => "\\n"
    case '\r' => "\\r"
    case '\t' => "\\t"
    case '\\' => "\\\\"
    case '\'' => "\\'"
    case c if c.toInt < 0x20 || c.toInt > 0x7e =>
      f"\\u{${c.toInt}%x}"
    case c => c.toString
  }

  def escapeString(s: String): String = s.flatMap {
    case '\n' => "\\n"
    case '\r' => "\\r"
    case '\t' => "\\t"
    case '\\' => "\\\\"
    case '"' => "\\\""
    case c if c.toInt < 0x20 || c.toInt > 0x7e => f"\\u{${c.toInt}%x}"
    case c => c.toString
  }

  // ---- naming ----------------------------------------------------------------

  private def nonterminalMatchFuncName(nonterminal: String): String =
    s"match_${camelToSnake(nonterminal)}"

  def camelToSnake(s: String): String = {
    val sb = new StringBuilder
    s.zipWithIndex.foreach { case (c, i) =>
      if (c.isUpper) {
        if (i > 0) sb.append('_')
        sb.append(c.toLower)
      } else sb.append(c)
    }
    sb.toString
  }

  /** snake_case + reserved-word avoidance for struct field names. */
  def rustFieldName(name: String): String = {
    val snake = camelToSnake(name)
    if (RustOptCodeGen.RustKeywords.contains(snake)) s"${snake}_" else snake
  }

  /**
   * Type-position-safe class name. `Self` cannot even be a raw identifier, and
   * prelude/support names (Box, Option, Ctx, ...) would shadow what the
   * generated code references unqualified. Renamed to `<name>Node` — applied to
   * Rust type names AND (via SchemaBuilder.rustSafeName, keep the lists in
   * sync) the proto message names, so prost output matches. Kotlin AST keeps
   * the original name; to_short_string also prints the original.
   */
  def rustClassName(name: String): String =
    if (RustOptCodeGen.RustReservedTypeNames.contains(name)) name + "Node" else name

  // ---- type rendering --------------------------------------------------------
  //
  // Mirrors Stage4RustEmit.rustType so the walker and the AST type definitions
  // agree on field types. `Msg` is always `Box<T>` to break cycles.

  def rustType(t: Type): String = t match {
    case Type.NodeType => "Vec<u8>"
    case Type.ClassType(name) => s"Box<${rustClassName(name)}>"
    case Type.OptionalOf(typ) => s"Option<${rustType(typ)}>"
    case Type.ArrayOf(typ) => s"Vec<${rustTypeUnboxed(typ)}>"
    case unionType: Type.UnionOf =>
      analysis.reduceUnionType(unionType) match {
        case reduced: Type.UnionOf =>
          throw new Exception(s"irreducible union type: ${Type.readableNameOf(reduced)}")
        case reduced => rustType(reduced)
      }
    case Type.EnumType(enumName) => enumName
    case Type.UnspecifiedEnumType(uniqueId) => analysis.shortenedEnumTypesMap(uniqueId)
    case Type.NullType => "Vec<u8>"
    case Type.AnyType => "Vec<u8>"
    case Type.BoolType => "bool"
    case Type.CharType => "char"
    case Type.StringType => "String"
    case Type.NothingType => "Vec<u8>"
  }

  // Vec<T> inner: Vec already heap-allocates, so the inner Msg need not be Boxed.
  private def rustTypeUnboxed(t: Type): String = t match {
    case Type.ClassType(name) => rustClassName(name)
    case _ => rustType(t)
  }

  /**
   * Return type for `match_*` functions. Message values are returned UNBOXED at
   * every level the walker controls: a bare `ModuleDef`, `Option<Attributes>`
   * (not `Option<Box<Attributes>>`), `Vec<Param>`. Boxing is added only when a
   * value is *stored into a struct field* (ConstructCall), so there is exactly
   * one place that introduces `Box`. This avoids double-boxing, since the
   * metalang type system has no notion of `Box` to detect "already boxed".
   */
  private def rustReturnType(t: Type): String = t match {
    case Type.ClassType(name) => rustClassName(name)
    case Type.OptionalOf(inner) => s"Option<${rustReturnType(inner)}>"
    case Type.ArrayOf(inner) => s"Vec<${rustReturnType(inner)}>"
    case _ => rustType(t)
  }

  // ---- coercion --------------------------------------------------------------
  //
  // Rust makes explicit three things the Kotlin codegen gets for free:
  //   - nullable promotion: `x`        used where `T?`        → `Some(x)`
  //   - boxing:             `T`        stored in `Box<T>`     → `Box::new(x)`
  //   - sealed up-cast:     child `C`  used where parent `P`  → `P::C(Box::new(x))`
  //
  // `coerce(expr, exprType, expectedRustShape)` rewrites the expression. We do
  // NOT chase the source type through bindings — we drive purely off `exprType`
  // (the inferred type of the producing ValuefyExpr) and the expected shape.

  private def classNameOf(t: Type): Option[String] = reduceType(t) match {
    case Type.ClassType(name) => Some(name)
    case _ => None
  }

  /** class -> ALL direct sealed parents (a class can belong to multiple unions). */
  private lazy val parentsOf: Map[String, Set[String]] = {
    val hierarchy = analysis.classRelations.toHierarchy
    (for {
      item <- hierarchy.allTypes.values.toList
      sub <- item.subclasses
    } yield sub -> item.className).groupBy(_._1).view.mapValues(_.map(_._2).toSet).toMap
  }

  /**
   * Direct-edge inheritance path from `child` up to `parent` (BFS, shortest).
   * Returned head-first from parent: List(parent, ..., child). None if not an
   * ancestor.
   */
  private def upcastPath(child: String, parent: String): Option[List[String]] = {
    if (child == parent) return Some(List(child))
    val visited = scala.collection.mutable.Set[String](child)
    val queue = scala.collection.mutable.Queue[List[String]](List(child))
    while (queue.nonEmpty) {
      val path = queue.dequeue()
      for (p <- parentsOf.getOrElse(path.head, Set())) {
        if (p == parent) return Some(p :: path)
        if (visited.add(p)) queue.enqueue(p :: path)
      }
    }
    None
  }

  private def reduceType(t: Type): Type = t match {
    case u: Type.UnionOf => analysis.reduceUnionType(u) match {
      case u2: Type.UnionOf =>
        // metalang 의 reduceUnionType 이 못 줄이는 union 이라도 class hierarchy 상
        // 공통 sealed 조상이 있으면 그 타입으로 — 중첩 choice 의 arm 들이 그 부모로
        // up-cast 되어야 Rust 의 if/else 타입이 통일된다.
        commonParentOf(u2).map(Type.ClassType).getOrElse(u2)
      case r => reduceType(r)
    }
    case _ => t
  }

  /**
   * Least common sealed ancestor of all members of an (irreducible) union,
   * via parentsOf BFS. Members must all be class types. Minimal total distance,
   * name-ordered for determinism.
   */
  private def commonParentOf(u: Type.UnionOf): Option[String] = {
    val members = u.types.toList.map(classNameOf)
    if (members.exists(_.isEmpty)) None
    else {
      def ancestorsOf(c: String): Map[String, Int] = {
        val dist = scala.collection.mutable.Map[String, Int]()
        val queue = scala.collection.mutable.Queue[(String, Int)]((c, 0))
        while (queue.nonEmpty) {
          val (cur, d) = queue.dequeue()
          for (p <- parentsOf.getOrElse(cur, Set.empty[String])) {
            if (!dist.contains(p)) {
              dist(p) = d + 1
              queue.enqueue((p, d + 1))
            }
          }
        }
        dist.toMap
      }
      val ancMaps = members.map(m => ancestorsOf(m.get))
      val common = ancMaps.map(_.keySet).reduce(_ intersect _)
      if (common.isEmpty) None
      else Some(common.toList.map(p => (ancMaps.map(_.apply(p)).sum, p)).sorted.head._2)
    }
  }

  /**
   * The expected target for a coercion. We only need a few shapes:
   *   - `FieldOf(declaredType)` — storing into a struct field (Box<T> for Msg)
   *   - `ReturnOf(nontermType)` — returning from a match_* fn (unboxed T)
   *   - `ElemOf(arrayElemType)` — an element of a Vec<unboxed>
   */
  sealed trait Target
  case class FieldOf(t: Type) extends Target
  case class ReturnOf(t: Type) extends Target
  case class ElemOf(t: Type) extends Target

  /**
   * Coerce `expr`, whose producing expression has inferred type `exprType`,
   * to satisfy `target`. The key facts:
   *   - match_* functions return UNBOXED class values, so the producer of a
   *     class is a bare `T`.
   *   - struct fields are `Box<T>` for Msg, `Option<Box<T>>` for Opt(Msg),
   *     `Vec<T>` (unboxed) for Arr(Msg).
   *   - sealed parents are enums; a child value must be wrapped in `P::C(Box)`.
   */
  private def coerce(expr: String, exprType: Type, target: Target): String = target match {
    case ReturnOf(t) =>
      // Return type is unboxed. Adjust for sealed up-cast, and for a bare value
      // flowing into an Optional return (`Some(...)`).
      reduceType(t) match {
        case Type.OptionalOf(inner) => coerceBranch(expr, exprType, Type.OptionalOf(inner))
        case other => sealedUpcast(expr, exprType, other)
      }
    case ElemOf(t) =>
      // Vec elements are unboxed; sealed up-cast only.
      sealedUpcast(expr, exprType, reduceType(t))
    case FieldOf(decl) =>
      reduceType(decl) match {
        case Type.ClassType(name) =>
          // Box<name>. Up-cast first (yields the parent enum value), then box —
          // the field itself is Box<Parent> regardless of whether an up-cast
          // happened.
          s"Box::new(${sealedUpcast(expr, exprType, Type.ClassType(name))})"
        case Type.OptionalOf(inner) =>
          // Field is `Option<Box<inner>>` (Msg) or `Option<inner>` (scalar).
          // The match value is `Option<inner-unboxed>` or a bare value. We add
          // the Box here (the single boxing site) and wrap bare values in Some.
          reduceType(exprType) match {
            case Type.OptionalOf(_) | Type.NullType =>
              // Already an Option; box the inner Msg via map.
              reduceType(inner) match {
                case Type.ClassType(_) => s"$expr.map(|x| Box::new(x))"
                case _ => expr
              }
            case _ =>
              // Bare value: up-cast/Box the inner, then wrap in Some.
              s"Some(${boxInnerForField(expr, inner)})"
          }
        case Type.ArrayOf(_) =>
          // Vec<unboxed>; elements coerced at construction (ArrayExpr/unroll).
          expr
        case _ => expr
      }
  }

  /** Box / up-cast a bare value being stored as the inner of an `Option<Box<_>>` field. */
  private def boxInnerForField(expr: String, inner: Type): String = reduceType(inner) match {
    case Type.ClassType(name) =>
      s"Box::new(${sealedUpcast(expr, inner, Type.ClassType(name))})"
    case _ => expr
  }

  /**
   * If `expr` (type `exprType`) is a (transitive) child of sealed `expected`,
   * wrap it level by level: P::M(Box::new(M::C(Box::new(expr)))).
   */
  private def sealedUpcast(expr: String, exprType: Type, expected: Type): String = {
    (classNameOf(exprType), reduceType(expected)) match {
      case (Some(child), Type.ClassType(parent)) if child != parent =>
        upcastPath(child, parent) match {
          case Some(path) =>
            // path = [parent, ..., child]; wrap from the child end outward.
            var acc = expr
            path.sliding(2).toList.reverse.foreach {
              case List(p, c) => acc = s"${rustClassName(p)}::${rustClassName(c)}(Box::new($acc))"
              case _ =>
            }
            acc
          case None => expr
        }
      case _ => expr
    }
  }

  /**
   * Coerce one branch of an if/ternary to the branch-result type. Match values
   * are unboxed at this level (boxing happens only at field stores), so we only
   * wrap a bare value flowing into an Optional result in `Some(...)`, applying a
   * sealed up-cast to the inner if needed. A branch already Option/Null passes
   * through.
   */
  private def coerceBranch(expr: String, from: Type, resultType: Type): String =
    reduceType(resultType) match {
      case Type.OptionalOf(inner) =>
        reduceType(from) match {
          case Type.OptionalOf(_) | Type.NullType => expr
          case _ => s"Some(${sealedUpcast(expr, from, inner)})"
        }
      case other => sealedUpcast(expr, from, other)
    }

  // ---- top-level entry: matchStart -------------------------------------------

  def matchStartFunc(): String = {
    varId = 0
    val startSymbol = analysis.ngrammar
      .symbolOf(analysis.ngrammar.nsymbols(analysis.ngrammar.startSymbol).asInstanceOf[NStart].produce)
      .asInstanceOf[NNonterminal]
    val returnType = rustReturnType(analysis.nonterminalTypes(analysis.startNonterminalName))
    _requiredNonterms += startSymbol.symbol.name
    _symbolsOfInterest += startSymbol.id
    s"""    pub fn match_start(&mut self) -> $returnType {
       |        let last_gen = self.source_chars.len() as i32;
       |        let kernel = self.history[last_gen as usize].get_single(${startSymbol.id}, 1, 0, last_gen);
       |        ${nonterminalMatchFuncName(startSymbol.symbol.name)}_(self, kernel.begin_gen, kernel.end_gen)
       |    }""".stripMargin
  }

  // ---- per-nonterminal match function ----------------------------------------
  //
  // Free functions taking `&mut Ctx` (named `match_x_`) rather than methods, to
  // sidestep Rust's restriction on calling `self.match_x()` while another field
  // of self is borrowed. The trailing underscore keeps them distinct from the
  // public `match_start` method on the Ctx struct.

  def nonterminalMatchFunc(nonterminal: String): String = {
    varId = 0
    val valuefyExpr = analysis.nonterminalValuefyExprs(nonterminal)
    val nontermType = analysis.nonterminalTypes(nonterminal)
    val body = unrollChoicesExpr(valuefyExpr.choices, "begin_gen", "end_gen", nonterminal, nontermType)
    val returnType = rustReturnType(nontermType)
    // The body's result type equals the nonterminal type; the only reshaping is
    // a sealed up-cast when each choice produced a concrete child but the
    // declared return is the parent. unrollChoicesExpr already up-casts per-arm
    // when there are multiple choices, so this top-level coerce is a no-op in
    // that case and only fires for single-choice bodies.
    val finalResult = coerce(body.result, nontermType, ReturnOf(nontermType))
    val bodyLines = (body.prepares :+ finalResult).map("        " + _).mkString("\n")
    s"""fn ${nonterminalMatchFuncName(nonterminal)}_(ctx: &mut Ctx, begin_gen: i32, end_gen: i32) -> $returnType {
       |$bodyLines
       |}""".stripMargin
  }

  def unrollChoicesExpr(
    choicesMap: Map[Symbols.Symbol, ValuefyExpr],
    beginGen: String,
    endGen: String,
    parentSymbolName: String,
    expectedType: Type,
  ): ExprBlob = {
    if (choicesMap.size == 1) {
      val (choiceSymbol, choiceExpr) = choicesMap.head
      val singleHint = reduceType(expectedType) match {
        case Type.ClassType(_) => Some(expectedType)
        case _ => None
      }
      val inner = valuefyExprToCode(choiceExpr, beginGen, endGen, choiceSymbol, SequenceVarName(None), singleHint)
      // Even with a single choice, the produced concrete type may be a child of
      // the declared (parent) type — up-cast the result. (e.g. JSON `Element = Value`.)
      val coerced = coerce(inner.result, producedType(choiceExpr), ReturnOf(expectedType))
      if (coerced == inner.result) inner
      else inner.copy(result = coerced)
    } else {
      val choiceSymbols = choicesMap.keys.toList.sortBy(analysis.ngrammar.idOf)
      val choiceVars = choiceSymbols.map(_ => newVar())
      val choices = choiceVars.zip(choiceSymbols)

      val tryCodes = choices.map { case (varName, choiceSymbol) =>
        val symbolId = analysis.ngrammar.idOf(choiceSymbol)
        val lastPointer = analysis.ngrammar.lastPointerOf(symbolId)
        _symbolsOfInterest += symbolId
        s"let $varName = ctx.history[$endGen as usize].find_by_begin_gen_opt($symbolId, $lastPointer, $beginGen);"
      }

      val assertCode = List(
        s"assert!(has_single_true(&[${choiceVars.map(_ + ".is_some()").mkString(", ")}]));"
      )

      val v = newVar()
      var requires = Set[String]()
      // Build an if / else if / else chain. Each arm is a Rust block expression,
      // up-cast to the expected (parent) type when the arm produced a concrete
      // sealed child.
      val armExprs = choices.zipWithIndex.map { case ((varName, choiceSymbol), index) =>
        val choiceExpr = choicesMap(choiceSymbol)
        val armHint = reduceType(expectedType) match {
          case Type.ClassType(_) => Some(expectedType)
          case _ => None
        }
        val exprCode = valuefyExprToCode(choiceExpr, beginGen, endGen, choiceSymbol, SequenceVarName(None), armHint)
        requires ++= exprCode.required
        val armType = producedType(choiceExpr)
        (classNameOf(armType), reduceType(expectedType)) match {
          case (Some(c), Type.ClassType(par)) if c != par && upcastPath(c, par).isEmpty =>
            System.err.println(s"DBGNOUP arm=$c expected=$par")
          case (None, Type.ClassType(par)) =>
            System.err.println(s"DBGNOUP nonclass-arm=${Type.readableNameOf(reduceType(armType))} expected=$par")
          case _ =>
        }
        val coerced = coerce(exprCode.asBlockExpr, armType, ReturnOf(expectedType))
        val isLast = index == choices.size - 1
        val cond = if (isLast) None else Some(s"$varName.is_some()")
        (cond, coerced)
      }
      val chain = armExprs.zipWithIndex.map { case ((cond, blockExpr), idx) =>
        cond match {
          case Some(c) if idx == 0 => s"if $c { $blockExpr }"
          case Some(c) => s"else if $c { $blockExpr }"
          case None => s"else { $blockExpr }"
        }
      }.mkString(" ")

      ExprBlob(tryCodes ++ assertCode ++ List(s"let $v = $chain;"), v, requires)
    }
  }

  private def typeOf(expr: ValuefyExpr): Type = analysis.typeInferer.typeOfValuefyExpr(expr).get

  /**
   * The type the generated Rust expression ACTUALLY produces. The type
   * inferer may type a choice arm by its enclosing union (the parent class),
   * which hides the need for a sealed up-cast — derive the concrete type
   * structurally where the expression shape determines it.
   */
  private def producedType(expr: ValuefyExpr): Type = expr match {
    case ValuefyExpr.ConstructCall(className, _) => Type.ClassType(className)
    case ValuefyExpr.MatchNonterminal(nonterminalName) =>
      analysis.nonterminalTypes(nonterminalName)
    case ValuefyExpr.Unbind(_, e) => producedType(e)
    case ValuefyExpr.SeqElemAt(_, e) => producedType(e)
    case ValuefyExpr.JoinBody(e) => producedType(e)
    case ValuefyExpr.JoinCond(e) => producedType(e)
    case _ => typeOf(expr)
  }

  case class SequenceVarName(var name: Option[String])

  // ---- the central recursion -------------------------------------------------

  def valuefyExprToCode(
    valuefyExpr: ValuefyExpr,
    beginGen: String,
    endGen: String,
    symbol: Symbols.Symbol,
    sequenceVarName: SequenceVarName,
    // 바깥 문맥이 기대하는 타입 — 중첩 UnrollChoices 의 arm 통일 타입을 문맥에
    // 맞추기 위해 전파한다 (typeInferer 의 union 은 irreducible 일 수 있고, LCA
    // 휴리스틱은 문맥과 다른 조상을 고를 수 있다).
    expectedHint: Option[Type] = None,
  ): ExprBlob = valuefyExpr match {
    case ValuefyExpr.InputNode =>
      // Reconstruct a partial parse tree. Not used by the PoC grammars; defer.
      ???
    case ValuefyExpr.MatchNonterminal(nonterminalName) =>
      val v = newVar()
      _requiredNonterms += nonterminalName
      ExprBlob(List(s"let $v = ${nonterminalMatchFuncName(nonterminalName)}_(ctx, $beginGen, $endGen);"), v, Set())
    case ValuefyExpr.Unbind(sym, expr) =>
      valuefyExprToCode(expr, beginGen, endGen, sym, sequenceVarName, expectedHint)
    case ValuefyExpr.JoinBody(bodyProcessor) =>
      val joinSymbol = symbol.asInstanceOf[Symbols.Join]
      valuefyExprToCode(bodyProcessor, beginGen, endGen, joinSymbol.sym, SequenceVarName(None), expectedHint)
    case ValuefyExpr.JoinCond(condProcessor) =>
      val joinSymbol = symbol.asInstanceOf[Symbols.Join]
      valuefyExprToCode(condProcessor, beginGen, endGen, joinSymbol.join, SequenceVarName(None))
    case ValuefyExpr.SeqElemAt(index, expr) =>
      val sequenceId = analysis.ngrammar.idOf(symbol)
      val sequence = analysis.ngrammar.nsequences(sequenceId)
      sequenceVarName.name match {
        case Some(seqVar) =>
          _symbolsOfInterest += sequence.sequence(index)
          valuefyExprToCode(expr, s"$seqVar[$index].0", s"$seqVar[$index].1", sequence.symbol.seq(index), sequenceVarName)
        case None =>
          val seqVar = newVar()
          _symbolsOfInterest += sequenceId
          _symbolsOfInterest ++= sequence.sequence
          val getSequenceElems =
            s"let $seqVar = get_sequence_elems(&ctx.history, $sequenceId, &[${sequence.sequence.mkString(", ")}], $beginGen, $endGen);"
          sequenceVarName.name = Some(seqVar)
          val elemValuefy = valuefyExprToCode(expr, s"$seqVar[$index].0", s"$seqVar[$index].1", sequence.symbol.seq(index), sequenceVarName)
          ExprBlob(getSequenceElems +: elemValuefy.prepares, elemValuefy.result, elemValuefy.required)
      }
    case ValuefyExpr.UnrollRepeatFromZero(elemProcessor) =>
      unrollRepeatCode("unroll_repeat0", elemProcessor, beginGen, endGen, symbol, arrayElemType(valuefyExpr))
    case ValuefyExpr.UnrollRepeatFromZeroNoUnbind(repeatSymbol, elemProcessor) =>
      assert(symbol == repeatSymbol)
      unrollRepeatCode("unroll_repeat0", elemProcessor, beginGen, endGen, symbol, arrayElemType(valuefyExpr))
    case ValuefyExpr.UnrollRepeatFromOne(elemProcessor) =>
      unrollRepeatCode("unroll_repeat1", elemProcessor, beginGen, endGen, symbol, arrayElemType(valuefyExpr))
    case ValuefyExpr.UnrollRepeatFromOneNoUnbind(repeatSymbol, elemProcessor) =>
      assert(symbol == repeatSymbol)
      unrollRepeatCode("unroll_repeat1", elemProcessor, beginGen, endGen, symbol, arrayElemType(valuefyExpr))
    case ValuefyExpr.UnrollChoices(choices) =>
      unrollChoicesExpr(choices, beginGen, endGen, symbol.toShortString, expectedHint.getOrElse(typeOf(valuefyExpr)))
    case ValuefyExpr.ConstructCall(className, params) =>
      val paramCodes = params.map(valuefyExprToCode(_, beginGen, endGen, symbol, sequenceVarName))
      val declParams = analysis.classParamTypes.getOrElse(className, List())
      check(declParams.size == paramCodes.size,
        s"param count mismatch for $className: ${declParams.size} fields vs ${paramCodes.size} args")
      val v = newVar()
      val fieldAssigns = declParams.zip(paramCodes).zip(params).map { case (((pname, fieldType), pc), paramExpr) =>
        val coerced = coerce(pc.result, producedType(paramExpr), FieldOf(fieldType))
        s"${rustFieldName(pname)}: $coerced"
      }
      // Include node_id/start/end in the same list so an empty param list does
      // not produce a leading comma (case objects / no-field case classes).
      val allAssigns = fieldAssigns ++ List(
        s"node_id: ctx.next_id()", s"start: $beginGen", s"end: $endGen")
      val construct =
        s"let $v = ${rustClassName(className)} { ${allAssigns.mkString(", ")} };"
      ExprBlob(paramCodes.flatMap(_.prepares) :+ construct, v, paramCodes.flatMap(_.required).toSet)
    case ValuefyExpr.FuncCall(funcType, params) =>
      funcCallToCode(funcType, params, beginGen, endGen, symbol, sequenceVarName)
    case ValuefyExpr.ArrayExpr(elems) =>
      val elemType = arrayElemType(valuefyExpr)
      val elemCodes = elems.map(valuefyExprToCode(_, beginGen, endGen, symbol, sequenceVarName))
      val coercedElems = elemCodes.zip(elems).map { case (ec, elem) =>
        coerce(ec.result, typeOf(elem), ElemOf(elemType))
      }
      ExprBlob(
        elemCodes.flatMap(_.prepares),
        s"vec![${coercedElems.mkString(", ")}]",
        elemCodes.flatMap(_.required).toSet)
    case ValuefyExpr.BinOp(op, lhs, rhs) =>
      val lhsCode = valuefyExprToCode(lhs, beginGen, endGen, symbol, sequenceVarName)
      val rhsCode = valuefyExprToCode(rhs, beginGen, endGen, symbol, sequenceVarName)
      val opExpr = op match {
        case ValuefyExpr.BinOpType.ADD =>
          (typeOf(lhs), typeOf(rhs)) match {
            // String + String: build a new String. Array + Array: chain + collect.
            case (Type.StringType, Type.StringType) =>
              s"format!(\"{}{}\", ${lhsCode.result}, ${rhsCode.result})"
            case (Type.ArrayOf(_), Type.ArrayOf(_)) =>
              s"{ let mut v = ${lhsCode.result}; v.extend(${rhsCode.result}); v }"
          }
        case ValuefyExpr.BinOpType.EQ =>
          check(typeOf(lhs) == typeOf(rhs), "lhs and rhs of == must be same type")
          s"(${lhsCode.result} == ${rhsCode.result})"
        case ValuefyExpr.BinOpType.NE =>
          check(typeOf(lhs) == typeOf(rhs), "lhs and rhs of != must be same type")
          s"(${lhsCode.result} != ${rhsCode.result})"
        case ValuefyExpr.BinOpType.BOOL_AND =>
          s"(${lhsCode.result} && ${rhsCode.result})"
        case ValuefyExpr.BinOpType.BOOL_OR =>
          s"(${lhsCode.result} || ${rhsCode.result})"
      }
      ExprBlob(lhsCode.prepares ++ rhsCode.prepares, opExpr, lhsCode.required ++ rhsCode.required)
    case ValuefyExpr.PreOp(op, expr) =>
      op match {
        case ValuefyExpr.PreOpType.NOT =>
          check(typeOf(expr) == Type.BoolType, "not can be applied only to boolean expression")
          val exprCode = valuefyExprToCode(expr, beginGen, endGen, symbol, sequenceVarName)
          val resultVar = newVar()
          ExprBlob(exprCode.prepares :+ s"let $resultVar = !${exprCode.result};", resultVar, exprCode.required)
      }
    case ValuefyExpr.ElvisOp(expr, ifNull) =>
      // `expr ?: ifNull`  → `expr.unwrap_or_else(|| ifNull)`. The Option here is
      // the typed AST's Option<T>; ifNull may itself need prepares so wrap in a
      // closure body block.
      val exprVar = newVar()
      val exprCode = valuefyExprToCode(expr, beginGen, endGen, symbol, sequenceVarName)
      val ifNullCode = valuefyExprToCode(ifNull, beginGen, endGen, symbol, sequenceVarName)
      ExprBlob(
        exprCode.prepares :+ s"let $exprVar = ${exprCode.result};",
        s"$exprVar.unwrap_or_else(|| ${ifNullCode.asBlockExpr})",
        exprCode.required ++ ifNullCode.required)
    case ValuefyExpr.TernaryOp(condition, ifTrue, ifFalse) =>
      // Both branches must produce the ternary's result type. When that type is
      // Optional and a branch produced a bare value (e.g. `cond ? value : null`
      // lowered here), the branch is coerced to `Some(value)`; a `null` branch
      // already became `None`.
      val resultType = typeOf(valuefyExpr)
      val conditionCode = valuefyExprToCode(condition, beginGen, endGen, symbol, sequenceVarName)
      val thenCode = valuefyExprToCode(ifTrue, beginGen, endGen, symbol, sequenceVarName)
      val elseCode = valuefyExprToCode(ifFalse, beginGen, endGen, symbol, sequenceVarName)
      val thenExpr = coerceBranch(thenCode.asBlockExpr, typeOf(ifTrue), resultType)
      val elseExpr = coerceBranch(elseCode.asBlockExpr, typeOf(ifFalse), resultType)
      val resultVar = newVar()
      ExprBlob(
        conditionCode.prepares ++
          List(s"let $resultVar = if ${conditionCode.result} { $thenExpr } else { $elseExpr };"),
        resultVar,
        conditionCode.required ++ thenCode.required ++ elseCode.required)
    case literal: ValuefyExpr.Literal =>
      literal match {
        case ValuefyExpr.NullLiteral => ExprBlob.code("None")
        case ValuefyExpr.BoolLiteral(value) => ExprBlob.code(s"$value")
        case ValuefyExpr.CharLiteral(value) => ExprBlob.code(s"'${escapeChar(value)}'")
        case ValuefyExpr.CharFromTerminalLiteral =>
          ExprBlob(List(), s"ctx.source_chars[$beginGen as usize]", Set())
        case ValuefyExpr.StringLiteral(value) => ExprBlob.code("\"" + escapeString(value) + "\".to_string()")
      }
    case enumValue: ValuefyExpr.EnumValue =>
      enumValue match {
        case ValuefyExpr.CanonicalEnumValue(enumName, ev) => ExprBlob.code(s"$enumName::$ev")
        case ValuefyExpr.ShortenedEnumValue(unspecifiedEnumTypeId, ev) =>
          val enumName = analysis.shortenedEnumTypesMap(unspecifiedEnumTypeId)
          ExprBlob.code(s"$enumName::$ev")
      }
  }

  private def unrollRepeatCode(
    helperFn: String,
    elemProcessor: ValuefyExpr,
    beginGen: String,
    endGen: String,
    symbol: Symbols.Symbol,
    elemType: Type,
  ): ExprBlob = {
    val v = newVar()
    val symbolId = analysis.ngrammar.idOf(symbol)
    val repeat = analysis.ngrammar.symbolOf(symbolId).asInstanceOf[NRepeat]
    val itemSymId = analysis.ngrammar.idOf(repeat.symbol.sym)
    val elemCode = valuefyExprToCode(elemProcessor, "k.0", "k.1", repeat.symbol.sym, SequenceVarName(None))
    _symbolsOfInterest ++= Set(symbolId, itemSymId, repeat.baseSeq, repeat.repeatSeq)
    // Each element is coerced to the Vec's element type (sealed up-cast when the
    // processor produced a concrete child of a sealed element type).
    val coercedElem = coerce(elemCode.result, typeOf(elemProcessor), ElemOf(elemType))
    // `xs.map { k -> prepares; result }` → into_iter().map(|k| { prepares result }).collect()
    val closureBody =
      if (elemCode.prepares.isEmpty) coercedElem
      else s"${elemCode.prepares.mkString(" ")} $coercedElem"
    ExprBlob(
      List(
        s"let $v: Vec<_> = $helperFn(&ctx.history, $symbolId, $itemSymId, ${repeat.baseSeq}, ${repeat.repeatSeq}, $beginGen, $endGen)",
        s"    .into_iter().map(|k| { $closureBody }).collect();"),
      v,
      elemCode.required)
  }

  /** elemType of an Array-typed ValuefyExpr (after union reduction). */
  private def arrayElemType(valuefyExpr: ValuefyExpr): Type = reduceType(typeOf(valuefyExpr)) match {
    case Type.ArrayOf(elemType) => elemType
    case other => throw new Exception(s"expected array type, got ${Type.readableNameOf(other)}")
  }

  def funcCallToCode(
    funcType: ValuefyExpr.FuncType.Value,
    params: List[ValuefyExpr],
    beginGen: String,
    endGen: String,
    symbol: Symbols.Symbol,
    sequenceVarName: SequenceVarName,
  ): ExprBlob = funcType match {
    case ValuefyExpr.FuncType.IsPresent =>
      check(params.size == 1, "ispresent function only can have exactly one parameter")
      val param = valuefyExprToCode(params.head, beginGen, endGen, symbol, sequenceVarName)

      @tailrec def isPresentCode(paramType: Type): String = paramType match {
        case Type.ArrayOf(_) => s"!${param.result}.is_empty()"
        case Type.OptionalOf(_) => s"${param.result}.is_some()"
        case Type.StringType => s"!${param.result}.is_empty()"
        case unionType: Type.UnionOf =>
          val reduced = analysis.reduceUnionType(unionType)
          check(!reduced.isInstanceOf[Type.UnionOf], "union type not supported in ispresent function")
          isPresentCode(reduced)
      }

      ExprBlob(param.prepares, isPresentCode(typeOf(params.head)), param.required)
    case ValuefyExpr.FuncType.IsEmpty =>
      check(params.size == 1, "isempty function only can have exactly one parameter")
      val param = valuefyExprToCode(params.head, beginGen, endGen, symbol, sequenceVarName)

      @tailrec def isEmptyCode(paramType: Type): String = paramType match {
        case Type.ArrayOf(_) => s"${param.result}.is_empty()"
        case Type.OptionalOf(_) => s"${param.result}.is_none()"
        case Type.StringType => s"${param.result}.is_empty()"
        case unionType: Type.UnionOf =>
          val reduced = analysis.reduceUnionType(unionType)
          check(!reduced.isInstanceOf[Type.UnionOf], "union type not supported in isempty function")
          isEmptyCode(reduced)
      }

      ExprBlob(param.prepares, isEmptyCode(typeOf(params.head)), param.required)
    case ValuefyExpr.FuncType.Chr =>
      check(params.size == 1, "chr function only can have exactly one parameter")
      val paramCode = valuefyExprToCode(params.head, beginGen, endGen, symbol, sequenceVarName)
      val result = typeOf(params.head) match {
        case Type.CharType => paramCode.result
      }
      ExprBlob(paramCode.prepares, result, paramCode.required)
    case ValuefyExpr.FuncType.Str =>
      val paramCodes = params.map(valuefyExprToCode(_, beginGen, endGen, symbol, sequenceVarName))

      // Produces a String. Each piece contributes a `&str`/`String` fragment;
      // we concatenate with format! / push. To keep it simple and uniform we
      // build via a block that pushes each fragment into a String.
      def toStringCode(input: String, inputType: Type): String = inputType match {
        case Type.ArrayOf(elemType) =>
          s"""$input.into_iter().map(|it| ${toStringCode("it", elemType)}).collect::<String>()"""
        case Type.OptionalOf(valueType) =>
          s"""$input.map(|it| ${toStringCode("it", valueType)}).unwrap_or_default()"""
        case Type.BoolType => s"$input.to_string()"
        case Type.CharType => s"$input.to_string()"
        case Type.StringType => input
        case unionType: Type.UnionOf =>
          val reduced = analysis.reduceUnionType(unionType)
          check(!reduced.isInstanceOf[Type.UnionOf], "union type not supported in str function")
          toStringCode(input, reduced)
      }

      val pieces = paramCodes.zip(params).map { case (paramCode, param) =>
        toStringCode(paramCode.result, typeOf(param))
      }
      // Concatenate pieces: `{ let mut s = String::new(); s.push_str(&p0); ... s }`
      val result =
        if (pieces.size == 1) pieces.head
        else s"""[${pieces.map(p => s"($p)").mkString(", ")}].concat()"""
      ExprBlob(paramCodes.flatMap(_.prepares), result, paramCodes.flatMap(_.required).toSet)
  }

  // ---- helpers ---------------------------------------------------------------

  /** Ordered snake_case field names for a constructor's declared params. */
  private def classParamFieldNames(className: String): List[String] =
    analysis.classParamTypes.getOrElse(className, List()).map(p => rustFieldName(p._1))

  // ---- AST type definitions (structs / enums / AstNode trait) ----------------

  /**
   * Emit one `pub struct` per concrete class and one `pub enum` per sealed
   * (abstract) class. Mirrors the PoC under examples/generated/rust/asdl_ast.rs.
   *
   * Every concrete struct carries `node_id`, `start`, `end`. The `AstNode`
   * trait exposes those three; each concrete type gets a mechanical impl. We
   * deliberately do NOT model the Kotlin `AstNode` sealed interface as an enum
   * (it would be a giant variant set); a trait is the natural Rust shape.
   */
  def classDefs(): String = {
    val hierarchy = analysis.classRelations.toHierarchy
    hierarchy.allTypes.values.toList.sortBy(_.className).map(classDef).mkString("\n")
  }

  private def classDef(cls: ClassHierarchyItem): String = {
    if (cls.subclasses.isEmpty) {
      // Concrete type → struct + a to_short_string() inherent impl matching
      // KotlinOptCodeGen's toShortString() (payload fields only; no id/span).
      val params = analysis.classParamTypes.getOrElse(cls.className, List())
      val fields = params.map { case (pname, ptype) =>
        s"    pub ${rustFieldName(pname)}: ${rustType(ptype)},"
      }
      // Field label uses the original (non-snake) param name to match Kotlin.
      val fmtPieces = params.map { case (pname, _) => s"$pname={}" }
      val fmtArgs = params.map { case (pname, ptype) =>
        shortStringFieldExpr(s"self.${rustFieldName(pname)}", ptype)
      }
      val fmtStr = s"${cls.className}(${fmtPieces.mkString(", ")})"
      val toShort =
        if (params.isEmpty)
          s"""    pub fn to_short_string(&self) -> String {
             |        "${cls.className}()".to_string()
             |    }""".stripMargin
        else
          s"""    pub fn to_short_string(&self) -> String {
             |        format!("$fmtStr", ${fmtArgs.mkString(", ")})
             |    }""".stripMargin
      s"""#[derive(Debug, Clone)]
         |pub struct ${rustClassName(cls.className)} {
         |${fields.mkString("\n")}
         |    pub node_id: i32,
         |    pub start: i32,
         |    pub end: i32,
         |}
         |
         |impl ${rustClassName(cls.className)} {
         |$toShort
         |}
         |""".stripMargin
    } else {
      // Abstract type → enum over its (sorted) subclasses, each Boxed.
      // to_short_string dispatches to the active variant.
      val subs = cls.subclasses.toList.sorted
      val variants = subs.map(sub => s"    ${rustClassName(sub)}(Box<${rustClassName(sub)}>),")
      val arms = subs.map(sub => s"            ${rustClassName(cls.className)}::${rustClassName(sub)}(x) => x.to_short_string(),")
      s"""#[derive(Debug, Clone)]
         |pub enum ${rustClassName(cls.className)} {
         |${variants.mkString("\n")}
         |}
         |
         |impl ${rustClassName(cls.className)} {
         |    pub fn to_short_string(&self) -> String {
         |        match self {
         |${arms.mkString("\n")}
         |        }
         |    }
         |}
         |""".stripMargin
    }
  }

  def enumDefs(): String = {
    val enums = analysis.enumValuesMap
    if (enums.isEmpty) "" else {
      enums.keySet.toList.sorted.map { enumName =>
        val values = enums(enumName).toList.sorted
        val members = values.map(v => s"    $v,")
        // to_short_string prints the variant name, matching Kotlin's `$enumVal`
        // (an enum's toString is its name).
        val arms = values.map(v => s"            $enumName::$v => \"$v\",")
        s"""#[derive(Debug, Clone, Copy, PartialEq, Eq)]
           |pub enum $enumName {
           |${members.mkString("\n")}
           |}
           |
           |impl $enumName {
           |    pub fn to_short_string(&self) -> String {
           |        match self {
           |${arms.mkString("\n")}
           |        }.to_string()
           |    }
           |}
           |""".stripMargin
      }.mkString("\n")
    }
  }

  /**
   * Rust expression producing the short-string fragment for one field, matching
   * `KotlinOptCodeGen.toShortStringExpr`. The result is a `String`/`&str`-ish
   * value spliced into a `format!("...{}...")`, so every branch yields something
   * `Display`-able.
   */
  private def shortStringFieldExpr(accessor: String, t: Type): String = reduceType(t) match {
    case Type.ClassType(_) => s"$accessor.to_short_string()"
    case Type.OptionalOf(inner) => reduceType(inner) match {
      case Type.ClassType(_) =>
        // Kotlin: `x?.toShortString()` → "null" when absent.
        s"""$accessor.as_ref().map(|x| x.to_short_string()).unwrap_or_else(|| "null".to_string())"""
      case _: Type.EnumType | _: Type.UnspecifiedEnumType =>
        s"""$accessor.as_ref().map(|x| x.to_short_string()).unwrap_or_else(|| "null".to_string())"""
      case Type.ArrayOf(elemType) =>
        // Opt(Array(...)): "null" when absent, else the array's short form.
        // NOTE: KotlinOptCodeGen mishandles this case (it falls through to the
        // bare expr and leaks node toString); Rust does the correct thing, so
        // the diff harness must canonicalise the Kotlin side for such fields.
        s"""$accessor.as_ref().map(|v| ${arrayShortExpr("v", elemType)}).unwrap_or_else(|| "null".to_string())"""
      case _ =>
        // Opt(scalar): Kotlin prints the value or "null".
        s"""$accessor.as_ref().map(|x| format!("{}", x)).unwrap_or_else(|| "null".to_string())"""
    }
    case Type.ArrayOf(elemType) => arrayShortExpr(accessor, elemType)
    case _: Type.EnumType | _: Type.UnspecifiedEnumType =>
      s"$accessor.to_short_string()"
    case Type.CharType =>
      // Kotlin prints the char directly.
      s"$accessor"
    case _ =>
      // bool / string / node bytes: Display.
      s"$accessor"
  }

  /** Short-string for a `Vec<elemType>` bound to `accessor`. Matches Kotlin's `[a, b]`. */
  private def arrayShortExpr(accessor: String, elemType: Type): String = reduceType(elemType) match {
    case Type.ClassType(_) | (_: Type.EnumType) | (_: Type.UnspecifiedEnumType) =>
      s"""format!("[{}]", $accessor.iter().map(|it| it.to_short_string()).collect::<Vec<_>>().join(", "))"""
    case _ =>
      // Array(scalar): Kotlin's List.toString() = "[a, b, c]".
      s"""format!("[{}]", $accessor.iter().map(|it| format!("{}", it)).collect::<Vec<_>>().join(", "))"""
  }

  /** `impl AstNode for Foo` for every concrete class, plus the trait itself. */
  def astNodeTraitAndImpls(): String = {
    val hierarchy = analysis.classRelations.toHierarchy
    val concretes = hierarchy.allTypes.values.toList.filter(_.subclasses.isEmpty).map(_.className).sorted
    val traitDef =
      s"""pub trait AstNode {
         |    fn node_id(&self) -> i32;
         |    fn start(&self) -> i32;
         |    fn end(&self) -> i32;
         |}
         |""".stripMargin
    val impls = concretes.map { c0 =>
      val c = rustClassName(c0)
      s"""impl AstNode for $c {
         |    fn node_id(&self) -> i32 { self.node_id }
         |    fn start(&self) -> i32 { self.start }
         |    fn end(&self) -> i32 { self.end }
         |}
         |""".stripMargin
    }.mkString("\n")
    traitDef + "\n" + impls
  }

  // ---- assembly --------------------------------------------------------------

  /**
   * Emit the whole `ast.rs` module: AST types, the walk context + walk
   * functions. The proto encoder lives in a separate file
   * (see [[RustProtoEncoderGen]]) so the two concerns stay decoupled.
   *
   * Layout:
   *   - enum definitions
   *   - struct / sealed-enum definitions
   *   - AstNode trait + impls
   *   - `struct Ctx { source_chars, history, ids }` + `match_start`
   *   - free `match_x_` functions
   */
  def generate(): String = {
    val writer = new StringWriter()

    writer.write("// Generated by RustOptCodeGen — do not edit by hand.\n")
    writer.write("// AST walk mirrors KotlinOptCodeGen; see\n")
    writer.write("// mgroup3/docs/phase_b_proto_design.md.\n")
    // Some leaf match fns don't read end_gen (e.g. a single-char terminal).
    writer.write("#![allow(unused_variables)]\n\n")
    writer.write("use crate::ktlib::{\n")
    writer.write("    get_sequence_elems, has_single_true, unroll_repeat0, unroll_repeat1,\n")
    writer.write("    IdIssuer, KernelSet,\n")
    writer.write("};\n\n")

    writer.write(enumDefs())
    writer.write("\n")
    writer.write(classDefs())
    writer.write("\n")
    writer.write(astNodeTraitAndImpls())
    writer.write("\n")

    // Walk context. Holds the char view, history, and id issuer.
    writer.write(
      """pub struct Ctx<'a> {
        |    pub source_chars: &'a [char],
        |    pub history: &'a [KernelSet],
        |    pub ids: IdIssuer,
        |}
        |
        |impl<'a> Ctx<'a> {
        |    pub fn new(source_chars: &'a [char], history: &'a [KernelSet]) -> Self {
        |        Self { source_chars, history, ids: IdIssuer::new(0) }
        |    }
        |
        |    fn next_id(&mut self) -> i32 {
        |        self.ids.next_id()
        |    }
        |
        |""".stripMargin)

    // matchStart is a method on Ctx (it reads source length + start symbol).
    writer.write(matchStartFunc())
    writer.write("\n}\n\n")

    // Free match functions for each reachable nonterminal.
    var visitedNonterms = Set[String]()
    while ((_requiredNonterms -- visitedNonterms).nonEmpty) {
      val next = (_requiredNonterms -- visitedNonterms).head
      writer.write(nonterminalMatchFunc(next))
      writer.write("\n\n")
      visitedNonterms += next
    }

    writer.toString
  }

  // ===========================================================================
  // Proto encoder: typed AST -> ID-based ParseResult (see phase_b_proto_design).
  //
  // Emitted into a separate `encode.rs`. Rules MUST match Stage2ProtoEmit:
  //   - one NodeEntry per concrete node; id monotonic from 1 (0 = absent).
  //   - Msg field           -> child encoded first, its id stored as int32.
  //   - Opt(Msg) field      -> `<f>_present: bool` + `<f>: i32` (0 when absent).
  //   - Opt(Arr(Msg))       -> `<f>_present: bool` + `<f>: Vec<i32>`.
  //   - Arr(Msg)            -> `<f>: Vec<i32>`.
  //   - scalars/enums       -> copied directly.
  //   - sealed parent       -> match, delegate to the concrete variant's enc fn.
  //   - nodes pushed post-order (children before parent) so the Kotlin decoder
  //     resolves in one forward pass.
  // ===========================================================================

  def generateEncoder(): String = {
    val hierarchy = analysis.classRelations.toHierarchy
    val all = hierarchy.allTypes.values.toList.sortBy(_.className)
    val concretes = all.filter(_.subclasses.isEmpty)
    val sealeds = all.filter(_.subclasses.nonEmpty)

    val header =
      """// Generated by RustOptCodeGen — do not edit by hand.
        |// typed AST -> ID-based proto ParseResult. See phase_b_proto_design.md.
        |
        |use crate::ast::*;
        |use crate::proto;
        |
        |/// Accumulates NodeEntry rows and hands out monotonic ids (from 1).
        |struct Encoder {
        |    nodes: Vec<proto::NodeEntry>,
        |    next_id: i32,
        |}
        |
        |impl Encoder {
        |    fn new() -> Self {
        |        Self { nodes: Vec::new(), next_id: 1 }
        |    }
        |    fn alloc(&mut self) -> i32 {
        |        let id = self.next_id;
        |        self.next_id += 1;
        |        id
        |    }
        |}
        |
        |""".stripMargin

    val concreteFns = concretes.map(concreteEncodeFn).mkString("\n")
    val sealedFns = sealeds.map(sealedEncodeFn).mkString("\n")

    // Public entry: encode the start symbol's type. `enc` is owned here, so the
    // call site borrows it mutably (inner fns already take `&mut Encoder`).
    val rootType = analysis.nonterminalTypes(analysis.startNonterminalName)
    val rootFn = rootType match {
      case Type.ClassType(name) => s"enc_${camelToSnake(name)}"
      case other => throw new Exception(s"start symbol type is not a class: ${Type.readableNameOf(other)}")
    }
    val entry =
      s"""/// Encode a typed AST root into a `ParseResult`.
         |pub fn encode(root: &${rustTypeUnboxed(rootType)}) -> proto::ParseResult {
         |    let mut enc = Encoder::new();
         |    let root_id = $rootFn(&mut enc, root);
         |    proto::ParseResult { root_id, nodes: enc.nodes }
         |}
         |""".stripMargin

    header + concreteFns + "\n" + sealedFns + "\n" + entry
  }

  /** `enc_<snake>` for a concrete class: encode children, build proto msg, push. */
  private def concreteEncodeFn(cls: ClassHierarchyItem): String = {
    val className = cls.className
    val fnName = s"enc_${camelToSnake(className)}"
    val params = analysis.classParamTypes.getOrElse(className, List())

    val prepares = scala.collection.mutable.ListBuffer[String]()
    val protoFields = scala.collection.mutable.ListBuffer[String]()

    params.foreach { case (pname, ptype) =>
      val field = rustFieldName(pname)
      ptype match {
        case Type.ClassType(_) =>
          // child id
          prepares += s"    let ${field}_id = ${encodeCallFor(ptype, s"&node.$field")};"
          protoFields += s"$field: ${field}_id"
        case Type.OptionalOf(Type.ClassType(_)) =>
          prepares += s"    let (${field}_present, ${field}_id) = match &node.$field {"
          prepares += s"        Some(x) => (true, ${encodeCallFor(ptype.asInstanceOf[Type.OptionalOf].typ, "x")}),"
          prepares += s"        None => (false, 0),"
          prepares += s"    };"
          protoFields += s"${field}_present: ${field}_present"
          protoFields += s"$field: ${field}_id"
        case Type.OptionalOf(Type.ArrayOf(elemT)) if isMsg(elemT) =>
          prepares += s"    let (${field}_present, $field): (bool, Vec<i32>) = match &node.$field {"
          prepares += s"        Some(xs) => (true, xs.iter().map(|x| ${encodeCallFor(elemT, "x")}).collect()),"
          prepares += s"        None => (false, Vec::new()),"
          prepares += s"    };"
          protoFields += s"${field}_present: ${field}_present"
          protoFields += s"$field: $field"
        case Type.OptionalOf(Type.ArrayOf(elemT2)) =>
          // Opt(Arr(scalar)): char/enum 은 i32 변환 (enum 은 +1 시프트).
          val conv = reduceType(elemT2) match {
            case Type.CharType => "xs.iter().map(|c| *c as i32).collect()"
            case Type.EnumType(_) | Type.UnspecifiedEnumType(_) =>
              "xs.iter().map(|e| (*e as i32) + 1).collect()"
            case _ => "xs.clone()"
          }
          prepares += s"    let (${field}_present, $field): (bool, Vec<_>) = match &node.$field {"
          prepares += s"        Some(xs) => (true, $conv),"
          prepares += s"        None => (false, Vec::new()),"
          prepares += s"    };"
          protoFields += s"${field}_present: ${field}_present"
          protoFields += s"$field: $field"
        case Type.OptionalOf(inner) =>
          // Opt(scalar/enum): present flag + copied value (default when absent).
          prepares += s"    let (${field}_present, $field) = match &node.$field {"
          prepares += s"        Some(x) => (true, ${copyScalar(inner, "x")}),"
          prepares += s"        None => (false, ${scalarDefault(inner)}),"
          prepares += s"    };"
          protoFields += s"${field}_present: ${field}_present"
          protoFields += s"$field: $field"
        case Type.ArrayOf(elemT) if isMsg(elemT) =>
          prepares += s"    let $field: Vec<i32> = node.$field.iter().map(|x| ${encodeCallFor(elemT, "x")}).collect();"
          protoFields += s"$field: $field"
        case Type.ArrayOf(elemT) =>
          // Vec of scalars. char/enum become i32 (with the enum +1 shift).
          reduceType(elemT) match {
            case Type.CharType =>
              protoFields += s"$field: node.$field.iter().map(|c| *c as i32).collect()"
            case Type.EnumType(_) | Type.UnspecifiedEnumType(_) =>
              protoFields += s"$field: node.$field.iter().map(|e| (*e as i32) + 1).collect()"
            case _ =>
              protoFields += s"$field: node.$field.clone()"
          }
        case _ =>
          // scalar / enum
          protoFields += s"$field: ${copyScalarField(ptype, s"node.$field")}"
      }
    }

    val protoFieldsStr =
      (protoFields.toList :+ "start: node.start" :+ "end: node.end").map("        " + _).mkString(",\n")

    s"""fn $fnName(enc: &mut Encoder, node: &${rustClassName(className)}) -> i32 {
       |${prepares.mkString("\n")}
       |    let id = enc.alloc();
       |    let msg = proto::${rustClassName(className)} {
       |$protoFieldsStr,
       |    };
       |    enc.nodes.push(proto::NodeEntry {
       |        id,
       |        node: Some(proto::node_entry::Node::${rustClassName(className)}(msg)),
       |    });
       |    id
       |}
       |""".stripMargin
  }

  /** `enc_<snake>` for a sealed parent: match and delegate to the variant. */
  private def sealedEncodeFn(cls: ClassHierarchyItem): String = {
    val fnName = s"enc_${camelToSnake(cls.className)}"
    val arms = cls.subclasses.toList.sorted.map { sub =>
      s"        ${rustClassName(cls.className)}::${rustClassName(sub)}(x) => enc_${camelToSnake(sub)}(enc, x),"
    }
    s"""fn $fnName(enc: &mut Encoder, node: &${rustClassName(cls.className)}) -> i32 {
       |    match node {
       |${arms.mkString("\n")}
       |    }
       |}
       |""".stripMargin
  }

  /** Build the `enc_x(enc, <expr>)` call for a Msg-or-sealed-typed value. */
  private def encodeCallFor(t: Type, expr: String): String = t match {
    case Type.ClassType(name) => s"enc_${camelToSnake(name)}(enc, $expr)"
    case Type.OptionalOf(inner) => encodeCallFor(inner, expr)
    case _ => throw new Exception(s"encodeCallFor on non-message type: ${Type.readableNameOf(t)}")
  }

  private def isMsg(t: Type): Boolean = t match {
    case Type.ClassType(_) => true
    case _ => false
  }

  // Scalar/enum copy at a proto field. Strings clone; char becomes i32
  // codepoint (proto stores char as int32 per Stage2ProtoEmit's CharType=>Int32).
  // Enums: the Rust AST enum discriminants are 0-based (sorted variant order)
  // while the proto enum reserves 0 for UNSPECIFIED and assigns the same
  // sorted order from 1 (Stage2ProtoEmit) — hence the +1 shift.
  private def copyScalarField(t: Type, expr: String): String = t match {
    case Type.StringType => s"$expr.clone()"
    case Type.CharType => s"$expr as i32"
    case Type.BoolType => expr
    case Type.EnumType(_) | Type.UnspecifiedEnumType(_) => s"($expr as i32) + 1"
    case _ => s"$expr.clone()"
  }

  private def copyScalar(t: Type, expr: String): String = t match {
    case Type.StringType => s"$expr.clone()"
    case Type.CharType => s"*$expr as i32"
    case Type.BoolType => s"*$expr"
    case Type.EnumType(_) | Type.UnspecifiedEnumType(_) => s"(*$expr as i32) + 1"
    case _ => s"$expr.clone()"
  }

  private def scalarDefault(t: Type): String = t match {
    case Type.StringType => "String::new()"
    case Type.CharType => "0"
    case Type.BoolType => "false"
    case Type.EnumType(_) | Type.UnspecifiedEnumType(_) => "0"
    case _ => "Default::default()"
  }
}

object RustOptCodeGen {
  /** SchemaBuilder.RUST_RESERVED_TYPE_NAMES 와 동일 목록 유지할 것. */
  val RustReservedTypeNames: Set[String] = Set(
    "Self", "Box", "Option", "Vec", "String", "Result", "Some", "None", "Ok", "Err",
    "Ctx", "Encoder", "Kernel", "KernelSet", "IdIssuer",
  )

  val RustKeywords: Set[String] = Set(
    "as", "break", "const", "continue", "crate", "else", "enum", "extern", "false", "fn",
    "for", "if", "impl", "in", "let", "loop", "match", "mod", "move", "mut", "pub", "ref",
    "return", "self", "Self", "static", "struct", "super", "trait", "true", "type", "unsafe",
    "use", "where", "while", "async", "await", "dyn", "abstract", "become", "box", "do",
    "final", "macro", "override", "priv", "typeof", "unsized", "virtual", "yield", "try",
  )
}
